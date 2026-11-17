package io.github.butterflysmp.rpg.core.mob;

/**
 * THE KNELL'S TELEPORT AFTER ITS BEAM LANDS (PLAN-dragon-mobs.md step R6; §4.2 N8). Authored in the mob file's
 * {@code teleport:} block. Ben: "The Knell can only teleport when it's Knell Beam hits a target"; "a degree arc from
 * the player would be nice. Maybe between 30 and 70 degrees"; "The same time between the warning shot and the actual
 * beam ... This should be a tunable number as well"; "The hit itself is the warning".
 *
 * <p><b>NAME THE QUANTITY:</b>
 * <ul>
 *   <li>{@code delayTicks}: TICKS from the beam's FIRE (the hit) to the move. 12, equal to the telegraph today, its own
 *       number (K11). Must be shorter than the beam's {@code cycle_ticks.min}, so a move never falls inside a
 *       telegraph: {@link MobDefinition} refuses it otherwise.</li>
 *   <li>{@code arcMinDegrees} / {@code arcMaxDegrees}: DEGREES the Knell is turned around its target, measured at the
 *       target's feet from the Knell's bearing (angle zero), both included. Ben's 30 and 70 (K12, K13).</li>
 *   <li>{@code spotsPerSide}: how many landing spots are tried on one side before the other. The seat's 5 (T-a,
 *       K14).</li>
 *   <li>{@code levelTolerance}: whole BLOCKS above or below the target's feet a landing may stand. The seat's 3
 *       (T-b, K15).</li>
 *   <li>{@code requireSight}: a landing must see the target, from the Knell's eye there to the target's aim point.
 *       The seat's yes (T-c, Q18).</li>
 * </ul>
 */
public record MobTeleport(int delayTicks, double arcMinDegrees, double arcMaxDegrees, int spotsPerSide,
                          int levelTolerance, boolean requireSight) {

    public MobTeleport {
        if (delayTicks < 1) {
            throw new IllegalArgumentException("teleport.delay_ticks must be at least 1 tick, was " + delayTicks);
        }
        if (!(arcMinDegrees >= 0 && arcMinDegrees <= 180)) {
            throw new IllegalArgumentException("teleport.arc_degrees.min must be 0 to 180 degrees, was " + arcMinDegrees);
        }
        if (!(arcMaxDegrees >= arcMinDegrees && arcMaxDegrees <= 180)) {
            throw new IllegalArgumentException("teleport.arc_degrees.max must be from arc_degrees.min ("
                    + arcMinDegrees + ") to 180 degrees, was " + arcMaxDegrees);
        }
        if (spotsPerSide < 1) {
            throw new IllegalArgumentException("teleport.spots_per_side must be at least 1, was " + spotsPerSide);
        }
        if (levelTolerance < 0) {
            throw new IllegalArgumentException("teleport.level_tolerance must be 0 or more blocks, was " + levelTolerance);
        }
    }
}
