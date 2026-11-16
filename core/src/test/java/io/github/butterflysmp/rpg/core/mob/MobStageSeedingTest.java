package io.github.butterflysmp.rpg.core.mob;

import org.junit.jupiter.api.Test;

import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * A staged mob's HEALTH comes from its stage, read off the live mob (the {@code mob_stage} tag), and the mob
 * definition's own body fields round-trip. Health here is the AUTHORED base, before M4's GS/100.
 */
class MobStageSeedingTest {

    private static MobRegistry mobs() {
        MobRegistry mobs = new MobRegistry();
        mobs.register(new MobDefinition("phage", "creeper", "Phage", 80, MobBody.VANILLA, "void",
                new MobStages(5, new StageBody(1.0, 1.5, 80), new StageBody(3.0, 0.5, 160))));
        mobs.register(new MobDefinition("knell", "wither_skeleton", "Knell", 360,
                new MobBody(OptionalDouble.of(1.6), OptionalDouble.of(0.6)), "void", null));
        return mobs;
    }

    @Test
    void aStagedMobSeedsFromItsStage() {
        assertEquals(80, MobSeeding.maxHealth(mobs(), "phage", 1, 20.0), 0.0);
        assertEquals(120, MobSeeding.maxHealth(mobs(), "phage", 3, 20.0), 0.0);
        assertEquals(160, MobSeeding.maxHealth(mobs(), "phage", 5, 20.0), 0.0);
    }

    /** An unstaged mob ignores any stage it is given; the old three-argument form is unchanged. */
    @Test
    void anUnstagedMobIgnoresTheStage() {
        assertEquals(360, MobSeeding.maxHealth(mobs(), "knell", 3, 20.0), 0.0);
        assertEquals(360, MobSeeding.maxHealth(mobs(), "knell", 20.0), 0.0);
        assertEquals(20.0, MobSeeding.maxHealth(mobs(), null, 3, 20.0), 0.0, "a vanilla mob stays vanilla");
    }

    @Test
    void aStageOutsideTheMobsRangeIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> MobSeeding.maxHealth(mobs(), "phage", 6, 20.0));
    }

    @Test
    void theBodyFieldsRoundTrip() {
        MobDefinition knell = mobs().find("knell").orElseThrow();
        assertEquals(OptionalDouble.of(1.6), knell.body().scale());
        assertEquals(OptionalDouble.of(0.6), knell.body().speedMultiplier());
        assertEquals("void", knell.element());
    }

    /** A body of 0 or less is refused; a staged mob carries its body in its stages, never twice. */
    @Test
    void aNonPositiveBodyOrABodyBesideStagesIsRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> new MobBody(OptionalDouble.of(0.0), OptionalDouble.empty()));
        assertThrows(IllegalArgumentException.class,
                () -> new MobBody(OptionalDouble.empty(), OptionalDouble.of(-1.0)));
        MobStages stages = new MobStages(5, new StageBody(1.0, 1.5, 80), new StageBody(3.0, 0.5, 160));
        assertThrows(IllegalArgumentException.class, () -> new MobDefinition("phage", "creeper", "Phage", 80,
                new MobBody(OptionalDouble.of(2.0), OptionalDouble.empty()), "void", stages));
    }

    /** The four-argument form every older call site uses still means "a vanilla body, no element, no stages". */
    @Test
    void theOldConstructorMeansNoBodyNoElementNoStages() {
        MobDefinition plain = new MobDefinition("wraith", "zombie", "Wraith", 50);
        assertEquals(MobBody.VANILLA, plain.body());
        assertEquals(null, plain.element());
        assertEquals(null, plain.stages());
    }
}
