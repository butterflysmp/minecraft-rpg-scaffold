# PLAN — Mob scaling: 5× vanilla, gear score by distance, and the nameplate

**Status: PLAN ONLY. No code, no boot.** Branched from `origin/master` at `35d2526` (read with
`git ls-remote origin master`, 2026-09-26). #161 (Recall) is untouched and independent: nothing below
edits a file Recall's branch changes.

Every citation names a **method or section**, never a line (CLAUDE.md, *the pointer names a SECTION*).

---

## §0 THE RULINGS — Ben, 2026-09-27, verbatim. Do not re-derive them.

- **M1.** Players are 5x vanilla health, so every mob is 5x easier. To keep the vanilla feel, EVERY
  vanilla mob gets 5x its vanilla HEALTH and 5x its vanilla DAMAGE.
- **M2.** Hostile mobs have a GEAR SCORE, and it scales their stats: multiplier = GS / 100, applied to
  HEALTH and DAMAGE only.
  - A GS 500 zombie: 20 x 5 x 5.00 = 500 HP.
  - A GS 500 Warden: 500 x 5 x 5.00 = 12,500 HP.
- **M3.** The blanket rule is distance from spawn: every 2,500 blocks is +100 GS. Exceptions come
  later, for boss mobs and activity mobs. Leave a seam for them and build none now.
- **M4.** Custom authored mobs (content/mobs/, e.g. the Knell at 360) do NOT get the vanilla 5x. They
  get only the gear-score multiplier.
- **M5.** A mob's gear score is STORED ON THE MOB. It survives chunk unloads and restarts, and is never
  re-rolled.
- **M6.** The nameplate reads "[GS] Name cur/max ❤", e.g. "[500] Zombie 500/500 ❤". The red heart
  stays.
- **M7.** PASSIVE mobs (cows, villagers, horses, etc.) get the 5x health, but NO gear score and no GS
  on the nameplate.
- **M8.** Environmental damage and healing ON mobs (fall, fire, lava, drowning, regeneration, potions)
  are scaled x5, to keep their vanilla proportion. There is no GS on top.
- **M9.** The Warden's 2,500 (12,500 at GS 500) exceeds vanilla's max-health attribute cap. The seat
  found the answer already in the code: mob HP lives in the plugin's own health store, seeded once
  from vanilla (MobSeeding, MobNameplateManager.seedCombatStats, and NameplateText, "cap-free").
  Scale at the seed. NEVER write the scaled value into the vanilla attribute.
- **M10.** Balancing player weapons and abilities is out of scope (Ben: "we'll do that later"). Loot
  and XP scaling are out of scope too.
- **M11.** THE NETHER starts at GS 200, and grows 8x faster than the overworld.
- **M12.** THE END starts at GS 300, and grows at the normal rate.

**The curves.** Distance is horizontal (X/Z), from that world's spawn point:

| world | GS | at 2,500 blocks |
|---|---|---|
| Overworld | `100 + distance / 25` | 200 |
| Nether | `200 + distance * 8 / 25` | 1,000 |
| End | `300 + distance / 25` | 400 |

**The seat's defaults — NOT ruled by Ben; each is overrulable:**
- the curves are continuous, not stepped;
- there is no cap;
- the overworld starting value is 100;
- a mob's GS is rounded to a whole number for storage and display.

---

## §1 FINDINGS — each confirmed or corrected from source

### 1.1 Mob health: where the seed runs, what re-seeding does, and what survives

