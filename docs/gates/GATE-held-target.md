# GATE — A SUMMON HOLDS ITS TARGET (G1 TO G8), WITH THE OWED ROWS R2 TO R6 RE-READ BY REFERENCE (2026-11-26)

**Status: NOT RUN.** The plans are `docs/plans/PLAN-summon-holds-target.md` (G1 to G8) and `docs/plans/PLAN-owed-fixes.md`
(R2 to R6, with R1 to R7 as its dated note). The scenario is `paper/src/main/resources/selftest/GATE-held-target.yml`. The
stack, bottom up: master `6c9b1ff4`, #330 `feat/owed-fixes`, #331 `feat/summon-holds-target`, then this ONE gate branch,
`feat/held-target-gate`, last and separate so it can move up. **Every prediction below was written before any boot, and
is frozen from the seat's approval.** No boot without Ben's own "ready" and the seat's approval of the code AND this gate
from origin. **The gates it re-reads by reference are frozen and not edited;** where a referenced row's prediction moves
because R2 to R6 moved what it reads, the NEW prediction is written HERE, beside the reason, and the frozen one stays.

## WHAT BEN TYPES — ONE COMMAND

```
/rpg selftest bone-club.DB6+blast-fungus.BF3.BF6.BF8+healing-tome.HT1+blink-tome.BT1.BT3.BT4.BT5.BT6+held-target
```

- Each referenced gate runs its own SETUP, then only the rows named (`SelfTestGateList`, the TT20 to TT27 form). The
  queue, step for step, is `HeldTargetGateCommandTest`: 20 scenarios, this gate's setup and S1 to S5 last.
- **He answers NOTHING.** No row in the whole command asks (asserted over the queue).
- **This gate's R0 supersedes every referenced gate's R0** (theirs are the record).
- He types it on the pad's server, standing anywhere; the first setup takes him to the pad.

