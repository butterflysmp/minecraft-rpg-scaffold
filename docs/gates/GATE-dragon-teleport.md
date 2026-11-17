# GATE — THE KNELL'S TELEPORT AFTER ITS BEAM LANDS (STEP R6), AND THE STACKED BOOT OF R5 AND R6

**Status: NOT RUN.** Every prediction below was written **before** any boot of `feat/dragon-teleport`, and this file
is **committed before the boot**. Readings go **beside** a prediction, never over it, and a prediction is not edited
once its row has been read; a correction is a dated note. **NO BOOT until the seat has diffed the code AND this gate,
and Ben says ready.**

**READ 2026-11-17, at the stacked boot of `e52e54b6`: R0a-R0d PASS, all fifteen rows AS PREDICTED; readings beside each prediction, and the READINGS NOTE at the end.**

**This PR is the STACK TOP** (`feat/dragon-teleport` on `feat/dragon-beam`, PR #195, which is passed and unbooted).
**The ONE R0 and the ONE run sheet of the stacked boot are HERE.** `GATE-dragon-beam.md`'s R0 and run sheet are
superseded (its dated note); its AUTO rows and KQ1 are still read in this boot, by the first selftest command.

**What this gate reads.** Step R6 of `PLAN-dragon-mobs.md` §5.R (N8): 12 ticks after the Knell's beam HITS its target,
the Knell moves to the circle around the target's feet at the horizontal distance it stood, 30 to 70 degrees around,
the side drawn; 5 spots a side, then the other side, then it stays (the seat's T-a); ground within 3 blocks of the
target's feet (T-b); sight of the target required (T-c). Its scenarios are `selftest/GATE-dragon-teleport.yml`.

## GAME MODE, DIFFICULTY, GEAR SCORE

- **SURVIVAL on every row.** `/rpg selftest` refuses outside SURVIVAL.
- **The difficulty is set by the selftest:** each gate's setup `difficulty: normal`; the run's `DIFFICULTY found=…
  set=NORMAL` and `RESTORED … difficulty=<what it found>`.
- **Gear score 100 on every Knell:** a hit is 35.000 before defence.

## THE PLAYER CANNOT DIE IN ANY ROW — THE ARITHMETIC, ROW BY ROW

**The floor:** his maximum is at least 100. Every row heals him first (`rpg heal 1000000`). **No Knell can reach him:**
a Wither tick from a melee hit is priced in proportion to his custom maximum (`RerouteDamagePrice` →
`DamageScale.toCustom`), so melee is ruled out by staging, not bounded by arithmetic. **A teleport does not change
that**, because every Knell here either cannot walk at all or cannot walk at its own speed:

- **Every row but KB11R2: the Knell is FROZEN** (NoAI from its pre-spawn consumer). It cannot walk or attack (READ,
  §7 F-B: `travel` and the goals need effective AI). **A teleport still moves it** (`Entity#teleport` sets the position
  directly, READ below), **and it is frozen where it lands.**
- **KB11R2: the one LIVE Knell, its `movement_speed` BASE set to 0** by a vanilla `attribute` command in the same step
  pass as its spawn. It keeps its AI, so its OWN cycle runs (I23 does not stop it), but it cannot walk: its every
  position is where it spawned or where a teleport put it, and **a teleport keeps its horizontal distance to him**
  (the radius, 8 blocks), far outside a melee reach (about 0.83 beyond its 1.12-wide box, INFERRED from vanilla's
  attack box, not read). INFERRED, not read: that a zero base speed stops a mob's own walking entirely (the attribute
  multiplies its travel input).

| row | what can hit him after a heal | the worst case, before defence |
|---|---|---|
| KB10R2, TP2-TP4, TP6-TP12 | one forced beam that hits | **35** |
| TP5 | one forced beam that misses | **0** predicted (35 if it hit) |
| TP13 | one forced beam that hits, then the Knell is killed | **35** |
| KB11R2 | the live Knell's OWN beams, at least 112 ticks apart (a 12-tick telegraph after a wait of at least 100 from the last fire), with a heal every 100 ticks | **35 between two heals**: two beams cannot fall in one 100-tick interval |
| TQ1 | one forced beam | **35** |

**THE BEAM GATE'S ROWS IN THIS BOOT:** unchanged (`GATE-dragon-beam.md`, THE PLAYER CANNOT DIE): every Knell that can
hit him there is frozen or penned, and the one live penned row (KB2) ends, and its Knell is removed by the next row's
clean, before its teleport is due (below, THE ROWS ALREADY STACKED). KB11's live penned Knell, which a hit would have
moved OUT of its pen, no longer runs: it is KB11R2 here.

## WHAT THE ROWS STAND ON — EACH FACT, AND ITS MARK

- **T1. The trigger, READ in our code:** `MobTeleporter` is a `MobBeamer.Listener`, told only inside
  `MobBeam.report(...).ifPresent(...)`, which is empty on a miss (`MobBeamTest.aMissRaisesNoReport`,
  `DragonBeamWiringTest`). No damage or projectile listener of ours moves a Knell, and no main source names `dodge`
  (`DragonTeleportWiringTest.onlyTheBeamsReportMovesAKnell`).
- **T2. The delay, READ in our code:** a `RepeatingTask` of period `delay_ticks` (12), started inside the FIRE step on
  the Knell's own entity scheduler (`PaperScheduler.onEntityLater` → `runDelayed(..., delay)`), running its body once.
  **So the TELEPORT line's `tick=` is the FIRE line's + 12.** Its `isActive` check stops it, unmoved, when the Knell is
  dead or invalid, and `onStop` then logs `TELEPORT skipped … reason=knell-gone`; a removal first reaches it through
  `MobBeamer.onRemove`, the same line.
- **T3. The landing, EXECUTED in jshell** through the same expressions `BeamTeleport.column` runs: the Knell's offset
  from his feet turned by the angle, RIGHT `(x cos a − z sin a, x sin a + z cos a)`. For a Knell 8 blocks due east:
  right 50 → `(+5.142300877, +6.128355545)`; left 50 → `(+5.142300877, −6.128355545)`; right 30 →
  `(+6.928203230, +4.000000000)`; right 70 → `(+2.736161147, +7.517540966)`; the right side's spread 30, 43.333,
  56.667, 70. For a Knell at `(+8, 0)` from a man who then stands 2 north: radius `8.246211251`, bearing
  `14.036243468` degrees, right 50 → `(+3.610211991, +7.413930764)` from his new feet.
- **T4. VALID, READ in the server jar** (build 74; `javap -c -p -cp run/versions/26.1.2/paper-26.1.2.jar` on
  `org.bukkit.craftbukkit.entity.CraftEntity`, `org.bukkit.craftbukkit.CraftRegionAccessor`,
  `net.minecraft.world.level.CollisionGetter`):
  - `wouldCollideUsing(box)` is `!level.noCollision(thisEntity, aabb)`; `hasCollisionsIn(box)` is
    `!level.noCollision(aabb)`; and `noCollision(entity, aabb)` is `noBlockCollision(…) && noEntityCollision(…) &&
    noBorderCollision(…)`, the block half iterating `getBlockCollisions`, the block collision SHAPES (not liquids).
  - **INFERRED, not read:** that a box whose face lies exactly on a block's face does not collide (vanilla's shape
    test is a strict intersection). Our boxes are pulled in by 0.001 on every face, so the reading does not depend on
    it. That the ground slab (0.05 to 0.001 below the feet, over the body's footprint) meets the block below whenever
    that block has a collision shape: it lies inside that block.
  - **The staging was checked by EXECUTION, not by eye** (jshell over the rows' own block boxes, the Knell's body
    1.12 × 3.84 inset 0.001): in TP6 and TP7 every one of the 35 heights on each walled side overlaps glass or the
    floor; in TP10 the 30-degree spot's footprint meets a floor block outside the pit, and the 50-degree spot's
    footprint meets none.
- **T5. Sight, READ in our code:** `World#rayTraceBlocks` from the Knell's eye at the landing (feet + `getEyeHeight`,
  3.360 above the feet, `GATE-dragon-beam.md` B2) to his aim point (feet + 1.080), `FluidCollisionMode.NEVER`, passable
  blocks ignored: the beam's own rule (I13). **EXECUTED in jshell with core's `BeamLine.meets`:** in TP11 the 50-degree
  spot's line meets the glass and the 30-degree spot's does not; in TP8 (2 below) and TP9 (2 above) the line from the
  landing meets no block; no row's glass meets the beam itself.
