package io.github.butterflysmp.rpg.core.combat;

import io.github.butterflysmp.rpg.core.FakeWorld;
import io.github.butterflysmp.rpg.core.Vec3;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ignite's blast: the fuse, the targeting rule, the attribution and the two damage flags.
 *
 * <b>THE FUSE IS WHY THIS SUITE EXISTS IN {@code core} AT ALL.</b> {@code FakeWorld}'s scheduler is a
 * CLOCK -- a task lands {@code delayTicks} in the future and fires only when the clock is advanced
 * past it. Every timing row below is written {@code advanceTicks(DELAY - 1)} - assert nothing -
 * {@code advanceTicks(1)} - assert exactly, which is the shape that cannot pass on an off-by-one.
 * An adapter-side equivalent would have no clock to advance and could only be boot-witnessed.
 *
 * <b>WHAT THIS SUITE CANNOT SEE, stated so a green run is not over-read: CHAINING.</b>
 * {@code FakeWorld.Dummy.applyDamage} only decrements a number -- there is no death path here, and
 * core never learns that anything died, which is exactly why one death stays one scheduled task.
 * A green suite proves each blast is delayed, mob-only, attributed and correctly flagged. It proves
 * NOTHING about a cascade. That is {@code GATE-ignite.md}'s job.
 *
 * Each row names the mutation it forces red.
 */
class IgniteTest {

    private static final double EPS = 1e-9;

    /** A vanilla mob's full health, and the number {@link Ignite#DAMAGE} is deliberately under. */
    private static final double VANILLA_MOB_HEALTH = 20.0;

    @Test
    void theBlastLandsOnTheFuseAndNotOneTickBefore() {
        var world = new FakeWorld();
        var victim = new FakeWorld.Dummy(Vec3.ZERO);
        var neighbour = new FakeWorld.Dummy(new Vec3(1, 0, 0));
        world.entities.add(neighbour);

        Ignite.detonate(world, Vec3.ZERO, UUID.randomUUID(), victim.id(), 1);

        assertEquals(100, neighbour.health, EPS, "nothing detonates on the death frame");
        assertEquals(0, neighbour.damageCalls, "and the blast has not been dealt early");

        world.advanceTicks(Ignite.DELAY_TICKS - 1);
        assertEquals(100, neighbour.health, EPS,
                "the fuse is " + Ignite.DELAY_TICKS + " ticks -- not " + (Ignite.DELAY_TICKS - 1));

        world.advanceTicks(1);
        assertEquals(100 - Ignite.DAMAGE, neighbour.health, EPS, "it lands on the fuse tick exactly");
        assertEquals(1, neighbour.damageCalls, "once, not twice");
        // Mutation: change DELAY_TICKS, or schedule with a different delay -> one of the two
        // assertions either side of the boundary reddens. Asserting BOTH sides is what makes this
        // insensitive to direction: a shorter fuse reddens the "not one tick before" row, a longer
        // one reddens the landing row.
    }

    @Test
    void theBlastFiresOncePerDetonationAndDoesNotRepeat() {
        var world = new FakeWorld();
        var neighbour = new FakeWorld.Dummy(new Vec3(1, 0, 0));
        world.entities.add(neighbour);

        Ignite.detonate(world, Vec3.ZERO, UUID.randomUUID(), UUID.randomUUID(), 1);

        world.advanceTicks(Ignite.DELAY_TICKS * 10);
        assertEquals(1, neighbour.damageCalls, "a blast is a single event, not a repeating task");
        assertEquals(0, world.pendingTasks(), "and it leaves nothing scheduled behind it");
        // Mutation: re-arm the task at the end of the body (the shape a DoT would have) -> the
        // damageCalls assertion reddens AND FakeWorld's MAX_TASKS_PER_ADVANCE trip-wire fires,
        // which is the runaway detector this mechanism is in core to get for free.
    }

