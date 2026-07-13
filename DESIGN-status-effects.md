# DESIGN — Element-Linked Status Effects

Status: **design target.** The *simple tier* is a near-term content pass with two
small engine additions. The *propagation engine* is a deferred milestone with real
safety hazards. Nothing here is built yet. Read `CLAUDE.md` first.

Assumes merged state: elements are content, `Status` is an existing `Targeted`
`EffectSpec`, and `StatusDefinition` is a sealed `Fire | Potion` type with `scorch`
(`kind: fire`) as the shipped DoT template.

---

## Why this exists

After Phase 2, an element is pure identity — Fire vs Water is flavor, no mechanics.
That was deliberate: the weak/strong damage triangle was removed, and
`multiplierAgainst` deleted. Element-linked statuses give elements **mechanical
weight back without a resistance triangle**: Fire abilities burn, Nature abilities
root, Water abilities slow/freeze. The element *does something* — but it's a shared
mechanical thread among an element's abilities, not "Fire beats Water."

Elements affect **battlefield state** (is the target burning, rooted, slowed), not
**damage math**. This is the design line, and it's load-bearing: see the Rooted
note below for why one early spec violated it and was cut.

This also directly serves the deferred class-axis verdict. Statuses are a major
lever for making cells feel distinct: a Nature Ranger that roots-and-kites plays
differently from a Fire Mage that burns-and-zones, *before* any passive or aspect.

---

## Scope, and what was cut

**Six statuses, four elements.** Two tiers.

**Simple tier — authorable now, two small new mechanics:**
- **Scorched** (Fire) — damage over time. *Already shipped* as `scorch`.
- **Rooted** (Nature) — target cannot move.
- **Soaked** (Water) — stacking slow, diminishing, with a floor.
- **Freeze** (Water) — target cannot move or attack.

**Propagation tier — one engine, deferred milestone:**
- **Thorns** (Nature) — damaging a Thorns target damages nearby targets. Depth-1,
  explicitly **not** a chain.
- **Ignite** (Fire) — a Scorched target killed by a Scorched attack explodes after
  a delay, damaging nearby targets. Explicitly **chains**.

**Cut for now, on the record so it's a decision not an omission:**
- **Unstable** (Void) — explode-on-damage + *tether*. Cut because "tether" was
  undefined (one yank vs a persistent positional leash = very different builds) and
  Void doesn't need mechanical identity yet.
- **Blighted** (Wither) — DoT-until-consumed + spread-on-death. Cut because it's
  **self-propagating with no natural terminator**: the DoT kills its own host, which
  spreads to neighbors, who rot and spread — one application could clear a whole mob
  pack unattended. Revisit with a spread cap / stack limit when Wither needs identity.

---

## THE SIMPLE TIER — near-term content pass

Each is a `StatusDefinition` an ability applies via the existing `Status` effect.
Three of the four map onto existing or vanilla mechanics; two need a small new
concept. **Author these, put them on abilities pushed toward each class's verb, and
replay Ranger vs Mage.** This is the axis test.

### Scorched — done

`scorch` ships (`kind: fire`, drives `setFireTicks`). It is the DoT template. Any
new element DoT is this shape.

### Rooted — immobilize only

Target cannot move. Maps to a vanilla movement lock (max slowness / a movement
attribute set to zero for the duration). **No new engine** beyond applying and
cleanly removing the effect on expiry.

> **Immobilize covers a movement-class matrix; fliers are a deliberate non-fix.**
> MOVEMENT_SPEED-0 + velocity-zero clamp attribute-scaled locomotion (walk, climb).
> AI re-issues other movement each tick, so the shared immobilize adds a per-tick
> position **anchor** (lock X/Z, cap Y) for the ranged-mob **strafe** (skeleton/stray/
> pillager/crossbow-piglin share one bow-attack goal) and the slime **hop** (jump
> impulse), and cancels `EntityTeleportEvent` for the enderman **teleport**.
> **Fliers (ghast/blaze/phantom) are NOT specially handled** — a frozen flier may
> drift; that is an accepted compromise, not a regression. The anchor tolerance
> (`ImmobilizePhysics.ANCHOR_DRIFT`) is a boot-tuned knob: too tight vibrates, too
> loose creeps; the target is "neither."

