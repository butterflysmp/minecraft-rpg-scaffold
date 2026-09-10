# GATE — element accrual and the damage glyph

**This file is the source of truth for this slice's boot gate.** It is versioned with the code
because, for every behaviour listed below, **these rows are the only check that exists anywhere in
the project.** 1403 tests pass with any of them deleted, and not one of them can watch a burn stop,
a glyph draw, or a number arrive on a screen.

> **THIS FILE IS CANONICAL. The tick-through page is an operator convenience and is NOT under version
> control.** Two copies of one record drift, and when they do this one wins — it is in `master`, it is
> reviewable in a diff, and it is the only copy a future reader can reach. If the page and this file
> disagree about a row, a figure or an attribution grade, **the page is wrong by definition**; fix the
> page, or regenerate it from here.
>
> Recorded because the mutation-note estate had exactly this problem — three greps gave three counts —
> and the fix there was the same one: name which copy is the record.

## GATE COMPLETE — twelve rows run, six refused, one open

Run against `1f6c93f` unless a row says otherwise.

| | |
|---|---|
| **Run and passed** | A1, A2, A3, A4, B1, B2, B3, C1, C2, C3, C4 |
| **RE-RUN OWED** | **D1** — its command could not parse; see the note under the attribution table |
| **Refused — cannot witness anything** | the six in their own section, which is the more valuable half |
| **Open, ruled left alone** | the `FIRE_TICK` suppression covers one cause of four |

### ATTRIBUTION, GRADED — and this section is the point of the file

`GATE-vanilla-damage.md` downgraded two rows because a blanket *"gates green"* is weaker evidence
than a figure. The same grading applies here, on **three** rungs rather than two. One row is neither
itemised nor blanket, and naming the middle rung rather than rounding it to one of the two is the
point of this section.

**ITEMISED (strongest) — the operator's own figures, two of them:**

- **A1, against `6d5eb34`:** *"knell takes 20 and then 6 18s."* Exact and unprompted. It confirmed
  the six-burn window and the silent first second **in one line**, and it is the measurement the cap
  ruling was then taken from. The strongest datum in the slice.
- **The open item, against `23ed254`:** *"the mob takes the normal fire damage in between the scorch
  damage only while standing in fire."* An itemised NEGATIVE observation, and **its phrasing is what
  diagnosed the cause** — block contact raises `FIRE`, catching fire raises `FIRE_TICK`, and only the
  second is suppressed.

**CONFIRMED AGAINST A STATED DISCRIMINATOR (middle) — one row, A3:**

> The operator affirmed the specific described outcomes, over repeated runs, **having been told in
> advance what would invalidate a run and why.** Stronger than the blanket confirmations because the
> failure mode was named to him before he looked; weaker than the two ITEMISED entries, which are his
> own unprompted figures.
>
> **What he ran, and what he reported.** The pair, multiple times. Run 1's knell died to the bolt
> **without a flame**; run 2's **survived at 1** and was killed by the first burn; the impact number
> read a **white 20** on every run. His words: *"it's passed every time without fail."*
>
> **WHY A3 COULD NOT USE A BLANKET — and this is the row's DESIGN, not its status.** It is why the
> re-run had to be staged the way it was, and it is the part to carry forward: A3 **inverts silently
> under a crit.** Its two runs are 340 and 339 damage, a crit kills both, and the pair collapses into
> two copies of run 1 — **which reads as a clean pass.** A blanket confirmation is exactly the
> evidence this row cannot accept, because the failure mode it has IS "looks like a pass".
>
> So the discriminator was stated first — the **white 20**, which is the thing that says no crit
> landed — and the affirmation is worth what it is worth *because it came after that*. **Anyone
> re-running A3 must stage it the same way**, or a green result carries no information.

**BLANKET (weakest) — three confirmations, no per-row figures:**

| against | words | carried |
|---|---|---|
| `1233469` | *"all gates green"* | D1 skipped, named by him |
| `23ed254` | *"all gates passed"* | two exceptions itemised separately |
| `1f6c93f` | *"everything passed"* | A4 — thin, but valid. A4 and A3's crit precondition both reached him in the same message as the final publish, and this came after |

