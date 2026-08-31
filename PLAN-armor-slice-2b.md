# Armor, Slice 2b — Mana Bank, and per-player Max Mana

Branch `feat/armor-slice-2b` off **`fc4b9ee`**, verified from the wire.

Slice 2a shipped Protection and Growth. Growth was *wiring* — `HealthState.max` already had a
`Stat`, a `setMaxModifier`, a target in the reconciler and `clampCurrentToMax`. Mana Bank had none of
that: `ResourcePool.max` was a single `final double` shared by every player on the server.

So 2b builds the mana equivalent first and hangs one enchant off it. Everything downstream was
already generic — `EnchantValues` is parameterized by `EnchantEffect`, `GearLore.appendFlatBonus`
takes a label and a colour, `bonusValue` is stat-agnostic, `StatsBarText.MANA_COLOR` is public — so
the tooltip cost one line. Six javadocs named the Mana Bank case by hand before it existed.

---

## Two corrections to the brief, both load-bearing

### There were FOUR reads of the global max, not three

| where | read |
|---|---|
| `current()` | an unseen owner is full |
| `regenerated()` | the `Math.min` ceiling |
| `tryConsume` | the `amount > max` never-satisfiable guard |
| **`tryConsume`, inside the `compute` lambda** | **a verbatim duplicate of the first** |

The fourth is why a grep found three and reading found four. `tryConsume` now resolves the ceiling
once at the top and passes the local down to `regenerated`.

### The DECREASE clamp was nearly free; the INCREASE was the broken one

`ResourcePool` stores *(amount, tick)*, not a current value, so `Math.min` in the regen path already
pulled current down on the next read. The explicit clamp is about **stating** it.

The increase side had no such luck, and was inconsistent in production before this slice:

- Owner **with** an entry → stored amount untouched, ceiling rises → **headroom**.
- Owner **with no** entry (never cast, just rejoined, just `/rpg mana refill`) → `current()` returns
  the **new** max instantly → **a free +30 mana**.

Same enchant, two behaviours, decided by state a player cannot see.

---

## Decisions taken

1. **Explicit clamp on max-mana decrease**, mirroring `clampCurrentToMax` — one rule for players
   across both max-stats, and stated where a refactor of `regenerated` cannot quietly take it.
2. **`MAX_CANDIDATES` stays 3.** Armor's pool becomes 4 and `candidateCount`'s `min(pool, cap)` does
   its designed job for the first time. Raising it would defeat subset-scarcity for every gear kind
   and would not let a player run four anyway — three slots × one active each is three.

   > Raising the cap was also **not** the one-constant change it looks like. `rawSlotFor(slot, cand)`
   > is `(2 + cand) * 9 + (2 + 2 * slot)`, so a 4th candidate lands on row 5 at 47/**49**/51 — and
   > `INPUT_SLOT` is 49. `renderCandidates` writes unconditionally (unlike `render()`, which skips
   > `INPUT_SLOT`), so it would paint a candidate icon over the player's armor and `onClose` would
   > hand back the icon.

---

## What was built

**Per-player Max Mana.** `HealthState` gains a ninth `Stat`; `CombatantStats` gains
`maxManaBonusValue` and `reconcileMaxManaModifiers`. SILENT, by the `reconcileDefenseModifiers`
precedent — `StatsBarSystem` polls the field every 10 ticks, so an event would be a second route to
one redraw. `ResourcePool` takes a `MaxResolver` and keeps its old `(LongSupplier, double, double)`
constructor, which is what left all 11 `ResourcePoolTest` tests and the five other construction sites
byte-identical — the faithfulness check the `EnchantCurve` lift used.

**Deviation from the plan, deliberate.** The plan called for a `stats.tracks(owner)` guard in the
resolver, because `CombatantStats.max(id)` throws for an untracked id and a mob firing a costed
trigger between bootstrap and register would have thrown from inside a cast. Modelling the stat as a
**bonus with base 0.0** instead of a total with base 100.0 makes the accessor total by construction
— the shape `defenseValue` already had — so there is no guard to forget. The base stays `MAX_MANA` in
paper, which is where the archetype pass wants it.

**The resolver is scoped to `DEFAULT_RESOURCE`.** `pools` is keyed by `(owner, resourceId)` and the
class promises "mana, and whatever else content asks for". A bare per-owner max would raise the cap
on every future resource at once, and `resourcesAreTrackedSeparatelyPerId` would stay green while
being wrong.

**One mechanism for both directions.** Read before, reconcile, write back on change.
`reconcileMaxManaModifiers` returns `boolean` where its siblings are `void` because the caller needs
to know: a reconcile that always reported "changed" would pin four times a second, re-stamping the
entry's `asOfTick` so the elapsed count never grows — **mana would stop regenerating entirely**,
silently, with the stat block still reading correctly.

