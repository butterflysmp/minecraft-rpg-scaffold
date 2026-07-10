#!/usr/bin/env bash
#
# Assert the built plugin jar is internally consistent.
#
#   ./scripts/check-jar.sh              validate, print a summary
#   ./scripts/check-jar.sh --print-jar  validate, print the jar's path on stdout
#
# Two of this project's failure modes are silent: a stale `main:` in
# paper-plugin.yml gives a clean build, a valid jar, and a server that never
# loads the plugin; stale <artifactSet> coordinates in paper/pom.xml give a
# clean build and a jar with rpg-core missing. Neither is a compile error.
# Both are visible in the jar. So look in the jar.
#
# This script is the ONLY place that decides which file is "the jar".
# dev-server.sh asks it rather than re-deriving it -- two checks that disagree
# about which jar was inspected are worse than one.
#
set -euo pipefail

PRINT_JAR=0
case "${1:-}" in
  --print-jar) PRINT_JAR=1 ;;
  '')          ;;
  *)           echo "Unknown option: $1" >&2; exit 2 ;;
esac

# Resolve the repo root from THIS FILE, not the caller's cwd. Same rule as
# dev-server.sh, for the same reason.
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$(cd "$SCRIPT_DIR/.." && pwd)"

# --- Which jar? -------------------------------------------------------------
# Assert exactly one; do not take the first. `find -print -quit` yields whichever
# entry readdir hands back -- unspecified order, not sorted. A guard whose subject
# is chosen arbitrarily can inspect one jar while the server runs another, and
# pass for a reason unconnected to what is deployed.
#
# A successful `package` leaves exactly one match. Shade writes the shaded jar to
# rpg-paper-<version>-shaded.jar, then renames it over rpg-<version>.jar and
# leaves the pre-shade copy as original-rpg-<version>.jar. Measured on a clean
# build: two jars on disk, one glob match.
#
# The -shaded exclusion is not boilerplate. A build interrupted between shade's
# write and its rename leaves rpg-paper-<version>-shaded.jar on disk, where it
# matches `rpg-*.jar` and is a DIFFERENT jar from the deployed one.
MATCHES=$(find paper/target -maxdepth 1 -name 'rpg-*.jar' ! -name '*-shaded.jar' ! -name 'original-*')
[ "$(printf '%s\n' "$MATCHES" | grep -c .)" -eq 1 ] || {
  echo "::error::expected exactly one plugin jar, found: ${MATCHES:-<none>}" >&2
  echo "         (did the build run? is a stale jar left in paper/target/?)" >&2
  exit 1
}
JAR=$MATCHES

# --- The package root, from the one authoritative source ---------------------
# Deriving it from main: instead (dirname twice) assumes RpgPlugin sits exactly
# two levels below the root. True today; it would silently compute the wrong
# ROOT the day anyone moves it deeper, then fail on a path that never existed.
# Costs a JVM start. That is the price of not having a second source of truth.
GROUP_ID=$(./mvnw -q -pl paper help:evaluate -Dexpression=project.groupId -DforceStdout | tr -d '\r')

# -q suppresses Maven's chatter, but a warning can still land on stdout and be
# captured as the value. ROOT is built by substituting into this string, so a
# garbage GROUP_ID yields a garbage ROOT and assertion 3 then fails for a reason
# that has nothing to do with shade. An unvalidated input to a guard is where the
# next false-green lives.
case "$GROUP_ID" in
  ''|*[!a-z0-9.]*)
    echo "::error::groupId lookup returned garbage: '$GROUP_ID'" >&2; exit 1 ;;
esac
ROOT="${GROUP_ID//./\/}/rpg"

# --- One listing, captured ---------------------------------------------------
# Never `unzip -l "$JAR" | grep -q ...`: grep -q exits on first match and closes
# the pipe, unzip dies of SIGPIPE (141), and `pipefail` hands that 141 to the
# caller. Inside `if ...; then fail; fi` the condition then reads FALSE *because
# the match was found* -- the check passes on exactly the input it exists to
# catch. Measured: 100 lines piped -> status 0; 100000 -> status 141. It is
# jar-size dependent, so it passes today and lies later.
LISTING=$(unzip -l "$JAR")

# tr -d '\r': .gitattributes pins mvnw and *.sh to LF, NOT *.yml, and
# core.autocrlf=true here. A fresh clone checks paper-plugin.yml out as CRLF;
# MAIN then carries \r, ENTRY becomes RpgPlugin\r.class, and this goes red
# locally while staying green on ubuntu-latest.
# (sed and tr read to EOF, so this pipeline has no SIGPIPE hazard.)
MAIN=$(unzip -p "$JAR" paper-plugin.yml | sed -n 's/^main:[[:space:]]*//p' | tr -d '\r')
[ -n "$MAIN" ] || { echo "::error::no main: in the bundled paper-plugin.yml" >&2; exit 1; }

# --- 1. main: agrees with groupId --------------------------------------------
# This is the divergence that made templating main: as ${project.groupId} unsafe.
# Detect it rather than cause it.
# "$GROUP_ID" is quoted, so it is a literal, not a glob -- independent of the
# validation above. (G='a*c'; case abc.Foo in "$G".*) does not match.)
case "$MAIN" in
  "$GROUP_ID".*) ;;
  *) echo "::error::main: is $MAIN but groupId is $GROUP_ID" >&2; exit 1 ;;
esac

# --- 2. main: names a class that is actually in the jar -----------------------
ENTRY="${MAIN//./\/}.class"
case "$LISTING" in
  *"$ENTRY"*) ;;
  *) echo "::error::main: names $MAIN but $ENTRY is not in the jar" >&2; exit 1 ;;
esac

# --- 3. shade actually bundled our own modules --------------------------------
# Assertion 2 cannot catch this: RpgPlugin.class is paper's own and ships whether
# or not core does.
for m in core storage; do
  case "$LISTING" in
    *"$ROOT/$m/"*) ;;
    *) echo "::error::shade dropped rpg-$m -- check paper/pom.xml <artifactSet>" >&2; exit 1 ;;
  esac
done

SUMMARY="Jar OK: $(basename "$JAR") -- $MAIN present under $ROOT; core and storage bundled."

# In --print-jar mode stdout carries the path and nothing else; the summary goes
# to stderr so a caller can safely do JAR="$(check-jar.sh --print-jar)".
if [ "$PRINT_JAR" -eq 1 ]; then
  echo "$SUMMARY" >&2
  printf '%s\n' "$JAR"
else
  echo "$SUMMARY"
fi
