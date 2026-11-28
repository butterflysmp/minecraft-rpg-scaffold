# GATE — SMOLDER'S NINE ARMS (THE SEAT'S V1), ONE ROW, ONE ASK (2026-11-28)

**Status: NOT RUN.** Built on `feat/ultimates-3`, from master `2386162e` (the merge of #342 to #345). The account of the
change is `docs/plans/PLAN-smolder.md` section 9 (V1); Solar Flare's half is `PLAN-solar-flare.md` section 7 (V2, V3) and
`GATE-solar-flare-3.md`. The scenario is `paper/src/main/resources/selftest/GATE-smolder-3.yml`. **Every prediction below
was written before any boot.** The frozen gates (`GATE-smolder-2.md`, `GATE-solar-flare-2.md`, with their READINGS) are
not edited. The queue, the lint, the one ask, the pick and the staging are held by `UltimatesThreeGateCommandTest`; the
point counts by `SmolderContentTest.nineArmsDrawThreeTimesTheThreeArmPointsEveryTick` (EXECUTED).

**GAME MODE: SURVIVAL.**

## WHAT BEN TYPES — ONE COMMAND, FOR BOTH -3 GATES

```
/rpg selftest smolder-3+solar-flare-3
```

- This gate's setup and SM8, then `GATE-solar-flare-3.md`'s setup and SF12: 4 scenarios. Both setups begin with `pad:`,
  so V7 (e) refuses neither.
- **He answers TWO questions: SM8 (Smolder's nine arms), then SF12 (Solar Flare's burst), the command's last step.** Each
  waits 3600 ticks; each row's tell line asks him to **keep his mouse still**.
- **EITHER GATE MAY BE TYPED ALONE** (`/rpg selftest smolder-3`, `/rpg selftest solar-flare-3`), and **both may ride the
  next piece's boot**, appended to that stack's command (`…+smolder-3+solar-flare-3`). That stack's gate then restates
  this command and re-predicts R0's counts for its own jar; the rows below do not change.
- **SM8 picks Smolder into his Fire Mage cell and SF12 Solar Flare into his Fire Ranger cell; the run puts his stored
  builds back whole at its end** (D2).

**How long it runs (an ESTIMATE from the scenarios' `wait:` steps, not a measurement):** this gate 214 ticks (setup 40,
SM8 174), Solar Flare's 144 (setup 40, SF12 104) (`awk` over each file's `- wait:` lines per row, 2026-11-28): **358
ticks, about 18 seconds of rows, then the two questions.**

## SAFETY — 0.000 FOR THE WHOLE COMMAND

- **No step damages him and no mob is summoned:** no `rpg damage`, no `damage`, no `kill @s`, no `summon`, no `spawn:` in
  either gate (asserted).
- **Smolder's pulses and Solar Flare's bursts cannot reach him. EXECUTED** (`UltimatesTwoGateArithmeticTest`, unchanged):
  `MobHostility.harms(ENGINE_ATTACKER, PLAYER, false, PvpPath.PULSE)` = **false**; and a front skips its caster by id
  (READ, `CastExecutor.ringPass`). V1 to V3 change a drawing, a count of drawn points and a sound: no damage path moved.
- **The burst is particles and one sound** (Route B, N4): no firework entity, before or after V2 and V3.
- **A fireball cannot strike him:** the trace excludes its caster by id (READ, `LiveTargets.except`), and every shot flies
  east, away from him.
- **The sum: this gate 0.000, `GATE-solar-flare-3.md` 0.000: 0.000**, at or under 20.000.

## CARRIED STATE — THE BOOTER'S CHECKLIST FOR THE WHOLE COMMAND, IN THIS ORDER (`.claude/rules/booting.md`)

**For a boot of `feat/ultimates-3` on its own.** If the gates ride the next piece's boot, that gate's checklist replaces
this one and carries steps 4 and 10 as written here.

1. The seat's approval of the code AND both gates from origin; Ben's own "ready".
2. **The head:** `git ls-remote --heads origin feat/ultimates-3` equal to what the seat diffed; CI green bound by
   `headSha`. `git status --porcelain` prints nothing and `git rev-parse HEAD` equals that head.
3. **No JVM:** `Get-CimInstance Win32_Process -Filter "Name='java.exe'"` counts 0.
4. **The backup**, to a NEW folder `C:\Users\Neb91\IdeaProjects\rpg-run-backups\<date>-pre-ultimates-3`: `run\world`,
   `run\plugins\Rpg` with their file counts and copy counts, his player `.dat` with its bytes, **his build file's bytes
   and hash, his accessory file's bytes and hash, and his profile's `archetypeId` and element quoted before the boot.**
   SM8 and SF12 pick into his mage/fire and ranger/fire cells, and **the run puts his stored builds back whole**
   (`CAPTURED … builds=[…]` and `RESTORED … builds=[…]` equal, no `NOT-RESTORED=builds(…)`). **His build file's bytes
   before the boot EQUAL its bytes after /stop.**
