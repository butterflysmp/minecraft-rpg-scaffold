package io.github.butterflysmp.rpg.core.accessory;

import io.github.butterflysmp.rpg.core.combat.HealthRegen;
import io.github.butterflysmp.rpg.core.combat.ManaRegen;
import io.github.butterflysmp.rpg.core.weapon.AccessoryDefinition;
import io.github.butterflysmp.rpg.core.weapon.ClassDamageModifiers.ClassGrant;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * What a player's worn accessories contribute to each stat, keyed by the source name the
 * reconcilers use -- the pure half of the accessory scanner. The paper half reads the store and the
 * profile, and hands this the four slots.
 *
 * <p>A record: an immutable value with its fields fixed at construction.
 *
 * <h2>The source keys: {@code accessory:<slot>}, and why the prefix</h2>
 *
 * {@code Stat.putModifier} is put-or-REPLACE, and every reconciler clears each applied source absent
 * from the map it is handed. So an accessory's key must not equal any other scanner's key, or one
 * would silently replace the other. The existing keys are bare {@code EquipmentSlot} names
 * ({@code "CHEST"}) or {@code "<prefix>:"+SLOT}; the {@code accessory:} prefix is disjoint from all
 * of them, which {@code AccessorySourceKeysTest} asserts against every prefix rather than trusting
 * the comments -- four of which were found describing the quiver keys wrongly.
 *
 * <p>No internal key here contains {@code quiver} (ruling A2).
 *
 * <h2>The merges are NAMED, TWO-ARGUMENT, and the reason is measured</h2>
 *
 * {@link #merged} and {@link #mergedGrants} take both maps, so dropping a source is dropping an
 * argument, which does not compile. The quiver-size merge in {@code PlayerHealthSystem} was inline
 * until the mutation that deleted its {@code putAll} reddened nothing in the whole suite. They do not
 * throw on a key collision: a throw would crash the reconcile loop, and collisions are made
 * impossible by the disjointness test instead.
 */
public record AccessoryContributions(Map<AccessoryStat, Map<String, Double>> byStat,
                                     Map<String, ClassGrant> classGrants) {

    /** The prefix every accessory source key carries. */
    public static final String SOURCE_PREFIX = "accessory:";

    /** Nothing worn, or nothing readable: every stat contributes an empty map. */
    public static final AccessoryContributions NONE =
            new AccessoryContributions(Map.of(), Map.of());

    public AccessoryContributions {
        Map<AccessoryStat, Map<String, Double>> copy = new EnumMap<>(AccessoryStat.class);
        byStat.forEach((stat, sources) -> copy.put(stat, Map.copyOf(sources)));
        byStat = Collections.unmodifiableMap(copy);
        classGrants = Map.copyOf(classGrants);
    }

    /** The source key for one accessory slot. */
    public static String sourceKey(int slot) {
        return SOURCE_PREFIX + AccessorySlots.requireSlot(slot);
    }

    /**
     * What the four slots contribute for a player whose profile class is {@code profileClass}.
     *
     * <p>{@code slots} has one entry per slot index; a {@code null} entry is an empty slot, or one
     * whose stored item could not be read -- either way it contributes nothing.
     *
     * <p>A slot contributes only when (1) its accessory is of the slot's kind -- a class item that
     * somehow sits in a universal slot is not honoured -- and (2) for a class accessory, the profile's
     * class is the accessory's class, EXACTLY ({@link AccessorySlots#contributes}, ruling A1).
     * {@link AccessoryStat#CLASS_DAMAGE} becomes a {@link ClassGrant}, not a flat modifier, so that
     * {@code ClassDamageModifiers.matching} can still gate it on the held weapon (ruling Q3).
     */
    public static AccessoryContributions of(List<AccessoryDefinition> slots, String profileClass) {
        if (slots.size() != AccessorySlots.COUNT) {
            throw new IllegalArgumentException("expected " + AccessorySlots.COUNT
                    + " accessory slots, got " + slots.size());
        }
        Map<AccessoryStat, Map<String, Double>> byStat = new EnumMap<>(AccessoryStat.class);
        Map<String, ClassGrant> grants = new HashMap<>();
        for (int slot = 0; slot < AccessorySlots.COUNT; slot++) {
            AccessoryDefinition accessory = slots.get(slot);
            if (accessory == null) continue;
            if (accessory.slot() != AccessorySlots.kindOf(slot)) continue;
            if (!AccessorySlots.contributes(accessory, profileClass)) continue;
            String key = sourceKey(slot);
            for (Map.Entry<AccessoryStat, Double> modifier : accessory.modifiers().entrySet()) {
                if (modifier.getKey() == AccessoryStat.CLASS_DAMAGE) {
                    grants.put(key, new ClassGrant(accessory.accessoryClass(), modifier.getValue()));
                } else {
                    byStat.computeIfAbsent(modifier.getKey(), s -> new HashMap<>())
                            .put(key, sourceValue(modifier.getKey(), modifier.getValue()));
                }
            }
        }
        return new AccessoryContributions(byStat, grants);
    }

    /**
     * The modifier an authored amount becomes, per stat.
     *
     * <p><b>Mana regen goes through {@link ManaRegen#contribution}</b>, the seam
     * {@code ManaRegenModifierItems} already uses: its javadoc names it THE one place a mana-regen
     * bonus becomes a stat modifier, so a future cap or curve lands there and reaches every source.
     * It is the identity today, so nothing an accessory contributes changes -- the point is that the
     * seam covers this source too, rather than being a rule every source but one obeys.
     *
     * <p><b>Health regen goes through {@link HealthRegen#contribution} for the identical reason</b> --
     * its javadoc makes the same "the ONE place" claim, for {@code HealthRegenModifierItems}.
     *
     * <p><b>Max mana deliberately does NOT go through {@code ManaBank.contribution}</b>: that seam's
     * claim is about a MANA BANK bonus -- an enchant -- and an accessory's max mana is not one. Max
     * health likewise has no stat-level seam ({@code Growth.contribution} is the Growth enchant's).
     */
    static double sourceValue(AccessoryStat stat, double amount) {
        return switch (stat) {
            case MANA_REGEN -> ManaRegen.contribution(amount);
            case HEALTH_REGEN -> HealthRegen.contribution(amount);
            case MAX_HEALTH, MAX_MANA, CRIT_CHANCE, CRIT_DAMAGE, DEFENSE, CLASS_DAMAGE -> amount;
        };
    }

    /** The desired sources for one stat. Never null; empty when nothing contributes. */
    public Map<String, Double> sources(AccessoryStat stat) {
        return byStat.getOrDefault(stat, Map.of());
    }

    /**
     * An existing scanner's desired map merged with the accessories' map for the same stat, ready
     * for the ONE reconcile call that stat gets. See the class note on why this is named.
     */
    public static Map<String, Double> merged(Map<String, Double> gear, Map<String, Double> accessories) {
        Map<String, Double> merged = new HashMap<>(gear);
        merged.putAll(accessories);
        return merged;
    }

    /** {@link #merged}'s twin for class-damage grants, merged BEFORE {@code matching} runs. */
    public static Map<String, ClassGrant> mergedGrants(Map<String, ClassGrant> gear,
                                                       Map<String, ClassGrant> accessories) {
        Map<String, ClassGrant> merged = new HashMap<>(gear);
        merged.putAll(accessories);
        return merged;
    }
}
