# PLAN — the Broken Bottle and its guaranteed enchant, Sharpened Glass (2026-11-25)

The seat's brief of 2026-11-25, via Ben. Ben's words, verbatim:

> "I would like to pull an old melee weapon from the old repo. The broken bottle."
>
> "1. 18 Damage, 2.0 Speed. +10% Crit Chance 2. Yes it's damage scales with gear score. Not it's crit chance 3. No sweep
> 4. Common Kinetic, outside of the tiers 5. Only lootable from Shipwrecks and Sunken Treasure 6. Sharpened Glass -
> +5/10/15% Crit Chance"
>
> "1. We will do loot from the world in a separate pass. Just have CC note it 2. Both actually 3. Leave it for now"

("Both": buried treasure chests AND underwater ruin chests.) Ben's numbers REPLACE the old game's (`BROKEN_BOTTLE` at
CreaperCrusher/Butterfly-SMP `cfde822`: damage 20, +30% crit).

## THE STACK (all on #308, `feat/armor-sets-owed`)

| PR | branch | what |
|---|---|---|
| 1 | `feat/broken-bottle` | the weapon: `crit_chance` on a weapon (K2), its tooltip line, `broken_bottle.yml`, vanilla bottle use blocked (K7) |
| 2 | `feat/sharpened-glass` | the enchant: one new effect, `crit_chance` (K9), `sharpened_glass.yml`, its line, the pools (K10) |
| 3 | `feat/broken-bottle-gate` | `GATE-broken-bottle.md` and its scenario; named rows of other gates by reference; P-R1; P-R2 |

## K1 TO K8 — THE WEAPON (PR 1)

**K1, damage scales with gear score (BEN).** No new path. READ: `WeaponAttackItems.desiredAttackModifiers` scales the
held weapon's `attack_damage` by `GearScore.scaledDamage(authored, score)`; `GearScore.carriesScore` is true for a
melee weapon that does not declare `unscored`. 18 at 100 is 18; at 200 it is 36 (`BrokenBottleContentTest`).

**K2, the crit bonus.** Declared in the weapon's YAML as `crit_chance: 0.10`, a probability, the same name and unit an
accessory uses (`keen_charm.yml`: `crit_chance: 0.05`).
- **The mechanism READ:** held items reach no stat through a weapon key today. The nearest existing path is the ONE
  crit-chance reconcile in `PlayerHealthSystem`'s loop, which merges the TEMP fixture's, the accessories', the
  fragments' and the level's maps through `AccessoryContributions.merged`, every pass, off the live equipment.
- **Built:** `core/weapon/HeldWeaponCrit.sources(held)` gives `{"weapon:MAIN_HAND": 0.10}` while the main hand holds a
  weapon declaring one, and nothing otherwise. It is merged into the reconcile's FIRST (gear) argument, as the armor
  set's bonuses are. No second accumulator, no second reconcile call (`HeldWeaponCritWiringTest`).
- **FLAT (BEN: "Not it's crit chance"):** the method takes no score.
- **Accepted on any weapon:** `WeaponLoader` reads `crit_chance` (default 0, in `KNOWN_KEYS`); `WeaponDefinition` refuses
  one outside [0, 1], by name. Only the bottle ships one. No weapon id in Java.

**K3, which hits.** Every hit whose crit is rolled from the caster's crit chance. READ: `BukkitCombatant.snapshot` reads
`stats.critChanceValue` and draws the roll ONCE per cast, at the cast. Off-hand or stowed: absent from the map, so
nothing. **A hit already in flight when he switches away: for melee there is none.** The swing is the cast, and its
crit was rolled at the swing. The reconcile runs every `RECONCILE_PERIOD_TICKS`, so for up to one period after a switch
the stat can still carry the old source. INFERRED, and not read on a server.

**K4, NO SWEEP (BEN).** READ: absent `sweep` is 0.0 (`WeaponLoader`), and `RpgListeners.onPlayerSweepAttack` cancels the
`ENTITY_SWEEP_ATTACK` when `SweepShare.sweeps(fraction)` is false. **Nothing built**; pinned by
`BrokenBottleContentTest.itDoesNotSweep`. The swords are untouched.

**K5, OUTSIDE THE TIERS (BEN). READ: a weapon has no tier field.** A sword's "tier" is just its material and its
`craft_result`. What the bottle gets for each thing a tier drives:

