package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.enchant.ActiveBehaviour;
import io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour;
import io.github.butterflysmp.rpg.core.enchant.EnchantEffect;
import io.github.butterflysmp.rpg.core.weapon.GrantedTriggers;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import io.github.butterflysmp.rpg.core.weapon.WeaponRegistry;
import io.github.butterflysmp.rpg.paper.weapon.WeaponLore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * LAPIS CRYSTAL'S GRANTED TRIGGER AS SHIPPED (D1, the seat's message of 2026-11-23): Ben's 30 mana and 600 ticks in the
 * content, the loader's refusals by name, and the tooltip showing the granted ability only while the enchant is active.
 */
class LapisCrystalContentTest {

    private static final Set<String> ELEMENTS = Set.of("fire", "kinetic", "nature", "undead", "void", "water", "wither");

    private static Logger capturing(List<String> into) {
        Logger log = Logger.getLogger("LapisCrystalContentTest-" + System.nanoTime());
        log.setUseParentHandlers(false);
        log.addHandler(new Handler() {
            @Override public void publish(LogRecord r) { into.add(r.getMessage()); }
            @Override public void flush() {}
            @Override public void close() {}
        });
        return log;
    }

    private static File content(String dir) {
        return Path.of("src", "main", "resources", "content", dir).toFile();
    }

    private static WeaponRegistry weapons() {
        WeaponRegistry w = new WeaponLoader(capturing(new ArrayList<>())).loadAll(content("weapons"));
        assertTrue(w.find("lapis_staff").isPresent(), "CONTROL: the Lapis Staff loaded");
        return w;
    }

    private static EnchantDefinition shipped(WeaponRegistry weapons) {
        List<String> warnings = new ArrayList<>();
        EnchantRegistry r = EnchantLoader.withWeapons(capturing(warnings), ELEMENTS::contains, weapons::find, false)
                .loadAll(content("enchants"));
        return r.find("lapis_crystal").orElseThrow(() -> new AssertionError("lapis_crystal did not load: " + warnings));
    }

    @Test
    void bensNumbersAreContent() {
        EnchantDefinition c = shipped(weapons());
        assertEquals("Lapis Crystal", c.displayName());
        assertEquals("lapis_staff", c.weapon(), "weapon-specific: the Lapis Staff's slot 0");
        assertEquals(EnchantEffect.GRANT_TRIGGER, c.effect());
        EnchantBehaviour.GrantTrigger g = (EnchantBehaviour.GrantTrigger) c.behaviour();
        assertEquals("left_click", g.input(), "Ben: a new left click ability");
        assertEquals("lapis_crystal/left_click", g.ability().id(), "its own id, so its own cooldown");
        assertEquals(600, g.ability().cooldownTicks(), "Ben: 30 s");
        assertEquals("mana", g.ability().cost().resourceId());
        assertEquals(30.0, g.ability().cost().amount(), 0.0, "Ben: 30 mana");
        assertEquals("kinetic", g.ability().element(), "the staff's element");
        assertEquals("lapis_staff", g.weaponId());
        assertEquals(new io.github.butterflysmp.rpg.core.ability.CastSpec.Orb(12, 10, 300, 0.6, "heart_of_the_sea",
                        new io.github.butterflysmp.rpg.core.ability.CastSpec.OrbShot(8, List.of(30.0, 50.0, 70.0), "kinetic", null)),
                g.ability().cast(), "D2: Ben's 12 blocks and 300 ticks; D3: Ben's 8 blocks, and his 30/50/70 (2026-11-23:"
                        + " \"Increase the damage of the crystal to 30/50/70%\"); the seat's flight, radius, look and element");
    }

    /**
     * D3, B5: the line shows THE NUMBER off the staff's own beam (21), not the percentage: 6.3 / 10.5 / 14.7 at Ben's
     * 30/50/70 (21 x 30 / 100, 21 x 50 / 100, 21 x 70 / 100, each exact in doubles, EXECUTED). A renderer that cannot see
     * the weapons shows the percent, and says whose.
     */
    @Test
    void itsLineShowsTheNumberNotThePercentage() {
        WeaponRegistry w = weapons();
        EnchantDefinition c = shipped(w);
        String head = "grants Left-Click: Lapis Crystal (Cooldown: 30.0s | Mana Cost: 30); a beam into it strikes each foe"
                + " within 8 for ";
        var gear = io.github.butterflysmp.rpg.core.weapon.GearClass.MAGE;
        assertEquals(head + "6.3 damage", io.github.butterflysmp.rpg.paper.weapon.EnchantEffectLine.bare(c, 1, gear, w::find));
        assertEquals(head + "10.5 damage", io.github.butterflysmp.rpg.paper.weapon.EnchantEffectLine.bare(c, 2, gear, w::find));
        assertEquals(head + "14.7 damage", io.github.butterflysmp.rpg.paper.weapon.EnchantEffectLine.bare(c, 3, gear, w::find));
        assertEquals(head + "50% of the beam", io.github.butterflysmp.rpg.paper.weapon.EnchantEffectLine.bare(c, 2, gear),
                "a renderer with no weapons");
    }

