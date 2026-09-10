# GATE — Ignite

**This file is the source of truth for the Ignite boot gate.** It is versioned with the code because
for several behaviours below **these rows are the only check that exists anywhere in the project**.
The suite passes with any of them deleted — 1406 tests, and not one of them can see a chain roll
through a pack, a death message name the right player, or an explosion fire twice.

## GATE RUN 2026-09-10 — GREEN ON A BLANKET CONFIRMATION, AND THAT IS ALL THE EVIDENCE THERE IS

**Ran against `3352af5`.** Production code is **byte-identical from `8e8731b` through `d288a1f`** —
`git diff 8e8731b..d288a1f -- core/src/main paper/src/main` is empty — so the binary under test is
`8e8731b`'s, and the three commits since it are records and gate text only.

| row | result | evidence |
|---|---|---|
| I1 · I2 · I3 · I4 · I5 · I6 · I7 · I9 · I10 · I11 · I12 | **PASS** | **BLANKET** — one confirmation, *"the gate ran green"*, covering all eleven |
| **I8** *(figure)* | **UNRUN** | **no observation was written down, and a figure row with an empty field is UNRUN, not passed** |

> **THIS IS RECORDED AT THE GRANULARITY IT WAS GIVEN, AND THE GRANULARITY IS ONE SENTENCE.**
> `GATE-vanilla-damage.md` states the rule this file is obeying: *"inflating a blanket statement into
> a specific observation is the failure this file exists to prevent."* Eleven per-row PASS cells with
> figures would be that inflation — **the D1 defect exactly**, which this repo has already paid for
> once (`GATE-element-accrual.md:70`: `1233469 — "all gates green" — D1 skipped, named by him`).
>
> **So the table is per-row in SHAPE and blanket in EVIDENCE, and says so.** Upgrading any row to
> ITEMISED needs a figure from the person who ran it. **Nothing here should be read as a figure.**

### AND TWO ROWS CANNOT ACCEPT A BLANKET AT ALL — THEIR FAILURE MODE IS "LOOKS LIKE A PASS"

This is `GATE-element-accrual.md`'s A3 lesson arriving on a new page. Two rows have an outcome that is
**indistinguishable from success unless someone read a specific thing**:

- **I12** — its own table says *"mob 5 takes **nothing** → **STAGING FAULT**, not a defect. Re-space
  and re-run."* A run where mob 5 was simply out of radius produces four detonations and a quiet fifth
  mob, **which is the pass condition to anyone not checking for the fire glyph.** The row is SOLE
  WITNESS for the depth cap *and* for the capture ordering, so a false pass here covers two
  properties at once.
- **I2** — the death message. A blast that credits nobody still damages, kills and chains. **The only
  symptom is text in the chat log**, and "the gate ran green" cannot say whether it was read.

**Both are OWED an itemised confirmation** — for I12, the glyph on mob 5; for I2, who the death
message named. They are marked PASS above because that is what was reported, and flagged here because
that report cannot discriminate.

## How to use it

- **NAME THE ROWS YOU ARE ABOUT TO RUN, BEFORE YOU RUN THEM.** A count against an unnamed set is not
  an answer, however precise the number looks.
- A row marked **figure** wants a written observation, not a tick. **A figure row has no checkbox** —
  its text field is what marks it complete, and a blank field is UNRUN, not passed.
- A row marked **SOLE WITNESS** names the behaviour it is the only check for anywhere. Skipping it is
  not reduced confidence; it is zero.

## What the suite already covers, so these rows do not have to

`IgniteTest` — 12 rows, core, on a clock — pins: the fuse lands on tick 20 and not on 19; one blast
per detonation with nothing left scheduled; mob-only; the corpse excluded from its own blast; the
radius straddled at ±0.5; credit to the applier and never to the victim; `fire` + **`ACCRUES` with
the link's own depth**; the **fourth link `INERT` and carrying no depth**; every link below it still
recruiting; `APPLIES` not `BYPASSED`; the visual at the detonation point; and `DAMAGE < 20`.

`ScorchStatusTest` pins that **`depth` is DEEPEST-wins while `cap` and `applier` are NEWEST-wins**, in
one fixture where the two rules disagree — the property the chain limit rests on.

