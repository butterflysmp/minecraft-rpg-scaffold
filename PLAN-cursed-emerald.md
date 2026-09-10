# PLAN — Cursed Emerald (port from cfde822)

**Status: AUTHORED. The wrapper shipped as PR #63 (`ab0739a`) and all five content files landed
together against it. Four of the five open questions are closed; Q3 is narrowed and still open.**

> ### WHAT THE WRAPPER SLICE AND ITS GATE CHANGED IN THIS PLAN — read before the rest
>
> The grammar arrived **exactly as sketched** (`windup_ticks`, `shots`, `interval_ticks`, `of:`), so
> the YAML below parses as written. What moved is the numbers and the rulings:
>
> | | plan said | shipped as | why |
> |---|---|---|---|
> | `range` | **64** | **32** | **Operator ruling on a MEASUREMENT.** `GATE-volley.md` V3 measured that particles stop rendering at **32 blocks** — a client cap. 64 would draw half a beam while the ray still hit. The ruling is **visible reach equals real reach**. |
> | `cooldown_ticks` | **0**, guard-owned | **30** | The wrapper derives the floor and `max(authored, derived)` enforces it either way, so 30 is redundant *as a guard*. Authored anyway for two other reasons — the tooltip renders the authored value, and shipped content must not carry a permanent expected boot warning. |
> | the in-flight guard | a bespoke `ActiveCasts` set | **a derived cooldown floor** | The property was right and the remedy was not. See the corrected ruling section below. |
> | the per-shot chime | needs an `on_shot:` hook | **dropped** | Gate V6 ran the row and **refused the hook with evidence.** |
>
> **Q1, Q2, Q4 and Q5 are closed — do not re-litigate them.** Q3 is narrowed to two untested cases.

Reviewed against `origin/master` at `24f9796`. **That commit is Ignite's merge — master moved
during this exchange**, and every citation below has been re-checked there rather than carried
forward from `66bd8f9`, where the plan was first written. Nothing in Ignite touches this weapon
(`kinetic` declares no `applies_status`, so no scorch, no detonation) but the line numbers had to
be re-earned regardless.

Source read: `summon/weapons/CursedEmeraldWeapon.java`, `crafting/RecipeRegistry.registerCursedEmerald`,
`menu/lore/SwordLoreRenderer` (the `CURSED_EMERALD` arm), `util/BeamVisuals`, `item/ItemFactory:225`,
all at `cfde822`.

---

## WHAT THE REVIEW CHANGED — read this before the rest

1. **Q1 was two questions.** Re-aiming and re-rolling crit are separable, and the plan only asked
   about the first. **Operator ruling: each shot rolls its own crit** — a fresh `Caster` projection
   per shot, new dice, re-aimed from the live eye.
2. **CE2 could not be a single figure and has been restaged around crit colour.** Details in the
   gate section; the old row called a clean pass a failure.
3. **The cooldown was a coincidence, not a guard, and the wrapper now refuses to start while one
   volley is in flight.**
4. `attack_damage` is authored `0`, not omitted — the plan cited *ABSENCE IS NOT A NEUTRAL VALUE*
   and then broke it two sections later.
5. **Q2 stays open. It needs a measurement, not a reading.** Q4 and Q5 will be ruled with the
   wrapper's brief, since both depend on grammar that does not exist yet.

---

---

## THE MECHANISM, STRIPPED

One right-click. Mana paid upfront and never re-checked. A 20-tick wind-up with an audible
telegraph. Then **six rays, 2 ticks apart, each re-aimed from the caster's live eye**, each
damaging the first body in the line.

That is the whole weapon. Thirty ticks, six numbers, one button.

**The re-aim is not incidental.** `fireCursedEmeraldPulse` reads `player.getEyeLocation()` fresh
on every shot, so the player tracks a moving target across the burst. A version that freezes the
aim at cast is a different weapon that shares its numbers — see Q1.

---

## WHAT IS DELIBERATELY LEFT BEHIND

Each of these exists in `cfde822` and must **not** come across. Listed because every one of them
is a thing a reader could mistake for part of the weapon.

