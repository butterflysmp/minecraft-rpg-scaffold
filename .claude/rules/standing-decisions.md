---
paths:
  - "paper/src/main/resources/content/**"
  - "PLAN-*.md"
  - "paper/src/main/java/**/content/**"
  - "core/src/main/java/**/enchant/**"
---

## Standing decisions — the accounts. The pointers are in CLAUDE.md, under the heading of the same name

- **NEW CONTENT CITES THE BOLTOR, NOT `hunters_bow`, `ironblade` OR `quiver_stone`.**
  Those **three** are **in a deletion set**, parked on *enough shipped weapons to replace them* (see
  `NEXT.md`). Every citation of one written between now and then is a comment that will point at a
  file that does not exist.

  > **THE SET HAD FOUR MEMBERS UNTIL 2026-09-25, AND THE FOURTH LEFT BY BEING DELETED, NOT RELEASED.**
  > `ability_stone` was deleted in the Ability Stone slice (PLAN-build-system.md section 1.4), in the
  > same commit that shipped the new Ability Stone item, so the name never referred to two things. Its
  > sweep was exactly the prose the measurement below predicted: at `041a054`,
  > `git grep -h ability_stone -- '*/src/main/java/*'` returned **13** lines; after the sweep, at the
  > slice's code commit `f92a163`, it returns **3**, and **all 3 are comments marked "since deleted"**
  > (the same `grep -cE` and the same code-line control as below). The accounts below that name
  > `ability_stone` are KEPT as the record of how it was ruled and measured.

  **THE SET WAS RULED IN TWO SITTINGS, AND THE DATES ARE KEPT SEPARATE BECAUSE THE MEASUREMENTS
  BELOW ARE.** `hunters_bow`, `ironblade` and `quiver_stone` — **2026-09-12**. `ability_stone` —
  **2026-09-22**, Ben, closing `GATE-gearscore.md`'s open flag: the 2026-09-21 *leave the stones
  scored* ruling rested on their being *"on their way out"*, which was true of `quiver_stone`, true
  of `volley_stone` by another mechanism, and **queued nowhere for `ability_stone`.** Adding it makes
  the premise true of all three rather than an assumption about one.

  **When a dev weapon is genuinely the only precedent, cite it AND MARK the citation** as standing on
  a weapon in the deletion set, so the eventual sweep finds it by **grepping for the marker** instead
  of re-deriving the reference graph.

  > **THE PLAN HAS A LOOP IN IT AND THIS IS THE FREE HALF OF THE FIX.** The precondition for deleting
  > the dev weapons is **more real weapons** — and new weapons are also **what adds references to the
  > dev weapons**, because the dev weapons are the precedents new prose derives from. `boltor.yml`,
  > the newest weapon in the project, **cites all three of the 2026-09-12 set.** So every weapon
  > authored between now and the deletion raises the deletion's cost.
  >
  > It compounds quietly because it is **staleness, not breakage**: nothing fails, nothing is listed,
  > and the bill arrives later as a sweep nobody scoped. Measured at `2a3fb68`: **every mention of
  > these three in `main` is a comment except one**, so the deletion's real cost is prose, not code.
  >
  > **THAT FIGURE COVERS THE 2026-09-12 THREE AND NOTHING ELSE — IT WAS TAKEN BEFORE THE FOURTH
  > EXISTED, so folding `ability_stone` into it would extend a measurement over a member it never
  > looked at.** Measured separately at `35f5a2a`, 2026-09-22:
  > `git grep -h ability_stone -- '*/src/main/java/*'` returns **13** lines and **all 13 are
  > comments, 0 code** (`grep -cE '^[[:space:]]*(\*|//|/\*)'`, controlled against a code line, which
  > it correctly does not count). Same shape as the other three: the cost is prose.
  >
  > **`boltor.yml` does NOT cite `ability_stone`** — measured `0` at the same revision, against
  > `hunters_bow 7`, `quiver_stone 7`, `ironblade 2`. **So the newest weapon raises the cost of the
  > first three and not of the fourth**, which is the one thing the loop above does not predict.

  **TWO THINGS TO KNOW ABOUT THE BOLTOR, SINCE IT IS NOW THE REFERENCE POINT:**

  - **Its numbers are RULED, not derived** — `quiver_size 8`, `cooldown_ticks 16`, `range 96`,
    `reload_ticks 60`, `attack_damage 19`. **`19` was ruled outright BECAUSE deriving it from
    `ironblade` was wrong**: `ironblade` is a dev weapon and is not balanced meaningfully.
    **So: never derive a new weapon's numbers from a dev weapon's.** Parity with a placeholder is
    parity with nothing, and it propagates — the derived number then becomes the next weapon's
    precedent and the placeholder's arbitrariness outlives the placeholder.
  - **`16` is on the 4-tick input grid, deliberately.** A held right-click's inputs are quantised onto
    a 4-tick grid, so a weapon's real fire interval is its authored cooldown **rounded UP to the next
    multiple of 4** — author `13` or `14` and you have authored `16`, **and the tooltip will not say
    so.** **Author multiples of 4.**

    > **THIS SAID "delivers an input only every 4 ticks" UNTIL 2026-09-13, AND THAT PERIOD CLAIM IS
    > FALSE.** `GATE-locust.md` row 1 read `INPUTS min 3t`, twice. **Holding right-click does not
    > produce a periodic stream at all** — operator's ruling, a property of the vanilla client.
    > **The grid survives and every prediction it makes survives with it**; what died is the sentence
    > explaining *why* there is a grid. Do not restore *every*, and do not change the 4 — the
    > arithmetic is confirmed at four measured points and the mechanism was never what the arithmetic
    > rested on. The mechanism, the measurements and the tooltip consequence are
    at `WeaponLoader`'s `cooldown_ticks` section — **this is the pointer, that is the account.**

    > **THE "MULTIPLES OF 8 IF IT MAY EVER BE DUAL-WIELDED" CLAUSE IS WITHDRAWN, 2026-09-13 — AND IT
    > IS WITHDRAWN FOR WANT OF A SUBJECT, NOT BECAUSE IT WAS WRONG.** The operator ruled that the
    > second Ranger weapon is a single item: *"it's not going to be Dual wielded."* **Nothing halves
    > any more**, so a rule about surviving halving has nothing to apply to. The `locust` ships at
    > **12** — not a multiple of 8, and correct.
    >
    > **`16` IS NOT RE-RULED, AND THE RECORD MUST STILL EXPLAIN WHY 16 AND NOT 12.** The clause was
    > one of the Boltor's reasons and its other reasons stand — the account at `WeaponLoader`'s
    > `cooldown_ticks` section, and the derivation in `boltor.yml`'s `cooldown_ticks` block, which
    > carries the same withdrawal note.
    >
    > **AND THE CLAUSE WAS UNSATISFIABLE ANYWAY, WHICH IS WHY THIS IS A WITHDRAWAL AND NOT A PAUSE.**
    > Halving moves a weapon onto a smaller cooldown, and **the attack-speed dead zone widens as the
    > cooldown shrinks** — `8 -> 4` is inert entirely, `16 -> 8` dead to +77.8%, `32 -> 16` to +28.0%.
    > To buy a *dual* dead zone as narrow as the Boltor's *single* +28% you must author **32**, a
    > 1.6-second shot. **Clean halving, attack-speed headroom and a fast weapon are jointly
    > unsatisfiable.** Full argument in `PLAN-locust.md`.

