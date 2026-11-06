# Shields, Slice 2b — Thorns, the reflect-to-attacker enchant

Branched off **`09c42eb`**, verified from the wire. With this the three original shield goals are
complete: block DR (Slice 1), Bulwark and the gear-gating axis (2a), and the reflect.

## Finalisation pass -- six changes after the first review

Folded into this branch before merge, and every number below RE-EXECUTED rather than carried over:

1. **The enchant is `Thorns`, not `Riposte`.** `DESIGN-status-effects.md` yields the name -- see the
   vacation note there. The propagation status takes a new name when it is built, and its four
   anti-loop safety rules travel with the MECHANIC, not with the word. `EnchantEffect.REFLECT` is the
   mechanism and is unchanged.
2. **The shield is `shield`, not `roundshield`** (id, display name, filename). No collision: the id
   comes from the filename and `material:` is a separate key, and no weapon is called `shield`.
3. **`block_dr` 0.5 -> 0.35.** Every pinned constant re-executed; see below.
4. **The reflect line is gone from the shield lore.** Thorns' number lives on the enchant line only.
5. **The Damage Reduction NUMBER is GREEN**, read off `StatsBarText.DEFENSE_COLOR` rather than
   picked -- a shield's reduction and armor's Defense are the same kind of number and compose, so
   they read the same colour. (Adventure has no `LIME`; `GREEN` is the bright one.)
6. **The stat is "Damage Reduction", not "Block"**, on the item AND in Bulwark's effect line, so the
   gear and the enchant that modifies it cannot name one stat two ways.

### What re-executing 0.35 changed, beyond the obvious

```
  plain        dr 0.35                 ->  a 15.0 hit passes 9.75
  Bulwark I    dr 0.39999999999999997  ->  9.000000000000002    tooltip 40%
  Bulwark II   dr 0.44999999999999996  ->  8.25                 tooltip 45%
  Bulwark III  dr 0.5                  ->  7.5                  tooltip 50%
```

**The drs are NOT the clean 0.40 / 0.45 they look like** -- `0.35 + 0.05` is `0.39999999999999997`.
The tooltip's one-decimal rounding is what renders them tidily; the tests carry the exact doubles.

**And the rationale for ADDITIVE changed.** At 0.5 the two rejected readings were bit-identical to
each other, so the shipped shield could tell "wrong" from "right" but never which wrong. At 0.35 all
three separate (`0.3999... / 0.3675 / 0.3825`), so the shipped base now discriminates the rule by
itself. `BulwarkTest` keeps 0.5 as `LEGACY_HALF` -- the coincidence there is precisely the case a
blind test would survive -- and adds an assertion that the shipped base separates.

**The gate discriminators moved too.** The rejected off-pass-through reading now reads **1 / 2 / 3**
rather than 1 / 2 / 2, because the pass-through is 9.75 instead of 7.5.

---

## Context

2a built the rails — the `GearClass` axis, the shield roll, the table, `BlockEnchantItems`' effect
scan, and the broken-shield gate whose own comment reserved a place *"for the reflect in Slice 2b"*.
Thorns rides all of it.

**The one genuinely new thing is the reflect seam: damage dealt back OUT of the mob→player rider, to
a second entity, credited to the blocking player.** That had never been done here.

---

## What shipped

**Thorns** reflects `10/20/30%` of the **pre-mitigation** blow — the attacker's raw attack stat,
before the block fraction *and* before the victim's armor. Off the 15.0 gate mob that is
`1.5 / 3.0 / 4.5`, which the damage popup **rounds to 2 / 3 / 5**.

Pre-mitigation is forced, not chosen: the post-mitigation figure does not exist yet on the thread
where the reflect is computed — the identical constraint `SweepShare` records. It is also what keeps
Thorns and Bulwark independently tunable. **A heavily armored player reflects more than the hit did
to them.** Call it *pre-mitigation*, never *pre-block*.

**It is called Thorns, and it took the name.** `DESIGN-status-effects.md` held "Thorns" for a Nature
propagation status with four anti-loop safety rules attached — a reservation with no code, no content
file and no slice. This shipped, so the doc **vacated** the name; see its "THE NAME THORNS IS
VACATED" note. There is exactly one Thorns in this project and it is this enchant.

The safety rules did **not** come with the word: they are about propagation, which this mechanic has
none of. The propagation status takes a new name when it is built and the rules stay attached to it
there.

