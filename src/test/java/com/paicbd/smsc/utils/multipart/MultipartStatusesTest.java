package com.paicbd.smsc.utils.multipart;

import com.paicbd.smsc.dto.MessageEvent;
import com.paicbd.smsc.dto.MessagePart;
import com.paicbd.smsc.utils.Converter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultipartPartStatusesTest {

    private static MessagePart part(int segmentSequence) {
        return MessagePart.builder()
                .messageId("test-msg-" + segmentSequence)
                .segmentSequence(segmentSequence)
                .totalSegment(3)
                .build();
    }

    private static List<MessagePart> threeParts() {
        return List.of(part(1), part(2), part(3));
    }

    private static MessageEvent multipartEvent() {
        return multipartEvent(threeParts());
    }

    private static MessageEvent multipartEvent(List<MessagePart> parts) {
        return MessageEvent.builder()
                .messageId("test-msg-1")
                .parentId("parent-001")
                .msgReferenceNumber("1")
                .totalSegment(3)
                .validityPeriod(60)
                .messageParts(new ArrayList<>(parts))
                .build();
    }

    @Test
    @DisplayName("A new delivery starts with every part not sent")
    void markAllAsNotSentClearsEveryPart() {
        MessageEvent message = multipartEvent();
        message.getMessageParts().get(0).setSent(true);
        message.getMessageParts().get(1).setSent(true);

        MultipartPartStatuses.markAllAsNotSent(message);

        assertTrue(message.getMessageParts().stream().noneMatch(MessagePart::isSent));
    }

    @Test
    @DisplayName("Marking one part as sent does not touch the others")
    void markAsSentChangesOnlyTheGivenSegment() {
        MessageEvent message = multipartEvent();

        MultipartPartStatuses.markAsSent(message, 2);

        assertFalse(message.getMessageParts().get(0).isSent());
        assertTrue(message.getMessageParts().get(1).isSent());
        assertFalse(message.getMessageParts().get(2).isSent());
    }

    @Test
    @DisplayName("Marking one part as not sent does not touch the others")
    void markAsNotSentChangesOnlyTheGivenSegment() {
        MessageEvent message = multipartEvent();
        message.getMessageParts().forEach(part -> part.setSent(true));

        MultipartPartStatuses.markAsNotSent(message, 2);

        assertTrue(message.getMessageParts().get(0).isSent());
        assertFalse(message.getMessageParts().get(1).isSent());
        assertTrue(message.getMessageParts().get(2).isSent());
    }

    @Test
    @DisplayName("Normalizing from the failed segment resets that part and every later part")
    void markAsNotSentFromSegmentNormalizesInconsistentState() {
        MessageEvent message = multipartEvent();
        message.getMessageParts().get(0).setSent(true);
        message.getMessageParts().get(1).setSent(false);
        message.getMessageParts().get(2).setSent(true);

        MultipartPartStatuses.markAsNotSentFromSegment(message, 2);

        assertTrue(message.getMessageParts().get(0).isSent());
        assertFalse(message.getMessageParts().get(1).isSent());
        assertFalse(message.getMessageParts().get(2).isSent());
    }

    @Test
    @DisplayName("Unknown and null segments are no-ops")
    void unknownOrNullSegmentIsNoOp() {
        MessageEvent message = multipartEvent();

        MultipartPartStatuses.markAsSent(message, 9);
        MultipartPartStatuses.markAsSent(message, null);
        MultipartPartStatuses.markAsNotSent(message, 9);
        MultipartPartStatuses.markAsNotSent(message, null);
        MultipartPartStatuses.markAsNotSentFromSegment(message, null);
        MultipartPartStatuses.markAsSent(null, 1);
        MultipartPartStatuses.markAsNotSent(null, 1);

        assertTrue(message.getMessageParts().stream().noneMatch(MessagePart::isSent));
    }

    @Test
    @DisplayName("The flags survive the JSON round trip used by Kafka")
    void sentFlagsSurviveJsonRoundTrip() {
        MessageEvent message = multipartEvent();
        MultipartPartStatuses.markAsSent(message, 1);

        MessageEvent afterRoundTrip = Converter.stringToObject(message.toString(), MessageEvent.class);

        Assertions.assertNotNull(afterRoundTrip);
        assertTrue(afterRoundTrip.getMessageParts().get(0).isSent());
        assertFalse(afterRoundTrip.getMessageParts().get(1).isSent());
    }

    @Test
    @DisplayName("Copying carries current flags onto a part read back from Scylla")
    void copySentFlagsOverwritesStaleStoredFlags() {
        MessageEvent current = multipartEvent();
        MultipartPartStatuses.markAsSent(current, 1);

        MessageEvent storedPart = multipartEvent();
        MultipartPartStatuses.copySentFlags(current, storedPart);

        assertTrue(storedPart.getMessageParts().get(0).isSent());
        assertFalse(storedPart.getMessageParts().get(1).isSent());
    }

    @Test
    @DisplayName("Copying from an event without parts leaves the target untouched")
    void copySentFlagsWithoutSourcePartsKeepsTarget() {
        MessageEvent target = multipartEvent();
        MultipartPartStatuses.markAsSent(target, 1);

        MultipartPartStatuses.copySentFlags(MessageEvent.builder().messageId("x").build(), target);
        MultipartPartStatuses.copySentFlags(null, target);
        MultipartPartStatuses.copySentFlags(target, null);

        assertTrue(target.getMessageParts().getFirst().isSent());
    }

    @Test
    @DisplayName("The flags are reported ordered by segment")
    void sentFlagsAreOrderedBySegment() {
        MessageEvent message = multipartEvent(List.of(part(3), part(1), part(2)));
        MultipartPartStatuses.markAsSent(message, 1);

        Map<Integer, Boolean> flags = MultipartPartStatuses.sentFlags(message);

        assertEquals(List.of(1, 2, 3), List.copyOf(flags.keySet()));
        assertEquals(Boolean.TRUE, flags.get(1));
        assertEquals(Boolean.FALSE, flags.get(2));
    }

    @Test
    @DisplayName("allSent is true only when a multipart event has every part sent")
    void allSentRequiresEveryPart() {
        MessageEvent message = multipartEvent();
        assertFalse(MultipartPartStatuses.allSent(message));

        message.getMessageParts().forEach(part -> part.setSent(true));
        assertTrue(MultipartPartStatuses.allSent(message));

        assertFalse(MultipartPartStatuses.allSent(MessageEvent.builder().messageId("single").build()));
        assertFalse(MultipartPartStatuses.allSent(null));
    }

    @Test
    @DisplayName("A non-multipart message reports no flags and reset is a no-op")
    void singleMessageIsSafe() {
        assertTrue(MultipartPartStatuses.sentFlags(MessageEvent.builder().messageId("x").build()).isEmpty());
        assertTrue(MultipartPartStatuses.sentFlags(null).isEmpty());
        MultipartPartStatuses.markAllAsNotSent(null);
        MultipartPartStatuses.markAsNotSentFromSegment(null, 1);
    }
}
