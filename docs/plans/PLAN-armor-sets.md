# Armor sets — authored Defense, copper, set bonuses, the four columns (2026-11-25)

Branched off `feat/summoner-owed` (#300, `cb171624`), itself on master `d86403f4`. Read from the wire before any work:

```
git ls-remote origin   (2026-11-25, before the first commit of this stack)
cb17162440329dd38900b78131405b83b0beeed4  refs/heads/feat/summoner-owed
d86403f4c3aa9a70202a67bd45cbbb14b96e7d74  refs/heads/master
```

**NOT BOOTED.** No boot without Ben's own "ready" and the seat's approval of code and gate from origin.

## 0. BEN'S WORDS, VERBATIM, 2026-11-25 (relayed by the seat)

- *"I say think we start making vanilla armor mintable and compatible with our systems"* / *"I forgot we did that.
  Regardless, they're not done yet. I would like to add set bonuses and bring them up to speed"*
- Asked full set or steps; what each gives; whether a broken piece breaks the set; whether gear score changes the
  bonus: *"1. Full set only 2. I'd like to see the base stats for the full sets for each armor type 3. No a broken
  piece doesn't break the set, but it does stop working like the other items 4. No it's flat  Bring copper armor in. No
  turtle helmet Our durability system Four column enchant layout yes"* (the last "yes": owned pieces refresh when an
  armor file changes)
- *"Leather 1. Change the defense to 5/8/7/4 respectively 2. Set bonus is +2 regen (this is to the every 5s number)"*
- *"Golden 1. Change the defense to 5/8/7/4 2. Set bonus +2 mana regen (this is every second)  Chainmail 1. Change the
  defense to 5/8/7/4 2. Set bonus +50% quiver size  Copper 1. Change the defense to 5/8/7/4 2. Set bonus +1 Max Summon
  Iron 1. Change the defense to 10/16/14/8 2. Set bonus +100 max health  Diamond 1. Change the defense to 15/24/21/12
  2. Set bonus +20 defense  Netherite 1. Change the defense to 15/24/21/12 2. Set bonus +40 defense"*
- *"1. Yes leather is health regen 2. .5s round up 3. Common 4. The first will be piece specific the second will be
  element specific the 3rd and 4th normal"*
- *"1. Helmet boots chestplate and leggings all different 2. both empty is fine"*
- The four numbers are helmet / chestplate / leggings / boots, in that order.

| set | head | chest | legs | feet | full set | rarity | set bonus (all four worn) |
|---|---|---|---|---|---|---|---|
| leather | 5 | 8 | 7 | 4 | 24 | common | +2 health regen per 5 s |
| golden | 5 | 8 | 7 | 4 | 24 | common | +2 mana regen per second |
| chainmail | 5 | 8 | 7 | 4 | 24 | common | +50% quiver size, halves round up |
| copper | 5 | 8 | 7 | 4 | 24 | common | +1 max summons |
| iron | 10 | 16 | 14 | 8 | 48 | common | +100 max health |
| diamond | 15 | 24 | 21 | 12 | 72 (92 with the set) | uncommon | +20 defense |
| netherite | 15 | 24 | 21 | 12 | 72 (112 with the set) | uncommon | +40 defense |

No turtle helmet. The six existing tiers' rarities are unchanged.

## 1. THE SPLIT

| PR | branch | holds |
|---|---|---|
| 1 | `feat/armor-sets-1` on #300 | P-1, P-2; A1-A4; the seven content files' Defense, copper (C1), no turtle (C2) |
| 2 | `feat/armor-sets-2` on PR 1 | B1-B8: the set, its decision, its six bonuses, its tooltip |
| 3 | `feat/armor-sets-3` on PR 2 | E1-E3: the `armor_slot:` declaration and the pools test |
| gate | `feat/armor-sets-gate` at the top | `GATE-armor-sets.md` and its scenario, with #300's owed rows first |