**The popup stays white.** `DamageNumberText` has two styles and colour means one thing: *this hit
crit*. A reflect is computed directly and never passes through the crit multiplier, so it *cannot*
crit — white says so honestly. The two-arg `applyDamage` makes that structural rather than a `false`
someone can flip, matching the sweep rider.

### `ShieldExchange` — a testability decision, not tidiness

Thorns's single load-bearing rule lived in `RpgListeners.onMobMeleeAttack`, which **cannot be
unit-tested** (a live `Player`, a live `LivingEntity`, a real `BLOCKING` modifier). A pure
`Thorns.reflected` test pins the arithmetic but cannot say *which* value the rider passed.

So the choice moved into a pure core record: `ShieldExchange.of(preMitigation, blocked, effectiveDr,
reflectPercent)` returns both numbers from one input. The reduction happens **inside**, so the rider
holds no reduced local to mis-pass — **there is no `incoming` variable in the rider any more.**

Proven by running the mutation it exists to catch (reflect off the reduced figure), which reddens 4:

```
"30% of the RAW 15.0 goes back -- 2.25 here means it reflected off the pass-through
 ==> expected: <4.5> but was: <2.25>"
```

It does **not** cover the deal-order or the inline `requireOwned` throw. Those stay boot-gated.

### The seam

```java
final double preMitigation = adapters.stats().attackValue(attacker.getUniqueId());
ShieldBlock.Outcome block = ShieldBlock.resolve(...);
ShieldExchange exchange = ShieldExchange.of(
        preMitigation, block.blocked(), block.effectiveDr(), block.reflectPercent());
…
event.setDamage(TOKEN_DAMAGE);
BukkitCombatant.of(victim, adapters).handle().applyDamage(exchange.applied(), attacker.getUniqueId());
if (Thorns.reflects(exchange.reflected())) {
    BukkitCombatant.of(attacker, adapters).handle().applyDamage(exchange.reflected(), victim.getUniqueId());
}
```

Byte-for-byte the construction `onPlayerSweepAttack` already uses — dealing custom damage to a second
entity credited to a dealer is proven. What is new is the *direction*.

**Why the thorns is LAST, and it is not tick ordering.** Both `applyDamage` calls defer to their
entity's next tick, so "the victim's damage lands first" holds on Paper by FIFO accident and is
meaningless on Folia. What *is* ordering-sensitive is the **throw**: `BukkitCombatant.of` runs INLINE
and its first act is `Regions.requireOwned`. Placed above, that throw skips `setDamage` — so
**vanilla's full damage lands on the player** — and skips the custom hit too. Placed last, a throw
costs the thorns and nothing else.

**`seedCombatStats` is the reflect's precondition**, not just the nameplate's: it is what makes the
mob tracked, and `CombatantStats.damage` is a silent no-op on an untracked combatant.

**One decode.** `ShieldBlock.resolve` hoists `EnchantItems.read` and scans the state twice, so a
blocked hit costs one PDC parse for both enchants. `reflectPercent` is `Thorns.NONE` on
`Outcome.NONE`, so a back-hit, an untagged shield, a dangling id and a broken shield all send nothing
back with no extra branch — one predicate, all three shield effects.

---

## Two defects this slice found in 2a's own work

**1. The validation switch was NOT exhaustiveness-checked, and 2a's comment said it was.**

Adding `EnchantEffect.REFLECT` was supposed to break two switches. It broke **one**.
`EnchantDefinition`'s validation was a switch **statement**, and Java only enforces exhaustiveness on
switch **expressions** — so the new constant fell through to **no validation at all**. For a reflect
that means an unvalidated negative percent reaching `stats.damage` and **healing the attacking mob**.

Fixed by choosing the rules as values (two switch *expressions* picking the `Gate` and whether a curve
is required). Proven: dropping `REFLECT` now gives `the switch expression does not cover all possible
input values`, BUILD FAILURE.

**2. `EnchantEffectLineTest` had zero coverage of the `BLOCK_DR` arm** — `grep -c` returned 0 — while
its javadoc claimed it asserted "every arm". It asserted every arm its hand-listed fixture array knew
about. Same discovery trap as the loader fixture 2a fixed one file over.

Also closed: **`BlockEnchantItems`' effect filter was unguarded.** 2a's cross-effect test used
Unbreaking, whose curve is empty, so deleting the filter returned `0.0` either way. Bulwark and
Thorns are the first two enchants that both carry curves and bind different mechanisms; the mutation
now reddens with `expected: <15.0> but was: <45.0>`.

