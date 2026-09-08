# GATE — Scorch, slice 1

**This file is the source of truth for the scorch boot gate.** It is versioned with the code because,
for every behaviour listed below, **these rows are the only check that exists anywhere in the
project.** The suite passes with any of them deleted — 1360 tests, and not one of them can see a
number arrive on a screen, a kill get credited, or a burn tick at the rate it claims.

## Run 2026-09-08 — 11 of 16, NOTHING BROKEN, and two "failures" were defects in the ROWS

**Run and passed:** S0, S1, S1c, S2, S2c, S3, S4, S4b, S6, S10, S11 — named, eleven.

**Not run:** **S5**, **S7** and **S12** are **BLOCKED INDEFINITELY** — there is no second account, and
all three need one. They are NOT owed and NOT deferred; "owed" reads as work in progress and these are
not in progress. **S8** had no runnable form at all. **S5 was twice described as runnable on this page;
both entries were wrong, and on mobs it would have produced a FALSE PASS.** See its section below.

**Two rows reported a failure the build did not have.** S1c and S4 were corrected against what the
content files actually do, **not** by swapping the expectation for whatever the boot produced. Both
corrections are recorded in the rows with the reasoning, because a row quietly edited to match an
observation is indistinguishable from a row that was right.

> **THE ROWS WERE WRONG IN THE SAME WAY, AND IT IS A NAMED RULE.** Both were written from
> `ScorchStatus`'s clock without reading the applier's own YAML. A gate row's preconditions include
> **everything else the same cast does** — see `NEXT.md`, *A GATE ROW NEEDS ITS PRECONDITIONS
> ENUMERATED*, where this is now the third witness and supplied the axis the first two did not name.

---

## READ THIS BEFORE RUNNING: one defect was found and fixed BEFORE the boot

The operator's review of `0affab9` found `ScorchStatus.apply`'s refresh arm calling `burnOnce`, so
**every re-application dealt an unscheduled damage tick outside the 20-tick clock this class exists to
own.** `solar_grenade`'s field would have burned at twice its stated rate for its whole life, and
"5% of max per second" would have meant "5% per second PLUS 5% per application".

**It is fixed, and the fix is under test** (`ScorchStatusTest.aRefreshDoesNotDealAnUNSCHEDULEDBurn` —
red before the change with burns at `[0, 10, 20, 40, 60, 80, 100, 120, 140]`, green after at
`[0, 20, 40, 60, 80, 100, 120, 140]`).

**S1 alone would NOT have caught it.** A single application is precisely the case the defect does not
touch. **S1c is the row that catches it** — see the corrected threshold in its row, which is the
whole story of this boot.

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
status. **So ONE `solar_grenade` cast applies scorch TWICE, at two different caps**, which is the
thing S4's original text missed:

| applier | authored damage, so cap | `duration_ticks` | on a 100-max target | on the 360-max Knell |
|---|---|---|---|---|
| `solar_lance` | 12 | 60 | `min(5, 12)` = **5**/tick | `min(18, 12)` = **12**/tick — **cap binds** |
| `solar_grenade` burst | 6 | 40 | `min(5, 6)` = **5**/tick | `min(18, 6)` = **6**/tick |
| `solar_grenade` field | **2** | 40, re-applied every 20 ticks for 100 | `min(5, 2)` = **2**/tick | **2**/tick |

> **WHICH APPLIER WITNESSES THE CAP DEPENDS ON THE TARGET'S MAX, and stating it as a fixed
> recommendation was a defect in the first draft of this page.** The original text said "use the
> field, not the lance". That sentence was scoped to a 100-max target and is true there — the lance's
> cap of 12 never binds against 5% of 100 — **but it sat next to S4b, which is about a HIGH-max
> target, where it is false.** At 360 the lance binds at 12 and reads far more clearly than the
> field's flat 2, because you can see the `min` actually choosing between 18 and 12.
>
> **The rule, rather than the recommendation:** the cap is witnessed by any applier whose cap is below
> `5% x the target's max`. Pick the applier from the target, not from this page.

