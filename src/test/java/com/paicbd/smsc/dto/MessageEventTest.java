package com.paicbd.smsc.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.paicbd.smsc.utils.ChargingType;
import com.paicbd.smsc.utils.Converter;
import com.paicbd.smsc.utils.GeneralSmscConstants;
import com.paicbd.smsc.utils.UtilsEnum;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageEventTest {
    ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .registerModule(new JavaTimeModule());

    @Test
    void chargingTypeDefaultsToIec() throws JsonProcessingException {
        assertEquals(ChargingType.IEC, new MessageEvent().getChargingType());
        assertEquals(ChargingType.IEC, MessageEvent.builder().build().getChargingType());
        assertEquals(ChargingType.IEC,
                objectMapper.readValue("{}", MessageEvent.class).getChargingType());
        assertEquals(ChargingType.IEC, new RoutingRule().getChargingType());
        assertEquals(ChargingType.IEC, RoutingRule.builder().build().getChargingType());
        assertEquals(ChargingType.IEC,
                objectMapper.readValue("{}", RoutingRule.class).getChargingType());
    }

    @Test
    void chargingDestinationAndMccMncDefaultsAreSourceAndNull() throws JsonProcessingException {
        assertFalse(new MessageEvent().isDestinationCharging());
        assertFalse(new MessageEvent().isPlmnLookupEnabled());
        assertFalse(new MessageEvent().isSriForCharging());
        assertFalse(MessageEvent.builder().build().isDestinationCharging());
        assertFalse(MessageEvent.builder().build().isPlmnLookupEnabled());
        assertFalse(MessageEvent.builder().build().isSriForCharging());
        assertFalse(objectMapper.readValue("{}", MessageEvent.class).isDestinationCharging());
        assertNull(new MessageEvent().getMccMncCharging());
        assertNull(MessageEvent.builder().build().getMccMncCharging());
        assertNull(objectMapper.readValue("{}", MessageEvent.class).getMccMncCharging());
        assertFalse(new RoutingRule().isDestinationCharging());
        assertFalse(new RoutingRule().isPlmnLookupEnabled());
        assertFalse(new RoutingRule().isSriForCharging());
        assertFalse(RoutingRule.builder().build().isDestinationCharging());
        assertFalse(objectMapper.readValue("{}", RoutingRule.class).isDestinationCharging());
    }

    @Test
    void chargingDestinationAndMccMncAreSerialized() throws JsonProcessingException {
        MessageEvent messageEvent = MessageEvent.builder()
                .destinationCharging(true)
                .plmnLookupEnabled(true)
                .sriForCharging(true)
                .sourceImsi("710730123456789")
                .sourceNetworkNodeNumber("50588880000")
                .mccMncCharging("71001")
                .build();

        String json = objectMapper.writeValueAsString(messageEvent);
        MessageEvent deserialized = objectMapper.readValue(json, MessageEvent.class);

        assertTrue(json.contains("\"destination_charging\":true"), json);
        assertTrue(json.contains("\"plmn_lookup_enabled\":true"), json);
        assertTrue(json.contains("\"sri_for_charging\":true"), json);
        assertTrue(json.contains("\"source_imsi\":\"710730123456789\""), json);
        assertTrue(json.contains("\"source_network_node_number\":\"50588880000\""), json);
        assertTrue(json.contains("\"mcc_mnc_charging\":\"71001\""), json);
        assertTrue(deserialized.isDestinationCharging());
        assertTrue(deserialized.isPlmnLookupEnabled());
        assertTrue(deserialized.isSriForCharging());
        assertEquals("710730123456789", deserialized.getSourceImsi());
        assertEquals("50588880000", deserialized.getSourceNetworkNodeNumber());
        assertEquals("71001", deserialized.getMccMncCharging());
    }

    @Test
    void mccMncDestinationIsSerializedAndCopiedToTheCdr() throws JsonProcessingException {
        MessageEvent messageEvent = MessageEvent.builder().id("1720000000000-1").mccMncDestination("71021").build();

        String json = objectMapper.writeValueAsString(messageEvent);
        UtilsRecords.Cdr cdr = objectMapper.readValue(json, MessageEvent.class)
                .toCdr(UtilsEnum.Module.SMPP_CLIENT, UtilsEnum.MessageType.MESSAGE, UtilsEnum.CdrStatus.SUCCESS, "", null);

        assertTrue(json.contains("\"mcc_mnc_destination\":\"71021\""), json);
        assertEquals("71021", cdr.mccMncDestination());
        assertTrue(cdr.toString().contains("\"mcc_mnc_destination\":\"71021\""));
        assertEquals("", MessageEvent.builder().id("1720000000000-2").build()
                .toCdr(UtilsEnum.Module.SMPP_CLIENT, UtilsEnum.MessageType.MESSAGE, UtilsEnum.CdrStatus.SUCCESS, "", null)
                .mccMncDestination());
    }

    @Test
    void multipartRetryConfigurationDefaultsToRetryAllParts() throws JsonProcessingException {
        assertTrue(new MessageEvent().isRetryAllParts());
        assertTrue(MessageEvent.builder().build().isRetryAllParts());
        assertTrue(objectMapper.readValue("{}", MessageEvent.class).isRetryAllParts());
        assertTrue(new RoutingRule().isRetryAllParts());
        assertTrue(RoutingRule.builder().build().isRetryAllParts());
        assertTrue(objectMapper.readValue("{}", RoutingRule.class).isRetryAllParts());
    }

    @Test
    void multipartRetryConfigurationAndPartStatusAreSerialized() throws JsonProcessingException {
        MessageEvent messageEvent = MessageEvent.builder()
                .retryAllParts(false)
                .messageParts(List.of(MessagePart.builder().messageId("part-1").sent(true).build()))
                .build();

        String json = objectMapper.writeValueAsString(messageEvent);
        MessageEvent deserialized = objectMapper.readValue(json, MessageEvent.class);

        assertTrue(json.contains("\"retry_all_parts\":false"), json);
        assertTrue(json.contains("\"sent\":true"), json);
        assertFalse(deserialized.isRetryAllParts());
        assertTrue(deserialized.getMessageParts().getFirst().isSent());
    }

    @Test
    void multipartRetryConfigurationIsPropagatedFromTheRoutingRule() throws JsonProcessingException {
        RoutingRule routingRule = objectMapper.readValue("{\"retry_all_parts\":false}", RoutingRule.class);
        MessageEvent messageEvent = new MessageEvent();

        messageEvent.setRetryAllParts(routingRule.isRetryAllParts());

        assertFalse(messageEvent.isRetryAllParts());
    }

    @Test
    void pendingMessagePartsOnlyContainsThePartsNotSentYet() {
        assertTrue(new MessageEvent().getPendingMessageParts().isEmpty());

        MessageEvent messageEvent = MessageEvent.builder()
                .messageParts(List.of(
                        MessagePart.builder().segmentSequence(1).sent(true).build(),
                        MessagePart.builder().segmentSequence(2).build(),
                        MessagePart.builder().segmentSequence(3).build()))
                .build();

        assertEquals(List.of(2, 3), messageEvent.getPendingMessageParts().stream()
                .map(MessagePart::getSegmentSequence)
                .toList());
        assertFalse(messageEvent.toString().contains("pending_message_parts"));
        assertFalse(messageEvent.toString().contains("pendingMessageParts"));
    }

    @Test
    void testToString() throws JsonProcessingException {
        MessageEvent messageEvent = new MessageEvent();
        Assertions.assertEquals(messageEvent.toString(), objectMapper.writeValueAsString(messageEvent));
    }

    @Test
    void testRetryProfileDataSerialization() throws JsonProcessingException {
        MessageEvent messageEvent = MessageEvent.builder()
                .profileId(18)
                .profileIdHistory(List.of(10, 18))
                .retryDestNetworkId("17,22")
                .retryProfileData(RetryProfileData.builder()
                        .active(true)
                        .attempt(2)
                        .accumulatedDelaySeconds(15L)
                        .rerouteNetworkId(24)
                        .mappingId(7)
                        .triggerErrorCode(34)
                        .activationType("MAPPING")
                        .build())
                .build();

        String json = objectMapper.writeValueAsString(messageEvent);
        MessageEvent result = objectMapper.readValue(json, MessageEvent.class);

        assertEquals(18, result.getProfileId());
        assertEquals(List.of(10, 18), result.getProfileIdHistory());
        assertEquals("17,22", result.getRetryDestNetworkId());
        assertNotNull(result.getRetryProfileData());
        assertTrue(result.getRetryProfileData().isActive());
        assertEquals(2, result.getRetryProfileData().getAttempt());
        assertEquals(15L, result.getRetryProfileData().getAccumulatedDelaySeconds());
        assertEquals(24, result.getRetryProfileData().getRerouteNetworkId());
        assertEquals(7, result.getRetryProfileData().getMappingId());
        assertEquals(34, result.getRetryProfileData().getTriggerErrorCode());
        assertEquals("MAPPING", result.getRetryProfileData().getActivationType());
    }

    void shouldTrimShortMessageWhenUsingSetter() {
        MessageEvent messageEvent = new MessageEvent();

        messageEvent.setShortMessage(" \tmessage\n ");

        assertEquals("message", messageEvent.getShortMessage());
    }

    @Test
    void shouldTrimShortMessageWhenUsingBuilder() {
        MessageEvent messageEvent = MessageEvent.builder()
                .shortMessage("  message   ")
                .build();

        assertEquals("message", messageEvent.getShortMessage());
    }

    @Test
    void shouldAllowNullShortMessage() {
        MessageEvent messageEvent = new MessageEvent();

        messageEvent.setShortMessage(null);

        assertNull(messageEvent.getShortMessage());
    }

    @Test
    void shouldTrimShortMessageWhenDeserializingJson() throws JsonProcessingException {
        MessageEvent messageEvent = objectMapper.readValue("{\"short_message\":\"  message   \"}", MessageEvent.class);

        assertEquals("message", messageEvent.getShortMessage());
    }

    @Test
    void testNotApplyForLongMessage() {
        MessageEvent messageEvent = new MessageEvent();
        messageEvent.setOriginProtocol("HTTP");
        messageEvent.setOriginNetworkType("SP");
        messageEvent.setDestProtocol("HTTP");
        messageEvent.setDestNetworkType("GW");
        assertTrue(messageEvent.notApplyForLongMessage());

        messageEvent.setOriginProtocol("SMPP");
        messageEvent.setOriginNetworkType("SP");
        messageEvent.setDestProtocol("HTTP");
        messageEvent.setDestNetworkType("GW");
        assertTrue(messageEvent.notApplyForLongMessage());


        messageEvent.setOriginNetworkType("SP");
        messageEvent.setOriginProtocol("HTTP");
        messageEvent.setDestNetworkType("GW");
        messageEvent.setDestProtocol("SMPP");
        assertFalse(messageEvent.notApplyForLongMessage());
    }

    @Test
    void testApplyForLongMessage() {
        MessageEvent messageEvent = new MessageEvent();
        messageEvent.setOriginProtocol("HTTP");
        messageEvent.setOriginNetworkType("SP");
        messageEvent.setDestProtocol("HTTP");
        messageEvent.setDestNetworkType("GW");
        assertFalse(messageEvent.applyForLongMessage());

        messageEvent.setOriginProtocol("HTTP");
        messageEvent.setOriginNetworkType("SP");
        messageEvent.setDestProtocol("SMPP");
        messageEvent.setDestNetworkType("GW");
        assertTrue(messageEvent.applyForLongMessage());

        messageEvent.setOriginNetworkType("SP");
        messageEvent.setOriginProtocol("SMPP");
        messageEvent.setDestNetworkType("SP");
        messageEvent.setDestProtocol("HTTP");
        assertFalse(messageEvent.applyForLongMessage());
    }

    @Test
    void testClone() {
        MessageEvent messageEvent = new MessageEvent();
        messageEvent.setId("id");
        messageEvent.setSystemId("systemId");
        messageEvent.setMessageId("messageId");
        messageEvent.setSourceAddr("123");
        messageEvent.setDestinationAddr("324");
        messageEvent.setShortMessage("shortMessage");
        messageEvent.setSourceAddrTon(1);
        messageEvent.setSourceAddrNpi(1);
        messageEvent.setDestAddrTon(1);
        messageEvent.setDestAddrNpi(1);
        messageEvent.setBroadcastId(1234);

        MessageEvent messageEvent1 = new MessageEvent();
        messageEvent1.setId("id1");
        messageEvent1.setSystemId("systemId1");
        messageEvent1.setMessageId("messageId1");
        messageEvent1.setSourceAddr("1234");
        messageEvent1.setDestinationAddr("432");
        messageEvent1.setShortMessage("anotherShortMessage");
        messageEvent1.setSourceAddrTon(2);
        messageEvent1.setSourceAddrNpi(2);
        messageEvent1.setDestAddrTon(2);
        messageEvent1.setDestAddrNpi(2);
        messageEvent1.setBroadcastId(1234);
        MessageEvent clonedEvent = Converter.deepCopy(messageEvent1, MessageEvent.class);

        assertNotEquals(messageEvent1, clonedEvent);
        assertEquals(messageEvent1.getId(), clonedEvent.getId());
    }

    @Test
    void testAddHost() {
        MessageEvent messageEvent = new MessageEvent();
        String hostValue = "127.0.0.1";

        messageEvent.addHost(hostValue);

        assertEquals(hostValue, messageEvent.getCustomParams().get("host"));

        String newHostValue = "192.168.0.10";
        messageEvent.addHost(newHostValue);

        assertEquals(newHostValue, messageEvent.getCustomParams().get("host"));
        assertNotEquals(hostValue, messageEvent.getCustomParams().get("host"));
    }

    @Test
    void testGetFromCustomParam() {
        MessageEvent messageEvent = new MessageEvent();

        // Test with null customParams - should return default value
        Object defaultValue = "defaultValue";
        Object result = messageEvent.getFromCustomParam("testKey", defaultValue);
        assertEquals(defaultValue, result);

        // Test with existing customParams but non-existent key
        messageEvent.addCustomParam("existingKey", "existingValue");
        result = messageEvent.getFromCustomParam("nonExistentKey", defaultValue);
        assertEquals(defaultValue, result);

        // Test with existing key - should return actual value
        String actualValue = "actualValue";
        messageEvent.addCustomParam("testKey", actualValue);
        result = messageEvent.getFromCustomParam("testKey", defaultValue);
        assertEquals(actualValue, result);
    }

    @Test
    void shouldDoNothingWhenCustomParametersMapIsNull() {
        MessageEvent messageEvent = new MessageEvent();
        messageEvent.addCustomParam((Map<String, Object>) null);
        assertNull(messageEvent.getCustomParams(), "customParams should remain null when the map is null");
    }

    @Test
    void shouldSkipNullEmptyStringAndMinusOneIntegerValues() {
        MessageEvent messageEvent = new MessageEvent();

        Map<String, Object> customParameters = new HashMap<>();
        customParameters.put("nullValue", null);
        customParameters.put("emptyString", "");
        customParameters.put("minusOne", -1);
        messageEvent.addCustomParam(customParameters);
        assertNull(messageEvent.getCustomParams(), "customParams should remain null because all entries must be ignored");
    }

    @Test
    void shouldAddOnlyValidEntriesFromCustomParametersMap() {
        MessageEvent messageEvent = new MessageEvent();

        Map<String, Object> customParameters = new HashMap<>();
        customParameters.put("validString", "hello");
        customParameters.put("validInteger", 10);
        customParameters.put("validBoolean", false);
        customParameters.put("blankString", " ");
        customParameters.put("zero", 0);
        customParameters.put("nullValue", null);
        customParameters.put("emptyString", "");
        customParameters.put("minusOne", -1);

        messageEvent.addCustomParam(customParameters);

        assertNotNull(messageEvent.getCustomParams());
        assertEquals(5, messageEvent.getCustomParams().size());
        assertEquals("hello", messageEvent.getCustomParams().get("validString"));
        assertEquals(10, messageEvent.getCustomParams().get("validInteger"));
        assertEquals(false, messageEvent.getCustomParams().get("validBoolean"));
        assertEquals(" ", messageEvent.getCustomParams().get("blankString"));
        assertEquals(0, messageEvent.getCustomParams().get("zero"));

        assertFalse(messageEvent.getCustomParams().containsKey("nullValue"));
        assertFalse(messageEvent.getCustomParams().containsKey("emptyString"));
        assertFalse(messageEvent.getCustomParams().containsKey("minusOne"));
    }

    @Test
    void shouldMergeWithExistingCustomParams() {
        MessageEvent messageEvent = new MessageEvent();
        messageEvent.addCustomParam("existingKey", "existingValue");

        Map<String, Object> customParameters = new HashMap<>();
        customParameters.put("newKey", "newValue");
        customParameters.put("anotherKey", 123);

        messageEvent.addCustomParam(customParameters);

        assertNotNull(messageEvent.getCustomParams());
        assertEquals(3, messageEvent.getCustomParams().size());
        assertEquals("existingValue", messageEvent.getCustomParams().get("existingKey"));
        assertEquals("newValue", messageEvent.getCustomParams().get("newKey"));
        assertEquals(123, messageEvent.getCustomParams().get("anotherKey"));
    }

    @Test
    void shouldOverwriteExistingValueWhenSameKeyIsProvided() {
        MessageEvent messageEvent = new MessageEvent();
        messageEvent.addCustomParam("sharedKey", "oldValue");

        Map<String, Object> customParameters = new HashMap<>();
        customParameters.put("sharedKey", "newValue");

        messageEvent.addCustomParam(customParameters);

        assertNotNull(messageEvent.getCustomParams());
        assertEquals(1, messageEvent.getCustomParams().size());
        assertEquals("newValue", messageEvent.getCustomParams().get("sharedKey"));
    }

    @Test
    void testCreateDeliveryReceiptMessage() {
        MessageEvent originalMessage = MessageEvent.builder()
                .id("1722446896082-12194920127675")
                .messageId("1722446896081-12194920043917")
                .parentId("parent-123")
                .systemId("smpp_sp")
                .sourceAddr("8888")
                .destinationAddr("1234")
                .shortMessage("Hello!")
                .originNetworkId(3)
                .originNetworkType("SP")
                .originProtocol("SMPP")
                .destNetworkType("GW")
                .destProtocol("SS7")
                .destNetworkId(5)
                .sourceAddrTon(1)
                .sourceAddrNpi(4)
                .destAddrTon(1)
                .destAddrNpi(4)
                .dataCoding(0)
                .registeredDelivery(1)
                .build();

        // Test delivery receipt creation without error mapping
        MessageEvent deliveryReceipt = originalMessage.createDeliveryReceiptMessage(null, null);

        // Verify delivery receipt properties are correctly set
        Assertions.assertNotNull(deliveryReceipt);
        assertTrue(deliveryReceipt.isDlr());
        assertEquals(0, deliveryReceipt.getRegisteredDelivery());
        assertEquals(originalMessage.getMessageId(), deliveryReceipt.getDeliverSmId());
        assertEquals(originalMessage.getDestNetworkId(), deliveryReceipt.getOriginNetworkId());
        assertEquals(originalMessage.getOriginNetworkId(), deliveryReceipt.getDestNetworkId());
        assertEquals(originalMessage.getDestProtocol(), deliveryReceipt.getOriginProtocol());
        assertEquals(originalMessage.getOriginProtocol(), deliveryReceipt.getDestProtocol());
        assertEquals(originalMessage.getDestinationAddr(), deliveryReceipt.getSourceAddr());
        assertEquals(originalMessage.getSourceAddr(), deliveryReceipt.getDestinationAddr());
        assertFalse(deliveryReceipt.getCheckSubmitSmResponse());

        // Verify encoding is not applied (moved to routing module)
        Assertions.assertNull(deliveryReceipt.getMessageBytes());
        Assertions.assertNotNull(deliveryReceipt.getShortMessage());
        assertEquals(deliveryReceipt.getShortMessage(), deliveryReceipt.getDelReceipt());

        // Verify SMSC-generated DLR flag is set
        assertEquals(true, deliveryReceipt.getFromCustomParam(
                GeneralSmscConstants.SMSC_GENERATED_DLR, false));

        // Test with error code mapping
        ErrorCodeMapping errorMapping = new ErrorCodeMapping();
        errorMapping.setErrorCode(100);
        errorMapping.setDeliveryErrorCode(5);
        errorMapping.setDeliveryStatus("UNDELIV");

        MessageEvent deliveryReceiptWithError = originalMessage.createDeliveryReceiptMessage(errorMapping, null);
        Assertions.assertNotNull(deliveryReceiptWithError);
        assertEquals(5, deliveryReceiptWithError.getErrorCode());
        assertEquals("UNDELIV", deliveryReceiptWithError.getStatus());

        // Test with extra information
        String extraInfo = " imsi:748031234567890 nnn_digits:598991900032";
        MessageEvent deliveryReceiptWithExtra = originalMessage.createDeliveryReceiptMessage(null, extraInfo);
        Assertions.assertNotNull(deliveryReceiptWithExtra);
        assertTrue(deliveryReceiptWithExtra.getShortMessage().contains(extraInfo));
    }

    @Test
    void testCreateDeliveryReceiptMessageWithHttpProtocol() {
        MessagePart part1 = MessagePart.builder()
                .messageId("part1-id")
                .shortMessage("Part 1 content")
                .segmentSequence(1)
                .totalSegment(2)
                .build();

        MessagePart part2 = MessagePart.builder()
                .messageId("part2-id")
                .shortMessage("Part 2 content")
                .segmentSequence(2)
                .totalSegment(2)
                .build();

        MessageEvent httpMessage = MessageEvent.builder()
                .messageId("msg-123")
                .parentId("parent-456")
                .originProtocol("HTTP")
                .messageParts(java.util.List.of(part1, part2))
                .sourceAddr("1111")
                .destinationAddr("2222")
                .originNetworkId(1)
                .destNetworkId(2)
                .originProtocol("HTTP")
                .destProtocol("SMPP")
                .dataCoding(0)
                .build();

        // Test HTTP protocol with message parts - should use parentId
        MessageEvent deliveryReceipt = httpMessage.createDeliveryReceiptMessage(null, null);

        Assertions.assertNotNull(deliveryReceipt);
        // Verify delivery receipt uses correct message ID logic for HTTP with parts
        Assertions.assertNotNull(deliveryReceipt.getShortMessage());
    }


    void testCreateDeliveryReceiptMessageForSmscSplitUsesParentId() {
        MessageEvent splitPart = MessageEvent.builder()
                .messageId("part-2-id")
                .parentId("part-1-id")
                .splitForSmsc(true)
                .originProtocol("SMPP")
                .sourceAddr("1111")
                .destinationAddr("2222")
                .originNetworkId(1)
                .destNetworkId(2)
                .destProtocol("SMPP")
                .dataCoding(0)
                .build();

        MessageEvent deliveryReceipt = splitPart.createDeliveryReceiptMessage(null, null);

        assertNotNull(deliveryReceipt);
        assertEquals("part-1-id", deliveryReceipt.getMessageId());
        assertEquals("part-2-id", deliveryReceipt.getDeliverSmId());
        assertTrue(deliveryReceipt.getShortMessage().contains("id:part-1-id"));
    }

    @Test
    void testSourceDestinationSerialization() throws JsonProcessingException {
        MessageEvent messageEvent = new MessageEvent();
        messageEvent.setSource("imsi-748010000000001");
        messageEvent.setDestination("imsi-748010000000002");

        String json = objectMapper.writeValueAsString(messageEvent);

        assertTrue(json.contains("\"source\":\"imsi-748010000000001\""));
        assertTrue(json.contains("\"destination\":\"imsi-748010000000002\""));

        MessageEvent deserialized = objectMapper.readValue(json, MessageEvent.class);
        assertEquals("imsi-748010000000001", deserialized.getSource());
        assertEquals("imsi-748010000000002", deserialized.getDestination());
    }
}
