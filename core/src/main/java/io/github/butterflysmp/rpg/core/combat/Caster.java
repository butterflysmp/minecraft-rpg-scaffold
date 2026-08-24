package io.github.butterflysmp.rpg.core.combat;

import java.util.UUID;

/**
 * The caster as a SOURCE of effects: an identity, plus the stats an effect needs when it lands.
 * Frozen at cast time, on the caster's own thread.
 *
 * This deliberately is NOT a {@link CombatantSnapshot}, though it is projected from one. A snapshot
 * describes a combatant as a TARGET -- where it is, whether it is alive, whether it is a player --
 * and those are facts about the cast frame. An effect can outlive that frame by a long way: a
 * projectile flies for up to a hundred ticks, and a lingering {@code Area} pulses for seconds after
 * its caster has logged out. Threading the whole snapshot down would put a stale position and a
 * stale liveness flag in reach of code running much later, where they read as current and are not.
 * This record carries only what stays TRUE about the caster-as-source once frozen.
 *
 * Which is also why the stats live here rather than being re-read at impact. {@code attackDamage}
 * is the caster's resolved ATTACK_DAMAGE, captured under {@code Regions.requireOwned} on the thread
 * that owns the caster. A projectile's impact resolves on the TARGET'S region -- cross-region from
 * the caster on Folia -- so reading the store there would be exactly the off-thread read the
 * snapshot/handle split exists to prevent. Melee reads the same frozen value: cast is effectively
 * hit for a swing within reach, so unifying costs nothing and leaves ONE path rather than two with
 * the race half-present.
 *
 * It is the extension point too. Class-typed damage modifiers (a {@code +N Ranged Damage} that
 * applies only while the held weapon's class matches) need more caster-side facts at impact; they
 * become fields here, not a third and fourth parameter on every method that threads a caster.
 */
public record Caster(UUID id, double attackDamage) {

    /** Project a cast-time snapshot down to what an effect landing later is allowed to read. */
    public static Caster of(CombatantSnapshot snapshot) {
        return new Caster(snapshot.id(), snapshot.attackDamage());
    }
}