**D1-D3 (our durability on armor) are STOPPED, section 4.** F1 is answered by a READ, section 5.

## 2. PR 1

### P-1 — one price for a mob's melee, on a player and on a summon (PLAN-summoner 11, R1, as ruled)

`core/summon/SummonHitPrice.of(melee, seededAttack, eventAmount, source)`: melee (`ENTITY_ATTACK`) is the dealer's
seeded attack stat, the number `onMobMeleeAttack` prices a player's hit from; every other mob cause keeps
`MobDamagePricing.price(eventAmount)` (M16). Its own class because `MobDamagePricingTest` pins that no public method
there takes a second double. Wired in `RpgListeners.onSummonDamaged`'s `MOB_HIT` arm.

- **Consequence for the gate:** SM7's scripted `mob_attack` on the wolf now reads the zombie's seeded attack (15.000 at
  score 100, the same EXECUTED figure as SM3A: `MobScaling.attackDamage(3.0, false, true, 100)`), not 10.000. SM7R is
  re-predicted in the gate PR.

### P-2 — the direct_summon attack-value refusal, and the stand-down removed (PLAN-summoner 11, R4, as ruled)

- `WeaponDefinition` refuses a weapon whose `left_click` is a `direct_summon` AND authors `attack_damage > 0`
  (`"binds left_click to a direct_summon ... may not also declare attack_damage"`). The Bone Whistle authors none.
- `RpgListeners.onPlayerMeleeAttack`'s 4c stand-down and `WeaponFire.leftClickDirects` are removed. `SummonDirectTest`
  now asserts the absence, with the melee handler found first as its control.

### A1 — the authored number is the source

- **READ, before:** `DefenseModifierItems.scan` read `vanillaArmorPoints(material)` per slot and fed it both to the stat
  (scaled by the piece's score) and to `nativeArmor`.
- **Now:** one lookup, `core/weapon/ArmorDefenseSource.authored(registry, tag, materialToken, slot)`: the tag, then the
  material (A3), filtered to the slot. The scan scales THAT by the piece's score; `nativeArmor` still accumulates
  vanilla's points, unscaled, first (`GearScoreWiringSignatureTest` pins the order and now also bans
  `nativeArmor += authored`). One walk, one entry per slot, as 2a built it.
- **CC'S CALL:** a material no content piece covers (the turtle helmet; a non-armor item in a slot) keeps VANILLA's
  points as its Defense rather than 0, so nothing Ben sees on a turtle helmet changes. Alternative: 0.
- `PlayerHealthSystem` now takes the `ArmorRegistry` (constructed after the armor load in `RpgPlugin`).
- Every "DISPLAY-ONLY" claim is corrected: `ArmorDefinition`, `ArmorLoader`, `ArmorItems`, `DefenseModifierItems` by
  dated notes; `diamond.yml`'s header block rewritten (it is a content header); the five other tier headers' one-line
  pointer rewritten. `PLAN-armor-slice-1.md` keeps its account and gains a dated pointer here.

### A2 — ArmorConsistency checks coverage

`core/weapon/ArmorCoverage.problems(serverArmor, pieces, excluded)`: a server armor material (minus the excluded) with
no piece; a piece naming a material the server lacks; a piece authored under a slot its material is not worn in; a
piece on an excluded material. `ArmorConsistency.serverArmor()` reads the server's side: every non-legacy item whose
vanilla defaults grant armor in one of the four slots, through the same `vanillaArmorPoints` the native half uses (the
BODY slot, horse and wolf armor, is not one of the four). It still warns at zero pieces, now naming the cost under A1
(every piece falls back to vanilla, no set exists). Positive control: `ArmorCoverageTest.aCompleteRosterHasNoProblems`
beside each fault, and `ArmorConsistencyTest`'s staged server with the uncovered piece then covered.

### A3 — an untagged piece

