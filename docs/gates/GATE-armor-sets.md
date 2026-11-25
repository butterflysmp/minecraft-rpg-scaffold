# GATE — ARMOR SETS: AUTHORED DEFENSE, COPPER, THE SET BONUSES, THE FOUR COLUMNS ON ARMOR

**Status: NOT RUN.** Branch `feat/armor-sets-gate`, on #303 `feat/armor-sets-3` (the stack #300 → #301 → #302 → #303).
Written 2026-11-25; every prediction committed before any boot. **No boot without Ben's own "ready" and the seat's
approval of code and gate from origin** (`.claude/rules/booting.md`, ticked line by line). **GAME MODE: SURVIVAL, every
row.**

**The scenario is `paper/src/main/resources/selftest/GATE-armor-sets.yml`, typed after the summoner gate as ONE command:**

```
/rpg selftest summoner+armor-sets
```

**Summoner FIRST, so #300's owed rows are read:** SM1R, SM3AR, SM3DR, SM4R, SM14R, SM16R (predicted in
`GATE-summoner.md` on #300), and **SM7R** (P-1's re-prediction, added on this branch to `GATE-summoner.md`; the scenario's
SM7 key renamed). Every other summoner row is a regression read against its own prediction there. Armor-sets' rows are
AUTO except **AS16, the one ASK, last**. On the pad, facing east; nothing is asked of his ground, position or inventory.
**Lint: green** (`SelfTestScenariosTest`, `ScenarioLint`).

The plan is `docs/plans/PLAN-armor-sets.md`. **What this gate does NOT stage, and why, is under *NOT STAGEABLE*.**

## SELF-STAGING — EVERY ROW

Every row but AS12 to AS15 opens the same way:
- `clear_inventory` and `clear_state`;
- `rpg heal 1000000`;
- **all four armour slots emptied** (`item replace entity @s armor.<slot> with minecraft:air`). `clear_inventory`
  leaves armour alone, READ in its javadoc: *"Armour and the off hand are not storage slots and are not touched."*
- a 10-tick wait, then a **BASELINE `rpg stats`**, read before any piece is worn.

It ends with the four slots emptied again. **Every stat prediction below is a DELTA against that row's own baseline.**
So his accessories, fragments or anything else he carries cannot move a reading.

- **Wearing a minted piece:**
  1. `hold: empty`;
  2. `rpg give <piece>` (the give lands in the held empty slot, READ: EC4R2's staging, read as predicted on its boot);
  3. `probe_held` (the witness that it did);
  4. `rpg gearscore set <score>`;
  5. `dump: held` where the row reads the tooltip;
  6. `item replace entity @s armor.<slot> from entity @s weapon.mainhand`;
  7. the hand emptied.

  **INFERRED:** that vanilla's `item replace … from` copies the whole stack, our `armor_id` included. If it did not,
  the piece would read as UNTAGGED. Under A3 an untagged piece has the same Defense and set at score 100, so the
  Defense rows would pass anyway, but AS6's 200 would read 48, not 96. **So AS6 is the witness for this staging.**
- **An untagged piece:** `item replace entity @s armor.<slot> with minecraft:<item>`.
- **The reconcile:** every 5 ticks (READ, `RECONCILE_PERIOD_TICKS = 5`), so each read waits 10 ticks, two periods.

## WHAT WAS READ OR EXECUTED, EACH MARKED

- **The stats sheet** — READ, `StatsSheet.build`: one REPLY line each for Max Health (`capacity`, a trimmed number),
  Health Regen and Mana Regen (`perFiveSeconds`: per-second × 5, two decimals, `/5s`), Max Mana, and Defense
  (`capacity`). Labels are padded to 13 (`LABEL_WIDTH`); padding is not part of a prediction.
- **The bases** — READ:
  - health regen `HealthRegen.BASE_PER_SECOND = 0.2` (`1.00/5s`);
  - mana regen `MANA_PER_TICK = 100 / (100 × 20)` = 1/s (`5.00/5s`);
  - max health `CombatantStats.DEFAULT_PLAYER_BASE = 100`;
  - Defense 0;
  - the level bonus at level 1 is 0 on all four (READ, `LevelBonus.at(1)`). The setup stages level 1.
- **EXECUTED (jshell, against `core/target/classes`):**
  - `0.6 × 5` = `3.00` and `3.0 × 5` = `15.00` (`%.2f`);
  - `Defense.applyDefense(10, 112)` = **4.716981132075472**, which prints `4.717`;
  - `Defense.applyDefense(10, 0)` = **10.0**.
- **The HURT line** — READ, `SelfTestRun.hurt`: `HURT before=<amount> after=<dealt> defense=<his Defense> dealer=self
  dealerType=self …` for `/rpg damage`. The command is `damageSelf`, which calls `CombatantStats.damage(id, amount, id,
  true)` (READ).
- **A tooltip** — READ, `ArmorLore.build` and `EnchantLore.lines`. Only ACTIVE enchants render, and a fresh give has
  none active, so each piece's lore is exactly:
  1. `Gear Score: <s>`;
  2. `Defense: <d>`;
  3. for a set with a bonus: a blank, `Set Bonus (4 pieces):`, and one line per entry;
  4. a blank, the flavor, a blank, and `<Rarity> <Slot noun>`.
  The `dump: held` line is `HELD <id> <MATERIAL> x1 name=… lore=[…]`.
- **The summon cap** — READ, `SummonService.cast`:
  - `SUMMON refused owner=<him> weapon=bone_whistle verdict=<v> max=<n>`, plus the notice REPLY
    `"You cannot summon more (max <n>)."`;
  - a refusal costs no mana and starts no cooldown.
  - `rpg maxsummons` replies `max_summons: <n>`.
- **The run's restore** puts back his inventory (armour included: `PlayerInventory.getContents`, READ in the paper-api
  javadoc, *"Returns all ItemStacks from the inventory"*), his health, the world's difficulty and the `max_summons`
  base (READ, `GATE-summoner.md`).
