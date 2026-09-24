package io.github.butterflysmp.rpg.paper.adapter;

/**
 * A {@link SpeedAttribute} that models the modifier state a test needs to READ BACK:
 * how many of our modifiers are present (must be 0 or 1, never N) and the resulting speed
 * factor. Without this, "exactly one modifier" and "base restored exactly" are boot-only --
 * too weak for the leak case, the same argument as the primitive's cancel-on-removal.
 *
 * It is wired to a {@link FakeTickTarget}'s active flag and throws on ANY touch while
 * inactive -- modelling CLAUDE.md's cardinal hazard, that you must never touch a removed
 * entity. So the death-path test fails loudly if the code reaches for the attribute after
 * the mob is gone, instead of that bug hiding until a server runs at scale.
 */
final class FakeSpeedAttribute implements SpeedAttribute {

    private final FakeTickTarget target;
    private int modifiers = 0;     // our keyed modifier count; a naive add-without-replace makes this climb
    private double factor = 1.0;   // the resulting speed multiple

    FakeSpeedAttribute(FakeTickTarget target) { this.target = target; }

    private void guard() {
        if (!target.isActive()) throw new AssertionError("touched a removed entity");
    }

    /**
     * Make the NEXT {@link #removeSpeedModifier} throw, once, and then behave normally.
     *
     * <p>Added 2026-09-24 for the loop-isolation rows. {@code SoakedStatus}'s body only counts down
     * and calls this at expiry, so <b>there is no other way to make that body throw through its
     * public API</b> -- and without a throwing body there is nothing to prove that {@code onStop}
     * releases the modifier when the expiry arm is skipped.
     *
     * <p><b>ONE-SHOT ON PURPOSE.</b> The release under test is the SECOND call, from {@code onStop};
     * a latch that stayed set would make that call throw too and the row could not tell "onStop
     * released it" from "onStop was never reached".
     */
    boolean failNextRemove = false;

    @Override public boolean hasSpeedModifier() { guard(); return modifiers > 0; }
    @Override public void addSpeedModifier(double f) { guard(); modifiers++; factor = f; }
    @Override public void removeSpeedModifier() {
        guard();
        if (failNextRemove) {
            failNextRemove = false;
            throw new IllegalStateException("staged failure inside removeSpeedModifier");
        }
        modifiers = 0;
        factor = 1.0;
    }

    /** How many of our modifiers are on the entity. The single-stable-modifier probe. */
    int modifierCount() { return modifiers; }

    /** Base speed multiple: 1.0 means base exactly restored. */
    double speedFactor() { return modifiers == 0 ? 1.0 : factor; }
}
