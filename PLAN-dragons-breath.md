# PLAN — The Dragon's Breath (Slice 14)

**This file is the ACCOUNT. `dragons_breath.yml` and `GATE-dragons-breath.md` are the pointers.** What
is here is the derivation, the arithmetic and the things that were measured rather than reasoned —
including the two mutations that came back green, which are the most useful findings in the slice.

---

## 1. THE WEAPON, AND WHICH NUMBERS ARE RULED

```
id dragons_breath   ranger   legendary   crossbow   fire
quiver_size 5     cooldown_ticks 32 (1.60s)  damage 9 per arrow
spread count 7    spread angle_degrees 3     NOT homing
```

**RULED:** `quiver_size 5`, `cooldown_ticks 32`, `damage 9`, `count 7`, `angle_degrees 3`,
`element fire`, `rarity legendary`, `class ranger`, the absence of homing, and the *rule* that a
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
direction_i = normalise( forward + tan(3°) · ( cos φ_i · right + sin φ_i · up ) )
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
  THIS at 60         60       5         12.00     9.40s         33.51 DPS
  THIS at 40         40       5          8.00     8.40s         37.50 DPS   <- proposed
  THIS at 32         32       5          6.40     8.00s         39.38 DPS
```

> **RECOMPUTED FOR A FIVE-ROUND MAGAZINE, 2026-09-22.** Every row moved: `ticks/round` is
> `reload/5` rather than `reload/4`, the firing span is FOUR intervals rather than three, and the
> magazine is 315 rather than 252. **The table Ben rules FROM is this one** — a proposal computed
> against a magazine that has since changed is a number nobody should be asked to rule on.
>
> The argument weakened slightly and the proposal did not move: at 60 this weapon's ammunition was
> **15.00** ticks/round against the Boltor's 7.50 — double — and is now **12.00**, still the most
> expensive in the project but no longer by a factor of two.

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
than adding to it (unlike knockback)"*. **That is a prediction, not a verdict.** `GATE-dragons-breath.md`
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
  crossbows sees `Ranged Damage: 19` on one and `Fire Damage: 9` on the other. **Not fixed
  here**: the label follows the mechanism, and rendering "Ranged" over a literal effect would be
  the tooltip describing something other than what produced it.

---

## 7. THE TOOLTIP OVERTURN

`deliveredShots` excluded a spread **by name**, with a stated reason: *"No single target receives
them in full, so `x 3` there would promise damage the player will not receive."*

**Overturned 2026-09-21, and the 3-degree ring is the ANSWER to that objection rather than a
separate decision.** The angle was chosen so a full seven-arrow hit EXISTS:

```
spread diameter = 2 · d · tan(3°)         a player-sized target is ~0.6 blocks wide

  d =  5 blocks   0.524   all seven can land
  d = 10 blocks   1.048   centre and one or two
  d = 20 blocks   2.096   centre only

  the whole pattern fits a 0.6-wide target out to  5.72 blocks
  the whole pattern fits a 0.9-wide target out to  8.59 blocks
