package io.github.butterflysmp.rpg.core.mob;

import io.github.butterflysmp.rpg.core.mob.MobHostility.PvpPath;
import io.github.butterflysmp.rpg.core.mob.MobHostility.Side;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.github.butterflysmp.rpg.core.mob.MobHostility.harms;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** THE ONE TABLE, cell by cell (the seat's P1-P4, 2026-11-24). */
class MobHostilityTableTest {

    @Test
    void theEightPlainCells() {
        for (PvpPath path : new PvpPath[] {null, PvpPath.RAY, PvpPath.SHOCKWAVE}) {   // the plain cells ignore the path
            assertTrue(harms(Side.PLAYER, Side.MOB, false, path), "PLAYER -> MOB yes");
            assertFalse(harms(Side.PLAYER, Side.SUMMON, false, path), "PLAYER -> SUMMON no");
            assertTrue(harms(Side.MOB, Side.PLAYER, false, path), "MOB -> PLAYER yes");
            assertFalse(harms(Side.MOB, Side.PLAYER, true, path), "MOB -> an exempt PLAYER no");
            assertFalse(harms(Side.MOB, Side.MOB, false, path), "MOB -> MOB no");
            assertTrue(harms(Side.MOB, Side.SUMMON, false, path), "MOB -> SUMMON yes (Ben's Phage ruling)");
            assertFalse(harms(Side.SUMMON, Side.PLAYER, false, path), "SUMMON -> PLAYER no");
            assertTrue(harms(Side.SUMMON, Side.MOB, false, path), "SUMMON -> MOB yes");
            assertFalse(harms(Side.SUMMON, Side.SUMMON, false, path), "SUMMON -> SUMMON no");
        }
    }

    /** PLAYER -> PLAYER, per path, TODAY'S BEHAVIOUR: every value, pinned, so a change to the enum is a red row here too. */
    @Test
    void playerToPlayerIsPerPathAsToday() {
        Map<PvpPath, Boolean> today = Map.ofEntries(
                Map.entry(PvpPath.SWING, true), Map.entry(PvpPath.RAY, true), Map.entry(PvpPath.PROJECTILE, true),
                Map.entry(PvpPath.CONE, true), Map.entry(PvpPath.BURST, true), Map.entry(PvpPath.AREA, true),
                Map.entry(PvpPath.DASH_LINE, false), Map.entry(PvpPath.TRAVEL_END_BURST, false),
                Map.entry(PvpPath.SHOCKWAVE, false), Map.entry(PvpPath.CRYSTAL_SHOT, false),
                Map.entry(PvpPath.IGNITE_SPLASH, false), Map.entry(PvpPath.HOMING_PICK, false),
                Map.entry(PvpPath.IMPLOSION, true), Map.entry(PvpPath.STATUS_TICK, true),
                // F4 (the seat, 2026-11-26, the Blast Fungus; Ben: "Correct", mobs only): SPARED, as TRAVEL_END_BURST.
                Map.entry(PvpPath.FUSED_BURST, false),
                // Z4 (the seat, 2026-11-27, Smolder; Ben: "No players for now"): SPARED.
                Map.entry(PvpPath.PULSE, false));
        assertEquals(PvpPath.values().length, today.size(), "every path has a pinned verdict");
        for (PvpPath p : PvpPath.values()) {
            assertEquals(today.get(p), harms(Side.PLAYER, Side.PLAYER, false, p), p.name());
        }
        assertThrows(IllegalArgumentException.class, () -> harms(Side.PLAYER, Side.PLAYER, false, null),
                "a PvP question with no path has no answer");
    }

    /** The guard's three cells, and that it never refuses a cell without a summon (H3: PvP untouched). */
    @Test
    void theGuardRefusesExactlyTheThreeSummonNoCells() {
        for (Side a : Side.values()) {
            for (Side v : Side.values()) {
                boolean expected = (a == Side.PLAYER && v == Side.SUMMON) || (a == Side.SUMMON && v == Side.PLAYER)
                        || (a == Side.SUMMON && v == Side.SUMMON);
                assertEquals(expected, MobHostility.refusedSummonCell(a, v), a + " -> " + v);
            }
        }
    }

    @Test
    void hostileToIsTheMobRow() {
        for (Side v : Side.values()) {
            for (boolean exempt : new boolean[] {false, true}) {
                assertEquals(harms(Side.MOB, v, exempt, null), MobHostility.hostileTo(v, exempt), v + " " + exempt);
            }
        }
        assertTrue(MobHostility.hostileTo(Side.SUMMON, false), "a mob's blast hurts a summon");
    }

    @Test
    void onlyAMobHitCountsForEnchantsAsBefore() {
        assertTrue(MobHostility.countsAsAMobHit(Side.MOB));
        assertFalse(MobHostility.countsAsAMobHit(Side.PLAYER));
        assertFalse(MobHostility.countsAsAMobHit(Side.SUMMON));
    }
}
