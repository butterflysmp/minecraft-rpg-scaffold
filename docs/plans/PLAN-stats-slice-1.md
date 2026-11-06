# Stats, Slice 1 — Health Regen

Branch `feat/health-regen` off **`6a686f5`**, verified from the wire.

Growth (Armor Slice 2a) let max health rise. Nothing filled it. There was **no passive health
regeneration anywhere in the repo** — a grep for `healthRegen|HEALTH_REGEN` returned only
`ResourcePool.regenPerTick`, which is mana. The only routes into custom HP were `/rpg heal` and
`/rpg mobheal`, so a bigger pool was strictly a bigger hole. Meanwhile vanilla's own regeneration ran
unsuppressed against the custom-HP render.

This is a slice that **builds a mechanism**, like 2b and unlike 2a: there was nothing to ride.

---

## Three corrections to the brief, all load-bearing

### The reconcile seam is not where the brief said

The brief named `StatsBarSystem:97` as "that exact seam, polled every 10 ticks". That class is a
**display poller** and only reads. The reconciler is `PlayerHealthSystem.startReconcileLoop`, period
**5** ticks, whose banner comment `// Nine stats converge on the same scan:` a new stat must bump.
The brief's conclusion (the read side is proven) survives; the file does not.

### `EffectSpec.Heal` did not heal custom HP

The brief said the only heal paths were "ability/command heal (`EffectSpec.Heal`, `/rpg heal`)". In
fact `BukkitCombatant.applyHeal` wrote **vanilla** health — unlike `applyDamage` seven lines above it,
which routes through `ctx.stats().damage(...)`. For a player that attribute is a display, rewritten
from the custom numbers on the next `HealthChange`. So `rekindle.yml`'s heal moved the bar for a
fraction of a second and healed **exactly zero** of the health combat uses. A shipped silent no-op,
recorded in `NEXT.md` and in `RpgCommand.mobMutate`'s javadoc. Closed here (commit 5).

### There was no rounding drift to accumulate around

The brief asked how to derive a per-tick heal "without rounding drift at small values (0.2 HP/s is
0.01 HP/tick — accumulate, don't truncate)". `CombatantStats.heal(UUID, double, …)` takes a **double**
and `HealthState.heal` does `current = Math.min(max, current + amount)`. 0.01 HP stores as exactly as
1.0 HP does. An accumulator is only needed against an integer heal seam and there is none, so cadence
became a cost/smoothness call rather than a correctness one.

---

## Decisions taken

1. **Two systems, ONE knob.** Base 1 HP / 5 s = 0.2 HP/s; ×5 while `getSaturation() > 0`, so a fed
   player at base regenerates a round 1.0 HP/s. A
   multiplier on the same stat rather than a second stat, so gear that boosts regeneration boosts the
   saturated rate with it. Below saturation the flat rate is the floor, never zero.

2. **A dedicated `HealthRegenSystem` at 20 ticks**, shaped on `StatsBarSystem`, not folded into the
   5-tick reconcile loop.
   > Rejected: the reconcile period's javadoc is explicitly about *equipment rescan frequency*.
   > Sharing it would couple regeneration speed to gear-swap responsiveness, and a later tuning of
   > one would silently retune the other. At 20 ticks the stat's value, the HP/s a sheet prints, and
   > the amount paid per fire are the same number — no conversion factor for Slice 3 to drift against.

3. ~~**The saturated window charges exhaustion, per HP HEALED**~~ — **ABANDONED, and gate row 4 is
   why.** The charge was justified as *restorative*: cancelling `SATIATED` was assumed to also cancel
   the exhaustion vanilla charges for it. **Measured on Paper 26.1.2, that is false** — with our heal
   cancelled and no charge of our own, saturation still drained in ~4–5 s. Vanilla drains saturation
   regardless of whether its regen tick healed. A charge of ours would have been a second one.

   **The design gets what it wanted for free.** Food still gates the rate: fed → the saturated tier,
   drained → the floor. That two-tier economy is vanilla's own drain plus our multiplier, with no
   custom cost. `EXHAUSTION_PER_HP`, `exhaustionFor`, the `setExhaustion` call and both their tests
   were **removed rather than shipped dormant** — a constant at 0 with a live method behind it is a
   mechanism nobody can see is dead.
   > Also rejected, before the measurement: a direct `setSaturation` decrement — it bypasses vanilla's
   > exhaustion accumulator, so it composes with neither sprinting nor food level. Moot now.

