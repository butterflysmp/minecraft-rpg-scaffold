# PLAN — LEGACY PORT: OLD WEAPONS AND MOBS FROM cfde822 (SURVEY)

**Phase 1: SURVEY ONLY. No Java, no content, no boot, nothing committed.** Ben picked **6 weapons and 5 mobs** on
2026-09-29 (RULINGS, below), and this revision surveys exactly those 11. It also proposes a build order for each
set. **Ben rules every number.**

**Read against:**
- master **`2d60e9a3`** (#167, mob scaling F20);
- `origin/feat/melee-m1` at **`e305ab3a`** (PR #168, unmerged);
- the old project `CreaperCrusher/Butterfly-SMP` at **`cfde822`**, package `org.example1.butterflysmp`.

**Citations** name a CLASS and a METHOD or a SECTION, never a line, and that includes the old repo. The short paths
are `PLAN-build-system.md`'s: `P/` = `paper/src/main/java/io/github/butterflysmp/rpg/paper/`, `C/` =
`core/src/main/java/io/github/butterflysmp/rpg/core/`, `content/` = `paper/src/main/resources/content/`.

**The brief:** *"Moving more content from the old project into this one (old weapons and mobs)."* It came from the
seat, and it is not committed.

**HOW TO READ A NUMBER IN THIS FILE:**
- A number labelled **cfde822** is a FACT about what the old code did. It is recorded, **never adopted**.
- A number this repo already ships says where it came from.
- Every number a port would author reads **`___ (Ben)`**.

**Two things are true of EVERY cfde822 number below, so neither transfers as-is:**
- **Old weapon damage scaled by item level.** `GearScore.scalePower(base, GS)` gave 1.0× at GS 1 and 12.05× at
  GS 100. So every cfde822 weapon damage figure is its **GS-1 base**.
- **Old mob HP was absolute**, set by `MobSpawnHelpers.applyVanillaMaxHp`. There was no ×5 and no gear score. Old
  cooldowns were milliseconds, converted here at 50 ms per tick.

**READ vs INFERRED.** Every claim is marked:
- *read*: I opened the method, or a survey agent did and I spot-checked it;
- *inferred*: reasoned from code around it;
- *UNVERIFIED*: a Paper or vanilla behaviour not read from the pinned jar.

For the 11 picked items, I read these myself at cfde822:
- `WitheredMobFactory` in full;
- `DragonfightReinforcements.spawnPhaseWave` / `spawnReinforcement`;
- `DragonfightManager.onDragonfightDamage`'s threshold block, `PHASE_TYPE_CAPS` and `phaseCapFor`;
- `ScattershotWeapon` in full;
- `MeleeWeaponListener.onBoneClubHit` and `applyFalchionWither`;
- `KnellBeamListener`'s constants and particle/sound calls, and `WitheredKnellBehavior`'s;
- `CatalogueRegistry.buildElementMap` / `buildClassMap` / `buildRarityMap`;
- `BossLootListener.dragonEpicPool`.

The rest comes from two read-only survey agents, whose reports listed what each read in full and what it skimmed.

---

## RULINGS

### BEN'S PICK, 2026-09-29, VERBATIM (copied byte for byte from the seat's file, not retyped)

```
Ben's weapons: "the Bone Club, Withered Falchion, Short Bow, Scattershot, Knell Beam, and the
Blaze King's staff."
Ben's mobs: "I want to start working on bringing the Ender Dragon fight event from the old repo
back into this soon. First we need the mobs from it: The Knell, Skreel, Phalanx, Phage, and
Goldrot Goon. We'll talk more about this later."
```

**What the pick rules, and what it does not:**
- **Q-P1 (the batch) is ANSWERED.** The six weapons and five mobs above replace §5's proposed batch. §5 is kept only
  as the record of what was proposed.
- **Q-P7 (the tomes' class) is RETIRED:** no tome was picked.
- **Q-P9 (undead farm animals) is RETIRED:** they were not picked.
- **Q-P8 (mobs: a separate thread?) is ANSWERED in direction:** the five are the Ender Dragon event's mobs, and they
  come first (*"First we need the mobs from it"*). What remains open is the build order, which §9 proposes.
- **NOT ruled by the pick:** any number, and whether *"Scattershot"* means a new item or the shipped
  `dragons_breath` (§2.4, Q-P10).
- *"We'll talk more about this later"* means the **dragon-fight event itself is NOT surveyed here** (§3.0).

### STANDING RULINGS THIS SURVEY TOUCHES — NOT RE-DERIVED HERE

| ruling | where it lives | what it does to this port |
|---|---|---|
| **Extract MECHANISMS as facts; never port STRUCTURE** | `HANDOFF-damage-system.md`, *Reference implementation* | Every sub-section below describes what the old code did, not a design to copy. cfde822's AI is `BukkitRunnable` loops, its names are `§` codes, and its listeners sprawl. **All three are BANNED here** (CLAUDE.md). |
| **NEW CONTENT CITES THE BOLTOR** | CLAUDE.md; `.claude/rules/standing-decisions.md` | **No number comes from `hunters_bow`, `ironblade` or `quiver_stone`.** The Boltor's set is RULED: `attack_damage 19`, `cooldown_ticks 16`, `range 96`, `quiver_size 8`, `reload_ticks 60`. |
| **Author cooldowns in multiples of 4** | same | Every `cooldown_ticks ___ (Ben)` must be a multiple of 4. **Two picked weapons' cfde822 cooldowns are off the grid** (§6 F4). |
| **NO CUSTOM ITEM STACKS ABOVE 1** | CLAUDE.md | `WeaponItems.mint` sets it. It matters for bases that stack in vanilla (§6 F9). |
| **A TRAVELLING RANGED WEAPON AUTHORS KNOCKBACK; an instant one does not** | standing-decisions, that section | It applies to the Short Bow, and to the Scattershot if it is a new item. **A travelling mage or melee weapon is UNRULED, not excluded:** Blaze King's Staff left-click, the Falchion's bomb. |
| **Actives are gear-blind BY DESIGN** (melee Q6) | `PLAN-melee-class.md` on the M1 branch, *BEN'S RULINGS ON SECTION 10* | Q6 covers **stone casts**. A **weapon's own trigger** carries the held weapon's gear score: `CastExecutor` writes `withTriggerScore(world.triggerScoreOf(...))` on every cast path, and `PaperCombatWorld.triggerScoreOf` reads the held weapon (*read*). **Q6 does not reach these six weapons.** |
| **Mob scaling M1–M27** | `PLAN-mob-scaling.md` §0 | **M4:** a custom mob gets GS/100 on its authored HP and **no** ×5. **M17:** the Knell's damage is parked (§6 F15 there). **M21:** hostile is `instanceof Enemy`; a custom mob on a PASSIVE base is ×1 with no GS. **M24:** mob damage is authored × GS/100 for custom mobs. **M25:** the End is GS 300 within 1,000 blocks of (0,0). **M27 / §6 F22:** the perched dragon is deferred to the dragon-fight work. |
| **Punch is PARKED** | `NEXT.md`, *PARKED — PUNCH* | Punch is built nowhere and rolls nowhere today. `dragons_breath` is the one shipped weapon that would be eligible. **The Short Bow would be a second.** This is a fact, not a reason. |
| **Dev-weapon deletion is parked on "enough shipped weapons to replace them"** | `NEXT.md`, *PARKED SLICE — THE DEV-WEAPON AND /kit DELETIONS* | **Bone Club and Withered Falchion are the first real melee weapons.** Today the only non-deletion-set melee weapon is the fixture `emberblade`. |

---

### Ben, 2026-09-29, VERBATIM: the second answers, and the seat's rulings on `0f24131d`

```
  Scattershot: "Yes, Kinetic"
  Old numbers: "Sure, everything will be getting tuned later so it's not that important right
     now"
  Acquisition: "We will cover acquisition later. Not important right now"
```

**THE SEAT'S READING (the seat's, not Ben's):**

- **Scattershot:** a NEW item, the old 5-arrow crossbow fan, element `kinetic`. **Not `dragons_breath`. Q-P10
  ANSWERED.** It is NOT content-only: it needs E6, a click-fired yaw fan (§2). So it is not in LEGACY-A unless E6
  lands first.
- **Old numbers:** cfde822's numbers are the STARTING values, marked PROVISIONAL (to be tuned). The Boltor rule still
  governs citations and the 4-tick grid. **Q-P2 ANSWERED.**
- **Acquisition:** `/rpg give` only, for now; no recipes, drops or loot. **Q-P4 ANSWERED.**

**SEAT RULINGS (mechanism), 2026-09-29:**

- **L4.** M17 stays separate from the mob body-fields slice. **Q-P11 ANSWERED: no.**
- **L5. Stack order, bottom-up:** #168 (M1) → LEVEL → NEXUS polish → LEGACY-A (Short Bow, Blaze King's Staff: content
  only) → later slices. Each slice has its own PR and gate, and each R0c predicts against the slice below it.
- **L6.** Nothing is built until Ben answers his questions for that slice.

## 1. WHAT I READ

**In this repo, at `2d60e9a3`:**
- the weapon schema: `WeaponLoader.parse` (and its `KNOWN_KEYS` and `TRIGGER_KEYS`), `AbilitySchema.parseCast`
  and its effect parser;
- the sealed types: `EffectSpec`, `CastSpec` (including the `Spread` javadoc), `SpreadPattern` and `DrawFan`;
- the mob side: `MobLoader`, `MobDefinition`, `StatusLoader` and `StatusDefinition`;
- the cast path: `CastExecutor.dispatch` / `landBasicMelee` / `launchRay` / `stepRay` / `detonate`;
- the effect side: `EffectApplier`'s `Knockback` and `Burst` arms, `applyToEach` and `applyToNearbyMobs`;
- the rest of the engine I cite: `ProjectileFlight.nearestMob`, `BasicMelee.isVanillaDriven`,
  `PaperCombatWorld.triggerScoreOf`, and `RpgCommand.spawnMob` (the one writer of `mob_id`);
- content: every file in `content/weapons/`, `elements/` and `statuses/`, plus `content/mobs/knell.yml`;
- plans: `PLAN-dragons-breath.md` §1, §2 and §11; `PLAN-mob-scaling.md` §0, §1.5, §6 F15 and §6 F22;
- git: the history of `scatter_shot` (`git log -S`);
- the whole-directory content tests named in §7, `MobLoaderTest.theBundledKnellLoads`, and the
  `new MobDefinition(` call sites.

**On `origin/feat/melee-m1`:**
- `git diff 2d60e9a3 origin/feat/melee-m1 --stat` (23 files, +1513 −25);
- every test diff, plus the `RpgPlugin`, `RpgCommand`, `CastExecutor`, `EffectApplier` and
  `brawlers_gauntlet.yml` diffs;
- `melee_fire.yml` and `GATE-melee-cell.md`'s R0c;
- `PLAN-melee-class.md`'s RULINGS, §1, §8 (the class names only), §9 and §10.

**From the pinned jar.** The jar is `paper-api-26.1.2.build.74-stable.jar` (`pom.xml`'s `paper.version`), read with
`javap` from `~/.jdks/openjdk-26.0.1`. **Only the names below were read. What each one does at runtime is
UNVERIFIED.**
- **Materials:** `org.bukkit.Material` has the seven `*_SPEAR`s, `TRIDENT` and `MACE`.
- **Item components:** `DataComponentTypes` has `KINETIC_WEAPON` and `PIERCING_WEAPON`.
- **Attributes:** `org.bukkit.attribute.Attribute` has `SCALE`, `MOVEMENT_SPEED`, `ATTACK_DAMAGE`,
  `FOLLOW_RANGE` and `KNOCKBACK_RESISTANCE`.
- **Mob control:**
  - `org.bukkit.entity.Mob` has `getPathfinder()` and `setTarget(LivingEntity)`;
  - `com.destroystokyo.paper.entity.Pathfinder` has `moveTo(Location)`, `moveTo(Location, double)` and
    `moveTo(LivingEntity)`;
  - `com.destroystokyo.paper.entity.ai.MobGoals` and `Goal` exist, and `Bukkit.getMobGoals()` returns one.
- **Entities:** `Entity` has `addPassenger`; `LivingEntity` has `setInvisible` and `getEquipment`.

---

## 2. THE SIX WEAPONS

**Tiers:**
- **(a)** content-only in today's schema (§4);
- **(b)** a named new engine capability;
- **(c)** a new system.

**Old class and element come from `CatalogueRegistry.buildClassMap` / `buildElementMap`.** Those maps' own comment
says the fields were *"display-only for now: no combat mechanic reads them yet"* (*read*). **So they record intent,
not behaviour.** cfde822's "elementless" (`ELEMENTLESS_IDS`) has no twin here. The nearest is `kinetic`, which
accrues nothing.

### 2.1 Bone Club

| | |
|---|---|
| **what it did** | A melee club. "Drumming Beat" (`MeleeWeaponListener.onBoneClubHit`, primary target only, never a sweep): every **consecutive fully-charged** hit counts, and the Nth deals **2×** its damage. The count resets on an under-charged swing (`getAttackCooldown() < 1.0`), after an idle timeout, or on swapping off (`onBoneClubSwapOff`). **The count is per WIELDER, not per target:** hits on different mobs chain (*read*). Rising drum-note pitch per count, a crit sound and particles on the Nth. No innate sweep (`CombatListener.onSwordSweep` skips it). |
| **cfde822 numbers (recorded, not adopted)** | material BONE, rarity COMMON, base 70 (`SwordData.SWORD_BASE_DAMAGE`), attack speed 0.8 (`Formulas.attackSpeed`), N = 4 (3 at GS ≥ 30), idle timeout 5000 ms. Source: skeleton drop 1/80 (`MobLootListener.onSkeletonDeath`). |
| **class / element** | cfde822: melee / undead. **Here:** `melee` / ___ (Ben; `undead` ships as an element with no status) |
| **tier** | **(b) — a COMBO COUNTER.** The swing itself is (a): `left_click` `melee` + `weapon_damage`, with `attack_speed` and no `sweep`. What is missing: (1) per-wielder state that survives between hits and resets on idle and on swap; (2) a conditional multiplier on the Nth hit; (3) the **full-charge condition**. The charge is already known in core: `landBasicMelee` receives `chargeScale` and builds `Caster.of(caster, chargeScale)` (*read*). But no effect or weapon field can condition on it. |

### 2.2 Withered Falchion

| | |
|---|---|
| **what it did** | **Left:** a normal sword swing. On a **fully-charged** primary hit, "Decay" deals a flat damage **once per second for 3 s** by sourceless `target.damage(n)` (`MeleeWeaponListener.onWitheredFalchionHit` → `applyFalchionWither`, *read*). **It is not a vanilla wither potion**: an owned per-second hit with its own dark-grey damage number. **Right, "Wither Bomb"** (`onWitheredFalchionInteract`): a non-incendiary SmallFireball, yield 0, with a smoke trail and zero contact damage (`onWitheredFalchionFireballEntityDamage`). On impact (`onWitheredFalchionSkullHit`) it deals AoE damage plus the same per-second wither tick, skipping party members and summons. |
| **cfde822 numbers** | STONE_SWORD, EPIC, max durability 1561. Its own damage curve: below GS 50, 44 + (GS−1)×65/49; from GS 50, 109 + (GS−50)×4/5 (`SwordLoreRenderer.applySwordLore`). Decay 5/s × 3 s (`WeaponFormulas`, GS-1 power base). Bomb: 21 damage, 1/s × 3 s, radius 3, cooldown 8 s (160 t), velocity 1.4, **no mana**. The `WITHERED_FALCHION_BOMB_UNLOCK_LEVEL` 50 is declared but unchecked. Source: the Withered Knight Commander pool (never called, §6 F10) and the dragon's epic pool (`BossLootListener.dragonEpicPool`). |
| **class / element** | cfde822: melee / **wither** (the only picked weapon with that element). **Here:** `melee` / ___ (Ben) |
| **tier** | **(a) for the swing and the bomb:** `left_click` `melee` + `weapon_damage`; `right_click` `projectile` (an `item:` body, `gravity` ___) + `burst` [`damage`]. **(b) for Decay:** it needs **an OWNED DoT STATUS KIND**, a flat per-second hit credited to the applier (Scorch's shape without Scorch's % of max HP), **and the full-charge condition** from §2.1. The content-only stand-in, a `potion` status with `potion_type: wither`, is a different mechanism (§6 F2). |

### 2.3 Short Bow

| | |
|---|---|
| **what it did** | An instant-fire bow (`ShortBowWeapon.onShortBowInteract`): a real arrow per right-click with a damage roll, arrow ammo from the inventory (`ArrowAmmo.pickFirst`), and arrows tagged **ARROW_NO_KNOCKBACK**. |
| **cfde822 numbers** | BOW, COMMON, max GS 30, damage 16–23 (`RangedData`), cooldown 500 ms (10 t; it goes through `StatService.rangedCooldown`, so Fire Rate shortens it), velocity 3.0. Source: crafted (sticks, redstone, string), or a Fletcher trade at level 4+ (`FletcherTradeListener`). |
| **class / element** | cfde822: ranger / elementless. **Here:** `ranger` / ___ (Ben) |
| **tier** | **(a):** `right_click` `projectile` with `body: arrow` + `weapon_damage` + **`knockback`**, which the ruling requires, **against cfde822** (§6 F3). It must also declare `quiver_size` and `reload_ticks`, because `ExpandedQuiverContentInvariantTest.everyRangerWeaponAuthorsAMagazineOrIsAnExemptDevWeapon` refuses a ranger weapon without them. Arrow ammo from the inventory has no counterpart here; the quiver is the ammo model. The trade is (c). |

### 2.4 Scattershot — AND WHAT `dragons_breath` KEPT AND CHANGED. THE ITEM QUESTION IS BEN'S (Q-P10)

**What cfde822's Scattershot did** (`ScattershotWeapon.fireFan`, read in full):
- A crossbow firing **5 real arrows** in a **horizontal yaw fan at −20/−10/0/+10/+20°** on right-click (the
  instant-fire path, `RangedListener.onManagedBowInteract`).
- It uses **one arrow of ammo per volley**. Only the centre arrow can be picked up, and only it carries a tipped
  arrow's potion.
- **If firework rockets are in the off-hand, it fires 5 rockets instead** and consumes one. A rocket hit bypasses
  i-frames, with an inline crit (`handleFireworkHit`).
- Every projectile carries **flat** damage (min = max), plus the bow's ranged enchant tags (`tagScattershotProjectile`).

**cfde822 numbers:**
- CROSSBOW, UNCOMMON, max GS 50;
- `SCATTERSHOT_BASE_DAMAGE` 25 per projectile (`RangedData`, a GS-1 power base);
- velocity 3.15;
- crossbow base cooldown 1250 ms (25 t, `RangedListener`'s `CROSSBOW_BASE_COOLDOWN_MS`), reduced by Fire Rate;
- class ranger, element **elementless** (`ELEMENTLESS_IDS`);
- source: crafted (`RecipeRegistry`), or a Fletcher trade at level 5+.

**The link to `dragons_breath` is a NAME, not a written provenance.**
- `dragons_breath` shipped in #135 (`adc6622a`) and was first named `scatter_shot` on branch `feat/14-scatter-shot`.
  `PLAN-dragons-breath.md` §11 records the rename *"`scatter_shot` → `dragons_breath`"*.
- But **no file in this repo cites `ScattershotWeapon` or cfde822**: `grep -i scattershot` over the repo's markdown
  and the weapon YAML returns nothing.
- My first draft said "redesigned from Scattershot". **That was an inference from the name, and it is corrected
  here.**

**Side by side, as both stand:**

| | cfde822 Scattershot | shipped `dragons_breath` (`PLAN-dragons-breath.md` §1, RULED) |
|---|---|---|
| bodies per press | 5 | **7** |
| pattern | a flat **yaw fan**, 10° apart, ±20° | **one down the aim + a ring** of 6 in the shooter's view plane (`SpreadPattern`), `angle_degrees 3` |
| ammo | 1 inventory arrow per volley; centre pickup | a **quiver**: `quiver_size 5`, `reload_ticks 40` |
| cadence | 1250 ms base, Fire-Rate-scaled | `cooldown_ticks 32` |
| damage | flat per arrow | literal `damage 9` per arrow |
| element / rarity | elementless / uncommon | **fire / legendary** (rebranded 2026-09-22 from kinetic / rare) |
| knockback | not suppressed by the weapon (*inferred*: no `KnockbackRegistry` call in `ScattershotWeapon`) | **authors `knockback`**, per the ruling |
| firework ammo, tipped arrows | yes | no |
| material | crossbow | crossbow |

**What `dragons_breath` KEPT:** a crossbow, several arrows per press from one decision, and flat damage per arrow.
**What it CHANGED:** everything else in the table.

**So Q-P10 is Ben's:** does *"Scattershot"* mean (i) **a second, separate item** that ports cfde822's 5-arrow yaw fan,
or (ii) **`dragons_breath`**, already shipped? **This survey does not pick.**

**Tier, if it is a separate item: (b) — A CLICK-FIRED YAW FAN.**
- A `spread:` block cannot express a flat fan: `SpreadPattern` builds a ring **around** the aim (*read*, its javadoc).
- The exact cfde822 fan already exists in core. `DrawFan`'s javadoc rules *"20 10 0 -10 -20"* at n = 5 (*read*).
- **But `DrawFan` is reached only by the Plume's draw path** (`PlumeDraw.release`; `WeaponFire`'s javadoc names the
  Plume as the only caller).
- So the capability is to expose the fan to a click-fired `projectile` cast. That is small, but it is a `CastSpec`
  field.
- It must also author a quiver and knockback, like any ranger weapon.
- **The firework variant is out of scope:** a projectile-body kind we do not have.

### 2.5 Knell Beam — AND HOW IT RELATES TO THE KNELL MOB

| | |
|---|---|
| **what it did** | `KnellBeamListener.onKnellBeamInteract` / `fireKnellBeam`: right-click spends mana **up front, not refunded**. **The aim locks at cast.** A **12-tick telegraph** draws a purple dust line at the start and near the end, with a bell resonate. Then the beam fires: a cyan dust line, bell and amethyst-chime sounds, **clipped by blocks**, and it **PIERCES every entity** whose hitbox, grown by the hit radius, meets the line. It skips party members and creative/spectator players, suppresses knockback, and **detonates End crystals** on its path. |
| **cfde822 numbers** | ECHO_SHARD, EPIC. Mana 13, cooldown 1200 ms (24 t; the class comment wrongly says none), telegraph 12 t, range 40, hit radius 1.0, damage `KNELL_BEAM_BASE_DAMAGE` 63 (a GS-1 power base) plus level, Eye and armour bonuses, × Attunement. Source: the dragon's epic pool (`BossLootListener.dragonEpicPool`, *read*). **Not dropped by the Knell mob.** |
| **class / element** | cfde822: **mage** (catalogue MAGIC) / **void**. **Here:** `mage` / ___ (Ben) |
| **tier** | **(b), two capabilities:** **PIERCE on a ray**, and **AN AIM-LOCKED WIND-UP**. Pierce: `launchRay` → `stepRay` resolves one `castRay` hit and stops (*read*). The nearest existing shape to the wind-up is `volley` with `shots: 1` and `windup_ticks`, **but a volley re-aims every shot from the live eye**: that is the cursed-emerald Q1 ruling, and `CastSpec.Volley`'s javadoc says so. **cfde822 locked the aim at cast.** Crystal detonation is out of scope until the dragon fight. |

**Relation to the Knell mob** (read in both classes; §3.1 describes the mob):
- **The same beam, twice.** The mob's `WitheredKnellBehavior.scheduleKnellBeamAttack` / `fireKnellBeam` and the
  weapon's `KnellBeamListener` use:
  - **the same telegraph colour** (dust RGB 95,35,165, size 0.8) and **the same beam colour** (RGB 40,230,235,
    size 1.4);
  - **range 40**, hit radius 1.0 and a 12-tick telegraph;
  - **the same three sound events** (bell resonate, bell use, amethyst chime).
- The weapon is the mob's attack, given to the player.
- **Two differences:**
  - **The mob's beam hits ONE locked target** (only if that target's box, grown by 1.0, still meets the beam). **The
    weapon PIERCES.**
  - **The mob's sound calls pass volume and pitch the other way round from the weapon's.** For example, the resonate
    is `(1.4f, 0.7f)` on the mob and `(0.7f, 1.4f)` on the weapon (*read*). Whether that was intended is not
    recorded (*inferred* to be a cfde822 slip).
- **Not a drop:** the mob drops nothing (dragon-fight minions have drops stripped, `DragonfightManager.onBossEntityDeath`).
  The weapon is dragon loot.
- **What a port can share:** the visuals (a telegraph visual and a beam visual) and the aim-locked wind-up. **The
  weapon's pierce is not shared.**

### 2.6 Blaze King's Staff

| | |
|---|---|
| **what it did** | **Left** (`StaffListener.onBlazeStaffInteract`): a non-incendiary SmallFireball. On hit (`onBlazeFireballEntityDamage`) its damage is replaced with the weapon's value plus offence bonuses, and it sets fire ticks. **Right, "Smolder":** ignites every non-party, non-summon living entity in a radius, then deals a per-second hit to each while it still burns. Its melee attack modifier is removed by the lore renderer (*inferred*: a vanilla-rod swing). |
| **cfde822 numbers** | BLAZE_ROD, LEGENDARY, max GS 99. Left: mana 8, cooldown 400 ms (8 t), velocity 1.5, damage 30 (GS-1 power base), 100 fire ticks. Right: mana 40, cooldown 20 s (400 t), radius 10, ignite 160 t, then a GS-scaled per-second hit while burning. `BLAZE_SMOLDER_UNLOCK_LEVEL` 50 is declared but unused. Source: blaze drop 1/500 (`MobLootListener.onBlazeDeath`). |
| **class / element** | cfde822: mage / **fire**. **Here:** `mage` / `fire`, if Ben keeps it. **Fire buys Scorch** through `fire.yml`'s `applies_status: scorch`. |
| **tier** | **(a)**. Left: `left_click` `projectile` (an `item:` body such as a fire charge; **not a fireball entity**, because only `item:` or `body: arrow` exist) + `damage`. Right: `right_click` `self` + `burst` [`damage`], with the burn carried by Scorch accrual. A `left_click` is allowed on a non-quiver weapon: `WeaponDefinition` refuses it only on a quiver weapon (*read*). **The Smolder burn needs a damage payload:** Scorch is capped by the applying hit's damage (`StatusDefinition.Scorch`'s javadoc), so a `status: scorch` burst with **no** damage burns for nothing (§6 F12). |

---

## 3. THE FIVE MOBS

### 3.0 What the dragon fight used each for (facts only; THE EVENT IS NOT SURVEYED HERE)

**The event's future home:**
- `PLAN-mob-scaling.md` M27 and §6 F22 already defer one dragon behaviour, *"a perched dragon is never knocked
  airborne by damage"*, to *"our Ender Dragon boss fight"*.
- Ben's words (RULINGS) say the event comes *"soon"*, after these mobs.
- **This section records only which mob the old fight spawned, when, and how many.**

**How cfde822's fight spawned them** (*read*: `DragonfightManager.onDragonfightDamage`,
`DragonfightReinforcements.spawnPhaseWave` / `spawnReinforcement` / `scheduleNext`, `PHASE_TYPE_CAPS`):

