# PLAN — The Scatter Shot (Slice 14)

**This file is the ACCOUNT. `scatter_shot.yml` and `GATE-scatter-shot.md` are the pointers.** What
is here is the derivation, the arithmetic and the things that were measured rather than reasoned —
including the two mutations that came back green, which are the most useful findings in the slice.

---

## 1. THE WEAPON, AND WHICH NUMBERS ARE RULED

```
id scatter_shot   ranger   rare   crossbow   kinetic
quiver_size 4     cooldown_ticks 32 (1.60s)  damage 9 per arrow
spread count 7    spread angle_degrees 5     NOT homing
```

**RULED:** `quiver_size 4`, `cooldown_ticks 32`, `damage 9`, `count 7`, `angle_degrees 5`,
`element kinetic`, `rarity rare`, `class ranger`, the absence of homing, and the *rule* that a
travelling ranged weapon authors knockback.

**NOT RULED, flagged at their own keys:** `reload_ticks` (proposed, §4), `material`, the knockback
**magnitude** (§5), and the absent `name:`.

---

## 2. WHY THE SPREAD IS A FIELD ON `Projectile` AND NOT A SEVENTH `CastSpec`

`CastSpec.Volley`'s own javadoc asks the next person wanting a new shape to reuse rather than
propose *"a seventh member of a sealed interface that did not need one."* **This slice is that
person, and a field is the answer.**

Volley is the wrong shape and its javadoc says why: it *"re-reads its caster before every shot --
fresh aim, fresh crit roll, fresh price"*, which is a burst the player steers. **A spread is the
opposite: one decision, delivered seven ways, in one frame.**

As a field it composes with `speed`, `gravity`, `trail`, `body` and the absent `homing`, so a
spread of *homing* arrows is expressible with no second mechanism, and
`CastSpec.minimumCooldownTicks`'s exhaustive switch needs no new arm.

> **AND THE SEALED SWITCH DID NOT CATCH THE ONE THING IT EXISTS FOR, WHICH IS WORTH RECORDING.**
> `WeaponLoreLines.deliveredShots` is an exhaustive switch specifically so *"a seventh shape cannot
> skip the question"*. **A count arrived through a FIELD on an existing member instead, and the
> `Projectile` arm still compiled.** Prefer the compile error — and know its limit.

**The field went on the TAIL**, per the ladder's own stated rule: each convenience constructor
drops the tail, never a middle field. `spread` reads more naturally beside `homing`, and putting it
there would have renumbered every existing call site.

---

## 3. THE GEOMETRY, AND THE ONE THING THAT ACTUALLY GOES WRONG

The ring is built in the **shooter's view plane** — Ben's ruling: it looks identical from the
shooter's side at every angle, including straight up.

```
direction_i = normalise( forward + tan(5°) · ( cos φ_i · right + sin φ_i · up ) )
φ_i = 60° · i,  i = 0..5        up = right × forward
```

### 3.1 The basis must be CARRIED, not derived — and the reason is one pitch value

The obvious derivation is `right = forward × worldUp`. **It is correct at every pitch except two.**
It is horizontal and perpendicular to forward's horizontal projection — which *is* the shooter's
right — for all `|pitch| < 90`. At the poles the look vector IS world up and the cross product is
the zero vector.

**The shooter's right depends only on YAW**, so it is defined everywhere. And **yaw is not
recoverable from the direction at the pole**, where both horizontal components are zero — so the
information has to come in from outside or be lost. That is why `Aim` gained a third component and
why `ViewAim` takes a `Location` rather than a direction.

### 3.2 *** THE CONSEQUENCE FOR TESTING, MEASURED RATHER THAN ASSUMED ***

**A fixture staged at the horizon cannot tell the two derivations apart.** Measured over the whole
reactor on 2026-09-21:

| mutation | kill set |
|---|---|
| `MUT14WORLDUP` — basis from world up | **4 rows, all in `SpreadPatternTest`.** `paper` ran fully: 898 tests, zero failures |
| `MUT14COLLAPSE` — ring offset forced to zero | **7 rows, all in `SpreadPatternTest`** |

```
shared by both     theRingSurvivesLookingStraightUp
                   theRingSurvivesLookingStraightDown
                   theHexagonIsStillAHexagonAtThePole
unique to COLLAPSE everyRingBodySitsAtTheAuthoredAngleFromTheAim
                   theRingIsEvenlySpacedIntoAHexagon
                   theRingHoldsItsShapeApproachingThePole
                   theSmallestLegalSpreadStillPlacesItsOneRingBody
unique to WORLDUP  theBasisIsBuiltFromTheRightParameterSoAParallelOneIsRefused
```

**So the two axes do NOT certify each other — each has a unique kill.** But the finding underneath
is sharper: **the three pole rows cannot distinguish them**, because the geometry mutation kills
those too. The only row that dies under a basis mutation and survives a geometry one is
`theBasisIsBuiltFromTheRightParameterSoAParallelOneIsRefused`.