### NAME THE QUANTITY, AND NAME THE SET OF THINGS THAT HAVE IT

**Before writing any `value_by_level`: name the quantity the curve moves, and answer BOTH halves.**

1. **ARITHMETIC — is the quantity non-zero and continuous at the values it will meet?** If it is
   quantised, floored, or absent, **author a FLAT value instead** — or park the feature.
2. **ELIGIBILITY — does every piece of gear this can ROLL ON actually possess that quantity?** If
   some do not, **the gate is wrong, and no curve fixes it.**

> **THE TWO HALVES ARE ONE RULE, NOT TWO, AND THAT IS THE WHOLE POINT.** An enchant can be
> **arithmetically honest and still grant zero**, because the weapon it landed on does not possess
> the thing it modifies. **Passing the first half reads as passing** — the number is real, the
> multiplicand is real, and the player still receives nothing.
>
> **THE SAME ENCHANT FAILED BOTH HALVES**, which is why they cannot be separate rules: Expanded
> Quiver failed the arithmetic half (a percentage of an integer magazine floors to nothing) and was
> ruled FLAT — **and then failed the eligibility half anyway**, because `hunters_bow` is
> `class: ranger` and authors no `quiver_size`. Fixing the first did not touch the second.

**The failure is not a weak enchant. It is a tooltip that advertises a number while the player
receives ZERO**, and nothing goes red, because a curve that resolves correctly and lands on nothing
is indistinguishable from one that works.