| driven by "tier" | what it is in code | the bottle |
|---|---|---|
| durability | the MATERIAL's (`WeaponDurability.maxOf`) | none: see K6 |
| repair | the anvil reads the material | nothing to repair |
| smithing | `smithing.yml` names diamond_sword -> netherite_sword | not named |
| mint on craft | `craft_result` | none, so no vanilla craft mints it |
| the tier label | there is none; the tooltip has no tier line (golden) | none |
| enchant eligibility | the CLASS (melee) and element (kinetic) | a melee kinetic weapon's |

It loads, mints by `/rpg give`, scores and shows its tooltip (golden: `Kinetic`, blank, `Melee Damage: 18`,
`Attack Speed: 2.0`, `Crit Chance: +10%`, blank, `Common Melee Weapon`).

**K6, DURABILITY. READ: the glass bottle has none.** `Items` registers `glass_bottle` through
`registerItem(String, Function)`, which passes a bare `new Item.Properties()` to `BottleItem::new`, with no
`.durability()`. The control: `IRON_SWORD`'s registration calls `Item.Properties.sword(ToolMaterial.IRON, …)`. That
`Material.getMaxDurability()` then returns 0 is INFERRED; `/rpg durability` reads it on the boot. So it never wears
or breaks (`WeaponDurability.wearOnce` returns at an empty max).

> **UNBREAKING IS STILL OFFERED TO IT. NOT BUILT, AND THIS IS A STOP ITEM FOR THE SEAT.** READ: `EnchantRoll.Requires`
> gates only RELOAD and QUIVER, so every non-accessory gear is offered `unbreaking`, whatever its durability. Eight shipped
> weapons already sit on materials with no durability call and are offered it today: `blaze_kings_staff` (blaze rod),
> `bone_whistle` (bone), `crystal_shard_staff` (amethyst shard), `cursed_emerald` (emerald) and `flint_staff` (stick),
> all READ; `knell_beam` (echo shard), `lapis_staff` (breeze rod) and `living_lantern` (lantern) not read.
> Keeping it off the bottle with no weapon id in Java takes a `requires: durability` gate on `unbreaking.yml`. That gate
> also takes it off those eight. No ruling covers that change to their pools, which is the STOP rule's last case.
> **The bottle's pool therefore offers Unbreaking, as the Flint Staff's does.** OPEN FOR BEN AND THE SEAT: (a) the
> general gate (eight more pools change, an inert offer gone everywhere), or (b) leave it.

**K7, vanilla bottle use BLOCKED on a minted bottle, a plain one untouched.** READ, every class in the pinned server jar
that names `Items.GLASS_BOTTLE`. How each use is blocked:

| what it does | where (READ) | blocked by |
|---|---|---|
| a water source -> water bottle | `BottleItem.use` | `onMintedBottleUse`: `setUseItemInHand(DENY)`; `handleUseItem` reads it before `useItem` |
| a dragon's breath cloud -> dragon's breath | `BottleItem.use` | the same |
| a water cauldron -> water bottle | `CauldronInteractions`, WATER dispatcher | `setUseInteractedBlock(DENY)` on `water_cauldron`; `useItemOn` reads it before `BlockState.useItemOn` |
| a full beehive or bee nest -> honey bottle | `BeehiveBlock.useItemOn`, which fires NO event of its own | the same, on `beehive` and `bee_nest` |
| a dispenser fills it | `DispenseItemBehavior$10` (fires `BlockDispenseEvent`) | `onMintedBottleDispense` cancels |
| a brewing stand's potion slot accepts it | `BrewingStandMenu$PotionSlot.mayPlaceItem` | `onMintedGearBrew` cancels a brew with any minted item in the stand |
| a crafting ingredient (`honey_bottle.json`) | `data/minecraft/recipe` | already: `onPrepareCraft` and `onCrafterCraft` (`CraftMatrixScreen`) |
| a cleric buys 9 | `villager_trade/cleric/4/glass_bottle_emerald.json` | not reachable: a minted item stacks to 1 (INFERRED; not built) |

Furnaces: no smelting recipe takes a glass bottle, and no fuel class names it. Both READ: the jar's recipe folder holds
`glass_bottle.json` (the bottle as OUTPUT) and `honey_bottle.json`, and that is all. **No entity takes a glass bottle**:
no entity class names it, and the dragon's breath cloud goes through the item's own use. Either hand: a minted bottle in
the off hand would be destroyed the same way.

**K8, LOOT. Nothing built.** `broken_bottle` is in NO source and NO dev source (`BrokenBottleContentTest`, with a
control). **No invariant requires a weapon to have a source or a recipe** (READ: `RpgPlugin` warns only when NO recipe
registers; `LootLoader` refuses a pooled id that does not ship, the opposite direction). So no exemption was needed.

