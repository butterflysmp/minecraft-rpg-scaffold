package io.github.butterflysmp.rpg.core.mob;

import io.github.butterflysmp.rpg.core.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The Phage's growth (PLAN-dragon-mobs.md, the plan's step R4). Ben: a small Phage grows when Void mobs die nearby
 * ("Void as in Void element", "Start with 4, let's try 6 block radius. Any deaths."); five stages; G1, one death is
 * one stage; G2, one death grows EVERY Phage in range. The rule decides from two creatures' FEET positions: a
 * sphere of the radius, inclusive, same world.
 */
class MobGrowthTest {

    private static MobDefinition phage(MobGrowth growth) {
        return new MobDefinition("phage", "creeper", "Phage", 80, MobBody.VANILLA, "void",
                new MobStages(5, new StageBody(1.0, 1.5, 80), new StageBody(3.0, 0.5, 160)),
                new MobSplit(2, 1, 0.5), growth);
    }

    private static final MobGrowth GROWTH = new MobGrowth("void", 6.0, 1);
    private static final MobDefinition PHAGE = phage(GROWTH);
    private static final Vec3 ORIGIN = new Vec3(0, 64, 0);

    private static MobGrowth.Death voidDeathAt(Vec3 at) {
        return new MobGrowth.Death(UUID.randomUUID(), "void", "world", at);
    }

    private static MobGrowth.Candidate phageAt(int stage, Vec3 at) {
        return new MobGrowth.Candidate(UUID.randomUUID(), PHAGE, stage, true, "world", at);
    }

    @Test
    void exactlySixBlocksAwayGrowsAndJustBeyondDoesNot() {
        assertEquals(2, MobGrowth.stageAfter(phageAt(1, new Vec3(6.0, 64, 0)), voidDeathAt(ORIGIN)));
        assertEquals(1, MobGrowth.stageAfter(phageAt(1, new Vec3(6.001, 64, 0)), voidDeathAt(ORIGIN)));
    }

    /** A SPHERE, not a cube: a corner of the 6-block cube is 10.39 blocks away and does not count. */
    @Test
    void theRadiusIsASphereNotACube() {
        assertEquals(1, MobGrowth.stageAfter(phageAt(1, new Vec3(6, 70, 6)), voidDeathAt(ORIGIN)));
        assertEquals(2, MobGrowth.stageAfter(phageAt(1, new Vec3(3, 67, 3)), voidDeathAt(ORIGIN)), "5.20 away");
    }

    @Test
    void anotherWorldDoesNotGrow() {
        MobGrowth.Candidate inNether = new MobGrowth.Candidate(UUID.randomUUID(), PHAGE, 1, true, "world_nether", ORIGIN);
        assertEquals(1, MobGrowth.stageAfter(inNether, voidDeathAt(ORIGIN)));
    }

    @Test
    void onlyTheGrowthElementGrows() {
        MobGrowth.Candidate p = phageAt(1, new Vec3(1, 64, 0));
        assertEquals(2, MobGrowth.stageAfter(p, voidDeathAt(ORIGIN)));
        assertEquals(1, MobGrowth.stageAfter(p, new MobGrowth.Death(UUID.randomUUID(), null, "world", ORIGIN)), "no element");
        assertEquals(1, MobGrowth.stageAfter(p, new MobGrowth.Death(UUID.randomUUID(), "fire", "world", ORIGIN)), "another");
    }

    @Test
    void stageFourGoesToFiveAndStageFiveStays() {
        assertEquals(5, MobGrowth.stageAfter(phageAt(4, new Vec3(1, 64, 0)), voidDeathAt(ORIGIN)));
        assertEquals(5, MobGrowth.stageAfter(phageAt(5, new Vec3(1, 64, 0)), voidDeathAt(ORIGIN)));
    }

    @Test
    void aDeadOrDyingPhageDoesNotGrow() {
        MobGrowth.Candidate dying = new MobGrowth.Candidate(UUID.randomUUID(), PHAGE, 1, false, "world", new Vec3(1, 64, 0));
        assertEquals(1, MobGrowth.stageAfter(dying, voidDeathAt(ORIGIN)));
    }

    @Test
    void aMobWithNoGrowthBlockNeverGrows() {
        MobGrowth.Candidate p = new MobGrowth.Candidate(UUID.randomUUID(), phage(null), 1, true, "world", new Vec3(1, 64, 0));
        assertEquals(1, MobGrowth.stageAfter(p, voidDeathAt(ORIGIN)));
        assertEquals(1, MobGrowth.stageAfter(new MobGrowth.Candidate(UUID.randomUUID(), null, 1, true, "world",
                new Vec3(1, 64, 0)), voidDeathAt(ORIGIN)), "a vanilla mob");
    }

    /** G2: EVERY Phage in range grows, not only the nearest; the far one and the stage 5 do not move. */
    @Test
    void oneDeathGrowsEveryPhageInRange() {
        MobGrowth.Candidate near = phageAt(1, new Vec3(1, 64, 0));
        MobGrowth.Candidate further = phageAt(2, new Vec3(0, 64, 5));
        MobGrowth.Candidate outside = phageAt(1, new Vec3(7, 64, 0));
        MobGrowth.Candidate big = phageAt(5, new Vec3(0, 64, 1));
        List<MobGrowth.Grow> grows = MobGrowth.resolve(voidDeathAt(ORIGIN), false, List.of(near, further, outside, big));
        assertEquals(List.of(new MobGrowth.Grow(near.id(), 1, 2, 1.0), new MobGrowth.Grow(further.id(), 2, 3, 5.0)), grows);
    }

