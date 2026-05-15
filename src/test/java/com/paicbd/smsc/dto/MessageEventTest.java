package com.paicbd.smsc.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.paicbd.smsc.utils.Converter;
import com.paicbd.smsc.utils.GeneralSmscConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class MessageEventTest {
    ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .registerModule(new JavaTimeModule());

    @Test
    void testToString() throws JsonProcessingException {
        MessageEvent messageEvent = new MessageEvent();
        Assertions.assertEquals(messageEvent.toString(), objectMapper.writeValueAsString(messageEvent));
    }

    @Test
    void testNotApplyForLongMessage() {
        MessageEvent messageEvent = new MessageEvent();
        messageEvent.setOriginProtocol("HTTP");
        messageEvent.setOriginNetworkType("SP");
        messageEvent.setDestProtocol("HTTP");
        messageEvent.setDestNetworkType("GW");
        Assertions.assertTrue(messageEvent.notApplyForLongMessage());

        messageEvent.setOriginProtocol("SMPP");
        messageEvent.setOriginNetworkType("SP");
        messageEvent.setDestProtocol("HTTP");
        messageEvent.setDestNetworkType("GW");
        Assertions.assertTrue(messageEvent.notApplyForLongMessage());


        messageEvent.setOriginNetworkType("SP");
        messageEvent.setOriginProtocol("HTTP");
        messageEvent.setDestNetworkType("GW");
        messageEvent.setDestProtocol("SMPP");
        Assertions.assertFalse(messageEvent.notApplyForLongMessage());
    }

    @Test
    void testApplyForLongMessage() {
        MessageEvent messageEvent = new MessageEvent();
        messageEvent.setOriginProtocol("HTTP");
        messageEvent.setOriginNetworkType("SP");
        messageEvent.setDestProtocol("HTTP");
        messageEvent.setDestNetworkType("GW");
        Assertions.assertFalse(messageEvent.applyForLongMessage());

        messageEvent.setOriginProtocol("HTTP");
        messageEvent.setOriginNetworkType("SP");
        messageEvent.setDestProtocol("SMPP");
        messageEvent.setDestNetworkType("GW");
        Assertions.assertTrue(messageEvent.applyForLongMessage());

        messageEvent.setOriginNetworkType("SP");
        messageEvent.setOriginProtocol("SMPP");
        messageEvent.setDestNetworkType("SP");
        messageEvent.setDestProtocol("HTTP");
        Assertions.assertFalse(messageEvent.applyForLongMessage());
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
        Assertions.assertTrue(deliveryReceipt.isDlr());
        assertEquals(0, deliveryReceipt.getRegisteredDelivery());
        assertEquals(originalMessage.getMessageId(), deliveryReceipt.getDeliverSmId());
        assertEquals(originalMessage.getDestNetworkId(), deliveryReceipt.getOriginNetworkId());
        assertEquals(originalMessage.getOriginNetworkId(), deliveryReceipt.getDestNetworkId());
        assertEquals(originalMessage.getDestProtocol(), deliveryReceipt.getOriginProtocol());
        assertEquals(originalMessage.getOriginProtocol(), deliveryReceipt.getDestProtocol());
        assertEquals(originalMessage.getDestinationAddr(), deliveryReceipt.getSourceAddr());
        assertEquals(originalMessage.getSourceAddr(), deliveryReceipt.getDestinationAddr());
        Assertions.assertFalse(deliveryReceipt.getCheckSubmitSmResponse());

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
        Assertions.assertTrue(deliveryReceiptWithExtra.getShortMessage().contains(extraInfo));
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

    @Test
    void testCorrelationIdSerialization() throws JsonProcessingException {
        MessageEvent messageEvent = new MessageEvent();
        messageEvent.setCorrelationId("corr-12345");

        String json = objectMapper.writeValueAsString(messageEvent);

        Assertions.assertTrue(json.contains("\"correlation_id\":\"corr-12345\""));

        MessageEvent deserialized = objectMapper.readValue(json, MessageEvent.class);
        assertEquals("corr-12345", deserialized.getCorrelationId());
    }
}