> ### D1 IS RE-RUN-OWED. ITS COMMAND COULD NOT PARSE.
>
> **Found 2026-09-09.** D1 was staged as `/rpg apply scorch 1 200`. **The argument order is
> `<status> <duration> <stacks>`**, so that reads as duration 1 / stacks 200 — and stacks is declared
> `IntegerArgumentType.integer(1, 20)`, so Brigadier fails the parse. The reader still has `200`
> unconsumed, so the shorter duration-only overload does not run either. **Nothing executed.**
>
> **D1's green is therefore over a command that could not have run** — and it was already this file's
> weakest evidence: itemised nowhere, no figures, and the one confirmation that *names* it says
> **skipped**. The corrected command is `/rpg apply scorch 200 1`, which is what the row's own
> arithmetic (a 200-tick window) was always derived from.
>
> **Marked re-run-owed rather than un-ticked and forgotten.** A row deleted from the "passed" list
> reads as one nobody chose to run; a row marked owed reads as one that must be. It is left in the
> table above with this note attached rather than silently moved, so the correction is visible to
> anyone who read the old list.
>
> **This is the "a check that did not run looks exactly like one that passed" defect arriving through
> a channel the page had not covered** — not a filter that failed, but a command line that was never
> valid. The row's arithmetic was scrutinised repeatedly; its SYNTAX never was.

---

## PRECONDITION FOR EVERY ROW BELOW: THE CRIT COIN FLIP

**A player crits 15% of the time for double damage, by default, and it cannot be turned off.**
Every figure stated below is the **non-crit** value.

This is now the fifth witness on `NEXT.md`'s *A GATE ROW NEEDS ITS PRECONDITIONS ENUMERATED* axis,
and the first that is not a fact about the world but **a coin flip that re-rolls on every swing** —
which is how twelve rows were written with an unstated 15% chance of a doubled hit.

- A crit **fails most rows loudly**: the number is yellow and roughly double, so it is obvious.
- A crit **inverts A3 silently**. See above.
- A crit **does not change any burn figure**, and that is A4's subject: the cap comes off the
  declared magnitude, not the crit-inflated one.

---

## THE TWELVE ROWS

### A1 — the burn is exactly half the hit, six times, starting one second late
`/rpg spawn knell` · `/rpg give flint_staff` · one bolt.

**Expect** `▲20` · **one full second of nothing** · six of `▲10` · then **silence**.
Total 20 + 60 = 80, nameplate **280/360**.

> **SOLE WITNESS, TWICE OVER.**
> 1. The **silence** is the only live evidence that `EntityScorchSink` passes `AccrualRule.INERT`. A
>    seventh number is the burn feeding itself — and **flipping `INERT` to `ACCRUES` in code leaves
>    the entire suite green** (measured: 813/17/573, zero failures), because nothing references that
>    class.
> 2. The knell is **alight throughout**, so vanilla raises a `FIRE_TICK` every second. **Six numbers
>    and no others** is the only witness that the suppression tokens them away.

### A2 — the percent-of-max arm, in the only place it still binds
Any vanilla 20-HP mob · `hunters_bow` · one arrow. **Expect** `▲6` then six of `▲1`. Survives at 8/20.

**The cap is 3 and the burn is 1. `1 != 3` IS the row.**

> **SOLE WITNESS.** After the cap halved, reaching the 5% arm on a knell needs a hit above **36**, and
> nothing ships above 20. A1 used to witness this and now witnesses the halved cap instead — a row
> whose subject changed underneath it while still passing.

### A3 — a lethal hit accrues nothing (the pair)
- **Run 1:** `/rpg mobdamage 340` → knell at 20 · one bolt (20). **Dies, NEVER CATCHES FIRE.**
- **Run 2:** fresh knell, `/rpg mobdamage 339` → knell at 21 · same bolt. **Survives at 1, catches
  fire, first burn kills it.**

