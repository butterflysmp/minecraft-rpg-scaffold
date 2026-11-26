package io.github.butterflysmp.rpg.core.weapon;

import java.util.Map;
import java.util.Optional;

/**
 * K2 (the seat's brief, 2026-11-25, the Broken Bottle): the crit chance the weapon IN THE MAIN HAND grants, as ONE source
 * for the ONE crit-chance reconcile ({@code PlayerHealthSystem}'s loop, merged with the fixtures', the accessories', the
 * fragments' and the level's maps through {@code AccessoryContributions.merged}). Never a second accumulator.
 *
 * <p><b>Present exactly while that weapon is in hand.</b> The loop re-reads the main hand every pass, so a weapon switched
 * away, dropped, stowed or moved to the off hand is simply absent from the next pass's map, and the reconciler removes
 * the source. No departure event to miss. A melee hit already in flight when he switches does not exist: the crit is
 * rolled once per cast, at the swing, off the stat as it stands then ({@code BukkitCombatant.snapshot}).
 *
 * <p><b>FLAT</b> (Ben: "Not it's crit chance"): the authored figure, whatever the stack's gear score. The method takes no
 * score, so none can reach it.
 */
public final class HeldWeaponCrit {

    private HeldWeaponCrit() {}

    /** The one key. Distinct from every slot-named key the TEMP fixture uses, and from the enchant's. */
    public static final String SOURCE = "weapon:MAIN_HAND";

    /** The held weapon's source, or nothing: absent (not 0) when no weapon is held or it declares no crit chance. */
    public static Map<String, Double> sources(Optional<WeaponDefinition> held) {
        return held.filter(w -> w.critChance() > WeaponDefinition.NO_CRIT)
                .map(w -> Map.of(SOURCE, w.critChance()))
                .orElse(Map.of());
    }
}