Three enchants proposed in one slice (2026-09-13) produced **four** failures of this rule, across both
halves — which is why it is stated as a rule rather than four notes:

| proposed | half | the quantity | how it fails |
|---|---|---|---|
| **Rapid Fire** | arithmetic | a **QUANTISED** cooldown | the 4-tick input grid swallows anything under **+28%** |
| **Expanded Quiver** | arithmetic | an **INTEGER** magazine | `QuiverSize.arrows` floors — one arrow is 11% at 9 rounds, so every tier below that grants nothing |
| **Punch** | **eligibility** | a knockback base that **DOES NOT EXIST** on a ray | `applyDamage` never calls `entity.damage()`, so a ray hit raises no `EntityKnockbackEvent`; base push `0.0` |
| **Expanded Quiver**, *again* | **eligibility** | a magazine the weapon **does not have** | `hunters_bow` is `class: ranger` with no `quiver_size`, so the enchant rolls on and renders `+2 Quiver Arrows` for nothing |

**Quantised, floored, absent, unpossessed — four different mechanisms, one question catches all
four**, and it is cheaper than any of the four investigations that found them separately.

> **THE FOURTH ROW IS WHY THE RULE HAS TWO HALVES.** It was found only after the arithmetic half had
> already "passed" the enchant and a flat value had been ruled. **`+2 arrows` is `+2 arrows`** — the
> arithmetic test cannot see it. It arrived through the **ROLL TABLE** instead, which is a door the
> one-half version of this rule does not watch.

> **AND PUNCH IS THE ONE THAT SURVIVED, WHICH IS THE HALF OF THIS RULE WORTH KNOWING.** Operator
> ruling, 2026-09-13: *"Other ranged weapons will have knockback, the instant hitting ranged weapons
> don't."* A **travelling** weapon authors an `EffectSpec.Knockback` in its `on_hit`, and +20/40/60%
> of an authored `strength` is a real percentage of a real value. **Punch's numbers shipped as first
> ruled — the only one of the three that needed no renumbering at all.**
>
> **THE FIX WAS NOT A DIFFERENT CURVE. IT WAS NAMING THE QUANTITY.** The other two were repaired by
> changing the arithmetic — park it, or go flat. This one was repaired by **supplying the missing
> multiplicand**, and the curve never moved. So the rule's instruction is *name the quantity and
> check it*, in that order, and **not** *"prefer flat"*: two of three failures did need flat, and
> reading the rule as a preference for flat would have renumbered a curve that was correct.
>
> **The check is the same either way, and that is the point** — you cannot tell which of the two
> outcomes you are in until you have named the quantity and gone and looked at it.

