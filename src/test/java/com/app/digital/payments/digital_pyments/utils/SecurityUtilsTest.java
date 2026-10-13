package com.app.digital.payments.digital_pyments.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class SecurityUtilsTest {

    @Test
    void hashRefreshTokenEsDeterministico() {
        String token = "un-refresh-token-de-prueba";
        assertEquals(SecurityUtils.hashRefreshToken(token), SecurityUtils.hashRefreshToken(token));
    }

    @Test
    void hashRefreshTokenEsDiferenteParaDistintosTokens() {
        assertNotEquals(SecurityUtils.hashRefreshToken("token-a"), SecurityUtils.hashRefreshToken("token-b"));
    }

    @Test
    void hashRefreshTokenTieneFormatoHex256() {
        assertEquals(64, SecurityUtils.hashRefreshToken("token").length());
    }
}
