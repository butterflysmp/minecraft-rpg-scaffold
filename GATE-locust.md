# GATE — the Locust: the cadence at 12, and the first dark beam in the project

**Status: NOT RUN. All three rows are predictions, written before any boot.**

**Every expected value in this file was recorded so that a later reading could disagree with it.**
That is the whole provenance of the document: a prediction revised after the fact proves nothing.
**When a row is read, its reading is written BESIDE its prediction and the prediction is NOT edited.**

**Three rows, one boot, and that is why this file exists rather than three flags in three files.**
The Locust ships with three unread questions — a cadence, a colour and a density. Left as loose notes
they get judged one at a time and forgotten two at a time. **Rows 2 and 3 are read in the same look,
by the same observer, at distance**, because they are the same photograph.

> **WHY NOT WIDEN `GATE-boltor.md` ROW 2 INSTEAD.** Two reasons, and the first is decisive: **the
> Locust's beam is a different file with a different colour and a 33% heavier sustained load, so a
> verdict on `boltor_beam` does not transfer to it.** Second, that row is about the Boltor and is
> still unread; hanging a second weapon on it makes one unread row into two unread questions wearing
> one heading. `GATE-boltor.md` row 2 gains a note that the density question now has a sibling — the
> sibling lives here.

---

## WHAT SHIPPED ALREADY, SO NO ROW RE-ASKS IT

`GATE-boltor.md` row 1 is **RUN and READ**: a cooldown of exactly 16 fired at 16, so
`CooldownTracker.isReady`'s `>=` is measured rather than assumed, and the model
`fire interval = ceil(effective_cooldown / 4) x 4` has no unmeasured residue class left. **Row 1
below is therefore a CONFIRMATION on a second weapon, not an open question** — and it is worth taking
anyway, for a reason stated in that row.

---

## ROW 1 — THE CADENCE AT COOLDOWN 12

| | |
|---|---|
| **weapon** | `locust` (`material: crossbow`, `cooldown_ticks: 12`, `quiver_size: 12`, `reload_ticks: 60`) |
| **do** | `/rpg firerate` **once to clear**, then hold right-click for one full magazine (12 shots), then `/rpg firerate` to read |
| **read** | the `FIRES` line: `count`, `window`, `mean`, `min` |
| **verdict** | `min 12` confirms the model on a second shipping weapon. `min 16` would mean the boundary is `>` after all, contradicting `GATE-boltor.md` row 1 |

**THE CLEAR IS NOT OPTIONAL AND IT IS THE ROW'S ONLY CONTROL.** `/rpg firerate` **prints and clears**,
so a run not preceded by a clear reports a sample mixed with whatever fired before it — including the
fires from walking to the test spot. The first invocation is the control; its output is discarded.

**Read `min`, not `mean`.** The mean is a fact about the *input stream* and drifts when inputs are not
perfectly periodic — `hunters_bow` read `mean 19.43` against a true interval of 16.

### THE EXPECTED SHAPE, WRITTEN IN ADVANCE

```
FIRES  count 12  mean 12.00  min 12      <- zero variance, the quiver_stone signature
```

`ceil(12/4) x 4 = 12`, and 12 is an exact multiple of 4, so under `>=` the input at `t = 12` finds the
cooldown expired and fires. Under `>` it would be missed and the next input at 16 would fire it.

### WHY TAKE IT AT ALL, GIVEN ROW 1 OF `GATE-boltor.md` ALREADY ANSWERED THIS

**Because the two weapons that carry the other two measured points are both in the deletion set.**
`quiver_stone` (11 -> 12) and `hunters_bow` (15 -> 16) go when the dev weapons go, and after that the
`>=` boundary rests on `boltor` **alone**. A second shipping weapon on an exact multiple of 4 is the
only thing that keeps the boundary falsifiable by more than one fixture.

> **AND IT IS NOT A SECOND `INPUT_FLOOR_TICKS` POINT. DO NOT LET IT BE CITED AS ONE.** `12 mod 4 = 0`
> is in the **BLIND** class for the grid-size question: `ceil(12/2) x 2 = 12` as well, so a 4-tick
> grid and a 2-tick grid agree here. `HeldFireQuantisationPinTest`'s dead-zone row remains the **sole**
> guard of that constant.

