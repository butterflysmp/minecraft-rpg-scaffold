package io.github.butterflysmp.rpg.paper.adapter;

/**
 * The three movement-speed operations Soaked needs, as a Bukkit-free seam so the
 * modifier lifecycle is unit-testable without a server.
 *
 * The granularity is deliberate: exposing the keyed modifier's presence and a raw add/remove
 * -- rather than an opaque setSlow(factor) -- is what lets a test prove the *single stable
 * modifier* property. If a caller added a modifier each tick instead of replacing the one
 * keyed modifier, the mob would drift below the intended value and never recover; the factor
 * math would be right and the application would leak. That bug is only observable if the seam
 * lets a test count modifiers. The Paper adapter ({@code EntitySpeedAttribute}) is 1:1 with
 * the AttributeInstance API and boot-witnessed.
 *
 * Two statuses use this, each with its own key: Soaked (factor {@code max(0.6, 0.9^stacks)})
 * and Rooted/Immobilize (factor {@code 0}, killing the AI's movement drive).
 */
public interface SpeedAttribute {

    /** Is our one keyed speed modifier currently present? */
    boolean hasSpeedModifier();

    /** Add ONE keyed modifier that multiplies base speed by {@code factor} (0 <= factor <= 1). */
    void addSpeedModifier(double factor);

    /** Remove our keyed modifier if present. Restores base speed exactly when it was the only one. */
    void removeSpeedModifier();
}
