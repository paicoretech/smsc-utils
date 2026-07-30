package com.paicbd.smsc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.paicbd.smsc.utils.Converter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BindEvent {
    @JsonProperty("event_type")
    private String eventType;

    @JsonProperty("network_id")
    private int networkId;

    @JsonProperty("system_id")
    private String systemId;

    @JsonProperty("host")
    private String host;

    @JsonProperty("port")
    private int port;

    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("timestamp")
    private long timestamp;

    @Override
    public String toString() {
        return Converter.valueAsString(this);
    }
}
