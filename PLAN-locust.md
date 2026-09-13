# PLAN — the Locust: a ruled Ranger weapon, and the withdrawal of the multiples-of-8 rule

> Slice C was scoped as a **two-item dual-wielded "Dual Boltor"**. That design is **refused**. This
> is the slice that replaces it: one weapon, no new mechanism, and the standing rule the refusal
> leaves without a subject.

## EVERY LOCATOR IN THIS FILE NAMES A SECTION, NEVER A LINE

This plan schedules commits that insert into the files it cites, so **any line number in it would be
falsified by its own commit 2**. That is `CLAUDE.md`'s own rule, and a plan is the document class
most guaranteed to break it.

> **AND A LINE NUMBER IS FALSIFIED BY WHICH TREE YOU READ, NOT ONLY BY LATER INSERTION. MEASURED
> DURING THIS PLAN'S REVIEW, WHERE IT COST A ROUND TRIP.** Two parties measured the same two files
> and produced contradictory **correct** answers, because this branch is three commits ahead of
> `origin/master`:
>
> | file | `origin/master` `f7b27e7` | `fix/dormant-findings-register` `964bb84` |
> |---|---|---|
> | `CLAUDE.md`, the multiples-of-8 rule | `:809` | `:852` — `git diff --numstat f7b27e7..HEAD -- CLAUDE.md` -> `43  0` |
> | `NEXT.md`, the dormant register | **does not exist** | `:9534` — added by `bd1eb96`, branch-only |
>
> On `master` those same numbers land in unrelated standing prose. **Neither reading was wrong; both
> omitted the revision.**
>
> **A LOCATOR IS A MEASUREMENT OF A TREE, NOT AN ADDRESS IN A DOCUMENT.** Cite `rev:path:line`, or
> cite a section. `grep -n` is to a line number what `git rev-list --count` is to a commit count.

> **AND ONE FALSE ABSENCE WAS MANUFACTURED AND CAUGHT IN THE SAME REVIEW.**
> `git grep "multiples of 8"` finds the rule but **neither place that quotes it**, because the phrase
> **wraps a line** in both — `CLAUDE.md`'s worked example breaks after *"multiples of"*, the pin's
> javadoc after *"if it may ever"*. A line-based grep cannot match across a wrap. **Searching the
> single token `dual-wield` is how all four citation sites below were found.** Filed as a third cause
> of false absence in `CLAUDE.md` by commit 3.

---

## Context

The operator's rulings, in order:

> *"let's stop calling it the Dual Boltor. it's not going to be Dual wielded so that's not a fitting
> name."*

> *"No"* to the alternating muzzle visual — *"although it's a good idea for the future."*

> *"One note, the Boltor should be Uncommon as well."*

> *"You know what, lets make it rare. And while we're at it, let's make it a little bit more unique.
> Give it the Nature element instead of Kinetic, and make the beam color more dark green color to
> resemble nature."*

> *"Keep 96"*

**Name ruled: `locust` / "Locust".** The rulings do **four** things at once, and this slice is the
four together:

1. **It authors a weapon** — 26 damage, 12 quiver, 3s reload, 12-tick RoF.
2. **It withdraws a standing rule.** *"multiples of 8 for anything that may ever be dual-wielded"*
   has no subject any more, and **12 is not a multiple of 8** — so under the old rule this weapon
   would read as violating a standing constraint. Corrected in the same commit that authors the
   counterexample, by naming what superseded it. **The Boltor's 16 is not re-ruled.**
3. **It makes a dormant finding live by the roadmap.** The operator's requirement 3 names
   enchantments that *"increase rate of fire"*. On a quantised weapon a percentage rate buff is
   **inert** until it crosses a whole grid step, and the Locust's dead zone is the **wider** of the
   two.
4. **It discharges a recorded refusal, and unparks an element.** `nature` is refused by name in
   `cursed_emerald.yml` on the grounds that *"elements are parked"* and adding one **unasked** is
   wrong. There is now a ruling, so the refusal's **condition is met rather than its judgement
   reversed** — and the Locust becomes the **first weapon in the project to carry a third element**.

**Scope: content only.** No dispatch change, no hand parameter, no `CooldownTracker` change,
`RpgListeners.onRightClick` byte-identical. The alternating-barrel visual is refused and recorded as
a future idea. **No new mechanism at all.**

---

## THE RULED NUMBER SET

| | Locust | Boltor | how decided |
|---|---|---|---|
| `attack_damage` | **26** | 19 | operator |
| `quiver_size` | **12** | 8 | operator |
| `reload_ticks` | **60** (3.00s) | 60 | operator |
| `cooldown_ticks` | **12** | 16 | operator |
| `id` / `display_name` | **`locust` / "Locust"** | — | operator |
| `rarity` | **`rare`** | **`uncommon`** — was `rare` | operator, both; **each re-ruled 2026-09-13, in opposite directions** |
| `element` | **`nature`** | `kinetic` | operator |
| `range` | **96** | 96 | operator — *"Keep 96"* |
| beam | **`locust_beam`, forked, dark green** | `boltor_beam` | operator |