`ElementAccrualTest` adds four rows on the **shared predicate** that Ignite's fire-kill clause and
stack accrual both call, including that `forHit` and the ignite clause differ by the **lethal gate
alone**.

**Every one was mutation-verified** — each mutation watched reddening its own row and no other.

**What none of them can see is CHAINING**, because `FakeWorld.Dummy.applyDamage` only decrements a
number: there is no death path in core, and core never learns that anything died. So the suite proves
each blast is delayed, targeted, attributed, flagged and **bounded at the right depth** — and proves
nothing about a cascade. That is `I5` and `I12`.

> **THE CAP HAS A UNIT ROW; ITS DELIVERY DOES NOT.** `IgniteTest` proves `detonate(4)` is terminal.
> **Nothing in the suite proves the death handler ever passes 4** — that needs `depth` captured before
> `forget`, which is paper-side ordering with no unit witness, exactly like the once-ness guard.
> `I12` is the only thing that can see it.

> **AND THE GATE ITSELF ONLY EXERCISES THE FAST LINK — SAID HERE RATHER THAN LEFT ABSENT.**
> `I5` and `I12` soften every mob so each blast is lethal, which is the **one-second** link: die,
> fuse, detonate. **The recruited-survivor path — scorched by a blast, burns six seconds, THEN
> detonates — is exercised only by `I10`, and only for a single link.**
>
> So the worst case the depth cap actually bounds, **four links at up to seven seconds each ≈ 28
> seconds**, has no row and cannot easily get one: it needs four mobs that each survive a blast and
> then die to the burn, staged so no branch outruns another. **An unwitnessed bound that is written
> down reads very differently from one that is merely missing** — the first is a known gap, the second
> is an assumption nobody knows they are making.

---

## Setup

`/rpg spawn knell` — a 360 HP wither skeleton, the gate's usual mob.
`/rpg apply scorch 200 1` — scorch it directly, no weapon needed. The dev path is the only way to
scorch something without also damaging it, which several rows below need.
`/rpg give flint_staff` / `emberblade` — fire weapons, for the rows that want scorch applied the way a
player would.

`Ignite`'s numbers, **ADOPTED 2026-09-10 on this gate's own run**: **fuse 20 ticks, radius 4.0,
damage 6.0, chain depth 4.** Every expectation below is written against those; if a row's number is
wrong, check the constant before believing the row.

> **They were PROVISIONAL until this gate ran, and this file was named as the authority that would
> rule them.** It ran green and no tuning was requested, so they are adopted — **a value still
> labelled provisional is a value nobody owns.** Changing one now is a decision that has to argue
> with `Ignite`'s javadoc, not housekeeping.

### THE DEV BURN'S ARITHMETIC, BECAUSE TWO ROWS ARE STAGED AGAINST IT AND ONE USED TO BE IMPOSSIBLE

`/rpg apply scorch 200 1` **declares no payload**, so the cap falls back to
`Scorch.UNDECLARED_CAP = 2.0`. Against a knell that is:

```
burn   = min(5% of 360, cap 2.0) = 2.0 per second
window = 200 ticks / 20          = 10 burns
total  = 20 damage, over 10 seconds
```

**A DEV-SCORCHED KNELL AT FULL HEALTH CANNOT DIE OF ITS BURN.** 20 against 360. Any row that wants a
burn *kill* has to soften the mob first, and any row that softens a mob and then scorches it is
racing a 2/second clock. Both facts below are consequences of this one number, and neither was costed
when the rows were first written.

### AND THE BLAST NOW SCORCHES ITS SURVIVORS, SO EVERY DAMAGE FIGURE HAS A SECOND HALF

A survivor takes the 6 **and is scorched by it**, at cap `6 × Scorch.CAP_FRACTION(0.5) = 3.0`, for
`damageTicksFor(120) = 6` burns:

| survivor | burn/sec | total |
|---|---|---|
| knell (360, 5% = 18) | `min(18, 3)` = **3** | 6 impact + 18 burn = **24** |
| vanilla mob (20, 5% = 1) | `min(1, 3)` = **1** | 6 impact + 6 burn = **12** |

> **READ THE FIRST NUMBER AND IGNORE THE BURN THAT FOLLOWS.** Every row below that says "takes 6"
> means the **impact**, which lands on the fuse tick. The first burn arrives a second later, so a
> surviving knell shows `6, 3, 3, 3…`. **Without this note the operator sees three numbers and cannot
> tell which one the row is about** — and `6` followed by `3` is not obviously different from a
> doubled `6, 6` glanced at in passing.

