# GATE — STEP F9e: THE COPY AND THE REVIVE (AND THE ONE BOOT FOR F9d's GATE TOO)

**Status: NOT RUN.** Every prediction below was written **before** any boot of `feat/fight-revive`, and this file is
committed before the boot. **It carries the ONE R0 and the ONE run sheet** for this gate and `GATE-fight-down.md`
(F9d, the branch this one is stacked on). The boot is on THIS branch's head.

**Game mode:** every row is **SURVIVAL**. RV4 and RVB3 (and every FD row) put him in SPECTATOR on purpose, and every
row ends with him home in survival. **Difficulty normal** (each setup sets it).

**Ben's rulings** (PLAN-dragon-fight.md section 1, 0d and 0e, verbatim):
- "When they die, a copy of them is on the floor. Another player has to hold shift/crouch on top of that players copy
  for 3 seconds to respawn them."
- "1. The copy should be placed just inside the ring in both cases / 2. 50% health / 3. No / 4. No but we will add
  modifiers to change that later".

**UNREAD ON A SERVER UNTIL A SECOND PLAYER EXISTS:** reviving a REAL player. Every row here is solo. A revive of a down
player needs another LIVING participant. The revive's code path is read up to restoring a player by the PRACTICE copy
(RVB2), and from there only by `FightReviveSourceTest` and core's `ReviveTrackerTest` (two and three players).

## WHAT WAS READ, EACH MARKED

- **The copy is an `org.bukkit.entity.Mannequin`, and it is a `LivingEntity`** (READ, javap on the pinned
  `paper-api`). It has `setProfile(ResolvableProfile)`, `setImmovable`, `setDescription` (the line under its name),
  `ResolvableProfile.resolvableProfile(PlayerProfile)` and a builder taking a `name`. `Entity` has
  `setPose(Pose, boolean)`, `setInvulnerable`, `setPersistent`, `setGravity`, `addScoreboardTag`. `LivingEntity` has
  `setCollidable`. `EntityType.MANNEQUIN` exists (a probe can count it).
- **It is NOT a `Mob`, a `Creeper`, a `Player` or an `ArmorStand`** (READ, the class hierarchy;
  `FightReviveSourceTest` asserts it by reflection).
- **That it draws lying down** (pose SLEEPING, fixed) is INFERRED. **Ben's eyes confirm** (RVB1).
- **The stats bar draws the action bar every 10 ticks unless a hold owns it** (READ, `StatsBarSystem`,
  `ActionBarHolds`). The revive's progress takes a 10-tick hold each tick it shows (SEAT PLACEHOLDER: the action bar).