---

## Verification

Baseline at `09c42eb`: **core 505 / storage 17 / paper 340.**
Final: **core 519 / storage 17 / paper 353. BUILD SUCCESS.**

### Mutations run — marker-grepped, `test-compile` first, restored from the scratchpad

| Mutation | Result |
|---|---|
| `ShieldExchange`: reflect off the reduced figure | **4 red** — the trap that was previously boot-only |
| `EnchantCurve.percentAt`: ignore the level | 7 red in `DamageEnchantsTest`, **through the delegation** |
| `EnchantDefinition`: drop `REFLECT` from the Gate expression | **compile error** — the restored guarantee |
| `BlockEnchantItems`: delete the effect filter | 2 red, `expected: <15.0> but was: <45.0>` |
| `EnchantEffectLine`: copy-paste "block" into the reflect arm | 1 red, naming both strings |
| `thorns.yml`: `effect: block_dr` | 1 red — a typo that was previously invisible |

**A comment that overclaimed, caught by running the mutation** (the same failure 2a's `BulwarkTest`
made). The cross-product wording test's first comment said only it could catch the copy-paste. It
**cannot** — the curves differ, so the strings are `"+15% block"` and `"+30% block"`, unequal, and it
passes. The byte-for-byte wording assertion is what reddens. Comment corrected.

**`DamageEnchantsTest` passes with ZERO edits** — that is the `EnchantCurve` move's faithfulness
check. There is deliberately no `EnchantCurveTest`: a mutation already reddens through the delegation.

---

## Boot gate -- RUN AND PASSED IN FULL, all nine rows

`./scripts/dev-server.sh --refresh-content`.

**Row 1, RE-RUN 2026-08-29 16:05 after the finalisation pass** (the first run at 15:19 predates the
rename and the 0.35 base, so it is superseded). Paper 26.1.2.build.74, deploy verified by mtime AND
size (both `469547` bytes) and the RENAMED content confirmed inside the shaded jar --
`content/enchants/thorns.yml`, `content/shields/shield.yml`:

```
[Rpg] Loaded 6 abilities, 7 visuals, 5 statuses, 7 elements, 6 enchants,
      2 kits, 5 weapons, 1 shields, 1 mobs
Done (5.096s)!
```

ZERO `Skipping malformed`, ZERO id-collision warnings (the shield id `shield` against the `shield`
material token and against every weapon id), no Rpg-sourced WARN or exception. So `class: shield`,
`effect: reflect` and `block_dr: 0.35` all parse through the real loaders on a live server.
**It is a load check and establishes nothing mechanical.**

> **The file lock fired for real this time, and it cost a run.** `dev-server.sh`'s own `rm -f` hit
> `Device or resource busy`, `set -e` aborted before it deployed, and the server never booted -- the
> exact sequence CLAUDE.md records. Two orphaned `java.exe` from an earlier boot held the jar.
>
> **And the check that missed them was `tasklist /FI ... | grep -c java.exe`, which reported 0 while
> two were running.** `Get-CimInstance Win32_Process -Filter "Name='java.exe'"` found both. Use that;
> and prove the lock is gone by actually opening the jar exclusively, not by trusting a process list. **Give a FRESH shield** — `rollOnAcquire` fires only
at acquisition, so a 2a shield carries no `enchant_rolled` flag and will never roll Thorns.

**The popup ROUNDS.** Executed: the gate reads whole numbers.

| Thorns | raw | **popup** | the REJECTED off-pass-through reading |
|---|---|---|---|
| I | 1.5 | **2** | 1 |
| II | 3.0 | **3** | 2 |
| III | 4.5 | **5** | 3 |

Gate on **III (5 vs 3)**; I is only one apart. Pin the **absolute** value: adding Bulwark
moves the hit the player takes but must NOT move the reflect, so the row is "III still reads 5", not
"the number changed".

| # | Check | Expected |
|---|---|---|
| 1 | boot log | `Loaded … 6 enchants`; no `Skipping malformed enchant` |
| 2 | `/rpg give shield` ×3, open the table | a slot offers **3** candidates — first time in shipped content |
| 3 | the shield tooltip, plain | `Damage Reduction: 35%` -- label gray, number GREEN, matching the Defense stat |
| 4 | Thorns I / II / III **alone**, block a 15.0 mob | popup over the mob reads **2 / 3 / 5**; the hit itself passes 9.75 |
| 5 | **Bulwark III also active**, Thorns III | reflect still **5**, and `Damage Reduction: 50%` on the tooltip |
| 6 | hit from behind with Thorns active | **no reflect and no reduction** — one predicate |
| 7 | a reflect that kills | blocker gets the kill, drops, XP and both statistics |
| 8 | `/rpg durability set 334`, block twice | after the break: **no reflect**, and the full 15.0 lands |
| 9 | watch the mob on each reflect | it **flashes red**, with no vanilla hurt sound |

