#!/bin/bash

# Ellithium Java Change Detector
# This script analyzes Java source code changes and detects new methods, classes, and API changes

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to extract method signatures from Java code
extract_methods() {
    local file="$1"
    local output_file="$2"
    
    if [ ! -f "$file" ]; then
        return 0
    fi
    
    print_status "Extracting methods from $file"
    
    # Extract method signatures with better regex patterns
    # This handles various method modifiers and complex signatures
    grep -E '^\s*(public|private|protected)?\s*(static\s+)?(final\s+)?(synchronized\s+)?(abstract\s+)?(native\s+)?(strictfp\s+)?[a-zA-Z_][a-zA-Z0-9_<>\[\]\s]*\s+[a-zA-Z_][a-zA-Z0-9_]*\s*\([^)]*\)\s*(throws\s+[^{]*)?\{?' "$file" | \
        sed 's/^\s*//' | \
        sed 's/\s*\{.*$//' | \
        sed 's/\s*$//' | \
        sed 's/\s\+/ /g' | \
        sort -u > "$output_file"
}

# Function to extract method signatures with full details for comparison
extract_methods_detailed() {
    local file="$1"
    local output_file="$2"
    
    if [ ! -f "$file" ]; then
        return 0
    fi
    
    print_status "Extracting detailed method signatures from $file"
    
    # Extract complete method signatures including all modifiers
    grep -E '^\s*(public|private|protected)?\s*(static\s+)?(final\s+)?(synchronized\s+)?(abstract\s+)?(native\s+)?(strictfp\s+)?[a-zA-Z_][a-zA-Z0-9_<>\[\]\s]*\s+[a-zA-Z_][a-zA-Z0-9_]*\s*\([^)]*\)\s*(throws\s+[^{]*)?\{?' "$file" | \
        sed 's/^\s*//' | \
        sed 's/\s*\{.*$//' | \
        sed 's/\s*$//' | \
        sed 's/\s\+/ /g' | \
        sort -u > "$output_file"
}

# Function to extract method names only (for finding modifications)
extract_method_names() {
    local file="$1"
    local output_file="$2"
    
    if [ ! -f "$file" ]; then
        return 0
    fi
    
    print_status "Extracting method names from $file"
    
    # Extract just method names for modification detection
    grep -E '^\s*(public|private|protected)?\s*(static\s+)?(final\s+)?(synchronized\s+)?(abstract\s+)?(native\s+)?(strictfp\s+)?[a-zA-Z_][a-zA-Z0-9_<>\[\]\s]*\s+[a-zA-Z_][a-zA-Z0-9_]*\s*\([^)]*\)\s*(throws\s+[^{]*)?\{?' "$file" | \
        sed 's/^.*[[:space:]]\([a-zA-Z_][a-zA-Z0-9_]*\)[[:space:]]*([^)]*).*$/\1/' | \
        sort -u > "$output_file"
}

# Function to extract class declarations
extract_classes() {
    local file="$1"
    local output_file="$2"
    
    if [ ! -f "$file" ]; then
        return 0
    fi
    
    print_status "Extracting classes from $file"
    
    # Extract class, interface, and enum declarations
    grep -E '^\s*(public\s+)?(abstract\s+)?(final\s+)?(class|interface|enum)\s+[a-zA-Z_][a-zA-Z0-9_]*' "$file" | \
        sed 's/^\s*//' | \
        sed 's/\s*\{.*$//' | \
        sed 's/\s*$//' | \
        sed 's/\s\+/ /g' | \
        sort -u > "$output_file"
}

# Function to extract detailed class declarations for modification detection
extract_classes_detailed() {
    local file="$1"
    local output_file="$2"
    
    if [ ! -f "$file" ]; then
        return 0
    fi
    
    print_status "Extracting detailed class declarations from $file"
    
    # Extract complete class declarations including inheritance and interfaces
    grep -E '^\s*(public\s+)?(abstract\s+)?(final\s+)?(class|interface|enum)\s+[a-zA-Z_][a-zA-Z0-9_]*(\s+extends\s+[^{]*)?(\s+implements\s+[^{]*)?' "$file" | \
        sed 's/^\s*//' | \
        sed 's/\s*\{.*$//' | \
        sed 's/\s*$//' | \
        sed 's/\s\+/ /g' | \
        sort -u > "$output_file"
}

