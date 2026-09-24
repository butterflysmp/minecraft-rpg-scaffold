package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.accessory.AccessorySlotKind;
import io.github.butterflysmp.rpg.core.accessory.AccessoryStat;
import io.github.butterflysmp.rpg.core.accessory.AccessoryType;
import io.github.butterflysmp.rpg.core.weapon.AccessoryDefinition;
import io.github.butterflysmp.rpg.core.weapon.AccessoryRegistry;
import io.github.butterflysmp.rpg.core.weapon.Rarity;
import io.github.butterflysmp.rpg.core.weapon.WeaponClass;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The accessory schema, and whether the SHIPPED roster satisfies it. The headline loads the real
 * {@code content/accessories} out of the resources tree, so a typo in a shipped file reddens here
 * rather than arriving as a short count in a boot log -- and asserts every RULED number, because the
 * roster's values are Ben's (RULINGS Q2), not the author's to drift.
 */
class AccessoryLoaderTest {

    private static final double MANA = 100.0;

    @TempDir
    Path dir;

    private final List<String> warnings = new ArrayList<>();

    private Logger capturing() {
        Logger logger = Logger.getLogger("AccessoryLoaderTest-" + System.nanoTime());
        logger.setUseParentHandlers(false);
        logger.addHandler(new Handler() {
            @Override public void publish(LogRecord r) { warnings.add(r.getMessage()); }
            @Override public void flush() { }
            @Override public void close() { }
        });
        return logger;
    }

    private AccessoryRegistry load(String id, String yaml) throws IOException {
        Files.writeString(dir.resolve(id + ".yml"), yaml, StandardCharsets.UTF_8);
        return new AccessoryLoader(capturing(), MANA).loadAll(dir.toFile(), taken -> false);
    }

    private static final String UNIVERSAL = "display_name: \"X\"\nrarity: uncommon\nmaterial: echo_shard\n"
            + "slot: universal\n";

    private void assertRefused(AccessoryRegistry registry, String id, String because) {
        assertTrue(registry.find(id).isEmpty(), id + " must not load");
        assertTrue(warnings.stream().anyMatch(w -> w.contains(because)),
                "refused for the stated reason '" + because + "'; got " + warnings);
    }

    // --- the shipped roster -------------------------------------------------------------------

    @Test
    void theShippedRosterLoads_allSix_withTheRuledNumbers() {
        File shipped = new File("src/main/resources/content/accessories");
        assertTrue(shipped.isDirectory(), "expected " + shipped.getAbsolutePath());
        AccessoryRegistry registry = new AccessoryLoader(capturing(), MANA).loadAll(shipped, id -> false);

        // ZERO IS A DEFECT; and SIX is the ruled roster (Q2), so a skipped file is a short count.
        assertEquals(6, registry.size(), "the ruled roster is six; warnings: " + warnings);
        assertTrue(warnings.isEmpty(), "no shipped file may warn: " + warnings);

        assertUniversal(registry, "ward_charm", "Ward Charm", Map.of(AccessoryStat.DEFENSE, 3.0));
        assertUniversal(registry, "keen_charm", "Keen Charm", Map.of(AccessoryStat.CRIT_CHANCE, 0.05));
        assertUniversal(registry, "mending_charm", "Mending Charm", Map.of(AccessoryStat.HEALTH_REGEN, 0.1));

        assertClassItem(registry, "brawlers_gauntlet", "Brawler's Gauntlet", WeaponClass.MELEE,
                AccessoryType.GAUNTLET, "netherite_scrap", Map.of(AccessoryStat.CLASS_DAMAGE, 3.0,
                        AccessoryStat.CRIT_DAMAGE, 0.25, AccessoryStat.MAX_MANA, -10.0));
        assertClassItem(registry, "fletchers_quiver", "Fletcher's Quiver", WeaponClass.RANGER,
                AccessoryType.QUIVER, "shulker_shell", Map.of(AccessoryStat.CLASS_DAMAGE, 3.0,
                        AccessoryStat.CRIT_CHANCE, 0.05, AccessoryStat.HEALTH_REGEN, -0.04));
        assertClassItem(registry, "sages_scroll", "Sage's Scroll", WeaponClass.MAGE,
                AccessoryType.SCROLL, "prismarine_crystals", Map.of(AccessoryStat.CLASS_DAMAGE, 3.0,
                        AccessoryStat.MAX_MANA, 20.0, AccessoryStat.CRIT_CHANCE, -0.03));
        // Mutation: change any ruled number in a shipped file -> reddens.
    }