**Read the applier's own YAML before running a row about it.** `solar_grenade`'s `area` block carries
`type: damage amount: 2` *alongside* the scorch it applies, on the same 20-tick interval — which is
what made S1c's original threshold wrong by exactly one.

Target max used 2026-09-08: **100 (standard mob) and 360 (Knell)**

---

## The rows

| # | action | expect | marks | result |
|---|---|---|---|---|
| **S0** | Cast `solar_lance` at a mob. Does it visibly catch fire? | yes | figure · the burn must stay VISIBLE while its damage stops being vanilla's | **PASS 2026-09-08** |
| **S1** | **THE RATE.** Cast `solar_lance` at a 100-max mob **once** and do not touch it again. Watch the nameplate. | Drains in **discrete steps of 5**, one per second, **three of them**, then stops. Not a smooth drain, not 20 Hz. | **sole witness** for the whole owned clock · discriminating | **PASS 2026-09-08 — "5, 5, 5", one per second, three ticks, then stops.** The owned clock witnessed, at exactly the predicted numbers |
| **S1c** | **S1's CONTROL, AND THE ROW THE PRE-BOOT FIX WAS MADE FOR.** Stand a mob in `solar_grenade`'s field for its full 100 ticks. **Count the floating damage numbers per second.** | **TWO numbers of 2 per second** — the field's own `amount: 2` payload AND the scorch tick, same cadence, same value, **different sources**. | **discriminating — THREE per second means the refresh burn is back** | **PASS 2026-09-08 at two. THE ROW WAS WRONG, NOT THE BUILD:** it said one, having been written from the clock without reading the `area` effect list. Threshold corrected from 1→2 and the defect signal from 2→3. **Concept sound, discrimination real, number off by one** |
| **S2** | While a mob burns from S1, watch for the drain **doubling** — our 5/sec plus a rerouted vanilla fire tick. | Single stream. No double-dip. | discriminating · witnesses the `FIRE_TICK` suppression | **PASS 2026-09-08** |
| **S2c** | **S2's control, because the suppression is NARROW ON PURPOSE.** Scorch a mob, then push it into **real fire or lava**. | Still takes **FIRE and LAVA in full** — only `FIRE_TICK` is replaced. | control · a suppression that swallowed all fire would pass S2 and still be wrong | **PASS 2026-09-08** |
| **S3** | **REQUIREMENT A, WHICH WAS INVERTED BEFORE THIS SLICE.** As the caster, scorch a mob and watch **your own screen** for floating numbers on each burn tick. | **One number per second, on YOUR screen.** | **sole witness** — popups draw only for the dealer. Before the slice, credit fell to the victim and **no number appeared at all** | **PASS 2026-09-08 — the inverted requirement is right way up** |
| **S4** | **THE CAP, AND THE HANDOVER.** Cast `solar_grenade` at a **100-max** target and watch the whole sequence — burst, then field. | **5, then 2.** The burst applies at cap 6 (`min(5,6)`=5); the field then takes over at cap 2. **The change of number IS the most-recent-applier rule, observed.** | discriminating · **sole witness** in game for the `min` AND for the cap/credit handover | **PASS 2026-09-08 — "both 2 and 5". THE ROW WAS WRONG, NOT THE BUILD:** it expected "2, not 5" and ignored the burst entirely, though one cast applies scorch twice. Rewritten to expect the sequence, which witnesses strictly more than the original |
| **S4b** | **The cap in the direction it was built for.** A **high-max** target. **Pick an applier whose cap is below 5% of that max** — on the 360-max Knell that is `solar_lance` (cap 12 against 5% = 18). | Each tick held to the **cap**, not to 5% of the big pool. On the Knell: **12/sec**. | **sole witness** for the anti-boss brake; uncapped, 5% of 5000 is 250/sec | **PASS 2026-09-08 — 12/sec on the 360-max Knell**, `min(18, 12)` = 12. The cap binding cleanly. Row rewritten: it had said "use the field", which is right at max 100 and wrong here |
| **S5** | **DEFENSE BYPASS. UNREACHABLE — the target must be a PLAYER (mobs carry no defense at all), and only a player can scorch a player.** | **12 and 12**; a broken bypass would read **12 and 7.7** | discriminating · was the sole witness for the paper wiring | **BLOCKED INDEFINITELY — no second account. NOT DEFERRED.** On two mobs it would PASS while witnessing nothing. The transposition it guarded is now a COMPILE ERROR (`CritState`/`DefenseRule`); see the section below for what remains uncovered |
| **S6** | **Die while burning** as a player, then respawn. | Not on fire. No drain. | **sole witness** for the respawn `forget` site | **PASS 2026-09-08** |
| **S7** | Scorch a **player**. | It applies and it ticks. | witnesses the deliberate divergence from Soaked/Immobilize, which skip players | **BLOCKED INDEFINITELY — no second account, and only a player can scorch a player.** NOT deferred. "Players can be scorched" is a deliberate divergence from Soaked/Immobilize and has **no witness and no route to one** |
| **S8** | ~~Cast `solar_lance` at a mob, wait ~2s, cast again.~~ **RE-SPECIFIED — see below.** Stand a mob in `solar_grenade`'s field and confirm the burn outlives any single 40-tick application. | The burn **continues past** a single window while the field keeps pulsing. | binary · the timer refreshes WHOLE | **UNRUN 2026-09-08 — THE ROW WAS UNRUNNABLE AS WRITTEN**, and worse than reported: **no applier in shipped content can refresh its own burn by re-casting.** See the table below |
| **S9** | **CONTROL FOR THE NEW SEALED KIND.** Trigger something still authored `kind: fire`. | Plain vanilla burn — **no** numbers, no credit, no capped DoT. | control · proves `kind: scorch` did not swallow the kind it was split from | *(not named in the run set)* |
| **S10** | **KILL CREDIT, END TO END.** Let a mob die **purely to the burn** — scorch it, stand back, do not touch it. | **Drops, XP orbs, and the MOB_KILLS statistic increment for YOU.** | **sole witness** for the player-dealer path on a scorch kill | **PASS 2026-09-08** |
| **S11** | Check the boot log for scorch warnings. | Silent. | binary | **PASS 2026-09-08 — silent** |
| **S12** | **THE FLAGGED AUDIT — a figure, NOT a pass.** With a second player, stand an **ally** inside the field. | *figure* — record what happens. | figure · **deliberately not gated** | **BLOCKED INDEFINITELY — no second account.** NOT deferred. The ally-scorch attribution question stays open and unobserved |

