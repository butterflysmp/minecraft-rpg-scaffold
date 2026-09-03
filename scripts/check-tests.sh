#!/usr/bin/env bash
#
# Assert every test-bearing module actually ran its tests.
#
#   ./scripts/check-tests.sh
#
# A green build that ran no tests is indistinguishable from a green build that
# ran all of them. Surefire prints nothing when a bad -Dtest pattern matches
# nothing, and BUILD SUCCESS looks the same either way.
#
# The invariant is PER-MODULE REPORT PRESENCE, not a non-zero total. The guard
# this replaces summed every module and failed only at 0. Measured: with
# <skip>true</skip> on paper's surefire, `./mvnw -B clean verify` returns BUILD
# SUCCESS, paper writes zero reports, and the old guard printed
#
#     Tests run across all modules: 101 (from 11 report files)
#
# and exited 0. Non-zero, green, one module's 55 tests gone. A bare total cannot
# see that; module presence can.
#
# STALENESS IS THE SECOND HOLE, and it is the same defect one layer up: this
# script reads whatever reports are ON DISK, from whenever they were written.
# Reports outlive the code they tested.
#
# Measured 2026-09-03: a branch with ~14 tests' worth of new work was reverted
# with `git stash`, and this script -- run against the reverted tree, with no
# build in between -- reported
#
#     Tests run across all modules: 1196
#
# for a tree that has 1182. Green, precise, and describing code that was no
# longer there. That is the stale-jar trap of CLAUDE.md's verification section,
# occurring inside the tool used to prove the tests ran.
#
# So: if any source file is NEWER than the newest surefire report, the reports
# describe an older tree and the total is not about the code in front of you.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$(cd "$SCRIPT_DIR/.." && pwd)"

# This list IS the spec: a module that runs zero test files is a hole, not a
# pass. rpg-parent is <packaging>pom</packaging> and has no tests, so it is
# absent. A new test-bearing module must be added here -- an unlisted module is
# not guarded. Deriving it from the parent's <modules> would need a pom parser
# in bash, and would silently include the parent.
MODULES="core storage paper"

missing=""
total=0

for m in $MODULES; do
  # `find | wc -l` cannot fail: find exits 0 with no matches. Safe under pipefail.
  count=$(find "./$m" -path '*/target/surefire-reports/*.txt' -type f | wc -l)

  if [ "$count" -eq 0 ]; then
    missing="$missing $m"
    echo "::error::module '$m' produced no surefire reports -- its tests did not run"
    continue
  fi

  # grep exits 1 when it matches nothing, which under `set -e` + pipefail would
  # abort the whole step and read as a spurious failure. The guard this replaces
  # carries that hazard latent; it survives only because matches happen to exist.
  # `|| true` neutralises it and awk sums an empty stream to 0.
  module_total=$(find "./$m" -path '*/target/surefire-reports/*.txt' -type f -print0 \
    | xargs -0 grep -h -o 'Tests run: [0-9]*' \
    | awk '{s += $3} END {print s+0}' || true)
  module_total=${module_total:-0}

  echo "module '$m': $count report file(s), $module_total test(s)"
  total=$((total + module_total))
done

echo "Tests run across all modules: $total"

# --- STALENESS -------------------------------------------------------------
#
# The newest source file against the newest report. `find -newer` is a strict
# mtime comparison, so a source touched in the same second as the report does
# not trip it -- deliberately: the false-positive cost here is a scary message
# on a correct run, and this guard is worth nothing if people learn to ignore it.
#
# EXIT 1, not a warning. A warning on a green line is read as green; the whole
# point is that a stale total is indistinguishable from a fresh one.
newest_report=$(find ./core ./storage ./paper -path '*/target/surefire-reports/*.txt' -type f \
  -printf '%T@ %p\n' 2>/dev/null | sort -rn | head -1 | cut -d' ' -f2-)

if [ -n "$newest_report" ]; then
  newer_sources=$(find ./core/src ./storage/src ./paper/src -name '*.java' -type f \
    -newer "$newest_report" 2>/dev/null | head -5)

  if [ -n "$newer_sources" ]; then
    echo "::error::STALE REPORTS -- source files are newer than the newest surefire report."
    echo "::error::The total above describes an OLDER tree. Run a build before believing it."
    echo "newest report: $newest_report"
    echo "newer sources (first 5):"
    echo "$newer_sources" | sed 's/^/  /'
    exit 1
  fi
fi

if [ -n "$missing" ]; then
  echo "::error::missing surefire reports for module(s):$missing"
  exit 1
fi

if [ "$total" -eq 0 ]; then
  echo "::error::surefire reports exist but ran 0 tests"
  exit 1
fi
