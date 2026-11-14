package io.github.butterflysmp.rpg.core.mining;

/**
 * What mining says to a player, and for how long. Mining S1-c.
 *
 * <p><b>Why here, as code and not in content:</b> the hold is one value Ben ruled for one message, the
 * same for every ore and vein size, so it is not per-ore data and does not belong in
 * {@code content/ores}. It lives in core so {@code MiningNoticesTest} pins it beside the rule it feeds
 * ({@link io.github.butterflysmp.rpg.core.hud.ActionBarHolds}), and the paper call site names it
 * rather than carrying a literal.
 */
public final class MiningNotices {

    private MiningNotices() { }

    /** The DENIED click's action-bar text (unchanged from slice 1's M-DENY). */
    public static final String DENIED_TEXT = "You need a pickaxe to mine this.";

    /**
     * How long the DENIED text owns the action bar, in SERVER TICKS. Ben, 2026-11-14: "Is it possible
     * to have the pickaxe message over ride the action bar for 1.5s". 1.5 s at 20 TPS = 30; under lag
     * it lasts longer in real time.
     */
    public static final int DENIED_HOLD_TICKS = 30;

    /**
     * Mining S2: how long after a miss sound PLAYS before the next one may, in SERVER TICKS, per player.
     * Ben, 2026-11-14: "Miss sound should have a slight cooldown, let's say .4 seconds". 0.4 s at 20 TPS
     * = 8. A miss inside it is still a MISS and still logged; it plays nothing ({@link MissSoundCooldown}).
     */
    public static final int MISS_SOUND_COOLDOWN_TICKS = 8;
}
