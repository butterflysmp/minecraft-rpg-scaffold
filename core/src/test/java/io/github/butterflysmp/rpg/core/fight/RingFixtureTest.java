package io.github.butterflysmp.rpg.core.fight;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * F4 + F5, THE DEV FIXTURE'S LOGIC (PLAN-dragon-fight.md section 6), against a fake port. C is at (100, 64, -50), so
 * no reading here can pass by coinciding with the origin. Ana starts at C.
 */
class RingFixtureTest {

    private static final FightSettings S = FightTestSettings.placeholders();
    private static final double CX = 100.0, CY = 64.0, CZ = -50.0;
    private static final UUID ANA = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID BO = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    /** The fake world: who is in the fight and where, and every call the fixture made. */
    static class FakePort implements RingFixture.Port {
        final List<RingFixture.Participant> present = new ArrayList<>();
        final List<String> lines = new ArrayList<>();
        final List<String> told = new ArrayList<>();
        final List<String> effects = new ArrayList<>();
        final List<String> damage = new ArrayList<>();
        int draws;
        int lastDrawPoints;
        List<UUID> lastViewers = List.of();

        void place(UUID id, String name, double x, double z) {
            present.removeIf(p -> p.id().equals(id));
            present.add(new RingFixture.Participant(id, name, x, CY, z));
        }

        @Override public List<RingFixture.Participant> participants() { return List.copyOf(present); }
        List<double[]> lastPoints = List.of();

        @Override public void draw(List<UUID> viewers, List<double[]> points) {
            draws++;
            lastDrawPoints = points.size();
            lastPoints = points;
            lastViewers = viewers;
        }
        @Override public void strikeEffect(UUID player, double x, double y, double z) {
            effects.add(player + " " + x + " " + z);
        }
        @Override public void damage(UUID player, double amount) { damage.add(player + " " + amount); }
        @Override public void log(String line) { lines.add(line); }
        @Override public void tell(String line) { told.add(line); }
        final List<Integer> lit = new ArrayList<>();
        int clears;
        int cues;
        @Override public void lightCrystal(int index) { lit.add(index); }
        @Override public void clearCrystals() { clears++; lit.clear(); }
        @Override public void refusedCue() { cues++; }

        long count(String prefix) { return lines.stream().filter(l -> l.startsWith(prefix)).count(); }
        String last(String prefix) {
            for (int i = lines.size() - 1; i >= 0; i--) if (lines.get(i).startsWith(prefix)) return lines.get(i);
            return null;
        }
    }

    private static RingFixture fixture(FakePort port) {
        return new RingFixture(S, RingPointsTest.look(), CX, CY, CZ, port);
    }

    private static void run(RingFixture f, int ticks) {
        for (int i = 0; i < ticks; i++) f.tick();
    }

    /**
     * THE WALL'S WITNESS (GATE-fight-site.md): each row's first point, as an arc from angle 0, is what was DRAWN on that
     * tick, and two readings 20 ticks apart show rows 0, 2, 4 turning +1.0 block and rows 1, 3, 5 turning -1.0.
     */
    @Test
    void theWallWitnessReadsTheDrawnPointsAndAlternateRowsTurnOppositeWays() {
        FakePort port = new FakePort();
        port.place(ANA, "ana", CX, CZ);
        RingFixture f = new RingFixture(S, RingPointsTest.sixRowWall(), CX, CY, CZ, port);
        assertEquals(List.of(), f.wallStartArcs(), "nothing before the first tick");
        run(f, 20);
        List<Double> first = f.wallStartArcs();
        assertEquals(6, first.size());
        double r = S.borderRadiusBlocks();
        int per = RingPoints.pointsPerHeight(r, RingPointsTest.sixRowWall());
        for (int row = 0; row < 6; row++) {
            double[] drawn = port.lastPoints.get(row * per);
            double a = Math.atan2(drawn[2] - CZ, drawn[0] - CX);
            if (a < 0) a += 2 * Math.PI;
            assertEquals(a * r, first.get(row), 1e-9, "row " + row + " witness = the point drawn at tick 20");
        }
        run(f, 20);
        List<Double> second = f.wallStartArcs();
        double c = 2 * Math.PI * r;
        for (int row = 0; row < 6; row++) {
            double d = (((second.get(row) - first.get(row)) % c) + c) % c;
            if (d > c / 2) d -= c;
            assertEquals(row % 2 == 0 ? 1.0 : -1.0, d, 1e-9, "row " + row + " in 20 ticks");
        }
    }

