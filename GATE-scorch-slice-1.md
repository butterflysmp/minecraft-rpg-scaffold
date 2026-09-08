# GATE — Scorch, slice 1

**This file is the source of truth for the scorch boot gate.** It is versioned with the code because,
for every behaviour listed below, **these rows are the only check that exists anywhere in the
project.** The suite passes with any of them deleted — 1360 tests, and not one of them can see a
number arrive on a screen, a kill get credited, or a burn tick at the rate it claims.

## Status: UNRUN. Drafted 2026-09-07 against `0affab9` plus the refresh fix below.

## READ THIS BEFORE RUNNING: one defect was found and fixed BEFORE the boot

The operator's review of `0affab9` found `ScorchStatus.apply`'s refresh arm calling `burnOnce`, so
**every re-application dealt an unscheduled damage tick outside the 20-tick clock this class exists to
own.** `solar_grenade`'s field would have burned at twice its stated rate for its whole life, and
"5% of max per second" would have meant "5% per second PLUS 5% per application".

**It is fixed, and the fix is under test** (`ScorchStatusTest.aRefreshDoesNotDealAnUNSCHEDULEDBurn` —
red before the change with burns at `[0, 10, 20, 40, 60, 80, 100, 120, 140]`, green after at
`[0, 20, 40, 60, 80, 100, 120, 140]`).

**This matters to the gate in a specific way: S1 alone would NOT have caught it.** A single
application is precisely the case the defect does not touch. **S1c is the row that catches it**, and
it exists because the defect was found first. That coupling is what this gate is built around — see
*S1 AND S1c* below.

> The tell was stylistic, and it is worth keeping as a review heuristic: **in a file where every
> other decision carries a paragraph, the one line with no comment is the one that was not decided.**
> Third time an uncommented line in otherwise-dense code has been the defect.

## How to use it

- **NAME THE ROWS YOU ARE ABOUT TO RUN, BEFORE YOU RUN THEM.** A count against an unnamed set is not
  an answer, however precise the number looks.
- A row marked **figure** wants a written observation, not a tick. **A figure row has no checkbox** —
  its text field is what marks it complete, and a blank field is UNRUN, not passed.
- A row marked **sole witness** names the behaviour it is the only check for. Skipping it is not
  reduced confidence; it is zero.
- A row marked **discriminating** fails if the specific defect the slice exists to prevent is present.
- A row marked **control** exists to stop another row crediting coverage it does not have.

## Rule 4 applies to every row here

*A gate row can be impossible, or real but non-discriminating, and both credit coverage that does not
exist.*

---

## BEFORE ANYTHING: the appliers, and the numbers each one predicts

**Scorch is applied by ABILITIES, not by weapon hits.** Weapon-damage stack accrual is explicitly not
in this slice — `Scorch.stacksFor` is built and tested, but its call site is an unmade decision. **A
row that says "hit it with a fire weapon" is unrunnable.** Every row below casts an ability.

The cap is `caster.payloadDamage()` — the authored headline damage of the effect list carrying the
status. So each applier predicts its own numbers, and **they differ enough that picking the wrong one
makes a row blind:**

| applier | authored damage, so cap | `duration_ticks` | on a 100-max target | ticks |
|---|---|---|---|---|
| `solar_lance` | 12 | 60 | `min(5, 12)` = **5**/tick | 3 |
| `solar_grenade` burst | 6 | 40 | `min(5, 6)` = **5**/tick | 2 |
| `solar_grenade` field | **2** | 40, re-applied every 20 ticks for 100 | `min(5, 2)` = **2**/tick | see S1c |
| `rekindle` | see file | 60 | | 3 |
| `ember_step` | see file | see file | | |

> **THE FIELD'S CAP OF 2 BINDS ON ANYTHING WITH MAX ABOVE 40**, which is the cheap way to witness the
> cap without a boss. `Scorch`'s javadoc warns that at max 100 the cap is invisible — that is true of
> the *lance* (cap 12, which never binds there) and **false of the field**. **S4 uses the field for
> exactly this reason. Running the cap row with the lance on a 100-max target proves nothing.**

Record before any row: **the target's custom max** and **which ability you cast**. S1, S4 and S5 are
unreadable without both.

Target max used this session: **____________**

---

## The rows

