package io.github.butterflysmp.rpg.paper.adapter;

/**
 * The pure decisions behind the immobilize's movement suppression, split out from Bukkit so
 * they are unit-testable: the per-tick position correction (the anchor) and whether a teleport
 * should be suppressed. The Bukkit bindings (teleport, event cancel) live in BukkitCombatant
 * and RpgListeners and are boot-witnessed.
 *
 * speed-attribute-0 + velocity-zero stop attribute-scaled locomotion (walk, climb). The anchor
 * catches what AI re-issues each tick and those clamps miss: the ranged-mob STRAFE (horizontal)
 * and the slime HOP (vertical jump impulse). Fliers (ghast/blaze/phantom) are not specially
 * handled -- an accepted compromise (see DESIGN-status-effects.md).
 */
public final class ImmobilizePhysics {

    private ImmobilizePhysics() {}

    /**
     * DEFAULT for the tuning knob {@code immobilize.anchor-drift-blocks} in config.yml (edit +
     * restart, no rebuild), read into {@code AdapterContext.anchorDrift()}. Horizontal drift
     * (blocks) tolerated before the anchor snaps a mob back.
     *
     * DEFAULT IS 0: snap EVERY tick. The anchor correction runs after the AI has already moved
     * the mob, so any nonzero tolerance T is the amplitude of a sawtooth -- the mob drifts up to
     * T, then snaps -- which reads as jitter (small T) or creep (large T). Zero means the reported
     * position is always the anchor: genuinely still. Left configurable only as an escape hatch.
     */
    public static final double ANCHOR_DRIFT = 0.0;

    /**
     * Minimum teleport distance (blocks) that counts as a "real" teleport to suppress, so the
     * anchor's own sub-block corrective teleports are never cancelled by the teleport suppressor.
     * Enderman teleports are many blocks; anchor corrections are sub-block.
     */
    public static final double MIN_TELEPORT = 2.0;

    /**
     * Where an immobilized mob should be snapped this tick, as {@code [x, y, z]}, or null if it is
     * within tolerance. Locks X/Z to the anchor (walk, strafe) if drifted past {@code driftSq};
     * caps Y at the anchor's Y (hop/jump/climb) while still allowing falling (Y below the anchor
     * is kept). The caller supplies the CURRENT yaw/pitch to the teleport -- this returns position
     * only -- so a rooted mob keeps tracking its target.
     */
    public static double[] correction(double cx, double cy, double cz,
                                      double ax, double ay, double az, double driftSq) {
        boolean drifted = (cx - ax) * (cx - ax) + (cz - az) * (cz - az) > driftSq;
        boolean rose = cy > ay;
        if (!drifted && !rose) return null;
        return new double[]{ax, Math.min(cy, ay), az};
    }

    /**
     * Cancel a teleport iff the mob is immobilized AND the jump is a real (large) teleport --
     * never the immobilize's own sub-block position correction.
     */
    public static boolean suppressTeleport(boolean immobilized, double distanceSq, double minSq) {
        return immobilized && distanceSq > minSq;
    }
}