4. **Cancel `SATIATED`, `REGEN`; cancel AND REROUTE `MAGIC`, `MAGIC_REGEN`, `EATING`; pass `CUSTOM`.**
   Rule: *never cancel a heal you are not ready to replace* — a cancelled potion is a silent no-op,
   worse than the flicker it replaces. Exhaustive `switch` expression, no `default`.
   > Rejected: cancelling everything including `CUSTOM`. A tidier invariant and a worse one — it eats
   > the unforeseen silently.

5. **`EATING` is rerouted, not passed.** The pinned API's javadoc calls it an *animal* reason;
   Bukkit's wider documentation calls it a player one, and the constant list cannot settle which is
   true for 26.1.2. Filing it with the unreachable boss reasons would be **asserting a mechanism
   instead of measuring one**. Treating it like `MAGIC` is correct either way and needs no gate row to
   prove a negative — and it leaves `PASS`'s "unreachable behind the player scope" true of everything
   actually in it.

6. **The reconciler returns `void`, unlike 2b's `boolean`.** Mana's boolean exists because its current
   lives in `ResourcePool` and a ceiling change must be pinned there. This stat is a rate with no
   current anywhere, read fresh every fire, so there is no transition to pin.

7. **Base is faction-conditional**, mirroring crit: `player ? BASE_PER_SECOND : 0.0`. That base *is*
   "mobs do not regenerate" — no second check at the tick site to contradict what the stat reads.

---

## What was built — seven commits

| # | commit | what |
|---|---|---|
| 1 | `2348338` | `HealthRegen` (core arithmetic) + `HeartScale.customFromHealthPoints` |
| 2 | `80004f3` | the tenth `Stat` on `HealthState`, and its `CombatantStats` face |
| 3 | `3f91da6` | `HealthRegenSystem` + `HealthRegenModifierItems` + the triad and reconcile wiring |
| 4 | `0f9c39c` | the `health_regen_boost_TEMP` fixture and `/rpg healthregen` |
| 5 | `9c49a3e` | `applyHeal` routes to custom HP — closes a shipped silent no-op |
| 6 | `cd533bd` | `EntityRegainHealthEvent`: `VanillaHealPolicy` + the handler |
| 7 | ~~set `EXHAUSTION_PER_HP`~~ | **NEVER WRITTEN — gate row 4 returned STOP** |
| 7' | *(this commit)* | strip the exhaustion machinery; `SATURATED_MULTIPLIER` → 5.0; gate results |

**The ordering was the point, and it paid.** The plan left commit sequencing as an "or"; it was fixed
so that commits 1–6 shipped with `EXHAUSTION_PER_HP` at **0.0** — making the deployed state at commit
6 exactly *suppression in, charge off*, which is what row 4 measures. **The measurement was sequenced
before the commit it would have authorized, and it refused to authorize it.**

Had the constant shipped at its derived 1.2 instead, the doubled drain would have presented as a
tuning problem rather than a false premise, and it would have been tuned toward zero one gate at a
time without anyone learning why.

The parameterised `exhaustionFor(healed, saturated, ratio)` existed so its tests could stay
discriminating while the constant sat at 0. It went with the rest — there is no dormant machinery
left, and the constant-at-zero device is recorded here rather than in the code.

---

## Verification — what was executed

**Unit.** `./mvnw clean package` → **core 592 / storage 17 / paper 401**, 0 failing.
Baseline at `6a686f5`: core 578 / storage 17 / paper 395.
(It was 594 before the gate; the two exhaustion tests went with the machinery they covered.)

`HealthStateTest` and `ResourcePoolTest` are left **byte-identical** — the faithfulness check on an
additive change, the device the 2b lift used.

**Every expected floating-point value was EXECUTED and pasted, never derived.** Two are not what the
arithmetic reads like it gives: a one-tick window yields `0.010000000000000002`, and the 99.9/100
headroom cap yields `0.09999999999999432`.

**Mutation — 24 planned, 24 RED.** Each was watched red *before* its row was written; sources were
copied to the scratchpad and restored from there, never `git checkout --`; each marker was grepped and
test-compiled before the result was believed; a run with no `Tests run:` line was treated as blind.

