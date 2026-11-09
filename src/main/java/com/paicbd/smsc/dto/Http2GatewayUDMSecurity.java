package com.paicbd.smsc.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Http2GatewayUDMSecurity {
    @JsonProperty("forward_registration_enabled")
    private boolean forwardRegistrationEnabled;

    @JsonProperty("mtls_enabled")
    private boolean mtlsEnabled;

    @JsonProperty("client_certificate_path")
    private String clientCertificatePath;

    @JsonProperty("client_private_key_path")
    private String clientPrivateKeyPath;

    @JsonProperty("truststore_ca_path")
    private String truststoreCaPath;

    // default using NRF token
    @JsonProperty("oauth2_enabled")
    private boolean oauth2Enabled;

    @JsonProperty("authorization_header")
    private String authorizationHeader = "Authorization";

    @JsonProperty("nrf_discovery_enabled")
    private boolean nrfDiscoveryEnabled;

    // Used only when NRF Discovery is disabled.
    @JsonProperty("api_root")
    private String apiRoot;

    @JsonProperty("registration_path")
    private String registrationPath;

    @JsonProperty("deregistration_path")
    private String deregistrationPath;

    @JsonProperty("subscriber_query_path")
    private String subscriberQueryPath;

    // Automatically resolved from the selected SS7 Gateway.
    @JsonProperty("smsf_map_address")
    private String smsfMapAddress;

    // Automatically resolved from the selected Diameter Gateway.
    @JsonProperty("smsf_diameter_address")
    private String smsfDiameterAddress;

    /**
     * Compatibility accessor for modules compiled against the original field name.
     */
    @JsonIgnore
    @SuppressWarnings("java:S100")
    public String getSmsf_diameter_address() {
        return smsfDiameterAddress;
    }

    /**
     * Compatibility accessor for modules compiled against the original field name.
     */
    @JsonIgnore
    @SuppressWarnings("java:S100")
    public void setSmsf_diameter_address(String smsfDiameterAddress) {
        this.smsfDiameterAddress = smsfDiameterAddress;
    }
}
