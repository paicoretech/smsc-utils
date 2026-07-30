package com.paicbd.smsc.utils;

import com.paicbd.smsc.dto.MessageEvent;
import com.paicbd.smsc.dto.UtilsRecords;
import org.jsmpp.util.DeliveryReceiptState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.HashMap;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
                false
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

}