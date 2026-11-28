# PLAN: Solar Flare, the Fire Ranger's ultimate

**Status: BUILT on `feat/solar-flare` (not booted, not merged),** stacked on `feat/water-douses-scorch` (#338). Its gate is
`GATE-solar-flare.md` on the ONE gate branch, `feat/smolder-gate`, moved to sit on this branch, last.

Sources: Ben's words of 2026-11-27 (relayed; quoted below) and the seat's Solar Flare brief of the same day (F1 to F14,
the gate). This file is the account; the brief is not committed.

## 1. Ben's words, verbatim, 2026-11-27

> "While that's running id like to scope the Fire Ranger Ultimate. I don't have a name for it so if you have any
> suggestions that would be appreciated. It fires 3 giant fireballs in an arc that travel straight for about 15 blocks.
> Each fireball deals 250 damage and explodes in a 6 block range either when it hits something or at the end of its
> range"

> "We'll use solar flare 1. Correct, 30 degrees 2. That is the explosion damage, the whole area takes that damage. 3. Yes
> it takes all the damage. 4. Before I answer this I'd like to know the maximum damage each ultimate can do 5. .8 seconds
> 6. Yes 7. Yes 8. Yes, with one change: when the fireballs explode, I want them to use the same particles that the
> smolder pulses do."

> "1. 80 mana and 3.5 minutes 2. The same expanding ring. Can we do the damage is dealt when when the ring gets to the
> target"

## 2. The rulings as built (F1 to F14), each with its READs

### F1, the flight and the fan
- **The fan is the engine's own spread, unchanged.** READ, `SpreadPattern.directionsFor`: the first direction is the
  aim; the other `count - 1` are spaced round a ring `angle_degrees` off it, at `phi = 2 pi i / (count - 1)` from the
  shooter's RIGHT toward his UP, in his view plane. `count 3, angle_degrees 30` gives `phi` 0 and 180: **exactly the aim
  and the aim turned 30 degrees to his right and to his left.** Fixed, not random. No field added.
- **Pinned:** `SolarFlareContentTest.theFanIsMinusThirtyZeroPlusThirty` runs the shipped spread through the pattern:
  yaws -120, -90, -60 for an aim due east (-90), and at a pitched aim each direction is 0 or 30 degrees off it.
- **Numbering (CC'S CALL):** LEFT TO RIGHT, by each direction's component along his right: **n=1 at -30, n=2 on the aim,
  n=3 at +30.** The alternative: the spread's own order (the aim first, then right, then left).
- **The flight, READ, `ProjectileFlight.step`:** the step at elapsed e traces the segment from `launch + e x v` to
  `launch + (e + 1) x v` on the cast's tick + e (the first inline on the cast's own frame); after the step at elapsed
  `L - 1` the flight ends at `launch + L x v`. So **speed 0.9375 (15/16) and `max_lifetime_ticks` 16, gravity 0.0, reach
  exactly 15.0 blocks** (16 x 0.9375 is exact in binary), the end on **the cast's tick + 15**: the sixteenth tick counting
  the cast's (Ben's 0.8 s). The launch point is his EYE (the aim's origin).

| tick (C = the cast) | the shot's segment, in blocks from his eye |
|---|---|
| C | 0 to 0.9375 |
| C + e | 0.9375 e to 0.9375 (e + 1) |
| C + 7 | 6.5625 to 7.5 |
| C + 8 | 7.5 to 8.4375 (a body 8 out is met here) |
| C + 15 | 14.0625 to 15.0, then it bursts at 15.0 (cause=range) |

### F2, what stops it (READ, `PaperCombatWorld.castRay`)
`world.rayTrace(from, direction, step, FluidCollisionMode.NEVER, ignorePassableBlocks true, RAY_SIZE, filter)`:
- **Blocks:** any non-passable block. **Fluids never** (water, lava). **Passable blocks never** (grass, flowers, torches).
- **Bodies:** `LiveTargets.except(caster)`: every live LIVING body except the caster, **minus a summon** (the sides table's
  PLAYER -> SUMMON "no": `MobHostility.refusedSummonCell`, the carried note). So **a passive animal (a cow, a villager)
  stops it, and so does another player** (`PvpPath.PROJECTILE` is `true` for PLAYER -> PLAYER, so he is "admitted" and the
  burst is at his feet; the ring spares him, `PULSE` is spared). **OPEN FOR BEN; the shared rule is unchanged.**
- **The MOB side is every non-player, non-summon living body** (READ, `CombatSides.classify`), so Solar Flare's rings,
  like Smolder's (and every burst's), strike a passive animal too. The seat's Z4 defines "hostile" as the MOB side, so
  this is not a stop. **OPEN FOR BEN** (one line, both ultimates).

