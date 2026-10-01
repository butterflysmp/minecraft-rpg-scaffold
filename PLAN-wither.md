# PLAN — WITHER: OUR WITHER STATUS, THEN THE WITHERED SHORTBOW (SURVEY)

**Phase 1: SURVEY ONLY. No Java, no content, no boot.** This file covers two slices, and both are built
**after the #168 → #169 → #170 → #171 stack merges**, as LEGACY-B is *(superseded 2026-10-01: they stack on #171 now,
above LEGACY-B; see RULINGS, 2026-10-01)*:

1. **WITHER-STATUS**: Wither becomes a status of OUR OWN, shaped like Scorch: our clock, our damage path, a cap,
   credit to the applier, and no armour. The `wither` element gains `applies_status`, as `fire` has `scorch`.
2. **WITHERED-SHORTBOW**, stacked on (1): an Uncommon ranged bow of element `wither`. Its Wither comes from the
   element, so it needs no extra `on_hit` status.

**Ben rules every number.** The seat's rulings WS1–WS4 decide the mechanism; this file surveys within them.

**Read against:**
- `feat/legacy-a` at **`ab23ce6f`** (#171, the stack top; none of #168–#171 merged). Both slices build on that tree.
  `content/weapons/short_bow.yml`, which the bow cites, exists **only** on the stack, not on `master`.
- the pinned jars, with `javap` from `~/.jdks/openjdk-26.0.1`:
  - the mojang-mapped server jar `run/versions/26.1.2/paper-26.1.2.jar`. This is the Paper-patched server, not pure
    vanilla;
  - `paper-api-26.1.2.build.74-stable.jar` (`pom.xml`'s `paper.version`), and its `-sources.jar` for parameter names.
- the old project at **`cfde822`**. It is not an object in this repo; it was read in the old project's checkout
  (`BSMPMenu`).

**Citations** name a CLASS and a METHOD or a SECTION, never a line. `P/` = `paper/src/main/java/io/github/butterflysmp/rpg/paper/`,
`C/` = `core/src/main/java/io/github/butterflysmp/rpg/core/`, `content/` = `paper/src/main/resources/content/`.

**Each claim is marked:** *read* (I opened the source or the bytecode), *inferred* (reasoned from the code around it)
or *UNVERIFIED* (not read). **Client-side rendering is UNVERIFIED throughout**, because the server jar carries no
renderer and no `Gui`. Arithmetic labelled **arith** is hand arithmetic on authored constants, **not a server reading**.
Every number a slice would author that is not already ruled reads **`___ (Ben)`**, with a PROPOSED or PROVISIONAL value
beside it.

---

## RULINGS

### BEN, 2026-09-30, VERBATIM

```
"let's write up a message for a new weapon, the Withered Shortbow. This will be an Uncommon
 ranged weapon of Wither element. It should apply Wither to the target on hit."

"let's make our Wither effect work. It should function fairly similar to Vanilla Wither does
 and how our scorch already does. Damage over time based off of the damage of the weapon used
 to inflict. Difference mobs Don't explode when killed by wither"
```

### THE SEAT'S RULINGS ON MECHANISM, 2026-09-30 (summarised; the brief is not committed)

- **WS1. Wither is OUR status, shaped like Scorch.** It is not the vanilla potion doing damage. It has our clock, our
  damage path, a cap from the hit that applied it, credit to the most recent applier, and armour does not reduce the
  tick. Where the machinery is the same as Scorch's (the clock, the credit, the cap), it is **shared, not forked**. The
  survey recommends either a new `kind:` or a parameterised scorch kind (section 3).
- **WS2. Wither deaths never explode.** Ignite keys on SCORCH only. The survey shows where Ignite reads it, and the
  slice owes a unit test and a gate row (section 4).
- **WS3. Wither sits on the ELEMENT.** `content/elements/wither.yml` gains `applies_status`. The status id is this
  file's proposal; the display name is Ben's ("Wither") (section 5).
- **WS4. The look is Ben's choice.** This file surveys the options and does not pick one (section 6).

### THE SEAT'S RULINGS ON THIS SURVEY, 2026-09-30 (after reading `a0b3cd55`)

- **A correction, from the seat.** WS1's *"the cap = the damage of the hit that applied it"* was loose. Scorch's ruled
  cap is `CAP_FRACTION 0.5`, and **Q-W2 stays Ben's** (section 3.4 stands as written).
- **WS1: ACCEPTED as recommended.** `kind: wither`; a shared core `DotRates` record; **one** parameterised paper store,
  created twice (`scorch()`, `wither()`). **Ignite reads only the scorch instance.**
- **S1: NO TEMP fixture.** WITHER-STATUS and WITHERED-SHORTBOW **stack and boot together, at one stack top**. The
  weapon-capped path is read in the bow's gate (the WB rows) **in the same boot**. The status gate uses `/rpg apply`
  for the clock and percent rows.
- **S2: Ignite's `depth` stays SCORCH-ONLY.** It does not go on the shared store.
- **S3: YES, a trace line.** **`DOTTICK`** under `/rpg mobtrace`, carrying: the target's uuid and type, the status id,
  the applier's uuid, the amount sent, the cap in force, and the tick time. It is emitted **for BOTH scorch and
  wither, from the shared store**, in **its own commit with a unit test**. The tick rows become **log-line rows**,
  and the popup becomes a secondary witness.
- **Order:** Ben's answers to Q-W1 to Q-W10 come next. **Nothing is built until #168–#171 merges.**
  *(Superseded 2026-10-01: the slices stack on #171 now. See *2026-10-01* below.)*

The sections below keep their survey text. Where a ruling settles them, they carry a `RULED` note and are not
rewritten.

### BEN, 2026-09-30, VERBATIM: the answers to Q-W1 to Q-W10

```
"8, yes everything else is fine"
```

**How the seat read it.** "8, yes" answers Q-W8. "Everything else is fine" accepts every PROPOSED value that had
one:

| | ruled |
|---|---|
| **Q-W1** | **a tick every 40 ticks (2 s)** |
| **Q-W2** | **`min(5% of max, cap)` per tick, the cap HALF the applying hit** (`CAP_FRACTION 0.5`, as Scorch) |
| **Q-W3** | **lasts 200 ticks (10 s)**, so 5 ticks (`ceil(200/40)`) |
| **Q-W4** | **a re-hit RESETS the timer, as Scorch** (newest cap and credit, the clock not restarted); **no stacking** |
| **Q-W5** | **it hurts players (PvP).** *Live only when PvP is: nothing can hit a player with a weapon today (section 8)* |
| **Q-W8** | **"yes": a mob that is scorched AND withered and dies to a Wither tick STILL EXPLODES.** Ignite stays "died while scorched", unchanged. **WS2's guarantee narrows to: a NEVER-SCORCHED mob killed by Wither does not explode.** The gate carries the negative row (never scorched) and a scorched+withered positive row |
| **Q-W9** | **the bow at 9.1's PROVISIONAL column**: damage 16, cooldown 16, quiver 8, reload 60 (the Short Bow's). The rest of that column, which "everything else is fine" also covers, is speed 3.0, gravity 0.05, lifetime 120 and knockback 0.1. **To be tuned later** |

**THE SEAT FILLED THREE QUESTIONS THAT HAD NO PROPOSAL.** Ben has been told and can overrule at the gate:

- **Q-W6: wither skeletons, the Wither and the Knell are IMMUNE** (vanilla-like). The Knell is a `wither_skeleton`,
  so **an entity-type list `[wither_skeleton, wither]` covers all three**. Here a type key is the ruling, not the
  per-entity trap `MobDefinition` warns about, because every wither skeleton is meant to be immune. *Phase 2:*
  author the list in `statuses/withering.yml` (content as data), not in Java.
- **Q-W7: option (a), the vanilla WITHER potion for its look only, with its own damage suppressed** (a gate in
  `onEnvironmentalDamage` beside FIRE_TICK's: cause `WITHER` && `wither().isWithered(id)` → token, before
  `damageWindow.claim`). The immune mobs are exactly the ones Paper refuses the potion on, so (a) loses nothing.
  Section 6.4's consequences carry over unchanged, including that the gate would swallow a real wither skeleton's
  vanilla wither on a player who is under ours too. **That case is only live once PvP is.**
- ~~**Q-W10: the bow's tooltip gets an "Inflicts Wither" line** (Ben may change the wording), and the status gains a
  display name "Wither" if the schema needs one. **It does, and section 5.3 says what that costs.**~~
  **OVERRULED by Ben, 2026-09-30: no such line** (see *2026-10-01* below).

### 2026-10-01: BEN OVERRULES Q-W10, THE SEAT RE-STAGES THE CAP ROWS, AND THE SLICES STACK NOW

**Ben, overruling the seat's Q-W10 fill (2026-09-30), verbatim:**

```
"no i don't want a tool tip that says it inflicts wither"
```

- **There is NO "Inflicts Wither" line.** Section 5.3's T1 and T2 are both **WITHDRAWN**: no status `display_name`,
  no `StatusDefinition` field, no `WeaponLore` change, no `StatusRegistry` passed to `WeaponLore.build`.
- **Q-W11 is MOOT.** Fire weapons are untouched, because nothing names a status.
- **WB1b is DELETED.** WB1 predicts the bow's tooltip with **only its element** naming Wither.

**The seat, the same day: the cap rows use MOB SCALING, not a hunt for a big mob.** The warden is dropped. A vanilla
hostile mob's max is `base × 5 × gs / 100` (`MobScaling.maxHealth`, *read*), so **a zombie's max equals its gear
score** (arith: `20 × 5 × gs / 100`). Both rows are **WITNESSED**, not conditional:

| row | the binding case | the control (the percent arm) |
|---|---|---|
| **WS2b** (`/rpg apply`, cap 2.0) | a **GS-100 zombie** (max 100; 5% = 5 > 2.0): each tick **2.0** | **`/rpg spawn zombie 20`** (max 20; 5% = 1.0 < 2.0): each tick **1.0** |
| **WB4b** (the bow, cap `0.5 × 16 = 8`) | **`/rpg spawn zombie 200`** (max 200; 5% = 10 > 8): each tick **8.0** | a **GS-100 zombie** (5% = 5 < 8): each tick **5.0** |

**Each DOTTICK amount is predicted from that mob's own `MOBSEED … max=` line, read in the boot**, as
`min(0.05 × max, cap)`, never from the table's arithmetic. The table is what the seat expects the `MOBSEED` lines to
say. A `max=` that differs from it is a reading to record, and the prediction follows the line.

**The seat, the same day: the slices no longer wait for #168–#171 to merge.** The stack is
`#171 (ab23ce6f) → LEGACY-B → WITHER-STATUS → WITHERED-SHORTBOW`, each with its own PR and its own GATE committed before
any boot. **One boot, at the new stack top, after the seat has diffed every slice.** Each R0c predicts against the slice
below. Nothing in #168–#171 changes.

### WHAT THE ANSWERS DO TO THE GATE (they move rows; the re-draft is in section 10)

- **The Knell is immune, so it can no longer be the large target that shows the cap binding.** The cap binds when
  `0.05 × max > cap` (arith):
  - under `/rpg apply` (cap 2.0), any mob with max > 40;
  - under the bow (cap `0.5 × 16 = 8`), a mob with max > 160.
  
  The one shipped custom mob is immune. **A vanilla candidate is a warden** (vanilla max 500), but whether mob scaling
  moves its custom max is NOT predicted. **The row reads the max off the nameplate in the boot**, then predicts from
  that.

### STANDING RULINGS THIS SURVEY TOUCHES, NOT RE-DERIVED HERE

- **No custom item stacks above 1.** The bow is minted through `WeaponItems.mint`, so it inherits the rule.
- **New content cites the Boltor**, not `hunters_bow`. The bow below cites `short_bow.yml` and the Boltor only.
  **Cooldowns are authored in multiples of 4.**
- **A travelling ranged weapon authors knockback.** The bow is `type: projectile`, so it declares `- type: knockback`.
- **Ignite: any mob that dies while scorched ignites** (2026-09-09; `C/combat/Ignite` class javadoc). This is binary:
  the killing blow need not be a fire hit. Section 4 shows how that ruling meets a mob that is both scorched and
  withered.
- **Scorch's cap is HALF the hit** (`Scorch.CAP_FRACTION = 0.5`). Ben ruled it from a measured burn (the `Scorch`
  javadoc on `CAP_FRACTION`). This does not match the literal words "the cap = the damage of the hit"; see section 3.4.

---

## 1. WHAT I READ

| file | what for |
|---|---|
| `C/combat/Scorch` | every rate: `RATE_PER_SECOND 0.05`, `PERIOD_TICKS 20`, `DEFAULT_DURATION_TICKS 120`, `CAP_FRACTION 0.5`, `UNDECLARED_CAP 2.0`; `damagePerTick`, `stacksFor`, `damageTicksFor` |
| `C/combat/Ignite` | the blast; `detonate` and its arguments |
| `P/adapter/ScorchStatus` | the state: `Active` (task, remaining, cap, applierId, element, depth); `apply`, `isScorched`, `applier`, `depth`, `forget` |
| `P/adapter/EntityScorchSink`, `ScorchSink` | the tick's damage path: `applyDamage(..., DefenseRule.BYPASSED, element, HitAccrual.inert())` |
| `P/adapter/ElementAccrual` | `forHit`, `accruesScorch`, and the private `scorch(...)` that applies `CAP_FRACTION` |
| `P/adapter/BukkitCombatant` | `applyDamage`'s accrual site and **Ignite's second clause**; `applyStatus`'s Scorch arm |
| `P/listener/RpgListeners` | `onEntityDeath` (**Ignite's first clause**); `onEnvironmentalDamage`'s FIRE_TICK suppression; `forget` on removal and respawn |
| `P/content/StatusDefinition`, `StatusLoader` | the sealed kinds and the `kind:` parse |
| `P/content/ElementDefinition`, `ElementLoader`, `ContentValidator.validateElements` | `applies_status` and its boot check |
| `P/health/VanillaDamagePolicy`, `MobHealPolicy`, `VanillaHealPolicy` | what happens to vanilla `WITHER` today |
| `content/elements/{wither,fire}.yml`, `content/statuses/scorch.yml`, `content/weapons/{short_bow,boltor}.yml`, `content/mobs/knell.yml` | content |
| `GATE-legacy-a.md`, `GATE-ignite.md` | gate shapes: the PLAYERHIT line, the test zombie, I7's negative control |
| `C/combat/TracedHit` | what PLAYERHIT covers |
| jar: `WitherMobEffect`, `DamageTypes`, `CraftEventFactory`, `WitherSkeleton`, `WitherBoss`; API: `DamageCause`, `PotionEffect`, `EntityPotionEffectEvent` | vanilla wither |
| `run/config/paper-world-defaults.yml` | wither immunity switches |

---

## 2. WHAT SCORCH IS TODAY (*read*)

Scorch has its arithmetic in core and its state in paper. **These are the parts WS1 says to share:**

| property | where | value / rule |
|---|---|---|
| clock | `ScorchStatus.apply` → `RepeatingTask.start(target, Scorch.PERIOD_TICKS, "scorch", ...)` | 20 ticks. The first burn lands one period after application, never on application. The tick burns and then decrements |
| per tick | `Scorch.damagePerTick(max, cap)` | `min(0.05 × victim max, cap)` |
| cap | `ElementAccrual.scorch(...)` | `declaredMagnitude × CAP_FRACTION`. `declaredMagnitude` is the hit without its crit, after charge, before Defense, and gear-score-scaled by `EffectApplier` |
| how long | `ElementAccrual.scorch(...)` | `Scorch.DEFAULT_DURATION_TICKS` = 120, which is 6 ticks (`damageTicksFor`) |
| gate | `Scorch.stacksFor(dealt)` | a hit that landed > 0 buys ≥ 1 stack. **The count is read only as yes/no**; nothing accumulates it |
| refresh | `ScorchStatus.apply`, refresh arm | the newest application rewrites `remaining`, `cap`, `applierId` and `element`. **The task is NOT restarted**, so a weapon hitting faster than the period still ticks (`reApplyingFasterThanThePeriodStillTicks`) |
| credit | `Active.applierId` → the sink's `deal(amount, applierId, element)` | the most recent applier |
| armour | `EntityScorchSink.deal` | `DefenseRule.BYPASSED` |
| loop guard | `EntityScorchSink.deal` | `HitAccrual.inert()`: the tick wears its element and never accrues again |
| lethal gate | `ElementAccrual.forHit` | `newCurrent <= 0` → no accrual (the ordering inversion with `forget`) |
| cleanup | `RpgListeners` | `forget` on mob removal, player quit and player respawn |
| **Ignite-only** | `Active.depth`, `ScorchStatus.depth(UUID)` | the chain depth. **No other mechanism reads it** |
| **fire-only** | `setFireTicks` in `BukkitCombatant` (two sites) + the FIRE_TICK gate in `onEnvironmentalDamage` | the look, and the suppression of vanilla's damage that the look carries |

---

## 3. WS1: A NEW `kind:`, OR A PARAMETERISED SCORCH KIND?

### 3.1 Everything that keys on "this is scorch" today (*read*)

| # | site | question it asks | what a parameterised `kind: scorch` wither would do |
|---|---|---|---|
| K1 | `ElementAccrual.accruesScorch`: `case StatusDefinition.Scorch -> true` | does this element's hit accrue? | **true for wither**, so wither hits accrue |
| K2 | `BukkitCombatant.applyDamage`, Ignite's **second clause** (`!wasScorched && dealt > 0 && newCurrent <= 0 && accruesScorch(...)`) | did a fire hit kill an unscorched mob? | **true for a lethal wither ARROW**, so the bow's killing shot **explodes** |
| K3 | `RpgListeners.onEntityDeath`: `adapters.scorch().isScorched(id)` | did a scorched mob die? | wither accrual goes into the one `ctx.scorch()` store, so **every withered mob that dies explodes**, from any cause |
| K4 | `BukkitCombatant.applyDamage`, the accrual lambda: `entity.setFireTicks(...)` then `ctx.scorch().apply(...)` | (unconditional once K1 passes) | **a withered mob catches fire visually**, and K5 then hides its FIRE_TICK |
| K5 | `RpgListeners.onEnvironmentalDamage`: `FIRE_TICK && adapters.scorch().isScorched(id)` | is this vanilla burn ours? | suppresses FIRE_TICK on withered mobs |
| K6 | `ContentValidator.validateElements`: `case Scorch -> { }` | can this status accrue? | passes |
| K7 | `BukkitCombatant.applyStatus`: `case StatusDefinition.Scorch` | the explicit / `/rpg apply` route | sets fire ticks and applies to `ctx.scorch()` |

**Under a parameterised kind, WS2 (no explosion) and the look are broken in five places at once (K2, K3, K4, K5,
K7).** Each would need an `if (kind is really scorch)` flag. That flag would be a Scorch special case in every
consumer, and a forgotten one fails silently: **a wither death that explodes looks exactly like the Ignite ruling
working.**

### 3.2 RECOMMENDATION: A NEW KIND, `kind: wither`, ON SHARED MACHINERY

- **A new sealed record `StatusDefinition.Wither(String id)`**, parsed from `kind: wither` in `StatusLoader.parse`.
  The three exhaustive switches over `StatusDefinition` (`ElementAccrual.accruesScorch`,
  `ContentValidator.validateElements`, `BukkitCombatant.applyStatus`) become **compile errors** until each one decides
  about Wither. That is the property worth having. The sealed type is *why* a new kind is safer here: the compiler
  lists the decisions.
  > *For Ben:* a "sealed interface" is a Java type whose list of sub-types is closed, so a `switch` over it must
  > handle every one, or it does not compile.
- **The clock, refresh, credit, cap and armour bypass are SHARED, not copied.** The proposed shape:
  - **core:** a `DotRates` record (period, rate per tick, cap fraction, default duration) owning `damagePerTick` and
    `damageTicksFor`. `Scorch` exposes its constants as one `DotRates` instance and a new `C/combat/Wither` exposes
    the other. **Each status's numbers live in its own file**, which keeps `Scorch`'s rule that the person tuning a
    status finds every rate in one place. The core unit test is written first (CLAUDE.md).
    > *For Ben:* a "record" is a small Java class that only holds values, declared in one line.
  - **paper:** `ScorchStatus`'s map-and-clock body becomes a store parameterised by `DotRates`. AdapterContext then
    holds **two instances**: `scorch()` and `wither()`. **Two instances is the WS2 guarantee:** `isScorched` (K3, K5)
    asks the scorch instance, which a wither application never touches.
- **What is NOT shared:**
  - `depth` belongs to Ignite. Kept on the shared store, the wither instance would carry a field that nothing reads,
    and `Scorch`'s javadoc refuses that shape (*"a write-only field is worse than a deleted one"*). **Phase 2
    decision:** either `depth` stays scorch-only (a subclass or a wrapper), or the shared store carries it with the
    wither instance always passing 0 and saying so. This file leans to the first, and does not rule it.
    **RULED (S2): scorch-only. `depth` does not go on the shared store.**
  - `setFireTicks` and the FIRE_TICK gate stay scorch's. Wither's look is section 6.
- **Accrual becomes a dispatch on the kind.** Today `ElementAccrual.forHit` → `ScorchAccrual` →
  `ctx.scorch().apply` is scorch from end to end. It becomes "which store does this element's status feed",
  answered by the same sealed switch. **`accruesScorch` must keep its name and its meaning.** It is Ignite's
  predicate (K2), so the wither path gets its own predicate, or the switch returns a kind. **It must never answer
  true for wither.**

### 3.3 Prose that becomes false, and that the WITHER-STATUS slice must edit in the same commit

- `content/elements/fire.yml`: *"FIRE IS THE ONLY ELEMENT THAT DECLARES THIS. The other six accrue nothing."*
- `ContentValidator.cannotAccrue`: *"Only scorch can be accrued from damage today"*. This is a **player-visible
  boot warning**.
- `ElementAccrual`'s class javadoc and `accruesScorch`'s *"An element that gains `applies_status: scorch` tomorrow
  ignites tomorrow"*. The sentence stays true; the surrounding text assumes scorch is the only accruing status.
- `ScorchStatus`'s class javadoc, wherever the parameterisation moves text.

### 3.4 THE CAP: "THE DAMAGE OF THE HIT" OR SCORCH'S HALF?

WS1 and Ben's words say the cap is *"based off of the damage of the weapon used to inflict"*. **Scorch's cap is
`0.5 ×` that** (`CAP_FRACTION`). "Share the cap with Scorch" and "the cap = the damage of the hit" are therefore two
different numbers. **This is Ben's call (Q-W2), and it is a field of `DotRates`**, so either answer costs the same.

---

## 4. WS2: NO DEATH EXPLOSION

### 4.1 Where Ignite reads "scorched" (*read*)

Ignite has **two triggers, and they are disjoint by design** (`BukkitCombatant.applyDamage`'s comment *"One question,
two reaches"*):

1. **`RpgListeners.onEntityDeath`**: `if (!adapters.scorch().isScorched(id)) return;` then `applier`, `depth`,
   `forget`, `Ignite.detonate`. This covers **any** death of a scorched mob.
2. **`BukkitCombatant.applyDamage`, the second clause**: a hit that killed a mob that was **not** scorched ignites it
   if `ElementAccrual.accruesScorch(...)` is true for the hit's element.

**Under the recommended shape, a wither death fires neither trigger:**
- (1) is false because wither lives in the wither instance;
- (2) is false for the wither TICK because the tick carries `HitAccrual.inert()` and `accruesScorch` returns false on
  `!accrual.accrues()` first. It is false for the bow's lethal ARROW because `accruesScorch` returns false for
  `StatusDefinition.Wither`.

### 4.2 The unit tests owed (paper's unit suite, no server)

**Both triggers live in `paper/`, so the tests do too.** Core cannot see them (`Ignite`'s javadoc: *"core never learns
that anything died"*). Both are pure unit tests, the 2-second loop.

| test | asserts | the mutation that must turn it red |
|---|---|---|
| `ElementAccrualTest`: a wither element, a lethal hit | `accruesScorch(elements(wither → withering), statuses(Wither kind), "wither", ACCRUES)` is **false** | `case Wither -> true` in `accruesScorch` |
| a store test (`ScorchStatusTest`'s shape): apply to `wither()` | `scorch().isScorched(id)` is **false** and `wither().isWithered(id)` is **true** | AdapterContext handing one instance to both (`wither = scorch`) |
| the control in the same class | a fire element still returns **true** | (proves the row above is not blind) |

**`onEntityDeath`'s own ordering has no unit witness**, exactly as for scorch today (`ScorchStatus.depth`'s javadoc).
The gate row is the only witness for that ordering.

### 4.3 THE EDGE THE RULING DOES NOT SETTLE: SCORCHED AND WITHERED

Under the standing Ignite ruling (*any mob that dies while scorched*; the killing blow need not be fire), **a mob that
is ALSO scorched, and is killed by a Wither tick, explodes.** This follows from K3, not from wither. Ben's *"mobs don't
explode when killed by wither"* may mean to override that, or may not. **It is Q-W8.** Until it is answered, the WS2
gate row uses a mob that has **never** been scorched.

> **RULED 2026-09-30 (Q-W8, Ben: "8, yes"): it still explodes.** Ignite is unchanged. WS2's guarantee is exactly *a
> never-scorched mob killed by Wither does not explode*, and the unit tests in 4.2 already test exactly that.

---

## 5. WS3: THE ELEMENT, THE ID, AND THE BLAST RADIUS

### 5.1 Everything wither-element today (*read*, `ab23ce6f`)

- **`content/elements/wither.yml`**: two keys, `display_name: "<dark_gray>Wither</dark_gray>"` and
  `damage_symbol: "<dark_gray>✖</dark_gray>"`. **No `applies_status`.**
- **No content uses the element.** Counting `element:` values across `content/`: fire 33, kinetic 13, void 8,
  nature 2, **wither 0**. `grep -ril wither` over `accessories armor aspects builds enchants fragments recipes shields
  tools visuals abilities` returns nothing (exit 1). The three weapon files that mention "wither" (`cursed_emerald`,
  `lapis_staff`, `locust`) do so **in comments listing the seven elements**.
- **Tests** list `"wither"` as one of the seven element ids (`ContentValidatorTest`, `ElementLoaderTest`,
  `WeaponLoreTest`). **None pins wither's `appliesStatus`.** `ElementLoaderTest`'s bundled row pins fire = scorch and
  kinetic = null only.
- **Code:** no element-`"wither"` literal in `P/` or `C/`. The other code hits are the vanilla entity (the Knell is a
  `wither_skeleton`), the damage and heal policy tables, and prose.

**So the blast radius of `wither.yml` gaining `applies_status` is the new bow alone**, plus the Wither-element
`damage_symbol` that its ticks will now draw.

### 5.2 The status id

**The namespaces do not meet in code.** `ElementRegistry` and `StatusRegistry` are separate, and fire → scorch already
uses different ids (*inferred* from the two registries; I found no lookup that crosses them). **They do meet in people:**
`/rpg apply <status>` takes a status id while every weapon file writes `element: wither`, and the gate rows quote
both. **PROPOSED: `withering`** (`content/statuses/withering.yml`, `kind: wither`). Other candidates: `withered`,
`blight`.

**The display name "Wither" has no surface to land on today.** `StatusDefinition` carries an id and a kind and nothing
else (*read*). No status file has a `display_name`, and no tooltip line names a status: `WeaponLore` renders the
element word and nothing about what it applies (*read*: no status reference in `WeaponLore` or `WeaponLoreLines`).
**The player-visible "Wither" today is the element's word on the tooltip.** Giving statuses a display name is new
schema, which makes it Q-W10.

### 5.3 WHAT "INFLICTS WITHER" COSTS (Q-W10, filled by the seat; *read* unless marked)

> **WITHDRAWN 2026-09-30: Ben overruled Q-W10** (*"no i don't want a tool tip that says it inflicts wither"*). Neither
> T1 nor T2 is built, and Q-W11 is moot. The costing below is kept as it was written.

**Today:** `WeaponLore.build(weapon, elements, ...)` takes the `ElementRegistry` and **no `StatusRegistry`**.
`elementLine` renders the element's `display_name`. `StatusDefinition`'s five records carry `id` (and, for two of them,
a mechanic field) and **no display name**. **So the line can be had two ways, and they cost very different amounts.**

**T1. DERIVED (the mechanic writes the line):** weapon element → `applies_status` → the status's `display_name` →
"Inflicts Wither".
- **schema:** an optional `display_name` in a status file. `StatusLoader.parse` reads it, and `StatusDefinition`
  gains it, as a field on all five records or as one interface method. **Every test that constructs a status record
  changes** (`StatusLoaderTest`, `ElementAccrualTest`, `ContentValidatorTest`, and so on); Phase 2 counts them.
- **plumbing:** `WeaponLore.build` gains the `StatusRegistry`, and `WeaponItems`, its one production caller, passes
  it, along with the tests that build lore.
- **THE BLAST RADIUS IS THE DECISION.** If `scorch.yml` gets a `display_name` too, **five shipped fire weapons**
  (`dragons_breath`, `ember_staff`, `emberblade`, `flint_staff`, `hunters_bow`) gain "Inflicts Scorch", and
  `golden-lore.txt` moves for each. If only `withering.yml` gets one (the line renders only when a name is present),
  fire weapons are unchanged and **wither is the only element whose tooltip names its status**. That inconsistency is
  scoped but visible. **Ben's: do fire weapons say "Inflicts Scorch" too? (Q-W11)**
- **It cannot lie.** The line exists exactly when the element accrues a status. Remove the accrual and the line goes
  with it.
- **One subtlety:** accrual reads each damage effect's `element:` and the line would read the weapon's top-level
  `element:`. For this bow the two agree (`wither` and `wither`). *Inferred:* a weapon whose two elements differ would
  show a line its hits do not earn. Phase 2 states which one the line reads.

**T2. AUTHORED (the content file writes the line):** a weapon-level key or a flavor line reading "Inflicts Wither".
- **cost:** almost nothing as flavor, which is zero code but renders italic and gray, as flavor does. A new key costs
  a little more.
- **it can lie.** Nothing ties the words to the mechanic. That is the `flint_staff.yml` "STACKS TO 64" shape: correct
  on the day it is written, and unwatched afterwards.

**RECOMMENDATION: T1, with `display_name` optional and the line rendered only when the status has one.** The display
name "Wither" goes in `withering.yml`. Whether `scorch.yml` gets one is Q-W11. **Where the line sits** (directly under
the element line, or below the gear-score line that Ben placed under the element on 2026-09-21) is proposed in Phase 2
against `golden-lore.txt`, not here.

---

## 6. WS4: THE LOOK (surveyed; NOT picked)

### 6.1 How Scorch looks today (*read*)

Scorch borrows vanilla fire: `entity.setFireTicks(max(current, duration))` at both apply sites. **That look carries
damage.** Vanilla's `FIRE_TICK` would reroute into custom HP uncapped, crediting the victim, so
`onEnvironmentalDamage` tokens it away while `isScorched`. That is **a shared visual as a coupling, with three ends**
(both `setFireTicks` sites and the gate). The named debt beside it is that FIRE, LAVA and HOT_FLOOR are not suppressed.

### 6.2 How vanilla wither damage arrives (*jar*, `javap -c`)

- **The cause:** `WitherMobEffect.applyEffectTick` calls `damageSources().wither()` with `fconst_1` → `hurtServer`.
  That is **1.0 damage per application**, with no health floor, so **it can kill** (Poison's `applyEffectTick` guards
  `getHealth() > 1`; Wither's has no such check). `CraftEventFactory` maps `DamageTypes.WITHER` →
  **`DamageCause.WITHER`**. Damage type key: `minecraft:wither`.
- **The cadence:** `shouldApplyEffectTickThisTick(tick, amp)` is `interval = 40 >> amp`, then `tick % interval == 0`
  (or every tick when `interval <= 0`). That gives **40 ticks at Wither I**, 20 at II, 10 at III, 5 at IV, 2 at V, and
  every tick from VI. **I verified both methods' bytecode myself**, beyond the delegated reading.
- **Suppression is clean on the API side:** `EntityDamageEvent` implements `Cancellable`, so a cause-`WITHER` event can
  be tokened or cancelled like FIRE_TICK. `PotionEffect` has constructors `(type, duration, amp, ambient, particles,
  icon)`, so particles and the HUD icon can each be switched off. `EntityPotionEffectEvent` is `Cancellable` with
  causes including `ATTACK`, `PLUGIN`, `MILK` and `WITHER_ROSE`.
- **Immunity is a Paper switch, and it is ON:** `WitherSkeleton.canBeAffected` and `WitherBoss.canBeAffected` read
  `paperConfig().entities.mobEffects.immuneToWitherEffect.{witherSkeleton, wither}`, and
  `run/config/paper-world-defaults.yml` sets both to `true`. *A per-world override file was not read.* **So the
  vanilla potion CANNOT be applied to the Knell (a wither skeleton) or the Wither.** It is refused, so neither would
  show the vanilla look.
- **The vanilla source in play today:** `WitherSkeleton.doHurtTarget` applies `MobEffects.WITHER` for **200 ticks**
  (Wither I, 10 s), via `EntityPotionEffectEvent.Cause.ATTACK`. A player hit by a wither skeleton gets it today.
- **Black hearts: UNVERIFIED.** The heart renderer is client-side and not in the server jar. A mob has no heart bar, so
  black hearts can only ever show on a **player** (*inferred*).

### 6.3 What our policies do with vanilla `WITHER` today (*read*)

- **`VanillaDamagePolicy`:** `case POISON, WITHER, MAGIC -> Action.REROUTE`, so vanilla wither damage becomes custom-HP
  damage. A wither tick names no causing entity, so `attributableId` falls back to the victim (the same reason
  FIRE_TICK needed its gate). `VanillaDamagePolicyTest` pins `WITHER → REROUTE`.
- **`MobHealPolicy`:** `case ENDER_CRYSTAL, WITHER_SPAWN, WITHER -> REROUTE`. That is a **heal reason**, the Wither
  boss healing ("The boss heals"). Our status heals nothing, so this is untouched. `VanillaHealPolicy` PASSes it.
- **The policy table must not change for any option below.** A suppression is a fact about a VICTIM and lives beside
  the FIRE_TICK gate, not in the cause table, as that gate's comment says.

### 6.4 The options (each with its consequences; none picked)

| | (a) the vanilla WITHER potion, applied for its look only | (b) our own particles only | (c) both |
|---|---|---|---|
| what shows | the vanilla particle swirl on mobs and players; black hearts and the HUD icon on a player (*UNVERIFIED, client*) | whatever `content/visuals` authors (a visual id, like `ignite_blast`), on our clock or on its own cadence | both |
| new code | apply the potion beside our store, and a **WITHER suppression gate** in `onEnvironmentalDamage` (cause `WITHER` && `wither().isWithered(id)` → token, before `damageWindow.claim`, the FIRE_TICK shape) | a visual call on each tick, or a separate particle loop | both |
| the coupling | **three ends again** (apply, gate, expiry), and the potion's duration runs on vanilla's clock beside ours | none: the look carries no damage | (a)'s |
| Knell / Wither | **no look**: Paper refuses the potion (6.2). Our damage still ticks unless Ben rules immunity (Q-W6) | shows on everything | particles only on those two |
| side effects | a token tick every 40 t flashes the mob's hurt animation (*inferred* from the FIRE_TICK token; UNVERIFIED for wither). Milk clears the look on a player while our clock runs. **The gate also swallows a real wither skeleton's vanilla wither on a player who is also under ours** (FIRE's sibling-cause debt, in reverse) | no hearts, no HUD icon | (a)'s |
| "fairly similar to vanilla" | the closest look | the least | — |

---

## 7. DID cfde822 APPLY WITHER? (a mechanism fact, NOT a precedent)

**No vanilla wither effect anywhere** (*read*, delegated: `git grep` over the cfde822 tree in the old project). The only
hit for `PotionEffectType.WITHER|DamageCause.WITHER|DamageType.WITHER|MobEffects.WITHER` is a comment. Its "wither"
was **a scheduled-task DoT**, in two places:
- **`encounter/withered/WitheredDetonationListener`** (elite only): a `BukkitRunnable` every 20 ticks for 140 ticks,
  `target.damage(20.0, DamageSources.magic(source))`, with SMOKE particles. Its javadoc says it was chosen over
  `PotionEffectType.WITHER` *"so the DPS stays exactly 20/s regardless of how the vanilla wither amplifier-to-tick math
  has drifted"*.
- **The Withered Falchion** (`weapon/MeleeWeaponListener.applyFalchionWither`): a `BukkitRunnable` every 20 ticks doing
  plain `target.damage(dps)`, with SMOKE particles and a manual damage indicator. DPS and duration came from
  `WeaponFormulas` by level. No potion, so no black hearts.
- **There was no withered bow.** `git grep -i -E "wither[a-z_ ]*bow|bow[a-z_ ]*wither"` returns nothing. Wither was an
  ELEMENT there too (`Element.WITHER`, "☣"), carried by the Falchion, the Withered Knight set and Goldrot.

Recorded as fact only: **`BukkitRunnable` and a flat `damage()` are banned or superseded here** (CLAUDE.md), and the
numbers are not adopted.

---

## 8. FOR BEN: WITHER-STATUS NUMBERS

Every row is blank. The PROPOSED value is a starting point, chosen so the effect is "fairly similar to vanilla".

| | Scorch (ruled) | vanilla Wither I (*jar*) | **Wither** | PROPOSED, and why |
|---|---|---|---|---|
| tick interval | 20 t | 40 t (`40 >> 0`) | **`___ (Ben)`** | **40 t**: vanilla's level I |
| damage per tick | `min(5% max, cap)` | 1.0 flat | **`___ (Ben)`** | **`min(5% max, cap)`**. On a 20-HP mob, 5% is 1.0 per 40 t, **exactly vanilla Wither I** (arith) |
| cap | `0.5 ×` the hit (`CAP_FRACTION`) | none | **`___ (Ben)`** | **0.5 × or 1.0 ×**, see 3.4. Gear-score-scaled like Scorch's |
| how long | 120 t (6 ticks) | 200 t from a wither skeleton (`doHurtTarget`, *jar*) | **`___ (Ben)`** | **200 t = 5 ticks** (arith, `ceil(200/40)`): the vanilla wither-skeleton hit |
| first tick | one period after application | on the effect's tick grid | — | one period in, like Scorch (shared clock) |
| re-hit while active | refresh the whole timer, newest cap and credit, clock not restarted; **no stacking** | a stronger or longer effect replaces the old one | **`___ (Ben)`** | **Scorch's refresh** (shared) |
| armour | bypassed | — | ruled (WS1): bypassed | — |
| PvP | can apply to players (`ScorchStatus` skips nobody), but **nothing can scorch a player today** (`Scorch` javadoc; PvP is a deferred rules decision, `RpgListeners`) | yes | **`___ (Ben)`** | **yes, on the same terms**: it only goes live when PvP does |
| immunity | none | wither skeletons and the Wither (Paper switch, on) | **`___ (Ben)`** | **mirror vanilla** for wither skeletons and the Wither; **the Knell is Ben's call** (it is a wither skeleton) |

**What the PROPOSED row does, as arithmetic (arith; gear score 100, no crit, one hit):**

| target | Scorch from a 16-damage fire hit | Wither, cap 0.5 × 16 = 8 | Wither, cap 1.0 × 16 = 16 |
|---|---|---|---|
| 20-max mob | `min(1.0, 8)` = 1.0 × 6 = **6** over 6 s | `min(1.0, 8)` = 1.0 × 5 = **5** over 10 s | 1.0 × 5 = **5** |
| 360-max Knell, if not immune | `min(18, 8)` = 8 × 6 = **48** | `min(18, 8)` = 8 × 5 = **40** | `min(18, 16)` = 16 × 5 = **80** |

On a vanilla-sized mob, the cap choice changes nothing; it decides only the large targets, as for Scorch.

---

## 9. WITHERED-SHORTBOW

### 9.1 The file, as a sketch (NOT written)

`content/weapons/withered_shortbow.yml`. The shape is cited from **`short_bow.yml`** (an instant-fire projectile bow) and
**the Boltor** (the ruled ranger numbers). It does **not** cite `hunters_bow`.

```yaml
id: withered_shortbow
display_name: "Withered Shortbow"
element: wither
rarity: uncommon
class: ranger
material: bow
attack_damage: ___ (Ben)
quiver_size:   ___ (Ben)
reload_ticks:  ___ (Ben)
flavor: ___ (Ben, or none)
triggers:
  right_click:
    cooldown_ticks: ___ (Ben)     # a multiple of 4
    cast:
      type: projectile
      speed: ___
      gravity: ___
      max_lifetime_ticks: ___
      body: arrow
    on_hit:
      - type: weapon_damage
        element: wither           # the element is what applies Wither (WS3); no `type: status` here
      - type: knockback           # the standing ruling: a travelling ranged weapon authors knockback
        strength: ___
```

| key | Short Bow (provisional, `short_bow.yml`) | Boltor (ruled) | **Withered Shortbow** | PROVISIONAL |
|---|---|---|---|---|
| rarity | common | uncommon | **uncommon** (Ben) | — |
| element | kinetic | kinetic | **wither** (Ben) | — |
| attack_damage | 16 | 19 | **`___ (Ben)`** | **16**, the Short Bow's: the Wither ticks are what make it uncommon |
| cooldown_ticks | 12 | 16 | **`___ (Ben)`** | **16**: one step slower than the Short Bow to pay for the DoT. A multiple of 4 either way |
| quiver_size | 8 | 8 | **`___ (Ben)`** | 8 |
| reload_ticks | 60 | 60 | **`___ (Ben)`** | 60 |
| speed / gravity / lifetime | 3.0 / 0.05 / 120 | (a ray) | **`___ (Ben)`** | the Short Bow's |
| knockback strength | 0.1 | (none; it is a ray) | **`___ (Ben)`** | 0.1 |

**Refresh and fire rate (arith):** at a 16 t cooldown against a 40 t Wither period, a held bow refreshes the window 2–3
times per period. The shared store's "refresh does not restart the clock" is what keeps it ticking. That is exactly
the `reApplyingFasterThanThePeriodStillTicks` case, now at period 40.

### 9.2 Tests and counts the bow moves (*read*, the current values)

- `ExpandedQuiverContentInvariantTest.KNOWN_RANGER_WEAPONS`: 7 → **8**.
- `golden-lore.txt`: one rendering block more.
- R0c's `Loaded ...` line: **weapons 14 → 15**. WITHER-STATUS moves **statuses 4 → 5**.
- `KNOWN_FIRE_DAMAGE_SITES`: unchanged (a wither site is not a fire site). **Phase 2 checks this against the test's
  own discovery, not this sentence.**

---

## 10. GATE ROWS: DRAFT PREDICTIONS, NOT RUN

**The game mode is SURVIVAL on every row** (the creative-divergence register: creative removes costs, and the bow's
magazine is one). Set-up follows `GATE-legacy-a.md`: `spawn_mobs false`, `/time set 18000`, level 1, no armour or
accessories, `/rpg mobtrace` ON, gear score 100, and #168's test zombie (`NoAI:1b`, a fresh one per row; **a line with
`crit=true` is not comparable, so re-fire**).

**A Wither TICK has no trace line.** `PLAYERHIT` is emitted by `TracedHit`, from `CastExecutor` only (*read*), and the
tick reaches `applyDamage` from the sink. Tick rows are witnessed by **the damage popup** (the `✖` glyph, dark gray) and
the nameplate, read by Ben. **Whether the slice adds a tick trace line is the seat's call (S3).**

> **RULED (S3): it does.** A `DOTTICK` line (fields in RULINGS) comes from the shared store, for scorch and wither. So
> every row below whose witness reads "popup" becomes a **DOTTICK row**: its prediction is the line, with the popup as a
> secondary witness. **The rows are re-drafted against DOTTICK's exact format once that commit exists.** Writing a log
> line's text before the line exists is a prediction about a string nobody has written.

### GATE-wither-status.md (WITHER-STATUS)

**THE SEAT MUST READ S1 FIRST:** at this slice's tip, **no shipped content applies Wither.** The element accrues it,
but no weapon wears the element until the bow. Every row here therefore applies it through
`/rpg apply withering <duration>`, which caps at `Scorch.UNDECLARED_CAP` (2.0), not at a weapon's figure.

> **RULED (S1): accepted as the split.** The two slices boot together at one stack top, so this gate is read in the
> same boot as the bow's. The clock and percent rows use `/rpg apply`; WB3 and WB4 are the weapon-capped path. There
> is no TEMP fixture.

| row | prediction (SURVIVAL) | witness |
|---|---|---|
| R0a–c | build line names the head; the jar has `statuses/withering.yml`; `Loaded ... 5 statuses ...`, every other count unchanged | log |
| **WS-C** · negative control · RUN FIRST | a never-scorched knell beside a second knell, killed by `/rpg mobdamage` with nothing applied: **no blast** (`GATE-ignite.md` I7's shape) | Ben |
| WS1 | `/rpg apply withering 200` on a 20-max zombie: a `✖` number of **1** every **40 t** (PROPOSED), **5** of them, first one 2 s in; **no fire on the mob** | popup, Ben |
| ~~WS2~~ | ~~the same on a knell (if not ruled immune): each tick **2** (`min(18, UNDECLARED_CAP 2.0)`), proving the cap binds~~ **WITHDRAWN 2026-09-30: the Knell is immune (Q-W6).** Replaced by WS2b | — |
| ~~WS2b (first draft)~~ | ~~a non-immune mob whose nameplate max is > 40~~ **RE-STAGED 2026-10-01 by the seat, below** | — |
| WS2b | the cap binding, by mob scaling: **a GS-100 zombie** (expected `MOBSEED … max=100`; 5% = 5 > 2.0): each DOTTICK **`min(0.05 × max, 2.0)` = 2.0**. **The control in the same block: `/rpg spawn zombie 20`** (expected `max=20`; 5% = 1.0): each DOTTICK **1.0**, the percent arm. **Each prediction is computed from that mob's own `MOBSEED max=` line** | DOTTICK (WITNESSED) |
| WS2c | **immunity**: `/rpg apply withering 200` on a knell, a plain wither skeleton and (if staged) a Wither: **no tick, no DOTTICK line, no potion swirl** (Q-W6) | DOTTICK absence + Ben. *Null observation: record the command and the target, or "nothing happened" proves nothing* |
| **WS3 · THE WS2 ROW** | a **never-scorched** mob with low HP (`/rpg mobdamage` it to ≤ 1 tick), withered, **dies to a Wither tick: NO explosion, no `ignite_blast`, the neighbour takes nothing** | Ben |
| WS3-P · the positive control, same boot | **RE-DRAFTED for Q-W8:** the same staging, but the mob is **`/rpg apply scorch` AND `/rpg apply withering`**, and **a Wither tick kills it: it EXPLODES** (Ben's "8, yes"; Ignite unchanged). This also proves Ignite still works, so WS3's silence means something | Ben + DOTTICK (the killing tick names `withering`) |
| WS4 | the death message for a WS3 kill names **the applier** (credit) | chat |
| WS5 | re-apply every 10 t for 100 t: ticks still arrive every 40 t (refresh does not restart the clock) | popup |
| WS6 | **the look, (a) as ruled:** the vanilla wither swirl on the zombie for the window; **exactly one damage per tick** (one DOTTICK, no second number from vanilla's WITHER: the gate held) | Ben + DOTTICK count |
| ~~WS7~~ | ~~immunity per Q-W6~~ **folded into WS2c** | — |
| WS8 | **players (Q-W5) are NOT stageable:** nothing can hit a player with a weapon until PvP is live. `/rpg apply` targets a mob by its aim-ray (*inferred* from `RpgCommand`'s "the mob the player is aiming at"). **Recorded as unwitnessed, not as passed** | — |

### GATE-withered-shortbow.md (WITHERED-SHORTBOW)

| row | prediction (SURVIVAL) | witness |
|---|---|---|
| R0a–c | the jar has `weapons/withered_shortbow.yml`; `14 → 15 weapons`, nothing else moved | log |
| WB1 | tooltip, in order (**no "Inflicts Wither" line**; Ben, Q-W10 overruled): **`Wither`** (dark gray), `Quiver: --/8`, blank, `Ranged Damage: <n>`, `Attack Speed: <20/cooldown, %.1f>`, …, **`Uncommon Ranged Weapon`**; a second `/rpg give` lands in its own slot | Ben |
| **WB2 · PLAYERHIT** | zombie at `~8 ~ ~0`, one right-click: an arrow flies and **`PLAYERHIT … source=withered_shortbow/right_click … element=wither sent=<attack_damage>.000 crit=false triggerScore=100`** | PLAYERHIT line |
| WB3 | after WB2's hit: `✖` tick numbers begin **one period later**, every period, for the ruled duration (the accrual path's first live instance) | popup |
| ~~WB4~~ | ~~on a knell (if not immune): ticks of `min(18, Q-W2's fraction × attack_damage)`~~ **WITHDRAWN: the Knell is immune.** Replaced by WB4b | — |
| ~~WB4b (first draft)~~ | ~~a warden, max > 160, else UNWITNESSED~~ **RE-STAGED 2026-10-01 by the seat, below; the warden is dropped** | — |
| WB4b | **the weapon's cap**, by mob scaling: **`/rpg spawn zombie 200`** (expected `MOBSEED … max=200`; 5% = 10 > 8): each DOTTICK **`min(0.05 × max, 8.0)` = 8.0** (`0.5 × 16`), **not 2.0**. **The control: a GS-100 zombie** (expected `max=100`; 5% = 5 < 8): each DOTTICK **5.0**, the percent arm. **Each prediction is computed from that mob's own `MOBSEED max=` line** | DOTTICK (`cap=8`; WITNESSED) |
| ~~WB1b~~ | ~~the tooltip carries "Inflicts Wither"~~ **DELETED 2026-09-30: Ben overruled Q-W10.** WB1 is the tooltip row, with only the element naming Wither | — |
| **WB5 · the WS2 row on the real path** | a never-scorched zombie, hit once, **dies to a Wither tick: no explosion**. Then one with ≤ attack_damage HP **killed by the arrow itself: no explosion** (Ignite's second clause, K2) | Ben |
| WB6 | it pushes (a zombie WITHOUT `NoAI`, as in SB3) | Ben |
| WB7 | eight arrows, then the reload (as in SB4, with the ruled numbers) | Ben |
| last step | put Ben's XP back | — |

---

## 11. STACKING AND THE FILES EACH SLICE TOUCHES (*inferred*; Phase 2 measures)

**WITHER-STATUS** (code + content). The core test comes first:
- core: `DotRates` (new), `Wither` (new), `Scorch` (exposes its `DotRates`), tests;
- paper: `StatusDefinition` (+`Wither`), `StatusLoader` (`kind: wither`), the store (parameterised `ScorchStatus`),
  `AdapterContext` (a second instance), `ElementAccrual` (the dispatch; `accruesScorch` unchanged in meaning),
  `BukkitCombatant` (the accrual lambda, the `applyStatus` arm), `ContentValidator` (the arm and the message),
  `RpgListeners` (`forget` for the wither store at the same three sites; the WITHER gate only under look option (a)),
  tests;
- content: `statuses/withering.yml` (new), `elements/wither.yml` (+`applies_status`), `elements/fire.yml` (the
  "only element" prose).

**ADDED BY THE 2026-09-30 ANSWERS** (still *inferred*; Phase 2 measures):
- **Q-W7 (a):** the potion applied beside the wither store, and **the WITHER gate in `onEnvironmentalDamage`**, which
  is now in scope and no longer conditional;
- **Q-W6:** the immunity list in `withering.yml` (`[wither_skeleton, wither]`), its parse, and the check at apply time;
- ~~**Q-W10 (T1):**~~ **WITHDRAWN 2026-09-30 (Ben overruled Q-W10): none of this is built.** ~~an optional status `display_name` (`StatusLoader`, `StatusDefinition`, and every test constructing a
  status record), `WeaponLore.build` taking the `StatusRegistry`, `WeaponItems` passing it, and `withering.yml`
  `display_name: "Wither"`. **This belongs in WITHER-STATUS** (it is status schema), and the bow's
  `golden-lore.txt` block shows its first rendering.~~

**DOTTICK** (S3, its OWN commit inside WITHER-STATUS, with its own unit test): the shared store emits the line on
each tick when `/rpg mobtrace` is on, for both instances. *Which class owns the mobtrace switch at that site, and
how the store reaches it, is Phase 2's to read.* `MobNameplateManager` holds the switch today, and `RpgPlugin` passes
it to PLAYERHIT.

**WITHERED-SHORTBOW** (content only, stacked on it, **booted in the same boot**): `weapons/withered_shortbow.yml`, `golden-lore.txt`,
`KNOWN_RANGER_WEAPONS`.

---

## 12. BEN'S QUESTIONS, IN PLAIN LANGUAGE

> **ANSWERED 2026-09-30: "8, yes everything else is fine".** The seat filled Q-W6, Q-W7 and Q-W10. The ruled values
> are in RULINGS, and the questions are kept below as they were asked. **One question is new, from Q-W10's costing:**
>
> - ~~**Q-W11. Should fire weapons say "Inflicts Scorch" the way the bow says "Inflicts Wither"?** Yes changes five
>   shipped tooltips. No leaves the bow as the only weapon whose tooltip names its effect (section 5.3).~~
>   **MOOT 2026-09-30: Ben overruled Q-W10, so no tooltip names a status.**

- **Q-W1. How often does Wither hurt?** Every 2 seconds like vanilla, or every second like Scorch? *(Proposed: every 2 s.)*
- **Q-W2. How hard does each tick hit?** Proposed: 5% of the target's max health, but never more than **half** the
  arrow's damage (like Scorch), **or** never more than **all** of it (your words, "based off the damage of the
  weapon"). This only matters on big targets.
- **Q-W3. How long does it last?** Proposed: 10 seconds, like a wither skeleton's hit. Scorch lasts 6.
- **Q-W4. Hitting a withered target again:** does it reset the timer (like Scorch, proposed) or stack up?
- **Q-W5. Does Wither hurt players?** It only matters once PvP is on. *(Proposed: yes.)*
- **Q-W6. Who is immune?** Vanilla makes wither skeletons and the Wither immune. **The Knell is a wither skeleton:**
  should it shrug off Wither too?
- **Q-W7. What should it look like?** (a) The real vanilla wither effect for looks only: the dark swirl, and black
  hearts on a player. Its own damage would be switched off. Vanilla will **not** put it on wither skeletons or the
  Knell. (b) Our own particles only, which work on everything. (c) Both.
- **Q-W8. A mob that is burning (Scorch) AND withering, and the Wither finishes it off: should it still explode?**
  Today's rule is "any mob that dies while scorched explodes".
- **Q-W9. The bow's numbers:** damage, cooldown (proposed 16 ticks, slower than the Short Bow's 12), quiver, reload,
  arrow speed and knockback. The table in 9.1 has the starting points.
- **Q-W10. Where should the word "Wither" (the effect's name) show?** Today nothing names a status. The bow's tooltip
  would say "Wither" only as its element.

## 13. THE SEAT'S QUESTIONS (ALL THREE RULED 2026-09-30, see RULINGS: S1 one boot, no TEMP; S2 depth scorch-only; S3 DOTTICK)

- **S1. WITHER-STATUS has no content applier at its own tip.** Its gate can only use `/rpg apply`, which caps at 2.0.
  The weapon-capped path is first witnessed in the bow's gate (WB3/WB4). Options: accept that split; or gate both
  slices in one boot with the status rows read after the bow lands. **A TEMP fixture is not proposed** (the owed-removal
  precedent).
- **S2. `depth` on the shared store:** scorch-only (a wrapper or subclass) or shared with the wither instance passing
  0 (3.2).
- **S3. A tick trace line**, so the tick rows get a log witness and not only Ben's eyes (section 10).

## 14. UNVERIFIED, IN ONE PLACE

- Black hearts and the potion's particle look: client-side.
- Whether a token WITHER tick flashes the hurt animation (inferred from FIRE_TICK's token only).
- A per-world Paper override of `immune-to-wither-effect` (only the defaults file was read).
- That no code path crosses the element and status registries (inferred, section 5.2).
- Every file list in section 11, and every arith figure in sections 8 and 9: these are arithmetic, not readings.
