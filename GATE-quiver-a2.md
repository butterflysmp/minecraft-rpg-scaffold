# GATE — the quiver, slice A2: quiver size and reload time as real stats

**Status: RUN, 2026-09-12 — ALL SEVEN ROWS PASS.** Written at commit 7, before any boot, so the rows
were staged from the design rather than from what a run happened to show — **and the run came back
green against rows written that way.** That pairing is what the provenance sentence was protecting,
and this is the moment it pays off: nothing below was fitted to an observation.

> **PER-ROW IN SHAPE, BLANKET IN EVIDENCE, AND THIS DOCUMENT SAYS WHICH.** The report was one
> sentence — *"all gates green"* — not seven readings. A blanket over rows with stated pass
> conditions answers each of them unambiguously, which is why every row below carries PASS. It does
> **not** attest them separately.
>
> **So two readings this document names as the ones a hurried run skips were not individually
> reported: R1's middle observation and R7's first.** That is recorded here rather than left for a
> later reader to discover, because the weight of the evidence is part of the record.
>
> **No reading has been written into a result column.** The pass conditions are in the rows already;
> the result is *green, by blanket*. Inventing `28/28` as though it had been reported would turn a
> blanket into seven fabricated observations, which is the failure this paragraph exists to prevent.

| row | | |
|---|---|---|
| **R1** | **PASS** | by blanket. Its middle observation — *still `9/9` with the instrument held* — was not separately attested. |
| **R2** | **PASS** | by blanket. |
| **R3** | **PASS** | by blanket. |
| **R4** | **PASS** | by blanket. |
| **R5** | **PASS** | by blanket. |
| **R6** | **PASS** | by blanket. |
| **R7** | **PASS** | by blanket. Its first reading — the one that separates the two tooltip mutations — was not separately attested. |
| **Q7** | **UNRUN** | **Not covered, and cannot be. See below — it is outside the table by construction.** |

**With those rows green, the four mutations below are witnessed.** They were green in the suite and
reddenable only on a server; a booted world has now answered them.

---

**Branch `feat/quiver`. Fixture `quiver_stone` (`quiver_size: 9`, `reload_ticks: 34`). Instruments
`/rpg quiversize` (+19 → **28**) and `/rpg reloadtime` (+14 → **48**), both DEV-gated, both
permanent.**

A1's gate (`GATE-quiver.md`) witnessed the mechanism: the count, the reload, the five verdicts, the
carry. **This one witnesses only what A2 added — the two stats reaching an item, and the four
mutations that are green in the suite and can only go red on a server.**

---

## WHY THIS DOCUMENT EXISTS: FOUR MUTATIONS THAT NO TEST CAN REDDEN

