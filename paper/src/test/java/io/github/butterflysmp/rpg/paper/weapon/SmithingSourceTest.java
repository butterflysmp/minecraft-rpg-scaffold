package io.github.butterflysmp.rpg.paper.weapon;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * NETHERITE AT THE SMITHING TABLE, THE PAPER HALF (the seat's brief, part g). The handler needs a live smithing table, so
 * its wiring is pinned from the source, comments stripped, each method bounded STRUCTURALLY. The decision is core's
 * ({@code SmithingUpgradesTest}); the table is {@code SmithingLoaderTest}'s.
 */
class SmithingSourceTest {

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

    /** The three arms: vanilla's own for a plain base; NO result on a refusal; the upgraded item on an upgrade. */
    @Test
    void thePrepareHandlerObeysCoresThreeArms() throws IOException {
        String h = body(code("listener/RpgListeners.java"),
                "public void onPrepareSmithing(org.bukkit.event.inventory.PrepareSmithingEvent event)");
        assertTrue(h.contains("smithing.decide(weaponIdOf(base),\n                materialTokenOf(inv.getInputTemplate()),"
                + " materialTokenOf(inv.getInputMineral()));"), h);
        assertTrue(h.contains("case NOT_OURS -> { }"), h);
        assertTrue(h.contains("case REFUSE -> event.setResult(null);"), h);
        assertTrue(h.contains(".map(into -> WeaponItems.upgrade(base, into, adapters)).orElse(null));"), h);
    }

    /** The upgrade IS a re-mint (the carry of enchants, wear, quiver and score), then the id is the upgrade's. */
    @Test
    void theUpgradeRemintsThenRestampsTheId() throws IOException {
        String u = body(code("weapon/WeaponItems.java"),
                "public static ItemStack upgrade(ItemStack base, WeaponDefinition into, AdapterContext adapters)");
        int remint = u.indexOf("ItemStack fresh = remint(base, into, adapters);");
        int stamp = u.indexOf("set(adapters.keys().weaponId,\n                org.bukkit.persistence.PersistentDataType.STRING, into.id())");
        assertTrue(remint > 0 && stamp > remint, "remint first, then the id: " + u);
        String r = body(code("weapon/WeaponItems.java"),
                "public static ItemStack remint(ItemStack old, WeaponDefinition current, AdapterContext adapters)");
        assertTrue(r.contains("GearItems.carryInstanceData(oldMeta, meta, adapters.keys().weaponId,\n"
                + "                    adapters.keys(), fresh.getType());"), "the carry, clamped to the NEW type: " + r);
    }

    /** Bound from content before the one registration. */
    @Test
    void theUpgradesAreBoundFromContent() throws IOException {
        String p = code("RpgPlugin.java");
        assertTrue(p.contains(".load(new File(contentDir, \"smithing.yml\"));"));
        int bind = p.indexOf("listeners.bindSmithing(smithing);");
        int reg = p.indexOf("getServer().getPluginManager().registerEvents(listeners, this);");
        assertTrue(bind > 0 && reg > bind);
    }
}
