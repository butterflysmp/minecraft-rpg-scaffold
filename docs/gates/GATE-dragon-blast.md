# GATE — THE PHAGE'S BLAST (DRAGON-MOBS STEP R3): R0, THE AUTO ROWS, BEN'S THREE QUESTIONS, AND THE RUN SHEET

**Status: NOT RUN.** Every prediction below was written **before** any boot of `feat/dragon-blast`, and this file is
**committed before the boot**. Readings go **beside** a prediction, never over it, and a prediction is not edited once
its row has been read; a correction is a dated note. **NO BOOT until the seat has diffed the code AND this gate, and Ben
says ready.**

> **2026-11-16: THE PREDICTIONS IN THIS FILE WERE REWRITTEN BEFORE ANY BOOT, AND THIS IS WHY.** The version first
> committed at `58165392` predicted a blast of 3 blocks at full damage (the seat's provisional P-c). Vanilla's reach is
> twice its power, 6, with falloff (`PLAN-dragon-mobs.md` §8 F2, §7 F-B), and Ben ruled *"keep vanillas shap"*. No row of
> that version was ever booted or read, so the seat directed that its predictions be rewritten in the same PR rather
> than kept as a dead record. **What changed:** BL1 and BL2 predict the falloff on the printed distance; BL3 and BL4 blow
> 7 blocks from the player instead of 5, so he stays outside the reach; BL6 and BL7 are now the reach's edge (5.90 hit,
> 6.05 not), where they were 3.00 and 3.05; BL9 is new, a fixed half-reach hit; R0b checks `reach: 6`; R0d counts 92.
> BL5, BL8, BQ1 and the run sheet are unchanged.

> **2026-11-16, LATER, STILL BEFORE ANY BOOT: SIX ROWS ADDED FROM THE OLDER DRAGON GATES, AND WHY.** The seat found that
> this PR's diff touched no older dragon scenario, while rows merged on 2026-11-15 (`GATE-dragon-fix.md`) put a LIVE
> Phage beside the player, and from R3 a live Phage within 3 blocks blows. The audit is `PLAN-dragon-mobs.md`, RULINGS,
> *THE BLAST AND THE ROWS ALREADY MERGED*. **What changed here:** a frozen parent's split children are now born frozen
> (S11); DS1R3, DS2R3, DS5R3, DS6R3, DG6R3 and DG9R3 are new rows, each re-predicting the `GATE-dragon-fix.md` row it is
> named for; S1's swell direction is now READ, and S11-S13 are new; R0b checks the moved keys; R0d counts 90; the
> "no SPLIT line anywhere" sentence is scoped to BL1-BL9 and BQ1. **No earlier prediction in this file was changed in
> what it predicts**, and the run sheet's commands are unchanged: every changed row is in this file.