# Function to extract class names only (for finding modifications)
extract_class_names() {
    local file="$1"
    local output_file="$2"
    
    if [ ! -f "$file" ]; then
        return 0
    fi
    
    print_status "Extracting class names from $file"
    
    # Extract just class names for modification detection
    grep -E '^\s*(public\s+)?(abstract\s+)?(final\s+)?(class|interface|enum)\s+[a-zA-Z_][a-zA-Z0-9_]*' "$file" | \
        sed 's/^.*[[:space:]]\(class\|interface\|enum\)[[:space:]]\+\([a-zA-Z_][a-zA-Z0-9_]*\).*$/\2/' | \
        sort -u > "$output_file"
}

# Function to extract field declarations
extract_fields() {
    local file="$1"
    local output_file="$2"
    
    if [ ! -f "$file" ]; then
        return 0
    fi
    
    print_status "Extracting fields from $file"
    
    # Extract field declarations
    grep -E '^\s*(public|private|protected)?\s*(static\s+)?(final\s+)?(volatile\s+)?(transient\s+)?[a-zA-Z_][a-zA-Z0-9_<>\[\]\s]*\s+[a-zA-Z_][a-zA-Z0-9_]*\s*[=;]' "$file" | \
        sed 's/^\s*//' | \
        sed 's/\s*[=;].*$//' | \
        sed 's/\s*$//' | \
        sed 's/\s\+/ /g' | \
        sort -u > "$output_file"
}

# Function to extract detailed field declarations for modification detection
extract_fields_detailed() {
    local file="$1"
    local output_file="$2"
    
    if [ ! -f "$file" ]; then
        return 0
    fi
    
    print_status "Extracting detailed field declarations from $file"
    
    # Extract complete field declarations including all modifiers
    grep -E '^\s*(public|private|protected)?\s*(static\s+)?(final\s+)?(volatile\s+)?(transient\s+)?[a-zA-Z_][a-zA-Z0-9_<>\[\]\s]*\s+[a-zA-Z_][a-zA-Z0-9_]*\s*[=;]' "$file" | \
        sed 's/^\s*//' | \
        sed 's/\s*[=;].*$//' | \
        sed 's/\s*$//' | \
        sed 's/\s\+/ /g' | \
        sort -u > "$output_file"
}

# Function to extract field names only (for finding modifications)
extract_field_names() {
    local file="$1"
    local output_file="$2"
    
    if [ ! -f "$file" ]; then
        return 0
    fi
    
    print_status "Extracting field names from $file"
    
    # Extract just field names for modification detection
    grep -E '^\s*(public|private|protected)?\s*(static\s+)?(final\s+)?(volatile\s+)?(transient\s+)?[a-zA-Z_][a-zA-Z0-9_<>\[\]\s]*\s+[a-zA-Z_][a-zA-Z0-9_]*\s*[=;]' "$file" | \
        sed 's/^.*[[:space:]]\+\([a-zA-Z_][a-zA-Z0-9_]*\)[[:space:]]*[=;].*$/\1/' | \
        sort -u > "$output_file"
}

# Function to detect modified methods (same name, different signature)
detect_modified_methods() {
    local base_dir="$1"
    local head_dir="$2"
    local output_file="$3"
    
    print_status "Detecting modified methods..."
    
    # Find methods that exist in both but have different signatures
    comm -12 "$base_dir/all_method_names.txt" "$head_dir/all_method_names.txt" | \
        while read -r method_name; do
            # Get full signatures for this method name from both commits
            grep -E ".*\s+$method_name\s*\(" "$base_dir/all_methods_detailed.txt" > /tmp/base_method_sig.txt
            grep -E ".*\s+$method_name\s*\(" "$head_dir/all_methods_detailed.txt" > /tmp/head_method_sig.txt
            
            # Compare signatures
            if ! diff -q /tmp/base_method_sig.txt /tmp/head_method_sig.txt > /dev/null 2>&1; then
                echo "MODIFIED: $method_name" >> "$output_file"
                echo "  Base: $(cat /tmp/base_method_sig.txt)" >> "$output_file"
                echo "  Head: $(cat /tmp/head_method_sig.txt)" >> "$output_file"
                echo "" >> "$output_file"
            fi
        done
}

