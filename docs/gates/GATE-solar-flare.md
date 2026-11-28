# GATE — SOLAR FLARE (F1 TO F14), LAST IN THE STACK'S ONE COMMAND (2026-11-27)

**Status: NOT RUN.** The plan is `docs/plans/PLAN-solar-flare.md`. The scenario is
`paper/src/main/resources/selftest/GATE-solar-flare.yml`. The stack, bottom up: master `16713667`, #336
`feat/regen-tooltips-sheet-unit`, #337 `feat/smolder`, #338 `feat/water-douses-scorch`, #340 `feat/solar-flare`, then
the ONE gate branch, `feat/smolder-gate` (#339), last, carrying this gate and `GATE-smolder.md`. **Every prediction below
was written before any boot, and is frozen from the seat's approval.** No boot without Ben's own "ready" and the seat's
approval of the code AND both gates from origin. Every figure is EXECUTED by `SolarFlareGateArithmeticTest` (S-1); the
queue, the lint, the two asks and the summons by `SolarFlareGateCommandTest`.

**GAME MODE: SURVIVAL**, every row.

## WHAT BEN TYPES — ONE COMMAND, FOR THE WHOLE STACK

```
/rpg selftest armor-sets.AS1.AS2+mana-battery.MB1+smolder+solar-flare
```

- The regen-tooltip rows (RT1 to RT3, `GATE-regen-tooltips.md`), then `GATE-smolder.md`'s eleven rows, then this gate's
  nine. 27 scenarios (`SolarFlareGateCommandTest`). Every setup begins with `pad:`.
- **He answers TWO questions: SM7 (Smolder's rings), then SF9 (Solar Flare's), the command's last step.** Each waits 3600
  ticks.
- **SM1 picks Smolder into his Fire Mage build and SF1 Solar Flare into his Fire Ranger build, and the run puts his
  stored builds back whole at its end** (the seat's D2, #341), with his own class and element.
- This command REPLACES the one `GATE-smolder.md` names; that gate's rows are unchanged.

**How long it runs (an ESTIMATE from the scenarios' `wait:` steps, not a measurement):** this gate's waits sum to **1388
ticks (69 s)** (setup 40, SF1 23, SF2 200, SF3 200, SF4 200, SF5 400, SF6 123, SF7 64, SF8 58, SF9 80; `awk`
over the file's `- wait:` lines per row, 2026-11-27); with Smolder's 2665 and the regen rows' 226, **4279 ticks, about
three and a half minutes of rows, then the two questions.**

## SAFETY — 0.000 FOR THE WHOLE COMMAND

- **No step damages him:** no `rpg damage`, no `damage`, no `kill @s` in the queue (asserted).
- **Every mob is a frozen NoAI zombie** (8 here, every one `NoAI:1b` and tagged `selftest`, asserted); his wolf is made
  NoAI by the row.
- **A ring cannot reach him. EXECUTED:** `MobHostility.harms(ENGINE_ATTACKER, PLAYER, false, PvpPath.PULSE)` = **false**;
  and the ring skips its caster by id (READ, `CastExecutor.ringPass`). SF5's wall bursts 2.5 from him: his own rings,
  and they spare him.
- **Both Ignite clauses' blast cannot reach him. EXECUTED:** `harms(ENGINE_ATTACKER, PLAYER, false, IGNITE_SPLASH)` =
  **false**. No staged mob dies in this gate besides.
- **A fireball cannot strike him:** the trace excludes its caster by id (READ, `LiveTargets.except`), and every shot flies
  away from him.
- **His wolf is refused by the ring. EXECUTED:** `harms(ENGINE_ATTACKER, SUMMON, false, PULSE)` = **false**.
- **The sum: this gate 0.000, GATE-smolder 0.000, the regen rows 0.000: 0.000**, at or under 20.000.

## CARRIED STATE — THE BOOTER'S CHECKLIST FOR THE WHOLE COMMAND, IN THIS ORDER (`.claude/rules/booting.md`)

1. The seat's approval of the code AND both gates from origin; Ben's own "ready".
2. **The heads:** `git ls-remote --heads origin` for master, `feat/regen-tooltips-sheet-unit`, `feat/smolder`,
   `feat/water-douses-scorch`, `feat/solar-flare`, `feat/selftest-restore-builds` and `feat/smolder-gate`, equal to what
   the seat diffed; CI green on
   each, bound by `headSha`. `git status --porcelain` prints nothing and `git rev-parse HEAD` equals `feat/smolder-gate`'s
   head.
3. **No JVM:** `Get-CimInstance Win32_Process -Filter "Name='java.exe'"` counts 0.
4. **The backup**, to a NEW folder `C:\Users\Neb91\IdeaProjects\rpg-run-backups\<date>-pre-smolder-solar-flare`:
   `run\world`, `run\plugins\Rpg` with their file counts and copy counts, his player `.dat` with its bytes, **his build
   file's bytes and his profile's `archetypeId` and element quoted before the boot.** SM1 and SF1 pick into his
   mage/fire and ranger/fire cells, and **the run puts his stored builds back whole** (D2, #341: `CAPTURED … builds=[…]`
   and `RESTORED … builds=[…]` equal, with no `NOT-RESTORED=builds(…)`). **His build file's bytes before the boot EQUAL
   its bytes after /stop** (one named exception: with NO build file before, a run that changed one leaves a file holding
   no cell; `PLAN-selftest-restore-builds.md`).
5. **The world's saved difficulty,** read from the decompressed `level.dat`, before and after the run.
6. **Config:** `dev-pack-on-join: true`. No `run\rpgfight`.
7. **"BOOTING NOW", in its own message, BEFORE the boot command**, with: "Ben: do NOT log in until I tell you R0 passed."
8. `./scripts/dev-server.sh --refresh-content` (it clears the deployed content, so neither deleted placeholder is left to
   be named); the `Done` matched to a log newer than the deployed jar.
9. R0 read on this boot's log; **"R0 PASSED" in its own message, before he logs in.** His first `joined the game` time
   reported beside R0's.
10. After his `/stop`: the clean-stop lines, no `java` process left; his build file and profile read again (step 4).

## R0 — THE BUILD, FOR THE WHOLE COMMAND (it SUPERSEDES `GATE-smolder.md`'s R0). IF ANY R0 FAILS, STOP

- **R0a:** `[Rpg] Build: <sha>`, `feat/smolder-gate`'s head as `git ls-remote --heads origin feat/smolder-gate` prints it,
  shortened. Not `-dirty`, not `unknown`.
- **R0b:** on the DEPLOYED jar, `docs/gates/GATE-smolder-r0b.ps1` **21 / 0, 3 / 0, 3 / 0** (unchanged) AND
  `docs/gates/GATE-solar-flare-r0b.ps1`: before `'--- absent'` **15 `PRESENT`, 0 `ABSENT`** (two of them D2's, #341);
  between `'--- absent'` and `'--- controls'` **2 `ABSENT`, 0 `PRESENT`**; after `'--- controls'` **2 `PRESENT`, 0
  `ABSENT`**. Each `Has` and `ValueLine` needle checked ABSENT first: Solar Flare's in `98ad57c6`'s main sources, D2's in
  `78857b8a`'s (`git grep -F`, 0 files each).
- **D2's lines, read at the run's start and end:** `SELFTEST RUN CAPTURED … builds=[…]` and `SELFTEST RUN RESTORED …
  builds=[…]`, the two `builds=` fields EQUAL, and no `NOT-RESTORED=builds(…)`; then one `BUILDS restore write settled
  ok=true` (only if a row changed his builds; an unchanged build is not written).
- **R0c:** `Loaded 11 abilities, 51 visuals, 5 statuses, 7 elements, 28 enchants, 3 pools, 6 fragments, 6 aspects, 27
  weapons, 1 shields, 28 armor, 5 tools, 8 accessories, …` (abilities 11: Smolder and Solar Flare added, both placeholders
  deleted; visuals 51: Smolder's two and Solar Flare's two) and `Mint-on-craft: 42 indexed, 42 claiming, of 61 gear
  definitions`. **0** `WARN]: [Rpg]` (control: `INFO]: [Rpg] Loaded` reads 1 or more); none of `Refusing`, `Skipping`,
  `SEVERE`, `Exception`. Controls: `47 visuals` ABSENT, `49 visuals` ABSENT.
- **R0d:** exactly one **`[Rpg] Selftest: dev server, 562 scenarios across 59 gates [melee-cell, …, smolder,
  solar-flare]`**. Recount: 59 files, 621 keys (`cat GATE-*.yml | grep -cE '^[A-Za-z0-9_-]+:'`), 59 setups, so 562.
  Control: 611 + 10 (this setup and SF1 to SF9) = 621. Controls: `553 scenarios` ABSENT, `58 gates` ABSENT.
- **R0e:** one `FIGHTWORLD sweep path=…\rpgfight folders=0`, no `deleting`, no `orphan`.

## THE CLOCK

- **C** is the server tick of a row's `drop: selected`: the cast, `SOLAR_FLARE cast … tick=C`, and the three `shot` lines
  on the same tick.
- **A shot stops on C + e** with e the least whole number for which 0.9375 (e + 1) reaches the face it meets (READ,
  `ProjectileFlight.step`), or at range on **C + 15**. Its `burst` line is on that tick; its ring's tick 0 is inline.
- **A ring hit lands on the burst's tick + the reach tick** of the body's horizontal distance d from the burst's origin:
  the first whole t with 6 t / 15 >= d. The later ring ticks are region tasks (the global scheduler), as Smolder's.
- **Burns:** the first 19 ticks after a hit, then every 20 (READ, `GATE-smolder.md`, THE CLOCK).

## THE COORDINATE TABLE (from his stand G, block-centred, feet y 0, eye y 1.62; he faces due east, yaw -90, pitch 0)

**D3 (the seat, 2026-11-27, applied here as in GATE-smolder.md): every casting row (SF2 to SF9) stands him at G = F +
(10, 0)** (F is the pad's stand). Mobs are summoned relative to him, so every offset below is from G and every distance,
tick and health is unchanged. Add 10 to x for the figure from F.

| row | what | at | notes |
|---|---|---|---|
| every | his aim and the fan | yaw -120, -90, -60 (n = 1, 2, 3), pitch 0 | the flight lines z = -x tan 30, 0, +x tan 30 |
| SF2, SF5, SF6, SF9 | the range origins | (12.990, 1.62, -7.5), (15, 1.62, 0), (12.990, 1.62, 7.5) | 15.0 from his eye |
| SF2 | z4, z7 | (19, 0, 0), (22, 0, 0) | 4 and 7 past the centre origin |
| SF3 | s8, s13 | (8, 0, 0), (13, 0, 0) | s8's face at 7.7 (a zombie is 0.6 wide, READ) |
| SF4 | the stone; b | block (6, 1, 0), face at 5.5; b at (7, 0, -1.5) | 2.121 from the burst |
| SF5 | t | (14, 0, 4) | 4.123 from origin 2, 3.643 from origin 3 |
| SF5 | the wall; u | blocks (3, 1, -2..2), faces at 2.5; u at (4.5, 0, 0) | the side shots meet it at z -1.443 and +1.443 |
| SF6 | his wolf; w | (14, 0, -2); (16, 0, 2) | both inside ring 2 |

- **Every ring against the pad, from EVERY origin a row uses (executed:
  `SolarFlareGateArithmeticTest.everyRingFromEveryOriginLiesInsideTheInteriorAndThePreflight`).** Edges from F: the
  interior **x -2.5 .. 45.5, z -13.5 .. 13.5**; the preflight `-2 -13 33 13` **x -2.5 .. 33.5, z -13.5 .. 13.5**.

| origin (from G) | from F | rows | x reach from F (origin +/- 6) | z reach | interior | preflight |
|---|---|---|---|---|---|---|
| range n=2, (15, 0) | (25, 0) | SF2, SF5, SF6, SF9 | 19 .. 31 | -6 .. 6 | yes | yes |
| range n=1, n=3, (12.990, -/+7.5) | (22.990, -/+7.5) | the same | 16.99 .. 28.99 | -13.5 .. 13.5 | yes (13.5 = the edge) | yes |
| s8's feet, (8, 0) | (18, 0) | SF3 | 12 .. 24 | -6 .. 6 | yes | yes |
| the stone's face, (5.5, 0) | (15.5, 0) | SF4 | 9.5 .. 21.5 | -6 .. 6 | yes | yes |
| the wall, (2.5, 0) and (2.5, -/+1.443) | (12.5, …) | SF5 | 6.5 .. 18.5 | -7.443 .. 7.443 | yes | yes |

  **The first version of this table checked the RANGE origins alone, from F, and was wrong:** SF5's wall bursts 2.5 from
  F reached x -3.5, outside the interior's -2.5. Found while applying D3, before any approval; G fixes it (6.5). **The side
  rings reach z 13.5, EXACTLY the interior's edge**, and no aim does better: a side origin's distance to the side is
  15 sin 30 whatever the aim's yaw or pitch. The rim (barrier blocks) lies beyond 13.5; no body can stand there.
- The preflight box is `-2 -13 33 13`, inside the interior; every staged body lies inside it (the same test).
- **Health, executed:** `MobScaling.maxHealth(600, false, true, 100)` = 3000 (a summoned pad zombie's score reads 100,
  `GATE-blast-fungus.md`; each row's first PROBE holds it).

## EVERY TERM OF A HIT (F6)

`sent` = 250 x (100 / 100) (the stone in hand: no score stamp, `GearScore.ABSENT`) x (1 + 0) (no enchant) + 0 (class
damage) + 0 (level: a stone cast is not a weapon hit), x charge 1.0, x crit. **A crit CAN occur: 15%, rolled ONCE per
cast for all three shots and their rings (`Crit.multiplier` 2.0):** every hit of one cast reads `sent=250.000
crit=false`, or every one `sent=500.000 crit=true`, `triggerScore=100`. A zombie's defence is 0, so it loses `sent`.
**Scorch:** cap (sent / crit) x 0.5 = **125 either way**; each burn **min(5% of 3000 = 150, 125) = 125**.

## THE ROWS

### SF1 — THE BUILD SCREEN, F11, AND THE PICK

- The first `dump: menu`: his cell now (his, not predicted).
- **After Ranger and Fire (the second dump), F11's reading, ONE of three, named by which:** (i) nothing saved for
  ranger/fire: **`Q: Solar Flare`**, lore `Ultimate -- the default`; (ii) a saved build naming the deleted placeholder:
  **`Q: (empty)`** and ONE **`BUILD unoffered player=<him> cell=ranger/fire slot=ULTIMATE id=ultimate_placeholder_ranger
  reads=empty kept=file`**, no error; (iii) a saved build naming solar_flare: `Q: Solar Flare`, lore `Ultimate`.
- **The picker's Solar Flare:** lore **`Fire three great fireballs in a fan.`**, **`Each bursts within 6 blocks for 250
  fire damage and scorches.`**, **`Cooldown: 210s`** (no mana: the picker's lore).
- **The cell after the pick:** **`Q: Solar Flare`**, lore `Ultimate`, the two lines, **`Damage: 250`**, **`Cost: 80`**,
  **`Cooldown: 210.0 s`** (executed, `AspectLore.lines`), a blank, `Click to change.`
- `STAGE stone … found=0 held=1`; `HELD` the stone.

### SF2 — THE FAN AND THE RANGE

- Before: z4, z7 **`3000.000/3000.000 fireTicks=0`**. **Mana `100.000`, then in the press's tick `20.000`.**
- **`SOLAR_FLARE cast tick=C`; `shot n=1 dir=-120.000 0.000 tick=C`, `n=2 dir=-90.000 0.000`, `n=3 dir=-60.000 0.000`**
  (pitch prints `0.000` or `-0.000`: the sign of a zero, not a reading).
- `item_display` within 24: **3** at C+5; **0** at C+20.
- **`burst n=1 cause=range origin=G+(12.990, 1.620, -7.500) tick=C+15`, `n=2 … G+(15.000, 1.620, 0.000)`, `n=3 …
  G+(12.990, 1.620, 7.500)`**, all on C+15 (the world coordinates are F's plus these).
- **z4: one `SOLAR_FLARE hit n=2 … dist=4.000 tick=C+25 sent=250.000`** (or 500.000) and its PLAYERHIT `source=solar_flare
  element=fire … triggerScore=100`; six burns of 125 from C+44; at C+160 **`2000.000`** (crit **`1750.000`**). **z7: no hit,
  `3000.000`.**

### SF3 — A MOB STOPS IT

- **`burst n=2 cause=mob origin=G+(8.000, 0.000, 0.000) tick=C+8`** (s8's own feet: its face is met on the C+8 step).
- **s8: `hit n=2 dist=0.000 tick=C+8`, one hit only** (no direct hit is added). **s13: `hit n=2 dist=5.000 tick=C+21`.**
  Shots 1 and 3 burst at range on C+15 and reach neither.
- At C+160: s8 and s13 each **`2000.000`** (crit **`1750.000`**).

### SF4 — A BLOCK STOPS IT

- `PROBE_BLOCKS` the stone is there. **`burst n=2 cause=block origin=G+(5.500, 1.620, 0.000) tick=C+5`** (the face the
  trace met). **b: `hit n=2 dist=2.121 tick=C+11`**; shots 1 and 3 burst at range and do not reach it.
- At C+160: **`2000.000`** (crit **`1750.000`**). The stone is put back by the run.

### SF5 — OVERLAP: TWO RINGS, THEN THREE

- **First cast, t:** **`hit n=3 dist=3.643 tick=C+25`** and **`hit n=2 dist=4.123 tick=C+26`**: 250 from each (F5);
  six burns of 125; at C+160 **`1750.000`** (crit **`1250.000`**).
- **Then the wall (5 blocks at 3 east, head height) and u behind it, a second cast at C':** **`burst n=2 cause=block
  origin=G+(2.500, 1.620, 0.000) tick=C'+2`**, **`n=1 … G+(2.500, 1.620, -1.443) tick=C'+3`**, **`n=3 … G+(2.500, 1.620,
  1.443) tick=C'+3`**. **u: `hit n=2 dist=2.000 tick=C'+7`, `hit n=1 dist=2.466 tick=C'+10`, `hit n=3 dist=2.466
  tick=C'+10`**: all three (F5). Six burns of 125. At C'+160 **`1500.000`** (crit **`750.000`**).
- **Why each ring reaches u:** the wall stops every shot 2.5 to 2.887 out, so the three origins lie within 2.466 of u at
  4.5, inside 6. No ring needs sight of u (a burst asks none). **He is 2.5 from the centre origin and is never struck.**

### SF6 — SUMMONS AND THE CASTER

- **w: `hit n=2 dist=2.236 tick=C+21`**; its burns at C+40 and C+60 (the third at C+80); at C+70 **`2500.000`** = 3000
  - 250 - 2 x 125 (crit **`2250.000`**), executed. The probe sits between two burns, so no phase decides it.
- **No line names his wolf**; its `SUMMONPROBE` health is the same before and after. **No `HURT` line**; his `SELF` reads
  the same before and after.

### SF7 — THE COST AND THE COOLDOWN

- Mana `100.000`, the cast at C, **`20.000`**.
- **The press at C+2: refused. `REPLY actionbar Solar Flare -- ready in 209.9s`** (the stone's own notice: 4198 / 20; the
  cooldown read at 4200 from the cast). Mana still `20.000`; no second `SOLAR_FLARE cast`.
- At C+22 `clear_state`: **no cancelled line** (all three burst at C+15; the run has ended). `rpg mana set 79`: `79.000`.
- **The press: refused. `REPLY actionbar Solar Flare -- needs 80 mana, you have 79`**; mana `79.000`; no cast.

### SF8 — CANCEL

- `item_display` within 24: **3** at C+3. At C+5 `clear_state`: **`SOLAR_FLARE cancelled caster=<him> remaining=3
  cause=selftest tick=C+5`.** At C+8: **0** (each flight's next step finds its run ended and removes its body). **No
  `burst` line, no `hit` line, ever.**

### SF9 — THE ASK (THE COMMAND'S LAST STEP)

- The cast at C, east down the pad; the question at C+60, after the three rings (C+15 to C+30).
- SEAT PLACEHOLDER wording: **"Did three large fireballs fly out in a fan, and did each burst into a ring of fire that
  spread outward? (yes / no / not sure)"**. Answer logged verbatim; no answer is NOT STATED.

## CC'S CALLS (the gate's)

1. Each Solar Flare row casts from the stone with his storage cleared (the `stone:` verb, `GATE-smolder.md`'s call).
2. SF5 stages its three-way with a WALL, so every shot is stopped near u; at range three origins are 15 apart at the
   ends and no point is within 6 of all three.
3. SF6's control zombie shares ring 2 with his wolf, so a ring that struck nothing could not pass the row.

## OPEN FOR BEN (this gate's; the stack's whole list is the report's)

- F2: a passive animal or another player in a flight path stops a fireball; the rings strike passive animals.
- F4: the ring's speed. F9: the fireball's look, trail and launch sound. F12: the description. SF9's wording.
- F11 (A3): a saved Fire Ranger build shows an empty ultimate until he picks Solar Flare.

## WHAT ONLY THE BOOT CAN SHOW

- The fireballs as drawn: the display's size at 2.5, its centring and its easing along the flight (SF9).
- The trail, the launch sound, and each ring at its burst.
- The flight and the rings on a real server's clock.

## R0b, RUN BEFORE COMMIT

On `paper/target/rpg-0.1.0-SNAPSHOT.jar` built from this tree (2026-11-27 20:43, `./mvnw -q package -DskipTests`,
before the commit, with D2 and D3): **`GATE-solar-flare-r0b.ps1` 15 `PRESENT` / 0 `ABSENT`, then 2 `ABSENT` / 0, then 2
`PRESENT` / 0; `GATE-smolder-r0b.ps1` 21 / 0, 3 / 0, 3 / 0, unchanged; both as predicted.** The first run after D2 read
14 / 1: the needle `BUILDS restore write settled ok=` sits in the anonymous `Port` class (`SelfTestSnapshot$N.class`),
not `SelfTestSnapshot.class`; it was moved to the CAPTURED field's ` builds=` (absent at `78857b8a`). (The run at 20:19,
before D2, read 13 / 0, 2 / 0, 2 / 0 against its own 13.) A reading of a jar on this machine, not of the deployed one.

## THE GATE'S MUTATIONS

| mutation | the change | red |
|---|---|---|
| MUTFLARENOAI | the control zombie w summoned with `NoAI:0b` | `everyMobThisGateSummons...` and the lint |
| MUTFLARECELL | one row's cell expectation `mage fire` | `everyMobThisGateSummons..._andEveryRowAfterSF1NeedsTheCell` |

Each a one-site splice (MUTFLARECELL's needle occurs 8 times; marker 1, delta 1 against the scratchpad copy), restored by
`cp` with 0 markers left.
