# GATE — SMOLDER (Z1 TO Z11) AND WATER PUTS OUT SCORCH (Y1 TO Y7, E1 TO E4), WITH THE REGEN-TOOLTIP ROWS (2026-11-27)

**Status: NOT RUN.** The plan is `docs/plans/PLAN-smolder.md` (Part 1 section 3, Part 2 section 6). The scenario is
`paper/src/main/resources/selftest/GATE-smolder.yml`. The stack, bottom up: master `16713667`, #336
`feat/regen-tooltips-sheet-unit`, #337 `feat/smolder`, #338 `feat/water-douses-scorch`, then this ONE gate branch,
`feat/smolder-gate`, last. **Every prediction below was written before any boot, and is frozen from the seat's
approval.** No boot without Ben's own "ready" and the seat's approval of the code AND this gate from origin. Every
figure is EXECUTED by `SmolderGateArithmeticTest` (S-1); the queue, the lint, the one ask and the summons by
`SmolderGateCommandTest`.

**GAME MODE: SURVIVAL**, every row (as both referenced gates declare for theirs).

## WHAT BEN TYPES — ONE COMMAND

> **SUPERSEDED, 2026-11-27 (before any boot), by Solar Flare (#340) joining the stack:** Ben types the ONE command in
> `GATE-solar-flare.md`, `/rpg selftest armor-sets.AS1.AS2+mana-battery.MB1+smolder+solar-flare`, which runs this gate's
> rows unchanged and then that gate's; SM7 is then the first of two questions, not the last. **That gate's R0 and
> CARRIED STATE are the boot's**, and they supersede this gate's two sections below (kept as written, for the record).
> Nothing else here moves.

```
/rpg selftest armor-sets.AS1.AS2+mana-battery.MB1+smolder
```

- armor-sets' SETUP, AS1, AS2; mana-battery's SETUP, MB1 (the regen-tooltip rows, predicted in `GATE-regen-tooltips.md`
  as RT1 to RT3, unchanged); then this gate's setup and its eleven rows. 17 scenarios (`SmolderGateCommandTest`). Every
  setup begins with `pad:`, so V7 (e) refuses none.
- **He answers ONE question, SM7's, the command's last step.** He watches first; it waits 3600 ticks for his answer.
- He types it on the pad's server, standing anywhere; the first setup takes him to the pad.
- **SM1 picks Mage, Fire and Smolder on Q through the Build screen, and that pick is SAVED** (CARRIED STATE, step 4).

**How long it runs (an ESTIMATE from the scenarios' `wait:` steps, not a measurement):** this gate's waits sum to
**2665 ticks (133 s)** (setup 40, SM1 23, SM2 393, SM3 380, SM4 380, SM5 280, SM6 64, WA1 214, WA2 208, WA3 270, WA6 173,
SM7 240; `awk` over the file's `- wait:` lines per row, 2026-11-27); the regen rows add 226 (`GATE-regen-tooltips.md`).
**About three minutes, then the one question.**

## SAFETY — 0.000 FOR THE WHOLE COMMAND

- **No step damages him:** no `rpg damage`, no `damage`, no `kill @s` in the queue (asserted, `SmolderGateCommandTest`).
- **Every mob is a frozen NoAI zombie** (18 summons, every one `NoAI:1b` and tagged `selftest`, asserted); his wolf is
  made NoAI by the row. They never act.
- **Smolder's pulse cannot reach him. EXECUTED:** `MobHostility.harms(ENGINE_ATTACKER, PLAYER, false, PvpPath.PULSE)` =
  **false** (the table's PULSE row: players spared, "no players for now"); and the ring skips its caster by id (READ,
  `CastExecutor.ringPass`).
- **An Ignite blast cannot reach him. EXECUTED:** `MobHostility.harms(ENGINE_ATTACKER, PLAYER, false,
  PvpPath.IGNITE_SPLASH)` = **false**. WA6's three blasts are 6 or more blocks from him besides.
- **No Scorch is ever applied to him**: no row aims fire at him; his own bolts fly away from him.
- **His wolf is refused by the pulse. EXECUTED:** `harms(ENGINE_ATTACKER, SUMMON, false, PULSE)` = **false**.
- **S-1:** no term prices damage to him, so there is none to execute beyond the three `harms` calls above; **S-2, S-3:**
  no dealer acts on him at any score.
- **The sum: every row 0.000, so 0.000**, at or under 20.000.

## CARRIED STATE — THE BOOTER'S CHECKLIST, IN THIS ORDER (`.claude/rules/booting.md`, each step ticked in the report)

1. The seat's approval of the code AND this gate from origin; Ben's own "ready".
2. **The heads:** `git ls-remote --heads origin` for master, `feat/regen-tooltips-sheet-unit`, `feat/smolder`,
   `feat/water-douses-scorch` and `feat/smolder-gate`, equal to what the seat diffed; CI green on each, bound by
   `headSha`. `git status --porcelain` prints nothing and `git rev-parse HEAD` equals `feat/smolder-gate`'s head.
3. **No JVM:** `Get-CimInstance Win32_Process -Filter "Name='java.exe'"` counts 0.
4. **The backup**, to a NEW folder `C:\Users\Neb91\IdeaProjects\rpg-run-backups\<date>-pre-smolder`: `run\world`,
   `run\plugins\Rpg` with their file counts and copy counts, his player `.dat` with its bytes, **his build file's bytes
   and his profile's `archetypeId` and element quoted before the boot.** The run's restore puts back his CELL (`RESTORED
   … cell=<class>/<element>`) but NOT a saved loadout: **SM1 saves Q = Smolder in his mage/fire cell, and that stays**
   (as `GATE-sunder.md`'s SU1 did). After the run his build file is read again and the change is named.
5. **The world's saved difficulty,** read from the decompressed `level.dat`, before and after the run.
6. **Config:** `dev-pack-on-join: true`. No `run\rpgfight`.
7. **"BOOTING NOW", in its own message, BEFORE the boot command**, with: "Ben: do NOT log in until I tell you R0 passed."
8. `./scripts/dev-server.sh --refresh-content` (it clears the deployed content, so no stale
   `ultimate_placeholder_mage.yml` is left to be named); the `Done` matched to a log newer than the deployed jar.
9. R0 read on this boot's log; **"R0 PASSED" in its own message, before he logs in.** His first `joined the game` time
   reported beside R0's.
10. After his `/stop`: the clean-stop lines, no `java` process left; his build file and profile read again (step 4).

**Weather is not captured by the run** (READ: `SelfTestSnapshot` has no weather field), so no row changes it, and WA4
(rain) is owed (below).

## R0 — THE BUILD. READ IT FIRST. IF ANY R0 FAILS, STOP

- **R0a:** `[Rpg] Build: <sha>`, `feat/smolder-gate`'s head as `git ls-remote --heads origin feat/smolder-gate` prints
  it, shortened. Not `-dirty`, not `unknown`.
- **R0b:** `docs/gates/GATE-smolder-r0b.ps1` on the DEPLOYED jar: before `'--- absent'` **21 `PRESENT`, 0 `ABSENT`**
  (6 `Entry`, 4 `ValueLine`, 11 `Has`, two of them #336's); between `'--- absent'` and `'--- controls'` **3 `ABSENT`,
  0 `PRESENT`**; after `'--- controls'` **3 `PRESENT`, 0 `ABSENT`**. Each `Has` and `ValueLine` needle was checked ABSENT
  in master `16713667`'s main sources before it was written (`git grep -F`, 0 files each, except `description: must be
  a YAML list`, which master has in `EnchantLoader.java` only; the needle reads `AbilityLoader.class`). **Run before
  commit on a jar built from this tree: the bottom of this file.**
- **R0c:** `Loaded 11 abilities, 49 visuals, 5 statuses, 7 elements, 28 enchants, 3 pools, 6 fragments, 6 aspects, 27
  weapons, 1 shields, 28 armor, 5 tools, 8 accessories, …` (abilities 11 = one added, one deleted; visuals +2:
  `smolder_ring`, `smolder_pulse`; enchants 28 since `mana_battery`) and `Mint-on-craft: 42 indexed, 42 claiming, of 61
  gear definitions`, unchanged. **0** `WARN]: [Rpg]` (control: `INFO]: [Rpg] Loaded` reads 1 or more); none of
  `Refusing`, `Skipping`, `SEVERE`, `Exception`. Controls: `47 visuals` ABSENT, `27 enchants` ABSENT.
- **R0d:** exactly one **`[Rpg] Selftest: dev server, 553 scenarios across 58 gates [melee-cell, …, mana-battery,
  smolder]`**. Recount: 58 files, 611 keys (`cat GATE-*.yml | grep -cE '^[A-Za-z0-9_-]+:'`), 58 setups, so 553. Control:
  599 + 12 (this setup and its eleven rows) = 611. Controls: `542 scenarios` ABSENT, `57 gates` ABSENT.
- **R0e:** one `FIGHTWORLD sweep path=…\rpgfight folders=0`, no `deleting`, no `orphan`.

## THE CLOCK (every tick prediction names it)

- **C** is the server tick of a Smolder row's `drop: selected` step: the stone's Q input, the cast (`SMOLDER cast …
  tick=C`). Pulse n fires at **C + 60 x (n - 1)** (`PulseFront.pulseTick`), from his feet at that tick.
- **A ring hit lands at the pulse's tick + the reach tick** of the body's horizontal distance d: the first whole t in
  0..30 with 12 x t / 30 >= d (`PulseFront.reachTick`, executed per body below). A hit is a REGION task.
- **READ by javap, the pinned 26.1.2 server:** on Paper the region scheduler is `FallbackRegionScheduler`, which
  delegates to the GLOBAL region scheduler, and `MinecraftServer.tickChildren` runs `FoliaGlobalRegionScheduler.tick`
  BEFORE every `EntityScheduler.executeTick`. `EntityScheduler.schedule` keys a delay at `tickCount + max(1, delay)` and
  `executeTick` increments `tickCount` before it looks. So **a burn booked in a hit's tick first lands 19 ticks later,
  then every 20** (re-armed inside `executeTick`), and a later hit, 60 ticks after the first, comes one tick AFTER a
  burn, never with one. Executed through the real store in `SmolderGateArithmeticTest` (its javadoc: the fake clock is
  run with each later hit one tick later, the same order).
- **P** is a Flint Staff row's press (`use: right_click`); a bolt flies 1.4 blocks a tick (gravity 0.05), so its hit is
  a few ticks after P (not predicted to the tick: the lines carry it).

## THE COORDINATE TABLE (from his stand F, block-centred: x east, z south; every zombie at his y)

| row | body | at | horizontal from the ring's origin | reach tick | its max health |
|---|---|---|---|---|---|
| SM2 | a3, a6, a11, a12 | (3, 0), (6, 0), (11.5, 0), (12.5, 0) | 3, 6, 11.5, 12.5 | 8, 15, 29, never | 3000 |
| SM2 | his wolf | (0, 4) | 4 | (refused by the table) | his summon |
| SM3 | b6 | (6, 0) | 6 | 15 | 3000 |
| SM4 | c11 | (0, -11) | 11 from F; 13.601 from F+8 | 28 (pulse 1); never | 3000 |
| SM4 | c19 | (19, 0) | 19 from F; 11 from F+8 | never; 28 (pulses 2 to 4) | 3000 |
| SM4 | him, moved | F+(8, 0) at C+40 | | | |
| SM5 | d6 | (6, 0) | 6 | 15 | 3000 |
| WA1 | w1 in water; w2 dry | (5, -3); (5, 3) | (a bolt each) | | 1000 |
| WA2 | v, then into the water | (5, 3), then (5, -3) | (a bolt) | | 1000 |
| WA3 | u in water | (6, -3) | 6.708 | 17 | 3000 |
| WA6 | k1, k1w; k2 in water, k2w; k3, k3w | (6, -8), (7.5, -8); (6, 0), (6, 2.5); (6, 8), (7.5, 8) | (bolts) | | k1 1000, k2 10, k3 10, witnesses 1000 |

- **The water cells:** a 3 x 3 glass ring at floor height with water in its centre, saved first (`save_blocks`) and put
  back by the run: WA1 and WA2 at x 4..6, z -4..-2 (water at (5, -3)); WA3 at x 5..7, z -4..-2 (water at (6, -3)); WA6 at
  x 5..7, z -1..1 (water at (6, 0)). Glass holds the water (it does not flow into a solid block) and stops nothing: a
  bolt aimed eye to eye passes above the 1-block ring, and a ring hit asks no line of sight.
- **Every ring against the pad:** radius 12 from F reaches x -12..12, z -12..12; from F+8, x -4..20. The pad's interior is
  x -2..45, z -13..13 (`SelfTestPadShape`), and the preflight box is `-2 -12 20 12`. **The ring's DRAWING west of x = -2
  is outside the pad; no body is staged there**, and the hits are by distance, not by the drawing.
- **Health, executed:** `MobScaling.maxHealth(600, false, true, 100)` = 3000; `(200, …)` = 1000; `(2, …)` = 10. The gear
  score of a summoned zombie on the pad reads 100 (`GATE-blast-fungus.md`: max_health 200 read as `1000.000/1000.000`); the
  first PROBE of each row is the reading that holds it.

## EVERY TERM OF A HIT

**Smolder (every SM row, WA3):** sent = 160 x (score / 100) x (1 + enchant%) + class damage + level damage, x charge x
crit. **His hand holds the Ability Stone**, which carries no score stamp: `GearScore.orAbsent` gives **ABSENT = 100**
(READ: `StoneItems` writes no score; `GearScoreItems.heldScore`). Enchant **0** (the stone has none), class damage **0**
(his armour is emptied by the setup and his accessory slots by the pad step), level damage **0** (a stone cast is not a
weapon hit), charge **1.0**. **A crit CAN occur: `Crit.BASE_CHANCE` 0.15, rolled ONCE per cast and frozen across its four
pulses; `Crit.multiplier(0.15, 1.0, roll)` = 2.0.** So every pulse of one cast reads **`sent=160.000 crit=false`, or
every one reads `sent=320.000 crit=true`**, and the `triggerScore=100` on every PLAYERHIT. A zombie's defence is **0**
(READ: `HealthState`'s defence is `Stat(0.0)` and only `PlayerHealthSystem` reconciles a modifier), so it loses `sent`.

**Scorch from it, by the element (A1):** cap = (sent / crit) x 0.5 = **80 either way**; window 120, reset by each hit;
each burn **min(5% of 3000 = 150, 80) = 80**, defence bypassed, credited to him.

**Flint Staff (WA1, WA2, WA6):** 20 x (100 / 100) (`rpg gearscore set 100`), enchant 0 (`rpg enchant clear`), class 0,
**level damage 0** (`LevelBonus.at(1).weaponDamage()` = 0.0, executed), charge 1.0: **`sent=20.000 crit=false` or
`sent=40.000 crit=true`**, each bolt its own roll. Cap **10**; each burn on a 1000 zombie **min(50, 10) = 10**.

## THE ROWS

### SM1 — THE BUILD SCREEN, A3, AND THE PICK

- The first `dump: menu` is the Build screen of whatever cell he has now (not predicted: it is his).
- **After Mage and Fire (the second dump), A3's reading, ONE of three, named by which:** (i) he has no saved mage/fire
  build: the Q cell reads **`Q: Smolder`**, lore `Ultimate -- the default`; (ii) a saved build names the deleted
  placeholder: **`Q: (empty)`**, and the log carries ONE **`BUILD unoffered player=<him> cell=mage/fire slot=ULTIMATE
  id=ultimate_placeholder_mage reads=empty kept=file`** (and no error); (iii) a saved build already names smolder:
  `Q: Smolder`, lore `Ultimate`. Whichever it is IS the reading of A3 on his real file.
- **The picker's Smolder (the third dump):** `Smolder`, lore **`Four pulses, 3s apart, burst from your feet.`**, **`Each
  deals 160 fire damage within 12 blocks and scorches.`**, **`Cooldown: 300s`**: no mana (`BuildMenu.abilityLore`).
- **The Build screen after the pick (the fourth dump):** **`Q: Smolder`**, lore `Ultimate`, the two description lines,
  **`Damage: 160`**, **`Cost: 80`**, **`Cooldown: 300.0 s`** (executed: `AspectLore.lines`), a blank, `Click to change.`
- `STAGE stone slot=<his stone slot> found=0 held=1` (the row cleared his storage), then `HELD … ` the Ability Stone.

### SM2 — THE FOUR PULSES

- Before: `PROBE` a3, a6, a11, a12 each **`customHP=3000.000/3000.000 fireTicks=0`**; `SUMMONPROBE` his wolf, alive.
- **Mana:** `MANA current=100.000` then, in the press's tick, **`current=20.000`**: 80, once.
- **`SMOLDER cast caster=<him> tick=C`**; **`SMOLDER pulse n=1 tick=C`**, **`n=2 tick=C+60`**, **`n=3 tick=C+120`**,
  **`n=4 tick=C+180`**, each **`origin=`** his feet (the same x y z four times: he does not move).
- **Hits, each pulse:** a3 at the pulse's tick **+8** `dist=3.000`, a6 **+15** `dist=6.000`, a11 **+29**
  `dist=11.500`: twelve `SMOLDER hit` lines, each `sent=160.000` (or all twelve 320.000) and its PLAYERHIT `source=smolder
  element=fire … crit=<false|true> triggerScore=100`. **No line names a12. No line names the wolf.**
- **Burns:** each hit zombie, fifteen `DOTTICK … status=scorch applier=<him> sent=80.000 cap=80.000`, the first at its
  first hit + 19 and every 20 to + 299 (a3 C+27 to C+307, a6 C+34 to C+314, a11 C+48 to C+328).
- After (C+340): a3, a6, a11 **`customHP=1160.000/3000.000`** (crit: **520.000**), `fireTicks=0`; a12
  **`3000.000/3000.000`**; the wolf's `SUMMONPROBE` health the same as before.

### SM3 — SCORCH FROM THE ELEMENT

- b6: `3000.000/3000.000 fireTicks=0`; the cast; hits at **C+15, C+75, C+135, C+195**.
- At C+17: **`customHP=2840.000`** (crit: **2680.000**) and **`fireTicks` lit** (117 to 119: the probe's phase against the
  vanilla countdown is not READ; positive is the reading).
- **Fifteen `DOTTICK … sent=80.000 cap=80.000`, C+34 to C+314, every 20** (min(5% of 3000, 80), the window restarted by
  each hit and run out 119 after the last).
- At C+340: **`1160.000/3000.000`** (crit: **520.000**) = 3000 - 4 x 160 - 15 x 80 (crit: - 4 x 320), executed.

### SM4 — THE ORIGIN MOVES, THE RING DOES NOT

- **Pulse 1 `origin=` F; at C+40 he is moved 8 east (`SELF` at F+8); pulses 2, 3, 4 `origin=` F+8.**
- c11: ONE hit, **`n=1` at C+28 `dist=11.000`**; none from pulses 2 to 4 (13.601 from F+8). c19: **`n=2` at C+88, `n=3`
  at C+148, `n=4` at C+208, `dist=11.000`**; none from pulse 1 (19 from F).
- Burns: c11 six, C+47 to C+147; c19 twelve, C+107 to C+327; each 80.
- At C+340: c11 **`2360.000`** (crit: **2200.000**), c19 **`1560.000`** (crit: **1080.000**), executed.

### SM5 — CANCEL

- d6 hit at **C+15 (`n=1`)** and **C+75 (`n=2`)**; at C+90, `clear_state` writes **`SMOLDER cancelled caster=<him>
  remaining=2 cause=selftest tick=C+90`**; **no `n=3`, no `n=4`, no third hit.**
- Nine burns, C+34 to C+194. At C+240: **`1960.000`** (crit: **1640.000**). (His death ending the run is the unit
  table's, `PulseRemovalWiringTest` and `PulseCastTest`; not staged.)

### SM6 — THE COOLDOWN AND THE COST

- Mana `100.000`; the press at C: `SMOLDER cast`, mana **`20.000`**.
- **The press at C+2: refused.** **`REPLY actionbar Smolder -- ready in 299.9s`** (the stone's own notice,
  `StoneNotice.onCooldown`: 5998 ticks / 20; this is the cooldown read at 6000 from the cast), mana still `20.000`, no
  second `SMOLDER cast`.
- At C+22 `clear_state`: **`SMOLDER cancelled caster=<him> remaining=3 cause=selftest tick=C+22`** (pulses 2 to 4 of the
  first cast) and the cooldown cleared. `rpg mana set 79`: `79.000`.
- **The press: refused.** **`REPLY actionbar Smolder -- needs 80 mana, you have 79`** (`StoneNotice.notEnoughMana`),
  mana still `79.000`, no `SMOLDER cast`.

### WA1 — WATER (A) AT THE APPLICATION

- `PROBE_BLOCKS` the cell's centre is WATER. w1, w2 each `1000.000/1000.000 fireTicks=0`.
- **w1 (in water), the bolt:** PLAYERHIT `sent=20.000` (or 40.000) and **`SCORCH refused target=zombie <w1> cause=water
  tick=<its hit>`**; **no DOTTICK names w1, ever**; w1 `980.000` (crit `960.000`) **`fireTicks=0` at both later probes:
  no flame (A4: a refusal sets none).**
- **w2 (dry), the bolt:** PLAYERHIT `sent=20.000` (or 40.000); six `DOTTICK … sent=10.000 cap=10.000`, its hit + 19 to
  + 119; 10 ticks after the press `fireTicks` lit; at the last probe **`920.000`** (crit **`900.000`**), `fireTicks=0`.

### WA2 — WATER (A), DOUSED AFTER

- v dry: the bolt (P); its first `DOTTICK … sent=10.000` at its hit + 19 (the hit a few ticks after P); at P+30 **lit,
  `970.000`** (crit `950.000`) = 1000 - the hit - that one burn.
- **v moved into the water at P+30. At P+35, before its second burn tick: `fireTicks=0` and no SCORCH line yet:
  vanilla's own extinguish (A4's "before": the flame out in water while the burn is still live).**
- **At its hit + 39: `SCORCH doused target=zombie <v> cause=water tick=…` and NO DOTTICK that tick, none after.**
- At P+165: **`970.000`** (crit `950.000`), unchanged since P+30, `fireTicks=0`.

### WA3 — SMOLDER AND WATER

- u (in water at 6.708): hits at **C+17, C+77, C+137, C+197**, `dist=6.708`, `sent=160.000` (or all 320.000); with each,
  **`SCORCH refused target=zombie <u> cause=water`**; **no DOTTICK names u**; at C+230 **`2360.000`** (crit
  **`1720.000`**), `fireTicks=0`.

### WA6 — IGNITE AND WATER, THE THREE BODIES (E1, E7)

- Before: k1, k1w, k2w, k3w `1000.000/1000.000`; k2, k3 `10.000/10.000`; all `fireTicks=0`.
- **k1, dry and scorched, then killed by a NON-fire hit (clause 1):** the bolt (PLAYERHIT `sent=20.000` or 40.000; its
  DOTTICKs `sent=10.000`); 61 ticks later `rpg mobdamage 2000` kills it (`dead=true` at the probe 30 later). No `IGNITE
  refused`. **Its blast 20 ticks after the death: k1w `994.000`** (Ignite.DAMAGE 6, defence 0, no stamp; read before
  k1w's own first burn at the blast + 19).
- **k2, in water, killed by ONE fire bolt (clause 2, wet):** PLAYERHIT `sent=20.000` (or 40.000), k2 `dead=true`, and
  **`IGNITE refused target=zombie <k2> cause=water tick=<its death>`**. No SCORCH line (a killing hit accrues nothing).
  **k2w `1000.000` at both of its probes: no blast.**
- **k3, dry, killed outright by ONE fire bolt with no time to scorch (clause 2, the control):** PLAYERHIT, k3
  `dead=true`, no IGNITE line; **its blast: k3w `994.000`** 38 ticks after the press.
- Every blast term: `Ignite.DAMAGE` 6 x (1 + 0 / 100) (no Mana Burn stamp), `DefenseRule.APPLIES` on a defence of 0,
  radius 4 (k1w 1.5 from k1, k3w 1.5 from k3, k2w 2.5 from k2; k2w 10.5 from k1 and 5.5 from k3), executed.

### SM7 — THE ASK (THE COMMAND'S ONE QUESTION)

- The cast at C; he is moved 8 east at C+40; the question at C+220, after the fourth ring has gone out (C+180 to C+210).
- SEAT PLACEHOLDER wording: **"Did each ring of fire spread out from your feet to about 12 blocks, and did the first
  ring stay where it started when you were moved? (yes / no / not sure)"**. Answer logged verbatim; no answer is NOT
  STATED.

## OWED, AND WHY (rain, splash, death)

- **WA4, RAIN (B): OWED to the unit table.** The run cannot capture and restore the world's weather (READ: no weather
  field in `SelfTestSnapshot`), and the pad's column was not READ to be rained on (its biome's precipitation). Held by
  `DouseTest` (the wet decision with rain) and `ScorchDouseTest` (`cause=rain` at a burn tick).
- **WA5, SPLASH (D): OWED to the unit table.** No existing verb throws a splash water bottle, and a plain summon of one
  was not READ to the depth staging needs (the thrown potion's item component on 26.1.2, and that an ownerless potion's
  hit reaches `onHitAsWater`). Held by `ScorchDouseTest.aSplashDouses…` and `DouseWiringTest.aSplashDousesTheServers…`.
- **His death ending the pulses:** the unit table (`PulseRemovalWiringTest`, `PulseCastTest`); a death is not staged.
- **A cauldron (E4):** no row; a cauldron does not douse by ruling, READ by javap (PLAN-smolder.md section 6.1).

## CC'S CALLS (the gate's)

1. **The `stone:` verb**: the Ability Stone converged into his hotbar by the join's own call, because `clear_inventory`
   empties it and nothing but a join or a respawn mints it again. The alternative: Smolder rows that do not clear his
   storage (his own items then sit in the inventory the row reads).
2. **SM1 stages the cell by the Build screen's own clicks** (as `GATE-melee-cell.md` MC1 and `GATE-sunder.md` SU1),
   because the `class:` verb keeps his element; every later row is skipped unless the cell reads mage fire.
3. **Zombies at max_health 600 (3000 custom)**: above every Smolder row's whole damage including a crit (SM2's 2480).
4. **WA1's dry control is a separate bolt, 31 ticks after the first** (the staff's 24-tick cooldown), not the same
   bolt.

## OPEN FOR BEN (every placeholder in this stack)

- Z2: the damage travels with the ring (the alternative: every body in range hit at the pulse's tick).
- The ring's particle and the pulse's sound; the description wording; SM7's question wording.
- A3: a saved build shows an empty ultimate until he picks Smolder.
- Y3: a wet body gains no new Scorch. E2: a splashed body killed by fire still ignites. E3: an armed detonation is not
  cancelled by water. E4: a cauldron puts out the flame (vanilla) but not the burn.
- K5 (E5): whether an Ability Stone cast, Smolder included, should end the Blink Tome's invisibility.

## WHAT ONLY THE BOOT CAN SHOW

- The ring as he sees it (spacing, height, speed, the 1.5 s to the edge), and the pulse's sound: SM7's question.
- The pulses arriving on their ticks on a real entity scheduler, and the burn's first tick at + 19 on a real server.
- The douse on a real body in real water, and the death frame's wet reading.

## R0b, RUN BEFORE COMMIT

On `paper/target/rpg-0.1.0-SNAPSHOT.jar` built from this tree (2026-11-27 19:35, `./mvnw -q package -DskipTests`,
before the commit): **21 `PRESENT` / 0 `ABSENT`, then 3 `ABSENT` / 0 `PRESENT`, then 3 `PRESENT` / 0 `ABSENT`, as
predicted.** The first run read the A1 needle `status_id` PRESENT: smolder.yml's own comment names the key while saying
it is not authored (a false presence, prose matched as the key). It was re-anchored to the list syntax `- type: status`,
and a control with the same form (`- type: damage`, PRESENT) was added. This is a reading of a jar on this machine, not
of the deployed one: R0b proper is the deployed jar's.