Reads the same authored Defense through the same lookup, at the base score (it carries no stamp:
`GearScore.orAbsent(empty)` = 100). From PR 2 it counts toward a set the same way. **What it cannot do:** carry
enchants (no container written), carry a gear score of its own, or show our tooltip (no lore is written on an item we
never minted). **SEAT'S CALL, OPEN FOR BEN.**

### A4 — the bar and the carried leak at Defense 112 (READ and EXECUTED; no STOP)

EXECUTED in jshell against `core/target/classes`:

| Defense | DR | `armorBarPoints` | `barModifier(d, 20)` | `applyDefense(100, d)` |
|---|---|---|---|---|
| 20 (full diamond before) | 0.16666666666666666 | 3.333333333333333 | -16.666666666666668 | 83.33333333333333 |
| 72 (full diamond, no set) | 0.4186046511627907 | 8.372093023255815 | -11.627906976744185 | 58.13953488372093 |
| 92 (diamond with set) | 0.4791666666666667 | 9.583333333333334 | -10.416666666666666 | 52.083333333333336 |
| 112 (netherite with set) | 0.5283018867924528 | 10.566037735849056 | -9.433962264150944 | 47.16981132075472 |

- **The bar at 112:** the attribute lands at 10.566 of 20, a 52.8% fill that equals the DR, the design. It does not
  clamp (Minecraft's armor attribute ceiling is 30, INFERRED from vanilla knowledge, not read here) and it is not
  negative. Netherite's native sum is 20 (READ: `ArmorMaterials` NETHERITE 3/6/8/3).
- **The leak at 112:** `NEXT.md`'s *ArmorBarOverride LEAKS OUR DR INTO VANILLA'S MITIGATION* is still INERT, for the
  reason it gives. READ, `VanillaDamagePolicy.forCause`: the only PASS causes on a player are `ENTITY_ATTACK` and
  `ENTITY_SWEEP_ATTACK` (owned by the tokened melee paths) and `KILL` and `VOID` (vanilla's `bypasses_armor` set);
  every other cause is REROUTE. So no cause reaches vanilla's armor curve with an untokened amount, at 20 or at 112.
- **What IS larger at 112, stated for the seat:** the LATENT size. The attribute now holds 10.57 where full diamond put
  3.33, so the day something passes untokened, vanilla's own curve would take a larger share. Nothing observable is
  worse today, so this is reported and not a STOP. Netherite's toughness (3 per piece) also sits on the attribute and
  is equally inert.

### C1 — copper (READ)

- `javap` on `paper-api-26.1.2.build.74-stable.jar`: `Material.COPPER_HELMET / _CHESTPLATE / _LEGGINGS / _BOOTS`, and
  `ItemType.COPPER_*` typed `ArmorMeta`.
- The server jar (`run/versions/26.1.2/paper-26.1.2.jar`) ships `data/minecraft/recipe/copper_{helmet,chestplate,
  leggings,boots}.json`, each `crafting_shaped` from `minecraft:copper_ingot`, result `minecraft:copper_<piece>`.
- `content/armor/copper.yml`: common (BEN), Ben's 5/8/7/4, vanilla's own item names, `craft_result` per piece, so a
  vanilla craft mints exactly as the other six (`Mint-on-craft` 38 → 42 indexed, 54 → 58 gear). Flavor:
  *"It greens where it is not worn."* **SEAT PLACEHOLDER 2026-11-25, OPEN FOR BEN.**
- Recounts, each with its control: `ArmorLoaderTest` seven tiers and 28 pieces against Ben's table; `UniqueSlotContentTest`
  28 armour / 64 gear / 64 pool rows (`grep -h "^    material:" content/armor/*.yml | wc -l` = 28); `SelfTestWeaponIdsTest`
  33 (28 armor + 5 tools); `BootCountLinesTest` 42 / 42 / 58; the `RpgPlugin` empty-roster warning (28 from seven).
- The golden file: predicted `49 / 25` (24 Defense lines modified, four 6-line copper blocks, the `103 → 107` tail),
  measured `49 / 25`.

### C2 — no turtle helmet

`ArmorConsistency.EXCLUDED = Set.of("turtle_helmet")`, with Ben's words on it. Mutation-tested (emptying it names the
turtle helmet as uncovered).

### Tests and mutations, PR 1

Each mutation's marker grepped present and the original gone, the delta against a pristine copy measured, the file
restored by `cp` and the marker re-grepped as 0.

| mutation | file | result |
|---|---|---|
| MUTP2: the refusal never fires | `WeaponDefinition` | RED, `WeaponDefinitionTest` (first run did not compile: a bare marker; re-run with a comment marker) |
| MUTP1: every arm prices the event | `SummonHitPrice` | RED, 15.0 expected, 10.0 read |
| MUTP1WIRE: the melee flag false | `RpgListeners` | RED, `SummonHitPriceWiringTest` (first two runs did NOT apply: a `$` anchor on CRLF, then a cross-line pattern under `-p`; both re-run) |
| MUTSTAND: a stand-down line reintroduced | `RpgListeners` | RED, `SummonDirectTest`'s absence row |
| MUTA1SCAN: scale vanilla, not authored | `DefenseModifierItems` | RED, two `GearScoreWiringSignatureTest` rows |
| MUTA3: no material fallback | `ArmorDefenseSource` | RED, two rows |
| MUTA1SLOT: no slot filter | `ArmorDefenseSource` | RED |
| MUTA2SLOT: no slot comparison | `ArmorCoverage` | RED |
| MUTA2EXCL: `EXCLUDED` emptied | `ArmorConsistency` | RED, 2 problems for 1 |
| MUTLADDER: leather chest 8 → 7 | `leather.yml` | RED on `leather_chestplate` |

## 3. PR 2 — the set (B1-B8)

### 3.1 B1 — the set and its `set_bonus:` list

- **A set is the four pieces of one tier file.** `ArmorDefinition` gains `set` (the tier name, from the filename) and
  `setBonus` (the tier's list), carried on all four pieces. No set id or stat name in Java beyond the type enum.
- `core/weapon/ArmorSetBonus(type, value)`, types `health_regen`, `mana_regen`, `max_summons`, `max_health`, `defense`.
  It refuses a non-finite or zero value, and a fractional `max_summons`.
- `ArmorLoader.setBonus` refuses each of these, and the tier is refused whole: a non-list, a non-map entry, an unknown
  type (listing the five), and a missing or non-numeric value. A set with fewer than four pieces cannot load: the slot
  walk refuses the tier at the first missing slot.
- **THE UNIT, in one place:** the YAML holds `health_regen` in Ben's per-5-seconds unit. `ArmorSetBonus.engineValue`
  divides it by 5. Everything else is authored in the engine's unit. The tooltip prints the authored value.

### 3.2 B2 — the decision (pure, `core/weapon/ArmorSetDecision`)

Active exactly when all four slots resolve, through the one lookup (`ArmorDefenseSource.resolve`: tag, then material),
to pieces of one set, each in its own slot. The `broken` flag is carried and deliberately not read. Tested as the seat's
table in `ArmorSetDecisionTest`:

- four of one set;
- three plus one other;
- three plus an empty slot;
- four with one broken;
- four all broken;
- minted and untagged mixed;
- plus a piece with no set, a helmet in the feet slot, and a missing slot.

**The paper side passes "not broken" for every slot** until D1-D3 are ruled. There is no armor broken state yet
(section 4).

### 3.3 B3 — each bonus on its existing path; and the STOP on quiver percent

READ, `PlayerHealthSystem.startReconcileLoop`:
- **The loop:** `RepeatingTask` every `RECONCILE_PERIOD_TICKS = 5` on the player's entity target. `onJoin` and
  `onRespawn` each re-register the player and start it. While he is dead the target is inactive.
- **Equip, unequip, a break and a drop** all reach the stats on the next pass, by ABSENCE. There is no event: the
  set's one key, `armorset:<tier>`, is missing from the desired map and the reconciler drops it. **Join and death**
  re-register at base and rebuild within one period.
- **The merges:** the set's contributions (`ArmorSetItems.contributions`, read ONCE per pass) go into the FIRST argument
  of each stat's ONE reconcile, through `AccessoryContributions.merged`. Never a second reconcile, never an inline
  `putAll` (the measured hollow guard). `ArmorSetWiringSignatureTest` pins all four. `AccessorySourceKeysTest` pins the
  key disjoint from every other source, for every shipped tier.

| stat | path | the set's value in the engine |
|---|---|---|
| health regen | `reconcileHealthRegenModifiers`, through `HealthRegen.contribution` | leather +0.4 HP/s |
| mana regen | `ManaTransition.reconcile`'s regen map (it pins the lazy pool), through `ManaRegen.contribution` | golden +2 mana/s |
| max health | `reconcileMaxModifiers` (`HealthState`'s headroom and clamp) | iron +100 |
| defense | `reconcileDefenseModifiers`; never `nativeArmor` | diamond +20, netherite +40 |
| max summons | not a `Stat`: `MaxSummons.effective`'s gear term, read at each cast and by `/rpg maxsummons` (B6) | copper +1 |
| quiver size % | **STOPPED** | — |

**STOPPED: `quiver_size_percent` (chainmail's +50%), on the seat's own B3 clause.** READ:
- `HealthState.quiverSizeBonus` is a `Stat`, and `Stat` is additive only: `value()` is the base plus the modifiers'
  sum, and no modifier has a mode.
- So a percent has no per-player modifier path. B5's "one place, `QuiverSize.resolve`" is the arithmetic; the
  per-player SOURCE the percent would need does not exist.
- Building one means a new multiplicative path on a shared stat, or a percent `Stat` on `HealthState`. Either is
  shared stat code no ruling covers.
- **What is pushed instead:** chainmail ships no `set_bonus`. Its file says why. `quiver_size_percent` is not a type,
  so the loader refuses it and no tooltip can advertise it. **The `2 → 3, 5 → 8, 8 → 12, 2+3 → 8` examples are
  unbuilt.**
- **Two things for whoever rules it.** First, Ben said *".5s round up"*. Read as round-half-up, every value at 50% is a
  whole number or a half, so that reading and "ceiling" agree on the whole 50% grid and differ only at other
  percentages. Second, READ: loaded arrows are clamped at the next write (`QuiverItems.setLoaded`), not on removal, as
  Expanded Quiver's removal behaves.

### 3.4 B4 — flat

No bonus reads a gear score. Diamond's +20 and netherite's +40 are their own source on the Defense stat, summed after
the pieces' scaled Defense: 72 + 20 = 92, 72 + 40 = 112 at score 100.

### 3.5 B6 — max summons

`ProfileService.maxSummons(id, gearGrants)`. Both callers pass `ArmorSetItems.contributions(...).summonGrants()`:
- `SummonService.cast`, before `SummonCap.decide`;
- `/rpg maxsummons`, which is where the number is shown. It is not on the stats sheet.

Taking the set off removes nothing; the next cast obeys the cap. `ArmorSetContributionsTest`: with two weapons' summons
live and the cap back at 1, a recast of either is `REFUSED_FULL`, and so is a third weapon's first cast.

### 3.6 B7 — max health, and the rates (READ)

- **Max health:** iron's +100 is headroom on gain and a clamp on loss, on the real store
  (`ArmorSetContributionsTest`: 100/100 → 100/200; healed to 200/200; set off → 100/100).
- **Health regen:** base `HealthRegen.BASE_PER_SECOND = 0.2` (1 per 5 s). With leather it is 0.6/s (3 per 5 s), shown
  as `3.00/5s` (`StatsSheetLines.perFiveSeconds`). Saturated, x5 (`SATURATED_MULTIPLIER`), 3.0/s.
- **Mana regen:** base `RpgPlugin.MANA_PER_TICK = 100 / (100 x 20)` = 1 mana/s, shown as `5.00/5s`. With golden it is
  3 mana/s, shown as `15.00/5s`.

### 3.7 B8 — the tooltip and the sheet

- Every piece of a set with a bonus shows: a blank line, `Set Bonus (4 pieces):` in gold, then one gray line per entry
  in Ben's unit (`+2 Health Regen per 5s`, `+2 Mana Regen per second`, `+1 Max Summons`, `+100 Max Health`,
  `+20 Defense`). It is static, and identical on all four pieces. **Wording, colours and placement: SEAT PLACEHOLDER
  2026-11-25, OPEN FOR BEN.**
- Golden predicted `72 / 0` (24 pieces x 3 lines; chainmail adds none), measured `72 / 0`.
- **The stats sheet** (`/rpg stats` and the Nexus head) needs no change. It reads the converged stats, so the active
  set's contribution shows in Max Health, Health Regen, Mana Regen and Defense. Max summons shows in the
  `/rpg maxsummons` reply.

### 3.8 The seven files' set_bonus blocks, as committed

| file | block |
|---|---|
| leather | `- type: health_regen / value: 2` |
| golden | `- type: mana_regen / value: 2` |
| chainmail | none (STOPPED, 3.3) |
| copper | `- type: max_summons / value: 1` |
| iron | `- type: max_health / value: 100` |
| diamond | `- type: defense / value: 20` |
| netherite | `- type: defense / value: 40` |

### 3.9 Mutations, PR 2

Each mutation was checked three ways: the marker present and the original gone; the delta against a pristine copy;
the marker back to 0 after restoring.

| mutation | result |
|---|---|
| MUTB1DIV: health regen not divided by 5 | RED |
| MUTB2SAME: two sets not compared | RED |
| MUTB2EMPTY: an empty slot skipped | RED, 2 rows |
| MUTB2BROKEN: a broken piece refused | RED, 2 rows |
| MUTB8: the tooltip prints the engine value | RED |
| MUTB3PREFIX: the key prefix is `growth:` | RED |
| MUTB3WIRE: Defense loses the set merge | RED |
| MUTB6: the cast passes no grant | RED. The first run did not compile (the needle left a package prefix); it was re-run |
| MUTB1LOAD: every piece's set is `x` | RED, 2 rows |
| MUTB1UNKNOWN: an unknown type is skipped | RED on `quiver_size_percent` |

## 4. STOPPED: D1-D3, OUR DURABILITY ON ARMOR — TWO RULINGS CONFLICT, AND ONE FACT DOES NOT HOLD

**READ, how weapons and shields own wear today** (`WeaponDurability`, `ShieldDurability`, core `Durability`):
- **The maximum is READ AT RUNTIME, from the material:** `WeaponDurability.maxOf(item)` is
  `item.getType().getMaxDurability()`. No content file authors a maximum; no definition carries one (`GearDefinition`'s
  javadoc: *"Not durability. Weapons and shields own their wear; armor's is vanilla's."*).
- **The wear is VANILLA's own `Damageable` damage on the stack**, written by `WeaponDurability.write`. There is no
  PDC durability record.
- Broken is `damage >= max - 1` (`Durability.isBroken`), floored there so the item is never destroyed. Vanilla's
  damage is cancelled through `PlayerItemDamageEvent` (shields always; bows and crossbows through `VanillaShots`).
- Repair: only `/rpg repair` and `/rpg durability`, both scoped to weapon and shield ids.

**The conflict:**
- **D1:** *"Armor uses the same wear ... READ how each existing kind does it and mirror it; no third implementation."*
  Mirroring means the maximum is read at runtime from the material.
- **D2:** *"each piece's max durability is vanilla's for that item (READ, authored into content, not read at runtime)."*
  An authored maximum the shared helpers do not read would be a display-only number (exactly what A1 just retired); a
  maximum they DO read means changing `WeaponDurability.maxOf`, which weapons and shields share, and no ruling covers
  that (THE STOP RULE: shared code no ruling covers).
- **And F1's *"a piece minted before this change with no durability record gets a full one"* has no subject:** no
  durability record exists; every armor item already carries vanilla `Damageable` damage, and
  `GearItems.carryWear` already carries it across a remint, clamped (READ).

**The vanilla maxima, READ, for whichever way it is ruled** (`ArmorMaterials` durability multiplier x `ArmorType` unit:
helmet 11, chestplate 16, leggings 15, boots 13):

| set | multiplier | head | chest | legs | feet |
|---|---|---|---|---|---|
| leather | 5 | 55 | 80 | 75 | 65 |
| golden | 7 | 77 | 112 | 105 | 91 |
| copper | 11 | 121 | 176 | 165 | 143 |
| chainmail | 15 | 165 | 240 | 225 | 195 |
| iron | 15 | 165 | 240 | 225 | 195 |
| diamond | 33 | 363 | 528 | 495 | 429 |
| netherite | 37 | 407 | 592 | 555 | 481 |

(Products EXECUTED in the shell from the two READ factors.)

**What depends on it, and waits:** B2's broken piece (it counts toward the set and gives nothing of its own) is decided
in core in PR 2 with a broken flag per slot, but the paper side passes "not broken" until armor has a broken state;
under vanilla wear a piece is destroyed at its maximum rather than floored, so no armor piece is ever "broken" today.
The gate's wear rows wait with it.

## 5. F1 — REFRESH (READ; nothing to build)

`GearRefresher.refresh` runs first in `RpgListeners.onJoin` and on `/rpg refresh`, walks the player's inventory contents
(the armor slots included), and remints every tagged item through `GearItems.remint`, whose armor arm is
`ArmorItems.remint`: a fresh mint from current content, the tag, wear (clamped), enchants and score carried. So armor
already refreshes from content on join by the weapons' mechanism. **There is no `ShieldRefresher` gap:** shields were
folded into the same `GearRefresher`. Under A1 the Defense does not even need the refresh (the scan reads content live);
the TOOLTIP does, and gets it. The set lines (PR 2) reach existing items the same way.

## 6. CC'S CALLS

| # | call | alternative |
|---|---|---|
| CC1 | an armor material no content piece covers keeps vanilla's points as its Defense | 0 |
| CC2 | P-1's melee test is the event's cause `ENTITY_ATTACK`, the dealer the damage source's causing entity | the direct entity |
| CC3 | the set lives on each piece (`ArmorDefinition.set` and `setBonus`), so the tooltip reads it with no registry | a separate set registry the tooltip would have to reach |
| CC4 | a tier with no `set_bonus:` is still a set, with no bonus | no set at all |
| CC5 | max summons' grant is read by the callers, which hold the player, and passed into `ProfileService.maxSummons` | the profile service reading equipment |

## 7. OPEN FOR BEN

| # | what | placeholder |
|---|---|---|
| O1 | copper's flavor line | *"It greens where it is not worn."* |
| O2 | A3: an untagged piece reads the same Defense and completes a set (the seat's call) | as built |
| O3 | the turtle helmet keeps vanilla's 2 Defense (CC1) | as built |
| O4 | D1-D3: authored or read maxima (section 4) | STOPPED |
| O5 | the set tooltip: header, line wording, colours, placement (B8) | `Set Bonus (4 pieces):`, gold; one gray line per entry |
| O6 | chainmail's +50% quiver: STOPPED (3.3); and "round up" as half-up vs ceiling at other percents | unbuilt |
| O7 | the order of B5's percent after the flat bonus (the seat's placeholder) | unbuilt |
