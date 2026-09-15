# GATE — the Dragon's Plume's release (slice H2)

**Status: NOT RUN. THE SLICE IS NOW BUILT, AND SLICE J HAS ADDED THREE ROWS TO IT.** ~~AND THE SLICE
IS NOT BUILT... None of that exists.~~
H2 spawns the projectiles through the existing cast path with F/F2's homing, spends the rounds, and
handles R4‴ (a partial draw: one arrow, no homing, **9 / 17 / 26 by band**) and R10 (under three
ticks, nothing).
**All of that now exists and none of it has been booted.**

**This file existed early for one reason: a row had to leave `GATE-plume-draw.md`, and a row with
nowhere to go is a row that gets deleted.** It has since gained the rows H2 made stageable.

```
R-1    NOT RUN   the cap, at exactly 2 and exactly 4 rounds
R-2    NOT RUN   the stranded round is visible and is not a bug
R-N1   NOT RUN   NO ARROW -- the only thing in the game that teaches R5
R-N2   NOT RUN   NO ROUNDS -- its own sentence, not the empty-quiver one
R-N3   NOT RUN   BOTH IN ONE FIGHT -- the row the two-key commit rests on
R-N4   NOT RUN   THE SILENCE, with its positive control
R-B1   NOT RUN   BAND 1 fires at 8 and NOT at 9        R4''' boundary
R-B2   NOT RUN   BAND 2 fires at 9 and at 14, NOT 15   R4''' boundary
R-B3   NOT RUN   BAND 3 fires at 15 and at 19          and 20 is a CHARGED release
```

> ## NEITHER NOTICE HAS EVER BEEN SEEN BY A PLAYER
>
> **Both are `paper`-side and neither has been booted.** `NoticeThrottleKeysTest` guards that the two
> throttle keys are DISTINCT; it does not and cannot guard that **either message ever appears.** A
> notice that is never sent, sent to nobody, swallowed by a throttle stamped elsewhere, or rendered
> as an empty component would pass every row in that file.
>
> **So R-N1 and R-N2 are the first evidence these surfaces exist at all**, and R-N3 is the first
> evidence of the thing the commit was actually argued on.

**Every row below was written BEFORE any boot**, and the predictions with them.

---

## THE ROWS

