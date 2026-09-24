# GATE — The Dragon's Breath (Slice 14)

**Status: NOT RUN. No row below has been booted.** Every prediction was written BEFORE any boot and
no prediction is edited once a row has been read. Readings go in the `READ` cell beside the
prediction they answer, never over it.

```
NOT RUN   20   R0 R1 R2 R3 R4 R5 R6 R7 R7b R8 R9 R10 R11 R12 R13 R14 R15 R16 R17 R18
         ──
         20   = git grep -c '^### R' <ref> -- GATE-dragons-breath.md
```

> **THE COUNT WAS RE-DERIVED FROM THE COMMAND, NOT ADJUSTED BY THE DELTA OF THIS CHANGE.** It read
> **12** until 2026-09-24; eight rows were added on the rebase onto `d336199` (R11-R13 for the real
> arrow body, R14-R16 for the quiver feedback the Breath inherits, R17-R18 the question rows).
> **A figure maintained by delta is wrong forever and by a growing amount**, so the number above is
> what the command printed.
>
> **AND THE NEW ROWS CONTINUE THE `R` SEQUENCE DELIBERATELY.** `B1`/`Q1` would have read better and
> would have been INVISIBLE to `^### R` — the count would still have said 12 and nobody would have
> looked. A naming scheme that the file's own instrument cannot see is the needle-scope defect this
> repo records, applied to itself.

**GAME MODE: SURVIVAL, for every row unless the row says otherwise.** Declared per the standing
debt in `CLAUDE.md`.

> ### WHY SURVIVAL MATTERS HERE, AND IT IS NOT A FORMALITY
>
> The creative-divergence register's shape is **creative removes a COST**, and this weapon is
> almost entirely made of costs. **`hasInfiniteMaterials()` does not reach it** — the Dragon's Breath
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
>
> **AND THE ELEMENT CHANGED TO FIRE ON 2026-09-22, WHICH ADDED TWO ROWS THAT ARE NOT ABOUT THE
> SPREAD AT ALL.** R9 reads the glyph and R10 reads the burn. Both are consequences of the element
> rather than of the geometry, so a failure in either attributes to `fire.yml` or to `Scorch`,
> **not to this weapon** — which is the whole reason they are separate rows.

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

> ### *** BOOT WITH `--refresh-content`, AND THIS IS NOT OPTIONAL ***
>
> **`dragons_breath.yml` and `dragons_breath_trail.yml` are NEW content files**, and the plugin ships
> content with `saveResource(path, false)`, which **never overwrites an existing file**. A new file
> IS copied, so the weapon would load — **but `dragons_plume.yml` and every other file already in
> `run/plugins/Rpg/content/` would stay at whatever an earlier boot left there.**
>
> **That is not hypothetical on this tree.** `#146` changed `dragons_plume.yml`'s tap speeds and
> cooldowns and `#147` changed nothing in content; a stale deployed Plume would make any comparison
> row between the two weapons read against a Plume that no longer exists. **`GATE-plume-trail.md`
> records this trap stranding a whole slice once already.**
>
> ```
> ./scripts/dev-server.sh --refresh-content
> ```

> ### *** AND THE BUILD LINE IS THE FIRST INSTRUMENT, BECAUSE IT NEEDS NO UNPACKING ***
>
> `#145` (`3ae97ff`) added a line at enable naming the commit the jar was built from. **It did not
> exist when this gate was written**, and it is strictly better than an mtime: it cannot be confused
> with a different jar, and it distinguishes a hand build from a `dev-server.sh` one.
>
> ```powershell
> Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' | Select-Object -First 1
> ```
>
> | it says | means |
> |---|---|
> | this branch's tip | **bound.** Compare it to the tip you meant to boot. |
> | `<hash>-dirty` | built from a MODIFIED tree, so it is **not any commit** |
> | `unknown -- NOT built by dev-server.sh` | a hand build or another tree. **Unbound.** |
>
> **The class and content probes below are still owed**, because this line proves which commit BUILT
> the jar and the probes prove which SYMBOLS are in it — and a checkout of the wrong branch followed
> by a correct build satisfies neither on its own.