| left behind | why it is not the mechanism |
|---|---|
| `target.setNoDamageTicks(0)` on every shot | A workaround for vanilla's 10-tick iframes, which would otherwise eat 4 of the 6 shots. `EffectSpec.Damage` drains the custom health store and never calls `entity.damage()`, so there are no iframes to beat. |
| `suppressNext` / `clear` knockback bracketing | Same absent-remedy shape `lapis_staff.yml` already records: no `entity.damage()` means no `EntityKnockbackEvent` is ever raised. Do not add a canceller for an event this path cannot fire. |
| The `EnderCrystal` branch and its `createExplosion(pos, 6.0f, ...)` | Dragonfight encounter code. Bosses are milestone 3 and there is no crystal in this repo. |
| `EMERALD_BEAM` legacy alias + the self-migrating PDC rewrite in the lore renderer | Migration debt for a rename that never happened here. |
| `GearScore.scalePower(42, level)`, `getSwordLevel`, gear score | No item levels in this repo. This is why the damage figure had to be re-decided rather than ported. |
| `levelDamageBonus` + `getEquippedEyeOfEndermanDamageBonus` + `getEquippedArmorMagicDamageBonus`, summed into a shot-#1-only `castBonus` | Three stat systems that do not exist here, plus a first-pulse-only rule invented to stop them stacking six times. |
| `attunementDamageMult` per shot | This repo has an `attunement` enchant; whether it multiplies is the enchant's business, not the weapon's. |
| The `cursedEmeraldActive` UUID set | This is `cooldown_ticks`. See the cooldown row in *Numbers*. |
| `BeamVisuals.handLocation` (draw from the hand, not the eye) and `VISUAL_GAP = 1.0` | Two separate fixes for the same problem — the caster's own beam filling their view. This repo draws from `aim.origin()` with no gap. Recorded as a known divergence, not ported: see gate row **CE4** before deciding it needs a remedy. |
| The hand-built `§`-code lore block in `SwordLoreRenderer` | Tooltips are derived here. `flavor:` is the only authored prose. |

---

## THE BLOCKER — a new `CastSpec` kind, and it is the feature chat's

**Measured, not assumed.** `CastSpec` is sealed over five kinds:

```
$ grep -n "record " core/src/main/java/io/github/butterflysmp/rpg/core/ability/CastSpec.java
5:    record Self()
6:    record Melee(double reach, double arcDegrees)
24:    record Ray(double range, String beam)
55:    record Projectile(double speed, double gravity, int maxLifetimeTicks, String trail, String item)
103:   record Dash(double distance, double speed, double lift, DashDirection direction)
```

One cast is one ray. The only repeating primitive in the schema is `EffectSpec.Area`, which is a
lingering field at a **fixed point** applying `Targeted` effects to whoever is near it — it cannot
re-cast anything, and it cannot re-aim. So there is no way to author a wind-up plus a six-shot
volley today.

**NOTE WHAT IS *NOT* BEING ASKED FOR: no new `EffectSpec` kind.** The payload is `damage` +
`visual`, both shipped. This is a `CastSpec` addition — still sealed, still mechanism, still not
mine, but a narrower ask than the content boundary's usual trigger.

### The shape the wrapper has to express

Sketched as content so the requirement is concrete. The feature chat owns the actual grammar; if
they want a different one, the plan's content files change and nothing else does.

```yaml
cast:
  type: volley          # name is theirs to pick
  windup_ticks: 20
  shots: 6
  interval_ticks: 2
  of:
    type: ray
    range: 32          # SHIPPED AS 32, not the 64 this sketch asked for -- see the header table
    beam: emerald_beam
```

A wrapper that runs an inner cast N times on a clock, after a delay. Deliberately generic: a
burst-fire projectile weapon later reuses it, and a wrapper that only knows how to repeat rays
would have to be widened the first time anyone wants one.

---

## QUESTIONS FOR THE FEATURE CHAT

**Q1 — RULED. Fresh `Caster` projection per shot: re-aimed AND re-rolled.**
The plan asked only about the aim. `Caster:57` carries `critMultiplier` **frozen at cast**, and
`EffectApplier:137-138` feeds it to `HitDamage.dealt` for every `Damage` effect — so re-reading the
eye while keeping the `Caster` would have given six shots **one shared crit roll**. The operator
ruled six independent rolls.

**WHAT ELSE THAT RE-READS, WHICH IS THE PART WITH TEETH.** `EffectApplier:136-138` shows the
authored amount passing through `enchantDamagePercent`, `classDamageBonus`, `chargeScale` and
`critMultiplier` — all off the same projection. **So the volley's stats are no longer atomic.** A
player who swaps weapons mid-burst has shots 4–6 priced off the new weapon.

**This is what makes Q3 load-bearing.** Under a frozen caster the cancellation rule was tidiness.
Under this ruling it decides a number, and it must be answered in that light.

**Q3 — What cancels a burst in flight? NOW A PRICING QUESTION, NOT A HOUSEKEEPING ONE. See below.**

**Q2 — CLOSED BY THE RANGE RULING, NOT BY OBSERVATION, AND THE DISTINCTION IS THE POINT.**
`GATE-volley.md` V3 measured the client's particle cap at **32 blocks** and the range was ruled to
match. **At range 32 the question cannot arise**: the plan's worry was three or four rays in flight
down a 64-block line, and V3 established that the overlap it was written about happens *beyond
render distance* — so V3 itself **passed for a reason it was not testing.** With visible reach and
real reach equal, there is no far half in which rays can pile up unseen.

