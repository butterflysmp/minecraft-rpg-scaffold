package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.ability.AbilityService;
import io.github.butterflysmp.rpg.core.ability.CastExecutor;
import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.ability.ResourceCost;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.combat.Aim;
import io.github.butterflysmp.rpg.core.enchant.ActiveBehaviour;
import io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * BEN'S FOUR CHANGES TO THE CRYSTAL (2026-11-23, after the run of 78826389), the seat's rulings: "1. Make it float up and
 * down and spin slightly similar to a dropped item" (THE MOTION IS THE BODY'S LOOK ONLY: the rest point does not move);
 * "2. Remove the glow, it is excessive"; "3. Increase the range of the shots from the lapis crystal to 15 blocks."; "4. Add
 * a few particles around the lapis crystal". Staging: his eye at (0, 1.62, 0) facing east; the orb rests at (12, 1.62, 0).
 */
class OrbLookTest {

    private static final Aim EAST = new Aim(new Vec3(0, 1.62, 0), new Vec3(1, 0, 0));
    private static final Vec3 REST = new Vec3(12, 1.62, 0);
    private static final CastSpec.OrbShot SHOT = new CastSpec.OrbShot(15, List.of(30.0, 50.0, 70.0), "kinetic", null);
    private static final CastSpec.OrbBlock STILL = new CastSpec.OrbBlock("lapis_block", 0.5, null);
    private static final CastSpec.OrbBlock LIVELY = new CastSpec.OrbBlock("lapis_block", 0.5, null, 0.1, 60, 120, 10,
            "crystal_motes");

    private static AbilityDefinition throwOf(CastSpec.OrbBlock look) {
        return new AbilityDefinition("lapis_crystal/left_click", "Lapis Crystal", "kinetic", 600, new ResourceCost("mana", 30),
                new CastSpec.Orb(12, 10, 300, 0.6, null, SHOT, look), List.of());
    }

    private static final AbilityDefinition BEAM = new AbilityDefinition("lapis_staff/right_click", "Lapis Beam", "kinetic",
            0, ResourceCost.FREE, new CastSpec.Ray(26, "lapis_beam", false, 0), List.of(new EffectSpec.Damage(21, "kinetic")));

    private record Stage(FakeWorld world, FakeWorld.Dummy him, FakeWorld.Dummy n) {}

    private static Stage throwOne(CastSpec.OrbBlock look) {
        var world = new FakeWorld();
        world.hitRadius = 0.3;
        var him = new FakeWorld.Dummy(Vec3.ZERO);
        him.player = true;
        him.behaviours = List.of(new ActiveBehaviour("lapis_crystal", 2,
                new EnchantBehaviour.GrantTrigger("lapis_staff", "left_click", throwOf(look))));
        var n = new FakeWorld.Dummy(new Vec3(12, 0, 5));
        world.entities.addAll(List.of(him, n));
        new CastExecutor(world).execute(new AbilityService.CastResult.Success(throwOf(look), him.snapshot(), EAST, true));
        return new Stage(world, him, n);
    }

    private static UUID body(Stage s) {
        return s.world().orbMarkers.getLast();
    }

    @Test
    void theRestPointNeverMovesWhileItBobsAndTurnsAndEachBeatPosesItAndShowsTheParticles() {
        Stage s = throwOne(LIVELY);
        s.world().advanceTicks(10);
        assertTrue(s.world().orbPoses.isEmpty(), "nothing in flight: at rest only");
        s.world().advanceTicks(200);
        assertEquals(REST, s.world().orbs.of(s.him().id()).orElseThrow().restPoint(), "the store's rest point");
        assertEquals(REST, s.world().markerPositions.get(body(s)), "the body's position: the rest point");
        assertEquals(11, s.world().orbBodyPlacements.get(body(s)).size(), "no placement after the arrival's");
        assertEquals(20, s.world().orbPoses.size(), "one pose a beat: 200 ticks of rest, a beat of 10");
        assertTrue(s.world().orbPoses.stream().allMatch(p -> p.interpolationTicks() == 10), "interpolated over the beat");
        assertEquals(LIVELY.pose(10), s.world().orbPoses.getFirst().pose(), "the first, one beat into the rest");
        long motes = s.world().presented.stream().filter("crystal_motes"::equals).count();
        assertEquals(20, motes, "the particles, once a beat");
        for (int i = 0; i < s.world().presented.size(); i++) {
            if (s.world().presented.get(i).equals("crystal_motes")) assertEquals(REST, s.world().presentedAt.get(i));
        }
    }

