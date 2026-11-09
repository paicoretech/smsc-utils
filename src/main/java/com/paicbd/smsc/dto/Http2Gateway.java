package com.paicbd.smsc.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.paicbd.smsc.utils.Converter;
import com.paicbd.smsc.utils.GeneralSmscConstants;
import com.paicbd.smsc.utils.Generated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
@Generated
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Http2Gateway {

    @JsonProperty("network_id")
    private int networkId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("nf_instance_id")
    private UUID nfInstanceId;

    @JsonProperty("external_id")
    private String externalId;

    @JsonProperty("status")
    private String status;

    @Builder.Default
    @JsonProperty("protocol")
    private String protocol = GeneralSmscConstants.HTTP2_PROTOCOL;

    @JsonProperty("host")
    private String host;

    @JsonProperty("mno_id")
    private int mnoId;

    @JsonProperty("enabled")
    private int enabled;

    @JsonProperty("multipart_parsing_enabled")
    private boolean multipartParsingEnabled;

    @JsonProperty("binary_sms_enabled")
    private boolean binarySmsEnabled;

    @JsonProperty("max_message_size")
    private int maxMessageSize;

    @JsonProperty("default_data_coding_id")
    private Integer defaultDataCodingId;

    @JsonProperty("request_timeout_ms")
    private int requestTimeoutMs;

    @JsonProperty("connection_timeout_ms")
    private int connectionTimeoutMs;

    @JsonProperty("idle_timeout_ms")
    private int idleTimeoutMs;

    @JsonProperty("max_response_time_ms")
    private int maxResponseTimeMs;

    @JsonProperty("thread_pool_size")
    private int threadPoolSize;

    @JsonProperty("messages_per_second")
    private int messagesPerSecond;

    @JsonProperty("messages_per_second_high")
    private int messagesPerSecondHigh;

    @JsonProperty("messages_per_second_medium")
    private int messagesPerSecondMedium;

    @JsonProperty("messages_per_second_low")
    private int messagesPerSecondLow;

    @JsonProperty("rate_limiting_enabled")
    private boolean rateLimitingEnabled;

    @JsonProperty("request_dlr")
    private int requestDlr;

    @JsonProperty("split_message")
    private boolean splitMessage;

    @JsonProperty("no_retry_error_code")
    private String noRetryErrorCode;

    @JsonProperty("auto_retry_error_code")
    private String autoRetryErrorCode;

    @JsonProperty("retry_alternate_destination_error_code")
    private String retryAlternateDestinationErrorCode;

    @JsonProperty("incoming_security")
    private Http2GatewayIncomingSecurity incomingSecurity;

    @JsonProperty("nrf_security")
    private Http2GatewayNrfSecurity nrfSecurity;

    @JsonProperty("outbound_security")
    private Http2GatewayOutboundSecurity outboundSecurity;

    @JsonProperty("udm_security")
    private Http2GatewayUDMSecurity udmSecurity;

    @Override
    public String toString() {
        return Converter.valueAsString(this);
    }
}