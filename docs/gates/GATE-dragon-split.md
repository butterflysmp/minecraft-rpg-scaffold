# GATE — DRAGON SPLIT (dragon-mobs step R2): a stage-5 Phage that dies leaves two stage-1 Phages; THE STACK TOP's R0 and run sheet

**Status: NOT RUN.** Every prediction below was written **before** any boot of `feat/dragon-split`, and this file is
**committed before the boot**. Readings go **beside** a prediction, never over it, and a prediction is not edited once
its row has been read. **NO BOOT until the seat has diffed the code AND this gate, and Ben says ready.**

**THIS PR IS THE STACK TOP.** `feat/dragon-split` (#R2) is stacked on `feat/dragon-bodies` (#189, step R1, NOT RUN).
**The coming boot is ONE STACKED BOOT of this branch's head**, and it reads BOTH gates. So:
- **The ONE R0 of that boot is this file's R0a-R0d.** `GATE-dragon-bodies.md`'s R0 rows are **SUPERSEDED in a stacked
  boot**, never PASS or FAIL (a dated note there says the same).
- **The ONE run sheet of that boot is this file's**, below. It replaces `GATE-dragon-bodies.md`'s run sheet for that
  boot. Its DB rows and B rows are still read against `GATE-dragon-bodies.md`'s predictions; only the R0 and the
  order of typing come from here.

> **NOTE, 2026-11-15 (added on `feat/dragon-growth`, the plan's step R4, before any boot; no prediction below is
> changed).** The stacked boot is now of `feat/dragon-growth`'s head. **This file's R0a-R0d and its run sheet are
> SUPERSEDED in that boot by `GATE-dragon-growth.md`'s.** DS1-DS6 and S1-S2 are still read against THIS file's
> predictions. **Checked row by row against growth, which is live in that boot:** every death in DS1-DS6 is a Phage's,
> so each is also a Void death, but no row has another Phage within 6 blocks of the one that dies (each row stages one
> Phage; the split's children are born after growth has collected its candidates). So growth adds no GROWTH line and
> changes no reading here. **S2's cleanup** kills two stage-1 Phages in one command: the first death schedules a
> stage change for the second on ITS next tick, by which time the same command has killed it, so the scheduled change
> finds it gone and does nothing (`MobGrower.applyGrowth` returns on `!isValid()`). Nothing is left, and no prediction
> rests on it.

**Plan:** `PLAN-dragon-mobs.md` — RULINGS (G4, E, "Any deaths", "No,", Ben's gear-score answer, the offset put to
him), §3.R.2, §4.2 N6, §5.R step R2. **The rule every AUTO row answers to:** `.claude/rules/verification.md`, *AN AUTO
READING IS A READING OF THE SERVER, NEVER OF THE CLIENT*.

```
ROWS     12   R0a R0b R0c R0d
              DS1 DS2 DS3 DS4 DS5 DS6
              S1 S2
         ──
         12   = 4 R0 + 6 DS + 2 S      git grep -c -E '^### ' <ref> -- docs/gates/GATE-dragon-split.md
```

The six `DS` rows are the scenarios in `paper/src/main/resources/selftest/GATE-dragon-split.yml`, added LAST in
`selftest/index.yml`; `SelfTestScenariosTest` pins that each scenario key is a `### <id> ` heading here.

## WHAT R2 BUILDS, AND WHAT IT DOES NOT

- **Built:** `phage.yml`'s `split:` block (`at: last`, `count: 2`, `child_stage: 1`, `spread: 0.5`). A Phage that dies
  at stage 5, of any cause that raises `EntityDeathEvent`, leaves two stage-1 Phages at full health, with its own
  gear score, half a block either side of it on x. A mob splits once at most (the `mob_split` mark). A split child of
  a mob the instrument tagged takes all its parent's tags.
- **NOT built, and no row tests it:** growth, any explosion of ours, anything on the other three mobs. Drops, coins and
  XP on the parent's death are unchanged.
- **NOT TESTABLE UNTIL R3: "an exploding Phage does not split" (Ben, E).** Nothing of ours explodes yet, and R1's guard
  cancels every vanilla explosion, so there is no path to an explosion to observe. **The seam is in place:** a vanilla
  creeper's own explosion is a `discard(EXPLODE)`, not a death (PLAN-dragon-mobs.md §7 F-C), and the split listens
  only to `EntityDeathEvent`. R3's detonation must remove the Phage the same way, without raising a death, and its gate
  must read that it left no children.

## GAME MODE

**SURVIVAL on every row.** `/rpg selftest` refuses outside SURVIVAL (`GATE-selftest.md` ST7). The difficulty must not
be PEACEFUL: on peaceful, monsters are removed.

## THE FACTS THE PREDICTIONS STAND ON

From the server jar `run/versions/26.1.2/paper-26.1.2.jar` (build 74), read with
`C:\Users\Neb91\.jdks\openjdk-26.0.1\bin\javap.exe -c -p -cp <jar> <class>`:

| fact | class#member |
|---|---|
| `/kill` is a death: `LivingEntity#kill` calls `hurtServer` with `damageSources().genericKill()` | `LivingEntity#kill` |
| the death event is raised from ONE place in `LivingEntity`, on the death path | `LivingEntity#dropAllDeathLoot` → `CraftEventFactory.callEntityDeathEvent` |
| `remove()` is not a death: it is `discard(PLUGIN)` | `CraftEntity#remove` |
| a despawn is not a death: `discard(DESPAWN)` | `Mob#checkDespawn` |
| a vanilla creeper's own explosion is not a death: `discard(EXPLODE)` | `Creeper#explodeCreeper` (R1's §7 F-C) |

A chunk unload is **INFERRED** not to be a death (an entity unloaded with its chunk is saved and removed, never
killed); not read here. **The selftest's row-start clean and restore use `remove()`**, so they never split anything.

**From this repo:** `SelfTestStaging.isKill` starts the settle only after a `cmd: "kill …"` step, and settle counts
only DEAD bodies (`dying(living, dead)`). **A split child is born ALIVE, so it is not counted and does not lengthen
the wait.** The settle's rule is unchanged; ST9R2 (owed) reads the same lines it would have.

**Every figure in the BODY predictions was EXECUTED** in jshell for R1 (`GATE-dragon-bodies.md`'s figures): a stage-1
Phage reads `scale=0.588 height=1.000 speedBase=0.3750 speedDefault=0.2500 speedX=1.500`. Its `maxHP` at gear score 300
is `80 × 300 / 100 = 240`, printed `240.000`; a stage 5 at 300 is `160 × 3 = 480`.

## CARRIED STATE, DONE BY THE BOOTER BEFORE THE STACKED BOOT, IN THIS ORDER

1. **No JVM running:** `Get-Process java -ErrorAction SilentlyContinue` prints nothing.
2. **A fresh dated backup**, with no server running, in ONE bash invocation from the repo root:

   ```bash
   B="/c/Users/Neb91/IdeaProjects/rpg-run-backups/$(date +%F)-pre-dragon-stack"; mkdir "$B" && cp -a run/plugins/Rpg run/world "$B/" && for p in "run/plugins/Rpg $B/Rpg" "run/world $B/world"; do set -- $p; echo "$1: $(find "$1" -type f | wc -l) files $(find "$1" -type f -printf '%s\n' | awk '{s+=$1} END{print s}') bytes; $2: $(find "$2" -type f | wc -l) files $(find "$2" -type f -printf '%s\n' | awk '{s+=$1} END{print s}') bytes; diff -rq lines: $(diff -rq "$1" "$2" | wc -l)"; done
   ```

   Report the folder, both pairs of counts, and `diff -rq lines: 0` for each.
3. **Check, do not assume:** `grep -n "dev-pack-on-join" run/plugins/Rpg/config.yml` must print
   `12:  dev-pack-on-join: true`. If it does not, STOP and report; do not edit it without the seat.
4. **Boot this branch's head:** `./scripts/dev-server.sh --refresh-content`. Only the booter boots, announcing
   "booting now", on Ben's "ready". `--refresh-content` is required: the mob files are new or changed.
5. **R0 binds to THIS boot:** read R0 only on a `latest.log` with a fresh mtime whose `[Rpg] Build:` line names this
   branch's head. **Readings count only after Ben's LOGIN in that boot's log.** Ben ends with `/stop`.

## THE RUN SHEET OF THE STACKED BOOT — BEN TYPES THESE, IN THIS ORDER. EVERY LINE IS LITERAL

1. `/gamemode survival` — **alone, the first line after login.**
2. `/difficulty` — read it out; the booter records it.
3. `/difficulty normal`
4. `/rpg selftest dragon-bodies` — wait for the chat to say it finished. Stand still; touch nothing while it runs.
5. `/rpg selftest dragon-split` — the same.
6. `GATE-dragon-bodies.md`'s Ben rows, **B1 to B6**, each with its own lines and cleanup, as written there.
7. This file's Ben rows, **S1 and S2**, below.
8. `/difficulty` — read it out again.
9. `/stop`

## R0 — THE STACK TOP'S BUILD. READ IT FIRST. IF ANY R0 FAILS, STOP

`GATE-dragon-bodies.md`'s R0 rows are SUPERSEDED at this boot. `GATE-nexus-back.md`'s, master's last, are too.

### R0a — the build line names THIS branch's head

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, this PR's head as `git ls-remote origin refs/heads/feat/dragon-split` prints it, shortened. **Not** `feat/dragon-bodies`'s `974e4357`. Not `-dirty`, not `unknown` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | **SUPERSEDED** at the stacked boot of `bda8e89c` (2026-11-15): `GATE-dragon-growth.md`'s R0a-R0d were read instead, all PASS |

### R0b — the jar carries BOTH steps

| prediction | instrument | READING |
|---|---|---|
| PRESENT, step R1: `selftest/GATE-dragon-bodies.yml`, `content/mob_elements.yml`, `content/mobs/phage.yml`, paper `health/MobBodyApplier.class`, paper `health/CustomCreeperGuard.class`, core `mob/MobElementResolver.class`. PRESENT, step R2: `selftest/GATE-dragon-split.yml`, paper `health/MobSplitter.class`, paper `health/MobSpawner.class`, core `mob/MobSplit.class`; in `content/mobs/phage.yml`, the line `split:`; in `selftest/index.yml`, the lines `  - dragon-bodies` and `  - dragon-split`. ABSENT: the control `selftest/NoSuchClassControl.class` | the scan below | **SUPERSEDED** at the stacked boot of `bda8e89c` (2026-11-15): `GATE-dragon-growth.md`'s R0a-R0d were read instead, all PASS |

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
               'io/github/butterflysmp/rpg/paper/selftest/NoSuchClassControl.class') {
  if ($zip.GetEntry($c)) { "PRESENT $c" } else { "ABSENT  $c" }
}
function Text($name) { $r = New-Object IO.StreamReader($zip.GetEntry($name).Open()); $t = $r.ReadToEnd(); $r.Close(); $t }
$idx = Text 'selftest/index.yml'
foreach ($g in 'dragon-bodies', 'dragon-split') {
  if ($idx -match "(?m)^  - $g\s*$") { "PRESENT index $g" } else { "ABSENT  index $g" }
}
if ((Text 'content/mobs/phage.yml') -match '(?m)^split:\s*$') { "PRESENT phage split:" } else { "ABSENT  phage split:" }
$zip.Dispose()
```

### R0c — the Loaded line, and nothing refused

| prediction | instrument | READING |
|---|---|---|
| **`[Rpg] Loaded 11 abilities, 29 visuals, 5 statuses, 7 elements, 11 enchants, 3 pools, 6 fragments, 6 aspects, 16 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 5 mobs, 1 mob elements, 3 recipes, 1 ores, 4 vein sizes`**: exactly `GATE-dragon-bodies.md`'s R0c prediction, because R2 adds a block to `phage.yml` and no file (`git diff --stat 974e4357 HEAD -- paper/src/main/resources/content` lists `phage.yml` alone). No `Refusing`, `Skipping`, `SEVERE` or `Exception`, and no `[Rpg]` `WARN`, beyond `volley_stone`'s known pair (`Content: weapon 'volley_stone' …`, `1 content problem(s)`); no `Skipping malformed mob` | `Select-String run\logs\latest.log -Pattern 'Loaded ','Refusing','Skipping','SEVERE','WARN\]: \[Rpg\]','Exception','Content:'` | **SUPERSEDED** at the stacked boot of `bda8e89c` (2026-11-15): `GATE-dragon-growth.md`'s R0a-R0d were read instead, all PASS |

### R0d — the dev marker reached the JVM, and every scenario loaded

| prediction | instrument | READING |
|---|---|---|
| Exactly one line **`[Rpg] Selftest: dev server, 74 scenarios across 13 gates [melee-cell, level, nexus-polish, legacy-a, legacy-b, wither-status, withered-shortbow, sunder, deft-hands, mining-auto, nexus-back, dragon-bodies, dragon-split]`**, and **no** `Refusing selftest` line | `Select-String run\logs\latest.log -Pattern 'Selftest:','Refusing selftest'` | **SUPERSEDED** at the stacked boot of `bda8e89c` (2026-11-15): `GATE-dragon-growth.md`'s R0a-R0d were read instead, all PASS |

**The 74 and the 13 are DERIVED.** From the repo root at this head:

```bash
cd paper/src/main/resources/selftest
grep -cE '^  - ' index.yml                                   # gates: 13
cat $(grep -E '^  - ' index.yml | tr -d '\r' | sed 's/  - /GATE-/; s/$/.yml/') | grep -cE '^[A-Za-z0-9-]+:'   # keys: 87
cat $(grep -E '^  - ' index.yml | tr -d '\r' | sed 's/  - /GATE-/; s/$/.yml/') | grep -cE '^setup:'           # setups: 13
```

Printed when this was written: **13, 87, 13**, so 87 − 13 = **74**. The same counts at `feat/dragon-bodies`
`974e4357` print 12, 80 and 12: 68, the line `GATE-dragon-bodies.md` R0d predicted (the control).
`SelfTestScenariosTest` pins the 74.

---

## THE INSTRUMENT'S ROWS — AUTO, SERVER ONLY

**The log instrument for every row:** `Select-String run\logs\latest.log -Pattern 'SELFTEST dragon-split|\[Rpg\] SPLIT|CREEPERGUARD'`,
read after Ben's LOGIN, inside the row's own `SELFTEST dragon-split <row> START … DONE` bracket unless a row says
otherwise. A `SPLIT` line is `[Rpg] SPLIT mob=phage parent=<uuid> stage=5 gs=300 children=2 childStage=1 [<uuid>
dx=-0.50, <uuid> dx=0.50]`; the uuids are the only fields not predicted. **A child spot inside a block would read
`<uuid> at-parent` instead of `dx=…`; the staging is open floor, so no row predicts it.** The child BODY line below is
one line, written once here:

```
BODY tag=<row> <uuid> type=creeper mob=phage stage=1 scale=0.588 height=1.000 speedBase=0.3750 speedDefault=0.2500 speedX=1.500 maxHP=240.000 gs=300 element=void dead=false
```

**The children keep their AI** (NoAI is entity data the split does not copy), so they walk toward the player and
swell, and `CREEPERGUARD prime cancelled` lines may appear inside DS1, DS2 and DS5. **Those are R1's guard working and
are not this gate's reading**; their count is not predicted.

### DS1 — a stage 5 killed by the PLAYER'S attack splits into two stage 1s at the parent's gear score (SURVIVAL) — witness: the log

| prediction | READING |
|---|---|
| (1) `REPLY "CREEPER damaged: 1/480 custom HP" [green]` (stage 5 at gs 300 is 480; 479 taken leaves 1). (2) `DRIVE attack Player#attack target=<the ds1 uuid> creeper …`, then **exactly one** `[Rpg] SPLIT mob=phage parent=<the ds1 uuid> stage=5 gs=300 children=2 childStage=1 [<uuid> dx=-0.50, <uuid> dx=0.50]`. (3) `ENTITIES CREEPER count=2 radius=16`. (4) **Exactly two** BODY lines, each the child line above with `tag=ds1`. **`gs=300` IS the reading that matters**: the overworld rolls 100, so 300 is the parent's score and not the place's. **INFERRED, not read:** that the iron sword's hit deals at least 1 custom health to a mob; if it deals less, there is no death and no SPLIT line, and this row reads that | **NOT AS PREDICTED.** (1) verbatim: `REPLY "CREEPER damaged: 1/480 custom HP" [green]`. (2) `DRIVE attack Player#attack target=13a6d25c-e3cc-454d-81c9-58f6f0d432df creeper d=3.000 charge=1.000`, then **no** SPLIT line. (3) `ENTITIES CREEPER count=1 radius=16`. (4) `BODY tag=ds1 13a6d25c-e3cc-454d-81c9-58f6f0d432df type=creeper mob=phage stage=5 … maxHP=480.000 gs=300 … dead=false`. The iron sword's hit did not kill (the row's own INFERRED caveat); why was not read |

### DS2 — a stage 5 killed by `/kill`, not the player, also splits (SURVIVAL) — witness: the log

| prediction | READING |
|---|---|
| `CMD /kill @e[tag=ds2]`, **exactly one** SPLIT line for the ds2 parent (as DS1's, `parent=<the ds2 uuid>`), then the settle's `SETTLED dying=0 ticks=<t> first=1` (the parent's body; the children are alive and not counted, so `first=1`, not 3), `ENTITIES CREEPER count=2 radius=16`, and **exactly two** child BODY lines with `tag=ds2`. `ticks=` is recorded, not predicted (ST9R's tick clause is still undiagnosed) | **NOT AS PREDICTED.** The tag landed on DS1's Phage (`REPLY "Target either already has the tag or has too many tags" [red]`, twice). `CMD /kill @e[tag=ds2]` → `REPLY "No entity was found" [red]`; `SETTLED dying=0 ticks=0 first=0`; `ENTITIES CREEPER count=1 radius=16`; `BODY tag=ds2 none within 16 blocks`. No SPLIT line |

### DS3 — a stage 3 killed does not split (SURVIVAL) — witness: the log

| prediction | READING |
|---|---|
| `CMD /kill @e[tag=ds3]`, **no** SPLIT line between DS3's START and DONE, and `ENTITIES CREEPER count=0 radius=16` | **NOT AS PREDICTED.** The tag landed on DS2's untagged stage 5: `SPLIT mob=phage parent=1ab28b31-1778-4300-8093-52e359567839 stage=5 gs=300 children=2 childStage=1 [1b0164cd-8bac-444b-b7ec-e712549d4b9e dx=-0.50, 16566a70-950b-4286-af7b-b27b31e3d652 dx=0.50]` inside DS3; `ENTITIES CREEPER count=3 radius=16` |

### DS4 — a stage 1 killed does not split (SURVIVAL) — witness: the log

| prediction | READING |
|---|---|
| `CMD /kill @e[tag=ds4]`, **no** SPLIT line between DS4's START and DONE, and `ENTITIES CREEPER count=0 radius=16` | **NOT AS PREDICTED (count).** No SPLIT line (as predicted). `ENTITIES CREEPER count=1 radius=16` (INFERRED: DS3's untagged stage 3) |

### DS5 — once only, read on the server: two after the split, still two forty ticks later (SURVIVAL) — witness: the log

| prediction | READING |
|---|---|
| **Exactly one** SPLIT line between DS5's START and DONE, and `ENTITIES CREEPER count=2 radius=16` **twice**, the second forty ticks after the first. **What this row can and cannot see:** it reads that one death splits once and nothing adds children afterwards. **It cannot stage a SECOND delivery of the death event** (only a plugin that cancels and revives can); that guard's proof is `MobSplitTest.aMobThatHasAlreadySplitAddsNone` and `DragonSplitWiringTest.theMarkIsReadThenWrittenBeforeAnyChildSpawns`, with the mutations `MUTONCE` and `MUTMARKWRITE` that redden them | **NOT AS PREDICTED (count); ONCE-ONLY HOLDS.** Exactly one `SPLIT mob=phage parent=91a6f23c-70d9-4f96-afd1-b82af253d9ac stage=5 gs=300 children=2 childStage=1 [0464d160-9e82-4b64-9e32-3e715eea5cc6 dx=-0.50, 87474e3b-4ef4-4f5d-a5ba-581d67c310b4 dx=0.50]`; `ENTITIES CREEPER count=3 radius=16` twice, forty ticks apart: 3, not 2, and unchanged |

### DS6 — the row-start clean removes the split's children (SURVIVAL) — witness: the log

| prediction | READING |
|---|---|
| The `CLEAN … removed=2 …` line that the run logs between DS5's DONE and DS6's START (DS5's two children, which carry DS5's tags; the parent was already gone), then, inside DS6, `ENTITIES CREEPER count=0 radius=16`. **Zero leaked children.** Without the instrument's tag rule the clean would read `removed=0` and the count `2` | **NOT AS PREDICTED (count).** DS6's own `CLEAN blocks=756 removed=2 inventory=kept …` line (logged just after DS6's START), as predicted. Then `ENTITIES CREEPER count=3 radius=16`, not 0 |

---

## BEN'S ROWS — BY SIGHT, AFTER `GATE-dragon-bodies.md`'s B1-B6. EACH ANSWER IS ONE WORD

**SURVIVAL, difficulty normal (the run sheet).** Stand on flat open ground.

### S1 — a stage-5 Phage killed leaves two small ones where it died (SURVIVAL)

`/rpg spawn phage stage 5` · `/data merge entity @e[type=creeper,limit=1,sort=nearest] {NoAI:1b}` · step back three
blocks and look at it · `/rpg mobdamage 159` · hit it once.

| question | READING |
|---|---|
| When it died, did two small Phages appear where it stood? **yes / no** | Ben: "Yes it splits when it dies" |

### S2 — the two small ones come at you (SURVIVAL)

Stand still after S1 and watch the two small Phages for about five seconds.

| question | READING |
|---|---|
| Did they walk toward you (and swell without exploding, as in B6)? **yes / no** | Ben: "yes" |

Cleanup: `/kill @e[type=creeper,distance=..20]` — **this kills them, and a stage-1 Phage does not split**, so nothing
is left.

> **READINGS NOTE, 2026-11-15 (a pointer; the account is `GATE-dragon-growth.md`, *READINGS NOTE, 2026-11-15*).** This
> gate's rows were read at the stacked boot of `bda8e89c`; R0 SUPERSEDED. Most rows read NOT AS PREDICTED, for two
> defects found there: the speed defect (`speedDefault=0.7000` on every BODY line) and the selftest's tag race (a new
> mob untagged, so it kept its AI and leaked into later rows). No prediction was edited.

> **DATED NOTE, 2026-11-15, after the first boot (`feat/dragon-fix`, Part A).** **This gate's R0 and run sheet are
> SUPERSEDED by `GATE-dragon-fix.md`**, the new stack top, which holds the ONE R0 and the ONE run sheet of the next
> boot. Every AUTO row here that read NOT AS PREDICTED is re-predicted there as a new row named for it (`…R2`); the
> predictions and readings above stay as they are. **The seat's misses, in its words, and the boot's departures are
> recorded in `PLAN-dragon-mobs.md`, RULINGS, *THE FIRST BOOT OF THE STACK*:** the seat passed this stack's speed
> predictions on arithmetic from `speedDefault=0.25` without the jar read for what `AttributeInstance#getDefaultValue`
> returns; the seat passed #190 without seeing that B2's and B6's cleanups split a stage 5; and the run sheet let a
> hostile mob stand beside Ben before the difficulty was set.
