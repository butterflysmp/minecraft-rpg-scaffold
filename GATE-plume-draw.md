# GATE — the Dragon's Plume's draw (slice H1)

**Status: FOUR ROWS READ, ONE OPEN, ONE MOVED.** Every row was written **before any boot**.

```
H-1    READ, SUPERSEDED   on a pling the weapon no longer uses -- now H-1b's positive control
H-1b   OPEN               the only unread row on this page
H-2    MOVED              -> GATE-plume-release.md; it could never have been run here
H-3    PASS               arrow YES, log YES -- the boot's headline result
H-4    RUN                no desync observed, COARSER than the row asked for
H-5    PASS               all five normal -- the gate-too-wide risk is closed
```

**Every observed cell carries its ROUTE**, because a reading whose route is unrecorded cannot be
audited later and this page has already lost track of three. **Only H-3 arrived through the
instrument** — the page's own store. H-1, H-4 and H-5 reached this record **through conversation**,
which is a weaker provenance and is marked as one rather than smoothed over.

> ## THE BLOCKER IS GONE — `dragons_plume.yml` LANDED
>
> **These rows waited on content.** H1's gate is a weapon of ours with `material: bow` binding no
> `right_click`, and the only bow shipped was `hunters_bow`, which binds one. The Plume's file merged
> in `d4af575` with a `draw` binding and no `right_click`, **so the gate is satisfiable and every row
> below can be run.**
>
> **What was never blocked:** H1's arithmetic is `core` and is guarded by `DrawChargeTest` (8 rows),
> which needs no server at all. What waited here was only what a server can answer.

---

## WHAT H1 IS FOR, IN ONE SENTENCE

**A bow that ticks and does not shoot.** Every high-risk unknown in this weapon is in the input layer,
and all of them are answered by a bow that only ticks — so firing, homing, damage and round-spending
are H2's, deliberately, and none of them can confuse a symptom here.

## THE ROWS

| row | staging | what to record | OBSERVED — and BY WHAT ROUTE |
|---|---|---|---|
| **H-1** | ~~Hold a full draw for **five seconds** with a full magazine.~~ **SUPERSEDED BY H-1b — the instrument changed.** | **READ ONCE, ON A `pling`, AND THE READING IS PRESERVED BELOW** rather than overwritten. See the block under this table. | **READ.** Five ticks sounded; a listener who could not see the screen called the count correctly. **Route: the operator, in conversation** — recorded here 2026-09-14, **boot date unknown**. Now the **positive control** for H-1b's fallback. |
| **H-1b** | **NOT RUN.** Hold a full draw for **five seconds** with a full magazine, now that the tick is `block.note_block.basedrum`. | **How many ticks sound, and whether the rise is tellable apart BY EAR** — have a second person, not watching the screen, call the count. Five expected, each step the same RATIO (1.2574). **The second half is the ruling's actual test**: R13 asks whether a player knows **WHICH STEP they are on**, not whether they can count events — so the listener should be asked to start listening LATE and still name the position. **And the fifth step is now a separate question**: a kick at pitch 2.0 is half as long and may stop reading as a kick (see `DrawCharge`). **IF NO DRUM SOUNDS AT ALL, DO NOT CONCLUDE THE TRACKER FAILED — run the fallback step below before recording anything.** | **OPEN. The only unread row on this page.** |
| **H-2** | **MOVED TO `GATE-plume-release.md` — THE CAP IS UNOBSERVABLE IN PRINCIPLE AT H1.** Not "hard to stage": there is no staging. | See the block below for the arithmetic, and the release gate for the row itself. **Do not spend an evening looking for a clever staging; the reason is structural.** | **n/a — moved, not skipped.** |
| **H-3** | **THE ARROW — and this row proves a MECHANISM, not an absence.** One arrow in the off-hand, nothing else in the bag. Draw, hold past full charge, release. | **Is the arrow still there?** It survives only if `clearActiveItem()` inside `PlayerStopUsingItemEvent` made `LivingEntity.releaseUsingItem`'s re-read at offset 72 yield EMPTY, so `BowItem.releaseUsing` — and therefore `draw()`, and therefore `useAmmo` — never ran. **If the arrow is gone, that specific chain is what failed**, and the server log will say so: the `EntityShootBowEvent` guard fires loudly precisely here. | **PASS — arrow YES, log YES.** The arrow survived the release and the loud guard did not fire. **Route: this page's own store**, 2026-09-14T04:42:16Z — the only row here whose reading arrived through the instrument rather than through a conversation. **THE BOOT'S HEADLINE RESULT; see the discharge below.** |
| **H-4** | **THE VISUAL DESYNC.** Draw, hold to five, release — and **watch the first-person hand and a second player's view of you**. Repeat while moving, and while looking up. | **Does the client keep animating a draw it no longer has?** The server clears the active item mid-release; nothing guarantees the client agrees. **Write this row's answer in words, not a verdict** — "the bow snapped back instantly" and "the arm stayed pulled for about a second" are different findings and both are passes for the mechanism. **No amount of bytecode reading could have predicted this row**, which is why it is here. | **RUN — no desync observed, AT A GRANULARITY THAT CANNOT SEPARATE THE TWO SHAPES THIS ROW EXISTS TO SEPARATE.** Operator, verbatim: *"H4 is fine everything looks normal"*. **Route: conversation.** A verdict about the mechanism, **not** the description in words the row asked for — see below. **NOT fully answered.** |
| **H-5** | **THE NEGATIVE ROW, and it is the one that fails if the gate is wrong.** With H1 installed: eat a food item to completion; raise and lower a shield; fire an **ordinary vanilla bow**; drink a potion; use a spyglass. | **Each must behave exactly as it did before.** `PlayerStopUsingItemEvent` fires for **every** item release on the server, so an ungated `clearActiveItem()` would break all of these silently. **This row exists because of the gate, and it is the row that catches a gate that does not gate.** | **PASS — all five normal.** Operator, verbatim: *"H5 is all good to"*. **Route: conversation.** **This is what closes the gate-too-wide risk**, whose failures are silent — see below. |

