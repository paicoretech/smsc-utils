package com.paicbd.smsc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Http2GatewayNrfSecurity {

    @JsonProperty("api_root")
    private String apiRoot;

    @JsonProperty("secret_path")
    private String secretPath;

    @JsonProperty("token_path")
    private String tokenPath;

    @JsonProperty("nrf_register_path")
    private String nrfRegisterPath;

    @JsonProperty("mtls_enabled")
    private boolean mtlsEnabled;

    @JsonProperty("client_certificate_path")
    private String clientCertificatePath;

    @JsonProperty("client_private_key_path")
    private String clientPrivateKeyPath;

    @JsonProperty("truststore_ca_path")
    private String truststoreCaPath;
}
