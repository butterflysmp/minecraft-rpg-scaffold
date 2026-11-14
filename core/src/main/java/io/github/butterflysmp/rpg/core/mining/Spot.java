package io.github.butterflysmp.rpg.core.mining;

/**
 * A weak spot, centred on patch pixel {@code (cu, cv)}. Its 3 x 3 mark covers pixels
 * {@code cu-1..cu+1}, and its 5 x 5 hit box covers {@code cu-2..cu+2} (see {@link SpotBox}).
 */
public record Spot(int cu, int cv) {}
