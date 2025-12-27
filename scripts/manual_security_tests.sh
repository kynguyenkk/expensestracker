#!/bin/bash

###############################################################################
# Manual Security Testing Script
# Based on security_testing_plan.md
# Test Cases: TC 5.1, 5.2, and various manual verification tests
#
# USAGE: bash manual_security_tests.sh
# PREREQUISITE: Server running on https://localhost:8443
###############################################################################

# Removed 'set -e' to continue testing even if some commands fail
# set -e

BASE_URL="https://localhost:8443"
API_PREFIX="/api"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test counters
PASS=0
FAIL=0

print_header() {
    echo ""
    echo "=========================================="
    echo "$1"
    echo "=========================================="
}

print_test() {
    echo -e "\n${YELLOW}[TEST]${NC} $1"
}

print_pass() {
    echo -e "${GREEN}✓ PASS${NC}: $1"
    ((PASS++))
}

print_fail() {
    echo -e "${RED}✗ FAIL${NC}: $1"
    ((FAIL++))
}

###############################################################################
# TC 5.1: HTTPS/TLS Configuration
###############################################################################

test_https_tls() {
    print_header "TC 5.1: HTTPS/TLS Configuration Tests"

    # Test 1: HTTPS is enabled
    print_test "Testing HTTPS connection..."
    # Use curl.exe explicitly on Windows and handle output properly
    HTTP_CODE=$(curl.exe -k -s -o /dev/null -w "%{http_code}" "$BASE_URL$API_PREFIX/users/login" 2>/dev/null || echo "000")
    # Accept any 2xx, 4xx, 5xx code - means server is responding via HTTPS
    if echo "$HTTP_CODE" | grep -qE "^(200|400|401|403|405)$"; then
        print_pass "HTTPS connection established (HTTP $HTTP_CODE)"
    else
        print_fail "HTTPS connection failed (HTTP $HTTP_CODE)"
    fi

    # Test 2: TLS version (skip if openssl hangs)
    print_test "Checking TLS version..."
    if command -v openssl >/dev/null 2>&1; then
        TLS_VERSION=$(timeout 3s sh -c "echo 'Q' | openssl s_client -connect localhost:8443 -tls1_2 2>&1" | grep "Protocol" | head -1 || echo "N/A")
        if echo "$TLS_VERSION" | grep -q "TLSv1.[23]"; then
            print_pass "TLS 1.2/1.3 supported: $TLS_VERSION"
        elif [ "$TLS_VERSION" = "N/A" ]; then
            echo -e "${YELLOW}⊘ SKIP${NC}: TLS check timed out or unavailable"
        else
            print_fail "Weak TLS version: $TLS_VERSION"
        fi
    else
        echo -e "${YELLOW}⊘ SKIP${NC}: openssl not available"
    fi

    # Test 3: Strong cipher suite (skip if openssl hangs)
    print_test "Checking cipher suite..."
    if command -v openssl >/dev/null 2>&1; then
        CIPHER=$(timeout 3s sh -c "echo 'Q' | openssl s_client -connect localhost:8443 2>&1" | grep "Cipher" | head -1 || echo "N/A")
        if echo "$CIPHER" | grep -qE "AES|CHACHA"; then
            print_pass "Strong cipher in use: $CIPHER"
        elif [ "$CIPHER" = "N/A" ]; then
            echo -e "${YELLOW}⊘ SKIP${NC}: Cipher check timed out or unavailable"
        else
            print_fail "Weak cipher: $CIPHER"
        fi
    else
        echo -e "${YELLOW}⊘ SKIP${NC}: openssl not available"
    fi
}

###############################################################################
# TC 5.2: Security Headers
###############################################################################

