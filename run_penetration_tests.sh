#!/bin/bash

###############################################################################
# Run Penetration Tests Only
# Requires server running on https://localhost:8443
# WARNING: Simulates real attacks!
###############################################################################

echo ""
echo "╔══════════════════════════════════════════════════════════════╗"
echo "║            Run Penetration Tests                             ║"
echo "║            ⚠  WARNING: Simulates Attacks!                    ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""

# Check if server is running
echo "Checking server status..."

if curl -k -s -o /dev/null -w "%{http_code}" "https://localhost:8443/api/users/login" 2>/dev/null | grep -qE "^[0-9]{3}$"; then
    echo "✓ Server is running on https://localhost:8443"
else
    echo ""
    echo "✗ Server is NOT running!"
    echo ""
    echo "Start server first:"
    echo "  ./mvnw.cmd spring-boot:run"
    echo ""
    exit 1
fi

# Confirm before running
echo ""
echo "⚠  WARNING: This will simulate real security attacks!"
echo "   Only run in test/development environment!"
echo ""
read -p "Continue? (y/N): " -n 1 -r
echo ""

if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Cancelled."
    exit 0
fi

# Run penetration tests
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Running Penetration Tests..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

bash scripts/penetration_tests.sh all | tee penetration_test_results.log

EXIT_CODE=${PIPESTATUS[0]}

# Parse results with Python
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Updating Excel Report..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

if command -v python3 >/dev/null 2>&1; then
    python3 parse_test_results.py
elif command -v python >/dev/null 2>&1; then
    python parse_test_results.py
else
    echo "⚠ Python not found - results saved to penetration_test_results.log"
fi

echo ""
echo "╔══════════════════════════════════════════════════════════════╗"
echo "║                     COMPLETED!                               ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""
echo "📊 Excel Report: security_test_results.csv"
echo "📄 Test Log: penetration_test_results.log"
echo ""
echo "✅ Open security_test_results.csv in Excel!"
echo ""

exit $EXIT_CODE
