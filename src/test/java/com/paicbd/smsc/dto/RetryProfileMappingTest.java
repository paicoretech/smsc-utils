package com.paicbd.smsc.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paicbd.smsc.utils.Converter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RetryProfileMappingTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private RetryProfileMapping sample() {
        return RetryProfileMapping.builder()
                .id(5)
                .originNetworkId(11)
                .destinationNetworkId(22)
                .errorCode("0x00000058")
                .profileId(1)
                .originProtocol("SMPP")
                .destinationProtocol("HTTP")
                .retryMappingConditions(List.of(
                        RetryProfileMapping.RetryMappingCondition.builder()
                                .id(10).type("STRING").field("error_code").operator("EQUALS").value("0x00000058").build()))
                .build();
    }

    @Test
    void toStringRendersTheSnakeCaseContract() throws Exception {
        JsonNode json = MAPPER.readTree(sample().toString());

        assertEquals(5, json.get("id").asInt());
        assertEquals(11, json.get("origin_network_id").asInt());
        assertEquals(22, json.get("destination_network_id").asInt());
        assertEquals("0x00000058", json.get("error_code").asText());
        assertEquals(1, json.get("profile_id").asInt());
        assertEquals("SMPP", json.get("origin_protocol").asText());
        assertEquals("HTTP", json.get("destination_protocol").asText());

        assertTrue(json.get("retry_mapping_conditions").isArray());
        assertEquals("EQUALS", json.get("retry_mapping_conditions").get(0).get("operator").asText());
        assertEquals("STRING", json.get("retry_mapping_conditions").get(0).get("type").asText());
    }

    @Test
    void theMappingCarriesOnlyTheProfileReference() throws Exception {
        JsonNode json = MAPPER.readTree(sample().toString());

        assertNull(json.get("retry_profile"), "the profile is published separately, not embedded");
        assertEquals(1, json.get("profile_id").asInt(), "the mapping must keep the reference to resolve it");
    }

    @Test
    void deserializesBackKeepingTheWholeAggregate() {
        RetryProfileMapping parsed = Converter.stringToObject(sample().toString(), RetryProfileMapping.class);

        assertNotNull(parsed);
        assertEquals(22, parsed.getDestinationNetworkId());
        assertEquals(1, parsed.getRetryMappingConditions().size());
        assertEquals("error_code", parsed.getRetryMappingConditions().get(0).getField());
        assertEquals("STRING", parsed.getRetryMappingConditions().get(0).getType());

    }

    @Test
    void anArrayOfMappingsIsTheShapeStoredUnderOneDestinationKey() throws Exception {
        String stored = Converter.valueAsString(List.of(sample(), sample()));
        JsonNode json = MAPPER.readTree(stored);

        assertTrue(json.isArray());
        assertEquals(2, json.size());
        assertEquals(22, json.get(1).get("destination_network_id").asInt());
    }

    @Test
    void defaultsAreEmptyCollectionsAndNullAggregates() {
        RetryProfileMapping empty = new RetryProfileMapping();

        assertNotNull(empty.getRetryMappingConditions(), "conditions must default to an empty list, not null");
        assertTrue(empty.getRetryMappingConditions().isEmpty());

        RetryProfileMapping built = RetryProfileMapping.builder().id(1).build();
        assertNotNull(built.getRetryMappingConditions(), "the builder must honour the empty-list default");
        assertTrue(built.getRetryMappingConditions().isEmpty());
    }

    @Test
    void unknownFieldsAreIgnoredSoModulesCanUpgradeIndependently() {
        String withExtraField = """
                {"id":7,"destination_network_id":22,"some_future_field":"whatever"}
                """;

        RetryProfileMapping parsed = Converter.stringToObject(withExtraField, RetryProfileMapping.class);

        assertNotNull(parsed);
        assertEquals(7, parsed.getId());
        assertEquals(22, parsed.getDestinationNetworkId());
    }
}
