#!/bin/bash

###############################################################################
# Penetration Testing Script
# Based on security_testing_plan.md
# Test Cases: TC 4.1 (SQL Injection), TC 7.1 (JWT Manipulation), TC 4.5 (XSS)
#
# ⚠️ WARNING: THESE ARE ATTACK SIMULATIONS!
# - Run ONLY on test/development environments
# - DO NOT run on production servers
# - These tests may trigger security alerts
#
# USAGE: bash penetration_tests.sh [test-name]
# Examples:
#   bash penetration_tests.sh sql-injection
#   bash penetration_tests.sh jwt-manipulation
#   bash penetration_tests.sh all
###############################################################################

set -e

BASE_URL="https://localhost:8443"
API_PREFIX="/api"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

print_warning() {
    echo -e "${RED}⚠️  WARNING ⚠️${NC}"
    echo -e "${RED}$1${NC}"
}

print_header() {
    echo ""
    echo "=========================================="
    echo -e "${BLUE}$1${NC}"
    echo "=========================================="
}

print_test() {
    echo -e "\n${YELLOW}[ATTACK TEST]${NC} $1"
}

print_result() {
    if [ "$1" == "BLOCKED" ]; then
        echo -e "${GREEN}✓ BLOCKED${NC}: Attack was successfully prevented"
    else
        echo -e "${RED}✗ VULNERABLE${NC}: Attack may have succeeded"
    fi
}

###############################################################################
# TC 4.1: SQL Injection Tests
###############################################################################

test_sql_injection() {
    print_header "TC 4.1: SQL Injection Attack Tests"
    
    print_warning "Testing SQL injection attacks..."

    # Test 1: Classic SQL injection in login
    print_test "SQL Injection: OR  1=1 bypass"
    RESPONSE=$(curl -k -s -w "\n%{http_code}" -X POST "$BASE_URL$API_PREFIX/users/login" \
        -H "Content-Type: application/json" \
        -d '{"phoneNumber": "admin\" OR \"1\"=\"1", "password": "anything"}')
    
    HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
    BODY=$(echo "$RESPONSE" | head -n -1)
    
    if [ "$HTTP_CODE" == "401" ] || [ "$HTTP_CODE" == "400" ] || [ "$HTTP_CODE" == "403" ]; then
        print_result "BLOCKED"
    else
        print_result "VULNERABLE"
        echo "Response: $BODY"
    fi

    # Test 2: UNION-based injection
    print_test "SQL Injection: UNION SELECT attack"
    RESPONSE=$(curl -k -s -w "\n%{http_code}" -X POST "$BASE_URL$API_PREFIX/users/login" \
        -H "Content-Type: application/json" \
        -d '{"phoneNumber": "0123456789\" UNION SELECT * FROM users--", "password": "test"}')
    
    HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
    
    if [ "$HTTP_CODE" == "401" ] || [ "$HTTP_CODE" == "400" ]; then
        print_result "BLOCKED"
    else
        print_result "VULNERABLE"
    fi

    # Test 3: Time-based blind injection
    print_test "SQL Injection: Time-based blind (SLEEP)"
    START_TIME=$(date +%s)
    RESPONSE=$(curl -k -s -w "\n%{http_code}" -X POST "$BASE_URL$API_PREFIX/users/login" \
        -H "Content-Type: application/json" \
        -d '{"phoneNumber": "0123456789\" AND SLEEP(5)--", "password": "test"}')
    END_TIME=$(date +%s)
    
    DURATION=$((END_TIME - START_TIME))
    HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
    
    if [ $DURATION -lt 3 ] && [ "$HTTP_CODE" == "401" ]; then
        print_result "BLOCKED"
    else
        print_result "VULNERABLE"
        echo "Response time: ${DURATION}s (should be < 3s)"
    fi

    # Test 4: Boolean-based injection
    print_test "SQL Injection: Boolean-based (AND 1=1)"
    RESPONSE=$(curl -k -s -w "\n%{http_code}" -X POST "$BASE_URL$API_PREFIX/users/login" \
        -H "Content-Type: application/json" \
        -d '{"phoneNumber": "0123456789\" AND 1=1--", "password": "test"}')
    
    HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
    
    if [ "$HTTP_CODE" == "401" ] || [ "$HTTP_CODE" == "400" ]; then
        print_result "BLOCKED"
    else
        print_result "VULNERABLE"
    fi

    # Test 5: Check for SQL error leakage
    print_test "SQL Injection: Error-based (check for SQL errors in response)"
    RESPONSE=$(curl -k -s -X POST "$BASE_URL$API_PREFIX/users/login" \
        -H "Content-Type: application/json" \
        -d '{"phoneNumber": "0123456789\"\"\"", "password": "test"}')
    
    if echo "$RESPONSE" | grep -qi "sql\|syntax\|mysql\|postgresql"; then
        print_result "VULNERABLE"
        echo "SQL error detected in response!"
    else
        print_result "BLOCKED"
    fi
}