**Not ruled — authored with the choice flagged at its own key**, the pattern `boltor.yml` uses:
`material: crossbow`, `class: ranger`, pierce none. **That list is now three items, not six.**
`rarity`, `element`, `range` and the beam have all been ruled since the first draft, and each moves
out of the flagged set into the table above rather than keeping a stale "choice, re-rule freely"
note beside a number the operator has since ruled.

### THE RARITY INVERSION NOW RESOLVES THE RIGHT WAY ROUND, AND IT TOOK BOTH RULINGS

The first draft flagged that the Locust's `uncommon` would sit **below** the Boltor's `rare` on a
weapon stronger on every axis, with the tooltip footer saying so out loud. **Two separate rulings
answered it, and neither reverses the other:**

```
  draft        Boltor  rare      19 dmg        Locust  uncommon   26 dmg     INVERTED
  ruling 1     Boltor  uncommon  19 dmg                                      levelled
  ruling 2                                     Locust  rare       26 dmg     ORDERED
```

**The Boltor's move to `uncommon` is NOT reversed by the Locust's move to `rare`.** They were ruled
separately, they compose, and the result is the ordering the flag asked for: the stronger weapon now
sits a tier above the weaker one instead of a tier below it.

**`rare` is not orphaned, and it does not shrink either.** Measured:
`grep -l "^rarity: rare" content/weapons/*.yml` returns **three** today (`boltor`, `ember_staff`,
`emberblade`). The Boltor leaves and the Locust arrives, so the tier stays at **three** —
`ember_staff`, `emberblade`, `locust`. The first draft said it would fall to two; that was true of
ruling 1 alone and **is falsified by ruling 2**, which is why the count is restated rather than left
standing.

Two consequences, and the second is the one no count can see: `boltor.yml`'s rarity block becomes a
**second** supersession, and the golden becomes a **mixed** diff. Both are carried below.

---

## THE ELEMENT: `nature`, AND THE FLAG IT DISCHARGES

**Ruled by the operator.** The Locust is the **first weapon in this project to reach a third
element**, and that is a precedent worth naming rather than a detail. Measured across all eleven
shipped weapons: `kinetic` (7) and `fire` (4), with `nature`, `undead`, `void`, `water` and `wither`
registered and unused.

### THE FLAG IS DISCHARGED, NOT DROPPED

The first draft flagged `element: kinetic` with *"applies_status would accrue at 1.67/s, unruled"* —
the worry being that a fast weapon carrying a status-declaring element buys an accrual rate nobody
has ruled. **Measured, and the worry does not arise:**

```
  content/elements/nature.yml, in full -- two lines, no applies_status:
      display_name:  "<green>Nature</green>"
      damage_symbol: "<green>✿</green>"

  grep -n "^applies_status" content/elements/*.yml   ->  fire.yml ONLY
```

**So nature accrues nothing, and the accrual worry is answered rather than inherited.** Said
explicitly, because **a flag that vanishes reads as an oversight and a flag that is discharged reads
as an answer.**

> **AND ONE OF MY OWN MEASUREMENTS WAS WRONG FIRST TIME, BY THE MECHANISM THIS FILE ALREADY WARNS
> ABOUT.** `grep -l "applies_status" content/elements/*.yml` returns **`fire.yml` AND `kinetic.yml`**
> — because `kinetic.yml`'s *comment* reads *"It declares no applies_status."* **A grep for a key
> matched the prose denying the key.** Anchoring at line start (`^applies_status`) is what separated
> them. Same family as the line-wrap false absence in the masthead: **the search matched text that
> was not the fact.**

### THE RECORDED REFUSAL IS DISCHARGED ON ITS OWN TERMS — IT IS NOT OVERTURNED

`cursed_emerald.yml`'s element block refuses exactly this element:

> *"`nature` WAS THE OBVIOUS REACH -- green stone, green beam -- AND IS REFUSED. Elements are parked,
> and `element:` is not cosmetic: it drives the damage glyph and, for a status-declaring element,
> accrual. Adding an element is one file and costs nothing, which is exactly why adding one **unasked**
> is easy and wrong."*

**The operative word is UNASKED. That paragraph refused an element added without a ruling; there is
now a ruling.** The refusal's *condition was met*, not its judgement reversed.

> **A RECORDED REFUSAL THAT NAMES ITS CONDITION IS DISCHARGED WHEN THE CONDITION IS MET, NOT
> OVERTURNED. Read what a refusal actually refused before recording a reversal** — *"not without a
> ruling"* and *"not ever"* look identical from a distance and they age in opposite directions.

**And the supersession is narrower than it first looks, which is the part to get right.** That
paragraph contains **two** claims and only one of them moves:

| claim | status |
|---|---|
| *"Elements are parked"* | **DISCHARGED** — `nature` is unparked by ruling, and ships on the Locust |
| *"this weapon is kinetic, because kinetic's `damage_symbol` is `""` so CE2's six numbers stay BARE"* | **UNTOUCHED** — a readability argument about the Cursed Emerald's own six-number burst, independent of parking |

