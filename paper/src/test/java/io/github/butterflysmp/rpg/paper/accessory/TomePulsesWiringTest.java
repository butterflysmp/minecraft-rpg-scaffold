package io.github.butterflysmp.rpg.paper.accessory;

import io.github.butterflysmp.rpg.core.accessory.AccessoryBehaviour;
import io.github.butterflysmp.rpg.core.accessory.AccessoryContributions.ActiveBehaviour;
import io.github.butterflysmp.rpg.core.accessory.AccessoryRefusals;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Healing Pulse's wiring (H3, H4, H6, H10) and the book's guards (H12). The pulse's DECISION is core's table
 * ({@code HealingPulsesTest}); what can only be read here is which function each piece calls, and the line formats. A
 * Bukkit-free test cannot run a tick, so the clock's behaviour is witnessed by the gate; these rows pin its shape.
 */
class TomePulsesWiringTest {

    private static final String P = "src/main/java/io/github/butterflysmp/rpg/paper/";

    private static String source(String file) throws IOException {
        return Files.readString(Path.of(P + file), StandardCharsets.UTF_8).replace("\r", "");
    }

    private static String body(String file, String signature) throws IOException {
        String src = source(file);
        int at = src.indexOf(signature);
        assertTrue(at > 0, file + ": '" + signature + "' is not found -- the scan is blind");
        int end = src.indexOf("\n    }\n", at);
        assertTrue(end > at, file + ": no closing brace after '" + signature + "'");
        return src.substring(at, end);
    }

    private static String annotationOf(String file, String signature) throws IOException {
        String src = source(file);
        int at = src.indexOf(signature);
        assertTrue(at > 0, signature + " is not found");
        return src.substring(src.lastIndexOf("@EventHandler", at), at);
    }

    private static final AccessoryBehaviour PULSE = new AccessoryBehaviour.HealingPulse("Radiant Pulse", 1, 80, 8,
            AccessoryBehaviour.Targets.PLAYERS, null, 0, null, null);

    /** H10: the two line formats, whole. */
    @Test
    void theTraceLines() {
        assertEquals("TOMEPULSE wearer=Ben level=2 tick=12345 candidates=1 healed=0",
                TomePulses.pulseLine("Ben", 2, 12345, 1, 0));
        assertEquals("TOMEPULSE target=Ben before=98.000 amount=2.000 after=100.000",
                TomePulses.targetLine("Ben", 98.0, 2.0, 100.0));
    }

    @Test
    void pulseOfFindsTheHealingPulse_andNothingElse() {
        assertTrue(TomePulses.pulseOf(List.of()).isEmpty());
        ActiveBehaviour tome = new ActiveBehaviour(0, "healing_tome", PULSE);
        assertEquals(tome, TomePulses.pulseOf(List.of(tome)).orElseThrow());
    }

    /**
     * H3: the clock is driven by the reconcile pass's ONE reading (the variable every stat merge uses), and re-checked at
     * the pulse through the same function. Nothing in TomePulses reads a slot or the store.
     */
    @Test
    void theClockReadsTheReconcilesOwnReading_neverTheSlot() throws IOException {
        String loop = body("health/PlayerHealthSystem.java", "private void startReconcileLoop(Player player) {");
        int reading = loop.indexOf("AccessoryContributions fromAccessories = accessoryContributions(id);");
        int clock = loop.indexOf("tomePulses.reconcile(player, fromAccessories.behaviours());");
        assertTrue(reading > 0 && clock > reading, "the clock reads the pass's own reading, after it is taken");
        assertTrue(body("health/PlayerHealthSystem.java", "public AccessoryContributions accessoryContributionsFor(UUID id) {")
                .contains("return accessoryContributions(id);"), "the re-check is the same function");
        String tome = source("accessory/TomePulses.java");
        assertTrue(tome.contains("pulseOf(contributions.apply(id).behaviours())"), "the pulse-time re-check");
        for (String forbidden : List.of(".service()", ".worn(", "accessories(", "VaultCodec", "AccessorySlots.contributes")) {
            assertFalse(tome.contains(forbidden), "TomePulses must not read the slot itself: " + forbidden);
        }
        assertTrue(source("RpgPlugin.java").contains("healthSystem::accessoryContributionsFor"), "bound to that function");
        // Mutation: delete the tomePulses.reconcile line -> reddens.
    }

