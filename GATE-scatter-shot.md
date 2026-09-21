# GATE — The Scatter Shot (Slice 14)

**Status: NOT RUN. No row below has been booted.** Every prediction was written BEFORE any boot and
no prediction is edited once a row has been read. Readings go in the `READ` cell beside the
prediction they answer, never over it.

```
NOT RUN   10   R0 R1 R2 R3 R4 R5 R6 R7 R7b R8
         ──
         10   = git grep -c '^### R' <ref> -- GATE-scatter-shot.md
```

**GAME MODE: SURVIVAL, for every row unless the row says otherwise.** Declared per the standing
debt in `CLAUDE.md`.

> ### WHY SURVIVAL MATTERS HERE, AND IT IS NOT A FORMALITY
>
> The creative-divergence register's shape is **creative removes a COST**, and this weapon is
> almost entirely made of costs. **`hasInfiniteMaterials()` does not reach it** — the Scatter Shot
> is a crossbow binding `right_click`, not a `BowItem`, so there is no ammunition short-circuit to
> hit. **But R6 is a magazine row**, and a magazine is exactly the kind of cost creative is in the
> habit of removing. Run every row in survival, and R6 especially.

> ### *** WHAT SLICE 14 CLAIMS, IN ONE SENTENCE, SO A FAILING ROW CAN BE ATTRIBUTED ***
>
> **One press puts SEVEN arrows in the air, arranged in a hexagon in the shooter's own view plane,
> for ONE quiver round and ONE damage roll.**
>
> A row that fails tells you which clause broke: the count (R1), the plane (R2, R3), the absence of
> homing (R4), the tooltip's promise (R5), the magazine (R6), the arithmetic (R7), the push (R7b),
> or the roll (R8).

---

## *** R0 — THE FIRST ROW OF EVERY GATE FILE ***

### R0 — The deployed build carries this slice

**RUN THIS FIRST. IF IT FAILS, STOP: NO OTHER ROW IN THE FILE IS READABLE.**

**SOLE WITNESS FOR EVERY OTHER ROW.** A wrong jar does not announce itself — it produces readings,
in the right shape, at plausible values. Slice 12b lost a boot to exactly this on 2026-09-20 and
got a complete, self-consistent, entirely false set of readings out of it.

> **THE PROBE IS POWERSHELL, AND THAT IS NOT A STYLE CHOICE.** `unzip` does not exist on the gate
> shell and `java` on `PATH` there is Oracle's JRE shim, which ships no `javap`. **A bash-shaped R0
> row is a row that does not get run**, and an unread R0 makes every row below unreadable.
> `CLAUDE.md`'s own `unzip -p` example is the form that does **not** run here.

| | |
|---|---|
| **Setup** | Unpack the DEPLOYED jar and probe for a symbol this slice introduces, plus a content key: <br><br>`Copy-Item run/plugins/rpg-<ver>.jar "$env:TEMP\deployed.zip" -Force`<br>`Expand-Archive "$env:TEMP\deployed.zip" -DestinationPath "$env:TEMP\deployed" -Force`<br>`$classes = Get-ChildItem "$env:TEMP\deployed\io\github\butterflysmp\rpg" -Recurse -Filter *.class`<br>`Write-Host "$($classes.Count) class files scanned"`<br>`$classes \| Select-String -Pattern 'SpreadPattern' -Encoding ascii \| Select-Object -ExpandProperty Path`<br>`Select-String -Path "$env:TEMP\deployed\content\weapons\scatter_shot.yml" -Pattern '^\s+count:'` |
| **Predict** | **The class count is NON-ZERO and is reported.** It is the no-op value: `0 scanned` means the unpack failed, and an absence underneath a zero means nothing. |
| **Predict** | `SpreadPattern` is found in **at least two** class files — `core/combat/SpreadPattern.class` and `core/ability/CastExecutor.class`, which calls it. A **0** means the deployed jar predates this slice, whatever its mtime says. |
| **Predict** | `scatter_shot.yml` is present in the jar and `count:` matches. A missing FILE and a missing KEY are different failures: the first is a deploy that did not run, the second is a jar built before the spread block was authored. |
| **Predict** | **`-Encoding ascii` is load-bearing.** A `.class` is binary and a text-mode read will not find a constant-pool string. If every probe returns nothing, check this before concluding anything about the jar. |
| **Predict** | **STATE WHICH TREE THE JAR WAS BUILT FROM.** There is a second worktree at `C:/Users/Neb91/IdeaProjects/rpg-12b`, and that ambiguity is what cost the 2026-09-20 boot. |
| **READ** | _(NOT RUN)_ |

---

## THE ROWS

