# PLAN — Dash lift fix + the Ability Stone (click-cast test item)

Read `CLAUDE.md` and `PLAN-ember-step.md` first. Two fixes, ordered so the second
unblocks testing the first.

**Build order: Ability Stone FIRST, then the lift fix.** You cannot test the WASD
dash (or verify the lift) through `/rpg cast` — opening chat to type the command
kills your movement input, so a command can only ever produce the stationary
fallback. The Ability Stone is the instrument that makes casting-while-moving
possible; without it, the lift fix is untestable for the case that matters.

---

# PART 1 — The Ability Stone (build first)

## The realization: it's a weapon

A weapon trigger and an ability are already the SAME thing. Confirmed in the tree:
- `AbilitySchema`'s comment: *"a weapon trigger — which IS an ability's cast plus an
  input — reuses it."*
- `WeaponService.fire` → `abilities.fireTrigger(caster, binding.ability(), aim)` — a
  weapon trigger fires an ability through the ability path.
- A weapon trigger's YAML is `cast: / cost: / on_hit:` — identical grammar to an
  ability.

So the Ability Stone is **a weapon whose click triggers are the abilities you want
to test.** The click-to-fire-while-moving machinery already exists and is hardened:
the packet swing listener (left-click), the right-click handler, `WeaponFire`. No
new casting path, no new input handling. The Stone is content plus a `/rpg give`.

## Why a command can't do this

`/rpg cast ember_step` requires opening chat, which releases WASD — so the cast
always sees "no movement keys" and takes the look-direction fallback. The WASD
requirement is **physically untestable via command.** A held item cast by clicking
lets you hold WASD and click, so `getCurrentInput()` resolves to live movement. This
is the `swing_TEMP` / `/rpg apply` lineage: a testing affordance that reaches what
the existing tools can't.

## Two versions — build the cheap one

- **A. Stone-is-a-weapon (build this):** `ability_stone.yml` is a weapon whose
  left/right-click triggers inline the ability's cast. To test Ember Step, its
  left-click trigger IS Ember Step's dash cast + payload. `/rpg give ability_stone`,
  hold it, move, click → casts while moving. Reuses everything; near-zero new code.
  To test a different ability, edit the yml and `--refresh-content`.
- **B. Runtime `/rpg bind <ability> <slot>` (escalation, NOT now):** bind arbitrary
  abilities to the Stone's slots at runtime without editing yml. More flexible for
  testing many abilities fast, but real new work (a bind command, per-item mutable
  binding state in PDC). **Defer** — build A, and if you find yourself constantly
  editing the Stone's yml to swap abilities, THAT friction is the signal to build B.
  Cheap version first, escalate on felt need.

## The one thing to confirm before building

A weapon trigger inlines a cast (see `emberblade.yml`: `cast: type: melee …`). Ember
Step's cast is `type: dash`. Confirm the weapon loader accepts a `dash` cast in a
trigger the same way it accepts `melee`/`projectile` — i.e. that the trigger cast
grammar and the ability cast grammar are genuinely the shared `AbilitySchema`, so
`type: dash` "just works" in a weapon trigger with no special-casing. If weapon
triggers hardcode a subset of cast types, that's a small generalization to make
first. (The `AbilitySchema` comment implies they're shared — verify it.)

## Content

```yaml
# content/weapons/ability_stone.yml
id: ability_stone
display_name: "<light_purple>Ability Stone</light_purple>"
element: kinetic          # neutral; it's a tool, not a themed weapon
rarity: common
material: amethyst_shard  # or any distinct held item; opaque, per WeaponDefinition.material
triggers:
  left_click:             # cast the ability under test on left-click while moving
    cooldown_ticks: 0     # no gate — it's a test tool; retune per ability under test
    cast:
      type: dash
      distance: 12
      speed: 1.6
      lift: 0.4           # Part 2's new field
    on_hit:
      - { type: damage, amount: 8, element: fire }
      - { type: knockback, strength: 1.0 }
      - { type: status, status_id: scorch, duration_ticks: 60 }
      - { type: visual, visual_id: solar_lance }
```

This is a permanent dev tool (like `/rpg apply`), NOT a `_TEMP` fixture — it stays.
It is gated only by `/rpg give` needing the dev/give permission; it does not ship in
any kit, so a normal player never has it.

## Acceptance (Part 1)

- `/rpg give ability_stone` puts the stone in hand; holding + left-click casts the
  bound ability. **While holding WASD.** This is the whole point — confirm a moving
  click casts, not just a standing one.
- Boot: the stone casts Ember Step on click; the WASD-direction test (Part 2's real
  gate) is now runnable.
- The stone does not ship in any kit (grep kits for `ability_stone` → absent).

---

# PART 2 — The Dash lift fix

## The problem

A purely horizontal ground impulse barely moves (~half a block) because **Minecraft
ground friction eats horizontal velocity in the first tick or two.** The 12-block
distance assumed the impulse carries; on the ground it doesn't. This is the known
"horizontal launch from the ground is damped; the same impulse in the air carries"
quirk — it's why vanilla knockback/slime-bounce arc you UP-and-out.

## The fix: a vertical component

Add a small upward impulse so the caster lifts off the ground and the horizontal
velocity isn't friction-damped — the dash becomes a shallow arc, not a ground
scrape. The impulse becomes:

```
velocity = (horizontalDirection × speed) + (up × lift)
```

New `lift` field on the `Dash` cast, alongside `distance`/`speed`. A yml tuning
number:
- small `lift` → a low, skimming dash;
- larger `lift` → a more arcing leap.

Dial `lift` against `speed` on the `--refresh-content` loop until a flat-ground dash
actually travels ~12 blocks.

## Why it interacts with WASD specifically

A WASD (moving) dash is **horizontal** — it has no upward look-pitch to save it from
ground friction, so it needs the lift MORE than a standing look-up dash does. A
standing dash that follows look-pitch can already gain height by aiming up; the
moving dash can't, which is exactly the case that was landing at half a block. The
lift is what makes the WASD dash carry.

## Change

- Add `double lift` to `record Dash(double distance, double speed, double lift)` in
  `core` (the sealed-switch sites for `Dash` are already there from Ember Step; this
  is a field add).
- The paper `Dash` executor adds `+ up × lift` to the impulse it already applies.
- `ember_step.yml` and `ability_stone.yml` gain `lift: 0.4` (starting guess — tune).

## Acceptance (Part 2)

- Boot via the Ability Stone (so you can move + click): a flat-ground WASD dash
  travels ~12 blocks, not half a block — tuned via `lift`/`speed` on the loop.
- The two direction gates, now actually runnable: strafe-left while facing an enemy →
  dash LEFT (reads movement); stand still → dash where facing (look fallback, may
  arc up with look-pitch).
- Still: passes through mobs, stops sensibly at walls, payload lands on the path,
  players untouched, caster unharmed, cooldown/cost gate.
- No core test changes needed for `lift` beyond the field threading through — it's a
  physics tuning value, feel-verified at boot, not unit-asserted (you can't unit-test
  "12 blocks feels right," same as the impulse itself).

---

## Rules

- The Stone reuses the weapon trigger path — no new casting code. If it needs new
  casting code, the "it's a weapon" premise is wrong; stop and reconsider.
- `lift` is `core` grammar on the `Dash` record; the impulse math is `paper`.
- Sealed `CastSpec` switch stays exhaustive — `Dash` already handled; `lift` is a
  field, not a new variant.
- Both the Stone and `lift` are tuning-first: numbers in yml, dialed on the
  `--refresh-content` loop, not hardcoded.
- Build Part 1 first — Part 2 is untestable without it.
