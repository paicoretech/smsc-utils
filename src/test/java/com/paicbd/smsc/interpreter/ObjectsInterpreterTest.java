package com.paicbd.smsc.interpreter;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paicbd.smsc.dto.MessageEvent;
import com.paicbd.smsc.dto.UtilsRecords;
import com.paicbd.smsc.exception.SerializationException;
import com.paicbd.smsc.utils.Converter;
import jakarta.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObjectsInterpreterTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    @DisplayName("Test converting JSON to XML")
    void complexXmlStringToMessageEvent() {
        String xml = """
                <messageEvent>
                    <aboutSms>
                        <uniqueId>14asfS</uniqueId>
                        <sourceAddrTypeOfNumber>1</sourceAddrTypeOfNumber>
                        <sourceAddrNumberingPlanIndicator>1</sourceAddrNumberingPlanIndicator>
                    </aboutSms>
                    <sourceAddr>8877</sourceAddr>
                    <destAddr>1800</destAddr>
                </messageEvent>
                """;

        Stream<FieldMapping> fieldMappings = Stream.of(
                new FieldMapping("aboutSms.uniqueId", "messageId", DataType.STRING),
                new FieldMapping("aboutSms.sourceAddrTypeOfNumber", "sourceAddrTon", DataType.INT),
                new FieldMapping("aboutSms.sourceAddrNumberingPlanIndicator", "sourceAddrNpi", DataType.INT),
                new FieldMapping("sourceAddr", "sourceAddr", DataType.STRING),
                new FieldMapping("destAddr", "destinationAddr", DataType.STRING)
        );

        JsonNode jsonNode = ObjectsInterpreter.xmlStringToJsonNode(xml);
        String stringJsonMessageEvent = ObjectsInterpreter
                .jsonNodeToSerializedString(jsonNode, fieldMappings, MessageEvent.class, PayloadFormat.JSON, null);
        MessageEvent event = Converter.stringToObject(stringJsonMessageEvent, MessageEvent.class);
        assertNotNull(event);

        assertEquals("14asfS", event.getMessageId());
        assertEquals(1, event.getSourceAddrTon());
        assertEquals(1, event.getSourceAddrNpi());
        assertEquals("8877", event.getSourceAddr());
        assertEquals("1800", event.getDestinationAddr());
    }

    @Test
    @DisplayName("Test converting JSON to XML")
    void complexStringToJsonNode() throws JsonProcessingException {
        MessageEvent messageEvent = MessageEvent.builder()
                .messageId("mId09jLwEE")
                .sourceAddrTon(1)
                .sourceAddrNpi(1)
                .sourceAddr("1234567")
                .destAddrTon(1)
                .destAddrNpi(1)
                .destinationAddr("123456")
                .dataCoding(1)
                .status("OK")
                .shortMessage("Hello World")
                .esmClass(64)
                .build();

        JsonNode stringMessageEvent = ObjectsInterpreter.objectToCamelCaseJsonNode(messageEvent);
        assertNotNull(stringMessageEvent);

        Stream<FieldMapping> fieldMappings = Stream.of(
                new FieldMapping("commandId", "commandId", DataType.INT),
                new FieldMapping("sourceAddrTon", "source.sourceAddrTon", DataType.BYTE),
                new FieldMapping("sourceAddrNpi", "source.sourceAddrNpi", DataType.BYTE),
                new FieldMapping("sourceAddr", "source.sourceAddr", DataType.STRING),
                new FieldMapping("destAddrTon", "dest.destAddrTon", DataType.BYTE),
                new FieldMapping("destAddrNpi", "dest.destAddrNpi", DataType.BYTE),
                new FieldMapping("destinationAddr", "dest.destinationAddr", DataType.STRING),
                new FieldMapping("dataCoding", "dataCoding", DataType.STRING),
                new FieldMapping("shortMessage", "text", DataType.STRING),
                new FieldMapping("esmClass", "esmClass", DataType.BYTE),
                new FieldMapping("status", "status", DataType.STRING),
                new FieldMapping("messageId", "messageId", DataType.STRING)
        );

        String stringJsonMessageEvent = ObjectsInterpreter.jsonNodeToSerializedString(
                stringMessageEvent, fieldMappings, JsonNode.class, PayloadFormat.JSON, null);
        assertNotNull(stringJsonMessageEvent);

        JsonNode resultJsonNode = OBJECT_MAPPER.readTree(stringJsonMessageEvent);
        assertNotNull(resultJsonNode);

        assertEquals("mId09jLwEE", resultJsonNode.get("messageId").asText());
        assertEquals("0x01", resultJsonNode.get("source").get("sourceAddrTon").asText());
        assertEquals("0x01", resultJsonNode.get("source").get("sourceAddrNpi").asText());
        assertEquals("1234567", resultJsonNode.get("source").get("sourceAddr").asText());
        assertEquals("0x01", resultJsonNode.get("dest").get("destAddrTon").asText());
        assertEquals("0x01", resultJsonNode.get("dest").get("destAddrNpi").asText());
        assertEquals("123456", resultJsonNode.get("dest").get("destinationAddr").asText());
        assertEquals("1", resultJsonNode.get("dataCoding").asText());
        assertEquals("0", resultJsonNode.get("commandId").asText());
        assertEquals("Hello World", resultJsonNode.get("text").asText());
        assertEquals("0x40", resultJsonNode.get("esmClass").asText()); // 64 is equal to 0x40
        assertEquals("OK", resultJsonNode.get("status").asText());

    }

    @Test
    @DisplayName("Test converting Complex XML to JSON")
    void complexXmlStringToMessage() {
        String xmlString = """
                <smpp>
                	<commandId>4</commandId>
                	<commandLength>47</commandLength>
                	<sequenceNumber>6</sequenceNumber>
                	<sourceAddress>
                		<address>6666</address>
                		<ton>0x01</ton>
                		<npi>1</npi>
                	</sourceAddress>
                	<destAddress>
                		<address>5555</address>
                		<ton>0X01</ton>
                		<npi>0x01</npi>
                	</destAddress>
                	<dataCoding>0x00</dataCoding>
                	<protocolId>0x00</protocolId>
                	<priority>0x00</priority>
                	<registerDelivery>0x01</registerDelivery>
                	<replaceIfPresent>0x00</replaceIfPresent>
                	<messageLength>6</messageLength>
                	<message>48656C6C6F21</message>
                	<clientId>server1-test1</clientId>
                	<host>127.0.0.1</host>
                	<esmClass>0x40</esmClass>
                </smpp>
                """;

        Stream<FieldMapping> fieldMappings = Stream.of(
                new FieldMapping("commandId", "commandId", DataType.INT),
                new FieldMapping("commandLength", "commandLength", DataType.INT),
                new FieldMapping("sequenceNumber", "sequenceNumber", DataType.INT),
                new FieldMapping("serviceType", "serviceType", DataType.STRING),
                new FieldMapping("sourceAddress.address", "sourceAddr", DataType.STRING),
                new FieldMapping("sourceAddress.ton", "sourceAddrTon", DataType.BYTE),
                new FieldMapping("sourceAddress.npi", "sourceAddrNpi", DataType.BYTE),
                new FieldMapping("destAddress.address", "destinationAddr", DataType.STRING),
                new FieldMapping("destAddress.ton", "destAddrTon", DataType.BYTE),
                new FieldMapping("destAddress.npi", "destAddrNpi", DataType.BYTE),
                new FieldMapping("scheduleDeliveryTime", "scheduleDeliveryTime", DataType.STRING),
                new FieldMapping("validityPeriod", "validityPeriod", DataType.STRING),
                new FieldMapping("dataCoding", "dataCoding", DataType.BYTE),
                new FieldMapping("protocolId", "protocolId", DataType.STRING),
                new FieldMapping("priority", "priority", DataType.STRING),
                new FieldMapping("registerDelivery", "registeredDelivery", DataType.BYTE),
                new FieldMapping("replaceIfPresent", "replaceIfPresent", DataType.STRING),
                new FieldMapping("messageLength", "messageLength", DataType.INT),
                new FieldMapping("message", "shortMessage", DataType.STRING),
                new FieldMapping("clientId", "systemId", DataType.STRING),
                new FieldMapping("esmClass", "esmClass", DataType.BYTE)
        );

        JsonNode jsonNode = ObjectsInterpreter.xmlStringToJsonNode(xmlString);

        String stringJsonMessage = ObjectsInterpreter
                .jsonNodeToSerializedString(jsonNode, fieldMappings, MessageEvent.class, PayloadFormat.JSON, null);

        assertNotNull(stringJsonMessage);
        MessageEvent event = Converter.stringToObject(stringJsonMessage, MessageEvent.class);
        assertNotNull(event);

        assertEquals("6666", event.getSourceAddr());
        assertEquals(1, event.getSourceAddrTon());
        assertEquals(1, event.getSourceAddrNpi());

        assertEquals("5555", event.getDestinationAddr());
        assertEquals(1, event.getDestAddrTon());
        assertEquals(1, event.getDestAddrNpi());

        assertEquals(0, event.getDataCoding());
        assertEquals(1, event.getRegisteredDelivery());
        assertEquals("48656C6C6F21", event.getShortMessage());
        assertEquals("server1-test1", event.getSystemId());
        assertEquals(64, event.getEsmClass());
    }

    @Test
    void complexXmlStringToJsonNode() {
        String xml = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <dialog mapMessagesSize="1" userObject="{{randomString}}">
                  <unstructuredSSRequest_Request dataCodingScheme="15" string="Paquete Sin Limite Activo&#xA;Saldo: $1 al 01/03/24&#xA;&#xA;1. Recargas&#xA;2. Paquetes&#xA;3. Adelantos&#xA;4. Servicios sin saldo&#xA;5. Detalle de saldo">
                    <msisdn nai="international_number" npi="ISDN" number="525532368999"/>
                    <ussdString>1</ussdString>
                  </unstructuredSSRequest_Request>
                </dialog>
                """;
        JsonNode jsonNode = ObjectsInterpreter.xmlStringToJsonNode(xml);
        assertNotNull(jsonNode);
    }

    @Test
    @DisplayName("Test converting MessageEvent to XML")
    void messageEventToXmlStringObject() {
        MessageEvent messageEvent = MessageEvent.builder()
                .messageId("mId09jLwEE")
                .sourceAddrTon(1)
                .sourceAddrNpi(1)
                .sourceAddr("123456")
                .destAddrTon(1)
                .destAddrNpi(1)
                .destinationAddr("678")
                .dataCoding(1)
                .status("OK")
                .shortMessage("Hello World")
                .esmClass(64)
                .delReceipt("1")
                .build();

        JsonNode stringMessageEvent = ObjectsInterpreter.objectToCamelCaseJsonNode(messageEvent);
        assertNotNull(stringMessageEvent);

        Stream<FieldMapping> fieldMappings = Stream.of(
                new FieldMapping("messageId", "messageId", DataType.STRING),
                new FieldMapping("commandId", "commandId", DataType.INT),
                new FieldMapping("sourceAddrTon", "source.sourceAddrTon", DataType.BYTE),
                new FieldMapping("sourceAddrNpi", "source.sourceAddrNpi", DataType.BYTE),
                new FieldMapping("sourceAddr", "source.sourceAddr", DataType.STRING),
                new FieldMapping("destAddrTon", "dest.destAddrTon", DataType.INT),
                new FieldMapping("destAddrNpi", "dest.destAddrNpi", DataType.INT),
                new FieldMapping("destinationAddr", "dest.destinationAddr", DataType.STRING),
                new FieldMapping("dataCoding", "dataCoding", DataType.STRING),
                new FieldMapping("shortMessage", "text", DataType.STRING),
                new FieldMapping("delReceipt", "dRec", DataType.BYTE)
        );

        String xmlStringEvent = ObjectsInterpreter.jsonNodeToSerializedString(
                stringMessageEvent, fieldMappings, JsonNode.class, PayloadFormat.XML, "sms");
        assertNotNull(xmlStringEvent);

        JsonNode resultJsonNode = ObjectsInterpreter.xmlStringToJsonNode(xmlStringEvent);
        assertNotNull(resultJsonNode);

        assertEquals("mId09jLwEE", resultJsonNode.get("messageId").asText());
        assertEquals("0x01", resultJsonNode.get("source").get("sourceAddrTon").asText());
        assertEquals("0x01", resultJsonNode.get("source").get("sourceAddrNpi").asText());
        assertEquals("123456", resultJsonNode.get("source").get("sourceAddr").asText());
        assertEquals(1, resultJsonNode.get("dest").get("destAddrTon").asInt());
        assertEquals(1, resultJsonNode.get("dest").get("destAddrNpi").asInt());
        assertEquals("678", resultJsonNode.get("dest").get("destinationAddr").asText());
        assertEquals("1", resultJsonNode.get("dataCoding").asText());
        assertEquals("Hello World", resultJsonNode.get("text").asText());
        assertEquals("0x01", resultJsonNode.get("dRec").asText());
    }

    @Test
    @DisplayName("Test converting DLR Request to JSON")
    void recordToMessageEvent() throws JsonProcessingException {
        DlrRequest dlrRequest = new DlrRequest(
                "A1C2D3FG",
                3,
                2,
                "1111",
                2,
                3,
                "2222",
                64,
                "error",
                "11",
                List.of(new UtilsRecords.OptionalParameter((short) 1, "SAD"))
        );

        JsonNode jsonNode = ObjectsInterpreter.objectToCamelCaseJsonNode(dlrRequest);
        assertNotNull(jsonNode);

        Stream<FieldMapping> fieldMappingsForCastRecordToJsonNode = Stream.of(
                new FieldMapping("messageId", "mid", DataType.STRING),
                new FieldMapping("sourceAddrTon", "saTON", DataType.INT),
                new FieldMapping("sourceAddrNpi", "saNPI", DataType.INT),
                new FieldMapping("sourceAddr", "sa", DataType.STRING),
                new FieldMapping("destAddrTon", "daTON", DataType.INT),
                new FieldMapping("destAddrNpi", "daNPI", DataType.INT),
                new FieldMapping("destinationAddr", "da", DataType.STRING),
                new FieldMapping("dataCoding", "dc", DataType.INT),
                new FieldMapping("status", "stat", DataType.STRING),
                new FieldMapping("errorCode", "ec", DataType.STRING),
                new FieldMapping("optionalParameters", "op", DataType.LIST)
        );

        String stringJsonMessage = ObjectsInterpreter.jsonNodeToSerializedString(
                jsonNode, fieldMappingsForCastRecordToJsonNode, JsonNode.class, PayloadFormat.JSON, null);

        assertNotNull(stringJsonMessage);

        JsonNode resultJsonNode = OBJECT_MAPPER.readTree(stringJsonMessage);
        assertNotNull(resultJsonNode);
        executeAssertionsForResultJsonNode(resultJsonNode);

        Stream<FieldMapping> fieldMappingsForCastJsonNodeToRecord = Stream.of(
                new FieldMapping("mid", "messageId", DataType.STRING),
                new FieldMapping("saTON", "sourceAddrTon", DataType.INT),
                new FieldMapping("saNPI", "sourceAddrNpi", DataType.INT),
                new FieldMapping("sa", "sourceAddr", DataType.STRING),
                new FieldMapping("daTON", "destAddrTon", DataType.LONG),
                new FieldMapping("daNPI", "destAddrNpi", DataType.DOUBLE),
                new FieldMapping("da", "destinationAddr", DataType.STRING),
                new FieldMapping("dc", "dataCoding", DataType.INT),
                new FieldMapping("stat", "status", DataType.STRING),
                new FieldMapping("ec", "errorCode", DataType.STRING),
                new FieldMapping("op", "optionalParameters", DataType.LIST)
        );

        String resultedStringDlrRequest = ObjectsInterpreter.jsonNodeToSerializedString(
                resultJsonNode, fieldMappingsForCastJsonNodeToRecord, DlrRequest.class, PayloadFormat.JSON, null);
        assertNotNull(resultedStringDlrRequest);
        DlrRequest newDlrRequest = OBJECT_MAPPER.readValue(resultedStringDlrRequest, DlrRequest.class);
        assertNotNull(newDlrRequest);

        executeAssertionsForNewDlrRequest(newDlrRequest);
    }

    @Test
    @DisplayName("Test converting JSON to XML")
    void testClazzToJsonNode() throws JsonProcessingException {
        String json = """
                {
                    "value": 1,
                    "name": "name",
                    "age": 1,
                    "id": 1,
                    "isTrue": true,
                    "": "empty"
                }
                """;

        JsonNode jsonNode = new ObjectMapper().readTree(json);
        assertNotNull(jsonNode);

        Stream<FieldMapping> fieldMappings = Stream.of(
                new FieldMapping("value", "value", DataType.INT),
                new FieldMapping("name", "name", DataType.STRING),
                new FieldMapping("age", "age", DataType.DOUBLE),
                new FieldMapping("id", "id", DataType.INT),
                new FieldMapping("isTrue", "isTrue", DataType.BOOLEAN),
                new FieldMapping("", "ls", DataType.STRING),
                new FieldMapping(null, "null", DataType.STRING)
        );

        String stringJson = ObjectsInterpreter.jsonNodeToSerializedString(
                jsonNode, fieldMappings, TestClazz.class, PayloadFormat.JSON, null);
        assertNotNull(stringJson);

        JsonNode resultJsonNode = OBJECT_MAPPER.readTree(stringJson);
        assertEquals(1, resultJsonNode.get("value").asDouble());
        assertEquals("name", resultJsonNode.get("name").asText());
        assertEquals(1, resultJsonNode.get("age").asInt());
        assertEquals(1, resultJsonNode.get("id").asLong());
    }

    @Test
    @DisplayName("Test converting JSON to XML using custom parameters")
    void customParams() throws JsonProcessingException {
        MessageEvent messageEvent = MessageEvent.builder()
                .messageId("mId09jLwEE")
                .sourceAddrTon(1)
                .sourceAddrNpi(1)
                .sourceAddr("123456")
                .destAddrTon(1)
                .destAddrNpi(1)
                .destinationAddr("678")
                .customParams(Map.of("key1", "value1", "key2", "value2"))
                .build();

        JsonNode stringMessageEvent = ObjectsInterpreter.objectToCamelCaseJsonNode(messageEvent);
        Stream<FieldMapping> stream = Stream.of(
                new FieldMapping("messageId", "targetMessageId", DataType.STRING),
                new FieldMapping("sourceAddrTon", "sms.source.addrTon", DataType.BYTE),
                new FieldMapping("sourceAddrNpi", "sms.source.addrNpi", DataType.BYTE),
                new FieldMapping("sourceAddr", "sms.source.addr", DataType.STRING),
                new FieldMapping("destAddrTon", "sms.dest.addrTon", DataType.BYTE),
                new FieldMapping("destAddrNpi", "sms.dest.addrNpi", DataType.BYTE),
                new FieldMapping("destinationAddr", "sms.dest.addr", DataType.STRING),
                new FieldMapping("customParams.key1", "sms.op1", DataType.STRING),
                new FieldMapping("customParams.key2", "sms.op2", DataType.STRING),
                new FieldMapping("ls", "ls", DataType.STRING)
        );

        String stringJsonMessageEvent = ObjectsInterpreter.jsonNodeToSerializedString(
                stringMessageEvent, stream, JsonNode.class, PayloadFormat.JSON, null);

        assertNotNull(stringJsonMessageEvent);

        JsonNode resultJsonNode = OBJECT_MAPPER.readTree(stringJsonMessageEvent);
        assertNotNull(resultJsonNode);

        assertEquals("mId09jLwEE", resultJsonNode.get("targetMessageId").asText());
        assertEquals("0x01", resultJsonNode.get("sms").get("source").get("addrTon").asText());
        assertEquals("0x01", resultJsonNode.get("sms").get("source").get("addrNpi").asText());
        assertEquals("123456", resultJsonNode.get("sms").get("source").get("addr").asText());
        assertEquals("0x01", resultJsonNode.get("sms").get("dest").get("addrTon").asText());
        assertEquals("0x01", resultJsonNode.get("sms").get("dest").get("addrNpi").asText());
        assertEquals("678", resultJsonNode.get("sms").get("dest").get("addr").asText());
        assertEquals("value1", resultJsonNode.get("sms").get("op1").asText());
        assertEquals("value2", resultJsonNode.get("sms").get("op2").asText());
        assertTrue(resultJsonNode.has("ls"));
        assertEquals("", resultJsonNode.get("ls").asText());
    }

    @Test
    @DisplayName("Test converting JSON to XML using data types Boolean, Long, and Double")
    void toJsonNodeUsingBooleanLongAndDouble() throws JsonProcessingException {
        String json = """
                {
                    "value": 1,
                    "name": "name",
                    "age": 1,
                    "id": 1,
                    "forVerification": true
                }
                """;

        JsonNode jsonNode = new ObjectMapper().readTree(json);
        assertNotNull(jsonNode);

        Stream<FieldMapping> fieldMappings = Stream.of(
                new FieldMapping("value", "value", DataType.LONG),
                new FieldMapping("name", "name", DataType.STRING),
                new FieldMapping("age", "age", DataType.DOUBLE),
                new FieldMapping("id", "id", DataType.INT),
                new FieldMapping("forVerification", "isTrue", DataType.BOOLEAN)
        );

        String stringJson = ObjectsInterpreter.jsonNodeToSerializedString(
                jsonNode, fieldMappings, JsonNode.class, PayloadFormat.JSON, null);
        assertNotNull(stringJson);

        JsonNode resultJsonNode = OBJECT_MAPPER.readTree(stringJson);
        assertEquals(1, resultJsonNode.get("value").asLong());
        assertEquals("name", resultJsonNode.get("name").asText());
        assertEquals(1, resultJsonNode.get("age").asDouble());
        assertEquals(1, resultJsonNode.get("id").asInt());
        assertTrue(resultJsonNode.get("isTrue").asBoolean());
    }

    @Test
    @DisplayName("Test converting JSON to XML with invalid cast value")
    void invalidCastValue() throws JsonProcessingException {
        String json = """
                {
                    "value": "1",
                    "age": 1,
                    "id": 1,
                    "forVerification": true
                }
                """;

        Stream<FieldMapping> fieldMappings = Stream.of(
                new FieldMapping("value", "value", DataType.LONG),
                new FieldMapping("name", "name", DataType.STRING),
                new FieldMapping("age", "age", DataType.DOUBLE),
                new FieldMapping("id", "id", DataType.INT),
                new FieldMapping("forVerification", "isTrue", DataType.BOOLEAN)
        );

        JsonNode jsonNode = OBJECT_MAPPER.readTree(json);

        String stringJson = ObjectsInterpreter.jsonNodeToSerializedString(
                jsonNode, fieldMappings, JsonNode.class, PayloadFormat.JSON, null);
        assertNotNull(stringJson);
    }

    @Test
    @DisplayName("Test converting JSON to XML with invalid cast value, forcing null result")
    void xmlToJsonNodeWithError() {
        assertNull(ObjectsInterpreter.xmlStringToJsonNode(null));
    }

    @Test
    @DisplayName("Test converting JSON to XML with invalid cast value, forcing the exception")
    void malformedJsonToXml() {
        assertThrows(SerializationException.class, () -> ObjectsInterpreter.convertJsonToXml(null, null));
    }

    @Test
    @DisplayName("Test converting JSON to XML with invalid cast value for list")
    void arrayToListWithInvalidDataValue() throws JsonProcessingException {
        String json = """
                {
                    "value": 2,
                    "name": "name",
                    "age": 1
                }
                """;

        JsonNode jsonNode = new ObjectMapper().readTree(json);
        assertNotNull(jsonNode);

        Stream<FieldMapping> fieldMappings = Stream.of(
                new FieldMapping("value", "value", DataType.LIST),
                new FieldMapping("name", "name", DataType.STRING),
                new FieldMapping("age", "age", DataType.INT)
        );

        String stringJson = ObjectsInterpreter.jsonNodeToSerializedString(
                jsonNode, fieldMappings, ArrayRecord.class, PayloadFormat.JSON, null);
        assertNotNull(stringJson);

        JsonNode resultJsonNode = OBJECT_MAPPER.readTree(stringJson);
        assertEquals(0, resultJsonNode.get("value").size()); // Will be fail because trying to cast integer to list
        assertEquals("name", resultJsonNode.get("name").asText());
        assertEquals(1, resultJsonNode.get("age").asInt());
    }

    @Test
    @DisplayName("Test converting JSON to XML with invalid cast value for list and using record")
    void arrayToListWithInvalidDataValueList() throws JsonProcessingException {
        String json = """
                {
                    "value": null,
                    "name": "name",
                    "age": 1
                }
                """;

        JsonNode jsonNode = new ObjectMapper().readTree(json);
        assertNotNull(jsonNode);

        Stream<FieldMapping> fieldMappings = Stream.of(
                new FieldMapping("value", "value", DataType.LIST),
                new FieldMapping("name", "name", DataType.STRING),
                new FieldMapping("age", "age", DataType.INT)
        );

        String stringJson = ObjectsInterpreter.jsonNodeToSerializedString(
                jsonNode, fieldMappings, ArrayRecord.class, PayloadFormat.JSON, null);
        assertNotNull(stringJson);

        JsonNode resultJsonNode = OBJECT_MAPPER.readTree(stringJson);
        assertFalse(resultJsonNode.has("value"));
        assertEquals("name", resultJsonNode.get("name").asText());
        assertEquals(1, resultJsonNode.get("age").asInt());
    }

    record ArrayRecord(
            List<Integer> value,
            String name,
            int age) {
    }

    void executeAssertionsForResultJsonNode(JsonNode resultJsonNode) {
        assertEquals("A1C2D3FG", resultJsonNode.get("mid").asText());
        assertEquals(3, resultJsonNode.get("saTON").asInt());
        assertEquals(2, resultJsonNode.get("saNPI").asInt());
        assertEquals("1111", resultJsonNode.get("sa").asText());
        assertEquals(2, resultJsonNode.get("daTON").asInt());
        assertEquals(3, resultJsonNode.get("daNPI").asInt());
        assertEquals("2222", resultJsonNode.get("da").asText());
        assertEquals(64, resultJsonNode.get("dc").asInt());
        assertEquals("error", resultJsonNode.get("stat").asText());
        assertEquals("11", resultJsonNode.get("ec").asText());
        assertEquals(1, resultJsonNode.get("op").size());
    }

    void executeAssertionsForNewDlrRequest(DlrRequest newDlrRequest) {
        assertEquals("A1C2D3FG", newDlrRequest.messageId());
        assertEquals(3, newDlrRequest.sourceAddrTon());
        assertEquals(2, newDlrRequest.sourceAddrNpi());
        assertEquals("1111", newDlrRequest.sourceAddr());
        assertEquals(2, newDlrRequest.destAddrTon());
        assertEquals(3, newDlrRequest.destAddrNpi());
        assertEquals("2222", newDlrRequest.destinationAddr());
        assertEquals(64, newDlrRequest.dataCoding());
        assertEquals("error", newDlrRequest.status());
        assertEquals("11", newDlrRequest.errorCode());
        assertEquals(1, newDlrRequest.optionalParameters().size());
    }

    record DlrRequest(
            @Nonnull @JsonProperty("message_id") String messageId,
            @JsonProperty("source_addr_ton") int sourceAddrTon,
            @JsonProperty("source_addr_npi") int sourceAddrNpi,
            @JsonProperty("source_addr") String sourceAddr,
            @JsonProperty("dest_addr_ton") int destAddrTon,
            @JsonProperty("dest_addr_npi") int destAddrNpi,
            @JsonProperty("destination_addr") String destinationAddr,
            @JsonProperty("data_coding") Integer dataCoding,
            @JsonProperty("status") String status,
            @JsonProperty("error_code") String errorCode,
            @JsonProperty("optional_parameters") List<UtilsRecords.OptionalParameter> optionalParameters
    ) {
        @Override
        public String toString() {
            return Converter.valueAsString(this);
        }
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    static class TestClazz {
        private double value;
        private String name;
        private int age;
        private long id;
        private boolean bValue;
    }
}