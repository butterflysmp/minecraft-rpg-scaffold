package io.github.butterflysmp.rpg.paper.health;

import org.bukkit.inventory.EquipmentSlot;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The part of the Mana Battery scan a unit test can reach (B2, 2026-11-27): {@code desiredModifiers} needs a live
 * {@code Player} and is boot-witnessed, like every scanner here (GATE-mana-battery MB2 to MB6). So its keys, and the
 * three lines of its body that carry the rulings, read off the source.
 *
 * <p><b>Unlike Mana Bank's prefix, this one guards a LIVE collision</b>: the {@code mana_regen_boost_TEMP} fixture
 * already feeds the same target, and the two maps are merged into one reconcile, so equal keys would keep only one.
 */
class ManaBatteryModifierItemsTest {

    private static List<String> code() throws IOException {
        return Files.readAllLines(Path.of("src", "main", "java", "io", "github", "butterflysmp", "rpg", "paper", "health",
                        "ManaBatteryModifierItems.java"), StandardCharsets.UTF_8).stream()
                .filter(l -> !(l.trim().startsWith("//") || l.trim().startsWith("*") || l.trim().startsWith("/*"))).toList();
    }

    @Test
    void everyKey_isDisjointFromTheFixturesAndFromABareSlot() {
        assertFalse(ManaBatteryModifierItems.SOURCE_PREFIX.isEmpty());
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            String key = ManaBatteryModifierItems.SOURCE_PREFIX + slot.name();
            assertNotEquals(slot.name(), key);
            assertNotEquals(ManaRegenModifierItems.SOURCE_PREFIX + slot.name(), key, "the TEMP fixture shares the target");
            assertNotEquals(ManaBankModifierItems.SOURCE_PREFIX + slot.name(), key);
        }
        // Mutation: SOURCE_PREFIX = ManaRegenModifierItems.SOURCE_PREFIX -> reddens.
    }

    /** WORN, not carried: it walks the four armor slots, never every slot (the TEMP fixture walks every slot). */
    @Test
    void itReadsTheWornPiecesOnly_byEffect_gatedOnBoosts() throws IOException {
        List<String> c = code();
        assertEquals(1, c.stream().filter(l -> l.contains("for (EquipmentSlot slot : DefenseModifierItems.ARMOR_SLOTS) {"))
                .count(), "the four armor slots: a piece in his inventory or hand is never read");
        assertEquals(0, c.stream().filter(l -> l.contains("EquipmentSlot.values()")).count(), "never every slot");
        assertTrue(c.stream().anyMatch(l -> l.contains("EnchantEffect.MANA_REGEN);")), "bound by effect");
        assertTrue(c.stream().anyMatch(l -> l.contains("if (ManaRegen.boosts(bonus)) {")),
                "absent, not zeroed: no source written for no bonus (a source every pass would pin the pool every pass)");
        // C2 (2026-11-27): authored per FIVE SECONDS, converted by ManaRegen's one conversion BEFORE the one seam a
        // mana-regen bonus becomes a modifier. Mutation: the conversion skipped -> reddens.
        assertTrue(c.stream().anyMatch(l -> l.contains(
                        "desired.put(SOURCE_PREFIX + slot.name(), ManaRegen.contribution(ManaRegen.perSecondFromPerFiveSeconds(bonus)));")),
                "the one conversion, then the one seam");
        assertEquals(List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET),
                List.of(DefenseModifierItems.ARMOR_SLOTS), "the control: ARMOR_SLOTS is the four worn slots, no hand");
    }
}