**IF THE CLIENT CAP EVER RISES, THIS REOPENS.** Q2 is answered only under the cap, and a longer
range would restore exactly the unwatched condition the plan named.

*The original question, kept because it states what had to be measured:*
**Do we actually need the walking ray here, or is true hitscan available?**
**The reviewer's answer: this cannot be settled from the code — it needs a measurement.**
`stepRay`'s comment records the operator's reasoning for accepting the walk, and it is sound *for
the case it was written against*: one shot, 26 blocks, *"about a tick"* to the caster and *"about
5 ticks"* to an observer. **The Cursed Emerald breaks all three premises at once** — 64 blocks is
up to four chunk-column segments, and shots land every 2 ticks, so **up to three rays are in
flight simultaneously**, each drawing its own beam, from a muzzle that may have moved between
them. Nobody has watched that. The recorded reasoning does not cover it and should not be read
as though it does.

The counter-argument is in the same comment and is unchanged: *"making the ray hitscan to remove
that delay would reintroduce the Folia region problem the chunk-column walk exists to prevent."*
So the question is not "is hitscan nicer" but **"is there a hitscan that stays inside one region,
or does the volley have to be authored around the walk?"**

**Q3 — What cancels a burst in flight? RAISED IN WEIGHT BY Q1'S RULING.**
`cfde822` aborts on: weapon swapped out of main hand, dropped, player offline, player dead, weapon
broken. Two of those are gone here — the emerald cannot break — and one cuts against a standing
decision: this repo deliberately lets a grenade **outlive its thrower's logout** rather than pin a
Bukkit entity.

**Under a per-shot projection this is no longer housekeeping.** A player who swaps to a stronger
weapon after shot 3 has shots 4–6 priced off it, so "does a swap cancel the burst" and "what do
the remaining shots hit for" are the same question. **Answer it as a pricing rule, not a cleanup
rule** — and note that the *only* reason it looks like a design choice rather than an exploit is
that PvP is out of scope.

**Q4 and Q5 — BOTH RULED WITH THE WRAPPER'S GATE. Kept below unchanged, as the statement of what
had to be ruled.**

> **Q4 — RULED: `on_cast` FIRES ONCE PER VOLLEY, AND `on_shot` IS NOT COMING. THE ANSWER WAS A
> DELETION.** `GATE-volley.md` V6 asked whether shots are countable *without* reading the damage
> numbers. They are — `on_hit` fires per shot and the impact visual carries the count on its own —
> so the hook the plan asked for is unnecessary. **`on_cast` admits VISUALS ONLY**; `AbilitySchema`
> throws by name for any other effect type there.
>
> **So cfde822's per-shot chime (0.4 / 1.6) has no home and is DROPPED, not deferred.** Recorded as
> **refused with evidence**: the row that would have justified the hook was run and said the hook is
> unnecessary. That is a different thing from a gap, and the file says so at the field.
>
> **Q5 — RULED: INHERIT THE MISS BEHAVIOUR, NO CHANGE.** `stepRay`'s detonate-on-miss is shared by
> **every** ray weapon; changing it from inside a content slice would alter the Lapis Staff from a
> commit whose title says *"emerald"*.
>
> **AND THE PLAN'S REASONING FOR THIS ROW IS RE-DERIVED RATHER THAN CARRIED FORWARD, BECAUSE THE
> NUMBER MOVED UNDER IT.** The note below says *"six mid-air bursts at 64 blocks"* and concluded the
> caster would certainly not see them. **At range 32 they land AT THE CAP EDGE — marginally visible
> rather than certainly invisible**, which is neither the plan's answer nor its opposite. New gate
> row **CE8** takes the observation; the old reasoning is not evidence for the new range.

**Q4 — Does the wrapper fire `on_cast` once, or once per shot?**
`cfde822` plays two distinct sounds: one chime at wind-up start (`0.6`/`0.7`) and one per shot
(`0.4`/`1.6`). The per-shot one has nowhere to live under a once-per-cast `on_cast`, and it cannot
go in the beam visual — `ContentValidator:434` already warns that `presentAlong` runs **once per
chunk-column segment**, so a sound in a beam fires several times per shot. If the answer is "once",
I need somewhere else to put the shot chime and will ask for it.

**Q5 — Should a missed shot detonate?**
`stepRay` calls `detonate(..., null, to)` on a clean miss, which is why `lapis_impact.yml` records
that it *"plays on a MISS too... bursts 26 blocks out in mid-air."* At six shots that becomes six
mid-air bursts at 64 blocks whenever the player misses. `cfde822` drew impact particles **only on
a real target** while playing the shot chime unconditionally, so this is a genuine behaviour
divergence rather than an inherited quirk.

