# GATE — ARMOR SETS: AUTHORED DEFENSE, COPPER, THE SET BONUSES, THE FOUR COLUMNS ON ARMOR

**Status: NOT RUN.** Branch `feat/armor-sets-gate`, on #307 `feat/armor-sets-w1` (the stack #300 → #301 → #302 → #303 → #305 → #306 → #307; extended
in place 2026-11-25 for the seat's re-rulings, before any approval or boot).
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
AUTO except **AS16, the one ASK, last** (the scenario lists AS17 to AS19 before it, so the ask still ends the run). On the pad, facing east; nothing is asked of his ground, position or inventory.
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
- **The run's restore** puts back his inventory, his health, the world's difficulty and the `max_summons` base (READ,
  `GATE-summoner.md`). **HIS ARMOUR AND OFF-HAND, W1 (the seat, 2026-11-25), are now captured and restored BY NAME and
  READ BACK** (`SelfTestWornSlots`, #307), at every gate boundary and in the final restore (the one funnel for END,
  every abort, stop, quit and disable). They no longer rest on a javadoc sentence.
  - READ, javap on the pinned server jar: `CraftInventoryPlayer` inherits `getContents` and `setContents` from
    `CraftInventory`.
  - NMS `Inventory.getContents` is 43 entries: storage 0–35, feet 36, legs 37, chest 38, head 39, off-hand 40, body
    41, saddle 42 (`EQUIPMENT_SLOT_MAPPING`, `EquipmentSlot.getIndex(36)` = 36 + index, FEET 0 … HEAD 3).
  - `setContents` writes every index `0..getSize()`.
  - So the array copy already carried the five, and the explicit path does not depend on it.
- **The armor key** is `rpg:armor_id`. READ: `Keys` builds it as `new NamespacedKey(plugin, "armor_id")` and the plugin
  is named `Rpg`. That the namespace is lowercased is INFERRED. AS14's give writes it under `PublicBukkitValues` in the
  item's `custom_data` (INFERRED, Paper's PDC storage).
