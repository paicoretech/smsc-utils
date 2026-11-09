package com.paicbd.smsc.utils;

import com.paicbd.smsc.dto.MessageEvent;
import com.paicbd.smsc.dto.UtilsRecords;
import com.paicbd.smsc.kafka.KafkaTopicsConstants;
import org.jsmpp.util.DeliveryReceiptState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ChargingUtilsTest {

    private static final String TEST_MESSAGE_ID = System.currentTimeMillis() + "-" + System.nanoTime();
    private static final String TEST_SOURCE_ADDR = "50587878787";
    private static final String TEST_DEST_ADDR = "50587878785";

    private UtilsRecords.SubmitSmResponseEvent createSubmitSmResponseEvent(boolean applyForRefund) {
        return new UtilsRecords.SubmitSmResponseEvent(
                "455555547",
                System.currentTimeMillis() + "-" + System.nanoTime(),
                "systemId",
                System.currentTimeMillis() + "-" + System.nanoTime(),
                TEST_MESSAGE_ID,
                "SMPP",
                1,
                "SP",
                null,
                null,
                null,
                TEST_MESSAGE_ID,
                2,
                applyForRefund,
                new HashMap<>(),
                false,
                "",
                "",
                ""
        );
    }

    private MessageEvent createDeliverSmEvent() {
        return MessageEvent.builder()
                .deliverSmId(TEST_MESSAGE_ID)
                .messageId(TEST_MESSAGE_ID)
                .sourceAddr(TEST_SOURCE_ADDR)
                .destinationAddr(TEST_DEST_ADDR)
                .build();
    }


    private MessageEvent createMessageEvent(boolean applyForRefund) {
        return MessageEvent.builder()
                .deliverSmId(TEST_MESSAGE_ID)
                .messageId(TEST_MESSAGE_ID)
                .sourceAddr(TEST_SOURCE_ADDR)
                .destinationAddr(TEST_DEST_ADDR)
                .applyForRefund(applyForRefund)
                .build();
    }


    @ParameterizedTest
    @EnumSource(value = DeliveryReceiptState.class, names = {"UNDELIV", "EXPIRED", "REJECTD"})
    @DisplayName("Test chekDeliverForRefundMessage for failed scenarios")
    void testChekDeliverForRefundMessageWithFailedStatusThenReturnTrue(DeliveryReceiptState failedState) {
        UtilsRecords.SubmitSmResponseEvent submitSmResponseEvent = createSubmitSmResponseEvent(true);
        MessageEvent deliverSmEvent = createDeliverSmEvent();

        Optional<MessageEvent> result = ChargingUtils.chekDeliverForRefundMessage(failedState, submitSmResponseEvent, deliverSmEvent);

        assertTrue(result.isPresent());
        MessageEvent refundedMessage = result.get();
        assertEquals(TEST_MESSAGE_ID, refundedMessage.getMessageId());
        assertTrue(refundedMessage.isReadyForRefund());
        assertTrue(refundedMessage.isApplyForRefund());
        assertEquals(TEST_DEST_ADDR, refundedMessage.getSourceAddr());
        assertEquals(TEST_SOURCE_ADDR, refundedMessage.getDestinationAddr());
    }


    @Test
    @DisplayName("Test chekDeliverForRefundMessage for success scenarios")
    void testChekDeliverForRefundMessageWithSuccessStatusThenReturnEmpty() {
        UtilsRecords.SubmitSmResponseEvent submitSmResponseEvent = createSubmitSmResponseEvent(true);
        MessageEvent deliverSmEvent = createDeliverSmEvent();
        Optional<MessageEvent> result = ChargingUtils.chekDeliverForRefundMessage(DeliveryReceiptState.DELIVRD, submitSmResponseEvent, deliverSmEvent);
        assertFalse(result.isPresent());
    }


    @Test
    @DisplayName("Test chekDeliverForRefundMessage for failed scenarios with applyForRefund in false")
    void testChekDeliverForRefundMessageWithFailedStatusAndApplyForRefundInFalseThenReturnEmpty() {
        UtilsRecords.SubmitSmResponseEvent submitSmResponseEvent = createSubmitSmResponseEvent(false); // applyForRefund = false
        MessageEvent deliverSmEvent = createDeliverSmEvent();
        Optional<MessageEvent> result = ChargingUtils.chekDeliverForRefundMessage(DeliveryReceiptState.UNDELIV, submitSmResponseEvent, deliverSmEvent);
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Test chekDeliverForRefundMessage for success scenarios with applyForRefund in false")
    void testChekDeliverForRefundMessageWithSuccessStatusAndApplyForRefundInFalseThenReturnEmpty() {
        UtilsRecords.SubmitSmResponseEvent submitSmResponseEvent = createSubmitSmResponseEvent(false);
        MessageEvent deliverSmEvent = createDeliverSmEvent();
        Optional<MessageEvent> result = ChargingUtils.chekDeliverForRefundMessage(DeliveryReceiptState.DELIVRD, submitSmResponseEvent, deliverSmEvent);
        assertFalse(result.isPresent());
    }


    @ParameterizedTest
    @DisplayName("Test checkMessageForRefund with applyForRefund True / False")
    @CsvSource({
            "true",
            "false"
    })
    void testCheckMessageForRefundWithApplyForRefund(boolean applyForRefund) {
        MessageEvent messageEvent = createMessageEvent(applyForRefund);
        boolean result = ChargingUtils.checkMessageForRefund(messageEvent);
        assertEquals(applyForRefund, result);
    }

    @Test
    @DisplayName("Publish the final ECUR completion event without modifying the original message")
    void testPublishChargingCompletion() {
        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
        MessageEvent messageEvent = MessageEvent.builder()
                .messageId(TEST_MESSAGE_ID)
                .parentId("parent-id")
                .chargingType(ChargingType.ECUR)
                .smscMessagePriority("MEDIUM")
                .isRetry(true)
                .retryNumber(3)
                .isLastRetry(true)
                .errorCode(34)
                .build();

        ChargingUtils.publishChargingCompletion(messageEvent, false, kafkaTemplate);

        ArgumentCaptor<String> eventCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq(KafkaTopicsConstants.CHARGING_COMPLETION_TOPIC), eventCaptor.capture());
        MessageEvent completionEvent = Converter.stringToObject(eventCaptor.getValue(), MessageEvent.class);
        assertEquals(TEST_MESSAGE_ID, completionEvent.getMessageId());
        assertEquals("parent-id", completionEvent.getParentId());
        assertTrue(completionEvent.isRetry());
        assertEquals(3, completionEvent.getRetryNumber());
        assertTrue(completionEvent.isLastRetry());
        assertEquals(34, completionEvent.getErrorCode());
        assertTrue(ChargingUtils.isChargingCompletion(completionEvent));
        assertFalse(ChargingUtils.isChargingSuccessful(completionEvent));
        assertEquals(Map.of(ChargingUtils.CHARGING_SUCCESS_PARAM, false), completionEvent.getCustomParams());
        assertNull(completionEvent.getStatus());
        assertNull(messageEvent.getCustomParams());
    }

    @Test
    @DisplayName("Identify a successful ECUR completion")
    void testSuccessfulChargingCompletion() {
        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
        MessageEvent messageEvent = MessageEvent.builder()
                .messageId(TEST_MESSAGE_ID)
                .chargingType(ChargingType.ECUR)
                .smscMessagePriority("MEDIUM")
                .build();

        ChargingUtils.publishChargingCompletion(messageEvent, true, kafkaTemplate);

        ArgumentCaptor<String> eventCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq(KafkaTopicsConstants.CHARGING_COMPLETION_TOPIC), eventCaptor.capture());
        MessageEvent completionEvent = Converter.stringToObject(eventCaptor.getValue(), MessageEvent.class);
        assertTrue(ChargingUtils.isChargingCompletion(completionEvent));
        assertTrue(ChargingUtils.isChargingSuccessful(completionEvent));
        assertEquals(Map.of(ChargingUtils.CHARGING_SUCCESS_PARAM, true), completionEvent.getCustomParams());
    }

    @Test
    @DisplayName("Do not publish a completion event for IEC, DLR, or an intermediate segment")
    void testDoNotPublishChargingCompletionForNonFinalEvents() {
        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
        MessageEvent iec = MessageEvent.builder()
                .chargingType(ChargingType.IEC)
                .smscMessagePriority("MEDIUM")
                .build();
        MessageEvent dlr = MessageEvent.builder()
                .chargingType(ChargingType.ECUR)
                .isDlr(true)
                .smscMessagePriority("MEDIUM")
                .build();
        MessageEvent intermediateSegment = MessageEvent.builder()
                .chargingType(ChargingType.ECUR)
                .splitForSmsc(true)
                .totalSegment(2)
                .segmentSequence(1)
                .smscMessagePriority("MEDIUM")
                .build();

        ChargingUtils.publishChargingCompletion(iec, true, kafkaTemplate);
        ChargingUtils.publishChargingCompletion(dlr, true, kafkaTemplate);
        ChargingUtils.publishChargingCompletion(intermediateSegment, true, kafkaTemplate);

        verify(kafkaTemplate, never()).send(anyString(), anyString());
    }

    @Test
    @DisplayName("Require ECUR charging only for retry messages that use Diameter charging")
    void testRequiresEcurChargingForRetry() {
        MessageEvent messageEvent = MessageEvent.builder()
                .isRetry(true)
                .diameterCharging(true)
                .chargingType(ChargingType.ECUR)
                .build();

        assertTrue(ChargingUtils.requiresEcurChargingForRetry(messageEvent));

        messageEvent.setChargingType(ChargingType.IEC);
        assertFalse(ChargingUtils.requiresEcurChargingForRetry(messageEvent));

        messageEvent.setChargingType(ChargingType.ECUR);
        messageEvent.setRetry(false);
        assertFalse(ChargingUtils.requiresEcurChargingForRetry(messageEvent));

        messageEvent.setRetry(true);
        messageEvent.setDiameterCharging(false);
        assertFalse(ChargingUtils.requiresEcurChargingForRetry(messageEvent));

        messageEvent.setDiameterCharging(true);
        messageEvent.addCustomParam(ChargingUtils.CHARGING_SUCCESS_PARAM, true);
        assertFalse(ChargingUtils.requiresEcurChargingForRetry(messageEvent));
    }

}
