package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.enchant.EnchantRoll;
import io.github.butterflysmp.rpg.core.weapon.Rarity;
import io.github.butterflysmp.rpg.core.weapon.WeaponClass;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import io.github.butterflysmp.rpg.core.weapon.WeaponRegistry;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE KNELL BEAM, AS SHIPPED: Ben's numbers and every seat placeholder, read from the real file (the seat's brief of
 * 2026-11-21, PR 3). The SOURCE tree, never the classpath, so a deleted file cannot be answered for by a stale copy in
 * {@code target/classes} (the hazard the Crystal Shard Staff's content test met).
 */
class KnellBeamContentTest {

    private static Logger collecting(List<String> warnings) {
        Logger log = Logger.getLogger("KnellBeamContentTest-" + System.nanoTime());
        log.setUseParentHandlers(false);
        log.addHandler(new Handler() {
            @Override public void publish(LogRecord r) {
                if (r.getLevel().intValue() >= Level.WARNING.intValue()) warnings.add(r.getMessage());
            }
            @Override public void flush() {}
            @Override public void close() {}
        });
        return log;
    }

    private static File dir(String name) {
        File d = Path.of("src", "main", "resources", "content", name).toFile();
        assertTrue(d.isDirectory(), "no content directory " + d.getAbsolutePath());
        return d;
    }

    private static WeaponDefinition beam(List<String> warnings) {
        WeaponRegistry weapons = new WeaponLoader(collecting(warnings)).loadAll(dir("weapons"));
        assertTrue(weapons.find("flint_staff").isPresent(), "CONTROL: the weapon walk sees the flint staff");
        return weapons.find("knell_beam").orElseThrow(() -> new AssertionError("weapons/knell_beam.yml did not load: "
                + warnings));
    }

    private static AbilityDefinition toll(WeaponDefinition w) {
        return w.trigger("right_click").orElseThrow().ability();
    }

    /** Ben's: the windup is the Knell mob's telegraph (read from the mob's file), the cooldown double it, void, locked. */
    @Test
    void bensNumbers() throws IOException {
        List<String> warnings = new ArrayList<>();
        WeaponDefinition w = beam(warnings);
        int telegraph = Integer.parseInt(Files.readAllLines(dir("mobs").toPath().resolve("knell.yml"), StandardCharsets.UTF_8)
                .stream().map(String::trim).filter(l -> l.startsWith("telegraph_ticks:")).findFirst().orElseThrow()
                .substring("telegraph_ticks:".length()).trim());
        assertEquals(24, telegraph, "CONTROL: the Knell mob's telegraph, read from its file");
        AbilityDefinition a = toll(w);
        CastSpec.Volley v = assertInstanceOf(CastSpec.Volley.class, a.cast());
        assertEquals(telegraph, v.windupTicks(), "Ben: the windup matches the Knell mob");
        assertEquals(2 * telegraph, a.cooldownTicks(), "Ben: the cooldown is double the windup");
        assertEquals("void", w.element(), "Ben: a void weapon");
        assertTrue(v.lockedAim(), "Ben: keep the locked aim");
        assertEquals(1, v.shots());
        CastSpec.Ray ray = assertInstanceOf(CastSpec.Ray.class, v.of());
        assertTrue(ray.tolerance() < 1.0, "Ben: more accuracy than the old 1.0");
        assertTrue(warnings.stream().noneMatch(m -> m.contains("knell_beam")), warnings.toString());
    }

    /** Every seat placeholder, pinned, so a change is a deliberate edit here too. */
    @Test
    void theSeatsPlaceholders() {
        WeaponDefinition w = beam(new ArrayList<>());
        assertEquals("Knell Beam", w.displayName());
        assertEquals(Rarity.EPIC, w.rarity(), "the first epic item");
        assertEquals("echo_shard", w.material());
        assertEquals(WeaponClass.MAGE, w.weaponClass());
        assertEquals(Optional.empty(), w.craftResult(), "no recipe and no craft_result (Ben: a dragon drop later)");
        AbilityDefinition a = toll(w);
        assertEquals("Knell's Toll", a.displayName());
        assertEquals(List.of("Locks a line where you look,", "then tolls a beam down it 1.2s later."), a.description());
        assertEquals("mana", a.cost().resourceId());
        assertEquals(13.0, a.cost().amount(), 0.0);
        CastSpec.Volley v = (CastSpec.Volley) a.cast();
        assertEquals(new CastSpec.Volley.Warning("knell_warning", List.of(0)), v.warning(), "one flash, at the click");
        assertEquals(new CastSpec.Ray(40, "knell_toll_line", true, 0.3), v.of(), "range 40, pierce, tolerance 0.3");
        assertEquals(List.of(new EffectSpec.Visual("knell_toll_cast")), a.onCast());
        assertEquals(List.of(new EffectSpec.Visual("knell_toll_hit"), new EffectSpec.Damage(63, "void")), a.onHit());
    }

    @Test
    void itsFourVisualsLoadAndTheBeamsHoldNoSound() {
        List<String> warnings = new ArrayList<>();
        VisualRegistry visuals = new VisualLoader(collecting(warnings)).loadAll(dir("visuals"));
        assertTrue(visuals.find("flint_impact").isPresent(), "CONTROL: the visual walk sees flint_impact");
        for (String id : List.of("knell_toll_cast", "knell_warning", "knell_toll_line", "knell_toll_hit")) {
            assertTrue(visuals.find(id).isPresent(), id + " did not load: " + warnings);
        }
        for (String beam : List.of("knell_warning", "knell_toll_line")) {
            assertTrue(visuals.find(beam).orElseThrow().steps().stream().noneMatch(s -> s instanceof VisualSpec.Sound),
                    beam + " is drawn as a beam and may hold no sound");
        }
    }

    /** No recipe mints it, and no loot pool names it (Ben: "It will be a dragon drop later"). */
    @Test
    void itHasNoRecipeAndJoinsNoLootPool() throws IOException {
        var recipes = new RecipeLoader(collecting(new ArrayList<>())).loadAll(dir("recipes"));
        assertTrue(recipes.find("flint_staff").isPresent(), "CONTROL: the recipe walk sees flint_staff");
        assertTrue(recipes.all().stream().noneMatch(r -> r.mints().equals("knell_beam")), "no recipe mints it");
        String loot = Files.readString(dir("loot").toPath().resolve("cascade.yml"), StandardCharsets.UTF_8);
        assertTrue(loot.contains("flint_staff"), "CONTROL: the loot scan sees a pooled id");
        assertFalse(loot.lines().filter(l -> !l.trim().startsWith("#")).anyMatch(l -> l.contains("knell_beam")),
                "no pool names it");
    }

    /**
     * NAME THE QUANTITY: a mage weapon draws attunement (effect damage; it reaches a literal damage, EffectApplier's
     * Damage arm) and unbreaking (effect durability). echo_shard has no durability, so Unbreaking rolls and grants
     * nothing, as on the flint and crystal staves. Pinned so a pool change re-takes the answer.
     */
    @Test
    void theEnchantPoolIsAttunementAndUnbreaking() {
        EnchantRegistry enchants = new EnchantLoader(collecting(new ArrayList<>())).loadAll(dir("enchants"));
        List<EnchantRoll.Rollable> roster = new ArrayList<>();
        for (EnchantDefinition d : enchants.all()) roster.add(new EnchantRoll.Rollable(d.id(), d.gearClass()));
        assertTrue(roster.size() >= 10, "CONTROL: the whole roster loaded, " + roster.size());
        var gearClass = io.github.butterflysmp.rpg.paper.weapon.GearItems.gearClassOf(beam(new ArrayList<>()));
        assertEquals(List.of("attunement", "unbreaking"),
                EnchantRoll.normalPoolFor(EnchantRoll.Has.EVERY, gearClass, roster).stream().map(EnchantRoll.Rollable::enchantId).sorted().toList());
    }

    /** What a minted beam's tooltip reads at score 100 (the gate's tooltip row predicts these lines). */
    @Test
    void aMintedBeamsTooltipReads() {
        WeaponDefinition w = beam(new ArrayList<>());
        ElementRegistry elements = new ElementLoader(collecting(new ArrayList<>())).loadAll(dir("elements"));
        List<String> lore = io.github.butterflysmp.rpg.paper.weapon.WeaponLore.build(w, elements,
                        java.util.OptionalInt.empty(), java.util.OptionalInt.empty(), java.util.OptionalInt.of(100)).stream()
                .map(c -> net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(c))
                .toList();
        assertEquals(List.of("Void", "Gear Score: 100", "", "Knell's Toll  Right-Click",
                "Locks a line where you look,", "then tolls a beam down it 1.2s later.", "Magic Damage: 63",
                "Cooldown: 2.4s | Mana Cost: 13", "", "Epic Magic Weapon"), lore);
    }
}