| | |
|---|---|
| **Predict** | **The `[Rpg] Build:` line names this branch's tip**, and is not `-dirty` and not `unknown`. |
| **Setup** | Unpack the DEPLOYED jar and probe for a symbol this slice introduces, plus a content key: <br><br>`Copy-Item run/plugins/rpg-<ver>.jar "$env:TEMP\deployed.zip" -Force`<br>`Expand-Archive "$env:TEMP\deployed.zip" -DestinationPath "$env:TEMP\deployed" -Force`<br>`$classes = Get-ChildItem "$env:TEMP\deployed\io\github\butterflysmp\rpg" -Recurse -Filter *.class`<br>`Write-Host "$($classes.Count) class files scanned"`<br>`$classes \| Select-String -Pattern 'SpreadPattern' -Encoding ascii \| Select-Object -ExpandProperty Path`<br>`Select-String -Path "$env:TEMP\deployed\content\weapons\dragons_breath.yml" -Pattern '^\s+count:'` |
| **Predict** | **The class count is NON-ZERO and is reported.** It is the no-op value: `0 scanned` means the unpack failed, and an absence underneath a zero means nothing. |
| **Predict** | `SpreadPattern` is found in **at least two** class files — `core/combat/SpreadPattern.class` and `core/ability/CastExecutor.class`, which calls it. A **0** means the deployed jar predates this slice, whatever its mtime says. |
| **Predict** | `dragons_breath.yml` is present in the jar and `count:` matches. A missing FILE and a missing KEY are different failures: the first is a deploy that did not run, the second is a jar built before the spread block was authored. |
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
| **Setup** | `/rpg give dragons_breath`. Face a flat wall about 10 blocks away, in the open, and fire ONE press. Count the arrows in flight, or the impacts on the wall. |
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
| **Predict** | Six impacts around a seventh, evenly spaced. At 10 blocks the pattern is about **1.05 blocks** across (`2 x 10 x tan(3°)`). |
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
suite's only witness is one row in `DragonsBreathContentTest` asserting the parsed block is null —
measured: `MUT14HOMING` reddens that row and nothing else in 2153 tests.

| | |
|---|---|
| **Setup** | Stand a mob in the open. Aim to one side of it, so the centre arrow passes within a couple of blocks and misses. Fire one press. |
| **Predict** | **Every arrow flies straight.** Nothing bends toward the mob; the misses stay missed. |
| **Predict** | **If an arrow curves, a homing block reached the deployed content** — compare against `dragons_plume`, which is the only weapon in the tree that may author one. |
| **Predict** | **CONTROL, in the same session:** `/rpg give dragons_plume`, draw fully, and fire past the same mob. **Its arrows DO seek.** Without this, "nothing curved" is satisfied by a build where homing is broken for everything. |
| **READ** | _(NOT RUN)_ |

---

### R5 — The tooltip reads `Fire Damage: 9 x 7`

| | |
|---|---|
| **Setup** | `/rpg give dragons_breath` and read the item tooltip. Hold it; do not open a menu. |
| **Predict** | A line reading exactly **`Fire Damage: 9 x 7`** — element-labelled, the PER-ARROW number, the count beside it, **ONE space each side of the `x`** per slice 12d. |
| **Predict** | **`63` appears NOWHERE on the tooltip.** The full-hit total is true of no single arrow, and folding it in is the defect the per-shot rule exists to prevent. |
| **Predict** | **NO `Attack Speed` line and NO `Ranged Damage` line.** This weapon's payload is a literal, so it renders an ABILITY block rather than a stat block. **That is correct, not a regression** — and it is the visible half of the "two damage-rendering shapes" finding. |
| **Predict** | `Quiver: 5/5` on a freshly given item. **`--/5` means the mint path did not stamp it** — absence renders as dashes, never as `0`. |
| **Predict** | `Cooldown: 1.6s`. |
| **READ** | _(NOT RUN)_ |

---

### R6 — The quiver reads 5, drops by ONE per press, and reloads after the fifth

**SOLE WITNESS FOR THE ONE-ROUND RULING IN THE FIELD.** `QuiversSignatureTest` pins the source
expression, but the spend itself needs a live `Player` and `ItemStack`, so no module can execute
it. **Measured 2026-09-21: before that guard existed, changing the spend from 1 to 7 left the
entire suite green at 2147.**

| | |
|---|---|
| **Setup** | SURVIVAL. `/rpg give dragons_breath`. Read the tooltip, then fire four presses, reading the quiver line after each. Then hold left-click to reload. |
| **Predict** | The count goes **5 → 4 → 3 → 2 → 1 → 0**. **ONE per press, though seven arrows leave.** |
| **Predict** | **If it drops by SEVEN and the weapon is empty after one press**, the spread was wired as a yaw FAN — `yawOffsets.length` rounds instead of one. That is the defect this row is the field witness for. |
| **Predict** | The sixth press is REFUSED and says so. A quiver weapon that fires on empty has lost its gate. |
| **Predict** | Left-click reloads, takes **2.00 seconds** (`reload_ticks: 40`), and restores **5**. |
| **Predict** | **CREATIVE DIVERGENCE — do not read this row in creative.** A magazine is a cost, and the register's shape is that creative removes costs. |
| **READ** | _(NOT RUN)_ |

