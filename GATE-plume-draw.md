# GATE — the Dragon's Plume's draw (slice H1)

**Status: FOUR ROWS READ, ONE OPEN, ONE MOVED.** Every row was written **before any boot**.

```
H-1    READ, SUPERSEDED   on a pling the weapon no longer uses -- now H-1b's positive control
H-1b   OPEN               tests POSITION, not count -- and its instrument has been DELETED
H-2    MOVED              -> GATE-plume-release.md; it could never have been run here
H-3    PASS               arrow YES, log YES -- the boot's headline result
H-4    RUN, INCOMPLETE    no desync observed, COARSER than the row asked for; residue rides with H-1b
H-5    PASS               all five normal -- the gate-too-wide risk is closed
```

> ## THE SOUND IS RULED — `block.note_block.hat` — AND H-1b IS STILL OPEN
>
> **Ben heard it and it suits the weapon.** Ruled 2026-09-14, on feel, which is the only instrument
> there has ever been for the material.
>
> **THAT SETTLES THE MATERIAL AND IT DOES NOT DISCHARGE H-1b.** The row's question is R13's
> property — **can a player tell HOW CHARGED THEY ARE**, which is naming a POSITION on the ladder by
> ear, having possibly started listening late. **A feel judgement does not establish that.** No
> position trial has been run on any candidate: this page's trial section carries **no hat trials at
> all**, and H-1's preserved reading is about COUNTING, on a pling.
>
> **So what H-1b now owes is narrower and sharper than it was: the position reading ON THE RULED
> KEY.** It is one key instead of eight, and it is the one that ships.
>
> > **DO NOT MARK THE ROW PASSED BECAUSE THE QUESTION THAT PROMPTED IT WAS SETTLED.** That is a
> > verdict standing in for a measurement, and it is the failure **this page has already recorded
> > three times** — a finding communicated and never written down, a row surviving a review because
> > the file was believed, a header saying ONE ROW READ while three had been. **A ruling about the
> > sound is not a reading about the ladder.**
>
> **AND THE INSTRUMENT WENT IN THE SAME COMMIT.** `/rpg drawsound` was born with a deletion trigger
> naming this exact day, written in three places. It was honoured as written — see the instrument
> section below, which now records what that costs the row.
>
> **H-4's residue stays open too, and still rides on the same draw.**

> ## AND THE LADDER IS THREE STEPS NOW — R1′ — SO H-1b IS A MATERIALLY EASIER QUESTION
>
> **R1′ amends the charge to 1 / 3 / 5 arrows: three steps, three sounds, two seconds past full
> draw.** The sound ruling is untouched — Ben ruled the timbre, not the ladder.
>
> **THE HARDEST PART OF R13 WAS TELLING FIVE STEPS APART BY EAR. IT IS NOW THREE, TWICE AS FAR
> APART** — a ratio of `1.5811` per step against `1.2574`, about eight semitones instead of four.
>
> > **SO A PASS MEANS LESS THAN IT WOULD HAVE, AND THE ROW SAYS SO RATHER THAN QUIETLY GETTING
> > EASIER.** A row whose difficulty drops between drafting and running, with no note, produces a
> > green cell that the next reader weighs against the question as originally written. **The question
> > this row now answers is "can a listener place one of THREE widely spaced steps", and that is not
> > the question it was written for.**
> >
> > **It is still the right row to run** — R13's property is unchanged and three steps is what ships.
> > What changed is how much a pass is evidence of.
>
> **The staging is otherwise untouched: the listener names WHICH STEP, never how many they heard.**
> Three trials, each entered at a different drawer-chosen step. **With three steps there are only
> three entry points**, so "a different step each time" now means all of them.

> **H-1b AND H-4's RESIDUE ARE ONE SITTING, AND EACH ROW SAYS SO.** Same weapon, same full draw,
> **same three seconds** (R1′ — it was five) — **only the eyes move.** H-1b asks what the EAR can
> place; H-4's residue asks
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
> **What was never blocked:** H1's arithmetic is `core` and is guarded by `DrawChargeTest` (12 rows
> since R1′), which needs no server at all. What waited here was only what a server can answer.

---

## WHAT H1 IS FOR, IN ONE SENTENCE

**A bow that ticks and does not shoot.** Every high-risk unknown in this weapon is in the input layer,
and all of them are answered by a bow that only ticks — so firing, homing, damage and round-spending
are H2's, deliberately, and none of them can confuse a symptom here.

## THE ROWS

