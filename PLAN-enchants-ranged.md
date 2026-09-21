# PLAN — Slice D: Expanded Quiver (Punch designed and parked)

**Status: EXPANDED QUIVER SHIPPED 2026-09-13 on `feat/expanded-quiver`. PUNCH DESIGNED AND PARKED.
Four mutations run, all four guarded — two of them only after this plan's own forecast was measured
and found wrong. Nothing has been BOOTED.** This document is the investigation, the design, and now
the result. Every claim that is a fact about this tree names the file it was read from; every claim
that is a ruling says whose.

> **THIS MASTHEAD SAID `NOT IMPLEMENTED. No Java written, no test written, no mutation owed.` UNTIL
> THE SLICE WAS ALREADY COMMITTED AND ON THE WIRE.** §5 was updated with the shipped result and the
> masthead was not, so the document carried **two answers** and the stale one came first.
>
> > **A CORRECTION THAT DOES NOT DELETE WHAT IT CORRECTS LEAVES TWO ANSWERS IN ONE DOCUMENT, AND THE
> > READER TAKES WHICHEVER THEY REACH FIRST.** Correcting a claim means **removing** it, not
> > preceding it with a better one. A status line is the worst case: it is read first and it is the
> > line least likely to be re-read while editing a section four screens down.

> ## SLICE D SHIPS EXPANDED QUIVER ONLY
>
> **Operator ruling, 2026-09-13.** Rapid Fire is **PARKED, design incomplete**. Punch is **PARKED,
> DESIGN COMPLETE** — it is finished, and §4 is what its trigger fires. The Punch sections below are
> **kept deliberately**: they are the design, not a record of a dead end.
>
> **Punch was parked for TESTABILITY, not for doubt.** No weapon in content authors knockback, so
> Punch has no eligible weapon — see §4.5. Shipping it would ship untestable behaviour that would be
> believed to work.

---

## 0. THE RULE THE SLICE PRODUCED, AND IT IS THREE FOR THREE

> **A PERCENTAGE NEEDS A QUANTITY TO MULTIPLY. NAME THE QUANTITY, AND CHECK THAT IT IS NON-ZERO AND
> CONTINUOUS, BEFORE AUTHORING A CURVE.**

| enchant | the quantity | why the percentage fails | disposition |
|---|---|---|---|
| **Rapid Fire** | a **QUANTISED** cooldown | the 4-tick input grid swallows anything under **+28%** | **PARKED** |
| **Expanded Quiver** | an **INTEGER** magazine | `QuiverSize.arrows` floors; one arrow is 11% at 9 rounds, so every tier below that grants **nothing** | **RULED FLAT** |
| **Punch** | a knockback base that **DOES NOT EXIST** *on the weapons it was aimed at* | `applyDamage` raises no vanilla event, so a **ray** hit's base push is **0.0** | **RESOLVED — numbers unchanged, PARKED on a weapon** |

**The failure is identical in all three and it is not weakness — it is ZERO.** A tooltip advertises a
number and the player receives nothing. The three failures are *independent*: quantisation, flooring,
and an absent base. One question catches all three, and it is cheaper than any of the three
investigations that found them separately.

**Recorded in `CLAUDE.md`** under *"A PERCENTAGE NEEDS A QUANTITY TO MULTIPLY"*.

### AND THE PATTERN BROKE AT THREE, WHICH IS THE MORE USEFUL HALF

**The slice was three-for-three until the operator supplied the missing quantity.** Punch's base
exists the moment a weapon authors one (§4.5), and **+20/40/60% of an authored `strength` is a real
percentage of a real value.**

> **PUNCH IS THE ONLY ONE OF THE THREE WHOSE NUMBERS SHIP AS FIRST RULED. THE FIX WAS NOT A DIFFERENT
> CURVE — IT WAS NAMING THE QUANTITY THE PERCENTAGE MULTIPLIES.**

Rapid Fire and Expanded Quiver were repaired by **changing the arithmetic** — park it, or go flat.
Punch was repaired by **supplying the multiplicand**, and its curve never moved.

**So the rule is *name the quantity and check it*, in that order — it is NOT "prefer flat".** Two of
three failures did need flat, and a reader who took the rule as a preference for flat would have
renumbered a curve that was already correct. **The check is identical in both outcomes, and that is
the point: you cannot tell which one you are in until you have named the quantity and gone and looked
at it.**

---

## 1. FIRST JOB, ANSWERED: THE QUIVER PIPELINE IS **ITEM-ONLY**

**Reported as asked, and the answer is the less convenient one.**

`QuiverSizeModifierItems.desiredModifiers` (`paper/health/QuiverSizeModifierItems.java`) walks
`EquipmentSlot.values()` and reads **one PDC key** off each stack:

```java
Double bonus = boostAmount(equipment.getItem(slot), keys.quiverSizeBoost);
```

`boostAmount` reads `PersistentDataType.DOUBLE` under `keys.quiverSizeBoost` and nothing else. **It
never touches `EnchantItems.read`, `EnchantValues.totalFor` or `EnchantRegistry`.** There is no
enchant surface on this path at all.

**So Expanded Quiver is NOT "a constant, a gate arm, a lore line and a yml".** It needs a new
scanner. The brief's optimistic branch does not hold, and I am saying so rather than assuming either
way.

### The template is exact, though, and it is the SIBLING file — not this one

`GrowthModifierItems.desiredModifiers` is the enchant-reading twin of the item-reading scanner
above, and it is four lines:

```java
double bonus = EnchantValues.totalFor(
        EnchantItems.read(equipment.getItem(slot), keys), enchants,
        EnchantEffect.MAX_HEALTH);
if (Growth.boosts(bonus)) {
    desired.put(SOURCE_PREFIX + slot.name(), Growth.contribution(bonus));
}
```

