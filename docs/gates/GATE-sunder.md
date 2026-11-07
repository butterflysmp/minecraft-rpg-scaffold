# GATE — SUNDER (Melee M2): the facing leap, the slam where it lands, no fall damage (and the stack's one R0)

**Status: NOT RUN.** Written 2026-11-06, before any boot of this branch. **NO BOOT until the seat says so.** This is the
new STACK TOP (`#168 → … → #175 → this`), so the stack's one R0 moves here from `GATE-selftest.md`, which records its R0
SUPERSEDED by this file.

Every prediction below was written **before** any row of this file was read, and **this file is COMMITTED before the
boot**. Readings go **beside** a prediction, never over it, and a prediction is not edited once its row has been read.

**Plan:** `docs/plans/PLAN-melee-class.md` section 8 (the mechanism), section 9.2 (the M2 table this file is built
from), and RULINGS: Ben's Q1 *"Always straight ahead"*, Q2 *"yes it slams where ever you land."*, Q3 *"None"*; the
seat's rulings 4 (the edge cases) and 5 (F-d: one mark detonates the slam and spends the fall immunity).

**EVERY SUNDER NUMBER IS PROVISIONAL** (section 8.5; Q4 not ruled): distance 5, speed 1.1, lift 0.5, radius 3.0,
knockback 0.6, cooldown 240, mana 30, and slam damage **20 — a PLACEHOLDER, Ben tunes**. They are predicted here as
authored, and M3's tuning boot moves them. A row that reads a number is reading the PROVISIONAL figure.

## GAME MODE

**SURVIVAL on every row** (fall damage is survival-only). `/rpg selftest` refuses to start in any other mode
(GATE-selftest.md ST7). Ben's hand rows (SU3, SU8) are SURVIVAL too.

```
ROWS     16   R0a R0b R0c R0d
              SU1 SU2 SU3 SU4 SU5 SU6 SU7 SU8 SU9 SU10 SU11 SU12
         ──
         16   = 4 R0 + 12 SU     git grep -c '^### R0\|^### SU' <ref> -- docs/gates/GATE-sunder.md
```

## THE WITNESSES, AND WHAT EACH CAN AND CANNOT SEE

- **`LANDING caster=<uuid> source=sunder how=<ending> slam=<true|false> at=<x y z | ->`**: new in this slice
  (`CombatWorld.traceLanding`, formatted by `Touchdown.line`, pinned by `TouchdownTest`). One line for EVERY ending
  of a Sunder mark, slam or not, under `/rpg mobtrace`. **So a "no slam" row reads a line that says `slam=false`,
  never an absent PLAYERHIT.** `at=` is the leaper's live feet, absolute block coordinates.
- **`PLAYERHIT … source=sunder … sent=<n> …`**: #168's trace, one per slam hit on a mob. `sent` is BEFORE Defense.
- **`PROBE tag=<t> …`**: a tagged mob's custom HP. **`SELF customHP=… at=… onGround=… fall=…`**: the PLAYER'S own
  state, new verb `probe_self` in this slice, because `probe` cannot see the player (`getNearbyEntities` excludes
  them).
- **`POSITIONS ZOMBIE n=… at=[…]`**: #175's `probe_positions`, relative to the player's feet.
- **CANNOT SEE:** the client's camera, the feel of the leap, the popup, a held key. Those are Ben's halves.

## THE HONESTY RULE, AS APPLIED HERE

**AUTO** = the instrument stages, drives through the shipped path and reads every predicted field from server lines.
**AUTO-PARTIAL** = it does part, and the row says which part is Ben's or unwitnessed. **BEN** = a hand row, because
the instrument cannot drive the condition; each says why.

