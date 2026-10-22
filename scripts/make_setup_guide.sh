#!/usr/bin/env bash
# Prints docs/setup-guide.html to docs/setup-guide.pdf (one A4 page) with
# Google Chrome, for staff and for the .dmg (docs/packaging.md). Run it after
# changing the guide; scripts/package_mac.sh puts the PDF on the .dmg.
set -euo pipefail
cd "$(dirname "$0")/.."

chrome="/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
[ -x "$chrome" ] || { echo "needs Google Chrome in /Applications" >&2; exit 1; }
profile=$(mktemp -d)
trap 'rm -rf "$profile"' EXIT
# The guide loads Mukta from the repo, so Chrome may read local files.
"$chrome" --headless=new --disable-gpu --no-pdf-header-footer --allow-file-access-from-files \
  --user-data-dir="$profile" --print-to-pdf="$PWD/docs/setup-guide.pdf" "file://$PWD/docs/setup-guide.html" 2>/dev/null
pages=$(python3 -c 'import re, sys; print(len(re.findall(rb"/Type\s*/Page[^s]", open(sys.argv[1], "rb").read())))' docs/setup-guide.pdf)
[ "$pages" = 1 ] || echo "warning: the guide is $pages pages, not 1" >&2
echo "docs/setup-guide.pdf: $pages page(s), $(du -h docs/setup-guide.pdf | cut -f1)"
