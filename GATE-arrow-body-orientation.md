# GATE — The arrow body's first frame

**Status: R1 and R2 FAILED, read 2026-09-22 on `7b5b936`, and CLOSED. R0 and R3-R7 NOT RUN.** Every prediction was
written BEFORE any boot and no prediction is edited once a row has been read. Readings go in the
`READ` cell beside the prediction they answer.

```
FAIL      2   R1 R2                 -- read 2026-09-22, bound to 7b5b936, CLOSED
NOT RUN   6   R0 R3 R4 R5 R6 R7     -- written before any boot of the new tip
         ──
         8   = git grep -c '^### R' <ref> -- GATE-arrow-body-orientation.md
```

> **THE BOOT SHA WAS REWRITTEN BY A REBASE, 2026-09-23.** R1 and R2 were read on **`7b5b936`**,
> which is the SHA Ben booted, and that is what the readings stay bound to. The branch was then
> rebased onto `d125507`, so that commit lives at **`90b2037`** now and `7b5b936` is unreachable
> on the branch. **The reading keeps the SHA that was actually booted** -- renaming it to
> `90b2037` would claim a boot that never happened -- and the mapping is recorded here so the
> tree can still be found.

**GAME MODE: SURVIVAL**, for every row. Nothing here is a cost, so no creative divergence applies —
declared anyway, per the standing debt in `CLAUDE.md`.

---

## WHAT THIS FIXES, AND WHY IT IS NOT SLICE 14'S

**A `Vec3` carries no rotation.** `toLocation` builds a `Location` from three doubles, so its yaw
and pitch are the constructor's defaults — **0 and 0, which is due south and level.** An arrow
derives its own rotation from `atan2` over `deltaMovement` **inside its own `tick()`**, so the
velocity set at creation is right one tick BEFORE the rotation derived from it is. **The frame in
between renders due south, whichever way the bolt is actually travelling.**

> **TWO WORDS IN THAT PARAGRAPH DID NOT SURVIVE THE POST-BOOT READ — *"the frame"*. 2026-09-22.**
> The diagnosis holds: a `Vec3` carries no rotation, and the body does spawn due south and level.
> **What is refuted is that it is ONE frame.** There is no first-tick snap to be one frame long:
> `xRotO` and `yRotO` do not appear in `AbstractArrow` at all, and the easing is **unconditional**
> — `lerpRotation(getYRot(), target)`, which is `Mth.lerp(0.2f, ..)` after normalising. A pre-fix
> body **eased** out of due south over roughly ten ticks instead of flicking once.
>
> **SO THIS PARAGRAPH UNDERSTATES WHAT SHIPPED**, and it is left standing rather than reworded
> because the understatement is the thing worth seeing: the mechanism was read correctly and the
> DURATION was assumed. Account: *WHY BOTH ROWS FAILED* below.

**It shipped with the arrow body and affects every `body: arrow` cast in the tree** — `dragons_plume`'s
charged release and its three tap bands. It was observed on the Plume, which is on `master` and
predates slice 14; slice 14 touched neither `spawnBoltMarker` nor `ProjectileFlight`, measured:

```
git diff origin/master...feat/14-scatter-shot --numstat -- <PaperCombatWorld> <ProjectileFlight>
  -> PaperCombatWorld changed in aimOf ONLY; ProjectileFlight not changed at all
```

**So this is a master defect that slice 14 merely gave a seventh reason to look at**, and it is
branched and reviewed on its own.

---

### R0 — The deployed build carries this fix

**RUN THIS FIRST. IF IT FAILS, STOP.** A wrong jar does not announce itself; it produces readings,
in the right shape, at plausible values.

**PowerShell, because that is the gate shell** — `unzip` does not exist there and `java` on `PATH`
is a JRE shim with no `javap`.

