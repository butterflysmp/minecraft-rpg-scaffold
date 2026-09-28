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
 * <p><b>RANGED ONLY, by the seat's ruling.</b> A bow or crossbow carries no attack-damage modifier (both
 * register with {@code durability} alone in {@code Items}), so none of these can move a mob's seeded
 * melee. The guaranteed MELEE weapons wait on F16's seed-timing question: whether the seed counts the
 * weapon depends on whether a player is in tracking range when the mob is added.
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
            "pillager", "crossbow"); // Pillager.populateDefaultEquipmentSlots, unconditional

    /** The item key for this vanilla entity key, or empty when vanilla guarantees it no weapon we give. */
    public static Optional<String> of(String entityKey) {
        return Optional.ofNullable(TABLE.get(entityKey));
    }

    /** The whole table, for the test that pins it. */
    public static Map<String, String> table() {
        return TABLE;
    }
}
