package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.weapon.Rarity;
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
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE CASCADE'S CONTENT (PLAN-loot.md, d): {@code content/loot/<file>.yml}, a {@code default:} table and per-source
 * overrides of chances and pools. A Loaded line per file printing each tier's written chance AND its true odds; a
 * refusal by name for a chance outside [0, 1], an unknown rarity, tiers out of rarity order, an unknown item id.
 */
class LootLoaderTest {

    @TempDir
    Path dir;

    private final List<String> lines = new ArrayList<>();
    private final Logger log = capturing();

    /** Each fixture item and its own rarity, as the content gives them (weapons/*.yml). */
    private static final Map<String, Rarity> ITEMS = Map.of("emberblade", Rarity.RARE, "locust", Rarity.RARE,
            "dragons_plume", Rarity.LEGENDARY, "volley_stone", Rarity.EXOTIC);

    private static java.util.Optional<Rarity> rarityOf(String id) {
        return java.util.Optional.ofNullable(ITEMS.get(id));
    }

    private Logger capturing() {
        Logger l = Logger.getAnonymousLogger();
        l.setUseParentHandlers(false);
        l.addHandler(new Handler() {
            @Override public void publish(LogRecord r) { lines.add(r.getLevel() + " " + r.getMessage()); }
            @Override public void flush() { }
            @Override public void close() { }
        });
        return l;
    }

    private static final String GOOD = """
            default:
              tiers:
                - rarity: exotic
                  chance: 0.01
                - rarity: legendary
                  chance: 0.03
                - rarity: epic
                  chance: 0.08
                - rarity: rare
                  chance: 0.12
            sources:
              test_source:
                tiers:
                  - rarity: legendary
                    chance: 0.5
                  - rarity: rare
                    chance: 1.0
                pools:
                  legendary: [dragons_plume]
                  rare: [emberblade, locust]
              pools_only:
                pools:
                  rare: [locust]
            """;

    private Map<String, LootContent> load(String yaml) throws IOException {
        Files.writeString(dir.resolve("test.yml"), yaml, StandardCharsets.UTF_8);
        return new LootLoader(log, LootLoaderTest::rarityOf, false).loadAll(dir.toFile());
    }

    @Test
    void theDefaultAndTheSourcesArriveAsWritten() throws IOException {
        LootContent c = load(GOOD).get("test");
        assertEquals(4, c.defaultTable().tiers().size(), lines.toString());
        assertEquals(Rarity.EXOTIC, c.defaultTable().tiers().get(0).rarity());
        assertEquals(0.12, c.defaultTable().tiers().get(3).chance(), 0.0);
        LootContent.Source s = c.sources().get("test_source");
        assertEquals(2, s.table().tiers().size(), "a source's own tiers override the default's chances");
        assertTrue(s.table().guaranteed());
        assertEquals(List.of("emberblade", "locust"), s.pools().get(Rarity.RARE));
        assertEquals(c.defaultTable().tiers(), c.sources().get("pools_only").table().tiers(),
                "a source with no tiers uses the default's");
        assertEquals(List.of("locust"), c.sources().get("pools_only").pools().get(Rarity.RARE));
    }

    /** e: the Loaded line prints the written chance and the true odds, so 3% and 2.97% are both visible. */
    @Test
    void theLoadedLinePrintsTheWrittenChanceAndTheTrueOdds() throws IOException {
        load(GOOD);
        assertEquals("INFO Loaded loot 'test': default exotic 1.00% (truly 1.000%), legendary 3.00% (truly 2.970%),"
                + " epic 8.00% (truly 7.682%), rare 12.00% (truly 10.602%), nothing truly 77.746%; 2 sources"
                + " [test_source, pools_only]", lines.get(0));
    }

    private void refusedNaming(String yaml, String... needles) throws IOException {
        Map<String, LootContent> got = load(yaml);
        assertTrue(got.isEmpty(), "refused: " + lines);
        assertEquals(1, lines.size(), lines.toString());
        assertTrue(lines.get(0).startsWith("WARNING Refusing loot 'test' in test.yml: "), lines.get(0));
        for (String n : needles) assertTrue(lines.get(0).contains(n), "names '" + n + "': " + lines.get(0));
    }

    @Test
    void aChanceOutsideZeroToOneIsRefusedByName() throws IOException {
        refusedNaming(GOOD.replace("chance: 0.08", "chance: 1.08"), "epic", "outside [0, 1]");
    }

    @Test
    void anUnknownRarityIsRefusedByName() throws IOException {
        refusedNaming(GOOD.replace("rarity: epic", "rarity: mythic"), "'mythic'");
    }

    @Test
    void tiersOutOfRarityOrderAreRefusedByName() throws IOException {
        refusedNaming(GOOD.replace("rarity: exotic", "rarity: common"), "rarest to commonest");
    }

    @Test
    void anUnknownItemIdIsRefusedByName() throws IOException {
        refusedNaming(GOOD.replace("rare: [locust]", "rare: [no_such_item]"), "'no_such_item'", "pools_only");
    }

    /** The seat's mechanism ruling (2026-11-19): a pool holds only items of its own rarity. */
    @Test
    void anItemUnderAnotherRaritysPoolIsRefusedByName() throws IOException {
        refusedNaming(GOOD.replace("legendary: [dragons_plume]", "legendary: [emberblade]"), "'emberblade'", "rare",
                "legendary", "test_source");
    }

    @Test
    void aMissingDefaultIsRefusedByName() throws IOException {
        refusedNaming(GOOD.replace("default:", "fallback:"), "'default:'");
    }

