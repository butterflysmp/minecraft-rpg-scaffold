package io.github.butterflysmp.rpg.core.summon;

import org.junit.jupiter.api.Test;

import java.util.OptionalInt;
import java.util.Set;

import static io.github.butterflysmp.rpg.core.summon.SummonCap.Verdict.NEW;
import static io.github.butterflysmp.rpg.core.summon.SummonCap.Verdict.REFUSED_FULL;
import static io.github.butterflysmp.rpg.core.summon.SummonCap.Verdict.REFUSED_UNKNOWN;
import static io.github.butterflysmp.rpg.core.summon.SummonCap.Verdict.REPLACE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * D6 (Ben, the seat; 2026-11-24): one summon per player per summon WEAPON ID; a cast while that weapon's summon lives
 * REPLACES it; a cast is REFUSED when his OTHER weapons' summons plus one exceed max_summons, or the stat is empty.
 * A refusal costs no mana and starts no cooldown.
 */
class SummonCapTest {

    private static OptionalInt max(int n) {
        return OptionalInt.of(n);
    }

    @Test
    void theCapTable() {
        // live = the weapon ids whose summon is alive; the cast is with "whistle".
        assertEquals(NEW, SummonCap.decide("whistle", Set.of(), max(1)), "none alive, room for one");
        assertEquals(REPLACE, SummonCap.decide("whistle", Set.of("whistle"), max(1)), "its own lives: replaced, at the cap");
        assertEquals(REFUSED_FULL, SummonCap.decide("whistle", Set.of("horn"), max(1)), "another weapon's fills the cap");
        assertEquals(NEW, SummonCap.decide("whistle", Set.of("horn"), max(2)), "another's, and room for a second");
        assertEquals(REPLACE, SummonCap.decide("whistle", Set.of("horn", "whistle"), max(2)), "both live, max 2: replace");
        assertEquals(REFUSED_FULL, SummonCap.decide("whistle", Set.of("horn", "whistle"), max(1)),
                "max lowered below what lives: even a replace is refused (others 1 + 1 > 1)");
        assertEquals(REFUSED_FULL, SummonCap.decide("whistle", Set.of(), max(0)), "max_summons 0: the cap row");
        assertEquals(REFUSED_FULL, SummonCap.decide("whistle", Set.of("whistle"), max(0)), "0, and its own alive");
        assertEquals(REFUSED_UNKNOWN, SummonCap.decide("whistle", Set.of(), OptionalInt.empty()), "the stat is empty");
    }

    @Test
    void onlyAnAllowedCastCharges() {
        assertTrue(NEW.charges());
        assertTrue(REPLACE.charges(), "a replace costs the full cost (Ben: Correct)");
        assertFalse(REFUSED_FULL.charges(), "no mana, no cooldown");
        assertFalse(REFUSED_UNKNOWN.charges());
    }
}