| row | staging | what to record |
|---|---|---|
| **R-1** | **THE CAP — moved from H1's gate, where it could not be run.** Fire enough arrows to bring the magazine to **exactly 2**, then hold a full draw for **2.6 seconds (52 ticks)** — R14 shortened it from 3.0. Repeat at **exactly 4**. | **The ticks must STOP when the NEXT STEP is unaffordable, and the release must fire EXACTLY the tracked step** (R3a). At 2 rounds: **one tick, one arrow, one round left over.** At 4 rounds: **two ticks, three arrows, one round left over.** Record ticks heard, arrows seen, and rounds remaining afterwards. Then, **without releasing**, have a reload complete mid-draw and record whether the ticks **RESUME**. |
| **R-2** | **THE STRANDED ROUND IS VISIBLE AND IS NOT A BUG.** Straight after R-1's two-round release, check the magazine. | **It must read 1, not 0.** The player paid for one arrow and keeps the round that could not buy a step. **If this reads as a bug to whoever runs it, that is the finding R3a's overturn condition needs** — the ruling is explicitly overturnable and the alternative (`min(step, rounds)`, stranding nothing) costs one comparison. |
| **R-N1** | **NO ARROW.** Hold a Plume with **an empty bag** — no arrows anywhere, off-hand included. Right-click. **The magazine may be full; that is the point.** | **PREDICTED: the bow does not move, and a message names the remedy** — a plain Arrow, in the off-hand. **THIS IS THE ONLY THING IN THE GAME THAT TEACHES R5**, so the row is not "did text appear". **Ask someone who has never been told**: could they act on it? Record the sentence verbatim and whether the reader knew what to do next. **A player can be FULLY LOADED and unable to draw**, which is the part that reads as a broken item. |
| **R-N2** | **NO ROUNDS.** Fire the magazine dry, then hold a **full** draw and release. | **PREDICTED: one message, naming the HOLD** — *"The draw was held for nothing — your quiver is empty. Left-click to reload."* **It must NOT be the empty-quiver line every other weapon uses.** Every other quiver weapon refuses BEFORE the shot; the Plume refuses **after 2.6 seconds of holding** (52 ticks, R14), and an identical message hides that the charge was paid. Record which sentence appeared. |
| **R-N3** | ***BOTH IN ONE FIGHT — THE ROW THE TWO-KEY COMMIT RESTS ON.*** Trigger R-N1's no-arrow notice, then **within 40 ticks (2 seconds)** trigger R-N2's no-rounds notice. The natural sequence does it for you: release on an empty magazine → *left-click to reload* → the reload eats the loose arrows → right-click → no arrow. | **PREDICTED: BOTH messages appear.** **NO UNIT TEST CAN SHOW THIS.** What is guarded today is that the two keys DIFFER, not that a player sees both — a notice can fail to appear for four reasons a key comparison cannot see. **If only one appears, the two-key argument is unproven and the second key is doing nothing.** Slice E's precedent is exact: two refusals sharing a throttle silence each other, **and it only shows up in the one sequence that matters.** |
| **R-N4** | **THE SILENCE, WITH ITS POSITIVE CONTROL. Two halves, one sitting, and the second half is not optional.** (i) Twitch the bow **under three ticks** — a flick of right-click, released immediately. (ii) **Then, changing nothing else**, do a real no-rounds release. | **PREDICTED: (i) NOTHING is said. (ii) the message appears.** **THE SECOND HALF IS WHAT MAKES THE FIRST A READING.** A correct silence and a notice that is broken, unwired or throttled-out are **the same observation** — and (i) alone would be reported as a pass under every one of them. Only a notice firing immediately afterwards, on the same weapon in the same sitting, separates them. **Same move as the pling kept as H-1b's control:** a known-audible thing proves the pipeline is alive before a silence is believed. |

---

## THE BAND ROWS — R4‴, AND THEY ARE STAGED AT BOUNDARIES BECAUSE NOTHING ELSE CAN READ THEM

**R4‴ makes the partial draw three shots — 9 / 17 / 26 — cut by vanilla's power curve in thirds.**
`DrawReleaseTest` guards the cut in `core`; **these rows are the only thing that can show the cut
survives the input path**, which is where the hold is actually counted.

```
held  3 - 8 ticks   ->  band 1   9 damage    tap1
held  9 - 14 ticks  ->  band 2  17 damage    tap2
held 15 - 19 ticks  ->  band 3  26 damage    tap3
held 20+            ->  charged, 34 and homing
```

| row | staging | what to record |
|---|---|---|
| **R-B1** | **BAND 1 AND ITS TOP BOUNDARY.** Tap as briefly as you can, repeatedly, into a mob at point-blank — then a fractionally longer press. **The readable pair is 8 vs 9 ticks**, which is 0.40s vs 0.45s and **cannot be hit reliably by hand**, so this row is run by VOLUME: twenty taps, recording the damage numbers that appear. | **PREDICTED: only `9` and `17` appear, never a number between them.** Record every distinct damage number seen and how many of each. **A `13`, or a `9` where the bow was clearly pulled halfway, is the finding.** The `void` glyph makes each number legible (`✧`), which is why this row is readable at all. |
| **R-B2** | **BAND 2, BOTH ITS BOUNDARIES.** Deliberately half-draw — roughly half a second — twenty times. | **PREDICTED: `17` dominates, with `9` and `26` at the edges of the attempt spread.** **THE INTERESTING RESULT IS A BAND THAT NEVER APPEARS**: if `17` never lands, band 2 is unhittable by hand and that is a finding for Ben about the thirds, not a defect in the cut. |
| **R-B3** | **BAND 3 AND THE ONE-TICK BOUNDARY WITH A CHARGED RELEASE.** Draw to *just* short of the bow's full pull, twenty times, at a mob at range. | **PREDICTED: `26` with NO seeking, and `34` WITH seeking, and nothing between.** **19 vs 20 is the single most important tick in the weapon**: one arrow either way, and the two shots differ in damage AND in whether they home. **Record whether a `26` ever visibly homed** — that would mean the tap path acquired a seek block, which no unit test watches for, since `core` never sees the content file. |

