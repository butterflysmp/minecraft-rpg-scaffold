# PLAN — `/rpg apply` : a dev command to test statuses in isolation

Read `CLAUDE.md` first. Small commit. Its job: turn "I can't figure out how to
trigger this status" into "one command applies any status at any stack count to the
mob I'm looking at," permanently, for every status test from here on.

Grounded in the tree: `RpgCommand` is Brigadier subcommands, each gated by
`.requires(source -> source.getSender().hasPermission(Permissions.X))` and an
`instanceof Player` check. Statuses are applied via
`BukkitCombatant.applyStatus(statusId, durationTicks, amplifier)` — where
`amplifier` is the stack/level parameter.

---

## Why this exists

The class/element gate is correct for players and **in the way for developers**.
Testing Soaked means first satisfying: pick a class, pick an element, have a kit
that grants an ability whose `onHit` applies Soaked, then cast it — and even then
you can't control stacks or duration. Every status (Rooted needed `rooted_TEMP`,
Soaked needs a trigger, Freeze will, every propagation status will) hits this wall.
Wiring a fresh `_TEMP` fixture onto the grenade each time is the slow way.

One permanent, permission-gated dev command replaces all of it. It is the
`swing_TEMP` / `rooted_TEMP` pattern generalized into a real tool instead of a
throwaway fixture — and unlike those, it *stays*, because a status-applier is
useful for the life of the project.

**Scope decision:** build the **status applier** (`/rpg apply`) now, not a general
ability-caster. The immediate need is testing *status mechanics* — the slow, the
stacks, the floor, the cleanup — and going through an ability adds a variable
(which ability, which cell, its cost/cooldown) you don't care about when you're
testing the status itself. Direct status application with controllable stacks and
duration is exactly the instrument the Soaked boot needs. A `/rpg testcast <ability>`
is a reasonable *later* sibling for the content pass; it is not this commit.

---

## The command

```
/rpg apply <statusId> [durationTicks] [stacks]
```

- **`<statusId>`** — required. A status id (`soaked`, `rooted`, `scorch`, …). Tab-
  completes from the loaded `StatusRegistry` (`statuses.all()`), so you can only
  name a real one and you can see what exists.
- **`[durationTicks]`** — optional, default e.g. 100 (5s). How long it lasts.
- **`[stacks]`** — optional, default 1. Maps to `applyStatus`'s `amplifier`
  parameter — which is exactly the stack/level input Soaked reads. For Soaked,
  `stacks=5` applies five stacks so you can watch the slow deepen toward the floor.

**Target:** the entity the player is looking at (raycast), mob-only — consistent
with the statuses being mob-only. If the player isn't looking at a valid living
entity, a clear message ("look at a mob to apply a status"), not a silent no-op.

**Applies via the existing seam.** The command resolves the target, wraps it the
same way combat does (`BukkitCombatant` / the `applyStatus` path), and calls
`applyStatus(statusId, durationTicks, stacks)`. It reuses the exact application code
abilities use — so what you test through `/rpg apply` is the *same* code path a real
ability triggers, not a parallel one. That is the property that makes it a valid
test instrument: if `/rpg apply soaked` behaves, a real Soaked ability behaves,
because they call the same `applyStatus`.

---

## Gating — it must never reach players

- **Permission:** a new `Permissions.DEV` (or reuse `Permissions.ADMIN` if that's
  the intended "operator/dev only" tier — check which fits the existing scheme).
  Gate the subcommand with `.requires(... hasPermission(Permissions.DEV))`, same
  pattern as every other subcommand. Default-deny: a normal player does not have it.
- **This is a dev/testing affordance, not a game feature.** It bypasses the entire
  class/element/kit gate on purpose. That is exactly why it must be permission-
  locked — an ungated `/rpg apply` lets any player inflict any status on any mob,
  which is not the game. Permission is the wall.
- Mirror the existing subcommands' `instanceof Player` handling: a console can't
  raycast a target, so console use gets a clear "players only" message.

---

## What this is NOT

- **Not** a status *definition* — it applies existing statuses, adds none.
- **Not** an ability caster — no cost, no cooldown, no cast shape. It calls
  `applyStatus` directly. (That's the point: isolate the status from the ability.)
- **Not** a fixture that comes out later — unlike `rooted_TEMP`, this stays. So it
  does **not** touch any ability's content; `rooted_TEMP` on the grenade is a
  separate thing and its own removal is still owed in the content pass.

---

## Acceptance

- `/rpg apply soaked 100 5` on a looked-at mob applies Soaked at 5 stacks for 5s —
  and the mob visibly slows, deepening toward the ~60% floor, then returns to exact
  normal speed on expiry. **This command is what makes that boot observation
  runnable** — it's the instrument for Soaked's actual acceptance test.
- `/rpg apply rooted` roots the looked-at mob (confirms the command works against
  the already-merged Rooted, not just Soaked).
- Tab-completion lists exactly the loaded status ids; an unknown id gives a named
  error, not a stack trace.
- A player **without** the dev permission gets "unknown command" / no access —
  confirm the gate holds (a permission test, or a boot check with a non-op player).
- Console (no target) gets a clear players-only message.

## Small unit coverage

The command is thin (parse args → raycast → `applyStatus`), mostly Bukkit-facing,
so most of it is boot-verified. But the **arg parsing** (defaults applied, stacks →
amplifier mapping, bad number handling) is server-free logic worth a small test, so
`/rpg apply soaked` (no numbers) provably means `soaked, 100, 1` and not a crash or a
wrong default.

## Rules

- Reuse the existing subcommand + permission pattern in `RpgCommand`; don't invent a
  parallel command structure.
- Reuse `applyStatus` — do not write a second status-application path. The whole
  value is that it exercises the same code an ability does.
- Permission-gate it. An ungated status-applier is not a game feature; it's an
  exploit. The gate is not optional.
