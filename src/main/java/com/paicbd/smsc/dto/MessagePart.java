package com.paicbd.smsc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.paicbd.smsc.utils.Generated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Set;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class MessagePart {
    @JsonProperty("message_id")
    private String messageId;

    @JsonProperty("short_message")
    private String shortMessage;

    @JsonProperty("msg_reference_number")
    private String msgReferenceNumber;

    @JsonProperty("total_segment")
    private Integer totalSegment;

    @JsonProperty("segment_sequence")
    private Integer segmentSequence;

    // only part without udh
    @JsonProperty("part_bytes")
    private byte[] partBytes;

    @JsonProperty("udh_bytes")
    private byte[] udhBytes;

    @JsonProperty("udh_raw")
    private Set<Udh> udhRaw;

    @JsonProperty("optional_parameters")
    private List<UtilsRecords.OptionalParameter> optionalParameters;
}
