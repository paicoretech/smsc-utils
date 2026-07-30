package com.paicbd.smsc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.paicbd.smsc.utils.Generated;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Generated
public class CommonVariables {
    private String key;
    private String value;
    @JsonProperty("data_type")
    private String dataType;
}