> **AND THAT ROW WAS RENAMED RATHER THAN ANNOTATED, WHICH IS THE PART TO COPY.** It was
> `aRightParallelToTheAimIsRefused` — accurate about its MECHANISM and silent about its VALUE, so
> it read as a routine validation row while being the sole witness of an axis.
>
> **The first fix was a javadoc saying so. That was the wrong fix.** A javadoc explaining that a
> differently-named row is an axis's only guard is **one tidy-up away from deletion by someone who
> reads only the name** — and a pruner reading names is exactly the reader it needs to survive.
>
> `CLAUDE.md` records the shape as *a row can be the only guard of something it does not mention*
> and prescribes marking it **in its own row**. **The stronger remedy is to stop it being unmentioned:
> name the row for what it GUARDS and leave the mechanism to the javadoc.** The account stays; the
> name now carries the load.

**And the near-pole row survived `MUT14WORLDUP`.** Swept at 80, 89, 89.9, 89.99, 89.999 degrees, a
world-up basis is still correct. **The defect is AT the pole, not near it** — a distinction that
matters, because "it degrades at steep angles" would send the next person to guard the wrong thing.

### 3.3 The sign of `up` is UNGUARDABLE, and it is named rather than guarded

Six points spaced 60° apart map onto themselves under reflection — `{0, 60, 120, 180, 240, 300}`
reversed is the same **set** — so flipping `up` produces a byte-identical ring. **No assertion on
the handedness can fail.** Stated in the `HitDamage` manner, where three of four orderings are
UNGUARDABLE rather than unguarded, because the alternative is a test row that cannot redden.

---

## 4. `reload_ticks` — A PROPOSAL, WITH THE METRIC IT RESTS ON

The brief left it unspecified. **40 is proposed and not picked silently**, on the project's own
ammunition metric — ticks per round, which `dragons_plume.yml` uses to price a magazine.

```
                 reload  quiver   ticks/round     cycle     sustained (full hits)
  boltor             60       8          7.50
  locust             60      12          5.00
  dragons_plume      60      25          2.40
  quiver_stone       34       9          3.78
  THIS at 60         60       4         15.00     7.80s         32.31 DPS
  THIS at 40         40       4         10.00     6.80s         37.06 DPS   <- proposed
  THIS at 32         32       4          8.00     6.40s         39.38 DPS
```

**At 60 — the ruled 3.0s that three shipped weapons carry — this weapon's ammunition would be twice
as expensive per round as anything in the project.** A four-round magazine pays the same wall-clock
reload as a twenty-five-round one, so the per-round price is what moves, and it moves a long way.

The cycle arithmetic, with its fencepost written out: **four shots span THREE intervals**, so
`3 × 32 = 96t = 4.80s` firing, plus the reload.

**Ben rules.** `reload_ticks` never reaches the tooltip — `WeaponLore` renders a quiver line and no
reload line at all — so it reaches a player only as the wait itself, which is why a feel judgement
is the right kind of support and why no arithmetic here can settle it.

---

## 5. KNOCKBACK — THE RULE IS RULED, THE MAGNITUDE IS MEASURED AT THE BOOT

`CLAUDE.md`'s standing decision of 2026-09-13 reaches this weapon: it is `type: projectile` and
`class: ranger`, so **silence would be an exception nobody ruled.** It is authored on that basis.

**Everything turns on a fact no reasoning settles: do seven applications in one frame SUM, or does
the last one WIN?** `0.1` is the arithmetically sensible figure under the first reading and very
nearly a no-op under the second.

**The static read predicts SUM:**

```java
// paper/.../adapter/BukkitCombatant.java, applyKnockback
entity.setVelocity(entity.getVelocity().add(v));
```

and its sibling documents the contrast — `applyImpulse`'s javadoc says *"REPLACES velocity rather
than adding to it (unlike knockback)"*. **That is a prediction, not a verdict.** `GATE-scatter-shot.md`
R7b measures it, with a **single-arrow control at the same strength** so seven pushes and one push
cannot read alike.

**The tension no single number resolves**, because the arrow count varies from 1 to 7 with range:

| per-arrow | full hit (if it sums) | single arrow |
|---|---|---|
| 0.057 | ≈ 0.4, the rule's own example | negligible |
| **0.1 — proposed** | 0.7, a real shove | 0.1, a light push |
| 0.4 | 2.8 — launches the target | 0.4 |

*"A full point-blank hit pushes like one ordinary shot"* and *"a single arrow at range pushes like
one ordinary shot"* are **jointly unsatisfiable.**

---

## 6. THE DAMAGE IS A LITERAL — MEASURED, NOT PREFERRED