    /** The arena (failure kills) and the ordinary-world fixture (nobody is killed) tell the participants different things. */
    @Test
    void theFailureMessageSaysWhetherItKills() {
        FakePort ordinary = new FakePort();
        ordinary.place(ANA, "ana", CX, CZ);
        RingFixture a = fixture(ordinary);
        run(a, 5);
        a.land(0.0, 0.0);
        run(a, 900);
        assertTrue(ordinary.told.contains("The fight has failed: FAILED_CLOSED (fixture: nobody is killed)."),
                ordinary.told.toString());

        FakePort arena = new FakePort() {
            @Override public boolean failureKills() { return true; }
        };
        arena.place(ANA, "ana", CX, CZ);
        RingFixture b = fixture(arena);
        run(b, 5);
        b.land(0.0, 0.0);
        run(b, 900);
        assertTrue(arena.told.contains("The fight has failed: FAILED_CLOSED."), arena.told.toString());
    }

    @Test
    void startIsTheBorderAtCInIdleAndRedrawsEveryFourTicks() {
        FakePort port = new FakePort();
        port.place(ANA, "ana", CX, CZ);
        RingFixture f = fixture(port);
        run(f, 3);
        assertEquals(0, port.draws, "no redraw before tick 4");
        f.tick();
        assertEquals(1, port.draws);
        assertEquals(906, port.lastDrawPoints, "302 points at each of 3 heights at radius 48");
        assertEquals(List.of(ANA), port.lastViewers);
        assertEquals("RINGTICK t=4 phase=IDLE centre=0.000 0.000 r=48.000 crystals=0 points=906 viewers=1",
                port.last("RINGTICK"));
        run(f, 4);
        assertEquals(2, port.draws);
        assertEquals(2, port.count("RINGTICK"), "one RINGTICK per redraw");
    }

    @Test
    void theCentreInRingTickIsTheOffsetFromC() {
        FakePort port = new FakePort();
        RingFixture f = fixture(port);
        run(f, 4);
        assertEquals(MageCycle.LandingResult.STARTED, f.land(18.0, 0.0));
        assertEquals("FIGHTPHASE t=4 IDLE->FAST_CLOSE", port.last("FIGHTPHASE"), "on the landing's own tick");
        run(f, 216);
        // Landed at t=4; the slow close starts at 4 + 216 = 220, about M = C + (18, 0).
        assertTrue(port.lines.contains("FIGHTPHASE t=220 FAST_CLOSE->SLOW_CLOSE"), port.lines.toString());
        // At radius 12: ceil(2 pi 12) = 76 points a height, 228 in all.
        assertEquals("RINGTICK t=220 phase=SLOW_CLOSE centre=18.000 0.000 r=12.000 crystals=0 points=228 viewers=0",
                port.last("RINGTICK"));
        assertEquals(2, port.count("FIGHTPHASE"), "one line per change, no repeat on the next tick");
    }

    @Test
    void outsideTheRingAtAStrikeTickIsStruckThroughTheCoreRingAndInsideResets() {
        FakePort port = new FakePort();
        port.place(ANA, "ana", CX + 60.0, CZ); // outside the 48 border
        RingFixture f = fixture(port);
        run(f, 40);
        assertEquals("STRIKE t=40 player=ana k=1 damage=20.000 outside", port.last("STRIKE"));
        assertEquals(List.of(ANA + " 20.0"), port.damage);
        assertEquals(List.of(ANA + " " + (CX + 60.0) + " " + CZ), port.effects, "the bolt lands where he stands");
        run(f, 40);
        assertEquals("STRIKE t=80 player=ana k=2 damage=40.000 outside", port.last("STRIKE"));
        run(f, 40);
        assertEquals("STRIKE t=120 player=ana k=3 damage=80.000 outside", port.last("STRIKE"));
        port.place(ANA, "ana", CX + 47.0, CZ); // back inside
        run(f, 40);
        assertEquals("STRIKE t=160 player=ana k=0 damage=0.000 inside", port.last("STRIKE"));
        assertEquals(3, port.damage.size(), "inside: no damage call");
        assertEquals(3, port.effects.size(), "and no bolt");
        port.place(ANA, "ana", CX + 60.0, CZ);
        run(f, 40);
        assertEquals("STRIKE t=200 player=ana k=1 damage=20.000 outside", port.last("STRIKE"), "reset to 20");
    }

    @Test
    void insideIsReadFromTheMagesRingNotTheBorder() {
        FakePort port = new FakePort();
        port.place(ANA, "ana", CX + 15.0, CZ); // inside the border, outside a 12-block ring about C
        RingFixture f = fixture(port);
        f.land(0.0, 0.0);
        run(f, 160);
        // Landed at t=0. At t=120 the fast close is at 120/144, radius 48 - 30 = 18: he is inside.
        assertEquals("STRIKE t=120 player=ana k=0 damage=0.000 inside", port.lines.stream()
                .filter(l -> l.startsWith("STRIKE t=120")).findFirst().orElse(null));
        // The slow close from t=144; at t=160 the radius is 12 - 0.4 x 0.8 = 11.68, and he is 15 from M: outside.
        assertEquals("STRIKE t=160 player=ana k=1 damage=20.000 outside", port.last("STRIKE"));
    }

