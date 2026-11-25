package io.github.butterflysmp.rpg.paper.summon;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P-1 (ruled 2026-11-25): the summon path's mob hit goes through {@code SummonHitPrice}, keyed on ENTITY_ATTACK and
 * fed the dealer's seeded attack -- the same stat the player path's melee reads. The decision is pinned in core
 * ({@code SummonHitPriceTest}); this pins that the listener asks it, since a listener needs a live server.
 */
class SummonHitPriceWiringTest {

    private static final Path LISTENERS = Path.of("src", "main", "java", "io", "github", "butterflysmp", "rpg",
            "paper", "listener", "RpgListeners.java");

    private static List<String> code() throws IOException {
        return Files.readAllLines(LISTENERS, StandardCharsets.UTF_8).stream()
                .filter(l -> !(l.trim().startsWith("//") || l.trim().startsWith("*") || l.trim().startsWith("/*"))).toList();
    }

    private static int at(List<String> c, String needle, int from) {
        for (int i = Math.max(0, from); i < c.size(); i++) if (c.get(i).contains(needle)) return i;
        return -1;
    }

    @Test
    void theSummonPathPricesMeleeFromTheSeededAttackAsThePlayerPathDoes() throws IOException {
        List<String> c = code();
        int summon = at(c, "public void onSummonDamaged(EntityDamageEvent event) {", 0);
        int mobHit = at(c, "case MOB_HIT -> {", summon);
        int price = at(c, "double priced = io.github.butterflysmp.rpg.core.summon.SummonHitPrice.of(", mobHit);
        int melee = at(c, "event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK,", price);
        int seeded = at(c, "adapters.stats().attackValue(meleeDealer.getUniqueId()),", price);
        assertTrue(summon > 0 && mobHit > summon && price > mobHit && melee == price + 1 && seeded == price + 2,
                "the summon's mob hit asks SummonHitPrice, with the melee cause and the seeded attack");
        int player = at(c, "public void onMobMeleeAttack(EntityDamageByEntityEvent event) {", 0);
        int playerSeeded = at(c, ": adapters.stats().attackValue(attacker.getUniqueId());", player);
        assertTrue(player > 0 && playerSeeded > player, "the player path's melee reads the same seeded stat");
    }
}