    private static void assertUniversal(AccessoryRegistry registry, String id, String name,
                                        Map<AccessoryStat, Double> modifiers) {
        AccessoryDefinition def = registry.find(id).orElseThrow(() -> new AssertionError(id + " did not load"));
        assertEquals(name, def.displayName());
        assertEquals(Rarity.UNCOMMON, def.rarity());
        assertEquals("echo_shard", def.material());
        assertEquals(AccessorySlotKind.UNIVERSAL, def.slot());
        assertEquals(modifiers, def.modifiers());
    }

    private static void assertClassItem(AccessoryRegistry registry, String id, String name, WeaponClass cls,
                                        AccessoryType type, String material, Map<AccessoryStat, Double> modifiers) {
        AccessoryDefinition def = registry.find(id).orElseThrow(() -> new AssertionError(id + " did not load"));
        assertEquals(name, def.displayName());
        assertEquals(Rarity.RARE, def.rarity());
        assertEquals(material, def.material());
        assertEquals(AccessorySlotKind.CLASS, def.slot());
        assertEquals(cls, def.accessoryClass());
        assertEquals(type, def.type());
        assertEquals(modifiers, def.modifiers());
        assertTrue(def.modifiers().keySet().stream().noneMatch(s -> s.token().contains("quiver")
                        || s.token().contains("reload")),
                "RULINGS Q6: no quiver_size or reload_time on any v1 accessory");
    }

    // --- the loader's own refusals ------------------------------------------------------------

    /** *** The max-HP refusal, through the loader: the row the max-HP mutation must redden here too. *** */
    @Test
    void aNegativeMaxHealthIsRefused() throws IOException {
        assertRefused(load("frail", UNIVERSAL + "modifiers:\n  max_health: -5\n"), "frail", "no floor");
    }

    @Test
    void aNegativeOverTheFourTimesBoundIsRefused_andOneUnderItLoads() throws IOException {
        assertRefused(load("greedy", UNIVERSAL + "modifiers:\n  crit_chance: 0.1\n  max_mana: -25\n"),
                "greedy", "too large");
        assertTrue(load("fair", UNIVERSAL + "modifiers:\n  crit_chance: 0.1\n  max_mana: -20\n")
                .find("fair").isPresent(), "4 x 20 = 80 < 100 loads; warnings: " + warnings);
    }

    @Test
    void anUnknownStatIsRefused_notIgnored() throws IOException {
        assertRefused(load("fast", UNIVERSAL + "modifiers:\n  attack_speed: 0.1\n"), "fast", "unknown stat");
        assertRefused(load("big", UNIVERSAL + "modifiers:\n  quiver_size: 2\n"), "big", "unknown stat");
    }

    @Test
    void aModifierThatIsNotANumberIsRefused() throws IOException {
        assertRefused(load("word", UNIVERSAL + "modifiers:\n  defense: lots\n"), "word", "not a number");
    }

    @Test
    void theDefinitionsOwnRulesArriveAsNamedSkippedFiles() throws IOException {
        assertRefused(load("arrowy", "display_name: \"A\"\nmaterial: arrow\nslot: universal\n"
                + "modifiers:\n  defense: 1\n"), "arrowy", "must be one of");
        assertRefused(load("mismatch", "display_name: \"M\"\nmaterial: shulker_shell\nslot: class\n"
                + "class: mage\ntype: quiver\nmodifiers:\n  defense: 1\n"), "mismatch", "belongs to");
        assertRefused(load("classy", UNIVERSAL + "class: ranger\nmodifiers:\n  defense: 1\n"),
                "classy", "slot: universal");
        assertRefused(load("noslot", "display_name: \"N\"\nmaterial: echo_shard\nmodifiers:\n  defense: 1\n"),
                "noslot", "expected universal or class");
    }

    /** An id another gear kind holds is REFUSED -- the other four kinds only warn. */
    @Test
    void anIdAnotherGearKindHoldsIsRefused() throws IOException {
        Files.writeString(dir.resolve("boltor.yml"), UNIVERSAL + "modifiers:\n  defense: 1\n");
        AccessoryRegistry registry = new AccessoryLoader(capturing(), MANA)
                .loadAll(dir.toFile(), id -> id.equals("boltor"));
        assertRefused(registry, "boltor", "already a weapon");
    }

    @Test
    void aMissingDirectoryLoadsNothing_quietly() {
        AccessoryRegistry registry = new AccessoryLoader(capturing(), MANA)
                .loadAll(dir.resolve("absent").toFile(), id -> false);
        assertEquals(0, registry.size());
        assertFalse(warnings.stream().anyMatch(w -> w.contains("skipped")));
    }
}
