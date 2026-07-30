package com.paicbd.smsc.dto.diameter;

import com.fasterxml.jackson.annotation.JsonProperty;
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
@NoArgsConstructor
@AllArgsConstructor
public class LocalPeer {
    private int id;
    private String uri;
    @JsonProperty("ip_addresses")
    private List<String> ipAddresses;
    private String realm;
    @JsonProperty("vendor_id")
    private int vendorId;
    @JsonProperty("product_name")
    private String productName;
    @JsonProperty("firmware_revision")
    private int firmwareRevision;
    private List<Application> applications;
}
