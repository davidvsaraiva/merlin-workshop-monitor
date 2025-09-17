#!/usr/bin/env bash
# Download the latest successful snapshot JAR from GitHub Actions into the current folder

set -euo pipefail

REPO="davidvsaraiva/merlin-workshop-monitor"
WORKFLOW="CI - build & test"   # or "build.yml"
ARTIFACT_NAME="snapshot"
OUT_DIR="."                     # current directory

echo "🔍 Finding latest successful run for $WORKFLOW on main..."
RUN_ID=$(gh run list -R "$REPO" \
  --branch main \
  --workflow "$WORKFLOW" \
  --limit 50 \
  --json databaseId,conclusion \
  --jq '[.[] | select(.conclusion=="success")][0].databaseId')

if [ -z "${RUN_ID:-}" ]; then
  echo "❌ No successful runs found."
  exit 1
fi

echo "➡️  Latest run ID: $RUN_ID"
echo "⬇️  Downloading artifact: $ARTIFACT_NAME into $OUT_DIR ..."
gh run download -R "$REPO" "$RUN_ID" -n "$ARTIFACT_NAME" -D "$OUT_DIR"

echo "✅ Done. Files saved in $OUT_DIR"
