package com.paicbd.smsc.kafka;

import com.paicbd.smsc.dto.MessageEvent;
import com.paicbd.smsc.utils.GeneralSmscConstants;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

@Slf4j
@UtilityClass
public class KafkaUtils {

    private static final Map<String, Function<MessageEvent, String>> smsDestinationTopics;

    static {
        Map<String, Function<MessageEvent, String>> map = new HashMap<>();
        map.put(GeneralSmscConstants.SMPP_PROTOCOL + "-" + GeneralSmscConstants.HIGH_PRIORITY,
                e -> e.getDestNetworkId() + KafkaTopicsConstants.SMPP_HIGH_MESSAGE_TOPIC_SUFFIX);
        map.put(GeneralSmscConstants.SMPP_PROTOCOL + "-" + GeneralSmscConstants.MEDIUM_PRIORITY,
                e -> e.getDestNetworkId() + KafkaTopicsConstants.SMPP_MEDIUM_MESSAGE_TOPIC_SUFFIX);
        map.put(GeneralSmscConstants.SMPP_PROTOCOL + "-" + GeneralSmscConstants.LOW_PRIORITY,
                e -> e.getDestNetworkId() + KafkaTopicsConstants.SMPP_LOW_MESSAGE_TOPIC_SUFFIX);

        map.put(GeneralSmscConstants.HTTP_PROTOCOL + "-" + GeneralSmscConstants.HIGH_PRIORITY,
                e -> e.getDestNetworkId() + KafkaTopicsConstants.HTTP_HIGH_MESSAGE_TOPIC_SUFFIX);
        map.put(GeneralSmscConstants.HTTP_PROTOCOL + "-" + GeneralSmscConstants.MEDIUM_PRIORITY,
                e -> e.getDestNetworkId() + KafkaTopicsConstants.HTTP_MEDIUM_MESSAGE_TOPIC_SUFFIX);
        map.put(GeneralSmscConstants.HTTP_PROTOCOL + "-" + GeneralSmscConstants.LOW_PRIORITY,
                e -> e.getDestNetworkId() + KafkaTopicsConstants.HTTP_LOW_MESSAGE_TOPIC_SUFFIX);

        map.put(GeneralSmscConstants.SS7_PROTOCOL + "-" + GeneralSmscConstants.HIGH_PRIORITY,
                e -> e.getDestNetworkId() + KafkaTopicsConstants.SS7_HIGH_MESSAGE_TOPIC_SUFFIX);
        map.put(GeneralSmscConstants.SS7_PROTOCOL + "-" + GeneralSmscConstants.MEDIUM_PRIORITY,
                e -> e.getDestNetworkId() + KafkaTopicsConstants.SS7_MEDIUM_MESSAGE_TOPIC_SUFFIX);
        map.put(GeneralSmscConstants.SS7_PROTOCOL + "-" + GeneralSmscConstants.LOW_PRIORITY,
                e -> e.getDestNetworkId() + KafkaTopicsConstants.SS7_LOW_MESSAGE_TOPIC_SUFFIX);

        map.put(GeneralSmscConstants.DIAMETER_PROTOCOL + "-" + GeneralSmscConstants.HIGH_PRIORITY,
                e -> e.getDestNetworkId() + KafkaTopicsConstants.DIAMETER_HIGH_MESSAGE_TOPIC_SUFFIX);
        map.put(GeneralSmscConstants.DIAMETER_PROTOCOL + "-" + GeneralSmscConstants.MEDIUM_PRIORITY,
                e -> e.getDestNetworkId() + KafkaTopicsConstants.DIAMETER_MEDIUM_MESSAGE_TOPIC_SUFFIX);
        map.put(GeneralSmscConstants.DIAMETER_PROTOCOL + "-" + GeneralSmscConstants.LOW_PRIORITY,
                e -> e.getDestNetworkId() + KafkaTopicsConstants.DIAMETER_LOW_MESSAGE_TOPIC_SUFFIX);

        map.put(GeneralSmscConstants.SIP_PROTOCOL + "-" + GeneralSmscConstants.HIGH_PRIORITY,
                e -> e.getDestNetworkId() + KafkaTopicsConstants.SIP_HIGH_MESSAGE_TOPIC_SUFFIX);
        map.put(GeneralSmscConstants.SIP_PROTOCOL + "-" + GeneralSmscConstants.MEDIUM_PRIORITY,
                e -> e.getDestNetworkId() + KafkaTopicsConstants.SIP_MEDIUM_MESSAGE_TOPIC_SUFFIX);
        map.put(GeneralSmscConstants.SIP_PROTOCOL + "-" + GeneralSmscConstants.LOW_PRIORITY,
                e -> e.getDestNetworkId() + KafkaTopicsConstants.SIP_LOW_MESSAGE_TOPIC_SUFFIX);

        smsDestinationTopics = Map.copyOf(map);
    }

