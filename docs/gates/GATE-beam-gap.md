# GATE — the beam origin gap

**This file is the source of truth for the beam-gap boot gate's CONTENT.** It is versioned with the
code because for every row below **these are the only checks that exist anywhere in the project.**
The suite passes with all of them deleted — and **not one of its tests can see a single pixel.**

> **NO SUITE TOTAL IS QUOTED IN THAT SENTENCE, AND THE FIRST DRAFT QUOTED ONE.** *"Not one of its
> tests can see a single pixel"* is the entire claim; a number adds nothing to it and **ages out the
> moment any test lands anywhere in the project**, with no way for a later reader to tell a live
> figure from a stale one. This was the **third** instance on this branch, written in the commit
> immediately after removing the other two — because those were figures about a *mutation run*, and
> removing them felt like the rule. **It is not. ANY suite figure outside a verify report ages out
> and cannot announce that it has**, which is exactly where `GATE-volley.md`'s `1462` lived.

> ### THE GATE EXISTS IN TWO DOCUMENTS, AND THE OTHER ONE LEADS
>
> The operator ticks through a **published HTML gate page**. That page is what gets edited during a
> session, so **it leads and this file follows.**
>
> | | authoritative on |
> |---|---|
> | **this file** | **CONTENT** — what a row says, its staging, its expected figures, its reasoning |
> | **the page** | **WHAT WAS ACTUALLY RUN** — ticks, observations, the order rows were taken in |
>
> **Update this file when the page changes, in the same session.** The page is not versioned, so a
> divergence that outlives the session is only findable by someone reading both. `GATE-ignite.md`
> records two instances of that drift in three days, invisible from both sides until it was.

---

## WHAT THIS GATE IS FOR, AND THE ONE DISTINCTION THAT ORGANISES IT

`BEAM_ORIGIN_GAP` skips the first blocks of every ray weapon's beam. `GATE-cursed-emerald.md` CE4
found the defect it addresses: the beam is hard to see through at **both** 3 and 30 blocks, which is
evidence for a fixed **near-field** cause rather than density.

> ### "DOES THE GAP EXIST" AND "DOES THE GAP HELP" ARE SEPARATE ROWS, AND A TICK ON ONE IS NOT THE OTHER
>
> **A slice can land with the constant plainly working and the visual no better.** That is a real
> outcome and these rows must be able to say it. **G-pb witnesses EXISTENCE. G5a and G5b witness
> EFFECT.** Do not let a green G-pb be read as the gap having helped.

**The unit tests already pin the mechanism**, so no row here re-checks it: that the gap is measured
from the ray origin, that it is spent once rather than per chunk plane, that a segment shorter than
the gap is suppressed and collapses into its own chunk column, and that each volley shot recomputes
it from that shot's own aim. **None of that is reachable from a boot gate, and none of these rows is
evidence for it.**

## THE CONSTANT IS PROVISIONAL, AND THIS FILE IS WHAT RULES IT

`1.0` is `cfde822`'s number for `cfde822`'s geometry. **G5a and G5b are the authority.** Until they
run, the number is nobody's — changing it is housekeeping rather than a decision. When they rule it,
`CastExecutor`'s javadoc gets an **ADOPTED** line and the provisional paragraph goes, on the
`Ignite` precedent.

**There is a ceiling and nothing enforces it:** a beam shorter than the gap draws **nothing**, so at
a gap of 3 both CE4's near staging and L0's fire at a wall, see no beam, and report *"not
blinding"* — **true, and measuring nothing.** Keep it well under 3.

---

## ROWS

Written before the boot, not after, and none of them is a tick-box.

> ### RUN 2026-09-10. **THE GAP IS ADOPTED AT 1.0 — AND THE WHOLE GATE WAS ANSWERED BY BLANKET.**
>
> **The operator's ruling: *"it's much better."*** That is his call on a feel question and it is
> final. `BEAM_ORIGIN_GAP` is **ADOPTED**.
>
> **TWO SENTENCES CARRY THIS ENTIRE GATE** — *"boot gates all green"* and *"it's much better"* — and
> the gate page's database is **empty**, so there is no per-row record behind them. **Every row below
> is therefore recorded PER-ROW IN SHAPE AND BLANKET IN EVIDENCE, and each says which.**
>
> | row | verdict | evidence class |
> |---|---|---|
> | **G-pb** | **PASS** | blanket — **but independently implied**, see below |
> | **G5a** | **BETTER** | **blanket** — the ruling, not the row's figures |
> | **G5b** | **BETTER** | **blanket**, same sentence; softer than G5a |
> | **G5c** | **PASS** | blanket, and **fair here** — two binary rows, nothing broke |
> | **G5d** | **GREEN** | **from the BYTES, not the boot** — a different kind |
>
> **WHY THIS DISTINCTION IS NOT PEDANTRY, AND IT IS G5a'S OWN DESIGN THAT MAKES IT MATTER.** That row
> exists because **SAME on `All` is a FAILING result** — and *a boot where nothing looks broken reads
> as "green" whether the muzzle improved or was unchanged.* **That is exactly how L0's "no need to
> change" got recorded and was never "no blob."** A file asserting *BETTER on All, BETTER on
> Decreased* would be asserting a measurement nobody took, and the next person retuning
> `samples_per_block` would start from it.
>
> **So: the direction is ruled. The per-setting magnitudes do not exist.**