- **T6. The move, READ in the server jar:** `CraftEntity.teleport0` builds a `TeleportTransition` with motion
  `Vec3.ZERO` and no relative flags; `Entity.teleport` → `teleportSameDimension` → `teleportSetPosition`: position,
  yaw, head yaw, pitch, `setDeltaMovement(ZERO)`, `clearMovementThisTick`. **Nothing touches navigation or the
  target**, so `MobTeleporter` stops the path after a move. A frozen Knell keeps its NoAI and does not fall (no
  `travel`), so its AT line after the move is the landing exactly.
- **T7. The instrument, READ in our code:** `teleport_draw: "<tag> <side> <angle>"` fixes the side and first angle of
  that Knell's NEXT decision only (`MobTeleporter.fix`, consumed by the decision); it logs `DRIVE teleport_draw tag=…
  <uuid> side=<s> first=<a>`. A row whose beam misses never consumes it.
- **T8. The beam rows' facts are `GATE-dragon-beam.md` B1-B11**, unchanged: a Knell 8 east, `dir=-0.961705 -0.274086
  0.000000`, a hit at `gap=0.000`, `length=12.259 clip=block`, `dealt=35.000`.

## THE ROWS ALREADY STACKED AND MERGED — THE AUDIT (the seat's ruling 9)

**Since this step, every beam that HITS moves its Knell 12 ticks after the fire**, and a LIVE Knell that moves is no
longer where its pen or its distance put it. Every row with a Knell was read for a hit, a position read after it, and
a clean that expects it where it was:

| row | a hit? | what R6 does to it | done |
|---|---|---|---|
| beam KB1 | yes, frozen | the row ends 20 ticks after the force, the fire 13-14 after it, so the move is due 25-26 after; the NEXT row's row-start clean removes the Knell a tick after its start (22 or so). **One `TELEPORT skipped mob=knell id=<the kb1 uuid> … reason=knell-gone` lands in KB2's bracket**; KB1's own lines are unchanged | noted in `GATE-dragon-beam.md`; KB1 unchanged |
| beam KB2 | recorded (it may clip on the pen) | if it hits, the same: the skip line in KB3's bracket, the live Knell removed before its move | the same |
| beam KB4, KB7 | yes, frozen | the row ends 23 after the force; the move is due 25-26 after, the removal about 24: **the skip line in KB5's and KB8's brackets.** The margin is one or two ticks (the removal's tick is INFERRED: the clean's `onEntity` runs a tick later, `GATE-dragon-fix.md` D2) | the same; a `TELEPORT … landed=` line in its place would mean the removal came later |
| beam KB3, KB5, KB6, KB8, KB9 | no | nothing | unchanged |
| beam **KB10** | yes | **its prediction "the Knell does NOT move after a hit" is now false** | **SUPERSEDED, never booted**: re-predicted here as **KB10R2**; its scenario left `GATE-dragon-beam.yml` |
| beam **KB11** | possibly (the pen) | **a hit would move a LIVE Knell out of its pen onto the circle around him, free to walk to him and melee: unsafe** | **SUPERSEDED, never booted**: re-predicted here as **KB11R2** (live, speed 0, no pen); its scenario left `GATE-dragon-beam.yml` |
| beam KQ1 | yes, twice (if he stands still the first time) | each hit moves the frozen Knell 12 ticks later, during the ASK, with the production draw: a `TELEPORT …` line in KQ1's bracket after each FIRE that hits; the second forced beam fires from wherever the first move put it (still 12 from him: the radius is kept) | noted; KQ1's predictions already RECORD the second beam's outcome |
| merged DB1R2, DB11R2, WS-C, WS2c | never (frozen, or no Knell) | a frozen Knell's own clock never begins a beam (I23), so it never hits, so it never moves | unchanged |
| **13 closed manual gates** with `rpg spawn knell` | a live Knell's own beam may hit the reader | it now also MOVES 12 ticks later | **a finding**, carried from #195: not re-predicted, not on any run sheet |

## CARRIED STATE, DONE BY THE BOOTER BEFORE THE BOOT, IN THIS ORDER

1. **No JVM running:** `Get-Process java -ErrorAction SilentlyContinue` prints nothing.
2. **A fresh dated backup**, with no server running, in ONE bash invocation from the repo root:

   ```bash
   B="/c/Users/Neb91/IdeaProjects/rpg-run-backups/$(date +%F)-pre-dragon-teleport"; mkdir "$B" && cp -a run/plugins/Rpg run/world "$B/" && for p in "run/plugins/Rpg $B/Rpg" "run/world $B/world"; do set -- $p; echo "$1: $(find "$1" -type f | wc -l) files $(find "$1" -type f -printf '%s\n' | awk '{s+=$1} END{print s}') bytes; $2: $(find "$2" -type f | wc -l) files $(find "$2" -type f -printf '%s\n' | awk '{s+=$1} END{print s}') bytes; diff -rq lines: $(diff -rq "$1" "$2" | wc -l)"; done
   ```

   If the folder exists (a boot the same day), add `-2` to its name. Report the folder, both pairs of counts, and
   `diff -rq lines: 0` for each.
3. **Check, do not assume:** `grep -n "dev-pack-on-join" run/plugins/Rpg/config.yml` must print
   `12:  dev-pack-on-join: true`. If it does not, STOP and report.
4. **Boot THIS branch's head** (the stack top, carrying R5 below it): `./scripts/dev-server.sh --refresh-content`,
   announcing "booting now", on Ben's "ready".
5. **R0 binds to THIS boot:** read R0 only on a log whose `[Rpg] Build:` line names this branch's head. **Readings count
   only after Ben's LOGIN in that boot's log.**

## THE RUN SHEET — BEN TYPES THESE, IN THIS ORDER. EVERY LINE IS LITERAL

**Two selftest commands, not one, because the two steps are two gates with two scenario files**, and each has its own
three questions; folding R6's rows into R5's file would rename rows that the seat has already diffed.

1. `/gamemode survival` — **alone, the first line after login.**
2. `/rpg selftest dragon-beam` — **ONCE.** Stand still and touch nothing, **except in its last part**: a Knell twelve
   blocks east of you will beam you twice, and a gold line in chat says what to do before each. **The first time,
   stand still and watch**: a thin purple line from the Knell to you is the warning, then a thick cyan line is the
   beam. **The second time, step sideways (A or D) as soon as you see the purple line.** After each hit the Knell may
   now move to a new spot: that is step R6. Three questions; answer each `yes` or `no`. Wait for the chat to say the
   run finished.
3. `/rpg selftest dragon-teleport` — **ONCE.** Stand still and touch nothing, **except in its last part**: a Knell
   twelve blocks east of you will beam you once. **Stand still; after the hit, watch where the Knell goes.** Three
   questions; answer each `yes` or `no`. Wait for the chat to say the run finished.
4. `/stop`

## R0 — THE BUILD, FOR THE STACKED BOOT. READ IT FIRST. IF ANY R0 FAILS, STOP

### R0a — the build line names THIS branch's head

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, this PR's head as `git ls-remote origin refs/heads/feat/dragon-teleport` prints it, shortened. **Not** `feat/dragon-beam`'s `1bfee281`, not master's `8671ab16`; not `-dirty`, not `unknown` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | **PASS, 2026-11-17.** `[04:24:10] [Server thread/INFO]: [Rpg] Build: e52e54b6`; `git ls-remote` printed `e52e54b6aabe58086e54486acc573a9275a878f0 refs/heads/feat/dragon-teleport`. `Done (6.118s)!` at 04:24:11; Ben's LOGIN at 04:25:16 |

### R0b — the jar carries BOTH steps

| prediction | instrument | READING |
|---|---|---|
| PRESENT in `content/mobs/knell.yml`: R5's `  cycle_ticks: { min: 100, max: 200 }`, `  telegraph_ticks: 12`, `  damage: 35`, and R6's `  delay_ticks: 12`, `  arc_degrees: { min: 30, max: 70 }`. Classes, each in its DECOMPRESSED entry: R5's `MobBeamer.class` `knell-beam`, `MobBeamLines.class` `BEAM telegraph mob=`; R6's `MobTeleporter.class` `knell-teleport`, `MobTeleportLines.class` `TELEPORT skipped mob=`, `SelfTestBeam.class` `DRIVE teleport_draw tag=`. Keys: `GATE-dragon-beam.yml` `KB1:` and `KQ1:` PRESENT, `KB10:` and `KB11:` ABSENT (moved by R6); `GATE-dragon-teleport.yml` `KB10R2:`, `KB11R2:`, `TQ1:` PRESENT. Controls: `NoSuchTextControl` ABSENT in `MobTeleporter.class`; `GATE-dragon-blast.yml` `BQ1R2:` PRESENT | the scan below | **PASS, 2026-11-17.** All nineteen lines as predicted: PRESENT the five `knell.yml` lines, `knell-beam`, `BEAM telegraph mob=`, `knell-teleport`, `TELEPORT skipped mob=`, `DRIVE teleport_draw tag=`, `dragon-beam KB1:`, `dragon-beam KQ1:`, `dragon-teleport KB10R2:`, `KB11R2:`, `TQ1:`, `dragon-blast BQ1R2:` (the key control); ABSENT `NoSuchTextControl` (the text control), `dragon-beam KB10:`, `dragon-beam KB11:` |

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path 'run\plugins\rpg-0.1.0-SNAPSHOT.jar'))
function Bytes($name) { $e = $zip.GetEntry($name); if ($e -eq $null) { return $null }; $s = $e.Open(); $m = New-Object IO.MemoryStream; $s.CopyTo($m); $s.Close(); [Text.Encoding]::GetEncoding(28591).GetString($m.ToArray()) }
$k = Bytes 'content/mobs/knell.yml'
foreach ($l in '  cycle_ticks: { min: 100, max: 200 }', '  telegraph_ticks: 12', '  damage: 35', '  delay_ticks: 12', '  arc_degrees: { min: 30, max: 70 }') {
  if ($k -match ("(?m)^" + [regex]::Escape($l) + "\s*$")) { "PRESENT knell.yml '$l'" } else { "ABSENT  knell.yml '$l'" }
}
foreach ($c in @(@('health/MobBeamer.class', 'knell-beam'), @('health/MobBeamLines.class', 'BEAM telegraph mob='), @('health/MobTeleporter.class', 'knell-teleport'), @('health/MobTeleporter.class', 'NoSuchTextControl'), @('health/MobTeleportLines.class', 'TELEPORT skipped mob='), @('selftest/SelfTestBeam.class', 'DRIVE teleport_draw tag='))) {
  $b = Bytes "io/github/butterflysmp/rpg/paper/$($c[0])"
  if ($b -eq $null) { "MISSING $($c[0])" } elseif ($b.Contains($c[1])) { "PRESENT $($c[0]) '$($c[1])'" } else { "ABSENT  $($c[0]) '$($c[1])'" }
}
foreach ($q in @(@('dragon-beam', 'KB1'), @('dragon-beam', 'KQ1'), @('dragon-beam', 'KB10'), @('dragon-beam', 'KB11'), @('dragon-teleport', 'KB10R2'), @('dragon-teleport', 'KB11R2'), @('dragon-teleport', 'TQ1'), @('dragon-blast', 'BQ1R2'))) {
  $y = Bytes "selftest/GATE-$($q[0]).yml"
  if ($y -eq $null) { "MISSING selftest/GATE-$($q[0]).yml" } elseif ($y -match "(?m)^$($q[1]):\s*$") { "PRESENT $($q[0]) $($q[1]):" } else { "ABSENT  $($q[0]) $($q[1]):" }
}
$zip.Dispose()
```

