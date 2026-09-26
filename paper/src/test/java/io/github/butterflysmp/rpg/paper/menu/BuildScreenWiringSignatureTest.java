package io.github.butterflysmp.rpg.paper.menu;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Build screen's wiring, read from source (PLAN-build-system.md section 3.3). Paper tests have no
 * server, so what a screen RENDERS is gated in play (GATE-build-screen.md); what can be pinned here is the
 * shape: the commands it replaces are gone, the hub routes slot 21 to it, and neither of its screens can
 * hold or render a real item.
 *
 * <p><b>Every absence row has a positive control beside it</b> -- a needle of the same shape that MUST
 * match in the same file -- so an absence cannot pass because the scan read nothing or the needle's
 * form was wrong.
 */
class BuildScreenWiringSignatureTest {

    private static final Path COMMAND = Path.of(
            "src", "main", "java", "io", "github", "butterflysmp", "rpg", "paper", "command", "RpgCommand.java");
    private static final Path PERMISSIONS = Path.of(
            "src", "main", "java", "io", "github", "butterflysmp", "rpg", "paper", "command", "Permissions.java");
    private static final Path PLUGIN_YML = Path.of("src", "main", "resources", "paper-plugin.yml");
    private static final Path HUB = Path.of(
            "src", "main", "java", "io", "github", "butterflysmp", "rpg", "paper", "menu", "NexusMenu.java");
    private static final Path BUILD_MENU = Path.of(
            "src", "main", "java", "io", "github", "butterflysmp", "rpg", "paper", "menu", "BuildMenu.java");
    private static final Path PICKER = Path.of(
            "src", "main", "java", "io", "github", "butterflysmp", "rpg", "paper", "menu", "BuildPickerMenu.java");

    /** Slice 3 deletes /rpg class, /rpg element (the seat's ruling on option 1) and the dev /rpg build set. */
    @Test
    void theClassElementAndBuildCommandsAreGone() throws IOException {
        List<String> command = read(COMMAND, 2000);
        assertTrue(indexOf(command, "Commands.literal(\"cast\")") > 0,
                "CONTROL: the needle's form must match a command that still exists");
        assertTrue(indexOf(command, "Commands.literal(\"class\")") < 0, "/rpg class is deleted");
        assertTrue(indexOf(command, "Commands.literal(\"element\")") < 0, "/rpg element is deleted");
        assertTrue(indexOf(command, "Commands.literal(\"build\")") < 0, "the dev /rpg build set is deleted");
        assertTrue(indexOf(command, "/rpg class") < 0 && indexOf(command, "/rpg element") < 0,
                "no player-facing text still names a deleted command");
    }

    /** The permission went with the commands: an unused node would read as a live gate. */
    @Test
    void theClassPermissionIsGoneFromBothHomes() throws IOException {
        List<String> permissions = read(PERMISSIONS, 10);
        assertTrue(indexOf(permissions, "\"rpg.command.cast\"") > 0, "CONTROL: a surviving node matches");
        assertTrue(indexOf(permissions, "\"rpg.command.class\"") < 0, "Permissions.CLASS is deleted");

        List<String> yml = read(PLUGIN_YML, 10);
        assertTrue(indexOf(yml, "rpg.command.cast:") > 0, "CONTROL: a surviving node matches");
        assertTrue(indexOf(yml, "rpg.command.class:") < 0, "the paper-plugin.yml node is deleted");
    }

    /** Slot 21 opens the Build screen, and is painted. */
    @Test
    void theHubRoutesSlot21ToTheBuildScreen() throws IOException {
        List<String> hub = read(HUB, 300);
        int branch = indexOf(hub, "if (click.slot() == NexusMenuLayout.BUILD_SLOT) {");
        assertTrue(branch > 0, "the hub has a Build branch");
        int opens = indexOf(hub, "new BuildMenu(viewer, adapters, profiles,", branch);
        int nextReturn = indexOf(hub, "return;", branch);
        assertTrue(opens > branch && opens < nextReturn, "the Build branch opens BuildMenu, inside that branch");
        assertTrue(indexOf(hub, "getInventory().setItem(NexusMenuLayout.BUILD_SLOT,") > 0, "slot 21 is painted");
    }

    /**
     * Neither screen holds an input slot or accepts an item, and neither renders a REAL item: no minted
     * Ability Stone, no clone of anything in the player's inventory. THE ROW THE "render a real item
     * instead of a clone" MUTATION REDDENS.
     */
    @Test
    void neitherScreenCanHoldOrRenderARealItem() throws IOException {
        for (Path path : List.of(BUILD_MENU, PICKER)) {
            List<String> menu = read(path, 100);
            int inputs = indexOf(menu, "protected Set<Integer> inputSlots() {");
            assertTrue(inputs > 0, path + " declares inputSlots");
            assertTrue(menu.get(inputs + 1).contains("return Set.of();"), path + ": no input slots");
            int accepts = indexOf(menu, "protected boolean acceptsInput(ItemStack cursor) {");
            assertTrue(accepts > 0 && menu.get(accepts + 1).contains("return false;"),
                    path + ": accepts nothing, unconditionally");

            assertTrue(indexOf(menu, "MenuIcons.icon(") > 0, "CONTROL: " + path + " renders through MenuIcons");
            assertTrue(indexOf(menu, "StoneItems.") < 0, path + " must not mint or copy an Ability Stone");
            assertTrue(indexOf(menu, "getInventory().getItem(") < 0 && indexOf(menu, ".clone()") < 0,
                    path + " must not render a copy of a real item");
        }
    }

    private static List<String> read(Path path, int atLeast) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        assertTrue(lines.size() > atLeast, "the scan must have read " + path + " -- read " + lines.size()
                + " lines, expected more than " + atLeast + "; an empty read would pass every absence row");
        return lines;
    }

    private static int indexOf(List<String> lines, String needle) {
        return indexOf(lines, needle, 0);
    }

    /** The first line at or after {@code from} holding {@code needle} outside a comment, or -1. */
    private static int indexOf(List<String> lines, String needle, int from) {
        for (int i = Math.max(0, from); i < lines.size(); i++) {
            String trimmed = lines.get(i).strip();
            if (trimmed.startsWith("*") || trimmed.startsWith("//") || trimmed.startsWith("/*")
                    || trimmed.startsWith("#")) continue;
            if (lines.get(i).contains(needle)) return i;
        }
        return -1;
    }
}