**That is the whole of the enchant-to-modifier bridge**, and it already exists. Expanded Quiver's
scanner is this file with `MAX_HEALTH` swapped for the new constant, `Growth` for `QuiverSize`, and
the slot loop narrowed to the main hand (§3.3).

### AND THE MERGE TRAP, WHICH THE TARGET FILE PREDICTED IN WRITING

`QuiverSizeModifierItems`'s own javadoc, under *"It reconciles alone, for now"*:

> **The moment a second source lands it must be MERGED here rather than reconciled separately**:
> `ModifierReconciler` removes every source absent from the map it is handed, so two calls against
> one target would each wipe the other's.

**A previous author wrote down the exact hazard this slice walks into.** Two
`reconcileQuiverSizeModifiers` calls would leave the stat holding whichever ran last — **silently,
and forever**.

**The worked precedent is `PlayerHealthSystem:200-202`**, where max health already has this shape:

```java
Map<String, Double> desiredMax = new HashMap<>(HealthModifierItems.desiredModifiers(player, keys));
desiredMax.putAll(GrowthModifierItems.desiredModifiers(player, keys, enchants));
stats.reconcileMaxModifiers(id, desiredMax);
```

with the comment **"TWO SOURCES, ONE RECONCILE CALL, and that is not a tidiness preference."**

Its second paragraph carries the other half, and it is a separate defect from the first:

> The Growth keys are namespaced (`"growth:CHEST"`) because `HealthModifierItems` walks ALL slots on
> bare slot names, so a fixture item and a Growth piece in the same slot would otherwise collide on
> one key and `Stat.putModifier` would keep only one of them.

**Both apply here verbatim.** The item scanner already prefixes `quiversize:`; the enchant scanner
needs its **own distinct prefix**, because a player holding a `quiver_size_boost` item *and* an
Expanded Quiver weapon in the same hand is one slot producing two sources.

**This is the single highest-risk line in the slice, and it is invisible to every test that does not
stage both sources at once.** A gate row must hold an instrument *and* an enchanted weapon
simultaneously; either alone passes under the broken wiring.

---

## 2. SECOND JOB, ANSWERED: THE `EnchantDefinition:110` WARNING IS **SPENT**, AND THE REAL GAP IS ELSEWHERE

**Read as instructed. The comment records a defect that has already been FIXED, and the brief's
instruction is aimed at the historical file rather than the current one.**

The comment says a previous author claimed a new constant would be a compile error and **it was
not** — a switch *statement* over an enum covers nothing silently, `REFLECT` fell through to **no
validation at all**, and a negative reflect could have healed the attacking mob.

**That was repaired in Slice 2b.** The current code reads `Gate gate = switch (effect) {` and
`boolean curved = switch (effect) {` — **switch EXPRESSIONS**, which Java requires to be exhaustive.

I swept every switch over `EnchantEffect` in `core/src/main` and `paper/src/main`. **There are three,
and all three are expressions:**

| site | form | guarded? |
|---|---|---|
| `EnchantDefinition:119` — `Gate gate = switch (effect)` | expression | **yes** |
| `EnchantDefinition:132` — `boolean curved = switch (effect)` | expression | **yes** |
| `EnchantEffectLine:77` — `return switch (definition.effect())` | expression | **yes** |

(`EnchantDefinition:165`'s `requireGate` switch is over `Gate`, not `EnchantEffect`, and is also an
expression — so a new `Gate` constant is likewise a compile error. Not needed here: Expanded Quiver
reuses `MAIN_HAND_ONLY`, §3.3.)

**So adding a constant today WILL fail to compile at three named sites until each is given an arm.**
The comment's warning is true of the file's history and false of its present. It should stay — it is
the account of what the mistake cost — but it should not be used to predict this slice's behaviour.

### THE GAP THE COMPILER GENUINELY DOES NOT COVER, AND IT IS A DIFFERENT GAP

**Those three sites are the ones that DESCRIBE and VALIDATE the effect. None of them is the one that
makes it DO anything.**

I can add the constant, satisfy all three switches, write the lore line, ship the yml — **and the
enchant will load, validate, roll, render a correct tooltip, and grant nothing** — because the
scanner in §1 and its `PlayerHealthSystem` call site are new code that nothing forces me to write.
**Nothing fails to compile. Nothing goes red.** A player sees "+2 arrows" on a weapon whose magazine
never moves.

**That is this slice's own headline pattern arriving at the level of the WIRING rather than the
CURVE** — a tooltip advertising a number the player does not receive. The compiler closes the
description hole and cannot see the delivery hole.

> **The guard is a gate row that reads the RESOLVED magazine off a real shot**, not a unit test that
> asserts the constant exists. Owed in §5.

---

## 3. EXPANDED QUIVER

**RULED BY THE OPERATOR: FLAT ARROWS, NOT A PERCENTAGE.** Everything in this section marked *ruled*
is the operator's; everything marked *derived* is mine and is overturnable.

### 3.1 The mechanism — `QuiverSize`, which already exists

`core/combat/QuiverSize.java` already ships `resolve`, `contribution`, `arrows` and `boosts`, and
`CombatantStats` / `HealthState` already carry the bonus field. **The core arithmetic needs no new
code.** The new work is the `EnchantEffect` constant that names it and the scanner that feeds it.

`QuiverSize`'s methods take `int` rather than `double`, deliberately — its javadoc: *"a fractional
arrow is unrepresentable rather than forbidden."*

### 3.2 THE SIGN TRAP — DIRECTION IS **POSITIVE**, SO `> NONE`

**The family holds two opposite conventions in files that look identical:**

| file | gate | direction | meaning of positive |
|---|---|---|---|
| `Growth` | `> NONE` | positive | more max health — **better** |
| `QuiverSize` | `> NONE` | positive | more arrows — **better** |
| `ManaBank` | `> NONE` | positive | more max mana — **better** |
| **`ReloadTime`** | **`!= NONE`**, named **`declares`** | **INVERTED** | more ticks — **WORSE** |

