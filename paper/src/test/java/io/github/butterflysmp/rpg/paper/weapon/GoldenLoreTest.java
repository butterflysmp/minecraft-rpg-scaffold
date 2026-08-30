package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.weapon.ArmorDefinition;
import io.github.butterflysmp.rpg.core.weapon.ArmorLoreLines;
import io.github.butterflysmp.rpg.core.weapon.ArmorSlot;
import io.github.butterflysmp.rpg.core.weapon.Rarity;
import io.github.butterflysmp.rpg.core.weapon.ShieldDefinition;
import io.github.butterflysmp.rpg.core.weapon.ShieldLoreLines;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import io.github.butterflysmp.rpg.paper.content.ArmorLoader;
import io.github.butterflysmp.rpg.paper.content.ElementLoader;
import io.github.butterflysmp.rpg.paper.content.ElementRegistry;
import io.github.butterflysmp.rpg.paper.content.ShieldLoader;
import io.github.butterflysmp.rpg.paper.content.WeaponLoader;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextDecoration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE EXTRACTION GATE: every shipped gear item's tooltip, serialized in full, pinned against a
 * golden file checked in beside this test.
 *
 * <p>The `GearDefinition`/`GearItems` refactor's contract is "minted items byte-identical before and
 * after". Minting itself needs a live server -- {@code new ItemStack(...)} throws without one -- so
 * the PDC and attribute half of that claim can only be boot-witnessed. The LORE half is pure
 * Adventure over pure records, and that is the half where the duplication actually lives:
 * {@code plain}, {@code blank}, {@code titleCase}, the footer shape and four copies of a
 * whole-number trimmer.
 *
 * <p>So this renders all three lore builders over ALL shipped content and every rarity, serializes
 * colour and every decoration alongside the text, and compares to {@code golden-lore.txt}. Refactor
 * the builders onto shared helpers and this file must not move by one character.
 *
 * <p><b>Why a golden file rather than more assertions.</b> The existing {@code WeaponLoreTest},
 * {@code ShieldLoreTest} and {@code ArmorLoreTest} already assert the tooltip's SHAPE, and they stay
 * the primary guard -- they say what is true and why. What they cannot do is notice an incidental
 * change nobody thought to assert: a lost italic on one line, a colour that silently became
 * DARK_GREEN, a blank that moved. A golden dump notices all of it at once precisely because it
 * asserts nothing and records everything.
 *
 * <p><b>Regenerate deliberately, never reflexively.</b> If this reddens during a refactor that was
 * supposed to preserve behaviour, the refactor is wrong -- not the golden file. Rewrite the golden
 * only when a tooltip change is the INTENT, and say so in the commit. Set
 * {@code -Dgolden.regenerate=true} to rewrite it, which deliberately requires typing something you
 * would not type by accident.
 *
 * <h2>IF YOU LANDED HERE BECAUSE THIS REDDENED AFTER A CONTENT EDIT, READ THIS FIRST</h2>
 *
 * You probably edited the wrong tree. There are two copies of every content file and they do
 * different jobs:
 *
 * <ul>
 *   <li><b>{@code paper/src/main/resources/content/}</b> -- the SOURCE tree. This test renders from
 *       it, so editing it reddens the golden and breaks the build.
 *   <li><b>{@code run/plugins/Rpg/content/}</b> -- the DEPLOYED tree. The server loads from it at
 *       boot, and {@code saveResource(path, false)} never overwrites what is already there.
 * </ul>
 *
 * <p>So a tuning or {@code /rpg refresh} check must edit the <b>deployed</b> tree and re-boot with
 * {@code ./scripts/dev-server.sh --no-build}. Editing the source tree trips this test; editing the
 * deployed tree without re-booting changes nothing the server can see, because
 * {@code GearRefresher.refresh} re-mints from the registry loaded at BOOT and there is no reload
 * path -- {@code /rpg refresh} rebuilds items from the definitions already in memory, not from the
 * files on disk. Both mistakes look like "my edit did nothing", from opposite directions.
 *
 * <p>Regenerating the golden to make a source edit green is only correct when the tooltip change is
 * the INTENT and ships. It is never the way to run a tuning experiment.
 *
 * <p>Discovers rather than enumerates, and refuses an empty walk: a golden file that pinned zero
 * items would pass forever, which is the failure this repo records twice.
 */
class GoldenLoreTest {

    private static final Logger LOG = Logger.getLogger(GoldenLoreTest.class.getName());
    private static final String GOLDEN = "golden-lore.txt";

    /** Bulwark levels to render a shield at -- 0 is the unenchanted identity. */
    private static final double[] BULWARK = {0.0, 0.15, 0.30, 0.45};

    @Test
    void everyShippedTooltipIsUnchanged() throws IOException {
        String actual = render();

        if (Boolean.getBoolean("golden.regenerate")) {
            Path out = Path.of("src/test/resources", GOLDEN);
            Files.createDirectories(out.getParent());
            Files.writeString(out, actual, StandardCharsets.UTF_8);
            throw new AssertionError("Golden file REGENERATED at " + out.toAbsolutePath()
                    + ". Re-run without -Dgolden.regenerate to verify, and say in the commit message"
                    + " why the tooltips were meant to change.");
        }

        String expected;
        try (InputStream in = GoldenLoreTest.class.getClassLoader().getResourceAsStream(GOLDEN)) {
            assertTrue(in != null, GOLDEN + " is missing from the test resources -- generate it with"
                    + " -Dgolden.regenerate=true and commit it");
            expected = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }

        // Normalised so a CRLF checkout cannot redden a green refactor.
        assertEquals(expected.replace("\r\n", "\n"), actual.replace("\r\n", "\n"),
                "A shipped tooltip changed. If this was NOT the intent, the refactor is wrong -- do"
                        + " not regenerate the golden file to make it green.");
    }