### R1 — Seven arrows are counted IN FLIGHT, not inferred from damage

**A DAMAGE TOTAL CANNOT COUNT ARROWS.** 63 could be seven arrows at 9 or one arrow at 63, and at
any range short of point blank the total will not be 63 anyway. **Count the bodies.**

| | |
|---|---|
| **Setup** | `/rpg give scatter_shot`. Face a flat wall about 10 blocks away, in the open, and fire ONE press. Count the arrows in flight, or the impacts on the wall. |
| **Predict** | **SEVEN** arrows leave on one press. |
| **Predict** | **If it reports ONE**, the spread block parsed and nothing called it — the `CastExecutor` wiring. That is `MUT14NOSPREAD`, in the field, and it was GREEN across the whole suite before `CastExecutorSpreadTest` was written. |
| **Predict** | **If it reports SEVEN but they all travel down one line**, the basis collapsed — see R2/R3, and expect those to fail too. |
| **Predict** | They arrive **together**, in one frame. A stagger means the spread was wired as a volley. |
| **READ** | _(NOT RUN)_ |

---

### R2 — The hexagon reads as a hexagon at the horizon

| | |
|---|---|
| **Setup** | Pitch **0** — level with the horizon. Fire one press at a flat wall ~10 blocks away. |
| **Predict** | Six impacts around a seventh, evenly spaced. At 10 blocks the pattern is about **1.75 blocks** across (`2 x 10 x tan(5°)`). |
| **Predict** | **The centre impact sits ON the crosshair.** The aim vector is fired first and undeviated; if the centre is offset, the ring was built around the wrong axis. |
| **Predict** | **THIS ROW CANNOT SEE THE BASIS, AND IS NOT EXPECTED TO.** Measured in the suite: a world-up basis and the shooter's own agree at every pitch but the poles, so a correct-looking reading here says nothing about R3. **Do not report R2 passing as evidence for R3.** |
| **READ** | _(NOT RUN)_ |

---

### R3 — *** THE HEXAGON SURVIVES AT PITCH EXACTLY +90 AND EXACTLY −90 ***

**THIS IS THE ROW THE WHOLE BASIS DESIGN EXISTS FOR, AND IT IS THE ONE A PLAYER MEETS BY ACCIDENT.**
It is not an edge case anyone has to look for: it is what happens the first time someone shoots at
a bird.

> **A basis derived by crossing the look vector with world up is correct at every pitch except
> these two**, where the look vector IS world up and the cross product is zero. So this row and its
> twin are the only readings in the file that can see the basis at all.

> ### *** THE PITCH MUST BE EXACTLY +/-90. "STEEP" AND "NEARLY VERTICAL" ARE NOT THIS ROW. ***
>
> **MEASURED, which is why this is stated as a requirement rather than a preference.**
> `SpreadPatternTest.theRingHoldsItsShapeApproachingThePole` sweeps **80, 89, 89.9, 89.99 and
> 89.999 degrees** against a world-up basis and **every one of them PASSES.** The defect is **AT**
> the pole, not near it: the cross product gets small approaching the pole and normalises perfectly
> well, and is the zero vector only when the look vector IS world up.
>
> **So a reading taken at "almost straight up" is GREEN AGAINST A BROKEN BASIS.** It is not a weak
> reading of this row — it is a reading of a different row, and reporting it here would be a false
> PASS on the one axis nothing else in the file can see.
>
> **Practically: push the pitch to the stop.** Minecraft clamps at exactly +/-90, so hold the mouse
> hard up (or hard down) until the view stops moving, and only then fire. If the crosshair can
> still move in that direction, the row has not been staged yet.

| | |
|---|---|
| **Setup** | Pitch **exactly +90** — mouse held up until the view STOPS moving — in the open. Fire one press and watch the arrows leave. Then **exactly −90**, view held hard down, standing on a flat floor, and fire again. |
| **Predict** | **Straight up: seven arrows, still a hexagon**, opening out as they rise. Same apparent shape as R2 from the shooter's point of view. |
| **Predict** | **Straight down: seven impacts on the floor**, a hexagon around a centre, roughly **1.7 blocks** across if fired from eye height onto the floor beneath. |
| **Predict** | **If the arrows collapse into a LINE, or into ONE arrow, the basis degenerated** — a world-up derivation, or `Aim`'s two-argument constructor reaching production. |
| **Predict** | **If the server logs `spread basis is degenerate`**, the guard fired: something built an `Aim` without the shooter's right. That is a defect in the wiring, not in the geometry, and `AimWiringSignatureTest` should have caught it at build time. |
| **Predict** | **Both poles, not one.** A sign error reaches one and not the other. |
| **Predict** | **THE READING STATES THE PITCH IT WAS TAKEN AT, and the only acceptable values are +90 and −90.** A reading with no pitch recorded is indistinguishable from one taken at 89, which passes against a broken basis — so an unanchored PASS here is worth nothing, and must not be written as one. |
| **READ** | _(NOT RUN)_ |

