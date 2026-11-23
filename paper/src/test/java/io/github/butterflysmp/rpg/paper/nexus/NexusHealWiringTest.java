package io.github.butterflysmp.rpg.paper.nexus;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE HEAL'S WIRING, READ FROM SOURCE (the seat's ruling M, 2026-11-22). The decision is core's {@code NexusHeal}; what no
 * unit test can drive is the beat and the inventory, so the paper half is pinned here: it runs BEFORE the mirror's
 * redraw, with the join path's guard (an absent profile reads as on) and the join path's converge call.
 */
class NexusHealWiringTest {

    private static final String P = "src/main/java/io/github/butterflysmp/rpg/paper/";

    private static String source(String file) throws IOException {
        return Files.readString(Path.of(P + file), StandardCharsets.UTF_8).replace("\r", "");
    }

    @Test
    void theBeatHealsBeforeItRedraws() throws IOException {
        String plugin = source("RpgPlugin.java");
        int bind = plugin.indexOf("this.statsBar.bindBeat(player -> {");
        assertTrue(bind > 0, "the beat hook is not found");
        String hook = plugin.substring(bind, plugin.indexOf("});", bind));
        int heal = hook.indexOf("NexusHealing.healIfMissing(player, adapters.keys(), profiles,");
        int redraw = hook.indexOf("NexusMirror.redraw(player, adapters.keys());");
        assertTrue(heal > 0 && redraw > heal, "heal at " + heal + ", redraw at " + redraw);
    }

    @Test
    void theHealUsesTheJoinPathsGuardAndCall() throws IOException {
        String healing = source("nexus/NexusHealing.java");
        int at = healing.indexOf("public static void healIfMissing(");
        assertTrue(at > 0, "healIfMissing is not found");
        String body = healing.substring(at, healing.indexOf("\n    }\n", at));
        assertTrue(body.contains("profile.map(PlayerProfile::starEnabled).orElse(true);"), "absent reads as on");
        assertTrue(body.contains("if (!NexusHeal.heals(starOn, found, !player.isDead())) return;"), body);
        assertTrue(body.contains("profile.map(PlayerProfile::nexusSlot).orElse(NexusLock.DEFAULT_LOCKED_SLOT);"), body);
        assertTrue(body.contains("NexusSlots.converge(player, keys, slot);"), body);

        String join = source("listener/RpgListeners.java");
        assertTrue(join.contains("if (profile.map(PlayerProfile::starEnabled).orElse(true)) {"), "CONTROL: the join path's guard");
        assertTrue(join.contains("profile.map(PlayerProfile::nexusSlot).orElse(NexusLock.DEFAULT_LOCKED_SLOT));"),
                "CONTROL: the join path's slot");
    }

    /** The count looks in EVERY slot, so a star moved by hold_only (or worn, or in the off hand) is found. */
    @Test
    void theCountLooksInEverySlot() throws IOException {
        String healing = source("nexus/NexusHealing.java");
        int at = healing.indexOf("static int countNexus(PlayerInventory inventory, Keys keys) {");
        assertTrue(at > 0);
        String body = healing.substring(at, healing.indexOf("\n    }\n", at));
        assertTrue(body.contains("for (int index = 0; index < inventory.getSize(); index++) {"), body);
        assertTrue(body.contains("if (NexusItems.isNexus(inventory.getItem(index), keys)) found++;"), body);
    }
}
