# PLAN — Ember Step (Fire Mage dash)

Read `CLAUDE.md`, `DESIGN-status-effects.md` first.

**Ember Step:** the Fire Mage dashes forward ~12 blocks, knocking back, damaging,
and scorching enemies in the dash path.

This is **the first ability that moves the caster.** That is the whole plan. The
damage/knockback/scorch payload is content you already have; the dash is a new
engine capability with a real design fork inside it.

---

## What exists vs what's new

Confirmed against the tree:

- **Cast shapes today:** `Self`, `Melee(reach, arc)`, `Ray(range)`,
  `Projectile(speed, gravity, lifetime)`. **None move the caster.** `Self` applies
  an effect to a stationary caster; the rest target outward. A dash is a new
  `CastSpec`.
- **Effects today:** `Damage`, `Knockback`, `Status`, `Burst`, `Area`, `Visual` —
  all exist. `solar_grenade` already composes `Damage` + `Status(scorch)` inside a
  `Burst`, which is exactly Ember Step's payload. **No new effect type needed.**
- **Caster movement:** nothing in the engine sets a player's velocity or position.
  The dash is genuinely new, and it is Bukkit-facing (moving a `Player`), so the
  *mechanism* lives in `paper`; the *cast declaration* is a `core` `CastSpec`.

So Ember Step = **one new cast (`Dash`) + a payload of existing effects applied to
whoever the dash passes through.**

---

## THE MECHANISM — locked

Decided in discussion; no fork remains. **Velocity impulse, direction from WASD,
look-direction fallback when stationary.**

### The dash is a velocity impulse
`player.setVelocity(direction × speed)` — once. Vanilla physics carries the Mage:
walls stop them, ledges drop them, momentum arcs them. 12 blocks is the *intended*
distance on flat open ground; actual distance varies with terrain. One call, no
custom collision — physics handles walls/ledges for free.

> **Named tradeoff (not a bug to fix later):** an impulse is momentum, so distance
> is approximate and the caster is briefly ballistic. "Fast and controlled" was the
> ask; a velocity dash is fast and *physical* rather than exact. If the boot feels
> "went further than I meant," that is the impulse mechanism, and the fix is tuning
> `speed` down — not a defect. Exact-distance would require the controlled-
> translation mechanism, which was deliberately not chosen (a precise glide can feel
> sterile; a lunge has weight).

### Direction comes from WASD movement, not look direction
The dash goes the way the player is **moving** (WASD input), not the way they are
**looking** — so facing an enemy while pressing strafe-left dashes you *left* while
still aimed at them. This is what makes Ember Step an evasive *step*, not a *charge*.

- Read actual input via **`Player.getCurrentInput()`** — available on this Paper
  (api-version 26.1; the input API landed well before it). It gives the real WASD
  key states (forward/back/left/right), not a velocity guess.
- Convert to a world-space direction relative to facing: forward-key → facing
  direction, strafe keys → perpendicular, combined and normalized. Then
  `direction × speed` is the impulse.

### Stationary fallback — look direction
When no movement keys are pressed, `getCurrentInput()` is all-false and there is no
movement direction. **Fall back to look direction** — a standing Ember Step lunges
where the caster faces. WASD-direction is the enhancement when moving; the cast
always does something sensible when still, never whiffs.

### Where the pieces live
`getCurrentInput()` and `setVelocity` touch the Bukkit `Player`, so **direction
resolution and the impulse are `paper`.** The `Dash` **cast** in `core` is
direction-agnostic — it is handed an already-resolved direction + distance, so core
never imports Bukkit and the WASD/look logic stays paper-side and boot-tested.

## Hit detection — "enemies in the way"

The payload hits enemies the dash *passes through*. With a velocity impulse the
*actual* path is ballistic (it may arc), so hit detection uses the **intended**
line, not the wobbly real one:

- **Sweep the intended line (recommended):** at cast, compute the line from the
  caster along the *resolved dash direction* (WASD or look) for `distance` blocks,
  and apply the payload to every enemy within ~1.5 blocks of it. Deterministic,
  computed once, independent of the caster's actual ballistic path. Matches the
  grenade's model (compute affected set, apply payload to each) and reuses the
  `Burst`-style set application the engine already has. The intended and actual
  paths are close enough over a short dash that this reads correctly.
