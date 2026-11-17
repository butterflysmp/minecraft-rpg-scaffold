# GATE — THE KNELL'S BEAM (STEP R5): LOCK, ROOT, FIRE, AND THE REPORT OF A HIT

**Status: NOT RUN.** Every prediction below was written **before** any boot of `feat/dragon-beam`, and this file is
**committed before the boot**. Readings go **beside** a prediction, never over it, and a prediction is not edited once
its row has been read; a correction is a dated note. **NO BOOT until the seat has diffed the code AND this gate, and Ben
says ready.**

**READ 2026-11-17, in the stacked boot of `e52e54b6`** (R0 in `GATE-dragon-teleport.md`): KB2 HOLLOW for the root, KB4's Knell moved before the next clean, every other row as predicted; readings beside each prediction, and the READINGS NOTE at the end.

**2026-11-17: approved to merge by the review seat after the boot of e52e54b6; see GATE-dragon-teleport.md for the readings.**

> **DATED NOTE, 2026-11-17, the Knell follow-up (`GATE-dragon-knell-2.md`). NOTHING BELOW IS EDITED.** Every row of
> `selftest/GATE-dragon-beam.yml` is now that gate's, renamed with the next suffix: **KB1-KB9 and KQ1 are SUPERSEDED by
> KB1R2-KB9R2 and KQ1R2** (65 damage, a 26-tick telegraph, the bell timing as content, every row ending itself). KB2R2
> re-reads the root on a WALKING Knell, which KB2 left unread. The predictions and readings below stand as the record.

> **DATED NOTE, 2026-11-17, step R6 (`feat/dragon-teleport`, stacked on this branch). NOTHING BELOW IS EDITED; this
> note says what changed around it.** The seat passed this gate by diff and left it unbooted, so that ONE boot reads
> R5 and R6 together.
> - **R0 AND THE RUN SHEET BELOW ARE SUPERSEDED in the stacked boot:** the ONE R0 (R0a-R0d, both steps' jar scan, 115
>   scenarios across 17 gates) and the ONE run sheet (`/rpg selftest dragon-beam`, then `/rpg selftest
>   dragon-teleport`) are `GATE-dragon-teleport.md`'s. This file's AUTO rows and KQ1 are still read in that boot.
> - **KB10 IS SUPERSEDED, never run:** R6 makes "the Knell does NOT move after a hit" false. Re-predicted as
>   **KB10R2** in `GATE-dragon-teleport.md`; its scenario left `selftest/GATE-dragon-beam.yml`.
> - **KB11 IS SUPERSEDED, never run:** under R6 a hit moves its LIVE penned Knell out of the pen, onto the circle
>   around the player, free to walk to him. Re-predicted as **KB11R2** (live, walking speed 0, no pen); its scenario
>   left `selftest/GATE-dragon-beam.yml`.
> - **Every row that HITS now leaves one TELEPORT line**, 12 ticks after its FIRE (`GATE-dragon-teleport.md`, THE ROWS
>   ALREADY STACKED): KB1, KB4 and KB7 (and KB2 if it hits) end before the move is due, so the NEXT row's clean removes
>   the Knell and **`TELEPORT skipped … reason=knell-gone` lands in the next row's bracket**; KQ1's two hits each move
>   the frozen Knell during its ASK steps, so KQ1's bracket carries a `TELEPORT …` line after each hit. **No
>   prediction below names a TELEPORT line, and none is changed.**