| row | staging | what to record | OBSERVED — and BY WHAT ROUTE |
|---|---|---|---|
| **H-1** | ~~Hold a full draw for **five seconds** with a full magazine.~~ **SUPERSEDED BY H-1b — the instrument changed.** | **READ ONCE, ON A `pling`, AND THE READING IS PRESERVED BELOW** rather than overwritten. See the block under this table. | **READ.** Five ticks sounded; a listener who could not see the screen called the count correctly. **Route: the operator, in conversation** — recorded here 2026-09-14, **boot date unknown**. Now the **positive control** for H-1b's fallback. |
| **H-1b** | **NOT RUN. ONE KEY NOW, NOT EIGHT — `block.note_block.hat`, the ruled one, which is what ships.** Two people, one sitting, one full magazine. The listener **faces away from the screen and is brought in LATE**: the drawer starts the draw, waits a step or two of their own choosing, and only then says *"listening"*. **The listener's job is to name WHICH STEP the next tick is — "that was the second of three" — never how many they heard.** **Three trials, each starting at a different drawer-chosen step — with three steps that is ALL OF THEM**; the listener is never told the entry point. **THE SWEEP IS GONE WITH ITS INSTRUMENT: `/rpg drawsound` was deleted when the sound was ruled**, so changing the key now costs an edit, a rebuild, a redeploy and a boot — which is why this row tests the ruled key and no other. **This rides in the same sitting as H-4's residue — see that row; do not run either alone.** | **CAN A LISTENER PLACE A TICK WITHOUT COUNTING FROM THE START?** Record, per trial: **the step the drawer entered on, the step the listener called, and the true step.** A count is not a pass and must not be written into this cell — **counting is what H-1 already measured on the pling, and it is exactly the thing R13 does not ask.** **THREE steps now (R1′), each a ratio of `1.5811` above the last — about eight semitones, where five steps were four.** **And the TOP step is a separate question**: pitch is a playback rate, so any note-block percussion at 2.0 is half as long and brighter — **whether a hat at 2.0 still reads as a hat is unmeasured** (see `DrawCharge`), **and R1′ makes a player meet it on every full release rather than only on the patient ones.** **IF NOTHING SOUNDS AT ALL, DO NOT CONCLUDE THE TRACKER FAILED — run the fallback step below before recording anything.** **AND IF THE RULED KEY FAILS THE POSITION TEST, THAT IS NOT A ROW FAILURE — it is a finding about R13 and it goes to Ben**, because the ruling assumes a rising tick can be placed by ear and this row would have measured that it cannot. | **OPEN. The only unread row on this page.** **THE RULING DID NOT CLOSE IT** — the key is settled on FEEL; this row is the POSITION reading, and no hat trial exists. **AND R1′ MADE IT EASIER — see the note above the table; a pass now means less than it would have.** |
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

## THE INSTRUMENT — `/rpg drawsound`, DELETED 2026-09-14 ON ITS OWN TRIGGER

**It existed, it was never used for the row it was built for, and it went anyway.** The trigger said
*the charge tick's sound is ruled, and on that day this subcommand and `PlumeDraw`'s mutable
`tickSound` field are deleted IN THE SAME COMMIT that authors the ruling* — written in three places
before it was needed: the field's javadoc, the subcommand's own comment, and this section. **Ben
ruled `block.note_block.hat` and the trigger was honoured as written.**

```
/rpg drawsound                 -- print the live key and the candidate list      GONE
/rpg drawsound <key>           -- set it, for this server run only               GONE
```

> **WHY IT WENT WHILE H-1b IS STILL OPEN, WHICH IS THE OBVIOUS OBJECTION AND WAS ANSWERED RATHER
> THAN OVERLOOKED.** The trigger is *the sound is ruled*, not *the row is read*, and those came apart
> — the ruling arrived on feel before any position trial. **Following the trigger as written costs
> this row its sweep. Re-reading the trigger to mean something more convenient costs the project the
> trigger.** An instrument that outlives its trigger is the thing this project keeps finding: the dev
> weapons and `/kit` are both still parked, years of "we will delete it later" each. **If the sound
> reopens, re-adding this is a small slice** — a literal, a suggester and one mutable field.

**WHAT IT COSTS THE ROW, STATED PLAINLY BECAUSE A DELETION'S BILL ARRIVES LATER.** Changing the key
is back to an edit, a rebuild, a redeploy and a boot — **exactly the per-candidate cost this command
was built to remove**, and the reason H-1b now tests one key rather than sweeping eight. The
fallback below pays that cost too.

### THE MEASUREMENT SURVIVES THE INSTRUMENT, AND THIS IS WHERE IT IS RESTATED