**Expanded Quiver's direction is POSITIVE. `> NONE` is correct**, the same as Growth and QuiverSize.

**Slice A2 already shipped this bug once**: `> NONE` copied from `QuiverSize` into `ReloadTime`,
which would have abolished reload-speed gear **silently, four times a second**.

> **A FAMILY WITH TWO CONVENTIONS EVENTUALLY COPIES THE WRONG ONE. The defect is not in either
> convention; it is in there being two, unmarked.**

**So: state the DIRECTION and the REASON at the comparison, not just the operator.** A reader who
finds `> NONE` and a reader who finds `!= NONE` must both be told which family they are in without
opening a second file.

### 3.3 The gate — `MAIN_HAND_ONLY` plus `class: ranger`

**Derived, not ruled.** A quiver belongs to the **held weapon**, so the enchant is read off the main
hand, like `DAMAGE`. `EnchantDefinition:148`'s `Gate` already has `MAIN_HAND_ONLY`, and its
`requireGate` arm allows exactly `MELEE`, `RANGER` and `MAGE`.

**The scanner therefore does NOT walk all slots.** It reads the main hand only — which diverges from
`GrowthModifierItems` (four armor slots) and from `QuiverSizeModifierItems` (all slots). **Say so at
the loop**, because the template it was copied from walks more than it does.

> **OPEN, and it decides one line:** `class: ranger` is the honest gate today, since only Ranger
> weapons author `quiver_size`. But `MAIN_HAND_ONLY` admits `MAGE` too, and a mage weapon with a
> magazine is not forbidden by anything. **Recommend `ranger`** — an enchant that enters a pool where
> it does nothing is precisely what `class:` exists to prevent, and it is the argument
> `EnchantDefinition` makes three times for three other effects.

### 3.4 THE POINTER THAT `expanded_quiver.yml` MUST CARRY

**The percentage question has now been asked TWICE, independently** — once by `QuiverSize`'s author,
who declined it and recorded the number in that class's javadoc, and once by the operator tonight,
who did not find that answer **because it lives in `core/combat/QuiverSize.java` and he was thinking
about an enchant.**

> **A SETTLED QUESTION NEEDS A POINTER WHERE IT IS ASKED, NOT ONLY AN ACCOUNT WHERE IT WAS SETTLED.**
> The same question arriving twice from two people is the answer being filed where neither was
> standing. **The second occurrence is the signal to add the pointer, because there will be a third.**

This is `CLAUDE.md`'s own two-homes convention, applied to a question rather than a rule:
`expanded_quiver.yml` is **the pointer**, `QuiverSize`'s class javadoc is **the account**. The
pointer must be obeyable without opening the account, and it must name a **section**, never a line.

Draft, to sit in `expanded_quiver.yml`:

```
# FLAT ARROWS, NOT A PERCENTAGE. One arrow is 11% at a 9-round magazine and 12.5% at the Boltor's
# 8, so any percentage tier below that grants NOTHING -- the tooltip would advertise a number the
# player does not receive. Changing the rounding mode does not rescue it and would put two rounding
# modes in one weapon. QuiverSize's class javadoc is the account; ReloadTime records why the MODE
# and not the phrase is the house rule.
```

### 3.5 The curve — **OPEN, operator's call**

**+1 / +2 / +3 arrows** is the obvious shape and is **nearly uniform in effect across the live
range**, which is the argument for it:

| weapon | authored | +1 | +2 | +3 |
|---|---|---|---|---|
| Boltor | 8 | +12.5% | +25% | +37.5% |
| Locust | 12 | +8.3% | +16.7% | +25% |

The live spread is **8 and 12**. `quiver_stone`'s 9 is in the deletion set and is not counted.

### 3.6 THE BOUND, PARKED WITH A NAMED TRIGGER

Flat `+N` loses relevance as magazines grow — **+3 on a 40-round weapon is 7.5%**: real, but not
worth an enchant slot.

> **TRIGGER: the first weapon shipped with `quiver_size` materially above 20.**

Not a date, an **event** — checkable by grep, which is the property `CLAUDE.md` asks of an
invalidator. `Quiver.applyPercent` already exists and is tested, carrying a recorded obligation that
any percentage reaching a capacity must pass through `resolve`, if that trigger ever fires.

---

## 4. PUNCH — **DESIGN COMPLETE, PARKED. NOT IN SLICE D.**

**RULED BY THE OPERATOR: +20% / +40% / +60% KNOCKBACK — and these ship unchanged.**

Knockback is genuinely continuous — no grid, no floor, no inert tiers — so **a percentage is the
right shape here**, unlike the other two. The problem was never the curve.

> **READ THIS SECTION IN ORDER; IT RECORDS A QUESTION THAT WAS OPEN AND IS NOW CLOSED.** §4.1 and
> §4.2 establish that a **ray**'s base push is `0.0` — which is why Punch cannot work on a Boltor.
> §4.3 is the ruling that resolved it: a **travelling** weapon authors its own knockback, so the
> percentage has a real multiplicand. §4.4 is the roll gate. §4.5 is why it is parked anyway, and its
> trigger.
>
> **§4.1's finding is not superseded by §4.3 — it is the reason §4.3 is shaped the way it is.** A ray
> having no push is now *the design*, not a defect.

### 4.1 THE BASE IS **ZERO** ON EXACTLY THE WEAPONS PUNCH IS FOR

**Traced as instructed. It is not ambiguous, so it did not need a boot to settle.**

`BukkitCombatant.applyDamage`'s javadoc states it outright:

> It does **NOT** deal vanilla damage: vanilla health is a puppet, not truth.

and, on the flash gate:

> When it is 0 (**an ability, which fires no vanilla event**) play the hurt animation ourselves.