# Function to detect modified classes (same name, different structure)
detect_modified_classes() {
    local base_dir="$1"
    local head_dir="$2"
    local output_file="$3"
    
    print_status "Detecting modified classes..."
    
    # Find classes that exist in both but have different declarations
    comm -12 "$base_dir/all_class_names.txt" "$head_dir/all_class_names.txt" | \
        while read -r class_name; do
            # Get full declarations for this class name from both commits
            grep -E ".*\s+$class_name(\s+extends|\s+implements|\s*\{|\s*$)" "$base_dir/all_classes_detailed.txt" > /tmp/base_class_decl.txt
            grep -E ".*\s+$class_name(\s+extends|\s+implements|\s*\{|\s*$)" "$head_dir/all_classes_detailed.txt" > /tmp/head_class_decl.txt
            
            # Compare declarations
            if ! diff -q /tmp/base_class_decl.txt /tmp/head_class_decl.txt > /dev/null 2>&1; then
                echo "MODIFIED: $class_name" >> "$output_file"
                echo "  Base: $(cat /tmp/base_class_decl.txt)" >> "$output_file"
                echo "  Head: $(cat /tmp/head_class_decl.txt)" >> "$output_file"
                echo "" >> "$output_file"
            fi
        done
}

# Function to detect modified fields (same name, different declaration)
detect_modified_fields() {
    local base_dir="$1"
    local head_dir="$2"
    local output_file="$3"
    
    print_status "Detecting modified fields..."
    
    # Find fields that exist in both but have different declarations
    comm -12 "$base_dir/all_field_names.txt" "$head_dir/all_field_names.txt" | \
        while read -r field_name; do
            # Get full declarations for this field name from both commits
            grep -E ".*\s+$field_name\s*[=;]" "$base_dir/all_fields_detailed.txt" > /tmp/base_field_decl.txt
            grep -E ".*\s+$field_name\s*[=;]" "$head_dir/all_fields_detailed.txt" > /tmp/head_field_decl.txt
            
            # Compare declarations
            if ! diff -q /tmp/base_field_decl.txt /tmp/head_field_decl.txt > /dev/null 2>&1; then
                echo "MODIFIED: $field_name" >> "$output_file"
                echo "  Base: $(cat /tmp/base_field_decl.txt)" >> "$output_file"
                echo "  Head: $(cat /tmp/head_field_decl.txt)" >> "$output_file"
                echo "" >> "$output_file"
            fi
        done
}

# Function to analyze package structure
analyze_packages() {
    local base_dir="$1"
    local output_file="$2"
    
    if [ ! -d "$base_dir" ]; then
        return 0
    fi
    
    print_status "Analyzing package structure in $base_dir"
    
    # Find all Java files and extract package information
    find "$base_dir" -name "*.java" -type f | \
        while read -r file; do
            # Extract package declaration
            package=$(grep -E '^package\s+[^;]+;' "$file" | head -1 | sed 's/^package\s*//' | sed 's/;$//')
            if [ -n "$package" ]; then
                echo "$package" >> "$output_file"
            fi
        done
    
    # Remove duplicates and sort
    if [ -f "$output_file" ]; then
        sort -u "$output_file" -o "$output_file"
    fi
}