###############################################################################
# TC 7.1: JWT Token Manipulation
###############################################################################

test_jwt_manipulation() {
    print_header "TC 7.1: JWT Token Manipulation Tests"
    
    print_warning "Testing JWT token manipulation attacks..."

    # First, get a valid token
    print_test "Getting valid JWT token..."
    
    # Register test user
    TIMESTAMP=$(date +%s)
    TEST_PHONE="09${TIMESTAMP: -8}"
    TEST_EMAIL="pentest${TIMESTAMP}@test.com"
    
    curl -k -s -X POST "$BASE_URL$API_PREFIX/users/register" \
        -H "Content-Type: application/json" \
        -d "{\"phoneNumber\": \"$TEST_PHONE\", \"email\": \"$TEST_EMAIL\", \"password\": \"PenTest123!\"}" > /dev/null

    LOGIN_RESPONSE=$(curl -k -s -X POST "$BASE_URL$API_PREFIX/users/login" \
        -H "Content-Type: application/json" \
        -d "{\"phoneNumber\": \"$TEST_PHONE\", \"password\": \"PenTest123!\"}")
    
    VALID_TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)
    
    if [ -z "$VALID_TOKEN" ]; then
        echo "Failed to get valid token"
        return
    fi
    
    echo "Got valid token: ${VALID_TOKEN:0:20}..."

    # Test 1: Modified payload
    print_test "JWT Manipulation: Modified userId in payload"
    
    # Decode and modify token (this is simplified - real attack would properly encode)
    HEADER=$(echo "$VALID_TOKEN" | cut -d'.' -f1)
    PAYLOAD=$(echo "$VALID_TOKEN" | cut -d'.' -f2)
    SIGNATURE=$(echo "$VALID_TOKEN" | cut -d'.' -f3)
    
    # Create fake token with different payload but same signature
    FAKE_TOKEN="${HEADER}.fakePayloadModified.${SIGNATURE}"
    
    RESPONSE=$(curl -k -s -w "\n%{http_code}" -X GET "$BASE_URL$API_PREFIX/transactions" \
        -H "Authorization: Bearer $FAKE_TOKEN")
    
    HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
    
    if [ "$HTTP_CODE" == "401" ]; then
        print_result "BLOCKED"
    else
        print_result "VULNERABLE"
    fi

    #  Test 2: No signature
    print_test "JWT Manipulation: Token without signature"
    
    NO_SIG_TOKEN="${HEADER}.${PAYLOAD}."
    
    RESPONSE=$(curl -k -s -w "\n%{http_code}" -X GET "$BASE_URL$API_PREFIX/transactions" \
        -H "Authorization: Bearer $NO_SIG_TOKEN")
    
    HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
    
    if [ "$HTTP_CODE" == "401" ]; then
        print_result "BLOCKED"
    else
        print_result "VULNERABLE"
    fi

    # Test 3: Modified signature
    print_test "JWT Manipulation: Invalid signature"
    
    INVALID_SIG_TOKEN="${HEADER}.${PAYLOAD}.invalidsignaturehere"
    
    RESPONSE=$(curl -k -s -w "\n%{http_code}" -X GET "$BASE_URL$API_PREFIX/transactions" \
        -H "Authorization: Bearer $INVALID_SIG_TOKEN")
    
    HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
    
    if [ "$HTTP_CODE" == "401" ]; then
        print_result "BLOCKED"
    else
        print_result "VULNERABLE"
    fi
}

###############################################################################
# TC 4.5: XSS Attack Tests
###############################################################################

