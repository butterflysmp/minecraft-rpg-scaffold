# PLAN — The Per-Tick Mob-Status Primitive, and Rooted

Read `CLAUDE.md`, `DESIGN-status-effects.md` first. This builds the **foundation**
that Rooted, Soaked, and Freeze all sit on, and configures Rooted as its simplest
use. Soaked and Freeze are follow-on commits, not this one.

Grounded in the current tree (HEAD `883c65a`):
- `StatusDefinition` is a sealed `Fire | Potion`. Adding a kind forces the
  exhaustive switch in `BukkitCombatant.applyStatus` to handle it — the compiler
  walks you there.
- Statuses are applied in `BukkitCombatant.applyStatus`, already hopped onto the
  entity's thread via `ctx.scheduler().onEntity(entity, …)`.
- `Scheduler` has `onEntity`, `onRegion`, `onRegionLater` — **one-shot** deferrals
  only. There is **no repeating task.** That is the gap this plan fills.

---

## Why this is a primitive, not just a status

Rooted, Soaked, and Freeze are all mob-only and all need the same thing vanilla
cannot give: **an effect that runs every tick for a duration and cleans up.**

- **Rooted** — set the movement-speed attribute to zero (kills the mob's self-propelled
  AI drive) *and* zero velocity every tick (kills imparted movement: knockback, jumps).
  Vanilla Slowness is useless: it is capped and doesn't stop jumping. **Correction:** an
  earlier version of this line claimed "only per-tick velocity control immobilizes." The
  code investigation disproved it — velocity-zero alone only cancels the AI's *output* each
  tick and leaves ~1% creep; the speed-attribute-0 is what actually immobilizes. This matches
  `DESIGN-status-effects.md` (which had it right) and the shipped `fix: Rooted is a real
  immobilize` commit.
- **Soaked** — recompute `max(0.6, 0.9^stacks)` and apply it every tick.
- **Freeze** — Rooted's velocity-zero plus attack suppression.

One primitive, three configurations. And it is the **convergence point of three
deferred systems**: passives ("always-on while equipped, re-evaluated each tick")
and the propagation engine ("do something on a tick/trigger") need the exact same
"repeating effect with clean lifecycle." Building it here — mob-only, where the
worst failure is a mob wandering when it shouldn't — is the right place for it to be
born, not inside the propagation engine where a lifecycle bug crashes the server.

**So this commit's real deliverable is the repeating-task lifecycle, correct. Rooted
is the simplest thing that exercises it.**

---

## The gap: `Scheduler` has no repeating task

Add a repeating primitive to the `Scheduler` interface (and `PaperScheduler` +
the fake). On Paper this is a repeating entity-scheduler task; on Folia it must be
the entity's own region scheduler (the entity owns its thread), consistent with how
`onEntity` already works. Shape (illustrative — the impl defines the truth):

```java
/** Runs `task` every `periodTicks` on the entity's thread until `task` returns
    false or the entity is removed. Returns a handle to cancel early. */
TaskHandle everyTickOnEntity(Entity entity, int periodTicks, BooleanSupplier task);
```

The **`task` returning false = "I'm done, stop scheduling me"** is the
self-terminating contract. That plus entity-removal is what prevents leaks (below).

**This is the load-bearing new capability.** It is also the thing passives and
propagation will reuse, so its lifecycle contract must be right the first time.

---

## The central hazard: cleanup on expiry OR death

A repeating task that outlives what it's attached to is the whole risk. Three ways a
per-tick status ends, and all three must stop the task and restore state:

1. **Duration expires.** The status has `durationTicks`; when they're up, the task
   stops and any modification it made is reverted (for Rooted, nothing to revert —
   velocity-zero leaves nothing behind once you stop zeroing; for Soaked, the speed
   modifier must be removed).
2. **The mob dies.** The common case — you CC things you're killing. A dead entity's
   task must stop *and not throw* trying to touch a removed entity. The repeating
   primitive must detect entity removal/death and self-cancel. A dangling task
   pointing at a dead entity, ticking forever, is the leak that kills a server at
   scale.
3. **The mob is re-statused / status refreshed.** Applying Rooted to an
   already-Rooted mob must not stack two tasks (two tasks both zeroing velocity is
   wasteful and the cleanup accounting breaks). Refresh the existing task's
   remaining duration; do not spawn a second.

**Acceptance is the cleanup, not the effect.** "It immobilizes" is easy. The tests
that matter:
- After the duration expires, the mob moves normally and **no task is still
  scheduled** for it.
- When a Rooted mob dies, the task stops and **nothing references the dead entity**
  (no exception, no lingering task).
- Rooting an already-Rooted mob results in **one** task, with refreshed duration —
  not two.

This is the same discipline as Soaked's future "base speed exactly restored" and the
`Regions.requireOwned` cleanup thinking: the failure mode is the lifecycle, not the
behavior.

---

## What's testable in `core` vs what's a boot observation

