# PLAN: Solar Flare, the Fire Ranger's ultimate

**Status: BUILT on `feat/solar-flare` (not booted, not merged),** stacked on `feat/water-douses-scorch` (#338). Its gate is
`GATE-solar-flare.md` on the ONE gate branch, `feat/smolder-gate`, moved to sit on this branch, last.

> **2026-11-28: MERGED (#340, `cb354faf`), booted (`01be9d3b`), and CHANGED BY BEN: section 6 is the account of the
> change (N1 to N7), built on `feat/solar-flare-volley`. Sections 1 to 5 are kept as the record of the shape before it;
> where section 6 says otherwise, section 6 holds (the fan, the 15 blocks, the travelling ring, F10's shared files).**

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

## 6. Ben's changes, 2026-11-28 (the seat's N1 to N7), on `feat/solar-flare-volley`

**Ben, verbatim (relayed by the seat):** *"3. The Ranger Ultimate, instead of a fan of three fireballs, I want 3
fireballs one after another. 4. Increase the Fireball speed they're too slow, Increase the range to 32 blocks. 5. I want
to change the particles to a firework-style explosion."* Then: *"2. .5 seconds 3. 0.8 seconds for the 32 blocks 4. yes
5. Instant, I'd like to mention I don't want a firework, I want a fireball that explodes like a firework"*. NOT STATED:
the burst's colours and shape; whether 250 changes (it STANDS); whether passive animals are spared (NOTHING changes).

### N1, one after another: a SALVO, not the volley (READ, CC'S CALL)

- **READ, `CastExecutor.volley`:** it does re-read the aim per shot (`world.aimOf` on the caster's thread), but it ALSO
  rebuilds the whole `Caster` per shot (`Caster.of(self.state())`: a fresh crit roll, the gear score and behaviours
  re-read), and books each next shot with `scheduleOn`, a link no run holds, so no removal path can end the shots still
  to come. N1 rules one crit at the cast and every pending shot ended by the pulses' removal path. **So the volley's
  mechanism carries a per-shot aim read and also two things N1 forbids: the pulses' own scheduling is used (N1's "else").**
- **`CastSpec.Projectile.Salvo(count, intervalTicks)`**, the record's new tail field; YAML `salvo: {count: 3,
  interval_ticks: 10}`. Refused without a `burst:` (the shots live in the burst's run, which is how a removal finds them)
  and beside a `spread:` (no ruling combines a fan with a salvo).
- **`CastExecutor.salvoShot`:** shot 1 on the cast's tick down the cast's aim; each next shot booked for
  `C + interval x n` through `bookOnCaster` (X6 (a): exact on the server's tick), a pending link of the run; on its tick
  it reads `world.aimOf` ON HIS OWN THREAD (his eye and look at that tick: Ben's "yes") and fires. Launch ticks **C, C+10,
  C+20** (`SolarFlareCastTest.threeShotsTenTicksApart…`, on the server's phases). **CC'S CALL:** shot 1 uses the cast's
  aim, read at the press P = C − 1 (the alternative: a hop to read it at C, which would move shot 1 off the cast's tick).
- **Once:** mana and the cooldown at the cast (`AbilityService`; `manaAndTheCooldownAreTakenOnceAtTheCast`); the crit
  rolled once at the cast and frozen for all three (`theCritRolledAtTheCastHoldsForEveryShot`: crit 2.0 at the cast, 1.0
  after, every burst strikes for 200). Shots numbered 1 to 3 in launch order.
- **The run** counts the three shots; `landed` counts bursts; **remaining = shots not yet burst** (an in-flight shot
  included). Ended between shots: the pending link is cancelled (shot 3 never launches), the in-flight flights stop at
  their next step (their `live` check), no burst, no body left (`endingTheRunBetweenShots…`, remaining 3 at tick 12;
  `endedAfterTheFirstBurstTwoRemain`, 2 at tick 17).

### N2, the flight: speed 2.0, gravity 0, lifetime 16

READ, `ProjectileFlight.step` (unchanged): the step at elapsed e traces `launch + 2e` to `launch + 2(e + 1)` on the launch
tick + e, the first inline; after the step at e = 15 the flight ends at **launch + 32.0** (16 x 2.0, exact), on the launch
tick + 15, from the eye point it left.

| tick (L = that shot's launch: C, C+10, C+20) | the shot's segment, blocks from its eye |
|---|---|
| L | 0 to 2 |
| L + e | 2e to 2(e + 1) |
| L + 15 | 30 to 32.0, then it bursts at 32.0 (`cause=range`) |

**SWEPT, READ by javap (`CraftWorld`, the pinned 26.1.2 server):** `castRay` calls `World#rayTrace(from, dir, step
length, NEVER, true, 0.0, filter)`; `rayTrace` runs `rayTraceBlocks` over the whole length (a `ClipContext` from the start
to start + dir x length, `ServerLevel.clip`: a walk of every block the segment crosses), shortens the entity trace to
the block hit, then `rayTraceEntities`, which takes the bodies in the swept box (`BoundingBox.of(start, end)
.expandDirectional(…).expand(raySize)`) and tests each one's box with `BoundingBox.rayTrace(start, dir, length)`, the
nearest winning. **So a 2-block step cannot pass through a thin wall or a body between two step ends**, pinned in the
fake by `aTwoBlockStepIsSweptSoAThinWallOrABodyBetweenStepEndsStopsTheShot` (a wall at 3 stops it on tick 1 at 3.0; a
body at 5 on tick 2, the burst at the body). F2's collision rule is otherwise unchanged.

### N3, the burst is instant: `travel_ticks: 0`

**READ BEFORE THE CHANGE, AND IT CONTRADICTED THE BRIEF:** 0 was REFUSED (`CastSpec.Pulses`: "travel_ticks must be 1
or more … the ring takes at least one tick"), and `PulseFront.radiusAt(0, r, 0)` returned 0, so even accepted, only a
body at distance 0 would have been struck. **Built anyway (the ruling, instant within 6, is unambiguous), and reported
first:** `Pulses` accepts 0 (below 0 refused); `radiusAt` is the whole radius from tick 0 when the travel is 0, so
`reachTick` is 0 for every distance up to the radius and −1 beyond (`PulseFrontTest.aTravelOfZero…`: 5.9 and 6.0 at 0,
6.1 never); `ringPass` ends after tick 0. One struck set per burst: three bursts in one place give a body 250 from each,
each on its own burst's tick (`threeBurstsInOnePlace…`: 5.9 struck at 4, 14, 24; 6.1 never).

### N4, "a fireball that explodes like a firework": ROUTE B

- **The body stays the fireball** (F9, as built).
- **READ, what a real firework does** (`FireworkRocketEntity.dealExplosionDamage`, the pinned server): with any star in
  its explosions list it hurts its rider and every `LivingEntity` in its box inflated by 5 with `distanceToSqr` at most
  25 and a clear `clip` to the body, for **(5 + 2 x stars) x sqrt((5 − d) / 5)**, through `hurtServer` with
  `damageSources().fireworks(rocket, owner)`: **players included**. With no star it hurts nothing, and the client draws
  no coloured burst.
- **Why not Route A:** its zero rests on cancelling that damage for every body at our seam, and SAFETY S-1 asks the
  worst case if the cancel failed EXECUTED through the real functions at the row's state. That path is vanilla's
  `hurtServer` and our own routing of a fireworks hit, which only a live server runs; it cannot be executed here. **So
  Route A's zero cannot be proven for every body: Route B, as the brief rules for that case.** No firework entity exists.
- **Route B as built** (`solar_flare_burst.yml`, every number a SEAT PLACEHOLDER): an ORANGE ball (90 TRAIL, `[255, 120,
  0]`) thrown out to the burst's own 6 blocks and a YELLOW one (60 TRAIL, `[255, 220, 40]`) to 4.5, both in 10 ticks; 40
  white FIREWORK sparks (spread 0.2, speed 0.3); `entity.firework_rocket.large_blast` (volume 2.0) and `.twinkle` (1.5),
  both keys READ in `SoundEvents` on the pinned jar. **The ball, READ, the client's `TrailParticle.tick`:** each tick a
  TRAIL moves `1 / (lifetime − age)` of the way to its target, so it flies a straight line at a steady speed and reaches
  its target on its last tick, then is removed (`age++ >= lifetime`). A TRAIL step's new `burst_radius` aims each of its
  `count` particles at one point of a sphere of that radius (`core.shape.SphereDirections.fibonacci`, an even lattice),
  so the ball grows to its radius and is gone. `burst_radius` is refused on any other particle, at 0, and beside a
  `spread` (a burst places every particle itself).

### N5, N6, N7

- **N5** was done on `feat/smolder-spiral` (the files were shared): Solar Flare names only its own files; its ring was
  `solar_flare_ring.yml` until this branch, and is now deleted and retired (the burst is instant and draws no ring).
- **N6:** the trace lines are unchanged in form; each shot line carries its own tick: C, C+10, C+20 on the one clock
  (`Bukkit.getCurrentTick()`, X6 (a)).
- **N7** (SEAT PLACEHOLDER 2026-11-28): "Fire three great fireballs, one after another." / "Each bursts within 6 blocks
  for 250 fire damage and scorches." (`SolarFlareContentTest` rebuilds both from the file.)

### CC'S CALLS (N1 to N7)

1. A salvo (the pulses' scheduling in the burst's run), not the volley: the volley re-rolls the crit per shot and its
   shots are no run's to end (READ above).
2. Shot 1 down the cast's aim (read at the press, one tick before it leaves); the alternative, a hop to read it at C.
3. The firework's look: two TRAIL balls (orange to 6, yellow to 4.5, 10 ticks), white sparks, the large blast and the
   twinkle.
4. `burst_radius` on a TRAIL step as the burst's look (the alternative, a new visual step type).
5. The ring visual deleted and retired rather than kept unused.

### Tests and mutations (N1 to N7)

`SolarFlareCastTest` rewritten for the new shape (11 rows, on the server's phases), `PulseFrontTest` (+1, N3),
`PulseCastTest` (the refusal row: −1 refused, 0 accepted), `SphereDirectionsTest` (1), `VisualLoaderTest` (+1 row, +3
refusals), `SolarFlareContentTest` (the fan row and the 15-block row replaced by the salvo and burst rows).

**The salvo's code was written before its rows**, so its mutations were run as a probe of rows written after it; each
reddened (marker 1, original 0, delta 1, restored byte-identical, markers left 0):

| mutation | the change | red |
|---|---|---|
| MUTSALVOTICK | each next shot due one tick early | four `SolarFlareCastTest` rows |
| MUTLOCKEDAIM | each later shot down the cast's aim | `eachShotLeavesDownHisAim…` |
| MUTCRITPERSHOT | a fresh `Caster` (and crit) per later shot | `theCritRolledAtTheCast…` |
| MUTSALVORUN | the run counts one shot, not three | six `SolarFlareCastTest` rows |
| MUTINSTANT | an instant front's radius 0 | `aTravelOfZero…`, `threeBurstsInOnePlace…`, `theCritRolled…` |
| MUTBURSTTARGET | the burst's targets at radius 1 | `aTrailBurstAimsEach…` |
| MUTSALVOSCHEMA | `salvo:` not parsed | three `SolarFlareContentTest` rows |
| MUTBURSTSPREAD | a burst beside a spread accepted | `trailsRefusalsAreNamedSkippedFiles` |

**Removed, not mutated:** a line that set the run's pending handle to −1 when a later shot fired; cancelling a link that
already ran does nothing, so no row could see it.

**Not unit-witnessed:** the presenter's burst loop in `PaperCombatWorld.particles` (a real world); the boot reads it.

### OPEN FOR BEN (N1 to N7)

- The firework's look (orange and yellow balls, white sparks, the sound), the description, the ask's wording.
- 250 per burst with three bursts in one place (750 is the normal case at a wall or a mob): he was told and did not say.
- Passive animals in the bursts and in the flight path (unchanged; asked twice, not answered).

**Only the boot can show:** the three fireballs leaving one after another, the firework as drawn, the sound.
