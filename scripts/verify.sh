#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

echo "==> Checking project invariants..."
python3 scripts/check_invariants.py

echo "==> Running unit tests, Android Lint, Detekt, and Debug APK build..."
./gradlew testDebugUnitTest lintDebug detekt assembleDebug --no-daemon

echo "==> All local verification checks passed."
