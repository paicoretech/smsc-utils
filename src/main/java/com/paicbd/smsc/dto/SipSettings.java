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
public class SipSettings {

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

    @JsonProperty("sip_ip")
    private String sipIp;

    @JsonProperty("sip_port")
    private int sipPort;

    @JsonProperty("sip_transport")
    private String sipTransport;

    @JsonProperty("sip_stack_name")
    private String sipStackName;

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


    @JsonProperty("register_auth_enabled")
    private boolean registerAuthEnabled;

    @JsonProperty("register_auth_realm")
    private String registerAuthRealm;

    @JsonProperty("register_auth_qop")
    private String registerAuthQop;

    @JsonProperty("register_auth_algorithm")
    private String registerAuthAlgorithm;


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

    @JsonProperty("sip_register_max_expires")
    private Integer sipRegisterMaxExpires;

    @JsonProperty("sip_ipsmgw_user")
    private String sipIpsmgwUser;

    @JsonProperty("sip_ipsmgw_domain")
    private String sipIpsmgwDomain;

    @JsonProperty("sip_ims_domain")
    private String sipImsDomain;

    @JsonProperty("sip_ims_ccf")
    private String sipImsCcf;

    @JsonProperty("sip_ims_ecf")
    private String sipImsEcf;

    @JsonProperty("sip_subscribe_target_host")
    private String sipSubscribeTargetHost;

    @JsonProperty("sip_subscribe_target_port")
    private Integer sipSubscribeTargetPort;

    @JsonProperty("sip_subscribe_target_transport")
    private String sipSubscribeTargetTransport;

    @JsonProperty("sip_local_via_host")
    private String sipLocalViaHost;

    @Override
    public String toString() {
        return Converter.valueAsString(this);
    }
}
