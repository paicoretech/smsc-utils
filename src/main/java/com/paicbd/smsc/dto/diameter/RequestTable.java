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
public class RequestTable {
    int size;
    @JsonProperty("clear_size")
    int clearSize;
}