### G-pb — **does the gap EXIST? figure.** Lapis Staff, flush against a wall

**Weapon: the LAPIS STAFF** (`/rpg give lapis_staff`), **both shots, on `All` AND `Decreased`.**

1. **Stand flush against a wall and fire.** Record: **beam, or no beam.**
2. **POSITIVE CONTROL, NOT OPTIONAL — SAME WEAPON, SAME SETTING:** step back to **roughly two
   blocks** and fire again. **A beam MUST appear.**

> **THE CONTROL AND THE SHOT MUST BE THE SAME WEAPON, WHICH THE FIRST DRAFT LEFT IMPLIED.** It said
> *"fire"* then *"fire again"*. Without that stated, the control does not close the confound below.

> **WHY THE LAPIS STAFF AND NOT THE CURSED EMERALD.** One beam, one line, nothing to disentangle.
> The emerald puts **six** beams on screen per cast, and *"no beam"* from six suppressed calls is
> not the same picture as from one. **The emerald's behaviour at point blank is worth its own
> observation rather than being mixed into this one.**

> **THE CONFOUND THIS ROW MUST NOT WALK INTO: A CORRECTLY SUPPRESSED BEAM AND THE SOLAR LANCE ARE
> OBSERVATIONALLY IDENTICAL.** `GATE-lapis-staff.md` **L11** records *"Cast Solar Lance → impact
> burst only. **No beam.**"* — PASS 2026-09-05. The impact visual still plays on a suppressed shot
> (see below), so both produce *burst at the hit point, no line*, for entirely unrelated reasons —
> and that weapon is one `/rpg give` away. **The same-weapon control is what separates them.**

> **THE SETTING IS RECORDED, AND ON THE SAME TWO THE OTHER ROWS USE**, so all three are comparable.
> This is the **nearest possible muzzle observation** — the exact class `GATE-lapis-staff.md:39`
> makes the setting a precondition for (*"L0 through L3 are meaningless without it"*).
> **THE FAILURE DIRECTION IS THE DANGEROUS ONE:** on `Minimal` a thin beam may not draw at all,
> producing *"no beam"* — **the same reading as a pass.** A row whose subject is an absence must
> name every condition that can manufacture that absence.

> **WHAT THE CONTROL ACTUALLY DRAWS, so a faint line is not ambiguous.** At gap 1.0 a two-block shot
> draws **one block of beam**: `round(1.0 × 4)` = **FOUR PARTICLES** at the authored
> `samples_per_block`. Near-field particles render large, so this should be unambiguous — **but if
> four reads as ambiguous in practice, the control wants a longer staging** (L6's five blocks gives
> sixteen), **and that is a finding about this row rather than about the gap.**

> #### RESULT — **PASS, ANSWERED BY BLANKET.** Neither the flush shot nor its control was recorded
> as a figure.
>
> **BUT THIS ROW IS NOT ISOLATED, WHICH IS WHY ITS BLANKET IS ACCEPTABLE WHERE G5a'S STILL OWES A
> CAVEAT.** G5a and G5b came back **better** — and **a gap that did not exist could not have improved
> the muzzle.** Existence is therefore **independently implied by the effect rows**, from a different
> direction than this row measures it.
>
> **Two rows agreeing from different directions is the opposite of a control succeeding for the wrong
> reason.** The Solar Lance confound this row was built to exclude is excluded by that too: a
> suppressed beam and L11's beamless cast look alike, but only one of them makes the muzzle better.

> **WHY A WALL AND NOT A MOB.** The gap is 1.0, so this row needs a hit point **under one block from
> the eye**. **Entity collision keeps you further away than a wall does, and the distance is not
> controllable** — a mob staging cannot reliably get inside the gap, and a row that cannot be staged
> is worse than no row. This repo has already lost one that way.
>
> **NO DISTANCE FIGURE IS WRITTEN INTO THIS ROW FROM ARITHMETIC.** *Flush against a wall* is the
> instruction; the observation is what it produces.
>
> **AND IF A FLUSH SHOT TURNS OUT TO BE OUTSIDE THE GAP IN PRACTICE, THAT IS A FINDING ABOUT THE
> CONSTANT** — the eye sits back from the wall further than the gap reaches — **and it belongs in
> this gate as a finding.** It is **not** a row to be quietly restaged at closer range until it
> agrees. Record what happened and let it rule the constant, which is what G5a/G5b are for anyway.

