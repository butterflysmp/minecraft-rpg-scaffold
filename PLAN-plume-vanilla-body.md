# PLAN — the Plume body becomes a real vanilla arrow

**SCOPED, NOT CODED.** This file carries the operator's ruling, the findings rescued from `#136`
before it was closed unmerged, the measurements that decide the design, and a recommendation.
**Nothing here has been implemented.**

---

## THE RULING — Ben, 2026-09-23

> **SPIKE MODE D IS THE LOOK.** The Plume body is a real vanilla arrow: spawned with
> `world.spawnArrow`, **physics ON**, **gravity ON**, and **NOT driven per tick**.

**THIS REVERSES SLICE K (`#90`)'s INERT-BODY DESIGN.** That slice's whole argument was that the body
is decoration and must be unable to affect anything: `setNoPhysics(true)` as *"THE ONE SWITCH"*, no
collision, no hit event, no in-ground sticking, and a velocity driven by us every tick. **The ruling
keeps the visual requirement and discards the mechanism**, so every guarantee that switch bought has
to be re-established by other means or given up deliberately. The list is in *WHAT OPTION (1) COSTS*
below.

### WHAT WAS AND WAS NOT REPORTED, STATED BECAUSE THE DIFFERENCE IS THE WHOLE VALUE OF THE SPIKE

| mode | shape | reading |
|---|---|---|
| **A** | `world.spawn` + `setNoPhysics(true)` + `setRotation` + driven | **NOT REPORTED** |
| **B** | `world.spawnArrow` + `setNoPhysics(true)` + driven | **NOT REPORTED** |
| **C** | `world.spawnArrow`, physics ON, gravity OFF, not driven | **NOT REPORTED** |
| **D** | `world.spawnArrow`, physics ON, gravity ON, not driven | **THE LOOK** — operator's ruling |

> ***A, B AND C ARE NOT READINGS AND MUST NOT BE INFERRED FROM D.*** D being the look does not make
> A wrong, B wrong or C wrong; it makes them **unread**. The spike's own decision table needed the
> pair *(A wrong, C/D right)* to conclude that `noPhysics` is the cause, and **that pair was never
> completed.** So the cause of the original defect remains **unattributed** — what is settled is
> which shape Ben wants, which is a different question and the one that matters for shipping.
>
> `spike/plume-body-modes` (`f8274e9`) is kept until this slice lands. **It is the evidence**, and it
> is the only place A, B and C can still be read if anyone wants the attribution.

---

## FINDINGS RESCUED FROM `#136`, WHICH IS CLOSED UNMERGED

**`#136`'s branch `fix/arrow-body-orientation` failed its boot gate twice and is not merging.** These
five findings cost two boots and a bytecode pass; they are recorded here because the branch is no
longer their home. **Measured from `run/versions/26.1.2/paper-26.1.2.jar`** (manifest
`Build-Number: 74` — the `paper.version` `pom.xml` pins and the build the boot logs name) unless
stated otherwise.

1. ***`7b5b936` WAS INERT: `world.spawn` DISCARDS A `Location`'s ROTATION FOR AN ARROW.***
   `CraftEntityTypes` registers `EntityType.ARROW` through `createAndMoveEmptyRot`, whose positioner
   is `MOVE_EMPTY_ROT` — `Entity.snapTo(x, y, z, 0.0F, 0.0F)`, **both floats constant**. A sibling
   positioner `MOVE` does read `SpawnData.yaw()`/`pitch()`; `ARROW` is not registered with it.
   `ENDER_PEARL`, `EXPERIENCE_BOTTLE` and `SPECTRAL_ARROW` share the arrow's rotation-discarding one.
   **So a fix that wrote the rotation onto the `Location` changed no rendered frame at all**, and it
   read as working.

