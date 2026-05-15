package com.paicbd.smsc.dto;

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
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class RoutingRule {
    @JsonProperty("id")
    private int id;

    @JsonProperty("origin_network_id")
    private int originNetworkId;

    @JsonProperty("origin_network_type")
    private String originNetworkType;

    @JsonProperty("regex_source_addr")
    private String originRegexSourceAddr;

    @JsonProperty("regex_source_addr_ton")
    private String originRegexSourceAddrTon;

    @JsonProperty("regex_source_addr_npi")
    private String originRegexSourceAddrNpi;

    @JsonProperty("regex_destination_addr")
    private String originRegexDestinationAddr;

    @JsonProperty("regex_dest_addr_ton")
    private String originRegexDestAddrTon;

    @JsonProperty("regex_dest_addr_npi")
    private String originRegexDestAddrNpi;

    @JsonProperty("regex_imsi_digits_mask")
    private String regexImsiDigitsMask;

    @JsonProperty("regex_network_node_number")
    private String regexNetworkNodeNumber;

    @JsonProperty("regex_calling_party_address")
    private String regexCallingPartyAddress;

    @JsonProperty("destination")
    private List<Destination> destination;

    @JsonProperty("new_source_addr")
    private String newSourceAddr;

    @JsonProperty("new_source_addr_ton")
    private int newSourceAddrTon;

    @JsonProperty("new_source_addr_npi")
    private int newSourceAddrNpi;

    @JsonProperty("new_destination_addr")
    private String newDestinationAddr;

    @JsonProperty("new_dest_addr_ton")
    private int newDestAddrTon;

    @JsonProperty("new_dest_addr_npi")
    private int newDestAddrNpi;

    @JsonProperty("origin_protocol")
    private String originProtocol;

    @JsonProperty("add_source_addr_prefix")
    private String addSourceAddrPrefix;

    @JsonProperty("remove_source_addr_prefix")
    private String removeSourceAddrPrefix;

    @JsonProperty("add_dest_addr_prefix")
    private String addDestAddrPrefix;

    @JsonProperty("remove_dest_addr_prefix")
    private String removeDestAddrPrefix;

    @JsonProperty("new_gt_sccp_addr")
    private String newGtSccpAddr;

    @Builder.Default
    @JsonProperty("drop_map_sri")
    private boolean dropMapSri = false;

    @JsonProperty("network_id_to_map_sri")
    private int networkIdToMapSri;

    @JsonProperty("network_id_to_permanent_failure")
    private int networkIdToPermanentFailure;

    @Builder.Default
    @JsonProperty("drop_temp_failure")
    private boolean dropTempFailure = false;

    @JsonProperty("network_id_temp_failure")
    private int networkIdTempFailure;

    @JsonProperty("custom_params_matcher")
    private List<CustomParamMatcher> customParamsMatcher;

    @Builder.Default
    @JsonProperty("has_filter_rules")
    private boolean hasFilterRules = false;

    @Builder.Default
    @JsonProperty("has_action_rules")
    private boolean hasActionRules = false;

    @Builder.Default
    @JsonProperty("is_sri_response")
    private boolean sriResponse = false;

    @Builder.Default
    @JsonProperty("check_sri_response")
    private boolean checkSriResponse = false;

    @JsonProperty("regex_short_message")
    private String regexShortMessage;

    @JsonProperty("new_short_message")
    private String newShortMessage;

    @Builder.Default
    @JsonProperty("diameter_charging")
    private boolean diameterCharging = false;

    @Builder.Default
    @JsonProperty("apply_for_refund")
    private boolean applyForRefund = false;

    @Builder.Default
    @JsonProperty("auto_map_version")
    private boolean autoMapVersion = true;

    @Builder.Default
    @JsonProperty("has_action_advanced_rules")
    private boolean hasActionAdvancedRules = false;

    @JsonProperty("action_advanced")
    private ActionAdvanced actionAdvanced;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Destination {
        @JsonProperty("priority")
        private int priority;
        @JsonProperty("network_id")
        private int networkId;
        @JsonProperty("dest_protocol")
        private String protocol;
        @JsonProperty("network_type")
        private String networkType;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CustomParamMatcher {
        @JsonProperty("property_name")
        private String propertyName;

        @JsonProperty("value_matcher")
        private Object valueMatcher;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ActionAdvanced {
        @JsonProperty("map_version")
        private Integer mapVersion = -1;

        @JsonProperty("operation_code_sri")
        private Integer operationCodeSri = -1;

        @JsonProperty("operation_code_mt")
        private Integer operationCodeMt = -1;

        @JsonProperty("ssn_smsc_sri")
        private Integer ssnSmscSri = -1;

        @JsonProperty("ssn_hlr_sri")
        private Integer ssnHlrSri = -1;

        @JsonProperty("ssn_msc_mt")
        private Integer ssnMscMt = -1;

        @JsonProperty("ssn_smsc_mt")
        private Integer ssnSmscMt = -1;

        @JsonProperty("sccp_source_address_sri")
        private String sccpSourceAddressSri = "";

        @JsonProperty("sccp_source_address_mt")
        private String sccpSourceAddressMt = "";

        @JsonProperty("sccp_destination_address_mt")
        private String sccpDestinationAddressMt = "";

        @JsonProperty("custom_map_layer_source_address_sri")
        private String customMapLayerSourceAddressSri = "";

        @JsonProperty("custom_map_layer_source_address_mt")
        private String customMapLayerSourceAddressMt = "";

        @JsonProperty("priority_flag_sri")
        private boolean priorityFlagSri = true;

        @JsonProperty("application_context_mt")
        private String applicationContextMt = "";
    }
}
