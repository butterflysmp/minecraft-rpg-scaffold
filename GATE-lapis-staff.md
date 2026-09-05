# GATE — the Lapis Staff

**This file is the source of truth for the Lapis Staff boot gate.** It is versioned with the code
because, for every behaviour listed below, **these rows are the only check that exists anywhere in
the project.** The suite passes with any of them deleted — 1294 tests, and not one of them can see
a colour, a spacing, or a delay.

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

Setting for this session: `________________`

If a second player is present for L4 anyway, have them read **L2** on a *different* setting. It
costs one more shot and turns a single-machine judgement into two points.

---

## The rows

| # | action | expect | marks | figure |
|---|---|---|---|---|
| **L0** | Fire while looking at a flat wall, from ~3 blocks. Is there a blue blob obscuring your view at the muzzle? | no | figure · **sole witness** for the 0.25-block first sample | |
| **L1** | What colour is the beam? | lapis blue | figure · the only check that the authored DustOptions reached the client | |
| **L2** | Fire a full-range shot across open ground. Density: too sparse / right / too busy? | *figure* | figure · **judge with L0, see the coupling below** | |
| **L3** | How thick is the beam? (authored size 1.2 against vanilla's 1.0) | *figure* | figure | |
| **L4** | **A SECOND PLAYER** stands perpendicular to a 26-block shot fired past them. Is the beam already whole when they first see it, or can they watch it grow? | whole | figure · **the only test of decision A's prediction** | |
| **L4c** | **L4's control.** Same shot fired **diagonally** (most chunk-plane crossings, so most ticks) as well as **down an axis** (fewest). Both figures. | *figure, both* | control | |
| **L5** | Caster-side: does the shot feel instant? | yes | figure | |
| **L6** | Fire at a wall 5 blocks away, on a 26-block range. | Beam **stops at the wall face**. Does not continue into or through the rock. | discriminating | |
| **L7** | Fire at a mob 10 blocks away, on a 26-block range. | Beam ends **at the mob**. Does not carry 16 more blocks past it. | discriminating · **sole witness** in game for the draw-to-hit-point rule | |
| **L8** | Fire at a stationary mob on flat ground. Does it slide? | **no** | witnesses the corrected decision C | |
| **L9** | Craft LAPIS_BLOCK / DIAMOND / DIAMOND vertically in a 3×3. Then attempt the same in the player's own 2×2 inventory grid. | 3×3 mints a Lapis Staff; 2×2 cannot hold three rows at all | binary | |
| **L10** | Fire 50 times, then inspect the item. Then stack two staves. | No durability bar. They merge. | binary | |
| **L11** | **Cast Solar Lance** (`/rpg` grant). | Impact burst only. **No beam.** | control · proves the new mechanism did not leak into the existing ray | |
| **L12** | Check the boot log for `lapis` warnings. | Silent. No unknown visual id, no beam-sound problem, no 2×2 recipe warning. | binary | |

---

## L0 AND L2 PULL IN OPPOSITE DIRECTIONS — the way C4 needs C5

Spacing is `1 / samples_per_block`, and the first sample sits **exactly one spacing off the eye**.
At the authored 4 per block that is 0.25 blocks, and **the figure does not depend on aim or on
segment length** (pinned by `BeamSamplesTest.theFirstSampleIsOneSpacingOffTheStartAndTheLastIsExactlyTheEnd`).

So:

> **If L2 reads "too sparse", the obvious fix — raising `samples_per_block` — moves the first
> particle CLOSER to the camera, and can red L0.**

Judge the two together, with both figures in hand. If they genuinely conflict, the answer is
probably a start offset rather than a density change — but that is a decision to take from two
readings, not in advance, and nothing has been built for it.

Both numbers live in `content/visuals/lapis_beam.yml`. A retune is a yml edit plus
`--refresh-content`, not a rebuild. That is why `samples_per_block` was made authorable rather
than left as a constant in Java.

---

## What these rows are witnessing that no test can

- **the colour** — `Color.fromRGB(40, 90, 240)` reaching a client as lapis blue
- **the size** — 1.2 against vanilla's 1.0
- **the density** — whether 4 per block reads as a line
- **the delay** — whether the chunk-column walk is as invisible as decision A predicts, which is a
  **prediction and not a measurement** until L4/L4c produce figures
- **the muzzle** — whether a soft coloured blob 0.25 blocks from the eye is in the way

`VisualLoaderTest.theShippedLapisBeamCarriesThePortedNumbers` proves the file on disk asks for the
right numbers. It cannot prove any of them looks right.
