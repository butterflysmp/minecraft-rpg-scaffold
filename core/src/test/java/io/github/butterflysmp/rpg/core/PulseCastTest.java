package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.ability.AbilityRegistry;
import io.github.butterflysmp.rpg.core.ability.AbilityService;
import io.github.butterflysmp.rpg.core.ability.CastExecutor;
import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.ability.ResourceCost;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.combat.Aim;
import io.github.butterflysmp.rpg.core.combat.AccrualRule;
import io.github.butterflysmp.rpg.core.combat.CooldownTracker;
import io.github.butterflysmp.rpg.core.combat.PulseFront;
import io.github.butterflysmp.rpg.core.combat.PulseLine;
import io.github.butterflysmp.rpg.core.combat.ResourcePool;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PULSES, CAUSED (Smolder, the seat's Z1 to Z7 and Z10, 2026-11-27). Each row stages the condition it names and reads
 * the outcome off each dummy's own health, damage count and the pulse lines, never off an arm's existence.
 *
 * <p>The cast here is Smolder's shape (4 pulses, 60 apart, radius 12, travel 30) with a damage of {@code 10} so a body's
 * health counts its hits: 100 is none, 90 one, 60 four. The caster stands at the origin; bodies stand on the ground
 * plane ({@code y = 0}) at the horizontal distances each row names. The world's clock starts at 0, so the cast is tick 0.
 */
class PulseCastTest {

    private static final double DAMAGE = 10;
    private static final CastSpec.Pulses SMOLDER = new CastSpec.Pulses(4, 60, 12.0, 30, "ring", "boom");

    private static AbilityDefinition ability(CastSpec.Pulses spec) {
        return new AbilityDefinition("smolder", "Smolder", "fire", 0, ResourceCost.FREE, spec,
                List.of(new EffectSpec.Damage(DAMAGE, "fire")));
    }

    private static void cast(FakeWorld world, FakeWorld.Dummy caster, AbilityDefinition def) {
        var registry = new AbilityRegistry();
        registry.register(def);
        var service = new AbilityService(registry, new CooldownTracker(() -> 0L), new ResourcePool(() -> 0L, 100, 1));
        var success = assertInstanceOf(AbilityService.CastResult.Success.class,
                service.cast(caster.snapshot(), def.id(), new Aim(caster.position(), new Vec3(1, 0, 0)), Set.of(def.id())));
        new CastExecutor(world).execute(success);
    }

    private static FakeWorld.Dummy body(FakeWorld world, double x, double z) {
        var d = new FakeWorld.Dummy(new Vec3(x, 0, z));
        world.entities.add(d);
        return d;
    }

    private static FakeWorld.Dummy caster(FakeWorld world) {
        var c = new FakeWorld.Dummy(Vec3.ZERO);
        c.player = true;
        world.entities.add(c);
        return c;
    }

    private static List<String> lines(FakeWorld world, PulseLine.Event event) {
        return world.pulseLines.stream().filter(p -> p.line().event() == event).map(FakeWorld.TracedPulse::text).toList();
    }

    /** The hit lines' (n, tick) for one body, in order. */
    private static List<String> hitsOn(FakeWorld world, FakeWorld.Dummy d) {
        return world.pulseLines.stream()
                .filter(p -> p.line().event() == PulseLine.Event.HIT && p.line().target().equals(d.id()))
                .map(p -> "n=" + p.line().n() + " tick=" + p.tick())
                .toList();
    }

    // =============================================================================== Z1, Z2: four pulses, each body once

    /**
     * THE GATE'S SM2, IN THE FAKE: bodies at 3, 6 and 11.5 blocks, one at 12.5 (outside), a SUMMON at 4 and another
     * PLAYER at 5. Four pulses at ticks 0, 60, 120, 180, each at his feet; each inside body struck ONCE PER PULSE on the
     * tick the reach table gives (3 at +8, 6 at +15, 11.5 at +29); the outside one, the summon and the player never; the
     * caster never.
     *
     * <p>Forces red: a ring timed from the cast instead of each pulse; a body struck on every tick the ring is past it;
     * the summon or player admitted; the outside body admitted (a box's corner, or a sphere's radius read as 12.5).
     */
    @Test
    void fourPulsesEachStrikingEachBodyInRangeOnceOnTheTickTheRingReachesIt() {
        var world = new FakeWorld();
        var him = caster(world);
        var at3 = body(world, 3, 0);
        var at6 = body(world, 0, 6);
        var at115 = body(world, -11.5, 0);
        var at125 = body(world, 12.5, 0);
        var wolf = body(world, 0, -4);
        wolf.summon = true;
        var other = body(world, 5, 0);
        other.player = true;

        cast(world, him, ability(SMOLDER));
        world.advanceTicks(400);

        assertEquals(List.of("n=1 tick=8", "n=2 tick=68", "n=3 tick=128", "n=4 tick=188"), hitsOn(world, at3));
        assertEquals(List.of("n=1 tick=15", "n=2 tick=75", "n=3 tick=135", "n=4 tick=195"), hitsOn(world, at6));
        assertEquals(List.of("n=1 tick=29", "n=2 tick=89", "n=3 tick=149", "n=4 tick=209"), hitsOn(world, at115));
        assertEquals(60.0, at3.health);
        assertEquals(60.0, at6.health);
        assertEquals(60.0, at115.health);
        assertEquals(100.0, at125.health, "12.5 blocks: outside");
        assertEquals(List.of(), hitsOn(world, at125));
        assertEquals(100.0, wolf.health, "a summon: PLAYER -> SUMMON is a no");
        assertEquals(100.0, other.health, "another player: PULSE is spared");
        assertEquals(100.0, him.health, "never the caster");
        assertEquals(0, wolf.damageCalls + other.damageCalls + him.damageCalls + at125.damageCalls);

        List<String> pulses = lines(world, PulseLine.Event.PULSE);
        assertEquals(4, pulses.size());
        for (int n = 1; n <= 4; n++) {
            assertTrue(pulses.get(n - 1).startsWith("SMOLDER pulse n=" + n + " tick=" + (n - 1) * 60 + " origin=0.000 0.000 0.000"),
                    pulses.get(n - 1));
        }
        assertEquals(List.of("SMOLDER cast caster=" + him.id() + " tick=0"), lines(world, PulseLine.Event.CAST));
        assertEquals(0, world.pulseRuns.size(), "the last pulse finished the run");
    }

    /**
     * Z1: EACH HIT IS AN ORDINARY ABILITY HIT. The damage arrives as FIRE, through the accruing rule (so Scorch follows by
     * the element, A1), credited to the caster, and its {@code sent=} is the amount the applier dealt, every term of
     * {@code HitDamage} included: here a 10 % enchant and a +2 class bonus, (10 x 1.10) + 2 = 13.
     */
    @Test
    void eachHitIsAFireDamageThatAccruesCreditedToTheCasterAndSentIsTheDealtAmount() {
        var world = new FakeWorld();
        var him = caster(world);
        him.enchantDamagePercent = 10;
        him.classDamageBonus = 2;
        var z = body(world, 3, 0);
        cast(world, him, ability(new CastSpec.Pulses(1, 60, 12.0, 30, null, null)));
        world.advanceTicks(40);
        assertEquals("fire", z.lastDamageElement);
        assertEquals(AccrualRule.ACCRUES, z.lastDamageAccrual);
        assertEquals(him.id(), z.lastDamageSource);
        double dealt = 100.0 - z.health;
        assertEquals(13.0, dealt, 1e-9);
        assertEquals(dealt, world.pulseLines.stream().filter(p -> p.line().event() == PulseLine.Event.HIT)
                .findFirst().orElseThrow().line().sent(), 1e-12);
    }

    // ======================================================================= Z1, SM4: the origin moves, the ring does not

    /**
     * He is moved 8 blocks east at tick 10, between pulse 1 and pulse 2. Pulse 1's ring keeps its origin: a body 11 west
     * of the OLD origin (19 from the new) is reached at tick 28, after the move, and only by pulse 1. A body 11 east of
     * the old origin (3 from the new) is reached by pulse 1 at tick 28 and by pulses 2 to 4 at +8. A body 15 east of the
     * old origin (7 from the new) is outside pulse 1 and inside the rest.
     */
    @Test
    void eachPulseOriginatesAtHisFeetOnItsTickAndItsRingStaysWhereItStarted() {
        var world = new FakeWorld();
        var him = caster(world);
        var west = body(world, -11, 0);
        var east = body(world, 11, 0);
        var farEast = body(world, 15, 0);
        cast(world, him, ability(SMOLDER));
        world.advanceTicks(10);
        him.moveTo(new Vec3(8, 0, 0));
        world.advanceTicks(400);

        assertEquals(List.of("n=1 tick=28"), hitsOn(world, west));
        assertEquals(List.of("n=1 tick=28", "n=2 tick=68", "n=3 tick=128", "n=4 tick=188"), hitsOn(world, east));
        assertEquals(List.of("n=2 tick=78", "n=3 tick=138", "n=4 tick=198"), hitsOn(world, farEast));
        List<String> pulses = lines(world, PulseLine.Event.PULSE);
        assertTrue(pulses.get(0).contains("origin=0.000 0.000 0.000"), pulses.get(0));
        assertTrue(pulses.get(1).contains("origin=8.000 0.000 0.000"), pulses.get(1));
    }

    /**
     * AT MOST ONCE PER PULSE (Z2): a body struck at 3 (tick 8) that then runs OUTWARD ahead of the ring, to 7 by tick 10,
     * is met by the ring again at tick 18 and is NOT struck again by that pulse. Pulse 2 strikes it anew. Forces red: the
     * struck set not carried from tick to tick.
     */
    @Test
    void aBodyThatRunsOutwardAheadOfTheRingIsStruckOncePerPulse() {
        var world = new FakeWorld();
        var him = caster(world);
        var runner = body(world, 3, 0);
        cast(world, him, ability(new CastSpec.Pulses(2, 60, 12.0, 30, null, null)));
        world.advanceTicks(10);
        runner.moveTo(new Vec3(7, 0, 0));
        world.advanceTicks(100);
        assertEquals(List.of("n=1 tick=8", "n=2 tick=78"), hitsOn(world, runner));
    }

    /**
     * WHEN (Z1), FROM THE CAST TICK, NOT FROM THE LAST PULSE: a cast at tick 7 fires at 7, 67, 127, 187. Forces red: a
     * pulseTick that is not castTick + (n - 1) x interval.
     */
    @Test
    void thePulsesFireOnTheCastTickPlusWholeIntervals() {
        var world = new FakeWorld();
        var him = caster(world);
        world.advanceTicks(7);
        cast(world, him, ability(SMOLDER));
        world.advanceTicks(400);
        assertEquals(List.of(7L, 67L, 127L, 187L), world.pulseLines.stream()
                .filter(p -> p.line().event() == PulseLine.Event.PULSE).map(FakeWorld.TracedPulse::tick).toList());
    }

    // ===================================================================== X6 (a): exact on the server's own phases

    /**
     * THE CONTROL: THE FAKE'S PHASES REPRODUCE THE BOOT OF {@code 01be9d3b}. With {@link FakeWorld#serverPhases} on, an
     * entity delay of 60 booked from the GLOBAL phase (test code, where a cast runs) lands 59 ticks later, as pulse 2 did
     * on every row (6111 to 6170); the same delay booked from the ENTITY phase lands 60 later, as pulses 3 and 4 did.
     * Without this row the exact-tick row below could pass on a fake that never models the phase at all.
     */
    @Test
    void theFakesPhasesReproduceTheBootsCPlus59AndAnEntityPhaseBookingIsExact() {
        var world = new FakeWorld();
        world.serverPhases = true;
        var him = caster(world);
        world.advanceTicks(7);
        long[] fromGlobal = {-1};
        long[] fromEntity = {-1};
        world.scheduleOnCancellable(him.id(), Vec3.ZERO, 60, () -> fromGlobal[0] = world.now());
        world.scheduleOnCancellable(him.id(), Vec3.ZERO, 1, () -> {
            long booked = world.now();
            world.scheduleOnCancellable(him.id(), Vec3.ZERO, 60, () -> fromEntity[0] = world.now() - booked);
        });
        world.advanceTicks(200);
        assertEquals(66L, fromGlobal[0], "booked from the global phase at 7: one short, 66");
        assertEquals(60L, fromEntity[0], "booked from the entity phase: exactly 60 later");
    }

    /**
     * EXACT ON THE SERVER'S PHASES (X6 (a)): the cast at 7 runs in the GLOBAL phase, and every pulse still lands on
     * {@code 7 + 60 (n - 1)}: 7, 67, 127, 187. Forces red: booking the next pulse straight from the cast's phase (the
     * build the boot of {@code 01be9d3b} read, pulse 2 at 66).
     */
    @Test
    void onTheServersPhasesEveryPulseLandsOnTheCastTickPlusWholeIntervals() {
        var world = new FakeWorld();
        world.serverPhases = true;
        var him = caster(world);
        var z = body(world, 3, 0);
        world.advanceTicks(7);
        cast(world, him, ability(SMOLDER));
        world.advanceTicks(400);
        assertEquals(List.of(7L, 67L, 127L, 187L), world.pulseLines.stream()
                .filter(p -> p.line().event() == PulseLine.Event.PULSE).map(FakeWorld.TracedPulse::tick).toList());
        assertEquals(List.of("n=1 tick=15", "n=2 tick=75", "n=3 tick=135", "n=4 tick=195"), hitsOn(world, z));
    }

    /**
     * THE SETTLE HOP IS A PENDING LINK OF THE RUN: ended on the cast's own tick, before the hop has run, the run hands
     * back that link's handle, it is cancelled, and no second pulse follows. Forces red: the hop's handle not held by
     * the run (the removal path would cancel nothing and pulse 2 would fire).
     */
    @Test
    void aRunEndedBeforeItsSettleHopRunsFiresNothingMore() {
        var world = new FakeWorld();
        world.serverPhases = true;
        var him = caster(world);
        cast(world, him, ability(SMOLDER));
        var ended = world.pulseRuns.end(him.id()).orElseThrow();
        world.cancelScheduled(ended.pendingHandle());
        assertEquals(3, ended.remaining());
        world.advanceTicks(400);
        assertEquals(1, lines(world, PulseLine.Event.PULSE).size());
    }

    /** A body that walks in BEHIND the ring is not struck by that pulse: the ring reached its distance earlier. */
    @Test
    void aBodyThatEntersBehindTheRingIsNotStruckByThatPulse() {
        var world = new FakeWorld();
        var him = caster(world);
        var walker = body(world, 20, 0);
        cast(world, him, ability(new CastSpec.Pulses(1, 60, 12.0, 30, null, null)));
        world.advanceTicks(15);              // the ring is at 6
        walker.moveTo(new Vec3(2, 0, 0));    // in, behind it
        world.advanceTicks(40);
        assertEquals(100.0, walker.health);
    }

    /**
     * IN RANGE IS THE BURST'S BOX AND THE HORIZONTAL RADIUS (A5). With the fixture answering as the server does (a box),
     * a body at the box's corner (9, 0, 9) is 12.73 horizontally: not struck. One 3 east and 11 up is inside the box and
     * struck on the tick its HORIZONTAL 3 gives (8); one 3 east and 13 up is outside the box's vertical reach.
     */
    @Test
    void inRangeIsTheBurstsBoxAndTheHorizontalRadiusAndTheTickIsHorizontal() {
        var world = new FakeWorld();
        world.boxSearch = true;
        var him = caster(world);
        var corner = body(world, 9, 9);
        var up11 = new FakeWorld.Dummy(new Vec3(3, 11, 0));
        var up13 = new FakeWorld.Dummy(new Vec3(3, 13, 0));
        world.entities.add(up11);
        world.entities.add(up13);
        cast(world, him, ability(new CastSpec.Pulses(1, 60, 12.0, 30, null, null)));
        world.advanceTicks(40);
        assertEquals(100.0, corner.health, "the box's corner is beyond 12 horizontally");
        assertEquals(List.of("n=1 tick=8"), hitsOn(world, up11));
        assertEquals(100.0, up13.health, "13 up: beyond the box");
    }

    // ===================================================================================== Z5: what ends pending pulses

    /**
     * ENDED BETWEEN PULSES 2 AND 3 (his death, quit, world change, the selftest's clear: the paper removal path does
     * exactly this): the run hands back remaining 2 and the pending link, which is cancelled, and no third pulse and no
     * further hit follow. Pulse 2's ring, already expanding, finishes (CC'S CALL): its 11.5-block hit at 89 lands.
     */
    @Test
    void endingTheRunBetweenPulsesTwoAndThreeLeavesTwoUnfiredAndNothingFollows() {
        var world = new FakeWorld();
        var him = caster(world);
        var z = body(world, 11.5, 0);
        cast(world, him, ability(SMOLDER));
        world.advanceTicks(70);   // pulse 2 fired at 60; its ring reaches 11.5 at 89
        var ended = world.pulseRuns.end(him.id()).orElseThrow();
        world.cancelScheduled(ended.pendingHandle());
        assertEquals(2, ended.remaining());
        world.advanceTicks(400);
        assertEquals(List.of("n=1 tick=29", "n=2 tick=89"), hitsOn(world, z));
        assertEquals(2, lines(world, PulseLine.Event.PULSE).size());
    }

    /** HE IS GONE (a quit): the next pulse is scheduled on him, so it never runs. */
    @Test
    void aCasterWhoIsGoneFiresNoFurtherPulse() {
        var world = new FakeWorld();
        var him = caster(world);
        var z = body(world, 3, 0);
        cast(world, him, ability(SMOLDER));
        world.advanceTicks(30);
        world.entities.remove(him);
        world.advanceTicks(400);
        assertEquals(1, lines(world, PulseLine.Event.PULSE).size());
        assertEquals(90.0, z.health);
    }

    /** A second cast while a run is pending replaces it, writes its cancelled line, and the two never both fire. */
    @Test
    void aSecondCastReplacesThePendingRunAndSaysSo() {
        var world = new FakeWorld();
        var him = caster(world);
        var z = body(world, 3, 0);
        cast(world, him, ability(SMOLDER));
        world.advanceTicks(30);
        cast(world, him, ability(SMOLDER));
        world.advanceTicks(400);
        List<String> cancelled = lines(world, PulseLine.Event.CANCELLED);
        assertEquals(List.of("SMOLDER cancelled caster=" + him.id() + " remaining=3 cause=replaced tick=30"), cancelled);
        assertEquals(5, lines(world, PulseLine.Event.PULSE).size(), "1 of the first run, 4 of the second");
        assertEquals(50.0, z.health, "five hits of 10");
    }

    // ================================================================================================ Z1: the shape

    /** The cooldown floor is the last ring's end: 3 x 60 + 30 = 210. */
    @Test
    void theCooldownFloorIsTheLastRingsEnd() {
        assertEquals(210, SMOLDER.minimumCooldownTicks());
    }

    /** The drawing: the ring visual round the ring each tick of its travel, the pulse visual once at each origin. */
    @Test
    void theRingIsDrawnEachTickAndThePulseVisualOncePerPulse() {
        var world = new FakeWorld();
        var him = caster(world);
        cast(world, him, ability(new CastSpec.Pulses(1, 60, 12.0, 30, "ring", "boom")));
        world.advanceTicks(40);
        assertEquals(1, world.presented.stream().filter("boom"::equals).count());
        int expected = 0;
        for (int t = 0; t <= 30; t++) expected += PulseFront.ringPoints(Vec3.ZERO, PulseFront.radiusAt(t, 12, 30)).size();
        assertEquals(expected, world.presented.stream().filter("ring"::equals).count());
    }

    /** No bundled content reaches these; each is caused. */
    @Test
    void aShapeThatCannotFireIsRefusedByName() {
        assertThrows(IllegalArgumentException.class, () -> new CastSpec.Pulses(0, 60, 12, 30, null, null));
        assertThrows(IllegalArgumentException.class, () -> new CastSpec.Pulses(4, 0, 12, 30, null, null));
        assertThrows(IllegalArgumentException.class, () -> new CastSpec.Pulses(4, 60, 0, 30, null, null));
        assertThrows(IllegalArgumentException.class, () -> new CastSpec.Pulses(4, 60, Double.NaN, 30, null, null));
        assertThrows(IllegalArgumentException.class, () -> new CastSpec.Pulses(4, 60, 12, 0, null, null));
        assertEquals(null, new CastSpec.Pulses(4, 60, 12, 30, " ", "").ring());
        assertFalse(SMOLDER.ring() == null);
    }

    /** Used only to keep an unused-import check honest about UUID: a run is keyed by the caster's id. */
    @Test
    void aRunIsKeyedByItsCaster() {
        var world = new FakeWorld();
        var him = caster(world);
        cast(world, him, ability(SMOLDER));
        UUID owner = world.pulseRuns.of(him.id()).orElseThrow().caster();
        assertEquals(him.id(), owner);
    }
}
