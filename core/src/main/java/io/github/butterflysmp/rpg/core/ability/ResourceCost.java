package io.github.butterflysmp.rpg.core.ability;

public record ResourceCost(String resourceId, double amount) {

    /**
     * The resource an ability spends unless its content says otherwise, and the id anything
     * DISPLAYING that resource must read.
     *
     * This exists as one constant because a mismatch between the two is silent. Nothing validates
     * a resource id at load time, and {@code ResourcePool.current} returns the pool's max for an id
     * it has never seen -- so a display reading a different id than the abilities spend would show a
     * full, never-moving bar rather than throwing or warning. Two literals cannot drift apart if
     * there is only one.
     */
    public static final String DEFAULT_RESOURCE = "mana";

    /** The reserved free-trigger sentinel. Not a pool: nothing is ever spent or displayed for it. */
    public static final ResourceCost FREE = new ResourceCost("none", 0);
}