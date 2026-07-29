package io.github.butterflysmp.rpg.core.weapon;

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
 *
 * material is the item the weapon is carried in ("iron_sword", "bow", ...), an opaque
 * presentation string core never interprets -- paper resolves it to a Bukkit Material,
 * exactly as it resolves the MiniMessage displayName. The bow needs a non-sword item, so
 * this became a field; every sword-shaped weapon leaves it at the DEFAULT_MATERIAL.
 */
public record WeaponDefinition(
        String id,
        String displayName,
        String element,
        Rarity rarity,
        String material,
        double attackDamage,
        List<TriggerBinding> triggers
) {
    /** The item a weapon renders as when its content does not say otherwise: a sword. */
    public static final String DEFAULT_MATERIAL = "iron_sword";

    public WeaponDefinition {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("weapon id required");
        if (element == null || element.isBlank()) throw new IllegalArgumentException("weapon element required (use kinetic, never absent)");
        if (rarity == null) throw new IllegalArgumentException("weapon rarity required");
        if (material == null || material.isBlank()) throw new IllegalArgumentException("weapon material required");
        // Attack damage is a stat the basic melee hit reads (via WeaponDamage). 0 is legal -- a
        // ranged/costed weapon (bow, staff) has no melee and declares none; negative is a content bug.
        if (attackDamage < 0) throw new IllegalArgumentException("weapon '" + id + "' attack_damage must be >= 0, got: " + attackDamage);
        if (triggers == null || triggers.isEmpty()) {
            throw new IllegalArgumentException("weapon '" + id + "' has no triggers");
        }
        triggers = List.copyOf(triggers);
    }

    /** A sword-shaped weapon with no declared attack damage: the shape older tests use. */
    public WeaponDefinition(String id, String displayName, String element, Rarity rarity,
                            List<TriggerBinding> triggers) {
        this(id, displayName, element, rarity, DEFAULT_MATERIAL, 0.0, triggers);
    }

    /** A weapon with an explicit material but no declared attack damage (kept for existing callers). */
    public WeaponDefinition(String id, String displayName, String element, Rarity rarity,
                            String material, List<TriggerBinding> triggers) {
        this(id, displayName, element, rarity, material, 0.0, triggers);
    }

    /** The binding fired by this input, if the weapon has one. */
    public Optional<TriggerBinding> trigger(String input) {
        for (TriggerBinding binding : triggers) {
            if (binding.input().equals(input)) return Optional.of(binding);
        }
        return Optional.empty();
    }
}
