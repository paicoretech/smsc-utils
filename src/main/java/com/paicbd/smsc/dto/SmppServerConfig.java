package com.paicbd.smsc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.paicbd.smsc.utils.Converter;
import com.paicbd.smsc.utils.Generated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class SmppServerConfig {
    private int id;
    private String name;
    private String ip;
    private int port;
    @JsonProperty("transaction_timer")
    private int transactionTimer;
    @JsonProperty("wait_for_bind")
    private int waitForBind;
    @JsonProperty("processor_degree")
    private int processorDegree;
    @JsonProperty("queue_capacity")
    private int queueCapacity;
    private String status; // could be in STARTED|STOPPED|FAILED
    private int enabled; // could be 0|1|2 (0=stopped, 1=started, 2=deleted)
    @JsonProperty("tls_enabled")
    @Builder.Default
    private boolean tlsEnabled = false;

    @Override
    public String toString() {
        return Converter.valueAsString(this);
    }
}