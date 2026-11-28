# GATE — REGEN TOOLTIPS IN THE SHEET'S UNIT (U1 TO U7), RE-READ BY REFERENCE (2026-11-27)

**Status: RUN 2026-11-27 on `01be9d3b`, the next stack's boot; see READINGS (all three as predicted).** The plan is
`docs/plans/PLAN-mana-battery.md`, *DATED NOTE, 2026-11-27 — REGEN TOOLTIPS IN THE SHEET'S UNIT*, and W4 is
`docs/plans/PLAN-owed-fixes.md`, its last dated note. The branch is `feat/regen-tooltips-sheet-unit`, from master
`16713667`. **Every prediction below was written before any boot, and is frozen from the seat's approval.** No boot
without Ben's own "ready" and the seat's approval of the code AND this gate from origin.

**THIS GATE HAS NO ROWS OF ITS OWN.** It re-reads, by gate.ROW reference, the three shipped rows that already dump these
tooltips: `armor-sets.AS1`, `armor-sets.AS2` and `mana-battery.MB1`. **The gates they belong to are frozen and not
edited;** their moved predictions are HERE, under their gate.ROW names (the seat's form for a re-read row). Every line
of those rows not named below reads as its own gate predicts it.

**GAME MODE: SURVIVAL**, as both referenced gates declare for these rows.

## WHAT BEN TYPES — ONE COMMAND, ON THE NEXT STACK'S BOOT

```
/rpg selftest armor-sets.AS1.AS2+mana-battery.MB1
```

- armor-sets' SETUP, then AS1 and AS2; then mana-battery's SETUP, then MB1. The queue, scenario by scenario, is
  `RegenTooltipsGateCommandTest`: 5 scenarios. **Not refused by V7 (e):** both setups begin with `pad:` (asserted).
- **He answers NOTHING.** No row asks (asserted over the queue).
- He types it on the pad's server, standing anywhere; the first setup takes him to the pad.
- It may be typed on its own, or after the next stack's own command, as that gate's run sheet says.

**How long it runs (an ESTIMATE from the scenarios' `wait:` steps, not a measurement):** the queued waits sum to **226
ticks (11.3 s)**: armor-sets' setup 40, AS1 56, AS2 56; mana-battery's setup 40, MB1 34 (summed by `awk` over each
row's `- wait:` lines in the two scenario files, 2026-11-27). With the row-start CLEANs, the pad steps and MB1's two
500-roll counts, **well under a minute, with no answer to give.**

## SAFETY — 0.000 FOR THE WHOLE COMMAND

No step deals him damage: no `rpg damage`, no `damage`, no mob is summoned (all three counted over the queue in
`RegenTooltipsGateCommandTest`). armor-sets' one damaging row, AS9, is not queued. **The sum is 0.000**, and it has no
term to execute (S-1 to S-3 apply to a sum with a term).

## R0 — THE NEXT STACK'S R0, PLUS TWO SYMBOLS

This gate's rows are readable only on a jar that carries this branch. The booting gate's own R0 stands; **its R0b adds
two symbol checks** on the DEPLOYED jar (a class's bytes, not an mtime):

- `io/github/butterflysmp/rpg/core/combat/StatsSheetLines.class` contains `perFiveSecondsBonus`;
- `io/github/butterflysmp/rpg/paper/menu/EquipmentMenu.class` contains `ACCESSORY menu `.

**R0d's scenario count does not move for this branch:** it adds no scenario and no gate.

## THE MOVED PREDICTIONS, BY gate.ROW

All three EXECUTED and pinned before any boot: AS1 and AS2 by `RegenTooltipsGateArithmeticTest`, MB1 by
`ManaBatteryGateArithmeticTest.mb1_theFourTooltipsWhole`.

### RT1 = armor-sets.AS1 — THE FOUR LEATHER TOOLTIPS

Each `dump: held` block, whole (score 100, bare):

- **`leather_helmet`:** `Gear Score: 100`, `Defense: 5`, `""`, `Set Bonus (4 pieces):`, **`+2 Health Regen/5s`**, `""`,
  `Better than nothing, and it knows it.`, `""`, `Common Helmet`.
- **`leather_chestplate`:** the same with `Defense: 8`, `Common Chestplate`. **`leather_leggings`:** `Defense: 7`,
  `Common Leggings`. **`leather_boots`:** `Defense: 4`, `Common Boots`.

**Moved:** `+2 Health Regen per 5s` (GATE-armor-sets.md's table) → **`+2 Health Regen/5s`**. **Not moved:** the row's
sheet readings, `Health Regen 3.00/5s` with the full set from `1.00/5s` bare: the engine is unchanged (U3), and the
tooltip's 2 IS that `3.00 − 1.00`.

### RT2 = armor-sets.AS2 — THE FOUR GOLDEN TOOLTIPS

- **`golden_helmet`:** `Gear Score: 100`, `Defense: 5`, `""`, `Set Bonus (4 pieces):`, **`+10 Mana Regen/5s`**, `""`,
  `It buys goodwill from things that cannot spend it.`, `""`, `Common Helmet`.
- **`golden_chestplate`:** `Defense: 8`, `Common Chestplate`. **`golden_leggings`:** `Defense: 7`, `Common Leggings`.
  **`golden_boots`:** `Defense: 4`, `Common Boots`.

**Moved:** `+2 Mana Regen per second` → **`+10 Mana Regen/5s`**. **Not moved:** the full set's `Mana Regen 15.00/5s`
from `5.00/5s`, which is the tooltip's 10 (the strength is still 2 per second; Ben, 2026-11-27: *"The golden set should
also"*).

### RT3 = mana-battery.MB1 — THE FOUR IRON TOOLTIPS WITH MANA BATTERY III, AND THE ENCHANT SHOW

- **helmet:** `Mana Battery III`, `""`, `Gear Score: 100`, `Defense: 10`, **`Mana Regen: +3/5s`**, `""`, `Set Bonus (4
  pieces):`, `+100 Max Health`, `""`, `Honest plate. It has no opinion about who wears it.`, `""`, `Common Helmet`.
- **chestplate:** the same with `Defense: 16`, `Common Chestplate`. **leggings:** `Defense: 14`, `Common Leggings`.
  **boots:** `Defense: 8`, `Common Boots`.
- On the helmet, `rpg enchant show`: **`  active: mana_battery III (+3 Mana Regen/5s)`**; its other lines as
  GATE-mana-battery.md's MB1.

**Moved:** `Mana Regen: +3` → **`Mana Regen: +3/5s`**, and `(+3 Mana Regen)` → **`(+3 Mana Regen/5s)`** (Ben,
2026-11-27: *"Yes the mana battery's tooltip should read /5"*). The value stays mana blue. **Not moved:** the pool,
the roll bands and everything else MB1 reads.

## W4 — UNIT-PINNED, AND WHERE A LATER ROW READS IT

W4's two lines are not read by the three rows above (none of them probes mana or clicks the Equipment menu):

- `MANA current=<c> max=<m> tick=<t>`: any later row that `probe_mana`s prints it; `SelfTestManaProbeTest` pins it.
- `ACCESSORY menu equip|unequip player=<name> slot=<s> item=<id> tick=<t>`: written only by a real click in the
  Equipment menu, which the selftest's `accessory:` step does not make (that step has its own `STAGE accessory …`
  line). **UNREAD on any boot until Ben equips through the menu;** `EquipmentMenuAccessoryLogTest` pins it.

## READINGS — THE BOOT OF `01be9d3b`, 2026-11-27 (written after the run; no prediction above is edited)

The boot, R0, his ground and his build are `GATE-solar-flare.md`'s READINGS. No `HURT` line in these rows; he answered
nothing; the queue ran armor-sets setup, AS1, AS2, mana-battery setup, MB1 in that order.

| row | reading | class |
|---|---|---|
| RT1 (AS1) | The four leather tooltips exact, line for line (`Defense: 5 / 8 / 7 / 4`, `+2 Health Regen/5s`, `Common Helmet` …). Sheet `Health Regen 1.00/5s` bare, `3.00/5s` with the set (Defense 24), back to `1.00`. | READ AS PREDICTED |
| RT2 (AS2) | The four golden tooltips exact (`+10 Mana Regen/5s`, `It buys goodwill from things that cannot spend it.`). Sheet `Mana Regen 5.00/5s` → `15.00/5s` → `5.00`. | READ AS PREDICTED |
| RT3 (MB1) | The four iron tooltips exact, each `Mana Battery III`, `Mana Regen: +3/5s` `[gray, blue]` (the value mana blue). `enchant show`: `  active: mana_battery III (+3 Mana Regen/5s)`. | READ AS PREDICTED |

The run-length estimate (226 ticks) measured 260 (48 + 62 + 62 + 48 + 40). `MANA` and `ACCESSORY menu` lines: not in
these rows, as written above.
