# GATE — Expanded Quiver (the ranged enchant, PR #71)

## **Status: RUN 2026-09-13. All five rows PASS.**

Verdicts read from the gate page's own store at `2026-09-13T22:09:43Z` — `a1`–`a5`, all PASS.

> **THIS FILE DID NOT EXIST UNTIL AFTER THE BOOT, AND THAT IS THE FIRST THING TO KNOW ABOUT IT.**
> Expanded Quiver shipped in `#71` **with no gate file at all** — the slice was merged to `master`
> having never been parsed by Paper, and the omission was recorded in that PR's conversation rather
> than in a gate. The predictions below are transcribed as they were written **before** the boot; the
> file around them was written after.
>
> **So this is a boot record, not a gate that governed a boot.** The distinction is real and it is
> stated once here rather than implied throughout.

---

## ⚠ READ THIS BEFORE THE TABLE: WHAT THIS RECORD DOES **NOT** CONTAIN

**The same two caveats as `GATE-quiver-ammo.md`, and for the same reasons.** Above the table, because
a reader must meet them before the verdicts.

### (i) NO OBSERVED FIGURES WERE CAPTURED. NOT ONE READING.

Every row carries a bare verdict. **No magazine count, no lore line, no refusal text was written
down.** So every observed cell reads **"PASS — figure not captured"**, never a number.

> **NO NUMBER IN AN OBSERVED CELL HAS BEEN RECONSTRUCTED, BY ANYONE.** A figure inferred after the
> fact from what the code *should* have produced is the prediction wearing the observation's clothes,
> and it would make this table self-confirming.

**A PASS WITH NO READING IS A VERDICT, NOT A MEASUREMENT.** This file holds verdicts.

**The cause, in one line so it is fixable rather than repeated:** the tick-through made the verdict one
click and the reading a typing job — **on a gate whose whole doctrine is that the observed column is
filled in beside the prediction.** A defect in the instrument, not in the operator.

### (ii) THE JAR-FRESHNESS CONTROL HAS NO ENTRY AT ALL

**The honest reconciliation, both halves, and this file's half is WEAKER than the ammo gate's:**

- **A-1 through A-3 prove NOTHING about freshness.** Expanded Quiver was already on `master` before
  this build, so all three would pass on **any jar built since `#71`**.
- **A-4 and A-5 are the discriminating rows**, because neither can pass unless **both slices are
  present at once** — the enchant that raises capacity, and the arrows that fill it.
- **That is luck in how the rows were written, NOT evidence that the control was taken.** It was
  specified for the ammo gate, it was not performed, and nothing here says it passed.

> **A control that was not run is not a control that passed, however sound the inference that replaces
> it.**

---

## ROW A-1 — THE ENCHANT LOADS

`/rpg enchant candidate 0 <TAB>` offers **`expanded_quiver`**.

| | prediction | observed |
|---|---|---|
| `expanded_quiver` appears in the completions | **yes** | PASS -- figure not captured |

> **THE COMPLETER IS THE INSTRUMENT, and that is why this row is first.** Its suggestions are built by
> walking `adapters.enchants().all()` — **the loaded registry itself** — so appearing there **IS** proof
> the YAML parsed, the effect name resolved to `EnchantEffect.QUIVER_SIZE`, and the gate accepted
> `class: ranger`. It is not a proxy for those things; it is downstream of all three.
>
> **A FAILURE HERE WOULD HAVE MADE A-2 THROUGH A-5 VOID RATHER THAN FAILED.** An enchant that never
> loaded cannot be refused on the wrong class, cannot raise a capacity, and cannot be rolled — every
> later row would have been measuring an absence and could not have distinguished "the gate works"
> from "there is nothing to gate".

---

## ROW A-2 — THE CLASS GATE HOLDS

The enchant is **refused on a non-ranger weapon**.

| | prediction | observed |
|---|---|---|
| refused on a weapon that is not `class: ranger` | **refused** | PASS -- figure not captured |

> `Gate.RANGER_ONLY`, which is `MAIN_HAND_ONLY` **minus `class: universal`**. That exclusion is the
> whole reason the gate exists rather than reusing the damage one: a universal quiver enchant would
> enter **every sword's roll pool** and sell an XP unlock that does nothing. It is the typo
> `expanded_quiver.yml`'s own comment block names.

---

## ROW A-3 — THE CAPACITY MOVES, AND BY THE AUTHORED CURVE

Boltor (`quiver_size: 8`) at enchant levels 1 / 2 / 3.

| | prediction | observed |
|---|---|---|
| level 1 | **9** | PASS -- figure not captured |
| level 2 | **10** | PASS -- figure not captured |
| level 3 | **11** | PASS -- figure not captured |

> **THE LOCUST WAS OPTIONAL AND NO READING SAYS WHETHER IT WAS TAKEN.** `locust` (`quiver_size: 12`)
> would resolve to **13 / 14 / 15** on the same curve. It was written as an optional second weapon and
> **the record does not say whether it was exercised.** Recorded as unknown rather than assumed either
> way — an optional row with no verdict is not a row that passed.

---

## ROW A-4 — THE TWO SLICES TOGETHER, FOR THE FIRST TIME EVER

Level 3 (resolved capacity **11**), fire dry, hold **exactly 11** arrows, reload.

