# GATE — the Plume body is a real vanilla arrow

**Status: R0 FAILED, 2026-09-23. R1-R8 NOT READ.** Every prediction was written BEFORE any boot
and no prediction is edited once a row has been read. Readings go in the `READ` cell beside the
prediction they answer, never over it.

```
FAIL      1   R0                        -- probe run 2026-09-23 by the review seat
NOT RUN   8   R1 R2 R3 R4 R5 R6 R7 R8   -- the deployed jar did not carry this slice
         ──
         9   = git grep -c '^### R' <ref> -- GATE-plume-vanilla-body.md
```

**GAME MODE: SURVIVAL**, for every row. **And it is not a formality here.** Creative
`hasInfiniteMaterials()` short-circuits `BowItem.use`'s ammunition check, so the draw starts without an
off-hand arrow and the arrow is never consumed — and **R7 is about an arrow entering the economy**, so
reading it in creative measures nothing.

---

## *** R0 FAILED. THE DEPLOYED JAR WAS THE SPIKE'S, NOT THIS SLICE'S. ***

**Ben booted on 2026-09-23 and reported *"Gates all look good."* That reading is honest and it is about
what he saw. It is not bound to `f44826f`, and the probe is what says so** — run after the fact, by the
review seat, because the reflog was ambiguous.

**THE MEASUREMENT, over `run/plugins/rpg-0.1.0-SNAPSHOT.jar` as it stands:**

```
509 class files scanned                       (non-zero, so the unpack worked)
onPlumeBodyHit    -> ABSENT                   the symbol THIS SLICE ADDS. Should be present.
setNoPhysics      -> 3 files                  the line THIS SLICE REMOVES. Should be 0.
plumebody         -> 1 file                   *** THE SPIKE'S COMMAND. Should not exist at all. ***
onPlumeBodyHitZZ  -> 0 files                  the control: the scan CAN return absent, so the
                                              readings above are not a blind instrument
```

**AND THE BOOT LOG IS THE STRONGER WITNESS, because it says what RAN rather than what is on disk:**

```
run/logs/latest.log
  [18:33:37] Starting minecraft server version 26.1.2        <- the only start in this log
  [18:36:12] [plume] A MARKER BODY dealt damage to TROPICAL_FISH -- setNoPhysics(true) DID NOT TAKE...
  [18:37:38] [plume] A MARKER BODY dealt damage to COW           -- setNoPhysics(true) DID NOT TAKE...
  [18:38:21] [plume] A MARKER BODY dealt damage to PLAYER        -- setNoPhysics(true) DID NOT TAKE...
  [18:38:23] [plume] A MARKER BODY dealt damage to SPIDER        -- setNoPhysics(true) DID NOT TAKE...
```

**That message text exists only in the PRE-SLICE code.** This slice rewrote it — it now names
`onPlumeBodyHit`'s cancel instead of a switch that no longer exists. **Four lines of the old wording
is four proofs that the old code was running.** The deployed jar's mtime is `18:33`, matching the
start; `git reflog` shows the working tree was moved to `spike/plume-body-modes` at `18:33:11`; and
the second worktree at `C:/Users/Neb91/IdeaProjects/rpg-12b` holds **no** deployed jar and **no**
boot log, so it is not the source either.

> ### AND THIS IS NOT THE ANVIL'S CASE, WHICH IS WHY IT IS RECORDED AS A FAILURE RATHER THAN A RULING
>
> `GATE-anvil.md` records a binding that **rests on Ben's statement because the probe was never run**,
> with circumstantial evidence that is *consistent* with it. **Here the probe WAS run and it
> CONTRADICTS the statement.** A ruling can settle what nobody measured; it cannot settle what a
> measurement refutes. **So there is no binding to record, and R1-R8 are NOT READ.**
>
> That file's own warning is what this is: *"a jar in the right place at the right time is exactly what
> a wrong-worktree build produced on 2026-09-20."* **The jar was in the right place, at a plausible
> time, and it was the wrong jar.**

### WHAT THE BOOT DID ESTABLISH, BECAUSE IT IS NOT NOTHING

- **PHYSICS WAS ON.** The four warnings require `stepMoveAndHit` to have found entities, which is gated
  on `!noPhysics`. **So the spike was in mode C, D or E** — not A and not B. (`PlumeBodyMode.current`
  initialises to `A` on every start, so Ben must have switched during the session.)
