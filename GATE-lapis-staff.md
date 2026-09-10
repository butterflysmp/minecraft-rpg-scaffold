# GATE — the Lapis Staff

**This file is the source of truth for the Lapis Staff boot gate.** It is versioned with the code
because, for every behaviour listed below, **these rows are the only check that exists anywhere in
the project.** The suite passes with any of them deleted — 1294 tests, and not one of them can see
a colour, a spacing, or a delay.

## Run 2026-09-05 — 12 of 14, and the two missing rows are BLOCKED INDEFINITELY, not owed

**Run:** L0, L1, L2, L3, L5, L6, L7, L8, L9, L10, L11, L12 — named, twelve.
**Unrun:** **L4 and L4c**, both for want of a second player. Not skipped as unimportant: they are
the **only** test of decision A's prediction, and that prediction is the reason the ray still walks
a chunk column per tick rather than being hitscan. **They are BLOCKED INDEFINITELY, not owed** -- there is no second account, and
the procedure below needs one. See the blocked note under the rows.

L0 and L5 read as accepted judgements rather than clean passes, and the operator's words are kept
verbatim in the rows. **L10's second half turned out never to have been a test**, and it surfaced a
wrong comment on `master` — see the row. The full account is in `NEXT.md`, under *The Lapis Staff*.

## How to use it

- **NAME THE ROWS YOU ARE ABOUT TO RUN, BEFORE YOU RUN THEM.** A count against an unnamed set is
  not an answer, however precise the number looks.
- A row marked **figure** wants a written observation, not a tick. **A figure row has no checkbox**
  — its text field is what marks it complete, and a blank field is UNRUN, not passed.
- A row marked **sole witness** names the behaviour it is the only check for. Skipping it is not
  reduced confidence; it is zero.
- A row marked **discriminating** fails if the specific defect the slice exists to prevent is
  present.
- A row marked **control** exists to stop another row crediting coverage it does not have.

## Rule 4 applies to every row here

*A gate row can be impossible, or real but non-discriminating, and both credit coverage that does
not exist.*

---

## BEFORE ANYTHING: record the client's Particles setting

**L0 through L3 are meaningless without it, and this is the first slice where that is true.**

Every visual in this repo is subject to the client's Particles video setting (All / Decreased /
Minimal), which alone would not be worth a note. **A beam is different in kind.** A burst degrades
gracefully — fewer flames, still recognisably a burst. **A line made of spacing does not**: thin it
and it stops being a line and becomes a dotted trail of unrelated specks. This is the repo's first
density-based visual, and density-based visuals degrade worse than count-based ones.

So a density judged "right" on All is not necessarily right for anyone else — and on an
accessibility-first project, the players most likely to be on Decreased or Minimal are the ones on
weaker hardware, not the ones who chose it.

> **A density figure with no setting attached is a measurement missing half its units.**

Setting for this session: **`All` AND `Decreased` — L0 through L3 were judged on both.**

That is better than the row asked for, and it is what makes L2's "right" mean something: the
density reads as a line on the setting a player with weaker hardware is most likely to be using,
not only on the one the runner happened to have. `Minimal` was not tested.

If a second player is present for L4 anyway, have them read **L2** on a *different* setting. It
costs one more shot and turns a single-machine judgement into two points.

---

## The rows

