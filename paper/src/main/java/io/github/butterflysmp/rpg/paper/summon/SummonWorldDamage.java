package io.github.butterflysmp.rpg.paper.summon;

import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

/**
 * Q4 (the seat's ruling, 2026-11-24; SEAT PLACEHOLDER, OPEN FOR BEN): WHAT A SUMMON TAKES FROM A VANILLA DAMAGE EVENT.
 * Today a tracked mob takes every world cause into its custom HP ({@code VanillaDamagePolicy} REROUTE) and passes KILL
 * and VOID. A summon takes NONE of the world causes. VOID removes it ({@code cause=void}), KILL removes it
 * ({@code cause=killed}). The ONE cause it still takes is a MOB's hit, as the one table says (MOB -> SUMMON: yes), priced
 * as a mob's hit on a player is ({@code MobDamagePricing}). Everything else is refused: nothing reaches its HP.
 */
public final class SummonWorldDamage {

    private SummonWorldDamage() {}

    /** What a vanilla damage event does to a summon. */
    public enum Outcome { MOB_HIT, REFUSED, REMOVE_VOID, REMOVE_KILLED }

    /**
     * @param cause      the event's cause
     * @param mobSourced the hit has a mob behind it (the listener's {@code MobDamagePricing.resolve}, non-empty)
     */
    public static Outcome decide(DamageCause cause, boolean mobSourced) {
        if (cause == DamageCause.VOID) return Outcome.REMOVE_VOID;
        if (cause == DamageCause.KILL) return Outcome.REMOVE_KILLED;
        return mobSourced ? Outcome.MOB_HIT : Outcome.REFUSED;
    }
}