### THE PIN ROW IS NOT WRITTEN UNTIL THIS ROW IS READ

`HeldFireQuantisationPinTest`'s `READINGS` are **boot measurements** — its constant is documented
*"MEASURED, NOT DERIVED"* and every row cites a gate readout. **A `12 -> 12` row added before this
boot would be a predicted value in a measured table.** When this row is read, add
`Reading("locust", 12, <measured min>, "GATE-locust.md row 1", false)` and say in its javadoc that it
adds a `>=` point and **not** an `INPUT_FLOOR_TICKS` point.

---

## ROW 2 — THE BEAM COLOUR, WHICH IS PROVISIONAL BY RULING

| | |
|---|---|
| **visual** | `locust_beam`, `color: [45, 120, 55]`, forest green |
| **do** | fire down a clear 96-block sightline, in **daylight over grass**, then again **at night**, then again **against stone or a dark backdrop** |
| **read** | is the line legible as a LINE against each backdrop, and is it distinguishable at a glance from `emerald_beam`'s bright green |
| **record** | the client **Particles** setting, and a verdict per backdrop — not one overall verdict |

**THE VALUE IS RULED AND PROVISIONAL, AND THIS ROW IS WHAT DISCHARGES IT.** `[45, 120, 55]` was
proposed and ruled with its condition attached: authored so the weapon ships with a colour, judged on
a client rather than in a file.

> **COLOUR IS THE ONE PROPERTY NO TEST IN THIS PROJECT CAN CHECK.** A wrong damage number reddens a
> test; a wrong cooldown reddens a pin; `golden-lore.txt` pins every tooltip character. **Nothing
> anywhere in the suite renders a particle.** The only instrument is a person looking at it.

### THE PREDICTION, AND IT IS A WORRY RATHER THAN A NUMBER

**Every other beam in the project is high-luminance** — `boltor_beam` `[200, 215, 235]` pale steel,
`emerald_beam` `[40, 220, 90]` bright green, `lapis_beam` `[40, 90, 240]` saturated blue. **This is
the first dark beam.**

**So the predicted failure is backdrop-dependent and asymmetric:** it reads well against sky and pale
terrain and **poorly against grass, foliage and stone** — which is where a *nature*-themed weapon will
most often be fired. **If it fails, it fails in the situation the colour was chosen for.** Recorded in
advance so the reading has something to disagree with.

**A brightened forest green is the obvious remedy and it is NOT pre-authorised** — raising luminance
walks it back toward `emerald_beam`, which is the thing it had to separate from. If this row reads
poorly, the finding goes to the operator with both constraints stated, not a fix.

---

## ROW 3 — THE DENSITY AT 633 POINTS PER SECOND, THE HEAVIEST LOAD IN THE PROJECT

| | |
|---|---|
| **visual** | `locust_beam` at `range: 96`, `samples_per_block: 4`, `size: 1.2` — **all inherited from `boltor_beam` and all unjudged** |
| **do** | fire single shots down 96+ blocks, then a **held burst** at the full 12-round magazine so the sustained rate is real |
| **read** | is it a LINE or a trail of unrelated specks — **at the muzzle AND at the far end** |
| **record** | the client **Particles** setting, and a verdict on each of the three numbers **separately** |

### THE FIGURES, DERIVED AND PRE-RECORDED

```
  per shot     (96 - 1.0) x 4 = ~380 points          IDENTICAL to the Boltor -- same length, same density

  sustained    boltor   380 x (20/16) = 475 points/s
               locust   380 x (20/12) = 633 points/s      +33.3%, exactly the cooldown ratio 16/12

  in flight    boltor   gap 16t vs a 5-10t walk  ->  margin 6t
               locust   gap 12t vs a 5-10t walk  ->  margin 2t
```

