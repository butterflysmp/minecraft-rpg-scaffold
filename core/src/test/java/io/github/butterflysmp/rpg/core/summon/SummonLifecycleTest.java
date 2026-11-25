package io.github.butterflysmp.rpg.core.summon;

import io.github.butterflysmp.rpg.core.Vec3;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 3b's pure core (the seat, 2026-11-24): the store, scaling (D3), the removal causes (D7), the backstop, the traces. */
class SummonLifecycleTest {

    private static final UUID HIM = new UUID(0, 1), OTHER = new UUID(0, 2);
    private static final UUID B1 = new UUID(1, 1), B2 = new UUID(1, 2), B3 = new UUID(1, 3);
    private static final SummonDefinition WOLF = new SummonDefinition("test_wolf", "wolf", "{owner}'s Wolf", 100, 20,
            8, 30, 32, 48, 20, 100);

    @Test
    void scalingIsAuthoredTimesScoreOverHundredFixedAtTheCast() {
        SummonStore.Summon s = SummonStore.Summon.cast(HIM, "whistle", B1, WOLF, 150);
        assertEquals(150.0, s.health(), 1e-9, "100 x 150 / 100");
        assertEquals(30.0, s.damage(), 1e-9, "20 x 150 / 100");
        assertEquals(150, s.score(), "the score it was cast at, kept");
        SummonStore.Summon high = SummonStore.Summon.cast(HIM, "whistle", B1, WOLF, 250);
        assertEquals(250.0, high.health(), 1e-9);
        assertEquals(50.0, high.damage(), 1e-9);
        // READ, GearScore: the score is clamped to its legal band (MIN 100) before it scales; the summon keeps the one used.
        SummonStore.Summon low = SummonStore.Summon.cast(HIM, "whistle", B1, WOLF, 75);
        assertEquals(100.0, low.health(), 1e-9, "75 is below the band: 100 x 100 / 100");
        assertEquals(100, low.score(), "the score it was scaled at, not the one asked");
    }

    @Test
    void theStoreCountsByWeaponAndAReplaceHandsBackTheOldBody() {
        SummonStore store = new SummonStore();
        assertEquals(Optional.empty(), store.place(SummonStore.Summon.cast(HIM, "whistle", B1, WOLF, 100)));
        assertEquals(Optional.empty(), store.place(SummonStore.Summon.cast(HIM, "horn", B2, WOLF, 100)));
        assertEquals(Optional.empty(), store.place(SummonStore.Summon.cast(OTHER, "whistle", B3, WOLF, 100)));
        assertEquals(Set.of("whistle", "horn"), store.liveWeapons(HIM), "his, by weapon id");
        assertTrue(store.owns(B1) && store.owns(B2) && store.owns(B3));
        UUID b4 = new UUID(1, 4);
        assertEquals(B1, store.place(SummonStore.Summon.cast(HIM, "whistle", b4, WOLF, 100)).orElseThrow().body(),
                "a recast of the same weapon hands back the body it replaces");
        assertFalse(store.owns(B1));
        assertEquals(Optional.of(HIM), store.remove(b4).map(SummonStore.Summon::owner));
        assertEquals(List.of(B2), store.removeAllOf(HIM).stream().map(SummonStore.Summon::body).toList());
        assertEquals(Set.of(), store.liveWeapons(HIM));
        assertEquals(List.of(B3), store.removeAll().stream().map(SummonStore.Summon::body).toList(), "server stop");
        assertFalse(store.owns(B3));
    }

    @Test
    void eachRemovalCauseIsOneValueWithItsLogToken() {
        assertEquals(List.of("death", "owner-death", "quit", "world", "replace", "stop", "clear", "orphan", "backstop",
                "void", "killed"), Arrays.stream(SummonRemoval.values()).map(SummonRemoval::token).toList());
    }

    @Test
    void theBackstopDecision() {
        assertEquals(SummonBackstop.Verdict.REMOVE, SummonBackstop.decide(false, true, 0), "a body the store does not own");
        assertEquals(SummonBackstop.Verdict.REMOVE, SummonBackstop.decide(true, false, 0),
                "its owner is not online in its world");
        assertEquals(SummonBackstop.Verdict.RESTRIP, SummonBackstop.decide(true, true, 1),
                "D1(a)'s UNVERIFIED, guarded: a goal not ours came back");
        assertEquals(SummonBackstop.Verdict.KEEP, SummonBackstop.decide(true, true, 0));
        assertEquals(SummonBackstop.Verdict.REMOVE, SummonBackstop.decide(false, true, 3), "removal before a restrip");
        assertEquals(20, SummonBackstop.PERIOD_TICKS, "the seat's 20 ticks");
    }

    @Test
    void theTraceLinesCarryOwnerWeaponBodyAndPosition() {
        SummonStore.Summon s = SummonStore.Summon.cast(HIM, "whistle", B1, WOLF, 150);
        Vec3 at = new Vec3(1.5, 64, -2.25);
        String where = "owner=" + HIM + " weapon=whistle body=" + B1 + " at=1.50 64.00 -2.25";
        assertEquals("SUMMON spawn " + where + " summon=test_wolf score=150 health=150.000 damage=30.000",
                SummonTrace.spawn(s, at));
        assertEquals("SUMMON target " + where + " target=" + B2, SummonTrace.target(s, at, B2));
        assertEquals("SUMMON hit " + where + " victim=" + B3
                        + " authored=20.000 score=150 sent=30.000 crit=none class=0 level=0 element=nature",
                SummonTrace.hit(s, at, B3, WOLF.damage(), "nature"));
        assertEquals("SUMMON teleport " + where + " from=9.00 64.00 0.00", SummonTrace.teleport(s, at, new Vec3(9, 64, 0)));
        assertEquals("SUMMON removed " + where + " cause=owner-death", SummonTrace.removed(s, at, SummonRemoval.OWNER_DEATH));
        assertEquals("SUMMON goals restored " + where + " foreign=2", SummonTrace.goalsRestored(s, at, 2));
    }
}
