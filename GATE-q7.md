# GATE — Q7, the held-right-click repeat interval

**Status: RUN, 2026-09-12. Q7 IS DISCHARGED after two slices.**

> ## THE ANSWER: THE INPUT FLOOR IS **4 TICKS**, AND IT IS WEAPON-INDEPENDENT.
>
> **`min 4t` on both weapons, on two materials.** That is Q7's number, and the second reading is what
> makes it a fact about the input rather than about a weapon.

### THE TWO READINGS, VERBATIM

```
hunters_bow  (material: bow, cooldown 15, no magazine)
  INPUTS  count 56  window 276t  mean 5.02t  min 4t
  FIRES   count 15  window 272t  mean 19.43t min 16t   COOLDOWN-LIMITED

quiver_stone (material: crossbow, cooldown 11, magazine 9)
  INPUTS  count 30  window 116t  mean 4.00t  min 4t
  FIRES   count 9   window 96t   mean 12.00t min 12t   COOLDOWN-LIMITED
```

**ALL FOUR MEANS RECOMPUTED FROM THE RAW FIGURES — which is why the row demanded them:**

| | window / gaps | = | printed |
|---|---|---|---|
| bow inputs | 276 / 55 | 5.0182 | **5.02** |
| bow fires | 272 / 14 | 19.4286 | **19.43** |
| stone inputs | 116 / 29 | 4.0000 | **4.00** |
| stone fires | 96 / 8 | 12.0000 | **12.00** |

**The instrument is not being believed; it is being checked.** A remembered *"about five a second"*
could not have been.

### THE PRE-RECORDED EXPECTATION WAS WRITTEN FIRST AND IT MATCHED

`PLAN-q7.md` recorded, **before the reading existed** and marked as outside knowledge, that the floor
was expected near **4t** from `startUseItem`'s `rightClickDelay`. **It agrees.**

That does not prove the mechanism — a prior can be right for the wrong reason — but **a prior
recorded in advance and met is worth more than one fitted afterwards**, and the ordering is checkable
in the commit history rather than asserted here.

### `NO REPEAT` DID NOT OCCUR, ON THE BOLTOR'S OWN MATERIAL

**30 inputs on a `material: crossbow`.** Held-repeat works there, so **slice B's ruling 1 is
implementable as stated** and the arm with no control never fired.

**~~The elapsed figure is still owed~~ — DISCHARGED, and the gate's own rule said it would be.** The
staging rule below promised that on an `INPUT-LIMITED` or `COOLDOWN-LIMITED` reading *"the elapsed
figure would have changed nothing, the reading stands, and the gap is closed in the next commit
regardless."* That is this commit. The reading stands unchanged; the instrument no longer has the
blind spot.

`FireCadence.Sample` now carries `sinceLastEventTicks` — LAST event to **now** — and
`/rpg firerate` prints it **on the `NO REPEAT` arm only**. `windowTicks` was not touched: it is still
first-event-to-last, which `MUTWINDOW` defends. The two endpoints sit side by side as separate
expressions so neither can be quietly rewritten into the other.

**AND IT CUTS ONE WAY, WHICH THE READOUT SAYS OUT LOUD.** It is elapsed time, not held time. A
handful of ticks **disqualifies** a `NO REPEAT` reading, because no repeat had the chance to arrive;
a large figure does **not** prove a hold — someone can tap and wait — it only fails to disqualify
one. Necessary, not sufficient. Verified by mutation rather than asserted: measuring it to the first
event instead of the last reddens 1 of 12 rows; measuring it to the last event instead of to now
reddens 3 of 12.

---
**Status: NOT RUN.** The instrument exists; the reading does not. **Only the operator can take it.**

**ONE ROW, AND THAT IS THE POINT.** Q7's entire failure history is being absorbed into a blanket
over sixteen other rows — *"all gates green"* answers every binary beside it and cannot answer this
one, because **Q7 has no green state**. It produces a number that exists nowhere else in the
project, so "green" cannot select an outcome. **A gate with one row cannot be blanketed.**