Every other claim in A2 is pinned by a unit row or a mutation. **These four are not, and cannot be**
— each needs a live `ItemStack` or a live `Player`, and this project has neither in its test
classpath. That is a standing decision (`new ItemStack(...)` throws without a server; the parent
pom's only test dependency is `junit-jupiter`), not an accident, and it prices every one of them:

| mutation | what it breaks | the row that catches it |
|---|---|---|
| **`MUTSTATREAD`** | `QuiverItems.resolveCapacity` returns `weapon.quiverSize()` again, ignoring the stat | **R1** |
| **`MUTAPPLYLORE2`** | `WeaponItems.applyLore` passes `empty` for the stamp while still calling `capacityInMeta` | **R7**, first reading |
| **`MUTSCANKEY`** | `QuiverSizeModifierItems` reads the reload key | **R3** |
| **`MUTSCANKEY2`** | `ReloadTimeModifierItems` reads the quiver key | **R3** |

**They are the point of this gate.** A run that reports "the quiver works" without **R7's first
reading**, **R1's middle observation** and **R3's drop step** has not touched any of them.

> **`MUTSTATREAD` AND `MUTAPPLYLORE2` LOOK IDENTICAL ON MOST SCREENS, AND ONLY ONE STAGING SEPARATES
> THEM.** Both leave a tooltip denominator of `9` where `28` belongs, so R1 catches either and names
> neither. **R7's first reading is where the NUMERATOR moves too** — `28/28` correct, `9/9` for
> `MUTSTATREAD`, `28/9` for `MUTAPPLYLORE2`, and that last is a numerator above its denominator,
> which no correct state in this project can produce.

---

## STAGING

```
/rpg give quiver_stone
/rpg quiversize            -- prints: +19 arrows: 9 -> 28 rounds once packed
/rpg reloadtime            -- prints: +14 ticks, SLOWER: 34 -> 48 ticks (1.70s -> 2.40s)
```

Both instruments work **held or worn**, and every slot is scanned — holding them in the off-hand
alongside the weapon is the fastest staging.

**Every number below is the RESOLVED one, and that is what a row reads.** 9, 19 and 28 are pairwise
different; so are 34, 14 and 48; and none of the six appears anywhere in `content/`. A reading of
"28" cannot be the bonus, the base, or another weapon's number.

---

## R — THE ROWS

| # | staging | pass condition |
|---|---|---|
| **R1** | give `quiver_stone`, read the tooltip. `/rpg quiversize`. **Read the tooltip AGAIN.** Then fire **once**. | **THREE readings, all named.** (1) `Quiver: 9/9`. (2) **still `Quiver: 9/9`** after the instrument is in hand. (3) after the shot, **`Quiver: 8/28`**. |
| **R2** | from R1's `8/28`, **relog**. Then `/rpg refresh`. Then open the enchant table. | `Quiver: 8/28` after each of the three, unchanged. *A1 proved the COUNT carries; this proves the CAPACITY STAMP carries with it.* |
| **R3** | hold both instruments. Fire once and reload once — read the tooltip and `/rpg stats`. **Then drop ONE instrument**, fire once and reload once, and read both again. | With both: **28** and **48t (2.40s)**. After dropping the quiver instrument: **9** and **still 48t**. After dropping the reload instrument instead: **still 28** and **34t (1.70s)**. |
| **R4** | with only `/rpg reloadtime` held, empty the magazine and left-click to reload. **Watch it.** | The weapon is dead **visibly longer than bare** — 2.40s against 1.70s — and `/rpg stats` reads `Reload 48t (2.40s)` while it is held. |
| **R5** | `/rpg reloadtime -14`. Read the command's own line. Then reload. | The command prints **`34 -> 20 ticks (1.70s -> 1.00s), FASTER`**, and the reload that follows is **visibly quicker than bare**. |
| **R6** | `/rpg stats` bare-handed, then again holding `quiver_stone` with both instruments. | Bare-handed: **exactly eight stat lines, the last being `Crit Damage`**. Holding: **exactly ten**, the last two being **`⚔ Quiver       28`** and **`  Reload       48t (2.40s)`**. |
| **R7** | hold the quiver instrument, left-click to reload. **Read the tooltip — this is a pass condition, not a setup step.** **Drop the instrument.** Read it again. Then fire **once**. | **THREE readings, all named.** (1) **`Quiver: 28/28`** — see the table below; this is the reading that separates the two tooltip mutations. (2) still **`28/28`** while dropped and unfired. (3) after the shot, **`Quiver: 9/9`**. |

---

### R1 — THE MIDDLE READING IS THE ROW, AND IT IS THE ONE A HURRIED RUN SKIPS

Reading (2) — *still `9/9` with the instrument in hand* — is the only observation that separates the
design from a plausible alternative. **Capacity re-resolves at a WRITE**, never on a reconcile tick,
so picking gear up changes nothing until the wielder fires or reloads. A row that equipped and fired
in one motion would pass identically against a reconcile-loop clamp, which is a different mechanism
with a different failure mode.

That behaviour is **endorsed, not tolerated**: *you pack your quiver, and what you packed is what you
carry.* If it reads `9/28` at step (2), the capacity is being resolved somewhere it should not be.

**Reading (3) discriminates three ways, and the THIRD is deliberately not split further:**

| reading | meaning |
|---|---|
| **`8/28`** | correct |
| `8/9` | **`MUTSTATREAD` OR `MUTAPPLYLORE2`** — this row cannot tell them apart. **R7's first reading can.** |
| `9/28` or `9/9` | the write never happened, or the render did not follow it — **boot row V1's defect**, in a new location |

> **AND THE MIDDLE LINE USED TO CLAIM FOUR WAYS, ON AN OBSERVATION NOBODY CAN TAKE.** It read
> *"`8/28` on the stored value but `8/9` on screen"* — and the paragraph that followed it said why
> that cannot be done: **there is no in-game way to read the PDC directly.** Worked through,
> the two mutations are identical on screen here:
>
> ```
> MUTSTATREAD     stamp 9,  loaded 8 -> capacityOf(of(9), 9)  = 9  ->  8/9
> MUTAPPLYLORE2   stamp 28, loaded 8 -> capacityOf(empty, 9)  = 9  ->  8/9
> ```
>
> `/rpg stats` does not rescue it either: the sheet resolves LIVE through `QuiverSize.resolve`, so it
> reads `28` under both. The row was sending an operator to look for an instrument that does not
> exist, which is a worse failure than an un-failable row — **it makes the row un-runnable, and an
> un-runnable row gets guessed or skipped.**

So the rendered denominator is the only witness either mutation has anywhere, and **separating them
needs a staging where the NUMERATOR differs too.** That is R7.

### R3 — THE DROP IS THE TEST, NOT THE EQUIP

Both instruments held and both stats moved proves only that two scanners ran. **It is the drop that
proves they are not the same scanner.** Each half is stated separately above because dropping either
one must move exactly one number.

| what moves when one instrument is dropped | meaning |
|---|---|
| **only that stat** | correct — the scanners are independent |
| **both** | `MUTSCANKEY` / `MUTSCANKEY2` — one scanner is reading the other's key |
| **neither** | the reconcile loop is not running, **or** the two source prefixes collided and each wipes the other's modifiers on alternate ticks |

That last case presents as *"the stat does nothing"* rather than as a crossed wire, which is why the
prefixes are also asserted disjoint in a unit row — the boot cannot tell those two apart on its own.

**This is what the two instruments were bought for.** A single item carrying both bonuses has no
staging in which one stat moves and the other does not, so this row could not exist. The price was
two `Keys`, two command arms and two scanners.

### R5 — THE ONE ROW THAT NEEDS A TYPED NEGATIVE, AND WHY IT IS COLLISION-PROOF ANYWAY

No reducing DEFAULT is possible: at base 34 every collision-free reducing value lands on an authored
number, and the only survivor (`-17`) resolves to **17, the bonus itself** — two independent
quantities equal, which is the defect that makes a row pass whether the rule exists or not. So the
instrument adds and the downward direction is typed by hand.

**`-14` resolves to 20, and 20 IS authored elsewhere in `content/`.** That does not reach this row:
the command prints **`34 -> 20 ticks (1.70s -> 1.00s), FASTER`** — base, resolved, both in seconds,
and the direction word. A collision on `20` alone cannot produce that triple.

> **The general rule, and it decides how future rows are written: a readout that carries its own
> base and delta is immune to a sweep collision on its result. A BARE number is not.** Every row
> above that reads a bare value — R1's `28`, R3's `28`/`9`, R6's `28` — uses a swept one. R5 does not
> have to.

### R7 — THE DECREASE CLAMPS AT THE WRITE, WHICH IS THE HALF NOTHING ELSE WITNESSES

#### Reading (1) — `28/28` — IS THE MOST DIAGNOSTIC OBSERVATION IN A2, AND IT WAS WRITTEN AS STAGING

It sat in the staging column with no alternatives named. It is the only place in the gate where the
NUMERATOR moves with the stamp as well as the denominator, which is exactly what separates the two
tooltip mutations R1 has to lump together:

| reading | meaning |
|---|---|
| **`28/28`** | correct |
| `9/9` | **`MUTSTATREAD`** — `resolveCapacity` ignored the stat, so the reload filled to the AUTHORED 9 and stamped 9 |
| `28/9` | **`MUTAPPLYLORE2`** — the stamp is 28 and the magazine really holds 28; the tooltip is rendering the authored fallback instead of the stamp |

**`28/9` is a numerator above its denominator, and no correct state in this project can produce
one.** `WeaponLoreLines.quiverLine` renders `loaded` verbatim against `capacity` with no clamp
(`:118-122`, checked), and `setLoaded` clamps the count to the SAME capacity it stamps — so the two
can only disagree if the render is reading a different number from the write. That is
`MUTAPPLYLORE2` and nothing else.

#### Reading (3) — after the shot

`DESIGN-stat-engine.md`'s ruled semantics: **increase is headroom, decrease clamps.** A2 honours the
decrease at `QuiverItems.setLoaded`, which holds the new capacity and the count in one call with
`Quiver.clamp` between them — deliberately **not** in the reconcile loop, because a clamp there would
be a second enforcement site for one capacity and an in-play write with no render.

The consequence is visible only in this exact sequence, and the three outcomes are three different
numbers:

| reading after the shot | meaning |
|---|---|
| **`9/9`** | correct — the count was clamped to the newly resolved capacity at the write |
| `27/9` | the capacity re-resolved and the count did not clamp — a magazine holding more than it can hold |
| `27/28` | the capacity did not re-resolve at all — the stamp is stale past a write |

---

## TWO CHECKS ON THE ROWS THEMSELVES

Both are properties of the ROWS rather than of the code, and both are the kind of defect that leaves
a gate report looking clean while proving less than it claims.

### 1 · NO ROW PASSES BY SOMETHING BEING ABSENT

Checked deliberately, because it is the easiest way to write a row that cannot fail.

- **R6's bare-handed half is a COUNT AND A NAMED LAST LINE** — *exactly eight, ending `Crit Damage`*
  — not *"no quiver lines"*. A sheet that failed to render its last two lines for an unrelated reason
  would pass the absence form and fail this one.
- **R1's middle reading is `9/9`**, a number, not *"nothing changed"*.
- **R3's failure modes are all POSITIVE readings** of the wrong number, including the "neither moves"
  case, which is stated as *both stats read their authored values* rather than as an absence.
- **R2 asserts `8/28` three times**, not *"nothing was lost"*.

### 2 · EVERY READING A ROW NAMES MUST BE ONE A PERSON AT A KEYBOARD CAN ACTUALLY TAKE

**Added after this document's own first review, which found a row failing it.** R1's table cited
*"`8/28` on the stored value but `8/9` on screen"* — a PDC read, which no in-game surface offers, in
a section that said so two paragraphs later.

**It fails differently from an absence-shaped condition, which is why it needs its own check.** An
absence-shaped row is UN-FAILABLE: it passes whether the rule exists or not. An unobservable row is
**UN-RUNNABLE**: the operator reaches it, has no instrument, and resolves that by guessing or by
skipping — and a skipped row reports the same as a passed one in any summary.

**The test, at writing time:** for each named reading, say which surface shows it. Every reading in
this gate now answers with one of exactly four: **the item tooltip**, **`/rpg stats`**, **a command's
own confirmation line**, or **the clock** (R4 and R5's "visibly longer/quicker"). A reading that
answers with none of those is not a pass condition yet.


---

## Q7 CARRIES FORWARD, STILL UNRUN, AND A2 DOES NOT DISCHARGE IT

**Q7 is the held-right-click repeat floor: the shortest interval at which a player can actually
fire.** It has been owed since A1 and it is owed still.

**It has no green state, which is why no run can quietly absorb it.** Q7 produces a NUMBER that
exists nowhere else in the project — there is no expected value for "pass" to be measured against.
Reporting it as green would mean nothing; reporting it as run without the number would mean less.

**What is priced on it, unchanged:**

- Slice C's **7-tick dual cooldown** is built on that unmeasured interval.
- The **reload BALANCE floor** — *what duration still brakes* — depends on the shortest achievable
  fire interval, and A2 deliberately shipped **no mechanism floor** for reload after three
  retractions. The balance question was left exactly here.

**So A2 hands Q7 forward untouched.** It is not in the table above, because putting it there would
let a blanket *"the gate ran green"* cover a row that cannot be green — **and A2's run WAS a
blanket**, which is exactly the case that construction was for.

> **A2 IS THE SECOND SLICE TO HAND Q7 ON, AND THE NEXT PLAN SHOULD INHERIT THE COUNT.** A1 carried
> it forward unrun; A2 carried it forward unrun. **A thing owed twice is a different fact from a
> thing owed once** — it is no longer "not got to yet", it is a measurement two slices have found a
> reason to defer, and the reason has been the same both times: nothing in either slice needed it,
> while two things outside both are priced on it.
>
> Slice B's plan opens with it, as a count and not as a mention: **owed since A1, deferred twice.**
>
> **AND IT IS NO LONGER UN-RUNNABLE: `GATE-q7.md` carries the row, and `/rpg firerate` is its
> instrument.** Both deferrals happened because nobody had built the thing that measures it — the
> recipe asked an operator to count shots by hand. That is struck; the counter counts. The reading
> is still the operator's to take.
>
> **DISCHARGED 2026-09-12.** `GATE-q7.md` carries the reading: **the input floor is 4 ticks**, on two
> weapons and two materials. A2 was the second slice to hand it on and the last.

---

## WHAT THIS GATE IS THE SOLE WITNESS FOR

**All four are now witnessed, by the run recorded at the head of this file** — by blanket, not by
four separate observations.

- **`MUTSTATREAD`, `MUTAPPLYLORE2`, `MUTSCANKEY`, `MUTSCANKEY2`** — all four green in the suite,
  all four only reddenable here.
- **The capacity stamp surviving a re-mint** (R2). A1 proved the count carries; the capacity key was
  added to `QuiverItems.carry` in A2 and nothing else has exercised it.
- **The decrease clamp at the write** (R7).
- **Both scanners being independent** (R3).

## WHAT IT IS NOT A WITNESS FOR, AND WHERE THOSE LIVE INSTEAD

- **The arithmetic of either stat.** `QuiverSizeTest`, `ReloadTimeTest` — including the reload
  downward direction and the `MIN_CAPACITY` floor, which have unit rows and passing mutations.
- **Which files may resolve or read either number.** Three signature rows scan both modules and pin
  named sets; a boot cannot see a second resolver at all.
- **Whether reload-speed gear is possible.** `MUTGATE` reddens in both modules; R5 only demonstrates
  it in play.
- **Balance.** Nothing here says 28 rounds or 2.4 seconds is right. The instruments exist to make the
  stats observable, and their numbers were chosen to be distinguishable rather than good.
