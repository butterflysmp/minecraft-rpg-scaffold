package io.github.butterflysmp.rpg.paper.selftest;

import io.github.butterflysmp.rpg.storage.PlayerProfile;

/**
 * C1, AS FIXED BY THE SEAT (2026-11-24): the selftest's capture of his STORED max_summons base, with its OWN flag saying
 * whether it was captured at all. A profile not loaded at the run's start is NOT captured, and is never restored: writing
 * the null it would carry would erase a real base. A captured null ("never set") IS restored, as null.
 *
 * <p>(A {@code record} is a small immutable class: Java writes its constructor and getters from the list.)
 */
record SelfTestSummonsBase(boolean captured, Integer base) {

    static final SelfTestSummonsBase NOT_CAPTURED = new SelfTestSummonsBase(false, null);

    /** What the run's start saw: nothing when the profile is not loaded, else the boxed base, null included. */
    static SelfTestSummonsBase capture(PlayerProfile profileOrNull) {
        return profileOrNull == null ? NOT_CAPTURED : new SelfTestSummonsBase(true, profileOrNull.maxSummonsOrNull());
    }

    /** Does the restore write it back? Only what was captured. */
    boolean writes() {
        return captured;
    }
}