---

## THE CONTENT — five files, authored after the wrapper exists

### `content/weapons/cursed_emerald.yml`

```yaml
id: cursed_emerald
display_name: "Cursed Emerald"
element: kinetic
rarity: uncommon
class: mage
material: emerald
attack_damage: 0           # explicit — see the note below; it is NOT a placeholder

flavor:
  - "Nine facets, and every one of them looking back."
  - "It asks for a moment before it answers."

triggers:
  right_click:
    name: "Cursed Volley"
    description:
      - "Channel, then loose six"
      - "bolts down your line of sight."
    cooldown_ticks: 30     # SHIPPED AS 30, not the 0 this sketch asked for. It equals the derived
                           # floor (20 + 5x2). Redundant as a guard -- max(authored, derived)
                           # enforces it either way -- and authored for the tooltip and to avoid a
                           # permanent boot warning. See the corrected ruling below.
    cost:
      resource: mana
      amount: 40           # PLACEHOLDER, re-decided from CE5's observation
    on_cast:
      - { type: visual, visual_id: emerald_windup }
    cast:
      type: volley
      windup_ticks: 20
      shots: 6
      interval_ticks: 2
      of:
        type: ray
        range: 32          # SHIPPED AS 32 -- particles cap at 32 blocks (GATE-volley.md V3)
        beam: emerald_beam
    on_hit:
      - { type: visual, visual_id: emerald_impact }
      - { type: damage, amount: 27, element: kinetic }
```

Comments to carry in the file, each of which is a thing a later reader would otherwise re-derive
wrongly:

- **`attack_damage: 0` IS AUTHORED, AND THE FIRST DRAFT OF THIS PLAN GOT IT WRONG.** It read
  `<omitted>` — and `WeaponLoader:88` is `s.getDouble("attack_damage", 0.0)`, so the absence
  resolves to exactly the value the omission meant. **Which is the trap:** it works, it is
  invisible, and it breaks the rule this same file cites two sections down. There is no basic
  attack here; the volley is the whole weapon, and that is a decision, so it is written.
- **`cooldown_ticks: 0` IS ALSO AUTHORED, FOR THE SAME REASON AND WITH A SHARPER ONE BEHIND IT.**
  Both loaders default it to `0` (`WeaponLoader:140`, `AbilityLoader:62`), so absence and intent
  again resolve alike. **The re-press guard is the wrapper's job, not this field's** — see the
  cooldown ruling below. A reader finding `0` here must not conclude the weapon is unguarded.

  *If the operator overrules and prefers a cooldown instead of the guard*, this becomes
  `cooldown_ticks: 30` with the derivation written **at the field** — 20 wind-up + 5 × 2 interval —
  and a test pinning `cooldown_ticks >= windup_ticks + (shots - 1) * interval_ticks`. **Not the
  bare 30 the first draft carried.**

- **`element: kinetic` on a weapon everyone will call magic.** `cfde822` typed it
  `DamageType.MAGIC` and advertised *"Magic Damage"* in its lore. That is a damage-source
  **category**, not an element, and this registry has no arcane or magic among its seven. This is
  the *identical* decision `lapis_staff.yml` records and it is being made the same way and for the
  same reason. **`nature` was the obvious reach — green stone, green beam — and is refused:**
  elements are parked, and `element:` is not cosmetic (it drives the glyph and, for a
  status-declaring element, accrual). Kinetic declares no `applies_status`, so **this weapon does
  not scorch and its kills do not ignite.** That is deliberate.
- **Kinetic's `damage_symbol` is `""` — deliberately unmarked.** So the volley draws **six bare
  numbers, two ticks apart, at nearly the same point.** Nothing in the repo has ever produced that
  and nobody has looked at it. Gate row **CE3**.
- **The emerald never breaks.** No durability, so `Durability.isBroken`'s
  `if (maxDurability <= 0) return false` staff-and-stone exemption applies. `cfde822`'s version
  *could* break — `ItemRegistry.isBroken` gated the cast — so this is a real change, confirmed by
  the operator. **Third craftable indestructible weapon**, after `flint_staff` and `lapis_staff`.
  The material is the lever; do not "fix" it by requiring durability.
- **No `craft_result`, and do not add one.** It claims a MATERIAL, and this weapon's material is
  `emerald` — the claim would mint a Cursed Emerald from every vanilla emerald on the server.
  `ContentValidator.validateCraftResults` would refuse it anyway (no durability). Same note
  `flint_staff.yml` and `lapis_staff.yml` carry.

### `content/recipes/cursed_emerald.yml`

