# PLAN — the quiver, slice A1: the per-item count

Mechanism slice. One new content file (a fixture), no shipped weapon changed.

> **This document ships in the repo as `PLAN-quiver.md`, committed on the branch.** Not optional
> housekeeping: `PLAN-beam-gap.md` records **three** instances of a plan reviewed in a chat, acted
> on, and never committed (`PLAN-cursed-emerald.md` among them), each found by the `--numstat`
> reconcile rule rather than by anyone noticing. Stage it with the first commit, and confirm with
> `git diff --cached --numstat --diff-filter=A` so an addition cannot hide inside a list of
> modifications.

---

## Context

The Ranger's identity weapon is the Boltor, and its brake is a quiver: a magazine that empties and
must be reloaded, rather than a cooldown or a mana cost. Nothing in this repo has a quiver — `grep`
for `quiver|ammo` across `--include=*.java --include=*.md --include=*.yml` returns **zero hits**, so
this is greenfield.

The full feature was briefed as four slices (A quiver / B single-wield Boltor / C dual-wield). **A
has been split**, by the ruling below. **A1 is this plan and this plan only**: the per-item ammo
count, carried correctly through every path that mints, re-mints, crafts or refreshes an item; the
left-click reload; and the refusals for an empty or reloading weapon. **No stats, no Boltor, no
dual-wield, no alternation.**

---

## CORRECTIONS OWED TO MY OWN SCOPING FIGURES

Stated because a figure whose basis is unstated cannot be checked, and these were quoted to the
operator as measured when one of them was not measured at all.

I claimed the three most recent stat-adding commits were **"17–21 files, ~900 LOC"**. Re-measured,
`git show --numstat --format="" <sha> | awk`:

| commit | | |
|---|---|---|
| `4a453ac` Stats Slice 2: Mana Regen | 19 files | +1461 / −68 |
| `8362262` Stats Slice 1: Health Regen | 21 files | +1501 / −24 |
| `48b8db9` Armor 2b: Mana Bank + Max Mana | **26 files** | +1439 / −26 |

**Two errors, and they are different kinds.** The file range `17–21` **excluded the largest commit
of the three** — an ordinary range error. The `~900 LOC` was **never a measurement**: it was an
exploring agent's estimate of *production + test code lines only*, quoted by me as though it
described the commits. The commits add **1439–1501 lines**, because each also carries `NEXT.md`
(+76 to +103) and a new `PLAN-*.md` (154–243 lines) that the estimate never counted.

So the real budget for A-as-briefed is **~40–55 files**, not ~35–45. The conclusion — that it is
past *"keep changes small enough that I can read them"* — holds, and holds harder.

---

## Rulings carried into this plan

**Ruled by the operator this session**, so they are not re-derived:

1. **A splits into A1 (the count) and A2 (the stats).** Reload time stays a modifiable stat in A2 —
   *not* left a constant, because enchants and gear are already in the design and a constant that
   later becomes a stat is a second migration.
2. **The carrier is `quiver_stone.yml`**, on `volley_stone.yml`'s precedent: permanent (not
   `_TEMP`), in no kit, no recipe, reachable only through `/rpg give` behind `rpg.command.give`.
   The `_TEMP` fixtures are refused on stronger grounds than their removal debt — `health_boost_TEMP`
   and `attack_speed_boost_TEMP` are **stat-modifier items, not weapons, and a quiver needs a
   carrier that can fire.**
3. **Rounding is `floor`**, with two conditions (below). It is the only mode where a modifier never
   delivers *more* than it claims; an under-delivery can be named on a tooltip, a silent inflation
   cannot.
4. **Boltor `attack_damage` is UNRULED.** `GATE-boltor.md` (slice B) is the authority, on the
   `BEAM_ORIGIN_GAP` precedent. Not derived from `hunters_bow` (a dev weapon by the operator's
   ruling), not from `GearScore.scalePower` (no levels here to scale).

**From the brief, recorded here because they become unfindable otherwise:**

- **THE HITSCAN REVERSAL IS KNOWING.** The old repo had already moved *off* hitscan — its javadoc
  reads *"fires a fast real Arrow (not a hitscan particle line)"* and, twice, *"as in the old
  hitscan"* — a deliberate change to keep the bolt *"a real, dodgeable arrow"* at 5.5f, near-hitscan
  velocity. **The operator is reversing that decision knowingly**: the new Boltor is `CastSpec.Ray`,
  like the Lapis Staff and the Cursed Emerald. Not carried: the arrow, the velocity, ammo-type
  mirroring, tipped-arrow handling, pickup status. This belongs in slice B's plan too; it is here so
  the reversal has a written home before B exists.
- **THE I-FRAME APPARATUS IS NOT PORTED, AND IT WAS CHECKED RATHER THAN ASSUMED.** The old
  `onBoltorArrowHit` spends most of its body defeating vanilla i-frames
  (`setNoDamageTicks(0)`/`setLastDamage(0)`, before *and* after, with a comment about the second
  arrow's event being *"silently dropped at the source"*). **It does not apply here**:
  `BukkitCombatant.applyDamage` drains custom HP through `ctx.stats().damage(...)` and `RpgListeners`
  calls the vanilla hit only as a cosmetic — *"the real number is custom HP"*.
  `cursed_emerald.yml:174-191` records the same finding independently, and CE2 observed six bolts at
  `interval_ticks: 2` as *"six numbers, countable."* **Written down because the opposite alarm was
  nearly briefed from the old repo's architecture**, and without this note the next reader re-derives
  it.

> **I could not verify the old-repo measurements.** `cfde822` is not an object in this repository
> (`git cat-file -t cfde822` → *fatal: Not a valid object name*) and `origin` is
> `butterflysmp/minecraft-rpg-scaffold`. Every old-repo figure above — 700 ms, 5.5f, the dual-wield
> both-hands rule, the falloff — is **the operator's measurement, taken on trust**, and is labelled
> as such rather than presented as re-checked.

---

## Scope of A1

| in | out |
|---|---|
| `core/.../weapon/Quiver.java` — the arithmetic, capacity and reload ticks as **parameters** | any `Stat`, `HealthState` field, `CombatantStats` method, `Keys` boost pair, `*ModifierItems` scanner — **all A2** |
| THREE PDC keys: the count, and the reload's start and completion ticks (the start is the restart guard's bound -- see the correction below; an earlier draft said two) | the Boltor, `CastSpec.Ray` wiring, dual-wield, alternation |
| `quiver_size:` / `reload_ticks:` on `WeaponDefinition` + `WeaponLoader` + **an unknown-key guard in `WeaponLoader`** (NOT `ContentValidator` -- see below) | a quiver enchant (`EnchantEffect` constant) |
| the stamp in `WeaponItems.mint`, the carry in `GearItems.carryInstanceData` | `/rpg stats` sheet line |
| the left-click reload | the held-right-click **fix** (A1 only *measures* the floor) |
| two new `CastResult` arms and their notices | |
| `quiver_stone.yml` | |
| ~~the owed mint test~~ -- **NOT PAYABLE, see the corrections section** | |

