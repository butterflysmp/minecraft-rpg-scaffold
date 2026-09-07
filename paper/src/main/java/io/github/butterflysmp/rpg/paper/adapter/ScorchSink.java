package io.github.butterflysmp.rpg.paper.adapter;

import java.util.UUID;

/**
 * The two things a scorch tick needs from the world, behind a seam so the tick body is testable
 * without a server -- {@link SpeedAttribute}'s role for Soaked, filled for a DoT.
 *
 * <p>Read and write are SEPARATE for the reason {@code CombatantHandle} states: you cannot hop a
 * thread and still return a value. {@link #victimMaxHealth()} is a READ, legal only on the thread that
 * owns the victim -- which is where the tick body already runs, because {@code RepeatingTask} fires on
 * the target's own thread. {@link #deal} is a DISPATCH and returns nothing.
 */
public interface ScorchSink {

    /**
     * The victim's CUSTOM max health -- the pool the 5% is a percentage of.
     *
     * Custom, not vanilla: vanilla's max is a puppet written from the custom numbers, and a percentage
     * of a puppet would be a percentage of a display. Returns the same figure {@code CombatantStats.max}
     * would, and reads it fresh each tick so a mid-burn max change is honoured rather than frozen at
     * application.
     */
    double victimMaxHealth();

    /**
     * Deal one scorch tick of {@code amount}, credited to {@code applierId}.
     *
     * <p><b>The applier reaching this parameter IS requirement A.</b> Before this slice the burn was
     * vanilla {@code FIRE_TICK} damage, and {@code RpgListeners.attributableId} falls back to the
     * VICTIM's own id when a damage event names no causing entity -- so scorch kills credited the
     * victim. Not a missing feature: an inverted one, since the vanilla damage boundary landed.
     *
     * <p>Implementations must pass {@code bypassesDefense = true}. See {@code Scorch}'s javadoc for the
     * three armour channels and why only this one is bypassed.
     */
    void deal(double amount, UUID applierId);
}