- **The Mage never attacks** (READ, `content/mobs/dark_mage.yml`: "no goals at all ... never moves, attacks or
  targets").
- **The store had no way to SET a health** (READ). `CombatantStats.setCurrent` is new: a rise is a HEAL, a fall a
  DAMAGE dealt by himself (`CombatantStatsTest`).
- **The selftest's one untyped living probe, `LEFTOVER`, is taken at a row's START** (READ, `SelfTestRun`), before any
  fight, and every copy is gone by then. **So no row can show that probe leaving a copy out**; its exclusion is
  witnessed by `FightReviveSourceTest` alone. RV1's typed probe is the positive control that a probe can see a copy.

## THE EXCLUSION LIST: EVERY PLACE THE COPY IS LEFT OUT, EACH WITH ITS TEST

**One predicate decides: `FightCopies.isCopy`, "every Mannequin is a copy"** (CC's reading, flagged: nothing else in the
plugin makes one). The places were mapped by a read-only sweep of `paper/src/main` (eight kinds), and each is in
`FightReviveSourceTest`:

| place | how it is left out | test |
|---|---|---|
| **LiveTargets.isLive**: every area, ray, projectile hit, implosion drag, Knell beam, Phalanx shot, mount pounce, Phage blast, growth candidate and dev look-ray | `&& !FightCopies.isCopy(entity)` | *theCopyIsNeverALiveBody* |
| **MobNameplateManager.seed**: the health store, the gear score, the nameplate, the boot sweep, the add event, the melee rider's seed | `if (FightCopies.isCopy(mob)) return null;` | *theCopyIsNeverSeeded* |
| **a player's attack on it** | `onCopyAttacked`, LOWEST, cancelled | *theCopyCannotBeAttacked* |
| **the fight's kill counting** | `onDeath` returns for a copy | *theCopyIsNeverAKill* |
| **the selftest's LEFTOVER count** and **its aim ray** | filtered by `isCopy` | *theSelftestsLivingProbesLeaveItOut* |
| **the beamer, the shooter, the pouncer** (`instanceof Mob`), **the Phage's fuse** (`instanceof Creeper`) | by its nature: not a Mob, not a Creeper | *aMannequinIsLivingButNoMob…* |
| **growth and split** (need a `mob_id`) | by its nature: no `mob_id` | the seed's test (nothing ever tags it) |
| **the DamageGate** | by its nature: keyed by the dragon's and the Mage's ids only | (no generic selection to exclude) |
| **vanilla damage and the store's reroute** | invulnerable, and never in the store | *theCopysBody* |

## THE ARITHMETIC (SELFTEST SAFETY): AT DEFENCE 0, HIS HEALTH 350 (every heal's reply)

| row | what can hurt him | the most | |
|---|---|---|---|
| RV1, RV2, RVB1, RVB2 | nothing: no dragon, no Mage; he stands inside the ring (every strike deals 0) | 0 | cannot hurt him |
| RV3 | nothing: the Mage never attacks; he is 2 from it, inside its 12 ring | 0 | cannot hurt him |
| RV4 | the closed ring's lightning: 20, 40, 80, 160, 320 a strike, 40 ticks apart | **lethal, on purpose** | he goes DOWN, not dead |
| RVB3 | `/kill` | **lethal, on purpose** | DOWN |
| `fight-down` FD1-FD4 | as `GATE-fight-down.md`'s table | **lethal, on purpose** | DOWN |

Every row in which anything can hurt him sets `death_rules` and heals first, and ends with him home, in survival,
healed.

## CARRIED STATE (CC, IN THIS ORDER) — A CHECKLIST, EACH LINE TICKED IN THE REPORT, NOT REMEMBERED

The booter's checklist is `.claude/rules/booting.md`; this list is its instance for this boot.

1. **No JVM:** `Get-Process java -ErrorAction SilentlyContinue` prints nothing.
2. **A fresh dated backup:** `run/plugins/Rpg` and `run/world` copied, as they are, to
   `../rpg-run-backups/2026-11-<dd>-pre-fight-revive`. Report:
   - the file and directory counts;
   - `diff -rq lines: 0` for each;
   - the `ls` of `world/dimensions`;
   - the overworld's file count (`world/dimensions/minecraft/overworld`).

   Nothing is restored (Ben's player file: his decision).
3. `grep -n "dev-pack-on-join" run/plugins/Rpg/config.yml` prints `12:  dev-pack-on-join: true`.
4. **No fight folders:** `find run/world/dimensions/rpgfight -mindepth 1` prints nothing.
5. **IN ITS OWN MESSAGE, BEFORE THE BOOT COMMAND IS SENT:** "booting now", and Ben told **NOT to log in until told R0
   passed.** The boot command goes in the next turn.
6. `./scripts/dev-server.sh --refresh-content`. **The REQUIRED deployed lines:**
   - `content/fights/dragon.yml` reads `  all_down_seconds: 5`, `  revive_seconds: 3`, `  revive_health_fraction: 0.5`;
   - and NO `max_revives:` line.
7. R0a to R0e read on **this boot's** log (its `[Rpg] Build:` names this branch's head).
8. **IN ITS OWN MESSAGE: Ben told R0 passed.** Any R0 not as predicted: STOP, tell the seat, tell Ben NOT to log in.
9. *(After his `/stop`.)* Report:
   - `rpgfight` empty;
   - the overworld's file count against step 2's;
   - `All dimensions are saved`;
   - no `java` process left.

## THE RUN SHEET — BEN TYPES THESE, IN THIS ORDER. EVERY LINE IS LITERAL

0. **DO NOT LOG IN UNTIL CC HAS TOLD YOU R0 PASSED. IF YOU ARE TOLD NOTHING, DO NOT LOG IN.**
1. `/gamemode survival` — **alone, the first line after login.**
2. Stand on flat open ground with room all round.
3. `/rpg selftest fight-down` — **ONCE.** Four rows, about three minutes.
   - **EVERY ROW PUTS YOU DOWN ON PURPOSE** (FD1 a kill, FD2 the void, FD3 the Mage's power, FD4 a kill). You become a
     spectator for a few seconds and are then sent home in survival, with everything you carried.
   - **Nothing else can hurt you.** Do not move while it runs.
4. `/rpg selftest fight-revive` — **ONCE.** Seven rows, about five minutes, and seven questions.
   - **RV4 and RVB3 put you down on purpose**, and nothing else hurts you.
   - RVB2 asks you to sneak on a practice copy.
   - Each question says whether it wants yes or no, or your own words.
5. **THE MANUAL ROW (FDM1, `GATE-fight-down.md`):**
   - Type `/rpg fight enter nodragon`.
   - Walk off the edge of the floor into the void.
   - **As soon as you are spectating, disconnect.**
   - Wait ten seconds, reconnect, and say where you are.
6. `/stop`

Nothing else this boot.

## R0 — THE BUILD. READ IT FIRST. IF ANY R0 FAILS, STOP

### R0a — the build line names THIS branch's head

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, `feat/fight-revive`'s head as `git ls-remote origin refs/heads/feat/fight-revive` prints it, shortened. Not `-dirty`, not `unknown` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | |

### R0b — the jar carries both steps

| prediction | instrument | READING |
|---|---|---|
| PRESENT: `dragon.yml` lines `  all_down_seconds: 5`, `  revive_seconds: 3`; `FightInstance.class` carrying `FIGHT copy laid`; `FightFixtures.class` carrying `FIGHT down player=` and `FIGHT revived player=`; the entries `io/github/butterflysmp/rpg/core/fight/DownRoster.class`, `…/CopyPlace.class`, `…/ReviveTracker.class`; `GATE-fight-down.yml` carrying `FD1:`; `GATE-fight-revive.yml` carrying `RV1:`. ABSENT: `dragon.yml` line `  environment: normal`, `NoSuchTextControl` in `FightInstance.class` | the PowerShell below, from the repo root | |

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path 'run\plugins\rpg-0.1.0-SNAPSHOT.jar'))
function Bytes($name) { $e = $zip.GetEntry($name); if ($e -eq $null) { return $null }; $s = $e.Open(); $m = New-Object IO.MemoryStream; $s.CopyTo($m); $s.Close(); [Text.Encoding]::GetEncoding(28591).GetString($m.ToArray()) }
function Line($text, $l, $label) { if ($text -ne $null -and $text -match ("(?m)^" + [regex]::Escape($l) + "\s*$")) { "PRESENT $label '$l'" } else { "ABSENT  $label '$l'" } }
$d = Bytes 'content/fights/dragon.yml'
Line $d '  all_down_seconds: 5' 'dragon.yml'; Line $d '  revive_seconds: 3' 'dragon.yml'; Line $d '  environment: normal' 'dragon.yml'
function Has($entry, $needle) { $b = Bytes $entry; if ($b -ne $null -and $b.Contains($needle)) { "PRESENT $entry '$needle'" } else { "ABSENT  $entry '$needle'" } }
Has 'io/github/butterflysmp/rpg/paper/fight/FightInstance.class' 'FIGHT copy laid'
Has 'io/github/butterflysmp/rpg/paper/fight/FightInstance.class' 'NoSuchTextControl'
Has 'io/github/butterflysmp/rpg/paper/fight/FightFixtures.class' 'FIGHT down player='
Has 'io/github/butterflysmp/rpg/paper/fight/FightFixtures.class' 'FIGHT revived player='
foreach ($c in 'DownRoster', 'CopyPlace', 'ReviveTracker') { if ($zip.GetEntry("io/github/butterflysmp/rpg/core/fight/$c.class") -ne $null) { "PRESENT entry $c.class" } else { "ABSENT  entry $c.class" } }
$f = Bytes 'selftest/GATE-fight-down.yml'; if ($f -ne $null -and $f -match "(?m)^FD1:\s*$") { "PRESENT GATE-fight-down.yml FD1:" } else { "ABSENT  GATE-fight-down.yml FD1:" }
$r = Bytes 'selftest/GATE-fight-revive.yml'; if ($r -ne $null -and $r -match "(?m)^RV1:\s*$") { "PRESENT GATE-fight-revive.yml RV1:" } else { "ABSENT  GATE-fight-revive.yml RV1:" }
$zip.Dispose()
```

**A jar from master reads ABSENT on every PRESENT line**, and the two controls ABSENT on either jar.

### R0c — the Loaded lines, and nothing refused

| prediction | instrument | READING |
|---|---|---|
| **`[Rpg] Loaded 11 abilities, 30 visuals, 5 statuses, 7 elements, 11 enchants, 3 pools, 6 fragments, 6 aspects, 16 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 7 mobs, 1 mob elements, 3 recipes, 1 ores, 4 vein sizes`** (unchanged), AND exactly one **`[Rpg] Loaded fight 'dragon': border 48.0, mage ring 12.0, v_fast 3.80, v_slow 0.60, e_kill 1.00, 10 crystals every 240 ticks, a strike every 40 ticks from 20.0; ring dust (170,0,255) size 1.60 every 1.00 blocks at [0.2, 1.7, 3.2, 4.7, 6.2, 7.7], row shift 0.25, turn 1.00/s alternating, every 4 ticks; mage 'dark_mage' refused item.shield.block, landing lightning on; crystals at 40.0, 12.0 up, lit block.beacon.power_select; participants within 128.0; arena at (0, 0) floor y 64, disc 56 x 3, 10 pillars 3 wide on 40.0 to +12, time 18000, entry (0.0, 0.0), environment end, all down 5 s; dragon 'ender_dragon' Mages below [0.8, 0.6, 0.4, 0.2], win return 200 ticks, bar purple, bite 80.0, wing 40.0, breath 30.0/s, one contact hit per 20 ticks; revive within 1.50 blocks for 3 s at 0.50 of max, no limit, label '{player} is down: sneak on them for {seconds} seconds to revive'`** (`FightLoaderTest` asserts it verbatim). No `Refusing`, `Skipping`, `SEVERE` or `Exception`. **No `[Rpg]` WARN beyond `volley_stone`'s known pair.** No `No fights loaded` | `Select-String run\logs\latest.log -Pattern 'Loaded\|Refusing\|Skipping\|SEVERE\|WARN\|Exception\|No fights loaded'` (up to the first `joined the game`) | |

### R0d — every scenario loaded

| prediction | instrument | READING |
|---|---|---|
| Exactly one **`[Rpg] Selftest: dev server, 228 scenarios across 28 gates [melee-cell, level, nexus-polish, legacy-a, legacy-b, wither-status, withered-shortbow, sunder, deft-hands, mining-auto, nexus-back, dragon-bodies, dragon-split, dragon-growth, dragon-blast, dragon-beam, dragon-teleport, dragon-shot, dragon-blink, dragon-skreel, drag-additive, fight-site, fight-arena, fight-dragon, fight-rules, fight-end, fight-down, fight-revive]`**, no refusal after it. **The control:** the last boot's `217 scenarios across 26 gates` is ABSENT | `Select-String run\logs\latest.log -Pattern 'Selftest:'` | |

**RECOUNTED** (2026-11-19, this branch's tree; `SelfTestScenariosTest` asserts 228):
- the gates in `index.yml`: **28**;
- `^[A-Za-z0-9_-]+:` over them: **256**;
- `^setup:`: **28**;
- so **228**.

**Control:** 243 (the second F9 boot) + fight-down's 5 + fight-revive's 8 = 256.

### R0e — THE ORPHAN SWEEP WITH NOTHING STAGED

| prediction | instrument | READING |
|---|---|---|
| During the enable, exactly one **`FIGHTWORLD sweep path=<abs>\run\world\dimensions\rpgfight folders=0`**, and no `deleting` and no `orphan` line | `Select-String run\logs\latest.log -Pattern 'FIGHTWORLD (sweep\|deleting\|orphan)'` | |

## WHAT THIS BRANCH ADDS TO `GATE-fight-down.md`'s ROWS (they run on this boot, on this code)

**Each FD row's prediction there stands, PLUS these lines** (written here, before the boot, because this branch's
code writes them; `GATE-fight-down.md` carries a dated SUPERSEDED note pointing here):

- **FD1:**
  - after `FIGHT down`, **`FIGHT copy laid player=BaronVonYeetus t=<t> at=<about 0.00> 0.00 <about 0.00> died=<his offset> moved=false revivable=true`**;
  - while he is down, a **`COPYTICK t=<t> copy=BaronVonYeetus at=… r=48.000 centre=0.000 0.000 inside_by=<about 48> revivable=true`** every 4 ticks;
  - at the end, **`FIGHT copy removed player=BaronVonYeetus t=<t'+100> why=end`** before `FIGHT ended`.
