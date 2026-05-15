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
public class Application {
    @JsonProperty("vendor_id")
    private int vendorId;
    @JsonProperty("auth_appl_id")
    private int authApplId;
    @JsonProperty("acct_appl_id")
    private int acctApplId;
}
