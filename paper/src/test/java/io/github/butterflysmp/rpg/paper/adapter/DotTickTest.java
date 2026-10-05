package io.github.butterflysmp.rpg.paper.adapter;

import io.github.butterflysmp.rpg.core.combat.Wither;
import io.github.butterflysmp.rpg.paper.content.StatusDefinition;
import io.github.butterflysmp.rpg.paper.content.StatusRegistry;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@code DOTTICK} (the seat's S3 ruling): one trace per DoT tick, from the SHARED store, for scorch AND
 * wither, carrying the victim's uuid and type, the status id, the applier, the amount sent, the cap in
 * force and the tick time. The gate's tick rows read this line, so its text is pinned here.
 */
class DotTickTest {

    private static final UUID VICTIM = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID APPLIER = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    /**
     * THE TEXT THE GATE ROWS QUOTE, exactly. Mutation: drop a field from the format -> reddens.
     */
    @Test
    void theLineCarriesEveryFieldTheRulingNames() {
        assertEquals("DOTTICK 00000000-0000-0000-0000-00000000000a zombie status=withering"
                        + " applier=00000000-0000-0000-0000-00000000000b sent=2.000 cap=2.000 tick=1234",
                new DotTick(VICTIM, "zombie", "withering", APPLIER, 2.0, 2.0).line(1234));
        assertEquals("DOTTICK 00000000-0000-0000-0000-00000000000a zombie status=scorch applier=-"
                        + " sent=1.000 cap=8.000 tick=7",
                new DotTick(VICTIM, "zombie", "scorch", null, 1.0, 8.0).line(7),
                "no applier on record prints '-', not 'null'");
    }

    /**
     * BOTH INSTANCES OF THE ONE STORE EMIT, ONE LINE PER TICK, BEFORE THE DAMAGE -- so the tick that
     * kills has its line ahead of the death. Staged at WS2b's numbers (cap 2.0 on a 100-max zombie
     * binds; on a 20-max one the percent arm gives 1.0).
     *
     * <p>Mutations: move the {@code trace.tick} call below {@code sink.deal} in {@code burnOnce} -> the
     * ordering assertion reddens. Delete it -> the counts redden, for scorch and wither alike.
     */
    @Test
    void scorchAndWitherBothTraceEveryTickBeforeItsDamage() {
        var clock = new FakeTickTarget();
        var witherSink = new FakeScorchSink(clock, 100.0);
        var scorchSink = new FakeScorchSink(clock, 20.0);
        scorchSink.victimType = "skeleton";

        List<DotTick> traced = new ArrayList<>();
        // How many wither burns had LANDED when each wither line was written: 0, 1, 2... if the line
        // comes first, 1, 2, 3... if it comes after the damage.
        List<Integer> witherBurnsAtTrace = new ArrayList<>();
        DotTrace trace = t -> {
            traced.add(t);
            if (t.statusId().equals("withering")) witherBurnsAtTrace.add(witherSink.count());
        };

        var wither = new DotStatus(Wither.RATES, "wither", "withering", trace);
        var scorch = new ScorchStatus("scorch", trace);
        wither.apply(VICTIM, clock, witherSink, 1, 2.0, APPLIER, 200, "wither");
        scorch.apply(UUID.randomUUID(), clock, scorchSink, 1, 8.0, APPLIER, 120, "fire", 0);
        clock.advance(400);

        List<DotTick> witherTicks = traced.stream().filter(t -> t.statusId().equals("withering")).toList();
        List<DotTick> scorchTicks = traced.stream().filter(t -> t.statusId().equals("scorch")).toList();
        assertEquals(5, witherTicks.size(), "wither: five ticks, five lines");
        assertEquals(6, scorchTicks.size(), "scorch: six ticks, six lines -- the same store, traced too");
        assertEquals(witherSink.count(), witherTicks.size(), "one line per burn, no more");

        DotTick first = witherTicks.get(0);
        assertEquals(VICTIM, first.victim());
        assertEquals("zombie", first.victimType());
        assertEquals(APPLIER, first.applier());
        assertEquals(2.0, first.sent(), 1e-9, "the amount SENT: min(5% of 100, cap 2.0)");
        assertEquals(2.0, first.cap(), 1e-9, "the cap in force");
        assertEquals(1.0, scorchTicks.get(0).sent(), 1e-9, "scorch on a 20-max: the percent arm");
        assertEquals("skeleton", scorchTicks.get(0).victimType(), "the type comes from the victim's sink");

        assertEquals(List.of(0, 1, 2, 3, 4), witherBurnsAtTrace,
                "each line is written BEFORE the damage it reports");
    }

    /**
     * THE STATUS ID IS THE CONTENT'S, RESOLVED BY KIND -- {@code withering}, the id a gate row quotes,
     * not the kind {@code wither}. Mutation: return the fallback unconditionally -> reddens.
     */
    @Test
    void theStoreIsNamedForTheLoadedStatusOfItsKind() {
        var statuses = new StatusRegistry();
        statuses.register(new StatusDefinition.Scorch("scorch"));
        statuses.register(new StatusDefinition.Wither("withering", Set.of()));

        assertEquals("withering", AdapterContext.statusIdOf(statuses, StatusDefinition.Wither.class, "wither"));
        assertEquals("scorch", AdapterContext.statusIdOf(statuses, StatusDefinition.Scorch.class, "x"));
        assertEquals("wither", AdapterContext.statusIdOf(null, StatusDefinition.Wither.class, "wither"),
                "no registry (a unit test): the fallback");
        assertEquals("wither", AdapterContext.statusIdOf(new StatusRegistry(),
                StatusDefinition.Wither.class, "wither"), "none of that kind loaded: the fallback");
    }
}
