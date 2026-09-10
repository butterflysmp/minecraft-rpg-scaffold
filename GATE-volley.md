# GATE — CastSpec.Volley

**This file is the source of truth for the volley boot gate's CONTENT.** It is versioned with the
code because for several behaviours below **these rows are the only check that exists anywhere in
the project.** The suite passes with any of them deleted — 1462 tests, and not one of them can see
whether eight beams one tick apart read as eight, as one, or as a flicker.

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

## THE INSTRUMENT

`/rpg give volley_stone`. **Two triggers, sharing no number with each other and none with any
weapon**, because a wrapper gated only at one weapon's settings has not been shown to generalise.

| | trigger | wind-up | shots | interval | inner cast | authored cooldown | derived floor |
|---|---|---|---|---|---|---|---|
| **Scatter** | right-click | 10 | 3 | 5 | projectile, `flint` body | **40** | 20 |
| **Rake** | left-click | 20 | 8 | **1** | ray, range 64, `volley_beam` | **0** | 27 |

Rake is **deliberately worse than anything content will author**: interval 1 is the scheduler's
floor, and eight shots at 64 blocks puts up to four rays in flight at once. It measures the
grammar's limit, not a weapon's tuning.

**Neither trigger costs mana**, so a row can be repeated as often as it needs to be. That is a
fixture decision; a real weapon prices its volley.

## PRECONDITIONS, WHICH EVERY FIGURE BELOW SILENTLY ASSUMES

`EffectApplier:136-138` passes the authored amount through `enchantDamagePercent`,
`classDamageBonus` and `chargeScale` **before** crit. So **4** and **8** hold only for an
**unenchanted stone, on a player carrying no class damage bonus and no charge scaling.** Run it
clean, or every number here is off by a multiplier and reads exactly like a broken volley.

---

## ROWS

Written before the boot, not after, and none of them is a tick-box.

### V1 — the wind-up is visible as a wind-up

Press left-click. Expect a chime, then **a full second of nothing**, then the burst.

**An instant first beam means `windup_ticks` was dropped**, and the telegraph is the entire reason a
volley's commitment cost is fair rather than a dead second. Then press right-click: its wind-up is
**half** a second, so the two must be *audibly different lengths*. One row, two wind-ups, because a
single wind-up cannot tell "the field is read" from "some fixed delay is applied".

### V2 — count the shots

`/rpg spawn knell`, hold still.

- **Scatter**: expect **three** flint bodies and **three** impacts.
- **Rake**: expect **eight** damage numbers, each `4` (white) or `8` (yellow).

**FEWER THAN THE AUTHORED COUNT IS THE DEFECT.** The knell's final HP alone cannot tell a missing
shot from a crit; the **count** can, and the colour explains any figure the count does not.

Kinetic's `damage_symbol` is `""`, so the numbers are **bare** — colour is the only signal on them,
with nothing competing for the eye. The element choice is what makes this row readable.

**AND BEFORE FIRING, READ BOTH TOOLTIPS AND WRITE DOWN WHAT THEY SAY ABOUT COOLDOWN.** Scatter's
line and Rake's *absence* of one are the observation that feeds the tooltip ruling under V5 — the
printed number is the authored one, and for Rake nothing is printed at all. This costs one glance
and it is the only place the player-facing half of that gap is looked at.

### V3 — **figure.** Q2, THE MEASUREMENT NOBODY HAS TAKEN

Rake at a wall, from **40 blocks** and again from **64**. Up to four rays in flight at once, each
drawing its own beam, from a muzzle that may have moved between them.

**Do they read as separate beams, as one thick beam, or as a flicker?** Write down what you saw.

**Nothing in this repo has ever produced this.** `stepRay`'s recorded reasoning — *"particles only
render about 30 blocks out… about a tick to the caster… about 5 ticks to an observer"* — was written
for **one shot at 26 blocks** and breaks all three premises here. It should not be read as covering
this, and this row is what replaces it.

*If eight at one tick are legible, six at two are safe. If they are a blur, content learns the bound
before it authors against it rather than after.*

### V4 — the re-aim and the re-roll

Strafe past a knell mid-Rake. **Do later beams follow, and does the beam originate from where you
are now or where you were?**

Then look for **mixed white and yellow numbers in one burst**. One shared roll cannot produce both,
so a burst that is uniformly one colour every single time is the tell that the projection was
frozen. (A uniform burst *once* is ordinary — eight shots at 15% miss entirely about a quarter of
the time. It is uniformity across many casts that is the signal.)

### V5 — the derived floor refuses a re-press, and it MOVES

Rake authors `cooldown_ticks: 0`, so **any refusal it produces can only have come from the
derivation.** That is why it is 0 and not 27: with the authored value equal to the floor, the row
would pass whether the floor existed or not — two independent quantities staged equal, which is a
control that succeeds for the wrong reason.

1. Spam left-click through a burst. Expect *"On cooldown"* until the last shot, then a fresh burst.
2. **Then edit `shots: 8` to `shots: 4`, `--refresh-content`, and confirm the refusal window got
   SHORTER.** That is the property under test — *the guard cannot drift from the numbers* — and step
   1 alone does not test it.