**Confirmed.** The seed is `MobNameplateManager.seedCombatStats`:
it reads the `mob_id` PDC tag, asks `MobSeeding.maxHealth(registry, mobId, vanillaMax)` for the max
(the definition's `max_health` when tagged and known, else the vanilla `MAX_HEALTH` attribute), reads
`ATTACK_DAMAGE` through `attackDamageOf`, and calls `CombatantStats.bootstrapIfAbsent` —
**register-if-absent**, so a second call is a no-op.

**Where it runs.** Three callers, all on the entity's own thread:

| caller | trigger |
|---|---|
| `RpgListeners.onEntityAdd` → `onMobAppear` → `seedCombatStats` | `EntityAddToWorldEvent` — spawn **and** chunk load both arrive here |
| `RpgListeners.onMobMeleeAttack` | belt-and-braces re-seed before reading the attack stat |
| `RpgCommand.mobMutate` (`/rpg mobdamage`, `/rpg mobheal`) | **CORRECTED — it does NOT call `seedCombatStats`.** It calls `bootstrapIfAbsent` itself with the raw vanilla attribute values, then `onMobAppear`. That is a **second seed path that bypasses `MobSeeding`**. Today it is harmless only because the add event always seeds first. Under scaling it would seed an unscaled mob if it ever ran first. Slice 1 routes it through `seedCombatStats`. |

**The vanilla attribute is never written for a mob.** The only `MAX_HEALTH` `setBaseValue` in
`paper/src/main` is `EntityHeartBar`, which is the player bar. So M9 is already true, and slice 1 must
keep it true. The store is uncapped (`HealthState`'s class javadoc), and so is the nameplate
(`NameplateText.of` does `Math.round` on doubles).

**What survives unload and restart: nothing in the store.** `onEntityRemove` → `onMobRemove` calls
`CombatantStats.clear` on **death, despawn and chunk unload alike**. On reload the add event re-seeds a
**fresh `HealthState` at full**. So today a mob's max survives an unload only because it is
recomputed from the same inputs. Its **current HP does not survive**: a damaged mob heals to full
across an unload, a restart or a portal. This is pre-existing, and is recorded in §6 F1.

**Consequence for M5.** The GS must live in the entity's PDC. The store cannot hold it, because it
forgets every mob that leaves a loaded chunk. The seed must read the stored GS before it rolls one.

### 1.2 Mob damage on a player: every path, and what the plugin sees today

There are **two pricing routes**, and they already disagree. **That disagreement is the most important
finding in this plan.**

- **Route A — `onMobMeleeAttack`**, gated to `DamageCause.ENTITY_ATTACK`. The amount is
  `stats().attackValue(attacker)`: the mob's seeded `ATTACK_DAMAGE` attribute, **×1**. Vanilla's
  `event.getDamage()` is tokened and discarded.
- **Route B — `onEnvironmentalDamage`**, for every cause that `VanillaDamagePolicy.forCause` REROUTEs
  on a tracked victim. This includes PROJECTILE, ENTITY_EXPLOSION, SONIC_BOOM, MAGIC, POISON, WITHER
  and THORNS. The amount is `DamageScale.toCustom(event.getDamage(), customMax, …, barIsPuppeted)`.
  For a player the bar is puppeted, so **k = playerMax / 20**, which is 5 for a base player.

> **ROUTE B IS ALREADY ×5 FOR A BASE PLAYER.** A skeleton's arrow, a creeper's blast and a warden's
> boom land today at five times their vanilla number on a 100-HP player. Only Route A (melee) is at
> ×1. So M1's "×5 damage" is **already true for every non-melee path**, but through a
> **percentage-of-max** conversion, not a flat ×5. At player max 400 it is ×20. **A slice that
> multiplies Route B by 5 again lands every arrow at ×25.** Slice 2's pricing must REPLACE Route B's
> conversion for mob-sourced damage, not stack on it. Whether that replacement is flat or stays
> percentage-of-max is §5 Q4.

| path | cause (see note ‡) | route today | amount today | where ×5 × GS/100 applies |
|---|---|---|---|---|
| melee: zombie, vindicator, piglin, spider, enderman, vex, bee sting's hit, warden melee, ravager, hoglin, dragon melee, iron golem | ENTITY_ATTACK | A | seeded `ATTACK_DAMAGE` ×1 | **at the seed**: seed attack = attribute × 5 × GS/100 (MobScaling). `onMobMeleeAttack` is unchanged. |
| arrows: skeleton, stray, bogged, parched, pillager crossbow | PROJECTILE | B | `getDamage()` × playerMax/20 | the mob-sourced branch of Route B; GS read off the **projectile** (stamped at launch) |
| trident (drowned) | PROJECTILE | B | same | same |
| blaze small fireball, ghast fireball impact | PROJECTILE ‡ | B | same | same; ghast blast below |
| ghast fireball blast, wither skull blast | ENTITY_EXPLOSION ‡ | B | same | same; GS from the direct entity (the fireball or skull) |
| wither skull hit | PROJECTILE ‡ | B | same | same |
| llama spit | PROJECTILE | B | same | same. A llama is **passive** (§1.4), so ×5 with no GS. |
| shulker bullet | PROJECTILE ‡ | B | same | same (hit damage only; the levitation is an effect, not damage) |
| creeper explosion | ENTITY_EXPLOSION | B | same | the mob-sourced branch; GS off the creeper itself (the direct entity) |
| witch splash harming | MAGIC | B | same | the mob-sourced branch; GS off the thrown potion, stamped at launch |
| witch poison, wither-skeleton wither, cave-spider and bee poison, the wither's wither effect | POISON / WITHER | B | `getDamage()` × playerMax/20 | **CANNOT BE SCALED BY GS TODAY.** A damage-over-time tick names no causing entity, so no GS is reachable. It stays ×5 by Route B, with no GS. §6 F5. |
| warden sonic boom | SONIC_BOOM | B | same | the mob-sourced branch; GS off the warden |
| evoker fangs | MAGIC ‡ | B | same | the mob-sourced branch; GS off the **causing** evoker (fangs are not a projectile and are not stamped) |
| guardian / elder guardian beam | MAGIC ‡ | B | same | the mob-sourced branch; GS off the guardian |
| dragon's breath cloud | MAGIC or DRAGON_BREATH ‡ | B | same | GS off the causing dragon, if the cloud names it (‡) |
| vex | ENTITY_ATTACK | A | ×1 | at the seed |
| pufferfish contact | ENTITY_ATTACK ‡ | A | **0 if it has no `ATTACK_DAMAGE` attribute** (`attackDamageOf` returns 0.0) | at the seed, and it stays 0. §6 F4. |

> ‡ **UNMEASURED.** Which vanilla `DamageType` maps to which Bukkit `DamageCause`, and which entity
> each event names as *direct* and *causing*, was **not read** for this plan. The route column holds
> whatever the cause is, because Route B catches every REROUTE cause. What changes is only *where the
> GS is read from*. Slice 2's first commit is a probe logger (cause, direct type, causing type,
> vanilla amount, applied amount), run once against each row, **before** the pricing is written.
> Settle ambiguous rows with `javap` against the `run/versions` server jar (memory:
> *javap is the jar instrument*).

**Paths the plugin cannot scale today, as findings:**
- **F-DoT.** POISON and WITHER ticks carry no source, so no GS reaches them (§6 F5).
- **F-melee-attribute.** Route A prices every mob melee hit from the **seed-time** `ATTACK_DAMAGE`.
  It ignores vanilla's own amount, so it ignores:
  - a weapon or enchant the mob picks up after it was seeded;
  - vanilla's random ranges (the iron golem's);
  - difficulty;
  - any mob whose contact damage is not that attribute.

  Pre-existing, and recorded in §6 F4. Scaling multiplies it faithfully, flaws included.
- **F-difficulty (UNMEASURED).** Route A reads an attribute, which does not change with difficulty.
  Route B reads `getDamage()`. Whether vanilla applies its difficulty adjustment to a player victim
  **before** the Bukkit event is built was **not read**. If it does, Route A and Route B also disagree
  by difficulty. It is settled by `javap` on the player's `hurtServer` in slice 2, and neither route
  is changed on a guess.

### 1.3 Environmental damage and healing on mobs

**Damage — confirmed, and it is ALREADY SCALED, by accident, BY THE WRONG FACTOR once GS exists.**
`onEnvironmentalDamage` gates on `stats().tracks(id)` (every seeded mob), then REROUTEs the cause
through `damageWindow.claim` and the shield resolution. The amount is
`DamageScale.toCustom(amount, customMax, vanillaMaxAttribute, barIsPuppeted=false)`. For a mob,
**k = customMax / its real `MAX_HEALTH` attribute**:

| mob | today | after slice 1 seeds scaled HP (the vanilla attribute unchanged, per M9) |
|---|---|---|
| zombie | 20 / 20 → **×1** | GS 100: 100/20 → ×5. GS 500: 500/20 → **×25** |
| Knell (custom) | 360 / 20 → **×18** | GS 100: ×18. GS 200: ×36 |