**What this gate reads.** Step R5 of `PLAN-dragon-mobs.md` §5.R: the Knell's beam (MB3 + E5) as Ben ruled it on
2026-11-17 (*"yes"* to the old game's numbers), with every behaviour he did not re-rule inherited from cfde822 or chosen
where the record is silent (§3.R.1.a, I1-I24). Its scenarios are `selftest/GATE-dragon-beam.yml`, one gate of its own
(`dragon-beam`, last in the index). The teleport after a hit is step R6 and is **not built**: KB10 is the row R6 will
re-predict.

## GAME MODE, DIFFICULTY, GEAR SCORE

- **SURVIVAL on every row.** `/rpg selftest` refuses outside SURVIVAL, and the beam never aims at a creative or
  spectator player (I22).
- **The difficulty is set by the selftest:** the setup's `difficulty: normal`, the run's `DIFFICULTY found=… set=NORMAL`
  and `RESTORED … difficulty=<what it found>`. The beam's damage does not depend on it (it raises no vanilla event,
  M24's undo has nothing to undo).
- **Gear score 100 on every Knell** (`spawn: "<tag> frozen knell 100"`), so the beam's damage before defence is the
  authored **35** × 100 / 100 = **35.000** (`MobBeam.price`, `MobBeamTest`).

## THE PLAYER CANNOT DIE IN ANY ROW — THE ARITHMETIC, ROW BY ROW

**The floor:** his maximum is at least 100 (`CombatantStats.DEFAULT_PLAYER_BASE`; INFERRED that no shipped modifier
lowers a player below it). Ben's own maximum is NOT relied on. Every row that can hit him first runs
`rpg heal 1000000`. The figure is the most the row can deal **before his defence**; defence only lowers it
(`damage × 100 / (100 + defense)`).

**Melee is ruled out by staging, not by arithmetic.** A wither skeleton's hit applies vanilla's Wither effect, and a
Wither tick names no entity, so it is priced in PROPORTION to his custom maximum (`RerouteDamagePrice`: everything not
mob-sourced goes to `DamageScale.toCustom`). That price is not bounded by anything this gate controls, so **no row lets
a Knell reach him**: every forced row's Knell is FROZEN (NoAI: it cannot walk or attack, READ §7 F-B, `isEffectiveAi`),
and the two LIVE Knells (KB2, KB11) are inside a two-high glass pen whose west wall is the block 5 east of him. A
penned Knell's body cannot pass x + 6.0; a melee hit needs its attack box (its box grown about 0.83 sideways, INFERRED
from vanilla's `getAttackBoundingBox`, not read) to touch his body, whose east face is at x + 0.8, more than 5 away.

| row | what can hit him after one heal | the worst case, before defence |
|---|---|---|
| KB1, KB4, KB7, KB10 | one forced beam from a frozen Knell | **35** |
| KB2 | one forced beam from a penned live Knell (it may be clipped by the pen's wall) | **35** |
| KB3, KB5, KB8 | one forced beam that misses (moved, moved, walled) | **0** predicted; 35 if it hit |
| KB6 | none (out of range) | **0** |
| KB9 | none (killed mid-telegraph) | **0** |
| KB11 | the penned Knell's own first beam; a second can only begin at its age ≥ 211 and fire at ≥ 223, after the row's 215 ticks (I2: a fresh 100-200 at the fire) | **35**; 70 if the clean's removal came after a second fire |
| KQ1 | two forced beams, a heal between | **35**, then **35** |

## WHAT THE ROWS STAND ON — EACH FACT, AND ITS MARK

- **B1. The bodies, READ** (`javap -c -p` on `net.minecraft.world.entity.EntityType`, the server jar of build 74,
  `<clinit>`): `wither_skeleton` `sized(0.7f, 2.4f)` `eyeHeight(2.1f)`; `player` `sized(0.6f, 1.8f)` `eyeHeight(1.62f)`;
  `husk` `sized(0.6f, 1.95f)`. The Knell's `scale: 1.6` (DB1R2's reading: `height=3.840`).
- **B2. The Knell's eye is 3.360 above its feet, INFERRED:** 2.1f × 1.6, the way `getDimensions` scales a type's
  dimensions (READ for the box, §7 F-C; the eye's share of that scaling was not opened). In jshell as a float:
  `3.359999895095825`. The TELEGRAPH and FIRE lines print the eye (`from=`), so this is READ AT THE BOOT, not assumed.
- **B3. His aim point is 1.080 above his feet:** `aim_height: 0.6` (content) × his height 1.8f (B1) =
  `1.0799999714`. Read back in the TELEGRAPH line's `to=`.
- **B4. Every geometric figure below was EXECUTED, never worked by hand:** jshell with `core/target/classes` on the
  class path, through core's own `BeamLine.aimed`, `clip`, `gap`, `meets`, `nearestTo` and `MobBeam.inRange`, from the
  floats in B1-B3. With the Knell 8 blocks east, both on one floor (a Knell's feet `(+8, 0, 0)` from his):
  `dir = (-0.961705, -0.274086, 0.000000)`; the line meets the floor's top face 12.258927 blocks out (at x − 3.789 from
  his feet, inside the row's floor); his gap standing still 0.000000; moved 3 south 2.700000 (miss), 1.2 south
  0.900000 (hit), 1.4 south 1.100000 (miss); a husk at +4 gap 0.143580 (met, spared); clipped by a glass wall's east
  face at +4.5, length 3.639369 and his gap 4.200000 (miss); a Knell at +41: eye to aim 41.063346 (out of 40), at +40
  it would still be 40.064927; the report's `to` (nearest point to his box's centre) `(-0.047446, +1.066478, 0)` from
  his feet. **A printed figure is predicted to three places (six for `dir`) and agrees within one unit of the last
  place**, because B2's float is not read until the boot.
- **B5. The floor stops the beam, INFERRED:** `World#rayTraceBlocks(…, FluidCollisionMode.NEVER, ignorePassable=true)`
  returns the stone's top face; that its hit position is exactly at his feet's height was not read. Hence KB1's
  `clip=block length=12.259`, not `clip=range length=40.000`.
- **B6. The clock, READ in our code:** one period-1 `RepeatingTask` per Knell (`MobBeamer.onAppear`); the attempt, the
  ROOT ticks and the FIRE are all steps of that loop; the fire is exactly `telegraph_ticks` (12) loop steps after the
  lock (`BeamCycleTest.theTelegraphRootsThenFiresExactlyItsLengthAfterTheLock`). **So the FIRE line's `tick=` is the
  TELEGRAPH line's + 12.** The BEAM step only marks the next step's attempt due (`DragonBeamWiringTest
  .aForceNeverRunsTheAttemptItself`), on the Knell's own thread, so the TELEGRAPH comes 1 or 2 ticks after the DRIVE
  line (which of the two depends on the entity scheduler's order of two tasks in one tick: not read).
- **B7. A frozen Knell's own clock never begins a beam, READ in our code** (I23; `aFrozenMobsOwnAttemptIsAFreshWait`).
  A frozen Knell still HAS a loop: every forced row's `DRIVE beam tag=… <uuid>` with no `refused=` is that witness.
- **B8. The damage path, READ in our code:** `BukkitCombatant.of(victim).handle().applyDamage(35.0, knellId)`, the mob
  hit's final call (`theDamageAndTheReportAreBothGatedOnTheHit`), so the selftest's HURT line names the Knell as dealer.
- **B9. The first wait starts when the Knell appears, READ in our code** (I3): the loop starts at
  `EntityAddToWorldEvent`, and its first attempt is the drawn wait's d-th step after. `lived=` is `getTicksLived()`. So a
  LIVE Knell's first own TELEGRAPH prints `lived=` within **99 to 201**: 100 to 200, plus or minus one for the order of
  our task against the entity's own tick count (not read).
- **B10. The root, READ in our code, its effect INFERRED:** at the lock, `stopPathfinding()`, the horizontal velocity
  zeroed once, and a TRANSIENT `MOVEMENT_SPEED` × 0 modifier until the fire; `stopPathfinding()` again every telegraph
  tick (I11). That this holds a walking Knell to the same feet position to three places is the prediction KB2 tests.
- **B11. Who is tested at the fire, READ in our code:** the locked target always; every other live body whose box,
  grown by the tolerance, meets the fired segment, listed `spared` and never hurt (`MobBeamTest`).

## THE ROWS ALREADY MERGED — THE AUDIT (the seat's ruling 9)

**Since this step, a Knell whose AI is on begins a beam of its own 100 to 200 ticks after it appears, at the nearest
survival player within 40 blocks, whether or not its AI has targeted him.** A frozen one never does (B7). So every
merged selftest row that spawns a Knell was read for whether its Knell is live and how long it lives:

| row (file) | the Knell | what it reads on this branch | done |
|---|---|---|---|
| DB1R2 (`GATE-dragon-bodies.yml`) | `spawn: "db1 frozen knell 100"`, alive about 5 ticks before its probe, then removed by the next row's clean | **unchanged**: frozen from its pre-spawn consumer (B7), and far short of 100 ticks either way | nothing |
| DB11R2 (`GATE-dragon-bodies.yml`) | `rpg spawn knell stage 2`, REFUSED by the command (a stage on an unstaged mob) | **unchanged**: no Knell exists | nothing |
| WS-C (`GATE-wither-status.yml`) | two `rpg spawn knell`, each followed at once by `data merge … {NoAI:1b,…}`, about 45 ticks | **unchanged**: both frozen in the same step pass as their spawn (the run executes every step up to the next `wait` in one pass, READ in `SelfTestRun.advance`, `GATE-dragon-blast-2.md` K7), so B7 holds from their first tick | nothing |
| WS2c (`GATE-wither-status.yml`) | `rpg spawn knell` + `data merge … {NoAI:1b,…}` at once, **alive about 245 ticks, 3 blocks from the player** | **unchanged, BECAUSE OF I23.** Without the NoAI guard it would beam him once or twice in those 245 ticks (35 before defence each, at the overworld's GS 100) and add HURT lines WS2c never predicted | nothing changed in the row; the guard is pinned twice: `DragonBeamWiringTest.aFrozenMobsOwnAttemptIsAFreshWait` and `SelfTestScenariosTest.noScenarioOutsideTheBeamGateLeavesALiveKnell` (which finds this gate's KB2 and KB11 as its control) |

**Found by** `grep -n -i knell paper/src/main/resources/selftest/*.yml`: those four rows' lines, and comments. **No
merged selftest row is re-predicted or retired.**

**OUTSIDE THE SELFTEST, NOT ON ANY RUN SHEET, NOT RE-PREDICTED — FOR THE SEAT:** 13 closed manual gates contain
`rpg spawn knell` (`grep -c "rpg spawn knell" docs/gates/*.md`: accessories-a, cursed-emerald, dragon-bodies,
element-accrual, ignite, mob-scaling-1, -2, -3, -f20, -m25, vanilla-damage, volley, wither-status). A LIVE Knell there now
beams the reader 100 to 200 ticks after it appears. Their readings were taken before the beam existed and stand; **a
re-run of any of them on this branch would not read as predicted** if its Knell lives past 100 ticks with AI on.

## CARRIED STATE, DONE BY THE BOOTER BEFORE THE BOOT, IN THIS ORDER

1. **No JVM running:** `Get-Process java -ErrorAction SilentlyContinue` prints nothing.
2. **A fresh dated backup**, with no server running, in ONE bash invocation from the repo root:

   ```bash
   B="/c/Users/Neb91/IdeaProjects/rpg-run-backups/$(date +%F)-pre-dragon-beam"; mkdir "$B" && cp -a run/plugins/Rpg run/world "$B/" && for p in "run/plugins/Rpg $B/Rpg" "run/world $B/world"; do set -- $p; echo "$1: $(find "$1" -type f | wc -l) files $(find "$1" -type f -printf '%s\n' | awk '{s+=$1} END{print s}') bytes; $2: $(find "$2" -type f | wc -l) files $(find "$2" -type f -printf '%s\n' | awk '{s+=$1} END{print s}') bytes; diff -rq lines: $(diff -rq "$1" "$2" | wc -l)"; done
   ```

   If the folder exists (a boot the same day), add `-2` to its name. Report the folder, both pairs of counts, and
   `diff -rq lines: 0` for each.
3. **Check, do not assume:** `grep -n "dev-pack-on-join" run/plugins/Rpg/config.yml` must print
   `12:  dev-pack-on-join: true`. If it does not, STOP and report.
4. **Boot this branch's head:** `./scripts/dev-server.sh --refresh-content`, announcing "booting now", on Ben's "ready".
5. **R0 binds to THIS boot:** read R0 only on a log whose `[Rpg] Build:` line names this branch's head. **Readings count
   only after Ben's LOGIN in that boot's log.**

## THE RUN SHEET — BEN TYPES THESE, IN THIS ORDER. EVERY LINE IS LITERAL

1. `/gamemode survival` — **alone, the first line after login.**
2. `/rpg selftest dragon-beam` — **ONCE. Run it one time only, even after it finishes.** Stand still and touch
   nothing, **except in the last part**: a Knell twelve blocks east of you will beam you twice, and a gold line in chat
   says what to do before each. **The first time, stand still and watch**: a thin purple line from the Knell to you is
   the warning, then a thick cyan line is the beam. **The second time, step sideways (A or D) as soon as you see the
   purple line.** Three questions appear in chat; answer each `yes` or `no`. Wait for the chat to say the run finished.
3. `/stop`

## R0 — THE BUILD. READ IT FIRST. IF ANY R0 FAILS, STOP

### R0a — the build line names THIS branch's head

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, this PR's head as `git ls-remote origin refs/heads/feat/dragon-beam` prints it, shortened. **Not** master's `8671ab16`; not `-dirty`, not `unknown` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | **SUPERSEDED in the stacked boot** (the dated note above): read as `GATE-dragon-teleport.md` R0a, **PASS, 2026-11-17**, `[Rpg] Build: e52e54b6` |

### R0b — the jar carries the beam

| prediction | instrument | READING |
|---|---|---|
| PRESENT: `content/mobs/knell.yml` with the lines `  cycle_ticks: { min: 100, max: 200 }`, `  telegraph_ticks: 12` and `  damage: 35`; `MobBeamer.class` carrying `knell-beam`; `MobBeamLines.class` carrying `BEAM telegraph mob=` and `BEAM report mob=`; `SelfTestBeam.class` carrying `DRIVE beam tag=`; the entry `selftest/GATE-dragon-beam.yml` with the keys `KB1:`, `KB11:` and `KQ1:`. ABSENT: the text `NoSuchTextControl` in `MobBeamer.class` (the text control), and the key `BL1:` in `GATE-dragon-blast.yml`; PRESENT `BQ1R2:` there (the key control) | the scan below | **SUPERSEDED in the stacked boot**: read as `GATE-dragon-teleport.md` R0b, **PASS, 2026-11-17**, both steps' scan |

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path 'run\plugins\rpg-0.1.0-SNAPSHOT.jar'))
function Bytes($name) { $e = $zip.GetEntry($name); if ($e -eq $null) { return $null }; $s = $e.Open(); $m = New-Object IO.MemoryStream; $s.CopyTo($m); $s.Close(); [Text.Encoding]::GetEncoding(28591).GetString($m.ToArray()) }
$k = Bytes 'content/mobs/knell.yml'
foreach ($l in '  cycle_ticks: { min: 100, max: 200 }', '  telegraph_ticks: 12', '  damage: 35') {
  if ($k -match ("(?m)^" + [regex]::Escape($l) + "\s*$")) { "PRESENT knell.yml '$l'" } else { "ABSENT  knell.yml '$l'" }
}
foreach ($c in @(@('health/MobBeamer.class', 'knell-beam'), @('health/MobBeamer.class', 'NoSuchTextControl'), @('health/MobBeamLines.class', 'BEAM telegraph mob='), @('health/MobBeamLines.class', 'BEAM report mob='), @('selftest/SelfTestBeam.class', 'DRIVE beam tag='))) {
  $b = Bytes "io/github/butterflysmp/rpg/paper/$($c[0])"
  if ($b -eq $null) { "MISSING $($c[0])" } elseif ($b.Contains($c[1])) { "PRESENT $($c[0]) '$($c[1])'" } else { "ABSENT  $($c[0]) '$($c[1])'" }
}
foreach ($q in @(@('dragon-beam', 'KB1'), @('dragon-beam', 'KB11'), @('dragon-beam', 'KQ1'), @('dragon-blast', 'BL1'), @('dragon-blast', 'BQ1R2'))) {
  $y = Bytes "selftest/GATE-$($q[0]).yml"
  if ($y -eq $null) { "MISSING selftest/GATE-$($q[0]).yml" } elseif ($y -match "(?m)^$($q[1]):\s*$") { "PRESENT $($q[0]) $($q[1]):" } else { "ABSENT  $($q[0]) $($q[1]):" }
}
$zip.Dispose()
```

**A jar from master reads ABSENT on the three `knell.yml` lines, MISSING on the three new classes and on
`GATE-dragon-beam.yml`**, and the same on the two controls. The class texts are searched in the DECOMPRESSED entry (a
zip's own bytes are deflated). **`KB1:` must match only `KB1:`, never `KB10:` or `KB11:`**: the needle is anchored at
`:` and end of line.

### R0c — the Loaded line, and nothing refused

| prediction | instrument | READING |
|---|---|---|
| **`[Rpg] Loaded 11 abilities, 29 visuals, 5 statuses, 7 elements, 11 enchants, 3 pools, 6 fragments, 6 aspects, 16 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 5 mobs, 1 mob elements, 3 recipes, 1 ores, 4 vein sizes`**, verbatim `GATE-dragon-blast-2.md` R0c's reading: this PR changes `knell.yml` and adds no content file. No `Refusing`, `Skipping`, `SEVERE` or `Exception`, and no `[Rpg]` `WARN`, beyond `volley_stone`'s known pair. **A `Skipping malformed mob 'knell.yml'` line would mean the beam block was refused** (and the Loaded line would read `4 mobs`) | `Select-String run\logs\latest.log -Pattern 'Loaded ','Refusing','Skipping','SEVERE','WARN','Exception','Content:'` | **SUPERSEDED in the stacked boot**: read as `GATE-dragon-teleport.md` R0c, **PASS, 2026-11-17** |

### R0d — the dev marker reached the JVM, and every scenario loaded

| prediction | instrument | READING |
|---|---|---|
| Exactly one line **`[Rpg] Selftest: dev server, 102 scenarios across 16 gates [melee-cell, level, nexus-polish, legacy-a, legacy-b, wither-status, withered-shortbow, sunder, deft-hands, mining-auto, nexus-back, dragon-bodies, dragon-split, dragon-growth, dragon-blast, dragon-beam]`**, and **no** `Refusing selftest` line (a malformed `beam` or `probe_at` is refused at load, by name) | `Select-String run\logs\latest.log -Pattern 'Selftest:','Refusing selftest'` | **SUPERSEDED in the stacked boot**: read as `GATE-dragon-teleport.md` R0d, **PASS, 2026-11-17**, `115 scenarios across 17 gates` |

**The 102 is RECOUNTED from the files at this branch's head:** in `paper/src/main/resources/selftest/`,
`grep -cE '^  - ' index.yml` prints **16**; `cat $(grep -E '^  - ' index.yml | tr -d '\r' | sed 's/  - /GATE-/;
s/$/.yml/') | grep -cE '^[A-Za-z0-9-]+:'` prints **118**; the same with `'^setup:'` prints **16**: 118 − 16 = **102**.
**The control that tells this build apart: at master `8671ab16` the same commands print 15, 105, 15: 90**, and the
gate list ends at `dragon-blast`. `SelfTestScenariosTest` pins the 102 and the twelve names.

---

## THE AUTO ROWS — SERVER ONLY, SURVIVAL, DIFFICULTY NORMAL

**The log instrument for every row:**
`Select-String run\logs\latest.log -Pattern 'SELFTEST dragon-beam|\[Rpg\] BEAM'`, read after Ben's LOGIN, inside the
row's own `SELFTEST dragon-beam <row> START … DONE` bracket. **A `[Rpg] BEAM` line comes from the Knell's loop, not
from the selftest, so it carries no row name: it belongs to the row whose bracket it falls in**, and every BEAM line
names its Knell's uuid, which the row's `DRIVE spawn` line printed.

**In EVERY row below (and not repeated in each):** after the row's `CLEAN …` line, **`LEFTOVER living=0 radius=16`**;
for each mob the row spawns, one `DRIVE spawn tag=<its tag> <uuid> <mob> frozen` (or `live`) and the command's reply,
a Knell `REPLY "Spawned Knell (wither_skeleton, 360 HP, GS 100 given)" [aqua, gray]` (INFERRED from the husk's reply
shape, `GATE-dragon-blast-2.md`; RECORD it), the husk `REPLY "Spawned husk (husk, 100 HP, GS 100 given)" [aqua, gray]`.
**No `BEAM` line names a Knell that is not the row's own. A non-zero LEFTOVER makes its row NOT AS PREDICTED.**

**Writing F for his aligned feet** (read from the TELEGRAPH line's `to=`, which is F + (0, 1.080, 0)) **and K for the
Knell's feet**:

- **A TELEGRAPH line** is `[Rpg] BEAM telegraph mob=knell id=<the Knell> gs=100 forced=<true|false> tick=<T>
  lived=<L> at=<K> target=player <Ben's uuid> from=<K + (0, 3.360, 0)> to=<F + (0, 1.080, 0)> dir=<D> range=40.00
  ticks=12`, with D = (to − from) / |to − from| to six places (B4: `-0.961705 -0.274086 0.000000` for a Knell 8 east).
- **A FIRE line** is `[Rpg] BEAM fire mob=knell id=<the Knell> gs=100 base=35.000 tick=<T + 12> at=<K> from=<the
  TELEGRAPH's from> dir=<the TELEGRAPH's dir> length=<l> clip=<block|range> tolerance=1.00 tested=<n> [<entries>]
  dealt=<35.000|none>`, an entry `<type>[/<mob id>] <uuid> gap=<g> <hit|miss|gone|exempt|spared>`, the locked target
  first.
- **A REPORT line**, on a hit only, `[Rpg] BEAM report mob=knell id=<the Knell> target=player <Ben's uuid>
  tick=<T + 12> from=<the FIRE's from> to=<…>`.
- **A HURT line** is `HURT before=35.000 after=<a> defense=<Dp> dealer=<the Knell's uuid> dealerType=<recorded>`, with
  `a = 35 × 100 / (100 + Dp)` to three places (Dp ≤ 0: a = 35.000), in the FIRE's tick. **A row that heals** logs
  `REPLY "Custom HP: <M>/<M>" [green]`, M recorded.

### KB1 — a forced beam at a still player: the telegraph, the fire 12 ticks later, one hit for 35 (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) `DRIVE beam tag=kb1 <the kb1 uuid>` (no `refused=`). (2) **Exactly one** TELEGRAPH for it, `forced=true`, `at=K`, K = F + (8, 0, 0), `from=` K + (0, 3.360, 0), `to=` F + (0, 1.080, 0), `dir=-0.961705 -0.274086 0.000000`, its `tick=T` 1 or 2 after the DRIVE's (B6). (3) **Exactly one** FIRE, `tick=T+12`, `at=` the TELEGRAPH's `at`, `length=12.259 clip=block tolerance=1.00 tested=1 [player <Ben's uuid> gap=0.000 hit] dealt=35.000`. (4) **Exactly one** REPORT, `tick=T+12`, `from=` the FIRE's, `to=` F + (−0.047, 1.066, 0.000). (5) **Exactly one** HURT, `before=35.000`, `dealer=<the kb1 uuid>`, `after` by the formula | B1-B8; B4's `length 12.258927` and `to (-0.047446, 1.066478, 0)`; B5 | **AS PREDICTED, 2026-11-17 (selftest e52e54b6).** (1) `DRIVE beam tag=kb1 5c21e9ce-7463-4289-a820-ee5e0c3c1483`. (2) `[Rpg] BEAM telegraph mob=knell id=5c21e9ce-7463-4289-a820-ee5e0c3c1483 gs=100 forced=true tick=3435 lived=6 at=-89.500 63.000 7.500 target=player b6ae27e9-6ca4-4bb5-a1fb-73cbadbabdd5 from=-89.500 66.360 7.500 to=-97.500 64.080 7.500 dir=-0.961705 -0.274086 0.000000 range=40.00 ticks=12`: F = -97.500 63.000 7.500, K = F + (8, 0, 0), the eye 3.360 above K. (3) `[Rpg] BEAM fire … tick=3447 at=-89.500 63.000 7.500 … length=12.259 clip=block tolerance=1.00 tested=1 [player b6ae27e9-6ca4-4bb5-a1fb-73cbadbabdd5 gap=0.000 hit] dealt=35.000`, T + 12. (4) `[Rpg] BEAM report … tick=3447 from=-89.500 66.360 7.500 to=-97.547 64.066 7.500` = F + (-0.047, 1.066, 0.000). (5) `HURT before=35.000 after=31.818 defense=10.000 dealer=5c21e9ce-7463-4289-a820-ee5e0c3c1483 dealerType=wither_skeleton/knell`: 35 × 100/110 = 31.818. Its Knell's `TELEPORT skipped … reason=knell-gone` landed in KB2's bracket, as the dated note predicted |

### KB2 — the root: a LIVE, penned Knell, forced mid-walk, stands still from the telegraph to the fire (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (0) `DRIVE spawn tag=kb2 <uuid> knell live`. (1) **THE CONTROL, that it was walking:** the two AT lines before the force, `AT tag=kb2 <uuid> x=<x1> …` and five ticks later `x=<x2> …`, with **x2 < x1 − 0.01** (it walks west, toward him, INFERRED from vanilla's melee goal; its follow range, about 16 by vanilla's default, covers his 14). **If x2 ≥ x1 − 0.01, the row is HOLLOW for (3) and is recorded as such, not as a pass.** (2) `DRIVE beam tag=kb2 <uuid>`, then **exactly one** TELEGRAPH for it, `forced=true`. (3) **Exactly one** FIRE, `tick=` the TELEGRAPH's + 12, and **its `at=` equal to the TELEGRAPH's `at=` on all three coordinates, to three places.** (4) The FIRE's `clip=` and `tested` entry are RECORDED: either `[player … gap=0.000 hit] dealt=35.000`, with one REPORT and one HURT `before=35.000`, or a clip on the pen's wall (`clip=block`) naming him a miss, with neither. (5) The AT line 20 ticks after the force: RECORDED (it may walk again after the fire) | B6, B10; the pen (THE PLAYER CANNOT DIE) | **HOLLOW for (3), 2026-11-17: the control (1) failed.** (0) `DRIVE spawn tag=kb2 f229e3f4-27fa-4cae-a3c6-6f309efd48dc knell live`. (1) `AT tag=kb2 f229e3f4-27fa-4cae-a3c6-6f309efd48dc x=-83.500 y=63.000 z=7.500` twice, five ticks apart: x2 = x1, **it was not walking**, so (3) proves nothing about the root. (2) One TELEGRAPH `forced=true tick=3480 lived=21 at=-83.500 63.000 7.500 … dir=-0.986997 -0.160739 0.000000`. (3) FIRE `tick=3492 at=-83.500 63.000 7.500`: equal to the TELEGRAPH's, but hollow. (4) The clip branch: `length=8.612 clip=block tolerance=1.00 tested=1 [player b6ae27e9-6ca4-4bb5-a1fb-73cbadbabdd5 gap=5.200 miss] dealt=none`, no REPORT, no HURT. (5) `AT … x=-83.500 y=63.000 z=7.500`. Also in this bracket, as predicted: `[Rpg] TELEPORT skipped mob=knell id=5c21e9ce-7463-4289-a820-ee5e0c3c1483 target=b6ae27e9-6ca4-4bb5-a1fb-73cbadbabdd5 reason=knell-gone` (KB1's Knell). See the READINGS NOTE for the diagnosis |

### KB3 — the line is fixed at the telegraph: he steps 3 south and is missed (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) DRIVE, then one TELEGRAPH (`forced=true`, `to=` F + (0, 1.080, 0), F his spot BEFORE the step). (2) `CMD /tp @s ~ ~ ~3` AFTER the TELEGRAPH line (it is 3 ticks after the DRIVE; the TELEGRAPH 1 or 2). (3) **Exactly one** FIRE, `tick=T+12`, the same `from` and `dir` as the TELEGRAPH (**the line did not follow him**), `length=12.259 clip=block … tested=1 [player <Ben's uuid> gap=2.700 miss] dealt=none`. (4) **No REPORT, no HURT** | B4: moved 3 south, gap 2.700000; the step is `tp`, so he is exactly 3 south | **AS PREDICTED, 2026-11-17.** TELEGRAPH `tick=3510 … to=-97.500 64.080 7.500 dir=-0.961705 -0.274086 0.000000`; `CMD /tp @s ~ ~ ~3` after it; FIRE `tick=3522 … from=-89.500 66.360 7.500 dir=-0.961705 -0.274086 0.000000 length=12.259 clip=block tolerance=1.00 tested=1 [player b6ae27e9-6ca4-4bb5-a1fb-73cbadbabdd5 gap=2.700 miss] dealt=none`; no REPORT, no HURT |

### KB4 — the tolerance, inside its edge: 1.2 south, his body 0.900 off the line, still hit (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| As KB3, the step `tp @s ~ ~ ~1.2`; the FIRE `… [player <Ben's uuid> gap=0.900 hit] dealt=35.000`; **one REPORT; one HURT `before=35.000`** | B4: gap 0.900000 ≤ 1.00 | **AS PREDICTED for its own lines, 2026-11-17**: TELEGRAPH `tick=3543`; FIRE `tick=3555 … [player b6ae27e9-6ca4-4bb5-a1fb-73cbadbabdd5 gap=0.900 hit] dealt=35.000`; REPORT `tick=3555 … to=-97.547 64.066 7.500`; `HURT before=35.000 after=31.818 defense=10.000 dealer=d0ab2d7f-d650-4f09-af42-095825f94b75 dealerType=wither_skeleton/knell`. **The dated note's prediction for its Knell is NOT AS PREDICTED:** in place of a `knell-gone` skip, KB5's bracket carries the move itself, BEFORE KB5's CLEAN line: `[Rpg] TELEPORT mob=knell id=d0ab2d7f-d650-4f09-af42-095825f94b75 target=player b6ae27e9-6ca4-4bb5-a1fb-73cbadbabdd5 tick=3567 from=-89.500 63.000 7.500 center=-97.500 63.000 8.700 radius=8.089 bearing=-8.531 side=left first=43.338 tried=1 [left 43.338 ok y=63.000 at=-92.505 63.000 2.337] landed=-92.505 63.000 2.337 moved=true`. The seat's ruling: diagnosed, nothing changed (READINGS NOTE) |

### KB5 — the tolerance, just outside it: 1.4 south, 1.100 off, missed (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| As KB3, the step `tp @s ~ ~ ~1.4`; the FIRE `… [player <Ben's uuid> gap=1.100 miss] dealt=none`; **no REPORT, no HURT**. KB4 is its control (the same staging, 0.2 nearer) | B4: gap 1.100000 > 1.00 | **AS PREDICTED, 2026-11-17.** FIRE `tick=3588 … [player b6ae27e9-6ca4-4bb5-a1fb-73cbadbabdd5 gap=1.100 miss] dealt=none`; no REPORT, no HURT. Its bracket also holds KB4's TELEPORT line (KB4's reading) |

### KB6 — a target beyond the range: no telegraph at all (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) `DRIVE beam tag=kb6 <uuid>` (no `refused=`: the loop exists). (2) **Exactly one** `[Rpg] BEAM none mob=knell id=<the kb6 uuid> forced=true tick=<t> range=40.00 reason=no-target`. (3) **No TELEGRAPH, no FIRE, no REPORT, no HURT** for it. (4) `AT tag=kb6 <uuid> …` 41.000 east of F in x. KB1 is its control (the same Knell 8 east is in range) | B4: eye to aim 41.063346 > 40 (and at 40 east it would be 40.064927, also out) | **AS PREDICTED, 2026-11-17.** `DRIVE beam tag=kb6 540c46f9-5f83-4ddd-8dec-6fc673e10daa`; `[Rpg] BEAM none mob=knell id=540c46f9-5f83-4ddd-8dec-6fc673e10daa forced=true tick=3609 range=40.00 reason=no-target`; no TELEGRAPH, FIRE, REPORT or HURT; `AT tag=kb6 540c46f9-5f83-4ddd-8dec-6fc673e10daa x=-56.500 y=63.000 z=7.500`, F + 41 in x |

### KB7 — a frozen husk in the line keeps its health; the beam passes it and hits him (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) Before: `PROBE tag=kb7h <uuid> husk customHP=100.000/100.000 fireTicks=0 dead=false …` (its `d` and `dy` RECORDED). (2) One TELEGRAPH. (3) **Exactly one** FIRE `… length=12.259 clip=block tolerance=1.00 tested=2 [player <Ben's uuid> gap=0.000 hit, husk <the kb7h uuid> gap=0.144 spared] dealt=35.000`. (4) One REPORT; **one** HURT `before=35.000` (his, not the husk's: the HURT tap is the run's player only). (5) After: the same PROBE line, **`customHP=100.000/100.000`** | B4: the husk's gap 0.143580; B11; the husk spawns at F + (4, 0, 0) | **AS PREDICTED, 2026-11-17.** `PROBE tag=kb7h de13dd3b-5b04-4e99-a057-8709b009ac3f husk customHP=100.000/100.000 fireTicks=0 dead=false d=4.000 dy=0.000` before and after; FIRE `tick=3651 … length=12.259 clip=block tolerance=1.00 tested=2 [player b6ae27e9-6ca4-4bb5-a1fb-73cbadbabdd5 gap=0.000 hit, husk de13dd3b-5b04-4e99-a057-8709b009ac3f gap=0.144 spared] dealt=35.000`; one REPORT; one `HURT before=35.000 after=31.818 defense=10.000 dealer=e83b1cd3-7610-4e78-b821-615c0e421b40 dealerType=wither_skeleton/knell`. Its Knell's `TELEPORT skipped … reason=knell-gone` landed in KB8's bracket, as the dated note predicted |

### KB8 — a block stops it: a glass wall 4 east clips the beam, and he is missed (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| One TELEGRAPH (the wall does not stop a TELEGRAPH: I7); **exactly one** FIRE `… length=3.639 clip=block tolerance=1.00 tested=1 [player <Ben's uuid> gap=4.200 miss] dealt=none`; **no REPORT, no HURT** | B4: clipped at the wall's east face (x + 4.5 from his feet's x, block x + 4), length 3.639369, his gap 4.200000 | **AS PREDICTED, 2026-11-17.** FIRE `tick=3681 … length=3.639 clip=block tolerance=1.00 tested=1 [player b6ae27e9-6ca4-4bb5-a1fb-73cbadbabdd5 gap=4.200 miss] dealt=none`; no REPORT, no HURT |

### KB9 — a Knell killed mid-telegraph fires nothing (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) DRIVE, one TELEGRAPH at tick T. (2) `CMD /kill @e[tag=kb9]` 5 ticks after the DRIVE. (3) **Exactly one** `[Rpg] BEAM cancelled mob=knell id=<the kb9 uuid> elapsed=<n>/12 reason=gone`, **1 ≤ n ≤ 11**, n RECORDED. (4) **No FIRE, no REPORT, no HURT** for it, through the row's 25 further ticks. KB1 is its control (the same staging, not killed, fires) | I19; the loop's `isActive` check (`RepeatingTask`, READ) and `onStop`'s cancel line (`MobBeamer.onAppear`) | **AS PREDICTED, 2026-11-17.** TELEGRAPH `tick=3699`; `CMD /kill @e[tag=kb9]`; `[Rpg] BEAM cancelled mob=knell id=7ef3f91b-817a-4dd7-8d7a-6663801459b3 elapsed=3/12 reason=gone` (n = 3); no FIRE, REPORT or HURT |

### KB10 — the Knell does NOT move after a hit (the teleport is step R6, not built) (SURVIVAL)

> **SUPERSEDED, 2026-11-17, step R6, NEVER RUN:** a hit now moves the Knell 12 ticks later. Re-predicted as KB10R2 in
> `GATE-dragon-teleport.md`; the scenario left `selftest/GATE-dragon-beam.yml`. The prediction below is kept as written.

| prediction | stands on | READING |
|---|---|---|
| (1) `AT tag=kb10 <uuid> x=<x> y=<y> z=<z>` before the force: F + (8, 0, 0). (2) One TELEGRAPH, one FIRE `… [player <Ben's uuid> gap=0.000 hit] dealt=35.000`, one REPORT, one HURT `before=35.000`. (3) 40 ticks after the force (past the fire + 12, where R6's teleport will land), **the AT line identical to (1) on all three coordinates.** **A frozen mob is NOT a hollow fixture for a teleport:** a teleport sets its position directly and does not need its AI (INFERRED: `Entity#teleport` is not gated on `isEffectiveAi`); it is hollow only for walking, which this row does not test. **Step R6 re-predicts THIS row** | B7; nothing in R5 moves a Knell | **NOT RUN (superseded, the note above); re-predicted and read as KB10R2 in `GATE-dragon-teleport.md`** |

### KB11 — the live cycle, by bound: a live penned Knell's own first beam between 100 and 200 ticks after it appears; a frozen one's never (SURVIVAL)

> **SUPERSEDED, 2026-11-17, step R6, NEVER RUN:** a hit would move this LIVE penned Knell out of its pen, within reach
> of the player. Re-predicted as KB11R2 in `GATE-dragon-teleport.md` (live, walking speed 0, no pen); the scenario
> left `selftest/GATE-dragon-beam.yml`. The prediction below is kept as written.

| prediction | stands on | READING |
|---|---|---|
| (1) `DRIVE spawn tag=kb11f <uuid> knell frozen`, then `DRIVE spawn tag=kb11 <uuid> knell live`. (2) **Exactly one** TELEGRAPH naming the kb11 uuid with **`forced=false`** and **99 ≤ L ≤ 201**, its `at=` inside the pen (x between F + 6.0 and F + 16.0). (3) Its FIRE, `tick=` its TELEGRAPH's + 12, `at=` the TELEGRAPH's; `clip` and outcome RECORDED, with a REPORT and one HURT `before=35.000` exactly when it names him `hit`. (4) **No BEAM line of any kind names the kb11f uuid**, though it stands in range (B7, I23). (5) **At most one further** TELEGRAPH for kb11, and only with `lived=` at least the first's + 111 (I2: a fresh 100-200 at the fire, the fire 12 after the lock; minus one for B9); if one begins, its FIRE or a `BEAM cancelled … reason=gone` (the next row's clean removes it) follows, RECORDED | B9, B7, I1, I2 | **NOT RUN (superseded, the note above); re-predicted and read as KB11R2 in `GATE-dragon-teleport.md`** |

---

## BEN'S ROW — BY THE ASK STEP, SURVIVAL, DIFFICULTY NORMAL

**AUTO-PARTIAL** (`verification.md`, *AN AUTO READING IS A READING OF THE SERVER*): the BEAM, REPORT and HURT lines are
the server's half; **what he saw and felt is his half, and only his answer reads it.** The run sheet tells him in
advance what to watch (step 2), and the selftest repeats it in gold chat before each beam.

### KQ1 — did you see the warning; could you step out of it; was it fair (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| **The server half.** (1) A heal, the gold line `Stand still and watch…`, then 60 ticks, `DRIVE beam tag=kq1 <uuid>`, one TELEGRAPH (`to=` his aim, `dir` along a 12-block line: the eye to the aim is 12.215 blocks, B4), one FIRE `… [player <Ben's uuid> gap=0.000 hit] dealt=35.000` **if he stood still**, one REPORT and one HURT `before=35.000`. (2) The three ASK lines with his answers, verbatim. (3) After the second gold line and 60 ticks, a second DRIVE, TELEGRAPH and FIRE: **its outcome is his doing and is RECORDED, not predicted**, as is its gap; a `hit` carries one REPORT and one HURT, a `miss` neither. **The run is the instrument; nothing here judges his answers.** **His half:** ASK 1 *"Did you see a warning before the beam hit you?"* (yes/no); ASK 2 *"Did the hit feel fair for a single shot?"* (yes/no); ASK 3 *"Was it possible to step out of the way in time?"* (yes/no) | I8, I10 (the warning's look is cfde822's, READ); I9 (12 ticks, 0.6 seconds) | **Server half, 2026-11-17.** (1) TELEGRAPH `forced=true tick=3814 lived=61 at=-85.500 63.000 7.500 … dir=-0.982424 -0.186661 0.000000`; FIRE `tick=3826 … length=18.001 clip=block tolerance=1.00 tested=1 [player b6ae27e9-6ca4-4bb5-a1fb-73cbadbabdd5 gap=0.000 hit] dealt=35.000`; one REPORT; `HURT before=35.000 after=31.818 …`. Then, as the dated note said, `TELEPORT … tick=3838 from=-85.500 63.000 7.500 center=-97.500 63.000 7.500 radius=12.000 bearing=0.000 side=right first=33.851 tried=1 [right 33.851 ok y=63.000 at=-87.534 63.000 14.184] landed=-87.534 63.000 14.184 moved=true`. (3) The second beam, from there: TELEGRAPH `tick=4223 lived=470 at=-87.534 63.000 14.184 … to=-97.500 64.080 7.500`; FIRE `tick=4235 … [player b6ae27e9-6ca4-4bb5-a1fb-73cbadbabdd5 gap=0.000 hit] dealt=35.000`: **he was hit**; one REPORT; one HURT; `TELEPORT … tick=4247 center=-97.500 63.000 5.168 radius=13.439 bearing=42.138 side=left first=36.442 tried=1 [left 36.442 ok …] landed=-84.127 63.000 6.502 moved=true` (he had moved 2.332 north by then). **His half, verbatim:** ASK 1 `answer="yes"`; ASK 2 `answer="i think it needs to be more damage"`; ASK 3 `answer="maybe a slight increase in duration"`. ASK 2 and ASK 3 are neither yes nor no, recorded as he gave them |

---

## READINGS NOTE, 2026-11-17 — READ IN THE STACKED BOOT OF `e52e54b6` (R0 and run sheet: `GATE-dragon-teleport.md`)

**Rows read:** KB1-KB9 and KQ1 (KB10 and KB11 superseded, never run). **AS PREDICTED:** KB1, KB3, KB5, KB6, KB7, KB8,
KB9, and KB4's own lines. **Two departures, each diagnosed and NOT acted on (the seat's ruling: diagnose before
anything changes):**

- **KB2 is HOLLOW for the root.** Its control required the penned live Knell to be walking at the force; both AT reads
  and both BEAM lines put it at `-83.500 63.000 7.500`, unmoved since its spawn. **INFERRED diagnosis, not read:** a
  wither skeleton chooses its target through vanilla's nearest-attackable-target goal, whose test needs line of sight,
  and the pen's glass is a collision block, so from inside the pen it never saw him, never targeted him, and never
  walked. The same glass clipped its beam (`length=8.612 clip=block … gap=5.200 miss`). **So the root (I11) is still
  unread on a walking Knell**; TELEGRAPH `at=` equal to FIRE `at=` on a Knell that was standing still proves nothing.
  A fixture that can see him and cannot reach him is owed.
- **KB4's Knell MOVED instead of being removed**: `TELEPORT … tick=3567 … landed=-92.505 63.000 2.337 moved=true`,
  logged at the start of KB5's bracket, before KB5's CLEAN line. Its FIRE was tick 3555, so the move was due at 3567,
  and KB4's row had not ended before that tick: **the dated note's arithmetic put the row's end 23 ticks after the force
  and it was 25** (the TELEGRAPH at 3543 is a tick after the force; the move at 3567 is the force + 25), so the clean's
  removal, a tick after KB5's CLEAN, came after the move. KB7, the same shape without the 3-tick step, left its skip line
  in KB8's bracket as predicted. Harmless here (a frozen Knell, removed a tick later); the margin is the thin spot the
  seat named.

**KQ1, Ben's answers, verbatim:** `yes`; `i think it needs to be more damage`; `maybe a slight increase in duration`.
The second and third are not yes or no; they read as asks for a higher K10 (35) and a longer K7 (12), and both are
Ben's to rule. His second beam hit him (`gap=0.000 hit`): he moved after the fire, not during the telegraph.
