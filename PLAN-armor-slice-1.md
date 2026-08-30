# Armor, Slice 1 — mintable armor pieces that source Defense

Branched off `origin/master` @ `59400c0`, verified from the wire (`git ls-remote --heads origin`
→ `59400c095fd7…` for `refs/heads/master`, equal to local `HEAD`, and the only remote branch).

## Context

Defense has been a finished, gated mechanic since `869a67c`: `DefenseModifierItems` reads the four
armor slots, `CombatantStats.reconcileDefenseModifiers` converges the stat, `Defense.applyDefense`
mitigates inside `CombatantStats.damage`, `ArmorBarOverride` drives the vanilla bar to read damage
reduction, and `StatsBarText` renders `⛨ 20`. What was missing was the *gear*: armor pieces carrying
rarity, lore, a stat block and an enchant container, the way weapons and shields do.

This slice mints them — 24 pieces, six material tiers × four slots. It is the third gear consumer,
and therefore the trigger the repo has recorded three times for extracting a shared
`GearDefinition`/`GearItems`. That extraction is the **immediate follow-up PR**, not this one.

## The headline: the Defense source did not change, and could not have

`DefenseModifierItems.armorOf` sums `ItemType.getDefaultAttributeModifiers(slot)` on
`Attribute.ARMOR` — **vanilla's own numbers for the material**, a read that is blind to anything on
the ItemStack. The requirement here is *Defense = the piece's vanilla armor points*. Those are the
same number, so:

> A minted diamond helmet and a plain one contribute the identical 3, through the identical code
> path. `DefenseModifierItems`, `ArmorBarOverride`, `PlayerHealthSystem`, `CombatantStats` and
> `Defense` are all **byte-identical to `59400c0`**.

`DefenseModifierItems`' own javadoc predicted this slice — *"when it lands, this is the one method
that has to learn about it"* — and the answer is that it does not have to yet. It learns in Slice 2,
when Protection makes a piece's Defense diverge from its material's.

## The coupling this leaves standing, and the check that guards it

`PlayerHealthSystem.java:181-184` uses **one map for two different jobs**:

```java
Map<String, Double> desiredDefense = DefenseModifierItems.desiredModifiers(player);
stats.reconcileDefenseModifiers(id, desiredDefense);          // the Defense STAT
ArmorBarOverride.apply(player, keys, stats.defenseValue(id),
        DefenseModifierItems.total(desiredDefense));          // the nativeArmor the bar CANCELS
```

Sound only while a piece's Defense equals the armor its material natively grants. `Defense.barModifier`'s
javadoc guards the *other* half of this (never read the live attribute — which this code already does
correctly); the unguarded half is divergence between the two numbers.

Slice 1 keeps them equal by construction, but the `defense:` value is now **authored in content**,
so a YAML edit can break the equality with nothing failing:

> The tooltip reads `Defense: 9` and looks right. The action bar reads 8 and looks right. The bar
> fills to the DR of 8 and looks right. The damage taken matches 8 and looks right.

Hence **`ArmorConsistency`** (`paper/.../content/ArmorConsistency.java`), run from `onEnable`: it
compares every authored value against vanilla's and warns per mismatch. It is the only moment the
two numbers are ever in the same JVM — `core` cannot reach an `ItemType` registry and `paper` has no
live server in the unit loop, which is why this is a boot check and not a test.

It also **warns when handed zero pieces**. That branch matters more than it looks: if `content/armor`
loads empty, every other signal still reads healthy (Defense keeps working — it is sourced from
vanilla, not from a tag), and a silent "0 mismatches" would be the strongest-looking evidence that
nothing is wrong.

**`defense:` is DISPLAY-ONLY in this slice**, and every content file says so at length. Editing it
changes the label, not the mitigation, and trips `ArmorConsistency`. It is a mirror of vanilla's
value, never a lever on it.

## What shipped

### Core (written first, with their tests)

| File | Notes |
|---|---|
| `weapon/ArmorSlot.java` | `HEAD/CHEST/LEGS/FEET`. **The constant names are a wire format**: `DefenseModifierItems` keys its map by `EquipmentSlot.name()` and the reconciler matches sources by that string, so `ArmorSlot.HEAD.name()` must equal `EquipmentSlot.HEAD.name()`. `ArmorConsistency` does `EquipmentSlot.valueOf(slot.name())`. Renaming `HEAD` to `HELMET` would read better and compile everywhere. |
| `weapon/ArmorDefinition.java` | Record. Its own type for the reason `ShieldDefinition` records: `WeaponDefinition` rejects an empty trigger list and requires a `WeaponClass`. `defense` refused-not-clamped via a **negated range** so `NaN` is caught. No upper bound — vanilla owns that number, and `ArmorConsistency` catches a wrong *small* value too, which no range check can. |
| `weapon/ArmorRegistry.java` | The shape of `ShieldRegistry`. |
| `weapon/ArmorLoreLines.java` | `defenseLabel`, and `slotNoun` as an exhaustive switch with no default arm. **No multiply-by-100**, unlike `ShieldLoreLines`: armor points are the stat's own unit, so the FP hazard that forced a rounding pass there never arises. |