So **slice 1 alone changes environmental damage on every mob**, with no edit to this handler. At GS
100 it lands exactly on M8's ×5. Above GS 100 it carries the GS on top, which is **what M8 says not to
do**. Slice 3 replaces the mob branch's factor with MobScaling's environmental factor. Between slices
1 and 3 the proportional (GS-on-top) behaviour ships. Say so in slice 1's PR body.

**Healing — CORRECTED: no vanilla heal reaches a mob's store at all.** `onRegainHealth` returns on its
first line for any non-`Player`. Its javadoc says so ("*Scope: tracked players only. A mob's health is
its own store's business and no vanilla heal is currently rewriting it*"). So these all move only the
mob's vanilla health, which nothing reads:
- a witch drinking healing;
- a regeneration effect on a mob;
- the wither's regen;
- ender crystals healing the dragon;
- a horse eating.

On the store they are no-ops. M8's heal half is **new wiring, not a rescale**: slice 3 adds a mob arm
with its own exhaustive policy.

**Instant health and harming on undead** arrive as damage (MAGIC), not heals. They ride Route B like
any MAGIC.

### 1.4 Hostile vs passive — how to tell them apart, and the edge cases

**Measured from the pinned `paper-api-26.1.2.build.74-stable.jar` (javap over every
`org.bukkit.entity` interface, the `extends` closure computed from the output).** `org.bukkit.entity.Enemy`
is implemented directly by `Monster`, `Slime`, `Ghast`, `Phantom`, `Shulker`, `Hoglin` and `EnderDragon`.
Its closure is:

> AbstractSkeleton, Blaze, Bogged, Breeze, CaveSpider, Creaking, Creeper, Drowned, ElderGuardian,
> EnderDragon, Enderman, Endermite, Evoker, Ghast, Giant, Guardian, Hoglin, Husk, Illager, Illusioner,
> MagmaCube, Parched, Phantom, PigZombie, Piglin, PiglinAbstract, PiglinBrute, Pillager, Raider,
> Ravager, Shulker, Silverfish, Skeleton, Slime, Spellcaster, Spider, Stray, Vex, Vindicator, Warden,
> Witch, Wither, WitherSkeleton, Zoglin, Zombie, ZombieVillager

**Recommendation: `mob instanceof Enemy` is the hostile test.** It is one interface, maintained by
Paper, and it covers every vanilla mob a player thinks of as hostile. The alternative is a list of our
own, which goes stale silently at the next mob drop. `Monster` alone is too narrow: it misses Slime,
Ghast, Phantom, Shulker, Hoglin and the dragon.

**THE EDGE CASES, for Ben** — each with the recommended rule:

| case | members | `Enemy`? | recommendation |
|---|---|---|---|
| neutral, inside `Enemy` | Enderman, Spider, Cave spider, Piglin, Zombified piglin (`PigZombie`) | yes | **HOSTILE: GS.** They fight back hard, and a GS-scaled enderman is the vanilla feel. |
| neutral, outside `Enemy` | Wolf, Bee, Iron golem, Llama, Trader llama, Polar bear, Goat, Dolphin, Panda, Fox | no | **PASSIVE: ×5 health, ×5 damage, no GS** (M1 still gives them ×5 damage, and M7 withholds GS). |
| slimes and magma cubes | Slime, MagmaCube, and their split children | yes | **HOSTILE; children INHERIT the parent's GS.** A child's HP comes from its own smaller vanilla max ×5 × the inherited GS. |
| tamed mobs | tamed wolf, cat, parrot, horse, and the rest | no (they are `Tameable`, and `Tameable` extends `Animals`) | **PASSIVE**, the same as untamed. Taming changes no stats. |
| undead mounts | Zombie horse, Skeleton horse (skeleton-trap horsemen), Zombie nautilus | no | **PASSIVE** by the rule. Their riders are hostile and get GS. Flagged because it will look odd that the mount has no `[GS]`. |
| hostile-looking `Animals` | Hoglin | yes (`Animals` **and** `Enemy`) | **HOSTILE**. `Enemy` decides; `Animals` does not. |
| the friendly ghast | HappyGhast | no (`Animals`) | **PASSIVE**. |
| the killer bunny | Rabbit (a variant of a passive type) | no | **PASSIVE**. The variant is not visible to the type test. |
| bosses | Wither, EnderDragon, Warden, ElderGuardian | yes | **HOSTILE, distance GS for now.** M3's exception seam is where they leave the curve later. |
| custom mob on a passive base | none shipped | n/a | M4 + M7 give **×1 and no GS**. Stated so the arm is decided rather than discovered. |
| players and armor stands | n/a | n/a | never seeded (the first guard in `seedCombatStats`) |

### 1.5 Custom mobs

**Confirmed.**
- `MobDefinition(id, baseEntity, displayName, maxHealth)`. There is **no `attack_damage` field**, so a
  custom mob's melee is seeded from its base entity's vanilla `ATTACK_DAMAGE` (`attackDamageOf`).
- `MobRegistry` is immutable-after-load, keyed by the `mob_id` tag (`Keys.mobId`, PDC STRING
  `mob_id`).
- `/rpg spawn <mob>` (`RpgCommand.spawnMob`) tags `mob_id`, sets the real `CustomName` and
  `CustomNameVisible=false` **inside the pre-spawn consumer**. That ordering is load-bearing: the add
  event seeds register-if-absent, so a tag written afterwards is ignored. **A GS written by
  `/rpg spawn … [gs]` has exactly the same constraint.**
- The only shipped custom mob is `knell.yml` (`wither_skeleton`, `max_health: 360`).

**Consequence of M4 that Ben should see: under M4 the Knell's damage is not scaled at all at GS 100.**
With no `attack_damage` of its own, it hits with a vanilla wither skeleton's attribute at ×1. After
slice 2, an **ordinary** wither skeleton hits ×5. **So the named boss would hit one fifth as hard as the
common mob it is built on.** §5 Q5.

### 1.6 The nameplate

**Confirmed.**
- `NameplateText.of(baseName, cur, max)` builds `<name> <cur>/<max> ❤` with the heart in
  `NamedTextColor.RED`, and the numbers `Math.round`ed.
- `MobNameplateManager` caches one `Nameplate` per mob: a fixed `baseName` plus an atomic
  (text, version).
- `onChange` rebuilds the text from `nameplate.baseName()` on every `HealthChange`.
- `tickViewer` runs a per-viewer 4-tick LOS loop and sends through `PacketNameplateSender`, which
  writes a per-viewer override of metadata index 2 (custom name). **The mob's real name is never
  written.**

**Where the GS goes.** The GS never changes (M5), so it belongs in the **fixed** half. Slice 1 bakes the
`[GS] ` prefix into the `baseName` handed to `registerIfAbsent`. `onChange` then carries it for free,
and no second field or version path is needed. A passive mob gets no prefix (M7).

**An opted-out mob** (`Keys.nameplateOptOut` present) is seeded and never plated. `onMobAppear` returns
after `seedCombatStats`. So it **still gets scaled stats and a stored GS**; it only shows no plate.
That is correct: its damage must not depend on its display. (Nothing in `paper/src/main` writes that
key; see §6 F8.)

### 1.7 What counts as "on spawn": when the GS is assigned, and from where

**One rule covers every row: the GS is assigned at the FIRST seed that finds no stored GS, from the
mob's position at that moment, and written to its PDC. Every later seed reads it back.** The first
seed is `EntityAddToWorldEvent`, and every row below except the startup case goes through it.

| arrival | assigned when | from which position | note |
|---|---|---|---|
| natural spawn | add event | spawn position | |
| spawner | add event | spawn position | |
| spawn egg / dispenser | add event | spawn position | |
| breeding | add event | birth position | only passive mobs breed, so nothing is assigned (M7). Hoglins breed and are `Enemy`: a piglet hoglin gets distance GS at birth. |
| raid wave, patrol, zombie reinforcements, jockeys | add event | spawn position | |
| `/summon` | add event | spawn position | **`/summon … {BukkitValues:{"rpg:mob_gear_score":N}}` presets the GS**, because the seed reads before it rolls. A free dev instrument, and also the reason the stored value must be bounds-checked on read (see §2.4). |
| `/rpg spawn <mob> [gs]` | pre-spawn consumer | n/a: the argument | written before the add event, exactly like `mob_id` |
| conversions: villager→zombie villager, zombie→drowned, husk→zombie, piglin/hoglin→zombified, skeleton→stray, zombie villager→villager | `EntityTransformEvent` | n/a: **INHERITED** from the source when the source had one; else the add-event roll at the new position | A villager has no GS, so its zombie gets distance GS at the conversion point. A cured zombie villager becomes passive and its stored GS is simply ignored (M7). |
| slime / magma cube split | `EntityTransformEvent` (reason SPLIT) | **INHERITED** from the parent | |
| already alive in loaded chunks when the plugin enables | **NOTHING seeds them today** | n/a | `RpgPlugin` has no sweep of existing entities. Whether any are loaded before our listeners register on this build (no `load:` key in `paper-plugin.yml`) is **unmeasured**. Slice 1 adds an idempotent enable-time sweep: every living non-player in every loaded world, `onMobAppear` on each entity's own scheduler. §6 F7. |
| **walks or is carried through a Nether or End portal** | the arrival add event **re-seeds**, reads the stored GS, and **KEEPS IT** | n/a | **M5: a mob that changes dimension keeps its stored GS.** A GS 140 zombie that walks into the Nether stays [140], even though the Nether curve at its arrival point would say something else. Its **current HP resets to full** on arrival (§1.1, §6 F1). |

> **The transform ordering is UNMEASURED and is a slice-1 probe, not an assumption.** If
> `EntityTransformEvent` fires **before** the new entity's add event, inheriting is one PDC write. If
> it fires **after**, the child has already been seeded from distance, and the handler must also
> re-seed it. Inheritance needs `computeIfAbsent` to be bypassable for exactly this case, which is a
> real design change. The probe logs both events' order on one zombie-villager conversion and one
> slime split before either handler is written.

---

## §2 ARCHITECTURE — one recommendation each, with the alternative named

### 2.1 `core/mob/MobScaling` — pure

```
maxHealth(double base, boolean isCustom, boolean isHostile, int gs) -> double       (slice 1)
attackDamage(double base, boolean isCustom, boolean isHostile, int gs) -> double    (slice 2)
environmentalFactor(boolean isCustom) -> double                                     (slice 3)
```

- `base` is `MobSeeding`'s answer for health (authored or vanilla) and the vanilla attribute for
  attack.
- Vanilla factor: **5** unless `isCustom` (M4).
- GS factor: **gs / 100** only when `isHostile` (M7).
- **The divisor is its own constant, `MobScaling.GS_BASELINE = 100`. It does NOT reuse
  `GearScore.BASELINE`.** They are different quantities that agree today. `GearScore`'s class javadoc
  already warns that three 100s in that class cannot be told apart by any test, and importing its
  divisor into mob arithmetic would couple item tuning to mob tuning invisibly.

**Recommended: three named methods, each landing in the slice that consumes it.**
*Alternative: one record-returning function `(maxHealth, attackDamage)`.* It is rejected because its
attack half would ship unconsumed for a whole slice. **An output nothing reads is a claim nothing
tests.**

`MobSeeding` keeps its one job (**which base**), and `MobScaling` does **how much**. Folding the
multiplier into `MobSeeding.maxHealth` was the alternative. It is rejected because `MobSeedingTest`'s
separation property (an untagged mob gets vanilla unchanged) is about the **base**, and it should
stay testable apart from the multiplier.

### 2.2 `core/mob/DistanceGearScore` — pure

```
of(MobDimension dimension, double horizontalDistance) -> int
```

- `MobDimension` is a new core enum `{OVERWORLD, NETHER, END}`, because core has no Bukkit.
- Paper maps `World.Environment` onto it. `NORMAL` and **`CUSTOM`** both map to `OVERWORLD`: a custom
  world has no ruling, and the overworld curve is the least surprising.
- Each curve is `start + distance * rateNumerator / 25`. **Multiply before dividing, as written in
  the ruling.** Nether is `200 + distance * 8 / 25`, **not** `distance * 0.32`.
- The result is `Math.round`ed (a seat default).
- **The whole grid is asserted from executed values, never predicted** (memory: *never predict
  floating point*).

The distance is `hypot(dx, dz)` from `World#getSpawnLocation()`. Euclidean is the seat's reading of
"distance", and §5 Q7 offers the alternative.

*Alternative: stepped tiers (`+100` per full 2,500).* It is rejected per the seat's
continuous-curve default, and it would be a one-function change if Ben overrules.

### 2.3 The M3 seam: `core/mob/GearScoreSource`

```
interface GearScoreSource { int gearScoreFor(MobDimension dimension, double horizontalDistance); }
```

**One implementation, `DistanceGearScoreSource`, delegating to `DistanceGearScore`. The seed holds one
field typed as the interface.** That field is the whole seam. There is no chooser, no list, and no
per-mob-type dispatch: M3 says to build none, and a chooser with one arm is an arm nothing exercises.

**The stored GS always wins over every source (M5).** The source is consulted only when the PDC has
none. So a later boss source can never re-roll a mob that already has a score.

*Alternative: a `switch` on entity type inside the seed.* It is rejected because it is the chooser
that M3 defers, built early.

### 2.4 GS storage: the PDC key `mob_gear_score`

- **`Keys.mobGearScore = new NamespacedKey(plugin, "mob_gear_score")`, type `INTEGER`**, on the
  entity. It is written **once**, at the first seed, and only for a hostile mob. A passive mob carries
  no key. Absence means "not hostile, or not yet seeded", and the hostility test is re-run at every
  seed.
- **Not `gear_score`.** That key is the item stamp (`GearScoreItems`, hard-capped at 500 by
  `GearScore.HARD_CAP`). A mob's GS is uncapped and is a different quantity. **Sharing the key means
  any grep for one finds both.**
- **Read-side guard.** `/summon … BukkitValues` can write any integer. A stored value below 1 is
  treated as absent and re-rolled, and the roll is logged. A stored value is never clamped silently
  from above, because no cap is ruled.
- **Proof that it survives**: gate rows **G12** (unload) and **G13** (restart). Each is witnessed by
  a `source=stored` seed log line for that UUID, **after** a removal line. Without that, a re-roll at
  the same position would print the same number and pass (see §4).

**The GS must also be readable off-thread**, for slice 2. A creeper's blast is priced on the
**victim's** thread, and on Folia the creeper may be owned by another region. So the seed also records
the GS in a `MobGearScores` map (a concurrent `UUID → int`) in paper/, forgotten in `onEntityRemove`
beside `meleeHits.forget`. **Projectiles are stamped with the shooter's GS at `ProjectileLaunchEvent`
(the same key)**, because an arrow can outlive its shooter and usually shares the victim's region.

### 2.5 Dev commands: `/rpg spawn <mob> [gs]` and `/rpg mobinfo`

**`/rpg spawn <mob> [gs]`**, extending the existing literal.
- `<mob>` resolves against `MobRegistry` first, then as a vanilla `EntityType` key (`zombie`,
  `minecraft:warden`). A custom id that shadows a vanilla name wins, and `ContentValidator` warns at
  boot.
- `[gs]` is an `IntegerArgumentType.integer(1, 10_000_000)`, written **inside the pre-spawn consumer**
  next to `mob_id`. Without `[gs]`, the mob rolls from its position like any other spawn.
- Given a passive type, `[gs]` is refused with a red line, not silently dropped.

*Alternative: a separate `/rpg spawnvanilla`.* It is rejected because one verb with one resolution
order is less to remember, and the pre-spawn-consumer rule then lives in one method.

**`/rpg mobinfo`** reads the looked-at mob, using the same aim ray as `mobMutate`. It prints:
- type, and `mob_id` or `-`;
- hostile or passive;
- the stored GS, or `none`, and **the GS its current position would roll**;
- custom cur/max and custom attack;
- **the vanilla `MAX_HEALTH` and `ATTACK_DAMAGE` attributes**.

The last line is what lets the gate check "×5" against a printed number rather than a remembered one,
and check M9 (the attribute still reads vanilla).

*Alternative: put the GS in `/rpg mobdamage`'s output.* It is rejected because a read should not
cost the mob HP.

### 2.6 `/rpg mobdamage` and `/rpg mobheal` under scaling

**The semantics are unchanged: the amounts are CUSTOM HP, typed by the operator, never scaled.** They
are instruments, and an instrument that multiplied by the target's GS would make every reading a
calculation. The one change is F2's: `mobMutate` seeds through `seedCombatStats` instead of its own
raw `bootstrapIfAbsent`, so a mob first touched by the command is scaled like any other.

---

## §3 SLICES — core tests first, then wiring; mutations and gate rows per slice

The seat's three-way cut holds. **One addition to it: GS assignment and storage ride in slice 1**,
because the nameplate cannot show a GS that nothing assigns. Slices 1 and 2 must both merge before
anyone plays. After slice 1 alone, mobs have ×5 HP and ×1 melee (§1.3 gives the interim environmental
behaviour).

### Slice 1 — health, GS assignment and storage, the nameplate, the dev commands

**Core, tests first:**
- `MobScaling.maxHealth` in `MobScalingTest`: the full grid over {vanilla, custom} × {hostile,
  passive} × GS {100, 200, 500, 1000}. It includes the M2 examples (zombie 20 → 500 at GS 500, Warden
  500 → 12,500) and the Knell (360 → 360 at GS 100).
- `DistanceGearScore` in `DistanceGearScoreTest`:
  - each world at 0, 2,500 and 10,000 blocks, plus `CUSTOM`→overworld;
  - **a rounding row at a half** (overworld d = 12.5 → 100.5);
  - a Nether row at a distance where `d*8/25` and `d*0.32` round differently, if one exists. Find it
    by executing, not by hand. If none is found in the range, say so in the PR rather than invent a
    row.
- `MobSeedingTest` is unchanged. It must still pass untouched, and that is its point.

**Paper wiring:**
- `Keys.mobGearScore`.
- `MobClassifier.isHostile(LivingEntity)`, which is `instanceof Enemy`.
- `MobNameplateManager.seedCombatStats`: read the stored GS, else roll from the source and write it
  (hostile only); scale the base through `MobScaling.maxHealth`; record the GS in `MobGearScores`.
  **The attack stays ×1 in this slice.**
- `onMobAppear` passes the `[GS] `-prefixed base name.
- `NameplateText` gets `of(OptionalInt gs, baseName, cur, max)` and `NameplateTextTest` rows. The
  prefix is plain text; colour is §5 Q10.
- `RpgListeners`: an `EntityTransformEvent` inheritance handler, **after the ordering probe**.
- `RpgPlugin`: the enable-time sweep (F7).
- `RpgCommand`: `spawn … [gs]` with vanilla types, `mobinfo`, and `mobMutate` via `seedCombatStats`.
- The stale javadocs listed in §6 F9 and F10 are corrected in the same commit as the code they
  describe.

**Mutations** (each must redden a named test; revert and re-run green; follow `verification.md`,
*THE EIGHT WAYS A MUTATION LIES*):
- the vanilla factor 5 → 1;
- `isCustom` ignored (the Knell reads 1,800);
- `isHostile` ignored (a cow reads a GS);
- `/ GS_BASELINE` → `/ 50`;
- the Nether rate 8 → 1;
- the End start 300 → 100;
- round → floor (reddened only by the half row).

**The unreachable-arm warning** (*AND THE ARM THAT MOST EARNS ITS KEEP…*): the "stored GS < 1" guard is
reachable **only** through `/summon BukkitValues`. Its gate row (G15) uses exactly that, or it is not
witnessed.

**Gates:** R0, then G1–G7, G12–G16.

### Slice 2 — damage, every path from §1.2

**First commit: the probe logger** (cause, direct entity, causing entity, vanilla amount, applied
amount, resolved GS). It is run against each §1.2 row, and **not merged**, the same way Recall's
tuning logger was a spike. Its output fills the ‡ cells in this plan's §1.2 table, in the PR.

**Core, tests first:**
- `MobScaling.attackDamage`, the same grid as `maxHealth`.
- `MobDamagePricing.fromEvent(vanillaAmount, gsOrAbsent)`, with the body decided by §5 Q4:
  - **flat**: `vanilla × 5 × gs/100`;
  - **percentage**: `DamageScale.toCustom(...) × gs/100`.

  **Its test must include a player max ≠ 100 row**, because at max 100 the two readings are the same
  number (§4 G9's note).

**Paper wiring:**
- `seedCombatStats` seeds the attack through `MobScaling.attackDamage`, so melee is done at the seed.
- `onEnvironmentalDamage`: when the victim is a **player** and the damage is **mob-sourced** (a
  non-player `LivingEntity` resolves as causing, or the direct entity carries `mob_gear_score`), price
  through `MobDamagePricing` **INSTEAD OF** `DamageScale`, never after it.
- **`DamageScale.toCustom` keeps its "one call site" property**: the branch chooses one of the two
  prices, and does not chain them.
- GS resolution order:
  1. the direct entity's PDC (projectile, potion or fireball, stamped at launch);
  2. `MobGearScores` for the causing UUID;
  3. **absent: ×5 with no GS, and a WARN line.** An unresolved GS must be visible, never ×1 in
     silence.
- `ProjectileLaunchEvent` stamps the projectile.
- Content: whatever §5 Q5 rules for the Knell's attack. If it is an `attack_damage` field, it goes
  on `MobDefinition` with a `MobLoader` row and a `ContentValidator` row.

**Mutations:**
- `attackDamage`'s vanilla factor → 1;
- the mob-sourced branch also applies `DamageScale` (it must redden: ×25);
- the resolution order reversed (a projectile whose shooter was re-rolled; a unit row on the resolver);
- the absent-GS arm returning 0.

**Gates:** R0, G8–G10, plus a probe table covering every §1.2 row that can be produced in-game.

### Slice 3 — environmental ×5 on mobs, and heals on mobs

**Core, tests first:**
- `MobScaling.environmentalFactor(isCustom)`: 5, or whatever §5 Q2 rules for custom mobs.
- A new `MobHealPolicy`: an **exhaustive switch over `RegainReason` with no default arm**, matching
  `VanillaHealPolicy`'s house style. **REROUTE every reason** on a tracked mob, because nothing
  replaces a mob's regeneration the way `HealthRegenSystem` replaces a player's. The one exception is
  a **decided** `CUSTOM` arm (REROUTE, following the damage side's *"rerouting honours it"*).

**Paper wiring:**
- `onEnvironmentalDamage`'s **mob** branch uses `amount × environmentalFactor` in place of
  `DamageScale.toCustom`'s attribute denominator.
- `onRegainHealth` gains a tracked-mob arm: cancel, then `stats().heal(id, amount × 5, id, true)`.
  Per M8 there is no GS, and §5 Q1 and Q3 hold the proportion question.
- Mob-sourced damage on a **mob** victim (a skeleton's arrow in a zombie) stays in this
  environmental bucket. See §6 F12.

**Mutations:** the factor restored to the attribute denominator (the GS 300 row reddens: ×15 vs ×5);
the heal arm's ×5 dropped; `REGEN` moved to PASS.

**Gates:** R0, G11, G17, G18.

---

## §4 GATE ROWS

**Game mode is declared per row** (verification.md, *THE CREATIVE-DIVERGENCE REGISTER*). **Creative
removes the cost that every damage row measures**: a creative player takes no damage and is not
targeted. **Every row that reads damage TO A PLAYER is SURVIVAL.** Read-only nameplate rows may be
CREATIVE.

**R0 (every slice): the deployed build carries this slice.** The startup line names the head sha. If
R0 fails, STOP.

| row | slice | mode | setup | expected | what keeps it from being hollow |
|---|---|---|---|---|---|
| **G1** | 1 | creative | a natural or egg zombie within ~10 blocks of overworld spawn | `[100] Zombie 100/100 ❤` | **A zombie's displayed GS equals its displayed max at every GS** (20 × 5 × GS/100 = GS), so on its own this row cannot tell "max from GS" from "max printed as GS". G1b breaks that. |
| **G1b** | 1 | creative | a spider at spawn, and one placed 2,500 out | `[100] Spider 80/80 ❤`, then `[200] Spider 160/160 ❤` | spider base 16 ≠ 20; `/rpg mobinfo` shows the vanilla attribute as the witness for ×5 |
| **G2** | 1 | creative | a zombie **placed** (egg or `/summon`) at x=2500, z=0 | `[200] Zombie 200/200 ❤` | `mobinfo`'s "would roll" line also reads 200, and the stored line reads `source=rolled` |
| **G3** | 1 | creative | `/summon warden` at spawn | `[100] Warden 2500/2500 ❤`; **`mobinfo` shows vanilla `MAX_HEALTH` = the vanilla value, not 2500**, and there is no error in the log | "no attribute error" is satisfied for free (nothing writes the attribute); **the attribute READ is the witness for M9** |
| **G4** | 1 | creative | `/rpg spawn knell` at spawn | `[100] Knell 360/360 ❤`, not 1800 | the mutation "isCustom ignored" produces 1800 |
| **G5** | 1 | creative | a cow at spawn | `Cow 50/50 ❤`, no `[..]`; `mobinfo` stored GS = none | a cow at 2,500 out also reads 50/50 (it rules out "GS 100 happened to be ×1") |
| **G6** | 1 | creative | a mob at the Nether spawn; one placed 2,500 out | `[200]`, then `[1000]` | read the Nether `getSpawnLocation()` first (`mobinfo` prints it) and place relative to it |
| **G7** | 1 | creative | an End mob (an enderman) near the End centre | `[300]` **if the End spawn point is the centre**. **Read the End `getSpawnLocation()` first.** If it is the obsidian platform ~100 blocks out, a centre mob reads ~[304]: that is **not a failure of the curve**, it is §5 Q11. | the reading is taken against the printed spawn point, not assumed |
| **G8** | 2 | **survival** | an unarmoured player at 100/100; one zombie hit at GS 100 | the player loses **5 × `mobinfo`'s vanilla `ATTACK_DAMAGE`** | the expected number is printed by the server, not predicted |
| **G8b** | 2 | survival | the same, from `/rpg spawn zombie 300` | 15 × the attribute | GS ≠ 100: the multiplier is really exercised |
| **G9** | 2 | survival | a creeper blast and a skeleton arrow, from `/rpg spawn creeper 300` / `skeleton 300`; **the player at max 100, then at max 400** (`health_boost_TEMP`) | the probe log's `applied / vanilla` = **15** at both maxes (flat), or 15 then 60 if Q4 rules percentage | **At GS 100 and max 100, master already prints ×5 for both**, so the row must use GS ≠ 100 **and** max ≠ 100 or it passes on master. It also catches the ×25 stacking bug directly. |
| **G10** | 2 | survival | a GS-300 skeleton shoots, then is `/kill`ed while the arrow flies | the arrow still prices at GS 300 (the stamp) | the "shooter gone" arm of the resolution order |
| **G11** | 3 | creative | a zombie at GS 300 falls, and the probe logs the FALL | `applied / vanilla` = **5**; before slice 3 the same row reads **15** | at GS 100 slices 1 and 3 agree (both ×5), so GS 300 is required |
| **G12** | 1 | creative | a GS-200 zombie; walk out until its chunk unloads; return | still `[200]`; the log shows `REMOVE <uuid>` then `SEED <uuid> source=stored gs=200` | **without the log pair the row is hollow in TIME**: a mob that never unloaded, or re-rolled at the same spot, prints the same [200] |
| **G13** | 1 | creative | a `/rpg spawn zombie 777` zombie; stop and restart the server | still `[777]`, with `source=stored` | 777 is a value **no position near spawn can roll**, so a re-roll cannot fake it |
| **G14** | 1 | survival (the mob must move by itself) | a GS-140 zombie (`/rpg spawn zombie 140`) walks or is pushed through a Nether portal | still `[140]` in the Nether, where any arrival point rolls at least [200] (`mobinfo`'s "would roll" line); **its HP reads full**, which is F1 and expected | the would-roll line proves the two numbers differ |
| **G15** | 1 | creative | `/summon zombie ~ ~ ~ {BukkitValues:{"rpg:mob_gear_score":0}}` | re-rolled from position, with a WARN line | this is the **only** way to reach the <1 arm |
| **G16** | 1 | creative | a zombie villager conversion, and a slime split, from a GS-300 source | the children read [300] | the parent's GS is not the one their position would roll |
| **G17** | 3 | creative | a regeneration potion splashed on a damaged zombie | its plate rises (it never moved before slice 3) | the before-state is taken on master |
| **G18** | 3 | creative | a Knell in lava | `applied / vanilla` = the §5 Q2 ruling (it is 18 today) | |

---

## §5 OPEN QUESTIONS FOR BEN — numbers and feel only, with a recommendation each

**Q1. M8 at GS above 100: "×5" and "keep the vanilla proportion" only agree at GS 100.** A GS 500
zombie has 25× vanilla HP. At ×5, a fall that kills a vanilla zombie outright takes 20% of its bar.
Lava, fire and drowning are all proportionally a fifth as strong on it. The alternative is
×5 × GS/100, which keeps the proportion exactly (and is what the code does by accident after slice
1).
*Recommendation: ×5 as ruled ("no GS on top").* Far-out mobs then shrug off lava traps and fall
farms. Confirm that this is the feel you want.

**Q2. Environmental damage on a CUSTOM mob.** Today the Knell takes **×18** (360/20). The options are
×5 (M8 read as "every mob"), ×1 (M4 read as "no vanilla 5×"), or ×18 (its authored bar relative to
its base).
*Recommendation: ×5.* M8 says "on mobs" without exception.

**Q3. Heals on mobs at high GS (the same shape as Q1).** At ×5, the dragon's crystals (GS 300, 3,000
HP) heal at a third of their vanilla proportion. A witch's healing potion on a GS 1000 witch is
nearly nothing.
*Recommendation: ×5 as ruled.* Flagged because crystals are the dragon fight's main mechanic.

**Q4. A mob's non-melee damage on a player: flat, or percentage of the player's max?** Today an arrow
takes **a percentage of the player's max**. A 4-damage arrow is 20 at max 100 and **80 at max 400**.
Mob melee is flat. Once slice 2 lands, one of these is true:
- **flat**: every mob hit is `vanilla × 5 × GS/100` whatever the player's max. An arrow and a sword
  from one mob then behave the same against a big health pool.
- **percentage**: an arrow keeps scaling with the player's max, and melee does not.

*Recommendation: flat* (M1 literally: "5x its vanilla damage"). This makes max-HP gear worth the same
against arrows as against swords.

**Q5. The Knell's damage.** Under M4, and with no `attack_damage` of its own, the Knell hits at ×1 a
vanilla wither skeleton, while ordinary wither skeletons hit ×5 after slice 2. **The boss would hit a
fifth as hard as its own base mob.**
*Recommendation: give the Knell an authored `attack_damage` (content, not code).* Pick the number;
×5 the wither skeleton's attribute is the "no weaker than its base" floor.

**Q6. Should a custom mob take distance GS?** Today `/rpg spawn knell` at 2,500 blocks would be a GS
200 Knell (720 HP).
*Recommendation: yes, distance, until M3's boss seam gives it an authored GS.* Confirm.

**Q7. The shape of "distance".** Euclidean (circles around spawn) or the larger of |x| and |z|
(squares, matching the world border and how people read coordinates)?
*Recommendation: Euclidean.*

**Q8. No cap, against a capped player.** Item GS hard-caps at 500 (`GearScore.HARD_CAP`). A mob's
passes 500 at **10,000 blocks** in the overworld, **5,000** in the End, and **938** in the Nether.
The Nether's 8× rate is in Nether blocks, so 938 Nether blocks is about 7,500 overworld blocks of
travel.
*Recommendation: keep no cap (the seat's default).* Confirm that the Nether crossing at under a
thousand blocks is intended.

**Q9. The §1.4 hostile line.**
*Recommendation: `Enemy` decides.* Endermen, spiders and piglins get GS. Wolves, bees, iron golems,
llamas, polar bears and goats are passive (×5, no GS). Undead horses and the zombie nautilus are
passive, and their riders are not.

**Q10. The nameplate's `[GS]`: plain, or coloured by band?**
*Recommendation: plain for now.* Colour is a later pass once bands mean something.

**Q11. The End's origin.** "From that world's spawn point". If the End's spawn point turns out to be
the obsidian platform (~100 blocks from the centre), mobs on the main island read ~[304] rather than
[300].
*Recommendation: measure it at slice 1's boot (G7). If it is not the centre, use (0, 0) for the End
only.*

---

## §6 FINDINGS — recorded, not fixed

- **F1. A mob's current HP resets to full on unload, restart and portal.** `onMobRemove` clears the
  store, and the next seed builds a fresh `HealthState` at full. It is pre-existing, and five times
  the HP makes it five times more exploitable (hit, walk away, return). A fix would persist current
  HP (or its fraction) in the PDC beside the GS. It is its own decision, because it changes the store
  from pure memory into partly persisted state.
- **F2. `mobMutate` is a second seed path.** It is **fixed in slice 1** (listed here so the finding is
  not lost if slice 1 is re-cut).
- **F3. Mob→mob melee never enters the store** (`NEXT.md`, *Custom HP moves ONLY through the
  `applyDamage` pipeline*). Scaling does not reach it: an iron golem kills a GS 1000 zombie in
  vanilla time, because the zombie's vanilla health is untouched (M9). It is a SUMMONER prerequisite,
  already recorded there.
- **F4. Mob melee is priced from the seed-time `ATTACK_DAMAGE` attribute.** It misses:
  - later equipment pickup;
  - vanilla's random ranges (the iron golem);
  - difficulty;
  - any contact damage that is not the attribute (the pufferfish candidate prices at 0).

  Scaling multiplies it faithfully.
- **F5. Damage-over-time ticks (POISON, WITHER) cannot carry a GS.** They name no causing entity.
  They stay ×5 through Route B, with no GS. A fix would scale at effect application.
- **F6. Difficulty may split Route A from Route B** (§1.2, unmeasured). It is settled in slice 2 by
  `javap`, not changed on a guess.
- **F7. No enable-time sweep.** A living entity present before our listeners register is never
  seeded until something hits it: no plate, and environmental damage passes to vanilla. **Fixed in
  slice 1** because GS assignment needs it. It is recorded because it is pre-existing.
- **F8. `nameplate_opt_out` has no writer in `paper/src/main`.** Only `/data` or `/summon` can set it.
- **F9. Stale on arrival, fixed in slice 1.**
  - `NameplateText`'s javadoc says *"nothing seeds a mob above vanilla's 1024 this phase"*. Slice 1's
    Warden does.
  - `MobDefinition`'s javadoc has the same shape. It says *"only `maxHealth` is wired this pass"*, and
    that stays true, so it is left alone.
- **F10. `DamageScale`'s class javadoc table** ("untagged mob 16 / 16 → k = 1", "the Knell 360 / 20 →
  k = 18") **becomes false in slice 1 and changes meaning in slice 3.** It is corrected in each of
  those slices' own commits.
- **F11. `GearScore`'s javadoc section *"UNTIL MOBS SCALE, THE SERVER GETS EASIER"*** is the debt
  this plan pays. Revisiting `GearScore.MIN`'s rationale (*"the bound that makes an unscaled-mob world
  playable"*) is M10's balance pass, not this one.
- **F12. Mob-sourced damage on a MOB victim** (a skeleton's arrow in a zombie, a creeper's blast in a
  crowd) is priced as environmental: ×5 after slice 3, with no attacker GS. A decision for the
  mob→mob pass.
- **F13. A player's VANILLA bow on a mob is also priced as environmental** (PROJECTILE, Route B).
  After slice 1 it is ×5 × GS/100 on the mob, and after slice 3 ×5. Weapon balance is M10's; the
  bucket is recorded so the balance pass finds it.
- **F14. Integer range.** At the overworld border (~30,000,000 blocks) the GS is about 1.2 million; in
  the Nether (Nether coordinates) it is about 9.6 million. Both fit an `int`. The HP is a `double` and
  `Math.round` returns a `long`, so nothing overflows. It is noted so no one "fixes" the type.

---

## REPORT CHECKLIST (for the seat)

- The PR is opened against `master` and is **not merged**. #161 is untouched.
- No code, no boot. The only file is this one.
