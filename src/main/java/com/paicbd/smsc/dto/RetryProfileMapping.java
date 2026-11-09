package com.paicbd.smsc.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.paicbd.smsc.utils.Converter;
import com.paicbd.smsc.utils.Generated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Generated
@JsonIgnoreProperties(ignoreUnknown = true)
public class RetryProfileMapping {
    @JsonProperty("id")
    private Integer id;

    @JsonProperty("origin_network_id")
    private Integer originNetworkId;

    @JsonProperty("destination_network_id")
    private Integer destinationNetworkId;

    @JsonProperty("error_code")
    private String errorCode;

    @JsonProperty("profile_id")
    private Integer profileId;

    @JsonProperty("origin_protocol")
    private String originProtocol;

    @JsonProperty("destination_protocol")
    private String destinationProtocol;

    @Builder.Default
    @JsonProperty("retry_mapping_conditions")
    private List<RetryMappingCondition> retryMappingConditions = new ArrayList<>();

    @Override
    public String toString() {
        return Converter.valueAsString(this);
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Generated
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RetryMappingCondition {
        @JsonProperty("id")
        private Integer id;

        @JsonProperty("type")
        private String type;

        @JsonProperty("field")
        private String field;

        @JsonProperty("operator")
        private String operator;

        @JsonProperty("value")
        private String value;
    }
}
