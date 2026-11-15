package io.github.butterflysmp.rpg.paper.menu;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * *** WHAT THIS FILE PROVES, AND WHAT IT DOES NOT. ***
 *
 * <h2>IT PROVES THE WIRING IS WRITTEN. IT NEVER PROVES THE SCREEN BEHAVES.</h2>
 *
 * A SOURCE SCAN, for the reason every {@code *WiringSignatureTest} here gives: a {@code Menu} cannot be constructed
 * without a running server ({@code Bukkit.createInventory}), and there is no MockBukkit. {@link DepartureTest} proves
 * the ORDER of a departure on a Bukkit-free class; this proves that {@code Menu.navigateTo} hands that class the real
 * {@code returnEverything}, that no explicit close crept back in, that the menu goes inert, and that each of the six
 * menu-to-menu buttons this change moved goes through it. What the SERVER then does (the implicit close reaching
 * {@code onClose}) is a jar fact (GATE-nexus-back.md F1-F4) and an AUTO row; what the CLIENT does with the screen and
 * the pointer is Ben's.
 *
 * <p>Anchored on statements, never on a bare token: this package's javadoc names {@code closeInventory} and
 * {@code returnEverything} in prose dozens of times. {@link #isComment} is why the absence rows mean anything, and
 * {@link #theScannerSkipsCommentaryAndStillSeesCode} is its control.
 */
class MenuNavigationSignatureTest {

    private static final Path MENU_DIR = Path.of(
            "src", "main", "java", "io", "github", "butterflysmp", "rpg", "paper", "menu");

    /** The six departures this change moved, each named by the branch line that opens it. */
    private static final Map<String, String> DEPARTURES = Map.of(
            "AnvilMenu.java", "if (click.slot() == BACK_SLOT && openedFromNexus()) {",
            "EnchantMenu.java", "if (click.slot() == BACK_SLOT && openedFromNexus()) {",
            "GrindstoneMenu.java", "if (click.slot() == BACK_SLOT && openedFromNexus()) {",
            "NexusVaultMenu.java", "if (click.slot() == NexusVaultLayout.BACK_SLOT && hub != null) {",
            "CraftingMenu.java#back", "if (click.slot() == CraftingMenuLayout.BACK_SLOT && origin() == Origin.FROM_NEXUS) {",
            "CraftingMenu.java#browser", "if (click.slot() == CraftingMenuLayout.BROWSER_SLOT) {");

    /**
     * Every {@code closeInventory()} left in the package, by file. Each is a Close button: the branch line before it
     * names a {@code CLOSE_SLOT} and the line after it is {@code return;} (asserted below). Close and Esc are NOT
     * changed by this slice (the seat's ruling 4).
     */
    private static final Map<String, Integer> CLOSE_BUTTONS = new TreeMap<>(Map.ofEntries(
            Map.entry("AnvilMenu.java", 1), Map.entry("BuildMenu.java", 1), Map.entry("BuildPickerMenu.java", 1),
            Map.entry("CraftingMenu.java", 1), Map.entry("EnchantMenu.java", 1), Map.entry("EquipmentMenu.java", 1),
            Map.entry("GrindstoneMenu.java", 1), Map.entry("LevelMenu.java", 1), Map.entry("NexusMenu.java", 1),
            Map.entry("NexusSlotPickerMenu.java", 1), Map.entry("NexusVaultMenu.java", 1),
            Map.entry("SettingsMenu.java", 1)));

    // ------------------------------------------------------------------ Menu.navigateTo

    @Test
    void navigateToGivesBackFirstThenHopsAndNeverClosesExplicitly() throws IOException {
        List<String> body = body(read(MENU_DIR.resolve("Menu.java"), 300),
                "protected final void navigateTo(Scheduler scheduler, Supplier<? extends Menu> next) {");

        int giveBack = indexOf(body, "this::returnEverything,");
        int hop = indexOf(body, "hop -> scheduler.onEntity(viewer, hop),");
        int open = indexOf(body, "() -> next.get().open());");
        assertTrue(giveBack >= 0, "navigateTo must hand Departure the REAL returnEverything as its give-back");
        assertTrue(hop > giveBack, "the hop is Departure's SECOND argument: the give-back is the first, and runs first");
        assertTrue(open > hop, "the next menu opens inside the hop (Departure's third argument), never inline");
        assertTrue(indexOf(body, "departure.begin(") >= 0, "and all three go through the one Departure");
        assertEquals(-1, indexOf(body, "closeInventory"),
                "NO EXPLICIT CLOSE. The implicit close openInventory performs (F1) is what reaches onClose; an explicit"
                        + " one is the visible close-and-reopen Ben reported");
    }

    @Test
    void menuJavaHasNoExplicitCloseAnywhere() throws IOException {
        assertEquals(-1, indexOf(read(MENU_DIR.resolve("Menu.java"), 300), "closeInventory"),
                "the base class never closes a screen itself");
    }

    /** The freeze: after a departure begins, the menu takes no click and permits no drag. */
    @Test
    void aDepartingMenuIsInertToClicksAndDrags() throws IOException {
        List<String> menu = read(MENU_DIR.resolve("Menu.java"), 300);

        List<String> click = body(menu, "public final void handleClick(InventoryClickEvent event) {");
        int cancel = indexOf(click, "event.setCancelled(true);");
        assertTrue(cancel >= 0, "the click is cancelled first, as it always was");
        assertEquals(cancel + 1, indexOf(click, "if (departure.underway()) return;"),
                "and the very next statement drops every click on a departing menu, BEFORE routing");

        List<String> drag = body(menu, "public final void handleDrag(InventoryDragEvent event) {");
        int dragCancel = indexOf(drag, "event.setCancelled(true);");
        assertTrue(dragCancel >= 0);
        assertEquals(dragCancel + 1, indexOf(drag, "if (departure.underway()) return;"),
                "and a drag onto a departing menu is refused right after it is cancelled");
    }

    // ------------------------------------------------------------------ the six departures

    @Test
    void eachOfTheSixDeparturesGoesThroughNavigateToAndNothingElse() throws IOException {
        for (Map.Entry<String, String> d : DEPARTURES.entrySet()) {
            String file = d.getKey().split("#")[0];
            List<String> lines = read(MENU_DIR.resolve(file), 300);
            int branch = indexOf(lines, d.getValue());
            assertTrue(branch >= 0, d.getKey() + ": the branch line must exist -- " + d.getValue());
            int end = indexOf(lines, "return;", branch);
            List<String> arm = lines.subList(branch, end + 1);

            assertTrue(indexOf(arm, "navigateTo(adapters.scheduler(),") >= 0, d.getKey() + " departs through navigateTo");
            assertEquals(-1, indexOf(arm, "closeInventory"), d.getKey() + ": no explicit close before the hop");
            assertEquals(-1, indexOf(arm, "onEntity"), d.getKey() + ": no hop of its own beside navigateTo's");
        }
    }

    /** No scheduled open survives in the five input-slot menus: every one of their departures is navigateTo's. */
    @Test
    void theFiveInputSlotMenusOpenNothingThemselves() throws IOException {
        for (String file : List.of("AnvilMenu.java", "CraftingMenu.java", "EnchantMenu.java", "GrindstoneMenu.java",
                "NexusVaultMenu.java")) {
            assertEquals(-1, indexOf(read(MENU_DIR.resolve(file), 300), ".open()"),
                    file + " must not call open() itself; navigateTo does");
        }
    }

    // ------------------------------------------------------------------ the census of closes

    @Test
    void everyRemainingCloseInTheMenuPackageIsACloseButton() throws IOException {
        Map<String, Integer> found = new TreeMap<>();
        List<Path> files;
        try (Stream<Path> s = Files.list(MENU_DIR)) {
            files = s.filter(p -> p.toString().endsWith(".java")).sorted().toList();
        }
        assertTrue(files.size() > 40, "the scan must have listed the menu package, found " + files.size());

        for (Path f : files) {
            List<String> lines = read(f, 0);
            for (int i = 0; i < lines.size(); i++) {
                if (isComment(lines.get(i)) || !lines.get(i).contains("closeInventory")) continue;
                found.merge(f.getFileName().toString(), 1, Integer::sum);

                assertEquals("viewer.closeInventory();", lines.get(i).trim(), f.getFileName() + ":" + (i + 1));
                assertEquals("return;", nextCode(lines, i).trim(),
                        f.getFileName() + ":" + (i + 1) + " a close is followed by return, never by a hop");
                assertTrue(previousCode(lines, i).contains("CLOSE_SLOT"),
                        f.getFileName() + ":" + (i + 1) + " a close sits under a CLOSE_SLOT branch, so it is a Close button");
            }
        }
        assertEquals(CLOSE_BUTTONS, found, "the census of closeInventory() calls in the menu package");
    }

    // ------------------------------------------------------------------ the scanner's own controls

    @Test
    void theScannerSkipsCommentaryAndStillSeesCode() {
        List<String> staged = List.of(
                "     * viewer.closeInventory();",
                "        // viewer.closeInventory();",
                "        viewer.closeInventory();");
        assertEquals(2, indexOf(staged, "closeInventory"),
                "a close named in javadoc or commented out is not a close; the code line still is");
        assertEquals(-1, indexOf(List.of("alpha"), "gamma"), "absence is -1, not 0");
    }

    @Test
    void theBodyReaderStopsAtTheMethodsOwnClosingBrace() {
        List<String> staged = List.of(
                "    void a() {",
                "        if (x) {",
                "            y();",
                "        }",
                "    }",
                "    void b() {",
                "        closeInventory();",
                "    }");
        List<String> a = body(staged, "void a() {");
        assertEquals(5, a.size(), "the body ends at the method's own brace, not the first one nested inside it");
        assertEquals(-1, indexOf(a, "closeInventory"), "and does not run on into the next method");
    }

    // ------------------------------------------------------------------ helpers

    private static List<String> read(Path path, int atLeast) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        assertTrue(lines.size() > atLeast, "the scan must have read " + path + "; read " + lines.size() + " lines");
        return lines;
    }

    /** The method starting at {@code declaration}, through its own closing brace (by indentation). */
    private static List<String> body(List<String> lines, String declaration) {
        int start = indexOf(lines, declaration);
        assertTrue(start >= 0, "declaration not found: " + declaration);
        String indent = lines.get(start).substring(0, lines.get(start).indexOf(lines.get(start).trim()));
        for (int i = start + 1; i < lines.size(); i++) {
            if (lines.get(i).equals(indent + "}")) return new ArrayList<>(lines.subList(start, i + 1));
        }
        throw new AssertionError("no closing brace for " + declaration);
    }

    private static int indexOf(List<String> lines, String needle) {
        return indexOf(lines, needle, 0);
    }

    private static int indexOf(List<String> lines, String needle, int from) {
        for (int i = Math.max(0, from); i < lines.size(); i++) {
            if (isComment(lines.get(i))) continue;
            if (lines.get(i).contains(needle)) return i;
        }
        return -1;
    }

    private static String nextCode(List<String> lines, int i) {
        for (int j = i + 1; j < lines.size(); j++) {
            if (!lines.get(j).isBlank() && !isComment(lines.get(j))) return lines.get(j);
        }
        return "";
    }

    private static String previousCode(List<String> lines, int i) {
        for (int j = i - 1; j >= 0; j--) {
            if (!lines.get(j).isBlank() && !isComment(lines.get(j))) return lines.get(j);
        }
        return "";
    }

    private static boolean isComment(String line) {
        String trimmed = line.trim();
        return trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/*");
    }
}