---

### R7 — A full seven-arrow hit deals 63, and the arithmetic is at gear score 100

| | |
|---|---|
| **Setup** | Stand a mob **within about 5 blocks** — point blank, where the whole pattern fits a target. Confirm the weapon's gear score is **100** before reading (`/rpg gearscore`, or give a fresh one). Fire ONE press and read the damage numbers. |
| **Predict** | **Seven damage numbers of 9**, totalling **63**. |
| **Predict** | **If fewer than seven numbers appear, the range was too long** — that is the weapon working, not failing. Re-take it closer. At 5 blocks the pattern is 0.52 blocks across and the whole hexagon still fits a player-sized target out to 5.72 blocks; at 10 it is 1.05 and it no longer does. |
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

### R9 — Seven gold triangles come up at once

**THE ELEMENT CHANGED FROM KINETIC TO FIRE ON 2026-09-22, AND THIS IS THE VISIBLE HALF.** kinetic's
`damage_symbol` is the empty string; fire's is `<gold>▲</gold>`. So every damage number this weapon
deals now draws a glyph beside it, and a full hit draws **seven of them in one frame** — which is a
thing nobody has seen, on a channel `fire.yml` says is only settled by looking.

| | |
|---|---|
| **Setup** | Point blank, all seven landing. Fire one press and look at the damage numbers. |
| **Predict** | **Seven gold ▲, one per number.** |
| **Predict** | **The glyph RENDERS rather than boxing.** `fire.yml` records that U+25B2 is in the same block as the proven ◆ but is itself a hypothesis until somebody looks — "which codepoints actually render is a client-font question". **A missing-glyph box is a font finding, not a weapon defect**, and it would appear on every fire weapon equally. |
| **Predict** | **Seven at once is legible rather than a smear.** `DamagePopupManager` jitters each number horizontally so rapid multi-hits cluster instead of stacking; that jitter was tuned for a *rapid* multi-hit, not for seven in ONE frame. **If they overlap into an unreadable pile, that is a finding about the popup jitter** and it belongs to that file, not to this weapon. |
| **READ** | _(NOT RUN)_ |

---

### R10 — The burn is ONE burn, and it is not seven times anything

**MEASURED IN THE SUITE FIRST — this row is the field check on a figure that is already executed.**
`SevenArrowScorchTest` drives the real `ScorchStatus` and finds that seven arrows burn EXACTLY as
much as one: the stack count has no consumer, the cap is overwritten by the most recent applier with
the same number six times, and the window is merely refreshed.

> **SO THE PREDICTION HERE IS A NEGATIVE, AND NEGATIVES ARE EASY TO READ WRONG.** Do not report
> "the burn looked normal" — report the numbers.

| | |
|---|---|
| **Setup** | SURVIVAL. A mob with a big health pool, so the cap binds and the burn is legible — a knell if one is to hand (360 max). Fire ONE press at point blank, then STOP and watch the burn tick out. |
| **Predict** | **Six burn ticks of 4.5**, totalling **27**, over six seconds. At GS 100. |
| **Predict** | **NOT 7 x 27.** If the burn is seven times anything, the stack count has grown a consumer or the cap is accumulating — either would be a change to `Scorch` that this weapon merely revealed. |
| **Predict** | **CONTROL, in the same session:** fire ONE press from far enough that only the centre arrow lands, and watch that burn. **It must be the SAME 27.** Without this the row cannot tell "seven arrows burn like one" from "the burn is too small to see". |
| **Predict** | **A Flint Staff bolt on the same mob burns MORE** — 10 per tick, 60 total — because the cap is half of ONE payload and a 20-damage bolt caps higher than a 9-damage arrow. **Counter-intuitive and measured**: the seven-arrow weapon has the weaker burn. |
| **Predict** | **On an ordinary 20-HP mob every fire weapon burns identically (6 total)**, because the 5% arm gives 1.0 and every cap exceeds it. **Do not read this row on a chicken** — it is a big-target fact and a small target makes all three weapons look the same. |
| **READ** | _(NOT RUN)_ |

---


---