**What each other jar reads, so the scan tells them apart:** a jar of `feat/dragon-beam` (`1bfee281`) reads ABSENT on
R6's two `knell.yml` lines, MISSING on `MobTeleporter.class`, `MobTeleportLines.class` and `GATE-dragon-teleport.yml`,
ABSENT on `DRIVE teleport_draw tag=`, and PRESENT on `KB10:` and `KB11:`. A jar of master reads ABSENT or MISSING on
everything of both steps. **The key needle is anchored at `:` and the line's end, so `KB1:` cannot match `KB10:`.**

### R0c — the Loaded line, and nothing refused

| prediction | instrument | READING |
|---|---|---|
| **`[Rpg] Loaded 11 abilities, 29 visuals, 5 statuses, 7 elements, 11 enchants, 3 pools, 6 fragments, 6 aspects, 16 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 5 mobs, 1 mob elements, 3 recipes, 1 ores, 4 vein sizes`**, verbatim `GATE-dragon-blast-2.md` R0c's reading: neither step adds a content file. No `Refusing`, `Skipping`, `SEVERE` or `Exception`, and no `[Rpg]` `WARN`, beyond `volley_stone`'s known pair. **A `Skipping malformed mob 'knell.yml'` line would mean its beam or teleport block was refused**, the Loaded line then reading `4 mobs` | `Select-String run\logs\latest.log -Pattern 'Loaded ','Refusing','Skipping','SEVERE','WARN','Exception','Content:'` | **PASS, 2026-11-17.** The Loaded line verbatim as predicted; the only `[Rpg]` WARNs are `volley_stone`'s pair (`Content: weapon 'volley_stone' trigger 'left_click' authors cooldown_ticks 0…` and `1 content problem(s)…`); the other WARN lines are Paper's version banner. No `Refusing`, `Skipping`, `SEVERE` or `Exception`, before or after the runs |

