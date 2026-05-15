package com.paicbd.smsc.kafka;

import com.paicbd.smsc.utils.Generated;
import lombok.experimental.UtilityClass;

@Generated
@UtilityClass
public class KafkaConsumerConstants {
    public static final String DB_INSERT_DATA_CDR_GROUP_ID = "smsc.db.insert.data";
    public static final String ROUTING_DELIVERY_GROUP_ID = "smsc.routing.delivery";

    public static final String ROUTING_HIGH_MESSAGE_GROUP_ID = "smsc.routing.high.message";
    public static final String ROUTING_MEDIUM_MESSAGE_GROUP_ID = "smsc.routing.medium.message";
    public static final String ROUTING_LOW_MESSAGE_GROUP_ID = "smsc.routing.low.message";

    public static final String RETRIES_HIGH_GROUP_ID = "smsc.high.retries";
    public static final String RETRIES_MEDIUM_GROUP_ID = "smsc.medium.retries";
    public static final String RETRIES_LOW_GROUP_ID = "smsc.low.retries";

    public static final String SMPP_DLR_GROUP_ID = "smsc.smpp.dlr";
    public static final String HTTP_DLR_GROUP_ID = "smsc.http.dlr";

    public static final String HTTP_HIGH_MESSAGE_GROUP_ID_PREFIX = "smsc.http.high.message.";
    public static final String HTTP_MEDIUM_MESSAGE_GROUP_ID_PREFIX = "smsc.http.medium.message.";
    public static final String HTTP_LOW_MESSAGE_GROUP_ID_PREFIX = "smsc.http.low.message.";

    public static final String SMPP_HIGH_MESSAGE_GROUP_ID_PREFIX = "smsc.smpp.high.message.";
    public static final String SMPP_MEDIUM_MESSAGE_GROUP_ID_PREFIX = "smsc.smpp.medium.message.";
    public static final String SMPP_LOW_MESSAGE_GROUP_ID_PREFIX = "smsc.smpp.low.message.";

    public static final String SS7_HIGH_MESSAGE_GROUP_ID_PREFIX = "smsc.ss7.high.message.";
    public static final String SS7_MEDIUM_MESSAGE_GROUP_ID_PREFIX = "smsc.ss7.medium.message.";
    public static final String SS7_LOW_MESSAGE_GROUP_ID_PREFIX = "smsc.ss7.low.message.";

    public static final String DIAMETER_HIGH_MESSAGE_GROUP_ID_PREFIX = "smsc.diameter.high.message.";
    public static final String DIAMETER_MEDIUM_MESSAGE_GROUP_ID_PREFIX = "smsc.diameter.medium.message.";
    public static final String DIAMETER_LOW_MESSAGE_GROUP_ID_PREFIX = "smsc.diameter.low.message.";

    public static final String DIAMETER_HIGH_CHARGING_GROUP_ID = "smsc.diameter.high.charging";
    public static final String DIAMETER_MEDIUM_CHARGING_GROUP_ID = "smsc.diameter.medium.charging";
    public static final String DIAMETER_LOW_CHARGING_GROUP_ID = "smsc.diameter.low.charging";


    public static final String HTTP_PROXY_GROUP_ID_PREFIX = "smsc.http.proxy.message";
    public static final String BACKEND_API_GROUP_ID_PREFIX = "smsc.backend.api";

    public static final String SIP_HIGH_MESSAGE_GROUP_ID_PREFIX = "smsc.sip.high.message.";
    public static final String SIP_MEDIUM_MESSAGE_GROUP_ID_PREFIX = "smsc.sip.medium.message.";
    public static final String SIP_LOW_MESSAGE_GROUP_ID_PREFIX = "smsc.sip.low.message.";
}
