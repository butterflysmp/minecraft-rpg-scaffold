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

1. **Two systems, ONE knob.** Base 1 HP / 5 s = 0.2 HP/s; ×4 while `getSaturation() > 0`. A
   multiplier on the same stat rather than a second stat, so gear that boosts regeneration boosts the
   saturated rate with it. Below saturation the flat rate is the floor, never zero.

2. **A dedicated `HealthRegenSystem` at 20 ticks**, shaped on `StatsBarSystem`, not folded into the
   5-tick reconcile loop.
   > Rejected: the reconcile period's javadoc is explicitly about *equipment rescan frequency*.
   > Sharing it would couple regeneration speed to gear-swap responsiveness, and a later tuning of
   > one would silently retune the other. At 20 ticks the stat's value, the HP/s a sheet prints, and
   > the amount paid per fire are the same number — no conversion factor for Slice 3 to drift against.

3. **The ×4 charges exhaustion, per HP HEALED, not per tick** — so the drain is cadence-invariant.
   Restorative, not additive: cancelling `SATIATED` also cancels the exhaustion vanilla charged for
   it. An unsaturated window is free; a saturated one charges the **whole** heal, floor included.
   > Rejected: a direct `setSaturation` decrement — it bypasses vanilla's exhaustion accumulator, so
   > it composes with neither sprinting nor food level.

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
| 7 | **OWED** | set `EXHAUSTION_PER_HP` — **blocked on gate row 4** |

**Deviation from the plan, deliberate.** The plan left commit ordering as an "or". It is now fixed:
commits 1–6 ship with `EXHAUSTION_PER_HP` at **0.0**, so the deployed state at commit 6 is
*suppression in, charge off* — exactly what row 4 measures. The measurement is sequenced **before the
thing it authorizes**. If row 4 shows the suppressed drain is not slower than vanilla's, the charge is
additive rather than restorative and commit 7 is never written.

That is also why `exhaustionFor` takes the ratio as a **parameter** rather than reading the constant:
a version that read it would return 0 on both branches until commit 7, making the
"charges-when-unsaturated" mutation impossible to redden — a test that cannot fail.

---

## Verification — what was executed

**Unit.** `./mvnw clean package` → **core 594 / storage 17 / paper 401**, 0 failing.
Baseline at `6a686f5`: core 578 / storage 17 / paper 395.
`./scripts/check-jar.sh` → OK. `./scripts/check-tests.sh` → core 59 reports/594, storage 2/17,
paper 46/401.

`HealthStateTest` and `ResourcePoolTest` are left **byte-identical** — the faithfulness check on an
additive change, the device the 2b lift used.

**Every expected floating-point value was EXECUTED and pasted, never derived.** Two are not what the
arithmetic reads like it gives: a one-tick window yields `0.010000000000000002`, and the 99.9/100
headroom cap yields `0.09999999999999432`.

**Mutation — 26 planned, 26 RED.** Each was watched red *before* its row was written; sources were
copied to the scratchpad and restored from there, never `git checkout --`; each marker was grepped and
test-compiled before the result was believed; a run with no `Tests run:` line was treated as blind.

| mutation | result |
|---|---|
| `SATURATED_MULTIPLIER` → 1.0 | RED `expected: <0.8> but was: <0.2>` |
| window fixed at 1.0 (drop the divisor) | RED `expected: <0.05> but was: <0.2>` |
| drop the `current >= max` guard | RED `expected: <0.0> but was: <-20.0>` |
| drop the `current <= 0` guard | RED `expected: <0.0> but was: <0.8>` |
| drop the `ratePerSecond <= 0` guard | RED `expected: <0.0> but was: <-4.0>` |
| drop the `periodTicks <= 0` guard | RED `expected: <0.0> but was: <-0.2>` |
| drop the headroom `Math.min` | RED `expected: <0.1> but was: <0.8>` |
| `exhaustionFor` drops the `!saturated` arm | RED `expected: <0.0> but was: <0.96>` |
| `exhaustionFor` ignores `healed` | RED `expected: <0.96> but was: <1.2>` |
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

## Boot gate — `./scripts/dev-server.sh` — **OWED, not run**

Kill orphaned `java.exe` first: the script dies, two JVMs do not, and they hold the deployed jar.
No content YAML changed, so `--refresh-content` is not required.

| # | Check | Expected |
|---|---|---|
| 1 | boot log | clean load; zero skipped content; no new WARNING |
| 2 | `/rpg damage 50`, food full but **saturation 0** (sprint it off), idle 30 s | ≈ **+6 HP** — the flat floor, and it is never zero |
| 3 | eat to restore saturation, idle 30 s while injured | ≈ **+24 HP** — the ×4 |
| 4 | **PREMISE WITNESS.** At commit 6 (`cd533bd`: suppression in, `EXHAUSTION_PER_HP` = 0): saturated, injured, idle — time saturation to zero. Compare against the same measurement with the plugin's regen listener disabled. | The suppressed drain must be **SLOWER** than vanilla's. **If it is not, the charge is additive rather than restorative — STOP, do not write commit 7.** This row also prints the numbers `EXHAUSTION_PER_HP` is tuned from. |
| 5 | after commit 7, saturated + injured, idle | saturation visibly falls and rolls into food level |
| 6 | full HP, saturated, idle 60 s | **no** heart-bar movement, no action-bar churn — the `current >= max` short-circuit |
| 7 | `/rpg healthregen` (+0.8), hold it, idle 30 s unsaturated | ≈ **+30 HP** (1.0 HP/s); drop it and the rate returns to base within a tick |
| 8 | die, respawn, idle | regeneration resumes — the loop self-cancels on the death screen and `onRespawn` restarts it |
| 9 | food 20, saturation > 0, injured, watch for vanilla's own regen | none — `SATIATED` is cancelled |
| 10 | drink a **healing potion** at 50/100 | custom HP jumps by the translated amount, and is **not** a silent no-op |
| 11 | cast `rekindle` (its `heal:` effect) | custom HP rises |
| 12 | set `difficulty=peaceful` temporarily, injure, idle | no vanilla `REGEN` heal. The only way to reach that arm — at `difficulty=easy` (this server's setting) it never fires |

**Rows 4, 7, 10 and 11 are the discriminating ones.** Rows 2, 3 and 5 pass on any working regeneration
at all. Row 4 is the only row that can **stop the slice**. Row 7 is the only row that fails if the
reconcile surface is unwired. Rows 10 and 11 each heal **zero** on the parent commit, so each fails
without its own commit.

---

## Out of scope

Slice 2 (Mana Regen as a per-player stat: the `regenPerTick` → `RegenResolver` lift, mirroring 2b's
`MaxResolver`, keeping the constant-wrapping constructor so `ResourcePoolTest` stays byte-identical).
Slice 3 (`/rpg stats`). A Health Regen **enchant** — this slice ships a `_TEMP` fixture instead, and
adds it to the removal debt. Mob passive regeneration. Regeneration persistence across sessions.
Showing the rate on the action bar. Crediting a heal to its caster (`CombatantHandle.applyHeal` takes
no `sourceId`; widening that port is where a heal-credit feature starts).
