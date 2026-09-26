# PLAN — THE BUILD SYSTEM: THE BUILD SUBMENU, THE ABILITY STONE, AND THE END OF KITS

**Plan only. No Java, no content, no boot.** Read against master **`bccef7d`**
(`bccef7d81411c74b391441313a05e175234ea9cf`, quoted from `git ls-remote origin master`, #154).
Citations name METHODS and SECTIONS, never line numbers. Short paths: `P/` =
`paper/src/main/java/io/github/butterflysmp/rpg/paper/`, `C/` =
`core/src/main/java/io/github/butterflysmp/rpg/core/`, `S/` =
`storage/src/main/java/io/github/butterflysmp/rpg/storage/`, `content/` =
`paper/src/main/resources/content/` (there is no top-level `content/` directory; `RpgPlugin`'s
`saveDefaultContent` copies this tree into `plugins/Rpg/content/`).

**Line budget: predicted ~750 lines, stated before writing.** The actual count is in the PR body,
measured with `wc -l`.

**The brief** (`BRIEF-plan-build-system.md`) is **not in this repo**. It was read from `Downloads\` and is
deliberately not committed.

**Engine quotes** come from the pinned jars, not memory. The server jar is
`run/versions/26.1.2/paper-26.1.2.jar`, whose `META-INF/MANIFEST.MF` reads
`Implementation-Version: 26.1.2-74-e4e17fc` — build 74, the same build as `pom.xml`'s
`paper.version` `26.1.2.build.74-stable`. Read with the JDK's `javap -c -p`. Where a question is about the
**client** (what packets it sends), no jar can answer it, and it is a SPIKE (§3.1.0).

---

## RULINGS — BEN, 2026-09-25. DECIDED; NOT RE-DERIVED HERE

Recorded verbatim from the brief.

1. The DESIGN doc's near-term test was run a while ago. **The whole build system is built now.**
2. **A new Nexus-like item, the Ability Stone.**
   - Left click and right click cast the player's two Actives.
   - Q casts the Ultimate.
   - It can be toggled on and off in Settings, the same way the Nexus star is.
   - Its slot can be chosen, but **only on the hotbar**.
3. **Aspects change abilities by ADDING new behaviour to them**, not just simple stat bonuses.
4. **Fragments are a fixed 4 slots.** Unlike Destiny, aspects do not add fragment slots.
5. **Everything in a pool is available from the start.** Unlock methods come later.
6. **The kit system is removed.** Being given weapons when selecting a class or element is not
   part of the vision. A replacement for weapon acquisition will be scoped later.
7. **The Build screen offers only FIRE for now: only cells that have a pool file.**
8. **Nothing needs to be craftable yet: this is not a live project. After kits go, /rpg give is the
   only weapon source, and that is accepted for now. State it as a known gap, not a defect.**

**Ben's rulings on §5, 2026-09-25, recorded verbatim. Each is removed from §5:**

9. **Q1: aspects MAY also change their target's numbers.** The design is §2.4.1.
10. **Q2: operators' /rpg cast can cast ANY ability, at any time, with NO cost and NO cooldown. It is a
    dev tool; everyone else casts only their loadout.** The design is §2.5.1.
11. **Q3: one of each fragment only; the same fragment cannot be slotted twice.**
12. **Q4: the Ability Stone's material is ECHO_SHARD.** Its collisions are in §2.5.2.
13. **Q5: the default is on, in hotbar slot 7, changeable via Settings (hotbar slots only).**
14. **Q6-Q10: as recommended.** That makes five rulings, 14 to 18, each taking the §5 recommendation it
    answered:
    - **14 (Q6):** right-clicking a hijacked block with the stone held **casts**. Sneaking still opens
      the block.
    - **15 (Q7):** a held left click is **one cast per press**. **REPLACED by ruling 19.** S2 still runs, because it measures the
      swing stream the debounce is built from.
    - **16 (Q8):** the §3.3 Build-screen layout, and the Build button is **ungated**.
    - **17 (Q9):** Ultimates use **shared mana plus a long cooldown** in v1.
    - **18 (Q10):** the names are "Build", "Ability Stone", "Ultimate", "Active", "Aspect" and
      "Fragment".
19. **Ben's ruling 19, 2026-09-25, from the S1/S2 spike: HOLDING an Ability Stone input RE-CASTS
    whenever the cooldown allows, for BOTH left and right click (option b). This replaces ruling 15's
    "one cast per press". The spike showed separate right-clicks as close as 1 tick apart, below the
    held repeat of exactly 4, so no gap rule can tell them apart.**

**And one clarification, given while building slice 1 (2026-09-25):** Q pressed over the stone with a
screen open **refuses and casts nothing**. §2.5 and ST5 stand as written, and the new Q row reads that
way.

**And one given while building slice 2 (2026-09-25):** `/rpg class` and `/rpg element` are **KEPT**,
weapon-free, until slice 3's Build screen deletes them. They write the cell and grant nothing. This
**replaces** §1.2's "DELETE the command" verdicts and §3.2's `/rpg build cell` dev instrument, which was not
built. See §3.2.1.

The loadout, per player:

- 1 Ultimate (a long cooldown);
- 2 Actives (shorter cooldowns);
- 2 Aspects;
- 4 Fragments (slight bonuses).

Every one of these is specific to the player's **class-element** combination.

The UI: a **Build** button in the Nexus hub at **slot 21**, directly left of Equipment (22). The Build
screen is where the player picks their Class, their Element and their loadout.

**And seven amendments the seat made when approving this plan's outline, 2026-09-25:**

1. **Aspect numbers** are not in the OUT list. Whether an aspect may also change an ability's numbers is
   §5 Q1, now ruling 9.
2. **An aspect whose target is not equipped** is equipped but INACTIVE (§2.4). Slice 5 has a row for it.
3. **The fragment negative bound spans both systems**: accessories (4) + fragments (4) (§2.3).
4. **The star is regression-gated** in slice 1, row by row (§3.1).
5. **The Q-drop and mining-swing spikes BLOCK slice 1's build** (§3.1.0).
6. **`/rpg cast` for operators** was §5 Q2, now ruling 10.
7. The two element/weapon-source questions became Ben's rulings 7 and 8 above, and are **not** in §5.

---

## 1. FINDINGS: THE CURRENT STATE, EACH CONFIRMED OR CORRECTED FROM SOURCE

### 1.1 How abilities reach play — CONFIRMED: a kit-granted ability has NO in-combat input

There are two engines, and they share one method at the end: `AbilityService.resolve`. It checks the
cooldown, then calls `ResourcePool.tryConsume` (the mana check and the deduction are one atomic call),
then triggers the cooldown.

**Path A, weapon triggers.** `WeaponLoader.parse` reads each key under a weapon's `triggers:` as an
input name. It builds that trigger's ability **inline**, as a fresh `AbilityDefinition` with the id
`weaponId + "/" + input` and archetype `"none"`. The keys a trigger accepts (`TRIGGER_KEYS`: `name`,
`description`, `cooldown_ticks`, `cost`, `cast`, `on_hit`, `on_cast`) contain **nothing that refers to a
registered ability by id**. A weapon can copy an ability's body and never cite it.

Each input's dispatch:

- `left_click`: `WeaponSwingListener.onSwing` (the arm-swing packet, hopped through
  `PacketListenerBase.bukkit`) calls `WeaponFire.attempt(..., "left_click", ...)`.
- `right_click`: `RpgListeners.onRightClick` calls `WeaponFire.attempt(..., "right_click", ...)`.
- `draw`, `tap1`-`tap3`: `PlumeDraw.release` calls `WeaponFire.attemptFan`.
- A basic melee hit: `onPlayerMeleeAttack` calls `WeaponFire.landVanillaMelee`, then
  `CastExecutor.landBasicMelee`. No `resolve` runs, so there is no cooldown and no mana.

`WeaponFire.attempt` calls `WeaponService.fire`, then `AbilityService.fireTrigger`. That method *"skips
the registry lookup and the castable gate cast() applies"*. Its cooldown key is `weaponId/input`.

**`on_cast` is not an input.** It is a visuals-only list (`AbilitySchema.parseCastVisuals` refuses
anything else), fired once per press by `CastExecutor.commit`.

**Path B, `/rpg cast <id>`.** `RpgCommand.cast` builds `castable = Set.copyOf(profile.unlockedAbilities())`
and calls `AbilityService.cast`. That checks, in order: the id is unknown → `UnknownAbility`; the id is
not in `castable` → `Locked`; then `resolve`. Here the cooldown key is the **bare ability id**. So
`/rpg cast rekindle` and `ability_stone`'s copy of rekindle do **not** share a cooldown.

**`unlockedAbilities`:**

- One production writer: `RpgCommand.applyKit`, through `ProfileService.setKit` and
  `PlayerProfile.withKit`.
- Two production readers: the `/rpg cast` tab-completion suggester, and `RpgCommand.cast`.

**Kit `abilities:` lists** flow only into `unlockedAbilities`. No listener, packet handler or item reads
them.

**VERDICT, CONFIRMED: a kit-granted ability can be cast ONLY by `/rpg cast`.** That is a chat command,
and opening chat releases movement. `ability_stone.yml`'s own header says so. **Corollaries:**

- `rekindle` and `void_slash` are in **no** kit. `/rpg cast` returns `Locked` for them, for everyone.
  Rekindle is playable only through `ability_stone`'s hand-copied trigger.
- **CORRECTED: the brief lists `arc_surge` among "the existing Fire abilities". It is
  `element: nature`.** The fire abilities are `solar_grenade`, `solar_lance`, `ember_step` and
  `rekindle`. `arc_surge` is granted by the **fire** Ranger kit all the same. That is the kit's choice,
  and it does not make the ability fire.

### 1.2 The kit system, every reference, each with a removal verdict

**Code:**

| site | what it is | verdict |
|---|---|---|
| `C/kit/KitDefinition` | record: classId, elementId, displayName, `List<WeaponGrant>`, abilityIds; refuses a kit that grants nothing | **DELETE** |
| `C/kit/KitRegistry` | keyed by `KitKey(classId, elementId)`; `classes()` feeds `/rpg class` validation and completion | **DELETE.** The `KitKey` idea (a composite key, not concatenation) moves to the pool registry (§2.2) |
| `C/kit/WeaponGrant` | `(weaponId, equip)` | **DELETE** |
| `P/content/KitLoader` | `loadAll`, `parse` | **DELETE** |
| `P/content/ContentValidator.validateKits` | element, dangling abilities and weapons, "grants nothing that exists" | **DELETE**; replaced by `validatePools` (§2.2) |
| `P/RpgPlugin` `onEnable` | loads kits, counts them in the "Loaded ..." line, passes `kits` to `RpgCommand.build`, validates, accessor `kits()` | **REWIRE** to pools |
| `RpgCommand.chooseClass` | refuses ids not in `kits.classes()` (case-sensitive), then `applyKit` | **DELETE the command** (§2.6); the Build screen replaces it |
| `RpgCommand.chooseElement` | refuses ids not in `ElementRegistry`, then `applyKit` | **DELETE the command** |
| `RpgCommand.applyKit` | `profiles.setKit(...)`, then `grantWeapons`, then "You are now ... / Unlocked: ..." | **DELETE** |
| `RpgCommand.grantWeapons` | mint, `EnchantRollItems.rollOnAcquire`, `GearScoreItems.stampOnAcquire`, first-empty-hotbar for `equip` | **DELETE** — the duplicate defect goes with it |
| `S/PlayerProfile.withKit` | class, element and abilities rewritten together | **REPLACE** with `withCell(classId, elementId)`; abilities no longer travel with it (§2.1) |
| `P/profile/ProfileService.setKit` | write-through | **REPLACE** with `setCell` |
| `Permissions.CLASS` (`rpg.command.class`) | gates `/rpg class`, `/rpg element` | **DELETE** with the commands |

**Content: `content/kits/` holds exactly two files.**

- `ranger_fire.yml`: `hunters_bow` (equip), and the abilities `[arc_surge]`.
- `mage_fire.yml`: `ember_staff` (equip), and the abilities `[solar_grenade, solar_lance, ember_step]`.

**Both are DELETED.** `boltor.yml`'s comment recorded the ruling when this plan was written (*"KITS ARE BEING REMOVED ...
KitRegistry, KitLoader and their tests go with the kit files, and mage_fire.yml goes at the same time"*; slice 2 rewrote that comment to the present state),
and `NEXT.md`'s *PARKED SLICE — THE DEV-WEAPON AND `/kit` DELETIONS* names "the build system" as the
trigger. **That trigger is now met.** The NEXT.md row is closed in slice 2's PR, with the sha.

**The duplicate-weapons defect (PLAN-accessories §8.1) — CONFIRMED, and WIDER than recorded.** §8.1
names `chooseClass` → `grantWeapons` with no same-class refusal. Two more routes exist:

- `chooseElement` also reaches `grantWeapons`, so `/rpg element fire` run twice mints twice.
- Switching ranger → mage **leaves the bow**: nothing ever takes a kit weapon back.

**Verdict: none is fixed; all three are deleted with `grantWeapons`.** No weapon is minted on a class
change after slice 2.

**Other readers of the class field (`archetypeId`), NOT kit code, which STAY:**
`PlayerHealthSystem.accessoryContributions`, the `/rpg stats` accessory lines, `EquipmentMenu`, and
`NexusMenu`'s stats lore. They read the profile class, and the Build screen now writes it.

**Tests:**

| test | verdict |
|---|---|
| `KitRegistryTest` (core, 5) | **DELETE** |
| `KitLoaderTest` (paper, 7, incl. `bundledRangerFireKitLoads` / `bundledMageFireKitLoads`) | **DELETE** |
| `ContentValidatorTest`'s 4 `validateKits` cases | **DELETE**; `validatePools` cases replace them |
| `ProfileServiceTest.setKit*` (3) | **REWRITE** for `setCell` |
| `PlayerProfileMigrationTest.withKit*` (2) | **REWRITE** for `withCell` |
| `GearScoreWiringSignatureTest` — "the kit grant must stamp", read from `RpgCommand` source | **EDIT**: the kit arm goes; the `/rpg give` and craft arms stay |
| `AccessorySlotsTest.theClassTokenIsTheKitSpelling` | **RENAME + re-point** to the pool's class token |
| `GearClassOfTest`, `ScorchContentInvariantTest` | prose only; **EDIT** the prose |

**Comments and docs that cite kits** (a word-boundary grep, since most raw hits are "Bukkit" and "skip"):

- **Java comments**, each **EDITED** in slice 2: `EffectApplier`, `EffectSpec`, `AccessorySlots`,
  `Scorch`, `WeaponDefinition` (*"element flavors a kit"*), `Permissions.DEV`, `InventoryCraft`,
  `EnchantRollItems` (*"a starter kit"*), `GearItems`, `GearScoreItems`.
- **Content comments**, each **EDITED**: `boltor.yml`, `dragons_breath.yml`, `locust.yml`,
  `flint_staff.yml`, `brawlers_gauntlet.yml` (*"no melee kit ships, so /rpg class melee is refused"* —
  that sentence becomes *"no melee pool ships"*), `rekindle.yml`, `ignite_blast.yml`,
  `quiver_stone.yml`, `volley_stone.yml`. `ability_stone.yml` is deleted in slice 1 (§1.4).
- **Docs:** `NEXT.md` (the parked-deletion section, the Commit F history), `DESIGN-build-system.md`,
  `PLAN-weapons-elements-classes.md`, `PLAN-accessories.md`, `GATE-accessories-a.md`,
  `GATE-gearscore.md`, `GATE-plume-draw.md`, `HANDOFF-damage-system.md`,
  `PLAN-abilitystone-dashlift.md`, `PLAN-enchant-rolls.md`, `PLAN-boltor.md` and others. **These are
  historical records and are NOT rewritten.** Only `NEXT.md`'s open parked item is closed.
  `CLAUDE.md`, `README.md`, `PROJECT.md` and all three `.claude/rules/*.md` have **zero** kit mentions.

**Where a new player receives a weapon today:**

1. **On join: NOTHING.** `RpgListeners.onJoin` runs `GearRefresher.refresh` (it rebuilds weapons already
   carried) and places the star. `PlayerProfile.fresh` sets class and element to `"none"` and grants no
   abilities.
2. **`/rpg class` + `/rpg element`** → `applyKit` → `grantWeapons`. This is the only automatic starter
   source. It is removed in slice 2.
3. **`/rpg give`**: `RpgCommand.give`, op-only (`rpg.command.give`).
4. **Crafting**: `InventoryCraft` mints for `content/recipes/`. There are three recipes, `cursed_emerald`,
   `flint_staff` and `lapis_staff`, and each `mints:` a weapon whose file reads **`class: mage`**. **No
   recipe makes a ranger or melee weapon.**

**So after slice 2, a new RANGER's only weapon source is `/rpg give`, and a new MAGE's are `/rpg give` or
three recipes.** Under ruling 8 this is **a known gap, not a defect**.

**A stale-file trap (inference from `saveResource(path, false)`, which never overwrites or deletes):**
deleting `kits/*.yml` from the jar leaves both files on disk in any existing `run/plugins/Rpg/content/`.
After slice 2 nothing loads that directory, so they are inert. **The slice 2 gate reads that no
"kits" count appears in the Loaded line**, rather than assuming it.

### 1.3 The Nexus star machinery — the parts, and which GENERALISE

| part | what it does | generalise or copy |
|---|---|---|
| `P/nexus/NexusItems.mint` | `NETHER_STAR` named "Nexus Menu", PDC `keys.nexus` BYTE 1, `setMaxStackSize(1)` | **copy** — a second item has its own mint |
| `NexusItems.isNexus` | `has(keys.nexus, BYTE)`. Identity is the key, never the material (`health_boost_TEMP` is also a nether star) | **generalise** to `has(key, BYTE)` |
| `P/nexus/NexusLock` (`refusesClick`, `refusesDrag`, `touchesTheStar`) | a pure decision over `Touched` slots. Knows the item only as a `cursorIsStar` flag and a `starAt` `IntPredicate`. Two arms: the locked slot, and follow-the-item | **ALREADY GENERIC.** Only the names say "star" |
| `P/nexus/NexusSlots.lockedSlotOf`, `chosenSlotOf`, `validSlotOr` (`MAX_SLOT = 35`) | profile slot + enabled toggle → a lock index | **generalise** — the slot getter, the enabled getter and the valid range become parameters |
| `NexusSlots.refuses` (click, drag) | translates a Bukkit event into `NexusLock` | **generalise**, then called once per item and OR-ed |
| `NexusSlots.touchedOf`, `isOwnInventoryScreen` | view → index | **already generic** |
| `NexusSlots.converge` | dedupe, keep the lowest, move or mint into the target, hand back the displaced occupant via `MenuSafety.give` | **generalise** (a descriptor supplies the predicate, mint and default slot) |
| `NexusSlots.removeStars` | delete all | **generalise** |
| `NexusOpenGesture.opensHub` | a LEFT/RIGHT click on the star in the own inventory screen opens the hub | **star-only**; the stone has no open gesture |
| `NexusCollisionNotice` | a throttled line when the star meets a hijacked block | **star-only** |
| `P/menu/SettingsMenu.setStar` | enable refuses an occupied slot; `converge` on / `removeStars` off; write-through | **generalise** into one toggle routine parameterised by the descriptor |
| `SettingsMenuLayout` | picker 22, toggle 24, back 48, close 49; `SETTING_SLOTS = {22, 24}` | **extend**: the stone picker and toggle need two new cells (§2.5) |
| `P/menu/NexusSlotPickerMenu` / `NexusSlotPickerLayout` | 36 cells (menu 9-35 → inv 9-35, menu 36-44 → hotbar 0-8); clones shown; occupied targets NOT refused (converge displaces) | **generalise** with an allowed-slot set, plus the **"reserved by your other item"** branch its javadoc says was dropped because *"we have one"* |
| `S/PlayerProfile.nexusSlot`, `starEnabledOrNull` (boxed, JSON `starEnabled`, null = ON) | persisted | **copy the shape** as new fields (§2.1) |
| `RpgListeners.onJoin` | `whenSettled` → `onEntity` → if enabled, `converge` | **generalise** — one call per descriptor, in a fixed order |
| `RpgListeners.onPlayerRespawn` | if enabled, `converge(lockedSlotOf)` | **generalise**, same order |
| `onPlayerDeath` | `setKeepInventory(true)`, drops cleared — nothing Nexus-specific | unchanged |

**The six star-only handlers** (all in `RpgListeners`, each identified by `NexusItems.isNexus`):

| handler | event, priority | action |
|---|---|---|
| `onNexusClick` | `InventoryClickEvent`, LOWEST | `NexusSlots.refuses` → cancel + `updateInventory`; the open gesture → hub |
| `onNexusDrag` | `InventoryDragEvent`, LOWEST | refuse |
| `onNexusDrop` | `PlayerDropItemEvent`, NORMAL, no `ignoreCancelled` | cancel + `updateInventory` |
| `onNexusSwapHand` | `PlayerSwapHandItemsEvent` | cancel if either hand is the star |
| `onNexusGiveToEntity` | `PlayerInteractEntityEvent` (also receives the `...AtEntity` subclass) | cancel |
| `onNexusArmorStand` | `PlayerArmorStandManipulateEvent` | cancel |

`NexusWiringSignatureTest` pins **all six names and their priorities**.

**RECOMMENDATION: GENERALISE, not copy.** Introduce a `LockedItem` descriptor in `P/nexus/`. It carries:

- the PDC key predicate;
- `mint`;
- the chosen-slot getter;
- the enabled getter;
- the default slot;
- the allowed slot set (star: 0-35; stone: 0-8).

`NexusLock` needs no change. `NexusSlots`' methods take a descriptor. The six handlers **keep their
names** (so the signature test stays green and meaningful) and ask *"is this ANY locked item"*.
`onNexusDrop` additionally casts when the item is the stone (§2.5).

**Why not copy:** a copied lock is two lock implementations that must stay in step. GATE-nexus.md rows
6.4 and 6.5 are two duplication holes found in ONE implementation. A second copy doubles the surface
for the next one.

**The hazard the second item creates (inference, stated as such):** `converge(A)` hands back whatever
sits on A's target through `MenuSafety.give`. If B sits there, B is displaced, and `converge(B)` then
moves it back. If both point at the same slot, they fight. **Three guards:**

1. **The picker refuses the other item's slot.** This restores the dropped "reserved" branch.
2. **Enabling refuses an occupied slot** (already true for the star), so it refuses the other item's
   slot.
3. **A fixed converge order: star first, then stone.** A unit test pins that the two targets can never be
   equal after any sequence of picker and toggle calls. The rule lives in core, as a pure
   `LockedSlots.conflict(starSlot, stoneSlot)`.

### 1.4 The old `ability_stone` weapon — DELETE IN SLICE 1

- **The file**, `content/weapons/ability_stone.yml`: `id: ability_stone`, `class: mage`,
  `material: amethyst_shard`, `rarity: exotic`, `element: kinetic`. It has one `left_click` trigger,
  "Test Cast": `cooldown_ticks: 0`, no `cost` (so `ResourceCost.FREE`), a reverse-facing `dash`, and an
  `on_hit` that is rekindle's body copied by hand.
- **Identity:** the generic `Keys.weaponId` (`"weapon_id"`) = `"ability_stone"`, plus the usual roll and
  score keys. **It owns no key.** Its synthesised ability id is `ability_stone/left_click`.
- **No `AbilityStone*` Java class exists.**
- **FOUND: `Keys.abilityId` = `NamespacedKey(plugin, "ability_id")` is DEAD** — declared with no
  javadoc, with no reader and no writer anywhere. It survived the package-rename commit `f57536d`. **The
  new item must not reuse it**, because the name invites exactly that. Slice 1 deletes the field, and
  the new key is named for the item (§2.5).

**Every citation:**

- **Java comments (13 lines, all comments, 0 code — the standing-decisions account's own measurement):**
  `DirectDamage`, `AbilityDefinition`, `Durability`, `CraftResultIndex`, `Ignite` (core); `WeaponItems`
  (×2), `WeaponDurability` (×2), `RpgListeners`, `RpgCommand`'s durability-message comment,
  `ContentValidator`, `RecipeProbe` (paper).
- **Tests:** `WeaponLoaderTest.bundledAbilityStoneContentLoads` (it loads the real file) → **DELETE**.
  `WeaponLoaderTest.loadsADashCastInATrigger` uses an **inline** fixture named `ability_stone` →
  **RENAME** the fixture, so the grep sweep reads zero. `WeaponLoreTest`'s `weaponIds` array → remove
  the id. `paper/src/test/resources/golden-lore.txt`'s `-- ability_stone` block → remove it. Slice 1 reads
  `GoldenLoreTest` for its update procedure; this plan did not. The comments in `AbilityLoaderTest`, `WeaponItemsTest`,
  `DamagePayloadTest`, `DurabilityTest` and `CastExecutorTriggerScoreTest` → **EDIT**.
- **Content comments:** `volley_stone.yml`, `quiver_stone.yml`, `lapis_staff.yml`, `flint_staff.yml`,
  `cursed_emerald.yml`, `visuals/flint_cast.yml` → **EDIT**, each to cite the Boltor or to drop the
  reference. `boltor.yml` cites it **0** times.
- **The deletion set.** `.claude/rules/standing-decisions.md`, the entry **NEW CONTENT CITES THE
  BOLTOR**, lists four members; `ability_stone` joined **2026-09-22**. Slice 1 **removes it from the
  set** in both the account and CLAUDE.md's pointer, which becomes *"`hunters_bow`, `ironblade`,
  `quiver_stone`"*. The account keeps a dated line recording that it was deleted, and by which PR.
  **This is a CLAUDE.md edit, so it rides slice 1's PR and is called out in the PR body.**
- **Docs:** `GATE-gearscore.md` (the closed flag), `PLAN-abilitystone-dashlift.md`,
  `PLAN-enchant-rolls.md`, `PLAN-weapon-lore.md`, and ~15 spots in `NEXT.md`. These are historical and
  are **not** rewritten. `GATE-gearscore.md` flags that three files still call the stone "PERMANENT"; those
  are among the content comments above.

**RECOMMENDATION: delete the old weapon in slice 1, the slice that ships the new item.** While both exist,
they would share a display name and nothing else. Deleting them together means the name never refers to
two things. **Rekindle loses its only playable route in the same commit, and gains a better one**: it
becomes a Fire Ranger Active in the default pool (§4).

### 1.5 The ability engine, as it bears on aspects

- **`C/ability/AbilityDefinition`**, a record: `id`, `displayName`, `element`, `archetypeId`,
  `cooldownTicks`, `ResourceCost cost`, `CastSpec cast`, `List<EffectSpec> onHit`, `description`,
  `List<EffectSpec.Visual> onCast`.
- **`CastSpec`** is sealed: `Self`, `Melee(reach, arc)`, `Ray(range, beam)`,
  `Projectile(speed, gravity, maxLifetime, trail, item, homing, body, spread)`,
  `Dash(distance, speed, lift, direction)`, `Volley(windup, shots, interval, of)`.
  `minimumCooldownTicks` is non-zero only for Volley.
- **`EffectSpec`** is sealed into two groups:
  - **Targeted**, skipped when there is no target: `Damage(amount, element)`, `WeaponDamage(element)`,
    `Heal(amount)`, `Knockback(strength)`, `Status(statusId, durationTicks, amplifier)`.
  - **Untargeted**, always run: `Burst(radius, List<Targeted>)`,
    `Area(radius, duration, interval, List<Targeted>)`, `Visual(id)`,
    `ThrowEmbers(angles, speed, lift, item, fuse, Burst, visual, trail)`.

  `Burst` and `Area` nest only Targeted effects, enforced both by the types and by
  `AbilitySchema.parseNestedEffects`.
- **The loader:** `AbilityLoader.parse` → `AbilitySchema` (shared with `WeaponLoader`). A bad file is
  skipped. `ContentValidator.validate` warns and never throws. `AbilityRegistry.register` throws on a
  duplicate id.
- **Cooldowns:** `C/combat/CooldownTracker`, a `Map<UUID, Map<String, Long>> readyAt`, **per player × per
  id**, in memory, cleared per player.
- **Mana:** `C/combat/ResourcePool`, per owner × resource, with lazy regen. `tryConsume` is the check and
  the deduction in one call, inside `AbilityService.resolve`.
- **Every read of an effect list at cast time** is from the `AbilityDefinition` inside
  `AbilityService.CastResult.Success`:
  - `CastExecutor.commit` (onCast, and onHit for `headlineDamage`);
  - `detonate` (onHit);
  - `dash` (onHit, via `EffectApplier.applyToSet`, which runs Untargeted effects at the pre-dash origin and
    Targeted effects on every body swept);
  - `landBasicMelee` and `volley` (`headlineDamage`);
  - `charges` and `resolve` (`isBasicAttack`).
- **`archetype:` — CONFIRMED DEAD.** It is parsed into `AbilityDefinition.archetypeId` and **read by
  nothing**. The values in content are `hunter`, `mage` and `ranger`. `hunter` is the Commit F archetype
  that kits generalised (`KitDefinition`'s javadoc). **It does not mean "this ability belongs to that
  class"**: `ember_step` is `archetype: mage` and `rekindle` is `archetype: ranger`, but `solar_grenade`
  and `solar_lance`, both in the MAGE kit, say `hunter`.
- **`element:` on an ability** is required. It is checked by `ContentValidator.checkElement` (warn only)
  and printed by `/rpg`. It drives nothing: the element that matters is each `Damage`'s own.

**WHERE AN ASPECT ATTACHES — RECOMMENDATION: a DERIVED DEFINITION, swapped into `Success`, per player, at
cast time.**

- `Success` already carries the definition from decide to execute, and `DashAim.resolve` already rewrites
  a `Success`. That is the precedent.
- The derived record keeps the **same id**, so the cooldown and the `Locked` gate are untouched. It
  **appends** to `onHit` / `onCast`, so `headlineDamage` and `isBasicAttack` (both "first damage effect
  found") are unchanged.
- The derivation is a pure core function, `AspectApplication.derive(AbilityDefinition,
  List<AspectDefinition>)`, unit-tested without a server.
- Derivations are **memoised per (ability id, sorted active-aspect ids)** — a handful of records per
  player.
- **Rejected alternatives:**
  - *At load time, a global derived registry*: aspects are per player, and the registry is shared.
  - *An extra list threaded into `detonate`, `dash`, `commit` and `volley`*: four signatures change, and
    each future cast shape must remember the second list.

### 1.6 The stat pipeline — fragments ride it, NOT unchanged

`PlayerHealthSystem.startReconcileLoop` runs every `RECONCILE_PERIOD_TICKS` (5). On each pass:

- It reads `accessoryContributions` once. That is `Accessories.contributions(id, profileClass)`, gated by
  `AccessorySlots.contributes`.
- It merges those into each stat's desired map **before** that stat's single reconcile. The stats are
  max HP, crit chance, crit damage, the mana pair through `ManaTransition.reconcile`, health regen and
  defense.
- `class_damage` goes in as `ClassGrant`s, into `ClassDamageModifierItems.desiredModifiers`, before
  `ClassDamageModifiers.matching`.

`ModifierReconciler.reconcile` removes every applied key absent from the desired map. So **two
reconciles of one stat wipe each other.**

Keys are strings: `accessory:<slot>`, plus the other scanners' prefixes. `AccessorySourceKeysTest` asserts
disjointness.

**A fragment can be a stat source on this loop, with FOUR changes, none of them to `ModifierReconciler`:**

1. **A third map at every merge point in `startReconcileLoop`.** `AccessoryContributions.merged` takes
   exactly two, so it becomes an n-way merge helper. Never a second reconcile call.
2. **A `fragment:<slot>` prefix**, added to `AccessorySourceKeysTest`'s other-scanner list.
3. **A negative bound that counts both systems** — §2.3.
4. **A `FragmentContributions`** parallel to `AccessoryContributions.of`. That class takes
   `AccessoryDefinition` and `AccessorySlots.COUNT`, so it cannot be reused literally. It reads the
   **build store**, not items: a fragment is an id in the loadout, not an `ItemStack`.

**There is no element gate in accessories today** (only the class). Fragments are cell-specific, so
their gate is **the loadout of the CURRENT cell** (§2.6), not an eligibility test.

### 1.7 Input capture for a held item — what fires today, quoted where it can be

**What the server does, quoted from the build-74 jar:**

- **`ServerGamePacketListenerImpl.handleAnimate`** (the arm-swing packet) ray-traces from the eyes, then:
  - no hit → `callPlayerInteractEvent(LEFT_CLICK_AIR)`;
  - `GameType.ADVENTURE` and a hit block → `LEFT_CLICK_BLOCK`;
  - otherwise, not `CREATIVE`, and a hit entity **beyond** `entityInteractionRange` →
    `LEFT_CLICK_AIR`.

  Then it **always** constructs and calls `PlayerArmSwingEvent`. **So in SURVIVAL, left-clicking a block
  is NOT reported by the swing packet.**
- **`ServerPlayerGameMode.handleBlockBreakAction`** (the dig packet) fires `PlayerInteractEvent`
  `LEFT_CLICK_BLOCK`, then `CraftEventFactory.callBlockDamageEvent`, then reads `getInstaBreak`.
- **Q:**
  - From the hotbar, with no screen open: `ServerPlayer.drop(boolean)` → `drop(ItemStack, Z, Z)` →
    `LivingEntity.drop(ItemStack, Z, Z, Z, Consumer)`. **That method constructs `PlayerDropItemEvent`.**
  - From an open container screen, `ContainerInput.THROW` in `AbstractContainerMenu.doClick` →
    `Player.drop(ItemStack, Z)` → the same `drop(ItemStack, Z, Z)`, and so the same event. CraftBukkit's
    `InventoryClickEvent` fires first, and a cancel there stops the drop.
  - In **creative**, the THROW arm calls `Player.handleCreativeModeItemDrop` instead.

**What the plugin does today:**

| input | listener(s) | what happens for a non-weapon item |
|---|---|---|
| left, air | `WeaponSwingListener.onPacketReceive` (ANIMATION, main hand) → `bukkit()` → `onSwing`: `Quivers.tryReloadHeldWeapon`, then `WeaponFire.attempt("left_click")` | nothing: both key on `weapon_id` |
| left, block (survival) | the swing packet, as above; **no** handler for `BlockDamageEvent`, `BlockBreakEvent`, or left-click `PlayerInteractEvent` | the block is mined normally |
| left, entity | swing (as above) + `onPrePlayerAttack` (records attack charge) + `onPlayerMeleeAttack` (HIGH, **any** item: `setDamage(TOKEN_DAMAGE)` 0.01, `landVanillaMelee` empty) + `onPlayerSweepAttack` | a 0.01 token hit plus vanilla hurt effects |
| right, air / block | `onRightClick` (no `ignoreCancelled`, main hand, RIGHT_CLICK_*). Order: (1) star → hub; (2) `openHijackedBlock` (enchanting table, crafting table, grindstone, anvils, ender chest; not when sneaking); (3) `WeaponFire.attempt("right_click")`; (4) `plumeDraw.onDrawStarted` | vanilla use |
| right, entity | `onNexusGiveToEntity` (star only), `onNexusArmorStand` (star only); `ExampleTelegraphListener` sees the packet and does nothing | vanilla interact |
| Q, no screen | `onNexusDrop` (star only) | dropped |
| Q, screen open | `onNexusClick` at LOWEST through `NexusLock`; in one of our menus, `MenuRouting` permits `DROP_*` in the player's own half | dropped |

**Creative in code:**

- `NexusLock.refusesClick`'s `CREATIVE && cursorIsStar` arm.
- `NexusOpenGesture` excludes `CREATIVE`.
- `MenuRouting.route` refuses `CREATIVE`.
- `QuiverAmmo` and `EquipmentMenu` read the game mode. **`QuiverAmmo`'s javadoc still says it is "THE ONLY
  GameMode READ", and that is stale** (§6).
- GATE-nexus.md row 8 is the precedent that creative splits one gesture into several single-slot writes.

**The conflicts a stone that casts on left, right and Q must resolve:**

1. **Q IS ALSO A LEFT CLICK — MEASURED (§3.1.0.1).** Every Q-drop measured came with an arm swing in the
   same tick, from the hotbar and from the open inventory, in survival and creative. Unguarded, one Q
   press casts the Ultimate **and** Active 1. *(This was a question here until the spike; S1 answered
   it.)*
2. **Holding left on a block in survival sends a swing EVERY TICK — MEASURED (§3.1.0.1)** — and a
   block-damage-abort on release. The mining itself must also be stopped.
3. **Left on an entity is two paths**: the swing packet and the attack event. It must cast once and deal
   no token hit.
4. **Right on an entity** may reach `PlayerInteractEntityEvent` **and** a RIGHT_CLICK_AIR
   `PlayerInteractEvent` (a client fallback; an inference, not verified). **SPIKE S3** — it runs in
   slice 1 but does not block the build: the guard below is correct whichever way S3 reads.
5. **Right on a hijacked block**: today the block wins over any held item except the star.
6. **Q inside a screen** reaches `InventoryClickEvent` first. Should it cast? (§2.5 says no.)
7. **Offhand**: `onRightClick` and the swing listener filter to the main hand, and `onNexusSwapHand` needs
   the widened predicate.
8. **Pinned names**: `NexusWiringSignatureTest` pins six handlers, and `NexusMenuLayoutTest` pins
   `PAINTED_SLOTS.size() == 9`.

---

## 2. THE ARCHITECTURE — ONE RECOMMENDATION EACH, WITH THE ALTERNATIVE NAMED

### 2.1 Storage for the build

**RECOMMENDATION: a separate repository, `S/BuildRepository`, modelled on accessories.** Its parts:

- `S/FileBuildRepository`, writing `builds/<uuid>.json` (temp then atomic move);
- `S/PlayerBuild(schemaVersion, playerId, Map<CellKey, Loadout> loadouts)`;
- `S/Loadout(String ultimate, List<String> actives /*2*/, List<String> aspects /*2*/, List<String> fragments /*4*/)`,
  where each entry is a content id or null;
- `S/BuildMigrations` (a v1 stamp; a newer version is refused).

Paper side: `P/build/BuildService` owns the cache, loads on join, writes through, and **poisons on a
failed write** — the `AccessoryService` pattern.

*(Java refresher for Ben: `Map<CellKey, Loadout>` is a lookup table from a (class, element) pair to that
cell's loadout.)*

- **One loadout PER CELL**, not one per player. Switching Fire Ranger → Fire Mage → Fire Ranger restores
  the Ranger build. Nothing is lost on a class change, which is the same promise the accessory inactive
  rule makes.
- **Ids, not items.** Fragments and aspects are not `ItemStack`s. So there is no base64 and no
  opaque-slot case. **An id no longer in the pool** (content removed) reads as an **empty slot**, logged
  once, and is kept in the file until the player overwrites it.

| | separate repository (recommended) | profile field |
|---|---|---|
| cost | 4 small storage classes, one service | schema 4 → 5, a 13th field through **all seven** `withX` rebuilds, a migration step, every `PlayerProfileMigrationTest` fixture |
| blast radius of a bad write | the build file | **the profile**: class, XP, Nexus slot |
| per-cell map | natural | a nested structure in the profile |

**What DOES go in the profile:**

1. **The cell.** `archetypeId` (class) and `elementId` already exist. `withKit` becomes
   `withCell(classId, elementId)`.
2. **The stone's slot and toggle**, as **boxed** fields beside the star's: `Integer stoneSlotOrNull`
   (null = the default slot) and `Boolean stoneEnabledOrNull` (null = the default state). **They sit in
   the profile, not the build store**, because join convergence reads the star's fields from the profile
   in `whenSettled`, and two stores would make convergence wait on both. **Boxed nulls need no migration
   step** — the same rule `starEnabled` used (`ProfileMigrations`' "no step" entries). The schema version
   still bumps to 5, so an older server refuses the file, as v3 → v4 did for `vaultMigrated`.

**The fate of `unlockedAbilities`: RETIRED, not removed.**

- The field stays in the record (removing it is a schema change for nothing). It is written `List.of()`
  by `withCell` and read by nothing.
- The "castable" set becomes **the equipped abilities of the current cell's loadout**.
- **Migration:** none needed. An old profile's list is ignored. Its old kit grants are superseded by the
  pool's default loadout (§2.2), which a player with a class and element gets automatically.
- A comment on the field says *"unread since slice 2; kept for schema stability"*, exactly as
  `archetypeId`'s name is kept.

### 2.2 How a pool is declared

**RECOMMENDATION: one POOL FILE per cell**, `content/builds/<class>_<element>.yml`, the kit files'
naming. It lists ids by kind and names the default loadout:

```yaml
class: ranger
element: fire
display_name: "<gold>Fire Ranger</gold>"
# every id below that does not exist at bccef7d is an ILLUSTRATIVE name, not a proposal (§4)
ultimates: [sunfall]                       # §4 — Ben designs
actives:   [rekindle, solar_lance, arc_surge]
aspects:   [banked_embers, searing_lance]
fragments: [ember_heart, keen_ember, kindling, warm_blood]
default:
  ultimate: sunfall
  actives: [rekindle, solar_lance]         # left, right