---

## S5 IS UNREACHABLE, AND THE CLAIM IT GUARDED IS NOW MITIGATED BY CONSTRUCTION

**Two earlier entries on this page were wrong about S5 and are corrected here rather than deleted.**
It was first called "the only runnable row with nothing recorded" (false — it needs a second account),
then re-specified with magnitudes for a sitting that cannot be scheduled. **There is no second
account.** S5 is **unreachable, not deferred**, and the record now says so.

**Why, verified in source:**

1. Defense is **player-only**. `reconcileDefenseModifiers` has exactly one production caller —
   `PlayerHealthSystem:264`, on a scan of WORN GEAR. `MobDefinition` is
   `(id, baseEntity, displayName, maxHealth)` with **no defense field**, so a test mob wearing armour
   is a FEATURE, not a content file.
2. Nothing but another player can scorch a player. Ability paths exclude the caster by UUID; `/rpg
   apply` filters `!(living instanceof Player)` (`RpgCommand.java:874`).

> **AND ON TWO MOBS THE ROW WOULD HAVE PASSED WITHOUT WITNESSING ANYTHING** — defense 0 against
> defense 0, `applyDefense` returning both unchanged, identical ticks whether or not the flag arrives.
> Rule 4's *real but non-discriminating* case, on a new core seam. **A false pass there is worse than
> the blank.**

