#!/bin/bash

# Ellithium Java Change Detector - Rewritten with Best Practices
# This script analyzes Java source code changes between two Git commits

set -euo pipefail  # Strict error handling

# Function to print clean output (no colors for GitHub Actions compatibility)
print_status() {
    echo "[INFO] $1"
}

print_success() {
    echo "[SUCCESS] $1"
}

print_warning() {
    echo "[WARNING] $1"
}

print_error() {
    echo "[ERROR] $1" >&2
}

# Function to safely extract Java elements using grep and awk
extract_java_elements() {
    local file="$1"
    local output_dir="$2"
    local basename_file="$3"
    
    # Validate file exists and is readable
    if [[ ! -f "$file" ]] || [[ ! -r "$file" ]]; then
        print_warning "File not accessible: $file"
        return 0
    fi
    
    # Check if file is empty
    if [[ ! -s "$file" ]]; then
        print_warning "File is empty: $file"
        return 0
    fi
    
    print_status "Processing $basename_file"
    
    # Extract methods using robust pattern matching
    awk '
    /^[[:space:]]*(public|private|protected|static|final|synchronized|abstract|native|strictfp)?[[:space:]]*[a-zA-Z_][a-zA-Z0-9_<>\[\]\s]*[[:space:]]+[a-zA-Z_][a-zA-Z0-9_]*[[:space:]]*\([^)]*\)[[:space:]]*(throws[[:space:]]+[^{]*)?[[:space:]]*\{?[[:space:]]*$/ {
        # Clean up the method signature
        gsub(/^[[:space:]]+/, "")  # Remove leading spaces
        gsub(/[[:space:]]*\{.*$/, "")  # Remove everything after {
        gsub(/[[:space:]]+/, " ")  # Normalize spaces
        print
    }' "$file" 2>/dev/null | sort -u > "$output_dir/${basename_file}_methods.txt" 2>/dev/null || touch "$output_dir/${basename_file}_methods.txt"
    
    # Extract method names only
    awk '
    /^[[:space:]]*(public|private|protected|static|final|synchronized|abstract|native|strictfp)?[[:space:]]*[a-zA-Z_][a-zA-Z0-9_<>\[\]\s]*[[:space:]]+[a-zA-Z_][a-zA-Z0-9_]*[[:space:]]*\([^)]*\)[[:space:]]*(throws[[:space:]]+[^{]*)?[[:space:]]*\{?[[:space:]]*$/ {
        # Extract just the method name
        match($0, /[[:space:]]+([a-zA-Z_][a-zA-Z0-9_]*)[[:space:]]*\(/)
        if (RSTART > 0) {
            print substr($0, RSTART + RLENGTH - 1, RLENGTH - 1)
        }
    }' "$file" | sort -u > "$output_dir/${basename_file}_method_names.txt" 2>/dev/null || true
    
    # Extract classes
    awk '
    /^[[:space:]]*(public|private|protected|abstract|final)?[[:space:]]*class[[:space:]]+[a-zA-Z_][a-zA-Z0-9_]*/ {
        gsub(/^[[:space:]]+/, "")
        gsub(/[[:space:]]*\{.*$/, "")
        gsub(/[[:space:]]+/, " ")
        print
    }' "$file" | sort -u > "$output_dir/${basename_file}_classes.txt" 2>/dev/null || true
    
    # Extract class names only
    awk '
    /^[[:space:]]*(public|private|protected|abstract|final)?[[:space:]]*class[[:space:]]+[a-zA-Z_][a-zA-Z0-9_]*/ {
        match($0, /class[[:space:]]+([a-zA-Z_][a-zA-Z0-9_]*)/)
        if (RSTART > 0) {
            print substr($0, RSTART + 6, RLENGTH - 6)
        }
    }' "$file" | sort -u > "$output_dir/${basename_file}_class_names.txt" 2>/dev/null || true
    
    # Extract fields
    awk '
    /^[[:space:]]*(public|private|protected|static|final|volatile|transient)?[[:space:]]*[a-zA-Z_][a-zA-Z0-9_<>\[\]\s]*[[:space:]]+[a-zA-Z_][a-zA-Z0-9_]*[[:space:]]*[=;]/ {
        gsub(/^[[:space:]]+/, "")
        gsub(/[[:space:]]*[=;].*$/, "")
        gsub(/[[:space:]]+/, " ")
        print
    }' "$file" | sort -u > "$output_dir/${basename_file}_fields.txt" 2>/dev/null || true
    
    # Extract field names only
    awk '
    /^[[:space:]]*(public|private|protected|static|final|volatile|transient)?[[:space:]]*[a-zA-Z_][a-zA-Z0-9_<>\[\]\s]*[[:space:]]+[a-zA-Z_][a-zA-Z0-9_]*[[:space:]]*[=;]/ {
        match($0, /[[:space:]]+([a-zA-Z_][a-zA-Z0-9_]*)[[:space:]]*[=;]/)
        if (RSTART > 0) {
            print substr($0, RSTART + 1, RLENGTH - 1)
        }
    }' "$file" | sort -u > "$output_dir/${basename_file}_field_names.txt" 2>/dev/null || true
}

# Function to safely count lines in a file
safe_line_count() {
    local file="$1"
    if [[ -f "$file" ]] && [[ -r "$file" ]]; then
        wc -l < "$file" 2>/dev/null || echo "0"
    else
        echo "0"
    fi
}

# Function to safely read file content
safe_read_file() {
    local file="$1"
    if [[ -f "$file" ]] && [[ -r "$file" ]]; then
        cat "$file" 2>/dev/null || echo ""
    else
        echo ""
    fi
}

# Function to safely combine files
safe_combine_files() {
    local pattern="$1"
    local output_file="$2"
    
    # Extract directory and filename from pattern
    local dir_path=$(dirname "$pattern")
    local file_pattern=$(basename "$pattern")
    
    # Use find with proper path handling
    if [[ -d "$dir_path" ]]; then
        find "$dir_path" -maxdepth 1 -name "$file_pattern" -type f -exec cat {} + 2>/dev/null | sort -u > "$output_file" 2>/dev/null || true
    else
        touch "$output_file"
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
    
    # Handle case where base_sha might be a tag name
    local base_commit
    if [[ "$base_sha" == v* ]]; then
        base_commit=$(git rev-parse "$base_sha" 2>/dev/null || echo "$base_sha")
        print_status "Base SHA is a tag, resolved to commit: $base_commit"
    else
        base_commit="$base_sha"
    fi
    
    # Validate commits exist
    if ! git rev-parse --verify "$base_commit" >/dev/null 2>&1; then
        print_error "Invalid base commit: $base_commit"
        exit 1
    fi
    
    if ! git rev-parse --verify "$head_sha" >/dev/null 2>&1; then
        print_error "Invalid head commit: $head_sha"
        exit 1
    fi
    
    # Get ALL Java files that exist in each commit (not just changed files)
    print_status "Extracting main source Java files (excluding test files)..."
    
    git ls-tree -r --name-only "$base_commit" 2>/dev/null | grep 'src/main/java.*\.java$' > "$output_dir/base/java_files.txt" 2>/dev/null || true
    git ls-tree -r --name-only "$head_sha" 2>/dev/null | grep 'src/main/java.*\.java$' > "$output_dir/head/java_files.txt" 2>/dev/null || true
    
    # Debug: Show what files we found
    print_status "Base commit Java files found:"
    if [[ -f "$output_dir/base/java_files.txt" ]]; then
        head -10 "$output_dir/base/java_files.txt" 2>/dev/null || true
    fi
    
    print_status "Head commit Java files found:"
    if [[ -f "$output_dir/head/java_files.txt" ]]; then
        head -10 "$output_dir/head/java_files.txt" 2>/dev/null || true
    fi
    
    # Extract content for each Java file
    while IFS= read -r file; do
        if [[ -n "$file" ]]; then
            local output_file="$output_dir/base/$(basename "$file")"
            if git show "$base_commit:$file" > "$output_file" 2>/dev/null; then
                # Validate the extracted file is readable
                if [[ -r "$output_file" ]] && [[ -s "$output_file" ]]; then
                    print_status "Successfully extracted: $file"
                else
                    print_warning "Extracted file is not readable or empty: $output_file"
                    rm -f "$output_file" 2>/dev/null || true
                fi
            else
                print_warning "Failed to extract: $file"
            fi
        fi
    done < "$output_dir/base/java_files.txt"
    
    while IFS= read -r file; do
        if [[ -n "$file" ]]; then
            local output_file="$output_dir/head/$(basename "$file")"
            if git show "$head_sha:$file" > "$output_file" 2>/dev/null; then
                # Validate the extracted file is readable
                if [[ -r "$output_file" ]] && [[ -s "$output_file" ]]; then
                    print_status "Successfully extracted: $file"
                else
                    print_warning "Extracted file is not readable or empty: $output_file"
                    rm -f "$output_file" 2>/dev/null || true
                fi
            else
                print_warning "Failed to extract: $file"
            fi
        fi
    done < "$output_dir/head/java_files.txt"
    
    # Analyze each Java file
    print_status "Analyzing Java files for changes..."
    
    # Process base files
    for file in "$output_dir"/base/*.java; do
        if [[ -f "$file" ]] && [[ -r "$file" ]]; then
            basename_file=$(basename "$file")
            print_status "Processing base file: $basename_file"
            extract_java_elements "$file" "$output_dir/base" "$basename_file"
        fi
    done
    
    # Process head files
    for file in "$output_dir"/head/*.java; do
        if [[ -f "$file" ]] && [[ -r "$file" ]]; then
            basename_file=$(basename "$file")
            print_status "Processing head file: $basename_file"
            extract_java_elements "$file" "$output_dir/head" "$basename_file"
        fi
    done
    
    # Combine all extracted data safely
    print_status "Combining extracted data..."
    
    safe_combine_files "$output_dir/base/*_methods.txt" "$output_dir/base/all_methods.txt"
    safe_combine_files "$output_dir/head/*_methods.txt" "$output_dir/head/all_methods.txt"
    safe_combine_files "$output_dir/base/*_method_names.txt" "$output_dir/base/all_method_names.txt"
    safe_combine_files "$output_dir/head/*_method_names.txt" "$output_dir/head/all_method_names.txt"
    
    safe_combine_files "$output_dir/base/*_classes.txt" "$output_dir/base/all_classes.txt"
    safe_combine_files "$output_dir/head/*_classes.txt" "$output_dir/head/all_classes.txt"
    safe_combine_files "$output_dir/base/*_class_names.txt" "$output_dir/base/all_class_names.txt"
    safe_combine_files "$output_dir/head/*_class_names.txt" "$output_dir/head/all_class_names.txt"
    
    safe_combine_files "$output_dir/base/*_fields.txt" "$output_dir/base/all_fields.txt"
    safe_combine_files "$output_dir/head/*_fields.txt" "$output_dir/head/all_fields.txt"
    safe_combine_files "$output_dir/base/*_field_names.txt" "$output_dir/base/all_field_names.txt"
    safe_combine_files "$output_dir/head/*_field_names.txt" "$output_dir/head/all_field_names.txt"
    
    # Find differences safely
    print_status "Calculating differences..."
    
    # Ensure analysis files exist before processing
    touch "$output_dir/analysis/new_methods.txt" "$output_dir/analysis/new_classes.txt" "$output_dir/analysis/new_fields.txt" 2>/dev/null || true
    touch "$output_dir/analysis/removed_methods.txt" "$output_dir/analysis/removed_classes.txt" "$output_dir/analysis/removed_fields.txt" 2>/dev/null || true
    
    # New methods
    if [[ -f "$output_dir/head/all_methods.txt" ]] && [[ -f "$output_dir/base/all_methods.txt" ]]; then
        comm -23 "$output_dir/head/all_methods.txt" "$output_dir/base/all_methods.txt" > "$output_dir/analysis/new_methods.txt" 2>/dev/null || true
    fi
    NEW_METHODS_COUNT=$(safe_line_count "$output_dir/analysis/new_methods.txt")
    
    # New classes
    if [[ -f "$output_dir/head/all_classes.txt" ]] && [[ -f "$output_dir/base/all_classes.txt" ]]; then
        comm -23 "$output_dir/head/all_classes.txt" "$output_dir/base/all_classes.txt" > "$output_dir/analysis/new_classes.txt" 2>/dev/null || true
    fi
    NEW_CLASSES_COUNT=$(safe_line_count "$output_dir/analysis/new_classes.txt")
    
    # New fields
    if [[ -f "$output_dir/head/all_fields.txt" ]] && [[ -f "$output_dir/base/all_fields.txt" ]]; then
        comm -23 "$output_dir/head/all_fields.txt" "$output_dir/base/all_fields.txt" > "$output_dir/analysis/new_fields.txt" 2>/dev/null || true
    fi
    NEW_FIELDS_COUNT=$(safe_line_count "$output_dir/analysis/new_fields.txt")
    
    # Removed methods
    if [[ -f "$output_dir/head/all_methods.txt" ]] && [[ -f "$output_dir/base/all_methods.txt" ]]; then
        comm -13 "$output_dir/head/all_methods.txt" "$output_dir/base/all_methods.txt" > "$output_dir/analysis/removed_methods.txt" 2>/dev/null || true
    fi
    REMOVED_METHODS_COUNT=$(safe_line_count "$output_dir/analysis/removed_methods.txt")
    
    # Removed classes
    if [[ -f "$output_dir/head/all_classes.txt" ]] && [[ -f "$output_dir/base/all_classes.txt" ]]; then
        comm -13 "$output_dir/head/all_classes.txt" "$output_dir/base/all_classes.txt" > "$output_dir/analysis/removed_classes.txt" 2>/dev/null || true
    fi
    REMOVED_CLASSES_COUNT=$(safe_line_count "$output_dir/analysis/removed_classes.txt")
    
    # Removed fields
    if [[ -f "$output_dir/head/all_fields.txt" ]] && [[ -f "$output_dir/base/all_fields.txt" ]]; then
        comm -13 "$output_dir/head/all_fields.txt" "$output_dir/base/all_fields.txt" > "$output_dir/analysis/removed_fields.txt" 2>/dev/null || true
    fi
    REMOVED_FIELDS_COUNT=$(safe_line_count "$output_dir/analysis/removed_fields.txt")
    
    # For now, set modified counts to 0 (can be enhanced later)
    MODIFIED_METHODS_COUNT=0
    MODIFIED_CLASSES_COUNT=0
    MODIFIED_FIELDS_COUNT=0
    
    # Get file change statistics
    FILES_MODIFIED=$(git diff --name-only "$base_commit" "$head_sha" 2>/dev/null | grep -c '\.java$' || echo "0")
    LINES_ADDED=$(git diff --stat "$base_commit" "$head_sha" 2>/dev/null | tail -1 | grep -o '[0-9]\+ insertion' | grep -o '[0-9]\+' || echo "0")
    LINES_REMOVED=$(git diff --stat "$base_commit" "$head_sha" 2>/dev/null | tail -1 | grep -o '[0-9]\+ deletion' | grep -o '[0-9]\+' || echo "0")
    
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
    
    if [[ "$NEW_METHODS_COUNT" -gt 0 ]]; then
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
    
    if [[ "$NEW_CLASSES_COUNT" -gt 0 ]]; then
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
    
    if [[ "$NEW_FIELDS_COUNT" -gt 0 ]]; then
        echo "Found $NEW_FIELDS_COUNT new field(s):" >> "$output_dir/analysis/change_report.md"
        echo "" >> "$output_dir/analysis/change_report.md"
        while IFS= read -r field; do
            echo "- \`$field\`" >> "$output_dir/analysis/change_report.md"
        done < "$output_dir/analysis/new_fields.txt"
    else
        echo "No new fields detected." >> "$output_dir/analysis/change_report.md"
    fi
    
    cat >> "$output_dir/analysis/change_report.md" << EOF

## 🗑️ Removed Methods
EOF
    
    if [[ "$REMOVED_METHODS_COUNT" -gt 0 ]]; then
        echo "Found $REMOVED_METHODS_COUNT removed method(s):" >> "$output_dir/analysis/change_report.md"
        echo "" >> "$output_dir/analysis/change_report.md"
        while IFS= read -r method; do
            echo "- \`$method\`" >> "$output_dir/analysis/change_report.md"
        done < "$output_dir/analysis/removed_methods.txt"
    else
        echo "No methods were removed." >> "$output_dir/analysis/change_report.md"
    fi
    
    cat >> "$output_dir/analysis/change_report.md" << EOF

## 🗑️ Removed Classes
EOF
    
    if [[ "$REMOVED_CLASSES_COUNT" -gt 0 ]]; then
        echo "Found $REMOVED_CLASSES_COUNT removed class(es):" >> "$output_dir/analysis/change_report.md"
        echo "" >> "$output_dir/analysis/change_report.md"
        while IFS= read -r class; do
            echo "- \`$class\`" >> "$output_dir/analysis/change_report.md"
        done < "$output_dir/analysis/removed_classes.txt"
    else
        echo "No classes were removed." >> "$output_dir/analysis/change_report.md"
    fi
    
    cat >> "$output_dir/analysis/change_report.md" << EOF

## 🗑️ Removed Fields
EOF
    
    if [[ "$REMOVED_FIELDS_COUNT" -gt 0 ]]; then
        echo "Found $REMOVED_FIELDS_COUNT removed field(s):" >> "$output_dir/analysis/change_report.md"
        echo "" >> "$output_dir/analysis/change_report.md"
        while IFS= read -r field; do
            echo "- \`$field\`" >> "$output_dir/analysis/change_report.md"
        done < "$output_dir/analysis/removed_fields.txt"
    else
        echo "No fields were removed." >> "$output_dir/analysis/change_report.md"
    fi
    
    cat >> "$output_dir/analysis/change_report.md" << EOF

## 📁 Modified Files
EOF
    
    git diff --name-only "$base_commit" "$head_sha" 2>/dev/null | grep '\.java$' | while read -r file; do
        echo "- \`$file\`" >> "$output_dir/analysis/change_report.md"
    done
    
    cat >> "$output_dir/analysis/change_report.md" << EOF

---

*This release was automatically generated by the Ellithium Enhanced Release System*
EOF
    
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
if [[ $# -ne 3 ]]; then
    print_error "Usage: $0 <base_sha> <head_sha> <output_dir>"
    exit 1
fi

BASE_SHA="$1"
HEAD_SHA="$2"
OUTPUT_DIR="$3"

# Set error handling
set -euo pipefail

# Trap errors to provide better error messages
trap 'print_error "Script failed at line $LINENO. Check the logs above for details."; exit 1' ERR

# Validate inputs
if [[ -z "$BASE_SHA" ]] || [[ -z "$HEAD_SHA" ]] || [[ -z "$OUTPUT_DIR" ]]; then
    print_error "All arguments must be non-empty"
    exit 1
fi

# Ensure output directory is writable
if ! mkdir -p "$OUTPUT_DIR" 2>/dev/null; then
    print_error "Cannot create output directory: $OUTPUT_DIR"
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