    @Test
    void aStillBodyHasNoBeat() {
        Stage s = throwOne(STILL);
        s.world().advanceTicks(10);
        assertEquals(-1, s.world().orbs.of(s.him().id()).orElseThrow().lookTask());
        s.world().advanceTicks(100);
        assertTrue(s.world().orbPoses.isEmpty());
        assertEquals(2, s.world().pendingTasks(), "its expiry and its backstop, and no beat");
    }

    /** The beam meets a bobbing orb exactly where it met a still one: same line, same hit, same tick. */
    @Test
    void theBeamMeetsABobbingOrbExactlyWhereItMetAStillOne() {
        List<String> lines = new java.util.ArrayList<>();
        for (CastSpec.OrbBlock look : List.of(STILL, LIVELY)) {
            Stage s = throwOne(look);
            s.world().advanceTicks(10 + 45);    // mid-bob: 45 ticks into the rest, three quarters of a period
            new CastExecutor(s.world()).execute(new AbilityService.CastResult.Success(BEAM, s.him().snapshot(),
                    new Aim(new Vec3(0, 1.62, 0), new Vec3(1, 0, 0)), true));
            s.world().advanceTicks(5);
            lines.add(s.world().orbShots.stream().map(o -> o.from() + "->" + o.to() + " " + o.visual()).toList()
                    + " N=" + s.n().health);
        }
        assertEquals(lines.get(0), lines.get(1));
        assertTrue(lines.get(0).startsWith("[" + REST + "->"), lines.get(0));
    }

    /**
     * "Increase the range of the shots ... to 15 blocks": the sphere, centre to feet, at its boundary both ways; and a mob
     * inside the server's BOX of 15 (its query, {@code getNearbyEntities}, is a box) but outside the sphere takes none. The
     * fake searches by the box here, as the server does, so the sphere in {@code orbShots} is the cut being tested.
     */
    @Test
    void theShotsReachFifteenAndNoFurther() {
        record Case(Vec3 at, boolean struck) {}
        // The straight pair is cut by the box as well; the diagonal pair (10.6 x sqrt 2 = 14.991, 10.61 x sqrt 2 = 15.005)
        // stands inside the box, so only the sphere decides it: the pair the M4 mutation of 2026-11-23 needed, which the
        // straight pair alone let survive.
        for (Case k : List.of(new Case(new Vec3(12, 1.62, 14.99), true), new Case(new Vec3(12, 1.62, 15.01), false),
                new Case(new Vec3(22.6, 1.62, 10.6), true), new Case(new Vec3(22.61, 1.62, 10.61), false),
                new Case(new Vec3(24, 1.62, 10), false))) {
            double d = Math.sqrt(k.at().distanceSquared(REST));
            var world = new FakeWorld();
            world.hitRadius = 0.3;
            world.boxSearch = true;
            var him = new FakeWorld.Dummy(Vec3.ZERO);
            him.player = true;
            var m = new FakeWorld.Dummy(k.at());
            world.entities.addAll(List.of(him, m));
            var placed = world.orbs.place(him.id(), UUID.randomUUID(), REST, 0.6, 300, "lapis_staff", "lapis_crystal", 2,
                    new io.github.butterflysmp.rpg.core.combat.Orbs.Shot(15, List.of(30.0, 50.0, 70.0), "kinetic", null));
            world.orbs.arrive(him.id(), placed.placed().generation(), 0);
            new CastExecutor(world).execute(new AbilityService.CastResult.Success(BEAM, him.snapshot(), EAST, true));
            world.advanceTicks(5);
            assertEquals(k.struck() ? 1 : 0, world.orbShots.size(), "a mob " + d + " from the orb, at " + k.at());
        }
    }

    @Test
    void noGlow() {
        assertEquals(-1, LIVELY.glowRgb(), "Ben: \"Remove the glow\": no colour");
        assertEquals(0x2A5CFF, new CastSpec.OrbBlock("lapis_block", 0.5, "#2A5CFF").glowRgb(), "the mechanism stays");
    }

