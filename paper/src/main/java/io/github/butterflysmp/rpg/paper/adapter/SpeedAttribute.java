package io.github.butterflysmp.rpg.paper.adapter;

/**
 * The three movement-speed operations Soaked needs, as a Bukkit-free seam so the
 * modifier lifecycle is unit-testable without a server.
 *
 * The granularity is deliberate: exposing the keyed modifier's presence and a raw add/remove
 * -- rather than an opaque setSlow(factor) -- is what lets a test prove the *single stable
 * modifier* property. If Soaked added a modifier each tick instead of replacing the one
 * keyed modifier, the mob would grind far below the floor and never recover; the factor math
 * would be right and the application would leak. That bug is only observable if the seam lets
 * a test count modifiers. The "replace" (remove-if-present then add) therefore lives in
 * {@link SoakedStatus}, tested here; the Paper adapter ({@code EntitySpeedAttribute}) is 1:1
 * with the AttributeInstance API and boot-witnessed.
 */
public interface SpeedAttribute {

    /** Is our one keyed slow modifier currently present? */
    boolean hasSoakModifier();

    /** Add ONE keyed modifier that multiplies base speed by {@code factor} (0 < factor <= 1). */
    void addSoakModifier(double factor);

    /** Remove our keyed modifier if present. Restores base speed exactly when it was the only one. */
    void removeSoakModifier();
}
