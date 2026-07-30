package com.paicbd.smsc.utils;

import com.paicbd.smsc.dto.MessageEvent;
import org.apache.logging.log4j.util.Strings;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Map.entry;

public class ErrorCodes {

    static final Map<Integer, String> smppErrorMapping = new ConcurrentHashMap<>();
    static final Map<Integer, String> ss7ErrorMapping = new ConcurrentHashMap<>();
    static final Map<Integer, String> httpErrorMapping = new ConcurrentHashMap<>();
    static final Map<Integer, String> diameterErrorMapping = new ConcurrentHashMap<>();
    static final Map<Integer, String> smscErrorMapping = new ConcurrentHashMap<>();
    static final Map<Integer, Map<Integer, String>> ss7SubError = new ConcurrentHashMap<>();
    static final Map<Integer, String> sipErrorMapping = new ConcurrentHashMap<>();


    static {
        String moduleError = "Module Error";

        smppErrorMapping.put(1, "Message Length is invalid");
        smppErrorMapping.put(2, "Command Length is invalid");
        smppErrorMapping.put(3, "Invalid Command ID");
        smppErrorMapping.put(4, "Incorrect BIND Status for given command");
        smppErrorMapping.put(5, "ESME Already in Bound State");
        smppErrorMapping.put(6, "Invalid Priority Flag");
        smppErrorMapping.put(7, "Invalid Registered Delivery Flag");
        smppErrorMapping.put(8, "System Error");
        smppErrorMapping.put(10, "Invalid Source Address");
        smppErrorMapping.put(11, "Invalid Destination Address");
        smppErrorMapping.put(12, "Message ID is invalid");
        smppErrorMapping.put(13, "Bind Failed");
        smppErrorMapping.put(14, "Invalid Password");
        smppErrorMapping.put(15, "Invalid System ID");
        smppErrorMapping.put(17, "Cancel SM Failed");
        smppErrorMapping.put(19, "Replace SM Failed");
        smppErrorMapping.put(20, "Message Queue Full");
        smppErrorMapping.put(21, "Invalid Service Type");
        smppErrorMapping.put(51, "Invalid number of destinations");
        smppErrorMapping.put(52, "Invalid Distribution List name");
        smppErrorMapping.put(64, "Destination flag is invalid (submit_multi)");
        smppErrorMapping.put(66, "Invalid 'submit with replace' request");
        smppErrorMapping.put(67, "Invalid esm_class field data");
        smppErrorMapping.put(69, "submit_sm or submit_multi failed");
        smppErrorMapping.put(72, "Invalid Source address TON");
        smppErrorMapping.put(73, "Invalid Source address NPI");
        smppErrorMapping.put(80, "Invalid Destination address TON");
        smppErrorMapping.put(81, "Invalid Destination address NPI");
        smppErrorMapping.put(83, "Invalid system_type field");
        smppErrorMapping.put(84, "Invalid replace_if_present flag");
        smppErrorMapping.put(85, "Invalid number of messages");
        smppErrorMapping.put(88, "Throttling error (ESME has exceeded allowed message limits)");
        smppErrorMapping.put(97, "Invalid Scheduled Delivery Time");
        smppErrorMapping.put(98, "Invalid message validity period (Expiry time)");
        smppErrorMapping.put(99, "Predefined Message Invalid or Not Found");
        smppErrorMapping.put(100, "ESME Receiver Temporary App Error Code");
        smppErrorMapping.put(101, "ESME Receiver Permanent App Error Code");
        smppErrorMapping.put(102, "ESME Receiver Reject Message Error Code");
        smppErrorMapping.put(103, "query_sm request failed");
        smppErrorMapping.put(192, "Error in the optional part of the PDU Body");
        smppErrorMapping.put(193, "Optional Parameter not allowed");
        smppErrorMapping.put(194, "Invalid Parameter Length");
        smppErrorMapping.put(195, "Expected Optional Parameter missing");
        smppErrorMapping.put(196, "Invalid Optional Parameter Value");
        smppErrorMapping.put(254, "Delivery Failure (used for data_sm_resp)");
        smppErrorMapping.put(255, "Unknown Error");
        smppErrorMapping.put(256, "Not authorised to use specified service_type");
        smppErrorMapping.put(257, "Prohibited from using specified operation");
        smppErrorMapping.put(258, "Specified service_type is unavailable");
        smppErrorMapping.put(259, "Specified service_type is denied");
        smppErrorMapping.put(260, "Invalid Data Coding Scheme");
        smppErrorMapping.put(261, "Source Address Sub unit is invalid");
        smppErrorMapping.put(262, "Destination Address Sub unit is invalid");
        smppErrorMapping.put(263, "Broadcast Frequency Interval is invalid");
        smppErrorMapping.put(264, "Broadcast Alias Name is invalid");
        smppErrorMapping.put(265, "Broadcast Area Format is invalid");
        smppErrorMapping.put(266, "Number of Broadcast Areas is invalid");
        smppErrorMapping.put(267, "Broadcast Content Type is invalid");
        smppErrorMapping.put(268, "Broadcast Message Class is invalid");
        smppErrorMapping.put(269, "broadcast_sm operation failed");
        smppErrorMapping.put(270, "query_broadcast_sm operation failed");
        smppErrorMapping.put(271, "cancel_broadcast_sm operation failed");
        smppErrorMapping.put(272, "Number of Repeated Broadcasts is invalid");
        smppErrorMapping.put(273, "Broadcast Service Group is invalid");
        smppErrorMapping.put(274, "Broadcast Channel Indicator is invalid");
        smppErrorMapping.put(300, "PDU TimeOut");
        smppErrorMapping.put(500, moduleError);
        smppErrorMapping.put(504, "PDU Exception");
        smppErrorMapping.put(505, "Invalid Response Exception");
        smppErrorMapping.put(506, "IO Exception");
        smppErrorMapping.put(513, "SMPP Connection Unavailable (No active session)");

        ss7ErrorMapping.put(1, "Unknown Subscriber");
        ss7ErrorMapping.put(2, "Unknown Base Station");
        ss7ErrorMapping.put(3, "Unknown MSC");
        ss7ErrorMapping.put(5, "Unidentified Subscriber");
        ss7ErrorMapping.put(6, "Absent SubscriberSM");
        ss7ErrorMapping.put(7, "Unknown Equipment");
        ss7ErrorMapping.put(8, "Roaming NotAllowed");
        ss7ErrorMapping.put(9, "Illegal Subscriber");
        ss7ErrorMapping.put(10, "Bearer Service Not Provisioned");
        ss7ErrorMapping.put(11, "Teleservice Not Provisioned");
        ss7ErrorMapping.put(12, "Illegal Equipment");
        ss7ErrorMapping.put(13, "Call Barred");
        ss7ErrorMapping.put(14, "Forwarding Violation");
        ss7ErrorMapping.put(15, "CUG Reject");
        ss7ErrorMapping.put(16, "Illegal SSOperation");
        ss7ErrorMapping.put(17, "SS Error Status");
        ss7ErrorMapping.put(18, "SS Not Available");
        ss7ErrorMapping.put(19, "SS Subscription Violation");
        ss7ErrorMapping.put(20, "SS Incompatibility");
        ss7ErrorMapping.put(21, "Facility Not Supported");
        ss7ErrorMapping.put(22, "Ongoing GroupCall");
        ss7ErrorMapping.put(23, "Invalid Target Base Station");
        ss7ErrorMapping.put(24, "No Radio Resource Available");
        ss7ErrorMapping.put(25, "No Handover Number Available");
        ss7ErrorMapping.put(26, "Subsequent Handover Failure");
        ss7ErrorMapping.put(27, "Absent Subscriber");
        ss7ErrorMapping.put(28, "Incompatible Terminal");
        ss7ErrorMapping.put(29, "Short Term Denial");
        ss7ErrorMapping.put(30, "Long Term Denial");
        ss7ErrorMapping.put(31, "Subscriber Busy For MTSMS");
        ss7ErrorMapping.put(32, "SM Delivery Failure");
        ss7ErrorMapping.put(33, "Message Waiting List Full");
        ss7ErrorMapping.put(34, "System Failure");
        ss7ErrorMapping.put(35, "Data Missing");
        ss7ErrorMapping.put(36, "Unexpected DataValue");
        ss7ErrorMapping.put(37, "PW Registration Failure");
        ss7ErrorMapping.put(38, "Negative PW Check");
        ss7ErrorMapping.put(39, "No Roaming Number Available");
        ss7ErrorMapping.put(40, "Tracing Buffer Full");
        ss7ErrorMapping.put(42, "Target Cell Outside Group Call Area");
        ss7ErrorMapping.put(43, "Number Of PW Attempts Violation");
        ss7ErrorMapping.put(44, "Number Changed");
        ss7ErrorMapping.put(45, "Busy Subscriber");
        ss7ErrorMapping.put(46, "No Subscriber Reply");
        ss7ErrorMapping.put(47, "Forwarding Failed");
        ss7ErrorMapping.put(48, "OR Not Allowed");
        ss7ErrorMapping.put(49, "ATI Not Allowed");
        ss7ErrorMapping.put(50, "No Group Call Number Available");
        ss7ErrorMapping.put(51, "Resource Limitation");
        ss7ErrorMapping.put(52, "Unauthorized Requesting Network");
        ss7ErrorMapping.put(53, "Unauthorized LCS Client");
        ss7ErrorMapping.put(54, "Position Method Failure");
        ss7ErrorMapping.put(58, "Unknownor Unreachable LCS Client");
        ss7ErrorMapping.put(59, "MM Event Not Supported");
        ss7ErrorMapping.put(60, "ATSI Not Allowed");
        ss7ErrorMapping.put(61, "ATM Not Allowed");
        ss7ErrorMapping.put(62, "Information Not Available");
        ss7ErrorMapping.put(71, "Unknown Alphabet");
        ss7ErrorMapping.put(72, "USSD Busy");
        ss7ErrorMapping.put(300, "Invoke TimeOut");
        ss7ErrorMapping.put(500, moduleError);
        ss7ErrorMapping.put(507, "Dialog Reject");
        ss7ErrorMapping.put(508, "Map User Abort");
        ss7ErrorMapping.put(509, "Map Provider Abort");
        ss7ErrorMapping.put(510, "MW Status MNRF Set (Absent Subscriber)");
        ss7ErrorMapping.put(511, "MW Status MCEF Set (Memory Capacity Exceeded)");
        ss7ErrorMapping.put(512, "Reject Component");
        ss7ErrorMapping.put(520, "Invalid Source Address TON");
        ss7ErrorMapping.put(801, "Missing MCC–MNC Routing Data");

        httpErrorMapping.put(400, "Bad Request");
        httpErrorMapping.put(401, "Unauthorized");
        httpErrorMapping.put(403, "Forbidden");
        httpErrorMapping.put(404, "Not Found");
        httpErrorMapping.put(405, "Method Not Allowed");
        httpErrorMapping.put(408, "Request Timeout");
        httpErrorMapping.put(409, "Conflict");
        httpErrorMapping.put(410, "Gone");
        httpErrorMapping.put(415, "Unsupported Media Type");
        httpErrorMapping.put(429, "Too Many Requests");
        httpErrorMapping.put(500, "Internal Server Error");
        httpErrorMapping.put(501, "Not Implemented");
        httpErrorMapping.put(502, "Bad Gateway");
        httpErrorMapping.put(503, "Service Unavailable");
        httpErrorMapping.put(504, "Gateway Timeout");

        diameterErrorMapping.put(600, "Charging Error");
        diameterErrorMapping.put(1001, "Diameter Multi Round Auth");
        diameterErrorMapping.put(2001, "Diameter Success");
        diameterErrorMapping.put(2002, "Diameter Limited Success");
        diameterErrorMapping.put(3001, "Diameter Command Unsupported");
        diameterErrorMapping.put(3002, "Diameter Unable To Deliver");
        diameterErrorMapping.put(3003, "Diameter Realm Not Served");
        diameterErrorMapping.put(3004, "Diameter Too Busy");
        diameterErrorMapping.put(3005, "Diameter Loop Detected");
        diameterErrorMapping.put(3006, "Diameter Redirect Indication");
        diameterErrorMapping.put(3007, "Diameter Application Unsupported");
        diameterErrorMapping.put(3008, "Diameter Invalid Hdr Bits");
        diameterErrorMapping.put(3009, "Diameter Invalid Avp Bits");
        diameterErrorMapping.put(3010, "Diameter Unknown Peer");
        diameterErrorMapping.put(3011, "Diameter Realm Redirect Indication");
        diameterErrorMapping.put(4001, "Diameter Authentication Rejected");
        diameterErrorMapping.put(4002, "Diameter Out Of Space");
        diameterErrorMapping.put(4003, "Election Lost");
        diameterErrorMapping.put(4010, "Diameter End User Service Denied");
        diameterErrorMapping.put(4011, "Diameter Credit Control Not Applicable");
        diameterErrorMapping.put(4012, "Diameter Credit Limit Reached");
        diameterErrorMapping.put(5001, "Diameter Avp Unsupported");
        diameterErrorMapping.put(5002, "Diameter Unknown Session Id");
        diameterErrorMapping.put(5003, "Diameter Authorization Rejected");
        diameterErrorMapping.put(5004, "Diameter Invalid Avp Value");
        diameterErrorMapping.put(5005, "Diameter Missing Avp");
        diameterErrorMapping.put(5006, "Diameter Resources Exceeded");
        diameterErrorMapping.put(5007, "Diameter Contradicting Avps");
        diameterErrorMapping.put(5008, "Diameter Avp Not Allowed");
        diameterErrorMapping.put(5009, "Diameter Avp Occurs Too Many Times");
        diameterErrorMapping.put(5010, "Diameter No Common Application");
        diameterErrorMapping.put(5011, "Diameter Unsupported Version");
        diameterErrorMapping.put(5012, "Diameter Unable To Comply");
        diameterErrorMapping.put(5013, "Diameter Invalid Bit In Header");
        diameterErrorMapping.put(5014, "Diameter Invalid Avp Length");
        diameterErrorMapping.put(5015, "Diameter Invalid Message Length");
        diameterErrorMapping.put(5016, "Diameter Invalid Avp Bit Combo");
        diameterErrorMapping.put(5017, "Diameter No Common Security");
        diameterErrorMapping.put(5030, "Diameter User Unknown");
        diameterErrorMapping.put(5031, "Diameter Rating Failed");
        // Messages Errors
        diameterErrorMapping.put(5550, "Diameter Absent User");
        diameterErrorMapping.put(5551, "Diameter User Busy For MT-SMS");
        diameterErrorMapping.put(5552, "Diameter Facility Not Supported");
        diameterErrorMapping.put(5553, "Diameter Illegal User");
        diameterErrorMapping.put(5554, "Diameter Illegal Equipment");
        diameterErrorMapping.put(5555, "Diameter SM Delivery Failure");
        diameterErrorMapping.put(5556, "Diameter Service Not Subscribed");
        diameterErrorMapping.put(5557, "Diameter Service Barred");
        diameterErrorMapping.put(5558, "Diameter MWD List Full");
        diameterErrorMapping.put(300, "TimeOut");
        diameterErrorMapping.put(500, moduleError);
        diameterErrorMapping.put(510, "MW Status MNRF Set (Absent Subscriber)");
        diameterErrorMapping.put(511, "MW Status MCEF Set (Memory Capacity Exceeded)");

        smscErrorMapping.put(501, "Not Routing Found");
        smscErrorMapping.put(502, "Not Destination Found");
        smscErrorMapping.put(503, "Not Supported");
        smscErrorMapping.put(701, "Blocked By Global DND");
        smscErrorMapping.put(702, "Blocked By Network DND");
        smscErrorMapping.put(703, "Blocked by Sender DND");
        smscErrorMapping.put(802, "Not Destination Address Registered");

        ss7SubError.put(32, Map.of(
                0, "Memory Capacity Exceeded",
                1, "Equipment Protocol Error",
                2, "Equipment Not SMEquipped",
                3, "Unknown Service Centre",
                4, "scCongestion",
                5, "Invalid SMEAddress",
                6, "Subscriber Not SCSubscriber")
        );

        ss7SubError.put(6, Map.of(
                0, "No Paging Response Via The MSC",
                1, "IMSI Detached",
                2, "Roaming Restriction",
                3, "Deregistered In The HLR For Non GPRS",
                4, "MS Purged For Non GPRS",
                5, "No Paging Response Via The SGSN",
                6, "GPRS Detached",
                7, "Deregistered In The HLR For GPRS",
                8, "MS Purged For GPRS",
                9, "Unidentified Subscriber Via The MSC"
        ));

        ss7SubError.put(2, Map.ofEntries(
                entry(0, "No Paging Response Via The MSC"),
                entry(1, "IMSI Detached"),
                entry(2, "Roaming Restriction"),
                entry(3, "Deregistered In The HLR For Non GPRS"),
                entry(4, "MS Purged For Non GPRS"),
                entry(5, "No Paging Response Via The SGSN"),
                entry(6, "GPRS Detached"),
                entry(7, "Deregistered In The HLR For GPRS"),
                entry(8, "MS Purged For GPRS"),
                entry(9, "Unidentified Subscriber Via The MSC"),
                entry(10, "Unidentified Subscriber Via The SGSN"),
                entry(11, "Deregistered In The HSS HLR For IMS"),
                entry(12, "No Response Via The IP SM GW")
        ));

        ss7SubError.put(27, Map.of(
                0, "IMSI Detach",
                1, "Restricted Area",
                2, "No Page Response",
                3, "Purged MS"
        ));

        ss7SubError.put(13, Map.of(
                0, "Barring Service Active",
                1, "Operator Barring"
        ));
    }