    @Test
    void closedStrikesGoOnAndPoweredStrikesStop() {
        FakePort closed = new FakePort();
        closed.place(ANA, "ana", CX, CZ);
        RingFixture c = fixture(closed);
        c.land(0.0, 0.0);
        run(c, 144 + 600);
        assertEquals("FIGHT FAILED_CLOSED t=744", closed.last("FIGHT FAILED"));
        assertEquals(1, closed.told.size(), "said once in chat");
        assertTrue(closed.told.get(0).contains("FAILED_CLOSED"));
        run(c, 40);
        assertEquals("STRIKE t=760 player=ana k=1 damage=20.000 outside", closed.last("STRIKE"),
                "every point is outside a closed ring, even C");
        run(c, 200);
        assertEquals(1, closed.count("FIGHT FAILED"), "the failure is logged once");

        FakePort powered = new FakePort();
        powered.place(ANA, "ana", CX, CZ);
        RingFixture p = fixture(powered);
        p.land(0.0, 0.0);
        run(p, 144);
        for (int k = 0; k < 6; k++) {
            run(p, 100);
            p.kill(ANA, CX, CZ);
        }
        run(p, 800 - 600);
        assertEquals("FIGHT FAILED_POWERED t=944", powered.last("FIGHT FAILED"));
        long strikesBefore = powered.count("STRIKE");
        run(p, 200);
        assertEquals(strikesBefore, powered.count("STRIKE"), "no strike of any kind once powered");
    }

    @Test
    void aKillIsLoggedWithTheRadiusBeforeAndAfter() {
        FakePort port = new FakePort();
        RingFixture f = fixture(port);
        f.land(0.0, 0.0);
        run(f, 244); // slow close from 144; at 244 the radius is 12 - 2 = 10
        assertTrue(f.kill(ANA, CX + 1.0, CZ));
        assertEquals("FIGHTKILL t=244 killer=" + ANA + " counted=true r_before=10.000 r_after=11.000",
                port.last("FIGHTKILL"));
        assertFalse(f.kill(ANA, CX + 30.0, CZ));
        assertEquals("FIGHTKILL t=244 killer=" + ANA + " counted=false r_before=11.000 r_after=11.000",
                port.last("FIGHTKILL"));
    }

    @Test
    void aPlayerWhoLeavesIsDroppedAndStartsAgainAt20() {
        FakePort port = new FakePort();
        port.place(ANA, "ana", CX + 60.0, CZ);
        port.place(BO, "bo", CX, CZ);
        RingFixture f = fixture(port);
        run(f, 80);
        assertEquals("STRIKE t=80 player=ana k=2 damage=40.000 outside", port.last("STRIKE t=80 player=ana"));
        port.present.removeIf(p -> p.id().equals(ANA));
        run(f, 40);
        assertEquals(List.of(BO), port.lastViewers, "only bo is drawn to");
        port.place(ANA, "ana", CX + 60.0, CZ);
        run(f, 40);
        assertEquals("STRIKE t=160 player=ana k=1 damage=20.000 outside", port.last("STRIKE t=160 player=ana"),
                "his count was dropped while he was gone");
    }

    @Test
    void stopEndsEverything() {
        FakePort port = new FakePort();
        port.place(ANA, "ana", CX + 60.0, CZ);
        RingFixture f = fixture(port);
        run(f, 40);
        int draws = port.draws;
        int hits = port.damage.size();
        int lines = port.lines.size();
        f.stop();
        assertTrue(f.stopped());
        assertEquals("FIGHT stop t=40", port.last("FIGHT stop"));
        run(f, 200);
        assertEquals(draws, port.draws, "no redraw after stop");
        assertEquals(hits, port.damage.size(), "no strike after stop");
        assertEquals(lines + 1, port.lines.size(), "nothing logged after the stop line");
        assertEquals(MageCycle.LandingResult.IGNORED_ENDED, f.land(0.0, 0.0), "and no input is taken");
    }