### R0d — the dev marker reached the JVM, and every scenario loaded

| prediction | instrument | READING |
|---|---|---|
| Exactly one line **`[Rpg] Selftest: dev server, 115 scenarios across 17 gates [melee-cell, level, nexus-polish, legacy-a, legacy-b, wither-status, withered-shortbow, sunder, deft-hands, mining-auto, nexus-back, dragon-bodies, dragon-split, dragon-growth, dragon-blast, dragon-beam, dragon-teleport]`**, and **no** `Refusing selftest` line (a malformed `teleport_draw` is refused at load, by name) | `Select-String run\logs\latest.log -Pattern 'Selftest:','Refusing selftest'` | **PASS, 2026-11-17.** `[Rpg] Selftest: dev server, 115 scenarios across 17 gates [melee-cell, level, nexus-polish, legacy-a, legacy-b, wither-status, withered-shortbow, sunder, deft-hands, mining-auto, nexus-back, dragon-bodies, dragon-split, dragon-growth, dragon-blast, dragon-beam, dragon-teleport]`, once; no `Refusing selftest` |

**The 115 is RECOUNTED from the files at this branch's head:** in `paper/src/main/resources/selftest/`,
`grep -cE '^  - ' index.yml` prints **17**; `cat $(grep -E '^  - ' index.yml | tr -d '\r' | sed 's/  - /GATE-/;
s/$/.yml/') | grep -cE '^[A-Za-z0-9-]+:'` prints **132**; the same with `'^setup:'` prints **17**: 132 − 17 = **115**.
**The control that tells this build apart: at `feat/dragon-beam` `1bfee281` the same commands print 16, 118, 16:
102**, and at master `8671ab16` 15, 105, 15: 90. `SelfTestScenariosTest` pins the 115 and every row name.

