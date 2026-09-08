# PLAN — the element content pass

**This stands between the accrual slice and its boot gate.** Not because the code is unfinished, but
because a gate run before it would measure an interim state and any tuning ruling taken from it would
encode that state as a requirement. *A tuning request is a measurement of current behaviour.*

Read `CLAUDE.md` first. The mechanisms this pass tunes are in `cad80f0`..`bc414eb`.

---

## It has TWO jobs, not three

A third — *"author the six missing `damage_symbol` glyphs"* — **was raised in review and does not
exist.** Measured:

```
$ grep -c '^damage_symbol:' content/elements/*.yml
fire 1   kinetic 1   nature 1   undead 1   void 1   water 1   wither 1
```

All seven were authored in `caa462e`. The glyph question that remains is **legibility on a real
client**, which is a gate observation rather than a content job — though it may *produce* a content
edit if a codepoint draws as a missing-glyph box. The candidate set is `✦ ◆ ✿ ☠ ✧ ≈ ✖`, and the yml
comments already mark it as boot-tunable.

---

## JOB 1 — accrued scorch's DURATION. Decide this FIRST; job 2 follows from it.

**This is the blocker, and the strip decision cannot be taken before it.**

A damage effect authors no duration, so accrual uses `Scorch.DEFAULT_DURATION_TICKS = 160` — a
constant that had **no production caller** until `941a0d5`. Eight of the nine explicit sites author
one:

| site | authored | under accrual |
|---|---|---|
| `solar_grenade:40` (burst) | 40 | 160 |
| `solar_grenade:62` (field pulse) | 40 | 160 |
| `emberblade:87` | 40 | 160 |
| `ember_step:30` | 60 | 160 |
| `rekindle:44` | 60 | 160 |
| `solar_lance:35` | 60 | 160 |
| `ability_stone:56` | 60 | 160 |
| `ember_staff:53` | 60 | 160 |
| `flint_staff:131` | 80 | 160 |

**Stripping the nine is therefore a 2x–4x GLOBAL SCORCH DURATION BUFF shipped as a cleanup.** Commit
size is not the objection; a silent global retune is.

### Three shapes

**(a) 160 is the accrual duration; the nine authored values are deleted as redundant.**
One duration everywhere, one source of truth, simplest mechanism. It is also the operator's original
*"scorch lasts 8 seconds"* finally delivered — the per-applier durations only ever existed because
content declared them. **Cost:** the buff above, plus the new sources below, both landing at once.

**(b) the element declares a duration beside `applies_status`.**
Keeps accrual tunable without touching the nine. **Cost:** it is the first step toward the general
effect system explicitly ruled out of the slice — an element declaring a duration is one field away
from declaring a rate, and the line was drawn at *"name the status and nothing else."* Not
recommended, recorded so the option is refused rather than forgotten.

**(c) ACCRUAL IS REFRESH-ONLY — it adds stacks to a burn that already exists and never starts one.**
`ScorchStatus.apply` already branches on exactly this (`active.get(id)` running or not), so the change
is a guard on `ctx.scorch().isScorched(id)`, not new machinery.

> **This dissolves every problem in this document at once**, which is why it is worth real
> consideration rather than a footnote: content keeps full control of *when* a burn starts and *for
> how long*; the eight authored durations keep meaning what they say; there is no double-application,
> because the explicit call starts the burn and accrual only feeds it; and the unasked-for new sources
> below stop being new — a weapon accrues only onto a fire an ability already lit.
>
> **The costs, and they are real.** `applies_status` would no longer *apply* anything — a naming
> problem, and possibly a design one. A fire weapon alone would never burn anything, so Ignite becomes
> unreachable without an ability to light first. And it weakens the bow as a gate instrument (see
> below), since the bow has no ability to light its own target.
>
> Whether "abilities light, weapons feed" is the intended shape is **the operator's call and nobody
> else's.** It is a coherent design, but it is a different one from what the slice was specified as.

---

