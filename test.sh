#!/bin/bash

# Script to run all tests with detailed report
# Usage: ./test.sh

echo "🧪 Kafka Metamorphosis - Test Suite"
echo "=================================="
echo ""

# Check if Leiningen is available
if ! command -v lein &> /dev/null; then
    echo "❌ Leiningen (lein) is not installed or not in PATH"
    echo "Please install Leiningen first: https://leiningen.org/"
    exit 1
fi

echo "📋 Running compilation check..."
if ! lein check; then
    echo "❌ Compilation failed!"
    exit 1
fi
echo "✅ Compilation successful"
echo ""

echo "🧪 Running all unit tests..."
echo ""

# Run tests with detailed output
lein test 2>&1 | tee test-results.log

# Check test results
if [ ${PIPESTATUS[0]} -eq 0 ]; then
    echo ""
    echo "🎉 All tests passed!"
    
    # Count tests and assertions from output
    TESTS=$(grep -o "Ran [0-9]* tests" test-results.log | grep -o "[0-9]*" || echo "0")
    ASSERTIONS=$(grep -o "containing [0-9]* assertions" test-results.log | grep -o "[0-9]*" || echo "0")
    FAILURES=$(grep -o "[0-9]* failures" test-results.log | grep -o "^[0-9]*" || echo "0")
    ERRORS=$(grep -o "[0-9]* errors" test-results.log | grep -o "^[0-9]*" || echo "0")
    
    echo ""
    echo "📊 Test Summary:"
    echo "   Tests: $TESTS"
    echo "   Assertions: $ASSERTIONS" 
    echo "   Failures: $FAILURES"
    echo "   Errors: $ERRORS"
    echo ""
    
    # Test Coverage by namespace
    echo "📋 Test Coverage by Namespace:"
    echo "   ✅ kafka-metamorphosis.core-test"
    echo "   ✅ kafka-metamorphosis.producer-test" 
    echo "   ✅ kafka-metamorphosis.admin-test"
    echo "   ✅ kafka-metamorphosis.serializers-test"
    echo "   ✅ kafka-metamorphosis.dev-test"
    echo ""
    
    echo "🚀 All systems ready for deployment!"
    
else
    echo ""
    echo "❌ Some tests failed!"
    echo ""
    echo "📋 Failed test details:"
    grep -A 5 -B 5 "FAIL\|ERROR" test-results.log || echo "Check test-results.log for details"
    echo ""
    echo "🔧 Please fix the failing tests before deployment"
    exit 1
fi

# Clean up
rm -f test-results.log

echo ""
echo "✅ Test suite completed successfully!"