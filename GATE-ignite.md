# GATE — Ignite

**This file is the source of truth for the Ignite boot gate.** It is versioned with the code because
for several behaviours below **these rows are the only check that exists anywhere in the project**.
The suite passes with any of them deleted — 1401 tests, and not one of them can see a chain roll
through a pack, a death message name the right player, or an explosion fire twice.

## How to use it

- **NAME THE ROWS YOU ARE ABOUT TO RUN, BEFORE YOU RUN THEM.** A count against an unnamed set is not
  an answer, however precise the number looks.
- A row marked **figure** wants a written observation, not a tick. **A figure row has no checkbox** —
  its text field is what marks it complete, and a blank field is UNRUN, not passed.
- A row marked **SOLE WITNESS** names the behaviour it is the only check for anywhere. Skipping it is
  not reduced confidence; it is zero.

## What the suite already covers, so these rows do not have to

`IgniteTest` — 10 rows, core, on a clock — pins: the fuse lands on tick 20 and not on 19; one blast
per detonation with nothing left scheduled; mob-only; the corpse excluded from its own blast; the
radius straddled at ±0.5; credit to the applier and never to the victim; `fire` + `INERT`; `APPLIES`
not `BYPASSED`; the visual at the detonation point; and `DAMAGE < 20`.

`ElementAccrualTest` adds four rows on the **shared predicate** that Ignite's fire-kill clause and
stack accrual both call: that an `INERT` hit of a scorch-declaring element answers **no**, that a
resolving-but-non-scorch status answers no, and that `forHit` and the ignite clause differ by the
**lethal gate alone**.

**All fourteen were mutation-verified** — each mutation was watched reddening its own row.

**What none of them can see is CHAINING**, because `FakeWorld.Dummy.applyDamage` only decrements a
number: there is no death path in core, and core never learns that anything died. That is `I5`.

---

## Setup

`/rpg spawn knell` — a 360 HP wither skeleton, the gate's usual mob.
`/rpg apply scorch 1 200` — scorch it directly, no weapon needed. The dev path is the only way to
scorch something without also damaging it, which several rows below need.
`/rpg give flint_staff` / `emberblade` — fire weapons, for the rows that want scorch applied the way a
player would.

`Ignite`'s provisional numbers: **fuse 20 ticks, radius 4.0, damage 6.0.** Every expectation below is
written against those; if a row's number is wrong, check the constant before believing the row.

---

## The rows

### I1 — a scorched mob that dies explodes, a second later
`/rpg spawn knell` twice, standing them within 2 blocks of each other. `/rpg apply scorch 1 200` on
the FIRST only. Kill the first (`/rpg mobdamage 400`).

**Expect:** a visible pause — about a second — then `solar_detonation` at the corpse, and the
second knell takes **6**.

