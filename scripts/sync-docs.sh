#!/usr/bin/env bash
# Sync root web app files to docs/ for GitHub Pages, excluding allowlisted differences.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DOCS_DIR="$ROOT_DIR/docs"

# Files that must match exactly (web entry points and assets)
SYNC_FILES=(
  "index.html"
  "app.css"
  "theme-init.js"
  "app.js"
  "robots.txt"
  "sitemap.xml"
  "404.html"
  "ai-handoff.js"
  "bibliography.js"
  "citation.js"
  "literature.js"
  "help.js"
  "backup-crypto.js"
  "calendar-ics.js"
  "student.js"
  "pdf-reader.js"
  "pdf-reader.css"
  "manifest.webmanifest"
  "sw.js"
)

echo "Syncing web files from root to docs/ ..."
for f in "${SYNC_FILES[@]}"; do
  if [[ -f "$ROOT_DIR/$f" ]]; then
    cp "$ROOT_DIR/$f" "$DOCS_DIR/$f"
    echo "  copied $f"
  else
    echo "  warning: $f not found in root"
  fi
done

# Sync vendor if present
if [[ -d "$ROOT_DIR/vendor" ]]; then
  mkdir -p "$DOCS_DIR/vendor"
  cp -R "$ROOT_DIR/vendor/." "$DOCS_DIR/vendor/"
  echo "  synced vendor/"
fi

echo "Sync complete. Run CI check to verify no drift."
