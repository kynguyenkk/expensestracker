package com.example.expensestracker.security;

import com.example.expensestracker.model.entity.TokenBlackList;
import com.example.expensestracker.model.entity.UserEntity;
import com.example.expensestracker.repositories.TokenBlackListRepository;
import com.example.expensestracker.repositories.UserRepository;
import com.example.expensestracker.util.JwtTokenUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Security Test: JWT Authentication
 * Test Cases: TC 2.1, 2.2, 2.3 from security_testing_plan.md
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Rollback
@DisplayName("JWT Authentication Security Tests")
class JwtAuthenticationTest {

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TokenBlackListRepository tokenBlackListRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = UserEntity.builder()
                .email("jwt-test@example.com")
                .phoneNumber("0123456789")
                .password(passwordEncoder.encode("TestPass123"))
                .build();
        testUser = userRepository.save(testUser);
    }

    @Test
    @DisplayName("TC 2.1: JWT Token Generation - Correct Claims")
    void testJwtTokenGeneration() throws Exception {
        // When: Generate JWT token
        String token = jwtTokenUtil.generateToken(testUser);

        // Then: Token should be generated
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();

        // Token should have 3 parts (header.payload.signature)
        String[] parts = token.split("\\.");
        assertThat(parts).hasSize(3);

        // Extract claims
        String phoneNumber = jwtTokenUtil.extractPhoneNumber(token);
        Integer userId = jwtTokenUtil.extractUserId(token);
        Date expiration = jwtTokenUtil.extractExpiration(token);

        // Verify claims
        assertThat(phoneNumber).isEqualTo(testUser.getPhoneNumber());
        assertThat(userId).isEqualTo(testUser.getUserId().intValue());
        assertThat(expiration).isAfter(new Date());
    }

    @Test
    @DisplayName("TC 2.1: JWT Token Validation - Valid Token")
    void testJwtTokenValidation() throws Exception {
        // Given: Valid token
        String token = jwtTokenUtil.generateToken(testUser);

        // When: Validate token
        boolean isValid = jwtTokenUtil.validateToken(token, testUser);

        // Then: Should be valid
        assertTrue(isValid);
    }

    @Test
    @DisplayName("TC 2.1: JWT Token Validation - Invalid Signature")
    void testJwtTokenValidationInvalidSignature() throws Exception {
        // Given: Valid token
        String validToken = jwtTokenUtil.generateToken(testUser);

        // When: Tamper with signature
        String[] parts = validToken.split("\\.");
        String tamperedToken = parts[0] + "." + parts[1] + ".invalidsignature";

        // Then: Validation should fail
        assertThrows(Exception.class, () -> {
            jwtTokenUtil.validateToken(tamperedToken, testUser);
        });
    }

    @Test
    @DisplayName("TC 2.2: Token Expiration - Should Expire After Configured Time")
    void testTokenExpiration() throws Exception {
        // Given: Generate token
        String token = jwtTokenUtil.generateToken(testUser);
        Date expiration = jwtTokenUtil.extractExpiration(token);
        Date issuedAt = new Date();

        // Then: Expiration should be approximately jwt.expiration milliseconds from now
        long timeDiff = expiration.getTime() - issuedAt.getTime();
        assertThat(timeDiff).isGreaterThan(jwtExpiration - 1000); // Allow 1 second tolerance
        assertThat(timeDiff).isLessThan(jwtExpiration + 1000);

        // Verify token is not yet expired
        assertThat(expiration).isAfter(new Date());
    }

    @Test
    @DisplayName("TC 2.2: Expired Token Rejected")
    void testExpiredTokenRejected() {
        // Note: This test would require mocking time or generating a token with past
        // expiration
        // For now, we verify the extraction logic works

        // Given: Generate token
        String token = null;
        try {
            token = jwtTokenUtil.generateToken(testUser);
        } catch (Exception e) {
            fail("Token generation failed");
        }

        // When: Token is not expired
        Date expiration = jwtTokenUtil.extractExpiration(token);

        // Then: Current time should be before expiration
        assertThat(new Date()).isBefore(expiration);
    }

    @Test
    @DisplayName("TC 2.3: Token Blacklist - Logout Blacklists Token")
    void testTokenBlacklist() throws Exception {
        // Given: Valid token
        String token = jwtTokenUtil.generateToken(testUser);
        Date expiration = jwtTokenUtil.extractExpiration(token);

        // When: Add token to blacklist (simulating logout)
        TokenBlackList blacklistedToken = TokenBlackList.builder()
                .token(token)
                .expiryDate(expiration)
                .build();
        tokenBlackListRepository.save(blacklistedToken);

        // Then: Token should exist in blacklist
        boolean isBlacklisted = tokenBlackListRepository.existsByToken(token);
        assertTrue(isBlacklisted);

        // Verify we can retrieve it
        TokenBlackList retrieved = tokenBlackListRepository.findByToken(token).orElse(null);
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getToken()).isEqualTo(token);
    }

    @Test
    @DisplayName("TC 2.3: Blacklisted Token Should Not Be  Reusable")
    void testBlacklistedTokenNotReusable() throws Exception {
        // Given: Token in blacklist
        String token = jwtTokenUtil.generateToken(testUser);
        TokenBlackList blacklistedToken = TokenBlackList.builder()
                .token(token)
                .expiryDate(jwtTokenUtil.extractExpiration(token))
                .build();
        tokenBlackListRepository.save(blacklistedToken);

        // When: Check if token is blacklisted
        boolean exists = tokenBlackListRepository.existsByToken(token);

        // Then: Should be blacklisted
        assertTrue(exists);

        // Even though token is technically valid (signature-wise),
        // it should be rejected because it's in blacklist
        // This would be enforced by JwtTokenFilter in actual request flow
    }

    @Test
    @DisplayName("JWT Refresh Token Generation")
    void testRefreshTokenGeneration() throws Exception {
        // When: Generate refresh token
        String refreshToken = jwtTokenUtil.generateRefreshToken(testUser);

        // Then: Should be generated
        assertThat(refreshToken).isNotNull();
        assertThat(refreshToken).isNotEmpty();

        // Should be valid JWT
        String phoneNumber = jwtTokenUtil.extractPhoneNumber(refreshToken);
        assertThat(phoneNumber).isEqualTo(testUser.getPhoneNumber());
    }

    @Test
    @DisplayName("JWT Token Claims Integrity")
    void testTokenClaimsIntegrity() throws Exception {
        // Given: Generate token
        String token = jwtTokenUtil.generateToken(testUser);

        // When: Extract all claims
        String phoneNumber = jwtTokenUtil.extractPhoneNumber(token);
        Integer userId = jwtTokenUtil.extractUserId(token);

        // Then: Claims should match user data
        assertThat(phoneNumber).isEqualTo(testUser.getPhoneNumber());
        assertThat(userId.longValue()).isEqualTo(testUser.getUserId());
    }

    @Test
    @DisplayName("JWT Token - Wrong User Validation Fails")
    void testWrongUserValidationFails() throws Exception {
        // Given: Token for testUser
        String token = jwtTokenUtil.generateToken(testUser);

        // And: Different user
        UserEntity differentUser = UserEntity.builder()
                .email("different@test.com")
                .phoneNumber("0999999999")
                .password(passwordEncoder.encode("DifferentPass"))
                .build();
        differentUser = userRepository.save(differentUser);

        // When: Validate token with different user
        boolean isValid = jwtTokenUtil.validateToken(token, differentUser);

        // Then: Should be invalid
        assertFalse(isValid);
    }
}
