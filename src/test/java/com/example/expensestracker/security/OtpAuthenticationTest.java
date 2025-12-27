package com.example.expensestracker.security;

import com.example.expensestracker.model.entity.UserEntity;
import com.example.expensestracker.repositories.UserRepository;
import com.example.expensestracker.service.UserService;
import com.example.expensestracker.util.OtpUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Security Test: OTP Authentication
 * Test Cases: TC 2.4, 2.5 from security_testing_plan.md
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Rollback
@DisplayName("OTP Authentication Security Tests")
class OtpAuthenticationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = UserEntity.builder()
                .email("otp-test@example.com")
                .phoneNumber("0123456789")
                .password(passwordEncoder.encode("TestPass123"))
                .build();
        testUser = userRepository.save(testUser);
    }

    @Test
    @DisplayName("TC 2.4: OTP Generation - Format and Length")
    void testOtpGeneration() {
        // When: Generate OTP
        String otp = OtpUtil.generateOtp();

        // Then: Should be 6 digits
        assertThat(otp).hasSize(6);
        assertThat(otp).matches("\\d{6}");

        // Should only contain digits
        assertTrue(otp.chars().allMatch(Character::isDigit));
    }

    @Test
    @DisplayName("TC 2.4: OTP Generation - Random Values")
    void testOtpGenerationRandomness() {
        // When: Generate multiple OTPs
        String otp1 = OtpUtil.generateOtp();
        String otp2 = OtpUtil.generateOtp();
        String otp3 = OtpUtil.generateOtp();

        // Then: Should be different (statistically)
        // Note: There's a tiny chance they could be same randomly
        assertThat(otp1).isNotEqualTo(otp2).isNotEqualTo(otp3);
    }

    @Test
    @DisplayName("TC 2.4: OTP Storage - BCrypt Hashing")
    void testOtpBCryptHashing() {
        // When: Send OTP (this hashes and stores it)
        String email = testUser.getEmail();
        userService.sendOtp(email);

        // Then: Refresh user and check OTP is hashed
        UserEntity user = userRepository.findByEmail(email).orElseThrow();

        assertThat(user.getOtpCode()).isNotNull();
        assertThat(user.getOtpCode()).startsWith("$2"); // BCrypt prefix
        assertThat(user.getOtpCode()).hasSize(60); // BCrypt length

        // OTP expiry should be set
        assertThat(user.getOtpExpiry()).isNotNull();
        assertThat(user.getOtpExpiry()).isAfter(LocalDateTime.now());
    }

    @Test
    @DisplayName("TC 2.4: OTP Expiration - Set to 1 Minute")
    void testOtpExpiration() {
        // When: Send OTP
        userService.sendOtp(testUser.getEmail());

        // Then: OTP should expire in approximately 1 minute
        UserEntity user = userRepository.findByEmail(testUser.getEmail()).orElseThrow();
        LocalDateTime expiry = user.getOtpExpiry();
        LocalDateTime expectedExpiry = LocalDateTime.now().plusMinutes(1);

        // Allow 5 seconds tolerance
        assertThat(expiry).isBetween(
                expectedExpiry.minusSeconds(5),
                expectedExpiry.plusSeconds(5));
    }

    @Test
    @DisplayName("TC 2.4: OTP Verification - Valid OTP Returns Reset Token")
    void testOtpVerificationSuccess() {
        // Given: Send OTP
        userService.sendOtp(testUser.getEmail());

        // Get the hashed OTP from database
        UserEntity user = userRepository.findByEmail(testUser.getEmail()).orElseThrow();
        String otpHash = user.getOtpCode();

        // For testing, we need to know the original OTP
        // Since OTP is random and sent via email, we'll test with a known OTP
        String testOtp = "123456";
        user.setOtpCode(passwordEncoder.encode(testOtp));
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        userRepository.save(user);

        // When: Verify OTP
        String resetToken = userService.verifyOtp(testUser.getEmail(), testOtp);

        // Then: Should return reset token
        assertThat(resetToken).isNotNull();
        assertThat(resetToken).isNotEmpty();

        // User should have reset token set
        user = userRepository.findByEmail(testUser.getEmail()).orElseThrow();
        assertThat(user.getResetPasswordToken()).isNotNull();
        assertThat(user.getResetPasswordTokenExpiry()).isNotNull();

        // OTP should be cleared
        assertThat(user.getOtpCode()).isNull();
        assertThat(user.getOtpExpiry()).isNull();
    }

    @Test
    @DisplayName("TC 2.4: OTP Verification - Invalid OTP Rejected")
    void testOtpVerificationInvalidOtp() {
        // Given: Valid OTP set
        String validOtp = "123456";
        testUser.setOtpCode(passwordEncoder.encode(validOtp));
        testUser.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        userRepository.save(testUser);

        // When/Then: Invalid OTP should throw exception
        assertThrows(DataIntegrityViolationException.class, () -> {
            userService.verifyOtp(testUser.getEmail(), "999999");
        });
    }

    @Test
    @DisplayName("TC 2.4: OTP Verification - Expired OTP Rejected")
    void testOtpVerificationExpiredOtp() {
        // Given: Expired OTP
        String validOtp = "123456";
        testUser.setOtpCode(passwordEncoder.encode(validOtp));
        testUser.setOtpExpiry(LocalDateTime.now().minusMinutes(2)); // Expired
        userRepository.save(testUser);

        // When/Then: Expired OTP should throw exception
        assertThrows(DataIntegrityViolationException.class, () -> {
            userService.verifyOtp(testUser.getEmail(), validOtp);
        });
    }

    @Test
    @DisplayName("TC 2.5: OTP Rate Limiting - Cannot Spam Requests")
    void testOtpRateLimiting() {
        // Given: Send OTP first time
        userService.sendOtp(testUser.getEmail());

        // When/Then: Second request immediately should fail
        assertThrows(IllegalStateException.class, () -> {
            userService.sendOtp(testUser.getEmail());
        }, "OTP đã được gửi và chưa hết hạn");
    }

    @Test
    @DisplayName("TC 2.5: OTP Rate Limiting - Can Request After Expiry")
    void testOtpRateLimitingAfterExpiry() {
        // Given: Send OTP and set it to expired
        userService.sendOtp(testUser.getEmail());

        UserEntity user = userRepository.findByEmail(testUser.getEmail()).orElseThrow();
        user.setOtpExpiry(LocalDateTime.now().minusMinutes(2)); // Expired
        userRepository.save(user);

        // When: Request OTP again (should work since previous expired)
        assertDoesNotThrow(() -> {
            userService.sendOtp(testUser.getEmail());
        });

        // Then: New OTP should be set
        user = userRepository.findByEmail(testUser.getEmail()).orElseThrow();
        assertThat(user.getOtpCode()).isNotNull();
        assertThat(user.getOtpExpiry()).isAfter(LocalDateTime.now());
    }

    @Test
    @DisplayName("Reset Token - Should Have Expiry")
    void testResetTokenExpiry() {
        // Given: Set OTP
        String testOtp = "123456";
        testUser.setOtpCode(passwordEncoder.encode(testOtp));
        testUser.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        userRepository.save(testUser);

        // When: Verify OTP
        userService.verifyOtp(testUser.getEmail(), testOtp);

        // Then: Reset token should have 5 minute expiry
        UserEntity user = userRepository.findByEmail(testUser.getEmail()).orElseThrow();
        LocalDateTime resetExpiry = user.getResetPasswordTokenExpiry();
        LocalDateTime expectedExpiry = LocalDateTime.now().plusMinutes(5);

        assertThat(resetExpiry).isBetween(
                expectedExpiry.minusSeconds(5),
                expectedExpiry.plusSeconds(5));
    }
}
