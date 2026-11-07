# PLAN — DEFT HANDS: A RANGER RELOAD-SPEED ENCHANT (PHASE 1 SURVEY)

**Phase 1: SURVEY ONLY.** No Java, no content, no boot. Read against **master `cd790dcc`**. The stack top
(`feat/sunder` `615a0d18`, PR #178) differs from master only in the three weapons named in section 2. Citations
name a CLASS and a METHOD or a SECTION, never a line. **READ** = read in source at that ref; **EXECUTED** = run in
`jshell` (openjdk-26.0.1); *UNVERIFIED* = a vanilla or Paper behaviour not read.

**Ben, 2026-11-06, verbatim (via the seat):** *"I'd like to pivot to adding a new Enchantment, Deft hands, increases
reload speed by 10/20/30% Ranger weapon exclusive"*

**Every number here is Ben's to rule.** Section 4 carries his open question undecided.

---

## 1. THE MECHANISM — `core/combat/ReloadTime` (READ)

**What it is.** A FLAT, WHOLE-TICK bonus added to a weapon's authored `reload_ticks`. It is `QuiverSize`'s
sibling, and its sign is inverted: positive means a SLOWER reload.
- `ReloadTime.resolve(authoredTicks, bonusStatValue)` = `authoredTicks + ticks(bonusStatValue)`.
- **No floor and no clamp.** A resolved 0 or below is an instant reload, and that is a reachable state (the class
  javadoc's table). The balance objection was explicitly left to "the first slice that ships reload-reduction gear".
  **This is that slice** (section 5).
- `ReloadTime.ticks(double)` is **FLOOR**. That is the house rule's *mode*. The class javadoc records that for this
  stat, floor rounds **in the player's favour**: `ticks(-2.1) = -3`, a shorter reload. *"Flooring rounds against the
  player"* is false here, and must not be copied.
- `ReloadTime.declares(int)` is **`!= NONE`**, deliberately NOT `> NONE` and NOT named `boosts`. A helpful reload
  item carries a NEGATIVE amount, so `> NONE` would declare nothing. **Copy `declares`, not `QuiverSize.boosts`.**

**What resolves it today.** It runs ONCE per reload, at the start: `Quivers.beginReload` calls
`ReloadTime.resolve(weapon.reloadTicks(), stats.reloadTimeBonusValue(player))` and stamps the deadline from that.
- Two read-outs show the same figure: `StatsSheetProjection` (the stats sheet's `Reload` line) and `/rpg reloadtime`.
- **The only thing feeding the stat today is the dev instrument**, `ReloadTimeModifierItems`: a CLOCK carrying
  `keys.reloadTimeBoost`, scanned on every equipment slot into `HealthState.reloadTimeBonus` under `reloadtime:<slot>`.
- **No enchant feeds it.**

**Can an enchant bind to it already? NO.** `EnchantEffect` is `DURABILITY, DAMAGE, BLOCK_DR, REFLECT, DEFENSE,
MAX_HEALTH, MAX_MANA, QUIVER_SIZE`, with no `RELOAD_TIME`. A file with `effect: reload_time` is refused as an unknown
effect: the loader's warning lists the eight. **Adding one means:**
- `EnchantEffect.RELOAD_TIME`, plus its arms in `EnchantDefinition`'s gate switch, its `curved` switch and
  `EnchantEffectLine`'s switch. **The compiler names all three**, because they are exhaustive expression switches
  (`PLAN-enchants-ranged.md` §6 records the same compile error doing the guarding for `QUIVER_SIZE`).
- A scanner, `DeftHandsModifierItems`, shaped like `ExpandedQuiverModifierItems`. It reads the MAIN HAND's enchants
  through `EnchantValues.totalFor(…, RELOAD_TIME)` and puts one `defthands:HAND` modifier into the SAME
  `reloadTimeBonus` stat.

**How a PERCENT tier becomes ticks — the shape proposed, not ruled.** The stat is flat ticks, so the percent must
become ticks against something. **Against the HELD weapon's AUTHORED `reload_ticks`, in the scanner, ONCE:**

```
contribution = Math.floorDiv(-authoredReload * pct, 100)      // (a); pct = 10, 20, 30 from value_by_level
```

- **ONE rounding, and it is `ReloadTime`'s mode (floor).** The scanner hands `contribution` (an int) to the stat.
  `ticks()` then floors an integral double, which is exact (its javadoc).
- **INTEGER arithmetic, not `pct / 100.0`.** EXECUTED: `34 * 0.1` is `3.4000000000000004` in `double`. All 18 points
  below happen to floor the same either way, so this is not a live defect today. But a curve value whose product
  lands on an integer would floor one tick too far through a `double`. `floorDiv` cannot.
- **A percent of AUTHORED, not of RESOLVED.** It does not compound with the flat dev instrument or any future flat
  source: they add (section 5).

## 2. NAME THE QUANTITY — every shipped weapon's `reload_ticks` (READ), and the tiers (EXECUTED)

**The quantity is the held weapon's authored `reload_ticks`, a whole number of ticks.**
- Every weapon at master and at the stack top was read with
  `grep -m1 -E '^\s*reload_ticks:' content/weapons/<w>.yml` (plus `class:` and `quiver_size:`).
- **Every weapon that reloads is `class: ranger`. Every ranger weapon reloads except `hunters_bow`.**

| weapon | class | `reload_ticks` | `quiver_size` | where |
|---|---|---|---|---|
| `boltor` | ranger | **60** | 8 | master |
| `locust` | ranger | **60** | 12 | master |
| `dragons_plume` | ranger | **60** | 25 | master |
| `dragons_breath` | ranger | **40** | 5 | master |
| `quiver_stone` | ranger | **34** | 9 | master (dev weapon, deletion set) |
| `short_bow` | ranger | **60** | 8 | stack (#171) |
| `withered_shortbow` | ranger | **60** | 8 | stack (#174) |
| `hunters_bow` | ranger | — | — | master (dev weapon, deletion set) |
| `cursed_emerald`, `ember_staff`, `flint_staff`, `lapis_staff`, `volley_stone` | mage | — | — | master |
| `blaze_kings_staff` | mage | — | — | stack (#172) |
| `emberblade`, `ironblade` | melee | — | — | master |

**So three authored values reach this enchant: 60, 40 and 34.** EXECUTED, with
`Math.floorDiv(-R * pct, 100)`; the `double` form gave the same tick at all 18 points:

| `reload_ticks` | 10% | 20% | 30% |
|---|---|---|---|
| 60 (five weapons) | −6 → **54** (2.70 s) | −12 → **48** (2.40 s) | −18 → **42** (2.10 s) |
| 40 (`dragons_breath`) | −4 → **36** (1.80 s) | −8 → **32** (1.60 s) | −12 → **28** (1.40 s) |
| 34 (`quiver_stone`) | −4 → **30** (1.50 s) | −7 → **27** (1.35 s) | −11 → **23** (1.15 s) |

**No tier rounds to zero change on any of them.** The smallest change is **−4 ticks**: 40 at 10%, and 34 at 10%,
where 3.4 floors to −4. **ARITHMETIC half: PASS.**

**One quantisation this table cannot see** (section 5, the input grid): the shortened reload is exact. But the first
shot after it waits for the next held right-click input, so a −4-tick saving may read as less in play. That is a
boot reading, not a rounding question.

## 3. ELIGIBILITY — `class: ranger`, and the weapon must HAVE a reload

**The class half: reuse `Gate.RANGER_ONLY`** (READ, `EnchantDefinition`'s gate switch and `requireGate`). It admits
`class: ranger` only and refuses `universal` by name. A universal reload enchant would enter every sword's pool,
inert, which is the dangerous typo `expanded_quiver.yml` warns about.

**The "has a reload" half. A CORRECTION TO THE BRIEF:** expanded_quiver does **NOT** keep a no-magazine ranger weapon
out of its roll pool or the enchant table. Measured:
- `EnchantRoll.poolFor(heldClass, roster)` filters on `gearClass` ALONE. The roster is `(id, gearClass)` pairs, with
  no weapon in it.
- The enchant table shows each item's ROLLED candidate slots (`EnchantMenu.renderCandidates` reads `EnchantSlot.candidates()`), which were drawn at acquire from that same class-only pool.
- `PLAN-enchants-ranged.md` §6.1 records this as the fourth arrival of the NAME-THE-QUANTITY defect, *"through the
  ROLL TABLE"*, and why each runtime home (`EnchantRoll.roll`, `EnchantEffectLine.bare`,
  `ContentValidator.validateEnchants`) cannot see a weapon.

**What expanded_quiver ships instead is a BUILD-TIME content invariant:**
- `ExpandedQuiverContentInvariantTest.everyRangerWeaponAuthorsAMagazineOrIsAnExemptDevWeapon`.
- `EXEMPT_DEV_WEAPONS = List.of("hunters_bow")`, pinned by `theExemptionSetHoldsExactlyTheOneDevWeaponItWasWrittenFor`.
- Its reachability control is `theWeaponContentIsREACHABLEAndHoldsTheRangerWeaponsExpected`.
- **`hunters_bow` can still roll Expanded Quiver today, and gets nothing.** That is accepted because it is a dev weapon
  in the deletion set.

**For reload, the same invariant already IS the gate, because magazine ⇔ reload is enforced in BOTH directions**
(`WeaponDefinition`'s constructor, pinned by `WeaponQuiverDefinitionTest.aReloadWithoutAQuiverIsRefused` and
`aQuiverWithoutAReloadIsRefused`). "Every ranger weapon has a magazine" therefore implies "every ranger weapon has a
reload". **Proposed:**
- A sibling invariant, `DeftHandsContentInvariantTest`, asserting `reload_ticks` directly. It does not lean on the
  ⇔, so a future relaxation of that rule cannot silently open the gate.
- The same one exemption (`hunters_bow`), and its own reachability control.

**`volley_stone` is `class: mage`** (READ), so it never enters a ranger pool. Nothing to exempt.

**The sign the brief asked about:** the SCANNER gates on `ReloadTime.declares(contribution)`, which is `!= NONE`. For
this enchant every contribution is NEGATIVE (−4 to −18), so `> NONE` (`QuiverSize.boosts`'s operator) would make the
enchant **declare nothing and do nothing, silently**. That is exactly the defect `ReloadTime.declares`' javadoc
records. **A unit row must pin a negative contribution being declared**, with a mutation to `> NONE` that reddens it.

**An eligibility hole the invariant does NOT cover** (the Punch shape): a ranger weapon added later with no
magazine, if it is ever not a dev weapon. The invariant reddens the build the day it lands. That is the intended
behaviour, and the fix is then a deliberate choice, as `ExpandedQuiverContentInvariantTest`'s javadoc says.

## 4. BEN'S OPEN QUESTION — CARRIED VERBATIM, NOT DECIDED

*"increases reload speed by 10/20/30%"* reads two ways:

- **(a) reload TIME × 0.9 / 0.8 / 0.7.** Contribution `floorDiv(-R × pct, 100)`; section 2's table, repeated:

  | `reload_ticks` | I | II | III |
  |---|---|---|---|
  | 60 | 54 (−10%) | 48 (−20%) | 42 (−30%) |
  | 40 | 36 | 32 | 28 |
  | 34 | 30 | 27 | 23 |

- **(b) reload SPEED × 1.1 / 1.2 / 1.3, so time ÷ 1.1 / 1.2 / 1.3.** Contribution `floorDiv(R × 100, 100 + pct) − R`
  (time floored, the house mode). EXECUTED:

  | `reload_ticks` | I | II | III |
  |---|---|---|---|
  | 60 | 54 (−9.1%) | 50 (−16.7%) | 46 (−23.3%) |
  | 40 | 36 | 33 | 30 |
  | 34 | 30 | 28 | 26 |

- **They agree at level I and diverge after.** At III, (a) takes 18 ticks off a 60-tick reload and (b) takes 14.
  **The seat recommends (a):** the tooltip's "30%" is then the share of the wait removed, which is what a player
  times. **Ben rules.** Either form passes section 2's no-zero check (min −4 under both).

## 5. WHERE IT TOUCHES EVERYTHING ELSE (READ unless marked)

- **Enchant table: cost and XP.** `EnchantCost.xpPoints(targetLevel, bookshelfPower)` is per LEVEL, not per enchant,
  so Deft Hands is priced like every other level-I/II/III enchant with no new numbers. **Max level 3** (Ben's three
  tiers). `value_by_level: [10, 20, 30]`, curved, so `requireCurve` applies (size = `max_level`).
- **The roll pool.** It enters every RANGER weapon's pool by class (section 3), including `hunters_bow`, where it is
  inert, accepted as for Expanded Quiver. **A second ranger enchant changes the ranger roll odds:** the pool goes
  from {universal + `expanded_quiver`} to {universal + both}. That changes the odds for every ranger roll, though no
  number changes.
- **Anvil.** It moves the gear SCORE, not enchants (`AnvilMenu`, F3's confirm lore in `PLAN-nexus-polish.md`).
  *Not re-read for enchant transfer*: the build slice confirms it, or records otherwise.
- **Grindstone.** `GrindstoneRefund` is generic over `EnchantState` (`REFUND_PERCENT` 35 of the list spend), so Deft
  Hands refunds like any enchant. No change.
- **The lore line.** A new `EnchantEffectLine` arm. **Proposed:** `-10% Reload Time` / `-20%` / `-30%` under (a), or
  `+10% Reload Speed` under (b).
  - "Reload" is the word the stats sheet already uses (`StatsSheetLines.RELOAD_TIME_LABEL = "Reload"`), and
    `EnchantEffectLine`'s quiver arm sets the rule that an enchant names its stat as the item already does.
  - The off-class arm renders `inert: …`, as `QUIVER_SIZE`'s does.
  - **The wording is Ben's.**
- **The golden-lore file: NO CHANGE.** `GoldenLoreTest` renders WEAPON, SHIELD and ARMOR *definitions*, and its
  `=== LORE LINES ===` block holds armor and shield labels only. `grep -ci 'enchant|quiver arrow' golden-lore.txt`
  is 0, so Expanded Quiver never reached it either. The enchant line's pin is `EnchantEffectLineTest`, as the
  quiver arm's is.
- **Stacking with other reload sources.** The stat is a flat sum. Today it holds only the dev clock instrument; the
  enchant adds a second source under its own key (`defthands:HAND`), and the two ADD. **A floor becomes reachable for
  the first time:** with the instrument at −30 and Deft III on a 34-tick reload, the reload resolves to 34 − 11 − 30 = −7, an
  instant reload. `ReloadTime`'s javadoc deferred that balance ruling to this slice. **The seat should rule** whether a floor
  is now needed. With shipped content alone (no instrument), the shortest resolved reload is 23 ticks, so no floor is
  needed for content.
- **Q7, and the reload cue and sweep.** No change. `Quivers.beginReload` resolves the duration ONCE, and
  `QuiverSweep.show` and `QuiverReloadCue.schedule` are both handed that post-modifier `reloadTicks`. Their javadocs
  say a reload-time bonus is heard on time.
  - **The held right-click 4-tick grid governs FIRE cadence, not the reload deadline**, which is an exact tick.
  - **But maturity is evaluated on READ** (`QuiverState`, through `Quiver.reloadComplete`). INFERRED: the first shot
    after maturity waits for the next held input, so a saving is felt quantised by input arrival. The gate measures
    it (DH4) rather than predicting it.
- **The R0 Loaded line.** `10 enchants` becomes **`11 enchants`**. `EnchantLoaderTest` derives its count from the
  shipped directory, so it needs no edit.

## 6. DRAFT GATE ROWS — `GATE-deft-hands.md`, ALL SURVIVAL

**Display name: `Deft Hands`** (Ben's). **Icon: PROPOSED `feather`** ("deft", light hands); the alternative is
`clock`, the dev reload instrument's material. **Ben's.** The enchant id would be `deft_hands`.

| row | witness | prediction (under (a); re-predicted if Ben rules (b)) | selftest? |
|---|---|---|---|
| R0 | — | moves to this gate if it is the new stack top: jar has `content/enchants/deft_hands.yml` and the scanner class; Loaded line `11 enchants` | n/a |
| DH1 | AUTO-PARTIAL | the `/rpg enchant` dev subcommands (`candidate <slot> <enchant>`, `level <slot> <candidate> <level>`, `active <slot> <candidate>`) put Deft Hands III on a held Boltor: the held lore's enchant line reads `-30% Reload Time`; the stats sheet's `Reload` reads `2.1s` (42 t) | yes: `cmd` + `dump: held`; the stats sheet is Ben's glance |
| DH2 | AUTO | the same Boltor: empty the magazine, reload. The reload completes **42 ticks** after it starts. Witness: a reload trace line, *if one exists*. **None does today**, so the build adds one (`RELOAD start=<t> completesAt=<t> ticks=42`) beside `LANDING`'s pattern, or DH2 is Ben's stopwatch | yes, once the line exists |
| DH3 | AUTO | levels I and II on a Boltor read 54 and 48; Deft I on `dragons_breath` reads 36 (the 40-tick base); on `quiver_stone` 30 (the −3.4 → −4 floor) | yes |
| DH4 | AUTO-PARTIAL | held right-click through a reload on the Deft III Boltor: the first shot after the reload lands no earlier than 42 t, and within one input spacing of it. **The gap is RECORDED, not predicted** (section 5) | yes (`use` in a loop); the feel is Ben's |
| DH5 | AUTO-PARTIAL | eligibility, by POOL, not by one roll. Candidates are drawn at random at acquire, so one Boltor showing Deft Hands is NOT predicted. Predicted: across the selftest's gives, no `ember_staff` (mage) or `emberblade` (melee) candidate list ever names Deft Hands. Pool membership itself (ranger in, mage and melee out) is the unit row on `EnchantRoll.poolFor` | yes: `rpg give` + `dump: held`, repeated; the in-pool half is the unit row |
| DH6 | AUTO | stacking: Deft III plus `/rpg reloadtime +14`: resolved 42 + 14 = **56**, in `/rpg reloadtime`'s printed verdict. Additive, not compounding | yes |
| DH7 | BEN | the cue: the reload sound plays at the shortened completion, not the authored one | no: a sound is the client's |
| DH8 | AUTO-PARTIAL | grindstone: strip a Deft III Boltor. The refund equals any level-III enchant's (`GrindstoneRefund.points`), and the stats sheet's Reload returns to 3.0s | yes for the refund line; the sheet is Ben's glance |

**Unit rows the build writes first:**
- the scanner's contribution at all 18 points, executed rather than characterised;
- `declares` admits a negative contribution, with the `> NONE` mutation;
- `DeftHandsContentInvariantTest` with its control;
- the loader refusing `class: universal` and `class: mage` for `effect: reload_time`;
- the lore line at I/II/III and its `inert:` arm.

## STACK PLAN

**The build is ONE PR, stacked on the current stack top, `feat/sunder` (#178, `615a0d18`).**
- **It overlaps Sunder in only three places, all mechanical:** `selftest/index.yml` (both add a gate),
  `SelfTestScenariosTest`'s expected map and row count, and the cumulative R0 (it moves from `GATE-sunder.md` to
  `GATE-deft-hands.md`).
- **No source file is shared:** Sunder touches dash, landing and ability code; Deft Hands touches enchant, quiver and
  stat code.
- **Sunder is PARKED.** If the seat wants Deft Hands to merge first, the cheaper base is `feat/selftest` (#175), and
  the two would rebase across each other on those three files only. **The seat picks; the default is the stack top.**

## QUESTIONS FOR BEN

1. **"10/20/30%": does it cut the reload TIME by that much (a: 3.0s → 2.1s at III), or make the reload that much
   FASTER (b: 3.0s → 2.3s at III)?** The seat recommends (a).
2. **The tooltip words:** `-30% Reload Time`, or something else?
3. **The icon:** a feather (proposed), a clock, or another item?
4. **Should it also be allowed on the dev bow `hunters_bow`,** which has no reload and would show the enchant while
   it does nothing? Expanded Quiver already does this there today.
5. **With other reload gear stacked on top, a reload could become instant.** Is that fine, or should there be a
   shortest-possible reload?