```yaml
mints: cursed_emerald

shape:
  - "ENE"
  - "NEN"
  - "ENE"

ingredients:
  E: emerald
  N: netherite_scrap
```

`cfde822`'s shape unchanged (`RecipeRegistry:180-183`). Three rows, so it cannot fit a 2×2 and is
safe from the inventory-grid hole **by shape rather than by guard**, exactly as both staves are.

### `content/visuals/emerald_windup.yml`

Sound only. `BLOCK_AMETHYST_BLOCK_CHIME`, volume `0.6`, pitch `0.7` — `cfde822`'s telegraph.
**The key must be verified against the pinned jar before this ships**, by the method
`lapis_cast.yml` records, not from memory:

```
unzip -p run/versions/<ver>/paper-<ver>.jar net/minecraft/sounds/SoundEvents.class \
  | grep -a -o 'block\.amethyst[a-z_.]*' | sort -u
```

### `content/visuals/emerald_beam.yml`

`DUST`, colour `[40, 220, 90]` (`Color.fromRGB(40, 220, 90)`), size `1.0`, `count: 1`,
`spread: 0.0`, `speed: 0.0`, `samples_per_block: 4`.

The `4` is exact, not eyeballed: `BeamVisuals.drawTrail`'s `density` parameter is documented
*"particles per block"* and the call site passes `4`. Same figure `lapis_beam.yml` landed on
independently.

**Particle-only, no sound step** — see Q4.

**And any density figure recorded for this file must name the client Particles setting it was
judged on**, per `lapis_beam.yml`'s rule that a density without a setting is a measurement missing
half its units.

### `content/visuals/emerald_impact.yml`

`DUST` at the same colour, `count: 8`, `spread: 0.25`, `speed: 0.0` (inert for DUST, authored so
its absence is not read as a choice); then `HAPPY_VILLAGER`, `count: 4`, `spread: 0.2`,
`speed: 0.0`.

**Its own file, not shared with `lapis_impact`.** A shared visual is a coupling that is invisible
at both ends — the trap `ember_burst` sprang on the Flint Staff.

Where the per-shot chime (`0.4`/`1.6`) lives depends on Q4. If `on_cast` fires per shot it goes
there; if it goes here it also plays on every miss (Q5).

---

## THE COOLDOWN RULING — a guard, because 30 was a coincidence

**The first draft of this plan proposed `cooldown_ticks: 30`, derived as 20 wind-up + 5 × 2
interval, and called it a faithful reproduction of `cfde822`'s `cursedEmeraldActive` set. The
review refused it, and the refusal is right.**

`cfde822` had an explicit set — a real guard, asking "is one running?". `cooldown_ticks: 30`
reproduces that behaviour **only while two independently-computed durations stay exactly equal.**

Fire shot 1 at `windup + interval` rather than at `windup`, and the volley ends at t=32 while the
cooldown expires at t=30. **Two volleys overlap — twelve shots interleaved, 80 mana.** Nothing
reddens: no test knows the two numbers are meant to be equal, and `windup_ticks` and
`cooldown_ticks` sit in one file with no stated relationship between them. **Anyone tuning the
wind-up breaks it, silently, and the breakage is a damage figure rather than a crash.**

**RULING AS WRITTEN: the wrapper refuses to start while one volley is in flight.** A real guard,
which is what the source had. This is the same species as `AN INVARIANT ONLY HOLDS WHILE THE THING
THAT MAKES IT HOLD IS STATED` — the derivation existed only in a plan document, and a plan is not a
mechanism.

> ### WHAT SHIPPED — THE PROPERTY WAS RIGHT AND THE REMEDY WAS NOT
>
> **The diagnosis above is correct and is why the plan's `cooldown_ticks: 30` was refused.** The
> remedy is not what was built. An `ActiveCasts` set is new mutable state with a lifecycle and every
> abort route to clean up — a guard that can **leak**.
>
> **`CastSpec.Volley` derives the floor from the three numbers it already owns**, and
> `AbilityService` stamps `max(authored, derived)` **last**. No new state, no lifecycle, no leak
> surface, and **the two durations cannot drift apart because there is only one of them.** A
> derivation that cannot drift beats a guard that can leak — strictly better, not merely smaller.
>
> **This is the named instance of `STATE THE PROPERTY; LET THE REMEDY BE CHOSEN AGAINST THE
> MATERIAL`** (`NEXT.md`). The property — *these two durations cannot drift apart* — is the durable
> half and survived being satisfied a different way. The remedy prescribed alongside it was a design
> decision made before anyone had looked at the material.
>
> **AND THE CONSEQUENCE FOR THIS FILE'S OTHER RECOMMENDATION:** the plan then said to author
> `cooldown_ticks: 0` because *"the guard is the wrapper's"*. **That is now wrong for a reason the
> plan could not have known**, and the weapon ships **30**. Authoring below the floor emits a boot
> warning by design — `volley_stone` authors 0 *deliberately*, because it is a test instrument and
> that warning is its witness — and **shipped content must not carry a permanent expected warning**.
> The tooltip renders the authored value, so 0 would also print no cooldown line on a weapon whose
> real guard is 30.