    @Test
    void theBlastIsMobOnlyAndSparesAPlayerStandingInIt() {
        var world = new FakeWorld();
        var mob = new FakeWorld.Dummy(new Vec3(1, 0, 0));
        var player = new FakeWorld.Dummy(new Vec3(2, 0, 0));
        player.player = true;
        world.entities.add(mob);
        world.entities.add(player);

        Ignite.detonate(world, Vec3.ZERO, UUID.randomUUID(), UUID.randomUUID(), 1);
        world.advanceTicks(Ignite.DELAY_TICKS);

        assertEquals(100 - Ignite.DAMAGE, mob.health, EPS, "the mob takes the blast");
        assertEquals(100, player.health, EPS, "the player standing in it takes nothing");
        assertEquals(0, player.damageCalls, "and was not hit for zero -- it was skipped entirely");
        // Mutation: delete the `c.state().player()` skip -> the player rows redden.
        //
        // THE PLAYER HAS TO BE INSIDE THE RADIUS OR THIS ROW CHECKS NOTHING. At x=2 with RADIUS 4.0
        // it is comfortably in range, so the skip is the only thing sparing it. A player parked
        // outside the radius would pass whether the skip existed or not -- the same defect shape as
        // a fixture whose two candidate rules agree.
    }

    @Test
    void theDeadMobIsExcludedFromItsOwnBlast() {
        var world = new FakeWorld();
        var corpse = new FakeWorld.Dummy(Vec3.ZERO);
        var neighbour = new FakeWorld.Dummy(new Vec3(1, 0, 0));
        world.entities.add(corpse);      // still findable: a death animation outlasts this fuse
        world.entities.add(neighbour);

        Ignite.detonate(world, Vec3.ZERO, UUID.randomUUID(), corpse.id(), 1);
        world.advanceTicks(Ignite.DELAY_TICKS);

        assertEquals(100, corpse.health, EPS, "a mob does not blast its own corpse");
        assertEquals(0, corpse.damageCalls, "and is skipped rather than hit for nothing");
        assertEquals(100 - Ignite.DAMAGE, neighbour.health, EPS, "the neighbour still takes it");
        // Mutation: drop the victimId skip -> the corpse rows redden. The neighbour assertion is
        // the control: it proves the blast fired at all, so a mutation that broke the whole fan-out
        // could not pass this row by making everything take nothing.
    }

    @Test
    void aMobOutsideTheRadiusTakesNothing() {
        var world = new FakeWorld();
        var inside = new FakeWorld.Dummy(new Vec3(Ignite.RADIUS - 0.5, 0, 0));
        var outside = new FakeWorld.Dummy(new Vec3(Ignite.RADIUS + 0.5, 0, 0));
        world.entities.add(inside);
        world.entities.add(outside);

        Ignite.detonate(world, Vec3.ZERO, UUID.randomUUID(), UUID.randomUUID(), 1);
        world.advanceTicks(Ignite.DELAY_TICKS);

        assertEquals(100 - Ignite.DAMAGE, inside.health, EPS, "inside the radius takes the blast");
        assertEquals(100, outside.health, EPS, "outside it takes nothing");
        // The pair straddles RADIUS by half a block either way, so shrinking OR growing the radius
        // reddens one of them. A single in-range target would pass against any radius >= its
        // distance, which is a row that only checks the blast happened.
    }

    @Test
    void theBlastIsCreditedToWHOEVERLITTHEFIREAndNotToTheVictimOrTheKiller() {
        var world = new FakeWorld();
        UUID lighter = UUID.randomUUID();
        UUID killer = UUID.randomUUID();
        var victim = new FakeWorld.Dummy(Vec3.ZERO);
        var neighbour = new FakeWorld.Dummy(new Vec3(1, 0, 0));
        world.entities.add(neighbour);

        Ignite.detonate(world, Vec3.ZERO, lighter, victim.id(), 1);
        world.advanceTicks(Ignite.DELAY_TICKS);

        assertEquals(lighter, neighbour.lastDamageSource,
                "the ignition is the fire's doing, so it is credited to whoever lit it");
        assertNotEquals(killer, neighbour.lastDamageSource, "not to whoever landed the last hit");
        assertNotEquals(victim.id(), neighbour.lastDamageSource, "and never to the exploding corpse");
        // THREE DISTINCT IDS, DELIBERATELY. With a single id in the fixture every candidate rule --
        // credit the lighter, credit the killer, credit the victim -- returns the same answer and
        // the row discriminates nothing. `killer` is never passed to detonate precisely so that a
        // future change routing the killer through here has to fail this row to land.
        //
        // Mutation: pass victimId as the damage source -> the victim assertion reddens. That is the
        // slice-1 credit inversion, where attribution fell back to the victim's own id and a mob
        // was recorded killing itself.
    }

