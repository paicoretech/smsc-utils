package com.paicbd.smsc.utils;

import org.jsmpp.bean.BindType;
import org.jsmpp.bean.InterfaceVersion;
import org.jsmpp.bean.NumberingPlanIndicator;
import org.jsmpp.bean.TypeOfNumber;
import org.jsmpp.util.DeliveryReceiptState;
import org.junit.jupiter.api.Test;

import static com.paicbd.smsc.utils.UtilsEnum.Module.DIAMETER;
import static com.paicbd.smsc.utils.UtilsEnum.Module.HTTP_CLIENT;
import static com.paicbd.smsc.utils.UtilsEnum.Module.HTTP_SERVER;
import static com.paicbd.smsc.utils.UtilsEnum.Module.ORCHESTRATOR;
import static com.paicbd.smsc.utils.UtilsEnum.Module.RETRIES;
import static com.paicbd.smsc.utils.UtilsEnum.Module.ROUTING;
import static com.paicbd.smsc.utils.UtilsEnum.Module.SIP;
import static com.paicbd.smsc.utils.UtilsEnum.Module.SMPP_CLIENT;
import static com.paicbd.smsc.utils.UtilsEnum.Module.SMPP_SERVER;
import static com.paicbd.smsc.utils.UtilsEnum.Module.SS7_CLIENT;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UtilsEnumTest {

    @Test
    void getTypeOfNumberTest() {
        assertEquals(TypeOfNumber.INTERNATIONAL, UtilsEnum.getTypeOfNumber(1));
        assertEquals(TypeOfNumber.NATIONAL, UtilsEnum.getTypeOfNumber(2));
        assertEquals(TypeOfNumber.NETWORK_SPECIFIC, UtilsEnum.getTypeOfNumber(3));
        assertEquals(TypeOfNumber.SUBSCRIBER_NUMBER, UtilsEnum.getTypeOfNumber(4));
        assertEquals(TypeOfNumber.ALPHANUMERIC, UtilsEnum.getTypeOfNumber(5));
        assertEquals(TypeOfNumber.ABBREVIATED, UtilsEnum.getTypeOfNumber(6));
        assertEquals(TypeOfNumber.UNKNOWN, UtilsEnum.getTypeOfNumber(7));
    }

    @Test
    void getNumberingPlanIndicatorTest() {
        assertEquals(NumberingPlanIndicator.ISDN, UtilsEnum.getNumberingPlanIndicator(1));
        assertEquals(NumberingPlanIndicator.DATA, UtilsEnum.getNumberingPlanIndicator(3));
        assertEquals(NumberingPlanIndicator.TELEX, UtilsEnum.getNumberingPlanIndicator(4));
        assertEquals(NumberingPlanIndicator.LAND_MOBILE, UtilsEnum.getNumberingPlanIndicator(6));
        assertEquals(NumberingPlanIndicator.NATIONAL, UtilsEnum.getNumberingPlanIndicator(8));
        assertEquals(NumberingPlanIndicator.PRIVATE, UtilsEnum.getNumberingPlanIndicator(9));
        assertEquals(NumberingPlanIndicator.ERMES, UtilsEnum.getNumberingPlanIndicator(10));
        assertEquals(NumberingPlanIndicator.INTERNET, UtilsEnum.getNumberingPlanIndicator(14));
        assertEquals(NumberingPlanIndicator.WAP, UtilsEnum.getNumberingPlanIndicator(18));
        assertEquals(NumberingPlanIndicator.UNKNOWN, UtilsEnum.getNumberingPlanIndicator(19));
    }

    @Test
    void getBindTypeTest() {
        assertEquals(BindType.BIND_TX, UtilsEnum.getBindType("TRANSMITTER"));
        assertEquals(BindType.BIND_RX, UtilsEnum.getBindType("RECEIVER"));
        assertEquals(BindType.BIND_TRX, UtilsEnum.getBindType("TRANSCEIVER"));
        assertThrows(IllegalStateException.class, () -> UtilsEnum.getBindType("UNKNOWN"));
    }

    @Test
    void getInterfaceVersionTest() {
        assertEquals(InterfaceVersion.IF_00, UtilsEnum.getInterfaceVersion("IF_00"));
        assertEquals(InterfaceVersion.IF_33, UtilsEnum.getInterfaceVersion("IF_33"));
        assertEquals(InterfaceVersion.IF_34, UtilsEnum.getInterfaceVersion("IF_34"));
        assertEquals(InterfaceVersion.IF_50, UtilsEnum.getInterfaceVersion("IF_50"));
        assertThrows(IllegalStateException.class, () -> UtilsEnum.getInterfaceVersion("IF_99"));
    }

    @Test
    void getDeliverReceiptStateTest() {
        assertEquals(DeliveryReceiptState.ENROUTE, UtilsEnum.getDeliverReceiptState("ENROUTE"));
        assertEquals(DeliveryReceiptState.DELIVRD, UtilsEnum.getDeliverReceiptState("DELIVRD"));
        assertEquals(DeliveryReceiptState.EXPIRED, UtilsEnum.getDeliverReceiptState("EXPIRED"));
        assertEquals(DeliveryReceiptState.DELETED, UtilsEnum.getDeliverReceiptState("DELETED"));
        assertEquals(DeliveryReceiptState.UNDELIV, UtilsEnum.getDeliverReceiptState("UNDELIV"));
        assertEquals(DeliveryReceiptState.ACCEPTD, UtilsEnum.getDeliverReceiptState("ACCEPTD"));
        assertEquals(DeliveryReceiptState.REJECTD, UtilsEnum.getDeliverReceiptState("REJECTD"));
        assertEquals(DeliveryReceiptState.UNKNOWN, UtilsEnum.getDeliverReceiptState("UNKNOWN"));
    }

    @Test
    void getTrafficModeTest() {
        assertEquals("Stall", UtilsEnum.getTrafficMode(0));
        assertEquals("override", UtilsEnum.getTrafficMode(1));
        assertEquals("loadshare", UtilsEnum.getTrafficMode(2));
        assertEquals("Broadcast", UtilsEnum.getTrafficMode(3));
        assertThrows(IllegalStateException.class, () -> UtilsEnum.getTrafficMode(4));
    }

    @Test
    void getRuleTypeTest() {
        assertEquals("Solitary", UtilsEnum.getRuleType(1));
        assertEquals("Dominant", UtilsEnum.getRuleType(2));
        assertEquals("Loadshared", UtilsEnum.getRuleType(3));
        assertEquals("Broadcast", UtilsEnum.getRuleType(4));
        assertThrows(IllegalStateException.class, () -> UtilsEnum.getRuleType(5));
    }

    @Test
    void getLoadSharingAlgorithmTest() {
        assertEquals("Undefined", UtilsEnum.getLoadSharingAlgorithm(1));
        assertEquals("Bit0", UtilsEnum.getLoadSharingAlgorithm(2));
        assertEquals("Bit1", UtilsEnum.getLoadSharingAlgorithm(3));
        assertEquals("Bit2", UtilsEnum.getLoadSharingAlgorithm(4));
        assertEquals("Bit3", UtilsEnum.getLoadSharingAlgorithm(5));
        assertEquals("Bit4", UtilsEnum.getLoadSharingAlgorithm(6));
        assertThrows(IllegalStateException.class, () -> UtilsEnum.getLoadSharingAlgorithm(7));
    }

    @Test
    void getOriginationTypeTest() {
        assertEquals("ALL", UtilsEnum.getOriginationType(1));
        assertEquals("LOCAL", UtilsEnum.getOriginationType(2));
        assertEquals("REMOTE", UtilsEnum.getOriginationType(3));
        assertThrows(IllegalStateException.class, () -> UtilsEnum.getOriginationType(4));
    }

    @Test
    void testEnumValues() {
        UtilsEnum.Module[] expectedValues = {
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
        };

        assertArrayEquals(expectedValues, UtilsEnum.Module.values());
    }

    @Test
    void testMessageTypeEnumValues() {
        UtilsEnum.MessageType[] expectedValues = {
                UtilsEnum.MessageType.MESSAGE,
                UtilsEnum.MessageType.DELIVER,
                UtilsEnum.MessageType.SS7API_SRI,
                UtilsEnum.MessageType.SS7API_MT,
                UtilsEnum.MessageType.SS7API_MESSAGE,
                UtilsEnum.MessageType.HR_SRI,
                UtilsEnum.MessageType.HR_MESSAGE
        };

        assertArrayEquals(expectedValues, UtilsEnum.MessageType.values());
    }

    @Test
    void testCdrStatusEnumValues() {
        UtilsEnum.CdrStatus[] expectedValues = {
                UtilsEnum.CdrStatus.RECEIVED,
                UtilsEnum.CdrStatus.ENQUEUE,
                UtilsEnum.CdrStatus.SUCCESS,
                UtilsEnum.CdrStatus.RETRY,
                UtilsEnum.CdrStatus.FAILED
        };

        assertArrayEquals(expectedValues, UtilsEnum.CdrStatus.values());
    }

    @Test
    void testGlobalTitleIndicatorEnumValues() {
        UtilsEnum.GlobalTitleIndicator[] expectedValues = {
                UtilsEnum.GlobalTitleIndicator.GT0001,
                UtilsEnum.GlobalTitleIndicator.GT0010,
                UtilsEnum.GlobalTitleIndicator.GT0011,
                UtilsEnum.GlobalTitleIndicator.GT0100,
        };
        assertArrayEquals(expectedValues, UtilsEnum.GlobalTitleIndicator.values());
    }

    @Test
    void testGlobalTitleIndicatorEnumNumber() {
        assertEquals("GT0001", UtilsEnum.GlobalTitleIndicator.GT0001.toString());
        assertEquals("GT0010", UtilsEnum.GlobalTitleIndicator.GT0010.toString());
        assertEquals("GT0011", UtilsEnum.GlobalTitleIndicator.GT0011.toString());
        assertEquals("GT0100", UtilsEnum.GlobalTitleIndicator.GT0100.toString());
    }
}