- **The armor key** is `rpg:armor_id`. READ: `Keys` builds it as `new NamespacedKey(plugin, "armor_id")` and the plugin
  is named `Rpg`. That the namespace is lowercased is INFERRED. AS14's give writes it under `PublicBukkitValues` in the
  item's `custom_data` (INFERRED, Paper's PDC storage).
- **`shift_click` widened (CC'S CALL, this branch):** the driver now matches an `armor_id` as well as a `weapon_id`, so
  AS13 can place a helmet in the table. `SelfTestWeaponIdsTest` accepts an armor id there.

## SAFETY: THE MOST THE RUN CAN DEAL HIM — A SUM, BEFORE THE ROWS (S-1 TO S-3)

**The cap: 20.000 over the whole `summoner+armor-sets` run.** No mob acts on him in armor-sets. AS4's wolf is ours, and
SUMMON → PLAYER is a refused cell (READ). No hostile is spawned in any armor-sets row.

| row | source | every term, EXECUTED | most dealt |
|---|---|---|---|
| summoner, all of it | `GATE-summoner.md`'s sum (SM3AR's 0.002 × 5.0 × 100/100) | executed there | **0.010** |
| AS9 | ONE `/rpg damage 10`, dealer himself | `applyDefense(10, D)`, D his staged Defense. At 112 it is 4.716981132075472. **At the worst bound, his Defense absent, `applyDefense(10, 0)` = 10.0** | **10.000** |
| every other armor-sets row | nothing touches his health | — | **0** |

**Sum, at every term's worst: 10.010 ≤ 20.000.**
- **S-3:** the dealer is himself, so no mob score is a term.
- **S-2:** AS9 heals him full before and after the one hit.
- **S-1:** both of AS9's figures are executed.

## CARRIED STATE (CC, IN THIS ORDER) — A CHECKLIST, EACH LINE TICKED IN THE REPORT

1. No `java` process; the heads equal origin (`git ls-remote --heads origin feat/armor-sets-gate`).
2. `git status --porcelain` empty; `git rev-parse HEAD` equals the remote head.
3. The world's saved difficulty read and reported; the setups set EASY and the restore puts it back.
4. `booting.md`, every step.

## THE RUN SHEET — BEN TYPES ONE LINE