A Boltor or Locust shot reaches `CastExecutor.detonate` → `effects.applyAll(ability.onHit(), …)` →
the damage effect → `applyDamage`. **`entity.damage()` is never called, so no `EntityDamageEvent`
fires, so no `EntityKnockbackEvent` is ever raised.** `RpgListeners.onCombatKnockback` returns
immediately on anything that is not `Cause.ENTITY_ATTACK`, which is vanilla melee.

**`VanillaDamagePolicy:52` does not say otherwise.** Its *"i-frames, hurt animation, knockback and
death all ride the event"* is about **rerouted vanilla causes** — lava, fall, drowning — where an
event already exists and is tokened rather than cancelled. **A ray hit raises no event for that
sentence to be about.** The brief read it as evidence a base exists; it is not evidence about this
path.

### 4.2 AND IT WAS ALREADY WRITTEN DOWN — TWICE, IN SHIPPED CONTENT

I did not need to derive this. `lapis_staff.yml:96-108` states the conclusion and anticipates the
exact wrong inference:

> **THIS BEAM DOES NOT KNOCK ITS TARGET BACK.** … `EffectSpec.Damage` reaches
> `BukkitCombatant.applyDamage`, which drains the CUSTOM health store and plays a manual hurt flash —
> **it never calls `entity.damage()`, so no vanilla damage event fires, so no `EntityKnockbackEvent`
> is ever raised.** `EffectApplier` knocks back only from an explicit `EffectSpec.Knockback`, which
> this weapon does not author.
>
> **A REMEDY THAT IS ABSENT LOOKS LIKE A PROBLEM THAT IS PRESENT.**

`cursed_emerald.yml:225` records the same mechanism independently.

**And the Boltor and Locust declare `on_hit: weapon_damage` only — no `Knockback` effect.** So:

> ### **+20% of zero is zero. Punch as ruled is INERT on exactly the two weapons it was designed for.**

**That is the slice's pattern arriving a third time**, and it is the reason this section stops here
rather than proceeding to a curve.

### 4.3 RESOLVED BY OPERATOR RULING — **shape (b), and the ruling defeats (b)'s objection**

> **"Other ranged weapons will have knockback, the instant hitting ranged weapons don't."**
> — operator, 2026-09-13

**A travelling ranged weapon AUTHORS its knockback. Punch multiplies what the weapon declared.**

```yaml
on_hit:
  - type: weapon_damage
    element: kinetic
  - type: knockback
    strength: 0.4
```

**NO NEW SCHEMA.** `EffectSpec.Knockback(double)` exists, `AbilitySchema:201` already parses
`type: knockback`, `EffectApplier` already dispatches it, and `CombatantHandle.applyKnockback` already
performs it. **Every piece was already in the tree.**

**So Punch's numbers ship exactly as first ruled — +20% / +40% / +60%.** It is the only one of the
three enchants that needed no renumbering.

#### Which shape was taken, and why (b)'s cost turned out not to be a cost

Of the three shapes put up in the previous draft, the ruling is **(b)** — *give the weapons an
authored base that Punch multiplies*. **The objection recorded against (b) was that it reintroduces
per-weapon data, the exact thing Rapid Fire parked on. That objection does not survive the ruling,
and the reason is worth keeping:**

> **THE CAST SHAPE MAKES THE OMISSION LEGIBLE.** Under (b)-as-feared, a ranged weapon with no
> authored base was an **oversight** — Punch silently inert, with no way to tell a deliberate
> omission from a forgotten field. Under the ruling, **`type: ray` is itself the declaration**:
> hitscan does not push, and the weapon says `ray` in its own file.

> #### CORRECTION, AND IT REVERSES THIS PARAGRAPH'S FIRST DRAFT
>
> **This block originally read *"the field's ABSENCE carries information"*. That is wrong, and the
> project had already ruled the opposite in writing.** `kinetic.yml:10-15` states the house
> convention for expressing a deliberate nothing:
>
> ```
> ""       a decision. Accepted silently -- an unflavoured hit draws a bare number.
> absent   a GAP. ContentValidator names it.
> ```
>
> — under the heading **PRESENT AND EMPTY**, and the rule *ABSENCE IS NOT A NEUTRAL VALUE*, which
> `Scorch.UNDECLARED_CAP` states for a third field.
>
> **So making absence load-bearing would put TWO CONVENTIONS FOR "DELIBERATELY NOTHING" IN CONTENT,
> UNMARKED** — which is the `> NONE` / `!= NONE` hazard of §3.2 arriving in the content layer, and
> that one cost a silently abolished feature.
>
> **AND THE GENERAL RULE UNDERNEATH, WHICH IS THE PART WORTH KEEPING:**
>
> > **AN AUTHORING RULE CAN CONVERT AN ABSENCE FROM A GAP INTO A STATEMENT — BUT ONLY IF SOMETHING
> > CHECKS THE RULE.** Unchecked, it converts nothing: **a correct absence and a forgotten one are
> > the same bytes**, and the rule exists only in the head of whoever remembers it.
>
> The repair is §4.4's gate, which now keys on the **cast shape** rather than on the effect's
> presence — so nothing rests on an absence, and a forgotten knockback is a **boot-time finding**
> rather than a silent one.

**And (a) is confirmed dead** by §4.1 — there is no event to read. **(c) is not needed**: the
percentage now has a real multiplicand, so it needs no invented reference. Vanilla's own Punch being
absolute per level is no longer an argument for anything here, since we are multiplying an authored
value rather than inventing one.

### 4.4 THE ROLL GATE — the part that keeps the tooltip honest

> **PUNCH MUST NOT ROLL ON A WEAPON THAT AUTHORS NO KNOCKBACK.**

Otherwise a player enchants a Boltor, the tooltip reads *"+40% knockback"*, and nothing happens —
**this slice's own defect arriving a FOURTH time, through the ROLL TABLE instead of the arithmetic.**
That is the thing §0 exists to catch, entering by a door §0 does not watch.

**A loader / roll-eligibility check, not a review obligation** — the whole difference between *inert
by design and visible in the content* and *inert in a way only a player discovers.*