**Owed since A1 (`GATE-quiver.md`), deferred twice** — A1 carried it forward, A2 carried it forward.
The reason both times was the same: **nobody had built the thing that measures it.**

---

## THE FINDING NOBODY WAS LOOKING FOR: AN AUTHORED `cooldown_ticks` IS NOT THE FIRE INTERVAL

**Both weapons fire slower than they are authored to, and the gap is not noise.**

**THE MODEL THAT FITS BOTH MINIMA EXACTLY.** A fire needs an input AND an expired cooldown, and
inputs only arrive every 4 ticks — so the real interval is **the cooldown rounded UP to the next
input**:

```
ceil(11 / 4) x 4 = 12      quiver_stone measured min 12   EXACT
ceil(15 / 4) x 4 = 16      hunters_bow  measured min 16   EXACT
```

`quiver_stone`'s entire sample is `mean 12.00, min 12` — **zero variance**, because its inputs were
perfectly periodic at `4.00t`. The bow's mean sits at 19.43 between 16 and 20 because its input
stream was not periodic; see the open finding below.

### WHAT THIS MEANS FOR ANYONE AUTHORING A FIRE RATE

- **`cooldown_ticks` has 4-TICK GRANULARITY under held fire.** Authoring **9, 10, 11 or 12 all
  produce 12.** An author who writes 11 has written 12, and nothing in the schema says so.
- **The Boltor at `cooldown_ticks: 14` would fire at 16**, not 14 — every DPS figure derived from 14
  is about **14% high**.
- **ATTACK SPEED IS QUANTISED TOO, AND THIS REACHES A SHIPPED STAT.** `effectiveCooldownTicks`
  divides and rounds, and the result is then rounded up to the next input. On `hunters_bow`:

  | attack speed | effective | fires at |
  |---|---|---|
  | 1.0x | 15 | **16** |
  | 1.1x | 14 | **16** |
  | 1.2x | 13 | **16** |
  | 1.3x | 12 | **12** |
  | 2.0x | 8 | **8** |

  **+10% and +20% attack speed buy NOTHING on that weapon.** The stat sheet moves, the tooltip
  moves, the weapon does not. That is a falsified number shown to a player, which is the class this
  project treats as worst.

### AND IT ANSWERS Q7'S ORIGINAL QUESTION IN THE DIRECTION NOBODY EXPECTED

`GATE-quiver.md` asked: *"If vanilla's held-use repeat is slower than 7 ticks, the dual halving buys
nothing."* **The repeat is 4t — FASTER than 7 — so the halving is not nullified.**

Slice C's 7-tick dual cooldown fires at **8**, its single-wield 14 at **16**: `16 -> 8` is a **real
2x**. But **7 and 8 are the same weapon**, so authoring 7 buys nothing over authoring 8.

**Two slices deferred a number that turned out to UNBLOCK the thing it was threatening.**

---

## THE MODEL HAS ONE UNTESTED BOUNDARY — MEASURE IT, DO NOT MODEL PAST IT

**Neither weapon has a cooldown that is an exact multiple of 4**, so whether a cooldown of exactly
**12 fires at 12 or at 16 is unmeasured.** That is the difference between `>=` and `>` in the
cooldown check, and it decides whether an author who writes 12 gets 12.

**The instrument already exists.** A weapon at `cooldown_ticks: 12` or `16` answers it in one hold.
**Owed, and named here rather than modelled.**

---

## OPEN FINDING: THE TWO MEANS DISAGREE AND THE CAUSE IS NOT ISOLATED

**The FLOOR agrees — 4t on both. The SUSTAINED MEAN does not: 5.02t on the bow, 4.00t on the stone.**

At a clean 4t the bow's 276-tick window would hold **69 intervals; 55 arrived — 80%.** On the stone,
29 expected and **29 delivered**, perfect.

**Two variables differ between the samples and NEITHER is controlled:**

| candidate | |
|---|---|
| **the MATERIAL** | `bow` has a draw animation, `crossbow` a load — different use states |
| **the SAMPLE LENGTH** | 276t against 116t; a longer hold has more opportunity to drop inputs |