> ### ⚠ WHY ALL THREE ARE AT BOUNDARIES, AND WHY A ROW IN THE MIDDLE OF A BAND IS WORTHLESS HERE
>
> **A tap staged at 5 ticks passes under ANY cut whatsoever** — under the ruled thirds, under the
> rejected pull-texture bands (`0 / 13 / 18`), and under a single flat tap that ignores the hold
> entirely. **The fixture would be measuring itself.** 8-vs-9 and 14-vs-15 are the only ticks in the
> whole range where a wrong cut is observable, which is the same discipline `DrawReleaseTest` uses
> and the same reason R-1 is staged at 2 and 4 rounds rather than 1 and 3.
>
> **AND THE HONEST DIFFICULTY IS STATED RATHER THAN DESIGNED AROUND: A HUMAN CANNOT HIT A SINGLE
> TICK.** One tick is 50ms. **So these rows are run by VOLUME and read as a DISTRIBUTION** — the
> question is *which damage numbers exist*, not *did tick 9 produce a 17*. **A row that pretended to
> stage an exact tick would be reporting precision it cannot have**, which is worse than a
> distribution honestly labelled as one.
>
> > **THE ONE THING NO UNIT TEST CAN SEE, AND THE REASON THESE ROWS ARE NOT REDUNDANT WITH
> > `DrawReleaseTest`:** `core` decides the band; **`dragons_plume.yml` authors the damage**, and
> > nothing checks that `Tap(2)` reaches the binding that says `17`. **A transposition of two
> > bindings' amounts is green everywhere in the suite** and visible only as a number on a mob.

> **R-1 IS `PLAN-dragons-plume.md`'s P5, LANDED — NOT A SECOND COPY OF IT.** The plan's §8 drafted its
> rows *"before the boot"* and says they go into a gate file when the slice is written; this is that
> file for the release half. **Anyone adding P5 again later would be writing the same row twice**,
> and two copies of a row drift exactly the way two accounts of a rule do.

> **THE SECOND HALF IS THE ONE THAT EARNS THE RULING, AND IT IS WHY THE ROW IS TWO PARTS.** R3 says
> the cap is read **LIVE**. *"Reads the magazine live"* and *"reads it once at full charge"* are
> **indistinguishable** on a magazine that does not change during the hold — so a row that only holds
> a short magazine would pass under either implementation. **Staging a reload that completes during
> the draw is what makes the two disagree.**

> **WHY 2 AND 4 AND NOT 1 AND 3, WHICH IS THE SAME DISCIPLINE THE CORE ROW USES.** R1′ made the
> charge 1/3/5, so the cap can land **between** steps for the first time, and R3a rules what happens
> there. **At 1 round and 3 rounds the ruled rule and the rejected one give the same answer** — both
> fire one arrow and three arrows respectively, stranding nothing. A row staged at 1 or 3 passes
> under either implementation and measures the fixture.
>
> **2 and 4 are the only round counts where the two rules disagree**, which is exactly why they are
> the staging. `DrawChargeTest`'s stranding row makes the same choice for the same reason, and its
> mutation matrix records that the older cap row is completely blind to R3a.

## WHY THIS ROW COULD NOT BE RUN AT H1

**Not "hard to stage" — unobservable in principle.** `roundsRemaining()` is `min(loaded, capacity)`,
clamped (slice G), and for the cap to bite it must come in under **5**, the maximum charge:

