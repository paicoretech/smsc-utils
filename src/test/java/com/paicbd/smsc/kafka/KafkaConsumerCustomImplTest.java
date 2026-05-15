package com.paicbd.smsc.kafka;

import com.paicbd.smsc.dto.MessageEvent;
import com.paicbd.smsc.scylla.ScyllaManager;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaConsumerCustomImplTest {

    @Mock
    private KafkaConsumerFactory kafkaConsumerFactory;

    @Mock
    private ScyllaManager scyllaManager;

    @Mock
    private KafkaConsumer<String, String> kafkaConsumerMock;

    private KafkaConsumerCustomImpl consumerCustom;


    private final String messageInRaw = MessageEvent.builder()
            .id(System.currentTimeMillis() + "-" + System.nanoTime())
            .messageId(System.currentTimeMillis() + "-" + System.nanoTime())
            .parentId("parent-123")
            .systemId("smpp_sp")
            .sourceAddr("8888")
            .destinationAddr("1234")
            .shortMessage("Hello!")
            .originNetworkId(3)
            .originNetworkType("SP")
            .originProtocol("SMPP")
            .destNetworkType("GW")
            .destProtocol("SS7")
            .destNetworkId(5)
            .sourceAddrTon(1)
            .sourceAddrNpi(4)
            .destAddrTon(1)
            .destAddrNpi(4)
            .dataCoding(0)
            .registeredDelivery(1)
            .build().toString();

    private final String topic = "test-topic";
    private final int maxPollRecords = 10;

    @BeforeEach
    void setUp() {
        when(kafkaConsumerFactory.createConsumer(anyMap(), eq(topic)))
                .thenReturn(kafkaConsumerMock);

        String groupId = "smsc-group-id";
        String server = "localhost:9092";
        consumerCustom = new KafkaConsumerCustomImpl(
                topic, groupId, server, maxPollRecords, kafkaConsumerFactory, scyllaManager
        );
    }

    @Test
    void startConsumerWhenLoadFromCacheThenShouldDelete() {
        List<String> cachedMessages = Collections.singletonList(messageInRaw);
        when(scyllaManager.getPendingMessagesByTopic(topic)).thenReturn(cachedMessages);
        consumerCustom.startConsumer();
        assertEquals(1, consumerCustom.getBufferQueue().size());
        verify(scyllaManager).deletePendingMessagesByTopic(topic);
    }

    @Test
    void startAndStopConsumerShouldGetDataFromCacheAndThenStoreOnStop() {
        when(scyllaManager.getPendingMessagesByTopic(topic)).thenReturn(Collections.emptyList());
        Map<TopicPartition, List<ConsumerRecord<String, String>>> recordsMap = new HashMap<>();
        List<ConsumerRecord<String, String>> recordList = new ArrayList<>();
        recordList.add(new ConsumerRecord<>(topic, 0, 0L, "", messageInRaw));
        recordsMap.put(new TopicPartition(topic, 0), recordList);
        ConsumerRecords<String, String> records = new ConsumerRecords<>(recordsMap);
        when(kafkaConsumerMock.poll(any(Duration.class))).thenReturn(records);
        consumerCustom.startConsumer();
        await().atMost(5, TimeUnit.SECONDS).until(() ->
                !consumerCustom.getBufferQueue().isEmpty()
        );
        assertEquals(maxPollRecords, consumerCustom.getBufferQueue().size());
        verify(kafkaConsumerMock, atLeastOnce()).poll(any(Duration.class));
        consumerCustom.stopConsumer();
        verify(kafkaConsumerMock).wakeup();
        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        verify(scyllaManager).insertInPendingMessagesTable(eq(topic), captor.capture());
        List<String> savedMessages = captor.getValue();
        assertEquals(maxPollRecords, savedMessages.size());
        assertTrue(consumerCustom.getBufferQueue().isEmpty());
    }


    @Test
    void startConsumerDoesNotStartIfAlreadyRunning() {
        when(scyllaManager.getPendingMessagesByTopic(topic)).thenReturn(Collections.emptyList());
        consumerCustom.startConsumer();
        consumerCustom.startConsumer();
        verify(scyllaManager, times(1)).getPendingMessagesByTopic(topic);
    }
}