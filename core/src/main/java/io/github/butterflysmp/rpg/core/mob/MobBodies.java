package io.github.butterflysmp.rpg.core.mob;

/**
 * How a body field becomes an attribute's BASE value. Pure, so both rules are unit rows.
 *
 * <p><b>Both rules are written so that applying them twice equals applying them once.</b> R1 applies a body once,
 * at spawn; R2 onward re-applies it when a Phage changes stage, and a rule that read the attribute's CURRENT value
 * would compound (0.6 x 0.6 on the second call).
 */
public final class MobBodies {

    private MobBodies() {}

    /** The port for one attribute on one live entity: a per-type default, and a base we may write. */
    public interface BodyAttribute {
        double defaultValue();
        double baseValue();
        void setBaseValue(double value);
    }

    /**
     * Speed is a MULTIPLIER of the type's DEFAULT movement speed. Read from {@code defaultValue()}, never from
     * {@code baseValue()}, which is what makes a second call a no-op.
     */
    public static void applySpeed(BodyAttribute speed, double multiplier) {
        speed.setBaseValue(speed.defaultValue() * multiplier);
    }

    /**
     * The {@code SCALE} that makes this body {@code targetHeight} blocks tall. The unscaled height is recovered from
     * the CURRENT height and scale ({@code currentHeight / currentScale}), so it holds whether or not a scale was
     * already applied: a creeper reads 1.7 at scale 1, and 3.0 at scale 3/1.7, and both give 1.7.
     */
    public static double scaleForHeight(double targetHeight, double currentHeight, double currentScale) {
        double unscaled = currentHeight / currentScale;
        return targetHeight / unscaled;
    }
}