> **"IGNITES" WAS REWORDED TO "CATCHES FIRE" ON 2026-09-09, AND IT IS NOT A STYLE EDIT.** Ignite is
> now a named mechanic whose rule is *"any mob that dies while scorched ignites"* — so **"Dies, NEVER
> IGNITES" reads as a claim that a dying scorched mob does not explode, which is the exact opposite
> of the ruling.** The row always meant the ordinary-English sense (no burn appears, because a lethal
> hit accrues nothing). Both readings are true of run 1, which is what made the collision invisible
> and would have made it survive a proofread.

**Neither run discriminates alone.** Run 1 passes if accrual were deleted entirely; run 2 passes if
the gate were loosened to `>= 0`. **A CRIT DESTROYS IT WITHOUT FAILING IT** — 40 kills both.

### A4 — a crit raises the hit and leaves the burn alone
`/rpg critchance 1.0`, **that item only** (crit damage already bases at +100%; a second item makes it
×3). Equip anywhere but the main hand. `knell` + `flint_staff`.

**Expect** yellow `▲40` then six of `▲10` — **the same ten A1 gives.**

> **SOLE WITNESS for the crit-free cap. `10` vs `18` is the discriminator.** If the burn reads 18, the
> cap is coming off the crit-inflated number: 40 halved is 20, which is over the knell's 5%-of-max
> 18, so the percent arm clamps it.

### B1 — sustained fire does not add burns (count, don't time)
`knell` + `hunters_bow` held for ten seconds. **Expect ~13 of `▲6` and EXACTLY 10 of `▲3`.**
If refresh burned, ~23 threes.

Crits read yellow `12` and are still hits; **the threes stay threes**, which is the crit-free cap
doing its job inside this row.

### B2 — refresh extends the window without restarting the clock
Four arrows over ~3s, then stop. Burns continue **six more seconds on the same 1.0s grid**, with no
stutter or phase shift where you stopped.

### B3 — a burn kill credits the shooter, not the victim
`/rpg mobdamage 345` → knell at 15 · one arrow (6 → 9) · stop. Three burns of 3; the third kills.
**The death message names the PLAYER.** Slice 1 found this credit *inverted*, not missing.

### C1 — three reachable glyphs draw as characters, not boxes
fire `▲` U+25B2 (`hunters_bow`) · nature `✿` U+273F (`arc_surge`) · void `✧` U+2727 (`void_slash`).

`▲` is the new one; fallbacks if it boxes are `✺` U+273A and `✹` U+2739.

> Chosen for a second reason: `✦` and `✧` are **adjacent codepoints differing only by fill**, so fire
> and void were separated by colour alone. `▲` separates on **SHAPE**.
> **INTERIM CHOICE, not a decision** — a resource pack makes it one line per element and no code.

### C2 — a crit turns the number yellow and leaves the mark alone
`knell` + `emberblade`. Normal `▲7`, crit `▲`+yellow `10` — **the triangle stays GOLD.**
If it yellows too, the number's colour is on the assembled component rather than on the number.

Moved off `ironblade` this round: kinetic has no glyph now.

### C3 — kinetic draws a bare number, and no stray gap in front of it
`knell` + `ironblade`. A bare `8`, **no leading space**. **AND NO KINETIC WARNING IN THE BOOT LOG** —
absent still means "legacy file" and warns; empty means chosen and is silent. **That half is invisible
in game** and must be read in the log.

### C4 — the burn wears the mark of whatever set it
Nothing new to stage — seen in A1, A2, B1. Hit `▲20`, burn `▲10`.
Was an open **question** in round one; a pass since `AccrualRule` landed.

### D1 — newest wins: three fields change hands in one observation
`knell` + `/rpg apply scorch 200 1`. Two **unmarked** `2`s — that 2 is `Scorch.UNDECLARED_CAP`,
because the command declares no payload. Then one flint bolt: `▲20`, and **every burn after reads
`▲10`.**

Element unmarked→`▲`, cap 2→10, credit nobody→you — **under one rule rather than three.**

