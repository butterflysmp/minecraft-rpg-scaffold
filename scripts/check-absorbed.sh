#!/usr/bin/env bash
#
# Is a branch's content already in master's history, so deleting it loses nothing?
#
#   ./scripts/check-absorbed.sh <branch> [--base <ref>] [--no-fetch]
#
# Every PR on this repo is SQUASH-merged, so the branch's commits never become
# ancestors of master. CLAUDE.md records what that breaks:
#
#   git branch --merged master     prints NOTHING for a fully merged branch.
#   git diff master <branch>       empty only while master has not moved since;
#                                  once master gains one commit an absorbed
#                                  branch diffs non-empty because it is BEHIND.
#                                  Measured 2026-08-28: feat/sweep-rides-vanilla,
#                                  fully absorbed, diffed as 24 files / 1122
#                                  deletions -- every line the crit commit on top.
#
# The question that can actually be answered is about CONTENT, not ancestry:
# does some commit reachable from master have a byte-identical TREE to this
# branch's tip? If yes, the work is in master whatever the commit graph says.
#
# WHY THIS IS A SCRIPT. The loop was hand-pasted at each cleanup. A
# CLAUDE.md-codified procedure that is still a hand-pasted loop is one paste
# error away from lying, and this is the check you least want to fat-finger:
# its output authorizes an irreversible `git branch -D`.
#
# EXIT CODES -- three states, deliberately not two:
#
#   0  ABSORBED      branch tree found in master's history. Safe to delete.
#   1  NOT ABSORBED  tree absent AND master has not moved past the branch point.
#                    A real STOP: there is unmerged work here.
#   2  INCONCLUSIVE  tree absent BUT master has commits the branch never saw.
#                    This check CANNOT authorize either way -- see below.
#   3  BLIND         the positive control failed. No verdict was reached.
#  64  usage error.
#
# WHY STATE 2 EXISTS, and why collapsing it into state 1 is a bug. The tree
# compared is the branch TIP's. If master gains any commit between the branch
# point and the squash-merge, the squash is built on that newer base, so its
# tree is `newbase + changes` while the branch tip is `oldbase + changes`.
# Genuinely absorbed, reports no match. A bare NOT-ABSORBED there is a FALSE
# REFUSAL, and it will happen the first time a PR lands behind another one.
#
# So the check is sound in one direction only, and that is the right direction
# for a delete guard:
#   - it can never falsely say ABSORBED (a tree match means the content is there)
#   - it CAN falsely say "absent" once master moves (hence state 2, not state 1)
#
# In state 2, confirm absorption from the PR's own merged state instead --
# `gh pr view <n> --json state,mergedAt,mergeCommit` -- and say which source the
# conclusion came from rather than letting a stale STOP stand.
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$(cd "$SCRIPT_DIR/.." && pwd)"

BRANCH=""
BASE=""
DO_FETCH=1

while [ $# -gt 0 ]; do
  case "$1" in
    --base)     BASE="${2:-}"; [ -n "$BASE" ] || { echo "--base needs a ref" >&2; exit 64; }; shift 2 ;;
    --no-fetch) DO_FETCH=0; shift ;;
    -h|--help)  sed -n '2,25p' "$SCRIPT_DIR/$(basename "${BASH_SOURCE[0]}")" | sed 's/^# \{0,1\}//'; exit 0 ;;
    -*)         echo "Unknown option: $1" >&2; exit 64 ;;
    *)          [ -z "$BRANCH" ] || { echo "Give exactly one branch" >&2; exit 64; }; BRANCH="$1"; shift ;;
  esac
done

[ -n "$BRANCH" ] || { echo "usage: $0 <branch> [--base <ref>] [--no-fetch]" >&2; exit 64; }

# FETCH BY DEFAULT. A stale origin/master is itself a source of false refusals:
# the squash could already be on the wire and simply not local yet. This only
# updates remote-tracking refs; it touches no working file and no local branch.
# --no-fetch exists for offline use, and says so in the output so a verdict
# reached against possibly-stale refs is never silently mistaken for a fresh one.
if [ "$DO_FETCH" -eq 1 ]; then
  git fetch --quiet origin || echo "WARNING: fetch failed; comparing against possibly stale refs" >&2
  FETCH_NOTE="fetched"
else
  FETCH_NOTE="NOT fetched (--no-fetch)"
fi

# Prefer the REMOTE master. Local master can lag or be ahead of the wire, and it
# is the wire that decides whether deleting the remote branch loses anything.
if [ -z "$BASE" ]; then
  if git rev-parse --verify --quiet origin/master >/dev/null; then BASE="origin/master"; else BASE="master"; fi