2. ***THE `noPhysics` YAW FLIP.*** `AbstractArrow.tick` computes `flag = !isNoPhysics()` once and
   gates the yaw target on it: `flag ? atan2(x, z) : atan2(-x, -z)`. **A `noPhysics` arrow's yaw eases
   toward 180° from its travel**, at `Mth.lerp(0.2f, ..)` a tick. The pitch is not negated. The branch
   is keyed on `noPhysics` **alone** — no `isInGround`, no `shakeTime`, no water test — and because the
   in-ground branch is itself `isInGround() && flag`, a `noPhysics` body reaches the negated branch
   **every tick it is alive**.
   **AND THE EASING IS UNCONDITIONAL:** `xRotO`/`yRotO` do not appear in `AbstractArrow` at all, so
   there is **no first-tick snap branch**. The pre-fix body eased out of due south over roughly ten
   ticks rather than flicking once.

3. ***THE ADD-ENTITY PACKET DOES CARRY OUR ROTATION, AND `needsSync` IS THE ONE THING `shoot` ADDS.***
   `Projectile.getAddEntityPacket(ServerEntity)` builds
   `ClientboundAddEntityPacket(entity, serverEntity, ownerId)`, which takes yaw, pitch, head-yaw
   **and velocity** from `ServerEntity.getLastSent*()` — and `ServerEntity`'s **constructor**
   initialises all four from the live entity (`Mth.packDegrees(entity.getYRot())`,
   `entity.getDeltaMovement()`). It is constructed when the entity joins the world, and a
   `world.spawn` consumer runs **before** `addEntityToWorld`. **So the spawn frame was correct on
   `f5179bb` and the spawn frame was never the defect.**
   Separately, `ServerEntity.sendChanges` opens its position-and-rotation gate on
   `forceStateResync || tickCount % updateInterval == 0 || needsSync || entityData.isDirty()`;
   **`EntityType.ARROW` is built with `updateInterval(20)`**, and `needsSync` is set by `setPosRaw`
   only for types whose interval is `Integer.MAX_VALUE`, so **movement does not set it.** The only act
   of `AbstractArrow.shoot` that our own spawn path did not already perform is **`needsSync = true`**.
   **And gravity-off is synced** — `setNoGravity` writes `DATA_NO_GRAVITY` into `SynchedEntityData` —
   so client/server divergence by gravity is ruled out.

4. ***BEN'S TWO READINGS, WHICH NO AMOUNT OF SERVER-SIDE READING COULD HAVE REPLACED.***
   - On `7b5b936` (the inert build): the bolt **"took a few seconds but did correct itself
     eventually"**. Had the server's flipped yaw reached the client, it would have settled pointing
     BACKWARDS and stayed there. **It settled correct**, so the client converges on the travel
     direction from its own copy of the arrow tick.
   - On `f5179bb` (rotation written to the entity at spawn, per-tick write removed): **still wrongly
     oriented in the early part of flight.**

5. ***THE SPIKE'S RULING*** — mode D, above.

> **THE BOOT SHAS ARE FROM BEFORE A REBASE AND ARE UNREACHABLE NOW.** `7b5b936` and `f5179bb` are the
> SHAs Ben booted, and the readings stay bound to them; after the rebase onto `d125507` their content
> lives at `90b2037` and `f5179bb` respectively (`f5179bb` was created after the rebase, so it is
> still reachable while the branch exists). **`fix/arrow-body-orientation` is left on the wire rather
> than deleted**, because deleting it is irreversible and nobody asked for it.
>
> **The sixth finding went to `master` on its own, as `#142`/`deb6494`:** *a comment that names a
> hazard reads as a guard against it*. It is a rule rather than a fact about arrows, which is why it
> shipped separately and why it unblocks the `CLAUDE.md` naming pass.

---

## *** THE MEASUREMENT THAT DECIDES THE DESIGN: THE TWO PATHS DO NOT AGREE ***

**A vanilla arrow has drag. `ProjectileFlight` does not model it.**