test_xss_attacks() {
    print_header "TC 4.5: XSS (Cross-Site Scripting) Tests"
    
    print_warning "Testing XSS attack prevention..."

    # Get valid token first
    TIMESTAMP=$(date +%s)
    TEST_PHONE="09${TIMESTAMP: -8}"
    TEST_EMAIL="xss${TIMESTAMP}@test.com"
    
    curl -k -s -X POST "$BASE_URL$API_PREFIX/users/register" \
        -H "Content-Type: application/json" \
        -d "{\"phoneNumber\": \"$TEST_PHONE\", \"email\": \"$TEST_EMAIL\", \"password\": \"XssTest123!\"}" > /dev/null

    LOGIN_RESPONSE=$(curl -k -s -X POST "$BASE_URL$API_PREFIX/users/login" \
        -H "Content-Type: application/json" \
        -d "{\"phoneNumber\": \"$TEST_PHONE\", \"password\": \"XssTest123!\"}")
    
    TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)

    # Test 1: Script tag in transaction description
    print_test "XSS: <script> tag in transaction description"
    
    XSS_PAYLOAD='<script>alert("XSS")</script>'
    RESPONSE=$(curl -k -s -X POST "$BASE_URL$API_PREFIX/transactions" \
        -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        -d "{\"amount\": 100, \"categoryId\": 1, \"description\": \"$XSS_PAYLOAD\", \"transactionDate\": \"2024-12-27\"}")
    
    # Check if script tag is in response
    if echo "$RESPONSE" | grep -q "<script>"; then
        print_result "VULNERABLE"
    else
        print_result "BLOCKED"
    fi

    # Test 2: Event handler XSS
    print_test "XSS: Event handler (onerror) injection"
    
    XSS_PAYLOAD='<img src=x onerror=alert(1)>'
    RESPONSE=$(curl -k -s -X POST "$BASE_URL$API_PREFIX/transactions" \
        -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        -d "{\"amount\": 100, \"categoryId\": 1, \"description\": \"$XSS_PAYLOAD\", \"transactionDate\": \"2024-12-27\"}")
    
    if echo "$RESPONSE" | grep -q "onerror"; then
        print_result "VULNERABLE"
    else
        print_result "BLOCKED"
    fi

    # Test 3: Check CSP header
    print_test "XSS: Content-Security-Policy header check"
    
    HEADERS=$(curl -k -I -s "$BASE_URL$API_PREFIX/users/login")
    
    if echo "$HEADERS" | grep -qi "Content-Security-Policy"; then
        print_result "BLOCKED"
        echo "CSP header present"
    else
        print_result "VULNERABLE"
        echo "CSP header missing"
    fi
}

###############################################################################
# Main Execution
###############################################################################

show_usage() {
    echo "Usage: bash penetration_tests.sh [test-name]"
    echo ""
    echo "Available tests:"
    echo "  sql-injection      - SQL injection attack tests"
    echo "  jwt-manipulation   - JWT token manipulation tests"
    echo "  xss                - Cross-Site Scripting tests"
    echo "  all                - Run all penetration tests"
    echo ""
    echo "Example: bash penetration_tests.sh sql-injection"
}

main() {
    echo "╔══════════════════════════════════════════════════════════════╗"
    echo "║           ⚠️  PENETRATION TESTING SCRIPT ⚠️                 ║"
    echo "║                                                              ║"
    echo "║  WARNING: These tests simulate real attacks!                ║"
    echo "║  Only run on test/development environments!                 ║"
    echo "║  DO NOT run on production servers!                          ║"
    echo "╚══════════════════════════════════════════════════════════════╝"
    echo ""
    
    read -p "Do you confirm this is a TEST environment? (yes/no): " CONFIRM
    if [[ ! "$CONFIRM" =~ ^[Yy]([Ee][Ss])?$ ]]; then
        echo "Aborted."
        exit 1
    fi

    TEST_NAME="${1:-all}"

    case "$TEST_NAME" in
        sql-injection)
            test_sql_injection
            ;;
        jwt-manipulation)
            test_jwt_manipulation
            ;;
        xss)
            test_xss_attacks
            ;;
        all)
            test_sql_injection
            test_jwt_manipulation
            test_xss_attacks
            ;;
        *)
            show_usage
            exit 1
            ;;
    esac

    echo ""
    echo "╔══════════════════════════════════════════════════════════════╗"
    echo "║  Penetration testing completed                               ║"
    echo "║  Review results above                                        ║"
    echo "║                                                              ║"
    echo "║  ⚠️  All attacks should be BLOCKED for a secure system      ║"
    echo "╚══════════════════════════════════════════════════════════════╝"
}

# Run main
main "$@"
