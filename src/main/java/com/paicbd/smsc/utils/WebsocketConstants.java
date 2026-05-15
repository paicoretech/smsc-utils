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

    //SMPP
    public static final String UPDATE_SMPP_SERVER_LISTENER = "/app/smpp-server/listener/update";
    public static final String UPDATE_ERROR_CODE_MAPPING = "/app/updateErrorCodeMapping"; // Receive mno_id as String
    public static final String DELETE_SMPP_SERVICE_PROVIDER = "/app/smpp/serviceProviderDeleted";
    public static final String UPDATE_SMPP_SERVICE_PROVIDER = "/app/smpp/updateServiceProvider";

    //HTTP
    public static final String DELETE_HTTP_SERVICE_PROVIDER = "/app/http/serviceProviderDeleted";
    public static final String UPDATE_HTTP_SERVICE_PROVIDER = "/app/http/updateServiceProvider";
    public static final String UPDATE_HTTP_SERVER_HANDLER = "/app/httpUpdateServerHandler";

    //General Settings
    public static final String UPDATE_GENERAL_SETTINGS_SMPP_HTTP = "/app/generalSettings";


}

