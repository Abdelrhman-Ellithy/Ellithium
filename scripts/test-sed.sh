#!/bin/bash

# Test script to verify sed expressions work correctly

echo "Testing sed expressions..."

# Test method name extraction
echo "Testing method name extraction:"
echo "public void testMethod(String param) {" | sed 's/^.*[[:space:]]\([a-zA-Z_][a-zA-Z0-9_]*\)[[:space:]]*([^)]*).*$/\1/'

# Test class name extraction
echo "Testing class name extraction:"
echo "public class TestClass {" | sed 's/^.*[[:space:]]\(class\|interface\|enum\)[[:space:]]\+\([a-zA-Z_][a-zA-Z0-9_]*\).*$/\2/'

# Test field name extraction
echo "Testing field name extraction:"
echo "private String testField;" | sed 's/^.*[[:space:]]\+\([a-zA-Z_][a-zA-Z0-9_]*\)[[:space:]]*[=;].*$/\1/'

echo "Sed expressions test complete!"
