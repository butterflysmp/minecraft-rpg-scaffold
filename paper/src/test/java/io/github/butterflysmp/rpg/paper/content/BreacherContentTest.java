package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.enchant.EnchantRoll;
import io.github.butterflysmp.rpg.core.recipe.RecipeRegistry;
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
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE BREACHER, AS SHIPPED: Ben's numbers and his recipe, read from the real files (the seat's brief of 2026-11-22,
 * PR 3), and every SEAT PLACEHOLDER pinned, so a retune is a deliberate edit here as well as in the content.
 *
 * <p>Ben, 2026-11-22, verbatim: "1. Common Rarity / 2. Kinetic / 3. Quiver Size 2 / 4. Instead of Arrows it uses
 * cobblestone as ammunition / 5. It fires like a shotgun with a 25 degree and 1s cooldown between shots", then "1. fine"
 * (7 pellets at 6), "2. Fine" (one cobblestone a round), "3. 3 seconds", "4. Fine" (a crossbow), "5. Fine" (fast
 * cobblestone chunks, about 12 blocks), "6. Name should just be "Breacher"", and the recipe "S G I / S G I / X X X".
 */
class BreacherContentTest {

    private static Logger collecting(List<String> warnings) {
        Logger log = Logger.getLogger("BreacherContentTest-" + System.nanoTime());
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

    /** The SOURCE tree, never the classpath: a warm target/classes keeps a deleted file (ContentFreshnessTest). */
    private static File dir(String name) {
        File d = Path.of("src", "main", "resources", "content", name).toFile();
        assertTrue(d.isDirectory(), "no content directory " + d.getAbsolutePath());
        return d;
    }

    private static WeaponRegistry weapons(List<String> warnings) {
        return new WeaponLoader(collecting(warnings)).loadAll(dir("weapons"));
    }

    private static WeaponDefinition breacher(List<String> warnings) {
        WeaponRegistry weapons = weapons(warnings);
        assertTrue(weapons.find("boltor").isPresent(), "CONTROL: the weapon walk sees the Boltor");
        return weapons.find("breacher")
                .orElseThrow(() -> new AssertionError("weapons/breacher.yml did not load: " + warnings));
    }

    private static AbilityDefinition blast(WeaponDefinition w) {
        return w.trigger("right_click").orElseThrow(() -> new AssertionError("no right_click trigger")).ability();
    }

    @Test
    void bensNumbers() {
        List<String> warnings = new ArrayList<>();
        WeaponDefinition w = breacher(warnings);
        assertEquals("Breacher", w.displayName(), "Ben: \"Name should just be Breacher\"");
        assertEquals(Rarity.COMMON, w.rarity(), "Ben: common");
        assertEquals("kinetic", w.element(), "Ben: kinetic");
        assertEquals(WeaponClass.RANGER, w.weaponClass(), "Ben: the iron tier RANGER weapon");
        assertEquals("crossbow", w.material(), "Ben: a crossbow, like the Boltor");
        assertEquals(2, w.quiverSize(), "Ben: quiver size 2");
        assertEquals(60, w.reloadTicks(), "Ben: 3 seconds");
        assertEquals("cobblestone", w.ammo(), "Ben: cobblestone, not arrows");
        assertEquals(Optional.empty(), w.craftResult(), "the recipe claims the weapon, never a craft_result");
        assertEquals(List.of("right_click"), w.triggers().stream().map(t -> t.input()).toList(), "one cast");

        AbilityDefinition a = blast(w);
        assertEquals(20, a.cooldownTicks(), "Ben: 1 s, on the 4-tick grid");
        assertTrue(a.cost().amount() == 0, "no mana: a ranger weapon pays in ammunition");
        List<EffectSpec.Damage> damage = a.onHit().stream()
                .filter(e -> e instanceof EffectSpec.Damage).map(e -> (EffectSpec.Damage) e).toList();
        assertEquals(List.of(new EffectSpec.Damage(40, "kinetic")), damage,
                "Ben, the rework: \"up the damage to 40 per pellet\", kinetic, a literal");
        CastSpec.Projectile p = assertInstanceOf(CastSpec.Projectile.class, a.cast(), "Ben: like a shotgun");
        assertEquals(4, p.spread().count(), "Ben, the rework: three in a triangle and \"a fourth pellet to the center\"");
        assertEquals(3.0, p.spread().angleDegrees(), 0.0, "Ben: \"drop the degree to 6\" total, 3 off the aim each");
        assertEquals(90.0, p.spread().rotationDegrees(), 0.0, "Ben: \"a triangle shape with the point up\"");
        assertTrue(warnings.stream().noneMatch(m -> m.contains("breacher")), "no loader warning: " + warnings);
    }

    /**
     * Each SEAT PLACEHOLDER 2026-11-22, pinned. The angle is the ring's OFFSET from the aim
     * ({@code SpreadPattern.directionsFor}): Ben's rework, "6 degrees total", is 3, turned 90 so the triangle points up.
     */
    @Test
    void theSeatsPlaceholders() {
        WeaponDefinition w = breacher(new ArrayList<>());
        AbilityDefinition a = blast(w);
        assertEquals(new CastSpec.Projectile(3.0, 0.0, 4, "breacher_trail", "cobblestone", null, null,
                        new CastSpec.Spread(4, 3.0, 90.0)), a.cast(),
                "speed 3.0, no gravity, 4 ticks (12 blocks), a cobblestone ITEM body, no homing; 4 at 3 off the aim, point up");
        assertEquals(List.of(new EffectSpec.Visual("breacher_cast")), a.onCast());
        assertEquals(List.of(new EffectSpec.Visual("breacher_hit"), new EffectSpec.Damage(40, "kinetic"),
                new EffectSpec.Knockback(0.1)), a.onHit(),
                "the hit visual, 40 kinetic, and knockback 0.1 (Ben's standing ruling: a travelling ranged weapon "
                        + "authors knockback; the strength is a placeholder)");
        assertEquals(List.of("Four chunks of cobblestone,", "a tight triangle 6 degrees wide."), a.description(),
                "SEAT PLACEHOLDER 2026-11-22, reworded for the rework");
        assertEquals(List.of("Iron, gunpowder, string,", "and whatever was lying around."), w.flavor());
        assertEquals("Breacher", a.displayName(), "no ability name: the header reads the weapon's");
    }

    /** 4 ticks at 3.0 is the seat's "about 12 blocks": a reach, not a range key (ProjectileFlight.step). */
    @Test
    void theReachIsTwelveBlocks() {
        CastSpec.Projectile p = (CastSpec.Projectile) blast(breacher(new ArrayList<>())).cast();
        assertEquals(12.0, p.speed() * p.maxLifetimeTicks(), 1e-9);
    }

    @Test
    void itsThreeVisualsLoad() {
        List<String> warnings = new ArrayList<>();
        VisualRegistry visuals = new VisualLoader(collecting(warnings)).loadAll(dir("visuals"));
        assertTrue(visuals.find("flint_impact").isPresent(), "CONTROL: the visual walk sees flint_impact");
        for (String id : List.of("breacher_cast", "breacher_trail", "breacher_hit")) {
            assertTrue(visuals.find(id).isPresent(), id + " did not load: " + warnings);
        }
        assertTrue(warnings.stream().noneMatch(m -> m.contains("breacher_")), warnings.toString());
    }

    /**
     * What a MINTED Breacher's tooltip reads (the gate's BR1 predicts this, word for word). The mint stamps a full
     * magazine ({@code WeaponItems.mint}: {@code QuiverItems.stampFull}), so a given one reads "Quiver: 2/2". The
     * damage line is the spread's count after the per-pellet 40 x score / 100 (WeaponLoreLines.deliveredShots): "40 x 4".
     */
    @Test
    void aMintedBreachersTooltipReads() {
        WeaponDefinition w = breacher(new ArrayList<>());
        ElementRegistry elements = new ElementLoader(collecting(new ArrayList<>())).loadAll(dir("elements"));
        java.util.function.IntFunction<List<String>> lore = score ->
                io.github.butterflysmp.rpg.paper.weapon.WeaponLore.build(w, elements, OptionalInt.of(2),
                                OptionalInt.of(2), OptionalInt.of(score)).stream()
                        .map(c -> net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                                .serialize(c)).toList();
        assertEquals(List.of("Kinetic", "Gear Score: 100", "Quiver: 2/2", "", "Breacher  Right-Click",
                "Four chunks of cobblestone,", "a tight triangle 6 degrees wide.", "Kinetic Damage: 40 x 4",
                "Cooldown: 1.0s", "", "Iron, gunpowder, string,", "and whatever was lying around.", "",
                "Common Ranged Weapon"), lore.apply(100));
        assertEquals("Kinetic Damage: 60 x 4", lore.apply(150).get(7), "the literal scales with the score");
    }

    /** BEN's recipe: S G I over S G I, the third row empty. A 3x2 shape; it mints the Breacher. */
    @Test
    void bensRecipeMintsIt() {
        RecipeRegistry recipes = new RecipeLoader(collecting(new ArrayList<>())).loadAll(dir("recipes"));
        assertTrue(recipes.find("flint_staff").isPresent(), "CONTROL: the recipe walk sees flint_staff");
        var r = recipes.find("breacher").orElseThrow(() -> new AssertionError("no recipe"));
        assertEquals("breacher", r.mints());
        assertEquals(List.of("SGI", "SGI"), r.shape());
        assertEquals(Map.of('S', "string", 'G', "gunpowder", 'I', "iron_ingot"), r.ingredients());
        assertFalse(r.fitsInTwoByTwo(), "three columns: never the player's 2x2 grid");
    }

    /**
     * NAME THE QUANTITY (CLAUDE.md), for every enchant that can roll. A ranger weapon draws {@code deft_hands},
     * {@code expanded_quiver}, {@code power} and {@code unbreaking} ({@code EnchantRoll.poolFor} gates by class only).
     * <ul>
     *   <li><b>power</b> moves each pellet's hitBase (6 x score / 100): continuous, and every Breacher has it.
     *       PASSES; level I rounds away on one pellet's popup (6.3 reads "6"), recorded in breacher.yml.</li>
     *   <li><b>deft_hands</b> moves reload_ticks 60: 54 / 48 / 42. PASSES.</li>
     *   <li><b>expanded_quiver</b> moves quiver_size 2 by +1/+2/+3, flat. PASSES (+50% at level I: OPEN FOR BEN).</li>
     *   <li><b>unbreaking</b> moves durability, and a crossbow has vanilla durability. PASSES.</li>
     * </ul>
     */
    @Test
    void theEnchantPoolAndEachQuantity() {
        EnchantRegistry enchants = new EnchantLoader(collecting(new ArrayList<>())).loadAll(dir("enchants"));
        List<EnchantRoll.Rollable> roster = new ArrayList<>();
        for (EnchantDefinition d : enchants.all()) roster.add(new EnchantRoll.Rollable(d.id(), d.gearClass()));
        assertTrue(roster.size() >= 10, "CONTROL: the whole roster loaded, " + roster.size());
        WeaponDefinition w = breacher(new ArrayList<>());
        var gearClass = io.github.butterflysmp.rpg.paper.weapon.GearItems.gearClassOf(w);
        assertEquals(List.of("deft_hands", "expanded_quiver", "power", "unbreaking"),
                EnchantRoll.poolFor(gearClass, roster).stream().map(EnchantRoll.Rollable::enchantId).sorted().toList());
        // Each quantity the pool moves exists on this weapon, non-zero:
        assertTrue(w.hasQuiver() && w.quiverSize() == 2, "expanded_quiver's magazine");
        assertEquals(60, w.reloadTicks(), "deft_hands' reload");
        assertTrue(blast(w).onHit().contains(new EffectSpec.Damage(40, "kinetic")), "power's per-pellet literal, 40 since the rework");
    }

    /** It joins NO loot pool: no file under content/loot names it. */
    @Test
    void itJoinsNoLootPool() throws IOException {
        File loot = dir("loot");
        int scanned = 0;
        try (var files = Files.walk(loot.toPath())) {
            for (Path f : files.filter(p -> p.toString().endsWith(".yml")).toList()) {
                scanned++;
                assertFalse(Files.readString(f, StandardCharsets.UTF_8).contains("breacher"), f + " names it");
            }
        }
        assertTrue(scanned > 0, "the loot walk found no files: it is blind");
    }
}
