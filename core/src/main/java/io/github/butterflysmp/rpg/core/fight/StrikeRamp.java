package io.github.butterflysmp.rpg.core.fight;

import java.util.HashMap;
import java.util.Map;

/**
 * F2, THE STRIKE RAMP: each player's count of consecutive lightning strikes outside the ring (Ben, 2026-11-18: "ramp
 * based on how many times in a row the player has been struck"; "start at 20, 40, 80 and keeps doubling").
 *
 * <p>One instance belongs to one fight, never to a static map (CLAUDE.md invariant 3). The caller asks
 * {@link #isStrikeTick} each tick and, on a strike tick, calls {@link #strike} once per player in the fight with
 * whether he is inside the ring. A player who leaves the fight is {@link #drop dropped}.
 *
 * <p>Gear-score scaling of the damage is OPEN (PLAN-dragon-mobs.md Q3) and is not applied here.
 *
 * @param <K> how the fight names a player (a UUID in {@code paper/}; a string in tests)
 */
public final class StrikeRamp<K> {

    private final FightSettings settings;
    private final Map<K, Integer> consecutive = new HashMap<>();

    public StrikeRamp(FightSettings settings) {
        this.settings = settings;
    }

    /** Whether lightning strikes on this tick of the fight: every {@code strikeIntervalTicks}, from tick 0. */
    public boolean isStrikeTick(long fightTick) {
        return fightTick % settings.strikeIntervalTicks() == 0;
    }

    /**
     * One strike tick for one player. Inside the ring: his count resets to 0 and he takes nothing. Outside (and
     * outside the border is outside): his count goes up by one and he takes {@link #damageForStrike} of it.
     *
     * @return the damage to apply through the fight's damage pipeline, 0 when inside
     */
    public double strike(K player, boolean insideRing) {
        if (insideRing) {
            consecutive.remove(player);
            return 0.0;
        }
        // Saturate instead of overflowing past Integer.MAX_VALUE (some 136 years of strikes at one every 2 s).
        int k = consecutive.merge(player, 1, (old, one) -> old == Integer.MAX_VALUE ? old : old + one);
        return damageForStrike(settings, k);
    }

    /** His current count of consecutive strikes; 0 if none, or never seen, or dropped. */
    public int consecutiveStrikes(K player) {
        return consecutive.getOrDefault(player, 0);
    }

    /** A player who left the fight: nothing is kept for him. */
    public void drop(K player) {
        consecutive.remove(player);
    }

    /** How many players have a count kept. */
    public int trackedPlayers() {
        return consecutive.size();
    }

    /**
     * THE TECHNICAL CEILING (not a gameplay number): the most one strike can offer. Since F6 + F7 a strike goes
     * through ARMOUR ({@code DefenseRule.APPLIES}; Ben, 2026-11-18: "yes"), and {@code Defense.applyDefense} computes
     * {@code damage x SCALE / (SCALE + defence)}, multiplying BEFORE it divides, so an offered {@code Double.MAX_VALUE}
     * becomes infinity (MEASURED, PLAN-dragon-fight.md section 6.2). Half of {@code MAX_VALUE / SCALE} keeps the product
     * finite for every defence of 0 or more; {@code StrikeRampTest} executes the curve at defences 0 to MAX_VALUE.
     * The guard lives HERE, in the ramp, so the defence curve every hit in the game reaches is not reordered.
     */
    public static final double CEILING_DAMAGE =
            Double.MAX_VALUE / (2.0 * io.github.butterflysmp.rpg.core.combat.Defense.SCALE);

    /**
     * The k-th consecutive strike's damage, before armour: {@code firstStrikeDamage x 2^(k-1)}, with no gameplay cap
     * (Ben). <b>It stays finite on the armour path for any k</b>: where the doubling would pass
     * {@link #CEILING_DAMAGE} (k of about 1014 with 20), the result is the ceiling. Any value that large kills.
     */
    public static double damageForStrike(FightSettings settings, int k) {
        if (k < 1) throw new IllegalArgumentException("a strike count starts at 1: " + k);
        double damage = settings.firstStrikeDamage() * Math.pow(2.0, k - 1);
        return Double.isFinite(damage) && damage < CEILING_DAMAGE ? damage : CEILING_DAMAGE;
    }
}