- **FD2 (a death in the void):** **`FIGHT copy laid player=BaronVonYeetus … at=47.00 0.00 <about 0.00> died=<about 60> <below -129> … moved=true revivable=true`**: the floor one block inside the border, 48 - 1. Then COPYTICK `inside_by=1.000`, and removed at the end.
- **FD3:** laid where the powered kill put him down (inside the ring the kills held open), `moved=false`; removed at the end.
- **FD4:** laid at the kill; **`FIGHT copy removed player=BaronVonYeetus t=<t> why=out`** at the teleport out, before `FIGHT nobody inside`.
- **FDM1 (manual):** laid at the void death (`moved=true`); **`FIGHT copy removed … why=out`** at his quit.
- **No row leaves a copy:** each world is deleted with no `MANNEQUIN` in it.

## THE AUTO ROWS — SERVER ONLY, SURVIVAL, DIFFICULTY NORMAL

### RV1 — a practice copy inside the ring; a probe sees it; the end removes it (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) The birth, as FD1's. (2) **`FIGHT copy laid practice name=Practice t=<t> at=5.00 0.00 0.00 asked=5.00 0.00 moved=false revivable=true`**; then a `COPYTICK … copy=Practice at=5.000 0.000 r=48.000 centre=0.000 0.000 inside_by=43.000 revivable=true` every 4 ticks. (3) **`ENTITIES MANNEQUIN count=1 radius=16 dying=0`**: the positive control, a typed probe sees the copy. (4) `rpg fight leave`: `FIGHTWORLD left … why=leave`, `returned`; on the next fight tick `FIGHT nobody inside`, **`FIGHT copy removed practice name=Practice t=<t> why=end`**, `FIGHT ended reason=nobody-inside-RUNNING`, the world deleted. (5) **`ENTITIES MANNEQUIN count=0 radius=64 dying=0`** (home) | `CopyPlaceTest` *aDeathInsideTheRing…*; `FightReviveSourceTest` *theCopysBody*, *theCopyIsLaidOnDown…* | |

