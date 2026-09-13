# GATE — the Locust: the cadence at 12, and the first dark beam in the project

**Status: ROW 1 RUN TWICE, 2026-09-13 — `min 12` both times, the boundary half CLOSED. Its predicted
SHAPE failed twice, and that opened a finding the operator then ruled on. ROWS 2 AND 3 carry an
OPERATOR BLANKET PASS and remain OPEN, NEAR THIRD ONLY — see their own sections for why a blanket
cannot close them.**

**No number in this file changed as a result of any of it.** `color`, `size`, `samples_per_block` and
`BEAM_ORIGIN_GAP` are as authored, and `locust.yml` was not touched.

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

### THE READINGS — RUN TWICE, 2026-09-13. `min 12` BOTH TIMES. THE COMPARISON IS `>=`.

**Verbatim, `locust`, `right_click`. The prediction above is NOT edited.**

```
sample 1    INPUTS  count 43  window 167t  mean 3.98t   min 3t
            FIRES   count 12  window 160t  mean 14.55t  min 12t

sample 2    INPUTS  count 46  window 228t  mean 5.07t   min 3t
            FIRES   count 12  window 148t  mean 13.45t  min 12t
```

**THE BOUNDARY HALF IS CLOSED.** `min 12` twice. Under `>` the input at `t = 12` would have been
missed and the weapon would have fired at 16. `CooldownTracker.isReady`'s `>=` is now measured on a
**second shipping weapon**, neither of which is in the deletion set.

**THE PREDICTED SHAPE IS WRONG, AND IT IS KEPT.** `mean 12.00, zero variance` was predicted; `14.55`
and `13.45` were read. **Twice, so it is not a bad sample.** It is wrong in the way that produced the
finding below, which is the only reason a prediction is worth keeping after it fails.

#### THE INTERVALS, AS THE SIMPLEST FIT — WHICH IS NOT THE DECOMPOSITION

```
sample 1   11 intervals summing 160   ->   4 x 12  +  7 x 16
sample 2   11 intervals summing 148   ->   7 x 12  +  4 x 16      mirror images
```

> **THE FIT IS NOT UNIQUE AND MUST NOT BE STATED AS THE DECOMPOSITION.** Allowing a 20 — one input
> missed entirely — sample 1 admits **four** fits and sample 2 **three**: `5x12 + 5x16 + 1x20` sums to
> 160 just as well. **Enumerated, not eyeballed.** What the readings support is *some* mixture of 12s
> and 16s; which mixture is not observable from summary statistics.

#### THE CAUSE — RULED BY THE OPERATOR, FROM OUTSIDE THE SYSTEM

> *"The fluctuation in the numbers is due to sub optimal timings. That's just a thing holding right
> click does in minecraft."*

**Holding right-click does not produce a periodic input stream.** That is a property of the vanilla
client, and it accounts for all of it at once: `min 3`, the means of `3.98` and `5.07`, and the fact
that a 12-tick cooldown sometimes waits for the input after next.

> **THIS IS A SOURCE OF EVIDENCE THIS PROJECT HAS NOT USED BEFORE, AND IT IS LABELLED RATHER THAN
> BLENDED IN.** It is **not derived** from anything in the repo and **not measured** by this gate —
> it is the operator's knowledge of the client, ruled from outside the system. It sits beside
> measurements in this file and it is not one, and the distinction is the same one this project keeps
> for estimates printed next to measured numbers.

**AND IT RETRO-EXPLAINS A READING TAKEN BEFORE ANYONE WAS LOOKING**, which is the strongest support
it could have: Q7's very first sample, `hunters_bow`, read `INPUTS mean 5.02` — a non-periodic stream
recorded on day one, by a gate asking a different question, and never explained until now.

#### WHAT THE READINGS DO AND DO NOT ESTABLISH

**A plausible story — "a jittery input stream degrades the fire rate" — appears refuted, because the
two means moved in OPPOSITE directions:** sample 2's input mean was worse (`5.07` vs `3.98`) and its
fire mean was better (`13.45` vs `14.55`).

> **THAT REFUTATION IS CONFOUNDED, AND I AM RECORDING IT AS INCONCLUSIVE RATHER THAN AS A RESULT.**
> **The two input means are not measured over comparable windows.** Sample 1's input window is `167t`
> against a `160t` fire window — **4% of it outside the firing period**. Sample 2's is `228t` against
> `148t` — **35% outside**, i.e. a third of that mean describes inputs during and after the reload,
> when no fire could result. A mean diluted over a longer tail is not the same quantity.
>
> **The MINIMUM is the statistic that survives**, because a minimum cannot be diluted by extending the
> window. `min 3` in both samples is clean evidence; the mean comparison is not.
>
> The operator's ruling supplies the cause directly, so nothing turns on the refutation either way —
> but a confounded comparison should not be filed as a finding just because its conclusion is
> convenient. **A refuted explanation is a result; an inconclusive one is not.**