The **lifecycle logic** should be core-testable with the fake scheduler: a fake
`everyTickOnEntity` that a test can advance tick-by-tick and inspect ("is a task
still registered after N ticks?", "did it cancel when I marked the entity removed?").
The `FakeWorld.schedule`-honours-delays work is the precedent — the fake must model
repeating tasks and cancellation, or the lifecycle tests prove nothing.

- **Core-testable:** task registered on apply; self-cancels when `task` returns
  false; self-cancels on entity-removed flag; refresh replaces rather than stacks;
  duration countdown ends the task. Prove each by advancing the fake and asserting
  task presence/absence — a mutation that skips the cancel must redden.
- **Boot observation (named, not faked):** the velocity-zero *actually* holds a mob
  in place against jump attempts on a real server; a dying mob leaves no console
  error. Thread identity (task runs on the entity's thread) is witnessed on boot the
  same way the swing hop was — `isOwnedByCurrentRegion` on one tick — not unit-tested.

If a lifecycle test needs a real server, the fake is too weak — fix the fake, the
same call made for `FakeWorld.castRay` and the swing deferral.

---

## Rooted, as the first configuration

- **New `StatusDefinition` kind: `Immobilize` (or `Rooted`).** Adding it to the
  sealed interface forces `BukkitCombatant.applyStatus`'s switch to handle it —
  compiler-guided, no `default` arm.
- **On apply:** start (or refresh) an `everyTickOnEntity` task that sets the mob's
  velocity to zero each tick, for `durationTicks`.
- **Content:** `rooted.yml`, `kind: rooted` (mirrors `scorch.yml`'s `kind: fire`).
  An ability applies it via the existing `Status` effect — no ability-schema change.
- **Mob-only:** if the entity is a player, the status does nothing (or is never
  applied to players). Decided: these are mob-only for now. State it explicitly so a
  future player-target doesn't silently get a broken half-immobilize.

`StatusLoader` gains a `case "rooted"`; `ContentValidator` needs no new check
(rooted references nothing external, unlike potion types).

---

## Commit boundary

**This commit: the repeating-task primitive + Rooted only.** Not Soaked (adds stack
state + a formula + modifier cleanup), not Freeze (adds attack suppression). Rooted
is the simplest configuration — zero velocity, no state beyond the task, nothing to
revert on cleanup — so it proves the primitive and its lifecycle against the easiest
possible case. If the cleanup leaks, you find it here, not tangled in stacking logic.

Follow-on commits, each its own review:
- **Soaked** — the primitive + a per-mob stack count, `max(0.6, 0.9^stacks)`,
  **all-at-once decay** (duration refreshes on new application; all stacks drop at
  expiry), and the speed-modifier-cleanup hazard (base speed exactly restored).
- **Freeze** — Rooted's velocity-zero + attack suppression (intercept/cancel the
  mob's attack; scope how far "cannot attack" reaches — melee, ranged, AI pathing).

---

## Acceptance

**Core (unit):**
- Applying Rooted registers exactly one repeating task; a mutation that fails to
  register reddens.
- The task self-cancels at duration end; a mutation skipping the cancel reddens and
  leaves a task scheduled (proving the test sees lifecycle, not just effect).
- Marking the entity removed self-cancels the task; a mutation skipping the
  removal-check reddens.
- Re-applying to an already-Rooted mob yields one task with refreshed duration, not
  two.

**Boot (human, named observations):**
- A rooted mob is held in place and **cannot jump away** (the vanilla-Slowness
  failure this exists to fix).
- Rooted expires → the mob moves normally again.
- Killing a rooted mob produces **no console error** and no lingering behavior.
- `--refresh-content` deploys `rooted.yml`; boot logs it with no dangling refs.

## Rules

- No Bukkit in `core`. The primitive's *interface* and lifecycle logic are testable;
  the Paper *impl* of `everyTickOnEntity` is `paper`.
- The fake scheduler must model repeating tasks + cancellation, or lifecycle tests
  prove nothing. Fix the instrument before trusting it.
- Confirm a mutation applied before believing its result.
- Cleanup is the acceptance. "It immobilizes" is not enough — prove the task is gone
  on expiry and on death.

---

## Resolution (how this was actually built)

`core/` cannot name `org.bukkit.Entity` (the zero-dependency invariant), so an
entity-scoped repeating task is a **`paper/` type** — the primitive lives on `Scheduler`,
not `CombatWorld`. To keep cancel-on-death a *unit test* rather than only a boot
observation, the primitive is written against a **small paper-local interface** — a tick
target that reports `isActive()` and can `scheduleTick(delay, run)` on its own thread —
**not against `Entity` directly**. The Paper impl (`EntityTaskTarget`) adapts a real
`LivingEntity` to that interface (thin, boot-witnessed); the lifecycle logic
(`RepeatingTask` + `ImmobilizeStatus`) is unit-tested in `paper` against a `FakeTickTarget`,
no server, via `./mvnw -pl paper test`.

The fake's fidelity is its **own step with its own acceptance**: the existing double
models one-shot `onEntity` only, so it first grows two capabilities — advance N ticks with
repeating tasks firing each tick, and observe whether a task is still registered or has
cancelled — and those are *proven* before any lifecycle test is trusted. Otherwise the
skip-cancel mutation reddens nothing and every lifecycle test is green against a fake that
doesn't behave like Paper (the FakeWorld-discarded-`delayTicks` bug, one layer up).