- **The trigger is the dragon's own HP crossing 80%, 60% and 40%** of its PDC max (`thresholds = {0.80, 0.60,
  0.40}`). Each crossing runs a wave for the phase just finished (wave 0 at 80%, 1 at 60%, 2 at 40%), plus
  dialogue and a storm override. *The code's own comments say 75/50/25; the array says 80/60/40.*
- **A trickle runs from the dragon's init until death.** It fires every 160 / 120 / 80 / 60 ticks for phases 0–3.
  Each tick tries one of every type the phase allows. During a storm's grace or active window it tries three
  Phages only.
- **Every spawn is capped** by `phaseCapFor(bossName, phase)` against the alive count. A capped spawn returns null.
  The Lesser Phage split bypasses the cap.
- **Anchors:** 3 points on a radius-15 ring around the End pillars, jittered ±2 blocks.

| mob | wave 0 (80%) | wave 1 (60%) | wave 2 (40%) | trickle from | cap by phase 0/1/2/3 |
|---|---|---|---|---|---|
| **Phage** | 6 | 4 | 2 | phase 0 (and the storm's 3) | 10 / 8 / 8 / 7 |
| **Goldrot Goon** | 3 | 3 | 2 | phase 0 | 4 / 4 / 3 / 2 |
| **Phalanx** | 0 | 2 | 3 | phase 1 | 0 / 3 / 4 / 4 |
| **Knell** | 0 | 1 | 3 | phase 1 | 0 / 2 / 4 / 5 |
| **Skreel (+ its Mount)** | 0 | 0 | 2 pairs | phase 2 | 0 / 0 / 3 / 4 (counted by Skreels, so pairs) |

*Also spawned by the waves, but not picked:* one Withered Dark Mage per wave, and one Withered Knight Commander in
wave 2.

**Shared by all five in cfde822** (`WitheredMobFactory`, *read*):
- they are wither skeletons, except the Skreel's spider mount;
- `setRemoveWhenFarAway(false)`, defence 0, follow range 64 (`applyDragonfightFollowRange`);
- registered as dragon-fight minions, with drops and XP stripped on death, and no friendly fire between them
  (`DragonfightFactionListener`).

### 3.0.1 How a mob is graded here

**Today's mob schema is `base_entity`, `display_name` and `max_health`, and nothing else** (`MobLoader`, *read*).
**`/rpg spawn` is the only way a custom mob enters the world** (`RpgCommand.spawnMob` is the one writer of
`mob_id`). So:
- **(a)** means a name and a health bar on a vanilla body, reachable by an operator command.
- **(b) MOB BODY FIELDS** means equipment, scale, speed and `attack_damage`. None is authorable today.
- **(b) MOB ON-DEATH EFFECTS** means a split or an explosion.
- **(c)** means scripted behaviour: a timed attack, a reaction, a movement state machine, or mounting.

**Mob scaling applies to all five the same way.**
- Every base is `WitherSkeleton` or `Spider`, and **both are in M21's `Enemy` closure → hostile → gear score**.
- M4: a custom mob gets **authored HP × GS/100, no ×5**.
- M25: in the End within 1,000 blocks of (0,0), which is the dragon's island, **GS is 300**. So an authored 360
  becomes 1,080 there.
- **cfde822's HPs were absolute and are recorded, not adopted.**
- M24 prices custom damage as **authored × GS/100**, but there is **no `attack_damage` field**. So every custom
  mob hits with its base entity's vanilla attribute at ×1 (M17 / §6 F15).
- **None of the five sits on a passive base, so M21's ×1 passive-base rule does not bite.**

### 3.1 The Knell — ALREADY SHIPS AS `content/mobs/knell.yml`. OLD vs NEW

| | cfde822 (`WitheredMobFactory.spawnWitheredKnight`, *read*) | shipped `knell.yml` (`aee156a8`, 2026-08-23) |
|---|---|---|
| base / name | wither skeleton, "§fKnell" in a PDC `BOSS_NAME` | `wither_skeleton`, `display_name: "Knell"` as the real CustomName |
| HP | 480, absolute | `max_health: 360`, × GS/100 (M4) |
| scale / speed | 1.6 / × 0.6 | vanilla / vanilla (**no field**) |
| gear | diamond armour, Protection I; diamond sword, Sharpness IV, Knockback I, Fire Aspect I | vanilla spawn equipment only (**no field**) |
| **the beam** | every 100–200 t: lock a target, root itself 12 t with a telegraph, then fire one locked-target beam for 35 flat magic damage, exempt from ×5 (`WitheredKnellBehavior.scheduleKnellBeamAttack` / `fireKnellBeam`) | **absent** |
| **the dodge** | any projectile hit is cancelled; it teleports 4–5 blocks sideways, perpendicular to the shot, and deletes the projectile, with no cooldown (`WitheredKnightSystem.onWitheredKnightDodgeProjectile` / `teleportKnightSideways`). Custom ranged weapons consult it (`tryWitheredKnightDodge`). It does not dodge melee or magic | **absent** |
| melee damage | vanilla attack × the ×5 multiplier (`CombatListener.onMobDamageMultiplier`) | **its base's vanilla attack at ×1 — M17, PARKED** (`PLAN-mob-scaling.md` §6 F15) |
| how it spawns | dragon waves and trickle (§3.0) | `/rpg spawn knell` only |

**So the shipped Knell is the identity and a health pool, not the mob.** M17 (*"The Knell's damage: PARKED … It
keeps its current behaviour"*) is still open, and **any port of the Knell's melee must first get M17 answered**,
because a body `attack_damage` field is what would close it. `MobLoaderTest.theBundledKnellLoads` asserts the name
and the base, and **bounds** the HP rather than pinning 360. So Ben can retune it without that test failing (*read*,
its own comment).

**Tier:**
- **(b) body fields** for scale, speed and gear;
- **(c) for the beam and the dodge**, which are the Knell's whole identity in cfde822;
- the dodge also needs a **pre-damage reaction hook on a custom mob**.

### 3.2 Phage (and the Lesser Phage it splits into)

**Found** as `WitheredMobFactory.spawnWitheredGrunt`. Its PDC name is "§7Phage", and its catalogue id is
`WITHERED_GRUNT` (Phage and grunt are one mob). The split form is `spawnLesserPhage` ("§7Lesser Phage").

| | |
|---|---|
| **what it did** | A wither skeleton in one random armour tier (gold, chain or iron), with a stone sword and vanilla AI. **On a player kill it splits** into 2 Lesser Phages ±0.5 x apart (`WitheredMinionDeathListener.onPhageDeathSplit`). Lesser Phages are small, fast, unarmoured, and never split. |
| **cfde822 numbers** | Phage: 160 HP, speed × 0.75. Lesser Phage: 80 HP, scale 0.6, speed × 1.10. The catalogue's "Phage 300" is stale (`CatalogueRegistry` disagrees with the factory). Coin drops 5 and 2 (`MobLootListener.witheredMobCoinDrop`). |
| **dragon fight** | the most numerous: waves 6/4/2, trickled from phase 0, and the storm's only spawn (§3.0) |
| **tier** | Name + HP is **(a)** today, as two files, `phage` and `lesser_phage`. **(b) body fields** for gear, scale and speed. **(b) MOB ON-DEATH EFFECTS** for the split: *spawn N of mob id X at the corpse*. |

### 3.3 Goldrot Goon

**Found** as `WitheredMobFactory.spawnWitheredRouge`. Its PDC name is "§eGoldrot Goon", and its catalogue id is
`WITHERED_ROUGE`.

**Its relation to `GoldrotSet` is the NAME and the gold theme only** (*read*: grep):
- `armor/GoldrotSet` is a player armour set whose bonus is orbiting Wither-skull "Golden Wither Orbs".
- **It never refers to the Goon, and the Goon's spawn never refers to it.**
- The Goon wears one **vanilla** gold chestplate, not a Goldrot piece.
- The Goldrot armour and the Goldrot Chalice are **dragon loot** (`BossLootListener`), so the link is thematic.

| | |
|---|---|
| **what it did** | A fast wither skeleton: a golden sword with Sharpness IV, a golden chestplate with Projectile Protection IV, vanilla AI. **On a player (or bounce-attributed) kill it explodes**: explosion particles and sound, then **flat damage to every living entity within 4 blocks** except itself and the dragon, exempt from the ×5, with no block damage (`WitheredMinionDeathListener.onGoldrotGoonDeathExplode`). That means **other minions and players alike**. |
| **cfde822 numbers** | 80 HP, speed × 1.25, explosion 50 in radius 4. The catalogue's 150 is stale. Coin drop 8. |
| **dragon fight** | waves 3/3/2, trickled from phase 0 (§3.0) |
| **tier** | Name + HP **(a)**. **(b) body fields.** **(b) MOB ON-DEATH EFFECTS** for the blast. **Pricing the blast is a ruling, not a tier:** it is a mob-dealt literal, and M16/M24 price mob damage as *vanilla × 5 × GS/100* or *authored × GS/100* (§6 F13). |

### 3.4 Phalanx

**Found** as `WitheredMobFactory.spawnWitheredArcher` ("§7Phalanx", catalogue `WITHERED_ARCHER`). Its behaviour is
in `WitheredPhalanxBehavior.schedulePhalanxBehavior`.

| | |
|---|---|
| **what it did** | A slow wither skeleton with a Power V bow and an offhand shield. **Its bow never fires**: every `EntityShootBowEvent` is cancelled (`onPhalanxBowSuppress`), so the draw is cosmetic. **Blink** (`phalanxBlink`): when a non-creative player comes within range while it is visible, it teleports to a random passable surface farther off and turns invisible. **Dark swirl** (`firePhalanxSwirl`): on a period it launches a non-entity homing bolt at the nearest player; the bolt stops on blocks and hits the first non-minion, non-dragon entity for flat, ×5-exempt damage. Firing makes it visible again. **One-shot shield** (`onPhalanxShieldAbsorb`): the first hit it takes is cancelled, the offhand shield is removed, and a shield-break sound plays. |
| **cfde822 numbers** | 240 HP, speed × 0.65. Blink trigger 5 blocks, landing 6–16 blocks, 8 attempts. Swirl every 100 t, acquire range 30, 0.4 blocks per step every 2 t, 10% homing, hit radius 1.2, range 35, max 200 t, damage 40. |
| **dragon fight** | waves 0/2/3, trickled from phase 1 (§3.0) |
| **tier** | **(c).** Blink with invisibility, the swirl, the shield and the bow suppression are all scripted behaviour. **One engine fact bites the swirl:** our projectile homing **skips players** (`ProjectileFlight.nearestMob`: `if (candidate.state().player()) continue;`, *read*), so a mob-cast homing bolt could not seek a player today. |

### 3.5 Skreel (with its Withered Mount)

**Found** in `WitheredMobFactory.spawnWitheredSpiderJockey`:
- the rider is a wither skeleton, "§7Skreel", catalogue `WITHERED_SPIDER_JOCKEY`;
- the mount is a spider, "§8Withered Mount";
- the rider is added with `spider.addPassenger(ws)`.

| | |
|---|---|
| **what it did** | A pair. The Skreel carries an **iron spear** with no enchants; the leap's damage is scripted, not the spear's. **Behaviour** (`WitheredSpiderJockeyBehavior.scheduleWitheredSpiderJockeyBehavior`) is a **RETREAT → WINDUP → LEAP → LAND** state machine that clears vanilla aggro every tick. Retreat circles to a stand-off band from the nearest player; wind-up has particles and a sound and aborts if the player leaves the band; the leap is a homing `setVelocity` arc; the landing hits a player within reach for flat, ×5-exempt damage plus knockback. **When either partner dies the survivor becomes a plain brawler** (`convertSkeletonToBrawler` / `convertSpiderToBrawler`). |
| **cfde822 numbers** | Mount: 120 HP, scale 1.5, speed × 1.3 while paired. Skreel: 120 HP. Search 50 blocks; stand-off 12 blocks, band 8–12; wind-up 15 t; leap timeout 40 t; landing reach 2.5, 35 damage, knockback 0.7 horizontal / 0.4 vertical. |
| **dragon fight** | 2 pairs in wave 2, trickled from phase 2; the cap counts Skreels, so it counts pairs (§3.0) |
| **tier** | **(c)**, and the most involved of the five. It is **two mob ids** (`skreel` and a mount) plus a **pairing** (a passenger on spawn, and survivor conversion), which today's schema cannot express. The leap state machine is scripted AI. A spear is carried (§6 F8). |

---

## 4. TODAY'S SCHEMA — WHAT "(a)" MEANS, READ FROM THE LOADERS

**Weapon top-level keys** (`WeaponLoader.KNOWN_KEYS`): `id`, `display_name`, `element`, `rarity`, `class` (required:
melee | ranger | mage), `material`, `attack_damage`, `attack_speed`, `sweep`, `quiver_size`, `reload_ticks`,
`flavor`, `triggers`, `craft_result`, `unscored`.

**Trigger keys** (`WeaponLoader.TRIGGER_KEYS`): `name`, `description`, `cooldown_ticks`, `cost`, `cast`, `on_hit`,
`on_cast`.

**Inputs:** `left_click` and `right_click`, plus the Plume's `draw` / `tap1..3`. There is **no sneak, drop or swap
input**.

**Cast types** (`AbilitySchema.parseCast`):
- `self`
- `melee` (reach, arc)
- `ray` (range, beam) — **single target, first hit**
- `projectile` (speed, gravity, lifetime, trail, `item` or `body: arrow`, `homing` — **non-players only** —, and
  `spread` — **a ring about the aim**)
- `dash`
- `volley` (windup, shots, interval, an inner cast) — **re-aims every shot**

**Effect types:**

| family | types |
|---|---|
| Targeted | `damage`, `weapon_damage`, `heal`, `knockback`, `status` |
| Untargeted | `burst`, `area`, `visual`, `throw_embers` |

`on_cast` takes `visual` only. **No effect is conditional**: there is no chance, charge or combo field.

**Status kinds:** `fire`, `potion`, `rooted`, `freeze`, `soaked`, `scorch`. Four ship. `scorch` is the only owned
damage-over-time, and it is **% of max HP capped by the applier's hit**.

**Elements:** seven ship. **Only `fire` has `applies_status`.** `wither`, `undead` and `void` are a colour and a
glyph.

**Mobs:** `base_entity`, `display_name`, `max_health`.

**Behaviours worth knowing:**
1. A `self` cast's Targeted effects land on the caster, and a `burst` in it skips only the caster.
2. A melee weapon's `left_click` runs its **whole** `on_hit` on the vanilla-driven path (`landBasicMelee` →
   `detonate`). *Inferred*: no shipped weapon does it yet.
3. A quiver weapon may not declare `left_click`.
4. **A burst hits other players** (`applyToEach` skips only the caster; §6 F6).

---

## 5. SUPERSEDED — THE PROPOSED BATCH (KEPT AS THE RECORD; Q-P1 ANSWERED BY BEN 2026-09-29)

*This was proposed before Ben picked. It is not the plan.*
- B1 Withered Falchion, B2 Soul Knife, B3 Crystal Shard Staff, B4 Earthquake Tome, B5 Short Bow.
- Alternates: Blaze King's Staff, Blast Fungus.

Ben's pick kept two of them, **B1 and B5**, took Blaze King's Staff from the alternates, and added Bone Club,
Scattershot and Knell Beam. **B2, B3 and B4 are not picked.** Their rows in the earlier draft are not carried
forward.

---

## 6. FINDINGS — RECORDED, NOT FIXED. NONE STOPS THE SURVEY

- **F1 — TWO PRECEDENTS DISAGREE.**
  - `flint_staff` and `lapis_staff` shipped **carrying cfde822 numbers**, and two tests assert them as *"the ported
    numbers"* (`ContentValidatorTest.theShippedLapisStaffCarriesThePortedNumbers`,
    `VisualLoaderTest.theShippedLapisBeamCarriesThePortedNumbers`).
  - **This brief records old numbers and never adopts them.**
  - Nothing written says which of the two governs a port (Q-P2).
- **F2 — THE FALCHION'S DECAY WAS AN OWNED FLAT DoT, NOT A POTION.**
  - cfde822 called `target.damage(n)` once a second and drew its own damage number (`applyFalchionWither`, *read*).
  - The content-only stand-in, `status` → a `potion` kind with `potion_type: wither`, is **a different mechanism**:
    - vanilla wither damage, **credited to nobody** (`StatusDefinition.Scorch`'s javadoc names this coupling for
      vanilla fire);
    - on a mob, **proportional** under M13 (*inferred*: a potion tick is environmental, not a player hit).
  - **The faithful port is (b), an owned flat DoT kind.**
  - A separate question: whether the `wither` element should `applies_status` it (Q-P3).
- **F3 — THE SHORT BOW'S cfde822 KNOCKBACK IS THE OPPOSITE OF THE RULING.**
  - cfde822 tagged its arrows `ARROW_NO_KNOCKBACK`.
  - **The ruling governs.** This is recorded so nobody "restores fidelity".
- **F4 — MULTIPLES OF 4, cfde822's cooldowns converted:**
  - Falchion bomb 160 ✓
  - Knell Beam 24 ✓
  - Blaze King's Staff left 8 ✓ and right 400 ✓
  - **Short Bow 10 ✗** (lands as 12)
  - **Scattershot 25 ✗** (crossbow base 1250 ms; lands as 28)
  - Bone Club has no cooldown: it is a melee cadence, `attack_speed`.

  These are facts about the old numbers. Ben's numbers must be multiples of 4.
- **F5 — NAME THE QUANTITY on the Falchion's bomb.**
  - Sharpness is `class: melee`, `value_by_level [5, 10, 15]`.
  - Whether it modifies a melee weapon's **right-click literal `damage`**, or only the swing, **was not traced.**
  - Trace it before the tooltip is written.
  - The same question applies to **Bone Club's 2×**: does it multiply after Sharpness or before?
- **F6 — A `burst` HITS OTHER PLAYERS.**
  - `applyToEach` skips only the caster.
  - **Smolder** and the **Falchion bomb** would hit players in their radius. cfde822 skipped party members and
    summons.
  - This is true of every shipped burst already (Q-P5).
- **F7 — RETIRED.** It was about undead farm animals, which were not picked.
- **F8 — SPEARS CARRY VANILLA BEHAVIOUR WE HAVE NOT READ.**
  - `KINETIC_WEAPON` and `PIERCING_WEAPON` exist in the pinned API (javap).
  - What a spear does on a **mob** (the Skreel) or a minted player weapon, against our custom-HP pipeline, is
    **UNVERIFIED**.
- **F9 — BASES THAT STACK IN VANILLA:** bone (Bone Club), echo shard (Knell Beam), blaze rod (Blaze King's Staff).
  - The mint caps them at 1, so the standing decision holds.
  - A vanilla stack of the same material is kept apart only by the PDC; see `flint_staff.yml`'s trap note.
  - Stone sword, bow and crossbow do not stack in vanilla.
- **F10 — cfde822 BUGS, NOT TO BE PORTED AS BEHAVIOUR** (*inferred*, not run):
  - `WitheredCommanderEncounter.triggerEndEncounter` NPEs, because `spawnWitheredKnight` returns null outside a
    fight (its phase −1 clamps to row 0, whose Knell cap is 0).
  - `BossLootListener.distributeBossLoot` / `witheredKnightLootPool` are never called.
  - The Skreel's coin rate falls back to wither skeleton's: a name mismatch in `MobLootListener`.
  - `CatalogueRegistry`'s mob HPs disagree with the factory.
  - The threshold comment says 75/50/25 against the code's 80/60/40.
  - The Knell mob's sound volume/pitch are the other way round from the weapon's (§2.5).
- **F11 — NO WAY TO OBTAIN A WEAPON EXCEPT A RECIPE OR `/rpg give`.**
  - All six weapons came from loot, drops, trades or recipes in cfde822.
  - Only recipes exist here (Q-P4).
- **F12 — SMOLDER NEEDS A DAMAGE PAYLOAD FOR SCORCH TO BURN.**
  - Scorch is *"capped by the damage of whatever applied it"* (`StatusDefinition.Scorch`).
  - A `status: scorch` burst with no `damage` has a cap basis of nothing.
  - **Name the quantity:** the Smolder burst's `damage` is the Scorch cap.
- **F13 — HOW A CUSTOM MOB'S SCRIPTED HIT IS PRICED IS UNRULED.**
  - M16 prices mob arrows, blasts and magic as *vanilla × 5 × GS/100*. M24 prices custom mobs as *authored ×
    GS/100*.
  - The Knell beam, the Goon's blast, the Phalanx swirl and the Skreel leap have **no vanilla base**, and today
    there is **no authored field**.
  - Every scripted mob attack needs this answered (Q-P12).
- **F14 — MOB HOMING CANNOT TARGET PLAYERS.**
  - `ProjectileFlight.nearestMob` skips players.
  - That is correct for a player's bolt, and it blocks the Phalanx swirl if it reuses the projectile cast.
- **F15 — `MobDefinition` IS A 4-FIELD RECORD WITH 8 CALL SITES.**
  - 7 are in tests (`MobRegistryTest` 5, `MobSeedingTest` 2), and 1 is `MobLoader`.
  - The body-fields slice either adds a convenience constructor, as `CastSpec.Projectile`'s ladder does, or edits
    all 8.
  - **M1 touches none of them.**

---

## 7. STACKING ON `feat/melee-m1` (PR #168, UNMERGED, `e305ab3a`)

### 7.1 What M1 touches (`git diff 2d60e9a3 origin/feat/melee-m1 --stat`, 23 files)

- **Core:**
  - `CastExecutor` gains `.withSource(ability.id())` at its three trigger-score sites;
  - `EffectApplier` gains a `trace(...)` call in the `Damage` and `WeaponDamage` arms;
  - also `Caster`, `CombatWorld`, `TracedHit` (new), `FakeWorld` and `PlayerHitTraceTest` (new).
- **Paper:** `RpgPlugin`, `AdapterContext`, `PaperCombatWorld`, `RpgCommand`.
- **Content:** 3 abilities, 2 aspects, `builds/melee_fire.yml`, and `brawlers_gauntlet.yml`'s header.
- **Tests:** `AspectLoaderTest`, `PoolLoaderTest`, `ScorchContentInvariantTest`.
- **Docs:** `GATE-melee-cell.md`, `PLAN-melee-class.md`.

**M1 adds no weapon, visual, recipe or mob, and touches no mob file.**

**M2 (Sunder, not yet built) names these in `PLAN-melee-class.md` §8:**
- `DashDirection.FACING`, `AbilitySchema.parseDashDirection` and `CastExecutor.dash`;
- a landing machine beside `SafeLandings`;
- `BukkitCombatant.applyImpulse` / `applyKnockback`.

### 7.2 The hazards, most awkward first

1. **`ScorchContentInvariantTest.KNOWN_FIRE_DAMAGE_SITES`.**
   - Master says **17**; M1 says **21**.
   - The test walks **all** of `content/` (`theFireDamageSitesAreALLDISCOVERED`).
   - **Blaze King's Staff is fire.** If both its left `damage` and its Smolder `damage` carry an effect-level
     `element: fire`, that is **+2 by pattern, 21 → 23 stacked** (*inferred from the pattern
     `^\s+element:\s*fire\b|element:\s*fire\s*\}`; recount, do not add*).
   - A fire Falchion or Bone Club would add more. `dragons_breath` is already counted.
   - **Stacked on M1 this is a sequential edit, not a conflict**, as long as no parallel slice adds fire sites.
2. **`ExpandedQuiverContentInvariantTest.KNOWN_RANGER_WEAPONS = 6`** (`theWeaponContentIsREACHABLEAndHoldsTheRangerWeaponsExpected`).
   - Short Bow → **7**. A separate Scattershot → **8**. If *"Scattershot"* means `dragons_breath`, it stays 7.
   - Both new bows must author a quiver (`everyRangerWeaponAuthorsAMagazineOrIsAnExemptDevWeapon`).
   - `theExemptionSetHoldsExactlyTheOneDevWeaponItWasWrittenFor` pins the exemption set at `hunters_bow` alone.
     **Add neither.**
3. **The R0c `Loaded …` line.**
   - F20 read, on master: `8 abilities … 2 pools … 4 aspects … 13 weapons … 1 mobs, 3 recipes`.
   - M1 read: `11 abilities … 3 pools … 6 aspects … 13 weapons … 1 mobs, 3 recipes`.
   - **Weapons: 13 + 5 or 13 + 6**, depending on Q-P10.
   - **Mobs: `1 mobs` today** (the Knell). The five picks add **four new ids** (`phage`, `goldrot_goon`,
     `phalanx`, `skreel`), plus **`lesser_phage`** if the split ships, plus **a mount id** if the Skreel ships.
     That is **5 to 7 mobs**, by slice.
   - **Recipes: 3 + however many Q-P4 gives.**
   - **Each slice's R0c predicts against the stack top as it stands at that slice**, not against master.
4. **Whole-directory counts** (no conflict with M1, but owed):
   - `GoldenLoreTest.everyShippedTooltipIsUnchanged`: regenerate `golden-lore.txt` and its `=== 92 renderings ===`
     footer once per weapon slice.
   - **`ContentValidatorTest` holds no whole-directory count.** Its size assertions run over copied subsets.
   - `MobLoaderTest.theBundledKnellLoads` copies `knell.yml` alone and asserts `1`, so **new mob files do not move
     it**. A Knell retune moves only its bounded HP check.
5. **Core files the (b) capabilities share with M1 and M2:**

   | capability (§9) | files it would touch | shared with |
   |---|---|---|
   | pierce on `ray` | `CastSpec.Ray`, `AbilitySchema` (ray arm), `CastExecutor.stepRay`, **`CombatWorld.castRay`** (the port returns one hit), **`FakeWorld`**, `PaperCombatWorld` | **M1:** `CombatWorld`, `FakeWorld`, `PaperCombatWorld`, `CastExecutor`. **M2:** `AbilitySchema`, `CastExecutor` |
   | aim-locked wind-up | `CastSpec.Volley` or `Ray`, `CastExecutor.volley` | **M1/M2:** `CastExecutor` |
   | full-charge condition + combo counter | `WeaponDefinition` / `WeaponLoader` (a key), **`Caster`** (it carries `chargeScale`), `CastExecutor.landBasicMelee`, possibly `EffectSpec` / **`EffectApplier`** | **M1:** `Caster`, `EffectApplier`, `CastExecutor` |
   | owned flat DoT status | `StatusDefinition`, `StatusLoader`, a status class beside `ScorchStatus`, `BukkitCombatant`'s status switch, `ElementAccrual`'s switch | **M2:** `BukkitCombatant` |
   | click-fired yaw fan | `CastSpec.Projectile` (a tail field), `AbilitySchema`, `CastExecutor`, `DrawFan` | **M1/M2:** `CastExecutor`; M2: `AbilitySchema` |
   | mob body fields | `MobDefinition` (8 call sites, §6 F15), `MobLoader`, `RpgCommand.spawnMob`'s pre-spawn consumer, `MobSeeding` | none |
   | mob on-death effects | the death path (`MobDeathSystem`), `MobDefinition`, a mob-as-caster `EffectApplier` route | **M1:** `EffectApplier` |

   **Every weapon capability touches `CastExecutor`**, which both M1 and M2 edit. Stacked, that is a sequence
   rather than a conflict. **But each capability slice rebases across M2 if M2 lands below it**, so its diff must
   be re-read after M2, not assumed.
6. **Registry ordering:** nothing found.
   - `class: melee` weapons do not need a melee cell.
   - Mobs key by filename.
   - `/rpg spawn` resolves by id.

### 7.3 The base is DECIDED: stacked on `feat/melee-m1`. What that costs this thread

**The batch's brief rules the base: every thread's slices stack on `feat/melee-m1`, each PR with its own GATE file
committed before any boot, one boot of the stack top reads every gate, and the stack merges bottom-up, #168 first.**
This section does not reopen that. It records what the stack costs this thread, so the gate is written for it:

- **The weapon slices depend on nothing in M1 except the `PLAYERHIT` witness**, which is M1's `TracedHit` under
  `/rpg mobtrace`. That fixes the order in the stack, not the base.
- **R0c predicts against the stack's top, not master** (§7.2.3).
- **`KNOWN_FIRE_DAMAGE_SITES` is recounted with the test's own pattern on the stacked tree** by the Blaze King's
  Staff slice (§7.2.1).
- **When #168 squash-merges, every slice above it is rebased with `--onto`**, and the merge procedure's
  `check-absorbed.sh` confirms the absorbed commits (`.claude/skills/merge-procedure`).

---

## 8. SLICE SHAPE (sketch; each gate is written when its numbers are ruled)

1. **Each weapon slice:**
   - one commit per weapon: its `.yml`, its new visuals, its recipe if Q-P4 gives one, and the count edits
     (§7.2);
   - the golden-lore regeneration last, once;
   - gate rows, game mode on each:
     - R0a–c;
     - the tooltip;
     - one hit on a GS-100 `/rpg spawn zombie` read by `PLAYERHIT` against the ruled number;
     - Short Bow and Scattershot: knockback distance **recorded, not predicted**;
     - Knell Beam: two zombies in line, **both** hit;
     - Blaze King's Staff: a Scorch reading after Smolder.
2. **Each capability slice ships first, with its core unit tests and a reddening mutation**, and with no content,
   or with its first consumer only.
3. **Each mob slice:**
   - the mob files;
   - `/rpg spawn <id>` rows in the End **and** the overworld, so M25's 300 against 100 is read, not assumed;
   - a `MOBSEED` line per mob;
   - for scripted mobs, the behaviour rows.
4. **The dragon fight is its own thread, after the mobs.**

---

## 9. BUILD ORDERS — PROPOSED. BEN RULES THE ORDER

### 9.1 The engine capabilities, named, and who needs each

| id | capability | needed by | shared with |
|---|---|---|---|
| **E1** | **full-charge condition** on a melee hit's effects | Bone Club (the count), Falchion (Decay) | — |
| **E2** | **combo counter**: per-wielder, idle and swap resets, the Nth-hit multiplier | Bone Club | builds on E1 |
| **E3** | **owned flat DoT status kind** | Falchion (Decay); a `wither` `applies_status` if Ben wants it (Q-P3) | the Phage/Goon/Commander family's wither theme later |
| **E4** | **pierce on `ray`** | Knell Beam | — |
| **E5** | **aim-locked wind-up** with a telegraph visual | Knell Beam | **the Knell mob's beam** (§2.5), so build it to be castable by a mob |
| **E6** | **click-fired yaw fan** (expose `DrawFan`) | Scattershot, **only if Q-P10 is a new item** | — |
| **MB1** | **mob body fields**: equipment (main hand, off hand, armour), scale, speed multiplier, `attack_damage` | all five mobs | **closes M17** if Ben rules `attack_damage` (Q-P11) |
| **MB2** | **mob on-death effects**: spawn N of a mob id; a burst at the corpse | Phage (split), Goldrot Goon (blast) | needs Q-P12 for the blast's price |
| **MB3** | **mob abilities**: a periodic cast by a mob at its target, through `CastExecutor` with the mob as caster | Knell (beam), Phalanx (swirl) | reuses E5; the swirl needs player-seeking homing (§6 F14) |
| **MB4** | **mob reactions**: pre-damage hooks (dodge, one-shot shield) and blink or invisibility | Knell (dodge), Phalanx | (c) |
| **MB5** | **mount pairing and a movement state machine** | Skreel + mount | (c); `MobGoals` exists in the jar (§1), cfde822 did it with `setVelocity` loops |

### 9.2 WEAPONS — proposed order

1. **W1 — Short Bow + Blaze King's Staff. Content only, (a).** No capability waits. It grows the roster at once
   (Q-P2's numbers are the only gate), and it pays the two count edits (`KNOWN_RANGER_WEAPONS`, fire sites) in one
   slice.
2. **W2 — Withered Falchion, swing and bomb, (a).** It is the **first real melee weapon**: the class with the
   fewest, and the one the `ironblade` deletion waits on. Decay is left out until E3, and the tooltip says nothing
   of it.
3. **E1 → E2 → W3 Bone Club.** The charge condition first, because two weapons need it. Then the counter. Then the
   club.
4. **E3 → W2b: Decay onto the Falchion**, gated on E1's charge condition.
5. **E5 → E4 → W4 Knell Beam.** The aim-locked wind-up first, because the Knell mob reuses it (§9.3). Then pierce.
6. **W5 Scattershot — only if Q-P10 is a new item: E6 → the item.** If *"Scattershot"* means `dragons_breath`,
   there is nothing to build.

*Why this order:*
- (a) before (b), so content ships while capabilities are planned;
- melee early, because the class and the deletion both need it;
- shared capabilities first (E1 serves two weapons, E5 serves a weapon and a mob);
- the one open identity question (Q-P10) last.

### 9.3 MOBS — proposed order

1. **MB1, mob body fields**, then **Phage + Lesser Phage + Goldrot Goon as bodies.** They are the dragon fight's
   most numerous mobs (§3.0) and vanilla-AI in cfde822, so they are nearly whole with body fields alone. This slice
   also decides M17 (Q-P11).
2. **MB2, on-death effects**, then **the Phage split and the Goon blast.** Q-P12 must be answered first.
3. **The Knell's body retune (MB1), then MB3 + E5 for its beam.** It shares E5 with the Knell Beam weapon, so
   whichever of W4 and this comes first builds E5 for both. Then **MB4's dodge.**
4. **Phalanx:** MB3's swirl (it needs §6 F14 answered), MB4's blink, invisibility and one-shot shield, and bow
   suppression.
5. **Skreel + mount:** MB5. Last, because it is the only pair, the only state machine, and the only spear (§6 F8).

*Why this order:*
- it follows the dragon fight's own escalation (wave 0 is Phages and Goons; Phalanx and Knell arrive at wave 1;
  Skreels at wave 2);
- each step adds one capability;
- the Knell sits mid-list, not first: **its shipped file is already a health pool, and its identity is (c)
  behaviour that needs MB3.**

---

## 10. QUESTIONS FOR BEN

*There is no §9 question block: section 10 keeps the number it has in the other PLAN files. Answers go under
RULINGS, verbatim, as the pick above does.*

- **Q-P1. ANSWERED by Ben 2026-09-29.** The six weapons and five mobs in RULINGS. §5 keeps the proposal as the record.
- **Q-P2. Numbers: which precedent governs a port?**
   - **(A)** Ben rules every blank against the Boltor. This is the brief's default.
   - **(B)** The flint/lapis precedent: carry the cfde822 numbers, converted from GS-1 bases, and adjust.
   - (§6 F1.) Either way, every blank is Ben's.
- **Q-P3. The Falchion's Decay:**
   - (i) a vanilla `potion: wither` stand-in;
   - (ii) the faithful owned DoT (E3), and should the `wither` element also `applies_status` it;
   - or (iii) drop it?
   - (§6 F2.)
- **Q-P4. How is each weapon obtained?** A recipe (which shape?), or `/rpg give` only until loot exists? (§6 F11.)
- **Q-P5. Should Smolder and the Falchion bomb hit other players?** Every burst does today. cfde822 skipped party
   members. (§6 F6.)
- **Q-P6. Order in the stack:** the base is ruled (stacked on #168, §7.3). Where does this thread sit relative to the
   Nexus-polish and level-bonus slices? If the batch authors fire and another slice does too, the lower one owns the
   first recount of `KNOWN_FIRE_DAMAGE_SITES`.
- **Q-P7. RETIRED:** no tome was picked.
- **Q-P8. ANSWERED in direction:** the five mobs come first, for the dragon fight. **Open:** the order in §9.3.
- **Q-P9. RETIRED:** undead farm animals were not picked.
- **Q-P10. "Scattershot":** a second, separate item porting cfde822's 5-arrow yaw fan (E6 plus the item), or the
   shipped `dragons_breath`? (§2.4.)
- **Q-P11. M17, the Knell's damage:** does the mob body-fields slice (MB1) add `attack_damage` and so close M17? If so,
   the Knell's number is Ben's, and so is whether GS/100 applies to it (`PLAN-mob-scaling.md` §6 F15's open reading).
- **Q-P12. Pricing a custom mob's scripted hit** (the beam, blast, swirl and leap): an authored literal × GS/100 (M24's
   custom arm), or something else? (§6 F13.)
- **Q-P13. The Knell's identity:** port the beam and the dodge (MB3/MB4), or keep it a health pool until the dragon
   fight needs it?
- **Q-P14. The Skreel's mount:** is it its own mob id (for example `withered_mount`, with its own HP and nameplate), or
   part of a `skreel` definition?
