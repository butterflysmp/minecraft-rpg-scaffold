# GATE — Q7, the held-right-click repeat interval

**Status: NOT RUN.** The instrument exists; the reading does not. **Only the operator can take it.**

**ONE ROW, AND THAT IS THE POINT.** Q7's entire failure history is being absorbed into a blanket
over sixteen other rows — *"all gates green"* answers every binary beside it and cannot answer this
one, because **Q7 has no green state**. It produces a number that exists nowhere else in the
project, so "green" cannot select an outcome. **A gate with one row cannot be blanketed.**

**Owed since A1 (`GATE-quiver.md`), deferred twice** — A1 carried it forward, A2 carried it forward.
The reason both times was the same: **nobody had built the thing that measures it.**

---

## THE ROW

| | staging | what to record |
|---|---|---|
| **Q7** | `/rpg give hunters_bow`. **Hold right-click for about ten seconds**, release, then `/rpg firerate`. | **Every figure the command prints**, verbatim — both lines and the verdict. Then repeat on `quiver_stone`. |

**`hunters_bow` FIRST, and the order is load-bearing.** Three gates can hold fires below inputs, in
`WeaponFire.attempt`'s own order: **durability** (`Broken`, `:90`), **the magazine**
(`Empty`/`Reloading`, `:106-116`) and **the cooldown** (`AbilityService:203`). `hunters_bow` has no
`quiver_size`, so on a repaired weapon only the cooldown can gate — which is the only configuration
where `COOLDOWN-LIMITED` means what it says. On `quiver_stone` the magazine empties after nine and
the fire count stops for a reason that is not the cooldown.

**The INPUT number is the answer either way**, and it is why the second reading is worth taking:
**the input floor should be weapon-independent, and two weapons disagreeing about it is a finding.**

### What the command prints, and why each figure is there

- **count, window, mean, minimum — for INPUTS and for FIRES.** `GATE-quiver.md`'s own Q7 text
  requires it: *"Record the raw count and the window, not only the derived interval: the division is
  recomputable, a remembered 'about five a second' is not."*
- **The MINIMUM is the floor Q7 is named for**; the mean is the sustained rate. They answer different
  questions — the floor prices slice C's 7-tick halving, the mean prices slice B's ruling 1
  (*"eight rounds is a few seconds"*). Both are printed, labelled, neither derived from the other.
- **The window runs first counted event to last**, never command-to-command, so the dead time while
  the operator releases the button and types does not drag the mean down over nothing.
- **An empty sample says so in words.** Run it twice and the second call has nothing; `min 0t` would
  read as an infinitely fast weapon rather than as a measurement that did not happen.
- **The weapon and its `cooldown_ticks` are named**, authored and effective — they differ exactly
  when the trigger is a basic attack, which `hunters_bow` is and `quiver_stone` is not.

### THE FOUR VERDICTS, EACH A DIFFERENT FACT

| printed | meaning |
|---|---|
| **INPUT-LIMITED** | inputs == fires. Nothing gated. **That interval IS Q7's answer.** |
| **COOLDOWN-LIMITED** | fewer fires than inputs. **Q7's answer is still the INPUT number**; the gap is what slice C needs before pricing a halving. |
| **NO REPEAT** | one input, then nothing, while the button was held. **The client is not re-sending on this material.** Not an instrument fault — see below. |
| **INSTRUMENT FAULT** | more fires than inputs. **Impossible**; a gate cannot fire more often than it is asked to. These words, not a verdict. |

**`INSTRUMENT FAULT` is the control**, and it is why two counters are worth more than one: an
instrument with no reading that would indict it cannot tell you when it is broken — and this slice
is priced on trusting one number nobody has ever taken.

**`NO REPEAT` IS A READING, AND IT IS THE MOST CONSEQUENTIAL ONE.** An item with a use action may
latch into a use state rather than repeat, and the Boltor's material is exactly such an item. If
that is what comes back, **held-repeat does not work on that material and slice B's ruling 1 is
unimplementable as stated** — it needs re-ruling before the slice proceeds. It is a named outcome on
the readout rather than a surprise in prose.

---

## WHAT WAS STRUCK FROM THE ORIGINAL RECIPE, AND THE ONE CLAUSE THAT OUTLIVED IT

> ~~set `cooldown_ticks` to **1** and `quiver_size` to **60** so the cooldown cannot be the
> limiter, hold right-click for a timed 10-second window, **count the shots**, divide~~

**STRUCK, on two counts.**

**The fixture edits.** `quiver_stone`'s numbers were chosen by an exhaustive collision sweep, and
eight other rows read bare values that depend on it; mutating a swept fixture to take one reading
spends a guarantee those rows stand on. **Counting inputs BEFORE the gate makes both edits
unnecessary — not just the cooldown one, because a magazine cannot limit an input either.** No
content file is touched by this commit.

**The hand-counting.** *"Count the shots"* is why this row was deferred twice: **a row that needs
hand-counting is un-runnable, and an un-runnable row gets deferred.** The instrument counts.

> ### THE SURVIVING CLAUSE
>
> *"…and run it on a **CANCELLED BINDING**, because that is the configuration that ships."*
>
> **This is not part of what was struck.** Held-use cadence is not independent of whether the
> interaction is cancelled, so a reading on an uncancelled binding is a number about a configuration
> nothing ships.
>
> **It is satisfied by construction, with one named exception.** `RpgListeners:485` cancels vanilla
> exactly when `WeaponFire.attempt` returns present. After the input counter the paths are: broken →
> present, quiver refusal → present, `isVanillaDriven` → **empty**, then `return result` — and
> `result` cannot be empty, because `WeaponService.fire` is `weapon.trigger(input).map(...)` and the
> line above has already proven `trigger(input)` present. **So `isVanillaDriven` is the only
> empty-returning path after the counter; there cannot be a second.**
>
> **A ranged weapon cannot reach that exception, and the reason is the CAST SHAPE, not the input.**
> `BasicMelee.isVanillaDriven` is `isBasicAttack(onHit) && cast instanceof CastSpec.Melee` — the
> input is not in the predicate, so nothing stops a weapon binding `right_click` to a Melee cast and
> reaching it. A ranged weapon cannot because its cast is Ray or Projectile. *That* reason stays true
> if someone later binds a melee ability to `right_click`.

---

## AN EXPECTATION RECORDED BEFORE THE READING

Vanilla's client gates held use on `rightClickDelay`, which `startUseItem()` sets to **4 ticks** — so
the input floor is *expected* near **4t, five per second**.

**This is outside knowledge, not repo-derived, and this repository has deliberately refused to assert
it** (`PLAN-quiver.md:656-659`: *"…is unknown and unmeasured"*). It is written down now, before the
number exists, for one reason: **if the reading disagrees, the reading wins and the disagreement is
the finding.** A prior recorded in advance is a prediction; the same prior recorded afterwards is a
thumb on the scale.

**If the floor lands near 4t, slice C's 7-tick dual cooldown buys nothing** — the weapon would
already be input-limited above it, and the halving would be a tooltip claim rather than a mechanism.
That is the question Q7 was always for.

---

## WHAT THIS GATE DOES NOT DO

- **It does not price anything.** `attack_damage`, `reload_ticks` and `cooldown_ticks` are slice B's
  and are proposed *after* this number exists — writing them first is writing against an unmeasured
  floor, which is what produced `MIN_RELOAD_TICKS` twice.
- **It does not witness the instrument's arithmetic.** `FireCadenceTest` does, with a fake clock and
  four run mutations. What needs a server is only that the two counters are reached at all.
- **It cannot be taken by the assistant.** The reading requires a booted world and a held button.
