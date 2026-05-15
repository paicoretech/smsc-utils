package com.paicbd.smsc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.paicbd.smsc.utils.Converter;
import com.paicbd.smsc.utils.Generated;
import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Generated
public class UtilsRecords {
    public record WebSocketConnectionParams(
            boolean isWsEnabled,
            String host,
            int port,
            String path,
            List<String> topicsToSubscribe,
            String headerKey,
            String headerValue,
            int retryInterval,
            String module
    ) {
        @Override
        public String toString() {
            return Converter.valueAsString(this);
        }
    }

    public record OptionalParameter(
            short tag, String value
    ) {
        public OptionalParameter(String tag, String value) {
            this(convertToShort(tag), value);
        }

        public OptionalParameter(Map<String, String> map) {
            this(convertToShort(String.valueOf(map.get("tag"))), String.valueOf(map.get("value")));
        }

        private static short convertToShort(String tag) {
            if (tag.startsWith("0x")) {
                return (short) Integer.parseInt(tag.replace("0x", ""), 16);
            } else {
                return Short.parseShort(tag);
            }
        }
    }

    public record SubmitSmResponseEvent(
            @JsonProperty("hash_id") String hashId,
            @JsonProperty("id") String id,
            @JsonProperty("system_id") String systemId,
            @JsonProperty("submit_sm_id") String submitSmId,
            @JsonProperty("submit_sm_server_id") String submitSmServerId,
            @JsonProperty("origin_protocol") String originProtocol,
            @JsonProperty("origin_network_id") int originNetworkId,
            @JsonProperty("origin_network_type") String originNetworkType,
            @JsonProperty("msg_reference_number") String msgReferenceNumber,
            @JsonProperty("total_segment") Integer totalSegment,
            @JsonProperty("segment_sequence") Integer segmentSequence,
            @JsonProperty("parent_id") String parentId,
            @JsonProperty("dest_network_id") int destNetworkId,
            @JsonProperty("apply_for_refund") boolean applyForRefund,
            @JsonProperty("custom_parameters") Map<String, Object> customParams,
            @JsonProperty("split_for_smsc") boolean splitForSmsc
    ) {

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

    public record JedisConfigParams(
            @Nonnull List<String> redisNodes,
            int maxTotal,
            int maxIdle,
            int minIdle,
            boolean blockWhenExhausted,
            int connectionTimeout,
            int soTimeout,
            int maxAttempts,
            String user,
            String password
    ) {
        @Override
        public String toString() {
            return Converter.valueAsString(this);
        }
    }

    public record Cdr(
            @JsonProperty("record_date") String recordDate,
            @JsonProperty("submit_date") String submitDate,
            @JsonProperty("delivery_date") String deliveryDate,
            @JsonProperty("message_type") String messageType,
            @JsonProperty("message_id") String messageId,
            @JsonProperty("mno_message_id") String mnoMessageId,
            @JsonProperty("origination_protocol") String originationProtocol,
            @JsonProperty("origination_network_id") String originationNetworkId,
            @JsonProperty("origination_network_name") String originationNetworkName,
            @JsonProperty("origination_type") String originationType,
            @JsonProperty("destination_protocol") String destinationProtocol,
            @JsonProperty("destination_network_id") String destinationNetworkId,
            @JsonProperty("destination_network_name") String destinationNetworkName,
            @JsonProperty("destination_type") String destinationType,
            @JsonProperty("routing_id") String routingId,
            @JsonProperty("status") String status,
            @JsonProperty("status_code") String statusCode,
            @JsonProperty("comment") String comment,
            @JsonProperty("dialog_duration") String dialogDuration,
            @JsonProperty("processing_time") String processingTime,
            @JsonProperty("data_coding") String dataCoding,
            @JsonProperty("validity_period") String validityPeriod,
            @JsonProperty("addr_src_digits") String addrSrcDigits,
            @JsonProperty("addr_src_ton") String addrSrcTon,
            @JsonProperty("addr_src_npi") String addrSrcNpi,
            @JsonProperty("addr_dst_digits") String addrDstDigits,
            @JsonProperty("addr_dst_ton") String addrDstTon,
            @JsonProperty("addr_dst_npi") String addrDstNpi,
            @JsonProperty("remote_dialog_id") String remoteDialogId,
            @JsonProperty("local_dialog_id") String localDialogId,
            @JsonProperty("local_spc") String localSpc,
            @JsonProperty("local_ssn") String localSsn,
            @JsonProperty("local_global_title_digits") String localGlobalTitleDigits,
            @JsonProperty("remote_spc") String remoteSpc,
            @JsonProperty("remote_ssn") String remoteSsn,
            @JsonProperty("remote_global_title_digits") String remoteGlobalTitleDigits,
            @JsonProperty("correlation_id") String correlationId,
            @JsonProperty("imsi") String imsi,
            @JsonProperty("nnn_digits") String nnnDigits,
            @JsonProperty("originator_sccp_address") String originatorSccpAddress,
            @JsonProperty("mt_service_center_address") String mtServiceCenterAddress,
            @JsonProperty("message") String message,
            @JsonProperty("esm_class") String esmClass,
            @JsonProperty("udhi") String udhi,
            @JsonProperty("registered_delivery") String registeredDelivery,
            @JsonProperty("msg_reference_number") String msgReferenceNumber,
            @JsonProperty("total_segment") String totalSegment,
            @JsonProperty("segment_sequence") String segmentSequence,
            @JsonProperty("retry_number") String retryNumber,
            @JsonProperty("parent_id") String parentId,
            @JsonProperty("broadcast_id") int broadcastId,
            @JsonProperty("local_translation_type") String localTranslationType,
            @JsonProperty("remote_translation_type") String remoteTranslationType,
            @JsonProperty("message_priority") String messagePriority,
            @JsonProperty("optional_parameters") String optionalParameters
    ) {
        @Override
        public String toString() {
            return Converter.valueAsString(this);
        }
    }

    public record CallbackHeaderHttp(
            @JsonProperty("header_name")
            String headerName,

            @JsonProperty("header_value")
            String headerValue
    ) {
    }

    public record BroadcastStatistic(
            @JsonProperty("broadcast_id") Integer broadcastId,
            @JsonProperty("message_id") String messageId,
            @JsonProperty("status") Integer status,
            @JsonProperty("date") String date,
            @JsonProperty("comment") String comment,
            @JsonProperty("dest_protocol") String destProtocol,
            @JsonProperty("dest_network_id") Integer destNetworkId,
            @JsonProperty("dest_network_type") String destNetworkType
    ) {
        @Override
        public String toString() {
            return Converter.valueAsString(this);
        }
    }

    public record HttpProxyResponse(
            @JsonProperty(value = "message_id") String messageId,
            @JsonProperty(value = "error") boolean error,
            @JsonProperty(value = "error_code") Integer errorCode,
            @JsonProperty(value = "error_message") String errorMessage
    ) {
        @Override
        public String toString() {
            return Converter.valueAsString(this);
        }
    }

    public record CcMccMnc(
            @JsonProperty(value = "country_code") String countryCode,
            @JsonProperty(value = "mcc_mnc") String mccMnc,
            @JsonProperty(value = "smsc") String smsc
    ) {
        @Override
        public String toString() {
            return Converter.valueAsString(this);
        }
    }

    public record ConcatenatedMessageDetails(
            int msgReferenceNumber,
            int totalSegments,
            int segmentSequence,
            String text
    ) {
    }
}