#### GATE ON THE CAST SHAPE, NOT ON THE PRESENCE OF THE EFFECT

**The obvious gate — *does `on_hit` contain a `Knockback`?* — is wrong, and it fails in one specific
direction.** It cannot distinguish:

| weapon | has knockback? | what it means | gate keyed on PRESENCE |
|---|---|---|---|
| a `ray` that correctly declares none | no | **correct by design** | refuses Punch — **right** |
| a `projectile` whose author simply **forgot** | no | **a GAP** | refuses Punch — **silently wrong** |

**For the ray it is right; for the projectile, Punch is quietly unrollable and nothing says why.** The
enchant is missing from a weapon that should have it and **no one finds out** — the failure arriving
through the door the gate does not watch.

**So key it on the cast shape, which is declared and checkable:**

```
type: ray                     knockback MUST BE ABSENT.   Present is a finding.
type: projectile + ranged     knockback MUST BE PRESENT.  Absent is a GAP, and
                              ContentValidator names it -- exactly as it names a
                              missing damage_symbol.
```

**Three things this buys, and the third is the one that matters:**

1. **Eligibility is DERIVABLE** from the cast shape — no second source of truth.
2. **The absence stops being load-bearing**, so the `kinetic.yml` convention is not contradicted.
3. **A forgotten knockback is caught AT BOOT**, not discovered by a player who cannot enchant their
   bow.

**This is the piece that would have been missing when the trigger fired**, which is why it is folded
in now rather than left to the weapon. **The remaining undesigned piece is only the enchant-level
seam** — see §4.6 — not the gate.

### 4.5 IT HAS NO HOME — **a SCHEDULING fact, not a design gap**

**Both measured 2026-09-13, and I verified both independently rather than taking them on report.**

**No weapon in content authors knockback.** `grep -rn "type: knockback" content/` returns exactly two
hits, both abilities:

```
content/abilities/ember_step.yml:29
content/abilities/void_slash.yml:40
```

**Every ranger weapon is a ray except `hunters_bow`**, which is `type: projectile` **and in the
dev-weapon deletion set**:

| ranger weapon | cast | status |
|---|---|---|
| `boltor` | `ray` | live — correctly takes no Punch |
| `locust` | `ray` | live — correctly takes no Punch |
| `quiver_stone` | `ray` | deletion set |
| **`hunters_bow`** | **`projectile`** | **deletion set** |

> **So Punch has ONE candidate weapon today — one that is leaving, and that would need knockback
> authored onto it first — and ZERO after the deletion.**

#### *** THE TRIGGER IS SATISFIED, 2026-09-21. `scatter_shot` IS THE FIRST ELIGIBLE WEAPON, AND NOTHING IS BUILT. ***

**Slice 14 shipped the Scatter Shot: `class: ranger`, `type: projectile`, and it AUTHORS
KNOCKBACK** — because `CLAUDE.md`'s standing decision of 2026-09-13 reaches it and silence would
have been an exception nobody ruled.

**That is the condition §4.4's roll gate tests, met by a live weapon for the first time.** The
table above is superseded rather than deleted — every row in it is still true of its own weapon:

| ranger weapon | cast | authors knockback | Punch eligible |
|---|---|---|---|
| `boltor` | `ray` | no | **no — correct by design** |
| `locust` | `ray` | no | **no — correct by design** |
| `dragons_plume` | `projectile` | **NO** | **no** — and this is the interesting row, see below |
| `quiver_stone` | `ray` | no | no; deletion set |
| `hunters_bow` | `projectile` | no | no; deletion set |
| **`scatter_shot`** | **`projectile`** | **YES, `strength: 0.1`** | ***YES — the first*** |

> **AND `dragons_plume` IS WHY THIS ENTRY WAS STILL PARKED THIS MORNING.** It shipped as a
> `type: projectile` ranger weapon and authors **no** knockback, so it fired §4.5's trigger by cast
> shape and then failed the roll gate by content. **A weapon being a projectile was never the
> condition; authoring the base was.** Worth keeping in front of whoever unparks this, because the
> Plume looks like a counter-example to the table above and is not.

**NOTHING IS BUILT BY SLICE 14, AND THAT IS DELIBERATE.** Unparking Punch is a decision with a roll
table, a tooltip and a gate behind it, and folding it into a weapon slice would make that slice the
one where an enchant quietly shipped. **The record is the deliverable here.**

**TWO THINGS THE UNPARKER SHOULD KNOW BEFORE PRICING IT:**

- **`scatter_shot`'s knockback MAGNITUDE is itself unruled.** `0.1` is a proposal pending
  `GATE-scatter-shot.md` R7b, which measures whether seven applications in one frame **sum** or
  whether the last one **wins**. **Punch is a PERCENTAGE of that base**, so a percentage of an
  unsettled number is a number nobody has ruled twice over.
- **This weapon delivers SEVEN bodies per press.** Punch at +60% of a per-arrow base applies seven
  times at point blank and once at range. **Nobody has ruled what a percentage knockback enchant
  means on a weapon whose application count varies with distance** — which is a genuinely new
  question this weapon creates, not one §4 answered.

#### THE ARGUMENT FOR PARKING IS TESTABILITY, NOT DOUBT

**The design is finished. That is exactly what a parked entry preserves.**

> **YOU CANNOT BOOT-TEST AN ENCHANT NO WEAPON CAN TAKE.** Shipping it means shipping untestable
> behaviour **that will be believed to work**, and the first person able to test it will be meeting a
> bug whose author is gone.

**And an enchant is worse than a guard here, which is why the standing "a guard with no instances
sits inert" ruling does not cover it.** A guard with no instances genuinely sits inert and costs
nothing. **An enchant with no eligible weapon either clutters a roll table or is invisible — and
NEITHER state announces itself.**

> **TRIGGER: the first ranger-class weapon with `type: projectile`.**