| | prediction | observed |
|---|---|---|
| magazine | **`11/11`** | PASS -- figure not captured |
| arrows left | **0** | PASS -- figure not captured |

> **THIS IS THE FIRST TIME THE TWO SLICES HAVE EVER RUN TOGETHER, AND IT IS WORTH STATING PLAINLY.**
> `#71` shipped **before arrows existed** — the enchant raised a magazine that refilled for free.
> `#72` was built and mutated **against an unmodified magazine** — every core row stages the Boltor's
> authored 8. **Neither slice's test suite ever saw the other's number.**
>
> This row is the only place `QuiverSize.resolve` and `Quiver.roundsNeeded` have been composed: the
> enchant decides the capacity, and the ammo cost is the gap to *that* capacity rather than to the
> authored one. It is also one of the two rows that could not pass on a stale jar (see caveat ii).

---

## ROW A-5 — CAPACITY DROPPED MID-RELOAD — **⚠ PARKED BY RULING, NOT PASSED CLEAN**

Paid for **11** rounds, then the capacity modifier is removed before maturity; the magazine seats
**8**. **Three arrows are gone.**

| | prediction | observed |
|---|---|---|
| paid | **11** | PASS -- figure not captured |
| seated | **8** | PASS -- figure not captured |
| arrows forfeited | **3** | PASS -- figure not captured |

> ### STATUS: **OBSERVED AND DEFERRED.** Not unnoticed, and not accepted.

**CAUSE.** `Quiver.reload` clamps the rounds **paid for** against the capacity resolved **at
maturity** — `clamp(0 + 11, 8) = 8` — so the difference is forfeited. **It is rulings 3 and 5
interacting, not a bug in either:** ruling 3 takes the arrows at the start, ruling 5 lets a partial
load stand, and nothing refunds a magazine that shrank in between.

**TODAY.** **Unreachable in play.** The only lever that moves quiver capacity is a dev command; **no
shipped gear grants quiver size outside the dev instrument**, so no player can produce this state.

> ### TRIGGER — **THE FIRST SHIPPED ITEM THAT GRANTS QUIVER SIZE OUTSIDE THE DEV INSTRUMENT.**
>
> A **condition on the tree**, checkable by grep, **not "later"**. On that day a player can unequip it
> mid-reload and this becomes reachable, and the forfeit stops being a dev-command curiosity.

### A SECOND UNGUARDED EDGE ON THE SAME PARK — FOUND IN SLICE G, 2026-09-14

**A-5's own state is `loaded = 8`: the clamp happens AT MATURITY, so the three arrows are forfeited
and the magazine is never over-full.** But the capacity modifier can also come off **AFTER** maturity
— and then the stamp still says **11** while capacity resolves to **8**, until the next write
re-clamps it. **That state is representable, readable, and it is not this row.**

**MEASURED: SUCH A MAGAZINE PAYS FOR NINE, NOT ELEVEN.**

```
spendRound   Quiver.spend(11) = 10
setLoaded    Quiver.clamp(10, 8) = 8      <- the surplus is lost at the FIRST shot, not carried
             so: one over-full shot, then a magazine of 8   =  9 total
```

> **AND NOTHING TESTS THAT COMPOSITION.** `Quiver.clamp` has rows and `Quiver.spend` has rows; the
> **nine is a property of the two together**, and it lives in `paper` — `Quivers.spendRound` plus
> `QuiverItems.setLoaded` — where no unit test reaches. **Two guarded halves and an unguarded whole.**

**WHAT SLICE G DID ABOUT IT: NOTHING TO THE WRITE PATH, DELIBERATELY.** `QuiverState.roundsRemaining()`
**clamps**, answering `8` where the magazine yields `9`. It under-promises by one and never
over-promises, which is what the Plume's R3 requires; the alternative — answering `9` — would make an
accessor model what the write funnel does next. **The gap is recorded in that method's javadoc and in
its test row so the next reader meets it before deciding the eight is an off-by-one.**

**THIS ROW'S TRIGGER COVERS BOTH EDGES** — the same shipped item that makes the forfeit reachable
makes the over-full window reachable — so no second trigger is invented. What is added is the
knowledge that **the park has two faces and only one of them was written down.**

---

> **THE TRIGGER IS PHRASED AS A CONDITION BECAUSE THE LAST PARK IN THIS PROJECT ROTTED.** Punch was
> parked on *"the first ranger-class weapon with `type: projectile`"* — **a condition that was already
> satisfied by `hunters_bow` on the day it was written**, and nothing fired. A trigger is only worth
> the grep that can evaluate it, and this one is: *does any shipped item grant quiver size that is not
> the dev instrument?*

---

## WHAT THIS RECORD DOES NOT COVER

- **The `locust` half of A-3** — optional, and no verdict either way (see A-3).
- **The roll table.** No row exercises `expanded_quiver` actually *rolling* onto a weapon through the
  enchant table; every row above reaches it through the dev command.
  `ExpandedQuiverContentInvariantTest` guards the content-side half of that at build time.
- **`hunters_bow`.** The one ranger weapon with **no** `quiver_size`, on which this enchant would
  render `+N Quiver Arrows` and grant zero. Refused at build time by the content-invariant test, which
  exempts it **by name**; not staged here.
