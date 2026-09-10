# PLAN — the beam origin gap

Mechanism slice. **No content changes.**

> **STATUS: commit 1 landed (`7bf2929`) — the constant, the origin carry, and the core tests.**
> Still owed: the gate file, the four prose corrections, and the CE4/L0 re-runs.
>
> **This document was reviewed and accepted in a chat and existed only there.** It was committed on
> the operator's instruction after `git ls-tree` showed it absent from the branch — **which would
> have been the third instance** of a plan reviewed, acted on, and never committed, after
> `PLAN-cursed-emerald.md` (found by the `--numstat`-reconcile rule's second application) and the
> case that rule was written for. The check that caught it is the one in `CLAUDE.md`: reconcile the
> command's file list against what you touched, in **both** directions — `--diff-filter=A` was
> empty, which correctly proved no new file hid among the modifications and equally proved the plan
> was not added.
>
> **Four amendments arrived after review and are folded in below**, each marked *(amendment N)* so
> the accepted-then-amended history stays readable rather than being smoothed away.

## Context

`GATE-cursed-emerald.md` **CE4** returned a finding, not a pass: the Cursed Emerald's beam is
*"slightly hard to see at both ranges"* (3 and 30 blocks). The corrected diagnosis — recorded in the
gate, named in `NEXT.md` — is that **screen coverage is dominated by the beam's nearest few metres**,
identical at both stagings, so 10× the particle count changing nothing is evidence **for a fixed
near-field cause and against density**.

The remedy is `cfde822`'s `VISUAL_GAP`: **skip the first N blocks.** It fixes the cause for *every*
ray weapon rather than one weapon's multiplier. It reaches the Lapis Staff **by design** — which is
exactly why this cannot be a content slice.

`BeamSamples`' own javadoc (lines 42-50) already flagged this as open: *"cfde822 drew at s = 0 —
literally AT the eye … the Flint Staff's gate found a FLAME at the eye WAS a problem … It is a gate
question, not a settled one."* This slice answers it.

## The premise the gate got wrong, and why

`GATE-cursed-emerald.md:242-244` says `presentAlong` *"takes the aim origin directly"*. **False — it
takes the SEGMENT START.** Verified:

- `launchRay:274-278` — `stepRay(..., aim.origin(), endpoints, 0)`. **`aim` dies here**; neither
  `aim.origin()` nor `aim.direction()` is threaded further.
- `stepRay:335` — the next hop passes `to` as the next `from`. So `from == aim.origin()` **only at
  index 0**; every later segment starts on a chunk plane.
- `ChunkTraversal.segmentEndpoints` returns **far ends only** — the origin is not element 0.

**Why it was wrong is the auditable part:** the claim was checked at one call site **on its first
iteration**, where it is true, and generalised to the loop. Same shape as CE4's own error —
*true of the sample, generalised to the population*.

The other half of that sentence — *no gap concept exists anywhere* — was separately verified and
**stands**.

## THE PROPERTY

> **The gap is a distance from the RAY ORIGIN, and it stays that distance however the chunk walk
> happens to segment the line.**

### Two implementations that violate it — ruled out explicitly

| approach | why it fails |
|---|---|
| **Inside `presentAlong` / `BeamSamples.along`** | Both see only segment-local `from`/`to`. A gap there is re-applied per segment: **a 30-block beam crossing two planes draws three holes.** `BeamSamples` already excludes `from` by one spacing for exactly this reason and must not gain a second, larger skip. |
| **Only when `index == 0`** | The gap silently becomes `min(GAP, distance to the first chunk plane)`. **Measured from `addPlaneCrossings`:** a caster at `x = 32.4` facing `-x` has a **0.4-block first segment**, so a 1.0 gap collapses to 0.4 — on a full-range shot, not a point-blank one. |

**Both fail by standing position, and CE4's re-run is one observation from one spot** — a control that
succeeds for the wrong reason.

### The approach, and what it is invariant to

Compute the gap boundary **once, from the aim**, and thread it unchanged:

- `launchRay` computes `Vec3 beamStart = aim.pointAt(BEAM_ORIGIN_GAP)` (`Aim.pointAt` already exists).
- `stepRay` gains one **frozen `Vec3`** parameter — safe across the region hop at `:335`, same rule
  the class javadoc already states for `Vec3`.
- Per segment: `drawFrom = beamStart` if `beamStart` lies ahead of `from` along `to - from`
  (`beamStart.subtract(from).dot(to.subtract(from)) > 0`), else `from`. `drawTo` is unchanged
  (`hit.point()` or `to`). If `drawTo` is not ahead of `drawFrom`, the span **collapses to
  `(drawTo, drawTo)`**.

**Invariant to every caster position and aim direction**, because `beamStart` is a single world point
and **no segment boundary enters the comparison.** Specifically the three stagings that break the
rejected approaches: mid-column, 0.4 blocks from a plane facing across it, and a diagonal crossing
x- and z-planes separately.

**Clipping inward preserves `presentAlong`'s contract** (segment must lie in one chunk column):
`beamStart` and `drawTo` both lie within `[from, to]`, which is column-bounded by construction.

### Above the port — stated, with its cost

**The gap is applied in `CastExecutor`, above the `CombatWorld` port.** `FakeWorld.presentAlong`
records `Beam(from, to, visualId)` exactly as handed, so the gap is **witnessable by a core unit test
(2-second loop)** rather than only by a boot gate (60-second loop, operator's time). The port
signature does not change; no sealed type is touched.

Below the port is not merely worse — `PaperCombatWorld.presentAlong` **has no access to the ray
origin**, so it would have to be given one, changing the port anyway *and* losing core visibility.

### The collapse point *(amendment 1)*

The suppressed call passes **the segment's own `from`**, not `drawTo`.
`PaperCombatWorld.presentAlong` hops on `onRegion(from)` — its javadoc's *"the end the caller is
already standing on"*. For any non-final segment `drawTo` is `to`, which lies **on a chunk plane**:
`ChunkTraversal.columnOf(16.0)` is 1 while the segment `[0,16]` is column 0. Collapsing there would
**schedule into a different region to draw nothing** and falsify that javadoc.

The invariant is stated over **what is passed**, not over `beamStart` — in the 32.4 staging
`beamStart` is ahead of `from` but **beyond `to`**, which is exactly why the collapse rule exists.

### Every suppressed segment is called with an empty span — not just point-blank

Per ruling. `stepRay` still calls `presentAlong(drawTo, drawTo, beam)`; `BeamSamples.along` returns
`List.of()` (`count <= 0`), so nothing is drawn.

**This converts an ABSENCE into a PRESENCE.** `presentedAlong.isEmpty()` passes for reasons unrelated
to the gap — the cast never resolved, the fixture authored no beam id so `beam != null` was false, a
cost check tripped. **A fixture wiring nothing passes it.** `size() == 1` positively witnesses that
the ray fired *and* reached the draw site; span length `0` only means something once that is
established. And **the witness is intrinsic**: a paired positive control is a separate assertion that
a later reader deletes as redundant, after which the test silently returns to the foolable version
and still passes.

**Cost, and it is UNMEASURED:** `PaperCombatWorld.presentAlong:547` calls
`ctx.scheduler().onRegion(...)` **before** `BeamSamples.along` returns empty, so **every suppressed
segment schedules a region task that draws nothing** — for a six-shot volley fired point-blank, six
no-op hops per cast. This plan does not claim it is negligible. Either measure it during
implementation and record the figure, or write **"unmeasured"** at the call site.

**A comment goes at the call site** saying the empty-span call exists so a core test can tell
*"suppressed by the gap"* from *"never ran"*, that **nothing in production reads it**, and that
removing it removes the **witness**, not the behaviour. It is otherwise the exact shape of a line a
cleanup pass wins an argument against.

## Core tests — must cross a chunk plane

A ray confined to one column measures the fixture. Staging uses the existing idiom (`Vec3.ZERO`,
`FORWARD`, range 26 → segments `[0,16]`, `[16,26]`).

1. **Gap is origin-relative** — `presentedAlong.get(0).from()` is exactly `BEAM_ORIGIN_GAP` from the
   ray origin. *Positive equality.*
2. **One gap, not one per plane** — `get(0).to().equals(get(1).from())` (contiguity), and
   `get(1).from().x() == 16.0`, **not 17.0**. This is the assertion the per-segment implementation
   fails.
3. **Near-chunk-edge invariance** — caster at `x = 32.4` facing `-x`: segment 0 is 0.4 blocks and is
   **suppressed** (empty span recorded); the gap boundary lands inside segment 1, and the first drawn
   point is still exactly 1.0 from the origin. This is the row the `index == 0` implementation fails.
4. **Suppressed-segment witness** — that staging records a call whose span length is 0, and
   `size()` proves the draw site was reached.

**The contiguity check must filter degenerate spans**, and *a filter's silence is not a result.*
**Control:** run the filter over the staging-3 list, which **contains** a degenerate entry, and assert
the count drops (3 → 2) before its quiet pass is read as contiguity holding.

**Mutations — all FOUR run and observed red, markers grepped both directions, restored from a
scratchpad copy (never `git checkout --`), byte-identical after each:**

| mutation | marker / original | observed |
|---|---|---|
| `BEAM_ORIGIN_GAP 1.0 → 0.0` | `MUTONE` 1 / 0 | **4 reds**, `expected <1.0> but was <0.0>` |
| origin carry → per-segment | `MUTPERSEG` 1 / 0 | anti-hole red at `x = 17.0`; origin property red at 1.4 |
| **collapse point → `drawTo`** *(amendment 1)* | `MUTCOLLAPSE` 1 / 0 | **exactly 1 red**, `32.0` instead of `32.4` |
| **`beamStart` hoisted to once-per-volley** *(amendment 2)* | `MUTHOIST` 2 / 0 | **exactly 1 red, and it is the volley row** |

The last two exist because **an assertion about an assertion is not evidence.** *"Asserted in the
test, so it cannot be silently re-introduced"* and *"a gap hoisted out of the projection fails only
there"* were both claims about guards **never seen fire** — the unreachable-guard shape
`ElementLoader.damageSymbol` is recorded for. `MUTHOIST` also settles the **second** half of *"fails
only there"*: 855 run, **1** failed.

### NAMED ARTIFACTS, DIFFED AGAINST THE BRANCH AT `7bf2929`

*A count against an unnamed set is not an answer.* `+2` is consistent with two, three or four new
methods; only the list distinguishes them.

| planned item | landed as | |
|---|---|---|
| 1 · origin-relative | `theBeamIsDrawnOncePerChunkSegmentOneTickApart` | **folded** — gap asserted at 1.0 from origin |
| 2 · one gap, not one per plane *(amendment 4)* | `theBeamIsDrawnOncePerChunkSegmentOneTickApart` | **folded** — segment two starts at `x = 16.0`, not 17.0 |
| 3 · near-chunk-edge (32.4) | `theGapIsMeasuredFromTheORIGINEvenWhenTheFirstSegmentIsShorterThanIt` | **NEW** |
| 4 · suppressed-segment witness | `theGapIsMeasuredFromTheORIGINEvenWhenTheFirstSegmentIsShorterThanIt` | **folded** into item 3's row |
| amendment 1 · collapse point | `theGapIsMeasuredFromTheORIGINEvenWhenTheFirstSegmentIsShorterThanIt` | **folded** into item 3's row |
| amendment 2 · volley, caster turns mid-burst | `eachShotsBeamStartsOneGapAlongTHATShotsOwnAim` | **NEW** |

**New `@Test` methods: 2. Removed: 0.** Counted from the diff, not from memory:
`git diff a06e271..7bf2929 -- '*Test.java' | grep -cE '^\+    void [a-zA-Z]'`.

**BOTH FOLD TARGETS STAGE A PLANE CROSSING**, which is what makes the folds legitimate rather than
an inherited single-column fixture:

- `theBeamIsDrawnOncePerChunkSegmentOneTickApart` — range 26 from `Vec3.ZERO` down `+x`, crossing
  `x = 16`; the row itself asserts the first segment ends **on** the plane and that a second segment
  is drawn a tick later.
- `theGapIsMeasuredFromTheORIGIN…` — origin `x = 32.4` facing `-x`, endpoints `x = 32.0, 16.0, 6.4`:
  **three** segments, two plane crossings.

**Items 3 and 4 are the guards** — the ones that catch an `index == 0` implementation and prove the
suppressed segment was reached — and both are observed red under mutation below, not merely present.

### Existing core tests this breaks — expected, and they are FIXED not deleted

Found during exploration; **not in the brief**, and the kind of thing that goes missing:

| test | line | why |
|---|---|---|
| `theBeamStopsAtTheBodyItStruckRatherThanAtTheSegmentsFarEnd` | `CastExecutorTest:947` | asserts `Vec3.ZERO, beam.from(), "it starts at the muzzle"` |
| `theBeamIsDrawnOncePerChunkSegmentOneTickApart` | `CastExecutorTest:998` | asserts `Vec3.ZERO` as `get(0).from()` |
| `theBeamStopsAtTheWallItStruck` | `CastExecutorTest:964` | to be checked — same idiom |
| `aVolleyOfRaysDrawsItsBeamONCEPerShot` | `CastExecutorVolleyTest:345` | asserts `from().y() == EYE` — **survives**, a horizontal aim's gap moves `x` only |

Each is updated to assert the **new** truth (start is one gap along the aim), and **the prose moves
with the value** — *"it starts at the muzzle"* becomes false and must be corrected, not left.

## The constant

`BEAM_ORIGIN_GAP = 1.0` in `CastExecutor`, beside `DASH_HIT_RADIUS`.

**Marked PROVISIONAL on the Ignite idiom** — `GATE-beam-gap.md` named as the authority that rules it,
marker removed in a housekeeping commit once the gate runs. *A value still labelled provisional is a
value nobody owns.* Its landing must not be read as approval.

**The ceiling goes at the constant, because nothing enforces it:** *a beam shorter than the gap draws
nothing.* CE4's near staging is **3 blocks**, `GATE-lapis-staff.md` L0's is **~3**, L6's is **5** (all
verified in their gate files). At gap 3, CE4 passes **by drawing zero particles** — indistinguishable
from never having run. `1.0` leaves two blocks at CE4's staging.

## `GATE-beam-gap.md` — G5 is a SET, and existence ≠ effect

Keep **"does the gap exist"** and **"does the gap help"** as separate rows; a tick on one is not the
other. A slice can land with the constant plainly working and the visual no better — a real outcome
the rows must be able to state.

| row | |
|---|---|
| **G5a** | **Lapis muzzle, before/after** — flat wall, ~3 blocks, on **`All` and `Decreased`**, judged against L0's 2026-09-05 reading **quoted in the row**. Record BETTER / SAME / WORSE per setting. **SAME on `All` is a FAILING result**, because `All` is where the clutter was. *This is the real lapis witness.* |
| **G5b** | **CE4 re-run** at 3 and 30, **both settings** — and the row states that **CE4's pre-state records no Particles setting** (absent from that file entirely, while `GATE-lapis-staff.md:39` makes it a precondition), so a "better" verdict is **softer evidence than L0's**. Do not compare across an unknown setting and report a clean improvement. |
| **G5c** | **Lapis L6/L7 regression only — labelled NOT a gap witness.** They observe the beam's END; the gap moves its START. Their passing confirms a paper prediction and carries no information about the gap. |
| **G5d** | The prose corrections below landed **and are reasoned**. |
| **G-pb** | **Point-blank existence row**, paired with a **positive control just beyond the gap where a beam MUST appear**, so "no beam" and "never ran" are distinguishable. |

**The slice does not close until every row resolves and CE4's pointer line is updated.**

## Corrections owed — each says WHY, not just what

1. **`GATE-cursed-emerald.md:242-244`** — the "takes the aim origin directly" clause. Correct in
   place, recording **segment start vs ray origin** and the first-iteration generalisation.
2. **`GATE-cursed-emerald.md` CE4** — becomes a **pointer with no tick box and no result column**, a
   status line rather than a row. **Both files state which is the authority** (G5b holds the row).
   Updating it is **part of G5b's own completion**, not later housekeeping — `GATE-ignite.md` records
   two drift instances in three days from exactly that gap.
3. **`GATE-lapis-staff.md` ~139-155** — three sentences become false: *"the first sample sits exactly
   one spacing off the eye … 0.25 blocks"*, *"a soft coloured blob 0.25 blocks from the eye"*, and
   *"the answer is probably a start offset … NOTHING HAS BEEN BUILT FOR IT"* — **this slice is that
   offset, and the file predicted it.** Also: the L0/L2 coupling's stated baseline is *"a measured
   imperfection rather than a clean muzzle"*; **if G5a comes back BETTER that baseline is void**, and
   a future `samples_per_block` retune starts from somewhere new. Say so where the baseline is stated.
4. **`BeamSamples` javadoc lines 42-50** — **not in the brief.** It calls the near-muzzle skip *"0.25
   BLOCKS"* from the eye and *"a gate question, not a settled one."* After this slice the first sample
   is one spacing past the **gap**, so the figure is wrong and the question is settled. Same class of
   falsified prose, in `core/` rather than a gate file.

## Verification

- `./mvnw -pl core test` — the daily loop; all new rows plus the four updated ones.
- Both mutations above, run and **reported red**, restored from a scratchpad copy (never
  `git checkout --`), with marker greps in both directions.
- `./mvnw test` as the final verify, and **the suite figure re-read from that run**, after the last
  file lands.
- Boot `./scripts/dev-server.sh` only for the gate rows; the gate is a separate session's work.

> **BUILD NOTE — the brief's version is refuted by a measurement already in this repo.**
> `ContentValidator.java:415-427`, dated 2026-07-10, records that `-pl paper -am compile`, a warm
> `-B compile`, and `clean compile` **all three fail identically** on an uncovered sealed arm:
> *"`clean` catches nothing here that a plain build does not. **Do not re-add the claim.**"*
> The real gap it names is different — **`./mvnw -pl core test` never compiles `paper/` at all.**
> Under this plan **no sealed type is touched** (a constant plus a private-method parameter), so
> nothing binds; the final `./mvnw test` covers `paper/` regardless. `clean` is not required.

## Scope

Core constant + the origin carry, core tests, `GATE-beam-gap.md`, and the four corrections.
**No content changes:** `emerald_beam.yml`'s `size: 1.0` / `samples_per_block: 4` are left alone and
`lapis_beam.yml` is not touched.
