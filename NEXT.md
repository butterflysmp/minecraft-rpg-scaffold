# NEXT.md — plan after the visuals/statuses work

Read `CLAUDE.md` and `PROJECT.md` first. Work these in order. Stop and report
after each commit.

Findings referenced here come from a review of commits `3782cd4..1901981`.

---

## Step 0 — Verified on a real server

Run manually on a local Paper server (not via `dev-server.sh` — see Commit D).
Observed:

- plugin loads; `/rpg abilities` and `/rpg cast` both work
- ~40 FLAME particles at impact
- the blaze sound plays
- nearby mobs catch fire (`scorch` -> `setFireTicks`)
- **the lingering area keeps pulsing damage**

That last one is the load-bearing observation. It proves `EffectApplier.tickArea`
reschedules itself through `world.schedule` -> `onRegionLater` across five tick
boundaries, re-resolving combatants each pulse while holding only a `casterId`.
The entity-retention fix and the `Scheduler` abstraction are both proven under
real conditions, and the `CombatWorld` port survives contact with a server.

Every number involved -- 8, 6, 2, 100, 20, 4.0, 40, 0.6 -- came from YAML. None of
it is in Java. **The content pipeline is real**, validated a milestone earlier
than milestone 4 asks for it.

Consequences for the plan:

- `api-version: '26.1'` is **correct**. Change its comment in `paper-plugin.yml`
  from a claim into a record: *confirmed loading on Paper 26.1.2*. Fold this into
  Commit A, which is already about deleting unbacked assertions.
- The mechanical half of milestone 1 is done.

### What remains open: is it fun?

Nobody who hasn't thrown the grenade can answer this. It is the only acceptance
criterion PROJECT.md gives milestone 1, and it gates everything below.

Throw it twenty more times. Not to test it -- to feel it:

- Does the 200-tick cooldown create anticipation, or make you go do something else?
- Is 8 direct + 6 splash + 10 lingering the right split, or should the burst feel
  heavier and the burn be incidental?
- 40 particles at 0.6 spread: a satisfying *whump*, or a polite puff?
- Does the blaze sound land on the impact frame, or a beat late?

Each is a number in `solar_grenade.yml` or `solar_detonation.yml`. Change it,
restart, throw again. **Time that loop.** Under a minute means the content
pipeline is doing its job and milestone 4 will be easy. Five minutes is a finding
worth fixing before there are three hundred of these files.

The numbers you settle on here become the baseline every future ability is
balanced against. Worth an hour.

**If it is not fun, stay here.** Tuning is not a delay in this plan; per
PROJECT.md it *is* the plan. Nothing below is worth doing on top of combat that
is not worth playing.

**No commit.** This is a design gate.

---

## Commit A — Delete the comments that lie

**Zero behaviour change. Do this before anything else, because false invariants
stop the next reader from checking.**

Three javadocs written in the same commit contradict each other:

- `AbilityService` (class javadoc): *"The caller passes the Success to a
  CastExecutor on the thread that owns the impact point."* — **Unimplementable.**
  You cannot hop to the region owning the impact before `CastExecutor` has
  resolved where the impact is. Chicken and egg.
- `PaperCombatWorld.combatantsNear`: *"Two entry points reach here, and both
  satisfy that."* — **False.** `RpgCommand` hops on `onRegion(eye, ...)`.
- `PaperCombatWorld.present`: correctly states the eye/impact discrepancy.
  **Keep this one.**

Replace the first two with an accurate statement of what is actually true today:
callers hop onto the region owning the *caster's eye*, which is correct for
`Self` and `Melee` and wrong for `Ray` and for any `Area` whose origin lands in
another region. Link to Commit C.

There is no `FIXES.md` in this repository and there must not be. The entry that
once said "dispatch ability effects on the owning region thread" was wrong: for a
`Ray` there is no such single thread. **Commit C below supersedes it.**

And in `paper-plugin.yml`, turn the `api-version` comment from a claim into a
record. It now has evidence behind it (Step 0): confirmed loading on Paper
26.1.2. Same principle as the javadocs — say what is known, not what is assumed.

**Done when:** no comment in the repository asserts a property that has not been
either proven or reduced to a stated assumption.
**Commit:** `docs: correct the region-thread claims in cast path javadocs`

---

## Commit B — Make `shieldElement()` fail soft

`BukkitCombatant.shieldElement()` calls `Element.valueOf(raw)` on a string read
from the entity's PDC. A bad value throws `IllegalArgumentException` in the
middle of a detonation.

Every other unknown-id path in this codebase warns once and continues. This one
crashes, violating CLAUDE.md's rule that a content mistake must not take the
server down.

