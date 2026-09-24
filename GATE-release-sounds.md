# GATE — release sounds for the Dragon's Plume and the Dragon's Breath

**Status: READ on `8c8d553`, 2026-09-24. All 9 rows.** Every prediction below was written **before**
any boot. Readings go **beside** a prediction, never over it, and a prediction is not edited once its
row has been read. **Ben's ruling, 2026-09-20: readings are verdicts, not figures** — a PASS cell
carries no measurement unless the row asked for one, and R0 is the row that asked.

```
READ      9   R0 R1 R2 R3 R4 R5 R6 Q1 Q2
         ──
          9   = git grep -c '^### R\|^### Q' <ref> -- GATE-release-sounds.md
```

> ### *** THE BINDING, TAKEN BEFORE ANY CELL WAS WRITTEN ***
>
> ```
> $ grep -c "\[Rpg\] Build:" run/logs/latest.log
> 1
> $ grep -m1 "\[Rpg\] Build:" run/logs/latest.log
> [04:32:53] [Server thread/INFO]: [Rpg] Build: 8c8d553
>
> [04:32:53] [Rpg] Loaded 6 abilities, 28 visuals, … 14 weapons, …
> [04:33:23] BaronVonYeetus joined the game
> [04:33:47] left        [04:34:08] joined again
> ```
>
> **ONE boot, a player on across two sessions, and `8c8d553` in no other log.**
>
> ### AND THE CONTENT REFRESH IS PROVEN ON BOTH HALVES, WHICH THIS SLICE NEEDED MORE THAN MOST
>
> **The five NEW visual files arrived**, each with its authored pitch:
>
> ```
> run/plugins/Rpg/content/visuals/dragons_plume_cast_tap1.yml   pitch: 0.8
>                                 …cast_tap2.yml                pitch: 1.0
>                                 …cast_tap3.yml                pitch: 1.15
>                                 …cast_draw.yml                pitch: 1.2
>                                 dragons_breath_cast.yml       pitch: 1.0
> ```
>
> **And the two CHANGED weapon files were OVERWRITTEN**, which is the half `saveResource(path,
> false)` would have skipped: the deployed `dragons_plume.yml` carries **4** `on_cast:` blocks
> naming all four new ids, and the deployed `dragons_breath.yml` carries `visual_id:
> dragons_breath_cast`.
>
> **THAT SECOND HALF IS THE WHOLE REASON R0 DEMANDED `--refresh-content`.** New files copy on their
> own; changed ones do not. Without the flag the five sound files would have landed and **nothing
> would have referenced them** — presenting as silence, which is indistinguishable from the feature
> not working. The deployed weapon files are the evidence that did not happen.

> **THE COUNT AND THE INSTRUMENT DISAGREED ON THE FIRST RUN, AND THAT IS THE CONTROL WORKING.** The
> header said **9** and `grep` said **8**: R0 was a `##` heading, so `^### R` could not see the one
> row the whole file depends on. **A count stated twice, from two sources, is a positive control that
> runs itself** — and it cost one command. R0 is now `### R0` under its own `##` banner, matching
> `GATE-dragons-breath.md`'s structure, and the two agree at 9.

**Branch:** `feat/release-sounds`, off `adc6622`.
**Declared game mode: SURVIVAL.** A formality here — nothing below depends on a cost creative
removes — but declared, because a gate that does not say costs readings.

---

## *** BEFORE THIS SLICE, BOTH WEAPONS WERE SILENT. THAT IS THE FINDING, NOT THE PREMISE ***

**Neither weapon plays any release sound today, and it is not because vanilla's is quiet — it is
because vanilla's never runs.**