> **The +20%-damage-taken clause was cut, deliberately.** An early spec had Rooted
> also amplify damage taken. That reintroduces a **damage-taken multiplier** — the
> exact thing Phase 2A deleted (`multiplierAgainst` gone, damage is the flat
> amount, guarded by `damageIsTheAmountRegardlessOfElement`). Re-adding it would
> reopen the damage pipeline that was deliberately closed and redden that test.
> Statuses affect battlefield state, not damage math. Rooted immobilizes; it does
> not amplify. If damage amplification is ever wanted, it is its own deliberate
> decision to reopen 2A, not a rider on a status.

### Soaked — stacking slow (new: stack state)

Each stack slows by 10% **multiplicatively**: 0.9, 0.81, 0.729, … with a **floor of
60% max speed** (never slower than 0.6× base). This needs the one new status mechanic
in this tier:

- **Stack count as real state.** `Status` today carries `durationTicks` + an
  `amplifier` int, but no notion of "N stacks, evaluate a curve, clamp to a floor."
  Soaked needs the count tracked per-target and the slow recomputed as
  `max(0.6, 0.9^n)`.
- **Attribute cleanup is where the bug is.** Minecraft speed is a modifier on an
  attribute. As stacks rise/fall and the status expires, the *right* modifier must
  be applied and later **fully removed** — a modifier left behind is a mob
  permanently slow after Soaked ended. The acceptance test is not "it slows"; it's
  "after Soaked expires, base speed is exactly restored." Leaked modifiers are the
  failure mode.

### Freeze — cannot move or attack (new: attack suppression)

- **Cannot move** — same movement lock as Rooted.
- **Cannot attack** — the hard half. Mobs have no vanilla "cannot attack" effect,
  so this means intercepting and cancelling the mob's attack while frozen. New
  enforcement code, with edge cases (ranged mobs, AI still pathing). Scope this
  deliberately; it is the one non-trivial piece of the simple tier.

### Composition — the simple tier's one cross-cutting rule

Rooted, Soaked, and Freeze are **three different movement mechanics** (root, stacking
slow, freeze). A target can have more than one. They must compose sanely — a Soaked
*and* Frozen mob shouldn't have the two fighting over the speed attribute. Decide the
rule once: e.g. Freeze/Root set movement to zero and override Soaked's partial slow
while active; Soaked's modifier is suspended, not stacked on top. Pin this so
multi-status targets are predictable.

---

## THE PROPAGATION ENGINE — deferred milestone

Thorns and Ignite are **two configurations of one engine**, and that engine has
hazards that a one-line spec hides. Do not build this as content; build it as a
system, once, correctly, then configure the two statuses on top.

**The shared shape:** *a status that, on a trigger (damage or death), schedules a
source-attributed effect against nearby targets, where that effect may or may not
re-trigger the same status depending on one boolean.*

- **Thorns:** trigger = the target takes damage; effect = damage nearby; re-trigger =
  **no** (depth-1, terminal).
- **Ignite:** trigger = a Scorched target **dies to a Scorched attack**; effect = a
  delayed explosion damaging nearby; re-trigger = **yes** (chains).

### The four safety rules — load-bearing, state up front

