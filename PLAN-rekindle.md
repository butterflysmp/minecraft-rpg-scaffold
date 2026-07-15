# PLAN — Rekindle (Fire Ranger retreat)

Read `CLAUDE.md`, `PLAN-ember-step.md` first.

**Rekindle:** the Fire Ranger throws 3 embers in a fan in front of them, then dashes
straight backward. The embers arc out, land, and after a 1-second fuse each explodes
in a 4-block burst dealing damage and scorching. The dash goes opposite facing
regardless of WASD, at Ember Step's strength.

**The Ranger counterpart to Ember Step, and deliberately its opposite verb.** Ember
Step is Mage-*commit* (dash IN, aggressive). Rekindle is Ranger-*kite* (dash AWAY,
leave a denial zone behind). This contrast IS the kite-vs-commit distinction the
class-axis verdict turns on — expressed mechanically, not just in numbers.

---

## What exists vs what's new (the scan settled most of it)

Confirmed against the tree:

- **Ordered co-effects on cast:** `EffectApplier.applyAll(List<EffectSpec>)` iterates
  the list in order. So "throw embers THEN dash" is just a list `[throw, dash]` — the
  list order **is** the sequence. No new sequencing engine. *(Fork resolved.)*
- **Delayed burst at a location:** `Scheduler.onRegionLater(Location, Runnable,
  delayTicks)` already exists — the exact primitive for a 1-second fuse at the
  landing point. No new scheduler work.
- **The arc:** the `Projectile` cast already flies with gravity. Three embers = three
  projectiles at fixed angles.
- **The burst:** `Burst` (radius damage + scorch) exists — it's the grenade's blast.
- **The dash:** the `Dash` cast (impulse + lift + swept payload) exists from Ember
  Step. Rekindle reuses it with a new *direction mode*.

**So the genuinely new pieces are just two, and both are small:**
1. A **timed-detonator effect** — "on the ember's landing, spawn a display marker and
   schedule a burst here after 20 ticks." The delay + burst are reused; the only new
   bit is the display marker and wiring impact→fuse instead of impact→detonate.
2. A **reverse-facing direction mode** on the `Dash` cast — one branch in the
   existing direction resolver.

---

## Piece 1 — the Ember: an arc that plants a timed detonator

An Ember is a **projectile whose impact starts a fuse instead of detonating.** The
grenade detonates on impact; the Ember lands, sits, and bursts 1 second later. So:

1. **Arc** — reuse the `Projectile` cast: launch from the caster with gravity toward
   a point a few blocks out. Three of them, at facing + {0°, +25°, −25°}.
2. **Land** — the projectile's impact does **not** burst. Its impact effect is
   *"plant a timed detonator here"*: spawn the display marker + `onRegionLater(burst,
   20 ticks)`.
3. **Fuse** — a **display-only blaze-powder marker** sits at the landing point for
   the 1 second. Display-only per decision: an item-display (or equivalent), **no
   physics, no pickup, no despawn-timer** — it exists to show where the ember will
   go off, and is removed on detonation.
4. **Detonate** — after 20 ticks: a **4-block `Burst`** (damage + scorch), marker
   removed. **Pure timer** — it detonates whether or not anything is near. No contact
   detection at all (simpler: no impact-damage logic on the fuse).

**The one new effect: "delayed burst at a point with a display marker."** Everything
it composes (`onRegionLater`, `Burst`, damage, scorch) exists; the new code is
spawning/removing the marker and wiring the projectile's impact to schedule rather
than detonate. Make it a general effect (a `DelayedBurst` / `TimedDetonator`) — any
future "mine / delayed explosion / trap" reuses it, same general-mechanic /
specific-content split as `Immobilize` vs `Rooted`.

**Marker cleanup is the hazard** (same discipline as every timed thing): the display
marker MUST be removed on detonation AND if the world/chunk unloads or the burst is
somehow cancelled — a leaked display entity is the leak-on-death shape one more time.
The marker's lifetime is exactly the fuse; tie its removal to the same scheduled task
that fires the burst, so they can't diverge.

### The 3-ember spread

Three `Projectile` throws at facing yaw + {0°, +25°, −25°}, each a few blocks. This
is trivial once one ember works — compute three directions, fire the same
throw-an-ember three times. Angles and distance are tuning numbers in the yml.

---

## Piece 2 — the backward dash (reuse Dash + a direction mode)

Rekindle's dash reuses Ember Step's `Dash` cast entirely — same impulse, same lift,
**same strength** (per decision: equal to Ember Step, so the pair feels matched — 12
in for the Mage, 12 out for the Ranger). The only difference is **direction**:

- Ember Step: direction = WASD movement (else forward-flat).
- Rekindle: direction = **reverse of facing, ALWAYS**, ignoring WASD entirely.

A retreat dash that went sideways because you happened to be strafing would defeat
the purpose — it must always be a straight backpedal. So the `Dash` cast gains a
**direction mode**:

```java
// on the Dash cast or its resolution
enum DashDirection { MOVEMENT_ELSE_FORWARD, REVERSE_FACING }
```

- `MOVEMENT_ELSE_FORWARD` — Ember Step's existing behavior (WASD, else forward-flat).
- `REVERSE_FACING` — Rekindle: take facing yaw, flatten to horizontal (no vertical —
  the same no-dash-straight-up guarantee already tested for Ember Step applies), and
  reverse it. Ignore `getCurrentInput()` entirely.