### WHAT IS COVERED AND WHAT IS NOT — so nobody reads "unrun" as "untested"

**The gap was one hop, not the whole claim.**

| the claim | witness |
|---|---|
| the bypass skips the Defense curve **entirely** rather than reducing its cut | `CombatantStatsTest.bypassesDefenseSkipsTheCurveENTIRELYRatherThanReducingItsCut` — same victim, same defense, same amount as the row above it, so it cannot pass by an accident of arithmetic |
| the flag **arrives at the sink** and is carried, not dropped | `FakeWorld` records it per call; asserted by the core suite |
| scorch's arithmetic, cap, credit, clock and lifetime | `ScorchTest`, `ScorchStatusTest` (16 rows) |
| **the paper wiring passes the RIGHT VALUE** — `EntityScorchSink` → `BukkitCombatant` → `CombatantStats` | **NO TEST, AND NO REACHABLE GATE ROW.** Mitigated by construction instead — see below |

### THE MITIGATION: the transposition no longer compiles

The unwitnessed line was two bare positional booleans, and transposed it silently meant *"this was a
crit, and Defense applies"*:

```java
handle().applyDamage(amount, applierId, false, true);       // before
handle().applyDamage(amount, applierId, CritState.NORMAL, DefenseRule.BYPASSED);   // now
```

`CritState` and `DefenseRule` are distinct enums, so the swap is a **compile error**. One layer down,
`CombatantStats.damage` carried **three** adjacent booleans (`dealerIsPlayer, wasCrit,
bypassesDefense` — six orderings, five wrong, all compiling, all three passed positionally by
`BukkitCombatant`); two are now types and `dealerIsPlayer` stays a boolean **because it is then the
only one**, with nothing to be transposed with.

**Verified as a mutation, not asserted:** the two arguments were swapped and the build run. It failed
with `incompatible types: DefenseRule cannot be converted to CritState`. Restored, `md5sum`-checked,
and `./mvnw clean package` re-run green at 801 / 17 / 542.

> **This does not make S5 unnecessary — it makes it unnecessary FOR THE TRANSPOSITION.** A wiring
> error the types cannot catch (the sink calling the wrong overload, a future caller passing
> `APPLIES`) still has no in-game witness. That residue is real and is why the row stays on the page,
> blocked, rather than being deleted.

---

## S8 WAS UNRUNNABLE, AND THE REASON IS WORSE THAN A MIS-SIZED WAIT

The row said "wait ~2s, cast again". `solar_lance`'s cooldown is **100 ticks**, not the ~40 the row
assumed. But the real finding is one level up — **measured across all four appliers:**

| applier | `cooldown_ticks` | scorch `duration_ticks` | can it refresh its own burn? |
|---|---|---|---|
| `solar_lance` | 100 | 60 | **no** |
| `solar_grenade` | 200 | 40 | **no** by re-cast |
| `rekindle` | 200 | 60 | **no** |
| `ember_step` | 160 | 60 | **no** |

**NOT ONE APPLIER CAN REFRESH ITS OWN BURN BY RE-CASTING.** Every cooldown outlives every duration, so
the row had **no runnable single-ability form at all** — it was not mis-sized, it was impossible, and
rule 4's impossible-row case is exactly what it was.

The only refresh paths that exist today are `solar_grenade`'s field re-applying on its own 20-tick
interval against its own 40-tick window, two different fire abilities overlapping, or two players.
**S8 is re-specified against the field.**

> **This is also why the refresh-burn defect was survivable in slice 1, and what ends when accrual
> lands.** Once a weapon applies stacks on damage, a weapon swinging faster than the burn lasts makes
> the refresh arm the COMMON path rather than a corner — and S8 becomes trivially runnable in the same
> change. See `NEXT.md`, *THE ACCRUAL IS DISPLACED, NOT DESCOPED*.

---

## S1 AND S1c — the pair, and why S1 alone is not enough

