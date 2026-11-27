# GATE — MANA BATTERY (B1 TO B6), WITH THE OWED V7 ROWS RE-READ BY REFERENCE (2026-11-27)

**Status: RUN 2026-11-27 on `486a0161`, the one command; readings at the end, under READINGS.** The plans are `docs/plans/PLAN-mana-battery.md` (B1 to B6) and `docs/plans/PLAN-owed-fixes.md` (V7,
its dated note and *V7 AS BUILT*). The scenario is `paper/src/main/resources/selftest/GATE-mana-battery.yml`. The stack,
bottom up: master `07f03c6b`, `feat/mana-battery`, `feat/owed-v7`, then this ONE gate branch, `feat/mana-battery-gate`,
last and separate so it can move up. **Every prediction below was written before any boot, and is frozen from the seat's
approval.** No boot without Ben's own "ready" and the seat's approval of the code AND this gate from origin. **The gate it
re-reads by reference (GATE-held-target.md) is frozen and not edited;** S3's and S5's new predictions are HERE (the
seat's 2026-11-27 form: a re-read row's moved prediction under its gate.ROW name, in the gate that re-reads it).

## WHAT BEN TYPES — ONE COMMAND

```
/rpg selftest held-target.S3.S5+mana-battery
```

- held-target's SETUP, then only S3 and S5; then this gate's setup and its seven rows. The queue, scenario by scenario,
  is `ManaBatteryGateCommandTest`: 11 scenarios. Neither gate is refused by V7 (e): both setups begin with `pad:`.
- **He answers NOTHING.** No row in the whole command asks (asserted over the queue).
- **This gate's R0 supersedes held-target's** (its R0 is the record).
- He types it on the pad's server, standing anywhere; the first setup takes him to the pad.
- **V7 (e) is NOT typed by Ben:** it is a unit and wiring test (`SelfTestPadRefusalTest`). He is asked, as before, not to
  type any of the 42 gates; they would now refuse with one line and run nothing.