# Main analysis function
analyze_changes() {
    local base_sha="$1"
    local head_sha="$2"
    local output_dir="$3"
    
    print_status "Starting Java change analysis..."
    print_status "Base SHA: $base_sha"
    print_status "Head SHA: $head_sha"
    
    # Create output directories
    mkdir -p "$output_dir"/{base,head,analysis}
    
    # Extract Java files from both commits
    print_status "Extracting Java source files..."
    
    # Get list of Java files in both commits (MAIN SOURCE ONLY, NO TEST FILES)
    print_status "Extracting main source Java files (excluding test files)..."
    git show --name-only "$base_sha" | grep 'src/main/java.*\.java$' > "$output_dir/base/java_files.txt" 2>/dev/null || true
    git show --name-only "$head_sha" | grep 'src/main/java.*\.java$' > "$output_dir/head/java_files.txt" 2>/dev/null || true
    
    # Debug: Show what files we found
    print_status "Base commit Java files found:"
    if [ -f "$output_dir/base/java_files.txt" ]; then
        cat "$output_dir/base/java_files.txt" | head -10
    fi
    
    print_status "Head commit Java files found:"
    if [ -f "$output_dir/head/java_files.txt" ]; then
        cat "$output_dir/head/java_files.txt" | head -10
    fi
    
    # Extract content for each Java file
    while IFS= read -r file; do
        if [ -n "$file" ]; then
            git show "$base_sha:$file" > "$output_dir/base/$(basename "$file")" 2>/dev/null || true
        fi
    done < "$output_dir/base/java_files.txt"
    
    while IFS= read -r file; do
        if [ -n "$file" ]; then
            git show "$head_sha:$file" > "$output_dir/head/$(basename "$file")" 2>/dev/null || true
        fi
    done < "$output_dir/head/java_files.txt"
    
    # Analyze each Java file
    print_status "Analyzing Java files for changes..."
    
    # Process base files
    for file in "$output_dir"/base/*.java; do
        if [ -f "$file" ]; then
            basename_file=$(basename "$file")
            extract_methods "$file" "$output_dir/base/${basename_file}_methods.txt"
            extract_methods_detailed "$file" "$output_dir/base/${basename_file}_methods_detailed.txt"
            extract_method_names "$file" "$output_dir/base/${basename_file}_method_names.txt"
            extract_classes "$file" "$output_dir/base/${basename_file}_classes.txt"
            extract_classes_detailed "$file" "$output_dir/base/${basename_file}_classes_detailed.txt"
            extract_class_names "$file" "$output_dir/base/${basename_file}_class_names.txt"
            extract_fields "$file" "$output_dir/base/${basename_file}_fields.txt"
            extract_fields_detailed "$file" "$output_dir/base/${basename_file}_fields_detailed.txt"
            extract_field_names "$file" "$output_dir/base/${basename_file}_field_names.txt"
        fi
    done
    
    # Process head files
    for file in "$output_dir"/head/*.java; do
        if [ -f "$file" ]; then
            basename_file=$(basename "$file")
            extract_methods "$file" "$output_dir/head/${basename_file}_methods.txt"
            extract_methods_detailed "$file" "$output_dir/head/${basename_file}_methods_detailed.txt"
            extract_method_names "$file" "$output_dir/head/${basename_file}_method_names.txt"
            extract_classes "$file" "$output_dir/head/${basename_file}_classes.txt"
            extract_classes_detailed "$file" "$output_dir/head/${basename_file}_classes_detailed.txt"
            extract_class_names "$file" "$output_dir/head/${basename_file}_class_names.txt"
            extract_fields "$file" "$output_dir/head/${basename_file}_fields.txt"
            extract_fields_detailed "$file" "$output_dir/head/${basename_file}_fields_detailed.txt"
            extract_field_names "$file" "$output_dir/head/${basename_file}_field_names.txt"
        fi
    done
    
    # Combine all extracted data
    cat "$output_dir"/base/*_methods.txt 2>/dev/null | sort -u > "$output_dir/base/all_methods.txt" || true
    cat "$output_dir"/head/*_methods.txt 2>/dev/null | sort -u > "$output_dir/head/all_methods.txt" || true
    cat "$output_dir"/base/*_methods_detailed.txt 2>/dev/null | sort -u > "$output_dir/base/all_methods_detailed.txt" || true
    cat "$output_dir"/head/*_methods_detailed.txt 2>/dev/null | sort -u > "$output_dir/head/all_methods_detailed.txt" || true
    cat "$output_dir"/base/*_method_names.txt 2>/dev/null | sort -u > "$output_dir/base/all_method_names.txt" || true
    cat "$output_dir"/head/*_method_names.txt 2>/dev/null | sort -u > "$output_dir/head/all_method_names.txt" || true
    
    cat "$output_dir"/base/*_classes.txt 2>/dev/null | sort -u > "$output_dir/base/all_classes.txt" || true
    cat "$output_dir"/head/*_classes.txt 2>/dev/null | sort -u > "$output_dir/head/all_classes.txt" || true
    cat "$output_dir"/base/*_classes_detailed.txt 2>/dev/null | sort -u > "$output_dir/base/all_classes_detailed.txt" || true
    cat "$output_dir"/head/*_classes_detailed.txt 2>/dev/null | sort -u > "$output_dir/head/all_classes_detailed.txt" || true
    cat "$output_dir"/base/*_class_names.txt 2>/dev/null | sort -u > "$output_dir/base/all_class_names.txt" || true
    cat "$output_dir"/head/*_class_names.txt 2>/dev/null | sort -u > "$output_dir/head/all_class_names.txt" || true
    
    cat "$output_dir"/base/*_fields.txt 2>/dev/null | sort -u > "$output_dir/base/all_fields.txt" || true
    cat "$output_dir"/head/*_fields.txt 2>/dev/null | sort -u > "$output_dir/head/all_fields.txt" || true
    cat "$output_dir"/base/*_fields_detailed.txt 2>/dev/null | sort -u > "$output_dir/base/all_fields_detailed.txt" || true
    cat "$output_dir"/head/*_fields_detailed.txt 2>/dev/null | sort -u > "$output_dir/head/all_fields_detailed.txt" || true
    cat "$output_dir"/base/*_field_names.txt 2>/dev/null | sort -u > "$output_dir/base/all_field_names.txt" || true
    cat "$output_dir"/head/*_field_names.txt 2>/dev/null | sort -u > "$output_dir/head/all_field_names.txt" || true
    
    # Find differences
    print_status "Calculating differences..."
    
    # New methods
    comm -23 "$output_dir/head/all_methods.txt" "$output_dir/base/all_methods.txt" > "$output_dir/analysis/new_methods.txt" 2>/dev/null || true
    NEW_METHODS_COUNT=$(wc -l < "$output_dir/analysis/new_methods.txt" 2>/dev/null || echo "0")
    
    # New classes
    comm -23 "$output_dir/head/all_classes.txt" "$output_dir/base/all_classes.txt" > "$output_dir/analysis/new_classes.txt" 2>/dev/null || true
    NEW_CLASSES_COUNT=$(wc -l < "$output_dir/analysis/new_classes.txt" 2>/dev/null || echo "0")
    
    # New fields
    comm -23 "$output_dir/head/all_fields.txt" "$output_dir/base/all_fields.txt" > "$output_dir/analysis/new_fields.txt" 2>/dev/null || true
    NEW_FIELDS_COUNT=$(wc -l < "$output_dir/analysis/new_fields.txt" 2>/dev/null || echo "0")
    
    # Removed methods
    comm -13 "$output_dir/head/all_methods.txt" "$output_dir/base/all_methods.txt" > "$output_dir/analysis/removed_methods.txt" 2>/dev/null || true
    REMOVED_METHODS_COUNT=$(wc -l < "$output_dir/analysis/removed_methods.txt" 2>/dev/null || echo "0")
    
    # Removed classes
    comm -13 "$output_dir/head/all_classes.txt" "$output_dir/base/all_classes.txt" > "$output_dir/analysis/removed_classes.txt" 2>/dev/null || true
    REMOVED_CLASSES_COUNT=$(wc -l < "$output_dir/analysis/removed_classes.txt" 2>/dev/null || echo "0")
    
    # Removed fields
    comm -13 "$output_dir/head/all_fields.txt" "$output_dir/base/all_fields.txt" > "$output_dir/analysis/removed_fields.txt" 2>/dev/null || true
    REMOVED_FIELDS_COUNT=$(wc -l < "$output_dir/analysis/removed_fields.txt" 2>/dev/null || echo "0")
    
    # Detect modifications (same name, different signature/structure)
    print_status "Detecting modifications..."
    
    # Modified methods
    detect_modified_methods "$output_dir/base" "$output_dir/head" "$output_dir/analysis/modified_methods.txt"
    MODIFIED_METHODS_COUNT=$(grep -c "^MODIFIED:" "$output_dir/analysis/modified_methods.txt" 2>/dev/null || echo "0")
    
    # Modified classes
    detect_modified_classes "$output_dir/base" "$output_dir/head" "$output_dir/analysis/modified_classes.txt"
    MODIFIED_CLASSES_COUNT=$(grep -c "^MODIFIED:" "$output_dir/analysis/modified_classes.txt" 2>/dev/null || echo "0")
    
    # Modified fields
    detect_modified_fields "$output_dir/base" "$output_dir/head" "$output_dir/analysis/modified_fields.txt"
    MODIFIED_FIELDS_COUNT=$(grep -c "^MODIFIED:" "$output_dir/analysis/modified_fields.txt" 2>/dev/null || echo "0")
    
    # Get file change statistics
    FILES_MODIFIED=$(git diff --name-only "$base_sha" "$head_sha" | grep '\.java$' | wc -l)
    LINES_ADDED=$(git diff --stat "$base_sha" "$head_sha" | tail -1 | grep -o '[0-9]\+ insertion' | grep -o '[0-9]\+' || echo "0")
    LINES_REMOVED=$(git diff --stat "$base_sha" "$head_sha" | tail -1 | grep -o '[0-9]\+ deletion' | grep -o '[0-9]\+' || echo "0")
    
    # Generate detailed report
    print_status "Generating change report..."
    
    cat > "$output_dir/analysis/change_report.md" << EOF
# 🔍 Ellithium Java Change Analysis Report

## 📊 Summary Statistics
- **New Methods:** $NEW_METHODS_COUNT
- **New Classes:** $NEW_CLASSES_COUNT
- **New Fields:** $NEW_FIELDS_COUNT
- **Removed Methods:** $REMOVED_METHODS_COUNT
- **Removed Classes:** $REMOVED_CLASSES_COUNT
- **Removed Fields:** $REMOVED_FIELDS_COUNT
- **Modified Methods:** $MODIFIED_METHODS_COUNT
- **Modified Classes:** $MODIFIED_CLASSES_COUNT
- **Modified Fields:** $MODIFIED_FIELDS_COUNT
- **Files Modified:** $FILES_MODIFIED
- **Lines Added:** $LINES_ADDED
- **Lines Removed:** $LINES_REMOVED

## 🆕 New Methods
EOF
    
    if [ "$NEW_METHODS_COUNT" -gt 0 ]; then
        echo "Found $NEW_METHODS_COUNT new method(s):" >> "$output_dir/analysis/change_report.md"
        echo "" >> "$output_dir/analysis/change_report.md"
        while IFS= read -r method; do
            echo "- \`$method\`" >> "$output_dir/analysis/change_report.md"
        done < "$output_dir/analysis/new_methods.txt"
    else
        echo "No new methods detected." >> "$output_dir/analysis/change_report.md"
    fi
    
    cat >> "$output_dir/analysis/change_report.md" << EOF

## 🏗️ New Classes
EOF
    
    if [ "$NEW_CLASSES_COUNT" -gt 0 ]; then
        echo "Found $NEW_CLASSES_COUNT new class(es):" >> "$output_dir/analysis/change_report.md"
        echo "" >> "$output_dir/analysis/change_report.md"
        while IFS= read -r class; do
            echo "- \`$class\`" >> "$output_dir/analysis/change_report.md"
        done < "$output_dir/analysis/new_classes.txt"
    else
        echo "No new classes detected." >> "$output_dir/analysis/change_report.md"
    fi
    
    cat >> "$output_dir/analysis/change_report.md" << EOF

## 🔧 New Fields
EOF
    
    if [ "$NEW_FIELDS_COUNT" -gt 0 ]; then
        echo "Found $NEW_FIELDS_COUNT new field(s):" >> "$output_dir/analysis/change_report.md"
        echo "" >> "$output_dir/analysis/change_report.md"
        while IFS= read -r field; do
            echo "- \`$field\`" >> "$output_dir/analysis/change_report.md"
        done < "$output_dir/analysis/new_fields.txt"
    else
        echo "No new fields detected." >> "$output_dir/analysis/change_report.md"
    fi
    
    if [ "$REMOVED_METHODS_COUNT" -gt 0 ] || [ "$REMOVED_CLASSES_COUNT" -gt 0 ] || [ "$REMOVED_FIELDS_COUNT" -gt 0 ]; then
        cat >> "$output_dir/analysis/change_report.md" << EOF

## 🗑️ Removed Elements
EOF
        
        if [ "$REMOVED_METHODS_COUNT" -gt 0 ]; then
            echo "- **Removed Methods:** $REMOVED_METHODS_COUNT" >> "$output_dir/analysis/change_report.md"
        fi
        if [ "$REMOVED_CLASSES_COUNT" -gt 0 ]; then
            echo "- **Removed Classes:** $REMOVED_CLASSES_COUNT" >> "$output_dir/analysis/change_report.md"
        fi
        if [ "$REMOVED_FIELDS_COUNT" -gt 0 ]; then
            echo "- **Removed Fields:** $REMOVED_FIELDS_COUNT" >> "$output_dir/analysis/change_report.md"
        fi
    fi
    
    # Add modification sections
    if [ "$MODIFIED_METHODS_COUNT" -gt 0 ] || [ "$MODIFIED_CLASSES_COUNT" -gt 0 ] || [ "$MODIFIED_FIELDS_COUNT" -gt 0 ]; then
        cat >> "$output_dir/analysis/change_report.md" << EOF

## 🔄 Modified Elements
EOF
        
        if [ "$MODIFIED_METHODS_COUNT" -gt 0 ]; then
            echo "- **Modified Methods:** $MODIFIED_METHODS_COUNT" >> "$output_dir/analysis/change_report.md"
        fi
        if [ "$MODIFIED_CLASSES_COUNT" -gt 0 ]; then
            echo "- **Modified Classes:** $MODIFIED_CLASSES_COUNT" >> "$output_dir/analysis/change_report.md"
        fi
        if [ "$MODIFIED_FIELDS_COUNT" -gt 0 ]; then
            echo "- **Modified Fields:** $MODIFIED_FIELDS_COUNT" >> "$output_dir/analysis/change_report.md"
        fi
    fi
    
    cat >> "$output_dir/analysis/change_report.md" << EOF

## 📁 Modified Files
EOF
    
    git diff --name-only "$base_sha" "$head_sha" | grep '\.java$' | while read -r file; do
        echo "- \`$file\`" >> "$output_dir/analysis/change_report.md"
    done
    
    # Add detailed modification information
    if [ "$MODIFIED_METHODS_COUNT" -gt 0 ]; then
        cat >> "$output_dir/analysis/change_report.md" << EOF

## 🔄 Modified Method Details
EOF
        cat "$output_dir/analysis/modified_methods.txt" >> "$output_dir/analysis/change_report.md" 2>/dev/null || true
    fi
    
    if [ "$MODIFIED_CLASSES_COUNT" -gt 0 ]; then
        cat >> "$output_dir/analysis/change_report.md" << EOF

## 🔄 Modified Class Details
EOF
        cat "$output_dir/analysis/modified_classes.txt" >> "$output_dir/analysis/change_report.md" 2>/dev/null || true
    fi
    
    if [ "$MODIFIED_FIELDS_COUNT" -gt 0 ]; then
        cat >> "$output_dir/analysis/change_report.md" << EOF

## 🔄 Modified Field Details
EOF
        cat "$output_dir/analysis/modified_fields.txt" >> "$output_dir/analysis/change_report.md" 2>/dev/null || true
    fi
    
    # Set output variables for GitHub Actions
    echo "new_methods_count=$NEW_METHODS_COUNT" >> "$GITHUB_OUTPUT"
    echo "new_classes_count=$NEW_CLASSES_COUNT" >> "$GITHUB_OUTPUT"
    echo "new_fields_count=$NEW_FIELDS_COUNT" >> "$GITHUB_OUTPUT"
    echo "removed_methods_count=$REMOVED_METHODS_COUNT" >> "$GITHUB_OUTPUT"
    echo "removed_classes_count=$REMOVED_CLASSES_COUNT" >> "$GITHUB_OUTPUT"
    echo "removed_fields_count=$REMOVED_FIELDS_COUNT" >> "$GITHUB_OUTPUT"
    echo "modified_methods_count=$MODIFIED_METHODS_COUNT" >> "$GITHUB_OUTPUT"
    echo "modified_classes_count=$MODIFIED_CLASSES_COUNT" >> "$GITHUB_OUTPUT"
    echo "modified_fields_count=$MODIFIED_FIELDS_COUNT" >> "$GITHUB_OUTPUT"
    echo "files_modified=$FILES_MODIFIED" >> "$GITHUB_OUTPUT"
    echo "lines_added=$LINES_ADDED" >> "$GITHUB_OUTPUT"
    echo "lines_removed=$LINES_REMOVED" >> "$GITHUB_OUTPUT"
    
    # Save the report content
    echo "change_report<<EOF" >> "$GITHUB_OUTPUT"
    cat "$output_dir/analysis/change_report.md" >> "$GITHUB_OUTPUT"
    echo "EOF" >> "$GITHUB_OUTPUT"
    
    print_success "Analysis complete!"
    print_status "📊 New Methods: $NEW_METHODS_COUNT"
    print_status "🏗️ New Classes: $NEW_CLASSES_COUNT"
    print_status "🔧 New Fields: $NEW_FIELDS_COUNT"
    print_status "🗑️ Removed Methods: $REMOVED_METHODS_COUNT"
    print_status "🗑️ Removed Classes: $REMOVED_CLASSES_COUNT"
    print_status "🗑️ Removed Fields: $REMOVED_FIELDS_COUNT"
    print_status "🔄 Modified Methods: $MODIFIED_METHODS_COUNT"
    print_status "🔄 Modified Classes: $MODIFIED_CLASSES_COUNT"
    print_status "🔄 Modified Fields: $MODIFIED_FIELDS_COUNT"
    print_status "📁 Files Modified: $FILES_MODIFIED"
}

# Main execution
if [ "$#" -ne 3 ]; then
    print_error "Usage: $0 <base_sha> <head_sha> <output_dir>"
    exit 1
fi

BASE_SHA="$1"
HEAD_SHA="$2"
OUTPUT_DIR="$3"

# Validate inputs
if [ -z "$BASE_SHA" ] || [ -z "$HEAD_SHA" ] || [ -z "$OUTPUT_DIR" ]; then
    print_error "All parameters are required"
    exit 1
fi

# Check if we're in a git repository
if ! git rev-parse --git-dir > /dev/null 2>&1; then
    print_error "Not in a git repository"
    exit 1
fi

# Validate commit SHAs
if ! git rev-parse --verify "$BASE_SHA" > /dev/null 2>&1; then
    print_error "Invalid base SHA: $BASE_SHA"
    exit 1
fi

if ! git rev-parse --verify "$HEAD_SHA" > /dev/null 2>&1; then
    print_error "Invalid head SHA: $HEAD_SHA"
    exit 1
fi

# Run analysis
analyze_changes "$BASE_SHA" "$HEAD_SHA" "$OUTPUT_DIR"
