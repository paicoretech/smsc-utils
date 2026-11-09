package com.paicbd.smsc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class RetryProfileData {

    @JsonProperty("active")
    private boolean active;

    @JsonProperty("attempt")
    private Integer attempt;

    @JsonProperty("accumulated_delay_seconds")
    private Long accumulatedDelaySeconds;

    @JsonProperty("reroute_network_id")
    private Integer rerouteNetworkId;

    @JsonProperty("mapping_id")
    private Integer mappingId;

    @JsonProperty("trigger_error_code")
    private Integer triggerErrorCode;

    @JsonProperty("activation_type")
    private String activationType;


}