    /** D3: an orb's shot is refused by name when its levels disagree with max_level, or its element is not loaded. */
    @Test
    void aMalformedShotIsRefusedByName(@TempDir Path dir) throws IOException {
        String head = "display_name: \"X\"\nmax_level: 3\neffect: grant_trigger\nclass: universal\nweapon: lapis_staff\n"
                + "grant_trigger:\n  input: left_click\n  ability:\n    cooldown_ticks: 20\n    cast:\n      type: orb\n"
                + "      range: 12\n      flight_ticks: 10\n      life_ticks: 300\n      radius: 0.6\n      item: stone\n"
                + "      shot:\n        radius: 8\n";
        Files.writeString(dir.resolve("good.yml"), head + "        percent_by_level: [20, 30, 40]\n        element: kinetic\n");
        Files.writeString(dir.resolve("two.yml"), head + "        percent_by_level: [20, 30]\n        element: kinetic\n");
        Files.writeString(dir.resolve("elem.yml"), head + "        percent_by_level: [20, 30, 40]\n        element: kinetik\n");
        List<String> warnings = new ArrayList<>();
        WeaponRegistry w = weapons();
        EnchantRegistry r = EnchantLoader.withWeapons(capturing(warnings), ELEMENTS::contains, w::find, false)
                .loadAll(dir.toFile());
        assertTrue(r.find("good").isPresent(), "the control: " + warnings);
        assertTrue(r.find("two").isEmpty());
        assertTrue(r.find("elem").isEmpty());
        assertTrue(warnings.stream().anyMatch(x -> x.contains("'two' declares max_level 3 but its grant_trigger: block has 2"
                + " levels")), warnings.toString());
        assertTrue(warnings.stream().anyMatch(x -> x.contains("'elem' grant_trigger: the orb's shot element 'kinetik' is not a"
                + " loaded element")), warnings.toString());
    }

    /** The tooltip (B5): the granted ability renders as any ability does while active, and not at all otherwise. */
    @Test
    void theTooltipShowsTheGrantOnlyWhileActive() {
        WeaponRegistry w = weapons();
        WeaponDefinition staff = w.find("lapis_staff").orElseThrow();
        EnchantDefinition c = shipped(w);
        ElementRegistry elements = new ElementLoader(capturing(new ArrayList<>())).loadAll(content("elements"));
        java.util.function.Function<List<ActiveBehaviour>, List<String>> lore = active ->
                WeaponLore.build(staff, elements, OptionalInt.empty(), OptionalInt.empty(), OptionalInt.of(100),
                                GrantedTriggers.granted(staff, active)).stream()
                        .map(x -> net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                                .serialize(x)).toList();
        List<String> on = lore.apply(List.of(new ActiveBehaviour("lapis_crystal", 1, c.behaviour())));
        List<String> off = lore.apply(List.of());
        assertTrue(on.contains("Lapis Crystal  Left-Click"), on.toString());
        assertTrue(on.contains("Throws a lapis crystal"), on.toString());
        assertTrue(on.containsAll(off), "the staff's own lines are untouched: " + off);
        assertFalse(off.contains("Lapis Crystal  Left-Click"), "not active: not shown");
        assertEquals(off.size() + 5, on.size(), "the block: a blank, the name, two description lines, the cadence: " + on);
    }

    /** D1's refusals, each by name, each beside a control that loads. */
    @Test
    void aGrantIsRefusedByNameOnABoundGestureAQuiverWeaponOrWithoutItsWeapon(@TempDir Path dir) throws IOException {
        String body = "  ability:\n    name: \"X\"\n    cooldown_ticks: 20\n    cast:\n      type: self\n";
        String head = "display_name: \"X\"\nmax_level: 1\neffect: grant_trigger\nclass: universal\n";
        Files.writeString(dir.resolve("good.yml"), head + "weapon: lapis_staff\ngrant_trigger:\n  input: left_click\n" + body);
        Files.writeString(dir.resolve("bound.yml"), head + "weapon: lapis_staff\ngrant_trigger:\n  input: right_click\n" + body);
        // left_click: the Boltor does not BIND it (its quiver takes it), so only the quiver refusal can stop this file.
        Files.writeString(dir.resolve("quiver.yml"), head + "weapon: boltor\ngrant_trigger:\n  input: left_click\n" + body);
        Files.writeString(dir.resolve("unweaponed.yml"), head + "grant_trigger:\n  input: left_click\n" + body);
        Files.writeString(dir.resolve("gesture.yml"), head + "weapon: lapis_staff\ngrant_trigger:\n  input: sneak\n" + body);
        Files.writeString(dir.resolve("no_cooldown.yml"), head + "weapon: lapis_staff\ngrant_trigger:\n  input: left_click\n"
                + body.replace("    cooldown_ticks: 20\n", ""));
        List<String> warnings = new ArrayList<>();
        WeaponRegistry w = weapons();
        EnchantRegistry r = EnchantLoader.withWeapons(capturing(warnings), ELEMENTS::contains, w::find, false)
                .loadAll(dir.toFile());
        assertTrue(r.find("good").isPresent(), "the control: " + warnings);
        for (String bad : List.of("bound", "quiver", "unweaponed", "gesture", "no_cooldown")) {
            assertTrue(r.find(bad).isEmpty(), bad);
        }
        java.util.function.Predicate<String> warned = s -> warnings.stream().anyMatch(x -> x.contains(s));
        assertTrue(warned.test("'bound' grant_trigger: weapon 'lapis_staff' already binds right_click"), warnings.toString());
        assertTrue(warned.test("'quiver' grant_trigger: weapon 'boltor' has a quiver"), warnings.toString());
        assertTrue(warned.test("'unweaponed' grant_trigger: is granted to one weapon"), warnings.toString());
        assertTrue(warned.test("'gesture' grant_trigger: input 'sneak' must be one of [left_click, right_click]"),
                warnings.toString());
        assertTrue(warned.test("'no_cooldown' grant_trigger: ability is missing a whole-number 'cooldown_ticks'"),
                warnings.toString());

        List<String> blind = new ArrayList<>();
        assertTrue(new EnchantLoader(capturing(blind), ELEMENTS::contains, id -> java.util.Optional.empty(), false)
                .loadAll(dir.toFile()).find("good").isEmpty(), "a loader that cannot see the weapons refuses every grant");
    }
}
