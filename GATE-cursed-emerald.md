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

### CE1 — the wind-up is visible as a wind-up

Press right-click. Expect **a chime, then a full second of nothing, then six shots.**

**An instant first shot means `windup_ticks` was dropped**, and the telegraph is the entire reason a
one-second commitment is fair rather than a dead second.

`emerald_windup.yml` is this row's only witness — if that sound is ever removed, CE1 stops being
able to tell a dropped wind-up from a working one.

*Mechanism-wise this is V1's territory; what is new is only that THIS weapon's `on_cast` is wired
and its sound key resolves on the pinned jar.*

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
the Lapis Staff draws one at 1.2. If the answer is "yes, blinding", `emerald_beam.yml`'s size or
`samples_per_block` is the lever, and it is a content fix.

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

### CE6 — track a moving target through the burst

Strafe past a knell mid-volley. **Do later shots follow, and does the beam originate from where you
are now or where you were?**

**NOT A SOLE WITNESS — see the coverage section above.** `GATE-volley.md` V4 established re-aim and
the per-shot crit roll **on the mechanism**, on a fixture staged harder than this. What CE6 adds is
that **this weapon authors a volley rather than something else**, and that the re-aim is visible at
the emerald's own cadence and range.

Record it as confirmation, not as discovery.

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
