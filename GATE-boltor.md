# GATE — the Boltor, the `>=` / `>` boundary at a cooldown on the 4-tick grid

**Status: NOT RUN.** **Every row below was written before any boot, and no reading has been taken.**
No figure in this file is an observation; the expected values are pre-recorded predictions, recorded
so that a later reading can disagree with them. When the row is run, the reading is written *beside*
the prediction and the prediction is not edited.

**This gate carries ONE row.** `PLAN-boltor.md:225` — *"the boundary row and only that"*. The
beam-density row, the elapsed figure and the `tuning:` marking are commits 3–5 and are not here.

---

## THE QUESTION

**Does a cooldown that is an exact multiple of 4 fire on its own tick, or on the next input?**

Q7 measured that a held right-click delivers an input only every **4 ticks**, so a weapon's real
interval is its authored cooldown **rounded up to the next input** (`GATE-q7.md`). Both weapons it
read were *off* the grid — `ceil(11/4)x4 = 12` and `ceil(15/4)x4 = 16` — and in both cases the round-up
lands on a strictly larger input, so **the boundary never arose**.

The Boltor is authored at **16**, which is on the grid. It is the first weapon in `content/` that can
answer this.

```
inputs at t = 0, 4, 8, 12, 16 …     fire at 0, cooldown 16 expires at t = 16
  elapsed >= cooldown  ->  the input at 16 finds it ready  ->  FIRES AT 16   (1.25 shots/s)
  elapsed >  cooldown  ->  16 is missed, next input at 20  ->  FIRES AT 20   (1.00 shots/s)
```

---

## ROW 1 — THE BOUNDARY READING

| | |
|---|---|
| **weapon** | `boltor` (`material: crossbow`, `cooldown_ticks: 16`, `quiver_size: 8`, `reload_ticks: 60`) |
| **do** | `/rpg firerate` **once to clear**, then hold right-click for one full magazine (8 shots), then `/rpg firerate` to read |
| **read** | the `FIRES` line: `count`, `window`, `mean`, `min` |
| **verdict** | `min 16` ⇒ the comparison is `>=`.  `min 20` ⇒ it is `>`. |

**THE CLEAR IS NOT OPTIONAL AND IT IS THE ROW'S ONLY CONTROL.** `/rpg firerate` **prints and clears**
(`RpgCommand.java:497`), so a run that was not preceded by a clear reports a sample mixed with
whatever fired before it — including the fires from walking to the test spot. The first invocation is
the control; its output is discarded.

**Read `min`, not `mean`.** Q7 established that the mean is a fact about the *input stream* and drifts
when inputs are not perfectly periodic — `hunters_bow` read `mean 19.43` against a true interval of
16. The minimum is the quantity the boundary changes.

### THE SOURCE PREDICTS 16, AND THAT IS STILL A PREDICTION

`CooldownTracker.isReady` is `currentTick.getAsLong() >= ready`
(`core/…/combat/CooldownTracker.java:35`) and `trigger` stamps `now + cooldownTicks`, so **reading the
code says 16.** That model is also **2-for-2** on the measured points (11→12, 15→16).

**It is recorded here as a prediction and not as an answer, for a stated reason:** neither measured
point could distinguish `>=` from `>`, because in both the round-up lands past the boundary either
way. And this whole question exists because a weapon authored at 15 did not fire at 15 — the last time
the authored number was read off the source and believed, it was wrong about the delivered interval.
**Read the source, then measure it.**

### THE EXPECTED SHAPE, WRITTEN IN ADVANCE

```
FIRES  count 8  mean 16.00  min 16      <- zero variance, the quiver_stone signature
```

`quiver_stone` read `mean 12.00, min 12` with **zero variance** because its inputs were perfectly
periodic on the same grid. The Boltor should do the same. **A mean between 16 and 20 is the
`hunters_bow` signature and means the input stream was not periodic — it is not a `>` reading**, and
the `min` is what settles it.

---

## WHAT THIS ROW DECIDES

### 1. WHETHER THE SHIPPED TOOLTIP LIES

`WeaponLoreLines.rangedAttackSpeedLabel` is `String.format("%.1f", 20.0 / cooldownTicks)` over the
**authored** cooldown, so it prints **"Attack Speed: 1.3"** for the Boltor either way —
`golden-lore.txt:15` records it.

```
on >=   the weapon delivers 1.25/s  ->  "1.3" is TRUE
on >    the weapon delivers 1.00/s  ->  "1.3" OVERSTATES IT BY 30%
```

**So this is not only a balance row.** On the `>` branch, the first Ranger weapon a player will ever
hold ships a stat line 30% higher than the weapon achieves, which is the class of defect this project
treats as worst.

Separately, and true on **either** branch: `20.0/15` and `20.0/16` both format to `"1.3"`, so
`golden-lore.txt:15` and `:76` are byte-identical and **the tooltip cannot distinguish a 15-tick
weapon from a 16-tick one.** The wider defect behind that — the formula never sees the quantisation at
all — is recorded at `WeaponLoader`'s `cooldown_ticks` section, not here.

### 2. EVERY CYCLE FIGURE IN `PLAN-boltor.md`

```
  >=   fires 16t  ->  cycle 172t / 8.60s   ->  sustained 0.9302 sh/s   DPS@19 17.67   burst 27.14
  >    fires 20t  ->  cycle 200t / 10.00s  ->  sustained 0.8000 sh/s   DPS@19 15.20   burst 21.71
```

**14% and 20% apart. Nothing downstream is priced on 16 until this row is taken.**

**This reading does NOT reopen `attack_damage: 19`, and is reported to the operator anyway.** 19 was
ruled outright rather than by comparison (`PLAN-boltor.md:30`), so no figure quoted beside it was ever
its basis and none can overturn it. But it was ruled on feel about an *outcome*, and on the `>` branch
that outcome is a fifth weaker than the numbers currently sitting next to the ruling. **The reading is
put in front of the operator when it lands so he can rule holding the real number.**

### 3. THE AUTHORING RULE FOR EVERY WEAPON AFTER THIS ONE

If it reads **16**, the honest cooldowns are the multiples of 4, and the multiples of 8 for anything
that will ever be dual-wielded (`boltor.yml:154`). If it reads **20**, **the grid is strictly-greater
and every one of those shifts by one** — the honest values become multiples-of-4-minus-one, and the
Boltor's own 16 would be re-ruled to 15.

---

## AFTER THE READING

- Write the reading beside the prediction above. **Do not edit the prediction.**
- If it reads 20: re-price the table in `PLAN-boltor.md`, re-rule `cooldown_ticks`, and say so at
  `WeaponLoader`'s `cooldown_ticks` section, whose *"ONE BOUNDARY IS UNMEASURED"* paragraph is
  discharged by this row either way.
- Report the recomputed sustained and burst figures to the operator, per §2.