---

## THE AUTO ROWS — SERVER ONLY, SURVIVAL, DIFFICULTY NORMAL

**The log instrument for every row:**
`Select-String run\logs\latest.log -Pattern 'SELFTEST dragon-teleport|\[Rpg\] BEAM|\[Rpg\] TELEPORT'`, read after
Ben's LOGIN, inside the row's own `SELFTEST dragon-teleport <row> START … DONE` bracket. A `[Rpg]` line carries no row
name: it belongs to the bracket it falls in, and names its Knell's uuid.

**In EVERY row below (and not repeated in each):** after the row's `CLEAN …` line, **`LEFTOVER living=0 radius=16`**
(a moved Knell is still inside the clean's 48 blocks); for each mob, `DRIVE spawn tag=<tag> <uuid> knell frozen` (or
`live`) and the reply `REPLY "Spawned Knell (wither_skeleton, 360 HP, GS 100 given)" [aqua, gray]` (RECORD it); the
`DRIVE teleport_draw tag=<tag> <uuid> side=<s> first=<a>` line, printing the row's own side and angle to three
places; `DRIVE beam tag=<tag> <uuid>`. **No BEAM or TELEPORT line names a Knell that is not the row's own, except the
one KB11R2 can leave in TQ1's bracket (its row).**

**Writing F for his aligned feet** (read from the TELEGRAPH line's `to=` as F + (0, 1.080, 0)) **and K = F + (8, 0, 0)
for the Knell's**, a forced HIT reads, exactly as `GATE-dragon-beam.md` KB1: one TELEGRAPH (`forced=true`, `at=K`,
`dir=-0.961705 -0.274086 0.000000`) at tick T; one FIRE at T+12, `length=12.259 clip=block tolerance=1.00 tested=1
[player <Ben's uuid> gap=0.000 hit] dealt=35.000`; one REPORT; one HURT `before=35.000`, `after = 35 × 100 / (100 +
Dp)`. **Then a TELEPORT line at T+24** (T2):

`[Rpg] TELEPORT mob=knell id=<the Knell> target=player <Ben's uuid> tick=<T+24> from=<K> center=<his feet then>
radius=<r> bearing=<b> side=<s> first=<a> tried=<n> [<side> <angle> <verdict>, …] landed=<x y z>|none moved=<true|->`,
a verdict `ok y=<h> at=<x y z>`, `no-footing blocked=<n> ungrounded=<n>`, `no-sight y=<h>` or `unloaded`.

### KB10R2 — re-predicts GATE-dragon-beam.md KB10: after a hit, it lands 50 degrees to the right, 8 from him (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) `AT tag=kb10r2 <uuid>` at K before the force. (2) The forced hit, as above. (3) **Exactly one** `TELEPORT … tick=<T+24> from=<K> center=<F> radius=8.000 bearing=0.000 side=right first=50.000 tried=1 [right 50.000 ok y=<F.y> at=<F + (5.142, 0, 6.128)>] landed=<F + (5.142, 0, 6.128)> moved=true`. (4) 40 ticks after the force, `AT tag=kb10r2 <uuid>` at **F + (5.142, 0, 6.128)**: its horizontal distance to him 8.000 (unchanged) and its bearing from him turned 50.000 degrees toward south | T1-T3, T6, T7, T8 | **AS PREDICTED, 2026-11-17.** F = -98.500 63.000 6.500 (the REPORT `to=-98.547 64.066 6.500`). (1) `AT tag=kb10r2 470dc3df-fc90-4f1f-8ea0-f4226b6ec2da x=-90.500 y=63.000 z=6.500` = K. (2) FIRE `tick=5285 … gap=0.000 hit] dealt=35.000`, one REPORT, `HURT before=35.000 after=31.818 defense=10.000 dealer=470dc3df-fc90-4f1f-8ea0-f4226b6ec2da dealerType=wither_skeleton/knell`. (3) `[Rpg] TELEPORT mob=knell id=470dc3df-fc90-4f1f-8ea0-f4226b6ec2da target=player b6ae27e9-6ca4-4bb5-a1fb-73cbadbabdd5 tick=5297 from=-90.500 63.000 6.500 center=-98.500 63.000 6.500 radius=8.000 bearing=0.000 side=right first=50.000 tried=1 [right 50.000 ok y=63.000 at=-93.358 63.000 12.628] landed=-93.358 63.000 12.628 moved=true`, the FIRE + 12. (4) `AT tag=kb10r2 470dc3df-fc90-4f1f-8ea0-f4226b6ec2da x=-93.358 y=63.000 z=12.628` = F + (5.142, 0, 6.128) |

### TP2 — the other side: 50 degrees left (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The forced hit; `TELEPORT … radius=8.000 bearing=0.000 side=left first=50.000 tried=1 [left 50.000 ok y=<F.y> at=<F + (5.142, 0, −6.128)>] landed=<the same> moved=true`; the AT line at **F + (5.142, 0, −6.128)** | T3 | **AS PREDICTED, 2026-11-17.** `TELEPORT … tick=5347 … radius=8.000 bearing=0.000 side=left first=50.000 tried=1 [left 50.000 ok y=63.000 at=-93.358 63.000 0.372] landed=-93.358 63.000 0.372 moved=true`; `AT tag=tp2 c4b31b65-1052-4e9b-8c1c-deb565ade59c x=-93.358 y=63.000 z=0.372` = F + (5.142, 0, -6.128) |