5. **The world's saved difficulty,** read from the decompressed `level.dat`, before and after the run.
6. **Config:** `dev-pack-on-join: true`. No `run\rpgfight`.
7. **"BOOTING NOW", in its own message, BEFORE the boot command**, with: "Ben: do NOT log in until I tell you R0 passed."
8. `./scripts/dev-server.sh --refresh-content`; the `Done` matched to a log newer than the deployed jar.
9. R0 read on this boot's log; **"R0 PASSED" in its own message, before he logs in.** His first `joined the game` time
   reported beside R0's.
10. After his `/stop`: the clean-stop lines, no `java` process left; his build file, accessory file and profile read
    again (step 4).

**Weather is not captured by the run** (READ: `SelfTestSnapshot` has no weather field); no row changes it.

## R0 — THE BUILD, FOR A BOOT OF THIS BRANCH. IF ANY R0 FAILS, STOP

- **R0a:** `[Rpg] Build: <sha>`, `feat/ultimates-3`'s head as `git ls-remote --heads origin feat/ultimates-3` prints it,
  shortened. Not `-dirty`, not `unknown`.
- **R0b:** `docs/gates/GATE-smolder-3-r0b.ps1` on the DEPLOYED jar: before `'--- absent'` **9 `PRESENT`, 0 `ABSENT`**;
  between `'--- absent'` and `'--- controls'` **5 `ABSENT`, 0 `PRESENT`**; after `'--- controls'` **2 `PRESENT`, 0
  `ABSENT`**. Each PRESENT needle was checked ABSENT in the same file at master `2386162e` first. **Run before commit on
  a jar built from this tree: the bottom of this file.**
- **D2's lines:** `SELFTEST RUN CAPTURED … builds=[…]` and `SELFTEST RUN RESTORED … builds=[…]`, the two `builds=`
  fields EQUAL, no `NOT-RESTORED=builds(…)`.
- **R0c:** `Loaded 11 abilities, 52 visuals, 5 statuses, 7 elements, 28 enchants, 3 pools, 6 fragments, 6 aspects, 27
  weapons, 1 shields, 28 armor, 5 tools, 8 accessories, …` and `Mint-on-craft: 42 indexed, 42 claiming, of 61 gear
  definitions`, **all unchanged from the last boot** (no file added or removed: `ls content/abilities/*.yml | wc -l` 11,
  `ls content/visuals/*.yml | wc -l` 52, on this tree). **0** `WARN]: [Rpg]` (control: `INFO]: [Rpg] Loaded` reads 1 or
  more); none of `Refusing`, `Skipping`, `SEVERE`, `Exception`.
- **R0d:** exactly one **`[Rpg] Selftest: dev server, 582 scenarios across 63 gates [melee-cell, …, smolder-2,
  solar-flare-2, smolder-3, solar-flare-3]`**. Recount: 63 files, 645 keys (`cat GATE-*.yml | grep -cE
  '^[A-Za-z0-9_-]+:'`), 63 setups, so 582. Control: 641 + 2 + 2 = 645. Controls: `580 scenarios` ABSENT, `61 gates`
  ABSENT.
- **R0e:** one `FIGHTWORLD sweep path=…\rpgfight folders=0`, no `deleting`, no `orphan`.

## THE CLOCK AND THE STAND

`GATE-smolder-2.md`'s THE CLOCK and its coordinate table hold unchanged (one clock, `Bukkit.getCurrentTick()`; **C = P +
1**; pulse n at **C + 30 (n − 1)**, exact). SM8 stands him at **G = F + (10, 0)**, block-centred, facing east, and moves
him to **G + 8** at P + 19; pulse 1's origin is G, pulses 2 to 4's G + 8. Each pulse's 12 blocks lie inside the interior
and this gate's preflight `-2 -12 30 12` (x −2 .. 22 from G, 6 .. 30 from G + 8).

## V1, EXECUTED: WHAT NINE ARMS DRAW

Through the calls `CastExecutor.ringPass` makes each tick t (`PulseFront.spiralPoints` over `(radiusAt(t − 1),
radiusAt(t)]`), from the shipped file (`SmolderContentTest.nineArmsDrawThreeTimesTheThreeArmPointsEveryTick`):

