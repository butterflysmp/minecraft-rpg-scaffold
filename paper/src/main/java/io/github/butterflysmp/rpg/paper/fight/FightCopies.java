package io.github.butterflysmp.rpg.paper.fight;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Mannequin;

/**
 * A DOWN PLAYER'S COPY (step F9e a): an {@link Mannequin} with his look and name, lying on the floor. It is a
 * {@code LivingEntity} (READ, javap on the pinned paper-api: {@code Mannequin extends LivingEntity}), so every system
 * that treats living things as combatants or mobs must leave it out. They all ask THIS predicate. The list of places is
 * in {@code GATE-fight-revive.md}, each with its test.
 *
 * <p><b>EVERY MANNEQUIN IS A COPY</b> (CC's reading, flagged): nothing else in this plugin makes one, so the class alone
 * decides, and a copy cannot slip through for want of a tag. {@link #TAG} is also set, so a probe and a cleanup can find
 * the fight's copies by name.
 */
public final class FightCopies {

    private FightCopies() {}

    /** The scoreboard tag on every copy the fight lays. */
    public static final String TAG = "rpg_fight_copy";

    /** Whether this entity is a fight's copy, which no combat system may see. */
    public static boolean isCopy(Entity entity) {
        return entity instanceof Mannequin;
    }
}