**All eight candidate ids were MEASURED PRESENT in the pinned jar** — extracted from `SoundEvents`'
1808 registered ids, not guessed from field names. **That reading was taken on a fixture that no
longer exists** (the command's suggester list), so it is restated here rather than left to be
recovered from a deleted file:

```
block.note_block.hat        *** THE RULED TICK, 2026-09-14, on feel ***
block.note_block.basedrum   the kick -- ruled, shipped, and superseded by the hat
block.note_block.snare      the kick ruling's recorded alternative; never auditioned
ui.button.click             ) the click candidates, never auditioned: R13 asks whether a
block.lever.click           ) player can PLACE a step, and a click's ATTACK is sharper --
block.comparator.click      ) a pitched click may place where a pitched drum does not
block.stone_button.click_on )
block.note_block.pling      THE POSITIVE CONTROL -- H-1's demonstrably-audible sound, and
                            NOT a position candidate; see the fallback below
```

> **ONE PROPERTY OF THE RULED KEY WAS RECORDED BEFORE THE LISTEN AND IS NOT THE REASON FOR THE
> RULING.** The deleted list described the hat as *the note-block family's own click, and the only
> candidate designed to be pitched across the full range*. **Written while the list was assembled,
> before anyone heard it.** It is kept because it is a fact about the sound and it bears on the fifth
> step — **and it is not evidence for the ruling, which was given as "it suits the weapon".**
> Retrofitting that note as the rationale would be putting an argument in Ben's mouth.

> **AND THE ARGUMENT THAT CHOSE THE KICK IS WITHDRAWN RATHER THAN RETARGETED.** *A snare's sharper
> transient is easier to COUNT; a kick carries the rise more legibly and so tells you WHICH STEP you
> are on* — that argument selected **a different key from the one now ruled**, so it does not become
> an argument for the hat by having survived it. **Same shape as `c64c181`**, where the tick became a
> kick and the SEMITONE argument that had chosen the ladder was withdrawn rather than reworded — *the
> argument that chose the ladder does not survive with it*.
>
> **THE LADDER ITSELF HAS NOW SURVIVED TWO MATERIAL CHANGES AND HAS NOT MOVED**, which is the best
> evidence there is that it is about RATIOS rather than about the sound it is played on. Recorded at
> `DrawCharge`'s pitch section, which is the account; this is the pointer.

## H-1b's FALLBACK STEP — A SILENT KEY AND A BROKEN TRACKER LOOK IDENTICAL

**Silence has two causes and they are not close in kind**, and **one of the two has since been
closed by measurement rather than by argument.**

### THE ID STRING IS MEASURED, AND THE TYPO BRANCH SHRANK — IT DID NOT VANISH

**This section used to read that only half the key was verified**: `SoundEvents` declares the FIELD
`NOTE_BLOCK_BASEDRUM`, and the ID STRING `block.note_block.basedrum` was **inferred** from the
field-name convention, corroborated by two siblings known to play. **That inference is now
unnecessary — and the measurement covers the newly ruled key too**, because it swept all eight at
once rather than only the incumbent.

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
| the SHIPPED key is misspelled | **closed by measurement** — `block.note_block.hat` is in the jar's own table, swept with the other seven |
| a key typed at the console is misspelled | **UNREACHABLE — there is no console any more.** It was *closed by construction* while `/rpg drawsound` refused an unresolvable key through `Registry.SOUND_EVENT`; with the command deleted, no key can be typed at all. **The cause is gone, not guarded** |
| the key resolves and is still inaudible | **OPEN** — resolution is not audibility; category, volume, distance and a resource pack all sit downstream of it |
| the tracker or the draw never ran | **OPEN, and it is the finding** |

**Two of four are not live, which is why the fallback stays.** The branch was never only about typos
— **it separates a sound problem from an architecture problem**, and the architecture half is the
half that matters. Deleting a discriminator because some of its causes were eliminated leaves the
remaining causes sharing one observation again, which is the situation it was written to end.

> **THE SECOND ROW IS WHY *CLOSED* AND *UNREACHABLE* ARE WRITTEN AS DIFFERENT WORDS HERE.** *Closed
> by construction* names a guard doing work; **the guard went with the command.** A reader who finds
> the old wording standing would believe a refusal path protects them, and there is no longer any
> path to protect. Same reason the arms of a validator get marked when no shipped content reaches
> them.

### THE FALLBACK — AND IT COSTS A REBUILD AGAIN

> **If nothing sounds, put `block.note_block.pling` in `PlumeDraw.TICK_SOUND`, rebuild, redeploy,
> and draw once.**
>
> ```
> pling sounds, the hat does not  ->  A SOUND PROBLEM. The key resolves but does not carry.
>                                     Not a defect in the draw.
> neither sounds                  ->  the tracker or the draw failed, and THAT is the real finding.
> ```

**It cost one line at a console for exactly as long as `/rpg drawsound` existed, and it is back to an
edit, a rebuild, a redeploy and a boot.** That is the bill for honouring the deletion trigger, and it
is written here rather than discovered by whoever runs the row. **It is still worth running: a
rebuild is minutes, and the alternative is recording a finding about the tracker that is really a
finding about a sound.**

**AND IT IS H-1's SUPERSEDED READING THAT MAKES IT POSSIBLE AT ALL.** The pling is not a guess at a
control: it **demonstrably played**, recorded 2026-09-14. **A superseded row that still carries its
reading has become a POSITIVE CONTROL** — which is the second reason not to have overwritten its
cell, and one nobody had in mind when the reading was preserved.

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

> **THE FIVE IS PART OF THE READING AND IS NOT A TYPO: R1′ MAKES IT UNREPRODUCIBLE.** That boot ran
> the one-arrow-per-second ladder, where a full draw sounded five times over four seconds. **A full
> draw now sounds THREE times over two.** The reading is preserved exactly as taken — a measurement
> outlives the configuration it was taken on, provided the record says the configuration is gone.

**THE INSTRUMENT IS ONE OF THOSE CONDITIONS, SO THE PASS DOES NOT CARRY ACROSS THE SWAP — AND THERE
HAVE NOW BEEN TWO SWAPS.** The tick became a `basedrum` after that listen — *"the piano doesn't suit
it, try snare or kick"* — and is now a `hat`, ruled 2026-09-14 on feel. **A drum is a different kind
of sound from a tone, not a different setting of the same one:** it is noise rather than pitch, and
it changes what the top of the ladder even is. **So H-1b is NOT RUN, and it is a new row rather than
a fresh figure written into H-1's cell.** Overwriting the cell would have destroyed the only evidence
this mechanism has ever produced.

> **AND THE SECOND SWAP SCOPES THE ROW AGAIN RATHER THAN THE READING.** H-1b was written against the
> kick; it is now run against the hat. **Nothing about the row's question moved** — a position test
> is a position test — but **the key under test did**, and a trial recorded on one key is not a
> reading about another. **There are no kick trials either**, so nothing is lost by the move; what
> would have been lost is a hat row quietly inheriting a kick row's staging without saying so.

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
- **The pitch ceiling, and moving off a tone widened it.** `2.0` is outside knowledge this machine
  cannot measure — the pinned API documents no range and the packet carries a raw float. **That half
  is unchanged by either swap.** What changed when the tick stopped being a tone is how the top of
  the ladder can FAIL: pitch is a **playback rate**, so any note-block percussion at 2.0 is **half as
  long and brighter**, where a pling at 2.0 was simply a high note. So H-1b asks two things of the
  fifth step — is it audible, and is it still the same sound — and either answer is **a reading, not
  a failure**.

  > **WHAT THE HAT DID TO THAT RISK IS UNMEASURED, AND IS NOT ESTIMATED HERE.** A hat is the driest
  > and shortest of the three, so it has less body to lose than a kick had — **but nobody has
  > listened to one at 2.0**, and a plausible sentence about which way it went would sit in a section
  > of measured ones and be read as another. H-1b's fifth-step question is unchanged and still owed.

## WHAT WAS READ OUT OF THE JAR, AND THE ONE THING ONLY A SERVER COULD SAY

**MEASURED, from `run/versions/26.1.2/paper-26.1.2.jar`:** that consumption happens inside `draw()`
at offset 71 while `EntityShootBowEvent` is constructed inside `shoot()` at 133 — so the arrow is
gone before that event object exists, and cancelling it can never keep the arrow; that
`PlayerStopUsingItemEvent` fires at 67 and the `useItem` field is re-read at 72; that
`stopUsingItem()` empties that field; and that `Item.releaseUsing` on an empty stack is
`iconst_0; ireturn`, which `AirItem` does not override.

**AND, ADDED 2026-09-14: the sound ids.** `SoundEvents` disassembles to **1808** id strings, and all
eight of this gate's candidates are among them — so **`block.note_block.hat`, the key that now
ships, is MEASURED PRESENT** rather than inferred from a field name, and so was the `basedrum` it
replaced. **The sweep covered all eight at once, which is why the ruling needed no second read.**

> **THE SAME READ ALSO CONFIRMED `Registry.SOUND_EVENT` EXPOSES A `@Nullable get(NamespacedKey)`,
> AND THAT HALF NOW HAS NO CONSUMER.** It is what let `/rpg drawsound` **refuse** an unresolvable key
> rather than print a disclaimer; the command was deleted on its trigger the same day the sound was
> ruled. **The fact about the platform stands and is kept for whoever re-adds the command** — the
> measurement outlives the instrument, provided the record says the instrument is gone.

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
