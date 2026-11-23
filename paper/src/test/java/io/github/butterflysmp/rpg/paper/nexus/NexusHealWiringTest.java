package io.github.butterflysmp.rpg.paper.nexus;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE HEAL'S WIRING, READ FROM SOURCE (the seat's ruling M, 2026-11-22). The decision is core's {@code NexusHeal}; what no
 * unit test can drive is the beat and the inventory, so the paper half is pinned here: it runs BEFORE the mirror's
 * redraw, with the join path's converge call, and only on a LOADED profile (the seat's correction, 2026-11-22: the join
 * path's "absent reads as on" holds only after the load settles, and the beat can run before it).
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
    void theHealUsesTheJoinPathsCallOnALoadedProfileOnly() throws IOException {
        String healing = source("nexus/NexusHealing.java");
        int at = healing.indexOf("public static void healIfMissing(");
        assertTrue(at > 0, "healIfMissing is not found");
        String body = healing.substring(at, healing.indexOf("\n    }\n", at));
        assertTrue(body.contains("boolean loaded = profile.isPresent();"), "loaded is the profile being present");
        assertTrue(body.contains("profile.map(PlayerProfile::starEnabled).orElse(false);"), "absent never reads as on here");
        assertFalse(body.contains(".orElse(true)"), "THE SEAT'S ERROR: an absent profile read as on, before the load");
        assertTrue(body.contains("if (!NexusHeal.heals(loaded, starOn, found, !player.isDead())) return;"), body);
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