```

- A `C/build/PoolRegistry` keyed by `CellKey(classId, elementId)` — `KitKey`'s composite key, kept.
- `P/content/PoolLoader` follows the ShieldLoader template: sorted, per-file, a bad file logged by name
  and skipped.
- `PoolLoader` **REFUSES the file** when any of these fails, with the id checks done against the
  registries after they load. (`ContentValidator` only warns today, which is why the refusal belongs to
  the loader.)
  - every id exists in its registry;
  - `default` is drawn from the lists;
  - there are at least 1 ultimate and 2 actives, so the default loadout can be filled;
  - no ability appears in both `ultimates` and `actives` of one pool.

  A pool that loads with holes would make the Build screen offer nothing.
- **Ruling 7 falls out of this:** the Build screen offers exactly the elements of the loaded pools.
- **Ultimate vs Active is a property of the POOL LISTING**, not of the ability file. The same ability
  could be an Active in one cell and absent from another.
- **`archetype:` is RETIRED.** It is dead (§1.5), and it contradicts the kits for two of the four fire
  abilities. Slice 2 removes the key from every ability file and makes `AbilityLoader` **ignore it with
  a one-time warning**, so a stale copy in `run/` does not refuse the file. The record field
  `archetypeId` on `AbilityDefinition` is deleted. The weapon triggers' hard-coded `"none"` goes with it.

**The alternative, tags on each item** (`kind`, `class`, `element` on every ability, fragment and aspect
file):

- *It scatters the pool across N files*, so the answer to "what can a Fire Ranger pick" is a grep.
- *It needs a many-valued tag* the first time one ability is shared by two cells.
- *It encodes Ultimate-ness on the ability*, which then cannot differ by cell.

The pool file is one place, one test, and a one-for-one replacement of the kit files.

### 2.3 New content kinds

**FRAGMENT** — `content/fragments/<id>.yml`, the id is the filename:

```yaml
display_name: "<gold>Ember Heart</gold>"
icon: blaze_powder            # menu icon only — never minted, never an item
description: ["A little more of you burns."]
modifiers:
  max_health: 4
