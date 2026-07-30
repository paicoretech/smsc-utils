package com.paicbd.smsc.kafka;

import com.paicbd.smsc.utils.Generated;
import lombok.experimental.UtilityClass;

@Generated
@UtilityClass
public class KafkaTopicsConstants {
    public static final String CDR_TOPIC = "cdr";
    public static final String BROADCAST_STATISTIC_TOPIC = "broadcast.statistic";

    public static final String PRE_MESSAGE_HIGH_TOPIC = "pre.message.high";
    public static final String PRE_MESSAGE_MEDIUM_TOPIC = "pre.message.medium";
    public static final String PRE_MESSAGE_LOW_TOPIC = "pre.message.low";

    public static final String PRE_DELIVER_TOPIC = "pre.deliver";

    public static final String HTTP_HIGH_MESSAGE_TOPIC_SUFFIX = ".http.sms.high";
    public static final String HTTP_MEDIUM_MESSAGE_TOPIC_SUFFIX = ".http.sms.medium";
    public static final String HTTP_LOW_MESSAGE_TOPIC_SUFFIX = ".http.sms.low";

    public static final String SMPP_HIGH_MESSAGE_TOPIC_SUFFIX = ".smpp.sms.high";
    public static final String SMPP_MEDIUM_MESSAGE_TOPIC_SUFFIX = ".smpp.sms.medium";
    public static final String SMPP_LOW_MESSAGE_TOPIC_SUFFIX = ".smpp.sms.low";

    public static final String SS7_HIGH_MESSAGE_TOPIC_SUFFIX = ".ss7.sms.high";
    public static final String SS7_MEDIUM_MESSAGE_TOPIC_SUFFIX = ".ss7.sms.medium";
    public static final String SS7_LOW_MESSAGE_TOPIC_SUFFIX = ".ss7.sms.low";

    public static final String DIAMETER_HIGH_MESSAGE_TOPIC_SUFFIX = ".diameter.sms.high";
    public static final String DIAMETER_MEDIUM_MESSAGE_TOPIC_SUFFIX = ".diameter.sms.medium";
    public static final String DIAMETER_LOW_MESSAGE_TOPIC_SUFFIX = ".diameter.sms.low";

    public static final String CHARGING_HIGH_MESSAGE_TOPIC = "charging.sms.high";
    public static final String CHARGING_MEDIUM_MESSAGE_TOPIC = "charging.sms.medium";
    public static final String CHARGING_LOW_MESSAGE_TOPIC = "charging.sms.low";

    public static final String SIP_HIGH_MESSAGE_TOPIC_SUFFIX = ".sip.sms.high";
    public static final String SIP_MEDIUM_MESSAGE_TOPIC_SUFFIX = ".sip.sms.medium";
    public static final String SIP_LOW_MESSAGE_TOPIC_SUFFIX = ".sip.sms.low";

    public static final String RETRIES_HIGH_TOPIC = "retries.sms.high";
    public static final String RETRIES_MEDIUM_TOPIC = "retries.sms.medium";
    public static final String RETRIES_LOW_TOPIC = "retries.sms.low";

    public static final String SMPP_DLR_TOPIC = "smpp.dlr";
    public static final String HTTP_DLR_TOPIC = "http.dlr";
    public static final String HTTP_PROXY_TOPIC = "http.proxy";
    public static final String MPS_MESSAGES_COUNTER = "mps.counter";
    public static final String SS7_HSS_REGISTER_TOPIC_SUFFIX = ".ss7.hss.register";
    public static final String DIAMETER_HSS_REGISTER_TOPIC_SUFFIX = ".diameter.hss.register";
    public static final String SMPP_BIND_EVENTS_TOPIC = "smpp.bind.events";
}
