package com.paicbd.smsc.dto.diameter;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class Peer {
    private int id;
    private String name;

    private String uri;
    @JsonProperty("attempt_connect")
    private boolean attemptConnect;
    private int rating;
    private String host;
    private String applications;
    private String ip;
    @JsonProperty("port_range")
    private String portRange;
    @JsonProperty("security_ref")
    private String securityRef;
    @JsonProperty("standby_addresses")
    private String standbyAddresses;
}
