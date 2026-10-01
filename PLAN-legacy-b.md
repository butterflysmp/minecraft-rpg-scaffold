# PLAN — LEGACY-B: THE BLAZE KING'S STAFF SHOOTS A REAL FIREBALL (SURVEY)

**Phase 1: SURVEY ONLY. No Java, no content, no boot, nothing committed.** This file surveys one weapon, the
Blaze King's Staff, which LEGACY-A (#171) shipped as a thrown `fire_charge` item on left-click. Ben's 2026-09-29
words (RULINGS, below) move it to a real fireball, flying straight with no gravity, on right-click. The seat ruled
that this is its own slice, LEGACY-B, built after the current stack boots. **Ben rules every number.**

**Read against:**
- `origin/feat/legacy-a` at **`eab57026`** (#171, the stack top: #168 → #169 → #170 → #171, none merged);
- the old project `CreaperCrusher/Butterfly-SMP` at **`cfde822`**, package `org.example1.butterflysmp`;
- the pinned jars, with `javap` from `~/.jdks/openjdk-26.0.1`:
  - the mojang-mapped server jar `run/versions/26.1.2/paper-26.1.2.jar`;
  - `paper-api-26.1.2.build.74-stable.jar` (`pom.xml`'s `paper.version`).

**Citations** name a CLASS and a METHOD or a SECTION, never a line. The short paths are
`PLAN-build-system.md`'s: `P/` = `paper/src/main/java/io/github/butterflysmp/rpg/paper/`, `C/` =
`core/src/main/java/io/github/butterflysmp/rpg/core/`, `content/` = `paper/src/main/resources/content/`.

**HOW TO READ A NUMBER IN THIS FILE:**
- A number labelled **cfde822** is a FACT about what the old code did. It is recorded, never adopted.
- A number labelled **jar** was read out of the pinned server or API jar with `javap -c`.
- A number labelled **awk** was computed with `awk` (double arithmetic, with the jar's `0.95f` widened to
  `0.949999988079071`). It is arithmetic on jar constants, **not a server reading.**
- Every number a slice would author, and that is not already ruled, reads **`___ (Ben)`**.

**READ vs INFERRED.** Every claim is marked:
- *read*: I opened the method's bytecode or source;
- *inferred*: reasoned from code around it;
- *UNVERIFIED*: not read. **Client-side rendering is UNVERIFIED throughout**: the server jar carries the shared
  entity classes, which the client also ticks, but not the renderers.

---

## RULINGS

### BEN, 2026-09-29, VERBATIM

```
"The Blaze King's Staff is supposed to shoot an actual fire ball not the item, it also isn't supposed
to be effected by gravity. A straight line. Also the ability should be Right Click not left."
```

**THE SEAT'S READING (the seat's, not Ben's):** the staff moves to a new slice, **LEGACY-B**, after the current
stack boots. It needs engine work: a projectile body that is a real fireball entity, with no gravity and a straight
flight, on `right_click`.

**What Ben's words rule, and what they do not:**
- **RULED: the body is a fireball entity, not an item.** Today it is `item: fire_charge`, which
  `PaperCombatWorld.spawnMarker` renders as a dropped `Item` entity.
- **RULED: no gravity, a straight line.** Today it is `gravity: 0.05`.
- **RULED: the input is right-click.** Today the trigger is `left_click`.
- **NOT RULED:** which fireball (§2.1: the blaze's small one or the ghast's large one, Q-B1); who owns the hit
  (§5.2, Q-B2); the flight's lifetime (Q-B4); and every presentation choice (Q-B6).

### THE NUMBERS STAY cfde822's PROVISIONAL ONES

Ben, 2026-09-29, from `PLAN-legacy-port.md`'s RULINGS: *"Sure, everything will be getting tuned later so it's not
that important right now"*. The seat's reading there was that cfde822's numbers are the starting values, marked
PROVISIONAL. **LEGACY-B moves none of them:**

| field | value | source |
|---|---|---|
| damage | **30**, fire | cfde822 `BLAZE_FIREBALL_BASE_DAMAGE` (a GS-1 base, carried direct) |
| mana | **8** | cfde822 `BLAZE_FIREBALL_MANA_COST` |
| cooldown | **8 ticks** | cfde822 `BLAZE_FIREBALL_COOLDOWN_MS` 400 ms. **On the 4-tick grid.** |
| speed | **1.5** | cfde822 `BLAZE_FIREBALL_VELOCITY` |
| gravity | **0** | **Ben's words** (*"A straight line"*). Replaces LEGACY-A's `0.05`. |
| max_lifetime_ticks | **___ (Ben)** | LEGACY-A carries `40`, which is `flint_staff`'s, not cfde822's. **cfde822 had no lifetime** (§4). Q-B4. |

### STANDING RULINGS THIS SURVEY TOUCHES — NOT RE-DERIVED HERE

| ruling | where it lives | what it does here |
|---|---|---|
| **Extract MECHANISMS as facts; never port STRUCTURE** | `HANDOFF-damage-system.md`, *Reference implementation* | §4 records what cfde822 did. Its `launchProjectile` plus event-override shape is **not** the design (§5.2). |
| **A TRAVELLING RANGED WEAPON AUTHORS KNOCKBACK** | `.claude/rules/standing-decisions.md`, that section | **It does not reach this staff.** A travelling MAGE weapon is **UNRULED, not excluded.** Do not add a `knockback` here, and do not cite that rule at this staff. |
| **Author cooldowns in multiples of 4** | CLAUDE.md, *NEW CONTENT CITES THE BOLTOR* | `8` is on the grid. A held right-click fires at most once per 8 ticks. |
| **NO CUSTOM ITEM STACKS ABOVE 1** | CLAUDE.md | Unchanged. LEGACY-A's BK1 already covers it. |
| **The Plume body: (1-prime), a real entity DRIVEN, core keeps hit authority** | `PLAN-plume-vanilla-body.md`, *DECIDED: OPTION (1-prime)* | **The closest precedent.** §5 proposes the same shape for a fireball, and lists where a fireball differs from an arrow. |
| **Punch is PARKED** | `NEXT.md`, *PARKED — PUNCH* | Untouched. The staff authors no knockback, so Punch must not roll on it anyway. |

---

## 1. WHAT I READ

**In this repo, at `eab57026`:**
- `C/ability/CastSpec.java`: `Projectile`, its compact constructor and its ladder;
- `C/combat/ProjectileFlight.java` in full: `launch`, `step`, `steer`, `resolve`, `Look`;
- `C/ability/CastExecutor.java`: `launch`, `launchOne`, `detonate`;
- `C/ability/effect/EffectApplier.java`: `apply` (the null-target gate);
- `C/combat/CombatWorld.java`: the marker port (`spawnMarker`, `spawnBoltMarker`, `driveMarker`, `removeMarker`,
  `markerLocation`), signatures and javadoc;
- `P/adapter/PaperCombatWorld.java`: `spawnMarker`, `ARMED_ARROW_LIFETIME`, `spawnBoltMarker`, `markerOf`,
  `driveMarker`, `removeMarker`, `markerLocation`;
- `P/listener/RpgListeners.java`: `onPlumeBodyPickup`, `onPlumeBodyHit`, `onPlumeBodyDamage`, and the right-click
  dispatch into `WeaponFire.attempt`;
- `P/content/AbilitySchema.java`: `parseCast`'s `projectile` arm, and `parseBody`;
- content: `blaze_kings_staff.yml`, `short_bow.yml`, and every `gravity:` line in `content/` (none is `0`);
- the staves' inputs: `flint_staff` and `lapis_staff` are `right_click` only;
- tests: `ScorchContentInvariantTest`'s `KNOWN_FIRE_DAMAGE_SITES` javadoc, and `golden-lore.txt`'s staff block;
- plans and gates: `PLAN-plume-vanilla-body.md` (the options and *DECIDED*), `GATE-plume-vanilla-body.md` (row
  shapes), `GATE-legacy-a.md` in full, and `PLAN-legacy-port.md` (RULINGS; on `origin/docs/batch-surveys`).

**At cfde822** (the clone in the scratchpad, not re-cloned): `StaffListener`'s Blaze King constants block,
`onBlazeStaffInteract`, `onBlazeFireballHit` and `onBlazeFireballEntityDamage`.

**From the server jar, bytecode read with `javap -c -p`:**
- `AbstractHurtingProjectile`: the constructors, `tick`, `applyInertia`, `canHitEntity`, `hurtServer`,
  `getInertia`, `getLiquidInertia`, `shouldBurn`, `getTrailParticle`, `assignDirectionalMovement`, `onDeflection`,
  and its save/load of `acceleration_power`;
- `Fireball`: `getDefaultItem`, `shouldRenderAtSqrDistance`;
- `SmallFireball` in full;
- `LargeFireball`: the constructors, `onHit`, `onHitEntity`;
- `Projectile`: `tick`, `checkLeftOwner`, `isOutsideOwnerCollisionRange`, `preHitTargetOrDeflectSelf`,
  `hitTargetOrDeflectSelf`, `deflect`, `onHit`, `onHitBlock`, `canHitEntity`, `mayInteract`, `isPickable`,
  `getPickRadius`, `hurtServer`;
- `Player`: `attack` (its deflect call) and `deflectProjectile`;
- `Entity`: `getDefaultGravity`, `getGravity`, `isOnFire`;
- `EntityType`'s `small_fireball` builder;
- `TntBlock.onProjectileHit`, `CampfireBlock.onProjectileHit`;
- `CraftEventFactory.callProjectileHitEvent`;
- `CraftFireball`, `CraftSizedFireball`, `CraftSmallFireball`, `AbstractProjectile`'s method list;
  `CraftEntity.setVelocity`;
- `CraftEntityTypes.createFireball` and its `DIRECTION` consumer;
- `CraftLivingEntity.launchProjectile`'s `Fireball` branch;
- the tags `redirectable_projectile`, `impact_projectiles` and `deflects_projectiles` under
  `data/minecraft/tags/entity_type/`.

**From the API jar:** the method lists of `org.bukkit.entity.Fireball`, `SizedFireball`, `SmallFireball`,
`LargeFireball`, `Projectile` and `Explosive`.

---

## 2. WHAT A SMALLFIREBALL DOES ON ITS OWN

### 2.1 Small or large: the two fireballs are different entities

`SmallFireball` is the blaze's (`minecraft:small_fireball`). `LargeFireball` is the ghast's (`minecraft:fireball`).
Both extend `Fireball`, which extends `AbstractHurtingProjectile`, which extends `Projectile` (*read*).

| | `SmallFireball` | `LargeFireball` |
|---|---|---|
| on entity hit | 5 fire damage, 5 s ignite (§2.2) | 6 damage, **no ignite** (`LargeFireball.onHitEntity`, `6.0f`, *read*) |
| on any hit | discards itself (`SmallFireball.onHit`) | **`ExplosionPrimeEvent`, then `Level.explode`**, then discards (`LargeFireball.onHit`, *read*) |
| punchable | **no**: not in `redirectable_projectile` | **yes**: the tag's first entry (*read*) |
| size (jar) | `0.3125 × 0.3125` (`EntityType`'s `small_fireball` builder) | not read |
| what cfde822 used | **this one** (§4) | — |

**This survey assumes the small one**: cfde822 used it, and it is the one whose hit, explosion and deflection are
cheapest to take over (§3). **Which fireball is Q-B1, and it is Ben's.**

### 2.2 Behaviour by behaviour (all *read* from the server jar unless marked)

| behaviour | what the jar does | where |
|---|---|---|
| **gravity** | **None, structurally.** `AbstractHurtingProjectile.tick` never calls `applyGravity`, and nor does `Fireball` or `SmallFireball` (counted: 0 references in all three). `Entity.getDefaultGravity` is `0.0`. So a vanilla fireball already flies straight. | `AbstractHurtingProjectile.tick` |
| **per-tick push** | `applyInertia` runs FIRST in `tick`: `delta = (delta + normalize(delta) × accelerationPower) × inertia`. The push is along the CURRENT direction, so it changes speed, never heading. | `AbstractHurtingProjectile.applyInertia` |
| **the push's size** | `accelerationPower` is a public double field, **default `0.1`** (constructor). It is saved and loaded as `acceleration_power`. On a deflection it is reset to `0.1`, or halved. | the constructor; `onDeflection` |
| **drag** | `getInertia` = **`0.95f`**; in water `getLiquidInertia` = **`0.8f`** (and four bubble particles a tick). | `getInertia`, `getLiquidInertia` |
| **speed** | So an undriven fireball tends to a terminal `0.1 × 0.95 / 0.05` = **1.9 blocks a tick** (awk), whatever it was launched at. | — |
| **move** | `setPos(position + delta)`, or to the hit point. It is **not** `move(MoverType.SELF, …)`, so there is no block collision in the move; hits come only from `ProjectileUtil.getHitResultOnMoveVector` with the `COLLIDER` clip. | `tick` |
| **despawn** | `tick` discards (`DESPAWN`) only if the owner is **non-null and removed**, or the chunk at its position is **not loaded**. **There is no lifetime and no timer.** With no owner it flies until it hits something or leaves loaded chunks. | `tick` |
| **self-burn** | `shouldBurn` is `true`, so every tick it calls `igniteForSeconds(1)` on itself. `small_fireball` is not `fireImmune` (its builder has no such call), so `isOnFire` is true server-side. | `tick`; `Entity.isOnFire` |
| **trail** | `createParticleTrail` adds `SMOKE` each tick through `level.addParticle`, which is client-drawn (*inferred*: the server path is a no-op; the client runs the same class). | `createParticleTrail`, `getTrailParticle` |
| **render gate** | `Fireball.shouldRenderAtSqrDistance`: **not rendered** while `tickCount < 2` **and** closer than √12.25 = **3.5 blocks** to the camera. So it pops into view about 3.5 blocks out, not at the staff (*inferred* for the client). | `Fireball.shouldRenderAtSqrDistance` |
| **look** | `Fireball` is an `ItemSupplier`. Its `DATA_ITEM_STACK` defaults to **`FIRE_CHARGE`**, so it renders as the fire-charge sprite. *UNVERIFIED*: whether it is billboarded, and whether a flame overlay draws (renderer code). | `Fireball.getDefaultItem`, `setItem` |
| **entity hit** | `SmallFireball.onHitEntity`: raises **`EntityCombustByEntityEvent`** (5.0 s). If that is not cancelled, the target is ignited for its duration. Then `hurtServer` with `damageSources().fireball(this, owner)` for **5.0**. If the hurt fails, the target's old fire ticks are restored. If it succeeds, `EnchantmentHelper.doPostAttackEffects`. | `SmallFireball.onHitEntity` |
| **block hit** | `SmallFireball.onHitBlock`: calls `super` (reaching `Projectile.onHitBlock`, which calls `BlockState.onProjectileHit` **unless `hitCancelled`**), then, **if `isIncendiary`** and the face-adjacent block is empty, raises `BlockIgniteEvent` and places fire. **The ignition is NOT gated on `hitCancelled`.** | `SmallFireball.onHitBlock`, `Projectile.onHitBlock` |
| **incendiary default** | `isIncendiary` defaults to **`true`**. `SmallFireball`'s shooter constructor re-sets it from `mobGriefing` **only when the owner is a `Mob`**; a player-owned or unowned one stays `true`. | the constructors |
| **block side-effects** | Through `BlockState.onProjectileHit`: `TntBlock` **primes** if the projectile `isOnFire` and `mayInteract`; `CampfireBlock` **lights** under the same test (and a `BlockIgniteEvent`). The fireball is always on fire (self-burn row). | `TntBlock.onProjectileHit`, `CampfireBlock.onProjectileHit` |
| **explosion** | **None on a SmallFireball.** No `explode` call anywhere in `SmallFireball`, `Fireball` or `AbstractHurtingProjectile`. Only `LargeFireball.onHit` explodes. | — |
| **after any hit** | `SmallFireball.onHit` calls `super.onHit`, then **`discard(HIT)` unconditionally**. | `SmallFireball.onHit` |
| **being hurt** | `AbstractHurtingProjectile.hurtServer` returns `false`, so no damage reaches it or deflects it. | `hurtServer` |
| **punch deflection** | `Player.attack` → `deflectProjectile`, which acts only on an entity in **`redirectable_projectile`**: `fireball`, `wind_charge`, `breeze_wind_charge`. **`small_fireball` is not in it.** `Projectile.isPickable` is the same tag test, and `getPickRadius` is `0` when not pickable, so the crosshair cannot select it (*inferred*: client picking). **A SmallFireball cannot be punched back.** | `Player.deflectProjectile`, `Projectile.isPickable` |
| **deflection by an entity** | `hitTargetOrDeflectSelf` asks the hit entity's `deflection(this)`. `deflects_projectiles` is **`breeze`** only (tag). *UNVERIFIED*: whether any other entity's `deflection` override returns non-`NONE`. | `hitTargetOrDeflectSelf` |
| **hitting its own shooter** | `Projectile.canHitEntity`: `owner == null || leftOwner || !owner.isPassengerOfSameVehicle(target)` (plus a `canSee` check when the owner is a player). `leftOwner` flips once the swept, 1-inflated box no longer touches the owner (`isOutsideOwnerCollisionRange`). So an OWNED one skips its shooter until it has left them (*inferred*: `isPassengerOfSameVehicle` of an entity with itself was not read); **an UNOWNED one passes this test for anybody from its first tick** (*read*). `AbstractHurtingProjectile.canHitEntity` also skips `noPhysics` entities. | `canHitEntity`, `checkLeftOwner` |
| **hit event** | `preHitTargetOrDeflectSelf` raises `ProjectileHitEvent` (Paper raises `ProjectileCollideEvent` first, for an entity hit) and sets `hitCancelled`. **For an ENTITY hit, a cancel skips everything.** **For a BLOCK hit, `hitTargetOrDeflectSelf` runs ANYWAY**, so `onHit` runs and the fireball is discarded. The cancel only reaches code that reads `hitCancelled`, which is `Projectile.onHitBlock`. | `preHitTargetOrDeflectSelf` |
| **tracking** | `clientTrackingRange 4`, `updateInterval 10` (builder). *UNVERIFIED*: the effective range after Paper/Spigot's tracking-range config. | `EntityType` |

---

## 3. HOW EACH BEHAVIOUR IS SUPPRESSED OR TAKEN OVER

**The API surface, from the API jar** (*read*):
- `Fireball`: `setDirection`, `getDirection`, `setAcceleration`, `getAcceleration`, `setPower`, `getPower`;
- `SizedFireball`: `getDisplayItem`, `setDisplayItem`;
- `Explosive`: `setYield`, `getYield`, `setIsIncendiary`, `isIncendiary`;
- `Projectile`: `setShooter`, `setHasLeftShooter`, `canHitEntity` (a query), `hitEntity`.

> **`setAcceleration` DOES NOT SET THE ACCELERATION. IT SETS THE VELOCITY, AND THAT IS READ, NOT ASSUMED.**
> `CraftFireball.setAcceleration(v)` is `handle.assignDirectionalMovement(toVec3(v), v.length())`, which is
> `setDeltaMovement(normalize(v) × |v|)`. That is the velocity. `getAcceleration` returns `getDeltaMovement`.
> `setPower` calls `setAcceleration`. `setDirection` rescales both to the current speed.
> **So `accelerationPower` (the `0.1` push) has NO API setter in this build.** It is reachable only through
> entity NBT (`acceleration_power`), which is NMS territory and BANNED here. The design must work WITH the `0.1`
> push and the `0.95` drag, not switch them off (§5.4).

| behaviour | how it is suppressed or taken over | cost / caveat |
|---|---|---|
| **gravity** | **Nothing to suppress.** Straightness comes from core's `gravity: 0` (§5.1). `setGravity(false)` is **inert** on this entity, because `tick` never reads gravity. | Setting it anyway "reads as the fix" and does nothing. If set, its comment must say so. |
| **push and drag** | **Taken over by driving**: `driveMarker` rewrites the velocity every tick (as it does for the arrow). But `applyInertia` runs **before** the move, so the body moves `(|v| + 0.1) × 0.95`, **not `|v|`** (§5.4). | **New to fireballs.** The arrow's drag runs after its move, which is why it had no such error. |
| **entity hit (damage, 5 s ignite, post-attack effects)** | **Cancel `ProjectileHitEvent`** for a tagged body. For an entity hit, `preHitTargetOrDeflectSelf` then returns before `onHit`, so `onHitEntity` never runs: no damage, no `EntityCombustByEntityEvent`, no discard. `onPlumeBodyHit` already does exactly this, keyed on the `markerEntity` tag, not on the type. | The body is still clamped to the hit point for that tick (`tick`'s `setPos(hit location)`). That is the same one-tick hitch the arrow has. |
| **backstops for the entity hit** | `onPlumeBodyDamage` already cancels and warns on ANY tagged damager, so it covers a fireball as written (*read*: it tests the tag, not `Arrow`). **Add:** `EntityCombustByEntityEvent` with a tagged combuster: cancel and warn, in the same loud style. | Two listeners that should never fire. |
| **block hit: vanilla block reactions (TNT, campfire, bell, target…)** | **Cancel `ProjectileHitEvent` for BLOCK hits too, for a fireball body.** That sets `hitCancelled`, and `Projectile.onHitBlock` returns before `BlockState.onProjectileHit`. **`onPlumeBodyHit` returns early on a block hit today**, on purpose for the arrow (so it sticks). A fireball needs the opposite. | Must be keyed on the body KIND, or the arrow starts refusing its stick. |
| **block hit: fire placement** | `setIsIncendiary(false)` at spawn. `SmallFireball.onHitBlock` gates the ignition on `isIncendiary` **and not on `hitCancelled`**, so the cancel above does NOT prevent it. **Backstop:** `BlockIgniteEvent` with a tagged igniting entity: cancel and warn. | **Load-bearing, and it will read as redundant beside the cancel.** It is not. |
| **block hit: the body's removal** | **Cannot be suppressed.** `discard(HIT)` is unconditional in `SmallFireball.onHit`, and a block hit reaches `onHit` even when cancelled. The body vanishes at its first block contact. | Harmless: `markerOf` then finds nothing, and `driveMarker`/`removeMarker` are already no-ops on a missing body (`driveMarker`'s comment: *"Silently absent is CORRECT"*). The flight resolves on its own ray. |
| **explosion** | **None to suppress on a SmallFireball.** `setYield(0)` writes `bukkitYield`, which only `LargeFireball`'s explosion path reads (*inferred* from `LargeFireball`'s load of `bukkitYield`). Inert here. | Were Ben to pick the LargeFireball (Q-B1): explosion runs in `onHit`, **which a block hit reaches even when cancelled**, so it would need `ExplosionPrimeEvent` cancelled for a tagged body. |
| **punch deflection** | **Nothing to suppress** on a SmallFireball (§2.2). And even a deflected body would have its velocity overwritten by the next `driveMarker` (*inferred*). The hit authority is core's `castRay` either way. | LargeFireball: punchable, and `deflect` sets the owner to the puncher. |
| **hitting its own shooter** | Two halves. **Ours:** `castRay(position, next, caster.id())` excludes the caster (*read*, `ProjectileFlight.step`). **Vanilla's:** the body is unowned (`setShooter(null)`, as the arrow body is), so it CAN find its shooter. That hit is cancelled like any other entity hit. | A shot aimed down may clamp the body on the shooter for one tick. |
| **despawn** | **No vanilla timer to arm.** The flight's `resolve` → `removeMarker` is the only normal exit. `setPersistent(false)` (as the arrow) drops it at chunk unload or save. With no owner, the owner-removed exit does not apply. | **The orphan case changes shape** (§5.5). |
| **look** | `setDisplayItem` exists (`SizedFireball`). The default sprite is already `FIRE_CHARGE`. | No change needed. |

---

## 4. HOW cfde822's FIREBALL WORKED (a mechanism fact, not a design)

*Read*, `StaffListener`:
- **Input:** `onBlazeStaffInteract` (`PlayerInteractEvent`, `HIGHEST`) on `LEFT_CLICK_AIR` / `LEFT_CLICK_BLOCK`. It
  cancels the event, checks the 400 ms cooldown (through `StatService.abilityCooldown`) and 8 mana, stamps the
  cooldown, and computes `damage = GearScore.scalePower(30, level)`.
- **Launch:** `player.launchProjectile(SmallFireball.class, eyeDirection × 1.5)`, then `setIsIncendiary(false)`. It
  tags the damage and `MAGIC_PROJECTILE` in the entity's PDC, and plays `ENTITY_BLAZE_SHOOT` (1.0, 1.2).
- **What `launchProjectile` does for it** (*read*, `CraftLivingEntity.launchProjectile`'s `Fireball` branch):
  - it builds `new SmallFireball(level, player, eyeDirection × 10)`. **The shooter is the player**, and the
    constructor's `assignDirectionalMovement` sets the velocity to the unit direction × `0.1`;
  - it snaps to the eye location with the player's yaw and pitch;
  - then `setVelocity(eyeDirection × 1.5)`;
  - `accelerationPower` stays `0.1`.
- **Flight:** pure vanilla. It is straight, because there is no gravity (§2.2), but **its speed climbs from 1.5
  toward 1.9 blocks a tick** (awk):

  | tick | 1 | 2 | 10 | 20 | 40 |
  |---|---|---|---|---|---|
  | speed | 1.5200 | 1.5390 | 1.6605 | 1.7566 | 1.8486 |
  | distance | 1.52 | 3.06 | 15.95 | 33.12 | 69.38 |

  **No lifetime.** It flew until it hit something, or its chunk unloaded, or its shooter was removed.
- **Entity hit:** vanilla's hit, with the damage **overwritten**. `onBlazeFireballEntityDamage`
  (`EntityDamageByEntityEvent`, damager a tagged `SmallFireball` shot by a player) sets the damage to
  `withPlayerOffenseBonuses(shooter, damage)` and the target's fire ticks to **100** (5 s). The vanilla 5 s
  combust also ran (*inferred*: nothing cancelled it).
- **Any hit:** `onBlazeFireballHit` (`ProjectileHitEvent`) spawned `FLAME` ×18, `LAVA` ×5 and `ITEM_FIRECHARGE_USE`
  (1.0, 1.3) at the fireball. It did not cancel.
- **Blocks:** non-incendiary, so no fire. Not cancelled, so vanilla block reactions ran (*inferred*: a fireball
  that is on fire and hits TNT primes it, per §2.2).
- **Shooter:** owned, so it skipped the shooter until it had left them (§2.2).

**What LEGACY-B keeps from this:** the entity type, the direction, the starting speed, non-incendiary blocks, and the
numbers. **What it does not keep:** vanilla hit authority (Q-B2), the rising speed, the unbounded lifetime (Q-B4), and
the unsuppressed block reactions.

---

## 5. HOW IT PLUGS INTO THIS ENGINE

### 5.1 What the engine does today (*read*)

- **Our projectile is a SIMULATED POINT with an optional rendered body, not a real entity.** `ProjectileFlight`
  advances a `Vec3` by `velocity` each tick. It traces `castRay(position, next, caster.id())` for the hit and
  subtracts `gravity` from the vertical each tick (`nextVelocity`). It schedules the next step on the region owning
  `next`.
- A body is spawned on the launch frame at the aim origin (`launch`: `spawnBoltMarker` for `body`, `spawnMarker` for
  `item`). It is **driven** by velocity at the end of each step (`driveMarker`) and removed on hit or fuse
  (`resolve`).
- **The body never decides anything.** `castRay` owns every hit; `CastExecutor.detonate` → `EffectApplier` owns
  every effect.
- **`gravity: 0` already means a straight line in core.** `launchOne` passes `aim.direction() × speed`, with no
  launch lift, and a zero gravity leaves `nextVelocity` equal to `velocity` every tick (*read*). **No core change is
  needed for straightness.** No shipped content authors `gravity: 0` today, so it is new in content and untested in
  a unit test.
- **The fuse:** at `max_lifetime_ticks` the flight lands at `next` with a null target. `EffectApplier.apply` skips a
  `Targeted` effect when `target == null`, so the staff's one `damage` effect lands on nothing (*read*).
- **`body:` is a closed set of one.** `AbilitySchema.parseBody` refuses anything but `arrow`. Its javadoc already
  says: *"the day a second body exists this becomes a switch and `CombatWorld.spawnBoltMarker` grows the id as a
  parameter."*
- **`item:` and `body:` are mutually exclusive**, refused in `CastSpec.Projectile`'s compact constructor and in
  `ProjectileFlight.Look`'s.

### 5.2 Who owns the hit — the Plume's three options, re-asked for a fireball (Q-B2)

`PLAN-plume-vanilla-body.md` ranked these for the arrow, and its reasoning carries across unchanged:

| option | shape | for a fireball |
|---|---|---|
| **(1-prime)** — RECOMMENDED | A real SmallFireball, **driven** every tick. Core keeps hit authority. | `core/` changes by one signature (§5.3). **All of the new cost is suppression** (§3), plus the drive law (§5.4). |
| (1) | A real fireball, not driven; core's flight models its push and drag | Core would have to model `+0.1 then ×0.95`. That changes the reach (60 → 69.38 blocks in 40 ticks, awk). No benefit over (1-prime). |
| (2) | The vanilla fireball owns the hit, as cfde822 did | **Forks the damage path.** It re-enters the custom health store, elements, crits and PLAYERHIT from a Bukkit event, and makes a `body: fireball` weapon a different kind of weapon from every other projectile. |

**The one thing that would change this is the Plume's own caveat:** if Ben wants the fireball to BEHAVE like a
vanilla one (catch the shooter's hitbox, be blocked by what blocks it), that is (2) by another name. That is a
gameplay ruling.

### 5.3 The smallest design: `body: fireball`, `gravity: 0`, on `right_click`

**Content** (`content/weapons/blaze_kings_staff.yml`):
- `triggers.left_click` → **`triggers.right_click`**, with the same block: name, description, cost, cooldown and
  `on_hit`. The same shape as `flint_staff` and `lapis_staff`, which are `right_click` only (*read*).
- In `cast`:
  - `item: fire_charge` → **`body: fireball`** (the two are mutually exclusive);
  - `gravity: 0.05` → **`gravity: 0`**;
  - `speed: 1.5` unchanged;
  - `max_lifetime_ticks: ___ (Ben)` (Q-B4).
- The header comment's *"this engine's projectiles are an item or an arrow, so it throws a fire charge"* is now
  false and must go. Its PROVISIONAL block gains the gravity line, citing Ben's words.
- **No `knockback` is added.** This is a mage weapon, UNRULED (the standing decision).
- The description *"Throws a burning charge."* is text, not a number. Keep it or reword it: Q-B7.

**Core:**
1. **`CombatWorld.spawnBoltMarker(Vec3 at, Vec3 velocity, int expectedLifetimeTicks)` grows a body id**, as
   `parseBody`'s javadoc anticipates. It becomes `spawnBoltMarker(Vec3, Vec3, int, String body)`, or a rename, to be
   decided in the build. Core stays kind-agnostic: the id is a string, as `item` already is.
2. **`ProjectileFlight.launch`** passes `look.body()` through. The three-branch dispatch is unchanged, because a
   fireball is a `body`, not a fourth kind.
3. **`CastSpec.Projectile`'s and `ProjectileFlight.Look`'s javadoc** stop saying `body` "renders a real ARROW".
4. **Tests first (CLAUDE.md: the core test before the paper wiring):**
   - a `ProjectileFlightTest` row: the body id reaches the port unchanged;
   - a `gravity 0` row: every driven vector is equal and every traced segment is collinear, **asserted over the
     whole flight, not one tick**;
   - `FakeWorld` records the id.

**Loader** (`P/content/AbilitySchema.parseBody`):
- The set becomes `{arrow, fireball}`. A typo stays a NAMED, SKIPPED file.
- `AbilityLoaderTest`: `body: fireball` parses; `body: fireballl` is refused; the control, `body: arrow`, still
  parses.

**Paper:**
1. **`PaperCombatWorld.spawnBoltMarker`** switches on the id. `arrow` stays exactly as it is. `fireball` spawns a
   `SmallFireball` through `world.spawn(loc, SmallFireball.class, consumer)`.
   - *Read:* `CraftEntityTypes.createFireball` is `createAndMove` plus `DIRECTION`, and `DIRECTION` sets the velocity
     to the Location's unit direction × 1.0. The velocity is then set to the flight's own. **Never
     `launchProjectile`**: that sets the player as the owner.
   - Configure it with:
     - `setIsIncendiary(false)` — load-bearing (§3);
     - `setShooter(null)`;
     - `setPersistent(false)`;
     - the `markerEntity` tag, which `markerOf`, the hit cancel and the damage backstop all read.
   - Optional and inert, and if set it must say so: `setYield(0)`, `setGravity(false)`.
2. **`PaperCombatWorld.driveMarker`** applies the fireball's drive law (§5.4) when the marker is a `Fireball`.
   **Arrows and items are unchanged.**
3. **`RpgListeners`:**
   - The hit cancel must also cancel BLOCK hits **for a fireball body only**. Either a kind test in
     `onPlumeBodyHit` or a sibling listener; the build decides which. The arrow must still stick.
   - Two new loud backstops: `EntityCombustByEntityEvent` and `BlockIgniteEvent`, each keyed on a tagged entity.
     Each cancels and warns. They are the same style as `onPlumeBodyDamage` and must say what did not take.
4. **Registration** stays in `RpgPlugin`'s one registration point.

**Suppressed, and by what** (the §3 table, reduced to the build's checklist):

| must not happen | stopped by | backstop |
|---|---|---|
| vanilla 5 damage / 5 s ignite on a mob or the shooter | entity-hit `ProjectileHitEvent` cancel | `onPlumeBodyDamage` (existing, tag-keyed); new combust backstop |
| fire placed on a block | `setIsIncendiary(false)` | new `BlockIgniteEvent` backstop |
| TNT primed, campfire lit, bell rung, target powered | **block-hit** `ProjectileHitEvent` cancel (fireball only) | none; a gate row (LB5) |
| explosion | nothing to stop (SmallFireball) | a gate row (LB6) |
| punch-back | nothing to stop (SmallFireball not pickable) | a gate row (LB7) |
| body drifting off the traced path | the drive law (§5.4) | a gate row (LB9) |

### 5.4 THE DRIVE LAW: THE ARROW'S "LANDS EXACTLY ON THE COMPUTED PATH" DOES NOT CARRY OVER

`driveMarker`'s javadoc says the driven body lands *"precisely"* on the path. The reason is that the arrow's drag is
applied **after** its move, and the next drive overwrites it. **A hurting projectile does the opposite.**
`AbstractHurtingProjectile.tick` calls `applyInertia` **first** (*read*). So a body driven with velocity `v` moves
`normalize(v) × (|v| + 0.1) × 0.95` that tick. The direction is right; only the speed is off.

| | per tick | error per tick | error at 40 ticks |
|---|---|---|---|
| driven with `v`, `|v|` = 1.5 | **1.520000** (awk) | +0.020000, **ahead** | **+0.8 blocks** along track |
| driven with `normalize(v) × (|v| / 0.95 − 0.1)` | **1.500000** (awk; `m` = 1.478947) | 0 | 0 |

- **The error is along-track, never lateral.** The direction is exact, so the body leads on the right line. It is the
  same kind of error the Plume plan judged tolerable (*"a lag on the right line, not a miss"*).
- **Compensating is one line in `driveMarker`**, and it makes the client agree too. The client runs the same `tick`
  on the same velocity, so the pre-compensated vector also moves it exactly `|v|` (*inferred*). The two constants
  (`0.1`, `0.95f`) are **jar facts with no API getter**, so they would be named constants with this section as their
  account, and a gate row as their witness.
- **In water** the drag is `0.8f`, so a compensated body over water slows. That is cosmetic, because `castRay`
  never consults the body.
- **Can a boot row see the uncompensated 0.8 blocks? Possibly not.** At 1.5 blocks a tick, 0.8 blocks is about half
  a tick of flight. **So: compensate, or accept? That is Q-B3.** The recommendation is to compensate, because it is
  free and it keeps one invariant true of every body kind.

### 5.5 THE ORPHAN CHANGES SHAPE

- An orphaned **arrow** dies on vanilla's own armed timer (`ARMED_ARROW_LIFETIME`).
- An orphaned **item** hangs.
- **An orphaned fireball has no timer at all** (§2.2). Undriven, it speeds up toward 1.9 blocks a tick and flies
  **straight until its first block contact** (where it is discarded; non-incendiary; and with the hit cancelled,
  no TNT) **or the edge of loaded chunks**. Over open ocean or sky that is the whole loaded radius. It cannot damage
  anything, because entity hits are cancelled.
- **When does a body orphan?** Only if the step chain stops without reaching `resolve`: an exception in a step, the
  region unloading mid-flight, or the plugin disabling. `setPersistent(false)` covers chunk unload and save.
- **Q-B5:** accept this, or add a backstop removal. `Scheduler` has no `onEntityLater` (CLAUDE.md lists `onEntity`,
  `onRegion`, `onRegionLater`, `onGlobal` and `async`). The Plume plan rejected a scheduled removal as *"a second
  mechanism … the one that rots"*.

### 5.6 WHAT ELSE DIFFERS FROM THE ARROW, RECORDED

- **The body can vanish before the flight resolves.** The body's hit test sweeps a `0.3125` box. `castRay` traces a
  point. So the body can graze a block corner the ray misses. It is then discarded (a block hit cannot be kept, §3),
  and the flight flies on invisibly to its real hit. The arrow sticks for one tick in the same case. *Inferred*: not
  staged, and not cheaply stageable (*WHAT THIS GATE CANNOT SEE*).
- **The body appears ~3.5 blocks out, not at the staff** (§2.2's render gate). `ProjectileFlight.launch`'s comment
  that *"a rendered BODY at the eye is the bolt leaving the staff"* is not true of this body kind. That is a client
  fact: *inferred* from the shared class, and the gate row LB3 reads it.
- **Ordering is the arrow's, already verified on Paper:** the scheduler's `driveMarker` runs before the entity ticks
  that server tick (`driveMarker`'s javadoc, *"offset 37 … offset 431"*). The drive law above depends on it. **On
  Folia it is unestablished**, as for the arrow.
- **`CraftEntity.setVelocity`** is a plain `setDeltaMovement` plus `hurtMarked` (*read*). `AbstractProjectile` and
  `CraftFireball` do not override it (*read*). So driving a fireball is the same call as driving an arrow.

---

## 6. GATE ROWS FOR LEGACY-B — DRAFT PREDICTIONS, NOT RUN

**Status: NOT RUN. Not a GATE file yet.** These are the rows a `GATE-legacy-b.md` would carry. It is written and
committed before any boot, in `GATE-legacy-a.md`'s shape. Readings go beside a prediction, never over it.

**Set-up** (from `GATE-legacy-a.md`'s *Set-up*, unchanged):
- SURVIVAL;
- `/gamerule spawn_mobs false`, `/time set 18000`;
- `/rpg playerxp set @s 1 levels` (**level 1**, so no level damage);
- no accessories and no armour;
- `/rpg mobtrace` ON;
- `/rpg give blaze_kings_staff`, then in hand `/rpg gearscore set 100` (**gear score 100**);
- `/rpg mana refill` before each firing row;
- the test zombie is #168's (*THE TEST ZOMBIE*), a fresh one per row. A `crit=true` line is not comparable: re-fire.

**Do the vanilla-control rows (LB5c, LB7) in a cleared test area:** they place real fire.

### R0a — the build line names this branch's head

| prediction | instrument | READING |
|---|---|---|
| `[Rpg] Build: <sha>`, LEGACY-B's head, shortened. Not `-dirty`, not `unknown`, not #171's `eab57026` or any head beneath it | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | |

### R0b — the jar carries the new content

| prediction | instrument | READING |
|---|---|---|
| PRESENT in the jar's `content/weapons/blaze_kings_staff.yml`: **`body: fireball`**, **`gravity: 0`**, **`right_click:`**. ABSENT: **`item: fire_charge`** and **`left_click:`**. Control: `short_bow.yml` still reads `body: arrow` | `GATE-level.md` R0b's scan, reading these files' text out of the jar | |

### R0c — nothing else moved

| prediction | instrument | READING |
|---|---|---|
| The `Loaded …` line **equals LEGACY-A's R0c prediction** (15 weapons, 27 visuals, …), unless Q-B6 adds a visual (then visuals **27 → ___**). No `Refusing` or `Skipping` naming `blaze_kings_staff`; no `SEVERE`; no exception beyond `volley_stone`'s known pair | `Select-String run\logs\latest.log -Pattern 'Loaded ','Refusing','Skipping','SEVERE','Exception'` | |

### LB1 — the tooltip reads Right-Click, and left-click does nothing (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| Hover (score 100): LEGACY-A BK1's rendering with ONE change: **`Fireball  Right-Click`** (was `Left-Click`). **Left-click at the air: no cast, no mana spent, no cooldown shown.** Whatever a left-click with `flint_staff` does today, it does here (*not surveyed*: record what is seen) | |

### LB2 — PLAYERHIT: 30 fire on a right-click, and no vanilla damage (SURVIVAL) — witness: PLAYERHIT line

| prediction | READING |
|---|---|
| Zombie offset `~8 ~ ~0`, aim at it (`/tp @s ~ ~ ~ facing entity @e[tag=t,limit=1] eyes`), **right-click once**: **`PLAYERHIT … source=blaze_kings_staff/right_click … element=fire sent=30.000 crit=false triggerScore=100`**. **Exactly one** PLAYERHIT for the shot. **No `[plume] A MARKER BODY dealt damage` warning** and **no new combust or ignite backstop warning** in the log. The zombie burns afterwards (Scorch, `fire.yml`), as in BK2 | |

### LB3 — it is a fireball, not an item (SURVIVAL) — witness: Ben, with F3+B

| prediction | READING |
|---|---|
| Fire into open air with F3+B on. The body is **the blaze's fireball**: a small fire-charge sprite that faces the camera, trailing **smoke**, with a **~0.31-block hitbox cube**. It is **not** a dropped item (no spin, no bob, no item-entity hitbox). It **appears a few blocks out**, not at the staff (the render gate, §5.6). **Positive control, same session:** `flint_staff`'s thrown item looks different in each of these respects | |

### LB4 — a straight line: no drop over the whole flight (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| Stand on a long flat surface. `/tp @s ~ ~ ~ -90 0` (east, level). Fire once. **The fireball stays at eye height for its whole flight**, with no visible drop at any range. It vanishes mid-air at about **1.5 × `max_lifetime_ticks` blocks** (60 at 40; Q-B4). **Positive control:** `flint_staff` from the same spot (gravity 0.05) **visibly drops**, so this staging CAN show a drop | |

### LB5 — no block ignition, no TNT, no campfire (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| **(a)** Face a wall of oak planks 10 blocks east, with air in front of it. Fire three times: **no fire block appears**, and each fireball **vanishes at the wall**. **(b)** Replace one plank with TNT and fire at it: **it does not prime.** **(c) Positive control, vanilla, NOT OURS:** `/summon small_fireball ~ ~1.6 ~2 {Motion:[1.0d,0.0d,0.0d]}` aimed at the planks. Vanilla's default is incendiary, so **it places fire**. That shows the staging can ignite. *UNVERIFIED*: the `Motion` key's spelling in 26.1. If the summon does not move, record it and do not read (c) as a pass | |

### LB6 — no explosion, at a block or at a mob (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| Fire at the plank wall and at a test zombie. **No explosion sound, no explosion particles, no broken blocks, no crater, no push.** This row guards the entity TYPE: a `LargeFireball` spawned by mistake explodes in `onHit`, which a cancelled block hit still reaches (§3) | |

### LB7 — a small fireball cannot be punched back (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| **This row witnesses the entity TYPE, not our driven body.** Our body moves 1.5 blocks a tick and lives ≤ 2 s, so it cannot be staged for a punch. `/summon small_fireball ~ ~1.6 ~2 {Motion:[0.0d,0.0d,0.0d],acceleration_power:0.0d}` (a stationary vanilla one; `acceleration_power` is the jar's save key). Left-click it: **the crosshair does not select it and nothing happens.** **Positive control:** `/summon fireball ~ ~1.6 ~2 {Motion:[0.0d,0.0d,0.0d],acceleration_power:0.0d}` (the ghast's). Left-click it: **it is deflected** along the look direction. *UNVERIFIED*: the summon keys; if either summon does not hold still, record it and do not read the row | |

### LB8 — the shooter is never hit (SURVIVAL) — witness: Ben + the log

| prediction | READING |
|---|---|
| Record health. `/tp @s ~ ~ ~ ~ 90` (straight down) and fire three times, then `~ ~ ~ ~ 60` and fire three more. **Health unchanged. Ben does not catch fire. No PLAYERHIT naming Ben. No backstop warning.** The ground under him does not ignite. Our `castRay` excludes the caster, and the unowned body's own hit on him is cancelled (§3) | |

### LB9 — the body arrives with the hit (SURVIVAL) — witness: Ben

| prediction | READING |
|---|---|
| Zombie offset `~30 ~ ~0`, `NoAI`, aimed. Fire once. **The fireball reaches the zombie at the moment the damage lands**: no visible gap before it, no body carrying on past it. **If Q-B3 ruled "accept"**, the prediction is instead a lead of up to `0.02 × ticks` blocks (awk), which is **0.4 blocks at this range** (20 ticks), and it may be invisible. That is the honest limit of this row | |

### LAST STEP — PUT BEN'S XP BACK

`/rpg playerxp set @s <the number recorded> xp`, as in `GATE-legacy-a.md`.

### WHAT THIS GATE CANNOT SEE

- **The body vanishing on a grazed corner** while the flight flies on (§5.6). There is no cheap staging.
- **The orphan** (§5.5). A healthy build never produces one.
- **Folia ordering** (§5.6).
- **The tests that could guard the paper half:** a unit test cannot see a rendered frame, which is the Plume's
  lesson. The `isIncendiary(false)` line and the block-hit cancel get **LB5** as their sole witnesses. The build
  must name that in their comments, as `spawnBoltMarker` names its own sole witness.

---

## 7. STACKING, AND THE FILES IT WOULD TOUCH

**Order (the seat's ruling):** LEGACY-B starts only **after the current stack boots and merges**: #168 (M1) → #169
(LEVEL) → #170 (NEXUS polish) → #171 (LEGACY-A). It branches from the `master` that contains #171's squash. The
merge state is read from origin (the memory note: *"verify merge state from origin, it's been wrong twice"*). Its R0c
predicts against LEGACY-A's R0c.

**Why not stacked on #171 now:**
- #171 is the stack top and is unbooted.
- LEGACY-B rewrites the very trigger block that #171's BK1 and BK2 read. A LEGACY-B commit on top would make those
  rows read a different staff.
- The stack already edits `PaperCombatWorld` (*read*: `git diff --stat master...feat/legacy-a` shows it +17; which PR in the stack made the edit was not checked). One
  clean base is the standing preference.

**Files it would touch** (proposed; the build confirms the list):

| module | file | change |
|---|---|---|
| content | `content/weapons/blaze_kings_staff.yml` | `left_click` → `right_click`; `item: fire_charge` → `body: fireball`; `gravity: 0`; lifetime ___ (Ben); header comment |
| core | `C/combat/CombatWorld.java` | `spawnBoltMarker` takes a body id |
| core | `C/combat/ProjectileFlight.java` | passes `look.body()`; `Look`'s javadoc |
| core | `C/ability/CastSpec.java` | `Projectile`'s javadoc (body is not only an arrow) |
| core test | `FakeWorld.java`, `ProjectileFlightTest.java` | the id reaches the port; a gravity-0 straight-line test over the whole flight |
| paper | `P/content/AbilitySchema.java` | `parseBody` accepts `fireball` |
| paper | `P/adapter/PaperCombatWorld.java` | `spawnBoltMarker` switch; the fireball's spawn and config; `driveMarker`'s drive law |
| paper | `P/listener/RpgListeners.java` | block-hit cancel for a fireball body; combust and ignite backstops |
| paper test | `AbilityLoaderTest.java` | `body: fireball` parses; a typo is refused |
| paper test | `golden-lore.txt` | the staff's `Left-Click` → `Right-Click` (one line; regenerate, then diff to confirm only that moved) |
| paper test | `ScorchContentInvariantTest` | **no change expected**: `KNOWN_FIRE_DAMAGE_SITES` counts `element: fire` lines, and moving the trigger moves none (*inferred*; recount with the test's own pattern) |
| docs | `GATE-legacy-b.md` | §6, committed before the boot |

---

## 8. OPEN QUESTIONS

| # | question | whose | recommendation |
|---|---|---|---|
| **Q-B1** | Which fireball? The **small** (blaze's, what cfde822 used: no explosion, not punchable) or the **large** (ghast's: explodes in `onHit` even on a cancelled block hit, punchable, bigger sprite) | Ben | small |
| **Q-B2** | Who owns the hit? **(1-prime)**: a driven body, core's `castRay` owns the hit. **(2)**: the vanilla fireball owns it, as cfde822 did, which forks the damage path | Ben (it is a gameplay ruling if he wants vanilla behaviour) | (1-prime) |
| **Q-B3** | Compensate the drive for the fireball's `+0.1 then ×0.95` (exact path), or accept a lead of ≤ 0.8 blocks over 40 ticks (awk)? | seat | compensate |
| **Q-B4** | `max_lifetime_ticks` ___ (Ben). LEGACY-A carries `40` from `flint_staff`: 60 blocks at 1.5. **cfde822 had none** (it flew about 69 blocks in 40 ticks and kept going, awk) | Ben | — |
| **Q-B5** | The orphan (§5.5): accept it (dies at the first block or unloaded chunk, harmless, can travel the loaded radius), or add a backstop removal? | seat | accept, and record it in `NEXT.md` |
| **Q-B6** | Presentation. cfde822 had a cast sound (`ENTITY_BLAZE_SHOOT`), impact particles (`FLAME` ×18, `LAVA` ×5) and an impact sound (`ITEM_FIRECHARGE_USE`). The staff today has none, and the vanilla smoke trail comes free. Add a visual? ___ (Ben) | Ben | out of scope unless asked |
| **Q-B7** | Description text: keep *"Throws a burning charge."* or reword? ___ (Ben) | Ben | — |

### THE SEAT'S RULINGS, 2026-09-29 (Q-B1, Q-B4, Q-B6 and Q-B7 have gone to Ben)

- **Q-B2: (1-prime).** A driven body, and core's `castRay` owns the hit, so the gear score, the level/weapon flag,
  PLAYERHIT and Scorch all apply unchanged. **No forked damage path.**
- **Q-B3: COMPENSATE.** The body flies on the exact line, and the LB straight-flight row predicts **no lead**.
- **Q-B5: the drive REMOVES the entity when its lifetime ends.** An orphan that outlives the drive (a chunk unloading
  mid-flight) is accepted, and recorded in `NEXT.md` when the slice is built.
- **When:** LEGACY-B is built AFTER the #168–#171 stack merges, stacked on master then.
  **SUPERSEDED by the seat, 2026-10-01:** LEGACY-B stacks on #171 (`ab23ce6f`) NOW, with its own PR and its own GATE
  committed before any boot, and WITHER-STATUS and WITHERED-SHORTBOW stack above it. One boot at the new stack top,
  after the seat has diffed every slice. LEGACY-B's R0c predicts against LEGACY-A's. Nothing in #168–#171 changes. So
  §7's *"Why not stacked on #171 now"* is overruled: the staff file #171 removed is re-added by LEGACY-B, and #171's BK
  rows no longer read the staff.

### BEN, 2026-09-29, VERBATIM: the answers to Q-B1, Q-B4 and Q-B6

```
  Q-B1: "yes, blaze fire ball"
  Q-B4: "8 second despawn time, not a set block range"
  Q-B6: "use the old sounds and effects"
```

Q-B7 was not answered.

**THE SEAT'S READING (the seat's, not Ben's):**

- **Q-B1:** a `SmallFireball`.
- **Q-B4:** `max_lifetime_ticks: 160`. The drive removes the entity at 160 (Q-B5). The range is whatever 160 ticks of
  flight covers: **no block range is authored or predicted**, and the LB rows read the despawn time.
- **Q-B6:** cfde822's sounds and particles, through the visual system as new visual entries. **R0c's visual count
  moves, so the gate predicts it.** Every parameter below is carried as **PROVISIONAL**.
- **Q-B7:** keep *"Throws a burning charge."* as the default.

**cfde822's exact call sites, read from `StaffListener` at cfde822:**

| when | cfde822 method | call | parameters |
|---|---|---|---|
| cast | `onBlazeStaffInteract`, the fireball arm | `playSound(player.getLocation(), ENTITY_BLAZE_SHOOT, …)` | volume **1.0**, pitch **1.2**, at the player |
| impact (entity AND block) | `onBlazeFireballHit` (`ProjectileHitEvent`) | `spawnParticle(FLAME, fireball location, …)` | count **18**, offsets **0.25 / 0.25 / 0.25**, extra **0.06** |
| impact | `onBlazeFireballHit` | `spawnParticle(LAVA, fireball location, …)` | count **5**, offsets **0.1 / 0.1 / 0.1**, extra **0** |
| impact | `onBlazeFireballHit` | `playSound(fireball location, ITEM_FIRECHARGE_USE, …)` | volume **1.0**, pitch **1.3** |

**NOT carried, because they are not the Fireball's:**
- the flint staff's `ITEM_FLINTANDSTEEL_USE` / `BLAZE_SHOOT 0.6/1.6` pair and its `FLAME ×12` / `×14` impacts, in the
  same class;
- Smolder's `LAVA ×25` and `ITEM_FIRECHARGE_USE 1.0/0.8`, since Smolder is removed.

cfde822 had no flight particles on the fireball: the entity's own vanilla rendering was the trail.

---

## 9. UNVERIFIED, IN ONE PLACE

- **Client rendering** of a SmallFireball: whether it is billboarded, whether a flame overlay draws, whether the smoke
  trail and the 3.5-block render gate behave as the shared class reads. LB3 reads these.
- **The effective tracking range** after Paper/Spigot config (`clientTrackingRange 4` is the builder's).
- **Whether any entity besides a breeze deflects** a projectile through `Entity.deflection`.
- **The `/summon` keys** `Motion` and `acceleration_power` as parsed in 26.1. Only the save side
  (`acceleration_power`) was read.
- **Any Paper config that despawns fireballs**: none read, none looked for.
- **Folia** ordering for the drive law.
- **What a left-click does with a right-only staff** (LB1): not surveyed.