Recorded in `NEXT.md` with the numbers, the gate, the authoring rule and the `on_hit` shape, **so the
weapon that fires the trigger inherits a finished design rather than this conversation.**

### 4.6 THE INJECTION POINT, AND THE `isBasicAttack` FEAR IS **UNFOUNDED**

**Checked as instructed, and the answer is not the one the brief expected. Reporting it, because the
architecture is still right and the reason must match the evidence.**

`DamagePayload.isBasicAttack` calls `of(onHit, 0.0)` → `firstDamage`, which walks the list and
returns the **first** effect that yields damage. `damageOf` maps
`case EffectSpec.Knockback k -> Optional.empty()`, and `firstDamage` **skips** an empty rather than
stopping on it:

```java
for (EffectSpec effect : effects) {
    Optional<TriggerDamage> damage = damageOf(effect, weaponAttackDamage);
    if (damage.isPresent()) return damage;      // empty CONTINUES
}
```

**So appending a `Knockback` to a weapon's `on_hit` cannot change `isBasicAttack`'s answer, at any
position in the list.** The `WeaponDamage` is still found, `source() == WEAPON_STAT` still holds,
`WeaponLore` still renders a stat block, and **no golden moves.** The predicted tooltip flip does not
exist.

**The architecture is still right, for a weaker and different reason:**

1. **`ability.onHit()` is loaded once and shared by every wielder.** A per-item enchant cannot append
   to it without copying the list per cast — the enchant is on the *item*, the list is on the
   *definition*.
2. **`EnchantDefinition:13`: content NAMES an effect and BOUNDS it; it never DEFINES one.**
   Synthesising an `EffectSpec` at runtime from an enchant level is code defining content, inverted.

**So: do not justify the architecture with the golden. The golden does not move.**

> #### AND THE RULING CHANGES THIS QUESTION RATHER THAN ANSWERING IT
>
> **This section asked where to INJECT a knockback. Under the ruling there is nothing to inject** —
> the weapon already authors one, so Punch **SCALES A VALUE THAT IS ALREADY IN THE LIST** rather than
> appending a second effect to it.
>
> **That retires the whole concern.** No effect is added, so `isBasicAttack` is untouched for the
> stronger reason that the list does not change at all; and neither of the two architectural
> objections above applies, because nothing mutates the shared `ability.onHit()`.
>
> **What replaces it is a narrower question, and it is the one §4.4's gate also turns on: WHERE does
> the authored `strength` meet the wielder's enchant level?** `EffectApplier` dispatches the
> `Knockback` and holds the caster; the enchant sits on the item. **That seam is the remaining piece
> of Punch's implementation design**, and it is deliberately left to the trigger rather than guessed
> at now — see §4.5. The numbers, the gate and the authoring rule are settled; this one line is not,
> and saying so is cheaper than inventing a seam against a weapon that does not exist.

---

## 5. WHAT IS OWED, AND WHAT IS NOT

> ## SHIPPED 2026-09-13. THIS SECTION IS NOW A RESULT, NOT A FORECAST.
>
> **Final verify run, re-read after the last file landed: `942 / 17 / 638` = 1597, zero failures.**
> (It was `942 / 17 / 633` = 1592 at the first commit; the five new
> `ExpandedQuiverModifierItemsTest` rows are the difference, and `1592` is left standing **only**
> where it names the suite the two failed mutations were run against.)
> Core gained 5 rows (`ExpandedQuiverTest`), paper 3 (`ExpandedQuiverContentInvariantTest`);
> baseline was `937 / 17 / 630` = 1584.
>
> ### THE MUTATION THAT WAS RUN: `MUTEXEMPT`
>
> **Target:** `ExpandedQuiverContentInvariantTest.EXEMPT_DEV_WEAPONS`, `List.of("hunters_bow")`
> → `List.of()`.
>
> **Both halves of the marker grep, as required:** marker present `1`, original gone `0`. Restored
> from a scratchpad copy — **never `git checkout --`** — and re-grepped: `markers left: 0`, original
> back, bytes `9246` → `9246`.
>
> > **THE BYTE DELTA WAS ZERO, AND THAT IS WHY THE GREP IS TWO CHECKS RATHER THAN A DELTA.**
> > `"hunters_bow"` and ` // MUTEXEMPT` are both **13 characters**, so the file's size did not move.
> > A byte/line delta — the instrument `CLAUDE.md` requires *alongside* the marker grep, because it
> > is the only thing that sees an edit applying TOO WIDELY — is **uninformative here** and, read
> > alone, would have looked exactly like an edit that did not apply. **The two-directional marker
> > grep is what settled it.** Neither instrument subsumes the other; this is the case that shows it
> > from the delta's side.
>
> **Result: 2 of 3 rows red, and the split is the verification.**
>
> | row | outcome | why |
> |---|---|---|
> | `everyRangerWeaponAuthorsAMagazineOrIsAnExemptDevWeapon` | **RED** | named `hunters_bow` in its failure message — it detects a real ranger weapon with no magazine |
> | `theExemptionSetHoldsExactlyTheOneDevWeaponItWasWrittenFor` | **RED** | expected; it pins the list the mutation edited |
> | `theWeaponContentIsREACHABLEAndHoldsTheRangerWeaponsExpected` | **green** | **correct** — it never reads the exemption, so it must NOT move |
>
> **The green row is the part worth reporting.** A mutation that reddened all three would have meant
> the positive control was entangled with the thing it controls for. It stayed green, which is what
> says the discovery guard and the invariant are independent.

