package io.github.butterflysmp.rpg.paper.health;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * K2 (the seat's brief, 2026-11-25): the held weapon's crit chance reaches the SAME stat accessories feed, merged into its
 * ONE reconcile, read off the MAIN HAND only. The loop needs a live player, so this pins the wiring by reading the source,
 * every needle anchored and the file read under a control. The decision is pinned in core ({@code WeaponCritChanceTest}).
 */
class HeldWeaponCritWiringTest {

    private static List<String> code() throws IOException {
        return Files.readAllLines(Path.of("src", "main", "java", "io", "github", "butterflysmp", "rpg", "paper", "health",
                "PlayerHealthSystem.java"), StandardCharsets.UTF_8).stream()
                .filter(l -> !(l.trim().startsWith("//") || l.trim().startsWith("*") || l.trim().startsWith("/*"))).toList();
    }

    /** Mutation: the source dropped from the merge, given a reconcile call of its own, or read off another hand. */
    @Test
    void theHeldWeaponIsOneMoreSourceInTheOneCritChanceReconcile() throws IOException {
        List<String> c = code();
        assertTrue(c.size() > 150, "control: the loop's file was read, " + c.size() + " code lines");
        assertEquals(1, c.stream().filter(l -> l.contains("stats.reconcileCritChanceModifiers(")).count(),
                "ONE reconcile for the stat, never a second");
        int call = -1;
        for (int i = 0; i < c.size(); i++) if (c.get(i).contains("stats.reconcileCritChanceModifiers(id, AccessoryContributions.merged(AccessoryContributions.merged(")) call = i;
        assertTrue(call > 0, "the reconcile's FIRST argument is itself a merge (the gear sources), as the armor set's are");
        assertTrue(c.get(call + 1).contains("CritModifierItems.desiredChanceModifiers(player, keys),"), c.get(call + 1));
        assertTrue(c.get(call + 2).contains("io.github.butterflysmp.rpg.core.weapon.HeldWeaponCrit.sources("),
                "the held weapon is merged into the gear argument: " + c.get(call + 2));
        assertTrue(c.get(call + 3).contains("WeaponItems.heldWeaponId(player, keys).flatMap(weapons::find))),"),
                "read off the MAIN HAND's weapon id, and the inner merge closes there: " + c.get(call + 3));
        assertTrue(c.get(call + 4).contains("fromAccessories.sources(AccessoryStat.CRIT_CHANCE)"),
                "the accessories follow, in the same outer call: " + c.get(call + 4));
        assertEquals(1, c.stream().filter(l -> l.contains("HeldWeaponCrit.sources(")).count(), "and nowhere else");
    }
}