## THE BODY ROWS — ADDED 2026-09-24, BECAUSE THE BODY STOPPED BEING INERT

**This slice was written when a body was a `noPhysics` marker that collided with nothing.** #144
removed that switch. `PLAN-dragons-breath.md` §12 is the walk; these are the rows it owes. **None of
the three can be seen by any unit test** — `spawnBoltMarker` needs a live World.

### R11 — All seven bodies carry the marker tag, so none of them deals a vanilla hit

| | |
|---|---|
| **Setup** | Fire one press point-blank into a mob and watch the damage numbers, then read `latest.log`. |
| **Predict** | **Seven numbers of 9 and nothing else.** No number that is not 9, no vanilla arrow damage on top. |
| **Predict** | **NO `[plume] EntityDamageByEntityEvent` backstop line in the log.** That backstop fires only when a body resolved a hit, which means its `ProjectileHitEvent` cancel did not take — and the cancel keys on the marker tag. **A single untagged body out of seven shows up here and nowhere else.** |
| **Predict** | The structural argument says this cannot fail: a spread makes seven separate `spawnBoltMarker` calls and the tag is set inside it, so there is no arm that tags some and not others. **The row exists because that is an argument and not a measurement.** |
| **READ** | _(NOT RUN)_ |

### R12 — The one-tick hitch, now up to seven times on one target

| | |
|---|---|
| **Setup** | Fire one press at a mob at about **5 blocks**, where the whole hexagon still fits it, and watch the bodies as they reach it. |
| **Predict** | **The bodies pass THROUGH and do not stop.** `stepMoveAndHit` calls `setPos(firstHit.getLocation())` **before** the event is raised, so cancelling cannot stop a body being clamped to the mob's surface for one tick; `driveMarker` sets the velocity again next tick and it flies on. |
| **Predict** | **WHAT IS BEING READ IS WHETHER THE HITCH IS VISIBLE, not whether it happens.** The Plume's note prices it at one body hitching by up to its per-tick step — 2.5 blocks here. **Seven bodies can hitch on the same mob in the same tick**, and this weapon's designed range is exactly where all seven are still in the cone. |
| **Predict** | If it reads as a visible stutter or a momentary cluster at the mob's surface, **that is a finding about the shared body, not about this weapon** — the Plume has it too, one body at a time. |
| **READ** | _(NOT RUN)_ |

### R13 — Point blank: bodies spawning inside or against a mob

| | |
|---|---|
| **Setup** | Stand **touching** a mob — inside its hitbox if you can — and fire one press. Then fire one with the mob against a wall directly behind it. |
| **Predict** | **Still seven damage numbers of 9.** The tag is set at spawn, before the body ticks at all, so a hit raised on the very first tick is cancelled like any other. |
| **Predict** | **No arrow is left stuck in the world and none is pickable.** A body that reaches the wall sticks, and the armed lifetime discards it on its first in-ground tick; pickup is `DISALLOWED` regardless. |
| **Predict** | **No `PlayerPickupArrowEvent` warning in the log**, walking over the spot afterwards. |
| **Predict** | **THE PLUME'S BOOTS DO NOT COVER THIS.** They were taken at range, and *"close range is the whole weapon"* is this one's own tooltip. |
| **READ** | _(NOT RUN)_ |

---

## THE QUIVER-FEEDBACK ROWS — WHAT THE BREATH INHERITS FROM #147

**Every quiver surface keys on `WeaponDefinition.hasQuiver()`, which is `quiverSize > 0`.** The Breath
authors `quiver_size: 5`, so it inherits all of them **by construction rather than by wiring** — there
is no per-weapon opt-in anywhere in the path. These rows read that the construction holds.

> **ONE INHERITANCE IT DOES *NOT* GET, AND IT IS WORTH SAYING.** `PlumeDraw.cap`'s settle is
> **draw-weapon only** — `drawWeapon` filters on `material: bow` AND on the weapon binding no
> `right_click`. The Breath is `material: crossbow` binding `right_click`, so `PlumeDraw` never sees
> it. **Its settles come from the reload cue, the stats bar, and the shot path** — three routes, none
> of them the Plume's.

### R14 — The HUD field reads the Breath's 5-round magazine

| | |
|---|---|
| **Setup** | Hold the Dragon's Breath and read the action-bar stats line. Fire once. |
| **Predict** | **`➹ 5/5`** between health and defense, in aqua. Then **`➹ 4/5`** after one press — **one round for seven bodies**, which is the spread-is-not-a-fan rule, guarded by `onePressSpendsOneRoundUnlessItIsAYawFan`. |
| **Predict** | Switch to a sword: **the field disappears.** Switch back: it returns. |
| **READ** | _(NOT RUN)_ |

