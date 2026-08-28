package io.github.butterflysmp.rpg.core.ability;

import io.github.butterflysmp.rpg.core.ability.effect.DamagePayload;

/**
 * Is a trigger the VANILLA-DRIVEN basic melee hit -- the one the {@code EntityDamageByEntityEvent}
 * rider owns, and the one the arm-swing packet path must therefore refuse to fire?
 *
 * <p>This exists so that exactly one predicate answers that question, consulted from two places
 * with opposite senses: the swing path skips when it is true, the rider selects when it is true.
 * Split into two copies and the two failure modes are the ones this whole change was made to avoid
 * -- the 120-degree cone quietly coming back, or one click being processed twice. Same discipline
 * as {@link DamagePayload#isBasicAttack}, which the tooltip, the cooldown scaler and the durability
 * charge already share for the same reason.
 *
 * <p><b>Both halves of the conjunction are load-bearing, and shipped content proves each.</b>
 * <ul>
 *   <li>{@code hunters_bow}'s shot IS a basic attack (a {@code weapon_damage} payload) but its cast
 *       is a {@code Projectile}. Vanilla's melee attack cannot fire it, so dropping the cast check
 *       would silently stop the bow from ever shooting.</li>
 *   <li>{@code void_slash} IS a {@code Melee} cast but carries a LITERAL damage payload -- it is an
 *       ability, not a basic attack. Dropping the payload check would hand it to the rider, where
 *       it would lose the arc and the burst that make it a sweep, and fire on a crosshair hit it
 *       never paid mana for.</li>
 * </ul>
 *
 * <p>Which is also why the melee weapons keep their {@code reach} / {@code arc_degrees} block in
 * content even though vanilla now owns both. The block is not vestigial: {@code cast: type: melee}
 * is the discriminator this predicate reads. Delete it and the trigger goes back to the packet path.
 */
public final class BasicMelee {

    private BasicMelee() {}

    /**
     * True when {@code definition} is a basic attack (its payload deals the wielder's ATTACK_DAMAGE
     * stat rather than an authored literal) AND its cast is a melee arc -- i.e. the hit that a
     * vanilla crosshair attack now delivers.
     */
    public static boolean isVanillaDriven(AbilityDefinition definition) {
        return DamagePayload.isBasicAttack(definition.onHit())
                && definition.cast() instanceof CastSpec.Melee;
    }
}
