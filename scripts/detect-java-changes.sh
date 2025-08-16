#!/bin/bash

# Ellithium Java Change Detector for Release Notes

set -euo pipefail

LATEST_TAG=$(git describe --tags --abbrev=0)
echo "[INFO] Comparing changes between $LATEST_TAG and main branch..."

CHANGED_FILES=$(git diff --name-only "$LATEST_TAG"..main | grep '\.java$' || true)

RELEASE_MD="scripts/release-java-changes.md"
echo "# Java API Changes since $LATEST_TAG" > "$RELEASE_MD"

for FILE in $CHANGED_FILES; do
    BASENAME=$(basename "$FILE")
    echo -e "\n## $FILE" >> "$RELEASE_MD"
    git diff "$LATEST_TAG"..main -- "$FILE" > "scripts/filediff.tmp"
    grep -E '^[+-].*\b(class|interface|enum|void|public|protected|private)\b' scripts/filediff.tmp \
        | grep -vE '^\+\+\+|---' >> "$RELEASE_MD"
    echo -e "\n<details><summary>Full diff</summary>\n\n" >> "$RELEASE_MD"
    cat scripts/filediff.tmp >> "$RELEASE_MD"
    echo -e "\n</details>" >> "$RELEASE_MD"
done

rm -f scripts/filediff.tmp

echo "[INFO] API diff written to $RELEASE_MD"