*The impact visual still plays at the hit point either way*, so feedback on a suppressed shot is
partial rather than absent. Whether "no beam" reads as *"you are touching it"* or as *"the weapon
did not fire"* is the question, and nobody has looked at it.

### G5a — **the LAPIS MUZZLE, before/after. figure. THIS IS THE REAL LAPIS WITNESS**

Fire at a flat wall from **~3 blocks**, on **`All` AND `Decreased`**, and judge against L0's recorded
reading — quoted here so the runner does not have to fetch it:

> **`GATE-lapis-staff.md` L0, 2026-09-05** — *"Fire while looking at a flat wall, from ~3 blocks. Is
> there a blue blob obscuring your view AT THE MUZZLE?"* · **sole witness** for the 0.25-block first
> sample · result: ***"a little clutter when casting on All; Decreased was much better. No need to
> change."*** → **ACCEPTED, MEASURED IMPERFECTION**

**Record BETTER / SAME / WORSE, per setting — two figures.**

> **SAME ON `All` IS A FAILING RESULT**, because `All` is where the clutter was.
>
> **L0 IS NOT RE-RUN AS WRITTEN.** Its own expectation is absence-shaped (*"expect: no"*) and it came
> back **not clean**, so re-running it as written would ask a yes/no question of a state already
> known to be neither. Run it **comparatively**, which is positive and discriminating.
>
> **THIS IS THE BETTER OF THE TWO EFFECT WITNESSES**, and it is worth knowing why: it has a
> **recorded pre-state on two named settings**. CE4 has a one-line impression.
>
> **IF G5a COMES BACK BETTER, THE L0/L2 BASELINE IS VOID.** That coupling's stated baseline is *"a
> measured imperfection rather than a clean muzzle"*, and a future `samples_per_block` retune would
> then be starting from somewhere new. Say so where the old baseline is stated.

> #### RESULT — **BETTER. ANSWERED BY BLANKET: THE OPERATOR'S RULING, NOT THIS ROW'S FIGURE.**
>
> *"It's much better."* **The per-setting BETTER/SAME/WORSE verdicts this row asks for were never
> recorded** — not on `All`, not on `Decreased`, and the gate page's database is empty.
>
> **THE ROW'S DISCRIMINATING HALF THEREFORE DID NOT RUN.** *SAME on `All` is a FAILING result* was
> the whole design, and a blanket "green" cannot distinguish BETTER from SAME. The ruling is a
> judgement that the muzzle improved; it is not the two-figure comparison written above.
>
> **THE L0/L2 BASELINE IS VOID — ON BLANKET EVIDENCE.** Recorded in `GATE-lapis-staff.md` where the
> old baseline is stated, and marked there as voided by a **ruling rather than a remeasurement**, so
> a future retune knows which it is starting from.

### G5b — **CE4's RE-RUN, at 3 and 30. figure**

Fire the Cursed Emerald at both distances, on **`All` AND `Decreased`**, both recorded.

> **THE PRE-STATE LACKS A SETTING, AND THAT WEAKENS THIS ROW ON PURPOSE.** The client Particles
> setting appears **nowhere** in `GATE-cursed-emerald.md`, while `GATE-lapis-staff.md:39` makes it a
> precondition (*"L0 through L3 are meaningless without it"*) and `PLAN-cursed-emerald.md` required
> it for that very file. **So CE4's *"slightly hard to see at both ranges"* is a reading with half
> its units missing**, and this re-run cannot fully compare against it.
>
> **Do not compare across an unknown setting and report a clean improvement.** A "better" verdict
> here is **softer evidence than L0's**, and the row says so rather than leaving a reader to weigh
> two figures that are not the same kind of measurement.

**G5b's completion includes the back-edit.** It does not count as run until
`GATE-cursed-emerald.md`'s **CE4 line** has been updated to point at the result. **Two files move in
one session or they drift** — `GATE-ignite.md` records two instances in three days.

> #### RESULT — **BETTER. ANSWERED BY BLANKET, under the same sentence as G5a.**
>
> **Four verdicts were called for — 3 and 30, each on `All` and `Decreased` — and none was taken.**
>
> **SOFTER THAN G5a EITHER WAY, for the reason this row was written to flag:** CE4's pre-state
> records **no Particles setting at all**, so even a full set of figures here could not have been
> compared cleanly against it. A blanket answer inherits that weakness and adds its own.
>
> **The back-edit is done**, which is what closes this row: `GATE-cursed-emerald.md`'s CE4 line now
> names this result and its evidence class, and is no longer RE-RUN OWED.

