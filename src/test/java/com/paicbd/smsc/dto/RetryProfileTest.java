package com.paicbd.smsc.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paicbd.smsc.utils.Converter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


class RetryProfileTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private RetryProfile sample() {
        return RetryProfile.builder()
                .profileId(1)
                .name("Retry profile")
                .maxDueDelaySeconds(30)
                .periodRSeconds(20)
                .steps(List.of(
                        RetryProfile.RetryProfileStep.builder()
                                .id(1).stepNumber(1).attempts(3).delaySeconds(60).build(),
                        RetryProfile.RetryProfileStep.builder()
                                .id(2).stepNumber(2).attempts(5).delaySeconds(300).build()))
                .action(RetryProfile.RetryProfileAction.builder()
                        .id(1)
                        .newSourceAddress("50588887777")
                        .newDestinationAddress("50577776666")
                        .removeSourceAddressPrefix("505")
                        .messagePriorityStaggeredEnabled(true)
                        .overrideMessagePriority("HIGH")
                        .build())
                .hasActions(true)
                .build();
    }

    @Test
    void toStringRendersTheSnakeCaseContract() throws Exception {
        JsonNode json = MAPPER.readTree(sample().toString());

        assertEquals(1, json.get("profile_id").asInt());
        assertEquals("Retry profile", json.get("name").asText());
        assertEquals(30, json.get("max_due_delay_seconds").asInt());
        assertEquals(20, json.get("period_r_seconds").asInt());

        assertNull(json.get("recovery_profile_id"), "an unset optional must be omitted from the payload");
        assertNull(json.get("reroute_network_id"), "an unset optional must be omitted from the payload");
    }

    @Test
    void stepsAndActionTravelWithTheProfile() throws Exception {
        JsonNode json = MAPPER.readTree(sample().toString());

        assertTrue(json.get("steps").isArray());
        assertEquals(2, json.get("steps").size());
        assertEquals(1, json.get("steps").get(0).get("step_number").asInt());
        assertEquals(60, json.get("steps").get(0).get("delay_seconds").asInt());
        assertEquals(300, json.get("steps").get(1).get("delay_seconds").asInt());

        JsonNode action = json.get("action");
        assertNotNull(action);
        assertEquals("50588887777", action.get("new_source_address").asText());
        assertEquals("505", action.get("remove_source_address_prefix").asText());
        assertTrue(action.get("message_priority_staggered_enabled").asBoolean());
        assertEquals("HIGH", action.get("override_message_priority").asText());
        assertTrue(json.get("has_actions").asBoolean(), "the flag must travel alongside the action");
    }

    @Test
    void hasActionsDefaultsToFalse() throws Exception {
        JsonNode json = MAPPER.readTree(RetryProfile.builder().profileId(1).build().toString());

        assertFalse(json.get("has_actions").asBoolean(), "a profile with no action must be flagged false");
        assertNull(json.get("action"));
    }

    @Test
    void deserializesBackKeepingTheWholeAggregate() {
        RetryProfile parsed = Converter.stringToObject(sample().toString(), RetryProfile.class);

        assertNotNull(parsed);
        assertEquals(1, parsed.getProfileId());
        assertEquals("Retry profile", parsed.getName());
        assertEquals(30, parsed.getMaxDueDelaySeconds());
        assertEquals(2, parsed.getSteps().size());
        assertEquals(5, parsed.getSteps().get(1).getAttempts());
        assertNotNull(parsed.getAction());
        assertEquals("HIGH", parsed.getAction().getOverrideMessagePriority());
    }

    @Test
    void aSingleProfileIsTheShapeStoredUnderOneProfileKey() throws Exception {
        JsonNode json = MAPPER.readTree(sample().toString());
        assertTrue(json.isObject(), "the value stored under a profile id must be a single object");
    }

    @Test
    void defaultsAreEmptyCollectionsAndNullAggregates() {
        RetryProfile empty = new RetryProfile();

        assertNotNull(empty.getSteps(), "steps must default to an empty list, not null");
        assertTrue(empty.getSteps().isEmpty());
        assertNull(empty.getAction());

        RetryProfile built = RetryProfile.builder().profileId(1).build();
        assertNotNull(built.getSteps(), "the builder must honour the empty-list default");
        assertTrue(built.getSteps().isEmpty());
    }

    @Test
    void unknownFieldsAreIgnoredSoModulesCanUpgradeIndependently() {
        String withExtraField = """
                {"profile_id":7,"name":"x","some_future_field":"whatever"}
                """;

        RetryProfile parsed = Converter.stringToObject(withExtraField, RetryProfile.class);

        assertNotNull(parsed);
        assertEquals(7, parsed.getProfileId());
        assertEquals("x", parsed.getName());
    }
}