    // --- Rendering ------------------------------------------------------------------------------

    private static String render() {
        StringBuilder out = new StringBuilder();
        int items = 0;

        ElementRegistry elements = new ElementLoader(LOG)
                .loadAll(new File("src/main/resources/content/elements"));

        List<WeaponDefinition> weapons =
                new ArrayList<>(new WeaponLoader(LOG)
                        .loadAll(new File("src/main/resources/content/weapons")).all());
        List<ShieldDefinition> shields =
                new ArrayList<>(new ShieldLoader(LOG)
                        .loadAll(new File("src/main/resources/content/shields")).all());
        List<ArmorDefinition> armor =
                new ArrayList<>(new ArmorLoader(LOG)
                        .loadAll(new File("src/main/resources/content/armor")).all());

        assertFalse(weapons.isEmpty(), "no shipped weapons discovered -- a golden over nothing passes forever");
        assertFalse(shields.isEmpty(), "no shipped shields discovered");
        assertFalse(armor.isEmpty(), "no shipped armor discovered");

        // Sorted by id so the dump does not depend on directory order.
        weapons.sort((a, b) -> a.id().compareTo(b.id()));
        shields.sort((a, b) -> a.id().compareTo(b.id()));
        armor.sort((a, b) -> a.id().compareTo(b.id()));

        out.append("=== WEAPONS ===\n");
        for (WeaponDefinition w : weapons) {
            out.append("-- ").append(w.id()).append('\n');
            appendLore(out, WeaponLore.build(w, elements));
            items++;
        }

        out.append("\n=== SHIELDS ===\n");
        for (ShieldDefinition s : shields) {
            for (double bulwark : BULWARK) {
                out.append("-- ").append(s.id()).append(" @bulwark=").append(bulwark).append('\n');
                appendLore(out, ShieldLore.build(s, bulwark));
                items++;
            }
        }

        out.append("\n=== ARMOR ===\n");
        for (ArmorDefinition a : armor) {
            out.append("-- ").append(a.id()).append('\n');
            appendLore(out, ArmorLore.build(a));
            items++;
        }

        // Every rarity through every footer, so a tier nobody ships is still pinned.
        out.append("\n=== RARITY x SLOT FOOTERS ===\n");
        for (Rarity rarity : Rarity.values()) {
            for (ArmorSlot slot : ArmorSlot.values()) {
                ArmorDefinition a = new ArmorDefinition("probe", "Probe", rarity, "probe", slot, 7,
                        List.of("flavour probe"));
                out.append("-- ").append(rarity).append('/').append(slot).append('\n');
                appendLore(out, ArmorLore.build(a));
                items++;
            }
            ShieldDefinition s = new ShieldDefinition("probe", "Probe", rarity, "shield", 0.35,
                    List.of("flavour probe"));
            out.append("-- ").append(rarity).append("/SHIELD\n");
            appendLore(out, ShieldLore.build(s));
            items++;
        }

        // The plain-text formatters, which is where the four trimmers live. Values chosen to include
        // the binary-FP cases ShieldLoreLines documents, so folding the trimmers cannot quietly
        // change rounding.
        out.append("\n=== LORE LINES ===\n");
        for (double d : new double[]{0, 1, 2.5, 3, 6, 8, 20, 0.5, 100}) {
            out.append("armor.defenseLabel(").append(d).append(") = ")
                    .append(ArmorLoreLines.defenseLabel(d)).append('\n');
            items++;
        }
        for (double d : new double[]{0, 0.29, 0.35, 0.5, 0.55, 0.125, 0.65, 1}) {
            out.append("shield.damageReductionLabel(").append(d).append(") = ")
                    .append(ShieldLoreLines.damageReductionLabel(d)).append('\n');
            items++;
        }
        for (ArmorSlot slot : ArmorSlot.values()) {
            out.append("armor.slotNoun(").append(slot).append(") = ")
                    .append(ArmorLoreLines.slotNoun(slot)).append('\n');
            items++;
        }

        assertTrue(items > 60, "the dump covered only " + items + " renderings, which is too few to"
                + " be the whole shipped roster -- a shrunken golden passes vacuously");
        out.append("\n=== ").append(items).append(" renderings ===\n");
        return out.toString();
    }

    /**
     * Serialize a component tree with colour and EVERY decoration, not just its text.
     *
     * Hand-rolled rather than reaching for a serializer, so the dump records exactly the three things
     * that matter here and cannot drift with a library version: the literal content, the colour, and
     * each decoration's explicit TRUE/FALSE/NOT_SET state. The last one is the point -- Minecraft
     * italicises custom lore by default, so ITALIC=NOT_SET and ITALIC=FALSE render differently, and a
     * serializer that omitted an unset decoration would hide exactly that regression.
     */
    private static void appendLore(StringBuilder out, List<Component> lore) {
        for (Component line : lore) {
            out.append("   ").append(describe(line)).append('\n');
        }
    }

    private static String describe(Component c) {
        StringBuilder s = new StringBuilder();
        s.append('"').append(c instanceof TextComponent t ? t.content() : "").append('"');
        s.append(" color=").append(c.color() == null ? "-" : c.color().asHexString());
        for (TextDecoration d : TextDecoration.values()) {
            s.append(' ').append(d.name().charAt(0)).append('=');
            s.append(switch (c.decoration(d)) {
                case TRUE -> "T";
                case FALSE -> "F";
                case NOT_SET -> ".";
            });
        }
        if (!c.children().isEmpty()) {
            s.append(" [");
            for (Component child : c.children()) s.append(describe(child)).append(' ');
            s.append(']');
        }
        return s.toString();
    }
}
