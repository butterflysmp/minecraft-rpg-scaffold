# GATE — THE PHAGE'S BLAST (DRAGON-MOBS STEP R3): R0, THE AUTO ROWS, BEN'S THREE QUESTIONS, AND THE RUN SHEET

**Status: NOT RUN.** Every prediction below was written **before** any boot of `feat/dragon-blast`, and this file is
**committed before the boot**. Readings go **beside** a prediction, never over it, and a prediction is not edited once
its row has been read; a correction is a dated note. **NO BOOT until the seat has diffed the code AND this gate, and Ben
says ready.**

The plan is `PLAN-dragon-mobs.md`: RULINGS (*Ben, 2026-11-15, VERBATIM: the blast's numbers*; the seat's provisional
P-a to P-e; the seat's mechanism rulings 20-26), §5.R.4 (step R3 as built), §8 F2, Q31-Q33. The scenarios are
`paper/src/main/resources/selftest/GATE-dragon-blast.yml`, paired with THIS file (`SelfTestScenariosTest.docOf`).

```
ROWS     13   R0a R0b R0c R0d
              BL1 BL2 BL3 BL4 BL5 BL6 BL7 BL8
              BQ1
         ──
         13   = 4 R0 + 8 BL + 1 BQ                git grep -c -E '^### ' <ref> -- docs/gates/GATE-dragon-blast.md
```

## GAME MODE, DIFFICULTY, GEAR SCORE AND THE PLAYER'S HEALTH

- **SURVIVAL on every row.** `/rpg selftest` refuses outside SURVIVAL. Vanilla's swell skips a creative or spectator
  target (§7 F-C), so a creative bench would read no live row at all.
- **The difficulty is set by the selftest itself**: the scenario file's `setup:` begins `difficulty: normal`; the run
  logs `DIFFICULTY found=<what the world had> set=NORMAL` and restores what it found at RUN START (`RESTORED …
  difficulty=<that>`). **Ben types no `/difficulty` line.**
- **Every mob is spawned at GEAR SCORE 100**, so every blast deals its AUTHORED damage: 20 at stage 1, 50 at stage 5
  (`MobBlastTest.theDamageIsPricedByGearScoreAsACustomMobsHitIs`: ×1 at GS 100, never ×5 for a custom mob).
- **THE PLAYER CANNOT DIE IN ANY ROW, AND WHY.** Each row in which he can be hit (BL1, BL2, BL6, and BQ1 twice) first
  runs `rpg heal 1000000`, which heals him to his maximum (`RpgCommand.healSelf`, capped at max). At most one blast
  reaches him per heal. The largest is **50 before his defence** (BL2, BQ1's second); defence only lowers it
  (`Defense.applyDefense`: `damage × 100 / (100 + defense)`, the damage itself when defence ≤ 0). **His maximum is at
  least 100** (`CombatantStats.DEFAULT_PLAYER_BASE`, a player's base; INFERRED that no shipped modifier lowers a
  player below it) and read 350 at the last boot. 50 < 100. The heal's reply prints the maximum he starts from.

## WHAT THE ROWS STAND ON — EACH FACT, AND ITS MARK

The server jar is `run/versions/26.1.2/paper-26.1.2.jar` (build 74); the API jar is
`~/.m2/repository/io/papermc/paper/paper-api/26.1.2.build.74-stable/paper-api-26.1.2.build.74-stable.jar`. Every jar fact
below was read with `C:\Users\Neb91\.jdks\openjdk-26.0.1\bin\javap.exe -c -p -cp <jar> <class>`.

- **S1. The fuse — READ.** `Creeper.tick`: `swell += swellDir` every tick, floored at 0; at `swell >= maxSwell` it calls
  `explodeCreeper()`. The tick runs while the creeper is alive whatever its AI, so a NoAI creeper keeps whatever swell
  direction it has; a fresh one's is −1 (INFERRED: `DATA_SWELL_DIR`'s default, not opened), so a FROZEN Phage never
  swells. `CraftCreeper#setMaxFuseTicks` writes `maxSwell` (`putfield maxSwell`); `getFuseTicks` reads `swell`;
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
- **S5. The damage — READ (the code).** `MobBlast.price`: the authored damage × GS/100 (`MobScaling.attackDamage`,
  custom). At GS 100: 20.000 and 50.000 (executed in jshell: `20.0+(50.0-20.0)*((double)(s-1)/4)` prints 20.0, 27.5,
  35.0, 42.5, 50.0). Each victim is hit through `BukkitCombatant.of(victim).handle().applyDamage(damage, phageId)`, the
  two-argument form a mob's melee hit takes, which defers the hit one tick to the victim's own scheduler; the store then
  applies defence. **So the HURT line follows the BLAST line by about a tick.**
- **S6. The HURT line — READ (the code).** One per DAMAGE change the store makes to the run's player inside a row:
  `HURT before=<offered> after=<landed> defense=<his defence> dealer=<uuid|self|none> dealerType=<type[/mob id]|gone>`.
  `before` is the store's own `amount` before the curve (`HealthChange#beforeDefense`); `after` is what landed.
  **`dealerType` is NOT predicted for a blast:** the Phage is removed in the same task as the blast, and whether
  `CraftWorld#getEntity` (→ `ServerLevel#getEntity`) still resolves a discarded entity a tick later was **not read**;
  it prints `gone` if not.
- **S7. The distances — READ (the code), executed (the arithmetic).** A frozen mob is spawned at the player's feet after
  a `tp` of the stated offset from a block centre, so feet to feet is the offset exactly where the coordinates are
  exact: `+3` → `d=3.00`; `+3.05` → 3.0499999999999970 at x = −91.5, whose square is not ≤ 9 (jshell). A frozen (NoAI)
  mob does not move. The radius is inclusive (`MobBlastTest.exactlyThreeBlocksIsInsideAndJustBeyondIsNot`).
- **S8. Health of the frozen bystanders — READ.** A husk's `MAX_HEALTH` is the attribute's registry default, 20
  (`Attributes`' `max_health` is `RangedAttribute(…, 20.0d, …)`; `Zombie.createAttributes`, which `Husk` inherits, adds
  no `MAX_HEALTH`); × 5 × GS/100 (M1, M2) = **100**. A stage-5 Phage at GS 100 is **160** (`DB4R2`'s body, ×1). A husk
  does not burn at noon (the setup's `time set 6000` would burn a zombie, and a burn is damage: the brief's "zombie" is a
  husk here for that reason).
- **S9. The radius in vanilla — READ, §8 F2.** Vanilla's creeper "3" is a power whose damage reaches 6 blocks with
  falloff. These rows test OUR rule (3 blocks, full damage); Q33 asks Ben.

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
3. `/stop`

**No cleanup lines:** every mob a row makes is tagged at spawn, and the run's restore removes every tagged mob. **No
`/difficulty` lines:** the selftest sets and restores it.

## R0 — THE BUILD. READ IT FIRST. IF ANY R0 FAILS, STOP

### R0a — the build line names THIS branch's head

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, this PR's head as `git ls-remote origin refs/heads/feat/dragon-blast` prints it, shortened. **Not** master's `94915476`. Not `-dirty`, not `unknown` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | |

### R0b — the jar carries step R3

| prediction | instrument | READING |
|---|---|---|
| PRESENT: the seven entries below the control. In `content/mobs/phage.yml`, the lines `blast:` and `  fuse_ticks: 30`; in `selftest/GATE-dragon-blast.yml`, the line `  - difficulty: normal`. ABSENT: the control `selftest/NoSuchClassControl.class` | the scan below | |

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
if ($p -match "(?m)^  fuse_ticks: 30\s*$") { "PRESENT fuse_ticks: 30" } else { "ABSENT  fuse_ticks: 30" }
if ((Text 'selftest/GATE-dragon-blast.yml') -match "(?m)^  - difficulty: normal\s*$") { "PRESENT difficulty dragon-blast" } else { "ABSENT  difficulty dragon-blast" }
$zip.Dispose()
```

### R0c — the Loaded line, and nothing refused

| prediction | instrument | READING |
|---|---|---|
| **`[Rpg] Loaded 11 abilities, 29 visuals, 5 statuses, 7 elements, 11 enchants, 3 pools, 6 fragments, 6 aspects, 16 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 5 mobs, 1 mob elements, 3 recipes, 1 ores, 4 vein sizes`**, verbatim `GATE-dragon-fix.md` R0c's reading: R3 adds a block to `phage.yml` and no content file (`git diff --stat 94915476 HEAD -- paper/src/main/resources/content` names `content/mobs/phage.yml` only), and the Phage still loads (`MobLoaderTest.everyBundledMobLoads`). No `Refusing`, `Skipping`, `SEVERE` or `Exception`, and no `[Rpg]` `WARN`, beyond `volley_stone`'s known pair (`Content: weapon 'volley_stone' …`, `1 content problem(s)`) | `Select-String run\logs\latest.log -Pattern 'Loaded ','Refusing','Skipping','SEVERE','WARN\]: \[Rpg\]','Exception','Content:'` | |

### R0d — the dev marker reached the JVM, and every scenario loaded

| prediction | instrument | READING |
|---|---|---|
| Exactly one line **`[Rpg] Selftest: dev server, 91 scenarios across 15 gates [melee-cell, level, nexus-polish, legacy-a, legacy-b, wither-status, withered-shortbow, sunder, deft-hands, mining-auto, nexus-back, dragon-bodies, dragon-split, dragon-growth, dragon-blast]`**, and **no** `Refusing selftest` line (a malformed `detonate`, `ignite`, `probe_fuse` or `await_fuse` step is refused at load, by name) | `Select-String run\logs\latest.log -Pattern 'Selftest:','Refusing selftest'` | |

**The 91 is DERIVED.** From `paper/src/main/resources/selftest/` at this head: `grep -cE '^  - ' index.yml` prints
**15**; `cat $(grep -E '^  - ' index.yml | tr -d '\r' | sed 's/  - /GATE-/; s/$/.yml/') | grep -cE '^[A-Za-z0-9-]+:'`
prints **106**; the same with `'^setup:'` prints **15**: 106 − 15 = **91**. At master `94915476` the first two print 14
and 96 (82, `GATE-dragon-fix.md` R0d's reading: the control). `SelfTestScenariosTest` pins the 91.

---

## THE AUTO ROWS — SERVER ONLY, SURVIVAL, DIFFICULTY NORMAL, GEAR SCORE 100

**The log instrument for every row:**
`Select-String run\logs\latest.log -Pattern 'SELFTEST dragon-blast|\[Rpg\] BLAST|\[Rpg\] GROWTH|\[Rpg\] SPLIT|CREEPERGUARD'`,
read after Ben's LOGIN, inside the row's own `SELFTEST dragon-blast <row> START … DONE` bracket.

**In EVERY row below (and not repeated in each):** after the row's `CLEAN …` line, **`LEFTOVER living=0 radius=16`**;
for each mob the row spawns, one `DRIVE spawn tag=<its tag> <uuid> <mob> frozen` (or `live` where the row says) and
the command's reply: a stage-1 Phage `REPLY "Spawned Phage (creeper, stage 1, 80 HP, GS 100 given)" [aqua, gray]`, a
stage-5 `REPLY "Spawned Phage (creeper, stage 5, 160 HP, GS 100 given)" [aqua, gray]`. **No `CREEPERGUARD prime` line
anywhere in the run** (ruling 26 removed it). **No `SPLIT` line anywhere in the run.** **A non-zero LEFTOVER in any row
makes that row NOT AS PREDICTED**, whatever else it reads, and names what was there.

**A row that heals** logs `REPLY "Custom HP: <M>/<M>" [green]`, M his maximum, recorded and not predicted.

### BL1 — a LIVE stage-1 Phage blows on the player for 20 before defence; it is gone; the floor is whole (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) `BLOCKS STONE count=108 box=-3 -1 -4 8 -1 4` before the spawn. (2) **Exactly one** `[Rpg] BLAST mob=phage id=<the bl1 uuid> stage=1 gs=100 damage=20.000 radius=3.00 victims=1 [player <Ben's uuid> d=<recorded; at most 3.00>] spared=0 []`. (3) **Exactly one** `HURT before=20.000 after=<A> defense=<D> dealer=<the bl1 uuid> dealerType=<recorded>`, after the BLAST line, with **A = 20 × 100 / (100 + D)** to the printed three places (D ≤ 0: A = 20.000). (4) `ENTITIES CREEPER count=0 radius=16`. (5) After `DRIVE return <x y z>`, `BLOCKS STONE count=108 box=-3 -1 -4 8 -1 4` again. (6) No `GROWTH` line | S2-S6; (1)/(5) the box is 12 × 1 × 9 = 108 (`GATE-dragon-fix.md` DB10R2 read it) and nothing of ours touches a block | |

### BL2 — a LIVE stage-5 Phage blows for 50 before defence, and leaves NO children (SURVIVAL) — Ben's E

| prediction | stands on | READING |
|---|---|---|
| (1) `BLOCKS STONE count=108 …` before. (2) **Exactly one** `[Rpg] BLAST mob=phage id=<the bl2 uuid> stage=5 gs=100 damage=50.000 radius=3.00 victims=1 [player <Ben's uuid> d=<recorded; at most 3.00>] spared=0 []`. (3) **Exactly one** `HURT before=50.000 after=<50 × 100 / (100 + D)> defense=<D> dealer=<the bl2 uuid> dealerType=<recorded>`. (4) `ENTITIES CREEPER count=0 radius=16`: **zero children**. (5) **No `SPLIT` line in the bracket.** (6) The second `BLOCKS STONE count=108 …` | S2-S6; (4)/(5): removal raises no death event (`CraftEntity#remove` is `discard(PLUGIN)`, `RpgListeners.onMobDeath`'s javadoc; `DragonBlastWiringTest.theBlastRemovesThePhageWithoutADeath`) | |

### BL3 — a blast is a Void death: a stage-1 Phage 3.00 away grows to stage 2; the one that blew never grows (SURVIVAL)

A frozen stage-1 Phage (`bl3x`) five blocks east is blown by DETONATE; a frozen stage-1 Phage (`bl3b`) stands 3.00 north
of it. The player is 5.00 from the blast, outside its radius.

| prediction | stands on | READING |
|---|---|---|
| (1) `DRIVE detonate tag=bl3x <the bl3x uuid>`. (2) **Exactly one** `[Rpg] BLAST mob=phage id=<the bl3x uuid> stage=1 gs=100 damage=20.000 radius=3.00 victims=0 [] spared=1 [creeper/phage <the bl3b uuid> d=3.00]`. (3) **Exactly one** `[Rpg] GROWTH mob=phage id=<the bl3b uuid> from=1 to=2 fed=creeper/phage <the bl3x uuid> element=void d=3.00 max=100`; **no GROWTH line names bl3x.** (4) `BODY tag=bl3b <uuid> type=creeper mob=phage stage=2 scale=0.882 height=1.500 speedBase=0.3125 speedType=0.2500 speedX=1.250 maxHP=100.000 gs=100 element=void dead=false`. (5) `BODY tag=bl3x none within 16 blocks`. (6) `ENTITIES CREEPER count=1 radius=16`. (7) No `HURT` line | S2, S7; (3) growth's death path with the Phage's own id (`MobGrower.onDeath`, `MobGrowth.stageAfter` skips the dead id: `MobGrowthTest.theDyingCreatureNeverGrowsItself`); stage 2's body as `GATE-dragon-fix.md` DG1R2 read it, its health 100 = stage 2's 100 × GS 100/100 | |

### BL4 — a frozen husk and a frozen stage-5 Phage inside the blast keep their health (SURVIVAL) — Ben's G

A frozen stage-1 Phage (`bl4x`) five blocks east is blown by DETONATE; a frozen husk (`bl4h`) 2.00 north of it and a
frozen stage-5 Phage (`bl4p`) 2.00 south. The player is 5.00 from the blast.

| prediction | stands on | READING |
|---|---|---|
| (1) Before the blast: `PROBE tag=bl4h <uuid> husk customHP=100.000/100.000 fireTicks=0 dead=false d=5.385 dy=0.000` and `PROBE tag=bl4p <uuid> creeper customHP=160.000/160.000 fireTicks=0 dead=false d=5.385 dy=0.000`. (2) `REPLY "Spawned husk (husk, 100 HP, GS 100 given)" [aqua, gray]` for the husk. (3) **Exactly one** `[Rpg] BLAST mob=phage id=<the bl4x uuid> stage=1 gs=100 damage=20.000 radius=3.00 victims=0 [] spared=2 [<both, in either order: husk <the bl4h uuid> d=2.00, creeper/phage <the bl4p uuid> d=2.00>]`. (4) After it, the two PROBE lines **unchanged**. (5) **No GROWTH line** (a stage 5 does not grow; a husk is not a candidate). (6) `ENTITIES CREEPER count=1 radius=16` | S8; `d=5.385` is √29 (jshell: `String.format("%.3f", Math.sqrt(29.0))` prints 5.385); `fireTicks=0` stands on `GATE-dragon-fix.md` DG7R2's frozen creeper reading 0 (INFERRED to hold for a husk); (2)'s colours INFERRED from the Phage's reply (an unnamed mob's type key inherits the aqua) | |

### BL5 — backing away cancels the fuse: past 7 blocks the swell falls and nothing blows (SURVIVAL) — Ben's F

A live stage-1 Phage two blocks east. When its swell reaches 10 of 30, the player steps nine blocks west (onto the
row's floor, which reaches x − 12). Five ticks later the swell is read, and then the Phage is killed, so it cannot follow
him into the next row.

| prediction | stands on | READING |
|---|---|---|
| (1) `FUSE tag=bl5 <uuid> fuse=10/30 ignited=false waited=<recorded>`. (2) After the step and five ticks, `FUSE tag=bl5 <uuid> fuse=<n>/30 ignited=false` with **n < 10**: **a BOUND, not a figure** (n is recorded): which tick the goal turns the swell down on, relative to the player's task, was not read, so n is 6, 7 or 8 by the READ mechanism, and the bound is what the claim needs. (3) `ENTITIES CREEPER count=1 radius=16`: alive. (4) `CMD /kill @e[tag=bl5]`, `SETTLED dying=0 ticks=<t> first=1`, then `ENTITIES CREEPER count=0 radius=16`. (5) **No BLAST line and no HURT line in the bracket.** (6) No SPLIT (a stage 1) and no GROWTH line (no other Phage) | S1, S3: the swell rises one a tick, so the first reading ≥ 10 polled each tick is 10; nine blocks west of a Phage at most two east is more than 7; past 7 the goal turns the swell down | |

### BL6 — a victim at exactly 3.00 blocks is hit (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) `DRIVE detonate tag=bl6 <the bl6 uuid>`. (2) **Exactly one** `[Rpg] BLAST mob=phage id=<the bl6 uuid> stage=1 gs=100 damage=20.000 radius=3.00 victims=1 [player <Ben's uuid> d=3.00] spared=0 []`. (3) **Exactly one** `HURT before=20.000 after=<20 × 100 / (100 + D)> defense=<D> dealer=<the bl6 uuid> dealerType=<recorded>` | S5-S7: the feet are exactly 3 apart, and the radius is inclusive | |

### BL7 — a victim 3.05 blocks away is not (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) `DRIVE detonate tag=bl7 <the bl7 uuid>`. (2) **Exactly one** `[Rpg] BLAST mob=phage id=<the bl7 uuid> stage=1 gs=100 damage=20.000 radius=3.00 victims=0 [] spared=0 []`. (3) **No HURT line in the bracket.** BL6 is this row's control: the same staging 0.05 nearer is hit | S7 | |

### BL8 — flint and steel's route still cannot light a Phage (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) `DRIVE ignite tag=bl8 <the bl8 uuid>`, then `[Rpg] CREEPERGUARD ignite cancelled mob=phage id=<the bl8 uuid>`. (2) Forty ticks later, `FUSE tag=bl8 <uuid> fuse=0/30 ignited=false`. (3) `ENTITIES CREEPER count=1 radius=16`. (4) **No BLAST line in the bracket** | `CraftCreeper#ignite()` → the server's `ignite()` → `CreeperIgniteEvent` (§7 F-C; flint and steel's `mobInteract` calls the same `ignite()`, `CustomCreeperGuard`'s javadoc), refused by `RpgListeners.onCustomCreeperIgnite`; S1: frozen, it never swells | |

**WHAT IS UNIT-ONLY, AND WHY:** a CREATIVE player inside a blast another player triggered. The selftest refuses to run
with more than one player online (`SelfTestGuard`), and a creative runner is refused too, so no row can stage it. The rule
is `MobBlastTest.mobsAreHostileToPlayersOnly` and `theBlastHitsPlayersInsideTheRadiusAndSparesEveryoneElseInside`: the
creative player is **not hurt**, and the BLAST line names him under `spared`.

---

## BEN'S ROWS — BY THE ASK STEP. EACH ANSWER IS ONE WORD

### BQ1 — the blast looks and sounds like one; the small one hurts "a little", the big one "a lot" (SURVIVAL)

Ben's words for the two sizes (PLAN-dragon-mobs.md, RULINGS, *On the Phage and the Goldrot Goon*): the big Phage
*"When it gets close to a player it explodes dealing a lot of damage"*; the small ones *"When they get close to the
player they explode dealing a little damage."* He is healed before each. A live stage-1 Phage (`bq1small`) blows on him
first (its BLAST and HURT lines are recorded beside the answers, `damage=20.000`); then a live stage 5 (`bq1big`,
`damage=50.000`). Each question waits 1200 ticks (one minute) for his next chat line. **These are at gear score 100**, so
the damage he feels is the authored 20 and 50 before his defence; at the End's 300 it would be three times that.

| prediction | READING |
|---|---|
| `ASK 1 question="Did that look and sound like an explosion?" answer="<his line>" ticks=<t>` | |
| `ASK 2 question="Your design says the small Phage explodes 'dealing a little damage'. Did that feel like a little?" answer="<his line>" ticks=<t>` | |
| `ASK 3 question="Your design says the big Phage explodes 'dealing a lot of damage'. Did that feel like a lot?" answer="<his line>" ticks=<t>` | |

**The answer cell is his line, verbatim.** "not sure", or no line before the minute is out
(`answer=NOT STATED after 1200 ticks`), is recorded as NOT STATED, never re-asked in this boot.
