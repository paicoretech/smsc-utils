package com.paicbd.smsc.utils;

import com.paicbd.smsc.dto.MessageEvent;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErrorCodesTest {

    @Test
    void testGetErrorDescription() {
        String result;
        //For SMPP
        result = ErrorCodes.getErrorDescription(UtilsEnum.Module.SMPP_SERVER, 88, null);
        assertEquals("Throttling error (ESME has exceeded allowed message limits)", result);

        //For HTTP
        result = ErrorCodes.getErrorDescription(UtilsEnum.Module.HTTP_SERVER, 400, null);
        assertEquals("Bad Request", result);

        result = ErrorCodes.getErrorDescription(UtilsEnum.Module.HTTP2_CLIENT, 503, null);
        assertEquals("Service Unavailable", result);

        result = ErrorCodes.getErrorDescription(UtilsEnum.Module.HTTP2_CLIENT, 599, null);
        assertEquals("Unknown SMSC Error", result);

        //Absent subscriber causes reported by the AMF
        result = ErrorCodes.getErrorDescription(UtilsEnum.Module.HTTP2_CLIENT, ErrorCodes.SMSF_UE_NOT_RESPONDING, null);
        assertEquals("UE Not Responding", result);

        result = ErrorCodes.getErrorDescription(UtilsEnum.Module.HTTP2_CLIENT, ErrorCodes.SMSF_UE_NOT_REACHABLE_FOR_SESSION, null);
        assertEquals("UE Not Reachable For Session", result);

        result = ErrorCodes.getErrorDescription(UtilsEnum.Module.HTTP2_CLIENT, ErrorCodes.SMSF_N1_MSG_NOT_TRANSFERRED, null);
        assertEquals("N1 Message Not Transferred", result);

        result = ErrorCodes.getErrorDescription(UtilsEnum.Module.HTTP2_CLIENT, ErrorCodes.SMSF_N1N2_FAILURE_CALLBACK, null);
        assertEquals("N1N2 Failure Callback", result);

        result = ErrorCodes.getErrorDescription(UtilsEnum.Module.HTTP2_CLIENT, ErrorCodes.SMSF_UE_TEMPORARY_REJECT, null);
        assertEquals("UE Temporary Reject", result);

        result = ErrorCodes.getErrorDescription(UtilsEnum.Module.HTTP2_CLIENT, ErrorCodes.SMSF_N1N2_CORRELATION_TIMEOUT, null);
        assertEquals("N1N2 Correlation Timeout", result);

        //The same numbers belong to other protocols and must not leak into HTTP/2
        result = ErrorCodes.getErrorDescription(UtilsEnum.Module.SS7_CLIENT, ErrorCodes.SMSF_UE_NOT_RESPONDING, null);
        assertEquals("Invalid Source Address TON", result);

        result = ErrorCodes.getErrorDescription(UtilsEnum.Module.ROUTING, ErrorCodes.SMSF_UE_NOT_RESPONDING, null);
        assertEquals("Unknown SMSC Error", result);

        //For Routing
        result = ErrorCodes.getErrorDescription(UtilsEnum.Module.ROUTING, ErrorCodes.NOT_ROUTING, null);
        assertEquals("Not Routing Found", result);

        result = ErrorCodes.getErrorDescription(
                UtilsEnum.Module.ROUTING, ErrorCodes.MCC_MNC_CHARGING_NOT_CONFIGURED, null);
        assertEquals("No MCC/MNC Configured For Charging Address", result);

        //For Diameter
        result = ErrorCodes.getErrorDescription(UtilsEnum.Module.DIAMETER, 5012, null);
        assertEquals("Diameter Unable To Comply", result);

        result = ErrorCodes.getErrorDescription(
                UtilsEnum.Module.DIAMETER,
                ErrorCodes.MCC_MNC_CHARGING_NOT_CONFIGURED,
                ErrorCodes.CHARGING_ERROR);
        assertEquals("No MCC/MNC Configured For Charging Address - Charging Error", result);

        //For SS7
        result = ErrorCodes.getErrorDescription(UtilsEnum.Module.SS7_CLIENT, 300, null);
        assertEquals("Invoke TimeOut", result);

        result = ErrorCodes.getErrorDescription(UtilsEnum.Module.SS7_CLIENT, 32, 1);
        assertEquals("SM Delivery Failure - Equipment Protocol Error", result);

        result = ErrorCodes.getErrorDescription(UtilsEnum.Module.SS7_CLIENT, 32, 15);
        assertEquals("SM Delivery Failure", result);

        //Part skipped due to previous failure
        result = ErrorCodes.getErrorDescription(UtilsEnum.Module.SS7_CLIENT, ErrorCodes.PART_SKIPPED_PREVIOUS_FAILURE, null);
        assertEquals("Message part skipped due to previous part failure", result);
    }

    @Test
    void testAddSubErrorKey() {
        MessageEvent messageEvent = new MessageEvent();
        Map<String, Object> customParams = new HashMap<>();
        customParams.put("type", 1);
        messageEvent.setCustomParams(customParams);
        ErrorCodes.addSubErrorKey(messageEvent, 1);
        assertEquals(Map.of("type", 1, "sub_error", 1), messageEvent.getCustomParams());

        messageEvent = new MessageEvent();
        ErrorCodes.addSubErrorKey(messageEvent, 1);
        assertEquals(Map.of("sub_error", 1), messageEvent.getCustomParams());

        messageEvent = new MessageEvent();
        ErrorCodes.addSubErrorKey(messageEvent, null);
        assertTrue(Objects.isNull(messageEvent.getCustomParams()));

    }
}