> **THE THIRD ONE IS THE REASON THIS IS A CHECK AND NOT A REMINDER TO BE CAREFUL.** *Is this
> continuous?* is a question about a number you can see. *Does this base exist at all?* is a question
> about a call you have to go and read — and the plausible answer was the wrong one, because
> `VanillaDamagePolicy` says knockback rides the vanilla event and the ability path raises no vanilla
> event for that sentence to be about.

**Practically: write down the quantity's actual authored values before the curve.** The live
magazine spread is `8` and `12`; the live knockback base on a ray is `0.0`. Both took one grep and
one trace, and both were available before any design.

Full account, all three instances, and the Punch base trace: `PLAN-enchants-ranged.md`.

### A TRAVELLING RANGED WEAPON AUTHORS KNOCKBACK; AN INSTANT-HITTING ONE DOES NOT

**Operator ruling, 2026-09-13.** A `type: projectile` ranged weapon **lands with impact** and declares
an `EffectSpec.Knockback` in its `on_hit`. A `type: ray` ranged weapon is **hitscan** and declares
none.

```yaml
on_hit:
  - type: weapon_damage
    element: kinetic
  - type: knockback          # travelling weapons only
    strength: 0.4
```

**No new schema.** `EffectSpec.Knockback(double)`, `EffectApplier` and
`CombatantHandle.applyKnockback` all already exist; `AbilitySchema` already parses `type: knockback`.

**THIS IS A CONTENT RULE, AND THE CODE DOES NOT IMPLY IT — WHICH IS THE ONLY REASON IT NEEDS WRITING
DOWN.** Neither cast shape produces knockback on its own: `CastExecutor.detonate` is the single
on-hit site and **every** shape routes through the custom health store, so nothing calls
`entity.damage()` and no `EntityKnockbackEvent` is ever raised. **You cannot read this rule off the
engine — both shapes look identical there.** It is a decision about what content declares.

**This is why Punch does nothing on a Boltor, and that is CORRECT rather than a gap.** A ray is
meant to have no push.

> **SCOPE, STATED BECAUSE THE OBVIOUS SWEEP IS WRONG.** The ruling is about **ranged** weapons.
> Measured 2026-09-13: three shipped weapons are `type: projectile` and author no knockback —
> `ember_staff` and `flint_staff` (mage) and `emberblade` (melee). **This rule does not reach them.**
> Do not "fix" them, and do not cite this rule at a staff.
>
> **AND THEY ARE UNRULED, NOT EXCLUDED — THE TWO ARE ONE KEYSTROKE APART AND ONLY ONE IS TRUE.**
> Nobody has decided that a travelling MAGE weapon should not push. **The question was never put.**
>
> > **AN UNRULED CASE MUST BE RECORDED AS UNRULED, NOT AS EXCLUDED.** *"Excluded"* says a decision
> > was taken and closes the question; *"unruled"* says it is still open and invites it. **Writing
> > the first when the second is true silently converts an omission into a ruling nobody made** —
> > and the next person to look finds a settled-looking answer with no author.

**THE ROLL GATE, AND IT IS WHAT KEEPS THE TOOLTIP HONEST:**

> **PUNCH MUST NOT ROLL ON A WEAPON THAT AUTHORS NO KNOCKBACK.** Otherwise a player enchants a
> Boltor, the tooltip reads *"+40% knockback"*, and nothing happens — **the exact dishonesty the rule
> above eliminated three times, arriving a fourth time through the ROLL TABLE instead of the
> arithmetic.**
>
> It is **mechanically checkable**: the weapon's `on_hit` list either contains a `Knockback` or it
> does not. That is a loader / roll-eligibility check, **not a review obligation** — and it is the
> whole difference between *inert by design, visible in the content* and *inert in a way only a
> player discovers*.

**Punch is PARKED, design complete** — no weapon can take it today. Trigger and full design in
`NEXT.md`; the design itself in `PLAN-enchants-ranged.md` §4.