### G5c — **lapis L6/L7 regression only. LABELLED NOT A GAP WITNESS**

Re-run `GATE-lapis-staff.md` L6 (wall at 5 blocks) and L7 (mob at 10 blocks) and confirm they still
pass.

> **THEIR PASSING CARRIES NO INFORMATION ABOUT THE GAP, AND THE LABEL IS THE POINT OF THE ROW.**
> Both observe the beam's **END**; the gap moves its **START**. `GATE-cursed-emerald.md` derived that
> on paper before the fix was recommended, so re-checking them **confirms a prediction we already
> made** — a control that succeeds for the wrong reason if anyone reads it as evidence.
>
> Keep it as a cheap regression check. **Do not record it as the lapis witness. G5a is.**

> #### RESULT — **PASS. Answered by blanket, and THIS IS THE ONE ROW WHERE THAT IS A FAIR READING.**
>
> Two **binary regression** rows — the beam stops at the wall, the beam stops at the mob — and
> nothing broke. *"Boot gates all green"* covers a binary regression check in a way it cannot cover
> a comparative judgement.
>
> **RECORDED AS ONE ANSWER, NOT TWO.** L6 and L7 were not observed separately, and writing two
> verdicts would manufacture a distinction the evidence does not carry.
>
> **STILL NOT A GAP WITNESS.** Both observe the beam's END. Their passing confirms the paper
> prediction that a start-gap cannot move an end — which is worth having, and is not evidence about
> the gap.

### G5d — **the prose corrections landed AND are reasoned**

Confirm each of the four, and that each records **WHY** it was wrong rather than only that it was —
*a value-only correction cannot be audited later*:

| | |
|---|---|
| `GATE-cursed-emerald.md:242-244` | *"takes the aim origin directly"* — it takes the **segment start**; `from == aim.origin()` only at index 0 |
| `GATE-cursed-emerald.md` CE4 | a **pointer with no tick box and no result column** — a status line, not a row. **Both files state which is the authority**; G5b holds the row |
| `GATE-lapis-staff.md` ~139-155 | three sentences: the first sample *"one spacing off the eye … 0.25 blocks"*, *"a soft coloured blob 0.25 blocks from the eye"*, and *"the answer is probably a start offset … NOTHING HAS BEEN BUILT FOR IT"* — **this slice is that offset, and the file predicted it** |
| `BeamSamples` javadoc, lines 42-50 | calls the near-muzzle skip *"0.25 BLOCKS"* from the eye and *"a gate question, not a settled one"*. After this slice the first sample is one spacing past the **gap**, so the figure is wrong and the question is settled |

**A row that can only be ticked from a diff is still a row**, and it is here because falsified gate
prose is a thing this repo sweeps for and has lost to before.

> #### RESULT — **GREEN FROM THE BYTES, NOT FROM THE BOOT.** The only row here with hard evidence,
> and it is **a different kind** from the four above.
>
> All four corrections verified by reading them at `970aea8`, plus the sweep that was owed:
> `grep -c "the rest of this file"` returns **0**, so the completeness claim that could not be
> honoured is gone rather than repaired.
>
> **This row could never have been answered by a boot**, which is why its evidence is stronger than
> every other row's here and why the two must not be added together.

---

## ~~THE SLICE DOES NOT CLOSE UNTIL EVERY ROW RESOLVES~~ — **CLOSED 2026-09-10**

**What closed it:** the operator's ruling (*"it's much better"*) adopting the gap at **1.0**, a
blanket-green boot across G-pb/G5a/G5b/G5c, and G5d verified from the bytes.
`GATE-cursed-emerald.md`'s CE4 pointer has been updated and **is no longer RE-RUN OWED**.

> **CLOSED IS NOT THE SAME AS FULLY MEASURED, AND THIS FILE SAYS SO RATHER THAN LETTING A GREEN
> IMPLY IT.** Four of the five rows were answered by blanket. **The gap's DIRECTION is ruled; its
> MAGNITUDE has never been compared against an alternative** — nobody has looked at 0.5 or 1.5, and
> G5a's per-setting figures do not exist. A future retune argues with a ruling, not with a
> measurement.
>
> **These stay open and are NOT closed by this slice's green:** the Cursed Emerald's **40 mana**
> (MEASURED, NOT APPROVED, no deadline), the **tooltip ruling** (older and larger than volleys), and
> **Q3's two untested volley-pricing cases** — a `weapon_damage` payload and a swap to an enchanted
> weapon.
