#!/usr/bin/env bash
#
# Is any file in the WORKING TREE mixed CRLF/LF?
#
#   ./scripts/check-crlf.sh [--quiet]
#
# WHY THIS EXISTS. A file that is part CRLF and part LF does not break the
# history -- it breaks the INSTRUMENT. CLAUDE.md's way of telling CRLF from LF
# is `tr -cd '\r' | wc -c` against the line count, and against a mixed file that
# returns a number which is neither, so every CR reading taken afterwards is
# untrustworthy. Measured 2026-09-19: AdapterContext.java ended 68 lines / 67 CR
# -- one LF line from a `\n` in a `perl` replacement -- in a slice that took many
# CR readings.
#
# It is produced by scripted edits. `sed -i` in Git Bash silently rewrites a
# CRLF file to LF; a `\n` inside a `perl` replacement inserts a lone LF into an
# otherwise-CRLF file. Neither reports anything. Run this BEFORE taking CR
# readings on a tree that scripted edits have touched.
#
# *** SCOPE, AND IT IS NARROWER THAN IT LOOKS. THIS MEASURES THE WORKING TREE. ***
# It says NOTHING about the history. With `core.autocrlf=true` -- which is this
# clone's local setting, global unset -- git normalises CRLF to LF on the way
# into the index, so a committed blob is LF and the CRLF is reconstructed on
# checkout. A mixed working tree therefore has a CLEAN blob, a fresh clone of the
# same commit has zero mixed files, and for a TRACKED file the fix is therefore a
# re-checkout rather than a commit:
#
#     rm <path> && git checkout -- <path>
#
# *** THAT DOES NOT APPLY TO AN UNTRACKED FILE, WHICH HAS NO BLOB TO RESTORE
# FROM. *** `git checkout --` on a file git has never seen cannot restore
# anything; it fails loudly, and that loud failure is the whole protection. Never
# `|| true` it. The failure output below says which case a flagged file is in.
#
# So this is local hygiene, not a CI gate. It protects the reader, not the repo.
# To ask the other question -- did a line-ending change reach history? -- the
# instrument is the blob, named as the blob:
#
#     git show <ref>:<path> | tr -cd '\r' | wc -c
#
# WHY `tr` AND NOT `sed`/`grep`. Both strip CR before matching in Git Bash, so a
# `\r$` test can never match on any file and `cat -A` prints `$` where the raw
# bytes hold `^M$`. Measured: `grep -c -v $'\r$'` reported EVERY line as lacking
# CRLF on a wholly-CRLF file. When the thing you are testing for is something a
# tool is entitled to normalise, the test must go through something that cannot
# normalise it. `tr` cannot. That is why it is right -- not thoroughness.
#
# THE CLASSIFIER, and the trailing-newline case it has to survive:
#
#   cr == 0    uniform LF     OK
#   cr == lf   uniform CRLF   OK
#   otherwise  MIXED          the instrument is poisoned on this file
#
# A file that does not end in a newline contributes neither a CR nor an LF for
# its last line, so `cr == lf` still holds and it is not a false positive. A lone
# CR (classic Mac) gives cr > lf and is flagged, correctly.
#
# *** UNIFORM LF ON A CRLF TREE IS NOT FLAGGED, AND THAT IS DELIBERATE. *** The
# hazard is mixture, not disagreement with neighbours. A few dozen files read
# uniform LF here -- `mvnw` and `*.sh` by .gitattributes, the rest simply landed
# that way -- and every one of them gives an honest CR reading. No live count is
# written down: this script PRINTS the tally on every run, so a literal here
# would be a figure maintained by delta with its own instrument two lines away.
#
# EXIT CODES -- three states, deliberately not two:
#
#   0  CLEAN         no file in the sweep is mixed.
#   1  MIXED         at least one is, and each is named with its counts.
#   3  BLIND         a control failed. NO VERDICT WAS REACHED.
#  64  usage error.
#
# *** WHY THERE ARE THREE CONTROLS AND NOT ONE. *** A scan that finds nothing
# reads exactly like a scan that ran correctly and found nothing -- this repo's
# oldest recorded defect. So the classifier is run against known inputs on every
# invocation. But ONE control is satisfiable by a broken instrument:
#
#   a classifier hardcoded to MIXED passes a negative control alone
#   a classifier hardcoded to CLEAN passes a positive control alone
#
# So both directions are asserted, and the positive side is asserted TWICE --
# once for CRLF and once for LF -- because the OK branch has two arms and a
# classifier that handled only one of them would still pass a single positive.
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$(cd "$SCRIPT_DIR/.." && pwd)"

QUIET=0
while [ $# -gt 0 ]; do
  case "$1" in
    --quiet) QUIET=1; shift ;;
    -h|--help) sed -n '2,6p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) echo "usage: $0 [--quiet]" >&2; exit 64 ;;
  esac
done

