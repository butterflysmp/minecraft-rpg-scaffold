# GATE — TRUE TOOLTIPS (2026-11-26), WITH #312'S TWO OWED ROWS BY REFERENCE

**Status: RUN 2026-11-26 on `88f1d4eb` (twice); readings at the end, under READINGS.** The plan is `docs/plans/PLAN-true-tooltips.md`. The scenario is
`paper/src/main/resources/selftest/GATE-true-tooltips.yml`. The stack, bottom up: #312 `feat/bottle-owed`, then
`feat/true-tooltips-resolvers`, `feat/true-tooltips` and this gate, `feat/true-tooltips-gate`. **Every prediction below
was written before any boot, and is frozen from the seat's approval.** No boot without Ben's own "ready" and the seat's
approval of the code AND this gate from origin.

## WHAT BEN TYPES — ONE COMMAND

```
/rpg selftest true-tooltips+broken-bottle.BB2R+armor-sets.AS15R2
```

- `true-tooltips`, the whole gate: its setup, then TT0 to TT19.
- `broken-bottle.BB2R`: the broken-bottle gate's SETUP, then only BB2R (`GATE-broken-bottle.md`, by reference).
- `armor-sets.AS15R2`: the armor-sets gate's setup, then only AS15R2 (`GATE-armor-sets.md`, by reference).

He types it on the pad's server, standing anywhere; the first setup takes him to the pad. **He answers ONE question,
TT19's, last of this gate.** BB2R and AS15R2 and their setups ask nothing.