    private static final Map<String, Function<MessageEvent, String>> dlrDestinationTopics = new HashMap<>();
    static {
        dlrDestinationTopics.put("SMPP", e -> KafkaTopicsConstants.SMPP_DLR_TOPIC);
        dlrDestinationTopics.put("HTTP", e -> KafkaTopicsConstants.HTTP_DLR_TOPIC);

        dlrDestinationTopics.put(GeneralSmscConstants.SS7_PROTOCOL + "-" + GeneralSmscConstants.HIGH_PRIORITY,
                e -> e.getDestNetworkId() + KafkaTopicsConstants.SS7_HIGH_MESSAGE_TOPIC_SUFFIX);
        dlrDestinationTopics.put(GeneralSmscConstants.SS7_PROTOCOL + "-" + GeneralSmscConstants.MEDIUM_PRIORITY,
                e -> e.getDestNetworkId() + KafkaTopicsConstants.SS7_MEDIUM_MESSAGE_TOPIC_SUFFIX);
        dlrDestinationTopics.put(GeneralSmscConstants.SS7_PROTOCOL + "-" + GeneralSmscConstants.LOW_PRIORITY,
                e -> e.getDestNetworkId() + KafkaTopicsConstants.SS7_LOW_MESSAGE_TOPIC_SUFFIX);

        dlrDestinationTopics.put(GeneralSmscConstants.DIAMETER_PROTOCOL + "-" + GeneralSmscConstants.HIGH_PRIORITY,
                e -> e.getDestNetworkId() + KafkaTopicsConstants.DIAMETER_HIGH_MESSAGE_TOPIC_SUFFIX);
        dlrDestinationTopics.put(GeneralSmscConstants.DIAMETER_PROTOCOL + "-" + GeneralSmscConstants.MEDIUM_PRIORITY,
                e -> e.getDestNetworkId() + KafkaTopicsConstants.DIAMETER_MEDIUM_MESSAGE_TOPIC_SUFFIX);
        dlrDestinationTopics.put(GeneralSmscConstants.DIAMETER_PROTOCOL + "-" + GeneralSmscConstants.LOW_PRIORITY,
                e -> e.getDestNetworkId() + KafkaTopicsConstants.DIAMETER_LOW_MESSAGE_TOPIC_SUFFIX);

        dlrDestinationTopics.put(GeneralSmscConstants.SIP_PROTOCOL + "-" + GeneralSmscConstants.HIGH_PRIORITY,
                e -> e.getDestNetworkId() + KafkaTopicsConstants.SIP_HIGH_MESSAGE_TOPIC_SUFFIX);
        dlrDestinationTopics.put(GeneralSmscConstants.SIP_PROTOCOL + "-" + GeneralSmscConstants.MEDIUM_PRIORITY,
                e -> e.getDestNetworkId() + KafkaTopicsConstants.SIP_MEDIUM_MESSAGE_TOPIC_SUFFIX);
        dlrDestinationTopics.put(GeneralSmscConstants.SIP_PROTOCOL + "-" + GeneralSmscConstants.LOW_PRIORITY,
                e -> e.getDestNetworkId() + KafkaTopicsConstants.SIP_LOW_MESSAGE_TOPIC_SUFFIX);
    }

