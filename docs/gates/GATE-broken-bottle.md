# GATE — THE BROKEN BOTTLE AND SHARPENED GLASS (2026-11-25), WITH #308'S THREE OWED ROWS BY REFERENCE

The plan is `docs/plans/PLAN-broken-bottle.md`. The scenario is `paper/src/main/resources/selftest/GATE-broken-bottle.yml`.
The stack, bottom up: #308 `feat/armor-sets-owed`, then `feat/broken-bottle`, `feat/sharpened-glass` and this gate,
`feat/broken-bottle-gate`. **Predictions are frozen from the seat's approval.** NOT BOOTED.

## WHAT BEN TYPES — ONE COMMAND

```
/rpg selftest broken-bottle+summoner.SM9R.SM16R2+armor-sets.AS15R
```

- `broken-bottle`, the whole gate: its setup and BB0 to BB8.
- `summoner.SM9R.SM16R2`: the summoner gate's SETUP, then only SM9R and SM16R2, in the gate's order. Their steps are
  the ones in `GATE-summoner.yml`, by reference, never copied.
- `armor-sets.AS15R`: the armor-sets gate's setup, then only AS15R.

**This replaces re-running the whole summoner and armor-sets gates for three rows** (about six minutes and two
repeated questions, SM17 and AS16). It is built in this PR: `SelfTestGateList` parses `<gate>.<ROW>[.<ROW>]` as an item
of the `+` list. It is pure, with tests and mutations.
- **Why a dot:** the argument is Brigadier's `word()`, which accepts `0-9 A-Z a-z _ - . +` and no colon or comma (READ,
  javap, `StringReader.isAllowedInUnquotedString`, brigadier 1.3.10). No shipped gate or row name contains a dot: 0
  of 491 keys before this gate.
- **Refused by name before anything starts:** an unknown row, an empty row name, `setup`, a row named twice, a gate
  named twice whole or by reference, and a CHAINED row (first step `after:`), which alone would only ever skip.
- **The seat's G5 (2026-11-24) read "no gate:row syntax".** This brief asks for exactly that, so it is read as the
  seat's revision of G5.

He types it on the pad's server, standing anywhere; the first setup takes him to the pad. **He answers ONE question,
BB8's, last of this gate.** The summoner and armor-sets setups follow and ask nothing; SM9R, SM16R2 and AS15R ask
nothing.

## SAFETY — 0.000 FROM THIS GATE; THE SUM, EXECUTED

- **This gate's rows deal him NOTHING.** No `damage @s`, no `rpg damage`, no mob that acts: every mob is a zombie with
  `NoAI:1b`, which never swings, and nothing names him as a target.
- **SM9R (by reference): 0.000 to him.** The seat's ruling on #308, each part READ:
  1. Its zombie is summoned `NoAI:1b` (`GATE-summoner.yml`, SM9R).
  2. The ONLY command naming it as a dealer is
     `damage @e[type=minecraft:wolf,distance=..12,limit=1,sort=nearest] 1 minecraft:mob_attack by @e[tag=s9r,limit=1]`.
     Its target is a WOLF selector, never `@s`, and `type=minecraft:wolf` cannot match a player.
  3. If that selector matches nothing: READ, `DamageCommand` gets the target through `EntityArgument.getEntity` →
     `EntitySelector.findSingleEntity`, which throws `NO_ENTITIES_FOUND` on an empty list before `hurtServer` is called.
     The command fails, and nothing is damaged.
  4. An abort's restore removes it: READ, `SelfTestSnapshot.restore` removes every nearby entity tagged `selftest`, and
     the zombie carries `selftest`.
- **SM16R2 (by reference): 0.000 to him.** Its mobs are its wolf and a NoAI zombie `s16`; the row damages no player (its
  steps, `GATE-summoner.yml`).
- **AS15R (by reference): 0.000.** A craft and a dump.
- **THE SUM: 0.000 + 0.000 + 0.000 + 0.000 = 0.000**, at or under 20.000. No term prices damage to him, so S-1 has
  nothing to execute. S-3: no dealer acts on him at any score the pad seeds.