### TP3 — the arc's near bound, 30 degrees right (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The forced hit; `TELEPORT … side=right first=30.000 tried=1 [right 30.000 ok y=<F.y> at=<F + (6.928, 0, 4.000)>] … moved=true`; the AT line at **F + (6.928, 0, 4.000)** | T3 | **AS PREDICTED, 2026-11-17.** `TELEPORT … tick=5397 … side=right first=30.000 tried=1 [right 30.000 ok y=63.000 at=-91.572 63.000 10.500] landed=-91.572 63.000 10.500 moved=true`; `AT tag=tp3 38d2d813-c81e-4f7c-87ce-4145234174f7 x=-91.572 y=63.000 z=10.500` = F + (6.928, 0, 4.000) |

### TP4 — the arc's far bound, 70 degrees right (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The forced hit; `TELEPORT … side=right first=70.000 tried=1 [right 70.000 ok y=<F.y> at=<F + (2.736, 0, 7.518)>] … moved=true`; the AT line at **F + (2.736, 0, 7.518)** | T3 | **AS PREDICTED, 2026-11-17.** `TELEPORT … tick=5447 … side=right first=70.000 tried=1 [right 70.000 ok y=63.000 at=-95.764 63.000 14.018] landed=-95.764 63.000 14.018 moved=true`; `AT tag=tp4 d2aadb35-6a0d-4973-9369-3248fede8741 x=-95.764 y=63.000 z=14.018` = F + (2.736, 0, 7.518) |

### TP5 — a forced MISS moves nothing (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) AT at K. (2) One TELEGRAPH; `CMD /tp @s ~ ~ ~3` after it; one FIRE `… [player <Ben's uuid> gap=2.700 miss] dealt=none`; **no REPORT, no HURT**. (3) **No TELEPORT line of any kind for it** through the 40 ticks. (4) AT at **K**, unchanged. The `teleport_draw` it was given is never consumed | T1; `GATE-dragon-beam.md` KB3 | **AS PREDICTED, 2026-11-17.** `AT tag=tp5 e65eddc3-6a73-46e5-9411-1332506760b1 x=-90.500 y=63.000 z=6.500` before; FIRE `tick=5485 … [player b6ae27e9-6ca4-4bb5-a1fb-73cbadbabdd5 gap=2.700 miss] dealt=none`; no REPORT, no HURT; **no TELEPORT line of either kind for e65eddc3** (`grep -c` 0); the same AT line after |

### TP6 — every right spot walled: it lands on the left (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The forced hit (the glass does not touch the beam, T5); `TELEPORT … side=right first=50.000 tried=6 [right 50.000 no-footing blocked=7 ungrounded=0, right 30.000 no-footing blocked=7 ungrounded=0, right 43.333 no-footing blocked=7 ungrounded=0, right 56.667 no-footing blocked=7 ungrounded=0, right 70.000 no-footing blocked=7 ungrounded=0, left 50.000 ok y=<F.y> at=<F + (5.142, 0, −6.128)>] landed=<the same> moved=true`; AT at **F + (5.142, 0, −6.128)** | T4 (35 of 35 heights walled, executed), T3 | **AS PREDICTED, 2026-11-17.** `TELEPORT … tick=5550 … side=right first=50.000 tried=6 [right 50.000 no-footing blocked=7 ungrounded=0, right 30.000 no-footing blocked=7 ungrounded=0, right 43.333 no-footing blocked=7 ungrounded=0, right 56.667 no-footing blocked=7 ungrounded=0, right 70.000 no-footing blocked=7 ungrounded=0, left 50.000 ok y=63.000 at=-93.358 63.000 0.372] landed=-93.358 63.000 0.372 moved=true`; `AT tag=tp6 … x=-93.358 y=63.000 z=0.372` |

### TP7 — walled in on every candidate: none, and it stays (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The forced hit; `TELEPORT … side=right first=50.000 tried=10 [right 50.000 no-footing blocked=7 ungrounded=0, right 30.000 …, right 43.333 …, right 56.667 …, right 70.000 …, left 50.000 …, left 30.000 …, left 43.333 …, left 56.667 …, left 70.000 no-footing blocked=7 ungrounded=0] landed=none moved=-` (every entry `no-footing blocked=7 ungrounded=0`); AT at **K** before and after | T4 | **AS PREDICTED, 2026-11-17.** `TELEPORT … tick=5600 … side=right first=50.000 tried=10 [` ten entries, every one `no-footing blocked=7 ungrounded=0`, ending `left 70.000 no-footing blocked=7 ungrounded=0] landed=none moved=-`; `AT tag=tp7 8fe56d88-6ea4-4f3a-bade-5b4e401eef7a x=-90.500 y=63.000 z=6.500` before and after |

### TP8 — a pit two deep: it stands on the pit's floor, 2 below his feet (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The forced hit; `TELEPORT … tried=1 [right 50.000 ok y=<F.y − 2> at=<F + (5.142, −2, 6.128)>] landed=<the same> moved=true` (heights tried 0, +1, −1, +2 with no ground under them, then −2 on the pit's stone); AT at **F + (5.142, −2, 6.128)** | T4, T5 (sight from the pit, executed) | **AS PREDICTED, 2026-11-17.** `TELEPORT … tick=5650 … tried=1 [right 50.000 ok y=61.000 at=-93.358 61.000 12.628] landed=-93.358 61.000 12.628 moved=true`; `AT tag=tp8 4f0085d8-8996-4b72-8ffb-3434c9493a60 x=-93.358 y=61.000 z=12.628` = F + (5.142, -2, 6.128) |

### TP9 — a step two high: it stands on top, 2 above his feet (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The forced hit; `TELEPORT … tried=1 [right 50.000 ok y=<F.y + 2> at=<F + (5.142, 2, 6.128)>] … moved=true` (0 and +1 blocked by the step, −1 by the floor, then +2 on its top); AT at **F + (5.142, 2, 6.128)** | T4, T5 | **AS PREDICTED, 2026-11-17.** `TELEPORT … tick=5700 … tried=1 [right 50.000 ok y=65.000 at=-93.358 65.000 12.628] landed=-93.358 65.000 12.628 moved=true`; `AT tag=tp9 e78c6eae-6e2b-415f-bab5-8a9126451a54 x=-93.358 y=65.000 z=12.628` = F + (5.142, 2, 6.128) |

