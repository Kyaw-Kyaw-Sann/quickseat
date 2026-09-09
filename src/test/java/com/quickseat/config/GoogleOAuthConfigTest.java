package com.quickseat.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

class GoogleOAuthConfigTest {
    @Test void configuresGoogleAuthorizationCodeClient() {
        ClientRegistrationRepository repository = new GoogleOAuthConfig().clientRegistrationRepository("client-id", "client-secret");
        var google = repository.findByRegistrationId("google");
        assertThat(google.getClientId()).isEqualTo("client-id");
        assertThat(google.getScopes()).contains("openid", "profile", "email");
        assertThat(google.getRedirectUri()).contains("/login/oauth2/code/");
    }
}