# The whole measurement, in one place, so the controls exercise the SAME code
# the sweep does. A control that runs a re-implementation of the classifier
# controls the re-implementation.
classify() {
  local f="$1" cr lf
  cr="$(tr -cd '\r' < "$f" | wc -c)"
  lf="$(tr -cd '\n' < "$f" | wc -c)"
  if   [ "$cr" -eq 0 ];    then echo "LF $cr $lf"
  elif [ "$cr" -eq "$lf" ]; then echo "CRLF $cr $lf"
  else                          echo "MIXED $cr $lf"
  fi
}

# --- CONTROLS --------------------------------------------------------------
# Built here rather than committed as fixtures: a committed mixed file would be
# normalised by git on the way in and would stop being mixed.
CTL_DIR="$(mktemp -d)"
trap 'rm -rf "$CTL_DIR"' EXIT

printf 'a\r\nb\nc\r\n'  > "$CTL_DIR/mixed"   # 2 CR, 3 LF  -> must be MIXED
printf 'a\r\nb\r\n'     > "$CTL_DIR/crlf"    # 2 CR, 2 LF  -> must be CRLF
printf 'a\nb\n'         > "$CTL_DIR/lf"      # 0 CR, 2 LF  -> must be LF

CTL_FAIL=""
for want in mixed:MIXED crlf:CRLF lf:LF; do
  name="${want%%:*}"; expect="${want##*:}"
  got="$(classify "$CTL_DIR/$name" | cut -d' ' -f1)"
  [ "$got" = "$expect" ] || CTL_FAIL="$CTL_FAIL  $name: expected $expect, classified $got"$'\n'
done

if [ -n "$CTL_FAIL" ]; then
  echo "BLIND -- control failed. No verdict was reached."
  printf '%s' "$CTL_FAIL"
  echo "  The classifier cannot be trusted, so a CLEAN report would be meaningless."
  exit 3
fi
[ "$QUIET" -eq 1 ] || echo "control: PASS -- MIXED flagged, CRLF and LF both passed; the sweep is not blind"

# --- THE SWEEP -------------------------------------------------------------
#
# *** TRACKED **AND** UNTRACKED-BUT-NOT-IGNORED, and the untracked half is the
# point. *** `git ls-files` alone lists only what is in the index, and a file
# just WRITTEN is not -- which is precisely the file a scripted edit is about to
# mangle. Sweeping the index only would go green on a tree whose newest file is
# the poisoned one.
#
# `--exclude-standard` is what keeps that affordable: it honours .gitignore, so
# `target/`, `run/` and `.idea/` never enter the walk. Measured at the time of
# writing: 699 tracked + 1 untracked = 700, with no build output in it.
total=0; n_lf=0; n_crlf=0; n_mixed=0; mixed_list=""

while IFS= read -r f; do
  # A tracked path can be absent from the working tree (sparse checkout, a
  # deletion staged but not committed). Skipping is correct -- there are no
  # bytes to measure -- and it is counted out of the total rather than silently
  # folded into it.
  [ -f "$f" ] || continue
  total=$((total + 1))
  read -r kind cr lf <<<"$(classify "$f")"
  case "$kind" in
    LF)    n_lf=$((n_lf + 1)) ;;
    CRLF)  n_crlf=$((n_crlf + 1)) ;;
    MIXED) n_mixed=$((n_mixed + 1))
           mixed_list="$mixed_list  $f  CR=$cr LF=$lf"$'\n' ;;
  esac
done < <(git ls-files --cached --others --exclude-standard)

# *** FINDING ZERO FILES IS A DEFECT, NOT A QUIET PASS. *** A sweep that walked
# nothing reports CLEAN, and on a tree this size that can only mean the walk
# itself broke.
if [ "$total" -eq 0 ]; then
  echo "BLIND -- the sweep measured 0 files. git ls-files returned nothing usable."
  exit 3
fi

if [ "$QUIET" -eq 0 ]; then
  echo "swept $total files in the WORKING TREE (tracked + untracked, ignores honoured): $n_lf LF, $n_crlf CRLF, $n_mixed MIXED"
fi

if [ "$n_mixed" -gt 0 ]; then
  echo "MIXED -- $n_mixed file(s) are part CRLF and part LF."
  printf '%s' "$mixed_list"
  echo "  Every CR reading taken on these is untrustworthy."
  echo
  echo "  TRACKED file: the blob is almost certainly clean, so the fix is a"
  echo "  re-checkout and produces no commit."
  echo "    git show HEAD:<path> | tr -cd '\\r' | wc -c     # confirm the blob is 0 first"
  echo "    rm <path> && git checkout -- <path>"
  echo
  echo "  *** UNTRACKED file: THERE IS NO BLOB AND NOTHING TO CHECK OUT. ***"
  echo "  'git checkout -- <path>' cannot restore a file git has never seen; it"
  echo "  fails, and that loud failure is the only protection. Normalise it in"
  echo "  place, or rewrite it -- do NOT reach for checkout and do NOT '|| true'"
  echo "  the attempt, which discards exactly the report you need."
  exit 1
fi

[ "$QUIET" -eq 1 ] || echo "CLEAN -- no file in the sweep is mixed."
exit 0
