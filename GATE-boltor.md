# GATE — the Boltor: the `>=` / `>` boundary, and the beam density at 96 blocks

**Status: NOT RUN.** **Every row below was written before any boot, and no reading has been taken.**
No figure in this file is an observation; the expected values are pre-recorded predictions, recorded
so that a later reading can disagree with them. When the row is run, the reading is written *beside*
the prediction and the prediction is not edited.

**This gate carries TWO rows, and both are BOOT-ONLY.** It shipped with one — `PLAN-boltor.md:225`,
*"the boundary row and only that"* — and the density row was added afterwards, deliberately, **before
either had been run**: the operator's time at a booted server is the scarce resource here, and two
boot-only rows written separately cost two trips. **Row 2 does not depend on row 1's reading**, so
nothing is contaminated by staging them together.

The elapsed figure landed in commit 3 (`GATE-q7.md` carries it) and the `tuning:` marking is
unwritten and unruled. **The boundary-dependent PROSE of commit 5 is deliberately still unwritten**,
because if row 1 reads 20 the tooltip overstates by 30% and that is a different paragraph — a row is
*written*, a conclusion is *taken*.

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

---

## ROW 2 — THE BEAM DENSITY AT 96 BLOCKS

**Nothing about this beam's appearance has ever been judged. `samples_per_block: 4` and `size: 1.2`
are INHERITED from beams half its length, and `BEAM_ORIGIN_GAP` was adopted at `1.0` on a
blanket-answered ruling and never compared against anything.** This is the longest line in the game
and the largest particle load any weapon has asked for.

| | |
|---|---|
| **weapon** | `boltor`, `range: 96`, visual `boltor_beam` |
| **do** | fire single shots at a clear sightline of 96+ blocks, then fire a held burst so two beams overlap in flight |
| **read** | is it a LINE or a trail of unrelated specks — **at the muzzle AND at the far end** |
| **record** | the client **Particles** setting it was judged on, and the verdict on each of the three numbers separately |

### THE STAGING REQUIREMENT THAT MAKES THIS ROW HARD, STATED BEFORE IT IS ATTEMPTED

**ONE OBSERVER CANNOT TAKE THIS ROW.** The client particle cap is ~32 blocks (`GATE-volley` V3), and
it is a cap on the distance from *the viewer* to the particle — not a property of the ray. So the
shooter renders roughly the **first third** of a 96-block beam and someone standing at the impact
point renders the **last third**. **Nobody renders the middle third at all.**

Judged from the muzzle alone this row measures a third of what is drawn — **and the wrong third**,
because it is the one nearest the eye and therefore densest on screen. A beam that looks solid from
behind the trigger is exactly what a too-low density looks like from there.

> **SO THE ROW NEEDS A SECOND VANTAGE POINT. `NEAR THIRD ONLY` IS THE LAST RESORT, NOT THE FIRST** —
> a row left open for want of a second account is how Q7 sat unread for two slices.

### TRY THIS FIRST — IT COSTS ONE COMMAND AND IT SETTLES ITSELF

```
fire at the 96-block sightline, then IMMEDIATELY /tp to the far end and look back
```

**Works → a full reading from one client. Fails → the second vantage is genuinely required, and
`NEAR THIRD ONLY` becomes a MEASURED fallback rather than an assumed one.** The difference between
*"we need two clients"* and *"we tried one and it cannot be done"* is one command.

> **PREDICTED FROM THE MECHANISM, NOT MEASURED — so the attempt is still worth making, and this is
> what it is testing.** A particle packet reaches only the clients in range **at the moment it is
> sent**. Teleporting afterwards cannot receive a packet that was never sent to you, so *particle
> lifetime is not the variable* — **reception is**.
>
> **But the beam is not drawn instantaneously.** `presentAlong` hops the region scheduler once per
> **chunk-column segment**, so a 96-block shot draws over **5–6 ticks axis-aligned, 9–11 at 45°**.
> That is the window, and it is the whole question:
>
> - **A typed `/tp` almost certainly misses it.** Seconds to type against a 5–6 tick draw.
> - **A tp issued INSIDE the window would catch the later segments** — the ones nobody renders. So
>   if the plain attempt fails, the variant worth one more try is the same teleport **on a keybind or
>   a macro**, fired immediately after the shot, or a repeating command block.
>
> **If both fail, that is a finding about the instrument and not a missing account**, and it should be
> written down as one: *the far two-thirds of a 96-block beam cannot be observed by the player who
> fired it, at any timing.*

