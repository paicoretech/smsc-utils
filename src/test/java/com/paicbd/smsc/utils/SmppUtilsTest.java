package com.paicbd.smsc.utils;

import com.paicbd.smsc.dto.Gateway;
import com.paicbd.smsc.dto.GeneralSettings;
import com.paicbd.smsc.dto.MessageEvent;
import com.paicbd.smsc.dto.UtilsRecords;
import org.jsmpp.bean.GSMSpecificFeature;
import org.jsmpp.bean.OptionalParameter;
import org.jsmpp.bean.SubmitSm;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class SmppUtilsTest {

    @Test
    @DisplayName("determineEncodingType when using a general Settings object and a valid encoding then return the value")
    void determineEncodingTypeUsingSettingsWhenEncodingIsValidThenReturnValue() {
        GeneralSettings generalSettings = GeneralSettings.builder()
                .encodingGsm7(EncodingUtils.GSM7)
                .encodingUcs2(EncodingUtils.UCS2)
                .encodingIso88591(EncodingUtils.ISO88591)
                .build();

        assertEquals(EncodingUtils.GSM7, SmppUtils.determineEncodingType(EncodingUtils.DCS_0, generalSettings));
        assertEquals(EncodingUtils.UCS2, SmppUtils.determineEncodingType(EncodingUtils.DCS_8, generalSettings));
        assertEquals(EncodingUtils.GSM7, SmppUtils.determineEncodingType(EncodingUtils.DCS_3, generalSettings));
    }

    @Test
    @DisplayName("determineEncodingType when using a gateway object and a valid encoding then return the value")
    void determineEncodingTypeUsingGatewayWhenEncodingIsValidThenReturnValue() {
        Gateway gateway = Gateway.builder()
                .encodingGsm7(EncodingUtils.GSM7)
                .encodingUcs2(EncodingUtils.UCS2)
                .encodingIso88591(EncodingUtils.ISO88591)
                .build();
        assertEquals(EncodingUtils.GSM7, SmppUtils.determineEncodingType(EncodingUtils.DCS_0, gateway));
        assertEquals(EncodingUtils.GSM7, SmppUtils.determineEncodingType(1, gateway));
        assertEquals(EncodingUtils.GSM7, SmppUtils.determineEncodingType(2, gateway));
        assertEquals(EncodingUtils.GSM7, SmppUtils.determineEncodingType(3, gateway));
        assertEquals(EncodingUtils.ISO88591, SmppUtils.determineEncodingType(4, gateway));
        assertEquals(EncodingUtils.ISO88591, SmppUtils.determineEncodingType(5, gateway));
        assertEquals(EncodingUtils.ISO88591, SmppUtils.determineEncodingType(6, gateway));
        assertEquals(EncodingUtils.ISO88591, SmppUtils.determineEncodingType(7, gateway));
        assertEquals(EncodingUtils.UCS2, SmppUtils.determineEncodingType(EncodingUtils.DCS_8, gateway));
        assertEquals(EncodingUtils.UCS2, SmppUtils.determineEncodingType(9, gateway));
        assertEquals(EncodingUtils.UCS2, SmppUtils.determineEncodingType(10, gateway));
        assertEquals(EncodingUtils.UCS2, SmppUtils.determineEncodingType(11, gateway));
    }

    @Test
    @DisplayName("determineEncodingType when using an invalid encoding then an IllegalStateException is thrown")
    void determineEncodingTypeWhenEncodingIsNotValidThenThrowException() {
        GeneralSettings smppGeneralSettingsMock = mock(GeneralSettings.class);
        int selectedEncodingType = SmppUtils.determineEncodingType(1, smppGeneralSettingsMock);
        assertEquals(EncodingUtils.GSM7, selectedEncodingType);
    }

    @Test
    @DisplayName("getTLV when executed the return then submitSm with optional parameters")
    void getTlvWhenMessageEventHasOptionalParametersThenReturnOptionalParameters() {
        String messageId = System.currentTimeMillis() + "-" + System.nanoTime();
        OptionalParameter sarTotalSegments = new OptionalParameter.Sar_total_segments((byte) 10);
        OptionalParameter sarSegmentSeqNum = new OptionalParameter.Sar_segment_seqnum((byte) 1);
        OptionalParameter sarMsgRefNum = new OptionalParameter.Sar_msg_ref_num((short) 1);
        OptionalParameter destNetworkId = new OptionalParameter.Dest_network_id("1");
        OptionalParameter alertOnMessageDelivery = new OptionalParameter.Alert_on_message_delivery((byte) 1);
        OptionalParameter broadcastErrorStatus = new OptionalParameter.Broadcast_error_status(1);
        OptionalParameter languageIndicator = new OptionalParameter.Language_indicator((byte) 1);


        MessageEvent submitSmEvent = MessageEvent.builder()
                .messageId(messageId)
                .shortMessage("test")
                .optionalParameters(List.of(
                        new UtilsRecords.OptionalParameter((short) 9999, "10"), // Testing Wrong Tag value
                        new UtilsRecords.OptionalParameter((short) 526, "10"),
                        new UtilsRecords.OptionalParameter((short) 527, "1"),
                        new UtilsRecords.OptionalParameter((short) 524, "1"),
                        new UtilsRecords.OptionalParameter((short) 1062, "1000"), // Testing Wring Value for Tag
                        new UtilsRecords.OptionalParameter((short) 1550, "1"),
                        new UtilsRecords.OptionalParameter((short) 4876, "1"),
                        new UtilsRecords.OptionalParameter((short) 1543, "1"),
                        new UtilsRecords.OptionalParameter((short) 525, "1")
                ))
                .build();

        var optionalParametersResult = SmppUtils.getTLV(submitSmEvent);
        assertNotNull(optionalParametersResult);
        assertEquals(7, optionalParametersResult.length);
        assertTrue(
                Arrays.stream(optionalParametersResult)
                        .map(optionalParameter -> optionalParameter.tag)
                        .toList().containsAll(Arrays.asList(sarTotalSegments.tag, sarSegmentSeqNum.tag,
                                sarMsgRefNum.tag, destNetworkId.tag, alertOnMessageDelivery.tag,
                                broadcastErrorStatus.tag, languageIndicator.tag))
        );
    }

    @Test
    @DisplayName("setTLV when optional parameters are configured then return the TLV is associated to the message")
    void setTlvWhenSetOptionalParametersThenMessageEventGetTLV() {
        Map<Short, OptionalParameter> optionalParameterMap = Map.of(
                (short) 30, new OptionalParameter.Receipted_message_id("1"),
                (short) 29, new OptionalParameter.Additional_status_info_text("sent"),
                (short) 524, new OptionalParameter.Sar_msg_ref_num((short) 1),
                (short) 1550, new OptionalParameter.Dest_network_id("1"),
                (short) 4876, new OptionalParameter.Alert_on_message_delivery((byte) 1),
                (short) 1543, new OptionalParameter.Broadcast_error_status(1),
                (short) 525, new OptionalParameter.Language_indicator((byte) 1)
        );


        String messageId = System.currentTimeMillis() + "-" + System.nanoTime();

        MessageEvent submitSmEvent = MessageEvent.builder()
                .messageId(messageId)
                .shortMessage("test")
                .commandStatus(4)
                .dataCoding(0)
                .esmClass(3)
                .sourceAddrTon(1)
                .sourceAddrNpi(1)
                .destAddrNpi(1)
                .destAddrTon(1)
                .sourceAddr("11111111")
                .destinationAddr("22222222")
                .build();

        SmppUtils.setTLV(submitSmEvent, optionalParameterMap.values().toArray(new OptionalParameter[0]));
        List<UtilsRecords.OptionalParameter> optionalParamList = submitSmEvent.getOptionalParameters();
        assertEquals(optionalParameterMap.size(), optionalParamList.size());
        optionalParamList.forEach(op -> {
            OptionalParameter optionalParameter = optionalParameterMap.get(op.tag());
            assertNotNull(optionalParameter);
            assertEquals(optionalParameter.tag, op.tag());
            switch (op.tag()) {
                case 29:
                    assertEquals("sent", op.value());
                    break;

                case 30, 524, 1550, 4876, 1543, 525:
                    assertEquals("1", op.value());

                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + op.tag());
            }
        });

    }

    @Test
    @DisplayName("deserializeTLV when the tag and the value are configured then execute the deserialization")
    void deserializeTlvWhenTagAndContentProvidedThenDeserialize() {
        String messageId = System.currentTimeMillis() + "-" + System.nanoTime();

        MessageEvent submitSmEvent = new MessageEvent();
        submitSmEvent.setMessageId(messageId);
        submitSmEvent.setShortMessage("test");

        OptionalParameter destAddressSubunit = new OptionalParameter.Dest_addr_subunit((byte) 1);
        OptionalParameter destNetworkType = new OptionalParameter.Dest_network_type((byte) 1);
        OptionalParameter destBearerType = new OptionalParameter.Dest_bearer_type((byte) 1);
        OptionalParameter destTelematicsId = new OptionalParameter.Dest_telematics_id((byte) 1);
        OptionalParameter sourceAddrSubunit = new OptionalParameter.Source_addr_subunit((byte) 1);
        OptionalParameter sourceNetworkType = new OptionalParameter.Source_network_type((byte) 1);
        OptionalParameter sourceBearerType = new OptionalParameter.Source_bearer_type((byte) 1);
        OptionalParameter sourceTelematicsId = new OptionalParameter.Source_telematics_id((byte) 1);
        OptionalParameter qosTimeToLive = new OptionalParameter.Qos_time_to_live((byte) 1);
        OptionalParameter payloadType = new OptionalParameter.Payload_type((byte) 1);
        OptionalParameter msMsgWaitFacilities = new OptionalParameter.Ms_msg_wait_facilities((byte) 1);
        OptionalParameter privacyIndicator = new OptionalParameter.Privacy_indicator((byte) 1);
        OptionalParameter sourceSubaddress = new OptionalParameter.Source_subaddress(new byte[]{0, 1});
        OptionalParameter destSubaddress = new OptionalParameter.Dest_subaddress(new byte[]{0, 1});
        OptionalParameter userResponseCode1 = new OptionalParameter.User_response_code(new byte[]{0, 1});
        OptionalParameter sourcePort = new OptionalParameter.Source_port(new byte[]{0, 1});
        OptionalParameter userMessageReference = new OptionalParameter.User_message_reference(new byte[]{0, 1});
        OptionalParameter destinationPort = new OptionalParameter.Destination_port(new byte[]{0, 1});
        OptionalParameter languageIndicator = new OptionalParameter.Language_indicator(new byte[]{0, 1});
        OptionalParameter sarTotalSegments = new OptionalParameter.Sar_total_segments(new byte[]{0, 1});
        OptionalParameter userResponseCode2 = new OptionalParameter.User_response_code(new byte[]{0, 1});
        OptionalParameter sarSegmentSeqnum = new OptionalParameter.Sar_segment_seqnum(new byte[]{0, 1});
        OptionalParameter scInterfaceVersion = new OptionalParameter.Sc_interface_version(new byte[]{0, 1});
        OptionalParameter callbackNumPresInd = new OptionalParameter.Callback_num_pres_ind(new byte[]{0, 1});
        OptionalParameter callbackNumAtag = new OptionalParameter.Callback_num_atag(new byte[]{0, 1});
        OptionalParameter numberOfMessages = new OptionalParameter.Number_of_messages(new byte[]{0, 1});
        OptionalParameter callbackNum = new OptionalParameter.Callback_num(new byte[]{0, 1});
        OptionalParameter dpfResult = new OptionalParameter.Dpf_result(new byte[]{0, 1});
        OptionalParameter setDpf = new OptionalParameter.Set_dpf(new byte[]{0, 1});
        OptionalParameter msAvailabilityStatus = new OptionalParameter.Ms_availability_status(new byte[]{0, 1});
        OptionalParameter networkErrorCode = new OptionalParameter.Network_error_code(new byte[]{0, 1});
        OptionalParameter messagePayload = new OptionalParameter.Message_payload(new byte[]{0, 1});
        OptionalParameter deliveryFailureReason = new OptionalParameter.Delivery_failure_reason(new byte[]{0, 1});
        OptionalParameter moreMessagesToSend = new OptionalParameter.More_messages_to_send(new byte[]{0, 1});
        OptionalParameter messageState = new OptionalParameter.Message_state(new byte[]{0, 1});
        OptionalParameter congestionState = new OptionalParameter.Congestion_state(new byte[]{0, 1});
        OptionalParameter ussdServiceOp = new OptionalParameter.Ussd_service_op(new byte[]{0, 1});
        OptionalParameter broadcastChannelIndicator = new OptionalParameter.Broadcast_channel_indicator(new byte[]{0, 1});
        OptionalParameter broadcastContentType = new OptionalParameter.Broadcast_content_type(new byte[]{0, 1});
        OptionalParameter broadcastMessageClass = new OptionalParameter.Broadcast_message_class(new byte[]{0, 1});
        OptionalParameter broadcastRepNum = new OptionalParameter.Broadcast_rep_num(new byte[]{0, 1});
        OptionalParameter broadcastFrequencyInterval = new OptionalParameter.Broadcast_frequency_interval(new byte[]{0, 1});
        OptionalParameter broadcastAreaIdentifier = new OptionalParameter.Broadcast_area_identifier(new byte[]{0, 1});
        OptionalParameter broadcastAreaSuccess = new OptionalParameter.Broadcast_area_success(new byte[]{0, 1});
        OptionalParameter broadcastEndTime = new OptionalParameter.Broadcast_end_time(new byte[]{0, 1});
        OptionalParameter broadcastServiceGroup = new OptionalParameter.Broadcast_service_group(new byte[]{0, 1});
        OptionalParameter billingIdentification = new OptionalParameter.Billing_identification(new byte[]{0, 1});
        OptionalParameter sourceNetworkId = new OptionalParameter.Source_network_id(new byte[]{0, 1});
        OptionalParameter destNetworkId = new OptionalParameter.Dest_network_id(new byte[]{0, 1});
        OptionalParameter sourceNodeId = new OptionalParameter.Source_node_id(new byte[]{0, 1});
        OptionalParameter destNodeId = new OptionalParameter.Dest_node_id(new byte[]{0, 1});
        OptionalParameter destAddrNpResolution = new OptionalParameter.Dest_addr_np_resolution(new byte[]{0, 1});
        OptionalParameter destAddrNpInformation = new OptionalParameter.Dest_addr_np_information(new byte[]{0, 1});
        OptionalParameter destAddrNpCountry = new OptionalParameter.Dest_addr_np_country(new byte[]{0, 1});
        OptionalParameter displayTime = new OptionalParameter.Display_time(new byte[]{0, 1});
        OptionalParameter smsSignal = new OptionalParameter.Sms_signal(new byte[]{0, 1});
        OptionalParameter msValidity = new OptionalParameter.Ms_validity(new byte[]{0, 1});
        OptionalParameter itsReplyType = new OptionalParameter.Its_reply_type(new byte[]{0, 1});
        OptionalParameter itsSessionInfo = new OptionalParameter.Its_session_info(new byte[]{0, 1});
        OptionalParameter vendorSpecificSourceMscAddr = new OptionalParameter.Vendor_specific_source_msc_addr(new byte[]{0, 1});
        OptionalParameter vendorSpecificDestMscAddr = new OptionalParameter.Vendor_specific_dest_msc_addr(new byte[]{0, 1});
        OptionalParameter broadcastContentTypeInfo = new OptionalParameter.Broadcast_content_type_info(new byte[]{0, 1});

        OptionalParameter[] optionalParameters = new OptionalParameter[]{
                destAddressSubunit,
                destNetworkType,
                destBearerType,
                destTelematicsId,
                sourceAddrSubunit,
                sourceNetworkType,
                sourceBearerType,
                sourceTelematicsId,
                qosTimeToLive,
                payloadType,
                msMsgWaitFacilities,
                privacyIndicator,
                sourceSubaddress,
                destSubaddress,
                userResponseCode1,
                sourcePort,
                userMessageReference,
                destinationPort,
                languageIndicator,
                sarTotalSegments,
                userResponseCode2,
                sarSegmentSeqnum,
                scInterfaceVersion,
                callbackNumPresInd,
                callbackNumAtag,
                numberOfMessages,
                callbackNum,
                dpfResult,
                setDpf,
                msAvailabilityStatus,
                networkErrorCode,
                messagePayload,
                deliveryFailureReason,
                moreMessagesToSend,
                messageState,
                congestionState,
                ussdServiceOp,
                broadcastChannelIndicator,
                broadcastContentType,
                broadcastMessageClass,
                broadcastRepNum,
                broadcastFrequencyInterval,
                broadcastAreaIdentifier,
                broadcastAreaSuccess,
                broadcastEndTime,
                broadcastServiceGroup,
                billingIdentification,
                sourceNetworkId,
                destNetworkId,
                sourceNodeId,
                destNodeId,
                destAddrNpResolution,
                destAddrNpInformation,
                destAddrNpCountry,
                displayTime,
                smsSignal,
                msValidity,
                itsReplyType,
                itsSessionInfo,
                vendorSpecificSourceMscAddr,
                vendorSpecificDestMscAddr,
                broadcastContentTypeInfo,
        };
        SmppUtils.setTLV(submitSmEvent, optionalParameters);
        assertNotNull(SmppUtils.getTLV(submitSmEvent));

    }

    @Test
    void messagePayloadTlv() {
        String stringHex = "0605043E94000048656c6c6f2054686973206d65737361676520646f6573206e6f74207265717565737420646c7221".toUpperCase();
        byte[] stringBytes = EncodingUtils.hexToBytes(stringHex);

        SubmitSm submitSm = new SubmitSm();
        submitSm.setDataCoding((byte) 0);
        submitSm.setEsmClass(GSMSpecificFeature.UDHI.value());
        OptionalParameter[] optionalParameters =
                new OptionalParameter[]{new OptionalParameter.Message_payload(stringBytes)};
        submitSm.setOptionalParameters(optionalParameters);

        byte[] messagePayload = SmppUtils.getMessagePayloadValue(submitSm);
        assertNotNull(messagePayload);
        assertEquals(stringHex, EncodingUtils.bytesToHex(messagePayload));

        SubmitSm submitSmWithoutTlv = new SubmitSm();
        submitSmWithoutTlv.setDataCoding((byte) 0);

        byte[] messagePayloadWithoutTlv = SmppUtils.getMessagePayloadValue(submitSmWithoutTlv);
        assertNotNull(messagePayloadWithoutTlv);
        assertEquals(0, messagePayloadWithoutTlv.length);
    }
}