test_security_headers() {
    print_header "TC 5.2: Security Headers Tests"

    # Use curl.exe and capture headers properly
    RESPONSE_HEADERS=$(curl.exe -k -I -s "$BASE_URL$API_PREFIX/users/login" 2>/dev/null)

    # Test 1: Strict-Transport-Security (HSTS)
    print_test "Checking HSTS header..."
    if echo "$RESPONSE_HEADERS" | grep -qi "Strict-Transport-Security"; then
        HSTS=$(echo "$RESPONSE_HEADERS" | grep -i "Strict-Transport-Security" | tr -d '\r')
        if echo "$HSTS" | grep -q "max-age=31536000"; then
            print_pass "HSTS header present with 1 year max-age"
        else
            print_fail "HSTS max-age insufficient"
        fi
    else
        print_fail "HSTS header missing"
    fi

    # Test 2: X-Frame-Options
    print_test "Checking X-Frame-Options..."
    if echo "$RESPONSE_HEADERS" | grep -qi "X-Frame-Options.*DENY"; then
        print_pass "X-Frame-Options: DENY present"
    else
        print_fail "X-Frame-Options missing or incorrect"
    fi

    # Test 3: Content-Security-Policy
    print_test "Checking Content-Security-Policy..."
    if echo "$RESPONSE_HEADERS" | grep -qi "Content-Security-Policy"; then
        print_pass "Content-Security-Policy header present"
    else
        print_fail "Content-Security-Policy header missing"
    fi

    # Test 4: X-Content-Type-Options
    print_test "Checking X-Content-Type-Options..."
    if echo "$RESPONSE_HEADERS" | grep -qi "X-Content-Type-Options.*nosniff"; then
        print_pass "X-Content-Type-Options: nosniff present"
    else
        print_fail "X-Content-Type-Options missing"
    fi
}

###############################################################################
# Basic Authentication Flow Test
###############################################################################

test_authentication_flow() {
    print_header "Basic Authentication Flow Test"

    # Test unique credentials
    TIMESTAMP=$(date +%s)
    TEST_PHONE="09${TIMESTAMP: -8}"
    TEST_EMAIL="test${TIMESTAMP}@example.com"
    TEST_PASSWORD="TestPass123"  # No ! to avoid bash expansion issues

    # Test 1: Register user
    print_test "Registering new user..."
    REGISTER_RESPONSE=$(curl.exe -k -s -w "\n%{http_code}" -X POST "$BASE_URL$API_PREFIX/users/register" \
        -H "Content-Type: application/json" \
        -d "{\"phone_number\": \"$TEST_PHONE\", \"email\": \"$TEST_EMAIL\", \"password\": \"$TEST_PASSWORD\", \"retype_password\": \"$TEST_PASSWORD\"}" 2>/dev/null)
    
    HTTP_CODE=$(echo "$REGISTER_RESPONSE" | tail -n1 | tr -d '\r')
    RESPONSE_BODY=$(echo "$REGISTER_RESPONSE" | head -n -1 | tr -d '\r')
    
    if [ "$HTTP_CODE" == "200" ]; then
        print_pass "User registration successful"
    else
        print_fail "User registration failed (HTTP $HTTP_CODE)"
        echo "  Phone: $TEST_PHONE, Email: $TEST_EMAIL"
        echo "  Response: $RESPONSE_BODY"
        return
    fi

    # Test 2: Login
    print_test "Logging in..."
    LOGIN_RESPONSE=$(curl.exe -k -s -w "\n%{http_code}" -X POST "$BASE_URL$API_PREFIX/users/login" \
        -H "Content-Type: application/json" \
        -d "{\"phone_number\": \"$TEST_PHONE\", \"password\": \"$TEST_PASSWORD\"}" 2>/dev/null)
    
    HTTP_CODE=$(echo "$LOGIN_RESPONSE" | tail -n1 | tr -d '\r')
    BODY=$(echo "$LOGIN_RESPONSE" | head -n -1 | tr -d '\r')
    
    if [ "$HTTP_CODE" == "200" ]; then
        ACCESS_TOKEN=$(echo "$BODY" | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)
        if [ -n "$ACCESS_TOKEN" ]; then
            print_pass "Login successful, token received"
        else
            print_fail "Token not found in response"
            return
        fi
    else
        print_fail "Login failed (HTTP $HTTP_CODE)"
        return
    fi

    # Test 3: Access protected endpoint
    print_test "Accessing protected endpoint..."
    PROTECTED_RESPONSE=$(curl.exe -k -s -w "\n%{http_code}" -X GET "$BASE_URL$API_PREFIX/transactions/search" \
        -H "Authorization: Bearer $ACCESS_TOKEN" 2>/dev/null)
    
    HTTP_CODE=$(echo "$PROTECTED_RESPONSE" | tail -n1 | tr -d '\r')
    if [ "$HTTP_CODE" == "200" ]; then
        print_pass "Protected endpoint accessible with token"
    else
        print_fail "Protected endpoint access failed (HTTP $HTTP_CODE)"
    fi

    # Test 4: Access without token
    print_test "Accessing protected endpoint without token..."
    NO_TOKEN_RESPONSE=$(curl.exe -k -s -w "\n%{http_code}" -X GET "$BASE_URL$API_PREFIX/transactions/search" 2>/dev/null)
    
    HTTP_CODE=$(echo "$NO_TOKEN_RESPONSE" | tail -n1 | tr -d '\r')
    if [ "$HTTP_CODE" == "401" ] || [ "$HTTP_CODE" == "403" ]; then
        print_pass "Protected endpoint rejected request without token"
    else
        print_fail "Protected endpoint should reject requests without token (got HTTP $HTTP_CODE)"
    fi
}