## K9 TO K11 — THE ENCHANT (PR 2)

**K9, an enchant effect that adds to crit chance. READ: none existed.** The effects were DURABILITY, DAMAGE, BLOCK_DR,
REFLECT, DEFENSE, MAX_HEALTH, MAX_MANA, QUIVER_SIZE, RELOAD_TIME and the eight behaviours, and none named crit. **Built:
ONE effect type, `crit_chance`**, in the manner of Mana Bank:
- core `CritChanceEnchant` (`boosts`, `contribution` = percent / 100, FLAT);
- `EnchantEffect.CRIT_CHANCE`: a stat with a curve, no behaviour block, gate MAIN_HAND_ONLY (a fighting class or
  universal), in every exhaustive switch;
- paper `CritChanceEnchantItems`: the main hand's EFFECTIVE `crit_chance` enchants (`EnchantValues.totalFor`) as ONE
  source, `critenchant:MAIN_HAND`, merged into the same gear argument as K2's `weapon:MAIN_HAND`.

The keys differ, so the two ADD: **15% base + 10% + 15% = 40% at level III, 30% at level I**. That is EXECUTED through
`StatsSheetLines.critChance` in `CritChanceEnchantTest`, not derived. Active exactly while it is active on the weapon in
hand: a locked or inactive candidate is not effective, and the loop re-reads the main hand every pass. The line (B5)
carries the real number: `+5% Crit Chance`, `+10% Crit Chance`, `+15% Crit Chance` (wording a SEAT PLACEHOLDER).

**K10, guaranteed, as G1 to G5.** `sharpened_glass.yml`: `weapon: broken_bottle`, `guaranteed: true`, `class: universal`
(the convention of every weapon-specific enchant), `value_by_level: [5, 10, 15]`. It is offered first in slot 0 of the
bottle only, locked at 0, bought for XP like any candidate, not pre-applied (`SharpenedGlassContentTest`, 500 rolls each
of the bottle and an iron sword).

| pool | the Broken Bottle | an iron sword (the control) |
|---|---|---|
| slot 0, weapon | `sharpened_glass` | none |
| slot 1, unique | `kinetic_tremors` (READ: the only `element: kinetic` enchant; `class: universal`) | `kinetic_tremors` |
| normal | `sharpness`, `unbreaking` (Unbreaking: see K6, OPEN) | `sharpness`, `unbreaking` |

**K11, where the sheet shows it. READ:** `/rpg stats` (`StatsSheet`), the line `line(null, CRIT_CHANCE_LABEL,
StatsSheetLines.critChance(v.critChance()), CRIT_COLOR)`: a two-space indent, the label padded to 13, then a whole
percent. The Nexus stats head shows the same lines. Quoted:

| crit chance | the line |
|---|---|
| 15 (base, an iron sword in hand) | `  Crit Chance  15%` |
| 25 (the bottle) | `  Crit Chance  25%` |
| 30 (the bottle, Sharpened Glass I) | `  Crit Chance  30%` |
| 40 (the bottle, Sharpened Glass III) | `  Crit Chance  40%` |

Those figures assume nothing else grants crit. Accessories and fragments do (`keen_charm` +5%; the level gives +0 crit
and +0 weapon damage at level 1, EXECUTED in `CritChanceEnchantTest.levelOneAddsNoCritAndNoWeaponDamage`). No selftest verb clears accessories or fragments, so the gate reads its
own baseline and predicts each row as that baseline plus an exact offset.

## THE GATE (PR 3)

`docs/gates/GATE-broken-bottle.md` holds the rows, R0 and SAFETY. What the PR adds to the selftest code:

- **Named rows of other gates, by reference.** `SelfTestGateList` reads `<gate>.<ROW>[.<ROW>]` as an item of the `+` list:
  that gate's setup, then only the rows named, in the gate's order. Ben types
  **`/rpg selftest broken-bottle+summoner.SM9R.SM16R2+armor-sets.AS15R`**.
  - Refused by name: an unknown or empty row, `setup`, a row twice, a gate twice, a chained row.
  - A dot, because the argument is Brigadier's `word()` (READ: no colon or comma).
  - **It revises the seat's G5 of 2026-11-24 ("no gate:row syntax")**, as this brief asks.
