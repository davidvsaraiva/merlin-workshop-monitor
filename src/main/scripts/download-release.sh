#!/usr/bin/env bash
# Download a released (tagged) shaded JAR from GitHub Releases into the current folder.
#
# Usage:
#   ./download-release.sh          # downloads the latest release
#   ./download-release.sh v1.0.0   # downloads a specific tag

set -euo pipefail

REPO="davidvsaraiva/merlin-workshop-monitor"
OUT_DIR="."                     # current directory

TAG="${1:-}"

if [ -z "$TAG" ]; then
  echo "🔍 Finding latest release for $REPO..."
  TAG=$(gh release list -R "$REPO" --limit 1 --json tagName --jq '.[0].tagName')
fi

if [ -z "${TAG:-}" ]; then
  echo "❌ No releases found."
  exit 1
fi

echo "➡️  Release: $TAG"
echo "⬇️  Downloading JAR asset(s) into $OUT_DIR ..."
gh release download "$TAG" -R "$REPO" -D "$OUT_DIR" -p "*.jar" --clobber

echo "✅ Done. Files saved in $OUT_DIR"
