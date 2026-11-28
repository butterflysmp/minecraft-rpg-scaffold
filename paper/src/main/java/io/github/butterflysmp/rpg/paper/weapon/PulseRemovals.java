package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.combat.PulseLine;
import io.github.butterflysmp.rpg.core.combat.PulseRuns;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import org.bukkit.Bukkit;

import java.util.UUID;

/**
 * EVERY REMOVAL OF A CASTER'S PENDING PULSES THAT IS NOT A NEWER CAST (Smolder, the seat's Z5 and A5, 2026-11-27): his
 * death, his quit, a world change, server stop, and the selftest's clear. One path, on {@code OrbRemovals}' pattern, so
 * every cause ends the run the same way: the next pulse's pending link is taken out of {@code PendingWork} (so it never
 * runs) and the {@code <LABEL> cancelled caster= remaining= cause= tick=} line is written while trace is on. A newer
 * cast's replacement is {@code CastExecutor}'s, cause {@code replaced}.
 *
 * <p>A pulse ALREADY EXPANDING is not ended here (CC'S CALL, Z5): its ring is point work at its own origin and
 * finishes. A class or build change mid-cast ends nothing: nothing here is called from one (Z5).
 */
public final class PulseRemovals {

    private PulseRemovals() {}

    /** End {@code caster}'s pending pulses, if any, for {@code cause}. Called on the server thread. Returns how many pulses never fired. */
    public static int end(AdapterContext ctx, UUID caster, String cause) {
        return ctx.pulseRuns().end(caster).map(ended -> discard(ctx, ended, cause)).orElse(0);
    }

    /** Server stop: every run, cause {@code stop}. */
    public static void endAll(AdapterContext ctx) {
        for (PulseRuns.Ended ended : ctx.pulseRuns().endAll()) discard(ctx, ended, "stop");
    }

    private static int discard(AdapterContext ctx, PulseRuns.Ended ended, String cause) {
        if (ended.pendingHandle() >= 0) ctx.work().take(ended.pendingHandle());
        if (ctx.mobTrace().getAsBoolean()) {
            ctx.log().info(PulseLine.cancelled(ended.run().label(), ended.run().caster(), ended.remaining(), cause)
                    .format("none", Bukkit.getCurrentTick()));
        }
        return ended.remaining();
    }
}