### Paper

| File | Notes |
|---|---|
| `weapon/ArmorItems.java` | mint / remint / `armorId` / `heldArmorId`. Sets `HIDE_ATTRIBUTES`. `materialOf` falls back **per slot** to the leather item — a single fallback would mint a chestplate for a `head` definition, which then reconciles into the wrong map key and shows the wrong footer noun, consistently enough to look deliberate. |
| `weapon/ArmorLore.java` | Defense line (gray label, `GREEN` number — the same `StatsBarText.DEFENSE_COLOR` the `⛨` field uses, because they report the same stat), flavour, blank, `"<Rarity> <SlotNoun>"` footer last. No enchant-composed overload, unlike `ShieldLore`: nothing in this slice changes a piece's Defense. |
| `content/ArmorLoader.java` | **One file, four definitions** — the one place armor diverges from every other loader. |
| `content/ArmorConsistency.java` | Above. |
| `resources/content/armor/*.yml` | Six tiers. `saveDefaultContent()` walks the jar with `JarFile`, so the new directory ships with no Java list to update. |

**Modified, all additive:** `Keys.java` (one field), `RpgPlugin.java` (load, count, zero-check,
three-way collision check, consistency run, one call site), `RpgCommand.java` (`give` resolves a
third registry; `HeldGear` gains a third arm).

### `HIDE_ATTRIBUTES`, and the one edit that would silently break the bar

The flag is **display only**. The piece keeps granting its vanilla armor, which is *required* —
`ArmorBarOverride` cancels the native sum and refills from DR, and that sum is `barModifier`'s input.

**Never `setAttributeModifiers` to strip the armor instead.** `armorOf` reads the MATERIAL's
defaults, not the stack's, so stripping them leaves it reporting 20 for a full diamond set while the
live attribute is 0. `barModifier` would be off by the whole set, the bar visibly wrong, the Defense
stat still right, and nothing anywhere would fail. Recorded in `ArmorItems`' javadoc.

`ShieldItems` argues *against* the flag ("nothing to hide"); armor is the opposite case, per
`WeaponItems.java:132-137` — *"the custom lore block IS the stat display."* Armor has two lines to
hide, and the second, Armor Toughness, advertises a stat this project does not implement.

### Content: six tier files, ids derived from the material token

Rarity is per-tier: **Common** for leather/chainmail/iron/golden, **Uncommon** for diamond/netherite.
Netherite is deliberately *not* higher than diamond — vanilla gives them the same armor points, and
what netherite actually adds (toughness, knockback resistance) this project does not model.

Ids come from each piece's `material` token (`diamond_helmet`), **not** the filename. That is the
divergence, and it is what keeps leather correct: vanilla's leather pieces are **Cap, Tunic, Pants,
Boots** against materials named `leather_helmet`/`_chestplate`/`_leggings`, so a name-derived id
would have produced `leather_cap` for an item whose material is `leather_helmet`. `display_name` is
authored per piece for the same reason — deriving it would have renamed three vanilla items, which
the brief forbids. Their footers still read "Common Helmet"; the footer says what *kind* of gear the
item is, the job "Rare Melee Weapon" does on a weapon.

`parseTier` walks the **slot axis**, not the file's keys: a missing slot is a named refusal and a
typo'd key (`foot:`) is simply never read. A bad tier is refused **whole** — a partially-loaded tier
is the worst outcome, since a player finds three quarters of a set with nothing in the log. The
warning names the file *and says it took four pieces with it*.

### Enchants: compatible, not rolled

Armor carries the container, `/rpg enchant` can write it, and remint moves the raw blob — but
`EnchantRollItems.rollOnAcquire` is **not** called. `EnchantRoll` is keyed on `GearClass`, which has
no `ARMOR` constant; adding one is a compile error in `GearClassLabel`'s two exhaustive switches and
in `GearClassTest`'s axis enumeration, by design, because it forces the decision about whether armor
is one class or four. That belongs with the enchants that need it (Slice 2).

