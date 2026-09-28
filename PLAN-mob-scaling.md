# PLAN — Mob scaling: 5× vanilla, gear score by distance, and the nameplate

**Status: PLAN ONLY. No code, no boot. §5 is fully answered (M13–M23, 2026-09-27) and §2–§4 are
updated to match.** Branched from `origin/master` at `35d2526` (read with
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

> **M8 IS SUPERSEDED BY M13 AND M15** (below). It stays above, verbatim, because it was ruled;
> nothing in this plan implements it.

> **RETIRED, SUPERSEDED BY M25 (2026-09-28).** This curve table is the pre-M25 rule, kept as the record.
> Under M25 the overworld is a flat 100 and the Nether a flat 200. The End is 300 to 1,000 blocks from
> (0, 0), then +100 per 10,000, capped at 500 (`core/mob/DimensionGearScore`). Do not derive from it.

**The curves.** Distance is horizontal (X/Z). The overworld and the Nether measure from that world's
spawn point; **the End measures from (0, 0) (M23)**. **Every curve clamps at 500 (M20):**

| world | GS | at 2,500 blocks | reaches the 500 cap at |
|---|---|---|---|
| Overworld | `min(500, 100 + distance / 25)` | 200 | 10,000 blocks |
| Nether | `min(500, 200 + distance * 8 / 25)` | **500 (capped; the uncapped curve gives 1,000)** | 937.5 blocks |
| End | `min(500, 300 + distance / 25)` | 400 | 5,000 blocks |

**The seat's defaults — NOT ruled by Ben; each is overrulable:**
- the curves are continuous, not stepped;
- ~~there is no cap~~ — **OVERRULED by M20: capped at 500**;
- the overworld starting value is 100;
- a mob's GS is rounded to a whole number for storage and display.

### Ben's rulings on §5, 2026-09-27, verbatim — M13–M23

- **M13.** Environmental damage ON mobs is PROPORTIONAL: vanilla amount / vanilla max HP x the mob's
  max HP. A fall that kills a vanilla zombie kills any zombie, at any GS. This REPLACES M8's flat x5.
- **M14.** The same proportional rule for CUSTOM mobs (the Knell included). No special case.
- **M15.** HEALING on mobs (regeneration, End crystals, potions) is proportional in the same way, so
  the crystals stay as strong as in vanilla.
- **M16.** Mob arrows, blasts and magic hit FLAT: vanilla x 5 x GS/100, the same as melee. This
  REPLACES DamageScale's share-of-player-max-HP pricing for mob-dealt damage. Slice 2 replaces that
  pricing; it does not multiply on top of it.
- **M17.** The Knell's damage: PARKED. It is not worried about now. It keeps its current behaviour;
  note it in §6 as open.
- **M18.** Custom mobs use the distance GS until the M3 boss exceptions exist.
- **M19.** Distance is a CIRCLE: horizontal Euclidean distance.
- **M20.** Mob GS is CAPPED AT 500, in every dimension. The curves clamp at 500 (overworld ~10,000
  blocks, End ~5,000, Nether ~940).
- **M21.** The hostile/passive line is as §1.4 recommends, the Enemy interface plus the listed edge
  cases. Record the full list in §0.
- **M22.** The [GS] on the nameplate is LIGHT BLUE (NamedTextColor.AQUA); the rest of the nameplate
  is unchanged.
- **M23.** The End is measured from (0, 0), always. Not its spawn point.

### Ben's ruling, 2026-09-28 — M24

- **M24.** MOB DAMAGE IGNORES DIFFICULTY. Every mob-to-player hit is exactly vanilla x 5 x GS/100
  (custom: authored x GS/100), on every path, whatever the server's difficulty. Gear score is the
  difficulty dial. **It closes §6 F6.**
  - **M24's reference (the seat, 2026-09-28): "vanilla" in M24 means vanilla at NORMAL, on every
    path.** Each difficulty channel leaves NORMAL untouched and moves EASY and HARD to it.

### Ben's ruling, 2026-09-28 — M25

- **M25.** It REPLACES the M3 distance curve for the overworld and the Nether, and the M23 End curve.
  - OVERWORLD: every hostile mob is GS 100. No distance term.
  - NETHER: every hostile mob is GS 200. No distance term.
  - END: GS 300 within 1,000 blocks of (0,0). Beyond that, +100 GS per 10,000 blocks.

  **The seat's defaults (Ben may overrule; they are defaults, not rulings):**
  - The End curve is CONTINUOUS, like the old ones:
    `GS = 300 + max(0, hypot(x, z) - 1000) / 100`, rounded, clamped at `MobGearScore.CAP` (500).
    Worked points: 1,000 → 300; 6,000 → 350; 11,000 → 400; 21,000 → 500 (the cap).
  - Mobs that already exist keep their stored GS (M5, never re-rolled). That is accepted on the dev
    world. It is stated here, and nothing is migrated.
  - The `GearScoreSource` seam (M3's exceptions) stays.

  **What M25 supersedes, so no reader applies the old curves:** the curve table under *The curves*
  above; M3's "every 2,500 blocks is +100 GS", for the overworld and the Nether; M11's Nether rate; and
  M12's End rate. M11's and M12's starting values (200, 300) and M23's End origin (0, 0) survive into
  M25. M19 (a circle) and M20 (the 500 cap) still apply to the End curve.

### Ben's rulings, 2026-09-28 — M26 and M27

- **M26.** Vanilla's health-based behaviour COMES BACK for scaled mobs, all of it:
  - the dragon's and the Wither's boss bars;
  - the Wither's half-health armour phase;
  - golem cracks;
  - witches drinking;
  - feeding pets and repairing golems;
  - every other reader in §7's table.

  Nothing is carved out; they all read the same number.
- **M27.** The perched dragon never being knocked airborne by damage is DEFERRED to the Ender Dragon
  boss fight work (Ben: "we'll come back to this later when we do our Ender Dragon boss fight").
  Record it as its own §6 finding, cross-referenced from §7. It is NOT in this slice.
  *(Recorded as §6 F22.)*

**M21's full list, as ruled.** HOSTILE (gets a GS) is `mob instanceof org.bukkit.entity.Enemy`, whose
closure in the pinned `paper-api-26.1.2.build.74-stable.jar` is: AbstractSkeleton, Blaze, Bogged,
Breeze, CaveSpider, Creaking, Creeper, Drowned, ElderGuardian, EnderDragon, Enderman, Endermite,
Evoker, Ghast, Giant, Guardian, Hoglin, Husk, Illager, Illusioner, MagmaCube, Parched, Phantom,
PigZombie, Piglin, PiglinAbstract, PiglinBrute, Pillager, Raider, Ravager, Shulker, Silverfish,
Skeleton, Slime, Spellcaster, Spider, Stray, Vex, Vindicator, Warden, Witch, Wither, WitherSkeleton,
Zoglin, Zombie, ZombieVillager. Every other living non-player is PASSIVE (×5 health, ×5 damage, no
GS). The edge cases:

| case | members | ruled |
|---|---|---|
| neutral, inside `Enemy` | Enderman, Spider, Cave spider, Piglin, Zombified piglin (`PigZombie`) | **HOSTILE: GS** |
| neutral, outside `Enemy` | Wolf, Bee, Iron golem, Llama, Trader llama, Polar bear, Goat, Dolphin, Panda, Fox | **PASSIVE: ×5 health, ×5 damage, no GS** |
| slimes and magma cubes | Slime, MagmaCube, and their split children | **HOSTILE; children INHERIT the parent's GS** |
| tamed mobs | tamed wolf, cat, parrot, horse, and the rest | **PASSIVE**, the same as untamed |
| undead mounts | Zombie horse, Skeleton horse, Zombie nautilus | **PASSIVE**; their riders are hostile |
| hostile `Animals` | Hoglin | **HOSTILE** — `Enemy` decides, `Animals` does not |
| the friendly ghast | HappyGhast | **PASSIVE** |
| the killer bunny | Rabbit | **PASSIVE** |
| bosses | Wither, EnderDragon, Warden, ElderGuardian | **HOSTILE, distance GS** until the M3 exceptions exist |
| custom mob on a passive base | none shipped | **×1 and no GS** (M4 + M7) |
| players and armor stands | — | never seeded |

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
> conversion for mob-sourced damage, not stack on it. **M16 rules the replacement FLAT:
> `vanilla × 5 × GS/100`, the same as melee.**

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
- ~~**F-difficulty (UNMEASURED).**~~ **MEASURED in slice 2 and CLOSED by M24** (§6 F6 has the
  account). Vanilla does adjust before the Bukkit event, so Route B was post-difficulty while Route A
  was not. After M24 neither route sees difficulty.

### 1.3 Environmental damage and healing on mobs

**Damage — confirmed, and IT IS ALREADY M13 AND M14, WORD FOR WORD.** `onEnvironmentalDamage` gates
on `stats().tracks(id)` (every seeded mob), then REROUTEs the cause through `damageWindow.claim` and
the shield resolution. The amount is
`DamageScale.toCustom(amount, customMax, vanillaMaxAttribute, barIsPuppeted=false)`. For a mob that is
**vanilla amount / its real `MAX_HEALTH` attribute × its custom max** — M13's formula exactly, and
the same arithmetic for a custom mob (M14):

| mob | today | after slice 1 seeds scaled HP (the vanilla attribute unchanged, per M9) |
|---|---|---|
| zombie | 20 / 20 → **×1** | GS 100: 100/20 → ×5. GS 500: 500/20 → **×25** |
| Knell (custom) | 360 / 20 → **×18** | GS 100: ×18. GS 200: ×36 |

So **the environmental damage half of M13/M14 needs no code change**: slice 1 raises the custom max
and the proportion follows. A fall that kills a vanilla zombie (`amount ≥ 20`) deals at least the
zombie's whole custom max, at any GS.

> **THIS IS A SECOND REASON FOR M9, AND IT IS SILENT.** The proportion is only right because the
> denominator is the **vanilla** attribute. Anything that ever writes the scaled max into
> `MAX_HEALTH` turns every environmental hit on a mob into ×1 — with no error, and with the nameplate
> still reading correctly. Slice 1's M9 gate row (G3) guards this too, and slice 3 pins it with
> `DamageScaleTest` rows at GS ≠ 100.

**Healing — CORRECTED: no vanilla heal reaches a mob's store at all.** `onRegainHealth` returns on its
first line for any non-`Player`. Its javadoc says so ("*Scope: tracked players only. A mob's health is
its own store's business and no vanilla heal is currently rewriting it*"). So these all move only the
mob's vanilla health, which nothing reads:
- a witch drinking healing;
- a regeneration effect on a mob;
- the wither's regen;
- ender crystals healing the dragon;
- a horse eating.

On the store they are no-ops. M15's heal half is **new wiring, not a rescale**: slice 3 adds a mob arm
with its own exhaustive policy, priced by the same proportion as the damage (vanilla amount /
vanilla max × the mob's custom max), so an End crystal is exactly as strong as in vanilla.

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

**RULED as M21: `mob instanceof Enemy` is the hostile test, plus the edge cases.** The full list and
the edge-case table are in §0, under M21 — **that is the account; this is the pointer**, so the two
cannot drift. Why `Enemy` rather than the alternatives: it is one interface, maintained by Paper, so
it does not go stale at the next mob drop the way a list of our own would; and `Monster` alone is too
narrow (it misses Slime, Ghast, Phantom, Shulker, Hoglin and the dragon).

Two notes the table does not carry. A tamed mob is `Tameable`, which extends `Animals`, so taming
changes nothing. A split slime child's HP comes from its own smaller vanilla max × 5 × the inherited
GS.

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

**Consequence of M4: the Knell's damage is not scaled at all at GS 100.** With no `attack_damage` of
its own, it hits with a vanilla wither skeleton's attribute at ×1. (Slice 2 applies M4's GS/100 to
that base, which changes nothing at GS 100; whether M17's "keeps its current behaviour" means the GS
factor too is flagged in §6 F15 for the seat's diff.) After slice 2, an **ordinary** wither skeleton hits ×5. **So the named boss hits one fifth as
hard as the common mob it is built on. PARKED by M17**: it keeps its current behaviour, and it is
open in §6 F15. Slice 2 adds no `attack_damage` field.

**M18: a custom mob takes the distance GS** like any hostile mob, until the M3 boss exceptions exist.
A Knell spawned 2,500 blocks out is GS 200, 720 HP. *(RETIRED by M25: an overworld Knell is GS 100, 360 HP,
at any distance. M18 itself stands; it now gives a custom mob the dimension GS.)*

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
```

**There is no environmental factor, by ruling.** M13–M15 make environmental damage and healing on a
mob **proportional** (vanilla amount / vanilla max × the mob's custom max), and that arithmetic
already exists as `DamageScale.toCustom` with `barIsPuppeted = false` (§1.3). A second copy of it in
`MobScaling` would be two accounts of one rule. The draft of this plan had an
`environmentalFactor(isCustom)` returning ×5; M13 withdrew it.

- `base` is `MobSeeding`'s answer for health (authored or vanilla) and the vanilla attribute for
  attack.
- Vanilla factor: **5** unless `isCustom` (M4).
- GS factor: **gs / 100** only when `isHostile` (M7).
- **The divisor is its own constant, `MobScaling.GS_BASELINE = 100`. It does NOT reuse
  `GearScore.BASELINE`.** They are different quantities that agree today. `GearScore`'s class javadoc
  already warns that three 100s in that class cannot be told apart by any test, and importing its
  divisor into mob arithmetic would couple item tuning to mob tuning invisibly.

**Recommended: two named methods, each landing in the slice that consumes it.**
*Alternative: one record-returning function `(maxHealth, attackDamage)`.* It is rejected because its
attack half would ship unconsumed for a whole slice. **An output nothing reads is a claim nothing
tests.**

`MobSeeding` keeps its one job (**which base**), and `MobScaling` does **how much**. Folding the
multiplier into `MobSeeding.maxHealth` was the alternative. It is rejected because `MobSeedingTest`'s
separation property (an untagged mob gets vanilla unchanged) is about the **base**, and it should
stay testable apart from the multiplier.

### 2.2 `core/mob/DistanceGearScore` — pure

> **REWRITTEN BY M25 (2026-09-28): the class is now `core/mob/DimensionGearScore`**, and
> `GearScoreSource.DISTANCE` is now `BLANKET`. The overworld and Nether are flat; the End is
> `300 + max(0, d - 1000) / 100`, rounded and clamped at `MobGearScore.CAP`. The section below is the
> pre-M25 design, kept as the record. Its rounding, clamp and NaN-guard reasoning carries over to the
> End curve, and its rates do not.

```
of(MobDimension dimension, double horizontalDistance) -> int
```

- `MobDimension` is a new core enum `{OVERWORLD, NETHER, END}`, because core has no Bukkit.
- Paper maps `World.Environment` onto it. `NORMAL` and **`CUSTOM`** both map to `OVERWORLD`: a custom
  world has no ruling, and the overworld curve is the least surprising.
- Each curve is `start + distance * rateNumerator / 25`. **Multiply before dividing, as written in
  the ruling.** Nether is `200 + distance * 8 / 25`, **not** `distance * 0.32`.
- **Then clamped: `min(CAP, …)` with `DistanceGearScore.CAP = 500` (M20), in every dimension.** The
  order of the clamp and the rounding cannot change the result, because 500 is a whole number; the
  code does compute, round, clamp, and says so.
- **`CAP` is its own constant, not `GearScore.HARD_CAP`.** Both are 500 today and they are different
  quantities (a mob's ceiling, an item's ceiling); §2.1's divisor argument applies unchanged. A
  mutation that swaps one for the other is invisible to every test while they are equal, so the
  naming is the only protection — the same position `GearScore`'s own javadoc takes on its three
  100s.
- The result is `Math.round`ed (a seat default).
- **The whole grid is asserted from executed values, never predicted** (memory: *never predict
  floating point*).

**The distance is `hypot(dx, dz)` (M19: a circle), measured from an ORIGIN that paper chooses per
dimension and passes in:**
- overworld and Nether: `World#getSpawnLocation()`;
- **the End: (0, 0), always (M23)** — never the End's spawn point.

`DistanceGearScore` itself only ever sees the distance. The origin choice is one small paper function,
`MobOrigin.of(World)`, and it is the thing G7 exercises.

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
  `GearScore.HARD_CAP`). A mob's GS is also capped at 500 (M20), but it is a different quantity with
  a different source. **Sharing the key means any grep for one finds both.**
- **Read-side guard.** `/summon … BukkitValues` can write any integer. A stored value **outside
  1..500** is treated as absent and re-rolled from position, and the roll is logged at WARN. That is
  not a re-roll in M5's sense: nothing of ours ever stored it.
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
- `[gs]` is an `IntegerArgumentType.integer(1, 500)` (M20), written **inside the pre-spawn consumer**
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
anyone plays. After slice 1 alone, mobs have ×5 HP and ×1 melee.

**Environmental damage on mobs is FINAL after slice 1, not interim.** It is already proportional
(§1.3), so raising the custom max is all M13 and M14 need. That is why slice 1 carries G11 and G18,
and why slice 3 shrinks to heals plus the core pins on the damage proportion. Folding slice 3 into
slice 1 was considered and not taken: heals are a new handler arm with a new exhaustive policy, and
slice 1 is already the largest.

### Slice 1 — health, GS assignment and storage, the nameplate, the dev commands

**Core, tests first:**
- `MobScaling.maxHealth` in `MobScalingTest`: the full grid over {vanilla, custom} × {hostile,
  passive} × GS {100, 200, 300, 500}. It includes the M2 examples (zombie 20 → 500 at GS 500, Warden
  500 → 12,500) and the Knell (360 → 360 at GS 100).
- *(RETIRED by M25: this test was replaced by `DimensionGearScoreTest`, which pins the M25 rows. The
  list below is slice 1's record.)* `DistanceGearScore` in `DistanceGearScoreTest`:
  - each world at 0 and 2,500 blocks, plus `CUSTOM`→overworld;
  - **the cap (M20), on both sides of it, in every world**: overworld 9,975 / 10,000 / 12,000
    (→ 499 / 500 / **500, not 580**); End 4,975 / 5,000 / 6,000 (→ 499 / 500 / **500, not 540**); Nether 900 / 937.5 / 2,500
    (→ 488 / 500 / **500, not 1,000**). The expected values come from running the function, per the
    memory, and the "not" figures are what a missing clamp returns;
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
- `MobOrigin.of(World)`: the world's spawn location, **except the End, which is (0, 0) (M23)**.
- `onMobAppear` passes the `[GS] `-prefixed base name.
- `NameplateText` gets `of(OptionalInt gs, baseName, cur, max)` and `NameplateTextTest` rows. **The
  `[GS] ` prefix is `NamedTextColor.AQUA` (M22); the name, the numbers and the red heart are
  unchanged.** The test asserts the prefix's colour, the heart's colour, and that the name and
  numbers carry no colour of their own — so a colour that leaks from the prefix onto the name
  reddens.
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
- round → floor (reddened only by the half row);
- the clamp removed (reddened by the 12,000 and Nether 2,500 rows);
- `CAP` 500 → 400;
- the prefix colour AQUA → none.

**The End origin is paper-side and has no unit test**, so G7 is its only witness: an End mob near
(0, 0) must read [300]. Place it **away from the End's spawn point** too, or the row cannot tell
(0, 0) from the spawn point.

**The unreachable-arm warning** (*AND THE ARM THAT MOST EARNS ITS KEEP…*): the out-of-range stored GS
guard is reachable **only** through `/summon BukkitValues`. Its gate rows (G15, G15b) use exactly
that, or it is not witnessed.

**Gates:** R0, then G1–G7, G11, G12–G16, G18, G19.

### Slice 2 — damage, every path from §1.2

**First commit: the probe logger** (cause, direct entity, causing entity, vanilla amount, applied
amount, resolved GS). It is run against each §1.2 row, and **not merged**, the same way Recall's
tuning logger was a spike. Its output fills the ‡ cells in this plan's §1.2 table, in the PR.

**Core, tests first:**
- `MobScaling.attackDamage`, the same grid as `maxHealth`.
- `MobDamagePricing.fromEvent(vanillaAmount, gsOrAbsent)` = **`vanilla × 5 × gs/100`, FLAT (M16)**.
  It takes **no player max at all** — the parameter's absence is the rule. **Its paper-side gate
  must use a player max ≠ 100**, because at max 100 the flat price and the old share-of-max price are
  the same number (§4 G9's note).
- **Every mob-to-player hit is priced by M16**: melee through the seeded attack (`attackDamage`),
  everything else through `fromEvent`. Both are `vanilla × 5 × GS/100`. The only exceptions are the
  DoT ticks that carry no source (§6 F5).

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
- **No content change.** The Knell's damage is parked (M17, §6 F15); no `attack_damage` field.

**Mutations:**
- `attackDamage`'s vanilla factor → 1;
- the mob-sourced branch also applies `DamageScale` (it must redden: ×25);
- the resolution order reversed (a projectile whose shooter was re-rolled; a unit row on the resolver);
- the absent-GS arm returning 0.

**Gates:** R0, G8–G10, plus a probe table covering every §1.2 row that can be produced in-game.

### Slice 3 — proportional healing on mobs (M15), and the core pins on proportional damage (M13, M14)

**Core, tests first:**
- **`DamageScaleTest` rows pinning M13/M14 for scaled mobs** (the arithmetic already exists and has
  no row above GS 100 today): a GS 300 zombie (custom 300, attribute 20) → ×15; a GS 500 zombie →
  ×25, and a lethal-for-vanilla amount (20) → exactly its whole custom max (500); a Knell at GS 100
  (360 / 20) → ×18 and at GS 200 (720 / 20) → ×36.
- A new `MobHealPolicy`: an **exhaustive switch over `RegainReason` with no default arm**, matching
  `VanillaHealPolicy`'s house style. **REROUTE every reason** on a tracked mob, because nothing
  replaces a mob's regeneration the way `HealthRegenSystem` replaces a player's. That covers
  `ENDER_CRYSTAL`, `WITHER`, `WITHER_SPAWN`, `REGEN`, `MAGIC`, `MAGIC_REGEN`, `EATING` and `SATIATED`.
  `CUSTOM` is a **decided** REROUTE, following the damage side's *"rerouting honours it"*.

**Paper wiring:**
- `onRegainHealth` gains a tracked-mob arm: cancel, then
  `stats().heal(id, DamageScale.toCustom(amount, max, vanillaMaxAttribute, false), id, true)`.
  **Proportional (M15), with the same arithmetic as the damage side, so an End crystal heals the
  dragon the same fraction of its bar as in vanilla.** Custom mobs take the same path (M14's "no
  special case" applies to heals as well).
- **`DamageScale` gains its second call site, and its javadoc's "one call site by construction" must
  be rewritten in the same commit.** The rule it protects is *no vanilla-denominated DAMAGE is
  converted twice* (k²). A heal is a different direction and cannot be converted twice with a damage.
  *Alternative: a separately named heal function with the same body.* Rejected, because two copies of
  one proportion are two accounts of one rule.
- The environmental damage branch is **unchanged** (§1.3).
- Mob-sourced damage on a **mob** victim (a skeleton's arrow in a zombie) stays in the proportional
  bucket. See §6 F12.

**Mutations:**
- the heal arm prices ×1 (the raw vanilla amount);
- the heal arm prices ×5 flat (M8's withdrawn rule; the GS 300 heal row reddens: ×5 vs ×15);
- `REGEN` or `ENDER_CRYSTAL` moved to PASS;
- `DamageScale`'s mob denominator replaced by the custom max (every row reads ×1).

**Gates:** R0, G11 and G18 re-read as regressions, then G17, G20.

---

## §4 GATE ROWS

> **M25 VERDICTS ON THIS TABLE (2026-09-28).** These rows were written against the pre-M25 curves. The
> live gate is `GATE-mob-scaling-m25.md`, which carries the still-unread rows forward.
> - **RETIRED, superseded by M25:** **G1b**'s second spider (2,500 out → `[200]`), **G2** (2,500 out →
>   `[200]`), **G6** (Nether `[200]`/`[360]`/`[500]`) and **G19** (12,000 out → `[500]`). Under M25 they
>   read `[100]`, `[100]`, `[200]` at all three distances, and `[100]`. Each was read PASS-as-reported on
>   slice 1, and that record stands for the curve it was read against.
> - **REWRITTEN under M25:** **G7** (the End centre is still `[300]`; the second enderman reads
>   `300 + max(0, d - 1000) / 100`, which is 300 anywhere within 1,000 of (0, 0)); **G14** (the Nether's
>   "would roll" is now exactly 200, not "at least 200"); **G15**/**G15b** (a re-roll near overworld spawn
>   is exactly `[100]`, not `~[100]`).
> - **CARRIED, unaffected:** G5 (passive), G12, G13, G16 (a stored or inherited GS), G11, G18
>   (proportional environmental damage), and the damage rows G8–G10.

**Game mode is declared per row** (verification.md, *THE CREATIVE-DIVERGENCE REGISTER*). **Creative
removes the cost that every damage row measures**: a creative player takes no damage and is not
targeted. **Every row that reads damage TO A PLAYER is SURVIVAL.** Read-only nameplate rows may be
CREATIVE.

**R0 (every slice): the deployed build carries this slice.** The startup line names the head sha. If
R0 fails, STOP.

| row | slice | mode | setup | expected | what keeps it from being hollow |
|---|---|---|---|---|---|
| **G1** | 1 | creative | a natural or egg zombie within ~10 blocks of overworld spawn | `[100] Zombie 100/100 ❤`, **the `[100]` in AQUA (light blue); "Zombie" and the numbers in their usual colour; the heart still red** (M22) | the colour is read on the client, not in the log; a colour leaking from the prefix onto the name fails the row. **A zombie's displayed GS equals its displayed max at every GS** (20 × 5 × GS/100 = GS), so on its own this row cannot tell "max from GS" from "max printed as GS". G1b breaks that. |
| **G1b** | 1 | creative | a spider at spawn, and one placed 2,500 out | `[100] Spider 80/80 ❤`, then `[200] Spider 160/160 ❤` | spider base 16 ≠ 20; `/rpg mobinfo` shows the vanilla attribute as the witness for ×5 |
| **G2** | 1 | creative | a zombie **placed** (egg or `/summon`) at x=2500, z=0 | `[200] Zombie 200/200 ❤` | `mobinfo`'s "would roll" line also reads 200, and the stored line reads `source=rolled` |
| **G3** | 1 | creative | `/summon warden` at spawn | `[100] Warden 2500/2500 ❤`; **`mobinfo` shows vanilla `MAX_HEALTH` = the vanilla value, not 2500**, and there is no error in the log | "no attribute error" is satisfied for free (nothing writes the attribute); **the attribute READ is the witness for M9** |
| **G4** | 1 | creative | `/rpg spawn knell` at spawn | `[100] Knell 360/360 ❤`, not 1800 | the mutation "isCustom ignored" produces 1800 |
| **G5** | 1 | creative | a cow at spawn | `Cow 50/50 ❤`, no `[..]`; `mobinfo` stored GS = none | a cow at 2,500 out also reads 50/50 (it rules out "GS 100 happened to be ×1") |
| **G6** | 1 | creative | a mob at the Nether spawn; one placed 500 out; one placed 2,500 out | `[200]`, then `[360]`, then **`[500]` — capped (M20); uncapped it would read [1000]** | read the Nether `getSpawnLocation()` first (`mobinfo` prints it) and place relative to it. The 500-block mob proves the 8× rate below the cap, which the capped 2,500 row cannot |
| **G7** | 1 | creative | an End mob (an enderman) placed within a few blocks of **(0, 0)**, and a second one placed at the End's spawn point (`mobinfo` prints it) | the first reads **[300]** (M23). The second reads `300 + its distance from (0, 0) / 25`, as `mobinfo`'s "would roll" line computes it — **not [300] unless the spawn point happens to be (0, 0)** | if the End's spawn point IS (0, 0) the two mobs cannot tell the origins apart; then place the second 250 blocks out on the main island instead and expect [310] |
| **G8** | 2 | **survival** | an unarmoured player at 100/100; one zombie hit at GS 100 | the player loses **5 × `mobinfo`'s vanilla `ATTACK_DAMAGE`** | the expected number is printed by the server, not predicted |
| **G8b** | 2 | survival | the same, from `/rpg spawn zombie 300` | 15 × the attribute | GS ≠ 100: the multiplier is really exercised |
| **G9** | 2 | survival | a creeper blast and a skeleton arrow, from `/rpg spawn creeper 300` / `skeleton 300`; **the player at max 100, then at max 400** (`health_boost_TEMP`) | the probe log's `applied / vanilla` = **15 at both maxes** (M16, flat). The old share-of-max pricing would read 15, then 60; the stacking bug reads 75 | **At GS 100 and max 100, master already prints ×5 for both**, so the row must use GS ≠ 100 **and** max ≠ 100 or it passes on master. |
| **G10** | 2 | survival | a GS-300 skeleton shoots, then is `/kill`ed while the arrow flies | the arrow still prices at GS 300 (the stamp) | the "shooter gone" arm of the resolution order |
| **G11** | 1 (re-read in 3) | creative | a zombie at GS 300 falls, and the probe logs the FALL; then a GS 500 zombie dropped from a height that kills a vanilla zombie (vanilla amount ≥ 20 in the log) | `applied / vanilla` = **15** (M13: 300 / 20); **the GS 500 zombie dies from the fall** | at GS 100 proportional and M8's withdrawn flat ×5 are the same number, so GS 300 is required; the lethal drop is M13's own sentence as a row |
| **G12** | 1 | creative | a GS-200 zombie; walk out until its chunk unloads; return | still `[200]`; the log shows `REMOVE <uuid>` then `SEED <uuid> source=stored gs=200` | **without the log pair the row is hollow in TIME**: a mob that never unloaded, or re-rolled at the same spot, prints the same [200] |
| **G13** | 1 | creative | a `/rpg spawn zombie 437` zombie near spawn; stop and restart the server | still `[437]`, with `source=stored` | 437 is under the cap and **no position near spawn can roll it**, so a re-roll cannot fake it |
| **G14** | 1 | survival (the mob must move by itself) | a GS-140 zombie (`/rpg spawn zombie 140`) walks or is pushed through a Nether portal | still `[140]` in the Nether, where any arrival point rolls at least [200] (`mobinfo`'s "would roll" line); **its HP reads full**, which is F1 and expected | the would-roll line proves the two numbers differ |
| **G15** | 1 | creative | `/summon zombie ~ ~ ~ {BukkitValues:{"rpg:mob_gear_score":0}}` | re-rolled from position, with a WARN line | this is the **only** way to reach the below-1 arm |
| **G15b** | 1 | creative | the same with `"rpg:mob_gear_score":900` | re-rolled from position (not [900], not clamped to [500]), with a WARN line | the above-cap arm; near spawn a re-roll reads ~[100], which is unambiguous |
| **G16** | 1 | creative | a zombie villager conversion, and a slime split, from a GS-300 source | the children read [300] | the parent's GS is not the one their position would roll |
| **G17** | 3 | creative | a regeneration potion splashed on a damaged GS 300 zombie; the probe logs the heal | its plate rises (it never moved before slice 3), and `applied / vanilla` = **15** (M15) | the before-state is taken on master; GS 300 separates proportional (15) from flat (5) |
| **G18** | 1 (re-read in 3) | creative | a `/rpg spawn knell` Knell in lava, then a `/rpg spawn knell 200` Knell in lava | `applied / vanilla` = **18**, then **36** (M14: 360 / 20, then 720 / 20) | 18 is today's number too, so the GS 200 Knell is the row that shows its custom max entering the proportion |
| **G19** | 1 | creative | a zombie **placed** 12,000 blocks from overworld spawn | **`[500]`, not `[580]`** (M20); `mobinfo`'s "would roll" also reads 500 | 580 is exactly what the uncapped curve gives, so the two outcomes cannot be confused |
| **G20** | 3 | creative | in the End: the dragon at GS 300 (it sits at ~[300] by M23), damaged, healing from a crystal; the probe logs the heal | `applied / vanilla` = the dragon's custom max / its vanilla attribute (**15** at GS 300) | the crystal is M15's named case; "the crystals stay as strong as in vanilla" is this ratio |

---

## §5 OPEN QUESTIONS FOR BEN

**None open.** All eleven were answered on 2026-09-27 and are recorded verbatim in §0 as M13–M23:

| was | ruled as |
|---|---|
| Q1 environmental damage above GS 100 | M13 — proportional |
| Q2 environmental damage on a custom mob | M14 — proportional, no special case |
| Q3 heals on mobs | M15 — proportional |
| Q4 mob non-melee damage on a player | M16 — flat, `vanilla × 5 × GS/100` |
| Q5 the Knell's damage | M17 — parked (§6 F15) |
| Q6 custom mobs and distance GS | M18 — distance, until the M3 exceptions |
| Q7 the shape of distance | M19 — a circle |
| Q8 a cap | M20 — capped at 500 |
| Q9 the hostile line | M21 — `Enemy` plus the edge cases |
| Q10 the `[GS]` colour | M22 — AQUA |
| Q11 the End's origin | M23 — (0, 0), always |

The one thing the seat is asked to confirm on the diff, which is a reading of a ruling rather than a
new question, is in §6 F15.

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
  `applyDamage` pipeline*). Scaling does not reach it: an iron golem kills a GS 500 zombie in
  vanilla time, because the zombie's vanilla health is untouched (M9). It is a SUMMONER prerequisite,
  already recorded there.
- **F4. Mob melee is priced from the seed-time `ATTACK_DAMAGE` attribute.** It misses:
  - later equipment pickup;
  - vanilla's random ranges (the iron golem);
  - difficulty;
  - any contact damage that is not the attribute (the pufferfish candidate prices at 0).

  Scaling multiplies it faithfully.
- **F5. Damage-over-time ticks (POISON, WITHER) cannot carry a GS.** They name no causing entity, so
  M16's flat pricing cannot reach them. They stay on Route B's share-of-player-max pricing (×5 at max
  100), with no GS. A fix would scale at effect application.
- ~~**F6. Difficulty may split Route A from Route B.**~~ **CLOSED by M24 (2026-09-28).**
  - **Measured** with `javap` on the pinned `paper-26.1.2.jar`. `Player.hurtServer` scales the amount
    when `scalesWithDifficulty()`: EASY `min(a/2 + 1, a)`, HARD `a x 3/2`, and PEACEFUL returns before
    any event. It then calls `LivingEntity.hurtServer`, which raises the Bukkit event (`Avatar` does not
    override it). So Route B was post-difficulty and Route A (the attribute) was not.
  - **Two more amount channels**, found by sweeping every `getDifficulty` reference in
    `net.minecraft.world.entity`:
    - `Guardian$GuardianAttackGoal.tick`: the beam is 1, +2 on HARD, +2 for an elder;
    - `AbstractArrow.setBaseDamageFromMob`: `v x 2 + triangle(id x 0.11, 0.57425)`. Its only caller is
      `ProjectileUtil.getMobArrow`, which is called only from `AbstractSkeleton.getArrow` and
      `Illusioner.performRangedAttack`.
  - **Slice 2 moves all three to NORMAL** (`core/mob/VanillaDifficulty`), M24's reference, where each
    is untouched. The player's scaling is inverted on the raw event amount, before the damage window.
    The guardian's HARD bonus is subtracted after it. The arrow's base is shifted by
    `(id - 2) x 0.11` at launch, because the hit is `ceil(speed x base)` and a ceiling cannot be
    reversed at impact. (`0f8fd81` shifted by `id x 0.11`, which normalised arrows to PEACEFUL alone.
    The seat caught it in review.)
  - **What M24 cannot reach, recorded rather than guessed:**
    - PEACEFUL, where a scaled hit never lands at all;
    - how OFTEN a mob hits. The skeleton's shot passes the difficulty id to its inaccuracy
      (`AbstractSkeleton.performRangedAttack`, read). `CrossbowAttackMob.performCrossbowAttack`,
      `Drowned.performRangedAttack`, the breeze's `Shoot.tick`, `Illusioner.performRangedAttack` and
      `WitherBoss.customServerAiStep` also reference difficulty; **their formulas were not read**, and
      none of them sets a damage amount the sweep could see;
    - effect DURATIONS: the wither skull's wither (10 s normal, 40 s hard, read). `CaveSpider`, `Bee`
      and `Husk` reference difficulty in `doHurtTarget`, not read further; they apply effects. Effect
      ticks name no entity (F5);
    - equipment rolled from LOCAL difficulty (`Mob.enchantSpawnedEquipment`, raid buffs). For example,
      a Power bow's bonus reaches the arrow.
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
  k = 18") **becomes false in slice 1** (an untagged zombie is 100 / 20 → k = 5), and its "one call
  site by construction" claim **becomes false in slice 3**, which adds the mob heal site. Each is
  corrected in that slice's own commit. *(Slice 3's half: restated as the rule, "no vanilla-denominated
  DAMAGE is converted twice", naming both sites, in the slice 3 commit.)*
- **F11. `GearScore`'s javadoc section *"UNTIL MOBS SCALE, THE SERVER GETS EASIER"*** is the debt
  this plan pays. Revisiting `GearScore.MIN`'s rationale (*"the bound that makes an unscaled-mob world
  playable"*) is M10's balance pass, not this one.
- **F12. Mob-sourced damage on a MOB victim** (a skeleton's arrow in a zombie, a creeper's blast in a
  crowd) is priced as environmental: **proportional** to the victim's bar (M13), with no attacker
  GS. A decision for the mob→mob pass.
- **F13. A player's VANILLA bow on a mob is also priced as environmental** (PROJECTILE, Route B), so
  it is proportional: ×5 × GS/100 on a vanilla mob after slice 1. Weapon balance is M10's; the bucket
  is recorded so the balance pass finds it.
- ~~**F14. Integer range.**~~ **WITHDRAWN by M20.** It said an uncapped GS reaches ~1.2 million at the
  overworld border and ~9.6 million in the Nether, and still fits an `int`. With the cap at 500 there
  is no range question left.
- **F15. OPEN — the Knell's damage (M17, parked).** It hits with a vanilla wither skeleton's
  `ATTACK_DAMAGE` at ×1 (M4, no vanilla 5×), while an ordinary wither skeleton hits ×5 after slice 2.
  So the named boss hits a fifth as hard as its base mob. It keeps its current behaviour and slice 2
  adds no `attack_damage` field.

  **One reading for the seat to confirm on the diff:** slice 2 applies M4's GS/100 to the Knell's
  attack like any custom mob's, so a GS 100 Knell is unchanged and a GS 200 Knell hits twice as
  hard. If M17's *"keeps its current behaviour"* is meant to cover the GS factor as well, slice 2
  exempts custom mobs' attack from `MobScaling.attackDamage` entirely. At GS 100 the two readings are
  the same number, so no gate row near spawn can tell them apart.
- **F16. FIXED: a VANILLA `/rpg spawn` now runs vanilla's own spawn setup (`randomizeData = true`, the
  seat's F16c ruling), and the melee seed counts the held weapon (`MeleeSeed`). The `DefaultMainHand`
  table below is SUPERSEDED and deleted; it is kept here as the record of how the ruling was reached —
  `/rpg spawn` gave vanilla mobs NO default weapon.** Ben found it on
  2026-09-28: a spawned skeleton has no bow. On that boot each G9/G9b skeleton was armed by hand with
  `/item replace entity @e[type=skeleton,sort=nearest,limit=1] weapon.mainhand with bow`.
  - **Cause, read with `javap` from the pinned `paper-26.1.2.jar`.**
    `CraftRegionAccessor.addEntity(entity, reason, consumer, randomizeData)` runs
    `if (randomizeData && entity instanceof Mob) mob.finalizeSpawn(...)`, then the consumer, then
    `addEntityToWorld`. **The offsets:** `15: iload 4` / `17: ifeq 59` / `21: instanceof Mob`, then
    **`55: Mob.finalizeSpawn`**, **`80: Consumer.accept`**, and **`88: addEntityToWorld`**, which raises
    `EntityAddToWorldEvent` and so runs the seed. **Correction:** the seat's first F16c ruling assumed the
    consumer ran BEFORE `finalizeSpawn`. That was wrong, and the build stopped on it. The consumer runs
    AFTER vanilla's setup and BEFORE the add, so vanilla's setup cannot overwrite `mob_gear_score`,
    `mob_id` or the name, by order, and a given GS still seeds as `source=stored`. `RpgCommand`'s spawn
    used to pass `randomizeData = false`, chosen for determinism.
    So `finalizeSpawn` never runs, and with it `populateDefaultEquipmentSlots` and the piglin's
    `createSpawnWeapon`, where every default weapon is handed out.
  - **SUPERSEDED BY F16c (below), deleted with its tests. As first built:** the fix. Its RANGED half is built (`core/mob/DefaultMainHand`, applied in `RpgCommand`'s pre-spawn
    consumer, pinned by `DefaultMainHandTest` and `SpawnDefaultMainHandSignatureTest`): bow for skeleton,
    stray, bogged, parched and illusioner; crossbow for pillager. The MELEE rows joined after the
    seat's seed-timing ruling at the end of this entry, and the table is applied to VANILLA spawns
    only.** Keep
    `randomizeData = false`, so determinism stays, and add a FIXED per-type main-hand table, applied in
    the pre-spawn consumer. It covers **only the mobs whose vanilla weapon is GUARANTEED**, read from
    each override's bytecode:

    | mob | guaranteed main hand | where, and the condition |
    |---|---|---|
    | skeleton, stray, bogged, parched | `BOW` | `AbstractSkeleton.populateDefaultEquipmentSlots`, after `super`'s rolled armour. None of the four declares its own override (Parched checked by class-block bounds) |
    | wither skeleton | `STONE_SWORD` | `WitherSkeleton.populateDefaultEquipmentSlots`, unconditional |
    | pillager | `CROSSBOW` | `Pillager.populateDefaultEquipmentSlots`, unconditional |
    | vindicator | `IRON_AXE` | `Vindicator.populateDefaultEquipmentSlots`, only when `getCurrentRaid()` is null (`ifnonnull → return`). A `/rpg spawn` is never in a raid |
    | illusioner | `BOW` | `Illusioner.finalizeSpawn`, unconditional |
    | vex | `IRON_SWORD` | `Vex.populateDefaultEquipmentSlots`, unconditional |
    | piglin brute | `GOLDEN_AXE` | `PiglinBrute.populateDefaultEquipmentSlots`, unconditional |

    **Corrections to the seat's expected list:** the zombified piglin's golden sword is **ROLLED**, not
    guaranteed: `ZombifiedPiglin.populateDefaultEquipmentSlots` picks by `nextInt` between
    `GOLDEN_SPEAR` and `GOLDEN_SWORD`. So it stays OFF. The illusioner, vex and piglin brute are
    **added**. The other four the seat expected are confirmed.
  - **Rolled, so they stay OFF, and the table must not grow them:**
    - the drowned's `TRIDENT` or `FISHING_ROD` (`nextFloat`/`nextInt`);
    - a zombie or husk's `IRON_SWORD`, `IRON_SPEAR` or `IRON_SHOVEL` (a difficulty-weighted
      `nextFloat`);
    - the piglin's `CROSSBOW` versus a `GOLDEN_SPEAR`/`GOLDEN_SWORD` (`createSpawnWeapon`);
    - the zombified piglin's golden weapon;
    - `Mob`'s random armour;
    - the fox's mouth item.
  - **The rows a weapon-less spawn silently changes:**
    - slice 2: **G9 and G9b** (the skeleton's arrow half); **G10** (the arrow in flight); **G-DIFF**
      (skeleton arrows); **P-ARROW** (skeleton, stray, bogged, and the pillager's crossbow bolt).
      Unarmed, each mob walks up and punches instead: an `ENTITY_ATTACK` line that looks like a
      reading, and is not one for the arrow rows. **P-TRIDENT** was already "natural spawns only",
      because the trident is rolled;
    - slice 1: **none change their reading**. Every slice 1 row reads health or a nameplate, not a
      held item. But `/rpg spawn knell` takes the same path, so **the Knell spawns without its stone
      sword** (M17's parked damage).
    - ~~**Unmeasured:** whether a held weapon's modifier is in `ATTACK_DAMAGE` when the seed reads
      it.~~ **MEASURED 2026-09-28, and the answer is "it depends on who is near"** (read with `javap`
      from `paper-26.1.2.jar`):
      1. **A natural spawn equips BEFORE the add.** `NaturalSpawner.spawnCategoryForPosition` calls
         `Mob.finalizeSpawn` (offset 497) before `ServerLevel.addFreshEntityWithPassengers` (538). The
         chunk-generation path does the same (500 before 511).
      2. **The add pairs nearby players before our event.** `ServerLevel$EntityCallbacks.onTrackingStart`
         calls `ServerChunkCache.addEntity` (offset 204), then builds and calls `EntityAddToWorldEvent`
         (258–261). `ChunkMap.addEntity` runs `TrackedEntity.updatePlayers(level.players())`. For each
         player within `getEffectiveRange()`, `updatePlayer` calls `ServerEntity.addPairing`, which
         calls `sendPairingData` synchronously.
      3. **Pairing folds the held weapon into the attribute.** `sendPairingData` calls
         `LivingEntity.detectEquipmentUpdates`, which calls `collectEquipmentChanges`. That applies
         each equipped item's modifiers (`ItemStack.forEachModifier`). The only other callers are
         `LivingEntity.tick`, `ArmorStand.tick` and `Player.detectEquipmentUpdates`, all later than
         the add.
      4. **Our seed runs INLINE in that event** (`RpgListeners.onEntityAdd` → `onMobAppear` → `seed`,
         not deferred).

      **So `seedCombatStats` counts a held weapon's modifier IF AND ONLY IF a player was within
      tracking range when the mob was added.** A wither skeleton spawning near a player seeds with its
      stone sword's damage; the same mob spawning out of range seeds without it, and keeps the smaller
      number for life, since the seed is register-if-absent. **This is pre-existing and affects every
      natural spawn with a weapon** (an F4 relative). A `/rpg spawn` with the melee table would always
      pair, because the caller stands on it, so it would match only the in-range half of natural spawns.
      Bows and crossbows register no attack modifier (`Items`: `durability` only; the stone sword goes
      through `.sword(ToolMaterial.STONE, …)`), which is why the ranged half ships without this question.

      **RECOMMENDATION (for the seat to rule before the melee half ships):** make the seed independent
      of pairing, by **always** counting the main-hand weapon. That means seeding `ATTACK_DAMAGE` as vanilla
      itself settles it after the first tick, which is the attribute value vanilla melee then deals
      (M1's "5x vanilla damage" is 5x that hit). Concretely: base value plus the held item's
      default attack modifiers, computed at the seed, rather than trusting whatever pairing has or has
      not applied. The alternative, always EXCLUDING the weapon, is equally deterministic, but prices a
      sword-wielding mob below its own vanilla hit. Once the seed is deterministic, the four melee
      entries (wither skeleton, vindicator, vex, piglin brute) can join the table, and `/rpg spawn`
      matches a natural spawn at any distance.

      **RULED by the seat (2026-09-28), and BUILT:** the seed ALWAYS counts the held main-hand weapon,
      whatever the pairing.
      - **How:** `core/mob/MeleeSeed` = `ATTACK_DAMAGE`'s BASE + the weapon's `ADD_VALUE` modifiers.
      - **Where the modifiers are read:** the item's full component,
        `ItemStack.getData(DataComponentTypes.ATTRIBUTE_MODIFIERS)` (pinned paper-api:
        `public <T> T getData(DataComponentType$Valued<T>)`), filtered to `ATTACK_DAMAGE` entries whose
        `EquipmentSlotGroup.test(EquipmentSlot.HAND)` holds. It is `@ApiStatus.Experimental`, like the
        `EQUIPPABLE` read `EquipmentReads` already makes.
      - **Operations:** every vanilla weapon builder read uses `ADD_VALUE` for attack damage
        (`ToolMaterial.createSwordAttributes`, `createToolAttributes`, which the axes reach through
        `Properties.axe` → `tool`, the spear's `Properties.spear`, and `TridentItem.createAttributes`). So
        no stop was needed. A non-ADD modifier at runtime is left out and WARNed, not ordered by guess.
      - **The table:** the four melee entries joined it (ten in all, applied to VANILLA spawns only).
        The Knell keeps no weapon, so its parked damage (M17) does not move.

    - **Findings, recorded and not fixed (the seat's list, plus what the reads found):**
      - **A weapon PICKED UP or dropped after seeding does not re-price.** A mob is seeded once (M5).
      - **The rolled weapons are counted only if rolled:** the zombified piglin's golden sword or
        spear, the drowned's trident, a zombie's sword, spear or shovel. The count now happens at the
        seed, consistently, whoever is near.
      - **F16c (RESOLVED below, by running vanilla's setup): `finalizeSpawn` also sets the wither skeleton's attack BASE.**
        `WitherSkeleton.finalizeSpawn` calls `setBaseValue(4.0)` on `ATTACK_DAMAGE` after `super`. The
        registered base is the attribute's default, **2.0**
        (`DefaultAttributes`: `WITHER_SKELETON` → `AbstractSkeleton.createAttributes` →
        `Monster.createMonsterAttributes` adds `ATTACK_DAMAGE` with no value; the `RangedAttribute`
        default is 2.0). **So a `/rpg spawn wither_skeleton` seeds 2.0 + stone sword 4.0 = 6.0, while
        a natural one seeds 4.0 + 4.0 = 8.0.** That breaks "`/rpg spawn` matches a natural spawn". A
        sweep of every method in `net.minecraft.world.entity` that writes `ATTACK_DAMAGE` found this the
        only fixed `finalizeSpawn` write. The others are state setters (slime and phantom size, baby
        hoglin and zoglin, goat age, the killer-bunny variant), and all of them write the base, which
        `getBaseValue()` sees. **Proposed:** a fixed base for the wither skeleton (4.0), applied in the
        consumer beside the weapon. **Not built, because it was not ruled.** F16b is predicted for
        the build as it stands (6.0).
      - **G16's slime half cannot be produced by `/rpg spawn`.** Without `finalizeSpawn` a slime never
        rolls a size (`Slime.setSize` is reached from it), so `/rpg spawn slime 300` is not the "large
        one" G16 asks for. *(RESOLVED by F16c below.)*

  - **F16c, RULED AND BUILT (the seat, 2026-09-28): a VANILLA `/rpg spawn` runs vanilla's own spawn
    setup.** The table was re-implementing `finalizeSpawn` one finding at a time: the weapons, then the
    wither skeleton's 4.0 base, then slime size. That would have kept leaking.
    - **`randomizeData = (def == null)`:** TRUE for a vanilla mob, FALSE for a custom one. A custom
      mob's content shape is authoritative, and the Knell stays unarmed with its parked damage (M17).
      `SpawnRandomizeSignatureTest` pins the condition; inverting it is a killed mutation.
    - **`DefaultMainHand` and both its tests are DELETED. `MeleeSeed` is KEPT:** the pairing timing is
      independent of how the weapon got there.
    - **So the F16c base divergence and the G16 slime gap are gone.** A spawned wither skeleton gets
      `finalizeSpawn`'s 4.0 base and its stone sword, as a natural one does. A spawned slime rolls a
      size.
    - **ACCEPTED: a dev spawn of a vanilla mob is now as random as a natural one.** Baby, equipment
      (including the rolled weapons: a zombie's sword, spear or shovel, the drowned's trident),
      variant, size and jockeys. Two spawns of one type are no longer the same mob.
    - **Jockeys, as the seat accepted them.**
      - **A rider or mount is its own mob,** scored as any natural spawn is: its GS rolls from position
        (`source=rolled`; passive, none). The command's `[gs]` applies to the named mob only.
      - **The chicken jockey is fine.** `Zombie.finalizeSpawn` calls `addFreshEntity(chicken)` itself
        (offset 317), so the chicken is added, raises its own `EntityAddToWorldEvent`, and gets no GS,
        being passive. The zombie is the named mob.
      - **KNOWN DIVERGENCE, NOT FIXED (ruling (a)):** a `/rpg spawn` of a VANILLA **spider** (vanilla's
        jockey roll) or **strider** (its rider roll) can create a rider that is set riding but **never
        added to the world**. `Spider.finalizeSpawn` and `Strider.spawnJockey` create and `startRiding`
        it with no add, and `/rpg spawn`'s path (`addEntityToWorld` → `ServerLevel.addFreshEntity` →
        Moonrise `EntityLookup.addNewEntity(e, false)` → `addEntity(e, false, false)`) adds no passenger;
        Moonrise's only recursive adder, `addRecursivelySafe`, serves chunk loading. So the rider raises
        no `EntityAddToWorldEvent`, and has **no seed and no GS**. **Its in-game behaviour is
        unmeasured.** **Natural spawns are unaffected** (`NaturalSpawner` uses
        `addFreshEntityWithPassengers`). **The jockey rates are unread**, so no rate is stated here.
        **Workaround: re-spawn.** **No gate row depends on a spider or strider rider.** In
        `GATE-mob-scaling-2.md`, P-DOT uses a cave spider, which extends `Spider` and so can roll the
        jockey, but it reads only the spider's own poison ticks. No row reads a strider. In the slice 1
        gate, G1b reads a spider's own nameplate.
    - **Gate predictions that fixed an ABSOLUTE vanilla number** (every other prediction is a ratio
      against that mob's own vanilla field, so it survives the randomness):
      - **F16b** is re-predicted, since the base is now 4.0: vanilla 8.000, applied 40.000, ratio 5.000.
      - **G-DIFFb's** `raw=3/6` and `vanilla=4` stay. A shulker bullet is a fixed `4.0f`, and a
        shulker's `finalizeSpawn` rolls no damage.
      - **G8** stays a ratio. A zombie may now spawn as a baby or holding a rolled weapon; `vanilla` is
        then that zombie's own attribute plus its weapon, and the ratio is still 5.000.
      - **G16** is rewritten: re-spawn until the slime is size 4, read by console.
- **F17. G17's staging was hollow: a zombie takes no Regeneration.** Read from the pinned server jar
  (26.1.2): `LivingEntity.canBeAffected` refuses REGENERATION and POISON for
  `#ignores_poison_and_regen`, which is `#undead`, which holds `#zombies`. The §4 row's
  *"a regeneration potion splashed on a damaged GS 300 zombie"* therefore fires no regain event and
  logs nothing. **Re-staged in `GATE-mob-scaling-3.md` on a GS 300 SPIDER** (240 / 16, the same ratio
  15), with a `G17-UNDEAD` control row that predicts the zombie's refusal. The prediction (x15) is
  unchanged.
- **F18. A tokened mob's vanilla health never climbs back, so its vanilla heals never stop.** Every
  hit on a tracked mob tokens the vanilla damage to 0.01, and both heals slice 3 reads (Regeneration,
  and the End crystal every 10 ticks) fire only while vanilla health is below the vanilla max (read
  from the jar). The reroute CANCELS the vanilla heal, so vanilla health stays below max for good, and
  the heals keep firing after the custom store is full. Each is a capped no-op (`HealthState.heal`
  clamps at max), so nothing a player sees changes; under `/rpg mobtrace` they log `before = after =
  max`, and a crystal-linked dragon logs one line every 10 ticks for as long as a crystal stands.
  Dev-only. **A fix would let the vanilla heal through as well as rerouting it, which is a new
  `MobHealPolicy` action, a decision for the seat.**
- **F19. No log line reads a mob's ENVIRONMENTAL damage.** §4's G11 says *"the probe logs the FALL"*
  and reads `applied / vanilla`; no such probe exists (`MOBHIT` is mob-to-player only). G11 and G18
  therefore stay the OBSERVATION rows they were on slices 1 and M25, and the ratio is pinned in
  `DamageScaleTest` alone. **A `MOBENV` trace beside `MOBHEAL` would make both log-witnessed**; not
  built, because the slice 3 brief names only the heal line.
- **F20. PLAYER-FACING: a mob hurt ONLY by our own damage keeps FULL vanilla health, so it never
  regenerates, and an End crystal never heals a dragon hurt only that way.** F18's premise ("a hit
  lowers vanilla health") holds only for the paths that TOKEN a vanilla damage event. Read from source
  (2026-09-28, `eb1c8d34`) and the pinned server jar:

  | damage path onto a tracked mob | how it lands | vanilla health |
  |---|---|---|
  | player melee, weapon melee swings | `onPlayerMeleeAttack` sets the event to the 0.01 token | **lowered** |
  | the sweep | `onPlayerSweepAttack` tokens | **lowered** |
  | environmental: fall, lava, fire, drowning, … | `onEnvironmentalDamage` tokens | **lowered** |
  | mob-on-mob (F12), a player's VANILLA bow (F13) | the same environmental rider, tokened | **lowered** |
  | a scorched mob's vanilla FIRE_TICK | suppressed, but "still tokens" | **lowered** (incidental) |
  | a `VanillaDamagePolicy` PASS cause | vanilla applies it untouched | **lowered** |
  | **every ability and weapon effect**: `EffectSpec.Damage` and `WeaponDamage`, delivered by rays, projectile bodies (their `ProjectileHitEvent` is cancelled), `Burst`, `Area` and `ThrowEmbers` | `BukkitCombatant.applyDamage`: the custom store plus `playHurtAnimation`; **no vanilla event** | **FULL** |
  | the scorch burn tick (`EntityScorchSink`), Ignite's detonation | `applyDamage` | **FULL** |
  | thorns and a shield reflect onto a mob attacker | `applyDamage` | **FULL** |
  | `/rpg mobdamage` (dev) | `applyDamage`, "the same entry point abilities use" | **FULL** |

  Both slice 3 heals test `getHealth() < getMaxHealth()` first (`RegenerationMobEffect`,
  `EnderDragon.checkCrystals`), so they **never fire** on a mob whose only damage was the FULL rows. So a
  Ranger or Mage fighting the dragon with a ray or a projectile weapon **fights a dragon its crystals
  cannot heal**, while a melee player's first swing turns the crystals on (and F18 keeps them on).
  Likewise a regeneration effect, or a witch's own healing (it drinks only below its vanilla max), does
  nothing for a mob hurt only by abilities. **Not a regression**: before slice 3, no vanilla heal reached
  the store at all. **Witnessed in `GATE-mob-scaling-3.md` F20a (the dragon) and F20b (a spider).**
  The fix is a decision for the seat (e.g. token the vanilla health in `applyDamage` too, or have the
  heal arm drive the heal instead of vanilla's gate), and nothing is built.
  **Read 2026-09-28 on `8d7e0c90`: F20a and F20b PASS.** A dragon hurt only by `/rpg mobdamage` logged
  no crystal heal for 14 s, and a spider logged no Regeneration heal for 20 s. Each started healing
  right after one punch.
- **F21. Two carried staging traps, both found on the slice 3 boot.**
  - **G18 is HOLLOW:** the Knell is a `wither_skeleton`, which vanilla makes immune to lava, so the
    "Knell in lava" row (carried unread since slice 1) can never read M14. A fall would work; the
    arithmetic is pinned by `DamageScaleTest`'s Knell rows.
  - **G11's GS 500 zombie must read `max=500` in its MOBSEED.** Since F16c a `/rpg spawn` runs vanilla
    spawn setup, which can roll a LEADER zombie with a raised MAX_HEALTH (seen: `max=1240`, vanilla
    49.6). That zombie survives the drop, correctly and as in vanilla, so it cannot read "both die".
- **F22. DEFERRED (M27): a perched dragon is never knocked airborne by damage.** Found by §7's read
  of the pinned jar:
  - `EnderDragon.hurt(…, EnderDragonPart, …)` adds the health drop across `reallyHurt` to
    `sittingDamageReceived` while a sitting phase is active, and takes off past `0.25 × getMaxHealth()`.
  - Our riders token every hit to 0.01, so the drop is 0.01 per hit, and a perched dragon never
    reaches the threshold from damage.
  - The F20 mirror writes vanilla health OUTSIDE `hurt()`, so it does not change this.

  **Deferred to the Ender Dragon boss fight work** (Ben, 2026-09-28: "we'll come back to this later
  when we do our Ender Dragon boss fight"). It is not in the F20 slice. §7.9 cross-references it.
- **F23. Staging traps met on the F20 boot (2026-09-28, `002352cb`). All are gate hygiene; none is a code
  defect.**
  - **A `/rpg spawn` mob is not persistent**, so vanilla's random despawn can remove it mid-row. The F20b spider
    vanished 7 s after its last heal (a `MOBREMOVE` with no command and no death). A row that reads "still alive
    later" must first make its mob persistent (console `data merge entity <uuid> {PersistenceRequired:1b}`), or
    a despawn reads exactly like a death.
  - **Two summoned dragons damage each other.** A dragon's head hurts what it touches, which spoils a "hurt only
    by X" row. Summon one.
  - **A `/rpg mobdamage` with no re-seed line in its own second has missed** (the ray found no mob). Four missed
    this boot. Read the re-seed line before reading the Health.
  - **Ungated vanilla heal sources log capped lines at full, and that is correct:** the Wither's REGEN, the iron
    golem's repair, and a horse's regen. F18's "no capped line" applies only to the GATED sources (Regeneration and
    the crystal). `GATE-mob-scaling-f20.md`'s M26-WITHER text said otherwise, and its readings record the miss.

---

## §7 F20 — THE VANILLA MIRROR (phase 1: the plan; no code)

**The recommendation is the seat's MIRROR, CONFIRMED by the pinned jar, with one widening**: it
mirrors every store write, raises as well as lowers, and runs from one listener rather than from each
path. Every jar claim below was read on 2026-09-28 from `run/versions/26.1.2/paper-26.1.2.jar` with
`javap -c`, and the source claims were read at `6dd1eeb9`.

### 7.1 THE RULE

After every write to a TRACKED MOB's custom store, set its vanilla health to

```
vanilla = clamp( newCurrent / customMax × vanillaMax ,  floor ,  vanillaMax )     floor = min(vanillaMax, 1.0)
```

where `vanillaMax` is the entity's own MAX_HEALTH attribute value. There are four conditions:
- **The attribute is never written (M9 stands).** Only `setHealth` is called.
- **Death stays ours.** When `newCurrent <= 0` (`reachedZero`), the mirror writes NOTHING, and
  `MobDeathSystem`'s `setHealth(0)` is the only thing that kills.
- **Floored above 0 while the store is above 0**, so vanilla never kills a mob that our store says is
  alive. The floor is the existing `VANILLA_LIVE_FLOOR` (1.0, today in `RpgListeners`), moved to
  the one place both use.
- **Players are excluded** (`targetIsPlayer`). `HeartBarRenderer` owns their bar.

### 7.2 THE ONE PLACE: THE `HealthChange` SEAM, AS A FIFTH `HealthListener`

The hook is neither the lowering call nor its callers. **It is the store's own notification, the
`HealthChange` seam.** `RpgPlugin` builds `new CombatantStats(new CompositeHealthListener(healthSystem,
nameplates, popups, mobDeath))`; the mirror is a fifth listener, `MobVanillaMirror`, in the same list.

**Why this is one place and not each path:** `CombatantStats` is the only writer of current health.
It has exactly three writers, and each emits exactly one `HealthChange`:
- `damage` emits `DAMAGE`;
- `heal` emits `HEAL`;
- `reconcileMaxModifiers` emits `MAX_CHANGE`.

So every path in §6 F20's table reaches the mirror without being named: `applyDamage` (abilities,
weapons, scorch, Ignite, reflects, `/rpg mobdamage`), the tokened riders, and every heal. **A NEW
damage path added next year is mirrored without anyone remembering to.** Hooking the callers instead
would be about seven sites, and the eighth, added later, would silently leave vanilla full. That is F20
again, reintroduced by omission. Hooking inside `CombatantStats` itself is impossible: `core` cannot
touch Bukkit. The listener IS the port.

**Threading:** `HealthListener.onChange` runs on the TARGET's owning thread. `MobDeathSystem`'s javadoc
states that contract and already relies on it to call `setHealth(0)`, so the mirror adds no new thread
exposure and makes no scheduler hop.

**The seed is the one write the seam does not carry, and it is named rather than missed.**
`bootstrapIfAbsent` creates a mob's store at FULL with no event, while vanilla health comes from the
entity's NBT and can be anything: F1 resets the store on every load, not the vanilla health. So
`MobNameplateManager.seedCombatStats` calls the SAME mirror function once after the bootstrap, and
vanilla goes to `vanillaMax`. **That makes one function with two callers: the seam for every write,
and the seed for creation.**

### 7.3 `LivingEntity.setHealth` ON THE PINNED JAR: THE FOUR READS

`CraftLivingEntity.setHealth(double)`, in full:
1. It rounds to float first (`d2f f2d`).
2. It checks `0 <= value <= getMaxHealth()`, or throws `IllegalArgumentException("Health value (%s) must
   be between 0 and %s…")`.
3. If `generation && value == 0` it discards the entity.
4. It calls `LivingEntity.setHealth(float)`.
5. If `value == 0` it calls `die(damageSources().generic())`.

`LivingEntity.setHealth(float)` for a non-player: `entityData.set(DATA_HEALTH_ID, Mth.clamp(v, 0,
getMaxHealth()))`. No class under `net.minecraft.world.entity` overrides `setHealth(float)`: a scan of
all 1,133 classes found only `LivingEntity`, which is the scan's control.

| question | answer, from the bytecode |
|---|---|
| **Does it fire an `EntityDamageEvent` or `EntityRegainHealthEvent`?** | **NO.** The only calls are the precondition, the synced-data write and, at exactly 0, `die`. **No recursion into our listeners** is possible, except `EntityDeathEvent` at 0, which the mirror never writes |
| **Does it play a hurt animation or sound, or reset the invulnerability ticks?** | **NO.** `invulnerableTime`, `hurtTime` and every sound call are absent from both methods |
| **Does it trigger death at `<= 0`, and what is the smallest safe value?** | **At EXACTLY 0 it dies immediately** (Craft's `die(generic)`). A negative value throws. **Any value that stays positive AFTER the float rounding is alive**; a double small enough to round to `0.0f` (below about half the smallest positive float) DIES. The floor of 1.0 is far from that edge, and is the value the token floor already writes today |
| **Does it change the dragon's phase or its crystal targeting?** | **NO.** `EnderDragon` has no `setHealth`. Its phase logic reads health only INSIDE `hurt(…, EnderDragonPart, …)`: health at or below 0 there means `setHealth(1.0f)` plus the DYING phase, and a perched dragon counts the drop across `reallyHurt` into `sittingDamageReceived` (takeoff past `0.25 × max`). A `setHealth` from outside `hurt` touches neither. **A crystal heals when all three hold** (`checkCrystals`): `nearestCrystal != null` (any `EndCrystal` within the bounding box inflated by 32, re-searched on a 1-in-10 roll each tick), `tickCount % 10 == 0`, and **`getHealth() < getMaxHealth()`**. The mirror makes that last test true exactly when our store is below max |

**The upper bound cannot throw.** `x = fraction × vanillaMax` with `fraction <= 1`, and Craft compares
the float-ROUNDED value against `getMaxHealth()`, itself the float max. Rounding is monotone, so
`x <= vanillaMax` rounds to at most the float max: measured on the leader zombie's attribute, 49.6
becomes `49.599998`. The core function still clamps, because a throw on the entity thread is the
failure to rule out, not reason about.

### 7.4 WHAT THE MIRROR TURNS ON: EVERY VANILLA READER OF A MOB'S HEALTH

A scan of the same 1,133 classes found 52 methods that call `getHealth()` or `getMaxHealth()`. For a
tracked mob, today each of them reads a vanilla health pinned at or near full by 0.01 tokens. The mob
side:

| class of reader | members (from the scan) | today | with the mirror |
|---|---|---|---|
| **gated heals, which are F20 itself** | `RegenerationMobEffect`, `EnderDragon.checkCrystals`, `AbstractHorse`/`Camel`/`Llama.handleEating`, `Wolf`/`Cat`/`AbstractNautilus`/`IronGolem.mobInteract` (feeding, iron repair), `Witch.aiStep` (drinks below max), `HappyGhast.continuousHeal` | fire only after a token hit, then never stop (F18) | fire exactly while the store is below max |
| **health shown to players** | `EnderDragonFight.updateDragon` and `WitherBoss.customServerAiStep` (boss bars, `setProgress(health / max)`), `Raid.getHealthOfLivingRaiders` (the raid bar), `IronGolem.getCrackiness`, `Wolf.getTailAngle`/`getAmbientSound` | **the dragon's and the Wither's boss bars sit at about full for the whole fight** | show the custom fraction |
| **behaviour keyed on health** | `WitherBoss.isPowered` (`health <= max / 2`: the armour phase that deflects arrows), `Axolotl.hurtServer` (plays dead), `Mob.getMaxFallDistance` (pathfinding) | **the Wither never enters its half-health phase** | vanilla's behaviour returns, keyed on the custom fraction |
| **writers we don't own** | `WitherBoss.makeInvulnerable` (the spawn charge-up), `Slime.setSize`, `Zombie.handleAttributes` (a leader) | unchanged | unchanged, and overwritten at the next store write |

**Most of this is a PLAYER-FACING change beyond F20**, which is §7.9's question for Ben.

### 7.5 F18 IS FIXED BY THE SAME LISTENER

F18 is "vanilla health never recovers, so the heals never stop." A `HEAL` change writes vanilla UP, so
**when the store reaches max, vanilla reaches `vanillaMax` exactly and both gated heals stop.**
`HealthState.heal` clamps to `max.value()`, the same double the fraction divides by, so the fraction is
exactly 1.0 there. The capped `before = after = max` lines disappear. This is why the mirror must fire
on every kind, not only on lowering writes: **a lowering-only mirror fixes F20 and leaves F18.**

### 7.6 A HEAL THROUGH OUR OWN PATH

- **Content `heal:` effects on a mob, `/rpg mobheal`**: `CombatantStats.heal` emits `HEAL`, and the mirror
  writes vanilla up.
- **A rerouted vanilla heal (slice 3):** `healTrackedMob` cancels the event, then calls `stats.heal`. The
  mirror's `setHealth` runs INSIDE the regain event, and vanilla's own post-event write is skipped
  because the event is cancelled (`checkCrystals`: `if (callEvent()) setHealth(getHealth() +
  getAmount())`). **Net effect: vanilla health rises by exactly the vanilla amount**, since the store
  rose by `amount × customMax / vanillaMax`. So vanilla moves as if vanilla had healed it, and nothing
  diverges.
- **The tokened riders** still subtract their 0.01 inside vanilla's `actuallyHurt`, and the store write
  lands a scheduler hop later (`applyDamage` runs `onEntity`), where the mirror overwrites the token.
  **The token floor stays**: it covers the window before that hop.

### 7.7 THE ALTERNATIVE, AND WHY IT IS REJECTED: OUR OWN REGEN AND CRYSTAL TICKING

The alternative: leave vanilla health alone and replace the gated vanilla heals with our own (a
Regeneration tick, a crystal-proximity tick, and so on). **Rejected, and the scan above is the
reason:**
1. **The gated set is not two sources, it is at least nine** (§7.4's first row), and every future
   Mojang heal is a new silent gap. That is the same "each path" failure the seam avoids.
2. **It fixes none of the display and behaviour readers.** The boss bars, the Wither's half-health phase
   and golem cracks read vanilla health, not heals. They would stay wrong.
3. **It duplicates vanilla logic that the mirror lets vanilla run itself**, at vanilla's own cadence,
   with vanilla's own conditions.

It would be preferable only if `setHealth` had side effects, and §7.3 measured none.

### 7.8 TESTS FIRST, MUTATIONS, GATE ROWS

**Core (tests first), a pure function** `MobVanillaMirror.healthFor(newCurrent, customMax, vanillaMax)`
returning an empty `OptionalDouble` for "write nothing". *(`OptionalDouble` is a box that holds one
double or nothing.)* The rows:
- full returns exactly `vanillaMax`, half returns half, and a tiny fraction returns the floor;
- `newCurrent <= 0` returns EMPTY (death is ours);
- `customMax` or `vanillaMax` that is `<= 0` or NaN returns EMPTY (fail soft: leave vanilla alone);
- a `vanillaMax` below 1 floors at `vanillaMax`;
- the result is never above `vanillaMax`, over a grid.

A second pure decision, `shouldMirror(HealthChange)`: `DAMAGE`, `HEAL` and `MAX_CHANGE` mirror, a
player never mirrors, and `reachedZero` never mirrors.

**Paper:**
- `MobVanillaMirror implements HealthListener`, registered in the composite.
- The seed calls the function once.
- A signature test pins both registrations. Without it, deleting either leaves every unit test green.

**Mutations owed**, each with what reddens:
1. writing the floor when `newCurrent <= 0`, which revives a mob at death → the core EMPTY row;
2. deleting the floor → the tiny-fraction row;
3. `vanillaMax` replaced by `customMax`, which would throw on the entity thread → the never-above-max
   grid;
4. `HEAL` excluded (lowering-only, which leaves F18) → the `shouldMirror` row;
5. the player exclusion dropped → the `shouldMirror` row;
6. the listener removed from the composite, or the seed call removed → the signature test.

**Gate rows**, to be predicted in the slice's gate file before its boot. The values below were computed
by executing the expression in Java, not predicted:
- **F20b′: RE-PREDICTED AS PASS WITH HEALS.**
  - A GS 300 spider hurt only by `/rpg mobdamage 100`: console `Health` is `9.333333f` (140 / 240 × 16).
  - Under Regeneration II it **heals without any punch**: `MOBHEAL … before=140.000 after=155.000`,
    and the next Health read is `10.333333f`.
  - At 240 the Health is `16.0f` and the heal lines STOP (F18 fixed): zero `before = after = max`
    lines.
- **F20a′: RE-PREDICTED AS PASS WITH HEALS.**
  - A dragon hurt only by `/rpg mobdamage 500`: `Health` is `166.66667f`, and the crystal heals it
    **without a punch**, 2500 → 3000 in 34 lines.
  - It ends at `Health 200.0f`, with no capped line.
  - Its boss bar visibly drops, then refills (an observation).
- **NO-RECURSION:**
  - `/rpg mobdamage 100` on a full, untouched GS 300 spider with no heal source nearby.
  - The Health moves to `9.333333f` with **zero MOBHEAL and zero MOBHIT lines**, and no exception or
    `StackOverflowError`.
  - The mirror's write raised no event.
- **NEVER KILLED BY THE MIRROR:**
  - `/rpg mobdamage 239` on a GS 300 spider leaves the store at 1 and `Health` at **`1.0f`** (the floor,
    not 0.0667), and the spider is **alive**.
  - ~~A punch (the 0.01 token) leaves it alive.~~ **Withdrawn while writing the gate (2026-09-28):** a
    punch also deals the player's custom damage, at least the 1 HP left, so it kills through our store.
    That is death being ours, and it tests nothing about the floor. The gate reads "still alive 10
    seconds later" instead.
  - `/rpg mobdamage 1` then kills it ONCE, with one death and its drops (death is ours).
- **Regressions:** G17 and G20 read as before (ratio 15.000).
- **Carried:** every row unread on `GATE-mob-scaling-3.md`.

### 7.9 FOR BEN — ANSWERED 2026-09-28

**Both questions below are answered, and are kept as asked.** Question 1: **yes, all of it (M26).**
Question 2: **deferred to the Ender Dragon boss fight work (M27), recorded as §6 F22.** The seat
approved §7 as written, and it is built on the same branch.

1. **Do you want vanilla's health-keyed behaviour back for scaled mobs?** The mirror returns it, all at
   once, keyed on the custom fraction:
   - the Wither's half-health armour phase (arrows deflect), which today never happens;
   - the dragon's and the Wither's boss bars moving, which today sit at about full;
   - iron golem cracks;
   - witches drinking;
   - pets and golems healable by feeding and iron.

   **Recommended: yes**, since that is how vanilla plays at 5×. The alternative is a mirror that
   excludes chosen readers, which cannot be done: they all read the same number.
2. **A neighbour found by this read, NOT fixed by the mirror:** a dragon perched on the podium takes off
   after taking 25% of its max in damage, counted as the health drop inside `hurt()`. Our tokens make
   that drop 0.01 per hit, so **a perched dragon is never knocked into the air by damage today**. The
   mirror writes outside `hurt()`, so it does not change this. Fix it in this slice, or record it as its
   own finding?

---

## REPORT CHECKLIST (for the seat)

- The PR is opened against `master` and is **not merged**. #161 is untouched.
- No code, no boot. The only file is this one.