    public static String getMessageDestinationTopic(MessageEvent messageEvent) {
        Function<MessageEvent, String> topicResolver = smsDestinationTopics.get(messageEvent.getDestProtocol() + "-" + messageEvent.getSmscMessagePriority());
        Objects.requireNonNull(topicResolver, "SMS destination topic not found for protocol: " + messageEvent.getDestProtocol());
        return topicResolver.apply(messageEvent);
    }

    public static String getDeliveryDestinationTopic(MessageEvent messageEvent) {
        Function<MessageEvent, String> topicResolver;
        if (GeneralSmscConstants.HTTP_PROTOCOL.equals(messageEvent.getDestProtocol()) ||
            GeneralSmscConstants.SMPP_PROTOCOL.equals(messageEvent.getDestProtocol())) {
            topicResolver = dlrDestinationTopics.get(messageEvent.getDestProtocol());
        } else {
            topicResolver = dlrDestinationTopics.get(messageEvent.getDestProtocol() + "-" + messageEvent.getSmscMessagePriority());
        }
        Objects.requireNonNull(topicResolver, "DLR destination topic not found for protocol: " + messageEvent.getDestProtocol());
        return topicResolver.apply(messageEvent);
    }


    public static String getRoutingTopicPriority(String messagePriority) {
        return switch (messagePriority) {
            case GeneralSmscConstants.HIGH_PRIORITY -> KafkaTopicsConstants.PRE_MESSAGE_HIGH_TOPIC;
            case GeneralSmscConstants.MEDIUM_PRIORITY -> KafkaTopicsConstants.PRE_MESSAGE_MEDIUM_TOPIC;
            case GeneralSmscConstants.LOW_PRIORITY -> KafkaTopicsConstants.PRE_MESSAGE_LOW_TOPIC;
            default -> {
                log.warn("Unknown message priority for Routing: {}. Defaulting to MEDIUM priority.", messagePriority);
                yield KafkaTopicsConstants.PRE_MESSAGE_MEDIUM_TOPIC;
            }
        };
    }


    public static String getRetriesTopicPriority(String messagePriority) {
        return switch (messagePriority) {
            case GeneralSmscConstants.HIGH_PRIORITY -> KafkaTopicsConstants.RETRIES_HIGH_TOPIC;
            case GeneralSmscConstants.MEDIUM_PRIORITY -> KafkaTopicsConstants.RETRIES_MEDIUM_TOPIC;
            case GeneralSmscConstants.LOW_PRIORITY -> KafkaTopicsConstants.RETRIES_LOW_TOPIC;
            default -> {
                log.warn("Unknown message priority for Retries: {}. Defaulting to MEDIUM priority.", messagePriority);
                yield KafkaTopicsConstants.RETRIES_MEDIUM_TOPIC;
            }
        };
    }

    public static String getChargingTopicPriority(String messagePriority) {
        return switch (messagePriority) {
            case GeneralSmscConstants.HIGH_PRIORITY -> KafkaTopicsConstants.CHARGING_HIGH_MESSAGE_TOPIC;
            case GeneralSmscConstants.MEDIUM_PRIORITY -> KafkaTopicsConstants.CHARGING_MEDIUM_MESSAGE_TOPIC;
            case GeneralSmscConstants.LOW_PRIORITY -> KafkaTopicsConstants.CHARGING_LOW_MESSAGE_TOPIC;
            default -> {
                log.warn("Unknown message priority for Charging: {}. Defaulting to MEDIUM priority.", messagePriority);
                yield KafkaTopicsConstants.CHARGING_MEDIUM_MESSAGE_TOPIC;
            }
        };
    }
}