---

## The rows

### I1 — a scorched mob that dies explodes, a second later
`/rpg spawn knell` twice, standing them within 2 blocks of each other. `/rpg apply scorch 200 1` on
the FIRST only. Kill the first (`/rpg mobdamage 400`).

**Expect:** a visible pause — about a second — then `ignite_blast` at the corpse, and the
second knell takes **6**.

**The pause is half the row.** An explosion on the death frame means the fuse was dropped, and
everything downstream (**safety rule 2**'s serialization, **safety rule 3** being nearly free) rests
on it.

> **"SAFETY RULE 2" AND "DECISION 2" ARE DIFFERENT THINGS AND THIS PAGE NAMES BOTH.** The safety
> rules are `DESIGN-status-effects.md`'s four — death-gating, serialization, fire-exactly-once,
> attribution — and **all four are intact**. The numbered *decisions* are this slice's rulings, and
> **decision 2 (the blast's accrual rule) was OVERTURNED** on 2026-09-09. They sit one character
> apart, so every reference on this page is qualified.

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

> **THIS IS THE ONLY PLACE SAFETY RULE 4 (attribution) IS VISIBLE ON A RUNNING SERVER, AND ITS FAILURE IS SILENT.** If the
> applier were read at detonation instead of captured at death it would be `null` — `forget` has
> already run by then — and the blast would still damage, still kill and still chain. **It would just
> credit nobody.** Nothing errors, nothing logs, and the only symptom is this message.
>
> A death message naming the KNELL rather than you is the slice-1 credit inversion returning.

### I3 — one blast, not two · **SOLE WITNESS for the once-ness guard**
Same as I1, with the neighbour at full health. Read the neighbour's damage number.

**Expect exactly `6`. NOT `12`.**

> **NOTHING IN THE SUITE CAN SEE THIS.** A double delivery of `EntityDeathEvent` cannot be
> constructed in a unit test, so deleting the `forget` guard in `onEntityDeath` leaves 1406 tests
> green. `6` against `12` is one number apart on a nameplate and it is the whole check.
>
> **Known in advance rather than discovered afterwards**, which is why this row was written as the
> guard landed. The API permits a re-fire — `EntityDeathEvent` is cancellable with `setReviveHealth`,
> so a cancelled death revives the same mob — and whether a single death can dispatch twice on this
> build is exactly what this row measures.

### I4 — the broadened trigger: it does not care what killed it · **SOLE WITNESS**
Three runs, one neighbour each, scorch applied by `/rpg apply scorch 200 1`:

- **`/kill`** the scorched knell
- **drown** it (push it underwater and wait)
- **fall** — drop it from a height

**Expect the blast in ALL THREE.**

> **THIS IS THE RULING ITSELF, AND IT IS THE HALF NO UNIT TEST REACHES.** `VOID` and `KILL` are PASSed
> by `VanillaDamagePolicy`, so custom HP never reaches zero and `MobDeathSystem` never fires — the
> whole reason Ignite hooks `EntityDeathEvent` rather than the `reachedZero` seam. **If any of the
> three fails to ignite, the hook is on the seam and the other two are passing by accident.**

### I5 — a pack cascades, serialized in time · **SOLE WITNESS for SAFETY RULE 2 (serialization)**
**THREE** knells in a **STRAIGHT LINE, 3 BLOCKS APART.** Soften all three to under 6
(`/rpg mobdamage 355`). **Kill the first with a fire weapon.**

**THE GEOMETRY IS A NUMBER, NOT "A LOOSE CLUSTER", AND IT IS DOING REAL WORK:**

```
3 blocks < RADIUS 4.0   -> each blast reaches its neighbour
6 blocks > RADIUS 4.0   -> and does NOT reach the one beyond
```

In a cluster where every mob sits inside radius 4 of every other, **blast 1 kills all its neighbours
at once and they detonate SIMULTANEOUSLY at depth 2** — fewer, fatter waves, and the depth never
climbs. The line is what makes the chain a chain.

> **THREE, NOT FOUR, AND THE CHANGE IS THE POINT OF THE ROW.** At four mobs this yields four
> detonations **whether the depth cap exists or not** — so a build with `MAX_CHAIN_DEPTH` deleted
> passes it exactly as written, and stating the ambiguity in the row does not help: the operator still
> ticks it green. Three tops out at depth 3 and **never approaches the cap**, so this row tests the
> WAVE and `I12` owns the LIMIT. One job each.
>
> **AND THE BURN-RACE WARNING THAT USED TO BE HERE IS DELETED, NOT SOFTENED.** It cautioned that
> hand-scorching four softened knells races a 2/second burn. **Under recruitment you scorch nothing
> at all** — the blast recruits the rest, and killing mob 1 with a fire weapon starts the chain
> through the fire-kill clause. The hazard is gone, and so is the row's last `/rpg apply`: one fewer
> place for the argument-order bug to live.

**Expect:** a **rolling wave** — one blast, a second, the next. **THREE separate detonations across
about three seconds**, slow enough to count deliberately, NOT a single screen-clear.

```
kill A -> detonate(1) kills B -> detonate(2) kills C -> detonate(3)
blast 3 finds nothing alive. Chain ends. Depth tops out at 3.
```

> **THIS EXPECT BLOCK SAID FOUR UNTIL 2026-09-09, AFTER THE ROW HAD ALREADY DROPPED TO THREE MOBS.**
> It was not re-derived when the count changed, so **the row could not pass**: an operator counting
> three against an expected four either reds a correct build, or adds a fourth mob to make the numbers
> agree — **which restores the exact four-mobs-four-links ambiguity the split existed to remove.**
> The failure the split was designed to prevent, arriving from the other side.

> **THE SERIALIZATION IS THE SAFETY RULE, NOT THE AESTHETIC.** Safety rule 3 ("each fires exactly once,
> against current state") is nearly free *because* the chain unrolls through time. If all three land
> on one frame, the delay is not being applied per-link and safety rule 3 stops being free.
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

### I8 — **figure** — the burn does not double-dip with the blast
**Soften the knell to 10 first** (`/rpg mobdamage 350`), then scorch it and let it burn to death on
its own clock — **no killing blow** — with a neighbour nearby.

It dies on the **fifth burn, at about five seconds**, with half the window still in hand.

**Observed:** ______________________

Record what the neighbour takes and whether the death message names the applier. A burn kill is the
one path where the scorch's own damage causes the death that triggers the blast, and it is the case
most likely to interact with `forget`'s ordering.

> **AS FIRST WRITTEN THIS ROW COULD NOT FIRE, AND THAT IS THE WORST KIND OF BROKEN ROW.** It said
> "scorch a knell, let it burn to death" with no softening — but a full knell is **360** and the
> dev-applied burn deals **20 in its entire window**. It cannot die. The row would sit for ten
> seconds and produce nothing.
>
> **On a figure row that is invisible:** there is no expected value for the absence to contradict, so
> a blank observation reads as "ran it, nothing notable" rather than as "this was never possible".
> A checkbox row would at least have gone unticked.

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

### I10 — a blast survivor IS scorched · **SOLE WITNESS for recruitment**
Two knells, **3 blocks apart**. Soften **A** to under 6 and scorch it
(`/rpg apply scorch 200 1`). Leave **B** at full health, unscorched. Kill **A**
(`/rpg mobdamage 400`).

**Expect:** B takes **6**, and then **burns — `3, 3, 3, 3, 3, 3`.** It was never scorched by hand;
the blast recruited it.

> **THIS ROW'S EXPECTATION IS THE EXACT INVERSE OF WHAT IT SAID YESTERDAY, AND THE OLD BODY IS KEPT
> BELOW RATHER THAN OVERWRITTEN.** A row whose expectation flips silently is how a later reader
> concludes the earlier reasoning was never there.
>
> **What it used to say — "a blast kill does NOT recruit", SOLE WITNESS for DECISION 2 (the blast's
> accrual rule, now OVERTURNED — NOT `DESIGN`'s safety rule 2, which is serialization and is
> intact):**
>
> > *"The blast wears `element: fire` — it must, for the glyph — so 'killed by a fire hit ignites'
> > read literally means a blast that kills an unscorched mob ignites it, and the cascade recruits
> > everything it kills. The terminator stops being 'the set you lit' and becomes 'you run out of
> > mobs'. A build missing that clause passes I1 through I9 and fails only here — in a spawner or a
> > farm, as a room-clearing chain nobody asked for."*
>
> **That reasoning was not wrong, and it was not discovered to be wrong.** It was **overturned by
> ruling**, and the ruling supplied the bound it said was missing: `MAX_CHAIN_DEPTH`. Recruitment
> without a cap really would terminate only when the mobs ran out. **The spawner risk was taken
> deliberately and then bounded** — which is why `I12` exists and why deleting the cap on the grounds
> that "recruitment was fine" would reinstate exactly the failure this paragraph describes.

### I11 — scorched AND fire-killed is still ONE blast · **the two-site guard, now the MAIN PATH**
**In this order**, beside a full-health neighbour:

1. **Soften** the knell to 15 (`/rpg mobdamage 345`) — a flint bolt is **20**, so without this the
   staff cannot kill a 360 HP knell and the row cannot fire at all.
2. **Scorch** it (`/rpg apply scorch 200 1`).
3. **One bolt, immediately.** Both clauses are now true at once: it was scorched *and* a fire blow
   killed it.

**Expect exactly `6`. NOT `12`.**

> **THIS ROW WAS WRITTEN IMPOSSIBLE FIRST TIME, exactly like I8** — "kill it with the flint staff"
> against a full-health knell, 20 against 360. Caught while costing I8's arithmetic, which is the
> argument for costing the numbers of every row that stages a kill rather than only the one that was
> reported broken. At 15 HP the burn gives you about seven seconds before it finishes the job
> itself; land the bolt well inside that.

> **The trigger deliberately lives in two places** — `onEntityDeath` for the scorched clause,
> `BukkitCombatant` for the fire-kill clause — because their domains are disjoint: the adapter cannot
> see a `/kill` or a drowning, and the listener cannot see what the killing blow was made of.
>
> **This row is what proves they do not overlap.** The suppression works only because `isScorched` is
> read **before** the damage call: the whole death chain fires synchronously inside it, so a read
> taken afterwards would always see `false` and every scorched mob killed by fire would blast twice.
> `6` against `12`, one number apart — I3's shape on a new path.

### I12 — the chain STOPS at four links · **SOLE WITNESS for the depth cap**
**FIVE** knells, straight line, **3 blocks apart**. Soften mobs **1–4** to under 6
(`/rpg mobdamage 355`). **Leave mob 5 at FULL HEALTH.** Kill mob 1 with a fire weapon.

**Expect four detonations, one per second, walking down the line. Mobs 1–4 die.**

**And the observation on mob 5 is stronger than "it did not explode":**

> **Mob 5 takes a `6` wearing the FIRE GLYPH — and then nothing at all. No burn ticks.**

**THE GLYPH IS THE HALF THAT MAKES THE ROW WORK.** Without it you cannot tell `INERT` from
out-of-radius — both look like a mob standing there, and *"I spaced them wrong"* is the plausible
explanation that gets written in the notes column. **The glyph proves the blast REACHED mob 5 and was
elemental; the absence of ticks proves the rule it carried was terminal.** A blast that missed
produces no number at all.

| observed | means |
|---|---|
| five or more detonations | the depth is not being counted |
| mob 5 takes 6 **and then burns** | depth counted, `terminal()` not applied |
| mob 5 takes **nothing** | **STAGING FAULT, not a defect.** Re-space and re-run |

**Mob 5 is at full health deliberately:** softened, it would die to the terminal blast and there would
be no survivor left to watch for burn ticks.

> **EVERY OTHER ROW ON THIS PAGE PASSES WITH `MAX_CHAIN_DEPTH` DELETED. This is the only row where
> the cap can red** — and it is also the only witness for a second, independent property: **that
> `depth` is captured on the death frame before `forget` runs.** Read at detonation it returns 0,
> every link detonates at depth 1, and the cap never engages — presenting as an unbounded cascade
> with nothing pointing at the read order. **Two safety properties, one row, no unit cover for
> either.**
>
> **One observation covers both halves of `terminal()`, and only because of the implementation.**
> *"Does not scorch"* and *"its kills do not ignite"* are the **same line** — the fire-kill clause
> asks `accruesScorch`, false for INERT. **If that clause ever branches on depth separately from the
> rule, this row silently stops covering the second half and nothing else covers it.**

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