- **P-R1** is in `verification.md`'s AUTO-reading account.
- **P-R2.** READ first: the row end already waited 2 ticks and drained once before `DONE`. #308's *"nothing prevents it"*
  overstated the gap. Built: one more tick and a second drain (`SelfTestRowEndDrainTest`).
- **One driver line.** `USE result useItemInHand=<r> useInteractedBlock=<r>` follows the `use:` driver's dispatch. It
  is the server's verdict, K7's AUTO witness.

## CARRIED (not built in this slice)

- **WORLD LOOT, IN BEN'S WORDS (K8).** Ben: *"We will do loot from the world in a separate pass. Just have CC note it."*
  The Broken Bottle is *"Only lootable from Shipwrecks and Sunken Treasure"*. Sunken Treasure means BOTH buried
  treasure chests AND underwater ruin chests (Ben: *"Both actually"*). The chances: *"Leave it for now"*. **Until that
  pass it is obtainable only by `/rpg give`.**
- **AND THE BLAST FUNGUS, FOR THE SAME PASS (2026-11-26, `PLAN-blast-fungus.md` F12).** Ben: *"Loot only, bastion chests,
  and pigging barters."* (piglin bartering). No chances given. Built nothing; in no source; obtainable only by
  `/rpg give` until the world-loot pass.
- **A custom look** for the item (the brief: not in this slice). It renders as a plain glass bottle.
- **K6's Unbreaking question**, above: **RULED (b), "leave it" (the seat, 2026-11-26).** Unbreaking stays in the bottle's
  pool. It is carried beside "Unbreaking rolls on weapons with no durability" in `PLAN-weapon-tiers.md`'s carried list,
  with the READ list of the five undamageable weapons and the three not read. OPEN FOR BEN.

## THE FOLLOW-UP ON MASTER (2026-11-26, `feat/bottle-owed`, no boot)

The seat's merge approval named six items, after the boot of `37292713`:

| item | what was done |
|---|---|
| **BB2R** | READ: PacketEvents 2.13.0 decodes a translation's numeric argument with `getAsInt()`, so `attribute … get` loses its decimals in OUR reader, not on the server. A new verb, `probe_attribute`, reads `AttributeInstance.getValue()` and `getBaseValue()` on the server, to three decimals. BB2R predicts `value=1.600` and `value=2.000` |
| **AS15R2** | the craft at level 3, the recipe screen's own, with his armour cleared. Traced step by step in `GATE-armor-sets.md`; its level and its score EXECUTED (`As15r2StagingTest`) |
| **LEVEL_GATED_MENU** | a lint rule: a command opening a level-gated screen runs at a staged level at least the screen's (read from `NexusScreens.Screen.unlockLevel`). It flags 7 findings over the 50 gates, pinned; no old row edited |
| **the leftover summon** | the row-start CLEAN removes his summons (cause clear) and logs `summons=<n>` on every unchained row |
| **SM9R's reply** | READ and recorded as expected, in `verification.md` |
| **the gate-document check** | `GateDocumentSectionsTest`: a CARRIED STATE heading and a WHAT BEN TYPES / RUN SHEET heading, a planted control, 15 of the 50 gate documents pinned as lacking one; none edited |
| **K6 carried; the read-back** | K6 above. Every saved-block restore reads its blocks back: `blocksBack=<matched>/<restored>` on the CLEAN and RESTORED lines |

## CC'S CALLS

| call | chosen | the alternative |
|---|---|---|
| the key's name and unit | `crit_chance: 0.10`, a probability, as accessories write it | `crit_chance_percent: 10` |
| where the source merges | into the reconcile's FIRST (gear) argument, as the armor set does | another trailing argument (it reddens `LevelWiringSignatureTest`, which needs the level's map to close the call) |
| the bound | refuse outside [0, 1] | clamp (the stat clamps at read anyway) |
| the tooltip line | `Crit Chance: +10%` under `Attack Speed`, grey and red like the stat block (wording a placeholder) | the accessory form, `+10% Crit Chance`, green |
| the blocks denied | only `water_cauldron`, `beehive` and `bee_nest`, so a chest still opens with the bottle in hand | deny every block's interaction |
| the brew | cancel any brew with ANY minted item in the stand | stop the bottle being placed in a potion slot (an inventory-click guard) |

## PLACEHOLDERS — OPEN FOR BEN (SEAT PLACEHOLDER 2026-11-25)

- reach 3.0 and arc 120 (the swords');
- flavor: none;
- the tooltip line's wording `Crit Chance: +10%`;
- the durability question (K6) and whether Unbreaking stays in its pool.
