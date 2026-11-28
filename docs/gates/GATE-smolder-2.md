# GATE — SMOLDER AS BEN CHANGED IT (M1 TO M4) AND THE SEAT'S X6, FIRST IN THE ONE COMMAND (2026-11-28)

**Status: NOT RUN.** The plans are `docs/plans/PLAN-smolder.md` section 8 (M1 to M4), `PLAN-owed-fixes.md` (X6) and
`PLAN-solar-flare.md` section 6 (N1 to N7). The scenario is `paper/src/main/resources/selftest/GATE-smolder-2.yml`. The
stack, bottom up: master `88858c40`, #342 `feat/x6-clock`, #343 `feat/smolder-spiral`, #344 `feat/solar-flare-volley`,
then the ONE gate branch, `feat/ultimates-2-gate`, last, carrying this gate and `GATE-solar-flare-2.md`. **Every
prediction below was written before any boot, and is frozen from the seat's approval.** The frozen gates
(`GATE-smolder.md`, `GATE-solar-flare.md`) are not edited: their rows that changed are re-predicted here under their old
names with the next suffix (R). Every figure is EXECUTED by `UltimatesTwoGateArithmeticTest` (S-1) on the server's order
as READ (`ScorchBurnsReadOrder`, controlled by the last boot's own SM2 and SM5 readings); the queue, the lint, the two
asks, the summons and the view steps by `UltimatesTwoGateCommandTest`.

**GAME MODE: SURVIVAL**, every row of both gates.

## WHAT BEN TYPES — ONE COMMAND, FOR BOTH GATES

```
/rpg selftest smolder-2+solar-flare-2
```

- This gate's setup and eight rows, then `GATE-solar-flare-2.md`'s setup and ten: 20 scenarios
  (`UltimatesTwoGateCommandTest`). Both setups begin with `pad:`, so V7 (e) refuses neither.
- **He answers TWO questions, one per gate: SM7R (Smolder's spirals), then SF9R (Solar Flare's fireballs), the command's
  last step.** Each waits 3600 ticks; each row's tell line asks him to **keep his mouse still**.
- He types it on the pad's server, standing anywhere; the first setup takes him to the pad.
- **SM1R picks Smolder into his Fire Mage cell and SF1R Solar Flare into his Fire Ranger cell; the run puts his stored
  builds back whole at its end** (the seat's D2, #341).

**How long it runs (an ESTIMATE from the scenarios' `wait:` steps, not a measurement):** this gate's waits sum to **1462
ticks** (setup 40, SM1R 23, SM2R 285, SM3R 271, SM4R 281, SM5R 211, SM6R 66, WA6R 134, SM7R 151), Solar Flare's to
**1476** (`awk` over each file's `- wait:` lines per row, 2026-11-28): **2938 ticks, about two and a half minutes of
rows, then the two questions.**

## SAFETY — 0.000 FOR THE WHOLE COMMAND

- **No step damages him:** no `rpg damage`, no `damage`, no `kill @s` in the queue (asserted).
- **Every mob is a frozen NoAI zombie** (10 here and 7 in `GATE-solar-flare-2.md`, every one `NoAI:1b` and tagged
  `selftest`, asserted); his wolf is made NoAI by its row.
- **Smolder's pulses and Solar Flare's bursts cannot reach him. EXECUTED:** `MobHostility.harms(ENGINE_ATTACKER, PLAYER,
  false, PvpPath.PULSE)` = **false**; and a front skips its caster by id (READ, `CastExecutor.ringPass`).
- **Ignite's blast cannot reach him. EXECUTED:** `harms(ENGINE_ATTACKER, PLAYER, false, IGNITE_SPLASH)` = **false**;
  WA6R's blast is at F + (6, −8), 10 from him besides.
- **His wolf is refused by a front. EXECUTED:** `harms(ENGINE_ATTACKER, SUMMON, false, PULSE)` = **false**.
- **N4, the firework, is ROUTE B: particles and sounds, NO firework entity** (`PLAN-solar-flare.md` 6, N4: a real
  firework's explosion hurts every living body within 5, players included, and its zero cannot be proven by S-1). So
  nothing in a burst can wound him but the burst's own front, which spares him (above). **`GATE-solar-flare-2.md` SF11
  READS it:** three bursts 1.5 blocks in front of him, NO `HURT` line, his health unchanged. Route A's worst-case sum is
  not owed: no firework entity exists.
- **A fireball cannot strike him:** the trace excludes its caster by id (READ, `LiveTargets.except`), and every shot
  flies away from him.
- **The sum: this gate 0.000, `GATE-solar-flare-2.md` 0.000: 0.000**, at or under 20.000.

## CARRIED STATE — THE BOOTER'S CHECKLIST FOR THE WHOLE COMMAND, IN THIS ORDER (`.claude/rules/booting.md`)

1. The seat's approval of the code AND both gates from origin; Ben's own "ready".
2. **The heads:** `git ls-remote --heads origin` for master, `feat/x6-clock`, `feat/smolder-spiral`,
   `feat/solar-flare-volley` and `feat/ultimates-2-gate`, equal to what the seat diffed; CI green on each, bound by
   `headSha`. `git status --porcelain` prints nothing and `git rev-parse HEAD` equals `feat/ultimates-2-gate`'s head.
3. **No JVM:** `Get-CimInstance Win32_Process -Filter "Name='java.exe'"` counts 0.
4. **The backup**, to a NEW folder `C:\Users\Neb91\IdeaProjects\rpg-run-backups\<date>-pre-ultimates-2`: `run\world`,
   `run\plugins\Rpg` with their file counts and copy counts, his player `.dat` with its bytes, **his build file's bytes
   and hash and his profile's `archetypeId` and element quoted before the boot.** SM1R and SF1R pick into his mage/fire
   and ranger/fire cells, and **the run puts his stored builds back whole** (`CAPTURED … builds=[…]` and `RESTORED …
   builds=[…]` equal, no `NOT-RESTORED=builds(…)`). **His build file's bytes before the boot EQUAL its bytes after
   /stop.**
5. **The world's saved difficulty,** read from the decompressed `level.dat`, before and after the run.
6. **Config:** `dev-pack-on-join: true`. No `run\rpgfight`.
7. **"BOOTING NOW", in its own message, BEFORE the boot command**, with: "Ben: do NOT log in until I tell you R0 passed."
8. `./scripts/dev-server.sh --refresh-content` (it clears the deployed content, so the two retired ring files are not
   left to be named); the `Done` matched to a log newer than the deployed jar.
9. R0 read on this boot's log; **"R0 PASSED" in its own message, before he logs in.** His first `joined the game` time
   reported beside R0's.
10. After his `/stop`: the clean-stop lines, no `java` process left; his build file and profile read again (step 4).

**Weather is not captured by the run** (READ: `SelfTestSnapshot` has no weather field), so no row changes it (X6 (d)).

## R0 — THE BUILD, FOR THE WHOLE COMMAND. IF ANY R0 FAILS, STOP

- **R0a:** `[Rpg] Build: <sha>`, `feat/ultimates-2-gate`'s head as `git ls-remote --heads origin feat/ultimates-2-gate`
  prints it, shortened. Not `-dirty`, not `unknown`.
- **R0b:** `docs/gates/GATE-smolder-2-r0b.ps1` on the DEPLOYED jar: before `'--- absent'` **21 `PRESENT`, 0 `ABSENT`**;
  between `'--- absent'` and `'--- controls'` **4 `ABSENT`, 0 `PRESENT`**; after `'--- controls'` **2 `PRESENT`, 0
  `ABSENT`**. Each `Has` and `ValueLine` needle was checked ABSENT in master `88858c40`'s main sources first (`git grep
  -F`, 0 files each; the value lines by an exact line match). **Run before commit on a jar built from this tree: the
  bottom of this file.**
- **D2's lines:** `SELFTEST RUN CAPTURED … builds=[…]` and `SELFTEST RUN RESTORED … builds=[…]`, the two `builds=`
  fields EQUAL, no `NOT-RESTORED=builds(…)`.
- **R0c:** `Loaded 11 abilities, 52 visuals, 5 statuses, 7 elements, 28 enchants, 3 pools, 6 fragments, 6 aspects, 27
  weapons, 1 shields, 28 armor, 5 tools, 8 accessories, …` (visuals 52 = 51 + `smolder_spiral` + `solar_flare_burst` −
  `smolder_ring`, whose rename `solar_flare_ring` is deleted too: `ls content/visuals/*.yml | wc -l`) and
  `Mint-on-craft: 42 indexed, 42 claiming, of 61 gear definitions`, unchanged. **0** `WARN]: [Rpg]` (control: `INFO]:
  [Rpg] Loaded` reads 1 or more); none of `Refusing`, `Skipping`, `SEVERE`, `Exception`. Control: `51 visuals` ABSENT.
- **R0d:** exactly one **`[Rpg] Selftest: dev server, 580 scenarios across 61 gates [melee-cell, …, solar-flare,
  smolder-2, solar-flare-2]`**. Recount: 61 files, 641 keys (`cat GATE-*.yml | grep -cE '^[A-Za-z0-9_-]+:'`), 61
  setups, so 580. Control: 621 + 9 + 11 = 641. Controls: `562 scenarios` ABSENT, `59 gates` ABSENT.
- **R0e:** one `FIGHTWORLD sweep path=…\rpgfight folders=0`, no `deleting`, no `orphan`.

## THE CLOCK (every tick prediction names it; X6 (a), `PLAN-owed-fixes.md` X6 AS BUILT)

- **ONE CLOCK.** Every line read here prints `Bukkit.getCurrentTick()`: the selftest's own lines (`MANA`, `DRIVE`,
  `STAGE`, `PROBE`), the `SMOLDER`/`SOLAR_FLARE` lines, `DOTTICK`, the `SCORCH` lines.
- **P** is a row's `drop: selected`: the selftest's step tick (its `DRIVE drop` and the `MANA` read in the same pass).
  The stone decides there (cooldown, mana, his aim), and the cast runs one tick later: **C = P + 1** (the `SMOLDER cast`
  line). The last boot read exactly this (`MANA … tick=6110`, `cast tick=6111`).
- **Pulse n at C + 30 (n − 1), EXACTLY** (`feat/x6-clock`'s settle hop; the last boot read pulse 2 one tick early).
- **A hit at its pulse's tick + the reach tick** of the body's horizontal distance d: the first whole t in 0..15 with
  12 t / 15 >= d (`PulseFront.reachTick`, executed per body below).
- **The first burn exactly 20 after a body's first hit, then every 20** (the last boot read this). **On a tick with a
  later hit and a burn, the hit's line first, the burn BEFORE the hit's refresh** (READ: the mob's due list runs in
  insertion order). Every burn count below runs through the real store on that order.
- **Every act that reads his view is preceded, one tick before, by a step that sets it** (X6 (b)): `tp @s ~ ~ ~ -90 0`
  (or `aim:`). So every `DRIVE drop` and `DRIVE use` below reads **`yaw=-90.000 pitch=0.000`** (or the aim's), unless
  he moves his mouse in that one tick; a different yaw or pitch on the line is the reading that says so.

## THE COORDINATE TABLE (x east, z south; every zombie at his y; executed by `UltimatesTwoGateArithmeticTest`)

**Every row that casts Smolder stands him at G = F + (10, 0), block-centred, facing east** (WA6R casts no pulse and
stands at F). The interior is x −2.5 .. 45.5, z −13.5 .. 13.5 from F; this gate's preflight `-2 -12 30 12` is x −2.5 ..
30.5, z −12.5 .. 12.5.

| row | body | from his stand | from F | horizontal from the pulse's origin | reach tick | max health |
|---|---|---|---|---|---|---|
| SM2R | a3, a6, a11, a12 | G + (3, 0), (6, 0), (11.5, 0), (12.5, 0) | (13, 0), (16, 0), (21.5, 0), (22.5, 0) | 3, 6, 11.5, 12.5 | 4, 8, 15, never | 3000 |
| SM2R | his wolf | G + (0, 4) | (10, 4) | 4 | (5; refused by the table) | his summon |
| SM3R | b6 | G + (6, 0) | (16, 0) | 6 | 8 | 3000 |
| SM4R | c11 | G + (0, −11) | (10, −11) | 11 from G; 13.601 from G + 8 | 14 (pulse 1); never | 3000 |
| SM4R | c19 | G + (19, 0) | (29, 0) | 19 from G; 11 from G + 8 | never; 14 (pulses 2 to 4) | 3000 |
| SM4R, SM7R | him, moved | G + (8, 0) at P+19 (C+18) | (18, 0) | | | |
| SM5R | d6 | G + (6, 0) | (16, 0) | 6 | 8 | 3000 |
| WA6R | k1, k1w | F + (6, −8), (7.5, −8) | the same | (a bolt; Ignite's 4; k1w 1.5 from k1) | | 1000 |

| origin (from F) | used by | x reach (origin ± 12) | z reach | inside the interior | inside the preflight |
|---|---|---|---|---|---|
| G = (10, 0) | every pulse 1, and every pulse of SM2R, SM3R, SM5R, SM6R | −2 .. 22 | −12 .. 12 | yes | yes |
| G + 8 = (18, 0) | pulses 2 to 4 of SM4R and SM7R | 6 .. 30 | −12 .. 12 | yes | yes (30 <= 30.5) |

WA6R's Ignite blast (radius 4) at F + (6, −8) reaches x 2 .. 10, z −12 .. −4: inside both.

## EVERY TERM OF A HIT

**Smolder (every SM row):** sent = 160 x (score / 100) x (1 + enchant%) + class damage + level damage, x charge x crit.
His hand holds the Ability Stone: no score stamp, so **ABSENT = 100**; enchant **0**; class damage **0** (armour emptied
by the setup, accessory slots by the pad step); level damage **0** (a stone cast is not a weapon hit); charge **1.0**.
**A crit CAN occur: `Crit.BASE_CHANCE` 0.15, rolled ONCE per cast and frozen for its four pulses; x 2.0.** So every hit
of one cast reads **`sent=160.000`**, or every one **`sent=320.000`**. A zombie's defence is **0**. **Scorch by the
element:** cap (sent / crit) x 0.5 = **80** either way; window 120, restarted by each hit; each burn **min(5% of 3000 =
150, 80) = 80**.

**Flint Staff (WA6R):** 20 x (100 / 100) (`rpg gearscore set 100`), enchant 0 (`rpg enchant clear`), class 0, level
damage 0 (level 1), charge 1.0: **`sent=20.000` or `40.000`**, cap **10**, each burn on a 1000 zombie **min(50, 10) =
10**. **Ignite:** `Ignite.DAMAGE` 6 x (1 + 0 / 100), defence 0: **994.000** on a 1000 zombie.

## THE ROWS

### SM1R — THE BUILD SCREEN AND THE PICK (re-predicts SM1 for N7's first line)

- The first `dump: menu` is his current cell (not predicted). After Mage and Fire, ONE of three, named by which: (i) no
  saved mage/fire build: **`Q: Smolder`**, lore `Ultimate -- the default` (the last boot's CAPTURED string had no
  mage/fire cell, so (i) is expected); (ii) a saved build naming the deleted placeholder: **`Q: (empty)`** and one `BUILD
  unoffered … slot=ULTIMATE id=ultimate_placeholder_mage reads=empty kept=file`; (iii) a saved build naming smolder:
  `Q: Smolder`, lore `Ultimate`.
- **The picker's Smolder:** `Smolder`, lore **`Four pulses, 1.5s apart, burst from your feet.`**, **`Each deals 160 fire
  damage within 12 blocks and scorches.`**, **`Cooldown: 300s`**, and the `"Current" [green]` line the last boot read
  (unpredicted then).
- **The cell after the pick:** `Q: Smolder`, `Ultimate`, the two lines, **`Damage: 160`**, **`Cost: 80`**,
  **`Cooldown: 300.0 s`**, a blank, `Click to change.` Then `STAGE stone … found=0 held=1` and `HELD` the stone.

### SM2R — THE FOUR PULSES, 30 APART (M1; re-predicts SM2)

- Before: `PROBE` a3, a6, a11, a12 each **`customHP=3000.000/3000.000 fireTicks=0`**; `SUMMONPROBE` his wolf, alive.
- The whistle's press reads `DRIVE use … yaw=-90.000 pitch=0.000`.
- **Mana:** `MANA current=100.000` (P − 1), then in the press's pass **`current=20.000 … tick=P`**: 80, once.
  **`DRIVE drop … yaw=-90.000 pitch=0.000`.**
- **`SMOLDER cast … tick=C`**; **`SMOLDER pulse n=1 tick=C`**, **`n=2 tick=C+30`**, **`n=3 tick=C+60`**, **`n=4
  tick=C+90`**, each `origin=` his feet at G (the same x y z four times).
- **Hits, each pulse:** a3 at the pulse's tick **+4** `dist=3.000`, a6 **+8** `dist=6.000`, a11 **+15** `dist=11.500`:
  twelve `SMOLDER hit` lines, each `sent=160.000` (or all twelve `320.000`), each with its `PLAYERHIT source=smolder
  element=fire … triggerScore=100`. **No line names a12. No line names the wolf.**
- **Burns (EXECUTED, ten each):** `DOTTICK … status=scorch applier=<him> sent=80.000 cap=80.000`, a3 **C+24 … C+204**,
  a6 **C+28 … C+208**, a11 **C+35 … C+215**, every 20.
- After (P+230 = C+229): a3, a6, a11 **`customHP=1560.000/3000.000 fireTicks=0`** = 3000 − 4 x 160 − 10 x 80 (crit:
  **`920.000`** = 3000 − 4 x 320 − 10 x 80); a12 **`3000.000/3000.000`**; the wolf's `SUMMONPROBE` as before.

### SM3R — SCORCH FROM THE ELEMENT (re-predicts SM3)

- b6 `3000.000/3000.000 fireTicks=0`; the cast at C; hits at **C+8, C+38, C+68, C+98** (`dist=6.000`).
- At P+10 (C+9): **`customHP=2840.000`** (crit **`2680.000`**) and **`fireTicks` positive** (lit; its value against the
  vanilla countdown is not predicted).
- **Ten `DOTTICK … sent=80.000 cap=80.000`, C+28 … C+208, every 20.**
- At P+230: **`1560.000/3000.000 fireTicks=0`** (crit **`920.000`**).

### SM4R — THE ORIGIN MOVES, THE FRONT DOES NOT (re-predicts SM4)

- **Pulse 1 `origin=` G at C; at P+19 (C+18) he is moved 8 east (`SELF` at G + 8 = F + 18); pulses 2, 3, 4 `origin=`
  G + 8, at C+30, C+60, C+90.**
- c11: **ONE hit, `n=1` at C+14 `dist=11.000`**; none from pulses 2 to 4 (13.601 from G + 8). c19: **`n=2` at C+44,
  `n=3` at C+74, `n=4` at C+104, `dist=11.000`**; none from pulse 1 (19 from G).
- Burns (EXECUTED): c11 **six**, C+34 … C+134; c19 **nine**, C+64 … C+224; each 80.
- At P+240 (C+239): c11 **`2360.000`** = 3000 − 160 − 6 x 80 (crit **`2200.000`**); c19 **`1800.000`** = 3000 − 3 x 160 −
  9 x 80 (crit **`1320.000`**).

### SM5R — CANCEL (re-predicts SM5, X6 (c))

- d6 hit at **C+8 (`n=1`)** and **C+38 (`n=2`)**; at P+45, `clear_state` writes **`SMOLDER cancelled caster=<him>
  remaining=2 cause=selftest tick=P+45`** (C+44); **no `n=3`, no `n=4`, no third hit.**
- **Seven burns (EXECUTED), C+28 … C+148** (the burn at C+28 runs; pulse 2's hit at C+38 shares no tick with a burn).
  At P+170: **`2120.000`** = 3000 − 2 x 160 − 7 x 80 (crit **`1800.000`**).

### SM6R — THE COOLDOWN AND THE COST, THE REGEN TERM NAMED (re-predicts SM6, X6 (c))

- `MANA current=100.000` (P − 1); the press at P: `SMOLDER cast` at C; **`MANA current=20.000 … tick=P`**.
- **The press at P+2: refused.** **`REPLY actionbar "Smolder -- ready in 299.9s"`** (5998 ticks / 20), and **`MANA
  current=20.100 … tick=P+2`** = 20 + 2 x **0.05**, the regen term (READ: `MANA_PER_TICK = MAX_MANA /
  (MANA_REFILL_SECONDS x 20)` = 100 / (100 x 20); the pool is lazily integrated from the spend; EXECUTED through the real
  `ResourcePool`). No second `SMOLDER cast`.
- At P+22 `clear_state`: **`SMOLDER cancelled caster=<him> remaining=3 cause=selftest tick=P+22`** (pulse 2 is not due
  until C+30 = P+31) and the cooldown cleared.
- At P+25, in one pass: the view step's tick has passed, `rpg mana set 79`, **`MANA current=79.000`**, the press
  **refused: `REPLY actionbar "Smolder -- needs 80 mana, you have 79"`**, **`MANA current=79.000`** again (same tick,
  so no regen term), no `SMOLDER cast`.

### WA6R — IGNITE CLAUSE 1, ITS VIEW SET (re-predicts WA6's clause 1, X6 (c))

- Before: k1, k1w **`1000.000/1000.000 fireTicks=0`**.
- `aim k1`, one tick, **the bolt** (Q, the press): `DRIVE use … yaw=<the aim's> pitch=<the aim's>`; PLAYERHIT
  **`sent=20.000`** (or `40.000`) on k1, a few ticks after Q (its flight; not predicted to the tick); k1's DOTTICKs
  **`sent=10.000 cap=10.000`** from its hit + 20, every 20.
- At Q+61: k1 alive, **scorched: `fireTicks` positive**, `customHP` = 1000 − 20 − 10 x (its DOTTICKs logged so far);
  k1w `1000.000`.
- **`aim k1`, ONE TICK, then `rpg mobdamage 2000`: the line before it is `DRIVE look yaw=… pitch=…` (the aim's), and NO
  `"Look at a mob."` reply.** k1 dies (2000 is more than it has left). **No `IGNITE refused`** (it is dry).
- **Its blast (clause 1: armed at the death, Ignite's 20-tick fuse): at the probe 30 ticks after the command, k1w
  `994.000`** (Ignite.DAMAGE 6, defence 0; read before k1w's own first burn, which would land 20 after the blast). k1:
  `dead=true`, or `none within 16 blocks` (removed on death), either one the reading of its death.

### SM7R — THE ASK (this gate's one question)

- The cast at C (view set at P − 1), pulses at C, C+30, C+60, C+90; he is moved 8 east at P+19, after pulse 1's front
  has reached its edge (C+15); pulses 2 to 4 from G + 8. The question at P+130 (C+129), after the fourth front (C+90 to
  C+105) and its spirals' 8-tick fade.
- The tell line: "Smolder is cast in a moment from your feet. Keep your mouse still and stand still; you will be moved 8
  blocks east after the first pulse. Watch the fire spread out."
- SEAT PLACEHOLDER wording: **"Did three spiral arms of fire wind out from your feet to about 12 blocks, and did they
  fade quickly? (yes / no / not sure)"**. Answer logged verbatim; no answer is NOT STATED.

## X6 IN THIS GATE

- **(a)** every tick above is on the corrected clock (pulse 2 at C+30, not C+59; first burns at hit + 20).
- **(b)** every press, drop and look-reading command has its view set one tick before it (the `VIEW_UNSET` lint, which
  every NEW gate must pass: `ViewUnsetLintTest`; the 32 gates it found when written are named and frozen); the drop line
  and a `DRIVE look` line print the yaw and pitch read; both ask rows' tell lines ask him to keep his mouse still.
- **(c)** WA6R (clause 1), SM5R, SM6R's mana (here) and SF7R's (`GATE-solar-flare-2.md`) are re-predicted from executed
  figures.
- **(d) NOT BUILT, and why:** rain (no weather field in the run's snapshot: a capture-and-restore of the world's weather
  is its own change to `SelfTestSnapshot`), the splash bottle (no verb throws one; READ owed on the thrown potion's
  component), and his death ending the pulses and shots (a death is not staged on the pad: SAFETY). Each stays on the unit
  table: `DouseTest`, `ScorchDouseTest` (rain, splash), `PulseRemovalWiringTest` and `PulseCastTest` (his death ends
  pending pulses; for the salvo, `SolarFlareCastTest`'s two cancel rows exercise the same removal path).

## CC'S CALLS (this gate's)

1. **One gate per ultimate, one command** (the alternative, one gate file holding both: two asks in one file would break
   "at most ONE ask per gate").
2. **The view step is `tp @s ~ ~ ~ -90 0`** (sync, applied in its own tick) rather than `aim:` where no mob is the
   target; `aim:` (WA6R) where one is.
3. **The `VIEW_UNSET` lint's predating list** (32 gates, frozen readings, by name), as `PAD_FIRST`'s is; the
   alternative, re-staging 32 frozen gates, edits gates that have been read.
4. **WA6R re-reads clause 1 alone** (k1, k1w): clauses 2 and 3 read as predicted.

## OPEN FOR BEN (every placeholder in this command)

- M2: the spiral shows three arms while the damage is a full circle. a0 (due east) and the hand (clockwise from above).
- M3: the spiral's particle (orange TRAIL, 8 ticks, 0.4 apart) and the pulse's puff (12 TRAIL drawing in to his feet).
- N4: the firework's look (an orange ball to 6, a yellow one to 4.5, white sparks, the large blast and the twinkle).
- N7: both descriptions. SM7R's and SF9R's question wording.
- 250 per burst with three bursts in one place (750 the normal case at a wall or a mob); passive animals in the bursts and
  the flight (unchanged).

## WHAT ONLY THE BOOT CAN SHOW

- The spiral as drawn (a line, not dots; the 0.4 s fade) and the puff: SM7R's question.
- The three fireballs one after another, each down his held view, and the firework as drawn: SF9R's question.
- Pulse 2 and shot 2 landing on C+30 and C+10 on a real server (the settle hop on a real entity scheduler).

## R0b, RUN BEFORE COMMIT

On `paper/target/rpg-0.1.0-SNAPSHOT.jar` built from this tree (2026-11-28 00:39, `./mvnw -q package -DskipTests`,
before the commit): **21 `PRESENT` / 0 `ABSENT`, then 4 `ABSENT` / 0 `PRESENT`, then 2 `PRESENT`, as predicted.** One
needle was dropped while writing it: `duration_ticks`, which master already has in 20 files (it would have been a
presence that witnessed nothing). This is a reading of a jar on this machine, not of the deployed one: R0b proper is the
deployed jar's.