```

- **The stat whitelist is `AccessoryStat`, REUSED.** It is the same eight tokens and the same negative
  policy per stat (`Negatives.REFUSED` / `BOUNDED`). A `FragmentLoader` refuses anything else.
- **`class_damage` is REFUSED on fragments in v1.** It is a `ClassGrant` path that accessories reach only
  from the class slot. A fragment is already class-scoped by its pool, so it would be the same number
  arriving by a second door. Revisit if Ben wants it.
- **One of each (ruling 11).** The Build screen never offers a fragment already slotted, and a loadout that names one twice (only a hand-edited file can) keeps the first, reads the rest as empty slots, and logs it. It is not refused: the other three fragments stay usable.
- **The display is the Build screen and the stats sheet**, a source line "Fragments", as §7 Q4 of the
  accessories plan did for accessories.

**THE NEGATIVE BOUND SPANS BOTH SYSTEMS (amendment 3).** Today `AccessoryNegatives.refusal` refuses a
BOUNDED negative unless `AccessorySlots.COUNT × |amount| < base`. That is four slots of one drawback. But
one stat can now be moved by **4 accessories + 4 fragments = 8 sources**. **So the bound for BOTH kinds
becomes `8 × |amount| < base`.**

- **One CLAMP, stated once:** `C/build/StatSourceBound.WORST_CASE_SOURCES = AccessorySlots.COUNT +
  FragmentSlots.COUNT`, and `AccessoryNegatives.refusal` is re-pointed at it.
- **Why one and not two:** two bounds (4× per system) would each pass while the pair reached the clamp.
  That is exactly the drawback that reads "−X" and does nothing, which NAME THE QUANTITY forbids.
- **Consequence, stated because it touches shipped content:** tightening the accessory bound can
  **refuse an accessory file that loads today.** Slice 4 measures every shipped accessory's negatives
  against `8×` before merging. If any fails, the plan is re-put to Ben, not quietly renumbered. (The six
  shipped accessories' negatives are read in slice 4, not asserted here.)

**ASPECT** — `content/aspects/<id>.yml`. See §2.4.

### 2.4 THE ASPECT VOCABULARY (ruling 3)

**What an aspect TARGETS: ONE ability id.** It is named in the aspect file, and must be in the same pool.
*Rejected: a slot target ("my Ultimate", whatever it is).* It would silently re-bind when the player swaps
the Ultimate, so one aspect would mean different things on different days, and every aspect × every
eligible ability would need designing. *Rejected: "every ability of the cell".* It is the same problem,
multiplied.

**What an aspect may ADD in v1, built only from existing `EffectSpec`:**

| key | adds to | what it may contain |
|---|---|---|
| `add_on_hit` | the target's `onHit`, **appended** | any `EffectSpec` the schema accepts: Targeted (damage, heal, knockback, status) and Untargeted (burst, area, visual, throw_embers) |
| `add_on_cast` | the target's `onCast`, appended | visuals only — the same rule `parseCastVisuals` enforces |

Both keys are parsed by the **existing `AbilitySchema.parseEffects` / `parseCastVisuals`**, so an aspect
can express exactly what an ability can, and **no new effect type is introduced.** "Add a status" is an
`add_on_hit: [{type: status, ...}]`. "Add a secondary blast" is a `burst`. "Add a lingering field" is an
`area`.

**Where appended effects land, stated because it differs by cast shape (from `CastExecutor` and
`EffectApplier.applyToSet`):**

- **projectile, ray, melee:** at the impact point and target, like the existing `onHit`;
- **dash:** Untargeted effects at the **pre-dash origin**, and Targeted effects on **every body swept**;
- **self:** on the caster.

The loader does not police this. The aspect file's author reads the target's cast shape, as ability
authors do today.

**How two aspects compose:**

- Both are appended, in **loadout slot order (aspect 1, then aspect 2)**. Order matters only for visual
  layering, because every effect resolves independently.
- **Two aspects on the SAME ability are allowed**, and stack.
- **The same aspect twice is refused** by the Build screen: it is one id.

**AN ASPECT WHOSE TARGET IS NOT EQUIPPED (amendment 2): it stays equipped and goes INACTIVE.**

- The Build screen shows it with *"Inactive — requires <ability> equipped"*.
- `AspectApplication.derive` ignores it.
- It becomes active again with no action when the target is re-equipped.
- This is the accessory inactive precedent (`AccessorySlots.contributes`; PLAN-accessories §2), for the
  same reason: nothing moves, so nothing can be lost or duplicated. *Rejected: unequipping it
  automatically*, because a player trying Actives in turn would lose their aspect choices.

**Deliberately OUT of v1:**

- **Conditions** ("if the target is scorched, also ..."). This needs a predicate language, and it is
  the "behaviour sub-language" `DESIGN-build-system.md` said to defer.
- **New cast shapes** (turning a ray into a projectile). This replaces the target's `cast`, which is not
  adding.
- **Resource changes** (a second resource, or a charge meter).
- **Passive, always-on effects** without a cast. There is no application loop for them; fragments are
  the always-on half.
- **Removing** anything from the target.
- *Changing the target's numbers was on this list until ruling 9.* It is now IN v1, under the whitelist
  in §2.4.1.

**Three worked examples on the shipped fire abilities:**

```yaml
# content/aspects/searing_lance.yml — the lance leaves a blast where it lands
display_name: "<gold>Searing Lance</gold>"
description: ["Solar Lance detonates on impact."]
target: solar_lance              # ray: appended effects land at the impact
add_on_hit:
  - type: burst
    radius: 2.5
    effects:
      - { type: damage, amount: 4, element: fire }
  - { type: visual, visual_id: solar_detonation }