**So `cursed_emerald` stays `kinetic`, for its own still-valid reason.** The note added in commit 2
says the parking premise is discharged and the weapon's own choice is unchanged — not that the
refusal was wrong.

**`lapis_staff.yml` was checked and needs NO change.** Its element paragraph refuses `void` *for the
Lapis Staff*, on a colour/flavour-mismatch argument, and restates the same general *"adding one
unasked is easy and wrong"* principle. **That principle is not falsified by an element added when
asked** — it is the principle this ruling satisfies. Checked and reported rather than silently
omitted, so the absence is not read as a miss.

---

## THE FIGURES — **DERIVED**. Not ruled, not measured.

```
12 shots span ELEVEN intervals.   11 x 12 = 132t firing  +  60t reload  =  192t  =  9.60s

  sustained   12 / 9.60        = 1.2500 sh/s   x26 = 32.50 DPS
  burst       12 x 26 / 6.60s  = 47.2727 DPS          (132t = 6.60s)
  magazine    12 x 26          = 312
```

Against the Boltor's **measured** figures (`GATE-boltor.md`, `## ROW 1 — THE BOUNDARY READING`):

| | Boltor | Locust | delta |
|---|---|---|---|
| sustained DPS | 17.6744 | 32.50 | **+84%** |
| burst DPS | 27.1429 | 47.2727 | **+74%** |
| **per magazine** | 152 | 312 | **+105%** |

**The per-magazine figure is the only one that crosses 2x**, and it is the one a player feels in an
engagement nobody reloads through. **Reported, not re-ruled.**

> **THE FENCEPOST THIS PROJECT HAS ALREADY PAID FOR TWICE**, both times on this weapon family.
> Twelve shots span **eleven** intervals: `11 x 12 = 132`, not `144`.

---

## THE TWO GOOD PROPERTIES OF 12

**1 — On the 4-tick grid.** `ceil(12/4) x 4 = 12`, so no quantisation penalty and the Attack Speed
tooltip is honest **by construction**: `WeaponLoreLines.rangedAttackSpeedLabel` gives
`20.0/12 = 1.6667` -> **"1.7"**, and the weapon delivers `1.6667/s` -> "1.7". Same digit, and here
for the right reason rather than by rounding luck.

**2 — A SECOND LIVE INSTRUMENT FOR THE `>=` BOUNDARY, ON A SHIPPING WEAPON.** 12 is an exact multiple
of 4, so it discriminates `>=` from `>`: under `>` the input at `t = 12` is missed and it fires at
16. After `hunters_bow` and `quiver_stone` are deleted, that boundary would have rested on `boltor`
alone. **It now rests on two, and neither is in the deletion set.**

> **DO NOT OVERCLAIM IT.** `12 mod 4 = 0`, which `HeldFireQuantisationPinTest`'s own table puts in
> the **BLIND** class for the grid-size question — `ceil(12/2) x 2 = 12` too. The Locust adds a
> **`>=` point** and **NOT an `INPUT_FLOOR_TICKS` point**. That constant's sole guard remains the
> pin's dead-zone row.

### THE PIN ROW IS DEFERRED — OPERATOR RULING

`HeldFireQuantisationPinTest`'s `READINGS` are **boot measurements**: the constant beside them is
documented *"MEASURED, NOT DERIVED"* and every row cites a gate readout. **A `12 -> 12` row for a
weapon that has never booted would be a predicted value in a measured table** — precisely the
provenance-mixing that file exists to prevent.

> **TRIGGER — the first `/rpg firerate` reading taken on the Locust.** On that day, add a fourth
> `Reading("locust", 12, <measured min>, "<readout>", false)` and state in its javadoc that it adds a
> **`>=` point and not an `INPUT_FLOOR_TICKS` point**.

**No gate file in this slice.** With no new mechanism there is nothing to boot-measure beyond that
cadence confirmation, and that row belongs to the pin, not to a gate.

---

## THE QUANTISATION MODEL — ONE COMPUTATION, TWO FORMS, BOTH LABELLED

**Computed once here and referenced everywhere below.** An exact threshold and the first sample past
it are **different facts wearing the same units**, and an earlier draft printed `+28.0%` and `+28.1%`
for one quantity without saying which was which.

The delivered interval is `ceil(effectiveCooldownTicks(n, s) / 4) x 4`. It reaches a target step `m`
when `round(n/s) <= m`, i.e. `n/s < m + 0.5`:

```
                    s  >  n / (m + 0.5)              EXACT, strict
```

**`m + 0.5`, not `m - 0.5`.** An earlier draft inverted the sign while pattern-matching the `3.5` in
the one-step form `s > n/(n - 3.5)` — which is this same formula at `m = n - 4`, since
`m + 0.5 = n - 3.5`. See the closing section: **a general form must reproduce the worked values it
sits above.**