**S1 measures the rate under exactly the condition the refresh defect does not affect.** One
application, one clock, one number per second — correct both before and after the fix. A gate built
only on S1 would have booted, read "5, 5, 5", and passed a build that doubled its own damage the
moment anything re-applied. **That is precisely what S1 did read on 2026-09-08**, which is the
cleanest possible demonstration that it is not sufficient on its own.

**S1c is the discriminating half**, and it works on a phase coincidence: the grenade field's
`tick_interval` is 20 and scorch's `PERIOD_TICKS` is 20, so the field's pulses land in phase with the
burn's own clock. **What the first draft of this row missed is that the field's `area` effect list
carries its own `type: damage amount: 2` on that same interval.** So:

```
  correct:          TWO numbers per second, of 2   (field payload + scorch tick)
  refresh burning:  THREE numbers per second       (field payload + scorch tick + inline refresh burn)
```

Still a count, not a judgement — the discrimination survives the correction intact, because the
defect adds one number either way. **Count the numbers. Do not eyeball the drain.**

> **The near-miss is worth keeping.** A runner trusting the original row would have reported a fixed
> build as broken, and the reflex on "the fix did not take" is to reopen the fix — the one part that
> had been proved by mutation. **A row that is wrong in the direction of a false alarm attacks the
> most-verified code on the page.**

---

## S3 IS THE ONLY WITNESS FOR THE THING THIS SLICE EXISTS TO FIX

Requirement A was not missing before this slice; it was **inverted**, and it inverted when the vanilla
damage boundary landed. `FIRE_TICK` is one of the 29 REROUTE causes, and `attributableId` falls back
to the target's own id when no entity caused the event — so **scorch kills credited the victim.**

Nothing in the suite can see a popup reach a client. `DamagePopupManager.shouldShow` requires a
player dealer and a non-null dealer id, and draws on the **dealer's** screen only. So:

- **numbers appear on the caster's screen** — the applier reached `ScorchSink.deal`
- **no numbers at all** — credit fell back to the victim, exactly as before the slice

There is no middle reading, which is why S3 is binary rather than a figure. **PASS 2026-09-08.**

---

## S12 IS NOT A PASS/FAIL, AND THAT IS DELIBERATE

Every target path excludes the caster by UUID, but Burst and Area exclude *the caster* without
excluding *other players* — so `solar_grenade`'s field re-scorches allies five times over its life.
This is not new damage; it is newly **credited** damage, and the first time an ally kill can be
attributed to a player.

**A friendly-fire rule is a decision nobody has been asked for.** S12 records what happens so the
decision is taken from an observation rather than from this paragraph. Do not "fix" it during the
boot. **BLOCKED INDEFINITELY 2026-09-08 — no second account.** The question stays open and unobserved.

---

## What these rows are witnessing that no test can

- **the rate** — whether the owned 20-tick clock actually paces the burn in game (**witnessed, S1**)
- **the credit** — whether the applier reaches a screen and a kill statistic (**witnessed, S3/S10**)
- **the cap** — whether the `min` binds (**witnessed, S4/S4b**), which at the wrong applier for the
  target's max is invisible
- **the handover** — the most-recent-applier rule changing cap and credit mid-burn (**witnessed, S4**)
- **the bypass** — whether armour is genuinely out of the burn (**NOT witnessed and NOT witnessable —
  S5 is BLOCKED INDEFINITELY. The transposition is now a compile error instead; the residual wiring
  risk is stated in S5's section**)
- **the suppression** — whether the burn stays visible while its damage stops being vanilla's
  (**witnessed, S2/S2c**)
- **the double** — whether re-application adds damage outside the clock (**witnessed, S1c, at the
  corrected threshold**)
- **players as victims** — the deliberate divergence from Soaked/Immobilize (**NOT witnessed — S7,
  BLOCKED INDEFINITELY, no route to a witness without a second account**)

`ScorchStatusTest` proves the scheduler deals eight ticks across exactly 160 ticks against a fake
clock. It cannot prove that a single one of them arrived.
