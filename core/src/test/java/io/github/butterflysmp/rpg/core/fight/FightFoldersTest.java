package io.github.butterflysmp.rpg.core.fight;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE ORPHAN SWEEP'S AND EVERY DELETE'S SAFETY (the F8 brief, section 4 f; the seat's ruling on the split, item 2). A
 * fight world's folder is deleted ONLY when every test passes:
 * <ol>
 *   <li>it sits directly in the fight's namespace folder ({@code dimensions/<namespace>/});</li>
 *   <li>its name has the fight's prefix;</li>
 *   <li>it holds the marker file the fight wrote at creation;</li>
 *   <li>its REAL path lies strictly inside the namespace folder's real path (no symlink or {@code ..} escape), and
 *       never under {@code dimensions/minecraft}.</li>
 * </ol>
 * Each refusal is tested here by making only that one test fail.
 */
class FightFoldersTest {

    @TempDir
    Path root;

    private static final String PREFIX = "fight_";
    private static final String MARKER = ".rpg-fight-world";

    private Path ns() throws IOException {
        return Files.createDirectories(root.resolve("world/dimensions/rpgfight"));
    }

    private static Path fightFolder(Path parent, String name, boolean marker) throws IOException {
        Path f = Files.createDirectories(parent.resolve(name));
        Files.writeString(f.resolve("region.mca"), "chunks");
        if (marker) Files.writeString(f.resolve(MARKER), "rpg fight world");
        return f;
    }

    @Test
    void aMarkedPrefixedFolderInTheNamespaceIsDeletedWhole() throws IOException {
        Path ns = ns();
        Path f = fightFolder(ns, "fight_ab12cd34", true);
        Files.createDirectories(f.resolve("data"));
        Files.writeString(f.resolve("data/x.dat"), "x");
        assertEquals(FightFolders.Verdict.DELETE, FightFolders.judge(ns, f, PREFIX, MARKER));
        assertEquals(4, FightFolders.deleteTree(ns, f, PREFIX, MARKER), "3 files and 1 inner folder");
        assertFalse(Files.exists(f));
        assertTrue(Files.isDirectory(ns), "the namespace folder itself stays");
    }

    @Test
    void refusalOneAFolderNotDirectlyInTheNamespaceFolder() throws IOException {
        Path ns = ns();
        Path elsewhere = fightFolder(Files.createDirectories(root.resolve("world/dimensions/other")), "fight_x", true);
        assertEquals(FightFolders.Verdict.NOT_IN_NAMESPACE, FightFolders.judge(ns, elsewhere, PREFIX, MARKER));
        Path nested = fightFolder(Files.createDirectories(ns.resolve("fight_outer")), "fight_inner", true);
        assertEquals(FightFolders.Verdict.NOT_IN_NAMESPACE, FightFolders.judge(ns, nested, PREFIX, MARKER),
                "a folder one level deeper is not directly in it");
        assertThrows(IllegalStateException.class, () -> FightFolders.deleteTree(ns, elsewhere, PREFIX, MARKER));
        assertTrue(Files.exists(elsewhere), "refused, untouched");
    }

    @Test
    void refusalTwoANameWithoutThePrefix() throws IOException {
        Path ns = ns();
        Path f = fightFolder(ns, "arena_ab12", true);
        assertEquals(FightFolders.Verdict.NO_PREFIX, FightFolders.judge(ns, f, PREFIX, MARKER));
        assertThrows(IllegalStateException.class, () -> FightFolders.deleteTree(ns, f, PREFIX, MARKER));
        assertTrue(Files.exists(f.resolve("region.mca")));
    }

    @Test
    void refusalThreeNoMarkerFile() throws IOException {
        Path ns = ns();
        Path f = fightFolder(ns, "fight_lookalike", false);
        assertEquals(FightFolders.Verdict.NO_MARKER, FightFolders.judge(ns, f, PREFIX, MARKER));
        Files.createDirectories(f.resolve(MARKER));
        assertEquals(FightFolders.Verdict.NO_MARKER, FightFolders.judge(ns, f, PREFIX, MARKER),
                "a FOLDER named like the marker is not the marker file");
        assertThrows(IllegalStateException.class, () -> FightFolders.deleteTree(ns, f, PREFIX, MARKER));
        assertTrue(Files.exists(f.resolve("region.mca")));
    }

    @Test
    void refusalFourANamespaceUnderDimensionsMinecraft() throws IOException {
        // Point the "namespace" at vanilla's own folder: every other test passes, the fourth refuses.
        Path vanilla = Files.createDirectories(root.resolve("world/dimensions/minecraft"));
        Path f = fightFolder(vanilla, "fight_overworld", true);
        assertEquals(FightFolders.Verdict.ESCAPES, FightFolders.judge(vanilla, f, PREFIX, MARKER));
        assertThrows(IllegalStateException.class, () -> FightFolders.deleteTree(vanilla, f, PREFIX, MARKER));
        assertTrue(Files.exists(f.resolve("region.mca")));
    }

    @Test
    void refusalFourASymlinkThatLeadsOutOfTheNamespace() throws IOException {
        Path ns = ns();
        Path outside = fightFolder(Files.createDirectories(root.resolve("precious")), "fight_real", true);
        Path link = ns.resolve("fight_link");
        try {
            Files.createSymbolicLink(link, outside);
        } catch (IOException | UnsupportedOperationException | SecurityException e) {
            Assumptions.abort("symbolic links cannot be made here: " + e);
        }
        assertEquals(FightFolders.Verdict.ESCAPES, FightFolders.judge(ns, link, PREFIX, MARKER),
                "its real path is outside the namespace");
        assertThrows(IllegalStateException.class, () -> FightFolders.deleteTree(ns, link, PREFIX, MARKER));
        assertTrue(Files.exists(outside.resolve("region.mca")), "what the link points at is untouched");
    }

    @Test
    void theStrictContainmentTestOnPathsAlone() {
        Path ns = Path.of("/srv/run/world/dimensions/rpgfight");
        assertTrue(FightFolders.strictlyInside(ns, ns.resolve("fight_a")));
        assertFalse(FightFolders.strictlyInside(ns, ns), "the namespace folder itself is not inside it");
        assertFalse(FightFolders.strictlyInside(ns, ns.resolve("fight_a/deeper")), "one level only");
        assertFalse(FightFolders.strictlyInside(ns, ns.resolve("../minecraft/overworld").normalize()));
        assertFalse(FightFolders.strictlyInside(ns, Path.of("/srv/run/world/dimensions/rpgfight2/fight_a")),
                "a sibling whose name starts the same is not inside it");
        assertTrue(FightFolders.underVanilla(Path.of("/srv/run/world/dimensions/minecraft/fight_a")));
        assertFalse(FightFolders.underVanilla(ns.resolve("fight_a")));
    }
}
