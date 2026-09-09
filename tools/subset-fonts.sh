#!/usr/bin/env bash
# Subset the bundled IBM Plex TTFs to the glyphs CarCareo actually renders.
#
# The app is offline-first and its UI text is Spanish + device-locale date/number/
# currency formatting. Anything outside the ranges below (user notes pasted in an
# exotic script, emoji, …) falls back to the system font at render time, so the
# subset only needs to cover Latin text + the punctuation/currency the formatters
# can emit.
#
# Ranges kept:
#   U+0000-024F  Basic Latin, Latin-1 Supplement, Latin Extended-A + -B
#                (á é í ñ ó ú ü ç ¿ ¡ · × ÷ ° ª º µ £ ¥ § — plus every other
#                 European accented letter, so user-typed names stay in-font)
#   U+02B0-02FF  spacing modifier letters
#   U+2000-206F  General Punctuation (en/em dash, curly quotes, bullet, …,
#                 no-break / thin / narrow-no-break spaces used as group separators)
#   U+20A0-20BF  Currency Symbols (€ and friends, for getCurrencyInstance)
#   U+2122       ™
#   U+2190-2193  basic arrows
#   U+2212 etc.  common math operators
#
# Requires: fonttools (pip install --user fonttools)
set -euo pipefail

FONT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../app/src/main/res/font" && pwd)"
UNICODES="U+0000-024F,U+02B0-02FF,U+2000-206F,U+20A0-20BF,U+2122,U+2190-2193,U+2212,U+2202,U+2206,U+220F,U+2211,U+221A,U+221E,U+2248,U+2260,U+2264,U+2265"

total_before=0
total_after=0
for f in "$FONT_DIR"/ibm_plex_*.ttf; do
    before=$(stat -c%s "$f")
    tmp="${f}.subset"
    pyftsubset "$f" \
        --unicodes="$UNICODES" \
        --layout-features='*' \
        --notdef-outline \
        --recalc-bounds \
        --drop-tables+=DSIG \
        --name-IDs='*' \
        --output-file="$tmp"
    after=$(stat -c%s "$tmp")
    mv "$tmp" "$f"
    total_before=$((total_before + before))
    total_after=$((total_after + after))
    printf '%-32s %7d -> %6d B  (%d glyphs)\n' \
        "$(basename "$f")" "$before" "$after" \
        "$(python3 -c "from fontTools.ttLib import TTFont;print(TTFont('$f')['maxp'].numGlyphs)")"
done
printf '%-32s %7d -> %6d B  (saved %d KB)\n' \
    TOTAL "$total_before" "$total_after" "$(( (total_before - total_after) / 1024 ))"