    /** The dying creature never grows itself, even if it is a Phage in its own radius. */
    @Test
    void theDyingCreatureNeverGrowsItself() {
        MobGrowth.Candidate self = phageAt(2, ORIGIN);
        MobGrowth.Death itsDeath = new MobGrowth.Death(self.id(), "void", "world", ORIGIN);
        assertEquals(List.of(), MobGrowth.resolve(itsDeath, false, List.of(self)));
    }

    /** THE ONCE-ONLY GUARD: a death already counted (the event delivered twice) grows nothing. */
    @Test
    void aDeathAlreadyCountedGrowsNothing() {
        MobGrowth.Candidate p = phageAt(1, new Vec3(1, 64, 0));
        MobGrowth.Death death = voidDeathAt(ORIGIN);
        assertEquals(List.of(new MobGrowth.Grow(p.id(), 1, 2, 1.0)), MobGrowth.resolve(death, false, List.of(p)));
        assertEquals(List.of(), MobGrowth.resolve(death, true, List.of(p)));
    }

    /** The radius, the element and the stages per death come from CONTENT. */
    @Test
    void radiusElementAndStagesPerDeathComeFromContent() {
        MobDefinition wide = phage(new MobGrowth("fire", 10.0, 2));
        MobGrowth.Candidate p = new MobGrowth.Candidate(UUID.randomUUID(), wide, 1, true, "world", new Vec3(9, 64, 0));
        assertEquals(3, MobGrowth.stageAfter(p, new MobGrowth.Death(UUID.randomUUID(), "fire", "world", ORIGIN)));
        assertEquals(1, MobGrowth.stageAfter(p, voidDeathAt(ORIGIN)), "fire grows it, void does not");
        MobGrowth.Candidate atFour = new MobGrowth.Candidate(UUID.randomUUID(), wide, 4, true, "world", ORIGIN);
        assertEquals(5, MobGrowth.stageAfter(atFour, new MobGrowth.Death(UUID.randomUUID(), "fire", "world", ORIGIN)),
                "two stages per death from 4 stops at the last stage");
    }

    @Test
    void aMalformedGrowthIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new MobGrowth(" ", 6.0, 1));
        assertThrows(IllegalArgumentException.class, () -> new MobGrowth("void", 0.0, 1));
        assertThrows(IllegalArgumentException.class, () -> new MobGrowth("void", 6.0, 0));
        assertThrows(IllegalArgumentException.class, () -> new MobDefinition("knell", "wither_skeleton", "Knell",
                360, MobBody.VANILLA, "void", null, null, GROWTH), "growth needs stages");
    }

    // --- THE DECISION IS FIXED AT THE DEATH (the follow-up of 2026-11-17; PLAN-dragon-mobs.md section 8 F5) ----------

    /**
     * WHO grows and HOW FAR it stood are decided at the death, from the positions at the death, and travel with the
     * grow: the distance is the candidate's feet to the death's point, AT THE DEATH. Distinct distances (1.0, 2.5, 4.25, each exact in a double)
     * so a transposition between grows is red.
     */
    @Test
    void eachGrowCarriesTheDistanceAtTheDeath() {
        MobGrowth.Candidate a = phageAt(1, new Vec3(1, 64, 0));
        MobGrowth.Candidate b = phageAt(3, new Vec3(0, 64, -2.5));
        MobGrowth.Candidate c = phageAt(2, new Vec3(0, 68.25, 0));
        List<MobGrowth.Grow> grows = MobGrowth.resolve(voidDeathAt(ORIGIN), false, List.of(a, b, c));
        assertEquals(List.of(1.0, 2.5, 4.25), grows.stream().map(MobGrowth.Grow::distance).toList());
        assertEquals(List.of(a.id(), b.id(), c.id()), grows.stream().map(MobGrowth.Grow::id).toList());
    }

    /**
     * THE LATER TICK RE-READS ONLY THE STAGE: {@link MobGrowth#restage} takes the definition, the stage NOW and
     * whether it is alive NOW, and no position, so it cannot re-decide who is in range. Two deaths in one tick each
     * apply on the Phage's own tick, the second reading the stage the first wrote, so they ADD UP (1, then 2, then 3).
     */
    @Test
    void theLaterTickReReadsOnlyTheStageSoTwoDeathsAddUp() {
        int afterFirst = MobGrowth.restage(PHAGE, 1, true);
        int afterSecond = MobGrowth.restage(PHAGE, afterFirst, true);
        assertEquals(2, afterFirst);
        assertEquals(3, afterSecond, "the second death reads the stage the first wrote");
        MobDefinition two = phage(new MobGrowth("void", 6.0, 2));
        assertEquals(5, MobGrowth.restage(two, 4, true), "two stages per death from 4 stops at the last stage");
        assertEquals(5, MobGrowth.restage(PHAGE, 5, true), "a stage 5 stays 5");
    }

    /** A Phage that died or was removed between the death and its own tick is refused; a mob with no growth too. */
    @Test
    void theLaterTickStillRefusesAPhageThatIsNoLongerAlive() {
        assertEquals(2, MobGrowth.restage(PHAGE, 2, false), "died in between: unchanged");
        assertEquals(1, MobGrowth.restage(new MobDefinition("k", "wither_skeleton", "K", 360), 1, true),
                "no growth block: unchanged");
        assertEquals(1, MobGrowth.restage(null, 1, true), "a vanilla mob: unchanged");
    }
}
