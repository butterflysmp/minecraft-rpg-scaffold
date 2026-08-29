# Shields, Slice 2b — Riposte, the reflect-to-attacker enchant

Branched off **`09c42eb`**, verified from the wire. With this the three original shield goals are
complete: block DR (Slice 1), Bulwark and the gear-gating axis (2a), and the reflect.

## Context

2a built the rails — the `GearClass` axis, the shield roll, the table, `BlockEnchantItems`' effect
scan, and the broken-shield gate whose own comment reserved a place *"for the reflect in Slice 2b"*.
Riposte rides all of it.

**The one genuinely new thing is the reflect seam: damage dealt back OUT of the mob→player rider, to
a second entity, credited to the blocking player.** That had never been done here.

---

## What shipped

**Riposte** reflects `10/20/30%` of the **pre-mitigation** blow — the attacker's raw attack stat,
before the block fraction *and* before the victim's armor. Off the 15.0 gate mob that is
`1.5 / 3.0 / 4.5`, which the damage popup **rounds to 2 / 3 / 5**.

Pre-mitigation is forced, not chosen: the post-mitigation figure does not exist yet on the thread
where the reflect is computed — the identical constraint `SweepShare` records. It is also what keeps
Riposte and Bulwark independently tunable. **A heavily armored player reflects more than the hit did
to them.** Call it *pre-mitigation*, never *pre-block*.

**Named Riposte, not Thorns.** `DESIGN-status-effects.md` reserves Thorns for a Nature propagation
status with four anti-loop safety rules attached. Rename the mechanic with no load-bearing
associations, not the one that has them.

**The popup stays white.** `DamageNumberText` has two styles and colour means one thing: *this hit
crit*. A reflect is computed directly and never passes through the crit multiplier, so it *cannot*
crit — white says so honestly. The two-arg `applyDamage` makes that structural rather than a `false`
someone can flip, matching the sweep rider.

### `ShieldExchange` — a testability decision, not tidiness

Riposte's single load-bearing rule lived in `RpgListeners.onMobMeleeAttack`, which **cannot be
unit-tested** (a live `Player`, a live `LivingEntity`, a real `BLOCKING` modifier). A pure
`Riposte.reflected` test pins the arithmetic but cannot say *which* value the rider passed.

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
if (Riposte.reflects(exchange.reflected())) {
    BukkitCombatant.of(attacker, adapters).handle().applyDamage(exchange.reflected(), victim.getUniqueId());
}
```

Byte-for-byte the construction `onPlayerSweepAttack` already uses — dealing custom damage to a second
entity credited to a dealer is proven. What is new is the *direction*.

**Why the riposte is LAST, and it is not tick ordering.** Both `applyDamage` calls defer to their
entity's next tick, so "the victim's damage lands first" holds on Paper by FIFO accident and is
meaningless on Folia. What *is* ordering-sensitive is the **throw**: `BukkitCombatant.of` runs INLINE
and its first act is `Regions.requireOwned`. Placed above, that throw skips `setDamage` — so
**vanilla's full damage lands on the player** — and skips the custom hit too. Placed last, a throw
costs the riposte and nothing else.

**`seedCombatStats` is the reflect's precondition**, not just the nameplate's: it is what makes the
mob tracked, and `CombatantStats.damage` is a silent no-op on an untracked combatant.

**One decode.** `ShieldBlock.resolve` hoists `EnchantItems.read` and scans the state twice, so a
blocked hit costs one PDC parse for both enchants. `reflectPercent` is `Riposte.NONE` on
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
Riposte are the first two enchants that both carry curves and bind different mechanisms; the mutation
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
| `riposte.yml`: `effect: block_dr` | 1 red — a typo that was previously invisible |

**A comment that overclaimed, caught by running the mutation** (the same failure 2a's `BulwarkTest`
made). The cross-product wording test's first comment said only it could catch the copy-paste. It
**cannot** — the curves differ, so the strings are `"+15% block"` and `"+30% block"`, unequal, and it
passes. The byte-for-byte wording assertion is what reddens. Comment corrected.

**`DamageEnchantsTest` passes with ZERO edits** — that is the `EnchantCurve` move's faithfulness
check. There is deliberately no `EnchantCurveTest`: a mutation already reddens through the delegation.

---

## Boot gate -- ROW 1 RUN AND PASSED; ROWS 2-9 OWED BY A HUMAN

`./scripts/dev-server.sh --refresh-content`.

**Row 1, run 2026-08-29 15:19.** Paper 26.1.2.build.74, deploy verified by mtime AND size before
booting (target and deployed both `469324` bytes) and `riposte.yml` confirmed inside the shaded jar:
`Loaded 6 abilities, 7 visuals, 5 statuses, 7 elements, 6 enchants, 2 kits, 5 weapons, 1 shields,
1 mobs` / `Done (6.477s)`, with ZERO `Skipping malformed enchant`. Six enchants, up from five, so
`effect: reflect` binds through the real `EnchantEffect` and `class: shield` through the real
`GearClass` on a live server. **It is a load check and establishes nothing mechanical.** **Give a FRESH roundshield** — `rollOnAcquire` fires only
at acquisition, so a 2a shield carries no `enchant_rolled` flag and will never roll Riposte.

**The popup ROUNDS.** Executed: the gate reads whole numbers.

| Riposte | raw | **popup** | the REJECTED off-pass-through reading |
|---|---|---|---|
| I | 1.5 | **2** | 1 |
| II | 3.0 | **3** | 2 |
| III | 4.5 | **5** | 2 |

Gate on **III (5 vs 2)**; I is only one apart. Pin the **absolute** value — with Bulwark III the
rejected reading still rounds to 2 at III, so "the number did not move" is *not* the discriminator.

| # | Check | Expected |
|---|---|---|
| 1 | boot log | `Loaded … 6 enchants`; no `Skipping malformed enchant` |
| 2 | `/rpg give roundshield` ×3, open the table | a slot offers **3** candidates — first time in shipped content |
| 3 | the shield tooltip at Riposte III | `Reflect: 30% to attacker` beneath the block line |
| 4 | Riposte I / II / III **alone**, block a 15.0 mob | popup over the mob reads **2 / 3 / 5** |
| 5 | **Bulwark III also active**, Riposte III | still **5**, and `Block: 65%` on the tooltip |
| 6 | hit from behind with Riposte active | **no reflect and no reduction** — one predicate |
| 7 | a reflect that kills | blocker gets the kill, drops, XP and both statistics |
| 8 | `/rpg durability set 334`, block twice | after the break: **no reflect** |
| 9 | watch the mob on each reflect | it **flashes red**, with no vanilla hurt sound |

**Two confounds the gate must control.** Run row 4 with **Riposte ALONE, no Bulwark** — swapping the
two `percentFor` effect arguments inside `resolve` is invisible to every unit test and presents as a
wrong-but-plausible ladder. And **do not swing while blocking**: a mob→player hit paints no popup, so
during a pure block the reflect is the only number over the mob.

**A bonus row Riposte earns.** It gives Slice 1's negative-zero trap its first *visible* instrument:
with Riposte III equipped, a hit from behind with the shield raised must paint **no number**. If the
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
