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

    /**
     * The port for one attribute on one live entity: the ENTITY TYPE's own base value, and a base we may write.
     *
     * <p><b>{@code typeBase()} is the quantity "the type's base", and only that.</b> It was called
     * {@code defaultValue()} until the 2026-11-15 boot, and the Bukkit binding answered it with
     * {@code AttributeInstance#getDefaultValue}, which is the ATTRIBUTE's registry default (0.7 for movement speed,
     * for every mob) and not the type's (0.25 for a creeper). Every dragon mob ran 2.3 to 2.8 times its ruled speed,
     * and the unit rows, faking 0.25, could not see it. The name now says which number it is, and the binding's source
     * is pinned in paper (DragonFixWiringTest).
     */
    public interface BodyAttribute {
        double typeBase();
        double baseValue();
        void setBaseValue(double value);
    }

    /**
     * Speed is a MULTIPLIER of the type's own base movement speed. Read from {@code typeBase()}, never from
     * {@code baseValue()}, which is what makes a second call a no-op, and what makes a grown body's next stage land
     * on the type's base times the new multiplier rather than on its already-written base.
     */
    public static void applySpeed(BodyAttribute speed, double multiplier) {
        speed.setBaseValue(speed.typeBase() * multiplier);
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