```

```yaml
# content/aspects/cinder_wake.yml — the ground you step off keeps burning
display_name: "<gold>Cinder Wake</gold>"
description: ["Ember Step leaves a burning patch where you started."]
target: ember_step               # dash: an Untargeted area lands at the PRE-dash origin
add_on_hit:
  - type: area
    radius: 3.0
    duration_ticks: 60
    tick_interval: 20
    effects:
      - { type: damage, amount: 2, element: fire }
```

```yaml
# content/aspects/banked_embers.yml — two more embers, thrown wide
display_name: "<gold>Banked Embers</gold>"
description: ["Rekindle throws two more embers."]
target: rekindle                 # dash: throw_embers is Untargeted, launched from the origin
add_on_hit:
  - type: throw_embers
    angles_degrees: [70, -70]
    speed: 0.6
    launch_lift: 0.25
    item: blaze_powder
    fuse_ticks: 30
    trail: ember_trail
    visual: ember_burst
    burst:
      radius: 4.0
      effects:
        - { type: damage, amount: 8, element: fire }
```

Every visual, status and element named above exists at `bccef7d` (`content/visuals/`, `content/elements/`).
**The numbers are PLACEHOLDERS for §4, not proposals.** NAME THE QUANTITY applies to them when Ben sets
them: `searing_lance`'s radius is a real quantity on a ray, whose impact is a point.

### 2.4.1 THE NUMBER-CHANGE HALF (ruling 9) — FOR THE SEAT'S REVIEW

This is a new optional key, **`modify:`**, which sits beside `add_on_hit` and `add_on_cast`. It changes
numbers the target **already has**. It never creates a number the target lacks: that is what `add_*` is
for.

```yaml
modify:
  - field: damage.amount     # a whitelisted field, below
    percent: -25             # and/or  flat: <n>
