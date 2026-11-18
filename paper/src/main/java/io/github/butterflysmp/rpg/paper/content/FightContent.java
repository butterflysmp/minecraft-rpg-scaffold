package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.fight.FightSettings;
import io.github.butterflysmp.rpg.core.fight.RingLook;

/**
 * One fight file, loaded ({@code content/fights/<id>.yml}): the fight's numbers, how its ring is drawn, and the dev
 * fixture's participant radius.
 *
 * @param id                       the file's name without {@code .yml}
 * @param settings                 every number the ring, the lightning and the Mage cycle run on
 * @param look                     how the ring is drawn
 * @param participantRadiusBlocks  the fixture's reach about C, blocks
 */
public record FightContent(String id, FightSettings settings, RingLook look, double participantRadiusBlocks) {}
