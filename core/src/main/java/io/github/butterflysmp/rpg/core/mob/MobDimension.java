package io.github.butterflysmp.rpg.core.mob;

/**
 * Which of the three gear-score curves a mob's position is read against (PLAN-mob-scaling.md, M3,
 * M11, M12). A core enum rather than Bukkit's {@code World.Environment}, because core has no Bukkit;
 * paper maps one onto the other ({@code MobOrigin}), and a {@code CUSTOM} world maps to OVERWORLD.
 */
public enum MobDimension {
    OVERWORLD,
    NETHER,
    END
}
