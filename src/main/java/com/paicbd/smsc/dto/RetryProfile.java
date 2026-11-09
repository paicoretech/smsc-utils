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
public class RetryProfile {
    @JsonProperty("profile_id")
    private Integer profileId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("recovery_profile_id")
    private Integer recoveryProfileId;

    @JsonProperty("reroute_network_id")
    private Integer rerouteNetworkId;

    @JsonProperty("max_due_delay_seconds")
    private int maxDueDelaySeconds;

    @JsonProperty("period_r_seconds")
    private Integer periodRSeconds;

    @Builder.Default
    @JsonProperty("steps")
    private List<RetryProfileStep> steps = new ArrayList<>();

    @JsonProperty("action")
    private RetryProfileAction action;

    @JsonProperty("has_actions")
    private boolean hasActions;

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
    public static class RetryProfileStep {
        @JsonProperty("id")
        private Integer id;

        @JsonProperty("step_number")
        private int stepNumber;

        @JsonProperty("attempts")
        private int attempts;

        @JsonProperty("delay_seconds")
        private int delaySeconds;
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Generated
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RetryProfileAction {
        @JsonProperty("id")
        private Integer id;

        @JsonProperty("new_source_address")
        private String newSourceAddress;

        @JsonProperty("new_destination_address")
        private String newDestinationAddress;

        @JsonProperty("remove_source_address_prefix")
        private String removeSourceAddressPrefix;

        @JsonProperty("add_source_address_prefix")
        private String addSourceAddressPrefix;

        @JsonProperty("remove_destination_address_prefix")
        private String removeDestinationAddressPrefix;

        @JsonProperty("add_destination_address_prefix")
        private String addDestinationAddressPrefix;

        @JsonProperty("message_priority_staggered_enabled")
        private boolean messagePriorityStaggeredEnabled;

        @JsonProperty("override_message_priority")
        private String overrideMessagePriority;
    }
}
