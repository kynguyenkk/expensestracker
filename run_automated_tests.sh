#!/bin/bash

###############################################################################
# Run Automated Tests Only
# Executes Maven tests and generates report with real results
###############################################################################

echo ""
echo "╔══════════════════════════════════════════════════════════════╗"
echo "║          Run Automated Tests & Generate Report               ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""

# Step 1: Run Maven tests
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Running Automated Tests (Maven)..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

./mvnw.cmd test

EXIT_CODE=$?

echo ""
if [ $EXIT_CODE -eq 0 ]; then
    echo "✓ All automated tests PASSED!"
else
    echo "⚠ Some tests may have failed (see report for details)"
fi

# Step 2: Parse results with Python
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Generating Excel Report..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Check Python
if command -v python3 >/dev/null 2>&1; then
    python3 parse_test_results.py
elif command -v python >/dev/null 2>&1; then
    python parse_test_results.py
else
    echo "✗ Python not found! Install: https://www.python.org/downloads/"
    exit 1
fi

echo ""
echo "╔══════════════════════════════════════════════════════════════╗"
echo "║                     COMPLETED!                               ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""
echo "📊 Excel Report: security_test_results.csv"
echo "📁 Surefire Reports: target/surefire-reports/"
echo ""
echo "✅ Open security_test_results.csv in Excel!"
echo ""

exit $EXIT_CODE