**How long it runs (an ESTIMATE from the scenarios' waits, not a measurement):** the queued `wait:` steps sum to **963
ticks (48 s)** (held-target's setup 40, S3 108, S5 277; this gate's setup 40, AC1 6, MB1 34, MB2 34, MB3 234, MB4 48,
MB5 84, MB6 58; summed by `awk` over the two files). With the row-start CLEANs and the two pad steps, **about a minute,
with no answer to give.**

## SAFETY — 0.000 FOR THE WHOLE COMMAND

- **No row wounds him.** No `/rpg damage`, no `damage @s`, in any queued row (asserted over the queue,
  `ManaBatteryGateCommandTest`).
- **This gate summons no mob** (asserted). S3's and S5's mobs are NoAI zombies (`HeldTargetGateCommandTest
  .everySummonedMobIsNoAi`): they never act.
- **His wolf cannot hurt him.** READ, `MobHostility`'s table, the SUMMON row's PLAYER cell is `no`.
- **S-1:** no term prices damage to him, so there is none to execute; **S-2, S-3:** no dealer acts on him at any score.
- **The sum: S3 0.000 + S5 0.000 + AC1 0.000 + MB1 to MB6 0.000 = 0.000**, at or under 20.000.

## CARRIED STATE — THE BOOTER'S CHECKLIST, IN THIS ORDER (`.claude/rules/booting.md`, each step ticked in the report)

1. The seat's approval of the code AND this gate from origin; Ben's own "ready".
2. **The heads:** `git ls-remote --heads origin` for master, `feat/mana-battery`, `feat/owed-v7` and
   `feat/mana-battery-gate`, equal to what the seat diffed; CI green on each. `git status --porcelain` prints nothing and
   `git rev-parse HEAD` equals `feat/mana-battery-gate`'s head.
3. **No JVM:** `Get-CimInstance Win32_Process -Filter "Name='java.exe'"` counts 0.
4. **The backup**, to a NEW folder `C:\Users\Neb91\IdeaProjects\rpg-run-backups\<date>-pre-mana-battery`: `run\world`,
   `run\plugins\Rpg` with their file counts and copy counts, his player `.dat` with its bytes, **his accessory file's bytes
   and his profile's `archetypeId` quoted before the boot**: whatever they are is what CAPTURED must read, what the first
   pad step's ACCESSORIES line must name, and what RESTORED must put back.
5. **The world's saved difficulty,** read from the decompressed `level.dat`, before and after the run.
6. **Config:** `dev-pack-on-join: true`. No `run\rpgfight`.
7. **"BOOTING NOW", in its own message, BEFORE the boot command**, with: "Ben: do NOT log in until I tell you R0 passed."
8. `./scripts/dev-server.sh --refresh-content`; the `Done` matched to a log newer than the deployed jar.
9. R0 read on this boot's log; **"R0 PASSED" in its own message, before he logs in.** His first `joined the game` time
   reported beside R0's.
10. After his `/stop`: the clean-stop lines, no `java` process left; his accessory file's bytes and `archetypeId` read again
    and compared with step 4's.

**H15 and V7 (a) for this run:** CAPTURED reads his class and four slots as step 4 found them; RESTORED puts back the same
(`accessories=[…]` equal to CAPTURED's, no `NOT-RESTORED=`, `cell=<his class>/<his element>`), read by named fields, and
now ending **`… summons=0 fused=0 blink=none drag_tick=false`** (V7 (c): S5 ends with its clear).

## R0 — THE BUILD. READ IT FIRST. IF ANY R0 FAILS, STOP

- **R0a:** `[Rpg] Build: <sha>`, `feat/mana-battery-gate`'s head as `git ls-remote --heads origin feat/mana-battery-gate`
  prints it, shortened. Not `-dirty`, not `unknown`.
- **R0b:** `docs/gates/GATE-mana-battery-r0b.ps1` on the DEPLOYED jar: before `'--- absent'` **22 `PRESENT`, 0 `ABSENT`**
  (3 `Entry`, 7 `ValueLine`, 12 `Has`; C6 added `perSecondFromPerFiveSeconds` on the scan's class); between `'--- absent'` and `'--- controls'` **3 `ABSENT`, 0 `PRESENT`**; after
  `'--- controls'` **2 `PRESENT`, 0 `ABSENT`**. `GATE-held-target-r0b.ps1` **22 / 0, 3 / 0, 2 / 0**, unchanged.
  Each Has needle was checked ABSENT in master's sources before it was written (one, `'Mana Regen'` in
  `ArmorLoreLines`, was not, and moved to `ArmorItems`; SummonTrace's is the target line's concatenation recipe, because
  the hit line already carries ` tick=`). **Run before commit on a jar built from this tree: see the bottom of this file.**
- **R0c:** `Loaded … 28 enchants, … 27 weapons, 1 shields, 28 armor, 5 tools, 8 accessories, …` (one content file
  added: `mana_battery.yml`; 27 at `07f03c6b`) and `Mint-on-craft: 42 indexed, 42 claiming, of 61 gear definitions`,
  unchanged (no gear added). **0** `WARN]: [Rpg]` (control: `INFO]: [Rpg] Loaded` reads 1 or more); none of
  `Refusing`, `Skipping`, `SEVERE`, `Exception`. Control: `27 enchants` ABSENT.
- **R0d:** exactly one **`[Rpg] Selftest: dev server, 542 scenarios across 57 gates [melee-cell, …, held-target,
  mana-battery]`**. Recount: 57 files, 599 keys (`cat GATE-*.yml | grep -cE '^[A-Za-z0-9_-]+:'`), 57 setups, so 542.
  Control: 591 + 8 (its setup, AC1 and MB1 to MB6) = 599. Controls: `535 scenarios` ABSENT, `56 gates` ABSENT. **V7 (e)
  refuses at the command, never at load: the 42 stay loaded and counted.**
- **R0e:** one `FIGHTWORLD sweep path=…\rpgfight folders=0`, no `deleting`, no `orphan`; no `FUSED removed … cause=orphan`.

## THE SETUPS, THE PAD STEPS AND V7 (a)

**Until a PAD step has placed him, the run touches nothing in the world (P1).** The command's first scenario is
held-target's setup; its row-start CLEAN runs where he typed: `CLEAN … world=skipped`, then `LINGER skipped`, then the
LEFTOVER line (read-only).

- **held-target's pad step:** `PAD world=… stand=… ok=7250`, `PAD stood at=<stand> …`, `ITEMS cleared=<n> pad […]` (a
  reading), and then, NEW (V7 (a)): **`ACCESSORIES cleared=<n> pad [<slot 0>,<slot 1>,<slot 2>,<slot 3>]`, the four
  entries EQUAL to the CAPTURED line's `accessories=[…]`**, and `n` the count of its non-`-` entries (on the boot of
  `ffacaa76` CAPTURED read `[healing_tome,-,-,-]`; whatever step 4 finds is the prediction). Then one **`PAD accessory
  settled slot=<s> ok=true`** per occupied slot.
