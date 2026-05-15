package com.paicbd.smsc.dto.diameter;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.paicbd.smsc.utils.Converter;
import com.paicbd.smsc.utils.Generated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@Generated
@NoArgsConstructor
@AllArgsConstructor
public class DiameterConfig {
    private Integer id;
    @JsonProperty("network_id")
    private Integer networkId;
    @JsonProperty("mno_id")
    private Integer mnoId;
    @JsonProperty("global_title")
    private String globalTitle;
    private String name;
    private boolean enabled;
    private Parameters parameters;
    private Network network;
    @JsonProperty("local_peer")
    private LocalPeer localPeer;
    @JsonProperty("extension_mode")
    private ConnectionType extensionMode;
    @JsonProperty("split_message")
    private boolean splitMessage;
    @JsonProperty("hss_update_enabled")
    private boolean hssUpdateEnabled;
    @JsonProperty("allowed_traffic")
    private boolean allowedTraffic;

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