| weapon | why vanilla is silent |
|---|---|
| **Plume** | `PlumeDraw.onRelease` calls `clearActiveItem()`, which empties `LivingEntity.useItem`; the re-read at offset 72 of `releaseUsingItem` then yields EMPTY and `ItemStack.releaseUsing` resolves to `Items.AIR.releaseUsing`, a pure no-op. **`BowItem.releaseUsing` never executes**, and the shoot sound is played inside it. Same lever R5 rests on. |
| **Breath** | `material: crossbow` binding `right_click`, and `RpgListeners` cancels the vanilla interaction for that case — *"Present == this weapon binds right_click. Suppress the vanilla interaction"*. **`CrossbowItem.use` never runs**, so neither its charge nor its shoot sound plays. |

**So there is nothing to double up with and nothing to suppress.** R5 below is the row that confirms
it: **one sound, not two.**

---

## *** THE CRITICAL READ, ANSWERED BEFORE ANY CONTENT WAS WRITTEN ***

**The question was whether `on_cast` fires once per PRESS or once per BODY** — because a per-body
hook would play the Breath's sound seven times stacked, and the Plume's five times on a full charge.

**IT IS ONCE PER PRESS, FOR BOTH, AND NO CODE CHANGE IS NEEDED.** Read from `CastExecutor`:

- `on_cast` is applied in **`commit()`** (`effects.applyAll(ability.onCast(), …)`), and `commit` is
  called **once** from `execute` and **once** from `executeFan`.
- **The Plume's fan**: `executeFan`'s own javadoc states it — *"IT COMMITS ONCE AND DISPATCHES N
  TIMES"*, and names this exact hazard: *"on_cast effects — the sound you hear when you let go —
  **FIVE sounds for one release**"* as the thing that wiring avoids.
- **The Breath's spread**: expanded in `launch()`, reached from `dispatch()` **below** `commit`.
  `launch`'s own comment says what depends on the ordering: *"the way to break it is to move the
  expansion ABOVE commit()"*.

> **THE SOUND IS NOT A THIRD RULE — IT IS THE SAME COMMIT-ONCE BOUNDARY** that makes a seven-body
> press cost ONE quiver round and roll ONE crit. Anything that moved the expansion above `commit`
> would break all three together, and `onePressSpendsOneRoundUnlessItIsAYawFan` already guards that
> boundary from the quiver side. **R1 and R2 are the behavioural confirmation; the guard is
> structural.**

---

## *** R0 — THE FIRST ROW OF EVERY GATE FILE. If it fails, STOP. ***

### R0 — The deployed build carries this slice

> ### *** BOOT WITH `--refresh-content`. FIVE NEW VISUAL FILES AND TWO CHANGED WEAPONS. ***
>
> `saveResource(path, false)` **never overwrites an existing file**. The five new visuals would be
> copied — **but `dragons_plume.yml` and `dragons_breath.yml` both CHANGED in this slice** (each
> gained `on_cast` blocks), and both already exist in `run/plugins/Rpg/content/`. **Without the flag
> the new sound files land and nothing references them**, which presents as complete silence: exactly
> the pre-slice behaviour, and indistinguishable from the feature not working.
>
> ```
> ./scripts/dev-server.sh --refresh-content
> ```

| # | prediction | instrument | READING |
|---|---|---|---|
| **R0a** | the build line names this branch's tip, not `-dirty`, not `unknown` | `Select-String -Path run\logs\latest.log -Pattern '\[Rpg\] Build:' \| Select-Object -First 1` | **PASS** *(8c8d553)* -- `[04:32:53] [Rpg] Build: 8c8d553`, one boot in the file, and 8c8d553 in no other session |
| **R0b** | **28 visuals** load — 23 at `adc6622` plus this slice's five | `Select-String -Path run\logs\latest.log -Pattern 'Loaded .* visuals'` | **28** *(8c8d553)* -- exactly as predicted. `[04:32:53] Loaded 6 abilities, 28 visuals, ...` |
| **R0c** | **no boot error naming any of the five visual ids, and none naming an unknown sound key** | `Select-String -Path run\logs\latest.log -Pattern 'dragons_plume_cast\|dragons_breath_cast\|Unknown sound\|Unknown visual'` | **PASS** *(8c8d553)* -- 0 matches. The only `[Rpg]` warnings in the session are the pre-existing `volley_stone` content line and its summary. Needle controlled: the same pattern matches in this file. |

