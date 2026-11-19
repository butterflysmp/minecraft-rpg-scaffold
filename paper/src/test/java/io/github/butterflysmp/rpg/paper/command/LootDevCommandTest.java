package io.github.butterflysmp.rpg.paper.command;

import io.github.butterflysmp.rpg.core.loot.CascadeLoot;
import io.github.butterflysmp.rpg.core.loot.CascadeTable;
import io.github.butterflysmp.rpg.core.loot.LootTally;
import io.github.butterflysmp.rpg.core.weapon.Rarity;
import io.github.butterflysmp.rpg.paper.content.LootContent;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Item 6: the dev roll command's resolution, its lines, and that it GIVES NOTHING. */
class LootDevCommandTest {

    private static final CascadeTable T = new CascadeTable(List.of(
            new CascadeTable.Tier(Rarity.EXOTIC, 0.01), new CascadeTable.Tier(Rarity.EPIC, 0.08),
            new CascadeTable.Tier(Rarity.RARE, 0.12)));

    private static Map<String, LootContent> loot() {
        return Map.of("cascade", new LootContent("cascade", T, Map.of("dev_x",
                new LootContent.Source(T, Map.of(Rarity.RARE, List.of("locust"))))));
    }

    @Test
    void defaultIsTheDefaultTableAndAnyOtherNameASource() {
        LootDevCommand.Resolved d = LootDevCommand.resolve(loot(), "default");
        assertEquals(T, d.table());
        assertNull(d.pools(), "the default has no pools: a rarity-only roll");
        assertEquals(List.of("locust"), LootDevCommand.resolve(loot(), "dev_x").pools().get(Rarity.RARE));
        assertNull(LootDevCommand.resolve(loot(), "nope"));
    }

    @Test
    void theRollLinesCarryGaveSkippedNothingTheTrueOddsAndTheItems() {
        LootDevCommand.Resolved r = LootDevCommand.resolve(loot(), "dev_x");
        LootTally t = LootTally.of(List.of(new CascadeLoot.Result(Rarity.RARE, "locust", List.of(Rarity.EPIC)),
                new CascadeLoot.Result(null, null, List.of())));
        List<String> lines = LootDevCommand.rollLines(r, t);
        assertEquals("Loot roll 'dev_x' x2 (nothing given to anyone):", lines.get(0));
        assertEquals("  epic: gave 0 (0.000%), skipped 1; truly 7.920%", lines.get(2));
        assertEquals("  rare: gave 1 (50.000%), skipped 0; truly 10.930%", lines.get(3));
        assertEquals("  nothing: 1 (50.000%); truly 80.150% before skips", lines.get(4));
        assertEquals("  items: {locust=1}", lines.get(5));
        assertEquals("LOOTROLL table=dev_x rolls=2 exotic=0/skipped:0/truly:1.000% epic=0/skipped:1/truly:7.920%"
                + " rare=1/skipped:0/truly:10.930% nothing=1/truly:80.150% items=1 given_to_players=0 picked={locust=1}",
                LootDevCommand.logLine(r, t));
    }

    @Test
    void theOddsLinesCarryTheWrittenAndTheTrue() {
        List<String> lines = LootDevCommand.oddsLines(LootDevCommand.resolve(loot(), "default"));
        assertEquals("  epic: written 8.00%, truly 7.920%", lines.get(2));
        assertEquals("  nothing: truly 80.150%", lines.get(4));
    }

    /** IT GIVES NOTHING: the class never touches an inventory, the world, or a give path. */
    @Test
    void theCommandGivesNothing() throws IOException {
        String c = Files.readString(Path.of("src/main/java/io/github/butterflysmp/rpg/paper/command/LootDevCommand.java"))
                .replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\\n]*", "");
        for (String banned : new String[] {"getInventory", "addItem", "dropItem", "setItem", "MenuSafety", "GearItems",
                ".give(", "mint(", "World"}) {
            assertTrue(!c.contains(banned), "the dev roll touches " + banned);
        }
        assertTrue(c.contains("MiningDevCommand.permitted(source.getSender().hasPermission(Permissions.DEV), devServer)"),
                "dev only, as /rpg mining is");
    }
}