**How long it runs (an ESTIMATE, from the scenario's waits, not a measurement):** this gate's `wait:` steps sum to about
700 ticks (35 s); with each step's own tick and the commands' replies, about a minute, then his answer to TT19. BB2R and
AS15R2 with their setups add about 15 s. **About a minute and a half, plus his answer.**

## SAFETY — 0.000 FROM THIS GATE; THE SUM, EXECUTED

- **This gate's rows deal him NOTHING.** No `damage @s`, no `rpg damage`, no mob that acts. Every mob is a zombie with
  `NoAI:1b`, which never swings, and nothing names him as a target. `ScenarioLint` (MOB_ACT_UNSTAGED) is green on it.
- **BB2R (by reference): 0.000.** READ, its steps in `GATE-broken-bottle.yml`: a give, two holds, two
  `probe_attribute` reads. No mob, no damage command.
- **AS15R2 (by reference): 0.000.** READ, its steps in `GATE-armor-sets.yml`: a craft at `/recipes` and a dump.
- **The two setups:** READ, `rpg heal`, kills, gamerules and the difficulty; no damage.
- **THE SUM: 0.000 + 0.000 + 0.000 = 0.000**, at or under 20.000. No term prices damage to him, so S-1 has nothing to
  execute and S-2 is kept. S-3: no dealer acts on him at any score the pad seeds.

## CARRIED STATE — THE BOOTER'S CHECKLIST, IN THIS ORDER (`.claude/rules/booting.md`, each step ticked in the report)

1. The seat's approval of the code AND this gate from origin; Ben's own "ready".
2. **The heads:** `git ls-remote --heads origin` for the three branches, equal to what the seat diffed; CI green on each.
   `git status --porcelain` prints nothing and `git rev-parse HEAD` equals the gate branch's head.
3. **No JVM:** `Get-CimInstance Win32_Process -Filter "Name='java.exe'"` counts 0.
4. **The backup**, to a NEW folder `C:\Users\Neb91\IdeaProjects\rpg-run-backups\2026-11-26-pre-true-tooltips`: `run\world`
   and `run\plugins\Rpg` with their file counts and copy counts, and his player `.dat` with its bytes.
5. **The world's saved difficulty,** read from the decompressed `level.dat`, before and after the run. (The armor-sets
   setup sets `easy` and this gate's sets `normal`; neither prices anything here.)
6. **Config:** `dev-pack-on-join: true`. No `run\rpgfight`.
7. **"BOOTING NOW", in its own message, BEFORE the boot command**, with: "Ben: do NOT log in until I tell you R0 passed."
8. `./scripts/dev-server.sh --refresh-content`; the `Done` matched to a log newer than the deployed jar.
9. R0 read on this boot's log; **"R0 PASSED" in its own message, before he logs in.** His first `joined the game` time
   reported beside R0's.
10. After his `/stop`: the clean-stop lines, no `java` process left.

## R0 — THE BUILD. READ IT FIRST. IF ANY R0 FAILS, STOP

### R0a — the build line names this branch's head, clean

`[Rpg] Build: <sha>`, `<sha>` being `feat/true-tooltips-gate`'s head as `git ls-remote --heads origin
feat/true-tooltips-gate` prints it, shortened. **Not `-dirty`, not `unknown`.** Instrument: `Select-String -Path
run\logs\latest.log -Pattern '\[Rpg\] Build:'`.

### R0b — the jar carries the stack and this gate

`docs/gates/GATE-true-tooltips-r0b.ps1`. **Prediction, line for line:**
- before `'--- absent'`: **17 `PRESENT`, 0 `ABSENT`** (5 `Entry`, 5 `ValueLine`, 7 `Has`);
- between `'--- absent'` and `'--- controls'`: **4 `ABSENT`, 0 `PRESENT`** (the literal "loose three", and C's three
  lines: no `Range: `, no `Wind-up`, no `Reload: `);
- after `'--- controls'`: **2 `PRESENT`, 0 `ABSENT`** (the instrument reads a class string it must find).

**It was run before commit on a jar built from this tree; see the PR body.**

### R0c — every Loaded line, and no `[Rpg]` WARN

- `[Rpg] Loaded …, 24 enchants, …, 25 weapons, 1 shields, 28 armor, …`: word for word #312's reading. This stack adds
  no content file (`ls content/enchants/*.yml | wc -l` = 24, `ls content/weapons/*.yml | wc -l` = 25).
- `Mint-on-craft: 42 indexed, 42 claiming, of 59 gear definitions`, and the loot line, word for word #312's.
- **0** `WARN]: [Rpg]` lines. A refused `{token}` or `ability_description:` would print one (`Skipping`), so this is
  also the loaders accepting the four authored files. Control: `INFO]: [Rpg] Loaded` reads 1 or more. None of
  `Refusing`, `Skipping`, `SEVERE`, `Exception`.

### R0d — every scenario loaded

Exactly one **`[Rpg] Selftest: dev server, 473 scenarios across 51 gates [melee-cell, …, broken-bottle,
true-tooltips]`**. Recount: 51 files, 524 keys (`cat GATE-*.yml | grep -cE '^[A-Za-z0-9_-]+:'`), 51 setups, so 473.
Control: 503 + 21 (the setup and TT0 to TT19) = 524. Controls: `453 scenarios` ABSENT, `50 gates` ABSENT.

### R0e — the orphan sweep with nothing staged

One `FIGHTWORLD sweep path=…\rpgfight folders=0`, no `deleting`, no `orphan`.

## THE ROWS

Each row's lines are those between `SELFTEST true-tooltips <row> START` and its `DONE`. **Clock: the server tick.**
- **A REPLY belongs to the earliest unanswered command whose text it answers (P-R1).**
- **A dump (`dump: held`)** logs `HELD <id> <MATERIAL> x1 name=… lore=[…]`, the lore as the SERVER built it, one line per
  `; `. **It is an AUTO reading of the server, never of the client:** what he sees on hover is TT19's ASK.
- **The enchant block** (READ, `EnchantLore.applied`): only ACTIVE enchants, at the TOP, each `Name <numeral>`, then a
  blank line, then the stat lines. A deactivated candidate is not drawn, so "deactivated" reads exactly as "bare".
- **The score line:** every row stages `rpg gearscore set 100` (TT16 also 200), so each weapon's second line is
  `Gear Score: 100` under its element, and each armor piece's first (READ, `BB1`'s reading; executed for armor in
  `TrueTooltipsGateArithmeticTest`).
- **Every figure below is EXECUTED in `TrueTooltipsGateArithmeticTest`** (the PLAYERHIT sums through `HitDamage`, with
  `TracedHit`'s `%.3f`; the tooltip figures through the shipped renderer at the row's staging), and every tooltip state
  is also one of T13's 1260 cases.
- **A ROW BEGINS FROM A CLEARED COOLDOWN:** TT9's Flint Staff, TT11's and TT12's Cursed Emerald are the only presses.
  TT9 is the staff's first press of the run. TT12's press is at least 80 + 40 ticks after TT11's, above the Emerald's
  30-tick cooldown.

### TT0 — LARGE SPARKS ON THE FLINT STAFF (the redraw at activate, level, deactivate)

Four dumps, the damage line and the enchant block:

| dump | enchant block | `Fire Damage:` |
|---|---|---|
| bare | none | `20` |
| III active | `Large Sparks III`, `""` | `32` |
| I | `Large Sparks I`, `""` | `24` |
| deactivated | none | `20` |

Every other line as bare: `Fire`, `Gear Score: 100`, `""`, `Fire Bolt  Right-Click`, `Hurls a fragment of burning mana`,
the damage line, `Cooldown: 1.2s | Mana Cost: 5`, `""`, the two flavour lines, `""`, `Common Magic Weapon`.

### TT1 — SHARP CRYSTALS ON THE CRYSTAL SHARD STAFF

`Kinetic Damage:` **16 / 25 / 19 / 16** (bare, III, I, deactivated); the block `Sharp Crystals III`, then `I`.

### TT2 — SHARPNESS ON AN IRON SWORD (a damage percent)

`Melee Damage:` **23 / 26.45 / 24.15 / 23**; `Attack Speed: 1.6` throughout; the block `Sharpness III`, then `I`.
EXECUTED: `23 x 1.15` is `26.449999999999996`, printed `26.45` (CC'S CALL 1, two decimals).

### TT3 — ECHO BEAM ON THE CURSED EMERALD (the count and the word)

| dump | the description's first line | `Kinetic Damage:` |
|---|---|---|
| bare | `Channel, then loose three` | `27 x 3` |
| III | `Channel, then loose six` | `27 x 6` |
| I | `Channel, then loose four` | `27 x 4` |
| deactivated | `Channel, then loose three` | `27 x 3` |

The second line is `bolts down your line of sight.` in all four; the cadence `Cooldown: 1.5s | Mana Cost: 40` unchanged.

### TT4 — SHARPENED GLASS ON THE BROKEN BOTTLE

`Crit Chance:` **+10% / +25% / +15% / +10%**; `Melee Damage: 18` and `Attack Speed: 2.0` throughout.

### TT5 — PROTECTION ON AN IRON CHESTPLATE (held)

`Defense:` **16 / 25 / 19 / 16**, the line after `Gear Score: 100`; the set-bonus block below it unchanged.

### TT6 — GROWTH ON AN IRON HELMET (held)

`Defense: 10` throughout. Bare: **no** `Health:` line. III: **`Health: +30`** directly under Defense. I: **`Health: +10`**.
Deactivated: no `Health:` line. (The shipped wording, which the seat keeps.)

### TT7 — MANA BANK ON AN IRON HELMET (held)

As TT6, with **`Mana: +30`** at III and **`Mana: +10`** at I.

### TT8 — THE FOUR BEHAVIOUR BLOCKS, QUOTED WHOLE

Each dump, in order (the enchant block first, then the stat lines):

- **Cursed Emerald, Focused Beam II:** `Focused Beam II`, `""`, `Kinetic`, `Gear Score: 100`, `""`, `Cursed Volley
  Right-Click`, `Channel, then loose one focused`, `beam down your line of sight.`, `Kinetic Damage: 162`, `Cooldown:
  1.5s | Mana Cost: 40`, `""`, `Nine facets, and every one of them looking back.`, `It asks for a moment before it
  answers.`, `""`, `Uncommon Magic Weapon`. **No `x N` and no range.**
- **Breacher, Shatter Caps III:** `Shatter Caps III`, `""`, `Kinetic`, `Gear Score: 100`, `Quiver: <n>/<c>`, `""`,
  `Breacher  Right-Click`, `One stone that shatters into`, `four chunks after 5 blocks.`, `Kinetic Damage: 85`,
  `Kinetic Damage: 40 x 4`, `Cooldown: 1.0s`, `""`, the two flavour lines, `""`, `Common Ranged Weapon`. (The quiver
  line is `Quiver: <n>/<c>`, the STAMP's count and capacity, which can include his own bonuses: not predicted, because
  T10 is withdrawn and the line is the stamp.)
- **Living Lantern, Airburst Flames III:** `Airburst Flames III`, `""`, `Fire`, `Gear Score: 100`, `""`, `Lantern's
  Breath  Right-Click`, `A seeking flame that splits`, `into three after 6 blocks.`, `Fire Damage: 45`, `Fire Damage: 35
  x 3`, `Cooldown: 0.8s | Mana Cost: 12`, `""`, `Common Magic Weapon`.
- **Lapis Staff, Lapis Crystal III:** `Lapis Crystal III`, `""`, `Kinetic`, `Gear Score: 100`, `""`, `Lapis Beam
  Right-Click`, `Right-click to fire a blue beam.`, `Kinetic Damage: 21`, `Cooldown: 1.0s | Mana Cost: 10`, `""`,
  `Lapis Crystal  Left-Click`, `Throws a lapis crystal`, `that floats where it lands.`, **`Kinetic Damage: 14.7`**,
  `Cooldown: 30.0s | Mana Cost: 30`, `""`, the two flavour lines, `""`, `Uncommon Magic Weapon`.

### TT9 — TRUTH: LARGE SPARKS III, THE LINE AGAINST THE HIT

The dump reads `Fire Damage: 32` (TT0). Then one press at a frozen zombie `t` at `~6`, facing east.

```
sent = ((amount + tune) x score/100) x enchantMultiplier + classDamageBonus + weaponLevelDamage) x chargeScale x crit
     = ((20 + 12) x 100/100)          x 1.0 (none)        + 0 (setup's sheet) + 0 (level 1, EXECUTED)) x 1.0 x 1 or 2
```

- **One** `PLAYERHIT source=flint_staff/right_click … element=fire sent=32.000 crit=false triggerScore=100`, or
  `sent=64.000 crit=true`. **The line's 32 IS that sum's item term.**
- `t`'s PROBE before: `customHP=1000.000/1000.000` (EXECUTED, `MobScaling.maxHealth(200, false, true, 100)`); after:
  below it. Night (18000), so no daylight burn moves it. Any DOTTICK lines after (the staff's scorch) are not this row.
- **The class term is 0 if the setup's `rpg stats` shows no `+N Magic Damage`.** If it shows one, the hit reads `32 + N`
  and the line still reads 32: that is T2 (the player's term is never on the item).

### TT10 — TRUTH: SHARPNESS III, A DAMAGE PERCENT AGAINST THE HIT

The dump reads `Melee Damage: 26.45`. One full-charge swing (40 ticks after the summon) at `t` at `~2`:
`sent = ((23 x 100/100) x 1.15 + 0 + 0) x 1.0 x 1 or 2`: **`sent=26.450 crit=false`**, or **`sent=52.900 crit=true`**,
`source=iron_sword/left_click`. The class term as TT9 (a `+N Melee Damage` on the setup's sheet adds N to the hit only).

### TT11 — TRUTH: ECHO BEAM III, THE COUNT AGAINST THE SHOTS

The dump reads `27 x 6` and `loose six`. One press at a frozen zombie `m` at `~10`:
- **Exactly six** `PLAYERHIT source=cursed_emerald/right_click … sent=27.000` (or `54.000` on a crit) between START and
  DONE, and no seventh. **The line's 6 IS the count of hits.**
- `m`'s PROBE before `1000.000/1000.000`; after, below it.

### TT12 — TRUTH: FOCUSED BEAM II, ONE BEAM AT THE LINE'S DAMAGE

The dump reads `Kinetic Damage: 162` with no `x N` (TT8). One press at `m` at `~10`:
- a `FOCUSEDBEAM` trace line (range 48), and **exactly one** `PLAYERHIT source=cursed_emerald/right_click … sent=162.000`
  (or `324.000` on a crit). No second hit.

### TT13 — TRUTH: PROTECTION III, THE TOOLTIP'S DEFENSE AGAINST THE SHEET

Three `rpg stats` reads, each after 10 ticks: **D0** nothing on his chest; **D1** the plain chestplate worn (its dump
`Defense: 16`); **D2** the chestplate with Protection III worn (its dump `Defense: 25`).
- **D1 − D0 = 16 and D2 − D0 = 25: the sheet's Defense moves by exactly what each tooltip says.** EXECUTED:
  `Protection.effectiveDefense(GearScore.scaledDefense(16, 100), 9)` = 25.0. D0 itself is not predicted (his accessories
  and fragments are not stageable).

### TT14 — C, WITNESSED: THE STAT AND ABILITY LINES DO NOT MOVE

Three pairs of dumps, bare then III. **In each pair the second is the first with ONE enchant line and a blank added at
the top, and every other line identical:**
- Lapis Staff + `Polished Scepter III`: `Kinetic Damage: 21`, no range line.
- Knell Beam + `Early Toll III`: `Void Damage: 63`; the description still `then tolls a beam down it 1.2s later.` (D3,
  OPEN FOR BEN), no wind-up line.
- Boltor + `Deft Hands III`: `Ranged Damage: 19`, `Attack Speed: 1.3`, its `Quiver:` line as bare; no reload line.

### TT15 — A CONDITIONAL ENCHANT: WILDFIRE III

Living Lantern, bare then `Wildfire III`: as TT14, one enchant line added; `Fire Damage: 45` and the prose unchanged.

### TT16 — A GEAR SCORE CHANGE WITH AN ENCHANT ACTIVE (the redraw at the gearscore site)

- Flint Staff + Large Sparks III: `Gear Score: 100` / **`Fire Damage: 32`**, then after `rpg gearscore set 200`:
  `Gear Score: 200` / **`Fire Damage: 64`** (`(20 + 12) x 2`).
- Iron sword + Sharpness III: **`26.45`**, then at 200 **`52.9`** (`23 x 2 x 1.15`, EXECUTED, printed `52.9`).

### TT17 — THE ENCHANT TABLE SHOWS THE SAME LORE (T12)

At level 13 (the table opens at 10; `LEVEL_GATED_MENU` green), the Flint Staff with Large Sparks III dumped held, then
shifted into the table's input: **the `SLOT` line holding it carries the SAME lore, line for line, as the held dump**
(`Large Sparks III`, `""`, `Fire`, `Gear Score: 100`, …, `Fire Damage: 32`, …). READ, `EnchantMenu`: the input is never
lore-patched, only reminted. His level goes back to 1 after (waited 40).

### TT18 — THE REDRAW AT THE CLEAR AND THE REFRESH WRITE SITES

Three dumps: Large Sparks III active (`32`); after `rpg enchant clear` (no enchant block, **`20`**); Large Sparks III put
back and `rpg refresh` run (the join's own `GearRefresher.refresh`): **`32`**, the block `Large Sparks III`.

### TT19 — THE ONE ASK, LAST

The Cursed Emerald with Echo Beam III (dumped: `27 x 6`, `loose six`) and the Breacher with Shatter Caps III, both in his
bar. **He hovers each and answers** whether the numbers read as what each weapon now does and whether each tooltip is
clear (`yes / no / not sure`). Predicted: **yes**. This is the client half the dumps cannot witness (the order and
colours as rendered).

### THE TWO OWED ROWS, BY REFERENCE

BB2R and AS15R2 are predicted in their own gate files (`GATE-broken-bottle.md` BB2R, `GATE-armor-sets.md` AS15R2), word
for word, unchanged by this stack.

## LINT AND THE SCAN RUN BEFORE COMMIT

- `ScenarioLint` is green on every row (`SelfTestScenariosTest`). **It caught one real defect while this gate was
  written:** TT19 held the Emerald on the tick after the Breacher's `rpg enchant active`, which runs a tick later on the
  entity scheduler and would have put Shatter Caps on the Emerald (`HELD_READ_THEN_CHANGE`). A `wait: 2` was added.
- R0b's script was run on a jar built from this tree before commit (the PR body quotes its output).

## CC'S CALLS IN THIS GATE

1. Sharpness staged in slot 0 by the dev command (a roll would put it in a normal slot): the engine reads
   `effective()`, never the slot index.
2. TT13 reads Defense as DELTAS on one row's own baseline, as the armor-sets gate does, because his accessories and
   fragments are not stageable.
3. TT9 to TT12 allow the crit reading (x 2) as the second outcome, as BB3 did.

## PLACEHOLDERS OPEN FOR BEN

The plan's section 13 is the list (prose of the three behaviour enchants, the rounding, the two unlabelled damage lines,
the crystal's shot line, range / wind-up / reload left alone, the Knell's "1.2s later", the quiver stamp, a broken
piece's lines). This gate adds none.

## WHAT ONLY THE BOOT CAN SHOW

- That the four authored files load with no WARN on the real server (R0c).
- That every write the pad reaches remints the held item so the dump after it reads the new lore (TT0, TT16, TT17, TT18):
  the suite can only prove each site calls the lore function (`TooltipRedrawSitesTest`).
- That the PLAYERHIT figures equal the lines on the real hit path (TT9 to TT12), and the sheet's Defense the tooltip's
  (TT13).
- That the client renders the rewritten lines legibly (TT19, his answer).

## READINGS — THE BOOT OF `88f1d4eb`, 2026-11-26

**No prediction above was edited.** Corrections are dated notes below.

### DATED NOTE, 2026-11-26 — THE SEAT'S ERROR, IN ITS WORDS

> *"Step 5 of my approval said R0b reads "17 PRESENT, then 4 ABSENT, then 2 controls ABSENT". I wrote the last clause
> from habit and did not check it against the gate."*

CC stopped before the boot; the seat ruled that the frozen gate governs (controls **2 PRESENT / 0 ABSENT**). Nothing in
the gate changed, and Ben's "ready" stood.

### CARRIED STATE, AS IT WAS ACTUALLY RUN

1. The seat's approval of `88f1d4eb8ce8` (with its ruling on the stop); Ben's "ready".
2. **Heads:** `git ls-remote` read master `5248b45c5bdf`, #312 `91a8de1c2036`, #313 `dbac1ff65e5f`, #314 `dbe1831aaebe`,
   #315 `88f1d4eb8ce8`. CI `build` success twice on each. `git status --porcelain` printed nothing; `HEAD` =
   `88f1d4eb8ce8938e70210b272dd7a562230f3bf0`.
3. **No JVM:** `java.exe` count 0.
4. **The backup**, to a NEW folder `C:\Users\Neb91\IdeaProjects\rpg-run-backups\2026-11-26-pre-true-tooltips` (absent
   before): `run\world` 193 files, copy 193; `run\plugins\Rpg` 181 files, copy 181; his player `.dat` 4,984 bytes, copy
   4,984 (`.dat_old` 5,013).
5. **The world's saved difficulty:** `normal`, read from the decompressed `level.dat`.
6. **Config line 12:** `  dev-pack-on-join: true`. No `run\rpgfight`.
7. **"BOOTING NOW" was sent in its own message, before the boot command**, which went in the next turn after Ben's "ok".
8. **The marker** was written in the boot command's own call: marker 13:10:19; deployed jar 13:10:49; `latest.log`'s
   first line `[13:10:50]`; `Done (7.023s)` at `[13:10:56]`.
9. **"R0 PASSED" was sent in its own message** after R0 was read (13:10:56 to 13:11:13). **Ben joined at 13:11:25**, after
   R0. After his `/stop` (13:18:27): no `java` process left.

### R0, BEFORE ANY LOGIN — ALL AS PREDICTED

- **R0a:** `[Rpg] Build: 88f1d4eb`.
- **R0b**, the DEPLOYED jar (13:10:49): **17 PRESENT / 0 ABSENT; 4 ABSENT / 0 PRESENT; controls 2 PRESENT / 0 ABSENT.**
- **R0c:** `Loaded 11 abilities, 44 visuals, 5 statuses, 7 elements, 24 enchants, 3 pools, 6 fragments, 6 aspects, 25
  weapons, 1 shields, 28 armor, 5 tools, 6 accessories, 7 mobs, …`; `Mint-on-craft: 42 indexed, 42 claiming, of 59 gear
  definitions`; **0** `WARN]: [Rpg]` (control: `INFO]: [Rpg] Loaded` 7); `Refusing` 0, `Skipping` 0, `SEVERE` 0,
  `Exception` 0, so the four edited content files loaded.
- **R0d:** `473 scenarios across 51 gates […, broken-bottle, true-tooltips]`; `453 scenarios` 0, ` 50 gates` 0.
- **R0e:** `FIGHTWORLD sweep path=…\rpgfight folders=0`; no `deleting`, no `orphan`.

### THE RUN — TWICE, AND WHY THE FIRST IS THE READING OF RECORD

- **Run 1:** his command at 13:11:42; `RUN START … rows=22 selftest=88f1d4eb`; `RUN CAPTURED armour=0 offhand=0`;
  `PAD tickets held=12`; the pad `ok=7250`, stood at `104.50 201.00 -3.50 yaw=-90.000`. It ran to the end:
  `RUN RESTORED xp=712834 cell=ranger/fire gamemode=SURVIVAL held=4 mana=100.0 trace=false spawn_mobs=false time=1000
  blocks=0 blocksBack=0/0 mobsRemoved=0 orbsRemoved=0 keep_inventory=false immediate_respawn=false difficulty=NORMAL
  health=134.000 vanilla_xp=143+0.500 spawn_phantoms=true max_summons_base=null armour=0 offhand=0 summons=0
  drag_tick=false`, then `PAD tickets released=12 cause=end` at 13:13:39. **RESTORED equals CAPTURED:** `armour=0`,
  `offhand=0`, `blocksBack=0/0`.
- **TT19 in run 1 read `answer=NOT STATED after 1200 ticks`**: the 60 s window closed before he answered.
- **Run 2:** he typed the same command again at 13:15:11 (not after an abort: run 1 had ended). It ran to the end, and its
  RESTORED line is word for word run 1's except `health=314.000`, again `armour=0 offhand=0 blocksBack=0/0`, `PAD
  tickets released=12 cause=end` at 13:16:57. **Every dump, every hit count and every sheet reading of run 2 is identical
  to run 1's** once the crit outcomes are set aside (extracted and `diff`ed). **Run 1 is the reading of record for TT0
  to TT18, BB2R and AS15R2; TT19's answer is run 2's**, given on a second run nobody asked for.
- 0 `SKIPPED`, 0 `ABORT`; 50 `DONE` (25 per run: three setups and 22 rows).
- **The runner behaved as #311's did** (the seat asked): the row clean, the stage lines, the P-R2 drain and the pad
  tickets all as before; nothing new or missing.

### THE ROWS (run 1; lore lines quoted as plain text, `|` between lines)

| row | reading | class |
|---|---|---|
| TT0 | `Fire Damage:` **20 / 32 / 24 / 20**; block `Large Sparks III`, `Large Sparks I`, none, none; every other line as bare | READ AS PREDICTED |
| TT1 | `Kinetic Damage:` **16 / 25 / 19 / 16**; `Sharp Crystals III`, `I` | READ AS PREDICTED |
| TT2 | `Melee Damage:` **23 / 26.45 / 24.15 / 23**; `Attack Speed: 1.6` throughout | READ AS PREDICTED |
| TT3 | `loose three` `27 x 3` / `loose six` `27 x 6` / `loose four` `27 x 4` / `loose three` `27 x 3` | READ AS PREDICTED |
| TT4 | `Crit Chance:` **+10% / +25% / +15% / +10%**; `Melee Damage: 18`, `Attack Speed: 2.0` throughout | READ AS PREDICTED |
| TT5 | `Defense:` **16 / 25 / 19 / 16**, after `Gear Score: 100`; set block unchanged | READ AS PREDICTED |
| TT6 | no Health / **`Health: +30`** / **`Health: +10`** / no Health; `Defense: 10` throughout | READ AS PREDICTED |
| TT7 | no Mana / **`Mana: +30`** / **`Mana: +10`** / no Mana | READ AS PREDICTED |
| TT8 | the four blocks, below | READ AS PREDICTED |
| TT9 | `Fire Damage: 32`; ONE `PLAYERHIT source=flint_staff/right_click sent=64.000 crit=true` (the allowed second outcome, 32 x 2); PROBE `t` 1000.000 → 904.000 | READ AS PREDICTED |
| TT10 | `Melee Damage: 26.45`; ONE `PLAYERHIT source=iron_sword/left_click sent=26.450 crit=false`; PROBE `t` 1000.000 → 973.550 | READ AS PREDICTED |
| TT11 | `27 x 6`, `loose six`; **exactly six** `PLAYERHIT source=cursed_emerald/right_click`: `27.000`, `27.000`, `27.000`, `54.000 crit=true`, `27.000`, `27.000` (VOLLEYSHOT shot=0 to 5); PROBE `m` 1000.000 → 811.000 | READ AS PREDICTED |
| TT12 | `Kinetic Damage: 162`, no `x N`; **exactly one** `PLAYERHIT … sent=162.000 crit=false`; VOLLEYSHOT shot=0 only; the trace `FOCUSED … range=48.000` (see the note on the token); PROBE `m` 1000.000 → 838.000 | READ AS PREDICTED (the trace's token, see below) |
| TT13 | sheet `⛨ Defense` **0 → 16 → 25**: D1 − D0 = **16**, D2 − D0 = **25**; the dumps `Defense: 16`, `Defense: 25` | READ AS PREDICTED |
| TT14 | each pair identical but for one enchant line and a blank on top: `Polished Scepter III` (`Kinetic Damage: 21`), `Early Toll III` (`Void Damage: 63`, still `then tolls a beam down it 1.2s later.`), `Deft Hands III` (`Ranged Damage: 19`, `Attack Speed: 1.3`, `Quiver: 8/8`) | READ AS PREDICTED |
| TT15 | `Wildfire III` added; `Fire Damage: 45` and the prose unchanged | READ AS PREDICTED |
| TT16 | `Gear Score: 100` `Fire Damage: 32` → `Gear Score: 200` `Fire Damage: 64`; `26.45` → `52.9` | READ AS PREDICTED |
| TT17 | the table's input `SLOT` lore equals the held dump line for line (`Large Sparks III | | Fire | Gear Score: 100 | … | Fire Damage: 32 | …`) | READ AS PREDICTED |
| TT18 | `32` → (clear) `20`, no block → (put back, `rpg refresh`) `32`, `Large Sparks III` | READ AS PREDICTED |
| TT19 | run 1: `answer=NOT STATED after 1200 ticks`. Run 2, VERBATIM: **"yes but, I want the damage type in the tool tip to match the class not the element. Ie Ranged Damage, Magic Damage, Melee Damage etc"** | run 1 NOT STATED; run 2 **yes**, with a request (below) |
| BB2R | `ATTRIBUTE attack_speed value=1.600 base=4.000`, then `value=2.000 base=4.000` | READ AS PREDICTED |
| AS15R2 | every step of its trace: `Set 3 level(s) … (now level 3 …)`; the four armour slots to air; 5 copper ingots; `/recipes` opened (no refusal) with `SLOT 0 COPPER_HELMET name="Copper Helmet"`; the click; no `CRAFT minted` line; `HELDID slot=0 item=copper_helmet`; `HELD copper_helmet … lore=Gear Score: 100 | Defense: 5 | | Set Bonus (4 pieces): | +1 Max Summons | | It greens where it is not worn. | | Common Helmet`; `Set 1 level(s)`. No `SKIPPED` | READ AS PREDICTED |

**TT8, the four blocks whole (run 1):**
- `Focused Beam II | | Kinetic | Gear Score: 100 | | Cursed Volley  Right-Click | Channel, then loose one focused | beam
  down your line of sight. | Kinetic Damage: 162 | Cooldown: 1.5s | Mana Cost: 40 | | Nine facets, … | It asks for a
  moment … | | Uncommon Magic Weapon`
- `Shatter Caps III | | Kinetic | Gear Score: 100 | Quiver: 2/2 | | Breacher  Right-Click | One stone that shatters into |
  four chunks after 5 blocks. | Kinetic Damage: 85 | Kinetic Damage: 40 x 4 | Cooldown: 1.0s | | Iron, gunpowder,
  string, | and whatever was lying around. | | Common Ranged Weapon`
- `Airburst Flames III | | Fire | Gear Score: 100 | | Lantern's Breath  Right-Click | A seeking flame that splits | into
  three after 6 blocks. | Fire Damage: 45 | Fire Damage: 35 x 3 | Cooldown: 0.8s | Mana Cost: 12 | | Common Magic Weapon`
- `Lapis Crystal III | | Kinetic | Gear Score: 100 | | Lapis Beam  Right-Click | Right-click to fire a blue beam. |
  Kinetic Damage: 21 | Cooldown: 1.0s | Mana Cost: 10 | | Lapis Crystal  Left-Click | Throws a lapis crystal | that floats
  where it lands. | Kinetic Damage: 14.7 | Cooldown: 30.0s | Mana Cost: 30 | | …`

**Run 2's crits** (all allowed second outcomes): TT11 two of six at `54.000`; TT12 `324.000 crit=true`.

### DATED NOTE, 2026-11-26 — TT12'S TRACE TOKEN (CC's wording, not a miss of the beam)

TT12 predicted "a `FOCUSEDBEAM` trace line (range 48)". The line printed is
`FOCUSED caster=… source=cursed_emerald/right_click from=104.500,202.620,-3.500 range=48.000 pressed=1514 tick=1534`:
the token is `FOCUSED` (READ, `PaperCombatWorld.traceFocusedBeam` → `focusedLine`). The trace, its source and its range
read as predicted; **CC named the wrong token**. The beam itself (one hit at 162) read as predicted.

### HIS SAFETY

**No `HURT` line on him in either run (0 in the log). Predicted NONE.** Every mob was a NoAI zombie (MOBSEED lines);
nothing named him as a target.

### WHAT NO ROW PREDICTED

1. **He ran the command twice.** Run 1's TT19 window (1200 ticks) closed before he answered; he started run 2, which
   repeated every row identically, and answered there. The window is the gate's `ask: "1 1200 …"`. OPEN for the seat:
   whether a hover question needs more than 60 s.
2. **His TT19 answer carries a new request, for Ben and the seat to rule:** *"I want the damage type in the tool tip to
   match the class not the element. Ie Ranged Damage, Magic Damage, Melee Damage etc"*. Today an ability's damage line
   is labelled by its ELEMENT (`Fire Damage`, `Kinetic Damage`), and only a basic attack's by its CLASS (`Melee Damage`,
   `Ranged Damage`), by the design recorded in `WeaponLore`'s javadoc. Nothing is built for it.
3. **Crits in a volley are drawn per shot**: TT11 read one crit among six in run 1 and two in run 2. The gate allowed a
   crit on each line, so it is not a miss.
4. After run 2 he gave himself a Cursed Emerald and ran `/rpg spawn knell` three times and `/rpg mana refill` three
   times (13:17:07 to 13:18:11), then `/stop`. Not readings.
5. **Every WARN after Done is Paper's own update notice:** *"You are running the latest build for your Minecraft version
   (26.1.2) However, you are 4 release(s) behind the latest stable release (26.3)!"* No other WARN or ERROR. (The upgrade
   procedure's Step 0 is the owner's; PacketEvents goes first.)

## THE CLASS LABELS — NEW ROWS TT20 TO TT27 FOR THE NEXT BOOT (2026-11-26, `feat/class-damage-labels`). NOT RUN

**Status: NOT RUN.** Ben's ruling at TT19 (run 2), verbatim: *"yes but, I want the damage type in the tool tip to match
the class not the element. Ie Ranged Damage, Magic Damage, Melee Damage etc"*. The account is `PLAN-true-tooltips.md`,
section 14. **TT0 to TT19 above are the record and are not edited**; their element labels (`Fire Damage: 32`) are what
that boot read. **Every prediction below is written before the boot and frozen from the seat's approval.**

### WHAT BEN TYPES — ONE COMMAND (NEXT BOOT)

```
/rpg selftest true-tooltips.TT20.TT21.TT22.TT23.TT24.TT25.TT26.TT27
```

This gate's setup, then only TT20 to TT27. **He answers ONE question, TT27's, last.** It waits **3600 ticks** and its
prompt ends **`You have 180 seconds.`** (`SelfTestAsk.prompt`).

**How long it runs (an ESTIMATE from the waits):** about 200 ticks of waits plus the steps, under half a minute, then
his answer.

### SAFETY — 0.000

No row deals him anything. TT26's one mob is a NoAI zombie that never acts; no damage command names him. **The sum:
0.000.** S-1 has nothing to execute.

### CARRIED STATE (NEXT BOOT)

The booting checklist (`.claude/rules/booting.md`) in its order, as for the boot of `88f1d4eb` above: the seat's approval
and Ben's "ready"; the heads and a clean tree; no JVM; a NEW dated backup with counts and his player file's size; the
world's saved difficulty; config line 12 `dev-pack-on-join: true`; no `run\rpgfight`; "BOOTING NOW" in its own message
before the boot command; `Done` matched to a marker made in the boot command's call; R0 read; "R0 PASSED" in its own
message before he logs in.

### R0 (NEXT BOOT)

- **R0a:** `[Rpg] Build: <head>`, this branch's head as `git ls-remote` prints it, not dirty.
- **R0b:** `docs/gates/GATE-true-tooltips-labels-r0b.ps1` on the DEPLOYED jar: before `'--- absent'` **8 PRESENT / 0
  ABSENT** (the label, the ask rule, the rows, and the two lines of the Flint Staff's new flavor); between `'--- absent'`
  and `'--- controls'` **3 ABSENT / 0 PRESENT** (the removed helper, TT27 at the short window, and the old flavor entry
  `  - "create fire by flinging mana into flint."`, which survives only in the file's comments, so the needle is
  anchored to the list entry); after `'--- controls'` **2 PRESENT / 0 ABSENT** (a class string, and the description's
  `      - "Hurls a fragment of burning mana"` entry: the anchored needle finds an entry that is there). Run before
  commit on a jar built from this tree (the PR body quotes it).
- **R0c:** as the boot of `88f1d4eb` (24 enchants, 25 weapons, 0 `[Rpg]` WARN, no Skipping): no content file is added
  or removed; `flint_staff.yml`'s flavor changes, and loads with no WARN.
- **R0d:** `[Rpg] Selftest: dev server, 481 scenarios across 51 gates`. Recount: 51 files, 532 keys, 51 setups. Control:
  524 + 8 = 532. `473 scenarios` ABSENT.
- **R0e:** `FIGHTWORLD sweep … folders=0`.

### THE ROWS (`dump: held`'s lore, plain, `|` between lines)

### TT20 — THE FLINT STAFF, LARGE SPARKS III

`Large Sparks III | | Fire | Gear Score: 100 | | Fire Bolt  Right-Click | Hurls a fragment of burning mana | **Magic Damage: 32** | Cooldown: 1.2s | Mana Cost: 5 | | **Early magicians learned you could | create fire by striking flint with mana.** | | Common Magic Weapon`

(Amended 2026-11-26, before any boot of these rows, by the seat's brief: Ben's new Flint Staff flavor, verbatim *"Early
magicians learned you could create fire by striking flint with mana."*, the break after "learned you could" a SEAT
PLACEHOLDER. It replaces "Prehistoric magicians learned you could / create fire by flinging mana into flint.", which TT0
to TT18 above read and keep as the record.)

### TT21 — THE CURSED EMERALD, BARE THEN FOCUSED BEAM II

bare: `Kinetic | Gear Score: 100 | | Cursed Volley  Right-Click | Channel, then loose three | bolts down your line of sight. | **Magic Damage: 27 x 3** | Cooldown: 1.5s | Mana Cost: 40 | …`; then `Focused Beam II | | … | Channel, then loose one focused | beam down your line of sight. | **Magic Damage: 162** | …`

### TT22 — THE BREACHER, SHATTER CAPS III

`Shatter Caps III | | Kinetic | Gear Score: 100 | Quiver: <n>/<c> | | Breacher  Right-Click | One stone that shatters into | four chunks after 5 blocks. | **Ranged Damage: 85** | **Ranged Damage: 40 x 4** | Cooldown: 1.0s | …` (the quiver line is the stamp, not predicted)

### TT23 — THE LAPIS STAFF, LAPIS CRYSTAL III

`Lapis Crystal III | | Kinetic | Gear Score: 100 | | Lapis Beam  Right-Click | Right-click to fire a blue beam. | **Magic Damage: 21** | Cooldown: 1.0s | Mana Cost: 10 | | Lapis Crystal  Left-Click | Throws a lapis crystal | that floats where it lands. | **Magic Damage: 14.7** | Cooldown: 30.0s | Mana Cost: 30 | …`

### TT24 — AN IRON SWORD, BARE (UNCHANGED)

unchanged: `Kinetic | Gear Score: 100 | | **Melee Damage: 23** | Attack Speed: 1.6 | | Common Melee Weapon`

### TT25 — A SHORT BOW, BARE (UNCHANGED)

unchanged: `Kinetic | Gear Score: 100 | Quiver: <n>/<c> | | **Ranged Damage: 16** | Attack Speed: 1.7 | | Short to draw. | Shorter to answer. | | Common Ranged Weapon`

### TT26 — TRUTH, AS TT9

the dump as TT20 (`Magic Damage: 32`); ONE `PLAYERHIT source=flint_staff/right_click … sent=32.000 crit=false` or `sent=64.000 crit=true` (EXECUTED, `TrueTooltipsGateArithmeticTest`: level 1 adds 0); PROBE `t` `customHP=1000.000/1000.000` before, below it after. The staff's first press of the run

### TT27 — THE ONE ASK, LAST, AT 3600 TICKS

the dump as TT20; then the ASK, 3600 ticks. Predicted: **yes**


**No element-labelled damage line** (`Fire Damage`, `Kinetic Damage`, `Void Damage`) in any dump of TT20 to TT27, and
the element line at the top unchanged. Each figure is also one of T13's cases, whose label check reads the weapon's
class from its own table.

### WHAT ONLY THE BOOT CAN SHOW

That the deployed jar renders the class labels on a held item, and that the client shows them legibly (TT27).
