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
        permits StatusDefinition.Fire, StatusDefinition.Potion, StatusDefinition.Immobilize {

    String id();

    /** A burn. Bukkit has no "burning" potion effect, so this drives setFireTicks. */
    record Fire(String id) implements StatusDefinition {}

    /**
     * A movement lock: zero the mob's velocity every tick for the duration. Vanilla Slowness
     * won't do -- it doesn't stop jumping, so a slowed mob hops away. Rooted is the first
     * configuration; Freeze will reuse this kind and add attack suppression. The general name
     * is chosen now because the sealed switch is compiler-enforced, so renaming later would
     * touch it. Carries no state beyond its id -- duration comes from the ability.
     */
    record Immobilize(String id) implements StatusDefinition {}

    /**
     * A vanilla potion effect. The key's syntax is validated at load, which needs
     * no server; whether it names a real effect is checked by ContentValidator at
     * startup, once Registry.MOB_EFFECT is reachable.
     */
    record Potion(String id, NamespacedKey potionType) implements StatusDefinition {}
}
