package com.paicbd.smsc.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.paicbd.smsc.utils.Converter;
import com.paicbd.smsc.utils.EncodingUtils;
import com.paicbd.smsc.utils.GeneralSmscConstants;
import com.paicbd.smsc.utils.ErrorCodes;
import com.paicbd.smsc.utils.SmppUtils;
import com.paicbd.smsc.utils.UtilsEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jsmpp.bean.DeliveryReceipt;
import org.jsmpp.bean.ESMClass;
import org.jsmpp.bean.GSMSpecificFeature;
import org.jsmpp.bean.MessageMode;
import org.jsmpp.bean.MessageType;
import org.jsmpp.bean.OptionalParameter;
import org.jsmpp.util.DeliveryReceiptState;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;


@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class MessageEvent {
    @JsonProperty("id")
    private String id;

    @JsonProperty("message_id")
    private String messageId;

    @JsonProperty("system_id")
    private String systemId;

    @JsonProperty("deliver_sm_id")
    private String deliverSmId;

    @JsonProperty("deliver_sm_server_id")
    private String deliverSmServerId;

    @JsonProperty("command_status")
    private int commandStatus;

    @JsonProperty("sequence_number")
    private int sequenceNumber;

    // NatureAddressIndicator
    @JsonProperty("source_addr_ton")
    private Integer sourceAddrTon;

    // NumberingPlan
    @JsonProperty("source_addr_npi")
    private Integer sourceAddrNpi;

    @JsonProperty("source_addr")
    private String sourceAddr;

    // NatureAddressIndicator
    @JsonProperty("dest_addr_ton")
    private Integer destAddrTon;

    // NumberingPlan
    @JsonProperty("dest_addr_npi")
    private Integer destAddrNpi;

    @JsonProperty("destination_addr")
    private String destinationAddr;

    @JsonProperty("command_id")
    private int commandId;

    @JsonProperty("command_length")
    private int commandLength;

    @JsonProperty("service_type")
    private String serviceType;

    @JsonProperty("schedule_delivery_time")
    private String scheduleDeliveryTime;

    @JsonProperty("protocol_id")
    private byte protocolId;

    @JsonProperty("priority_flag")
    private byte priorityFlag;

    @JsonProperty("replace_if_present")
    private byte replaceIfPresent;

    @JsonProperty("esm_class")
    private Integer esmClass;

    @JsonProperty("validity_period")
    private long validityPeriod;

    @JsonProperty("string_validity_period")
    private String stringValidityPeriod;

    @JsonProperty("registered_delivery")
    private Integer registeredDelivery;

    @JsonProperty("data_coding")
    private Integer dataCoding;

    @JsonProperty("sm_default_msg_id")
    private int smDefaultMsgId;

    @JsonProperty("short_message")
    private String shortMessage;

    @JsonProperty("delivery_receipt")
    private String delReceipt;

    @JsonProperty("status")
    private String status;

    @JsonProperty("error_code")
    private Integer errorCode;

    @JsonProperty("check_submit_sm_response")
    private Boolean checkSubmitSmResponse;

    @JsonProperty("optional_parameters")
    private List<UtilsRecords.OptionalParameter> optionalParameters;

    @JsonProperty("origin_network_type")
    private String originNetworkType;

    @JsonProperty("origin_protocol")
    private String originProtocol;

    @JsonProperty("origin_network_id")
    private int originNetworkId;

    @JsonProperty("origin_network_name")
    private String originNetworkName;

    @JsonProperty("dest_network_type")
    private String destNetworkType;

    @JsonProperty("dest_protocol")
    private String destProtocol;

    @JsonProperty("dest_network_id")
    private int destNetworkId;

    @JsonProperty("dest_network_name")
    private String destNetworkName;

    @JsonProperty("routing_id")
    private int routingId;

    private String msisdn;

    @JsonProperty("address_nature_msisdn")
    private Integer addressNatureMsisdn;

    @JsonProperty("numbering_plan_msisdn")
    private Integer numberingPlanMsisdn;

    @JsonProperty("remote_dialog_id")
    private Long remoteDialogId;

    @JsonProperty("local_dialog_id")
    private Long localDialogId;

    @JsonProperty("sccp_called_party_address_pc")
    private Integer sccpCalledPartyAddressPointCode;

    @JsonProperty("sccp_called_party_address_ssn")
    private Integer sccpCalledPartyAddressSubSystemNumber;

    @JsonProperty("sccp_called_party_address")
    private String sccpCalledPartyAddress;

    @JsonProperty("sccp_calling_party_address_pc")
    private Integer sccpCallingPartyAddressPointCode;

    @JsonProperty("sccp_calling_party_address_ssn")
    private Integer sccpCallingPartyAddressSubSystemNumber;

    @JsonProperty("sccp_calling_party_address")
    private String sccpCallingPartyAddress;

    // GT of the Gateway
    @JsonProperty("global_title")
    private String globalTitle;

    @JsonProperty("global_title_indicator")
    private String globalTitleIndicator;

    @JsonProperty("translation_type")
    private Integer translationType;

    @JsonProperty("smsc_ssn")
    private Integer smscSsn;

    @JsonProperty("hlr_ssn")
    private Integer hlrSsn;

    @JsonProperty("msc_ssn")
    private Integer mscSsn;

    @JsonProperty("map_version")
    private Integer mapVersion;

    @JsonProperty("is_retry")
    private boolean isRetry;

    @JsonProperty("retry_dest_network_id")
    private String retryDestNetworkId;

    @JsonProperty("retry_number")
    private Integer retryNumber;

    @JsonProperty("is_last_retry")
    private boolean isLastRetry;

    @JsonProperty("is_network_notify_error")
    private boolean isNetworkNotifyError;

    @JsonProperty("due_delay")
    private int dueDelay;

    @JsonProperty("accumulated_time")
    private int accumulatedTime;

    @JsonProperty("drop_map_sri")
    private boolean dropMapSri;

    @JsonProperty("network_id_to_map_sri")
    private int networkIdToMapSri;

    @JsonProperty("network_id_to_permanent_failure")
    private int networkIdToPermanentFailure;

    @JsonProperty("drop_temp_failure")
    private boolean dropTempFailure;

    @JsonProperty("network_id_temp_failure")
    private int networkIdTempFailure;

    @JsonProperty("correlation_id")
    private String correlationId;

    @JsonProperty("imsi")
    private String imsi;

    @JsonProperty("network_node_number")
    private String networkNodeNumber;

    @JsonProperty("network_node_number_nature_of_address")
    private Integer networkNodeNumberNatureOfAddress;

    @JsonProperty("network_node_number_numbering_plan")
    private Integer networkNodeNumberNumberingPlan;

    @JsonProperty("mo_message")
    private boolean moMessage;

    @Builder.Default
    @JsonProperty("is_sri_response")
    private boolean sriResponse = false;

    @Builder.Default
    @JsonProperty("check_sri_response")
    private boolean checkSriResponse = false;

    @JsonProperty("msg_reference_number")
    private String msgReferenceNumber;

    @JsonProperty("total_segment")
    private Integer totalSegment;

    @JsonProperty("segment_sequence")
    private Integer segmentSequence;

    @JsonProperty("originator_sccp_address")
    private String originatorSccpAddress;

    @JsonProperty("udh_length")
    private int udhLength;

    @Builder.Default
    @JsonProperty("udh_bytes")
    private byte[] udhBytes = new byte[0];

    @Builder.Default
    @JsonProperty("udh_raw")
    private Set<Udh> udhRaw = new HashSet<>();

    @JsonProperty("parent_id")
    private String parentId;

    @JsonProperty("is_dlr")
    private boolean isDlr;

    @JsonProperty("message_parts")
    private List<MessagePart> messageParts;

    @JsonProperty("split_for_smsc")
    private boolean splitForSmsc = false;

    @JsonIgnore
    @Builder.Default
    private boolean process = true;

    @JsonProperty("broadcast_id")
    private Integer broadcastId;

    @JsonProperty("custom_parameters")
    private Map<String, Object> customParams;

    @JsonProperty("diameter_charging")
    private boolean diameterCharging;

    @Builder.Default
    @JsonProperty("use_proxy")
    private boolean useProxy = false;

    @JsonProperty("apply_for_refund")
    private boolean applyForRefund;

    @JsonProperty("ready_for_refund")
    private boolean readyForRefund;

    @JsonProperty("message_bytes")
    private byte[] messageBytes;

    @JsonProperty("message_length")
    private int messageLength;

    @JsonProperty("local_translation_type")
    private Integer localTranslationType;

    @JsonProperty("remote_translation_type")
    private Integer remoteTranslationType;

    @JsonProperty("api_message_type")
    private UtilsEnum.MessageType apiMessageType;

    @JsonProperty("smsc_message_priority")
    private String smscMessagePriority;

    @JsonProperty("sip_message_supported")
    private boolean sipMessageSupported;

    /**
     * @deprecated
     */
    @Deprecated(forRemoval = true)
    public UtilsRecords.Cdr toCdr(
            UtilsEnum.Module module, UtilsEnum.MessageType messageType, UtilsEnum.CdrStatus cdrStatus) {
        return toCdr(module, messageType, cdrStatus, "", null);
    }

    /**
     * @deprecated
     */
    @Deprecated(forRemoval = true)
    public UtilsRecords.Cdr toCdr(
            UtilsEnum.Module module,
            UtilsEnum.MessageType messageType,
            UtilsEnum.CdrStatus cdrStatus,
            String mnoMessageId) {
        return toCdr(module, messageType, cdrStatus, mnoMessageId, null);
    }

    public UtilsRecords.Cdr toCdr(
            UtilsEnum.Module module,
            UtilsEnum.MessageType messageType,
            UtilsEnum.CdrStatus cdrStatus,
            String mnoMessageId,
            String commentOverride) {

        if (Objects.nonNull(commentOverride)) {
            commentOverride =  Converter.removeLineBreaks(commentOverride);
        }
        return toCdrHelper(module, messageType, cdrStatus, mnoMessageId, commentOverride);
    }

    private UtilsRecords.Cdr toCdrHelper(
            UtilsEnum.Module module,
            UtilsEnum.MessageType messageType,
            UtilsEnum.CdrStatus cdrStatus,
            String mnoMessageId,
            String commentOverride) {

        long deliveryDate = System.currentTimeMillis();
        String comment = commentOverride;

        if (Objects.isNull(comment)) {
            comment = "";
            if (UtilsEnum.MessageType.DELIVER.equals(messageType)) {
                comment = Objects.nonNull(this.status) ? this.status : "";
            }
            if (UtilsEnum.CdrStatus.FAILED.equals(cdrStatus)) {
                Integer subError = Objects.nonNull(this.customParams) ?
                        (Integer) this.customParams.getOrDefault(ErrorCodes.SUB_ERROR_KEY, null) :
                        null;
                comment = ErrorCodes.getErrorDescription(module, this.errorCode, subError);
            }
        }

        long generationDate = System.currentTimeMillis();
        long submitDate = parseSubmitDate(this.id);
        long dialogDuration = deliveryDate - submitDate;
        long processingTime = generationDate - submitDate;

        return new UtilsRecords.Cdr(
                defaultIfNull(String.valueOf(generationDate)),
                defaultIfNull(String.valueOf(submitDate)),
                defaultIfNull(String.valueOf(deliveryDate)),
                defaultIfNull(messageType.name()),
                defaultIfNull(this.messageId),
                defaultIfNull(mnoMessageId),
                defaultIfNull(this.originProtocol),
                defaultIfNull(this.originNetworkId),
                Converter.removeLineBreaks(defaultIfNull(this.originNetworkName)),
                defaultIfNull(this.originNetworkType),
                defaultIfNull(this.destProtocol),
                defaultIfNull(this.destNetworkId),
                Converter.removeLineBreaks(defaultIfNull(this.destNetworkName)),
                defaultIfNull(this.destNetworkType),
                defaultIfNull(Integer.toString(this.routingId)),
                defaultIfNull(cdrStatus.name()),
                defaultIfNull(this.errorCode),
                defaultIfNull(comment),
                defaultIfNull(Long.toString(dialogDuration)),
                defaultIfNull(Long.toString(processingTime)),
                defaultIfNull((this.dataCoding != null) ? this.dataCoding : 0),
                defaultIfNull(this.validityPeriod),
                defaultIfNull(this.sourceAddr),
                defaultIfNull(this.sourceAddrTon),
                defaultIfNull(this.sourceAddrNpi),
                defaultIfNull(this.destinationAddr),
                defaultIfNull(this.destAddrTon),
                defaultIfNull(this.destAddrNpi),
                defaultIfNull(this.remoteDialogId),
                defaultIfNull(this.localDialogId),
                defaultIfNull(this.sccpCallingPartyAddressPointCode),
                defaultIfNull(this.sccpCallingPartyAddressSubSystemNumber),
                defaultIfNull(this.sccpCallingPartyAddress),
                defaultIfNull(this.sccpCalledPartyAddressPointCode),
                defaultIfNull(this.sccpCalledPartyAddressSubSystemNumber),
                defaultIfNull(this.sccpCalledPartyAddress),
                defaultIfNull(this.correlationId),
                defaultIfNull(this.imsi),
                defaultIfNull(this.networkNodeNumber),
                defaultIfNull(this.originatorSccpAddress),
                defaultIfNull(this.globalTitle),
                defaultIfNull(this.getMessageForCdr()),
                defaultIfNull(this.esmClass),
                defaultIfNull((this.udhLength > 0) ? "1" : "0"),
                defaultIfNull(this.registeredDelivery),
                defaultIfNull(this.msgReferenceNumber),
                defaultIfNull(this.totalSegment),
                defaultIfNull(this.segmentSequence),
                defaultIfNull(this.retryNumber),
                defaultIfNull(this.parentId),
                (Objects.isNull(this.broadcastId) ? 0 : this.broadcastId),
                defaultIfNull(this.localTranslationType),
                defaultIfNull(this.remoteTranslationType),
                defaultIfNull(this.smscMessagePriority),
                defaultIfNull(Objects.nonNull(this.optionalParameters) ? Converter.valueAsString(this.optionalParameters.stream()
                        .filter(obj -> obj.tag() != OptionalParameter.Tag.MESSAGE_PAYLOAD.code()).toList()) : null)

        );
    }

    private String defaultIfNull(Object value) {
        return value == null ? "" : value.toString();
    }

    private Long parseSubmitDate(String idEvent) {
        return Long.parseLong(idEvent.split("-")[0]);
    }

    private String getMessageForCdr() {
        if (Objects.isNull(this.shortMessage)) return "";
        return Converter.removeLineBreaks(this.shortMessage);
    }

    public boolean notApplyForLongMessage() {
        return "SP".equalsIgnoreCase(this.destNetworkType) ||
                "HTTP".equalsIgnoreCase(this.getDestProtocol()) ||
                (Objects.nonNull(this.messageParts) && !this.messageParts.isEmpty()) ||
                EncodingUtils.messageContainsInfoAboutConcatenatedSms(this);
    }

    public boolean applyForLongMessage() {
        return "GW".equalsIgnoreCase(this.destNetworkType) &&
                !"HTTP".equalsIgnoreCase(this.getDestProtocol()) &&
                (Objects.isNull(this.messageParts) || this.messageParts.isEmpty()) &&
                !EncodingUtils.messageContainsInfoAboutConcatenatedSms(this);
    }


    /**
     * Create a MessageEvent in deliveryReceipt Format using the originator MessageEvent
     *
     * @param errorCodeMapping The Error code Mapping of the scenario, if errorCodeMapping is null, it will create
     *                         a DeliveryReceiptMessage with status DELIVRD,
     * @param extraInformation A String with extra info that will concat on the short message
     * @return A MessageEvent representing deliveryReceipt.
     */
    public MessageEvent createDeliveryReceiptMessage(ErrorCodeMapping errorCodeMapping, String extraInformation) {
        MessageEvent deliveryReceiptMessageEvent;
        DeliveryReceiptState deliveryReceiptState = DeliveryReceiptState.DELIVRD;
        int error = 0;
        String currentMessageId = ("HTTP".equalsIgnoreCase(this.getOriginProtocol()) && (Objects.nonNull(this.getMessageParts()) && !this.getMessageParts().isEmpty())) ?
                this.getParentId() : this.getMessageId();

        DeliveryReceipt deliveryReceipt = new DeliveryReceipt(currentMessageId, 1, 1,
                new Date(), new Date(), deliveryReceiptState, "000", "");
        ESMClass deliverEsmClass = new ESMClass(MessageMode.DEFAULT, MessageType.SMSC_DEL_RECEIPT, GSMSpecificFeature.DEFAULT);
        deliveryReceiptMessageEvent = Converter.deepCopy(this, MessageEvent.class);
        deliveryReceiptMessageEvent.setUdhBytes(null);
        deliveryReceiptMessageEvent.setUdhLength(0);
        deliveryReceiptMessageEvent.setUdhRaw(null);
        if (errorCodeMapping != null) {
            deliveryReceipt.setSubmitted(1);
            deliveryReceipt.setDelivered(0);
            deliveryReceiptState = UtilsEnum.getDeliverReceiptState(errorCodeMapping.getDeliveryStatus());
            error = errorCodeMapping.getDeliveryErrorCode();
            deliveryReceipt.setFinalStatus(deliveryReceiptState);
            deliveryReceipt.setError(error + "");
            log.debug("Creating deliver_sm with status {} and error {} for submit_sm with id {}", deliveryReceiptState, error, this.getParentId());
        }

        String dlrMessage = Objects.isNull(extraInformation) ? deliveryReceipt.toString() : deliveryReceipt.toString().concat(extraInformation);
        deliveryReceiptMessageEvent.setId(System.currentTimeMillis() + "-" + System.nanoTime());
        deliveryReceiptMessageEvent.setRegisteredDelivery(0);
        deliveryReceiptMessageEvent.setDeliverSmId(this.getMessageId());
        deliveryReceiptMessageEvent.setDlr(true);
        deliveryReceiptMessageEvent.setImsi(null);
        deliveryReceiptMessageEvent.setShortMessage(dlrMessage);
        deliveryReceiptMessageEvent.setDelReceipt(dlrMessage);
        deliveryReceiptMessageEvent.setOptionalParameters(null);
        deliveryReceiptMessageEvent.setMessageBytes(null);
        deliveryReceiptMessageEvent.setOriginNetworkId(this.getDestNetworkId());
        deliveryReceiptMessageEvent.setOriginNetworkName(this.getDestNetworkName());
        deliveryReceiptMessageEvent.setOriginProtocol(this.getDestProtocol());
        deliveryReceiptMessageEvent.setOriginNetworkType(this.getDestNetworkType());
        deliveryReceiptMessageEvent.setDestNetworkId(this.getOriginNetworkId());
        deliveryReceiptMessageEvent.setDestNetworkName(this.getOriginNetworkName());
        deliveryReceiptMessageEvent.setDestProtocol(this.getOriginProtocol());
        deliveryReceiptMessageEvent.setDestNetworkType(this.getOriginNetworkType());
        deliveryReceiptMessageEvent.setSourceAddrTon(this.getDestAddrTon());
        deliveryReceiptMessageEvent.setSourceAddrNpi(this.getDestAddrNpi());
        deliveryReceiptMessageEvent.setSourceAddr(this.getDestinationAddr());
        deliveryReceiptMessageEvent.setDestAddrTon(this.getSourceAddrTon());
        deliveryReceiptMessageEvent.setDestAddrNpi(this.getSourceAddrNpi());
        deliveryReceiptMessageEvent.setDestinationAddr(this.getSourceAddr());
        deliveryReceiptMessageEvent.setCheckSubmitSmResponse(false);
        deliveryReceiptMessageEvent.setEsmClass((int) deliverEsmClass.value());
        deliveryReceiptMessageEvent.setMsisdn(deliveryReceiptMessageEvent.getDestinationAddr());
        deliveryReceiptMessageEvent.setAddressNatureMsisdn(deliveryReceiptMessageEvent.getDestAddrTon());
        deliveryReceiptMessageEvent.setNumberingPlanMsisdn(deliveryReceiptMessageEvent.getDestAddrNpi());
        deliveryReceiptMessageEvent.setStatus(deliveryReceipt.getFinalStatus().name());
        deliveryReceiptMessageEvent.setErrorCode(Integer.parseInt(deliveryReceipt.getError()));
        deliveryReceiptMessageEvent.setRetryNumber(null);
        deliveryReceiptMessageEvent.setMsgReferenceNumber(null);
        deliveryReceiptMessageEvent.setTotalSegment(null);
        deliveryReceiptMessageEvent.setSegmentSequence(null);
        deliveryReceiptMessageEvent.addCustomParam(GeneralSmscConstants.SMSC_GENERATED_DLR, true);
        List<UtilsRecords.OptionalParameter> dlrTLVs = List.of(
                new UtilsRecords.OptionalParameter("0x001e", currentMessageId),
                new UtilsRecords.OptionalParameter("0x0423", EncodingUtils.bytesToHex(SmppUtils.getNetworkErrorCode(error))),
                new UtilsRecords.OptionalParameter("0x0427", (deliveryReceiptState.value() + 1) + ""));
        deliveryReceiptMessageEvent.setOptionalParameters(dlrTLVs);
        log.debug("new deliveryReceipt message event: {}", deliveryReceiptMessageEvent);
        return deliveryReceiptMessageEvent;
    }

    public void addCustomParam(String key, Object value) {
        if (Objects.isNull(this.getCustomParams())) {
            Map<String, Object> params = new HashMap<>();
            params.put(key, value);
            this.setCustomParams(params);
            return;
        }
        this.getCustomParams().put(key, value);
    }

    public void addHost(String value) {
        this.addCustomParam("host", value);
    }

    public Object getFromCustomParam(String key, Object defaultValue) {
        if (Objects.isNull(this.getCustomParams())) {
            return defaultValue;
        }
        return this.getCustomParams().getOrDefault(key, defaultValue);
    }

    public boolean isFinalSegmentForSplitSmsc() {
        boolean hasSegmentInfo = Objects.nonNull(this.totalSegment) && Objects.nonNull(this.segmentSequence);
        boolean isLastSegment = hasSegmentInfo && Objects.equals(this.totalSegment, this.segmentSequence);
        return !this.splitForSmsc || !hasSegmentInfo || isLastSegment;
    }

    @Override
    public String toString() {
        return Converter.valueAsString(this);
    }
}