- **Per-tick proximity during the dash:** while dashing, each tick hit nearby
  enemies. More faithful to actual path (esp. Option A's ballistic arc) but needs
  de-dup (an enemy you're next to for 3 ticks must be hit once, not thrice — the
  grenade's snapshot/fire-once discipline) and only works with a per-tick dash.

**Recommendation: sweep the line, computed at cast.** It sidesteps de-dup entirely
(each enemy is in-or-out of the swept volume once), it's deterministic and testable
in `core` (given a line and entity positions, who's hit is pure geometry), and it
doesn't care whether the caster's actual physics path wobbled. The payload —
`Damage` + `Knockback` + `Status(scorch)` — applies to that set, exactly like the
grenade's burst applies to its radius set.

---

## The new `CastSpec.Dash`

```java
record Dash(double distance, double speed) implements CastSpec {}
```

- `distance` — the intended dash length (~12), used for the swept hit-line.
- `speed` — the impulse magnitude (`direction × speed` → `setVelocity`).
- **Direction is NOT in the cast** — it is resolved paper-side at cast time
  (WASD via `getCurrentInput()`, else look direction) and handed to the executor.
  Core stays direction-agnostic and Bukkit-free.

Adding it to the sealed `CastSpec` forces the exhaustive switch in `CastExecutor`
(and anywhere else that switches on cast shape) to handle it — compiler-guided, no
`default` arm. That is the safety the sealed hierarchy buys; use it.

`CastExecutor`'s new `Dash` arm, in `paper`: resolve the direction (WASD via
`getCurrentInput()`, else look), apply the velocity impulse to the caster, compute
the swept intended line, resolve the enemy set, apply the payload to each through
the same effect path the grenade uses. The **effect application is shared**
with existing casts — Ember Step must reuse `EffectApplier`/the payload path, not a
parallel one, so its `Damage`/`Knockback`/`Status` behave identically to every other
source.

---

## What's testable in core vs boot

- **Core (unit):** the swept-line hit geometry — given a start, a direction, a
  distance, and entity positions, the correct set is "hit." Pure geometry, no
  server, `FakeWorld`. A mutation shrinking the sweep radius drops an enemy → reddens.
  The payload composition (does a Dash cast carry Damage+Knockback+Status) is
  loader/schema-testable like every ability.
- **Boot (human, named):** the *feel* — does ~12 blocks read right, does it stop
  sensibly at a wall (Option A), does the knockback throw enemies the right way, does
  scorch land on everyone passed through. And the caster-movement thread/ownership:
  moving a player is Bukkit-on-the-owning-thread, so witness it behaves (no rubber-
  band, no desync) the way the swing hop was witnessed.

Distance-feel and wall behavior are **inherently boot** — you cannot unit-test
"12 blocks feels right." Name them; don't fake them.

---

## The content file (payload — the known half)

```yaml
# content/abilities/ember_step.yml
id: ember_step
display_name: "<gold>Ember Step</gold>"
element: fire
cooldown_ticks: 160          # tune in play
cost:
  resource: energy
  amount: 30                 # costed — Mage economy; tune in play
cast:
  type: dash
  distance: 12
  speed: 1.6                 # Option A impulse; tune in play
on_hit:                      # applied to each enemy in the swept path
  - type: damage
    amount: 8
    element: fire
  - type: knockback
    strength: 1.0
  - type: status
    status_id: scorch
    duration_ticks: 60
  - type: visual
    visual_id: solar_lance   # reuse an existing fire visual for now, or add one
```

All `on_hit` effects already exist. The `dash` cast type is the only new grammar,
and the loader/`ContentValidator` gain a `dash` case the same way every cast shape
is parsed.

---

## Scope & commit boundary

**This commit: the `Dash` cast (Option A impulse + swept-line hit) + Ember Step.**

- Do **not** build Option B's controlled-translation collision system unless the
  boot proves the impulse feels wrong.
- Do **not** add a new visual engine — reuse an existing fire visual; a dedicated
  Ember Step visual is a later polish file, not this commit.
- The dash is Fire-Mage content, but the `Dash` **cast** is general — any future
  ability (a Ranger dodge-dash, a Melee lunge) reuses it. Build the cast general,
  the ability specific. Same as `Immobilize` (mechanic) vs `Rooted`/`Freeze`
  (configs).

## Design forks to confirm before building

1. **Stationary fallback** — confirm: no WASD keys pressed → dash goes where the
   caster looks (not "no dash"). WASD-direction is the enhancement, look is the
   floor, so the cast never whiffs when still.
2. **Does the caster take the payload?** No — `on_hit` applies to *enemies* passed
   through, never the dasher. Confirm the swept-line set excludes the caster.
3. **Dash through mobs, or stop at them?** A velocity impulse passes *through* mobs
   (they're not collision for a flying player) — which is what "knocking back
   enemies in the way" implies (you pass through, they get flung). Confirm: the dash
   does not halt on an enemy, only on terrain.
4. **PvP:** does Ember Step's payload hit players, or mobs only? The statuses are
   mob-only today (Rooted/Soaked/Freeze), but Damage/Knockback/scorch could hit
   players. Decide the target filter — likely "enemies" = hostile mobs for now,
   consistent with the mob-only status scope.

## Acceptance

- **Core:** swept-line geometry test (correct enemy set for a given dash line);
  a mutation to the sweep radius reddens it. Loader test: `ember_step.yml` parses
  with the `dash` cast and the four-effect payload; `ContentValidator` accepts it,
  and an unknown cast type warns.
- **Boot:** cast Ember Step — the Mage lunges ~12 blocks; enemies along the path
  take damage, get knocked back, and are scorched; the dash stops sensibly at a wall
  and passes through mobs (does not halt on them); the caster is unharmed; energy is
  spent and cooldown gates recast. **WASD-direction check:** face an enemy and press
  strafe-left while casting — the Mage dashes LEFT, not toward the enemy (proves it
  reads movement, not look). **Stationary check:** stand still (no keys) and cast —
  the Mage dashes where it faces (the look fallback). Witness caster-movement is
  clean (no rubber-band/desync) — moving a player is Bukkit-on-the-owning-thread.

## Rules

- The `Dash` cast is `core` grammar; the caster-moving mechanism is `paper`. No
  Bukkit in `core`.
- Reuse `EffectApplier`/the payload path — Ember Step's effects must be the same
  code the grenade's are, not a parallel application.
- Sealed `CastSpec` switch stays exhaustive, no `default` arm.
- Distance/impulse/cooldown/cost are tuning numbers — name them in the yml (done
  above) so they're dialed on the working `--refresh-content` loop, not in Java.
