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
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE LIVING LANTERN, AS SHIPPED: Ben's numbers and his recipe, read from the real files (the seat's brief of
 * 2026-11-22, PR 3), and every SEAT PLACEHOLDER pinned, so a retune is a deliberate edit here as well as in the content.
 *
 * <p>Ben, 2026-11-22, verbatim: "An area of effect arc in front of the player that travels for about 6 blocks dealing a
 * decent bit of damage. / 2. Fire element / 3. 12 mana 0.8s second cooldown 14 damage 60 degree arc / 4. Keep the lantern
 * item", and the recipe "I = Iron Nugget / F = Fire Charge / I I I / I F I / I I I".
 */
class LivingLanternContentTest {

    private static Logger collecting(List<String> warnings) {
        Logger log = Logger.getLogger("LivingLanternContentTest-" + System.nanoTime());
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

    private static WeaponDefinition lantern(List<String> warnings) {
        WeaponRegistry weapons = new WeaponLoader(collecting(warnings)).loadAll(dir("weapons"));
        assertTrue(weapons.find("flint_staff").isPresent(), "CONTROL: the weapon walk sees the flint staff");
        return weapons.find("living_lantern")
                .orElseThrow(() -> new AssertionError("weapons/living_lantern.yml did not load: " + warnings));
    }

    private static AbilityDefinition breath(WeaponDefinition w) {
        return w.trigger("right_click").orElseThrow(() -> new AssertionError("no right_click trigger")).ability();
    }

    @Test
    void bensNumbers() {
        List<String> warnings = new ArrayList<>();
        WeaponDefinition w = lantern(warnings);
        assertEquals("fire", w.element(), "Ben: Fire element");
        assertEquals("lantern", w.material(), "Ben: keep the lantern item");
        assertEquals(Optional.empty(), w.craftResult(), "the recipe claims the weapon, never a craft_result");
        assertEquals(List.of("right_click"), w.triggers().stream().map(t -> t.input()).toList(), "one cast");

        AbilityDefinition a = breath(w);
        assertEquals(16, a.cooldownTicks(), "Ben: 0.8 s");
        assertEquals("mana", a.cost().resourceId());
        assertEquals(12.0, a.cost().amount(), 0.0, "Ben: 12 mana");
        List<EffectSpec.Damage> damage = a.onHit().stream()
                .filter(e -> e instanceof EffectSpec.Damage).map(e -> (EffectSpec.Damage) e).toList();
        assertEquals(List.of(new EffectSpec.Damage(45, "fire")), damage, "Ben (2026-11-23): 45, fire, a literal damage");
        CastSpec.Projectile shot = assertInstanceOf(CastSpec.Projectile.class, a.cast(),
                "Ben (2026-11-23): a slow moving homing projectile, 1 shot");
        assertEquals(0.35, shot.speed(), 0.0, "Ben: 7 blocks a second = 7 / 20 a tick");
        assertEquals(7.0, shot.homing().searchRadius(), 0.0, "Ben: homing within 7");
        assertEquals(null, shot.spread(), "Ben: 1 shot instead of three");
        assertTrue(warnings.stream().noneMatch(m -> m.contains("living_lantern")), "no loader warning: " + warnings);
    }

    /**
     * BEN'S 28 BLOCKS OF REACH, AS THE ENGINE CAPS IT: by LIFETIME (ProjectileFlight has no distance cap). Homing keeps the
     * velocity's length, so travel is speed x ticks whatever it chases: 0.35 x 80 = 28.0. Executed here, not predicted.
     */
    @Test
    void theReachIsTwentyEightBlocksOfTravel() {
        CastSpec.Projectile shot = (CastSpec.Projectile) breath(lantern(new ArrayList<>())).cast();
        assertEquals(80, shot.maxLifetimeTicks());
        assertEquals(0.0, shot.gravity(), 0.0, "no gravity, so no speed is gained or lost falling");
        assertEquals(28.0, shot.speed() * shot.maxLifetimeTicks(), 1e-9, "Ben: reach 28 blocks");
    }

    /** Each SEAT PLACEHOLDER, pinned (2026-11-22's kept, 2026-11-23's new). */
    @Test
    void theSeatsPlaceholders() {
        WeaponDefinition w = lantern(new ArrayList<>());
        assertEquals("Living Lantern", w.displayName());
        assertEquals(Rarity.COMMON, w.rarity(), "the iron sword's");
        assertEquals(WeaponClass.MAGE, w.weaponClass());
        AbilityDefinition a = breath(w);
        assertEquals("Lantern's Breath", a.displayName(), "kept by the brief");
        assertEquals(new CastSpec.Projectile(0.35, 0.0, 80, "lantern_front", null, new CastSpec.Homing(0.45, 0, 7), null),
                a.cast(), "no body, the cone's front reused as the trail, the old repo's 0.45 turn, seeking from the muzzle");
        assertEquals(List.of(new EffectSpec.Visual("lantern_cast")), a.onCast());
        assertEquals(List.of(new EffectSpec.Visual("lantern_hit"), new EffectSpec.Damage(45, "fire")), a.onHit(),
                "one strike of 45, nothing else on the hit");
    }

    /**
     * THE CONE STAYS IN THE ENGINE, AND NO SHIPPED WEAPON USES IT NOW (the brief: "say that no shipped weapon uses it
     * now"). Over every shipped weapon and trigger, by the loaded cast, never a text grep of `type: cone`.
     */
    @Test
    void noShippedWeaponCastsACone() {
        WeaponRegistry weapons = new WeaponLoader(collecting(new ArrayList<>())).loadAll(dir("weapons"));
        int triggers = 0;
        for (WeaponDefinition w : weapons.all()) {
            for (var t : w.triggers()) {
                triggers++;
                assertTrue(!(t.ability().cast() instanceof CastSpec.Cone), w.id() + " " + t.input() + " casts a cone");
            }
        }
        assertTrue(triggers > 20, "CONTROL: the walk saw the shipped triggers, " + triggers);
    }

    @Test
    void itsThreeVisualsLoad() {
        List<String> warnings = new ArrayList<>();
        VisualRegistry visuals = new VisualLoader(collecting(warnings)).loadAll(dir("visuals"));
        assertTrue(visuals.find("flint_impact").isPresent(), "CONTROL: the visual walk sees flint_impact");
        for (String id : List.of("lantern_cast", "lantern_front", "lantern_hit")) {
            assertTrue(visuals.find(id).isPresent(), id + " did not load: " + warnings);
        }
        assertTrue(warnings.stream().noneMatch(m -> m.contains("lantern_")), warnings.toString());
    }

    /**
     * What a MINTED lantern's tooltip reads (the gate's LL1 predicts this, word for word). The cooldown line formats the
     * authored 16 as Ben's 0.8 s; the damage is 45 x score / 100 (14 until the 2026-11-23 rework).
     */
    @Test
    void aMintedLanternsTooltipReads() {
        WeaponDefinition w = lantern(new ArrayList<>());
        ElementRegistry elements = new ElementLoader(collecting(new ArrayList<>())).loadAll(dir("elements"));
        java.util.function.IntFunction<List<String>> lore = score ->
                io.github.butterflysmp.rpg.paper.weapon.WeaponLore.build(w, elements, java.util.OptionalInt.empty(),
                                java.util.OptionalInt.empty(), java.util.OptionalInt.of(score)).stream()
                        .map(c -> net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                                .serialize(c)).toList();
        assertEquals(List.of("Fire", "Gear Score: 100", "", "Lantern's Breath  Right-Click",
                "Looses a slow, seeking flame", "that hunts within 7 blocks.", "Magic Damage: 45",
                "Cooldown: 0.8s | Mana Cost: 12", "", "Common Magic Weapon"), lore.apply(100));
        assertEquals("Magic Damage: 67.5", lore.apply(150).get(6), "the literal scales with the score");
    }

    /** BEN's recipe: eight iron nuggets round one fire charge. */
    @Test
    void bensRecipeMintsIt() {
        RecipeRegistry recipes = new RecipeLoader(collecting(new ArrayList<>())).loadAll(dir("recipes"));
        assertTrue(recipes.find("flint_staff").isPresent(), "CONTROL: the recipe walk sees flint_staff");
        var r = recipes.find("living_lantern").orElseThrow(() -> new AssertionError("no recipe"));
        assertEquals("living_lantern", r.mints());
        assertEquals(List.of("III", "IFI", "III"), r.shape());
        assertEquals(Map.of('I', "iron_nugget", 'F', "fire_charge"), r.ingredients());
    }

    /**
     * NAME THE QUANTITY (CLAUDE.md), for every enchant that can roll. A mage weapon draws {@code attunement} and
     * {@code unbreaking}.
     * <ul>
     *   <li><b>attunement</b> moves EFFECT DAMAGE. ARITHMETIC: the quantity is the authored 14, continuous, and
     *       attunement's multiplier reaches a literal {@code damage} effect ({@code EffectApplier}'s Damage arm, as on
     *       the other staves). ELIGIBILITY: the lantern authors one. PASSES.</li>
     *   <li><b>unbreaking</b> moves DURABILITY. ELIGIBILITY: a {@code lantern} has no vanilla durability, so it rolls and
     *       grants nothing. FAILS, as on the flint, crystal and knell staves; carried open, not fixed here.</li>
     * </ul>
     * Pinned so the day the pool changes this reddens and the answer is re-taken.
     */
    @Test
    void theEnchantPoolIsAttunementAndUnbreaking() {
        EnchantRegistry enchants = new EnchantLoader(collecting(new ArrayList<>())).loadAll(dir("enchants"));
        List<EnchantRoll.Rollable> roster = new ArrayList<>();
        for (EnchantDefinition d : enchants.all()) roster.add(new EnchantRoll.Rollable(d.id(), d.gearClass()));
        assertTrue(roster.size() >= 10, "CONTROL: the whole roster loaded, " + roster.size());
        var gearClass = io.github.butterflysmp.rpg.paper.weapon.GearItems.gearClassOf(lantern(new ArrayList<>()));
        assertEquals(List.of("attunement", "unbreaking"),
                EnchantRoll.normalPoolFor(EnchantRoll.Has.EVERY, gearClass, roster).stream().map(EnchantRoll.Rollable::enchantId).sorted().toList());
    }

    /**
     * RULING 7: a minted weapon is never placed. {@code RpgListeners.onWeaponPlace} cancels a BlockPlaceEvent whose item
     * carries a weapon id, whatever the hand. Read from the source, bounded at the next member. A lantern is the first
     * weapon material that places, so no earlier row could see this.
     */
    @Test
    void aMintedWeaponIsNeverPlaced() throws IOException {
        String src = Files.readString(Path.of("src/main/java/io/github/butterflysmp/rpg/paper/listener/RpgListeners.java"),
                StandardCharsets.UTF_8).replace("\r", "");
        int start = src.indexOf("public void onWeaponPlace(org.bukkit.event.block.BlockPlaceEvent event)");
        assertTrue(start > 0, "the placement guard is not found: the scan is blind");
        assertTrue(src.substring(src.lastIndexOf("@EventHandler", start), start).startsWith("@EventHandler"),
                "it is an event handler");
        int end = src.indexOf("\n    }\n", start);
        String body = src.substring(start, end);
        assertTrue(body.contains("WeaponItems.weaponId(event.getItemInHand(), adapters.keys())"), "keyed by weapon id");
        assertTrue(body.contains("event.setCancelled(true);"), "and it cancels");
    }
}
