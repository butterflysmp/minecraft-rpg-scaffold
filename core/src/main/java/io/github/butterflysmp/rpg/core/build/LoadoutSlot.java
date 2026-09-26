package io.github.butterflysmp.rpg.core.build;

/**
 * The three casting slots of a loadout, each bound to one Ability Stone input (PLAN-build-system.md,
 * ruling 2): left click casts {@link #ACTIVE_1}, right click {@link #ACTIVE_2}, Q {@link #ULTIMATE}.
 */
public enum LoadoutSlot {
    ACTIVE_1,
    ACTIVE_2,
    ULTIMATE
}
