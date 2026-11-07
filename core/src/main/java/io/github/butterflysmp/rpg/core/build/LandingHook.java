package io.github.butterflysmp.rpg.core.build;

import io.github.butterflysmp.rpg.core.Vec3;

/**
 * What a leap does when its mark ends: Sunder's slam (PLAN-melee-class.md section 8.2, S1). Handed to
 * {@code CombatantHandle.armLanding} by the dash and called back by whoever steps the mark, on the thread that owns
 * the leaping entity. EXACTLY ONE of the two methods is called, exactly once, per arming.
 */
public interface LandingHook {

    /** The mark ended in a slamming {@link Touchdown} ({@link Touchdown#slams()}), with the leaper's live feet. */
    void landed(Vec3 feet, Touchdown how);

    /** The mark ended without a slam: {@link Touchdown#BACKSTOP}, {@link Touchdown#FORGOTTEN} or {@link Touchdown#SUPERSEDED}. */
    void ended(Touchdown how);
}