---

### R4 — An arrow fired past a mob does NOT curve toward it

**The absence of the homing block is a RULING, and this is the only reading that can see it.** The
suite's only witness is one row in `ScatterShotContentTest` asserting the parsed block is null —
measured: `MUT14HOMING` reddens that row and nothing else in 2153 tests.

| | |
|---|---|
| **Setup** | Stand a mob in the open. Aim to one side of it, so the centre arrow passes within a couple of blocks and misses. Fire one press. |
| **Predict** | **Every arrow flies straight.** Nothing bends toward the mob; the misses stay missed. |
| **Predict** | **If an arrow curves, a homing block reached the deployed content** — compare against `dragons_plume`, which is the only weapon in the tree that may author one. |
| **Predict** | **CONTROL, in the same session:** `/rpg give dragons_plume`, draw fully, and fire past the same mob. **Its arrows DO seek.** Without this, "nothing curved" is satisfied by a build where homing is broken for everything. |
| **READ** | _(NOT RUN)_ |

---

### R5 — The tooltip reads `Kinetic Damage: 9 x 7`

| | |
|---|---|
| **Setup** | `/rpg give scatter_shot` and read the item tooltip. Hold it; do not open a menu. |
| **Predict** | A line reading exactly **`Kinetic Damage: 9 x 7`** — element-labelled, the PER-ARROW number, the count beside it, **ONE space each side of the `x`** per slice 12d. |
| **Predict** | **`63` appears NOWHERE on the tooltip.** The full-hit total is true of no single arrow, and folding it in is the defect the per-shot rule exists to prevent. |
| **Predict** | **NO `Attack Speed` line and NO `Ranged Damage` line.** This weapon's payload is a literal, so it renders an ABILITY block rather than a stat block. **That is correct, not a regression** — and it is the visible half of the "two damage-rendering shapes" finding. |
| **Predict** | `Quiver: 4/4` on a freshly given item. **`--/4` means the mint path did not stamp it** — absence renders as dashes, never as `0`. |
| **Predict** | `Cooldown: 1.6s`. |
| **READ** | _(NOT RUN)_ |

---

### R6 — The quiver reads 4, drops by ONE per press, and reloads after the fourth

**SOLE WITNESS FOR THE ONE-ROUND RULING IN THE FIELD.** `QuiversSignatureTest` pins the source
expression, but the spend itself needs a live `Player` and `ItemStack`, so no module can execute
it. **Measured 2026-09-21: before that guard existed, changing the spend from 1 to 7 left the
entire suite green at 2147.**

| | |
|---|---|
| **Setup** | SURVIVAL. `/rpg give scatter_shot`. Read the tooltip, then fire four presses, reading the quiver line after each. Then hold left-click to reload. |
| **Predict** | The count goes **4 → 3 → 2 → 1 → 0**. **ONE per press, though seven arrows leave.** |
| **Predict** | **If it drops by SEVEN and the weapon is empty after one press**, the spread was wired as a yaw FAN — `yawOffsets.length` rounds instead of one. That is the defect this row is the field witness for. |
| **Predict** | The fifth press is REFUSED and says so. A quiver weapon that fires on empty has lost its gate. |
| **Predict** | Left-click reloads, takes **2.00 seconds** (`reload_ticks: 40`), and restores **4**. |
| **Predict** | **CREATIVE DIVERGENCE — do not read this row in creative.** A magazine is a cost, and the register's shape is that creative removes costs. |
| **READ** | _(NOT RUN)_ |

---

### R7 — A full seven-arrow hit deals 63, and the arithmetic is at gear score 100

| | |
|---|---|
| **Setup** | Stand a mob **within about 3 blocks** — point blank, where the whole pattern fits a target. Confirm the weapon's gear score is **100** before reading (`/rpg gearscore`, or give a fresh one). Fire ONE press and read the damage numbers. |
| **Predict** | **Seven damage numbers of 9**, totalling **63**. |
| **Predict** | **If fewer than seven numbers appear, the range was too long** — that is the weapon working, not failing. Re-take it closer. At 5 blocks the pattern is 0.87 blocks across; at 10 it is 1.75 and a player-sized target can no longer catch it all. |
| **Predict** | **If ONE number of 63 appears, the arrows are not resolving independently.** |
| **Predict** | **THE SCORE ANCHOR IS PART OF THE READING.** 9 is the value at GS 100 and the literal is scaled by `score/100` over a legal band of 100..500 — so a scored weapon reads up to **45 per arrow and 315 a full hit**. A reading taken at an unstated score is one point on a five-fold range. |
| **READ** | _(NOT RUN)_ |

