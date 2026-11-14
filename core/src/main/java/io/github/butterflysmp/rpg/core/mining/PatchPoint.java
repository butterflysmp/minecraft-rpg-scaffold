package io.github.butterflysmp.rpg.core.mining;

/**
 * A point on a patch, in fractional PIXELS (1 pixel = 1/16 block). {@code u} runs to the player's
 * right and {@code v} up, from the footprint's bottom-left corner as the player sees it. Pixel
 * {@code (cu, cv)} covers {@code [cu, cu + 1) x [cv, cv + 1)}.
 */
public record PatchPoint(double u, double v) {}
