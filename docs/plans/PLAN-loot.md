# PLAN — THE CASCADING LOOT SYSTEM

> **Ben, 2026-11-19 (PLAN-dragon-fight.md section 1, entry h, answer 2), verbatim:** "A Cascading loot system is used for
> deciding the rarity of drops given out to players. It works like this. First the rarest rarity is rolled. then if
> that doesn't land, the next rarest rarity is rolled and so on. In our world today, that would look something like
> this: Exotic 1% then Legendary 3% then Epic 8% then Rare 12% and so on. Numbers are place holders. We can tune these
> numbers to specific drop sources but this is the over arching system."

**Status:** the core rule and its content loader are BUILT on `feat/loot-cascade` (branched from master `868df417`).
**Nothing in the game uses it.** No boot, no gate. Written 2026-11-19 from the seat's queue, item 5.

## 1. WHAT EXISTS TODAY (READ, `master` at `868df417`)

The sweep was a read-only search over `core/`, `storage/` and `paper/src/main` and the content YAML. The facts below
were then spot-checked against `git show master:`.

### 1.a The rarities

`core/.../weapon/Rarity.java` is an enum, in this order: **`COMMON, UNCOMMON, RARE, EPIC, LEGENDARY, EXOTIC`**.
- **Order is the declaration order** (ordinal). There is no rank field and no YAML list. Until this branch no code
  compared rarities.
- `Rarity.fromName` is a case-insensitive name match, null on a miss.
- **Each rarity carries:**
  - a colour (`paper/.../weapon/RarityColors.of`: white, green, blue, dark purple, gold, aqua);
  - an anvil cost (`core/.../anvil/AnvilCost.levelFor`: 10, 16, 25, 40, 60 and 90 levels);
  - its title-cased name in the lore footer (`GearLore.appendRarityFooter`) and the display name's colour.
- **It does not enter** the gear score (`GearScore`: "RARITY DOES NOT ENTER ANY OF THIS") or the enchant roll.
- **The content's `rarity:` key** (every loader defaults to `common`):
  - **weapons:**
    - common: flint_staff, ironblade, short_bow
    - uncommon: boltor, cursed_emerald, hunters_bow, lapis_staff, withered_shortbow
    - rare: ember_staff, emberblade, locust
    - legendary: blaze_kings_staff, dragons_breath, dragons_plume
    - exotic: quiver_stone, volley_stone
  - **armour:** chainmail, golden, iron and leather common; diamond and netherite uncommon.
  - **the shield, and every tool:** common.
  - **accessories:** keen, mending and ward charms uncommon; brawler's gauntlet, fletcher's quiver and sage's scroll
    rare.
- **No content is EPIC today.** Mobs have no rarity.

### 1.b Every place a player is given an item, or something drops

- **`/rpg give <id>`** (`RpgCommand.give`): mint, then the enchant roll, then the gear-score roll, then `addItem`. It
  refuses a full inventory. **Random:** the two rolls, `ThreadLocalRandom`.
- **Crafting** (`InventoryCraft.commitCraft`, the one output for the grid and Quick Craft): a recipe claimed by a gear
  definition is minted and rolled as `/rpg give` is; others are the vanilla result. Recipes are registered by
  `RecipeRegistrar` from `content/recipes/` and gear's `craft_result:`.
- **Mining** (`MiningService.onBroke`): a broken patch pays `raw_iron` by `addItem`, the overflow dropped at his feet.
  **Random:** the per-spot amounts (`PayoutRule.roll`), the spot count (`SpotLayout.roll`) and the vein size
  (`SizeRoll.roll`, weighted).
- **Mob deaths:** ordinary mobs keep **vanilla's drops and XP**; nothing of ours adds or clears drops. A Phalanx shot
  drops nothing. Mob equipment never drops (drop chance 0).
- **The dragon fight's win:** vanilla's drops and XP are suppressed (a SEAT PLACEHOLDER). **No item is given.**
- **A player's death:** inventory kept, nothing dropped.
- **The Nexus star and the Ability Stone:** re-minted into their locked slots on join and respawn. Not random, not loot.
- **Hand-backs, not new items:** menus returning held items (`MenuSafety.give`), the vault's `take`, the anvil and the
  enchant menu re-minting in place.
- **Searched for and not found:** chests or `LootGenerateEvent`, kits, starter items, rewards, quests, `getDrops().add`,
  `setDrops`.

### 1.c Anything already called a loot table, a drop table, a pool or a roll

- **No loot table, drop table or drop chance exists**, in code or content.
- "Loot" appears only in comments (`WeaponDefinition`: "rarity parameterizes a loot roll in Phase 4"; `EnchantRollItems`
  and `GearScoreItems`: "a loot drop, a starter kit, a quest reward").