He is told **R0 passed**, logs in, stands anywhere, types `/rpg selftest summoner+armor-sets`, answers SM17's and AS16's
questions, waits for `SELFTEST armor-sets END`, then `/stop`.

## HOW LONG THE RUN IS

- **Armor-sets' waits sum to 918 ticks (45.9 s)**
  (`tr -d '\r' < GATE-armor-sets.yml | awk '/^  - wait: /{s+=$3} END {print s}'`), plus AS16's ask (up to 1200 ticks,
  60 s, or until he answers). 687 steps (`grep -c '^  - '`); steps that are not waits share their pass's tick (READ,
  `advance`).
- **Summoner's waits: 1473 ticks (73.65 s)** (its own gate), plus SM17's ask.
- **About 3 to 5 minutes in all (ESTIMATE:** the row-boundary clean and settle, and the two asks' answer times, are not
  measured). The run's `START` and `END` lines give the real figure.

## R0 — THE BUILD. READ IT FIRST. IF ANY R0 FAILS, STOP

### R0a — the build line names the stack top's head, clean

`[Rpg] Build: <sha>`, `<sha>` being `feat/armor-sets-gate`'s head as `git ls-remote --heads origin feat/armor-sets-gate`
prints it, shortened. **Not `-dirty`, not `unknown`.** Instrument: `Select-String -Path run\logs\latest.log -Pattern
'\[Rpg\] Build:'`.

### R0b — the jar carries the stack and this gate

`docs/gates/GATE-armor-sets-r0b.ps1`. **Prediction, line for line:**
- before `'--- absent'`: **30 `PRESENT`, 0 `ABSENT`**;
- between `'--- absent'` and `'--- controls'`: **4 `ABSENT`, 0 `PRESENT`**;
- after `'--- controls'`: **4 `ABSENT`, 0 `PRESENT`**.

Counted from the script: 19 `ValueLine` + 7 `Entry` + 4 `Has` = 30, then 4 and 4. **It was run before commit on a jar
built from this tree; see the PR body.**

### R0c — every Loaded line, and no `[Rpg]` WARN

- `[Rpg] Loaded 11 abilities, 44 visuals, 5 statuses, 7 elements, 23 enchants, …, 24 weapons, 1 shields, 28 armor, …`
  (`ls` at this branch: 11, 44, 5, 7, 23, 24, 1; armor `grep -h "^    material:" content/armor/*.yml | wc -l` = 28).
- `[Rpg] Loaded 1 summon(s)`.
- `Mint-on-craft: 42 indexed, 42 claiming, of 58 gear definitions` (`BootCountLinesTest`).
- **No `ArmorConsistency` line at all.** The coverage check is clean: READ, `ArmorMaterials` has LEATHER, COPPER,
  CHAINMAIL, IRON, GOLD, DIAMOND, TURTLE_SCUTE, NETHERITE and ARMADILLO_SCUTE; the turtle helmet is excluded; the wolf
  armour's slot is BODY.
- **0** `WARN]: [Rpg]` lines (control: `INFO]: [Rpg] Loaded` 1 or more). None of `Refusing`, `Skipping`, `SEVERE`,
  `Exception`.

### R0d — every scenario loaded

Exactly one **`[Rpg] Selftest: dev server, 436 scenarios across 49 gates [melee-cell, …, summoner, armor-sets]`**.
Recount: 49 files, 485 keys, 49 setups; control 467 + 18 = 485. Controls: `419 scenarios` and `435 scenarios` ABSENT.

### R0e — the orphan sweep with nothing staged

One `FIGHTWORLD sweep path=…\rpgfight folders=0`, no `deleting`, no `orphan`.

## THE ROWS

Each row's lines are those between `SELFTEST armor-sets <row> START` and its `DONE`. **Clock: the server tick.** In each
row, `B` names that row's first (bare) `rpg stats` reading:
- `B.D` Defense;
- `B.HR` Health Regen;
- `B.MR` Mana Regen;
- `B.MH` Max Health.

**AS0 predicts the bare values** for a player wearing nothing that grants them. If AS0 reads otherwise, every later row
still reads, by its deltas.

**Each tooltip block (`dump: held`), for the seven sets.** The lore lines in order, with `<d>` the piece's Defense at
the row's score:

| set | `<d>` at 100: head / chest / legs / feet | set lines after `Defense: <d>` | flavor | footer rarity |
|---|---|---|---|---|
| leather | 5 / 8 / 7 / 4 | `""`, `Set Bonus (4 pieces):`, `+2 Health Regen per 5s` | `Better than nothing, and it knows it.` | Common |
| golden | 5 / 8 / 7 / 4 | `""`, `Set Bonus (4 pieces):`, `+2 Mana Regen per second` | `It buys goodwill from things that cannot spend it.` | Common |
| chainmail | 5 / 8 / 7 / 4 | **none** | `Ten thousand rings, and every one of them someone's afternoon.` | Common |
| copper | 5 / 8 / 7 / 4 | `""`, `Set Bonus (4 pieces):`, `+1 Max Summons` | `It greens where it is not worn.` | Common |
| iron | 10 / 16 / 14 / 8 | `""`, `Set Bonus (4 pieces):`, `+100 Max Health` | `Honest plate. It has no opinion about who wears it.` | Common |
| diamond | 15 / 24 / 21 / 12 | `""`, `Set Bonus (4 pieces):`, `+20 Defense` | `Cut from a single lattice, and cold to the touch.` | Uncommon |
| netherite | 15 / 24 / 21 / 12 | `""`, `Set Bonus (4 pieces):`, `+40 Defense` | `Salvaged from something that did not survive the fall.` | Uncommon |