```

**THE WHITELIST.** Anything not in this table is refused by `AspectLoader`, naming the field.

| field | what it addresses in the target | type, unit | forms |
|---|---|---|---|
| `cooldown_ticks` | the ability's cooldown | **int**, ticks | flat, percent |
| `cost` | `cost.amount` | double, mana | flat, percent |
| `damage.amount` | **every** `Damage` effect in the target's `on_hit`, including those nested in `burst`, `area` and `throw_embers.burst` | double, HP | flat, percent |
| `heal.amount` | every `Heal`, nested included | double, HP | flat, percent |
| `knockback.strength` | every `Knockback`, nested included | double, the push scalar | flat, percent |
| `burst.radius` | every `Burst`, including `throw_embers.burst` | double, blocks | flat, percent |
| `area.radius` | every `Area` | double, blocks | flat, percent |
| `area.duration_ticks` | every `Area` | **int**, ticks | flat, percent |
| `status.duration_ticks` | every `Status` **except `scorch`** (below) | **int**, ticks | flat, percent |

**Deliberately NOT on the list, each with its reason:**

- `weapon_damage`: it has no amount. It deals the caster's frozen attack stat, so there is no base to
  change.
- `area.tick_interval`: it changes the pulse count and the pulse rate at once. That is two quantities
  in one field, and the duration field already moves the count.
- Every `cast:` field (range, speed, dash distance, volley shots): a changed cast shape is still OUT
  (§2.4).
- `throw_embers` angles, speed and fuse: these are the geometry of the throw, not magnitudes.
- `status.amplifier`: nothing in shipped content authors one, so its quantity has not been named.

**"Every X, nested included" is the only addressing in v1.** There is no per-index addressing: it breaks
silently when the base ability's effect list is reordered, which is the line-number problem in another
form.

**THE ARITHMETIC — one formula, for every field and any number of aspects.** For one field of one
target, over every ACTIVE aspect on it:

    resolved = (base + Σ flat) × (1 + Σ percent / 100)

- Flats **add** with flats. Percents **add** with percents (two +10% make +20%, not +21%). The one
  multiplication happens after both sums.
- **So the order of aspects does not matter**: both sums commute. Slot order still decides only the
  order of *appended* effects (§2.4).
- **`modify` applies to the TARGET'S OWN effects, never to effects an aspect appends.** The appended
  effects are authored at their final numbers. Otherwise aspect A's `damage.amount −25%` would scale
  aspect B's appended burst, and two aspects would no longer be independent. So `derive` runs
  **modify first, append second.**
- **Inactive aspects** (target not equipped) contribute to neither sum.

**NAME THE QUANTITY, PER FIELD.** For each field: can the change take effect at the values it will meet,
and what does the loader do when it cannot?

**The policy is REFUSE, never round.** A rounded value is a number nobody authored, and the tooltip would
then advertise it. A refusal names the field, the resolved value, and the nearest legal values either
side.

- **`cooldown_ticks`**
  - *Arithmetic:* integer ticks, floored by `CastSpec.minimumCooldownTicks` (non-zero only for Volley)
    inside `AbilityService.resolve`. A resolved value below the floor would be clamped, and so inert.
  - *The 4-tick grid* (`WeaponLoader`'s `cooldown_ticks` section; the standing decision **NEW CONTENT
    CITES THE BOLTOR**) applies to **held right-click**: inputs from a held right-click land on a 4-tick
    grid. **The stone's Active 2 is a right-click input**, so whenever that input is held, a cooldown
    that is not a multiple of 4 is silently rounded UP, and the tooltip would not say so.
  - **Ruling 19 makes this exposure real, not hypothetical:** a held right-click re-casts whenever the
    cooldown allows, and its inputs arrive exactly 4 ticks apart (§3.1.0.1). A 13-tick cooldown held
    re-casts every 16. *This bullet previously said the one-cast-per-press recommendation removed the
    exposure; ruling 19 replaced that recommendation.*
  - *This bullet used to say "a stone ability can also reach a player through a weapon trigger's copy".
    That is false by §1.1*: a weapon trigger copies an ability's body inline and cannot reference a
    registered ability, so an aspect-modified ability never reaches a trigger. Corrected on the seat's
    review of `7c2da6c`.
  - **The loader refuses** a resolved value that is:
    - not an integer;
    - not a multiple of 4;
    - below the cast's floor;
    - `≤ 0`.
- **`cost`**
  - *Arithmetic:* continuous mana, and `tryConsume` compares a double.
  - **Refuse if resolved `< 0`.** `0` is allowed, and means free.
- **`damage.amount`, `heal.amount`**
  - *Arithmetic:* continuous HP.
  - **Refuse if resolved `≤ 0`**: a zero-damage effect is the "−X that does nothing" case.
  - *Stated, not guarded:* scorch accrual floors dealt damage into stacks (`Scorch.stacksFor`), so a
    small damage change can leave the stack count unchanged. That is a side effect of damage, not the
    field's quantity, and the tooltip names damage, not stacks.
- **`knockback.strength`**
  - *Arithmetic:* continuous.
  - **Refuse `≤ 0`.**
  - *Eligibility, which is the half that bites:* a ray does not push, because no vanilla knockback rides
    the ability path (standing decision, *A TRAVELLING RANGED WEAPON AUTHORS KNOCKBACK*). This field
    addresses the target's authored `Knockback` effects, so **a target with none makes the modification
    address NOTHING**, and the loader **refuses a `modify` whose field matches zero effects in the
    target.** The same refusal covers every "every X" field. It is the eligibility half, made
    mechanical.
- **`burst.radius`, `area.radius`**
  - *Arithmetic:* continuous blocks. `EffectSpec` already refuses `≤ 0`, and the loader refuses the same.
  - *Stated, not guarded:* who is caught is discrete (bodies are where they are), but the quantity itself
    is continuous.
- **`area.duration_ticks` — QUANTISED, and this is the trap in the list.**
  - `EffectApplier.tickArea` pulses at `interval, 2·interval, ...` while `next ≤ duration`, so the pulse
    count is `floor(duration / interval)`.
  - A change that does not cross a multiple of `tick_interval` changes **nothing**. `solar_grenade`'s
    area is 100/20, 5 pulses; +10% makes 110, still 5.
  - **The loader refuses a resolved duration that is not a multiple of that `Area`'s `tick_interval`.**
- **`status.duration_ticks`**
  - Integer ticks. Refuse a non-integer or `≤ 0` result.
  - **`scorch` is excluded outright.** Its burn count is `Scorch.damageTicksFor = ceil(duration / 20)`,
    which is quantised to its period. Scorch durations are also no longer authored in content (that
    method's javadoc), so there is no base to meet.
  - *NOT TRACED, AND SAID SO:* whether `freeze`, `rooted`, `soaked` and `surge` (a `potion` kind) are
    continuous in their duration. **Slice 5 traces each status kind's use of `durationTicks` before
    this field ships.** Until then the loader refuses `status.duration_ticks` on any kind not yet traced.

**The loader checks COMBINATIONS, not just single aspects.** A pool is finite, so `AspectLoader` (after
the pools load) resolves every field for every target:

- with each aspect alone;
- with **every pair** of aspects in the same pool that target the same ability.

Two slots means a pair is the worst case. Any illegal resolution refuses **both** files' pair, by name.
This is cheap: a pool has a handful of aspects.

**WHAT THE PLAYER SEES — the resolved number, never the base alone.**

- The Build screen renders each equipped ability's icon lore **from the derived definition**, the same
  `AspectApplication.derive` output the cast uses. So the tooltip and the cast **cannot disagree**: one
  function feeds both.
- A changed field reads `Damage: 9  (12)`: the resolved value, with the base in gray parentheses.
- Cooldown is shown in seconds to one decimal.
- An aspect's own icon lists its changes as authored (`−25% damage`) and its additions as lines.
- The stone's lore names abilities only, not numbers.

**The three examples, each with a `modify` added and resolved by hand:**

- **`searing_lance`** adds `modify: [{field: damage.amount, percent: -25}]`. The lance trades its hit for
  the blast.
  - `solar_lance`'s one `Damage` resolves to `(12 + 0) × (1 − 0.25)` = **9**.
  - The appended burst's `4` is untouched (modify first, append second).
  - Legal: > 0.
- **`cinder_wake`** adds `modify: [{field: cooldown_ticks, flat: 40}]`. The patch costs tempo.
  - `ember_step` resolves to `(160 + 40) × 1` = **200**.
  - Legal: an integer, a multiple of 4, > 0, and a dash has no floor.
- **`banked_embers`** adds `modify: [{field: cost, flat: 10}, {field: cooldown_ticks, percent: 20}]`.
  More embers cost more.
  - Cost: `(35 + 10) × 1` = **45**.
  - Cooldown: `(200 + 0) × 1.20` = **240**. Legal: 240 is a multiple of 4.
  - **The control:** `percent: 15` would give 230, and the loader refuses it, naming 228 and 232.
- **A pair, to show composition.** Suppose a second, hypothetical `solar_lance` aspect carries
  `damage.amount +10%` and is equipped beside `searing_lance`. Then
  `12 × (1 + (−25 + 10)/100)` = `12 × 0.85` = **10.2**.
  - The percents are **summed**, so it is not `12 × 0.75 × 1.10 = 9.9`. The test asserts the value it
    gets by EXECUTING that expression in Java, not a predicted decimal: 0.85 is not exact in binary,
    and a floating-point result is never written down from reasoning.

### 2.5 The Ability Stone and cast input

**The item.**

- PDC key **`build_stone`** (BYTE, `Keys.buildStone`). It does not collide with `weapon_id`, the dead
  `ability_id`, or the old weapon's id.
- `setMaxStackSize(1)` at the mint, per the standing decision.
- **No `weapon_id`**, so `WeaponFire`, the durability code and every gear scanner ignore it by
  construction.
- **Display name "Ability Stone"** (ruling 2).
- **The material is `ECHO_SHARD`** (ruling 12). The accessories plan's §3.7 already measured it against
  the 26.1.2 villager trade table (wanted by no villager). Its only vanilla use is the recovery-compass
  recipe. Slice 1's ST11 still tries the brewing stand, the furnace fuel slot and the smithing table.
  **Echo shard is also the material of the three universal accessories and of `volley_stone`**, so
  identity can never be material: §2.5.2.
- Lore renders the **current** loadout (Left: X / Right: Y / Q: Z). It is refreshed on every Build-screen
  save and on join.

**Slot and toggle.**

- The `LockedItem` descriptor (§1.3) has an allowed set of **hotbar 0-8** (ruling 2).
- The picker shows only row 9's nine cells: the same `NexusSlotPickerLayout` with the storage rows as
  filler.
- **The default is ON, in hotbar slot 7, and it can be changed in Settings to hotbar slots only**
  (ruling 13). `PlayerProfile`'s boxed nulls resolve to exactly these values.
- **A player with class or element `none` has no loadout.** The stone is still issued if enabled, and
  every press says *"Choose a build in the Nexus"*. This is one rule and one notice. *Rejected: not
  issuing it* — the toggle would then mean two things.

**The input mapping, with each conflict's resolution:**

| press | route | resolution |
|---|---|---|
| **left, air** | `PlayerArmSwingEvent` (main hand) in the one listener, decided **one tick later** by the Q-swing guard (below) | casts Active 1. **A held left click in air is ONE input** — measured: the client does not repeat it |
| **left, block (survival)** | the same swing path (a swing arrives EVERY tick while held, measured) + a new `BlockDamageEvent` arm → **cancel** while the stone is in the main hand | every swing tries the cast (ruling 19), so a held left click on a block re-casts whenever the cooldown allows; **the stone never damages or breaks a block** |
| **left, entity** | the same swing path casts; `onPrePlayerAttack` **cancels** the attack when the stone is held | one cast per swing, no token hit, no vanilla hurt flash |
| **right, air / block** | `onRightClick`: the stone branch goes **second**, after the star and **before** `openHijackedBlock`; **main-hand event only** (a right-click on a block fires once per hand in the same tick, measured, and the off-hand event is ignored); it cancels the event | casts Active 2; a crafting table right-clicked with the stone casts rather than opens (ruling 14). Held: every input tries the cast (ruling 19) |
| **right, entity** | `onNexusGiveToEntity` (widened) cancels **and casts**, main hand only | one cast per input |
| **Q, no screen** | `onNexusDrop` (widened) cancels, `updateInventory`, **records the drop tick**, and **casts the Ultimate** | casts; the stone never leaves the slot; the same-tick swing does not cast Active 1 |
| **Q, screen open** | `onNexusClick` refuses at LOWEST, and **records the drop-attempt tick** for the guard; the drop event never fires | **refuses, never casts** (the clarification under RULINGS); the same-tick swing — which arrives BEFORE the click, measured — does not cast Active 1 either |
| **offhand** | `onNexusSwapHand` (widened) refuses; the swing and right-click paths are main-hand only | never casts |
| **creative** | **hotbar Q** reaches `PlayerDropItemEvent` exactly as in survival (measured), so it casts the Ultimate. **Inventory Q** arrives as `ClickType.CREATIVE` (measured, action `PLACE_ALL`), which `NexusLock.refusesClick`'s existing `CREATIVE` arm refuses once the locked-item predicate is widened; it records the drop-attempt tick like the survival screen path | hotbar Q casts; inventory Q refuses and casts nothing; one creative gate row (ST12) |

**HOLDING AN INPUT — RULING 19: it RE-CASTS whenever the cooldown allows, for left and right click.**

- **Every input tries the cast.** There is no debounce, no gap and no release detection, because none is
  needed: `AbilityService.resolve`'s cooldown is what paces a hold. **Holding cannot cast faster than the
  cooldown**, since each input is simply refused until the cooldown ends.
- **A refused input is SILENT except for one action-bar line** — never chat. The line is throttled
  (`StoneNotice`, below), because a held block-swing tries every tick.
- **What a hold actually delivers, from the spike (§3.1.0.1):**
  - a held right-click: an input every **4** ticks, in air and on a block;
  - a held left-click on a block: a swing every **1** tick;
  - a held left-click in air: **one** swing and nothing more.

  So a held left click re-casts on a block and **not** in air. That is the client's behaviour, not a
  rule of ours, and the gate records it rather than working around it.
- **The multiple-of-4 cooldown rule now does real work** (§2.4.1): a held right-click re-casts on the
  4-tick grid.
- *Withdrawn, by ruling 19:* one cast per press with a debounce gap `G`. The spike showed separate
  right-clicks 1-3 ticks apart, below the held repeat of exactly 4, so no `G` could separate a press from
  a hold. `PressDebounce` is removed from the plan.

**THE Q-SWING GUARD — a swing in the SAME TICK as a stone drop attempt never casts Active 1.**

- **Why it must decide a tick late, measured:** from the hotbar the drop arrives BEFORE the swing; from
  an open inventory the swing arrives BEFORE the drop click, in the same tick. A decision taken at the
  swing would miss the second case.
- **So a stone swing is not cast at once.** The swing handler records its tick and schedules the
  decision one tick later (`Scheduler.onEntityLater(player, ..., 1)`). The deferred task casts Active 1
  only if no stone drop attempt was recorded **in the swing's tick**. The cost is 1 tick (50 ms) of
  latency on Active 1 only.
- **The rule is pure core:** `C/build/SwingGuard.castsActive1(swingTick, lastDropAttemptTick)` returns
  `swingTick != lastDropAttemptTick`. The paper side only records ticks (per player, in memory, cleared
  on quit) and asks it.
- **Why `PlayerArmSwingEvent` rather than the packet listener:** it runs on the main thread in the same
  tick as the drop events it must be ordered against (quoted from `handleAnimate`, §1.7), and CLAUDE.md
  prefers the Bukkit API over packets. `WeaponSwingListener` is untouched.
- **Found while designing this, recorded in §6 and NOT fixed here:** a weapon with a `left_click`
  trigger has the same defect today. Q on a held weapon fires its left-click trigger through
  `WeaponSwingListener`.

**Where the checks live.** Each input resolves the slot's ability id from `BuildService`, derives the
aspects (§1.5), then calls the **existing** `AbilityService.cast(caster, id, aim, castable)` with
`castable` = the current cell's equipped ids. So:

- the cooldown check, the mana check and the deduction are `resolve`, unchanged;
- the cooldown key is the **bare ability id**, shared with `/rpg cast`;
- `Locked` can only mean the loadout changed mid-press.

**THREADING:** every stone handler is a Bukkit event handler on the player's thread, and the deferred
swing decision goes through `Scheduler.onEntityLater`. **Nothing new runs on a Netty thread.**

**Feedback when an ability is on cooldown or short of mana:**

- A `P/build/StoneNotice` on the **action bar**, `QuiverNotice`-style. It is throttled through
  `CooldownTracker` with the keys `__stone_cooldown_notice` and `__stone_mana_notice`.
- `NoticeThrottleKeysTest` gains both keys.
- It names the ability and the seconds left (`OnCooldown.ticksRemaining`).
- **Known collision, stated:** `StatsBarSystem` also writes the action bar periodically, and will
  overwrite the notice. `QuiverNotice` already lives with this. The gate row reads that the notice is
  **visible**, not how long it lasts.

### 2.5.1 `/rpg cast` after kits (ruling 10)

**Two paths, split on one permission: `Permissions.DEV` = `rpg.command.dev`** (`paper-plugin.yml`:
`default: op`). That is the node the other dev instruments check. *`rpg.command.cast` stays as the gate on
the command itself (`default: true`) and is not the split.*

- **With `rpg.command.dev`: ANY registered ability, at any time, with NO cost and NO cooldown.**
  - The existing `AbilityService.cast` cannot express this: `resolve` always checks and triggers the
    cooldown and calls `tryConsume`.
  - So core gains **`AbilityService.castUnchecked(caster, abilityId, aim)`**. It looks the id up, and
    returns `UnknownAbility` or a `Success` **without** touching `CooldownTracker` or `ResourcePool`.
    It is the `fireTrigger` shape without `resolve`.
  - Aspects are **not** applied: a dev cast is of the registered ability as authored.
  - **It must not start a cooldown**, so a dev cast never blocks that player's stone press of the same
    ability.
- **Without it: only the current cell's equipped abilities**, through the normal `cast` →
  `resolve` path, with the cost, the cooldown and the aspects applied. This is the same castable set the
  stone uses (§2.5).
- Tab completion follows the split: every ability id for a dev player, and the equipped ids otherwise.

*(Java refresher for Ben: `castUnchecked` returning a `Success` without calling `resolve` is what "no
cost, no cooldown" means in code — the two checks live only in `resolve`.)*

### 2.5.2 Echo shard: three kinds of item share one material, and identity must never be material

At `bccef7d`, `ECHO_SHARD` is the material of:

- the three universal accessories (`keen_charm`, `mending_charm`, `ward_charm`; `AccessoryDefinition.MATERIALS`);
- the dev weapon `volley_stone`;
- and after slice 1, the Ability Stone.

**Every identity check in the codebase is a PDC key:**

- `isNexus` → `nexus`;
- `AccessoryItems.accessoryId` → `accessory_id`;
- `WeaponItems.weaponId` → `weapon_id`;
- the new `isStone` → `build_stone`.

**Code paths that key on an item's MATERIAL**, from a grep for `getType()` comparisons and `Material.`
constants in `paper/src/main`, each with its verdict for the stone and a Ward Charm:

| path | what it keys on | verdict |
|---|---|---|
| `QuiverAmmo` (planning and `consume`) | `Material.ARROW` | neutral — not echo shard |
| `DefenseModifierItems.vanillaArmorPoints` | the material's vanilla armour value | neutral — echo shard has none |
| `RpgListeners.onRightClick`'s hijack lookup (`hijackedBlocks`) | the clicked **block**'s material | neutral — not an item |
| `AccessoryItems` material resolution | falls back to `ECHO_SHARD` when minting an accessory | minting only; never identity |
| `CraftingMenu`'s matrix signature, `RecipeProbe`, `IngredientLore` | ingredient **material** — this is how vanilla recipes match | **THE ONE REAL MATERIAL DOOR.** Echo shard is a recovery-compass ingredient. Accessories are closed by `CraftMatrixScreen.isGear` (`accessory_id` arm → `CONTAINS_GEAR`). **The stone is not gear, so `isGear` does not close it.** It is closed the way the star (a beacon ingredient) is: **the lock stops it leaving its slot**, so it can never reach a grid. ST16 tries |
| `isSimilar` comparisons (`MenuRouting`, `MenuSafety`, `CraftingMenu`) | the whole meta, PDC included | **safe by construction** — a stone and a charm differ in PDC, so they never stack or merge |

*Alternative, named:* add a `build_stone` arm to `isGear`. That is belt and braces, but it makes "gear"
mean "anything we mint", which `isGear`'s callers (`QuiverAmmo`) do not expect. **Not recommended while
the lock holds.** If ST16 fails, it becomes the fix.

### 2.6 Class/element change

The Build screen is the **only** writer of the cell after slice 2. `/rpg class` and `/rpg element` are
deleted.

| thing | on a cell change |
|---|---|
| **loadout** | the new cell's saved loadout, or **the pool's `default`** if none is saved. The old cell's loadout is **kept** in the build file |
| **fragments' stats** | the next reconcile tick removes the old cell's `fragment:*` sources and adds the new cell's. This is the convergence the accessories rely on, with no event |
| **accessories** | unchanged rule: a class-slot accessory goes inert when the class differs (`AccessorySlots.contributes`), and comes back on return |
| **cooldowns** | **kept.** They are keyed by ability id. Switching away and back cannot reset an Ultimate, and a shared ability keeps its timer across cells. *Alternative: clear on change* — a free Ultimate reset for two clicks |
| **mana** | untouched |
| **weapons** | **nothing is minted or removed** (ruling 6) |
| **the stone's lore** | refreshed |

**A change mid-cast is NOT refused.** A volley windup or a live projectile already carries its own
`Success`, with its derived definition, so changing the cell mid-flight is safe by construction. Slice 3
has a row that proves it (BB7). *The alternative, refusing a change while a cast is live, guards a case
the design has already made harmless.*

### 2.7 Weapon acquisition after kits (ruling 6, ruling 8)

**Interim: admin `/rpg give`, plus the three mage recipes. This is INTERIM, not the replacement.**

Every place a new player could receive a weapon today (§1.2): nothing on join; `/rpg class` / `/rpg
element` (**removed**); `/rpg give` (op-only, stays); crafting (three recipes, all `class: mage`,
stays).

**Known gap, per ruling 8, not a defect:** after slice 2 a non-op Ranger has **no** weapon source, and a
non-op Mage has only the three recipes. The Build screen does not mention weapons.

---

## 3. SLICES, EACH WITH ITS GATE

**The seat's order is kept, with one justified adjustment.** Slice 1 needs a *fixed* loadout to cast
from before storage exists. **Recommendation: slice 1 ships the POOL FILES and their loader, and casts
the pool's `default`.** Pools are pure content plus a loader, and do not depend on kit removal. The kit
files then go in slice 2 with storage, exactly as the seat ordered. *The alternative, a hard-coded
loadout in Java, is banned* (CLAUDE.md: content is data).

Every slice follows these conventions:

- The **core test is written before the paper wiring**, and `./mvnw -pl core test` runs after every
  core change.
- Every gate file declares **game mode per row**, and opens with **R0a** (the boot-log `Build:` line names
  the branch tip, not `-dirty`) and **R0b** (a PowerShell zip scan of the deployed jar for the slice's new
  classes, plus a control class that must be ABSENT).
- **Mutations are named with the test or row each must redden**, and run with
  `-Dmaven.test.failure.ignore=true`, reading each module's report separately (the Slice B harness
  finding).

### 3.1 SLICE 1 — THE ABILITY STONE, CASTING A FIXED LOADOUT

#### 3.1.0 PREREQUISITES — THE SPIKES. **SLICE 1 IS NOT BUILT UNTIL S1 AND S2 ARE READ** (amendment 5)

Each spike is a throwaway branch with **one logging listener**. It is never merged. A figure is recorded
in `GATE-build-stone.md`'s spike section with the sha it ran on.

| spike | question | instrument | outcome that changes the design |
|---|---|---|---|
| **S1 — blocking** | Does a successful Q-drop make the client send an ANIMATION (swing) packet? And does a **cancelled** drop? | log every ANIMATION packet in `WeaponSwingListener` and every `PlayerDropItemEvent`, with the tick; press Q 10× on a droppable item, then 10× on the star (cancelled); survival | **If a swing follows Q:** the left-click branch suppresses a swing that lands within N ticks of a stone drop on the same player. N is **measured**, not guessed. **If not:** no guard, and a gate row pins the absence |
| **S2 — blocking** | **(a) LEFT:** while holding left on a block in survival, how many ANIMATION packets arrive, at what spacing, and does cancelling `BlockDamageEvent` change the stream? **(b) RIGHT (added on the seat's review of `7c2da6c`):** while holding right-click with an echo-shard test item, in air and on a block, how many RIGHT_CLICK `PlayerInteractEvent`s arrive, at what tick spacing, what is the largest gap between two held inputs, and does anything reach the server on release? | the same log, with `PlayerInteractEvent` added; (a) hold left 3 s on stone with a stone-like test item, once uncancelled and once with `BlockDamageEvent` cancelled; (b) hold right 3 s in air, then 3 s on stone, then five separate quick presses, recording each input's tick | it decides whether a held left click is **one cast per press** (a debounce measured from the stream) or **re-casts whenever the cooldown allows** (the Boltor precedent for held input). **Ruling 15 chose one cast per press; the measurements (§3.1.0.1) led to ruling 19, which replaced it with re-cast while held and removed the debounce.** For (b), the gap `G` must exceed the largest held-input gap AND be smaller than the gap between the quick separate presses. **If the two ranges overlap, a press cannot be told from a hold, and the recommendation goes back to Ben before slice 1 builds.** If something does arrive on release, it replaces the gap rule |
| S3 — in slice 1 | Does right-clicking an entity also produce a RIGHT_CLICK_AIR `PlayerInteractEvent`? | log both events with the tick | whether the same-tick guard ever fires (the guard ships either way) |
| S4 — in slice 1 | What do Q and a left click do in CREATIVE? | the same log | recorded in the creative register; no survival row depends on it |

#### 3.1.0.1 THE SPIKE, MEASURED AT `d7b4087` (2026-09-25) — S1, S2(a), S2(b), and S4 answered early

**How it was measured.** The harness was one logging-only file, `SpikeInputLog`, plus MONITOR-priority
handlers in `RpgListeners`. It lived on the throwaway branch `spike/stone-input`, which was never pushed
and has been deleted. It logged one line per input from a player holding an `ECHO_SHARD` in the main
hand, each with `Bukkit.getCurrentTick()`, the game mode and the event's final cancelled state. The
events were: `PlayerAnimationEvent` (and so `PlayerArmSwingEvent`), `PlayerInteractEvent`,
`PlayerInteractEntityEvent`, `PrePlayerAttackEntityEvent`, `BlockDamageEvent`, `BlockDamageAbortEvent`,
`PlayerDropItemEvent` and `InventoryClickEvent`. The boot log read `[Rpg] Build: d7b4087`. Ben played the
steps, and the figures below come from `run/logs/latest.log`. **Nothing was cancelled by the harness**, so
every drop measured is an UNcancelled one.

**S1 — Q:**

- **Hotbar, survival:** 3 drops (ticks 15041, 15241, 15327). **Each was followed by an arm swing in the
  SAME tick**, logged after the drop. No `LEFT_CLICK_AIR` `PlayerInteractEvent` came with those swings,
  but the swing packet (and `PlayerArmSwingEvent`) did.
- **Open inventory, survival:** 1 drop (tick 15471). **The swing, the `DROP`/`DROP_ONE_SLOT` inventory click
  and the drop all landed in one tick, with the swing logged FIRST.**
- *Not measured:* whether a CANCELLED drop is followed by a swing. The guard (§2.5) does not depend on
  it, because it keys on the drop ATTEMPT.

**S2(a) — left click:**

- **Held on a stone block:** one `LEFT_CLICK_BLOCK` and one `BlockDamageEvent` at the start, then **47
  swings over ticks 15747-15792, 1 tick apart** (45 gaps of 1, one pair in a single tick). Then
  **`BlockDamageAbortEvent` at 15793 — a release signal**, 1 tick after the last swing.
- **Held in air:** **one** swing, at tick 23335, and nothing more until the next click 44 ticks later.
  **The client does not repeat a held left click in air.** Nothing arrives on release.
- **Quick separate clicks in air:** gaps of 4, 3, 4, 4, 3, 4, 4, 9, 2, 2, 2, 2, 2, 2, **1**, 2, 2, 2, 11, 9,
  9, 7 and 8 ticks. The smallest gap between separate left presses is **1**.
- *Not measured:* the swing stream with `BlockDamageEvent` CANCELLED. ST3 reads it in play.

**S2(b) — right click:**

- **Held in air:** inputs **exactly 4 ticks apart** (11 inputs at 16066-16106; a second hold, 11 inputs at
  23197-23237, the same).
- **Held on a stone block:** inputs **exactly 4 ticks apart** (12 inputs at 16239-16283), and **each input
  fired TWICE, once for `HAND` and once for `OFF_HAND`, in the same tick.**
- **Quick separate presses:** a first run was exactly 8 apart every time (16435-16467). Later runs had gaps
  of 10, 5, 6, 6, 5, 6, 5, 5, 4, 6, 8, **1**, 3, 2, 2, 2, 2, 2, 2, 2, 3, and then 10, 34, 10, 6, 5, 6, 5, 12,
  6, 5, 11, 10.
- **Release:** nothing arrives, in air or on a block.
- **CORRECTION, kept on purpose:** the first reading of these figures inferred that separate presses also
  land on the 4-tick grid, because the first quick run was exactly 8 apart. **The later runs falsify
  that** — gaps of 1, 2, 3, 5 and 6 occurred. Only a HELD right-click is on the grid. That is why ruling
  19 replaced the debounce.

**S4 — creative, one pass, a control:**

- **Hotbar Q:** a drop plus a same-tick swing (16796, 16806), as in survival.
- **Inventory Q:** a swing, **two `ClickType.CREATIVE` / `PLACE_ALL` inventory clicks** around the drop,
  all in one tick (16785). **That is the creative click type, not `DROP`.**
- **Held left click on stone:** a single tick (16835) with `LEFT_CLICK_BLOCK` **and** `LEFT_CLICK_AIR` and
  one swing. The block breaks instantly, so there is no stream.
- **Held right click in air:** 8 inputs, 4 ticks apart.

**S3** (right-click on an entity) was not run. Its guard is now moot: every input tries the cast
(ruling 19), and a duplicate input is paced by the cooldown like any other.

#### 3.1.1 Files

- **core:**
  - `C/build/CellKey`;
  - `C/build/PoolDefinition`, `C/build/PoolRegistry`;
  - `C/build/LoadoutSlot` (enum `ACTIVE_1`, `ACTIVE_2`, `ULTIMATE`);
  - `C/build/StoneInput` — a pure map from (input, main hand, screen open, gamemode) to a
    `LoadoutSlot` or a refusal;
  - `C/build/LockedSlots` (the conflict rule);
  - `C/build/SwingGuard` (the Q-swing guard, §2.5);
  - `AbilityService.castUnchecked` (ruling 10, §2.5.1).
- **paper:**
  - `P/content/PoolLoader`;
  - `ContentValidator.validatePools`;
  - `P/nexus/LockedItem` (the descriptor);
  - `NexusSlots` parameterised;
  - `P/build/StoneItems` (mint, `isStone`, lore);
  - `P/build/StoneCaster` (slot → id → `AbilityService.cast`);
  - `P/build/StoneNotice`;
  - `Keys.buildStone`, and **`Keys.abilityId` deleted**;
  - `RpgListeners`' six handlers widened, plus the `onSwing`, `onRightClick`, `onPrePlayerAttack` and new
    `BlockDamageEvent` arms;
  - `SettingsMenu`, `SettingsMenuLayout` (two cells);
  - `NexusSlotPickerMenu` (hotbar-only mode, the reserved branch);
  - `S/PlayerProfile` + `ProfileMigrations` (the two boxed fields, v5);
  - `RpgCommand.cast`: split on `Permissions.DEV`. A dev player uses `castUnchecked` over every id; everyone else is limited to the current cell's pool-default loadout (slice 2 swaps in the saved loadout);
  - `ProfileService.setStoneSlot` / `setStoneEnabled`.
- **content:** `content/builds/ranger_fire.yml`, `content/builds/mage_fire.yml` (defaults only, §4).
- **CLAUDE.md** + `.claude/rules/standing-decisions.md`: `ability_stone` leaves the deletion set (§1.4).

#### 3.1.2 Unit tests, core first

- **core:**
  - `StoneInputTest`: the whole grid of input × hand × screen × gamemode.
  - `SwingGuardTest`: a swing in the same tick as a drop attempt does not cast; one tick before or
    after does; no drop ever recorded casts.
  - `LockedSlotsTest`: the two items can never share a slot after any sequence of picks and toggles;
    stone picks outside 0-8 are refused.
  - `PoolRegistryTest`: composite key; duplicate refused.
- **storage:** `PlayerProfileMigrationTest` gains a v4 → v5 case (the boxed nulls read as the defaults).
- **paper:**
  - `PoolLoaderTest`: the bundled pools load; a default outside the lists is refused; an ability in both
    lists is refused.
  - `NexusWiringSignatureTest` still green, with the same names.
  - `NoticeThrottleKeysTest` gains the keys.
  - `SettingsMenuLayoutTest` gains the cells.
  - A test that no `Keys` field contains `ability`.
  - **core** `AbilityServiceTest`: `castUnchecked` returns `Success` for any registered id, leaves `CooldownTracker.ticksRemaining` at 0 and the pool's mana unchanged, and a normal `cast` right after it is NOT `OnCooldown`.
  - **paper** `StoneItemsTest`: a minted Ward Charm is not `isStone`; a minted stone has no `accessory_id` and `Accessories` decodes it as nothing; both are `ECHO_SHARD`, and they are not `isSimilar`.

**Mutations owed:**

| mutation | must redden |
|---|---|
| delete the picker's reserved branch | `LockedSlotsTest` + row ST7 |
| swap the converge order | `LockedSlotsTest` |
| delete the Q-in-screen refusal | `StoneInputTest` + ST5 |
| delete the `BlockDamageEvent` cancel | ST3 |
| delete the `onPrePlayerAttack` cancel | ST4 |
| `castUnchecked` calls `resolve` | `AbilityServiceTest` + ST14 |
| `isStone` keys on `Material.ECHO_SHARD` | `StoneItemsTest` + ST16 |
| remove the Q-swing guard (`castsActive1` always true) | `SwingGuardTest` + ST13 |
| cast on the off-hand right-click event too | ST6 (two casts' worth of mana per click) |

#### 3.1.3 Gate — `GATE-build-stone.md`. **Game mode: SURVIVAL, except ST12**

| row | prediction, written now | risk |
|---|---|---|
| R0a / R0b | `Build:` names the tip; jar has `StoneItems`, `LockedItem`, `PoolLoader`; `WeaponSwingListener` present; control ABSENT | wrong jar |
| ST0 | `content/weapons/ability_stone.yml` absent from the jar; `/rpg give ability_stone` refused as unknown | the deletion |
| ST1 | a Fire Ranger joins: the stone is in the default hotbar slot, one item, not stackable onto a second | mint, stack 1 |
| ST2 | left in air: Active 1 casts (its visual); again at once: the action-bar notice names the ability and seconds | cast + cooldown feedback |
| ST3 | hold left on a stone block for 5+ s: **the block shows no crack and does not break**; Active 1 re-casts each time its cooldown ends (the ST18 count) | the block, ruling 19 |
| ST4 | left on a zombie: Active 1 casts once; the zombie takes **no** 0.01 token hit and shows no hurt flash | attack path |
| ST5 | Q with no screen: the Ultimate casts and the stone stays; Q with the inventory open on the stone: **nothing casts**, the stone stays | Q both ways |
| ST6 | right in air: Active 2; right on a crafting table: Active 2 casts and the table does **not** open (ruling 14) | order vs hijacked blocks |
| ST6b | right on a villager: one cast (count visuals), no trade screen | entity + S3 echo |
| ST7 | picker: storage slots are not offered; the star's slot shows "reserved"; choosing slot 2 moves the stone | hotbar-only, reserved |
| ST8 | Settings toggle OFF removes the stone and frees its slot; ON refuses an occupied slot and names it | toggle |
| ST9 | death and respawn: one stone, in its slot; rejoin: same | re-issue |
| ST10 | a `none` player presses left: the "Choose a build" notice, no cast | no loadout |
| ST11 | the stone in brewing stand / furnace fuel / smithing table: refused or not consumed; no villager trade | inertness |
| ST12 | **CREATIVE**: hotbar Q casts the Ultimate and not Active 1; inventory Q over the stone refuses, casts nothing, and the stone stays in its slot | creative register |
| ST13 | **Q, both ways (S1):** hotbar Q — the Ultimate casts and **Active 1 does NOT** (no Active 1 visual, and no Active 1 cooldown on the next left click); inventory Q over the stone — **nothing** casts, neither the Ultimate nor Active 1, and the stone stays | the Q-swing guard |
| ST17 | **HOLD right-click** in air for 5+ s (timed): Active 2 casts at once, then again each time its cooldown ends; **count = floor(hold ticks / cooldown ticks) + 1**; refused inputs show only the action-bar line, and chat stays empty | ruling 19, right |
| ST18 | **HOLD left-click on a stone block** for 5+ s (timed): Active 1 re-casts the same way — **count = floor(hold / cooldown) + 1**; the block is untouched (ST3); action bar only. Then hold left in AIR for 5 s: **one** cast (the client sends one swing, §3.1.0.1) | ruling 19, left |
| ST14 | **an operator** (`rpg.command.dev`): `/rpg cast void_slash` (in no pool) casts; mana unchanged (HUD); cast it again at once: it casts again; a stone press of an equipped ability right after a dev cast of it is **not** on cooldown | ruling 10 |
| ST15 | **a non-op**: `/rpg cast void_slash` is refused as not in the loadout; `/rpg cast <Active 1>` casts, spends mana and starts the cooldown the stone then reports; tab completion lists only the loadout | ruling 10 |
| ST16 | **echo-shard identity**: carry a Ward Charm and the stone. Left, right and Q with the **charm** held do nothing stone-like (no cast, and Q drops it). The charm equips into an accessory slot; the stone is refused there, and the stats sheet shows no accessory line for it. Settings toggle OFF removes the stone and **not** the charm. The stone cannot be moved into a crafting grid (the lock), so no recovery compass. The charm and stone never stack or merge on a cursor | §2.5.2 |

**THE STAR IS REGRESSION-GATED AT THE SLICE TIP (amendment 4).** The `LockedItem` generalisation touches
`NexusSlots`, `converge`, the six handlers, `SettingsMenu` and the picker. So every existing star row
that reads those is **re-run at the slice tip, beside the stone rows**, and recorded in
`GATE-build-stone.md` under *STAR REGRESSION*, with the sha:

- **GATE-nexus.md, the lock:**
  - ROW 1 (the cursor at death, 1a-1c);
  - ROW 2 (item frame, armour stand);
  - ROW 3 (the performed routes);
  - ROW 4 (the coordinate conversion, both views);
  - ROW 6 (the silent refusal, 6.1-6.5, including 6.4′);
  - ROW 8 (creative duplication, 8a-8f; 8e and 8f are the ship condition).
- **GATE-nexus.md, convergence:**
  - ROW 5 (join convergence, 5a-5d);
  - ROW 20 (the ordinary join);
  - ROW 21 (the race);
  - ROW 22 (migration on an old file, re-read on v5);
  - ROW 23 (a corrupt profile still gets a star);
  - ROW 24 (death and respawn).
- **GATE-nexus.md, Settings and the picker:**
  - ROW 25 (the torch lore);
  - ROW 26 (choosing a slot moves star and lock);
  - ROW 27 (back vs close);
  - ROW 28 (the two refusals);
  - ROW 29 (survives a rejoin);
  - ROW 30 (the hub control).
- **GATE-nexus.md, slice 7 (the star opens the hub):** ROWS 49-57, especially ROW 53 (Q, F and
  number-key still refuse) and ROW 56 (the creative control).
- **GATE-nexus.md, slice 8 (any of 36 slots):** ROWS 58-65 — **this proves the star's range was not
  narrowed to the stone's**.
- **GATE-nexus.md, slice 10 (the Settings hub):**
  - ROW 80 (`/menu` with the star disabled);
  - ROW 81 (screen labels);
  - ROWS 82-85 (the picker's clones: nothing leaves);
  - ROWS 86-91 (toggle off frees, join and respawn do not re-mint, toggle on refuses occupied, a pick
    does not re-enable, the control).
- **GATE-accessories-b.md:** B9 (the star cannot enter an armour or accessory slot).

**Rows NOT re-run, each with its reason:**

- ROWS 9-19: hub rendering, the stats head. The hub changes in slice 3, and ROW 30 covers it here.
- ROWS 31-47: stations.
- ROWS 66-77: player level.
- ROWS 92-114: the vault.

None of these reads a `NexusSlots`, `converge` or handler path.

**Deletes:** `ability_stone.yml`, `Keys.abilityId`, `bundledAbilityStoneContentLoads`, the golden-lore
block, and the id in `WeaponLoreTest`. The 13 comment lines and the content comments are **edited**.

#### 3.1.4 AS BUILT — where slice 1 differs from the lines above, each with its reason

The gate file is `GATE-build-stone.md`, and its rows and mutation results are the record. What moved:

1. **`StoneItemsTest` became `StoneIdentityTest`.** A test that mints a stone and a Ward Charm is not
   writable: `new ItemStack` throws without a server and there is no MockBukkit (`NexusSlots`' javadoc
   records the same limit). The test pins the key by reflection and reads `isStone`'s body. The
   item-level half is ST16, played. Mutation M6 (material instead of key) is killed by it.
2. **`ConvergePlan` was added (core), and it was not in this plan.** `NexusSlots.converge`'s javadoc
   carried a named debt, the plan/execute split, whose trigger was "the next slice that opens this method
   for any other reason". This slice opened it. The split is what finally kills `MUTWELDDEFAULT` (M8),
   which had left the whole suite green.
3. **Mutation "swap the converge order" was predicted to redden `LockedSlotsTest`. It SURVIVED, and the
   prediction was wrong, not the code.** The order is invisible to unit tests, and harmless while
   `LockedSlots` keeps the two targets distinct, which is what `LockedSlotsTest` actually proves.
4. **`StoneInput` takes no game mode.** The spike found creative's inputs arrive as the same events as
   survival's (§3.1.0.1), and creative's in-screen Q is a screen input like survival's. A parameter no
   rule reads would be a hollow row in the grid.
5. **Ruling 14's "sneaking still opens" is implemented, but its recommendation's parenthetical was wrong.**
   It called that "the existing rule in `openHijackedBlock`". In fact the existing rule is the INVERSE:
   for a weapon, sneaking is the escape hatch that lets it CAST. The stone's branch therefore calls the
   block's opener directly. ST6c reads it.
6. **The off-hand half of a stone right-click is CANCELLED, not merely ignored.** Measured, a right-click on
   a block fires once per hand. Ignoring the off-hand event would still let vanilla place an off-hand
   torch on every cast. ST6 reads it.
7. **The stone's left click uses `PlayerArmSwingEvent`, not `WeaponSwingListener`** (§2.5's Q-swing guard).
   `WeaponSwingListener` is untouched.
8. **The no-loadout notice names `/rpg class` and `/rpg element`,** not "the Build screen", because the
   Build screen is slice 3.
9. **`/rpg class` and `/rpg element` now also re-render the stone's lore.** Slice 2 deletes both commands,
   and the refresh moves to the Build screen.
10. **`ScorchContentInvariantTest`'s site count went 13 -> 14.** It is -1 for the deleted weapon's ember
    burst and +2 for the placeholder Ultimates. The first run read 15 from a stale `target/`, which
    `ContentFreshnessTest` named on the same run; `./mvnw clean` cleared it.

### 3.2 SLICE 2 — KIT REMOVAL AND BUILD STORAGE

- **Files:**
  - `S/BuildRepository`, `FileBuildRepository`, `PlayerBuild`, `Loadout`, `BuildMigrations`;
  - `P/build/BuildService`;
  - `PlayerProfile.withCell`, `ProfileService.setCell`;
  - `StoneCaster` reads `BuildService` (falling back to the pool default when no loadout is saved);
  - `RpgCommand.cast`'s non-dev path reads the saved loadout instead of slice 1's pool default (§2.5.1);
  - `AbilityLoader` ignores `archetype` with a warning;
  - `AbilityDefinition.archetypeId` deleted;
  - `WeaponLoader`'s `"none"` deleted.

  **Temporary dev instrument:** `/rpg build set <slot> <id>` and `/rpg build cell <class> <element>`, gated
  like the old dev commands. They are marked `// DEV: delete when GATE-build-screen.md passes` — the
  accessories dev-command precedent, deleted in slice 3.
