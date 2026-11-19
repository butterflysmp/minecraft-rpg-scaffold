# GATE — THE DEV ROLL COMMAND (PLAN-loot.md, THE WIRING ORDER'S FIRST STEP; THE SEAT'S QUEUE, ITEM 6)

**Status: NOT RUN, and NO BOOT NOW.** Written 2026-11-19 on `feat/loot-cascade` (on master `868df417`), before any
boot. **It boots with a later stack, after #219 is rebased onto master** (the seat's ruling: #219 waits for #216, #217
and #218). At that boot its R0 rows join that boot's R0, and the recount is redone on that tree.

**Game mode:** SURVIVAL. **Difficulty:** any (nothing here fights). **Nothing here can hurt him or give him anything:**
`/rpg loot` rolls and tells, and gives nothing.

**Ben's rulings it stands on** (PLAN-loot.md, its head, verbatim):
- "First the rarest rarity is rolled. then if that doesn't land, the next rarest rarity is rolled and so on";
- "If a rarity lands but that source has no item general skip that rarity";
- "Yes equally likely across every item";
- the wiring order, "That's fine".

## WHAT IT IS

- **`/rpg loot roll <source|default> [count]`:** dev only (`Permissions.DEV` AND the dev-server marker, as
  `/rpg mining` is). It rolls that table `count` times (1 by default, at most 100,000, `LootRolls.MAX_ROLLS`) with a
  real random source (`new SplittableRandom()`).
  - **It tells the caller, per tier:** how many gave (or landed, for the default), how many were skipped for an empty
    pool, and the true odds; how many rolls gave nothing; and, for a source, every item picked with its count.
  - **One `LOOTROLL` log line per call** carries the same figures.
  - **It gives nothing:** `LootDevCommandTest` scans the class for any inventory, world or give path.
- **`/rpg loot odds <source|default>`:** the written chance and the true odds per tier.
- **The example source, `dev_example_not_a_drop`:** under `dev_sources:` in `content/loot/cascade.yml`, loaded ONLY
  on a dev server.
  - Every dev source starts `dev_`, and no real source may, so it cannot be mistaken for a drop.
  - Its pools are existing items, each under its own rarity: exotic 1, legendary 3, rare 6, uncommon 7, common 2.
  - **No epic pool** (no content is epic), so an epic that lands is skipped. The deletion set's dev weapons are left
    out.

## R0 (WITH THE LATER BOOT'S)

### R0c-L — the loot Loaded line

| prediction | instrument | READING |
|---|---|---|
| Exactly one **`[Rpg] Loaded loot 'cascade': default exotic 1.00% (truly 1.000%), legendary 3.00% (truly 2.970%), epic 8.00% (truly 7.682%), rare 12.00% (truly 10.602%), uncommon 20.00% (truly 15.549%), common 40.00% (truly 24.879%), nothing truly 37.318%; 1 sources [dev_example_not_a_drop]`** (a dev server; `LootLoaderTest` asserts it verbatim). No `Refusing loot` | `Select-String run\logs\latest.log -Pattern 'Loaded loot\|Refusing loot'` | |

### R0b-L — the jar carries it

| prediction | instrument | READING |
|---|---|---|
| PRESENT: `content/loot/cascade.yml` line `  dev_example_not_a_drop:`; the entries `io/github/butterflysmp/rpg/core/loot/CascadeTable.class` and `…/LootRolls.class`; `io/github/butterflysmp/rpg/paper/command/LootDevCommand.class` carrying `LOOTROLL`; `GATE-loot-roll.yml` carrying `LR2:`. ABSENT (the control): `cascade.yml` line `  dev_try_me:` | the later boot's R0b PowerShell, with these lines added | |

## THE ROWS

### LR1 — the odds, written and true (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| (1) `rpg loot odds default`: REPLY `Loot odds 'default':`, then `  exotic: written 1.00%, truly 1.000%`, `  legendary: written 3.00%, truly 2.970%`, `  epic: written 8.00%, truly 7.682%`, `  rare: written 12.00%, truly 10.602%`, `  uncommon: written 20.00%, truly 15.549%`, `  common: written 40.00%, truly 24.879%`, `  nothing: truly 37.318%`. (2) `rpg loot odds dev_example_not_a_drop`: the same lines under its name (it uses the default's tiers) | `CascadeTableTest` *theTrueOdds…*; `LootDevCommandTest` *theOddsLines…* | |

