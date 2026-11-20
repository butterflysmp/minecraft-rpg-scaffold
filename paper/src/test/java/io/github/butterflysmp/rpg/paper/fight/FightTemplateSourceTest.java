package io.github.butterflysmp.rpg.paper.fight;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Item 2 of the seat's queue (PLAN-dragon-mobs.md C3): THE TEMPLATE-FILE ARENA SOURCE. Placing, reading and saving a
 * structure need a server, so the wiring is pinned from the source, comments stripped; the pure half is core's
 * {@code ArenaLayoutTest}.
 */
class FightTemplateSourceTest {

    private static final Path PAPER = Path.of("src/main/java/io/github/butterflysmp/rpg/paper");

    private static String strip(String src) {
        src = src.replace("\r\n", "\n").replaceAll("(?s)/\\*.*?\\*/", "");
        return src.replaceAll("//[^\\n]*", "");
    }

    private static String code(String file) throws IOException {
        return strip(Files.readString(PAPER.resolve(file)));
    }

    private static String body(String code, String signature) {
        int from = code.indexOf(signature);
        assertTrue(from >= 0, "found: " + signature);
        int to = code.length();
        for (String next : new String[] {"@Override", "\n    public ", "\n    private ", "\n    void ", "\n    static ",
                "\n    String ", "\n    Object ", "\n    ArenaLayout ", "\n    Path ", "\n    io.github"}) {
            int i = code.indexOf(next, from + signature.length());
            if (i >= 0 && i < to) to = i;
        }
        return code.substring(from, to);
    }

    /** The source decides; a refused template is logged by name and the generated arena is built instead. */
    @Test
    void theSourceDecidesAndARefusedTemplateFallsBackToGenerated() throws IOException {
        String b = body(code("fight/FightFixtures.java"),
                "private io.github.butterflysmp.rpg.core.fight.ArenaLayout buildArena(World w, FightContent f)");
        int ask = b.indexOf("if (arena.source() == FightContent.Source.TEMPLATE) {");
        int load = b.indexOf("templates.load(arena.templateFile(), arena.shape().floorY(), f.settings().crystalCount());");
        int place = b.indexOf("if (t instanceof ArenaTemplates.Loaded loaded) return templates.place(w, loaded);");
        int refused = b.indexOf("\"FIGHTWORLD template refused name=\"");
        int generated = b.indexOf("worlds.build(w, arena.shape());\n        return arena.shape().layout();");
        assertTrue(ask > 0 && load > ask && place > load && refused > place && generated > refused, b);
        assertTrue(code("fight/FightFixtures.java").contains(
                "ArenaSite site = new ArenaSite(w, buildArena(w, f), f.arena().allDownSeconds() * 20);"));
    }

    /** A file that is missing, unreadable, empty or whose markers are wrong is refused, with the reason. */
    @Test
    void aMissingOrMalformedFileIsRefusedWithItsReason() throws IOException {
        String l = body(code("fight/ArenaTemplates.java"), "Object load(String name, int floorY, int crystalCount)");
        assertTrue(l.contains("if (!Files.isRegularFile(file)) return \"no file \" + file;"), l);
        assertTrue(l.contains("} catch (IOException | RuntimeException e) {"), "unreadable: " + l);
        assertTrue(l.contains("if (s.getPalettes().isEmpty()) return \"no blocks in it\";"), l);
        assertTrue(l.contains("if (r instanceof TemplateLayout.Refused refused) return refused.why();"), "the markers' rule: " + l);
    }

    /** Placing: at the layout's origin, unrotated, whole, no entities; then every marker is removed. */
    @Test
    void placingPutsTheFileAtTheOriginAndRemovesTheMarkers() throws IOException {
        String p = body(code("fight/ArenaTemplates.java"), "ArenaLayout place(World w, Loaded t)");
        int place = p.indexOf("t.structure().place(new Location(w, ok.originX(), ok.originY(), ok.originZ()), false, StructureRotation.NONE,");
        int remove = p.indexOf("for (int[] m : ok.markersWorld()) w.getBlockAt(m[0], m[1], m[2]).setType(Material.AIR, false);");
        assertTrue(place > 0 && remove > place, p);
        assertTrue(p.contains("Mirror.NONE, 0, 1.0f, new Random(0));"), "integrity 1.0: every block");
    }

    /** Exporting: markers written for the fill, the file saved, and the world's blocks put back whatever happened. */
    @Test
    void exportingMarksTheFileAndPutsTheWorldBack() throws IOException {
        String e = body(code("fight/ArenaTemplates.java"), "String export(World w, ArenaShape shape, ArenaLayout layout, String name)");
        int mark = e.indexOf("setType(i == 0 ? PODIUM_MARKER : CRYSTAL_MARKER, false);");
        int fill = e.indexOf("s.fill(corner, size, false);");
        int save = e.indexOf("Bukkit.getStructureManager().saveStructure(file.toFile(), s);");
        int back = e.indexOf("} finally {");
        int restore = e.indexOf("w.getBlockAt(m[0], m[1], m[2]).setType(was.get(i), false);");
        assertTrue(mark > 0 && fill > mark && save > fill && back > save && restore > back, e);
        assertTrue(e.contains("if (!name.matches(\"[a-z0-9_]{1,64}\"))"), "a name cannot leave the arenas folder");
        assertTrue(code("fight/FightCommand.java").contains("Commands.literal(\"export\")"), "the dev command");
    }

    /** The markers the file and the reader agree on. */
    @Test
    void theMarkersAreOneConstantPair() throws IOException {
        String t = code("fight/ArenaTemplates.java");
        assertTrue(t.contains("static final Material PODIUM_MARKER = Material.TARGET;"));
        assertTrue(t.contains("static final Material CRYSTAL_MARKER = Material.LODESTONE;"));
        String l = body(t, "Object load(String name, int floorY, int crystalCount)");
        assertTrue(l.contains("if (b.getType() == PODIUM_MARKER) {") && l.contains("} else if (b.getType() == CRYSTAL_MARKER) {"), l);
    }

    /** The probe reads the layout, so a template's fire scan covers the template. */
    @Test
    void theProbeReadsTheLayout() throws IOException {
        String p = body(code("fight/FightFixtures.java"), "private String probeArena(ArenaFight a, String when)");
        assertTrue(p.contains("int r = s.scanRadius();") && p.contains("int toY = s.floorY() + s.scanTopAboveFloor();"), p);
    }
}