**The pause is half the row.** An explosion on the death frame means the fuse was dropped, and
everything downstream (rule 2's serialization, rule 3 being nearly free) rests on it.

**And watch the corpse: NO damage number should appear over it.** The dead mob is excluded from its
own blast, but a death animation and a 20-tick fuse are now the **same order of magnitude**, so
whether the corpse is still present and still tracked when the blast lands is a race server timing
decides. A tracked corpse taking damage renders a number over a body.

> **The marginal timing is why this observation is worth making rather than assuming.** At the old
> 10-tick fuse the corpse was reliably there; at 20 it sometimes will not be — so a missing exclusion
> would produce an **intermittent** artifact, and an intermittent artifact is the kind nobody
> reproduces on demand. This row cannot separate "the exclusion worked" from "the corpse was already
> gone", and does not need to: a number over a corpse is the artifact, and its absence is the pass.

### I2 — the death message names YOU · **SOLE WITNESS for attribution**
Same setup, but bring the second knell low first: `/rpg mobdamage` it to under 6, then scorch and kill
the first so the blast finishes the second.

**Expect:** the death message for the SECOND knell **names you**.

> **THIS IS THE ONLY PLACE RULE 4 IS VISIBLE ON A RUNNING SERVER, AND ITS FAILURE IS SILENT.** If the
> applier were read at detonation instead of captured at death it would be `null` — `forget` has
> already run by then — and the blast would still damage, still kill and still chain. **It would just
> credit nobody.** Nothing errors, nothing logs, and the only symptom is this message.
>
> A death message naming the KNELL rather than you is the slice-1 credit inversion returning.

### I3 — one blast, not two · **SOLE WITNESS for the once-ness guard**
Same as I1, with the neighbour at full health. Read the neighbour's damage number.

**Expect exactly `6`. NOT `12`.**

> **NOTHING IN THE SUITE CAN SEE THIS.** A double delivery of `EntityDeathEvent` cannot be
> constructed in a unit test, so deleting the `forget` guard in `onEntityDeath` leaves 1401 tests
> green. `6` against `12` is one number apart on a nameplate and it is the whole check.
>
> **Known in advance rather than discovered afterwards**, which is why this row was written as the
> guard landed. The API permits a re-fire — `EntityDeathEvent` is cancellable with `setReviveHealth`,
> so a cancelled death revives the same mob — and whether a single death can dispatch twice on this
> build is exactly what this row measures.

### I4 — the broadened trigger: it does not care what killed it · **SOLE WITNESS**
Three runs, one neighbour each, scorch applied by `/rpg apply scorch 1 200`:

- **`/kill`** the scorched knell
- **drown** it (push it underwater and wait)
- **fall** — drop it from a height

**Expect the blast in ALL THREE.**

> **THIS IS THE RULING ITSELF, AND IT IS THE HALF NO UNIT TEST REACHES.** `VOID` and `KILL` are PASSed
> by `VanillaDamagePolicy`, so custom HP never reaches zero and `MobDeathSystem` never fires — the
> whole reason Ignite hooks `EntityDeathEvent` rather than the `reachedZero` seam. **If any of the
> three fails to ignite, the hook is on the seam and the other two are passing by accident.**

### I5 — a pack cascades, serialized in time · **SOLE WITNESS for rule 2**
Spawn four knells in a loose cluster, all within ~3 blocks of a neighbour. `/rpg mobdamage` them all
to under 6 so a single blast is lethal. Scorch **all four**. Kill one.

**Expect:** a **rolling wave** — one blast, a second, the next, a second, the next. **Four links is
four seconds end to end**, so this is slow enough to count deliberately: four separate detonations,
NOT a single screen-clear.

> **THE SERIALIZATION IS THE SAFETY RULE, NOT THE AESTHETIC.** Rule 3 ("each fires exactly once,
> against current state") is nearly free *because* the chain unrolls through time. If all four land on
> one frame, the delay is not being applied per-link and rule 3 stops being free.
>
> **This is also the runaway check.** Core gets `FakeWorld`'s trip-wire for free; a real server gets
> this row. If the wave does not terminate, stop and `/kill @e` before anything else.

### I6 — a player standing in the blast takes nothing
Stand inside the radius of I1's detonation.

**Expect:** no damage, no knockback, no fire.

**Accepted inconsistency, on the record:** a `solar_grenade`'s burst WOULD hurt you here. Ignite is
mob-only and a burst is not, deliberately — see `Ignite.detonate`'s javadoc for why a cascade and an
aimed burst are different things.

### I7 — an unscorched mob killed by something NON-FIRE does not ignite · **the negative control**
`/rpg give ironblade` (kinetic). `/rpg spawn knell`, do **not** scorch it, kill it with the ironblade
beside a neighbour.

**Expect: NOTHING.** No visual, no damage to the neighbour.

> **THE CONTROL GOT STRICTER WHEN THE TRIGGER WIDENED, NOT WEAKER.** It used to read "an unscorched
> mob does not ignite" — which a build that ignited on **every fire kill** would still pass, since
> the old fixture never specified the weapon. Now it discriminates between *"ignites on any death"*
> and *"ignites on a fire death"*, which are different bugs. **The weapon must be non-fire, or the
> row has stopped being a control.**
>
> Without it every other row on this page is unfalsifiable: a build that ignited on every death would
> pass I1 through I5 perfectly.

### I9 — a mob ONE-SHOT by fire, never scorched, explodes · **SOLE WITNESS for the fire-kill clause**
`/rpg give flint_staff`. `/rpg spawn knell`, `/rpg mobdamage 355` so it sits at 5, beside a
neighbour. Do **not** scorch it. Kill it with one bolt.

**Expect:** the blast fires — the neighbour takes **6** — even though the knell was never alight for
a single tick.

> **NOTHING ELSE ON THIS PAGE REACHES THIS CLAUSE**, and no unit test can: the trigger lives in
> `BukkitCombatant`, past the seam, and needs a real entity dying from a real hit.
>
> **It is the operator's rule** — *"a mob killed by a fire weapon should ignite even though it hasn't
> had time to scorch yet"* — and it works WITHOUT loosening the accrual gate: no stacks are granted
> and no `ScorchStatus` entry is created, so nothing is left behind to leak. The kill is credited to
> **you**, because a mob that was never scorched has no lighter and the killing blow *is* the fire.
>
> **The failure that looks like a pass:** if the clause read the element without the `AccrualRule`,
> this row would still pass — and the cascade would recruit every mob it killed. That is `I10`.

### I10 — a blast kill does NOT recruit · **SOLE WITNESS for ruling 2 under the new clause**
Two knells at full health beside a third that is scorched and low. Kill the scorched one so its blast
lands on both neighbours — then bring one neighbour to just under 6 first, so **the blast itself
kills it**.

**Expect:** the blast kills that neighbour and **NOTHING further happens.** No second detonation, no
wave.

> **THIS IS THE ROW A NAIVE IMPLEMENTATION FAILS AND NOTHING ELSE CATCHES.** The blast wears
> `element: fire` — it must, for the glyph — so *"killed by a fire hit ignites"* read literally means
> **a blast that kills an unscorched mob ignites it**, and the cascade recruits everything it kills.
> The terminator stops being "the set you lit" and becomes "you run out of mobs".
>
> The discriminator is `AccrualRule`: the blast passes `INERT`, every weapon hit passes `ACCRUES`.
> A build missing that clause passes **I1 through I9** and fails only here — in a spawner or a farm,
> as a room-clearing chain nobody asked for.

### I11 — scorched AND fire-killed is still ONE blast · **the two-site guard**
`/rpg apply scorch 1 200` on a knell, then kill it **with the flint staff** beside a full-health
neighbour. Both clauses are now true at once.

**Expect exactly `6`. NOT `12`.**

> **The trigger deliberately lives in two places** — `onEntityDeath` for the scorched clause,
> `BukkitCombatant` for the fire-kill clause — because their domains are disjoint: the adapter cannot
> see a `/kill` or a drowning, and the listener cannot see what the killing blow was made of.
>
> **This row is what proves they do not overlap.** The suppression works only because `isScorched` is
> read **before** the damage call: the whole death chain fires synchronously inside it, so a read
> taken afterwards would always see `false` and every scorched mob killed by fire would blast twice.
> `6` against `12`, one number apart — I3's shape on a new path.

### I8 — **figure** — the burn does not double-dip with the blast
Scorch a knell, let it burn to death on its own clock (no killing blow), with a neighbour nearby.

**Observed:** ______________________

Record what the neighbour takes and whether the death message names the applier. A burn kill is the
one path where the scorch's own damage causes the death that triggers the blast, and it is the case
most likely to interact with `forget`'s ordering.

---

## THE REFUSED ROWS

**Each would pass while witnessing nothing.** Recorded because that shape credits coverage that does
not exist.

**1. Defense mitigation on the blast.**
Every mob is defense 0 — `reconcileDefenseModifiers` has ONE production caller, on player worn gear —
and I6 means no player can be caught in a blast. So `APPLIES` and `BYPASSED` produce **the same
number on every target in the game**. No row can separate them.

> **THIS REFUSAL IS PERMANENT, NOT DEFERRED**, and it is a consequence of decision 4 rather than a
> gap. The unit row asserts the FLAG AS DELIVERED, which is the only thing that can see the seam, and
> the recorded reasoning is the only thing defending the choice. **When a choice cannot be witnessed,
> the written reason is all that survives to defend it.**

**2. A player-damaging cascade.**
There is no second account. The mob-only rule means there is nothing to test, and if the rule were
ever reversed, **the row still could not be run** — the S5/S7/S12 shape.

**3. Attribution across two lighters.**
The case where per-mob and propagated attribution actually disagree is *a pack lit by two players,
chained by one kill*. It needs a second account. The unit row covers the mechanism with three distinct
ids; the two-player case is unwitnessable here and is recorded as such rather than left looking
covered.

**4. Whether the corpse is in radius at +10 ticks, as a row of its own.**
Measuring the server's despawn timing is not measuring our code, so there is no row for it. **But
the exclusion it justifies is not a shrug** — a tracked corpse taking the blast emits a
`HealthChange`, which renders a **floating damage number over a corpse**. That is player-visible, so
`I1` carries the observation instead: *no number over the corpse*. What `I1` cannot separate is "the
exclusion worked" from "the corpse was already gone" — and it does not need to, because the guard is
one comparison and the artifact it prevents is the thing anyone would report.
