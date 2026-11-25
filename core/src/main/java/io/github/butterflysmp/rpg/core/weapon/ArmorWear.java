package io.github.butterflysmp.rpg.core.weapon;

import io.github.butterflysmp.rpg.core.combat.stat.HealthChange;
import io.github.butterflysmp.rpg.core.enchant.Unbreaking;

import java.util.OptionalInt;

/**
 * Armor wear (the seat's D1' to D4', 2026-11-25), decided purely. The weapons' and shields' own kernel
 * ({@link Durability}, {@link Unbreaking}) does every piece of arithmetic; this class only says WHEN armor wears.
 *
 * <ul>
 *   <li><b>D2', WHICH HITS (SEAT PLACEHOLDER, OPEN FOR BEN):</b> a DAMAGE on a PLAYER that went THROUGH Defense
 *       ({@link HealthChange#throughDefense}) and offered more than nothing. Every one of the store's damage paths
 *       reaches {@code CombatantStats.damage}, so this is the one place to ask. A bypassed hit (a void tick, a fight's
 *       true damage, a scorch burn) wears nothing, because Defense did not price it. CC'S CALL: a hit that offered 0
 *       is not a hit.</li>
 *   <li><b>D3', BROKEN:</b> {@link Durability#isBroken}, damage at max - 1, exactly as a weapon. A broken piece takes
 *       no further wear.</li>
 *   <li><b>D4', NEVER DESTROYED:</b> the wear is {@link Durability#wear}, which clamps at max - 1, the weapons' floor.
 *       Vanilla's own armor damage is cancelled in paper, so vanilla never reaches its break.</li>
 *   <li><b>Unbreaking</b> exactly as on a weapon: {@link Unbreaking#consumes} of the piece's active level.</li>
 * </ul>
 */
public final class ArmorWear {

    private ArmorWear() {}

    /** One wear per qualifying hit, as a shield's block wears 1. */
    public static final int WEAR_PER_HIT = 1;

    /** D2': does this change wear the target's worn armor? */
    public static boolean wears(HealthChange change) {
        return change.kind() == HealthChange.Kind.DAMAGE
                && change.targetIsPlayer()
                && change.throughDefense()
                && change.beforeDefense() > 0;
    }

    /**
     * One piece's new damage after a qualifying hit, or empty when it does not wear: it has no durability, it is
     * already broken, or Unbreaking spared it.
     *
     * @param roll a uniform draw in [0, 1) for Unbreaking
     */
    public static OptionalInt wearOne(int currentDamage, int maxDurability, int unbreakingLevel, double roll) {
        if (maxDurability <= 0) return OptionalInt.empty();
        if (Durability.isBroken(currentDamage, maxDurability)) return OptionalInt.empty();
        if (!Unbreaking.consumes(unbreakingLevel, roll)) return OptionalInt.empty();
        return OptionalInt.of(Durability.wear(currentDamage, WEAR_PER_HIT, maxDurability));
    }
}