| | |
|---|---|
| **Setup** | `Copy-Item run/plugins/rpg-<ver>.jar "$env:TEMP\deployed.zip" -Force`<br>`Expand-Archive "$env:TEMP\deployed.zip" -DestinationPath "$env:TEMP\deployed" -Force`<br>`$classes = Get-ChildItem "$env:TEMP\deployed\io\github\butterflysmp\rpg" -Recurse -Filter *.class`<br>`Write-Host "$($classes.Count) class files scanned"`<br>`$classes \| Select-String -Pattern 'setDirection' -Encoding ascii \| Select-Object -ExpandProperty Path`<br>`$classes \| Select-String -Pattern 'BodyRotation' -Encoding ascii \| Select-Object -ExpandProperty Path` |
| **Predict** | **The class count is NON-ZERO and is reported.** It is the no-op value: `0 scanned` means the unpack failed, and an absence underneath a zero means nothing. |
| **Predict** | *** THE NEEDLES ARE INVERTED FROM THIS ROW'S FIRST VERSION, AND R0 HAS NEVER BEEN READ. *** The first version predicted `setDirection` PRESENT, because the fix then routed the rotation through `Location.setDirection`. That fix was measured INERT and is deleted, so the needle it named is now the ABSENT one. **Rewriting a prediction is legal here only because this row carries no reading** -- see R1 and R2, which do, and are closed rather than re-pointed. |
| **Predict** | **`setDirection` is found in NOTHING: zero hits across every scanned class.** A hit means the deleted helper is back, or the jar predates this slice. |
| **Predict** | **`BodyRotation` is found in `PaperCombatWorld.class`** -- the PRESENT needle, and a symbol that exists only because of this slice. A `BodyRotation.class` of its own is in the jar too, under `io/github/butterflysmp/rpg/core/combat/`, and the scan above walks `io/github/butterflysmp/rpg`, so it sees both. **Zero hits means the deployed jar predates this slice, whatever its mtime says.** |
| **Predict** | `-Encoding ascii` is load-bearing: a `.class` is binary and a text-mode read will not find a constant-pool string. |
| **Predict** | **STATE WHICH TREE THE JAR WAS BUILT FROM.** There is a second worktree at `C:/Users/Neb91/IdeaProjects/rpg-12b`, and that ambiguity cost a boot on 2026-09-20. |
| **READ** | _(NOT RUN)_ |

---

### R1 — *** A PLUME ARROW LOOKS FIRED FROM A BOW ON THE FRAME IT APPEARS ***

**This is the whole fix, and the reading is a LOOK rather than a number.**