    /** H4: quit stops it; death stops it through the entity task; the first pulse is one interval after the start. */
    @Test
    void theClockStopsOnQuitAndDeath_andStartsOneIntervalOut() throws IOException {
        assertTrue(body("health/PlayerHealthSystem.java", "public void onQuit(UUID id) {")
                .contains("tomePulses.onQuit(id);"));
        String start = body("accessory/TomePulses.java", "private void start(Player player, ActiveBehaviour active) {");
        assertTrue(start.contains("new EntityTaskTarget(player, scheduler, log)"), "inactive while dead: stops");
        assertTrue(start.contains("RepeatingTask.start(target, pulse.intervalTicks(), \"tome-pulse\""));
        String reconcile = body("accessory/TomePulses.java", "public void reconcile(Player player, List<ActiveBehaviour> active) {");
        assertTrue(reconcile.contains("if (pulse.isEmpty() || player.isDead())"), "inactive or dead: stop, never start");
        assertTrue(reconcile.contains("if (current != null && current.task().isRunning()) return;"),
                "one task per wearer: a running clock is never restarted (no re-anchor while it contributes)");
        // READ: RepeatingTask.start arms its first step one period out (arm() -> scheduleTick(periodTicks, step)).
        assertTrue(source("scheduler/RepeatingTask.java").replace(" ", "").contains("target.scheduleTick(periodTicks,this::step);"));
    }

    /** H6: the one heal path, the wearer as dealer; others on their own thread; never the dead; never past max. */
    @Test
    void theHealIsTheEnginesOnePath_onEachTargetsThread() throws IOException {
        String apply = body("accessory/TomePulses.java",
                "private void apply(Player target, UUID wearerId, HealingPulses.Heal h, AccessoryBehaviour.HealingPulse pulse) {");
        assertTrue(apply.contains("stats.heal(id, amount, wearerId, true);"));
        assertTrue(apply.contains("if (!target.isValid() || target.isDead() || !stats.tracks(id)) return;"));
        assertTrue(apply.contains("double amount = Math.min(h.amount(), stats.max(id) - before);"));
        String pulse = body("accessory/TomePulses.java", "private void pulse(Player wearer, AccessoryBehaviour.HealingPulse pulse) {");
        assertTrue(pulse.contains("scheduler.onEntity(target, () -> apply(target, wearerId, h, pulse));"));
        assertTrue(pulse.contains("PlayerLevel.effectiveLevel(profile.get().lifetimeXp())"), "L is the WEARER'S level");
        assertFalse(source("accessory/TomePulses.java").contains("resources"), "no mana is read or spent");
        assertTrue(pulse.indexOf("log.info(pulseLine(") < pulse.indexOf("for (HealingPulses.Heal h : d.heals())"),
                "the pulse line is written before any heal, so a pulse that heals nobody still writes it");
    }

    /** H12: the three guards are registered, run first, and refuse with the words. */
    @Test
    void theBookGuardsAreWired() throws IOException {
        String l = "listener/RpgListeners.java";
        String trade = "public void onAccessoryTrade(io.papermc.paper.event.player.PlayerPurchaseEvent event) {";
        assertTrue(annotationOf(l, trade).contains("priority = EventPriority.LOWEST"));
        assertTrue(body(l, trade).contains("AccessoryVanillaUses.refusesTrade("));
        assertTrue(body(l, trade).contains("event.setCancelled(true);"));
        assertTrue(body(l, trade).contains("m.getItem(0)") && body(l, trade).contains("m.getItem(1)"), "both cost slots");
        String shelf = "public void onAccessoryShelved(PlayerInteractEvent event) {";
        assertTrue(annotationOf(l, shelf).contains("priority = EventPriority.LOWEST"));
        assertTrue(body(l, shelf).contains("org.bukkit.Material.CHISELED_BOOKSHELF"));
        assertTrue(body(l, shelf).contains("event.setCancelled(true);"));
        String hopper = "public void onAccessoryHoppered(org.bukkit.event.inventory.InventoryMoveItemEvent event) {";
        assertTrue(body(l, hopper).contains("InventoryType.CHISELED_BOOKSHELF"));
        assertTrue(body(l, hopper).contains("event.setCancelled(true);"));
        assertEquals("Accessories cannot be traded.", AccessoryRefusals.TRADE);
        assertEquals("Accessories cannot be shelved.", AccessoryRefusals.SHELF);
    }

    /** H12's rules, as rows: our accessory is refused; a plain book is not. */
    @Test
    void theGuardRows() {
        assertTrue(AccessoryVanillaUses.refusesTrade(true, false));
        assertTrue(AccessoryVanillaUses.refusesTrade(false, true), "the second cost slot too");
        assertFalse(AccessoryVanillaUses.refusesTrade(false, false), "a plain book trades");
        assertTrue(AccessoryVanillaUses.refusesShelf(true, true));
        assertFalse(AccessoryVanillaUses.refusesShelf(true, false), "a plain book shelves");
        assertFalse(AccessoryVanillaUses.refusesShelf(false, true), "any other block is not this guard's");
    }

    /** H13: A4 holds for every accessory, the tome included, with the existing words. */
    @Test
    void a4sWordsAreUnchanged() {
        assertEquals("Accessories cannot be enchanted.", AccessoryRefusals.ENCHANT);
        assertEquals("Accessories cannot be worked at the anvil.", AccessoryRefusals.ANVIL);
        assertEquals("Accessories have nothing to grind off.", AccessoryRefusals.GRINDSTONE);
        assertEquals("Accessories carry no gear score.", AccessoryRefusals.GEAR_SCORE);
        assertEquals("Accessories do not wear out.", AccessoryRefusals.DURABILITY);
    }
}
