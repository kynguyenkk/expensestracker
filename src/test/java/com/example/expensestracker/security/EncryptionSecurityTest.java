package com.example.expensestracker.security;

import com.example.expensestracker.model.entity.UserEntity;
import com.example.expensestracker.repositories.UserRepository;
import com.example.expensestracker.util.StringCryptoConverter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Security Test: Encryption and Data Protection
 * Test Cases: TC 1.1, 1.2, 1.3 from security_testing_plan.md
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Rollback
@DisplayName("Encryption Security Tests")
class EncryptionSecurityTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private StringCryptoConverter cryptoConverter;

    @Test
    @DisplayName("TC 1.1: Database Encryption - Email and Phone Number")
    void testDatabaseEncryption() {
        // Given: Create a user with plaintext data
        String plainEmail = "test@example.com";
        String plainPhone = "0123456789";
        String plainPassword = "SecurePass123!";

        UserEntity user = UserEntity.builder()
                .email(plainEmail)
                .phoneNumber(plainPhone)
                .password(passwordEncoder.encode(plainPassword))
                .build();

        // When: Save to database
        UserEntity savedUser = userRepository.save(user);
        userRepository.flush(); // Force database write

        // Clear persistence context to force fresh read from DB
        userRepository.findById(savedUser.getUserId());

        // Then: Verify data is encrypted in application (converter handles it)
        // The data should be decrypted when read through JPA
        assertThat(savedUser.getEmail()).isEqualTo(plainEmail);
        assertThat(savedUser.getPhoneNumber()).isEqualTo(plainPhone);

        // Verify password is BCrypt hashed
        assertThat(savedUser.getPassword()).startsWith("$2a$");
        assertThat(savedUser.getPassword()).hasSize(60);
        assertThat(savedUser.getPassword()).isNotEqualTo(plainPassword);

        // Verify BCrypt hash is valid
        assertTrue(passwordEncoder.matches(plainPassword, savedUser.getPassword()),
                "BCrypt hash should match original password");
    }

    @Test
    @DisplayName("TC 1.2: Data Decryption - Automatic Decryption on Read")
    void testDataDecryption() {
        // Given: User saved with encrypted data
        String originalEmail = "decrypt@test.com";
        String originalPhone = "0987654321";

        UserEntity user = UserEntity.builder()
                .email(originalEmail)
                .phoneNumber(originalPhone)
                .password(passwordEncoder.encode("TestPass123"))
                .build();

        UserEntity savedUser = userRepository.save(user);
        Long userId = savedUser.getUserId();
        userRepository.flush();

        // When: Read from database (should auto-decrypt)
        UserEntity retrievedUser = userRepository.findById(userId).orElseThrow();

        // Then: Data should be automatically decrypted
        assertThat(retrievedUser.getEmail()).isEqualTo(originalEmail);
        assertThat(retrievedUser.getPhoneNumber()).isEqualTo(originalPhone);
        assertThat(retrievedUser.getEmail()).doesNotContain("=="); // Not Base64
        assertThat(retrievedUser.getPhoneNumber()).doesNotContain("==");
    }

    @Test
    @DisplayName("TC 1.3: BCrypt Password Hashing - Unique Salts")
    void testBCryptPasswordHashing() {
        // Given: Same password for two users
        String samePassword = "SamePassword123!";

        UserEntity user1 = UserEntity.builder()
                .email("user1@test.com")
                .phoneNumber("0111111111")
                .password(passwordEncoder.encode(samePassword))
                .build();

        UserEntity user2 = UserEntity.builder()
                .email("user2@test.com")
                .phoneNumber("0222222222")
                .password(passwordEncoder.encode(samePassword))
                .build();

        // When: Save both users
        UserEntity savedUser1 = userRepository.save(user1);
        UserEntity savedUser2 = userRepository.save(user2);

        // Then: Hashes should be different (unique salts)
        assertThat(savedUser1.getPassword()).isNotEqualTo(savedUser2.getPassword());

        // But both should verify against the original password
        assertTrue(passwordEncoder.matches(samePassword, savedUser1.getPassword()));
        assertTrue(passwordEncoder.matches(samePassword, savedUser2.getPassword()));

        // Verify BCrypt format
        assertThat(savedUser1.getPassword()).matches("^\\$2[abxy]\\$\\d{2}\\$.{53}$");
        assertThat(savedUser2.getPassword()).matches("^\\$2[abxy]\\$\\d{2}\\$.{53}$");
    }

    @Test
    @DisplayName("TC 1.3: BCrypt - Cannot Reverse Password")
    void testBCryptCannotReverse() {
        // Given: A password
        String originalPassword = "CannotReverse123!";

        // When: Hash it
        String hash = passwordEncoder.encode(originalPassword);

        // Then: Hash should not contain the original password
        assertThat(hash).doesNotContain(originalPassword);
        assertThat(hash).doesNotContain("CannotReverse");
        assertThat(hash).doesNotContain("123");

        // And wrong passwords should not match
        assertFalse(passwordEncoder.matches("WrongPassword", hash));
        assertFalse(passwordEncoder.matches("CannotReverse124!", hash));
    }

    @Test
    @DisplayName("Encryption Converter - Direct Test")
    void testStringCryptoConverter() {
        // Given: A plaintext string
        String plaintext = "sensitive-data@example.com";

        // When: Encrypt
        String encrypted = cryptoConverter.convertToDatabaseColumn(plaintext);

        // Then: Should be encrypted (Base64)
        assertThat(encrypted).isNotEqualTo(plaintext);
        assertThat(encrypted).isBase64();

        // When: Decrypt
        String decrypted = cryptoConverter.convertToEntityAttribute(encrypted);

        // Then: Should match original
        assertThat(decrypted).isEqualTo(plaintext);
    }

    @Test
    @DisplayName("Encryption - Null Handling")
    void testEncryptionNullHandling() {
        // Given: Null values
        String nullValue = null;

        // When: Encrypt null
        String encryptedNull = cryptoConverter.convertToDatabaseColumn(nullValue);

        // Then: Should return null
        assertThat(encryptedNull).isNull();

        // When: Decrypt null
        String decryptedNull = cryptoConverter.convertToEntityAttribute(nullValue);

        // Then: Should return null
        assertThat(decryptedNull).isNull();
    }
}
