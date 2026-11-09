package com.paicbd.smsc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.paicbd.smsc.utils.Converter;
import com.paicbd.smsc.utils.Generated;
import com.paicbd.smsc.utils.UtilsEnum;
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
public class Ss7Settings {
    @JsonProperty("name")
    private String name;

    @JsonProperty("enabled")
    private  Integer enabled;

    @JsonProperty("status")
    private String status;

    @JsonProperty("protocol")
    private String protocol;

    @JsonProperty("network_id")
    private Integer networkId;

    @JsonProperty("mno_id")
    private Integer mnoId;

    @JsonProperty("global_title")
    private String globalTitle;

    @JsonProperty("global_title_indicator")
    private UtilsEnum.GlobalTitleIndicator globalTitleIndicator;

    @JsonProperty("translation_type")
    private int translationType;

    @JsonProperty("smsc_ssn")
    private int smscSsn;

    @JsonProperty("hlr_ssn")
    private int hlrSsn;

    @JsonProperty("msc_ssn")
    private int mscSsn;

    @JsonProperty("map_version")
    private int mapVersion;

    @Builder.Default
    @JsonProperty("report_sm_delivery_status")
    private boolean reportSmDeliveryStatus = true;

    @JsonProperty("auto_retry_error_code")
    private String autoRetryErrorCode;

    @JsonProperty("no_retry_error_code")
    private String noRetryErrorCode;

    @JsonProperty("retry_alternate_destination_error_code")
    private String retryAlternateDestinationErrorCode;

    @JsonProperty("split_message")
    private boolean splitMessage;

    @JsonProperty("api_enabled")
    private boolean apiEnabled;

    @JsonProperty("app_token")
    private String appToken;

    @JsonProperty("hss_update_enabled")
    private boolean hssUpdateEnabled;

    @JsonProperty("allowed_traffic")
    private boolean allowedTraffic;

    @JsonProperty("allowed_ussi")
    private boolean allowedUssi;

    @JsonProperty("sip_network_id")
    private Integer sipNetworkId;


    @Override
    public String toString() {
        return Converter.valueAsString(this);
    }
}