fi

git rev-parse --verify --quiet "$BRANCH" >/dev/null \
  || { echo "No such branch: $BRANCH" >&2; exit 64; }
git rev-parse --verify --quiet "$BASE" >/dev/null \
  || { echo "No such base ref: $BASE" >&2; exit 64; }

BRANCH_TIP="$(git rev-parse "$BRANCH")"
BASE_TIP="$(git rev-parse "$BASE")"
BRANCH_TREE="$(git rev-parse "$BRANCH^{tree}")"

echo "branch : $BRANCH  ($(git rev-parse --short "$BRANCH_TIP"))  tree $BRANCH_TREE"
echo "base   : $BASE  ($(git rev-parse --short "$BASE_TIP"))  [$FETCH_NOTE]"
echo

# One `git log` rather than a `git rev-parse` per commit: one process instead of
# hundreds, and nothing to break early out of. Collected into a variable BEFORE
# scanning, because a pipeline whose reader exits early can SIGPIPE the writer
# and trip `pipefail` -- exit 141 on the very input the check exists to handle,
# the hazard check-jar.sh records for `unzip -l | grep -q`.
HISTORY="$(git log --format='%H %T' "$BASE")"

# The awk prints at most one line and NEVER calls exit, for the same reason:
# reading to EOF costs nothing here and cannot signal its writer.
find_tree() {
  awk -v want="$1" '$2 == want && !seen { print $1; seen = 1 }' <<<"$HISTORY"
}

# --- POSITIVE CONTROL ------------------------------------------------------
#
# THE ASSERTION THAT MAKES A REFUSAL MEAN ANYTHING. A loop that reports "not
# found" for everything is indistinguishable from one correctly reporting
# absence -- the same defect CLAUDE.md records twice for content scans, where
# finding zero items read as working. So before any verdict, prove the search
# CAN say found: the base tip's own tree is trivially present in the base's
# history, so failing to find it means the search is broken, not that the tree
# is absent.
CONTROL_TREE="$(git rev-parse "$BASE^{tree}")"
CONTROL_HIT="$(find_tree "$CONTROL_TREE")"
if [ -z "$CONTROL_HIT" ]; then
  echo "BLIND -- control failed."
  echo "  The base tip's own tree ($CONTROL_TREE) was not found in its own history."
  echo "  The search is broken, so NO verdict below would mean anything. Refusing to answer."
  exit 3
fi
echo "control: PASS -- known-present tree found at $(git rev-parse --short "$CONTROL_HIT"); the search is not blind"
echo

# --- THE VERDICT -----------------------------------------------------------

MATCH="$(find_tree "$BRANCH_TREE")"
if [ -n "$MATCH" ]; then
  echo "ABSORBED at $MATCH"
  echo "  A commit reachable from $BASE has a byte-identical tree, so deleting"
  echo "  '$BRANCH' loses nothing."
  echo
  echo "  git branch -D $BRANCH"
  echo "  git push origin --delete $BRANCH"
  echo "  git fetch --prune"
  echo "  (list 'git ls-remote --heads origin' before and after -- the wire, not a"
  echo "   local ref, is what says the remote branch is gone)"
  exit 0
fi

# Tree absent. WHICH KIND of absent decides whether this is a verdict or a
# refusal to answer, and the merge-base is what separates them.
MERGE_BASE="$(git merge-base "$BASE" "$BRANCH")"
if [ "$MERGE_BASE" = "$BASE_TIP" ]; then
  echo "NOT ABSORBED -- STOP, do not delete."
  echo "  The branch tree is not in $BASE's history, and $BASE has NOT moved past"
  echo "  the branch point (merge-base == base tip). Nothing has been merged; this"
  echo "  branch holds work that exists nowhere else."
  exit 1
fi

echo "INCONCLUSIVE -- this check cannot authorize a delete here."
echo "  The branch tree is not in $BASE's history, BUT $BASE has advanced past the"
echo "  merge-base ($(git rev-parse --short "$MERGE_BASE")), by $(git rev-list --count "$MERGE_BASE..$BASE_TIP") commit(s) the branch never saw."
echo
echo "  A squash built on a newer base has tree 'newbase + changes' while this"
echo "  branch tip is 'oldbase + changes'. Those differ even when the work IS"
echo "  merged, so absence here is NOT evidence of unmerged work."
echo
echo "  Confirm from the PR's merged state instead, and SAY which source you used:"
echo "      gh pr view <n> --json state,mergedAt,mergeCommit"
echo "  Do not read this as a STOP, and do not delete on it either."
exit 2
