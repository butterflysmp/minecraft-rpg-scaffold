package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.weapon.GearDefinition;
import io.github.butterflysmp.rpg.paper.selftest.SelfTestLines;
import io.github.butterflysmp.rpg.paper.weapon.WeaponItems;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * R5, BEN'S RULE (2026-11-26, verbatim): "1. No item names should not look slanted". READ, the 26.1.2 jars: the server's
 * {@code ItemStack.getStyledHoverName} (what {@code effectiveName()} returns) and the client's tooltip and hotbar name all
 * wrap the stored name in an empty parent coloured by the item's vanilla rarity and made ITALIC when a {@code custom_name}
 * is present; the client resolves a child's style with {@code Style.applyTo}, where the child's own italic, when set, wins
 * over the parent's. So a name is drawn upright exactly when the STORED name says {@code italic: false} itself.
 *
 * <p>Every minted gear and accessory name is written in ONE place, {@link WeaponItems#displayName} (weapons, shields,
 * armour, tools, accessories). This test walks every shipped definition through it; the source half checks that the five
 * mint sites call it. The instrument's {@code [white, <colour>] italic} on a HELD line is the WRAPPER's italic (it reports
 * italic when ANY node says so, without resolving inheritance): pinned last, so the reading is explained by a test.
 */
class MintedNamesAreNotSlantedTest {

    private static List<GearDefinition> everyGearAndAccessory() {
        Logger q = Logger.getLogger("MintedNamesAreNotSlantedTest-" + System.nanoTime());
        q.setUseParentHandlers(false);
        File c = Path.of("src", "main", "resources", "content").toFile();
        List<GearDefinition> gear = new ArrayList<>();
        gear.addAll(new WeaponLoader(q).loadAll(new File(c, "weapons")).all());
        gear.addAll(new ShieldLoader(q).loadAll(new File(c, "shields")).all());
        gear.addAll(new ArmorLoader(q).loadAll(new File(c, "armor")).all());
        gear.addAll(new ToolLoader(q).loadAll(new File(c, "tools")).all());
        gear.addAll(new AccessoryLoader(q, 100.0).loadAll(new File(c, "accessories"), id -> false).all());
        return gear;
    }

    /** Mutation: {@code .decoration(ITALIC, false)} removed from WeaponItems.displayName. */
    @Test
    void everyMintedNameSaysItalicFalseItself() {
        List<GearDefinition> gear = everyGearAndAccessory();
        assertTrue(gear.size() >= 69, "control: every gear and accessory definition was loaded, " + gear.size());
        List<String> slanted = new ArrayList<>();
        for (GearDefinition g : gear) {
            Component name = WeaponItems.displayName(g.displayName(), g.rarity());
            if (name.decoration(TextDecoration.ITALIC) != TextDecoration.State.FALSE) slanted.add(g.id());
        }
        assertEquals(List.of(), slanted, "a name not explicitly upright is drawn slanted under the client's wrapper");
    }

    /** Mutation: any of the five mint sites writing its name some other way. */
    @Test
    void theFiveMintSitesWriteTheNameThroughTheOnePlace() throws IOException {
        Path w = Path.of("src", "main", "java", "io", "github", "butterflysmp", "rpg", "paper", "weapon");
        String[][] sites = {
                {"WeaponItems.java", "meta.displayName(displayName(weapon.displayName(), weapon.rarity()));"},
                {"ShieldItems.java", "meta.displayName(WeaponItems.displayName(shield.displayName(), shield.rarity()));"},
                {"ArmorItems.java", "meta.displayName(WeaponItems.displayName(armor.displayName(), armor.rarity()));"},
                {"ToolItems.java", "meta.displayName(WeaponItems.displayName(tool.displayName(), tool.rarity()));"},
                {"AccessoryItems.java", "meta.displayName(WeaponItems.displayName(accessory.displayName(), accessory.rarity()));"},
        };
        for (String[] site : sites) {
            String src = Files.readString(w.resolve(site[0]));
            assertTrue(src.contains(site[1]), site[0] + " writes its name through WeaponItems.displayName");
            assertEquals(1, src.split("meta\\.displayName\\(", -1).length - 1, site[0] + ": one name write");
        }
    }

    /** The HELD line's shape, explained: the server's wrapper (white, italic) around our upright blue name. */
    @Test
    void theInstrumentReportsTheWrappersItalic_notWhatIsDrawn() {
        Component stored = Component.text("Blink Tome", NamedTextColor.BLUE).decoration(TextDecoration.ITALIC, false);
        Component wrapper = Component.empty().color(NamedTextColor.WHITE).decorate(TextDecoration.ITALIC).append(stored);
        assertEquals("\"Blink Tome\" [white, blue] italic", SelfTestLines.styled(wrapper),
                "the boot of 1f447197's BT1 name, reproduced from the READ wrapper");
        assertEquals(TextDecoration.State.FALSE, wrapper.children().getFirst().decoration(TextDecoration.ITALIC),
                "and the child the client draws says upright itself");
    }
}
