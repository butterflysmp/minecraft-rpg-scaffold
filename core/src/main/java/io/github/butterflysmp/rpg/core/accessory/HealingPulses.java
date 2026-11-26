package io.github.butterflysmp.rpg.core.accessory;

import io.github.butterflysmp.rpg.core.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * ONE PULSE OF A HEALING PULSE, AS A PURE DECISION (the seat's H5, 2026-11-26): who is a candidate, who is hurt, and how
 * much each is healed. No Bukkit; the paper side gathers the candidates on the wearer's own thread and applies the heals
 * each on its target's thread ({@code TomePulses}). {@code HealingPulsesTest} pins it as a table.
 *
 * <h2>The rule</h2>
 *
 * <ul>
 *   <li><b>A candidate</b> is an entity the behaviour's targets allow (players: never a mob, a summon or an armour stand,
 *       H7), in the wearer's world, whose feet are within {@code radius} blocks of the wearer's feet (inclusive; CC'S
 *       CALL), alive (not dead and custom health above 0), and not a spectator. The wearer is one of them.
 *   <li><b>A hurt candidate</b> (health below its max, the engine's custom values) is healed by
 *       {@code min(level x heal_per_level, max - health)}, so a heal never raises health past max. A candidate at full
 *       health is skipped. Nobody hurt: no heal at all.
 *   <li><b>{@code level}</b> is the WEARER'S level at the pulse's tick, never the target's (Ben: "1 health per the players
 *       level that has it equipped").
 * </ul>
 */
public final class HealingPulses {

    private HealingPulses() {}

    /** What an entity near the wearer is, as far as the pulse cares. */
    public enum Kind { PLAYER, MOB, SUMMON, ARMOR_STAND, OTHER }

    /**
     * One entity near the wearer, read on the wearer's thread: its custom {@code health} and {@code max} as the stats
     * sheet reads them, its world's name, its feet.
     */
    public record Candidate(UUID id, String name, Kind kind, String world, Vec3 feet, boolean alive, boolean spectator,
                            double health, double max) {}

    /** One heal the pulse decided: the target's health before, the amount, and the health it will read after. */
    public record Heal(UUID id, String name, double before, double amount, double after) {}

    /** The pulse's answer: how many candidates it counted, and each heal, in candidate order. */
    public record Decision(int candidates, List<Heal> heals) {
        public Decision {
            heals = List.copyOf(heals);
        }

        public int healed() {
            return heals.size();
        }
    }

    /** Is this entity one the pulse counts at all (H5, H7)? Hurt or not is decided after. */
    public static boolean isCandidate(AccessoryBehaviour.HealingPulse pulse, String wearerWorld, Vec3 wearerFeet,
                                      Candidate c) {
        boolean allowedKind = switch (pulse.targets()) {
            case PLAYERS -> c.kind() == Kind.PLAYER;
        };
        return allowedKind
                && c.world() != null && c.world().equals(wearerWorld)
                && c.feet().distanceSquared(wearerFeet) <= pulse.radius() * pulse.radius()
                && c.alive() && c.health() > 0
                && !c.spectator();
    }

    /** The amount one pulse at {@code level} heals a candidate missing {@code missing}: never more than it misses. */
    public static double amount(AccessoryBehaviour.HealingPulse pulse, int level, double missing) {
        return Math.min(level * pulse.healPerLevel(), missing);
    }

    /**
     * Decide one pulse. Each candidate is counted once (by id, first seen), whatever the search returned.
     *
     * @param level the wearer's level at this tick, 1 or more
     */
    public static Decision decide(AccessoryBehaviour.HealingPulse pulse, int level, String wearerWorld, Vec3 wearerFeet,
                                  List<Candidate> nearby) {
        if (level < 1) throw new IllegalArgumentException("a level is 1 or more, was " + level);
        Set<UUID> seen = new HashSet<>();
        int candidates = 0;
        List<Heal> heals = new ArrayList<>();
        for (Candidate c : nearby) {
            if (!seen.add(c.id())) continue;
            if (!isCandidate(pulse, wearerWorld, wearerFeet, c)) continue;
            candidates++;
            if (!(c.health() < c.max())) continue;   // full: skipped
            double a = amount(pulse, level, c.max() - c.health());
            heals.add(new Heal(c.id(), c.name(), c.health(), a, Math.min(c.max(), c.health() + a)));
        }
        return new Decision(candidates, heals);
    }
}
