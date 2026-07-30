package com.paicbd.smsc.interpreter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paicbd.smsc.dto.Gateway;
import com.paicbd.smsc.dto.MessageEvent;
import com.paicbd.smsc.dto.UtilsRecords;
import com.paicbd.smsc.utils.EncodingUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PayloadInterpreterTest {

    @ParameterizedTest
    @ValueSource(strings = {"STRING", "HEX"})
    @DisplayName("interpretPayloadForSend with xml payload and shortMessage property String and Hexadecimal")
    void interpretPayloadForSendWithXmlPayloadThenDoSuccessfully(String dataType) throws JsonProcessingException {
        List<UtilsRecords.OptionalParameter> optionalParameters = List.of(
                new UtilsRecords.OptionalParameter((short) 1, "value1"),
                new UtilsRecords.OptionalParameter((short) 2, "value2")
        );

        MessageEvent event = MessageEvent.builder()
                .sourceAddr("123456789")
                .sourceAddrTon(2)
                .sourceAddrNpi(2)
                .destinationAddr("987654321")
                .destAddrTon(1)
                .destAddrNpi(1)
                .esmClass(64)
                .systemId("test")
                .shortMessage("Hello")
                .messageBytes(new byte[]{72, 101, 108, 108, 111})
                .registeredDelivery(1)
                .optionalParameters(optionalParameters)
                .isDlr(true)
                .checkSubmitSmResponse(true)
                .customParams(Map.of("opp", Map.of("key", "value"), "customClientId", "INNO"))
                .build();

        String xml = String.format("""
            <smpp>
            	<commandId>4</commandId>
            	<commandLength>47</commandLength>
            	<sequenceNumber>6</sequenceNumber>
            	<serviceType></serviceType>
            	<sourceAddress>
            		<address>{{sourceAddr:STRING}}</address>
            		<ton>{{sourceAddrTon:INT}}</ton>
            		<npi>{{sourceAddrNpi:INT}}</npi>
            	</sourceAddress>
            	<destAddress>
            		<address>{{destinationAddr:STRING}}</address>
            		<ton>{{destAddrTon:HEX_STRING}}</ton>
            		<npi>{{destAddrNpi:HEX_STRING}}</npi>
            	</destAddress>
            	<scheduleDeliveryTime></scheduleDeliveryTime>
            	<validityPeriod></validityPeriod>
            	<dataCoding>{{esmClass:HEX_STRING}}</dataCoding>
            	<protocolId>{{esmClass:HEX_STRING}}</protocolId>
            	<priority>{{esmClass:HEX_STRING}}</priority>
            	<registerDelivery>{{registeredDelivery:BOOLEAN}}</registerDelivery>
            	<replaceIfPresent>0x00</replaceIfPresent>
            	<messageLength>6</messageLength>
            	<message>{{shortMessage:%s}}</message>
            	<clientId>{{systemId:STRING}}</clientId>
            	<host>127.0.0.1</host>
            	<esmClass>{{esmClass:HEX_STRING}}</esmClass>
            	<optParams>{{optionalParameters:LIST:optionalParam|tag=tag1,value=value1}}</optParams>
            	<deliverSm>{{isDlr:BOOLEAN}}</deliverSm>
            	<unknownProperty>{{unknown:STRING}}</unknownProperty>
            	<inconvertibleBoolean>{{systemId:BOOLEAN}}</inconvertibleBoolean>
            	<check>{{checkSubmitSmResponse:BOOLEAN}}</check>
            	<cp>{{customParams.opp.key:MAP}}</cp>
            	<customClientId>{{customParams.customClientId:STRING}}</customClientId>
            </smpp>
            """, dataType);

        PayloadFormat pf = PayloadFormat.XML;
        String formatedXml = PayloadInterpreter.interpretPayloadForSend(xml, event, pf);
        System.out.println(formatedXml);
        assertNotNull(formatedXml);

        JsonNode node = ObjectsInterpreter.xmlStringToJsonNode(formatedXml);
        assertNotNull(node);

        assertEquals("123456789", node.get("sourceAddress").get("address").asText());
        assertEquals(2, node.get("sourceAddress").get("ton").asInt());
        assertEquals(2, node.get("sourceAddress").get("npi").asInt());
        assertEquals("987654321", node.get("destAddress").get("address").asText());
        assertEquals("0x01", node.get("destAddress").get("ton").asText());
        assertEquals("0x01", node.get("destAddress").get("npi").asText());
        assertEquals("0x40", node.get("dataCoding").asText());
        assertEquals("0x40", node.get("protocolId").asText());
        assertEquals("0x40", node.get("priority").asText());
        assertTrue(node.get("registerDelivery").asBoolean());
        assertEquals("0x00", node.get("replaceIfPresent").asText());
        assertEquals(6, node.get("messageLength").asInt());
        assertEquals("test", node.get("clientId").asText());
        assertEquals("INNO", node.get("customClientId").asText());

        JsonNode optParamsNode = node.get("optParams").get("optionalParam");
        if (optParamsNode == null) {
            optParamsNode = node.get("optParams").get("ls");
        }
        assertNotNull(optParamsNode, "The optional parameter node must not be null");
        assertEquals("value1", optParamsNode.get(0).get("value1").asText());
        assertEquals(1, optParamsNode.get(0).get("tag1").asInt());

        if ("STRING".equals(dataType)) {
            assertEquals("Hello", node.get("message").asText());
        } else {
            assertEquals("48656C6C6F", node.get("message").asText());
        }
    }

    @ParameterizedTest(name = "interpretPayloadForSend(JSON): LIST {0}")
    @MethodSource("optionalParamsCases")
    @DisplayName("interpretPayloadForSend(JSON) with LIST: no rename and with rename")
    void interpretJsonPayloadListCases(String placeholder,
                                       String keyTag,
                                       String keyValue) throws JsonProcessingException {
        List<UtilsRecords.OptionalParameter> optionalParameters = List.of(
                new UtilsRecords.OptionalParameter((short) 1, "value1"),
                new UtilsRecords.OptionalParameter((short) 2, "value2")
        );

        MessageEvent event = MessageEvent.builder()
                .sourceAddr("123456789")
                .sourceAddrTon(2)
                .sourceAddrNpi(2)
                .destinationAddr("987654321")
                .destAddrTon(1)
                .destAddrNpi(1)
                .esmClass(64)
                .checkSubmitSmResponse(false)
                .systemId("test")
                .shortMessage("Hello")
                .registeredDelivery(1)
                .optionalParameters(optionalParameters)
                .customParams(Map.of("customClientId", "INNO"))
                .build();

        String json = """
        {
          "sourceAddress": {
            "address": "{{sourceAddr:STRING}}",
            "ton": "{{sourceAddrTon:HEX_STRING}}",
            "npi": "{{sourceAddrNpi:HEX_STRING}}"
          },
          "destAddress": {
            "address": "{{destinationAddr:STRING}}",
            "ton": "{{destAddrTon:HEX_STRING}}",
            "npi": "{{destAddrNpi:HEX_STRING}}"
          },
          "dataCoding": "{{esmClass:HEX_STRING}}",
          "optionalParamsOut": "%s",
          "customClientId": "{{customParams.customClientId:STRING}}"
        }
        """.formatted(placeholder);

        String formatted = PayloadInterpreter.interpretPayloadForSend(json, event, PayloadFormat.JSON);
        JsonNode node = new ObjectMapper().readTree(formatted);

        assertNotNull(node);
        assertEquals("123456789", node.get("sourceAddress").get("address").asText());
        assertEquals("0x02", node.get("sourceAddress").get("ton").asText());
        assertEquals("987654321", node.get("destAddress").get("address").asText());
        assertEquals("0x40", node.get("dataCoding").asText());
        assertEquals("INNO", node.get("customClientId").asText());

        JsonNode list = node.get("optionalParamsOut");
        assertNotNull(list);
        assertTrue(list.isArray());
        assertEquals(2, list.size());

        assertEquals(1, list.get(0).get(keyTag).asInt());
        assertEquals("value1", list.get(0).get(keyValue).asText());
        assertEquals(2, list.get(1).get(keyTag).asInt());
        assertEquals("value2", list.get(1).get(keyValue).asText());
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> optionalParamsCases() {
        return Stream.of(
                // Case 1: without rename -> original keys (tag, value)
                org.junit.jupiter.params.provider.Arguments.of(
                        "{{optionalParameters:LIST}}",
                        "tag",
                        "value"
                ),
                // Case 2: with rename -> renamed keys (id, paramValue)
                org.junit.jupiter.params.provider.Arguments.of(
                        "{{optionalParameters:LIST tag=id,value=paramValue}}",
                        "id",
                        "paramValue"
                )
        );
    }

    @Test
    void toXml() throws JsonProcessingException {
        String xml = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <dialog mapMessagesSize="1" userObject="{{systemId:STRING}}">
                  <unstructuredSSRequest_Request dataCodingScheme="{{dataCoding:HEX_STRING}}" string="{{shortMessage:STRING}}">
                    <msisdn nai="international_number" npi="ISDN" number="{{destinationAddr:STRING}}"/>
                  </unstructuredSSRequest_Request>
                </dialog>
                """;

        MessageEvent event = MessageEvent.builder()
                .systemId("test")
                .dataCoding(64)
                .shortMessage("Hello")
                .destinationAddr("987654321")
                .build();

        String formatedXml = PayloadInterpreter.interpretPayloadForSend(xml, event, PayloadFormat.XML);
        System.out.println(formatedXml);
        assertNotNull(formatedXml);

        JsonNode node = ObjectsInterpreter.xmlStringToJsonNode(formatedXml);
        assertNotNull(node);

        assertEquals("test", node.get("userObject").asText());
        assertEquals("0x40", node.get("unstructuredSSRequest_Request").get("dataCodingScheme").asText());
        assertEquals("Hello", node.get("unstructuredSSRequest_Request").get("string").asText());
        assertEquals("987654321", node.get("unstructuredSSRequest_Request").get("msisdn").get("number").asText());
    }

    @Test
    void complexXmlStringInMessageEvent() {
        String mapper = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <dialog mapMessagesSize="1" userObject="{{systemId:STRING}}">
                  <unstructuredSSRequest_Request dataCodingScheme="{{dataCoding:HEX_STRING}}" string="{{shortMessage:STRING}}">
                    <msisdn nai="international_number" npi="ISDN" number="{{destinationAddr:STRING}}"/>
                    <ussdString>{{sourceAddr:STRING}}</ussdString>
                  </unstructuredSSRequest_Request>
                </dialog>
                """;

        String request = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <dialog mapMessagesSize="1" userObject="test">
                  <unstructuredSSRequest_Request dataCodingScheme="0x01" string="Unlimited Active Package, Balance Details">
                    <msisdn nai="international_number" npi="ISDN" number="525532368999"/>
                    <ussdString>1</ussdString>
                  </unstructuredSSRequest_Request>
                </dialog>
                """;

        MessageEvent resultedMessageEvent = new MessageEvent();
        assertDoesNotThrow(() -> PayloadInterpreter.interpreterPayloadForReceive(request, mapper, resultedMessageEvent, PayloadFormat.XML));
        System.out.println(resultedMessageEvent);

        assertEquals("test", resultedMessageEvent.getSystemId());
        assertEquals(1, resultedMessageEvent.getDataCoding());
        assertEquals("Unlimited Active Package, Balance Details", resultedMessageEvent.getShortMessage());
        assertEquals("525532368999", resultedMessageEvent.getDestinationAddr());
        assertEquals("1", resultedMessageEvent.getSourceAddr());
    }

    @Test
    void complexJsonValuesInMessageEvent() {
        String mapper = """
                {
                  "dialog": {
                    "mapMessagesSize": 1,
                    "userObject": "{{systemId:STRING}}",
                    "unstructuredSSRequest_Request": {
                      "dataCodingScheme": "{{dataCoding:HEX_STRING}}",
                      "string": "{{shortMessage:STRING}}",
                      "msisdn": {
                        "nai": "international_number",
                        "npi": "ISDN",
                        "number": "{{destinationAddr:STRING}}"
                      },
                      "ussdString": "{{sourceAddr:STRING}}"
                    }
                  }
                }
                """;

        String request = """
                {
                  "dialog": {
                    "mapMessagesSize": 1,
                    "userObject": "test",
                    "unstructuredSSRequest_Request": {
                      "dataCodingScheme": "0x01",
                      "string": "Unlimited Active Package, Balance Details",
                      "msisdn": {
                        "nai": "international_number",
                        "npi": "ISDN",
                        "number": "525532368999"
                      },
                      "ussdString": "1"
                    }
                  }
                }
                """;

        MessageEvent resultedMessageEvent = new MessageEvent();
        assertDoesNotThrow(() -> PayloadInterpreter.interpreterPayloadForReceive(request, mapper, resultedMessageEvent, PayloadFormat.JSON));
        System.out.println(resultedMessageEvent);

        assertEquals("test", resultedMessageEvent.getSystemId());
        assertEquals(1, resultedMessageEvent.getDataCoding());
        assertEquals("Unlimited Active Package, Balance Details", resultedMessageEvent.getShortMessage());
        assertEquals("525532368999", resultedMessageEvent.getDestinationAddr());
        assertEquals("1", resultedMessageEvent.getSourceAddr());
    }

    @Test
    void xml019ToMessageEvent() {
        String xml = """
                <smpp>
                	<commandId>4</commandId>
                	<commandLength>47</commandLength>
                	<sequenceNumber>6</sequenceNumber>
                	<serviceType></serviceType>
                	<sourceAddress>
                		<address>6666</address>
                		<ton>0x01</ton>
                		<npi>0x01</npi>
                	</sourceAddress>
                	<destAddress>
                		<address>5555,333,111,222,123,322,122,331,876,666,544,345,775,126</address>
                		<ton>0x01</ton>
                		<npi>0x01</npi>
                	</destAddress>
                	<scheduleDeliveryTime></scheduleDeliveryTime>
                	<validityPeriod></validityPeriod>
                	<dataCoding>0x00</dataCoding>
                	<protocolId>0x00</protocolId>
                	<priority>0x00</priority>
                	<registerDelivery>0x01</registerDelivery>
                	<replaceIfPresent>0x00</replaceIfPresent>
                	<messageLength>6</messageLength>
                	<message>48656C6C6F21</message>
                	<clientId>server1-test1</clientId>
                	<host>127.0.0.1</host>
                	<esmClass>0x03</esmClass>
                	<optParams>
                		<pop>
                			<tag>0x01</tag>
                			<lenght>0x02</lenght>
                			<value>0x76</value>
                		</pop>
                		<pop>
                			<tag>0x02</tag>
                			<lenght>0x02</lenght>
                			<value>0x65</value>
                		</pop>
                	</optParams>
                	<customClientId>INNO</customClientId>
                </smpp>
                """;

        String xmlMapper = """
                <smpp>
                	<commandId>4</commandId>
                	<commandLength>47</commandLength>
                	<sequenceNumber>6</sequenceNumber>
                	<serviceType></serviceType>
                	<sourceAddress>
                		<address>{{sourceAddr:STRING}}</address>
                		<ton>{{sourceAddrTon:HEX_STRING}}</ton>
                		<npi>{{sourceAddrNpi:HEX_STRING}}</npi>
                	</sourceAddress>
                	<destAddress>
                		<address>{{destinationAddr:LIST}}</address>
                		<ton>{{destAddrTon:HEX_STRING}}</ton>
                		<npi>{{destAddrNpi:HEX_STRING}}</npi>
                	</destAddress>
                	<scheduleDeliveryTime></scheduleDeliveryTime>
                	<validityPeriod></validityPeriod>
                	<dataCoding>{{dataCoding:HEX_STRING}}</dataCoding>
                	<protocolId>0x00</protocolId>
                	<priority>0x00</priority>
                	<registerDelivery>{{registeredDelivery:HEX_STRING}}</registerDelivery>
                	<replaceIfPresent>0x00</replaceIfPresent>
                	<messageLength>6</messageLength>
                	<message>{{shortMessage:STRING}}</message>
                	<clientId>{{systemId:STRING}}</clientId>
                	<host>127.0.0.1</host>
                	<esmClass>{{esmClass:HEX_STRING}}</esmClass>
                	<optParams>{{optionalParameters:LIST:pop}}</optParams>
                	<customClientId>{{customParams.customClientId:STRING}}</customClientId>
                </smpp>
                """;

        MessageEvent resultedMessageEvent = new MessageEvent();
        resultedMessageEvent.setMessageId("019");
        assertDoesNotThrow(() -> PayloadInterpreter.interpreterPayloadForReceive(xml, xmlMapper, resultedMessageEvent, PayloadFormat.XML));

        System.out.println(resultedMessageEvent);

        assertEquals("6666", resultedMessageEvent.getSourceAddr());
        assertEquals(1, resultedMessageEvent.getSourceAddrTon());
        assertEquals(1, resultedMessageEvent.getSourceAddrNpi());
        assertEquals("[5555, 333, 111, 222, 123, 322, 122, 331, 876, 666, 544, 345, 775, 126]", resultedMessageEvent.getDestinationAddr());
        assertEquals(1, resultedMessageEvent.getDestAddrTon());
        assertEquals(1, resultedMessageEvent.getDestAddrNpi());
        assertEquals(0, resultedMessageEvent.getDataCoding());
        assertEquals(0, resultedMessageEvent.getProtocolId());
        assertEquals(0, resultedMessageEvent.getPriorityFlag());
        assertEquals(1, resultedMessageEvent.getRegisteredDelivery());
        assertEquals(0, resultedMessageEvent.getReplaceIfPresent());
        assertEquals("48656C6C6F21", resultedMessageEvent.getShortMessage());
        assertEquals("server1-test1", resultedMessageEvent.getSystemId());
        assertEquals(3, resultedMessageEvent.getEsmClass());
        assertEquals(2, resultedMessageEvent.getOptionalParameters().size());
        assertEquals("INNO", resultedMessageEvent.getCustomParams().get("customClientId"));

    }

    @Test
    void json019ToMessageEvent() {
        String json = """
                {
                  "commandId": 4,
                  "commandLength": 47,
                  "sequenceNumber": 6,
                  "serviceType": "",
                  "sourceAddress": {
                    "address": "6666",
                    "ton": "0x01",
                    "npi": "0x01"
                  },
                  "destAddress": {
                    "address": "5555,333,111,222,123,322,122,331,876,666,544,345,775,126",
                    "ton": "1",
                    "npi": "0x01"
                  },
                  "scheduleDeliveryTime": "",
                  "validityPeriod": "",
                  "dataCoding": "0x00",
                  "protocolId": "1",
                  "priority": "0x00",
                  "registerDelivery": true,
                  "replaceIfPresent": "0x00",
                  "messageLength": 6,
                  "message": "48656C6C6F21",
                  "clientId": "server1-test1",
                  "host": "127.0.0.1",
                  "esmClass": "0x03",
                  "optParams": [
                    {
                    "tag": "0x01",
                    "value": "0x76"
                    },
                    {
                    "tag": "0x02",
                    "value": "0x65"
                    }
                  ],
                  "isDlr": "0x01",
                  "rdId": 1,
                  "unknown": null,
                  "validityPeriod": 1200,
                  "check": true,
                  "allParams": {
                    "cp1": {
                      "key1": "value1"
                    },
                    "cp2": {
                      "key2": "value2"
                    }
                  },
                  "customClientId": "INNO"
                }""";

        String jsonMapper = """
                {
                  "commandId": "{{commandId:INT}}",
                  "commandLength": 47,
                  "sequenceNumber": 6,
                  "serviceType": "",
                  "sourceAddress": {
                    "address": "{{sourceAddr:STRING}}",
                    "ton": "{{sourceAddrTon:HEX_STRING}}",
                    "npi": "{{sourceAddrNpi:HEX_STRING}}"
                  },
                  "destAddress": {
                    "address": "{{destinationAddr:LIST}}",
                    "ton": "{{destAddrTon:INT}}",
                    "npi": "{{destAddrNpi:HEX_STRING}}"
                  },
                  "scheduleDeliveryTime": "",
                  "validityPeriod": "",
                  "dataCoding": "{{dataCoding:HEX_STRING}}",
                  "protocolId": "{{protocolId:BYTE}}",
                  "priority": "0x00",
                  "registerDelivery": "{{registeredDelivery:BOOLEAN}}",
                  "replaceIfPresent": "0x00",
                  "messageLength": 6,
                  "message": "{{shortMessage:STRING}}",
                  "clientId": "{{systemId:STRING}}",
                  "host": "127.0.0.1",
                  "esmClass": "{{esmClass:HEX_STRING}}",
                  "optParams": "{{optionalParameters:LIST}}",
                  "isDlr": "{{isDlr:HEX_STRING}}",
                  "rdId": "{{remoteDialogId:LONG}}",
                  "unknown": "{{imsi:STRING}}",
                  "validityPeriod": "{{validityPeriod:LONG}}",
                  "check": "{{checkSubmitSmResponse:BOOLEAN}}",
                  "allParams": "{{customParams:MAP}}",
                  "customClientId": "{{customParams.customClientId:STRING}}"
                }
                """;

        MessageEvent resultedMessageEvent = new MessageEvent();
        resultedMessageEvent.setMessageId("019");
        assertDoesNotThrow(() -> PayloadInterpreter.interpreterPayloadForReceive(json, jsonMapper, resultedMessageEvent, PayloadFormat.JSON));

        System.out.println(resultedMessageEvent);

        assertEquals("6666", resultedMessageEvent.getSourceAddr());
        assertEquals(1, resultedMessageEvent.getSourceAddrTon());
        assertEquals(1, resultedMessageEvent.getSourceAddrNpi());
        assertEquals("[5555, 333, 111, 222, 123, 322, 122, 331, 876, 666, 544, 345, 775, 126]", resultedMessageEvent.getDestinationAddr());
        assertEquals(1, resultedMessageEvent.getDestAddrTon());
        assertEquals(1, resultedMessageEvent.getDestAddrNpi());
        assertEquals(0, resultedMessageEvent.getDataCoding());
        assertEquals(1, resultedMessageEvent.getProtocolId());
        assertEquals(0, resultedMessageEvent.getPriorityFlag());
        assertEquals(1, resultedMessageEvent.getRegisteredDelivery());
        assertEquals(0, resultedMessageEvent.getReplaceIfPresent());
        assertEquals("48656C6C6F21", resultedMessageEvent.getShortMessage());
        assertEquals("server1-test1", resultedMessageEvent.getSystemId());
        assertEquals(3, resultedMessageEvent.getEsmClass());
        assertEquals(2, resultedMessageEvent.getOptionalParameters().size());
        assertEquals(1, resultedMessageEvent.getRemoteDialogId());
        assertEquals(1200, resultedMessageEvent.getValidityPeriod());
        assertEquals("INNO", resultedMessageEvent.getCustomParams().get("customClientId"));
    }

    @Test
    void invalidDataTypeProduceIllegalArgumentException() {
        String xml = """
                <smpp>
                	<commandId>4</commandId>
                	<commandLength>47</commandLength>
                	<sequenceNumber>6</sequenceNumber>
                	<serviceType></serviceType>
                	<sourceAddress>
                		<address>{{sourceAddr:STRING}}</address>
                		<ton>{{sourceAddrTon:INT}}</ton>
                		<npi>{{sourceAddrNpi:UNKNOWN}}</npi>
                	</sourceAddress>
                </smpp>
                """;

        MessageEvent event = MessageEvent.builder()
                .sourceAddr("123456789")
                .sourceAddrTon(2)
                .sourceAddrNpi(2)
                .build();

        assertThrows(IllegalArgumentException.class, () -> PayloadInterpreter.interpretPayloadForSend(xml, event, PayloadFormat.XML));
        assertThrows(IllegalArgumentException.class, () -> PayloadInterpreter.interpreterPayloadForReceive(xml, xml, event, PayloadFormat.XML));
    }

    @Test
    void customParamsOriginTests() {
        String jsonWithCustomParams = """
                {
                    "messageId": "019",
                    "text": "Hello",
                    "params" : {
                        "p1": {
                            "key": "value"
                        },
                        "isDlr": true,
                        "rdId": 1,
                        "unknown": null,
                        "customClientId": "INNO"
                    }
                }
                """;

        String jsonMapper = """
                {
                    "messageId": "{{messageId:STRING}}",
                    "text": "{{shortMessage:STRING}}",
                    "params": "{{customParams:MAP}}",
                    "customClientId": "INNO"
                }
                """;

        MessageEvent resultedMessageEvent = new MessageEvent();
        assertDoesNotThrow(() -> PayloadInterpreter.interpreterPayloadForReceive(jsonWithCustomParams, jsonMapper, resultedMessageEvent, PayloadFormat.JSON));

        System.out.println(resultedMessageEvent);

        assertEquals("019", resultedMessageEvent.getMessageId());
        assertEquals("Hello", resultedMessageEvent.getShortMessage());
        assertNotNull(resultedMessageEvent.getCustomParams());
        assertEquals("INNO", resultedMessageEvent.getCustomParams().get("customClientId"));
    }

    @Test
    void customParamsDestinationTests() throws JsonProcessingException {
        MessageEvent event = MessageEvent.builder()
                .messageId("019")
                .shortMessage("Hello")
                .customParams(Map.of("p1", Map.of("key", "value"),  "customClientId", "INNO"))
                .isDlr(true)
                .build();

        String jsonMapper = """
                {
                    "messageId": "{{messageId:STRING}}",
                    "text": "{{shortMessage:STRING}}",
                    "params": "{{customParams:MAP}}"
                }
                """;

        String formatedJson = PayloadInterpreter.interpretPayloadForSend(jsonMapper, event, PayloadFormat.JSON);
        System.out.println(formatedJson);

        JsonNode node = new ObjectMapper().readTree(formatedJson);
        assertNotNull(node);
    }

    @ParameterizedTest
    @CsvSource({
            "0, null, '0x05', '525532368999', 'STRING'",
            "0, 'Hello world!', '0x00', '525532368999', 'STRING'",
            "0, '00480065006C006C006F00200077006F0072006C00640021', '0x08', '003500320035003500330032003300360038003900390039', 'HEX'",
            "0, '48656C6C6F20776F726C6421', '0x03', '353235353332333638393939', 'HEX'",
            "64, '07460000030102014d65737361676520706172742031', '0x05', '353235353332333638393939', 'HEX'",
            "0, '', '0x05', '525532368999', 'STRING'"
    })
    @DisplayName("Test to get short message from hex string with valid data coding and invalid data coding")
    void testForConvertShortMessageFromHexStringWithValidDataCodingThenShortMessageOk(int esmeClass, String messageHex, String dataCodingHex, String destinationAddressStr, String dataType) {
        String mapper = String.format("""
                {
                  "dialog": {
                    "mapMessagesSize": 1,
                    "esmeClass": "{{esmClass:INT}}",
                    "userObject": "{{systemId:STRING}}",
                    "unstructuredSSRequest_Request": {
                      "dataCodingScheme": "{{dataCoding:HEX_STRING}}",
                      "string": "{{shortMessage:%s}}",
                      "msisdn": {
                        "nai": "international_number",
                        "npi": "ISDN",
                        "number": "{{destinationAddr:%s}}"
                      },
                      "ussdString": "{{sourceAddr:STRING}}"
                    }
                  }
                }
                """, dataType, dataType);

        String request = String.format("""
                {
                  "dialog": {
                    "mapMessagesSize": 1,
                    "esmeClass": %d,
                    "userObject": "test",
                    "unstructuredSSRequest_Request": {
                      "dataCodingScheme": "%s",
                      "string": "%s",
                      "msisdn": {
                        "nai": "international_number",
                        "npi": "ISDN",
                        "number": "%s"
                      },
                      "ussdString": "1"
                    }
                  }
                }
                """, esmeClass, dataCodingHex, messageHex, destinationAddressStr);

        MessageEvent resultedMessageEvent = new MessageEvent();
        assertDoesNotThrow(() -> PayloadInterpreter.interpreterPayloadForReceive(request, mapper, resultedMessageEvent, PayloadFormat.JSON));
        System.out.println(resultedMessageEvent);

        assertEquals("test", resultedMessageEvent.getSystemId());

        if ("0x00".equalsIgnoreCase(dataCodingHex)) {
            assertEquals(0, resultedMessageEvent.getDataCoding());
        } else if ("0x03".equalsIgnoreCase(dataCodingHex)) {
            assertEquals(3, resultedMessageEvent.getDataCoding());
        } else if ("0x08".equalsIgnoreCase(dataCodingHex)) {
            assertEquals(8, resultedMessageEvent.getDataCoding());
        }
        else if ("0x05".equalsIgnoreCase(dataCodingHex)) {
            assertEquals(5, resultedMessageEvent.getDataCoding());
        }

        assertEquals(destinationAddressStr, resultedMessageEvent.getDestinationAddr());
        assertEquals("1", resultedMessageEvent.getSourceAddr());

        PayloadInterpreter.convertShortMessageByEncodingType(resultedMessageEvent, getGateway());
        if (!"0x05".equalsIgnoreCase(dataCodingHex)) {
            assertEquals("Hello world!", resultedMessageEvent.getShortMessage());
        } else if (Objects.nonNull(resultedMessageEvent.getShortMessage())) {
            //assertEquals(messageHex, resultedMessageEvent.getShortMessage())
            if (!messageHex.isBlank()) {
                assertNotNull(resultedMessageEvent.getUdhRaw());
                assertTrue(resultedMessageEvent.getUdhLength() > 0);
            }
        }

        if (Objects.isNull(resultedMessageEvent.getShortMessage()) || resultedMessageEvent.getShortMessage().isBlank()) {
            assertNull(resultedMessageEvent.getMessageBytes());
        } else {
            assertNotNull(resultedMessageEvent.getMessageBytes());
        }
    }

    @Test
    @DisplayName("convertToTargetType throws exception when conversion fails")
    void convertToTargetTypeShouldThrowExceptionWhenConversionFails() {
        String invalidInteger = "abc";

        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                PayloadInterpreter.convertToTargetType(invalidInteger, Integer.class)
        );

        assertTrue(exception.getMessage().contains("Failed to convert value"));
    }

    @Test
    @DisplayName("interpretPayloadForSend includes UDH when udhLength is greater than zero")
    void interpretPayloadForSendShouldIncludeUdhWhenUdhLengthGreaterThanZero() throws JsonProcessingException {
        byte[] udhBytes = new byte[]{0x05, 0x00, 0x03, 0x0A, 0x01};
        byte[] messageBytes = "Hello".getBytes(StandardCharsets.UTF_8);

        MessageEvent event = MessageEvent.builder()
                .shortMessage("Hello")
                .udhBytes(udhBytes)
                .udhLength(udhBytes.length)
                .messageBytes(messageBytes)
                .build();

        String jsonMapper = """
            {
              "message": "{{shortMessage:HEX}}"
            }
            """;

        String result = PayloadInterpreter.interpretPayloadForSend(jsonMapper, event, PayloadFormat.JSON);
        System.out.println(result);

        String expectedUdhHex = EncodingUtils.bytesToHex(udhBytes);
        String expectedMsgHex = EncodingUtils.bytesToHex(messageBytes);

        assertTrue(result.contains(expectedUdhHex + expectedMsgHex));
    }


    private Gateway getGateway() {
        return Gateway.builder()
                .encodingGsm7(EncodingUtils.GSM7)
                .encodingUcs2(EncodingUtils.UCS2)
                .encodingIso88591(EncodingUtils.ISO88591)
                .build();
    }
}