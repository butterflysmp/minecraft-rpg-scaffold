package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.ability.ResourceCost;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.weapon.Rarity;
import io.github.butterflysmp.rpg.core.weapon.TriggerBinding;
import io.github.butterflysmp.rpg.core.weapon.WeaponClass;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import io.github.butterflysmp.rpg.core.weapon.WeaponRegistry;
import io.github.butterflysmp.rpg.paper.content.ElementDefinition;
import io.github.butterflysmp.rpg.paper.content.ElementLoader;
import io.github.butterflysmp.rpg.paper.content.ElementRegistry;
import io.github.butterflysmp.rpg.paper.content.WeaponLoader;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The tooltip's two colour axes, which are owned by two different places on purpose.
 *
 * The ELEMENT line wears the element's own colour, read from that element's content file. It used
 * to wear the RARITY's colour, which meant a weapon's element never showed its own identity --
 * Emberblade's "Fire" rendered blue because the weapon is rare. The bug was invisible on Ironblade
 * (kinetic is white, common is white) and on Emberblade's footer, so this pins the case where the
 * two colours genuinely disagree: a fire weapon at a non-red tier.
 *
 * Pure Adventure and a plain-Java registry -- no ItemStack, so no running server needed.
 */
class WeaponLoreTest {

    private static final ElementDefinition FIRE = new ElementDefinition("fire", "<red>Fire</red>");

    private static ElementRegistry elementsWithFire() {
        ElementRegistry registry = new ElementRegistry();
        registry.register(FIRE);
        return registry;
    }

    /** A rare (BLUE) fire (RED) weapon: the two colour axes disagree, which is the whole point. */
    private static WeaponDefinition rareFireSword() {
        AbilityDefinition slash = new AbilityDefinition(
                "emberblade/left_click", "Ember Slash", "fire", "none",
                10, ResourceCost.FREE, new CastSpec.Melee(3.5, 120),
                List.of(new EffectSpec.WeaponDamage("fire")), List.of("A cut that smoulders."));
        return new WeaponDefinition("emberblade", "Emberblade", "fire", Rarity.RARE,
                WeaponClass.MELEE, "iron_sword", 7.0,
                List.of(new TriggerBinding("left_click", slash)), List.of("Flavour."));
    }

    /** The effective colour of a line: MiniMessage may hang the colour on a child, not the root. */
    private static TextColor colorOf(Component component) {
        if (component.color() != null) return component.color();
        for (Component child : component.children()) {
            TextColor found = colorOf(child);
            if (found != null) return found;
        }
        return null;
    }