| | value | where it was read |
|---|---|---|
| vanilla arrow **drag**, in air | **`×0.99` per tick**, applied to the whole velocity | `AbstractArrow.tick` → `applyInertia(0.99f)`, and `applyInertia` is `setDeltaMovement(getDeltaMovement().scale(f))`. Gated on `!isInWater()` |
| vanilla arrow drag, **in water** | `×0.6` per tick | `getWaterInertia()` |
| vanilla arrow **gravity** | **`0.05`** | `AbstractArrow.getDefaultGravity()` |
| core's velocity update | `velocity.add(new Vec3(0, -gravity, 0))` — **gravity only, NO drag** | `ProjectileFlight`'s `nextVelocity`, the single velocity update in the file |
| the Plume's authored numbers | `speed 2.5`, `gravity 0.05`, `max_lifetime_ticks 120` | `dragons_plume.yml` |

**Both models move-then-update in the same order** (`pos += v`, then update `v`), so the *only*
difference is the missing `×0.99`.

### THE DIVERGENCE, COMPUTED RATHER THAN ESTIMATED

Both models stepped tick by tick from `speed 2.5` fired horizontally, `gravity 0.05`, drag `0.99` on
the vanilla side only. Distances in blocks; `dx` is along-track, `dy` vertical.

| core has travelled | tick | core position | vanilla position | **apart** |
|---|---|---|---|---|
| **10 blocks** | 4 | `(10.00, −0.30)` | `(9.85, −0.30)` | **0.149** |
| **20 blocks** | 8 | `(20.00, −1.40)` | `(19.31, −1.37)` | **0.687** |
| **40 blocks** | 16 | `(40.00, −6.00)` | `(37.14, −5.73)` | **2.877** |

and over the Plume's full authored lifetime:

```
t= 20   core (50.00, -9.50)     vanilla (45.52, -8.95)     apart   4.510
t= 40   core (100.00, -39.00)   vanilla (82.76, -34.49)    apart  17.824
t= 60   core (150.00, -88.50)   vanilla (113.21, -73.58)   apart  39.700
t=120   core (300.00, -357.00)  vanilla (175.15, -249.69)  apart 164.626
```

**The divergence is almost entirely ALONG-TRACK.** At 40 blocks it is `dx 2.864` against `dy 0.271` —
**the body lags behind the resolved point on the same line rather than leaving it.** A lag reads very
differently from a lateral miss: the arrow is drawn *behind* where the hit resolves, so at 40 blocks a
mob takes damage about 2.9 blocks — five arrow-lengths — before the arrow reaches it.

### THE TOLERANCE, AND THE HONEST ANSWER IS AWKWARD

**The hitbox lenience core uses for a bolt's ray is `RAY_SIZE = 0.0`** — `PaperCombatWorld`'s own
comment: *"How much to inflate entity hitboxes when tracing. 0 = exact bounding box."* **So there is
no lenience on this path at all**, and on a strict reading *any* divergence is outside it.

**The only lenience figure in the tree is `TARGET_LENIENCE = 0.3`**, and it belongs to
`RpgCommand`'s targeting helper — **a different path, not the bolt's.** Using `0.3` as the working
tolerance for a *visual* body is defensible (it is about half a player hitbox's width, so it is
roughly where a mismatch becomes visible) but it is a **BORROWED FIGURE, NOT A MEASURED ONE**, and it
is named as such rather than presented beside the measured numbers as though it were one.

**Against `0.3`, measured: the two paths cross it at tick 6, with the bolt 15 blocks out.**

> **SO THE ANSWER TO BEN'S CONDITIONAL IS YES: IT DIVERGES BEYOND TOLERANCE, AND OPTION (1) NEEDS
> CORE TO ADOPT VANILLA'S DRAG.**

### WHAT ADOPTING DRAG IN CORE CHANGES — AND IT IS A RULED NUMBER

`ProjectileFlight`'s `nextVelocity` would become `velocity.scale(0.99).add(0, -gravity, 0)`. One line.
**Its consequences are not one line:**