    /** The pose turns the block about its own centre and bobs it straight up and down over the rest point. */
    @Test
    void thePoseKeepsTheBlocksCentreOverTheRestPoint() {
        assertEquals(new CastSpec.OrbBlock.Pose(-0.25, -0.25, -0.25, 0), LIVELY.pose(0), "the still centring at 0");
        for (long t = 0; t <= 240; t++) {
            CastSpec.OrbBlock.Pose p = LIVELY.pose(t);
            // An independent rotation: the model centre (1/2,1/2,1/2) scaled by 0.5, turned by yaw about Y as a matrix.
            double c = Math.cos(p.yaw()), sn = Math.sin(p.yaw()), h = 0.25;
            double x = c * h + sn * h, z = -sn * h + c * h;
            assertEquals(0, p.tx() + x, 1e-12, "t=" + t);
            assertEquals(0, p.tz() + z, 1e-12, "t=" + t);
            assertEquals(0.1 * Math.sin(2 * Math.PI * t / 60), p.ty() + h, 1e-12, "the bob, t=" + t);
        }
        assertEquals(0.1, LIVELY.pose(15).ty() + 0.25, 1e-12, "a quarter period: the top of the bob");
        assertEquals(Math.PI, LIVELY.pose(60).yaw(), 1e-12, "half a turn at 60 of 120");
    }

    /**
     * Every removal path ends the look beat with the orb, and the backstop ends it too: nothing left scheduled. A beat
     * also stops itself when its orb or body is gone, so a pending count alone cannot tell a cancel from that; each path
     * is checked to CANCEL the beat that was pending at that moment ({@code FakeWorld.cancelledHandles}).
     */
    @Test
    void theLookBeatEndsWithTheOrbOnEveryRemoval() {
        // expiry
        Stage e = throwOne(LIVELY);
        e.world().advanceTicks(10 + 299);
        long beatAtExpiry = e.world().orbs.of(e.him().id()).orElseThrow().lookTask();
        e.world().advanceTicks(1);
        assertTrue(e.world().cancelledHandles.contains(beatAtExpiry), "the expiry cancelled the pending beat");
        assertEquals(0, e.world().pendingTasks(), "after the expiry");
        int poses = e.world().orbPoses.size();
        e.world().advanceTicks(100);
        assertEquals(poses, e.world().orbPoses.size(), "no beat after it");
        // replacement
        Stage r = throwOne(LIVELY);
        r.world().advanceTicks(60);
        UUID first = body(r);
        long beatAtReplace = r.world().orbs.of(r.him().id()).orElseThrow().lookTask();
        new CastExecutor(r.world()).execute(new AbilityService.CastResult.Success(throwOf(LIVELY), r.him().snapshot(), EAST, true));
        assertTrue(r.world().cancelledHandles.contains(beatAtReplace), "the replacement cancelled the first's pending beat");
        r.world().advanceTicks(400);
        assertEquals(0, r.world().pendingTasks(), "after the replacement and the second's expiry");
        assertFalse(r.world().orbPoses.stream().anyMatch(p -> p.body().equals(first) && p.tick() > 60), "the first's beat ended");
        // the store's removal with its cancel, as every paper removal does (OrbRemovals.discard)
        Stage d = throwOne(LIVELY);
        d.world().advanceTicks(40);
        var gone = d.world().orbs.remove(d.him().id()).orElseThrow();
        for (long h : new long[]{gone.expiryTask(), gone.backstopTask(), gone.lookTask()}) d.world().cancelScheduled(h);
        assertTrue(gone.lookTask() >= 0, "it had a beat to cancel");
        assertEquals(0, d.world().pendingTasks(), "after a removal that cancels its three handles");
        // the backstop: the expiry ended by something else, the store still holding the orb
        Stage b = throwOne(LIVELY);
        b.world().advanceTicks(10);
        b.world().cancelScheduled(b.world().orbs.of(b.him().id()).orElseThrow().expiryTask());
        b.world().advanceTicks(399);                 // tick 409: the backstop fires at 410
        long beatAtBackstop = b.world().orbs.of(b.him().id()).orElseThrow().lookTask();
        b.world().advanceTicks(1);
        assertTrue(b.world().cancelledHandles.contains(beatAtBackstop), "the backstop cancelled the pending beat");
        b.world().advanceTicks(100);
        assertEquals(0, b.world().pendingTasks(), "the backstop ended the beat");
        assertEquals("removed:backstop", b.world().orbLines.getLast().event() + ":" + b.world().orbLines.getLast().cause());
    }
}