    @Test
    void aDeathLogsItsPhaseOnItsOwnTickAndTheReturnEndsInIdle() {
        FakePort port = new FakePort();
        RingFixture f = fixture(port);
        f.land(0.0, 0.0);
        run(f, 244);                 // slow close from 144; radius 10 at 244
        f.mageDie();
        assertEquals("FIGHTPHASE t=244 SLOW_CLOSE->RETURNING", port.last("FIGHTPHASE"));
        // Back up to 12 in (12 - 10) / 0.4 s = 100 ticks, then the fast close reversed, 144: home at 244 + 244 = 488.
        run(f, 243);
        assertEquals("FIGHTPHASE t=244 SLOW_CLOSE->RETURNING", port.last("FIGHTPHASE"));
        f.tick();
        assertEquals("FIGHTPHASE t=488 RETURNING->IDLE", port.last("FIGHTPHASE"));
    }

    @Test
    void crystalsLightOneEveryIntervalAndGoOutOnTheMagesDeathOnThatTick() {
        FakePort port = new FakePort();
        RingFixture f = fixture(port);
        f.land(0.0, 0.0);
        run(f, 144 + 79);
        assertEquals(List.of(), port.lit, "none before slow close + 80");
        f.tick();
        assertEquals(List.of(1), port.lit);
        assertEquals("CRYSTAL t=224 lit index=1", port.last("CRYSTAL"));
        run(f, 160);
        assertEquals(List.of(1, 2, 3), port.lit);
        f.mageDie(); // t = 384
        assertEquals(1, port.clears, "every crystal removed");
        assertEquals(List.of("CRYSTAL t=384 removed index=1", "CRYSTAL t=384 removed index=2",
                "CRYSTAL t=384 removed index=3"), port.lines.stream().filter(l -> l.contains("removed")).toList());
        run(f, 50);
        assertEquals(1, port.clears, "and nothing more while it returns");
    }

    @Test
    void poweredCrystalsStayUntilStop() {
        FakePort port = new FakePort();
        port.place(ANA, "ana", CX, CZ);
        RingFixture f = fixture(port);
        f.land(0.0, 0.0);
        run(f, 144);
        for (int k = 0; k < 6; k++) {
            run(f, 100);
            f.kill(ANA, CX, CZ);
        }
        run(f, 200);
        assertEquals("FIGHT FAILED_POWERED t=944", port.last("FIGHT FAILED"));
        assertEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), port.lit);
        run(f, 100);
        assertEquals(0, port.clears, "POWERED: they stay");
    }

    @Test
    void aRefusedHitIsLoggedWithTheTickThePhaseAndTheAmountAndCued() {
        FakePort port = new FakePort();
        RingFixture f = fixture(port);
        f.land(0.0, 0.0);
        run(f, 30);
        assertFalse(f.mageDamageable(), "fast close");
        f.refused(42.5, "ability");
        assertEquals("MAGEGATE t=30 phase=FAST_CLOSE amount=42.500 source=ability refused", port.last("MAGEGATE"));
        assertEquals(1, port.cues);
        run(f, 114);
        assertTrue(f.mageDamageable(), "the slow close's first tick");
    }

    @Test
    void theStrikeLimitStopsTheFixtureAfterTheLimitthStrike() {
        FakePort port = new FakePort();
        port.place(ANA, "ana", CX + 60.0, CZ);
        RingFixture f = fixture(port);
        f.strikeLimit(3);
        run(f, 119);
        assertFalse(f.stopped(), "two strikes: still running");
        f.tick(); // t = 120, his third
        assertEquals("STRIKE t=120 player=ana k=3 damage=80.000 outside", port.last("STRIKE"));
        assertEquals("FIGHT strike-limit t=120 player=ana k=3", port.last("FIGHT strike-limit"));
        assertTrue(f.stopped(), "stopped after the third");
        assertEquals(3, port.damage.size(), "and no fourth is ever dealt");
    }

    @Test
    void theDeathLineCarriesTheTicksSinceTheSlowCloseStarted() {
        FakePort port = new FakePort();
        RingFixture f = fixture(port);
        f.land(0.0, 0.0);
        run(f, 200); // slow close from 144
        f.mageDie();
        assertEquals("FIGHT die t=200 phase=RETURNING slow_close_started=t=144 ticks_in_slow_close=56",
                port.last("FIGHT die"));
    }

    @Test
    void aLandingIsLoggedWithItsResult() {
        FakePort port = new FakePort();
        RingFixture f = fixture(port);
        run(f, 7);
        assertEquals(MageCycle.LandingResult.REFUSED_TOO_FAR, f.land(36.01, 0.0));
        assertEquals("FIGHT land t=7 dx=36.010 dz=0.000 result=REFUSED_TOO_FAR", port.last("FIGHT land"));
        run(f, 1);
        assertEquals("RINGTICK t=8 phase=IDLE centre=0.000 0.000 r=48.000 crystals=0 points=906 viewers=0",
                port.last("RINGTICK"), "the ring is unchanged");
        assertEquals(0, port.count("FIGHTPHASE"), "and no phase changed");
    }
}