- ***THE PLUME'S REACH IS A RULING AND IT BREAKS.*** `dragons_plume.yml` records *"R11 ruled the reach
  at ~300 blocks: speed 2.5 over max_lifetime_ticks 120"*. With drag, 120 ticks gives **175 blocks**
  horizontally, not 300. **That is Ben's ruling to revisit, not a number to quietly correct.**
- **`ProjectileFlight`'s own derived figure goes stale.** Its javadoc states *"a flat stray lands about
  22.5 blocks"* — a no-drag derivation. With drag it is nearer 19 blocks.
- **It reaches TEN projectile casts across SEVEN content files**, not just the Plume:
  `dragons_plume.yml`, `emberblade.yml`, `ember_staff.yml`, `flint_staff.yml`, `hunters_bow.yml`,
  `volley_stone.yml` and `abilities/solar_grenade.yml`. **Every authored `speed` in the tree is a
  reach that changes.**
- **Homing interacts with it.** `steer` scales by `velocity.length()` *"the speed the bolt actually
  has, gravity included"* — with drag that speed now decays, so a homing bolt slows as it chases.
  Whether that is desirable is a design question nobody has been asked.

---

## THE TWO OPTIONS

### Option (1) — core keeps hit authority; the real arrow is visual

**The new code, enumerated:**

| item | what it needs | notes |
|---|---|---|
| **the entity-hit cancel** | a **new** `ProjectileHitEvent` listener cancelling for PDC-tagged bodies | **There is no such listener today** — the only marker listeners are `onPlumeBodyPickup` (`PlayerPickupArrowEvent`) and `onPlumeBodyDamage` (`EntityDamageByEntityEvent`). And the event's own javadoc says cancelling **"does NOT prevent block collisions"**, so it buys the entity half only. |
| **block-stick behaviour** | **no API exists.** `isInBlock()`/`getAttachedBlock()` are queries; nothing sets `inGround`. A tagged body must be **removed** when it sticks — polled, or off `ProjectileHitEvent` on a block. | Until it is removed, the visual has **stopped** while core's ray flies on. A bolt fired at terrain stops at the wall and the hit resolves past it. |
| **cleanup at core's resolved hit** | **already exists and needs nothing.** `ProjectileFlight.resolve` calls `world.removeMarker(markerId)` on **both** exits — a ray hit and the fuse running out. | The gap is the reverse case: a body that stopped early stays drawn until core resolves. |
| **`onPlumeBodyDamage`** | **it stops being a detector and becomes the fix.** Its javadoc's premise is *"With `setNoPhysics(true)` there is no route to it at all … So if this fires, the switch did not take"*. Under (1) the route is open by design, so it must cancel **silently** — and the loud detector is lost. | Same for the pickup guard's reasoning: `playerTouch`'s `isInGround() \|\| isNoPhysics()` disjunction is no longer satisfied in mid-air, so **`setPickupStatus(DISALLOWED)` stops being load-bearing in flight** and starts mattering only once a body sticks. |
| **drag in core** | required, per the measurement above, with the ruled-reach consequence | |

### Option (2) — the vanilla arrow is the authority, as in the old project

The arrow's own hit resolves the damage; core stops tracing. This is what `cfde822` did.