- **This gate's pad step:** `ITEMS cleared=<k> pad [<type> x<k>, …]`, a READING: S5's `kill @e[tag=h5]` is a death and
  can drop items (INFERRED `rotten_flesh`, k ≥ 0, as GATE-held-target predicted for DB6's kills); S3's mobs go by the
  CLEAN, which drops nothing. Then **`ACCESSORIES cleared=0 pad [-,-,-,-]`** (S3 took the tome off; S5 equips none). No
  `PAD accessory settled` line.
- **This gate's setup:** after `wait 40`, `SELF …`, then `rpg stats`: the baseline sheet, **`Mana Regen 5.00/5s`** (the
  base 1.0/s, EXECUTED in `ManaBatteryRefillTest`: `perFiveSeconds(perSecond(0.05 + perTick(0)))`). **Call his bonus B
  (per second) = (that figure − 5.00) / 5.** If B is not 0 (a fragment of his grants mana regen: the one source no row can
  stage), every sheet prediction below is the stated figure + 5B, and MB3's gains move by 100 × perTick(B); the
  DIFFERENCES predicted are unchanged.

## THE REFERENCED ROWS, RE-PREDICTED

### held-target.S3 — AS `GATE-held-target.md` S3, WITH V7 (b) AND (d)

Every line S3 predicts there, with these moved or added:

