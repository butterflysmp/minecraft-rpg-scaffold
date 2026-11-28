package io.github.butterflysmp.rpg.core.combat;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static io.github.butterflysmp.rpg.core.combat.Douse.Ignition.CLAUSE_1;
import static io.github.butterflysmp.rpg.core.combat.Douse.Ignition.CLAUSE_2;
import static io.github.butterflysmp.rpg.core.combat.Douse.Ignition.NONE;
import static io.github.butterflysmp.rpg.core.combat.Douse.Ignition.REFUSED;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Water puts out Scorch: the one wet decision (Y2) and E1's death table, as tables; the three trace lines (Y7, E1). */
class DouseTest {

    private static final Optional<Douse.Cause> DRY = Optional.empty();
    private static final Optional<Douse.Cause> WATER = Optional.of(Douse.Cause.WATER);
    private static final Optional<Douse.Cause> RAIN = Optional.of(Douse.Cause.RAIN);

    @Test
    void theWetDecisionIsATableOfTwoBooleansAndWaterNamesItselfFirst() {
        assertEquals(DRY, Douse.wet(false, false));
        assertEquals(WATER, Douse.wet(true, false));
        assertEquals(RAIN, Douse.wet(false, true));
        assertEquals(WATER, Douse.wet(true, true), "CC'S CALL: both hold, the line names the water");
    }

    @Test
    void eOnesTableEveryCellOfScorchedFireKillAndWet() {
        // scorched, fire kill, wet -> what the death does. A wet body ignites by NEITHER clause (E1).
        assertEquals(NONE, Douse.atDeath(false, false, DRY));
        assertEquals(NONE, Douse.atDeath(false, false, WATER), "nothing to refuse: no line either");
        assertEquals(CLAUSE_2, Douse.atDeath(false, true, DRY));
        assertEquals(REFUSED, Douse.atDeath(false, true, WATER), "clause 2 does not detonate a wet body");
        assertEquals(REFUSED, Douse.atDeath(false, true, RAIN));
        assertEquals(CLAUSE_1, Douse.atDeath(true, false, DRY));
        assertEquals(REFUSED, Douse.atDeath(true, false, WATER),
                "clause 1, the death and the wet state in one tick before a burn tick doused it");
        assertEquals(REFUSED, Douse.atDeath(true, false, RAIN));
        assertEquals(CLAUSE_1, Douse.atDeath(true, true, DRY), "a scorched body is the listener's whatever killed it");
        assertEquals(REFUSED, Douse.atDeath(true, true, RAIN));
    }

    @Test
    void theThreeLinesInTheirPinnedShapes() {
        UUID id = UUID.fromString("00000000-0000-0000-0000-00000000000a");
        assertEquals("SCORCH doused target=zombie 00000000-0000-0000-0000-00000000000a cause=water tick=101",
                new DouseLine(DouseLine.Event.DOUSED, "zombie", id, Douse.Cause.WATER).format(101));
        assertEquals("SCORCH doused target=zombie 00000000-0000-0000-0000-00000000000a cause=splash tick=102",
                new DouseLine(DouseLine.Event.DOUSED, "zombie", id, Douse.Cause.SPLASH).format(102));
        assertEquals("SCORCH refused target=husk 00000000-0000-0000-0000-00000000000a cause=rain tick=103",
                new DouseLine(DouseLine.Event.REFUSED, "husk", id, Douse.Cause.RAIN).format(103));
        assertEquals("IGNITE refused target=zombie 00000000-0000-0000-0000-00000000000a cause=water tick=104",
                new DouseLine(DouseLine.Event.IGNITE_REFUSED, "zombie", id, Douse.Cause.WATER).format(104));
    }
}