- ***AND IT MEASURED THAT THIS SLICE'S CANCEL IS NECESSARY.*** With physics on and no
  `onPlumeBodyHit`, the entity hit reached `AbstractArrow.onHitEntity` and was stopped only by the
  **backstop**, four times. `doKnockback` lives *inside* `onHitEntity` and runs **before** the damage
  event the backstop listens to — **so those four mobs were knocked back.** R5 predicts that must not
  happen, and the log is direct evidence that the late cancel is not sufficient. **The design is
  right; it just was not deployed.**
- **The LOOK rows are PLAUSIBLY fine and are still not read.** Spike mode E is, by construction, the
  shape this slice ships, so a session that ended in mode E would have shown the correct look. **That
  is an argument, not a reading**, and R1 is the sole witness for a line no unit test guards — which
  is exactly the row that cannot be settled by inference.

### WHAT IT COSTS AND WHAT FIXES IT

**Nothing merges.** `#144` stays open.

```
git checkout feat/plume-vanilla-body     # f44826f, or the rebased tip
./scripts/dev-server.sh                  # builds AND deploys, so the jar cannot be stale
```

then **R0 first**, and this time its fourth prediction — *state which tree the jar was built from* —
is the field that would have caught this in one line.

---
## WHAT THIS SLICE DID, AND WHAT IT IS ANSWERING FOR

`spawnBoltMarker` now calls **`world.spawnArrow`** with **no `setNoPhysics`** and **no
`setGravity(false)`**. `driveMarker` is unchanged — it still sets the velocity every tick. That is
spike **mode E**, which Ben read as *"perfect, exactly what we're looking for"*.

**`setNoPhysics(true)` CAUSED THE ORIGINAL DEFECT, and that is an attribution rather than an
inference:** spike modes B and E differ in that one line — both `spawnArrow`, both driven — and B read
*"still has the problem"*. The account is `PLAN-plume-vanilla-body.md`.

> ### *** THE ONE LINE THIS SLICE REMOVED HAS NO UNIT GUARD. THIS FILE IS ITS ONLY WITNESS. ***
>
> **Measured, not assumed.** `MUTNOPHYS` spliced `body.setNoPhysics(true);` back in — marker present 1,
> line delta against a pristine copy 1 — and the full suite stayed **GREEN at 1189 / 62 / 912 = 2163,
> zero failures**. **Nothing reddened.**
>
> **That is the finding, not a gap to be filled with a test.** Every consequence of that line is a
> rendered frame: a body that points the wrong way, a body that passes through a wall, a body that does
> not stick. **No unit test can see any of them**, and a source scan asserting *the line is absent*
> would only prove the source says what it was meant to say — which is precisely the trap the closed
> `#136` branch fell into with a green guard beside an inert fix.
>
> **SO R1's EAST ROW IS THE SOLE WITNESS FOR THE WHOLE SLICE.** If it is not read, nothing is verified.

---

### R0 — the deployed build carries this slice

**RUN THIS FIRST. IF IT FAILS, STOP.** A wrong jar does not announce itself; it produces readings, in
the right shape, at plausible values.

**PowerShell, because that is the gate shell** — `unzip` does not exist there and `java` on `PATH` is a
JRE shim with no `javap`.

