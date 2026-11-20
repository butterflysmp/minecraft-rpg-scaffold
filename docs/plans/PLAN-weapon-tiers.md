# PLAN — WEAPON TIERS: THE SWORD LADDER (READ-ONLY SURVEY)

**SURVEY ONLY. No Java, no content, no gate, no boot.** This file records Ben's words, reads what master has today,
and proposes an order of small steps for the seat and Ben. Nothing here is built, and **Ben rules every number.**

**Read against:** master **`868df417`** (the squash of #215). The pinned server is Paper `26.1.2` build 74
(`pom.xml`'s `paper.version` = `26.1.2.build.74-stable`). It is separate from the dragon-fight stack (#216-#218) and
from the loot cascade (#219), and touches neither.

**Citations.** The seat asked for a file and a line for every claim, so each one carries `path:line` **at
`868df417`**. CLAUDE.md's rule is that a pointer names a SECTION, because a line goes stale on any insertion, so
every citation also names its class and method or its YAML key. **Trust the name over the number once master
moves.** Short paths: `P/` = `paper/src/main/java/io/github/butterflysmp/rpg/paper/`, `C/` =
`core/src/main/java/io/github/butterflysmp/rpg/core/`, `content/` = `paper/src/main/resources/content/`, `RL` =
`P/listener/RpgListeners.java`.

**READ / INFERRED.** **READ** means the code or the jar was seen doing it. **INFERRED** means it was reasoned, usually
about vanilla or Bukkit behaviour that no code in this repo states. Jar facts come from `javap` on
`run/versions/26.1.2/paper-26.1.2.jar`, the mojang-mapped server jar the dev server runs. Its `version.json`
reads `"id": "26.1.2"`, and `org.bukkit.Material` was read from the pinned
`paper-api-26.1.2.build.74-stable.jar`. **The server jar's own Paper build number was not read**, because its file
name does not carry one. So a jar fact below is a 26.1.2 vanilla fact. That this is build 74's copy is
UNVERIFIED.

**PROVENANCE OF THE CITATIONS.**
- **Section 2 (b–g) and section 3** were gathered by four read-only survey agents at `868df417`.
- **Re-read by hand:**
  - the token damage and its guard (`RL:170`, `RL:2320-2336`);
  - `landVanillaMelee`'s early return;
  - the sweep cancel (`RL:2231-2239`);
  - the repair ruling (`RL:384-388`);
  - `RecipeProbe.claimedBy`;
  - the warn-only validator loop in `RpgPlugin`;
  - `WeaponItems.mint`'s modifier and `attackDamageModifier`;
  - `WeaponDurability.maxOf`;
  - `WeaponLoader`'s `material` default and `craft_result` parse;
  - `WeaponLoreTest`'s hard-coded id list;
  - `WeaponLoaderTest.onlyTheBowsRightClickIsABasicAttack`'s count;
  - `GATE-dragon-split.yml`'s note.
- **All jar facts (2h–2j) and the weapon table (2a, 2k)** were read directly.
- **Every other line number is the agents' and was not individually re-opened.**

---

## 1. BEN'S WORDS AND THE RULINGS

### 1.1 Ben, 2026-11-19, VERBATIM: the deletion and the ladder

The seat had listed every weapon with its class, element and rarity. Ben:

> "Just a quick note, the emberblade should also be slated for deletion.
> Another thing, the plan with early game Melee weapons is to have wood, stone, iron, diamond, and netherite swords as a kind of "progression tiers" and to have similar weapons of the other classes that would fit into those tiers. Gold and Copper also have tiers but they're side ways progression for iron and stone respectively. This means the Melee weapons are just simply swords. We'll need minted swords compatible with Enchantments"

**The seat's readings (the seat's, not Ben's):**
- `emberblade` joins `hunters_bow`, `ironblade` and `quiver_stone` in the deletion set.
- The melee ladder is the vanilla swords: wooden, stone, iron, diamond, netherite. Copper sits beside stone and golden
  beside iron.
- Ranger and mage get equivalents per tier. They are not designed yet.
- "Minted" means our own keyed item carrying our stats.

> **A FLAG, NOT AN ACTION.** CLAUDE.md's standing decision *NEW CONTENT CITES THE BOLTOR* and its account in
> `.claude/rules/standing-decisions.md` still name the deletion set as **three** (`hunters_bow`, `ironblade`,
> `quiver_stone`). PLAN-legacy-port.md calls emberblade *"the only non-deletion-set melee weapon"*. Bringing those
> files in line with Ben's words is a rules change for its own PR. This survey changes neither.

### 1.2 Ben, 2026-11-19, VERBATIM: the six answers

The seat asked:
1. what "sideways" means for copper and gold;
2. damage and attack speed per tier;
3. element and rarity, assuming all kinetic and common;
4. whether netherite comes from a smithing-table upgrade of a diamond sword and keeps its enchantments;
5. whether swords that are found or dropped, not crafted, become minted swords;
6. whether the ranger and mage equivalents are known.

Ben:

> "1. We'll get back to that
>  2. Largely Vanilla numbers except damage we'll increase by 3x
>  3. Diamond and Netherite will be Uncommom, the others Common. Everything kinetic
>  4. Yes
>  5. Yes
>  6. Somewhat, some of them are already in the game"

**The seat's readings (the seat's, not Ben's):**
- Attack speed, sweep and reach stay vanilla's.
- Attack damage is vanilla's times 3.
- Diamond and netherite are **uncommon**. Wooden, stone, copper, iron and golden are **common**.
- Every tier sword is **kinetic**.
- Netherite is made at the smithing table from a diamond sword and **carries its enchantments**.
- A sword obtained by **ANY** route is minted.
- "Sideways" is **OPEN**. *(Answered for the swords in 1.2b, below.)*

### 1.2b Ben, 2026-11-19, VERBATIM: golden and copper

> "For more clarification on Gold and Copper, the swords specifically should do slightly more damage than stone and less than iron. The armor sets are where the tier is a side step in progression. We will get to armor later"

**The seat's readings (the seat's, not Ben's):**
- **This ANSWERS "sideways" for the swords.** The golden sword and the copper sword each deal **more than the stone
  sword and less than the iron sword.**
- They are **the two exceptions to "vanilla × 3"**.
- The golden and copper swords keep **vanilla's attack speed and durability**.
- **The sideways step belongs to the ARMOUR sets.** Ben has deferred them ("We will get to armor later"). This is
  recorded as a **pointer for a later armour plan**, and nothing for it is designed here.

> **SEAT PLACEHOLDER, FLAGGED FOR BEN. IT IS NOT HIS.** **16 for both**, if stone reads 15 and iron 18. The seat has
> asked him to choose between **both at 16** or **copper 16 and golden 17**. 2j shows that the placeholder holds only
> under one of 2i's two ×3 readings.

### 1.3 What is now RULED (moved here from the open list)

| question | ruling | source |
|---|---|---|
| Which swords | wooden, stone, iron, diamond, netherite as the ladder; copper and golden as side tiers | 1.1 |
| Melee weapons are swords | yes, "just simply swords" | 1.1 |
| Minted and enchantable | yes | 1.1 |
| Damage | vanilla × 3, **except golden and copper**. **Which vanilla number is multiplied is flagged in 2i** | 1.2 answer 2 |
| Golden and copper sword damage | **more than stone, less than iron**. The exact figures are OPEN (2j, section 5) | 1.2b |
| "Sideways" | **for the swords, answered by the row above.** The sideways step is the ARMOUR sets', deferred | 1.2b |
| Golden and copper attack speed and durability | vanilla's (seat reading) | 1.2b |
| Attack speed | vanilla (1.6 for all seven, 2i) | 1.2 answer 2 |
| Rarity | diamond and netherite uncommon, the other five common | 1.2 answer 3 |
| Element | kinetic, all seven | 1.2 answer 3 |
| Netherite | smithing-table upgrade of the diamond sword, enchantments kept | 1.2 answer 4 |
| Found or dropped swords | minted, whatever the route | 1.2 answer 5 |
| Emberblade | slated for deletion | 1.1 |

**Still open** is section 5.

### 1.4 Ben, 2026-11-20, VERBATIM: the seven numbers, the sweep, the names

The seat asked:
- which vanilla number is tripled;
- golden and copper at 16, or 16 and 17;
- whether Ironblade's sweep goes on every tier sword;
- plain vanilla names or his own.

His first line answers a separate question, about the void, and is recorded in `PLAN-world-damage.md`. Ben:

> "1. The void: no it should not stop healing/regen
>  2. Here's the damage I want from each sword:
>
>  Wood - 11
>  Stone - 14
>  Copper - 17
>  Gold - 20
>  Iron - 23
>  Diamond - 27
>  Netherite - 31
>
>  3. Yes all swords should have the built in sweep
>  4. Names plan vanilla names with the rarity color"

**The seat's readings (the seat's, not Ben's, 2026-11-20):**
- **His seven numbers SUPERSEDE "vanilla x 3" and the 16/17 placeholder.** The x3-base question (2i) is moot.
- **"The built in sweep" is ironblade's sweep value (0.5)** and its vanilla-driven left click.
- **Names are vanilla's English names**, coloured by rarity as every weapon's display name already is.
- **Attack speed 1.6; reach and durability stay vanilla's.**
- **Diamond and netherite are uncommon; the rest common. All kinetic, all class melee.**

**The seat's two rulings on the survey's questions for it (2026-11-20):**
- **"Any route" (Ben's answer 5) is built as CONVERT ON ARRIVAL** (4.2's option b), not per source.
- **A contested or mismatched `craft_result` FAILS CLOSED at boot:** refused by name, not warned.

**Now RULED, added to 1.3's table:**

| question | ruling | source |
|---|---|---|
| Damage, all seven | wooden 11, stone 14, copper 17, golden 20, iron 23, diamond 27, netherite 31 | 1.4 answer 2 |
| Sweep | every tier sword has ironblade's 0.5 | 1.4 answer 3 |
| Names | vanilla's, in the rarity colour | 1.4 answer 4 |
| Any route | convert on arrival | the seat, 1.4 |
| A bad `craft_result` | refused at boot | the seat, 1.4 |

---

## 2. WHAT EXISTS TODAY

### 2a. Every weapon on master

All 16 files in `content/weapons/`. `—` means the key is absent. A melee weapon's `attack_damage` is its basic
swing. A ranged weapon with a `weapon_damage` basic uses `attack_damage` as its shot. A mage weapon authors no
`attack_damage`, so its trigger's literal `on_hit` damage is in 2k.

| id | display name | class | element | rarity | material | attack_damage | attack_speed | deletion set |
|---|---|---|---|---|---|---|---|---|
| blaze_kings_staff | Blaze King's Staff (`:34`) | mage (`:37`) | fire (`:35`) | legendary (`:36`) | blaze_rod (`:38`) | — | — | no |
| boltor | Boltor (`:34`) | ranger (`:79`) | kinetic (`:40`) | uncommon (`:74`) | crossbow (`:95`) | 19 (`:132`) | — | no |
| cursed_emerald | Cursed Emerald (`:22`) | mage (`:64`) | kinetic (`:62`) | uncommon (`:63`) | emerald (`:79`) | 0 (`:86`) | — | no |
| dragons_breath | Dragon's Breath (`:39`) | ranger (`:109`) | fire (`:84`) | legendary (`:94`) | crossbow (`:118`) | — | — | no |
| dragons_plume | Dragon's Plume (`:32`) | ranger (`:52`) | void (`:48`) | legendary (`:40`) | bow (`:57`) | 34 (`:99`) | — | no |
| ember_staff | Ember Staff (`:20`) | mage (`:23`) | fire (`:21`) | rare (`:22`) | blaze_rod (`:24`) | — | — | no |
| **emberblade** | Emberblade (`:12`) | melee (`:15`) | fire (`:13`) | rare (`:14`) | — → **iron_sword** | 7 (`:18`) | 1.6 (`:24`) | **Ben, 2026-11-19 (1.1)**; not yet in CLAUDE.md |
| flint_staff | Flint Staff (`:14`) | mage (`:17`) | fire (`:15`) | common (`:16`) | stick (`:54`) | — | — | no |
| **hunters_bow** | Hunter's Bow (`:32`) | ranger (`:35`) | fire (`:33`) | uncommon (`:34`) | bow (`:36`) | 6 (`:41`) | — | **yes** |
| **ironblade** | Ironblade (`:13`) | melee (`:18`) | kinetic (`:14`) | common (`:15`) | — → **iron_sword** | 8 (`:22`) | 1.6 (`:28`) | **yes** |
| lapis_staff | Lapis Staff (`:19`) | mage (`:39`) | kinetic (`:37`) | uncommon (`:38`) | breeze_rod (`:57`) | — | — | no |
| locust | Locust (`:33`) | ranger (`:102`) | nature (`:58`) | rare (`:89`) | crossbow (`:114`) | 26 (`:126`) | — | no |
| **quiver_stone** | Quiver Stone (`:42`) | ranger (`:57`) | kinetic (`:46`) | exotic (`:52`) | crossbow (`:75`) | 0 (`:81`) | — | **yes** |
| short_bow | Short Bow (`:30`) | ranger (`:33`) | kinetic (`:31`) | common (`:32`) | bow (`:34`) | 16 (`:35`) | — | no |
| volley_stone | Volley Stone (`:24`) | mage (`:34`) | kinetic (`:25`) | exotic (`:33`) | echo_shard (`:35`) | 0 (`:45`) | — | no. It is a dev tool, `unscored: true` (`:70`) |
| withered_shortbow | Withered Shortbow (`:32`) | ranger (`:35`) | wither (`:33`) | uncommon (`:34`) | bow (`:36`) | 16 (`:37`) | — | no |

- **Sweep.** Only the two melee weapons author `sweep: 0.5` (`ironblade.yml:38`, `emberblade.yml:34`).
- **Material.** Neither melee weapon authors one. `WeaponLoader.parse` reads
  `s.getString("material", WeaponDefinition.DEFAULT_MATERIAL)` (`P/content/WeaponLoader.java:289`), and
  `WeaponDefinition.DEFAULT_MATERIAL = "iron_sword"` (`C/weapon/WeaponDefinition.java:42`). READ.
- **So today there are exactly two melee weapons, both mint as an `iron_sword`, and both are now in the deletion set.**

### 2b. How a weapon is minted and recognised in a hand

**The key (READ).** It is one PDC string, `weapon_id`: `Keys` constructor, `new NamespacedKey(plugin, "weapon_id")`
(`P/adapter/Keys.java:456`). Shields, armour, tools and accessories each have their own key (`Keys.java:457-460`).

**`WeaponItems.mint`** (`P/weapon/WeaponItems.java:88-209`), READ:
- **Material:** `new ItemStack(materialOf(weapon.material()))` (`:90`). An unknown name falls back to `IRON_SWORD`
  (`materialOf`, `:315-318`).
- **Name and id:** the name is coloured by rarity (`:103`). The id is set with
  `meta.getPersistentDataContainer().set(keys.weaponId, STRING, weapon.id())` (`:104`).
- **Stack size:** `meta.setMaxStackSize(1)` (`:114`). This is the standing decision NO CUSTOM ITEM STACKS ABOVE 1.
- **Attack damage:** exactly one ATTACK_DAMAGE modifier, keyed `keys.meleeSuppressor`, `ADD_NUMBER`, `MAINHAND`.
  - For a vanilla-driven melee weapon with `attack_damage > 0`, its value is `attackDamageModifier(attack_damage)` =
    `attack_damage − VANILLA_BASE_ATTACK_DAMAGE` (`:76-78`, `:99-100`, `:140-142`).
  - Otherwise it is a flat suppression (`:146-148`).
  - **So an authored `attack_damage` is the TOTAL swing, including the player's base 1.** Ironblade's 8 becomes a +7
    modifier. This decides 2i's question.
- **Attack speed:** none is pinned on the item. It is reconciled onto the PLAYER from the weapon's authored
  `attack_speed` (comment at `:136-139`).
  - INFERRED (Bukkit): writing any attribute modifier to the meta replaces the material's default modifiers, so the
    sword's own −2.4 speed modifier is gone. The player-side reconcile supplies the cadence instead.
- **Flags:** only `HIDE_ATTRIBUTES` (`:156`).
- **Lore:** stats and flavour, plus an enchant block with a glint override driven by our enchant state (`:252-276`).
- **Not set:** durability or max damage, unbreakable, custom model data. A grep of `paper/src/main` for
  `setUnbreakable`, `setCustomModelData` and `setMaxDamage` finds nothing. READ.
- **Not in `mint`:** the enchant roll (`EnchantRollItems.rollOnAcquire`) and the gear-score stamp
  (`GearScoreItems.stampOnAcquire`). Both run only at acquisition: `RpgCommand.give` (`P/command/RpgCommand.java:1634-1642`)
  and `InventoryCraft.commitCraft` (`P/menu/InventoryCraft.java:318-323`). READ.

**Reading it back (READ).** There is one reader. `GearItems.idOf` (`P/weapon/GearItems.java:67-71`) returns empty for an
item with no meta or no `weapon_id`. `WeaponItems.heldWeaponId` (`WeaponItems.java:328-330`) wraps it for the main hand.

**An unminted vanilla item of the same material (READ, per call site).** It is "not ours" everywhere:
- no trigger fires (`WeaponFire`, `:122-124`);
- no melee hit lands (`WeaponFire.landVanillaMelee`, `:317-320`);
- no sweep (`RL:2231-2239`);
- no attack-damage or attack-speed contribution (`WeaponAttackItems`, `:46`, `:78`);
- no class bonus (`ClassDamageModifierItems`, `:91`);
- no damage enchants (`DamageEnchantItems`, `:73-79`);
- never broken (`WeaponDurability`, `:74`);
- not scoreable (`GearScoreItems`, `:288-298`);
- left alone by `GearRefresher` (`:66`, `Untagged`);
- refused by the enchant table, anvil and grindstone (2f).

A plain iron sword and a minted Ironblade are the same material, and only the tag tells them apart.

### 2c. What a plain vanilla sword does in a player's hand today

The path is the same for all seven materials, because no code branches on the sword's material:

1. **`RL.onPrePlayerAttack`** (`RL:2147-2164`) stores the attack charge for any mob victim, whatever is held. READ.
2. **`RL.onPlayerMeleeAttack`** (`RL:2320-2351`) handles `ENTITY_ATTACK` from a player to a non-player.
   - The broken-weapon cancel needs a `weapon_id`, so a plain sword skips it (`:2330`).
   - Then, **unconditionally**, `event.setDamage(TOKEN_DAMAGE)` (`:2335`), with `TOKEN_DAMAGE = 0.01` (`RL:170`),
     followed by `floorSoTokenCannotKill(victim)` (`:2336`). READ.
3. **`WeaponFire.landVanillaMelee`** (`P/weapon/WeaponFire.java:317-320`) looks up `heldWeaponId`. With no id it
   returns `Optional.empty()` before any damage is dealt. READ.
4. **Mob health.** Every mob is on custom HP (`RL:810-821` → `P/health/MobNameplateManager.java:265-267`).
   `VanillaDamagePolicy` passes melee causes through untouched, because the rider above owns them
   (`P/health/VanillaDamagePolicy.java:139-140`). So the vanilla number is never converted into custom HP. READ.
5. **The repo states it outright.** `paper/src/main/resources/selftest/GATE-dragon-split.yml:12-15`: *"a vanilla iron
   sword deals NO custom damage to a custom mob by design."* READ.

| sword | damage our pipeline deals | attack speed | sweeps | enchants |
|---|---|---|---|---|
| wooden, stone, copper, iron, golden, diamond, netherite | **0 custom HP**. A 0.01 vanilla token lands for the hurt flash and i-frames, floored so it cannot kill | **vanilla 1.6**. Our reconcile removes its own modifier when no weapon is held (`P/weapon/WeaponAttackItems.java:77-84`, `C/combat/AttackSpeedAttribute.java:51-53`, `P/health/AttackSpeedAttributeOverride.java:57-61`), so the material's −2.4 stands. READ for the code; INFERRED that the vanilla value then applies | **no**. The sweep event is cancelled when the held item has no `sweep` (`RL:2231-2239`, `C/combat/SweepShare.java:62-64`). READ | **none of ours** (`DamageEnchantItems` returns an empty map, `:73-78`). A vanilla enchant cannot be applied by a player (2f). If one were present, its bonus would sit inside the vanilla amount that `setDamage(0.01)` replaces. INFERRED |

Also READ:
- **No melee class bonus.** The held class is null (`P/weapon/ClassDamageModifierItems.java:91-96`).
- **No gear score.** The `GearScoreItems` javadoc names "a vanilla sword" as not a candidate (`:243-246`).
- **No ATTACK_DAMAGE stat contribution** (`WeaponAttackItems.java:24-25`, `:46-55`).

**Side effects (INFERRED from READ code):**
- A plain-sword hit still claims the victim's 10-tick melee window (`RL:2342`). That blocks another player's custom
  hit on that mob for 10 ticks.
- It still gets vanilla knockback.
- **Vanilla still wears the plain sword**, because no `PlayerItemDamageEvent` handler covers weapons (`RL:2934-2939` is
  shields only).

**In one line: a plain vanilla sword today is a 0-damage, 1.6-speed, non-sweeping prop that still wears out.**

### 2d. Mint on craft

**How it works for a tool (READ):**
- **Parsing.** `content/tools/iron.yml` declares `craft_result`. `ToolLoader` reads it from each entry's own section
  (`P/content/ToolLoader.java:143-147`). There the section key is both the id and the material.
- **Normalising.** `CraftResultToken.normalise` (`C/weapon/CraftResultToken.java:59-68`) lower-cases the claim and
  strips `minecraft:` (`token`, `:38-42`). It is empty when absent and throws on blank.
- **The index.** `CraftResultIndex` (`C/weapon/CraftResultIndex.java`) builds `byResult` (material → gear) from every
  claim (`:116-121`). A material claimed by two definitions is dropped from the index, with a warning naming both
  (`:126-142`). It also builds `byRecipe` from `recipes/*.yml` `mints:` (`:152-171`).
- **Who feeds the index.** It is built at boot from **weapons, shields, armour and tools** (`P/RpgPlugin.java:481-487`).
- **Where the swap happens.** In our own crafting menu, not in a Bukkit event:
  - The CRAFTING_TABLE block is hijacked into `CraftingMenu` (`RL:363-364`, `openHijackedBlock` `RL:1408-1420`).
  - `InventoryCraft.commitCraft` (`P/menu/InventoryCraft.java:271-325`) takes `Bukkit.craftItemResult(...)`, asks
    `RecipeProbe.claimedBy`, and on a claim runs `GearItems.mint`, then `EnchantRollItems.rollOnAcquire`, then
    `GearScoreItems.stampOnAcquire` (`:298-323`).
  - `RecipeProbe.claimedBy` (`P/menu/RecipeProbe.java:384-397`) checks our recipe key first. It then checks the result
    material, gated on the result having durability (`WeaponDurability.maxOf(result).isEmpty()` → no claim).
- **What refuses at boot: nothing hard.** `ContentValidator.validateCraftResults`
  (`P/content/ContentValidator.java:230-260`) flags three cases: a claim that is not a material, a claim that is not
  the definition's own `material()`, and a material with no durability. **It only warns.** `RpgPlugin` logs each
  problem as `Content:` and then *"The server is still running"* (`P/RpgPlugin.java:1065-1071`), and the index is built
  from every claim regardless. READ.
  - The only hard refusals are a blank `craft_result` (the record throws and the file is skipped) and, for tools, a
    `kind` that does not match the material (`C/weapon/ToolDefinition.java:95`).
  - `CraftResultValidationTest` (`paper/src/test/.../content/CraftResultValidationTest.java:43-51`) exercises shield and
    armour fixtures only, with fake predicates. **It has no weapon case.**

**Can a WEAPON use it today? YES, with no Java change (READ):**
- `WeaponLoader` lists `craft_result` as a known key (`:138-140`) and parses it with no default (`:390-394`).
- `WeaponDefinition` normalises it (`C/weapon/WeaponDefinition.java:126`), and the index takes weapons (`RpgPlugin.java:481-487`).
- `WeaponLoaderTest.java:906` authors `craft_result` on a weapon.
- **No shipped weapon uses it.** The three recipe-made staves (`flint_staff`, `lapis_staff`, `cursed_emerald`) use the
  second route below instead, because their materials have no durability (`flint_staff.yml:152-163`).

**The second route (READ).** `recipes/*.yml` declare `mints:`, `shape:` and `ingredients:`.
- `RecipeRegistrar.build` registers a `ShapedRecipe` keyed `rpg:<file>` whose result is a plain stack of the gear's
  material (`P/content/RecipeRegistrar.java:151-180`).
- `claimedBy`'s recipe-key arm then mints it, with no durability gate.
- A sword could use this route, but would need its own shape. A copy of vanilla's sword shape would compete with the
  vanilla recipe, and which one the matcher picks was not read.

**What a sword ladder would take for crafting (INFERRED from the READ code above):**
- Each tier file declares `material: <x>_sword` and `craft_result: <x>_sword`. All seven materials have durability.
- **Exactly one definition may claim each material.** Ironblade and emberblade sit on `iron_sword` but claim nothing,
  so an iron-tier sword claiming `iron_sword` is not contested. If a second file ever claimed it, the index would drop
  **both** and neither would mint (`CraftResultIndex.java:126-142`), and boot would only warn.
- Vanilla sword recipes are three rows tall, so the 2x2 inventory grid, which does not mint (`RecipeRegistrar.java:136-143`),
  cannot make one. The Crafter block refuses any durable output (`RL:1879-1881`).
- **So crafting needs no code.** The only code worth considering is making the contested-claim warning fail closed,
  which is the seat's call and not required.

### 2e. Every other way a sword reaches a player

| route | intercepted today? | what the player gets |
|---|---|---|
| **Crafting table** | yes, through the hijacked menu (2d) | minted if a weapon claims the material; **today no weapon claims any sword, so a plain vanilla sword** |
| **`/rpg give <id>`** | yes. Brigadier, `RpgCommand` (`P/command/RpgCommand.java:217-241`), and `give()` (`:1580-1650`) mints, rolls, stamps and adds | a minted weapon |
| **Smithing table** | **no.** A case-insensitive grep for `smith` in `paper/src/main` finds only a comment (`P/menu/RecipeProbe.java:176`). SMITHING_TABLE is not among the hijacked blocks (`RL:360-430`), and there is no `PrepareSmithingEvent` or `SmithItemEvent` listener. READ | vanilla behaviour (INFERRED). See the netherite note below |
| **Chest loot** | **no.** There is no `LootGenerateEvent` or `LootTable` code on master. READ | vanilla items (INFERRED) |
| **Mob drops** | **no.** Death handlers only clear drops: shots (`RL:1040-1044`), player death (`RL:2054-2057`), and the dragon in a fight (`P/fight/FightInstance.java:584-586`). No mob YAML has a `drops:` key. READ | vanilla drops, including a zombie's or skeleton's held sword (INFERRED) |
| **Villager trades** | **no.** Nothing matches `VillagerAcquireTrade` or `MerchantRecipe`, apart from one javadoc mention (`P/menu/CraftingMenu.java:531`). READ | vanilla (INFERRED) |
| **Loot cascade** | not on master. It is PR #219, held, with no source wired | — |

`rollOnAcquire` and `stampOnAcquire` have exactly two callers, `RpgCommand.java:1635/1642` and
`InventoryCraft.java:319/323`. So **`/rpg give` and the crafting table are the only routes that mint today.** READ.

**A minted weapon through a vanilla netherite upgrade:**
- **Vanilla's part (INFERRED).** The smithing transform copies the base item's components onto `netherite_sword`, so
  `custom_data` (our PDC id, enchants and gear score), the name, lore and attribute modifiers survive.
- **Our part (READ).** Every join runs `GearRefresher.refresh` (`RL:714`, `P/weapon/GearRefresher.java:46-118`), which
  calls `WeaponItems.remint`, which calls `mint`, which builds `new ItemStack(materialOf(weapon.material()))`
  (`WeaponItems.java:90`, `:356-368`). Only the id, wear, enchants, quiver and gear score carry over
  (`GearItems.carryInstanceData`, `P/weapon/GearItems.java:253-260`). `/rpg refresh`, the enchant table, `/rpg enchant`
  and the grindstone take the same re-mint path.
- **The outcome (INFERRED).** **The netherite sword silently reverts to the definition's material on the next re-mint,
  and the ingot is lost.** Nothing maps a diamond-tier id to a netherite-tier id.

### 2f. Enchants

**Each enchant file (READ; the id is the filename, `P/content/EnchantLoader.java:61-65`):**

| id | `class:` (line) | effect (line): what it does | max_level (line) | values |
|---|---|---|---|---|
| attunement | mage (`:23`) | `damage` (`:22`): ×(1+v%) while a mage weapon is held | 3 (`:21`) | 5/10/15 |
| bulwark | shield (`:46`) | `block_dr` (`:45`): +v points to the block fraction, capped at 1.0 | 3 (`:44`) | 5/10/15 |
| deft_hands | ranger (`:32`) | `reload_time` (`:31`): reload −v% | 3 (`:30`) | 10/20/30 |
| expanded_quiver | ranger (`:65`) | `quiver_size` (`:64`): +v arrows | 3 (`:63`) | 1/2/3 |
| growth | armor (`:37`) | `max_health` (`:36`): +v max health per piece | 3 (`:35`) | 10/20/30 |
| mana_bank | armor (`:47`) | `max_mana` (`:46`): +v max mana per piece | 3 (`:45`) | 10/20/30 |
| power | ranger (`:17`) | `damage` (`:16`): ×(1+v%) on a ranger weapon | 3 (`:15`) | 5/10/15 |
| protection | armor (`:37`) | `defense` (`:36`): +v armour per piece | 3 (`:35`) | 3/6/9 |
| **sharpness** | **melee** (`:33`) | `damage` (`:32`): ×(1+v%) on a melee weapon | 3 (`:31`) | 5/10/15 |
| thorns | shield (`:50`) | `reflect` (`:49`): reflects v% of the raw blow while blocking | 3 (`:48`) | 10/20/30 |
| **unbreaking** | **universal** (`:31`) | `durability` (`:30`): skips wear, at chance 1/(level+1) (`C/enchant/Unbreaking.java:51-56`) | 3 (`:29`) | none |

**How an enchant decides eligibility (READ).** **On class alone, never on material.**
- `EnchantLoader` requires `class:`. `universal` becomes null, and everything else goes through `GearClass.fromName`
  (`P/content/EnchantLoader.java:116-137`).
- `EnchantDefinition` ties each effect to a gate, for example DAMAGE → main hand only (`P/content/EnchantDefinition.java:119-141`).
- The roll pool keeps an enchant whose class is null or equals the gear's class (`C/enchant/EnchantRoll.java:83-98`).
- The gear's class comes from `GearItems.gearClassOf` → `GearClass.of(weapon.weaponClass())` (`P/weapon/GearItems.java:216-224`).
- At runtime, `DamageEnchantItems` reads `weapon_id`, then the definition, then the class (`:73-79`), and
  `DamageEnchants.matching` skips a grant of another class (`C/enchant/DamageEnchants.java:106-113`).
- **No material allowlist exists.** A grep of `P/` for `_SWORD`, `Tag.ITEMS` and `Set.of(Material` finds only the
  mint fallback.

**So a minted melee sword's pool is `sharpness` + `unbreaking`.**

**How enchants are applied (READ):**
- **Rolled on acquisition only:** `/rpg give` and a minting craft (2b). The roll writes candidates.
- **Enchanting table: replaced.** It opens `EnchantMenu` (`RL:360-362`), and the vanilla event is always cancelled
  (`RL:1408-1420`). The table only unlocks, activates or levels candidates already rolled onto the item, charging XP,
  and then re-mints (`P/menu/EnchantMenu.java:610-689`).
- **Anvil, all three states: replaced** with `AnvilMenu` (`RL:392-397`). It moves gear score between items of a kind
  and never touches enchants.
- **Grindstone: replaced.** It strips our enchants and re-mints (`P/menu/GrindstoneMenu.java:360-375`).
- **Command.** `/rpg enchant show|clear|candidate|level`, dev permission (`RpgCommand.java:297-320`). `candidate` has
  **no class check** (`:1866-1880`), by design, as a gate instrument.
- **Storage.** PDC `enchant_data` (STRING) and `enchant_rolled` (BYTE) (`Keys.java:492-493`), in the wire format of
  `C/enchant/EnchantCodec.java:13-20`. They are carried across a re-mint as raw bytes (`GearItems.java:92-101`).
- **Vanilla enchantments** are neither stripped nor read. A player cannot apply one, because the table and anvil are
  replaced. The known leak is mob gear that spawns already enchanted, left as an open decision (`docs/NEXT.md:7633-7640`).

**Would anything refuse or mishandle a minted sword? No (READ).** A minted melee weapon on `iron_sword` is already the
shipped case (ironblade, emberblade). Every check keys on `weapon_id` and the class, never on the material. The only
material-sensitive guard is a stack-size check (`EnchantMenu.java:318-321`), and swords do not stack.
**An unminted sword is refused at every surface:** the table (`EnchantMenu.java:307-312`), the anvil
(`AnvilMenu.java:235-237`), the grindstone (`GrindstoneMenu.java:150`) and `/rpg enchant` (`RpgCommand.java:2193-2195`).

**Enchants across a smithing upgrade today.** There is no smithing code (2e).
- INFERRED: vanilla's transform keeps `custom_data`, so `enchant_data` survives the upgrade itself.
- READ: the next re-mint carries `enchant_data` byte for byte (`GearItems.java:253-260`).
- **So the enchants already survive, and the netherite MATERIAL is what does not.** Ben's answer 4 ("keeps its
  enchantments") needs no new enchant code. It needs the upgrade to land on a netherite-tier definition.

### 2g. Durability, repair and Unbreaking on a minted weapon

- **Max durability is the material's vanilla maximum** (`P/weapon/WeaponDurability.java:22-28`, `maxOf` reads the
  item's `Damageable`). READ. So each tier sword would inherit the jar's figure in 2i: wooden 59 up to netherite 2031.
- **Wear (READ).** Our pipeline charges 1 per landed basic hit. `CastExecutor.landBasicMelee` → `onBasicAttackUse`
  (`C/ability/CastExecutor.java:314`) → `WeaponDurability.applyWearOnUse`, with `WEAR_PER_USE = 1`
  (`WeaponDurability.java:82`, wired at `P/weapon/WeaponFire.java:332`). That is at most one wear per mob per 10-tick
  window. Sweep never wears (`RL:2210-2213`).
- **Unbreaking applies to weapons** (`WeaponDurability.java:134-135`): wear is consumed with chance 1/(level+1). READ.
- **Breaking (READ).** The item is never destroyed. Wear stops at `max − 1`, and that state counts as broken
  (`C/weapon/Durability.java:33`, `:52-55`, `:66-69`, `:85-89`).
  - The player gets *"Your weapon is broken -- repair it before using it."* plus the item-break sound, throttled
    (`P/weapon/BrokenNotice.java:53`, `:101-111`).
  - Afterwards the swing event is cancelled outright (`RL:2327-2333`).
- **Repair: none in play.** The ruling in the code (`RL:384-388`): *"vanilla rename and vanilla repair are BOTH GONE
  ... NOTHING in the game restores durability except /rpg durability, which is a dev route."* `AnvilMenu` never touches
  durability. Mending does not exist as an item enchant, and `mending_charm`/`fragment_mending` grant `health_regen`
  only. READ.
- **Open risk: vanilla may ALSO wear a minted sword.** It is INFERRED, not measured.
  - READ from the jar: every sword's `applySwordProperties` adds a `DataComponents.WEAPON` component,
    `new Weapon(1)`, with item damage per attack 1. The minted item keeps its material's components, and its positive
    attack-damage modifier lets vanilla's attack run.
  - Nothing cancels `PlayerItemDamageEvent` for weapons.
  - The one "vanilla wears nothing" measurement (`docs/NEXT.md:7664-7677`) predates vanilla-driven melee.
  - If vanilla does charge, wear is about 2 per hit, our Unbreaking reduces only our half, and vanilla's charge could
    pass our `max − 1` floor and destroy the item.
  - **One boot row settles it:** 10 hits with a fresh minted sword, then read the bar.
  - **This matters more on the ladder than it did before:** a golden sword has 32 durability.

### 2h. Swords in the pinned jar

READ, by `javap`:
- `net.minecraft.world.item.Items` in `run/versions/26.1.2/paper-26.1.2.jar` declares exactly seven `*_SWORD` fields:
  `WOODEN_SWORD`, `COPPER_SWORD`, `STONE_SWORD`, `GOLDEN_SWORD`, `IRON_SWORD`, `DIAMOND_SWORD`, `NETHERITE_SWORD`.
- `org.bukkit.Material` and `org.bukkit.inventory.ItemType` in `paper-api-26.1.2.build.74-stable.jar` declare the same
  seven. `Material` also has five `LEGACY_*_SWORD` constants (no copper), which are not real items.

**Copper has a vanilla sword in 26.1.2: `minecraft:copper_sword`. READ.**

The jar also registers seven `*_spear` items (wooden, stone, copper, iron, golden, diamond, netherite) in
`Items`. READ, recorded only because Ben's "similar weapons of the other classes" may want to know. No proposal follows
from it.

### 2i. Vanilla sword numbers, from the jar, and Ben's ×3

**READ, by `javap -c`:**
- Every one of the seven is registered as `Item$Properties.sword(ToolMaterial.X, 3.0f, -2.4f)` (`Items.<clinit>`).
- `ToolMaterial.createSwordAttributes` adds two `MAINHAND` modifiers, both `ADD_VALUE`:
  - ATTACK_DAMAGE `3.0 + attackDamageBonus`, under `Item.BASE_ATTACK_DAMAGE_ID`;
  - ATTACK_SPEED `-2.4`, under `Item.BASE_ATTACK_SPEED_ID`.
- `Player.createAttributes` sets the player's base ATTACK_DAMAGE to `1.0` (`dconst_1`). ATTACK_SPEED is added with its
  registered default `4.0` (`Attributes.<clinit>`).
- `ToolMaterial.<clinit>` constructor arguments are, in order, (incorrect-blocks tag, durability, mining speed,
  attackDamageBonus, enchantmentValue, repair tag):

| sword | ToolMaterial | durability | attackDamageBonus | **modifier** (3 + bonus) | **total** (player base 1 + modifier) | attack speed (4.0 base, −2.4 modifier) | enchantmentValue |
|---|---|---|---|---|---|---|---|
| wooden | WOOD | 59 | 0 | 3 | **4** | 1.6 | 15 |
| stone | STONE | 131 | 1 | 4 | **5** | 1.6 | 5 |
| copper | COPPER | 190 | 1 | 4 | **5** | 1.6 | 13 |
| iron | IRON | 250 | 2 | 5 | **6** | 1.6 | 14 |
| golden | GOLD | 32 | 0 | 3 | **4** | 1.6 | 22 |
| diamond | DIAMOND | 1561 | 3 | 6 | **7** | 1.6 | 10 |
| netherite | NETHERITE | 2031 | 4 | 7 | **8** | 1.6 | 15 |

The speed column is the float `-2.4f` added to `4.0`. It is shown as 1.6, which is what both melee weapons author
today (`ironblade.yml:28`).

**Ben's ×3, both readings:**

| sword | × 3 of the **total** | × 3 of the **modifier** |
|---|---|---|
| wooden | 12 | 9 |
| stone | 15 | 12 |
| copper | 15 | 12 |
| iron | 18 | 15 |
| golden | 12 | 9 |
| diamond | 21 | 18 |
| netherite | 24 | 21 |

**Which base our content uses (READ).** Our `attack_damage` is the **total**: `WeaponItems.attackDamageModifier` writes
`attack_damage − 1` (2b), and ironblade's 8 sits against vanilla iron's 6 total.
- **So the total column is the reading that fits the schema.**
- **FLAG FOR BEN: the two readings differ by 3 on every tier** (iron 18 against 15).
- INFERRED: vanilla's own tooltip shows the total ("6 Attack Damage" on an iron sword), so "vanilla numbers" most
  plausibly means the total. **Ben rules it.**

### 2j. Which tiers come out equal

With the jar's numbers:
- **golden = wooden**: 4 total, 12 at ×3;
- **copper = stone**: 5 total, 15 at ×3.

This matches the seat's expectation. **And attack speed is 1.6 on all seven**, so at "largely vanilla numbers" the
two side tiers are identical to the tier they sit beside in damage AND speed.

What vanilla does vary between each pair (READ):

| pair | durability | enchantmentValue |
|---|---|---|
| golden vs wooden | 32 vs 59 | 22 vs 15 |
| copper vs stone | 190 vs 131 | 13 vs 5 |

Neither column does anything on our side today (INFERRED from READ code):
- **enchantmentValue** feeds the vanilla table, and our table is replaced. Our rolls key on class alone (2f).
- **Durability** has no repair in play (2g), so golden's 32 makes it strictly worse than wooden, not sideways.

**At plain "vanilla × 3", "sideways" would have had to come from something other than damage or speed.** Ben's 1.2b
answers it for the swords by taking golden and copper OFF vanilla × 3. They deal more than stone and less than iron.

**The whole numbers strictly between stone and iron, with the jar's real numbers (2i).** These are integers between
two integers, so they are exact:

| ×3 base (2i) | stone × 3 | iron × 3 | whole numbers strictly between |
|---|---|---|---|
| **total** (player base 1 + modifier) | 15 | 18 | **16, 17** |
| **modifier** (3 + attackDamageBonus) | 12 | 15 | **13, 14** |

- **There are two in either case**, because the gap is 3 under both bases: iron's bonus is 1 above stone's, and ×3
  makes the gap 3.
- **But the base changes WHICH numbers they are, and that breaks the placeholder.**
  - **The seat's placeholder of 16 holds only under the total reading.**
  - Under the modifier reading, 16 is above iron's 15. It would break Ben's "less than iron", and "copper 16, golden
    17" would break it for both swords.
- **FLAG FOR BEN:**
  - rule 2i's base first;
  - then pick the golden and copper numbers from that row;
  - "both at 16 / copper 16 and golden 17" becomes "both at 13 / copper 13 and golden 14" if the modifier reading wins.
- **Only two values exist between them,** so "both the same" or "one each" are the only shapes available. A wider choice
  would need a fractional `attack_damage`. The schema reads it as a double (`s.getDouble("attack_damage", 0.0)`,
  `P/content/WeaponLoader.java:293`, READ), and no shipped melee weapon authors one.

Golden and copper keep **vanilla's attack speed (1.6) and durability (32 golden, 190 copper)** (seat reading, 1.2b).
2g's no-repair note still applies: **the golden sword has 32 durability and nothing in play repairs it.**

### 2k. Existing ranger and mage weapons that could sit on a ladder

Ben: *"some of them are already in the game."* Every non-deletion ranger and mage weapon, in order of authored
per-hit damage. **No tiers are assigned; that is Ben's.**

The figures are not comparable across classes or cadences. A ranger's shot has a `cooldown_ticks` and a magazine, and
a mage's cast costs mana. The order is by the number alone.

| weapon | class | rarity | material | authored damage (source) | cadence / cost |
|---|---|---|---|---|---|
| dragons_plume | ranger | legendary | bow | 34 charged (`attack_damage`, `:99`); taps 9 / 17 / 26 (`:637`, `:673`, `:709`) | taps `cooldown_ticks: 8` |
| blaze_kings_staff | mage | legendary | blaze_rod | 30 (`on_hit` damage, `:67`) | 8 mana, `cooldown_ticks: 8` |
| cursed_emerald | mage | uncommon | emerald | 27 (ray `on_hit`, `:199`) | 40 mana, `cooldown_ticks: 30` |
| locust | ranger | rare | crossbow | 26 (`attack_damage`, `:126`) | `cooldown_ticks: 12` |
| lapis_staff | mage | uncommon | breeze_rod | 21 (ray `on_hit`, `:93`) | 10 mana, `cooldown_ticks: 20` |
| flint_staff | mage | common | stick | 20 (`on_hit` damage, `:117`) | 5 mana, `cooldown_ticks: 24` |
| boltor | ranger | uncommon | crossbow | 19 (`attack_damage`, `:132`, RULED) | `cooldown_ticks: 16` |
| short_bow | ranger | common | bow | 16 (`attack_damage`, `:35`) | `cooldown_ticks: 12` |
| withered_shortbow | ranger | uncommon | bow | 16 (`attack_damage`, `:37`) | `cooldown_ticks: 16` |
| ember_staff | mage | rare | blaze_rod | 16 (burst `on_hit`, `:50`) | 30 mana, `cooldown_ticks: 20` |
| dragons_breath | ranger | legendary | crossbow | 9 per arrow (`:387`), a spread of 7 | `cooldown_ticks: 32` |

`volley_stone` is left out. It is a mage-class dev tool (`unscored: true`, `volley_stone.yml:70`), and its class line
says *"mage is as good as any here"* (`:34`).

---

## 3. THE DELETION SET: ironblade, emberblade, hunters_bow, quiver_stone

**Nothing is removed by this survey.**
- **What the counts are.** Matching LINES, from case-insensitive `rg` over the patterns `ironblade`, `emberblade`,
  `hunters_bow|hunter'?s.?bow`, `quiver_stone|quiver.?stone`.
- **Scope.** `target/` and `run/` were excluded. The deployed copies exist in `run/plugins/Rpg/content/weapons/`.
- **Positive control.** `rg -c -i boltor content/weapons/boltor.yml` = 19.
- **Provenance.** The counts were taken by a survey agent at `868df417`. The decisive lines were re-read by hand:
  `WeaponLoreTest.java:276`'s hard-coded id list, and the token and sweep lines in 2c.

| id | content (own file + other files) | main Java | tests | selftest scenarios | gates | plans | other docs |
|---|---|---|---|---|---|---|---|
| ironblade | 15 (3 + 12 comments in 7 files) | 9, **all comments** | 68 | 0 | 8 in 5 files | 42 | 38 |
| emberblade | 14 (3 + 11 comments in 9 files) | 12, **all comments** | 75 | **32 lines in 8 files: 13 give/hold step pairs** | 45 in 20 files | 54 | 40 |
| hunters_bow | 17 (2 + 15 comments in 8 files) | 24, **all comments** | 55 | 0 | 32 in 10 files | 54 | 45 |
| quiver_stone | 20 (3 + 17 comments in 6 files) | 19, **all comments** | 24 | 5 lines in 1 file (3 steps) | 40 in 8 files | 62 | 30 |

"Other docs" means `docs/NEXT.md`, `.claude/rules/*.md`, `CLAUDE.md` and `docs/HANDOFF-damage-system.md`.

**Shared by all four:**
- **No main Java hard-codes any of them.** Every main-code hit is a comment or javadoc.
- **No content file references one in a way that loads.** Every cross-reference is a `#` comment.
- **`GoldenLoreTest.everyShippedTooltipIsUnchanged`** reads `src/main/resources/content/weapons` directly. It fails at
  once on any deletion until `paper/src/test/resources/golden-lore.txt` drops that weapon's block (`-- emberblade` `:84`,
  `-- hunters_bow` `:112`, `-- ironblade` `:121`, `-- quiver_stone` `:154`).
- **`ContentFreshnessTest`** goes red on a warm `target/`, because the stale classpath copy survives.
- Items already minted in worlds would go down `RefreshVerdict`'s deleted-id path. INFERRED.

**What breaks per id if its file were removed today:**
- **ironblade:**
  - Tests:
    - `WeaponLoaderTest.bundledIronbladeContentLoads` (`:669-700`, real file, after a clean build);
    - `WeaponLoreTest.everyShippedWeaponRendersAgainstTheShippedElements` (hard-coded list at `:276`).
  - Gate rows become unreplayable as written:
    - `GATE-ignite.md` kinetic control;
    - `GATE-element-accrual.md` knell row;
    - `GATE-nexus.md` sharpness staging;
    - `GATE-gearscore.md` SCALE row.
  - Selftest: none.
- **emberblade (the largest set):**
  - Tests:
    - `WeaponLoaderTest.bundledEmberbladeContentLoads` (`:704-741`);
    - `WeaponLoaderTest.onlyTheBowsRightClickIsABasicAttack` (asserts the file is present and `registry.size()==3`);
    - `WeaponLoreTest` `:276`.
  - **13 selftest steps across 7 scenario files fail at run time.** `rpg give emberblade` / `hold: emberblade` appear in
    `GATE-dragon-blink`, `GATE-dragon-shot`, `GATE-drag-additive`, `GATE-fight-dragon`, `GATE-dragon-skreel`,
    `GATE-level` and `GATE-fight-site`. **No unit test checks a scenario's weapon ids against content**, so this would
    surface only in a run. INFERRED.
  - It is **the melee weapon the dragon selftests kill with**. A replacement melee weapon must exist, and the scenarios
    must be repointed, before emberblade can go.
  - Gate rows include:
    - GATE-level BN2, WD1, WD2;
    - GATE-melee-cell MC5, MC6, MC8;
    - the DS1 chain (dragon-fix DS1R2 through implosion-drag DS1R6);
    - GATE-fight-site MG2R2;
    - GATE-dragon-shot SH2;
    - GATE-nexus-polish D rows.
- **hunters_bow:**
  - Tests:
    - `WeaponLoaderTest.bundledHuntersBowContentLoads` and `onlyTheBowsRightClickIsABasicAttack`;
    - `WeaponLoreTest` `:276`;
    - `HeldFireQuantisationPinTest` fails **by design** ("PINNED FIXTURE GONE"; restate the reading in NEXT.md, then
      drop the row);
    - `DeftHandsContentInvariantTest` and `ExpandedQuiverContentInvariantTest` REACHABLE counts (8 → 7).
  - Their `EXEMPT_DEV_WEAPONS = List.of("hunters_bow")` literals become dead. These are the marked deletion-set
    citation sites.
  - Gate rows: GATE-q7 Q7 (order load-bearing) and the boltor and locust readings tables.
- **quiver_stone:**
  - Tests:
    - `HeldFireQuantisationPinTest` (by design);
    - both content-invariant REACHABLE counts.
  - The constants `9` and `34` in `QuiverSizeTest`, `ReloadTimeTest` and the two modifier-item tests do not load the
    file. They still pass, but would match no shipped weapon. The dev instruments' `DEFAULT_BOOST` values were sized
    against it. INFERRED.
  - Selftest `GATE-deft-hands` DH3 and DH6 fail at run time.
  - It is the fixture of `GATE-quiver` and `GATE-quiver-a2`.

**Ordering consequence.** The deletion is still parked on *enough shipped weapons to replace them*. The sword ladder
supplies the melee replacements. **Emberblade cannot go until a tier sword exists AND the 13 selftest steps are
repointed at it.**

---

## 4. WHAT A SWORD LADDER WOULD TAKE: A PROPOSAL, NOT A BUILD

### 4.1 The content shape, seven files

One file per sword in `content/weapons/`. Every number below is a placeholder until Ben rules 2i's base. The shape
copies the existing melee schema (`attack_damage`, `attack_speed`, `sweep`, a `left_click` with `cast: type: melee`
and a `weapon_damage` on_hit). **It does not take any number from ironblade or emberblade**: their numbers are not
balanced (standing decision *NEW CONTENT CITES THE BOLTOR*), and Ben's ruling sources every number from vanilla × 3.

```yaml
id: ___                     # names are OPEN (section 5)
display_name: "___"
element: kinetic            # RULED, all seven
rarity: common              # RULED: common for wooden/stone/copper/iron/golden; uncommon for diamond/netherite
class: melee
material: iron_sword        # one of the seven, 2h
craft_result: iron_sword    # the same material; exactly one file may claim it (2d). Omitted on netherite (4.2 step 4)
attack_damage: ___          # vanilla x 3 (2i): 18 (total) or 15 (modifier) for iron. Ben rules which.
                            # golden and copper are NOT x 3: strictly between stone and iron (1.2b, 2j)
attack_speed: 1.6           # vanilla, all seven (2i)
sweep: ___                  # OPEN: see the note below
triggers:
  left_click:
    cast:
      type: melee           # the discriminator BasicMelee.isVanillaDriven reads; keep it
      reach: 3.0
      arc_degrees: 120
    on_hit:
      - type: weapon_damage
        element: kinetic
```

> **SWEEP IS NOT SETTLED BY "VANILLA".** Our `sweep:` is a FRACTION of the primary hit's final number
> (`ironblade.yml:30-38`, `C/combat/SweepShare.java`). Vanilla's sweep is not a fraction of the hit. It is a separate
> small amount raised by Sweeping Edge (INFERRED; not read from the jar for this survey). **"Sweep stays vanilla's"
> therefore has no direct translation.** The two shipped swords use 0.5. That is a seat or Ben question, listed in 5.

### 4.2 The smallest set of code changes, by 2d–2g

1. **Crafting (2d): no code.** It is content only: `craft_result` on six files.
   - Optional: make a contested or mismatched `craft_result` fail closed at boot instead of warning
     (`ContentValidator.validateCraftResults`, `RpgPlugin`'s Content loop). A contested claim silently mints neither.
2. **Enchants (2f): no code.** A minted sword's pool is already `sharpness` + `unbreaking`, and every enchant surface
   accepts it.
3. **Durability (2g): no code for max durability**, which already comes from the material.
   - **One measurement is owed first:** does vanilla also wear a minted sword (2g, open risk)?
   - If it does, the fix is a `PlayerItemDamageEvent` cancel scoped by `weapon_id`, the same shape as the shield
     handler at `RL:2934-2939`.
4. **Netherite (2e, Ben's answer 4): code, small.**
   - **Content.** The diamond-tier file names its upgrade, e.g. `smithing_upgrade: <netherite id>`, a new optional
     key; or the netherite file names its base. The netherite file claims no `craft_result`, since netherite swords are
     not crafted.
   - **Code.** One listener on `PrepareSmithingEvent` (and/or `SmithItemEvent`).
     - When the base is a minted weapon with a declared upgrade, it replaces the result with
       `mint(upgradeDefinition)`.
     - It then carries the instance data through the existing `GearItems.carryInstanceData` (`P/weapon/GearItems.java:253-260`):
       enchants, wear and gear score. **This is what makes "keeps its enchantments" true.**
     - Wear clamps to the new maximum, and 1561 → 2031 only widens.
   - **A refusal is owed too:** a minted weapon with **no** declared upgrade must not be smithed into netherite and
     then re-minted back (today's silent ingot loss, 2e).
   - The core test: a pure mapping, "base id + template + addition → upgrade id or refuse".
5. **Every other acquisition route (2e, Ben's answer 5): code. This is the largest piece and needs a decision first.**
   Two shapes:
   - **(a) Per source.** Intercept chest loot (`LootGenerateEvent`), mob drops (`EntityDeathEvent` drops, including a
     zombie's held sword), villager trades (`VillagerAcquireTradeEvent` or the trade result), and any later loot source.
     There are four or more listeners, each needing `mint` + `rollOnAcquire` + `stampOnAcquire`, and a new source is a
     new hole.
   - **(b) Convert on arrival.** One rule: an untagged vanilla sword whose material a weapon claims is minted the
     moment it reaches a player's inventory (pickup, chest take, trade result), with `GearRefresher` sweeping
     inventories on join as the backstop.
     - Today `GearRefresher` leaves `Untagged` items alone (`P/weapon/GearRefresher.java:66`), so it would gain one
       arm.
     - It is one place, and it also converts the vanilla swords players already hold, which fits "ANY route".
     - The cost is more inventory events to cover.
   - **The survey recommends (b)** as the smaller and closed design, but it is the seat's and Ben's call.
   - Either way, the pure part, "material → claimed definition or none", is `CraftResultIndex.forResult`, which
     already exists. So the core test is small.

### 4.3 Tests and gate rows the steps would need

- **Unit (core first, per CLAUDE.md):**
  - the smithing upgrade mapping (4.2 step 4);
  - the convert-on-arrival decision (4.2 step 5: untagged + claimed material → mint, tagged → leave, unclaimed →
    leave);
  - a `CraftResultValidationTest` weapon case (it has none today).
- **Content tests:**
  - `GoldenLoreTest`'s golden file gains one block per new sword. It discovers every shipped weapon.
  - A test that each of the seven materials is claimed by exactly one weapon, so a contested claim cannot pass silently.
- **Gate rows** (each a later gate, committed before its boot):
  - craft each tier at the table → minted, tooltip, rarity colour;
  - hit a mob with each tier → custom damage equals the ruled figure (read against the HURT line, per
    `.claude/rules/verification.md`);
  - sweep present or absent as ruled;
  - 10 hits → durability bar, which settles 2g's open risk;
  - enchant a sword at the table → sharpness/unbreaking only;
  - smith a diamond tier with enchants → netherite tier with the same enchants, still netherite after a rejoin;
  - a dropped or looted or traded vanilla sword → minted on arrival;
  - a plain vanilla sword already in an inventory → minted on join.

### 4.4 An order of small steps

1. **Rulings first** (section 5): the ×3 base, sweep, names. No code.
2. **One iron-tier sword, content only.** Prove mint-on-craft, the tooltip, the damage figure and the wear question in
   one small boot. No Java.
3. **The other six tier files, content only.** The netherite file has no `craft_result`.
4. **Netherite smithing.** A core mapping + test, then the one listener.
5. **Convert-on-arrival** (or per-source, as ruled). A core decision + test, then the wiring.
6. **The vanilla-wear fix,** only if step 2's row shows double wear.
7. **Repoint the selftests** from emberblade to a tier sword. Then the deletion of ironblade and emberblade can be
   scoped as its own slice, per section 3.

### 4.5 Where the ranger and mage ladders would reuse the same pieces

- **The craft route.** Mint-on-craft keys on a durable vanilla material. `bow` and `crossbow` are durable, so a ranger
  tier on a vanilla bow or crossbow uses `craft_result` unchanged.
  - The catch is the same: **one claim per material**. Several ranger tiers on `bow` would contest it, and the recipe
    route (`recipes/*.yml` + `mints:`) is how distinct items share a material.
  - Mage staves are already on the recipe route, because `blaze_rod`, `stick` and `breeze_rod` have no durability.
- **The smithing listener.** Its mapping is not sword-specific. Any weapon that declares an upgrade could use it.
- **Convert-on-arrival.** It applies to a vanilla `bow` or `crossbow` exactly as to a sword, but only once a ranger
  weapon claims that material.
  - Note: today a plain vanilla bow's arrow is rerouted into custom HP (`P/health/VanillaDamagePolicy.java:164`), unlike
    a plain sword, so the plain-bow case differs from 2c. INFERRED; the reroute amounts were not traced.
- **Enchants.** Ranger and mage pools already exist: power, deft_hands and expanded_quiver; attunement.

---

## 5. OPEN, FOR BEN (listed, not answered)

1. **The exact golden and copper sword numbers**, strictly between stone and iron (1.2b).
   - The seat's placeholder, not Ben's: both at 16, or copper 16 and golden 17.
   - **They depend on item 2.** Under the total reading the choices are 16 and 17; under the modifier reading they are
     13 and 14 (2j).
2. **Which vanilla number the ×3 multiplies:** the total with the player's base 1 (iron 18), or the item's modifier
   (iron 15). The schema's `attack_damage` is the total (2i).
3. **Sweep per tier.** Vanilla's sweep is not a fraction, and ours is (4.1).
4. **The ranger and mage ladders:** which existing weapons sit on which tier (2k), and what is new. Ben:
   *"Somewhat, some of them are already in the game."*
5. **Names** of the seven swords.
6. **The armour sets: LATER, by Ben's word** (1.2b). This is where the sideways step lives. It is a pointer for a
   future armour plan, and nothing is designed here.

*"What sideways means" left this list on 2026-11-19. For the swords it is answered by 1.2b.*

**Also for the seat, not Ben's design:**
- convert-on-arrival vs per-source (4.2 step 5);
- whether a contested `craft_result` should fail closed;
- the CLAUDE.md / standing-decisions update adding emberblade to the deletion set (1.1).