This is exactly the line shields drew in their own Slice 1. `HeldGear.gearClass()` returns **null**
for armor and every caller gates on `isArmor()`; `/rpg enchant show` refuses armor rather than
passing null into `EnchantEffectLine`'s no-default-arm switch. **Do not "fix" that by handing it
`SHIELD`** — it would make a helmet eligible for Bulwark and Thorns.

**`EnchantMenu` was deliberately left untouched** (a change from the plan, which proposed a third
`PlacedGear` arm). `acceptsInput` already refuses armor at the door with an accurate message, and
both `resolveGear` callers null-guard. The table is the roll/unlock UI; armor is not rolled in this
slice, so an armor arm would be an arm that immediately refuses — and it would make `gearClass()`
nullable in a second place.

---

## Verification actually run

```
./mvnw clean package    -> BUILD SUCCESS
./scripts/check-jar.sh  -> Jar OK, core and storage bundled
./scripts/check-tests.sh-> per-module reports present; 937 tests across all modules
```

| module | before (`59400c0`) | after | new |
|---|---|---|---|
| core | 519 | **544** | 25 |
| storage | 17 | 17 | 0 |
| paper | 349 | **376** | 27 |

Zero failures. Every new test class confirmed to have produced a surefire report file — `BUILD
SUCCESS` with no `Tests run:` line means zero tests ran.

### The additive promise, verified by diff rather than argument

`git diff 59400c0 -- <file>` is **empty** for all twelve:

`WeaponItems` · `ShieldItems` · `WeaponLore` · `ShieldLore` · `WeaponDefinition` ·
`ShieldDefinition` · `Defense` · `DefenseModifierItems` · `ArmorBarOverride` · `PlayerHealthSystem` ·
`CombatantStats` · `EnchantMenu`

So a weapon/shield/Defense regression from this slice is structurally impossible, and the armor
feature's failure surface is isolated.

### Mutations run

Each confirmed to **compile and apply** (marker grepped), and each restored from a **scratchpad
copy**, never `git checkout --`. Residue re-grepped as zero afterwards.

| mutation | result |
|---|---|
| `!(defense >= 0)` → `defense < 0` in `ArmorDefinition` | **RED** — `aNaNDefenseIsRefused…`: NaN loaded, nothing thrown |
| two `ArmorSlot`s share a footer noun | **RED** — 2 tests |
| rename `ArmorSlot.HEAD` → `HELMET` (switch + fixtures fixed so it genuinely compiles) | **RED** — 3 tests. Bonus signal: `fromName("helmet")` started resolving, so the rename would also have silently changed content parsing |
| `ArmorLoader` skips a bad piece instead of refusing the tier | **RED** — `aTierMissingASlot…`: `expected: <0> but was: <3>` |
| append a line after the rarity footer in `ArmorLore` | **RED** — 6 tests |
| `ArmorConsistency` returns 0 silently on an empty registry | **RED** — 2 tests |

> **One mutation initially reported nothing, and it was not a pass.** `./mvnw -pl paper test-compile`
> exited 1 with *"Could not collect dependencies … rpg-parent:pom … was not found"* — a reactor
> resolution failure, not a compile error, and my `grep 'error:'` matched none of it. The test never
> ran and the empty output read exactly like green. Paper-only runs need `-am`, and the surefire
> property is **`-Dsurefire.failIfNoSpecifiedTests=false`**, not the bare `-DfailIfNoSpecifiedTests`.
> Every mutation verdict above was re-taken from the **report file**, not from console output.

---

## Boot gate — OWED BY A HUMAN

`./scripts/dev-server.sh --refresh-content` (`saveResource(path, false)` never overwrites, and a
stale deployed tree has silently swallowed a content change here before — `117168e`).

**Before booting:** confirm no orphaned `java.exe` holds the jar —
`Get-CimInstance Win32_Process -Filter "Name='java.exe'"`, **not** `tasklist | grep -c`, which
reported 0 while two were running. Verify the deploy by mtime **and** size, and `unzip -l` the shaded
jar for `content/armor/diamond.yml`. (Confirmed present in the built jar: all six tier files.)

