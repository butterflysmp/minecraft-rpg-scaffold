package io.github.butterflysmp.rpg.core.mob;

/**
 * Which dimension's gear score a mob gets (PLAN-mob-scaling.md, M25, which replaced the M3/M11/M12
 * distance curves): a flat 100 or 200, or the End's curve. A core enum rather than Bukkit's
 * {@code World.Environment}, because core has no Bukkit;
 * paper maps one onto the other ({@code MobOrigin}), and a {@code CUSTOM} world maps to OVERWORLD.
 */
public enum MobDimension {
    OVERWORLD,
    NETHER,
    END
}