    public static final int TIMEOUT_ERROR = 300;
    public static final int SYSTEM_ERROR = 500;
    public static final int NOT_ROUTING = 501;
    public static final int NOT_DESTINATION = 502;
    public static final int NOT_SUPPORTED = 503;
    public static final int PDU_EXCEPTION_ERROR = 504;
    public static final int INVALID_RESPONSE_EXCEPTION_ERROR = 505;
    public static final int IO_EXCEPTION_ERROR = 506;
    public static final int DIALOG_REJECT = 507;
    public static final int MAP_USER_ABORT = 508;
    public static final int MAP_PROVIDER_ABORT = 509;
    public static final int MW_STATUS_MNRF_SET_ERROR = 510;
    public static final int MW_STATUS_MCEF_SET_ERROR = 511;
    public static final int MAP_REJECT_COMPONENT = 512;
    public static final int SMPP_CONNECTION_UNAVAILABLE = 513;
    public static final int INVALID_SOURCE_ADDR_TON = 520;
    public static final int CHARGING_ERROR = 600;
    public static final String SUB_ERROR_KEY = "sub_error";
    public static final int BLOCKED_BY_DND_GLOBAL = 701;
    public static final int BLOCKED_BY_DND_NETWORK = 702;
    public static final int BLOCKED_BY_DND_SENDER = 703;
    public static final int MCC_MNC_NO_FOUND = 801;

