# PLAN — TRUE TOOLTIPS: AN ITEM'S STAT LINES REFLECT ITS OWN ACTIVE ENCHANTS (2026-11-26)

From the design seat's brief via Ben, 2026-11-26 (T1 to T14), and its ruling on CC's two STOPs the same day (D1 to D4).
Built on #312 (`feat/bottle-owed` `91a8de1c`). The stack, bottom up: `feat/true-tooltips-resolvers` (the executor's
resolvers extracted), `feat/true-tooltips` (this plan, the tooltips, the tests), `feat/true-tooltips-gate`
(`docs/gates/GATE-true-tooltips.md`). NOT BOOTED.

## 1. BEN'S WORDS, VERBATIM, 2026-11-26

- *"I'm over turning a ruling I made weeks ago. The tool tips inside of weapons don't account for enchantments. I'd like
  to change this so the stats reflect exactly what you're getting"*
- Asked what counts (A: only the item's own active enchants; B: everything affecting the player) and how it reads (the
  true number, or the number with the enchant's share): *"1. A 2. Just the true number"*
- Asked whether behaviour enchants rewrite the ability lines (B) or keep only their own line (A), and whether armor is
  included: *"3. B 4. Yes,"*
- Shown three options for range, wind-up and reload (A: a new line always shown; B: a line only while an enchant moves
  it; C: leave these three alone): *"C. Leave them alone for now. Damage based stats are more my concern right now"*

## 2. WHAT THIS OVERTURNS, AND WHAT IT DOES NOT

- **OVERTURNED 2026-11-26:** `PLAN-enchant-columns.md`, OPEN FOR BEN item 3, *"The weapons' own tooltips are NOT edited
  with a tune active: the Flint Staff still reads its authored 20 (the enchant's line says +12), the Cursed Emerald
  "27 x 3"."* The Flint Staff with Large Sparks III now reads `Fire Damage: 32`, and the Cursed Emerald with Echo Beam III
  reads `27 x 6`. That item carries a pointer here.
- **NOT overturned:** Ben's 2026-11-14 ruling that an item's ENCHANT LINE is the name and numeral only (*"just Deft Hands
  III"*, `PLAN-deft-hands.md`). `EnchantLore` is untouched; what changed is the stat and ability lines above it.

## 3. THE SEAT'S ERROR, IN ITS WORDS (2026-11-26, recorded as the seat asked)

> *"T4 and T5 told you to fold range, wind-up and reload into "the line's existing format". I had not read the
> tooltips: no weapon has a Range, Wind-up or Reload line. T6's example wording ("Max Health: +40") was also mine and
> wrong; and I did not read that armor and shields already fold their own enchants."*

CC stopped before building (READ, `golden-lore.txt`, 108 renderings at `91a8de1c`: 0 Range, 0 Wind-up, 0 Reload lines;
the 16 hits for "Range" are all "Ranged Damage" or "Ranged Weapon"). The seat: *"Stopping before building was right."*

## 4. THE RULINGS AS BUILT

- **T1, ONE TRUTH.** The tooltip of the EFFECTIVE item: the definition, this stack's score and its ACTIVE enchants, read
  by the engine's own resolvers (section 6). No second formula.
- **T2, WHAT COUNTS (Ben: "A").** Only the item's own active enchants and score (section 5 says, per weapon kind, which
  terms of `sent=` are the item's).
- **T3, ACTIVE ONLY.** Every read is `EnchantState.effective()`: an inactive, locked or level-0 candidate changes
  nothing; one enchant acts per slot.
- **T4/T5, NARROWED TO DAMAGE-BASED LINES (the seat, after Ben's "C").** Damage adds and percents, the `x N` count,
  crit chance. "Just the true number": the line's own format and colour, one figure.
- **STOP 1, BEN'S "C":** NO Range, Wind-up or Reload line. Polished Scepter, Early Toll and Deft Hands change NOTHING on
  the stat lines; they keep their enchant line. Focused Beam's block states its damage and NOT its range. T13 asserts
  the whole tooltip is unchanged under each of them, every level.
- **T6/T11, ARMOR AND SHIELDS: ALREADY BUILT** (READ, `ArmorItems.applyLore`, `ShieldItems.applyLore`). Defense with its
  own Protection (`Protection.effectiveDefense`, the stat scan's function), `Health: +N` for Growth, `Mana: +N` for Mana
  Bank, Damage Reduction with its own Bulwark (`Bulwark.effectiveDr`, the block rider's). The shipped behaviour and
  wording stand (the seat). Built here: nothing but a pure `statLore` extracted from each `applyLore`, unchanged, so T13
  can drive it.
- **T7, BEHAVIOUR BLOCKS (Ben: "B"), damage and counts only.** `BehaviourBlocks`: ONE exhaustive switch over every
  `EnchantBehaviour` type; prose from the enchant's YAML `ability_description:` (SEAT PLACEHOLDER).
- **T8, CONDITIONAL:** section 7's table.
- **T9, EVERY WRITE REDRAWS:** section 8.
- **T10 WITHDRAWN (the seat).** The quiver line stays exactly as it is: it reads the stamp, which includes the wielder's
  own bonuses. It is the ONE deliberate exception to T2, because "12/8" would be a lie the player can see. Expanded
  Quiver therefore needs no tooltip work. OPEN FOR BEN.
- **T12, THE ENCHANT TABLE** shows the input through `gear.remint` (READ, `EnchantMenu`), which runs the same
  `applyLore`. No second builder: `WeaponLore.build`, `ArmorLore.build` and `ShieldLore.build` are each called from
  exactly one main-code site (READ, `git grep`).
- **T13, THE CONSISTENCY TEST:** section 9. **T14, THE GOLDEN:** section 10.
- **D1, TOKENS.** `{shots}` (a numeral) and `{shots_word}` (one to twelve as a word, a numeral above), filled from the
  EFFECTIVE cast by `DescriptionTokens.fill`. A cast HAS a count when it is a volley or a projectile with a spread.
  `WeaponLoader` refuses an unknown token and a shot token on a cast with no count, naming the weapon and trigger;
  `EnchantLoader` the same for a granted trigger's description, and refuses an unknown token in `ability_description:`
  by name (its cast is checked against the weapon's in `TrueTooltipsContentTest`, which can load both).
- **D2.** `cursed_emerald.yml` reads `"Channel, then loose {shots_word}"`: "three" bare, "six" under Echo Beam III.
  The sweep for numbers a DAMAGE-BASED enchant in a pool changes (every `description:` under `weapons/`, `abilities/`
  and `enchants/` with a digit or a number word, 2026-11-26):

  | file | the words | tokenised? |
  |---|---|---|
  | `cursed_emerald` | "loose three" | **YES**, `{shots_word}` (Echo Beam sets the volley's shots) |
  | `breacher` | "Four chunks", "6 degrees" | no: no enchant changes its spread; under Shatter Caps the block's prose is replaced (T7) |
  | `bone_whistle` | "100 health, 20 damage" | no: the wolf's numbers, and no damage enchant is in a summoner pool (T13's walk: no enchant moves its tooltip) |
  | `dragons_breath` | "A hexagon of arrows" | no: no enchant changes its spread |
  | `knell_beam` | "1.2s later" | **no, by D3** (wind-up); OPEN FOR BEN |
  | `living_lantern` | "within 7 blocks" | no: a homing radius, not a damage or count |

  `{damage}` is not added: no shipped description states a damage figure an item enchant changes.
- **D3.** Not tokenised: wind-up, range, reload, cooldown. The Knell Beam's description still reads "1.2s later" under
  Early Toll. OPEN FOR BEN.
- **D4.** Shatter Caps' replacement prose uses `{shots_word}`, filled from the Breacher's own spread ("four").
- **LAPIS CRYSTAL (READ, then fixed):** its granted block rendered its name, prose and cadence, but NO shot number: the
  orb's ability has no `Damage` in its on_hit, so B5 was not met. It now shows the shot as a damage line: the staff's
  beam's first literal damage x the level's percent, through the item's terms: 21 x 30/50/70% = **6.3 / 10.5 / 14.7** at
  score 100. Polished Scepter does not move it (a range tune, and only one of the two can be active in slot 0); a
  damage percent on the staff (Attunement) does, as it moves the beam (T13, every level, both scores).

## 5. WHICH TERMS OF A HIT'S `sent=` ARE ON THE TOOLTIP (T2)

READ, `EffectApplier` (`Damage` and `WeaponDamage` arms), `HitDamage`, `CastExecutor`:

```
sent    = hitBase x chargeScale x critMultiplier            (then Wildfire's chain factor, a fight term)
hitBase = itemTerm + classDamageBonus + weaponLevelDamage
itemTerm = (amount x score/100) x (1 + itemDamagePercent/100)      <- THE TOOLTIP'S NUMBER
```

| weapon kind | `amount` | on the tooltip (the item's) | the player's, never on it |
|---|---|---|---|
| melee basic (swords, the bottle) | `attack_damage` | amount, score, its own Sharpness | class bonus (accessories, gear), level bonus, charge, crit, Wildfire |
| ranged basic (Boltor, bows, crossbows) | `attack_damage` | amount, score, its own Power | class, level, charge (whatever the arm's `chargeScale` carries; not READ for a vanilla draw here), crit |
| mage / ranger ability (literal damage) | authored amount + its own damage tune (Large Sparks, Sharp Crystals) | amount, tune, score, its own Attunement / Power | class, level, crit |
| a behaviour's hit (Focused Beam, Shatter's stone, Airburst's bursts) | the enchant level's damage | the level's damage, score, its own damage percent | class, level, crit |
| the Lapis Crystal's shot | beam amount x the level's percent | that, score, its own damage percent | the beam's player terms (it carries every term of the beam that set it off) |
| crit chance line | the weapon's `crit_chance` | plus its own Sharpened Glass | the base 15%, accessories, fragments, level, the TEMP fixtures |

**The `x N` count** is the payloads one press delivers (`WeaponLoreLines.deliveredShots` over the TUNED cast), so
Echo Beam's 4/5/6 reads as `x 4/5/6`.

## 6. THE RESOLVERS EXTRACTED (T1)

| resolver | extracted from | called by the engine at | called by the tooltip at |
|---|---|---|---|
| `BehaviourCasts.replaceDamage` | three copies in `CastExecutor` (`launchStone`, `split`, `focusedBeam`) | each of the three | `BehaviourBlocks` |
| `BehaviourCasts.focusedBeam` | `CastExecutor.volley`'s loop | `volley`, shot 0 | `BehaviourBlocks` |
| `BehaviourCasts.shatter` | `CastExecutor.launch`'s loop | `launch` | `BehaviourBlocks` |
| `BehaviourCasts.airburst` | `CastExecutor.launchOne`'s loop and split condition | `launchOne` | `BehaviourBlocks` |
| `BehaviourCasts.orbShotAmount` | `CastExecutor.orbShots`' first-damage-times-percent | `orbShots` | `BehaviourBlocks` (Lapis) |
| `DamageEnchantItems.percentOn` | `desiredModifiers`' grants and class gate (now over an `EnchantState`) | `desiredModifiers` (the stat reconcile) | `ItemEnchantTermsReader` |
| `ArmorItems.statLore`, `ShieldItems.statLore` | each `applyLore`, unchanged | `applyLore` | T13 |

Used as they were: `CastTunes.tune` (the commit's one seam), `BehaviourItems.activeOf` (the press's behaviours, with the
weapon filter), `GrantedTriggers.granted`, `EnchantValues.totalFor` (`CRIT_CHANCE`, `DEFENSE`, `MAX_HEALTH`, `MAX_MANA`,
`BLOCK_DR`), `HitDamage.hitBase`, `GearScore.scaledDamage`, `HeldWeaponCrit.sources`, `CritChanceEnchant.contribution`.

**The executor's tests stayed green untouched across the extraction** (core 2623 run, 0 failures, 1 skipped, before
any new test), and mutating two of the extracted resolvers reddens the executor's OWN tests (section 11), which is what
proves the press calls them.

## 7. EVERY SHIPPED ENCHANT AND THE LINE IT MOVES (READ from `content/enchants/*.yml`, 24 files)

| enchant | effect | the line it moves |
|---|---|---|
| attunement | damage +5/10/15% (mage) | every damage line of a mage weapon |
| power | damage +5/10/15% (ranger) | every damage line of a ranger weapon |
| sharpness | damage +5/10/15% (melee) | `Melee Damage` |
| large_sparks | cast tune, damage +4/8/12 (flint_staff) | `Fire Damage` 20 → 24/28/32 |
| sharp_crystals | cast tune, damage +3/6/9 (crystal_shard_staff) | `Kinetic Damage` 16 → 19/22/25 |
| echo_beam | cast tune, shots 4/5/6 (cursed_emerald) | `27 x 4/5/6`, and the description's "four/five/six" |
| sharpened_glass | crit +5/10/15 points (broken_bottle) | `Crit Chance` +10% → +15/20/25% |
| focused_beam | behaviour (cursed_emerald) | the block: its prose, `Kinetic Damage: 108/162/216`, no `x N` |
| shatter_caps | behaviour (breacher) | the block: its prose, the stone `35/60/85`, then `40 x 4` |
| airburst_flames | behaviour (living_lantern) | the block: its prose, the flame `45`, then `25/30/35 x 3` |
| lapis_crystal | grant trigger (lapis_staff) | its granted block, now with its shot: `6.3/10.5/14.7` |
| protection | defense +3/6/9 (armor) | `Defense` (already, unchanged) |
| growth | max health +10/20/30 (armor) | `Health: +N` (already, unchanged) |
| mana_bank | max mana +10/20/30 (armor) | `Mana: +N` (already, unchanged) |
| bulwark | block DR (shield) | `Damage Reduction` (already, unchanged) |
| polished_scepter | cast tune, range +4/8/12 (lapis_staff) | **left alone (C)** |
| early_toll | cast tune, wind-up -2/4/8 ticks (knell_beam) | **left alone (C)**; its prose still says "1.2s later" |
| deft_hands | reload -10/20/30% (ranger) | **left alone (C)** |
| expanded_quiver | quiver +1/2/3 (ranger) | **no tooltip work (T10 withdrawn)**: the quiver line reads the stamp |
| unbreaking | durability | no line: it is a chance per use that a use does not wear; there is no durability stat line |
| thorns | reflect (shield) | **conditional**: acts only when a block lands |
| wildfire | hit chain (living_lantern) | **conditional**: +5/10/15% per consecutive hit on ONE target within 3 s, capped 60% |
| kinetic_tremors | shockwave (kinetic, unique) | **conditional**: fires after 3 hits within 3 s |
| mana_burn | scorch boost (fire mage, unique) | **conditional**: scales only the scorch a hit applies, and its Ignite |

**T8's "fixed part":** none of the four conditional enchants has a part that is true of every hit. Wildfire's first hit
is x1.0; Kinetic Tremors deals nothing before its third hit; Mana Burn moves only scorch. So none folds anything.

## 8. EVERY WRITE SITE AND ITS REDRAW (T9)

`TooltipRedrawSitesTest` scans main code for every enchant-state or score write and requires the ONE lore function
within three lines; the count is pinned at **9** and a planted write with no redraw is its control.

| site | writes | redrawn by |
|---|---|---|
| `RpgCommand` (`rpg enchant clear`) | `EnchantItems.clear` | `finishEnchant` → `gear.remint` |
| `RpgCommand` (`rpg enchant candidate / level / active / inactive`) | `EnchantItems.write` | `finishEnchant` → `gear.remint` |
| `AnvilMenu` (the upgrade, and its preview) | `GearScoreItems.write` x2 | `GearItems.refreshLore` |
| `EnchantMenu` (the table's clicks) | `EnchantItems.write` | `gear.remint` |
| `GrindstoneMenu` | `EnchantItems.write` | `GearItems.remint` |
| `EnchantRollItems.rollOnAcquire` (mint, `/rpg give`, a craft) | `EnchantItems.write` | `GearItems.refreshLore` |
| `GearScoreDevCommand` (`rpg gearscore set`) | `GearScoreItems.write` | `lookup.remint` |
| `GearScoreItems.stampOnAcquire` | `write` | `GearItems.refreshLore` |

Smithing and repair write neither: they `remint`, which carries the state and runs the same `applyLore`. The refresher on
join (`GearRefresher.refresh`) remints every carried item. **Why a scan and not a live read:** no test here can hold an
`ItemMeta` (no server in the suite); the function the sites reach is pure and T13 proves what it renders. The gate reads
the lore after each write the pad reaches.

## 9. T13 — THE CONSISTENCY TEST

`TrueTooltipsConsistencyTest`, generated from content: every shipped weapon, armor piece and shield; every enchant ANY of
its four slots' pools admit (`EnchantRoll.poolForSlot`, the roll's own); every level; at scores 100 and 250. The
expectation is written from the ENGINE's side (the executor's order of decisions, the enchant curves, the reconcile's
functions), never by calling the renderer.

- **The count, pinned: 1260 cases** (weapons 570, armor 672, shields 18), of which **558** are cases where the engine
  moves nothing and the whole tooltip is asserted equal to the bare one (C and T8, witnessed). Taken at `91a8de1c` plus
  this slice; it is printed by the test (`TRUE TOOLTIPS T13:`), and a new weapon, enchant or level moves it.
- **Its controls:** `theComparatorSeesAPlantedMismatch` (a +1 on the amount, a wrong count, and the pre-slice tooltip
  are each seen); the content load asserts its counts, and **the first run's control fired**: the plain `EnchantLoader`
  constructor loads 11 of 24 enchants (it drops every weapon-specific and element enchant), so the test uses the
  server's `EnchantLoader.withWeapons`.
- **Its first real run found two things, both in the test, neither in the code:** the display rounds `49.875` to
  `49.88`, exactly 0.005 off, so the tolerance is half a hundredth plus 1e-9; and the shield expectation doubled its
  label.
- **Not seen:** the enchant block (unchanged, pinned elsewhere); the quiver line (T10 withdrawn); a BROKEN armor piece:
  READ, `DefenseModifierItems`, `GrowthModifierItems` and `ManaBankModifierItems` all skip a broken piece, while its
  tooltip shows its Defense, Health and Mana as when whole. That is today's behaviour and the ruling keeps it (T11).

## 10. T14 — THE GOLDEN, PREDICTED BEFORE IT WAS REGENERATED

**Predicted:** the 747 existing lines unchanged except the tail; a new section `ENCHANTED WEAPONS (top level, score
absent)` rendering each weapon with each enchant its pools admit, at its top level, WHERE THE TOOLTIP MOVES: melee 8 x
Sharpness and the bottle's Sharpened Glass (9), ranger 9 x Power (9), mage 7 x Attunement (7), and Large Sparks, Sharp
Crystals, Echo Beam, Focused Beam, Lapis Crystal, Airburst Flames, Shatter Caps (7): **32 renderings**. Lines: 300 lore
lines at the bare counts (summed from the file) + 8 (Shatter 1, Airburst 1, Lapis's granted block 6) = 308, + 32
headers, + a blank and the section title, + the new tail: **+343 / -1**, and `=== 108 renderings ===` →
`=== 140 renderings ===`.

**Read:** `git diff --numstat` **343 1**, the tail `=== 140 renderings ===`, `REGENERATED` in the log, and the 32
headers exactly the predicted set. The bare Cursed Emerald still reads "three" (from the token).

## 11. TESTS AND MUTATIONS

New: `core/.../weapon/TrueTooltipsCoreTest` (16), `paper/.../weapon/TrueTooltipsConsistencyTest` (3),
`paper/.../weapon/TooltipRedrawSitesTest` (2), `paper/.../content/TrueTooltipsContentTest` (4). Changed:
`GoldenLoreTest` (the section), `FocusedBeamContentTest` (reads the FILLED sentence).

Sixteen mutations (the four batches ran 3 + 5 + 5 + 3; the table below has 16 rows), each a literal edit with a marker, applied to a pristine scratchpad copy's file, **marker present 1,
lines changed 1** against that copy, red, restored by `cp`, `cmp` identical, **markers left 0**. Two (MUTTOKEN,
MUTFOCUS) insert text before the original, so their "original gone" reads 1 by construction (the mutation-lies table's
eighth row); the line delta is their instrument.

| mutation | the edit | reddened |
|---|---|---|
| MUTPCT | the item's damage percent dropped from `itemDamage` | T13, 162 lines |
| MUTTUNE | the tooltip skips `CastTunes.tune` | T13 (18 lines: Large Sparks, Sharp Crystals, Echo Beam x 3 levels x 2 scores), the control, the examples |
| MUTCRIT | the crit enchant's contribution x 0 | T13 (6), the examples |
| MUTTOKEN | `fill` returns the line unfilled | `TrueTooltipsCoreTest` (core; the reactor stopped there) |
| MUTFOCUS | the Focused Beam arm yields nothing | T13 (6), the examples |
| MUTSHATTER | the stone's damage + 1 | T13 (6) |
| MUTBURST | the bursts' damage + 1 | T13 (6) |
| MUTORB | the crystal's shot x 2 | T13 (6) |
| MUTPROT | armor's Protection points 0 | T13 (168), the examples |
| MUTSHIELD | the shield's Bulwark 0 | T13 (6) |
| MUTREADER | the reader hands no behaviours | T13 (42 = 7 behaviour enchants x 3 x 2), the control, the examples |
| MUTPROSE | the reader drops the YAML prose | `GoldenLoreTest`, the examples |
| MUTREDRAW | the roll's write left without its redraw | `TooltipRedrawSitesTest` |
| MUTREPLACE | `BehaviourCasts.replaceDamage` + 1 | the EXECUTOR's tests: `FocusedBeamTest`, `ShatterCapsTest`, `AirburstTest` (13 rows) |
| MUTORBAMT | `orbShotAmount` / 50 | the EXECUTOR's `OrbBeamTest` (7 rows) |
| MUTROUND | `damageNumber` unrounded | `TrueTooltipsCoreTest.damageNumberRoundsAwayFloatingPointNoiseOnly` |

## 12. CC'S CALLS

1. **Two-decimal display rounding** (`WeaponLoreLines.damageNumber`). `17 x 1.15` is `19.549999999999997` in a double;
   the tooltip prints `19.55`. The alternative, printing the double whole as `trimNumber` does, shows the noise.
   `33.25` still prints exact. OPEN FOR BEN.
2. **Shatter Caps and Airburst Flames show TWO damage lines**, both `Kinetic`/`Fire Damage:` (the stone, then the
   chunks; the flame, then the bursts), told apart by the prose above them. The alternative, a per-line label authored
   in YAML ("Stone Damage"), adds a schema key. OPEN FOR BEN.
3. **The Lapis Crystal's shot** is a plain `Kinetic Damage:` line in the granted block; its "beam" is the staff's first
   own ray trigger (the right click). The alternative names the shot in its label. OPEN FOR BEN.
4. **`ability_description:` is an enchant-level key** (not inside the behaviour block), refused on a stat enchant and as
   a scalar. The alternative, a key inside each behaviour block, would need four parsers.
5. **The behaviour enchants' prose:** Lapis Crystal authors none (its own ability prose stands); the other three author
   placeholders (section 13).
6. **The split:** the executor extraction is its own PR, a pure refactor its own tests cover, so the seat can diff it
   apart from the tooltips.

## 13. OPEN FOR BEN (SEAT PLACEHOLDER 2026-11-26 unless dated otherwise)

1. Range, wind-up and reload: Ben, *"Leave them alone for now."* Polished Scepter, Early Toll and Deft Hands show only
   their enchant line.
2. The Knell Beam's description still reads *"then tolls a beam down it 1.2s later."* under Early Toll (D3).
3. The quiver line reads the stamp, which includes the wielder's own bonuses: the one deliberate exception to T2
   (T10 withdrawn).
4. Focused Beam's prose: *"Channel, then loose one focused / beam down your line of sight."*
5. Shatter Caps' prose: *"One stone that shatters into / {shots_word} chunks after 5 blocks."* ("four").
6. Airburst Flames' prose: *"A seeking flame that splits / into three after 6 blocks."*
7. CC'S CALLS 1 to 3 (section 12): the rounding, the two unlabelled damage lines, the crystal's shot line.
8. A broken armor piece's tooltip still shows its Defense, Health and Mana, which the engine does not grant while broken
   (today's behaviour, kept by T11).
