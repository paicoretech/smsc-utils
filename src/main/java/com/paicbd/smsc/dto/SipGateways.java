package com.paicbd.smsc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.paicbd.smsc.utils.Converter;
import com.paicbd.smsc.utils.Generated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@Slf4j
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class SipGateways {

    @JsonProperty("name")
    private String name;

    @JsonProperty("enabled")
    private Integer enabled;

    @JsonProperty("status")
    private String status;

    @JsonProperty("protocol")
    private String protocol;

    @JsonProperty("network_id")
    private Integer networkId;

    @JsonProperty("mno_id")
    private Integer mnoId;

    @JsonProperty("ip_address")
    private String ipAddress;

    @JsonProperty("port")
    private int port;

    @JsonProperty("transport")
    private String transport;

    @JsonProperty("messages_per_second")
    private int messagesPerSecond;

    @JsonProperty("messages_per_second_high")
    private int messagesPerSecondHigh;

    @JsonProperty("messages_per_second_medium")
    private int messagesPerSecondMedium;

    @JsonProperty("messages_per_second_low")
    private int messagesPerSecondLow;

    @JsonProperty("transaction_timeout")
    private int transactionTimeout;

    @JsonProperty("retransmission_base_interval_ms")
    private int retransmissionBaseIntervalMs;

    @JsonProperty("retransmission_max_interval_ms")
    private int retransmissionMaxIntervalMs;

    @JsonProperty("network_timeout_ms")
    private int networkTimeoutMs;

    @JsonProperty("thread_pool_size")
    private int threadPoolSize;

    @JsonProperty("retransmission_filter")
    private boolean retransmissionFilter;

    @JsonProperty("max_message_size")
    private int maxMessageSize;

    @JsonProperty("receive_udp_buffer_size")
    private int receiveUdpBufferSize;

    @JsonProperty("send_udp_buffer_size")
    private int sendUdpBufferSize;

    @JsonProperty("aggressive_cleanup")
    private boolean aggressiveCleanup;

    @JsonProperty("auto_retry_error_code")
    private String autoRetryErrorCode;

    @JsonProperty("no_retry_error_code")
    private String noRetryErrorCode;

    @JsonProperty("retry_alternate_destination_error_code")
    private String retryAlternateDestinationErrorCode;

    @JsonProperty("routing_enable_ss7")
    private boolean routingEnableSs7;

    @JsonProperty("routing_enable_diameter")
    private boolean routingEnableDiameter;

    @JsonProperty("routing_registration_traffic_ss7_gateway_id")
    private Integer routingRegistrationTrafficSs7GatewayId;

    @JsonProperty("routing_registration_traffic_diameter_gateway_id")
    private Integer routingRegistrationTrafficDiameterGatewayId;

    @JsonProperty("routing_ussi_traffic_ss7_gateway_id")
    private Integer routingUssiTrafficSs7GatewayId;

    @JsonProperty("register_max_expires")
    private Integer registerMaxExpires;

    @JsonProperty("ipsmgw_user")
    private String ipsmgwUser;

    @JsonProperty("ipsmgw_domain")
    private String ipsmgwDomain;

    @JsonProperty("ims_domain")
    private String imsDomain;

    @JsonProperty("ims_ccf")
    private String imsCcf;

    @JsonProperty("ims_ecf")
    private String imsEcf;

    @JsonProperty("subscribe_target_host")
    private String subscribeTargetHost;

    @JsonProperty("subscribe_target_port")
    private Integer subscribeTargetPort;

    @JsonProperty("subscribe_target_transport")
    private String subscribeTargetTransport;

    @JsonProperty("local_via_host")
    private String localViaHost;

    @JsonProperty("global_title")
    private String globalTitle;

    @JsonProperty("ussi_default_datacoding_id")
    private Integer ussiDefaultDatacodingId;

    @JsonProperty("split_message")
    private boolean splitMessage;

    @Override
    public String toString() {
        return Converter.valueAsString(this);
    }
}