| tick after its pulse | 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 | 12 | 13 | 14 | 15 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| points, three arms (the control: PLAN-smolder.md section 8) | 1 | 9 | 9 | 12 | 12 | 15 | 18 | 21 | 21 | 24 | 27 | 30 | 33 | 36 | 36 | 39 |
| points, NINE arms | 1 | 27 | 27 | 36 | 36 | 45 | 54 | 63 | 63 | 72 | 81 | 90 | 99 | 108 | 108 | 117 |

**1027 a pulse (343 at three), 4108 a cast; at most 738 alive at once** (each lives 8 ticks). The arms are 40 degrees
apart; each arm is drawn exactly as an arm of three was (`PulseFrontTest.atNineArmsEveryPointLiesOnOneOfNineArms…`).

## THE ROW

### SM8 — THE PICK, THEN THE ASK (this gate's one question)

- **The pick, as SM1R:** `rpg playerxp`, `clear_inventory`, `clear_state`, `/build`, Class, Mage, Element, Fire, `Q: `,
  Smolder, close (SM1R's own clicks and waits; its menu dumps left out). Then **`CELL class=mage element=fire`** (the
  `expect_cell` step). Any other cell: the row stops there, `SKIPPED`, and no cast is made.
- **The staging, as SM7R, step for step:** the stand at G, `stone`, `hold`, the mana refilled, the tell line, his view
  set at P − 1, the press at P.
- **`DRIVE drop … yaw=-90.000 pitch=0.000`** (unless he moves his mouse in that tick: a different yaw or pitch on the
  line is the reading that says so). **`SMOLDER cast … tick=C`**, C = P + 1: the cast line is the witness that his Q
  is Smolder.
- **`SMOLDER pulse n=1 tick=C`** at G; at P + 19 (C + 18) the `tp ~8`; **`n=2 tick=C+30`**, **`n=3 tick=C+60`**,
  **`n=4 tick=C+90`**, each `origin=` G + 8. **No `SMOLDER hit` line** (no body is staged; he is the caster, the front
  skips him).
- The tell line: "Smolder is cast in a moment from your feet. Keep your mouse still and stand still; you will be moved 8
  blocks east after the first pulse. Watch the fire spread out."
- **The question at P + 130 (C + 129)**, after the fourth front (C + 90 to C + 105) and its last points' 8 ticks (gone by
  C + 113). SEAT PLACEHOLDER 2026-11-28 wording: **"Did nine spiral arms of fire wind out from your feet, and are there
  enough particles now? (yes / no / not sure)"**. Answer logged verbatim; no answer is NOT STATED.

## CC'S CALLS (this gate's)

1. **One row per gate, the pick and the ask together**, the pick first and `expect_cell` after it (the alternative, the
   pick in the setup: a setup is not a row, and `expect_cell` would then guard nothing the row did).
2. **The menu dumps left out of the pick** ("nothing else in them but what staging the ask needs"); the cast line
   witnesses the pick.
3. **The setup is the -2 gate's, step for step** (asserted), so the carried state and the pad are the read ones.
4. **The rows are named SM8 and SF12**, the next numbers after their gates' last new rows, not an R suffix: neither
   re-predicts a row.

## OPEN FOR BEN

- **V1's question wording** (SEAT PLACEHOLDER 2026-11-28).
- Unchanged and still open: the spiral shows its arms while the damage is a full circle; a0 (due east) and the hand
  (clockwise from above); the spiral's particle (orange TRAIL, 8 ticks, 0.4 apart); the pulse's puff; the description.

## WHAT ONLY THE BOOT CAN SHOW

- Whether nine arms are "enough particles" on his screen (SM8's question).
- Solar Flare's burst all orange and the boom in place of the firework sound (SF12's question).

## R0b, RUN BEFORE COMMIT

On `paper/target/rpg-0.1.0-SNAPSHOT.jar` built from this tree (2026-11-28 14:58, `./mvnw -q package -DskipTests`, before
the commit): **9 `PRESENT` / 0 `ABSENT`, then 5 `ABSENT` / 0 `PRESENT`, then 2 `PRESENT`, as predicted.** **Its control,
the jar the last boot deployed (`run/plugins`, `4f26ca31`, 01:08):** 9 `ABSENT`, then 5 `PRESENT`, then 2 `PRESENT`:
exactly inverted, so every needle can see both states. **One needle was corrected while running it:** the old yellow
was first a substring needle (`[255, 220, 40]`), and it read PRESENT on this tree because the burst file's own comment
names the old colour (a false presence: prose about the thing). It is now a value line, `    color: [255, 220, 40]`.
This is a reading of jars on this machine, not of the deployed one: R0b proper is the deployed jar's.
