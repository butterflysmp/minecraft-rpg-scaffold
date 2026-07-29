# PLAN — Death (Pass A): mob death, consuming `reachedZero`

Read `CLAUDE.md` first. Branch off `master` **after Pass 2 merges** (Pass 2 is on
`feat/damage-mob-to-player`, PR open, not yet in master) — or off the Pass 2 tip if you get ahead
of the merge. Small, contained: one new `HealthListener` on the seam. **Mob death only this pass**;
player death (the respawn lifecycle) is a deliberate follow-up.

## What this pass does

Finally consume `reachedZero` — the death hook built-unconsumed since 1a. When a mob's CUSTOM HP
crosses to 0, the mob actually dies (real vanilla death: animation, drops, XP, kill credit), instead
of sitting alive at the display floor. Player death stays deferred — a player reaching 0 still sits
at the floor alive (unchanged) this pass.

## The trigger is ready and already tested

- `HealthState.damage` returns `reachedZero` — **true only on the hit that crosses from >0 to
  exactly 0**, once, never re-firing on an already-0 target (unit-tested at the `HealthState` level).
- `CombatantStats.damage` carries it on the `HealthChange` (`:74`). Every combat path — melee and
  every ability (Ember Step, Rekindle) — routes through `applyDamage → stats.damage`, so mob death
  from abilities comes free.
- The seam fires **on the mob's owning thread** (`applyDamage → scheduler.onEntity(mob) →
  stats.damage → onChange`), so reading and killing the mob in the listener is legal.

## The change — one new `HealthListener`, added to the composite LAST

New paper class (e.g. `paper/.../health/MobDeathSystem.java`), a `HealthListener`:

```java
@Override
public void onChange(HealthChange change) {
    if (!change.reachedZero()) return;
    if (change.targetIsPlayer()) return;   // player death is the follow-up pass; a player sits at the floor, alive
    // On the mob's owning thread (applyDamage -> onEntity(mob) -> stats.damage -> onChange): killing here is legal.
    Entity target = /* resolve change.target() on this thread */;
    if (!(target instanceof LivingEntity mob) || mob.isDead()) return;

    // Kill credit: resolve the dealer -> a Player, via the Folia-safe Attribution pattern (null cross-region).
    Player killer = change.dealerIsPlayer() ? /* resolve change.dealer() to a Player */ : null;
    if (killer != null) mob.setKiller(killer);   // drops + XP + advancement credited to the killer

    mob.setHealth(0);                            // REAL vanilla death -- see "the trap" below
}
```

Wire it into the composite at `RpgPlugin` (`:133`), **last** so the heart bar / nameplate / popup
render the final state before the mob is killed:

```java
new CompositeHealthListener(healthSystem, nameplates, popups, mobDeath)
```

`CompositeHealthListener` already isolates a throwing listener from the store mutation and its
siblings, so a death-path glitch can't corrupt a live combat tick.

## The trap: you cannot kill by dealing damage

The naive `mob.damage(lethal, killer)` is **self-defeating** — our own `onPlayerMeleeAttack` rider
tokens every player→mob `EntityDamageByEntityEvent` down to 0.01, so a "lethal" damage kill gets
neutered and the mob survives. `mob.setHealth(0)` sets health directly, fires `EntityDeathEvent`
**without** an `EntityDamageByEntityEvent`, so it bypasses the rider entirely and produces a real
death. That is why the kill is `setHealth(0)`, not damage.

`setKiller(player)` before it credits the killer (drops, XP orbs, advancements). Bukkit's kill-credit
rules are finicky, so this is a **boot check**, not an assumption: if XP/loot don't land, the
fallback is a rider-exempt lethal damage (a cause the rider skips) — but try `setKiller` +
`setHealth(0)` first, it's the clean path.

## Reuse, don't rebuild

- **Cleanup is free.** `mob.setHealth(0)` → death → `EntityRemoveFromWorldEvent` → the existing
  `onEntityRemove` (`:110`) calls `onMobRemove`, which clears **both** the nameplate and the custom
  HP store. No new cleanup code; no leak.
- **Killer resolution** reuses the `Attribution.attributableSource` pattern (resolve the dealer UUID
  on the mob's thread, region-ownership-checked) — Folia-safe: an uncredited death cross-region
  rather than an illegal cross-region read. On Paper it always resolves.

## Invariants to hold (state, don't drift)

- **The token floors STAY.** `VANILLA_LIVE_FLOOR` (mob) exists so the 0.01 token can't kill on
  *non-lethal* hits. Death does NOT remove it — death is an explicit kill on the `reachedZero`
  transition only. A hit that doesn't zero custom HP must still leave the mob alive.
- **Player death is untouched.** The `targetIsPlayer` guard skips players; they still clamp at 0 and
  sit at the half-heart floor, alive, exactly as today. Do not kill players this pass.
- **`reachedZero` fires once.** Already guaranteed by `HealthState.damage` (only the crossing hit).
  The `mob.isDead()` guard is belt-and-suspenders against a second delivery.

## Scope — in / deferred (record in NEXT.md, don't drop)

- **IN:** mob death on custom-HP-zero (melee + ability), with killer credit and existing cleanup.
- **DEFER — player death.** The respawn lifecycle (release the floor on the lethal hit, vanilla
  death → respawn → reset custom HP to base 100) is the next death pass. NEXT.md line.
- **DEFER — environmental death.** Players are currently immortal to fall/fire/lava/drowning: those
  hit vanilla health, which is floored, so they never kill and never touch custom HP. Consistent with
  the existing "environmental → custom HP" gap; note it explicitly in NEXT.md so it's a known
  consequence, not a surprise, and revisit with the environmental-damage pass.
- **DEFER — custom death messages / drop tables / mob→mob kills.** Vanilla defaults this pass.

## Guarding & testability

The listener is Bukkit-coupled (`setHealth`/`setKiller`/entity resolution) → **boot-witnessed**, like
the other seam displays. The `reachedZero` trigger itself is already unit-tested at `HealthState`. If
a pure predicate falls out (`shouldKill(change) = reachedZero && !targetIsPlayer`), a small reddening
test over the `Kind`/`targetIsPlayer` cases is welcome (mirrors the popup's `shouldShow` test) — but
don't wrap Bukkit types to manufacture one. `core/` stays untouched (139 green).

## Boot verification — the real gate

- **Mob dies on custom-HP-zero:** `/rpg mobdamage` a mob to 0 (and a real ironblade/ability killing
  blow) → the mob actually dies (death animation, disappears), not stuck alive at the floor.
- **Killer credit (the finicky one):** a player's killing blow yields **XP orbs + normal drops +
  advancement** (kill a cow → beef/leather/XP to the player). If not, that's the `setKiller` fallback
  signal.
- **Ability kills:** Ember Step / Rekindle finishing a mob kills it, credited to the caster.
- **Cleanup:** no ghost nameplate after death; the store doesn't leak (`trackedCount` returns to
  baseline).
- **Non-lethal still doesn't kill:** a hit that doesn't zero custom HP leaves the mob alive (token
  floor holds).
- **Player unaffected:** `/rpg damage` a player to 0 → still does NOT die (sits at the floor); no
  accidental player death this pass.
- **Regression:** nameplate, popup, heart bar, player→mob and mob→player all still behave.

## Commit shape & scope guard

One commit: `MobDeathSystem` + the composite wire + NEXT.md notes (player death next, environmental
immunity). Small. If player death, environmental death, custom drop tables, or death messages are
proposed, **decline** — this pass is mob death on `reachedZero` via `setHealth(0)` with credit, reusing
existing cleanup. The rest are their own passes.
