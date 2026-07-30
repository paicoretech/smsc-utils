package com.paicbd.smsc.utils;

import org.jsmpp.bean.BindType;
import org.jsmpp.bean.InterfaceVersion;
import org.jsmpp.bean.NumberingPlanIndicator;
import org.jsmpp.bean.TypeOfNumber;
import org.jsmpp.util.DeliveryReceiptState;

public class UtilsEnum {
    private UtilsEnum() {
        throw new IllegalStateException("Utility class");
    }

    public static TypeOfNumber getTypeOfNumber(int ton) {
        return switch (ton) {
            case 1 -> TypeOfNumber.INTERNATIONAL;
            case 2 -> TypeOfNumber.NATIONAL;
            case 3 -> TypeOfNumber.NETWORK_SPECIFIC;
            case 4 -> TypeOfNumber.SUBSCRIBER_NUMBER;
            case 5 -> TypeOfNumber.ALPHANUMERIC;
            case 6 -> TypeOfNumber.ABBREVIATED;
            default -> TypeOfNumber.UNKNOWN;
        };
    }

    public static NumberingPlanIndicator getNumberingPlanIndicator(int npi) {
        return switch (npi) {
            case 1 -> NumberingPlanIndicator.ISDN;
            case 3 -> NumberingPlanIndicator.DATA;
            case 4 -> NumberingPlanIndicator.TELEX;
            case 6 -> NumberingPlanIndicator.LAND_MOBILE;
            case 8 -> NumberingPlanIndicator.NATIONAL;
            case 9 -> NumberingPlanIndicator.PRIVATE;
            case 10 -> NumberingPlanIndicator.ERMES;
            case 14 -> NumberingPlanIndicator.INTERNET;
            case 18 -> NumberingPlanIndicator.WAP;
            default -> NumberingPlanIndicator.UNKNOWN;
        };
    }

    public static BindType getBindType(String bindType) {
        return switch (bindType) {
            case "TRANSMITTER" -> BindType.BIND_TX;
            case "RECEIVER" -> BindType.BIND_RX;
            case "TRANSCEIVER" -> BindType.BIND_TRX;
            default -> throw new IllegalStateException("Unexpected BindType value: " + bindType);
        };
    }

    public static InterfaceVersion getInterfaceVersion(String interfaceVersion) {
        return switch (interfaceVersion) {
            case "IF_00" -> InterfaceVersion.IF_00;
            case "IF_33" -> InterfaceVersion.IF_33;
            case "IF_34" -> InterfaceVersion.IF_34;
            case "IF_50" -> InterfaceVersion.IF_50;
            default -> throw new IllegalStateException("Unexpected InterfaceVersion value: " + interfaceVersion);
        };
    }

    public static DeliveryReceiptState getDeliverReceiptState(String status) {
        return switch (status) {
            case "ENROUTE" -> DeliveryReceiptState.ENROUTE;
            case "DELIVRD" -> DeliveryReceiptState.DELIVRD;
            case "EXPIRED" -> DeliveryReceiptState.EXPIRED;
            case "DELETED" -> DeliveryReceiptState.DELETED;
            case "UNDELIV" -> DeliveryReceiptState.UNDELIV;
            case "ACCEPTD" -> DeliveryReceiptState.ACCEPTD;
            case "REJECTD" -> DeliveryReceiptState.REJECTD;
            default -> DeliveryReceiptState.UNKNOWN;
        };
    }

    private static final String  ERROR_MESSAGE = "Unexpected value: ";

    public static String getTrafficMode(int id) {
        return switch (id) {
            case 0 -> "Stall";
            case 1 -> "override";
            case 2 -> "loadshare";
            case 3 -> "Broadcast";
            default -> throw new IllegalStateException(ERROR_MESSAGE + id);
        };
    }

    public static String getRuleType(int id) {
        return switch (id) {
            case 1 -> "Solitary";
            case 2 -> "Dominant";
            case 3 -> "Loadshared";
            case 4 -> "Broadcast";
            default -> throw new IllegalStateException(ERROR_MESSAGE + id);
        };
    }

    public static String getLoadSharingAlgorithm(int id) {
        return switch (id) {
            case 1 -> "Undefined";
            case 2 -> "Bit0";
            case 3 -> "Bit1";
            case 4 -> "Bit2";
            case 5 -> "Bit3";
            case 6 -> "Bit4";
            default -> throw new IllegalStateException(ERROR_MESSAGE + id);
        };
    }

    public static String getOriginationType(int id) {
        return switch (id) {
            case 1 -> "ALL";
            case 2 -> "LOCAL";
            case 3 -> "REMOTE";
            default -> throw new IllegalStateException(ERROR_MESSAGE + id);
        };
    }

    public enum Module {
        SMPP_SERVER,
        SMPP_CLIENT,
        HTTP_SERVER,
        HTTP_CLIENT,
        SS7_CLIENT,
        ROUTING,
        ORCHESTRATOR,
        RETRIES,
        DIAMETER,
        SIP
    }

    public enum MessageType {
        MESSAGE,
        DELIVER,
        SS7API_SRI,
        SS7API_MT,
        SS7API_MESSAGE,
        HR_SRI,
        HR_MESSAGE
    }

    public enum CdrStatus {
        RECEIVED,
        ENQUEUE,
        SUCCESS,
        RETRY,
        FAILED
    }

    public enum GlobalTitleIndicator {
        GT0001("GLOBAL_TITLE_INCLUDES_NATURE_OF_ADDRESS_INDICATOR_ONLY"),
        GT0010("GLOBAL_TITLE_INCLUDES_TRANSLATION_TYPE_ONLY"),
        GT0011("GLOBAL_TITLE_INCLUDES_TRANSLATION_TYPE_NUMBERING_PLAN_AND_ENCODING_SCHEME"),
        GT0100("GLOBAL_TITLE_INCLUDES_TRANSLATION_TYPE_NUMBERING_PLAN_ENCODING_SCHEME_AND_NATURE_OF_ADDRESS");

        public final String gtName;
        GlobalTitleIndicator(String gtName) {
            this.gtName = gtName;
        }
    }
}