**It is much larger than it looks**, and the reasons are in this repo rather than in the old one:
`castRay` owns every hit in this engine, `applyDamage` never calls `entity.damage()`, and the whole
custom health store, element accrual, crit roll and damage-popup path hangs off core's resolution.
**Handing authority to the entity means re-entering all of that from a Bukkit event**, and it changes
what a `body: arrow` weapon *is* — every non-arrow body (`spawnMarker`'s items) would still resolve
through core, so the engine would have two damage paths keyed on the body kind.

## RECOMMENDATION: OPTION (1), WITH DRAG ADOPTED IN CORE AND THE REACH RE-RULED

**Because the divergence is along-track rather than lateral, and because (2) forks the damage path.**

- (1) keeps one hit authority, which is the invariant everything else in `core/` is built on.
- The drag line is small and its cost is **a ruling, not a rewrite** — Ben re-rules the reach, and the
  authored `max_lifetime_ticks` absorbs it.
- (2)'s cost is structural and permanent; (1)'s costs are a listener, a stick-cleanup, and a lost
  detector.

**THE ONE THING THAT WOULD CHANGE THIS RECOMMENDATION** is if Ben wants the arrow to *behave* like a
vanilla arrow as well as look like one — sticking in walls, being blocked by cover it should be
blocked by. That is (2) by another name and it is a gameplay ruling, not a design preference.

## WHAT GOES, AND WHAT IT IS SAFE TO NOT CARRY

- ***`BodyRotation` HAS NO READER UNDER THE RULING, AND MUST NOT BE CARRIED.*** `spawnArrow` calls
  `AbstractArrow.shoot`, which computes and writes `setYRot(atan2(x, z))` / `setXRot(atan2(y, h))`
  itself from the direction — **the same convention `BodyRotation` was written to produce.** Measured
  on `spike/plume-body-modes`: its only readers are the mode-A spawn write (deleted by the ruling) and
  `spikeSpawn`'s `Location` rotation, **which `shoot` overwrites and which is already decorative**.
  So `BodyRotation.java` and `BodyRotationTest.java` (11 rows) are **new code that the ruling makes
  dead before it ships**. Do not port them.
  > **The measurement inside them survives their deletion, and that is what this section is for:** the
  > projectile convention is `yaw = toDegrees(atan2(x, z))`, `pitch = toDegrees(atan2(y, h))`, its
  > witness is `Projectile.shoot`, and `Location.setDirection` writes a **different** convention
  > (`atan2(-x, z)`, `atan(-y/h)`) whose no-op directions are **north and south**.
- **`BoltBodySignatureTest` (7 rows) pins the shape the ruling deletes** — `world.spawn(toLocation(at),
  Arrow.class`, the `setRotation` call, the `setNoPhysics(true)` premise, and the absence of a
  per-tick write. **Every row is about to be false.** It is a source scan, and the lesson recorded in
  its own javadoc applies to its replacement: a source scan can only ever prove the source. **Rewrite
  it to pin the `spawnArrow` call and the deliberate ABSENCE of `setNoPhysics`, or delete it and say
  so** — the second is defensible now that the body is a plain vanilla arrow with nothing unusual to
  protect.

## EVERY CALLER, WALKED

| caller | bodies per press | notes |
|---|---|---|
| `dragons_plume.yml` charged release | 1 | `body: arrow`, `speed 2.5`, `gravity 0.05` |
| `dragons_plume.yml` tap bands × 3 | 1 each | same numbers |
| `dragons_breath.yml` (slice 14, `feat/14-scatter-shot`) | **7** | `spread: count: 7, angle_degrees: 3`; each body is its own `launch(...)` and therefore its own `spawnBoltMarker`, so it becomes **seven `spawnArrow` calls and seven physics arrows per press** |

> **SLICE 14 IS THE LOUD CASE FOR EVERY COST IN OPTION (1).** Seven physics arrows per press means
> seven chances to stick in a wall, seven `ProjectileHitEvent`s to cancel, and seven bodies to clean
> up. Its own content notes that *"seven arrows reach one target only inside about six blocks"* — so
> its spread is a close-range mechanic, which is exactly the range at which the path divergence is
> still under `0.3` blocks. **The two slices should be sequenced deliberately**, and `#135` is open.

## WHAT THIS PLAN DOES NOT ANSWER

- **Why A, B and C look the way they look.** Unread; see the ruling section.
- **What the client actually draws, in any mode.** Nothing server-side settles it, and this plan does
  not pretend otherwise. The boot rows are the instrument.
- **Whether the reach re-ruling changes the Plume's balance.** That is Ben's, and it is the gate on
  adopting drag.
