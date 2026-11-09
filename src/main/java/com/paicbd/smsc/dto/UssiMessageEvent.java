package com.paicbd.smsc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.paicbd.smsc.utils.Converter;
import com.paicbd.smsc.utils.ErrorCodes;
import com.paicbd.smsc.utils.UtilsEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class UssiMessageEvent {
    @JsonProperty("id")
    private String id;

    @JsonProperty("parent_id")
    private String parentId;

    @JsonProperty("short_code")
    private String shortCode;

    @JsonProperty("msisdn")
    private String msisdn;

    @JsonProperty("short_message")
    private String shortMessage;

    @JsonProperty("data_coding")
    private Integer dataCoding;

    @JsonProperty("tcap_message_type")
    private String tcapMessageType;

    @JsonProperty("invoke_id")
    private long invokeId;

    @JsonProperty("origin_network_id")
    private int originNetworkId;

    @JsonProperty("origin_protocol")
    private String originProtocol;

    @JsonProperty("dest_network_id")
    private int destNetworkId;

    @JsonProperty("dest_protocol")
    private String destProtocol;

    @JsonProperty("global_title")
    private String globalTitle;

    @JsonProperty("global_title_indicator")
    private String globalTitleIndicator;

    @JsonProperty("translation_type")
    private Integer translationType;

    @JsonProperty("ussd_ssn")
    private Integer ussdSsn;

    @JsonProperty("hlr_ssn")
    private Integer hlrSsn;

    @JsonProperty("msc_ssn")
    private Integer mscSsn;

    @JsonProperty("events")
    private List<String> events;

    @JsonProperty("error_code")
    private Integer errorCode;

    @Override
    public String toString() {
        return Converter.valueAsString(this);
    }

    public UtilsRecords.CdrUssi toCdr(
            UtilsEnum.Module module,
            UtilsEnum.CdrStatus cdrStatus) {
        return toCdrHelper(module, cdrStatus);
    }

    public UssiMessageEvent createResponseMessage() {
        return UssiMessageEvent.builder()
                .id(this.id)
                .parentId(parentId)
                .shortCode(this.shortCode)
                .msisdn(this.msisdn)
                .shortMessage(this.shortMessage)
                .dataCoding(this.dataCoding)
                .tcapMessageType(this.tcapMessageType)
                .invokeId(this.invokeId)
                .originNetworkId(this.destNetworkId)
                .originProtocol(this.destProtocol)
                .destNetworkId(this.originNetworkId)
                .destProtocol(this.originProtocol)
                .globalTitle(this.globalTitle)
                .globalTitleIndicator(this.globalTitleIndicator)
                .translationType(this.translationType)
                .ussdSsn(this.ussdSsn)
                .hlrSsn(this.hlrSsn)
                .mscSsn(this.mscSsn)
                .errorCode(this.errorCode)
                .events(this.events).build();
    }

    private UtilsRecords.CdrUssi toCdrHelper(
            UtilsEnum.Module module,
            UtilsEnum.CdrStatus cdrStatus) {
        long deliveryDate = System.currentTimeMillis();
        String comment = "";

        if (UtilsEnum.CdrStatus.FAILED.equals(cdrStatus)) {
            comment = ErrorCodes.getErrorDescription(module, this.errorCode, null);
        }

        long generationDate = System.currentTimeMillis();
        long submitDate = parseSubmitDate(this.parentId);
        long dialogDuration = deliveryDate - submitDate;
        long processingTime = generationDate - submitDate;

        return new UtilsRecords.CdrUssi(
                defaultIfNull(String.valueOf(generationDate)),
                defaultIfNull(String.valueOf(submitDate)),
                defaultIfNull(String.valueOf(deliveryDate)),
                defaultIfNull(this.id),
                defaultIfNull(this.originProtocol),
                defaultIfNull(this.originNetworkId),
                defaultIfNull(this.destProtocol),
                defaultIfNull(this.destNetworkId),
                defaultIfNull(cdrStatus.name()),
                defaultIfNull(comment),
                defaultIfNull(Long.toString(dialogDuration)),
                defaultIfNull(Long.toString(processingTime)),
                defaultIfNull((this.dataCoding != null) ? this.dataCoding : 0),
                defaultIfNull(this.globalTitle),
                defaultIfNull(this.globalTitleIndicator),
                defaultIfNull(this.translationType),
                defaultIfNull(this.shortCode),
                defaultIfNull(this.msisdn),
                defaultIfNull(this.getMessageForCdr()),
                defaultIfNull(this.tcapMessageType),
                defaultIfNull(this.invokeId),
                defaultIfNull(this.ussdSsn),
                defaultIfNull(this.hlrSsn),
                defaultIfNull(this.mscSsn),
                defaultIfNull(this.parentId),
                Converter.listToSeparatedString(events, ","),
                defaultIfNull(this.errorCode)
        );
    }

    private String defaultIfNull(Object value) {
        return value == null ? "" : value.toString();
    }

    private Long parseSubmitDate(String idEvent) {
        return Long.parseLong(idEvent.split("-")[0]);
    }

    private String getMessageForCdr() {
        String shortMessageFromEvents = Converter.listToSeparatedString(this.events, ",");
        if (Objects.isNull(shortMessageFromEvents)) return "";
        return Converter.removeLineBreaks(shortMessageFromEvents);
    }
}
