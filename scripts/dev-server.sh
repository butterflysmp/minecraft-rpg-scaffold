#!/usr/bin/env bash
# Replaces Gradle's run-paper. Build, deploy, boot, in one command.
set -euo pipefail

RUN_DIR="run"
PAPER_VERSION="26.1.2"

mkdir -p "$RUN_DIR/plugins"

# First run: fetch a Paper jar and accept the EULA yourself.
if [ ! -f "$RUN_DIR/paper.jar" ]; then
  echo "No Paper jar found."
  echo "Download the ${PAPER_VERSION} build matching paper.version in pom.xml:"
  echo "  https://papermc.io/downloads/paper"
  echo "Save it as $RUN_DIR/paper.jar"
  echo
  echo "You must also download PacketEvents (Bukkit/Paper build) into"
  echo "$RUN_DIR/plugins/ -- it is a runtime dependency, not shaded."
  echo "  https://modrinth.com/plugin/packetevents/versions"
  exit 1
fi

if [ ! -f "$RUN_DIR/eula.txt" ]; then
  echo "eula=true not set. Read Mojang's EULA, then create $RUN_DIR/eula.txt"
  echo "containing: eula=true"
  exit 1
fi

echo "==> Building"
mvn -q -T 1C clean package

echo "==> Deploying"
rm -f "$RUN_DIR"/plugins/rpg-*.jar
cp paper/target/rpg-*.jar "$RUN_DIR/plugins/"

echo "==> Booting Paper"
cd "$RUN_DIR"
exec java -Xms2G -Xmx2G -jar paper.jar --nogui