> ### ALL FOUR NOW RUN — AND TWO OF THEM WERE GUARDING NOTHING
>
> **The previous revision of this section said `MUT-SIGN`, `MUT-MERGE` and `MUT-PREFIX` were
> "asserted and green, which is not verified". That was the right thing to say and it understated
> the problem: two of the three were guarding NOTHING AT ALL.**
>
> | mutation | target | before | after the fix |
> |---|---|---|---|
> | `MUTEXEMPT` | exemption list | **2/3 red** | — |
> | `MUTSIGN` | `QuiverSize.boosts` `>` → `!=` | **2 red** (mine + `QuiverSizeTest`) | — |
> | `MUTMERGE` | delete the `putAll` | **0 failures of 1592** | **2/5 red** |
> | `MUTPREFIX` | alias prefix to `quiversize:` | **0 failures of 1592** | **2/5 red** |
>
> **THE TWO THAT FAILED ARE THE TWO THE TARGET FILE PREDICTED BEFORE THE SLICE BEGAN.**
> `QuiverSizeModifierItems` wrote both traps down under *"It reconciles alone, for now"*, and
> `PlayerHealthSystem:198` documents the key collision for max health. **Both were understood, both
> were written about at length, and neither was caught by anything.**
>
> > **THE MUTATION YOU RUN FIRST IS THE ONE YOU THOUGHT OF LAST.** `MUTEXEMPT` was invented during
> > this slice and was run immediately. `SIGN`, `MERGE` and `PREFIX` were predicted by the target
> > file *before the slice started* — and **a guard written against a KNOWN trap is the one most
> > likely to be asserted and trusted, because the trap is already understood.** Understanding is not
> > evidence.
>
> **WHY THE ASSERTIONS MISSED, AND IT IS THE SAME CAUSE TWICE.** `ExpandedQuiverTest` models both
> defects against `Stat` **directly**, with the key strings written as **literals**. So it never
> reads `SOURCE_PREFIX` and never reaches `PlayerHealthSystem`. It **documented** the traps
> faithfully and **guarded** neither. A test that reproduces a defect is not the same as a test that
> is wired to the code that could cause it.
>
> **THE FIXES ARE STRUCTURAL WHERE THEY CAN BE:**
>
> - **`MUT-MERGE`** — the inline `new HashMap<>(a)` + `putAll(b)` was extracted into
>   `ExpandedQuiverModifierItems.mergedSources(a, b)`. **Dropping a source now means dropping an
>   argument, which does not compile**, and the remaining reachable mutation — editing that body —
>   reddens `ExpandedQuiverModifierItemsTest`. A unit test can exist here only because the function
>   takes plain maps and no `Player`.
> - **`MUT-PREFIX`** — new rows read the **actual constants** of both scanners, and also refuse
>   either being a *prefix of* the other, which mere inequality does not catch.
>
> **AND ONE THING IS RECORDED RATHER THAN FIXED: the max-health pair is still inline and is unguarded
> by the same measurement.** `PlayerHealthSystem` merges `HealthModifierItems` and
> `GrowthModifierItems` with exactly the `putAll` shape this slice just proved reddens nothing. That
> is a second edit to a second stat, and bundling it into a quiver slice is how a change stops being
> reviewable.

When Expanded Quiver lands, the mutation set is knowable in advance and **the axis count matters**:

- `MUT-SIGN` — `> NONE` becomes `!= NONE` on the new gate. **Must redden**, or the sign trap in §3.2
  is unguarded.
- `MUT-MERGE` — drop the `putAll` in `PlayerHealthSystem`, leaving two reconcile calls. **Must
  redden**, and only a row staging **both** sources at once can see it (§1).
- `MUT-PREFIX` — alias the new scanner's `SOURCE_PREFIX` to `quiversize:`. **Must redden**, and only
  a row holding an instrument and an enchanted weapon **in the same slot** can see it.

**Count the axes before counting the mutations:** the merge and the prefix are two degrees of freedom
in one wiring change, and one splice cannot certify both — `MUT-MERGE` is invisible to a single-slot
staging and `MUT-PREFIX` is invisible to a two-slot one.

**Stage so no two quantities collide.** The Boltor's 8 with `+2` resolving to `10`, against an
instrument's `DEFAULT_BOOST` of `19` resolving to `27`: **8, 2, 10, 19 and 27 are pairwise
distinct**, so no reading can be confused for another and no transposition can hide.

---

## 6. ORDER

1. ~~Record the §0 percentage rule in `CLAUDE.md`.~~ **DONE** — landed with the pattern-break note
   from §0, so the rule reads as *name the quantity* rather than *prefer flat*.
2. ~~Record the knockback authoring rule and Punch's roll gate in `CLAUDE.md`.~~ **DONE** — with its
   scope stated, since three shipped non-ranger projectile weapons author no knockback and are
   **outside** the rule rather than in violation of it.
3. ~~Move Punch to `NEXT.md` against its trigger.~~ **DONE.**
4. ~~Fix the falsified comment in §7.~~ **DONE** — corrected rather than deleted, with the
   supersession recorded at the site.
5. ~~**Expanded Quiver — THE WHOLE OF SLICE D.**~~ **DONE, 2026-09-13.** Shipped in the planned order:

   | # | artefact | note |
   |---|---|---|
   | 1 | `EnchantEffect.QUIVER_SIZE` | the only `core` change — **no new mechanism**, it names the already-shipped `QuiverSize` |
   | 2 | `ExpandedQuiverTest` (core, 5 rows) | written **before** the paper wiring, per `CLAUDE.md` |
   | 3 | `EnchantDefinition` — `Gate` arm, `curved` arm, **new `Gate.RANGER_ONLY`** + its `requireGate` arm | see below: `MAIN_HAND_ONLY` was **wrong** |
   | 4 | `EnchantEffectLine` — `QUIVER_SIZE` arm | renders `+N Quiver Arrows`, singular at 1 |
   | 5 | **`ExpandedQuiverModifierItems`** | the scanner. Main hand ONLY — narrower than both files it was modelled on |
   | 6 | **`PlayerHealthSystem`** merged reconcile | `new HashMap<>(instrument)` + `putAll(enchant)` + ONE `reconcileQuiverSizeModifiers` |
   | 7 | `expanded_quiver.yml` | carries the §3.4 pointer verbatim |
   | 8 | `ExpandedQuiverContentInvariantTest` (paper, 3 rows) | **not in the original plan** — see §6.1 |

   > **§3.3 WAS WRONG ABOUT THE GATE, AND THE PLAN'S OWN RECOMMENDATION IS WHAT CAUGHT IT.** It said
   > `MAIN_HAND_ONLY` plus `class: ranger`. **`MAIN_HAND_ONLY` admits `gearClass == null`** —
   > `class: universal` — which is correct for `DAMAGE` (every weapon deals some) and is **the
   > dangerous typo here**: a universal quiver enchant enters every sword's roll pool and sells an XP
   > unlock that does nothing, the exact refusal `ARMOR_ONLY` and `SHIELD_ONLY` each exist to make.
   >
   > So `Gate.RANGER_ONLY` was added — `MAIN_HAND_ONLY` minus that one value. **The `requireGate`
   > switch is over `Gate` and is an EXPRESSION, so the new constant was a compile error there until
   > it was given an arm** — §2's claim about compiler guarding, tested by accident and holding.

