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

        //For Routing
        result = ErrorCodes.getErrorDescription(UtilsEnum.Module.ROUTING, ErrorCodes.NOT_ROUTING, null);
        assertEquals("Not Routing Found", result);

        //For Diameter
        result = ErrorCodes.getErrorDescription(UtilsEnum.Module.DIAMETER, 5012, null);
        assertEquals("Diameter Unable To Comply", result);

        //For SS7
        result = ErrorCodes.getErrorDescription(UtilsEnum.Module.SS7_CLIENT, 300, null);
        assertEquals("Invoke TimeOut", result);

        result = ErrorCodes.getErrorDescription(UtilsEnum.Module.SS7_CLIENT, 32, 1);
        assertEquals("SM Delivery Failure - Equipment Protocol Error", result);

        result = ErrorCodes.getErrorDescription(UtilsEnum.Module.SS7_CLIENT, 32, 15);
        assertEquals("SM Delivery Failure", result);
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