| `n` | first step | **EXACT threshold** | as % | first `0.001` **sample** past it |
|---|---|---|---|---|
| 4 | **never moves** — 4 is the input floor | — | — | — |
| 8 | `8 -> 4` | `8/4.5` = **1.777778** | +77.7778% | 1.778 |
| **12** | `12 -> 8` | `12/8.5` = **1.411765** | +41.1765% | 1.412 |
| **16** | `16 -> 12` | `16/12.5` = **1.280000** | +28.0000% | 1.281 |
| 24 | `24 -> 20` | `24/20.5` = **1.170732** | +17.0732% | 1.171 |
| 32 | `32 -> 28` | `32/28.5` = **1.122807** | +12.2807% | 1.123 |

**Only the EXACT column is quoted in prose anywhere in this slice.** `1.28` is the figure already
recorded in `boltor.yml`'s dead-zone block and in the pin's dead-zone row, so quoting the sampled
`1.281` would have contradicted two existing records.

### THE FULL LADDERS, BECAUSE A STEP COUNT IS ONLY TRUE UNDER A CAP AND THERE IS NO CAP

```
  Locust,  cooldown 12               Boltor,  cooldown 16
    12 ->  8   s > 12/8.5  = 1.4118    16 -> 12   s > 16/12.5 = 1.2800
     8 ->  4   s > 12/4.5  = 2.6667    12 ->  8   s > 16/8.5  = 1.8824
     4 is the input floor               8 ->  4   s > 16/4.5  = 3.5556
                                        4 is the input floor

    TWO steps                           THREE steps
```

> **AN EARLIER DRAFT SAID "ONE STEP" AND "TWO STEPS", WHICH IS TRUE ONLY UNDER AN UNSTATED CEILING**
> between `s = 2.0` and `s = 2.667`. **Measured: no such ceiling exists.** `AttackSpeed` declares
> `MIN_SPEED = 0.1` and **no maximum** — `git grep "MAX_SPEED"` returns nothing — and the only live
> source, the `attack_speed_boost_TEMP` dev item, permits a bonus of `0.0..20.0`, i.e. `s` up to
> `21.0`. **So the ladders are printed in full rather than summarised, and no figure here depends on
> an assumption it does not state.**

What the input floor makes unreachable is any interval **below** 4 — a different claim about a
different step. Both weapons reach 4; neither goes past it.

---

## THE FINDING — A RATE-OF-FIRE ENCHANTMENT IS INERT BELOW A WHOLE GRID STEP

**This reaches the enchantments the operator said he is planning.** From the table above:

- **cooldown 12** — nothing at or below `s = 1.411765` (**+41.18%**), then `12 -> 8`, **+50%** in one
  step.
- **cooldown 16** — nothing at or below `s = 1.28` (**+28%**), then `16 -> 12`, **+33%** in one step.

**So ordinary enchantment tiers — +5%, +10%, +15% — are worth EXACTLY ZERO on both weapons**, and the
Locust's dead zone is the wider. The tooltip would print a higher attack speed while the delivered
rate stayed byte-identical: **the shipped-lie shape `GATE-boltor.md` row 1 was run to rule out.**

### THE PROPERTY, AND THE REMEDY IS THE OPERATOR'S TO RULE AGAINST THE MATERIAL

> **A RATE-OF-FIRE ENCHANTMENT MUST MOVE THE DELIVERED INTERVAL, AND ON A QUANTISED WEAPON ONLY WHOLE
> GRID STEPS DO. A tier that cannot move it is not a weak tier, it is an INERT one.**

The obvious shape is **subtractive rather than multiplicative** — a tier that removes 4 ticks always
moves exactly one step, `16 -> 12 -> 8`, and never lies. **But that is a different mechanism from
`AttackSpeed.effectiveCooldownTicks`, and it is a design decision, not a fix.** No enchantment is
authored in this slice and nothing is chosen for him.

### THE REGISTER ENTRY'S COUNT IS NOT ZERO, AND THE TRIGGER ATTACHES TO THE HALF THAT IS

The dead-zone row of `NEXT.md`'s `### DORMANT FINDINGS` table reads **`0` shipped, `1` dev fixture** —
corrected in an earlier pass because the original grep scope could not see the
`attack_speed_boost_TEMP` dev item. What *is* measured at zero, separately, is the **enchant** column:

```
grep -rn "attack_speed" content/armor content/tools content/enchants content/shields   -> NONE
```

**So the trigger attaches to the enchant count, not to a plain zero:** *the first rate-of-fire
enchantment authored.* Commit 3 writes it into the register row.

---

## THE MULTIPLES-OF-8 RULE NOW HAS A LIVE COUNTEREXAMPLE