> **R0c IS THE KEY CHECK AND IT IS CHEAP.** `VisualLoader.soundKey()` resolves at LOAD, so a bad
> sound key is a **named boot failure**, not a silent no-op. All three keys — `entity.arrow.shoot`,
> `item.crossbow.shoot`, `entity.blaze.shoot` — were verified declared in
> `net/minecraft/sounds/SoundEvents.class` on the pinned `26.1.2` jar before shipping, so R0c is
> expected to be empty. **An empty R0c with a non-empty R0b is the pair that means the files loaded
> AND resolved.**

---

## THE ROWS

### R1 — ONE sound per press on the Breath's seven-arrow ring

| | |
|---|---|
| **Setup** | `/rpg give dragons_breath`. Fire one press into open air and listen. |
| **Predict** | **ONE crossbow shot, not seven.** A stacked seven would be unmistakable — it reads as a single loud crack rather than a shot. |
| **Predict** | **A quiet fire hiss under it** — `entity.blaze.shoot` at volume 0.35. |
| **Predict** | If it plays seven times, the spread moved above `commit()` — and **the quiver cost and the crit roll are broken too**, so check R6 of `GATE-dragons-breath.md` before touching this file. |
| **READ** | **PASS, per Ben** *(8c8d553)* |

### R2 — ONE sound per press on the Plume's charged fan

| | |
|---|---|
| **Setup** | Hold a full draw and release, so five arrows leave at once. |
| **Predict** | **ONE arrow-shoot sound, not five.** |
| **Predict** | Same attribution as R1: five sounds means `executeFan` stopped committing once, and the durability billing is wrong in the same breath — `executeFan`'s javadoc pairs them. |
| **READ** | **PASS, per Ben** *(8c8d553)* |

### R3 — Each tap band has its own pitch, and they rise

| | |
|---|---|
| **Setup** | Fire a tap in each band — held 3-8, 9-14, 15-19 ticks — then a full draw. Four releases. |
| **Predict** | **Four sounds, rising in pitch**: 0.8, 1.0, 1.15, 1.2. |
| **Predict** | **The bottom three are clearly distinct.** 0.8 → 1.0 → 1.15 are the wide steps. |
| **Predict** | **THE TOP TWO MAY NOT BE.** 1.15 against 1.2 is what vanilla's own curve gives between t=18 and t=20, because it flattens toward full draw. **If tap3 and a full draw sound the same, that is the curve, not a mistake in the numbers** — see Q1. |
| **READ** | **PASS, per Ben** *(8c8d553)* -- four sounds, rising. **And the top two ARE distinguishable**: the prediction flagged 1.15 against 1.2 as possibly too close, and Ben did not report them as the same. See Q1. |

### R4 — A refused press is silent

| | |
|---|---|
| **Setup** | Empty the Breath's magazine and press fire. Then press again during the reload. |
| **Predict** | **No shot sound on either.** `on_cast` is reached only on a Success — a press refused by an empty quiver, a running reload or a cooldown never commits. The refusal notice and its dispenser-fail click still play; that is a different sound and a different surface. |
| **Predict** | Same on the Plume: a release below the vanilla floor, or on an empty magazine, is silent of a SHOT sound. |
| **READ** | **PASS, per Ben** *(8c8d553)* |

### R5 — No doubled vanilla sound, and none missing

