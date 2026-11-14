package io.github.butterflysmp.rpg.paper.selftest;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The {@code shift_click} verb (GATE-nexus-back.md) reaches {@code MenuRouting.shiftMove}, the branch that PERFORMS a
 * move, and nothing else. A source scan: the driver needs a live view. What it proves: the event the driver builds
 * carries the action the router keys its shift branch on, and the slot type the view itself reports. What it does not:
 * that the move happened. That is NB1-NB6's {@code INVENTORY} lines.
 */
class SelfTestShiftClickDriverTest {

    private static final Path DRIVERS = Path.of("src", "main", "java", "io", "github", "butterflysmp", "rpg", "paper",
            "selftest", "SelfTestDrivers.java");
    private static final Path ROUTING = Path.of("src", "main", "java", "io", "github", "butterflysmp", "rpg", "paper",
            "menu", "MenuRouting.java");

    @Test
    void theDriverBuildsAShiftMoveAndTheRouterPerformsThatAction() throws IOException {
        String drivers = code(DRIVERS);
        assertTrue(drivers.contains("case SHIFT_CLICK -> shiftClick(p, a);"), "the verb is dispatched to its driver");
        assertTrue(drivers.contains("new InventoryClickEvent(view, view.getSlotType(raw), raw,"),
                "the slot type is the view's own, so getClickedInventory names the half a client click would");
        assertTrue(drivers.contains("ClickType.SHIFT_LEFT, InventoryAction.MOVE_TO_OTHER_INVENTORY));"),
                "a SHIFT_LEFT MOVE_TO_OTHER_INVENTORY, nothing looser");

        String routing = code(ROUTING);
        assertTrue(routing.contains("if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {"),
                "and the router keys its performed shift-move on exactly that action");
    }

    /** The code lines only: javadoc here names the same calls in prose. */
    private static String code(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        assertTrue(lines.size() > 100, "read " + path);
        StringBuilder sb = new StringBuilder();
        for (String l : lines) {
            String t = l.trim();
            if (t.startsWith("//") || t.startsWith("*") || t.startsWith("/*")) continue;
            sb.append(t).append('\n');
        }
        return sb.toString();
    }
}