### F3, F4, F5: the explosion is ONE pulse front at the impact point
- **Reuse, not a copy:** `CastSpec.Projectile` gains a tail field `burst`, a `CastSpec.Pulses` of count 1 (Smolder's own
  record), and the executor's `burstAt` calls Smolder's own `ringPass` with the impact point as its origin (it already
  took the origin as a parameter). ONE radius function (`PulseFront.radiusAt`), ONE reach rule (`PulseFront.reachTick`),
  ONE "in range" measure (the burst's box cut to the radius horizontally, timed by horizontal distance from the fixed
  origin), ONE struck set per front, ONE sides check (`PvpPath.PULSE`). Smolder's rows (`PulseCastTest`, 14) pass
  unchanged.
- **The three origins (F3):**
  - **On a MOB:** the STRUCK BODY's own position (its feet), so its ring reaches it at distance 0 on the burst's own tick
    and nothing else from that shot does (F5: a direct hit adds nothing). Pinned with a fixture whose hit point is on
    the body's surface, not its position (`FakeWorld.surfaceHits`: with it off, a burst at the hit point passed every row).
  - **On a BLOCK:** the point the trace met it (`RayTraceResult.getHitPosition`, READ), on the block's face.
  - **At the END OF RANGE:** the end of the last step, `launch + 16 x 0.9375`, at his eye's height.
- **F4:** radius 6, `travel_ticks` 15: Smolder's speed, 12 in 30 (SEAT PLACEHOLDER, OPEN FOR BEN). A body at distance d is
  struck on the burst's tick + the first whole t with `6 t / 15 >= d` (`d = 4` at + 10).
- **F5:** three shots make three fronts, each its own struck set, so a body inside two or three takes 250 from each
  (`SolarFlareCastTest`: two rings at range, three behind a wall).

### F6, every term of a hit
Each ring's hit is the ordinary Damage arm, as Smolder's (`EffectApplier`):
`sent = HitDamage.dealt(hitBase(GearScore.scaledDamage(250, score), enchant%, classDamage + 0), charge 1.0, crit)`.
- **score**: the held item's (`GearScoreItems.heldScore`); the Ability Stone carries no stamp, so **100**.
- **enchant%, class damage**: the cast's snapshot; **0** with the stone in hand, no armour and no accessory.
- **level damage: 0** (a stone cast is not a weapon hit). **charge 1.0.**
- **crit**: `Crit.BASE_CHANCE` 0.15, rolled ONCE at the cast (the snapshot) and frozen for all three shots and their
  rings (`CastExecutor.launch`'s own account, unchanged): **250 uncrit, 500 crit** (`1 + Crit.BASE_DAMAGE` = 2.0).
- Then the mob's defence (**0** on a mob, READ, `HealthState`'s `Stat(0.0)`).
- **Scorch by the element:** cap `(sent / crit) x 0.5` = **125**, a crit does not raise it; window 120, reset by each hit;
  each burn `min(5% of max, 125)`. Ignite and the water rule apply as to every fire hit. No status step authored.

### F7, who is hit
The sides table, `PvpPath.PULSE` (the same path: a front's hit is a pulse's): players spared, summons refused (PLAYER ->
SUMMON), never the caster (by id). No new entry: **16 PvP paths, unchanged** (`MobHostility.java` is not in the diff).

