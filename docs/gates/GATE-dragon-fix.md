# GATE — THE DRAGON STACK'S FIX, AFTER ITS FIRST BOOT: THE STACK TOP's R0, RE-PREDICTED ROWS, AND THE RUN SHEET

**Status: NOT RUN.** Every prediction below was written **before** any boot of `feat/dragon-fix`, and this file is
**committed before the boot**. Readings go **beside** a prediction, never over it, and a prediction is not edited once
its row has been read; a correction is a dated note. **NO BOOT until the seat has diffed the code AND this gate, and Ben
says ready.**

> **2026-11-15: approved to merge by the review seat after the boot of d59d2bf5; see GATE-dragon-fix.md for the readings.**

> **2026-11-16: STEP R3 (PR #193) TOOK EIGHT ROWS OF THIS FILE OUT OF ITS SCENARIOS, BEFORE ANY BOOT OF R3.** From R3 a live Phage within 3 blocks of the player blows. DB10R2 and DQ1 are retired; DS1R2, DS2R2, DS5R2, DS6R2, DG6R2 and DG9R2 are re-predicted in GATE-dragon-blast.md as DS1R3, DS2R3, DS5R3, DS6R3, DG6R3 and DG9R3. Each row carries its own dated note below; **no prediction or reading in this file was changed.** The audit is PLAN-dragon-mobs.md, RULINGS, *THE BLAST AND THE ROWS ALREADY MERGED*.

**THIS PR IS THE STACK TOP.** `feat/dragon-fix` is stacked on `feat/dragon-growth` (#191), on `feat/dragon-split`
(#190), on `feat/dragon-bodies` (#189). The first boot of the stack (`bda8e89c`, 2026-11-15) read most AUTO rows NOT AS
PREDICTED; its readings and dated notes are in the three lower gates, and the record (the seat's misses, the boot's
departures, Ben's readings, Q30) is `PLAN-dragon-mobs.md`, RULINGS, *THE FIRST BOOT OF THE STACK*.

- **The ONE R0 of the next boot is this file's R0a-R0d.** The three lower gates' R0 rows and run sheets are SUPERSEDED
  (a dated note in each says so).
- **Every AUTO row that read NOT AS PREDICTED is re-predicted here as a NEW row named for it** (`DB1R2`, `DS1R2`,
  `DG7R2`, ...). The old predictions and readings stay as they are. **DB8 and DB9 read as predicted and are not re-run.**
- **The scenarios are still `selftest/GATE-dragon-bodies.yml`, `-split.yml` and `-growth.yml`** (so the run sheet keeps
  its three selftest commands), but their keys are THIS file's rows: `SelfTestScenariosTest` pairs the three dragon
  scenario files with this file (`docOf`). That pairing rule is new; the seat may prefer a fourth gate id.

```
ROWS     29   R0a R0b R0c R0d
              DB1R2 DB2R2 DB3R2 DB4R2 DB5R2 DB6R2 DB7R2 DB10R2 DB11R2 DQ1
              DS1R2 DS2R2 DS3R2 DS4R2 DS5R2 DS6R2
              DG1R2 DG2R2 DG3R2 DG4R2 DG5R2 DG6R2 DG7R2 DG8R2 DG9R2
         ──
         29   = 4 R0 + 9 DB + 1 DQ + 6 DS + 9 DG      git grep -c -E '^### ' <ref> -- docs/gates/GATE-dragon-fix.md
```

## GAME MODE AND DIFFICULTY

**SURVIVAL on every row.** `/rpg selftest` refuses outside SURVIVAL. **The difficulty is set by the selftest itself**:
each dragon scenario file's `setup:` begins `difficulty: normal`, the run logs
`DIFFICULTY found=<what the world had> set=NORMAL`, and its one restore puts back what it found at RUN START, on every
exit path (`RESTORED … difficulty=<that>`). **Ben types no `/difficulty` line.** (The seat's third miss: on 2026-11-15
a hostile mob stood beside Ben before the difficulty was set, and he was killed on hard.)

## PART B — THE DIAGNOSES, EACH WITH ITS EVIDENCE AND ITS MARK

The server jar is `run/versions/26.1.2/paper-26.1.2.jar` (build 74); the API jar is
`~/.m2/repository/io/papermc/paper/paper-api/26.1.2.build.74-stable/paper-api-26.1.2.build.74-stable.jar`. Every jar
fact below was read with `C:\Users\Neb91\.jdks\openjdk-26.0.1\bin\javap.exe -c -p -cp <jar> <class>`.

**D1. SPEED — READ.**
- `AttributeInstance#getDefaultValue` returns the ATTRIBUTE's registry default, the same for every mob:
  `org.bukkit.craftbukkit.attribute.CraftAttributeInstance#getDefaultValue` is
  `handle.getAttribute().value().getDefaultValue()`, and `net.minecraft.world.entity.ai.attributes.Attributes`'
  static initialiser registers `movement_speed` as `new RangedAttribute("attribute.name.movement_speed", 0.7d, 0, …)`.
  **So it returns 0.7 for a creeper, a wither skeleton and a spider alike** (and the boot read `speedDefault=0.7000`
  on every BODY line, creeper, wither skeleton, spider, enderman and zombie).
- The TYPE's own base is set in each type's `createAttributes`: `monster.Creeper` `MOVEMENT_SPEED 0.25d`,
  `monster.skeleton.AbstractSkeleton` `0.25d`, `monster.spider.Spider` `0.30000001192092896d` (0.3f widened).
- **The API that reads it:** `org.bukkit.entity.EntityType#getDefaultAttributes()` (API jar) calls
  `UnsafeValues#getDefaultEntityAttributes(key)`, which `CraftMagicNumbers` implements as
  `new UnmodifiableAttributeMap(DefaultAttributes.getSupplier(type))`; its `getAttribute` wraps
  `AttributeSupplier#getAttributeInstance` in `UnmodifiableAttributeInstance`, whose `getBaseValue` (inherited from
  `CraftAttributeInstance`) is `handle.getBaseValue()`: **the supplier's number for the type.** No annotation marks it
  deprecated or experimental (`javap -v`).
- **Why a second apply cannot compound:** the source is the TYPE's supplier, which nothing we write to a live entity
  reaches. A grown Phage (its base already written as 0.25 × 1.5) re-applied at stage 2 is written 0.25 × 1.25, not
  0.375 × 1.25. Pinned by `MobBodiesTest.aGrownBodyIsRescaledFromTheTypesBaseNotFromItsOwn`.

**D2. TAGGING — READ (the deferral), INFERRED (which of the two the selector picked).**
- The row-start clean removes the last row's mobs with `scheduler.onEntity(e, e::remove)`
  (`SelfTestSnapshot.removeTagged`). `PaperScheduler.onEntity` is `entity.getScheduler().run(…)`;
  `io.papermc.paper.threadedregions.scheduler.FoliaEntityScheduler#run` is `runDelayed(…, 1L)` (`lconst_1`), which
  calls `io.papermc.paper.threadedregions.EntityScheduler#schedule`, keyed at `tickCount + delay` in
  `oneTimeDelayed`; `EntityScheduler#executeTick` increments `tickCount` and runs that key's tasks. **So a removal never
  runs inside the player's task that scheduled it**: it runs at the mob's own next `executeTick`.
- The run then calls `advance()` in the SAME task (`SelfTestRun.nextScenario`), so the row's `/rpg spawn` and the
  `/tag @e[type=…,distance=..1.5,sort=nearest,limit=1]` after it ran **while the last row's mob still existed.** Both
  mobs stood at the same spot (every row spawns at the same block, five or three east of the start), so both were inside
  1.5 and at the same distance, about 0. **Which one `sort=nearest` returns on a tie was NOT read** (the selector's
  order is the level's entity lookup). The 2026-11-15 log says it was the OLD one exactly when the last row left a mob
  of the SAME type on the same spot: DB3 (after DB2's creeper), DB4, DB6 (after DB5's wither skeleton), DS2-DS5, DG2,
  DG4, DG6, DG8 failed; DB2 (after a wither skeleton), DB5 (after creepers), DB7 (a spider), DS1 and DG1 (first of their
  gates) succeeded. **The fix does not depend on the tie:** a mob tagged by identity in its own pre-spawn consumer
  leaves nothing to choose between.
- **What a boot would have to show to settle the tie, if anyone wants it:** both uuids in one row, the old mob's
  removal tick and the selector's pick. Not needed for this fix, and not asked.

**D3. DS1 — READ (from the code), and the candidates ruled out.**
- **The cause:** a vanilla iron sword deals NO custom damage to a custom mob, by design. `RpgListeners.onPlayerMeleeAttack`
  sets the vanilla event's damage to `TOKEN_DAMAGE` (0.01) and floors it so it cannot kill, then deals the custom hit
  through `WeaponFire.landVanillaMelee`, which returns empty when `WeaponItems.heldWeaponId` finds no weapon id. An
  untagged iron sword has none. So the Phage stayed at 1/480.
- **Ruled out:** invulnerability ticks (25 ticks passed after `/rpg mobdamage`; vanilla's are 20), the attack cooldown
  (the DRIVE line read `charge=1.000`), reach and facing (`Player#attack` skips the reach check; the aim read
  `d=3.000`), NoAI (a NoAI mob still takes damage; DS2's frozen Phage died to `/kill`).
- **The fix (C4):** DS1R2 attacks with the emberblade, one of our melee weapons, through the same `ATTACK` step;
  `GATE-level.md` WD1 is the precedent for `rpg give emberblade` + `hold: emberblade` + `attack:`.

**D4. THE SWELL — READ (the server), INFERRED (the client).**
- `monster.Creeper#tick`: while the swell direction is positive, `swell += dir`; at `swell >= maxSwell` it sets
  `swell = maxSwell` and calls `explodeCreeper()`. `maxSwell` is 30 (`Creeper.<init>`, `bipush 30`).
- `Creeper#explodeCreeper`, cancelled (`ExplosionPrimeEvent.isCancelled`, offset 44): **`swell = 0` (offset 107) and
  `DATA_IS_IGNITED` set false (offsets 111-121). Nothing else.** `DATA_SWELL_DIR` is not touched, and `swell` itself is
  not a synced field (the synced ones are `DATA_SWELL_DIR`, `DATA_IS_POWERED`, `DATA_IS_IGNITED`).
- **So on the server the swell really resets**: it climbs again from 0, and while the Phage stays close the guard is
  asked again, about every 30 ticks.
- **The client is told nothing about the reset.** It keeps receiving swell direction +1 and animates its own counter
  from it, so (INFERRED: the client's code is not in the server jar) **it shows the Phage held at full swell until the
  direction turns negative, which is when the player moves away.** That is what Ben saw on 2026-11-15 (B6), and the
  prediction "swell, reset, swell" was wrong about the client. It stays cosmetic until R3 replaces the goal.

**D5. DB10's DAMAGE — UNVERIFIED (the attacker), READ (what the log does say).**
- Between DB10's START and DONE the log has `SELF customHP=350.000/350.000 at=-95.50 63.00 4.50` before and
  `SELF customHP=324.545/350.000 at=-102.93 63.00 6.13` after, and three `CREEPERGUARD prime cancelled` lines (two for
  the db10 Phage, one for leaked Phage `5a4bcfd9-…`). **No line names a damage source**: the instrument logs no damage
  to the player. A Phage cannot have done it (its only attack, the blast, was cancelled each time).
- **The only damaging mobs alive were leaks:** the second run's untagged Skreel (DB6's, AI on, which DB7's
  `ENTITIES WITHER_SKELETON count=1` and DB11's count saw), and whatever the first run (on hard) left, which was never
  cleaned between the two runs. **The likeliest attacker is that Skreel**, a wither skeleton, which had killed Ben in
  the first run (`BaronVonYeetus was slain by Skreel`, 21:43:50), and a 7.4-block displacement fits melee knockback.
  **Not proven.** To prove it, a boot would need `/rpg mobtrace` on, or a damage line for the player.

## PART C — WHAT THE FIXES CHANGED, IN BRIEF

1. **Speed:** `MobBodies.BodyAttribute.defaultValue()` is now `typeBase()`; `MobBodyApplier.typeBaseSpeed(entity)`
   reads `EntityType#getDefaultAttributes()`. PROBE_BODY prints it as `speedType=` (the old `speedDefault=` field is
   gone, so no old line can be mistaken for a new one).
2. **Spawn and tag in one step:** the `SPAWN` verb (`"<tag> <frozen|live> <mob> [stage <n>] [<gs>]"`) runs
   `RpgCommand.spawnFor`, `/rpg spawn`'s own path, with the tags and NoAI added inside the pre-spawn consumer. Logged
   `DRIVE spawn tag=<t> <uuid> <mob> frozen|live`. Every dragon scenario uses it.
3. **A row starts clean or says so:** after every row-start clean, `LEFTOVER living=<n> radius=16[ <type uuid>, …]`,
   counted two ticks later (after the removals have run). Every row below predicts `LEFTOVER living=0 radius=16`.
4. **DS1:** killed by the emberblade.
5. **Difficulty:** the `DIFFICULTY` verb, in each dragon setup; recorded at RUN START and restored by the one restore.
6. **ASK:** `"<n> <timeout ticks> <question>"`; logs `ASK <n> question="…" answer="<his line, verbatim>" ticks=<t>`,
   or `ASK <n> question="…" answer=NOT STATED after <timeout> ticks`. Never PASS, FAIL or an interpretation.
7. **Automated:** B5 is DB2R2's and DB4R2's speeds; B6 is DB10R2; S1 and S2 are DS1R2's children and their
   `TARGET … target=self` lines (the new `PROBE_TARGET` verb). Also new: `RETURN` (back to the run's start, for DB10R2's
   second floor count).

**The rows below stand on:** speeds, D1; tags, counts and `LEFTOVER`, D2; DS1R2, D3; DB10R2, D4. Every other number
(scale, height, maxHP, gear score, element, distances, GROWTH and SPLIT shapes) is the one the lower gates predicted and
the 2026-11-15 boot READ, where a row set up correctly, unchanged by this fix.

## CARRIED STATE, DONE BY THE BOOTER BEFORE THE BOOT, IN THIS ORDER

1. **No JVM running:** `Get-Process java -ErrorAction SilentlyContinue` prints nothing.
2. **A fresh dated backup**, with no server running, in ONE bash invocation from the repo root:

   ```bash
   B="/c/Users/Neb91/IdeaProjects/rpg-run-backups/$(date +%F)-pre-dragon-fix"; mkdir "$B" && cp -a run/plugins/Rpg run/world "$B/" && for p in "run/plugins/Rpg $B/Rpg" "run/world $B/world"; do set -- $p; echo "$1: $(find "$1" -type f | wc -l) files $(find "$1" -type f -printf '%s\n' | awk '{s+=$1} END{print s}') bytes; $2: $(find "$2" -type f | wc -l) files $(find "$2" -type f -printf '%s\n' | awk '{s+=$1} END{print s}') bytes; diff -rq lines: $(diff -rq "$1" "$2" | wc -l)"; done
   ```

   If the folder already exists (a boot the same day), add `-2` to its name. Report the folder, both pairs of counts,
   and `diff -rq lines: 0` for each.
3. **Check, do not assume:** `grep -n "dev-pack-on-join" run/plugins/Rpg/config.yml` must print
   `12:  dev-pack-on-join: true`. If it does not, STOP and report.
4. **Boot this branch's head:** `./scripts/dev-server.sh --refresh-content`, announcing "booting now", on Ben's "ready".
5. **R0 binds to THIS boot:** read R0 only on a `latest.log` with a fresh mtime whose `[Rpg] Build:` line names this
   branch's head. **Readings count only after Ben's LOGIN in that boot's log.**

## THE RUN SHEET — BEN TYPES THESE, IN THIS ORDER. EVERY LINE IS LITERAL

1. `/gamemode survival` — **alone, the first line after login.**
2. `/rpg selftest dragon-bodies` — stand still and touch nothing, **except when a gold question appears in chat**: then
   walk as it says, and type your answer (`yes` or `no`) in chat. There are two questions. Wait for the chat to say the
   run finished.
3. `/rpg selftest dragon-split` — stand still; wait for it to finish.
4. `/rpg selftest dragon-growth` — stand still; wait for it to finish.
5. `/stop`

**No cleanup lines:** every mob a row makes is tagged at spawn, and the run's restore removes every tagged mob, split
children included. **No `/difficulty` lines:** the selftest sets and restores it.

## R0 — THE STACK TOP'S BUILD. READ IT FIRST. IF ANY R0 FAILS, STOP

### R0a — the build line names THIS branch's head

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, this PR's head as `git ls-remote origin refs/heads/feat/dragon-fix` prints it, shortened. **Not** `feat/dragon-growth`'s `39bb2e90` or `bda8e89c`. Not `-dirty`, not `unknown` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | **PASS.** `[23:07:25] [Server thread/INFO]: [Rpg] Build: d59d2bf5`; `git ls-remote origin refs/heads/feat/dragon-fix` printed `d59d2bf580100ebc9d52eb224695dea967ba4d02`. latest.log mtime 23:07:26, read 23:07:39 |

### R0b — the jar carries all four steps

| prediction | instrument | READING |
|---|---|---|
| PRESENT: the thirteen entries `GATE-dragon-growth.md` R0b named (R1, R2 and growth), and the fix's `paper/selftest/SelfTestSpawn.class` and `paper/selftest/SelfTestAsk.class`. In `selftest/GATE-dragon-bodies.yml`, `-split.yml` and `-growth.yml`, the line `  - difficulty: normal`; in `-bodies.yml`, the key `DQ1:`. ABSENT: the control `selftest/NoSuchClassControl.class` | the scan below | **PASS.** 15 of 15 entries PRESENT (the thirteen and `SelfTestSpawn.class`, `SelfTestAsk.class`); `PRESENT difficulty dragon-bodies`, `-split`, `-growth`; `PRESENT DQ1:`; `ABSENT  io/github/butterflysmp/rpg/paper/selftest/NoSuchClassControl.class` (the control) |

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path 'run\plugins\rpg-0.1.0-SNAPSHOT.jar'))
foreach ($c in 'selftest/GATE-dragon-bodies.yml',
               'content/mob_elements.yml',
               'content/mobs/phage.yml',
               'io/github/butterflysmp/rpg/paper/health/MobBodyApplier.class',
               'io/github/butterflysmp/rpg/paper/health/CustomCreeperGuard.class',
               'io/github/butterflysmp/rpg/core/mob/MobElementResolver.class',
               'selftest/GATE-dragon-split.yml',
               'io/github/butterflysmp/rpg/paper/health/MobSplitter.class',
               'io/github/butterflysmp/rpg/paper/health/MobSpawner.class',
               'io/github/butterflysmp/rpg/core/mob/MobSplit.class',
               'selftest/GATE-dragon-growth.yml',
               'io/github/butterflysmp/rpg/paper/health/MobGrower.class',
               'io/github/butterflysmp/rpg/core/mob/MobGrowth.class',
               'io/github/butterflysmp/rpg/paper/selftest/SelfTestSpawn.class',
               'io/github/butterflysmp/rpg/paper/selftest/SelfTestAsk.class',
               'io/github/butterflysmp/rpg/paper/selftest/NoSuchClassControl.class') {
  if ($zip.GetEntry($c)) { "PRESENT $c" } else { "ABSENT  $c" }
}
function Text($name) { $r = New-Object IO.StreamReader($zip.GetEntry($name).Open()); $t = $r.ReadToEnd(); $r.Close(); $t }
foreach ($g in 'dragon-bodies', 'dragon-split', 'dragon-growth') {
  if ((Text "selftest/GATE-$g.yml") -match "(?m)^  - difficulty: normal\s*$") { "PRESENT difficulty $g" } else { "ABSENT  difficulty $g" }
}
if ((Text 'selftest/GATE-dragon-bodies.yml') -match "(?m)^DQ1:\s*$") { "PRESENT DQ1:" } else { "ABSENT  DQ1:" }
$zip.Dispose()
```

### R0c — the Loaded line, and nothing refused

| prediction | instrument | READING |
|---|---|---|
| **`[Rpg] Loaded 11 abilities, 29 visuals, 5 statuses, 7 elements, 11 enchants, 3 pools, 6 fragments, 6 aspects, 16 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 5 mobs, 1 mob elements, 3 recipes, 1 ores, 4 vein sizes`**, verbatim as read on 2026-11-15: the fix touches no content file (`git diff --stat 39bb2e90 HEAD -- paper/src/main/resources/content` is empty). No `Refusing`, `Skipping`, `SEVERE` or `Exception`, and no `[Rpg]` `WARN`, beyond `volley_stone`'s known pair (`Content: weapon 'volley_stone' …`, `1 content problem(s)`) | `Select-String run\logs\latest.log -Pattern 'Loaded ','Refusing','Skipping','SEVERE','WARN\]: \[Rpg\]','Exception','Content:'` | **PASS, verbatim.** `[Rpg] Loaded 11 abilities, 29 visuals, 5 statuses, 7 elements, 11 enchants, 3 pools, 6 fragments, 6 aspects, 16 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 5 mobs, 1 mob elements, 3 recipes, 1 ores, 4 vein sizes`; `[Rpg]` WARN only `volley_stone`'s known pair; no Refusing, Skipping, SEVERE or Exception in the whole log |

### R0d — the dev marker reached the JVM, and every scenario loaded

| prediction | instrument | READING |
|---|---|---|
| Exactly one line **`[Rpg] Selftest: dev server, 82 scenarios across 14 gates [melee-cell, level, nexus-polish, legacy-a, legacy-b, wither-status, withered-shortbow, sunder, deft-hands, mining-auto, nexus-back, dragon-bodies, dragon-split, dragon-growth]`**, and **no** `Refusing selftest` line (a malformed spawn, ask or difficulty step is now refused at load, by name) | `Select-String run\logs\latest.log -Pattern 'Selftest:','Refusing selftest'` | **PASS, verbatim.** `[Rpg] Selftest: dev server, 82 scenarios across 14 gates [melee-cell, level, nexus-polish, legacy-a, legacy-b, wither-status, withered-shortbow, sunder, deft-hands, mining-auto, nexus-back, dragon-bodies, dragon-split, dragon-growth]`; no `Refusing selftest` |

**The 82 is DERIVED.** From `paper/src/main/resources/selftest/` at this head: `grep -cE '^  - ' index.yml` prints
**14**; `cat $(grep -E '^  - ' index.yml | tr -d '\r' | sed 's/  - /GATE-/; s/$/.yml/') | grep -cE '^[A-Za-z0-9-]+:'`
prints **96**; the same with `'^setup:'` prints **14**: 96 − 14 = **82**. At `39bb2e90` the same commands print 14, 97,
14: 83, `GATE-dragon-growth.md` R0d's line (the control). `SelfTestScenariosTest` pins the 82.

---

## THE AUTO ROWS — SERVER ONLY, SURVIVAL, DIFFICULTY NORMAL

**The log instrument for every row:**
`Select-String run\logs\latest.log -Pattern 'SELFTEST dragon-|\[Rpg\] GROWTH|\[Rpg\] SPLIT|CREEPERGUARD'`, read after
Ben's LOGIN, inside the row's own `SELFTEST <gate> <row> START … DONE` bracket.

**In EVERY row below (and not repeated in each):** after the row's `CLEAN …` line, **`LEFTOVER living=0 radius=16`**;
and for each mob the row spawns, one `DRIVE spawn tag=<its tag> <uuid> <mob> frozen` (or `live` where the row says),
followed by the command's own `REPLY "Spawned …" [aqua, gray]`. **A non-zero LEFTOVER in any row makes that row NOT AS
PREDICTED**, whatever else it reads, and names what was there.

**A BODY line**, as below, is `BODY tag=<t> <uuid> type=… mob=… stage=… scale=… height=… speedBase=… speedType=…
speedX=… maxHP=… gs=… element=… dead=false`. The uuids are the only fields not predicted.

### DB1R2 — the Knell's body, at the wither skeleton's own speed (SURVIVAL) — re-predicts GATE-dragon-bodies.md DB1

| prediction | READING |
|---|---|
| `BODY tag=db1 <uuid> type=wither_skeleton mob=knell stage=- scale=1.600 height=3.840 speedBase=0.1500 speedType=0.2500 speedX=0.600 maxHP=360.000 gs=100 element=void dead=false` | **AS PREDICTED, verbatim** (`LEFTOVER living=0 radius=16`). `SELFTEST dragon-bodies DB1R2 BODY tag=db1 1c9dfdd9-2610-4b85-8166-9e5c42661e6a type=wither_skeleton mob=knell stage=- scale=1.600 height=3.840 speedBase=0.1500 speedType=0.2500 speedX=0.600 maxHP=360.000 gs=100 element=void dead=false`;  |

### DB2R2 — a Phage, no stage given, is stage 1, at 1.5 × the creeper's speed (SURVIVAL) — re-predicts DB2; B5's AUTO half

| prediction | READING |
|---|---|
| `REPLY "Spawned Phage (creeper, stage 1, 80 HP, GS 100 given)" [aqua, gray]`, then `BODY tag=db2 <uuid> type=creeper mob=phage stage=1 scale=0.588 height=1.000 speedBase=0.3750 speedType=0.2500 speedX=1.500 maxHP=80.000 gs=100 element=void dead=false` | **AS PREDICTED, verbatim** (`LEFTOVER living=0 radius=16`). `SELFTEST dragon-bodies DB2R2 REPLY "Spawned Phage (creeper, stage 1, 80 HP, GS 100 given)" [aqua, gray]`; `SELFTEST dragon-bodies DB2R2 BODY tag=db2 4bcd56ac-942b-440c-9850-ef0897890c38 type=creeper mob=phage stage=1 scale=0.588 height=1.000 speedBase=0.3750 speedType=0.2500 speedX=1.500 maxHP=80.000 gs=100 element=void dead=false`;  |

### DB3R2 — a Phage at stage 3 (SURVIVAL) — re-predicts DB3

| prediction | READING |
|---|---|
| `BODY tag=db3 <uuid> type=creeper mob=phage stage=3 scale=1.176 height=2.000 speedBase=0.2500 speedType=0.2500 speedX=1.000 maxHP=120.000 gs=100 element=void dead=false` | **AS PREDICTED, verbatim** (`LEFTOVER living=0 radius=16`). `SELFTEST dragon-bodies DB3R2 BODY tag=db3 0da4787f-1089-4c30-a543-09f3668e347f type=creeper mob=phage stage=3 scale=1.176 height=2.000 speedBase=0.2500 speedType=0.2500 speedX=1.000 maxHP=120.000 gs=100 element=void dead=false`;  |

### DB4R2 — a Phage at stage 5, at half the creeper's speed (SURVIVAL) — re-predicts DB4; B5's AUTO half

| prediction | READING |
|---|---|
| `BODY tag=db4 <uuid> type=creeper mob=phage stage=5 scale=1.765 height=3.000 speedBase=0.1250 speedType=0.2500 speedX=0.500 maxHP=160.000 gs=100 element=void dead=false`. **With DB2R2, B5's server half:** the stage-1 base is 0.3750 and the stage-5 base 0.1250, three times slower | **AS PREDICTED, verbatim** (`LEFTOVER living=0 radius=16`). `SELFTEST dragon-bodies DB4R2 BODY tag=db4 20fa4e13-da9f-45e1-ab99-257ad9020928 type=creeper mob=phage stage=5 scale=1.765 height=3.000 speedBase=0.1250 speedType=0.2500 speedX=0.500 maxHP=160.000 gs=100 element=void dead=false`;  |

### DB5R2 — the Phalanx's body (SURVIVAL) — re-predicts DB5

| prediction | READING |
|---|---|
| `BODY tag=db5 <uuid> type=wither_skeleton mob=phalanx stage=- scale=1.000 height=2.400 speedBase=0.1625 speedType=0.2500 speedX=0.650 maxHP=240.000 gs=100 element=void dead=false` | **AS PREDICTED, verbatim** (`LEFTOVER living=0 radius=16`). `SELFTEST dragon-bodies DB5R2 BODY tag=db5 f6266e07-2a72-42d9-bfbb-91800a41da91 type=wither_skeleton mob=phalanx stage=- scale=1.000 height=2.400 speedBase=0.1625 speedType=0.2500 speedX=0.650 maxHP=240.000 gs=100 element=void dead=false`;  |

### DB6R2 — the Skreel alone; nothing spawns its mount (SURVIVAL) — re-predicts DB6

| prediction | READING |
|---|---|
| `BODY tag=db6 <uuid> type=wither_skeleton mob=skreel stage=- scale=1.000 height=2.400 speedBase=0.2500 speedType=0.2500 speedX=1.000 maxHP=120.000 gs=100 element=void dead=false`, then `ENTITIES SPIDER count=0 radius=8` | **AS PREDICTED, verbatim** (`LEFTOVER living=0 radius=16`). `SELFTEST dragon-bodies DB6R2 BODY tag=db6 6c5f9c31-8890-4528-8dcb-88344fd43635 type=wither_skeleton mob=skreel stage=- scale=1.000 height=2.400 speedBase=0.2500 speedType=0.2500 speedX=1.000 maxHP=120.000 gs=100 element=void dead=false`; `SELFTEST dragon-bodies DB6R2 ENTITIES SPIDER count=0 radius=8`;  |

### DB7R2 — the mount alone, at 1.3 × the spider's speed, with NO element (SURVIVAL) — re-predicts DB7

| prediction | READING |
|---|---|
| `BODY tag=db7 <uuid> type=spider mob=withered_mount stage=- scale=1.500 height=1.350 speedBase=0.3900 speedType=0.3000 speedX=1.300 maxHP=120.000 gs=100 element=none dead=false`, then `ENTITIES WITHER_SKELETON count=0 radius=8`. (`speedType=0.3000` prints the spider's `0.30000001192092896`; `speedBase` is that × 1.3, `0.39000001549720765`: both executed in jshell) | **AS PREDICTED, verbatim** (`LEFTOVER living=0 radius=16`). `SELFTEST dragon-bodies DB7R2 BODY tag=db7 a7106c3f-a442-4597-a7d3-fccb7d647123 type=spider mob=withered_mount stage=- scale=1.500 height=1.350 speedBase=0.3900 speedType=0.3000 speedX=1.300 maxHP=120.000 gs=100 element=none dead=false`; `SELFTEST dragon-bodies DB7R2 ENTITIES WITHER_SKELETON count=0 radius=8`;  |

### DB10R2 — a Phage beside the player for 100 ticks swells, is refused, and never explodes (SURVIVAL) — re-predicts DB10; B6's AUTO half

> **2026-11-16: RETIRED BY STEP R3, BEFORE ANY BOOT OF IT.** From R3 a live Phage within 3 blocks of the player swells and BLOWS, so "never explodes" is no longer true and this row's steps would read a blast its prediction does not name. **GATE-dragon-blast.md BL1 carries the behaviour now** (a live stage 1 two blocks east swells and blows; the floor is whole). The scenario is gone from selftest/GATE-dragon-bodies.yml; this row's prediction and reading stand above and below as they were.

A stage-1 Phage, AI **ON** (`DRIVE spawn tag=db10 <uuid> phage live`), two blocks east of the player.

| prediction | READING |
|---|---|
| (1) `BLOCKS STONE count=108 box=-3 -1 -4 8 -1 4` before the wait, and the same line after it, taken after `DRIVE return <x y z>` put the player back on the row's block. (2) `BODY tag=db10 <uuid> type=creeper mob=phage stage=1 scale=0.588 height=1.000 speedBase=0.3750 speedType=0.2500 speedX=1.500 maxHP=80.000 gs=100 element=void dead=false`. (3) **At least one** `[Rpg] CREEPERGUARD prime cancelled mob=phage id=<the db10 uuid>` between START and DONE, and **no other creeper's id** in any CREEPERGUARD line in the bracket. **D4:** after each refusal the server's swell is 0 and climbs again, so further lines for the same id are 30 or more ticks apart while it stays close; **the count is a LOWER BOUND, not a prediction of the number** (when it acquires its target is vanilla's random timing); the number seen is recorded. (4) The two `SELF customHP=` readings are EQUAL: the player took no damage. **What the client shows (held at full swell while close) is not a server reading, and no row asks it** (D4; Ben read it on 2026-11-15) | **NOT AS PREDICTED, on (4) only.** (1) as predicted: `BLOCKS STONE count=108 box=-3 -1 -4 8 -1 4` before and after, the second after `DRIVE return -91.43 63.00 5.85`. (2) as predicted. (3) as predicted: 3 x `CREEPERGUARD prime cancelled mob=phage id=f657d82a-55db-4263-ba5b-bd5bec6c14cd`, no other id in the bracket. (4) **NOT EQUAL:** `SELF customHP=166.000/350.000 at=-91.50 63.00 5.50 onGround=true fall=0.000` before and `SELF customHP=176.000/350.000 at=-91.43 63.00 5.85 onGround=true fall=0.000` after. It ROSE by 10: the player started the row below his maximum and regenerated (INFERRED; no damage line exists, and no line lowered it). The prediction's word was "equal", which assumed a full-health start; the claim it stood for (no damage taken) is not contradicted. The row's lines: `SELFTEST dragon-bodies DB10R2 BLOCKS STONE count=108 box=-3 -1 -4 8 -1 4`; `SELFTEST dragon-bodies DB10R2 SELF customHP=166.000/350.000 at=-91.50 63.00 5.50 onGround=true fall=0.000`; `SELFTEST dragon-bodies DB10R2 REPLY "Spawned Phage (creeper, stage 1, 80 HP, GS 100 given)" [aqua, gray]`; `CREEPERGUARD prime cancelled mob=phage id=f657d82a-55db-4263-ba5b-bd5bec6c14cd`; `CREEPERGUARD prime cancelled mob=phage id=f657d82a-55db-4263-ba5b-bd5bec6c14cd`; `CREEPERGUARD prime cancelled mob=phage id=f657d82a-55db-4263-ba5b-bd5bec6c14cd`; `SELFTEST dragon-bodies DB10R2 BODY tag=db10 f657d82a-55db-4263-ba5b-bd5bec6c14cd type=creeper mob=phage stage=1 scale=0.588 height=1.000 speedBase=0.3750 speedType=0.2500 speedX=1.500 maxHP=80.000 gs=100 element=void dead=false`; `SELFTEST dragon-bodies DB10R2 DRIVE return -91.43 63.00 5.85`; `SELFTEST dragon-bodies DB10R2 BLOCKS STONE count=108 box=-3 -1 -4 8 -1 4`; `SELFTEST dragon-bodies DB10R2 SELF customHP=176.000/350.000 at=-91.43 63.00 5.85 onGround=true fall=0.000`;  |

### DB11R2 — the spawn command's two refusals spawn nothing (SURVIVAL) — re-predicts DB11

| prediction | READING |
|---|---|
| `REPLY "'phage' has stages 1 to 5; 6 is not one of them." [red]`, then `REPLY "'knell' has no stages. Spawn it without 'stage'." [red]`, then `ENTITIES CREEPER count=0 radius=8` and `ENTITIES WITHER_SKELETON count=0 radius=8` | **AS PREDICTED, verbatim** (`LEFTOVER living=0 radius=16`). `SELFTEST dragon-bodies DB11R2 ENTITIES CREEPER count=0 radius=8`; `SELFTEST dragon-bodies DB11R2 ENTITIES WITHER_SKELETON count=0 radius=8`; `SELFTEST dragon-bodies DB11R2 REPLY "'phage' has stages 1 to 5; 6 is not one of them." [red]`; `SELFTEST dragon-bodies DB11R2 REPLY "'knell' has no stages. Spawn it without 'stage'." [red]`;  |

### DS1R2 — a stage 5 killed by the PLAYER'S emberblade splits into two stage 1s, which come for him (SURVIVAL) — re-predicts GATE-dragon-split.md DS1; S1's and S2's AUTO halves

> **2026-11-16: SUPERSEDED BY STEP R3, BEFORE ANY BOOT OF IT.** Its two live children three blocks from the player would now blow on him before the TARGET probe (they primed four times in this row's bracket on 2026-11-15). **Re-predicted as GATE-dragon-blast.md DS1R3**, which reads S1 with the children born frozen and S2 from each thawed child's blast. The scenario is gone from selftest/GATE-dragon-split.yml; this row's prediction and reading stand as they were.

| prediction | READING |
|---|---|
| (1) `REPLY "Spawned Phage (creeper, stage 5, 480 HP, GS 300 given)" [aqua, gray]` and `REPLY "CREEPER damaged: 1/480 custom HP" [green]`. (2) `DRIVE attack Player#attack target=<the ds1 uuid> creeper d=3.000 charge=1.000` (d as read on 2026-11-15; a full charge after 40 ticks, the emberblade's being ~12.5, `SelfTestRun`'s javadoc), then **exactly one** `[Rpg] SPLIT mob=phage parent=<the ds1 uuid> stage=5 gs=300 children=2 childStage=1 [<uuid> dx=-0.50, <uuid> dx=0.50]`. (3) `ENTITIES CREEPER count=2 radius=16`. (4) **Exactly two** BODY lines, each `BODY tag=ds1 <uuid> type=creeper mob=phage stage=1 scale=0.588 height=1.000 speedBase=0.3750 speedType=0.2500 speedX=1.500 maxHP=240.000 gs=300 element=void dead=false`. (5) **S2:** sixty ticks later, **exactly two** `TARGET tag=ds1 <uuid> type=creeper target=self dead=false`, one per child uuid. **INFERRED, not read:** that the emberblade's hit takes at least 1 custom health (any positive hit kills at 1); and that each child acquires the player within 90 ticks (vanilla's target goal is random-interval; the children have AI because NoAI is not a tag and is not inherited) | **AS PREDICTED, verbatim** (`LEFTOVER living=0 radius=16`). `SELFTEST dragon-split DS1R2 REPLY "Spawned Phage (creeper, stage 5, 480 HP, GS 300 given)" [aqua, gray]`; `SELFTEST dragon-split DS1R2 REPLY "CREEPER damaged: 1/480 custom HP" [green]`; `SELFTEST dragon-split DS1R2 DRIVE attack Player#attack target=454dd801-1519-4b88-83d1-689113f9c603 creeper d=3.000 charge=1.000`; `SPLIT mob=phage parent=454dd801-1519-4b88-83d1-689113f9c603 stage=5 gs=300 children=2 childStage=1 [ab58fb5a-50ff-451c-ba7f-3925f5cdea66 dx=-0.50, e1d8e386-b4cf-4df4-92ab-f918dcbceeed dx=0.50]`; `SELFTEST dragon-split DS1R2 ENTITIES CREEPER count=2 radius=16`; `SELFTEST dragon-split DS1R2 BODY tag=ds1 ab58fb5a-50ff-451c-ba7f-3925f5cdea66 type=creeper mob=phage stage=1 scale=0.588 height=1.000 speedBase=0.3750 speedType=0.2500 speedX=1.500 maxHP=240.000 gs=300 element=void dead=false`; `SELFTEST dragon-split DS1R2 BODY tag=ds1 e1d8e386-b4cf-4df4-92ab-f918dcbceeed type=creeper mob=phage stage=1 scale=0.588 height=1.000 speedBase=0.3750 speedType=0.2500 speedX=1.500 maxHP=240.000 gs=300 element=void dead=false`; `CREEPERGUARD prime cancelled mob=phage id=ab58fb5a-50ff-451c-ba7f-3925f5cdea66`; `CREEPERGUARD prime cancelled mob=phage id=e1d8e386-b4cf-4df4-92ab-f918dcbceeed`; `CREEPERGUARD prime cancelled mob=phage id=ab58fb5a-50ff-451c-ba7f-3925f5cdea66`; `CREEPERGUARD prime cancelled mob=phage id=e1d8e386-b4cf-4df4-92ab-f918dcbceeed`; `SELFTEST dragon-split DS1R2 TARGET tag=ds1 ab58fb5a-50ff-451c-ba7f-3925f5cdea66 type=creeper target=self dead=false`; `SELFTEST dragon-split DS1R2 TARGET tag=ds1 e1d8e386-b4cf-4df4-92ab-f918dcbceeed type=creeper target=self dead=false`;  |

### DS2R2 — a stage 5 killed by `/kill` splits (SURVIVAL) — re-predicts DS2

> **2026-11-16: SUPERSEDED BY STEP R3, BEFORE ANY BOOT OF IT.** Its two children were live beside the player; from R3 a live Phage blows. **Re-predicted as GATE-dragon-blast.md DS2R3**, the same steps, the children born frozen. The scenario is gone from selftest/GATE-dragon-split.yml; this row's prediction and reading stand as they were.

| prediction | READING |
|---|---|
| `CMD /kill @e[tag=ds2]`, **exactly one** SPLIT line `parent=<the ds2 uuid> stage=5 gs=300 children=2 childStage=1 [<uuid> dx=-0.50, <uuid> dx=0.50]`, then `SETTLED dying=0 ticks=<t> first=1`, `ENTITIES CREEPER count=2 radius=16`, and **exactly two** BODY lines `tag=ds2`, each as DS1R2's (4). `ticks=` is recorded | **AS PREDICTED, verbatim** (`LEFTOVER living=0 radius=16`). `SELFTEST dragon-split DS2R2 REPLY "Spawned Phage (creeper, stage 5, 480 HP, GS 300 given)" [aqua, gray]`; `SPLIT mob=phage parent=2345f96f-5177-4974-a94e-99197c0ca96e stage=5 gs=300 children=2 childStage=1 [996bc823-9ca2-44ed-9129-e5c23520d3d8 dx=-0.50, b3aa2dfc-675a-4121-a098-209d54d35871 dx=0.50]`; `SELFTEST dragon-split DS2R2 SETTLED dying=0 ticks=20 first=1`; `SELFTEST dragon-split DS2R2 ENTITIES CREEPER count=2 radius=16`; `SELFTEST dragon-split DS2R2 BODY tag=ds2 996bc823-9ca2-44ed-9129-e5c23520d3d8 type=creeper mob=phage stage=1 scale=0.588 height=1.000 speedBase=0.3750 speedType=0.2500 speedX=1.500 maxHP=240.000 gs=300 element=void dead=false`; `SELFTEST dragon-split DS2R2 BODY tag=ds2 b3aa2dfc-675a-4121-a098-209d54d35871 type=creeper mob=phage stage=1 scale=0.588 height=1.000 speedBase=0.3750 speedType=0.2500 speedX=1.500 maxHP=240.000 gs=300 element=void dead=false`;  |

### DS3R2 — a stage 3 killed does not split (SURVIVAL) — re-predicts DS3

| prediction | READING |
|---|---|
| `CMD /kill @e[tag=ds3]`, **no** SPLIT line in the bracket, and `ENTITIES CREEPER count=0 radius=16` | **AS PREDICTED, verbatim** (`LEFTOVER living=0 radius=16`). `SELFTEST dragon-split DS3R2 REPLY "Spawned Phage (creeper, stage 3, 360 HP, GS 300 given)" [aqua, gray]`; `SELFTEST dragon-split DS3R2 SETTLED dying=0 ticks=20 first=1`; `SELFTEST dragon-split DS3R2 ENTITIES CREEPER count=0 radius=16`;  |

### DS4R2 — a stage 1 killed does not split (SURVIVAL) — re-predicts DS4

| prediction | READING |
|---|---|
| `CMD /kill @e[tag=ds4]`, **no** SPLIT line in the bracket, and `ENTITIES CREEPER count=0 radius=16` | **AS PREDICTED, verbatim** (`LEFTOVER living=0 radius=16`). `SELFTEST dragon-split DS4R2 REPLY "Spawned Phage (creeper, stage 1, 240 HP, GS 300 given)" [aqua, gray]`; `SELFTEST dragon-split DS4R2 SETTLED dying=0 ticks=20 first=1`; `SELFTEST dragon-split DS4R2 ENTITIES CREEPER count=0 radius=16`;  |

### DS5R2 — once only: two after the split, still two forty ticks later (SURVIVAL) — re-predicts DS5

> **2026-11-16: SUPERSEDED BY STEP R3, BEFORE ANY BOOT OF IT.** Its two live children primed three times between its two counts on 2026-11-15; from R3 each first prime is a blast that removes the Phage, so the second count could not read 2. **Re-predicted as GATE-dragon-blast.md DS5R3**, the same steps, the children born frozen. The scenario is gone from selftest/GATE-dragon-split.yml; this row's prediction and reading stand as they were.

| prediction | READING |
|---|---|
| **Exactly one** SPLIT line in the bracket, and `ENTITIES CREEPER count=2 radius=16` **twice**, forty ticks apart | **AS PREDICTED, verbatim** (`LEFTOVER living=0 radius=16`). `SELFTEST dragon-split DS5R2 REPLY "Spawned Phage (creeper, stage 5, 480 HP, GS 300 given)" [aqua, gray]`; `SPLIT mob=phage parent=c1e5dbfd-03b8-4f14-9c98-8c751eb3a853 stage=5 gs=300 children=2 childStage=1 [43fb8bf6-a75e-485a-9d40-b7f8268c440b dx=-0.50, 775663bd-37a8-480c-bfc1-1e1f9e5d1ae0 dx=0.50]`; `SELFTEST dragon-split DS5R2 SETTLED dying=0 ticks=20 first=1`; `SELFTEST dragon-split DS5R2 ENTITIES CREEPER count=2 radius=16`; `CREEPERGUARD prime cancelled mob=phage id=43fb8bf6-a75e-485a-9d40-b7f8268c440b`; `CREEPERGUARD prime cancelled mob=phage id=775663bd-37a8-480c-bfc1-1e1f9e5d1ae0`; `CREEPERGUARD prime cancelled mob=phage id=43fb8bf6-a75e-485a-9d40-b7f8268c440b`; `SELFTEST dragon-split DS5R2 ENTITIES CREEPER count=2 radius=16`;  |

### DS6R2 — the row-start clean removes DS5R2's children (SURVIVAL) — re-predicts DS6

> **2026-11-16: SUPERSEDED BY STEP R3, BEFORE ANY BOOT OF IT.** It reads DS5R2's children and must follow it, so it moved with it. **Re-predicted as GATE-dragon-blast.md DS6R3.** The scenario is gone from selftest/GATE-dragon-split.yml; this row's prediction and reading stand as they were.

| prediction | READING |
|---|---|
| DS6R2's own `CLEAN blocks=756 removed=2 inventory=kept …` (DS5R2's two children, carrying its tags), then `LEFTOVER living=0 radius=16`, then `ENTITIES CREEPER count=0 radius=16` | **AS PREDICTED, verbatim** `SELFTEST dragon-split DS6R2 CLEAN blocks=756 removed=2 inventory=kept return=-91.43 63.00 5.85`; (`LEFTOVER living=0 radius=16`). `SELFTEST dragon-split DS6R2 ENTITIES CREEPER count=0 radius=16`;  |

### DG1R2 — four Enderman deaths take a stage 1 to stage 5, one stage each (SURVIVAL) — re-predicts GATE-dragon-growth.md DG1

| prediction | READING |
|---|---|
| `BODY tag=dg1 <uuid> type=creeper mob=phage stage=1 scale=0.588 height=1.000 speedBase=0.3750 speedType=0.2500 speedX=1.500 maxHP=240.000 gs=300 element=void dead=false`. Then, after each kill in turn, **exactly one** `[Rpg] GROWTH mob=phage id=<the dg1 uuid> from=N to=N+1 fed=enderman <uuid> element=void d=… max=…` (N = 1 to 4; `max=300`, `360`, `420`, `480`; the FIRST `d=2.00`) and a BODY line for dg1 at stage N+1: stage 2 `scale=0.882 height=1.500 speedBase=0.3125 speedType=0.2500 speedX=1.250 maxHP=300.000`; stage 3 `scale=1.176 height=2.000 speedBase=0.2500 … speedX=1.000 maxHP=360.000`; stage 4 `scale=1.471 height=2.500 speedBase=0.1875 … speedX=0.750 maxHP=420.000`; stage 5 `scale=1.765 height=3.000 speedBase=0.1250 … speedX=0.500 maxHP=480.000`; `speedType=0.2500 gs=300 element=void dead=false` on every line. **No GROWTH line names any other id** | **AS PREDICTED, verbatim** (`LEFTOVER living=0 radius=16`). `SELFTEST dragon-growth DG1R2 REPLY "Spawned Phage (creeper, stage 1, 240 HP, GS 300 given)" [aqua, gray]`; `SELFTEST dragon-growth DG1R2 BODY tag=dg1 00980cc4-3df7-448e-9546-74dc12660438 type=creeper mob=phage stage=1 scale=0.588 height=1.000 speedBase=0.3750 speedType=0.2500 speedX=1.500 maxHP=240.000 gs=300 element=void dead=false`; `GROWTH mob=phage id=00980cc4-3df7-448e-9546-74dc12660438 from=1 to=2 fed=enderman f51f4ec5-e952-4083-b296-26b9309ff7df element=void d=2.00 max=300`; `SELFTEST dragon-growth DG1R2 SETTLED dying=0 ticks=20 first=1`; `SELFTEST dragon-growth DG1R2 BODY tag=dg1 00980cc4-3df7-448e-9546-74dc12660438 type=creeper mob=phage stage=2 scale=0.882 height=1.500 speedBase=0.3125 speedType=0.2500 speedX=1.250 maxHP=300.000 gs=300 element=void dead=false`; `GROWTH mob=phage id=00980cc4-3df7-448e-9546-74dc12660438 from=2 to=3 fed=enderman d85439dc-0d1d-48d5-92ff-3058961e9583 element=void d=2.00 max=360`; `SELFTEST dragon-growth DG1R2 SETTLED dying=0 ticks=20 first=1`; `SELFTEST dragon-growth DG1R2 BODY tag=dg1 00980cc4-3df7-448e-9546-74dc12660438 type=creeper mob=phage stage=3 scale=1.176 height=2.000 speedBase=0.2500 speedType=0.2500 speedX=1.000 maxHP=360.000 gs=300 element=void dead=false`; `GROWTH mob=phage id=00980cc4-3df7-448e-9546-74dc12660438 from=3 to=4 fed=enderman 44965db9-95b9-423e-b831-0fa6db692e8e element=void d=2.00 max=420`; `SELFTEST dragon-growth DG1R2 SETTLED dying=0 ticks=20 first=1`; `SELFTEST dragon-growth DG1R2 BODY tag=dg1 00980cc4-3df7-448e-9546-74dc12660438 type=creeper mob=phage stage=4 scale=1.471 height=2.500 speedBase=0.1875 speedType=0.2500 speedX=0.750 maxHP=420.000 gs=300 element=void dead=false`; `GROWTH mob=phage id=00980cc4-3df7-448e-9546-74dc12660438 from=4 to=5 fed=enderman 1aaa3897-6787-4391-90ec-1035f4698329 element=void d=2.00 max=480`; `SELFTEST dragon-growth DG1R2 SETTLED dying=0 ticks=20 first=1`; `SELFTEST dragon-growth DG1R2 BODY tag=dg1 00980cc4-3df7-448e-9546-74dc12660438 type=creeper mob=phage stage=5 scale=1.765 height=3.000 speedBase=0.1250 speedType=0.2500 speedX=0.500 maxHP=480.000 gs=300 element=void dead=false`;  |

### DG2R2 — one death advances two Phages (SURVIVAL) — re-predicts DG2

| prediction | READING |
|---|---|
| **Exactly two** GROWTH lines in the bracket, `from=1 to=2 … max=300`: one `id=<the dg2a uuid> … d=2.00`, one `id=<the dg2b uuid> … d=3.61`; then a stage-2 BODY line for each tag (`speedBase=0.3125 speedType=0.2500 speedX=1.250 maxHP=300.000 gs=300`) | **AS PREDICTED, verbatim** (`LEFTOVER living=0 radius=16`). `SELFTEST dragon-growth DG2R2 REPLY "Spawned Phage (creeper, stage 1, 240 HP, GS 300 given)" [aqua, gray]`; `SELFTEST dragon-growth DG2R2 REPLY "Spawned Phage (creeper, stage 1, 240 HP, GS 300 given)" [aqua, gray]`; `GROWTH mob=phage id=6e92c079-9e42-4701-ac8c-cda29b9fc5d7 from=1 to=2 fed=enderman 6a7b378e-9340-4a44-b058-0c987e6c96a5 element=void d=2.00 max=300`; `GROWTH mob=phage id=ac5e9365-2fec-434c-badb-519d960034af from=1 to=2 fed=enderman 6a7b378e-9340-4a44-b058-0c987e6c96a5 element=void d=3.61 max=300`; `SELFTEST dragon-growth DG2R2 SETTLED dying=0 ticks=20 first=1`; `SELFTEST dragon-growth DG2R2 BODY tag=dg2a 6e92c079-9e42-4701-ac8c-cda29b9fc5d7 type=creeper mob=phage stage=2 scale=0.882 height=1.500 speedBase=0.3125 speedType=0.2500 speedX=1.250 maxHP=300.000 gs=300 element=void dead=false`; `SELFTEST dragon-growth DG2R2 BODY tag=dg2b ac5e9365-2fec-434c-badb-519d960034af type=creeper mob=phage stage=2 scale=0.882 height=1.500 speedBase=0.3125 speedType=0.2500 speedX=1.250 maxHP=300.000 gs=300 element=void dead=false`;  |

### DG3R2 — 6.05 blocks grows nothing; exactly 6.00 does (SURVIVAL) — re-predicts DG3

| prediction | READING |
|---|---|
| After the 6.05 kill: **no** GROWTH line, and a stage-1 BODY line for dg3 (as DG1R2's first). After the 6.00 kill: **exactly one** GROWTH line `id=<the dg3 uuid> from=1 to=2 … d=6.00 max=300` and a stage-2 BODY line | **AS PREDICTED, verbatim** (`LEFTOVER living=0 radius=16`). `SELFTEST dragon-growth DG3R2 REPLY "Spawned Phage (creeper, stage 1, 240 HP, GS 300 given)" [aqua, gray]`; `SELFTEST dragon-growth DG3R2 SETTLED dying=0 ticks=20 first=1`; `SELFTEST dragon-growth DG3R2 BODY tag=dg3 7bc3866e-b06b-489a-a3f4-6704571c3fc8 type=creeper mob=phage stage=1 scale=0.588 height=1.000 speedBase=0.3750 speedType=0.2500 speedX=1.500 maxHP=240.000 gs=300 element=void dead=false`; `GROWTH mob=phage id=7bc3866e-b06b-489a-a3f4-6704571c3fc8 from=1 to=2 fed=enderman 8ea0ce8c-ef43-417a-8dc0-0dd6175a289b element=void d=6.00 max=300`; `SELFTEST dragon-growth DG3R2 SETTLED dying=0 ticks=20 first=1`; `SELFTEST dragon-growth DG3R2 BODY tag=dg3 7bc3866e-b06b-489a-a3f4-6704571c3fc8 type=creeper mob=phage stage=2 scale=0.882 height=1.500 speedBase=0.3125 speedType=0.2500 speedX=1.250 maxHP=300.000 gs=300 element=void dead=false`;  |

### DG4R2 — a zombie's death grows nothing; an Enderman's does (SURVIVAL) — re-predicts DG4

| prediction | READING |
|---|---|
| After the zombie's kill: **no** GROWTH line, a stage-1 BODY line for dg4. After the Enderman's: **exactly one** GROWTH line `id=<the dg4 uuid> from=1 to=2 fed=enderman <uuid> element=void d=2.00 max=300`, a stage-2 BODY line | **AS PREDICTED, verbatim** (`LEFTOVER living=0 radius=16`). `SELFTEST dragon-growth DG4R2 REPLY "Spawned Phage (creeper, stage 1, 240 HP, GS 300 given)" [aqua, gray]`; `SELFTEST dragon-growth DG4R2 SETTLED dying=0 ticks=20 first=1`; `SELFTEST dragon-growth DG4R2 BODY tag=dg4 ffe9bdc5-e096-4c38-9934-aaf9f3e99340 type=creeper mob=phage stage=1 scale=0.588 height=1.000 speedBase=0.3750 speedType=0.2500 speedX=1.500 maxHP=240.000 gs=300 element=void dead=false`; `GROWTH mob=phage id=ffe9bdc5-e096-4c38-9934-aaf9f3e99340 from=1 to=2 fed=enderman e539605f-f225-4fbe-a86e-99f863a9bfca element=void d=2.00 max=300`; `SELFTEST dragon-growth DG4R2 SETTLED dying=0 ticks=20 first=1`; `SELFTEST dragon-growth DG4R2 BODY tag=dg4 ffe9bdc5-e096-4c38-9934-aaf9f3e99340 type=creeper mob=phage stage=2 scale=0.882 height=1.500 speedBase=0.3125 speedType=0.2500 speedX=1.250 maxHP=300.000 gs=300 element=void dead=false`;  |

### DG5R2 — a Phage's own death grows another (SURVIVAL) — re-predicts DG5

| prediction | READING |
|---|---|
| **No** SPLIT line. **Exactly one** GROWTH line in the bracket, `id=<the dg5a uuid> from=1 to=2 fed=creeper/phage <the dg5b uuid> element=void d=2.00 max=300`, and a stage-2 BODY line for dg5a | **AS PREDICTED, verbatim** (`LEFTOVER living=0 radius=16`). `SELFTEST dragon-growth DG5R2 REPLY "Spawned Phage (creeper, stage 1, 240 HP, GS 300 given)" [aqua, gray]`; `SELFTEST dragon-growth DG5R2 REPLY "Spawned Phage (creeper, stage 3, 360 HP, GS 300 given)" [aqua, gray]`; `GROWTH mob=phage id=121efe11-fbc1-418b-8f04-62ee633a5df7 from=1 to=2 fed=creeper/phage 8f96d202-3b53-499c-8e89-43da31c39095 element=void d=2.00 max=300`; `SELFTEST dragon-growth DG5R2 SETTLED dying=0 ticks=20 first=1`; `SELFTEST dragon-growth DG5R2 BODY tag=dg5a 121efe11-fbc1-418b-8f04-62ee633a5df7 type=creeper mob=phage stage=2 scale=0.882 height=1.500 speedBase=0.3125 speedType=0.2500 speedX=1.250 maxHP=300.000 gs=300 element=void dead=false`;  |

### DG6R2 — a stage 5's death splits it; its children stay at 1, the bystander goes to 3 (SURVIVAL) — re-predicts DG6

> **2026-11-16: SUPERSEDED BY STEP R3, BEFORE ANY BOOT OF IT.** Its two children were live beside the player; from R3 a live Phage blows. **Re-predicted as GATE-dragon-blast.md DG6R3**, the same steps, the children born frozen. The scenario is gone from selftest/GATE-dragon-growth.yml; this row's prediction and reading stand as they were.

| prediction | READING |
|---|---|
| (1) **Exactly one** `SPLIT mob=phage parent=<the dg6p uuid> stage=5 gs=300 children=2 childStage=1 [<uuid> dx=-0.50, <uuid> dx=0.50]`. (2) **Exactly one** GROWTH line in the bracket: `id=<the dg6b uuid> from=2 to=3 fed=creeper/phage <the dg6p uuid> element=void d=2.00 max=360`; **none names either child.** (3) A stage-3 BODY line for dg6b (`scale=1.176 height=2.000 speedBase=0.2500 speedType=0.2500 speedX=1.000 maxHP=360.000 gs=300`). (4) **Exactly two** stage-1 BODY lines for dg6p, `maxHP=240.000 gs=300`. (5) `ENTITIES CREEPER count=3 radius=16` | **AS PREDICTED, verbatim** (`LEFTOVER living=0 radius=16`). `SELFTEST dragon-growth DG6R2 REPLY "Spawned Phage (creeper, stage 2, 300 HP, GS 300 given)" [aqua, gray]`; `SELFTEST dragon-growth DG6R2 REPLY "Spawned Phage (creeper, stage 5, 480 HP, GS 300 given)" [aqua, gray]`; `SPLIT mob=phage parent=627d2381-ab66-452f-ae24-6b46b3337d8e stage=5 gs=300 children=2 childStage=1 [7dfd93ad-db4d-497f-a9cd-3318a147c5d2 dx=-0.50, d14d7e04-f442-4f1a-a6fd-2df9fe7258cd dx=0.50]`; `GROWTH mob=phage id=c847c76f-ccf5-4487-bc7b-8961944a563b from=2 to=3 fed=creeper/phage 627d2381-ab66-452f-ae24-6b46b3337d8e element=void d=2.00 max=360`; `SELFTEST dragon-growth DG6R2 SETTLED dying=0 ticks=20 first=1`; `SELFTEST dragon-growth DG6R2 BODY tag=dg6b c847c76f-ccf5-4487-bc7b-8961944a563b type=creeper mob=phage stage=3 scale=1.176 height=2.000 speedBase=0.2500 speedType=0.2500 speedX=1.000 maxHP=360.000 gs=300 element=void dead=false`; `SELFTEST dragon-growth DG6R2 BODY tag=dg6p 7dfd93ad-db4d-497f-a9cd-3318a147c5d2 type=creeper mob=phage stage=1 scale=0.588 height=1.000 speedBase=0.3750 speedType=0.2500 speedX=1.500 maxHP=240.000 gs=300 element=void dead=false`; `SELFTEST dragon-growth DG6R2 BODY tag=dg6p d14d7e04-f442-4f1a-a6fd-2df9fe7258cd type=creeper mob=phage stage=1 scale=0.588 height=1.000 speedBase=0.3750 speedType=0.2500 speedX=1.500 maxHP=240.000 gs=300 element=void dead=false`; `SELFTEST dragon-growth DG6R2 ENTITIES CREEPER count=3 radius=16`;  |

### DG7R2 — half health before, half of the new maximum after (SURVIVAL) — re-predicts DG7

| prediction | READING |
|---|---|
| `REPLY "CREEPER damaged: 120/240 custom HP" [green]`; then `PROBE tag=dg7 <uuid> creeper customHP=120.000/240.000 …`; then **exactly one** GROWTH line in the bracket, `id=<the dg7 uuid> from=1 to=2 … max=300`; then `PROBE tag=dg7 <uuid> creeper customHP=150.000/300.000 …` (120 of 240 rescaled to 300 reads 150, executed for `GATE-dragon-growth.md`) | **AS PREDICTED, verbatim** (`LEFTOVER living=0 radius=16`). `SELFTEST dragon-growth DG7R2 REPLY "Spawned Phage (creeper, stage 1, 240 HP, GS 300 given)" [aqua, gray]`; `SELFTEST dragon-growth DG7R2 REPLY "CREEPER damaged: 120/240 custom HP" [green]`; `SELFTEST dragon-growth DG7R2 PROBE tag=dg7 ea901070-dd21-4924-bb8a-77a6788fe913 creeper customHP=120.000/240.000 fireTicks=0 dead=false d=3.000 dy=0.000`; `GROWTH mob=phage id=ea901070-dd21-4924-bb8a-77a6788fe913 from=1 to=2 fed=enderman 373bf806-ecca-4f0d-8b6c-8a8cda8c5390 element=void d=2.00 max=300`; `SELFTEST dragon-growth DG7R2 SETTLED dying=0 ticks=20 first=1`; `SELFTEST dragon-growth DG7R2 PROBE tag=dg7 ea901070-dd21-4924-bb8a-77a6788fe913 creeper customHP=150.000/300.000 fireTicks=0 dead=false d=3.000 dy=0.000`;  |

### DG8R2 — a stage 5 beside a death stays 5; a stage 1 goes to 2 (SURVIVAL) — re-predicts DG8

| prediction | READING |
|---|---|
| **Exactly one** GROWTH line in the bracket, `id=<the dg8small uuid> from=1 to=2 … d=3.61 max=300`; **none** naming dg8big. A stage-5 BODY line for dg8big (`maxHP=480.000`), a stage-2 BODY line for dg8small | **AS PREDICTED, verbatim** (`LEFTOVER living=0 radius=16`). `SELFTEST dragon-growth DG8R2 REPLY "Spawned Phage (creeper, stage 5, 480 HP, GS 300 given)" [aqua, gray]`; `SELFTEST dragon-growth DG8R2 REPLY "Spawned Phage (creeper, stage 1, 240 HP, GS 300 given)" [aqua, gray]`; `GROWTH mob=phage id=b3e22158-138d-485f-92bb-71201c8d2b6e from=1 to=2 fed=enderman b403b191-8618-4da9-882e-9a81ca4636a5 element=void d=3.61 max=300`; `SELFTEST dragon-growth DG8R2 SETTLED dying=0 ticks=20 first=1`; `SELFTEST dragon-growth DG8R2 BODY tag=dg8big 517fdea0-ed81-4267-86fe-4cecdd1846ce type=creeper mob=phage stage=5 scale=1.765 height=3.000 speedBase=0.1250 speedType=0.2500 speedX=0.500 maxHP=480.000 gs=300 element=void dead=false`; `SELFTEST dragon-growth DG8R2 BODY tag=dg8small b3e22158-138d-485f-92bb-71201c8d2b6e type=creeper mob=phage stage=2 scale=0.882 height=1.500 speedBase=0.3125 speedType=0.2500 speedX=1.250 maxHP=300.000 gs=300 element=void dead=false`;  |

### DG9R2 — a Phage grown to 5, then killed, splits (SURVIVAL) — re-predicts DG9

> **2026-11-16: SUPERSEDED BY STEP R3, BEFORE ANY BOOT OF IT.** Its two children were live beside the player; from R3 a live Phage blows. **Re-predicted as GATE-dragon-blast.md DG9R3**, the same steps, the children born frozen. The scenario is gone from selftest/GATE-dragon-growth.yml; this row's prediction and reading stand as they were.

| prediction | READING |
|---|---|
| **Exactly one** GROWTH line `id=<the dg9 uuid> from=4 to=5 … d=2.00 max=480`, then a stage-5 BODY line for dg9. After its kill: **exactly one** `SPLIT mob=phage parent=<the dg9 uuid> stage=5 gs=300 children=2 childStage=1 [<uuid> dx=-0.50, <uuid> dx=0.50]`, **exactly two** stage-1 BODY lines for dg9 (`maxHP=240.000 gs=300`), and `ENTITIES CREEPER count=2 radius=16` | **AS PREDICTED, verbatim** (`LEFTOVER living=0 radius=16`). `SELFTEST dragon-growth DG9R2 REPLY "Spawned Phage (creeper, stage 4, 420 HP, GS 300 given)" [aqua, gray]`; `GROWTH mob=phage id=08999397-9c2c-48bb-9f37-dadb96833905 from=4 to=5 fed=enderman 4adf0e69-95e1-4bb0-9f62-69e47ee10465 element=void d=2.00 max=480`; `SELFTEST dragon-growth DG9R2 SETTLED dying=0 ticks=20 first=1`; `SELFTEST dragon-growth DG9R2 BODY tag=dg9 08999397-9c2c-48bb-9f37-dadb96833905 type=creeper mob=phage stage=5 scale=1.765 height=3.000 speedBase=0.1250 speedType=0.2500 speedX=0.500 maxHP=480.000 gs=300 element=void dead=false`; `SPLIT mob=phage parent=08999397-9c2c-48bb-9f37-dadb96833905 stage=5 gs=300 children=2 childStage=1 [2142a415-af2f-453f-ac5e-92fdc3665521 dx=-0.50, 17d3eec5-a3e4-4ac6-95aa-690cf79e7bfc dx=0.50]`; `SELFTEST dragon-growth DG9R2 SETTLED dying=0 ticks=20 first=1`; `SELFTEST dragon-growth DG9R2 BODY tag=dg9 2142a415-af2f-453f-ac5e-92fdc3665521 type=creeper mob=phage stage=1 scale=0.588 height=1.000 speedBase=0.3750 speedType=0.2500 speedX=1.500 maxHP=240.000 gs=300 element=void dead=false`; `SELFTEST dragon-growth DG9R2 BODY tag=dg9 17d3eec5-a3e4-4ac6-95aa-690cf79e7bfc type=creeper mob=phage stage=1 scale=0.588 height=1.000 speedBase=0.3750 speedType=0.2500 speedX=1.500 maxHP=240.000 gs=300 element=void dead=false`; `SELFTEST dragon-growth DG9R2 ENTITIES CREEPER count=2 radius=16`;  |

---

## BEN'S ROWS — BY THE ASK STEP, INSIDE `/rpg selftest dragon-bodies`. EACH ANSWER IS ONE WORD

**Sizes, the split and growth were read by Ben on 2026-11-15 and are NOT asked again.** With the speed fixed, only
the feel of the two speeds is his.

### DQ1 — the big Phage is very slow; the small one is fast but can be backed away from (SURVIVAL)

> **2026-11-16: RETIRED BY STEP R3, BEFORE ANY BOOT OF IT.** Ben answered both questions on 2026-11-15 (below). From R3 its two live Phages would blow on him while he walks away and back, so it is not re-asked. Nothing carries it now: the speeds are DB2R2's and DB4R2's BODY lines, and the feel is his two answers. The scenario is gone from selftest/GATE-dragon-bodies.yml.

A stage-5 and a stage-1 Phage, AI **ON** (`DRIVE spawn tag=dq1big … live`, `DRIVE spawn tag=dq1small … live`), five
blocks east of him and three apart. Each question waits 1200 ticks (one minute) for his next chat line.

| prediction | READING |
|---|---|
| `ASK 1 question="Walk away from the two Phages for a few seconds, then look back. Is the BIG Phage very slow?" answer="yes" ticks=<t>` | Ben, by the ASK step: `SELFTEST dragon-bodies DQ1 ASK 1 question="Walk away from the two Phages for a few seconds, then look back. Is the BIG Phage very slow?" answer="yes" ticks=179` |
| `ASK 2 question="Is the SMALL Phage fast, but still possible to back away from?" answer="yes" ticks=<t>` | Ben, by the ASK step: `SELFTEST dragon-bodies DQ1 ASK 2 question="Is the SMALL Phage fast, but still possible to back away from?" answer="yes" ticks=88` |

**The answer cell is his line, verbatim.** "not sure", or no line before the minute is out
(`answer=NOT STATED after 1200 ticks`), is recorded as NOT STATED, never re-asked in this boot.

---

## READINGS NOTE, 2026-11-15: THE BOOT OF `d59d2bf5`

**Booted** 23:07 on Ben's "READY"; backup `rpg-run-backups/2026-11-15-pre-dragon-fix` (Rpg 129 files 378063 bytes,
world 193 files 204963726 bytes, `diff -rq lines: 0` each); `12:  dev-pack-on-join: true`. LOGIN 23:09:00. Ben typed
exactly the run sheet: `/gamemode survival`, the three selftests, `/stop`. **Stop clean** at 23:11:10 (`Stopping
server`, `Saving worlds`, `All dimensions are saved`; no java process left). **R0a-R0d PASS.** No prediction above was
edited.

**Each run's difficulty and restore:** every setup read `DIFFICULTY found=NORMAL set=NORMAL` (the world was already
normal, from the last boot's `/difficulty normal`), and every `RESTORED … difficulty=NORMAL`. **So the set-and-restore
was exercised but not DISCRIMINATED**: found and set were the same value. A run started from another difficulty is
what would show the restore putting back something different.

**Every LEFTOVER line read `living=0 radius=16`**, in all 25 rows and all three setups.

**28 of the 29 rows read AS PREDICTED, verbatim. DB10R2 did not, on its (4) only:** Ben's custom health was below its
maximum when the row began (`166.000/350.000`) and read `176.000/350.000` after 100 ticks. It rose; nothing lowered it,
and no line in the bracket is a damage line. The prediction said "equal", written on the assumption of a full-health
start (DB10 read `350.000/350.000` on 2026-11-15). **INFERRED: regeneration.** The claim (4) stood for, that the player
took no damage, is not contradicted, but the row is recorded NOT AS PREDICTED because the predicted figure was not read.

**The two inferred facts DS1R2 stood on were read:** the emberblade's hit killed the Phage at 1/480 (`Phage was slain by
BaronVonYeetus using [Emberblade]`, then the SPLIT line), and both children targeted the player within the 90 ticks
(`target=self`, twice).
