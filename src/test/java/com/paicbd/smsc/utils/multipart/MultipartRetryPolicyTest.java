package com.paicbd.smsc.utils.multipart;

import com.paicbd.smsc.dto.MessageEvent;
import com.paicbd.smsc.dto.MessagePart;
import com.paicbd.smsc.utils.Converter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultipartRetryPolicyTest {

    private static MessagePart part(int sequence, boolean sent) {
        return MessagePart.builder()
                .messageId("part-" + sequence)
                .segmentSequence(sequence)
                .totalSegment(3)
                .sent(sent)
                .build();
    }

    private static MessageEvent messageWith(boolean retry, boolean retryAllParts) {
        MessageEvent message = MessageEvent.builder()
                .messageId("test-msg-1")
                .parentId("parent-001")
                .totalSegment(3)
                .isRetry(retry)
                .build();
        message.setRetryAllParts(retryAllParts);
        return message;
    }

    @Test
    @DisplayName("retry_all_parts true resolves to RETRY_ALL")
    void retryAllPartsTrueResolvesToRetryAll() {
        assertEquals(
                MultipartRetryMode.RETRY_ALL,
                MultipartRetryPolicy.resolve(messageWith(true, true)));
    }

    @Test
    @DisplayName("retry_all_parts false resolves to FAILED_ONLY")
    void retryAllPartsFalseResolvesToFailedOnly() {
        assertEquals(
                MultipartRetryMode.FAILED_ONLY,
                MultipartRetryPolicy.resolve(messageWith(true, false)));
    }

    @Test
    @DisplayName("Absent retry_all_parts keeps the MessageEvent default and retries all")
    void retryAllPartsAbsentResolvesToRetryAll() {
        MessageEvent fromJson = Converter.stringToObject(
                "{\"message_id\":\"test-msg-1\"}",
                MessageEvent.class);

        assertEquals(MultipartRetryMode.RETRY_ALL, MultipartRetryPolicy.resolve(fromJson));
    }

    @Test
    @DisplayName("A null message safely falls back to RETRY_ALL")
    void nullMessageResolvesToRetryAll() {
        assertEquals(MultipartRetryMode.RETRY_ALL, MultipartRetryPolicy.resolve(null));
    }

    @Test
    @DisplayName("FAILED_ONLY resumes only during a retry")
    void failedOnlyResumesOnlyDuringRetry() {
        assertTrue(MultipartRetryPolicy.shouldResumeFromFailedPart(messageWith(true, false)));
        assertFalse(MultipartRetryPolicy.shouldResumeFromFailedPart(messageWith(false, false)));
        assertFalse(MultipartRetryPolicy.shouldResumeFromFailedPart(messageWith(true, true)));
        assertFalse(MultipartRetryPolicy.shouldResumeFromFailedPart(null));
    }

    @Test
    @DisplayName("Parts are ordered by segment sequence and null sequences are placed last")
    void orderBySegmentSequenceOrdersParts() {
        MessagePart withoutSequence = MessagePart.builder().messageId("no-sequence").build();

        List<MessagePart> ordered = MultipartRetryPolicy.orderBySegmentSequence(
                List.of(part(3, false), withoutSequence, part(1, false), part(2, false)));

        assertEquals(Arrays.asList(1, 2, 3, null),
                ordered.stream().map(MessagePart::getSegmentSequence).toList());
    }

    @Test
    @DisplayName("Null and empty part lists are safe")
    void orderBySegmentSequenceHandlesEmptyInput() {
        assertTrue(MultipartRetryPolicy.orderBySegmentSequence(null).isEmpty());
        assertTrue(MultipartRetryPolicy.orderBySegmentSequence(List.of()).isEmpty());
    }

    @Test
    @DisplayName("FAILED_ONLY resumes at the first unsent segment")
    void resolveResumeSegmentFindsFirstPendingPart() {
        MessageEvent message = messageWith(true, false);
        List<MessagePart> ordered = List.of(
                part(1, true),
                part(2, false),
                part(3, false));

        assertEquals(2, MultipartRetryPolicy.resolveResumeSegment(message, ordered));
    }

    @Test
    @DisplayName("An inconsistent T-F-T state still resumes from the first unsent part")
    void resolveResumeSegmentHandlesInconsistentLaterSuccess() {
        MessageEvent message = messageWith(true, false);
        List<MessagePart> ordered = List.of(
                part(1, true),
                part(2, false),
                part(3, true));

        assertEquals(2, MultipartRetryPolicy.resolveResumeSegment(message, ordered));
    }

    @Test
    @DisplayName("If every part is marked sent, retry defensively starts from the first segment")
    void resolveResumeSegmentFallsBackWhenAllSent() {
        MessageEvent message = messageWith(true, false);
        List<MessagePart> ordered = List.of(
                part(1, true),
                part(2, true),
                part(3, true));

        assertEquals(MultipartRetryPolicy.FIRST_SEGMENT,
                MultipartRetryPolicy.resolveResumeSegment(message, ordered));
    }

    @Test
    @DisplayName("An out-of-range pending segment falls back to the first segment")
    void resolveResumeSegmentFallsBackForOutOfRangeSegment() {
        MessageEvent message = messageWith(true, false);
        List<MessagePart> ordered = List.of(
                part(1, true),
                part(2, true),
                MessagePart.builder()
                        .messageId("part-4")
                        .segmentSequence(4)
                        .totalSegment(3)
                        .sent(false)
                        .build());

        assertEquals(MultipartRetryPolicy.FIRST_SEGMENT,
                MultipartRetryPolicy.resolveResumeSegment(message, ordered));
    }

    @Test
    @DisplayName("Missing multipart state safely falls back to the first segment")
    void resolveResumeSegmentHandlesMissingInput() {
        MessageEvent message = messageWith(true, false);

        assertEquals(MultipartRetryPolicy.FIRST_SEGMENT,
                MultipartRetryPolicy.resolveResumeSegment(message, null));
        assertEquals(MultipartRetryPolicy.FIRST_SEGMENT,
                MultipartRetryPolicy.resolveResumeSegment(message, List.of()));
        assertEquals(MultipartRetryPolicy.FIRST_SEGMENT,
                MultipartRetryPolicy.resolveResumeSegment(null, List.of(part(1, false))));
    }
}
