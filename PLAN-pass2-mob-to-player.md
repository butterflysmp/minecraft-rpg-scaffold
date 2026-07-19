# PLAN — Pass 2: mob→player custom damage (ride the vanilla event)

Read `CLAUDE.md` first. Branch off `master` (`b3de9ba`, 1a+1b merged). This is the ONE place
the ride-the-event pattern literally applies — a mob hitting a player fires a real
`EntityDamageByEntityEvent` we ride for cosmetics and own the mechanics of. Small, contained:
one new handler mirroring `onPlayerMeleeAttack` in reverse. Resist scope creep.

## Decision locked (the i-frame fork)

**Preserve vanilla i-frames.** We ride only the events vanilla actually fires, so the player's
invulnerability windows are preserved *automatically* — no `noDamageTicks` touching, no bypass
machinery. A player can be hit at most once per ~0.5s window regardless of swarm size. This
isolates the mob→player PIPELINE from the i-frame FEEL: the swarm-melt bypass (reset/shorten the
window) is a deliberate later tuning fork, not this pass. **Do not reset or shorten i-frames here.**

## What's already true on master (don't rebuild it)

- `RpgListeners.onPlayerMeleeAttack` (`:207`) and `onCombatKnockback` (`:228`) handle **player→mob
  only** and explicitly skip the player-victim case (`// mob->player is Pass 2`). Constants:
  `TOKEN_DAMAGE = 0.01` (`:51`), `VANILLA_LIVE_FLOOR = 1.0` (`:57`).
- `onFrozenMeleeAttack` (`:189`) cancels the event when the *damager* is frozen — a frozen mob
  already deals nothing, so the new handler must not re-process a cancelled event (see below).
