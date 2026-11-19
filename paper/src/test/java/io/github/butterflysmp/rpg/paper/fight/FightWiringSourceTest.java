package io.github.butterflysmp.rpg.paper.fight;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * F6 + F7's wiring sits behind real entities and real events, so no unit test runs it. This pins it from the source
 * (the shape of {@code FightPortSourceTest}), comments stripped first, and each method bounded STRUCTURALLY (from its
 * signature to the next member's), never by a line count:
 * <ul>
 *   <li>the gate is asked at the store's one entry BEFORE the store, and a refusal returns;</li>
 *   <li>a refused vanilla hit is cancelled at LOWEST; the fight's crystals' damage is cancelled at LOWEST;</li>
 *   <li>the Mage and every crystal are NOT persisted, and a crystal is invulnerable.</li>
 * </ul>
 */
class FightWiringSourceTest {

    private static final Path MAIN = Path.of("src/main/java/io/github/butterflysmp/rpg/paper");

    private static String code(String relative) throws IOException {
        String src = Files.readString(MAIN.resolve(relative));
        src = src.replaceAll("(?s)/\\*.*?\\*/", "");
        return src.replaceAll("//[^\\n]*", "");
    }

    /** The text from {@code signature} to the next {@code @EventHandler}, {@code @Override} or method declaration. */
    private static String body(String code, String signature) {
        int from = code.indexOf(signature);
        assertTrue(from >= 0, "found: " + signature);
        int to = code.length();
        for (String next : new String[] {"@EventHandler", "@Override", "\n    public ", "\n    private ", "\n    void "}) {
            int i = code.indexOf(next, from + signature.length());
            if (i >= 0 && i < to) to = i;
        }
        return code.substring(from, to);
    }

    @Test
    void theGateIsAskedBeforeTheStoreAndARefusalReturns() throws IOException {
        String code = code("adapter/BukkitCombatant.java");
        int gate = code.indexOf("ctx.gate().refuses(entity.getUniqueId(), amount,");
        int store = code.indexOf("ctx.stats().damage(entity.getUniqueId(), amount");
        assertTrue(gate > 0 && store > 0, "both present: gate=" + gate + " store=" + store);
        assertTrue(gate < store, "the gate is asked before the store");
        String between = code.substring(gate, store);
        assertTrue(between.matches("(?s).*\\)\\)\\s*\\{\\s*return;\\s*}.*"), "and a refusal returns at once");
    }

    @Test
    void aRefusedVanillaHitIsCancelledAtLowest() throws IOException {
        String code = code("listener/RpgListeners.java");
        String handler = body(code, "public void onGatedDamage(EntityDamageEvent event)");
        int at = code.indexOf("public void onGatedDamage(EntityDamageEvent event)");
        String annotation = code.substring(code.lastIndexOf("@EventHandler", at), at);
        assertTrue(annotation.contains("EventPriority.LOWEST"), annotation);
        assertTrue(handler.contains("adapters.gate().refuses(") && handler.contains("event.setCancelled(true);"), handler);
    }

    @Test
    void theFightsCrystalsAreUnbreakableAtLowest() throws IOException {
        String code = code("listener/RpgListeners.java");
        String handler = body(code, "public void onFightCrystalDamage(EntityDamageEvent event)");
        int at = code.indexOf("public void onFightCrystalDamage(EntityDamageEvent event)");
        assertTrue(code.substring(code.lastIndexOf("@EventHandler", at), at).contains("EventPriority.LOWEST"));
        assertTrue(handler.contains("adapters.keys().fightCrystal") && handler.contains("event.setCancelled(true);"),
                handler);
        String port = body(code("fight/BukkitFightPort.java"), "public void lightCrystal(int index)");
        assertTrue(port.contains("c.setInvulnerable(true);"), "the backstop");
        assertTrue(port.contains("adapters.keys().fightCrystal"), "marked, so only the fight's crystals are protected");
    }

    @Test
    void theMageAndEveryCrystalAreNotPersisted() throws IOException {
        assertTrue(body(code("fight/BukkitFightPort.java"), "public void lightCrystal(int index)")
                .contains("c.setPersistent(false);"), "each crystal");
        assertTrue(body(code("fight/FightInstance.java"), "private void spawnMage(double x, double y, double z)")
                .contains("e.setPersistent(false);"), "the Mage");
    }
}
