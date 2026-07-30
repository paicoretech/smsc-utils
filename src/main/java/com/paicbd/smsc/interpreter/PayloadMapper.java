package com.paicbd.smsc.interpreter;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.paicbd.smsc.dto.CallbackHeaderHttp;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PayloadMapper {
    @JsonProperty("event_type")
    private String eventType;
    private String direction;
    @JsonProperty("body_type")
    private String bodyType;
    private String template;
    private String path;
    @JsonProperty("use_proxy")
    private boolean useProxy = false;
    @JsonProperty("default_template")
    private boolean defaultTemplate = false;
    @JsonProperty("callback_headers_http")
    private List<CallbackHeaderHttp> callbackHeadersHttp = new ArrayList<>();
}