### RV2 — a practice copy asked for outside the ring lands one block inside its edge (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) **`FIGHT copy laid practice name=Outside t=<t> at=47.00 0.00 0.00 asked=60.00 0.00 moved=true revivable=true`**. (2) `COPYTICK … copy=Outside at=47.000 0.000 r=48.000 … inside_by=1.000 revivable=true`. (3) The end as RV1's | `CopyPlaceTest` *aDeathOutsideLies…* | |

### RV3 — during a fast close the copy's place stays inside the ring at every ring tick (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) `FIGHT land t=<S> dx=0.000 dz=0.000 result=STARTED`, `FIGHTPHASE t=<S> IDLE->FAST_CLOSE`, `FIGHT mage spawned …`. (2) **`FIGHT copy laid practice name=Edge t=<S> at=40.00 0.00 0.00 asked=40.00 0.00 moved=false revivable=true`**. (3) **EVERY `COPYTICK … copy=Edge` LINE HAS `inside_by=` AT LEAST 1.000** (at the printed 3 decimals, 0.999 allowing the rounding). r falls from 48 towards the Mage's 12; while r - 1 is more than 40 the copy stays at 40, and after that it is `at=<r - 1> 0.000 … inside_by=1.000`. (4) `FIGHTPHASE … FAST_CLOSE->SLOW_CLOSE` about 190 ticks on, the copy then at about 11. By the row's end the slow close (0.6 a second) has taken r below 12, the copy one block inside. (5) Every `STRIKE … k=0 damage=0.000 inside`. (6) The end, the copy removed `why=end`, the Mage removed | `CopyPlaceTest` *aClosingRing…*, *aMovingRing…*; `FightReviveSourceTest` *eachTickTheCopyIsPlaced…* | |

