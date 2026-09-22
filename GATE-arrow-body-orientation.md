# GATE — The arrow body's first frame

**Status: NOT RUN.** Every prediction was written BEFORE any boot and no prediction is edited once
a row has been read. Readings go in the `READ` cell beside the prediction they answer.

```
NOT RUN   3   R0 R1 R2
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
| **READ** | _(NOT RUN)_ |

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
| **READ** | _(NOT RUN)_ |

---

## WHAT THIS GATE CANNOT SEE

- **Whether the bolt's later flight looks right.** That is unchanged by this fix and was already
  correct: the rotation has always been derived from `deltaMovement` every tick after the first.
  **This fix touches exactly one frame.**
- **The scatter/spread case.** That weapon is on `feat/14-scatter-shot` and this branch is off
  `master`, deliberately. Seven bodies per press is a louder version of the same frame and will be
  visible there once both have merged — it is not a reason to read this row on that branch.
