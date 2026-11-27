package io.github.butterflysmp.rpg.paper.menu;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * W4 (the seat, 2026-11-27): <b>AN ACCESSORY EQUIP AND AN UNEQUIP THROUGH THE EQUIPMENT MENU EACH WRITE ONE LOG LINE</b>
 * -- who, slot, item, tick -- so a gate can read the moment the store changed instead of inferring it from the next
 * reconcile. One line per gesture, written when the store ACCEPTS the write (the click's tick, the moment the stored
 * slot changes); a refused write logs nothing, and the player is told as before.
 */
class EquipmentMenuAccessoryLogTest {

    private static String menu() throws IOException {
        return Files.readString(Path.of("src/main/java/io/github/butterflysmp/rpg/paper/menu/EquipmentMenu.java"))
                .replace("\r", "");
    }

    /** The line, word for word, every field distinct so a transposition cannot pass. */
    @Test
    void theLineIsWordForWord() {
        assertEquals("ACCESSORY menu equip player=Ben slot=2 item=mending_charm tick=4021",
                EquipmentMenu.accessoryLine("equip", "Ben", 2, "mending_charm", 4021));
        assertEquals("ACCESSORY menu unequip player=Neb slot=0 item=healing_tome tick=77",
                EquipmentMenu.accessoryLine("unequip", "Neb", 0, "healing_tome", 77));
        // Mutation: drop the tick field, or swap slot and tick -> reddens.
    }

    /**
     * Each of the three write paths logs once, and only on the accepted branch. Mutation: the call moved before the
     * write (it would log a refused write), or removed from any path.
     */
    @Test
    void eachPathLogsOnce_onlyWhenTheWriteIsAccepted() throws IOException {
        String m = menu();
        assertTrue(m.length() > 10_000, "control: EquipmentMenu was read");

        int cursor = m.indexOf("private void equipFromCursor(int index, ItemStack cursor) {");
        String cursorBody = m.substring(cursor, m.indexOf("\n    }\n", cursor));
        assertTrue(cursorBody.contains("} else {\n            logAccessory(\"equip\", index, def.get().id());\n        }"),
                cursorBody);

        int shift = m.indexOf("private boolean accessoryShiftIn(ItemStack moving, Consumer<ItemStack> setSource) {");
        String shiftBody = m.substring(shift, m.indexOf("\n    }\n", shift));
        int refused = shiftBody.indexOf("setSource.accept(taken);");
        int logged = shiftBody.indexOf("logAccessory(\"equip\", target, def.get().id());");
        assertTrue(refused > 0 && logged > refused, "logged after the refused branch returns: " + shiftBody);

        int un = m.indexOf("private void unequip(int index, String text, boolean toCursor) {");
        String unBody = m.substring(un, m.indexOf("\n    }\n", un));
        assertTrue(unBody.contains("if (accepted) logAccessory(\"unequip\", index,"), unBody);

        assertEquals(3, m.split("logAccessory\\(\"", -1).length - 1, "three call sites: two equips, one unequip");
        assertTrue(m.contains("adapters.log().info(accessoryLine(verb, viewer.getName(), index, item, Bukkit.getCurrentTick()));"),
                "the line goes to the server log, with the server tick");
    }
}
