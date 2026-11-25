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
 * B3 and B6 (the seat, 2026-11-25): the set's bonuses reach the EXISTING stat paths. The reconcile loop needs a live
 * player, so no unit test reaches it; this pins the wiring by reading the source, with every needle anchored on the
 * line it must sit on and a control that the file was read. The decisions are pinned in core
 * ({@code ArmorSetDecisionTest}, {@code ArmorSetContributionsTest}).
 */
class ArmorSetWiringSignatureTest {

    private static final Path MAIN = Path.of("src", "main", "java", "io", "github", "butterflysmp", "rpg", "paper");

    private static List<String> code(Path p) throws IOException {
        return Files.readAllLines(MAIN.resolve(p), StandardCharsets.UTF_8).stream()
                .filter(l -> !(l.trim().startsWith("//") || l.trim().startsWith("*") || l.trim().startsWith("/*"))).toList();
    }

    private static int at(List<String> c, String needle) {
        for (int i = 0; i < c.size(); i++) if (c.get(i).contains(needle)) return i;
        return -1;
    }

    private static long count(List<String> c, String needle) {
        return c.stream().filter(l -> l.contains(needle)).count();
    }

    @Test
    void theLoopReadsTheSetOnceAndMergesItIntoFourReconcilesFirstArguments() throws IOException {
        List<String> c = code(Path.of("health", "PlayerHealthSystem.java"));
        assertTrue(c.size() > 150, "control: the loop's file was read (198 code lines, comments filtered, 2026-11-25), " + c.size() + " lines");
        assertEquals(1, count(c, "fromSet = ArmorSetItems.contributions(player, keys, armor);"), "ONE read per pass");
        assertEquals(4, count(c, "fromSet.sources(AccessoryStat."), "exactly the four Stat-path bonuses");

        int max = at(c, "stats.reconcileMaxModifiers(id, AccessoryContributions.merged(AccessoryContributions.merged(desiredMax, fromSet.sources(AccessoryStat.MAX_HEALTH)),");
        int def = at(c, "stats.reconcileDefenseModifiers(id, AccessoryContributions.merged(AccessoryContributions.merged(worn.defense(), fromSet.sources(AccessoryStat.DEFENSE)),");
        int mana = at(c, "AccessoryContributions.merged(AccessoryContributions.merged(ManaRegenModifierItems.desiredModifiers(player, keys), fromSet.sources(AccessoryStat.MANA_REGEN)),");
        int regen = at(c, "AccessoryContributions.merged(HealthRegenModifierItems.desiredModifiers(player, keys), fromSet.sources(AccessoryStat.HEALTH_REGEN)),");
        assertTrue(max > 0 && def > 0 && mana > 0 && regen > 0,
                "each bonus merges into its own stat's ONE reconcile: max=" + max + " def=" + def + " mana=" + mana
                        + " regen=" + regen);
        assertTrue(at(c, "stats.reconcileHealthRegenModifiers(id, AccessoryContributions.merged(") == regen - 1,
                "the regen merge is the first argument of the health-regen reconcile");
        assertTrue(at(c, "ArmorBarOverride.apply(player, keys, stats.defenseValue(id), worn.nativeArmor());") > def,
                "the bar draws the converged Defense (set included) and the NATIVE sum, which the set never touches");
        assertEquals(0, count(c, "nativeArmor() + fromSet") + count(c, "nativeArmor += fromSet"),
                "the set bonus never reaches the native armor the bar cancels");
    }

    @Test
    void bothSummonCapReadersPassTheWornSetsGrant() throws IOException {
        List<String> s = code(Path.of("summon", "SummonService.java"));
        int cast = at(s, "java.util.OptionalInt max = profiles.maxSummons(player.getUniqueId(),");
        assertTrue(cast > 0 && s.get(cast + 1).contains(
                "ArmorSetItems.contributions(player, ctx.keys(), armor).summonGrants());"), "the cast");
        List<String> r = code(Path.of("command", "RpgCommand.java"));
        int cmd = at(r, "java.util.OptionalInt now = profiles.maxSummons(player.getUniqueId(),");
        assertTrue(cmd > 0 && r.get(cmd + 1).contains(
                "ArmorSetItems.contributions(player, adapters.keys(), armor).summonGrants());"), "/rpg maxsummons");
    }
}
