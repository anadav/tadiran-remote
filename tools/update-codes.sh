#!/bin/sh
# Refresh the bundled IR codes from tadiran-irdb.
set -e
cd "$(dirname "$0")/.."
curl -sfL -o app/src/main/assets/Tadiran_1345_full.ir \
  https://raw.githubusercontent.com/anadav/tadiran-irdb/main/ACs/Tadiran/Tadiran_1345_full.ir
git diff --stat -- app/src/main/assets