Resolve through a null-returning lookup (a static `Map<String, Element>`, or
`Arrays.stream(values())...findFirst().orElse(null)`), then `ctx.warnOnce(...)`
naming the entity UUID and the bad value, and treat it as unshielded.

**Done when:** a `core/` test proves `Element` has a null-safe lookup, and a mob
with a garbage `rpg:shield_element` PDC value takes damage with one warning.
**Commit:** `fix: fail soft on an unrecognised shield element`

---

## Commit C — The Folia correctness port change

This is the big one. Three review findings are the same bug wearing three hats,
and fixing them separately means fixing them twice.

### The finding that forces the design

`Combatant.isAlive()` and `Combatant.shieldElement()` are the only two methods on
the port that **return a value**. Every mutator (`applyDamage`, `applyHeal`,
`applyKnockback`, `applyStatus`) hops onto the owning entity thread via
`scheduler.onEntity`. The two readers cannot: **you cannot hop a thread and
still return synchronously.** Blocking on a future from a region thread
deadlocks.

So "wrap the readers in `onEntity`" is not available. The port itself is wrong.

### The shape of the fix

Split `Combatant` into a **read snapshot** and a **write handle**:

```java
// core/combat — an immutable value, captured on the owning thread.
public record CombatantSnapshot(UUID id, Vec3 position, boolean alive,
                                Element shieldElement) {}

// core/combat — dispatches; never returns world state.
public interface CombatantHandle {
    UUID id();
    void applyDamage(double amount, Element element, UUID sourceId);
    void applyHeal(double amount);
    void applyKnockback(Vec3 direction, double strength);
    void applyStatus(String statusId, int durationTicks, int amplifier);
}
```

`CombatWorld.combatantsNear` and `castRay` return snapshots paired with handles.
The snapshot is read on the region thread that owns the entity, at the moment it
is found — which is the only thread where reading it is legal.

`EffectApplier` reads only snapshots and calls only handles. It never asks the
world a question it isn't on the right thread to ask.

### What this subsumes

**Finding 1 — `Ray` cannot be resolved by a single hop.** `world.rayTrace` over
`range: 30` inherently spans regions. There is no "the region owning the impact"
to hop to, because the impact is what the trace is computing.

Rays must be **stepped** segment by segment through `onRegionLater`, re-entering
each new region as the segment crosses into it — exactly what `CastExecutor.step`
already does correctly for projectiles. Model `resolveAlongAim` on `launch`.

Pick a segment length and justify it in the commit message. A region is a group
of chunks, so a segment of a few blocks is safe and cheap.

**Finding 2 — `Success` retains a live entity across a tick.** `Success` holds a
`Combatant`, and its own javadoc says it must never be stored across ticks. But
`RpgCommand` captures it in the lambda handed to `onRegion(eye, ...)`, and
`RegionScheduler.execute` carries no inline guarantee — on Folia that is a
cross-thread dispatch that may land next tick.

After this change, `Success` carries `UUID casterId` and the `Aim`. No entity
reference crosses the hop. This is the same discipline `EffectApplier` and
`CastExecutor.launch` already follow; the entry point was the one seam nobody
checked.

**Finding 3 — attribution.** While the port is open, do the deferred fix:
`applyDamage(double, Element, UUID sourceId)`. The Paper adapter resolves
`sourceId` to a `LivingEntity` and calls `entity.damage(amount, source)`, so mobs
aggro the caster and kill credit lands. Do not widen this port twice.

The `Element` argument is currently dropped on the floor
(`BukkitCombatant.applyDamage`). Decide what it means — custom damage type,
resistance, or purely a multiplier already applied in `EffectApplier` — and say
so in the commit message. Do not leave it unused a second time.

### Sequencing

- **C1** — `core/` only. New `CombatantSnapshot` / `CombatantHandle`,
  `EffectApplier` and `CastExecutor` rewritten against them, stepped ray
  resolution, `Success` carries `casterId`. `FakeWorld` updated. All existing
  `core/` tests green plus: a ray crossing a simulated region boundary, and a
  test proving no `Combatant` outlives the tick that produced it.
- **C2** — `paper/` adapter. `BukkitCombatant` splits. `PaperCombatWorld`
  captures snapshots on the owning thread. `RpgCommand` passes `casterId`.
  Attribution wired through `entity.damage(amount, source)`.

`core/pom.xml` must still have zero dependencies when you are done. If a fix
wants a Bukkit type in `core`, the fix is wrong.

**Done when:** every world read in `paper/` happens on the thread owning that
location or entity, and no comment has to apologise for an exception.
**Commits:** `refactor: split Combatant into snapshot and handle` /
`feat: step ray casts across region boundaries` /
`feat: attribute ability damage to the caster`

---

## Commit D — Housekeeping

