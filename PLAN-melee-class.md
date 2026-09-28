# PLAN — THE MELEE CLASS: THE FIRST MELEE CELL, AND SUNDER

**Phase 1: SURVEY ONLY. No Java, no content, no boot.** Read against master **`2d60e9a3`**
(`2d60e9a3a2b76dcc92ac16bbe6fa3468337580c1`, quoted from `git ls-remote origin master` on 2026-09-28, #167). Citations name METHODS and SECTIONS, never line numbers. The short
paths are `PLAN-build-system.md`'s: `P/` = `paper/src/main/java/io/github/butterflysmp/rpg/paper/`, `C/` =
`core/src/main/java/io/github/butterflysmp/rpg/core/`, `content/` = `paper/src/main/resources/content/`.

**Line budget: predicted ~600 lines, stated before writing.** The actual count is in the PR body, measured with
`wc -l`.

**The brief is not in this repo.** It came from the seat in two messages and is deliberately not committed.

**Engine quotes are read from the pinned jars, not from memory.** The server jar is
`run/versions/26.1.2/paper-26.1.2.jar`, and its `META-INF/MANIFEST.MF` reads `Implementation-Version:
26.1.2-74-e4e17fc`, `Build-Number: 74`: the same build as `pom.xml`'s `paper.version` `26.1.2.build.74-stable`.
The API jar is `paper-api-26.1.2.build.74-stable.jar` from `~/.m2`. Both were read with `javap -c -p` (JDK 26.0.1).
**A claim marked *not read from the jar* is a vanilla constant quoted from memory**, and a gate row reads it before
anything rests on it.

---

## RULINGS — DECIDED; NOT RE-DERIVED HERE

**Ben's pick (via the seat, 2026-09-28): the next thread is the Melee class.**

**Ben's rulings A and B (via the seat, 2026-09-28), recorded verbatim:**

- **A. The first cell is `melee_fire`.** (Section 2 of the survey drops the void comparison. Still say what
  void_slash's fate is: stays unpooled, or moves later.)
- **B. Ben's first Melee ability: SUNDER, element kinetic.** *"The player leaps forward and slams into the ground,
  dealing damage and knocking enemies back."*

**The seat's proposal on B, to be confirmed or corrected (§4): Sunder is CLASS-WIDE, in a new
`content/builds/melee.yml`,** the way `recall` sits in `ranger.yml` (`PLAN-build-system.md` §7.1), so it is offered in
every melee cell, whatever the element. **The other `melee_fire` slots (1 more Active, the Ultimate, Aspects) ship as
placeholders unless Ben names them. Fragments reuse the shared five.**

---

## 1. THE SEAT'S PREMISES, EACH CONFIRMED OR CORRECTED FROM SOURCE

| premise | verdict | the source |
|---|---|---|
| No `content/builds/melee*.yml`, so the Build screen has no Melee cell | **CONFIRMED** | `content/builds/` holds `mage_fire.yml`, `ranger.yml` and `ranger_fire.yml`. `PoolLoaderTest.exactlyTheTwoFirePoolsShip` pins exactly that list, and `registry.size() == 2` |
| Only `ranger_fire` and `mage_fire` ship, plus the class-wide `ranger.yml` | **CONFIRMED** | the same test |
| Melee weapons exist and work: `ironblade` and `emberblade` | **CONFIRMED, with a standing-decision caveat.** Both are `class: melee` with `attack_damage` (8 and 7), `attack_speed: 1.6` and `sweep: 0.5`. **`ironblade` is in the deletion set** (CLAUDE.md, *NEW CONTENT CITES THE BOLTOR*). Nothing in this plan derives a number from it | `content/weapons/ironblade.yml`, `emberblade.yml` |
| `brawlers_gauntlet` is a melee weapon | **CORRECTED: it is a class-slot ACCESSORY**, not a weapon (`slot: class`, `class: melee`, `class_damage: 3`, `crit_damage: 0.25`, `max_mana: -10`). Its header reads *"NOBODY CAN WEAR THIS YET, and that is RULED"* (PLAN-accessories RULINGS Q1). That header goes stale the day a melee cell ships (§8) | `content/accessories/brawlers_gauntlet.yml` |
| the melee-class enchants | **ONE: Sharpness** (`class: melee`, `value_by_level [5, 10, 15]`). The other nine are `mage`, `ranger`, `shield`, `armor` or `universal` | `content/enchants/*.yml`, the `class:` line of each |
| `void_slash` is the only `CastSpec.Melee` ability, element void, in no pool, carrying `soaked_TEMP` | **CONFIRMED.** One qualification: the two melee WEAPONS' `left_click` triggers are also `type: melee`, but they never reach the arc (§6.1) | `content/abilities/void_slash.yml`; `BasicMelee.isVanillaDriven` |

---

## 2. THE PATH A MELEE CELL TAKES, AND WHAT ASSUMES RANGER OR MAGE

### 2.1 The path, file to cast

1. **`content/builds/melee_fire.yml`** (and `melee.yml`, §4) are copied to `plugins/Rpg/content/` by
   `RpgPlugin`'s `saveDefaultContent`.
2. **`PoolLoader.loadAll`** makes two passes. Pass 1 parses every file with no `element:` as a class file
   (`parseClass`: the stem must equal `class:`, and the keys are limited to `CLASS_FILE_KEYS` = class, actives,
   ultimates). Pass 2 parses each cell (`parse`), merges its class's ids in through `ClassPool.mergeUltimates` /
   `mergeActives`, and refuses unknown abilities and fragments. It also refuses a behaviour fragment or an aspect
   whose target the pool does not offer.
3. **`PoolDefinition`'s compact constructor** requires at least 1 ultimate and at least 2 actives (after the
   merge), no duplicates, no id in both roles, and a default drawn from the lists.
4. **`PoolRegistry.register`** keys the pool on `CellKey(classId, elementId)`, compared exactly.
5. **`ContentValidator.validatePools`** checks the ELEMENT only (`checkElement`). **The class id is never
   checked against anything** (§2.3, finding F4).
6. **The Build screen.** `BuildPickerMenu`'s CLASS options are `BuildRules.classes(pools.all())`, and its ELEMENT
   options are `BuildRules.elementsFor`. A class pick goes through `BuildRules.elementAfterClassPick` into
   `ProfileService.setCell`. The label is `BuildMenu.capitalised(classId)`, so "melee" renders as **"Melee"**. The
   class icon is `Material.NAME_TAG` for every class.
7. **The loadout.** `Stones.equippedFor` → `LoadoutResolution` → the saved loadout, or the pool's `default`.
8. **The cast.** `StoneCaster.cast` → `AbilityService.cast(caster, id, aim, castable, derive)` →
   `StoneCaster.run` → `DashAim.resolve` → `CastExecutor.execute` on the aim's region.

### 2.2 What assumes ranger or mage: NOTHING IN MAIN CODE

`git grep -nE '"(ranger|mage|melee)"' -- '*/src/main/*'` returns **seven** lines at `2d60e9a3`. **None of them is a
class-id branch.** Three are the cast-type token `"melee"` (`AbilitySchema`'s parse and its volley refusal, and
`CastExecutor.fireInner`'s volley refusal), and four are javadoc or comments (`AccessorySlots`, `CellKey`,
`EnchantLoader`, `PlayerProfile`). **Positive control:** the same pattern over `*/src/test/*` returns 120 lines
across 20 files, so the grep can see a class literal when there is one.

Every class-keyed decision in main code goes through one of three things, and each is class-generic:

- **pools:** `BuildRules.classes` / `elementsFor`, from whatever pools loaded;
- **the weapon/enchant axis:** `WeaponClass` (`MELEE`, `RANGER`, `MAGE`) and `GearClass.of`, both of which already
  have MELEE;
- **accessories:** `AccessorySlots.contributes(accessory, profileClass)` compares the profile's class string to
  the accessory's `classToken` exactly.

### 2.3 What would REFUSE or BREAK on a new class id

| site | what happens with `melee_fire.yml` shipped | verdict |
|---|---|---|
| `PoolLoaderTest.exactlyTheTwoFirePoolsShip` | **reddens**: it pins the directory listing and `size() == 2` | **updated in the content slice**, and renamed (it will no longer be "two" or all "fire") |
| `PoolLoaderTest`'s bundled-pool rows | `theBundledFireRangerPoolLoads` and `theBundledFireMagePoolLoads` are unaffected. A `theBundledFireMeleePoolLoads` row is owed, in their shape | new test |
| `PoolDefinition` | refuses a melee cell with < 2 actives after the merge. **With Sunder class-wide, the cell needs only ONE own Active** | a content rule, not a break |
| `PoolLoader.parse`'s behaviour-fragment check | **refuses `fragment_ember_cache` in a melee pool** (it targets `recall`, which a melee pool does not offer). The shared five are stat fragments and pass | content rule: list only the five (§7) |
| `ClassPool` | a `melee.yml` that offers nothing is refused ("offers nothing"); one with no loaded cell logs *"has no cell to offer its abilities to"* | the seat's §4 proposal satisfies both |
| **the class id** | **never validated.** `class: melle` would ship a class called "Melle" that no accessory, enchant or weapon matches, and nothing warns | **finding F4** (§9.3) |
| `AccessorySlots.contributes` | the Brawler's Gauntlet becomes **wearable and live** for a melee profile the moment a melee cell exists. **Intended** (PLAN-accessories §2: *"(a) as its own slice when Ben designs melee"*) | a gate row (MC8); the gauntlet's header comment is updated in the same slice |
| `ClassDamageModifiers.matching` | keys on the **held weapon's** class. **With the stone in hand the held class is null, so the gauntlet's `class_damage 3` grants 0 to every stone cast** | **finding F1, player-facing** (§5.4) |
| the tests that name `"ranger"`/`"mage"` (`BuildRulesTest`, `PoolRegistryTest`, `AccessorySlotsTest`, `LoadoutResolutionTest`, storage tests) | all are **hand-built fixtures**, not the shipped directory. Unaffected. `AccessorySlotsTest` already asserts a melee profile does NOT contribute a ranger accessory | none |
| lore | `BuildMenu.capitalised` → "Melee". `WeaponClassLabel.of(MELEE)` already exists (the weapons' tooltips use it). The stone lore renders whatever the loadout names | none |
| layout | the class picker has 28 option cells (`BuildMenuLayout.OPTION_SLOTS`), and three classes sort as `mage, melee, ranger` | none |

---

## 3. THE FIRST CELL: `melee_fire` (RULING A)

**The void comparison is dropped, as ruled.** What `melee_fire` costs:

- **The element does work.** `elements/fire.yml` declares `applies_status: scorch`, so **every fire-element hit
  from the cell's placeholders accrues Scorch** (`ElementAccrual.forHit`, called from `BukkitCombatant.applyDamage`).
  **Sunder is kinetic** (ruling B) and accrues nothing. So in `melee_fire` the Sunder hit and the placeholder hits
  behave differently on the Scorch bar, and a gate row reads both (MC12).
- **The fire abilities that exist are all someone else's shape**: `ember_step` (Mage dash), `solar_grenade` (Mage
  burst, carrying `rooted_TEMP`, which `lingering_sun` depends on) and `solar_lance` (a ray, in both current
  pools). §7 proposes a new placeholder rather than borrowing one.

**`void_slash`'s fate — RECOMMENDATION: it STAYS UNPOOLED.** Ruling A gives it no cell, and moving it later is its
own decision, with three costs named now:

1. **`soaked_TEMP` is baked into its burst.** It is owed removal by the content pass (NEXT.md, *Deferred,
   deliberately*). It is **not** test-guarded, so it comes out clean, **unless an aspect is ever written against
   its `status` field**. That is exactly how `lingering_sun` became coupled to `solar_grenade`'s `rooted_TEMP`
   (`PLAN-build-system.md` §6 item 10).
2. **Two carried gate rows use it as THE "in no pool" witness:** `GATE-build-stone.md` (*"`/rpg cast void_slash`
   (in no pool) casts"*, and the deop row *"void_slash is not in your loadout"*) and `GATE-build-storage.md` (the
   same pair). Pooling it falsifies those predictions for a player of that cell. **Re-point them at another unpooled
   ability** (e.g. `recall_updraft`) in the same change.
3. **A void ability in a fire cell** is the objection ruling 20 settled for `arc_surge`: it makes "the cell's
   element" mean nothing. It fits only a future `melee_void`.

---

## 4. A CLASS-WIDE `melee.yml` — THE SEAT'S PROPOSAL, CONFIRMED

**CONFIRMED, with no code change.** `PoolLoader`'s class-file path is class-generic (§2.1 step 2). A file
`content/builds/melee.yml` reading

    class: melee
    actives:
      - sunder

is parsed by `parseClass` (stem `melee` == class `melee`; only permitted keys) and merged into every
`melee_<element>.yml` after the cell's own ids. The rules that bind it, all already enforced:

- **One fact, one home:** listing `sunder` in `melee_fire.yml` as well is REFUSED (`ClassPool.merge`), not
  de-duplicated.
- **The cell's `default` may name `sunder`** even though the cell does not list it: the merge runs before
  `PoolDefinition` checks the default (the `ranger_fire` → `recall` precedent).
- **Without `melee.yml`, a `melee_fire.yml` whose default names `sunder` is refused.** That mirrors
  `PoolLoaderTest.theShippedFireRangerPoolNeedsItsClassFile`, and a melee twin of that row is owed.
- **Ranger and Mage are offered no Sunder** (the `aClassFileReachesOnlyItsOwnClass` shape, extended to three
  classes).

**What else could go in it:** only `ultimates:` (a class-wide Ultimate) — the key exists and is merged the same
way. **Nothing is proposed for it.** Fragments, aspects and a default are refused in a class file by design
(`CLASS_FILE_KEYS`).

---

## 5. THE STONE AND THE SWORD

### 5.1 What each input does TODAY, stone in hand vs a melee weapon in hand

"Sword" means `emberblade` (a `right_click` Fireball) or `ironblade` (no `right_click`). Every row is survival.

| input | **Ability Stone** in the main hand | **emberblade / ironblade** in the main hand |
|---|---|---|
| **left, air** | `onStoneSwing` (`PlayerArmSwingEvent`, main hand) → `StoneCaster.onSwing` → **decided one tick later** by `SwingGuard.castsActive1` → **Active 1**. The packet path (`WeaponSwingListener.onSwing` → `WeaponFire.attempt`) finds no `weapon_id` and does nothing | `WeaponSwingListener.onSwing` → `Quivers.tryReloadHeldWeapon` (no quiver: false) → `WeaponFire.attempt("left_click")`. It records a Q7 INPUT, runs the broken gate, then **`BasicMelee.isVanillaDriven` → empty. Nothing fires.** The swing animates |
| **left, entity** | `onPrePlayerAttack` **cancels the attack** (no token hit, no hurt flash); the swing casts **Active 1** | vanilla's crosshair attack: `onPrePlayerAttack` records the charge (`meleeHits.record`), then `onPlayerMeleeAttack` lands the weapon's `weapon_damage` payload (`CastExecutor.landBasicMelee`). A full-charge swing also runs `onPlayerSweepAttack` at `sweep: 0.5` |
| **left, block** | `onStoneBlockDamage` cancels (no crack, no break); a swing arrives **every tick** while held, so it **re-casts Active 1 whenever the cooldown allows** (ruling 19) | vanilla mining |
| **right, air/block** | `onRightClick`'s stone branch (after the star, before the hijacked blocks) → **Active 2**; sneaking on a hijacked block opens it instead (ruling 14) | `onRightClick` → `openHijackedBlock`, then `WeaponFire.attempt("right_click")`: **emberblade casts its Fireball** (40 mana, 60 ticks); ironblade binds none → vanilla |
| **right, entity** | `onNexusGiveToEntity` (widened) → **Active 2** | vanilla interact |
| **Q** | `onNexusDrop` cancels, `recordDropAttempt`, → **the Ultimate**; the same-tick swing is refused by the guard | **the sword is DROPPED** (vanilla; `onNexusDrop` returns for anything that is not the star or the stone). The same-tick swing reaches `WeaponFire.attempt("left_click")` and ends at `isVanillaDriven` → nothing |
| **F** | `onNexusSwapHand` refuses | vanilla offhand swap |

**So the Ultimate is reachable ONLY with the stone in hand**, and **Q on a sword throws the sword**.

### 5.2 §6.9 (Q on a held weapon fires its left-click trigger) — CORRECTED: it does NOT bite the shipped melee weapons

`PLAN-build-system.md` §6 item 9 is true, and its mechanism is the Q-drop's same-tick arm swing reaching
`WeaponSwingListener`. **But for both shipped melee weapons the swing ends at `BasicMelee.isVanillaDriven` and
returns empty,** because their `left_click` is a `weapon_damage` on `type: melee`: the vanilla attack event owns
that hit, not the swing. It counts a Q7 INPUT and does nothing else. Where §6.9 DOES bite:

- **a quiver weapon:** `Quivers.tryReloadHeldWeapon` runs FIRST in `onSwing`, so Q on a Boltor or Locust starts a
  reload;
- **any weapon whose `left_click` is NOT a basic attack** — for example a future melee weapon with a costed
  left-click special, or a staff. That weapon casts on Q.

**For Melee, the Q problem is the DROP, not the left-click.** A Melee player who reaches for Q with the sword
out loses the sword to the ground and casts nothing.

### 5.3 The swap has a vanilla cost — READ FROM THE JAR

`net.minecraft.world.entity.player.Player.tick` (build 74, bytecode offsets 284-324): if
`!ItemStack.matches(lastItemInMainHand, mainHand)` **and** `!ItemStack.isSameItem(lastItemInMainHand, mainHand)`,
it calls **`resetAttackStrengthTicker()`**. The same sequence is in `Player.detectEquipmentUpdates` (offsets
17-53), which is gated on Paper's `updateEquipmentOnPlayerActions`. `isSameItem` compares the item TYPE, and echo
shard is not a sword. **So every stone → sword swap resets the attack charge**, and the first swing back is
under-charged. `AttackCharge` scales the hit by that charge, and a sweep needs full charge.

*How long a full charge takes at `attack_speed: 1.6` — about 12.5 ticks (20 / 1.6) — is vanilla's formula
quoted from memory, **not read from the jar**. Gate row MC6 reads the effect, not the tick count.*

### 5.4 FINDING F1 (player-facing): a stone cast carries NONE of the sword's bonuses

A stone cast snapshots the caster with the stone in hand. Three bonuses key on the **held** item:

| bonus | where it is read | with the stone held |
|---|---|---|
| class damage (the Gauntlet's `+3 Melee`) | `ClassDamageModifiers.matching(heldClass, …)`: *"`heldClass` is null when the hand holds nothing, or nothing of ours. That returns an EMPTY map"* | **0** |
| Sharpness | `DamageEnchantItems`: *"MAIN HAND ONLY"*, and the enchant's class vs the held weapon's class | **0** |
| gear score on a literal | `EffectApplier`'s `Damage` arm: `GearScore.scaledDamage(d.amount(), caster.triggerScore())`, where `triggerScore` = `PaperCombatWorld.triggerScoreOf` → `GearScoreItems.heldScore`. The stone has no stamp → `GearScore.orAbsent` → `ABSENT` = **100** | **×1**, whatever the player wears |

**Plus a reconcile lag.** Class damage and the enchant percentage are stats reconciled by `PlayerHealthSystem`
every `RECONCILE_PERIOD_TICKS = 5`. So a stone cast within about 5 ticks of switching away from the sword may still
carry the sword's grants. The gear score does not lag: `triggerScoreOf` reads the live hand.

**This is not Melee-specific.** A Ranger's or Mage's Actives already cast at score 100 with no class damage.
**It bites Melee hardest**, because the Gauntlet is a class-damage accessory whose class can now be worn, and it
will never touch a Melee player's Actives. **A search of `NEXT.md`, `PLAN-build-system.md`, `PLAN-mob-scaling.md` and
`GATE-build-stone.md` found this recorded nowhere.** It is a question for Ben (§10 Q6), not something this plan
fixes.

### 5.5 The options — MECHANISM ONLY, NONE PICKED

| option | what changes | what it collides with |
|---|---|---|
| **O1. Status quo: swap** (number keys / scroll) | nothing | §5.3's charge reset; F1; Q on the sword drops it |
| **O2. A held melee weapon also carries the stone's inputs** (e.g. right = Active 2, Q = Ultimate, while a melee weapon is in hand; left stays the swing) | `onRightClick` and `onNexusDrop` gain a "melee weapon held" arm; the Q-drop of that weapon is cancelled | **ruling 2** (the stone is the item that casts); `emberblade`'s own `right_click` Fireball (one input, two owners); §6.9's same-tick swing (harmless here, §5.2) |
| **O3. The stone in the OFF hand** | the stone's inputs are read from the off hand while a sword is in the main hand | **ruling 2's "only on the hotbar"**; `onNexusSwapHand` refuses F today; the off-hand right-click is cancelled today (`onRightClick`'s first arm) |
| **O4. Sneak-modified inputs on a melee weapon** (sneak + right = Active 2, sneak + Q = Ultimate) | as O2, gated on `isSneaking` | the hijacked-block rule (sneaking is the weapon's escape hatch in `openHijackedBlock`); still ruling 2 |
| **O5. Actives resolve bonuses from the best melee weapon in the hotbar** (addresses F1 only, not the swap) | the stone-cast snapshot reads a hotbar weapon for class, enchant and score | "held" is the whole gate the three bonuses share; `triggerScoreOf`'s contract |

**O1 needs no code. O2-O4 each change ruling 2, so they are Ben's. O5 is independent of the others.**

---

## 6. `CastSpec.Melee` IN LIVE PLAY

### 6.1 What reaches it today: NOTHING SHIPPED CASTS IT FOR A PLAYER

- **No pool lists `void_slash`**, so no stone casts it. An operator's `/rpg cast void_slash` reaches it through
  `AbilityService.castUnchecked` (ruling 10). A non-op is refused (*"not in your loadout"*).
- **The weapons' `left_click`** is `type: melee`, but `WeaponFire.attempt` returns empty at
  `BasicMelee.isVanillaDriven`, and the vanilla attack lands through `CastExecutor.landBasicMelee`, which *"minus
  `meleeTarget`"* never searches an arc (its javadoc).
- `CastExecutor.fireInner` refuses Melee inside a volley (`notRepeatable("melee")`), and so does `AbilitySchema`.

### 6.2 What the targeting does — `CastExecutor.dispatch`'s Melee arm and `meleeTarget`

1. `meleeTarget` searches `world.combatantsNear(aim.origin(), melee.reach())`, where the origin is the **EYE**.
2. For each candidate (not the caster), it takes `toCandidate = candidate.position() − eye`, where
   **`position()` is the FEET**. It rejects the candidate if `normalize(toCandidate) · aim.direction() <
   cos(arcDegrees / 2)`, then keeps the nearest one whose `lineOfSightClear(eye, candidate.sightPoint())` holds (the
   line-of-sight check gates the bound, as its javadoc requires).
3. **Hit:** `detonate` at the TARGET's position, so the direct `Damage` lands on it, and a `Burst` centres on
   its feet.
4. **Miss:** `detonate(…, null, aim.pointAt(reach))`, which is 3.5 blocks along the look direction **at eye height**.
   A null target skips every Targeted effect, so **the direct damage is dropped and only the burst lands**, centred
   on that point.
5. Durability is charged only on connect, and only for weapon charges (`charges && target != null`). An ability
   never charges (`aMeleeAbilityNeverChargesAUseEvenOnConnect`).

### 6.3 Two properties the unit tests cannot see — READ FROM THE JAR AND EXECUTED

**(a) THE SEARCH IS A BOX, NOT A SPHERE — FINDING F2, and it contradicts a recorded claim.**
`PaperCombatWorld.combatantsNear` calls `world.getNearbyEntities(center, r, r, r)`. In build 74,
`CraftWorld.getNearbyEntities(Location, D, D, D, Predicate)` builds `BoundingBox.of(center, r, r, r)`, and
`BoundingBox.of` (api jar) is `center ± r` on each axis. That becomes an `AABB` for `ServerLevel.getEntities`, which
returns entities whose hitboxes **intersect** it. So **the live query is a 2r cube that catches hitboxes**.
`FakeWorld.combatantsNear` is a **sphere on the position point** (`distanceSquared(center) <= r²`), and
`CombatWorld.combatantsNear`'s contract says *"within `radius`"*.

- `meleeTarget` applies **no distance check of its own**, so live reach runs out to the box corner:
  `√3 × 3.5 ≈ 6.06`, plus half a hitbox. **`CastExecutorTest.meleeMissesBeyondItsReach` is a hollow fixture in
  SPACE**: it puts the target on an axis at 5, where the box and the sphere agree.
- **The same query backs every `Burst`** (`EffectApplier.applyToNearby`). So every authored burst radius is a
  cube half-extent live, including the rulings' `radius: 3.0` / `4.0` embers.
- **`PLAN-dragons-plume.md`'s P3 note states the opposite**: *"`combatantsNear(pos, 10)` is a **radius-10
  sphere**"*. The jar says it is a box. That note is a record this finding corrects (§9.3).

**(b) POINT-BLANK TARGETS FALL OUTSIDE THE ARC.** The cone is measured from the eye to the feet. With level aim, the
angle down to a target's feet at horizontal distance `d` is `atan(eyeHeight / d)`. **Executed in `jshell`** with
the standing eye height 1.62 (*not read from the jar*) and `arc_degrees: 120` (half-angle 60°):

| `d` (blocks) | 0.50 | 0.80 | 0.90 | **0.935** | 1.00 | 2.00 | 3.10 |
|---|---|---|---|---|---|---|---|
| dot | 0.2949 | 0.4428 | 0.4856 | 0.4999 | 0.5253 | 0.7771 | 0.8863 |
| in the arc | no | no | no | **the edge** | yes | yes | yes |

**So a mob hugging the caster (feet closer than ~0.94 blocks, aim level) is MISSED by the direct hit**, and only a
burst centred 3.5 ahead reaches it. Looking down widens it. **This is geometry from the code and an executed
expression. No boot has seen it** (MC13).

### 6.4 What is still untested

- **No gate row has ever read `meleeTarget`'s geometry.** `void_slash` appears in `GATE-build-stone.md` and
  `GATE-build-storage.md` only as the dev-cast / not-in-your-loadout witness, and in `GATE-element-accrual.md` only as
  the void glyph.
- **Covered by unit tests** (`CastExecutorTest`, against `FakeWorld`'s sphere): nearest in the arc, behind the
  caster, full-cone width, beyond reach, self-exclusion, the walled-nearer / clear-farther order, all walled, the
  sight trace from the eye to the target's eye, and use charging.
- **Not covered anywhere:** the box (F2), point-blank (§6.3b), the miss burst's eye-height centre, void_slash's
  `knockback 0.6` on a live mob, and a Melee cast from the STONE (it has never been in a loadout).

---

## 7. PLACEHOLDERS VS DESIGN

**Proposed `content/builds/melee_fire.yml`.** Every entry marked PLACEHOLDER is a `# PLACEHOLDER -- Ben designs`
line, so a grep finds it. Every id not shipped at `2d60e9a3` is ILLUSTRATIVE:

    class: melee
    element: fire
    display_name: "<gold>Fire Melee</gold>"        # PLACEHOLDER name -- Ben's ("Fire Warrior"? "Fire Brawler"?)
    ultimates:
      - ultimate_placeholder_melee                 # PLACEHOLDER (new file)
    actives:
      - active_placeholder_melee                   # PLACEHOLDER (new file); + sunder, CLASS-WIDE (melee.yml)
    fragments: [fragment_vigor, fragment_focus, fragment_keen, fragment_mending, fragment_ward]   # the shared five
    aspects:   [<two placeholders, below>]
    default:
      ultimate: ultimate_placeholder_melee
      actives: [sunder, active_placeholder_melee]  # left, right -- PROPOSED; which hand Sunder sits on is Ben's feel

| slot | proposal | why |
|---|---|---|
| **Ultimate** | a new `ultimate_placeholder_melee`, the two shipped placeholders' pattern: built only from existing `CastSpec`/`EffectSpec`, `cooldown_ticks: 1200`, `mana 60`, header *"NOT A DESIGN AND ITS NUMBERS ARE NOT A PROPOSAL"*. Shape: **a `type: self` burst** (a ground-pound around the caster), so Q reads differently from Sunder's leap and the Active's arc | the ranger (ray) and mage (projectile) placeholders each chose a distinct shape on purpose |
| **Active 2** | a new `active_placeholder_melee`: **`type: melee`, fire damage, a small burst**. It is the ONLY way `CastSpec.Melee` gets any live coverage from a stone (§6.4) | *Rejected:* `void_slash` (§3: void, `soaked_TEMP`, the witness rows); `solar_grenade` (drags `rooted_TEMP`); `ember_step` (the Mage's commit dash, too close to Sunder) |
| **Active 1** | **Sunder**, class-wide (ruling B) | — |
| **Aspects** | **two placeholders, numbers-only (`modify`), each targeting a PLACEHOLDER** (one on the Active, one on the Ultimate). **None targets Sunder**: an aspect on Ben's ability is design | ruling 23 (unequippable) comes free with the existing picker. `PoolDefinition` would also accept zero, but then the aspect cells show nothing to gate |
| **Fragments** | the shared five; **not** `fragment_ember_cache` (refused, §2.3) | ruling 11: one of each |

**`void_slash`'s `soaked_TEMP` coupling if it ever moves into a pool:** the fixture becomes part of a player build
(a Soaked slow on every swing), exactly as `solar_grenade`'s `rooted_TEMP` did. Remove the fixture **before**
pooling. And if an aspect ever targets its status, the two must go in one change (the `lingering_sun` shape).
**Recommended: not now** (§3).

---

## 8. SUNDER'S MECHANISM (RULING B)

### 8.1 What `CastSpec.Dash` does today, and the forward direction

`CastSpec.Dash(distance, speed, lift, direction, safeLanding)`. `CastExecutor.dash` does four things:

1. It applies **one impulse** through `CombatantHandle.applyImpulse`, which **REPLACES velocity**
   (`BukkitCombatant.applyImpulse`: `entity.setVelocity`): `aim.direction() × speed + (0, lift, 0)`.
2. If `safeLanding`, it calls `armSafeLanding()`, queued after the impulse on the same entity scheduler.
3. It finds the dash's hits along a `SweptLine` of length `distance`, from the **pre-dash feet**.
4. **`effects.applyToSet(ability.onHit(), …, hits, origin, facing)` — the whole `on_hit` fires AT TAKE-OFF.**

`DashAim.resolve` picks the direction, and **there are TWO**:

| `direction` | resolves to | WASD |
|---|---|---|
| `movement_else_forward` (the default, Ember Step) | `movementDirection(player)`: the WASD keys as a ground-plane vector off the yaw; **no key → forward along the flattened facing** | **steers it** |
| `reverse_facing` (Recall, the Updraft leap) | `reverseFacing(yaw)` = `directionFromInput(yaw, false, false, false, false).negate()` | ignored |

**"Leap forward, ignoring WASD" does not exist.** The forward equivalent of `reverse_facing` is the same expression
without `.negate()`: `directionFromInput(yaw, false, false, false, false)`. Two options:

- **(a) a new `DashDirection.FACING`.** One enum arm, one `DashAim.resolve` arm (an exhaustive switch, so the
  compiler names the site), and one `AbilitySchema.parseDashDirection` token. `CastExecutor.dash`'s `facing` line
  already treats every non-`REVERSE_FACING` direction as forward, so it needs no change.
- **(b) reuse `movement_else_forward`.** No code, and a player can steer the leap sideways or back with WASD. **That is
  a feel question for Ben** (§10 Q1).

**The calibration Sunder inherits from §7.8 (measured, not predicted):**

- **ground friction on the first tick halves a grounded dash.** Recall's `speed 1.1` gives 4.64 at the first landing
  and 5.25 at rest, grounded; about 2× mid-jump;
- movement keys move it about ±1 block (air control);
- pitch does nothing;
- the per-tick simulation was **exact for the small leap** (predicted 4.43 / 1.94, measured 4.43 / 1.95) and
  **wrong for Recall's big impulse** (predicted 14.6, measured 9.69).

**So Sunder's numbers are tuned at a boot (M3), not simulated.**

### 8.2 THE SLAM: THERE IS NO ON-LANDING EFFECT HOOK

**Measured from the code:**

- **A dash's `on_hit` fires at TAKE-OFF** (§8.1 step 4). An authored `burst` / `damage` / `knockback` on Sunder
  today would hit **where the player stood, on the cast frame**, plus anyone on the swept line. That is the opposite
  of a slam.
- **The Updraft leap's embers are thrown at take-off too** (`recall_updraft.yml`: *"Thrown at TAKE-OFF"*), and they
  detonate on a **fuse** (`fuse_ticks: 30`) at the item's **live** position. `PaperCombatWorld`'s thrown-item javadoc:
  *"there is no landing detection"*.
- **§6.12, answered: why the front fan lands 9-12 out against ruling 28's 3-4.** It has nothing to do with the leap
  landing. The embers are separate items thrown from the take-off feet along facing at ember `speed 0.6`
  (Rekindle's throw, ruling 37). The Ember Cache ring's `0.25` lands 3.16-4.23. **Throw speed alone sets the
  distance, and the leap's own landing is never observed by anything that deals damage.**
- **The ONLY landing detector in the codebase is `SafeLanding` / `SafeLandings`**, and it guards fall damage only.
  `SafeLandings.arm` schedules a per-tick `step` on the entity's own scheduler, reading `isOnGround()` and
  `getFallDistance()`. Its core machine goes `ARMED → AIRBORNE → LANDED → CLEARED`, with `TAKE_OFF_TICKS = 4` (a
  leap that never leaves the ground clears), `BACKSTOP_TICKS = 200`, the fall-absorbed arm (water, ladder,
  cobweb), and clearing on death and quit. **It detects the landing, and does nothing with it except spend a
  fall-immunity mark.**

**The options, each against the six cases the seat named:**

| case | **S1. Ground-contact poll** (a `SafeLanding`-shaped machine whose LANDED transition detonates an `on_land:` effect list at the LIVE feet) | **S2. Fixed-delay burst** (detonate N ticks after the cast at the caster's live position) | **S3. Burst at the PREDICTED landing point** (computed at cast from physics, detonated after the predicted airtime) |
|---|---|---|---|
| **a wall** | the player stops and drops; the slam lands at the wall's foot. **Correct** | fires wherever the player is at N, possibly still falling or sliding | **the point is beyond or inside the wall**; the burst's radius query then reaches through it (a `Burst` is a plain radius query, `meleeTarget`'s javadoc) |
| **a ledge** | lands on the lower level, **later**; the slam is correct but late | fires mid-air above the drop | the point is on the upper level; the player is below |
| **landing in water** | **no ground contact.** The fall-absorbed arm clears it with **no slam**, unless a rule says "a water entry is a landing" | slams in the water | the point is on the surface |
| **a leap that never leaves the ground** (a ceiling) | `TAKE_OFF_TICKS` clears it: **no slam**, or a rule "a failed take-off slams in place" | slams in place at N | slams at a point the player never reached |
| **the player dies or logs out mid-leap** | the mark is dropped on death/quit, and the entity scheduler retires the task: **no slam** | an **entity**-scheduled task retires with the entity: no slam. A **region** task with a frozen position slams posthumously | a region task with a frozen point **detonates posthumously**, credited to a dead or absent caster |
| **a leap started in the air** | ARMED sees `!onGround` on tick 1 and goes AIRBORNE; the slam lands wherever it lands (~2× distance, §7.8). **Correct** | fires early relative to the ~2× flight | the prediction must model an air start, and §7.8 showed the model fails for large impulses |
| **cost** | a new effect hook (`on_land:`) on the dash, a core machine plus its tests, and the per-tick task (which exists) | the least code: an entity-scheduled delay | a physics model in core that §7.8 already falsified at Recall's scale |
| **trusted input** | `isOnGround` is client-reported (§7.6): a lying client can delay or suppress its OWN slam | none | none |

**Not picked.** S1 is the only one of the three that is right for a wall, a ledge and an air start. Its open cases
(water, a failed take-off) each need one stated rule. That is Ben's call on feel (§10 Q2), and the plan does not
choose.

### 8.3 THE DAMAGE AND KNOCKBACK PATH — THE SAME PIPELINE AS EVERY ABILITY HIT, with one correction

Whichever slam option lands, it detonates the ordinary effects:

1. **`EffectSpec.Burst`** → `EffectApplier.applyToNearby` → `world.combatantsNear(origin, radius)` (**a box live**,
   F2) → `applyToEach`, which excludes the caster.
2. **`EffectSpec.Damage`** → `HitDamage.dealt(HitDamage.hitBase(GearScore.scaledDamage(amount, triggerScore),
   enchantDamagePercent, classDamageBonus), chargeScale, critMultiplier)` → `CombatantHandle.applyDamage(…,
   DefenseRule.APPLIES, element)` → `BukkitCombatant.applyDamage` on the target's entity scheduler →
   **`ctx.stats().damage`, the custom store**. That goes to the HealthChange seam: the nameplate, the mob death
   chain, and `ElementAccrual` (kinetic accrues nothing). **Identical to `solar_grenade`'s burst and every other
   ability hit.**
3. **The gear score, both ends.** The MOB's side is its stored score: health × 5 × GS/100 (slice 1). The CASTER's
   side is `triggerScore` = **the stone's, 100** (F1). So Sunder's literal is not scaled by the player's gear.
4. **`EffectSpec.Knockback`** → the direction is `target.position − origin`, then `BukkitCombatant.applyKnockback`
   → **`entity.setVelocity(velocity + normalize(dir) × strength)`**, which ADDS to velocity. No
   `EntityKnockbackEvent` is raised (the standing decision *A TRAVELLING RANGED WEAPON…*). **For a slam on level
   ground the direction's `y` is about 0, so the push has no lift.** How far a grounded mob slides under ground
   friction at a given `strength` is **not measured** (MC11).

**CORRECTION: `MOBHIT` will NOT witness it.** `RpgListeners.traceMobHit`'s javadoc: *"`MOBHIT` -- one line per
**mob-to-player** hit while `/rpg mobtrace` is on"*. `/rpg mobtrace` today logs `MOBSEED`, `MOBHIT`, `MOBHEAL` and
`MOBREMOVE`, and **none of them is a player-to-mob hit**. The live witnesses for a Sunder hit on a mob are:

- the damage popup (the number, and its element glyph);
- the nameplate;
- `/rpg mobinfo` (chat only, per the M25 boot).

A `MOBDAMAGE`-style trace line would be new code (§10 Q8), not a reading.

### 8.4 FALL DAMAGE ON THE SLAM — THE OPTIONS, NONE PICKED (Ben's feel)

Today a player's fall reaches `RpgListeners.onEnvironmentalDamage`, whose FIRST arm cancels a FALL while
`safeLandings().consumeFall(id)` is live; otherwise `VanillaDamagePolicy.forCause(FALL)` reroutes it to custom HP.

| option | mechanism | consequence |
|---|---|---|
| **F-a. `safe_landing: true`** | exists; one YAML key | **the whole first landing is free, however far** (§7.6): Sunder off a cliff lands free |
| **F-b. no immunity** | nothing | vanilla fall damage on landing, **if** the apex is high enough to deal any (*vanilla's 3-block threshold, not read from the jar*); a low Sunder may never trigger it |
| **F-c. immunity capped by height** | `safe_landing` plus one `fallDistance` comparison in the FALL arm (§7.6 named it) | a slam off a ledge still hurts past the cap |
| **F-d. the slam consumes the fall** | with S1, one mark does both: LANDED detonates the slam **and** spends the immunity | ties F to S1 |

### 8.5 THE NUMBERS — BLANK FOR BEN, with PROPOSED starting values beside Recall's

**Every Sunder number is Ben's.** The PROPOSED column is a tuning-boot starting point only. **Nothing is derived
from a dev weapon, and nothing is final.** Cooldowns are multiples of 4 (the held-input grid).

| number | **Ben's** | PROPOSED start | Recall, for comparison (ruled) | the basis for the proposal |
|---|---|---|---|---|
| leap distance (the intended length; also the swept line) | ___ | 5 | 5 (ruling 34) | parity of reach with the class-wide Ranger dash |
| `speed` (horizontal) | ___ | 1.1 | 1.1, LOCKED (rulings 34, 39) | Recall's grounded ~5.25; direction reversed |
| `lift` (vertical) | ___ | 0.5 | 0.3 (ruling 25) | enough airtime to read as a leap. **An estimate**; the small-leap simulation was exact at 0.85 → 4.43 up, and 0.5 is not simulated |
| slam damage | ___ | — | — (Recall deals none) | **no ruled melee precedent exists.** The comparators are ruled but not melee: Ember Cache 8 in r4 (27), the leap's embers 60 in r3 (32) |
| slam radius | ___ | 3.0 | — | the ruled ember radius (32); note it is a cube half-extent live (F2) |
| knockback `strength` | ___ | 0.6 | — | `void_slash`'s authored 0.6 (**unruled and never measured**; a starting point only) |
| `cooldown_ticks` | ___ | 240 | 360 (ruling 36) | a multiple of 4; an offensive tool shorter than a retreat |
| mana | ___ | 30 | 35 (ruling 25) | below Recall's |

---

## 9. SLICES, GATES, AND WHAT IS A FINDING VS A STOP

### 9.1 The order — RECOMMENDATION: THE CELL FIRST, SUNDER SECOND

Sunder is blocked on three of Ben's calls: direction (Q1), the slam (Q2) and fall damage (Q3). **The cell is
blocked on none**, provided it ships with two placeholder Actives. So:

- **M1 — the `melee_fire` cell, playable end to end, all placeholders.** `melee_fire.yml` with
  `active_placeholder_melee` **and a second placeholder Active** (Sunder is not in it yet, so no `melee.yml`: a
  class file offering nothing is refused). `ultimate_placeholder_melee`, two placeholder aspects, the shared five
  fragments. The `PoolLoaderTest` updates, and the Gauntlet's header comment. **Content and tests only; no Java
  expected.**
- **M2 — Sunder.** The direction (Q1), the slam hook (Q2) with its core machine and unit tests first, and fall
  damage (Q3). `sunder.yml` plus `melee.yml`; the second placeholder Active leaves `melee_fire.yml`, and the
  default becomes `[sunder, active_placeholder_melee]`.
- **M3 — the Sunder tuning boot.** The `§7.8` pattern: a throwaway per-tick logger on a `spike/` branch, NEVER
  merged, and numbers moved over the `--refresh-content` loop until Ben rules them.

*The alternative, Sunder first:* M2 before M1 means Sunder is reachable only by an operator's `/rpg cast` until the
pool lands. It is gateable, but no Melee player exists to feel it.

### 9.2 Gate rows. **Game mode on every row.** R0 is PowerShell (the jar scan the build-system gates use)

**M1 — `GATE-melee-cell.md`, SURVIVAL unless a row says otherwise:**

| row | mode | prediction, written now | reads |
|---|---|---|---|
| R0a | — | the boot's `Build:` line names the tip, not `-dirty` | the jar is the slice |
| R0b | — | PowerShell: the unpacked plugin jar's class count is > 0, and `content/builds/melee_fire.yml` is present in it | the content shipped |
| MC1 | survival | the Build screen's class picker offers **Mage, Melee, Ranger** (sorted); picking Melee offers element **Fire** only | `BuildRules.classes` / `elementsFor` |
| MC2 | survival | a fresh Fire Melee shows the default loadout: Q = the Ultimate placeholder, Left / Right = the two placeholders | `PoolDefinition`'s default |
| MC3 | survival | stone: left casts Active 1, right casts Active 2, Q casts the Ultimate; the stone never leaves its slot | §5.1, stone column |
| MC4 | survival | Fire Ranger and Fire Mage loadouts are unchanged (each opened and read) | no regression |
| MC5 | survival | Q with `emberblade` in hand: **the sword is dropped** and nothing casts. RECORDED as today's behaviour, not a defect | §5.1, §5.2 |
| MC6 | survival | swap stone → emberblade, and swing at once: **an under-charged hit** (a smaller popup than a full-charge swing on the same mob), and no sweep | §5.3, the jar reading |
| MC7 | survival | the fragment picker offers the shared five and **not** Ember Cache | §2.3 |
| MC8 | survival | as Fire Melee, the Brawler's Gauntlet goes into the class slot and is live (the stats sheet shows `+3 Melee Damage`); switch to Fire Ranger → inert; switch back → live | `AccessorySlots.contributes`, PLAN-accessories §2 |
| MC9 | survival | **F1, read not fixed:** with the Gauntlet on, a melee Active cast off the stone at a zombie deals **the same** number as without it; the same hit with `emberblade` held via an operator's `/rpg cast` (op, survival) shows **+3** | §5.4 |
| MC10 | survival | the placeholder Melee Active cast from the stone at a zombie ~2 blocks ahead, aim level: the direct hit and the burst both land (two popups) | §6.2 hit |
| MC13 | survival | the same Active with the zombie **~0.5 blocks** from the caster, aim level: **only the burst lands** (one popup), per §6.3b | point-blank |
| MC14 | survival | the same Active at a zombie ~3 blocks **diagonally** off-axis (x ≈ z ≈ 3.2, aim at it): **it is hit**, though its feet are 4.53 away horizontally and 4.81 from the eye (both executed in `jshell`, eye height 1.62), beyond `reach` 3.5 | F2, the box |
| MC12 | survival | a fire-element placeholder hit on a zombie adds Scorch stacks (the nameplate burn) | `applies_status: scorch` |
| MC15 | creative | hotbar Q with the stone casts the Ultimate; inventory Q refuses (ST12's shape, re-read for a Melee cell) | the creative split |

**M2 — `GATE-sunder.md`, SURVIVAL (fall damage is survival-only):**

| row | mode | prediction |
|---|---|---|
| R0a/R0b | — | as M1; plus the slam machine's class **present** in the jar |
| SU1 | survival | Fire Melee's Active picker offers Sunder; Fire Ranger's and Fire Mage's do **not** |
| SU2 | survival | Sunder on flat ground: a forward leap, then damage and knockback **at the landing**, not at take-off (a zombie at the start point takes nothing; one at the landing takes the hit) |
| SU3 | survival | the chosen direction rule (Q1): with A held, the leap goes **forward** (FACING) or **left** (movement) |
| SU4 | survival | into a wall: the slam lands at the wall's foot, on this side only |
| SU5 | survival | off a 4-block ledge: the slam lands on the lower level |
| SU6 | survival | into water: the ruled water behaviour (Q2) |
| SU7 | survival | under a 2-block ceiling: the ruled failed-take-off behaviour |
| SU8 | survival | die mid-leap (`/kill` by console, timed): **no** posthumous slam |
| SU9 | survival | from mid-jump: ~2× distance, and the slam at the actual landing |
| SU10 | survival | fall damage per the ruled option (Q3), with HP read before and after; then a normal 6-block drop deals normal damage (the mark cleared) |
| SU11 | survival | a Sunder hit on a GS-300 `/rpg spawn` zombie: the popup equals the authored damage (score 100, F1); the nameplate drops by that amount |
| SU12 | survival | knockback: a zombie at the slam's edge is pushed away from the landing point; distance **recorded**, not predicted |

**M3's figures are recorded in its plan section with the sha and n**, like §7.8.

### 9.3 Findings for `PLAN-build-system.md` §6 (or this plan's own §6 when built) vs stops

**FINDINGS — recorded, not fixed, and none stops the survey:**

- **F1 (player-facing — raise with the seat before M1's gate):** a stone cast carries no class damage, no
  Sharpness, and gear score 100, whatever is worn (§5.4). Cross-class; worst for Melee.
- **F2 (contradicts a recorded claim):** `combatantsNear` is a hitbox-intersecting CUBE live and a point SPHERE in
  `FakeWorld`. This affects `meleeTarget`'s reach and every `Burst`. `PLAN-dragons-plume.md`'s P3 note says
  "sphere", and `CastExecutorTest.meleeMissesBeyondItsReach` is hollow in SPACE (§6.3a).
- **F3:** `PLAN-build-system.md` §6 item 9 holds, but does **not** reach the shipped melee weapons' left-click
  (§5.2). Their Q cost is the drop.
- **F4:** a pool's `class:` is never validated (§2.3). A typo ships a class that nothing matches.
- **F5:** `brawlers_gauntlet.yml`'s *"NOBODY CAN WEAR THIS YET"* header is falsified by M1 and is edited there.
- **F6:** if `void_slash` is ever pooled, the two carried "in no pool" witness rows (GATE-build-stone,
  GATE-build-storage) must be re-pointed (§3).

**STOPS — report to the seat and build nothing further:**

- anything that would change a **ruled** number (Recall's, the embers', the Boltor's);
- **fixing F1 or F2 in code**. Each changes player-facing numbers across all three classes, so each is a ruling
  first;
- a gate prediction in §9.2 that a jar reading or an executed expression contradicts before the boot.

---

## 10. QUESTIONS FOR BEN

1. **Sunder's direction:** a new `FACING` (forward, WASD ignored), or `movement_else_forward` (WASD steers)?
   (§8.1)
2. **The slam:** S1 (ground contact), S2 (fixed delay) or S3 (predicted point)? With S1: does a water entry slam,
   and does a failed take-off slam in place? (§8.2)
3. **Fall damage on the slam:** F-a, F-b, F-c or F-d? (§8.4)
4. **Sunder's numbers:** every blank in §8.5.
5. **The stone and the sword:** O1-O4 (O2-O4 revise ruling 2). (§5.5)
6. **F1:** should a stone cast carry the held-or-hotbar melee weapon's class damage, Sharpness and gear score (O5)?
   Or are Actives deliberately gear-blind?
7. **Names:** the cell's display name, and which hand Sunder sits on in the default.
8. **A player-to-mob trace line** for gates (nothing logs one today, §8.3): wanted, or are the popup and
   `/rpg mobinfo` enough?
