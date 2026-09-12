# PLAN — Q7's instrument, and the row it makes runnable

> **Slice B, commit 1 of N.** A2 shipped and merged as `249a85e`; its record is
> `PLAN-quiver-a2.md` on `master`. **This is not slice B's plan — that follows the reading.**

## Context

**Q7 — the held-right-click repeat interval — has been owed since A1 and deferred twice.** It is the
one row in two gates with **no green state**: it produces a number that exists nowhere else in the
project, so "green" cannot select an outcome.

**The reason it keeps being deferred is that nobody built the thing that measures it.** Its recipe
asks an operator to hold a button for a timed ten seconds, **count the shots by hand**, and divide.
A row that needs hand-counting is un-runnable, and an un-runnable row gets deferred — which is
exactly what happened, twice.

**It is now doubly load-bearing.** Ruling 1 makes held-repeat the Boltor's maximum rate of fire, so
the interval *is* the weapon's DPS denominator; and at range 96 it decides whether bolts overlap in
flight. Three of slice B's figures are priced on it: `attack_damage`, `reload_ticks`,
`cooldown_ticks`.

**This plan is ONE COMMIT: the instrument and its gate row. It is not slice B's plan.** Writing the
rest before the number exists is writing against an unmeasured floor — the specific failure that
produced `MIN_RELOAD_TICKS` twice.

---

## WHAT IS BUILT, AND THE TWO RULINGS THAT SHAPE IT

**RULED: count BOTH, and report the pair with its relation.** A single number cannot say which of
two mechanisms produced it — a reading of 15 on `hunters_bow` is indistinguishable between *the
input floor is 15* and *the cooldown is 15 and the floor is lower*. Two counters, one verdict word:

| verdict | meaning |
|---|---|
| **INPUT-LIMITED** | inputs == fires. **That interval IS Q7's answer.** |
| **COOLDOWN-LIMITED** | fires slower. **Q7's answer is still the INPUT number**; the gap is slice C's. |
| **NO REPEAT** | one input, then nothing, while the button is held. The client is not re-sending. **Not an instrument fault** — the instrument worked and the answer is that this material does not repeat. **Ruling 1 then needs re-ruling before slice B proceeds**, and the operator is told so on the readout rather than left to infer it. |
| **INSTRUMENT FAULT** | inputs slower than fires. **Impossible** — a gate cannot fire more often than it is asked to. Printed as those words, not as a verdict. |

**That third arm is the control**, and it is why the pair is worth more than the sum: an instrument
with no reading that would indict it cannot tell you when it is broken.

**RULED: `/rpg firerate`, print and clear.** Eight figures — **count, window, mean, minimum** for
inputs and for fires — plus the verdict word, the weapon, and its cooldown.

- **The window is FIRST COUNTED EVENT TO LAST**, never command-to-command, or the mean is arithmetic
  over the dead time before the operator started holding.
- **An empty sample says so in words.** Run twice and the second call has nothing; printing `0` or a
  zero mean there is a reading that looks like an answer. This is the one command where a plausible
  zero would be believed.
- **The reading names its weapon and that weapon's `cooldown_ticks`.** The input floor should be
  weapon-independent and the fire number is not; **two weapons disagreeing about the input floor is
  a finding**, and a reading that cannot surface it is worth less.
- **MINIMUM is not decoration.** Q7 is named *the repeat FLOOR*: the floor is the smallest gap, and
  the mean is the sustained rate. They answer different questions — the minimum prices slice C's
  7-tick halving, the mean prices ruling 1's *"eight rounds is a few seconds"* brake.

### The original recipe is struck, and ONE CLAUSE OUTLIVES IT

> ~~set `cooldown_ticks` to **1** and `quiver_size` to **60** so the cooldown cannot be the
> limiter~~ — **DO NOT.** `quiver_stone`'s numbers were chosen by an exhaustive collision sweep and
> eight other rows read bare values that depend on it. Mutating a swept fixture to take one reading
> spends a guarantee those rows stand on.
>
> **Counting inputs BEFORE the gate makes BOTH edits unnecessary**, not just the cooldown one: a
> magazine cannot limit an input either. **No content file is touched by this commit.**

