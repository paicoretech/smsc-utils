package com.paicbd.smsc.utils;

import com.paicbd.smsc.dto.MessageEvent;
import com.paicbd.smsc.dto.UtilsRecords;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.jsmpp.util.DeliveryReceiptState;

import java.util.Optional;

@Slf4j
@UtilityClass
public class ChargingUtils {

    public static boolean checkMessageForRefund(MessageEvent messageEvent) {
        if (!messageEvent.isApplyForRefund()) {
            log.debug("The message with id {} is not applying for refund", messageEvent.getMessageId());
            return false;
        }
        log.warn("The message with id {} will be refunded", messageEvent.getMessageId());
        messageEvent.setReadyForRefund(true);
        return true;
    }


    public static Optional<MessageEvent> chekDeliverForRefundMessage(DeliveryReceiptState deliveryReceiptState,
                                                       UtilsRecords.SubmitSmResponseEvent submitSmResponseEvent,
                                                       MessageEvent deliverSmEvent) {
        Optional<MessageEvent> messageEventOptional = Optional.empty();
        if (ChargingUtils.isFailedStatusForRefund(deliveryReceiptState) && submitSmResponseEvent.applyForRefund()) {
            MessageEvent messageEventToRefund = new MessageEvent();
            messageEventToRefund.setMessageId(submitSmResponseEvent.submitSmServerId());
            messageEventToRefund.setReadyForRefund(true);
            messageEventToRefund.setApplyForRefund(true);
            messageEventToRefund.setSourceAddr(deliverSmEvent.getDestinationAddr());
            messageEventToRefund.setDestinationAddr(deliverSmEvent.getSourceAddr());
            messageEventOptional = Optional.of(messageEventToRefund);
        }
        return messageEventOptional;
    }


    private static boolean isFailedStatusForRefund(DeliveryReceiptState deliveryReceiptState) {
        return (deliveryReceiptState == DeliveryReceiptState.UNDELIV
                || deliveryReceiptState == DeliveryReceiptState.EXPIRED
                || deliveryReceiptState == DeliveryReceiptState.REJECTD);
    }
}
