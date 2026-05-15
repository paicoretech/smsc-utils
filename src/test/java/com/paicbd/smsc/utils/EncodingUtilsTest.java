package com.paicbd.smsc.utils;

import com.paicbd.smsc.dto.MessageEvent;
import com.paicbd.smsc.dto.Udh;
import org.jsmpp.bean.DataCoding;
import org.jsmpp.bean.DataCodings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static com.paicbd.smsc.utils.ConverterTest.testPrivateConstructor;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EncodingUtilsTest {
    @Test
    void testSmppEncodingTestPrivateConstructor() throws NoSuchMethodException {
        testPrivateConstructor(EncodingUtils.class);
    }

    @Test
    void getDataCodingTest() {
        DataCoding dc0 = EncodingUtils.getDataCoding(0);
        DataCoding dc3 = EncodingUtils.getDataCoding(3);
        DataCoding dc8 = EncodingUtils.getDataCoding(8);

        assertEquals(DataCodings.newInstance((byte) 0), dc0);
        assertEquals(DataCodings.newInstance((byte) 3), dc3);
        assertEquals(DataCodings.newInstance((byte) 8), dc8);

        assertDoesNotThrow(() -> EncodingUtils.getDataCoding(10));
    }

    @Test
    void encodeAndDecodeMessageTest() {
        String message = "Hello, World!";
        byte[] gsm7 = EncodingUtils.encodeMessage(message, 0);
        byte[] ucs2 = EncodingUtils.encodeMessage(message, 2);
        byte[] iso88591 = EncodingUtils.encodeMessage(message, 3);
        byte[] utf8 = EncodingUtils.encodeMessage(message, 1);

        assertEquals("Hello, World!", EncodingUtils.decodeMessage(gsm7, 0));
        assertEquals("Hello, World!", EncodingUtils.decodeMessage(ucs2, 2));
        assertEquals("Hello, World!", EncodingUtils.decodeMessage(iso88591, 3));
        assertEquals("Hello, World!", EncodingUtils.decodeMessage(utf8, 1));

        String hexString = "48656c6c6f2c20576f726c6421".toUpperCase();
        byte[] hex = EncodingUtils.encodeMessage(hexString, 11);
        String expected = EncodingUtils.decodeMessage(hex, 11);
        assertEquals(hexString, expected.toUpperCase());

        String helloString = "Hello!";
        String helloHex = "48656C6C6F21";
        byte[] helloStringBytes = EncodingUtils.encodeMessage(helloString, EncodingUtils.ISO88591);
        byte[] helloHexBytesHex = EncodingUtils.encodeMessage(helloHex, EncodingUtils.ISO88591);
        assertArrayEquals(helloStringBytes, helloHexBytesHex);

        var hex1 = EncodingUtils.decodeMessage(helloStringBytes, EncodingUtils.ISO88591);
        var hex2 = EncodingUtils.decodeMessage(helloHexBytesHex, EncodingUtils.ISO88591);
        assertEquals(hex1, hex2);
    }

    @Test
    void isValidDataCodingTest() {
        assertDoesNotThrow(() -> EncodingUtils.isValidDataCoding(0));
        assertDoesNotThrow(() -> EncodingUtils.isValidDataCoding(3));
        assertDoesNotThrow(() -> EncodingUtils.isValidDataCoding(8));
        assertDoesNotThrow(() -> EncodingUtils.isValidDataCoding(100));
        assertFalse(EncodingUtils.isValidDataCoding(10));
        assertTrue(EncodingUtils.isValidDataCoding(0));
    }

    @Test
    @DisplayName("buildConcatenationUdh should build 8-bit UDH for reference <= 255")
    void buildConcatenationUdhShouldBuild8BitUdh() {
        byte[] udh = EncodingUtils.buildConcatenationUdh(4, 2, 1, false);
        assertArrayEquals(new byte[]{0x05, 0x00, 0x03, 0x04, 0x02, 0x01}, udh);
    }

    @Test
    @DisplayName("buildConcatenationUdh should build 16-bit UDH for small reference")
    void buildConcatenationUdhShouldBuild16BitUdhForSmallReference() {
        byte[] udh = EncodingUtils.buildConcatenationUdh(4, 2, 1, true);
        assertArrayEquals(new byte[]{0x06, 0x08, 0x04, 0x00, 0x04, 0x02, 0x01}, udh);
    }

    @Test
    @DisplayName("buildConcatenationUdh should build 16-bit UDH for reference > 255")
    void buildConcatenationUdhShouldBuild16BitUdhForReferenceAbove255() {
        byte[] udh = EncodingUtils.buildConcatenationUdh(400, 3, 2, true);
        assertArrayEquals(new byte[]{0x06, 0x08, 0x04, 0x01, (byte) 0x90, 0x03, 0x02}, udh);
    }

    @Test
    @DisplayName("applyConcatenationUdhPrefix should set 8-bit UDH when no existing UDH")
    void applyConcatenationUdhPrefixShouldSet8BitUdhWhenNoExistingUdh() {
        MessageEvent event = MessageEvent.builder()
                .msgReferenceNumber("26")
                .totalSegment(2)
                .segmentSequence(1)
                .messageBytes(new byte[]{0x48, 0x65})
                .build();

        EncodingUtils.applyConcatenationUdhPrefix(event, false);

        assertEquals(5, event.getUdhLength());
        assertArrayEquals(new byte[]{0x05, 0x00, 0x03, 0x1A, 0x02, 0x01}, event.getUdhBytes());
        assertArrayEquals(new byte[]{0x05, 0x00, 0x03, 0x1A, 0x02, 0x01, 0x48, 0x65}, event.getMessageBytes());
    }

    @Test
    @DisplayName("applyConcatenationUdhPrefix should set 16-bit UDH when no existing UDH")
    void applyConcatenationUdhPrefixShouldSet16BitUdhWhenNoExistingUdh() {
        MessageEvent event = MessageEvent.builder()
                .msgReferenceNumber("26")
                .totalSegment(2)
                .segmentSequence(1)
                .messageBytes(new byte[]{0x48, 0x65})
                .build();

        EncodingUtils.applyConcatenationUdhPrefix(event, true);

        assertEquals(6, event.getUdhLength());
        assertArrayEquals(new byte[]{0x06, 0x08, 0x04, 0x00, 0x1A, 0x02, 0x01}, event.getUdhBytes());
        assertArrayEquals(new byte[]{0x06, 0x08, 0x04, 0x00, 0x1A, 0x02, 0x01, 0x48, 0x65}, event.getMessageBytes());
    }

    @Test
    @DisplayName("applyConcatenationUdhPrefix should combine with existing UDH")
    void applyConcatenationUdhPrefixShouldCombineWithExistingUdh() {
        Set<Udh> udhRaw = new HashSet<>();
        udhRaw.add(new Udh("04", "FF"));
        byte[] existingUdhBytes = new byte[]{0x04, (byte) 0xFF, (byte) 0xEE, (byte) 0xDD, (byte) 0xCC};
        byte[] content = new byte[]{0x0A, 0x0B};
        MessageEvent event = MessageEvent.builder()
                .msgReferenceNumber("27")
                .totalSegment(3)
                .segmentSequence(2)
                .messageBytes(EncodingUtils.prepend(existingUdhBytes, content))
                .udhRaw(udhRaw)
                .udhLength(4)
                .udhBytes(existingUdhBytes)
                .build();

        EncodingUtils.applyConcatenationUdhPrefix(event, true);

        assertEquals(10, event.getUdhLength());
        assertArrayEquals(new byte[]{
                0x0A,
                0x08, 0x04, 0x00, 0x1B, 0x03, 0x02,
                (byte) 0xFF, (byte) 0xEE, (byte) 0xDD, (byte) 0xCC
        }, event.getUdhBytes());
        assertArrayEquals(new byte[]{
                0x0A,
                0x08, 0x04, 0x00, 0x1B, 0x03, 0x02,
                (byte) 0xFF, (byte) 0xEE, (byte) 0xDD, (byte) 0xCC,
                0x0A, 0x0B
        }, event.getMessageBytes());
    }

    @Test
    @DisplayName("udhContainsConcatenationIei should return true for IEI 0x00")
    void udhContainsConcatenationIeiShouldReturnTrueForIei00() {
        MessageEvent event = MessageEvent.builder()
                .udhRaw(Set.of(new Udh("04", "FF"), new Udh("00", "030A0201")))
                .build();
        assertTrue(EncodingUtils.udhContainsConcatenationIei(event));
    }

    @Test
    @DisplayName("udhContainsConcatenationIei should return true for IEI 0x08")
    void udhContainsConcatenationIeiShouldReturnTrueForIei08() {
        MessageEvent event = MessageEvent.builder()
                .udhRaw(Set.of(new Udh("04", "FF"), new Udh("08", "04019003")))
                .build();
        assertTrue(EncodingUtils.udhContainsConcatenationIei(event));
    }

    @Test
    @DisplayName("udhContainsConcatenationIei should return false when no concatenation IEI")
    void udhContainsConcatenationIeiShouldReturnFalseWhenNoConcatenationIei() {
        MessageEvent event = MessageEvent.builder()
                .udhRaw(Set.of(new Udh("04", "FF"), new Udh("02", "AA")))
                .build();
        assertFalse(EncodingUtils.udhContainsConcatenationIei(event));
    }

    @Test
    @DisplayName("udhContainsConcatenationIei should return false when udhRaw is null")
    void udhContainsConcatenationIeiShouldReturnFalseWhenNull() {
        MessageEvent event = MessageEvent.builder().build();
        event.setUdhRaw(null);
        assertFalse(EncodingUtils.udhContainsConcatenationIei(event));
    }

    @Test
    @DisplayName("applyConcatenationUdhFields should set 8-bit UDH and leave messageBytes unchanged")
    void applyConcatenationUdhFieldsShouldSet8BitUdh() {
        byte[] originalBytes = new byte[]{0x48, 0x65};
        MessageEvent event = MessageEvent.builder()
                .msgReferenceNumber("26")
                .totalSegment(2)
                .segmentSequence(1)
                .messageBytes(originalBytes)
                .build();

        EncodingUtils.applyConcatenationUdhFields(event, false);

        assertEquals(5, event.getUdhLength());
        assertArrayEquals(new byte[]{0x05, 0x00, 0x03, 0x1A, 0x02, 0x01}, event.getUdhBytes());
        assertArrayEquals(originalBytes, event.getMessageBytes());
    }

    @Test
    @DisplayName("applyConcatenationUdhFields should set 16-bit UDH and leave messageBytes unchanged")
    void applyConcatenationUdhFieldsShouldSet16BitUdh() {
        byte[] originalBytes = new byte[]{0x48, 0x65};
        MessageEvent event = MessageEvent.builder()
                .msgReferenceNumber("26")
                .totalSegment(2)
                .segmentSequence(1)
                .messageBytes(originalBytes)
                .build();

        EncodingUtils.applyConcatenationUdhFields(event, true);

        assertEquals(6, event.getUdhLength());
        assertArrayEquals(new byte[]{0x06, 0x08, 0x04, 0x00, 0x1A, 0x02, 0x01}, event.getUdhBytes());
        assertArrayEquals(originalBytes, event.getMessageBytes());
    }

    @Test
    @DisplayName("applyConcatenationUdhFields should merge with existing UDH and leave messageBytes unchanged")
    void applyConcatenationUdhFieldsShouldMergeWithExistingUdh() {
        Set<Udh> udhRaw = new HashSet<>();
        udhRaw.add(new Udh("04", "FF"));
        byte[] existingUdhBytes = new byte[]{0x04, (byte) 0xFF, (byte) 0xEE, (byte) 0xDD, (byte) 0xCC};
        byte[] originalBytes = new byte[]{0x0A, 0x0B};
        MessageEvent event = MessageEvent.builder()
                .msgReferenceNumber("27")
                .totalSegment(3)
                .segmentSequence(2)
                .messageBytes(originalBytes)
                .udhRaw(udhRaw)
                .udhLength(4)
                .udhBytes(existingUdhBytes)
                .build();

        EncodingUtils.applyConcatenationUdhFields(event, true);

        assertEquals(10, event.getUdhLength());
        assertArrayEquals(new byte[]{
                0x0A,
                0x08, 0x04, 0x00, 0x1B, 0x03, 0x02,
                (byte) 0xFF, (byte) 0xEE, (byte) 0xDD, (byte) 0xCC
        }, event.getUdhBytes());
        assertArrayEquals(originalBytes, event.getMessageBytes());
    }
}