> **SURVIVING CLAUSE — run it on a CANCELLED BINDING, because that is the configuration that
> ships.** Held-use cadence is not independent of whether the interaction is cancelled; a reading on
> an uncancelled binding is a number about a configuration nothing ships. **This is stated at the
> counter**, because the next reader finds the recipe struck through and needs to see which clause
> outlived it.

---

## DESIGN

### 1 · `core/combat/FireCadence.java` — the recorder and the arithmetic

Stateful core class on `CooldownTracker`/`DamageWindow`'s exact precedent: a `LongSupplier
currentTick` injected so a fake clock drives the tests, `ConcurrentHashMap` keyed by player, **no
Bukkit**. Reuse that shape rather than inventing one.

- `record(UUID player, String input, Kind kind, String weaponId)` — `Kind` is `INPUT` or `FIRE`.
  Keyed by `(player, input, kind)` so **no magic string lives in `WeaponFire`**; the command asks
  for `"right_click"`, which is where the question is actually being asked.
- Per-sample state: count, first tick, last tick, previous tick, **minimum interval**, the weapon id,
  and **a flag set if the weapon id ever changed**.
- `sample(UUID, String input, Kind)` → `Optional<Sample>` — **empty when nothing was recorded**, so
  absence cannot be rendered as a zero.
- `Sample`: `count`, `windowTicks` (last − first), `meanIntervalTicks` (window / (count − 1)),
  `minIntervalTicks`, `weaponId`, `mixedWeapons`.
- **`count == 1` has no interval at all** and says so — one event is a timestamp, not a cadence.
- `Verdict verdict(Sample inputs, Sample fires)` — static, pure, the three arms above.
- `clear(UUID)` — the command's "print and clear".

**A sample spanning two weapons is not a reading.** If the weapon changed mid-sample the readout
prints **MIXED — retake it**, not a number. That is the control for the *"two weapons disagreeing is
a finding"* requirement: it cannot be a finding if one sample silently averages both.

### 2 · `paper/weapon/WeaponFire.java` — two calls, one funnel

Both counters go in `WeaponFire.attempt`, which is the **narrowest point every successful weapon
fire passes through**, has `player`, `input` and `weapon` in scope, and runs on the player's own
thread before the region hop.

| counter | site | why there |
|---|---|---|
| **INPUT** | immediately after the binding check (`:83`) | the click arrived and this weapon binds it — **before** the broken gate, the quiver gate and the cooldown. No gate can hide it. |
| **FIRE** | inside `result.ifPresent` on `CastResult.Success` (`:134-135`) | reached only on a real success; every refusal has already returned. |

**THE CANCELLED-BINDING CLAUSE IS SATISFIED BY CONSTRUCTION, AND THE EXCEPTION LIST IS PROVABLY
COMPLETE.** `RpgListeners:485` cancels vanilla exactly when `attempt` returns present. After the
input counter the paths are: broken → **present**, quiver refusal → **present**, `isVanillaDriven` →
**empty**, then `return result` — and **`result` cannot be empty**, because `WeaponService.fire` is
`weapon.trigger(input).map(...)` and the line above has already proven `trigger(input)` present. So
`isVanillaDriven` is the **only** empty-returning path after the counter. That is *there cannot be a
second*, not *I looked and found one*.

**AND A RANGED WEAPON CANNOT REACH IT BECAUSE ITS CAST IS NOT MELEE — NOT BECAUSE OF THE INPUT.**
`BasicMelee.isVanillaDriven` is `isBasicAttack(onHit) && cast instanceof CastSpec.Melee`
(`BasicMelee:40-43`); **the input is not in the predicate**, so nothing stops a weapon binding
`right_click` to a Melee cast and reaching it. The conclusion survives only because *ranged* means
Ray or Projectile, never Melee. The first draft of this plan attached the true conclusion to the
wrong reason — *"it is a left-click concern"* — which is the sixth shape at small scale, in the
sentence carrying the plan's only by-construction argument. **The cast-shape reason stays true if
someone later binds a melee ability to `right_click`.**

### 3 · `paper/command/RpgCommand.java` — `/rpg firerate`

`Permissions.DEV`, on `healthregen`'s optional-argument shape. Prints through the **same core
functions the gate path uses** — the house rule for a dev instrument.

```
Fire cadence — quiver_stone, right_click
  INPUTS  count 47  window 188t  mean 4.09t  min 4t
  FIRES   count 17  window 187t  mean 11.69t min 11t
  cooldown_ticks 11 authored, 11 effective (not a basic attack — attack speed does not scale it)
  COOLDOWN-LIMITED — fires slower. Q7's answer is the INPUT number: min 4t, mean 4.09t.
