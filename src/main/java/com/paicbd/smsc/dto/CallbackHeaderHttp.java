package com.paicbd.smsc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.paicbd.smsc.utils.Generated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Generated
public class CallbackHeaderHttp {
    @JsonProperty("header_name")
    private String headerName;

    @JsonProperty("header_value")
    private String headerValue;
}