**How long it runs (an ESTIMATE from the scenarios' waits, not a measurement):** the queued rows' `wait:` steps sum to
**2013 ticks (101 s)**; the boot of `1f447197` ran the Blink Tome gate's 1395 ticks of waits in 73 s. **About two
minutes, with no answer to give.**

## SAFETY — 0.000 FOR THE WHOLE COMMAND

- **No row wounds him.** No `/rpg damage`, no `damage @s`, in any queued row (HT1 is the Healing Tome's tooltip row; its
  wounds are HT2 to HT6, not queued).
- **Every mob is a NoAI zombie** (`NoAI:1b` in every `summon` of this gate, asserted by
  `HeldTargetGateCommandTest.everySummonedMobIsNoAi`; the referenced rows' as their gates assert): it never acts.
- **His wolf cannot hurt him.** READ, `MobHostility`'s table, the SUMMON row's PLAYER cell is `no`; `CombatSides.refused`
  reads that table, and the summon's hit goes through `applyDamageAs(MobHostility.Side.SUMMON, …)`.
- **The blasts (BF3, BF6) spare him** (`PvpPath.FUSED_BURST`, SPARED; EXECUTED in
  `BlastFungusGateArithmeticTest.straightDownTheyRestAtHisFeetAndHeTakesNothing`).
- **S-1:** no term prices damage to him, so there is none to execute; **S-2, S-3:** no dealer acts on him at any score.
- **The sum: DB6 0.000 + BF3 0.000 + BF6 0.000 + BF8 0.000 + HT1 0.000 + BT1, BT3 to BT6 0.000 + S1 to S5 0.000 =
  0.000**, at or under 20.000.

## CARRIED STATE — THE BOOTER'S CHECKLIST, IN THIS ORDER (`.claude/rules/booting.md`, each step ticked in the report)

1. The seat's approval of the code AND this gate from origin; Ben's own "ready".
2. **The heads:** `git ls-remote --heads origin` for master, `feat/owed-fixes`, `feat/summon-holds-target` and
   `feat/held-target-gate`, equal to what the seat diffed; CI green on each. `git status --porcelain` prints nothing and
   `git rev-parse HEAD` equals `feat/held-target-gate`'s head.
3. **No JVM:** `Get-CimInstance Win32_Process -Filter "Name='java.exe'"` counts 0.
4. **The backup**, to a NEW folder `C:\Users\Neb91\IdeaProjects\rpg-run-backups\<date>-pre-held-target`: `run\world`,
   `run\plugins\Rpg` with their file counts and copy counts, his player `.dat` with its bytes, **his accessory file's bytes
   and his profile's `archetypeId` quoted before the boot** (he equipped a Healing Tome and became a mage after the last
   run: whatever they are now is what CAPTURED must read and RESTORED put back).
5. **The world's saved difficulty,** read from the decompressed `level.dat`, before and after the run.
6. **Config:** `dev-pack-on-join: true`. No `run\rpgfight`.
7. **"BOOTING NOW", in its own message, BEFORE the boot command**, with: "Ben: do NOT log in until I tell you R0 passed."
8. `./scripts/dev-server.sh --refresh-content`; the `Done` matched to a log newer than the deployed jar.
9. R0 read on this boot's log; **"R0 PASSED" in its own message, before he logs in.** His first `joined the game` time
   reported beside R0's.
10. After his `/stop`: the clean-stop lines, no `java` process left; his accessory file's bytes and `archetypeId` read again
    and compared with step 4's.

**H15 for this run:** CAPTURED reads his class and four slots as step 4 found them; RESTORED puts back the same
(`accessories=[…]` equal to CAPTURED's, no `NOT-RESTORED=`, `cell=<his class>/<his element>`), read by named fields (the
seat's ruling of 2026-11-26), ending `… summons=0 fused=0 blink=none drag_tick=false`.

## R0 — THE BUILD. READ IT FIRST. IF ANY R0 FAILS, STOP

- **R0a:** `[Rpg] Build: <sha>`, `feat/held-target-gate`'s head as `git ls-remote --heads origin feat/held-target-gate`
  prints it, shortened. Not `-dirty`, not `unknown`.
- **R0b:** FIVE scripts on the DEPLOYED jar.
  - `docs/gates/GATE-held-target-r0b.ps1`: before `'--- absent'` **20 `PRESENT`, 0 `ABSENT`** (4 `Entry`, 13 `Has`, 3
    `ValueLine`); between `'--- absent'` and `'--- controls'` **3 `ABSENT`, 0 `PRESENT`** (no `chase_limit: 32`, no
    `LIMIT`, no `chase_limit` key in the loader); after `'--- controls'` **2 `PRESENT`, 0 `ABSENT`**.
  - `GATE-blink-tome-r0b.ps1` **27 / 0, 4 / 0, 2 / 0**; `GATE-healing-tome-r0b.ps1` **25 / 0, 4 / 0, 2 / 0**;
    `GATE-bone-club-r0b.ps1` **21 / 0, 5 / 0, 2 / 0**; `GATE-blast-fungus-r0b.ps1` **18 / 0, 4 / 0, 2 / 0**, unchanged.
  - **Run before commit on a jar built from this tree (`paper/target/rpg-0.1.0-SNAPSHOT.jar`, 2026-11-27 00:00): all five
    as predicted.**
- **R0c:** `[Rpg] Loaded 1 summon(s)`; `Loaded … 27 enchants, … 27 weapons, 1 shields, 28 armor, 5 tools, 8 accessories,
  …` and `Mint-on-craft: 42 indexed, 42 claiming, of 61 gear definitions`, unchanged (no content file added or removed;
  `bone_whistle_wolf.yml` loads under its new key). **0** `WARN]: [Rpg]` (control: `INFO]: [Rpg] Loaded` reads 1 or
  more); none of `Refusing`, `Skipping`, `SEVERE`, `Exception`.
- **R0d:** exactly one **`[Rpg] Selftest: dev server, 535 scenarios across 56 gates [melee-cell, …, healing-tome,
  blink-tome, held-target]`**. Recount: 56 files, 591 keys (`cat GATE-*.yml | grep -cE '^[A-Za-z0-9_-]+:'`), 56 setups,
  so 535. Control: 585 + 6 (its setup and S1 to S5) = 591. Controls: `530 scenarios` ABSENT, `55 gates` ABSENT.
- **R0e:** one `FIGHTWORLD sweep path=…\rpgfight folders=0`, no `deleting`, no `orphan`; no `FUSED removed … cause=orphan`.

## THE SETUPS' NEW LINE (R4)

After each referenced gate's setup CLEAN line, and this gate's: **`ITEMS cleared=<n> radius=48 [<type> x<k>, …]`**.

- **The first (bone-club's): a READING, not a prediction:** whatever lies within 48 of the stand, named by type.
- **blast-fungus's, after DB6:** DB6 ends with `kill @e[tag=t]`, and a zombie's death can drop items: `ITEMS cleared=<k>`
  with each type named (INFERRED `rotten_flesh`; k ≥ 0; a reading). **This is R4's identification of a stray:** the kind
  of item the Bone Club rows' kills leave, named by the line, where the boot of `1f447197` could only count them.
- **healing-tome's, blink-tome's and this gate's:** `ITEMS cleared=0 radius=48 []` (no row before them kills a mob: BF3,
  BF6 and BF8 remove their zombies through the CLEAN, which drops nothing; HT1 and the BT rows the same).

## THE REFERENCED ROWS, WITH WHAT R2 TO R6 MOVED

### bone-club.DB6 — AS `GATE-bone-club.md` DB6, AND R3

Every line DB6 predicts (charges `1.000`, `1.000`, **`0.620`**, `1.000`; STREAK `n=0 … 0.150 … counted`, `n=1 … 0.200 …
counted`, **`n=2 step=5 chance=0.150 charged=false result=reset:uncharged`** with `sent=22.331 crit=false` or `sent=44.662
crit=true`, `n=0 … 0.150 … counted`), with two NEW predictions: **every `DRIVE reset_charge` line reads
`cooldown=0.0200->0.0200`** (R3: `Player#attack` reset the charge already; the verb is a documented no-op), and **its
row-start CLEAN reads `drum=0`** (bone-club's setup lands no hit).

### blast-fungus.BF3 — AS `GATE-blast-fungus.md` BF3, AND R4

Every FUSED, MANA, USE, PLAYERHIT and PROBE line as BF3 predicts (`ba` 835.000, `bo` 945.000, `bn` 1000.000, or the
crits). **NEW, the three item samples: each `POSITIONS ITEM n=3 at=[…]`, the three entries `6.65 0.00 0.00
nether_wart_block fused marker`, `6.03 0.00 2.81 nether_wart_block fused marker`, `6.03 0.00 -2.81 nether_wart_block
fused marker` in any order, the same across the samples** (READ: a fused chunk's body is an item marker, `spawnMarker`,
tagged `fused` by `claimFusedBody`); **the fourth sample `POSITIONS ITEM n=0 at=[] radius=12`**.

### blast-fungus.BF6 — AS `GATE-blast-fungus.md` BF6, AND R6

The wolf's `SUMMONPROBE … health=100.000/100.000` before and after, no PLAYERHIT naming it, `bs` 835.000 (or 670.000).
**NEW, the points against the throw's own `at=`:** with the throw at `x, 202.620, z`, the three rests at T + 2 at `x,
201.000, z`, `x, 201.000, z + 0.721` and `x, 201.000, z − 0.721`, and three `detonate … cause=fuse` there at T + 40, each
to 0.002. (He is aligned two ticks before the press, so x and z read the block centre; the prediction does not rest on it.)

### blast-fungus.BF8 — AS `GATE-blast-fungus.md` BF8, AND R4

**NEW: `ENTITIES ITEM count=0 radius=32 dying=0 types=[]`** (the setup cleared any stray; BF3's and BF6's bodies went with
their chunks; their zombies went through the CLEAN). `STAGE clear_state … fused=0`. With the type printed, a non-zero
count would now NAME what it is, so this row can tell "no body left" from "no item at all".

### healing-tome.HT1 AND blink-tome.BT1 — THE READ NAME SHAPE (R5)

As HT1 and BT1, every line, with the name predicted as READ: **`HELD healing_tome BOOK x1 name="Healing Tome" [white,
blue] italic lore=[…]`** and **`HELD blink_tome BOOK x1 name="Blink Tome" [white, blue] italic lore=[…]`**, the lore
whole as each gate quotes it, and A4's three REPLYs. (READ, both jars: the `[white, …] italic` is the vanilla wrapper's;
the stored name says `italic: false` and the client draws it upright. `PLAN-owed-fixes.md`, R5.)

### blink-tome.BT3, BT4, BT5, BT6 — AS `GATE-blink-tome.md`, AND R2, WITH THEIR NEXT ROW-START CLEAN LINES

Each row's own lines as its gate predicts (the blink `dist=3.000 … safe=508 forgot=1`, B ∈ [M, M + 5]; BT3's
REFUSED-TARGET, INVIS 160 − (R − B), END expired at exactly B + 160; BT4's `reason=cast` between DRIVE use and USE result;
BT5's `reason=melee` after DRIVE attack; BT6's whistle with no END, then END expired at B + 160). **NEW, R2's tail, in
each:** `accessory: clear` → **`BLINK STOP wearer=<him> tick=<t> reason=<inactive or ended>`, t within 5 ticks of the
clear**; then the row's last `clear_state` → **`STAGE clear_state … blink=none`** in all four (each state has ended
before it: BT3's and BT6's by expiry, BT4's by the cast, BT5's by the melee hit); **and NO second `BLINK wearer=` line in
any of the four rows** (BT3's item 7 now holds). **Their next row-start CLEAN lines:** BT4's, BT5's and BT6's, and this gate's setup's (after BT6), each **`…
drum=0 blink=none`**.

## THE ROWS OF THIS GATE

Each row's lines are those between `SELFTEST held-target <row> START` and its `DONE`. **Every row is AUTO, SURVIVAL, on the
pad, and stages itself:** level 2, empty inventory and state, empty slots, a mage, 40 ticks, `rpg maxsummons reset`
(READ: `MaxSummons.DEFAULT` is 1), the Bone Whistle given and held, score 100, aligned facing east, mana refilled, its
right click: **`SUMMON spawn … at=<his feet> summon=bone_whistle_wolf score=100 health=100.000 damage=20.000`**.

**THE TRACE LINE (G7):** `SUMMON target <owner, weapon, body, at> target=<uuid|none> why=<the tier held> [dropped=<…>]
reason=<…>`, written ONCE per change. **A wolf's hit:** `SUMMON hit … victim=<z> authored=20.000 score=100 sent=20.000
crit=none class=0 level=0 element=nature tick=<t>`, at least 20 ticks apart (the cadence), each lowering the zombie's
custom health by exactly **20.000** (sent = authored 20 × score 100/100; no crit, class or level term; defence 0: READ on
a boot, `GATE-summoner.md` SM3D's `100.000` → `80.000`). **A zombie's health:** `max_health` 200 → `customHP=1000.000/
1000.000` (EXECUTED, `MobScaling.maxHealth(200, false, true, 100)`, `GATE-bone-club.md` BC4), then **1000 − 20 × n**, n
the `SUMMON hit … victim=<it>` lines in the row.

### S1 — KEPT PAST 32

A zombie `h1` at the stand + (40, 0); he steps to (30, 0) and casts there (the wolf at his feet). `PROBE h1 …
customHP=1000.000/1000.000 … d=10.000`.
1. `DRIVE mob_target <h1> tag=h1 tick=M`; within a tick **`SUMMON target … target=<h1> why=targeting
   reason=acquired:targeting`**; the wolf walks the 10 and its `SUMMON hit … victim=<h1> … sent=20.000` lines begin
   (INFERRED: inside the 40 ticks).
2. `SUMMONPROBE … target=<h1> tier=targeting …`.
3. **He steps back 30 west** (`tp @s ~-30 ~ ~`; `SELF … at=<stand x+0.50> 201.00 <stand z+0.50>`): `h1` is now 40 from
   him and the wolf beside it. **Within 3 ticks, exactly once: `SUMMON target … target=<h1> why=targeting reason=kept`**
   (its targeting reason lapsed: 40 > 30). **No `SUMMON target` line with `dropped=` in the row** — D4 would have written
   **`SUMMON target … target=none why=none dropped=limit`** here (READ on a boot: `GATE-summoner.md` SM5) — **and no
   `SUMMON teleport` line** (the wolf is about 39 from him, under 48).
4. The three `SUMMONPROBE`s after: **`target=<h1> tier=targeting`**, `dist_him` between 37 and 41 (the wolf within 2 of
   `h1`'s feet, 40 from his).
5. `PROBE h1 … customHP=<1000 − 20 n>/1000.000`, n its `SUMMON hit` lines.

### S2 — KEPT WHEN THE REASON LAPSES

A zombie `h2` 4 east. `DRIVE mob_target <h2> tag=h2 tick=M`; within a tick **`SUMMON target … target=<h2> why=targeting
reason=acquired:targeting`**; `SUMMONPROBE … target=<h2> tier=targeting`. Then **`DRIVE mob_target <h2> tag=h2 clear
tick=C`**; within 3 ticks, exactly once, **`SUMMON target … target=<h2> why=targeting reason=kept`**; **`TARGET tag=h2
<uuid> type=zombie target=none dead=false`**; both later `SUMMONPROBE`s **`target=<h2> tier=targeting`**. D4 would have
written **`SUMMON target … target=none why=none`** at C (no `dropped=`: alive and inside 32). `PROBE h2` =
1000 − 20 n.

### S3 — THE BLINK DOES NOT CALL IT OFF

BT2's stand (four NoAI blockers, `bt` 3 east of where he stands, him 20 east; `HeldTargetGateCommandTest` asserts the
same summons and move as BT2, so `BlinkPadLandingTest`'s 508 landings are S3's; READ, `BlinkTomes.blocksALanding`: his
wolf, side SUMMON, blocks none). The wolf at his feet; `DRIVE mob_target <bt> tag=bt tick=M`; **`SUMMON target …
target=<bt> why=targeting reason=acquired:targeting`**; `SUMMONPROBE … target=<bt> tier=targeting`.
1. The tome given, held, `equip 0` at E: **`BLINK START … tick=T0`, T0 ∈ [E, E + 5]**; **`BLINK wearer=<him> tick=B
   trigger=zombie <bt's uuid> dist=3.000 from=<stand x+20.500> 201.000 <stand z+0.500> to=<x> 201.000 <z> safe=508
   forgot=1`, B = T0 + 5** (the condition is already true; BT10 read exactly that), to= as `GATE-blink-tome.md` bounds
   it (x − sx in [4.5, 36.5], z − sz in [−12.5, 13.5], a block centre, within 16, more than 5 from every mob).
2. **Within 2 ticks of B, exactly once: `SUMMON target … target=<bt> why=targeting reason=kept`** (`bt` forgot him).
3. `SELF … at=<to's x> 201.00 <to's z>`; **`SUMMONPROBE … target=<bt> tier=targeting`** twice, its `at=` within 2 of
   `bt` (the wolf stays at its target), `dist_him` = the wolf's distance to his landing; **no `SUMMON teleport` line in
   the row**; `INVIS present=true … blinked=true`.
4. If the wolf hits `bt` after B, its hit asks the seam to turn `bt` on him and is refused: **`BLINK REFUSED-TARGET
   mob=zombie <bt's uuid> tick=<t>`, once** (INFERRED: a hit lands in the 36 ticks after B).
5. R2's tail: `accessory: clear` → `BLINK STOP … reason=<inactive or ended>`; then **`BLINK END … reason=cleared`** and
   **`STAGE clear_state … summons=1 … blink=ended`**; **exactly one `BLINK wearer=` line in the row.** S4's CLEAN `blink=none`.

### S4 — A DIRECTED ORDER SWITCHES

Zombies `a4` 4 east and `b4` at (6, 3). `DRIVE mob_target <a4> tag=a4 tick=M`; **`SUMMON target … target=<a4>
why=targeting reason=acquired:targeting`**; `SUMMONPROBE … target=<a4> tier=targeting`. Then `aim: b4` and his left click
(`DRIVE reload held=bone_whistle`, as SM3C): within 3 ticks **`SUMMON target … target=<b4> why=directed
reason=switched:targeting->directed`**, and **`SUMMONPROBE … target=<b4> tier=directed … directed=<b4>`**.

### S5 — IDLE, IT COMES BACK

A zombie `h5` 14 east. `DRIVE mob_target <h5> tag=h5 tick=M`; **`SUMMON target … target=<h5> why=targeting
reason=acquired:targeting`**; its `SUMMON hit … victim=<h5>` lines; `SUMMONPROBE … target=<h5> tier=targeting`, `dist_him`
between 12 and 15. The row kills `h5`: within 2 ticks **`SUMMON target … target=none why=none dropped=dead
reason=dropped:dead`** (READ: a killed body stays in the world for its 20 death ticks, `LivingEntity.tickDeath`, and
`LiveTargets.isLive` is false for it, so the guard reads it DEAD, not gone); `SUMMONPROBE … target=none tier=none`. 100
ticks later **`SUMMONPROBE … target=none tier=none` with `dist_him` LESS than the first one's** (it walks back toward him,
as it always did: `needsReturn` beyond 8), and **no `SUMMON teleport` line** (it never passes 48). The control that G4
left the idle follow alone.

## THE COORDINATE TABLE (pad coordinates from the stand column: x east, z south; the preflight `0 -13 44 13`)

| what | where | inside 0..44 × −13..13 |
|---|---|---|
| the stand (every setup and row-start CLEAN) | (0, 0) | yes |
| S1: `h1`; him; his wolf | (40, 0); (30, 0) then (0, 0); (30, 0) to within 2 of `h1` | yes |
| S2: `h2`; the wolf | (4, 0); (0, 0) to within 2 of `h2` | yes |
| S3: blockers; `bt`; him; the wolf | (17, ±13), (23, ±13); (23, 0); (20, 0); (20, 0) to within 2 of `bt` | yes |
| S3: every landing a blink can choose | x 4..36, z −13..13, y 1: 508 spots, ENUMERATED (`BlinkPadLandingTest`, BT2's layout) | yes |
| S3: after the blink | no second blink: the cooldown, then the tome off and settled (R2) before the clear | — |
| S4: `a4`; `b4`; the wolf | (4, 0); (6, 3); (0, 0) to within 2 of `a4`, then of `b4` | yes |
| S5: `h5`; the wolf | (14, 0); (0, 0) to within 2 of `h5`, then back toward (0, 0) | yes |

The referenced rows' coordinates are their own gates' tables, inside their own preflights.

## NOT READ BY THIS BOOT, SAID PLAINLY, AND OWED

- **THE 72 BOUNDARY: THE PAD IS 48 LONG, SO 72 CANNOT BE STAGED ON IT. NOT READ; it is `SummonGuardTest`'s table
  (72.0 kept, 72.001 given up), owed to ground that is 72 long.** No row leaves the pad.
- **The upward switch onto a mob that DAMAGED him** needs a wound: not staged; the unit table's; owed.
- A held target in another world; an owner who dies mid-fight (the summon is removed, as before): not staged.
- **A 40-block chase:** S1 makes the distance by moving HIM (CC'S CALL below); whether a wolf paths 40 blocks to a target
  is not read (READ: its follow range is the vanilla 16, which caps one path search).
- What a player sees of a name (R5): Ben's answer, `PLAN-owed-fixes.md`, the marked place.

## CC'S CALLS IN THIS GATE

1. **S1 makes the 40 by moving HIM, not by a chase:** READ, `PathNavigation.getMaxPathLength` is `max(FOLLOW_RANGE,
   requiredPathLength)` and `Wolf.createAttributes` sets no follow range (the default 16), so one path search cannot reach
   40; the alternative, a 40-block chase, rests on partial paths, untested.
2. **S1 to S5 take their zombie at the TARGETING tier** (staged by `mob_target`), the tier whose reason can be made to
   lapse by staging; S4's order is the directed tier.
3. **S3 is BT2's layout exactly**, so its landings are the enumerated ones.
4. **The referenced rows' moved predictions are written here,** their frozen gates untouched.

## PLACEHOLDERS OPEN FOR BEN

`PLAN-summon-holds-target.md`: the 72 read from the OWNER to the target; a mob that hits him switches the summon off one
that only targets him. `PLAN-owed-fixes.md`: his answer on what he sees of a name.

## WHAT ONLY THE BOOT CAN SHOW

That the server's goal carries the held target tick to tick as the table does (every `reason=`); that a blink's forget,
a cleared target and a 40-block distance each leave the wolf on its target; that the directed order switches it; that the
idle walk back still runs; that the setups name what they clear; and that the owed rows read as R2 to R6 predict.
