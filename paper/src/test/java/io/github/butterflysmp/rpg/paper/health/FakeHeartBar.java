package io.github.butterflysmp.rpg.paper.health;

/**
 * A {@link HeartBar} that records what was written, so the tier-to-vanilla rendering is unit-testable
 * without a server -- the same shape as {@code FakeSpeedAttribute}.
 *
 * It has no way to REPORT a vanilla health value back to the renderer, by design: the renderer must
 * derive its write from custom health alone, and the write-only seam is what guarantees it cannot do
 * otherwise. The recorded fields are the probes a test asserts on.
 */
final class FakeHeartBar implements HeartBar {

    int maxHealthPoints = -1;
    double healthPoints = -1;
    int renders = 0;

    @Override
    public void render(int maxHealthPoints, double healthPoints) {
        this.maxHealthPoints = maxHealthPoints;
        this.healthPoints = healthPoints;
        this.renders++;
    }
}