###############################################################################
# Rate Limiting Test
###############################################################################

test_rate_limiting() {
    print_header "TC 4.4: Rate Limiting Test"

    print_test "Sending rapid requests to test rate limiting..."
    
    RATE_LIMITED=false
    for i in {1..15}; do
        HTTP_CODE=$(curl.exe -k -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL$API_PREFIX/users/login" \
            -H "Content-Type: application/json" \
            -d '{"phoneNumber": "0000000000", "password": "test"}' 2>/dev/null | tr -d '\r')
        
        echo -n "Request $i: HTTP $HTTP_CODE "
        
        if [ "$HTTP_CODE" == "429" ]; then
            echo -e "${GREEN}[RATE LIMITED]${NC}"
            RATE_LIMITED=true
            break
        else
            echo ""
        fi
    done

    if [ "$RATE_LIMITED" = true ]; then
        print_pass "Rate limiting active - request blocked after threshold"
    else
        print_fail "Rate limiting not working - all 15 requests passed"
    fi
}

###############################################################################
# Main Execution
###############################################################################

main() {
    echo "╔══════════════════════════════════════════════════════════════╗"
    echo "║        Security Testing Script - ExpensesTracker            ║"
    echo "║        Testing server: $BASE_URL                   ║"
    echo "╚══════════════════════════════════════════════════════════════╝"

    # Check if server is running
    print_test "Checking if server is running..."
    # Try curl (works in Git Bash) or curl.exe fallback
    if command -v curl >/dev/null 2>&1; then
        HTTP_CODE=$(curl -k -s -o /dev/null -w "%{http_code}" "$BASE_URL$API_PREFIX/users/login" 2>/dev/null || echo "000")
    elif command -v curl.exe >/dev/null 2>&1; then
        HTTP_CODE=$(curl.exe -k -s -o /dev/null -w "%{http_code}" "$BASE_URL$API_PREFIX/users/login" 2>/dev/null || echo "000")
    else
        HTTP_CODE="000"
    fi
    
    # Check if we got a valid HTTP response
    if [ "$HTTP_CODE" != "000" ] && [ -n "$HTTP_CODE" ]; then
        print_pass "Server is running (HTTP $HTTP_CODE)"
    else
        print_fail "Server is not running on $BASE_URL"
        echo "Please start the server first: mvn spring-boot:run"
        exit 1
    fi

    # Run test suites
    test_https_tls
    test_security_headers
    test_authentication_flow
    test_rate_limiting

    # Summary
    print_header "Test Summary"
    TOTAL=$((PASS + FAIL))
    echo -e "Total Tests: $TOTAL"
    echo -e "${GREEN}Passed: $PASS${NC}"
    echo -e "${RED}Failed: $FAIL${NC}"
    
    if [ $FAIL -eq 0 ]; then
        echo -e "\n${GREEN}✓ All tests passed!${NC}"
        exit 0
    else
        echo -e "\n${RED}✗ Some tests failed${NC}"
        exit 1
    fi
}

# Run main
main
