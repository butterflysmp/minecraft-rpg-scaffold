# PLAN — ACCESSORIES AND THE EQUIPMENT SUBMENU

**Plan only. No Java, no content.** Read against master `ff339d1` (#150 on top of `2966a0b`, docs only).
Every `file:line` below was read in that tree. Short paths: `P/` = `paper/src/main/java/io/github/butterflysmp/rpg/paper/`,
`C/` = `core/src/main/java/io/github/butterflysmp/rpg/core/`, `S/` = `storage/src/main/java/io/github/butterflysmp/rpg/storage/`.

**Line budget: predicted ~650 lines, stated before writing.** The actual count is in the PR body, measured
with `wc -l` (not PowerShell's `Measure-Object -Line`, which skips blank lines — it reads a 606-line
plan as 456).

**The brief** (`BRIEF-accessories-plan.md`) is **not in this repo**. It was not at the repo root; the
copy read is `Downloads\BRIEF-accessories-plan.md`, byte-identical (SHA-256 `8DCC0AF1…ECA4E`) to the one
in `Downloads\Claude outputs\`. It is deliberately not committed.

---

## 0. BEN'S RULINGS. DECIDED; NOT RE-DERIVED HERE

| # | ruling |
|---|---|
| A1 | The class slot is gated on the **PROFILE** class (`PlayerProfile.archetypeId`, set by `/rpg class`), not the held weapon. |
| A2 | The Ranger accessory is called **"Quiver"**, and the magazine keeps its name. **Internal identifiers must not collide**: own PDC key, own content id, nothing reuses any `quiver_*` key, source prefix or notice id. |
| A3 | v1 acquisition is **admin give only** (`/rpg give`). |
| A4 | v1 accessories take **no part in enchanting, the anvil, the grindstone or gear score**. Each refuses them **explicitly**; none is silently absent from an exhaustive switch. |

**And four amendments Ben made when approving this plan's outline, 2026-09-24:**

1. **`class_damage` is not excluded.** A1 decides who may *wear* a class accessory;
   `ClassDamageModifiers.matching` decides *when* the stat applies (the held weapon's class). They do not
   conflict: a Gauntlet granting +Melee Damage boosts melee hits only. Conditional on §7 Q3.
2. **`GearClass.ACCESSORY` becomes a token an enchant yml's `class:` can name**, so `EnchantLoader` must
   refuse `class: accessory` explicitly (A4). It is in §3.4's refusal list.
3. **The duplicate-kit-weapons defect is recorded as a pre-existing finding** (§1.1, §8). Not fixed here.
4. **§7 Q6's reason is the eligibility half of NAME THE QUANTITY**, not the wording clash, which A2
   already accepted.

---

## 1. THE SEAT'S NINE FINDINGS, CONFIRMED OR CORRECTED

### 1.1 Player class — PARTIAL (the substance holds; three details corrected)

- **Free string, CONFIRMED.** `S/PlayerProfile.java:19` `String archetypeId,`; `:333-334` records that it
  now carries the class id and the field name is kept for schema stability.
- **No reader in combat or stats, CONFIRMED.** Production readers are exactly two:
  `P/command/RpgCommand.java:2033` (picks which "Locked" line `/rpg cast` prints) and `:2128` (carries the
  class forward when the element changes). Nothing in `core/`, no health, stat or combat code reads it.
  Ability gating reads `unlockedAbilities` (`RpgCommand.java:1975`).
- **`chooseClass` accepts only `kits.classes()`, CONFIRMED.** `RpgCommand.java:2103`
  `if (!kits.classes().contains(classId)) {`; `kits.classes()` is the kit keys (`C/kit/KitRegistry.java:42-46`).
  Shipped kits: `content/kits/mage_fire.yml:12` `class: mage`, `ranger_fire.yml:15` `class: ranger`.
  **`/rpg class melee` is refused today**, `RpgCommand.java:2104-2107` (`"Unknown class: "`, `return 0`).
- **Fresh profile is `"none"`, CONFIRMED.** `PlayerProfile.java:99` `NONE = "none"`, placed in the class
  position by `fresh()` at `:253`.
- **CORRECTED — "lowercase" is a convention, not a property.** Nothing lowercases a class id: the argument
  is read raw (`RpgCommand.java:195,201`), the kit loader reads raw (`P/content/KitLoader.java:57,66-70`),
  and matching is case-sensitive. `/rpg class Ranger` is refused. The ids are lowercase only because the
  YAML is. **Consequence for §3.6: the class-slot gate compares against the kit token exactly as
  `chooseClass` does, or the two disagree about what "ranger" means.**
- **CORRECTED — `"none"` is not the only non-class value.** The compact constructor defaults a null
  `elementId` to `NONE` but **not a null `archetypeId`** (`PlayerProfile.java:107-109`). A profile file
  missing the key loads with a null class. §2 treats null exactly as `"none"`.
- **NEW, PRE-EXISTING DEFECT (amendment 3): re-choosing a class mints duplicate kit weapons.**
  `grantWeapons` (`RpgCommand.java:2179-2209`) only ever adds (`:2195` `setItem`, `:2198` `addItem`), and
  nothing in `chooseClass` (`:2096-2110`) refuses re-choosing the current class. `/rpg class ranger` run
  twice yields two bows, each rolled and stamped (`:2187`, `:2191`). **Recorded, not fixed** — §8.

**Under A1 the seat's conclusion stands: nobody can wear Gauntlets today.** §2 decides what to do.

### 1.2 All gear stats come from the reconcile loop — CONFIRMED, with three corrections

- **Every 5 ticks, CONFIRMED.** `P/health/PlayerHealthSystem.java:42` `RECONCILE_PERIOD_TICKS = 5`, started
  at `:173`, from `onJoin` (`:139`) and `onRespawn` (`:160`).
- **Nothing listens for equip events, CONFIRMED.** No handler for `PlayerArmorChangeEvent` or
  `EntityEquipmentChangedEvent` exists in `paper/src/main`. The design reason is
  `C/combat/stat/ModifierReconciler.java:8-15` (converge by diffing; a missed event leaks).
- **Why two scanners on one stat wipe each other, CONFIRMED.** `ModifierReconciler.java:43-46` clears every
  applied source absent from the desired map. The warnings: `PlayerHealthSystem.java:194-198` (max HP),
  `:288-294` (quiver size), `P/health/QuiverSizeModifierItems.java:61-64`, and
  `P/health/GrowthModifierItems.java:30-37` on why keys need a namespace (`putModifier` is put-or-replace).
- **The existing merges.** Max HP inline at `PlayerHealthSystem.java:203-205`
  (`HealthModifierItems` ∪ `GrowthModifierItems`, one `reconcileMaxModifiers`); quiver size via
  `ExpandedQuiverModifierItems.mergedSources` (`:309-311`); max mana and mana regen as a *pair* through
  `ManaTransition.reconcile` (`:256-258`), never short-circuited (`C/combat/ManaTransition.java:66-67`).
- **CORRECTED — there are no `armor:head`-style keys.** Keys are either a bare `EquipmentSlot.name()`
  (`"CHEST"`) or `"<prefix>:"+SLOT`. §3.5 lists every prefix.
- **CORRECTED — four comments misdescribe the quiver-size keys as bare.** The code prefixes them
  `"quiversize:"` (`QuiverSizeModifierItems.java:80`, `:135`); the comments at `:66`,
  `PlayerHealthSystem.java:296-298` and `ExpandedQuiverModifierItems.java:142` say bare. Stale prose,
  harmless today, recorded in §8. **It matters to this slice only as a warning: the comments are not a
  reliable map of the prefixes, so §3.5's disjointness is a TEST, not a reading of comments.**
- **CORRECTED — not quite "all".** Shield block DR, Bulwark and Reflect are read **per hit**, outside the
  loop (`P/weapon/ShieldBlock.java:126-170`). Movement speed is a status modifier only
  (`P/adapter/EntitySpeedAttribute.java:30-42`). Neither is an accessory candidate.

**The seat's prescription stands:** accessories are in no equipment slot, so they need their own scanner
reading the in-memory store; namespaced keys; merged into each shared stat's map before its one reconcile.

### 1.3 Negative modifiers work; max HP has no floor — CONFIRMED, and it generalises

- `C/combat/stat/HealthState.java:252-254` `max()` returns `max.value()`; `C/combat/stat/Stat.java:36-40`
  is `base + Σ modifiers` with no floor; `Stat.java:59-61` `putModifier` has no sign check.
- The only clamps on HP are on **current** (`HealthState.java:982`, `:989`, `:992-995`). The heart bar's
  `MIN_MAX_HEALTH_POINTS = 2` (`P/health/HeartBarRenderer.java:47-48`, applied `:66`) is display only.
- **Max mana has no floor either.** Base `MAX_MANA = 100.0` (`P/RpgPlugin.java:110`) plus
  `maxManaBonus` (base 0, `HealthState.java:99`); the pool clamps only the *current* amount
  (`C/combat/ResourcePool.java:188`).
- **And several stats clamp where they are READ, which makes a drawback INERT at the clamp** — the
  NAME-THE-QUANTITY rule, arriving through a negative:
  - crit chance: `C/combat/Crit.java:65` clamps to [0,1];
  - defense: `C/combat/Defense.java:54` `if (defense <= 0) return damage;` — negative defense never
    amplifies damage;
  - attack speed: `C/ability/AttackSpeed.java:48-49`, floor `MIN_SPEED = 0.1` (`:28`).

  A `-5 defense` drawback on an unarmoured player reads *"-5 Defense"* and does nothing. §3.3 turns this
  into a loader rule.

### 1.4 Identity is a PDC string per gear kind — CONFIRMED, and the switch list is longer

- Keys: `P/adapter/Keys.java:361-364` (`weapon_id`, `shield_id`, `armor_id`, `tool_id`; fields `:16,27,39,55`).
  `:50-53` explains four keys rather than one id plus a kind byte.
- **Sealed:** `C/weapon/GearDefinition.java:61-62`
  `permits WeaponDefinition, ShieldDefinition, ArmorDefinition, ToolDefinition`.
- **Exhaustive switches over `GearDefinition`:** `P/weapon/GearItems.java:131-136` (`remint`), `:167-172`
  (`refreshLore`), `:191-196` (`mint`), `:213-218` (`gearClassOf`); `P/menu/SuggestionTiers.java:40-48`.
- **Exhaustive switches over `GearClass`** (`C/weapon/GearClass.java:29-81`: MELEE, RANGER, MAGE, SHIELD,
  ARMOR, TOOL): `C/weapon/GearScore.java:235-240` (`scoreable`), `C/anvil/TransferKey.java:122-131`,
  `P/weapon/GearClassLabel.java:27`, `:51`, `:72`.
- **Hand-written id-key chains (no compiler help), nine:** §3.4 lists each with its arm.
- **`isGear` CONFIRMED** at `P/menu/CraftMatrixScreen.java:117-123`, with its own warning at `:105-111`
  (*"A KEY MISSING FROM THIS CHAIN IS A HOLE … and it is silent"*). **`QuiverAmmo` depends on it**:
  `P/weapon/QuiverAmmo.java:82` `AMMO = Material.ARROW`, `:193` `if (CraftMatrixScreen.isGear(item, keys)) continue;`.
- **EXTENDED:** `QuiverAmmo.consume` (`:155-170`) re-checks only the material (`:160`), not `isGear` — the
  gear check happens at planning time only. Not a defect today; it is why §3.7 bans `ARROW` outright
  rather than relying on `isGear`.

### 1.5 `SuggestionTier.ACCESSORY` exists — CONFIRMED IN SUBSTANCE, misquoted

`C/weapon/SuggestionTier.java:47-55`: *"A shield today, and whatever an accessory gear kind turns out to be
… When an accessory kind lands it joins here rather than forcing a seventh position, and
`SuggestionTiers`' exhaustive switch is what makes that a decision someone has to take."* The brief's
phrase *"a future accessory kind joins it"* is not verbatim; the nearest is `SuggestionTiers.java:42-45`
(*"so an accessory kind joins it later rather than forcing a new position"*). Order:
`WEAPON, ACCESSORY, TOOL, ARMOR, MATERIAL, VANILLA`.

### 1.6 The menu framework cancels first and performs moves itself — CONFIRMED

- `P/menu/Menu.java:210-214`: `event.setCancelled(true);` is the first statement of `handleClick`.
- `P/menu/MenuRouting.java:100` number key → `hotbarMove`; `:101` F → `offhandMove`; `:116` double-click →
  `collectToCursor`; `:146-148` shift-click → `shiftMove`. Plain moves inside the player's own half are
  **un-cancelled** (`:152-155`, `OWN_INVENTORY_ACTIONS`).
- **So while any of our menus is open, vanilla shift-to-equip does not run**: a shift-click on armour in
  the bottom half is `MOVE_TO_OTHER_INVENTORY`, routed to `shiftMove`, and the plugin decides. §4.3 is
  where the Equipment screen does vanilla's job.

### 1.7 The Vault's storage shape — PARTIAL (shape confirmed; two behaviours corrected)

- **CONFIRMED:** base64 `serializeAsBytes()` (`P/vault/VaultCodec.java:64`, decode `:84`); one entry per
  slot (`S/VaultEntry.java:37`, sparse `S/PlayerVault.java:49`); own repository and file
  (`S/VaultRepository.java:22`, `S/FileVaultRepository.java:38`, `<uuid>.json` `:55`, temp-then-atomic-move
  `:123-127`, wired at `P/RpgPlugin.java:527-529` on the profile's I/O executor); own schema and migrations
  (`PlayerVault.java:60` `CURRENT_SCHEMA_VERSION = 1`, `S/VaultMigrations.java:31-63`).
- **CONFIRMED — the profile is expensive to extend.** 12 fields (`PlayerProfile.java:16-71`), schema
  `CURRENT_SCHEMA_VERSION = 4` (`:80`), seven `withX` methods each rebuilding all 12
  (`:264-340`), migrations `S/ProfileMigrations.java:20-162`.
- **CORRECTED — "write-through on every change" holds at the service, not the screen.** Every mutation
  rewrites the whole vault (`P/vault/VaultService.java:252-254`), but screen writes are **deferred one tick
  and coalesced** (`P/menu/NexusVaultMenu.java:506-511`), and quitting does not save
  (`VaultService.java:134-137`).
- **CORRECTED — a corrupt SLOT costs one slot, a corrupt FILE costs everything.** An undecodable item is
  kept opaque and shown as a barrier (`NexusVaultMenu.java:1007-1013`, written back `:560-565`): one
  *usable* slot lost, no data lost. **Structural** damage — duplicate `(page, slot)`
  (`PlayerVault.java:77-84`), out-of-range entry (`VaultEntry.java:40-46`), bad JSON
  (`FileVaultRepository.java:72-73`) — refuses the **whole vault** for the session (`:86-88`, `:112-117`).
- **Write failure poisons the cache** (`VaultService.java:254-275`, `poison` `:344-348`): later writes are
  refused, the menu degrades (`NexusVaultMenu.java:589-599`).

### 1.8 The Nexus has no Equipment button — CONFIRMED; the row is CORRECTED

- `PAINTED_SLOTS` at `P/menu/NexusMenuLayout.java:208-210`: close 49, settings 50, stats 13, vault 29,
  anvil 30, crafting 31, enchant 32, grindstone 33. `FILLER_SLOTS` is the complement (`:230-235`).
- **CORRECTED — row 5 is the wrong row.** The band table (`:56-60`) assigns by **kind**: row 3 (18-26) is
  *"other features"*, row 4 (27-35) *"crafting-type menus"*, row 5 (36-44) *"unassigned"*; `:63-65` says
  *"its KIND picks its band"*. An Equipment screen is a feature, not a crafting-type menu, so it belongs
  in **row 3**, which is also empty. Ben confirmed this correction.
- Level gating lives in `P/menu/NexusStationGate.java` (crafting 3 `:93`, anvil 7 `:111`, enchanting 10
  `:113`, grindstone 13 `:114`, vault 20 `:145`), checked once in front of every branch at
  `P/menu/NexusMenu.java:216-222`. Settings, Close and Stats are ungated.

### 1.9 Nothing plans accessories, an equipment screen or gear loadouts — CONFIRMED

A case-insensitive search of every root `.md` (there is no `docs/`): *equipment screen*, *equipment set*,
*gauntlet* — no hits; *scroll* only as a verb (`NEXT.md:5047`). `DESIGN-build-system.md`'s "loadout" is an
**ability** loadout (`:9`, `:39-40`, `:59-60`, `:155`). "Accessory" appears only as the display tier
(`NEXT.md:3900-3903`) and a placeholder (`C/anvil/TransferKey.java:27-29`: *"ACCESSORIES DO NOT EXIST
YET … There is no `ACCESSORY` in `GearClass`"*). §6 records the collision.

---

## 2. SLICE 0 — MELEE AS A CHOOSABLE CLASS

| option | what it costs | what it commits |
|---|---|---|
| **(a)** a `melee_fire` kit | content only: one kit yml naming `emberblade` (a shipped melee weapon) and some ability list | **a melee ability design.** `ranger_fire.yml`'s own header warns the next step is a cell that can be *played*, "never 'start filling the grid'". A placeholder kit becomes a precedent, which is the dev-weapon loop CLAUDE.md records |
| **(b)** decouple "choosable" from "has a kit" | Java: `chooseClass` (`RpgCommand.java:2103`) and `applyKit` (`:2138-2171`) both assume a kit; a kit-less class needs a second path and its own abilities answer (none? keep the old?) | a class with no abilities, which `/rpg cast` then has to explain |
| **(c)** ship Gauntlets unwearable | zero. The loader accepts `class: melee`; the class slot refuses it for everyone today | a Gauntlet can be minted and seen but never worn until (a) or (b) lands |

**RECOMMENDATION: (c) for v1, and (a) as its own slice when Ben designs melee.** Every class-slot
mechanic — gate, inert-on-change, the locked look — is fully gateable with `ranger` and `mage`, which ship.
(a) is the right eventual answer but it is a *design* decision about melee abilities, not a plumbing step
for accessories.

**What a `"none"` player's class slot does — LOCKED.** It renders locked, accepts nothing, and the scanner
contributes nothing from it. A null `archetypeId` (§1.1) is treated identically.

**What happens to a worn class accessory when the class changes — IT STAYS AND GOES INERT.**
- Nothing moves, so `keepInventory` and a full inventory never arise. (For the record: the plugin forces
  keep-inventory per death at `P/listener/RpgListeners.java:1521`, and `MenuSafety.give` drops at the
  player's feet when full, `P/menu/MenuSafety.java:33-44` — neither is needed by this answer.)
- The scanner's class gate (§3.6) already produces it: the next reconcile tick removes the sources.
- The Equipment screen and the item's lore mark it **inactive** and say why ("Requires class: Ranger").
- Returning to the class reactivates it with no action.
- Rejected: *return it to the inventory* (needs a space, a drop rule and a dupe analysis for a benefit
  nobody asked for); *refuse class changes while worn* (makes `/rpg class` depend on a store it does not
  otherwise read, and fails confusingly if the store is unavailable).

---

## 3. SLICE A — THE ACCESSORY KIND, STORAGE AND STATS. NO MENU

### 3.1 Content schema — `content/accessories/<id>.yml`, one file per accessory, id = filename

```yaml
display_name: "<gold>Hunter's Quiver</gold>"
rarity: rare                     # the existing rarity enum
material: shulker_shell          # §3.7 — from an allowlist, not free
flavor: "..."
slot: class                      # universal | class
class: ranger                    # class items only: melee | ranger | mage (WeaponClass tokens)
type: quiver                     # class items only: gauntlet | quiver | scroll
modifiers:
  crit_damage: 0.15
  max_mana: -10                  # a drawback — §3.3 decides whether it loads
```

### 3.2 Loader validation (`AccessoryLoader`, the `ShieldLoader` template: sorted, per-file, a bad file logged by name and skipped)

- `slot: class` **requires** `class` and `type`; `slot: universal` **refuses** both.
- `type` must match `class`: melee↔gauntlet, ranger↔quiver, mage↔scroll. One table, one test.
- `class` parses through `WeaponClass` (`C/weapon/WeaponClass.java:19-22`), case-insensitively on load,
  and is stored as the lowercase kit token — **the same spelling `chooseClass` compares** (§1.1).
- Every key under `modifiers` must be in the §3.5 whitelist; anything else refuses the file.
- **The id must be unique across all five registries, and here that is an ERROR.** Today a cross-kind
  clash only logs a warning (`P/RpgPlugin.java:244-250`, `:270-281`, `:307ff`), and `/rpg give` resolves
  first-match (`RpgCommand.java:1340-1343`). A new kind should not inherit a known soft spot.
- **A2 binds PDC keys, source prefixes and notice ids — not content ids.** A content id such as
  `hunters_quiver` is fine; what A2 demands of it is that it not *collide*, which the cross-kind rule
  above enforces (`quiver_stone` is a weapon id and stays one).

### 3.3 Negatives: refuse where the arithmetic is unguarded or inert

**Max HP: REFUSED in v1** (the brief's "one or the other"). A floor is a change to `HealthState` with its
own death-edge questions (current clamps to max; a max below current kills?), and v1 does not need it.
§7 Q7.

For every other stat, NAME THE QUANTITY first. The player bases, read at `ff339d1`:

| stat | player base | where | negative allowed in v1? |
|---|---|---|---|
| max_health | 100.0 | `CombatantStats.java:26` `DEFAULT_PLAYER_BASE` | **no** — no floor |
| max_mana | 100.0 | `RpgPlugin.java:110` `MAX_MANA` + bonus | yes, **bounded** (below) |
| crit_chance | 0.15 | `Crit.java:44` | yes, bounded — clamps at 0 (`Crit.java:65`) |
| crit_damage | 1.0 | `Crit.java:47` | yes, bounded |
| health_regen | 0.2 /s | `HealthRegen.java:56` | yes, bounded |
| mana_regen | bonus 0 over a base rate in `RpgPlugin` | `HealthState.java:163` | **no** — the composition below 0 was not traced |
| defense | 0.0 | `HealthState.java:83` | **no** — inert when unarmoured (`Defense.java:54`) |
| class_damage | 0.0 | `HealthState.java:49` | **no** — base 0, and below-weapon damage was not traced |

**The bound:** a negative is refused unless `4 × |amount| < base` — four slots each carrying the same
drawback still cannot reach the clamp. That is conservative (it ignores other gear) and mechanical, and
it makes the tooltip honest by construction: a loaded drawback is always a real reduction.

### 3.4 Identity: the new kind, and every arm it forces

**New types.** `C/weapon/AccessoryDefinition` (record: id, displayName, rarity, material, flavor,
`AccessorySlotKind slot`, `WeaponClass accessoryClass` nullable, `AccessoryType type` nullable,
`Map<AccessoryStat, Double> modifiers`), joining the `GearDefinition` permits. `C/weapon/AccessoryRegistry`
(`extends GearRegistry<AccessoryDefinition>`, ~3 lines like `ShieldRegistry`). New PDC key
**`accessory_id`** in `Keys.java` beside the four. `GearClass.ACCESSORY`.

*(For Ben, Java refresher: a **sealed interface** lists every class allowed to implement it, which is what
lets the compiler reject a `switch` that forgets one. A **record** is a class whose fields are fixed at
construction and which gets equals/hashCode for free.)*

**Compiler-forced arms — adding to `permits` and to `GearClass` makes each of these a compile error until
decided:**

| site | arm |
|---|---|
| `GearItems.java:131` `remint` | `AccessoryItems.mint(...)` — lore refresh on a reminted stack |
| `GearItems.java:167` `refreshLore` | `AccessoryLore` |
| `GearItems.java:191` `mint` | `AccessoryItems.mint` |
| `GearItems.java:213` `gearClassOf` | `GearClass.ACCESSORY` |
| `SuggestionTiers.java:40` | `SuggestionTier.ACCESSORY` — the slot reserved for it |
| `GearScore.java:235` `scoreable` | `ACCESSORY -> false` (A4) |
| `TransferKey.java:122` | `ACCESSORY -> throw`, as `TOOL` does; `:27-29`'s "does not exist yet" note is rewritten |
| `GearClassLabel.java:27`, `:51`, `:72` | "Accessory" labels |

**Hand-written chains — nothing fails to compile here, so each is listed and each gets an EXPLICIT arm:**

| site | arm (A4 = an explicit refusal with a message) |
|---|---|
| `CraftMatrixScreen.java:117-123` `isGear` | **add** `accessory_id` — CONTAINS_GEAR, and `QuiverAmmo:193` |
| `EnchantMenu.java:235-283` `resolveGear`, `:290-294` `acceptsInput` | **refuse**, A4: *"Accessories cannot be enchanted."* |
| `AnvilMenu.java:729-747` `resolve` (via `acceptsInput` `:223`) | **refuse**, A4 |
| `GrindstoneMenu.java:436-454` `resolve` (via `acceptsInput` `:138`) | **refuse**, A4 |
| `RpgCommand.java:1867-1921` `resolveHeldGear` (`/rpg enchant`, `/rpg gearscore`) | **refuse**, A4 |
| `GearScoreItems.java:287-295` `candidateScore` | **explicit skip** beside the tool skip (`:271-280`), A4 |
| `GearRefresher.java:66-69` | **include** — refresh accessory lore in the inventory |
| `RpgCommand.java:1477-1478` `/rpg durability` | already refuses non-weapon/shield; add the accessory to its message |
| `RpgCommand.java:1340-1343` `/rpg give` | **add** `accessories.find(id)`; suggestions at `:240-247` |
| **`EnchantRollItems.rollOnAcquire`** (`P/weapon/EnchantRollItems.java:73-75`, called `RpgCommand.java:1375`) | **refuse** — found writing this plan: a universal-gated enchant would otherwise roll onto an accessory at give time, A4 |
| **`GearScoreItems.stampOnAcquire`** (`:161`, called `RpgCommand.java:1382`) | **refuse** — no `gear_score` key on an accessory, A4 |
| **`EnchantLoader.gearClass`** (`P/content/EnchantLoader.java:115-128`, via `GearClass.fromName` `C/weapon/GearClass.java:92-98`) | **refuse `class: accessory`** (amendment 2), A4 |

> **THE LAST ARM IS THE ONE MOST LIKELY TO BE UNREACHABLE, AND ITS TEST MUST SAY SO.** Every enchant gate
> in `EnchantDefinition.requireGate` is an ALLOWLIST (`P/content/EnchantDefinition.java:172-260`: each
> gate states what it CAN be), so `class: accessory` would already be refused by whichever gate the
> effect selects — with a gate message, not an A4 one. Deleting the explicit arm therefore leaves the file
> refused and a "file is refused" test green. **The test asserts the A4 message text**, which is the only
> reading the arm's deletion can redden.

**`instanceof` sites checked and neutral** (an accessory takes the false branch, which is correct):
`RpgCommand.java:1821` (is-shield), `GearScoreItems.java:310` and `AnvilMenu.java:710` (unscored weapon),
`AnvilMenu.java:720`, `RecipeProbe.java:197`, `:325`, `RecipeCatalogue.java:226` (armour slot). Accessories
do **not** join `allGear` (`RpgPlugin.java:349-354`, the crafting index) — no recipes in v1 (A3).

**A2 in identifiers.** New names: PDC `accessory_id`; source prefix `accessory:`; lore and notice ids
under `accessory.*`. **No internal identifier contains `quiver`.** The Ranger `type` token is `quiver`
because it is the player-facing word Ben ruled; it is a content enum value, never a key or prefix, and a
test asserts no `Keys` field built in this slice contains `quiver`.

### 3.5 Stats: the whitelist, and the scanner each one merges with

**Stats live in the DEFINITION, never on the item's PDC.** The all-slot scanners (#1, #4, #7, #8, #10, #11
below) read their keys off *any* equipped stack, main hand included — an accessory carrying those keys
would grant its stats while merely **held**. The accessory scanner reads the store, resolves each
`accessory_id` through the registry, and emits.

| stat | merges with (scanner, file:line of its `desired…`) | merge point |
|---|---|---|
| max_health (positive only) | #1 `HealthModifierItems.java:54` + #2 `GrowthModifierItems.java:59` | `PlayerHealthSystem.java:203-205`, before `reconcileMaxModifiers` |
| max_mana | #9 `ManaBankModifierItems.java:52` | the first map into `ManaTransition.reconcile` (`:256-258`) |
| mana_regen (positive only) | #10 `ManaRegenModifierItems.java:81` | the second map into the same call — **both maps, one call**, never split |
| crit_chance | #7 `CritModifierItems.java:66` | `:232` |
| crit_damage | #8 `CritModifierItems.java:71` | `:233` |
| health_regen | #11 `HealthRegenModifierItems.java:86` | `:269` |
| defense (positive only) | #15 `DefenseModifierItems.scan` `:110` | `:325`, into `worn.defense()`'s map, **before** `ArmorBarOverride.apply` (`:326`) so the bar shows it |
| **class_damage — CONDITIONAL on §7 Q3** (amendment 1) | #5 `ClassDamageModifierItems.java:81` | into `equippedGrants` (`:90-100`) **before** `ClassDamageModifiers.matching` (`C/weapon/ClassDamageModifiers.java:52-62`), so the held-weapon-class gate applies to accessory grants exactly as to worn ones. Class items only; the grant's class is the accessory's class |

**Excluded, each for a stated reason:**

- **attack_damage — base 0 for players BY DESIGN.** `C/combat/stat/CombatantStats.java:44-46`: *"Players
  base attack at 0 -- weapon-only melee … an unarmed player deals nothing."* A flat accessory bonus would
  make an unarmed punch deal damage. `class_damage` is the route that respects this, which is part of why
  amendment 1 matters.
- **attack_speed** — the 4-tick input grid (`WeaponLoader`'s `cooldown_ticks` section) swallows anything
  under +28% on a 16-tick weapon; a small accessory bonus is inert. Arithmetic half of NAME THE QUANTITY.
- **enchant_damage_percent** — enchant-owned (#6), and accessories are out of enchanting (A4).
- **quiver_size, reload_time** — §7 Q6: the eligibility half fails.

**Source keys:** `accessory:<slotIndex>`, slot index 0 = class, 1-3 = universal. Existing prefixes that
must stay disjoint: bare `EquipmentSlot` names; `growth:`, `manabank:`, `manaregen:`, `regen:`,
`quiversize:`, `expandedquiver:`, `reloadtime:`; enchant ids (#6). **A unit test builds every scanner's
key set from a fixture and asserts disjointness** — per §1.2, the comments are not a reliable map.

### 3.6 The class gate (A1)

The scanner emits the class slot **only while `archetypeId` equals the accessory's class token**, compared
exactly as `chooseClass` compares (§1.1); `null` and `"none"` never match. Pure function in `core/`:
`AccessoryEligibility.contributes(slotKind, accessoryClass, profileClass)`. The profile is read from
`ProfileService`'s cache on the reconcile tick; an unloaded profile contributes nothing from the class slot.

### 3.7 Material: not wearable, not ammo, not food, not placeable, not usable — and not SELLABLE

**Found writing this plan: villagers buy by material.** The 26.1 trade table is data-driven. Measured in
`run/versions/26.1.2/paper-26.1.2.jar`, `data/minecraft/villager_trade/**`: **387** files, **386** with an
id-based `wants` (the other is a wandering trader's water bottle), and the wanted ids include `paper`,
`leather`, `rabbit_hide`, `rabbit_foot`, `string`, `feather`, `flint`, `stick`, `iron_ingot`, `arrow`.
Instrument controlled: the same extraction over `armorer/1/emerald_iron_boots.json` printed
`minecraft:emerald`, the known answer. **`PAPER` for a scroll and `LEATHER`/`RABBIT_HIDE` for a quiver are
therefore out** — a player could sell a minted accessory for emeralds.

Proposed materials:

| use | material | why it is inert |
|---|---|---|
| universal | `ECHO_SHARD` | only use is the recovery-compass recipe (CONTAINS_GEAR refuses it once `isGear` knows the key); not a trim material, not fuel, not brewed, not wanted by any villager |
| Gauntlet | `NETHERITE_SCRAP` | only use is the ingot recipe (CONTAINS_GEAR); the smithing input is the *ingot*; not a trim material; its item entity survives lava, which suits a valuable item |
| Quiver | `SHULKER_SHELL` | only use is the shulker-box recipe (CONTAINS_GEAR); no use action, not wearable, not wanted |
| Scroll | `PRISMARINE_CRYSTALS` | only uses are crafting recipes (CONTAINS_GEAR); no use action, not wanted |

**The villager half is measured; the rest is from vanilla knowledge and NOT jar-verified** — brewing
ingredients, furnace fuel, smithing trim materials, allay duplication, beacon payment. Slice A's gate
carries a row that tries each accessory in a brewing stand, a furnace fuel slot and a smithing table
(§5.2 A9), because a claim of inertness is a false absence until something tries to consume it.
`ARROW` is banned by the loader outright (§1.4's `consume` note). All four natively stack to 64;
`setMaxStackSize(1)` at the mint, per the standing decision.

### 3.8 Minting and lore

`P/weapon/AccessoryItems.mint`: PDC `accessory_id`, `meta.setMaxStackSize(1)` (as
`ShieldItems.java:63`, `ArmorItems.java:90`), name, rarity colour, flavor, then **one lore line per
modifier, drawbacks in red with their sign**, then the slot line (*"Universal accessory"* / *"Ranger class
accessory"*). Lore is rendered from the definition, never stored stats.

### 3.9 Storage — a separate repository, modelled on the Vault

**RECOMMENDATION: separate repository.** `S/AccessoryRepository` (port), `S/FileAccessoryRepository`
(`accessories/<uuid>.json`, temp-then-atomic-move as `FileVaultRepository.java:123-127`),
`S/PlayerAccessories` (record: schemaVersion, playerId, `List<AccessoryEntry>` — slot 0-3 + base64),
`S/AccessoryMigrations` (v1 stamp, newer-version refusal as `VaultMigrations.java:32-37`). Wired beside
the vault on the same storage I/O executor (`RpgPlugin.java:527-529`). A `P/accessory/AccessoryService`
owns the cache, as `VaultService` does.

| | separate repository | profile field |
|---|---|---|
| cost | 4 small storage classes, one service | schema 4→5, a 13th field through **all seven** `withX` rebuilds, a migration step, and every `PlayerProfileMigrationTest` fixture |
| blast radius of a bad accessory write | the accessory file | **the profile** — class, XP, Nexus slot |
| loadouts later (§6) | reuse the same store | a second field or a nested structure in the profile |

- **Load on join:** async, as `VaultService.onJoin` (`VaultService.java:96-115`); missing file = empty.
  Until loaded, the scanner contributes nothing and the menu shows the slots as loading.
- **Save:** write-through on every equip/unequip **at the service**, whole record (4 entries — small).
  No screen-side coalescing is needed: an equip is one gesture, not a drag.
- **A corrupt slot costs one slot:** an undecodable entry is kept opaque, rendered as a barrier, written
  back unchanged — the vault's `NexusVaultMenu.java:1007-1013` behaviour. **A structural fault** (bad
  JSON, duplicate slot, slot out of 0-3) makes the store unavailable for the session, logged SEVERE; the
  scanner contributes nothing and the menu refuses accessory edits. That is the vault's actual behaviour
  (§1.7), stated rather than softened.
- **A failed write poisons the store** (`VaultService.java:254-275` pattern): later writes refused,
  SEVERE log. **The ordering that prevents a duplicate:** unequip writes the store FIRST and hands the item
  to the cursor only when the write completes; equip takes the item off the cursor and returns it if the
  write fails. The residual — the write succeeds on disk but the completion is lost — is the vault's
  DECISION 2 residual (`PLAN-vault-screen.md`) and is accepted on the same terms.

### 3.10 Admin surface

- `/rpg give <accessory_id>` — §3.4.
- **Dev instrument: `/rpg accessory equip <slot 0-3>` (from the main hand) and `unequip <slot>`**, gated
  like the other dev commands. It exists so Slice A can be gated before Slice B exists. **Deletion
  trigger: the Slice B gate file reads PASS on every Equipment-screen row.** It is marked `// DEV:
  delete when GATE-accessories-b.md passes` so the sweep finds it by marker.

---

## 4. SLICE B — THE EQUIPMENT SUBMENU

### 4.1 The Nexus button

Row 3, **slot 22** — directly under the stats head (13), in the band `NexusMenuLayout.java:56-60` gives to
*other features*. Added to `PAINTED_SLOTS`. Icon `ARMOR_STAND`. **Ungated**, like Stats and Settings: the
screen shows armour the player can already wear from level 1 (§7 Q5).

### 4.2 The screen

54 slots, the house layout (back 48, close 49).

- **Four armour slots** (head, chest, legs, feet) in a column — **live views of `player.getEquipment()`**,
  re-rendered after every click; no copy is ever authoritative.
- **Four accessory slots** in a second column: slot 0 (class) framed in the class colour with its own
  label; slots 1-3 (universal) neutral. **A locked class slot** (profile class `none`/null) shows a
  barrier-pane with *"Choose a class to unlock"*; **an inactive one** (§2) shows the item with an
  *"Inactive — requires class: X"* line.
- **The off-hand is EXCLUDED.** It is where shields live, vanilla's F key already covers it, and every slot
  added here is a slot where the menu must reproduce vanilla rules. Nothing Ben asked for needs it.

### 4.3 The armour slots do vanilla's job — every rule the menu must reproduce

1. **Fit comes from the equippable component**, not a material list: the item's equippable slot must be
   that slot, and its `allowed_entities` (if set) must admit a player. Heads and a carved pumpkin are
   head-equippable; an elytra is chest.
2. **Curse of Binding cannot be removed** in survival/adventure (creative may). Nothing in the codebase
   handles binding today (no `BINDING_CURSE` hit in any `.java`) — this is new.
3. **The equip sound** is the component's `equip_sound`, played at the player.
4. **Cursor swap:** cursor item into an occupied slot swaps, if it fits; an item that does not fit is
   refused and stays on the cursor.
5. **Shift-click from the player's inventory** goes to the matching armour slot if empty; otherwise it is
   refused (vanilla does not swap on shift).
6. **One item from a stack:** a stack of 16 heads places one and leaves 15.
7. **The Nexus star is refused.** `NexusLock.touchesTheStar` (`P/nexus/NexusLock.java:310-317`) guards
   only the player-inventory slots, so an armour slot here is a new door the lock does not watch.
8. **Number-key and F** over an armour or accessory slot: refused (the framework performs them only for
   input slots, `MenuRouting.java:461`, `:487`, and these are not input slots).
9. **Double-click collect** never pulls from these slots (not input slots).

The decision — *may item X go into slot Y, given binding and game mode?* — is a pure function in `core/`
(`ArmorPlacement.decide`) fed a small value describing the item, so rules 1, 2, 4, 5 and 6 are unit-tested
without a server.

### 4.4 Accessory slots

Accept only an item with `accessory_id` whose definition's slot kind matches (class item → slot 0 only;
universal → 1-3), **and** for slot 0 the profile class must match (A1) — or the slot is locked. Equip and
unequip go through `AccessoryService` with §3.9's ordering. Duplicates are allowed (two of the same
universal) unless §7 Q2 rules otherwise.

### 4.5 Vanilla inventory, untouched — proved listener by listener

**The slice adds NO listener.** The Equipment screen is a `Menu`, so it is dispatched by the three holder
checks that already exist. Every listener that could see the player's own inventory screen
(`InventoryType.CRAFTING`, no menu of ours open), and why it returns early:

| event | handler | early return |
|---|---|---|
| `InventoryClickEvent` | `onMenuClick` `RpgListeners.java:1074-1079` | top holder is not a `Menu` |
| `InventoryDragEvent` | `onMenuDrag` `:1088-1093` | same |
| `InventoryCloseEvent` | `onMenuClose` `:1099-1104` | same |
| `InventoryClickEvent` (LOWEST) | `onNexusClick` `:1136-1179` | `if (!NexusSlots.refuses(...)) return;` (`:1138`) — **acts only on the star**. Pre-existing, flagged, not changed |
| `InventoryDragEvent` (LOWEST) | `onNexusDrag` `:1187-1192` | star only |
| `PlayerSwapHandItemsEvent` | `onNexusSwapHand` `:1210-1216` | returns unless either hand is the star |
| `PlayerItemHeldEvent` | `onHeldItemChange` `:1464-1471` | no early return, **never cancels** — re-derives quiver cooldown overlays only |
| `PlayerInteractEvent` | `onRightClick` `:787-940` | a non-weapon in hand on air falls through every branch (`WeaponFire.java:124`, `PlumeDraw.java:265`); **right-click-to-equip armour on air stays vanilla** |

No handler exists for `InventoryOpenEvent`, `EntityPickupItemEvent`, `BlockDispenseArmorEvent` or
`PlayerArmorChangeEvent`. All handlers are registered at the single point `RpgPlugin.java:531-538`.
**The proof is only as good as that last sentence**, so Slice B's gate re-reads the handler list from
source (§5.3 B0b).

### 4.6 Duplication and loss

- **`returnedSlots()` returns `Set.of()`.** Both columns are views of state held elsewhere (the player's
  equipment, the accessory store); the default `inputSlots()` would hand back rendered copies — a dupe.
  `returnEverything` (`Menu.java:305-317`) still returns the **cursor**, always.
- **On close:** cursor returned; nothing else to do.
- **Disconnect mid-click:** quit runs `closeInventory()` first (`RpgListeners.java:1474-1480`), so the
  cursor returns while the inventory can still be written. An accessory write in flight completes or
  poisons under §3.9's ordering; the item is never in both places.
- **Death with the menu open:** `onPlayerDeath` closes first (`:1519`) and keep-inventory is forced
  (`:1521`); the cursor returns before anything else.
- **Full inventory:** `MenuSafety.give` drops at the player's feet with a message (`MenuSafety.java:33-44`).
- **Shutdown:** `RpgPlugin.java:760-765` calls `returnEverything` for any open `Menu`.

---

## 5. TESTS AND GATE ROWS

### 5.1 Unit tests — written before the paper wiring

**core/:** `AccessoryEligibilityTest` (class match; `none`; null; wrong case refused, matching
`chooseClass`); `AccessoryNegativesTest` (the 4× bound, per stat, at the boundary both sides);
`AccessorySourceKeysTest` (disjoint from every prefix in §3.5); `ArmorPlacementTest` (the whole grid of
rules 1, 2, 4, 5, 6 × survival/creative); `ClassDamageModifiersTest` gains a case with an accessory-keyed
grant (conditional on Q3).
**storage/:** `PlayerAccessoriesTest` (duplicate slot, out-of-range slot, blank entry refused),
`AccessoryMigrationsTest` (v0 stamp, newer refused), `FileAccessoryRepositoryTest` (round trip, atomic
write).
**paper/:** `AccessoryLoaderTest` (class/universal/type rules; unknown stat; negative max HP; the
bound; `ARROW` refused; cross-kind id clash is an error); `EnchantLoaderTest` gains **`class: accessory`
refused with the A4 message** (§3.4's note); a test that no `Keys` field added here contains `quiver`.

**Mutations owed**, each named with the row it must redden: delete the `isGear` arm; delete the class-gate
check; merge-after-reconcile instead of before (max HP); drop the `rollOnAcquire` refusal; delete the
`class: accessory` arm.

### 5.2 `GATE-accessories-a.md` — Slice A. **Game mode: SURVIVAL, every row** (creative removes consumption)

| row | prediction, written now | risk it witnesses |
|---|---|---|
| **R0a** | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` names the branch tip, not `-dirty`, not `unknown` | the wrong jar |
| **R0b** | the deployed jar's constant pool contains `accessory_id`, and `content/accessories/` lists the roster files (PowerShell probe) | a right commit, wrong symbols |
| A1 | `/rpg give <universal>` yields **one** item; a second give does **not** stack onto it; lore shows every modifier, the drawback red | stack-of-2 state edits |
| A2 | equip a +HP universal while wearing Growth armour: max HP = 100 + armour growth + accessory, **both** sources present after 20 ticks | the scanner-wipe |
| A3 | a Ranger with a Quiver-type accessory in slot 0: its stat applies; `/rpg class mage`: gone within 5 ticks; `/rpg class ranger`: back | A1 gate, inert-on-change |
| A4 | a `none` profile: dev-equip into slot 0 is refused | locked slot |
| A5 | holding (not equipping) an accessory in the main hand changes **no** stat | stats on PDC |
| A6 | enchant table, anvil, grindstone each refuse it **with the A4 message**; `/rpg gearscore` refuses it | A4 explicit |
| A7 | a freshly given accessory has no enchant data and no `gear_score` key | `rollOnAcquire` / `stampOnAcquire` |
| A8 | equip, restart the server, rejoin: still equipped, stats still applied | persistence |
| A9 | each roster material: refused by the brewing stand, the furnace fuel slot and the smithing table **or** not consumed by them; a villager offers no trade for it | inertness (§3.7's unverified half) |
| A10 | hand-corrupt one slot's base64 in `accessories/<uuid>.json`: that slot shows unreadable, the other three still apply | one slot, not all |
| A11 | an arrow-material test item cannot exist — the loader refuses the file (read in the log) | QuiverAmmo |

### 5.3 `GATE-accessories-b.md` — Slice B. **Game mode: SURVIVAL, except where a row says creative**

| row | prediction | risk |
|---|---|---|
| **R0a / R0b** | as above, plus the jar contains the Equipment menu class | wrong jar |
| B0b | `git grep -n '@EventHandler' <sha> -- paper/src/main` lists exactly the handlers in §4.5's table, no more | a listener added since the proof |
| B1 | **no menu open**: shift-click a chestplate in the vanilla inventory equips it; right-click one in air equips it | Ben's hard requirement |
| B2 | the Nexus shows the Equipment button at slot 22 at level 1 | button |
| B3 | the screen's armour column matches what the player wears; take the helmet out → the player's head is empty (F5) | one source of truth |
| B4 | a chestplate on the cursor into the head slot is refused and stays on the cursor | fit |
| B5 | a Curse of Binding helmet cannot be taken out (survival); **creative**: it can | binding |
| B6 | equipping plays the armour's equip sound | sound |
| B7 | shift-click a helmet from the bottom half: goes to the head slot; with a helmet already on, refused | shift routing |
| B8 | a stack of heads: one placed, the rest stay | stack of one |
| B9 | the Nexus star cannot enter an armour or accessory slot | the new door |
| B10 | disconnect with an item on the cursor: rejoin, it is in the inventory, not duplicated | quit path |
| B11 | die (to `/kill`) with the menu open and an item on the cursor: after respawn, one copy | death path |
| B12 | full inventory, close with an item on the cursor: dropped at feet with the message | full inventory |
| B13 | accessory slot 0 rejects a universal; slots 1-3 reject a class item | slot kinds |

---

## 6. SLICE C — LOADOUTS. DEFERRED, NAME ONLY

**Collision:** `DESIGN-build-system.md` uses "loadout" for the **ability** loadout (`:9`, `:39-40`). A gear
feature called "loadouts" would give one word two systems. **Proposed player-facing name: "Equipment
Sets".** The §3.9 store is shaped so a set can reuse it. Nothing else is planned.

---

## 7. OPEN QUESTIONS FOR BEN — each with a recommendation

1. **Slice 0: which option?** → **(c) now** (Gauntlets authored, unwearable), **(a) as its own slice**
   when melee is designed.
2. **First roster?** → **3 universals** (one each for defence, crit, regen), **one Quiver** (ranger) and
   **one Scroll** (mage), **one Gauntlet** authored for the unwearable path. May a player equip two copies
   of one universal? → **yes** in v1; revisit if it dominates.
3. **`class_damage` on class accessories?** (amendment 1) → **include**, merged into
   `ClassDamageModifierItems`' grants before `matching`, class items only.
4. **Do accessory stats show on the stats sheet?** → **yes**, as their own source line, so a drawback is
   visible where the total is. **HUD?** → **nothing in v1.**
5. **Equipment button level?** → **ungated**, like Stats and Settings.
6. **May a Ranger Quiver modify `quiver_size` or `reload_time`?** → **not in v1.** The eligibility half of
   NAME THE QUANTITY: A1 gates on the *profile* class, not the held weapon, so a Ranger holding
   `hunters_bow` — `class: ranger`, no `quiver_size` — would wear a *"+2 Quiver Arrows"* that does
   nothing. It is the fourth row of CLAUDE.md's table, arriving through a new door. It needs a
   held-weapon gate like `matching`'s before it can be honest.
7. **Max HP drawbacks?** → **refused in v1**; a floor is its own change with death-edge questions. And
   **the 4× bound for other negatives** (§3.3) → accept as stated.

---

## 8. FINDINGS THIS PLAN CREATES IN OTHER FILES — recorded, not fixed

1. **Duplicate kit weapons on re-choosing a class** — `RpgCommand.java:2179-2209`, reached from
   `chooseClass` (`:2096-2110`) with no same-class refusal. Pre-existing (amendment 3).
2. **Four stale key comments** — `QuiverSizeModifierItems.java:66`, `PlayerHealthSystem.java:296-298`,
   `ExpandedQuiverModifierItems.java:142` describe bare keys; the code prefixes `quiversize:`.
3. **Cross-kind id clashes only warn** — `RpgPlugin.java:244-250`, `:270-281`, `:307ff`; `/rpg give`
   takes the first match. Accessories make it an error for themselves (§3.2); the other four kinds are
   unchanged.
4. **A null `archetypeId` survives load** — `PlayerProfile.java:107-109` defaults `elementId` but not the
   class. Accessories treat null as `none`; the constructor is unchanged.
