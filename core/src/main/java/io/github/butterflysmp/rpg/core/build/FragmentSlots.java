package io.github.butterflysmp.rpg.core.build;

/**
 * A cell's fragment slots: a FIXED four (ruling 4 -- aspects do not add slots). Also the stat-source key
 * each slot contributes under, {@code fragment:<slot>}, which must stay disjoint from every other
 * scanner's prefix ({@code AccessorySourceKeysTest}) because {@code ModifierReconciler} removes any key
 * absent from the desired map.
 */
public final class FragmentSlots {

    private FragmentSlots() {}

    /** Four, fixed. {@code CellLoadout.FRAGMENTS} stores the same four; a paper test pins the two equal. */
    public static final int COUNT = 4;

    public static final String SOURCE_PREFIX = "fragment:";

    public static String sourceKey(int slot) {
        if (slot < 0 || slot >= COUNT) throw new IllegalArgumentException("fragment slot " + slot);
        return SOURCE_PREFIX + slot;
    }
}