3. Restore `shots: 8`.

**Expect exactly one `Content:` warning at boot**, naming `volley_stone` `left_click`, its authored
`0` and its derived `27`. That warning is the load-time explanation the floor owes an author, and it
is **expected, not a defect**. Scatter must produce **no** warning: its authored 40 exceeds its floor
of 20, and a warning there would mean the floor is overwriting deliberate values.

> **AND THE GOLDEN TOOLTIP DUMP TURNED UP A GAP — WHICH IS OLDER AND LARGER THAN A VOLLEY.** The
> tooltip renders the **authored** `cooldown_ticks`, so Scatter shows *"Cooldown: 2.0s"* and **Rake
> shows no cooldown line at all** — while Rake's real, enforced guard is 27 ticks. `ContentValidator`
> tells the AUTHOR at load; nothing tells the PLAYER. That is *a value that was overridden and a
> value that was never read look identical*, arriving at the one surface where the reader has no file
> to check.
>
> **VOLLEYS ARE THE SECOND CONSUMER OF THIS GAP, NOT A NEW BUG.** `WeaponLoreLines`' own class
> javadoc already records that a ranged basic attack's cadence is its authored `cooldown_ticks`
> *"through `AttackSpeed.effectiveCooldownTicks`"* — so **the tooltip has been an approximation for
> one class of weapon since before this slice existed.** The derived floor joins attack-speed scaling
> as a second reason the printed number is not the stamped one.
>
> **SO THE RULING IS NOT "WHAT LINE SHOULD A VOLLEY SHOW".** It is: **should the tooltip render what
> `AbilityService` will actually stamp** — `max(authored, derived)`, and attack-speed-scaled for a
> basic attack? A volley-shaped fix would leave the older half of the same defect standing and make
> the remaining gap look *deliberate* to the next reader, which is exactly the reasoning
> `ContentValidator`'s projectile-`item` arm already gives for validating both call sites or neither.
>
> **V2 takes the observation; the ruling is the TOOLTIP's, not this slice's.** Glance at both
> triggers' tooltips before firing and note what each says about cooldown. Nothing is changed here:
> rendering the stamped value is a `WeaponLoreLines` change that moves every affected weapon's
> tooltip and needs a decision about what the line should SAY (*"Cooldown: 1.35s"* reads as authored;
> *"Burst: 1.35s"* does not).

### V6 — **figure.** Q4, and it has TWO stagings in one row

**Ask: can you tell how many shots fired WITHOUT counting the damage numbers?**

`on_hit` already fires per shot, so the impact visual is a partial per-shot cue and the real question
is whether it carries the count on its own. If it does, `on_shot` is a hook nobody needs and the
answer is a deletion rather than an addition.

**Then fire Rake at open air.** On a clean miss the impact plays 64 blocks out in mid-air, where the
caster almost certainly cannot see it — **so a volley that misses has no per-shot feedback at all.**
That is the sharpest form of the gap and it will not appear in a staging against a target standing
still. Hit, then miss; both go in this row.

*If the answer is "a cue is wanted", `on_shot` is the answer and there is no cheaper one:* the chime
belongs **at the caster, at fire time**, and an impact visual plays **at the target, at land time**.

### V7 — **figure.** Q3's observation, taken before anyone rules

Start Rake, then **switch to a different weapon before shot 4.**

**Whatever shots 4–8 hit for is the answer**, and it cannot be read off the code — the projection
reads whatever is in the hand at that tick. Record the numbers. *"Does a swap cancel the burst" and
"what do the remaining shots hit for" are the same question, and it is a pricing rule rather than a
housekeeping one.*

### V8 — the projectile inner cast is not a rumour

Scatter visibly throws **three flint bodies**, half a second apart, arcing under gravity.

This is the row that makes *"the wrapper generalises past rays"* a measurement instead of a claim.
Without it, the whitelist's second member is admitted on an argument.

### V9 — the caster leaves mid-burst

Start Rake and **log out before shot 8** (a second account, or `/kill`). Log back in.

Expect: the remaining shots simply never happen, no error in console, and **no beam continuing from
where you were standing.** A volley is scheduled on its caster; with no caster there is nothing to
re-read and nothing to fire.

---

## WHAT THE SUITE ALREADY COVERS, SO THE GATE IS NOT ASKED TO RE-CHECK IT

Stated so that a row is not invented for something a test already pins, and so that a green gate is
not read as evidence for these:

- **Timing** — every shot's tick, both sides of each boundary (`CastExecutorVolleyTest`).
- **The re-READ** — that each shot re-projects and re-aims, and that a frozen projection reddens.
  What the suite **cannot** see is that the adapter draws a **new crit number** per read; that is
  V4's half.
- **Stopping** on a gone or dead caster, in core. V9 is the same behaviour through a real logout.
- **The floor's arithmetic and its application**, including that a longer authored cooldown is kept.
- **The whitelist's refusals**, each by name, and the validator's recursion into the inner cast.

**None of the suite can see a single pixel.** V3 and V6 are presentation's only witnesses, and V5's
step 2, V7 and V9 are the only checks that the mechanism behaves through the real server rather than
through a fake clock.
