package io.github.butterflysmp.rpg.core.accessory;

import io.github.butterflysmp.rpg.core.Vec3;
import io.github.butterflysmp.rpg.core.accessory.HealingPulses.Candidate;
import io.github.butterflysmp.rpg.core.accessory.HealingPulses.Decision;
import io.github.butterflysmp.rpg.core.accessory.HealingPulses.Heal;
import io.github.butterflysmp.rpg.core.accessory.HealingPulses.Kind;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * H5 and H7 (the seat, 2026-11-26): one pulse of the Radiant Pulse as a TABLE. The wearer stands at the origin of "world";
 * every row is one entity near him, and the row says whether it is a candidate and what it is healed. The pulse is the
 * shipped one (1 per level, 80 ticks, 8 blocks, players).
 */
class HealingPulsesTest {

    private static final AccessoryBehaviour.HealingPulse PULSE = AccessoryFixtures.radiantPulse();
    private static final Vec3 HERE = new Vec3(0, 64, 0);

    private static Candidate c(String name, Kind kind, String world, double dx, boolean alive, boolean spectator,
                               double health, double max) {
        return new Candidate(UUID.nameUUIDFromBytes(name.getBytes()), name, kind, world,
                new Vec3(dx, 64, 0), alive, spectator, health, max);
    }

    private static Candidate hurtPlayer(String name, double dx) {
        return c(name, Kind.PLAYER, "world", dx, true, false, 80, 100);
    }

    /** The table: row, entity, expected candidate?, expected amount at L=7 (or -1 for not healed). */
    @Test
    void theTable_atLevelSeven() {
        record Row(String why, Candidate c, boolean candidate, double amount) {}
        List<Row> rows = List.of(
                new Row("the wearer himself, hurt by 20", c("ben", Kind.PLAYER, "world", 0, true, false, 80, 100), true, 7),
                new Row("a player 8.0 away exactly (inclusive)", hurtPlayer("edge", 8.0), true, 7),
                new Row("a player 8.001 away", hurtPlayer("far", 8.001), false, -1),
                new Row("a player missing 3 (less than L): healed by 3, to max", c("cap", Kind.PLAYER, "world", 2, true, false, 97, 100), true, 3),
                new Row("a player missing exactly L", c("exact", Kind.PLAYER, "world", 2, true, false, 93, 100), true, 7),
                new Row("a player at full health: a candidate, skipped", c("full", Kind.PLAYER, "world", 2, true, false, 100, 100), true, -1),
                new Row("a dead player", c("dead", Kind.PLAYER, "world", 2, false, false, 0, 100), false, -1),
                new Row("a player at 0 custom health, not yet dead", c("zero", Kind.PLAYER, "world", 2, true, false, 0, 100), false, -1),
                new Row("a spectator", c("spec", Kind.PLAYER, "world", 2, true, true, 50, 100), false, -1),
                new Row("a player in another world, same coordinates", c("nether", Kind.PLAYER, "world_nether", 0, true, false, 50, 100), false, -1),
                new Row("a hurt mob (H7)", c("zombie", Kind.MOB, "world", 2, true, false, 10, 100), false, -1),
                new Row("a hurt summon (H7, until parties)", c("wolf", Kind.SUMMON, "world", 2, true, false, 10, 100), false, -1),
                new Row("an armour stand (H7)", c("stand", Kind.ARMOR_STAND, "world", 2, true, false, 10, 100), false, -1),
                new Row("anything else", c("other", Kind.OTHER, "world", 2, true, false, 10, 100), false, -1));

        for (Row r : rows) {
            assertEquals(r.candidate(), HealingPulses.isCandidate(PULSE, "world", HERE, r.c()), r.why());
            Decision d = HealingPulses.decide(PULSE, 7, "world", HERE, List.of(r.c()));
            assertEquals(r.candidate() ? 1 : 0, d.candidates(), r.why());
            if (r.amount() < 0) {
                assertEquals(0, d.healed(), r.why());
            } else {
                Heal h = d.heals().get(0);
                assertEquals(r.amount(), h.amount(), r.why());
                assertEquals(r.c().health(), h.before(), r.why());
                assertEquals(r.c().health() + r.amount(), h.after(), r.why());
                assertTrue(h.after() <= r.c().max(), "never past max: " + r.why());
            }
        }
        // Mutations: `<=` -> `<` on the radius reddens "8.0 away exactly"; drop the kind check reddens the mob, summon
        // and stand rows; drop the world check reddens "another world"; drop the min() reddens "missing 3".
    }

    /** L is the WEARER'S level, applied to every target alike; the amount is L x heal_per_level. */
    @Test
    void theAmountIsTheWearersLevel_forEveryTarget() {
        for (int level : new int[] {1, 2, 3, 5, 50}) {
            Decision d = HealingPulses.decide(PULSE, level, "world", HERE,
                    List.of(c("ben", Kind.PLAYER, "world", 0, true, false, 40, 105),
                            c("ally", Kind.PLAYER, "world", 3, true, false, 10, 100)));   // both miss more than 50
            assertEquals(2, d.healed());
            for (Heal h : d.heals()) assertEquals(level, h.amount(), "L=" + level + " on " + h.name());
        }
        AccessoryBehaviour.HealingPulse half = new AccessoryBehaviour.HealingPulse("P", 0.5, 80, 8, AccessoryBehaviour.Targets.PLAYERS,
                null, 0, null, null);
        assertEquals(25.0, HealingPulses.decide(half, 50, "world", HERE, List.of(c("x", Kind.PLAYER, "world", 1, true, false, 10, 100))).heals().get(0).amount(),
                "heal_per_level scales it: Ben's 'A level 50 player would heal 50' is 50 x 1");
    }

    /** Nobody hurt: the pulse decides no heal at all, and still counts its candidates for the trace (H10). */
    @Test
    void nobodyHurt_noHeal_candidatesStillCounted() {
        Decision d = HealingPulses.decide(PULSE, 9, "world", HERE, List.of(
                c("ben", Kind.PLAYER, "world", 0, true, false, 105, 105),
                c("ally", Kind.PLAYER, "world", 4, true, false, 100, 100),
                c("zombie", Kind.MOB, "world", 1, true, false, 3, 100)));
        assertEquals(2, d.candidates());
        assertEquals(0, d.healed());
        assertTrue(HealingPulses.decide(PULSE, 9, "world", HERE, List.of()).heals().isEmpty());
    }

    @Test
    void oneEntityFoundTwiceIsOneCandidate() {
        Candidate ally = hurtPlayer("ally", 2);
        Decision d = HealingPulses.decide(PULSE, 4, "world", HERE, List.of(ally, ally));
        assertEquals(1, d.candidates());
        assertEquals(1, d.healed());
    }

    @Test
    void aLevelBelowOneIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> HealingPulses.decide(PULSE, 0, "world", HERE, List.of()));
    }
}
