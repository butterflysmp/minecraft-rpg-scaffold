package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.fight.FightSettings;
import io.github.butterflysmp.rpg.core.fight.RingLook;

/**
 * One fight file, loaded ({@code content/fights/<id>.yml}): the fight's numbers, how its ring is drawn, the Mage and
 * the crystals (steps F6 + F7), and the dev fixture's participant radius.
 *
 * @param id                       the file's name without {@code .yml}
 * @param settings                 every number the ring, the lightning and the Mage cycle run on
 * @param look                     how the ring is drawn
 * @param mageMob                  the mob file the fixture spawns as the Mage ({@code content/mobs/<id>.yml})
 * @param refusedCue               the sound at the Mage when a hit on him is refused
 * @param crystalRingRadiusBlocks  the crystals' spots lie evenly round a circle about C of this radius, blocks
 * @param crystalHeightBlocks      and this far above C's ground, blocks
 * @param litCue                   the sound at a crystal as it lights
 * @param participantRadiusBlocks  the fixture's reach about C, blocks
 */
public record FightContent(String id, FightSettings settings, RingLook look, String mageMob, Cue refusedCue,
                           double crystalRingRadiusBlocks, double crystalHeightBlocks, Cue litCue,
                           double participantRadiusBlocks) {

    /** A sound: its key ({@code item.shield.block}), volume and pitch. */
    public record Cue(String sound, float volume, float pitch) {}
}