```

> **THE RING NARROWED 5° → 3° ON 2026-09-22, AND THIS TABLE MOVED WITH IT.** Every figure above was
> 5°'s until then — the full hit reached only **3.43 blocks** against **5.72** now.
>
> **THIS TABLE IS NOT DECORATION: IT IS THE ARGUMENT THAT LET THE RULE BE OVERTURNED.** A
> justification quoting numbers the content no longer carries leaves the overturn resting on
> arithmetic nobody can reproduce, and the next reader finds a rule reversed for reasons that do
> not check out. **The narrowing makes the argument STRONGER, which is exactly why it would have
> been easy not to re-check** — a figure that moves in your favour is the one nobody audits.
>
> The same table appears in `WeaponLoreLines.deliveredShots`' javadoc, beside the rule it
> justifies. **Both were updated**; two copies of one argument drifting apart is the two-homes
> failure `CLAUDE.md` names, and this one has the rule in one file and the content in another.

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
| `DragonsBreathContentTest` | reads the YAML — the block parses and stores whether or not it is used |
| `WeaponLoreLinesTest` | a different method, still saying `x 7` |
| `GoldenLoreTest` | still renders the damage line with its count |

**The Dragon's Breath would have shipped firing a single arrow while its tooltip promised seven**, and
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
a SPREAD        1 round    the Dragon's Breath -- SEVEN bodies, ONE round, ruled
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
  uniform"* across `boltor 8` and `locust 12`; at `quiver_size 5` it is **+20% / +40% / +60%**. The
  table is updated and the claim narrowed — the shape is now known to be wrong at BOTH ends and
  right in between. A second trigger for revisiting is recorded.
- **`/rpg firerate`'s instance list was stale.** It said *"only `boltor` (8) and `quiver_stone` (9)
  carry `quiver_size` at all"* — `locust`, `dragons_plume` and now `dragons_breath` had joined. **The
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

---

## 11. THE REBRAND, 2026-09-22 — AND THE ELEMENT IS THE ONLY PART THAT BUYS A MECHANISM

`scatter_shot` → `dragons_breath`, "Scatter Shot" → "Dragon's Breath", **element kinetic → FIRE**,
**rarity rare → LEGENDARY**. Zero deployments, so no migration and no legacy arm: the id is renamed
at the source and nothing has ever written it to a PDC.

**Three of the four are cosmetic. The element is not**, and it is the one that was measured.

### 11.1 *** WHAT SEVEN FIRE ARROWS ACCRUE. EXECUTED, NOT REASONED. ***

Kinetic accrues nothing — the anchored sweep `grep -n "^applies_status" content/elements/*.yml`
returns `fire.yml` alone. **Fire buys scorch.** Seven arrows per press is **seven accrual events**,
and a full magazine at point blank is **thirty-five**. Nothing in the tree has ever delivered fire
at that rate, so the plausible reading is that a press stacks scorch seven times faster than the
mechanism was tuned for.

**IT DOES NOT. `SevenArrowScorchTest` drives the real `ScorchStatus` through the real clock and
reads the real burns off the sink:**

> ### SEVEN ARROWS BURN EXACTLY AS MUCH AS ONE.

Three properties of scorch, none of them a property of this weapon:

```
stacks   Scorch.stacksFor(dealt) -- and the count HAS NO CONSUMER. scorch.yml's own words:
         "read ONLY as a yes/no gate ... Stacks do NOT scale the damage and are NOT
         accumulated". Seven applications buy 28 stacks and 28 does nothing.
cap      ScorchStatus: "most recent applier owns the cap". Seven identical arrows
         overwrite it with the same number six times.
window   refreshed by each application. Seven refreshes in ONE tick is one refresh.
burn     min(5% of victim max, cap) every 20 ticks -- reads the CAP and nothing else.
```

**THE COMPARISON, against a knell (360 max), all at GS 100:**

| weapon | per-payload | cap | per tick | total over the 120-tick window |
|---|---|---|---|---|
| **Dragon's Breath** | 9 | 4.5 | 4.5 | **27** |
| Emberblade | 7 | 3.5 | 3.5 | 21 |
| Flint Staff | 20 | 10.0 | 10.0 | **60** |

**So this weapon's burn is the WEAKEST of the three, by more than a factor of two against the Flint
Staff** — because the cap is HALF OF ONE PAYLOAD (`Scorch.CAP_FRACTION`), and **a weapon that fires
seven small payloads caps lower than one that fires a single large one.**

> **AND ON AN ORDINARY 20-HP MOB ALL THREE BURN IDENTICALLY (6 total)**, because the 5% arm gives
> 1.0 and every cap exceeds it. **The comparison above is a BIG-TARGET fact** — quoting it without
> this makes the weapon read as weak everywhere when it is weak only where the cap binds.

**A full magazine is five presses and still ONE burn.** The window is 120 ticks against a 32-tick
cooldown, so presses land inside one another's windows and merely refresh. There is one `Active`
per victim, so thirty-five accrual events produce a continuous burn at the same 4.5 per tick — not
five concurrent ones.

**THE FINDING FOR BEN, STATED AS THE OPPOSITE OF THE WORRY:** the seven-fold delivery buys nothing
in accrual, and the lever anybody tuning this will reach for is the **per-arrow damage**, which
raises the burn linearly. Adding arrows does not raise it at all.

### 11.2 What the element changed that IS visible

- **The glyph.** kinetic's `damage_symbol` is the empty string; fire's is `<gold>▲</gold>`. A full
  hit now draws **seven gold triangles in one frame** — new, and `fire.yml` says that channel is
  only settled by looking. Gate row **R9**.
- **The burn itself.** Gate row **R10**, which is the field check on §11.1's executed figures and
  carries a single-arrow control, because a negative result is easy to read wrong.
- **`ScorchContentInvariantTest`'s site count moved 12 → 13** and noticed on the first full-reactor
  run after the rebrand. **One new SITE that fires SEVEN payloads** — the first content site in the
  tree whose accrual count per press is not one, which is now said in that constant's javadoc so a
  future reader counting fire *hits* is not misled.

### 11.3 LEGENDARY MOVES A PRICE

`AnvilCost` bands on the **target's** rarity, so this weapon goes from RARE (**25 levels / 910 XP**)
to LEGENDARY (**60 / 8670**) — a 9.5× jump to transfer an enchant onto it. **Nothing is changed for
it**: `AnvilCost` already carries the legendary arm. Recorded so the jump is chosen rather than
discovered at an anvil, and pinned by a row in `DragonsBreathContentTest` because a rarity reads as
cosmetic and this one is not.

### 11.4 THE FIRE TRAIL — AUTHORED FIRST, DENSITY PROVISIONAL

`flint_staff.yml` records the convention and this follows it: *"the trail first, deliberately, so
the body could be authored against a stream somebody had actually watched."*

**`dragons_breath_trail` is its own visual, not `flint_trail`**, for the reason `flint_trail`'s own
file gives about `ember_trail`: *"A SHARED VISUAL IS A COUPLING AND THE COUPLING IS INVISIBLE AT
BOTH ENDS."* Its numbers were tuned around ONE bolt on a 24-tick cooldown.

**THE DENSITY, AS A NUMBER, BOTH WAYS:**

```
                       per bolt per tick     a whole press in flight
flint_trail            3  (FLAME 2, SMOKE 1)          3   (one bolt)
dragons_breath_trail   1  (FLAME 1)                   7   (seven bolts)

per bolt:   ONE THIRD of flint_trail's
per press:  2.33x flint_staff's, because there are seven of them
```

**SEVEN CONCURRENT TRAILS IS THE CASE `flint_trail` NEVER HAD TO SURVIVE.** At its density a press
would draw **21 particles per tick** in a cone a few degrees wide — not a stream of seven arrows but
a single bright smear, in which the individual bolts stop being visible at all.

**The smoke is dropped, and that is a decision.** Seven smoke streams overlapping inside a 3° cone
grey out the middle of the pattern: the arrows would be legible at the edges and lost in the centre,
which is the opposite of what a spread needs to show.

> **IT MAY WELL READ THIN, AND THAT IS WHY IT IS AUTHORED FIRST.** One flame per tick is the
> sparsest trail in the project, and the seven-fold overlap that justifies it is exactly the thing
> arithmetic cannot show you. **Raising FLAME to 2 doubles a press to 14/tick**, still under
> `flint_trail`'s 21, and is the obvious next value. **Do not tune the body around this until
> somebody has watched it.**

### 11.5 TWO NAME COLLISIONS, FLAGGED NOT FIXED

- **"Dragon's Breath" is also a VANILLA ITEM** (`DRAGON_BREATH`, the cauldron-collected bottle).
- **The tree now has two "Dragon's" weapons**, beside `dragons_plume`.

Neither is a defect and both are the operator's to want. Written down so nobody reads either as an
oversight — and noting that the two weapons are otherwise easy to confuse in a log: both are
`class: ranger`, both author `body: arrow`, and `dragons_plume` is the only weapon that may author
a homing block while this one is ruled not to.

---

# 12. THE RE-READ AGAINST THE REAL-ARROW BODY — 2026-09-24

**This slice was written when a body was an inert `noPhysics` marker. It is not one any more.** #144
(`16939b6`, `895e5d2`) removed `setNoPhysics(true)` from `PaperCombatWorld.spawnBoltMarker` — measured
rather than reasoned: spike modes B and E differ in that one line, and the operator read B as *"still
has the problem"* and E as *"perfect, exactly what we're looking for"*.

**Every body claim in this slice was walked on the rebase onto `d336199`.** What follows is what
changed, what did not, and — the half that is easy to skip — **what turned out not to have depended
on the body at all.**

## 12.1 WHAT NO LONGER HOLDS, AND IS CORRECTED IN PLACE

**`dragons_breath.yml`'s `body:` note read _"A real arrow, oriented along its travel, INERT IN EVERY
OTHER WAY"_.** The second half is withdrawn. The old text is quoted in the file rather than deleted,
because it was true when written.

A body now **collides with blocks and sticks** (the armed lifetime discards it on its first in-ground
tick), **collides with entities** and raises `ProjectileHitEvent`, is explicitly **pickup-DISALLOWED**,
and carries damage 0 / knockback 0 / not-critical as the fallback behind a cancel that might not
register. Gravity applies and is inert, because `driveMarker` overwrites the velocity every tick.

## 12.2 THE THREE THINGS THAT ARE NEW *BECAUSE THERE ARE SEVEN*

**The Plume has one body per shot. This weapon has seven, and two of the body's properties scale with
that count while one does not.**

- **THE MARKER TAG IS ON ALL SEVEN, BY CONSTRUCTION.** `onPlumeBodyHit` cancels a hit only for a body
  carrying `keys().markerEntity`, so an untagged body deals a real vanilla arrow hit — damage this
  weapon never authored, on a target `castRay` may not have chosen. **A spread produces seven separate
  calls to `spawnBoltMarker`, not one call with seven arguments**, and the tag is set inside it, so
  there is no arm in which some bodies are tagged and others are not. **This is a structural argument,
  not a measurement**, and nothing in either module can see it — `spawnBoltMarker` needs a live World.
  **GATE row B1.**

- **THE ONE-TICK HITCH IS NOW UP TO SEVEN HITCHES ON ONE TARGET.** `stepMoveAndHit` calls
  `setPos(firstHit.getLocation())` **before** raising the event, so cancelling cannot prevent the body
  being clamped to a mob's surface for that tick. The Plume's note prices this at one body hitching by
  up to its 2.5-block step. **This weapon fires seven bodies at 2.5 speed into a 3-degree cone, and its
  own content says seven arrows reach one target only inside about six blocks — which is exactly where
  every body is still in the cone.** So the worst case is not hypothetical: it is the weapon's designed
  range. Nobody has seen it. **GATE row B2.**

- **POINT-BLANK IS A CASE THE PLUME NEVER HAD.** *"Close range is the whole weapon"* is this weapon's
  own tooltip. A body spawned at the shooter's eye, against or inside a mob's hitbox, may raise its hit
  on the **first** tick — before `driveMarker` has run once. The cancel still applies (the tag is set at
  spawn, before the body ever ticks), so the outcome should be a clamp-then-fly rather than a hit. **It
  is not measured and it is not derivable from the Plume's boots, which were taken at range. GATE row B3.**

## 12.3 WHAT DID **NOT** CHANGE, CHECKED RATHER THAN ASSUMED

- **`SevenArrowScorchTest` asserts nothing about the body.** Its six rows are scorch ARITHMETIC — burn
  counts, the half-payload cap, refresh-not-stack, and the comparison against a Flint Staff bolt — all
  driven through `ScorchStatus` against `FakeScorchSink` and `FakeTickTarget`. **It never spawns a body
  and never asserts one is inert**, so #144 does not reach it. Said explicitly because "seven arrows" in
  the name invites the opposite assumption.
- **The spread arithmetic, the ring geometry and the damage table** are all pre-flight: they decide
  direction vectors, and a body is spawned from the result. Nothing in them reads a body's physics.
- **`onePressSpendsOneRoundUnlessItIsAYawFan`** — the quiver-cost guard — is about `WeaponFire`'s spend
  expression, above the body entirely.

## 12.4 A DEFECT FOUND IN THE WALK, IN MASTER, **NOT FIXED HERE**

**`RpgListeners.onPlumeBodyPickup`'s javadoc and its log message both still describe the PRE-#144
body**, and they are the explanation a person reads at the moment that guard fires:

> *"playerTouch's guard is `isInGround() OR isNoPhysics()`, and the body runs noPhysics, so nothing
> else refuses this."*

**The body does not run `noPhysics`.** `spawnBoltMarker` states the corrected mechanism directly:
*"Under the old switch the SECOND disjunct was always true, so the body was pickable in MID-AIR …
Now the second disjunct is always FALSE and the FIRST one goes live the instant the body sticks."*
**The hazard INVERTED — mid-air to in-ground — and this guard's account describes the old half.**

**Not fixed in this slice.** It is master's file, this branch touches it nowhere else, and a hunk in an
unrelated file is how a revision regresses what it was not revising. **The correction is written out
here so whoever takes it has nothing to re-derive:** the two sentences should say the body is pickable
**once it sticks in a block**, and that `DISALLOWED` is what refuses a *stuck* arrow rather than a
flying one. **TRIGGER: the next commit that touches `RpgListeners`' Plume-body handlers at all.**

> **It matters more than a stale comment usually does, because this guard's own javadoc argues that
> its firing IS the detector** — *"a guard that logs when it fires cannot be hollow"*. A detector
> whose message misexplains the mechanism sends the next person to look for a switch that is gone.
