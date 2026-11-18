package io.github.butterflysmp.rpg.core.mob;

import java.util.OptionalDouble;

/**
 * THE PHAGE'S OWN FUSE, ONE LIVE PHAGE'S (the seat's ruling 1 on #204: route (b), our fuse with the swell shown by
 * packet). A pure state machine; paper owns one per live Phage, ticks it once a server tick, and SHOWS its direction to
 * the client. <b>The server's truth is this, never vanilla's swell</b>, whose goal is removed.
 *
 * <p><b>THE RULES ARE VANILLA'S, READ</b> (PLAN-dragon-mobs.md section 7 F-C; {@code Creeper#tick}, {@code SwellGoal
 * #canUse} and {@code #tick} in the pinned server jar), with our distances in place of its literals:
 * <ol>
 *   <li><b>First the swell moves</b> by its direction, +1 or -1, floored at 0 ({@code Creeper#tick}: {@code swell +=
 *       dir}, {@code if (swell < 0) swell = 0}), and at the fuse's ticks it DETONATES ({@code swell >= maxSwell}).</li>
 *   <li><b>Then the direction is decided</b>, as {@code SwellGoal} does later in the same tick: a DOWN fuse turns UP
 *       (START) when its target is STRICTLY inside the trigger ({@code distanceToSqr < 9.0}) and in sight; an UP fuse
 *       turns DOWN (RESET) when the target is gone, STRICTLY beyond the cancel ({@code > 49.0}), or out of sight.</li>
 * </ol>
 * So a START on tick N detonates on tick N + fuse, and a reset swell falls a tick at a time, as the client's
 * animation does: a restart resumes from where it is.
 *
 * <p><b>NAME THE QUANTITY:</b> {@code distance}: BLOCKS between the Phage's FEET and its target's FEET (vanilla's
 * {@code distanceToSqr(Entity)}, positions), empty when it has no target it is hostile to; {@code trigger} and
 * {@code cancel}: BLOCKS, the stage's ({@link StagedDistance}); {@code fuseTicks}: TICKS.
 */
public final class MobFuse {

    /** What one tick did. */
    public enum Event { NONE, START, RESET, DETONATE }

    private int swell;
    private int direction = -1;   // vanilla's DATA_SWELL_DIR default (READ, Creeper#defineSynchedData)

    public Event tick(OptionalDouble distance, boolean sight, double trigger, double cancel, int fuseTicks) {
        swell += direction;
        if (swell < 0) swell = 0;
        if (swell >= fuseTicks) {
            swell = fuseTicks;
            return Event.DETONATE;
        }
        if (direction < 0) {
            if (distance.isPresent() && distance.getAsDouble() < trigger && sight) {
                direction = 1;
                return Event.START;
            }
            return Event.NONE;
        }
        if (forced) return Event.NONE;   // the instrument's light holds to the blast
        if (distance.isEmpty() || distance.getAsDouble() > cancel || !sight) {
            direction = -1;
            return Event.RESET;
        }
        return Event.NONE;
    }

    /** The swell, in TICKS: 0 to the fuse. */
    public int swell() {
        return swell;
    }

    /** +1 while fusing, -1 otherwise: the value the client is shown. */
    public int direction() {
        return direction;
    }

    public boolean fusing() {
        return direction > 0;
    }

    private boolean forced;

    /**
     * THE INSTRUMENT'S LIGHT (2026-11-18, GATE-implosion-drag.md's Part D row): turn the fuse UP now, wherever its target
     * is, and HOLD it up to the blast: no distance, sight or absence resets it after this. The selftest's only; no game
     * path calls it. Returns true when it was down (a new start, for the caller to show), false when it was already lit.
     */
    public boolean light() {
        forced = true;
        if (direction > 0) return false;
        direction = 1;
        return true;
    }

    /** True once {@link #light} has held this fuse lit. */
    public boolean forced() {
        return forced;
    }
}