---

## NUMBERS, AND WHERE EACH ONE CAME FROM

**Nothing in this table is mine.** Two rows are the operator's from this session, one is derived
arithmetic, the rest are `cfde822`'s.

| value | figure | source |
|---|---|---|
| damage per shot | **27** | **Operator, this session.** NOT `cfde822`, which was `scalePower(42, level)` — 42 at L1, 100 at L50 — on a gear-score curve with nothing to hang it on here. |
| range | **32** — *was 64 in this plan* | **Operator ruling, on GATE-volley.md V3's MEASUREMENT** — particles stop rendering at 32 blocks, so visible reach now equals real reach. NOT `cfde822`'s `132.0`, which was matched to a weapon that does not exist in this repo. |
| shots | 6 | `CURSED_EMERALD_SHOT_COUNT` |
| interval | 2 ticks | `CURSED_EMERALD_SHOT_INTERVAL` |
| wind-up | 20 ticks | `CURSED_EMERALD_WINDUP_TICKS` |
| mana | 40 | `CURSED_EMERALD_MANA_COST`. **The one number nobody re-decided — see below.** |
| cooldown | **30 authored, equal to the derived floor** | **The review refused a bare `30` and was right**; what shipped is the wrapper deriving the floor itself, with `max(authored, derived)` applied last. The 30 in the file is for the TOOLTIP and to avoid a permanent boot warning — not as the guard. See the corrected ruling section. |
| rarity | uncommon | class javadoc, *"Uncommon, L1-50"* |
| material | `emerald` | `ItemFactory:225` |
| element | kinetic | Operator, this session |

> ### MEASURED 2026-09-10: PARTICLES STOP RENDERING AT **32 BLOCKS**. `range: 64` DRAWS HALF A BEAM.
>
> From `GATE-volley.md` **V3**, on a live boot — the first time anything in this repo has actually
> produced the figure, replacing `stepRay`'s *"about 30 blocks"* estimate with a measurement. It is a
> **client** cap, not server config, so no setting on our side moves it.
>
> **The Cursed Emerald's beam will stop halfway to a target it still damages.** The ray hits at 64;
> the visual reaches 32. That is not a bug in the volley wrapper and nothing in the slice can fix it.
>
> **THIS IS A DECISION THIS PLAN OWES, AND IT IS CHEAPER NOW THAN AFTER AUTHORING.** Two ways to
> settle it, and either is fine — what is not fine is `range: 64` shipping with nobody having known:
>
> - **Match the range to the visible reach** (~32), so the beam ends where it lands; or
> - **Keep 64 as a recorded decision** — the weapon deliberately outranges its own visual, the beam
>   reads as a direction rather than a full path. Write that down *here*, or the next reader files it
>   as a rendering bug and goes looking for a fix that does not exist.
>
> Note this interacts with **Q5** and the mid-air-burst question above: at 64 a clean miss detonates
> *beyond* the render cap, so the miss has no impact visual either — the beam is the only cue, and
> only its near half is drawn.

### What 27 × 6 actually does, so the figure is checkable rather than a vibe

**162 is the FLOOR, not the expectation.** Under the per-shot ruling every shot carries an
independent 15% roll at ×2 (`Crit.BASE_CHANCE` 0.15, `Crit.multiplier(0.15, 1.0, 0.10) -> 2.0`),
so a cast deals `27 × (6 + crits)` for crits in 0…6:

```
  162 · 189 · 216 · 243 · 270 · 297 · 324        (6 shots, 0..6 crits)
```

**Six shots at 15% averages 0.9 crits**, so ~189 is as ordinary an outcome as 162 and neither is
a defect. Everything below uses 162 to stay conservative and comparable.

- Against `knell` (360 HP, the only mob any gate figure is derived against): **three casts**, or
  two plus change on a lucky roll.
- Against a vanilla 20 HP mob: **shot one kills it**, and shots two through six carry on down the
  line to whatever is behind, or miss and detonate at **32** blocks (Q5 — ruled; re-derived for the
  new range, see CE8).
- Beside the shipped roster: `flint_staff` is 20 per shot at 5 mana / 24 ticks, `lapis_staff` 21 at
  10 mana / 20 ticks. So this is **~5.4× the per-cycle damage of the Lapis Staff for 4× the mana**,
  on a 50% longer cycle, with a 1-second commitment before anything happens.

### The mana figure is the loose end, and it is named rather than quietly kept

