package io.github.butterflysmp.rpg.paper.content;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * WATER PUTS OUT SCORCH IS WIRED AT EVERY SEAM THE SERVER OWNS (the seat's Y1, Y3, A4 and E1, 2026-11-27), read off
 * the source as {@code PulseRemovalWiringTest} reads its sites, because a real entity cannot be built here. What each
 * seam DECIDES is {@code DouseTest}'s table; what the store does is {@code ScorchDouseTest}'s.
 */
class DouseWiringTest {

    private static String read(String path) throws IOException {
        return Files.readString(Path.of("src/main/java/io/github/butterflysmp/rpg/paper", path), StandardCharsets.UTF_8)
                .replace("\r", "");
    }

    private static int count(String s, String needle) {
        int n = 0;
        for (int at = s.indexOf(needle); at >= 0; at = s.indexOf(needle, at + 1)) n++;
        return n;
    }

    @Test
    void clauseTwoAsksTheWetReadingBeforeItDetonates() throws IOException {
        String b = read("adapter/BukkitCombatant.java");
        int clause = b.indexOf("ElementAccrual.accruesScorch(ctx.elements(), ctx.statuses(), element, accrual.rule())) {");
        assertTrue(clause > 0, "clause 2's guard");
        String block = b.substring(clause, b.indexOf("Aggro-on-hit", clause));
        int wet = block.indexOf("EntityScorchSink.wetness(entity);");
        int decide = block.indexOf("Douse.atDeath(false, true, wet)");
        int refused = block.indexOf("ctx.scorch().igniteRefused(entity.getUniqueId(), entity.getType().key().value(), wet.get());");
        int detonate = block.indexOf("Ignite.detonate(");
        assertTrue(wet > 0 && decide > wet && refused > decide && detonate > refused,
                "read wet, decide, refuse, else detonate: " + wet + " " + decide + " " + refused + " " + detonate);
    }

    @Test
    void clauseOneAsksTheSameReadingAfterTheOncenessGuardAndBeforeItDetonates() throws IOException {
        String l = read("listener/RpgListeners.java");
        int at = l.indexOf("public void onEntityDeath(EntityDeathEvent event) {");
        String body = l.substring(at, l.indexOf("\n    }\n", at));
        int forget = body.indexOf("adapters.scorch().forget(id);");
        int wet = body.indexOf("EntityScorchSink.wetness(mob);");
        int decide = body.indexOf("Douse.atDeath(true, false, wet)");
        int refused = body.indexOf("adapters.scorch().igniteRefused(id, mob.getType().key().value(), wet.get());");
        int detonate = body.indexOf("Ignite.detonate(");
        assertTrue(forget > 0 && wet > forget && decide > wet && refused > decide && detonate > refused,
                "forget, read wet, decide, refuse, else detonate");
    }

    @Test
    void aSplashDousesTheServersOwnExtinguishSet() throws IOException {
        String l = read("listener/RpgListeners.java");
        int at = l.indexOf("public void onWaterBottleSplash(io.papermc.paper.event.entity.WaterBottleSplashEvent event) {");
        assertTrue(at > 0, "the splash listener");
        String body = l.substring(at, l.indexOf("\n    }\n", at));
        assertTrue(body.contains("event.getToExtinguish()"), "the server's set");
        assertTrue(body.contains("adapters.scorch().douse(body.getUniqueId(),"), "douses");
        assertTrue(body.contains("Douse.Cause.SPLASH"), "as a splash");
    }

    @Test
    void bothScorchFlameSitesSetTheFlameOnlyWhenTheApplicationLanded() throws IOException {
        String b = read("adapter/BukkitCombatant.java");
        int accrual = b.indexOf("if (ctx.scorch().apply(entity.getUniqueId(),");
        int accrualFlame = b.indexOf("Math.max(entity.getFireTicks(), accrued.durationTicks())", accrual);
        assertTrue(accrual > 0 && accrualFlame > accrual, "the accrual arm's flame follows its apply");
        assertEquals(1, count(b, "Math.max(entity.getFireTicks(), accrued.durationTicks())"), "one accrual flame");
        int applied = b.indexOf("boolean applied = ctx.scorch().apply(entity.getUniqueId(),");
        int appliedFlame = b.indexOf("if (applied) entity.setFireTicks(Math.max(entity.getFireTicks(), durationTicks));");
        assertTrue(applied > 0 && appliedFlame > applied, "the status arm's flame follows its apply");
        String scorchArm = b.substring(b.indexOf("case StatusDefinition.Scorch ignored -> {"), appliedFlame);
        assertEquals(0, count(scorchArm, "setFireTicks"), "no flame before the Scorch arm's apply");
    }

    @Test
    void productionsStoreCarriesTheDouseTrace() throws IOException {
        String a = read("adapter/AdapterContext.java");
        assertTrue(a.contains("dotTrace(log, mobTrace), douseTrace(log, mobTrace)),"), "the scorch store's three arguments");
    }
}