## H-4's READING IS COARSER THAN ITS ROW, AND THAT IS RECORDED RATHER THAN REPAIRED

**Operator, verbatim: *"H4 is fine everything looks normal"*.**

**The row asked for the answer IN WORDS, and it said why:** *"the bow snapped back instantly"* and
*"the arm stayed pulled for about a second"* are **different findings and both are passes for the
mechanism**. *"Everything looks normal"* does not distinguish them.

> **SO THE CELL HOLDS A VERDICT ABOUT THE MECHANISM AND NOT A DESCRIPTION OF THE BEHAVIOUR**, and it
> says which. **A reading coarser than its row is still a reading** — no desync was observed, which
> is a real answer to *"does the client keep animating a draw it no longer has"*. What it does not
> answer is the shape.
>
> **IT IS NOT ELABORATED INTO DETAIL NOBODY REPORTED, AND IT IS NOT MARKED FULLY ANSWERED.** Writing
> "snapped back instantly" here would be inventing an observation; marking the row closed would lose
> the question. **The row stays RUN and incomplete**, which is the only honest cell.
>
> **The residue is cheap to collect**, if anyone wants it: one more draw, watching the hand.

## H-5 CLOSED THE RISK WHOSE FAILURES ARE SILENT — SAY SO, BECAUSE IT WILL READ AS ROUTINE

**Operator, verbatim: *"H5 is all good to"*.** Food, shield, vanilla bow, potion, spyglass — all five
normal.

**This is the row that catches a gate that does not gate**, and its failure mode is the reason it
cannot be treated as a formality: `PlayerStopUsingItemEvent` fires for **every item release on the
server**, so an ungated `clearActiveItem()` would have broken eating, blocking, drinking, scoping and
every vanilla bow — **silently, on a server where nobody was testing the Plume.** Nothing would have
reddened; somebody would simply have found that food no longer worked.

> **SO H-5 IS WHY NOTHING ELSE ON THAT SERVER BROKE, AND THAT SENTENCE IS HERE BECAUSE A ROW OF FIVE
> "normal"s IS THE MOST LIKELY THING ON THIS PAGE TO BE READ LATER AS BOILERPLATE.** It is the
> opposite: it is the only evidence that the gate on the item is the right width. H-3 proves the
> clear happens; **H-5 proves it happens to nothing else.**

## H-1b's FALLBACK STEP — A SILENT KEY AND A BROKEN TRACKER LOOK IDENTICAL

**Silence has two causes and they are not close in kind.** The sound key is `block.note_block.basedrum`
and **only half of it is verified**: `SoundEvents` declares the FIELD `NOTE_BLOCK_BASEDRUM`, but the
ID STRING is **inferred** from the field-name convention — corroborated by two siblings known to
play, not measured. **A misspelled key is silent, and at H-1b silence reads as the mechanism
failing.**

**So the discriminator lives in this row rather than in whoever is at the keyboard:**

> **If no drum sounds, swap `PlumeDraw.TICK_SOUND` to `block.note_block.pling`, rebuild, and draw
> once.**
>
> ```
> pling sounds, drum does not   ->  THE ID STRING IS WRONG. A typo, not a defect. Fix the string.
> neither sounds                ->  the tracker or the draw failed, and THAT is the real finding.
> ```

