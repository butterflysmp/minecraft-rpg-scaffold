package io.github.butterflysmp.rpg.core.build;

import java.util.Optional;

/**
 * Which loadout slot, if any, one Ability Stone input casts (PLAN-build-system.md section 2.5).
 *
 * <p>Pure, so the whole grid is a unit test. The paper side works out the three facts and does the
 * cast; this decides. Game mode is NOT a parameter, and that is measured rather than assumed: the
 * spike at {@code d7b4087} (section 3.1.0.1) found creative's hotbar Q, clicks and holds arrive as the
 * same events as survival's, and creative's in-screen Q is a screen input like survival's.
 */
public final class StoneInput {

    private StoneInput() {}

    /** The three physical inputs the stone listens to. */
    public enum Input { LEFT, RIGHT, DROP }

    /**
     * @param mainHand   the event came from the main hand. A right-click on a block fires once per hand
     *                   in the same tick (measured), and only the main-hand one may cast.
     * @param screenOpen the input arrived through an open inventory screen. A cast from there would fire
     *                   at an aim the player cannot see behind the menu, so it refuses -- Q over the
     *                   stone in the inventory casts NOTHING (the clarification under RULINGS).
     * @return the slot to cast, or empty for a refused input
     */
    public static Optional<LoadoutSlot> slotFor(Input input, boolean mainHand, boolean screenOpen) {
        if (!mainHand || screenOpen) return Optional.empty();
        return Optional.of(switch (input) {
            case LEFT -> LoadoutSlot.ACTIVE_1;
            case RIGHT -> LoadoutSlot.ACTIVE_2;
            case DROP -> LoadoutSlot.ULTIMATE;
        });
    }
}