The nine `HealthRegen` rows were **re-run after the gate**, because the ×5 change and the exhaustion
strip moved their numbers. The rest live in files untouched since their first run.

| mutation | result |
|---|---|
| `SATURATED_MULTIPLIER` → 1.0 | RED `expected: <1.0> but was: <0.2>` |
| window fixed at 1.0 (drop the divisor) | RED `expected: <0.05> but was: <0.2>` |
| drop the `current >= max` guard | RED `expected: <0.0> but was: <-20.0>` |
| drop the `current <= 0` guard | RED `expected: <0.0> but was: <1.0>` |
| drop the `ratePerSecond <= 0` guard | RED `expected: <0.0> but was: <-5.0>` |
| drop the `periodTicks <= 0` guard | RED `expected: <0.0> but was: <-0.25>` |
| drop the headroom `Math.min` | RED `expected: <0.1> but was: <1.0>` |
| `boosts` uses `>=` | RED `expected: <false> but was: <true>` |
| `contribution` halves its input | RED `expected: <0.8> but was: <0.4>` |
| `customFromHealthPoints` drops the `× 2` | RED `expected: <20.0> but was: <40.0>` |
| `customFromHealthPoints` returns 1:1 | RED `expected: <20.0> but was: <4.0>` |
| drop its `healthPoints <= 0` guard | RED `expected: <0.0> but was: <-20.0>` |
| base the regen `Stat` unconditionally | RED `expected: <0.0> but was: <0.2>` |
| `healthRegenValue` uses `require()` | RED `IllegalState no health state tracked for …` |
| `reconcileHealthRegenModifiers` skips the reconcile | RED `expected: <1.0> but was: <0.2>` |
| …and emits a `HealthChange` | RED `expected: <0> but was: <3>` |
| `healthRegenTarget.clearModifier` no-ops | RED `expected: <0.2> but was: <0.5>` |
| `healthRegenTarget.setModifier` no-ops | RED `expected: <1.0> but was: <0.2>` |
| `SOURCE_PREFIX` → `""` | RED `expected: <false> but was: <true>` |
| `DEFAULT_BOOST` → 0.05 | RED `expected: <1.0> but was: <0.25>` |
| `MAGIC` → CANCEL (cancel without replacing) | RED `MAGIC is cancelled with nothing replacing it` |
| `EATING` → the boss/crystal PASS arm | RED `expected: <REROUTE> but was: <PASS>` |
| `CUSTOM` → CANCEL | RED `expected: <PASS> but was: <CANCEL>` |
| `SATIATED` → PASS | RED `expected: <CANCEL> but was: <PASS>` |

Two rows from the pre-gate run are **gone with the code they covered**: `exhaustionFor` dropping its
`!saturated` arm, and `exhaustionFor` ignoring `healed`. 26 → 24.

**One test's fixture had to move, and it would otherwise have stopped being able to fail.** The
headroom cap's "does not bite" row used 99.0/100. At ×4 a fed second paid 0.8, so headroom 1.0 did not
bite. At ×5 it pays exactly 1.0 — `Math.min(1.0, 1.0)` — so the row would have asserted the right
number for the wrong reason and survived deletion of the cap. Moved to 98.0/100, and the mutation
re-run against it.

**And the exhaustiveness guard was watched, not asserted.** Deleting `VanillaHealPolicy`'s `CUSTOM`
arm gives `VanillaHealPolicy.java:[71,16] the switch expression does not cover all possible input
values` — so a tenth `RegainReason` in a future Paper release cannot compile rather than falling into a
default. The coverage row in `VanillaHealPolicyTest` is the backstop for the day someone silences that
by adding one.

**Two mutations first reported DID NOT COMPILE, then BLIND, and neither was true of the mutation.**
`-pl paper` alone cannot resolve `rpg-core` (the parent POM is not in `~/.m2`), and the flag is
`-Dsurefire.failIfNoSpecifiedTests`, not `-DfailIfNoSpecifiedTests` — the unprefixed form is silently
ignored and surefire aborts having run nothing. Recorded because a harness that printed GREEN there
would have been reporting on a build that never ran a test.

**What is NOT covered offline, stated rather than left to look covered:** the regeneration loop body,
the equipment scan body, the fixture mint, and the event handler all need a live server —
`new ItemStack(...)` throws without a `RegistryAccess` and this project has no MockBukkit, as
`WeaponItemsTest` states at length. The gate below is what stands in for a mutation table there.

