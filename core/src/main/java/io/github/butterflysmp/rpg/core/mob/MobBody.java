package io.github.butterflysmp.rpg.core.mob;

import java.util.OptionalDouble;

/**
 * A custom mob's body fields (PLAN-dragon-mobs.md step R1, the plan's MB1 limited to scale and speed; health is
 * {@link MobDefinition#maxHealth}). Each field is OPTIONAL, and absent means "leave the vanilla value alone", so a
 * mob file with neither key loads and spawns exactly as it did before this record existed.
 *
 * <p><b>NAME THE QUANTITY:</b>
 * <ul>
 *   <li>{@code scale} is the value of the entity's {@code SCALE} attribute (vanilla default 1.0). It sizes the
 *       hitbox and the model together (PLAN-dragon-mobs.md section 7 F-C).</li>
 *   <li>{@code speedMultiplier} MULTIPLIES the base entity's DEFAULT movement-speed attribute, never an absolute
 *       speed: 0.6 on a wither skeleton is 0.6 x that type's default. See {@link MobBodies#applySpeed}.</li>
 * </ul>
 *
 * <p>({@code OptionalDouble} is Java's "a double, or nothing": {@code OptionalDouble.empty()} means the file did not
 * author the key.)
 */
public record MobBody(OptionalDouble scale, OptionalDouble speedMultiplier) {

    /** Neither field authored: the body stays vanilla. */
    public static final MobBody VANILLA = new MobBody(OptionalDouble.empty(), OptionalDouble.empty());

    public MobBody {
        if (scale == null) scale = OptionalDouble.empty();
        if (speedMultiplier == null) speedMultiplier = OptionalDouble.empty();
        requirePositive("scale", scale);
        requirePositive("speed", speedMultiplier);
    }

    public boolean isVanilla() {
        return scale.isEmpty() && speedMultiplier.isEmpty();
    }

    private static void requirePositive(String name, OptionalDouble value) {
        if (value.isPresent() && !(value.getAsDouble() > 0 && Double.isFinite(value.getAsDouble()))) {
            throw new IllegalArgumentException(name + " must be a positive number, was " + value.getAsDouble());
        }
    }
}
