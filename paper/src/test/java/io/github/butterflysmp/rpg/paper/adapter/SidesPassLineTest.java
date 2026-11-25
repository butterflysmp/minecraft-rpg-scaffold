package io.github.butterflysmp.rpg.paper.adapter;

import io.github.butterflysmp.rpg.core.mob.MobHostility;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** X4(a): the shape GATE-summoner.md SM6 reads, exactly. */
class SidesPassLineTest {

    @Test
    void theLineNamesTheAttackerAndTheBody() {
        UUID body = UUID.fromString("00000000-0000-0000-0000-00000000000b");
        assertEquals("SIDES pass attacker=PLAYER body=00000000-0000-0000-0000-00000000000b",
                PaperCombatWorld.sidesPassLine(MobHostility.Side.PLAYER, body));
    }
}