    private static String textOf(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    @Test
    void theElementLineWearsTheElementsOwnColourNotTheRaritys() {
        List<Component> lore = WeaponLore.build(rareFireSword(), elementsWithFire());

        Component elementLine = lore.get(0);
        assertEquals("Fire", textOf(elementLine), "the element's authored name, not the raw id");
        assertEquals(NamedTextColor.RED, colorOf(elementLine),
                "fire.yml declares <red>Fire</red>; the tooltip must use the element's colour");
        // The load-bearing half: this weapon is RARE, so the old behaviour painted this line blue.
        assertNotEquals(RarityColors.of(Rarity.RARE), colorOf(elementLine),
                "the element line must not be coloured by the weapon's rarity tier");
    }

    @Test
    void theRarityFooterStillWearsTheRaritysColour() {
        List<Component> lore = WeaponLore.build(rareFireSword(), elementsWithFire());

        // Rarity still owns the footer -- that line IS the tier, so fixing the element line
        // must not have moved this one too.
        Component footer = lore.get(lore.size() - 1);
        assertEquals("Rare Melee Weapon", textOf(footer));
        assertEquals(RarityColors.of(Rarity.RARE), colorOf(footer));
    }

    /**
     * Every weapon we actually ship, rendered through the loaders we actually run, against the
     * elements we actually ship. The other tests here use a synthetic weapon; this one covers the
     * "all shapes render, none crash the give" gate mechanically -- sword, bow, staff, dev tool --
     * instead of leaving it to someone remembering to /rpg give five items.
     *
     * DISCOVERS rather than asserts, so it must fail loudly on finding nothing: a glob that matches
     * zero weapons, or a resource stream that silently yields nothing on a shaded jar, would
     * otherwise read as a pass having checked no weapon at all.
     */
    @Test
    void everyShippedWeaponRendersAgainstTheShippedElements(@TempDir Path dir) throws IOException {
        String[] weaponIds = {"ironblade", "emberblade", "hunters_bow", "ember_staff", "ability_stone"};
        String[] elementIds = {"fire", "water", "nature", "undead", "void", "wither", "kinetic"};

        Path weaponsDir = Files.createDirectory(dir.resolve("weapons"));
        Path elementsDir = Files.createDirectory(dir.resolve("elements"));
        for (String id : weaponIds) {
            copyBundled("/content/weapons/" + id + ".yml", weaponsDir.resolve(id + ".yml"));
        }
        for (String id : elementIds) {
            copyBundled("/content/elements/" + id + ".yml", elementsDir.resolve(id + ".yml"));
        }

        Logger log = Logger.getLogger("WeaponLoreTest-" + System.nanoTime());
        ElementRegistry elements = new ElementLoader(log).loadAll(elementsDir.toFile());
        WeaponRegistry weapons = new WeaponLoader(log).loadAll(weaponsDir.toFile());

        // Finding nothing is a defect, not a quiet no-op.
        assertEquals(weaponIds.length, weapons.all().size(), "every shipped weapon must load");
        assertEquals(elementIds.length, elements.size(), "every shipped element must load");

        for (WeaponDefinition weapon : weapons.all()) {
            List<Component> lore = assertDoesNotThrow(() -> WeaponLore.build(weapon, elements),
                    () -> "lore build crashed for " + weapon.id());
            assertFalse(lore.isEmpty(), () -> "empty lore for " + weapon.id());

            // The element line must carry the element's OWN declared colour, for every shipped
            // weapon -- not the rarity's. hunters_bow is the case that used to be wrong: fire
            // element (red) at uncommon rarity (green).
            ElementDefinition element = elements.find(weapon.element()).orElseThrow(
                    () -> new AssertionError(weapon.id() + " names an element that does not ship"));
            TextColor expected = colorOf(MiniMessage.miniMessage().deserialize(element.displayName()));
            assertEquals(expected, colorOf(lore.get(0)),
                    () -> weapon.id() + "'s element line must wear " + weapon.element() + "'s colour");

            // And the name the item mints with must be its rarity's colour, always.
            assertEquals(RarityColors.of(weapon.rarity()),
                    colorOf(WeaponItems.displayName(weapon.displayName(), weapon.rarity())),
                    () -> weapon.id() + "'s item name must wear its rarity tier's colour");
        }
    }

    private static void copyBundled(String resource, Path target) throws IOException {
        try (var in = WeaponLoreTest.class.getResourceAsStream(resource)) {
            assertNotNull(in, "bundled content is missing from the classpath: " + resource);
            Files.write(target, in.readAllBytes());
        }
    }

    @Test
    void anUnknownElementFallsBackToGrayRatherThanCrashingTheGive() {
        // ContentValidator rejects a dangling element at boot, so this cannot happen in
        // production -- but a cosmetic line must never be the thing that kills a /rpg give.
        WeaponDefinition weapon = rareFireSword();
        List<Component> lore = assertDoesNotThrow(
                () -> WeaponLore.build(weapon, new ElementRegistry()));

        Component elementLine = lore.get(0);
        assertEquals("Fire", textOf(elementLine), "falls back to the title-cased id");
        assertEquals(NamedTextColor.GRAY, colorOf(elementLine));
    }
}