| | |
|---|---|
| **Setup** | `/rpg give dragons_plume`. Put a plain arrow in the off-hand (R5's workaround — the bow will not draw without one). Fire a **tap** (held 3–8 ticks) while aimed roughly **EAST or WEST**, and watch the arrow at the muzzle. |
| **Predict** | **The arrow points along its travel from the instant it appears.** No visible flick, snap or quarter-turn in the first moments of flight. |
| **Predict** | **EAST or WEST, AND THAT IS THE WHOLE STAGING — SOUTH IS THE NO-OP VALUE OF THIS ROW.** The old rotation was yaw 0 = **due SOUTH**, so a shot fired south is **CORRECT UNDER THE DEFECT** and reads as a clean pass. <br><br>**This is the hollow-fixture rule, and the direction is not a convenience.** Ask what this row does if the fix is reverted: facing east it fails, facing south it passes, and *the row is the same row*. A staging that cannot fail is measuring the fixture. <br><br>**DO NOT "SIMPLIFY" THIS TO WHATEVER DIRECTION YOU HAPPEN TO BE FACING.** It reads like an arbitrary compass bearing and it is the only thing making the row falsifiable. |
| **Predict** | **CONTROL, in the same session:** fire once facing **SOUTH** as well. It looked right before the fix and must still look right after — if the south shot now looks wrong, the rotation is being taken from something other than the velocity. |
| **Predict** | **If the flick is still there**, the deployed jar predates the fix (see R0) or the rotation is being overwritten after spawn. |
| **READ** | **FAIL — 2026-09-22, on `7b5b936`.** Ben booted this branch and ruled: *"the arrows are not correct."* **NO FIGURES** — his ruling of 2026-09-20. <br><br>**THE READING DOES NOT SAY WHICH OF THE FOUR PREDICTIONS FAILED**, and it is recorded that way rather than apportioned by guess. What it settles is the row's subject: the body still does not look fired from a bow. <br><br>**THE CAUSE WAS MEASURED AFTER THE BOOT AND IS NOT WHAT EITHER HYPOTHESIS SAID — see *WHY BOTH ROWS FAILED* below.** |

---

### R2 — Straight up and straight down, where the old pitch was most wrong

**The old rotation was pitch 0 — LEVEL. A bolt fired vertically was ninety degrees out**, which is
the largest error the defect could produce and the easiest to see.

| | |
|---|---|
| **Setup** | Same weapon and setup. Fire a tap straight **UP**, then straight **DOWN** at the floor. |
| **Predict** | **The arrow points up, and then down, from the frame it appears.** Under the defect both spawned level and swung into line a tick later. |
| **Predict** | **Both, not one.** A sign error in the pitch reaches one and not the other. |
| **Predict** | **It does not point at the FLOOR when fired up.** `Location.setDirection` on a zero vector writes `pitch = 90` — straight down — and a guard leaves a zero direction alone instead. No caller produces one today; this predicts the guard is not firing spuriously. |
| **READ** | **FAIL — 2026-09-22, on `7b5b936`.** Same boot, same ruling, no figures. <br><br>**The third prediction is now known to have been UNREADABLE on this tip**, which is not the same as its being wrong: `facing()`'s rotation never reaches the arrow at all, so the zero-vector guard could neither fire nor fire spuriously. See *WHY BOTH ROWS FAILED* below. |

---

## R1 AND R2 ARE CLOSED AT FAIL AND ARE NOT RE-READ

**They were read on `7b5b936`, where the fix was inert, and their readings stand.** The rows below
replace them on the new tip: R1's staging was EAST-or-WEST *at the muzzle*, and R2's third prediction
rested on a zero-vector guard that no longer exists. **Re-pointing a row that has been read is exactly
what the no-edit-after-reading rule forbids**, so they are left closed and new rows are written
instead — before the boot, as every prediction in this file was.

**WHAT CHANGED IN THE CODE, so the rows below can be read against something:** the rotation is now
computed in `core` (`BodyRotation.along`, the `Projectile.shoot` convention) and written to the
**ENTITY** at two sites — `Entity#setRotation` inside the spawn consumer, and again in `driveMarker`
**every tick**. `facing()` and `Location.setDirection` are deleted.

---

### R3 — *** AN EAST BOLT POINTS EAST FOR ITS WHOLE FLIGHT, NOT JUST AT THE MUZZLE ***

**This is the row R1 should have been.** R1 watched the muzzle, because the diagnosis was a one-frame
defect. It is not one frame: the platform's easing is unconditional, and for a `noPhysics` arrow its
target is the REVERSED yaw, so the interesting question is what the body does over a second of flight.

| | |
|---|---|
| **Setup** | `/rpg give dragons_plume`. Plain arrow in the off-hand (R5's workaround). Fire a **tap** (held 3–8 ticks) aimed **EAST**, along a long sightline, and **watch the bolt all the way out** — not the first frame. |
| **Predict** | **It points along its travel from the frame it appears AND STAYS THERE.** No flick at the muzzle, and **no slow swing** over the following second. |
| **Predict** | **THE SLOW SWING IS A SEPARATE READING FROM THE MUZZLE FLICK, AND THERE IS NO LONGER A WRITE TO BLAME FOR IT.** A per-tick corrective write was built and then **removed**, because the operator's own reading showed the client settling on the travel direction unaided. So if the body starts correct and then turns to point backwards and STAYS there, the finding is that **the client is not converging after all** — not that a write failed. **Say which of the two you saw**, and for how long. |
| **Predict** | **East is staged because it is NOT a no-op of either mirror.** The look-convention mirror leaves north and south alone; the `noPhysics` flip leaves east and west alone **in the server's own target** but not in what a correct fix must write. East fails visibly under the first and is the direction R1 was staged on, so it is the continuity reading. |
| **READ** | _(NOT RUN)_ |

---

### R4 — SOUTH-EAST, THE ONLY STAGING THAT SEPARATES ALL THREE CONVENTIONS

**Added because R1 could not have failed under the rival hypothesis, and that was a defect in the gate
rather than in the code.** R1's staging (east/west) and its south control agree with each other on one
of the three candidate conventions each. **South-east agrees with none of them**, so it is the one shot
whose reading names which convention is live.

Yaw in degrees, for a velocity in each direction — computed, not eyeballed:

| shot | `Projectile.shoot` (correct) | `Location.setDirection` (the inert fix) | the `noPhysics` tick target |
|---|---|---|---|
| **south** | `0` | `0` | `180` |
| **east** | `90` | `270` | `90` |
| **south-east** | `45` | `315` | `225` |

| | |
|---|---|
| **Setup** | Same weapon and setup. Fire a tap aimed **SOUTH-EAST** — diagonally, roughly 45° between south and east — and watch the bolt out. |
| **Predict** | **The bolt points SOUTH-EAST.** Not south-west (the look-convention mirror), and not north-west (the `noPhysics` flip). **Three distinguishable answers, and only one of them is a pass.** |
| **Predict** | **NAME WHICH WAY IT POINTED IF IT IS WRONG.** A bare *"wrong"* here loses the whole value of the row: the direction identifies the convention, and that is the only cheap diagnosis available. |
| **READ** | _(NOT RUN)_ |

---

### R5 — Straight up and straight down, where the pitch error is largest

**The pitch is the half of the old convention error that the look convention also got wrong**, and a
vertical shot is where ninety degrees of it is unmissable.

| | |
|---|---|
| **Setup** | Same weapon and setup. Fire a tap straight **UP**, then straight **DOWN** at the floor. Watch each for a full second. |
| **Predict** | **The arrow points UP, then DOWN, from the frame it appears and for the whole flight.** |
| **Predict** | **BOTH, NOT ONE.** A sign error in the pitch reaches one and not the other, and the two rows are one boot apart. |
| **Predict** | **The yaw carries no information on a vertical shot** — with no horizontal component it is a roll about the body's own axis. `BodyRotation` pins it at 0 so a change is deliberate; **do not read anything into the fletching's orientation here.** |
| **READ** | _(NOT RUN)_ |

---

### R6 — SOUTH, AND THIS TIME THE CONTROL CAN FAIL

**R1's south shot was a control that could not fail, and this row is the correction.** Under the
look-convention mirror, south is the **no-op value**: yaw `0` under both conventions, so a south bolt
read as a clean pass with the defect fully present. **That is not true of the `noPhysics` flip.**

> **THIS ROW'S REASONING WAS CORRECTED BEFORE ITS BOOT, 2026-09-23, AND THE CORRECTION IS THE
> INTERESTING PART.** It said: *a south bolt under the `noPhysics` flip points NORTH, so this row
> separates "the per-tick write works" from "the platform's easing wins".* **The arithmetic is right
> and the conclusion is not.** The flip's target for `(0, 0, +1)` really is `180` — due north — but
> **that is a fact about the SERVER's `getYaw()`, and the operator's reading shows it does not
> render**: on `7b5b936`, with no rotation applied at all, a bolt *"took a few seconds but did
> correct itself eventually"*. A flip that reached the client would have settled it BACKWARDS, not
> correct. **So the flip is invisible, and there is no per-tick write any more for this row to be
> about.**
>
> **SOUTH IS STILL THE RIGHT CONTROL, FOR THE ORIGINAL REASON ONLY.** It is the **no-op value of the
> look-convention mirror** — yaw `0` under both conventions — so it is the one direction that reads as
> a clean pass with that defect fully present. **The row's job is to prove the spawn write is being
> applied at all**, by being the shot that cannot distinguish the two conventions: if south looks right
> and R3's east does not, the write is landing in the wrong convention rather than not landing.
>
> **AND IF A SOUTH BOLT DOES SETTLE POINTING NORTH, THAT IS A NEW FINDING**, not the expected failure:
> it would mean the server's flipped yaw reaches the client after all, against the reading above.

| | |
|---|---|
| **Setup** | Same weapon and setup. Fire a tap aimed **SOUTH** and watch the bolt out for a full second. |
| **Predict** | **It points SOUTH for the whole flight.** |
| **Predict** | **IF IT TURNS TO POINT NORTH, THAT CONTRADICTS THE READING THIS ROW WAS REASONED FROM** — the server's flipped yaw would be reaching the client, which the 2026-09-22 boot indicates it does not. **Report it as a contradiction, not as a failure of the fix**; the two call for different work. |
| **Predict** | **THIS ROW IS NOT A NO-OP AND THE PREVIOUS SOUTH ROW WAS.** Stated here because the two look identical in the log: *"fired south, looked right"* means nothing on `7b5b936` and everything on this tip. |
| **READ** | _(NOT RUN)_ |

---

### R7 — *** THE LAYER ROW: NOTHING SERVER-SIDE CAN SEE WHAT THE CLIENT DRAWS ***

**Every other row in this file is read with eyes on a client, and the fix is written on a server.** This
row exists because the gap between those two is measured to be real, and no amount of reading the
server jar closes it.

**WHAT IS MEASURED, from the pinned build:**

```
MinecraftServer.tickChildren  bc 31   FoliaGlobalRegionScheduler.tick()   <- driveMarker runs here
ServerLevel.tick              bc 436  ServerChunkCache.tick -> ChunkMap.tick()
                                      -> newTrackerTick -> ServerEntity.sendChanges()
ServerLevel.tick              bc 591  EntityTickList.forEach -> AbstractArrow.tick()
```

`ServerEntity.sendChanges` reconsiders rotation only when `forceStateResync`, or
`tickCount % updateInterval == 0`, or `needsSync`, or the entity data is dirty — and
**`EntityType.ARROW` is built with `updateInterval(20)`**, while `needsSync` is set by `setPosRaw`
only for types whose interval is `Integer.MAX_VALUE`. **So the server's yaw reaches a client roughly
once every twenty ticks, and the client fills in the other nineteen itself.**

**WHAT IS NOT MEASURED, AND IS NOT GUESSED AT HERE:** the client runs the same `AbstractArrow.tick`
on its own copy, and whether it takes the flipped branch depends on `isNoPhysics()` — **a field the
server does not sync.** So the client's copy probably computes the UNflipped `atan2(x, z)`, which is
the correct convention, and would then agree with our write. **That is reasoning, not a reading**, and
the client is not in this jar. **This row is what settles it.**

| | |
|---|---|
| **Setup** | Any of R3–R6, watched for **several seconds** on a long flight rather than a short one. The Plume's charged release is the longest-lived body available. |
| **Predict** | **The body's heading does not drift, oscillate, or snap periodically.** A **periodic** correction — a visible twitch roughly once a second — would be the twenty-tick sync arriving and disagreeing with what the client had drawn in between. |
| **Predict** | *** IF THE BOLT STILL FLIES BACKWARDS ON THIS BUILD, THE SERVER'S ROTATION IS LOSING TO THE CLIENT'S OWN TICK, AND THE FIX IS IN THE WRONG LAYER. *** No further server-side write helps, and that is measured rather than assumed: `driveMarker` runs in the earliest phase of the tick, before both the tracker send and the arrow's own tick, so **a write placed there was the best a setter could do — and it was removed for want of a reader.** **The next move would be a packet, not a setter** — which crosses into PacketEvents and needs a ruling, not a patch. |
| **Predict** | **AND A PASS HERE DOES NOT PROVE THE MECHANISM, ONLY THE OUTCOME.** If the bolts look right, we still will not know whether it is our write or the client's own arithmetic doing it. **Record that as a pass with the mechanism unresolved**, rather than as confirmation of the paragraph above. |
| **READ** | _(NOT RUN)_ |

---

## AND THE OPERATOR'S SECOND OBSERVATION, WHICH SETTLED THE LAYER QUESTION

**Reported 2026-09-23, about the same `7b5b936` boot**, and recorded here rather than in R1's or R2's
`READ` cell because **those rows are closed and a reading is not edited once written**:

> the bolt **"took a few seconds but did correct itself eventually"**.

**THAT IS THE ANSWER TO WHETHER THE SERVER'S FLIPPED YAW RENDERS, AND IT IS NO.** On that build no
rotation was applied at spawn at all, and the server's own steady-state target for a `noPhysics` arrow
is `atan2(-x, -z)` — **backwards**. Had that value been what the client drew, the bolt would have
settled pointing the wrong way and stayed there. It settled CORRECT. **So the client converges on the
travel direction from its own copy of the arrow tick, and the server's rotation is not what determines
what a player sees after the first frames.**

**WHAT IT COST, AND THE COST IS THE POINT:** a per-tick corrective write in `driveMarker` was designed,
measured into the right scheduler phase, built and tested — and then **removed**, because this one
sentence shows it has no reader. The spawn-consumer write stays: the add-entity packet's yaw and pitch
are the only thing supplying the first rendered frame. **The server-side yaw of a bolt in flight
remains about 180 degrees out from its travel, known and unfixed.**

**AND THE SEQUENCE IS WORTH KEEPING.** The order was read from the bytecode *before* the per-tick line
was written, which is what made the line correct; it was **one observation from the operator that made
it pointless.** Neither step was wasted and neither could have replaced the other — the measurement
established what a server-side write can do, and the observation established that nothing needs it.

---

## WHY BOTH ROWS FAILED
 — MEASURED 2026-09-22, AFTER THE BOOT

**NO PREDICTION ABOVE IS EDITED.** Every `Predict` cell stands exactly as it was written before the
boot. This section is the post-hoc account, and it was produced by **reading the shipped artifacts**
rather than by reasoning about them.

**PROVENANCE, BECAUSE EVERYTHING BELOW RESTS ON IT.** The bytecode was read out of
`run/versions/26.1.2/paper-26.1.2.jar` with `C:/Users/Neb91/.jdks/openjdk-26.0.1/bin/javap.exe -c -p`.
That jar's manifest says **Paper `26.1.2-74-e4e17fc`, `Build-Number: 74`** — the same build `pom.xml`
pins as `paper.version`, and the same build the boot logs record for this dev server. **The code read
here is the code that ran under Ben's plume.** `Location.setDirection` is quoted from
`paper-api-26.1.2.build.74-stable-sources.jar` in `~/.m2`: real source, not bytecode.

### *** THE FIX IS INERT. `world.spawn` THROWS A LOCATION'S ROTATION AWAY FOR AN ARROW. ***

`CraftEntityTypes` registers `EntityType.ARROW` through **`createAndMoveEmptyRot`**, and that
positioner is `MOVE_EMPTY_ROT`:

```
Entity.snapTo(SpawnData.x(), SpawnData.y(), SpawnData.z(), 0.0F, 0.0F)
                                                          ^^^^  ^^^^
                                                          yaw   pitch  -- both fconst_0
```

`facing()` computes a yaw and a pitch and writes them onto the `Location`. The arrow is then
positioned at **yaw 0, pitch 0 — due south and level**, which is the defect this branch exists to
remove. A sibling positioner, `MOVE`, *does* read `SpawnData.yaw()` and `SpawnData.pitch()` — **arrows
are not registered with it.** `ENDER_PEARL`, `EXPERIENCE_BOTTLE` and `SPECTRAL_ARROW` share the
arrow's rotation-discarding one.

**So `7b5b936` cannot change a rendered frame.** Not a wrong rotation — **no rotation**, exactly as
before.

> **AND THAT IS WHY R0 BEING *NOT RUN* DOES NOT LEAVE THESE FAILS AMBIGUOUS.** R0 exists to catch a
> stale jar, and here a stale jar and a correct jar **produce the same reading**: the change is inert
> in both. R0 stays NOT RUN rather than being back-filled from an argument.

### WHERE THE ROTATION MUST BE WRITTEN INSTEAD, AND THE SEAM THAT FOLLOWS

`CraftRegionAccessor.spawn` calls `createEntity(...)` (construct, then position with the rotation
discarded) and then `addEntity(nms, reason, consumer, randomize)`, which calls
**`consumer.accept(entity.getBukkitEntity())` and only then `addEntityToWorld(...)`**. The consumer
`spawnBoltMarker` already passes therefore runs **before the entity joins the world**, so a rotation
written inside it is in place before any add-entity packet exists.
`org.bukkit.entity.Entity#setRotation(float yaw, float pitch)` is in the pinned API;
`CraftEntity.setRotation` normalises through `Location.normalizeYaw` (to `(-180, 180)`) and
`Location.normalizePitch` (clamped to `[-90, 90]`) and calls `Entity.forceSetRotation`.

**The arithmetic is a pure function of the velocity and belongs in `core/`. The application point is
the ENTITY, inside the spawn consumer — not the `Location`, in any convention.**

### THE FOUR SIGN CONVENTIONS, AS THE PINNED ARTIFACTS WRITE THEM

`Mth.atan2(a, b)` was **executed** against `Math.atan2(a, b)` over seven quadrant cases rather than
assumed: equal to within `2e-6` degrees. So its first argument is the opposite and its second the
adjacent, exactly as `Math.atan2`.

| where | yaw | pitch |
|---|---|---|
| `Location.setDirection` (**source**) | `toDegrees((atan2(-x, z) + 2PI) % 2PI)` | `toDegrees(atan(-y / sqrt(x*x + z*z)))` |
| `Projectile.shoot` | `atan2(x, z) * 180/PI` | `atan2(y, horizontalDistance) * 180/PI` |
| `Projectile.updateRotation` | `lerpRotation(yRotO, atan2(x, z) * 180/PI)` | `lerpRotation(xRotO, atan2(y, h) * 180/PI)` |
| `AbstractArrow.tick` | `lerpRotation(getYRot(), atan2(±x, ±z) * 180/PI)` — **the signs depend on `isNoPhysics()`; see below** | `lerpRotation(getXRot(), atan2(y, h) * 180/PI)` |

`Location.setDirection` also early-returns when `x == 0 && z == 0`, writing `pitch = y > 0 ? -90 : 90`
and **leaving yaw untouched**. A straight-up shot went down that path, never through `facing()`'s zero
guard — `lengthSquared()` is not zero for a vertical velocity.

**`Projectile.shoot` is the WITNESS for what a correctly-oriented arrow's rotation fields hold**, and
it is a witness rather than an argument: every vanilla bow arrow is spawned through it and looks right
on the frame it appears. Its convention — `yRot = atan2(x, z)`, `xRot = atan2(y, h)`, degrees — is the
value to write.

### THE REVIEW SEAT'S HYPOTHESIS: CONFIRMED ON THE ARITHMETIC, REFUTED ON THE MECHANISM

| the hypothesis said | measured |
|---|---|
| `setDirection` writes the entity-look convention, `atan2(-x, z)` / `-atan(y/h)` | **CONFIRMED** from source; the pitch is `atan(-y/xz)`, the same thing |
| the projectile convention is `atan2(x, z)` / `atan2(y, h)` | **CONFIRMED verbatim**, in `Projectile.shoot` and in `tick`'s physics branch |
| therefore every spawned body is MIRRORED — east/west 180° out, up/down inverted, north/south unaffected | **REFUTED, with the arithmetic intact.** The mismatch is real and **unreachable**: the rotation is discarded before it reaches the arrow. Nothing is mirrored because nothing is applied. |
| `AbstractArrow.tick` snaps rotation only when `xRotO == 0 && yRotO == 0`, so a non-zero start eases at 0.2/tick instead | **REFUTED on the branch, CONFIRMED on the lerp.** `xRotO` and `yRotO` do not appear in `AbstractArrow` at all. The lerp is **unconditional**: `lerpRotation(getYRot(), target)`, and `lerpRotation` is `Mth.lerp(0.2f, from, to)` after normalising. |

**Two consequences of that last row, and neither is cosmetic.** The hypothesis' conclusion was *worse
than before*; the truth is **exactly as bad as before, to the frame.** And because the lerp was
already unconditional, **there was never a one-frame flick to begin with**: a pre-fix body eased out
of due south over roughly ten ticks. This file's own prose — *"visibly snaps into line a tick later"*
— understates what shipped, and so does `032b012`'s message.

### A SECOND FINDING, FROM THE SAME READ — AND IT IS NOT THIS BRANCH'S

**`AbstractArrow.tick` NEGATES THE YAW FOR A `noPhysics` ARROW, AND EVERY BOLT BODY IS `noPhysics`.**
Local 1 is `!isNoPhysics()`, assigned once at bytecode 12 and never reassigned; it gates the yaw
target and nothing else:

```
442: iload_1
443: ifne 473          -- flag set, physics:     atan2( x,  z)
446:                   -- flag clear, noPhysics:  atan2(-x, -z)   <-- every bolt body takes THIS branch
```

`spawnBoltMarker` sets `setNoPhysics(true)` unconditionally — *the one switch*. So the yaw the
**server** eases a bolt body toward is **180° from its direction of travel**, at 20% a tick. The pitch
target is not negated; this is yaw only.

**IT IS NOT MEASURED IN-GAME AND IS NOT CLAIMED AS A VISIBLE DEFECT.** What a client renders also
depends on client-side ticking and on rotation packets, and neither is in this jar's server code or in
anything this gate has looked at. What *is* measured is only that the server's steady-state yaw for a
`noPhysics` arrow disagrees with `Projectile.shoot` by exactly 180°. It needs a row of its own — *does
a long-lived bolt turn to point backwards over its first second?* — and that row is not written here,
because this file's subject is one frame.

> **IT ALSO PUTS A QUESTION MARK ON THE FIRST BULLET OF *WHAT THIS GATE CANNOT SEE*.** *"was already
> correct"* is an assertion there, never a reading, and this is a reason to doubt it. The bullet is
> left standing rather than quietly reworded: it was written as a prediction's neighbour, and the
> doubt belongs here, dated.

### OWED WORK FOR THE SLICE THAT WRITES THE FIX

- **A SOUTH-EAST ROW, AND HERE IS WHAT MAKES IT WORTH A BOOT.** It is the only staging that separates
  all three candidate conventions, because no two of them agree on it. Yaw in degrees, for a velocity
  in that direction:

  | shot | `setDirection` | `Projectile.shoot` | `noPhysics` tick target |
  |---|---|---|---|
  | **south** `(0, 0, +1)` | `0` | `0` | `180` |
  | **east** `(+1, 0, 0)` | `270` | `90` | `90` |
  | **south-east** `(+1, 0, +1)` | `315` | `45` | `225` |

  **South cannot tell `setDirection` from `shoot`. East cannot tell `shoot` from the `noPhysics`
  target. South-east tells all three apart** — which is the property R1's east/west staging and its
  south control do not have between them.

- **R0's NEEDLE MOVES WITH THE CODE.** `setDirection` is still the correct present-needle for
  `7b5b936`, and must become an ABSENT-needle the moment the fix stops routing through it; the
  present-needle then has to be a symbol the new code adds. Naming it is part of writing the fix, not
  of reading this boot.
---

## WHAT THIS GATE CANNOT SEE

- **Whether the bolt's later flight looks right.** That is unchanged by this fix and was already
  correct: the rotation has always been derived from `deltaMovement` every tick after the first.
  **This fix touches exactly one frame.**

  > **BOTH SENTENCES ABOVE ARE ASSERTIONS, AND BOTH ARE NOW OPEN OR FALSE. 2026-09-22.**
  >
  > ***"was already correct"* — OPEN, never read.** This bullet reasoned from *the rotation is
  > derived from `deltaMovement` every tick*, which is true and insufficient.
  > **`AbstractArrow.tick` derives the yaw as `atan2(-x, -z)` when `isNoPhysics()`** — and every
  > bolt body sets `setNoPhysics(true)` — so the **server's** steady-state yaw for a bolt is
  > **180° from its travel, for the whole flight.** Whether a player sees that is **not**
  > established: client-side ticking and rotation packets were not read, and no claim is made
  > about them. The point is only that this bullet cites a reading nobody took.
  >
  > ***"touches exactly one frame"* — FALSE.** It touches none: `world.spawn` discards a
  > `Location`'s rotation for an arrow. See *WHY BOTH ROWS FAILED* below.
- **The scatter/spread case.** That weapon is on `feat/14-scatter-shot` and this branch is off
  `master`, deliberately. Seven bodies per press is a louder version of the same frame and will be
  visible there once both have merged — it is not a reason to read this row on that branch.