Small, and both directly serve "scales to big servers" and "survives version
updates," which are priorities 1 and 3 in PROJECT.md.

1. **Run or delete `scripts/dev-server.sh`.** It has never been executed, yet
   `README.md` and `CLAUDE.md` both instruct Claude Code to use it. A script
   nobody has run looks like a working path and is not one. This is the same
   defect class as Commit A's javadocs: an artifact asserting something works
   with nothing behind the assertion. Run it once and fix what breaks, or delete
   it and document the manual steps you actually use.
2. **Rename the package.** `io.github.yourname` appears in ~40 files. Do it now;
   it only gets more embarrassing.
3. **Add CI.** A GitHub Actions workflow on push: JDK 25, `mvn -pl core test`,
   then `mvn clean package`. Ten minutes of work. This is what catches a
   `paper.version` bump that breaks `core` — which, per CLAUDE.md's upgrade
   procedure, step 4, means `core` grew an illegal dependency.
4. **Add Renovate or Dependabot** for `paper.version` and
   `packetevents.version`. Per CLAUDE.md's upgrade procedure, PacketEvents is the
   gate: it must bump first, and it typically lags a Minecraft drop by one to two
   weeks. A bot that opens the PR is how you notice the gate opened.

**Commit:** `chore: fix dev script, rename package, add CI and dependency automation`

---

## Commit E — Milestone 1 content: three abilities, zero Java

`Melee`, `Ray`, and `Self` are unit-tested and no content file exercises them in
game. Milestone 1 wants three abilities and one class.

Add three abilities as **YAML only**. Not one line of Java.

This is simultaneously milestone 1's content requirement and a live rehearsal of
milestone 4 ("add a second class entirely through config"). **If Commit E
requires touching Java, the content pipeline is broken, and you want to discover
that now with three abilities rather than later with three hundred.**

Ship a `Ray` ability only after Commit C. A 30-block ray is the exact case the
stepped resolver exists for, and shipping it earlier ships a Folia bug as content.

**Done when:** three abilities load, each exercises a different `CastSpec`, and
`git diff --stat` shows only `.yml` files.
**Commit:** `feat: add solar lance, void slash and arc surge`

---

## Deferred, deliberately

- **`Bukkit::getCurrentTick`** backs `CooldownTracker` and `ResourcePool`. Folia
  has no single global tick; regions tick independently. This is the one Folia
  hazard the `Scheduler` abstraction does not cover, because it is not a
  scheduling call. Revisit before you actually run Folia.
- **Read-then-write ordering.** `isAlive()` is read before `applyDamage` is
  dispatched, so an area pulse hitting five mobs sees five living targets and
  schedules five hits. Harmless today. The moment you add an on-kill effect, it
  double-fires. Leave a comment in `EffectApplier` now; fix it when on-kill lands.
- **A throw inside `tickArea` kills the field permanently.** `applyToNearby` runs
  before `world.schedule`, so any exception from any effect skips the reschedule and
  the lingering area never pulses again. Commit B removed the only known thrower
  (`Element.valueOf` on an untrusted PDC string); it did not make the path safe in
  general. Deliberately not hardened: a `finally` that reschedules would convert one
  loud failure into a quiet one repeated every `tick_interval`, and a per-combatant
  `catch` would swallow real bugs exactly where the threading rules say they surface
  at scale. Revisit only if a second thrower appears.
- **Energy constants.** `MAX_ENERGY` and `ENERGY_PER_TICK` are Java constants in
  `RpgPlugin`. In a class-based RPG, energy is per-archetype content. Same
  cheap-now/migration-later argument that moved `VisualSpec` to a `steps:` list.
- **`Element.multiplierAgainst`** is still the placeholder 1.5x/1.0x rule.
- **Hot-reloading content.** If Step 0's tuning loop turned out slow, a
  `/rpg reload` command that rebuilds the three registries would pay for itself
  immediately. It is cheap because the loaders are already pure and server-free.
  Deferred only because it must not be written before Commit C settles what a
  registry hands out.
- **`PlayerProfile.archetypeId` / `unlockedAbilities`** are persisted and read by
  nothing. `/rpg cast` lets any permitted player cast any loaded ability. This is
  milestone 1's "one class" requirement and belongs right after Commit E.

---

## Rules for this work

- After every commit: `./mvnw -pl core test`. After every batch:
  `./mvnw clean package` and a manual boot.
- If a fix requires importing Bukkit into `core/`, stop and ask. That means the
  fix is wrong.
- Prefer a failing `core/` test that reproduces the bug before fixing it.
- Do not fix a compile error by widening the architecture.
- Explain any Java feature introduced that is not already used in the codebase.
- When you say something is verified, say what you executed.
