package com.paicbd.smsc.cdr;

import com.paicbd.smsc.dto.MessageEvent;
import com.paicbd.smsc.dto.UtilsRecords;
import com.paicbd.smsc.kafka.KafkaTopicsConstants;
import com.paicbd.smsc.utils.Converter;
import com.paicbd.smsc.utils.ErrorCodes;
import com.paicbd.smsc.utils.UtilsEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class CdrProcessorTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;
    private CdrProcessor cdrProcessor;

    @BeforeEach
    void setup() {
        cdrProcessor = new CdrProcessor();
    }

    @Test
    void testPublishCdrSuccessMessageShouldSendCdrOnly() {
        MessageEvent event = baseMessageEvent();
        UtilsRecords.Cdr cdr = event.toCdr(
                UtilsEnum.Module.SMPP_CLIENT,
                UtilsEnum.MessageType.MESSAGE,
                UtilsEnum.CdrStatus.SUCCESS,
                "",
                null
        );

        cdrProcessor.publishCdr(event, UtilsEnum.Module.SMPP_CLIENT, UtilsEnum.MessageType.MESSAGE, UtilsEnum.CdrStatus.SUCCESS, kafkaTemplate);

        ArgumentCaptor<String> captorCdrStr = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq(KafkaTopicsConstants.CDR_TOPIC), captorCdrStr.capture());
        UtilsRecords.Cdr cdrSaved = Converter.stringToObject(captorCdrStr.getValue(), UtilsRecords.Cdr.class);
        assertNotNull(cdrSaved);
        assertEquals(cdr.messageId(), cdrSaved.messageId());
        assertEquals(cdr.messageType(), cdrSaved.messageType());
        assertEquals(cdr.status(), cdrSaved.status());
        verifyNoMoreInteractions(kafkaTemplate);
    }


    @Test
    @DisplayName("toCdr should create Cdr with correct comment when messageType is DELIVER and status is not null")
    void toCdrShouldCreateCdrWithCorrectCommentWhenMessageTypeIsDeliverAndStatusIsNotNull() {
        MessageEvent event = baseMessageEvent();

        UtilsRecords.Cdr cdr = event.toCdr(
                UtilsEnum.Module.SMPP_CLIENT,
                UtilsEnum.MessageType.DELIVER,
                UtilsEnum.CdrStatus.SUCCESS,
                "mno-message-456",
                null
        );

        assertNotNull(cdr);
        assertEquals("mno-message-456", cdr.mnoMessageId());
        assertEquals(UtilsEnum.MessageType.DELIVER.name(), cdr.messageType());
    }


    @ParameterizedTest
    @MethodSource("cdrTestParameters")
    void testPublishCdrWithBroadcastShouldSendBothCdrAndStatistic(String priority) {
        MessageEvent event = baseMessageEvent();
        event.setBroadcastId(999);

        cdrProcessor.publishCdr(event, "", UtilsEnum.Module.SMPP_CLIENT, UtilsEnum.MessageType.MESSAGE,
                UtilsEnum.CdrStatus.SUCCESS, kafkaTemplate);

        ArgumentCaptor<String> captorEventStr = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq(KafkaTopicsConstants.BROADCAST_STATISTIC_TOPIC), anyString());
        verify(kafkaTemplate).send(eq(KafkaTopicsConstants.CDR_TOPIC), captorEventStr.capture());
        verifyNoMoreInteractions(kafkaTemplate);

        String eventStr = captorEventStr.getValue();
        UtilsRecords.Cdr cdrProcessed = Converter.stringToObject(eventStr, UtilsRecords.Cdr.class);
        assertNotNull(cdrProcessed);
        assertEquals(999L, cdrProcessed.broadcastId());
        assertEquals(event.getMessageId(), cdrProcessed.messageId());
        assertEquals(UtilsEnum.CdrStatus.SUCCESS.name(), cdrProcessed.status());
    }


    @Test
    void testPublishCdrFailedWithSubErrorShouldSendCdrWithErrorDescription() {
        MessageEvent event = baseMessageEvent();
        event.setErrorCode(6); // SS7: Absent SubscriberSM
        ErrorCodes.addSubErrorKey(event, 2); // Roaming Restriction

        cdrProcessor.publishCdr(event, UtilsEnum.Module.SS7_CLIENT, UtilsEnum.MessageType.MESSAGE, UtilsEnum.CdrStatus.FAILED, kafkaTemplate);

        ArgumentCaptor<String> cdrCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq(KafkaTopicsConstants.CDR_TOPIC), cdrCaptor.capture());

        String sentCdr = cdrCaptor.getValue();
        assertTrue(sentCdr.contains("Roaming Restriction"));
        assertTrue(sentCdr.contains("Absent SubscriberSM"));
    }

    @Test
    @DisplayName("toCdr should serialize optional parameters to JSON string when TLVs are present")
    void toCdrShouldSerializeOptionalParametersToJsonWhenTlvsArePresent() {
        MessageEvent event = baseMessageEvent();
        event.setOptionalParameters(java.util.List.of(
                new UtilsRecords.OptionalParameter((short) 29, "original-uuid-1234"),
                new UtilsRecords.OptionalParameter((short) 30, "test-value")
        ));

        UtilsRecords.Cdr cdr = event.toCdr(
                UtilsEnum.Module.SMPP_SERVER,
                UtilsEnum.MessageType.MESSAGE,
                UtilsEnum.CdrStatus.SUCCESS,
                "",
                null
        );

        assertNotNull(cdr);
        assertNotNull(cdr.optionalParameters());
        assertTrue(cdr.optionalParameters().contains("\"tag\":29"));
        assertTrue(cdr.optionalParameters().contains("\"value\":\"original-uuid-1234\""));
        assertTrue(cdr.optionalParameters().contains("\"tag\":30"));
        assertTrue(cdr.optionalParameters().contains("\"value\":\"test-value\""));
    }

    @Test
    @DisplayName("toCdr should return empty string for optional parameters when no TLVs are present")
    void toCdrShouldReturnEmptyStringForOptionalParametersWhenNoTlvsArePresent() {
        MessageEvent event = baseMessageEvent();

        UtilsRecords.Cdr cdr = event.toCdr(
                UtilsEnum.Module.SMPP_SERVER,
                UtilsEnum.MessageType.MESSAGE,
                UtilsEnum.CdrStatus.SUCCESS,
                "",
                null
        );

        assertNotNull(cdr);
        assertEquals("", cdr.optionalParameters());
    }

    private MessageEvent baseMessageEvent() {
        return MessageEvent.builder()
                .id(System.currentTimeMillis() + "-" + System.nanoTime())
                .messageId(System.currentTimeMillis() + "-" + System.nanoTime())
                .originProtocol("SMPP")
                .originNetworkId(1)
                .originNetworkType("GW")
                .destProtocol("SMPP")
                .destNetworkId(2)
                .destNetworkType("GW")
                .routingId(5)
                .shortMessage("Hello world")
                .sourceAddr("111")
                .destinationAddr("222")
                .dataCoding(0)
                .validityPeriod(60000L)
                .build();
    }

    @Test
    void testPublishCdrWithFiveParameters() {
        MessageEvent event = baseMessageEvent();

        cdrProcessor.publishCdr(event, UtilsEnum.Module.SMPP_CLIENT,
                UtilsEnum.MessageType.MESSAGE, UtilsEnum.CdrStatus.SUCCESS, kafkaTemplate);

        ArgumentCaptor<String> captorCdrStr = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq(KafkaTopicsConstants.CDR_TOPIC), captorCdrStr.capture());

        UtilsRecords.Cdr cdrSaved = Converter.stringToObject(captorCdrStr.getValue(), UtilsRecords.Cdr.class);
        assertNotNull(cdrSaved);
        assertEquals(event.getMessageId(), cdrSaved.messageId());
        assertEquals(UtilsEnum.MessageType.MESSAGE.name(), cdrSaved.messageType());
        assertEquals(UtilsEnum.CdrStatus.SUCCESS.name(), cdrSaved.status());
        verifyNoMoreInteractions(kafkaTemplate);
    }

    @Test
    void testPublishCdrWithSixParametersWithoutPriority() {
        MessageEvent event = baseMessageEvent();
        String mnoMessageId = "mno-123";

        cdrProcessor.publishCdr(event, mnoMessageId, UtilsEnum.Module.SMPP_CLIENT,
                UtilsEnum.MessageType.MESSAGE, UtilsEnum.CdrStatus.SUCCESS, kafkaTemplate);

        ArgumentCaptor<String> captorCdrStr = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq(KafkaTopicsConstants.CDR_TOPIC), captorCdrStr.capture());

        UtilsRecords.Cdr cdrSaved = Converter.stringToObject(captorCdrStr.getValue(), UtilsRecords.Cdr.class);
        assertNotNull(cdrSaved);
        assertEquals(event.getMessageId(), cdrSaved.messageId());
        assertEquals(mnoMessageId, cdrSaved.mnoMessageId());
        assertEquals(UtilsEnum.MessageType.MESSAGE.name(), cdrSaved.messageType());
        assertEquals(UtilsEnum.CdrStatus.SUCCESS.name(), cdrSaved.status());
        verifyNoMoreInteractions(kafkaTemplate);
    }

    @Test
    void testPublishCdrWithBroadcastAndNonMessageTypeShouldNotSendStatistics() {
        MessageEvent event = baseMessageEvent();
        event.setBroadcastId(999);

        cdrProcessor.publishCdr(event, UtilsEnum.Module.SMPP_CLIENT, UtilsEnum.MessageType.DELIVER,
                UtilsEnum.CdrStatus.SUCCESS, kafkaTemplate);

        verify(kafkaTemplate).send(eq(KafkaTopicsConstants.CDR_TOPIC), anyString());
        verifyNoMoreInteractions(kafkaTemplate);
    }

    @Test
    void testPublishCdrWithBroadcastAndFailedStatus() {
        MessageEvent event = baseMessageEvent();
        event.setBroadcastId(999);
        event.setErrorCode(2);
        cdrProcessor.publishCdr(event, UtilsEnum.Module.SMPP_CLIENT, UtilsEnum.MessageType.MESSAGE,
                UtilsEnum.CdrStatus.FAILED, kafkaTemplate);

        ArgumentCaptor<String> captorEventStr = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq(KafkaTopicsConstants.BROADCAST_STATISTIC_TOPIC), anyString());
        verify(kafkaTemplate).send(eq(KafkaTopicsConstants.CDR_TOPIC), captorEventStr.capture());
        verifyNoMoreInteractions(kafkaTemplate);

        String eventStr = captorEventStr.getValue();
        UtilsRecords.Cdr cdrProcessed = Converter.stringToObject(eventStr, UtilsRecords.Cdr.class);
        assertNotNull(cdrProcessed);
        assertEquals(UtilsEnum.CdrStatus.FAILED.name(), cdrProcessed.status());
    }

    @Test
    @DisplayName("publishCdr with comment override should use provided comment in CDR")
    void publishCdrWithCommentOverrideShouldUseProvidedComment() {
        MessageEvent event = baseMessageEvent();
        String responsePayload = "{\"code\":\"failed\",\"description\":\"Login failed\",\"message_id\":null}";

        cdrProcessor.publishCdr(event, "", UtilsEnum.Module.HTTP_CLIENT,
                UtilsEnum.MessageType.MESSAGE, UtilsEnum.CdrStatus.SUCCESS, responsePayload, kafkaTemplate);

        ArgumentCaptor<String> captorCdrStr = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq(KafkaTopicsConstants.CDR_TOPIC), captorCdrStr.capture());

        UtilsRecords.Cdr cdrSaved = Converter.stringToObject(captorCdrStr.getValue(), UtilsRecords.Cdr.class);
        assertNotNull(cdrSaved);
        assertEquals(responsePayload, cdrSaved.comment());
        assertEquals(UtilsEnum.CdrStatus.SUCCESS.name(), cdrSaved.status());
        assertEquals(event.getMessageId(), cdrSaved.messageId());
    }

    @Test
    @DisplayName("publishCdr with null comment should use default comment logic")
    void publishCdrWithNullCommentShouldUseDefaultLogic() {
        MessageEvent event = baseMessageEvent();

        cdrProcessor.publishCdr(event, "", UtilsEnum.Module.HTTP_CLIENT,
                UtilsEnum.MessageType.MESSAGE, UtilsEnum.CdrStatus.SUCCESS, null, kafkaTemplate);

        ArgumentCaptor<String> captorCdrStr = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq(KafkaTopicsConstants.CDR_TOPIC), captorCdrStr.capture());

        UtilsRecords.Cdr cdrSaved = Converter.stringToObject(captorCdrStr.getValue(), UtilsRecords.Cdr.class);
        assertNotNull(cdrSaved);
        assertEquals("", cdrSaved.comment());
        assertEquals(UtilsEnum.CdrStatus.SUCCESS.name(), cdrSaved.status());
    }

    private static Stream<Arguments> cdrTestParameters() {
        return Stream.of(
                Arguments.of("LOW"),
                Arguments.of("MEDIUM"),
                Arguments.of("HIGH"),
                Arguments.of(null, null, null)
        );
    }
}