### TP10 — ground four down, beyond the 3-block tolerance: refused; it lands on the next spot (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The forced hit; `TELEPORT … tried=2 [right 50.000 no-footing blocked=0 ungrounded=7, right 30.000 ok y=<F.y> at=<F + (6.928, 0, 4.000)>] … moved=true`; AT at **F + (6.928, 0, 4.000)**. TP8 is its control (the same pit, 2 deep, is stood in) | T4 (both footprints, executed) | **AS PREDICTED, 2026-11-17.** `TELEPORT … tick=5750 … tried=2 [right 50.000 no-footing blocked=0 ungrounded=7, right 30.000 ok y=63.000 at=-91.572 63.000 10.500] landed=-91.572 63.000 10.500 moved=true`; `AT tag=tp10 … x=-91.572 y=63.000 z=10.500` |

### TP11 — ground, but no sight of him: refused by sight; it lands on the next spot (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The forced hit; `TELEPORT … tried=2 [right 50.000 no-sight y=<F.y>, right 30.000 ok y=<F.y> at=<F + (6.928, 0, 4.000)>] … moved=true`; AT at **F + (6.928, 0, 4.000)**. KB10R2 is its control (the same spot, no wall, is taken) | T5 (executed: the wall meets the 50-degree line and not the 30's) | **AS PREDICTED, 2026-11-17.** `TELEPORT … tick=5800 … tried=2 [right 50.000 no-sight y=63.000, right 30.000 ok y=63.000 at=-91.572 63.000 10.500] landed=-91.572 63.000 10.500 moved=true`; `AT tag=tp11 … x=-91.572 y=63.000 z=10.500` |

### TP12 — he steps 2 north during the delay: the circle is around where he is at the end (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The forced hit at F; `CMD /tp @s ~ ~ ~-2` after the FIRE line and before T+24; `TELEPORT … center=<F + (0, 0, −2)> radius=8.246 bearing=14.036 side=right first=50.000 tried=1 [right 50.000 ok y=<F.y> at=<F + (3.610, 0, 5.414)>] … moved=true` (F + (0, 0, −2) + (3.610, 0, 7.414)); AT at **F + (3.610, 0, 5.414)**, 8.246 from his new feet | T3 | **AS PREDICTED, 2026-11-17.** FIRE `tick=5838`; `CMD /tp @s ~ ~ ~-2` after it; `TELEPORT … tick=5850 from=-90.500 63.000 6.500 center=-98.500 63.000 4.500 radius=8.246 bearing=14.036 side=right first=50.000 tried=1 [right 50.000 ok y=63.000 at=-94.890 63.000 11.914] landed=-94.890 63.000 11.914 moved=true`; `AT tag=tp12 541874c7-5c8e-4ad8-b63a-bcad1104da25 x=-94.890 y=63.000 z=11.914` = F + (3.610, 0, 5.414) |

### TP13 — the Knell is killed during the delay: nothing moves, and one line says why (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| The forced hit; `CMD /kill @e[tag=tp13]` 18 ticks after the force (after the FIRE, before T+24); **exactly one** `TELEPORT skipped mob=knell id=<the tp13 uuid> target=<Ben's uuid> reason=knell-gone`; **no TELEPORT decision line** | T2 | **AS PREDICTED, 2026-11-17.** TELEGRAPH `tick=5883`, FIRE `tick=5895 … gap=0.000 hit] dealt=35.000`, one HURT; `CMD /kill @e[tag=tp13]` after the FIRE; **exactly one** `[Rpg] TELEPORT skipped mob=knell id=746d35df-1fba-432e-b0c9-323a4d4b6996 target=b6ae27e9-6ca4-4bb5-a1fb-73cbadbabdd5 reason=knell-gone`; no decision line |

### KB11R2 — re-predicts GATE-dragon-beam.md KB11: the live cycle by bound, unbroken by a move (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) `DRIVE spawn tag=kb11r2f <uuid> knell frozen`, `DRIVE spawn tag=kb11r2 <uuid> knell live`, `CMD /attribute @e[tag=kb11r2,limit=1] minecraft:movement_speed base set 0` and its reply (RECORDED), `DRIVE teleport_draw tag=kb11r2 <uuid> side=right first=50.000`. (2) Its FIRST own TELEGRAPH: `forced=false`, **99 ≤ lived ≤ 201** (`GATE-dragon-beam.md` B9), `at=` K. (3) Its FIRE 12 later, a hit (`gap=0.000 hit`, the line open), its REPORT, one HURT `before=35.000`. (4) **A TELEPORT 12 after that FIRE, `side=right first=50.000 … landed=<F + (5.142, 0, 6.128)> moved=true`.** (5) **THE CLOCK UNBROKEN: its NEXT own TELEGRAPH `forced=false`, `at=` the landing (it cannot walk), with `lived` − the first's `lived` − 12 within 100 to 200** (both read from the one Knell's tick count in the same task order, so B9's ±1 cancels) (I2: a fresh 100-200 wait drawn AT the fire, which the move does not touch). (6) Every later own beam the same way: each TELEGRAPH's `lived` minus the previous one's minus 12 within 100 to 200; each FIRE 12 after; each hit's TELEPORT 12 after its FIRE, **its side and angle the production draw's, RECORDED**, its `radius=` 8.000 (the radius is kept). At least two own TELEGRAPHs fit in the 430 ticks; a third may. (7) **No BEAM or TELEPORT line names the kb11r2f uuid.** (8) Between any two `REPLY "Custom HP: …"` heal lines, at most one HURT. (9) A telegraph or a move still pending at the row's end is cancelled or skipped by the next row's clean, in TQ1's bracket, RECORDED | B9, I1, I2, I23; T2, T3, T6 | **AS PREDICTED, 2026-11-17.** (1) `DRIVE spawn tag=kb11r2f 8ef9e299-e014-4b85-b939-4b7f6f27e41f knell frozen`, `DRIVE spawn tag=kb11r2 77ef2cfa-7fac-48d4-8ddc-189a811c05b8 knell live`, `REPLY "The base value for attribute Speed for entity Knell set to 0" []`, `DRIVE teleport_draw tag=kb11r2 … side=right first=50.000`. (2) `BEAM telegraph … forced=false tick=6136 lived=181 at=-90.500 63.000 6.500`: 99 ≤ 181 ≤ 201. (3) FIRE `tick=6148 … gap=0.000 hit] dealt=35.000`, REPORT, one HURT. (4) `TELEPORT … tick=6160 … side=right first=50.000 tried=1 [right 50.000 ok y=63.000 at=-93.358 63.000 12.628] landed=-93.358 63.000 12.628 moved=true`. (5) **The clock unbroken:** `BEAM telegraph … forced=false tick=6268 lived=313 at=-93.358 63.000 12.628`: 313 − 181 − 12 = 120, within 100 to 200, at the landing. (6) FIRE `tick=6280 … gap=0.000 hit] dealt=35.000`; `TELEPORT … tick=6292 … radius=8.000 bearing=50.000 side=left first=37.703 tried=1 [left 37.703 ok y=63.000 at=-90.684 63.000 8.204] landed=-90.684 63.000 8.204 moved=true` (the production draw); no third TELEGRAPH in the 435 ticks; `AT tag=kb11r2 … x=-90.684 y=63.000 z=8.204`. (7) No line names 8ef9e299. (8) Two HURT lines, each between two heals. (9) Nothing pending at the end (TQ1's `CLEAN … removed=2`, no cancel or skip line) |

