# RPG

Minecraft action RPG. Paper plugin, Maven, Java 25.

    ./mvnw clean package     # build
    ./mvnw -pl core test     # fast unit tests, no server needed
    ./scripts/dev-server.sh  # build + deploy + boot local Paper

Use the wrapper (`./mvnw`, or `mvnw.cmd` on cmd/PowerShell). It pins Maven 3.9.9
and downloads it on first use, so no system Maven install is needed.

Read `CLAUDE.md` before writing code — it documents the banned patterns,
the PacketEvents threading contract, and the module boundaries.
Read `PROJECT.md` for scope and milestones.

## Modules

| Module | Depends on | Purpose |
|---|---|---|
| `core` | *nothing* | Game logic. Abilities, combat, elements. Unit-testable. |
| `storage` | `core`, Gson | `PlayerRepository` port + file implementation. |
| `paper` | `core`, `storage`, Paper API, PacketEvents | Adapters. The only Minecraft-aware module. |

## First run

1. Download a Paper jar matching `paper.version` in `pom.xml` → `run/paper.jar`
2. Download PacketEvents (Paper build) → `run/plugins/`
3. Create `run/eula.txt` containing `eula=true`
4. `./scripts/dev-server.sh`
5. In game: `/rpg abilities`