    @Test
    void theBlastWearsFireAndRECRUITSUntilTheLastLink() {
        // THIS ROW ASSERTED THE OPPOSITE UNTIL 2026-09-09, AND THE OLD ASSERTION IS KEPT HERE AS THE
        // RECORD OF WHAT WAS REFUSED. It read theBlastWearsFireForTheGLYPHButAccruesNOTHING and
        // pinned AccrualRule.INERT on every link, with this reasoning:
        //
        //   "a survivor gains no stacks, so a blast never recruits its own fuel ... that flip is the
        //    whole difference between a cascade bounded by the set you lit and one whose only
        //    terminator is running out of mobs."
        //
        // THAT REASONING WAS NOT WRONG. It was overturned BY RULING, and the ruling supplied the
        // bound the reasoning said was missing: MAX_CHAIN_DEPTH. Recruitment without a cap really
        // would terminate only when the mobs ran out. A row whose expectation flips silently is how
        // a later reader concludes the earlier argument was never made.
        var world = new FakeWorld();
        var neighbour = new FakeWorld.Dummy(new Vec3(1, 0, 0));
        world.entities.add(neighbour);

        Ignite.detonate(world, Vec3.ZERO, UUID.randomUUID(), UUID.randomUUID(), 1);
        world.advanceTicks(Ignite.DELAY_TICKS);

        assertEquals("fire", neighbour.lastDamageElement, "marked as fire: the glyph and the matrix");
        assertEquals(AccrualRule.ACCRUES, neighbour.lastDamageAccrual,
                "and it RECRUITS -- a survivor is scorched, so the cascade grows its own fuel");
        assertEquals(1, neighbour.lastDamageDepth, "carrying the depth of the link that dealt it");
        // Mutation: swap ACCRUES for INERT -> the accrual assertion reddens, and the cascade stops
        // spreading through anything it does not outright kill. Nothing else in the suite would
        // notice: the damage numbers, the timing and the targeting are identical either way.
    }

    @Test
    void theFOURTHLinkIsTERMINALAndCarriesNoDepth() {
        // THE CAP, AND IT IS THE ONLY UNIT ROW THAT CAN SEE IT.
        //
        // MAX_CHAIN_DEPTH bounds a cascade's DURATION, which is the property that matters: width is
        // uncomfortable, but an unbounded chain in a dense room runs until the mobs are gone. The
        // fourth link still fires and still damages -- it simply stops propagating.
        var world = new FakeWorld();
        var atCap = new FakeWorld.Dummy(new Vec3(1, 0, 0));
        world.entities.add(atCap);

        Ignite.detonate(world, Vec3.ZERO, UUID.randomUUID(), UUID.randomUUID(), Ignite.MAX_CHAIN_DEPTH);
        world.advanceTicks(Ignite.DELAY_TICKS);

        assertEquals(100 - Ignite.DAMAGE, atCap.health, EPS,
                "the last link DAMAGES normally -- it is not a no-op");
        assertEquals("fire", atCap.lastDamageElement, "and still wears its element, so it draws a glyph");
        assertEquals(AccrualRule.INERT, atCap.lastDamageAccrual,
                "but INERT: it recruits nobody, and its kills cannot ignite either");
        assertEquals(0, atCap.lastDamageDepth,
                "and it carries NO depth -- an INERT hit's depth is read by nothing, ever");
        // ONE ASSERTION COVERS BOTH HALVES OF terminal(), AND ONLY BECAUSE OF THE IMPLEMENTATION.
        // "Does not scorch" and "its kills do not ignite" are the SAME LINE: the fire-kill clause
        // asks ElementAccrual.accruesScorch, which is false for INERT. If that clause ever branches
        // on depth separately from the rule, this row silently stops covering the second half and
        // NOTHING ELSE COVERS IT -- neither here nor in the gate.
        //
        // Mutation: delete the cap (always chained) -> the INERT assertion reddens.
        // Mutation: >= becomes > -> the cascade runs to FIVE links, and this row reddens because
        // depth 4 would still be accruing. That off-by-one is the whole ruling.
    }

