package io.github.butterflysmp.rpg.paper.menu;

import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * *** THE HOOK'S DEFAULT LEAVES EVERY EXISTING MENU'S ROUTING IDENTICAL. The row the
 * "add the hook to an existing menu" and "add the armour slots to inputSlots()" mutations redden. ***
 *
 * <p>No {@link Menu} can be built in a unit test (its constructor calls {@code Bukkit.createInventory};
 * there is no MockBukkit), so the proof is structural, and it is the seat's ruling: the
 * {@code MenuRouting} diff is confined to the one line where the from-player shift found no input
 * slot and returned null, and that line now asks {@code shiftInElsewhere} first and falls through to
 * the same {@code return null} when it declines. <b>So a menu that does not override the hook routes
 * exactly as before</b> -- and this test proves which menus override it: every compiled {@code Menu}
 * subclass is loaded WITHOUT initialisation and inspected, and only {@link EquipmentMenu} declares it.
 *
 * <p>The scan walks the compiled classes directory rather than a list, so a tenth menu is found, not
 * assumed. A control asserts it finds a known subclass.
 */
class ShiftInHookTest {

    private static final String HOOK = "shiftInElsewhere";

    private static List<Class<?>> allMenuSubclasses() throws IOException, URISyntaxException {
        Path root = Path.of(Menu.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        List<Class<?>> found = new ArrayList<>();
        try (Stream<Path> files = Files.walk(root)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".class")).toList()) {
                String name = root.relativize(file).toString()
                        .replace('\\', '/').replace('/', '.').replaceAll("\\.class$", "");
                Class<?> type;
                try {
                    type = Class.forName(name, false, ShiftInHookTest.class.getClassLoader());
                } catch (ClassNotFoundException | LinkageError e) {
                    continue;   // a class that cannot even be linked without the server is no Menu we can see
                }
                if (Menu.class.isAssignableFrom(type) && type != Menu.class) found.add(type);
            }
        }
        return found;
    }

    private static boolean declaresHook(Class<?> type) {
        for (Method m : type.getDeclaredMethods()) {
            if (m.getName().equals(HOOK)) return true;
        }
        return false;
    }

    @Test
    void onlyTheEquipmentMenuOverridesTheHook_everyOtherMenuKeepsTheDefault() throws Exception {
        List<Class<?>> menus = allMenuSubclasses();
        Set<String> names = new TreeSet<>();
        for (Class<?> m : menus) names.add(m.getSimpleName());
        assertTrue(names.contains("AnvilMenu"), "control: the scan found a known subclass -- " + names);
        assertEquals(Set.of("AnvilMenu", "CraftingMenu", "EnchantMenu", "GrindstoneMenu", "NexusMenu",
                        "NexusSlotPickerMenu", "SettingsMenu", "RecipeBrowserMenu", "NexusVaultMenu",
                        "EquipmentMenu", "BuildMenu", "BuildPickerMenu"), names,
                "the nine existing menus, the Equipment screen, and the Build screen and its picker (which keep"
                        + " the default: they hold nothing, so a shift-in moves nothing) -- a new menu must be"
                        + " added here with its verdict");

        for (Class<?> m : menus) {
            boolean expected = m == EquipmentMenu.class;
            assertEquals(expected, declaresHook(m),
                    m.getSimpleName() + (expected ? " must override " : " must NOT override ") + HOOK);
        }
        // Mutation: add a shiftInElsewhere override to any existing menu -> reddens.
    }

    @Test
    void theDefaultDeclinesAndIsNotFinal_soTheSubclassOverrideIsTheOnlyPath() throws Exception {
        Method hook = Menu.class.getDeclaredMethod(HOOK, ItemStack.class, Consumer.class);
        assertTrue(Modifier.isProtected(hook.getModifiers()));
        assertEquals(boolean.class, hook.getReturnType());
        // The default's body cannot be CALLED without a Menu instance; its source is the proof, and it
        // is the one-line `return false;` the seat reviewed. Read it, so a change to it reddens here.
        String source = Files.readString(Path.of(
                "src/main/java/io/github/butterflysmp/rpg/paper/menu/Menu.java"));
        int at = source.indexOf("protected boolean " + HOOK + "(");
        assertTrue(at > 0, "the hook is declared in Menu.java");
        int open = source.indexOf('{', at);
        int close = source.indexOf('}', open);
        assertEquals("return false;", source.substring(open + 1, close).trim(),
                "the default must decline: anything else changes routing for every menu that does not override it");
        // Mutation: make the hook's default `return true;` -> reddens.
    }

    @Test
    void theEquipmentMenuHasNoInputSlots_inTheSource() throws Exception {
        // inputSlots() is what makes the number key, F, drag and double-click refuse by construction.
        // The method body cannot be called without a server, so its source is asserted.
        String source = Files.readString(Path.of(
                "src/main/java/io/github/butterflysmp/rpg/paper/menu/EquipmentMenu.java"));
        int at = source.indexOf("protected Set<Integer> inputSlots()");
        assertTrue(at > 0);
        int open = source.indexOf('{', at);
        int close = source.indexOf('}', open);
        assertEquals("return Set.of();", source.substring(open + 1, close).trim(),
                "no armour or accessory slot may be an input slot");
        // Mutation: add the armour slots to inputSlots() -> reddens.
    }
}
