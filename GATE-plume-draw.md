# GATE — the Dragon's Plume's draw (slice H1)

**Status: FOUR ROWS READ, ONE OPEN AND RESTAGED, ONE MOVED.** Every row was written **before any
boot**.

```
H-1    READ, SUPERSEDED   on a pling the weapon no longer uses -- now H-1b's positive control
H-1b   OPEN, RESTAGED     tests POSITION, not count -- and it now has an instrument
H-2    MOVED              -> GATE-plume-release.md; it could never have been run here
H-3    PASS               arrow YES, log YES -- the boot's headline result
H-4    RUN, INCOMPLETE    no desync observed, COARSER than the row asked for; residue rides with H-1b
H-5    PASS               all five normal -- the gate-too-wide risk is closed
```

> **H-1b AND H-4's RESIDUE ARE ONE SITTING, AND EACH ROW SAYS SO.** Same weapon, same full draw,
> same five seconds — **only the eyes move.** H-1b asks what the EAR can place; H-4's residue asks
> what the HAND does at release. Running them separately costs a second boot and buys nothing, and
> **a row that only one of the two rows mentions is a row that gets run alone**, which is how H-4
> came back coarser than it was written.

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
| **H-1b** | **NOT RUN — AND RESTAGED, BECAUSE THE OLD STAGING COULD BE PASSED WITHOUT ANSWERING R13.** Two people, one sitting, one full magazine. The listener **faces away from the screen and is brought in LATE**: the drawer starts the draw, waits a step or two of their own choosing, and only then says *"listening"*. **The listener's job is to name WHICH STEP the next tick is — "that was the third of five" — never how many they heard.** At least three trials per sound, each starting at a **different, drawer-chosen** step; the listener is never told the entry point. **Sweep the sounds live with `/rpg drawsound <key>`** — `block.note_block.basedrum` first, because it is the ruled one, then the click candidates listed below. **This rides in the same sitting as H-4's residue — see that row; do not run either alone.** | **CAN A LISTENER PLACE A TICK WITHOUT COUNTING FROM THE START?** Record, per trial: **the step the drawer entered on, the step the listener called, and the true step.** A count is not a pass and must not be written into this cell — **counting is what H-1 already measured on the pling, and it is exactly the thing R13 does not ask.** Five steps, each a ratio of **1.2574** above the last. **And the fifth step is a separate question**: a kick at pitch 2.0 is half as long and may stop reading as a kick (see `DrawCharge`). **IF NOTHING SOUNDS AT ALL, DO NOT CONCLUDE THE TRACKER FAILED — run the fallback step below before recording anything.** **AND IF EVERY CANDIDATE FAILS THE POSITION TEST, THAT IS NOT A ROW FAILURE — it is a finding about R13 and it goes to Ben**, because the ruling assumes a rising tick can be placed by ear and this row would have measured that it cannot. | **OPEN. The only unread row on this page.** |
| **H-2** | **MOVED TO `GATE-plume-release.md` — THE CAP IS UNOBSERVABLE IN PRINCIPLE AT H1.** Not "hard to stage": there is no staging. | See the block below for the arithmetic, and the release gate for the row itself. **Do not spend an evening looking for a clever staging; the reason is structural.** | **n/a — moved, not skipped.** |
| **H-3** | **THE ARROW — and this row proves a MECHANISM, not an absence.** One arrow in the off-hand, nothing else in the bag. Draw, hold past full charge, release. | **Is the arrow still there?** It survives only if `clearActiveItem()` inside `PlayerStopUsingItemEvent` made `LivingEntity.releaseUsingItem`'s re-read at offset 72 yield EMPTY, so `BowItem.releaseUsing` — and therefore `draw()`, and therefore `useAmmo` — never ran. **If the arrow is gone, that specific chain is what failed**, and the server log will say so: the `EntityShootBowEvent` guard fires loudly precisely here. | **PASS — arrow YES, log YES.** The arrow survived the release and the loud guard did not fire. **Route: this page's own store**, 2026-09-14T04:42:16Z — the only row here whose reading arrived through the instrument rather than through a conversation. **THE BOOT'S HEADLINE RESULT; see the discharge below.** |
| **H-4** | **THE VISUAL DESYNC.** Draw, hold to five, release — and **watch the first-person hand and a second player's view of you**. Repeat while moving, and while looking up. | **Does the client keep animating a draw it no longer has?** The server clears the active item mid-release; nothing guarantees the client agrees. **Write this row's answer in words, not a verdict** — "the bow snapped back instantly" and "the arm stayed pulled for about a second" are different findings and both are passes for the mechanism. **No amount of bytecode reading could have predicted this row**, which is why it is here. | **RUN — no desync observed, AT A GRANULARITY THAT CANNOT SEPARATE THE TWO SHAPES THIS ROW EXISTS TO SEPARATE.** Operator, verbatim: *"H4 is fine everything looks normal"*. **Route: conversation.** A verdict about the mechanism, **not** the description in words the row asked for — see below. **NOT fully answered. THE RESIDUE IS ONE MORE DRAW, WATCHING THE HAND, AND IT RIDES IN H-1b's SITTING** — same weapon, same five-second draw, only the eyes move. Named in H-1b's staging as well, so neither row can be run alone. |
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

