# PLAN — THE FIVE DRAGON-FIGHT MOBS FROM cfde822 (SURVEY)

**Phase 1: SURVEY ONLY. No Java, no content, no boot, nothing ruled.** It covers the five mobs Ben picked on
2026-09-29 (`PLAN-legacy-port.md` RULINGS): **the Knell, Skreel (with its spider mount), Phalanx, Phage (with the
Lesser Phage it splits into) and Goldrot Goon.** For each it records what cfde822 did, what our mob schema would
need to express it, and a proposed build order. Then it states the design constraints for the later dragon fight.
**Ben rules every number and every order.**

**Read against:**
- master **`cd790dcc`** (#176, the docs tidy);
- the old project `CreaperCrusher/Butterfly-SMP` at **`cfde822`** (`cfde8222ec2ac35661963776e9d8f0582bc4e12a`),
  package `org.example1.butterflysmp`. It is not a local clone; every old file below was fetched with
  `gh api "repos/CreaperCrusher/Butterfly-SMP/contents/<path>?ref=cfde8222ec2ac35661963776e9d8f0582bc4e12a"`.

**What this builds on, not redoes:** `PLAN-legacy-port.md` §2.5 (Knell Beam), §3 (the five mobs, the spawn table,
the Knell OLD vs NEW), §4 (today's schema), §6 (findings F8, F10, F13, F14, F15) and §9.1/§9.3 (the capability ids
**MB1–MB5** and **E5**, reused here unchanged so the two files name one thing one way). Every fact carried over from it
was re-read at cfde822. **Where the two disagree, §2 says so.** `PLAN-mob-scaling.md` supplies M4, M17, M21, M24,
M25, M27 and §6 F15 / F22.

**Citations** name a CLASS and a METHOD or a SECTION, never a line, and that includes the old repo. The short paths
are `PLAN-build-system.md`'s: `P/` = `paper/src/main/java/io/github/butterflysmp/rpg/paper/`, `C/` =
`core/src/main/java/io/github/butterflysmp/rpg/core/`, `content/` = `paper/src/main/resources/content/`.

**HOW TO READ A NUMBER IN THIS FILE:**
- A number labelled **cfde822** is a FACT about what the old code did. It is recorded, **never adopted**.
- Every number a port would author reads **`___ (Ben)`**.
- **Old mob HP was absolute**, set by `MobSpawnHelpers.applyVanillaMaxHp` onto the vanilla `MAX_HEALTH` attribute.
  There was no gear score. Ours is a custom store × GS/100 (M4), so no cfde822 HP transfers as-is.
- **Old mob timers in these classes are TICKS, not milliseconds.** Every timer below came from a `runTaskLater` /
  `runTaskTimer` argument or a tick counter, so nothing needs the 50 ms/tick conversion. Seconds are shown beside
  ticks for reading only (20 t = 1 s). *(The ms conversion in `PLAN-legacy-port.md` applies to weapon cooldowns.)*
- **Old mob damage came in two kinds.** A vanilla melee or arrow hit on a player was ×5
  (`CombatListener.onMobDamageMultiplier`, `Formulas.MOB_DAMAGE_MULTIPLIER = 5.0`) times a mob-level factor. A
  scripted literal (the beam, blast, swirl, leap) was **exempt** from the ×5 (`CombatListener.runMobDamageMultiplierExempt`).
  These five carry no `MOB_LEVEL`, so their level factor is 1 (*inferred*: `MobLootListener.onHostileMobDeathCoins`'s
  comment says BOSS_NAME mobs never get a level; `Formulas.mobLevelDamageFactor` was not read). **Item-level scaling
  (`GearScore.scalePower`) is a weapon fact and does not apply to any mob number here.**

**READ vs INFERRED.** Every claim is marked:
- *read*: I opened the method at the stated ref;
- *inferred*: reasoned from code around it, not opened or not run;
- *UNVERIFIED*: a Paper or vanilla behaviour not read from the pinned jar or the server jar.

---

## 1. WHAT I READ

**At cfde822, in full** (13 files fetched to a scratch directory and read; the list is the `fetch` arguments):
- `encounter/withered/`: `WitheredMobFactory`, `WitheredKnellBehavior`, `WitheredKnightSystem` (a facade),
  `WitheredKnightUtil`, `WitheredMinionDeathListener`, `WitheredPhalanxBehavior`, `WitheredSpiderJockeyBehavior`;
- `encounter/dragonfight/`: `DragonfightManager`, `DragonfightReinforcements`, `DragonfightFactionListener`,
  `DragonfightCombatListener`;
- `mob/MobSpawnHelpers`, `mob/MobLootListener`.

**At cfde822, in part:** `CombatListener.onMobDamageMultiplier` / `runMobDamageMultiplierExempt`;
`Formulas.MOB_DAMAGE_MULTIPLIER` (the constant only); `PlayerInteracts.getNearestPlayer`;
`RangedListener.resolveBounceAwareKiller`; `CatalogueRegistry`'s mob list (`MobEntry` rows); the
`spawnWitheredKnight` call in `WitheredCommanderEncounter` (grep only).

**Found by** `gh api "repos/CreaperCrusher/Butterfly-SMP/git/trees/cfde8222ec2ac35661963776e9d8f0582bc4e12a?recursive=1"
--jq '.tree[].path'` (323 paths) filtered with `grep -i -E 'dragon|knell|skreel|phalanx|phage|goldrot|goon|mob|wither|spider|minion|faction'`.
**No class is named after any of the five mobs.** They live under their pre-rename names: Phage = `Grunt`, Goldrot
Goon = `Rouge`, Phalanx = `Archer`, Knell = `Knight`, Skreel = `SpiderJockey` (*read*: `WitheredMobFactory`'s
`spawnWithered*` methods and the PDC `BOSS_NAME` strings they set).

**In this repo, at `cd790dcc`:** `content/mobs/knell.yml` (the only mob file; `ls content/mobs/`);
`C/mob/MobDefinition`; `P/content/MobLoader`; `P/command/RpgCommand.spawnMob`; `P/content/ContentValidator.validateMobs`;
`C/combat/ProjectileFlight.nearestMob`; `C/combat/Caster`'s record header; `C/mob/MobDamagePricing`'s class javadoc;
`AbilityService.cast`'s signature; `storage/.../PlayerRepository` (that it exists). **Not read:** the
`Scheduler` interface itself (only its `onEntity` call sites in `P/listener/RpgListeners`).

---

## 2. WHERE cfde822 DISAGREES WITH `PLAN-legacy-port.md`, OR ADDS TO IT

None of these changes a tier in that file. Each changes what a port would have to reproduce.

- **D1 — A WAVE IS CAPPED BY THE NEXT PHASE'S ROW.** `DragonfightManager.onDragonfightDamage` writes the new phase
  index into its phase map and only then schedules `spawnPhaseWave(world, phaseToRun)` for the next tick (*read*: the
  order of the two statements). Every `spawnWithered*` reads its cap through `currentDragonPhaseInWorld`, which reads
  that map. So **wave 0 is capped by row 1, wave 1 by row 2, wave 2 by row 3** (*inferred* from the order; not run).
  The legacy table's cap column is correct as a table; which row a wave meets was not stated. Every wave count fits
  its row on its own, so a wave is clipped only by minions already alive from the trickle (*inferred*).
- **D2 — THE GOON'S BLAST PROBABLY SPARES OTHER MINIONS.** The legacy file says the blast hits *"other minions and
  players alike."* The source's own javadoc on `WitheredMinionDeathListener.onGoldrotGoonDeathExplode` says the
  opposite: friendly fire between minions is cancelled, *"so the explosion can't chain through other Goons / Phages
  / Archers."* The blast calls `le.damage(50.0, ws)` with the dead Goon as the source (*read*), and
  `DragonfightFactionListener.onDragonfightFriendlyFire` cancels an `EntityDamageByEntityEvent` whose damager and
  victim both carry `DRAGONFIGHT_MINION` (*read*). **Whether `LivingEntity.damage(double, Entity)` raises that event is
  *UNVERIFIED*.** If it does, the blast hits players, Endermen and any untagged mob, and no minion. The dragon is
  skipped by name either way (*read*).
- **D3 — THERE WAS A FACTION WAR WITH ENDERMEN.** The legacy file records only *"no friendly fire between them."*
  `DragonfightFactionListener.tickDragonfightFactions` also ran every 20 t while a fight was live (*read*):
  - each registered minion with no target, a dead target or a friendly target was pointed at the **nearest Enderman
    within 30 blocks**;
  - each Enderman with no live target was pointed at the **nearest minion within 30 blocks**;
  - `onEndermanSpawnDuringDragonfight` cancelled **NATURAL** Enderman spawns in the End inside |x| ≤ 200 and |z| ≤ 200
    while any fight was active.
- **D4 — THE PHALANX'S BLINK TRIGGER IS A BOX, NOT A RADIUS.** `schedulePhalanxBehavior` queries
  `getNearbyEntities(loc, 5.0, 5.0, 5.0)`, which takes half-extents (*UNVERIFIED*: the Bukkit signature's meaning), so a
  player anywhere in a 10 × 10 × 10 box triggers it. Creative and spectator players are skipped (*read*).
- **D5 — THE PHALANX'S SHIELD EATS THE FIRST DAMAGE OF ANY KIND.** `onPhalanxShieldAbsorb` listens on
  `EntityDamageEvent`, not the by-entity event (*read*). So a fall, fire, or a blast spends it as readily as a
  player's hit. The legacy file says *"the first hit it takes."*
- **D6 — THE KNELL'S DODGE ALSO CATCHES HITSCAN.** `onWitheredKnightDodgeProjectile` fires on a `Projectile` damager
  **or** on any by-entity hit whose damage type is `ARROW` (the Boltor-style hitscan) (*read*). It always cancels the
  damage. If both sidestep landings are blocked it **cancels and does not move** (`teleportKnightSideways` returns
  early; *read*). So a Knell backed against a wall is immune to arrows and moves nowhere.
- **D7 — THE SKREEL DOES NOT CIRCLE.** `circleAroundPlayer`'s own javadoc says it returns a point **along the
  player-to-spider axis** at 12 blocks, *"so the spider doesn't teleport-circle"* (*read*). The legacy file says
  *"Retreat circles to a stand-off band."* Also *read*: the loop starts **20 t after spawn**; the wind-up aborts if the
  player leaves **6.4–18 blocks** (0.8 × 8 and 1.5 × 12); the landing hit's damager is **the rider, not the spider**.
- **D8 — cfde822 CONTRADICTS ITSELF ON THE SPIDER'S SURVIVOR SPEED.** The class javadoc of
  `scheduleWitheredSpiderJockeyBehavior` says the survivor gets *"default AI + speed buff"*. `convertSpiderToBrawler`
  **divides** the speed by 1.3, removing the paired buff (*read*). The code governs what it did. Neither governs a port.
- **D9 — `applyDragonfightFollowRange` IS ON `DragonfightManager`**, not on `MobSpawnHelpers` (*read*: both classes).
  It sets `FOLLOW_RANGE` base to `DRAGONFIGHT_WITHER_FOLLOW_RANGE` = 64 on every minion, the spider included.
- **D10 — COINS WERE PAID EVEN THOUGH DROPS WERE STRIPPED.** `DragonfightManager.onBossEntityDeath` clears drops and
  XP on a tagged minion (*read*). `MobLootListener.onHostileMobDeathCoins` pays coins with `plugin.addTreasure`, a
  direct credit rather than a drop (*inferred*: `addTreasure` not opened). Its table pays Knell 15, Goon 8, Phalanx 8,
  Phage 5, Mount 5, Lesser Phage 2, **and keys the Skreel as `"§7Withered Spider Jockey"`**, which no mob carries, so
  the Skreel falls through to the type default (*read*; legacy F10 says the same).
- **D11 — THE CATALOGUE'S HP COLUMN IS STALE, CONFIRMED.** `CatalogueRegistry`'s `MobEntry` rows read Phage 300,
  Goldrot Goon 150, Phalanx 400, Knell (listed as *"Withered Knight"*) 600, Skreel 200. The factory sets 160, 80, 240,
  480, 120 (*read*, both). The factory is what spawned.

---

## 3. THE FIVE MOBS

**Shared by all five at cfde822** (*read*: `WitheredMobFactory`, `DragonfightManager`):
- **body:** a wither skeleton, except the Skreel's spider mount. `setRemoveWhenFarAway(false)`; Defense 0
  (`MobSpawnHelpers.applyMobDefense(…, 0.0)`, which per its javadoc makes worn armour cosmetic: vanilla armour and
  protection reductions are zeroed); follow range 64 (D9); every equipment drop chance set to 0;
- **identity:** a PDC `BOSS_NAME` string with a `§` colour code, and a PDC `DRAGONFIGHT_MINION` byte. The spawn method
  registers the entity in the per-world minion registry;
- **AI:** vanilla, unless the mob's own section says otherwise. Targeting is vanilla's, plus D3's Enderman reconcile;
- **every spawn is cap-checked** by `phaseCapFor(bossName, currentDragonPhaseInWorld)`, except the Lesser Phage. A
  capped spawn returns null. Outside a fight the phase is −1, which clamps to row 0 (legacy F10's NPE).

**The fight's spawn table** is in `PLAN-legacy-port.md` §3.0 and was re-read against
`DragonfightReinforcements.spawnPhaseWave` / `spawnReinforcement` / `scheduleNext` and `PHASE_TYPE_CAPS`. **Every
figure in it matches.** D1 adds which row a wave meets. Two details it omits (*read*):
- the trickle starts at `initDragonfightEncounter` (phase 0, 160 t), before any threshold;
- each trickle fire picks **one** anchor and jitters every spawn ±2 blocks around it. The anchors are 3 points on a
  radius-15 ring **around (0, 0)**, rotated randomly (`findDragonfightGroupAnchors`); a void column is skipped.

### 3.1 The Knell — SHIPS TODAY AS `content/mobs/knell.yml`, AS A NAME AND A HEALTH POOL

| | cfde822 (`spawnWitheredKnight`, `WitheredKnellBehavior`, *read*) | port |
|---|---|---|
| **what it did** | A big, slow, armoured wither skeleton with vanilla melee. Every 5–10 s it locked a beam on a player, rooted itself for the telegraph, and fired. It sidestepped every arrow. | — |
| **body** | wither skeleton; `SCALE` 1.6; speed × 0.6; HP 480; full diamond armour, Protection I on each piece; diamond sword with Sharpness IV, Knockback I, Fire Aspect I | scale ___ (Ben), speed ___ (Ben), HP ___ (Ben) (ships 360), gear ___ (Ben) |
| **AI** | vanilla wither-skeleton melee; the beam loop runs beside it | — |
| **attack: melee** | vanilla sword hit × 5 on a player. Its vanilla value with that sword is *UNVERIFIED* (not computed) | ___ (Ben); **M17 is NOTED, NOT DECIDED** |
| **attack: beam** | **trigger:** a random 100–200 t (5–10 s) delay, then the vanilla target if it is a player, else the nearest non-creative, non-spectator player within 40. No player in range: wait another cycle. **Telegraph:** locks origin (its eye) and endpoint (the target at 0.6 × its height) once; bell resonate; a purple dust line (RGB 95,35,165, size 0.8) flashed at tick 0 and tick 9; `stopPathfinding()` every tick for **12 t** (0.6 s). **Fire:** block-clipped, a cyan dust line (RGB 40,230,235, size 1.4), bell + amethyst chime. **Hits only the locked player**, and only if that player's box grown by 1.0 still meets the segment. **Damage:** 35, `DamageType.MAGIC`, ×5-exempt. **Cooldown:** the next random 100–200 t starts after it fires. **Range:** 40. | every number ___ (Ben) |
| **reaction: dodge** | **trigger:** any projectile damager, or any `ARROW`-typed by-entity hit (D6). Cancels the damage, deletes the projectile, and teleports 4–5 blocks (uniform random) perpendicular to the incoming direction, picking left or right at random and falling back to the other side if the landing is solid (`WitheredKnightUtil.isPassableLanding`: the block and the one above are non-solid). Soul and soul-fire particles and an amethyst step at both ends. **No cooldown.** Custom ranged weapons that dealt damage by direct `damage()` calls consulted it through `tryWitheredKnightDodge`. Melee, magic and summons are not dodged. | ___ (Ben), and whether at all |
| **dragon fight** | waves 0/1/3 (wave 0 at 80% has none); trickled from phase 1; cap 0/2/4/5. Drops and XP stripped; 15 coins (D10) | — |

**What ships here** (*read*, `knell.yml` and `RpgCommand.spawnMob`): `base_entity: wither_skeleton`,
`display_name: "Knell"`, `max_health: 360`. **It spawns with NO weapon and NO armour**: `spawnMob` passes
`randomizeData = (def == null)`, so a custom mob skips vanilla's spawn equipment (`PLAN-mob-scaling.md` §6 F16 records
the same). It has vanilla scale and speed, no beam and no dodge, and it hits with a bare wither skeleton's
`ATTACK_DAMAGE` × GS/100 and no ×5 (M4, §6 F15). The beam's relation to the Knell Beam weapon is
`PLAN-legacy-port.md` §2.5, re-read here: same colours, range, hit radius and telegraph length; the mob's beam hits one
locked target where the weapon pierces; the mob's `playSound` volume/pitch pairs are the weapon's swapped.

### 3.2 Phage, and the Lesser Phage

| | Phage (`spawnWitheredGrunt`, *read*) | Lesser Phage (`spawnLesserPhage`, *read*) | port |
|---|---|---|---|
| **what it did** | the fight's rank and file: an armoured wither skeleton with a stone sword | the split form: small, fast, unarmoured | — |
| **body** | HP 160; speed × 0.75; one armour tier rolled per spawn, uniform over gold / chainmail / iron, all four pieces of that tier; stone sword | HP 80; `SCALE` 0.6; speed × 1.10; all armour cleared; stone sword | HP, scale, speed, gear ___ (Ben) |
| **AI** | vanilla | vanilla | — |
| **attacks** | vanilla melee × 5 only | vanilla melee × 5 only | ___ (Ben) |
| **on death** | **split:** only when `resolveBounceAwareKiller` finds a player (the killer, or a PDC `LAST_BOUNCE_KILLER`). Spawns 2 Lesser Phages at the corpse, +0.5 and −0.5 on x. Despawns, `/kill` and non-player deaths do not split (`WitheredMinionDeathListener.onPhageDeathSplit`) | never splits | count, offset ___ (Ben) |
| **dragon fight** | waves 6/4/2; trickled from phase 0; cap 10/8/8/7; **the storm's only spawn** (3 per trickle fire during a storm's grace or active window) | **bypasses the cap** (the method has no cap check); tagged `DRAGONFIGHT_MINION` and registered, so it counts toward the global registry but has no cap column of its own | — |

Both carry the catalogue id `WITHERED_GRUNT` (*read*). Coins 5 and 2 (D10).

### 3.3 Goldrot Goon

| | cfde822 (`spawnWitheredRouge`, `onGoldrotGoonDeathExplode`, *read*) | port |
|---|---|---|
| **what it did** | a fast, gold-clad wither skeleton that explodes when a player kills it | — |
| **body** | HP 80; speed × 1.25; golden chestplate with Projectile Protection IV (cosmetic under Defense 0, *inferred* from `applyMobDefense`'s javadoc); golden sword with Sharpness IV | HP, speed, gear ___ (Ben) |
| **AI** | vanilla | — |
| **attack: melee** | vanilla melee × 5 | ___ (Ben) |
| **on death: blast** | **trigger:** a player kill as in §3.2 (`resolveBounceAwareKiller`). **Telegraph:** none; an `EXPLOSION_EMITTER` particle and a generic explode sound at the corpse. **Damage:** 50 to every living entity in a 4 × 4 × 4 half-extent box (`getNearbyLivingEntities(at, 4, 4, 4)`) except itself and any Ender Dragon, ×5-exempt, no block damage. **Who it hits:** D2. | radius, damage ___ (Ben); who it hits ___ (Ben) |
| **dragon fight** | waves 3/3/2; trickled from phase 0; cap 4/4/3/2 | — |

**Its relation to `GoldrotSet` is the name and the gold theme only** (`PLAN-legacy-port.md` §3.3; not re-read here,
*inferred* still true because neither factory method nor death listener refers to it, *read*). Coins 8.

### 3.4 Phalanx

| | cfde822 (`spawnWitheredArcher`, `WitheredPhalanxBehavior`, *read*) | port |
|---|---|---|
| **what it did** | a slow wither skeleton with a bow it never fires. It warps away and turns invisible when a player gets close, shoots a slow homing dark swirl every 5 s, and a shield absorbs its first damage | — |
| **body** | HP 240; speed × 0.65; bow with Power V in the main hand (*inferred* cosmetic, because every shot is cancelled; see AI); shield in the off hand; PDC `PHALANX_SHIELD` byte | HP, speed, gear ___ (Ben) |
| **AI** | vanilla skeleton AI still runs and draws the bow; **every `EntityShootBowEvent` from it is cancelled** (`onPhalanxBowSuppress`). Which goal vanilla picks with a bow is *UNVERIFIED*. One loop every tick runs the blink and the swirl | — |
| **behaviour: blink** | **trigger:** a non-creative, non-spectator player in the D4 box while it is **not** already invisible. Tries 8 random points 6–16 blocks out at the column's highest block + 1; the first passable one wins; none passes, no blink. **Effect:** squid-ink puffs at both ends, an enderman-teleport sound, `setInvisible(true)` and a PDC `PHALANX_INVISIBLE` byte. **Cooldown:** none, but it cannot blink again until it is visible | every number ___ (Ben) |
| **attack: swirl** | **trigger:** a tick counter reaches 100 t (5 s); target the nearest non-creative, non-spectator player within 30. No target: the counter resets and it tries again in 100 t. Firing **clears invisibility**. **Telegraph:** a wither-ambient sound at launch; no wind-up. **Flight:** not an entity. Starts at its eye aimed at the target's eye; moves 0.4 blocks every 2 t; each step blends 10% toward the target's eye and renormalises; re-acquires the nearest player within 35 if the target is lost; three squid-ink particles in a rotating triangle per step. **Ends** on a solid block, past 35 blocks from its start, after 200 steps (400 t), or when the Phalanx dies. **Hit:** the first living entity within a 1.2 half-extent box that is not itself, not the dragon and not a minion; **players and non-minion mobs alike.** **Damage:** 40 via `damage(40.0, ws)`, ×5-exempt | every number ___ (Ben) |
| **reaction: one-shot shield** | the first `EntityDamageEvent` of any cause while it holds `PHALANX_SHIELD` is cancelled; shield-break sound; off hand cleared; byte removed (D5) | ___ (Ben), and what spends it ___ (Ben) |
| **dragon fight** | waves 0/2/3; trickled from phase 1; cap 0/3/4/4. Coins 8 | — |

### 3.5 Skreel, with its Withered Mount

| | cfde822 (`spawnWitheredSpiderJockey`, `WitheredSpiderJockeyBehavior`, *read*) | port |
|---|---|---|
| **what it did** | a wither skeleton riding a big spider. The pair keeps 8–12 blocks off a player, winds up, leaps in a homing arc, and slams down. If either dies, the other becomes an ordinary brawler | — |
| **body: mount** | spider; `BOSS_NAME` "§8Withered Mount"; HP 120; `SCALE` 1.5; speed × 1.3 while paired; **no catalogue id, no equipment**; follow range 64 | every number ___ (Ben) |
| **body: rider** | wither skeleton; "§7Skreel"; catalogue `WITHERED_SPIDER_JOCKEY`; HP 120; iron spear in the main hand, no enchants (its comment: the leap's damage is scripted, the spear is for melee after a dismount); speed untouched. Mounted with `spider.addPassenger(ws)` | HP, gear ___ (Ben) |
| **AI** | **vanilla targeting suppressed:** every tick, `setTarget(null)` on both. Target is the nearest non-creative, non-spectator player within 50 blocks of the spider. No target: stop and reset to RETREAT. A target change resets to RETREAT. A four-state loop, every tick from 20 t after spawn | — |
| **RETREAT** | if the player is 8–12 blocks away, go to WINDUP; else pathfind (speed arg 1.6) to the point 12 blocks from the player along the player-to-spider axis (D7) | ___ (Ben) |
| **WINDUP (the telegraph)** | stop; on entry a spider-ambient sound and 15 crit particles. If the player leaves 6.4–18 blocks, back to RETREAT. After **15 t** (0.75 s) launch: horizontal speed = horizontal distance / 28 (a nominal 28 t flight), vertical 1.2, spider-hurt sound | ___ (Ben) |
| **LEAP** | every tick, re-aim the horizontal velocity at the player at that same speed (homing); keep the vertical. **Lands** when it touches ground after leaving it. **Aborts** to RETREAT if, once airborne, its horizontal speed falls below half the launch speed ("stuck"), or after **40 t** (2 s) | ___ (Ben) |
| **LAND: the hit** | if the player is within 2.5 blocks of the spider and not creative or spectator: **35** damage with **the rider as damager**, ×5-exempt; knockback 0.7 horizontal away from the spider, 0.4 vertical; a crit sound. Then RETREAT. **Cooldown:** none beyond the loop; the next leap needs a fresh RETREAT → WINDUP | every number ___ (Ben) |
| **pair break** | the mount dies: the rider leaves the vehicle and `setAI(true)` (a no-op, *inferred*: AI was never disabled). The rider dies: the spider stops pathing, `setAI(true)`, and its speed is divided by 1.3 (D8). The loop ends either way | ___ (Ben) |
| **dragon fight** | 2 pairs in wave 2; trickled from phase 2; cap 0/0/3/4, **counted by Skreels**, so it caps pairs. Both entities are tagged and registered. Coins: mount 5, Skreel the type default (D10) | — |

**One vanilla fact bites the spear:** what a spear does when a mob wields it is *UNVERIFIED* (`PLAN-legacy-port.md`
§6 F8).

---

## 4. WHAT OUR MOB SCHEMA CAN SAY TODAY

**The whole schema is four fields** (*read*): `MobDefinition(id, baseEntity, displayName, maxHealth)`, filled by
`MobLoader.parse` from `base_entity`, `display_name` and `max_health`; the id is the filename. **`MobDefinition` has 8
constructor call sites**: 5 in `MobRegistryTest`, 2 in `MobSeedingTest`, 1 in `MobLoader` (counted with
`grep -rn 'new MobDefinition(' --include=*.java .` piped to a per-file count, at `cd790dcc`). Its own javadoc says it
is *"Shaped to grow. {@code attackDamage} and the rest of the stat block become further components."*

**What a content file can and cannot express, read from the Knell:**
- **CAN:** which vanilla entity; its real CustomName (MiniMessage); its custom HP pool, which is uncapped (the
  `knell.yml` comment), and × GS/100 under M4 with GS from M25 (overworld 100, Nether 200, End 300 within 1,000 blocks
  of (0, 0)).
- **CAN, by omission:** vanilla AI and vanilla targeting, because nothing replaces them.
- **CANNOT:** equipment (it spawns bare, §3.1), scale, speed, attack damage, follow range, any timed behaviour, any
  reaction to damage, anything on death, a mount, a faction, a drop table.
- **ONE WAY IN:** `/rpg spawn <id>` is the only writer of `mob_id` (`RpgCommand.spawnMob`, *read*). There is no
  natural spawn and no spawner.

**Engine facts that bite** (*read* unless marked):
- **Homing skips players.** `ProjectileFlight.nearestMob` has `if (candidate.state().player()) continue;`. A
  mob-cast homing bolt reusing the projectile cast could not seek a player (legacy F14).
- **The cast pipeline takes a snapshot, not a player.** `AbilityService.cast(CombatantSnapshot caster, …)`. Every
  paper caller found (`P/build/StoneCaster`, `P/command/RpgCommand`) is player-driven (*inferred* from a grep for
  `.cast(` in `paper/src/main/java`; whether a mob snapshot runs end to end is not known).
- **Mob damage pricing is M24's:** a custom mob's hit is authored × GS/100, but no authored field exists, so every
  scripted mob hit is unpriced (legacy F13).

### 4.1 Each capability, tiered

**(a)** content-only, expressible in YAML today. **(b)** an engine capability: a new field or handler in an existing
system. **(c)** a new system. The ids are `PLAN-legacy-port.md` §9.1's.

| capability | needed by | tier | note |
|---|---|---|---|
| name + HP on a vanilla body | all five, as six files | **(a)** | the Lesser Phage and the mount are their own files |
| equipment: main hand, off hand, four armour slots; enchants on them; drop chance 0 | Knell, Phage, Goon, Phalanx, Skreel | **(b) MB1** | the per-spawn **random armour tier** of the Phage is a small extra: a pick-one list |
| `SCALE`, `MOVEMENT_SPEED` multiplier, `FOLLOW_RANGE` | Knell, Lesser Phage, mount; speed on four | **(b) MB1** | `Attribute.SCALE`, `MOVEMENT_SPEED`, `FOLLOW_RANGE` exist in the pinned jar (`PLAN-legacy-port.md` §1, javap; runtime behaviour *UNVERIFIED*) |
| `attack_damage` | all five's vanilla melee | **(b) MB1** | **closes M17 only if Ben rules it** (NOTED, NOT DECIDED) |
| suppress the vanilla bow shot | Phalanx | **(b)** | one listener keyed on `mob_id` |
| on death: spawn N of mob id X | Phage | **(b) MB2** | must name its kill condition (player kill only, at cfde822) |
| on death: an area hit | Goon | **(b) MB2** | its price is legacy Q-P12; who it hits is D2 |
| a timed attack at a player: lock, telegraph, root, fire | Knell beam | **(b)/(c) MB3 + E5** | MB3 is the runner (a periodic cast by a mob); E5 is the aim-locked wind-up, shared with the Knell Beam weapon |
| a homing bolt that seeks players | Phalanx swirl | **(b)** inside MB3 | needs a player-seeking mode beside `nearestMob`'s skip (legacy F14) |
| a pre-damage reaction: cancel + sidestep | Knell dodge | **(c) MB4** | it must see custom weapons' hits too, which today resolve through our own pipeline, not vanilla projectiles (*inferred*) |
| a pre-damage reaction: one-shot shield | Phalanx | **(c) MB4** | the same hook as the dodge |
| proximity blink + invisibility, revealed on attack | Phalanx | **(c) MB4** | `LivingEntity.setInvisible` exists in the jar (legacy §1) |
| a mounted pair, spawned together, with survivor conversion | Skreel + mount | **(c) MB5** | `Entity.addPassenger` exists in the jar (legacy §1) |
| a movement state machine (retreat, wind-up, leap, land) | Skreel | **(c) MB5** | cfde822 used pathfinder `moveTo` plus `setVelocity` every tick |
| faction: no friendly fire among fight mobs; war with Endermen | all five | **(c), the fight's** | it is fight state, not a mob field (§6 C4) |
| drops/XP stripped, coin payout | all five | **(c), the fight's** | there is no coin or drop table for mobs here today (*inferred*: no `drops` key in `MobLoader`) |

---

## 5. PROPOSED BUILD ORDER — SMALLEST FIRST. BEN RULES THE ORDER

This refines `PLAN-legacy-port.md` §9.3; where it differs, it says why.

1. **S1 — MB1, mob body fields** (equipment, scale, speed, follow range, and `attack_damage` if M17 is ruled). Core
   unit test first, then `MobLoader`; 8 call sites (§4). **Unlocks:** Phage, Lesser Phage and Goldrot Goon as nearly
   whole mobs (all three are vanilla AI at cfde822); the Knell's body; the mount's and the Phalanx's bodies, inert.
2. **S2 — MB2, on-death effects** (spawn N of an id; an area hit). **Unlocks:** the Phage split and the Goon blast,
   so the fight's wave-0 mobs are complete. Gated on the blast's price (legacy Q-P12) and on who it hits (Q4 below).
3. **S3 — MB3 + E5, a mob's timed attack with an aim-locked telegraph.** **Unlocks:** the Knell's beam, and E5 for the
   Knell Beam weapon (legacy §9.2 step 5) for free.
4. **S4 — MB4, pre-damage reactions + blink.** **Unlocks:** the Knell's dodge and the Phalanx's shield, which share one
   hook, and the Phalanx's blink. *Differs from legacy §9.3*, which split the dodge (step 3) from the Phalanx (step 4):
   the shield and the dodge are the same hook, so building it once is the smaller slice.
5. **S5 — the Phalanx's swirl**: MB3 with a player-seeking homing mode, plus bow suppression. **Unlocks:** the whole
   Phalanx.
6. **S6 — MB5, the mounted pair and its state machine.** **Unlocks:** the Skreel. Last, because it is the only pair,
   the only state machine and the only spear.

**Why this order:** each step adds one capability; it follows the old fight's escalation (wave 0 is Phages and Goons,
wave 1 adds Phalanx and Knell, wave 2 adds Skreels); and S1–S2 give the fight its most numerous mobs before any
scripted behaviour. **The dragon fight itself is its own thread, after these**, and the faction and drop rows of §4.1
belong to it.

**Each mob slice's gate** carries `/rpg spawn <id>` rows in the End and the overworld, so M25's 300 against 100 is
read, not assumed (legacy §8). **The game mode is declared on every row** (`verification.md`, *THE
CREATIVE-DIVERGENCE REGISTER*): every cfde822 behaviour above skips creative and spectator players, so a creative
bench reads none of them.

---

## 6. DESIGN CONSTRAINTS FOR THE LATER DRAGON FIGHT

**Ben's direction, as briefed (intent, not his words verbatim):** one **TEMPORARY WORLD per group** now, built so it
can move to **a proxy plus a per-fight server** later. The fight does not care where it runs. The **arena is a
template file**. **Fight state lives in the fight**, not in static singletons (CLAUDE.md invariant 3). **Player saving
stays behind its storage port** (`PlayerRepository`).

Each constraint below says what it forbids in practice, and what cfde822 did instead (*read* unless marked). The
mobs above do not need the fight to exist, but **every mob capability must be built so the fight can own it.**

- **C1 — ONE TEMPORARY WORLD PER GROUP.**
  - **Forbids:** finding the fight by dimension or by a fixed world (`Environment.THE_END`, `getEnderDragonBattle()`,
    `getServer().getWorlds()` scans); fixed coordinates as fight geometry ((0, 0), the radius-15 anchor ring, the
    ±200 Enderman box); one fight per world assumed by a map keyed on world UUID shared across fights; a mob behaviour
    that reads anything outside its own world.
  - **cfde822 did:** all of those. The fight was the vanilla End's dragon, started through the `DragonBattle` respawn
    ritual (`spawnDragonfightEncounter`), anchored at (0, 0), and its registries were keyed by world UUID.
  - **And it meets M25** (*inferred*): GS is read from the dimension and the distance from (0, 0). A temporary world's
    environment and coordinates would set the minions' GS. Whether the fight sets its own GS through M3's
    `GearScoreSource` seam is Q9.
- **C2 — THE FIGHT DOES NOT CARE WHERE IT RUNS (proxy + per-fight server later).**
  - **Forbids:** `Bukkit.getOnlinePlayers()` or `world.getPlayers()` as the roster (invariant 3); server-wide
    broadcasts (cfde822 used `getServer().broadcast` for start, fail and death); reading another fight's state; a fight
    that assumes the player's profile was loaded by this JVM before the fight began.
  - **Requires** (*inferred*): the roster is handed to the fight when it starts; getting players in and out is one
    seam (a local teleport now, a proxy transfer later); the fight reports its result as data, not as side effects on
    shared server state.
- **C3 — THE ARENA IS A TEMPLATE FILE.**
  - **Forbids:** using vanilla End generation, pillars or crystals as the arena; anchor points, rings and boxes as Java
    literals; the spawn table (waves, caps, cadences, the storm's Phage-only rule) in Java. **Invariant 2 already says
    the spawn table is content**, and the template is where its positions live.
  - **cfde822 did:** every one as a Java constant (`PHASE_TYPE_CAPS`, `DRAGONFIGHT_REINFORCEMENT_PERIOD_TICKS`,
    `DRAGONFIGHT_PILLAR_RING_RADIUS`, the `switch` in `spawnPhaseWave`).
- **C4 — FIGHT STATE LIVES IN THE FIGHT.**
  - **Forbids:** fight state in a plugin-wide manager or a static; a mob's behaviour loop that outlives its fight; a
    cleanup that sweeps "every tagged entity in the world" rather than the fight's own list; `BukkitRunnable` or the
    Bukkit scheduler (banned; every loop goes through `Scheduler`, and a per-mob loop through `onEntity`).
  - **cfde822 did:** one manager held 7 `Map` fields of per-fight state keyed by dragon or world UUID, and
    `DragonfightReinforcements` an 8th (counted by grepping `private final Map<` in both fetched files). Every mob
    loop was a self-rescheduling `BukkitRunnable` that ended only when its mob died. Teardown swept every entity
    tagged `DRAGONFIGHT_MINION` in the world.
  - **So the mob capabilities must be fight-ownable:** an MB3/MB4/MB5 behaviour is started and stopped by its owner,
    and a `mob_id` mob spawned by a fight belongs to that fight (*inferred* design consequence).
- **C5 — PLAYER SAVING STAYS BEHIND `PlayerRepository`.**
  - **Forbids:** the fight writing player data itself (files, PDC as the record of rewards); rewards granted only in
    memory on the fight's server; `Scheduler.async` touching the Bukkit API while it saves.
  - **cfde822 did:** credited coins directly (`plugin.addTreasure`, D10) on the shared server.

**NOTED, NOT DECIDED:**
- **M17, the Knell's damage** (`PLAN-mob-scaling.md` §0 M17, §6 F15). S1 can add `attack_damage`; whether it does,
  and the Knell's number, are Ben's (legacy Q-P11).
- **M27 / F22, the perched dragon** that damage never knocks airborne (`PLAN-mob-scaling.md` §0 M27, §6 F22). For the
  record only: cfde822's perched dragon took **× 0.4 on every non-melee hit** and **× 4 on MAGIC** to undo vanilla's
  0.25 (`DragonfightCombatListener.onPerchedDragonDamage` / `onDragonVanillaScalingCompensation`, *read*), and the
  dragon was immune while a Commander, a Dark Mage or any End crystal lived (`onDragonfightImmunityGate`, *read*).
  Recorded, not adopted.

---

## QUESTIONS FOR BEN

*Answers go into a RULINGS block at the top of this file, verbatim. Legacy Q-P11 to Q-P14 stay open where they are;
Q2, Q3 and Q5 below restate three of them in one line each so this list stands alone.*

1. Should these five mobs only appear inside the dragon fight, or also out in the world?
2. Should the Knell get its own hit strength now (M17), or keep hitting like a bare wither skeleton for a while longer?
3. The beam, the blast, the swirl and the leap: should each be a number you set, times gear score / 100, like other custom mobs?
4. Should the Goon's death blast hurt other dragon-fight mobs, or only players?
5. Is the Skreel's spider its own mob, with its own name and health bar?
6. When one of the Skreel pair dies, should the other keep fighting as a normal mob?
7. Should the Phalanx's shield block the first damage of any kind (a fall, fire), or only the first hit from a player?
8. Should the Knell sidestep every arrow, every time, as the old one did, even though that makes it nearly immune to rangers?
9. Inside the fight's world, what gear score should the mobs have: the End's 300, or a number the fight sets?
10. Should the fight's mobs fight Endermen, as the old fight's did?
11. Should the fight's mobs drop anything or pay coins, given that the old ones dropped nothing but still paid coins?
12. If the server restarts mid-fight, is that fight simply lost?
13. Is the order in §5 right: body fields first, so the Phage and the Goon come first?