### RV4 — the ring CLOSED: his copy stays where he went down and cannot be revived (SURVIVAL; DOWN ON PURPOSE)

| prediction | stands on | READING |
|---|---|---|
| (1) As AE4R3 (`GATE-fight-down.md`): the slow close shuts the ring, `FIGHT FAILED_CLOSED t=<c>`, and the lightning strikes him (`STRIKE … outside`, 20 then doubling) until he goes down: **`FIGHT down player=BaronVonYeetus t=<d> cause=<c> at=<about -2.00 0.00 0.00>`**. (2) **`FIGHT copy laid player=BaronVonYeetus t=<d> at=<the same -2.00 0.00 0.00> died=… moved=false revivable=false`**: no place is inside a closed ring. (3) `COPYTICK … r=0.000 … revivable=false`. (4) `FIGHT all-down … outcome=FAILED_CLOSED returns_in=100`; 100 ticks later the return, `mode … from=SPECTATOR to=SURVIVAL why=end`, the copy removed `why=end` | `CopyPlaceTest` *aClosedRing…*; `DownRosterTest` | |

## BEN'S ROWS — HIS WORDS, VERBATIM; "not sure" IS NOT STATED; ONE THING PER QUESTION; NO PASS OR FAIL

### RVB1 — the practice copy's look: questions 1 and 2 (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) `FIGHT copy laid practice name=Practice … at=3.00 0.00 0.00 … moved=false revivable=true`. (2) His answers, verbatim: **1** "Does the copy look like a player lying on the floor, with a name and a line above it?" and **2** "What, if anything, looks wrong with the copy?". The pose drawing lying down is INFERRED; his answer is the witness | `FightReviveSourceTest` *theCopysBody* | |

