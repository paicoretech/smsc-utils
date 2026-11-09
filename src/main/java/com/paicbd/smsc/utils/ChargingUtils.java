package com.paicbd.smsc.utils;

import com.paicbd.smsc.dto.MessageEvent;
import com.paicbd.smsc.dto.UtilsRecords;
import com.paicbd.smsc.kafka.KafkaTopicsConstants;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.jsmpp.util.DeliveryReceiptState;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Objects;
import java.util.Optional;

@Slf4j
@UtilityClass
public class ChargingUtils {

    public static final String CHARGING_SUCCESS_PARAM = "charging_success";

    public static void publishChargingCompletion(MessageEvent messageEvent, boolean successful,
                                                  KafkaTemplate<String, String> kafkaTemplate) {
        if (messageEvent.isDlr()
                || !ChargingType.ECUR.equals(messageEvent.getChargingType())
                || !messageEvent.isFinalSegmentForSplitSmsc()) {
            return;
        }

        MessageEvent completionEvent = Converter.deepCopy(messageEvent, MessageEvent.class);
        completionEvent.addCustomParam(CHARGING_SUCCESS_PARAM, successful);
        kafkaTemplate.send(
                KafkaTopicsConstants.CHARGING_COMPLETION_TOPIC,
                completionEvent.toString());
    }

    public static boolean isChargingCompletion(MessageEvent messageEvent) {
        return Objects.nonNull(messageEvent)
                && ChargingType.ECUR.equals(messageEvent.getChargingType())
                && Objects.nonNull(messageEvent.getCustomParams())
                && messageEvent.getCustomParams().containsKey(CHARGING_SUCCESS_PARAM);
    }

    public static boolean isChargingSuccessful(MessageEvent messageEvent) {
        return isChargingCompletion(messageEvent)
                && Boolean.TRUE.equals(messageEvent.getCustomParams().get(CHARGING_SUCCESS_PARAM));
    }

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

        return chekDeliverForRefundMessage(deliveryReceiptState, deliverSmEvent, submitSmResponseEvent.applyForRefund(), submitSmResponseEvent.submitSmServerId());
    }

    public static Optional<MessageEvent> chekDeliverForRefundMessage(DeliveryReceiptState deliveryReceiptState, MessageEvent deliverSmEvent, boolean applyForRefund, String submitSmServerId) {
        Optional<MessageEvent> messageEventOptional = Optional.empty();
        if (ChargingUtils.isFailedStatusForRefund(deliveryReceiptState) && applyForRefund) {
            MessageEvent messageEventToRefund = new MessageEvent();
            messageEventToRefund.setMessageId(submitSmServerId);
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

    public static boolean requiresEcurChargingForRetry(MessageEvent messageEvent) {
        return Objects.nonNull(messageEvent)
                && messageEvent.isRetry()
                && messageEvent.isDiameterCharging()
                && ChargingType.ECUR.equals(messageEvent.getChargingType())
                && !isChargingCompletion(messageEvent);
    }
}