A wrong implementation here doesn't feel bad, it **takes the server down**. The safe
version and the dangerous version look identical in a one-line spec ("targets explode
when ignited"). These four rules are the difference:

1. **Ignition is DEATH-GATED, never living-threshold.** A target ignites only when
   *killed* by a Scorched attack — never on repeated application to a living target.
   This is the primary loop-breaker: a target can only ignite once because it can
   only die once. The living-threshold version (an alive target explodes at N stacks,
   damages a neighbor who crosses their own threshold while alive, …) loops with
   everyone still alive and re-triggering. **Do not implement the living-threshold
   version.** Someone will be tempted; this rule is why not.

2. **Propagation is DELAYED and SERIALIZED, never instant.** An ignition fires N ticks
   *after* the death, via the existing scheduler (the same deferral primitive as the
   grenade's first-area-pulse fix, `FakeWorld.schedule` honoring `delayTicks`, and
   the region-thread scheduling). This unrolls the chain **through time**: A dies →
   half a second → A explodes → kills B → half a second → B explodes. Serializing in
   time is what makes "each fires once, against current state" trivial instead of a
   same-tick graph-resolution problem. It is also **better feeling** — a visible wave
   rolling through the pack, like Destiny ignitions, not a confusing screen-clear.

3. **Each effect resolves against a snapshot and fires EXACTLY ONCE.** This is the
   grenade-double-hit / on-kill-ordering bug class (the deferred NEXT.md note: "five
   area pulses all see a living target and all fire"). Death-triggered explosions are
   that note coming due. Decide who is dead *before* firing, fire once per death, and
   never let one death's explosion be counted by another's same-tick resolution. The
   delay (rule 2) makes this mostly free, but it must be explicit.

4. **Source is ATTRIBUTED THROUGH THE CHAIN.** The explosion's `casterId` is the
   **original attacker**, propagated through each ignition — so a Mage whose staff
   starts a 4-mob Ignite chain gets credit for all four. This extends the
   `casterId`-through-the-port work already built for direct casts. Without it,
   chained kills credit the exploding corpse, or nobody.

### The one global interaction rule — decide before building

*Does propagated / status-inflicted damage trigger OTHER statuses?*

With Unstable and Blighted cut, this shrinks to a tractable 2×2, but it must be
answered as a single global policy, not per-status:

- Can **Thorns** damage kill a Scorched mob and thereby **Ignite** it?
- Can an **Ignite** explosion trigger a nearby mob's **Thorns**?

Pick one rule and apply it everywhere: e.g. *propagated damage carries the attacker's
source and CAN trigger death-gated statuses (Ignite), but does NOT re-trigger the
propagating status itself (Thorns stays depth-1).* Whatever the choice, decide it
once, globally — answering it per-status is how a Fire/Nature mob pack detonates in a
way no one can follow.

### Tuning knobs (YAML feel-dials, once the engine exists)

- **Ignition delay length** — short = fast rolling cascade; long = slow, dramatic,
  interruptible chain. A feel dial.
- **Does the explosion count as a Scorched attack?** — the single boolean that
  controls whether chains propagate through explosions (yes = your intent) or only
  direct Scorched kills ignite (no = explosions terminal). Testable in isolation.

---

## Dependency order

1. **Simple tier** — Scorched (done), Rooted, Soaked (stack state), Freeze (attack
   suppression), plus the composition rule. Content pass + two small mechanics.
2. **Replay Ranger vs Mage** with status-bearing ability pools. The axis test.
3. **Propagation engine** — only if the simple tier proves element-statuses make
   classes feel distinct. Build the one parameterized engine with all four safety
   rules and the global interaction policy, then configure Thorns + Ignite.
4. **Deferred statuses** — Unstable, Blighted — revisit with the propagation engine
   in hand and their terminators designed (tether definition; Blight spread cap).

## Forks left open, on purpose

- **Multi-movement-status composition** — the exact override rule when Root/Soak/
  Freeze coincide. Pin during the simple tier.
- **Global "what triggers what"** — the propagation interaction policy. Decide before
  the engine.
- **Ignition delay + explosion-re-triggers** — feel dials, tune in play.
- **Attack-suppression scope** — how far Freeze's "cannot attack" reaches (melee only?
  ranged? AI pathing?). Pin during Freeze.
