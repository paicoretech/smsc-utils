package com.paicbd.smsc.dto;

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
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class ErrorCodeMapping {
    @JsonProperty("error_code")
    private int errorCode;

    @JsonProperty("delivery_error_code")
    private int deliveryErrorCode;

    @JsonProperty("delivery_status")
    private String deliveryStatus;
}