The rule, in `CLAUDE.md`'s `## Standing decisions — the operator's, not derivable from the material`,
in the Boltor entry's second bullet:

> Author **multiples of 4**; **multiples of 8** for anything that may ever be dual-wielded, since
> only those halve cleanly.

**12 is not a multiple of 8.** Under the two-item design the Locust would have read as violating a
standing rule. Under the ruling it is correct, **because nothing halves any more.**

### WHY TWO ITEMS COULD NEVER HAVE SATISFIED ITS OWN CONSTRAINTS — MEASURED

Halving a cooldown moves a weapon **up** the threshold table, and the dead zone **widens** as the
cooldown shrinks. Only multiples of 8 halve cleanly, so these four are the entire candidate set:

| single | dual (halved) | dual-wield dead zone (EXACT) |
|---|---|---|
| **8** | 4 | **attack speed wholly inert — 4 is the input floor** |
| **16** | 8 | **+77.7778%** |
| **24** | 12 | +41.1765% |
| **32** | 16 | +28.0000% |

`8` was already rejected for headroom (`boltor.yml`, the `cooldown_ticks` block). **To buy a *dual*
dead zone as narrow as the Boltor's *single-wield* +28%, you must author 32 — a 1.6-second shot,
which is not a rapid-fire weapon.**

**The three constraints — clean halving, attack-speed headroom, a fast weapon — are jointly
unsatisfiable.** `boltor.yml`'s dead-zone block already named the tension and deferred it:

> *"The honest cooldowns for attack-speed response and the honest cooldowns for dual-wield halving
> are NOT the same set; that tension is slice C's."*

**Slice C's answer is that there is no dual-wield.**

### THE CITATION SWEEP, ALL SITES FOUND WITH THE SINGLE TOKEN `dual-wield`

Three **quote the rule as standing** and go stale the moment it changes:

| file | section | action |
|---|---|---|
| `CLAUDE.md` | `## Standing decisions …` -> the Boltor entry's `16` bullet | **the rule itself** — rewrite |
| `CLAUDE.md` | `### AND THE GENERAL FORM …` -> the *"pointer must be obeyable without opening the account"* bullet | it uses the rule's text as **the worked example of a well-formed pointer** — update the quote; the point about pointer *form* survives untouched |
| `HeldFireQuantisationPinTest` | class javadoc -> the **POINTER** bullet of the `THIS IS A PIN` list | update the quote. **No `READINGS` row.** |

Two state it as a **live authoring rule** and gain a superseded note, re-ruling nothing:

- `boltor.yml` — the `cooldown_ticks` block, paragraph *"WHY A MULTIPLE OF 8 RATHER THAN OF 4."*
  **16 stands, for its other reasons, and the record must still explain why 16 and not 12.**
- `GATE-boltor.md` — `### 3. THE AUTHORING RULE FOR EVERY WEAPON AFTER THIS ONE`.

One is superseded by the **element** ruling:

- `cursed_emerald.yml` — the element block. Its *"Elements are parked"* premise is **discharged**;
  its *"this weapon is kinetic so CE2's six numbers stay bare"* judgement is **untouched**, and the
  weapon does not change. `lapis_staff.yml` was checked and needs nothing — its principle is
  satisfied, not falsified, by an element added **when asked**.

One more is superseded by the **rarity** ruling rather than the halving one:

- `boltor.yml` — the `rarity:` block. Its recorded justification (*epic is unused; `rare` has
  precedent; a baseline two tiers up reads as an outlier*) is **overturned. Supersede with the ruling
  and its date; do not delete.** That paragraph explicitly invited re-ruling — *"Re-rule it freely;
  nothing else in this file depends on it"* — and the record should show the invitation being taken
  up rather than the text quietly changing.

**Everything else is left alone, deliberately.** `PLAN-quiver.md`, `GATE-quiver.md`, `GATE-q7.md`,
`PLAN-boltor.md`, `PLAN-q7.md`, `PLAN-quiver-a2.md`, `GATE-quiver-a2.md` and `FireCadence.java` are
**dated records of past slices, not standing rules** — the same treatment `boltor.yml` gives its two
hitscan reversals. Rewriting them would delete the reasoning that made this ruling necessary.
`quiver_stone.yml` is in the deletion set. **This boundary goes in the commit body so the omission is
not read as an oversight.**

---

## `locust.yml` — SHAPE

Filename **is** the id. `class:` is required (missing or unknown -> file skipped and named).
`cooldown_ticks` lives **inside the trigger**, never at top level. The quiver is the **pair**
`quiver_size` + `reload_ticks`; one without the other is rejected, as is any `left_click` trigger on
a quiver weapon — left-click is the reload.

```yaml
id: locust
display_name: "Locust"
element: nature           # RULED. Accrues nothing -- nature declares no applies_status.
rarity: rare              # RULED. The Boltor goes to uncommon in the same commit; see the ordering.
class: ranger
material: crossbow        # wears AND empties; Broken is checked before Empty
attack_damage: 26         # a real MAIN_HAND stat, read back by weapon_damage
quiver_size: 12
reload_ticks: 60
flavor: [ ... ]           # must NOT quote a resolvable stat — see below
triggers:
  right_click:            # the only trigger
    cooldown_ticks: 12
    cast: { type: ray, range: 96, beam: locust_beam }
    on_hit: [ { type: weapon_damage, element: nature } ]
```