- **Unit tests:**
  - **storage:** `PlayerBuildTest` (per-cell map; wrong list lengths refused; a duplicate fragment keeps the first and blanks the rest, ruling 11), `BuildMigrationsTest`,
    `FileBuildRepositoryTest` (round trip, atomic write).
  - **core:** `LoadoutResolutionTest` (saved vs default; an id missing from the pool reads empty).
  - **paper:** the rewritten `ProfileServiceTest` / `PlayerProfileMigrationTest` cases.
- **Mutations owed:**

  | mutation | must redden |
  |---|---|
  | resolve from the default even when a loadout is saved | `LoadoutResolutionTest` + BS3 |
  | drop the per-cell key (one loadout per player) | `PlayerBuildTest` + BS4 |
  | leave `castable` = `unlockedAbilities` | BS6 |

- **Gate — `GATE-build-storage.md`, SURVIVAL:**

  | row | prediction | risk |
  |---|---|---|
  | R0a / R0b | jar has `FileBuildRepository`; `KitLoader.class` **ABSENT** | wrong jar |
  | BS1 | boot log's Loaded line has **no** kits count; `/rpg class` and `/rpg element` are unknown commands | deletion |
  | BS2 | a fresh player receives **no** weapon anywhere (join, dev cell set) | ruling 6 |
  | BS3 | dev-set Active 1 to `solar_lance`: left casts the lance; restart; still the lance | persistence |
  | BS4 | cell → mage, cell → ranger: the ranger loadout is restored | per-cell |
  | BS5 | Ultimate on cooldown, switch cell and back: still on cooldown | cooldowns kept |
  | BS6 | `/rpg cast` of an ability not in the loadout: refused for a non-op; a dev player casts it free and with no cooldown (ruling 10, already gated in slice 1 as ST14) | castable |
  | BS7 | hand-corrupt `builds/<uuid>.json`: the store is unavailable, SEVERE logged, the stone falls back to the default loadout and says so | structural fault |
  | BS8 | an old profile with `unlockedAbilities` and a kit class loads, and its cell is kept | migration |

