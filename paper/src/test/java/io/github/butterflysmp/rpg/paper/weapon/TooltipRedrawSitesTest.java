package io.github.butterflysmp.rpg.paper.weapon;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * T9 (true tooltips, the seat's ruling of 2026-11-26): EVERY WRITE REDRAWS. Every place in main code that writes an
 * item's enchant state or gear score is followed, within {@link #WINDOW} lines, by the ONE lore function for that item
 * ({@code refreshLore}, {@code remint}, or {@code finishEnchant}, which remints). A write that leaves stale lore is a
 * defect, found here.
 *
 * <p><b>Why a source scan and not a live read:</b> this repo's tests have no server (no MockBukkit), so no test can hold
 * an {@code ItemMeta}; every write site takes one. The one lore function is pure ({@code WeaponLore.build} over
 * {@code ItemEnchantTermsReader}), and T13 proves what it renders; this proves every write reaches it. The boot's gate
 * reads the lore after each write the pad can reach. The site count is PINNED, so a new write site fails here until it
 * is looked at, and a planted write with no redraw is the control.
 */
class TooltipRedrawSitesTest {

    static final int WINDOW = 3;
    /** PINNED, recounted from this scan at 91a8de1c + this slice: RpgCommand 2, AnvilMenu 2, EnchantMenu 1, GrindstoneMenu 1,
     *  GearScoreDevCommand 1, EnchantRollItems 1, GearScoreItems.stampOnAcquire 1. */
    static final int EXPECTED_SITES = 9;

    private static final Pattern WRITE = Pattern.compile(
            "(EnchantItems|GearScoreItems)\\.(write|clear)\\(|^\\s*write\\(meta, rolled,");
    private static final Pattern REDRAW = Pattern.compile("refreshLore\\(|remint\\(|finishEnchant\\(");

    record Site(String where, boolean redraws) {}

    static List<Site> scan(String file, List<String> lines) {
        List<Site> out = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            String code = line.strip();
            if (code.startsWith("*") || code.startsWith("//") || code.startsWith("/*")) continue;   // prose is not a call
            if (code.startsWith("public static void write(") || code.startsWith("public static void clear(")) continue;
            if (!WRITE.matcher(line).find()) continue;
            boolean redraws = false;
            for (int j = i; j <= Math.min(lines.size() - 1, i + WINDOW); j++) {
                if (REDRAW.matcher(lines.get(j)).find()) redraws = true;
            }
            out.add(new Site(file + ":" + (i + 1), redraws));
        }
        return out;
    }

    @Test
    void everyEnchantAndScoreWriteIsFollowedByTheLoreFunction() throws IOException {
        List<Site> sites = new ArrayList<>();
        try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
            for (Path p : files.filter(f -> f.toString().endsWith(".java")).sorted().toList()) {
                sites.addAll(scan(p.getFileName().toString(), Files.readAllLines(p, StandardCharsets.UTF_8)));
            }
        }
        sites.forEach(s -> System.out.println("T9 WRITE SITE " + s.where() + (s.redraws() ? " redraws" : " DOES NOT REDRAW")));
        assertEquals(List.of(), sites.stream().filter(s -> !s.redraws()).map(Site::where).toList(),
                "an enchant or score write that leaves the tooltip stale");
        assertEquals(EXPECTED_SITES, sites.size(), "the write sites moved: read each new one, then re-pin the count");
    }

    @Test
    void aPlantedWriteWithNoRedrawIsCaught() {
        List<Site> planted = scan("Planted.java", List.of(
                "        held.editMeta(meta -> EnchantItems.write(meta, after, adapters.keys()));",
                "        player.sendMessage(done);",
                "        return;",
                "    }",
                "        held.editMeta(meta -> GearScoreItems.write(meta, score, adapters.keys()));",
                "        lookup.remint(player, held, adapters);",
                "     * EnchantItems.write(meta, after) in a javadoc is not a call"));
        assertEquals(List.of(new Site("Planted.java:1", false), new Site("Planted.java:5", true)), planted);
    }
}
