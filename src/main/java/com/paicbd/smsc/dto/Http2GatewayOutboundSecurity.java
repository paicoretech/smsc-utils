package com.paicbd.smsc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Http2GatewayOutboundSecurity {

    @JsonProperty("oauth2_enabled")
    private boolean oauth2Enabled;

    @JsonProperty("using_nrf")
    private boolean usingNrf;

    @JsonProperty("using_external_endpoint")
    private boolean usingExternalEndpoint;

    @JsonProperty("token_endpoint")
    private String tokenEndpoint;

    @JsonProperty("client_id")
    private String clientId;

    @JsonProperty(value = "client_secret", access = JsonProperty.Access.WRITE_ONLY)
    private String clientSecret;

    @JsonProperty("scope")
    private String scope;

    @JsonProperty("mtls_enabled")
    private boolean mtlsEnabled;

    @JsonProperty("client_certificate_path")
    private String clientCertificatePath;

    @JsonProperty("client_private_key_path")
    private String clientPrivateKeyPath;

    @JsonProperty("truststore_ca_path")
    private String truststoreCaPath;
}