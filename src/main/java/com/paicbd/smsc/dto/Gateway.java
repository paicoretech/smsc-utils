package com.paicbd.smsc.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.paicbd.smsc.interpreter.PayloadMapper;
import com.paicbd.smsc.utils.Converter;
import com.paicbd.smsc.utils.Generated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@Generated
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Gateway {
    @JsonProperty("network_id")
    private int networkId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("system_id")
    private String systemId;

    @JsonProperty("password")
    private String password;

    @JsonProperty("ip")
    private String ip;

    @JsonProperty("port")
    private int port;

    @JsonProperty("bind_type")
    private String bindType;

    @JsonProperty("system_type")
    private String systemType;

    @JsonProperty("interface_version")
    private String interfaceVersion;

    @JsonProperty("sessions_number")
    private int sessionsNumber;

    @JsonProperty("address_ton")
    private int addressTON;

    @JsonProperty("address_npi")
    private int addressNPI;

    @JsonProperty("address_range")
    private String addressRange;

    @JsonProperty("tps")
    private int tps;

    @JsonProperty("status")
    private String status;

    @JsonProperty("enabled")
    private int enabled;

    @JsonProperty("enquire_link_period")
    private int enquireLinkPeriod;

    @JsonProperty("enquire_link_timeout")
    private int enquireLinkTimeout;

    @JsonProperty("request_dlr")
    private Integer requestDLR;

    @JsonProperty("no_retry_error_code")
    private String noRetryErrorCode;

    @JsonProperty("retry_alternate_destination_error_code")
    private String retryAlternateDestinationErrorCode;

    @JsonProperty("bind_timeout")
    private int bindTimeout;

    @JsonProperty("bind_retry_period")
    private int bindRetryPeriod;

    @JsonProperty("pdu_timeout")
    private int pduTimeout;

    @JsonProperty("pdu_degree")
    private int pduProcessorDegree;

    @JsonProperty("thread_pool_size")
    private int threadPoolSize;

    @JsonProperty("mno_id")
    private int mno;

    @JsonProperty("tlv_message_receipt_id")
    private boolean tlvMessageReceiptId;

    @JsonProperty("message_id_decimal_format")
    private boolean messageIdDecimalFormat;

    @Builder.Default
    @JsonProperty("active_sessions_numbers")
    private int successSession = 0;

    @JsonProperty("protocol")
    private String protocol;

    @JsonProperty("auto_retry_error_code")
    private String autoRetryErrorCode;

    @Builder.Default
    @JsonProperty("encoding_iso88591")
    private int encodingIso88591 = 3;

    @Builder.Default
    @JsonProperty("encoding_gsm7")
    private int encodingGsm7 = 0;

    @Builder.Default
    @JsonProperty("encoding_ucs2")
    private int encodingUcs2 = 2;

    @JsonProperty("split_message")
    private boolean splitMessage;

    @JsonProperty("split_smpp_type")
    private String splitSmppType;

    @Builder.Default
    @JsonProperty("tls_enabled")
    private boolean tlsEnabled = false;

    private List<PayloadMapper> interpreter;

    @JsonProperty("authentication_types")
    private String authenticationTypes;

    @Builder.Default
    @JsonProperty("header_security_name")
    private String headerSecurityName = "Authorization";

    @JsonProperty("user_name")
    private String userName;

    @JsonProperty("passwd")
    private String passwd;

    @JsonProperty("token")
    private String token;

    @JsonProperty("messages_per_second")
    private int messagesPerSecond;

    @JsonProperty("messages_per_second_high")
    private int messagesPerSecondHigh;

    @JsonProperty("messages_per_second_medium")
    private int messagesPerSecondMedium;

    @JsonProperty("messages_per_second_low")
    private int messagesPerSecondLow;

    @Override
    public String toString() {
        return Converter.valueAsString(this);
    }
}
