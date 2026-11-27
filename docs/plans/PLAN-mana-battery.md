# PLAN — MANA BATTERY, A FOURTH ARMOR STAT ENCHANT (2026-11-27)

**Status: BUILT, NOT BOOTED.** From the seat's brief of 2026-11-27, via Ben. Branch `feat/mana-battery`, from master
`07f03c6b` (tree `9620316c`). Its gate is `GATE-mana-battery.md`, at the top of the stack (`feat/mana-battery-gate`).

## BEN'S WORDS, VERBATIM, 2026-11-27

> "Let's do a new armor enchant, Mana Battery. Increases mana regen by 1/2/3 per 5 seconds"

The seat asked: per 5 seconds or per second; how it reads; which slot. Ben:

> "per second not 5 seconds, normal slot"

So, as first built: **+1 / +2 / +3 mana per SECOND** at I / II / III, in a NORMAL slot of any armor piece.
**SUPERSEDED the same day: Ben chose "B", per FIVE SECONDS. See the section below.**

## DATED NOTE, 2026-11-27 — BEN CHOSE "B": PER FIVE SECONDS (the seat's C1 to C6)

The seat owned its error: the brief's B5 described the sheet as per second; it shows per five seconds (READ below).
Ben was then shown both readings in the sheet's own unit (A as built: the sheet +5 / +10 / +15 per 5s; B his first
wording: the sheet +1 / +2 / +3 per 5s, the tooltip and the sheet reading the same number), and answered, verbatim:
**"B"**. The seat's rulings, as built:

- **C1:** `mana_battery.yml` keeps `value_by_level: [1, 2, 3]`, now mana per FIVE SECONDS (BEN). Its comment carries
  his three statements in order, what the numbers do, and that the gear score does not move them.
- **C2: THE CONVERSION HAS ONE HOME.** `ManaRegen.perSecondFromPerFiveSeconds(v) = v / StatsSheetLines
  .RATE_WINDOW_SECONDS`, the sheet's own conversion inverted over the sheet's own window; `ManaBatteryModifierItems`
  calls it before `ManaRegen.contribution`. No literal 5 or 0.2 anywhere else. The result is per second and composes in
  ticks through `perTick`, as every source does. **The chain, EXECUTED (`ManaBatteryRefillTest.theChain_authoredToTheSheet`),
  authored -> stat per second -> pool per tick -> the sheet's printed figure:**

  | worn | stat /s | per tick | sheet raw | printed |
  |---|---|---|---|---|
  | nothing | 0.0 | 0.05 | 5.0 | `5.00/5s` |
  | I | 0.2 | 0.060000000000000005 | 6.000000000000001 | `6.00/5s` |
  | II | 0.4 | 0.07 | 7.000000000000001 | `7.00/5s` |
  | III | 0.6 | 0.08 | 8.0 | `8.00/5s` |
  | III + II | 1.0 | 0.1 | 10.0 | `10.00/5s` |
  | III x 4 | 2.4 | 0.16999999999999998 | 16.999999999999996 | `17.00/5s` |
  | III + II + the golden set's 2/s | 3.0 | 0.2 | 20.0 | `20.00/5s` |

  **Every printed figure is exactly +1.00 / +2.00 / +3.00 per level and the sums exact; none is off by a rounding
  step** (three raw values sit one ULP off, which two decimals never show).
