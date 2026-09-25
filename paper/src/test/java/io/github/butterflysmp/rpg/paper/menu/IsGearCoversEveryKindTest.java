package io.github.butterflysmp.rpg.paper.menu;

import io.github.butterflysmp.rpg.core.weapon.AccessoryDefinition;
import io.github.butterflysmp.rpg.core.weapon.ArmorDefinition;
import io.github.butterflysmp.rpg.core.weapon.GearDefinition;
import io.github.butterflysmp.rpg.core.weapon.ShieldDefinition;
import io.github.butterflysmp.rpg.core.weapon.ToolDefinition;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * *** {@code CraftMatrixScreen.isGear} ASKS EVERY GEAR KIND'S KEY. The row the isGear mutation must
 * redden. ***
 *
 * <p>{@code isGear}'s own javadoc: <i>"A KEY MISSING FROM THIS CHAIN IS A HOLE ... nothing here fails
 * to compile when the chain falls behind GearDefinition -- there is no switch, no sealed type and no
 * test that can see the omission."</i> This is that test. It walks {@code GearDefinition}'s PERMITTED
 * SUBCLASSES at runtime, so a sixth kind is a red here until it is mapped below -- and then a red again
 * until {@code isGear} asks its key.
 *
 * <p>A missing arm is not cosmetic: the crafting surface would consume the item as an ingredient,
 * and {@code QuiverAmmo}'s planning pass would not skip it.
 *
 * <p>The body is bounded STRUCTURALLY -- from the declaration to the first line that is exactly
 * {@code }} at the method's indentation -- never by a line count (the proximity-bound defect).
 */
class IsGearCoversEveryKindTest {

    private static final Path SCREEN = Path.of("src/main/java/io/github/butterflysmp/rpg/paper/menu/CraftMatrixScreen.java");

    /** The id reader each gear kind is identified by. */
    private static final Map<Class<?>, String> READER = Map.of(
            WeaponDefinition.class, "WeaponItems.weaponId(",
            ShieldDefinition.class, "ShieldItems.shieldId(",
            ArmorDefinition.class, "ArmorItems.armorId(",
            ToolDefinition.class, "ToolItems.toolId(",
            AccessoryDefinition.class, "AccessoryItems.accessoryId(");

    @Test
    void everyPermittedGearKindHasAnArmInIsGear() throws IOException {
        Set<Class<?>> permitted = Arrays.stream(GearDefinition.class.getPermittedSubclasses())
                .collect(Collectors.toSet());
        assertEquals(READER.keySet(), permitted,
                "a gear kind was added or removed: map its id reader here, and give isGear its arm");

        String body = isGearBody();
        assertTrue(body.contains("WeaponItems.weaponId("), "control: the scan found isGear's body");
        for (Map.Entry<Class<?>, String> kind : READER.entrySet()) {
            assertTrue(body.contains(kind.getValue()),
                    kind.getKey().getSimpleName() + " has no arm in isGear -- the crafting grid would eat it");
        }
        // Mutation: delete the AccessoryItems.accessoryId arm from isGear -> reddens.
    }

    private static String isGearBody() throws IOException {
        List<String> lines = Files.readAllLines(SCREEN, StandardCharsets.UTF_8);
        int start = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).contains("public static boolean isGear(")) { start = i; break; }
        }
        assertTrue(start >= 0, "isGear must exist");
        int end = start;
        while (end < lines.size() && !lines.get(end).equals("    }")) end++;
        assertTrue(end < lines.size(), "isGear's closing brace must be found");
        return String.join("\n", lines.subList(start, end + 1));
    }
}
