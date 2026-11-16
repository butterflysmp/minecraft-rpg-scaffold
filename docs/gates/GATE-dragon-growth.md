# GATE — DRAGON GROWTH (the plan's step R4, built ahead of R3): Void deaths nearby grow a Phage; THE STACK TOP's R0 and run sheet

**Status: NOT RUN.** Every prediction below was written **before** any boot of `feat/dragon-growth`, and this file is
**committed before the boot**. Readings go **beside** a prediction, never over it, and a prediction is not edited once
its row has been read. **NO BOOT until the seat has diffed the code AND this gate, and Ben says ready.**

**THIS PR IS THE STACK TOP.** `feat/dragon-growth` is stacked on `feat/dragon-split` (#190, step R2), which is stacked
on `feat/dragon-bodies` (#189, step R1). None is booted. **The coming boot is ONE STACKED BOOT of this branch's
head**, and it reads all three gates. So:
- **The ONE R0 of that boot is this file's R0a-R0d.** `GATE-dragon-bodies.md`'s and `GATE-dragon-split.md`'s R0 rows
  are **SUPERSEDED in a stacked boot**, never PASS or FAIL (a dated note in each says the same).
- **The ONE run sheet of that boot is this file's**, below. It replaces both earlier run sheets for that boot. Their
  DB, DS, B and S rows are still read against THEIR OWN predictions; only the R0 and the order of typing come from here.

**THE BUILD ORDER IS NOT YET BEN'S.** Growth (R4) was built before the self-destruct (R3) on the seat's reading of
Ben's *"Is there anything more we can do?"* as a go-ahead (`PLAN-dragon-mobs.md`, RULINGS, *THE BUILD ORDER CHANGED*;
Q28). If he reverses it, this gate waits for R3.

**Plan:** `PLAN-dragon-mobs.md` — RULINGS (Ben's growth words, G1 and G2 confirmed, "No,", "Same share", the order
change), §5.R.3. **The rule every AUTO row answers to:** `.claude/rules/verification.md`, *AN AUTO READING IS A
READING OF THE SERVER, NEVER OF THE CLIENT*.

```
ROWS     15   R0a R0b R0c R0d
              DG1 DG2 DG3 DG4 DG5 DG6 DG7 DG8 DG9
              G1 G2
         ──
         15   = 4 R0 + 9 DG + 2 G      git grep -c -E '^### ' <ref> -- docs/gates/GATE-dragon-growth.md
```

The nine `DG` rows are the scenarios in `paper/src/main/resources/selftest/GATE-dragon-growth.yml`, added LAST in
`selftest/index.yml`; `SelfTestScenariosTest` pins that each scenario key is a `### <id> ` heading here. **No new verb**:
every row uses `CMD`, `WAIT`, `AIM`, `PROBE`, `PROBE_BODY` (R1's) and `PROBE_ENTITIES`.

## WHAT IT BUILDS, AND WHAT IT DOES NOT

- **Built:** `phage.yml`'s `growth:` block (`element: void`, `radius: 6`, `stages_per_death: 1`). A death that raises
  `EntityDeathEvent`, of a creature whose element resolves to void (our Void mobs by file, a vanilla Enderman by the
  table), grows EVERY living Phage below stage 5 whose feet are within 6.00 blocks of the dead creature's feet, in the
  same world, by one stage: the stage tag, the body (R1's applier, re-applied), and the health (the new stage's maximum
  at the mob's own gear score, current at the same share). One `GROWTH` line per stage change. A death is counted once
  (`mob_growth_fed`). In the one death handler, growth runs BEFORE the split, so a stage 5's death grows the others and
  never its own children.
- **NOT built, and no row tests it:** any explosion of ours (so a self-destruct counting as a Void death waits for
  R3, behind the seam `MobGrower.reportDeath`), a cap on Phages, visuals or sounds for growing, the other three mobs.
- **NOT checked by this step:** whether a grown body fits where it stands. What the server does about it is a finding
  in the plan (§5.R.3), read from the jar; no row stages a low ceiling.

## GAME MODE

**SURVIVAL on every row.** `/rpg selftest` refuses outside SURVIVAL. The difficulty must not be PEACEFUL: on peaceful,
monsters are removed, Endermen included.

## THE FACTS THE PREDICTIONS STAND ON

From the server jar `run/versions/26.1.2/paper-26.1.2.jar` (build 74), read with
`C:\Users\Neb91\.jdks\openjdk-26.0.1\bin\javap.exe -c -p -cp <jar> <class>`:

| fact | class#member |
|---|---|
| `/kill` hurts with `genericKill` for `3.4028235E38f` | `LivingEntity#kill` |
| an Enderman's damage handler calls `Monster.hurtServer` (offset 59), where a lethal hit DIES and raises the death event, BEFORE its escape branch (`tryEscape`, offset 108, then `teleport()`). **So the death is observed where the Enderman stood**, whatever the escape does after | `EnderMan#hurtServer` |
| a NoAI mob runs no AI step, so a summoned Enderman does not teleport in daylight or wander while it waits (*INFERRED* from `NoAI`'s meaning; the AI path was not read) | — |
| a SCALE change calls `refreshDimensions` → `fudgePositionAfterSizeChange` → `Level.findFreePosition`, and moves the mob to the free spot found (`setPos`) | `Entity#refreshDimensions`, `Entity#fudgePositionAfterSizeChange` |

**Every BODY figure was EXECUTED in jshell**, following the re-applied height through each stage (the growing body's
`getHeight()` is `(float) 1.7f * (float) scale`, and the next scale is `target / (currentHeight / scale)`):

| stage | scale | height | speedBase | speedX | maxHP at gs 300 |
|---|---|---|---|---|---|
| 1 | 0.588 | 1.000 | 0.3750 | 1.500 | 240.000 |
| 2 | 0.882 | 1.500 | 0.3125 | 1.250 | 300.000 |
| 3 | 1.176 | 2.000 | 0.2500 | 1.000 | 360.000 |
| 4 | 1.471 | 2.500 | 0.1875 | 0.750 | 420.000 |
| 5 | 1.765 | 3.000 | 0.1250 | 0.500 | 480.000 |

`speedDefault=0.2500` on every line. **A BODY line at stage N below** is `BODY tag=<tag> <uuid> type=creeper mob=phage
stage=N scale=… height=… speedBase=… speedDefault=0.2500 speedX=… maxHP=… gs=300 element=void dead=false`, with
the row of this table for N. **The health rescale was executed too:** 120 of 240, rescaled to 300, reads 150.

**A GROWTH line** is `[Rpg] GROWTH mob=phage id=<uuid> from=<a> to=<b> fed=<type>[/<mob id>] <uuid> element=void
d=<blocks, 2 dp> max=<new max, 0 dp>`. The uuids are the only fields not predicted. **Where a row predicts `d`, it is
the first growth of that Phage**: a body that grows is re-centred by `fudgePositionAfterSizeChange`, so a later `d` is
recorded, not predicted.

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
6. `/rpg selftest dragon-growth` — the same.
7. `GATE-dragon-bodies.md`'s Ben rows, **B1 to B6**, each with its own lines and cleanup, as written there, **with one
   change: after B2's cleanup and after B6's cleanup, type that same cleanup line a SECOND time**
   (`/kill @e[type=creeper,distance=..20]`). Each kills a stage-5 Phage, which since R2 splits into two small ones; the
   second line removes them. Without it they walk into the next rows, and S1's `sort=nearest` could pick one. (R1's
   gate was written before the split existed; `GATE-dragon-split.md` did not catch this. Recorded in the note on
   `GATE-dragon-bodies.md`.)
8. `GATE-dragon-split.md`'s Ben rows, **S1 and S2**, as written there.
9. This file's Ben rows, **G1 and G2**, below.
10. `/difficulty` — read it out again.
11. `/stop`

## R0 — THE STACK TOP'S BUILD. READ IT FIRST. IF ANY R0 FAILS, STOP

`GATE-dragon-bodies.md`'s and `GATE-dragon-split.md`'s R0 rows are SUPERSEDED at this boot, as are master's last
(`GATE-nexus-back.md`).

### R0a — the build line names THIS branch's head

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, this PR's head as `git ls-remote origin refs/heads/feat/dragon-growth` prints it, shortened. **Not** `feat/dragon-split`'s `56a8bb00` nor `feat/dragon-bodies`'s `974e4357`. Not `-dirty`, not `unknown` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | **PASS.** `[21:34:11] [Server thread/INFO]: [Rpg] Build: bda8e89c`; `git ls-remote origin refs/heads/feat/dragon-growth` printed `bda8e89c6584fff9e3e6c64cd4406c78b9cff078`. latest.log mtime 21:34:12, read 21:34:26. Not -dirty, not unknown |

### R0b — the jar carries ALL THREE steps

| prediction | instrument | READING |
|---|---|---|
| PRESENT, R1: `selftest/GATE-dragon-bodies.yml`, `content/mob_elements.yml`, `content/mobs/phage.yml`, paper `health/MobBodyApplier.class`, paper `health/CustomCreeperGuard.class`, core `mob/MobElementResolver.class`. PRESENT, R2: `selftest/GATE-dragon-split.yml`, paper `health/MobSplitter.class`, paper `health/MobSpawner.class`, core `mob/MobSplit.class`. PRESENT, growth: `selftest/GATE-dragon-growth.yml`, paper `health/MobGrower.class`, core `mob/MobGrowth.class`. In `content/mobs/phage.yml`, the lines `split:` and `growth:`; in `selftest/index.yml`, `  - dragon-bodies`, `  - dragon-split` and `  - dragon-growth`. ABSENT: the control `selftest/NoSuchClassControl.class` | the scan below | **PASS.** 13 of 13 `PRESENT` class/file entries; `PRESENT index dragon-bodies`, `PRESENT index dragon-split`, `PRESENT index dragon-growth`; `PRESENT phage split:`, `PRESENT phage growth:`; `ABSENT  io/github/butterflysmp/rpg/paper/selftest/NoSuchClassControl.class` (the control) |

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
               'io/github/butterflysmp/rpg/paper/selftest/NoSuchClassControl.class') {
  if ($zip.GetEntry($c)) { "PRESENT $c" } else { "ABSENT  $c" }
}
function Text($name) { $r = New-Object IO.StreamReader($zip.GetEntry($name).Open()); $t = $r.ReadToEnd(); $r.Close(); $t }
$idx = Text 'selftest/index.yml'
foreach ($g in 'dragon-bodies', 'dragon-split', 'dragon-growth') {
  if ($idx -match "(?m)^  - $g\s*$") { "PRESENT index $g" } else { "ABSENT  index $g" }
}
$phage = Text 'content/mobs/phage.yml'
foreach ($k in 'split', 'growth') {
  if ($phage -match "(?m)^${k}:\s*$") { "PRESENT phage ${k}:" } else { "ABSENT  phage ${k}:" }
}
$zip.Dispose()
```

### R0c — the Loaded line, and nothing refused

| prediction | instrument | READING |
|---|---|---|
| **`[Rpg] Loaded 11 abilities, 29 visuals, 5 statuses, 7 elements, 11 enchants, 3 pools, 6 fragments, 6 aspects, 16 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 5 mobs, 1 mob elements, 3 recipes, 1 ores, 4 vein sizes`**: exactly the line R1 and R2 predicted, because growth adds a block to `phage.yml` and no file (`git diff --stat 56a8bb00 HEAD -- paper/src/main/resources/content` lists `phage.yml` alone). No `Refusing`, `Skipping`, `SEVERE` or `Exception`, and no `[Rpg]` `WARN`, beyond `volley_stone`'s known pair (`Content: weapon 'volley_stone' …`, `1 content problem(s)`); no `Skipping malformed mob` | `Select-String run\logs\latest.log -Pattern 'Loaded ','Refusing','Skipping','SEVERE','WARN\]: \[Rpg\]','Exception','Content:'` | **PASS, verbatim.** `[Rpg] Loaded 11 abilities, 29 visuals, 5 statuses, 7 elements, 11 enchants, 3 pools, 6 fragments, 6 aspects, 16 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 5 mobs, 1 mob elements, 3 recipes, 1 ores, 4 vein sizes`. `[Rpg]` WARN: only `volley_stone`'s known pair. No Refusing, Skipping, SEVERE or Exception in the whole log |

### R0d — the dev marker reached the JVM, and every scenario loaded

| prediction | instrument | READING |
|---|---|---|
| Exactly one line **`[Rpg] Selftest: dev server, 83 scenarios across 14 gates [melee-cell, level, nexus-polish, legacy-a, legacy-b, wither-status, withered-shortbow, sunder, deft-hands, mining-auto, nexus-back, dragon-bodies, dragon-split, dragon-growth]`**, and **no** `Refusing selftest` line | `Select-String run\logs\latest.log -Pattern 'Selftest:','Refusing selftest'` | **PASS, verbatim.** `[Rpg] Selftest: dev server, 83 scenarios across 14 gates [melee-cell, level, nexus-polish, legacy-a, legacy-b, wither-status, withered-shortbow, sunder, deft-hands, mining-auto, nexus-back, dragon-bodies, dragon-split, dragon-growth]`; no `Refusing selftest` |

**The 83 and the 14 are DERIVED.** From the repo root at this head:

```bash
cd paper/src/main/resources/selftest
grep -cE '^  - ' index.yml                                   # gates: 14
cat $(grep -E '^  - ' index.yml | tr -d '\r' | sed 's/  - /GATE-/; s/$/.yml/') | grep -cE '^[A-Za-z0-9-]+:'   # keys: 97
cat $(grep -E '^  - ' index.yml | tr -d '\r' | sed 's/  - /GATE-/; s/$/.yml/') | grep -cE '^setup:'           # setups: 14
```

Printed when this was written: **14, 97, 14**, so 97 − 14 = **83**. The same counts at `feat/dragon-split`
`56a8bb00` print 13, 87 and 13: 74, the line `GATE-dragon-split.md` R0d predicted (the control).
`SelfTestScenariosTest` pins the 83.

---

## THE INSTRUMENT'S ROWS — AUTO, SERVER ONLY

**The log instrument for every row:** `Select-String run\logs\latest.log -Pattern 'SELFTEST dragon-growth|\[Rpg\] GROWTH|\[Rpg\] SPLIT'`,
read after Ben's LOGIN, inside the row's own `SELFTEST dragon-growth <row> START … DONE` bracket. **Every Phage in
these rows has NoAI**, so it neither moves nor swells; no `CREEPERGUARD` line is expected and none is a reading here.

### DG1 — four Enderman deaths take a stage 1 to stage 5, one stage each (SURVIVAL) — witness: the log

| prediction | READING |
|---|---|
| A stage-1 BODY line (tag `dg1`) before any death. Then, after each of the four kills in turn: **exactly one** `GROWTH mob=phage id=<the dg1 uuid> from=N to=N+1 fed=enderman <uuid> element=void d=… max=…` (N = 1, 2, 3, 4; `max=300`, `360`, `420`, `480`; the FIRST reads **`d=2.00`**: the Phage 3 blocks east, the Enderman 5), and a BODY line at stage N+1: heights **1.500, 2.000, 2.500, 3.000**, speedX **1.250, 1.000, 0.750, 0.500**, maxHP **300.000, 360.000, 420.000, 480.000**, `gs=300` on every line | **NOT AS PREDICTED, on speed only.** Four `GROWTH mob=phage id=c69dc79d-cecb-4b84-bf9e-82ecb56d73d0 from=N to=N+1 fed=enderman <uuid> element=void d=2.00 max=…` lines, max `300`, `360`, `420`, `480` (as predicted; every d read 2.00). BODY lines stage 1 to 5: heights 1.000/1.500/2.000/2.500/3.000, speedX 1.500/1.250/1.000/0.750/0.500, maxHP 240/300/360/420/480, gs=300 (as predicted). **But** `speedBase=1.0500/0.8750/0.7000/0.5250/0.3500 speedDefault=0.7000` against the predicted 0.3750…0.1250 and 0.2500 (the speed defect: see the readings note) |

### DG2 — one death advances TWO Phages (SURVIVAL) — witness: the log

| prediction | READING |
|---|---|
| **Exactly two** GROWTH lines for the one death, `from=1 to=2`, one for the `dg2a` Phage with **`d=2.00`** and one for the `dg2b` Phage with **`d=3.61`** (`√(2² + 3²)` = 3.6056); then a stage-2 BODY line for each tag | **NOT AS PREDICTED.** The two GROWTH lines match: `GROWTH mob=phage id=66a4be07-c012-4cb1-9ece-d694663a00d8 from=1 to=2 fed=enderman 262973fb-0902-4f91-9e3a-bc805829f5b1 element=void d=2.00 max=300` and `GROWTH mob=phage id=564165b0-ed83-40c6-829f-6cd0a9884208 from=1 to=2 fed=enderman 262973fb-0902-4f91-9e3a-bc805829f5b1 element=void d=3.61 max=300`. But `BODY tag=dg2a none within 16 blocks` (the tag landed on DG1's leftover: `REPLY "Target either already has the tag or has too many tags" [red]`), and dg2b's BODY has the speed defect (`speedBase=0.8750 speedDefault=0.7000`) |

### DG3 — a death at 6.05 blocks advances nothing; at exactly 6.00 it does (SURVIVAL) — witness: the log

| prediction | READING |
|---|---|
| After the 6.05 kill: **no** GROWTH line, and a stage-**1** BODY line for `dg3`. After the 6.00 kill (the control: the radius is INCLUSIVE): **exactly one** GROWTH line `from=1 to=2 … d=6.00 max=300` and a stage-**2** BODY line | **NOT AS PREDICTED, on speed only.** After the 6.05 kill: no GROWTH line, and `BODY tag=dg3 05fdd2ff-8104-4fb7-87f8-cd0a8299398a type=creeper mob=phage stage=1 … maxHP=240.000 gs=300`. After the 6.00 kill: exactly one `GROWTH mob=phage id=05fdd2ff-8104-4fb7-87f8-cd0a8299398a from=1 to=2 fed=enderman 91a96a1d-4cee-4b56-a897-807df1656ece element=void d=6.00 max=300` and a stage-2 BODY. Both BODY lines carry `speedBase=1.0500`/`0.8750 speedDefault=0.7000` |

### DG4 — a zombie's death advances nothing; an Enderman's does (SURVIVAL) — witness: the log

| prediction | READING |
|---|---|
| After the zombie's kill: **no** GROWTH line, a stage-**1** BODY line for `dg4`. After the Enderman's (the control): **exactly one** GROWTH line `from=1 to=2 fed=enderman … d=2.00 max=300`, a stage-**2** BODY line | **NOT AS PREDICTED.** After the zombie's kill: no GROWTH line (as predicted). `BODY tag=dg4 none within 16 blocks`, twice (the tag race). After the Enderman's: TWO GROWTH lines, `GROWTH mob=phage id=66a4be07-c012-4cb1-9ece-d694663a00d8 from=2 to=3 fed=enderman ca87e2f4-e3ec-4413-b163-22bbf4a85a9d element=void d=4.05 max=360` (DG2's untagged leftover) and `GROWTH mob=phage id=c713363a-d8d2-4254-b357-760f72a3eccf from=1 to=2 fed=enderman ca87e2f4-e3ec-4413-b163-22bbf4a85a9d element=void d=2.00 max=300` (INFERRED to be the untagged dg4 Phage, by its d=2.00) |

### DG5 — a Phage's own death advances another Phage (SURVIVAL) — witness: the log

| prediction | READING |
|---|---|
| **No** SPLIT line (the dead Phage was stage 3). **Exactly one** GROWTH line `id=<the dg5a uuid> from=1 to=2 fed=creeper/phage <the dg5b uuid> element=void d=2.00 max=300`, and a stage-2 BODY line for `dg5a` | **NOT AS PREDICTED (count).** No SPLIT line (as predicted). The predicted line is present: `GROWTH mob=phage id=f960a585-349b-4461-bae6-b852b6ebeff7 from=1 to=2 fed=creeper/phage 96f5bd4d-2fec-434c-b0e3-d0218702ed5e element=void d=2.00 max=300`, with `BODY tag=dg5a f960a585-… stage=2 …` (speed defect). But there are three GROWTH lines, not one: leftovers `66a4be07-… from=3 to=4 … d=4.56 max=420` and `c713363a-… from=2 to=3 … d=3.54 max=360` also grew |

### DG6 — a stage 5 killed beside a stage 2: two children at stage 1, the bystander to stage 3 (SURVIVAL) — witness: the log

| prediction | READING |
|---|---|
| (1) **Exactly one** SPLIT line, `parent=<the dg6p uuid> stage=5 gs=300 children=2 childStage=1 [<uuid> dx=-0.50, <uuid> dx=0.50]`. (2) **Exactly one** GROWTH line in the row: `id=<the dg6b uuid> from=2 to=3 fed=creeper/phage <the dg6p uuid> element=void d=2.00 max=360`. **No GROWTH line names either child.** (3) A stage-**3** BODY line for `dg6b`. (4) **Exactly two** stage-**1** BODY lines for `dg6p` (the children carry the parent's tags): `maxHP=240.000 gs=300`. (5) `ENTITIES CREEPER count=3 radius=16`. **(2) and (4) together are the parent-and-children reading**: the children stand 1.5 and 2.5 blocks from the bystander's spot and 0.5 from where the parent died, inside the radius, and do not grow | **NOT AS PREDICTED; THE PARENT-AND-CHILDREN READING HOLDS.** (1) as predicted: `SPLIT mob=phage parent=c057186b-7502-4aaa-a7b4-a4d5e9385419 stage=5 gs=300 children=2 childStage=1 [c1446c7d-9eab-45d7-9848-f3059c4b416a dx=-0.50, bc625cd3-ceac-4fe6-858f-93ebe31697c6 dx=0.50]`. (2) `GROWTH mob=phage id=c9c34994-5eed-4170-9e7b-da4e984d0697 from=2 to=3 fed=creeper/phage c057186b-7502-4aaa-a7b4-a4d5e9385419 element=void d=2.00 max=360` is present, plus two leftovers (`66a4be07-… from=4 to=5 … d=4.56 max=480`, `c713363a-… from=3 to=4 … d=3.54 max=420`). **No GROWTH line names either child.** (3) `BODY tag=dg6b none within 16 blocks` (the tag race). (4) two stage-1 BODY lines for dg6p, `c1446c7d-…` and `bc625cd3-…`, `maxHP=240.000 gs=300` (speed defect). (5) `ENTITIES CREEPER count=5 radius=16`, not 3 |

### DG7 — half health before, half of the new maximum after (SURVIVAL) — witness: the log

| prediction | READING |
|---|---|
| `REPLY "CREEPER damaged: 120/240 custom HP" [green]`; then `PROBE tag=dg7 <uuid> creeper customHP=120.000/240.000 …`; then **exactly one** GROWTH line `from=1 to=2 … max=300`; then `PROBE tag=dg7 <uuid> creeper customHP=150.000/300.000 …` | **NOT AS PREDICTED; THE ROW'S CONDITION NEVER PRESENTED.** `REPLY "CREEPER damaged: 240/360 custom HP" [green]`: the aim-ray hit a stage-3 leftover (INFERRED from the 360 maximum), not dg7. `PROBE tag=dg7 a3cd8050-3ef5-4ac8-a89c-66b3bb66f31e creeper customHP=240.000/240.000 …`; then `GROWTH mob=phage id=a3cd8050-3ef5-4ac8-a89c-66b3bb66f31e from=1 to=2 fed=enderman 0842814f-a238-4478-8494-c5607c7640aa element=void d=2.00 max=300` (plus two leftovers'); then `PROBE tag=dg7 a3cd8050-… creeper customHP=300.000/300.000 …`. Full stayed full; half health was never read |

### DG8 — a stage 5 beside a death stays 5; a stage 1 beside the same death goes to 2 (SURVIVAL) — witness: the log

| prediction | READING |
|---|---|
| **Exactly one** GROWTH line for the death, naming the `dg8small` Phage `from=1 to=2`; **none** naming `dg8big`. A stage-**5** BODY line for `dg8big` (maxHP 480.000), a stage-**2** BODY line for `dg8small` | **NOT AS PREDICTED.** `BODY tag=dg8big none within 16 blocks` (the tag race). `GROWTH mob=phage id=fe33075e-105e-4b8a-a6ec-598745883a03 from=1 to=2 fed=enderman 169f29da-aabf-49f7-942b-5213cb9d9351 element=void d=3.61 max=300` and `BODY tag=dg8small fe33075e-… stage=2 … maxHP=300.000 gs=300` (speed defect). A second GROWTH, leftover `c9c34994-… from=4 to=5 … d=3.06 max=480`. No GROWTH line `from=5` anywhere in the row |

### DG9 — a Phage grown to stage 5, then killed, splits (SURVIVAL) — witness: the log

| prediction | READING |
|---|---|
| **Exactly one** GROWTH line `from=4 to=5 … d=2.00 max=480`, then a stage-**5** BODY line for `dg9`. After its kill: **exactly one** SPLIT line `parent=<the dg9 uuid> stage=5 gs=300 children=2 childStage=1 […]`, **exactly two** stage-**1** BODY lines for `dg9` (`maxHP=240.000 gs=300`), and `ENTITIES CREEPER count=2 radius=16`. **The grown Phage carried no `mob_split` mark**, which is why it split | **NOT AS PREDICTED (count and speed); SPLIT AFTER GROWTH HOLDS.** `GROWTH mob=phage id=a3a0d10c-b368-4cab-a475-415649ca0b73 from=4 to=5 fed=enderman 6309c3f5-346b-4dfc-8f81-50ee0424f783 element=void d=2.00 max=480`, a stage-5 BODY for dg9, then `SPLIT mob=phage parent=a3a0d10c-b368-4cab-a475-415649ca0b73 stage=5 gs=300 children=2 childStage=1 [b67f7913-e79f-4740-bb92-90b0653384a3 dx=-0.50, 25cfd596-25fb-4a8c-a610-6b7011a0f059 dx=0.50]`, two stage-1 BODY lines for dg9 `maxHP=240.000 gs=300` (speed defect). `ENTITIES CREEPER count=6 radius=16`, not 2 |

---

## BEN'S ROWS — BY SIGHT, AFTER `GATE-dragon-split.md`'s S1 AND S2. EACH ANSWER IS ONE WORD

**SURVIVAL, difficulty normal (the run sheet).** Stand on flat open ground.

### G1 — one Enderman killed beside a small Phage makes it visibly bigger (SURVIVAL)

`/rpg spawn phage` · `/data merge entity @e[type=creeper,limit=1,sort=nearest] {NoAI:1b}` · step back three blocks ·
`/summon enderman ~1 ~ ~ {NoAI:1b}` · look at the Enderman · `/rpg mobdamage 199` · hit it once · look at the Phage.

| question | READING |
|---|---|
| Did the small Phage visibly get bigger when the Enderman died? **yes / no** | Ben: "yes" |

### G2 — after four, it is the big one (SURVIVAL)

Repeat these four lines three more times, staying where you are: `/summon enderman ~1 ~ ~ {NoAI:1b}` · look at the
Enderman · `/rpg mobdamage 199` · hit it once.

| question | READING |
|---|---|
| Is the Phage now the big one, about three blocks tall like B2's? **yes / no** | Ben: "yes" |

Cleanup: `/kill @e[type=creeper,distance=..20]` — **this kills a stage 5, so it splits; type the same line once more**
to remove the two small ones. `/kill @e[type=enderman,distance=..20]` removes any Enderman left.

**Why `/rpg mobdamage 199`:** a vanilla Enderman at gear score 100 has 40 × 5 = 200 custom health (PLAN-mob-scaling.md
M1, M2), so 199 leaves one, and one hit kills it where it stands.

---

## READINGS NOTE, 2026-11-15: THE STACKED BOOT OF `bda8e89c`. THE ACCOUNT; THE OTHER TWO GATES CARRY POINTERS HERE

**Booted** 21:34 on Ben's "ready"; backup `rpg-run-backups/2026-11-15-pre-dragon-stack` (Rpg 124 files 368857 bytes,
world 193 files 204964733 bytes, `diff -rq lines: 0` each); `12:  dev-pack-on-join: true`. LOGIN 21:42:29. Stop clean
at 21:59:42 (`Stopping server`, `Saving worlds`, `All dimensions are saved`; no java process left). **R0a-R0d PASS.**
Difficulty read out by Ben at the start: **hard**; at the end: **normal**. Ben's gear score was set to 500 by his own
`/rpg gearscore set 500` at 21:48:26, before every B, S and G row.

**No prediction above was edited.** The READING cells were written after the stop, from the log.

**`dragon-bodies` ran TWICE.** 21:43:31 on hard, before `/difficulty normal` (Ben was then killed: `BaronVonYeetus was
slain by Skreel`, 21:43:50); and 21:44:44 on normal. The DB readings are the SECOND run's; the first run's lines have
the same shape.

**Three defects, read at this boot. None is fixed here.**

1. **THE SPEED DEFECT (R1, code).** Every BODY line reads `speedDefault=0.7000`, and every `speedBase` is `0.7000 ×
   speedX`. `MobBodyApplier`'s default comes from `AttributeInstance.getDefaultValue()`, which returns the ATTRIBUTE's
   default (0.7 for movement speed), not the entity type's base (0.25 for a creeper, 0.3 for a spider). So every
   dragon mob moves at 2.8× (creeper, wither skeleton) or about 2.3× (spider) the speed the plan ruled. *The 0.7 is
   READ from the log; that `getDefaultValue()` is the source is READ from the code (`MobBodyApplier` and the probe both
   call it); the multipliers are DERIVED.* The unit tests model a default of 0.25, so they cannot see it. This is why
   every BODY row that predicts speed is NOT AS PREDICTED, and very likely why the Skreel killed Ben.
2. **THE TAG RACE (selftest, R1's pattern, copied in R2 and growth).** `/tag @e[type=…,distance=..1.5,sort=nearest,limit=1]`
   right after `/rpg spawn` repeatedly tagged the PREVIOUS row's mob, which still stood on the same spot
   (`REPLY "Target either already has the tag or has too many tags" [red]` on the follow-up tag lines). The new mob was
   never tagged, so `NoAI` never reached it, the row-start clean never removed it, and it walked, fought and was grown
   in later rows. *Which half of the race loses (the clean's removal not yet visible to the selector, or a tie in
   `sort=nearest` at distance 0) was NOT diagnosed.* It explains DB3, DB4, DB6, DB7's and DB11's counts, DS2-DS6,
   DG2, DG4-DG9's extra lines and counts, and the `mobsRemoved=0` restores.
3. **DS1: the iron sword's hit did not kill** a stage 5 at 1/480. The row's own INFERRED caveat; why was not read.

**What held despite them, read in this log:** growth by one stage per Void death, at the right heights, maxima and
gear score (DG1); two Phages from one death (DG2); 6.00 inclusive, 6.05 not (DG3); a zombie's death grows nothing
(DG4); a Phage's death grows another (DG5); **a stage 5's death grows no GROWTH line naming either child (DG6)**; no
`from=5` GROWTH line anywhere; a grown stage 5 splits (DG9); one split per death (DS5); the refusals' wording (DB11).

**An unruled interaction, read after the run:** Ben's single `/kill @e[type=creeper,distance=..40]` at the end of the
growth run killed four stage 5s in one command. Each split, and the OTHER parents' deaths then grew those new
children (`GROWTH mob=phage id=86b761f3-… from=1 to=2 … from=2 to=3 … from=3 to=4`, from three different parents).
The rule as built allows it: only a creature's own children are excluded, and only by order. **Killing a pack at
once leaves bigger children.** For Ben: Q30.

**Ben's B6, verbatim:** *"it stayed swelled until I move away from it"*, which is not the predicted swell-and-snap-back.

**Ben's proposals at this boot (for the seat, not built):** automate the by-sight rows that a server reading can
answer (B5, B6, S1, S2), keep two or three sight rows for size; and an `ASK` step that posts a row's question in chat
during the run and logs Ben's typed `yes`/`no` as `ANSWER <row> …`. Meanwhile his chat lines already reach the log
as `<BaronVonYeetus> …`.

> **DATED NOTE, 2026-11-15, after the first boot (`feat/dragon-fix`, Part A).** **This gate's R0 and run sheet are
> SUPERSEDED by `GATE-dragon-fix.md`**, the new stack top, which holds the ONE R0 and the ONE run sheet of the next
> boot. Every AUTO row here that read NOT AS PREDICTED is re-predicted there as a new row named for it (`…R2`); the
> predictions and readings above stay as they are. **The seat's misses, in its words, and the boot's departures are
> recorded in `PLAN-dragon-mobs.md`, RULINGS, *THE FIRST BOOT OF THE STACK*:** the seat passed this stack's speed
> predictions on arithmetic from `speedDefault=0.25` without the jar read for what `AttributeInstance#getDefaultValue`
> returns; the seat passed #190 without seeing that B2's and B6's cleanups split a stage 5; and the run sheet let a
> hostile mob stand beside Ben before the difficulty was set.