**Mana Bank.** `+10/20/30 Max Mana` on armor, `ARMOR_ONLY`, curved. The burst counterpart to Growth's
survival. `EnchantEffect.MAX_MANA` produced exactly three compile errors, all switch EXPRESSIONS, all
converted in 2a — the "enumerate the axis" lesson confirming rather than assuming.

---

## Verification — what was executed

**Unit.** `./mvnw clean package` → **core 578 / storage 17 / paper 395**, 0 failing.
`GoldenLoreTest` green (no shipped item carries an active Mana Bank, so a move would have meant
something changed that should not have). `check-jar.sh` OK. Baseline at `fc4b9ee` was core 559 /
storage 17 / paper 390.

**Mutation — six planned, and TWO OF THEM DID NOT REDDEN.** Each was confirmed to compile and apply
by grepping its marker and reading the surefire report file, not the console.

| mutation | result |
|---|---|
| `setCurrent` skips the `Math.min` | **GREEN — the check did not exist.** Fixed; see below. |
| resolve the ceiling inside `compute` | **GREEN — the check did not exist.** Fixed; see below. |
| `reconcileMaxManaModifiers` always returns true | RED — `expected: <false> but was: <true>` |
| empty `ManaBankModifierItems.SOURCE_PREFIX` | RED — the disjoint-key guard |
| `EnchantEffectLine`'s arm weakened to a bare "Mana" | RED — `expected: < (+10 Max Mana)> but was: < (+10 Mana)>` |
| `ManaBank.boosts` accepts zero | RED — `expected: <false> but was: <true>` |

**The two green ones are the main finding of the slice.**

1. Deleting `setCurrent`'s clamp left all ten tests green. `current()` calls `regenerated()`, which
   ends in its own `Math.min` against the same ceiling — so the test reported the clamped number
   whether or not the clamp was ever WRITTEN, while its own comment claimed it "reddens where a
   read-only test would not". The stored value is observable only when the ceiling rises again with
   **no pin behind it**; unequip-then-re-equip does *not* expose it, because the re-equip pins a
   value it just read back, already clamped. That was measured too, not assumed.

2. The plan claimed resolving inside `compute` would break `concurrentSpendsCannotOverdrawThePool`.
   All 22 pool tests stayed green. The observable property is the **arity**: two reads straddling a
   gear change make "the guard passed, then the spend refused" reachable, which reaches the player
   as "needs 110, you have 130".

Both gaps are closed by named tests that were watched fail under the mutation and pass with it
restored, and both `ResourcePool` javadocs asserting the old stories are corrected in place.

---

## Boot gate — `./scripts/dev-server.sh --refresh-content` — **OWED, not run**

Orphaned-`java.exe` check first. Tuning edits go in the **deployed** tree with `--no-build`.

| # | Check | Expected |
|---|---|---|
| 1 | boot log | `9 enchants`; zero skipped; zero `ArmorConsistency` mismatches |
| 2 | `/rpg give diamond_chestplate` ×4, read the slots | ≤3 candidates per slot, drawn from four; Mana Bank appears across slots |
| 3 | tooltip of a Mana Bank piece | **`Mana: +30`** under `Defense: N`, in blue |
| 4 | equip at FULL mana, having cast at least once | `✦` max rises, current unchanged — headroom |
| 5 | **equip at full mana having NEVER cast this session** | **also headroom** — the absent-entry case |
| 6 | unequip at full | current **clamps** to the new max |
| 7 | unequip while already below the new max | current **untouched** |
| 8 | cast, then wait | regen refills to the **boosted** max, not 100 |
| 9 | an ability edited to cost more than 100 (deployed tree, `--no-build`) | uncastable bare, **castable** with Mana Bank on |
| 10 | swap a Mana Bank piece for a plain one and back | max follows within a tick, no stranded modifier |
| 11 | `/rpg mana refill` with Mana Bank on | fills to the **boosted** max |

**Rows 5 and 9 are the discriminating ones.** Row 4 passes on the pre-2b behaviour by accident — a
player with an entry already gets headroom. Row 5 is the only row that fails if the pin is missing.
Row 9 is the only row that exercises `tryConsume`'s guard against a per-player max; every other row
reads through `current()`.

---

## Out of scope

Raising `MAX_CANDIDATES`; per-archetype base `MAX_MANA` (this slice makes the max per-player and
leaves the BASE a constant); mana persistence (`storage/` never mentions mana, and a rejoin still
starts full); `/rpg reload`.

**Note for the archetype pass:** `EnchantCost`'s javadoc cites `MAX_MANA` as the archetype of "a
uniform system knob rather than per-enchant content". That is now half-wrong — the base stays
uniform, the resolved value does not.
