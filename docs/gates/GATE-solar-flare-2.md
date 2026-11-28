# GATE — SOLAR FLARE AS BEN CHANGED IT (N1 TO N7), LAST IN THE ONE COMMAND (2026-11-28)

**Status: RUN 2026-11-28 on `4f26ca31`; see READINGS (every row READ AS PREDICTED; awaiting the seat).** The plan is `docs/plans/PLAN-solar-flare.md` section 6. The scenario is
`paper/src/main/resources/selftest/GATE-solar-flare-2.yml`. The stack and the gate branch are `GATE-smolder-2.md`'s.
**Every prediction below was written before any boot, and is frozen from the seat's approval.** `GATE-solar-flare.md`
is frozen and not edited; its rows that changed are re-predicted here under their old names with the next suffix (R),
and two rows are new (SF10, SF11). Every figure is EXECUTED by `UltimatesTwoGateArithmeticTest`; the queue, the lint, the
asks, the summons and the three view steps by `UltimatesTwoGateCommandTest`.

**GAME MODE: SURVIVAL**, every row.

## WHAT BEN TYPES — THE ONE COMMAND IS `GATE-smolder-2.md`'s

**This is the pointer; the account (the command, the two questions, the builds, how long) is `GATE-smolder-2.md`'s
WHAT BEN TYPES:** `/rpg selftest smolder-2+solar-flare-2`. This gate runs second; SF9R is the command's last step.

## CARRIED STATE — `GATE-smolder-2.md`'s, FOR THE WHOLE COMMAND

