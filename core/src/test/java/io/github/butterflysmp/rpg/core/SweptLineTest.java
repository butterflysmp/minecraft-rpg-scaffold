package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.combat.Combatant;
import io.github.butterflysmp.rpg.core.combat.SweptLine;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The swept-line hit-set: who a dash of distance 10 and radius 1.5 along +X passes through.
 *
 * These two tests are the guard for both the geometry and the mob-only rule. Verified by
 * mutation, as the project requires: shrinking the sweep radius in SweptLine drops {@code
 * inside} and reddens the first test; deleting the player skip lets {@code player} through and
 * reddens the second. A test that cannot fail is worth nothing, so both were watched red.
 */
class SweptLineTest {

    private static final Vec3 ALONG_X = new Vec3(1, 0, 0);
    private static final double DISTANCE = 10;
    private static final double RADIUS = 1.5;

    @Test
    void sweepsEnemiesWithinTheRadiusAndMissesThoseOutsideOrOffTheEnds() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var inside = new FakeWorld.Dummy(new Vec3(5, 0, 1.4));    // perpendicular 1.4 < 1.5
        var outside = new FakeWorld.Dummy(new Vec3(5, 0, 1.6));   // perpendicular 1.6 > 1.5
        var pastEnd = new FakeWorld.Dummy(new Vec3(12, 0, 0));    // 12 blocks out, past distance 10
        var behind = new FakeWorld.Dummy(new Vec3(-2, 0, 0));     // behind the start
        add(world, caster, inside, outside, pastEnd, behind);

        Set<Combatant> hit = sweep(world, caster);

        assertTrue(has(hit, inside), "an enemy 1.4 blocks off the line is inside the 1.5 sweep");
        assertFalse(has(hit, outside), "an enemy 1.6 blocks off the line is outside the 1.5 sweep");
        assertFalse(has(hit, pastEnd), "an enemy past the dash distance is not swept");
        assertFalse(has(hit, behind), "an enemy behind the start is not swept");
        assertFalse(has(hit, caster), "the caster never dashes into themselves");
    }

    @Test
    void sparesPlayersBecauseThePayloadIsMobOnly() {
        var world = new FakeWorld();
        var caster = new FakeWorld.Dummy(Vec3.ZERO);
        var mob = new FakeWorld.Dummy(new Vec3(5, 0, 0));        // squarely on the line
        var player = new FakeWorld.Dummy(new Vec3(6, 0, 0));     // squarely on the line
        player.player = true;
        add(world, caster, mob, player);

        Set<Combatant> hit = sweep(world, caster);

        assertTrue(has(hit, mob), "a mob standing on the line is swept");
        assertFalse(has(hit, player), "a player on the line is spared -- the dash payload is mob-only");
    }

    private static Set<Combatant> sweep(FakeWorld world, FakeWorld.Dummy caster) {
        // A generous candidate sphere, exactly as CastExecutor's Dash arm gathers them.
        var candidates = world.combatantsNear(new Vec3(5, 0, 0), 50);
        return Set.copyOf(SweptLine.enemiesAlong(Vec3.ZERO, ALONG_X, DISTANCE, RADIUS,
                candidates, caster.id()));
    }

    private static boolean has(Set<Combatant> hits, FakeWorld.Dummy who) {
        return hits.stream().anyMatch(c -> c.id().equals(who.id()));
    }

    private static void add(FakeWorld world, FakeWorld.Dummy... dummies) {
        for (var d : dummies) world.entities.add(d);
    }
}