### GATE RESULT — RUN AND PASSED IN FULL

**Rows 2-9 reported passing by the operator at the keyboard, 2026-08-30.** Row 1 was the machine's
(`Loaded … 6 enchants … 1 shields`, `Done (5.096s)`, zero skipped, zero id-collision warnings).

So the whole of Slice 2b is witnessed: the reflect fires out of the mob→player rider and is credited
to the blocker, it is independent of Bulwark, it falls off the same predicate as the reduction, a
lethal reflect pays out, and the shield's new stat line reads as intended in the new colour.

### Row 4 is MEASURED, not merely observed — the popup read **5** at Thorns III

Confirmed by the operator, 2026-08-30. That single figure is the most load-bearing datum in the whole
gate, because **it is the one thing no unit test can reach**: whether the RIDER passed the
pre-mitigation blow. `Thorns.reflected` and `EnchantCurve.percentAt` are both unit-tested, so the
arithmetic and the level→percent lookup were never in question; only the value selection at the call
site was, and `ShieldExchange` moved as much of that into core as could be moved.

**A reading of 5 is produced by the shipped path and by nothing else.** Executed against the real
classes rather than reasoned about — every rival lands somewhere a glance can tell apart:

```
  SHIPPED   off the pre-mitigation 15.0            -> 5
  REJECTED  off the pass-through 9.75              -> 3
  REJECTED  off the pass-through, Bulwark III on   -> 2
  SWAPPED   effect args (reflect reads Bulwark 15%)-> 2
  SWAPPED   and off the pass-through               -> 1
```

So the one figure settles **two** things at once:

1. **The reflect is off the pre-mitigation blow.** The `final` capture in the rider reached the seam
   as designed; the rejected off-pass-through reading is ruled out.
2. **The two `percentFor` effect arguments in `resolve` are not transposed** — that swap reads 2, not
   5, and it is invisible to every unit test in the suite.

**And it settles the second one whether or not Bulwark was equipped**, since every swapped or
pass-through variant lands at 1, 2 or 3. The table's "Thorns alone, no Bulwark" confound was still
right to state — it is what makes the row easy to read — but this particular conclusion does not rest
on it. The other confound stands unchanged: do not swing while blocking, or the extra number over the
mob is your own melee.

**Rows 2, 3 and 5-9 remain operator-observed rather than measured**, at the granularity they were
reported, and that is the Slice 1 precedent kept deliberately: *"THE EXACT HP FIGURE WAS NOT CAPTURED
into this record, so the row is logged as operator-observed rather than measured."* None of them
carries a rival reading that a wrong implementation could plausibly produce, which is why row 4 was
the one worth chasing a number for and they are not.

**Two confounds the gate must control.** Run row 4 with **Thorns ALONE, no Bulwark** — swapping the
two `percentFor` effect arguments inside `resolve` is invisible to every unit test and presents as a
wrong-but-plausible ladder. And **do not swing while blocking**: a mob→player hit paints no popup, so
during a pure block the reflect is the only number over the mob.

**A bonus row Thorns earns.** It gives Slice 1's negative-zero trap its first *visible* instrument:
with Thorns III equipped, a hit from behind with the shield raised must paint **no number**. If the
strict `<` were ever relaxed to `<=`, a white `5` appears over a mob that hit you in the back.

**Three side effects to record, not discover.** The mob flashes red with no hurt sound
(`playHurtAnimation` fires because the *mob's* i-frames are clear); `mob.setTarget` fires an
`EntityTargetLivingEntityEvent` per reflect (inert today, but it can override a different target);
and a lethal reflect from a player who disconnects inside the one-tick deferral window kills the mob
with **nothing credited** — `dealerIsPlayer` goes false, so no popup, drops, XP or statistics, though
the damage still lands.

---

## Out of scope

A reflect on a vanilla shield (still zero-protection), any change to what "broken" means (2a settled
it: no custom mitigation, not dead — vanilla still dampens knockback), a `ShieldRefresher` for the
join path, and a shared `Gear` abstraction (armor is the trigger).
