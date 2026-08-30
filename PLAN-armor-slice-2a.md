# Armor, Slice 2a — the gating axis, Protection and Growth

Branched off `origin/master` @ `2bb0da8`, verified from the wire.

## Context

Slice 1 (#37) shipped 24 mintable armor pieces that source Defense. The gear extraction (#38) gave
the three gear kinds a shared `GearDefinition`/`GearItems`. What armor still lacked was **enchants of
its own**: it carried the container but had no `GearClass`, so it never rolled, the table refused it,
and `/rpg enchant show` declined.

2a closes the axis and hangs two enchants off it — **Protection** (+3/6/9 Defense) and **Growth**
(+10/20/30 Max Health), both stacking per piece. Unbreaking rides free. These are the first enchants
that are flat stat boosts feeding the reconcile loop; every existing one acts at a damage or block
seam. Mana Bank is 2b.

Five commits, in dependency order, because the first one had to land alone.

---

## 1. The Defense/`nativeArmor` split (`eda8cde`)

`DefenseModifierItems` fed **one map to two jobs** — the Defense stat, and the `nativeArmor` the
armor bar cancels. Sound only while a piece's Defense equalled its material's vanilla points, which
was true for exactly as long as nothing could add Defense. Protection is that thing.

The cost, now pinned as arithmetic in `DefenseTest`: a Protection III piece in every slot contributes
36 Defense that puts **nothing** on the vanilla armor attribute, so defense is 56 while nativeArmor
stays 20. Feed the first where the second belongs and `barModifier` over-subtracts by the enchant's
contribution — the attribute lands at **−28.82**, which Minecraft clamps to 0.

> The bar then reads **EMPTY on the most-armored player in the game**, while the stat, the mitigation
> and the tooltip all stay correct. Nothing throws, and no unit test can see the wiring.

`scan` returns a `Worn` record carrying both numbers from **one walk** — two methods would be two
walks, and a player swapping a piece between them would get a stat and a bar from different
equipment. The defense map keeps **one entry per slot** whose value is that piece's total, because
both alternatives fail silently: `Stat.putModifier` is put-or-**replace** (two sources keyed `CHEST`
means the second wins), and `ModifierReconciler.reconcile` removes every source absent from the map
it is given (reconciling twice has the calls annihilate each other).

`DefenseTest` gained the case the file never had — every prior `barModifier` assertion passed the
**same value twice**. Every number executed, and the identity is **not bit-exact**:
`nativeArmor + barModifier(56, 20)` gives `7.179487179487179` against `armorBarPoints(56)`'s
`7.17948717948718`.

## 2. `percent_by_level` → `value_by_level` (`8a2a7db`)

The field never held a percent — every `/100` lives in the mechanism that asked. Merely imprecise
while damage, block and reflect were the only callers; a plain lie once Protection and Growth grant
flat points. `EnchantCurve.percentAt` → `valueAt` with it, and the two error messages an author
actually reads ("one percent per level").

`DamageEnchants.percentAt` **keeps** its name and signature — a damage curve genuinely is a percent,
and leaving it untouched preserves the property `EnchantCurve`'s javadoc rests on: `DamageEnchantsTest`
passes with zero edits.

A breaking schema change, deliberately without an alias. An old-key file now parses as having no
curve, which `requireCurve` refuses — a named, skipped file rather than an enchant that silently
grants nothing.

## 3. The ARMOR gating axis (`acdbb28`)

`GearClass { MELEE, RANGER, MAGE, SHIELD, ARMOR }`. One constant: every armor enchant is armor-wide,
and `ArmorSlot` keeps owning the axis that needs four values.

### The compiler catches three of eight sites, and four docs claimed otherwise

`RpgCommand`, `ArmorDefinition`, `NEXT.md` and `PLAN-armor-slice-1.md` all said the addition "is a
compile error in `GearClassLabel`'s two exhaustive switches and in `GearClassTest`'s axis
enumeration". True, and badly incomplete. `GearClass.of` switches `WeaponClass`, so it does not
break; `fromName` is a `values()` loop that silently begins accepting `class: armor`.

| | site | how it failed |
|---|---|---|
| compile | `GearClassLabel.of` / `.describe` | needed arms |
| runtime | `GearClassTest` axis count | 4 → 5 |
| **silent** | `RpgCommand.gearClass()` | kept returning `null` |
| **silent** | `RpgCommand.effectSuffix()` | kept returning `""` |
| **silent** | `RpgCommand` SHOW arm | kept refusing armor |
| **silent** | `EnchantMenu.PlacedGear` | a two-way ternary — **would have minted a helmet as a shield** |
| **silent** | `EnchantDefinition` gate | below |

`GearClassTest` now records that count, because it is very nearly the only thing that notices.

### `ANY_BUT_SHIELD` was a latent bug, named from the wrong side

It refused the one kind that existed when written. The instant `ARMOR` appeared, `effect: damage` +
`class: armor` **loaded clean** — and `DamageEnchantItems` reads the main hand through `GearClass.of`,
which yields only MELEE/RANGER/MAGE. Structurally unreachable: exactly the defect the shield refusal
was written to prevent, arriving through the door the check did not name.

Now `MAIN_HAND_ONLY`, stated as what the gate **can** be, so the next gear kind is refused by default.
Mutation-tested.

`requireGate`'s switch became an **expression**. As a statement, adding `ARMOR_ONLY` would have
fallen through to no validation at all — the precise shape of the REFLECT bug the file's own comment
documents.

`EnchantEffect` gained `DEFENSE` and `MAX_HEALTH`; the new effect lines print **points, not percents**
and reuse the item's own words. Armor rolls **and** the table accepts it — shields 2a recorded why
they are inseparable: without table access "a shield would mint with locked candidates no player
could ever buy."

## 4. Protection (`513e300`)

`base + bonus`, per piece, summed by the reconciler. **No clamp**, and `ProtectionTest` proves why
rather than asserting it: Bulwark needs `Shield.clamp` because `0.9 + 0.15` clamps to total immunity,
but `Defense.applyDefense` is asymptotic — walked at +100, +1000, +10000 and +1e9, a hit still lands
every time.

Executed ceiling: full diamond with Protection III in all four slots is 56 points →
**35.8974358974359%** against bare diamond's **16.666666666666666%**. Roughly double, and still not
halving a hit.

**`BlockEnchantItems` became `EnchantValues`** (`percentFor` → `totalFor`). It has been parameterized
by `EnchantEffect` since 2b and never knew anything about blocking; reading DEFENSE through something
called "Block" would have been the same lie `value_by_level` just removed. The alternative was a
second copy of the sum — and this project duplicates structure, never logic.

The tooltip composes through the **same function** the scan does, so a Protection III chestplate reads
`Defense: 17`. `build(armor)` delegates at `Protection.NONE`, an exact identity — which is why the
golden never moved.

## 5. Growth (`5ad64e0`)

Merged into the **single** `reconcileMaxModifiers` call, for the reason §1 gives. And the keys are
**namespaced** — the first in this codebase. `HealthModifierItems` walks all slots on bare slot names,
so a `health_boost_TEMP` and a Growth chestplate would both want `"CHEST"` and one would erase the
other. `"growth:CHEST"` makes them disjoint.

`GrowthTest` pins the consequence in core (same key → 130, namespaced → 430);
`GrowthModifierItemsTest` pins the cause. Both mutation-tested.

The three transition rules are `HealthState`'s and predate the enchant, but Growth is the first thing
that makes them reachable: equipping is headroom (100/100 → 100/130), removing clamps down (130/130 →
100/100), removing while hurt does not (40/130 → 40/100). The third stops the second passing on an
implementation that set `current = max` on removal — which would make taking armor **off** a heal.

Growth and Protection read as siblings and are **not** equally scaled: +36 Defense is bent by a curve,
+120 Max Health is a straight doubling. Recorded in `growth.yml`, which is the lever.

---

## Verification actually run

```
./mvnw clean package     -> BUILD SUCCESS
./scripts/check-jar.sh   -> Jar OK, core and storage bundled
./scripts/check-tests.sh -> 955 tests across all modules
```

| module | `2bb0da8` | now | new |
|---|---|---|---|
| core | 544 | **557** | 13 |
| storage | 17 | 17 | 0 |
| paper | 377 | **382** | 5 |

**`GoldenLoreTest` green at every commit.** Tasks 1–3 were required to move no shipped tooltip, and
Task 4's overload is an exact identity at `Protection.NONE`, so it did not either.

### Mutations run

Marker-grepped, `test-compile` first, restored from scratchpad copies, residue re-grepped as zero.

| mutation | result |
|---|---|
| revert the damage gate to `gearClass == SHIELD` | **RED** — `effect: damage` + `class: armor` loads |
| empty `GrowthModifierItems.SOURCE_PREFIX` | **RED** — the disjoint-key guard |

Plus the compiler-forced ones, which are their own proof: dropping either `EnchantDefinition` switch
arm, or either `GearClassLabel` arm, does not build.

---

## Boot gate — OWED BY A HUMAN

`./scripts/dev-server.sh --refresh-content`. Check for orphaned `java.exe` with
`Get-CimInstance Win32_Process` first. Tuning edits go in the **deployed** tree with `--no-build`;
the source tree trips the golden.

| # | Check | Expected |
|---|---|---|
| 1 | boot log | `8 enchants`; zero `Skipping malformed`; zero `ArmorConsistency` mismatches |
| 2 | `/rpg give diamond_chestplate` ×3 | slots offer **Protection / Growth / Unbreaking** — the first armor roll |
| 3 | any armor roll | **never Bulwark or Thorns** — the gate holding |
| 4 | table: unlock + activate Protection II on a chestplate | tooltip reads **Defense: 14** |
| 5 | equip it | `⛨` rises by 6 |
| 6 | **the armor bar, with Protection active** | **a partial fill, NOT empty** |
| 7 | `/rpg damage 100` in full diamond + Protection III ×4 | visibly less than the ~83 Slice 1 pinned |
| 8 | Growth III on a chestplate, at full HP | max +30, **current unchanged** (headroom) |
| 9 | remove that piece at full HP | current **clamps down** |
| 10 | `/rpg enchant show` on armor | works; no crash; inert lines correct |
| 11 | four Protection III pieces | `⛨` rises by **36**, not 9 |

**Row 6 is the discriminating row.** Rows 4, 5 and 7 all pass whether or not the split landed — the
stat, the tooltip and the mitigation are correct without it. Only the bar breaks, and it breaks to
*empty*, which is unmistakable. Rows 8 and 9 are the pair that pins Growth's transition rules in both
directions; either alone can pass on a wrong implementation.

---

## Out of scope

**2b (Mana Bank), its own plan.** `ResourcePool.max` is a single `final double` shared by every
player, with no `ModifierTarget` and no reconcile path — re-confirmed at `2bb0da8`. It needs max made
per-player-modifiable **plus** the max-decrease clamp decision `HealthState` already made and
`ResourcePool` gets for free only because max never moves (`tryConsume` rejects `amount > max`,
`regenerated` does `Math.min(max, …)`, and `current()` returns `max` for an unseen owner — all three
read the global). 2b also takes the armor pool to **4**, the first shipped pool past
`MAX_CANDIDATES`.

**Not in 2a:** per-slot gating constants; a `/rpg reload`; `ContentValidator` for shields or armor;
the missing crafting hook.