**This is the pointer; the booter's checklist, R0 and the SAFETY sum for the whole command are `GATE-smolder-2.md`'s.**
This gate adds nothing to the carried state: its rows put back the blocks they place (`save_blocks`), and the run puts
back his builds (SF1R's pick).

## THE CLOCK

`GATE-smolder-2.md`'s THE CLOCK, and for a shot: **shot 1 leaves at C = P + 1 down the aim the stone read at P; shot 2 at
C+10 and shot 3 at C+20, each down his view read on his own thread on its own tick** (N1). **His view is set one tick
before each:** at P − 1 (before the drop), at P + 10 and at P + 20. A shot launched at L takes the step that traces 2e to
2e + 2 blocks from his eye on L + e (N2), so a body or block whose face is d blocks out stops it on **L + floor(d / 2)**;
with nothing in the way it bursts at **32.0** blocks on **L + 15**. **A burst is instant (N3): every body within 6
(horizontal) is struck on the burst's own tick.** Each shot line carries its own tick (N6).

## THE COORDINATE TABLE (x east, z south; every zombie at his y; executed by `UltimatesTwoGateArithmeticTest`)

**Every casting row stands him at H = F + (5, 0), block-centred, facing east**, so a shot's 32 blocks and its burst's 6
end at F + 43, inside the interior's 45.5. The preflight `-2 -13 45 13` is x −2.5 .. 45.5, z −13.5 .. 13.5, the interior.
His eye is 1.62 above his feet; a burst's origin y is the eye's for a range or block burst, the mob's feet for a mob burst.

| row | body | from H | from F | horizontal from its burst's origin | max health |
|---|---|---|---|---|---|
| SF2R | z59, z61 | (37.9, 0), (38.1, 0) | (42.9, 0), (43.1, 0) | 5.9, 6.1 from the range point H + 32 | 3000 |
| SF3R | s8, s13 | (8, 0), (13, 0) | (13, 0), (18, 0) | 0, 5 from s8 | 3000 |
| SF4R | b | (7, −1.5) | (12, −1.5) | 2.121 from the stone's face at H + 5.5 | 3000 |
| SF6R | his wolf, w | (30, −2), (34, 2) | (35, −2), (39, 2) | 2.828, 2.828 from H + 32 | his summon; 3000 |
| SF11 | e | (3, 1.5) | (8, 1.5) | 2.121 from the stone's face at H + 1.5 | 3000 |

| burst origin (from F) | rows | x reach (± 6) | z reach | inside both |
|---|---|---|---|---|
| (37, 0), the range point | SF2R, SF6R, SF7R, SF9R | 31 .. 43 | −6 .. 6 | yes |
| (36.514, 5.557) and (36.514, −5.557) | SF10's shots 2 and 3 | 30.51 .. 42.51 | −11.56 .. 11.56 | yes |
| (13, 0), s8's feet | SF3R | 7 .. 19 | −6 .. 6 | yes |
| (10.5, 0), the stone's face | SF4R | 4.5 .. 16.5 | −6 .. 6 | yes |
| (6.5, 0), SF11's wall | SF11 | 0.5 .. 12.5 | −6 .. 6 | yes |

The blocks: SF4R's stone at H + (6, 1, 0) (its west face 5.5 from his eye), SF11's at H + (2, 1, 0) (face 1.5 out), each
at head height (his eye at 1.62 is inside the block's 1 .. 2), saved first and put back by the run.

## EVERY TERM OF A HIT

sent = 250 x (score / 100) x (1 + enchant%) + class damage + level damage, x charge x crit. The stone: **score 100
(ABSENT)**, enchant **0**, class **0**, level **0**, charge **1.0**; **a crit CAN occur, rolled ONCE at the cast for all
three shots** (N1), x 2.0. So every hit of one cast reads **`sent=250.000`**, or every one **`sent=500.000`**. Defence
**0**. **Scorch:** cap (sent / crit) x 0.5 = **125** either way, window 120 restarted by each hit, each burn **min(150,
125) = 125**. **A body struck by all three bursts (offsets 0, 10, 20): seven burns, +20 … +140, 875 (EXECUTED); its end
3000 − 750 − 875 = `1375.000` (crit: 3000 − 1500 − 875 = `625.000`).**

## THE ROWS

### SF1R — THE BUILD SCREEN AND THE PICK (re-predicts SF1 for N7)

- After Ranger and Fire, ONE of three, named by which: (i) **`Q: Solar Flare`**, `Ultimate -- the default`; (ii)
  **`Q: (empty)`** and one `BUILD unoffered … cell=ranger/fire slot=ULTIMATE id=ultimate_placeholder_ranger reads=empty
  kept=file` (the last boot read (ii): his saved build names it, and the run put it back); (iii) `Q: Solar Flare`,
  `Ultimate`.
- **The picker:** `Solar Flare`, lore **`Fire three great fireballs, one after another.`**, **`Each bursts within 6
  blocks for 250 fire damage and scorches.`**, **`Cooldown: 210s`**, the `"Current"` line if it is his current pick.
- **The cell after:** `Q: Solar Flare`, `Ultimate`, the two lines, **`Damage: 250`**, **`Cost: 80`**, **`Cooldown: 210.0
  s`**, a blank, `Click to change.` `STAGE stone … found=0 held=1`; `HELD` the stone.

### SF2R — ONE AFTER ANOTHER, 32 BLOCKS, THREE BURSTS IN ONE PLACE, INSTANT (N1 to N3; re-predicts SF2)

- Before: z59, z61 **`3000.000/3000.000 fireTicks=0`**. Mana **`100.000`**, then **`20.000 … tick=P`**. **`DRIVE drop …
  yaw=-90.000 pitch=0.000`.**
- **`SOLAR_FLARE shot n=1 dir=-90.000 0.000 tick=C`**, **`n=2 … tick=C+10`**, **`n=3 … tick=C+20`** (each pitch prints
  `0.000` or `-0.000`; the sign of a zero is not predicted).
- Displays (`probe_entities item_display`): **1** at P+3 (shot 1 only), **2** at P+22 (shot 1 has burst; 2 and 3 fly),
  **0** at the end.
- **`SOLAR_FLARE burst n=1 cause=range origin=<H + (32, 1.620, 0)> tick=C+15`**, **`n=2 … tick=C+25`**, **`n=3 …
  tick=C+35`**: three bursts at ONE origin, 32.0 from his eye.
- **z59: `hit n=1 dist=5.900 tick=C+15`, `n=2 … tick=C+25`, `n=3 … tick=C+35`**, each `sent=250.000` (or all `500.000`),
  struck on each burst's own tick; **seven burns of 125, C+35 … C+155**; at P+180 **`1375.000`** (crit `625.000`).
- **z61: no hit line, `3000.000/3000.000`** at the end (6.1 is beyond 6).

### SF10 (NEW) — EACH SHOT FOLLOWS HIS VIEW AT THE MOMENT IT LEAVES

- View yaw −90 at P − 1, **−80 at P+10, −100 at P+20**, pitch 0.
- **`shot n=1 dir=-90.000 … tick=C`, `n=2 dir=-80.000 … tick=C+10`, `n=3 dir=-100.000 … tick=C+20`**: three directions.
- **Three range bursts, three origins (EXECUTED):** `n=1` at H + (32.000, 1.620, 0) on C+15; `n=2` at H + (31.514, 1.620,
  5.557) on C+25; `n=3` at H + (31.514, 1.620, −5.557) on C+35.

### SF3R — A MOB STOPS EACH SHOT (re-predicts SF3)

- s8's box begins 7.7 out (a zombie is 0.6 wide), so each shot is stopped on its launch tick + 3: **`burst n=1 cause=mob
  origin=<s8's feet, H + (8, 0, 0)> tick=C+3`**, **`n=2 … tick=C+13`**, **`n=3 … tick=C+23`**.
- **s8 `dist=0.000`** and **s13 `dist=5.000`**, each at C+3, C+13, C+23, each `sent=250.000` (or 500).
- Seven burns each, C+23 … C+143. At P+180: **s8 `1375.000`, s13 `1375.000`** (crit `625.000`).

### SF4R — A BLOCK STOPS EACH SHOT (re-predicts SF4, never yet read)

- `BLOCKS STONE count=1` at H + (6, 1, 0). Each shot meets the stone's face 5.5 out on its launch tick + 2: **`burst n=1
  cause=block origin=<H + (5.5, 1.620, 0)> tick=C+2`**, **`n=2 … tick=C+12`**, **`n=3 … tick=C+22`**.
- **b: `dist=2.121`** at C+2, C+12, C+22; seven burns, C+22 … C+142; at P+180 **`1375.000`** (crit `625.000`).

### SF6R — HIS WOLF UNTOUCHED (re-predicts SF6)

- The whistle's press reads `DRIVE use … yaw=-90.000 pitch=0.000`; his wolf frozen at H + (30, −2).
- Three range bursts at H + 32 (C+15, C+25, C+35). **w: `dist=2.828`** at each; at P+180 **`1375.000`** (crit `625.000`).
  **No line names the wolf**; its `SUMMONPROBE` health the same before and after; `SELF` the same before and after.

### SF7R — THE COOLDOWN AND THE COST, THE REGEN TERM NAMED (re-predicts SF7, X6 (c))

- `MANA current=100.000` (P − 1); the press at P: `SOLAR_FLARE cast` at C; **`MANA current=20.000 … tick=P`**.
- **The press at P+2: refused.** **`REPLY actionbar "Solar Flare -- ready in 209.9s"`** (4198 / 20) and **`MANA
  current=20.100 … tick=P+2`** = 20 + 2 x **0.05** (the regen term, EXECUTED as in SM6R). No second cast.
- Shots at C, C+10, C+20 (views held). At P+22 `clear_state`: **`SOLAR_FLARE cancelled caster=<him> remaining=2
  cause=selftest tick=P+22`** (shot 1 burst at C+15 = P+16; shots 2 and 3 in flight).
- At P+25, in one pass: `rpg mana set 79`, **`MANA current=79.000`**, the press **refused: `REPLY actionbar "Solar Flare
  -- needs 80 mana, you have 79"`**, **`79.000`**, no cast.

### SF8R — CANCEL BETWEEN SHOTS (re-predicts SF8)

- Shot 1 at C, shot 2 at C+10 (view held at P+10). At P+12: displays **2**, then `clear_state`: **`SOLAR_FLARE cancelled
  caster=<him> remaining=3 cause=selftest tick=P+12`** (none had burst).
- At P+15: displays **0**. **No `shot n=3` line, ever; no `SOLAR_FLARE burst` line.**

### SF11 (NEW) — N4'S CLOSE BURST: THE SAFETY READ

- `BLOCKS STONE count=1` at H + (2, 1, 0). Each shot meets its face 1.5 out on its launch tick: **`burst n=1
  cause=block origin=<H + (1.5, 1.620, 0)> tick=C`**, **`n=2 … tick=C+10`**, **`n=3 … tick=C+20`**: three bursts 1.5
  blocks in front of his eye.
- **e: `dist=2.121`** at C, C+10, C+20; seven burns, C+20 … C+140; at P+180 **`1375.000`** (crit `625.000`).
- **HIM: NO `HURT` line in the row, and `SELF` the same before and after** (the burst's front spares players, EXECUTED;
  the firework is particles and sounds, Route B: nothing else in it acts on a body).

### SF9R — THE ASK (the command's last step)

- The cast at C (views held at P − 1, P + 10, P + 20); three fireballs one after another down the pad, each bursting at
  range on C+15, C+25, C+35. The question at P+60 (C+59), after the third burst's 10-tick ball.
- The tell line: "Solar Flare is cast in a moment, east down the pad. Keep your mouse still and stand still; watch the
  fireballs leave and where each one bursts."
- SEAT PLACEHOLDER wording: **"Did three fireballs fly out one after another, and did each explode like a firework? (yes
  / no / not sure)"**. Answer logged verbatim; no answer is NOT STATED.

## CC'S CALLS (this gate's)

1. **H = F + (5, 0)**, not G: 32 + 6 down the pad must end inside the interior (F + 43 of 45.5).
2. **SF10 turns him 10 degrees each way** (not 90): a turned shot's burst must stay on the pad (z within 11.56 of 13.5).
3. **SF11's wall at 1.5** (the seat: within 3) with a witness behind it, so the row reads that the bursts happened.

## OPEN FOR BEN, AND WHAT ONLY THE BOOT CAN SHOW

`GATE-smolder-2.md`'s lists, which cover both gates.

## READINGS — THE BOOT OF `4f26ca31`, 2026-11-28 (written after the run; no prediction above is edited)

The boot, R0, his ground and his build, SAFETY (no `HURT` line), the clock and the view are `GATE-smolder-2.md`'s
READINGS (the command's). H read as `109.50 201.00 -3.50`; his eye 1.62 above (`202.620`).

| row | reading | class |
|---|---|---|
| SF1R | Case **(ii)**: `Q: (empty)` and one `BUILD unoffered … cell=ranger/fire slot=ULTIMATE id=ultimate_placeholder_ranger reads=empty kept=file`. Picker: `Fire three great fireballs, one after another.`, `Each bursts within 6 blocks for 250 fire damage and scorches.`, `Cooldown: 210s`, `Click to choose.` (no `Current`: not his pick). Cell after: `Q: Solar Flare`, `Ultimate`, both lines, `Damage: 250`, `Cost: 80`, `Cooldown: 210.0 s`, `""`, `Click to change.` `STAGE stone … found=0 held=1`. | READ AS PREDICTED |
| SF2R | z59, z61 `3000.000`. Mana `100.000` (2932) → `20.000` (2933); C = 2934. Shots `-90.000 0.000` at 2934, 2944, 2954. Displays **1** (P+3), **2** (P+22), **0** (the end). Bursts `cause=range origin=141.500 202.620 -3.500` at 2949, 2959, 2969 (C+15, +25, +35): ONE origin, H + 32. **z59 `dist=5.900` on each burst's own tick**, `sent=500.000`, `crit=true` ×3; seven burns 2969–3089, `125.000`; end **`625.000`**. **z61: no hit, `3000.000`.** | READ AS PREDICTED (the crit branch) |
| SF10 | C = 3141. Shots `-90.000 0.000` (3141), **`-80.000 0.000` (3151)**, **`-100.000 0.000` (3161)**. Bursts `cause=range` at 3156 `141.500 202.620 -3.500`, 3166 **`141.014 202.620 2.057`**, 3176 **`141.014 202.620 -9.057`** = H + (32, 0), H + (31.514, 5.557), H + (31.514, −5.557). | READ AS PREDICTED |
| SF3R | C = 3248. `burst cause=mob origin=117.500 201.000 -3.500` (s8's feet, H + 8) at 3251, 3261, 3271 (C+3, +13, +23). s8 `dist=0.000` and s13 `dist=5.000` at each, `sent=250.000`. Seven burns each, 3271–3391. End s8 **`1375.000`**, s13 **`1375.000`**. | READ AS PREDICTED |
| SF4R | `BLOCKS STONE count=1`. C = 3475. `burst cause=block origin=115.000 202.620 -3.500` (H + 5.5) at 3477, 3487, 3497 (C+2, +12, +22). b `dist=2.121` at each; seven burns 3497–3617; end **`1375.000`**. **The block stop, read for the first time.** | READ AS PREDICTED |
| SF6R | Whistle `yaw=-90.000 pitch=0.000`; the wolf at `139.50 201.00 -5.50` (H + (30, −2)). C = 3716; range bursts at 3731, 3741, 3751. w `dist=2.828` ×3; end **`1375.000`**. **No line names the wolf**; its `SUMMONPROBE` `100.000/100.000` before and after; `SELF 100.000/100.000` before and after. | READ AS PREDICTED |
| SF7R | Mana `100.000` (3921); the press at 3922 → `20.000`; C = 3923. The press at 3924: **`MANA current=20.100`**, `"Solar Flare -- ready in 209.9s"`. Shots 3923, 3933, 3943; burst 1 at 3938. **`SOLAR_FLARE cancelled remaining=2 cause=selftest tick=3944`** (P+22). At 3947: `79.000`, the press refused, **`"Solar Flare -- needs 80 mana, you have 79"`**, `79.000`. | READ AS PREDICTED |
| SF8R | C = 3995; shots 3995, 4005. Displays **2**, then **`SOLAR_FLARE cancelled remaining=3 cause=selftest tick=4006`** (P+12), then displays **0**. No `shot n=3`, no burst line. | READ AS PREDICTED |
| SF11 | `BLOCKS STONE count=1` at H + 2. `SELF 100.000/100.000` before. C = 4087. `burst cause=block origin=111.000 202.620 -3.500` (H + 1.5) at 4087, 4097, 4107 (C, +10, +20). e `dist=2.121`, `sent=500.000`, `crit=true` ×3; seven burns 4107–4227; end **`625.000`**. **His `SELF 100.000/100.000` after; NO `HURT` line.** | READ AS PREDICTED (the crit branch); the SAFETY read holds |
| SF9R | C = 4294; shots 4294, 4304, 4314; range bursts 4309, 4319, 4329. **Ben (01:12:43): "yes, but I don't like the firework sound. Make the particles orange too and it will be perfect"** | READ AS PREDICTED on what he saw and heard (the burst at 32 was seen and heard: Q1's fix); his note is a change request for the seat |