| # | action | expect | marks | figure |
|---|---|---|---|---|
| **L0** | Fire while looking at a flat wall, from ~3 blocks. Is there a blue blob obscuring your view at the muzzle? | no | figure · **sole witness** for the 0.25-block first sample | **NOT a clean "no". Operator, verbatim: *"a little clutter when casting on All; Decreased was much better. No need to change."*** ACCEPTED, MEASURED IMPERFECTION — see the coupling below, this is the baseline a density retune starts from |
| **L1** | What colour is the beam? | lapis blue | figure · the only check that the authored DustOptions reached the client | **"lapis blue".** The authored `Color.fromRGB(40, 90, 240)` reached the client |
| **L2** | Fire a full-range shot across open ground. Density: too sparse / right / too busy? | *figure* | figure · **judge with L0, see the coupling below** | **"right" — on BOTH All and Decreased.** No tuning. 4 per block stands |
| **L3** | How thick is the beam? (authored size 1.2 against vanilla's 1.0) | *figure* | figure | **"right".** Size 1.2 stands |
| **L4** | **A SECOND PLAYER** stands perpendicular to a 26-block shot fired past them. Is the beam already whole when they first see it, or can they watch it grow? | whole | figure · **the only test of decision A's prediction** | **BLOCKED INDEFINITELY (recorded 2026-09-08; unrun since 2026-09-05).** There is no second account, so this row has no runnable form -- see the blocked-indefinitely note below. **Decision A's justification remains a PREDICTION**, and will until an account exists |
| **L4c** | **L4's control.** Same shot fired **diagonally** (most chunk-plane crossings, so most ticks) as well as **down an axis** (fewest). Both figures. | *figure, both* | control | **BLOCKED INDEFINITELY**, with L4 -- and it is a SEPARATE verdict, easy to lose when L4 is counted alone |
| **L5** | Caster-side: does the shot feel instant? | yes | figure | **NOT "yes" — a softer pass than the row expected. Operator, verbatim: *"close enough, no need to change."*** Accepted; nothing to change, and nothing here speaks for the observer L4 tests |
| **L6** | Fire at a wall 5 blocks away, on a 26-block range. | Beam **stops at the wall face**. Does not continue into or through the rock. | discriminating | **PASS 2026-09-05** |
| **L7** | Fire at a mob 10 blocks away, on a 26-block range. | Beam ends **at the mob**. Does not carry 16 more blocks past it. | discriminating · **sole witness** in game for the draw-to-hit-point rule | **PASS 2026-09-05** |
| **L8** | Fire at a stationary mob on flat ground. Does it slide? | **no** | witnesses the corrected decision C | **PASS 2026-09-05** — no slide, so the corrected decision C holds in game |
| **L9** | Craft LAPIS_BLOCK / DIAMOND / DIAMOND vertically in a 3×3. Then attempt the same in the player's own 2×2 inventory grid. | 3×3 mints a Lapis Staff; 2×2 cannot hold three rows at all | binary | **PASS 2026-09-05** |
| **L10** | Fire 50 times, then inspect the item. ~~Then stack two staves.~~ | No durability bar. ~~They merge.~~ | binary | **PASS on the durability half 2026-09-05.** **THE MERGE HALF WAS NEVER A TEST** — `WeaponItems.mint` has capped every minted weapon at `setMaxStackSize(1)` since `347967b`, so two staves cannot merge and the expectation could not have been met by working code. Written from the material, exactly like the `flint_staff.yml` comment it exposed. **The action was performed and the staves did not merge** — the row failed as written and passed in substance. Replaced by **L10b**, which is the same action against the correct expectation. The count is unaffected: 12 of 14 turns on L4/L4c, not on this |
| **L10b** | **L10's replacement, and it is the SAME ACTION.** Mint two staves and try to stack them onto one slot. | They **refuse to merge** — two items, two slots | binary · **the first in-game witness of `347967b`**, which shipped saying "NOT witnessed in-game, in either direction" | **PASS 2026-09-05, on the observation that failed L10 as written.** The stack was attempted and the staves did not merge; that is what exposed the comment. The row text is corrected to the behaviour the code has, so the next runner is not sent to re-witness a merge that cannot happen |
| **L11** | **Cast Solar Lance** (`/rpg` grant). | Impact burst only. **No beam.** | control · proves the new mechanism did not leak into the existing ray | **PASS 2026-09-05** — the control held, the beam did not leak into the existing ray |
| **L12** | Check the boot log for `lapis` warnings. | Silent. No unknown visual id, no beam-sound problem, no 2×2 recipe warning. | binary | **PASS 2026-09-05** — silent |

---


## L4 AND L4c ARE BLOCKED INDEFINITELY, NOT OWED — there is no second account

**Corrected 2026-09-08.** An earlier version of this section proposed running these alongside scorch
slice 1's second-client rows "in one sitting". **That sitting cannot be scheduled: there is no second
account, and the procedure below needs one.** Two clients on one machine still means two accounts.

**"Owed" was the wrong record**, and the reason it matters is that owed reads as *work in progress* —
a row someone will get to. These will not be got to. They are **blocked on a resource that does not
exist**, and the honest entry says so, so nobody re-plans a sitting around them a third time.

**What is therefore NOT witnessed, stated plainly rather than left as a blank cell:** decision A's
prediction — *a beam drawn as the ray walks is indistinguishable from an instant one FOR AN OBSERVER
WHO IS NOT THE CASTER* — is the justification for the chunk-column traversal over hitscan, and it
**remains a prediction**. `L5` tested the caster and read "close enough". Nothing tests the observer,
and nothing will until an account exists.

> **Do not delete these rows.** A blocked row that states its blocker is a live record; a deleted one
> silently becomes an untested claim nobody remembers making. The same applies to
> `GATE-scorch-slice-1.md`'s `S7` and `S12`, blocked identically.

**If a second account ever exists**, these two and scorch's `S5`, `S7` and `S12` share the blocker and
should run together — **five verdicts, one setup**. The procedure for L4/L4c is below; S5's is in the
scorch gate. Until then this paragraph is a contingency, not a plan.

## THE PROCEDURE, FOR THE DAY A SECOND ACCOUNT EXISTS

**It needs a second ACCOUNT, which is the blocker above — not merely a second machine.**

**They are not skipped. Skipping a sole witness is not reduced confidence; it is zero** — and what
is at zero here is decision A's whole justification: *a beam drawn as the ray walks is
indistinguishable from an instant one FOR AN OBSERVER WHO IS NOT THE CASTER.* That claim is why the
chunk-column traversal was kept rather than making the ray hitscan. **L5 tests the caster and read
"close enough". Nothing tests the observer.** Until L4 runs, the reason for the design is a
prediction.

**The procedure, requiring one machine and no second person:**

1. Log a **second client on the same machine under a second account** onto the dev server.
2. Park that client **perpendicular to the shot line**, near its middle, watching across the beam.
3. From the first client fire a **26-block shot** past them: once **diagonally** (most chunk-plane
   crossings, so the most ticks — the worst case) and once **down an axis** (fewest — the best).
4. From the observing client, answer L4's question for each: **already whole, or can you watch it
   grow?** Record both figures; the diagonal is the one that decides it.

Two clients on one machine share a GPU and can stutter, which biases *toward* seeing growth. That
is the safe direction for this row: a "whole" verdict under that handicap is stronger, and a
"grows" verdict wants a re-run before it is believed.

---

## L0 AND L2 PULL IN OPPOSITE DIRECTIONS — the way C4 needs C5

Spacing is `1 / samples_per_block`, and the first sample sits **exactly one spacing off ~~the eye~~
THE DRAW START**. At the authored 4 per block that is 0.25 blocks ~~from the eye~~ **past the draw
start**, and **the figure does not depend on aim or on segment length** (pinned by
`BeamSamplesTest.theFirstSampleIsOneSpacingOffTheStartAndTheLastIsExactlyTheEnd`).

> **CORRECTED 2026-09-10 — THE DRAW START IS NO LONGER THE EYE.** `BEAM_ORIGIN_GAP` skips the first
> blocks of every ray weapon's beam, so the first particle now sits at **gap + 0.25** from the eye,
> not 0.25. **The 0.25-from-the-eye figure is gone**, and with it the premise this whole section was
> reasoning from.
>
> **WHY it was wrong rather than merely out of date:** the sentence was true of a beam drawn from
> `aim.origin()`, which is what `stepRay` did until the gap landed. It was **a correct reading of a
> mechanism that has since changed** — not a mistake when written — which is exactly the class this
> repo sweeps for, because a reader has no way to date a sentence from its own text.

So:

> **If L2 reads "too sparse", the obvious fix — raising `samples_per_block` — moves the first
> particle CLOSER to the camera, and can red L0.**

Judge the two together, with both figures in hand. If they genuinely conflict, the answer is
probably a start offset rather than a density change — but that is a decision to take from two
readings, not in advance, and ~~nothing has been built for it~~ **IT HAS NOW BEEN BUILT.**

> **THIS FILE PREDICTED THE FIX, AND THE FIX LANDED 2026-09-10.** `BEAM_ORIGIN_GAP` **is** that
> start offset. It arrived from the other direction — `GATE-cursed-emerald.md` CE4 measured the
> muzzle clutter on a six-beam weapon and traced it to a near-field cause — but it is the same
> remedy this section named in advance, and it now applies to **every** ray weapon including this
> one.
>
> **So the conflict this section was written to arbitrate may no longer exist:** raising
> `samples_per_block` still moves the first particle toward the camera, but the camera is now a gap
> away. `GATE-beam-gap.md` **G5a** is what measures whether that changed anything here.

Both numbers live in `content/visuals/lapis_beam.yml`. A retune is a yml edit plus
`--refresh-content`, not a rebuild. That is why `samples_per_block` was made authorable rather
than left as a constant in Java.

> **2026-09-05 — THE PAIR CAME BACK SPLIT, AND THE SPLIT IS THE BASELINE ANY RETUNE STARTS FROM.**
> L2 read **"right"** on both settings, so nothing needs raising today. L0 did **not** read a clean
> "no": *"a little clutter when casting on All; Decreased was much better. No need to change."* At
> the shipped density the muzzle is **already slightly cluttered on the highest particle setting**
> — accepted, not absent.
>
> > **AND THIS BASELINE IS NOW PROVISIONAL, BECAUSE THE GEOMETRY UNDER IT MOVED.** *"A measured
> > imperfection rather than a clean muzzle"* was measured on 2026-09-05 against a beam drawn **from
> > the eye**. `BEAM_ORIGIN_GAP` (2026-09-10) starts it a gap out.
> >
> > **`GATE-beam-gap.md` G5a re-reads exactly this**, comparatively, on both settings — and **if it
> > comes back BETTER, this baseline is VOID** and any future `samples_per_block` retune starts from
> > somewhere new. Do not carry the 2026-09-05 reading forward as the current state without checking
> > G5a's result first.
>
> So the coupling has bitten before anyone touched a number. A future *"it's too sparse, raise
> `samples_per_block`"* would move the first sample **closer than 0.25 blocks from an eye that is
> already carrying clutter**, and would be starting from a measured imperfection rather than from a
> clean muzzle. **Do not read "L0: no need to change" as "L0: no blob."**

---

## What these rows are witnessing that no test can

- **the colour** — `Color.fromRGB(40, 90, 240)` reaching a client as lapis blue
- **the size** — 1.2 against vanilla's 1.0
- **the density** — whether 4 per block reads as a line
- **the delay** — whether the chunk-column walk is as invisible as decision A predicts, which is a
  **prediction and not a measurement** until L4/L4c produce figures
- **the muzzle** — whether a soft coloured blob ~~0.25 blocks from the eye~~ **one gap plus 0.25
  blocks from the eye** is in the way *(corrected 2026-09-10 with the rest of this file's
  0.25-from-the-eye figures — `BEAM_ORIGIN_GAP` moved the draw start off the eye; see the L0/L2
  section above)*

`VisualLoaderTest.theShippedLapisBeamCarriesThePortedNumbers` proves the file on disk asks for the
right numbers. It cannot prove any of them looks right.