- **NEW, the align (V7 (b)):** after the wolf's cast and its first probe, `CMD /execute align xz positioned ~0.5 ~ ~0.5
  run tp @s ~ ~ ~ -90 0` and its reply `Teleported … to <sx + 20.5>, 201.000000, <sz + 0.5>` (sx, sz: the stand's block
  column), then **one tick later** `DRIVE mob_target … tag=bt tick=M`.
- **`SUMMON target … target=<bt> why=targeting reason=acquired:targeting tick=<t>`, t ∈ [M, M + 2]** (V7 (d): readable
  now; INFERRED bound: the wolf's goal runs every tick and decides on its next run).
- **The blink:** `BLINK wearer=… tick=B trigger=zombie <bt> dist=<d> from=<fx> 201.000 <fz> to=<x> 201.000 <z>
  safe=<s> forgot=1` with **`fx − sx ∈ [20.000, 21.000]` and `fz − sz ∈ [0.000, 1.000]`** (within 0.5 of the aligned
  point: the box of drift), **`d ∈ [2.500, 3.536]`** (the distance to `bt` at (sx + 23.5, sz + 0.5) from any point of
  that box), **`s ∈ [495, 508]`** (EXECUTED: `BlinkPadLandingTest`, 441 starts; 508 if he did not move at all, 507 at the
  last boot's 0.185), `to=` a block centre with `x − sx ∈ [4.5, 36.5]`, `z − sz ∈ [−12.5, 13.5]`, inside the box.
- **`SUMMON target … target=<bt> why=targeting reason=kept tick=<k>`, exactly once, k ∈ [B, B + 6]** (the bound the last
  boot read without a tick, now readable; GATE-held-target's original "within 2 ticks of B" is reported as k − B).
- Unchanged: `INVIS present=true ticks=152 … blinked=true`, `BLINK REFUSED-TARGET … ` once, the wolf's hits on `bt`, no
  `SUMMON teleport`, R2's tail (`BLINK STOP … reason=ended`, `BLINK END … reason=cleared`, `STAGE clear_state … blink=ended`),
  exactly one `BLINK wearer=` line.

### held-target.S5 — AS `GATE-held-target.md` S5, WITH V7 (c) AND (d)

- `DRIVE mob_target … tag=h5 tick=M`; **`SUMMON target … target=<h5> why=targeting reason=acquired:targeting tick=<t>`,
  t ∈ [M, M + 2].**
- **NEW (V7 (c)): at least one `SUMMON hit … victim=<h5> authored=20.000 score=100 sent=20.000 crit=none class=0
  level=0 element=nature tick=<h>` with h < M + 120**, successive hits 20 apart (the last boot's S1: 2937, 2957, …).
- At M + 120: `SUMMONPROBE … target=<h5> tier=targeting`, `dist_him` in [12, 15]; then the kill.
- **`SUMMON target … target=none why=none dropped=dead reason=dropped:dead tick=<k>`, k ∈ [M + 120, M + 122]**, before
  the probe 2 ticks later: `SUMMONPROBE … target=none tier=none`, `dist_him=<d1>`.
- 100 ticks later `SUMMONPROBE … target=none tier=none`, `dist_him` LESS than d1; no `SUMMON teleport` line.
- **NEW (V7 (c)): the row's last step, `clear_state`:** `SUMMON removed … cause=clear` for his wolf, then `STAGE
  clear_state … summons=1 …`. **The run's RESTORED reads `summons=0`.**

## THE COORDINATE TABLE (pad coordinates from the stand column: x east, z south)

| what | where | inside the preflight `0 -2 2 2` / held-target's `0 -13 44 13` |
|---|---|---|
| this gate: him, every row (no row moves him; no mob) | (0, 0) | yes |
| S3, S5 | as GATE-held-target.md's table; S3's new align point is its existing stand (20, 0) | yes |

## THE ROWS

Every row: level 1, `clear_inventory`, `clear_state`, `accessory: clear`, `class: mage`, and his four armour slots
emptied before it puts on its own (MB1 wears nothing). **Every sheet figure is the `Mana Regen` line of a `rpg stats`
reply,** read 10 ticks after the wear (two reconcile passes).

> **RE-PREDICTED IN PLACE, 2026-11-27, BEFORE ANY APPROVAL (the seat's C5): Ben chose "B".** Mana Battery's values are
> mana per FIVE SECONDS, the sheet's own unit, so each level moves the sheet by its own number. Every figure below is
> EXECUTED through the whole chain (authored per 5 s -> `ManaRegen.perSecondFromPerFiveSeconds` -> the stat per second
> -> the pool per tick -> `StatsSheetLines.perFiveSeconds`) in `ManaBatteryRefillTest.theChain_authoredToTheSheet`:

| worn | nothing | I | II | III | III + II | III + II + the golden set's 2/s |
|---|---|---|---|---|---|---|
| `Mana Regen` | `5.00/5s` | `6.00/5s` | `7.00/5s` | `8.00/5s` | `10.00/5s` | `20.00/5s` |

### AC1 — V7 (a): THE PAD STEP EMPTIED HIS SLOTS

**`STAGE accessory clear cleared=0`** (the row's own clear finds nothing: this gate's pad step cleared them, and nothing
since equipped one). With the two ACCESSORIES lines above and the RESTORED line, this is the (a) row: cleared on the pad,
put back at the end.

### MB1 — THE TOOLTIP, WHOLE, AND THE POOL OVER 500 ROLLS

For each of `iron_helmet`, `iron_chestplate`, `iron_leggings`, `iron_boots` in turn: `HELDID slot=<n> item=<piece>`
(anything else VOIDS that piece's readings), then the DUMP of its lore, **whole, as EXECUTED** in
`ManaBatteryGateArithmeticTest.mb1_theFourTooltipsWhole` (score 100, Mana Battery III active):

- **helmet:** `Mana Battery III`, `""`, `Gear Score: 100`, `Defense: 10`, **`Mana Regen: +3`**, `""`, `Set Bonus (4
  pieces):`, `+100 Max Health`, `""`, `Honest plate. It has no opinion about who wears it.`, `""`, `Common Helmet`.
- **chestplate:** the same with `Defense: 16` and `Common Chestplate`.
- **leggings:** `Defense: 14`, `Common Leggings`. **boots:** `Defense: 8`, `Common Boots`.

`Mana Regen: +3`'s value is drawn in mana blue (`StatsBarText.MANA_COLOR`, as Mana Bank's `Mana: +30`; asserted in
`ManaBatteryTest`); the DUMP's colour field names it if it prints colours. On the helmet only, `rpg enchant show`:
`"Iron Helmet -- 1 slot(s)"` (the shape read after a clear and one candidate on four weapons), **`  Slot 0:
[*mana_battery III]`**, **`  active: mana_battery III (+3 Mana Regen)`**, `  raw: …`.

Then for EACH piece, `rpg enchant pool`: **`Pool: growth, mana_bank, mana_battery, protection, unbreaking`**, **`Unique
pool: `**, **`Weapon pool: `** (four normals at `07f03c6b`, without `mana_battery`); and `rpg enchant rollstats 500`:

- **`ROLLSTATS <piece> element=none rolls=500 weaponPool=0 uniquePool=0 normalPool=5`** (was `normalPool=4`);
- **`ROLLSTATS slot 0 (weapon): 0=500 1=0 2=0 3=0 4=0`** and **`ROLLSTATS slot 1 (unique): 0=500 1=0 2=0 3=0 4=0`**
  (slots 0 and 1 of armor unchanged: empty);
- `ROLLSTATS slot 2: 0=0 1=<a> 2=<b> 3=<c> 4=<d>` and `slot 3:` the same shape, a + b + c + d = 500, each inside its
  ±3σ band, EXECUTED (`mb1_theRollBands`): **a ∈ [57, 105], b ∈ [157, 221], c ∈ [148, 212], d ∈ [30, 70]**. The
  distribution is the same as at four normals (the cascade's largest tier is 4, so a pool of four already filled it);
- `ROLLSTATS true odds (before the pool cap): 4=10.000% 3=36.000% 2=37.800% 1=16.200%`.

### MB2 — THE SHEET: BASELINE, THEN ONE PIECE AT III

- Bare: **`Mana Regen 5.00/5s`** (the row's own baseline).
- The iron chestplate with Mana Battery III worn: `ARMOR slot=CHEST item=iron_chestplate damage=0 max=<m>
  broken=false`; **`Mana Regen 8.00/5s`** = baseline + 3.00, the number on the tooltip. B4: it does not move with
  score.

### MB3 — THE REAL REFILL, WITH ITS CONTROL

The terms (EXECUTED through the pool's own functions, `ManaBatteryRefillTest.mb3_theRefillOverAHundredTicks`):

```
gain over 100 ticks = 100 × (MANA_PER_TICK + ManaRegen.perTick(bonus per second))
bonus per second    = ManaRegen.perSecondFromPerFiveSeconds(3) = 0.6
MANA_PER_TICK = 100.0 / (100 × 20) = 0.05          ManaRegen.perTick(0.6) = 0.03
with the piece (III):       10 + 100 × (0.05 + 0.03) = 18.000     (EXECUTED: 18.0 exactly)
the control (none):         10 + 100 × 0.05          = 15.000     (EXECUTED: 15.0 exactly)
```

**The clock:** the server tick. `rpg mana set 10` and the first `probe_mana` run in the same pass of the run's advance,
on his entity scheduler (same tick: the command does not hop, READ); the second `probe_mana` runs 100 ticks later, by
`wait: 100`, in the same phase.

- Worn: `Mana Regen 8.00/5s`; `Mana set to 10.` (green); **`MANA current=10.000 max=100.000`**; 100 ticks later **`MANA
  current=18.000 max=100.000`**.
- Off (the control): `Mana Regen 5.00/5s`; **`MANA current=10.000 max=100.000`**; 100 ticks later **`MANA current=15.000
  max=100.000`**.
- **The piece adds exactly 3.000 over 100 ticks** (100 × 0.03: 3 mana per 5 seconds over 5 seconds). `max=` is his ceiling: 100.000 unless something of his
  raises max mana (then the row reads it; the currents do not move, all being below 100).

### MB4 — LEVELS I AND II

- At I: **`Mana Regen 6.00/5s`** (+1 per 5 s). Taken to the hand, set to II, worn again: **`Mana Regen 7.00/5s`** (+2
  per 5 s).

### MB5 — STACKING, AND THE GOLDEN SET

- Iron chestplate at III and iron leggings at II: `ARMOR` lines CHEST and LEGS, `broken=false`; **`Mana Regen
  10.00/5s`** (3 + 2 per 5 s).
- A golden full set, its chestplate at III and its leggings at II: four `ARMOR` lines, golden; **`Mana Regen 20.00/5s`**
  (the enchants' 5 per 5 s + the set's 2 per SECOND, i.e. 10 per 5 s; the set's +2/s READ on the boot of the armor sets,
  `GATE-armor-sets.md` AS2: `15.00/5s` from `5.00/5s`; its unit unchanged, C3).
- The golden boots swapped for untagged iron boots (the set off: its control): **`Mana Regen 10.00/5s`** (the set's
  gone, the two enchants still worn). Equal to this row's first reading by construction: both are the two enchants
  alone.

### MB6 — CARRIED, NOT WORN; BROKEN; REPAIRED

- The piece at III in his HAND, nothing worn: **`Mana Regen 5.00/5s`** (a carried piece adds nothing).
- `rpg durability set 100000` in the hand, then worn: **`ARMOR slot=CHEST item=iron_chestplate damage=<m − 1> max=<m>
  broken=true`** (`Durability.clamp` holds damage at max − MIN_USES, MIN_USES = 1, READ; `isBroken` is `damage ≥ max −
  1`); **`Mana Regen 5.00/5s`** (a broken piece adds nothing).
- Back to the hand, `rpg durability set 0`, worn again: **`ARMOR … damage=0 … broken=false`**; **`Mana Regen 8.00/5s`**
  (the control: the same piece, unbroken, adds 3 per 5 s).

## CC'S CALLS (the gate's)

1. **The enchant is put on the piece by the dev commands in column 0**, as TT7 put Mana Bank on; the roll placement (normal
   slots 2 and 3) is MB1's pool and rollstats readings, and `ArmorPoolsTest`/`ManaBatteryTest`. The alternative stages
   columns 0 and 1 first so it can sit in column 2.
2. **The sheet is read in its own unit, `/5s`,** which is now also Mana Battery's authored unit (Ben's "B").
3. **MB5's golden set uses untagged iron boots as the control**, the swap GATE-armor-sets AS2 used.

## OPEN FOR BEN (every placeholder in this stack)

1. The icon, `redstone_block` (SEAT PLACEHOLDER 2026-11-27).
2. The stat-block line's wording, **`Mana Regen: +3`**; whether to show `/5s` (C4).
3. The enchant effect suffix, **`(+3 Mana Regen)`**; whether to show `/5s` (C4).
4. **RESOLVED 2026-11-27: Ben chose "B"** (the tooltip and the sheet now read the same number). **Still open (C3): the
   golden set's tooltip says `+2 Mana Regen per second` while the sheet moves by 10** (`5.00/5s` -> `15.00/5s`).
5. The refusal line, **`Selftest: '<gate>' predates the pad and would run on your ground. Refused.`** (SEAT PLACEHOLDER).
6. The pad step's line, `ACCESSORIES cleared=<n> pad […]`: a log line, not chat (nothing for Ben to see; listed for
   completeness).

## WHAT ONLY THE BOOT CAN SHOW

- That a real worn piece's enchant reaches the pool's rate on the server (the scanner is boot-witnessed: it needs a live
  Player), and that the sheet, the stat and the refill agree (MB2 to MB6).
- That the pad step's clear lands on his real store and the RESTORED line puts his slots back (AC1, H15).
- That the wolf's push is inside the box of drift in practice (S3's `from=`), and that S5 reads a hit before its kill.
- What the tooltip LOOKS like to him (the colour as drawn): an AUTO reading of the server, never of the client.

## R0b, RUN BEFORE COMMIT

On `paper/target/rpg-0.1.0-SNAPSHOT.jar` built from this tree (2026-11-27 15:42, after Ben's "B" and the rebase,
`./mvnw -o -q package -DskipTests`): **`GATE-mana-battery-r0b.ps1`: 22 `PRESENT` / 0 `ABSENT`, then 3 `ABSENT` / 0, then
2 `PRESENT` / 0, as predicted** (the first run, 15:14, before "B", read 21 / 0, 3 / 0, 2 / 0 against its own 21) (the SummonTrace needle matched the target line's recipe, `reason=`,
char 1, ` tick=`). **`GATE-held-target-r0b.ps1`: 24 `PRESENT` and 3 `ABSENT` in all, which is its 22 + 2 and 3,
unchanged.** This is a reading of a jar on this machine, not of the deployed one: R0b proper is the deployed jar's.

## READINGS — THE BOOT OF `486a0161`, 2026-11-27 (written after the run; nothing above this heading was edited)

**HIS ACCESSORY FILE DID NOT COME BACK AS IT WAS, AND NOTHING WAS LOST: THE SEAT RULES THE REPAIR; NOTHING WAS TOUCHED.**
Before the boot: `accessories\b6ae27e9-….json` **582 bytes**, sha256 `C8BC8B808928D550…`, slot 0 = the Healing Tome. After
`/stop`: **95 bytes, `"entries": []`, written at 15:53:00**. The run's RESTORED (15:52:44) read `accessories=[healing_tome,-,-,-]`
back, as predicted; the file was rewritten **16 s later**, 5 s before `/stop`, with **no log line of any kind between
`RUN HEALTH` (15:52:45) and `/stop` (15:53:05)** except the session-host line. **His player file, saved at `/stop`, now
holds the Healing Tome in his inventory** (decompressed `.dat`: `healing_tome` 0 before -> 1 after, `Healing Tome` 0 -> 1;
the pre-boot copy is in the backup). So the tome moved from the slot to his inventory after the run. INFERRED: he took it
out of the slot himself in the menu (an unequip writes the file and logs nothing). UNVERIFIED: any automatic ejection.
His profile reads `archetypeId: "mage"`, `elementId: "fire"`, as before. The world's saved difficulty: `normal` before
and after.

### The boot (`.claude/rules/booting.md`, each step as ticked)

1. The seat's approval of `486a0161`; Ben's own "ready".
2. `git ls-remote --heads origin`: master `07f03c6b`, `feat/mana-battery` `871f5c52`, `feat/owed-v7` `4b171bbb`,
   `feat/mana-battery-gate` `486a0161`; `git status --porcelain` empty; `HEAD` = `486a0161`.
3. `java.exe` count 0.
4. Backup `rpg-run-backups\2026-11-27-pre-mana-battery`: `world` 193 / 193 files, `Rpg` 191 / 191; his `.dat` 5739 bytes;
   accessory file 582 bytes (`C8BC8B80…`, slot 0 one item); `archetypeId: "mage"`, `elementId: "fire"`.
5. `level.dat` difficulty `normal`. 6. Config line 12 `dev-pack-on-join: true`; no `run\rpgfight`.
7. "BOOTING NOW", its own message, before the boot command. 8. `dev-server.sh --refresh-content`; marker 15:50:04, jar
   15:50:35, log's first line 15:50:36, `Done (6.778s)` 15:50:42.
9. R0 read 15:50:59, "R0 PASSED" its own message; **his `joined the game` 15:51:45**, after it. After `/stop` (15:53:05):
   `java.exe` 0.

### R0 — READ AS PREDICTED

`[Rpg] Build: 486a0161`. R0b on the deployed jar: `GATE-mana-battery-r0b.ps1` 22 / 0, 3 / 0, 2 / 0;
`GATE-held-target-r0b.ps1` 22 / 0, 3 / 0, 2 / 0. `Loaded 1 summon(s)`; `… 28 enchants, … 27 weapons, 1 shields, 28 armor,
5 tools, 8 accessories, …`; `Mint-on-craft: 42 indexed, 42 claiming, of 61 gear definitions`; 0 `WARN]: [Rpg]`, 7 `INFO]:
[Rpg] Loaded`, 0 `Refusing`, `Skipping`, `SEVERE`, `Exception`; `27 enchants` absent. One `Selftest: dev server, 542
scenarios across 57 gates [melee-cell, …, held-target, mana-battery]`; `535 scenarios`, `56 gates` absent.
`FIGHTWORLD sweep … folders=0`; no `deleting`, no `orphan`.

### HIS GROUND, THE PAD STEPS, CAPTURED AND RESTORED — READ AS PREDICTED

- The run 15:51:51 to 15:52:44, `FINISHED`. **0 `HURT` lines in the log: SAFETY held.**
- `CAPTURED armour=0 offhand=0 class=mage accessories=[healing_tome,-,-,-]`.
- held-target's setup CLEAN: `… inventory=kept return=-104.47 63.00 15.21 drum=0 blink=none world=skipped`, then `LINGER
  skipped` (his ground: nothing touched before the pad). Its pad step: `PAD … ok=7250`, `PAD stood at=104.50 201.00 -3.50
  …`, `ITEMS cleared=0 pad []`, **`ACCESSORIES cleared=1 pad [healing_tome,-,-,-]`** (= CAPTURED's), **`PAD accessory settled
  slot=0 ok=true`**.
- This gate's pad step: `ITEMS cleared=0 pad []` (S5's kill dropped nothing on the pad), **`ACCESSORIES cleared=0 pad
  [-,-,-,-]`**, no settled line. Every later CLEAN `world=pad`, `LINGER work=0 entities=0 pad`.
- `RESTORED xp=712600 cell=mage/fire gamemode=SURVIVAL held=3 mana=100.0 trace=false spawn_mobs=false time=1000 blocks=0
  blocksBack=0/0 mobsRemoved=0 orbsRemoved=0 keep_inventory=false immediate_respawn=false difficulty=NORMAL health=162.000
  vanilla_xp=118+0.916 spawn_phantoms=true max_summons_base=null saturation=0.0 accessories=[healing_tome,-,-,-] armour=0
  offhand=0 summons=0 fused=0 blink=none drag_tick=false`: **accessories equal CAPTURED's, `summons=0` (V7 (c))**, his
  armour back (`armour=0` as captured); `RUN HEALTH saved=162.000 … wrote=162.000`.

### held-target.S3 — READ AS PREDICTED, ONE FIELD A MISS (THE GATE'S)

Stand column (104, −4). The align `Teleported … to 124.500000, 201.000000, -3.500000`, then `DRIVE mob_target … tag=bt
tick=1495` (M). **`SUMMON target … target=<bt> why=targeting reason=acquired:targeting tick=1495`** (M, in [M, M + 2]).
Hit 1501. `STAGE accessory equip slot=0 id=blink_tome tick=1507` (E); `BLINK START … tick=1508` (T0 ∈ [E, E + 5]);
**`BLINK wearer=BaronVonYeetus tick=1513 trigger=zombie <bt> dist=3.180 from=124.320 201.000 -3.500 to=115.500 201.000
-15.500 safe=507 forgot=1`**: fx − sx = 20.320 ∈ [20, 21], fz − sz = 0.500 ∈ [0, 1] (drift 0.180 west of the align, inside
the box), dist ∈ [2.500, 3.536], **safe=507 ∈ [495, 508]**, to= a block centre at (11.5, −11.5), inside the box. **`SUMMON
target … reason=kept tick=1515`, exactly once: k − B = 2** (inside [B, B + 6]; GATE-held-target's "within 2 ticks of B"
now READ: 2). `SELF … at=115.50 201.00 -15.50` = to=. Hits 1521, 1541; `BLINK REFUSED-TARGET … tick=1522` once; no `SUMMON
teleport`. R2's tail: `BLINK STOP … tick=1543 reason=ended`, `SUMMON removed … cause=clear`, `BLINK END … tick=1549
reason=cleared`, `STAGE clear_state … summons=1 … blink=ended`. One `BLINK wearer=` line.
- **MISS, the gate's: `INVIS present=true ticks=150`, against the copied `ticks=152`.** The figure is 160 − (probe − B), the
  probe at E + 16 = 1523, B = 1513: 150. The last boot's 152 had B = E + 8; this one B = E + 6 (T0 at E + 1, not E + 3). A
  prediction carried as a constant over a quantity that moves with the check clock's phase; the product read consistent
  with its own B.

### held-target.S5 — READ AS PREDICTED

`DRIVE mob_target … tag=h5 tick=1610` (M); `SUMMON target … acquired:targeting tick=1610`. **`SUMMON hit … victim=<h5>
authored=20.000 score=100 sent=20.000 …` at 1670, 1690, 1710** (20 apart, all before M + 120 = 1730: V7 (c)). Probe at M +
120: `target=<h5> tier=targeting dist_him=13.33`. **`SUMMON target … dropped=dead reason=dropped:dead tick=1730`** (M + 120).
Probes after: `target=none tier=none`, `dist_him` 9.42, then 7.60 (less); no `SUMMON teleport`. **`SUMMON removed … cause=clear`,
`STAGE clear_state … summons=1`** (V7 (c)).

### AC1 — READ AS PREDICTED

`STAGE accessory clear cleared=0`.

### MB1 — READ AS PREDICTED

Each piece `HELDID slot=0 item=<piece>`. The four lores, whole, equal to the executed ones: `["Mana Battery III"; "";
"Gear Score: 100"; "Defense: 10|16|14|8"; "Mana Regen: +3" [gray, blue]; ""; "Set Bonus (4 pieces):"; "+100 Max Health";
""; "Honest plate. …"; ""; "Common Helmet|Chestplate|Leggings|Boots"]` (the name's `[white] italic` is R5's wrapper shape,
the instrument's). `"Iron Helmet -- 1 slot(s)"`, `"  Slot 0: [*mana_battery III]"`, `"  active: mana_battery III (+3 Mana
Regen)"`. Each piece: `Pool: growth, mana_bank, mana_battery, protection, unbreaking`, `Unique pool: `, `Weapon pool: `;
`normalPool=5`; slots 0 and 1 `0=500`; the true-odds line as predicted. Slots 2 and 3 (1 / 2 / 3 / 4 candidates), every
count in its band and each summing to 500: helmet 96/179/183/42 and 80/178/193/49; chestplate 80/184/184/52 and
65/188/201/46; leggings 79/201/174/46 and 91/183/174/52; boots 75/195/183/47 and 89/195/158/58.