Every other Ranger weapon authors `attack_damage` + `weapon_damage` and renders a STAT BLOCK. This
one authors a literal and renders an ABILITY block. **Two reasons, both read out of the source:**

**1. The cooldown.** `AbilityService` scales a BASIC ATTACK's authored cooldown by the caster's
attack speed and uses a literal's verbatim:

```java
int cooldownTicks = DamagePayload.isBasicAttack(def.onHit())
        ? AttackSpeed.effectiveCooldownTicks(def.cooldownTicks(), caster.attackSpeed())
        : def.cooldownTicks();
```

Under `weapon_damage`, **both ruled numbers become base values** — 1.6s would be 1.6s only at
attack speed exactly 1.0.

**2. The tooltip would lie.** `rangedAttackSpeedLabel` computes from the AUTHORED cooldown, so the
Attack Speed line would state a rate the weapon does not fire at for every player off 1.0 — the
exact defect class slice 12c closed. Its own javadoc measures the neighbour: across authored 4..40,
**18 of 37** values print a rate the weapon does not deliver.

**And `x 7` is only renderable on this shape at all** — the count is emitted in the ability block,
and the stat block has no slot for it.

### 6.1 Crit reaches a literal — CHECKED, not assumed

`EffectApplier`'s `Damage` arm passes `caster.critMultiplier()` into `HitDamage.dealt` and
`CritState.of(...)` into `applyDamage`, **identically to the `weapon_damage` arm.** So Ben's "one
damage roll for the shot, crit included" holds on this shape. Seven uncrittable arrows was the
risk; it is not the case.

### 6.2 What it costs, said rather than hidden

- **No durability wear per shot.** Not a basic attack. `flint_staff` is the precedent.
- **No Attack Speed line, no Ranged Damage line.** `attack_damage` is not authored: the weapon has
  no basic attack.
- **Gear score scales the 9**: 9 at GS 100, **45 at GS 500**, so a full hit is 63 at 100 and
  **315 at the cap**.
- **OWED FINDING — THE RANGER CLASS NOW HAS TWO DAMAGE-RENDERING SHAPES.** A player comparing two
  crossbows sees `Ranged Damage: 19` on one and `Kinetic Damage: 9` on the other. **Not fixed
  here**: the label follows the mechanism, and rendering "Ranged" over a literal effect would be
  the tooltip describing something other than what produced it.

---

## 7. THE TOOLTIP OVERTURN

`deliveredShots` excluded a spread **by name**, with a stated reason: *"No single target receives
them in full, so `x 3` there would promise damage the player will not receive."*

**Overturned 2026-09-21, and the 5-degree ring is the ANSWER to that objection rather than a
separate decision.** The angle was chosen so a full seven-arrow hit EXISTS:

```
spread diameter = 2 · d · tan(5°)         a player-sized target is ~0.6 blocks wide

  d =  5 blocks   0.87   all seven can land
  d = 10 blocks   1.75   centre and one or two
  d = 20 blocks   3.50   centre only
```

`DrawFan`'s ruled 10° was **rejected for this weapon for exactly that reason**: at 10° the full hit
is unreachable at any range and the old objection would still stand. **So the overturn is
conditional on the geometry**, and a spread authored wide enough to make a full hit impossible
re-opens it.

**The known inconsistency, recorded rather than smoothed over:** a Plume five-arrow release still
renders nothing, because `DrawFan` is not a `CastSpec` and cannot reach the method. Two fans, two
answers — defensible, but **not decided by the same mechanism.**

**A volley of a spread multiplies** (`3 × 7 = 21`). Authorable today, shipped by nothing.

---

## 8. *** THE TWO GREEN MUTATIONS — THE MOST USEFUL FINDINGS IN THE SLICE ***

`CLAUDE.md`: *a green first run is the finding.* Two came back green, and both are recorded as a
PAIR — the green measurement, then the identical mutation re-applied against the new guard.

### 8.1 `MUT14NOSPREAD` — the weapon fired ONE arrow and the suite stayed green

The spread branch in `CastExecutor.launch` was disabled (`if (false)`). **The entire suite stayed
green at 2148 tests.** Every other check kept passing for a reason that made it useless:

| check | why it could not see it |
|---|---|
| `SpreadPatternTest` | calls `SpreadPattern` DIRECTLY — it never asks whether anything calls it |
| `ScatterShotContentTest` | reads the YAML — the block parses and stores whether or not it is used |
| `WeaponLoreLinesTest` | a different method, still saying `x 7` |
| `GoldenLoreTest` | still renders `Kinetic Damage: 9 x 7` |

**The Scatter Shot would have shipped firing a single arrow while its tooltip promised seven**, and
only a boot could have caught it. That is `CLAUDE.md`'s *zero callers on a new accessor* defect in
its purest form.

