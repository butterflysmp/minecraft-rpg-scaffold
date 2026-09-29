package io.github.butterflysmp.rpg.paper.menu;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The screen commands (Ben, 2026-09-29: "Names are good"): the names, the gates, and the permission nodes,
 * read from source where no server is available.
 */
class ScreenPermissionsTest {

    private static final Path PLUGIN_YML = Path.of("src", "main", "resources", "paper-plugin.yml");
    private static final Path COMMANDS = Path.of(
            "src", "main", "java", "io", "github", "butterflysmp", "rpg", "paper", "command", "ScreenCommands.java");

    @Test
    void theCommandsAreExactlyBensNames() {
        assertEquals(List.of("level", "build", "gear", "vault", "anvil", "craft", "recipes", "enchanting",
                        "grindstone", "settings"),
                Arrays.stream(NexusScreens.Screen.values()).map(NexusScreens.Screen::command).toList(),
                "and /menu, which already exists. Not /enchant: that is vanilla's root (javap, EnchantCommand)");
    }

    /**
     * The hub's gates, as LITERALS -- Ben's numbers -- so a command that stopped asking the gate, or a gate
     * that moved, reddens here rather than quietly agreeing. /recipes takes crafting's.
     */
    @Test
    void eachCommandCarriesItsHubButtonsLevelGate() {
        assertEquals(20, NexusScreens.Screen.VAULT.unlockLevel());
        assertEquals(7, NexusScreens.Screen.ANVIL.unlockLevel());
        assertEquals(3, NexusScreens.Screen.CRAFT.unlockLevel());
        assertEquals(3, NexusScreens.Screen.RECIPES.unlockLevel(), "the browser's Back opens crafting");
        assertEquals(10, NexusScreens.Screen.ENCHANTING.unlockLevel());
        assertEquals(13, NexusScreens.Screen.GRINDSTONE.unlockLevel());
        for (NexusScreens.Screen ungated : List.of(NexusScreens.Screen.LEVEL, NexusScreens.Screen.BUILD,
                NexusScreens.Screen.GEAR, NexusScreens.Screen.SETTINGS)) {
            assertEquals(1, ungated.unlockLevel(), ungated + " has no gate, as its hub button has none");
        }
    }

    /**
     * An undeclared node defaults to OP -- the yml's own warning -- so a command whose node is missing is
     * silently locked to operators. Each node must be declared, and followed within its block by default: true.
     */
    @Test
    void everyScreenCommandsNodeIsDeclaredForEveryone() throws IOException {
        List<String> yml = Files.readAllLines(PLUGIN_YML, StandardCharsets.UTF_8);
        assertTrue(yml.stream().anyMatch(l -> l.trim().equals("rpg.command.menu:")), "CONTROL: /menu's node is found");
        for (NexusScreens.Screen screen : NexusScreens.Screen.values()) {
            int at = -1;
            for (int i = 0; i < yml.size(); i++) {
                if (yml.get(i).trim().equals(screen.permission() + ":")) {
                    assertEquals(-1, at, screen.permission() + " is declared once");
                    at = i;
                }
            }
            assertTrue(at >= 0, screen.permission() + " is declared in paper-plugin.yml");
            assertEquals("default: true", yml.get(at + 2).trim(), screen.permission() + " is for everyone");
        }
    }

    /** The command class decides nothing about a screen: it opens none itself and copies no gate. */
    @Test
    void theCommandClassOpensScreensOnlyThroughNexusScreens() throws IOException {
        String source = Files.readString(COMMANDS, StandardCharsets.UTF_8);
        assertTrue(source.contains("NexusScreens.open("), "CONTROL: the opener is called");
        assertTrue(source.contains("NexusScreens.refusal("), "and the refusal asked");
        assertEquals(-1, source.indexOf("Menu("), "no screen is constructed here");
        assertEquals(-1, source.indexOf("unlockLevel"), "no gate is read here");
    }
}
