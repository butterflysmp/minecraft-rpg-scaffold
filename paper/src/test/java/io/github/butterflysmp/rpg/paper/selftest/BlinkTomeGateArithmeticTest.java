package io.github.butterflysmp.rpg.paper.selftest;

import io.github.butterflysmp.rpg.core.Vec3;
import io.github.butterflysmp.rpg.core.accessory.AccessoryLoreLines;
import io.github.butterflysmp.rpg.core.accessory.Blinks;
import io.github.butterflysmp.rpg.core.weapon.AccessoryDefinition;
import io.github.butterflysmp.rpg.paper.content.AccessoryLoader;
import io.github.butterflysmp.rpg.paper.weapon.AccessoryLore;
import io.github.butterflysmp.rpg.paper.weapon.WeaponItems;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * GATE-blink-tome's predicted figures, EXECUTED (2026-11-26): the HELD line through the instrument's own formatter, the
 * sheet lines, and the row figures the BLINK line carries (the trigger's distance, from=, the box to= must lie in).
 */
class BlinkTomeGateArithmeticTest {

    private static AccessoryDefinition tome() {
        return new AccessoryLoader(Logger.getLogger("t"), 100.0)
                .loadAll(new File("src/main/resources/content/accessories"), id -> false)
                .find("blink_tome").orElseThrow();
    }

    /** BT1: the HELD line's name and lore, whole, as SelfTestLines.styled prints them. Predicted before it was run. */
    @Test
    void theHeldLine() {
        AccessoryDefinition tome = tome();
        String name = SelfTestLines.styled(WeaponItems.displayName(tome.displayName(), tome.rarity()));
        List<String> lore = new ArrayList<>();
        for (Component c : AccessoryLore.build(tome)) lore.add(SelfTestLines.styled(c));
        assertEquals("\"Blink Tome\" [blue]", name);
        assertEquals("[\"Blink\" [gold]; \"When a hostile mob targeting you comes within 5 blocks,\" [gray]; "
                + "\"you teleport to safety and turn invisible for 8s.\" [gray]; \"Attacking ends the invisibility.\" [gray]; "
                + "\"Cooldown: 60s\" [gray]; \"Mage class accessory\" [gray]; \"\" []; \"Rare Accessory\" [blue]]",
                "[" + String.join("; ", lore) + "]");
    }

    /** BT2 and BT10: the stats sheet's accessory lines. */
    @Test
    void theSheetLines() {
        AccessoryDefinition tome = tome();
        assertEquals("Blink Tome: Blink", AccessoryLoreLines.sheetLine("Blink Tome", tome, true, "Magic"));
        assertEquals("Blink Tome: inactive (requires Mage)", AccessoryLoreLines.sheetLine("Blink Tome", tome, false, "Magic"));
    }

    /**
     * BT2: the BLINK line's dist= is the trigger's feet to his, 3.000; from= is the stand + (20, 0, 0); every to= is a
     * block centre on the pad's floor, at least 5 from the trigger and every blocker, within 16 of from=. The enumeration
     * of every choosable to= is BlinkPadLandingTest's; here the three figures the line prints are executed.
     */
    @Test
    void theBlinkLinesFigures() {
        Vec3 from = BlinkPadLandingTest.FROM;
        Vec3 trigger = BlinkPadLandingTest.at(BlinkPadLandingTest.TRIGGER);
        assertEquals(3.0, Math.sqrt(from.distanceSquared(trigger)), 0.0, "dist=3.000");
        Blinks.Search s = Blinks.search(BlinkPadLandingTest.BLINK, from, BlinkPadLandingTest.WORST,
                BlinkPadLandingTest.hostiles(BlinkPadLandingTest.TRIGGER));
        double nearest = Double.MAX_VALUE;
        double farthest = 0;
        for (Blinks.Spot sp : s.safe()) {
            nearest = Math.min(nearest, Math.sqrt(sp.feet().distanceSquared(trigger)));
            farthest = Math.max(farthest, Math.sqrt(sp.feet().distanceSquared(from)));
            assertEquals(0.5, sp.feet().x() - Math.floor(sp.feet().x()), 0.0, "a block centre");
        }
        assertTrue(nearest > 5.0, "every to= is more than 5 from the trigger: " + nearest);
        assertTrue(farthest <= 16.0, "every to= is within 16 of from=: " + farthest);
    }
}
