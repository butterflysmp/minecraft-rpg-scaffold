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
 */
public record FightContent(String id, FightSettings settings, RingLook look, String mageMob, Cue refusedCue,
                           double crystalRingRadiusBlocks, double crystalHeightBlocks, Cue litCue,
                           double participantRadiusBlocks, Arena arena) {

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
    public record Arena(ArenaShape shape, long timeOfDayTicks, double entryDxBlocks, double entryDzBlocks) {}
}