#### `min 3` IS THE GENUINELY NEW OBSERVATION, AND IT REFUTES A PROVENANCE SENTENCE

**Every prior input reading in the project read `min 4`. These two read `min 3`.**

| reading | count | window | mean | min |
|---|---|---|---|---|
| `hunters_bow`, `GATE-q7.md` | 56 | 276t | 5.02 | **4** |
| `quiver_stone`, `GATE-q7.md` | 30 | 116t | 4.00 | **4** |
| `boltor`, `GATE-boltor.md` row 1 | 31 | 120t | 4.00 | **4** |
| **`locust` sample 1** | 43 | 167t | 3.98 | **3** |
| **`locust` sample 2** | 46 | 228t | 5.07 | **3** |

**So a jittery MEAN was never new** — it is on record from Q7's first reading. What is new is that
**the floor is not 4.** `INPUT_FLOOR_TICKS`'s provenance sentence reads *"minimum 4 on both"*, and
that is now false of the project as a whole.

> **THE CONSTANT DOES NOT CHANGE, AND THE MODEL'S PREDICTIONS ARE UNAFFECTED.**
> `ceil(effective / 4) x 4` predicted `12` and the weapon delivered a **minimum** of `12`, twice. Every
> measured point still fits.
>
> **What weakens is the model's stated MECHANISM, not its predictions.** "Inputs arrive on a 4-tick
> grid" is refuted; "the delivered interval is the authored cooldown rounded up to the next multiple
> of 4" still holds at every point ever measured. **A model can keep being right after its explanation
> stops being.** The word *exactly* comes out wherever the floor is stated as a period; the arithmetic
> stays.

#### THE SHAPE FAILURE WAS ALREADY NAMED, ONE GATE OVER

**The prediction reached for the wrong one of two signatures that were both already written down.**
`GATE-boltor.md` row 1 states it: *"A mean between 16 and 20 is the `hunters_bow` signature and means
the input stream was not periodic — it is not a `>` reading, and the `min` is what settles it."*

```
quiver_stone signature   mean == min, zero variance      <- what row 1 predicted
hunters_bow  signature   mean > min, non-periodic input  <- what row 1 got, twice
```

**Scaled to this weapon, the readings are the `hunters_bow` signature exactly** — `19.43 / 16 = 1.214`
against `14.55 / 12 = 1.212`. **And the prescription written for it was already correct: read the
`min`.** The boundary was never in doubt; only the prediction of the shape was wrong.

#### WITHDRAWN — A FOURTH ROW RE-READING THE BOLTOR UNDER A NON-CLEAN STREAM

**Proposed in review and withdrawn before it was written.** The reasoning was sound: two of the
project's samples were non-clean, every published sustained figure assumes a clean stream, and one
more magazine on the Boltor would separate *a Locust property* from *a project-wide one*.

**The operator supplied the cause directly, so the row has no question left to ask.** It is
project-wide, it is the client, and a second Boltor magazine would measure a fact already ruled.

> **A ROW PROPOSED TO ISOLATE A CAUSE DIES WHEN THE CAUSE ARRIVES FROM ELSEWHERE.** Recorded as
> withdrawn with its reason rather than dropped — **the variance is still plainly visible in the two
> readings above**, and without this paragraph the next reader finds an unexplained wobble and no sign
> that anyone chased it.

#### THE PUBLISHED FIGURES ARE IDEAL-STREAM FIGURES, AND THEY STAND

> *"As for the numbers in the docs, leaving them as is is fine."*

**No re-pricing. `locust.yml` is untouched and every figure in `PLAN-locust.md` stands.** What changes
is that they are now *labelled*:

```
                    sh/s      DPS @ 26
  ideal stream     1.2500      32.50     <- what the docs publish
  sample 1         1.0909      28.36
  sample 2         1.1538      30.00
```

**Both readings landed under the published figure, in the same direction.** The published numbers
describe a perfectly periodic input stream, which the client does not produce.

