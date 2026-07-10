package io.github.butterflysmp.rpg.core.weapon;

import io.github.butterflysmp.rpg.core.element.Element;

import java.util.List;
import java.util.Optional;

/**
 * One weapon, fully described. Constructed from YAML by the content loader in the
 * paper module -- core never reads files.
 *
 * A weapon is a container of triggers. element and rarity are inert reserved data
 * in Phase 1: element flavors a kit and gates use in Phase 3, rarity parameterizes
 * a loot roll in Phase 4. Here they only color the item name. element is mandatory
 * and never null -- an unflavored weapon is KINETIC, not absent.
 */
public record WeaponDefinition(
        String id,
        String displayName,
        Element element,
        Rarity rarity,
        List<TriggerBinding> triggers
) {
    public WeaponDefinition {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("weapon id required");
        if (element == null) throw new IllegalArgumentException("weapon element required (use KINETIC, never null)");
        if (rarity == null) throw new IllegalArgumentException("weapon rarity required");
        if (triggers == null || triggers.isEmpty()) {
            throw new IllegalArgumentException("weapon '" + id + "' has no triggers");
        }
        triggers = List.copyOf(triggers);
    }

    /** The binding fired by this input, if the weapon has one. */
    public Optional<TriggerBinding> trigger(String input) {
        for (TriggerBinding binding : triggers) {
            if (binding.input().equals(input)) return Optional.of(binding);
        }
        return Optional.empty();
    }
}