40 mana per 30 ticks is **26.7 mana/second sustained** against a 100 mana pool — the pool empties in
under four seconds and supports two and a half casts. `cfde822`'s cost was set against its own
regen, its own pool and its own gear, none of which came across.

Per the standing rule that a tuning request against a system nobody has watched encodes a guess as
a requirement: **40 ships as a placeholder and gets re-decided from gate row CE5's observation**,
not from this document.

---

## GATE ROWS — `GATE-cursed-emerald.md`

Written before the boot, not after, and none of them is a tick-box.

- **CE1 — the wind-up is visible as a wind-up.** Press, and expect a chime, then **a full second of
  nothing**, then the volley. *An instant first shot means `windup_ticks` was dropped*, and the
  telegraph is the entire reason the weapon has a commitment cost.
- **CE2 — six shots land, and the crits are countable.**
  `/rpg spawn knell`, hold still, one cast. **COUNT THE DAMAGE NUMBERS, AND COUNT THE YELLOW ONES.**
  Expect **six numbers**, each `27` (white) or `54` (yellow). The knell then reads
  `360 − 27 × (6 + yellows)`. **Expect about one yellow per cast** — six shots at 15% averages 0.9.

  **FEWER THAN SIX NUMBERS IS THE DEFECT.** The final HP figure alone cannot tell a missing shot
  from a crit; the **count** can, and the colour explains any figure the count does not.

  *Why the first draft's version of this row was wrong, kept because the failure is instructive:*
  it read **"expect 198"**, which is right only 38% of the time, and named `27, 54, 135` as its
  failure vocabulary. **198's near neighbours are clean passes** — 171 is six shots with one crit,
  and would have been filed as shots being eaten. **135 is not reachable at all** with six shots
  (`225 / 27` is not an integer). The row was wrong in both directions at once. *A gate row that
  names an exact figure against a 15% coin flip that cannot be disabled is not a gate row, and this
  repo has already shipped one that did not say so.*

  **PRECONDITIONS, WHICH THE FIGURES SILENTLY ASSUME.** `EffectApplier:136-138` passes the authored
  amount through `enchantDamagePercent`, `classDamageBonus` and `chargeScale` **before** crit. So
  `27` and `54` hold only for **an unenchanted emerald, on a player carrying no class damage bonus
  and no charge scaling.** Run it clean, or every number on this row is off by a multiplier and
  reads exactly like a broken volley.

  **WHY THIS ROW IS READABLE AT ALL: `DamageNumberText.of` renders a crit YELLOW and a normal hit
  WHITE**, and kinetic's `damage_symbol` is `""`, so the numbers are **bare** — colour is the only
  signal on them, with no glyph competing for the eye. **The element choice is what makes this
  gate row work.** Had the element been fire, six gold `▲` marks would sit beside six numbers whose
  only distinction is also a warm colour.

- **CE3 — read the six damage numbers. figure — write down what you saw.**
  **Run this as the SAME staging as CE2, one cast, two questions** — both are "read the six
  numbers." It keeps its own row because it asks what CE2 does not: whether six numbers two ticks
  apart at one point are **legible at all**. Do they read as six, as a blur, or as one number
  flickering? **This row is presentation's only witness**, and the answer decides whether the volley
  needs a stagger, an offset, or nothing.

  **THE 2-TICK SPACING IS A PROPERTY OF THE STAGING, NOT OF THE WEAPON.** `stepRay` walks chunk
  columns, so resolution scales with **distance** — a near shot can land before an earlier far one.
  At one fixed target the premise holds. **Sweeping across near and far bodies, the numbers arrive
  out of firing order**, and "I only saw five" gets filed as a defect when it was a reordering.
  Run CE3 against a single stationary target, and if you sweep, say so.
- **CE4 — does your own beam blind you?** `cfde822` solved this twice over (draw from the hand,
  skip the first block) and neither fix came across. Fire at a wall from **3 blocks and from 30**.
  *If the answer is "no", the two fixes were solving a problem this repo's geometry does not have,
  and that is worth writing down as much as a yes is.*

  > **THE DISTANCES MOVED WITH THE RANGE RULING.** This row read *"from 3 blocks and from 40"*, and
  > **40 is no longer a meaningful staging** — it is past the weapon's whole reach. The near figure
  > is what this row actually tests (your own beam filling your view is a near-muzzle problem); the
  > far one is now 30, just inside both the range and the particle cap, so the beam is drawn along
  > its entire length.
- **CE5 — mana. figure.** Cast until dry from a full bar; record how many casts and how long the
  refill takes. **This is the observation the 40 gets re-decided from.**
- **CE6 — track a moving target through the burst.** Q1 landed as re-aim, **so this row is live and
  not conditional.** Strafe a knell mid-volley: do later shots follow, and does the beam originate
  from where you are now or where you were?
