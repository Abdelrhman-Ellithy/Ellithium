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
    
    # Check if output directory is writable
    if [[ ! -w "$output_dir" ]]; then
        print_warning "Output directory not writable: $output_dir"
        return 1
    fi
    
    print_status "Processing $basename_file"
    
    # Extract methods using robust pattern matching with better error handling
    local methods_file="$output_dir/${basename_file}_methods.txt"
    if timeout 30 awk '
    /^[[:space:]]*(public|private|protected|static|final|synchronized|abstract|native|strictfp)?[[:space:]]*[a-zA-Z_][a-zA-Z0-9_<>\[\]\s]*[[:space:]]+[a-zA-Z_][a-zA-Z0-9_]*[[:space:]]*\([^)]*\)[[:space:]]*(throws[[:space:]]+[^{]*)?[[:space:]]*\{?[[:space:]]*$/ {
        # Clean up the method signature
        gsub(/^[[:space:]]+/, "")  # Remove leading spaces
        gsub(/[[:space:]]*\{.*$/, "")  # Remove everything after {
        gsub(/[[:space:]]+/, " ")  # Normalize spaces
        print
    }' "$file" 2>/dev/null | sort -u > "$methods_file" 2>/dev/null; then
        if [[ -f "$methods_file" ]] && [[ -s "$methods_file" ]]; then
            print_status "Successfully extracted methods from $basename_file"
        else
            print_warning "Methods file created but appears empty for $basename_file"
            touch "$methods_file"
        fi
    else
        print_warning "Failed to extract methods from $basename_file, creating empty file"
        touch "$methods_file"
    fi
    
    # Extract method names only
    if timeout 30 awk '
    /^[[:space:]]*(public|private|protected|static|final|synchronized|abstract|native|strictfp)?[[:space:]]*[a-zA-Z_][a-zA-Z0-9_<>\[\]\s]*[[:space:]]+[a-zA-Z_][a-zA-Z0-9_]*[[:space:]]*\([^)]*\)[[:space:]]*(throws[[:space:]]+[^{]*)?[[:space:]]*\{?[[:space:]]*$/ {
        # Extract just the method name
        match($0, /[[:space:]]+([a-zA-Z_][a-zA-Z0-9_]*)[[:space:]]*\(/)
        if (RSTART > 0) {
            print substr($0, RSTART + RLENGTH - 1, RLENGTH - 1)
        }
    }' "$file" 2>/dev/null | sort -u > "$output_dir/${basename_file}_method_names.txt" 2>/dev/null; then
        print_status "Successfully extracted method names from $basename_file"
    else
        print_warning "Failed to extract method names from $basename_file, creating empty file"
        touch "$output_dir/${basename_file}_method_names.txt"
    fi
    
    # Extract classes
    if timeout 30 awk '
    /^[[:space:]]*(public|private|protected|abstract|final)?[[:space:]]*class[[:space:]]+[a-zA-Z_][a-zA-Z0-9_]*/ {
        gsub(/^[[:space:]]+/, "")
        gsub(/[[:space:]]*\{.*$/, "")
        gsub(/[[:space:]]+/, " ")
        print
    }' "$file" 2>/dev/null | sort -u > "$output_dir/${basename_file}_classes.txt" 2>/dev/null; then
        print_status "Successfully extracted classes from $basename_file"
    else
        print_warning "Failed to extract classes from $basename_file, creating empty file"
        touch "$output_dir/${basename_file}_classes.txt"
    fi
    
    # Extract class names only
    if timeout 30 awk '
    /^[[:space:]]*(public|private|protected|abstract|final)?[[:space:]]*class[[:space:]]+[a-zA-Z_][a-zA-Z0-9_]*/ {
        match($0, /class[[:space:]]+([a-zA-Z_][a-zA-Z0-9_]*)/)
        if (RSTART > 0) {
            print substr($0, RSTART + 6, RLENGTH - 6)
        }
    }' "$file" 2>/dev/null | sort -u > "$output_dir/${basename_file}_class_names.txt" 2>/dev/null; then
        print_status "Successfully extracted class names from $basename_file"
    else
        print_warning "Failed to extract class names from $basename_file, creating empty file"
        touch "$output_dir/${basename_file}_class_names.txt"
    fi
    
    # Extract fields
    if timeout 30 awk '
    /^[[:space:]]*(public|private|protected|static|final|volatile|transient)?[[:space:]]*[a-zA-Z_][a-zA-Z0-9_<>\[\]\s]*[[:space:]]+[a-zA-Z_][a-zA-Z0-9_]*[[:space:]]*[=;]/ {
        gsub(/^[[:space:]]+/, "")
        gsub(/[[:space:]]*[=;].*$/, "")
        gsub(/[[:space:]]+/, " ")
        print
    }' "$file" 2>/dev/null | sort -u > "$output_dir/${basename_file}_fields.txt" 2>/dev/null; then
        print_status "Successfully extracted fields from $basename_file"
    else
        print_warning "Failed to extract fields from $basename_file, creating empty file"
        touch "$output_dir/${basename_file}_fields.txt"
    fi
    
    # Extract field names only
    if timeout 30 awk '
    /^[[:space:]]*(public|private|protected|static|final|volatile|transient)?[[:space:]]*[a-zA-Z_][a-zA-Z0-9_<>\[\]\s]*[[:space:]]+[a-zA-Z_][a-zA-Z0-9_]*[[:space:]]*[=;]/ {
        match($0, /[[:space:]]+([a-zA-Z_][a-zA-Z0-9_]*)[[:space:]]*[=;]/)
        if (RSTART > 0) {
            print substr($0, RSTART + 1, RLENGTH - 1)
        }
    }' "$file" 2>/dev/null | sort -u > "$output_dir/${basename_file}_field_names.txt" 2>/dev/null; then
        print_status "Successfully extracted field names from $basename_file"
    else
        print_warning "Failed to extract field names from $basename_file, creating empty file"
        touch "$output_dir/${basename_file}_field_names.txt"
    fi
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

# Function to safely escape Java content for shell processing
safe_escape_java_content() {
    local content="$1"
    # Escape special characters that could cause shell interpretation issues
    echo "$content" | sed 's/[(){}[\]]/\\&/g' | sed 's/[<>]/\\&/g' | sed 's/;/\\&/g' 2>/dev/null || echo "$content"
}

# Function to safely combine files
safe_combine_files() {
    local pattern="$1"
    local output_file="$2"
    
    # Extract directory and filename from pattern
    local dir_path=$(dirname "$pattern")
    local file_pattern=$(basename "$pattern")
    
    # Clear output file first
    > "$output_file" 2>/dev/null || true
    
    # Use find with proper path handling and process files one by one
    if [[ -d "$dir_path" ]]; then
        while IFS= read -r -d '' file; do
            if [[ -f "$file" ]] && [[ -r "$file" ]]; then
                # Use cat with proper error handling and append to output
                cat "$file" 2>/dev/null >> "$output_file" || true
            fi
        done < <(find "$dir_path" -maxdepth 1 -name "$file_pattern" -type f -print0 2>/dev/null)
        
        # Sort and remove duplicates if file has content
        if [[ -s "$output_file" ]]; then
            sort -u "$output_file" > "${output_file}.tmp" 2>/dev/null && mv "${output_file}.tmp" "$output_file" 2>/dev/null || true
        fi
    fi
    
    # Ensure file exists even if empty
    touch "$output_file" 2>/dev/null || true
}

# Main analysis function
analyze_changes() {
    local base_sha="$1"
    local head_sha="$2"
    local output_dir="$3"
    
    print_status "Starting Java change analysis..."
    print_status "Base SHA: $base_sha"
    print_status "Head SHA: $head_sha"
    
    # Create output directories with better error handling
    if ! mkdir -p "$output_dir"/{base,head,analysis} 2>/dev/null; then
        print_error "Failed to create output directories"
        exit 1
    fi
    
    # Ensure directories are writable
    if [[ ! -w "$output_dir" ]]; then
        print_error "Output directory is not writable: $output_dir"
        exit 1
    fi
    
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
    
    # Validate commits exist and are accessible
    validate_commit() {
        local commit="$1"
        local commit_name="$2"
        
        if ! git rev-parse --verify "$commit" >/dev/null 2>&1; then
            print_error "Invalid $commit_name commit: $commit"
            return 1
        fi
        
        # Test if we can access the commit content
        if ! git show "$commit" --name-only >/dev/null 2>&1; then
            print_error "Cannot access content of $commit_name commit: $commit"
            return 1
        fi
        
        print_status "✅ $commit_name commit validated: $commit"
        return 0
    }
    
    # Validate both commits
    if ! validate_commit "$base_commit" "base"; then
        print_error "Base commit validation failed"
        exit 1
    fi
    
    if ! validate_commit "$head_sha" "head"; then
        print_error "Head commit validation failed"
        exit 1
    fi
    
    # Get ALL Java files that exist in each commit (not just changed files)
    print_status "Extracting main source Java files (excluding test files)..."
    
    # Function to safely list Java files from a commit
    safe_list_java_files() {
        local commit="$1"
        local output_file="$2"
        
        # Clear output file
        > "$output_file" 2>/dev/null || true
        
        # Try multiple patterns to find Java files
        local found_files=false
        
        # Pattern 1: Standard Maven structure
        if git ls-tree -r --name-only "$commit" 2>/dev/null | grep 'src/main/java.*\.java$' > "$output_file" 2>/dev/null; then
            if [[ -s "$output_file" ]]; then
                print_status "Successfully listed Java files from commit $commit (Maven structure)"
                found_files=true
            fi
        fi
        
        # Pattern 2: If no files found, try broader search
        if [[ "$found_files" == false ]]; then
            if git ls-tree -r --name-only "$commit" 2>/dev/null | grep '\.java$' > "$output_file" 2>/dev/null; then
                if [[ -s "$output_file" ]]; then
                    print_status "Successfully listed Java files from commit $commit (broad search)"
                    found_files=true
                fi
            fi
        fi
        
        # Pattern 3: If still no files, try to find any Java files in the commit
        if [[ "$found_files" == false ]]; then
            if git show "$commit" --name-only 2>/dev/null | grep '\.java$' > "$output_file" 2>/dev/null; then
                if [[ -s "$output_file" ]]; then
                    print_status "Successfully listed Java files from commit $commit (commit diff)"
                    found_files=true
                fi
            fi
        fi
        
        # If no files found with any method, create empty list
        if [[ "$found_files" == false ]]; then
            print_warning "No Java files found in commit $commit with any method"
            touch "$output_file"
        fi
        
        # Validate the file list
        if [[ ! -s "$output_file" ]]; then
            print_warning "No Java files found in commit $commit"
        fi
    }
    
    # List Java files from both commits
    safe_list_java_files "$base_commit" "$output_dir/base/java_files.txt"
    safe_list_java_files "$head_sha" "$output_dir/head/java_files.txt"
    
    # Debug: Show what files we found
    print_status "Base commit Java files found:"
    if [[ -f "$output_dir/base/java_files.txt" ]] && [[ -r "$output_dir/base/java_files.txt" ]]; then
        head -10 "$output_dir/base/java_files.txt" 2>/dev/null || true
    else
        print_warning "Cannot read base commit Java files list"
    fi
    
    print_status "Head commit Java files found:"
    if [[ -f "$output_dir/head/java_files.txt" ]] && [[ -r "$output_dir/head/java_files.txt" ]]; then
        head -10 "$output_dir/head/java_files.txt" 2>/dev/null || true
    else
        print_warning "Cannot read head commit Java files list"
    fi
    
    # Add a safety check for file count
    local base_file_count=$(wc -l < "$output_dir/base/java_files.txt" 2>/dev/null || echo "0")
    local head_file_count=$(wc -l < "$output_dir/head/java_files.txt" 2>/dev/null || echo "0")
    
    print_status "Base commit has $base_file_count Java files"
    print_status "Head commit has $head_file_count Java files"
    
    # If no files found, create a minimal working set
    if [[ "$base_file_count" -eq 0 ]] && [[ "$head_file_count" -eq 0 ]]; then
        print_warning "No Java files found in either commit, this might indicate a repository issue"
        print_status "Trying alternative file discovery methods..."
        
        # Try to find any Java files in the repository
        local any_java_files=$(find . -name "*.java" -type f 2>/dev/null | head -5 || true)
        if [[ -n "$any_java_files" ]]; then
            print_status "Found Java files in working directory:"
            echo "$any_java_files" | while read -r found_file; do
                print_status "  - $found_file"
            done
        else
            print_warning "No Java files found in working directory either"
        fi
        
        # Continue with empty file lists - the script will handle this gracefully
    fi
    
    # Extract content for each Java file with better error handling
    while IFS= read -r file; do
        if [[ -n "$file" ]]; then
            local output_file="$output_dir/base/$(basename "$file")"
            local file_path="$file"
            
            # Try to extract the file using multiple git show strategies
            local extracted=false
            
            # Method 1: Try git show with the full path
            if git show "$base_commit:$file_path" > "$output_file" 2>/dev/null; then
                if [[ -r "$output_file" ]] && [[ -s "$output_file" ]]; then
                    print_status "Successfully extracted via git show: $file"
                    extracted=true
                fi
            fi
            
            # Method 2: If git show failed, try with just the filename (in case path structure changed)
            if [[ "$extracted" == false ]]; then
                local filename=$(basename "$file_path")
                if git show "$base_commit:$filename" > "$output_file" 2>/dev/null; then
                    if [[ -r "$output_file" ]] && [[ -s "$output_file" ]]; then
                        print_status "Successfully extracted via git show (filename only): $filename"
                        extracted=true
                    fi
                fi
            fi
            
            # Method 3: Try to find the file in the commit with a broader search
            if [[ "$extracted" == false ]]; then
                local filename=$(basename "$file_path")
                # Search for files with the same name in the commit
                local found_file=$(git ls-tree -r --name-only "$base_commit" 2>/dev/null | grep "/$filename$" | head -1)
                if [[ -n "$found_file" ]]; then
                    if git show "$base_commit:$found_file" > "$output_file" 2>/dev/null; then
                        if [[ -r "$output_file" ]] && [[ -s "$output_file" ]]; then
                            print_status "Successfully extracted via git show (found path): $found_file"
                            extracted=true
                        fi
                    fi
                fi
            fi
            
            # Method 4: If all failed, create empty file
            if [[ "$extracted" == false ]]; then
                print_warning "Failed to extract: $file, creating empty file"
                touch "$output_file"
            fi
        fi
    done < "$output_dir/base/java_files.txt"
    
    while IFS= read -r file; do
        if [[ -n "$file" ]]; then
            local output_file="$output_dir/head/$(basename "$file")"
            local file_path="$file"
            
            # Try to extract the file using multiple git show strategies
            local extracted=false
            
            # Method 1: Try git show with the full path
            if git show "$head_sha:$file_path" > "$output_file" 2>/dev/null; then
                if [[ -r "$output_file" ]] && [[ -s "$output_file" ]]; then
                    print_status "Successfully extracted via git show: $file"
                    extracted=true
                fi
            fi
            
            # Method 2: If git show failed, try with just the filename (in case path structure changed)
            if [[ "$extracted" == false ]]; then
                local filename=$(basename "$file_path")
                if git show "$head_sha:$filename" > "$output_file" 2>/dev/null; then
                    if [[ -r "$output_file" ]] && [[ -s "$output_file" ]]; then
                        print_status "Successfully extracted via git show (filename only): $filename"
                        extracted=true
                    fi
                fi
            fi
            
            # Method 3: Try to find the file in the commit with a broader search
            if [[ "$extracted" == false ]]; then
                local filename=$(basename "$file_path")
                # Search for files with the same name in the commit
                local found_file=$(git ls-tree -r --name-only "$head_sha" 2>/dev/null | grep "/$filename$" | head -1)
                if [[ -n "$found_file" ]]; then
                    if git show "$head_sha:$found_file" > "$output_file" 2>/dev/null; then
                        if [[ -r "$output_file" ]] && [[ -s "$output_file" ]]; then
                            print_status "Successfully extracted via git show (found path): $found_file"
                            extracted=true
                        fi
                    fi
                fi
            fi
            
            # Method 4: If all failed, create empty file
            if [[ "$extracted" == false ]]; then
                print_warning "Failed to extract: $file, creating empty file"
                touch "$output_file"
            fi
        fi
    done < "$output_dir/head/java_files.txt"
    
    # Analyze each Java file
    print_status "Analyzing Java files for changes..."
    
    # Process base files
    for file in "$output_dir"/base/*.java; do
        if [[ -f "$file" ]] && [[ -r "$file" ]] && [[ -s "$file" ]]; then
            basename_file=$(basename "$file")
            print_status "Processing base file: $basename_file"
            # Add a small delay to prevent overwhelming the system
            sleep 0.1 2>/dev/null || true
            if ! extract_java_elements "$file" "$output_dir/base" "$basename_file"; then
                print_warning "Failed to extract elements from $basename_file, continuing with next file"
            fi
        fi
    done
    
    # Process head files
    for file in "$output_dir"/head/*.java; do
        if [[ -f "$file" ]] && [[ -r "$file" ]] && [[ -s "$file" ]]; then
            basename_file=$(basename "$file")
            print_status "Processing head file: $basename_file"
            # Add a small delay to prevent overwhelming the system
            sleep 0.1 2>/dev/null || true
            if ! extract_java_elements "$file" "$output_dir/head" "$basename_file"; then
                print_warning "Failed to extract elements from $basename_file, continuing with next file"
            fi
        fi
    done
    
    # Combine all extracted data safely
    print_status "Combining extracted data..."
    
    # Function to safely combine files with pattern matching
    combine_files_safe() {
        local base_dir="$1"
        local pattern="$2"
        local output_file="$3"
        
        # Clear output file
        > "$output_file" 2>/dev/null || true
        
        # Find and process files one by one
        for file in "$base_dir"/$pattern; do
            if [[ -f "$file" ]] && [[ -r "$file" ]]; then
                cat "$file" 2>/dev/null >> "$output_file" || true
            fi
        done
        
        # Sort and deduplicate if file has content
        if [[ -s "$output_file" ]]; then
            sort -u "$output_file" > "${output_file}.tmp" 2>/dev/null && mv "${output_file}.tmp" "$output_file" 2>/dev/null || true
        fi
    }
    
    # Combine files using the safe function
    combine_files_safe "$output_dir/base" "*_methods.txt" "$output_dir/base/all_methods.txt"
    combine_files_safe "$output_dir/head" "*_methods.txt" "$output_dir/head/all_methods.txt"
    combine_files_safe "$output_dir/base" "*_method_names.txt" "$output_dir/base/all_method_names.txt"
    combine_files_safe "$output_dir/head" "*_method_names.txt" "$output_dir/head/all_method_names.txt"
    
    combine_files_safe "$output_dir/base" "*_classes.txt" "$output_dir/base/all_classes.txt"
    combine_files_safe "$output_dir/head" "*_classes.txt" "$output_dir/head/all_classes.txt"
    combine_files_safe "$output_dir/base" "*_class_names.txt" "$output_dir/base/all_class_names.txt"
    combine_files_safe "$output_dir/head" "*_class_names.txt" "$output_dir/head/all_class_names.txt"
    
    combine_files_safe "$output_dir/base" "*_fields.txt" "$output_dir/base/all_fields.txt"
    combine_files_safe "$output_dir/head" "*_fields.txt" "$output_dir/head/all_fields.txt"
    combine_files_safe "$output_dir/base" "*_field_names.txt" "$output_dir/base/all_field_names.txt"
    combine_files_safe "$output_dir/head" "*_field_names.txt" "$output_dir/head/all_field_names.txt"
    
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

# Check if we're in a git repository
if ! git rev-parse --git-dir > /dev/null 2>&1; then
    print_error "Not in a git repository"
    exit 1
fi

# Check git status and provide debugging info
print_status "Git repository status:"
print_status "Current directory: $(pwd)"
print_status "Git root: $(git rev-parse --show-toplevel 2>/dev/null || echo 'Unknown')"
print_status "Git remote: $(git remote get-url origin 2>/dev/null || echo 'No origin remote')"

# Trap errors to provide better error messages
trap 'print_error "Script failed at line $LINENO. Check the logs above for details."; exit 1' ERR

# Add timeout protection for long-running operations
timeout_handler() {
    print_error "Operation timed out. This might be due to large files or system issues."
    exit 1
}

# Set timeout for the entire script (5 minutes)
trap timeout_handler ALRM
readonly SCRIPT_TIMEOUT=300

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
