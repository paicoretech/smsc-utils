package com.paicbd.smsc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Http2GatewayIncomingSecurity {

    @JsonProperty("oauth2_enabled")
    private boolean oauth2Enabled;

    @JsonProperty("expected_issuer")
    private String expectedIssuer;

    @JsonProperty("require_mtls")
    private boolean requireMtls;

    @JsonProperty("ca_certificate_path")
    private String caCertificatePath;

    @JsonProperty("expected_amf_certificate_path")
    private String expectedAmfCertificatePath;

    @JsonProperty("server_certificate_path")
    private String serverCertificatePath;

    @JsonProperty("server_private_key_path")
    private String serverPrivateKeyPath;
}