Each block: `Gear Score: 100`, then `Defense: <d>`, then the set lines, then `""`, the flavor, `""`, and `<rarity>
<Helmet|Chestplate|Leggings|Boots>`. The set lines are the same on all four pieces (B8, static). The name is the piece's
display name (leather's are `Leather Cap`, `Leather Tunic`, `Leather Pants`, `Leather Boots`). A block with `Defense: 3`
on a diamond helmet, or no set lines on an iron piece, is a FAIL reading.

### AS0 — THE BASELINE

Bare, at level 1, `rpg stats`:
- `Max Health 100`;
- `Health Regen 1.00/5s`;
- `Mana Regen 5.00/5s`;
- `Defense 0`.

These hold if nothing he carries grants them; the summoner boot read `defense=0.000` with cleared hands. Also
`max_summons: 1`, and `SELF customHP=<B.MH>/<B.MH>`.

### AS1 — LEATHER

- The four leather tooltip blocks, as the table.
- **Full set:** `Defense <B.D + 24>`, **`Health Regen <B.HR + 2.00>/5s`**: `3.00/5s` from the bare `1.00/5s` (EXECUTED).
  Mana Regen and Max Health equal B.
- **The head swapped for an untagged iron helmet:** `Defense <B.D + 29>` (10 + 8 + 7 + 4) and `Health Regen <B.HR>`. The
  bonus is OFF.

### AS2 — GOLDEN

- The four golden blocks.
- **Full set:** `Defense <B.D + 24>`, **`Mana Regen <B.MR + 10.00>/5s`**: `15.00/5s` from `5.00/5s` (2/s × 5, EXECUTED).
  Health Regen and Max Health equal B.
- **Head swapped (untagged iron helmet):** `Defense <B.D + 29>` and `Mana Regen <B.MR>`.

### AS3 — CHAINMAIL: NO BONUS, BECAUSE ITS BONUS IS STOPPED

The four chainmail blocks, with **no set lines**. Full set: `Defense <B.D + 24>`; Health Regen, Mana Regen and Max
Health all equal B. **This is the control row: a set with no bonus moves nothing but Defense.**

### AS4 — COPPER: THE SECOND SUMMON SLOT

The base is staged 0 (`rpg maxsummons 0`, `max_summons: 0 (base set to 0)`). This is **CC'S CALL**: only one summon
weapon ships, so "a second summon NEW where one alone is REFUSED_FULL" is staged as **the wolf itself, refused at a
cap of 0 and NEW at 0 + 1**. The arithmetic is the same: the set's +1 is the only thing that admits the cast. The
fixture summon lives only in test content and cannot be loaded on the server.
1. **Bare press:** `SUMMON refused … weapon=bone_whistle verdict=REFUSED_FULL max=0`, REPLY `"You cannot summon more
   (max 0)."`, and `SUMMONPROBE none owner=<him> store=0`.
2. The four copper blocks. Full set: `Defense <B.D + 24>`; then **`max_summons: 1`**.
3. **Press with the set:** `SUMMON spawn … summon=bone_whistle_wolf score=100 health=100.000 damage=20.000`, and the
   probe `alive=true health=100.000/100.000 … within=8:true`. The first press was refused, so it charged no cooldown
   (READ).
4. **The head swapped:** `max_summons: 0`, and **the probe still `alive=true`** (B6: nothing removed).
5. **After 200 ticks** (the whistle's cooldown, from press 3; 215 ticks elapse), the press reads `verdict=REFUSED_FULL
   max=0`. The others count is 0 and 0 + 1 > 0, so the next cast obeys the cap. The probe still shows the one wolf,
   `alive=true`.
6. Then the base is reset, and `clear_state` removes the wolf (`cause=clear`).

### AS5 — IRON: MAX HEALTH, HEADROOM THEN CLAMP

1. `SELF customHP=<B.MH>/<B.MH>` (healed full).
2. The four iron blocks. Full set: `Defense <B.D + 48>`, `Max Health <B.MH + 100>`.
3. **Headroom:** `SELF customHP=<c>/<B.MH + 100>`, with **`B.MH ≤ c ≤ B.MH + R`**. `R` is the regeneration paid in the
   wait: at most one window of the saturated rate, `HealthRegen.healAmount` at `B.HR × 5`, about 1.0 (ESTIMATE; his food
   is not staged). **`c` reading `B.MH + 100` is the FAIL** (a heal on equip).
4. Healed: `SELF customHP=<B.MH + 100>/<B.MH + 100>`.
5. **The head swapped for an untagged leather helmet:** **`SELF customHP=<B.MH>/<B.MH>`**, EXACTLY: the clamp, and no
   regeneration above a full max. `rpg stats`: `Max Health <B.MH>`, `Defense <B.D + 43>` (5 + 16 + 14 + 8).

### AS6 — IRON AT SCORE 200 (THE SECOND SCORE; B4 FLAT)

- The four blocks read `Gear Score: 200` and **`Defense: 20 / 32 / 28 / 16`** (authored × 200/100).
- Full set: **`Defense <B.D + 96>`** and **`Max Health <B.MH + 100>`**. The bonus is not doubled (B4).
- **96 is the witness that the worn copies are TAGGED.** An untagged copy reads at base score: 48.

### AS7 — DIAMOND: 72 + 20

- The four diamond blocks.
- Full set: **`Defense <B.D + 92>`**.
- **Head swapped (untagged iron helmet):** **`Defense <B.D + 67>`** (10 + 24 + 21 + 12, the 20 off).

### AS8 — NETHERITE: 72 + 40

- The four netherite blocks.
- Full set: **`Defense <B.D + 112>`**.
- **Head swapped:** **`Defense <B.D + 67>`**.

### AS9 — THE MITIGATION (SAFETY's one hit)

Full netherite, no dumps: `Defense <B.D + 112>`. Healed, then `rpg damage 10`:
- **`HURT before=10.000 after=<applyDefense(10, B.D + 112)> defense=<B.D + 112>.000 dealer=self dealerType=self`.**
- **At B.D = 0: `after=4.717 defense=112.000`** (EXECUTED: 4.716981132075472). `after=10.000` is the FAIL: no
  mitigation. So is `after=8.333`: the old full diamond, `applyDefense(10, 20)`.
- REPLY `"Custom HP: <B.MH − 5>/<B.MH>"`, rounded by `%.0f`.

### AS10 — UNTAGGED DIAMOND (A3)

Four vanilla diamond pieces, none of ours: **`Defense <B.D + 92>`**. That is the same 72 as minted pieces, AND the set's
20. A reading of `B.D + 20` would be vanilla's points with the set; `B.D + 72` would be the Defense with no set. Both
are FAILs.

### AS11 — MIXED NETHERITE (A3)

Two minted (head, chest) and two untagged (legs, feet): **`Defense <B.D + 112>`**, one set.

### AS12 — A HELMET'S POOLS (E3)

- `HELDID slot=<n> item=iron_helmet`.
- REPLY `"Iron Helmet -- 4 slot(s)"`, `Slot 0: []`, `Slot 1: []`.
- Slots 2 and 3: each 1 to 4 of `growth`/`mana_bank`/`protection`/`unbreaking`, distinct (the roll's draw).
- **`Pool: growth, mana_bank, protection, unbreaking`, `Unique pool: `, `Weapon pool: `.** The weapon pool line is
  empty: slot 0's key is now `armor_slot:head`, and no enchant declares it (E2).

### AS13 — THE TABLE ON A HELMET, AT LEVEL 13

1. The level REPLY.
2. `DRIVE shift-click … bottom slot=<n> iron_helmet x1`.
3. The menu dump:
   - **`SLOT 10 GRAY_DYE name="No weapon enchant"`** with `lore=["Nothing is tied to this weapon yet."]`;
   - **`SLOT 12 GRAY_DYE name="No unique enchant"`** with `lore=["Nothing unique is tied to this item's element
     yet."]`;
   - the normal columns at 14 and 16, from the four.
   - The marker wording is the weapon's, as E2 ruled. That a helmet's slot-0 marker says "weapon" is **OPEN FOR BEN
     (O8)**.

### AS14 — A PRE-CHANGE PIECE, REFRESHED (F1)

The give writes a tagged iron helmet carrying slice 1's lore: `Defense: 2`, no score, no set lines.
1. **Before:** `HELD iron_helmet IRON_HELMET x1 … lore=["Defense: 2"]`.
2. **After `rpg refresh`** (the join's own `GearRefresher.refresh`, READ): `HELD iron_helmet … lore=["Defense: 10", "",
   "Set Bonus (4 pieces):", "+100 Max Health", "", "Honest plate. It has no opinion about who wears it.", "", "Common
   Helmet"]`. No `Gear Score` line (INFERRED: remint carries the score the item has, and it has none). A `Gear Score` line there is not a FAIL of F1; the Defense line then equals `10 × score/100`.
3. **If the first dump reads `HELD - IRON_HELMET`** (no id), the give's component syntax did not tag it (INFERRED). Then
   the row DID NOT TEST, and only a rejoin with a pre-change item can show it.

### AS15 — COPPER CRAFTED MINTS (C1)

Five copper ingots and the Recipe Book (crafting unlocks at level 3; AS13 left him at 13):
1. The menu dump shows `Copper Helmet`.
2. The click: `CRAFT minted … copper_helmet`.
3. Held: `HELD copper_helmet COPPER_HELMET x1 name="Copper Helmet" lore=["Gear Score: <stamped>", "Defense: <5 ×
   stamped/100>", "", "Set Bonus (4 pieces):", "+1 Max Summons", "", "It greens where it is not worn.", "", "Common
   Helmet"]`.
4. The stamped score is read from its own line. The Defense line must equal `5 × score/100`.

### AS16 — THE ONE ASK, LAST

A full iron set is worn, then the question: *"Hover a piece of your iron armour: is the Set Bonus clear, and does the
+100 Max Health show on your stats? (yes / no / not sure)"*.
- **It asks whether it WORKS and is CLEAR.** His answer is recorded verbatim.
- **AUTO cannot witness** how the tooltip renders on his client, or how the stats screen reads to him (verification.md:
  an AUTO reading is a reading of the server).

## NOT STAGEABLE (AND WHY)

- **Chainmail's quiver on the Breacher (2 → 3) and Dragon's Breath (5 → 8): NOT STAGED.** The bonus is STOPPED
  (`PLAN-armor-sets.md` 3.3: no per-player path for a percent). AS3 reads that nothing moves.
- **Wear: a piece losing 1 per priced hit, breaking at 0, then giving no Defense while its set stays: NOT STAGED.**
  D1-D3 are STOPPED (`PLAN-armor-sets.md` 4): armor has no broken state, and vanilla still wears it.
- **What only the boot can show:**
  - the client's tooltip render and the stats screen;
  - that `item replace … from` keeps the tag (AS6 witnesses it);
  - that AS14's give tags the item;
  - the recipe click (AS15);
  - the summon cast against a live cap (AS4).