> ### *** R7 AND R1 ARE A CROSS-CHECK, AND IT IS WRITTEN DOWN RATHER THAN LEFT TO A READER ***
>
> A gate sheet checks each row against reality and never the rows against each other, so the
> conjunction goes in a row rather than in a summary nobody re-reads. **Both readings already
> exist and neither costs another boot.**
>
> R1 counts seven arrows IN FLIGHT. R7 counts seven damage numbers ON A TARGET. **Two passing rows
> that disagree is the interesting outcome**: seven in flight and fewer than seven landing at point
> blank means the bodies are not resolving independently, and neither row reports that on its own.

---

### R7b — Knockback: seven pushes against ONE, read the same way

**THE MAGNITUDE IS NOT RULED AND THIS ROW IS WHAT RULES IT.** `0.1` is a proposal. Everything turns
on whether seven applications in one frame **SUM** or whether the last one **WINS** — `0.1` is the
sensible figure under the first reading and very nearly a no-op under the second, and no amount of
reasoning about the design settles which the platform does.

> **The static read predicts SUM.** `BukkitCombatant.applyKnockback` is
> `entity.setVelocity(entity.getVelocity().add(v))`, and its sibling `applyImpulse` documents the
> contrast in its own javadoc: *"REPLACES velocity rather than adding to it (unlike knockback)"*.
> **That is a prediction, not a verdict.**

| | |
|---|---|
| **Setup** | Stand a mob on flat open ground. **(a)** Fire ONE press at **point blank** so all seven land, and mark how far it is pushed. **(b)** Back off to about 15 blocks so exactly ONE arrow lands, and fire again at the same mob from the same footing. Compare the two distances. |
| **Predict** | **THE CONTROL IS (b), AND WITHOUT IT THIS ROW MEASURES NOTHING.** One push and seven pushes must produce different numbers, or the instrument cannot tell the two mechanisms apart. |
| **Predict** | **If they SUM:** (a) pushes roughly seven times as far as (b). Report both distances. |
| **Predict** | **If the last one WINS:** (a) and (b) push the same distance, and the weapon effectively has no knockback worth the key. |
| **Predict** | **Report WHICH of the two you observed, in those words**, and the two distances. The figure Ben rules on is the one with this measurement under it. |
| **Predict** | **Same mob type, same ground, same footing for both.** Knockback resistance and slope both move the distance. |
| **READ** | _(NOT RUN)_ |

---

### R8 — A crit is all seven or none

**Ben's ruling: ONE damage roll for the shot, crit included, applied to all seven arrows.** The
property is INHERITED rather than built — the crit is drawn once into the snapshot
(`BukkitCombatant.snapshot`: *"drawn HERE and frozen -- once per cast, never per damage arm"*) and
`CastExecutor.commit` projects it once for the whole press.

> **A build that re-rolled per arrow passes every test that does not stage a crit**, because
> without a crit the seven amounts are identical either way. Only a boot with a real crit chance
> can see it.

| | |
|---|---|
| **Setup** | Point blank, so all seven land. Fire repeatedly and watch the damage numbers per press. `/rpg crit` to raise the chance if the base rate makes this slow. |
| **Predict** | **Within one press, all seven numbers are the same.** Either all seven are crit-coloured and raised, or none is. |
| **Predict** | **A MIXED press is the failure** — some arrows crit and others not, from one press. That is a per-arrow roll, and it means the spread is being expanded above the commit. |
| **Predict** | Across presses the numbers DO vary, which is the control: if every press is identical the crit chance is zero and the row is reading nothing. |
| **READ** | _(NOT RUN)_ |

---

## WHAT THIS GATE CANNOT SEE, SAID SO IT IS NOT ASSUMED

- **The reload figure is a PROPOSAL.** `reload_ticks: 40` is the only unruled number in the content
  file. R6 reads that the reload takes 2.00 seconds; it cannot say whether 2.00 seconds is right.
- **The knockback magnitude** is R7b's output, not its input.
- **The material collision.** Four weapons now share `crossbow` — `boltor`, `locust`,
  `quiver_stone` and this one. No row here reads it, because there is nothing to read: they are
  genuinely indistinguishable in a hotbar, and custom model data is not in this slice.
- **The ability-block name.** No `name:` is authored, so the tooltip reads
  `Scatter Shot  Right-Click`. That is a fallback, not a ruling — if it reads worse than a name of
  its own, it is a one-word change.
