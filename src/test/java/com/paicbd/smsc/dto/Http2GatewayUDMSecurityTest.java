package com.paicbd.smsc.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Http2GatewayUDMSecurityTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void diameterAddressPreservesJsonAndLegacyAccessorCompatibility() throws JsonProcessingException {
        Http2GatewayUDMSecurity security = new Http2GatewayUDMSecurity();
        security.setSmsfDiameterAddress("diameter.example.com");

        String json = objectMapper.writeValueAsString(security);
        Http2GatewayUDMSecurity deserialized = objectMapper.readValue(
                "{\"smsf_diameter_address\":\"legacy.example.com\"}",
                Http2GatewayUDMSecurity.class);

        assertTrue(json.contains("\"smsf_diameter_address\":\"diameter.example.com\""), json);
        assertFalse(json.contains("\"smsfDiameterAddress\""), json);
        assertEquals("legacy.example.com", deserialized.getSmsfDiameterAddress());
        assertEquals("legacy.example.com", deserialized.getSmsf_diameter_address());

        deserialized.setSmsf_diameter_address("compatible.example.com");
        assertEquals("compatible.example.com", deserialized.getSmsfDiameterAddress());
    }
}
