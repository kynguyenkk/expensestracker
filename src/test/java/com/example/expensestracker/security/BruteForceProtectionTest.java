package com.example.expensestracker.security;

import com.example.expensestracker.model.entity.UserEntity;
import com.example.expensestracker.repositories.UserRepository;
import com.example.expensestracker.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Security Test: Brute-force Attack Protection
 * Test Cases: TC 4.2, 4.3 from security_testing_plan.md
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Rollback
@DisplayName("Brute-force Protection Security Tests")
class BruteForceProtectionTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private UserEntity testUser;
    private static final String CORRECT_PASSWORD = "CorrectPass123";
    private static final String WRONG_PASSWORD = "WrongPassword";

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = UserEntity.builder()
                .email("bruteforce-test@example.com")
                .phoneNumber("0987654321")
                .password(passwordEncoder.encode(CORRECT_PASSWORD))
                .accountNonLocked(true)
                .failedLoginAttempts(0)
                .build();
        testUser = userRepository.save(testUser);
    }

    @Test
    @DisplayName("TC 4.2: Account Lockout - After 5 Failed Attempts")
    void testAccountLockoutAfter5Attempts() {
        String phoneNumber = testUser.getPhoneNumber();

        // Attempt 1-4: Should track failed attempts
        for (int i = 1; i <= 4; i++) {
            try {
                userService.login(phoneNumber, WRONG_PASSWORD + i);
                fail("Login should fail with wrong password");
            } catch (Exception e) {
                // Expected
            }

            UserEntity user = userRepository.findByPhoneNumber(phoneNumber).orElseThrow();
            assertThat(user.getFailedLoginAttempts()).isEqualTo(i);
            assertTrue(user.isAccountNonLocked());
        }

        // Attempt 5: Should lock account
        assertThrows(BadCredentialsException.class, () -> {
            userService.login(phoneNumber, WRONG_PASSWORD + "5");
        });

        UserEntity user = userRepository.findByPhoneNumber(phoneNumber).orElseThrow();
        assertThat(user.getFailedLoginAttempts()).isEqualTo(5);
        assertFalse(user.isAccountNonLocked());
        assertThat(user.getLockTime()).isNotNull();
        assertThat(user.getLockTime()).isBefore(LocalDateTime.now().plusSeconds(5));
    }

    @Test
    @DisplayName("TC 4.2: Locked Account Rejects Correct Password")
    void testLockedAccountRejectsCorrectPassword() {
        String phoneNumber = testUser.getPhoneNumber();

        // Lock the account by 5 failed attempts
        for (int i = 1; i <= 5; i++) {
            try {
                userService.login(phoneNumber, WRONG_PASSWORD + i);
            } catch (Exception e) {
                // Expected
            }
        }

        // Now try with CORRECT password - should still fail
        assertThrows(BadCredentialsException.class, () -> {
            userService.login(phoneNumber, CORRECT_PASSWORD);
        });

        UserEntity user = userRepository.findByPhoneNumber(phoneNumber).orElseThrow();
        assertFalse(user.isAccountNonLocked());
    }

    @Test
    @DisplayName("TC 4.3: Account Auto-Unlock After 15 Minutes")
    void testAccountAutoUnlockAfter15Minutes() throws Exception {
        String phoneNumber = testUser.getPhoneNumber();

        // Lock the account
        testUser.setFailedLoginAttempts(5);
        testUser.setAccountNonLocked(false);
        testUser.setLockTime(LocalDateTime.now().minusMinutes(16)); // Locked 16 minutes ago
        userRepository.save(testUser);

        // When: Try to login with correct password
        var response = userService.login(phoneNumber, CORRECT_PASSWORD);

        // Then: Should succeed and unlock account
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isNotNull();

        UserEntity user = userRepository.findByPhoneNumber(phoneNumber).orElseThrow();
        assertTrue(user.isAccountNonLocked());
        assertThat(user.getFailedLoginAttempts()).isEqualTo(0);
        assertThat(user.getLockTime()).isNull();
    }

    @Test
    @DisplayName("TC 4.3: Locked Account Still Locked Before 15 Minutes")
    void testAccountStillLockedBefore15Minutes() {
        String phoneNumber = testUser.getPhoneNumber();

        // Lock the account recently (5 minutes ago)
        testUser.setFailedLoginAttempts(5);
        testUser.setAccountNonLocked(false);
        testUser.setLockTime(LocalDateTime.now().minusMinutes(5));
        userRepository.save(testUser);

        // When/Then: Try to login - should fail
        assertThrows(BadCredentialsException.class, () -> {
            userService.login(phoneNumber, CORRECT_PASSWORD);
        });

        UserEntity user = userRepository.findByPhoneNumber(phoneNumber).orElseThrow();
        assertFalse(user.isAccountNonLocked());
    }

    @Test
    @DisplayName("Failed Attempts Reset On Successful Login")
    void testFailedAttemptsResetOnSuccess() throws Exception {
        String phoneNumber = testUser.getPhoneNumber();

        // Set some failed attempts
        testUser.setFailedLoginAttempts(3);
        userRepository.save(testUser);

        // When: Successful login
        userService.login(phoneNumber, CORRECT_PASSWORD);

        // Then: Failed attempts should reset
        UserEntity user = userRepository.findByPhoneNumber(phoneNumber).orElseThrow();
        assertThat(user.getFailedLoginAttempts()).isEqualTo(0);
        assertThat(user.getLockTime()).isNull();
    }

    @Test
    @DisplayName("Failed Attempts Increment Correctly")
    void testFailedAttemptsIncrement() {
        String phoneNumber = testUser.getPhoneNumber();

        // Attempt 1
        try {
            userService.login(phoneNumber, WRONG_PASSWORD);
        } catch (Exception e) {
        }

        UserEntity user = userRepository.findByPhoneNumber(phoneNumber).orElseThrow();
        assertThat(user.getFailedLoginAttempts()).isEqualTo(1);

        // Attempt 2
        try {
            userService.login(phoneNumber, WRONG_PASSWORD + "2");
        } catch (Exception e) {
        }

        user = userRepository.findByPhoneNumber(phoneNumber).orElseThrow();
        assertThat(user.getFailedLoginAttempts()).isEqualTo(2);

        // Attempt 3
        try {
            userService.login(phoneNumber, WRONG_PASSWORD + "3");
        } catch (Exception e) {
        }

        user = userRepository.findByPhoneNumber(phoneNumber).orElseThrow();
        assertThat(user.getFailedLoginAttempts()).isEqualTo(3);
    }

    @Test
    @DisplayName("Lockout Error Message Contains Duration")
    void testLockoutErrorMessage() {
        String phoneNumber = testUser.getPhoneNumber();

        // Lock the account
        for (int i = 1; i <= 5; i++) {
            try {
                userService.login(phoneNumber, WRONG_PASSWORD + i);
            } catch (Exception e) {
            }
        }

        // Verify error message mentions lock duration
        Exception exception = assertThrows(BadCredentialsException.class, () -> {
            userService.login(phoneNumber, CORRECT_PASSWORD);
        });

        String message = exception.getMessage();
        assertThat(message).containsIgnoringCase("khóa");
        assertThat(message).containsIgnoringCase("15");
        assertThat(message).containsIgnoringCase("phút");
    }
}
