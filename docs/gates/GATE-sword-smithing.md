# GATE — THE SWORD LADDER, PR 2: NETHERITE AT THE SMITHING TABLE (feat/sword-smithing)

**Status: NOT RUN, and no boot of its own.** Written 2026-11-20 on `feat/sword-smithing`, stacked on `feat/sword-tiers`,
before any boot. **Every prediction below was written before the boot.**

> **SUPERSEDED IN PART, 2026-11-20 (the seat's split ruling): THE R0 AND THE RUN SHEET ARE NOT HERE.**
> `GATE-sword-convert.md` (PR 3, the stack's top) carries the one R0 and the one run sheet for all three PRs. This file's
> rows run on that boot, and their readings go in this file's cells.

**Game mode:** SURVIVAL. **Nothing here can hurt him** (a smithing table, no mob). The arithmetic: **0 against 100.**

**Ben's word** (PLAN-weapon-tiers.md 1.2, answer 4), asked whether netherite comes from a smithing-table upgrade of a
diamond sword and keeps its enchantments: **"Yes"**.

## WHAT WAS READ, EACH MARKED

- **The smithing events** (READ, javap on the pinned `paper-api-26.1.2.build.74-stable`):
  - `PrepareSmithingEvent` extends `com.destroystokyo.paper.event.inventory.PrepareResultEvent` (which carries the
    result) and returns a `SmithingInventory`;
  - `SmithingInventory` has `getInputTemplate`, `getInputEquipment` and `getInputMineral`;
  - `SmithItemEvent` extends `InventoryClickEvent`: the take.
- **No smithing code existed before this branch** (READ: no `PrepareSmithingEvent` or `SmithItemEvent` in `paper/`). A
  minted weapon in a smithing table was vanilla's to transform, and lost its identity (the survey's 2e).
- **The carry** (READ, `GearItems.carryInstanceData`): the id tag, then wear (clamped to the NEW material's maximum by
  `carryWear`), then the enchant container, the quiver and the gear score. `WeaponItems.remint` calls it with the fresh
  item's type.

## AS BUILT

- **Core `SmithingUpgrades.decide(base id, template, addition)`**, pure. Three arms:
  - **UPGRADE** a declared rule;
  - **REFUSE** a minted base with no rule for these inputs (or an empty slot);
  - **NOT_OURS** a plain item, left to vanilla.
  Tokens are normalised as craft results are. Two rules for one key are refused at construction.
- **Content `smithing.yml`:** one upgrade, `diamond_sword + netherite_upgrade_smithing_template + netherite_ingot ->
  netherite_sword`. `SmithingLoader` refuses a bad row by name (`Refusing smithing upgrade <n>: …`) and logs **`Loaded
  smithing: 1 upgrades [diamond_sword + netherite_upgrade_smithing_template + netherite_ingot -> netherite_sword]`**.
- **Paper:**
  - `onPrepareSmithing` sets the result: the upgrade (`WeaponItems.upgrade`, a re-mint as the netherite sword that
    carries the instance data, then the id re-stamped) or, on REFUSE, **no result**, so nothing can be taken and no
    ingot is spent;
  - `onSmithItem` logs the witness line `SMITH took player=<n> from=<id> to=<id> material=<m> wear=<d>/<max>
    enchant_data=<raw> score=<s>`.

## THE ROWS

### SSA1 — he is handed a minted, enchanted Diamond Sword, a template and an ingot (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `rpg give diamond_sword`, held; `rpg durability set 100`: REPLY `<u>/1561 uses left (damage 100)` (1561 is diamond's maximum, the survey's 2i, READ). `rpg enchant show`: REPLY naming its rolled candidates, each `sharpness` or `unbreaking` (recorded, to compare with SSA2). `give … netherite_upgrade_smithing_template 1` and `give … netherite_ingot 1` succeed | `/rpg durability` (READ); PR 1's pool | |

### SSM1 — the smith (SURVIVAL; Ben's hands)

No selftest verb drives a smithing table (the verbs are READ), so:

| prediction | stands on | READING |
|---|---|---|
| **The line Ben does:** "At a smithing table, put the template, the Diamond Sword and the ingot in, and take the Netherite Sword." The result slot shows **Netherite Sword** before he takes it. On the take: **`SMITH took player=BaronVonYeetus from=diamond_sword to=netherite_sword material=netherite_sword wear=100/2031 enchant_data=<the same raw blob SSA1's sword carried> score=<T>`**. 2031 is netherite's maximum; the wear of 100 carries, under the new maximum | `SmithingSourceTest`; `GearItems.carryInstanceData` (READ) | |

### SSA2 — the netherite sword kept its enchants and its wear (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| `hold: netherite_sword` finds it. `dump: held`: **"Netherite Sword"** in the uncommon colour, `Melee Damage: <31 x T/100>`, `Uncommon Melee Weapon`. `rpg enchant show`: **the same candidates, levels and active choice as SSA1's.** `rpg durability damage 1`: REPLY **`… /2031 uses left (damage 101)`**: the carried 100 plus the read's 1 | the same | |

### SSB1 — question 2 (SURVIVAL)

| prediction | stands on | READING |
|---|---|---|
| His answer, verbatim, to **"Did the smithed Netherite Sword keep its enchantments, as far as you can see?"** No PASS or FAIL; "not sure" is NOT STATED | SSM1, SSA2 | |

### SSU1 — the mapping (UNIT, not a boot row)

`SmithingUpgradesTest`, 7 rows: the declared upgrade; the right base with the wrong template or addition, or an empty
slot, is refused; tokens normalise; two rules for one key are refused at construction; an empty table refuses.
Mutations: an unmatched base PASSed to vanilla (3 red), match on the base alone (1), a plain item refused (1).

### SSU2 — the refusal (UNIT and SOURCE, not a boot row)

**A minted weapon with no declared upgrade (a minted Iron Sword with the template and an ingot) gives NO result.** It is
not staged on the server: it needs Ben's hands at a table for a row whose prediction is "nothing appears". It is
`SmithingUpgradesTest` *aMintedBaseWithNoDeclaredUpgradeIsRefused* and `SmithingSourceTest` (`case REFUSE ->
event.setResult(null);`; mutation: the arm made a no-op, 1 red). **Flagged:** whether a refusal row should be added for
Ben.

### SSU3 — the carry (SOURCE, not a boot row)

`SmithingSourceTest`: the upgrade is a re-mint (`carryInstanceData` with the fresh item's type), then the id is
re-stamped. Mutation: a fresh mint instead of a re-mint (1 red). SSA2 reads the same thing on a server.

## WHAT THIS GATE DOES NOT READ

- **Armour and tools at the smithing table:** this branch decides weapons only. A minted diamond armour piece or tool is
  still vanilla's to transform. Flagged.
- **A refusal on a server** (SSU2).
