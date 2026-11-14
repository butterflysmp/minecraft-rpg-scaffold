# GATE — SUNDER (Melee M2): the facing leap, the slam where it lands, no fall damage (and the stack's one R0)

**Status at `e82df2b3` (the stack-top boot, 2026-11-14; run 3, L2206-L3657, is the run of record proposed to the seat; the boot account is `GATE-deft-hands.md`'s): R0a-d SUPERSEDED (GATE-deft-hands.md R0, all four AS PREDICTED); SU1, SU2, SU4 (as ruled), SU5, SU6, SU7, SU9, SU11, SU12 AS PREDICTED; SU8's death half AS PREDICTED (amended), its quit half NOT READ; SU3 AS PREDICTED by Ben (both directions "yes"); SU10 VOID by its own water instrument (`BLOCKS WATER count=155`), and its ordinary drop took no damage. A finding, recorded per the seat's ruling 3: `selftest/GATE-sunder.yml`'s header comment still says SU8 has no scenario, while its line 125 adds one (left as is).**

**Status at `61e722f2` (the stack-top boot, 2026-11-10; run 3 is the run of record; the boot account is `GATE-deft-hands.md`'s): R0a-d SUPERSEDED (GATE-deft-hands.md R0, PASS); SU1, SU5, SU6, SU7 as predicted; SU2 as predicted, plus one unpredicted hit on a leftover mob; SU4 as predicted, the slam through a wall CONFIRMED (a design question for Ben, not fixed); SU10 as predicted with a staging caveat (run 2's SU10 is VOID); SU9 NOT AS PREDICTED (it landed a block high); SU11 PARTIAL (its hit arrived after its bracket); SU12 NOT AS PREDICTED (no landing and no hit in its bracket); SU3 NOT READ (Ben paused; carried); SU8 NOT STAGEABLE (no console input; carried). NOT a merge candidate: the stack merges bottom-up.**

**The seat, 2026-11-12:**
- **SU8's amendment and the SU9/SU11/SU12 diagnoses are READ and ACCEPTED, pending the next boot.** That is the `death_rules` scenario with the restore, and the three staging-artefact diagnoses. They were committed as `c53c78f7`, which is `ba6b4a10` after the rebase onto `55425aad` (the patch id is unchanged). Acceptance is of the amendment and the diagnoses, NOT a reading: the four rows stay unread until the next boot.
- **SU4 is ruled by Ben: the slam ignores walls, revisitable.** See SU4.

**Status: NOT RUN.** Written 2026-11-06, before any boot of this branch. *Amended 2026-11-06, before any boot: R0 moved on to `GATE-deft-hands.md` (see the R0 section).* **NO BOOT until the seat says so.** This is the
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

**SUPERSEDED, 2026-11-06, before any boot: the stack's one R0 is now `GATE-deft-hands.md`'s** (Deft Hands is stacked on this branch and is the new top). The rows below were never read; at the boot they are recorded SUPERSEDED, never PASS or FAIL.

Every gate beneath carries its R0 SUPERSEDED, never PASS or FAIL. `GATE-selftest.md`'s note names this file. The one
exception is unchanged: `GATE-nexus-polish.md` R0d was #170's merge bar and is merged.

### R0a — the build line names this PR's head

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, this PR's head as `gh pr view <n> --json headRefOid` prints it, shortened. Not `-dirty`, not `unknown`, and not #175's or any head beneath | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | **61e722f2, 2026-11-10: SUPERSEDED** by `GATE-deft-hands.md`'s R0 (PASS). Never PASS or FAIL here. |

**e82df2b3, 2026-11-14: SUPERSEDED** by `GATE-deft-hands.md`'s R0 (AS PREDICTED).

### R0b — the jar carries every slice, Sunder, and NOT the deleted thrust

| prediction | instrument | READING |
|---|---|---|
| PRESENT: GATE-selftest.md R0b's ten slice entries (unchanged); **this slice's** core `build/Touchdown.class`, `content/abilities/sunder.yml`, `content/builds/melee.yml`. **ABSENT: `content/abilities/active_placeholder_melee_thrust.yml`** (deleted here; a jar that still carries it is a stale build) and the control. In `sunder.yml`, as anchored YAML: PRESENT `direction: facing`, `on_land:`; ABSENT a top-level `safe_landing:` | the scan below | **61e722f2, 2026-11-10: SUPERSEDED** by `GATE-deft-hands.md`'s R0 (PASS). Never PASS or FAIL here. |

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

**e82df2b3, 2026-11-14: SUPERSEDED** by `GATE-deft-hands.md`'s R0b (AS PREDICTED: `sunder.yml`, `melee.yml`, `Touchdown.class` PRESENT; the thrust ABSENT).

### R0c — the cumulative Loaded line, and nothing refused

| prediction | instrument | READING |
|---|---|---|
| `Loaded 11 abilities, 29 visuals, 5 statuses, 7 elements, 10 enchants, 3 pools, 6 fragments, 6 aspects, 16 weapons, 1 shields, 24 armor, 5 tools, 6 accessories, 1 mobs, 3 recipes` — **identical to GATE-selftest.md's R0c**: abilities −1 (the thrust) +1 (sunder) = 11; `melee.yml` is a CLASS file, not a pool, so pools stay 3 (cells). Counted at this head: `ls content/abilities/*.yml` = 11. No `Refusing`, `Skipping`, `SEVERE` or exception beyond `volley_stone`'s known pair, none naming `sunder`, `melee` or `melee_fire`, and **no `has no cell to offer`** warning | `Select-String run\logs\latest.log -Pattern 'Loaded ','Refusing','Skipping','SEVERE','Exception','offer'` | **61e722f2, 2026-11-10: SUPERSEDED** by `GATE-deft-hands.md`'s R0 (PASS). Never PASS or FAIL here. |

**e82df2b3, 2026-11-14: SUPERSEDED** by `GATE-deft-hands.md`'s R0c (AS PREDICTED).

### R0d — the dev marker reached the JVM, and every scenario loaded

| prediction | instrument | READING |
|---|---|---|
| Exactly one line **`[Rpg] Selftest: dev server, 34 scenarios across 8 gates [melee-cell, level, nexus-polish, legacy-a, legacy-b, wither-status, withered-shortbow, sunder]`** (24 + this gate's 10: SU1, SU2, SU4, SU5, SU6, SU7, SU9, SU10, SU11, SU12), and **no** `Refusing selftest` line | `Select-String run\logs\latest.log -Pattern 'Selftest:','Refusing selftest'` | **61e722f2, 2026-11-10: SUPERSEDED** by `GATE-deft-hands.md`'s R0 (PASS). Never PASS or FAIL here. |

**e82df2b3, 2026-11-14: SUPERSEDED** by `GATE-deft-hands.md`'s R0d (AS PREDICTED: 38 scenarios).

---

## THE ROWS

**The log instrument for every AUTO row:** `Select-String run\logs\latest.log -Pattern 'SELFTEST sunder','LANDING','PLAYERHIT','PROBE','SELF ','POSITIONS'`,
read after Ben's LOGIN, each row inside its own `SELFTEST sunder <row> START … DONE` bracket.

### SU1 — Fire Melee's Left picker offers Sunder, and the pick holds (SURVIVAL) — witness: the log + a unit row

| prediction | READING |
|---|---|
| The Build screen's first `DUMP` shows a slot named `Left: <ability>`. The Left picker's `DUMP` lists an option named **`Sunder`** and one named **`Melee Active (placeholder)`**, and **no option containing `thrust`**. After the `click "Sunder"`, the Build screen's `DUMP` shows **`Left: Sunder`**. *Not predicted:* the first `Left:` name (Ben's saved build decides it: `Sunder` with lore `Active -- the default` if he never saved one, an empty slot if his save names the thrust). The other half — Fire Ranger's and Fire Mage's pickers do NOT offer Sunder — is `PoolLoaderTest.aClassFileReachesOnlyItsOwnClass`, not this row | **61e722f2, 2026-11-10 (run 3): as predicted.** The Build screen's first dump: L3937 `Left: Sunder` (the first name, not predicted: run 1's and run 2's SU1 had already saved Sunder). The Left picker: L3950 `Melee Active (placeholder)` and L3951 `Sunder`, and no option containing `thrust`; the click; L3955 `Saved: Left = Sunder`; the Build screen after it, L3960 `Left: Sunder`. |

**e82df2b3, 2026-11-14 (run 3): AS PREDICTED.** L3172 `CELL class=melee element=fire`. The first Build `DUMP`: L3178 `Left: Sunder` (Ben's saved build; not predicted). The Left picker (L3189-L3194): **`Melee Active (placeholder)`** (L3191) and **`Sunder`** (L3192, "Current"); no option containing `thrust`. L3195 the click on `Sunder`; L3196 `REPLY "Saved: Left = Sunder"`; the Build `DUMP` after it shows **`Left: Sunder`** (L3201).

### SU2 — on flat ground the slam lands where the leap lands, not at take-off (SURVIVAL) — witness: LANDING + PLAYERHIT

| prediction | READING |
|---|---|
| Two NoAI zombies: **`sa` at `~0 ~ ~-2.5`** (2.5 beside the take-off point, inside a take-off burst) and **`sb` at `~5 ~ ~2`** (beside the landing). One Left. **Exactly one `LANDING … source=sunder how=LANDED slam=true at=<x y z>`**, with `y` equal to the first `SELF`'s y (±0.01), `z` equal to it (±0.5: FACING is straight ahead), and `x` greater by **3.5 to 7.0** (section 7.8: Recall's grounded speed 1.1 travelled 4.64 to first landing; lift 0.5 is not simulated, so the band is wide on purpose). **Exactly one `PLAYERHIT … source=sunder target=<sb's uuid> zombie … element=kinetic sent=20.000 … triggerScore=100`**, and **none naming `sa`**. **`sent=20.000` holds for `crit=false`; on `crit=true`, `sent` is 20 × the crit multiplier, and the line is read with that, not failed.** `sa`'s `PROBE` customHP is unchanged; `sb`'s drops. **The `LANDING` line comes before the `PLAYERHIT`** (the trace runs first). If the landing `x` comes out beyond 8.3 from the start, `sb` falls outside the cube and the row is VOID, not FAIL: record the `x` | **61e722f2, 2026-11-10 (run 3): as predicted for `sa` and `sb`; one unpredicted extra hit.** L4005 SELF at -133.50 63.00 9.50; L4010 `LANDING ... how=LANDED slam=true at=-127.68 63.00 9.50` (x +5.82, inside 3.5-7.0; y and z equal); L4012 `PLAYERHIT ... target=bb5fe995 (sb) zombie element=kinetic sent=20.000 crit=false triggerScore=100`; none naming `sa`; L4015 `sa` 100.000/100.000; L4016 `sb` 80.000/100.000; the LANDING before the PLAYERHIT. **Unpredicted:** L4011, a second PLAYERHIT on `6ee7c57e`, WS3's neighbour zombie (tag `n`), which no row ever removed: a staging leak. |

**e82df2b3, 2026-11-14 (run 3): AS PREDICTED.** L3239 `SELF … at=-117.50 63.00 7.50`. **Exactly one** L3243 `LANDING … source=sunder how=LANDED slam=true at=-111.68 63.00 7.50`: x **+5.82** (band 3.5 to 7.0), y equal, z equal. **Exactly one** L3244 `PLAYERHIT … source=sunder target=19f6ee73-… zombie element=kinetic sent=20.000 crit=false triggerScore=100`, which is `sb`. None names `sa` (`b23cd462`). `sa` stays `100.000/100.000` (L3237 → L3247); `sb` goes 100 → **80** (L3238 → L3248). The LANDING comes before the PLAYERHIT. No hit on an earlier row's mob (the 61e722f2 leak is gone; `GATE-selftest.md` ST8). Ben's half (the leap's look): NOT READ.

### SU3 — the leap ignores WASD: with A held it still goes straight ahead (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| **Hand row.** Fire Melee, stone in hand, on open flat ground facing a landmark. Hold **A** and press Left: the leap goes **straight ahead**, toward the landmark, not left. Then hold **S** and press Left: still straight ahead, not back. With `/rpg mobtrace` on, each `LANDING` line's `at=` is ahead of where you stood along your facing. (`movement_else_forward` would have gone left, then back.) | **61e722f2, 2026-11-10: NOT READ.** Ben paused before it (seat, 2026-11-10); carried. |

**e82df2b3, 2026-11-14: AS PREDICTED, by Ben.** With A held, the leap went straight ahead — *"yes"* (answer 17). With S held — *"yes"* (answer 18). Trace on (the `RELOAD` lines at L3754 and L3756 print only with it). L3757 `LANDING … source=sunder how=LANDED slam=true at=-121.26 63.00 5.10` (09:16:43); L3758 `… at=-107.98 63.00 4.14` (09:16:55). **The log holds no start position or facing for these two casts**, so "`at=` ahead along your facing" cannot be checked from it; the reading is Ben's.

### SU4 — into a wall: the slam lands at the wall's foot — and reaches THROUGH it (SURVIVAL) — witness: LANDING + PLAYERHIT

| prediction | READING |
|---|---|
| A stone wall at `~3` (x = start + 3, its west face 2.5 from the player's centre), 4 high. A NoAI zombie **`sw` at `~4.5 ~ ~0`, BEHIND the wall**. One Left. **`LANDING … how=LANDED slam=true`** with `x` at most **start + 2.2** (the wall face minus the player's half-width, 0.3) and `y` equal to the start's (±0.01). **AND `sw` IS HIT: one `PLAYERHIT … source=sunder target=<sw's uuid> … sent=20.000` (or 20 × the crit multiplier on `crit=true`).** **This contradicts section 9.2's SU4 ("on this side only"), and it is predicted from the code, not guessed:** a `Burst` is `world.combatantsNear(origin, radius)`, a plain box query with no line-of-sight test (section 8.2's own S3 column says so; finding F2). `sw` is 2.3 to 2.8 blocks from the landing feet in `x`, inside the 3.0 half-extent. **A finding for the seat, raised before the boot (see the PR):** the mechanism Ben ruled (S1) does land at the wall's foot, and its slam is not wall-blocked | **61e722f2, 2026-11-10 (run 3): as predicted, and the finding is CONFIRMED.** L4041 SELF at -127.50; L4047 `LANDING ... how=LANDED slam=true at=-125.30 63.00 9.50` (start + 2.20, y equal); L4048 `PLAYERHIT ... target=6b5398ae (sw, behind the wall) ... element=kinetic sent=40.000 crit=true` (20 x 2.0); L4051 `sw` 60.000/100.000. The slam reaches through the wall (the box query, finding F2). Recorded as read: a design question for Ben, not fixed (seat, 2026-11-10). |

**SU4 RULED, 2026-11-12 (Ben, via the seat), verbatim, and REVISITABLE:** *"ignore walls for now but I might change my mind later"*. **The slam ignores walls, as built.** The reading above stands as the ruled behaviour, and no code changes. If Ben reverses it, the fix is an engine-wide line-of-sight option on `Burst` (finding F2), not a Sunder special case. The ruling is recorded in `PLAN-melee-class.md` RULINGS.

**e82df2b3, 2026-11-14 (run 3): AS PREDICTED, as ruled (the slam ignores walls).** L3276 `SELF at=-117.50 63.00 7.50`; L3280 `LANDING … how=LANDED slam=true at=-115.30 63.00 7.50`: x **+2.20**, the bound itself (at most start + 2.2), and y equal. L3281 `PLAYERHIT … target=fa7319e6-… zombie … sent=20.000 crit=false`, which is `sw`, behind the wall. L3284 `sw` `80.000/100.000`.

### SU5 — off a 4-block ledge: the slam lands on the lower level (SURVIVAL) — witness: LANDING

| prediction | READING |
|---|---|
| The floor ends 1.5 blocks ahead; the lower floor is 4 below. One Left. **`LANDING … how=LANDED slam=true at=<x y z>`** with **`y` = the start's y − 4** (±0.01) and `x` at least start + 1.5. **Exactly one LANDING line**: none at the edge, none in mid-air | **61e722f2, 2026-11-10 (run 3): as predicted.** L4071 SELF at -125.50 63.00; L4077 `LANDING ... how=LANDED slam=true at=-118.97 59.00 9.50` (y = 63 - 4; x at least start + 1.5); exactly one LANDING in the bracket. |

**e82df2b3, 2026-11-14 (run 3): AS PREDICTED.** L3306 `SELF … 63.00`; exactly one L3310 `LANDING … how=LANDED slam=true at=-110.95 59.00 7.50`: y = 63 − 4, and x +6.55 (at least +1.5).

### SU6 — into water: a fall-absorbed end IS a landing, and slams at the live feet (SURVIVAL) — witness: LANDING

| prediction | READING |
|---|---|
| One take-off block, then water 3 deep (its surface level with the start's floor, 2 to 12 ahead). One Left. **Exactly one `LANDING … source=sunder slam=true`**, with **`how=ABSORBED` or `how=LANDED`** — a RANGE, stated as one: water zeroes the fall distance mid-air (ABSORBED) unless the leaper's feet reach the bottom first and read on-ground (LANDED). *UNVERIFIED*: which vanilla gives. **`slam=true` is the control; `how` is recorded.** `y` below the start's y. **No `how=BACKSTOP`, no `slam=false`** | **61e722f2, 2026-11-10 (run 3): as predicted; `how=ABSORBED` recorded.** L4100 SELF y 59.00; L4104 `LANDING ... how=ABSORBED slam=true at=-113.68 58.57 8.50` (y below the start's); no BACKSTOP and no `slam=false`. |

**e82df2b3, 2026-11-14 (run 3): AS PREDICTED.** Exactly one L3340 `LANDING … source=sunder how=ABSORBED slam=true at=-111.68 62.57 7.50`. `how` recorded: **ABSORBED**. y 62.57 is below the start's 63.00. No BACKSTOP, no `slam=false`.

### SU7 — under a 2-block ceiling: the slam still happens, in place or on landing (SURVIVAL) — witness: LANDING

| prediction | READING |
|---|---|
| Flat floor, a stone ceiling 2 blocks up (a 0.2-block gap over a 1.8-tall player). One Left. **Exactly one `LANDING … source=sunder slam=true`**, with **`how=NO_TAKE_OFF` or `how=LANDED`** — a RANGE: if the 0.2 gap lets the player leave the ground for a tick, the mark goes AIRBORNE and lands (LANDED); if not, `TAKE_OFF_TICKS` (4) clears it and it slams in place (NO_TAKE_OFF, the seat's ruling 4). **Both slam, so `slam=true` is the control and `how` is recorded.** `y` equal to the start's (±0.01). NO_TAKE_OFF itself is unit-tested (`TouchdownTest.aFailedTakeOffSlamsInPlace`, `SafeLandingsTest.aFailedTakeOffSlamsInPlace`) | **61e722f2, 2026-11-10 (run 3): as predicted; `how=LANDED` recorded.** L4127 SELF y 56.00; L4131 `LANDING ... how=LANDED slam=true at=-110.25 56.00 8.50`. |

**e82df2b3, 2026-11-14 (run 3): AS PREDICTED.** Exactly one L3367 `LANDING … how=LANDED slam=true at=-115.25 63.00 7.50`. `how` recorded: **LANDED**. y equal to the start's.

### SU8 — dying mid-leap: no posthumous slam (SURVIVAL) — witness: Ben + the log

| prediction | READING |
|---|---|
| **Hand row, with `/rpg mobtrace` ON and a zombie ~5 ahead.** Ben presses Left; the console runs `/kill <Ben>` while he is in the air (about half a second after the press). **`LANDING … source=sunder how=FORGOTTEN slam=false at=-`**, and **no `PLAYERHIT … source=sunder`** after it. On respawn, nothing slams. Then once more with a **quit** mid-leap instead of a death: the same `how=FORGOTTEN slam=false` line | **61e722f2, 2026-11-10: NOT STAGEABLE this boot.** The console `/kill` needs console input, and the dev server has none (stdin is null, RCON is off). Carried; the proposal is under WHAT THIS GATE CANNOT SEE. |

**SU8 AMENDED, 2026-11-10, BEFORE THE NEXT BOOT (the seat's ruling): the DEATH half is now AUTO-PARTIAL, by a new
scenario; the QUIT half stays Ben's.**

**The scenario**, `selftest/GATE-sunder.yml#SU8`:
- a zombie 5 ahead; `death_rules: on`; one Left;
- 10 ticks later, `cmd: kill @s`. That is `DamageCause.KILL`, which `VanillaDamagePolicy` passes to vanilla, so it is
  the server's own damage-to-death path, the same as the console `/kill` this row named. *It is issued from Ben's own
  command source, not the console, so it needs his op.*

**What `death_rules` does.** It sets `keep_inventory` and `immediate_respawn` for this row only. The run then waits
for the respawn instead of aborting, polling on the REGION scheduler (`SelfTestRun.awaitRespawn`), because a death
retires the player's own scheduler. Both rules go back at the row's end, and both are in the run's restore set. The
plugin already keeps a dying player's inventory (`onPlayerDeath`'s `setKeepInventory(true)`); the game rule is a
second guard, by the ruling.

**The AUTO half, predicted, in SU8's bracket, in order:**
1. `DEATH_RULES keep_inventory=<b>->true immediate_respawn=<b>->true`;
2. `DRIVE swing`, then `CMD /kill @s`;
3. **`LANDING … source=sunder how=FORGOTTEN slam=false at=-`**;
4. `SETTLED dying=0 …`, then `RESPAWNED ticks=<t>` with `t` < 200;
5. the `SELF` after the respawn reads `customHP=100.000/100.000` (a respawn resets it);
6. the last `PROBE tag=t` reads 100.000/100.000, or `none within 16 blocks` if the respawn point is far. Either is
   accepted, because the probe is not this row's witness;
7. `RULES restored keep_inventory=<b> immediate_respawn=<b>`, with the values DEATH_RULES found.

**No `PLAYERHIT … source=sunder` anywhere in the bracket, and no `RUN ABORTED`.** The run's `RUN RESTORED` line now
ends `keep_inventory=<b> immediate_respawn=<b>`, the RUN START values.

**The quit half** (a quit mid-leap gives the same `how=FORGOTTEN slam=false` line) **stays Ben's hand step**: the
instrument cannot quit its own player.

**e82df2b3, 2026-11-14 (run 3): the DEATH half AS PREDICTED, as amended; the quit half NOT READ.** In order: L3392 `DEATH_RULES keep_inventory=false->true immediate_respawn=false->true`; L3395 `DRIVE swing`; L3397 `CMD /kill @s`; L3398 **`LANDING … source=sunder how=FORGOTTEN slam=false at=-`**; L3401 `SETTLED dying=0 ticks=0`; L3402 `RESPAWNED ticks=1` (under 200); L3405 `SELF customHP=100.000/100.000` (at the respawn point, y 68.00); L3406 `PROBE tag=t … 100.000/100.000 … d=17.000`; L3407 `RULES restored keep_inventory=false immediate_respawn=false`, the values DEATH_RULES found. **No `PLAYERHIT … source=sunder` in the bracket (L3371-L3408), and no `RUN ABORTED`.** Run 3's `RUN RESTORED` (L3657) ends `keep_inventory=false immediate_respawn=false`.

### SU9 — from mid-air: the leap lands farther, and the slam is at the real landing (SURVIVAL) — witness: LANDING

| prediction | READING |
|---|---|
| Flat floor; the player is teleported 1.2 blocks up and presses Left on the same tick (an AIR START: the mark sees not-on-ground on its first step). **Exactly one `LANDING … how=LANDED slam=true`** with `y` equal to the floor the start stood on (the `SELF` taken BEFORE the teleport, ±0.01), and `z` within 0.5. **The landing `x` is RECORDED and compared with SU2's, not predicted** (section 7.8 saw ~2× for Recall mid-jump; this is a teleport, not a jump) | **61e722f2, 2026-11-10 (run 3): NOT AS PREDICTED, on y.** L4150 SELF before the teleport, y 56.00 at x -105.50; L4153 `tp @s ~ ~1.2 ~`; L4158 `LANDING ... how=LANDED slam=true at=-96.54 57.00 8.50`: y 57.00 against the predicted 56.00 (±0.01); z equal. The landing x is recorded: +8.96 from the start (SU2's was +5.82). Why it came down a block high is not read (the row's air fill reported 802 blocks changed, L4147). |

**SU9 DIAGNOSED, 2026-11-10, before the next boot, read-only from the run-3 log and the scenario: a STAGING ARTEFACT,
not a code defect, so the prediction stands.**
- **The staging.** Run 3 staged SU9 underground. The floor had stacked down to y 56, 7 below the surface at y 63, so
  the row's fills cut a pocket: the stone floor changed 25 of 162 cells (the rest were already solid) and the air fill
  802 of 972 (L4145, L4148).
- **The landing.** The player came down ON a block at y 56 (L4158 at y 57.00, and L4159 `onGround=true` at 57.00),
  inside a box the air fill had just cleared.
- **The likeliest cause** is a falling block: sand or gravel from the y 62 ceiling the pocket opened under. The area is
  beach (turtles, frogs and a drowned spawned near it). **No line names the block, so this is UNVERIFIED.**
- **`a8ad33bb` removes the condition:** every unchained row now stages from the run's start, on the surface, so SU9's
  box has open air above it.
- **Instrument added (before the boot):** after the landing, `probe_blocks` WATER, SAND and GRAVEL over the box,
  relative to the landing (`-10 0 -4 7 5 4`). **Predicted: `count=0` for all three.** A non-zero SAND or GRAVEL count
  would witness the falling-block cause.

**e82df2b3, 2026-11-14 (run 3): AS PREDICTED; the 61e722f2 staging artefact did not recur.** L3426 `SELF … at=-117.50 63.00 7.50` (before the teleport); L3429 `/tp @s ~ ~1.2 ~`; exactly one L3434 `LANDING … how=LANDED slam=true at=-108.25 63.00 7.50`: y equal to the floor, z equal. **x recorded: +9.25, against SU2's +5.82** (farther). L3436-L3438 `BLOCKS WATER count=0`, `SAND count=0`, `GRAVEL count=0`.

### SU10 — no fall damage on a Sunder landing at any height; then an ordinary drop hurts (SURVIVAL) — witness: SELF

| prediction | READING |
|---|---|
| SU5's ledge again (a ~5-block fall counting the lift: above vanilla's 3-block threshold, *UNVERIFIED* from the jar). `SELF` before the cast; one Left; `SELF` after the landing (40 t later): **customHP identical**, and **one `LANDING … how=LANDED slam=true`** (the seat's ruling 5: the mark spent its immunity AND slammed). Then, 40 t later, `tp @s ~ ~7 ~` and 40 t: the third `SELF` reads **customHP lower than the second** (the mark cleared; an ordinary fall hurts). **The amount is recorded, not predicted.** And **no LANDING line for the ordinary drop** (no mark) | **61e722f2, 2026-11-10 (run 3): the fields as predicted, with a staging caveat.** L4181 SELF 100.000/100.000 at y 57.00; L4185 `LANDING ... how=LANDED slam=true at=-89.07 54.00 8.50`; L4187 SELF 100.000/100.000, identical; L4189 the ordinary `tp @s ~ ~7 ~`; L4192 SELF 80.200/100.000, lower (the amount recorded); no LANDING for the ordinary drop. **The caveat:** the ledge was 3 blocks (57 to 54), not the ~5 the row assumes, and no line shows whether that height would hurt without Sunder. **Run 2's SU10 is VOID** (L2815-L2827): the leap ended in water (L2820 `onGround=false fall=0.000` at y 57.70, 40 t after the cast), and the only LANDING, L2825, came after the ordinary drop. |

**SU10 INSTRUMENT ADDED, 2026-11-10, before the next boot.** The row digs 5 below wherever it stages, which is below
sea level on a surface at y 63. Run 2's SU10 flooded. So after the ordinary drop there is a WATER `probe_blocks` over
the pit, relative to the player's final position (`-9 -2 -4 7 7 4`). **Predicted `count=0`.** A non-zero count makes
the row VOID (the leap ended in water), not FAIL. **Stage the run on dry ground, away from water.**

**e82df2b3, 2026-11-14 (run 3): VOID, by the row's own water instrument; and its second half reads NOT AS PREDICTED.** The Sunder half, as predicted: L3460 `SELF customHP=100.000/100.000 at=… 63.00`; one L3465 `LANDING … how=LANDED slam=true at=-110.95 59.00 7.50`; L3467 `SELF customHP=100.000/100.000`, identical. The ordinary drop: L3469 `/tp @s ~ ~7 ~`; L3473 the third `SELF` reads **`customHP=100.000/100.000 at=-112.11 59.00 8.70 onGround=true`, not lower**. L3474 **`BLOCKS WATER count=155 box=-9 -2 -4 7 7 4`**: non-zero, so the row is VOID by the 2026-11-10 instrument, not FAIL. The likeliest reading is that the pit, dug 5 below a surface at y 63 at a spot near water, held water when the drop ended, and a fall into water does no damage. **UNVERIFIED:** no line says where the 155 blocks are or when they arrived. No LANDING for the ordinary drop, as predicted. **This is a bar, and it is not met.**

### SU11 — a slam on a GS-300 zombie deals the authored 20, sent at score 100 (SURVIVAL) — witness: PLAYERHIT + PROBE

| prediction | READING |
|---|---|
| `/rpg spawn zombie 300` placed 5 ahead and 2 aside (slowed in place with NoAI). **`MOBSEED <uuid> zombie gs=300 …`** for it. One Left. **`PLAYERHIT … source=sunder target=<that uuid> zombie element=kinetic sent=20.000 crit=<b> triggerScore=100`** (`sent=20.000` on `crit=false`; 20 × the crit multiplier on `crit=true`) — the stone's score (finding F1, ruled by Q6), not the gear's. Its `PROBE` customHP drops; **the drop is recorded** (it is after the zombie's Defense, which this row does not predict). Ben's half (secondary): the popup reads the same number as the drop | **61e722f2, 2026-11-10 (run 3): PARTIAL.** L4212 `MOBSEED e4d8fa45 zombie gs=300 source=stored max=300`; L4229 PROBE 300.000/300.000 at d=5.385; L4232 the swing; L4234 the 60 t PROBE, 300.000/300.000, with the player still 1.2 above it (d=2.020 dy=-1.198); L4235 DONE. The predicted line arrived after the bracket: L4238 `LANDING ... how=LANDED slam=true at=-83.78 54.00 8.50` and L4239 `PLAYERHIT ... target=e4d8fa45 ... element=kinetic sent=20.000 crit=false triggerScore=100`, as predicted, at the stone's score. Its PROBE drop was never read: the next row's `/kill` removed it. Why the leap was still in the air at 60 t is not read; the server lagged about 3 s at 09:50:04-09:50:07 (L4233, `Mana refilled` replied 3 s after its command). |

**SU11 DIAGNOSED, 2026-11-10, before the next boot: a STAGING ARTEFACT (water), not a code defect, so the prediction
stands.**
- **The staging.** SU11 was staged at y 54: 9 below the surface, below sea level (63), and beside the ocean. The air
  fill changed 395 of 630 cells (L4206).
- **The reading.** The 60 t PROBE found the player still 1.2 above the floor (L4234: `d=2.020 dy=-1.198`). On dry
  ground a leap lands in about 20 ticks (SU2: swing L4008, LANDING L4010, within one second). **A player held up for
  60 ticks is floating.** The same stretch in run 2 read `onGround=false fall=0.000` in water (SU10, L2820).
- **Why the mark landed late.** It ended when his feet reached the floor (L4238 `how=LANDED`), after the bracket. That
  is the designed behaviour in water (SU6's range: ABSORBED or LANDED): water keeps zeroing the fall distance, so the
  mark ends only on the ground or at the backstop.
- **Instrument added:** a WATER `probe_blocks` at the row's end, relative to the landing (`-6 0 -4 7 4 4`).
  **Predicted `count=0`.**

**e82df2b3, 2026-11-14 (run 3): AS PREDICTED.** L3497 `MOBSEED 9cb5bcdd-… zombie gs=300 … max=300`; L3518 `LANDING … how=LANDED slam=true`; L3519 `PLAYERHIT … target=9cb5bcdd-… zombie element=kinetic sent=20.000 crit=false triggerScore=100`. PROBE 300 → **280** (L3514 → L3521): **the drop is recorded as 20**. L3522 `BLOCKS WATER count=0`. Ben's half (the popup): NOT READ.

### SU12 — knockback pushes a mob away from where the leap came down (SURVIVAL) — witness: POSITIONS

| prediction | READING |
|---|---|
| An AI zombie from `/rpg spawn zombie`, 5 ahead and 2 aside (`+z`), given slowness 255 so it stands. `POSITIONS ZOMBIE` sampled before the cast and every 2 ticks for 30 ticks after it. **After the `LANDING` line, the zombie's `z` relative to the landing feet INCREASES** (pushed away, `+z`), and it takes one `PLAYERHIT … source=sunder`. **How far is RECORDED, not predicted** (MC11 measured nothing). *UNVERIFIED*: that slowness 255 suppresses walking but not knockback velocity; if the zombie never moves, the row is VOID for that reason, not a FAIL of the slam | **61e722f2, 2026-11-10 (run 3): NOT AS PREDICTED.** L4257 `MOBSEED f14797c0 zombie gs=100 ... max=212`; L4277 the swing. `POSITIONS ZOMBIE` first shows TWO zombies, the `s12` zombie at +5 and SU11's zombie at 0 (killed by this row's `/kill`, L4240: a dying body), both shifting together as the player moved; after the leap, one zombie with its relative z constant at 2.00, so no +z push. No LANDING and no PLAYERHIT inside the bracket after the cast, and none later in the log (28 LANDING lines in all, none after L4298); L4297 `s12` 211.538/211.538, not hit. The leap's end is not in the bracket. Cause not read; carried. |

**SU12 DIAGNOSED, 2026-11-10, before the next boot: the same water artefact as SU11, so the prediction stands.**
- **The staging.** SU12 was staged at y 54, like SU11. Its air fill changed 614 of 630 cells (L4251), so the pocket
  was cut almost wholly from solid ground, below sea level. A drowned came into tracking range in the tick it staged
  (L4285).
- **The reading.** After the cast the player rose above the zombie and hung there. The relative dy went -0.91, -1.48,
  -1.81, -2.00, -2.11, -2.16, then -2.17 drifting to -1.94 over about 15 ticks (L4280-L4296). That is buoyancy, not a
  fall.
- **Why no slam.** There is no LANDING because his feet never reached the ground inside the bracket. With no slam
  there is no PLAYERHIT and no push.
- **Instrument added:** a WATER `probe_blocks` at the row's end (`-6 0 -4 7 4 4`). **Predicted `count=0`.**

**A RUN-LOOP FINDING FROM THESE RUNS, recorded and not fixed (not asked).** A death in a row WITHOUT `death_rules`
does not abort the run; it stalls it silently. Run 2 shows it: Ben drowned at L2831, 11 seconds into SU11's 240-tick
wait, and the row logged nothing more until his `/rpg selftest stop` 78 seconds later (L2900). The wait is a task on
the player's own scheduler, and `PaperScheduler.onEntityLater` passes no retired callback, so a task a death retires
simply never runs. `invalidated()`'s `"the player died"` check is therefore never reached. The likely fix is an abort
from the death event; it is owed to the seat.

**e82df2b3, 2026-11-14 (run 3): AS PREDICTED.** L3545 `MOBSEED c74ded8c-… zombie`. `POSITIONS ZOMBIE`, relative to the player (L3562-L3583): z is 2.00 through the leap; L3575 `LANDING … how=LANDED slam=true`; L3576 one `PLAYERHIT … target=c74ded8c-… sent=20.000 crit=false`; then z goes **2.56, 3.02, 3.16, 3.21, 3.22, 3.22, 3.22**: it increases, pushed away. **The push is recorded: +1.22 in z** (x −0.82 → −1.69). L3584 `80.000/100.000`; L3585 `BLOCKS WATER count=0`. The zombie moved, so slowness 255 did not suppress the knockback.

---

## WHAT THIS GATE CANNOT SEE

- **The feel of the leap and the numbers' fitness.** That is M3, the tuning boot.
- **The BACKSTOP ending** (a leap airborne for 200 ticks): not staged. `TouchdownTest` and `SafeLandingsTest` pin that
  it ends `slam=false`. **Reaching it in play is a finding**, never a reading to pass.
- **A SUPERSEDED ending** (a second Sunder before the first lands) is impossible inside the 240-tick cooldown, and is
  unit-tested only.
- **The FALL-event path vs the step path:** which of the two detonates a given landing is not logged. Both are
  unit-tested to slam exactly once (`SafeLandingsTest`).
- **SU8, the death mid-leap: NOT STAGEABLE at the `61e722f2` boot (2026-11-10), carried.** Its prediction needs the
  console to `/kill` Ben about half a second after he presses Left, and the dev server takes no console input (stdin is
  null, `enable-rcon=false`). **Proposed, not built (the seat, 2026-11-10, asked for a proposal only).** There are
  two routes:
  - **(a) Dev-only RCON**, enabled by `dev-server.sh`. It gives a console, but the timing is a person's, so the half
    second is not reproducible. It also opens a network port on the dev machine.
  - **(b) A selftest step that kills the player mid-leap through the real damage path**, a fixed number of ticks after
    the swing: deterministic, and inside the instrument's brackets. It meets the two reasons this row was BEN only if
    the step also sets `keep_inventory` and `immediate_respawn` for the row and the run's restore puts both back. That
    removes the inventory duplication, and the respawn screen that halts a row.

  **Recommended: (b).** It is reproducible, and it adds no port. **Neither covers SU8's quit half:** the instrument
  cannot quit its own player (a player who leaves aborts the run), so the quit stays Ben's.

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