- `BukkitCombatant.applyDamage(double, UUID)` (`:77`) is the custom-damage path and **reuses
  cleanly for a player victim** — verified: the aggro block guards on `entity instanceof Mob` (a
  player won't aggro), `dealerIsPlayer` resolves false for a mob source, and the
  `noDamageTicks == 0` flash gate skips the manual flash because the vanilla hit already flashed the
  player. So the handler just tokens the event and calls `applyDamage` on the player.
- Players are registered in `CombatantStats` (base 100) with the heart bar on the seam. The store
  side is ready; this pass drains it.

## The change — one new handler in `RpgListeners`

Mirror `onPlayerMeleeAttack`, victim/damager reversed. `ignoreCancelled = true` so a frozen mob's
cancelled hit is not processed:

```java
/**
 * Ride a MOB's melee hit on a player: keep vanilla's cosmetics (flash/sound/i-frames), own the
 * mechanics. Token the vanilla damage so the player's vanilla hearts barely move and the token
 * can't kill (death is deferred), then drain the player's CUSTOM HP via applyDamage. i-frames are
 * PRESERVED -- we ride only what vanilla fires, so we touch noDamageTicks nowhere.
 *
 * Amount bridge: the mob's vanilla attack damage (event.getDamage(), BASE, pre-token) IS the custom
 * amount, until mob attack-damage becomes a custom stat (a later pass) -- the mob analog of
 * bootstrapping mob HP from vanilla MAX_HEALTH. No vanilla armor/reduction baked in (we own that
 * later); read getDamage(), not getFinalDamage().
 */
@EventHandler(ignoreCancelled = true)
public void onMobMeleeAttack(EntityDamageByEntityEvent event) {
    if (!(event.getEntity() instanceof Player victim)) return;          // mob->player only
    if (!(event.getDamager() instanceof LivingEntity attacker)) return; // a living melee attacker
    if (attacker instanceof Player) return;                             // player->player is a later rules decision

    double incoming = event.getDamage();          // vanilla attack damage = the custom amount (interim bridge)
    event.setDamage(TOKEN_DAMAGE);                // ride: keep flash/sound/i-frames, no double, can't kill
    BukkitCombatant.of(victim, adapters).handle().applyDamage(incoming, attacker.getUniqueId());
}
```

That is the whole mechanical change. `applyDamage` (deferred to the player's thread) drains custom
HP, fires the seam (→ heart bar drops), and its flash gate self-suppresses the double.

## Two deliberate choices to state (not silently adopt)

- **Knockback: keep vanilla on mob→player.** `onCombatKnockback` cancels vanilla ATTACK knockback
  only for player→mob (weapon-declared KB is owned there); it already skips players. **Leave it
  unchanged.** Mobs have no declared KB spec, so cancelling here would make mob hits push the player
  zero — worse than vanilla. Vanilla mob-hit knockback is a fine default until a mob-attack-KB stat
  exists. Caveat to check at boot: tokening damage to 0.01 may slightly weaken vanilla's
  damage-scaled KB component — if a zombie hit feels like it barely nudges the player, that's the
  signal to own mob→player KB in a follow-up, not now.
- **Token-can't-kill the PLAYER.** The 0.01 token is a real vanilla damage event on the player. The
  player heart bar already floors vanilla health at ~half a heart (a display write must not kill a
  live player), which is >> 0.01, so the token cannot kill. Do NOT add a new floor unless boot
  disproves this — but **boot-verify it explicitly** (below): a near-dead player hit by a mob must
  survive (death is the next pass).

## Scope — in / deferred (record, don't drop)

- **IN:** melee mob→player (a `LivingEntity` damager, `Player` victim).
- **DEFER — mob projectile→player.** A skeleton's arrow fires `EntityDamageByEntityEvent` with the
  *arrow* (a `Projectile`) as damager, not the mob → the `LivingEntity` check skips it. Owning ranged
  mob damage needs shooter resolution off the projectile; its own follow-up, same isolate-risk
  discipline. Note it in NEXT.md.
- **DEFER — player→player (PvP).** A rules decision, not this pass (the handler skips a Player
  damager).
- **DEFER — environmental / DoT → player** (fall, fire, drowning, Scorch): those are
  `EntityDamageEvent`, not attacks, and relate to the existing "Scorch DoT / `applyHeal` bypass
  custom HP" gap. Out of scope; already recorded.

## Guarding & testability

Like `onPlayerMeleeAttack`, this is a Bukkit event handler — **boot-witnessed**, not unit-tested;
there's no pure logic to redden here (the amount bridge is a single `getDamage()` read). If a pure
seam falls out naturally (e.g. a `boolean isOwnedMobMelee(damager, victim)` predicate), a small
reddening test is welcome, but do not manufacture one — don't wrap Bukkit types just to have a test.
Core stays untouched (139 green); paper gains the handler.

## Boot verification — the real gate

- A mob hitting a player **drains the player's custom HP** by the mob's damage — the heart bar drops
  by that amount; vanilla cosmetics (red flash, hurt sound) are kept.
- **Not double-damaged:** the player's vanilla hearts don't plummet (token only) — the custom drop
  is the mechanical one, once.
- **i-frames preserved:** a mob can land at most one hit per ~0.5s window; standing in a mob's face
  does not multi-drain within the window.
- **Token-can't-kill:** take a player to near-zero custom HP (`/rpg damage`), then let a mob hit them
  — they **survive** (clamp at 0, `reachedZero` fires unconsumed; death is the next pass).
- **No popup on mob hits:** the dealer is a mob (`dealerIsPlayer` false), so no damage number pops —
  the heart bar is the witness. Confirm nothing stray appears.
- **Knockback feel:** the player still gets pushed by mob hits (vanilla KB kept); confirm it's not
  neutered by the token (see the KB caveat).
- **Regression:** player→mob is untouched — ironblade/abilities still drop the nameplate and pop
  numbers exactly as before.

## Commit shape & scope guard

One commit: the handler + the NEXT.md deferral notes (projectile / PvP / environmental). Small,
likely +15 lines. If a broader "unified damage-source router," mob-attack-KB, or the i-frame bypass
is proposed, **decline** — Pass 2 is melee mob→player riding the vanilla event, i-frames preserved,
vanilla KB kept. Death, ranged, and the swarm-melt fork are their own later passes.