    @Test
    void theShippedTableLoadsWithNoSource() throws IOException {
        Path shipped = Path.of("src/main/resources/content/loot/cascade.yml");
        Files.copy(shipped, dir.resolve("cascade.yml"));
        Map<String, LootContent> got = new LootLoader(log, LootLoaderTest::rarityOf, false).loadAll(dir.toFile());
        LootContent c = got.get("cascade");
        assertEquals(List.of(Rarity.EXOTIC, Rarity.LEGENDARY, Rarity.EPIC, Rarity.RARE, Rarity.UNCOMMON, Rarity.COMMON),
                c.defaultTable().tiers().stream().map(t -> t.rarity()).toList(), lines.toString());
        assertEquals(0.01, c.defaultTable().tiers().get(0).chance(), 0.0, "Ben's placeholder numbers");
        assertEquals(0.03, c.defaultTable().tiers().get(1).chance(), 0.0);
        assertEquals(0.08, c.defaultTable().tiers().get(2).chance(), 0.0);
        assertEquals(0.12, c.defaultTable().tiers().get(3).chance(), 0.0);
        assertEquals(Map.of(), c.sources(), "NO source is wired");
    }

    /**
     * Item 6: THE DEV-ONLY EXAMPLE SOURCE lives under {@code dev_sources:}, loaded ONLY on a dev server, and every id
     * there starts {@code dev_}. A real source may not start {@code dev_}, so the two cannot be mistaken.
     */
    @Test
    void devSourcesLoadOnlyOnADevServerAndAreNamedSo() throws IOException {
        String yaml = GOOD + """
                dev_sources:
                  dev_try_me:
                    pools:
                      rare: [locust]
                """;
        Files.writeString(dir.resolve("test.yml"), yaml, StandardCharsets.UTF_8);
        LootContent off = new LootLoader(log, LootLoaderTest::rarityOf, false).loadAll(dir.toFile()).get("test");
        assertTrue(!off.sources().containsKey("dev_try_me"), "not on an ordinary server");
        LootContent on = new LootLoader(log, LootLoaderTest::rarityOf, true).loadAll(dir.toFile()).get("test");
        assertEquals(List.of("locust"), on.sources().get("dev_try_me").pools().get(Rarity.RARE), "on a dev server");
        lines.clear();
        Files.writeString(dir.resolve("test.yml"), yaml.replace("dev_try_me:", "try_me:"), StandardCharsets.UTF_8);
        assertTrue(new LootLoader(log, LootLoaderTest::rarityOf, true).loadAll(dir.toFile()).isEmpty());
        assertTrue(lines.get(0).contains("dev source 'try_me'") && lines.get(0).contains("dev_"), lines.get(0));
        lines.clear();
        assertTrue(load(GOOD.replace("test_source:", "dev_sneaky:")).isEmpty(), "a real source named dev_");
        assertTrue(lines.get(0).contains("source 'dev_sneaky'") && lines.get(0).contains("dev_sources"), lines.get(0));
    }

    /** The shipped example, against the shipped content's own rarities (every weapon and accessory file). */
    @Test
    void theShippedDevExampleNamesRealItemsOfTheirOwnRarityAndNoEpic() throws IOException {
        Map<String, Rarity> real = new java.util.HashMap<>();
        for (String kind : List.of("weapons", "accessories")) {
            try (var files = Files.list(Path.of("src/main/resources/content", kind))) {
                for (Path f : files.toList()) {
                    String id = f.getFileName().toString().replace(".yml", "");
                    String r = Files.readAllLines(f).stream().filter(l -> l.startsWith("rarity:")).findFirst()
                            .map(l -> l.substring("rarity:".length()).trim()).orElse("common");
                    real.put(id, Rarity.fromName(r));
                }
            }
        }
        Files.copy(Path.of("src/main/resources/content/loot/cascade.yml"), dir.resolve("cascade.yml"));
        LootContent c = new LootLoader(log, id -> java.util.Optional.ofNullable(real.get(id)), true)
                .loadAll(dir.toFile()).get("cascade");
        LootContent.Source ex = c.sources().get("dev_example_not_a_drop");
        assertTrue(ex != null, lines.toString());
        // GATE-loot-roll.md R0c predicts this line verbatim, on a dev server.
        assertEquals("INFO Loaded loot 'cascade': default exotic 1.00% (truly 1.000%), legendary 3.00% (truly 2.970%),"
                + " epic 8.00% (truly 7.682%), rare 12.00% (truly 10.602%), uncommon 20.00% (truly 15.549%),"
                + " common 40.00% (truly 24.879%), nothing truly 37.318%; 1 sources [dev_example_not_a_drop]", lines.get(0));
        assertTrue(!ex.pools().containsKey(Rarity.EPIC) || ex.pools().get(Rarity.EPIC).isEmpty(), "no epic, so the skip shows");
        for (Rarity r : List.of(Rarity.EXOTIC, Rarity.LEGENDARY, Rarity.RARE, Rarity.UNCOMMON, Rarity.COMMON)) {
            assertTrue(!ex.pools().getOrDefault(r, List.of()).isEmpty(), "a pool for " + r);
        }
        // The four dev weapons, deleted 2026-11-21 (emberblade was in the rare pool until then).
        for (String banned : List.of("hunters_bow", "ironblade", "quiver_stone", "emberblade")) {
            assertTrue(ex.pools().values().stream().noneMatch(p -> p.contains(banned)), "no deleted dev weapon: " + banned);
        }
        assertEquals(List.of("ember_staff", "locust", "brawlers_gauntlet", "fletchers_quiver", "sages_scroll"),
                ex.pools().get(Rarity.RARE), "the rare pool, five entries, as written");
    }

    @Test
    void noFilesIsNoTables() {
        File empty = dir.resolve("none").toFile();
        assertTrue(new LootLoader(log, LootLoaderTest::rarityOf, false).loadAll(empty).isEmpty());
    }
}