- **Deletes:** everything in §1.2 marked DELETE; both kit files; `archetype:` from every ability file; the
  duplicate-weapons defect (with its code). `NEXT.md`'s parked `/kit` item is closed.

#### 3.2.1 AS BUILT — where slice 2 differs from the lines above, each with its reason

The gate file is `GATE-build-storage.md`.

1. **`/rpg class` and `/rpg element` are KEPT, weapon-free** (the clarification under RULINGS). They call
   `ProfileService.setCell` and grant nothing, and `grantWeapons` is deleted. So the only dev instrument
   is `/rpg build set <ultimate|active1|active2> <id>`, marked `// DEV: delete when GATE-build-screen.md
   passes`. `/rpg class` now accepts the POOLS' classes, where it used to accept the kits'. BS1 changes
   to read "no weapon granted", not "unknown command".
2. **`BuildMigrationsTest` is folded into `FileBuildRepositoryTest`.** A newer schema is refused and an
   unstamped file is stamped to 1, both through the real load path. A separate class would re-test the
   same two lines without the Gson round trip.
3. **`ContentValidator.validateKits` became `validatePools`**, and it keeps only the element check. The
   kit check's ability and "grants nothing" halves are now REFUSALS in `PoolLoader` and
   `PoolDefinition` (slice 1), not boot warnings.
4. **`LoadoutResolution`'s rule for a saved loadout is per slot: the saved id if the pool still offers it
   in that ROLE, else EMPTY.** It never falls back slot by slot to the default. A player who saved a
   build gets their build, with a visible hole. **Its first test run found a real crash:** the pool's
   `List.copyOf` lists throw on `contains(null)`, and a saved empty slot is null. The check is now
   null-safe, and `savedEmptySlotsStayEmpty` (mutation S3) holds it.
5. **An empty slot has its own action-bar line** (`StoneNotice.emptySlot`, a 4th stone throttle key;
   `NoticeThrottleKeysTest` 11 -> 12).
6. **An unusable build store casts the pool default, and the stone's lore says so** ("Build unavailable
   -- casting the default."). A still-loading build also reads as nothing-saved, for the join-time
   window only. The lore is re-rendered when the load settles.
7. **Stale `run/.../content/kits/*.yml` are named once at boot and NEVER deleted**
   (`RpgPlugin.warnRetiredKitFiles`). Nothing reads that directory now, so they are inert. A plugin
   deleting an operator's files on boot is the worse surprise, and `--refresh-content` clears them on a
   dev server.
8. **`archetype:` is ignored with ONE warning per load naming every file**, and never refused. An old
   deployed copy must not lose an ability over a key that means nothing now.
9. **The "leave castable = unlockedAbilities" mutation is not runnable as written.** `RpgCommand.cast`
   stopped reading `unlockedAbilities` in slice 1 (ruling 10). What slice 2 adds is that the field is
   written EMPTY on every cell change (`withCell`), and mutation S4 guards that. BS6 reads the castable
   set in play.
10. **A harness defect, recorded because it is the "check that did not run" shape.** The first mutation
    run had a stray line that APPLIED each mutation before the pristine copy was taken. So all seven
    were left in the tree, the "pristine" copies were mutated, and no test ran. It was caught by
    `APPLY FAILED` on every row. The code was committed (`72c02e2`), so the diff was read (exactly the
    seven marker lines) and the files were restored from the commit. The harness line was removed and
    the run repeated. All seven were then killed, each restored with `cmp` identical and zero markers.

### 3.3 SLICE 3 — THE BUILD SCREEN: CLASS, ELEMENT, ULTIMATE AND ACTIVES

- **Files:**
  - `P/menu/BuildMenu` + `BuildMenuLayout` (54 slots, back 48, close 49), with sub-pickers for class,
    element, ultimate, active 1 and active 2;
  - `NexusMenuLayout` (`BUILD_SLOT = 21`, `PAINTED_SLOTS` 9 → 10), `NexusMenu` render + click;
  - `NexusMenuLayoutTest`;
  - core `BuildRules` (pure: which choices are legal given the pools; picking an ability already in the
    other Active slot **swaps** them).
  - **The dev command is deleted.**
