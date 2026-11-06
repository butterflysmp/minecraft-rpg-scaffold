# PLAN — Death (Pass B): player death + respawn lifecycle

Read `CLAUDE.md` first. Branch off `master` **after Death Pass A merges** (Pass A is on
`feat/death-mob`, PR open, not yet in master) — or off the Pass A tip if you get ahead of the merge.
`git checkout -b feat/death-player`. This is the deferred half of death: a player whose custom HP
hits 0 finally dies, with a respawn lifecycle. Heavier than the mob side — the respawn reset is the
real work.

## Decision locked (death cost)

**Keep inventory + XP.** Death is a setback (respawn, position reset), not a loot loss. A
`PlayerDeathEvent` handler forces `keepInventory` + `keepLevel` and clears the drop/exp lists.

## What's already true (don't rebuild)

- `reachedZero` fires for players too; `MobDeathSystem` skips them (`!targetIsPlayer`). Today a player
  at 0 clamps and sits at the half-heart floor, alive.
- The half-heart floor is a **display floor** in `HeartBarRenderer` (`MIN_LIVE_HEALTH_POINTS = 1.0`),
  and its javadoc literally anticipates this pass: *"Death arrives with the next-phase damage system;
  until then the bar is purely cosmetic and never lethal."* The floor is CORRECT and **stays** — it
  stops a *display write* from killing on a non-lethal hit. Death is a separate explicit `setHealth(0)`,
  not floor removal.
- `PlayerHealthSystem` owns the player lifecycle: `onJoin` (register base 100, render, start the
  reconcile loop), `onQuit` (clear), and `onChange` (render the heart bar, hopping to the player's
  thread). `RpgListeners` wires join (`:84`) and quit (`:175`) — **there is no respawn handler.**
- The reconcile loop and the nameplate-viewer loop both run on `EntityTaskTarget`
  (`isActive = isValid() && !isDead()`), so **both self-cancel during the death screen.**

## The three parts

### 1. Kill on `reachedZero` — fold into `PlayerHealthSystem.onChange`

Do NOT add a separate death listener here. `onChange` already renders on every change; on the
`reachedZero` change a render (floored to half a heart) and a kill would race. Put the decision in one
place — if `reachedZero`, kill instead of render:

```java
@Override
public void onChange(HealthChange change) {
    if (!change.targetIsPlayer()) return;
    Player player = Bukkit.getPlayer(change.target());
    if (player == null) return;
    if (change.reachedZero()) {
        scheduler.onEntity(player, () -> player.setHealth(0));   // real death; no floor render competes
        return;
    }
    scheduler.onEntity(player, () ->
            renderer.render(new EntityHeartBar(player), change.newCurrent(), change.max()));
}
```

`setHealth(0)` fires a normal `PlayerDeathEvent` → death screen. Runs on the player's owning thread
(`onChange` is already there via `applyDamage → onEntity(player)`); the extra `onEntity` hop is
harmless and keeps the "emitted from any thread" contract. No re-entrancy concern: the
`PlayerDeathEvent` handler only sets flags (below), it doesn't touch the store.

### 2. Keep-inventory `PlayerDeathEvent` handler (in `RpgListeners`)

```java
@EventHandler
public void onPlayerDeath(PlayerDeathEvent event) {
    event.setKeepInventory(true);
    event.getDrops().clear();
    event.setKeepLevel(true);
    event.setDroppedExp(0);
}
```

Global (all player deaths keep inventory) — the only path that kills a player right now is our
`setHealth(0)`, so scoping it tighter buys nothing. Import `org.bukkit.event.entity.PlayerDeathEvent`.

### 3. Respawn — reset HP AND restart the loops death tore down (the real work)

New `PlayerRespawnEvent` handler in `RpgListeners` that re-runs the per-entity setup death cancelled:

```java
@EventHandler
public void onPlayerRespawn(PlayerRespawnEvent event) {
    healthSystem.onRespawn(event.getPlayer());     // reset custom HP to full + render + RESTART reconcile loop
    nameplates.onViewerJoin(event.getPlayer());    // RESTART the nameplate LOS loop (also self-cancelled on death)
}
```

`PlayerHealthSystem.onRespawn(player)` mirrors `onJoin` — `stats.register(id, DEFAULT_PLAYER_BASE,
true)` (fresh at full base), render, and **`startReconcileLoop(player)`** (the loop is dead; without
this, gear +HP is never tracked again). Equipment headroom re-applies on the loop's next tick — the
bar may briefly show full base then dip as gear reconciles; acceptable, note at boot.

Do NOT reload the profile on respawn — it persists across death; only the entity-bound loops need
restarting.

## Invariants / subtleties (state, don't drift)

- **The display floor STAYS.** It's correct for non-lethal renders; death is `setHealth(0)`, not floor
  removal. A hit that doesn't zero custom HP still leaves the player alive at half a heart.
- **Both loops MUST restart on respawn.** The reconcile loop AND the nameplate-viewer loop self-cancel
  on the death screen (`!isDead()` in `EntityTaskTarget`). This is the easy-to-miss bug — a respawned
  player would otherwise lose gear-bonus tracking and stop seeing mob nameplates.
- **`reachedZero` fires once** (guaranteed by `HealthState.damage`), so the kill triggers once.
- **`onQuit` does NOT run on death** (death ≠ logout), so custom HP stays at 0 until `onRespawn`
  resets it — the respawn handler owns the reset.

## Scope — in / deferred

- **IN:** player death on custom-HP-zero (from mob hits or `/rpg damage`), keep-inventory/XP, full
  respawn lifecycle (HP reset + both loops restarted).
- **DEFER — environmental → custom HP.** Environmental damage (fall/fire/lava) still acts on vanilla
  health, decoupled from custom HP — the existing recorded gap, unchanged by this pass. Don't try to
  route it here.
- **DEFER — custom death messages, post-death debuffs, PvP death rules.** Not this pass.

## Guarding & testability

Boot-witnessed — the kill, keep-inventory, respawn reset, and loop restarts are all Bukkit-coupled and
can't be faked. The `reachedZero` trigger is already unit-tested at `HealthState`. No pure predicate
falls out cleanly (the kill decision is woven into `onChange`); don't manufacture a test by wrapping
Bukkit types. `core/` untouched (139 green).

## Boot verification — the real gate

- **Player dies on custom-HP-zero:** let a mob drain a player to 0 (and `/rpg damage` to 0) → the
  player actually **dies** (death screen), not stuck at the half-heart floor.
- **Keep inventory + XP:** on death the player keeps all items and XP — nothing drops, level intact.
- **Respawn resets to full:** after respawn the heart bar is full and custom HP is back at base (100).
- **Loops restart (the subtle one):** after respawn, equip a +HP item → max rises (reconcile loop
  alive); look at a mob → its nameplate shows (viewer loop alive). Both would be dead if respawn didn't
  restart them.
- **Non-lethal unaffected:** a hit that doesn't zero custom HP still leaves the player alive at the
  floor; the bar renders normally.
- **Regression:** mob death (Pass A), mob→player, player→mob, nameplate, popup all still behave.

## Commit shape & scope guard

One commit: `PlayerHealthSystem` (onChange kill branch + `onRespawn`) + `RpgListeners` (death +
respawn handlers) + NEXT.md (player death done; environmental gap still open). Small-ish. If custom
death messages, environmental-death routing, or post-death penalties are proposed, **decline** — this
pass is player death on `reachedZero` with keep-inventory and a correct respawn lifecycle.
