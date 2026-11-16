# GATE — DRAGON BODIES (dragon-mobs step R1): the four mobs' bodies, the Phage's five stages, and the element of a creature

**Status: NOT RUN.** Every prediction below was written **before** any boot of `feat/dragon-bodies`, and this file is
**committed before the boot**. Readings go **beside** a prediction, never over it, and a prediction is not edited once
its row has been read. **NO BOOT until the seat has diffed the code AND this gate, and Ben says ready.**

> **NOTE, 2026-11-15 (added on `feat/dragon-split`, step R2, before any boot; no prediction below is changed).** This
> gate will be read in a **STACKED BOOT** of `feat/dragon-split`'s head, which carries this step and the next. In
> that boot **this file's R0a-R0d are SUPERSEDED** by `GATE-dragon-split.md`'s R0 (its R0a names that branch's head,
> not this one's, and its R0b-R0d cover both steps), and **this file's run sheet is replaced** by
> `GATE-dragon-split.md`'s, which runs this gate's selftest and B1-B6 in it. **DB1-DB11 and B1-B6 are read against
> the predictions in THIS file, unchanged.** One consequence for DB2-DB4's `maxHP`: unchanged, because R2 adds a
> `split:` block to `phage.yml` and touches no stage body.

> **NOTE, 2026-11-15 (added on `feat/dragon-growth`, the plan's step R4, before any boot; no prediction below is
> changed).** The stacked boot is now of `feat/dragon-growth`'s head. **This file's R0a-R0d and its run sheet are
> SUPERSEDED in that boot by `GATE-dragon-growth.md`'s** (which also supersede `GATE-dragon-split.md`'s). DB1-DB11 and
> B1-B6 are still read against THIS file's predictions. **Checked row by row against growth:** no DB row kills
> anything, so growth changes none of their readings. **A defect in the B rows, found on this branch: B2's and B6's
> cleanup `/kill @e[type=creeper,distance=..20]` each kill a stage-5 Phage, which since R2 SPLITS and leaves two live
> small Phages.** The rows were written before the split existed, and `GATE-dragon-split.md`'s note missed it. The
> stacked run sheet in `GATE-dragon-growth.md` has Ben type each of those two cleanup lines twice. No prediction here
> rests on the cleanup.

**Plan:** `PLAN-dragon-mobs.md` — RULINGS (Ben's R1 answers and the seat's rulings 8-10), §3.R.0 (the element on a
mob), §4.2 (MB1 limited, N1, N2) and §5.R step R1. **The rule every AUTO row answers to:**
`.claude/rules/verification.md`, *AN AUTO READING IS A READING OF THE SERVER, NEVER OF THE CLIENT*.

```
ROWS     21   R0a R0b R0c R0d
              DB1 DB2 DB3 DB4 DB5 DB6 DB7 DB8 DB9 DB10 DB11
              B1 B2 B3 B4 B5 B6
         ──
         21   = 4 R0 + 11 DB + 6 B      git grep -c -E '^### ' <ref> -- docs/gates/GATE-dragon-bodies.md
```

The eleven `DB` rows are the scenarios in `paper/src/main/resources/selftest/GATE-dragon-bodies.yml`, added LAST in
`selftest/index.yml`; `SelfTestScenariosTest` pins that each scenario key is a `### <id> ` heading here. The six `B`
rows are Ben's, by sight.

## WHAT R1 BUILDS, AND WHAT IT DOES NOT