**Neither is chosen.** This is exactly the *"two weapons disagreeing is a finding"* case the row was
built to surface — arriving in the mean rather than in the floor, which is why both figures are
printed and neither is derived from the other.

**It does not disturb Q7's answer**: the floor is the minimum, and the minimum agreed.

---

## THE ROW

| | staging | what to record |
|---|---|---|
| **Q7** | `/rpg give hunters_bow`. **Hold right-click for about ten seconds** — genuinely held, not tapped; the instrument cannot tell the difference and one arm depends on it — release, then `/rpg firerate`. | **Every figure the command prints**, verbatim — both lines and the verdict. Then repeat on `quiver_stone`. |

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
| **NO REPEAT** | one input, then nothing, while the button was held. **The client is not re-sending on this material.** Not an instrument fault — but **this arm has no control on this build**, see below before acting on it. |
| **INSTRUMENT FAULT** | more fires than inputs. **Impossible**; a gate cannot fire more often than it is asked to. These words, not a verdict. |

**`INSTRUMENT FAULT` is the control**, and it is why two counters are worth more than one: an
instrument with no reading that would indict it cannot tell you when it is broken — and this slice
is priced on trusting one number nobody has ever taken.

**`NO REPEAT` IS A READING, AND IT IS THE MOST CONSEQUENTIAL ONE.** An item with a use action may
latch into a use state rather than repeat, and the Boltor's material is exactly such an item. If that
is what comes back, **held-repeat does not work on that material and slice B's ruling 1 is
unimplementable as stated** — it needs re-ruling before the slice proceeds. It is a named outcome on
the readout rather than a surprise in prose.

### AND IT IS THE ONE ARM WITH NO CONTROL — WRITTEN DOWN BEFORE THE READING EXISTS

**`NO REPEAT` fires on `count == 1`, and `count == 1` is also what a single TAP produces.** The
instrument cannot see the button; it sees events. *"Held ten seconds and it never repeated"* and
*"clicked once and ran the command"* are **the same reading**, and nothing currently printed
separates them.

Every other arm has a control. `INSTRUMENT FAULT` is itself the control for the pair; `MIXED` guards
the two-weapon case; an absent sample says so in words. **This one has none — and it is the arm that
re-rules the slice.**

> **THE RULE, AND IT IS RECORDED NOW SO IT CANNOT LOOK LIKE A RESPONSE TO AN INCONVENIENT RESULT:**
>
> **If this reading comes back `NO REPEAT`, it is NOT ACTED ON until it has been retaken on a build
> that carries the elapsed figure.** Nobody re-rules ruling 1 on a reading that cannot rule out a
> mis-take.
>
> **If it comes back `INPUT-LIMITED` or `COOLDOWN-LIMITED`, the elapsed figure would have changed
> nothing**, the reading stands, and the gap is closed in the next commit regardless.
>
> **The missing figure:** ticks elapsed between the single input and the command, printed on that arm
> only. A ten-second hold that produced one event shows ~200t; a tap-then-command shows a handful. It
> does not *prove* a hold — someone can tap and wait — but it makes the mis-take visible, and lets
> this row state its staging as a **checkable condition** rather than an instruction nobody can
> verify was followed.
>
> **It must NOT be implemented by changing `window`.** `window` is first event to last, and
> `MUTWINDOW` exists to keep it that way. This is a separate figure on one arm.

**DISCHARGED — and the blockquote above is left exactly as it was written.** Not one word of it is
edited, because it was recorded *before* the reading precisely so that it could not later look like a
response to the result; rewriting it now would spend the only thing it was built to prove. The
discharge is recorded beside the reading instead, at the top of this file.

Every clause of it was met: a separate figure (`Sample.sinceLastEventTicks`), on one arm only (the
`NO REPEAT` branch of `verdictLine`), and `window` untouched — still `lastTick - firstTick`, still
guarded by `MUTWINDOW`. The reading that was actually taken came back `COOLDOWN-LIMITED`, which is
the branch this rule said the figure would not have changed, so **nothing is retaken and nothing is
re-ruled.**

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