| # | action | expect | marks | figure |
|---|---|---|---|---|
| **S0** | Cast `solar_lance` at a mob. Does it visibly catch fire? | yes | figure · the burn must stay VISIBLE while its damage stops being vanilla's | |
| **S1** | **THE RATE.** Cast `solar_lance` at a 100-max mob **once** and do not touch it again. Watch the nameplate. | Drains in **discrete steps of 5**, one per second, **three of them**, then stops. Not a smooth drain, not 20 Hz. | **sole witness** for the whole owned clock · discriminating | |
| **S1c** | **S1's CONTROL, AND THE ROW THE PRE-BOOT FIX WAS MADE FOR.** Stand a mob in `solar_grenade`'s field for its full 100 ticks. **Count the floating damage numbers per second.** | **ONE scorch number per second, of 2.** The field pulses every 20 ticks, in phase with scorch's own clock. | **discriminating — TWO per second means the refresh burn is back** | |
| **S2** | While a mob burns from S1, watch for the drain **doubling** — our 5/sec plus a rerouted vanilla fire tick. | Single stream. No double-dip. | discriminating · witnesses the `FIRE_TICK` suppression | |
| **S2c** | **S2's control, because the suppression is NARROW ON PURPOSE.** Scorch a mob, then push it into **real fire or lava**. | It still takes **FIRE and LAVA damage in full** — only `FIRE_TICK` is replaced. | control · a suppression that swallowed all fire would pass S2 and still be wrong | |
| **S3** | **REQUIREMENT A, WHICH WAS INVERTED BEFORE THIS SLICE.** As the caster, scorch a mob and watch **your own screen** for floating numbers on each burn tick. | **One number per second, on YOUR screen.** | **sole witness** — popups draw only for the dealer, so this is the only in-game proof the applier reaches the sink. Before the slice, credit fell to the VICTIM, `dealerIsPlayer` was false, and **no number appeared at all** | |
| **S4** | **THE CAP.** Cast `solar_grenade` and let a **100-max** target stand in the **field**. | **2 per tick, not 5.** The field's cap of 2 binds below 5% of 100. | discriminating · **sole witness** in game for the `min` | |
| **S4b** | **The cap in the direction it was built for.** Same, against a **high-max** target (a boss, or a mob with a large custom max). | Still **2 per tick** — not 5% of the big pool. | **sole witness** for the anti-boss brake; uncapped, 5% of 5000 is 250/sec | |
| **S5** | **DEFENSE BYPASS.** Scorch an **armoured** target and an unarmoured one of the **same max**, with the same ability. | **Identical per-tick numbers.** Armour delays scorch through stack accrual rather than blunting the burn. | discriminating · sole witness for `bypassesDefense` reaching the sink | |
| **S6** | **Die while burning** as a player, then respawn. | Not on fire. No drain. | **sole witness** for the respawn `forget` site — quit handling does not run on death, and the entity-removal handler filters players out | |
| **S7** | Scorch a **player** (second client, or at yourself). | It applies and it ticks. | witnesses the deliberate divergence from Soaked and Immobilize, which both skip players | |
| **S8** | Cast `solar_lance` at a mob, wait about two seconds, cast again. | The burn **continues past** the original 60-tick window. | binary · the timer refreshes WHOLE | |
| **S9** | **CONTROL FOR THE NEW SEALED KIND.** Trigger something still authored `kind: fire`. | A plain vanilla burn — **no** floating numbers, no credit, no capped DoT. | control · proves `kind: scorch` did not swallow the kind it was split from | |
| **S10** | **KILL CREDIT, END TO END.** Let a mob die **purely to the burn** — scorch it, then stand back and do not touch it. | **Drops, XP orbs, and the MOB_KILLS statistic increment for YOU.** | **sole witness** for `MobDeathSystem`'s player-dealer path on a scorch kill | |
| **S11** | Check the boot log for scorch warnings. | Silent. No unknown status kind, no cap-invariant complaint. | binary | |
| **S12** | **THE FLAGGED AUDIT — a figure, NOT a pass.** With a second player, stand an **ally** inside `solar_grenade`'s field. | *figure* — record what happens to them. | figure · **deliberately not gated**, see below | |

---

## S1 AND S1c — the pair, and why S1 alone is not enough

**S1 measures the rate under exactly the condition the refresh defect does not affect.** One
application, one clock, one number per second — correct both before and after the fix. A gate built
only on S1 would have booted, read "5 per second, three ticks", and passed a build that doubled its
own damage the moment anything re-applied.

**S1c is the discriminating half**, and it works because of a coincidence worth writing down: the
grenade field's `tick_interval` is 20 and scorch's `PERIOD_TICKS` is 20, so **the field's pulses land
in phase with the burn's own clock.** With the refresh arm burning, each pulse contributes an inline
tick *and* a scheduled one, on the same tick:

```
  correct:          ONE number per second, of 2
  refresh burning:  TWO numbers per second, same tick
```

That is a count, not a judgement, which is what makes it a boot row rather than a figure. **Count the
numbers. Do not eyeball the drain.**

> If the two rows disagree — S1 clean, S1c doubled — the refresh arm is the first place to look, and
> `aRefreshDoesNotDealAnUNSCHEDULEDBurn` is the test that should have been red.

---

## S3 IS THE ONLY WITNESS FOR THE THING THIS SLICE EXISTS TO FIX

Requirement A was not missing before this slice; it was **inverted**, and it inverted when the vanilla
damage boundary landed. `FIRE_TICK` is one of the 29 REROUTE causes, and `attributableId` falls back
to the target's own id when no entity caused the event — so **scorch kills credited the victim.**

Nothing in the suite can see a popup reach a client. `DamagePopupManager.shouldShow` requires a
player dealer and a non-null dealer id, and draws on the **dealer's** screen only. So:

- **numbers appear on the caster's screen** — the applier reached `ScorchSink.deal`
- **no numbers at all** — credit fell back to the victim, exactly as before the slice

There is no middle reading, which is why S3 is binary rather than a figure.

---

## S12 IS NOT A PASS/FAIL, AND THAT IS DELIBERATE

Every target path excludes the caster by UUID, but Burst and Area exclude *the caster* without
excluding *other players* — so `solar_grenade`'s field re-scorches allies five times over its life.
This is not new damage; it is newly **credited** damage, and the first time an ally kill can be
attributed to a player.

**A friendly-fire rule is a decision nobody has been asked for.** S12 records what happens so the
decision is taken from an observation rather than from this paragraph. Do not "fix" it during the
boot.

---

## What these rows are witnessing that no test can

- **the rate** — whether the owned 20-tick clock actually paces the burn in game, rather than the
  invulnerability window pacing it (the D3/D3a failure, which cost a boot and an unusable test world)
- **the credit** — whether the applier reaches a screen and a kill statistic
- **the cap** — whether the `min` binds, which at the wrong applier or the wrong max is invisible
- **the bypass** — whether armour is genuinely out of the burn
- **the suppression** — whether the burn stays visible while its damage stops being vanilla's
- **the double** — whether re-application adds damage outside the clock (S1c, and only S1c)

`ScorchStatusTest` proves the scheduler deals eight ticks across exactly 160 ticks against a fake
clock. It cannot prove that a single one of them arrived.
