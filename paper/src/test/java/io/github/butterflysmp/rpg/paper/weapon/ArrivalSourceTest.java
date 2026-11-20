package io.github.butterflysmp.rpg.paper.weapon;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CONVERT ON ARRIVAL, THE PAPER HALF (the seat's brief, part h). The handlers need a live inventory, so the wiring is
 * pinned from the source, comments stripped, each method bounded STRUCTURALLY. The decision is core's
 * ({@code ArrivalClaimsTest}).
 */
class ArrivalSourceTest {

    private static final Path PAPER = Path.of("src/main/java/io/github/butterflysmp/rpg/paper");

    private static String code(String file) throws IOException {
        String src = Files.readString(PAPER.resolve(file)).replace("\r\n", "\n");
        src = src.replaceAll("(?s)/\\*.*?\\*/", "");
        return src.replaceAll("//[^\\n]*", "");
    }

    private static String body(String code, String signature) {
        int from = code.indexOf(signature);
        assertTrue(from >= 0, "found: " + signature);
        int to = code.length();
        for (String next : new String[] {"@EventHandler", "\n    public ", "\n    private ", "\n    static "}) {
            int i = code.indexOf(next, from + signature.length());
            if (i >= 0 && i < to) to = i;
        }
        return code.substring(from, to);
    }

    private static String listeners() throws IOException {
        return code("listener/RpgListeners.java");
    }

    /** Every live route: the slot-change event, deferred a tick, through the one conversion. */
    @Test
    void theSlotChangeHandlerConvertsAClaimedArrivalOnTheNextTick() throws IOException {
        String h = body(listeners(),
                "public void onInventorySlotChange(io.papermc.paper.event.player.PlayerInventorySlotChangeEvent event)");
        assertTrue(h.contains("if (decision.kind() != io.github.butterflysmp.rpg.core.weapon.ArrivalClaims.Kind.MINT) return;"), h);
        assertTrue(h.contains("adapters.scheduler().onEntity(player, () -> convertArrival(player, slot, decision.weaponId(),"
                + " \"slot-change\"));"), h);
    }

    /** The conversion re-checks the slot, then mints with the roll and the stamp, and logs one ARRIVAL line. */
    @Test
    void theConversionRechecksThenMintsRollsAndStamps() throws IOException {
        String c = body(listeners(), "private void convertArrival(Player player, int slot, String weaponId, String via)");
        int recheck = c.indexOf("if (again.kind() != io.github.butterflysmp.rpg.core.weapon.ArrivalClaims.Kind.MINT"
                + " || !weaponId.equals(again.weaponId())) {");
        int convert = c.indexOf("ArrivalConverter.convert(current, weapon, player, adapters);");
        int set = c.indexOf("player.getInventory().setItem(slot, minted);");
        assertTrue(recheck > 0 && convert > recheck && set > convert, c);
        String m = body(code("weapon/ArrivalConverter.java"),
                "public static ItemStack convert(ItemStack plain, WeaponDefinition weapon, Player player, AdapterContext adapters)");
        int mint = m.indexOf("ItemStack minted = GearItems.mint(weapon, adapters);");
        int wear = m.indexOf("GearItems.carryWear(old, meta, minted.getType())");
        int roll = m.indexOf("EnchantRollItems.rollOnAcquire(minted, weapon, adapters);");
        int stamp = m.indexOf("GearScoreItems.stampOnAcquire(minted, weapon, player, adapters);");
        assertTrue(mint > 0 && wear > mint && roll > wear && stamp > roll, "mint, wear, roll, stamp: " + m);
    }

    /** The backstop: on join, after the refresh. */
    @Test
    void aPlainSwordAlreadyHeldConvertsOnJoinAfterTheRefresh() throws IOException {
        String l = listeners();
        int refresh = l.indexOf("GearRefresher.refresh(event.getPlayer(), weapons, shields, armor, tools, adapters);");
        int sweep = l.indexOf("convertArrivalsOnJoin(event.getPlayer());");
        assertTrue(refresh > 0 && sweep > refresh);
        String j = body(l, "private void convertArrivalsOnJoin(Player player)");
        assertTrue(j.contains("convertArrival(player, slot, decision.weaponId(), \"join\");"), j);
    }

    /** Built from the craft index and the smithing upgrades, and bound before the one registration. */
    @Test
    void theClaimsAreBuiltAndBound() throws IOException {
        String p = code("RpgPlugin.java");
        assertTrue(p.contains("ArrivalClaims.build(weapons.all(), craftResults, smithing,"));
        int bind = p.indexOf("listeners.bindArrivals(arrivals);");
        int reg = p.indexOf("getServer().getPluginManager().registerEvents(listeners, this);");
        assertTrue(bind > 0 && reg > bind);
    }
}
