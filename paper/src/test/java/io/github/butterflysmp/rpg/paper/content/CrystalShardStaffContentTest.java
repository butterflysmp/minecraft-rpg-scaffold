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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE CRYSTAL SHARD STAFF, AS SHIPPED: Ben's numbers read from the real file (the seat's brief of 2026-11-21, PR A).
 *
 * <p>Ben, 2026-11-21, verbatim: "The old version was a slow traveling homing shot. I want this one to be fast hitting.
 * keep the homing but not too much damage. Say 16 to start. Kinetic damage type keep the 8 mana cost with a 0.7s
 * cooldown", and "Keep the item and recipe". So: 16 kinetic, 8 mana, 14 ticks (0.7 s, NOT rounded to the 4-tick grid),
 * homing kept, an amethyst shard, the old recipe (amethyst shard over cobblestone over stick).
 *
 * <p>The SEAT PLACEHOLDERS (speed 3.0, gravity 0, lifetime 7, the homing block 0.45 / 0 / 18, the name "Crystal Shard")
 * are pinned too, so that a retune is a deliberate edit to this file as well as to the content. The same flight numbers
 * are carried by {@code CrystalShardCatchAngleTest} in core, which cannot read YAML; {@link #theFlightIsTheSeatsPlaceholders}
 * is what keeps the two in step.
 */
class CrystalShardStaffContentTest {

    /** The flight numbers {@code CrystalShardCatchAngleTest} also carries. Change one, change both. */
    static final double SPEED = 3.0;
    static final double GRAVITY = 0.0;
    static final int LIFETIME = 7;
    static final double LERP = 0.45;   // SEAT PLACEHOLDER 2026-11-21, raised from the old 0.18
    static final double ACTIVATION = 0.0;
    static final double SEARCH_RADIUS = 18.0;

    private static Logger collecting(List<String> warnings) {
        Logger log = Logger.getLogger("CrystalShardStaffContentTest-" + System.nanoTime());
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

    /**
     * The SOURCE tree, never the classpath. Written first against the classpath, this test stayed GREEN with
     * crystal_shard_staff.yml deleted: a warm {@code target/classes} still held the old copy, which a resource copy
     * never removes ({@code ContentFreshnessTest}'s hazard). Read from source, a deleted file reddens the test.
     */
    private static File dir(String name) {
        File d = Path.of("src", "main", "resources", "content", name).toFile();
        assertTrue(d.isDirectory(), "no content directory " + d.getAbsolutePath());
        return d;
    }

    private static WeaponDefinition staff(List<String> warnings) {
        WeaponRegistry weapons = new WeaponLoader(collecting(warnings)).loadAll(dir("weapons"));
        assertTrue(weapons.find("flint_staff").isPresent(), "CONTROL: the weapon walk sees the flint staff");
        return weapons.find("crystal_shard_staff")
                .orElseThrow(() -> new AssertionError("weapons/crystal_shard_staff.yml did not load"));
    }

    private static AbilityDefinition shard(WeaponDefinition w) {
        return w.trigger("right_click").orElseThrow(() -> new AssertionError("no right_click trigger")).ability();
    }

    @Test
    void bensNumbers() {
        List<String> warnings = new ArrayList<>();
        WeaponDefinition w = staff(warnings);
        assertEquals("Crystal Shard Staff", w.displayName());
        assertEquals(WeaponClass.MAGE, w.weaponClass());
        assertEquals(Rarity.COMMON, w.rarity());
        assertEquals("kinetic", w.element());
        assertEquals("amethyst_shard", w.material(), "Ben: keep the item");
        assertEquals(Optional.empty(), w.craftResult(), "the recipe claims the weapon, never a craft_result");
        assertEquals(List.of("right_click"), w.triggers().stream().map(t -> t.input()).toList(), "one cast, no left");

        AbilityDefinition a = shard(w);
        assertEquals(14, a.cooldownTicks(), "Ben: 0.7 s, exactly as he said, NOT rounded to the grid");
        assertEquals("mana", a.cost().resourceId());
        assertEquals(8.0, a.cost().amount(), 0.0, "Ben: keep the 8 mana cost");
        List<EffectSpec.Damage> damage = a.onHit().stream()
                .filter(e -> e instanceof EffectSpec.Damage).map(e -> (EffectSpec.Damage) e).toList();
        assertEquals(List.of(new EffectSpec.Damage(16, "kinetic")), damage, "Ben: 16, kinetic, a literal damage");
        assertTrue(warnings.stream().noneMatch(m -> m.contains("crystal_shard_staff")),
                "no loader warning names the staff: " + warnings);
    }

    @Test
    void theFlightIsTheSeatsPlaceholders() {
        AbilityDefinition a = shard(staff(new ArrayList<>()));
        CastSpec.Projectile p = assertInstanceOf(CastSpec.Projectile.class, a.cast());
        assertEquals(SPEED, p.speed(), 0.0);
        assertEquals(GRAVITY, p.gravity(), 0.0);
        assertEquals(LIFETIME, p.maxLifetimeTicks());
        assertEquals("amethyst_shard", p.item(), "the body is the old shard item");
        assertEquals("crystal_trail", p.trail());
        assertEquals(new CastSpec.Homing(LERP, ACTIVATION, SEARCH_RADIUS), p.homing(), "homing kept");
        assertEquals("Crystal Shard", a.displayName(), "SEAT PLACEHOLDER");
        assertEquals(List.of(new EffectSpec.Visual("crystal_cast")), a.onCast());
        assertTrue(a.onHit().contains(new EffectSpec.Visual("crystal_impact")), a.onHit().toString());
    }

    @Test
    void itsThreeVisualsLoad() {
        List<String> warnings = new ArrayList<>();
        VisualRegistry visuals = new VisualLoader(collecting(warnings)).loadAll(dir("visuals"));
        assertTrue(visuals.find("flint_impact").isPresent(), "CONTROL: the visual walk sees flint_impact");
        for (String id : List.of("crystal_cast", "crystal_trail", "crystal_impact")) {
            assertTrue(visuals.find(id).isPresent(), id + " did not load: " + warnings);
        }
        assertTrue(warnings.stream().noneMatch(m -> m.contains("crystal_")), warnings.toString());
    }

    /**
     * What a MINTED staff's tooltip reads (the gate's CS1 predicts this, word for word): its damage line is the authored
     * 16 x score / 100, so 16 at 100 and 24 at 150. And the cooldown prints Ben's 0.7 s: the line formats the AUTHORED
     * 14 ({@code WeaponLoreLines.cooldownLabel}), never what held fire delivers.
     */
    @Test
    void aMintedStaffsTooltipReads() {
        WeaponDefinition w = staff(new ArrayList<>());
        ElementRegistry elements = new ElementLoader(collecting(new ArrayList<>())).loadAll(dir("elements"));
        java.util.function.IntFunction<List<String>> lore = score ->
                io.github.butterflysmp.rpg.paper.weapon.WeaponLore.build(w, elements, java.util.OptionalInt.empty(),
                                java.util.OptionalInt.empty(), java.util.OptionalInt.of(score)).stream()
                        .map(c -> net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                                .serialize(c)).toList();
        assertEquals(List.of("Kinetic", "Gear Score: 100", "", "Crystal Shard  Right-Click",
                "Fires a homing crystal shard", "at the nearest enemy.", "Kinetic Damage: 16",
                "Cooldown: 0.7s | Mana Cost: 8", "", "Common Magic Weapon"), lore.apply(100));
        assertEquals("Kinetic Damage: 24", lore.apply(150).get(6), "the literal scales with the score");
    }

    /** Ben: "Keep the item and recipe". The old repo's shape: A over C over S (RecipeRegistry, cfde822). */
    @Test
    void theOldRecipeMintsIt() {
        RecipeRegistry recipes = new RecipeLoader(collecting(new ArrayList<>())).loadAll(dir("recipes"));
        assertTrue(recipes.find("flint_staff").isPresent(), "CONTROL: the recipe walk sees flint_staff");
        var r = recipes.find("crystal_shard_staff").orElseThrow(() -> new AssertionError("no recipe"));
        assertEquals("crystal_shard_staff", r.mints());
        assertEquals(List.of("A", "C", "S"), r.shape());
        assertEquals(Map.of('A', "amethyst_shard", 'C', "cobblestone", 'S', "stick"), r.ingredients());
    }

    /**
     * NAME THE QUANTITY (CLAUDE.md): what can roll on it, and whether the quantity each moves exists here. A mage
     * weapon draws {@code attunement} (effect damage, which reaches a literal damage effect: EffectApplier's Damage
     * arm) and {@code unbreaking} (effect durability). {@code amethyst_shard} has no durability, so Unbreaking rolls
     * and grants nothing: the eligibility half FAILS, as it already does on the flint staff (a stick). Pinned so that
     * the day the pool changes, this reddens and the answer is re-taken. Reported to the seat, not fixed here.
     */
    @Test
    void theEnchantPoolIsAttunementAndUnbreaking() {
        EnchantRegistry enchants = new EnchantLoader(collecting(new ArrayList<>())).loadAll(dir("enchants"));
        List<EnchantRoll.Rollable> roster = new ArrayList<>();
        for (EnchantDefinition d : enchants.all()) roster.add(new EnchantRoll.Rollable(d.id(), d.gearClass()));
        assertTrue(roster.size() >= 10, "CONTROL: the whole roster loaded, " + roster.size());
        var gearClass = io.github.butterflysmp.rpg.paper.weapon.GearItems.gearClassOf(staff(new ArrayList<>()));
        assertEquals(List.of("attunement", "unbreaking"),
                EnchantRoll.poolFor(gearClass, roster).stream().map(EnchantRoll.Rollable::enchantId).sorted().toList());
    }
}