## R0 — THE BUILD. READ IT FIRST. IF ANY R0 FAILS, STOP

### R0a — the build line names this branch's head, clean

`[Rpg] Build: <sha>`, `<sha>` being `feat/broken-bottle-gate`'s head as `git ls-remote --heads origin
feat/broken-bottle-gate` prints it, shortened. **Not `-dirty`, not `unknown`.** Instrument: `Select-String -Path
run\logs\latest.log -Pattern '\[Rpg\] Build:'`.

### R0b — the jar carries the stack and this gate

`docs/gates/GATE-broken-bottle-r0b.ps1`. **Prediction, line for line:**
- before `'--- absent'`: **30 `PRESENT`, 0 `ABSENT`** (15 `ValueLine`, 4 `Entry`, 11 `Has`);
- between `'--- absent'` and `'--- controls'`: **4 `ABSENT`, 0 `PRESENT`** (no sweep, no craft_result, in no loot
  source, no recipe file);
- after `'--- controls'`: **3 `ABSENT`, 0 `PRESENT`** (the old game's 30% and 20, and G5's old javadoc phrase).

**It was run before commit on a jar built from this tree; see the PR body.**

### R0c — every Loaded line, and no `[Rpg]` WARN

- `[Rpg] Loaded …, 24 enchants, …, 25 weapons, 1 shields, 28 armor, …`. Recounted at this branch:
  - `ls content/enchants/*.yml | wc -l` = 24 (23 at `9c01d43f`, + `sharpened_glass`);
  - `ls content/weapons/*.yml | wc -l` = 25 (24, + `broken_bottle`).
  Every other figure is #308's reading, unchanged.
- `Mint-on-craft: 42 indexed, 42 claiming, of 59 gear definitions` (58, + the bottle; it claims no craft:
  `BootCountLinesTest`).
- **The loot line is word for word #308's**: no source or dev source gains an item.
- **0** `WARN]: [Rpg]` lines. Control: `INFO]: [Rpg] Loaded` reads 1 or more. An unknown-key WARN for `crit_chance`
  would be the loader not knowing the key; `BrokenBottleContentTest` pins that it does. None of `Refusing`,
  `Skipping`, `SEVERE`, `Exception`.

### R0d — every scenario loaded

Exactly one **`[Rpg] Selftest: dev server, 451 scenarios across 50 gates [melee-cell, …, armor-sets, broken-bottle]`**.
Recount: 50 files, 501 keys (`cat GATE-*.yml | grep -cE '^[A-Za-z0-9_-]+:'`), 50 setups, so 451. Control: 491 + 10 =
501. Controls: `442 scenarios` ABSENT, `49 gates` ABSENT.

### R0e — the orphan sweep with nothing staged

One `FIGHTWORLD sweep path=…\rpgfight folders=0`, no `deleting`, no `orphan`.

## THE ROWS

Each row's lines are those between `SELFTEST broken-bottle <row> START` and its `DONE`. **Clock: the server tick.**
- **REPLY reading rule (P-R1):** a REPLY belongs to the earliest unanswered command whose text it answers, never to
  "the CMD above it".
- **The crit baseline:** `B` names a row's FIRST `rpg stats` Crit Chance line, read with an iron sword in hand. Every
  crit prediction is `B` plus an exact offset. His accessories and fragments are not stageable (no selftest verb
  clears them), so they cannot move a reading.
- **B is predicted 15%** for a player wearing nothing that grants crit. EXECUTED: `Crit.BASE_CHANCE` 0.15, and
  `LevelBonus.at(1)` grants 0 crit and 0 weapon damage (`CritChanceEnchantTest`, `BrokenBottleGateArithmeticTest`). If
  B reads otherwise, every later row still reads, by its offsets.
- **The sheet line:** `  Crit Chance  <n>%` (READ, `StatsSheet.line`: a two-space indent, the label padded to 13).

**Every figure below is EXECUTED in `BrokenBottleGateArithmeticTest` with the inputs the rows stage.**

### BB0 — THE BASELINE

An iron sword at score 100, level 1: `  Crit Chance  15%` (B₀). The Accessories block, if any, is recorded as read.
- **A `+N Melee Damage` in it is the class term of BB3's sum.** Predicted absent, so N = 0.

### BB1 — THE TOOLTIP

1. `HELDID slot=<n> item=broken_bottle`.
2. `rpg enchant show`, a fresh roll: `  Slot 0: [sharpened_glass locked]`, `  Slot 1: [kinetic_tremors locked]` and
   `-- 4 slot(s)`. Slots 2 and 3 are not predicted.
3. After `rpg enchant clear` and score 100, `dump: held`: **`HELD broken_bottle GLASS_BOTTLE x1`**, the name `Broken
   Bottle` in the common colour, and the lore, in order:
   - `Kinetic`
   - `Gear Score: 100`
   - `""`
   - `Melee Damage: 18`
   - `Attack Speed: 2.0`
   - `Crit Chance: +10%`
   - `""`
   - `Common Melee Weapon`

   **No tier line** (none exists for any weapon: the golden lore file). No flavor.

### BB2 — THE HELD CRIT, FLAT, MAIN HAND ONLY; THE ATTACK SPEED

Each change of hand waits 10 ticks before its read.

| step | Crit Chance line | `attribute … attack_speed get` reply ends |
|---|---|---|
| iron sword, score 100 | `B₂` (15%) | `is 1.6` |
| the bottle, score 100 | `B₂ + 10` (25%) | `is 2.0` |
| iron sword again | `B₂` (15%) | — |
| the bottle in the OFF hand, the iron sword in the main | `B₂` (15%) | — |
| the bottle back in the main hand (`HELDID … item=broken_bottle`), **score 200** | `B₂ + 10` (25%): FLAT | — |

- **The attribute:** EXECUTED, `VANILLA_BASE + AttackSpeedAttribute.modifier(2.0, 1.0)` = 2.0 and with 1.6 it is 1.6,
  at an attack-speed stat of 1.0. No accessory grants attack speed (READ: `AccessoryStat` has no such stat).
- **The reply's words are INFERRED** from vanilla's `commands.attribute.value.get.success`, "Value of attribute %s for
  %s is %s". Only the trailing figure is predicted.
- **This is the swords' witness and one more.** The swords' 1.6 was witnessed by the tooltip line `Attack Speed: 1.6`
  (`GATE-sword-convert.md`). BB1 reads the bottle's `Attack Speed: 2.0` the same way. The attribute reply is the value
  the server actually paces his swing with.

### BB3 — ONE HIT AT 100 AND ONE AT 200, EVERY TERM NAMED

Staging: a fresh NoAI zombie `t` at `~2`, `max_health` 200, at night. Enchants cleared. The two PROBEs before the hit are
the control, predicted EQUAL. EXECUTED: `MobScaling.maxHealth(200, false, true, 100)` = 1000.0, the same as MB4aR's
reading with this staging.

```
sent = ((18 x score/100) x enchantMultiplier + classDamageBonus + weaponLevelDamage) x chargeScale x critMultiplier
     = ((18 x 100/100)   x 1.0               + 0 (BB0's N)      + 0 (level 1, EXECUTED)) x 1.0 (40 ticks; full at 10) x 1 or 2
```

| score | PROBE `t` before (twice) | PLAYERHIT | PROBE `t` after |
|---|---|---|---|
| 100 | `customHP=1000.000/1000.000` | `source=broken_bottle/left_click … element=kinetic sent=18.000 crit=false triggerScore=100`, or `sent=36.000 crit=true` | below 1000.000 (by what its defence lets through; not predicted) |
| 200 | `customHP=1000.000/1000.000` | `sent=36.000 crit=false triggerScore=200`, or `sent=72.000 crit=true` | below 1000.000 |

- **enchantMultiplier 1.0:** `rpg enchant clear` leaves nothing active; Sharpness could roll and is not active.
- **The crit's chance at each hit** is BB2's `B + 10`. Whether it crits is the roll, so both readings are allowed.
- **Off-hand or stowed:** BB2. **A hit already in flight when he switches away:** for melee there is none. The crit is
  rolled at the swing (`BukkitCombatant.snapshot`), so it is not a row.

### BB4 — SHARPENED GLASS ON THE SHEET

| step | `rpg enchant show` | Crit Chance line |
|---|---|---|
| iron sword | — | `B₄` (15%) |
| the bottle, Sharpened Glass active at III | `  Slot 0: [*sharpened_glass III]`, `  active: sharpened_glass III (+15% Crit Chance)` | `B₄ + 25` (**40%**) |
| the same, at I | `  Slot 0: [*sharpened_glass I]`, `  active: sharpened_glass I (+5% Crit Chance)` | `B₄ + 15` (**30%**) |

EXECUTED: 0.15 + 0.10 + 0.15 prints `40%`, and 0.15 + 0.10 + 0.05 prints `30%`. The enchant's source
`critenchant:MAIN_HAND` and the bottle's `weapon:MAIN_HAND` are separate keys, so they ADD.

### BB5 — THE POOLS OVER 500 ROLLS

| weapon | `rpg enchant show` | `ROLLSTATS … weaponPool= uniquePool= normalPool=` | slot 0 | slot 1 |
|---|---|---|---|---|
| broken_bottle | `Slot 0: [sharpened_glass locked]`, `Slot 1: [kinetic_tremors locked]` | `element=kinetic rolls=500 weaponPool=1 uniquePool=1 normalPool=2` | `1=500` | `1=500` |
| iron_sword (control) | `Slot 0: []`, `Slot 1: [kinetic_tremors locked]` | `element=kinetic rolls=500 weaponPool=0 uniquePool=1 normalPool=2` | `0=500` | `1=500` |

- Each show names its weapon once, in order.
- The bottle's normal pool of two is `sharpness` and `unbreaking` (`SharpenedGlassContentTest`).
- **Unbreaking is offered though the bottle cannot wear: OPEN, see the plan's K6.**

### BB6 — NO SWEEP, WITH THE IRON SWORD AS THE CONTROL

Two NoAI zombies, `t` at `~2` and `u` at `~2 ~ ~1`, `max_health` 200, a full charge, one hit on `t`.
- **The bottle:** `u`'s three PROBEs read `customHP=1000.000/1000.000`, **all three EQUAL**. No sweep.
- **The iron sword, the same staging:** `u`'s two PROBEs before are EQUAL; its PROBE after reads **below them**, by no
  more than half of what `t` took (`SweepShare.of(primary, 0.5)`, EXECUTED: 11.5 of 23.0). Its PLAYERHIT `sent=23.000`
  or `46.000`.
- **If the control does not fall, the row is VOID, not a pass.** The staging could not show a sweep.

### BB7 — DURABILITY, AND THE SERVER'S VERDICT ON A RIGHT CLICK

The new driver line `USE result useItemInHand=<r> useInteractedBlock=<r>` is what every listener left of the synthetic
press. READ, the pinned API: the event starts at `DEFAULT` and, with no block, `useInteractedBlock=DENY`. **It is an
AUTO reading of the SERVER's verdict, never of what vanilla does next:** the press is synthetic, and no packet follows
it. BB8 reads the real click.

| held | `rpg durability …` reply | USE result |
|---|---|---|
| the Broken Bottle | `glass_bottle has no durability -- it can never break.` (K6, READ) | `useItemInHand=DENY useInteractedBlock=DENY` |
| a plain glass bottle (given into the held empty slot) | — | `useItemInHand=DEFAULT useInteractedBlock=DENY`; `ITEMS material=glass_bottle count=1 … ` with **no** `gear=` |
| an iron sword (control) | `250/250 uses left (damage 0)` (READ: `GATE-sword-tiers.md` STA3's `250/250`) | `useItemInHand=DEFAULT useInteractedBlock=DENY` |

After the bottle's press: `HELDID … item=broken_bottle`.

### BB8 — THE ONE ASK: THE WATER, BY HIS OWN CLICK

**Staging:**
- the blocks `1 -1 -2` to `5 1 2` are saved, and the run's restore puts them back;
- a glass ring at `~2..~4, ~-1..~1`, with a water SOURCE at `~3` (the ring holds it: the floor is solid and four
  sides are glass);
- the Broken Bottle in his hand, and a plain glass bottle in the next slot.

**Before the ASK:** `ITEMS material=glass_bottle count=2 … gear=1` and `ITEMS material=potion count=0`.

**The ASK (words):** *"Right-click the water in the glass ring with the Broken Bottle, then with the plain glass bottle.
Did the Broken Bottle stay a Broken Bottle while the plain one filled, and is its tooltip clear?"*
- **It is about whether it WORKS and is CLEAR. No tuning.** His answer is logged verbatim, not judged.

**After it:**
- `ITEMS material=glass_bottle count=1 … gear=1`: the minted bottle, unfilled, its tag kept;
- `ITEMS material=potion count=1`: the plain bottle became a water bottle, which is the control;
- `HELDID … item=<broken_bottle or none>`, depending on the slot he left selected. Not predicted.

**If the potion count is 0, the control did not fill, and the row is VOID for the bottle, not a pass.**

### THE THREE OWED ROWS, BY REFERENCE

**SM9R, SM16R2 and AS15R are predicted in `GATE-summoner.md` and `GATE-armor-sets.md`, as #308 re-predicted them, and
are read there.** This file restates none of their predictions; two accounts drift. They run after their own gates'
setups, which stage what they always staged. SM9R's SAFETY note is above.

## HOW LONG THE RUN IS

Counted from the steps' waits plus each row's fixed costs (the row-start clean, ~1 tick, and the row end, 3 ticks
after P-R2). It does not count a 1200-tick ASK timeout; he answers.

| part | ticks (waits) | about |
|---|---|---|
| broken-bottle setup | 40, plus the pad and preflight | 2 s |
| BB0-BB7 | 12 + 6 + 56 + 104 + 32 + 8 + 106 + 12 = 336 | 17 s |
| BB8 | 6, plus his two clicks and his answer | ~30 s (his) |
| summoner setup, SM9R, SM16R2 | 40 + 25 + 119 = 184 | 9 s |
| armor-sets setup, AS15R | 40 + 50 = 90 | 5 s |
| row ends and cleans | 12 rows x ~4 ticks | 2-3 s |
| **total** | about 706 ticks of the server, plus BB8 | **about 1 minute: ~35 s of the server, ~30 s his** |

Waits summed by `awk` over each row's `wait:` steps in the three scenario files. The three pad builds are not counted.

## CC'S CALLS IN THIS GATE

| call | chosen | the alternative |
|---|---|---|
| a reference's separator | `.` (a colon is not a `word()` character) | a quoted or greedy argument, which would break the second-word row form |
| a reference's row order | the gate's file order | the order typed |
| the water row | the ONE ASK, its outcome read AUTO by `probe_items` | AUTO only, which no verb can do: `use:` is synthetic and vanilla never sees it |
| the server's verdict | one new `USE result` line after the `use:` driver | none (then K7's hook has no AUTO witness on a server) |
| P-R2 | one more tick and a second drain inside the row end, which already waited 2 ticks and drained once (READ) | leave the row end as it was |

## PLACEHOLDERS OPEN FOR BEN (SEAT PLACEHOLDER 2026-11-25)

The plan's list: reach and arc, no flavor, the tooltip's crit wording, the enchant line's wording, the enchant's icon
(`glass_pane`), K6's Unbreaking.

## WHAT ONLY THE BOOT CAN SHOW

- that the reconcile really moves the stat with the hand (BB2);
- the hit's `sent` (BB3), the sweep's absence (BB6);
- the right-click verdict on the server (BB7) and the real fill (BB8);
- the attribute's printed value;
- that the three references run their rows as their own gates would.

## READINGS

None. Not booted.
