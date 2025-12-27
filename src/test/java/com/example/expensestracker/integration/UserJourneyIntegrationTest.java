package com.example.expensestracker.integration;

import com.example.expensestracker.model.dto.request.UserLoginDTO;
import com.example.expensestracker.model.dto.request.UserRegisterDTO;
import com.example.expensestracker.model.dto.response.LoginResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration Test: Complete User Journey
 * Test Cases: TC 6.1, 6.2 from security_testing_plan.md
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
@Rollback
@DisplayName("User Journey Integration Tests")
class UserJourneyIntegrationTest {

        @Autowired
        private TestRestTemplate restTemplate;

        @Test
        @DisplayName("TC 6.1: Complete User Journey - Register → Login → Logout")
        void testCompleteUserJourney() {
                String uniquePhone = "0" + System.currentTimeMillis() % 1000000000;
                String uniqueEmail = "journey" + System.currentTimeMillis() + "@test.com";
                String password = "Journey123!";

                // Step 1: Register
                UserRegisterDTO registerDTO = new UserRegisterDTO();
                registerDTO.setPhoneNumber(uniquePhone);
                registerDTO.setEmail(uniqueEmail);
                registerDTO.setPassword(password);
                registerDTO.setRetypePassword(password);

                ResponseEntity<Map> registerResponse = restTemplate.postForEntity(
                                "/api/users/register",
                                registerDTO,
                                Map.class);

                assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

                // Step 2: Login
                UserLoginDTO loginDTO = new UserLoginDTO();
                loginDTO.setPhoneNumber(uniquePhone);
                loginDTO.setPassword(password);

                ResponseEntity<LoginResponse> loginResponse = restTemplate.postForEntity(
                                "/api/users/login",
                                loginDTO,
                                LoginResponse.class);

                assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(loginResponse.getBody()).isNotNull();
                assertThat(loginResponse.getBody().getAccessToken()).isNotNull();
                assertThat(loginResponse.getBody().getRefreshToken()).isNotNull();

                String accessToken = loginResponse.getBody().getAccessToken();

                // Step 3: Access protected endpoint with token
                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(accessToken);
                HttpEntity<Void> request = new HttpEntity<>(headers);

                ResponseEntity<Object> transactionsResponse = restTemplate.exchange(
                                "/api/transactions/search", // Use /search endpoint (returns List)
                                HttpMethod.GET,
                                request,
                                Object.class);

                assertThat(transactionsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

                // Step 4: Logout
                ResponseEntity<Map> logoutResponse = restTemplate.exchange(
                                "/api/users/logout",
                                HttpMethod.POST,
                                request,
                                Map.class);

                assertThat(logoutResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

                // Step 5: Try to access after logout (should fail)
                ResponseEntity<Object> afterLogoutResponse = restTemplate.exchange(
                                "/api/transactions/search", // Use /search endpoint
                                HttpMethod.GET,
                                request,
                                Object.class);

                assertThat(afterLogoutResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("TC 3.1: Protected Endpoint Requires Authentication")
        void testProtectedEndpointRequiresAuth() {
                // When: Access protected endpoint without token
                ResponseEntity<Object> response = restTemplate.getForEntity(
                                "/api/transactions/search", // Use /search endpoint
                                Object.class);

                // Then: Should be forbidden/unauthorized
                assertThat(response.getStatusCode()).isIn(HttpStatus.FORBIDDEN, HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("TC 3.1: Invalid Token Rejected")
        void testInvalidTokenRejected() {
                // Given: Invalid token
                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth("invalid.token.here");
                HttpEntity<Void> request = new HttpEntity<>(headers);

                // When: Access protected endpoint
                ResponseEntity<Object> response = restTemplate.exchange(
                                "/api/transactions/search", // Use /search endpoint
                                HttpMethod.GET,
                                request,
                                Object.class);

                // Then: Should be unauthorized
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("Public Endpoints Accessible Without Auth")
        void testPublicEndpointsAccessible() {
                // Test register endpoint
                ResponseEntity<Map> registerResponse = restTemplate.postForEntity(
                                "/api/users/register",
                                new UserRegisterDTO(),
                                Map.class);

                // Should not be unauthorized (might be bad request due to validation)
                assertThat(registerResponse.getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);

                // Test login endpoint
                ResponseEntity<Map> loginResponse = restTemplate.postForEntity(
                                "/api/users/login",
                                new UserLoginDTO(),
                                Map.class);

                // Should not be unauthorized
                assertThat(loginResponse.getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("Token Refresh Works")
        void testTokenRefresh() {
                // Given: Register and login
                String uniquePhone = "0" + System.currentTimeMillis() % 1000000000;
                String uniqueEmail = "refresh" + System.currentTimeMillis() + "@test.com";

                UserRegisterDTO registerDTO = new UserRegisterDTO();
                registerDTO.setPhoneNumber(uniquePhone);
                registerDTO.setEmail(uniqueEmail);
                registerDTO.setPassword("RefreshTest123!");
                registerDTO.setRetypePassword("RefreshTest123!"); // Add retype password

                restTemplate.postForEntity("/api/users/register", registerDTO, Map.class);

                UserLoginDTO loginDTO = new UserLoginDTO();
                loginDTO.setPhoneNumber(uniquePhone);
                loginDTO.setPassword("RefreshTest123!");

                ResponseEntity<LoginResponse> loginResponse = restTemplate.postForEntity(
                                "/api/users/login",
                                loginDTO,
                                LoginResponse.class);

                String refreshToken = loginResponse.getBody().getRefreshToken();

                // When: Use refresh token
                Map<String, String> refreshRequest = new HashMap<>();
                refreshRequest.put("refreshToken", refreshToken);

                ResponseEntity<LoginResponse> refreshResponse = restTemplate.postForEntity(
                                "/api/users/refresh-token",
                                refreshRequest,
                                LoginResponse.class);

                // Then: Should get new access token
                assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(refreshResponse.getBody()).isNotNull();
                assertThat(refreshResponse.getBody().getAccessToken()).isNotNull();
        }
}
