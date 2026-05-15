package com.paicbd.smsc.utils;

import com.paicbd.smsc.dto.MessageEvent;
import com.paicbd.smsc.dto.Udh;
import com.paicbd.smsc.dto.UtilsRecords;
import lombok.extern.slf4j.Slf4j;
import org.jsmpp.bean.DataCoding;
import org.jsmpp.bean.DataCodings;
import org.jsmpp.bean.OptionalParameter;
import org.restcomm.protocols.ss7.map.datacoding.GSMCharset;
import org.restcomm.protocols.ss7.map.datacoding.GSMCharsetDecoder;
import org.restcomm.protocols.ss7.map.datacoding.GSMCharsetDecodingData;
import org.restcomm.protocols.ss7.map.datacoding.GSMCharsetEncoder;
import org.restcomm.protocols.ss7.map.datacoding.GSMCharsetEncodingData;
import org.restcomm.protocols.ss7.map.datacoding.Gsm7EncodingStyle;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class EncodingUtils {
    public static final Set<Integer> GSM7_DATA_CODINGS = Set.of(
            0, 1, 2, 3, 16, 17, 18, 19, 32, 33, 34, 35, 48, 49, 50, 51,
            64, 65, 66, 67, 80, 81, 82, 83, 96, 97, 98, 99, 112, 113, 114, 115,
            208, 209, 210, 211, 212, 213, 214, 215, 216, 217, 218, 219, 220, 221, 222, 223,
            240, 241, 243
    );

    public static final Set<Integer> UCS2_DATA_CODINGS = Set.of(
            8, 9, 10, 11, 24, 25, 26, 27, 40, 41, 42, 43, 56, 57, 58, 59,
            72, 73, 74, 75, 88, 89, 90, 91, 104, 105, 106, 107, 120, 121, 122, 123,
            224, 225, 226, 227, 228, 229, 230, 231, 232, 233, 234, 235, 236, 237, 238, 239
    );

    public static final Set<Short> requiredTagsForConcatenatedSms = Set.of(
            OptionalParameter.Tag.SAR_MSG_REF_NUM.code(),
            OptionalParameter.Tag.SAR_TOTAL_SEGMENTS.code(),
            OptionalParameter.Tag.SAR_SEGMENT_SEQNUM.code()
    );

    public static final int GSM7 = 0;
    public static final int UTF8 = 1;
    public static final int UCS2 = 2;
    public static final int ISO88591 = 3;
    public static final int DCS_0 = 0;
    public static final int DCS_8 = 8;
    public static final int DCS_3 = 3;
    private static final Charset gsm7Charset = new GSMCharset("GSM", new String[]{});
    public static final Set<Integer> udhEsmeClassValues = Set.of(64, 65, 66, 67, 68, 72, 80, 96, 112);

    // UDH byte lengths including the UDHL byte itself
    public static final int CONCATENATION_UDH_8BIT_LENGTH = 6;   // UDHL(1)+IEI(1)+IEDL(1)+ref(1)+total(1)+seq(1)
    public static final int CONCATENATION_UDH_16BIT_LENGTH = 7;  // UDHL(1)+IEI(1)+IEDL(1)+ref_hi(1)+ref_lo(1)+total(1)+seq(1)

    private EncodingUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Builds the UDH byte array for a concatenated SMS segment.
     * Use 8-bit (IEI 0x00) for networks requiring the legacy 6-byte format.
     * Use 16-bit (IEI 0x08) for the extended 7-byte format with a larger reference space.
     */
    public static byte[] buildConcatenationUdh(int msgRef, int total, int seq, boolean use16Bit) {
        if (use16Bit) {
            return new byte[]{
                    0x06,                  // UDHL = 6 (6 bytes follow)
                    0x08,                  // IEI = 0x08 (16-bit concatenation)
                    0x04,                  // IEDL = 4 bytes of IE data
                    (byte) (msgRef >> 8),  // Reference number high byte
                    (byte) msgRef,         // Reference number low byte
                    (byte) total,          // Total number of segments
                    (byte) seq             // Sequence number of this segment
            };
        }
        return new byte[]{
                0x05,          // UDHL = 5 (5 bytes follow)
                0x00,          // IEI = 0x00 (8-bit concatenation)
                0x03,          // IEDL = 3 bytes of IE data
                (byte) msgRef, // Reference number (8-bit, wraps at 256)
                (byte) total,  // Total number of segments
                (byte) seq     // Sequence number of this segment
        };
    }

    /**
     * Prepends the concatenation UDH to a MessageEvent, handling both the case where no
     * UDH exists and the case where one is already present (e.g. originator-provided UDH).
     * Updates udhBytes, udhLength, and messageBytes on the event in place.
     */
    public static void applyConcatenationUdhPrefix(MessageEvent event, boolean use16Bit) {
        int msgRef = Integer.parseInt(event.getMsgReferenceNumber());
        byte[] concatUdh = buildConcatenationUdh(msgRef, event.getTotalSegment(), event.getSegmentSequence(), use16Bit);

        if (event.getUdhLength() > 0) {
            // Existing UDH present — extract content bytes before modifying udhLength,
            // then prepend the concatenation IE to the existing UDH.
            byte[] contentBytes = getCleanedBytes(event);
            byte[] newUdhBytes = buildMergedUdhBytes(event.getUdhBytes(), concatUdh);
            event.setUdhLength(newUdhBytes[0] & 0xFF);
            event.setUdhBytes(newUdhBytes);
            event.setMessageBytes(prepend(newUdhBytes, contentBytes));
        } else {
            // No existing UDH — concatenation UDH becomes the complete UDH.
            byte[] contentBytes = event.getMessageBytes() != null ? event.getMessageBytes() : new byte[0];
            event.setUdhLength(concatUdh[0] & 0xFF);
            event.setUdhBytes(concatUdh);
            event.setMessageBytes(prepend(concatUdh, contentBytes));
        }
    }

    public static void applyConcatenationUdhFields(MessageEvent event, boolean use16Bit) {
        int msgRef = Integer.parseInt(event.getMsgReferenceNumber());
        byte[] concatUdh = buildConcatenationUdh(msgRef, event.getTotalSegment(), event.getSegmentSequence(), use16Bit);

        if (event.getUdhLength() > 0) {
            byte[] newUdhBytes = buildMergedUdhBytes(event.getUdhBytes(), concatUdh);
            event.setUdhLength(newUdhBytes[0] & 0xFF);
            event.setUdhBytes(newUdhBytes);
        } else {
            event.setUdhLength(concatUdh[0] & 0xFF);
            event.setUdhBytes(concatUdh);
        }
    }

    private static byte[] buildMergedUdhBytes(byte[] existingUdh, byte[] concatUdh) {
        byte[] concatIeContent = Arrays.copyOfRange(concatUdh, 1, concatUdh.length);
        int existingIeLen = existingUdh[0] & 0xFF;
        int newUdhl = concatIeContent.length + existingIeLen;
        byte[] newUdhBytes = new byte[1 + newUdhl];
        newUdhBytes[0] = (byte) newUdhl;
        System.arraycopy(concatIeContent, 0, newUdhBytes, 1, concatIeContent.length);
        System.arraycopy(existingUdh, 1, newUdhBytes, 1 + concatIeContent.length, existingIeLen);
        return newUdhBytes;
    }

    /**
     * Returns true if the message event already carries a concatenation UDH element
     * (either 8-bit IEI 0x00 or 16-bit IEI 0x08), indicating segments were already
     * assembled by the originator and no further UDH construction is needed.
     */
    public static boolean udhContainsConcatenationIei(MessageEvent messageEvent) {
        if (Objects.isNull(messageEvent.getUdhRaw()) || messageEvent.getUdhRaw().isEmpty()) {
            return false;
        }
        return messageEvent.getUdhRaw().stream()
                .anyMatch(udh -> Objects.equals(udh.iei(), "00") || Objects.equals(udh.iei(), "08"));
    }

    public static DataCoding getDataCoding(int encodingType) {
        return DataCodings.newInstance((byte) encodingType);
    }

    public static byte[] encodeMessage(String message, int encodingType) {
        return switch (encodingType) {
            case GSM7 -> encodeGsm7(message);
            case UCS2 -> encodeUcs2(message);
            case ISO88591 -> isHexadecimal(message) ? hexToBytes(message) : encodeIso88591(message);
            case UTF8 -> encodeUtf8(message);
            default -> {
                if (isHexadecimal(message)) {
                    yield hexToBytes(message);
                } else {
                    throw new IllegalStateException("Unexpected encodingType value: " + encodingType + " for message: " + message);
                }
            }
        };
    }

    public static String decodeMessage(byte[] messageBytes, int encodingType) {
        return switch (encodingType) {
            case GSM7 -> decodeGsm7(messageBytes);
            case UCS2 -> decodeUcs2(messageBytes);
            case ISO88591 -> decodeIso88591(messageBytes);
            case UTF8 -> decodeUtf8(messageBytes);
            default -> bytesToHex(messageBytes);
        };
    }

    public static int getEncodingTypeFromDataCoding(int dataCoding) {
        if(GSM7_DATA_CODINGS.contains(dataCoding)) {
            return GSM7;
        } else if (UCS2_DATA_CODINGS.contains(dataCoding)) {
            return UCS2;
        } else {
            return ISO88591;
        }
    }

    public static boolean isHexadecimal(String message) {
        return (message.length() % 2 == 0) && message.matches("^[0-9A-Fa-f]+$");
    }

    public static byte[] getCleanedBytes(MessageEvent event) {
        if (event.getUdhLength() > 0) {
            return Arrays.copyOfRange(event.getMessageBytes(), event.getUdhLength() + 1, event.getMessageBytes().length);
        }
        return event.getMessageBytes();
    }

    public static byte[] getCleanedBytes(byte[] incomingBytes) {
        int udhLength = incomingBytes[0] & 0xFF;
        if (udhLength > 0) {
            return Arrays.copyOfRange(incomingBytes, udhLength + 1, incomingBytes.length);
        }
        return incomingBytes;
    }

    public static byte[] getUdhBytes(byte[] messageBytes) {
        int udhLength = messageBytes[0] & 255;
        return Arrays.copyOf(messageBytes, udhLength + 1);
    }

    public static byte[] hexToBytes(String hex) {
        int length = hex.length();
        byte[] data = new byte[length / 2];
        for (int i = 0; i < length; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                                  + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            hexString.append(String.format("%02X", b));
        }
        return hexString.toString();
    }

    public static void parseUdh(byte[] messageBytes, MessageEvent messageEvent) {
        int udhLength = messageBytes[0] & 0xFF;
        if (udhLength == 0) {
            log.debug("UDH Length is not valid: {}", udhLength);
            return;
        }
        byte[] udhBytes = Arrays.copyOf(messageBytes, udhLength + 1);
        messageEvent.setUdhLength(udhLength);
        messageEvent.setUdhBytes(udhBytes);
        int index = 1;
        while (index < udhBytes.length) {
            int ieiPos = index;
            int iei = udhBytes[index++] & 0xFF;
            int iel = udhBytes[index++] & 0xFF;
            if (index + iel > udhBytes.length) {
                break;
            }
            byte[] contentBytes = Arrays.copyOfRange(udhBytes, ieiPos, index + iel);
            index += iel;
            String contentHex = bytesToHex(contentBytes);
            messageEvent.getUdhRaw().add(new Udh(String.format("%02X", iei), contentHex));
        }
    }

    public static byte[] prepend(byte[] prefix, byte[] original) {
        if (prefix == null || original == null) {
            throw new IllegalArgumentException("Arrays cannot be null");
        }

        byte[] result = new byte[prefix.length + original.length];
        System.arraycopy(prefix, 0, result, 0, prefix.length);
        System.arraycopy(original, 0, result, prefix.length, original.length);
        return result;
    }

    private static byte[] encodeGsm7(String message) {
        GSMCharsetEncoder encoder = (GSMCharsetEncoder) gsm7Charset.newEncoder();
        encoder.setGSMCharsetEncodingData(new GSMCharsetEncodingData(Gsm7EncodingStyle.bit8_smpp_style, null));
        ByteBuffer byteBuffer = null;
        try {
            byteBuffer = encoder.encode(CharBuffer.wrap(message));
        } catch (CharacterCodingException e) {
            log.error("Error encoding GSM 7", e);
        }
        if (byteBuffer == null) {
            log.warn("GSM 7 encoding byteBuffer is null");
            return new byte[0];
        }
        byte[] data = new byte[byteBuffer.limit()];
        byteBuffer.get(data);
        return data;
    }

    private static byte[] encodeUcs2(String message) {
        return message.getBytes(StandardCharsets.UTF_16BE);
    }

    private static byte[] encodeIso88591(String message) {
        return message.getBytes(StandardCharsets.ISO_8859_1);
    }

    private static byte[] encodeUtf8(String message) {
        return message.getBytes(StandardCharsets.UTF_8);
    }

    private static String decodeGsm7(byte[] messageBytes) {
        GSMCharsetDecoder decoder = (GSMCharsetDecoder) gsm7Charset.newDecoder();
        decoder.setGSMCharsetDecodingData(new GSMCharsetDecodingData(Gsm7EncodingStyle.bit8_smpp_style,
                Integer.MAX_VALUE, 0));
        ByteBuffer byteBuffer = ByteBuffer.wrap(messageBytes);
        CharBuffer charBuffer = null;
        try {
            charBuffer = decoder.decode(byteBuffer);
        } catch (CharacterCodingException e) {
            log.error("Error decoding message", e);
        }
        if (charBuffer == null) {
            log.warn("GSM 7 decoding charBuffer is null");
            return null;
        }
        return charBuffer.toString();
    }

    private static String decodeUcs2(byte[] messageBytes) {
        return new String(messageBytes, StandardCharsets.UTF_16BE);
    }

    private static String decodeIso88591(byte[] messageBytes) {
        return new String(messageBytes, StandardCharsets.ISO_8859_1);
    }

    private static String decodeUtf8(byte[] messageBytes) {
        return new String(messageBytes, StandardCharsets.UTF_8);
    }

    public static boolean isValidDataCoding(Integer code) {
        int validCode = Optional.ofNullable(code).orElse(0);
        return validCode == DCS_0 || validCode == DCS_3 || validCode == DCS_8;
    }

    public static boolean messageContainsInfoAboutConcatenatedSms(MessageEvent messageEvent) {
        boolean udhApply = Objects.nonNull(messageEvent.getUdhRaw()) && messageEvent.getUdhRaw().stream().anyMatch(udh -> Objects.equals(udh.iei(), "00") || Objects.equals(udh.iei(), "08"));
        if (udhApply) {
            log.debug("Message {} applies for concatenated SMS due to UDH", messageEvent.getMessageId());
            return true;
        }

        if (Objects.isNull(messageEvent.getOptionalParameters())) {
            log.debug("Message {} does not apply for concatenated SMS due to missing optional parameters", messageEvent.getMessageId());
            return false;
        }
        return messageEvent.getOptionalParameters().stream()
                .map(UtilsRecords.OptionalParameter::tag)
                .collect(Collectors.toSet())
                .containsAll(requiredTagsForConcatenatedSms);
    }
}