### R15 — The sweep, the reload sound, and the settle all reach it

| | |
|---|---|
| **Setup** | Empty the magazine, left-click to reload, and watch the Breath's hotbar slot and the action bar. |
| **Predict** | *"Reloading..."* on the **action bar** with the crossbow loading-start sound, and the cooldown **sweep** wiping across the Breath's icon over the reload's length. |
| **Predict** | **One `loading_end` click as the sweep empties, and the HUD reads `➹ 5/5` ON that click** — not on the next action. That is #147's cue settle, reached here through the same `Quivers.beginReload`. |
| **Predict** | **Only the Breath sweeps.** Put another quiver weapon in the hotbar: its icon is untouched. The cooldown group is per weapon id. |
| **READ** | _(NOT RUN)_ |

### R16 — The settle survives switching away mid-reload

| | |
|---|---|
| **Setup** | Start a reload, switch to another hotbar slot before it matures, wait past maturity, switch back. |
| **Predict** | The HUD reads **`➹ 5/5` within half a second** of switching back. Nothing settled it while it was unheld — the cue declined because a different weapon was in hand — so **this is the stats-bar settle doing it**, which is #147's `StatsBarSystem.heldMagazine`. |
| **Predict** | And the weapon **fires immediately**; no press is wasted re-settling. |
| **READ** | _(NOT RUN)_ |

---

## THE QUESTION ROWS — BEN RULES AT THE BOOT

**These are PROPOSALS, not rulings.** They ship at the values below so the weapon is bootable; the
row is where the number is decided.

### R17 — Reload feel: is 40 right?

| | |
|---|---|
| **Setup** | Fire five presses, run dry, reload, and repeat until you have a feel for the cycle. |
| **Question** | `reload_ticks: 40` is **2.0 seconds**, PROPOSED. Against the neighbours: the Boltor is 60 (3.0s) for 8 rounds, the Plume 60 for 25. **This weapon empties in five presses at 32-tick cooldown — about 1.6s of firing for 2.0s of reloading**, which is the tightest fire-to-reload ratio in the project. |
| **Options** | (a) **32** — a reload no longer than the cooldown between presses; the weapon never really stops. (b) **40** — as shipped. (c) **60** — the project's standing reload, and the Breath becomes a burst weapon with a real pause. |
| **ANSWER** | _(NOT RUN)_ |

### R18 — Knockback feel: is 0.1 right?

| | |
|---|---|
| **Setup** | Fire point-blank at a mob so all seven land, then at range so one or two do. |
| **Question** | `strength: 0.1` is PROPOSED, and its VALUE is what is being asked — **the rule that a travelling ranged weapon authors knockback at all is settled** (operator's ruling, 2026-09-13). |
| **Question** | **READ R7b FIRST.** Whether seven pushes SUM or OVERWRITE changes what 0.1 means by a factor of seven, and the answer decides whether this question is about 0.1 or about 0.7. |
| **Options** | (a) as shipped. (b) too weak to read at all — raise it. (c) too strong at point blank, where all seven land. |
| **ANSWER** | _(NOT RUN)_ |

> ### *** MATERIAL AND NAME ARE FLAGGED, NOT ASKED ***
>
> `material: crossbow` and the name colliding with vanilla's `DRAGON_BREATH` bottle (and with
> `dragons_plume`) are recorded in `PLAN-dragons-breath.md` §11.5 as the operator's to want. **They
> are deliberately NOT question rows**: a gate row asks something a boot can answer, and neither of
> these is answered by looking at the weapon in play.
## WHAT THIS GATE CANNOT SEE, SAID SO IT IS NOT ASSUMED

- **The reload figure is a PROPOSAL.** `reload_ticks: 40` is the only unruled number in the content
  file. R6 reads that the reload takes 2.00 seconds; it cannot say whether 2.00 seconds is right.
- **The knockback magnitude** is R7b's output, not its input.
- **The material collision.** Four weapons now share `crossbow` — `boltor`, `locust`,
  `quiver_stone` and this one. No row here reads it, because there is nothing to read: they are
  genuinely indistinguishable in a hotbar, and custom model data is not in this slice.
- **The ability-block name.** No `name:` is authored, so the tooltip reads
  `Dragon's Breath  Right-Click`. That is a fallback, not a ruling — if it reads worse than a name of
  its own, it is a one-word change.
