package com.paicbd.smsc.dto;

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
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class GeneralSettings {
    private int id;

    @JsonProperty("validity_period")
    private int validityPeriod;

    @JsonProperty("max_validity_period")
    private  int maxValidityPeriod;

    @JsonProperty("source_addr_ton")
    private int sourceAddrTon;

    @JsonProperty("source_addr_npi")
    private int sourceAddrNpi;

    @JsonProperty("dest_addr_ton")
    private int destAddrTon;

    @JsonProperty("dest_addr_npi")
    private int destAddrNpi;

    @JsonProperty("encoding_iso88591")
    private int encodingIso88591;

    @JsonProperty("encoding_gsm7")
    private int encodingGsm7;

    @JsonProperty("encoding_ucs2")
    private int encodingUcs2;

    @Override
    public String toString() {
        return Converter.valueAsString(this);
    }
}