### RVB2 — the revive: questions 3, 4 and 5 (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) The practice copy at 3 0. (2) When he sneaks on it: **`FIGHT revive started by=BaronVonYeetus copy=Practice t=<t>`**. When he lets go: **`FIGHT revive broken by=BaronVonYeetus copy=Practice t=<t>`**, and his bar reads `Revive of Practice interrupted`. (3) On the 60th unbroken tick: **`FIGHT revived practice name=Practice by=BaronVonYeetus t=<t> at=3.00 0.00 0.00`** and `FIGHT copy removed practice name=Practice t=<t> why=revived`. His bar reads `Reviving Practice: <x> / 3 s` while he holds. (4) His answers, verbatim, to **3**, **4** and **5**. **If no `revived` line appears, he did not hold it for three seconds within the radius, and the row says so** | `ReviveTrackerTest`; `FightReviveSourceTest` *theReviveOffers…*, *theProgressHolds…* | |

### RVB3 — going down himself and spectating: questions 6 and 7 (SURVIVAL; DOWN ON PURPOSE)

| prediction | stands on | READING |
|---|---|---|
| (1) As FD1 with this branch's lines: `FIGHT down`, `copy laid … revivable=true`, `spectating`, `FIGHT all-down`, five seconds, the return in survival, the copy removed `why=end`. (2) His answers, verbatim, to **6** and **7** | `GATE-fight-down.md` FD1 | |

## WHAT THIS GATE DOES NOT READ

- **A REAL REVIVE: a second player reviving a down one.** UNREAD on a server until one exists. Read in
  `ReviveTrackerTest` (two and three players) and wired in `FightReviveSourceTest`, up to the restore.
- **The revived player's mode, place and 50% health** on a server, for the same reason.
- **`max_revives`:** unset (Ben: "No"), so no limit is reached. The one rule is `ReviveRules.mayRevive`.
- **The `LEFTOVER` probe leaving a copy out:** no row can reach it (see READ above).
