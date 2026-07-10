#!/usr/bin/env bash
#
# Build the plugin, deploy it to a local Paper server, boot it.
#
#   ./scripts/dev-server.sh            build, deploy, run
#   ./scripts/dev-server.sh --setup    check run/ is ready, don't build
#   ./scripts/dev-server.sh --no-build skip Maven, boot what is deployed
#
# Run from Git Bash or WSL on Windows. Not cmd. Not by double-clicking, which
# closes the window before you can read the error.
#
set -euo pipefail

# Resolve the repo root from THIS FILE's location, not the caller's working
# directory. Without this, run/ lands wherever you happened to be standing.
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Absolute, because --help reads this file back and BASH_SOURCE[0] holds the path
# the caller typed. Relative to their directory, not to the one we are about to
# cd into. Same defect as RUN_DIR had.
SELF="$SCRIPT_DIR/$(basename "${BASH_SOURCE[0]}")"

cd "$REPO_ROOT"

RUN_DIR="run"
PLUGINS_DIR="$RUN_DIR/plugins"

# Keep in step with paper.version in pom.xml. A different build is a different API.
PAPER_BUILD="26.1.2.build.74"
PACKETEVENTS_VERSION="2.13.0"

DO_BUILD=1
SETUP_ONLY=0
for arg in "$@"; do
  case "$arg" in
    --setup)    SETUP_ONLY=1 ;;
    --no-build) DO_BUILD=0 ;;
    -h|--help)  sed -n '2,10p' "$SELF" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *)          echo "Unknown option: $arg" >&2; exit 2 ;;
  esac
done

mkdir -p "$PLUGINS_DIR"

# --- Preflight -------------------------------------------------------------
# Collect every problem, then report together. One at a time means three more
# round trips to discover what you actually need.

problems=()

if ! command -v java >/dev/null 2>&1; then
  problems+=("java is not on PATH. Minecraft 26.1+ requires Java 25.")
fi

if [ ! -f "$RUN_DIR/paper.jar" ]; then
  problems+=("Missing $RUN_DIR/paper.jar
     Download build $PAPER_BUILD (matching paper.version in pom.xml) from
     https://papermc.io/downloads/paper and save it as $RUN_DIR/paper.jar")
fi

# PacketEvents is a runtime dependency, not shaded. paper-plugin.yml declares it
# required, so Paper refuses to load the plugin without it -- and the error it
# prints does not obviously say so.
if ! ls "$PLUGINS_DIR"/packetevents*.jar >/dev/null 2>&1; then
  problems+=("Missing PacketEvents in $PLUGINS_DIR/
     Download the Paper/Spigot build of $PACKETEVENTS_VERSION from
     https://modrinth.com/plugin/packetevents/versions")
fi

if [ ! -f "$RUN_DIR/eula.txt" ]; then
  problems+=("Missing $RUN_DIR/eula.txt
     Read Mojang's EULA at https://aka.ms/MinecraftEULA
     Then create the file containing exactly one line:  eula=true")
elif ! grep -qx 'eula=true' "$RUN_DIR/eula.txt" 2>/dev/null; then
  problems+=("$RUN_DIR/eula.txt does not contain 'eula=true'
     The server will refuse to start. Accepting the EULA is yours to decide.")
fi

if [ "${#problems[@]}" -gt 0 ]; then
  echo
  echo "Cannot start the dev server. ${#problems[@]} thing(s) to fix:"
  echo
  for p in "${problems[@]}"; do
    printf '  * %s\n\n' "$p"
  done
  echo "Re-check with: ./scripts/dev-server.sh --setup"
  exit 1
fi

if [ "$SETUP_ONLY" -eq 1 ]; then
  echo "Preflight passed. $RUN_DIR/ is ready. Run ./scripts/dev-server.sh to boot."
  exit 0
fi

# --- Build -----------------------------------------------------------------

if [ "$DO_BUILD" -eq 1 ]; then
  echo "==> Building"
  # 'clean' is not optional. Maven will not recompile a paper/ file just because
  # a core/ file changed, so an incremental build can miss a non-exhaustive
  # switch over a sealed interface. See ContentValidator's javadoc.
  ./mvnw -q clean package
fi

# find, not `ls | head`: under `set -o pipefail`, a failing ls is a silent trap.
JAR="$(find paper/target -maxdepth 1 -name 'rpg-*.jar' -print -quit 2>/dev/null || true)"

if [ -z "$JAR" ]; then
  echo "ERROR: no plugin jar in paper/target/. Did the build succeed?" >&2
  exit 1
fi

echo "==> Deploying $(basename "$JAR")"
rm -f "$PLUGINS_DIR"/rpg-*.jar
cp "$JAR" "$PLUGINS_DIR/"

# --- Boot ------------------------------------------------------------------

echo "==> Booting Paper (type 'stop' or Ctrl+C to quit)"
echo
cd "$RUN_DIR"
exec java -Xms2G -Xmx2G -jar paper.jar --nogui
