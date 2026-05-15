package com.paicbd.smsc.dto.diameter;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.paicbd.smsc.utils.Generated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@Generated
@NoArgsConstructor
@AllArgsConstructor
public class Parameters {
    @JsonProperty("accept_undefined_peer")
    private boolean acceptUndefinedPeer;
    @JsonProperty("duplicate_protection")
    private boolean duplicateProtection;
    @JsonProperty("duplicate_timer")
    private int duplicateTimer;
    @JsonProperty("duplicate_size")
    private int duplicateSize;
    @JsonProperty("use_uri_as_fqdn")
    private boolean useUriAsFqdn;
    @JsonProperty("queue_size")
    private int queueSize;
    @JsonProperty("message_time_out")
    private int messageTimeOut;
    @JsonProperty("stop_time_out")
    private int stopTimeOut;
    @JsonProperty("cea_time_out")
    private int ceaTimeOut;
    @JsonProperty("iac_time_out")
    private int iacTimeOut;
    @JsonProperty("dwa_time_out")
    private int dwaTimeOut;
    @JsonProperty("dpa_time_out")
    private int dpaTimeOut;
    @JsonProperty("rec_time_out")
    private int recTimeOut;
    @JsonProperty("peer_fsm_thread_count")
    private int peerFSMThreadCount;

    @JsonProperty("single_local_peer")
    private boolean singleLocalPeer;
    @JsonProperty("session_time_out")
    private long sessionTimeOut;
    @JsonProperty("bind_delay")
    private long bindDelay;

    @JsonProperty("request_table")
    private RequestTable requestTable;
}
