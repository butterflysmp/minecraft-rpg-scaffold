package io.github.butterflysmp.rpg.core.summon;

/**
 * ONE SUMMON, AS DECLARED IN {@code content/summons/<id>.yml} (D2, the seat's 3a, 2026-11-24). No summon id or weapon id
 * is named in Java; a weapon names its summon by a {@code type: summon} cast.
 *
 * <p>The numbers, and whose they are: {@code health} and {@code damage} are the AUTHORED values, scaled by the weapon's
 * gear score at the cast (D3). {@code guardRadius} 8 and {@code triggerRadius} 30 are BEN's (D4). {@code chaseLimit}
 * (32, measured from the CASTER), {@code teleportBack} (48), {@code attackCadenceTicks} (20) and
 * {@code damagedWindowTicks} (how long "a mob that damaged him" stays a reason to attack it) are SEAT PLACEHOLDERS, OPEN
 * FOR BEN; each is required in the file, with no default, so none is invented here.
 *
 * <p>(A {@code record} is a small immutable class: Java writes its constructor and getters from the list. The block
 * after the header is the "compact constructor", which runs first and refuses a bad value.)
 */
public record SummonDefinition(String id, String entity, String name, String element, double health, double damage,
                               double guardRadius, double triggerRadius, double chaseLimit, double teleportBack,
                               int attackCadenceTicks, int damagedWindowTicks) {

    public SummonDefinition {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("a summon needs an id");
        if (entity == null || entity.isBlank()) throw new IllegalArgumentException("summon '" + id + "' names no entity");
        if (!(health > 0 && Double.isFinite(health))) {
            throw new IllegalArgumentException("summon '" + id + "' health must be more than 0, was " + health);
        }
        if (!(damage > 0 && Double.isFinite(damage))) {
            throw new IllegalArgumentException("summon '" + id + "' damage must be more than 0, was " + damage);
        }
        positive(id, "guard_radius", guardRadius);
        positive(id, "trigger_radius", triggerRadius);
        positive(id, "chase_limit", chaseLimit);
        positive(id, "teleport_back", teleportBack);
        positive(id, "attack_cadence_ticks", attackCadenceTicks);
        positive(id, "damaged_window_ticks", damagedWindowTicks);
        // The seat's D4: a chase limit below the trigger radius would make Ben's "targets the caster within 30" false.
        if (chaseLimit < triggerRadius) {
            throw new IllegalArgumentException("summon '" + id + "' chase_limit " + chaseLimit
                    + " is below its trigger_radius " + triggerRadius + ": it could never reach a mob it must attack");
        }
        if (teleportBack <= guardRadius) {
            throw new IllegalArgumentException("summon '" + id + "' teleport_back " + teleportBack
                    + " must be beyond its guard_radius " + guardRadius);
        }
    }

    private static void positive(String id, String key, double v) {
        if (!(v > 0 && Double.isFinite(v))) {
            throw new IllegalArgumentException("summon '" + id + "' " + key + " must be more than 0, was " + v);
        }
    }
}
