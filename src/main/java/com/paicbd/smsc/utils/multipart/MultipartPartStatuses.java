package com.paicbd.smsc.utils.multipart;

import com.paicbd.smsc.dto.MessageEvent;
import com.paicbd.smsc.dto.MessagePart;
import com.paicbd.smsc.utils.Generated;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

@Slf4j
public final class MultipartPartStatuses {

    @Generated
    private MultipartPartStatuses() {
        throw new IllegalStateException("Utility Class");
    }

    /**
     * Resets every multipart part to pending/not sent.
     */
    public static void markAllAsNotSent(MessageEvent message) {
        partsOf(message).forEach(part -> part.setSent(false));
    }

    /**
     * Marks the requested segment as successfully sent.
     */
    public static void markAsSent(MessageEvent message, Integer segmentSequence) {
        updateSentStatus(message, segmentSequence, true);
    }

    /**
     * Marks the requested segment as not sent. This is used for a failed active part.
     */
    public static void markAsNotSent(MessageEvent message, Integer segmentSequence) {
        updateSentStatus(message, segmentSequence, false);
    }

    /**
     * Normalizes a FAILED_ONLY retry from the resume segment onward.
     *
     * <p>This protects against inconsistent states such as [true, false, true]. If segment 2 is
     * the first failed/pending part, segment 3 cannot be considered successful for the new
     * continuation attempt and is reset to not sent as well.</p>
     */
    public static void markAsNotSentFromSegment(MessageEvent message, Integer startSegment) {
        if (Objects.isNull(startSegment)) {
            return;
        }

        partsOf(message).stream()
                .filter(part -> Objects.nonNull(part.getSegmentSequence()))
                .filter(part -> part.getSegmentSequence() >= startSegment)
                .forEach(part -> part.setSent(false));
    }

    /**
     * Copies the current sent flags to another MessageEvent containing the same multipart parts.
     * This is useful when a part is restored from Scylla with stale status values.
     */
    public static void copySentFlags(MessageEvent source, MessageEvent target) {
        if (Objects.isNull(source) || Objects.isNull(target)) {
            return;
        }

        Map<Integer, Boolean> sentFlags = sentFlags(source);
        if (sentFlags.isEmpty()) {
            return;
        }

        partsOf(target).forEach(part -> {
            Boolean sent = sentFlags.get(part.getSegmentSequence());
            if (Objects.nonNull(sent)) {
                part.setSent(sent);
            }
        });
    }

    /**
     * Returns the sent state indexed by segment sequence, ordered by segment.
     */
    public static Map<Integer, Boolean> sentFlags(MessageEvent message) {
        Map<Integer, Boolean> flags = new TreeMap<>();
        partsOf(message).forEach(part -> {
            if (Objects.nonNull(part.getSegmentSequence())) {
                flags.put(part.getSegmentSequence(), part.isSent());
            }
        });
        return flags;
    }

    /**
     * Returns true only when the message contains parts and all of them are marked as sent.
     */
    public static boolean allSent(MessageEvent message) {
        List<MessagePart> parts = partsOf(message);
        return !parts.isEmpty() && parts.stream().allMatch(MessagePart::isSent);
    }

    private static void updateSentStatus(MessageEvent message, Integer segmentSequence, boolean sent) {
        if (Objects.isNull(segmentSequence)) {
            return;
        }

        partOf(message, segmentSequence).ifPresent(part -> {
            part.setSent(sent);
            log.debug("Multipart part {} of message {} marked as {}",
                    segmentSequence,
                    Objects.nonNull(message) ? message.getMessageId() : null,
                    sent ? "sent" : "not sent");
        });
    }

    private static List<MessagePart> partsOf(MessageEvent message) {
        return Objects.nonNull(message) && Objects.nonNull(message.getMessageParts())
                ? message.getMessageParts()
                : List.of();
    }

    private static Optional<MessagePart> partOf(MessageEvent message, Integer segmentSequence) {
        return partsOf(message).stream()
                .filter(part -> segmentSequence.equals(part.getSegmentSequence()))
                .findFirst();
    }
}
