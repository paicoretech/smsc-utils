package com.paicbd.smsc.utils;

import com.paicbd.smsc.dto.MessageEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.restcomm.protocols.ss7.map.api.smstpdu.AbsoluteTimeStamp;
import org.restcomm.protocols.ss7.map.api.smstpdu.DataCodingScheme;
import org.restcomm.protocols.ss7.map.api.smstpdu.SmsSubmitTpdu;
import org.restcomm.protocols.ss7.map.api.smstpdu.UserData;
import org.restcomm.protocols.ss7.map.api.smstpdu.ValidityPeriod;
import org.restcomm.protocols.ss7.map.api.smstpdu.ValidityPeriodFormat;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class SignalingFactoryTest {

    static final int ALLOWED_DELAY_FOR_MIN_OR_SEC = 10;
    private final SignalingFactory factory = new SignalingFactory();

    static Stream<Arguments> relativeValidityParams() {
        return Stream.of(
                Arguments.of(0, 0L),
                Arguments.of(1, 3600L),
                Arguments.of(5, 5L * 3600)
        );
    }

    static Stream<Arguments> absoluteTimeStampParameters() {
        return Stream.of(
                Arguments.of(TimeOfCalendar.YEAR, (GregorianCalendar.getInstance().get(Calendar.YEAR)) - 2000),
                Arguments.of(TimeOfCalendar.MONTH, (GregorianCalendar.getInstance().get(Calendar.MONTH)) + 1),
                Arguments.of(TimeOfCalendar.DAY, (GregorianCalendar.getInstance().get(Calendar.DAY_OF_MONTH))),
                Arguments.of(TimeOfCalendar.HOUR, (GregorianCalendar.getInstance().get(Calendar.HOUR))),
                Arguments.of(TimeOfCalendar.MINUTE, (GregorianCalendar.getInstance().get(Calendar.MINUTE))),
                Arguments.of(TimeOfCalendar.SECOND, (GregorianCalendar.getInstance().get(Calendar.SECOND)))
        );
    }

    @ParameterizedTest
    @MethodSource("relativeValidityParams")
    void setValidityPeriodRelative(int hours, long expectedSeconds) {
        SmsSubmitTpdu tpdu = mock(SmsSubmitTpdu.class);
        ValidityPeriod vp = mock(ValidityPeriod.class);
        when(tpdu.getValidityPeriod()).thenReturn(vp);
        when(tpdu.getValidityPeriodFormat()).thenReturn(ValidityPeriodFormat.fieldPresentRelativeFormat);
        when(vp.getRelativeFormatHours()).thenReturn(Double.valueOf(hours));

        MessageEvent msg = mock(MessageEvent.class);
        factory.setValidityPeriod(msg, tpdu);
        verify(msg).setValidityPeriod(expectedSeconds);
    }

    @Test
    void setValidityPeriodAbsolutesameTime() {
        SmsSubmitTpdu tpdu = mock(SmsSubmitTpdu.class);
        ValidityPeriod vp = mock(ValidityPeriod.class);
        AbsoluteTimeStamp ats = mock(AbsoluteTimeStamp.class);
        when(tpdu.getValidityPeriod()).thenReturn(vp);
        when(tpdu.getValidityPeriodFormat()).thenReturn(ValidityPeriodFormat.fieldPresentAbsoluteFormat);
        when(vp.getAbsoluteFormatValue()).thenReturn(ats);

        Calendar now = Calendar.getInstance();
        int localOffsetSec = -now.getTimeZone().getOffset(now.getTimeInMillis()) / 1000;
        int quarters = localOffsetSec / (15 * 60);

        when(ats.getYear()).thenReturn(now.get(Calendar.YEAR));
        when(ats.getMonth()).thenReturn(now.get(Calendar.MONTH) + 1);
        when(ats.getDay()).thenReturn(now.get(Calendar.DAY_OF_MONTH));
        when(ats.getHour()).thenReturn(now.get(Calendar.HOUR_OF_DAY));
        when(ats.getMinute()).thenReturn(now.get(Calendar.MINUTE));
        when(ats.getSecond()).thenReturn(now.get(Calendar.SECOND));
        when(ats.getTimeZone()).thenReturn(quarters);

        MessageEvent msg = mock(MessageEvent.class);
        factory.setValidityPeriod(msg, tpdu);
        verify(msg).setValidityPeriod(0L);
    }

    @Test
    void setValidityPeriodUnsupported() {
        SmsSubmitTpdu tpdu = mock(SmsSubmitTpdu.class);
        ValidityPeriod vp = mock(ValidityPeriod.class);
        when(tpdu.getValidityPeriod()).thenReturn(vp);
        when(tpdu.getValidityPeriodFormat()).thenReturn(ValidityPeriodFormat.fieldNotPresent);

        MessageEvent msg = mock(MessageEvent.class);
        factory.setValidityPeriod(msg, tpdu);
        verify(msg).setValidityPeriod(80L);
    }

    @ParameterizedTest
    @MethodSource("absoluteTimeStampParameters")
    void getAbsoluteTimeStamp(TimeOfCalendar value, int expectedDateValue) {
        AbsoluteTimeStamp result = factory.getAbsoluteTimeStampImpl();
        int timeResult = getTimeOfCalendar(value, result);
        boolean isValidResult = dateValuesAreTheSame(value, timeResult, expectedDateValue);
        assertTrue(isValidResult);
    }

    @Test
    void setValidityPeriod() {
        SmsSubmitTpdu tpdu = mock(SmsSubmitTpdu.class);
        when(tpdu.getValidityPeriod()).thenReturn(null);
        when(tpdu.getValidityPeriodFormat()).thenReturn(null);
        MessageEvent msg = mock(MessageEvent.class);
        factory.setValidityPeriod(msg, tpdu);
        verify(msg, never()).setValidityPeriod(anyLong());
    }

    @Test
    void setValidityPeriodNull() {
        SmsSubmitTpdu tpdu = mock(SmsSubmitTpdu.class);
        when(tpdu.getValidityPeriod()).thenReturn(mock(ValidityPeriod.class));
        when(tpdu.getValidityPeriodFormat()).thenReturn(null);
        MessageEvent msg = mock(MessageEvent.class);
        factory.setValidityPeriod(msg, tpdu);
        verify(msg, never()).setValidityPeriod(anyLong());
    }

    @Test
    void buildUserDataForMessageGsm7() {
        MessageEvent e = mock(MessageEvent.class);
        when(e.getDataCoding()).thenReturn(0);
        when(e.getShortMessage()).thenReturn("hi");
        when(e.getUdhBytes()).thenReturn(new byte[]{0x01, 0x02});
        DataCodingScheme dcs = mock(DataCodingScheme.class);
        UserData ud = factory.buildUserDataForMt(e, dcs);
        assertEquals("hi", ud.getDecodedMessage());
    }

    @Test
    void buildUserDataForMessage() {
        byte[] udh = {0x0A, 0x0B};
        byte[] msg = {0x11, 0x22};
        MessageEvent messageEvent = MessageEvent.builder()
                .dataCoding(8)
                .shortMessage("hi")
                .udhBytes(new byte[]{0x01, 0x02})
                .udhLength(2)
                .udhBytes(udh)
                .messageBytes(msg)
                .build();
        DataCodingScheme dcs = mock(DataCodingScheme.class);
        byte[] expected = {0x0A, 0x0B, 0x11, 0x22};
        try (var mocked = mockStatic(EncodingUtils.class)) {
            mocked.when(() -> EncodingUtils.prepend(udh, msg)).thenReturn(expected);
            UserData ud = factory.buildUserDataForMt(messageEvent, dcs);
            assertArrayEquals(expected, ud.getEncodedData());
        }
    }

    @Test
    void processValid() {
        MessageEvent msg = mock(MessageEvent.class);
        byte[] encoded = {0x00, 0x01, 0x02};
        UserData ud = mock(UserData.class);
        when(ud.getEncodedData()).thenReturn(encoded);
        when(ud.getDecodedMessage()).thenReturn("decoded");
        try (var mocked = mockStatic(EncodingUtils.class)) {
            mocked.when(() -> EncodingUtils.parseUdh(encoded, msg)).thenAnswer(inv -> null);
            mocked.when(() -> EncodingUtils.getCleanedBytes(encoded)).thenReturn(new byte[]{0x01, 0x02});
            factory.processUdhAndShortMessage(msg, ud, true, 0, true);
            verify(msg).setShortMessage("decoded");
            var cap = org.mockito.ArgumentCaptor.forClass(byte[].class);
            verify(msg).setMessageBytes(cap.capture());
            assertArrayEquals(new byte[]{0x01, 0x02}, cap.getValue());
        }
    }

    @Test
    void processInvalid() {
        MessageEvent msg = mock(MessageEvent.class);
        byte[] encoded = {0x01, 0x02};
        UserData ud = mock(UserData.class);
        when(ud.getEncodedData()).thenReturn(encoded);
        try (var mocked = mockStatic(EncodingUtils.class)) {
            mocked.when(() -> EncodingUtils.getEncodingTypeFromDataCoding(4)).thenReturn(4);
            mocked.when(() -> EncodingUtils.bytesToHex(encoded)).thenReturn("0102");
            mocked.when(() -> EncodingUtils.encodeMessage("0102", 4)).thenReturn(new byte[]{0x07});
            factory.processUdhAndShortMessage(msg, ud, false, 4, false);
            verify(msg).setUdhLength(0);
            verify(msg).setUdhBytes(new byte[0]);
            verify(msg).setShortMessage("0102");
            var cap = org.mockito.ArgumentCaptor.forClass(byte[].class);
            verify(msg).setMessageBytes(cap.capture());
            assertArrayEquals(new byte[]{0x07}, cap.getValue());
        }
    }

    private int getTimeOfCalendar(TimeOfCalendar time, AbsoluteTimeStamp result) {
        return switch (time) {
            case YEAR -> result.getYear();
            case MONTH -> result.getMonth();
            case DAY -> result.getDay();
            case HOUR -> result.getHour();
            case MINUTE -> result.getMinute();
            case SECOND -> result.getSecond();
        };
    }

    private boolean dateValuesAreTheSame(TimeOfCalendar timeOfCalendar, int timeResult, int expectedDateValue) {
        boolean evaluateDelay = timeOfCalendar == TimeOfCalendar.MINUTE || timeOfCalendar == TimeOfCalendar.SECOND;
        return evaluateDelay
                ? (Math.abs(timeResult - expectedDateValue) < ALLOWED_DELAY_FOR_MIN_OR_SEC)
                : (timeResult == expectedDateValue);
    }


    enum TimeOfCalendar {YEAR, MONTH, DAY, HOUR, MINUTE, SECOND}
}