Sample cleared.
```

`cooldown_ticks` is printed **authored and effective**, composed through
`AttackSpeed.effectiveCooldownTicks` — because for a `weapon_damage` basic attack like
`hunters_bow` the two differ, and the authored number alone would not explain the fire figure.

### 4 · `GATE-q7.md` — one row, which is the point

**A new single-row gate, not a section in an existing one.** Q7's entire failure mode is being
absorbed into a blanket over sixteen other rows; **a gate with one row cannot be blanketed.** It
carries the struck recipe, the surviving cancelled-binding clause, the three verdict arms, and the
instruction that **the operator takes the reading — I cannot.**

One pointer line added to `GATE-quiver-a2.md`'s Q7 section so the carry-forward does not dead-end.
`GATE-quiver.md` (A1, merged) is left untouched.

---

## TWO MEASURED FACTS THAT CHANGED THE DESIGN

**`CastSpec.minimumCooldownTicks` is 0 for Ray, Projectile, Melee and Self — and that is a
MEASUREMENT SOMEBODY ALREADY TOOK, not a re-derivation.** `WeaponLoader:154-157` records it as
*"Measured, by invoking it: Ray, Projectile, Melee and Self all return 0; only Volley derives one
(30, for the Cursed Emerald windup 20 + (6-1) x 2)."* This plan stands on that measurement. So a weapon with a low `cooldown_ticks` has no hidden floor under it,
and **Q7 can be answered on shipped content, unmodified**: `quiver_stone` (11) and `hunters_bow` (15)
will both report COOLDOWN-LIMITED while their INPUT numbers answer Q7 directly. This is what makes
the fixture edits unnecessary rather than merely undesirable.

**`hunters_bow`'s cooldown IS attack-speed scaled and `quiver_stone`'s is not** — the bow's `on_hit`
is `weapon_damage` so `DamagePayload.isBasicAttack` is true; the stone's is a plain `damage` effect
so its 11 ticks are used verbatim (`AbilityService:205-209`). The readout must therefore print both
figures or the fire number will look wrong on one of the two weapons.

## AN EXPECTATION RECORDED BEFORE THE READING, SO IT CANNOT BE RETROFITTED

Vanilla's client gates held use on `rightClickDelay`, which `startUseItem()` sets to **4 ticks** — so
the input floor is *expected* near 4t / 5 per second. **This is outside knowledge, not repo-derived,
and this repo has deliberately refused to assert it.** It is written down now, before the number
exists, for one reason: **if the reading disagrees, the reading wins and the disagreement is the finding.**

**AND THE OTHER OUTCOME IS NOT AN INSTRUMENT FAILURE — IT IS THE READING THAT MATTERS MOST.** An
item with a use action may latch into a use state and **stop repeating entirely**. That is the
NO REPEAT arm, and it is why it is a named verdict rather than a prose aside:

> **Crossbow-ness is the RISK, not the protection.** A use-action item is exactly the thing that may
> stop repeating; **cancelling the interaction is what might prevent the use state starting.** And
> `quiver_stone`'s `material: crossbow` is not there to protect the measurement — `PLAN-quiver.md`
> records why it is there: *"It must match the Boltor's configuration on TWO axes, not one."* **The
> fixture matches the shipping weapon.** That is the reason, and it is a better one.

**If NO REPEAT is what comes back, held-repeat does not work on the Boltor's material and ruling 1
is unimplementable as stated** — the ruling this entire slice is built on. That would be the single
most valuable thing Q7 could ever produce, which is why the operator is told it in words on the
readout instead of meeting it as a surprise.

**If the floor lands near 4t, slice C's 7-tick dual cooldown buys nothing** — the weapon would
already be input-limited above it, and the halving would be a tooltip claim rather than a mechanism.
That is the question Q7 was always for.

---

## A THIRD FACT, FOUND WHILE VERIFYING, THAT THE PLAN DID NOT HAVE

**THREE GATES CAN HOLD FIRES BELOW INPUTS, NOT ONE** — measured in `WeaponFire.attempt`'s own order:
**durability** (`Broken`, `:90`), **the magazine** (`Empty`/`Reloading`, `:106-116`) and **the
cooldown** (`AbilityService:203`). So `COOLDOWN-LIMITED` only means what it says on a weapon with no
magazine and undamaged durability.

**`hunters_bow` has no `quiver_size`** — checked — so the gate row reads it there **first** and
`quiver_stone` second. On the stone the magazine empties after nine and the fire count stops for a
reason that is not the cooldown; a row that took only that reading would mis-attribute it. The
verdict line says so where it is printed, not only in the gate.

## FILES

| file | change |
|---|---|
| `core/.../combat/FireCadence.java` | **new** — recorder, `Sample`, `Kind`, `Verdict` |
| `core/src/test/.../combat/FireCadenceTest.java` | **new** — the rows below |
| `paper/.../weapon/WeaponFire.java` | two `record` calls, one new parameter |
| `paper/.../listener/RpgListeners.java` | pass the recorder |
| `paper/.../packet/WeaponSwingListener.java` | pass the recorder |
| `paper/.../RpgPlugin.java` | construct with `Bukkit::getCurrentTick` |
| `paper/.../command/RpgCommand.java` | `/rpg firerate` + handler |
| `GATE-q7.md` | **new** — the single row |
| `GATE-quiver-a2.md` | one pointer line |
| `PLAN-q7.md` | **new** — this file |

**`FireCadence` is passed as a 7th parameter to `WeaponFire.attempt`, NOT added to
`AdapterContext`.** That record is positional with fifteen components including an adjacent
same-typed pair (`ImmobilizeStatus immobilize, … ImmobilizeStatus freeze`); A2 spent a commit
converting an eight-double positional signature to a builder precisely to stop adding to lists like
that. Seven parameters of seven distinct types cannot transpose.

**No content file is touched.**

## VERIFICATION

- `./mvnw -pl core test` per edit; `./mvnw clean package` as the final verify, suite quoted as
  `core / storage / paper` and **re-read after the last file lands**.
- **`FireCadenceTest` rows**, each naming what it forces red:
  - the mean is `window / (count − 1)`, driven by a fake clock
  - **the minimum is the smallest gap, not the mean** — staged jittery (4, 4, 1, 4 ⇒ min 1, mean 3.25)
  - the window is **first event to last**, not "now minus first" — staged by advancing the clock
    after the last event and asserting the window does not grow
  - `count == 1` reports **no interval**, not `0`
  - an unrecorded sample is **absent**, not a zero sample
  - a weapon change mid-sample sets `mixedWeapons`
  - **the verdict's three arms, including INSTRUMENT FAULT** — the control
- **Mutations**, each spliced by line number, marker grepped both directions, byte delta derivable,
  restored from a scratchpad copy, **and the region printed back**:
  - `MUTWINDOW` — window measured to `currentTick` instead of the last event
  - `MUTMEAN` — divide by `count` instead of `count − 1`
  - `MUTMIN` — report the mean where the minimum belongs
  - `MUTFAULT` — collapse the INSTRUMENT FAULT arm into COOLDOWN-LIMITED

  **RUN, all four red, deltas derivable from the edit:** `MUTMEAN` **+5** (−6 + 11, **3 of 9 red**),
  `MUTWINDOW` **+20** (+7 + 13, reading `400` where `8` belongs), `MUTMIN` **+28** (+18 + 10,
  reading `3` for a floor of `1`), `MUTFAULT` **+12** (0 + 12, the control reporting a broken
  instrument as a measurement). Each restored byte-identical by `cmp`; `markers left: 0`.
- **Boot-only and recorded as such:** both `record` call sites need a live `Player`. The gate row is
  their only witness.

## WHAT HAPPENS AFTER THIS COMMIT

**Stop.** The operator takes the reading — I cannot, and `GATE-q7.md` says so. Slice B's plan
follows the number, with `attack_damage`, `reload_ticks` and `cooldown_ticks` proposed against a
measured rate rather than an estimate.