---

## Boot gate — `./scripts/dev-server.sh` — **RUN AND PASSED, 2026-08-31**

Operator-confirmed. **Rows 1, 2, 3, 6, 7, 8 and 11 pass. Row 4 returned its STOP signal, which is the
slice's most valuable result rather than a failure.** Rows 9 and 10 were exercised as part of the
single-clean-regen check: the handler fires and potions translate into custom HP.

| # | Check | Result |
|---|---|---|
| 1 | boot log | **PASS** — clean load, zero skipped content |
| 2 | `/rpg damage 50`, food full but saturation 0, idle 30 s | **PASS** — the flat floor, never zero |
| 3 | eat to restore saturation, idle 30 s while injured | **PASS** — the saturated tier (run at ×4; see below) |
| 4 | **PREMISE WITNESS**, at `cd533bd`: suppression in, charge off — time saturation to zero | **STOP.** Saturation still drained in **~4–5 s**. Cancelling `SATIATED` does **not** stop vanilla charging exhaustion; vanilla drains saturation on its own. The restorative premise is false and a charge of ours would have doubled the drain. **Commit 7 was never written.** |
| 5 | ~~witness the exhaustion charge~~ | **DROPPED** — there is no charge to witness |
| 6 | full HP, saturated, idle 60 s | **PASS** — no heart-bar movement, no churn: the `current >= max` short-circuit |
| 7 | `/rpg healthregen` (+0.8), hold, idle 30 s unsaturated | **PASS** — the rate rises and returns on drop. The reconcile surface is wired |
| 8 | die, respawn, idle | **PASS** — regeneration resumes; the respawn restart is load-bearing and works |
| 9 | food 20, saturation > 0, injured — watch for vanilla's own regen | **PASS** — single clean regen, no double-heal |
| 10 | drink a healing potion | **PASS** — the handler fires and the potion translates into custom HP, not a silent no-op |
| 11 | cast an ability with a `heal:` effect (`arc_surge`) | **PASS** — custom HP rises. Heals **zero** on the parent commit |
| 12 | peaceful `REGEN` | **NOT RUN, low priority.** The arm stays in the exhaustive switch, but the target server is never peaceful and at `difficulty=easy` it is unreachable |

**Row 4 is the row that justifies the whole ordering.** It was deliberately scheduled at commit 6,
before the commit it would have authorized, and it refused to authorize it. Rows 7, 10 and 11 are the
other discriminating ones — each heals nothing, or fails outright, on the parent commit of the change
it checks. Rows 2, 3 and 6 pass on any working regeneration.

**One number is NOT boot-witnessed, and saying so is the point.** Rows 2 and 3 ran at
`SATURATED_MULTIPLIER = 4.0`. The constant was retuned to **5.0** afterwards — a fed player at base
now regenerates a round 1.0 HP/s, dropping to the 0.2 floor once vanilla drains the saturation. The
*mechanism* those rows witnessed is unchanged; the *absolute HP figure* in row 3 is not what a re-run
would print.

## Out of scope

Slice 2 (Mana Regen as a per-player stat: the `regenPerTick` → `RegenResolver` lift, mirroring 2b's
`MaxResolver`, keeping the constant-wrapping constructor so `ResourcePoolTest` stays byte-identical).
Slice 3 (`/rpg stats`). A Health Regen **enchant** — this slice ships a `_TEMP` fixture instead, and
adds it to the removal debt. Mob passive regeneration. Regeneration persistence across sessions.
Showing the rate on the action bar. Crediting a heal to its caster (`CombatantHandle.applyHeal` takes
no `sourceId`; widening that port is where a heal-credit feature starts).

**Deferred with a known defect, recorded rather than left to be discovered: the potion reroute
overheals at high max HP.** `HeartScale.customFromHealthPoints` scales a cancelled `MAGIC` /
`MAGIC_REGEN` amount to a **proportion of custom max**. That is right near 100 HP — a 4-point potion is
two hearts, so 20 HP — and badly wrong above it: at a Growth-raised ceiling the same potion heals
**300+**. Proportional was chosen over 1:1 because 1:1 makes every potion worthless as ceilings rise.
The answer is neither; it needs **a cap or a fixed custom heal amount**, in a later slice. Also in
`NEXT.md`.