| | |
|---|---|
| **Setup** | Fire each weapon once with nothing else happening, and listen for a second shot sound underneath ours. |
| **Predict** | **Exactly one shot event per press on each weapon.** No vanilla bow twang on the Plume, no vanilla crossbow release on the Breath. |
| **Predict** | **This is expected to be trivially true and is a row anyway**, because the two suppression mechanisms are completely different — `clearActiveItem()` on one, a cancelled interact on the other — and a change to either would show here first. |
| **READ** | **PASS, per Ben** *(8c8d553)* -- one shot event per press on each weapon; no vanilla twang under either |

### R6 — The Breath's reload sounds are unaffected

| | |
|---|---|
| **Setup** | Empty the Breath, reload, and listen through the whole cycle. |
| **Predict** | Still `loading_start` on the press and `loading_end` at maturity — **#147's cue, untouched.** The new `item.crossbow.shoot` is a third, distinct sound at a different moment, and the three must not blur into one. |
| **Predict** | **If the shot and the reload-start read as the same sound**, that is a finding about the choice of key rather than about the wiring — and Q2 is where it is ruled. |
| **READ** | **PASS, per Ben** *(8c8d553)* -- `loading_start`, the new `crossbow.shoot`, and `loading_end` stay three distinct sounds |

---

## THE QUESTION ROWS — BEN RULES AT THE BOOT

**Everything below ships PROVISIONAL.** The derivations are sound; nobody has heard any of it.

### Q1 — The Plume's pitch ladder

| | |
|---|---|
| **Question** | `0.8 / 1.0 / 1.15 / 1.2` across tap1, tap2, tap3, draw. **Derived, not invented**: vanilla's own `pitch = 1/(rand*0.4+1.2) + power*0.5` at its centre, with power taken from vanilla's charge curve at the SAME tick anchors R14's ruled tap speeds use — `t = 5 / 12 / 18 / 20`. |
| **Options** | (a) **right** — leave it. (b) **the top two are indistinguishable** — spread the ladder rather than move one value, because the flatness is vanilla's curve. (c) **the whole range is too narrow** — widen it and stop tracking vanilla. (d) **too wide / the low tap sounds wrong.** |
| **ANSWER** | **ANSWERED "as shipped", per Ben** *(8c8d553)* -- **THE LADDER IS RULED: 0.8 / 1.0 / 1.15 / 1.2, rising with charge.** Option (a). The tap3/draw closeness was called out in R3s own prediction as the thing most likely to fail, and it did not -- so the flatness vanillas curve produces near full draw is ACCEPTED rather than merely unnoticed. |

### Q2 — The Breath's fire layer

| | |
|---|---|
| **Question** | `entity.blaze.shoot` at volume **0.35** under `item.crossbow.shoot`. **0.35 is below `flint_cast`'s 0.6 deliberately** — that staff fires one bolt on a 24-tick cooldown and this fires seven bodies on 32, so a layer tuned for one shot is not a layer tuned for seven. |
| **Options** | (a) **right.** (b) **too loud** — drop toward 0.2. (c) **inaudible** — raise it, or it is doing nothing and should go. (d) **drop the layer entirely** — the crossbow shot alone is the weapon. |
| **ANSWER** | **ANSWERED "as shipped", per Ben** *(8c8d553)* -- **THE FIRE LAYER IS RULED at volume 0.35.** Option (a). Options (b) too loud, (c) inaudible and (d) drop it entirely were all declined. |

---

## WHAT THIS GATE CANNOT SEE

- **Whether the pitch ladder tracks vanilla's bow as a PLAYER perceives it.** The arithmetic matches
  vanilla's formula; whether that reads as *"the same feel as a bow"* is Q1 and only an ear settles it.
- **The sounds at range, or with several players firing.** Every row is one player, one press. Volume
  falloff and overlap between two Breath users are unread.
- **Nothing here is unit-testable.** A `VisualSpec.Sound` is a key, a volume and a pitch handed to
  `World.playSound`; there is no assertion to make about it that is not a restatement of the file.
  **The only mechanical guard is `soundKey()` resolving at load, which R0c reads.**