The plan is `PLAN-dragon-mobs.md`: RULINGS (*Ben, 2026-11-15, VERBATIM: the blast's numbers*; the seat's provisional
P-a, P-b, P-d, P-e; the seat's mechanism rulings 20-26; *THE SEAT'S MISS ON THE BLAST'S RADIUS*, Ben's *"keep vanillas
shap"* and rulings 27-31), §5.R.4 (step R3 as built), §7 F-B (the jar's reach and curve), §8 F2 and F3, Q31-Q35. The
scenarios are `paper/src/main/resources/selftest/GATE-dragon-blast.yml`, paired with THIS file
(`SelfTestScenariosTest.docOf`).

```
ROWS     20   R0a R0b R0c R0d
              BL1 BL2 BL3 BL4 BL5 BL6 BL7 BL8 BL9
              DS1R3 DS2R3 DS5R3 DS6R3 DG6R3 DG9R3
              BQ1
         ──
         20   = 4 R0 + 9 BL + 6 R3 + 1 BQ         git grep -c -E '^### ' <ref> -- docs/gates/GATE-dragon-blast.md
```

## GAME MODE, DIFFICULTY, GEAR SCORE AND THE PLAYER'S HEALTH

- **SURVIVAL on every row.** `/rpg selftest` refuses outside SURVIVAL. Vanilla's swell skips a creative or spectator
  target (§7 F-C), so a creative bench would read no live row at all.
- **The difficulty is set by the selftest itself**: the scenario file's `setup:` begins `difficulty: normal`; the run
  logs `DIFFICULTY found=<what the world had> set=NORMAL` and restores what it found at RUN START (`RESTORED …
  difficulty=<that>`). **Ben types no `/difficulty` line.**
- **Every mob is spawned at GEAR SCORE 100**, so every blast's damage at ZERO distance (`base=`) is its AUTHORED damage:
  20 at stage 1, 50 at stage 5 (`MobBlastTest.theDamageIsPricedByGearScoreAsACustomMobsHitIs`: ×1 at GS 100, never ×5
  for a custom mob). Out to the reach it is multiplied by `MobBlast.falloff`.
- **THE PLAYER CANNOT DIE IN ANY ROW, AND WHY.** Each row in which he can be hit (BL1, BL2, BL6, BL9, and BQ1 twice)
  first runs `rpg heal 1000000`, which heals him to his maximum (`RpgCommand.healSelf`, capped at max). At most one blast
  reaches him per heal. The largest he can take is **50 before his defence** (a stage 5 at zero distance; BL2 and BQ1's
  second): the factor is at most 1 (`falloff` is 1 at zero and falls), and defence only lowers it
  (`Defense.applyDefense`: `damage × 100 / (100 + defense)`, the damage itself when defence ≤ 0). **His maximum is at
  least 100** (`CombatantStats.DEFAULT_PLAYER_BASE`, a player's base; INFERRED that no shipped modifier lowers a player
  below it) and read 350 at the last boot. **50 < 100**, so at least 50 is left. The heal's reply prints the maximum he
  starts from.
- **AND THE SIX R3 ROWS (below BL9) KEEP THEIR OWN GEAR SCORES:** 300, as in `GATE-dragon-fix.md`, except DS1R3's 200.
  **Only DS1R3 can hit him**, and it is the one row where two blasts follow one heal: two thawed stage-1 children at GS
  200, each **40** at zero distance, and the one that blows second may first have grown a stage from the first's blast
  (a blast is a Void death, BL3), so **55** (stage 2's authored 27.5 × 2). At most **40 + 55 = 95 before his defence,
  under his 100.** DS1R3 is at 200 and not 300 for exactly this: at 300 it would be 60 + 82.5 = 142.5. 200 still tells
  the children's inherited gear score from the default 100.

## WHAT THE ROWS STAND ON — EACH FACT, AND ITS MARK

The server jar is `run/versions/26.1.2/paper-26.1.2.jar` (build 74); the API jar is
`~/.m2/repository/io/papermc/paper/paper-api/26.1.2.build.74-stable/paper-api-26.1.2.build.74-stable.jar`. Every jar fact
below was read with `C:\Users\Neb91\.jdks\openjdk-26.0.1\bin\javap.exe -c -p -cp <jar> <class>`.

- **S1. The fuse — READ.** `Creeper.tick`: `swell += swellDir` every tick, floored at 0; at `swell >= maxSwell` it calls
  `explodeCreeper()`. The tick runs while the creeper is alive whatever its AI, so a NoAI creeper keeps whatever swell
  direction it has; a fresh one's is −1 (READ 2026-11-16: `Creeper.defineSynchedData` defines `DATA_SWELL_DIR` as
  `iconst_m1`; this said INFERRED, not opened, until then), so a Phage frozen from birth never swells, **and one frozen
  after its swell began keeps swelling and blows** (S11). `CraftCreeper#setMaxFuseTicks` writes `maxSwell` (`putfield maxSwell`); `getFuseTicks` reads `swell`;
  `addAdditionalSaveData` writes `maxSwell` as `"Fuse"`.
- **S2. The prime — READ.** `Creeper.explodeCreeper` fires `ExplosionPrimeEvent` (`CraftEventFactory.callExplosionPrimeEvent`).
  Cancelled, it sets `swell = 0` and clears `DATA_IS_IGNITED`, and nothing else; not cancelled, it explodes and
  discards. **Our handler cancels it first, always**, then runs our detonation (`RpgListeners.onCustomCreeperPrime`;
  `DragonBlastWiringTest.thePrimeIsCancelledBeforeOurDetonationRuns`). `CraftCreeper#explode` calls `explodeCreeper()`
  directly, so the DETONATE step raises the same event a completed swell does.
- **S3. The swell's start and cancel — READ in §7 F-C** (`SwellGoal`): it starts with a live target at
  `distanceToSqr < 9.0` and turns the swell down past `49.0` or without line of sight. Measured position to position.
- **S4. A live Phage two blocks from the player swells to the prime within 100 ticks — READ at the last boot**:
  `GATE-dragon-fix.md` DB10R2 logged three primes for one stage-1 Phage two blocks east in 100 ticks. The live rows wait
  120. **That a stage 5 does the same is INFERRED** (same goal, same distances; its speed is irrelevant while it stands
  within 3).
- **S5. The reach and the curve — READ, §7 F-B.** Vanilla's creeper hurts out to `radius * 2` = 6, inclusive
  (`ServerExplosion#hurtEntities`), measured between the victim's position and the blast's centre, the creeper's
  position: **feet to feet, the same two points ours uses.** Its damage is `(x*x + x)/2 * 7 * f + 1`,
  `x = (1 - d/f) * exposure` (`ExplosionDamageCalculator#getEntityDamageAmount`). Ours keeps only the shape:
  `MobBlast.falloff(d, 6) = (x*x + x)/2`, `x = 1 - d/6`, exposure 1, no `* 7 * f`, no `+ 1`. **No cover, no knockback.**
- **S6. The damage — READ (the code).** A victim at distance d is dealt `base × falloff(d)` before its defence, where
  `base = MobBlast.price` (the authored damage × GS/100). A victim exactly at the reach is listed with factor 0 and not
  hit. Each other victim is hit through `BukkitCombatant.of(victim).handle().applyDamage(dealt, phageId)`, the
  two-argument form a mob's melee hit takes, which defers the hit one tick to the victim's own scheduler; the store then
  applies defence. **So the HURT line follows the BLAST line by about a tick, and its `before` is the BLAST line's
  `dealt` (the same double, printed to the same three places).**
- **S7. The HURT line — READ (the code).** One per DAMAGE change the store makes to the run's player inside a row:
  `HURT before=<offered> after=<landed> defense=<his defence> dealer=<uuid|self|none> dealerType=<type[/mob id]|gone>`.
  `before` is the store's own `amount` before the curve (`HealthChange#beforeDefense`); `after` is what landed.
  **`dealerType` is NOT predicted for a blast:** the Phage is removed in the same task as the blast, and whether
  `CraftWorld#getEntity` (→ `ServerLevel#getEntity`) still resolves a discarded entity a tick later was **not read**;
  it prints `gone` if not.
- **S8. The distances — READ (the code), executed (the arithmetic).** A frozen mob is spawned at the player's feet after
  a `tp` of the stated offset from a block centre and back, so feet to feet is the offset. Executed in jshell for start
  x = −92.0, −91.5, 13.0 and 200.0: `+5.9` and back measures 5.9000000000000060 or 5.9 exactly, printing `d=5.900
  factor=0.008472 dealt=0.169` at base 20 every time; `+6.05` measures 6.0499999999999970 to 6.0500000000000110, beyond
  6 every time; `+3` and `+7` are exact. A frozen (NoAI) mob does not move. The reach is inclusive
  (`MobBlastTest.exactlyTheReachIsInsideAndJustBeyondIsNot`).
- **S9. Health of the frozen bystanders — READ.** A husk's `MAX_HEALTH` is the attribute's registry default, 20
  (`Attributes`' `max_health` is `RangedAttribute(…, 20.0d, …)`; `Zombie.createAttributes`, which `Husk` inherits, adds
  no `MAX_HEALTH`); × 5 × GS/100 (M1, M2) = **100**. A stage-5 Phage at GS 100 is **160** (`DB4R2`'s body, ×1). A husk
  does not burn at noon (the setup's `time set 6000` would burn a zombie, and a burn is damage: a husk stands in for the
  brief's zombie for that reason).
- **S10. The printed figures, and the tolerance on a formula read against them.** The BLAST line prints `d` to three
  places, `factor` to six and `dealt` to three. Where a row predicts `factor = falloff(d)` on the PRINTED `d`, the
  printed `d` is off the true one by at most 0.0005, and the curve's steepest slope is at zero distance,
  `(2x + 1) / (2 × reach)` at x = 1: **0.25 per block** (executed: `3.0/12`). So the factor recomputed from the printed
  `d` agrees with the printed factor **within 0.000125 + 0.0000005**, and `dealt` agrees with `base × printed factor`
  **within 0.0005 (its own rounding) + base × 0.0000005**. These are the tolerances, and that is their basis.

- **S11. A frozen parent's children are born frozen — READ (the code and the jar).** `SelfTestService.adoptSplitChild`
  runs inside each child's pre-spawn consumer (`MobSplitter.onDeath`), so before its first tick. When the parent carries
  the instrument's tag and has no AI (`SelfTestSnapshot.freezeSplitChild`, `SelfTestSplitChildTest`), it calls
  `setAI(false)` on the child; for every tagged parent it logs `CHILD parent=<uuid> child=<uuid> frozen=<true|false>`,
  row-prefixed, before the `SPLIT` line, **its `frozen` read back from the child** (`!hasAI()`), not the rule's answer,
  so the line witnesses the freeze landing. NoAI stops the goals (`Mob.isEffectiveAi` is the parent's `&& !isNoAi()`, and
  `LivingEntity.aiStep` runs `serverAiStep` only when it is true), and only `SwellGoal.tick` sets the swell direction to
  +1 besides an ignite (S1), so a child born frozen never swells. **Freezing at birth is the point:** S1's tick would
  carry a swell begun before a later freeze through to the blast.
- **S12. Whom a live child can swell at — READ.** `Creeper.registerGoals`' target selector holds
  `NearestAttackableTargetGoal(Player)` and `HurtByTargetGoal`, and `SwellGoal.canUse` needs a live target at
  `distanceToSqr < 9.0` (S3). In DS1R3 nothing hurts a child (a blast spares mobs, `MobHostility`) and Ben is the one
  player online (`SelfTestGuard`), so **a BLAST line naming a live child is that child having targeted him within 3
  blocks**: S2's AUTO half.
- **S13. The thaw — INFERRED.** DS1R3 thaws its children with vanilla's
  `execute as @e[tag=ds1,type=creeper] run data merge entity @s {NoAI:0b}`, which merges the tag into each child's saved
  data and loads it back. **That it changes nothing else of ours is INFERRED**: our body, stage and gear score live in
  attributes, scale and the persistent data container, all saved and loaded by the same merge. Its timing is not
  predicted: the children born live in `GATE-dragon-fix.md` DS1R2 primed twice each within 90 ticks of birth (READ at
  the 2026-11-15 boot, `CREEPERGUARD` lines, the same prime event), and DS1R3 waits 200 after the thaw.

**What no row here can show, said once:** the `max=30` in a FUSE line cannot tell content's 30 from vanilla's default
30, because the content value IS vanilla's (Ben's ruling). The wiring is pinned by
`DragonBlastWiringTest.theFuseIsWrittenAtSpawn` instead.

## CARRIED STATE, DONE BY THE BOOTER BEFORE THE BOOT, IN THIS ORDER

1. **No JVM running:** `Get-Process java -ErrorAction SilentlyContinue` prints nothing.
2. **A fresh dated backup**, with no server running, in ONE bash invocation from the repo root:

   ```bash
   B="/c/Users/Neb91/IdeaProjects/rpg-run-backups/$(date +%F)-pre-dragon-blast"; mkdir "$B" && cp -a run/plugins/Rpg run/world "$B/" && for p in "run/plugins/Rpg $B/Rpg" "run/world $B/world"; do set -- $p; echo "$1: $(find "$1" -type f | wc -l) files $(find "$1" -type f -printf '%s\n' | awk '{s+=$1} END{print s}') bytes; $2: $(find "$2" -type f | wc -l) files $(find "$2" -type f -printf '%s\n' | awk '{s+=$1} END{print s}') bytes; diff -rq lines: $(diff -rq "$1" "$2" | wc -l)"; done
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
2. `/rpg selftest dragon-blast` — stand still and touch nothing, **except when a gold question appears in chat**: then
   type your answer (`yes` or `no`) in chat. There are three questions. Wait for the chat to say the run finished.
   (In one row, DS1R3, two small Phages walk to you and blow; stand still for that too.)
3. `/stop`

**Why only this one selftest (2026-11-16):** every row the R3 audit changed is in THIS file (DS1R3 … DG9R3). The three
older dragon files lost rows and changed none (`git diff` on each shows deletions and a header note only), so none of
their remaining rows is re-run.

**No cleanup lines:** every mob a row makes is tagged at spawn, and the run's restore removes every tagged mob. **No
`/difficulty` lines:** the selftest sets and restores it.

## R0 — THE BUILD. READ IT FIRST. IF ANY R0 FAILS, STOP

### R0a — the build line names THIS branch's head

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, this PR's head as `git ls-remote origin refs/heads/feat/dragon-blast` prints it, shortened. **Not** master's `94915476`, and **not** `58165392` (this PR's head before the shape change). Not `-dirty`, not `unknown` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | |

### R0b — the jar carries step R3, at vanilla's shape

| prediction | instrument | READING |
|---|---|---|
| PRESENT: the seven entries below the control. In `content/mobs/phage.yml`, the lines `blast:`, `  reach: 6` and `  fuse_ticks: 30`, and **no** `  radius: 3` (the shape before 2026-11-16); in `selftest/GATE-dragon-blast.yml`, the line `  - difficulty: normal`. ABSENT: the control `selftest/NoSuchClassControl.class` | the scan below | |

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path 'run\plugins\rpg-0.1.0-SNAPSHOT.jar'))
foreach ($c in 'selftest/GATE-dragon-blast.yml',
               'content/mobs/phage.yml',
               'io/github/butterflysmp/rpg/core/mob/MobBlast.class',
               'io/github/butterflysmp/rpg/core/mob/MobHostility.class',
               'io/github/butterflysmp/rpg/paper/health/MobBlaster.class',
               'io/github/butterflysmp/rpg/paper/selftest/SelfTestBlast.class',
               'io/github/butterflysmp/rpg/paper/selftest/SelfTestDamageTap.class',
               'io/github/butterflysmp/rpg/paper/selftest/NoSuchClassControl.class') {
  if ($zip.GetEntry($c)) { "PRESENT $c" } else { "ABSENT  $c" }
}
function Text($name) { $r = New-Object IO.StreamReader($zip.GetEntry($name).Open()); $t = $r.ReadToEnd(); $r.Close(); $t }
$p = Text 'content/mobs/phage.yml'
if ($p -match "(?m)^blast:\s*$") { "PRESENT blast:" } else { "ABSENT  blast:" }
if ($p -match "(?m)^  reach: 6\s*$") { "PRESENT reach: 6" } else { "ABSENT  reach: 6" }
if ($p -match "(?m)^  radius: 3\s*$") { "PRESENT radius: 3" } else { "ABSENT  radius: 3" }
if ($p -match "(?m)^  fuse_ticks: 30\s*$") { "PRESENT fuse_ticks: 30" } else { "ABSENT  fuse_ticks: 30" }
if ((Text 'selftest/GATE-dragon-blast.yml') -match "(?m)^  - difficulty: normal\s*$") { "PRESENT difficulty dragon-blast" } else { "ABSENT  difficulty dragon-blast" }
foreach ($k in @(@('dragon-blast', 'DS1R3'), @('dragon-blast', 'DG9R3'), @('dragon-bodies', 'DB11R2'), @('dragon-bodies', 'DB10R2'), @('dragon-bodies', 'DQ1'), @('dragon-split', 'DS1R2'), @('dragon-growth', 'DG9R2'))) {
  if ((Text "selftest/GATE-$($k[0]).yml") -match "(?m)^$($k[1]):\s*$") { "PRESENT $($k[0]) $($k[1]):" } else { "ABSENT  $($k[0]) $($k[1]):" }
}
$zip.Dispose()
```

**And the moved rows (2026-11-16):** PRESENT `dragon-blast DS1R3:`, `dragon-blast DG9R3:` and `dragon-bodies DB11R2:`
(the control: a row that stayed, so the needle can find a key in that file); ABSENT `dragon-bodies DB10R2:`,
`dragon-bodies DQ1:`, `dragon-split DS1R2:` and `dragon-growth DG9R2:`. A jar built before the audit reads the
opposite on all six.

**The `radius: 3` line is the control for the `reach: 6` one:** `growth:`'s `  radius: 6` is in the same file and must
not satisfy either needle, and the old blast key must read ABSENT.

### R0c — the Loaded line, and nothing refused

| prediction | instrument | READING |
|---|---|---|
| **`[Rpg] Loaded 11 abilities, 29 visuals, 5 statuses, 7 elements, 11 enchants, 3 pools, 6 fragments, 6 aspects, 16 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 5 mobs, 1 mob elements, 3 recipes, 1 ores, 4 vein sizes`**, verbatim `GATE-dragon-fix.md` R0c's reading: R3 adds a block to `phage.yml` and no content file (`git diff --stat 94915476 HEAD -- paper/src/main/resources/content` names `content/mobs/phage.yml` only), and the Phage still loads (`MobLoaderTest.everyBundledMobLoads`). No `Refusing`, `Skipping`, `SEVERE` or `Exception`, and no `[Rpg]` `WARN`, beyond `volley_stone`'s known pair (`Content: weapon 'volley_stone' …`, `1 content problem(s)`) | `Select-String run\logs\latest.log -Pattern 'Loaded ','Refusing','Skipping','SEVERE','WARN\]: \[Rpg\]','Exception','Content:'` | |

### R0d — the dev marker reached the JVM, and every scenario loaded

| prediction | instrument | READING |
|---|---|---|
| Exactly one line **`[Rpg] Selftest: dev server, 90 scenarios across 15 gates [melee-cell, level, nexus-polish, legacy-a, legacy-b, wither-status, withered-shortbow, sunder, deft-hands, mining-auto, nexus-back, dragon-bodies, dragon-split, dragon-growth, dragon-blast]`**, and **no** `Refusing selftest` line (a malformed `detonate`, `ignite`, `probe_fuse` or `await_fuse` step is refused at load, by name) | `Select-String run\logs\latest.log -Pattern 'Selftest:','Refusing selftest'` | |

**The 90 is DERIVED.** From `paper/src/main/resources/selftest/` at this head: `grep -cE '^  - ' index.yml` prints
**15**; `cat $(grep -E '^  - ' index.yml | tr -d '\r' | sed 's/  - /GATE-/; s/$/.yml/') | grep -cE '^[A-Za-z0-9-]+:'`
prints **105**; the same with `'^setup:'` prints **15**: 105 − 15 = **90**. The controls: at `ba74df59` (before the
audit of 2026-11-16) the same commands print 15, 107, 15, so 92; at `58165392`, 106, so 91; at master `94915476` they
print 14 and 96, so 82 (`GATE-dragon-fix.md` R0d's reading). `SelfTestScenariosTest` pins the 90.

---

## THE AUTO ROWS — SERVER ONLY, SURVIVAL, DIFFICULTY NORMAL, GEAR SCORE 100

**The log instrument for every row:**
`Select-String run\logs\latest.log -Pattern 'SELFTEST dragon-blast|\[Rpg\] BLAST|\[Rpg\] GROWTH|\[Rpg\] SPLIT|CREEPERGUARD'`,
read after Ben's LOGIN, inside the row's own `SELFTEST dragon-blast <row> START … DONE` bracket.

**In EVERY row below (and not repeated in each):** after the row's `CLEAN …` line, **`LEFTOVER living=0 radius=16`**;
for each mob the row spawns, one `DRIVE spawn tag=<its tag> <uuid> <mob> frozen` (or `live` where the row says) and
the command's reply: a stage-1 Phage `REPLY "Spawned Phage (creeper, stage 1, 80 HP, GS 100 given)" [aqua, gray]`, a
stage-5 `REPLY "Spawned Phage (creeper, stage 5, 160 HP, GS 100 given)" [aqua, gray]`. **No `CREEPERGUARD prime` line
anywhere in the run** (ruling 26 removed it). **No `SPLIT` line in BL1-BL9 or BQ1** (the six R3 rows split on
purpose, and say so). **A non-zero LEFTOVER in any row
makes that row NOT AS PREDICTED**, whatever else it reads, and names what was there.

**A BLAST line** is `[Rpg] BLAST mob=phage id=<uuid> stage=<n> gs=100 base=<20.000|50.000> reach=6.00 victims=<n>
[player <uuid> d=<d> factor=<f> dealt=<x>, …] spared=<n> [<type>[/<mob id>] <uuid> d=<d> factor=<f>, …]`.

**A row that heals** logs `REPLY "Custom HP: <M>/<M>" [green]`, M his maximum, recorded and not predicted.

### BL1 — a LIVE stage-1 Phage blows on the player; the damage falls off with his distance; it is gone; the floor is whole (SURVIVAL)

**The distance cannot be held fixed in a live row:** the swell starts wherever the Phage stands when the player is
within 3 and in sight, which is vanilla's AI timing (S3, S4). So the figure is the formula on the printed distance.

| prediction | stands on | READING |
|---|---|---|
| (1) `BLOCKS STONE count=108 box=-3 -1 -4 8 -1 4` before the spawn. (2) **Exactly one** BLAST line, `id=<the bl1 uuid> stage=1 gs=100 base=20.000 reach=6.00 victims=1 [player <Ben's uuid> d=<D> factor=<F> dealt=<X>] spared=0 []`, with **F = falloff(D) = (x² + x)/2, x = 1 − D/6**, and **X = 20 × F**, each within S10's tolerance. **A BOUND, stated as one:** D < 3 (the swell starts inside 3 and the Phage stands while it swells, INFERRED from `SwellGoal`, S3), so F > 0.375 and X > 7.500. (3) **Exactly one** `HURT before=<X, the same three places> after=<A> defense=<Dp> dealer=<the bl1 uuid> dealerType=<recorded>`, after the BLAST line, with **A = X × 100 / (100 + Dp)** to the printed three places (Dp ≤ 0: A = X). (4) `ENTITIES CREEPER count=0 radius=16`. (5) After `DRIVE return <x y z>`, `BLOCKS STONE count=108 box=-3 -1 -4 8 -1 4` again. (6) No `GROWTH` line | S2-S7, S10; (1)/(5) the box is 12 × 1 × 9 = 108 (`GATE-dragon-fix.md` DB10R2 read it) and nothing of ours touches a block | |

### BL2 — a LIVE stage-5 Phage blows on the player from a base of 50, and leaves NO children (SURVIVAL) — Ben's E

| prediction | stands on | READING |
|---|---|---|
| (1) `BLOCKS STONE count=108 …` before. (2) **Exactly one** BLAST line, `id=<the bl2 uuid> stage=5 gs=100 base=50.000 reach=6.00 victims=1 [player <Ben's uuid> d=<D> factor=<F> dealt=<X>] spared=0 []`, with **F = falloff(D)** and **X = 50 × F** (S10's tolerance); **the bound** D < 3, so X > 18.750. (3) **Exactly one** `HURT before=<X> after=<X × 100 / (100 + Dp)> defense=<Dp> dealer=<the bl2 uuid> dealerType=<recorded>`. (4) `ENTITIES CREEPER count=0 radius=16`: **zero children**. (5) **No `SPLIT` line in the bracket.** (6) The second `BLOCKS STONE count=108 …` | S2-S7, S10; (4)/(5): removal raises no death event (`CraftEntity#remove` is `discard(PLUGIN)`, `RpgListeners.onMobDeath`'s javadoc; `DragonBlastWiringTest.theBlastRemovesThePhageWithoutADeath`) | |

### BL3 — a blast is a Void death: a stage-1 Phage 3.00 away grows to stage 2; the one that blew never grows (SURVIVAL)

A frozen stage-1 Phage (`bl3x`) SEVEN blocks east is blown by DETONATE; a frozen stage-1 Phage (`bl3b`) stands 3.00 north
of it. The player is 7.00 from the blast, outside its 6-block reach.

| prediction | stands on | READING |
|---|---|---|
| (1) `DRIVE detonate tag=bl3x <the bl3x uuid>`. (2) **Exactly one** `[Rpg] BLAST mob=phage id=<the bl3x uuid> stage=1 gs=100 base=20.000 reach=6.00 victims=0 [] spared=1 [creeper/phage <the bl3b uuid> d=3.000 factor=0.375000]`. (3) **Exactly one** `[Rpg] GROWTH mob=phage id=<the bl3b uuid> from=1 to=2 fed=creeper/phage <the bl3x uuid> element=void d=3.00 max=100`; **no GROWTH line names bl3x.** (4) `BODY tag=bl3b <uuid> type=creeper mob=phage stage=2 scale=0.882 height=1.500 speedBase=0.3125 speedType=0.2500 speedX=1.250 maxHP=100.000 gs=100 element=void dead=false`. (5) `BODY tag=bl3x none within 16 blocks`. (6) `ENTITIES CREEPER count=1 radius=16`. (7) No `HURT` line | S2, S5, S8; 0.375000 is `falloff(3, 6)` (jshell); (3) growth's death path with the Phage's own id (`MobGrower.onDeath`; `MobGrowth.stageAfter` skips the dead id: `MobGrowthTest.theDyingCreatureNeverGrowsItself`); stage 2's body as `GATE-dragon-fix.md` DG1R2 read it, its health 100 = stage 2's 100 × GS 100/100 | |

### BL4 — a frozen husk and a frozen stage-5 Phage well inside the reach keep their health (SURVIVAL) — Ben's G

A frozen stage-1 Phage (`bl4x`) SEVEN blocks east is blown by DETONATE; a frozen husk (`bl4h`) 2.00 north of it and a
frozen stage-5 Phage (`bl4p`) 2.00 south, each a third of the way to the reach. The player is 7.00 from the blast.

| prediction | stands on | READING |
|---|---|---|
| (1) Before the blast: `PROBE tag=bl4h <uuid> husk customHP=100.000/100.000 fireTicks=0 dead=false d=7.280 dy=0.000` and `PROBE tag=bl4p <uuid> creeper customHP=160.000/160.000 fireTicks=0 dead=false d=7.280 dy=0.000`. (2) `REPLY "Spawned husk (husk, 100 HP, GS 100 given)" [aqua, gray]` for the husk. (3) **Exactly one** `[Rpg] BLAST mob=phage id=<the bl4x uuid> stage=1 gs=100 base=20.000 reach=6.00 victims=0 [] spared=2 [<both, in either order: husk <the bl4h uuid> d=2.000 factor=0.555556, creeper/phage <the bl4p uuid> d=2.000 factor=0.555556>]`. (4) After it, the two PROBE lines **unchanged**. (5) **No GROWTH line** (a stage 5 does not grow; a husk is not a candidate). (6) `ENTITIES CREEPER count=1 radius=16` | S9; 0.555556 is `falloff(2, 6)` and `d=7.280` is √53 (jshell: `String.format("%.3f", Math.sqrt(53))` prints 7.280); `fireTicks=0` stands on `GATE-dragon-fix.md` DG7R2's frozen creeper reading 0 (INFERRED to hold for a husk); (2)'s colours INFERRED from the Phage's reply (an unnamed mob's type key inherits the aqua) | |

### BL5 — backing away cancels the fuse: past 7 blocks the swell falls and nothing blows (SURVIVAL) — Ben's F

A live stage-1 Phage two blocks east. When its swell reaches 10 of 30, the player steps nine blocks west (onto the
row's floor, which reaches x − 12). Five ticks later the swell is read, and then the Phage is killed, so it cannot follow
him into the next row. **Unchanged by the shape change**: nothing blows in this row.

| prediction | stands on | READING |
|---|---|---|
| (1) `FUSE tag=bl5 <uuid> fuse=10/30 ignited=false waited=<recorded>`. (2) After the step and five ticks, `FUSE tag=bl5 <uuid> fuse=<n>/30 ignited=false` with **n < 10**: **a BOUND, not a figure** (n is recorded): which tick the goal turns the swell down on, relative to the player's task, was not read, so n is 6, 7 or 8 by the READ mechanism, and the bound is what the claim needs. (3) `ENTITIES CREEPER count=1 radius=16`: alive. (4) `CMD /kill @e[tag=bl5]`, `SETTLED dying=0 ticks=<t> first=1`, then `ENTITIES CREEPER count=0 radius=16`. (5) **No BLAST line and no HURT line in the bracket.** (6) No SPLIT (a stage 1) and no GROWTH line (no other Phage) | S1, S3: the swell rises one a tick, so the first reading ≥ 10 polled each tick is 10; nine blocks west of a Phage at most two east is more than 7; past 7 the goal turns the swell down | |

### BL6 — just inside the reach (5.90 blocks), the player is hit for a sliver (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) `DRIVE detonate tag=bl6 <the bl6 uuid>`. (2) **Exactly one** `[Rpg] BLAST mob=phage id=<the bl6 uuid> stage=1 gs=100 base=20.000 reach=6.00 victims=1 [player <Ben's uuid> d=5.900 factor=0.008472 dealt=0.169] spared=0 []`. (3) **Exactly one** `HURT before=0.169 after=<0.169… × 100 / (100 + Dp), to three places> defense=<Dp> dealer=<the bl6 uuid> dealerType=<recorded>` | S5-S8: jshell, `20 × falloff(5.9000000000000060, 6)` prints `0.169`, factor `0.008472` | |

### BL7 — just beyond the reach (6.05 blocks), the player is not hit (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) `DRIVE detonate tag=bl7 <the bl7 uuid>`. (2) **Exactly one** `[Rpg] BLAST mob=phage id=<the bl7 uuid> stage=1 gs=100 base=20.000 reach=6.00 victims=0 [] spared=0 []`. (3) **No HURT line in the bracket.** BL6 is this row's control: the same staging 0.15 nearer is hit | S8: 6.05 measures above 6 at every start tried (jshell) | |

### BL8 — flint and steel's route still cannot light a Phage (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) `DRIVE ignite tag=bl8 <the bl8 uuid>`, then `[Rpg] CREEPERGUARD ignite cancelled mob=phage id=<the bl8 uuid>`. (2) Forty ticks later, `FUSE tag=bl8 <uuid> fuse=0/30 ignited=false`. (3) `ENTITIES CREEPER count=1 radius=16`. (4) **No BLAST line in the bracket** | `CraftCreeper#ignite()` → the server's `ignite()` → `CreeperIgniteEvent` (§7 F-C; flint and steel's `mobInteract` calls the same `ignite()`, `CustomCreeperGuard`'s javadoc), refused by `RpgListeners.onCustomCreeperIgnite`; S1: frozen, it never swells | |

### BL9 — at half the reach (3.00 blocks), the player takes three eighths of the base (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) `DRIVE detonate tag=bl9 <the bl9 uuid>`. (2) **Exactly one** `[Rpg] BLAST mob=phage id=<the bl9 uuid> stage=1 gs=100 base=20.000 reach=6.00 victims=1 [player <Ben's uuid> d=3.000 factor=0.375000 dealt=7.500] spared=0 []`. (3) **Exactly one** `HURT before=7.500 after=<7.5 × 100 / (100 + Dp), to three places> defense=<Dp> dealer=<the bl9 uuid> dealerType=<recorded>` | S5-S8: `falloff(3, 6)` = 0.375 exactly (x = 0.5; jshell), × 20 = 7.5 | |

**WHAT IS UNIT-ONLY, AND WHY:** a CREATIVE player inside a blast another player triggered. The selftest refuses to run
with more than one player online (`SelfTestGuard`), and a creative runner is refused too, so no row can stage it. The rule
is `MobBlastTest.mobsAreHostileToPlayersOnly` and `theBlastHitsPlayersInsideTheReachAndSparesEveryoneElseInside`: the
creative player is **not hurt**, and the BLAST line names him under `spared`. **So is a victim EXACTLY at the reach**
(listed, factor 0, not hit): the instrument places a frozen Phage at an offset from a block centre, and whether `+6` and
back measures exactly 6.0 depends on the start's coordinates in floating point, so a row could not promise which side of
the edge it lands on (the same test, `edge` at 6.0).

---

## THE OLDER SPLIT ROWS, RE-PREDICTED FOR R3 — SERVER ONLY, SURVIVAL, DIFFICULTY NORMAL

**Why these are here** (the seat's brief of 2026-11-16; the audit is `PLAN-dragon-mobs.md`, RULINGS, *THE BLAST AND THE
ROWS ALREADY MERGED*): each of these rows killed a frozen stage 5 a few blocks from the player and left two LIVE
children beside him, and from R3 a live Phage within 3 blocks blows. Each is now a new row named for the
`GATE-dragon-fix.md` row it re-predicts, with the next suffix. **DS2R3, DS5R3, DS6R3, DG6R3 and DG9R3 keep that row's
steps EXACTLY** (diffed against the old scenario blocks, 2026-11-16); what changed is that a frozen parent's children
are born frozen (S11). **DS1R3's steps differ**: see its row. The old rows' predictions and readings stay in
`GATE-dragon-fix.md`, with a dated note.

**In each of these rows, beyond the every-row lines above:** the spawn replies are the old row's, verbatim (a stage 5
at GS 300 `REPLY "Spawned Phage (creeper, stage 5, 480 HP, GS 300 given)" [aqua, gray]`; a stage 2 `… stage 2, 300 HP …`;
a stage 4 `… stage 4, 420 HP …`). **Before each `SPLIT` line, exactly two `CHILD parent=<that SPLIT's parent uuid>
child=<uuid> frozen=true`, their two child uuids the `SPLIT` line's two** (DS1R3 too: it thaws them later). **A kill
step** logs `CMD /kill @e[tag=<t>]` and then `SETTLED dying=0 ticks=<t> first=1`, `ticks=` recorded.

### DS1R3 — a stage 5 killed by the PLAYER'S emberblade splits into two stage 1s (S1); thawed, each comes for him and blows (S2) (SURVIVAL) — re-predicts GATE-dragon-fix.md DS1R2

**What changed from DS1R2, and why:** the gear score is **200**, not 300 (so his survival is arithmetic, not a reading
of his maximum; see *THE PLAYER CANNOT DIE*), so `rpg mobdamage 319` brings the stage 5's 320 to 1. S2 was DS1R2's
`probe_target` sixty ticks after the split; the children are now born frozen and could not target him, so DS1R3 heals
him, thaws them (S13), and reads S2 from their BLAST lines (S12). **DS1R2's `TARGET` lines are not re-read anywhere**:
their claim, that a child comes for the player, is carried here by the blast that ends it.

| prediction | stands on | READING |
|---|---|---|
| (1) `REPLY "Spawned Phage (creeper, stage 5, 320 HP, GS 200 given)" [aqua, gray]` and `REPLY "CREEPER damaged: 1/320 custom HP" [green]`. (2) `DRIVE attack Player#attack target=<the ds1 uuid> creeper d=3.000 charge=1.000`, the two `CHILD … frozen=true` lines, then **exactly one** `[Rpg] SPLIT mob=phage parent=<the ds1 uuid> stage=5 gs=200 children=2 childStage=1 [<A> dx=-0.50, <B> dx=0.50]`. (3) `ENTITIES CREEPER count=2 radius=16`. (4) **Exactly two** BODY lines, each `BODY tag=ds1 <uuid> type=creeper mob=phage stage=1 scale=0.588 height=1.000 speedBase=0.3750 speedType=0.2500 speedX=1.500 maxHP=160.000 gs=200 element=void dead=false`, the uuids A and B. (5) `REPLY "Custom HP: <M>/<M>" [green]`, then the thaw's `CMD` line (its reply recorded, not predicted). (6) **Within the 200 ticks, exactly two BLAST lines, one `id=<A>` and one `id=<B>`, in either order.** Call the first X and the second Y. X's is `stage=1 gs=200 base=40.000 reach=6.00 victims=1 [player <Ben's uuid> d=<D1> factor=<F1> dealt=<40 × F1>] spared=<n> […]`, with **n = 1, naming `creeper/phage <Y> d=<d> factor=<falloff(d)>`, exactly when Y stood within 6 of X**, else `spared=0 []`. **If n = 1:** exactly one `[Rpg] GROWTH mob=phage id=<Y> from=1 to=2 fed=creeper/phage <X> element=void d=<that d, to two places> max=200` after X's BLAST, and Y's BLAST is `stage=2 gs=200 base=55.000 …`. **If n = 0:** no GROWTH line, and Y's BLAST is `stage=1 gs=200 base=40.000 …`. Either way Y's is `victims=1 [player <Ben's uuid> d=<D2> factor=<F2> dealt=<base × F2>] spared=0 []`. Each F is `falloff(D)` within S10's tolerance, **and the bound D < 3** (BL1's), so each F > 0.375. (7) **Exactly two** `HURT before=<each dealt> after=<before × 100 / (100 + Dp)> defense=<Dp> dealer=<X, then Y> dealerType=<recorded>`. (8) `ENTITIES CREEPER count=0 radius=16`. (9) No SPLIT line after the first, and no `CREEPERGUARD prime` line | (1)-(4) as `GATE-dragon-fix.md` DS1R2 read them, at GS 200: 160 × 2 = 320 for the parent, 80 × 2 = 160 for each child; S11 for the CHILD lines. (6): S12 (a BLAST from a live child is its target within 3 blocks: S2); the spared/GROWTH link because the blast's reach and growth's radius are both 6, both inclusive, both between the same two points (the dead Phage's position, read before it is removed, and the other Phage's; `MobBlaster.detonate` calls `MobGrower.onDeath` before `remove()`); stage 2's 27.5 × 2 = 55 and its health 100 × 2 = 200; no split, because a blast is a removal (BL2). **The timing depends on vanilla's AI** (S13): that both blow inside 200 ticks is INFERRED from DS1R2's primes. **The one shape (6) does not cover:** X and Y blowing in the SAME tick, where Y would blow at stage 1 although X's line spares it. It is recorded if read, as NOT AS PREDICTED | |

### DS2R3 — a stage 5 killed by `/kill` splits; its children are born frozen (SURVIVAL) — re-predicts GATE-dragon-fix.md DS2R2

| prediction | stands on | READING |
|---|---|---|
| `CMD /kill @e[tag=ds2]`, the two `CHILD parent=<the ds2 uuid> … frozen=true` lines, **exactly one** SPLIT line `parent=<the ds2 uuid> stage=5 gs=300 children=2 childStage=1 [<uuid> dx=-0.50, <uuid> dx=0.50]`, then `SETTLED dying=0 ticks=<t> first=1`, `ENTITIES CREEPER count=2 radius=16`, and **exactly two** BODY lines `tag=ds2`, each `type=creeper mob=phage stage=1 scale=0.588 height=1.000 speedBase=0.3750 speedType=0.2500 speedX=1.500 maxHP=240.000 gs=300 element=void dead=false`. **No BLAST line and no HURT line** | DS2R2's reading (2026-11-15); S11 | |

### DS5R3 — once only: two after the split, still two forty ticks later (SURVIVAL) — re-predicts GATE-dragon-fix.md DS5R2

**Why this row needed R3's fix and was not merely moved:** DS5R2's two live children primed three times between its two
counts on 2026-11-15 (`CREEPERGUARD` lines, READ). On this branch each first prime is a blast, which removes its
Phage, so DS5R2's second count could not have read 2.

| prediction | stands on | READING |
|---|---|---|
| The two `CHILD … frozen=true` lines, **exactly one** SPLIT line in the bracket (`parent=<the ds5 uuid> stage=5 gs=300 children=2 childStage=1 …`), and `ENTITIES CREEPER count=2 radius=16` **twice**, forty ticks apart. **No BLAST line and no HURT line** | DS5R2's reading; S11, S1: a child born frozen never swells | |

### DS6R3 — the row-start clean removes DS5R3's children (SURVIVAL) — re-predicts GATE-dragon-fix.md DS6R2

| prediction | stands on | READING |
|---|---|---|
| DS6R3's own `CLEAN blocks=756 removed=2 inventory=kept …` (DS5R3's two children, carrying its tags; 756 is DS5R3's saved box, 12 × 7 × 9), then `LEFTOVER living=0 radius=16`, then `ENTITIES CREEPER count=0 radius=16` | DS6R2's reading; the order in the scenario file (DS6R3 directly after DS5R3) | |

### DG6R3 — a stage 5's death splits it; its frozen children stay at 1, the bystander goes to 3 (SURVIVAL) — re-predicts GATE-dragon-fix.md DG6R2

| prediction | stands on | READING |
|---|---|---|
| (0) The two `CHILD parent=<the dg6p uuid> … frozen=true` lines. (1) **Exactly one** `SPLIT mob=phage parent=<the dg6p uuid> stage=5 gs=300 children=2 childStage=1 [<uuid> dx=-0.50, <uuid> dx=0.50]`. (2) **Exactly one** GROWTH line in the bracket: `id=<the dg6b uuid> from=2 to=3 fed=creeper/phage <the dg6p uuid> element=void d=2.00 max=360`; **none names either child.** (3) A stage-3 BODY line for dg6b (`scale=1.176 height=2.000 speedBase=0.2500 speedType=0.2500 speedX=1.000 maxHP=360.000 gs=300`). (4) **Exactly two** stage-1 BODY lines for dg6p, `maxHP=240.000 gs=300`. (5) `ENTITIES CREEPER count=3 radius=16`. (6) **No BLAST line and no HURT line** | DG6R2's reading; S11 | |

### DG9R3 — a Phage grown to 5, then killed, splits into two frozen children (SURVIVAL) — re-predicts GATE-dragon-fix.md DG9R2

| prediction | stands on | READING |
|---|---|---|
| **Exactly one** GROWTH line `id=<the dg9 uuid> from=4 to=5 … d=2.00 max=480`, then a stage-5 BODY line for dg9. After its kill: the two `CHILD parent=<the dg9 uuid> … frozen=true` lines, **exactly one** `SPLIT mob=phage parent=<the dg9 uuid> stage=5 gs=300 children=2 childStage=1 [<uuid> dx=-0.50, <uuid> dx=0.50]`, **exactly two** stage-1 BODY lines for dg9 (`maxHP=240.000 gs=300`), and `ENTITIES CREEPER count=2 radius=16`. **No BLAST line and no HURT line** | DG9R2's reading; S11 | |

---

## BEN'S ROWS — BY THE ASK STEP. EACH ANSWER IS ONE WORD

### BQ1 — the blast looks and sounds like one; the small one hurts "a little", the big one "a lot" (SURVIVAL)

Ben's words for the two sizes (PLAN-dragon-mobs.md, RULINGS, *On the Phage and the Goldrot Goon*): the big Phage
*"When it gets close to a player it explodes dealing a lot of damage"*; the small ones *"When they get close to the
player they explode dealing a little damage."* He is healed before each. A live stage-1 Phage (`bq1small`) blows on him
first (its BLAST and HURT lines are recorded beside the answers, `base=20.000`, and the `dealt` at his distance); then a
live stage 5 (`bq1big`, `base=50.000`). Each question waits 1200 ticks (one minute) for his next chat line. **These are at
gear score 100**, so the most he can feel is the authored 20 and 50 before his defence, less with distance; at the End's
300 it would be three times that.

| prediction | READING |
|---|---|
| `ASK 1 question="Did that look and sound like an explosion?" answer="<his line>" ticks=<t>` | |
| `ASK 2 question="Your design says the small Phage explodes 'dealing a little damage'. Did that feel like a little?" answer="<his line>" ticks=<t>` | |
| `ASK 3 question="Your design says the big Phage explodes 'dealing a lot of damage'. Did that feel like a lot?" answer="<his line>" ticks=<t>` | |

**The answer cell is his line, verbatim.** "not sure", or no line before the minute is out
(`answer=NOT STATED after 1200 ticks`), is recorded as NOT STATED, never re-asked in this boot.
