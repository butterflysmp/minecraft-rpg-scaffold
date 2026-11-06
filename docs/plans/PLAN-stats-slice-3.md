# Stats, Slice 3 — `/rpg stats`

Branch `feat/rpg-stats` off **`0bc4d45`**, verified from the wire.

The read-only command that closes the arc. Seven of the eight lines were one-home reads before this
slice started, because Slices 1 and 2 built them that way. The eighth — **Damage** — was the real
work, and it was a **de-duplication**, not a read.

---

## The centrepiece: the composition had TWO copies and its explanation had THREE

`EffectApplier` wrote `(base × multiplier(pct) + classBonus) × chargeScale × critMultiplier` out
twice, token-for-token identical but for the base. A sheet that re-derived it would have been the
**third copy**, and the sheet's whole claim is that its number *is* a real swing.

Worse, the 14.95 ordering witness — `8*1.15 + 5 = 14.2`, not `(8+5)*1.15 = 14.95` — was written in
`EffectApplier`'s arm comment, `Caster`'s javadoc **and** `AttackCharge`'s: four descriptions of a
formula nobody could point at.

**And one of the three was already false.** `AttackCharge` said the charge scale "is the LAST
transform". The crit multiplier is applied after it. Written before crit existed, never revisited,
harmless only because both are bare multiplies outside the parenthesis. Corrected.

---

## Decisions taken

1. **Two methods, not one.** `HitDamage.hitBase(base, pct, classBonus)` holds the ordering hazard;
   `HitDamage.dealt(hitBase, chargeScale, crit)` is the product tail, where order cannot change the
   answer. Both arms call `dealt(hitBase(...), …)`; the sheet calls `hitBase(...)` alone.
   **Executed: `dealt(hitBase, 1.0, 1.0) == hitBase` is exactly true**, so the sheet shows a
   full-charge non-crit swing rather than a near-miss of one — and passes no neutral `1.0` ceremony.

2. **Build values only.** Capacities and rates. A chat message is a frozen snapshot; a printed
   `47/100` is stale as it scrolls and would sit disagreeing with the action bar, which shows live
   current/max twice a second and is the right home for it.

3. **Self-only, and that is threading, not scope.** `Stat.modifiers` is a plain `LinkedHashMap`
   (`Stat.java:29`) and `value()` iterates it; only the outer `states` map is concurrent. Self runs on
   the caller's own region thread — the same thread their reconcile loop runs on. A `<player>` target
   would iterate maps the *target's* loop mutates on *their* region thread four times a second.
   Deferred with that reason, in `NEXT.md`.

4. **`rpg.command.stats`, `default: true`, declared in `paper-plugin.yml`.** The one player-facing
   command in the arc. An undeclared node silently defaults to op-only — hence gate row 3, run as a
   **non-op** player.

5. **Icons promoted to public; `DAMAGE_COLOR` / `CRIT_COLOR` added** — in `StatsBarText`, for the
   reason its colours are already public: importing makes it a compile-time link rather than two
   strings that happen to match. Regen lines wear their **parent's** colour, which is what makes the
   pairing read without a separator.

---

## What was built — three commits

| # | commit | what |
|---|---|---|
| 1 | `13f2694` | `HitDamage`, both arms migrated, three prose copies consolidated, `AttackCharge` corrected |
| 2 | `dbd35a2` | `StatsSheetLines` — the text half and its three unit conventions |
| 3 | `6fb70da` | `StatsSheet`, `/rpg stats`, the permission node, the HUD constants |

---

## Verification — what was executed

**Unit.** `./mvnw clean package` → **core 625 / storage 17 / paper 409**, 0 failing.
Baseline at `0bc4d45`: core 613 / storage 17 / paper 403.
`./scripts/check-jar.sh` OK. `./scripts/check-tests.sh` → 1051 across three modules.

**Faithfulness.** `EffectApplierTest` byte-identical to `0bc4d45` (`git diff --quiet`) — its four
factor pins (85.8 ordering, 92.70/85.80 charge placement, 92 class addend, 8.0 crit) are what prove
the extraction changed nothing in the shipped combat path.

**`GoldenLoreTest` green, and `golden-lore.txt` byte-identical.** Verified, not assumed:
`git diff 0bc4d45 -- core/.../weapon/ paper/.../weapon/` is **empty**, so no `*LoreLines` formatter
was edited. Adding colour constants and promoting icons are invisible to the golden, as predicted.

**Three unit conventions, and the formatter that had to be written because neither existing one
worked.** `Math.round` is the HUD's (glanceable); `GearLoreLines.trimNumber` prints the base mana rate
as **`1.6666666666666665`** — executed, and asserted in the test so nobody re-adopts it. Capacities
take `trimNumber`, rates and damage take two decimals. Consequence stated rather than discovered: the
sheet and the bar can differ in the last digit (`137.5` vs `138`); they answer different questions.

**Every expected string was produced by EXECUTING the expression.**

**Mutation — 14 planned, 13 RED and 1 GREEN, reported as measured.**