### MB2 TO MB6 — READ AS PREDICTED

- **The baseline (the setup's sheet): `Mana Regen 5.00/5s`, so B = 0.**
- **MB2:** bare `5.00/5s`; `ARMOR slot=CHEST item=iron_chestplate damage=0 max=240 broken=false`; **`8.00/5s`**.
- **MB3:** worn `8.00/5s`; `Mana set to 10.`; **`MANA current=10.000 max=100.000`**, then **`MANA current=18.000
  max=100.000`**; off `5.00/5s`; **`10.000`**, then **`15.000`**. The MANA line carries no tick field: the interval is the
  scenario's `wait: 100`, and both pairs read 5 s apart on the log's clock (15:52:23 -> 15:52:28, 15:52:28 -> 15:52:33).
- **MB4:** I `6.00/5s`, II `7.00/5s`.
- **MB5:** iron III + II `10.00/5s`; golden full set (four golden ARMOR lines) **`20.00/5s`**; golden boots swapped
  `10.00/5s`.
- **MB6:** in hand `5.00/5s`; `1/240 uses left (damage 239) -- BROKEN`, `ARMOR … damage=239 max=240 broken=true` (max − 1),
  `5.00/5s`; `240/240 uses left (damage 0)`, `ARMOR … damage=0 … broken=false`, **`8.00/5s`**.

### WHAT IT FOUND

1. **The product read as predicted on every row:** Mana Battery's tooltip, pool, sheet and refill, the pad step's
   accessory clear and its restore, S3's alignment and drift box, S5's hit before the kill, `tick=` on the target line.
2. **One MISS, the gate's:** S3's INVIS ticks, a constant copied over a phase-dependent quantity.
3. **His accessory slot after the run:** the Healing Tome is in his inventory, not slot 0; written to his file 16 s after
   RESTORED, with nothing logged. For the seat to rule, and Ben to say whether he moved it.
