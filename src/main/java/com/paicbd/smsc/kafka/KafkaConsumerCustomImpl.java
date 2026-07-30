package com.paicbd.smsc.kafka;

import com.paicbd.smsc.scylla.ScyllaManager;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class KafkaConsumerCustomImpl {
    private final String topicName;
    private final String groupId;
    private final String servers;
    private final int maxPollRecords;
    private final ScyllaManager scyllaManager;
    private final KafkaConsumerFactory kafkaConsumerFactory;

    @Getter
    private final BlockingQueue<String> bufferQueue = new LinkedBlockingQueue<>();
    private final Object consumerLock = new Object();
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    private Thread consumerThread;
    private KafkaConsumer<String, String> kafkaConsumer;


    public KafkaConsumerCustomImpl(String topicName, String groupId, String servers, int maxPollRecords, KafkaConsumerFactory kafkaConsumerFactory, ScyllaManager scyllaManager) {
        this.topicName = topicName;
        this.groupId = groupId;
        this.servers = servers;
        this.maxPollRecords = maxPollRecords;
        this.scyllaManager = scyllaManager;
        this.kafkaConsumerFactory = kafkaConsumerFactory;
    }

    public synchronized void startConsumer() {
        if (isRunning.get()) {
            log.warn("Kafka Consumer for topic {} is already marked as running.", this.topicName);
            return;
        }

        synchronized (consumerLock) {
            if (Objects.nonNull(this.kafkaConsumer)) {
                this.kafkaConsumer = null;
            }
            Map<String, Object> props = this.getStringKafkaConnectionPropertiesMap();
            this.kafkaConsumer = kafkaConsumerFactory.createConsumer(props, this.topicName);
        }

        isRunning.set(true);

        this.getFromCache();
        log.info("Starting Kafka Consumer for topic: {}", this.topicName);
        this.consumerThread = new Thread(this::runConsumerLoop);
        this.consumerThread.setDaemon(true);
        this.consumerThread.setName("KafkaConsumer-" + topicName);
        this.consumerThread.start();
    }


    private void runConsumerLoop() {
        try {
            while (isRunning.get() && !Thread.currentThread().isInterrupted()) {
                if (bufferQueue.size() >= maxPollRecords) {
                    this.sleepLoop();
                    continue;
                }
                this.pollData();
            }
        } catch (WakeupException e) {
            log.info("Consumer for topic {} waking up for shutdown.", topicName);
        } catch (Exception ex) {
            log.error("Error processing Kafka messages for topic {}: {}", topicName, ex.getMessage());
        } finally {
            this.closeInternal();
            isRunning.set(false);
        }
    }

    private void sleepLoop() {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void closeInternal() {
        synchronized (consumerLock) {
            try {
                if (Objects.nonNull(kafkaConsumer)) {
                    if (Thread.interrupted()) {
                        log.debug("Current thread interrupted during consumer close for topic {}", this.topicName);
                    }
                    kafkaConsumer.close(Duration.ofSeconds(5));
                    log.info("Kafka Consumer closed for topic {}", this.topicName);
                }
            } catch (Exception e) {
                log.warn("Error closing consumer: {}", e.getMessage());
            } finally {
                this.kafkaConsumer = null;
            }
        }
    }

    public void stopConsumer() {
        if (!isRunning.get()) return;
        log.info("Signal stopping Kafka Consumer for topic {}", topicName);
        isRunning.set(false);
        synchronized (consumerLock) {
            if (Objects.nonNull(this.kafkaConsumer)) {
                this.kafkaConsumer.wakeup();
            }
        }

        if (Objects.nonNull(this.consumerThread) && this.consumerThread.isAlive())
            this.consumerThread.interrupt();

        if (!this.bufferQueue.isEmpty())
            this.saveToCache();
    }

    private void saveToCache() {
        List<String> bufferedRecords = new ArrayList<>();
        this.bufferQueue.drainTo(bufferedRecords);
        if (!bufferedRecords.isEmpty()) {
            log.debug("Persisting {} records from buffer queue", bufferedRecords.size());
            this.scyllaManager.insertInPendingMessagesTable(this.topicName, bufferedRecords);
        }
    }

    private void getFromCache() {
        var result = scyllaManager.getPendingMessagesByTopic(this.topicName);
        this.bufferQueue.addAll(result);
        if (!result.isEmpty()) {
            log.debug("Loaded {} records into buffer queue from cache", result.size());
            this.scyllaManager.deletePendingMessagesByTopic(this.topicName);
        }
    }

    private void pollData() {
        KafkaConsumer<String, String> localConsumer;
        synchronized (consumerLock) {
            localConsumer = this.kafkaConsumer;
        }
        if (localConsumer == null) return;
        ConsumerRecords<String, String> records = localConsumer.poll(Duration.ofMillis(1000));
        if (records != null && !records.isEmpty()) {
            log.debug("Kafka Consumer poll {} records for topics", records.count());
            for (ConsumerRecord<String, String> recordItem : records) {
                boolean offered = bufferQueue.offer(recordItem.value());
                if (!offered) {
                    log.error("Buffer queue is full. Unable to add record from topic {} partition {} offset {}", recordItem.topic(), recordItem.partition(), recordItem.offset());
                }
            }
        }
    }

    private Map<String, Object> getStringKafkaConnectionPropertiesMap() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, this.servers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, this.groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getCanonicalName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getCanonicalName());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, this.maxPollRecords);
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 1);
        props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, (1024 * 1024 * 1024)); // 1 GB
        props.put(ConsumerConfig.FETCH_MAX_BYTES_CONFIG, 1024 * 1024 * 1024);
        props.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, 1024 * 1024 * 1024);
        return props;
    }
}