| | |
|---|---|
| **Setup** | `Copy-Item run/plugins/rpg-<ver>.jar "$env:TEMP\deployed.zip" -Force`<br>`Expand-Archive "$env:TEMP\deployed.zip" -DestinationPath "$env:TEMP\deployed" -Force`<br>`$classes = Get-ChildItem "$env:TEMP\deployed\io\github\butterflysmp\rpg" -Recurse -Filter *.class`<br>`Write-Host "$($classes.Count) class files scanned"`<br>`$classes \| Select-String -Pattern 'onPlumeBodyHit' -Encoding ascii \| Select-Object -ExpandProperty Path`<br>`$classes \| Select-String -Pattern 'setNoPhysics' -Encoding ascii \| Select-Object -ExpandProperty Path`<br>`$classes \| Select-String -Pattern 'onPlumeBodyHitZZ' -Encoding ascii \| Select-Object -ExpandProperty Path` |
| **Predict** | **The class count is NON-ZERO and is reported.** It is the no-op value: `0 scanned` means the unpack failed, and an absence underneath a zero means nothing. Measured at build time: **505**. |
| **Predict** | **PRESENT: `onPlumeBodyHit` is found, in `RpgListeners.class`.** It is the symbol this slice adds and exists nowhere else in the project's history. **A 0 means the deployed jar predates this slice, whatever its mtime says.** |
| **Predict** | **ABSENT: `setNoPhysics` is found in NOTHING. Zero files.** It is the line the slice removes and the line the defect was. **A hit means it is back.** |
| **Predict** | ***AND THE THIRD PATTERN IS THE CONTROL, BECAUSE AN ABSENCE PROVES NOTHING UNLESS THE SCAN CAN PRODUCE ONE.*** `onPlumeBodyHitZZ` cannot be in any jar. It must return **0 files** — printing nothing. If it returns a hit, the scan is matching something other than what it is asked for and **the `setNoPhysics` zero above means nothing either.** <br><br>Measured at build time, all four together: `505 scanned`, `onPlumeBodyHit` in `RpgListeners.class`, `setNoPhysics` **0**, `onPlumeBodyHitZZ` **0**. |
| **Predict** | **`-Encoding ascii` is load-bearing**: a `.class` is binary and a text-mode read will not find a constant-pool string. |
| **Predict** | **STATE WHICH TREE THE JAR WAS BUILT FROM.** There is a second worktree at `C:/Users/Neb91/IdeaProjects/rpg-12b`, and that ambiguity cost a boot on 2026-09-20. |
| **READ** | ***FAIL -- 2026-09-23.*** The probe was run by the review seat after Ben reported *"Gates all look good"*, because `git reflog` was ambiguous about which branch the deployed jar was built from. **It is the SPIKE's jar.** 509 classes scanned; `onPlumeBodyHit` **ABSENT**; `setNoPhysics` in **3** files; `plumebody` in **1**; control needle `onPlumeBodyHitZZ` **0**, so the scan is not blind. <br><br>**`run/logs/latest.log` corroborates and is the stronger witness:** the only server start is `18:33:37`, and four `[plume] A MARKER BODY dealt damage` lines carry the **pre-slice wording** (*"setNoPhysics(true) DID NOT TAKE"*), which this slice rewrote. The jar's mtime is `18:33` and the reflog shows the tree moved to `spike/plume-body-modes` at `18:33:11`. <br><br>**The fourth prediction -- state which tree the jar was built from -- was NOT reported, and it is the one line that would have caught this before the session.** See the account above. |

---

### R1 — *** EAST: STRAIGHT FROM RELEASE AND STRAIGHT TO THE END. THE SOLE WITNESS. ***

**This row is the whole slice.** The mutation above proves no unit test can see what it sees.

| | |
|---|---|
| **Setup** | `/rpg give dragons_plume`. Plain arrow in the off-hand (the bow will not draw without one). Fire a **tap** (held 3–8 ticks) aimed **EAST**, along a long open sightline with nothing in range, and watch the bolt **all the way out** — not just the muzzle. |
| **Predict** | **It points along its travel from the frame it appears, and STAYS THERE for the whole flight.** No flick at the muzzle, no slow swing, no drift. **As in spike mode E.** |
| **Predict** | **EAST, AND THE DIRECTION IS NOT A CONVENIENCE.** The two defects this has passed through have opposite blind spots: the look-convention mirror leaves NORTH and SOUTH unaffected, and the `noPhysics` flip leaves EAST and WEST unaffected in the server's own target. East fails visibly under the first and is the direction every previous reading was staged on, so it is the continuity reading. |
| **Predict** | **IF IT SWINGS, `setNoPhysics` IS BACK OR THE JAR IS WRONG** — check R0 before anything else. Those are the only two ways this row fails that the mutation above has not already ruled out. |
| **READ** | _(NOT RUN)_ |

---

### R2 — SOUTH-EAST, the only staging that separates all three conventions

Yaw in degrees, for a velocity in each direction — computed, not eyeballed:

| shot | the projectile convention (correct) | `Location.setDirection` | the `noPhysics` flip |
|---|---|---|---|
| **south** | `0` | `0` | `180` |
| **east** | `90` | `270` | `90` |
| **south-east** | `45` | `315` | `225` |

| | |
|---|---|
| **Setup** | Same weapon and setup. Fire a tap aimed **SOUTH-EAST** — diagonally, roughly 45° between south and east — and watch it out. |
| **Predict** | **The bolt points SOUTH-EAST.** Not south-west (the look-convention mirror), not north-west (the `noPhysics` flip). **Three distinguishable answers and only one is a pass.** |
| **Predict** | **NAME WHICH WAY IT POINTED IF IT IS WRONG.** A bare *"wrong"* loses the row's whole value: the direction identifies the convention, and that is the only cheap diagnosis available. |
| **READ** | _(NOT RUN)_ |

---

### R3 — UP and DOWN, where the pitch error is largest