### LR2 — 10,000 rolls of the example source: each tier near its true odds, epic always skipped, nothing given (SURVIVAL)

**The expected counts, computed** (10,000 rolls). Each tolerance is 4 standard deviations of a binomial, rounded inwards.
**Every epic that lands is SKIPPED and the roll goes on**, so the tiers below epic give more often than their table odds:
- a roll reaches rare with probability 0.99 x 0.97 = 0.9603, epic counting as a miss;
- the command still prints each tier's TABLE true odds beside its count, and the gap between them is the skip's.

| tier | what is counted | p (computed) | expected | 4 sigma | accepted range |
|---|---|---|---|---|---|
| exotic | gave | 0.010000 | 100.0 | 39.8 | 61..139 |
| legendary | gave | 0.029700 | 297.0 | 67.9 | 230..364 |
| epic | **gave 0; SKIPPED** | 0.076824 | 768.2 skipped | 106.5 | **gave exactly 0; skipped 662..874 (above zero)** |
| rare | gave | 0.115236 | 1152.4 | 127.7 | 1025..1280 |
| uncommon | gave | 0.169013 | 1690.1 | 149.9 | 1541..1840 |
| common | gave | 0.270420 | 2704.2 | 177.7 | 2527..2881 |
| nothing | gave nothing | 0.405631 | 4056.3 | 196.4 | 3860..4252 |

| prediction | stands on | READING |
|---|---|---|
| (1) Before: **`INVENTORY volley_stone slots=<s1>`** and **`INVENTORY locust slots=<s2>`** (whatever he carries). (2) The reply begins **`Loot roll 'dev_example_not_a_drop' x10000 (nothing given to anyone):`**, and each tier's line carries a count in the range above, `skipped 0` for every tier but epic, and its table true odds. (3) **One `LOOTROLL table=dev_example_not_a_drop rolls=10000 …` line**, ending **`given_to_players=0 picked={…}`**: the picked items sum to the gave counts (all but nothing's), each item under its own rarity's pool. (4) After: **the same two `INVENTORY` lines as before, unchanged** — the roll gave nothing | `LootTallyTest`, `LootRollsTest`, `LootDevCommandTest` *theCommandGivesNothing* | |

### LR3 — 10,000 rolls of the default table: landings near the table's true odds, nothing skipped (SURVIVAL)

The expected counts, computed (no pools, so every landing counts and nothing is skipped). Each tolerance is 4 sigma,
rounded inwards:

| tier | p (the table's true odds) | expected | accepted range |
|---|---|---|---|
| exotic | 0.010000 | 100.0 | 61..139 |
| legendary | 0.029700 | 297.0 | 230..364 |
| epic | 0.076824 | 768.2 | 662..874 |
| rare | 0.106017 | 1060.2 | 938..1183 |
| uncommon | 0.155492 | 1554.9 | 1410..1699 |
| common | 0.248787 | 2487.9 | 2315..2660 |
| nothing | 0.373180 | 3731.8 | 3539..3925 |

| prediction | stands on | READING |
|---|---|---|
| REPLY `Loot roll 'default' x10000 (nothing given to anyone):`. Each tier reads **`landed <n>`** in its range, with **`skipped 0`**; nothing in its range. **Against LR2: epic LANDS here and is skipped there**, and the tiers below it give more there. The pair shows the skip. One `LOOTROLL table=default rolls=10000 … items=0 given_to_players=0` line, with no `picked=` | `LootRollsTest` *withoutPoolsItTalliesRarities* | |

### LR4 — one roll when no count is given (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| REPLY `Loot roll 'dev_example_not_a_drop' x1 (nothing given to anyone):`. Its tier counts sum, with nothing's, to 1; one `LOOTROLL … rolls=1 …` line | `LootDevCommand` (the default count, 1) | |

### LR5 — an unknown table is refused (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| REPLY **`No loot table 'no_such_table'. Tables: [default, dev_example_not_a_drop]`**, and no `LOOTROLL` line | `LootDevCommandTest` *defaultIsTheDefaultTable…* | |

## WHAT THIS GATE DOES NOT READ

- **Any real drop source:** none exists; `sources: {}` ships empty.
- **The uniform pick's evenness within a pool:** the `picked=` counts show it, and no range is predicted. A pool of 6
  over 1152 rare rolls is about 192 each.
- **A non-dev server:** there the example is not loaded and the command is absent (`LootLoaderTest`).