### 6.1 THE ONE THING SHIPPED THAT THE PLAN DID NOT NAME

**`ExpandedQuiverContentInvariantTest`, and it exists because `hunters_bow` is `class: ranger` and
authors no `quiver_size`.**

`expanded_quiver.yml` is `class: ranger`, so it enters that weapon's roll pool, renders
*"+2 Quiver Arrows"*, and grants **zero**. **That is §0's defect arriving through the ROLL TABLE** —
the fourth time, and exactly the shape the operator named for Punch in §4.4.

**The three obvious homes cannot see what they would need to**, each measured rather than assumed:

| home | what it receives | why it cannot |
|---|---|---|
| `EnchantRoll.roll` | `(gearClass, roster, rng)` | the roster is `(id, gearClass)` pairs — **no weapon at all** |
| `EnchantEffectLine.bare` | `(definition, level, GearClass)` | no `WeaponDefinition`, so it cannot render an `inert:` line for this case |
| `ContentValidator.validateEnchants` | `(enchants, materialExists)` | cannot see the weapons |

Each could be taught to, and **each is a signature change rippling through its callers.** The
content-invariant test needs none, is **build-time rather than boot-time**, and **cannot be built
past** — strictly stronger than `ContentValidator`, whose own javadoc says it *"warns; never
throws"*. Same shape as `ScorchContentInvariantTest`.

`hunters_bow` is exempted **by name**, as a dev weapon already in the deletion set, with the
exemption's size pinned so it cannot rot into a blanket. **The day a NEW ranger weapon ships without
a magazine, the suite reddens and the author chooses deliberately.** Marked
`DELETION-SET-CITATION: hunters_bow` for the eventual sweep.

**Punch is out of the slice entirely.** Its design is complete and parked at §4.5's trigger; nothing
in it blocks or is blocked by Expanded Quiver.

**Expanded Quiver goes first**, as directed: its target pipeline exists, its direction is
unambiguous, and it establishes whether an enchant can reach the modifier path at all — **which it
turns out it cannot today, and that answer is what Punch also needed.**

---

## 7. A FALSIFIED COMMENT FOUND ON THE WAY, RECORDED HERE SO IT IS NOT LOST TO THE TRANSCRIPT

`BukkitCombatant.java:154` describes the melee path as:

> *(see `RpgListeners`' player-melee handler, **which tokens it and cancels its knockback**)*

**The cancel half is false.** `RpgListeners.onCombatKnockback` **returns early** — does not cancel —
for the hit that claimed the `MeleeHits` window, and `RpgListeners:1508-1513` records that this
**replaced** the older design:

> This replaces the design's older *"always cancel vanilla KB, then apply the declared one"*, which
> left melee pushing nothing at all because no shipped weapon declares one.

**`BukkitCombatant:154` still describes the superseded design.** It is `CLAUDE.md`'s *"prose that
outlived its mechanism"* — a comment a reader can check, unlike a flavour line, but one that
currently tells them melee has no vanilla push when melee is the **only** path that has one.

**It is not in this slice's scope and it is a one-line fix.** Per `CLAUDE.md`'s rule that a finding
living only in the conversation is not recorded, it is written here now rather than left for the
commit that happens to touch that file.

---

## 8. STILL OPEN FOR THE OPERATOR — NONE OF THESE BLOCKS THE PLAN

- **Expanded Quiver's curve** — `+1/+2/+3` recommended (§3.5).
- **Does a bigger magazine mean a longer reload?** **The brief said `reload_ticks` is "a flat 60".
  It is flat across the two LIVE weapons and not across the three authored ones** — measured with
  `grep -rn "^reload_ticks:" content/weapons/`:

  | weapon | `quiver_size` | `reload_ticks` |
  |---|---|---|
  | Boltor | 8 | 60 |
  | Locust | 12 | 60 |
  | `quiver_stone` (deletion set) | 9 | **34** |

  **So reload does not merely fail to scale with capacity — across Boltor and `quiver_stone` it
  INVERTS**: the larger magazine reloads faster. Nothing is wrong with that (they are a real weapon
  and a dev weapon, and §0's descent rule says not to derive from the second), but it means there is
  **no existing relationship for Expanded Quiver to preserve.**

  **My read: leave it.** Coupling them makes Expanded Quiver partly a self-nerf, and the enchant has
  no way to say so on a tooltip — which is §0's failure wearing the opposite sign.
- ~~**Punch's base — the §4.3 choice.**~~ **RULED, §4.3.** It was the only blocking item, and
  **nothing blocks the slice now.**
- **Display names, icons and `max_level` for Expanded Quiver.** `icon` defaults to `enchanted_book`
  when absent and is never a reason a working enchant fails to load, so it is cosmetic and last.
  **Punch's cosmetics are deferred with Punch** — naming it now would put a display string in the
  roll table's reach ahead of any weapon that can take it.