> **`element:` APPEARS TWICE AND BOTH MUST MOVE.** Measured: **9 of the 11** shipped weapons carry
> `element:` at top level **and** again indented inside `on_hit` — the Boltor's second one is in the
> `weapon_damage` entry under its `right_click` trigger. Found with
> `grep -cE "^[[:space:]]+element:"`, which is the check to re-run rather than a line to revisit.
> **A mismatched pair is exactly the split-subject defect this project keeps finding**, and nothing
> in the schema cross-checks the two.

- **`weapon_damage`, not a literal** — so a future "+N Ranged Damage" modifier has something to grip,
  and so `WeaponLore` renders a stat block rather than an ability section.
- **`flavor` must not quote `quiver_size`.** `boltor.yml`'s *"Eight bolts, then three seconds"* names
  a number `QuiverSize.resolve` can change per wielder. **A falsified comment misleads a reader who
  can check it; a falsified flavour line misleads a player who cannot.** The Locust's flavour names
  no resolvable stat, and says why it diverges from the Boltor's.

### `locust_beam.yml` — FORKED, AND THE INHERITED FIGURES SAY SO IN THOSE WORDS

The beam forks by ruling: *"make the beam color more dark green color to resemble nature."* It lives
at `content/visuals/locust_beam.yml`, id from filename, and `ContentValidator` resolves
`cast.beam` against the visual registry — a name that does not load is a named problem at boot.
`VolleyFixtureTest` asserts `visuals.all().size() >= 14`; there are **20** today, so a 21st is safe.

**THE COLOUR MUST SEPARATE FROM `emerald_beam`, NOT ONLY FROM `boltor_beam`.** The convention is
stated in `boltor_beam.yml` — *"the colour is owned by nothing else, checked against the three
shipped beams"* — and the check matters here because **one shipped beam is already green:**

```
  lapis_beam    [ 40,  90, 240]   saturated deep blue
  volley_beam   [230,  60, 230]   magenta
  emerald_beam  [ 40, 220,  90]   GREEN  <- the one to separate from
  boltor_beam   [200, 215, 235]   pale steel
```

A **dark** green is what the ruling asks for and is also what separates it: `emerald_beam` is a
bright mid-green, so the dominant channel is where the daylight is. The authored value is a choice
inside the ruling — flagged at the key, re-rulable, and it must be **judged on a client**, since
colour is the one property no unit test can check.

> ### THE DENSITY FIGURES ARE INHERITED AND UNJUDGED, AND THE FILE MUST SAY SO IN THOSE WORDS
>
> `samples_per_block`, `size` and the origin gap are **not a second opinion. They are the same
> unjudged figures**, copied from a file that says of itself *"nothing here has been judged yet; 4 is
> inherited."* **A number that has been copied once looks more settled than the number it was copied
> from**, which is the descent-launders failure this repo already records.
>
> **`GATE-boltor.md` row 2 is OPEN and now covers TWO files.** Row 2 must say so, or the day it is
> read someone judges one beam and believes both are settled.

**AND THE OVERLAP PARAGRAPH IS NOT COPIED — IT IS WEAPON-SPECIFIC ARITHMETIC.** `boltor_beam.yml`
says *"the Boltor fires every 16 ticks against a 5-10 tick flight"*, which stays true of the Boltor.
The Locust fires every 12 against the same flight, so it gets **its own figures**, derived here:

```
  per shot     (96 - 1.0) x 4 = 380 points        IDENTICAL -- same length, same inherited density

  sustained    Boltor  380 x (20/16) = 475 points/s
               Locust  380 x (20/12) = 633 points/s        +33.3%, exactly the cooldown ratio 16/12

  in flight    Boltor  gap 16t vs 5-10t walk  ->  margin 6t
               Locust  gap 12t vs 5-10t walk  ->  margin 2t
```

**The per-shot load is unchanged; the sustained load is a third higher.** So the Locust — not the
Boltor — is now the project's heaviest particle load, and row 2's *"nobody has looked at it yet"*
applies to it more strongly than to the weapon the row was written for.

**Power reads cleanly on 26.** `enchants/power.yml` is `class: ranger`, so it reaches this weapon.
`DamageNumberText` rounds with `Math.round`: plain **26**, I `27.3 -> "27"`, II `28.6 -> "29"`,
III `29.9 -> "30"` — four distinguishable values, as on the Boltor (19/20/21/22) and unlike
`hunters_bow` (6/6/7/7).

---

## COMMIT SPLIT

