# GATE — The arrow body's first frame

**Status: R1 and R2 FAILED, read 2026-09-22 on `7b5b936`. R0 NOT RUN.** Every prediction was
written BEFORE any boot and no prediction is edited once a row has been read. Readings go in the
`READ` cell beside the prediction they answer.

```
FAIL      2   R1 R2    -- read 2026-09-22, bound to 7b5b936
NOT RUN   1   R0
         ──
         3   = git grep -c '^### R' <ref> -- GATE-arrow-body-orientation.md
```

**GAME MODE: SURVIVAL**, for every row. Nothing here is a cost, so no creative divergence applies —
declared anyway, per the standing debt in `CLAUDE.md`.

---

## WHAT THIS FIXES, AND WHY IT IS NOT SLICE 14'S

**A `Vec3` carries no rotation.** `toLocation` builds a `Location` from three doubles, so its yaw
and pitch are the constructor's defaults — **0 and 0, which is due south and level.** An arrow
derives its own rotation from `atan2` over `deltaMovement` **inside its own `tick()`**, so the
velocity set at creation is right one tick BEFORE the rotation derived from it is. **The frame in
between renders due south, whichever way the bolt is actually travelling.**

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
| **Setup** | `Copy-Item run/plugins/rpg-<ver>.jar "$env:TEMP\deployed.zip" -Force`<br>`Expand-Archive "$env:TEMP\deployed.zip" -DestinationPath "$env:TEMP\deployed" -Force`<br>`$classes = Get-ChildItem "$env:TEMP\deployed\io\github\butterflysmp\rpg" -Recurse -Filter *.class`<br>`Write-Host "$($classes.Count) class files scanned"`<br>`$classes \| Select-String -Pattern 'setDirection' -Encoding ascii \| Select-Object -ExpandProperty Path` |
| **Predict** | **The class count is NON-ZERO and is reported.** It is the no-op value: `0 scanned` means the unpack failed, and an absence underneath a zero means nothing. |
| **Predict** | `setDirection` is found in **`PaperCombatWorld.class`**. A **0** means the deployed jar predates this fix, whatever its mtime says. |
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

## WHY BOTH ROWS FAILED — MEASURED 2026-09-22, AFTER THE BOOT

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
- **The scatter/spread case.** That weapon is on `feat/14-scatter-shot` and this branch is off
  `master`, deliberately. Seven bodies per press is a louder version of the same frame and will be
  visible there once both have merged — it is not a reason to read this row on that branch.