- **Layout (ruling 16):** row 1 holds class and element; row 2 holds the Ultimate
  and Actives 1 and 2, labelled "Q", "Left" and "Right"; row 3 holds the two aspect cells; row 4 holds the
  four fragment cells. Rows 3-4 render **"Coming in a later update"** barrier panes until slices 4 and 5.
  Every icon is a **rendered clone**: `inputSlots()` is empty and `returnedSlots()` is `Set.of()`, the
  picker's anti-dupe rule.
- **Button:** slot 21, **ungated** like Equipment (ruling 16).
- **Unit tests:**
  - **core:** `BuildRulesTest` (only pool members offered; the swap; the class list = pools' classes;
    the element list = elements with a pool for the class, i.e. **FIRE only** at ship, ruling 7).
  - **paper:** `BuildMenuLayoutTest`, `NexusMenuLayoutTest` (10 painted; 21 is Build).
- **Mutations owed:**

  | mutation | must redden |
  |---|---|
  | offer every registered ability | `BuildRulesTest` + BB4 |
  | allow the same ability in both Actives | `BuildRulesTest` + BB5 |
  | render a real item instead of a clone | BB8 |

- **Gate — `GATE-build-screen.md`, SURVIVAL:**

  | row | prediction |
  |---|---|
  | R0a / R0b | as usual, plus `BuildDevCommand` **ABSENT** |
  | BB1 | the hub shows Build at 21, directly left of Equipment, at level 1 |
  | BB2 | a `none` player: class offers Ranger and Mage; element offers **Fire only** |
  | BB3 | choose Ranger + Fire: the stone's lore shows the default loadout |
  | BB4 | the Active picker offers exactly the pool's actives — no Mage-only ability |
  | BB5 | pick Active 2's ability for Active 1: the two swap |
  | BB6 | switch to Mage: the Ranger choices are kept (switch back) |
  | BB7 | fire a `solar_grenade` and switch cell mid-flight: it lands with its original effects (§2.6) |
  | BB8 | shift-click, number-key, F and double-click on any icon: nothing leaves (GATE-nexus ROWS 83-85's method) |
  | BB9 | the Equipment button and screen are unchanged (the control) |

- **Deletes:** `/rpg build` (the dev command).

### 3.4 SLICE 4 — FRAGMENTS

- **Files:**
  - `C/build/FragmentDefinition`, `FragmentRegistry`, `FragmentSlots` (`COUNT = 4`);
  - `C/build/StatSourceBound`;
  - `AccessoryNegatives` re-pointed at the combined bound;
  - `P/content/FragmentLoader`;
  - `P/health/FragmentContributions`;
  - `startReconcileLoop`'s n-way merge;
  - the stats-sheet "Fragments" line;
  - the Build screen's row 4;
  - `content/fragments/*.yml` (§4).
- **Unit tests:**
  - **core:** `StatSourceBoundTest` — `8 × |amount| < base` at the boundary both sides, and **the pair
    case: 4 accessories and 4 fragments each at the bound cannot reach the clamp**. The existing
    `AccessoryNegativesTest` is re-pointed.
  - **paper:**
    - `FragmentLoaderTest` (whitelist, `class_damage` refused, negatives);
    - `AccessorySourceKeysTest` (the `fragment:` prefix is disjoint);
    - a test that merges three maps and asserts **both** accessory and fragment keys survive one reconcile;
    - **every shipped accessory still loads under the 8× bound** — if this reddens, stop and re-put
      to Ben (§2.3).
- **Mutations owed:**

  | mutation | must redden |
  |---|---|
  | reconcile fragments in a second call | the merge test + BF2 |
  | leave `AccessoryNegatives` at `COUNT` | `StatSourceBoundTest`'s pair case |
  | drop the cell gate (apply every cell's fragments) | BF4 |

- **Gate — `GATE-build-fragments.md`, SURVIVAL:**

  | row | prediction |
  |---|---|
  | R0a / R0b | as usual |
  | BF1 | slot a +HP fragment: max HP rises within 5 ticks; the stats sheet shows a Fragments line |
  | BF2 | wear a +HP accessory too: **both** sources present after 20 ticks |
  | BF3 | a fragment already slotted is not offered for a second slot, and a hand-edited build file with a duplicate loads it ONCE and logs it (ruling 11) |
  | BF4 | switch to Mage: the Ranger fragments' stats are gone; back: they return |
  | BF5 | restart: the fragments and their stats are kept |
  | BF6 | the boot log shows every shipped accessory loaded (the bound did not refuse one) |

- **Deletes:** row 4's "coming later" panes.

### 3.5 SLICE 5 — ASPECTS

- **Files:**
  - `C/build/AspectDefinition` (id, displayName, description, target, `addOnHit`, `addOnCast`,
    `List<NumberChange> modify`);
  - `C/build/AspectField` — the §2.4.1 whitelist as an enum. Each value carries its type (int or double),
    its legality rule, and the effect variant it addresses;
  - `C/build/NumberResolution` — the formula `(base + Σ flat) × (1 + Σ percent/100)` plus the per-field
    legality check. It is pure, and it returns either the resolved value or a refusal naming the nearest
    legal neighbours;
  - `C/build/AspectApplication.derive` (pure, memoised): **modify first, append second**;
  - `P/content/AspectLoader`. It reuses `AbilitySchema.parseEffects` / `parseCastVisuals` and, after
    the pools load, runs the single-aspect and same-target PAIR resolution of §2.4.1;
  - `ContentValidator` (the target exists and is in the aspect's pool);
  - `StoneCaster` swaps the derived definition into `Success`. So does the non-dev `/rpg cast`; the dev
    `castUnchecked` does not (§2.5.1);
  - the Build screen's row 3, with the inactive rendering, and ability-icon lore rendered **from
    `derive`'s output** (resolved value, with the base in gray);
  - `content/aspects/*.yml` (§4), with the §2.4.1 `modify` blocks.
- **Traced before `status.duration_ticks` ships:** each status kind's use of `durationTicks` (`freeze`,
  `rooted`, `soaked`, the `potion` kind). The findings are recorded in the gate file, and each kind that
  traces continuous is added to the field's allowed set. Until then the loader refuses the field on it.
- **Unit tests:**
  - **core `AspectApplicationTest`:**
    - it appends and never prepends;
    - the id is kept;
    - `headlineDamage` is unchanged by an append;
    - two aspects in slot order;
    - an inactive aspect contributes to neither the appends nor the sums;
    - the same inputs → the same (memoised) record;
    - **`modify` never touches an appended effect** (A's −25% leaves B's appended burst at its authored
      number);
    - `damage.amount` reaches nested `Damage` inside `burst`, `area` and `throw_embers.burst`.
  - **core `NumberResolutionTest`, the whole grid:**
    - flat only, percent only, both;
    - two aspects (the sums commute: A+B = B+A);
    - the §2.4.1 pair example **computed by executing the expression**, and asserting the result is not
      the multiplicative 9.9;
    - `cooldown_ticks`: 240 legal, 230 refused and naming 228 / 232, a value under a Volley floor
      refused, 0 refused;
    - `area.duration_ticks`: 110 on interval 20 refused, 120 legal;
    - `cost`: −1 refused, 0 legal;
    - `damage.amount`: 0 refused.
  - **paper `AspectLoaderTest`:**
    - a target outside the pool refused;
    - a non-visual in `add_on_cast` refused;
    - a field off the whitelist refused, naming it;
    - **a `modify` matching zero effects in the target refused** (`knockback.strength` on `solar_lance`);
    - `status.duration_ticks` on scorch refused;
    - an illegal PAIR refused though each aspect alone is legal;
    - the three §2.4 / §2.4.1 examples load.
  - **paper `BuildLoreTest`:** an ability icon with `searing_lance` equipped reads `Damage: 9` and shows
    the base `12`, rendered from the same `derive` call as the cast.
- **Mutations owed:**

  | mutation | must redden |
  |---|---|
  | prepend instead of append | `AspectApplicationTest`'s `headlineDamage` case |
  | drop the inactive check | `AspectApplicationTest` + BA4 |
  | derive into the global registry | BA5 (another player casts the base ability) |
  | append first, modify second | `AspectApplicationTest`'s appended-effect case + BA7 |
  | multiply the percents instead of summing | `NumberResolutionTest`'s pair case |
  | round instead of refuse | `NumberResolutionTest`'s 230 case |
  | drop the zero-match refusal | `AspectLoaderTest`'s `knockback.strength` case |
  | drop the pair check | `AspectLoaderTest`'s pair case |
  | render the lore from the base definition | `BuildLoreTest` + BA8 |

- **Gate — `GATE-build-aspects.md`, SURVIVAL:**

  | row | prediction |
  |---|---|
  | R0a / R0b | as usual |
  | BA1 | `searing_lance` equipped: the lance's hit shows a detonation and damages a second mob within 2.5 blocks |
  | BA2 | `cinder_wake`: a burning patch at the **start** of the dash, not the end |
  | BA3 | `banked_embers`: five embers, not three |
  | BA4 | **the target unequipped (amendment 2):** `searing_lance` equipped, `solar_lance` swapped out of both Actives: the Build screen shows the aspect **Inactive — requires Solar Lance**; nothing changes on any cast, **including the lance's own numbers when re-equipped in another slot while the aspect is inactive**; re-equip the lance: BA1 holds again with no other action |
  | BA5 | a second player without the aspect casts `solar_lance`: **no** detonation, and its hit is the base 12 (per player, not global) |
  | BA6 | `cinder_wake`: Ember Step's cooldown, read off the action-bar notice on an immediate re-press, is 10.0 s (200 ticks), not 8.0 s |
  | BA7 | `searing_lance`: the lance's direct hit is **9**, and the appended burst still deals **4** (the damage popup, or the mob's HP change, per hit) |
  | BA8 | the Build screen's Solar Lance icon reads `Damage: 9 (12)`; `banked_embers` equipped: Rekindle's icon reads cost `45 (35)` and cooldown `12.0 s (10.0 s)` |
  | BA9 | an operator's `/rpg cast solar_lance` with `searing_lance` equipped: the base lance (12, no detonation), free, no cooldown (ruling 10) |

- **Deletes:** row 3's "coming later" panes.

---

## 4. CONTENT NEEDED, PER SLICE

**Minimum: Fire Ranger and Fire Mage.** "Ben" means Ben must design it (name, numbers, feel).
"Placeholder" means the slice can ship with it, marked `# PLACEHOLDER — Ben designs` so a grep finds
them.

| slice | content | Fire Ranger | Fire Mage | who |
|---|---|---|---|---|
| 1 | pool files with a `default` | default: Active 1 `rekindle`, Active 2 `solar_lance`, Ultimate **placeholder** | default: Active 1 `ember_step`, Active 2 `solar_grenade`, Ultimate **placeholder** | defaults: **placeholder**, from shipped abilities |
| 1 | **Ultimates** (none exist) | 1 | 1 | **Ben.** Slice 1 ships a placeholder Ultimate per cell, built only from existing `CastSpec`/`EffectSpec` with long cooldowns, so Q can be gated |
| 2-3 | **Actives**, ≥ 2 per cell, more for a choice | have: `rekindle`, `solar_lance`, `arc_surge` (nature, today's kit grant) | have: `solar_grenade`, `solar_lance`, `ember_step` | **Ben** for any new one. Slice 3's BB4 needs the two pools to **differ**, and today they share `solar_lance` — that is fine, since BB4 reads a Mage-*only* ability |
| 3 | a choice of Ultimates (≥ 2 per cell) | 2 | 2 | **Ben**. The screen ships with one each |
| 4 | **Fragments** | 4+ | 4+ | numbers **Ben**; the slice can ship **placeholders** built on the four universal stats (max HP, crit chance, crit damage, health regen) |
| 5 | **Aspects** | ≥ 2 | ≥ 2 | **Ben.** §2.4's three examples are placeholders the slice can ship (2 Ranger: `searing_lance`, `banked_embers`; 1 Mage: `cinder_wake` — plus one more Mage placeholder on `solar_grenade`) |

**`arc_surge` and the TEMP fixtures:** `solar_grenade` still carries the `rooted` TEMP fixture in its
burst (the memory `temp-status-fixtures-owe-removal`). Pooling it makes a TEMP status part of a player
build. **That removal is owed by the content pass, and slice 1's PR body names it** rather than fixing
it here.

---

## 5. OPEN QUESTIONS FOR BEN

**Q1-Q10 were ruled on 2026-09-25**: they are rulings 9-18 in the RULINGS section, and are not repeated
here. One question is PARKED. It is unanswered, and nothing in slices 1-5 waits on it.

1. **PARKED: should `arc_surge` (a NATURE ability) stay in the Fire Ranger pool?**
   - Today it is the Fire Ranger kit's only ability, and §2.2's example pool carries it forward.
   - **Recommendation: drop it.** `rekindle` and `solar_lance` fill both Active slots, and they are
     slice 1's default loadout anyway. A nature ability in a fire cell makes "the cell's element" mean
     nothing.
   - Until ruled, `ranger_fire.yml` keeps it, as the kit does today (§2.2's example). It is in no
     default loadout, so the default loadout does not depend on the answer, and dropping it is one line.

---

## 6. FINDINGS THIS PLAN CREATES IN OTHER FILES — RECORDED, NOT FIXED HERE

1. **`Keys.abilityId` ("ability_id") is dead**: declared, with no reader or writer. It is deleted in
   slice 1 (§1.4).
2. **The duplicate-weapons defect is wider than PLAN-accessories §8.1**: `chooseElement` also mints, and a
   class switch never takes weapons back. It is deleted with `grantWeapons` in slice 2 (§1.2).
3. **`archetype:` is dead and contradicts the kits** for `solar_grenade` and `solar_lance` (`hunter`, in
   the Mage kit). It is retired in slice 2 (§2.2).
4. **`arc_surge` is `element: nature`** in the fire Ranger kit. That is not a defect, but the brief's
   "Fire abilities" list was wrong about it (§1.1).
5. **Stale kit files survive in `run/`**: `saveResource(path, false)` never deletes, so they are inert
   after slice 2 (§1.2).
6. **`QuiverAmmo`'s javadoc says it is "THE ONLY GameMode READ"**, and `EquipmentMenu` also reads it.
   Stale prose.
7. **`ProfileService.setStarEnabled`'s javadoc sits above `setVaultMigrated`.** Misplaced prose.
8. **`GATE-gearscore.md`'s three "PERMANENT" notes on `ability_stone`** become moot in slice 1, and their
   content-comment halves are edited there.
9. **Q on a held WEAPON also fires its `left_click` trigger.** Measured: every Q-drop comes with a
   same-tick arm swing (§3.1.0.1). `WeaponSwingListener` reads that swing as a left click. The stone is
   guarded (§2.5); weapons are not. Recorded while building slice 1, and not fixed there.