    @Test
    void everyLinkBELOWTheCapStillRecruits() {
        // THE CONTROL FOR THE ROW ABOVE. Without it, "the fourth is inert" passes against an
        // implementation where EVERY link is inert -- which is the pre-ruling behaviour, and exactly
        // the regression a careless revert would produce.
        for (int depth = 1; depth < Ignite.MAX_CHAIN_DEPTH; depth++) {
            var world = new FakeWorld();
            var neighbour = new FakeWorld.Dummy(new Vec3(1, 0, 0));
            world.entities.add(neighbour);

            Ignite.detonate(world, Vec3.ZERO, UUID.randomUUID(), UUID.randomUUID(), depth);
            world.advanceTicks(Ignite.DELAY_TICKS);

            assertEquals(AccrualRule.ACCRUES, neighbour.lastDamageAccrual,
                    "link " + depth + " is below the cap and must still recruit");
            assertEquals(depth, neighbour.lastDamageDepth,
                    "and must carry ITS OWN depth -- a constant here would break the chain count");
        }
        // The depth assertion is what stops chained(depth) being written as chained(1): every link
        // would recruit, every link would look right, and the cap would never be reached.
    }

    @Test
    void theBlastGoesTHROUGHDefenseUnlikeTheBurnItCameFrom() {
        var world = new FakeWorld();
        var neighbour = new FakeWorld.Dummy(new Vec3(1, 0, 0));
        world.entities.add(neighbour);

        Ignite.detonate(world, Vec3.ZERO, UUID.randomUUID(), UUID.randomUUID(), 1);
        world.advanceTicks(Ignite.DELAY_TICKS);

        assertTrue(neighbour.damageCalls > 0, "control: the blast landed at all");
        assertEquals(false, neighbour.lastDamageBypassedDefense,
                "a blast is a flat number dealt once, so armour blunts it like any other hit");
        // THE BIT AS DELIVERED IS THE ROW, and it has to be: FakeWorld carries no defense, so
        // honouring the flag would be a no-op here and an assertion about the resulting NUMBER
        // would pass against a port that dropped the parameter on the floor. The recorded flag is
        // the only thing that can see the seam.
        //
        // Mutation: pass DefenseRule.BYPASSED -> this reddens. Nothing else would: with no defense
        // in the fake, both rules produce identical damage.
    }

    @Test
    void theBlastPresentsItsVisualAtTheDetonationPointOnTheFuseTick() {
        var world = new FakeWorld();

        Ignite.detonate(world, new Vec3(5, 6, 7), UUID.randomUUID(), UUID.randomUUID(), 1);
        assertTrue(world.presented.isEmpty(), "the boom waits for the fuse");

        world.advanceTicks(Ignite.DELAY_TICKS);
        assertTrue(world.presented.contains(Ignite.VISUAL_ID), "and lands with the blast");
        assertEquals(new Vec3(5, 6, 7), world.presentedAt.get(0), "at the detonation point");
        // Fires whether or not anything was in radius -- there is no target in this fixture at all.
        // A blast that only announced itself when it hit something would look like a bug in game.
    }

    @Test
    void theProvisionalNumbersArePinnedAndDAMAGESitsBelowAVanillaMob() {
        assertEquals(20, Ignite.DELAY_TICKS,
                "one second -- DELIBERATELY longer than DESIGN's half-second example, so a "
                        + "four-link cascade takes four seconds and reads as a wave rather than an "
                        + "event. Changed 10 -> 20 on 2026-09-09; this row is what made that a "
                        + "decision instead of a drift");
        assertEquals(4.0, Ignite.RADIUS, EPS, "in family with solar_grenade's burst");
        assertEquals(6.0, Ignite.DAMAGE, EPS, "solar_grenade's burst damage");

        // THE INVARIANT, WHICH IS THE HALF THAT ACTUALLY MATTERS. The three values above are
        // PROVISIONAL and the gate is what rules them, so pinning them alone would just make a
        // tuning pass red for no reason. This is the relationship a tuning pass must not break
        // without re-arguing it: a blast that does not kill a full-health vanilla mob is what makes
        // a chain something you SET UP rather than something that happens to you.
        assertTrue(Ignite.DAMAGE < VANILLA_MOB_HEALTH,
                "Ignite.DAMAGE must stay below a vanilla mob's " + VANILLA_MOB_HEALTH
                        + ": at or above it every blast is lethal to a healthy mob and one ignition "
                        + "clears a pack unaided, which changes the mechanism's CHARACTER and not "
                        + "merely its number. Raising it that far means re-making the propagation "
                        + "argument in Ignite's javadoc, not editing this row.");
    }
}