**TWO ACCOUNTS ON ONE MACHINE IS THE ORDINARY ANSWER, NOT THE HARD CASE.** A dev box running a local
Paper server can hold a second client; one shoots, one stands at the target. That is the route that
certainly works, and the attempts above exist only because they are cheaper to try than to arrange.

**A single-observer reading, if it comes to that, is not this row** — it is a near-third reading, and
recording it as the density verdict settles a question it did not ask. Write `NEAR THIRD ONLY` on it,
say which attempts were made, and leave the row open.

### THE THREE NUMBERS ARE JUDGED SEPARATELY, BECAUSE THEY FAIL DIFFERENTLY

| number | today | what a bad reading looks like |
|---|---|---|
| `samples_per_block` | **4** | the line comes apart into specks — worse the further along it you look, and worse again on a reduced Particles setting |
| `size` | **1.2** | solid enough near, but two overlapping beams read as one fat smear rather than two bolts |
| `BEAM_ORIGIN_GAP` | **1.0** | the beam starts visibly detached from the weapon, or starts so close it occludes the crosshair |

**`BEAM_ORIGIN_GAP` IS THE ONE WITH NO ALTERNATIVE EVER TRIED.** It is a `core` constant shared by
every beam in the project, so a change to it moves the Lapis Staff and the Cursed Emerald too —
**which is the reason to judge it on the weapon that stresses it and to change it nowhere in this
slice.** If it reads wrong, that is a finding, not an edit.

### PRE-RECORDED, SO THE READING HAS SOMETHING TO DISAGREE WITH

```
lapis_beam     26 blocks   ~104 points per shot   settled, nobody is asking
emerald_beam   32 blocks   ~128 points per shot   went to size 1.0 for SIX overlapping beams
boltor_beam    96 blocks   ~380 points per shot   95 drawn x 4, the first 1.0 block skipped
               two in air  ~760 points            16t between shots against a 5-10 tick flight
```

**Expectation, written in advance: `4` holds at the muzzle and comes apart at the far end**, because
angular density falls with distance while `samples_per_block` is constant in WORLD space. If the far
observer reports a solid line, that prediction is wrong and the inheritance was better than it looked.

### AND THE SETTING IS HALF THE UNITS

`lapis_beam.yml`'s rule: **a density recorded without the client Particles setting it was judged on is
a measurement missing half its units.** It bites hardest here. A density-based visual degrades worse
than a count-based one — a thinned line stops being a line and becomes unrelated specks — and this is
the longest line in the game, so it has the most length over which to come apart. **Judge it on
`All`, then again on `Decreased`**, and record both; shipping a beam that only works on `All` is a
decision, not an accident, and it should be made knowingly.

---

## AFTER THE READING

- Write the reading beside the prediction above. **Do not edit the prediction.**
- If it reads 20: re-price the table in `PLAN-boltor.md`, re-rule `cooldown_ticks`, and say so at
  `WeaponLoader`'s `cooldown_ticks` section, whose *"ONE BOUNDARY IS UNMEASURED"* paragraph is
  discharged by this row either way.
- Report the recomputed sustained and burst figures to the operator, per §2.
- **Row 2: change nothing in the same commit that records it.** `samples_per_block`, `size` and
  `BEAM_ORIGIN_GAP` are three separate questions and the last one is a shared `core` constant; a
  reading that moves a number in the act of taking it cannot be checked afterwards. Record the
  verdict, then tune in a commit that says what it is tuning against.
- **A single-observer row 2 is recorded as `NEAR THIRD ONLY` and stays open.** It is not a failure to
  take half of it; it is a failure to call half of it the answer.