## JOB 2 — the nine explicit `status: scorch` sites

Every one of the nine has a co-located `element: fire` damage effect, so **none would lose its scorch
if stripped** — verified site by site, not inferred from a count:

```
ability_stone:55  damage 8   <- status :56  |  solar_lance:30  damage 12  <- status :35
ember_staff:49    damage 16  <- status :53  |  rekindle:43     damage 8   <- status :44
flint_staff:116   damage 20  <- status :131 |  ember_step:24   damage 8   <- status :30
emberblade:83     damage 12  <- status :87  |  solar_grenade:36 damage 6  <- status :40
                                            |  solar_grenade:58 damage 2  <- status :62
```

Stripping is mechanical dedup verifiable by a grep, not nine judgement calls. **But under (a) it
carries the duration buff, and under (c) it must NOT happen at all** — the explicit calls become the
only thing that starts a burn.

### The question underneath, which outlives this pass

**Once elements accrue, is `type: status, status_id: scorch` still legitimate content, or is it now a
duplicate of what `element: fire` does?** Under (a) it is a duplicate and should go. Under (c) it is
the *only* way to start a burn and is load-bearing. The answer is not "tidy the nine" either way — it
is a statement about what the two mechanisms are for.

---

## What changes in game the moment this pass lands — the three interim facts

These are already true on the branch and are why the gate waits.

**1. NEW SOURCES.** Scorch now lands where it never did. Audited across all nine fire files, not just
the explicit status sites:

| source | shape | per hit | rate |
|---|---|---|---|
| `emberblade:58` | `weapon_damage` fire, 7 dmg, sweep 0.5 | ~3 stacks | every swing, + ~1 per bystander |
| `hunters_bow:58` | `weapon_damage` fire, 6 dmg | ~3 stacks | **every arrow, `cooldown_ticks: 15`** |
| `solar_grenade:23` | direct hit, 8 dmg | ~4 stacks | already covered by its own burst |

The bow was missed by the first audit and is the highest-rate of the three.

**2. DOUBLE APPLICATION.** `solar_grenade` goes from six applications per cast to twelve, from two
different cap bases, with two different stack rules — the explicit path hardcodes 1, accrual uses
`stacksFor` (up to 10 on the Flint Staff's 20).

**3. THE STACK CHANGE SHIPS INVISIBLE.** Stacks do not scale damage today, so 1-vs-10 is unobservable
— until Ignite reads a 50%-of-max threshold against a count that quietly went up tenfold. **This is
the one that detonates a slice later**, and it is the reason the count matters now rather than then.

---

## The gate, after this pass — and the one row only a human can see

**The bow is the instrument, not a balance problem.** `hunters_bow` is a dev weapon; its ~1.33
applications per second against a 160-tick window means **roughly ten applications inside one burn**.
Nothing else in the repo applies scorch fast enough to witness **refresh-without-burn** on a live
client — `aRefreshDoesNotDealAnUNSCHEDULEDBurn` guards it in `ScorchStatusTest`, and the bow is where
a person can watch it hold.

> **DISCRIMINATING ROW:** shoot a high-HP mob repeatedly and watch the burn's cadence. The tick list
> must stay on its own 20-tick clock. **If re-application adds an off-schedule burn, that is the
> refresh defect visible in the only place a human can see it.**

Under shape **(c)** this row needs rebuilding: the bow could not light its own target, so the mob
must be lit by an ability first and then fed by the bow. Worth noting before choosing, because the
choice costs the cleanest instrument in the slice.

Other rows the gate will want, once the decisions above are made: glyph legibility on all seven; the
armoured-target row that proves accrual is post-mitigation; the `--refresh-content` verification chain;
and `/rpg apply scorch 1 20` onto a live accrued burn.

---

## Order

1. **Operator decides job 1** — (a), (b) or (c). Everything else follows.
2. Job 2 executes that decision across the nine sites.
3. Gate, written against the decision, with the scorch rows valid as tuning inputs for the first time.