---

## BEN'S ROW — BY THE ASK STEP, SURVIVAL, DIFFICULTY NORMAL

**AUTO-PARTIAL** (`verification.md`, *AN AUTO READING IS A READING OF THE SERVER*): the BEAM and TELEPORT lines are the
server's half; **what he saw, and whether it felt fair, is his half, and only his answer reads it.** The run sheet
tells him in advance what to watch (step 3), and the selftest says it again in gold chat.

### TQ1 — did it move; could you find it; is that fair to a bow (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| **The server half.** A heal; the gold line; 60 ticks; one forced beam from a frozen Knell 12 east: a hit if he stood still (one REPORT, one HURT `before=35.000`). **No `teleport_draw`: the production draw.** 12 ticks after the FIRE, one TELEPORT, `radius=12.000 bearing=0.000`, its side, first angle, every candidate and the landing **RECORDED**, not predicted (an open floor: `tried=1` and `moved=true` are expected, not required). Then the three ASK lines with his answers, verbatim. **His half:** ASK 1 *"After it hit you, did it move to a new spot?"*; ASK 2 *"Could you find it again straight away?"*; ASK 3 *"Does that feel fair to someone fighting it with a bow?"* (yes/no each) | T1-T6; the sight and sound at each end are cfde822's sidestep's, READ (`PLAN-dragon-mobs.md` §3.1: soul and soul-fire particles, an amethyst step) | **Server half, 2026-11-17.** TELEGRAPH `forced=true tick=6451 lived=61 at=-86.500 63.000 6.500`; FIRE `tick=6463 … length=18.001 clip=block … gap=0.000 hit] dealt=35.000`; REPORT; `HURT before=35.000 after=31.818 …`; `TELEPORT … tick=6475 from=-86.500 63.000 6.500 center=-98.500 63.000 6.500 radius=12.000 bearing=0.000 side=left first=32.546 tried=1 [left 32.546 ok y=63.000 at=-88.385 63.000 0.044] landed=-88.385 63.000 0.044 moved=true`. **His half, verbatim:** ASK 1 `question="After it hit you, did it move to a new spot?" answer="yes"`; ASK 2 `question="Could you find it again straight away?" answer="yes"`; ASK 3 `question="Does that feel fair to someone fighting it with a bow?" answer="yes"` |

---

## READINGS NOTE, 2026-11-17 — THE STACKED BOOT OF `e52e54b6`

**What ran:** one boot of `feat/dragon-teleport` `e52e54b6` (`Done` 04:24:11); Ben's LOGIN 04:25:16; `/gamemode
survival`; `/rpg selftest dragon-beam` once (04:27:02 to 04:28:20); `/rpg selftest dragon-teleport` once (04:28:33 to
04:29:44); `/stop` 04:29:57, a clean stop (the chunk and I/O saves logged, the boot script exited 0, no JVM left). No
extra run. Each run read `DIFFICULTY found=NORMAL set=NORMAL` and restored `difficulty=NORMAL`.

**R0a-R0d PASS. All fifteen rows of this gate read AS PREDICTED**, with KB10R2 and KB11R2 the two re-predictions, and
TQ1's server half and Ben's three answers recorded. **Every TELEPORT came exactly 12 ticks after its FIRE (17 of 17)
and every FIRE exactly 12 after its TELEGRAPH (25 of 25), across both runs.** Every landing the gate predicted read to
three places. **The Knell's eye printed 3.360 above its feet in all 26 TELEGRAPH lines**, so `GATE-dragon-beam.md`
B2, INFERRED when written, now has its reading. Every HURT line (22 of them, both runs) is a Knell's beam,
`before=35.000`, `dealerType=wither_skeleton/knell`; nothing else hurt him. All 27 LEFTOVER lines read `living=0`.

**The two departures were in `GATE-dragon-beam.md`'s rows, and are recorded there:** KB2's control did not hold (its
penned Knell never walked), and KB4's Knell moved before the next row's clean removed it (the thin margin the seat
accepted). Both are diagnosed in that gate's READINGS NOTE; **nothing was changed.**