- **The other "pools" are not item pools:**
  - `EnchantRoll.poolFor` pools ENCHANT IDS for a gear class, picked uniformly without replacement;
  - the build pools (`PoolDefinition`, `PoolLoader`) pool a cell's abilities, fragments and aspects;
  - `ResourcePool` is mana.
- **The only weighted draw** is inline in mining's `SizeRoll.roll` (integer weights, walked down). **There is no generic
  weighted-random helper in core**; every other draw is uniform.

## 2. THE RULE, AS BUILT (core, `io.github.butterflysmp.rpg.core.loot`)

- **`CascadeTable`:**
  - Tiers run rarest to commonest (`Rarity`'s order), each a rarity once and a chance in [0, 1].
  - A roll draws once per tier, in order. A tier lands when its draw is below its chance. The first to land is the
    rarity; none landing is "nothing".
  - **SEAT READING, flagged:** only the LAST tier may be guaranteed (1.0); a guaranteed tier above another is refused,
    because the tiers below it could never land.
  - The random source is passed in as a `DoubleSupplier`, never a static.
- **The true odds** (`CascadeTable.trueOdds`): each tier's chance times the product of the misses above it, and
  nothing's. For Ben's four numbers:
  - exotic 1.000%, legendary 2.970%, epic 7.682%, rare 10.602% (all computed);
  - **nothing 77.746%**.
- **The item** (`CascadeLoot.roll`): a source gives, per rarity, a pool of item ids.
  - **SEAT PLACEHOLDER, flagged for Ben:** the pick within a rarity is UNIFORM. `PoolPick.UNIFORM` is one rule, so
    weights replace it in one place.
  - **SEAT READING, flagged:** a rarity that lands with an EMPTY pool for that source FALLS THROUGH. It counts as a miss
    and the next tier is rolled as usual, rather than giving nothing, and the result lists each rarity that fell
    through. (The alternative reading, "the next tier is given outright", is not built.)

## 3. THE CONTENT (`content/loot/*.yml`, `LootLoader`)

- **The layout (CC's proposal, flagged):**
  - one `default:` cascade;
  - `sources:` by id, each with optional `tiers:` (its own chances; absent, the default's) and `pools:` (per rarity,
    item ids).
- **Refused whole, by name and reason:**
  - a chance outside [0, 1];
  - an unknown rarity;
  - tiers out of rarity order or repeated;
  - a guaranteed tier above another;
  - an unknown item id (one no gear registry has);
  - a missing `default:`.
- **The Loaded line** prints each default tier's written chance and its true odds, and nothing's:
  `Loaded loot 'cascade': default exotic 1.00% (truly 1.000%), legendary 3.00% (truly 2.970%), …; 0 sources []`.
- **Shipped: ONE table, `content/loot/cascade.yml`.**
  - Ben's placeholders: exotic 1%, legendary 3%, epic 8%, rare 12%.
  - Below them, SEAT PLACEHOLDERS: uncommon 20%, common 40%, not guaranteed.
  - Its true odds, computed: 1.000, 2.970, 7.682, 10.602, 15.549 and 24.879%; nothing 37.318%.
  - **No source.**

## 4. WHAT EACH EXISTING DROP SOURCE WOULD TAKE TO WIRE (a proposed order, for Ben; none is built)

1. **A dev command first** (`/rpg loot roll <source> [n]`): roll a source n times and print the tier counts against
   the true odds. It reads the system on a server with no gameplay change.
2. **A mob drop**: the next hostile mob to drop gear. It needs an `EntityDeathEvent` hook that adds an item, minted
   then rolled (enchants, gear score) as `/rpg give` is, and an answer to "which mobs, and does vanilla's drop stay".
3. **The dragon fight's win**: today it suppresses vanilla's drops and gives nothing. It needs a source per fight
   (Ben's "specific drop sources"), and a ruling on who receives it: each participant, or one drop.
4. **Chests** (none exist): a `LootGenerateEvent` hook, or our own containers.
5. **Mining**: it pays a material, not gear, so a cascade there would be a second payout, not a replacement.
6. **Crafting and `/rpg give`**: not loot (the player chose the item). No cascade.

**Every wiring needs:**
- the mint and roll path `/rpg give` already uses;
- a test of the hook;
- a gate.

**And every source needs its pools written in content**, an item listed under its own rarity. Whether the loader
should REFUSE an item listed under another rarity's pool is open for Ben; it is not checked today.

## 5. OPEN, FOR BEN

- The two SEAT READINGS above: the guaranteed last tier only, and the empty-pool fall-through.
- The uniform pick within a rarity, a SEAT PLACEHOLDER.
- The tiers below rare (uncommon 20%, common 40%), SEAT PLACEHOLDERS.
- The content layout.
- The wiring order in section 4.
- **No content is EPIC today**, so a source's epic pool would be empty, and an epic that lands falls through.