The direction resolution is paper-side (it reads the `Player`); the mode is declared
in the cast (`core`). Reuse the **flatten-to-horizontal** logic already built and
unit-tested for Ember Step's fallback — REVERSE_FACING is that same flatten, negated.
So the "can't dash vertically" guarantee holds for free, and it's the same tested
code path.

**Confirm:** the reverse direction is also horizontal (zero Y), so a Ranger looking
up and casting Rekindle dashes back-and-flat, not down-and-back. Same tested rule as
Ember Step — extend the `DashAimTest` to cover REVERSE_FACING having zero Y.

---

## Ordering — embers first, then dash

Rekindle's cast is an ordered list of untargeted effects:

```
on_cast (applied in list order by applyAll):
  1. throw-embers   (spawn 3 arcing embers in the fan)
  2. dash-self      (reverse-facing dash at Ember Step strength)
```

Embers first so they launch from the caster's CURRENT position (in front of where
you're standing), THEN you retreat — you back away from the zone you just planted. If
the dash fired first, the embers would launch from your retreated position, which is
wrong. `applyAll` preserves list order, so authoring them `[embers, dash]` in the yml
gives the right sequence. **Confirm the throw reads the pre-dash position** (capture
the caster origin before the dash effect moves them — a snapshot, so the embers'
launch point isn't corrupted by the dash that follows in the same cast).

---

## Content

```yaml
# content/abilities/rekindle.yml
id: rekindle
display_name: "<gold>Rekindle</gold>"
element: fire
cooldown_ticks: 200          # tune
cost:
  resource: energy
  amount: 35                 # Ranger's costed utility; tune
cast:
  type: self                 # the caster is the origin; effects fire on cast
on_cast:                     # ordered — embers first, then dash
  - type: throw_embers       # the new spread-of-timed-detonators effect
    count: 3
    angles_degrees: [0, 25, -25]
    distance: 4              # a few blocks in front; tune
    marker: blaze_powder     # display-only
    fuse_ticks: 20           # 1 second
    burst:
      radius: 4.0
      effects:
        - { type: damage, amount: 8, element: fire }
        - { type: status, status_id: scorch, duration_ticks: 60 }
  - type: dash
    direction: reverse_facing
    distance: 12             # match Ember Step
    speed: 2.3               # match Ember Step's tuned value
    lift: 0.3                # match Ember Step's tuned value
```

(Exact schema is the loader's to define — this shows the shape. `throw_embers` /
`DelayedBurst` is the new grammar; `dash` + `direction` is the new mode on existing
grammar; everything inside `burst` is existing effects.)

---

## What's testable in core vs boot

- **Core (unit):**
  - the 3-ember spread geometry — given facing yaw and {0,+25,−25}/distance, the
    three landing directions are correct (pure trig). A mutation to an angle reddens.
  - REVERSE_FACING direction is horizontal (zero Y) and opposite facing — extend
    `DashAimTest`. A mutation leaking pitch, or not negating, reddens.
  - the embers' launch origin is the caster's PRE-dash position (snapshot), not
    corrupted by the dash — a mutation that reads post-dash position reddens.
- **Boot (human, named):**
  - the embers arc out in a fan, land, show blaze-powder markers, and burst after ~1s
    with damage + scorch in a 4-block radius; markers vanish on detonation (no leaked
    display entities — check the world after several casts).
  - the dash goes straight back, ~12 blocks, flat (not down) even looking up, at the
    same strength as Ember Step.
  - order reads right: embers appear in front, THEN you retreat from them.

---

## Scope & forks to confirm

- **This is Fire Ranger content + two small engine additions** (the timed-detonator
  effect, the reverse dash mode). Do not over-build: the embers are display-only
  pure-timer (no physics, no contact) by decision — keep them that way.
- **Confirm the projectile-impact-schedules-a-fuse path**: the ember reuses
  `Projectile`, but its impact must fire a *delay-scheduling* effect (plant detonator)
  rather than an immediate burst. Confirm the projectile impact can invoke a
  scheduling effect, not only immediate ones. If the projectile hard-wires immediate
  impact effects, that's the small generalization to make.
- **Marker cleanup** tied to the fuse task so display and detonation can't diverge.
- **Mob-only** — the burst's damage/scorch: scorch is already mob-only; confirm the
  burst's target filter matches Ember Step's (reuse the `CombatantSnapshot.player`
  exclusion so embers don't damage players, consistent with the mob-only scope).

## Acceptance

- **Core:** spread-geometry test, REVERSE_FACING-is-horizontal test, pre-dash-origin
  snapshot test — each with a reddening mutation.
- **Boot (via the Ability Stone so you can move + click):** 3 embers arc into a fan,
  land, mark, and burst after 1s (damage + scorch, 4-block); the caster dashes
  straight back ~12 blocks flat; embers-before-dash order reads; no leaked markers
  after repeated casts; players unharmed by the bursts.

## Rules

- Reuse `Projectile` (arc), `onRegionLater` (fuse), `Burst` (detonation), `Dash`
  (retreat) — the new code is the display marker + the reverse direction mode, not a
  parallel projectile/burst/dash.
- `DelayedBurst`/`throw_embers` and the dash `direction` mode are `core` grammar; the
  display-entity spawn and direction resolution are `paper`.
- Sealed switches stay exhaustive — new effect/mode arms, no `default`.
- Angles, distance, fuse, burst radius, dash numbers are yml tuning values on the
  `--refresh-content` loop.
- Marker lifetime = fuse lifetime; tie removal to the burst task. A leaked display
  entity is the leak-on-death shape — clean up on detonation and on world/chunk
  unload.
