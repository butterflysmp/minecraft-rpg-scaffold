package io.github.butterflysmp.rpg.core.build;

/**
 * Where the two locked items -- the Nexus star and the Ability Stone -- may sit, so that they can
 * NEVER share a slot (PLAN-build-system.md section 1.3).
 *
 * <p>Two locked items pointing at one slot would fight: converging one hands the other back through
 * {@code MenuSafety.give}, and converging the other moves it back again. This class is the rule that
 * makes that unreachable, and it is pure so the "never share" claim is a test over sequences of picks,
 * not a hope.
 *
 * <p>Slots are {@code PlayerInventory} indices: 0-8 hotbar, 9-35 storage.
 */
public final class LockedSlots {

    /** The star may sit in any of the 36 main slots. */
    public static final int STAR_MAX_SLOT = 35;

    /** The stone may sit on the HOTBAR only (ruling 2). */
    public static final int STONE_MAX_SLOT = 8;

    /** The stone's default (ruling 13): hotbar slot 7, beside the star's default of 8. */
    public static final int DEFAULT_STONE_SLOT = 7;

    private LockedSlots() {}

    /** Why a pick is refused. */
    public enum Refusal {
        /** Outside the item's allowed range. */
        OUT_OF_RANGE,
        /** The other locked item's slot -- the picker's "reserved" branch. */
        RESERVED
    }

    /** @return the refusal, or null if the star may move to {@code slot} while the stone is at {@code stoneSlot} */
    public static Refusal refuseStarPick(int slot, int stoneSlot) {
        if (slot < 0 || slot > STAR_MAX_SLOT) return Refusal.OUT_OF_RANGE;
        return slot == stoneSlot ? Refusal.RESERVED : null;
    }

    /** @return the refusal, or null if the stone may move to {@code slot} while the star is at {@code starSlot} */
    public static Refusal refuseStonePick(int slot, int starSlot) {
        if (slot < 0 || slot > STONE_MAX_SLOT) return Refusal.OUT_OF_RANGE;
        return slot == starSlot ? Refusal.RESERVED : null;
    }

    /**
     * The stone's EFFECTIVE slot, given what the profile stores and where the star is.
     *
     * <p>A stored slot is honoured when it is on the hotbar and not the star's. The picks above keep
     * those two true for every slot a player chooses -- so the fallback exists for the one case no pick
     * produces: a player who moved the STAR to hotbar 7 before the stone existed, and so has no stone
     * slot stored and a default that the star already holds. They get the highest hotbar slot the star
     * is not in. A stored value outside the hotbar (a hand-edited file) takes the same route.
     *
     * @param storedOrNull the profile's stone slot; null means "never chosen"
     */
    public static int stoneSlot(Integer storedOrNull, int starSlot) {
        int wanted = storedOrNull == null ? DEFAULT_STONE_SLOT : storedOrNull;
        if (wanted >= 0 && wanted <= STONE_MAX_SLOT && wanted != starSlot) return wanted;
        if (DEFAULT_STONE_SLOT != starSlot) return DEFAULT_STONE_SLOT;
        for (int slot = STONE_MAX_SLOT; slot >= 0; slot--) {
            if (slot != starSlot) return slot;
        }
        throw new AssertionError("a hotbar of nine always has a slot the star is not in");
    }
}
