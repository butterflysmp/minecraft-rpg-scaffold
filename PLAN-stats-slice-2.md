# Stats, Slice 2 — Mana Regen as a per-player stat

Branch `feat/mana-regen` off **`8362262`**, verified from the wire.

Slice 2b lifted `ResourcePool`'s **ceiling** behind a `MaxResolver`. The **rate** never moved:
`regenPerTick` was still a bare `double` with exactly one read in the tree and no accessor at all, so
this lift had zero read-side callers to migrate. This slice is the second half of that pair, plus the
eleventh `Stat`.

---

## Two corrections to the brief, both load-bearing

### The rename it asked for would have re-rated every player

The brief made per-second canonical, with `perTick()` dividing by 20 and `MANA_PER_TICK` renamed to
`MANA_PER_SECOND`. Measured before writing any code:

| expression | value |
|---|---|
| `MAX_MANA / (60 * 20)` (shipped) | `0x1.5555555555555p-4` |
| `(MAX_MANA / 60.0) / 20.0` | `0x1.5555555555556p-4` |
| `==` | **false** |

One ULP. The rename would have shifted the regeneration rate for every player on the server —
including players wearing no mana gear — silently, and by an amount no boot gate could observe.

**Per-tick stays canonical and textually unchanged; per-second is derived from it.** The resolver
composes in ticks: `MANA_PER_TICK + ManaRegen.perTick(bonus)`. With no bonus that is `x + 0.0`, which
is exactly `x`, so an unenchanted player is bit-for-bit unaffected by the whole slice.

And the obvious round-trip test would have been a **false law**: `(x*20)/20` round-trips for every
value tried, `(x/20)*20` does not — it fails for `1.6666666666666667`, exactly the per-second figure
written by hand for this pool. The derived base is `…665`.

### The `void` vs `boolean` rule was on the wrong axis

Slice 1's `healthRegenTarget` javadoc said a rate can return `void` because it has "no current
anywhere". That is a proxy. `manaRegenBonus` is also a rate with no current and **does** need a
boolean and a pin. The axis is **eager vs lazy-integrated**: health regen pays `rate × dt` each second
and has nothing accrued to re-price; mana computes `amount + elapsed × rate` on read and therefore
re-prices the past. Corrected at both sources.

---

## Decisions taken

1. **Generalize the pin, don't add a second.** One `before` read, both reconciles **into locals**, one
   `setCurrent` if either moved. Mana has one current; the ceiling and the slope both govern it.
2. **The pin lives in core (`ManaTransition`), not inline.** Every argument was already a core type.
   The decisive reason is the `||` short-circuit trap — see Verification.
3. **`manaRegenBonus` is a BONUS at base 0.0, per second**, mirroring `maxManaBonus` including why:
   the base lives in `RpgPlugin` beside the `MAX_MANA` it derives from, which `NEXT.md` records as
   becoming archetype content.
4. **`ResourcePool.regen(owner, resourceId)`** mirrors `max(...)`, so a display composes base + bonus
   once. `/rpg manaregen` already reads through it.
5. **`current` resolves the rate lazily; `tryConsume` cannot.** The absent branch returns the ceiling
   and never touches the rate — and that is the hot read. `tryConsume` cannot know the entry is absent
   until inside `compute`, and peeking would break atomicity.

---

## What was built — six commits

| # | commit | what |
|---|---|---|
| 1 | `4a46462` | `RegenResolver` + the `ResourcePool` lift (three constructors, injected rate, `regen()`) |
| 2 | `59e16ec` | `ManaRegen` — the bonus surface and the one unit conversion |
| 3 | `595b5a9` | the eleventh `Stat`, and the two corrected javadocs |
| 4 | `3eefa5e` | `ManaTransition` — both reconciles, one stamp |
| 5 | `cec8b96` | paper wiring; `MANA_PER_TICK` deliberately NOT renamed |
| 6 | *(this)* | the `mana_regen_boost_TEMP` fixture, `/rpg manaregen`, and the docs |

---

## Verification — what was executed

**Unit.** `./mvnw clean package` → **core 613 / storage 17 / paper 403**, 0 failing.
Baseline at `8362262`: core 592 / storage 17 / paper 401.
`./scripts/check-jar.sh` OK. `./scripts/check-tests.sh` per-module reports present.

**Faithfulness, checked rather than asserted.** `ResourcePoolTest` and `HealthStateTest` confirmed
byte-identical via `git diff --quiet`. All **nine** pre-existing `new ResourcePool(...)` sites
untouched — `git diff 8362262 | grep '^[-+].*new ResourcePool'` shows added lines only, no removals.
`MANA_PER_TICK`'s assignment likewise unchanged, so the shipped double is bit-identical by
construction.