## THE INSTRUMENT — `/rpg drawsound`, AND THE DAY IT GETS DELETED

**H-1b is a question about a sound, and until now the only way to change the sound was to edit a
constant, rebuild, redeploy and reboot.** That is a per-candidate cost of minutes on a question with
**eight** candidates, which is how a sweep silently turns into "we tried the first one and it seemed
fine".

```
/rpg drawsound                 -- print the live key and the candidate list
/rpg drawsound <key>           -- set it, for this server run only
```

**It is the project's existing shape, not a new one.** `/rpg quiversize`, `/rpg reloadtime` and
`/rpg firerate` all exist for the same reason: a question needed an instrument, and an instrument
that requires a rebuild does not get used.

**THE CANDIDATES, ALL EIGHT MEASURED PRESENT IN THE PINNED JAR:**

```
block.note_block.basedrum   THE RULED TICK -- the incumbent, and what H-1b tests first
block.note_block.snare      the recorded alternative, should the kick not carry
block.note_block.hat        a drier, shorter drum
ui.button.click             ) the click candidates: R13 asks whether a player can place
block.lever.click           ) a step, and a click's ATTACK is sharper than a kick's --
block.comparator.click      ) a pitched click may place where a pitched kick does not
block.stone_button.click_on )
block.note_block.pling      THE POSITIVE CONTROL -- H-1's demonstrably-audible sound, and
                            NOT a position candidate; see the fallback below
```

**IT REFUSES A KEY IT CANNOT RESOLVE, AND THAT IS A DECISION.** The command looks the key up in
`Registry.SOUND_EVENT` and, on a miss, prints the refusal and **leaves the live sound alone**. The
alternative — accept anything and let `playSound` swallow it — would hand H-1b a fourth cause of
silence at exactly the moment it is trying to tell three apart. **An instrument that can be put into
a state it cannot report is not an instrument.**

> ### THE DELETION TRIGGER, WRITTEN DOWN BEFORE IT IS NEEDED
>
> **WHEN THE TICK SOUND IS RULED — that is, when H-1b comes back and Ben names the key — this
> subcommand and `PlumeDraw`'s mutable `tickSound` field BOTH GO, and the ruled key becomes a
> constant again.**
>
> **It is written here and in the field's own javadoc**, because a dev lever with no stated end
> outlives its question and becomes a thing nobody dares remove. The field is mutable **only** to
> serve this gate row; the moment the row closes, mutability is a liability with no reader.

## H-1b's FALLBACK STEP — A SILENT KEY AND A BROKEN TRACKER LOOK IDENTICAL

**Silence has two causes and they are not close in kind**, and **one of the two has since been
closed by measurement rather than by argument.**

### THE ID STRING IS NOW MEASURED, AND THE TYPO BRANCH SHRANK — IT DID NOT VANISH

**This section used to read that only half the key was verified**: `SoundEvents` declares the FIELD
`NOTE_BLOCK_BASEDRUM`, and the ID STRING `block.note_block.basedrum` was **inferred** from the
field-name convention, corroborated by two siblings known to play. **That inference is now
unnecessary.**

