package com.paicbd.smsc.utils;

@Generated
public class WebsocketConstants {
    private WebsocketConstants() {}

    //Diameter
    public static final String ADD_OR_UPDATE_CHARGING_DIAMETER_GATEWAY = "/app/diameter/charging/add";
    public static final String ADD_OR_UPDATE_SMS_DIAMETER_GATEWAY = "/app/diameter/sms/add";
    public static final String REMOVE_DIAMETER_GATEWAY = "/app/diameter/sms/remove";
    public static final String START_DIAMETER_GATEWAY = "/app/diameter/startGateway";
    public static final String STOP_DIAMETER_GATEWAY = "/app/diameter/stopGateway";
    public static final String START_PEER_FOR_DIAMETER_GATEWAY = "/app/diameter/startPeer";
    public static final String STOP_PEER_FOR_DIAMETER_GATEWAY = "/app/diameter/stopPeer";

    //Routing Rules
    public static final String UPDATE_ROUTING_RULE = "/app/update/routingRules";
    public static final String DELETE_ROUTING_RULE = "/app/delete/routingRules";

    //SMPP
    public static final String UPDATE_SMPP_SERVER_LISTENER = "/app/smpp-server/listener/update";
    public static final String UPDATE_ERROR_CODE_MAPPING = "/app/updateErrorCodeMapping"; // Receive mno_id as String
    public static final String DELETE_SMPP_SERVICE_PROVIDER = "/app/smpp/serviceProviderDeleted";
    public static final String UPDATE_SMPP_SERVICE_PROVIDER = "/app/smpp/updateServiceProvider";
    public static final String UPDATE_SMPP_GATEWAY = "/app/updateGateway";
    public static final String CONNECT_SMPP_GATEWAY = "/app/connectGateway";
    public static final String STOP_SMPP_GATEWAY = "/app/stopGateway";
    public static final String DELETE_SMPP_GATEWAY = "/app/deleteGateway";

    //HTTP
    public static final String DELETE_HTTP_SERVICE_PROVIDER = "/app/http/serviceProviderDeleted";
    public static final String UPDATE_HTTP_SERVICE_PROVIDER = "/app/http/updateServiceProvider";
    public static final String UPDATE_HTTP_SERVER_HANDLER = "/app/httpUpdateServerHandler";
    public static final String UPDATE_HTTP_GATEWAY = "/app/http/updateGateway";
    public static final String CONNECT_HTTP_GATEWAY = "/app/http/connectGateway";
    public static final String STOP_HTTP_GATEWAY = "/app/http/stopGateway";
    public static final String DELETE_HTTP_GATEWAY = "/app/http/deleteGateway";

    //SIP
    public static final String DELETE_SIP_GATEWAYS_ENDPOINT = "/app/sip/deleteGateway";
    public static final String UPDATE_SIP_GATEWAYS_ENDPOINT = "/app/sip/updateGateway";
    public static final String CONNECT_SIP_GATEWAYS_ENDPOINT = "/app/sip/connectGateway";
    public static final String STOP_SIP_GATEWAYS_ENDPOINT = "/app/sip/stopGateway";

    //HTTP2
    public static final String CONNECT_HTTP2_GATEWAY_ENDPOINT = "/app/http2/connectGateway";
    public static final String STOP_HTTP2_GATEWAY_ENDPOINT = "/app/http2/stopGateway";
    public static final String DELETE_HTTP2_GATEWAYS_ENDPOINT = "/app/http2/deleteGateway";

    //SS7
    public static final String UPDATE_SS7_CONFIG = "/app/ss7/updateGateway";
    public static final String CONNECT_SS7_GATEWAY = "/app/ss7/connectGateway";
    public static final String DELETE_SS7_CONFIG = "/app/ss7/deleteGateway";
    public static final String STOP_SS7_GATEWAY_ENDPOINT = "/app/ss7/stopGateway";
    public static final String START_SS7_ASSOCIATION_ENDPOINT = "/app/ss7/startAssociation";
    public static final String STOP_SS7_ASSOCIATION_ENDPOINT = "/app/ss7/stopAssociation";
    public static final String START_SS7_SOCKET_ENDPOINT = "/app/ss7/startSocket";
    public static final String STOP_SS7_SOCKET_ENDPOINT = "/app/ss7/stopSocket";
    public static final String SS7_HOT_RELOAD_ENDPOINT = "/app/ss7/hotReload";
    public static final String SS7_SETTINGS_UPDATE_ENDPOINT = "/app/ss7/settings/update";

    //General Settings
    public static final String UPDATE_GENERAL_SETTINGS_SMPP_HTTP = "/app/generalSettings";
    public static final String GENERAL_SMSC_RETRY_ENDPOINT = "/app/generalSmsRetry";
    public static final String GENERAL_SETTINGS_DIAMETER_ENDPOINT = "/app/generalSettings/diameter";
    public static final String DELETE_GENERAL_SETTINGS_DIAMETER_ENDPOINT = "/app/generalSettings/delete/diameter";
}