| row | witness | what the instrument does | Ben's half / why not AUTO |
|---|---|---|---|
| SU1 | AUTO-PARTIAL | dumps the Build screen and the Left picker, and **picks Sunder into Left** through the real menu | the Ranger/Mage "not offered" half is a unit row (`PoolLoaderTest.aClassFileReachesOnlyItsOwnClass`), not driven |
| SU2 | AUTO | the stone's Left (`PlayerArmSwingEvent`) on flat ground, a mob at take-off and one beside the landing | the leap's look |
| SU3 | BEN | — | **the instrument cannot hold a movement key** (`Player#getCurrentInput` is the client's). With no key held, FACING and `movement_else_forward` both go forward, so an AUTO reading would be satisfied by either rule: a range, not a control |
| SU4 | AUTO | a wall 2.5 blocks ahead; a mob behind it | — |
| SU5 | AUTO | a 4-block ledge 1.5 blocks ahead | — |
| SU6 | AUTO | water beyond the take-off block | — |
| SU7 | AUTO-PARTIAL | a ceiling 2 blocks up | **the ending is a RANGE** (see the row), so only `slam=true` is a control |
| SU8 | BEN | — | **the instrument cannot survive its own player's death**: the run's restore would re-give an inventory that the death dropped (a duplication), and a respawn screen halts the row |
| SU9 | AUTO-PARTIAL | a cast teleported 1.2 blocks up, mid-air | a teleport, not a jump: the leap starts with no upward velocity of its own |
| SU10 | AUTO | the ledge again, with the player's HP before and after, then an ordinary 7-block drop | — |
| SU11 | AUTO | a GS-300 `/rpg spawn` zombie beside the landing | the popup and nameplate (secondary) |
| SU12 | AUTO-PARTIAL | an AI zombie, slowed so it does not walk, beside the landing; its position sampled | **the push distance is RECORDED, not predicted**; *UNVERIFIED*: that slowness 255 leaves knockback velocity untouched |

**A SIDE EFFECT THE RESTORE DOES NOT UNDO, STATED BEFORE THE BOOT:** SU1 SAVES the Fire Melee cell's Left = Sunder in
Ben's profile (a real pick, through the real menu). The run's restore puts back the cell, XP, inventory, position,
mode, mana, trace, rules, time and blocks — **not a saved loadout**. A player whose saved Fire Melee build still names
the deleted `active_placeholder_melee_thrust` reads that slot as **empty** until they pick (`LoadoutResolution` rule
2, by design), so SU1's pick is also what every later row needs.

## Set-up

- Ben logs in; **the Fire Melee cell** (every row's `expect_cell: "melee fire"` SKIPS otherwise). **SURVIVAL.**
- `/rpg selftest sunder` (or `all`). The gate's setup turns the trace on, sets night, stops mob spawns.
- **Each casting row opens with `wait: 240`**: Sunder's PROVISIONAL cooldown, so a row is never refused by the one
  before it. **Each row stages around wherever the player stands** (a leap moves them), aligned to a block centre and
  facing east (yaw −90): every `fill` box is SAVED first, and the run's restore puts every block back.
- Ben's hand rows (SU3, SU8) run AFTER the selftest, with `/rpg mobtrace` ON.
- **`/rpg selftest all` NOW CASTS SUNDER IN TWO MERGED ROWS, AND THEIR READINGS ARE NOT THOSE ROWS ANY MORE.**
  `GATE-melee-cell.md` MC3 and `GATE-level.md` WD3 (both merged, both read at 38cc9903) drive the stone's **Left**,
  which was `active_placeholder_melee_thrust` and is Sunder from this slice. MC3's Right and Q then fire from ~5
  blocks past its zombie. **Do not read MC3 or WD3 from a run at this head**: their gates are closed. Run the
  unmerged gates and `sunder` by name, or read those two rows' lines as noise.

---

## R0 — THE ONE R0 FOR THE WHOLE STACK (moved here from GATE-selftest.md). READ IT FIRST. IF ANY R0 FAILS, STOP

Every gate beneath carries its R0 SUPERSEDED, never PASS or FAIL. `GATE-selftest.md`'s note names this file. The one
exception is unchanged: `GATE-nexus-polish.md` R0d was #170's merge bar and is merged.

### R0a — the build line names this PR's head

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, this PR's head as `gh pr view <n> --json headRefOid` prints it, shortened. Not `-dirty`, not `unknown`, and not #175's or any head beneath | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | _(not run)_ |

### R0b — the jar carries every slice, Sunder, and NOT the deleted thrust

| prediction | instrument | READING |
|---|---|---|
| PRESENT: GATE-selftest.md R0b's ten slice entries (unchanged); **this slice's** core `build/Touchdown.class`, `content/abilities/sunder.yml`, `content/builds/melee.yml`. **ABSENT: `content/abilities/active_placeholder_melee_thrust.yml`** (deleted here; a jar that still carries it is a stale build) and the control. In `sunder.yml`, as anchored YAML: PRESENT `direction: facing`, `on_land:`; ABSENT a top-level `safe_landing:` | the scan below | _(not run)_ |

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path 'run\plugins\rpg-0.1.0-SNAPSHOT.jar'))
foreach ($c in 'io/github/butterflysmp/rpg/core/combat/TracedHit.class',
               'io/github/butterflysmp/rpg/core/progression/LevelBonus.class',
               'io/github/butterflysmp/rpg/paper/menu/NexusScreens.class',
               'content/weapons/short_bow.yml',
               'io/github/butterflysmp/rpg/paper/adapter/FireballDrive.class',
               'io/github/butterflysmp/rpg/paper/adapter/DotStatus.class',
               'content/statuses/withering.yml',
               'content/weapons/withered_shortbow.yml',
               'io/github/butterflysmp/rpg/paper/selftest/SelfTestService.class',
               'selftest/index.yml',
               'io/github/butterflysmp/rpg/core/build/Touchdown.class',
               'content/abilities/sunder.yml',
               'content/builds/melee.yml',
               'content/abilities/active_placeholder_melee_thrust.yml',
               'io/github/butterflysmp/rpg/paper/menu/NoSuchClassControl.class') {
  if ($zip.GetEntry($c)) { "PRESENT $c" } else { "ABSENT  $c" }
}
$r = New-Object IO.StreamReader($zip.GetEntry('content/abilities/sunder.yml').Open()); $s = $r.ReadToEnd(); $r.Close()
foreach ($k in '^\s+direction: facing\b', '^\s+on_land:', '^\s+safe_landing:') {
  if ($s -match ('(?m)' + $k)) { "PRESENT $k" } else { "ABSENT  $k" }
}
$zip.Dispose()
```

### R0c — the cumulative Loaded line, and nothing refused

| prediction | instrument | READING |
|---|---|---|
| `Loaded 11 abilities, 29 visuals, 5 statuses, 7 elements, 10 enchants, 3 pools, 6 fragments, 6 aspects, 16 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 1 mobs, 3 recipes` — **identical to GATE-selftest.md's R0c**: abilities −1 (the thrust) +1 (sunder) = 11; `melee.yml` is a CLASS file, not a pool, so pools stay 3 (cells). Counted at this head: `ls content/abilities/*.yml` = 11. No `Refusing`, `Skipping`, `SEVERE` or exception beyond `volley_stone`'s known pair, none naming `sunder`, `melee` or `melee_fire`, and **no `has no cell to offer`** warning | `Select-String run\logs\latest.log -Pattern 'Loaded ','Refusing','Skipping','SEVERE','Exception','offer'` | _(not run)_ |

### R0d — the dev marker reached the JVM, and every scenario loaded

| prediction | instrument | READING |
|---|---|---|
| Exactly one line **`[Rpg] Selftest: dev server, 34 scenarios across 8 gates [melee-cell, level, nexus-polish, legacy-a, legacy-b, wither-status, withered-shortbow, sunder]`** (24 + this gate's 10: SU1, SU2, SU4, SU5, SU6, SU7, SU9, SU10, SU11, SU12), and **no** `Refusing selftest` line | `Select-String run\logs\latest.log -Pattern 'Selftest:','Refusing selftest'` | _(not run)_ |

---

## THE ROWS

**The log instrument for every AUTO row:** `Select-String run\logs\latest.log -Pattern 'SELFTEST sunder','LANDING','PLAYERHIT','PROBE','SELF ','POSITIONS'`,
read after Ben's LOGIN, each row inside its own `SELFTEST sunder <row> START … DONE` bracket.

### SU1 — Fire Melee's Left picker offers Sunder, and the pick holds (SURVIVAL) — witness: the log + a unit row

| prediction | READING |
|---|---|
| The Build screen's first `DUMP` shows a slot named `Left: <ability>`. The Left picker's `DUMP` lists an option named **`Sunder`** and one named **`Melee Active (placeholder)`**, and **no option containing `thrust`**. After the `click "Sunder"`, the Build screen's `DUMP` shows **`Left: Sunder`**. *Not predicted:* the first `Left:` name (Ben's saved build decides it: `Sunder` with lore `Active -- the default` if he never saved one, an empty slot if his save names the thrust). The other half — Fire Ranger's and Fire Mage's pickers do NOT offer Sunder — is `PoolLoaderTest.aClassFileReachesOnlyItsOwnClass`, not this row | _(not run)_ |

### SU2 — on flat ground the slam lands where the leap lands, not at take-off (SURVIVAL) — witness: LANDING + PLAYERHIT

| prediction | READING |
|---|---|
| Two NoAI zombies: **`sa` at `~0 ~ ~-2.5`** (2.5 beside the take-off point, inside a take-off burst) and **`sb` at `~5 ~ ~2`** (beside the landing). One Left. **Exactly one `LANDING … source=sunder how=LANDED slam=true at=<x y z>`**, with `y` equal to the first `SELF`'s y (±0.01), `z` equal to it (±0.5: FACING is straight ahead), and `x` greater by **3.5 to 7.0** (section 7.8: Recall's grounded speed 1.1 travelled 4.64 to first landing; lift 0.5 is not simulated, so the band is wide on purpose). **Exactly one `PLAYERHIT … source=sunder target=<sb's uuid> zombie … element=kinetic sent=20.000 … triggerScore=100`**, and **none naming `sa`**. **`sent=20.000` holds for `crit=false`; on `crit=true`, `sent` is 20 × the crit multiplier, and the line is read with that, not failed.** `sa`'s `PROBE` customHP is unchanged; `sb`'s drops. **The `LANDING` line comes before the `PLAYERHIT`** (the trace runs first). If the landing `x` comes out beyond 8.3 from the start, `sb` falls outside the cube and the row is VOID, not FAIL: record the `x` | _(not run)_ |

### SU3 — the leap ignores WASD: with A held it still goes straight ahead (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| **Hand row.** Fire Melee, stone in hand, on open flat ground facing a landmark. Hold **A** and press Left: the leap goes **straight ahead**, toward the landmark, not left. Then hold **S** and press Left: still straight ahead, not back. With `/rpg mobtrace` on, each `LANDING` line's `at=` is ahead of where you stood along your facing. (`movement_else_forward` would have gone left, then back.) | _(not run)_ |

### SU4 — into a wall: the slam lands at the wall's foot — and reaches THROUGH it (SURVIVAL) — witness: LANDING + PLAYERHIT

| prediction | READING |
|---|---|
| A stone wall at `~3` (x = start + 3, its west face 2.5 from the player's centre), 4 high. A NoAI zombie **`sw` at `~4.5 ~ ~0`, BEHIND the wall**. One Left. **`LANDING … how=LANDED slam=true`** with `x` at most **start + 2.2** (the wall face minus the player's half-width, 0.3) and `y` equal to the start's (±0.01). **AND `sw` IS HIT: one `PLAYERHIT … source=sunder target=<sw's uuid> … sent=20.000` (or 20 × the crit multiplier on `crit=true`).** **This contradicts section 9.2's SU4 ("on this side only"), and it is predicted from the code, not guessed:** a `Burst` is `world.combatantsNear(origin, radius)`, a plain box query with no line-of-sight test (section 8.2's own S3 column says so; finding F2). `sw` is 2.3 to 2.8 blocks from the landing feet in `x`, inside the 3.0 half-extent. **A finding for the seat, raised before the boot (see the PR):** the mechanism Ben ruled (S1) does land at the wall's foot, and its slam is not wall-blocked | _(not run)_ |

### SU5 — off a 4-block ledge: the slam lands on the lower level (SURVIVAL) — witness: LANDING

| prediction | READING |
|---|---|
| The floor ends 1.5 blocks ahead; the lower floor is 4 below. One Left. **`LANDING … how=LANDED slam=true at=<x y z>`** with **`y` = the start's y − 4** (±0.01) and `x` at least start + 1.5. **Exactly one LANDING line**: none at the edge, none in mid-air | _(not run)_ |

### SU6 — into water: a fall-absorbed end IS a landing, and slams at the live feet (SURVIVAL) — witness: LANDING

| prediction | READING |
|---|---|
| One take-off block, then water 3 deep (its surface level with the start's floor, 2 to 12 ahead). One Left. **Exactly one `LANDING … source=sunder slam=true`**, with **`how=ABSORBED` or `how=LANDED`** — a RANGE, stated as one: water zeroes the fall distance mid-air (ABSORBED) unless the leaper's feet reach the bottom first and read on-ground (LANDED). *UNVERIFIED*: which vanilla gives. **`slam=true` is the control; `how` is recorded.** `y` below the start's y. **No `how=BACKSTOP`, no `slam=false`** | _(not run)_ |

### SU7 — under a 2-block ceiling: the slam still happens, in place or on landing (SURVIVAL) — witness: LANDING

| prediction | READING |
|---|---|
| Flat floor, a stone ceiling 2 blocks up (a 0.2-block gap over a 1.8-tall player). One Left. **Exactly one `LANDING … source=sunder slam=true`**, with **`how=NO_TAKE_OFF` or `how=LANDED`** — a RANGE: if the 0.2 gap lets the player leave the ground for a tick, the mark goes AIRBORNE and lands (LANDED); if not, `TAKE_OFF_TICKS` (4) clears it and it slams in place (NO_TAKE_OFF, the seat's ruling 4). **Both slam, so `slam=true` is the control and `how` is recorded.** `y` equal to the start's (±0.01). NO_TAKE_OFF itself is unit-tested (`TouchdownTest.aFailedTakeOffSlamsInPlace`, `SafeLandingsTest.aFailedTakeOffSlamsInPlace`) | _(not run)_ |

### SU8 — dying mid-leap: no posthumous slam (SURVIVAL) — witness: Ben + the log

| prediction | READING |
|---|---|
| **Hand row, with `/rpg mobtrace` ON and a zombie ~5 ahead.** Ben presses Left; the console runs `/kill <Ben>` while he is in the air (about half a second after the press). **`LANDING … source=sunder how=FORGOTTEN slam=false at=-`**, and **no `PLAYERHIT … source=sunder`** after it. On respawn, nothing slams. Then once more with a **quit** mid-leap instead of a death: the same `how=FORGOTTEN slam=false` line | _(not run)_ |

### SU9 — from mid-air: the leap lands farther, and the slam is at the real landing (SURVIVAL) — witness: LANDING

| prediction | READING |
|---|---|
| Flat floor; the player is teleported 1.2 blocks up and presses Left on the same tick (an AIR START: the mark sees not-on-ground on its first step). **Exactly one `LANDING … how=LANDED slam=true`** with `y` equal to the floor the start stood on (the `SELF` taken BEFORE the teleport, ±0.01), and `z` within 0.5. **The landing `x` is RECORDED and compared with SU2's, not predicted** (section 7.8 saw ~2× for Recall mid-jump; this is a teleport, not a jump) | _(not run)_ |

### SU10 — no fall damage on a Sunder landing at any height; then an ordinary drop hurts (SURVIVAL) — witness: SELF

| prediction | READING |
|---|---|
| SU5's ledge again (a ~5-block fall counting the lift: above vanilla's 3-block threshold, *UNVERIFIED* from the jar). `SELF` before the cast; one Left; `SELF` after the landing (40 t later): **customHP identical**, and **one `LANDING … how=LANDED slam=true`** (the seat's ruling 5: the mark spent its immunity AND slammed). Then, 40 t later, `tp @s ~ ~7 ~` and 40 t: the third `SELF` reads **customHP lower than the second** (the mark cleared; an ordinary fall hurts). **The amount is recorded, not predicted.** And **no LANDING line for the ordinary drop** (no mark) | _(not run)_ |

### SU11 — a slam on a GS-300 zombie deals the authored 20, sent at score 100 (SURVIVAL) — witness: PLAYERHIT + PROBE

| prediction | READING |
|---|---|
| `/rpg spawn zombie 300` placed 5 ahead and 2 aside (slowed in place with NoAI). **`MOBSEED <uuid> zombie gs=300 …`** for it. One Left. **`PLAYERHIT … source=sunder target=<that uuid> zombie element=kinetic sent=20.000 crit=<b> triggerScore=100`** (`sent=20.000` on `crit=false`; 20 × the crit multiplier on `crit=true`) — the stone's score (finding F1, ruled by Q6), not the gear's. Its `PROBE` customHP drops; **the drop is recorded** (it is after the zombie's Defense, which this row does not predict). Ben's half (secondary): the popup reads the same number as the drop | _(not run)_ |

### SU12 — knockback pushes a mob away from where the leap came down (SURVIVAL) — witness: POSITIONS

| prediction | READING |
|---|---|
| An AI zombie from `/rpg spawn zombie`, 5 ahead and 2 aside (`+z`), given slowness 255 so it stands. `POSITIONS ZOMBIE` sampled before the cast and every 2 ticks for 30 ticks after it. **After the `LANDING` line, the zombie's `z` relative to the landing feet INCREASES** (pushed away, `+z`), and it takes one `PLAYERHIT … source=sunder`. **How far is RECORDED, not predicted** (MC11 measured nothing). *UNVERIFIED*: that slowness 255 suppresses walking but not knockback velocity; if the zombie never moves, the row is VOID for that reason, not a FAIL of the slam | _(not run)_ |

---

## WHAT THIS GATE CANNOT SEE

- **The feel of the leap and the numbers' fitness.** That is M3, the tuning boot.
- **The BACKSTOP ending** (a leap airborne for 200 ticks): not staged. `TouchdownTest` and `SafeLandingsTest` pin that
  it ends `slam=false`. **Reaching it in play is a finding**, never a reading to pass.
- **A SUPERSEDED ending** (a second Sunder before the first lands) is impossible inside the 240-tick cooldown, and is
  unit-tested only.
- **The FALL-event path vs the step path:** which of the two detonates a given landing is not logged. Both are
  unit-tested to slam exactly once (`SafeLandingsTest`).

## UNIT ROWS AND MUTATIONS (run 2026-11-06 against this branch's working tree, before this file was committed)

`TouchdownTest` (8), `SunderSlamTest` (8), `SafeLandingsTest` (8), `DashAimTest` (+2), `AbilityLoaderTest` (+3),
`PoolLoaderTest` (updated). Each mutation below landed with marker 1 and one changed line, its test reddened, and the
file was restored byte-identical (markers 0):

| mutation | killed by |
|---|---|
| `Touchdown.of`: the backstop check removed | `TouchdownTest.theBackstopClearsAnAirborneMarkWithoutASlam` |
| `CastExecutor.slam`: detonate at the take-off point, not the live feet | `SunderSlamTest.theSlamHitsAtTheLiveLandingAndNotAtTheTakeOffPoint` |
| `CastExecutor.dash`: arm BOTH marks | `SunderSlamTest.aSlamDashWithSafeLandingTrueStillArmsOnlyTheLandingMark` |
| `SafeLandings.consumeFall`: the FALL arm does not slam | `SafeLandingsTest.aFallEventBeforeTheStepSeesTheGroundSlamsOnceAndCancels` |
| `SafeLandings.step`: never mark the slam fired | `SafeLandingsTest.aLowLeapIsSlammedByTheStepAtTheLiveFeetOnceAndTheMarkStaysImmuneForTheLandingTick` |
| `SafeLandings.forget`: slam posthumously (`landed(Vec3.ZERO, FORGOTTEN)`) | `SafeLandingsTest.forgettingMidLeapEndsItWithoutASlamAndTheStepNeverFiresLater` (an assertion; a first form, `feet(null)`, was killed only by an NPE and was replaced) |
| `AbilitySchema.parseOnLand`: the untargeted check disabled | `AbilityLoaderTest.aTargetedEffectAtTheTopOfOnLandIsRefused` |