**One line each way, and it separates a typo from an architecture failure in a single boot instead of
a debugging session.**

**AND IT IS H-1's SUPERSEDED READING THAT MAKES THIS POSSIBLE.** The pling is not a guess at a
control: it **demonstrably played**, recorded 2026-09-14. **A superseded row that still carries its
reading has become a POSITIVE CONTROL for the instrument** — which is the second reason not to have
overwritten its cell, and one nobody had in mind when the reading was preserved.

> **Same shape as the two silences this page already separates, one layer down:** two different
> causes producing one identical observation, and a cheap experiment only one of them survives.

---

## WHY H-2 LEFT: THE CAP CANNOT BIND AT H1, AND THAT IS ARITHMETIC RATHER THAN A MISSING LEVER

**R3's cap is `roundsRemaining()`, which slice G defines as `min(loaded, capacity)`, clamped.** For
that to stop the ticks it must come in **under 5**, the maximum charge. On a Plume at H1 it cannot:

```
loaded    25 at mint (QuiverItems.stampFull), and IT CANNOT FALL.
          Quivers.spendRound has exactly ONE call site -- WeaponFire:183, gated on a
          Success and on the input not being left_click -- and the Plume binds NO
          left_click and NO right_click, so WeaponFire.attempt returns empty for it.
          H1 fires nothing. Left-click only reloads, and a full magazine is ALREADY_FULL.

capacity  authored 25 plus modifiers, and every instrument that exists ONLY ADDS:
          /rpg quiversize is DoubleArgumentType.doubleArg(0.0, 200.0) -- floor ZERO --
          and the quiver_size_boost item it mints adds the same bonus.

          min(25, >= 25) = 25,  against a maximum charge of 5.
```

> **SO THE CAP IS UNOBSERVABLE IN PRINCIPLE ON A MAGAZINE FIVE TIMES THE MAXIMUM CHARGE THAT CANNOT
> SPEND A ROUND.** That phrasing is chosen over *"not stageable"* deliberately: the second invites
> somebody to go looking for a clever staging, and there is not one to find.
>
> **BOTH HALVES GO, NOT JUST THE COUNT.** The live re-read has no lever either — raising capacity
> mid-draw does not move `min(25, capacity)`, so a reload completing during a hold changes nothing
> observable.
>
> **TRIGGER — THE FIRST SLICE IN WHICH A PLUME ROUND CAN BE SPENT.** That is H2's release: one fired
> arrow takes `loaded` to 24, and a few more make both halves stageable immediately. A condition on
> the tree, not a "later".
>
> **And one other way it could become stageable, which does not exist today:** a capacity modifier
> that SUBTRACTS. `QuiverSize.arrows` handles a negative — `QuiverSizeTest` has a row asserting
> `arrows(-2.1) == -3` — so the arithmetic permits it and only the instruments forbid it. **The
> argument above rests on no instrument producing one, not on the maths refusing it**, which is a
> weaker claim and is stated as such.

**THIS WAS FOUND, TOLD, AND NOT WRITTEN DOWN.** The measurement existed in a chat and never reached
a file, so the row survived a review that read the file and reported what it said. That is this
project's own recorded failure — *a finding that lives only in the conversation is not recorded*.

> ### THREE INSTANCES ON THIS ONE PAGE, AND THAT IS A PATTERN RATHER THAN A RUN OF BAD LUCK
>
> ```
> 1  H-1's pling reading        read, told, never written -- the file still said "NOT RUN"
> 2  H-2's unobservability      measured, told, never written -- the row survived a review
> 3  H-3, H-4 and H-5's results three rows read, the header still said "ONE ROW READ"
> ```
>
> **Two is a coincidence; three is a mechanism, and the mechanism is that this page is updated by
> whoever is writing a commit rather than by whoever ran the boot.** Every instance has the same
> shape: the finding existed, it was communicated, and the durable record did not move — so the next
> reader of the file was told something false by a document that looked maintained.
>
> **IT IS COMPOUNDING RATHER THAN REPEATING.** Instance 2 survived a review *because* of instance 1's
> habit: the file was read, believed, and reported back accurately — and the file was wrong. **A
> stale gate page is worse than an empty one**, because an empty one is obviously unfinished.
>
> **The cheapest fix is not discipline, it is the ROUTE column** now on every observed cell. A cell
> that must name how its reading arrived cannot silently stay empty while a reading exists
> elsewhere, and a reading whose route is "conversation" is visibly weaker than one whose route is
> the instrument.

---

## H-1's READING, PRESERVED — AND A READING IS SCOPED TO THE CONDITIONS IT WAS TAKEN UNDER

