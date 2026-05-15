package com.paicbd.smsc.cdr;

import com.paicbd.smsc.dto.MessageEvent;
import com.paicbd.smsc.dto.UtilsRecords;
import com.paicbd.smsc.kafka.KafkaTopicsConstants;
import com.paicbd.smsc.utils.BroadcastMessageStatus;
import com.paicbd.smsc.utils.UtilsEnum;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import static com.paicbd.smsc.utils.GeneralSmscConstants.SMSC_DATETIME_FORMATTER;

@Slf4j
public class CdrProcessor {

    public void publishCdr(@NonNull MessageEvent event, UtilsEnum.Module module, UtilsEnum.MessageType messageType,
                           UtilsEnum.CdrStatus status, KafkaTemplate<String, String> kafkaTemplate) {
        this.publishCdr(event, "", module, messageType, status, null, kafkaTemplate);
    }

    public void publishCdr(
            @NonNull MessageEvent event, String mnoMessageId, UtilsEnum.Module module,
            UtilsEnum.MessageType messageType, UtilsEnum.CdrStatus status, KafkaTemplate<String, String> kafkaTemplate) {
        this.publishCdr(event, mnoMessageId, module, messageType, status, null, kafkaTemplate);
    }

    public void publishCdr(
            @NonNull MessageEvent event, String mnoMessageId, UtilsEnum.Module module,
            UtilsEnum.MessageType messageType, UtilsEnum.CdrStatus status,
            String comment, KafkaTemplate<String, String> kafkaTemplate) {
        UtilsRecords.Cdr cdr = event.toCdr(module, messageType, status, mnoMessageId, comment);
        sendBroadcastStatistics(cdr, kafkaTemplate);
        kafkaTemplate.send(KafkaTopicsConstants.CDR_TOPIC, cdr.toString());
    }

    private void sendBroadcastStatistics(UtilsRecords.Cdr cdr, KafkaTemplate<String, String> kafkaTemplate) {
        if (cdr.broadcastId() > 0 && UtilsEnum.MessageType.MESSAGE.name().equals(cdr.messageType())) {
            Instant instant = Instant.ofEpochMilli(Long.parseLong(cdr.deliveryDate()));
            LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(SMSC_DATETIME_FORMATTER);
            var broadcastStatistic = new UtilsRecords.BroadcastStatistic(
                    cdr.broadcastId(),
                    cdr.messageId(),
                    UtilsEnum.CdrStatus.SUCCESS.toString().equals(cdr.status()) ?
                            BroadcastMessageStatus.SENT.getValue() : BroadcastMessageStatus.FAILED.getValue(),
                    localDateTime.format(formatter),
                    cdr.comment(),
                    cdr.destinationProtocol(),
                    Integer.parseInt(cdr.destinationNetworkId()),
                    cdr.destinationType()
            );
            kafkaTemplate.send(
                    KafkaTopicsConstants.BROADCAST_STATISTIC_TOPIC,
                    broadcastStatistic.toString());
        }
    }
}
