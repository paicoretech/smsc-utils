package com.paicbd.smsc.utils.multipart;

import com.paicbd.smsc.dto.MessageEvent;
import com.paicbd.smsc.dto.MessagePart;
import com.paicbd.smsc.utils.Generated;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Slf4j
public final class MultipartRetryPolicy {

    public static final int FIRST_SEGMENT = 1;

    @Generated
    private MultipartRetryPolicy() {
        throw new IllegalStateException("Utility Class");
    }

    /**
     * Resolves the configured multipart retry mode. A null message falls back to RETRY_ALL.
     */
    public static MultipartRetryMode resolve(MessageEvent message) {
        if (Objects.isNull(message)) {
            return MultipartRetryMode.RETRY_ALL;
        }

        return message.isRetryAllParts()
                ? MultipartRetryMode.RETRY_ALL
                : MultipartRetryMode.FAILED_ONLY;
    }

    /**
     * FAILED_ONLY is meaningful only during a retry. Initial delivery always starts from the first
     * segment even when retry_all_parts is configured as false.
     */
    public static boolean shouldResumeFromFailedPart(MessageEvent message) {
        return Objects.nonNull(message)
                && message.isRetry()
                && MultipartRetryMode.FAILED_ONLY.equals(resolve(message));
    }

    /**
     * Returns a new list ordered by segment_sequence. Null segment values are placed last.
     */
    public static List<MessagePart> orderBySegmentSequence(List<MessagePart> parts) {
        if (Objects.isNull(parts) || parts.isEmpty()) {
            return List.of();
        }

        return parts.stream()
                .sorted(Comparator.comparing(
                        MessagePart::getSegmentSequence,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    /**
     * Resolves the segment from which a FAILED_ONLY retry must resume.
     *
     * <p>Defensive fallbacks mirror the SS7 behavior:</p>
     * <ul>
     *     <li>If every part is already marked as sent, retry from segment 1.</li>
     *     <li>If the first pending segment is outside total_segment, retry from segment 1.</li>
     *     <li>If there are no usable parts, retry from segment 1.</li>
     * </ul>
     */
    public static int resolveResumeSegment(MessageEvent message, List<MessagePart> orderedParts) {
        if (Objects.isNull(message) || Objects.isNull(orderedParts) || orderedParts.isEmpty()) {
            return FIRST_SEGMENT;
        }

        Integer firstNotDelivered = orderedParts.stream()
                .filter(Objects::nonNull)
                .filter(part -> !part.isSent())
                .map(MessagePart::getSegmentSequence)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

        if (Objects.isNull(firstNotDelivered)) {
            log.warn("Every part of parentId {} is marked as delivered but the message came back to be retried; "
                            + "sending the whole message again",
                    message.getParentId());
            return FIRST_SEGMENT;
        }

        Integer totalSegments = message.getTotalSegment();
        if (Objects.nonNull(totalSegments)
                && (firstNotDelivered < FIRST_SEGMENT || firstNotDelivered > totalSegments)) {
            log.warn("Resume segment {} of parentId {} is out of range for {} segments; "
                            + "sending the whole message again",
                    firstNotDelivered,
                    message.getParentId(),
                    totalSegments);
            return FIRST_SEGMENT;
        }

        return firstNotDelivered;
    }
}