> **MEASURED 2026-09-14** against the pinned `run/versions/26.1.2/paper-26.1.2.jar`: `SoundEvents`
> disassembles to **1808 id strings**, and all eight ids this gate cares about are among them —
> `block.note_block.basedrum`, `.snare`, `.hat`, `.pling`, `ui.button.click`, `block.lever.click`,
> `block.comparator.click`, `block.stone_button.click_on`.
>
> **AND THE EARLIER ATTEMPT AT THIS MEASUREMENT FOUND NOTHING, FOR A REASON WORTH KEEPING.** The
> first sweep grepped the disassembly for a *quoted* `"block.note_block.basedrum"` and came back
> empty — `javap` renders a string constant as `// String block.note_block.basedrum`, **with no
> quotes at all.** A pattern that cannot match the text it is looking for returns the same silence
> as a genuine absence, and that silence was very nearly written down here as *"unverifiable"*.

**What that does to the branch below, stated exactly, because the temptation is to delete it:**

| cause of silence | status now |
|---|---|
| the DEFAULT key is misspelled | **closed by measurement** — the id is in the jar's own table |
| a key typed at the console is misspelled | **closed by construction** — `/rpg drawsound` resolves through `Registry.SOUND_EVENT` and **REFUSES** a key it cannot find, leaving the live sound untouched |
| the key resolves and is still inaudible | **OPEN** — resolution is not audibility; category, volume, distance and a resource pack all sit downstream of it |
| the tracker or the draw never ran | **OPEN, and it is the finding** |

**Two of four closed is why the fallback stays.** The branch was never only about typos — **it
separates a sound problem from an architecture problem**, and the architecture half is the half that
matters. Deleting a discriminator because one of its causes was eliminated leaves the remaining
causes sharing one observation again, which is the situation it was written to end.

### THE FALLBACK, AS IT IS NOW RUN — NO REBUILD

> **If nothing sounds, type `/rpg drawsound block.note_block.pling` and draw once.**
>
> ```
> pling sounds, the other does not  ->  A SOUND PROBLEM. The key resolves but does not carry; try
>                                       the next candidate. Not a defect in the draw.
> neither sounds                    ->  the tracker or the draw failed, and THAT is the real finding.
> ```

**It used to cost an edit, a rebuild and a reboot. It now costs one line at the console**, which is
the whole reason the command exists — and the reason the sound sweep and the position test can
happen in one sitting instead of one per build.

**AND IT IS H-1's SUPERSEDED READING THAT MAKES THIS POSSIBLE.** The pling is not a guess at a
control: it **demonstrably played**, recorded 2026-09-14. **A superseded row that still carries its
reading has become a POSITIVE CONTROL for the instrument** — which is the second reason not to have
overwritten its cell, and one nobody had in mind when the reading was preserved.

> **THE PLING IS A CONTROL FOR AUDIBILITY AND FOR NOTHING ELSE.** H-1 measured that a listener could
> COUNT five plings. **It did not measure that anyone could place one**, and H-1b must not be allowed
> to inherit that pass — the pling was ruled out as the tick sound, so a position result taken on it
> would be a reading about a sound the weapon does not use. Use it to prove the pipeline is alive,
> then swap back.

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

> **AND THE SECOND SCOPE, FOUND WHEN H-1b WAS RESTAGED: THIS READING IS ABOUT COUNTING, AND R13 IS
> NOT.** *"Called the count correctly"* says a listener who heard the whole draw could total the
> events. **It says nothing about whether a listener arriving mid-draw could name WHICH step they
> were on** — and that second thing is the ruling's actual claim, that a rising tick tells a player
> how charged they are.
>
> **THE ORIGINAL STAGING COULD HAVE BEEN PASSED WITHOUT ANSWERING THE QUESTION IT WAS WRITTEN FOR**,
> because five evenly spaced events are countable whether or not they are distinguishable. The
> restaged row forces the harder property by denying the listener the start.
>
> So H-1's reading survives as **two** controls and neither is a position result: the pling is
> **audible** (which is what the fallback uses it for), and the tracker **emits five events**.

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

**AND, ADDED 2026-09-14: the sound ids.** `SoundEvents` disassembles to **1808** id strings, and all
eight of this gate's candidates are among them — so `block.note_block.basedrum` is **MEASURED, no
longer inferred from its field name**. The same read confirmed `Registry.SOUND_EVENT` exposes a
`@Nullable get(NamespacedKey)`, which is what lets `/rpg drawsound` **refuse** an unresolvable key
rather than print a disclaimer. See the fallback section for what this closed and what it did not.

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