**Mutation — 21 planned, 21 RED**, each watched before its row was written; markers grepped,
`test-compile` first, sources restored from the scratchpad, never `git checkout --`.

| mutation | result |
|---|---|
| resolve the RATE inside `compute` | RED `expected: <1> but was: <2>` |
| hoist the rate resolve above `current`'s ternary | RED `expected: <0> but was: <1>` |
| `regenerated` ignores the injected rate | RED `expected: <20.0> but was: <10.0>` |
| `regen()` returns a constant | RED `expected: <2.0> but was: <1.0>` |
| drop `requireNonNegative` | RED `Expected IllegalArgumentException … nothing was thrown` |
| `TICKS_PER_SECOND` → 10.0 | RED `expected: <0.05> but was: <0.1>` |
| `ManaRegen.boosts` uses `>=` | RED `expected: <false> but was: <true>` |
| `ManaRegen.contribution` halves | RED `expected: <1.0> but was: <0.5>` |
| `perTick` offset by `1e-18` | RED `expected: <0.0> but was: <1.0E-18>` |
| base the regen `Stat` at 1.0 | RED `expected: <0.0> but was: <1.0>` |
| `manaRegenBonusValue` uses `require()` | RED `IllegalState no health state tracked for …` |
| reconcile always returns true | RED `expected: <false> but was: <true>` |
| reconcile always returns false | RED `expected: <true> but was: <false>` |
| **the `\|\|` short-circuit form** | RED `expected: <2.0> but was: <0.0>` |
| pin unconditionally | RED `expected: <false> but was: <true>` |
| pin only on `maxChanged` | RED `expected: <true> but was: <false>` |
| pin only on `regenChanged` | RED `expected: <true> but was: <false>` |
| read AFTER the reconciles | RED `expected: <20.0> but was: <60.0>` |
| `SOURCE_PREFIX` → `""` | RED `expected: <false> but was: <true>` |
| `SOURCE_PREFIX` → `"manabank:"` | RED `expected: not equal but was: <manabank:HAND>` |
| `DEFAULT_BOOST` → 0.05 | RED `expected: <2.6666666666666665> but was: <1.7166666666666666>` |

**Three of these are silent in production and no gate row would catch them**: the short circuit stops
a stat converging, the unconditional pin freezes regeneration entirely, and the missing rate pin
grants free mana on equip. That is what the `ManaTransition` extraction bought.

**Not covered offline:** the two resolver lambdas, the reconcile loop, the scan body and the fixture
mint all need a live server (`new ItemStack(...)` throws without a `RegistryAccess`; no MockBukkit).

---

## Boot gate — `./scripts/dev-server.sh` — **OWED, not run**

Kill orphaned `java.exe` first — the script dies, the JVMs do not, and they hold the deployed jar.

| # | Check | Expected |
|---|---|---|
| 1 | boot log | clean load, zero skipped content |
| 2 | `/rpg mana refill`, cast, watch the bar | mana regenerates at all — the freeze guard in the wild |
| 3 | `/rpg manaregen`, hold it, cast, watch | visibly faster (~37 s a bar, not 60); drop it → back to base within a tick |
| 4 | **THE PIN.** Cast to empty, idle ~12 s **without touching gear**, note the bar, THEN equip the fixture | mana **does not jump**; it continues faster *from where it was* |
| 5 | same, then **unequip** | no sudden drop |
| 6 | equip Mana Bank at partial mana | still headroom, not a top-up — 2b regression |
| 7 | `/rpg mana refill` | message still names the right max |

**Rows 4 and 5 are the discriminating ones** — the only rows that fail without the pin, and row 4
fails visibly (a ~20-mana jump) on the parent commit. Row 3 is the only row that fails if the reconcile
surface is unwired. Row 2 is the only row that fails if the pin fires unconditionally.

---

## Out of scope

Slice 3 (`/rpg stats`) — handed `ResourcePool.regen(...)` and `ManaRegen.perSecond(...)`, so it needs
no conversion of its own. A real mana-regen enchant (this ships a `_TEMP` fixture, now nine). Mana
persistence. Showing the rate on the action bar.

**Recorded, not fixed:** the base rate does not scale with a raised ceiling. `MANA_PER_TICK` means "a
full bar in 60 seconds", but with Mana Bank at +120 the ceiling is 220 while the rate stays base — so
an enchanted player takes **132 s** to fill, their bar bigger and their refill proportionally slower.
Shipped behaviour, predating this slice. It wants deciding alongside per-archetype `MAX_MANA`, not
before it. In `NEXT.md`.
