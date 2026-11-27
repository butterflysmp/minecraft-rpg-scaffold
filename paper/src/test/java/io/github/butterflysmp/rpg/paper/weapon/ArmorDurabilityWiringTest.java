package io.github.butterflysmp.rpg.paper.weapon;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * D1' to D5' (the seat, 2026-11-25), the paper wiring, by reading the source: every method here needs a live player, so
 * the decisions are pinned in core ({@code ArmorWearTest}) and this pins that paper asks them, ONCE.
 *
 * <ul>
 *   <li>ONE wear per hit: {@code applyWearOnHit} is called from exactly one place, the store's listener, and vanilla's
 *       own armor damage is cancelled for our pieces.</li>
 *   <li>A broken piece: no Defense (the scan), no Growth, no Mana Bank (their scanners), and it still counts toward the
 *       set (the real flag passed to the decision, which ignores it).</li>
 * </ul>
 */
class ArmorDurabilityWiringTest {

    private static final Path MAIN = Path.of("src", "main", "java", "io", "github", "butterflysmp", "rpg", "paper");

    private static List<String> code(Path p) throws IOException {
        return Files.readAllLines(MAIN.resolve(p), StandardCharsets.UTF_8).stream()
                .filter(l -> !(l.trim().startsWith("//") || l.trim().startsWith("*") || l.trim().startsWith("/*"))).toList();
    }

    private static int at(List<String> c, String needle) {
        for (int i = 0; i < c.size(); i++) if (c.get(i).contains(needle)) return i;
        return -1;
    }

    @Test
    void wearIsAppliedFromExactlyOnePlaceTheStoresListener() throws IOException {
        long callers;
        try (Stream<Path> files = Files.walk(MAIN)) {
            callers = files.filter(f -> f.toString().endsWith(".java")).mapToLong(f -> {
                try {
                    return Files.readAllLines(f, StandardCharsets.UTF_8).stream()
                            .filter(l -> !(l.trim().startsWith("//") || l.trim().startsWith("*")))
                            .filter(l -> l.contains("ArmorDurability.applyWearOnHit(")).count();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).sum();
        }
        assertEquals(1, callers, "one trigger, so a hit cannot wear twice");
        List<String> l = code(Path.of("health", "ArmorWearListener.java"));
        int gate = at(l, "if (!ArmorWear.wears(change)) return;");
        int wear = at(l, "scheduler.onEntity(player, () -> ArmorDurability.applyWearOnHit(player, keys, armor, cooldowns.get()));");
        assertTrue(gate > 0 && wear > gate, "core's decision first, then the hop to his own thread");
        List<String> p = code(Path.of("RpgPlugin.java"));
        assertTrue(at(p, "new io.github.butterflysmp.rpg.paper.health.ArmorWearListener(scheduler, keys, armor, () -> cooldowns)") > 0,
                "and the listener is in the store's composite");
    }

    @Test
    void vanillasOwnArmorDamageIsCancelledForOurPieces() throws IOException {
        List<String> l = code(Path.of("listener", "RpgListeners.java"));
        int handler = at(l, "public void onArmorItemDamage(PlayerItemDamageEvent event) {");
        assertTrue(handler > 0, "the handler exists");
        assertTrue(l.get(handler + 1).contains("ArmorDurability.isOurArmor(event.getItem(), adapters.keys(), armor)")
                && l.get(handler + 2).contains("event.setCancelled(true);"), "it cancels for our armor");
        assertTrue(l.get(handler - 1).contains("@EventHandler(ignoreCancelled = true)"), "and it is registered");
    }

    @Test
    void aBrokenPieceGivesNoDefenseNoGrowthNoManaBankAndStillCountsTowardTheSet() throws IOException {
        List<String> d = code(Path.of("health", "DefenseModifierItems.java"));
        int nat = at(d, "nativeArmor += vanilla;");
        int skip = at(d, "if (io.github.butterflysmp.rpg.paper.weapon.ArmorDurability.isBroken(piece)) continue;");
        int put = at(d, "if (contributed > 0) defense.put(slot.name(), contributed);");
        assertTrue(nat > 0 && skip > nat && put > skip,
                "the native sum still counts it (it is worn), then a broken piece contributes no Defense");
        // 2026-11-27: + Mana Battery (the seat's B2: a broken piece gives nothing).
        for (String scanner : List.of("GrowthModifierItems.java", "ManaBankModifierItems.java", "ManaBatteryModifierItems.java")) {
            List<String> g = code(Path.of("health", scanner));
            int loop = at(g, "for (EquipmentSlot slot : DefenseModifierItems.ARMOR_SLOTS) {");
            assertTrue(loop > 0 && g.get(loop + 1).contains(
                    "if (io.github.butterflysmp.rpg.paper.weapon.ArmorDurability.isBroken(equipment.getItem(slot))) continue;"),
                    scanner + ": a broken piece's enchant source is absent, so it is removed as if unequipped");
        }
        List<String> s = code(Path.of("health", "ArmorSetItems.java"));
        assertTrue(at(s, "io.github.butterflysmp.rpg.paper.weapon.ArmorDurability.isBroken(item)));") > 0,
                "the set decision is handed the real broken flag");
    }

    @Test
    void theBrokenPredicateIsTheWeaponsOwn() throws IOException {
        List<String> a = code(Path.of("weapon", "ArmorDurability.java"));
        int m = at(a, "public static boolean isBroken(ItemStack item) {");
        assertTrue(m > 0 && a.get(m + 1).contains("return WeaponDurability.isBroken(item);"), "no third implementation");
    }
}
