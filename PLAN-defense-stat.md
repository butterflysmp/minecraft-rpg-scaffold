# Defense — a working stat, sourced from vanilla armor, with a DR armor bar

Branched off `origin/master` @ `93f41d2`.

Five stats reconciled and `applyDamage` reduced nothing. This pass makes defense the sixth stat,
sources it from the vanilla armor a player already wears, mitigates damage through it, commandeers
the vanilla armor bar to read damage reduction instead of material, and fills in the `⛨` action-bar
field pass 2 reserved.

Custom gear (rarity/stats/enchants minted onto armor) is the long run and explicitly **not** this
pass. `DefenseModifierItems.armorOf` is the one method that will have to learn about it.

## The curve — `core/.../combat/Defense.java`

Diminishing returns, not flat subtraction:

```
applyDefense(damage, defense) = damage * 100/(100 + defense)
damageReduction(defense)      = defense / (100 + defense)
```

One curve read two ways, and `applyDefense(d, x) == d * (1 - damageReduction(x))` is asserted so the
number a player reads cannot drift from the damage they take. Flat subtraction was rejected: it makes
immunity reachable, and a small hit against big defense goes negative and heals.

Also here, for the same reason `HeartScale` is (a vanilla fact stated as arithmetic, no Bukkit
needed): `armorBarPoints(defense) = damageReduction × 20` and
`barModifier(defense, nativeArmor) = armorBarPoints − nativeArmor`.

Full diamond is 20 armor → **~16.7% reduction, ~1/6 of the bar**. Deliberately low: diamond is starter
gear, and a starter set that halved damage leaves real gear nothing to grant.

## The source — vanilla armor per slot

`paper/.../health/DefenseModifierItems.java` reads each of the four armor slots via
`ItemType.getDefaultAttributeModifiers(slot)` and sums the `ADD_NUMBER` modifiers on `Attribute.ARMOR`.
Vanilla's own numbers; no `Material -> armor` table, which would be the banned in-Java-content pattern
and would miss every armor item a future Minecraft drop adds.

- The attribute key is **`armor`**, not `generic.armor` — names flattened in modern versions.
- The four armor slots are named explicitly; `EquipmentSlot.values()` also yields `HAND`, `OFF_HAND`,
  `BODY` and `SADDLE`.
- Keyed by `EquipmentSlot.name()`, so removal is by **absence** through the same leak-proof
  `ModifierReconciler` diff the other five stats use. No departure event to miss.

## The mitigation — `CombatantStats.damage`

**Not `EffectApplier`.** There are five call sites reaching custom HP, and two of them skip
`EffectApplier` entirely — mob melee (`RpgListeners.onMobMeleeAttack`) and `/rpg damage`
(`RpgCommand.damageSelf`). A reduction placed in `EffectApplier` would leave a mob hitting an armored
player completely unmitigated, which is the central thing this pass exists to do.

All five converge on `CombatantStats.damage`, which is in core, pure, and the last statement before
`HealthState.damage` moves HP:

```java
double dealt = Defense.applyDefense(amount, state.defenseValue());
boolean reachedZero = state.damage(dealt);
listener.onChange(new HealthChange(..., dealt, ...));
```

Order is `base × enchantPercent + classBonus` (caster side) **then** `× 100/(100+defense)` (target
side, last). Defense resists the whole blow, not the pre-bonus base.

`HealthChange` carries the **post-mitigation** amount: the seam is the source of truth for how much HP
actually moved, so the popup, the nameplate and kill credit all report a change that really happened.

`CombatantSnapshot` is untouched — defense is read from the store at the moment of mitigation, not
frozen at cast time.

## The DR armor bar

`paper/.../health/ArmorBarOverride.java` adds an `armor` attribute modifier of
`barModifier(defense, nativeArmor)` so `native + modifier = armorBarPoints(defense)`. Same idea as
`WeaponItems.VANILLA_MELEE_SUPPRESSION`; same entity-side add/remove-by-key mechanics as
`EntitySpeedAttribute`.

