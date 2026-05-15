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
public class Realm {
    private int id;
    private String name;

    private String uri;
    private String peers;
    @JsonProperty("local_action")
    private String localAction;
    private boolean dynamic;
    @JsonProperty("exp_time")
    private int expTime;
    private Application application;
}