### F8, the cast and its end
- 80 mana and 4200 ticks, once, at the cast (`AbilityService`).
- **One run per caster** in `PulseRuns` (the server's one), counting shots still to burst. New `PulseRuns.landed`: shots
  burst in ANY order, so it COUNTS where `fired` takes a pulse's number; the last one ends the run.
- **Its end is Smolder's path, unchanged:** `PulseRemovals.end` on death, quit, world change, server stop, `clear_state`
  and the row-start clean write `SOLAR_FLARE cancelled remaining=<shots not yet burst> cause=`. **Each flight asks at
  every step whether its run is still current** (a new `ProjectileFlight.launch` overload with a `live` check; every other
  flight passes `() -> true`, unchanged): an ended run's shots stop there, no burst, their bodies removed, within one tick.
  Every step is already a `PendingWork` link at its point (READ, `PaperCombatWorld.schedule`), so the pad's clean ends one
  too. **A ring already expanding finishes** (Smolder's CC'S CALL).
- **K5 as E5:** no stone cast ends the Blink Tome's invisibility, Solar Flare included.
- **One run per caster, shared with Smolder (CC'S CALL):** a player has one Q, so a second ultimate run replaces the first
  (`cause=replaced`). The alternative: runs keyed by caster and ability.

### F9, the body
- **READ:** an `item:` body is a dropped ITEM driven by velocity (`spawnMarker`), a `body:` is an arrow or a fireball
  entity (`spawnBoltMarker`); neither can be scaled (an item entity has no scale; a fireball is not a living entity, so no
  `SCALE` attribute).
- **Added, the smallest declared body:** `body_scale` on an item body. Above 0 the flight spawns
  `CombatWorld.spawnDisplayMarker`: an **`ItemDisplay`** carrying the item, scaled by its transformation, billboarded to
  face the viewer, `setTeleportDuration(1)`, `setPersistent(false)`, tagged `markerEntity` after the add (the Lapis orb's
  `BlockDisplay` shape, READ there). `driveMarker` PLACES a display at the step's end (a display has no mover); the
  flight's own end, or its run's, removes it. The selftest's clean finds a leaked one by its marker tag.
- An item display is drawn about its position (INFERRED; the boot reads it). `fire_charge`, scale 2.5, the
  `solar_flare_trail` flame trail and the `solar_flare_cast` launch sound (`item.firecharge.use`, already verified by
  another visual): SEAT PLACEHOLDERS.

### F10, Smolder's own ring and burst
`solar_flare.yml`'s burst names `smolder_ring` and `smolder_pulse`; both visuals now say a change moves both ultimates.
Pinned by `SolarFlareContentTest.theRingAndTheBurstAreSmoldersOwnFiles`.

### F11, the placeholder
`ranger_fire.yml`'s ultimates and default name `solar_flare`; `ultimate_placeholder_ranger.yml` is deleted and retired
(a stale copy is skipped and named). A saved loadout naming it reads an EMPTY Q with the `BUILD unoffered` line (A3):
`LoadoutResolutionTest.aSavedLoadoutNamingTheDeletedRangerPlaceholderReadsAnEmptyUltimate`. Two melee placeholders'
comments pointed at the deleted file; marked `(deleted 2026-11)`.

### F12, the description
SEAT PLACEHOLDER: "Fire three great fireballs in a fan." / "Each bursts within 6 blocks for 250 fire damage and
scorches." Rebuilt from the parsed file by `SolarFlareContentTest`.

### F13, the lines (pinned by `PulseRunsTest`)
```
SOLAR_FLARE cast caster=<uuid> tick=<t>
SOLAR_FLARE shot n=<1..3> dir=<yaw pitch> tick=<t> caster=<uuid>
SOLAR_FLARE burst n=<n> cause=<mob|block|range> origin=<x y z> tick=<t> caster=<uuid>
SOLAR_FLARE hit n=<n> target=<type uuid> dist=<horizontal> tick=<t> sent=<amount> caster=<uuid>
SOLAR_FLARE cancelled caster=<uuid> remaining=<n> cause=<cause> tick=<t>
```
`dir` is in the server's convention (yaw 0 south, -90 east, growing to his right; pitch positive down). `caster=` is
added beside the seat's fields, as on Smolder's lines.

### F14, recounts (on `feat/solar-flare`, against `98ad57c6`; each recounted from its source)
- abilities **11** (11 at the base: one added, one deleted): `ls content/abilities/*.yml | wc -l`.
- builds **5**, unchanged.
- visuals **51** (49: `solar_flare_trail`, `solar_flare_cast`): `ls content/visuals/*.yml | wc -l`.
- PvP paths **16**, unchanged (the enum's constants by `grep -oE '[A-Z_]+\((true|false)\)'`; a first `awk` that counted
  lines read 13, because several lines hold two constants).
- Fire damage sites **19 -> 19** (`ScorchContentInvariantTest`, -1 +1).
- Gates and scenarios: on the gate branch (`GATE-solar-flare.md`).

## 3. CC'S CALLS

1. Shots numbered left to right (n=1 at -30).
2. On a mob, the burst's origin is the struck body's position (feet), so the ring meets it at 0 (F5 asks for distance 0);
   the alternative is the trace's hit point (distance ~0.3 to 0.9, the ring's tick 1 or 2).
3. A body that stops a shot is `cause=mob` whatever it is (another player included).
4. One ultimate run per caster, shared with Smolder.
5. `body_scale` on an item body as the scaled body (an `ItemDisplay`), billboarded CENTER.
6. The burst refuses a `count` or `interval_ticks` by name (one front).

## 4. Tests and mutations

New: `SolarFlareCastTest` (8, core), `SolarFlareContentTest` (7), a `ContentValidatorTest` row, two `PulseRunsTest` rows,
a `LoadoutResolutionTest` row. `PoolLoaderTest` and `AspectLoaderTest` name `solar_flare` where they named the deleted id.

| mutation | the change | red |
|---|---|---|
| MUTORDER | the left-to-right sort removed | five `SolarFlareCastTest` rows |
| MUTORIGINMOB2 | a mob burst at the trace's hit point | `aBodyStopsItsShot...` |
| MUTRANGE | the end of range reported as a block | `threeShots...`, `aSummonOrAPlayer...` |
| MUTLIVE | the flight never asks its run | `endingTheRunInFlight...` |
| MUTLANDED | a landing not counted | `PulseRunsTest.landed...`, `eachShotsBody...` |
| MUTSCALE | the scaled body never spawned | `eachShotsBody...` |
| MUTSCHEMA | `burst:` not parsed | `SolarFlareContentTest` (2 rows) |
| MUTVALIDBURST | the burst's visuals unchecked | `ContentValidatorTest.aProjectilesBurst...` |
| MUTRETIRED2 | the placeholder not retired | `aStaleCopyOfTheDeletedPlaceholder...` |
| MUTYAW | the shot line's yaw sign | `PulseRunsTest.theShotAndBurstLines`, `threeShots...` |

**MUTORIGINMOB (the first attempt) SURVIVED, and the cause was the FIXTURE:** `FakeWorld.castRay` reported a body hit at
the body's own position, so "the struck body's position" and "the hit point" were the same number in every row (a
hollow fixture, KIND). `FakeWorld.surfaceHits` reports the point where the segment meets the body's radius; the stopper
row turns it on, and the re-run (MUTORIGINMOB2) reddens it.

**Not unit-witnessed (a real entity):** `PaperCombatWorld.spawnDisplayMarker` and the display arm of `driveMarker`. The
boot reads the body (SF9's question) and the entity probe reads none left (SF8).

## 5. OPEN FOR BEN

- F2: a passive animal or another player in a flight path stops a fireball; the rings (Smolder's too) strike passive
  animals (the MOB side).
- F4: the ring's speed (Smolder's, 6 blocks in 15 ticks).
- F9: the body (`fire_charge` at 2.5), the trail, the launch sound.
- F12: the description. The gate's SF9 question wording.
- F11 (A3): a saved Fire Ranger build shows an empty ultimate until he picks Solar Flare.
- Mana and cooldown were Ben's ("80 mana and 3.5 minutes"); his question on "the maximum damage each ultimate can do"
  was answered by the seat before he ruled them.

**Only the boot can show:** the fireballs as drawn (the display's size, centring and easing), the trail, the launch
sound, the ring at each burst, and the flight on a real server's clock.