- **`probe_armor` added (CC'S CALL, an instrument only, 2026-11-25):** one `ARMOR slot=… item=… damage=… max=… broken=…`
  line per worn slot, read through the weapons' own helpers (`WeaponDurability.damageOf`, `maxOf`, `isBroken`). It writes
  nothing.
- **`shift_click` widened (CC'S CALL, this branch):** the driver now matches an `armor_id` as well as a `weapon_id`, so
  AS13 can place a helmet in the table. `SelfTestWeaponIdsTest` accepts an armor id there.

## SAFETY: THE MOST THE RUN CAN DEAL HIM — A SUM, BEFORE THE ROWS (S-1 TO S-3)

**The cap: 20.000 over the whole `summoner+armor-sets` run.** No mob ACTS on him in armor-sets: AS4's wolf is ours, and
SUMMON → PLAYER is a refused cell (READ). The one hostile, AS18's `s18`, is a FROZEN (NoAI) zombie whose only hit is a
scripted one, staged exactly as SM3AR (its attack 0.002), and it is killed in its row (and by any abort's restore, READ
`GATE-summoner.md` X8).

| row | source | every term, EXECUTED | most dealt |
|---|---|---|---|
| summoner, all of it | `GATE-summoner.md`'s sum (SM3AR's 0.002 × 5.0 × 100/100) | executed there | **0.010** |
| AS9 | ONE `/rpg damage 10`, dealer himself | `applyDefense(10, D)`, D his staged Defense. At 112 it is 4.716981132075472. **At the worst bound, his Defense absent, `applyDefense(10, 0)` = 10.0** | **10.000** |
| AS18 | ONE `/rpg damage 1`, dealer himself | `applyDefense(1, 48)` = 0.6756756756756757; worst bound `applyDefense(1, 0)` = 1.0 | **1.000** |
| AS18 | ONE scripted `damage @s 0.2 mob_attack by s18`, priced from `s18`'s seeded attack (READ, R1) | `MobScaling.attackDamage(0.002, false, true, 100)` = 0.01, through `applyDefense(0.01, 48)` = 0.006756756756756757. S-3: the pad's worst score is 100 (READ, `GATE-summoner.md`): 0.01 | **0.010** |
| AS19 | TWO `/rpg damage 1`, dealer himself | `applyDefense(1, 48)` = 0.6756756756756757, then `applyDefense(1, 32)` = 0.7575757575757576; worst bound 1.0 each | **2.000** |
| every other armor-sets row | nothing touches his health | — | **0** |

**Sum, at every term's worst: 0.010 + 10.000 + 1.000 + 0.010 + 2.000 = 13.020 ≤ 20.000.**
- **S-3:** AS9's and AS19's dealer is himself, so no mob score is a term; AS18's mob term is at the pad's worst score.
- **S-2:** each wounding row heals him full before and after each hit.
- **S-1:** every figure above is executed (jshell, against `core/target/classes`).
- **Wear is not a SAFETY term:** it moves his items, never his health. Every hit above also wears his worn pieces by 1
  (D2'), and the run's restore puts his own inventory back, armour included.
- **A JVM killed mid-run** reaches no restore. His worn armour is then in the pre-boot backup that `booting.md`'s
  carried state takes. READ: this 26.1 server keeps player data at `world/players/data/<uuid>.dat` (not `playerdata/`),
  and the summoner boot's backup `rpg-run-backups\2026-11-25-pre-summoner\world\players\data\` holds his
  `b6ae27e9-….dat`. That the vanilla `.dat` holds the armour slots is INFERRED.

## CARRIED STATE (CC, IN THIS ORDER) — A CHECKLIST, EACH LINE TICKED IN THE REPORT

1. No `java` process; the heads equal origin (`git ls-remote --heads origin feat/armor-sets-gate`).
2. `git status --porcelain` empty; `git rev-parse HEAD` equals the remote head.
3. The world's saved difficulty read and reported; the setups set EASY and the restore puts it back.
4. `booting.md`, every step.

## THE RUN SHEET — BEN TYPES ONE LINE

He is told **R0 passed**, logs in, stands anywhere, types `/rpg selftest summoner+armor-sets`, answers SM17's and AS16's
questions, waits for `SELFTEST armor-sets END`, then `/stop`.

**W1, HIS OWN ARMOUR, READ AT THE RUN'S EDGES:**
- **At the start:** the line `SELFTEST RUN CAPTURED armour=<n of 4> offhand=<0|1>` follows `RUN START`. Its two
  figures are what he wears at capture, recorded as read (AS0's preamble).
- **At every gate boundary and at the end:** the boundary's `CLEAN … inventory=restored` line and the
  `SELFTEST RUN RESTORED …` line end with **`armour=<the captured n> offhand=<the captured n>`** and **no
  `NOT-RESTORED=`**.
- **Any `NOT-RESTORED=<slot>` is a FAIL that names the slot:** the restore wrote it and read back something other than
  what was captured (ItemStack's own equality). Stop and read his inventory before anything else.

## HOW LONG THE RUN IS

- **Armor-sets' waits sum to 1183 ticks (59.15 s)**
  (`tr -d '\r' < GATE-armor-sets.yml | awk '/^  - wait: /{s+=$3} END {print s}'`), plus AS16's ask (up to 1200 ticks,
  60 s, or until he answers). 894 steps (`grep -c '^  - '`); steps that are not waits share their pass's tick (READ,
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
- before `'--- absent'`: **41 `PRESENT`, 0 `ABSENT`**;
- between `'--- absent'` and `'--- controls'`: **4 `ABSENT`, 0 `PRESENT`**;
- after `'--- controls'`: **5 `ABSENT`, 0 `PRESENT`**.

Counted from the script, by section: 22 `ValueLine` + 11 `Entry` + 8 `Has` = 41, then 4 and 5 (W1 added `SelfTestWornSlots`, the CAPTURED and NOT-RESTORED literals, and the control `armour=5`). **It was run before commit on a jar
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

Exactly one **`[Rpg] Selftest: dev server, 439 scenarios across 49 gates [melee-cell, …, summoner, armor-sets]`**.
Recount: 49 files, 488 keys, 49 setups; control 467 + 21 = 488. Controls: `419 scenarios`, `436 scenarios` and `438 scenarios`
ABSENT.

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
| chainmail | 5 / 8 / 7 / 4 | `""`, `Set Bonus (4 pieces):`, `+50% Quiver Size` | `Ten thousand rings, and every one of them someone's afternoon.` | Common |
| copper | 5 / 8 / 7 / 4 | `""`, `Set Bonus (4 pieces):`, `+1 Max Summons` | `It greens where it is not worn.` | Common |
| iron | 10 / 16 / 14 / 8 | `""`, `Set Bonus (4 pieces):`, `+100 Max Health` | `Honest plate. It has no opinion about who wears it.` | Common |
| diamond | 15 / 24 / 21 / 12 | `""`, `Set Bonus (4 pieces):`, `+20 Defense` | `Cut from a single lattice, and cold to the touch.` | Uncommon |
| netherite | 15 / 24 / 21 / 12 | `""`, `Set Bonus (4 pieces):`, `+40 Defense` | `Salvaged from something that did not survive the fall.` | Uncommon |

Each block: `Gear Score: 100`, then `Defense: <d>`, then the set lines, then `""`, the flavor, `""`, and `<rarity>
<Helmet|Chestplate|Leggings|Boots>`. The set lines are the same on all four pieces (B8, static). The name is the piece's
display name (leather's are `Leather Cap`, `Leather Tunic`, `Leather Pants`, `Leather Boots`). A block with `Defense: 3`
on a diamond helmet, or no set lines on an iron piece, is a FAIL reading.

### AS0 — THE BASELINE

**Preamble (W1):** what he wears at capture is the run's `SELFTEST RUN CAPTURED armour=<n> offhand=<0|1>` line,
recorded here as read. AS0 itself clears his armour slots; the restore puts them back.

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

### AS3 — CHAINMAIL: ITS BONUS IS THE QUIVER, SO NO SHEET STAT MOVES BUT DEFENSE

The four chainmail blocks, now WITH their set lines (`+50% Quiver Size`, Q3). Full set: `Defense <B.D + 24>`; Health
Regen, Mana Regen and Max Health all equal B. No quiver weapon is held here, so the sheet has no `Quiver` line. The
quiver itself is AS17's.

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
   - **`SLOT 10 GRAY_DYE name="No piece enchant"`** with `lore=["Nothing is tied to this armor slot yet."]` (the marker by the
     gear's kind, 2026-11-25, SEAT PLACEHOLDER; `"No weapon enchant"` here is the FAIL);
   - **`SLOT 12 GRAY_DYE name="No unique enchant"`** with `lore=["Nothing unique is tied to this item's element
     yet."]`;
   - the normal columns at 14 and 16, from the four.
   - Armor's marker comes from `EnchantMenuLayout.emptyMarkerFor(slot, gearClass)`; every weapon keeps its words.

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

### AS17 — CHAINMAIL'S QUIVER: 2 → 3, 5 → 8, BACK, AND Q4

Facing east, nothing on the pad. Both weapons are given BEFORE the set, so each mints full at its authored size. The
`Quiver` line is the sheet's live `QuiverSize.resolve` for the held weapon (READ, `StatsSheetProjection`).
1. **Bare, Breacher held:** `Quiver 2`.
2. The four chainmail pieces worn.
3. **With the set:** Breacher **`Quiver 3`**, Dragon's Breath **`Quiver 8`**. EXECUTED: `QuiverSize.resolve(2, 0, 50)`
   = 3 and `resolve(5, 0, 50)` = 8.
4. **The head swapped for an untagged iron helmet:** Breacher **`Quiver 2`**, Dragon's Breath **`Quiver 5`**
   (`resolve(2, 0, 0)` = 2, `resolve(5, 0, 0)` = 5).
5. **The head back** (an untagged chainmail helmet completes the set, A3). Then **Q4**, on Dragon's Breath, read from
   its own lore line `Quiver: <loaded>/<capacity>` (`WeaponLoreLines`, rewritten only by `QuiverItems.setLoaded`, READ):
   - before any press: **`Quiver: 5/5`**;
   - one press (one round, as the Breacher's gate read for its spread): **`Quiver: 4/8`**, the capacity re-resolved at
     the write;
   - the reload (`DRIVE reload`, 40 ticks, the wait 50): **`Quiver: 8/8`**;
   - **the head swapped:** the sheet reads **`Quiver 5`**, and the lore still reads **`Quiver: 8/8`**. Nothing is
     written on removal: Q4, as Expanded Quiver;
   - **one press:** **`Quiver: 5/5`**, 7 clamped to the new 5 at the write (`Quiver.clamp`). **`Quiver: 7/8` or `7/5`
     is the FAIL.**
6. His own arrows skip him (READ, `GATE-summoner.md`): no HURT line in the row.

### AS18 — WEAR: ONE PER HIT, ONCE (D2')

A minted iron set at score 100; `probe_armor` lines `ARMOR slot=<s> item=<id> damage=<d> max=<m> broken=<b>`. The
maxima, READ at runtime from the material: helmet 165, chestplate 240, leggings 225, boots 195 (`ArmorMaterials` IRON
15 × `ArmorType` 11 / 16 / 15 / 13, READ).
1. **Fresh:** four lines, each `damage=0 broken=false`, with those maxima.
2. **`rpg damage 1`:** `HURT before=1.000 after=0.676 defense=48.000 dealer=self` at B.D = 0 (EXECUTED
   0.6756756756756757). Then **every piece `damage=1`**.
3. **The scripted mob hit by the frozen `s18`, staged as SM3AR:**
   - `HURT before=0.010 after=0.007 defense=48.000 dealer=<s18> dealerType=zombie` (EXECUTED: 0.01, then
     0.006756756756756757).
   - Then **every piece `damage=2`**.
   - **`damage=3` on any piece is the FAIL:** vanilla's own armor damage on that hit was not suppressed. READ: vanilla
     wears 1 for any hit, `(int) max(1, damage / 4)`.
   - **`damage=1` is a FAIL too:** the mob's hit did not wear.
4. No piece is broken.

### AS19 — A PIECE BREAKS; THE SET STAYS (D3')

An iron set at score 100, the chestplate staged `rpg durability set 238` while held. Its REPLY is recorded; READ, the
command is scoped to our armor since D1'.
1. **Before:**
   - `ARMOR slot=CHEST … damage=238 max=240 broken=false`, the others `damage=0`;
   - `rpg stats`: `Defense <B.D + 48>`, `Max Health <B.MH + 100>`.
2. **One `rpg damage 1`** (`HURT before=1.000 after=0.676 defense=48.000`):
   - **REPLY `"A piece of your armor is broken -- it no longer protects you. Repair it."`**
     (`ArmorBrokenNotice.MESSAGE`, SEAT PLACEHOLDER);
   - **`ARMOR slot=CHEST … damage=239 max=240 broken=true`** (broken at max − 1, the weapons' predicate), the others
     `damage=1`;
   - `rpg stats`: **`Defense <B.D + 32>`** (its 16 gone) and **`Max Health <B.MH + 100>`**, still, because the broken
     piece still counts toward the set (Ben).
3. **A second `rpg damage 1`** (`HURT before=1.000 after=0.758 defense=32.000`, EXECUTED 0.7575757575757576):
   - **`CHEST … damage=239`**: no further wear;
   - the others `damage=2`;
   - the chestplate is never removed.
4. **`Defense <B.D + 48>` after the break, or `Max Health <B.MH>`, is the FAIL.**

### AS16 — THE ONE ASK, LAST

A full iron set is worn, then the question: *"Hover a piece of your iron armour: is the Set Bonus clear, and does the
+100 Max Health show on your stats? (yes / no / not sure)"*.
- **It asks whether it WORKS and is CLEAR.** His answer is recorded verbatim.
- **AUTO cannot witness** how the tooltip renders on his client, or how the stats screen reads to him (verification.md:
  an AUTO reading is a reading of the server).

## NOT STAGEABLE (AND WHY)

- **Both entries that stood here are now STAGED** (the seat's re-rulings, 2026-11-25): chainmail's quiver is AS17, and
  wear and breaking are AS18 and AS19.
- **Unbreaking on armor is not staged on the pad:** a rolled piece's enchants are locked until the table activates them,
  so only `ArmorWearTest` reads it (III at rolls 0.24 and 0.25).
- **What only the boot can show:**
  - the client's tooltip render and the stats screen;
  - that `item replace … from` keeps the tag (AS6 witnesses it);
  - that AS14's give tags the item;
  - the recipe click (AS15);
  - the summon cast against a live cap (AS4).
  - vanilla's armor wear on a real mob hit and its suppression (AS18 step 3);
  - the broken notice's sound;
  - the magazine's clamp at a real press (AS17).

## READINGS — THE BOOT OF `732c4e02`, 2026-11-25 (written after the run; no prediction above is edited)

**The boot:**
- marker 16:28:12; `./scripts/dev-server.sh --refresh-content`; deployed jar 16:28:38; `latest.log` first line 16:28:39;
  `Done (6.389s)` 16:28:45.
- R0 read before any login: R0a `[Rpg] Build: 732c4e02`; R0b on the DEPLOYED jar 41 PRESENT / 0, 4 ABSENT / 0, 5
  controls ABSENT / 0; R0c `… 24 weapons, 1 shields, 28 armor …`, `Mint-on-craft: 42 indexed, 42 claiming, of 58 gear
  definitions`, 0 `[Rpg]` WARN (control: 7 `INFO [Rpg] Loaded`), no ArmorConsistency line; R0d `439 scenarios across 49
  gates […, summoner, armor-sets]`; R0e `FIGHTWORLD sweep … folders=0`.
- **Ben joined 16:37:39**, after R0. `SELFTEST RUN START summoner+armor-sets rows=42 selftest=732c4e02` 16:37:45;
  `SELFTEST armor-sets setup START` 16:39:26; `SELFTEST RUN FINISHED` 16:40:45; his `/stop` 16:40:47. No abort.
- **Backup:** `rpg-run-backups\2026-11-25-pre-armor-sets` (world 193 files, Rpg 178), his
  `world\players\data\b6ae27e9-6ca4-4bb5-a1fb-73cbadbabdd5.dat` 4,999 bytes. The world's saved difficulty before the
  boot: `normal`.

**W1, HIS PROPERTY, FIRST:**
- `SELFTEST RUN CAPTURED armour=0 offhand=0`
- the one gate-boundary line, `SELFTEST armor-sets setup CLEAN … inventory=restored armour=0 offhand=0 …`
- `SELFTEST RUN RESTORED xp=712824 cell=ranger/fire gamemode=SURVIVAL held=3 mana=100.0 trace=false spawn_mobs=false
  time=1000 blocks=1 mobsRemoved=0 orbsRemoved=0 keep_inventory=false immediate_respawn=false difficulty=NORMAL
  health=160.000 vanilla_xp=150+0.694 spawn_phantoms=true max_summons_base=null armour=0 offhand=0 summons=0
  drag_tick=false`

Restored equals captured. He wore nothing in the five slots, and 0 `NOT-RESTORED` lines are in the run.

**THE BASELINE.** AS0 read `B` = Max Health 100, Health Regen 1.00/5s, Mana Regen 5.00/5s, Defense 0. **AS13 raised
his level to 13, and nothing put it back, so AS14 to AS19 ran at level 13:** their own bare sheets read Max Health 160,
Defense 2. Every delta prediction holds against that row's own bare read.

| row | class | the lines |
|---|---|---|
| AS0 | READ AS PREDICTED | `Max Health 100`, `Health Regen 1.00/5s`, `Mana Regen 5.00/5s`, `Defense 0`; `max_summons: 1`; `SELF customHP=100.000/100.000` |
| AS1 | READ AS PREDICTED | four leather blocks as the table (`Defense: 5/8/7/4`, `+2 Health Regen per 5s`); full set `Defense 24`, **`Health Regen 3.00/5s`**; head swapped `Defense 29`, `Health Regen 1.00/5s` |
| AS2 | READ AS PREDICTED | four golden blocks (`+2 Mana Regen per second`); full set `Defense 24`, **`Mana Regen 15.00/5s`**; swapped `Defense 29`, `Mana Regen 5.00/5s` |
| AS3 | READ AS PREDICTED | four chainmail blocks with `+50% Quiver Size`; `Defense 24`; Health Regen, Mana Regen, Max Health equal B |
| AS4 | READ AS PREDICTED | `max_summons: 0 (base set to 0)`; `SUMMON refused … verdict=REFUSED_FULL max=0`, `"You cannot summon more (max 0)."`, `SUMMONPROBE none … store=0`; four copper blocks; `Defense 24`; `max_summons: 1`; `SUMMON spawn … score=100`; head swapped `max_summons: 0`, probe still `alive=true`; after 200 ticks `REFUSED_FULL max=0`, probe `alive=true`; cleared `cause=clear` |
| AS5 | READ AS PREDICTED | `SELF 100.000/100.000`; four iron blocks; **`SELF 100.000/200.000`** (headroom, c = B.MH exactly); healed `200.000/200.000`; head swapped **`SELF 100.000/100.000`**, `Max Health 100`, `Defense 43` |
| AS6 | READ AS PREDICTED | blocks `Gear Score: 200`, `Defense: 20/32/28/16`; **`Defense 96`**, **`Max Health 200`** |
| AS7 | READ AS PREDICTED | four diamond blocks (`+20 Defense` on each); **`Defense 92`**; swapped **`Defense 67`** |
| AS8 | READ AS PREDICTED | four netherite blocks (`+40 Defense` on each); **`Defense 112`**; swapped **`Defense 67`** |
| AS9 | READ AS PREDICTED | `Defense 112`; **`HURT before=10.000 after=4.717 defense=112.000 dealer=self dealerType=self`**; `"Custom HP: 95/100"` |
| AS10 | READ AS PREDICTED | four untagged diamond pieces: **`Defense 92`** |
| AS11 | READ AS PREDICTED | two minted, two untagged netherite: **`Defense 112`** |
| AS12 | READ AS PREDICTED | `HELDID slot=0 item=iron_helmet`; `"Iron Helmet -- 4 slot(s)"`, `Slot 0: []`, `Slot 1: []`, slots 2 and 3 from the four; `Pool: growth, mana_bank, protection, unbreaking`, `Unique pool: `, `Weapon pool: ` |
| AS13 | READ AS PREDICTED | the level REPLY (13); `DRIVE shift-click … bottom slot=81 iron_helmet x1`; **`SLOT 10 GRAY_DYE name="No piece enchant"` `lore=["Nothing is tied to this armor slot yet."]`**; `SLOT 12 GRAY_DYE name="No unique enchant"`; `SLOT 14 ANVIL "Unbreaking"`, `SLOT 16 GOLDEN_APPLE "Growth"` |
| AS14 | READ AS PREDICTED | before `HELD iron_helmet … lore=["Defense: 2"]`; `"Refreshed 1 item(s) from current content."`; after `lore=["Defense: 10"; ""; "Set Bonus (4 pieces):"; "+100 Max Health"; ""; "Honest plate. …"; ""; "Common Helmet"]`, no Gear Score line |
| AS15 | **MISS** (one predicted line absent; the mint read as predicted) | the menu `SLOT 0 COPPER_HELMET name="Copper Helmet"`, the click on slot 0; held `HELD copper_helmet … lore=["Gear Score: 100"; "Defense: 5"; ""; "Set Bonus (4 pieces):"; "+1 Max Summons"; ""; "It greens where it is not worn."; ""; "Common Helmet"]`. **No `CRAFT minted` line**: see the note below |
| AS17 | READ AS PREDICTED | bare Breacher `Quiver 2`; with the set Breacher **`Quiver 3`**, Dragon's Breath **`Quiver 8`**; head swapped **`Quiver 2`** and **`Quiver 5`**; Q4: `"Quiver: 5/5"`, press, **`"Quiver: 4/8"`**, reload (`RELOAD … authored=40 resolved=40`), **`"Quiver: 8/8"`**, head swapped: sheet `Quiver 5`, lore still **`"Quiver: 8/8"`**, press, **`"Quiver: 5/5"`**; no HURT |
| AS18 | READ AS PREDICTED (see the level note) | bare `Max Health 160`, `Defense 2`; fresh `ARMOR … damage=0` with max 165/240/225/195, `broken=false`; set `Max Health 260`, `Defense 50`; `rpg damage 1`: **`HURT before=1.000 after=0.667 defense=50.000 dealer=self`**, every piece **`damage=1`**; the scripted mob hit: `MOBSEED … zombie gs=100 … stand_d=3.00`, **`HURT before=0.010 after=0.007 defense=50.000 dealer=<s18> dealerType=zombie`**, every piece **`damage=2`** (not 3: vanilla's wear suppressed) |
| AS19 | READ AS PREDICTED (see the level note) | `"2/240 uses left (damage 238)"`; `CHEST … damage=238 … broken=false`, others 0; `rpg damage 1`: `HURT before=1.000 after=0.667 defense=50.000`; **`"A piece of your armor is broken -- it no longer protects you. Repair it."`**; **`CHEST … damage=239 max=240 broken=true`**, others `damage=1`; **`Max Health 260`** (the set stays); second `rpg damage 1`: **`HURT before=1.000 after=0.746 defense=34.000`**, **`Defense 34`** (B.D 2 + 32), **`CHEST … damage=239`** (no further wear), others `damage=2` |
| AS16 | READ AS PREDICTED | `ASK 1 question="Hover a piece of your iron armour: is the Set Bonus clear, and does the +100 Max Health show on your stats? (yes / no / not sure)" answer="yes"`. **His answer, verbatim: `yes`** |

**NOTE, 2026-11-25 — AS15's `CRAFT minted` line.** READ, `InventoryCraft.commitCraft`: the witness line is logged
`if (definition instanceof WeaponDefinition)`, so a minted ARMOR piece never emits it. The prediction named a line armor
cannot produce. The prediction was the defect, not the code. The craft minted, as the held tooltip (`Gear Score: 100`,
`Defense: 5`, the set lines) shows. A re-prediction is AS15R, and its witness would be the held item, or an
`INVENTORY` probe.

**NOTE, 2026-11-25 — AS18 and AS19 at level 13.** Their HURT literals were written "at B.D = 0". AS13 left him at level
13, whose level bonus is Defense 2 (`LevelBonus`: level / 5), and each row's own bare sheet read `Defense 2`. EXECUTED at
the staged Defense:
- `Defense.applyDefense(1, 50)` = 0.6666666666666666, which prints `0.667`;
- `applyDefense(0.01, 50)` = 0.006666666666666667, `0.007`;
- `applyDefense(1, 34)` = 0.746268656716418, `0.746`.

Those are the three readings. Every delta (B.D + 48, B.D + 32, B.MH + 100) held. **The seam is the scenario's: AS13
raises the level, and no row after it resets it.**

**HIS SAFETY — every HURT line on him in the whole run:**

| row | line | executed prediction |
|---|---|---|
| SM3AR | `HURT before=0.010 after=0.010 defense=0.000 dealer=<s3a> dealerType=zombie` | 0.010 |
| AS9 | `HURT before=10.000 after=4.717 defense=112.000 dealer=self` | 4.716981132075472 |
| AS18 | `HURT before=1.000 after=0.667 defense=50.000 dealer=self` | 0.6666666666666666 at D 50 |
| AS18 | `HURT before=0.010 after=0.007 defense=50.000 dealer=<s18> dealerType=zombie` | 0.006666666666666667 |
| AS19 | `HURT before=1.000 after=0.667 defense=50.000 dealer=self` | 0.6666666666666666 |
| AS19 | `HURT before=1.000 after=0.746 defense=34.000 dealer=self` | 0.746268656716418 |

**Sum of `after=`: 6.814**, against the worst-bound sum 13.020 and the cap 20.000.

**What no row predicted:**
- the level seam above;
- REPLY lines are drained a step late, so a command's REPLY can carry a later timestamp than the line after it (AS4's
  `max_summons: 0`, AS17's Quiver replies), with order preserved;
- the Paper notice after Done, that this server is 4 releases behind the latest stable (26.3; the upgrade procedure's
  step 0).
- **PAD:** tickets `held=12`, `released=12 cause=end`. **MOBSEED beyond the pad:** one, a drowned at `stand_d=237.75`, the
  world mob the summoner boot recorded. **WARN after Done:** only that Paper notice (6 lines); no `[Rpg]` WARN, no
  ERROR.