    public static final int NOT_DESTINATION_ADDR_REGISTERED = 802;

    private ErrorCodes() {
        throw new IllegalStateException("Utility class");
    }


    public static String getErrorDescription(UtilsEnum.Module module, int errorCode, Integer subError) {
        String defaultError = "Unknown SMSC Error";
        return switch (module) {
            case SMPP_CLIENT, SMPP_SERVER -> smppErrorMapping.getOrDefault(errorCode, defaultError);
            case HTTP_CLIENT, HTTP_SERVER -> httpErrorMapping.getOrDefault(errorCode, defaultError);
            case SS7_CLIENT -> {
                String error = ss7ErrorMapping.getOrDefault(errorCode, defaultError);
                if (Objects.nonNull(subError)) {
                    String subErrorDescription = ss7SubError.getOrDefault(errorCode, Map.of()).getOrDefault(subError, "");
                    yield error + (Strings.isEmpty(subErrorDescription) ? "" : " - " + subErrorDescription);
                }
                yield error;
            }
            case ROUTING, ORCHESTRATOR, RETRIES -> smscErrorMapping.getOrDefault(errorCode, defaultError);
            case DIAMETER -> {
                String error = diameterErrorMapping.getOrDefault(errorCode, defaultError);
                if (Objects.nonNull(subError)) {
                    String subErrorDescription = diameterErrorMapping.getOrDefault(subError, "");
                    yield error + (Strings.isEmpty(subErrorDescription) ? "" : " - " + subErrorDescription);
                }
                yield error;
            }
            case SIP -> sipErrorMapping.getOrDefault(errorCode, defaultError);
        };
    }

    public static void addSubErrorKey(MessageEvent messageEvent, Integer subError) {
        if (Objects.isNull(subError)) {
            return;
        }
        messageEvent.addCustomParam(SUB_ERROR_KEY, subError);
    }

}
