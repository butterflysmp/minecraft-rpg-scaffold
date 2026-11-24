package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.enchant.CandidateCascade;
import io.github.butterflysmp.rpg.core.enchant.CandidateCascade.Tier;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** {@code /rpg enchant rollstats <n>}'s lines, exactly (2026-11-23): the gate predicts their shape, not their counts. */
class EnchantRollStatsTest {

    @Test
    void theLinesNameTheGearTheRollsEachSlotsCountsAndTheTrueOdds() {
        CandidateCascade bens = new CandidateCascade(List.of(
                new Tier(4, 0.10), new Tier(3, 0.40), new Tier(2, 0.70), new Tier(1, 1.0)));
        int[][] tally = {{0, 1000, 0, 0, 0}, {0, 160, 380, 360, 100}, {0, 1, 2, 3, 994}, {1000, 0, 0, 0, 0}};
        assertEquals(List.of(
                "ROLLSTATS iron_sword element=kinetic rolls=1000 uniquePool=1 normalPool=2",
                "ROLLSTATS slot 0 (unique): 0=0 1=1000 2=0 3=0 4=0",
                "ROLLSTATS slot 1: 0=0 1=160 2=380 3=360 4=100",
                "ROLLSTATS slot 2: 0=0 1=1 2=2 3=3 4=994",
                "ROLLSTATS slot 3: 0=1000 1=0 2=0 3=0 4=0",
                "ROLLSTATS true odds (before the pool cap): 4=10.000% 3=36.000% 2=37.800% 1=16.200%"),
                EnchantRollItems.rollStatsLines("iron_sword", Optional.of("kinetic"), 1000, tally, bens, 1, 2));
    }
}
