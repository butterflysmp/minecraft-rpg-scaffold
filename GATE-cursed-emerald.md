# GATE — Cursed Emerald

**This file is the source of truth for the Cursed Emerald boot gate's CONTENT.** It is versioned
with the code because for several behaviours below **these rows are the only check that exists
anywhere in the project.** The suite passes with any of them deleted — **1472 tests** (853 core /
17 storage / 602 paper, `./mvnw test` at the slice's final verify), and not one of them can see a
single pixel.

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

> ### AND THIS IS WHY THE GATE IS ITS OWN DOCUMENT RATHER THAN A SECTION OF THE PLAN
>
> **A plan is edited to record DECISIONS. A gate is edited to record RUNS.** Put both jobs in
> `PLAN-cursed-emerald.md` and every future edit is ambiguous about which one it is doing — the
> drift shape, arriving before any drift.
>
> There is also a lifecycle argument: **plans go stale by design once their slice ships.** A gate
> living inside one inherits that staleness **with no signal**. `GATE-volley.md` will outlive every
> `PLAN-*` around it, and so will this.

---

## WHAT `GATE-volley.md` ALREADY COVERS, SO THIS GATE IS NOT ASKED TO RE-CHECK IT

**Read this before any row below, or a green emerald gate gets read as a second independent
confirmation of the volley slice — and the wrapper gets credited with coverage it received once.**

**THE MECHANISM IS ALREADY WITNESSED.** V1–V9 ran green on **2026-09-10** against `volley_stone`, a
fixture built to be **deliberately worse than anything content would author** — eight shots one tick
apart at 64 blocks, against this weapon's six at two ticks at 32. That run established, on the
mechanism:

| | witnessed by |
|---|---|
| the wind-up is a real, audible delay and reads the authored field | V1, two triggers at different lengths |
| each shot **re-aims** from the live eye | V4 |
| each shot **rolls its own crit** | V4, mixed colours in one burst |
| the derived cooldown floor refuses a re-press **and moves with the numbers** | V5, including the `shots: 8 → 4` half |
| the whitelist's **projectile** member genuinely works | V8 |
| a caster leaving mid-burst stops the volley cleanly | V9 |

**SO MOST CE ROWS BELOW WITNESS CONTENT, NOT MECHANISM.** They check that `cursed_emerald.yml`
**WIRES** the wrapper correctly — that this weapon authors a volley, at these numbers, naming these
visuals. They do not re-establish that the wrapper works, and a pass here is not evidence that it
does.

> **CE6 IS THE CLEAREST CASE AND IS LABELLED ACCORDINGLY.** *"Track a moving target through the
> burst"* is **V4's observation on a different weapon.** V4 already established re-aim and the
> per-shot crit roll **on the mechanism**. CE6 adds only that the emerald authors a volley rather
> than something else. **Keep it, but it is not a sole witness**, and it must not be recorded as
> one.

**What is genuinely new here, and therefore what this gate is actually for:**

1. **The emerald's own numbers on screen** — 27 and 54, six of them, at *this* cadence (CE2).
2. **The range ruling in practice** — 32 blocks, and what a miss looks like at the render cap (CE8).
3. **The mana economy**, which nothing has ever measured in this repo (CE5).
4. **The caster's own beam**, at a colour and density nothing else ships (CE4).

---

## PRECONDITIONS, WHICH EVERY FIGURE BELOW SILENTLY ASSUMES

`EffectApplier:136-138` passes the authored amount through `enchantDamagePercent`,
`classDamageBonus` and `chargeScale` **before** crit. So **27** and **54** hold only for an
**unenchanted emerald, on a player carrying no class damage bonus and no charge scaling.** Run it
clean, or every number here is off by a multiplier and reads exactly like a broken volley.

---

## ROWS

Written before the boot, not after, and none of them is a tick-box.

> ### RUN 2026-09-10, against `master` at `f1d6aba`. SIX PASS, ONE FORCED, ONE FINDING.
>
> **Itemised per row rather than blanket-ticked**, because these were reported individually and a
> summary would flatten three results that are not plain passes.
>
> | row | verdict | |
> |---|---|---|
> | **CE1** | **PASS** | chime, a full second, then six shots |
> | **CE2** | **PASS** | six numbers, countable, crits legible by colour |
> | ~~CE3~~ | — | deleted before the run; `GATE-volley.md` V2 had answered the harder version |
> | **CE4** | **FINDING — RESOLVED ELSEWHERE, no tick box** | *"slightly hard to see at BOTH ranges."* Fixed by `BEAM_ORIGIN_GAP`; **re-run recorded as `GATE-beam-gap.md` G5b — BETTER, answered by blanket.** Not a row that can be ticked here; G5b is the authority. See below. |
> | **CE5** | **PASS — RUN, MATCHED PREDICTION** | and **40 mana was MEASURED, NOT APPROVED** |
> | **CE6** | **PASS** | confirmation, not discovery — V4 is the mechanism's witness |
> | **CE7** | **UNCHANGED, AS FORCED** | real, and **not evidence about volley pricing** |
> | **CE8** | **PASS — RUN, MATCHED PREDICTION** | |
>
> **CE5 and CE8 carry no written figures, by operator decision:** both matched the arithmetic this
> file predicted in advance, and **transcribing a prediction is not an observation.** Recorded as
> *run, matched prediction* rather than given invented numbers — the alternative is a figure that
> looks measured and was copied.

### CE1 — the wind-up is visible as a wind-up

Press right-click. Expect **a chime, then a full second of nothing, then six shots.**

**An instant first shot means `windup_ticks` was dropped**, and the telegraph is the entire reason a
one-second commitment is fair rather than a dead second.

`emerald_windup.yml` is this row's only witness — if that sound is ever removed, CE1 stops being
able to tell a dropped wind-up from a working one.

*Mechanism-wise this is V1's territory; what is new is only that THIS weapon's `on_cast` is wired
and its sound key resolves on the pinned jar.*

> #### RESULT — **PASS.** Chime, a full second of nothing, then the volley.

### CE2 — **six shots land, and the crits are countable**

`/rpg spawn knell`, hold still, one cast. **COUNT THE DAMAGE NUMBERS, AND COUNT THE YELLOW ONES.**

Expect **six numbers**, each `27` (white) or `54` (yellow).

```
knell final HP = 360 − 27 × (6 + yellows)
```

**PER-SHOT ROLLS MEAN 162 IS THE NO-CRIT CASE, NOT THE EXPECTATION.** Every shot carries an
independent 15% roll at ×2 (`Crit.BASE_CHANCE` 0.15, `Crit.multiplier(0.15, 1.0, 0.10) -> 2.0`), so
a cast deals `27 × (6 + crits)`:

```
  162 · 189 · 216 · 243 · 270 · 297 · 324        (6 shots, 0..6 crits)
```

**Six shots at 15% averages 0.9 crits, so ONE YELLOW IS THE MODAL OUTCOME** and ~189 is as ordinary
a result as 162. Neither is a defect.

**FEWER THAN SIX NUMBERS IS THE DEFECT. Everything else is arithmetic.** A single expected HP figure
cannot separate a missing shot from a crit; **the count can, and the colour explains any figure the
count does not.**

> *Why the plan's first version of this row was wrong, kept because the failure is instructive:* it
> read **"expect 198"**, right only 38% of the time, and named `27, 54, 135` as its failure
> vocabulary. **198's near neighbours are clean passes** — 171 is six shots with one crit and would
> have been filed as shots being eaten — and **135 is not reachable at all** (`225 / 27` is not an
> integer). The row was wrong in both directions at once. *A gate row that names an exact figure
> against a 15% coin flip that cannot be disabled is not a gate row.*

**WHY THIS ROW IS READABLE AT ALL:** `DamageNumberText.of` renders a crit **yellow** and a normal hit
**white**, and kinetic's `damage_symbol` is `""`, so the numbers are **bare** — colour is the only
signal, with no glyph competing for the eye. **The element choice is what makes this row work.** Had
it been fire, six gold `▲` marks would sit beside six numbers whose only distinction is also a warm
colour.

**AND THIS ROW ABSORBED CE3'S SURVIVING HALF** (see below). While counting, also say whether **27
and 54 read correctly at this cadence** — six figures, two ticks apart, at nearly one point. That is
a content-tuning observation, not a legibility one, and if it comes back badly the first thing to
thin is `emerald_impact.yml`, not the volley's timing.

> #### RESULT — **PASS.** Six numbers, countable, and the crits legible by colour.
>
> The absorbed half came back clean too: **27 and 54 read correctly at this cadence**, so
> `emerald_impact.yml` needs no thinning. **Note that this is a separate question from CE4** — the
> impact bursts are not what is hard to see; the beam's near field is.

### ~~CE3 — are six numbers two ticks apart legible?~~ **DELETED, AND THE REASON IS RECORDED**

**`GATE-volley.md` V2 ANSWERED THE HARDER VERSION OF THIS QUESTION.** It counted **eight numbers one
tick apart** and confirmed them countable. **Six at two ticks is strictly easier** — fewer, slower,
and no more crowded.

V3's own note stated the inference in advance: *"if eight at one tick are legible, six at two are
safe."*

**A row that is already answered and run anyway produces a pass that credits nothing**, and this
project has a standing rule about coverage that does not exist. So the row is deleted rather than
demoted, and **the number CE3 is not reused** — `PLAN-cursed-emerald.md` and this file both cite CE
numbers, and silently recycling one would make an old reference point at a new question.

**The honest remaining question was never legibility** — it was whether 27 and 54 read correctly at
*this* weapon's cadence, which is content tuning. **Folded into CE2's observation**, where it is
taken on the same staging with the same cast.

### CE4 — **does your own beam blind you?**

Fire at a wall from **3 blocks** and from **30**.

`cfde822` solved this twice over — `BeamVisuals.handLocation` (draw from the hand, not the eye) and
`VISUAL_GAP = 1.0` (skip the first block) — and **neither fix came across**. This repo draws from
`aim.origin()` with no gap. Recorded as a known divergence, not ported, and this row decides whether
it needs a remedy at all.

*If the answer is "no", the two fixes were solving a problem this repo's geometry does not have, and
that is worth writing down as much as a yes is.*

> **THE DISTANCES MOVED WITH THE RANGE RULING.** The plan read *"from 3 blocks and from 40"*, and
> **40 is now past the weapon's whole reach** — it would have measured nothing. The near figure is
> what this row actually tests, since a beam filling your view is a near-muzzle problem; the far one
> is 30, just inside both the range and the 32-block particle cap, so the beam is drawn along its
> entire length.

**Six beams per cast at `size: 1.0`** is more than anything else in the repo puts on screen at once —
the Lapis Staff draws one at 1.2. ~~If the answer is "yes, blinding", `emerald_beam.yml`'s size or
`samples_per_block` is the lever, and it is a content fix.~~ **STRUCK — the row was run and that
sentence is wrong in both halves. It is not a content fix, and the density is not the lever.**

> ## CE4 — **RESULT: FINDING. ~~RE-RUN OWED, NOT GREEN.~~ RESOLVED 2026-09-10 — see the foot of this section.**
>
> **Observed 2026-09-10:** *"slightly hard to see at **both** ranges"* — 3 blocks and 30.
>
> ### THE FIRST DIAGNOSIS WAS BACKWARDS, AND THE CORRECTION IS THE USEFUL PART
>
> The initial reading was that *"both ranges"* **ruled out** a near-muzzle cause, since a one-block
> gap cannot help a beam that obstructs along its whole length. **That inference is inverted.**
>
> The beam starts at the **eye** and runs down the **view axis**. Particles near the eye project
> **large and dead-centre**; particles thirty blocks out project **tiny and dead-centre**. **Screen
> coverage is dominated by the nearest few metres — and those are identical at both stagings.**
>
> | hypothesis | 3 blocks | 30 blocks | predicts |
> |---|---|---|---|
> | cumulative density | 12 ppb | 120 ppb | **30 dramatically worse** |
> | near-field obstruction | same first metre | same first metre | **the same at both** ← *observed* |
>
> **TEN TIMES THE TOTAL PARTICLE COUNT PRODUCING NO CHANGE IN DIFFICULTY IS EVIDENCE AGAINST
> DENSITY AND FOR A FIXED NEAR-FIELD CAUSE.**
>
> > **THE SHAPE, WORTH MORE THAN THIS ROW: AN INVARIANT OBSERVATION ACROSS A CHANGED QUANTITY RULES
> > OUT THAT QUANTITY AS THE CAUSE — IT DOES NOT IMPLICATE WHATEVER ELSE WAS IN THE FRAME.**
> > Invariance was read as pointing at the far segment. Invariance is exactly what the *near-field*
> > hypothesis predicts. The observation was doing real work and was read backwards; a null result
> > eliminates, it does not nominate.
>
> ### THIS IS MECHANISM. IT DOES NOT COME BACK TO CONTENT.
>
> **MEASURED, not assumed:** no gap or offset concept exists anywhere in the tree — no
> `VISUAL_GAP`, no `startOffset`, no `skipFirst`. ~~and `CombatWorld.presentAlong(from, to,
> visualId)` takes the aim origin directly (`CastExecutor:321`).~~ **`emerald_beam.yml` cannot
> express either fix.** Do not attempt this from a content slice.
>
> > **STRUCK 2026-09-10 — `presentAlong` DOES NOT TAKE THE AIM ORIGIN. IT TAKES THE SEGMENT START.**
> > `launchRay` passes `aim.origin()` as `from` for index 0 and **`aim` dies there**; `stepRay:335`
> > passes the previous segment's `to` as the next `from`, so every later segment starts **on a
> > chunk plane**. `aim.origin()` is not threaded through `stepRay` at all.
> >
> > **WHY IT WAS WRONG, WHICH IS THE PART A VALUE-ONLY CORRECTION WOULD LOSE:** the claim was
> > checked at **one call site on its first iteration**, where it is true, and generalised to the
> > loop. *True of the sample, generalised to the population* — the same shape as CE4's own
> > misreading one turn earlier.
> >
> > The consequence is not cosmetic: it is why the fix had to **thread a gap boundary computed once
> > from the aim**, rather than deriving one per segment. See `PLAN-beam-gap.md`.
> >
> > **The rest of the sentence stands** — the no-gap-concept half was separately verified.
>
> **Two candidates, both the operator's:**
>
> | | fix | |
> |---|---|---|
> | **1** | **SKIP THE FIRST N BLOCKS** (`cfde822`'s `VISUAL_GAP = 1.0`) | **RECOMMENDED.** It fixes **the cause** rather than the multiplier. Six beams pushed this over the threshold, but **the obstruction is there for every ray weapon** — the Lapis Staff draws from the eye too, at `size: 1.2`, just once. A gap fixes all of them. It also **keeps the beam honest**: the drawn line still lies along the traced ray, starting a metre out. |
> | **2** | **DRAW FROM THE HAND** (`cfde822`'s `BeamVisuals.handLocation`) | **The cost, named before choosing:** the drawn line would no longer **be** the traced line — the beam becomes a cosmetic lie about the ray's path. Normal for a gun, but it collides with something concrete: **`GATE-volley.md` V4 and this file's CE6 both read the beam's ORIGIN as evidence of re-aim** (*"does the beam originate from where you are now or where you were?"*). Move the origin to the hand and **those rows stop witnessing what they were written to witness.** |
>
> **Thinning `emerald_beam.yml` reduces only the multiplier and leaves the cause for the next
> multi-beam weapon to rediscover.** That is why the struck sentence above was wrong.
>
> ### WHAT MUST BE TRUE OF ANY FIX
>
> - **CE4 IS RE-RUN AFTERWARDS, at both 3 and 30.** The current observation **expires the moment the
>   geometry changes.** This row is **RE-RUN OWED**, not green, and must not be ticked by the fix
>   landing. — **CONSTRAINT MET 2026-09-10:** the re-run happened as G5b and the row was **not**
>   ticked by the fix landing. It was closed by a recorded result, which is the distinction this
>   line was written to protect.
>
>   > ### THIS IS A POINTER, NOT A ROW. THE AUTHORITY IS `GATE-beam-gap.md` **G5b**.
>   >
>   > **CE4 has no tick box and no result column from here on.** It is a status line. The re-run
>   > itself — at 3 and 30, on **`All` and `Decreased`**, carrying the caveat that this file records
>   > **no Particles setting** for the original observation — lives in **G5b** and is recorded there.
>   >
>   > **A ROW THAT CAN BE TICKED IN TWO PLACES WILL BE**, and then two files carry results and
>   > neither knows the other moved. One authority, one forwarding note.
>   >
>   > **Updating this line is part of G5b's own completion**, not a later housekeeping task: G5b does
>   > not count as run until this pointer names its result. **Two files move in one session or they
>   > drift** — `GATE-ignite.md` records two instances of exactly that in three days.
>   >
>   > ---
>   >
>   > ### RESOLVED 2026-09-10. **CE4 IS NO LONGER RE-RUN OWED.**
>   >
>   > **G5b's result: BETTER — and ANSWERED BY BLANKET.** The operator ruled *"it's much better"* and
>   > adopted `BEAM_ORIGIN_GAP` at 1.0. **The four figures G5b asked for — 3 and 30, each on `All`
>   > and `Decreased` — were never taken**, and the gate page's database is empty.
>   >
>   > **SOFTER THAN THE LAPIS WITNESS, for the reason this row flagged in advance:** CE4's own
>   > pre-state records **no Particles setting anywhere in this file**, so a re-run could not have
>   > been compared cleanly against it even with full figures. That caveat is why G5a and not G5b is
>   > the better evidence that the gap helped.
>   >
>   > **This line is still a status, not a tick.** The result lives in G5b.
> - **THE LAPIS STAFF IS RE-CHECKED**, because a gap changes it too. That is the *point* of the fix,
>   and it is also precisely what makes this not a content slice: a content commit titled *"emerald"*
>   must not alter another weapon's appearance.
> - **`emerald_beam.yml`'s `size: 1.0` and `samples_per_block: 4` ARE LEFT ALONE** unless the re-run
>   says otherwise. Per beam the emerald is already **smaller** than lapis; tuning it now would be
>   treating a symptom of a misdiagnosis.
>
> **IT IS A SLICE, on the volley precedent:** sealed-schema change or core constant, loader arm if
> authored, validator arm, core tests, and its own gate row.
>
> ### ONE OPINION FROM THE CONTENT SIDE, OFFERED NOT DECIDED
>
> The operator invited the *shape* to be argued rather than assumed. **A per-visual field
> (`skip_first_blocks:` on the particle step) is the option to be most careful about**, for a reason
> this repo already has a name for: **absence is not a neutral value.** Every future beam author
> would have to remember it, and forgetting is **silent** — a new beam ships obstructing and nobody
> learns why for a slice or two.
>
> A **single value applied in the beam-drawing path** cannot be forgotten and fixes every ray weapon
> at once, which is the property that makes candidate 1 attractive in the first place. Its cost is
> that a future beam wanting **no** gap — drawn from a turret, a fake entity, anything not at a
> player's eye — cannot opt out. **That case does not exist yet**, so adding the field for it now
> would be prescribing a remedy before anyone has looked at the material. Ship the unauthorable
> version; add the override when a second case actually needs one.
>
> > **AND THE CALL-SITE COUNT IS THE EVIDENCE, NOT THE INTUITION. MEASURED: `presentAlong` HAS
> > EXACTLY ONE PRODUCTION CALLER.**
> >
> > ```
> > CastExecutor.java:321   if (beam != null) world.presentAlong(from, hit.map(RayHit::point).orElse(to), beam);
> > ```
> >
> > That is **the only line in the codebase that draws a beam.** Every other occurrence is the
> > `CombatWorld` interface declaration, the `PaperCombatWorld` implementation, the test fake, or
> > javadoc.
> >
> > **So a per-visual `skip_first_blocks:` field would pay a PERMANENT AUTHORING COST — one every
> > future beam author must remember, failing SILENTLY when forgotten — to configure something with
> > a single call site.** The opt-out case does not exist, *and* the thing it would opt out of has
> > one caller. Both halves matter: the first says nobody needs the flexibility yet, the second says
> > the flexibility buys almost nothing even if they did.
>
> **This is the content chat's read, not a ruling.** The feature chat owns it.
>
> ### WHAT THE GAP DOES TO THE LAPIS STAFF — CHECKED, NOT ASSUMED
>
> The fix reaches lapis **by design**, so `GATE-lapis-staff.md` was read before recommending it.
> **NO LAPIS ROW BREAKS:**
>
> | row | what it observes | why a start-gap cannot touch it |
> |---|---|---|
> | **L6** | *"beam stops at the wall face"*, fired at **5 blocks** | observes the **END**. The gap moves the **START**. |
> | **L7** | *"beam ends at the mob"*, fired at **10 blocks** — **sole witness** for the draw-to-hit-point rule | same |
> | **L3** | thickness at `size: 1.2` | unaffected |
> | **L11** | control — *"no beam"* from Solar Lance | unaffected |
>
> **SAY IT PRECISELY, BECAUSE TWO CLAIMS ARE BEING CONFLATED AND ONLY ONE IS TRUE: lapis's
> APPEARANCE changes; lapis's GATE does not.** *"It touches lapis"* and *"it invalidates lapis's
> gate"* are different statements. The first is the point of the fix. The second would be a reason
> to hesitate, and it is false.
>
> ### THE GAP SIZE HAS A HARD CEILING, AND IT COMES FROM CE4 ITSELF
>
> **A BEAM SHORTER THAN THE GAP DRAWS NOTHING AT ALL.**
>
> The suggested range was 1–3 blocks. **At 3, CE4's near staging IS 3 blocks** — this row would fire
> at a wall, see **no beam whatsoever**, and report *"not blinding"*. **True, and measuring
> nothing.**
>
> **THE ROW WOULD PASS BY DRAWING ZERO PARTICLES — a control that succeeds for the wrong reason, in
> the row whose entire job is verifying this fix.** That is this project's named failure, landing in
> the worst possible place.
>
> `cfde822`'s `VISUAL_GAP = 1.0` leaves two blocks of beam at that staging. **The upper end of the
> suggested range breaks the row that has to verify it.**
>
> **The constant is bounded from above by the shortest staging any gate row uses, and NOTHING
> ENFORCES THAT RELATIONSHIP.** The two tightest, measured from the gate files:
>
> | staging | row | beam left at gap 1.0 | at gap 3.0 |
> |---|---|---|---|
> | **3 blocks** | **CE4 near** — the binding constraint | 2 blocks | **nothing** |
> | 5 blocks | `GATE-lapis-staff.md` L6 | 4 blocks | 2 blocks |
>
> **Write it AT THE CONSTANT, with the number:** *must stay well under CE4's 3-block near staging,
> or that row measures a beam that was never drawn.*
>
> ### AND THE GAP CREATES A NEW BEHAVIOUR THAT WANTS ITS OWN ROW
>
> **A point-blank shot, closer than the gap, now draws NO BEAM.** Whether that reads as *"you are
> touching it"* or as *"the weapon did not fire"* is a question **nobody has looked at** — and the
> impact visual still plays at the hit point either way, so the feedback is partial rather than
> absent.
>
> **Add it to the gap slice's own gate rather than discovering it from a player report.**
>
> ### THE CONSTANT SHIPS MARKED PROVISIONAL, ON THE VOLLEY AND IGNITE PRECEDENT
>
> **`1.0` is `cfde822`'s number for `cfde822`'s geometry.** Ship it **marked provisional**, with
> **CE4's re-run as what rules it** — the same discipline Ignite's four constants got, where the
> gate ruled them and the marker came off in the housekeeping commit.
>
> **Do not let it ship unmarked.** A gap nobody chose deliberately is exactly the magic number that
> `theRayRangeIs32AndTheFileSaysWHY`'s mutation exists to prevent — *a value-only assertion cannot
> see a deleted reason*, and an unmarked inherited constant has no reason to delete.

### CE5 — **mana. figure. AND THE FIGURE IT MEASURES IS PROVISIONAL BY DECLARATION**

Cast until dry from a full bar. **Record how many casts, and how long the refill takes.**

The arithmetic to check it against: 40 mana per 30 ticks is **26.7 mana/second sustained** against a
100 pool — **under four seconds to dry, two and a half casts.**

> **THIS ROW IS WHERE 40 GETS RE-DECIDED, WHICH MEANS THIS ROW CANNOT ALSO VALIDATE IT.**
>
> `cfde822`'s `CURSED_EMERALD_MANA_COST` was set against **that** repo's regen, **that** repo's pool
> and **that** repo's gear. **None of that economy came across.** The number was carried because it
> was the only one anybody had, and `cursed_emerald.yml` says so at the field.
>
> **So a completed CE5 means "40 was observed" and NEVER "40 is fine."** Without this sentence a
> green CE5 silently promotes a placeholder into a decision — the exact shape of the standing rule
> that **a tuning request against a system nobody has watched encodes a guess as a requirement.**
>
> The observation is the input to a decision the operator has not made yet. Record what happened;
> do not tick this as an approval.

> #### RESULT — **PASS. RUN, MATCHED PREDICTION. AND 40 MANA WAS MEASURED, NOT APPROVED.**
>
> The run matched the arithmetic this row predicted in advance, and **no figures were transcribed,
> by operator decision** — copying a prediction into the observation slot produces a number that
> *looks* measured and was not, and the row would stop being able to disagree with the file.
>
> **THE DISTINCTION THIS ROW WAS WRITTEN TO PROTECT IS STILL LIVE, AND THE GREEN TICK IS EXACTLY
> WHAT THREATENS IT.** `40` remains an **unruled placeholder** inherited from an economy that did
> not come across. *A green tick beside an unruled placeholder is how a guess becomes a
> requirement* — which is the standing rule this row was written in advance to prevent.
>
> **So: CE5 is green and 40 is still undecided. Those are not in tension; they are the whole point
> of the row.**

### CE6 — track a moving target through the burst

Strafe past a knell mid-volley. **Do later shots follow, and does the beam originate from where you
are now or where you were?**

**NOT A SOLE WITNESS — see the coverage section above.** `GATE-volley.md` V4 established re-aim and
the per-shot crit roll **on the mechanism**, on a fixture staged harder than this. What CE6 adds is
that **this weapon authors a volley rather than something else**, and that the re-aim is visible at
the emerald's own cadence and range.

Record it as confirmation, not as discovery.

> #### RESULT — **PASS, as confirmation.** Later shots follow, and the beam originates from where
> the caster is now.
>
> **Recorded as confirming that this weapon authors a volley and re-aims at its own cadence — NOT as
> an independent witness of re-aim.** `GATE-volley.md` V4 established that on the mechanism, on a
> harder staging. Two green rows here and there are **one** piece of evidence about the wrapper, not
> two.
>
> **AND CE4's CANDIDATE 2 WOULD BREAK THIS ROW**, which is the concrete cost named there: this row
> reads the beam's *origin* as the evidence. Drawing from the hand leaves it testing nothing.

### CE7 — **swap weapons mid-burst. figure — AND KNOW BEFORE RUNNING IT THAT IT CANNOT MOVE**

Start a volley, switch to a different weapon before shot 4. Record whatever shots 4–6 hit for.

> **A PASS HERE PROVES ALMOST NOTHING, AND THE ROW SAYS SO IN ADVANCE SO THAT NOBODY REPORTS IT AS
> Q3 ANSWERED.**
>
> `GATE-volley.md` V7 ran exactly this staging on `volley_stone` and got *"same damage"* — **forced
> by the fixture.** An authored `amount:` is captured in the walker's closure, and no swap can reach
> it. **This weapon authors `amount: 27`, so it is the same shape**, and a swap will not move its
> numbers either.
>
> ***A row that could not have come out any other way is not evidence.***
>
> **Run it anyway** — it confirms the burst is *not cancelled* and that shots 4–6 still land, which
> is a real property and is genuinely this weapon's. **Record it as "unchanged, as forced."**

**Q3 IS NARROWED, NOT ANSWERED.** The pricing risk lives in exactly two places, and **neither is
tested anywhere:**

| untested case | why nothing here reaches it |
|---|---|
| a volley whose payload is **`weapon_damage`** | this weapon authors `27`; nothing re-reads the weapon at shot time |
| a swap **to an ENCHANTED weapon**, or one carrying a class bonus | the preconditions above require a clean run, so every multiplier is 1 |

> #### RESULT — **"normal damage." UNCHANGED, AS FORCED — exactly as the row predicted of itself.**
>
> **What this DOES establish, and it is real and is this weapon's:** the burst is **not cancelled**
> by a swap, and shots 4–6 still land.
>
> **What it does NOT establish: anything about volley pricing.** `amount: 27` is authored and closed
> over in the walker; **no swap could have reached it.** The row was written in advance to say so,
> and it came out as written — which is the definition of a result that carries no information about
> the hypothesis.
>
> **Q3 STAYS NARROWED. Both untested cases above are intact.**

### CE8 — **NEW. Where does a MISSED volley burst, now that the range is 32? figure**

Fire at open sky. **Write down whether you can see the impacts at all.**

**THIS ROW EXISTS BECAUSE A NUMBER MOVED UNDER AN ARGUMENT.** `PLAN-cursed-emerald.md`'s Q5 note
reasoned from `range: 64` that six mid-air bursts would be **certainly invisible** to the caster —
and that was the entire basis for accepting the inherited detonate-on-miss behaviour.

**At range 32 the bursts land AT the particle render cap** — *marginally* visible rather than
certainly invisible. **That is neither the plan's answer nor its opposite**, so the conclusion is
re-derived here rather than carried forward with its premise swapped.

*If six bursts at the cap edge read as noise, the remedy is a content one — thin `emerald_impact.yml`
— before it is ever a mechanism one.* Changing `stepRay`'s detonate-on-miss stays **out of a content
slice** regardless: it is shared by every ray weapon, and altering it from a commit titled *"emerald"*
would silently change the Lapis Staff.

> #### RESULT — **PASS. RUN, MATCHED PREDICTION.**
>
> The bursts land at the cap edge as the re-derivation predicted, and **they do not read as noise** —
> no thinning is called for. Q5's ruling stands on an observation at the shipped range rather than on
> the plan's reasoning at a range that was never authored.
>
> **No figures transcribed, by operator decision**, for the reason CE5 records: copying a prediction
> into an observation slot yields a number that looks measured and is not.

---

## WHAT THE SUITE ALREADY COVERS, SO NO ROW IS INVENTED FOR IT

`CursedEmeraldContentTest` (five rows, added with this slice) pins, at build time:

- **all five files load** through the real loaders, behind a `lapis_staff` positive control;
- **every `visual_id` the weapon names resolves** — windup, beam, impact;
- **the recipe mints the weapon**, and its three-row shape **cannot fit a 2×2**;
- **`range` is 32 — and the file still carries WHY**;
- **the weapon produces NO boot warning**, the mirror of Rake's expected one.

**None of it can see a single pixel, a sound, or a mana bar.** Every row above is presentation,
economy or feel, and a green suite is not evidence for any of them.

> ### THE RANGE ROW GUARDS A REASON, WHICH IS NEW IN THIS PROJECT
>
> `theRayRangeIs32AndTheFileSaysWHY` was **verified by mutation**: stripping the ruling sentence from
> `cursed_emerald.yml` **while leaving `range: 32` correct** turns it red. That is the first time
> here that a **reason** has been shown to be under test rather than asserted to be.
>
> **A VALUE-ONLY ASSERTION CANNOT SEE A DELETED REASON, AND A DELETED REASON IS HOW A RULING BECOMES
> A MAGIC NUMBER.** `range: 32` without its sentence is indistinguishable from somebody's
> preference, and **the next person who wants more reach has nothing to argue with** — they cannot
> tell a measurement from a taste call, so they change it.
>
> **RECORDED TRIGGER, NOT A FIX FOR THIS BRANCH: `volley_stone`'s `cooldown_ticks: 0` HAS EXACTLY
> THIS SHAPE AND IS UNGUARDED.** The *value* is pinned by
> `theFixtureProducesEXACTLYONEBootWarningAndItIsRakesFloor`. The **reason** it is zero — that it is
> the only value which makes V5 discriminate, since authoring the floor would stage two independent
> quantities as equal — lives in prose that nothing reads. Written here so it is found deliberately,
> rather than by someone deciding that a zero looks like an oversight and helpfully authoring 27.