| | |
|---|---|
| **Setup** | Same weapon and setup. Fire a tap straight **UP**, then straight **DOWN** at the floor. Watch each for a full second. |
| **Predict** | **The arrow points UP, then DOWN, from the frame it appears and for the whole flight.** |
| **Predict** | **BOTH, NOT ONE.** A sign error in the pitch reaches one and not the other, and the two readings are one shot apart. |
| **Predict** | **The DOWN shot will meet the floor**, so read its orientation in the air and expect R6's behaviour when it lands. Do not read the two rows as one. |
| **READ** | _(NOT RUN)_ |

---

### R4 — SOUTH, and it is a real control this time

**R1's ancestor had a south shot that could not fail**, because south is the **no-op value of the
look-convention mirror**: yaw `0` under both conventions, so a south bolt read as a clean pass with
that defect fully present.

| | |
|---|---|
| **Setup** | Same weapon and setup. Fire a tap aimed **SOUTH** and watch it out. |
| **Predict** | **It points SOUTH for the whole flight.** |
| **Predict** | **ITS JOB IS TO PROVE THE ROTATION IS APPLIED AT ALL, and only that.** If south looks right and R1's east does not, the rotation is landing in the **wrong convention** rather than not landing — two different repairs, and this pair is the only cheap way to tell them apart. |
| **Predict** | **IT IS NOT A HOLLOW ROW AND ITS ANCESTOR WAS.** Stated because the two look identical in a log: *"fired south, looked right"* meant nothing on the closed branch and means something here. |
| **READ** | _(NOT RUN)_ |

---

### R5 — *** A BOLT FIRED THROUGH A MOB: ONE DAMAGE, NO KNOCKBACK, AND A ONE-TICK HITCH ***

**This is the row for the mechanism the slice adds.** `onPlumeBodyHit` cancels the `ProjectileHitEvent`
for a tagged body, and cancelling is measured to be enough: `Projectile.preHitTargetOrDeflectSelf`
reads its own `hitCancelled` flag and **never calls `hitTargetOrDeflectSelf`**, which is where
`hurtOrSimulate`, `doKnockback` and `discard` all live.

| | |
|---|---|
| **Setup** | Stand a mob — a zombie or a cow — about 15 blocks away on open ground. Fire a **charged release** straight **THROUGH** it, so the bolt's path continues past it. Watch the mob's health, the mob's feet, and the arrow. |
| **Predict** | **The mob takes core's damage EXACTLY ONCE.** One damage number, one health drop. A second, smaller, uncoloured hit would be vanilla's arrow damage arriving — which means the cancel did not take. |
| **Predict** | **NO KNOCKBACK FROM THE ARROW.** The mob does not lurch. `doKnockback` lives inside `onHitEntity` and the cancel is upstream of it. <br><br>A `weapon_damage` effect can author its own knockback; **the Plume does not** — it is a `type: projectile` weapon and the standing rule is that a travelling ranged weapon MAY author `EffectSpec.Knockback` in its `on_hit`, and this one does not. So any push at all is the arrow's. |
| **Predict** | **THE ARROW DOES NOT STOP IN THE MOB. It continues past.** No arrow sticking out of the mob, no arrow vanishing on contact. |
| **Predict** | ***BUT IT WILL HITCH FOR ONE TICK, AND THAT IS PREDICTED RATHER THAN HOPED AWAY.*** `stepMoveAndHit` calls `setPos(firstHit.getLocation())` **BEFORE** the event is raised, so the body is clamped to the mob's surface for that tick whatever the cancel then does. At the Plume's `speed 2.5` that is up to **2.5 blocks of lost advance in one tick**, and `driveMarker` flies it on from there. <br><br>**SAY WHETHER THE HITCH IS VISIBLE.** It is the one cost of this design that no API can remove — `Projectile.canHitEntity` is a query with no setter anywhere on the API. If it reads as an arrow stalling on the mob, that is a finding and not a failure of the cancel. |
| **Predict** | **`[plume] A MARKER BODY dealt damage` MUST NOT APPEAR IN THE LOG.** That is `onPlumeBodyDamage`, the backstop. It now guards a LIVE path rather than an impossible one, so its firing means the cancel did not take — check that `onPlumeBodyHit` registered. |
| **READ** | _(NOT RUN)_ |

---

### R6 — A bolt into a wall: the body sticks, and the armed lifetime removes it

**Read from the pinned jar before choosing, rather than reaching for a poll.** When a driven arrow
sticks, `AbstractArrow.tick` takes the in-ground branch, calls `tickDespawn()` and **returns early** —
before the move, before the rotation, before the inertia. `tickDespawn` is
`life++; if (life >= <config despawn rate>) discard(DESPAWN)`, and `life` is armed at
`Integer.MAX_VALUE - 1`, so **the first in-ground tick discards it whatever that config says.** And
driving does not un-stick it: `CraftEntity.setVelocity` is a plain `setDeltaMovement` plus
`hurtMarked`, and touches neither `inGround` nor `life`.

