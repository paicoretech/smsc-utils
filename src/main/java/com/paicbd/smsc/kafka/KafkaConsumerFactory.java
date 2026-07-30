package com.paicbd.smsc.kafka;

import com.paicbd.smsc.utils.Generated;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.util.List;
import java.util.Map;

@Generated
public class KafkaConsumerFactory {
    public KafkaConsumer<String, String> createConsumer(Map<String, Object> props, String topic) {
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(List.of(topic));
        return consumer;
    }
}