**A boot happened, on `block.note_block.pling`, and it read:** five ticks sounded, and a listener who
could not see the screen called the count correctly. **The mechanism worked.**

**THE INSTRUMENT IS ONE OF THOSE CONDITIONS, SO THE PASS DOES NOT CARRY ACROSS THE SWAP.** The tick
is now a `basedrum`, ruled after that listen — *"the piano doesn't suit it, try snare or kick."* A
drum is a different kind of sound, not a different setting of the same one: it is noise rather than a
tone, and it changes what the top of the ladder even is. **So H-1b is NOT RUN, and it is a new row
rather than a fresh figure written into H-1's cell.** Overwriting the cell would have destroyed the
only evidence this mechanism has ever produced.

> **AND THE READING WAS NEVER IN THIS FILE UNTIL NOW, WHICH IS ITS OWN FINDING.** It reached this
> record on **2026-09-14**, from the operator's report, while the file still said `Status: NOT RUN`
> and H-1's cell was the prediction. **`CLAUDE.md`: a finding that lives only in the conversation is
> not recorded** — not greppable by the next person, not surviving the session, unable to fail.
>
> **The boot's own date is NOT KNOWN and is not invented here.** What is dated is the writing-down.
> That gap is exactly what the rule is about: by the time anyone thought to record the reading, the
> one fact nobody thought to keep was when it was taken.

---

> **H-3 AND H-5 FAIL IN OPPOSITE DIRECTIONS, AND THAT IS WHY BOTH ARE HERE.** H-3 fails if the clear
> does not happen; H-5 fails if it happens to things it should not. A gate that is too narrow loses
> the arrow; a gate that is too wide breaks eating. Neither row can see the other's defect.
>
> **BOTH NOW READ PASS, WHICH IS THE ONLY COMBINATION THAT MEANS ANYTHING.** Either alone would have
> been consistent with a gate of the wrong width: H-3 passing on a gate that fires for everything,
> H-5 passing on a gate that fires for nothing. **The pair is the measurement.**

## WHAT THIS GATE DOES NOT COVER

- **Anything that fires.** H1 spawns no projectile, spends no round, deals no damage. H2's rows are
  about arrows; these are about architecture. They live in **`GATE-plume-release.md`**, which already
  holds the one row that had to leave this page.
- **The homing constants.** `PLAN-dragons-plume.md` §5 carries them `INHERITED AND UNJUDGED` with
  rows P1-P4, and nothing here touches them.
- **The pitch ceiling, and the instrument change widened it.** `2.0` is outside knowledge this
  machine cannot measure — the pinned API documents no range and the packet carries a raw float.
  **That half is unchanged.** What changed is how the top of the ladder can FAIL: a pling at 2.0 was
  simply a high note, while **a kick at 2.0 is half as long and may stop reading as a kick at all**.
  So H-1b asks two things of the fifth step — is it audible, and is it still the same sound — and
  either answer is **a reading, not a failure**.

## WHAT WAS READ OUT OF THE JAR, AND THE ONE THING ONLY A SERVER COULD SAY

**MEASURED, from `run/versions/26.1.2/paper-26.1.2.jar`:** that consumption happens inside `draw()`
at offset 71 while `EntityShootBowEvent` is constructed inside `shoot()` at 133 — so the arrow is
gone before that event object exists, and cancelling it can never keep the arrow; that
`PlayerStopUsingItemEvent` fires at 67 and the `useItem` field is re-read at 72; that
`stopUsingItem()` empties that field; and that `Item.releaseUsing` on an empty stack is
`iconst_0; ireturn`, which `AirItem` does not override.

**~~NOT MEASURED, AND IT IS THIS GATE'S JOB:~~ — DISCHARGED BY H-3, 2026-09-14T04:42:16Z.** The one
thing this gate existed to answer was whether `clearActiveItem()` **called from inside that handler**
behaves live as the bytecode reads. It was written as *"the handler runs on the main thread inside
the same call, so it **should** — and 'should' is what the boot is for."*

> **IT HOLDS LIVE.** H-3 read **arrow YES, log YES**: the off-hand arrow survived the release, and
> the loud `EntityShootBowEvent` guard — which fires exactly when the clear has NOT taken — stayed
> silent. **The chain that was read out of bytecode and had never executed on a server now has.**
>
> **THE GUARD'S SILENCE IS HALF THE RESULT, AND IT IS THE HALF THAT IS EASY TO SKIP.** The arrow
> being present is consistent with several things; the arrow being present **while a detector aimed
> at the exact failure said nothing** is what makes this a reading about the mechanism rather than
> about the outcome. That is why the guard was built to log rather than to cancel quietly.
