package com.example.websocketexample.config.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

import javax.crypto.SecretKey;
import java.lang.reflect.Method;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {
    private JwtTokenProvider jwtTokenProvider;
    private final String SECRET = "mySuperSecretKeyThatIsAtLeast256BitsLongForHS256Algorithm";
    private final int EXPIRATION_MS = 86400000;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        // Устанавливаем значения через рефлексию, так как поля с @Value
        setField(jwtTokenProvider, "jwtSecret", SECRET);
        setField(jwtTokenProvider, "jwtExpirationMs", EXPIRATION_MS);
    }

    @Test
    void generateToken_WithAuthentication_ShouldReturnValidToken() {
        Authentication authentication = new TestingAuthenticationToken("testuser", "password");

        String token = jwtTokenProvider.generateToken(authentication);

        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals("testuser", jwtTokenProvider.getUsernameFromToken(token));
    }

    @Test
    void generateToken_WithUsername_ShouldReturnValidToken() {
        Authentication authentication = new TestingAuthenticationToken("testuser", "password");
        String token = jwtTokenProvider.generateToken(authentication);

        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals("testuser", jwtTokenProvider.getUsernameFromToken(token));
    }

    @Test
    void getUsernameFromToken_ShouldReturnCorrectUsername() {
        Authentication authentication = new TestingAuthenticationToken("testuser", "password");
        String token = jwtTokenProvider.generateToken(authentication);

        String username = jwtTokenProvider.getUsernameFromToken(token);

        assertEquals("testuser", username);
    }

    @Test
    void validateToken_ShouldReturnFalseForExpiredToken() throws Exception {
        // Создаем просроченный токен
        String expiredToken = Jwts.builder()
                .setSubject("testuser")
                .setIssuedAt(new Date(System.currentTimeMillis() - EXPIRATION_MS - 1000))
                .setExpiration(new Date(System.currentTimeMillis() - 1000))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();

        assertFalse(jwtTokenProvider.validateToken(expiredToken));
    }

    @Test
    void validateToken_ShouldReturnFalseForInvalidToken() {
        assertFalse(jwtTokenProvider.validateToken("invalid.token.here"));
    }

    @Test
    void validateToken_ShouldReturnFalseForMalformedToken() {
        assertFalse(jwtTokenProvider.validateToken("header.payload.signature"));
    }

    @Test
    void validateToken_ShouldReturnFalseForEmptyToken() {
        assertFalse(jwtTokenProvider.validateToken(""));
    }

    @Test
    void validateToken_ShouldReturnFalseForNullToken() {
        assertFalse(jwtTokenProvider.validateToken(null));
    }

    @Test
    void getSigningKey_ShouldHandleShortSecret() throws Exception {
        JwtTokenProvider shortSecretProvider = new JwtTokenProvider();
        setField(shortSecretProvider, "jwtSecret", "short");
        setField(shortSecretProvider, "jwtExpirationMs", EXPIRATION_MS);

        assertNotNull(getSigningKey());
    }

    private SecretKey getSigningKey() throws Exception {
        Method method = JwtTokenProvider.class.getDeclaredMethod("getSigningKey");
        method.setAccessible(true);
        return (SecretKey) method.invoke(jwtTokenProvider);
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
    }
}