- **Built:** a mob file may author `scale` (the value of `SCALE`), `speed` (a MULTIPLIER of the base type's DEFAULT
  movement speed) and `element` (an id in `content/elements/`; unknown is refused at load). A staged mob authors
  `stages:` (count, and the first and last stage's height in blocks, speed and health), interpolated linearly. The
  vanilla table `content/mob_elements.yml` holds one row, `enderman: void`. `/rpg spawn <mob> stage <n> [gs]` places
  a staged mob at a stage (default 1). Our creepers never detonate the vanilla way.
- **NOT built, and no row tests it:** growth, the split, any explosion of ours, the beam, the teleport, the
  projectile, the shield, the blink, the leap, gear, pairing the Skreel with its mount, the Wither question, and
  hostility between mobs. **The Phalanx has no bow and melees like a bare wither skeleton; the Knell is unarmed.**

## GAME MODE

**SURVIVAL on every row.** `/rpg selftest` refuses outside SURVIVAL (`GATE-selftest.md` ST7). DB10 and B6 need it on
their own account: vanilla's `SwellGoal` stops the swell against a creative or spectator target
(`EntitySelector.NO_CREATIVE_OR_SPECTATOR`, PLAN-dragon-mobs.md §7 F-C), so in creative a Phage never swells and
DB10 would pass while testing nothing. **And the difficulty must not be PEACEFUL**: on peaceful, monsters are removed
and a creeper never targets.

## THE JAR FACTS THE PREDICTIONS STAND ON (javap, Paper 26.1.2 build 74, read 2026-11-15)

Server jar `run/versions/26.1.2/paper-26.1.2.jar` (MANIFEST `Implementation-Version: 26.1.2-74-e4e17fc`,
`Build-Number: 74`), read with `C:\Users\Neb91\.jdks\openjdk-26.0.1\bin\javap.exe -c -p -cp <jar> <class>`; API
source from `paper-api-26.1.2.build.74-stable-sources.jar` (`jar xf`).

| fact | class#member |
|---|---|
| base sizes (width, height): creeper `0.6, 1.7`; wither skeleton `0.7, 2.4`; spider `1.4, 0.9`; enderman `0.6, 2.9`; zombie `0.6, 1.95` | `EntityType.<clinit>`, `sized(FF)` after each `ldc` name |
| a living entity's dimensions are the type's, times `getScale()`; `getScale()` is the `SCALE` attribute's value, `d2f` | `LivingEntity#getDimensions`, `LivingEntity#getScale` |
| a change to `SCALE` on a live entity calls `refreshDimensions()` | `LivingEntity#onAttributeUpdated` |
| an entity's dimensions are set from its TYPE in its constructor, so in the pre-spawn consumer `getHeight()` is the unscaled height | `Entity.<init>` (`EntityType.getDimensions`, `putfield dimensions`) |
| `Entity#getHeight()` is `getBbHeight()` | `CraftEntity#getHeight` |
| `SCALE` and `MOVEMENT_SPEED` are in every living entity's attributes | `LivingEntity#createLivingAttributes` |
| default movement speed: skeletons `0.25d`, creeper `0.25d`, spider `0.30000001192092896d` (*INFERRED* that `WitherSkeleton` takes `AbstractSkeleton`'s) | `AbstractSkeleton#createAttributes`, `Creeper#createAttributes`, `Spider#createAttributes` |
| the fuse is 30 ticks (`maxSwell`); a CANCELLED `ExplosionPrimeEvent` leaves the creeper alive, swell 0, ignited false | `Creeper.<init>`, `Creeper#explodeCreeper` |
| flint and steel / a fire charge (`ItemTags.CREEPER_IGNITERS`) call `ignite()` → `setIgnited(true)` → `CreeperIgniteEvent` | `Creeper#mobInteract`, `Creeper#setIgnited` |
| lightning fires `CreeperPowerEvent` (cancellable) and CHARGES the creeper; it does not ignite it | `Creeper#thunderHit` |

**Every figure in the predictions below was EXECUTED, not predicted** (jshell, on the JDK above): the height is
`(float) typeHeight * (float) scale` printed `%.3f`; the Phage's scale is `height / (double) 1.7f`; `speedX` is
`base / default` printed `%.3f`; `speedBase` and `speedDefault` are printed `%.4f`; `maxHP` is the custom store's
max printed `%.3f`. At gear score 100, `maxHP` is the authored health × 1.00 (PLAN-mob-scaling.md M4).

## CARRIED STATE, DONE BY THE BOOTER BEFORE THE BOOT, IN THIS ORDER

1. **No JVM running:** `Get-Process java -ErrorAction SilentlyContinue` prints nothing.
2. **A fresh dated backup**, with no server running, in ONE bash invocation from the repo root:

   ```bash
   B="/c/Users/Neb91/IdeaProjects/rpg-run-backups/$(date +%F)-pre-dragon-bodies"; mkdir "$B" && cp -a run/plugins/Rpg run/world "$B/" && for p in "run/plugins/Rpg $B/Rpg" "run/world $B/world"; do set -- $p; echo "$1: $(find "$1" -type f | wc -l) files $(find "$1" -type f -printf '%s\n' | awk '{s+=$1} END{print s}') bytes; $2: $(find "$2" -type f | wc -l) files $(find "$2" -type f -printf '%s\n' | awk '{s+=$1} END{print s}') bytes; diff -rq lines: $(diff -rq "$1" "$2" | wc -l)"; done
   ```

   Report the folder, both pairs of counts, and `diff -rq lines: 0` for each.
3. **Check, do not assume:** `grep -n "dev-pack-on-join" run/plugins/Rpg/config.yml` must print
   `12:  dev-pack-on-join: true`. If it does not, STOP and report; do not edit it without the seat.
4. **Boot:** `./scripts/dev-server.sh --refresh-content`. Only the booter boots, announcing "booting now", on Ben's
   "ready". **`--refresh-content` is required**: the five mob files and `mob_elements.yml` are new or changed, and
   `saveResource` never overwrites.
5. **R0 binds to THIS boot:** read R0 only on a `latest.log` with a fresh mtime whose `[Rpg] Build:` line names this
   PR's head. **Readings count only after Ben's LOGIN in that boot's log.** Ben ends with `/stop`.

## THE RUN SHEET — BEN TYPES THESE, IN THIS ORDER. EVERY LINE IS LITERAL; NOTHING IS FILLED IN

1. `/gamemode survival` — **alone, the first line after login.**
2. `/difficulty` — read it out; the booter records it.
3. `/difficulty normal`
4. `/rpg selftest dragon-bodies` — wait for the run to finish (the chat says so). **Stand still; do not touch
   anything while it runs.**
5. Ben's rows, B1 to B6, below, in order, each with its own lines.
6. `/difficulty` — read it out again.
7. `/stop`

## R0 — THIS PR'S BUILD. READ IT FIRST. IF ANY R0 FAILS, STOP

`GATE-nexus-back.md`'s R0 rows (the last gate on master) are SUPERSEDED at this boot, never PASS or FAIL.

### R0a — the build line names this PR's head

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, this PR's head as `git ls-remote origin refs/heads/feat/dragon-bodies` prints it, shortened. Not `-dirty`, not `unknown` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | _(not run)_ |

### R0b — the jar carries this branch

| prediction | instrument | READING |
|---|---|---|
| PRESENT: `selftest/GATE-dragon-bodies.yml`, `content/mob_elements.yml`, `content/mobs/phage.yml`, paper `health/MobBodyApplier.class`, paper `health/CustomCreeperGuard.class`, core `mob/MobElementResolver.class`; in `selftest/index.yml`, the line `  - dragon-bodies`. ABSENT: the control `selftest/NoSuchClassControl.class` | the scan below | _(not run)_ |

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path 'run\plugins\rpg-0.1.0-SNAPSHOT.jar'))
foreach ($c in 'selftest/GATE-dragon-bodies.yml',
               'content/mob_elements.yml',
               'content/mobs/phage.yml',
               'io/github/butterflysmp/rpg/paper/health/MobBodyApplier.class',
               'io/github/butterflysmp/rpg/paper/health/CustomCreeperGuard.class',
               'io/github/butterflysmp/rpg/core/mob/MobElementResolver.class',
               'io/github/butterflysmp/rpg/paper/selftest/NoSuchClassControl.class') {
  if ($zip.GetEntry($c)) { "PRESENT $c" } else { "ABSENT  $c" }
}
$r = New-Object IO.StreamReader($zip.GetEntry('selftest/index.yml').Open()); $idx = $r.ReadToEnd(); $r.Close()
if ($idx -match '(?m)^  - dragon-bodies\s*$') { "PRESENT index dragon-bodies" } else { "ABSENT  index dragon-bodies" }
$zip.Dispose()
```

### R0c — the Loaded line, and nothing refused

| prediction | instrument | READING |
|---|---|---|
| **`[Rpg] Loaded 11 abilities, 29 visuals, 5 statuses, 7 elements, 11 enchants, 3 pools, 6 fragments, 6 aspects, 16 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 5 mobs, 1 mob elements, 3 recipes, 1 ores, 4 vein sizes`**. Derived from `GATE-nexus-back.md` R0c's reading (`… 6 accessories, 1 mobs, 3 recipes …`), changed in exactly two places: **`5 mobs`** (knell, phage, phalanx, skreel, withered_mount) and the new **`1 mob elements`**, because this branch's content diff is `content/mobs/` and `content/mob_elements.yml` only (`git diff --stat 5b71e24b HEAD -- paper/src/main/resources/content`). No `Refusing`, `Skipping`, `SEVERE` or `Exception`, and no `[Rpg]` `WARN`, beyond `volley_stone`'s known pair (`Content: weapon 'volley_stone' …`, `1 content problem(s)`); no `Skipping malformed mob`; no `Refusing row` | `Select-String run\logs\latest.log -Pattern 'Loaded ','Refusing','Skipping','SEVERE','WARN\]: \[Rpg\]','Exception','Content:'` | _(not run)_ |

### R0d — the dev marker reached the JVM, and every scenario loaded

| prediction | instrument | READING |
|---|---|---|
| Exactly one line **`[Rpg] Selftest: dev server, 68 scenarios across 12 gates [melee-cell, level, nexus-polish, legacy-a, legacy-b, wither-status, withered-shortbow, sunder, deft-hands, mining-auto, nexus-back, dragon-bodies]`**, and **no** `Refusing selftest` line | `Select-String run\logs\latest.log -Pattern 'Selftest:','Refusing selftest'` | _(not run)_ |

**The 68 and the 12 are DERIVED.** From the repo root at this head:

```bash
cd paper/src/main/resources/selftest
grep -cE '^  - ' index.yml                                   # gates: 12
cat $(grep -E '^  - ' index.yml | tr -d '\r' | sed 's/  - /GATE-/; s/$/.yml/') | grep -cE '^[A-Za-z0-9-]+:'   # keys: 80
cat $(grep -E '^  - ' index.yml | tr -d '\r' | sed 's/  - /GATE-/; s/$/.yml/') | grep -cE '^setup:'           # setups: 12
```

Printed when this was written: **12, 80, 12**, so 80 − 12 = **68**. The same counts at master `5b71e24b` print 11, 68
and 11: 57, the line `GATE-nexus-back.md` R0d predicted (the control). `SelfTestScenariosTest` pins the 68.

---

## THE INSTRUMENT'S ROWS — AUTO, SERVER ONLY

**The log instrument for every row:** `Select-String run\logs\latest.log -Pattern 'SELFTEST dragon-bodies|CREEPERGUARD'`,
read after Ben's LOGIN, inside the row's own `SELFTEST dragon-bodies <row> START … DONE` bracket. A `BODY` line is
`SELFTEST dragon-bodies <row> BODY tag=<t> <uuid> type=… mob=… stage=… scale=… height=… speedBase=… speedDefault=…
speedX=… maxHP=… gs=… element=… dead=…`; `<uuid>` is the only field not predicted. **Every row is AUTO for its log
half only. No row here covers what Ben SEES**: the size and the walk on screen are his rows, B1-B5.

**What the driver skips** (each a thing Ben's typing does and the instrument does not): `CMD` is `performCommand`,
which skips `PlayerCommandPreprocessEvent` and the "issued server command" log line, so each `CMD` line is the
instrument's own. NoAI is set on DB1-DB7's mobs after their spawn, which touches no attribute.

### DB1 — the Knell's body (SURVIVAL) — witness: the log

| prediction | READING |
|---|---|
| `BODY tag=db1 <uuid> type=wither_skeleton mob=knell stage=- scale=1.600 height=3.840 speedBase=0.1500 speedDefault=0.2500 speedX=0.600 maxHP=360.000 gs=100 element=void dead=false` | _(not run)_ |

### DB2 — a Phage, no stage given, is stage 1 (SURVIVAL) — witness: the log

| prediction | READING |
|---|---|
| `REPLY "Spawned Phage (creeper, stage 1, 80 HP, GS 100 given)" [aqua, gray]` (the root is aqua, the name uncoloured, the bracket gray: `RpgCommand.spawnMob` and `SelfTestLines.collect`), and `BODY tag=db2 <uuid> type=creeper mob=phage stage=1 scale=0.588 height=1.000 speedBase=0.3750 speedDefault=0.2500 speedX=1.500 maxHP=80.000 gs=100 element=void dead=false` | _(not run)_ |

### DB3 — a Phage at stage 3 (SURVIVAL) — witness: the log

| prediction | READING |
|---|---|
| `BODY tag=db3 <uuid> type=creeper mob=phage stage=3 scale=1.176 height=2.000 speedBase=0.2500 speedDefault=0.2500 speedX=1.000 maxHP=120.000 gs=100 element=void dead=false` | _(not run)_ |

### DB4 — a Phage at stage 5 (SURVIVAL) — witness: the log

| prediction | READING |
|---|---|
| `BODY tag=db4 <uuid> type=creeper mob=phage stage=5 scale=1.765 height=3.000 speedBase=0.1250 speedDefault=0.2500 speedX=0.500 maxHP=160.000 gs=100 element=void dead=false` | _(not run)_ |

**DB2 to DB4 together are the interpolation, read live:** height 1, 2, 3; speedX 1.5, 1.0, 0.5; health 80, 120, 160.
`MobStagesTest` holds all five stages exactly; these three are its server-side witness. **And the three maxHP values
differ, so the seed read each mob's own stage**: a seed that ignored the stage would print `maxHP=80.000` in all three
(`DragonBodiesWiringTest.theSeedReadsTheStage`, and the mutation `MUTSEEDSTAGE` that reddens it).

### DB5 — the Phalanx's body (SURVIVAL) — witness: the log

| prediction | READING |
|---|---|
| `BODY tag=db5 <uuid> type=wither_skeleton mob=phalanx stage=- scale=1.000 height=2.400 speedBase=0.1625 speedDefault=0.2500 speedX=0.650 maxHP=240.000 gs=100 element=void dead=false` | _(not run)_ |

### DB6 — the Skreel alone; nothing spawns its mount (SURVIVAL) — witness: the log

| prediction | READING |
|---|---|
| `BODY tag=db6 <uuid> type=wither_skeleton mob=skreel stage=- scale=1.000 height=2.400 speedBase=0.2500 speedDefault=0.2500 speedX=1.000 maxHP=120.000 gs=100 element=void dead=false`, then `ENTITIES SPIDER count=0 radius=8` | _(not run)_ |

### DB7 — the mount alone, with NO element; nothing spawns a rider (SURVIVAL) — witness: the log

| prediction | READING |
|---|---|
| `BODY tag=db7 <uuid> type=spider mob=withered_mount stage=- scale=1.500 height=1.350 speedBase=0.3900 speedDefault=0.3000 speedX=1.300 maxHP=120.000 gs=100 element=none dead=false`, then `ENTITIES WITHER_SKELETON count=0 radius=8` | _(not run)_ |

### DB8 — a vanilla Enderman is Void by the table (SURVIVAL) — witness: the log

| prediction | READING |
|---|---|
| `BODY tag=db8 <uuid> type=enderman mob=- stage=- … element=void dead=false`. Only `type`, `mob`, `stage` and `element` are predicted: a summoned Enderman's other fields are vanilla's, not this slice's | _(not run)_ |

### DB9 — a vanilla zombie has no element (SURVIVAL) — witness: the log

| prediction | READING |
|---|---|
| `BODY tag=db9 <uuid> type=zombie mob=- stage=- … element=none dead=false`. As DB8, only those four fields are predicted | _(not run)_ |

### DB10 — a Phage beside the player for 100 ticks swells and never explodes (SURVIVAL) — witness: the log

A stage-1 Phage, AI ON, spawned two blocks east of the player; 100 ticks is more than three vanilla fuses of 30.

| prediction | READING |
|---|---|
| (1) `BLOCKS STONE count=108 box=-3 -1 -4 8 -1 4` **before and after** the wait: the floor is 12 × 9 blocks (x from −3 to 8, z from −4 to 4), and no block broke. (2) `BODY tag=db10 <uuid> type=creeper mob=phage stage=1 scale=0.588 height=1.000 speedBase=0.3750 speedDefault=0.2500 speedX=1.500 maxHP=80.000 gs=100 element=void dead=false`: alive after the wait. (3) **At least one** `[Rpg] CREEPERGUARD prime cancelled mob=phage id=<the db10 uuid>` between DB10's START and DONE: the fuse completed and was refused, so the row did not pass by the Phage never swelling. **The count is a LOWER BOUND, not a prediction of the number** (when the creeper acquires its target is vanilla's random goal timing); the number seen is recorded. (4) The two `SELF customHP=` readings are equal: the player took no damage. **Zero `CREEPERGUARD` lines makes this row NOT AS PREDICTED even if (1), (2) and (4) hold**, because then nothing was tested | _(not run)_ |

### DB11 — the spawn command's two refusals (SURVIVAL) — witness: the log

| prediction | READING |
|---|---|
| `REPLY "'phage' has stages 1 to 5; 6 is not one of them." [red]`, then `REPLY "'knell' has no stages. Spawn it without 'stage'." [red]`, then `ENTITIES CREEPER count=0 radius=8` and `ENTITIES WITHER_SKELETON count=0 radius=8`: neither refusal spawned anything | _(not run)_ |

---

## BEN'S ROWS — BY SIGHT, AFTER THE SELFTEST RUN. EACH ANSWER IS ONE WORD

**SURVIVAL on every row, difficulty normal (the run sheet).** Stand on flat open ground. Type each row's lines in
order; answer, then type its cleanup line. The mobs in B1-B4 have their AI switched off so they stand still beside
you.

### B1 — the stage-1 Phage is about one block tall (SURVIVAL)

`/rpg spawn phage` · `/data merge entity @e[type=creeper,limit=1,sort=nearest] {NoAI:1b}` · step back and look.

| question | READING |
|---|---|
| Is it about one block tall, its head about level with the top of a block beside it? **yes / no** | _(not run)_ |

Cleanup: `/kill @e[type=creeper,distance=..20]`

### B2 — the stage-5 Phage is about three blocks tall (SURVIVAL)

`/rpg spawn phage stage 5` · `/data merge entity @e[type=creeper,limit=1,sort=nearest] {NoAI:1b}` · step back.

| question | READING |
|---|---|
| Is it about three blocks tall, clearly taller than you? **yes / no** | _(not run)_ |

Cleanup: `/kill @e[type=creeper,distance=..20]`

### B3 — the Knell is clearly bigger than you (SURVIVAL)

`/rpg spawn knell` · `/data merge entity @e[type=wither_skeleton,limit=1,sort=nearest] {NoAI:1b}` · step back.

| question | READING |
|---|---|
| Is it clearly bigger than you? **yes / no** | _(not run)_ |

Cleanup: `/kill @e[type=wither_skeleton,distance=..20]`

### B4 — the mount is clearly bigger than a normal spider (SURVIVAL)

`/rpg spawn withered_mount` · `/data merge entity @e[type=spider,limit=1,sort=nearest] {NoAI:1b}` · walk four blocks
away · `/summon spider ~ ~ ~ {NoAI:1b}` · look at both.

| question | READING |
|---|---|
| Is the Withered Mount clearly bigger than the plain spider? **yes / no** | _(not run)_ |

Cleanup: `/kill @e[type=spider,distance=..20]`

### B5 — the stage-5 Phage walks visibly slower than the stage-1 (SURVIVAL)

`/rpg spawn phage` · `/rpg spawn phage stage 5` · walk away from them in a straight line for a few seconds, then
turn and look.

| question | READING |
|---|---|
| Did the small one keep up and the big one fall behind? **yes / no** | _(not run)_ |

Keep both alive for B6.

### B6 — a Phage next to you swells and does not explode (SURVIVAL)

Stand still and let the small Phage from B5 reach you. Watch it for about five seconds.

| question | READING |
|---|---|
| It swelled and shrank back, and never exploded? **yes / no** — and was any block broken around you? **yes / no** | _(not run)_ |

Cleanup: `/kill @e[type=creeper,distance=..20]`

**What B6 is expected to look like, said in advance so it is not read as a defect:** the Phage swells (flashes white
and grows), then snaps back to normal and starts again, for as long as you stand close. That is the cancelled
explosion (PLAN-dragon-mobs.md §7 F-C), and it is temporary until step R3 replaces the swell with ours.