| | |
|---|---|
| **Setup** | Fire a **tap** at a wall about 10 blocks away. Watch where the bolt meets the block. |
| **Predict** | **The body does not survive in the wall.** It is gone within about a tick of touching the block — one frame of an arrow in the stone at most. |
| **Predict** | **NO ARROW ACCUMULATES.** Fire ten taps at the same wall. **Zero arrows stuck in it afterwards.** A row that fires once cannot see a leak, and a leak is what a missing cleanup looks like. |
| **Predict** | **NO CLEANUP CODE WAS ADDED, ON PURPOSE.** The armed lifetime already does it. If arrows DO accumulate, the read above is wrong and a poll is owed — **say which, because that is the difference between a design that was measured and one that was assumed.** |
| **Predict** | **The trail and the damage are unaffected.** `castRay` resolves on the block independently; the body sticking is a visual event, not a resolution. |
| **READ** | _(NOT RUN)_ |

---

### R7 — A stuck body cannot be picked up. *** AND THIS GUARD JUST WENT LIVE ***

**`playerTouch`'s guard is `isInGround() OR isNoPhysics()`.** Under the old switch the SECOND disjunct
was always true, so the body was pickable in MID-AIR and `setPickupStatus(DISALLOWED)` was what refused
it. **Now the second disjunct is always false and the FIRST goes live the instant the body sticks.**
The hazard moved from mid-air to in-ground; it did not go away.

| | |
|---|---|
| **Setup** | **SURVIVAL.** Fire taps at a wall a few blocks away and **walk into the impact point repeatedly**, immediately, while bolts are still landing. Then check the inventory. |
| **Predict** | **NO ARROW ENTERS THE INVENTORY.** Not one, over ten shots. |
| **Predict** | **`[plume] PlayerPickupArrowEvent fired for a MARKER BODY` MUST NOT APPEAR.** That handler is the loud detector behind `DISALLOWED`; if it fires, the pickup status did not take and a free arrow was one call from the economy. |
| **Predict** | **THE WINDOW IS ABOUT ONE TICK WIDE, WHICH MAKES THIS ROW HARD TO FAIL BY ACCIDENT — SO FIRE IT MANY TIMES.** R6 removes the body on its first in-ground tick, so the pickable window is tiny. **A single shot that mints nothing is not evidence.** This is the hollow-fixture rule: ask what the row does if `DISALLOWED` is deleted, and if one shot would still pass, one shot is measuring the fixture. |
| **READ** | _(NOT RUN)_ |

---

### R8 — The Plume's four casts, because the body is not one weapon's

Every `body: arrow` cast in the tree goes through `spawnBoltMarker`. `dragons_plume.yml` authors
**four**: the charged release and three tap bands.

| | |
|---|---|
| **Setup** | Fire each of the four: a **full charge**, and a **tap** in each of the three bands (roughly 3–8, 9–15 and 16+ ticks held — see the weapon's `draw` block for the authored boundaries). Aim **EAST** each time. |
| **Predict** | **All four look the same: straight from release, straight to the end.** |
| **Predict** | **FOUR READINGS, NOT ONE.** The bands differ in speed and damage, and a defect that scales with speed would show in one band and not another. A single "the Plume looks right" does not answer this row. |
| **Predict** | **AND SLICE 14 IS NOT READ HERE.** `dragons_breath.yml`'s seven bodies per press are on `feat/14-scatter-shot`, which rebases onto this slice after it merges. Seven bodies is a louder version of the same frame, not a separate question — **do not read it on this branch.** |
| **READ** | _(NOT RUN)_ |

---

## WHAT THIS GATE CANNOT SEE

- **Whether the one-tick hitch in R5 matters to a player.** The row asks for a judgement, and a
  judgement is what it gets. There is no API that removes it.
- **The path divergence, because there is none to see.** A driven arrow steps by exactly core's
  velocity — the move in `AbstractArrow.tick` happens before `applyInertia(0.99f)` touches the delta —
  so the body is on the resolved path by construction, at 0.000 blocks. **That is arithmetic, not a
  boot reading**, and no row here can confirm or refute it.
- **Anything about `core/`.** This slice changes none of it: no drag, no re-ruled reach, none of the ten
  projectile casts across seven content files.
- **What the client does between rotation packets.** `EntityType.ARROW` carries `updateInterval(20)`, so
  the client fills in most frames from its own copy of the arrow tick. Every row above is a LOOK for
  exactly that reason.
