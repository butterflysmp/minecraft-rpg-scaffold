package io.github.butterflysmp.rpg.paper.content;

import org.bukkit.NamespacedKey;

/**
 * What a status id *means*, mechanically. Not how strong it is and not how long
 * it lasts: duration and amplifier come from the ability that applies it, via
 * EffectSpec.Status. A status definition maps an id to a mechanic.
 *
 * Sealed so BukkitCombatant.applyStatus switches exhaustively over the kinds.
 */
public sealed interface StatusDefinition
        permits StatusDefinition.Fire, StatusDefinition.Potion,
                StatusDefinition.Immobilize, StatusDefinition.Soaked {

    String id();

    /** A burn. Bukkit has no "burning" potion effect, so this drives setFireTicks. */
    record Fire(String id) implements StatusDefinition {}

    /**
     * A movement lock: MOVEMENT_SPEED to zero (kills the mob's AI drive) plus per-tick
     * velocity-zero (kills knockback/jumps). The two configurations of this one mechanic:
     *   - Rooted = {@code suppressAttacks=false} -- cannot move; can still turn and melee in range.
     *   - Freeze = {@code suppressAttacks=true}  -- cannot move OR attack; the listeners cancel a
     *              frozen mob's melee, projectiles, and creeper detonation.
     * The general kind name is deliberate -- Freeze reuses the immobilize rather than forcing a
     * second sealed kind. Duration comes from the ability.
     */
    record Immobilize(String id, boolean suppressAttacks) implements StatusDefinition {}

    /**
     * A vanilla potion effect. The key's syntax is validated at load, which needs
     * no server; whether it names a real effect is checked by ContentValidator at
     * startup, once Registry.MOB_EFFECT is reachable.
     */
    record Potion(String id, NamespacedKey potionType) implements StatusDefinition {}

    /**
     * A stacking, multiplicative movement-slow: each stack multiplies speed by 0.9, floored
     * at 0.6x base. The stack count is real per-target state, and the speed modifier must be
     * fully removed at expiry -- a leaked modifier is a permanently-slow mob. The 0.9/0.6
     * tuning is named-constant in SoakedStatus for now; it moves to YAML when the curve is
     * tuned in play. Config-named (not "StackingSlow") because nothing else reuses it yet.
     */
    record Soaked(String id) implements StatusDefinition {}
}