---

## THE CENTREPIECE — how the count is carried

The brief's fear is *"EVERY path which mints, copies, re-stamps, grants or crafts an item must carry
the count."* **Measured: the repo has already centralised that into two funnels**, and the entire
carry is two edits.

### The two edits

**1 · The stamp — `WeaponItems.mint`, `paper/.../weapon/WeaponItems.java:88-144`.**
Stamp the count to **full, explicitly**, beside `meta.setMaxStackSize(1)` at `:114`.
**It must sit BEFORE the `applyLore(meta, weapon, adapters)` call that closes the `editMeta`
block** — the tooltip reads the stamped count, and lore rendered before the stamp renders the wrong
number.

> **THE ENCHANT PRECEDENT IS THE MIRROR OF THIS CASE, NOT THE SAME TRAP, AND THE FIRST DRAFT OF
> THIS PARAGRAPH CITED IT AS THOUGH IT WERE.** `remint:264-277` says the enchant block was safe at
> mint *"against an empty container, **which was a no-op**"* — i.e. **absence rendered NOTHING**.
> And `WeaponItems.java:180-181` records that *"applyLore already runs **twice** per remint (once
> against an empty container, once against the carried state)"*, so the enchant case is
> **self-correcting**: the second pass sees the carried state.
>
> **The quiver inverts both halves.** Absence renders **a number**, not nothing — which is this
> plan's own *absence is not a neutral value* section — and **a fresh mint has no second `applyLore`
> pass** to correct it.
>
> So: the ordering requirement is **right**; the hazard is **FRESH-MINT-ONLY** (re-mint is
> self-correcting here for the same reason the enchant case is); and the precedent is the **mirror**.
> Say it that way at the site. A reader who checks `remint:264-277` against a comment claiming *"the
> same trap one field over"* finds it does not say that, and then has to work out which of the two is
> wrong — which is how a load-bearing ordering constraint becomes a magic line someone reorders.

**And `carryInstanceData`'s one deliberate omission supports classifying the count as instance
data rather than display.** `WeaponItems.java:178` says the carry moves *"weapon_id, wear and the
enchant blob, and **pointedly not this**"* — *this* being the **glint**, which is display and is
recomputed from the carried state. The phrase reads at a glance like a conflict with the carry
roster; cite what it actually omits, so it does not.

**2 · The carry — `GearItems.carryInstanceData`, `paper/.../weapon/GearItems.java:190-195`.**
Today it is exactly three lines, under a javadoc that is the whole contract:

```java
// "The three things a re-mint carries forward … Everything NOT copied here is
//  DISPLAY and is rebuilt from current content."
carryTag(from, to, idKey);
carryWear(from, to, material);
carryEnchants(from, to, keys);
```

The count becomes **the fourth thing carried**. That one line fixes every re-mint path at once.

### What inherits for free, and it is everything

Because all four kinds route through those two funnels, **no other file needs to know the count
exists.** Verified by `grep` for `carryInstanceData|GearItems.mint|GearItems.remint`:

| path | file:line | reaches the funnel by |
|---|---|---|
| `/rpg give` | `RpgCommand.java:939` | `GearItems.mint` |
| kit grant (`/rpg class`, `/rpg element`) | `RpgCommand.java:1679` | `WeaponItems.mint` directly — **same body** |
| mint-on-craft (the only craft that rolls) | `InventoryCraft.java:317` | `GearItems.mint` |
| **join** | `RpgListeners.java:319` → `GearRefresher.java:107` | `GearItems.remint` |
| `/rpg refresh` | `RpgCommand.java:967` → `GearRefresher.java:107` | `GearItems.remint` |
| `/rpg enchant` (5 of 6 ops) | `RpgCommand.java:1356` | `GearItems.remint` |
| every enchant-table click | `EnchantMenu.java:143` | `GearItems.remint` |
| the other three `remint` bodies | `ShieldItems:184`, `ArmorItems:114`, `ToolItems:157` | all call `carryInstanceData` |

**Miss edit 2 and the count silently resets on every relog** — which is precisely the failure
`carryEnchants` was written to prevent, arriving through the same door. `carryWear`'s own javadoc
names the shape: *"Losing this would be a relog-to-repair exploit, since a re-mint happens on join
and on every `/rpg enchant` write."*

### The four paths that do NOT inherit, and they need a decision

Four sites mint a **real `ItemStack` that is displayed but never granted** — they call a mint body
and skip the roll:

- `InventoryCraft.java:137` — the `MenuSafety.fits` probe (discarded)
- `CraftingMenu.java:506` — the result-slot preview
- `CraftingMenu.java:696` — the suggestion icon
- `RecipeBrowserMenu.java:357` — the recipe-browser icon

**A preview minted full will advertise a full quiver.** For A1 that is *correct and wanted* — a
crafted quiver weapon genuinely arrives full, so the preview tells the truth. **Recorded rather than
left**, because the day anything mints non-full these four become lies, and nothing would redden.

### ABSENCE IS NOT A NEUTRAL VALUE — and the argument is already written in this repo

An unstamped quiver and an empty one must not read the same. **`volley_stone.yml:42-45` solved this
exact problem once already**, in the very file A1 takes as its carrier precedent:

> *"AUTHORED 0, NOT OMITTED. `WeaponLoader:88` is `getDouble("attack_damage", 0.0)`, so the absence
> would resolve to exactly the value the omission meant — which is the trap: it works, it is
> invisible, and it hides a decision."*

So: **mint stamps FULL explicitly; a missing key on an item that declares `quiver_size:` is a defect
that WARNS, never a zero that is accepted.** Cite that comment at the read site rather than
re-deriving the argument.

Two further absence hazards, both measured:

- **Unknown YAML keys were silently ignored.** A typo'd `quivver_size:` loaded clean and produced a
  crossbow with no magazine. **LANDED as an unknown-key warning in `WeaponLoader.parse`**, tested by
  *causing* the condition — a real file with a real typo, through the real `loadAll`, warning read
  back by its text.

  > **IT IS NOT IN `ContentValidator`, AND THE FIRST DRAFT OF THIS PLAN SAID IT WOULD BE — TWICE,
  > plus in two gate rows.** That was the wrong home, and the reason is worth keeping: **every guard
  > in `WeaponDefinition` validates a value that WAS READ, and a misspelled key is never read at
  > all.** `s.getInt("quivver_size", NO_QUIVER)` returns the default and nothing downstream can
  > observe that anything was wrong. So this cannot be a value check at any layer — **it has to be a
  > KEY check, and the only place that can see the authored keys is the loader**, which holds the
  > `ConfigurationSection`. `ContentValidator` runs after parsing, on resolved objects, where the
  > typo has already vanished.
  >
  > **Scope, so the omission is not mistaken for an oversight: weapons only.** Armor, shields, tools,
  > enchants, abilities and mobs have the identical hazard and no such check. A general strict-key
  > facility across every loader is a separate pass; weapons go first because the quiver is the first
  > field here where a typo yields a weapon that is *silently wrong* rather than visibly broken.
  >
  > **The known-key set is hand-maintained**, which is `DamageSignatureTest`'s "today's set" problem
  > again. Four rows guard it: the typo warns, the full legitimate schema is silent, **every shipped
  > weapon file is silent** (that one because the first two use fixtures this test wrote and cannot
  > say whether the check is about to cry wolf over nine real weapons), and **`KNOWN_KEYS` equals the
  > set `parse` actually reads**.
  >
  > > **THE FOURTH ROW EXISTS BECAUSE THE DRIFT CLAIM WAS HALF TRUE, AND THE FALSE HALF READ AS
  > > REASSURANCE.** The javadoc said the burden *"fails towards noise and not towards silence"*.
  > > That holds for a key ADDED to `parse` and forgotten in the set — spurious warning, loud,
  > > self-correcting. It is **false for a key REMOVED from `parse` and left in the set**: authoring
  > > it then warns nothing and does nothing, which is *the exact defect this guard exists to
  > > prevent, reintroduced by the guard's own staleness*. **Measured, not argued** — a bogus
  > > `"sweap"` entry passed all 33 rows in silence, because the typo row uses a deliberate
  > > misspelling and the other two assert silence, which a stale entry produces.
  > >
  > > **And the fourth row's first version failed for a reason worth keeping.** It matched
  > > `s.getX("…")` against the raw source and reported `quivver_size` as a key the loader reads —
  > > picked up from `KNOWN_KEYS`' own javadoc, which uses that exact typo as its worked example.
  > > **A source scan that does not strip prose is reading documentation as if it were code**, and
  > > this repo makes that the norm rather than an edge case: `CLAUDE.md` records javadocs quoting
  > > their own call sites as the reason a mutation target usually appears twice. The strip has its
  > > own control, because a stripper that silently did nothing returns the whole file and every
  > > assertion below then runs against the state that just failed.
  >
  > **SCOPE HAS TWO AXES AND THE PLAN PREVIOUSLY NAMED ONLY ONE.** Weapons only — *and* originally
  > **top-level keys only**, because `s.getKeys(false)` is not recursive. **The second axis is now
  > closed: trigger blocks are checked too**, against a separate `TRIGGER_KEYS` set of seven —
  > **`name`, `description`, `cooldown_ticks`, `cost`, `cast`, `on_hit`, `on_cast`** (the set named,
  > not just counted).
  >
  > **TAKEN BEFORE `quiver_stone.yml` IS AUTHORED, AND THE ORDER IS THE WHOLE ARGUMENT.** That file
  > is a brand-new hand-written trigger block, and it is the **instrument** that measures the
  > held-right-click repeat floor. A fixture with a silently-ignored key in its trigger is *still a
  > valid weapon* — it just measures a different configuration than it claims to, yields a plausible
  > number, and looks entirely correct. **A control succeeding for the wrong reason, on the single
  > artifact whose number slice C's dual-wield cooldown is chosen against.** Guarding afterwards
  > would check the file only if someone re-ran the boot against it.
  >
  > The two sets are **deliberately not merged**: one combined set would make `cast:` legal at the
  > top level and `material:` legal inside a trigger, both silently ignored — the exact failure both
  > checks prevent. `MUTMERGE` reddens five rows.
  >
  > **The remaining boundary, named because an unstated depth reads as an oversight rather than a
  > line:** everything *below* a trigger is still unchecked — the fields inside `cast:`, and the
  > entries of `on_hit:` / `on_cast:` / `cost:`. Those parse through `AbilitySchema` against a
  > grammar **shared with abilities**, so extending the check there covers both content kinds at
  > once, which is the separate loader pass rather than this one.

### THE STANDING NO-STACK DECISION GAINS ITS SECOND AND STRONGER REASON

`CLAUDE.md`'s *"NO CUSTOM ITEM STACKS ABOVE 1"* is currently justified by durability, enchants and
instance data being per-item. **A quiver makes it load-bearing for a value that changes every
shot**, and `WeaponItems.java:105-114` already argues this. Add the reason where the decision is
recorded, and note that `MenuRouting.java:216` and `CraftingMenu.java:323/356` depend on the cap.

> **AND THE CAP STILL HAS NO TEST — this slice owes it.** `NEXT.md:1797-1804` records:
> *"Four `setMaxStackSize(1)` calls … and zero tests across 1294. That is why a comment could
> contradict the rule for nine days with a green suite: nothing could have reddened. **OWED: a mint
> test per gear kind** … it belongs in the next slice that touches `weapon/`."* **A1 is that
> slice.** Pay it: four rows, one per mint body.

---

## `core/.../weapon/Quiver.java` — modelled on `Durability`, and A2's confinement

**`Durability` is the exact precedent and it is not an analogy.** It is per-item mutable state,
mutated in play on every use, written back through one funnel, with its decisions in core as pure
`int` arithmetic and its Bukkit I/O in a thin paper class. `Quiver` is the same shape with a
different verb:

| `Durability` / `WeaponDurability` | `Quiver` / `QuiverItems` |
|---|---|
| `maxDurability` (the material's) | `capacity` (**a parameter**) |
| `damage` counts **up** to a floor | `loaded` counts **down** to zero |
| `wear(current, amount, max)` | `spend(loaded)` |
| `repair(current, amount)` | `reload()` → `capacity` |
| `isBroken(current, max)` | `isEmpty(loaded)` |
| `clamp(proposed, max)` — the re-mint guard | `clamp(proposed, capacity)` — same job |

`clamp` matters for the same reason it does there: `carryWear`'s javadoc explains that copying a raw
value onto a **lower** maximum would land past it. A quiver carried onto a weapon whose capacity
dropped needs the identical clamp, and A2 makes capacity movable, so this is not hypothetical.

> **The capacity-change semantics are already a written standing decision and are reused verbatim**
> — `DESIGN-stat-engine.md:68-79`: **max increases → current unchanged (headroom, never a free
> heal); max decreases → current clamped.** A quiver that gains capacity does not gain arrows; one
> that loses capacity loses the overflow. Same rule, same words, no new decision.

### A2's confinement, made OBSERVABLE rather than intended

The split was bought with the claim *"only the two call sites that supply capacity and reload ticks
change."* **That is a prediction about a future diff, and a prediction with no guard is how a split
gets paid for and not delivered.** It holds only if `Quiver` never reads the shipped numbers itself
— not once, not as a default, not in a convenience constructor. A `Quiver` that secretly reads a
constant still compiles, still passes, and still looks like the plan.

**Two guards, one behavioural and one mechanical:**

1. **Every core test constructs at values that are NOT the shipped ones** — capacity **3**, reload
   **7 ticks** — and asserts the behaviour there. A `Quiver` reading a constant fails immediately.
2. **`QuiverSignatureTest`**, on the established idiom. `DamageSignatureTest`
   (`core/src/test/.../combat/DamageSignatureTest.java`) exists for exactly this argument, in its own
   words: *"a safety that holds on a condition nobody wrote down where it would be violated"*, so it
   is written *"where it WILL be seen — in a red build."* The quiver's version asserts that every
   public method on `Quiver` takes **primitives only** — no `WeaponDefinition`, no `CombatantStats`,
   no `ItemStack` — and that the class declares **no `static final int` capacity or reload field.**
   `Durability` already satisfies both properties (`wear(int, int, int)`), so this pins an existing
   house shape rather than inventing one.

With both in place, A2 is **provably** a call-site change: the test already proved the class does not
know the shipped numbers.

### The rounding rule, and the two conditions on it

`floor`, at one named site in `Quiver`. **The mode is the easy half; the defect lives in the
EXPRESSION.** Brute-forced base 1..64 × percent 1..200, keeping only cases whose exact product is a
whole number, and counting where `floor` loses one:

```
base*(1+p/100)      13      <- the natural-reading form, and the broken one
base + base*(p/100)  0
base*(100+p)/100     0
```

with, executed and reproduced: `25 +16% → 28.999999999999996` (floor 28, exact **29**),
`50 +16% → 57.99999999999999` (exact **58**), `45 +40% → 62.99999999999999` (exact **63**).

**Write the expression at the site and say why that form and not the other**, or the next reader
simplifies it to `base*(1+p/100)` because it reads better and nothing fails.

> **AND THE SHIPPED NUMBER HIDES IT.** Base 8 is a **power of two** and never hits in the searched
> range — executed: `8 +16% = 9.28`, `8 +25% = 10.0`, both exact. **A test at 8 proves nothing about
> 25**, and 25 is ordinary the moment flat gear modifiers stack before a percentage. Put `25 +16% →
> 29` in the core test as a named case with its expected value.

**CONDITION 2 — THE DEAD ZONE IS NOT A ROUNDING PROBLEM AND NO MODE FIXES IT.** At base 8, one arrow
is **12.5%**. Under `floor`, *"+5% Quiver Size"* and *"+10% Quiver Size"* both deliver **zero**. An
enchant whose tooltip advertises a buff and gives nothing is a falsified claim to a player, and this
repo already has the rule: *a falsified comment misleads a reader who can check it; a falsified
flavour line misleads a player who cannot.* **So the tooltip must render what is STAMPED, not what
was authored** — or the dead zone must be closed. Escalated below rather than settled here.

**ESCALATED TO THE OPERATOR — the modifier SHAPE, which is his and has not been ruled.** The
rounding question assumed a **percentage**; he ruled only *"modifiable by enchants and gear."* This
repo already carries **both** conventions on the same `Stat` class — `attackSpeed` is a multiplier
neutral at 1.0, `classDamage` is *"a SUMMAND in points, so 0"*. **An integer capacity fits the
summand convention exactly**: under *"+2 Quiver"* there is no rounding mode, no dead zone and no
expression hazard, and the number a player reads is the number they get. `floor` ships regardless —
a percentage will eventually arrive from a set bonus or a global buff even if quiver enchants are
flat, and *"we decided not to have percentages"* is a premise that outlives itself silently — but it
is written as **the rule for a percentage reaching an integer stat**, not as the quiver's modifier
design.

---

## The reload — left-click, lazy, and leak-proof by construction

**Input.** Left-click, per the ruling. Three measured constraints shape it:

1. **`WeaponSwingListener` fires roughly once per tick while left-click is HELD** (~20/s) and
   **discards every outcome but `Broken`** (`.filter(CastResult.Broken.class::isInstance)`). A
   reload bound here is re-entered 20 times a second and must be state-gated, and any refusal it
   wants heard must be spoken explicitly.
2. **`type: melee` is a trapdoor.** `WeaponFire.attempt:106` returns **empty** for a vanilla-driven
   melee trigger, so a `left_click` trigger declared `type: melee` never reaches the swing listener
   at all — it is delivered by the `EntityDamageByEntityEvent` rider instead.
3. **A weapon that declares a quiver claims left-click.** `hunters_bow.yml` reserves left-click for
   *"a future melee/special"*; a quiver weapon spends it. **A weapon declaring both `quiver_size:`
   and a `left_click` trigger is a content error the validator refuses at boot** — stated as a rule
   rather than left to whichever wins.

### THE FENCEPOST IS RULED HERE, BECAUSE A1 DECIDES IT WHETHER OR NOT ANYONE RULES IT

**A1 ships the reload. Whatever the left-click handler does while the fire cooldown is still
running IS the answer** — decided by implementation rather than by ruling — and slice B would then
quote rates off a behaviour nobody chose. That is a premise created silently by an earlier slice.
An earlier draft of this plan handed the question to slice B, which **is** deciding it by omission.

> **RULED: the reload is NOT gated by the fire cooldown. Left-click reloads immediately, mid-burst,
> at any point in the magazine.**

**The reason is that gating is the one that costs extra code.** Cooldowns are keyed per-player per
`weaponId + "/" + input` (`WeaponLoader.java:136`), so `quiver_stone/left_click` and
`quiver_stone/right_click` are **already independent buckets** — measured, not assumed. Gating the
reload on the fire timer means explicitly coupling two timers the system keeps apart; ungated is what
the existing mechanism does if nobody intervenes. Choosing the branch that requires new coupling, for
no stated gameplay reason, would be the worse default. And a tactical reload mid-magazine is the
point of a magazine: a player who fires and immediately reloads should not eat a dead 0.7 s.

**Consequences, stated so slice B inherits a number rather than a range:** the cycle is the **free**
branch — single **7.90 s → 1.0127 sh/s**, dual **11.25 s → 1.4222 sh/s**, **ratio 1.40×**. Slice B
quotes those, not the gated figures, and **the ruling is what makes them quotable at all.**

**Named core test row (row 8 below), so it cannot drift**: spend one shot, then reload on the very
next tick, and assert the reload **begins** rather than waiting out `cooldown_ticks`. A gate row
(Q11) observes the same thing in play, but **the test is the authority** — the boot row is a witness
that the rule survived the wiring.

**Where the timer lives, and why it is not scheduled.** The reload completion tick is stamped **on
the item**, as a second PDC key, and evaluated **on read**:

| state | behaviour |
|---|---|
| key absent | not reloading |
| `now < completesAt` | `Reloading` refusal, `completesAt - now` ticks remaining |
| `now >= completesAt` | stamp count = capacity, clear the key, and the shot proceeds |

**This is lazy integration, and the precedent is mana.** `PlayerHealthSystem.java:256-260` names the
axis: mana is *"LAZY-INTEGRATED (amount + elapsed × rate, evaluated on read)"* while health regen is
eager. A scheduled task would need an expiry event, and **an expiry event is a thing that can be
missed** — the player swaps the weapon away, drops it, dies, logs out — which is the identical
argument `ModifierReconciler`'s javadoc makes for diffing over listening: *"a single missed event
LEAKS … Reconcile does not care HOW an item left."* Lazy evaluation has no event to miss.

**It also settles three cases for free**, each of which a scheduled task gets wrong:

- **Two identical quiver weapons.** Cooldowns are keyed per-player per-`weaponId/input`
  (`WeaponLoader.java:136`), **never per item** — so a per-player reload timer would let a swap
  mid-reload fill the *wrong* weapon. On the item, it cannot.
- **Swap away and back.** The reload resumes exactly where it was, because it never stopped being a
  property of the item. This is the brief's own ruling — *quiver contents travel with the item* —
  applied to the reload as well as the count.
- **Logout.** `CooldownTracker` is cleared on quit (`RpgListeners.java:780`) and holds no
  persistence; the item keeps its own state.

> **ONE EDGE THE LAZY FORM INTRODUCES, AND IT MUST BE GUARDED.** `Bukkit.getCurrentTick()` **resets
> on server restart**, so a reload spanning a restart leaves a `completesAt` in a future that never
> arrives — a permanently dead weapon, with a tooltip that reads correctly. **Guard: if
> `now < startedAt`, the clock has moved backwards; treat the reload as complete.**
>
> > **CORRECTED AFTER COMMIT 1 REVIEW — THE FIRST FORM OF THIS GUARD WAS A FREE INSTANT RELOAD.**
> > It read `completesAt - now > reloadTicks`, which is sound only while `reloadTicks` cannot move
> > between the stamp and the read — and `reloadTicks` **is the quantity A2 exists to make movable.**
> > Executed against that version: stamped at 100 with a 60-tick reload, then read at 105 with the
> > stat dropped to 20, it returned **complete** and reported **0 ticks remaining**. Equipping
> > reload-speed gear mid-reload was free ammunition, and it looked like the item working well.
> >
> > **THE PROPERTY, which the next guard written here inherits: a guard's bound must be a quantity
> > that CANNOT LEGITIMATELY CHANGE between the stamp and the read.** So the item stamps
> > `startedAt` alongside `completesAt` — same instant, same clock — and the live duration does not
> > enter the method at all. **The defect is unrepresentable rather than guarded against**, the same
> > move the fencepost makes by giving `reloadCompletesAt` no cooldown parameter.
> >
> > **No test could have caught it**, because every clock row passed the same `reloadTicks` for the
> > stamp and the read: the fixture held fixed the one condition under which the guard was correct,
> > so the suite was green for a reason unrelated to the guard being right. The row that now varies
> > it is `shorteningTheReloadDurationMidFlightDoesNotFinishItEarly`, and `MUTBOUND` reintroduces the
> > old form and reddens it.
> >
> > **And the committed deadline is STAMPED, never re-derived from the live stat**, so an in-flight
> > reload keeps the length it was committed at. Existing ruling, not a new one —
> > `AbilityService.resolve`: *"the swing you have already committed to keeps the cadence it was
> > committed at."*
> >
> > **Residual, named rather than left to be found:** a restart landing *inside* `[startedAt,
> > completesAt)` is indistinguishable from an ordinary reload and is not caught. The cost is bounded
> > by one reload duration; the unbounded case — a weapon stranded forever — is what the guard takes.
> This is the same family as `Durability.wear`'s `long` widening, whose javadoc says the quiet part:
> *"a huge wear would silently become a FULL REPAIR … a debuff looping around into the strongest
> possible buff."* Both are a counter trusted past the range it is valid over.
>
> **THE TICK SUPPLIER IS PRECEDENT. THE PERSISTENCE IS NOT — AND BOTH HALVES GO AT THE SITE.**
> `Bukkit::getCurrentTick` is already injected as a `LongSupplier` into `MeleeHits`
> (`RpgListeners.java:159`) and `DamageWindow` (`:165`). **That is good news this plan did not
> claim: the injection seam makes the backwards-clock row unit-testable with no server**, by handing
> `Quiver` a supplier that jumps backwards — so it is a core row, not a boot row.
>
> **But both existing users compare ticks WITHIN a session and hold only in-memory state that a
> restart clears** (`MeleeHits:230` is `hit.tick() != currentTick.getAsLong()` — a same-tick test).
> **The quiver is the first thing in this repo to write a tick value to DISK.** So the guard is the
> necessary consequence of a genuinely new use, not defensive habit, and **the two existing call
> sites are NOT evidence that the restart question was ever considered.** Without that sentence the
> next reader sees `getCurrentTick` in three places and assumes it was settled somewhere.

**Where the decrement fires.** Not in the `on_hit` grammar. Measured: **there is no effect type that
consumes an item**, and an `EffectSpec` arm would be **the wrong thread** — a projectile impact
resolves on the *target's* region, a ray's later segments on whatever regions they cross. The seam
already exists and is already occupied by exactly this kind of work:

> `CastExecutor`'s `onBasicAttackUse` listener — *"THREADING — do not move either call site. This
> may only ever be run SYNCHRONOUSLY within `execute` … which is entered on the thread owning the
> aim's origin, which for a weapon is the caster's own eye … Running this from one of those would
> write the caster's inventory from a foreign thread — the exact bug the snapshot/handle split
> exists to prevent."*

That is where `WeaponDurability.applyWearOnUse` already rides. **The quiver decrement rides the same
seam, on the caster's own thread**, and `applyWearOnUse`'s own javadoc argues for it: *"there is one
of it so the enchant below had one seam to plug into instead of two sites to reopen."*

**Consequence to state rather than discover: a Boltor wears AND empties.** `material: crossbow` has
vanilla durability, so two independent per-item counters decrement per shot, and both route to
`BrokenNotice`. Name which message wins when both fire.

**No `CombatantSnapshot` component is needed.** Unlike attack speed — read inside core's `resolve`,
which has no access to the store — the quiver gate lives in **paper's `WeaponFire.attempt`**, which
holds `adapters.stats()` and already runs on the caster's thread. A live read is legal there. That
saves the three-file snapshot freeze (`CombatantSnapshot`, `Caster`, `BukkitCombatant`) in A2 as
well, and is worth writing down so A2 does not pay for it out of habit.

---

## The refusals — two new `CastResult` arms, not a reused `Broken()`

The brief says to reuse `CastResult.Broken` + `BrokenNotice`'s 40-tick throttle rather than invent
messaging. **Reuse the mechanism; do not reuse the arm.** `Broken()`'s javadoc says *"worn to its
floor and inert until repaired"*, and `BrokenNotice` says *"Your weapon is broken — repair it before
using it."* A full-durability empty Boltor is neither, and shipping that string would be a falsified
player-facing line — the class of defect this repo tracks by name.

So: **`Empty()` and `Reloading(long ticksRemaining)`**, added to the same sealed interface, **minted
at the same site** — `WeaponFire.attempt`, after the binding is resolved and **before any cooldown or
resource is touched**, exactly as `Broken()` is at `:89-91`, so an empty weapon spends nothing and
trips no cooldown. Each gets its own `__`-prefixed `CooldownTracker` key on `BrokenNotice`'s
pattern, whose javadoc gives the reason: the tracker is *"already concurrent … already keyed per
player, and already cleared on quit … so this adds no state that can leak."*

**Adding arms is the point, not the cost.** `Broken()`'s javadoc: *"It lives in this sealed interface
anyway … so the two exhaustive switches over CastResult must handle it or fail to compile. A refusal
the caller can forget to render is a weapon that silently does nothing."*

> **BUT ONE OF THE TWO SWITCHES IS NOT ACTUALLY EXHAUSTIVE, AND THIS IS MEASURED.**
> `RpgListeners.onRightClick:511` is a real `switch` and **will** fail to compile. `WeaponSwingListener`
> uses `.filter(CastResult.Broken.class::isInstance)` — **an ignored `Optional`, which the compiler
> does not check** — and that risk is called out verbatim in that file at `:80-92`. So the left-click
> path will compile clean while silently dropping both new refusals. **Handle it explicitly and say
> in the plan that the compiler did not help here**, or the reload refusal is invisible on exactly the
> input that triggers reloads.

---

## `quiver_stone.yml` — the carrier, and the instrument

On `volley_stone.yml`'s precedent, with its header conventions copied deliberately: permanent, not
`_TEMP`; `rarity: exotic` for hotbar visibility; in no kit, no recipe, no `craft_result`; and a
header that states its numbers are nobody else's.

**It must match the Boltor's configuration on TWO axes, not one.** `material: crossbow` is necessary
and **not sufficient**: the held-repeat floor depends on the material *and* on whether the weapon
**binds `right_click`**, because binding is what cancels vanilla (`hunters_bow.yml:5` — *"binding the
shot to right_click is what suppresses the vanilla draw"*). A fixture matching on material but
differing on the binding measures a **different configuration**, and the number transfers falsely
into the Boltor's cooldown decision. **So it binds `right_click` and cancels vanilla exactly as the
Boltor will, and the gate row says that is why the number transfers** — otherwise it is a control
that succeeds for the wrong reason: it yields a plausible figure either way and nothing looks wrong.

**Numbers must collide with nothing shipped.** `volley_stone`'s header requires its numbers share
none *"with each other AND none with the Cursed Emerald."* **My first proposal of `reload_ticks: 20`
already collided** — `cursed_emerald.yml` authors `windup_ticks: 20`. Run the sweep across every
file in `content/weapons/` and `content/abilities/`, **name the set checked in the header**, and pick
from what is left. A collision-avoidance claim with no named set is the same defect as a count
against an unnamed set.

`quiver_size: 3` survived a check against the emerald and the volley fixture, **and that check was
not exhaustive** — say so, or finish it.

Also authored explicitly, on `volley_stone`'s own rule: `attack_damage`, and the quiver fields. An
omitted `quiver_size:` would resolve to a default that *"works, is invisible, and hides a decision."*

> **THIS IS ALSO WHAT ANSWERS A1's MECHANISM-UNREACHABLE PROBLEM, and the two rulings are connected
> rather than independent.** A `Quiver` that no shipped weapon equips is a guard with no instances —
> and this repo's rule is that such a guard gets deleted if mechanism-unreachable, or owes a
> forward-cover note if content-unreachable. **With `quiver_stone`, the mechanism is reachable in
> production through `/rpg give`**, so it is neither, and owes no forward-cover note. Say that in the
> code, so the next reader does not add one.

---

## THE HELD-REPEAT FLOOR — measured first, as its own row, with a count

**A number past the floor silently does nothing.** If vanilla's held-use repeat is slower than 7
ticks, slice C's dual-wield halving buys nothing: the weapon fires at the repeat rate while the
tooltip advertises 2×, and it reads as a balance opinion rather than a bug.

**And the hazard is worse than the brief assumed.** Measured in this codebase:
**`PlayerInteractEvent` is dispatched once per press. There is no held-repeat signal anywhere in the
repo** — no held-state tick loop exists, and nothing reads one. Whether vanilla re-fires
`RIGHT_CLICK_AIR` while the button is held, and at what interval, for a **use-animation material
with its interaction cancelled**, is **unknown and unmeasured**. That is not a slice-C ceiling
question; it decides whether *"HOLD RIGHT-CLICK"* is achievable at all without a new mechanism.

**So it is A1's gate row, because A1 is the first slice with an instrument.** Not *"does it feel
fast"* — **a count**:

> Hold right-click on `quiver_stone` for a fixed wall-clock window with the cooldown set to 1 tick,
> so the cooldown cannot be the limiter. **Count the shots.** The repeat interval is the window
> divided by the count. Run it on a cancelled binding, because that is the configuration that
> ships.

Write the relationship **at the constant**, the way `BEAM_ORIGIN_GAP`'s ceiling is written: *nothing
enforces this and so it is written here*, naming the measured floor, the two numbers that must stay
above it (14 single, 7 dual), and what happens if they do not. **If the floor is above 7, the
operator has a decision to make about what dual-wield actually buys**, and he should make it with
the number in hand.

---

## Core tests, and the mutations that must redden

Every row constructs `Quiver` at **capacity 3, reload 7** — never the shipped numbers — which is
guard 1 for A2's confinement.

1. **Spend to empty, then refuse.** *Two positive assertions with the capacity NAMED BEFORE THE
   RUN*, not one "does it stop":
   - **shot N lands** — an off-by-one refusing at N−1 fails here, and a stop-only row misses it;
   - **shot N+1 is refused**, with the refusal **observed** as `Empty`, never as "nothing happened",
     which is indistinguishable from a misfire.
   **Count the landed shots against the authored capacity.** *A count is the figure; a stop is not* —
   capacity N and shots-to-empty are necessarily the same number, so a row that only checks "it
   stopped at N" passes whether the rule exists or not. Two equal quantities in one row.
2. **Reload restores exactly capacity**, not capacity+1 and not the pre-spend count.
3. **The clamp** — a count above capacity resolves down, on `Durability.clamp`'s precedent.
4. **Capacity-change semantics** — increase leaves the count unchanged (headroom), decrease clamps.
   The `DESIGN-stat-engine.md` rule, exercised now so A2 inherits it proven.
5. **`25 +16% → 29`**, the named floating-point case, with its expected value.
6. **The backwards-clock guard** — `now < startedAt` reads as complete. Driven by a `LongSupplier`
   that jumps backwards, on the `MeleeHits`/`DamageWindow` injection seam, so this is a core row and
   needs no server. **Paired with a row that VARIES the reload duration between the stamp and the
   read** — without it the fixture cannot produce the failure at all, which is how the first version
   of this guard shipped with a green suite.
6b. **The floor rule in BOTH directions** — a positive modifier never delivers more than it claims,
   a negative one never delivers less. **At least one debuff case with a fractional part ≥ 0.5**
   (`8 −5% = 7.6`), or the debuff half is asserted at values where floor and `round` agree and
   therefore guards nothing — measured, not assumed.
7. **`QuiverSignatureTest`** — guard 2.
8. **The fencepost** — a reload started while the fire cooldown is still running **begins
   immediately**. The ruling above, pinned so it cannot drift into the gated branch by a later edit.

**Mutations.** Per `CLAUDE.md`, each needs the marker grepped in **both** directions (*marker
present* **and** *original gone*) **plus a measured line/byte delta or `git diff --numstat`**, because
the grep proves an edit landed and says nothing about where else. Restore from a scratchpad copy,
never `git checkout --`. Markers carry no punctuation and no `/`.

| mutation | expected |
|---|---|
| `isEmpty` `<= 0` → `< 0` | row 1's *shot N+1* reddens, row 1's *shot N* does not |
| `reload()` → `capacity - 1` | row 2 |
| remove the `clamp` in the carry | row 3 |
| `floor` expression → `base*(1+p/100)` | row 5 only, at 25 — **and NOT at 8**, which is the point |
| drop the backwards-clock guard (`MUTNOGUARD`) | row 6's restart half |
| **bound the guard by the live duration again (`MUTBOUND`)** | **row 6's mid-flight half — the defect, reintroduced** |
| `floor` → `Math.round` (`MUTROUND`) | rows 6b and the dead-zone row |
| delete the stamp in `WeaponItems.mint` | the mint test, and nothing else |

**Before mutating, `grep -c` the target string.** This repo's javadocs quote their own constants
constantly, so *a target appearing in both a comment and the code it documents is the normal case
here, not an edge one* — and a non-global `s///` replaces the comment, which comes first in the file.

---

## Boot gate — `GATE-quiver.md`

Kill orphaned `java.exe` first; the script dies, two JVMs do not, and they hold the jar.

| # | check | expected |
|---|---|---|
| Q1 | boot log | clean load, **zero skipped content**, and **no unknown-key warning from any shipped weapon** — the `WeaponLoader` guard staying quiet on real content, which `everyShippedWeaponFileIsSilentUnderTheUnknownKeyCheck` already pins in unit form |
| Q2 | `/rpg give quiver_stone`, fire and **count** | shot N lands; shot N+1 refused, refusal **seen** |
| Q3 | left-click mid-magazine | reload starts; firing refused for the authored window; then full |
| Q4 | **fire once, then relog** | the count is **exactly what it was** — the `carryInstanceData` row |
| Q5 | fire once, then `/rpg refresh` | same. Row Q4 without the relog, so a pass names which funnel |
| Q6 | fire once, then use the enchant table | same — the third `remint` caller |
| Q7 | **the repeat-floor count** | the measurement above, with the number recorded |
| Q8 | two `quiver_stone`s, one spent | **two halves, and only one is new.** *They do not stack* re-witnesses `setMaxStackSize(1)`, which this slice now also unit-tests; *the spent one stays spent* is **the new witness** — per-item state, not per-weapon-id. A pass must name which half it saw |
| Q9 | start a reload, swap away, swap back | resumes; does not restart, does not complete early |
| Q10 | author a file with a typo'd quiver key | boot **warns by name** — `WeaponLoader`'s unknown-key guard. **Downgraded to confirmatory**: it is already caused-not-asserted in `WeaponLoaderTest` against the real `loadAll`, so this row witnesses the wiring rather than the rule |
| Q11 | fire once, left-click on the next tick | the reload **starts at once** — the fencepost ruling, witnessed in play. Core row 8 is the authority; this confirms the wiring kept it |

**Q4, Q5 and Q6 are the discriminating rows**: they are the only checks that the carry landed, and
each names a different funnel caller, so a pass says *which*.

> **THIS SENTENCE USED TO READ "Q10 is the only row that fails if the validator arm was written but
> is unreachable", AND IT WAS WRONG IN THE WORSE DIRECTION.** The arm had not been written at all —
> `git diff --numstat origin/master origin/feat/quiver -- '*ContentValidator*'` returned zero rows —
> so **Q10 was a row that could not pass**, for a reason that sentence did not contemplate. Caught in
> review, not by me.
>
> **The rule that catches this was quoted in this very plan, one section above the guard that went
> missing:** *"THE PLAN ITEMS THAT SILENTLY FAIL TO LAND ARE THE GUARDS. Not a random sample —
> selection. A missing feature is reported by the person who wanted it; a missing guard produces no
> symptom at all."* Its remedy is the named-artifact diff, and **not running it is what let this
> through.** It has now been run over every artifact this plan names, mechanically rather than from
> recollection, and the table is in the commit report.

> **Q8's stack half is kept even though a unit test now covers it, and the precedent is why.**
> `GATE-lapis-staff.md` L10/L10b: the in-game stack action is what exposed a comment that had been
> false for nine days with a green suite. But the row must say which half it is witnessing, or a
> pass reads as evidence for both when only one of them was ever at risk.

---

## Prose corrections owed

1. **`CLAUDE.md`, Standing decisions** — the no-stack rule gains its second and stronger reason.
2. **`NEXT.md:1797-1804`** — **the owed mint test is NOT payable as phrased, and this plan scoped it
   into A1 wrongly.** The debt asks for *"a mint test per gear kind"*, and `new ItemStack(...)` throws
   *"No RegistryAccess implementation found"* without a running server with no MockBukkit in the
   project — `WeaponItemsTest`'s own class javadoc records this and tests the DECISION instead of a
   constructed item. So `setMaxStackSize(1)` cannot be witnessed by any unit test in this module.

   **Rewrite the debt beside the debt**, not only in a commit report: either re-phrase it as
   something payable (a decision-level assertion, or a source-level check) or mark it
   **operator-gate-only**, with gate row Q8 named as its sole witness. *A debt phrased as impossible
   is a debt that gets skipped forever and re-read as outstanding* — and it has already survived one
   slice that way. **Not marked paid here.**
3. **`PlayerHealthSystem.java:171`** — *"Eleven stats converge on the same scan"* becomes thirteen.
   **A2's correction, not A1's**, named here so it is not missed. Note the irony on the record: that
   comment carries a 2026-era note saying it was *"phrased now so it cannot go stale again"*, and it
   still leads with a count.
4. **`hunters_bow.yml:8`** — *"Left-click stays free for a future melee/special"* is still true of
   the bow and becomes false of quiver weapons generally. Leave the bow alone per the ruling; state
   the rule in `quiver_stone.yml`'s header instead.

---

## Verification

- `./mvnw -pl core test` — the daily loop, all new rows.
- `./mvnw clean package` as the **final** verify, and **every suite figure re-read from that run,
  after the last file lands**, quoted with its breakdown (`core / storage / paper`) so the total is
  checkable by addition. Never from the run that was green when a paragraph was written.
- `./scripts/check-jar.sh`, `./scripts/check-tests.sh`.
- `GoldenLoreTest` — a quiver line changes `golden-lore.txt` for `quiver_stone` **only**; regenerate
  with `-Dgolden.regenerate` and confirm no other weapon's block moved.
- All six mutations run and **reported as observed**, with both grep directions and a `--numstat`
  delta per edit.
- Report the file list from `git diff --numstat` **and** from what was touched, and **reconcile them
  in both directions** — `git status --porcelain` for the new files, since `--numstat` cannot see an
  untracked one and a four-row report that omits the plan reads as complete.
- `git ls-remote --heads origin` pasted before and after any push. *"Pushed" and "not pushed" are
  both claims.*

---

## Carried forward, unresolved — so slice B does not re-present my errors

**The fire-rate anchors I gave the operator were quoted past a fencepost.** Eight shots at 14 ticks
**span 4.9 s** (t = 0, 0.7 … 4.9), not 5.6. The 5.6 assumes **the eighth shot's cooldown elapses
before the reload starts** — an unruled mechanic: *nobody has said whether the fire cooldown gates
the reload input.* Executed, both branches:

| | single | dual (16 @ 7t, 6 s sequential reload) | ratio |
|---|---|---|---|
| cooldown gates reload | 8.60 s → **0.9302 sh/s** | 11.60 s → **1.3793 sh/s** | **1.48×** |
| reload interrupts freely | 7.90 s → **1.0127 sh/s** | 11.25 s → **1.4222 sh/s** | **1.40×** |

**~9% apart, and every DPS figure inherits it.** A figure quoted to three significant figures on top
of an unruled mechanic reads as measured and is not — the same shape as a suite total written before
the last file lands.

> **RULED IN A1, NOT DEFERRED — see the fencepost section above.** An earlier draft handed this to
> slice B, which would have been **deciding it by omission**, because A1 ships the reload and its
> implementation *is* the answer. The ruling is **ungated**, so **the second row is the live one**:
> slice B quotes **1.0127 / 1.4222 sh/s, ratio 1.40×**. The first row stays on the record as the
> branch that was refused, with its reason, so a future retune argues with a ruling rather than
> rediscovering a fork.

**Dual-wield is NOT 2×, under either branch.** Sequential reloads double the reload alongside the
fire rate, so it buys a **2× burst for 5.6 s and then hands most of it back**. The operator should
see that before he rules a damage number, not after.

**Two further labels the anchors were missing**: every figure is **pre-crit** (a 15% coin flip that
cannot be disabled — it does not move the *relative* comparison, but these are not the numbers a
player sees), and the comparison is **not like for like** — the emerald's sustain is **mana**-gated
at 26.7 mana/s against a 100 pool by its own comment, the Boltor's is **reload**-gated. Putting their
DPS side by side without naming each brake invites a pick against the wrong constraint.

**Also carried, for slices B and C:**

- **`attack_damage`: UNRULED.** `GATE-boltor.md` is the authority.
- **The off-hand ruling is still owed.** The old repo fired an **off-hand-only** Boltor at the normal
  rate; the new repo's `event.getHand() != EquipmentSlot.HAND` filter means it would **not fire at
  all**. Two repos reached the same main-hand guard independently, so the guard stays — but which
  behaviour ships for an off-hand-only Boltor must be **stated in slice C's plan, not left to fall
  out.**
- **No pierce.** `stepRay`'s javadoc: *"The walk stops at the first body … if rays are ever made to
  PIERCE … a set of already-hit ids would have to be threaded through these calls."* A crossbow that
  stops at the first mob may or may not be wanted — slice B states it.
- **A ray past 32 blocks draws only its near half** (client particle cap, measured `GATE-volley` V3).
  It still **hits** at full range. Ranger is the first class where long range is the point, so
  whoever authors `range:` owns this.
- **The Boltor is the load test for `BEAM_ORIGIN_GAP`.** Its magnitude was adopted at 1.0 on a
  ruling *answered by blanket*, never compared against 0.5 or 1.5, and G5a's per-setting figures were
  never taken. The Cursed Emerald put **six** beams up and CE4 read *"hard to see through"*; a
  dual-wielded held-fire Boltor draws one every 7 ticks, indefinitely, from the eye. **Slice B's gate
  needs a beam-density row from the start** — both Particles settings, near and far staging — rather
  than rediscovering CE4 the hard way. A bad result is a finding about the magnitude, and the
  `ADOPTED` javadoc is written to be argued with.
- **`CastSpec.Volley` stats are not atomic** (`cursed_emerald.yml:193-200`): a mid-burst swap
  re-prices later shots off whatever is in hand. **Directly relevant to slice C** — a per-shot
  decrement in a volley is a per-shot read of live player state, the exact shape that note warns
  about. *"Do not read this weapon's stability as evidence that volleys are stable."*
- **Alternation is a new mechanism wearing an old name.** The old repo's `boltorNextShotOffHand`
  alternated only the arrow's **spawn position** by ±0.35 blocks — *"Direction stays the centered eye
  vector so aim is unaffected"* — with no quiver and nothing to run out of. Making it decide **which
  quiver decrements** is new. Plan and test it as new.