- `nativeArmor` is the sum from the equipped **pieces**, never `attribute.getValue()` — the attribute
  is what this writes to, so reading it back would have the value chase itself down every scan.
- **Idempotent**: an unchanged value writes nothing. This runs 4×/second per player; re-adding an
  identical modifier would spam attribute-sync packets, and a duplicate key is rejected outright.
- At zero defense the modifier is **removed**, not set to zero. Cleared on quit too, since API-added
  modifiers persist in player data.

## The `⛨` action-bar field

`⛨ N` between health and mana, in **lime** — `NamedTextColor.GREEN` is Minecraft's lime (`#55FF55`);
Adventure's `DARK_GREEN` is Minecraft's darker green.

The field renders **only when `Math.round(defense) >= 1`**, so the bar can never print `⛨ 0`. Children
are therefore 3 unarmored and 5 armored, with mana at index 2 or 4.

> **The reddening pass 2 predicted did not fire, and that must not be read as a passing check.**
> `PLAN-action-bar-hud.md:45-50` predicted the child-index assertions would redden when this field was
> inserted. That assumed an *unconditional* field. Because the field is conditional, every
> pre-existing assertion — all of which pass defense 0 — kept its indices and stayed green; the only
> forced signal was the compile break from the new parameter. The armored layout is covered by seven
> tests written specifically for it. Their greenness is the evidence; the old tests' greenness is not.

## Verification actually run

```
./mvnw clean package     -> BUILD SUCCESS: core 387, storage 17, paper 270, 0 failures
./scripts/check-jar.sh   -> Jar OK, core and storage bundled
```

Counts before this pass were core 371 / paper 263, so 16 core and 7 paper tests are new.

**Mutation-tested, each confirmed to compile and apply (marker grepped) before the result was
believed, and each restored from a scratchpad copy rather than `git checkout --`:**

| mutation | result |
|---|---|
| drop the `defense <= 0` guard in `applyDefense` | RED — `applyDefense(10, -50)` gave **20.0**, double damage |
| change `SCALE` in `damageReduction` only | RED — 6 tests, including the `theTwoCurvesAreOneCurve` identity |
| remove the mitigation from `CombatantStats.damage` | RED — current 75→70, event amount 25→30 |
| gate the `⛨` field on the raw value not the rounded one | RED — 0.49 rendered a `⛨ 0` field |
| render the `⛨` field unconditionally | RED — 8 tests, incl. the unarmored-layout pins |

Floating point was **executed, never predicted**: the `applyDefense == d*(1-DR)` identity is *not*
bit-exact (it differs by ~3.6e-15 at defense 50), so it is asserted with a delta. A test written from
the algebra would have demanded `==` and failed on correct code.

## Boot gate — OWED, not yet run

Every row needs a `Player`; a console log proves only that the plugin loaded.

```bash
./scripts/dev-server.sh --refresh-content
```

| # | check | expected |
|---|---|---|
| 1 | Equip a diamond set piece by piece | `⛨` field **appears** on the first piece, number climbs 3 → 11 → 17 → 20, bar fills proportionally |
| 2 | `/rpg damage 100` bare | 100 custom HP lost |
| 3 | `/rpg damage 100` in full diamond | ~83 lost, matching `applyDefense(100, 20)` — not 100, not 50 |
| 4 | Read the armor bar in full diamond | ~1/6 filled (~1.7 of 10 icons), **not** half. This is also the row that falsifies the assumption that a player's base `armor` attribute is 0 |
| 5 | Strip all armor | `⛨` field **disappears**, bar empties, no stranded modifier |
| 6 | Let a mob melee you, armored vs bare | armored hit visibly smaller — the path that skips `EffectApplier`, so this row is what proves the seam choice |
| 7 | Break/drop one piece mid-combat, then rejoin | number and bar drop by that piece; one bar after rejoin, no stale modifier |

Rows 3, 4 and 6 can only pass if the design is right. The rest can pass by accident.
