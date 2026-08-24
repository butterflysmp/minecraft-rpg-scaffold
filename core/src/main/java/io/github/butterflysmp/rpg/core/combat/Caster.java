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
 * {@code classDamageBonus} is the class-typed damage modifier pass this record's javadoc used to
 * PREDICT, now landed. It is the sum of the caster's equipped {@code +N <Class> Damage} gear whose
 * class matches the class of the weapon they hold, and {@code EffectApplier} adds it to BOTH direct
 * damage arms -- the {@code WeaponDamage} arm's stat and the literal {@code Damage} arm's authored
 * amount alike. Being a field here rather than a parameter is what the prediction was about: every
 * method that threads a caster already carries it, and the cast-time freeze that makes
 * {@code attackDamage} Folia-safe covers it for free.
 *
 * Two things it deliberately is NOT. It is not the weapon's inherent damage -- that stays in
 * ATTACK_DAMAGE (or in the literal), so the bonus adds on top rather than replacing, and the
 * emberblade's fireball can take +Melee without inheriting the swing's 8. And it is not gated on
 * whether the payload is a basic attack: the gate is the HELD WEAPON'S CLASS, which is what lets the
 * bonus reach a literal-damage weapon like {@code ember_staff} that reads no stat at all.
 */
public record Caster(UUID id, double attackDamage, double classDamageBonus) {

    /** Project a cast-time snapshot down to what an effect landing later is allowed to read. */
    public static Caster of(CombatantSnapshot snapshot) {
        return new Caster(snapshot.id(), snapshot.attackDamage(), snapshot.classDamageBonus());
    }
}