- **C3: THE UNIT IS THE EFFECT'S.** Every enchant with `effect: mana_regen` is authored per five seconds
  (`EnchantEffect.MANA_REGEN`'s javadoc, the conversion's). The golden set's `mana_regen` and the accessories' stay per
  SECOND, unchanged (BEN: "this is every second"). **OPEN FOR BEN: the golden set's tooltip says `+2 Mana Regen per
  second` while the sheet moves by 10 (`5.00/5s` -> `15.00/5s`).**
- **C4:** the tooltip reads the authored number, now the number the sheet moves by: `Mana Regen: +3` and `(+3 Mana
  Regen)`, as built. Wording SEAT PLACEHOLDER; **whether to show "/5s" is OPEN FOR BEN.**
- **C5:** `GATE-mana-battery.md` re-predicted in place (never approved). MB3, EXECUTED through the pool's own functions
  (`mb3_theRefillOverAHundredTicks`): 10 + 100 x (0.05 + `ManaRegen.perTick(0.6)` = 0.03) = **18.000**; the control 15.000.
- **C6:** T13's armor expectation is now the engine's per-second modifier over the sheet's window (the number the line
  shows); its count does not move (1476). The golden renderings do not move (predicted: no armor piece in
  `golden-lore.txt` carries an enchant; read: the file is unmodified after the suite). One new mutation: the conversion
  skipped.

## A READ THAT DISAGREES WITH THE BRIEF'S WORDING (reported, not a stop: no ruling's content moves)

> **RESOLVED 2026-11-27: Ben chose "B" (the dated note above).** Kept as the record of what was built first; the
> `+15.00/5s` below is the "A" reading and no longer what ships.

B5 says *"The stats sheet's Mana Regen line shows the total per second, as today."* **READ: it shows it per FIVE
seconds.** `StatsSheet` renders the line with `StatsSheetLines.perFiveSeconds(v.manaRegenPerSecond())`, `RATE_WINDOW_SECONDS
= 5`; `GATE-armor-sets.md` read the base as `Mana Regen 5.00/5s`. The STAT is per second (the seat's READ is right
there), and the golden set's tooltip says `+2 Mana Regen per second`. Nothing here changes the sheet (B5: "as today"), so
**Mana Battery III shows on the sheet as `+15.00/5s`** (3/s x 5) while its tooltip says `+3`. **OPEN FOR BEN**: whether
he wants that, since his "per second not 5 seconds" was answered against a description of the sheet that was not what it
shows.

## WHAT WAS READ ON MASTER (each confirmed)

- Three armor-only stat enchants: `protection` (`effect: defense`, `[3, 6, 9]`), `growth` (`max_health`, `[10, 20,
  30]`), `mana_bank` (`max_mana`, `[10, 20, 30]`), each `class: armor`. `unbreaking` is the universal one.
- `EnchantDefinition`'s Gate switch holds `DEFENSE, MAX_HEALTH, MAX_MANA -> Gate.ARMOR_ONLY`; its `curved` switch lists
  all three `true`.
- The mana-regen stat: `HealthState.manaRegenBonus = new Stat(0.0)`, per SECOND, gear-contributed. The pool's resolver
  (`RpgPlugin`) is `MANA_PER_TICK + ManaRegen.perTick(stats.manaRegenBonusValue(owner))`, `MANA_PER_TICK = 100.0 / (100
  * 20)` = 0.05/tick = 1.0/s. `ManaRegen.contribution` is the one seam a mana-regen bonus becomes a modifier.
- **The stat is ADDITIVE:** `Stat.value()` is base plus the sum of its modifiers (READ, `Stat.value()`: `base` plus each modifier), and every source enters
  under its own key through ONE reconcile (`ManaTransition.reconcile`'s `desiredRegen`), which today merges the
  `mana_regen_boost_TEMP` fixture, the armor set's `MANA_REGEN` (golden's +2), the accessories' and the fragments'.

## B4: WHAT "CURVED" DOES — NOTHING TO DO WITH GEAR SCORE

`curved` in `EnchantDefinition` means only **"this effect HAS a `value_by_level`"**: it routes the file to
`requireCurve` (size equals `max_level`, no negative, no null) rather than `requireNoCurve`. The value a piece grants is
`EnchantValues.totalFor` -> `EnchantCurve.valueAt(valueByLevel, level)`: **a list index by level, and nothing else.**
No gear score, no rarity, no tier enters it. `DefenseModifierItems` says so where the question would be asked: the
piece's score multiplies its AUTHORED Defense and **never an enchant's points**.

| | score 100 | score 200 |
|---|---|---|
| Mana Bank III | **+30** max mana | **+30** max mana |
| Mana Battery I / II / III | **+1 / +2 / +3** mana per 5 s (C1; was "/s") | **+1 / +2 / +3** mana per 5 s |

**The number does not move with score**, so it is not listed OPEN FOR BEN on that ground. Pinned by
`ManaBatteryTest.theValueDoesNotMoveWithTheScore` (the piece's score written at 100 and 200, the scan's sum read).

## WHAT WAS BUILT (B1 to B6)

- **B1, `content/enchants/mana_battery.yml`:** `display_name "Mana Battery"`, `icon: redstone_block` (SEAT
  PLACEHOLDER 2026-11-27), `max_level 3`, `effect: mana_regen`, `class: armor`, `value_by_level [1, 2, 3]` (BEN). No
  `armor_slot:`, no `element:`, no `weapon:`, not guaranteed: so it rolls in armor's NORMAL slots (2 and 3) only.
- **B2, `EnchantEffect.MANA_REGEN`:** held to `Gate.ARMOR_ONLY` beside the other three, `curved` true, no behaviour.
  Its mechanism is `ManaRegen` (`boosts`, `contribution`), which existed: like `QUIVER_SIZE`, an existing mechanism
  gaining its first enchant, so no new core class.
- **B2, the scan, `ManaBatteryModifierItems`:** the four `DefenseModifierItems.ARMOR_SLOTS`, each worn piece's
  `EnchantValues.totalFor(..., MANA_REGEN)`, a BROKEN piece skipped (`ArmorDurability.isBroken`, as Mana Bank's D3'), a
  piece merely carried never read (it walks the armor slots only), absent-not-zeroed, gated on `ManaRegen.boosts`.
  Key `manabattery:<SLOT>`.
- **B3, one path:** `PlayerHealthSystem` merges it into the FIRST argument of the regen map beside the TEMP fixture
  with `AccessoryContributions.merged`, so it reaches `ManaTransition.reconcile`'s one `reconcileManaRegenModifiers`
  call. No second reconcile, no new clock; the resolver adds `ManaRegen.perTick(bonus)` to the per-tick base (composed
  in ticks, as `ManaRegen`'s javadoc rules).
- **B5, the tooltip, as Mana Bank's:** the enchant block's `Mana Battery III` (EnchantLore, unchanged), and one
  stat-block line under Defense, `Mana Regen: +3` (blue, `StatsBarText.MANA_COLOR`), through the same `addBonus` Mana
  Bank's `Mana: +30` uses; label `ArmorLoreLines.MANA_REGEN_LABEL = "Mana Regen"`. `EnchantEffectLine`'s suffix (the
  enchant table and `/rpg enchant show`) reads `+3 Mana Regen`. **Both wordings SEAT PLACEHOLDER 2026-11-27, OPEN FOR
  BEN** (neither names the per-second unit).
- **B6, pools:** armor's normal pool goes from four to FIVE: `growth, mana_bank, mana_battery, protection,
  unbreaking` (file-name order: `EnchantLoader` sorts the files). Slots 0 and 1 unchanged (empty). The cascade's
  largest tier is 4 (`enchant_candidates.yml`), so a pool of four already filled every tier: **the slot tallies'
  distribution does not change**; only `normalPool=` and which ids can appear do.

## CC'S CALLS

1. **`rpg mana set <amount>` (dev),** so a gate row stages his mana low by one command: `ResourcePool.setCurrent`, the
   same call the reconcile's pin makes, so it stamps the tick it runs in. The alternative: cast a costed staff to spend
   it, which needs a target, a cooldown and a cost read.
2. **The stat-block line is added** (as Mana Bank has one), not only the effect suffix; the alternative is the enchant
   line alone, which would be the only armor stat enchant with no stat-block line.
3. **The TEMP fixture stays** (`ManaRegenModifierItems`'s javadoc says it comes out "when a real mana-regen enchant
   lands"). Removing it changes `/rpg manaregen` and older gates; it belongs to the content pass that removes the other
   `_TEMP` fixtures. Its javadoc now says the enchant has landed and the fixture is owed removal.
