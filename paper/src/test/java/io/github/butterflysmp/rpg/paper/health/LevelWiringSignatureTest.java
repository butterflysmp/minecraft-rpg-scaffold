package io.github.butterflysmp.rpg.paper.health;

import io.github.butterflysmp.rpg.core.accessory.AccessoryStat;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The LEVEL rides the one reconcile per stat (PLAN-level-bonuses.md), read from source, because the loop
 * needs a live {@code Player} and no unit test reaches it.
 *
 * <p>Two things, both of which would fail silently in play. A level map reconciled in a SECOND call for a
 * stat would wipe the gear, accessory and fragment keys every pass (the rule
 * {@link FragmentWiringSignatureTest} guards). And a level map that is simply absent at one stat grants
 * nothing there, with every screen still reading correctly -- the level screen advertises the bonus from
 * {@code LevelBonus}, not from the stat.
 *
 * <p>The guard: each of Ben's four stats has its {@code fromLevel.sources(...)} on the line directly after
 * its fragments' map, inside the same {@code merged(...)} call; no other stat has one; and the weapon damage
 * reconciles once, from {@code weaponDamageSources()}.
 */
class LevelWiringSignatureTest {

    private static final Path HEALTH = Path.of(
            "src", "main", "java", "io", "github", "butterflysmp", "rpg", "paper", "health", "PlayerHealthSystem.java");

    /** Ben's four stat bonuses (2026-09-29). The fifth, weapon damage, is its own stat. */
    private static final List<AccessoryStat> GRANTED = List.of(AccessoryStat.MAX_HEALTH, AccessoryStat.CRIT_CHANCE,
            AccessoryStat.HEALTH_REGEN, AccessoryStat.DEFENSE);

    @Test
    void eachGrantedStatMergesTheLevelInTheSameCallAsItsFragments() throws IOException {
        List<String> lines = Files.readAllLines(HEALTH, StandardCharsets.UTF_8);
        assertTrue(lines.size() > 300, "the scan must have read the file, read " + lines.size());

        for (AccessoryStat stat : GRANTED) {
            String fragments = "fromFragments.sources(AccessoryStat." + stat.name() + ")";
            String level = "fromLevel.sources(AccessoryStat." + stat.name() + ")";
            int site = -1;
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).contains(fragments)) site = i;
            }
            assertTrue(site >= 0, "CONTROL: " + stat + "'s fragment merge point is found");
            assertTrue(lines.get(site).trim().endsWith(fragments + ","),
                    stat + ": the fragments' map is followed by another argument, not closed: " + lines.get(site));
            assertTrue(lines.get(site + 1).trim().startsWith(level + ")"),
                    stat + ": the level's map is the next argument of the same merged(...) call: " + lines.get(site + 1));
        }

        long levelReads = lines.stream().filter(line -> line.contains("fromLevel.sources(")).count();
        assertEquals(GRANTED.size(), levelReads, "the level merges into Ben's four stats and no other");
    }

    @Test
    void theWeaponDamageReconcilesOnceFromItsOwnSources() throws IOException {
        List<String> lines = Files.readAllLines(HEALTH, StandardCharsets.UTF_8);
        long calls = lines.stream()
                .filter(line -> line.contains("stats.reconcileLevelDamageModifiers(id, fromLevel.weaponDamageSources());"))
                .count();
        assertEquals(1, calls, "one reconcile call for the level damage stat");
    }
}