| mutation | result |
|---|---|
| `(base + classBonus) × multiplier` — the 14.95 inversion | RED `expected: <85.8> but was: <85.05>` |
| drop the `+ classBonus` addend | RED `expected: <79.0> but was: <84.0>` |
| drop `chargeScale` from `dealt` | RED `expected: <96.45> but was: <85.8>` |
| drop `critMultiplier` from `dealt` | RED `expected: <16.0> but was: <8.0>` |
| rates via `trimNumber` | RED `expected: <0.20/s> but was: <0.2/s>` |
| drop the `/s` unit | RED `expected: <0.20/s> but was: <0.20>` |
| crit chance as a raw probability | RED `expected: <15%> but was: <0.15>` |
| crit damage as the bonus, not `1 + v` | RED `expected: <2.00x> but was: <1.00x>` |
| capacities at two decimals | RED `expected: <100> but was: <100.00>` |
| labels unpadded | RED `expected: <Max Health   > but was: <Max Health>` |
| `MAX_MANA_LABEL` reused for health | RED `expected: <8> but was: <7>` |
| health regen wears `MANA_COLOR` | RED `expected: <red> but was: <blue>` |
| label and value colours swapped | RED `expected: <gray> but was: <red>` |
| hardcode `NamedTextColor.RED` for the HUD constant | **GREEN** |

**The four `HitDamage` rows all reddened `EffectApplierTest` — the untouched suite — as well as the
new one.** That is the useful part: the extraction is load-bearing for the shipped combat path, not
just for its own test.

**The green row is honest and stays in the table.** `NamedTextColor.RED` *is*
`StatsBarText.HEALTH_COLOR` — the same singleton — so no unit test can distinguish a hardcoded colour
from an imported one. Asserting against the constant catches **divergence** the day a HUD colour
changes; it cannot catch the hardcoding. The compile-time import is the guard, and `ArmorLoreTest`
faces the identical limit for the identical reason.

**WHAT NO TEST CAN CATCH, stated because it is the slice's one real residual risk.** `HitDamage`
guarantees every caller shares the **formula**. It cannot guarantee they share the **inputs**. The
combat path reads its three summands off a snapshot frozen at cast time; the sheet reads them live.
They agree because `BukkitCombatant.snapshot` is a straight read of `attackValue` /
`classDamageValue` / `enchantDamagePercentValue` and `Caster.of` copies all three through unchanged —
**verified at the source during this slice**. If a transform is ever added there, the sheet drifts and
both callers remain correct in isolation, so nothing reddens. `snapshot` now carries a javadoc saying
so. **Gate row 5 is the standing check.**

---

## Boot gate — `./scripts/dev-server.sh` — **RUN AND PASSED, 2026-08-31**

**All eight rows, operator-confirmed, including both discriminating ones — and rows 2 and 6 re-run
after the `/5s` display change and the mana rebalance.**

Kill orphaned `java.exe` first.

| # | Check | Expected |
|---|---|---|
| 1 | boot log | clean load, zero skipped content |
| 2 | `/rpg stats` bare-handed | nine lines; Damage `0.00`, Crit `15%` / `2.00x`, regens **`1.00/5s`** and **`5.00/5s`** |
| 3 | **as a NON-OP player** | it runs — proves the yml node landed and did not silently become op-only |
| 4 | hold an authored weapon, `/rpg stats` | Damage is the composed hit, not the raw attack value |
| 5 | **swing that weapon at full charge, no crit; compare to row 4** | the numbers **match** |
| 6 | equip Growth / Mana Bank / a regen fixture | the matching lines move; remove → they return |
| 7 | glance between the sheet and the action bar | health/mana/defense the same colour in both |
| 8 | read the value column | chat is a **proportional** font — confirm the ragged edge is acceptable, or say so. **Re-check after the rebalance**: the regen values gained a digit (`5.00/5s`, `10.00/5s` with the fixture), so the column may sit differently |

**Rows 3 and 5 are the discriminating ones.** Row 5 is the only check in existence for the input seam
above — it fails if the sheet and the swing ever stop sharing inputs, which no unit test can see. Row
3 is the only row that fails if the permission node is used but undeclared.

### Re-gate after the tuning pass — **RUN AND PASSED**

Two tuning changes landed after the gate table was written: regen is displayed **per five seconds**,
and the mana base was **rebalanced** to a 100-second refill (1 mana/s).

| row | status |
|---|---|
| 2, 6 | **RE-RUN AND PASSED** — bare-handed reads `1.00/5s` and `5.00/5s`; the fixture moves the line |
| 8 | **glanced** — the column holds with the wider numbers |
| 3, 4, 5, 7 | **not re-run, and correctly so** — the extraction, the permission node and the colours were untouched by the tuning, which the byte-identical `EffectApplierTest` and `golden-lore.txt` confirm mechanically |

The last row is the point of scoping a re-gate rather than re-running everything: the tuning changed
two numbers and no behaviour, and there is a mechanical check saying so.

---

## Out of scope

An optional `<player>` argument, deferred with the threading reason above and recorded in `NEXT.md`
so the omission does not read as an oversight. No action-bar changes. No new stats beyond the eight.
Carried, still recorded and not fixed: Slice 1's potion overheal at high max HP, and Slice 2's
base-regen-vs-ceiling coupling.
