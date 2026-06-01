package com.example.user_service.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private final String secret = "mySuperSecretKeyForJwtSigningMustBeAtLeast256BitsLong!!";
    private final long expiration = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", secret);
        ReflectionTestUtils.setField(jwtUtil, "expiration", expiration);
        jwtUtil.init();
    }

    @Test
    void testGenerateAndValidateToken() {
        String email = "test@example.com";
        String role = "CUSTOMER";
        Long userId = 123L;

        String token = jwtUtil.generateToken(email, role, userId);
        assertNotNull(token);
        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    void testExtractClaims() {
        String email = "seller@example.com";
        String role = "ROLE_SELLER";
        Long userId = 456L;

        String token = jwtUtil.generateToken(email, role, userId);

        assertEquals(email, jwtUtil.extractUsername(token));
        assertEquals("SELLER", jwtUtil.extractRole(token)); // ROLE_ prefix stripped
        assertEquals(userId, jwtUtil.extractUserId(token));
    }

    @Test
    void testValidateInvalidToken() {
        assertFalse(jwtUtil.validateToken("invalidToken"));
        assertFalse(jwtUtil.validateToken(null));
    }
}