- **CE7 — swap weapons mid-burst. figure.** Start a volley, switch to a different weapon before
  shot 4. **Whatever shots 4–6 hit for is the answer to Q3 arriving as an observation.** *Under a
  per-shot projection this cannot be checked by reading the code — the projection reads whatever
  is in the hand at that tick. Record the numbers before anyone rules on cancellation.*

  > **AND KNOW BEFORE RUNNING IT THAT THIS ROW CANNOT MOVE, SO A PASS PROVES ALMOST NOTHING.**
  > `GATE-volley.md` V7 ran exactly this staging on `volley_stone` and got *"same damage"* — **forced
  > by the fixture**, because an authored `amount:` is captured in the walker's closure and no swap
  > can reach it. **This weapon authors `amount: 27`, so it is the same shape**, and a swap will not
  > move its numbers either. *A row that could not have come out any other way is not evidence.*
  >
  > **Run it anyway** — it confirms the burst is not cancelled and that shots 4–6 still land — but
  > **record it as "unchanged, as forced" rather than as Q3 answered.** The pricing risk lives in a
  > `weapon_damage` payload or a swap to an **enchanted** weapon; neither exists here and neither is
  > tested.

- **CE8 — NEW. Where does a MISSED volley burst, now that the range is 32? figure.**
  Fire at open sky and **write down whether you can see the impacts at all.**

  **THIS ROW EXISTS BECAUSE THE NUMBER MOVED UNDER THE REASONING.** The plan's Q5 note argued from
  `range: 64` that six mid-air bursts would be **certainly invisible** to the caster, and that was
  the whole basis for calling the inherited miss behaviour acceptable. **At 32 the bursts land AT
  the particle cap** — marginally visible rather than certainly invisible. That is neither the
  plan's answer nor its opposite, and **re-deriving beats carrying the old conclusion forward with
  its premise changed.**

  *If six bursts at the cap edge read as noise, the remedy is a content one (thin `emerald_impact`)
  before it is ever a mechanism one — and changing `stepRay`'s detonate-on-miss stays out of a
  content slice regardless, because it is shared by every ray weapon.*

---

## SEQUENCING — THIS IS THE PART THAT DECIDES WHAT RUNS TODAY

**The reviewer's instruction: do not author the five content files until the wrapper ships.** A
content file written against a grammar that then changes is the collision the boundary exists to
prevent. The wrapper is **a slice, not a bolt-on** — a new kind on a sealed interface, a repeating
clock in core, a fresh `Caster` projection per shot on the caster's own region thread, and Q2's
unwatched question about three rays in flight at 64 blocks. It gets its own brief and its own gate
first.

**One correction to the shape of that instruction, since it affects what a Claude Code session
could be handed.** Only `weapons/cursed_emerald.yml` carries the volley grammar; the recipe and the
three visuals are grammar-independent and could in principle land early. **They should not**, and
the reason is stronger than the one given:

- `RecipeRegistrar:150-157` — a recipe whose `mints` cannot be resolved is **warned about and then
  DROPPED, not registered.** Shipping `recipes/cursed_emerald.yml` before the weapon exists puts a
  dead file and a boot warning in the tree for however long the wrapper takes.
- A visual referenced by nothing is worse, because it is **silent**. `ContentValidator` walks
  references outward — content naming a target that does not exist — and has no orphan check, so
  three unreferenced visuals would sit in the tree invisible at boot forever.

**So: all five files land together, after the wrapper. Nothing here is runnable yet.**

> ### DONE, AND THE SEQUENCING HELD EXACTLY AS WRITTEN.
>
> The wrapper shipped first as its own slice with its own fixture and its own gate (PR #63,
> `ab0739a`), and **all five files landed together afterwards**, against a grammar that existed and
> had been watched. Neither the recipe nor the visuals went early, so the tree never carried a
> dropped recipe with a boot warning, nor three orphan visuals that `ContentValidator` has no check
> for.
>
> **This is the worked example that the content/mechanism split holds under load:** a content plan
> named a blocker it could not build, the blocker became a slice with its own gate, three findings
> came out of running that gate, and one of them (`range: 64` → `32`) came back and **changed a
> number in this document before it was ever authored.** The alternative — authoring against a
> sketched grammar — would have put `range: 64` in the tree and the measurement would have arrived
> as a bug report.

---

## BOUNDARY

The content in this plan touches `content/**.yml` only and needs **no new `EffectSpec` kind**. The
`CastSpec` wrapper, the re-aim decision (Q1), the hitscan question (Q2) and the cancellation rule
(Q3) are all mechanism and all the feature chat's. If any content file here ends up in the same
branch as a `core/` change, something crossed the line.
