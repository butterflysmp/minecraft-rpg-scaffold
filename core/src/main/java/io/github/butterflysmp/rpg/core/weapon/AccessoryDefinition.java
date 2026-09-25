package io.github.butterflysmp.rpg.core.weapon;

import io.github.butterflysmp.rpg.core.accessory.AccessorySlotKind;
import io.github.butterflysmp.rpg.core.accessory.AccessoryStat;
import io.github.butterflysmp.rpg.core.accessory.AccessoryType;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * One accessory, as authored in {@code content/accessories/<id>.yml} -- the fifth gear kind.
 *
 * <p>An accessory is worn in one of a player's four accessory slots (one class, three universal) and
 * adds stat modifiers, drawbacks included. It lives in no equipment slot, so nothing vanilla reads
 * it: its stats are read from THIS definition by the accessory scanner, never from the item. That is
 * load-bearing -- the all-slot scanners read their keys off any equipped stack, main hand included,
 * so an accessory carrying those keys would grant its stats while merely HELD.
 *
 * <p>A record: its fields are fixed at construction, and equals/hashCode come free. It implements
 * the SEALED {@link GearDefinition}, which is what makes every exhaustive switch over gear kinds a
 * compile error until it says what an accessory does there.
 *
 * <h2>What the constructor refuses -- each a named, skipped file in the boot log</h2>
 *
 * <ul>
 *   <li>A class accessory with no {@code class} or {@code type}, a universal one with either, or a
 *       type whose class disagrees with {@code class} (melee wears gauntlets, ranger a quiver, mage a
 *       scroll -- {@link AccessoryType} holds the table).
 *   <li>A material outside {@link #MATERIALS}. The allowlist is the plan's §3.7: each is inert --
 *       not wearable, not ammunition, not food, not placeable, no use action, and not bought by any
 *       villager (measured in the 26.1.2 trade table). {@code ARROW} is therefore refused, which
 *       matters because {@code QuiverAmmo.consume} re-checks only the material.
 *   <li>No modifiers, a zero or non-finite one, or {@link AccessoryStat#CLASS_DAMAGE} on a universal
 *       accessory (a universal item has no class to grant damage to).
 * </ul>
 *
 * <p><b>Negatives are NOT checked here</b>: one bound needs the max-mana base, which lives outside
 * core. The loader applies {@code AccessoryNegatives} to every modifier -- see that class.
 */
public record AccessoryDefinition(
        String id,
        String displayName,
        Rarity rarity,
        String material,
        AccessorySlotKind slot,
        WeaponClass accessoryClass,
        AccessoryType type,
        Map<AccessoryStat, Double> modifiers,
        List<String> flavor
) implements GearDefinition {

    /**
     * The only base materials an accessory may mint onto -- see the class note for what makes each
     * inert. A new material joins here with its reason in {@code PLAN-accessories.md} §3.7's table.
     */
    public static final Set<String> MATERIALS =
            Set.of("echo_shard", "netherite_scrap", "shulker_shell", "prismarine_crystals");

    public AccessoryDefinition {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("accessory id must not be blank");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("accessory '" + id + "' has a blank display_name");
        }
        if (rarity == null) {
            throw new IllegalArgumentException("accessory '" + id + "' has no rarity");
        }
        if (material == null || !MATERIALS.contains(material.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("accessory '" + id + "' has material '" + material
                    + "'; it must be one of " + MATERIALS.stream().sorted().toList()
                    + " -- each is inert: not wearable, not ammunition, not food, not placeable,"
                    + " and not bought by any villager");
        }
        material = material.toLowerCase(Locale.ROOT);
        if (slot == null) {
            throw new IllegalArgumentException("accessory '" + id + "' has no slot"
                    + " (universal or class)");
        }
        switch (slot) {
            case UNIVERSAL -> {
                if (accessoryClass != null || type != null) {
                    throw new IllegalArgumentException("accessory '" + id + "' is slot: universal and"
                            + " must not name a class or a type -- those are for slot: class");
                }
            }
            case CLASS -> {
                if (accessoryClass == null || type == null) {
                    throw new IllegalArgumentException("accessory '" + id + "' is slot: class and"
                            + " must name both a class (melee, ranger, mage) and a type"
                            + " (gauntlet, quiver, scroll)");
                }
                if (type.weaponClass() != accessoryClass) {
                    throw new IllegalArgumentException("accessory '" + id + "' is type: "
                            + type.token() + ", which belongs to "
                            + type.weaponClass().name().toLowerCase(Locale.ROOT) + ", not "
                            + accessoryClass.name().toLowerCase(Locale.ROOT));
                }
            }
        }
        if (modifiers == null || modifiers.isEmpty()) {
            throw new IllegalArgumentException("accessory '" + id + "' has no modifiers -- an"
                    + " accessory that changes nothing has nothing to show on its tooltip");
        }
        for (Map.Entry<AccessoryStat, Double> modifier : modifiers.entrySet()) {
            Double amount = modifier.getValue();
            if (modifier.getKey() == null || amount == null || !Double.isFinite(amount) || amount == 0.0) {
                throw new IllegalArgumentException("accessory '" + id + "' has modifier "
                        + modifier.getKey() + ": " + amount + "; every modifier must be a finite,"
                        + " non-zero number");
            }
            if (modifier.getKey() == AccessoryStat.CLASS_DAMAGE && slot != AccessorySlotKind.CLASS) {
                throw new IllegalArgumentException("accessory '" + id + "' grants class_damage but is"
                        + " slot: universal; class damage is granted to the accessory's own class, so"
                        + " only a class accessory may carry it");
            }
        }
        modifiers = Collections.unmodifiableMap(new EnumMap<>(modifiers));
        flavor = flavor == null ? List.of() : List.copyOf(flavor);
    }

    /** Accessories are never minted by crafting in v1 (ruling A3: admin give only). */
    @Override
    public Optional<String> craftResult() {
        return Optional.empty();
    }
}