**The per-shot load is unchanged; the sustained load is a third higher.** `GATE-boltor.md` row 2 was
written calling the Boltor *"the largest particle load any weapon in this project has asked for"* —
**that sentence is now about this weapon instead**, and row 2 gains a note saying so.

### ONE OBSERVER CANNOT TAKE ROWS 2 OR 3

**The 32-block client particle cap means the shooter renders roughly the first third of this beam and
a target at the far end renders roughly the last third. Nobody renders the middle third at all.**
Judged from the muzzle alone, both rows measure a third of what is drawn — **and the densest third, at
that**, which biases toward a verdict of "fine".

**Try first, it costs one command:** fire at the sightline, then immediately `/tp` to the far end and
look back. **Predicted to fail** — particle *lifetime* is not the variable, *reception* is, and the
draw window is 5-6 ticks axis-aligned, 9-11 at 45°. A keybound macro or a repeating command block is
the variant worth trying next. **A single-observer reading is written `NEAR THIRD ONLY` and the row
stays open.**

### JUDGE THE THREE NUMBERS SEPARATELY, BECAUSE THEY FAIL DIFFERENTLY

- **`samples_per_block: 4`** — spacing. Too low and the line becomes specks.
- **`size: 1.2`** — this is `lapis_beam`'s single-beam figure, not `emerald_beam`'s `1.0`. Emerald went
  slimmer because six of its beams overlap within one cast; **that is a different quantity and does
  not obviously call for the same remedy here.**
- **`BEAM_ORIGIN_GAP: 1.0`** — a **`core` constant shared by every beam**, adopted on a
  blanket-answered ruling and never compared against `0.5` or `1.5`. **If it reads wrong, that is a
  finding, not an edit** — changing it moves every beam in the game.

> ### DO NOT READ `emerald 1.0`, `boltor 1.2` AND THIS BEAM AS A SERIES
>
> **The three `size` values are not points on a gradient, and a row told to look for one does not come
> back empty — it comes back WRONG.**
>
> **THE QUANTITIES ARE INCOMMENSURABLE.** `emerald_beam` went slimmer because **six beams overlap
> within one cast** — that is **SPATIAL** overlap, six lines occupying neighbouring space
> simultaneously. What this beam has is **TEMPORAL**: one line, redrawn every 12 ticks, with the
> previous shot's particles still in the air. **Thinning is a plausible remedy for the first and has
> no obvious bearing on the second**, because nothing here is competing for space with anything.
>
> **A READER LOOKING FOR A GRADIENT CAN ALWAYS FIT ONE TO THREE POINTS.** `1.0` at six-overlapping,
> `1.2` at two, and a denser weapon after that arranges itself into a trend on sight, and the trend
> would recommend a number. **Three numbers always admit a fit; that is a fact about three numbers,
> not about beams.**
>
> **RECORDED AS WITHDRAWN AFTER BEING RAISED, RATHER THAN NEVER MADE.** This was proposed as a finding
> during review of `PLAN-locust.md` — that `1.2` sits at the light end of a gradient — and retracted
> by its author on noticing the spatial/temporal split. **A retracted finding is worth more in the
> record than one never written down**, because the pattern is genuinely visible and the next person
> to notice it needs to find the reason it was dropped rather than rediscovering it as new.
>
> **So judge `size` against what this beam does at 96 blocks and 633 points/s, and against nothing
> else.**

**Judge on `All`, then again on `Decreased`, and record both.** A density-based visual degrades worse
than a count-based one, and this is the longest line in the game, so it has the most length over which
to come apart.

---

## AFTER THE READING

- **Do not edit the predictions.** Write each reading beside its prediction. That the predictions were
  recorded before the boot is the only thing that makes them evidence.
- **No row changes any number in the commit that records it.** A row is *written*; a conclusion is
  *taken*. If row 2 reads poorly the colour finding goes to the operator; if row 3 reads poorly the
  density finding does. Neither is fixed in the same commit that reads it.
- **Row 1 alone unblocks a code change** — the fourth `Reading` in
  `HeldFireQuantisationPinTest` — and that is additive, with its scope stated in its own javadoc.
