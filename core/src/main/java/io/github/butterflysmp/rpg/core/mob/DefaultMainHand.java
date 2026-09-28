package io.github.butterflysmp.rpg.core.mob;

import java.util.Map;
import java.util.Optional;

/**
 * The main-hand item a dev spawn hands a vanilla mob whose vanilla weapon is GUARANTEED
 * (PLAN-mob-scaling.md §6 F16).
 *
 * <p><b>Why it exists.</b> {@code /rpg spawn} passes {@code randomizeData = false}, for determinism, and
 * the server then skips {@code Mob.finalizeSpawn} ({@code CraftRegionAccessor.addEntity}, read with
 * {@code javap}). That is where every default weapon is handed out, so a spawned skeleton had no bow.
 * This table puts back only what vanilla would have given with certainty. Rolled items (the drowned's
 * trident, a zombie's sword, the zombified piglin's golden weapon) stay off, which keeps a dev spawn
 * deterministic.
 *
 * <p><b>Ranged, then melee.</b> A bow or crossbow carries no attack-damage modifier (both register with
 * {@code durability} alone in {@code Items}). The melee weapons do, and they joined only after the seat
 * ruled that the seed counts the held weapon deterministically ({@link MeleeSeed}), instead of whenever a
 * player happened to pair with the mob.
 *
 * <p><b>Applied to VANILLA spawns only.</b> A custom mob keeps its content definition's shape. The Knell
 * is a wither skeleton, and giving it a stone sword would move its parked damage (M17).
 *
 * <p>Keys are vanilla entity keys and item keys, without the {@code minecraft:} namespace, so this stays
 * free of any Bukkit type.
 */
public final class DefaultMainHand {

    private DefaultMainHand() {}

    private static final Map<String, String> TABLE = Map.of(
            "skeleton", "bow",       // AbstractSkeleton.populateDefaultEquipmentSlots, unconditional
            "stray", "bow",          //   (no override)
            "bogged", "bow",         //   (no override)
            "parched", "bow",        //   (no override; checked by class-block bounds)
            "illusioner", "bow",     // Illusioner.finalizeSpawn, unconditional
            "pillager", "crossbow",  // Pillager.populateDefaultEquipmentSlots, unconditional
            // MELEE, after the seat's ruling that the seed counts the held weapon deterministically
            // (MeleeSeed): /rpg spawn now seeds as a natural spawn does, at any distance.
            "wither_skeleton", "stone_sword", // WitherSkeleton.populateDefaultEquipmentSlots, unconditional
            "vindicator", "iron_axe",         // Vindicator...: only when getCurrentRaid() is null
            "vex", "iron_sword",              // Vex.populateDefaultEquipmentSlots, unconditional
            "piglin_brute", "golden_axe");    // PiglinBrute.populateDefaultEquipmentSlots, unconditional

    /** The item key for this vanilla entity key, or empty when vanilla guarantees it no weapon we give. */
    public static Optional<String> of(String entityKey) {
        return Optional.ofNullable(TABLE.get(entityKey));
    }

    /** The whole table, for the test that pins it. */
    public static Map<String, String> table() {
        return TABLE;
    }
}
