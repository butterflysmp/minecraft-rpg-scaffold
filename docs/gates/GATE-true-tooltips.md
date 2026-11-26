# GATE — TRUE TOOLTIPS (2026-11-26), WITH #312'S TWO OWED ROWS BY REFERENCE

**Status: NOT RUN.** The plan is `docs/plans/PLAN-true-tooltips.md`. The scenario is
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