> **THE DEV PATH IS THE ONLY PLACE THIS IS VISIBLE**, since fire is the only element that accrues.
> Also expect the burn to end **six seconds after the bolt** rather than ten after the command: the
> known refresh shortening, deferred, not a bug.

---

## THE SIX REFUSED ROWS

**This is the more valuable half of the file and it is nowhere else in the repo.** Each would have
passed while witnessing nothing, which is the shape that credits coverage that does not exist.

**1. An armoured target proving accrual is post-mitigation.**
Mobs always have defense 0 — `reconcileDefenseModifiers` has ONE production caller, on player worn
gear, and `knell.yml` is three keys with no defense field. And nothing in the game can scorch a
player. **The row would PASS WHILE WITNESSING NOTHING** — the same shape as the `S5` row the operator
was sent to run twice in the previous slice. Post-mitigation stays unit-covered only.

**2. Stack counts, which went from 1 to as many as 10.**
No command reads a mob's stack count, and stacks do not scale damage. **The change has no in-game
consequence at all** — not merely invisible in a diff, unobservable on a running server.

> **THE REFUSAL IS NOW PERMANENT, NOT DEFERRED.** This row read *"it becomes witnessable the day
> Ignite reads it, which is why that threshold is recorded undecided."* Both halves are dead: the
> operator ruled on 2026-09-09 that **any mob that dies while scorched ignites**, binary, so the
> threshold is decided AND **Ignite never reads a count.**
>
> The accumulator was deleted in the same commit rather than left unread, so there is no longer a
> count to witness at all. This row cannot become runnable by anything currently planned — it would
> take a NEW consumer that defines what a count means, which is exactly the re-add trigger recorded
> in `Scorch`'s javadoc.

**3. `undead ☠` / `water ≈` / `wither ✖` glyph legibility.**
No shipped content deals those three elements. Authored and unreachable. `wither`'s `<dark_gray>` is
the one most likely to be illegible when something finally does.

**4. Anything involving a scorched PLAYER.**
`/rpg apply` filters to non-players and no mob casts, so **nothing in the game can scorch a player.**
The permanently-unscorchable-at-zero consequence recorded in `ElementAccrual` is likewise unreachable
from a client.

**5. Sweep accrual on bystanders, measured.**
Loosely visible — swing an emberblade into two mobs and both should catch fire — but the share is half
the primary, so a bystander buys one stack and on a small mob its burn reads `1`, **the same as A2's.**
Nothing distinguishes "the sweep accrued" from "something else lit it". A sanity glance, not a row.
**Its unit row is the only witness that the sweep passes `ACCRUES` and not `INERT`.**

**6. A dev command that deals ELEMENTAL damage.**
There isn't one. `/rpg mobdamage` uses the two-argument arity, carries no element, and accrues
nothing. **Every row above must be driven by a real weapon.** An `[element]` argument would make A1
and A2 exact without one, and is worth its own small commit.

---

## OPEN: THE SUPPRESSION COVERS ONE CAUSE OF FOUR

Ruled left alone. Recorded as a bounded question rather than a symptom.

```
FIRE_TICK   suppressed while scorched     <- the stream we replaced
FIRE        NOT.  Standing in a fire block.   <- what was observed
LAVA        NOT.
HOT_FLOOR   NOT.  Magma block.
```

Under the other three, a scorched victim takes **our capped, credited, defense-bypassing burn PLUS
vanilla's uncapped uncredited one** — the precise doubling the suppression exists to prevent,
arriving through a sibling cause.

**Not a one-line fix, which is why it is a question.** Suppressing `LAVA` would mean a scorched mob
takes **LESS** lava damage than an unscorched one — scorch as a defensive buff. This repo already
refused that shape once, which is why the `FIRE_TICK` gate sits **before** `damageWindow.claim`: a
suppressed tick dealt nothing, so it must not consume window budget either.

> **The bounded question:** which of these four should scorch suppress, and does suppressing `LAVA`
> make scorch a defensive buff? An answer per cause, like the standing *"which causes should
> `Defense` touch?"* question `DefenseRule` was built for.
