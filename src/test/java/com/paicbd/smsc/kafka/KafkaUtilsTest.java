package com.paicbd.smsc.kafka;

import com.paicbd.smsc.dto.MessageEvent;
import com.paicbd.smsc.utils.GeneralSmscConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KafkaUtilsTest {

    private static final String HIGH = "HIGH";
    private static final String MEDIUM = "MEDIUM";
    private static final String LOW = "LOW";

    static Stream<Arguments> messageTopicParams() {
        return Stream.of(
                Arguments.of("SMPP", 101, KafkaTopicsConstants.SMPP_HIGH_MESSAGE_TOPIC_SUFFIX, HIGH),
                Arguments.of("SMPP", 101, KafkaTopicsConstants.SMPP_MEDIUM_MESSAGE_TOPIC_SUFFIX, MEDIUM),
                Arguments.of("SMPP", 101, KafkaTopicsConstants.SMPP_LOW_MESSAGE_TOPIC_SUFFIX, LOW),

                Arguments.of("HTTP", 202, KafkaTopicsConstants.HTTP_HIGH_MESSAGE_TOPIC_SUFFIX, HIGH),
                Arguments.of("HTTP", 202, KafkaTopicsConstants.HTTP_MEDIUM_MESSAGE_TOPIC_SUFFIX, MEDIUM),
                Arguments.of("HTTP", 202, KafkaTopicsConstants.HTTP_LOW_MESSAGE_TOPIC_SUFFIX, LOW),

                Arguments.of("SS7", 303, KafkaTopicsConstants.SS7_HIGH_MESSAGE_TOPIC_SUFFIX, HIGH),
                Arguments.of("SS7", 303, KafkaTopicsConstants.SS7_MEDIUM_MESSAGE_TOPIC_SUFFIX, MEDIUM),
                Arguments.of("SS7", 303, KafkaTopicsConstants.SS7_LOW_MESSAGE_TOPIC_SUFFIX, LOW),

                Arguments.of("DIAMETER", 404, KafkaTopicsConstants.DIAMETER_HIGH_MESSAGE_TOPIC_SUFFIX, HIGH),
                Arguments.of("DIAMETER", 404, KafkaTopicsConstants.DIAMETER_MEDIUM_MESSAGE_TOPIC_SUFFIX, MEDIUM),
                Arguments.of("DIAMETER", 404, KafkaTopicsConstants.DIAMETER_LOW_MESSAGE_TOPIC_SUFFIX, LOW)
        );
    }

    static Stream<Arguments> dlrTopicParams() {
        return Stream.of(
                Arguments.of("SMPP", 999, KafkaTopicsConstants.SMPP_DLR_TOPIC, false, null),
                Arguments.of("HTTP", 999, KafkaTopicsConstants.HTTP_DLR_TOPIC, false, null),
                Arguments.of("SS7", 77, KafkaTopicsConstants.SS7_HIGH_MESSAGE_TOPIC_SUFFIX, true, HIGH),
                Arguments.of("SS7", 77, KafkaTopicsConstants.SS7_MEDIUM_MESSAGE_TOPIC_SUFFIX, true, MEDIUM),
                Arguments.of("SS7", 77, KafkaTopicsConstants.SS7_LOW_MESSAGE_TOPIC_SUFFIX, true, LOW),
                Arguments.of("DIAMETER", 88, KafkaTopicsConstants.DIAMETER_HIGH_MESSAGE_TOPIC_SUFFIX, true, HIGH),
                Arguments.of("DIAMETER", 88, KafkaTopicsConstants.DIAMETER_MEDIUM_MESSAGE_TOPIC_SUFFIX, true, MEDIUM),
                Arguments.of("DIAMETER", 88, KafkaTopicsConstants.DIAMETER_LOW_MESSAGE_TOPIC_SUFFIX, true, LOW)
        );
    }

    @ParameterizedTest
    @MethodSource("messageTopicParams")
    void messageTopicsMapping(String protocol, int netId, String expectedSuffix, String priority) {
        MessageEvent messageEvent = MessageEvent.builder()
                .destProtocol(protocol)
                .smscMessagePriority(priority)
                .destNetworkId(netId)
                .build();
        var topic = KafkaUtils.getMessageDestinationTopic(messageEvent);
        assertEquals(netId + expectedSuffix, topic);
    }

    @ParameterizedTest
    @MethodSource("dlrTopicParams")
    void dlrTopicsMapping(String protocol, int netId, String expected, boolean useNetworkId, String priority) {
        MessageEvent messageEvent = MessageEvent.builder()
                .destProtocol(protocol)
                .destNetworkId(netId)
                .smscMessagePriority(priority)
                .build();

        var topic = KafkaUtils.getDeliveryDestinationTopic(messageEvent);
        if (useNetworkId) {
            assertEquals(netId + expected, topic);
        } else {
            assertEquals(expected, topic);
        }
    }

    @Test
    void unknownProtocolThrows() {
        MessageEvent messageEvent = MessageEvent.builder()
                .destProtocol("XMPP")
                .build();

        var messageTopicError = assertThrows(NullPointerException.class, () -> KafkaUtils.getMessageDestinationTopic(messageEvent));
        assertTrue(messageTopicError.getMessage().contains("SMS destination topic not found"));

        var dlrTopicError = assertThrows(NullPointerException.class, () -> KafkaUtils.getDeliveryDestinationTopic(messageEvent));
        assertTrue(dlrTopicError.getMessage().contains("DLR destination topic not found"));
    }


    @ParameterizedTest(name = "priority={0} -> topic={1}")
    @MethodSource("priorityProviderForRouting")
    void shouldReturnExpectedKafkaTopicBasedOnPriorityForRouting(String priority, String expectedTopic) {
        String result = KafkaUtils.getRoutingTopicPriority(priority);
        assertEquals(expectedTopic, result);
    }

    @ParameterizedTest(name = "priority={0} -> topic={1}")
    @MethodSource("priorityProviderForRetries")
    void shouldReturnExpectedKafkaTopicBasedOnPriorityRetries(String priority, String expectedTopic) {
        String result = KafkaUtils.getRetriesTopicPriority(priority);
        assertEquals(expectedTopic, result);
    }

    private static Stream<Arguments> priorityProviderForRouting() {
        return Stream.of(
                Arguments.of(
                        GeneralSmscConstants.HIGH_PRIORITY,
                        KafkaTopicsConstants.PRE_MESSAGE_HIGH_TOPIC
                ),
                Arguments.of(
                        GeneralSmscConstants.MEDIUM_PRIORITY,
                        KafkaTopicsConstants.PRE_MESSAGE_MEDIUM_TOPIC
                ),
                Arguments.of(
                        GeneralSmscConstants.LOW_PRIORITY,
                        KafkaTopicsConstants.PRE_MESSAGE_LOW_TOPIC
                ),
                Arguments.of(
                        "UNKNOWN_PRIORITY",
                        KafkaTopicsConstants.PRE_MESSAGE_MEDIUM_TOPIC
                )
        );
    }

    @ParameterizedTest(name = "[{index}] priority=''{0}'' → expected=''{1}''")
    @MethodSource("provideChargingTopicPriorityCases")
    void getChargingTopicPriorityReturnsExpectedTopic(String inputPriority, String expectedTopic) {
        String result = KafkaUtils.getChargingTopicPriority(inputPriority);

        assertEquals(expectedTopic, result);
    }

    private static Stream<Arguments> provideChargingTopicPriorityCases() {
        return Stream.of(
                Arguments.of(GeneralSmscConstants.HIGH_PRIORITY, KafkaTopicsConstants.CHARGING_HIGH_MESSAGE_TOPIC),
                Arguments.of(GeneralSmscConstants.MEDIUM_PRIORITY, KafkaTopicsConstants.CHARGING_MEDIUM_MESSAGE_TOPIC),
                Arguments.of(GeneralSmscConstants.LOW_PRIORITY, KafkaTopicsConstants.CHARGING_LOW_MESSAGE_TOPIC),

                Arguments.of("UNKNOWN_VALUE", KafkaTopicsConstants.CHARGING_MEDIUM_MESSAGE_TOPIC),
                Arguments.of("CRITICAL", KafkaTopicsConstants.CHARGING_MEDIUM_MESSAGE_TOPIC),
                Arguments.of("high", KafkaTopicsConstants.CHARGING_MEDIUM_MESSAGE_TOPIC),
                Arguments.of("", KafkaTopicsConstants.CHARGING_MEDIUM_MESSAGE_TOPIC),
                Arguments.of(" ", KafkaTopicsConstants.CHARGING_MEDIUM_MESSAGE_TOPIC)
        );
    }

    @ParameterizedTest(name = "[{index}] null input throws NullPointerException")
    @NullSource
    void getChargingTopicPriority_whenNull_throwsNullPointerException(String nullPriority) {
        assertThrows(NullPointerException.class,
                () -> KafkaUtils.getChargingTopicPriority(nullPriority));
    }

    private static Stream<Arguments> priorityProviderForRetries() {
        return Stream.of(
                Arguments.of(
                        GeneralSmscConstants.HIGH_PRIORITY,
                        KafkaTopicsConstants.RETRIES_HIGH_TOPIC
                ),
                Arguments.of(
                        GeneralSmscConstants.MEDIUM_PRIORITY,
                        KafkaTopicsConstants.RETRIES_MEDIUM_TOPIC
                ),
                Arguments.of(
                        GeneralSmscConstants.LOW_PRIORITY,
                        KafkaTopicsConstants.RETRIES_LOW_TOPIC
                ),
                Arguments.of(
                        "UNKNOWN_PRIORITY",
                        KafkaTopicsConstants.RETRIES_MEDIUM_TOPIC
                )
        );
    }
}
