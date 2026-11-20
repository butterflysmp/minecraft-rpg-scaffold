package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.fight.ArenaShape;
import io.github.butterflysmp.rpg.core.fight.FightSettings;
import io.github.butterflysmp.rpg.core.fight.RingLook;

/**
 * One fight file, loaded ({@code content/fights/<id>.yml}): the fight's numbers, how its ring is drawn, the Mage and
 * the crystals (steps F6 + F7), the dev fixture's participant radius, and the fight's own world and arena (step F8b).
 *
 * @param id                       the file's name without {@code .yml}
 * @param settings                 every number the ring, the lightning and the Mage cycle run on
 * @param look                     how the ring is drawn
 * @param mageMob                  the mob file the fixture spawns as the Mage ({@code content/mobs/<id>.yml})
 * @param refusedCue               the sound at the Mage when a hit on him is refused
 * @param crystalRingRadiusBlocks  in an ordinary world, the crystals' spots lie evenly round a circle about C of this
 *                                 radius, blocks
 * @param crystalHeightBlocks      and this far above C's ground, blocks
 * @param litCue                   the sound at a crystal as it lights
 * @param participantRadiusBlocks  the ordinary-world fixture's reach about C, blocks
 * @param arena                    the fight's own world and its generated arena (step F8b)
 * @param dragon                   the arena fight's dragon (step F9)
 * @param mageLandingLightning     a lightning EFFECT at the Mage's landing spot on the landing tick (F9b, section 6;
 *                                 Ben: "Add a lightning strike to the mage landing"), both sites
 */
public record FightContent(String id, FightSettings settings, RingLook look, String mageMob, Cue refusedCue,
                           double crystalRingRadiusBlocks, double crystalHeightBlocks, Cue litCue,
                           double participantRadiusBlocks, Arena arena, Dragon dragon, boolean mageLandingLightning,
                           Revive revive) {

    /**
     * THE REVIVE (step F9e; Ben, PLAN-dragon-fight.md section 1, 0d and 0e).
     *
     * @param radiusBlocks   how near his copy a reviver must sneak ({@code revive_radius_blocks}; SEAT PLACEHOLDER 1.5,
     *                       Ben's "on top of")
     * @param seconds        how long, without a break ({@code revive_seconds}; Ben's "3 seconds")
     * @param healthFraction his health on revive, of his maximum ({@code revive_health_fraction}; Ben's "50% health")
     * @param maxRevives     revives per player per fight; empty (the key absent) is unlimited (Ben: "No")
     * @param label          the line above the copy (SEAT PLACEHOLDER wording); {@code {player}} and {@code {seconds}}
     *                       are filled in
     */
    public record Revive(double radiusBlocks, int seconds, double healthFraction, java.util.OptionalInt maxRevives,
                         String label) {}

    /**
     * THE ARENA FIGHT'S DRAGON (step F9).
     *
     * @param mob             the mob file it is spawned from ({@code content/mobs/<id>.yml}), at gear score 100
     * @param mageThresholds  Ben's fractions of its maximum, strictly falling: each one it falls strictly below lands a
     *                        Mage (the queue is core's {@code DragonCrossings})
     * @param winReturnTicks  its death is the win; the arena's normal end follows this many ticks later
     * @param bossBarColor    the boss bar's colour, an Adventure colour name ({@code purple})
     */
    public record Dragon(String mob, java.util.List<Double> mageThresholds, int winReturnTicks, String bossBarColor,
                         io.github.butterflysmp.rpg.core.fight.DragonAttacks attacks) {}

    /** A sound: its key ({@code item.shield.block}), volume and pitch. */
    public record Cue(String sound, float volume, float pitch) {}

    /**
     * THE FIGHT'S OWN WORLD AND ITS GENERATED STAND-IN ARENA (the F8 brief, section 4 a and b; SEAT PLACEHOLDERS).
     *
     * @param shape          the disc and the pillars; crystal spot n on pillar n
     * @param timeOfDayTicks the world's fixed time (18000 is midnight; the seat's placeholder for the sky)
     * @param entryDxBlocks  where an entering player stands, blocks from the arena's centre, x
     * @param entryDzBlocks  and z
     */
    /**
     * @param environment F9c (Ben, 2026-11-19: "yes let's try it", plan 6.9 option a): the fight world's environment.
     *                    END gives the End's sky; vanilla's own dragon fight is made inert by a per-world Paper switch
     *                    and checked after creation. NORMAL is the night-sky arena of F8b.
     * @param allDownSeconds F9d c (SEAT PLACEHOLDER): seconds between ALL DOWN (no participant alive inside, at least
     *                    one down) and everyone's return home; 0 or more
     */
    public record Arena(ArenaShape shape, long timeOfDayTicks, double entryDxBlocks, double entryDzBlocks,
                        Environment environment, int allDownSeconds, Source source, String templateFile) {}

    /**
     * Where the arena comes from (PLAN-dragon-mobs.md C3; the seat's queue, item 2): {@code generated} (the stand-in,
     * the shipped choice) or {@code template} with {@code template_file: <name>}, a saved structure in the plugin's
     * {@code arenas/<name>.nbt}. A missing or malformed template is refused by name when the fight starts, and the
     * generated arena is built instead.
     */
    public enum Source {
        GENERATED, TEMPLATE;

        public String fileName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }

    /** The fight world's environment: {@code end} or {@code normal} in the file. */
    public enum Environment {
        END, NORMAL;

        public String fileName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }
}
