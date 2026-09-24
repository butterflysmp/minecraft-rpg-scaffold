---
paths:
  - "pom.xml"
  - "**/pom.xml"
---

## Upgrade procedure

Do **not** bump `paper.version` alone. Order of operations:

0. **Notice the release. Nothing does this for you.** There is no bot on this
   repo, by decision — see `NEXT.md` D4. Check
   <https://modrinth.com/plugin/packetevents/versions> yourself. An absent
   notification looks exactly like nothing to notify, and this step is the one
   that silently never happens.
1. Check PacketEvents supports the new Minecraft drop. It typically lags a
   Minecraft release by 1–2 weeks. It is the gate.
2. Bump `packetevents.version` first, confirm it builds.
3. Bump `paper.version`.
4. Run `./mvnw -pl core test`. If `core` tests break on a Paper bump, `core` has an
   illegal dependency — that is the real bug.
5. Boot `./scripts/dev-server.sh` and smoke-test one ability end to end.

Never use version ranges. Pin exact builds so the build is reproducible.
