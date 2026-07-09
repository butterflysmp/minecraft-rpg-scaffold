package io.github.yourname.rpg.paper.content;

import org.bukkit.NamespacedKey;

/**
 * What a status id *means*, mechanically. Not how strong it is and not how long
 * it lasts: duration and amplifier come from the ability that applies it, via
 * EffectSpec.Status. A status definition maps an id to a mechanic.
 *
 * Sealed so BukkitCombatant.applyStatus switches exhaustively over the kinds.
 */
public sealed interface StatusDefinition permits StatusDefinition.Fire, StatusDefinition.Potion {

    String id();

    /** A burn. Bukkit has no "burning" potion effect, so this drives setFireTicks. */
    record Fire(String id) implements StatusDefinition {}

    /**
     * A vanilla potion effect. The key's syntax is validated at load, which needs
     * no server; whether it names a real effect is checked by ContentValidator at
     * startup, once Registry.MOB_EFFECT is reachable.
     */
    record Potion(String id, NamespacedKey potionType) implements StatusDefinition {}
}
