# GATE — THE SUMMONER: THE BONE WHISTLE AND ITS WOLF

**Status: NOT RUN.** Branch `feat/summoner-gate`, on #298 `feat/summoner-5` (the stack #289 → #298). Written
2026-11-25, **reworked the same day for the seat's X1–X11**; every prediction committed before any boot. **No boot
without Ben's own "ready" and the seat's approval of code and gate from origin** (`.claude/rules/booting.md`, ticked
line by line). **GAME MODE: SURVIVAL, every row.**

**The scenario is `paper/src/main/resources/selftest/GATE-summoner.yml`, typed after the mage gate as ONE command:**

```
/rpg selftest mage-guaranteed+summoner
```

Mage-guaranteed's NEW readings in that run: **MG1R, MG2R2, MB1R, MB4aR, MB4bR, MB5R** (predicted in
`GATE-mage-guaranteed.md`); every other mage row is a **regression read** against its own prediction there.
Summoner's rows are AUTO except **SM17, the one ASK, last**. On the pad, facing east; nothing is asked of Ben's ground,
position or inventory. **Lint: green, and pinned clean** (`SelfTestScenariosTest`), including the new
`SUMMON_UNSTAGED`.

## X1 — EVERY ROW IS SELF-STAGED

Every row that reads a wolf opens the same way: `clear_inventory`, `clear_state` (cooldowns cleared, any summon of his
removed with cause `clear`), the whistle given and held, the score set, his turn east and a wait, mana refilled, **ITS
OWN cast**, and a `probe_summon` before it acts. It ends by killing its mobs. A row that needs a second item (SM4's
sword, SM6's Boltor, SM14 and SM15's Lapis Staff, SM16's Living Lantern) switches to it **after** its own cast:
`clear_inventory` does not remove a summon (no removal cause names a weapon leaving the hand; READ, `SummonRemoval`'s
eleven). **The lint enforces it:** `SUMMON_UNSTAGED` flags a row that reads a summon (a `probe_summon`, or a command
whose selector names a shipped summon's entity) with no `use` after a `clear_state` earlier in the row. Its planted
control is `ScenarioLintTest.aRowReadsOnlyASummonItCastItself`, and `SummonLintSelectorsTest` holds the selector set
equal to the shipped summons' entities.

## WHAT WAS READ OR EXECUTED, EACH MARKED

- **Difficulty scaling of a mob's hit on a PLAYER — READ, javap, `Player.hurtServer`:** when `scalesWithDifficulty()`,
  PEACEFUL returns false, EASY gives `min(a / 2 + 1, a)`, HARD `a × 3 / 2`, NORMAL unchanged; then `Avatar.hurtServer`.
  The scaling is in `Player`, so **a hit on the wolf is never scaled**.
- **Pricing — READ, `MobDamagePricing.price` → `MobScaling.attackDamage`:** `5.0 × gs/100 × base` for a vanilla hostile;
  a stored score is bounded 1..500 (`MobGearScore`).
- **A mob's defence — READ by grep:** the defence stat's only writer is `PlayerHealthSystem` (a player's equipment). A
  zombie's resolves to its base 0.0, and `Defense.applyDefense(x, 0)` returns x. **The defence term on every hit on a
  zombie or the wolf here is ×1.**
- **His health — READ:** `/rpg heal 1000000` fills his custom HP; the snapshot captures it at run start and the restore
  writes it back (`HEALTH saved=…`); the restore also puts back the world's difficulty and the `max_summons` base.
- **Steps share a tick until a wait — READ, `SelfTestRun.advance`:** non-wait steps run in one pass. So a `probe_mana`
  right after a `use` reads the mana in the press's own tick: **no regeneration term** (X11).
- **`aim` faces eye to eye — READ, `SelfTestDrivers.aim`.** **A `cmd` runs as him (`performCommand`), so `~` and `^`
  are anchored at his FEET** (vanilla's default anchor for an executing entity; INFERRED, not javap'd). **So the old
  SM6's `^ ^-0.6 ^3` put the wolf 0.6 into the floor: X4(b) was right, and the row is restaged on a block.**
- **The ray's filter — READ, javap, `CraftWorld.rayTraceEntities`:** the predicate runs inside
  `getNearbyEntities(sweptBox, filter)`, BEFORE each body's own `BoundingBox.rayTrace`. So the new `SIDES pass` line
  applies that same box test itself and is logged **only for a body on the line**.
- **Area effects skip a summon BEFORE damage — READ:** the shockwave (`BehaviourHits.shockwave`), the crystal shot
  (`CastExecutor`'s orb shot) and homing's pick (`ProjectileFlight.nearestMob`) each drop a body the table refuses
  (`MobHostility.harms(ENGINE_ATTACKER, side, …)`) before applying anything. **So no SIDES line is logged for the
  wolf there; the witness that it was inside is its position against the effect's centre and radius.**
- **A real entity left click is two packets — READ, `WeaponSwingListener`:** the ANIMATION packet reaches `onSwing`
  (the direct), and the attack packet reaches `Player.attack` (the stand-down). SM3D drives both in one tick.
- **The wolf's speed — READ, javap, `Wolf.createAttributes`:** `MOVEMENT_SPEED` 0.3. `Pathfinder.moveTo(…, 1.0)` walks
  it at its own speed (multiplier 1.0, INFERRED from the API's contract). The waits below are sized for a floor of
  **0.1 block a tick** (2 blocks a second), well under a wolf's walk (INFERRED; the boot decides).
- **The cooldown notice — EXECUTED:** `"On cooldown for %.1fs".formatted(195 / 20.0)` prints **`On cooldown for
  9.8s`** (jshell, the JDK's own formatter); sent by `sendMessage`, so it is a REPLY line (READ, `RpgListeners`).
- **The pad — READ, `SelfTestPadShape`:** interior x −2..45, z −13..13 from the stand, floor at dy 0, a barrier rim at
  x 46. Held chunks: `floorDiv(stand + X1 − 1, 16)` to `floorDiv(stand + X2 + 1, 16)` east-west.
- **X8, the run's end — READ, `SelfTestRun.end` and `SelfTestSnapshot.restore`:** every exit (END, every ABORTED, quit,
  server-stop) goes through `end()`, and its restore removes **every `selftest`-tagged mob within 48** (on server-stop
  through an inline scheduler). **So SM3B's live zombie cannot outlive an abort.** **But a summon is not tagged**, and
  nothing removed it at a run's end: only quit and server-stop removed his summons, through the summon service.
  **Added:** `end()` now removes all his summons, cause `clear`, after the restore, logged `summons=<n>` on the
  RESTORED line (`SummonWiringTest.theRunsEndRemovesHisSummons`, mutation MUTENDSUMMONS red).
- **X10 — BUILT:** a weapon whose summon cast names no loaded summon is refused at load, the file named
  (`WeaponLoader.requireLoadedSummons`; summons load before weapons, READ, `RpgPlugin`).
  `WeaponSummonRefusalTest` (2 rows, the shipped whistle loaded and refused), mutation MUTSUMMONREFUSE red.
- **INFERRED, the boot decides:** that `damage <e> <n> minecraft:mob_attack by <zombie>` reaches our pricing as that
  zombie's melee (SM3A, SM7, SM9); that a vanilla `summon`ed zombie on the pad stores score 100 (the enchant-columns
  readings: max health 200 read as custom `1000.000`, i.e. × 5.0 × 100/100).

## SAFETY: THE MOST THE RUN CAN DEAL HIM — A SUM, BEFORE THE ROWS

**The cap: 20.000 over the whole `mage-guaranteed+summoner` run.** No row lets a mob with attack above 0 act.

| source | staged attack | AI | distance | lifetime | most hits × biggest hit |
|---|---|---|---|---|---|
| mage-guaranteed, every mob | — | frozen NoAI (its own SAFETY, READ) | 6+ east | per row | **0** |
| SM3A `s3a` | none acting: one scripted `damage @s 0.2 mob_attack by s3a` | NoAI | 3 east | 7 ticks | **1 × 0.2 (EASY: min(1.1, 0.2)) × 5.0 × (gs ≤ 500)/100 = 5.000** |
| SM3B `s3b` | `attack_damage` **0.0** | live, set on him | 12 east | 5 ticks, then killed; any abort removes it (X8) | **0** (0 → EASY 0 → `price` 0 for a non-positive amount) |
| every other zombie (SM3C, SM3D, SM4–SM7, SM9, SM12, SM14–SM17) | — | NoAI | 2–12 east | its row | **0** |
| the wolf | — | ours; SUMMON → PLAYER is a refused cell | beside him | its row | **0** |
| his own ray, beams, bursts, shockwave, crystal shot | — | each skips the caster (READ: the shockwave's and the shot's `never himself`; the ray's `except(ignoreId)`) | — | — | **0** |

**Sum: 5.000 ≤ 20.000**, every term at its worst (the zombie's score at the cap; his armour and EASY counted as
nothing). His health is staged full in the setup.

## CARRIED STATE (CC, IN THIS ORDER) — A CHECKLIST, EACH LINE TICKED IN THE REPORT

1. No `java` process; the heads equal origin (`git ls-remote --heads origin feat/summoner-gate`).
2. `git status --porcelain` empty; `git rev-parse HEAD` equals the remote head.
3. The world's saved difficulty read and reported; the setup sets EASY and the restore puts it back.
4. `booting.md`, every step.

## THE RUN SHEET — BEN TYPES ONE LINE

He is told **R0 passed**, logs in, stands anywhere, types `/rpg selftest mage-guaranteed+summoner`, answers SM17's
question, waits for `SELFTEST summoner END`, then `/stop`.

## HOW LONG THE RUN IS

**Summoner's waits sum to 1473 ticks (73.65 s)**
(`tr -d '\r' < GATE-summoner.yml | awk '/^  - wait: /{s+=$3} END {print s}'`), plus SM17's ask (up to 1200 ticks, 60
s, or until he answers). Steps that are not waits share their pass's tick (READ, `advance`), so they add nothing beyond
the waits (536 steps: `grep -c '^  - '`). **About 1.5 to 2.5 minutes for summoner (ESTIMATE: the row-boundary clean and
settle, and the ask's answer time, are not measured).** With mage-guaranteed's 2690 ticks of waits: **about 4 to 5.5 minutes in all (ESTIMATE).** The run's `START` and
`END` lines give the real figure.

## R0 — THE BUILD. READ IT FIRST. IF ANY R0 FAILS, STOP

### R0a — the build line names the stack top's head, clean

`[Rpg] Build: <sha>`, `<sha>` being `feat/summoner-gate`'s head as `git ls-remote --heads origin feat/summoner-gate`
prints it, shortened. **Not `-dirty`, not `unknown`.** Instrument: `Select-String -Path run\logs\latest.log -Pattern
'\[Rpg\] Build:'`.

### R0b — the jar carries the summoner stack and this gate

`docs/gates/GATE-summoner-r0b.ps1`. **Prediction, line for line:** before `'--- absent'`, **31 `PRESENT`, 0 `ABSENT`**;
between `'--- absent'` and `'--- controls'`, **4 `ABSENT`, 0 `PRESENT`**; after `'--- controls'`, **4 `ABSENT`, 0
`PRESENT`** (counted from the script: 39 `ValueLine`/`Has`/`Entry` lines = 31 + 4 + 4). The rework added X1's lint
rule, X2's `tick=`, X4's `SIDES pass`, X10's refusal, `SM8A:`, `SM17:` and the ask line; and `SM8:` ABSENT (split).
**Run before commit on a jar built from this tree: see the PR body.**

### R0c — every Loaded line, and no `[Rpg]` WARN

- `[Rpg] Loaded 11 abilities, 44 visuals, 5 statuses, 7 elements, 23 enchants, …, 24 weapons, …` (`ls` at this branch:
  11, 44, 5, 7, 23, 24); **`[Rpg] Loaded 1 summon(s)`**; `Mint-on-craft: 38 indexed, 38 claiming, of 54 gear
  definitions`.
- **No `Skipping malformed weapon 'bone_whistle.yml'`** (X10: its wolf loads first).
- **0** `WARN]: [Rpg]` lines (control: `INFO]: [Rpg] Loaded` 1 or more); none of `Refusing`, `Skipping`, `SEVERE`,
  `Exception`, `Dev-only`, `currently prevented`.

### R0d — every scenario loaded

Exactly one **`[Rpg] Selftest: dev server, 419 scenarios across 48 gates [melee-cell, …, mage-guaranteed, summoner]`**
(recount: 48 files, 467 keys, 48 setups; control 444 + 23 = 467). Controls: `418 scenarios`, `420 scenarios` and
`410 scenarios` ABSENT.

### R0e — the orphan sweep with nothing staged

One `FIGHTWORLD sweep path=…\rpgfight folders=0`, no `deleting`, no `orphan`.

## THE ROWS

Each row's lines are those between `SELFTEST summoner <row> START` and its `DONE`. **Clock: the server tick
(`Bukkit.getCurrentTick`).** Wolf figures: `health = 100 × score/100`, `damage = 20 × score/100`. Every wolf probe is
`probe_summon "8"`: `SUMMONPROBE weapon=bone_whistle body=<b> alive=true health=<cur>/<max> at=<x y z> dist_him=<d>
within=8:<bool> target=… directed=…`, or `SUMMONPROBE none owner=<him> store=<n>`. **The cast each row opens with**
reads `SUMMON spawn … summon=bone_whistle_wolf score=100 health=100.000 damage=20.000` and a probe `health=100.000/100.000
… within=8:true target=none` (not repeated below). `<X Y Z>` is his stand (feet, block centre) after the row's turn.

### SM1 — THE CAST, THE COOLDOWN'S REFUSAL, THE REPLACE

- Press 1: mana `before` then `after` in the same tick: **`after = before − 40` exactly** (no regen term: X11).
- Press 2, 5 ticks after press 1 (the clock: press 1's tick, both presses in the step pass after a wait): **refused by
  the cooldown**: **`REPLY "On cooldown for 9.8s"`** (200 − 5 = 195 ticks; the format executed); no `SUMMON spawn`; mana
  **unchanged** in that tick.
- Press 3 at score 200, ≥ 207 ticks after press 1: **`SUMMON removed … cause=replace`, then `SUMMON spawn … score=200
  health=200.000 damage=40.000`**; probe `health=200.000/200.000`.

### SM2 — TELEPORT BACK, ON THE PAD

He steps to `(X−2, Z−6)` (`SELF … at=<X−2> <Y> <Z−6>`); the wolf is put at `(X+45, Z+10)`, **49.65** from him
(√(47² + 16²)), both on the interior's floor. **`SUMMON teleport … from=<X+45> <Y> <Z+10>`** (each within 0.5: at most
one tick's walk before the goal's first tick sees > 48), then a probe `within=8:true`. **CC'S CALL:** inside the pad;
the alternative (the seat's) is X + 50, which is past the barrier rim at x 46 and over the void, where `fall` is
refused by Q4. It is in a held chunk only if `(stand + 46) mod 16 ≤ 11`: true on the stand x 104 that the 2026-11-23
reading recorded, so it depends on where spawn is.

### SM3A — A MOB THAT DAMAGED HIM: why=damaged

One scripted `0.2` by the frozen `s3a`. **His `HURT before=1.000`**: 0.2 × EASY ×1 (0.2 < 2) × 5.0 × gs(`s3a`) 100/100
(INFERRED score); `after` = before through his defence, as the line says. Then **`SUMMON target … target=<s3a>
why=damaged`**; probe `target=<s3a>`.

### SM3B — A MOB TARGETING HIM WITHIN 30: why=targeting

`DRIVE mob_target <s3b> tag=s3b`, then within 3 ticks **`SUMMON target … target=<s3b> why=targeting`**; probe
`target=<s3b>`. **No HURT line** (attack 0; it is 12 east and killed 3 ticks later).

### SM3C — A DIRECTED MOB, BY THE SWING: why=directed

`DRIVE reload held=bone_whistle`, then **`SUMMON target … target=<s3c> why=directed`**; probe `directed=<s3c>
target=<s3c>`.

### SM3D — A DIRECT AT MELEE RANGE: why=directed, AND THE STAND-DOWN

`s3d` 2 east. In ONE tick: `DRIVE attack Player#attack target=<s3d> … attack_damage=<a>` (`a` > 0, so the event is
raised) and `DRIVE reload held=bone_whistle`. **NO PLAYERHIT** (the stand-down cancels the attack); **`probe s3d`
before = after** (`100.000`: a vanilla zombie's 20 × 5.0 × 100/100); **`SUMMON target … target=<s3d> why=directed`**.

### SM4 — A MOB HE MERELY HIT IS LEFT ALONE

After his own cast, the iron sword (charged by 25 ticks) hits `s4`: `DRIVE attack Player#attack target=<s4>`;
`probe s4` falls by the hit's `sent=`. **No `SUMMON target … target=<s4>`**; probe `target=none`.

### SM5 — THE CHASE LIMIT

Directed at `s5` (as SM3C), then `s5` put 40 east of him: within 3 ticks **`SUMMON target … target=none why=none
dropped=limit`**; probe `target=none directed=none`.

### SM6 — SIDES: HIS RAY THROUGH HIS WOLF

**The geometry (X4(b)):** a stone block at `(X+3, Y, Z)` (saved first; the row-end restore puts the floor back), the
wolf on it (feet at Y+1.00, box to Y+1.85, ±0.3 in z); `s6` at `(X+6, Y, Z)`, eye at Y+1.74; his eye at Y+1.62,
aimed eye to eye: **the ray crosses x = X+3 at Y+1.68 and z = Z, inside the wolf's box**, and over the block (top at
Y+1.00). Probe before the press: `at=<X+3> <Y+1> <Z>`.
**The press: exactly ONE `SIDES pass attacker=PLAYER body=<the wolf's body>`** (X4(a); **absent: the row DID NOT
TEST**). **`PLAYERHIT source=boltor/right_click target=<s6> … sent=19.000`** (`38.000 crit=true` on a crit): 19 × 100/100
× enchant 1.0 (`rpg enchant clear`; `show` prints no slot active) + class 0 + level 0 (level 1), × charge 1.0, × crit.
**`probe s6`: `100.000` → `81.000`** (or `62.000`). **The wolf: `100.000/100.000` before and after**; no hit line names
it.

### SM7 — A MOB'S PRICED HIT ON THE WOLF LANDS

A scripted `2` by the frozen `s7`. **The wolf: `100.000` → `90.000`**: 2 × 5.0 × gs(`s7`) 100/100 (INFERRED score), no
difficulty term (not a player), defence ×1.

### SM8A — WORLD DAMAGE REFUSED

`in_fire`, `fall`, `drown`, 5 each: **probe `health=100.000/100.000`** after; no `SUMMON removed`.

### SM8B — THE VOID REMOVES IT

`out_of_world` 5: **`SUMMON removed … cause=void`**; probe `SUMMONPROBE none … store=0`.

### SM8C — /kill REMOVES IT

**`SUMMON removed … cause=killed`**; probe `none … store=0`.

### SM9 — ITS DEATH (O1), THEN A FRESH CAST

A scripted 1000000 by the frozen `s9` (≥ 1000000 × 5.0 × 1/100 ≫ 100): **`SUMMON removed … cause=death`**, probe
`none`. Then `clear_state` and a new cast: **`SUMMON spawn … score=100 health=100.000 damage=20.000`**, probe
`health=100.000/100.000`.

### SM10 — THE CAP AT max_summons 0

**`SUMMON refused owner=<him> weapon=bone_whistle verdict=REFUSED_FULL max=0`**; **`REPLY "You cannot summon more (max
0)."`** once; mana **before = after** in the press's tick; probe `none`.

### SM11 — CLEAR_STATE LEAVES NO SUMMON

After his own cast: `STAGE clear_state … summons=1` and **`SUMMON removed … cause=clear`**; probe **`SUMMONPROBE none
owner=<him> store=0`**.

### SM12 — THE WOLF ATTACKS (X2)

`s12a`: NoAI, max health 200, **`probe s12a` `customHP=1000.000`** before. Directed (`DRIVE reload`), then **`SUMMON
target … target=<s12a> why=directed`**. **THE WALK IS NOT PREDICTED AS A NUMBER.** The wait is 140 ticks: the wolf
starts at most 8 from him and walks to within 2.0 of a zombie 6 east, at most about 12 blocks, which takes 120 ticks at
the 0.1-a-tick floor. So at least 20 ticks are left for hits; at its real walk, far more. **Predicted: at least THREE
`SUMMON hit … victim=<s12a> authored=20.000 score=100 sent=20.000 crit=none class=0 level=0 element=nature tick=<t>`**,
every term: authored 20, score 100 (× 1.0), crit none, class 0, level 0, defence ×1 (READ). **The cadence: consecutive
hit lines' `tick=` differ by exactly 20.** The clock is the first hit line's `tick`. The phase: the goal's `tick()`,
in the server tick's entity phase, and all the hits come from one body's goal. **`probe s12a` after: `1000 − 20 × N`**,
N being the number of hit lines in the row before that probe.
**Then at score 200** (`clear_state`, a fresh wolf `score=200 health=200.000 damage=40.000`, a fresh `s12b` at
`1000.000`): the same, with **`authored=20.000 score=200 sent=40.000`**, at least three lines 20 ticks apart, and
**`probe s12b` = `1000 − 40 × N`**.

> **The minimum of three is a FLOOR, not a count.** If the 0.1-a-tick floor is wrong, the row reads fewer than three and
> says so; the per-line terms, the 20-tick spacing and the `1000 − 20 N` identity do not depend on N.

### SM13 — IT FOLLOWS (X3)

He steps 12 east, to `(X+12, Z)`, inside the interior (`SELF … at=<X+12> <Y> <Z>`). After 160 ticks (12 blocks at the
0.1-a-tick floor need 120): **probe `within=8:true target=none`**, and **no `SUMMON teleport` line** in the row (12 <
48).

### SM14 — SIDES: A SHOCKWAVE WITH THE WOLF INSIDE (X5)

Kinetic Tremors I on the Lapis Staff, after his own cast (`show`: `"  active: kinetic_tremors I (3 hits on one foe in
3s: a 10 damage shockwave, 3 blocks)"`). `s14` at `(X+3, Z)`, max 200; the wolf put at `(X+3, Z−2)`. **The witness that
it is inside:** probe `at=<X+3> <Y> <Z−2>` before and after: **2.00 from `s14`'s feet, against the radius 3**. Three
beams 22 ticks apart: **three `PLAYERHIT source=lapis_staff/right_click target=<s14> … element=kinetic sent=21.000`**
(`42.000` on its own crit): 21 × 100/100 × 1.0 + 0 + 0. After the third, **exactly ONE
`kinetic_tremors/shockwave` PLAYERHIT, on `s14`, `sent=10.000`** (`20.000` if the third beam crit): 10 × 100/100 × 1.0
+ 0 + 0. **None on the wolf** (READ: the shockwave drops a refused body before applying). **`probe s14`:
`1000 − 63 − 10 = 927.000`** (less more on crits, each named). **The wolf `100.000/100.000`.**

### SM15 — SIDES: A CRYSTAL SHOT WITH THE WOLF INSIDE (X5)

Lapis Crystal II, after his own cast. The orb thrown 12 east: `ORB rest … at=<X+12.000>,<Y+1.620>,<Z>`. `s15` at
`(X+12, Z−5)`, max 200, 5.26 from the rest. The wolf put at `(X+6, Z+4)`: probe `at=<X+6> <Y> <Z+4>`, **7.39 from the rest
(√(36 + 16 + 1.62²)) against the shots' 15.** It is 7.2 from him, inside its guard 8, so it stays. One beam east into
the orb: **no `lapis_staff/right_click` PLAYERHIT**; **exactly one `lapis_crystal/crystal` PLAYERHIT, on `s15`,
`sent=10.500`** (`21.000` if the beam crit): beam 21 × 100/100 × 1.0 × 50% (level II) + 0 + 0; **exactly one `ORBSHOT …
to=<s15's sight point>`**; **none on the wolf** (READ: the shot drops a refused body). **`probe s15`: `989.500`** (or
`979.000`). **The wolf `100.000/100.000`.**

### SM16 — SIDES: A BURST THROUGH THE WOLF (X5)

Airburst Flames II on the Living Lantern, after his own cast (`show`: `"  active: airburst_flames II (splits after 6
blocks into 3 bursts of 30 damage)"`: EC14R2's read format with today's `travel_blocks: 6`, INFERRED). A stone block at `(X+7, Y, Z)` (saved first), the wolf on it: probe `at=<X+7>
<Y+1> <Z>`, 7.07 from him (stays). `s16` at `(X+10, Z)`, max 200. **`SPLIT caster=<him> source=living_lantern/right_click
at=<X+6.000>,<Y+1.620>,<Z> bursts=3`**. The bursts fly at Y+1.62 (the centre one straight; the side ones at ±12°,
±0.21 in z at x = X+7). **The wolf's box at x X+7 spans Y+1.00..1.85 and z ±0.3, so the centre burst crosses it.**
**Witness: at least ONE `SIDES pass attacker=PLAYER body=<the wolf>`** (one per projectile segment that crosses it; the
side bursts may add more). **Then `k` PLAYERHITs `source=living_lantern/right_click target=<s16> element=fire
sent=30.000`** (`60.000` on a crit), 1 ≤ k ≤ 3: the centre burst for certain, and each side burst if its homing (search
7) brings it in. Each is 30 × 100/100 × 1.0 + 0 + 0. **No `sent=45.000`** (the bolt split at 6 and never reached 10).
**`probe s16` = `1000 − 30 k` less each DOTTICK burn on it**, each read from its line. **The wolf `100.000/100.000`.**
**No SIDES pass line: the row DID NOT TEST.**

### SM17 — THE ONE ASK, LAST (X9)

His own wolf, directed at `s17` (NoAI, max 200) 6 east: `SUMMON target … why=directed`, then hit lines as SM12 during the
question. **The ask:** *"Did you see your wolf run to the zombie and attack it? (yes / no / not sure)"*, one answer, up
to 1200 ticks. **Predicted: yes.** This is Ben's half: whether it WORKS and is CLEAR, seen.

## EVERY COORDINATE LIES INSIDE THE PREFLIGHTED BOX (Z3, the seat's ruling of 2026-11-25)

**The setup preflights `-2 -6 45 10`** (it was `0 -6 42 10`, which SM2's x −2 and x 45 fell outside). READ,
`SelfTestPreflight`: the box is in whole blocks from his own block, each bound −64..64, and at every column his feet and
head must be AIR and the floor SOLID. READ, `SelfTestPadShape`: the pad's interior is x −2..45, z −13..13, floored, so
the box is measurable. **Every row starts on the stand** (READ, `SelfTestRun`: the row-start CLEAN teleports him to
`stage()`, the stand, logging `return=`). Stand-relative, x east, z south; the wolf spawns at his own feet (READ,
`SummonService.spawn`):

| row | him | mobs and blocks | wolf put at |
|---|---|---|---|
| every cast | (0,0) | — | spawns at (0,0) |
| SM2 | (−2,−6) | — | (45,10) |
| SM3A / SM3B / SM3C / SM3D | (0,0) | zombie (3,0) / (12,0) / (6,0) / (2,0) | — |
| SM4 | (0,0) | zombie (2,0) | — |
| SM5 | (0,0) | zombie (6,0), then (40,0) | — |
| SM6 | (0,0) | block (3,0), zombie (6,0) | on the block, (3,0) +1 |
| SM7, SM9 | (0,0) | zombie (4,3) | — |
| SM12, SM17 | (0,0) | zombies (6,0) | — |
| SM13 | (12,0) | — | follows him |
| SM14 | (0,0) | zombie (3,0) | (3,−2) |
| SM15 | (0,0) | zombie (12,−5); orb rest (12,0) | (6,4) |
| SM16 | (0,0) | block (7,0), zombie (10,0) | on the block, (7,0) +1 |

**Every x lies in −2..45 and every z in −6..10.** SM2's separation stays 49.65 (√(47² + 16²)), beyond teleport_back's 48.

## NOT STAGEABLE YET (X7)

**The cap with ANOTHER weapon's summon** (a second summon weapon held while one wolf lives) cannot be staged: one
summon weapon ships. It is covered by `SummonCap`'s table in core (NEW, REPLACE, REFUSED_FULL, REFUSED_UNKNOWN, by
weapon id). No second weapon is invented for this gate.

## REMOVAL CAUSES THE PAD STAGES

replace (SM1), void (SM8B), killed (SM8C), death (SM9), clear (SM11, and every row's opening `clear_state`, and the
run's end, X8). **Not stageable here:** owner-death, quit, world, stop, orphan, backstop (unit tests only).

## READINGS — THE BOOT OF `f48f78e9`, 2026-11-25 (written after the run; no prediction above is edited)

**The boot:** marker 01:33:16; `./scripts/dev-server.sh --refresh-content`; deployed jar 01:33:44; `latest.log` first line
01:33:45; `Done (6.440s)` 01:33:51. **R0 read 01:34:13, before any login. Ben joined 01:49:25.** `SELFTEST RUN START
mage-guaranteed+summoner rows=44 selftest=f48f78e9` 01:49:32; `SELFTEST summoner setup START` 01:52:00; `SELFTEST RUN
FINISHED` 01:53:39; his `/stop` 01:55:53. No abort. The world's saved difficulty before the boot: `normal` (`level.dat`,
`difficulty_settings`). Backup: `rpg-run-backups\2026-11-25-pre-summoner` (world 193 files, Rpg 175).

| row | class | the lines |
|---|---|---|
| R0a | READ AS PREDICTED | `[Rpg] Build: f48f78e9` |
| R0b | READ AS PREDICTED | on the DEPLOYED jar: 31 PRESENT / 0 ABSENT; 4 ABSENT; 4 controls ABSENT |
| R0c | READ AS PREDICTED | `Loaded 11 abilities, 44 visuals, 5 statuses, 7 elements, 23 enchants, …, 24 weapons, …`; `Loaded 1 summon(s)`; `Mint-on-craft: 38 indexed, 38 claiming, of 54 gear definitions`; 0 `WARN]: [Rpg]` (control: 7 `INFO]: [Rpg] Loaded`); none of Refusing, Skipping, SEVERE, Exception, Dev-only, currently prevented |
| R0d | READ AS PREDICTED | one `Selftest: dev server, 419 scenarios across 48 gates [… mage-guaranteed, summoner]`; 418, 420, 410 absent |
| R0e | READ AS PREDICTED | `FIGHTWORLD sweep path=…\run\world\dimensions\rpgfight folders=0`; no deleting, no orphan |
| setup | READ AS PREDICTED | `PREFLIGHT ok box=-2..45 east -6..10 south air=1632/1632 floor solid=816/816`; `DIFFICULTY found=NORMAL set=EASY`; the stand `104.50 201.00 -3.50` |
| SM1 | **MISS** (one clause) | mana `100.000` → `60.000` in the press's tick; the wolf `score=100 health=100.000 damage=20.000`; press 2: `REPLY "On cooldown for 9.8s"`, no spawn. **The replace logged `SUMMON spawn … score=200 health=200.000 damage=40.000` BEFORE `SUMMON removed … cause=replace`; the prediction said removed, then spawn.** Press 2's "mana unchanged in that tick" **DID NOT TEST**: the row has no read before press 2 in that tick; it read `60.250`, 5 ticks of regen on `60.000`, so nothing was spent |
| SM2 | READ AS PREDICTED | `SELF … at=102.50 201.00 -9.50`; **`SUMMON teleport … from=149.50 201.00 6.50`** (X+45, Z+10 exactly); probe `within=8.0:true` |
| SM3A | **MISS** | `SUMMON target … why=damaged` as predicted; **his `HURT before=15.000 after=15.000 defense=0.000 dealer=<s3a> dealerType=zombie`, against the predicted 1.000.** See SAFETY |
| SM3B | READ AS PREDICTED | `DRIVE mob_target …`; `SUMMON target … why=targeting`; no HURT line |
| SM3C | READ AS PREDICTED | `DRIVE reload held=bone_whistle`; `SUMMON target … why=directed`; probe `directed=<s3c>` |
| SM3D | **DID NOT TEST WHAT IT WAS MEANT TO** | **`DRIVE attack … attack_damage=0.000`, then `DRIVE attack NO_DAMAGE_EVENT`: the attack raised no event, so the stand-down was never exercised.** `SUMMON target … why=directed`; no PLAYERHIT. `probe s3d` read `100.000` then **`80.000`: the wolf's own `SUMMON hit … tick=22245`**, which the prediction (before = after) did not allow for |
| SM4 | **MISS** | his sword landed (`PLAYERHIT source=iron_sword/left_click … sent=23.000`); **then `SUMMON target … target=<s4> why=targeting` and `SUMMON hit … tick=22315`**; `probe s4` `57.000`. The prediction was "left alone, target=none" |
| SM5 | READ AS PREDICTED | `why=directed`, then `target=none why=none dropped=limit` |
| SM6 | READ AS PREDICTED | the wolf on the block `at=107.50 202.00 -3.50`; **exactly one `SIDES pass attacker=PLAYER body=57ce27d1-0c8b-4fe6-941e-4dade1cf9d2c` (the wolf's body)**; `PLAYERHIT source=boltor/right_click … sent=19.000 crit=false`; `probe s6` `100.000` → `81.000`; the wolf `100.000/100.000` both probes. (Unpredicted, after: `SUMMON target … target=<s6> why=targeting`) |
| SM7 | READ AS PREDICTED | the wolf `100.000` → **`90.000`**. (Unpredicted: the after-probe shows `target=<s7>`, with no `SUMMON target` line) |
| SM8A | READ AS PREDICTED | fire, fall, drown: `100.000/100.000` after |
| SM8B | READ AS PREDICTED | `SUMMON removed … cause=void`; `SUMMONPROBE none … store=0` |
| SM8C | READ AS PREDICTED | `SUMMON removed … cause=killed`; `none … store=0` |
| SM9 | READ AS PREDICTED | `SUMMON removed … cause=death`; `none`; then a fresh `100.000/100.000` |
| SM10 | READ AS PREDICTED | `SUMMON refused … verdict=REFUSED_FULL max=0`; `REPLY "You cannot summon more (max 0)."`; mana `100.000` and `100.000` in the press's tick; `none … store=0` |
| SM11 | READ AS PREDICTED | `SUMMON removed … cause=clear`; `STAGE clear_state … summons=1`; `none … store=0` |
| SM12 | READ AS PREDICTED | `why=directed`; at score 100 six hits `authored=20.000 score=100 sent=20.000 crit=none class=0 level=0 element=nature`, **ticks 22683, 22703, 22723, 22743, 22763, 22783** (each 20 apart); `probe s12a` `1000.000` → **`880.000`** (= 1000 − 6 × 20). At score 200, six hits `score=200 sent=40.000`, **ticks 22859, 22879, 22899, 22919, 22939, 22959**; `probe s12b` → **`760.000`** (= 1000 − 6 × 40) |
| SM13 | READ AS PREDICTED | `SELF … at=116.50 201.00 -3.50`; after 160 ticks `within=8.0:true target=none` (`dist_him=7.65`); 0 `SUMMON teleport` lines |
| SM14 | **MISS** (the probe) | `"  Slot 0: [*kinetic_tremors I]"`; the wolf `at=107.50 201.00 -5.50` (2.00 from `s14`), later `-4.34` (0.84), inside the 3; beams `sent=42.000 crit=true`, `42.000 crit=true`, `21.000`; **exactly one `kinetic_tremors/shockwave … sent=10.000`**, none on the wolf; the wolf `100.000/100.000`. **But `SUMMON target … target=<s14> why=targeting` after the first beam, and four `SUMMON hit` (ticks 23244, 23264, 23284, 23304): `probe s14` `805.000` = 1000 − 42 − 42 − 21 − 10 − 80**, against 927 less the named crits |
| SM15 | READ AS PREDICTED | `ORB rest at=116.500,202.620,-3.500`; the wolf `at=110.50 201.00 0.50` (7.39 from the rest); `ORBSHOT … to=116.500,202.740,-8.500`; **exactly one `lapis_crystal/crystal … sent=10.500`**; `probe s15` `989.500`; the wolf `100.000/100.000`. (Unpredicted, after: `why=targeting` on `s15`) |
| SM16 | **DID NOT TEST WHAT IT WAS MEANT TO** | the wolf on the block `at=111.50 202.00 -3.50`; **`SPLIT … at=110.724,201.911,-3.500`** (predicted `<X+6.000>,<Y+1.620>` = 110.500, 202.620); **no `SIDES pass` line, no burst PLAYERHIT, `probe s16` `1000.000`**. INFERRED: the bolt homed down towards `s16` before the split, so the bursts flew at about +0.91, below the stone block's top (+1.00) at x 111, and ended in it |
| SM17 | READ AS PREDICTED | `why=directed`; six hits ticks 23636 … 23736, 20 apart; **Ben's answer, verbatim: `ASK 1 question="Did you see your wolf run to the zombie and attack it? (yes / no / not sure)" answer="yes" ticks=91`** |

**HIS SAFETY: MISS.** One HURT line on him in the whole run: SM3A's `HURT before=15.000 after=15.000 defense=0.000`. **Sum
15.000: above the gate's bound of 5.000, under the cap of 20.000.** INFERRED cause, not read: a mob's melee on a player is
priced from the mob's attack attribute (a zombie's 3.0 × 5.0 × 100/100 = 15), not from the scripted 0.2. If so, the
SAFETY table's SM3A term was the wrong quantity: at the score cap 500 the same hit is 75, above 20.000.

**THE RESTORED LINE, WHOLE:** `SELFTEST RUN RESTORED xp=712824 cell=ranger/fire gamemode=SURVIVAL held=2 mana=100.0
trace=false spawn_mobs=false time=1000 blocks=2 mobsRemoved=0 orbsRemoved=0 keep_inventory=false immediate_respawn=false
difficulty=NORMAL health=112.000 vanilla_xp=150+0.694 spawn_phantoms=true max_summons_base=null summons=1 drag_tick=false`;
then `PAD tickets released=12 cause=end`; `HEALTH saved=112.000 customHP=112.000/350.000 wrote=112.000`. **`summons=1` is
SM17's wolf, removed by X8's end-of-run removal**; `max_summons_base=null` is the default restored (no stored base).

**WHAT NO ROW PREDICTED:**
1. **A mob he hits starts targeting him, so the wolf takes it** (`why=targeting`: SM4, SM6, SM14, SM15). This conflicts
   with the ruling that the wolf leaves alone a mob he merely hit. The mechanism is not read.
2. **The whistle's attack is `0.000`, so a real entity click raises no damage event** (SM3D). The stand-down may never be
   reached by the whistle (INFERRED).
3. `SUMMONPROBE`'s `target=` reads the body's vanilla target: SM7 shows `target=<s7>` with no `SUMMON target` line.
4. Every cast seeds the wolf as `MOBSEED … wolf gs=- source=passive max=<its health>` at the stand (`stand_d=0.00`, once
   `0.02`).
5. **After the run, Ben used the whistle near spawn against Knells:** `BEAM telegraph mob=knell … target=wolf …`; hits
   20 apart; three `cause=replace` removals; `cause=stop` at his `/stop`.
6. Six Paper WARN lines after Done: *"you are 4 release(s) behind the latest stable release (26.3)"* (no `[Rpg]` WARN).
7. No `SUMMON goals restored` line in the run.
