package com.paicbd.smsc.kafka;

import com.paicbd.smsc.dto.MessageEvent;
import com.paicbd.smsc.utils.Converter;
import com.paicbd.smsc.utils.Generated;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.LockSupport;

@Slf4j
public class KafkaConsumerHandler {

    @Generated
    public void stoppingPerSeconds(int tps, int batchMessage, long start) {
        long elapsed = System.nanoTime() - start;
        long realTime = (batchMessage * 1_000_000_000L) / tps;
        long sleepTime = realTime - elapsed;
        if (sleepTime > 0) {
            LockSupport.parkNanos(sleepTime);
        }
    }

    public List<List<MessageEvent>> partitionList(List<MessageEvent> list, int size) {
        List<List<MessageEvent>> parts = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            parts.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return parts;
    }


    public int batchPerWorker(int totalMessages, int workersPerGateway, int recordsToTake) {
        int workers = workersPerGateway > 0 ? workersPerGateway : 1;
        int min = Math.min(recordsToTake, totalMessages);
        int bpw = min / workers;
        return bpw > 0 ? bpw : 1;
    }


    private int drainQueue(KafkaConsumerCustomImpl kafkaConsumerCustom, int limit, List<String> output) {
        int drained = 0;
        if (Objects.isNull(kafkaConsumerCustom))
            return drained;

        BlockingQueue<String> queue = kafkaConsumerCustom.getBufferQueue();
        while (drained < limit) {
            String msg = queue.poll();
            if (msg == null) break;
            output.add(msg);
            drained++;
        }
        return drained;
    }


    public List<MessageEvent> fetchMessages(int maxPollRecords, int highQuota, int medQuota, int lowQuota,
                                            KafkaConsumerCustomImpl kafkaConsumerHigh, KafkaConsumerCustomImpl kafkaConsumerMedium, KafkaConsumerCustomImpl kafkaConsumerLow) {
        List<String> result = new ArrayList<>(maxPollRecords);
        int fetchedHigh = this.drainQueue(kafkaConsumerHigh, highQuota, result);
        int fetchedMed = this.drainQueue(kafkaConsumerMedium, medQuota, result);
        int fetchedLow = this.drainQueue(kafkaConsumerLow, lowQuota, result);
        int remaining = maxPollRecords - (fetchedHigh + fetchedMed + fetchedLow);
        if (remaining > 0) {
            remaining -= this.drainQueue(kafkaConsumerHigh, remaining, result);
            if (remaining > 0) remaining -= this.drainQueue(kafkaConsumerMedium, remaining, result);
            if (remaining > 0) this.drainQueue(kafkaConsumerLow, remaining, result);
        }
        return result
                .stream()
                .map(message -> Converter.stringToObject(message, MessageEvent.class))
                .filter(Objects::nonNull)
                .toList();
    }


    public List<String> fetchDataInRaw(int maxPollRecords, KafkaConsumerCustomImpl kafkaConsumerCustom) {
        List<String> result = new ArrayList<>(maxPollRecords);
        this.drainQueue(kafkaConsumerCustom, maxPollRecords, result);
        return result
                .stream()
                .filter(Objects::nonNull)
                .toList();
    }

}