**`CastExecutorSpreadTest` was then written** and the identical mutation re-applied — same marker
figures, same line delta — and it reddened **3 rows** at 2153 tests.

### 8.2 `MUT14QUIVER7` — a seven-round bill, and nothing could see it

`WeaponFire`'s spend changed from `1` to `7`. **Suite green at 2147.** The path needs a live
`Player` and `ItemStack`, so no module can execute it.

**`QuiversSignatureTest.onePressSpendsOneRoundUnlessItIsAYawFan` was then written** and the
identical mutation re-applied: it reddened, alone.

**The rule it pins, and why the spread does not change it:**

```
a plain press   1 round    every weapon
a YAW FAN       N rounds   the Plume's release -- N arrows, N rounds, ruled
a SPREAD        1 round    the Scatter Shot -- SEVEN bodies, ONE round, ruled
```

**The spread costs one BY CONSTRUCTION**: it is expanded inside `CastExecutor.launch`, below the
commit, so it never produces a `yawOffsets` array and the spend expression never sees it. **Had it
been wired as a fan it would have billed seven rounds and nothing would have said so.**

### 8.3 An instrument lesson from the same pass

**The first two `MUT14NOCOUNT` runs under-reported their kill sets**, and the reason is not a bad
grep: `paper` depends on `core`, so a reactor that fails in `core` **never runs `paper` at all** —
and `-fae` does not help, because the skip is a dependency skip rather than a failure skip. The
absence in `paper` was an artefact of `paper` never running.

**Only `-Dmaven.test.failure.ignore=true` gives a true cross-module kill set.** Every figure in §8
and §3.2 was re-measured with it, and each run's per-module line is quoted to prove all three
modules executed. This is `CLAUDE.md`'s *a zero in the wrong scope is indistinguishable from an
absence*, arriving through the build tool.

---

## 9. FINDINGS THIS WEAPON CREATES IN OTHER FILES

- **`expanded_quiver.yml`'s uniformity argument is falsified.** Its flat `+1/2/3` is *"nearly
  uniform"* across `boltor 8` and `locust 12`; at `quiver_size 4` it is **+25% / +50% / +75%**. The
  table is updated and the claim narrowed — the shape is now known to be wrong at BOTH ends and
  right in between. A second trigger for revisiting is recorded.
- **`/rpg firerate`'s instance list was stale.** It said *"only `boltor` (8) and `quiver_stone` (9)
  carry `quiver_size` at all"* — `locust`, `dragons_plume` and now `scatter_shot` had joined. **The
  roster is removed rather than corrected**, because a hand-maintained list in a guard's javadoc is
  a figure maintained by delta; the grep that answers the real question is given instead.
- **`ExpandedQuiverContentInvariantTest`'s ranger count moved 5 → 6**, and it noticed on the first
  full-reactor run — which is what the constant is for.
- **Punch's roll condition is now SATISFIED by a shipped weapon** for the first time. Recorded,
  dated, in `PLAN-enchants-ranged.md`. **Nothing built.**
- **`DashAim.resolve` was discarding the shooter's right**, and a NAMED LIST is why nobody saw it.
  See below.

---

## 10. *** A NAMED LIST CAN ONLY CHECK THE SITES SOMEBODY ALREADY THOUGHT OF ***

`AimWiringSignatureTest` shipped scanning **four named files** for two-argument `Aim`
constructions, justified on the grounds that *a walk finding nothing would pass*. **Widened to a
walk over every main source in both modules, it immediately found a FIFTH site the list did not
contain:**

```java
// DashAim.resolve, before
Aim dashAim = new Aim(success.aim().origin(), direction);
```

**It threw away the right vector `ViewAim` had just read off the yaw**, one call earlier, and
rebuilt it from the direction it was handed.

**Harmless today and fixed anyway.** A spread is a field on `Projectile`, so no Dash can carry one,
and a dash direction is yaw-only besides. But *"the shape that needs the basis cannot reach this
path"* is a fact about **today's content**, not about the method — and this is a site that hands an
`Aim` onward. `aim.pointing(direction)` carries the field and costs nothing.

> **THE OBJECTION THAT JUSTIFIED THE NAMED LIST WAS REAL AND IS ANSWERED BY A CONTROL, NOT BY A
> SHORTER SCAN.** A walk that finds nothing must not pass — so the row asserts **two** things: that
> a plausible number of files were read (`> 200`, against 325 today), **and** that at least one
> construction was actually inspected. An empty walk and a walk whose needle stopped matching both
> fail loudly, and the scan still reaches everywhere.
>
> **Verified by causing it:** reverting `DashAim` to the two-argument form reddens the row, naming
> the file and line. The guard was shown able to fire on the very site that motivated it.

**The general form: a scan bounded by a list of suspects is bounded by the author's imagination.**
Bound it structurally — a walk plus a size control — and the control does the job the list was
being asked to do.