> **THE ASSUMPTION WAS INVISIBLE UNTIL TONIGHT AND MUST NOT GO BACK TO BEING INVISIBLE JUST BECAUSE IT
> WAS ACCEPTED.** Ruling that the figures stand settles what they *are*; it does not make them
> unconditional. They are ideal-stream figures, that is now a known and accepted property, and the two
> samples that fall below them are recorded above.

### WHY TAKE IT AT ALL, GIVEN ROW 1 OF `GATE-boltor.md` ALREADY ANSWERED THIS

**Because the two weapons that carry the other two measured points are both in the deletion set.**
`quiver_stone` (11 -> 12) and `hunters_bow` (15 -> 16) go when the dev weapons go, and after that the
`>=` boundary rests on `boltor` **alone**. A second shipping weapon on an exact multiple of 4 is the
only thing that keeps the boundary falsifiable by more than one fixture.

> **AND IT IS NOT A SECOND `INPUT_FLOOR_TICKS` POINT. DO NOT LET IT BE CITED AS ONE.** `12 mod 4 = 0`
> is in the **BLIND** class for the grid-size question: `ceil(12/2) x 2 = 12` as well, so a 4-tick
> grid and a 2-tick grid agree here. `HeldFireQuantisationPinTest`'s dead-zone row remains the **sole**
> guard of that constant.

### THE PIN ROW IS NOW WRITTEN — THIS ROW UNBLOCKED IT

**`Reading("locust", 12, 12, "GATE-locust.md row 1", false)` is added to
`HeldFireQuantisationPinTest`.** The pin records the **measured minimum**, which is `12` in both
samples.

> **THE VARIANCE DOES NOT TOUCH THE PIN, AND THAT IS A PROPERTY OF WHAT THE PIN ASSERTS.** It pins
> `deliveredIntervalTicks(authored, 1.0) == measuredMin` — a minimum against a model of the floor.
> `mean 14.55` is a fact about the input stream; the pin asserts nothing about it. **A reading can be
> too noisy for one conclusion and exact for another**, and the gate calling the sample non-periodic
> does not make it inconclusive for the quantity the pin holds.

**The original condition below is left standing as written, because it is the reason the row waited.**

### THE CONDITION THAT MADE IT WAIT

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

## ROWS 2 AND 3 — AN OPERATOR BLANKET PASS. THE VERDICT FIELDS STAY EMPTY.

**What was actually got, in full:**

> *"The other gates passed too so let's continue."*

**That is a blanket, and it goes in evidence exactly as given.** It is recorded here rather than
written into the rows, because **turning it into per-number verdicts would be itemising from a
blanket** — `samples_per_block` holds, `size` holds, `BEAM_ORIGIN_GAP` holds, on `All` and again on
`Decreased`, six readings from one sentence that named none of them. **The pass condition would be
the absence of something noticed, which is not a pass condition.**

**Both rows stay OPEN and are marked `NEAR THIRD ONLY`.** Two specific things a blanket cannot carry,
and both were written into the rows in advance *precisely so a reading could disagree with them*:

- **ROW 2'S PREDICTION WAS BACKDROP-SPECIFIC.** It predicted the colour reads well against sky and
  pale terrain and **badly against grass, foliage and stone** — the backdrops a nature weapon lives
  on. A general *"passed"* does not say whether the predicted failure case was ever in the sample.
- **ROW 3'S NEAR THIRD DISCRIMINATES NOTHING.** Angular density is highest at the muzzle, so **a clean
  near third is the predicted appearance under both the good case and the bad one.** It is the one
  segment where passing and failing look identical — which is why the row demanded a second vantage
  before it was ever run.

**Unless there was a second vantage at distance and a named client `Particles` setting, neither row
has the observation it asks for.** Recorded as owed rather than closed.

> **THIS COSTS NOTHING TO LEAVE OPEN.** Nothing downstream is gated on either row, `locust_beam`'s
> colour is already marked **PROVISIONAL** in the file itself, and the density figures are already
> marked **INHERITED AND UNJUDGED**. The weapon ships either way. **An open row is cheap; a verdict
> nobody took is not.**

## AFTER THE READING

- **Do not edit the predictions.** Write each reading beside its prediction. That the predictions were
  recorded before the boot is the only thing that makes them evidence.
- **No row changes any number in the commit that records it.** A row is *written*; a conclusion is
  *taken*. If row 2 reads poorly the colour finding goes to the operator; if row 3 reads poorly the
  density finding does. Neither is fixed in the same commit that reads it.
- **Row 1 alone unblocks a code change** — the fourth `Reading` in
  `HeldFireQuantisationPinTest` — and that is additive, with its scope stated in its own javadoc.
