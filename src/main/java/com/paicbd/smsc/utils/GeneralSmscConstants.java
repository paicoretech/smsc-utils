package com.paicbd.smsc.utils;

@Generated
public class GeneralSmscConstants {
    private GeneralSmscConstants() {
    }

    public static final String SMPP_HTTP_GATEWAYS_HASH_NAME = "gateways";
    public static final String SERVICE_PROVIDERS_HASH_NAME = "service_providers";
    public static final String ERROR_CODE_MAPPING_HASH_NAME = "error_code_mapping";
    public static final String SS7_GATEWAYS_HASH_NAME = "ss7_gateways";
    public static final String SS7_SETTINGS_HASH_NAME = "ss7_settings";
    public static final String ROUTING_RULES_HASH_NAME = "routing_rules";
    public static final String CONFIGURATIONS_HASH_NAME = "configurations";
    public static final String GENERAL_SETTINGS_HASH_NAME = "general_settings";
    public static final String DIAMETER_GATEWAYS_HASH_NAME = "diameter_gateways";
    public static final String BALANCE_HANDLER_HASH_NAME = "balance_handler";
    public static final String SMPP_SERVER_CONFIGURATIONS_HASH_NAME = "smpp_server_configurations";
    public static final String SMPP_MESSAGE_PARTS_HASH_NAME = "smpp_message_parts";
    public static final String NETWORK_ERROR_SUFFIX_HASH_NAME = "_absent_subscriber";


    public static final String GENERAL_SETTINGS_SMPP_HTTP_CONFIG_KEY = "smpp_http";
    public static final String GENERAL_SETTINGS_RETRIES_KEY = "smsc_retry";
    public static final String CONFIGURATIONS_CHARGING_KEY = "charging";
    public static final String DIAMETER_PROTOCOL = "DIAMETER";
    public static final String SS7_PROTOCOL = "SS7";
    public static final String HTTP_PROTOCOL = "HTTP";
    public static final String SMPP_PROTOCOL = "SMPP";
    public static final String SIP_PROTOCOL = "SIP";
    public static final String SMSC_DATETIME_FORMATTER = "yyyy-MM-dd HH:mm:ss";

    public static final String MESSAGE_PRIORITY = "message_priority"; // Use for Custom Param in SubmitResult
    public static final String HIGH_PRIORITY = "HIGH";
    public static final String MEDIUM_PRIORITY = "MEDIUM";
    public static final String LOW_PRIORITY = "LOW";

    public static final String GENERAL_SETTINGS_COMMON_KEY = "common_settings";
    public static final String USE_DND_FILTERING = "USE_DND_FILTERING";
    public static final String SMSC_GENERATED_DLR = "smsc_generated_dlr";

    // ss7 for advanced action
    public static final String AUTO_MAP_VERSION = "auto_map_version";
    public static final String HAS_ACTION_ADVANCED_RULES = "has_action_advanced_rules";
    public static final String MAP_VERSION = "map_version";
    public static final String OPERATION_CODE_SRI = "operation_code_sri";
    public static final String OPERATION_CODE_MT = "operation_code_mt";
    public static final String SSN_SMSC_SRI = "ssn_smsc_sri";
    public static final String SSN_HLR_SRI = "ssn_hlr_sri";
    public static final String SSN_MSC_MT = "ssn_msc_mt";
    public static final String SSN_SMSC_MT = "ssn_smsc_mt";
    public static final String SCCP_SOURCE_ADDRESS_SRI = "sccp_source_address_sri";
    public static final String SCCP_DESTINATION_ADDRESS_SRI = "sccp_destination_address_sri";
    public static final String SCCP_SOURCE_ADDRESS_MT = "sccp_source_address_mt";
    public static final String SCCP_DESTINATION_ADDRESS_MT = "sccp_destination_address_mt";
    public static final String CUSTOM_MAP_LAYER_SOURCE_ADDRESS_SRI = "custom_map_layer_source_address_sri";
    public static final String CUSTOM_MAP_LAYER_SOURCE_ADDRESS_MT = "custom_map_layer_source_address_mt";
    public static final String PRIORITY_FLAG_SRI = "priority_flag_sri";
    public static final String APPLICATION_CONTEXT_MT = "application_context_mt";

    // Sip
    public static final String SIP_SETTINGS_HASH_NAME = "sip_settings";
    public static final String SIP_DESTINATION_URI = "SIP_DESTINATION_URI";

    // Multipart message reference type propagated via customParams
    public static final String MSG_REFERENCE_TYPE = "msgReferenceType";
    public static final String MSG_REFERENCE_TYPE_8BIT = "8BIT";
    public static final String MSG_REFERENCE_TYPE_16BIT = "16BIT";
}