```
loaded    25 at mint, and IT CANNOT FALL AT H1. Quivers.spendRound has exactly ONE call
          site -- WeaponFire:183, gated on a Success and on the input not being
          left_click -- and the Plume binds NEITHER click, so WeaponFire.attempt returns
          empty for it. H1 fires nothing; left-click only reloads; a full magazine is
          ALREADY_FULL.

capacity  authored 25 plus modifiers, and every instrument ONLY ADDS: /rpg quiversize is
          DoubleArgumentType.doubleArg(0.0, 200.0) -- floor ZERO -- and the
          quiver_size_boost item it mints adds the same bonus.

          min(25, >= 25) = 25,  against a maximum charge of 5.
```

**BOTH HALVES WERE BLOCKED, not just the count:** raising capacity mid-draw does not move
`min(25, capacity)`, so the live re-read had no lever either.

> **THE TRIGGER THAT RELEASED IT WAS THIS SLICE, AND IT IS ALREADY SATISFIED THE MOMENT H2 FIRES
> ONCE.** One arrow takes `loaded` to 24; a few more put it under five and both halves become
> stageable immediately. **That is why the row lives here rather than waiting on a ruling.**
>
> **One other path exists in the arithmetic and not in the tree:** a capacity modifier that
> SUBTRACTS. `QuiverSize.arrows` handles a negative — `QuiverSizeTest` asserts `arrows(-2.1) == -3`
> — so the maths permits it and only the instruments forbid it. **The argument above rests on no
> instrument producing one**, which is a weaker claim than the arithmetic refusing it, and is stated
> as such.

---

## WHY THE NOTICE ROWS ARE FOUR AND NOT TWO

**R-N1 and R-N2 each prove one surface exists. Neither proves the thing the slice was argued on**, and
R-N4's first half proves nothing at all on its own. The two extra rows are the two failures a
straightforward pair would report as passes:

```
R-N1 + R-N2 alone     both messages exist          <- and a SHARED key passes this, if you
                                                      run them more than 2 seconds apart
R-N4 (i) alone        the twitch said nothing      <- and an UNWIRED notice passes this
```

> **THE FIRST IS A TIMING ACCIDENT, WHICH IS WHAT MAKES IT DANGEROUS.** R-N1 and R-N2 run in
> whatever order and at whatever pace a person happens to work at. **Run them a minute apart and a
> single shared key passes both** — the throttle window is 40 ticks and has long since lapsed. The
> defect appears only inside the window, which is exactly where a player meets it and exactly where
> an unhurried tester does not. **R-N3 forces the interval.**
>
> **THE SECOND IS THIS PROJECT'S OLDEST SHAPE:** a check that did not run looks exactly like a check
> that passed. A silence is the purest case — there is nothing to look at either way.

> **AND R-N3's PREDICTION IS THE ONE TO WRITE DOWN BEFORE RUNNING, because the failure is a NON-EVENT.**
> *"Both messages appeared"* is only meaningful against *"both were expected"*. A tester who has not
> written the expectation first sees one message, reads it, acts on it, and never notices the second
> was owed — **which is precisely what the player does, and why the bug would come back as "the reload
> broke my bow" rather than as a missing notice.**

## WHAT THIS GATE DOES NOT COVER

- **The input layer.** The draw, the tracker, the tick and the release's suppression of vanilla's
  shot are `GATE-plume-draw.md`'s, and H-1b there is still unread.
- **The homing constants.** `PLAN-dragons-plume.md` §5 carries them `INHERITED AND UNJUDGED` with
  rows P1-P4, and the sight gate's visible half is P9. H2 makes those runnable for the first time,
  because nothing has ever fired one of these arrows.
- **Whether the notices are the RIGHT WORDS.** These rows check that a message appears, names a
  remedy, and is not its sibling's sentence. **Whether a player finds the wording clear is R-N1's
  "could they act on it" half and nothing more** — and if the answer is no, that is a finding about
  the sentence rather than a row failure.
- **R5 itself.** The notice teaches the workaround; it does not make the workaround less of one.
  **`PlumeNotice.noArrow` carries a deletion trigger** — the day the off-hand requirement is
  replaced, the method and its key go in the same commit — and **R-N1 goes with them.**