| # | commit | what |
|---|---|---|
| **1** | `PLAN-locust.md` | this file |
| **2** | the weapon, the rule, **and the Boltor's rarity** | `locust.yml`, **`visuals/locust_beam.yml`**, both `CLAUDE.md` rule sections, the pin's POINTER quote, `boltor.yml`'s two superseded notes, **`cursed_emerald.yml`'s parking supersession**, `GATE-boltor.md` (`### 3.` **and row 2's two-file scope**), `golden-lore.txt`. **One commit by requirement** — the counterexample and the correction must not be separable, **and the Boltor's rarity is the same golden regeneration; splitting it would mean regenerating twice** |
| **3** | the records | `NEXT.md`: the dead-zone trigger, the deferred muzzle-visual idea. `CLAUDE.md`: the **line-wrap** false-absence cause, and the two figure/prose seam rules below |

---

## VERIFICATION

- `./mvnw -pl core test` after each edit; `./mvnw clean package` as the final verify, suite quoted
  `core / storage / paper` and **re-read after the last file lands** — never from a mid-slice run.

### THE GOLDEN BASELINE, WITH ITS METHOD

`"renderings"` is `GoldenLoreTest.render()`'s `items` counter, and **it is not the count of `-- `
blocks.** Eight `items++` sites: one per weapon, per shield x bulwark, per armor piece, per
rarity x slot footer — **and one per `=== LORE LINES ===` entry, which emits no `-- ` prefix at
all.** Measured on this branch:

```
  grep -c "^-- " golden-lore.txt                                 ->  69
  "=== LORE LINES ===" entries (9 + 8 + 4 ArmorSlots, no "-- ")  ->  21
                                                                    ---
  the file's own tail: "=== 90 renderings ==="                       90     69 + 21, checks by addition
```

A weapon adds **one `-- ` block and one item**, so the count movement is **`69 -> 70` blocks and
`90 -> 91` renderings**, with a `-- locust` block sorted by id between `lapis_staff` and
`quiver_stone`.

> ### A COUNT CANNOT SEE A SUBSTITUTION, AND THE BOLTOR RULING JUST ADDED ONE
>
> **Both counts above are invariant under modification.** The rarity word is the tooltip footer and
> its **colour is keyed to the tier**, so the Boltor's block *changes* rather than the roster growing:
>
> ```
>   boltor today   "Rare Ranged Weapon"      color=#5555FF
>   boltor after   "Uncommon Ranged Weapon"  color=#55FF55
> ```
>
> `#55FF55` is **measured from the golden itself** — `grep -o '"Uncommon Ranged Weapon" color=#[0-9A-F]*'`
> over the current file — not assumed from the palette.

### THE NEW `locust` BLOCK DIFFERS IN TWO LINES, NOT THREE — AND THE THIRD WAS PREDICTED

Measured against the existing golden rather than predicted, which is what caught it:

```
  1  element line   "Nature" color=#55FF55        (today the Boltor renders "Kinetic" color=#FFFFFF)
  2  footer         "Rare Ranged Weapon" color=#5555FF
```

> **THE DAMAGE GLYPH NEVER REACHES THE TOOLTIP, SO THERE IS NO THIRD LINE.** It was expected that
> nature's `damage_symbol` would mark the Locust's damage lines. **It does not:**
>
> ```
>   grep -n "▲" golden-lore.txt                       ->  NO HITS, anywhere in the file
>   emberblade (fire) renders   "Melee Damage: " ["7" color=#FF5555]      -- a BARE number
> ```
>
> **Fire weapons already ship a `damage_symbol` and their tooltips carry no glyph.** `damage_symbol`
> is consumed by `DamageNumberText` — **floating combat text, not lore** — so it cannot appear in a
> `WeaponLore` dump. The rendering shape was observable on a shipped fire weapon, and reading it beat
> reasoning about it.

**`#55FF55` for `<green>` is DERIVED, not read off the golden** — no shipped weapon renders `nature`
yet. It is MiniMessage's named-colour table, corroborated twice inside this same file:
`<red>` -> `#FF5555` on fire, `<white>` -> `#FFFFFF` on kinetic. **The regeneration confirms or
refutes it**, and that is the check.
>
> **If the Boltor footer failed to regenerate, `69 -> 70` and `90 -> 91` would both still be exactly
> right and the check would pass.** Adding a row and changing a row are different events, and only
> one of them moves a total. So the count controls are **necessary and not sufficient**:
>
> - **Assert the content, not only the total:** the `-- boltor` block's footer must read
>   `"Uncommon Ranged Weapon" color=#55FF55`.
> - **The expected diff shape is an ADDITION PLUS A MODIFICATION.** A diff containing **only** an
>   addition is the **alarming** answer, not the expected one — decided here, before running it.

- Regenerate from `paper/`, because `Path.of("src/test/resources", GOLDEN)` is module-relative:
  `./mvnw -pl paper test -Dtest=GoldenLoreTest -Dgolden.regenerate=true`. It **fails by design** on
  the regenerating run; re-run without the property to verify.
  > **CHECK THE ARTEFACT, NOT THE INVOCATION.** `git status --porcelain` on the golden afterwards.
  > **A commit adding a weapon in which the golden is unmodified is the alarming answer** — this repo
  > has already recorded a regenerate that ran, did not fail, and wrote nothing.

### TESTS THAT SHOULD STAY GREEN WITHOUT EDITS — CHECKED, NOT ASSUMED

- `WeaponLoaderTest` copies every shipped `.yml` and requires **zero** unknown-key warnings: a free
  strictness check on the new file.
- `ScorchContentInvariantTest.KNOWN_FIRE_DAMAGE_SITES = 12` is **untouched, and the element ruling
  does not change that.** Its scan matches `element: fire` specifically, not "any element", so the
  Locust's two `element: nature` lines are invisible to it. The conclusion survived the ruling; the
  reason it survived did not, and the reason is what a later reader checks.
- `WeaponLoreTest`'s `assertEquals(3, ...)` is scoped to its own 5-id `@TempDir` list, not to shipped
  content.
- `VolleyFixtureTest`'s `>= 7` is a lower bound.

### REPORTING

- **Report the file list from `git diff --numstat` AND from what was touched, and RECONCILE them.**
  `PLAN-locust.md` is **new and untracked**, and invisible to `--numstat` by design — use
  `git status --porcelain` so an addition cannot hide inside a list of modifications.
- **No mutation work is owed.** This slice adds no Java logic, no assertion and no guard. Say so
  rather than reporting an empty mutation section.
- **Boot-only and NOT taken here:** the cadence confirmation at cooldown 12, with its trigger named
  above.

---

## TWO RULES FROM THIS PLAN'S OWN REVIEW — FILED TOGETHER IN COMMIT 3

**Same revision, same file, opposite directions.** Both sit at the seam between a figure and the
sentence about the figure, and each alone reads as a one-off.

- **A REVISION REGRESSES WHAT IT WAS NOT REVISING.** One draft stated the Locust's second step
  correctly at `s > 12/4.5 = 2.6667`. The next, revising a table one section away, rewrote that
  untouched sentence into `s > 12/0.5, unreachable` — **wrong in the value and wrong in the
  conclusion, from a draft that had the right number and a sweep that had printed it.**
  **Diff a revision against the draft it replaced, not only against the defect list: the defect list
  says what was wrong, never what was right and got touched anyway.**

- **A GENERAL FORM MUST REPRODUCE THE WORKED VALUES IT SITS ABOVE.** The next draft generalised the
  verified closed form to `s > n/(m - 0.5)`. The correct form is `m + 0.5`; the sign inverted while
  pattern-matching the `3.5` in `n/(n - 3.5)` instead of rederiving
  `round(n/s) <= m  iff  n/s < m + 0.5`. It ran **~13% high at every step**, which does not read as
  an error — **it reads as a weapon having more attack-speed headroom than it has, and it would be
  believed.**

  > **THE TELL WAS TOTAL, AND THAT IS THE DIAGNOSTIC.** The stated form disagreed with **every** row
  > printed beneath it. **A formula that is merely mis-stated usually matches somewhere by luck; one
  > that matches nowhere was never checked against its own data.** Substituting one row back costs
  > nothing, and it is the only thing separating a derived generalisation from a pattern-matched one.
  >
  > **The values are not the portable artefact — the formula is.** The rows are about two specific
  > weapons; the formula is what the next author reaches for when pricing the third.

---

## OPEN FOR THE OPERATOR — THE ONE PLAYER-VISIBLE SURPRISE, AND IT IS NOT IN THE TOOLTIP

**Every damage number the Locust deals will draw a green flower beside it.** `nature`'s
`damage_symbol` is `"<green>✿</green>"`; `kinetic`'s is `""`, which is why the Boltor's hits are
bare. This follows **from the element**, not from anything authored in `locust.yml`.

**It is a COMBAT-TEXT change, not a tooltip one** — which is why the golden shows only two moved
lines and why no test in the suite will show it. **It is visible only on a booted server**, and it is
put in front of the operator for that reason.

> **DO NOT AUTHOR A WORKAROUND.** If the element is wanted without the glyph, that is a different and
> larger question — **the symbol belongs to the element, not to the weapon**, so suppressing it for
> one weapon would mean either a new per-weapon override key or editing `nature.yml` for every future
> nature weapon. Neither is this slice's to decide.

## OUT OF SCOPE

- **The alternating muzzle visual — REFUSED**, and recorded in `NEXT.md` as a deferred idea rather
  than as scope: *"although it's a good idea for the future."*
- **Any enchantment.** The inert-tier property goes in front of the operator; the remedy is his.
- **The dev-weapon deletion.** Still parked on *enough shipped weapons to replace them*. The Locust
  is one more, and it **cites only the Boltor** — this slice creates no new references to the
  deletion set.
- **Re-ruling the Boltor's `cooldown_ticks: 16`.** Its *rarity* is re-ruled here by the operator and
  that is in scope; the halving derivation that produced 16 is superseded without 16 moving.
- The other weapons' rarities. `ember_staff` and `emberblade` keep `rare`, and the Locust joins them
  as the Boltor leaves, so the tier stays at **three**.
- **`cursed_emerald`'s own element.** Its parking premise is discharged; the weapon stays `kinetic`
  for its own separate, still-valid reason.
