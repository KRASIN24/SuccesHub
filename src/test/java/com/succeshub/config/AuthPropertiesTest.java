package com.succeshub.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthPropertiesTest {

    @Test
    void frontendUrlWithSlash_appendsWhenMissing() {
        AuthProperties props = new AuthProperties();
        props.setFrontendUrl("http://localhost:4200");
        assertEquals("http://localhost:4200/", props.frontendUrlWithSlash());
    }

    @Test
    void frontendUrlWithSlash_keepsExistingSlash() {
        AuthProperties props = new AuthProperties();
        props.setFrontendUrl("https://app.example.com/");
        assertEquals("https://app.example.com/", props.frontendUrlWithSlash());
    }

    @Test
    void defaultsMatchLocalDev() {
        AuthProperties props = new AuthProperties();
        assertEquals("http://localhost:4200/", props.frontendUrlWithSlash());
        assertEquals(List.of("http://localhost:4200"), props.getCorsAllowedOrigins());
        assertEquals(
                "http://localhost:8080/realms/succeshub-realm/protocol/openid-connect/logout",
                props.getKeycloakLogoutUri());
    }
}