| # | Check | Expected |
|---|---|---|
| 1 | boot log | `Loaded … 24 armor`; zero `Skipping malformed armor tier`, zero id-collision warnings, **zero `ArmorConsistency` mismatches**. *A load check — establishes nothing mechanical, except that all 24 authored numbers match vanilla.* |
| 2 | `/rpg give diamond_helmet` | name **green** (Uncommon); lore `Defense: 3`, flavour, `Uncommon Helmet` last |
| 3 | `/rpg give iron_chestplate` | name **white** (Common), footer `Common Chestplate` |
| 4 | tooltip of any minted piece | **no `+N Armor`, no `+N Armor Toughness`** — `HIDE_ATTRIBUTES` holding |
| 5 | equip minted diamond head → chest → legs → feet | `⛨` climbs **3 → 11 → 17 → 20** |
| 6 | `/rpg damage 100` in full minted diamond | **~83 lost** — `applyDefense(100, 20)` |
| 7 | armor bar in full minted diamond | **~1/6 filled** |
| 8 | strip all minted armor | `⛨` gone, bar empty, no stranded modifier |
| 9 | hold a minted helmet: `/rpg enchant candidate 0 unbreaking` → `level 0 0 3` → `active 0 0` | enchant lines **above** everything, footer still last, glint appears. `/rpg enchant show` on armor replies the weapon-and-shield-only message rather than throwing |
| 10 | `/rpg give leather_helmet` | reads **Leather Cap**, footer `Common Helmet` |
| 11 | mob melee, minted-armored vs bare | armored hit visibly smaller |

### The rows that could only pass if the design is right

Rows 1, 2, 3, 8, 10, 11 can pass on a plausible-looking wrong implementation. These cannot:

- **Rows 5–7 together.** These are the same three numbers the Defense gate already pinned for
  *vanilla* diamond (`PLAN-defense-stat.md` rows 1/3/4). Minted diamond reproducing 3/11/17/20, ~83
  and ~1/6 is the proof that minting changed **nothing** about the source — the entire claim of this
  slice, witnessed rather than asserted. Any divergence means the mint altered the item's attributes.
- **Row 4 read together with row 7.** Row 4 alone witnesses `HIDE_ATTRIBUTES`. If row 4 fails while
  5–7 pass, the flag is merely missing (cosmetic). If row 4 passes and **row 7 is wrong**, the
  modifiers were *stripped* rather than hidden — the landmine. Neither row alone distinguishes them.
- **Row 9.** Proves the third `HeldGear` arm dispatches to `ArmorItems.remint` and that lore rebuilds
  with the footer still last. Without it, armor is enchant-compatible only in principle.

**A guard that fires is not a hypothesis to argue with.** If row 7 reads full rather than ~1/6, that
is the result. Test the explanation before believing it.

---

## Out of scope, and recorded in `NEXT.md`

- **The `GearDefinition`/`GearItems` extraction — the committed next PR.** Three shapes on disk now.
  Single gate: minted weapon, shield **and** armor byte-identical across the refactor. It should also
  fold the duplicated whole-number trimmer (four copies now) and `ArmorConsistency`'s duplicated
  vanilla-armor read.
- **Slice 2**: Protection / Growth / Mana Bank, the `ARMOR` `GearClass`, the roll, `/rpg enchant
  show`. Its **first task is splitting the Defense read from the `nativeArmor` read**. Also:
  **Max Mana is not a reconciled stat** — `ResourcePool.max` is a single `final double` shared by
  every player, with no `ModifierTarget` and no reconcile path, unlike `HealthState.max`. Mana Bank
  is a slice of its own, not wiring, and needs a max-decrease clamp decision `ResourcePool` currently
  gets for free because max never moves. **Verify before planning Slice 2.**
- **Turtle helmet** — a HEAD-only seventh tier that breaks the 6×4 grid the per-tier loader is built
  on, and it grants Water Breathing, a vanilla status effect this project does not model. Waiting on
  status-effects-on-gear (`DESIGN-status-effects.md`) — a named dependency, not an oversight.
- **Armor durability is vanilla's, untouched.** Weapons and shields own their wear; armor does not.
  And because mob melee is tokened to `0.01`, minted armor will barely wear at all. A known.
- **No `ArmorRefresher`** — armor lore will not rebuild from content on rejoin. `ShieldRefresher` is
  still missing for the same reason; the extraction PR is the natural place to fix both.
- **No `ContentValidator.validateArmor`** — shields have none either.
- **Untagged vanilla armor still sources Defense**, and always did. Unlike a vanilla shield (which
  gives zero custom protection), a plain diamond chestplate works fully. Minting adds rarity, lore
  and an enchant container — **not** mitigation. Stated plainly so nobody later reads it as a bug.
- **Non-tokened damage** (fall, fire, explosions, projectiles) never reaches custom HP — no handler
  exists — and `ArmorBarOverride` has already driven the vanilla `armor` attribute down to the DR
  value, so vanilla's own mitigation of those sources is computed against ~3.33 rather than 20. This
  is **pre-existing from the Defense pass**, not introduced here, and it lands on vanilla health that
  `HeartBarRenderer` overwrites from custom HP.
