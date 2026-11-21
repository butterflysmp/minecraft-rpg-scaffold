package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.enchant.EnchantRoll;
import io.github.butterflysmp.rpg.core.weapon.CraftResultIndex;
import io.github.butterflysmp.rpg.core.weapon.GearClass;
import io.github.butterflysmp.rpg.core.weapon.Rarity;
import io.github.butterflysmp.rpg.core.weapon.WeaponClass;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import io.github.butterflysmp.rpg.core.weapon.WeaponRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.net.URISyntaxException;
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

import static org.junit.jupiter.api.Assertions.*;

/**
 * THE SWORD LADDER AS CONTENT (PLAN-weapon-tiers.md 1.4; the seat's brief, PR 1). Ben, 2026-11-20, verbatim: "Wood - 11 /
 * Stone - 14 / Copper - 17 / Gold - 20 / Iron - 23 / Diamond - 27 / Netherite - 31", "Yes all swords should have the
 * built in sweep", "Names plan vanilla names with the rarity color". Every figure below is read from the shipped files,
 * so a typo in any one of them reddens the row that names it.
 */
class SwordTiersContentTest {

    @TempDir
    Path dir;

    /** id, Ben's damage, display name, material, rarity, craft_result (netherite: none, it is smithed). */
    private record Tier(String id, double damage, String name, Rarity rarity, String craftResult) {}

    private static final List<Tier> LADDER = List.of(
            new Tier("wooden_sword", 11, "Wooden Sword", Rarity.COMMON, "wooden_sword"),
            new Tier("stone_sword", 14, "Stone Sword", Rarity.COMMON, "stone_sword"),
            new Tier("copper_sword", 17, "Copper Sword", Rarity.COMMON, "copper_sword"),
            new Tier("golden_sword", 20, "Golden Sword", Rarity.COMMON, "golden_sword"),
            new Tier("iron_sword", 23, "Iron Sword", Rarity.COMMON, "iron_sword"),
            new Tier("diamond_sword", 27, "Diamond Sword", Rarity.UNCOMMON, "diamond_sword"),
            new Tier("netherite_sword", 31, "Netherite Sword", Rarity.UNCOMMON, null));

    private static Path content() {
        try {
            var url = SwordTiersContentTest.class.getResource("/content");
            assertNotNull(url, "bundled content is missing from the test classpath");
            return Path.of(url.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Logger quiet(List<String> warnings) {
        Logger log = Logger.getLogger("SwordTiersContentTest-" + System.nanoTime());
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

    private static WeaponRegistry shippedWeapons(List<String> warnings) {
        return new WeaponLoader(quiet(warnings)).loadAll(content().resolve("weapons").toFile());
    }

    /** THE SEAT'S FIRST CHECK: a weapon with no flavor: at all loads cleanly, with no warning and an empty list. */
    @Test
    void aWeaponWithNoFlavourLoadsCleanly() throws Exception {
        Files.writeString(dir.resolve("plain.yml"), """
                id: plain
                display_name: "Plain"
                element: kinetic
                rarity: common
                class: melee
                material: iron_sword
                attack_damage: 23
                attack_speed: 1.6
                sweep: 0.5
                triggers:
                  left_click:
                    cast:
                      type: melee
                      reach: 3.0
                      arc_degrees: 120
                    on_hit:
                      - type: weapon_damage
                        element: kinetic
                """, StandardCharsets.UTF_8);
        List<String> warnings = new ArrayList<>();
        WeaponRegistry registry = new WeaponLoader(quiet(warnings)).loadAll(new File(dir.toString()));
        assertEquals(List.of(), warnings);
        assertEquals(List.of(), registry.find("plain").orElseThrow().flavor());
    }

    /** Each sword's number, read from its file; the shape is ironblade's basic attack, no special. */
    @Test
    void everyTierSwordCarriesBensNumberAndTheLadderShape() {
        List<String> warnings = new ArrayList<>();
        WeaponRegistry weapons = shippedWeapons(warnings);
        for (Tier tier : LADDER) {
            WeaponDefinition w = weapons.find(tier.id()).orElseThrow(() -> new AssertionError("no weapon " + tier.id()));
            assertEquals(tier.damage(), w.attackDamage(), 0.0, tier.id() + ": Ben's number");
            assertEquals(tier.name(), w.displayName(), tier.id());
            assertEquals(tier.id(), w.material(), tier.id() + ": the vanilla sword of that tier");
            assertEquals(tier.rarity(), w.rarity(), tier.id());
            assertEquals("kinetic", w.element(), tier.id());
            assertEquals(WeaponClass.MELEE, w.weaponClass(), tier.id());
            assertEquals(1.6, w.attackSpeed(), 0.0, tier.id());
            assertEquals(0.5, w.sweep(), 0.0, tier.id() + ": ironblade's built-in sweep");
            assertEquals(List.of(), w.flavor(), tier.id() + ": Ben has given no flavour, so none is authored");
            assertEquals(Optional.ofNullable(tier.craftResult()), w.craftResult(), tier.id());
            assertEquals(List.of("left_click"), w.triggers().stream().map(t -> t.input()).toList(),
                    tier.id() + ": the basic attack only, no special");
        }
        assertTrue(warnings.stream().noneMatch(m -> LADDER.stream().anyMatch(t -> m.contains("'" + t.id() + "'"))),
                "no loader warning names a tier sword: " + warnings);
    }

    /** ONE CLAIM PER SWORD MATERIAL, across ALL shipped gear: six crafted, netherite none (it is smithed, PR 2). */
    @Test
    void exactlyOneGearClaimsEachCraftedSwordAndNoneClaimsNetherite() {
        WeaponRegistry weapons = shippedWeapons(new ArrayList<>());
        List<io.github.butterflysmp.rpg.core.weapon.GearDefinition> all = new ArrayList<>(weapons.all());
        all.addAll(new ShieldLoader(quiet(new ArrayList<>())).loadAll(content().resolve("shields").toFile()).all());
        all.addAll(new ArmorLoader(quiet(new ArrayList<>())).loadAll(content().resolve("armor").toFile()).all());
        all.addAll(new ToolLoader(quiet(new ArrayList<>())).loadAll(content().resolve("tools").toFile()).all());
        for (Tier tier : LADDER) {
            long claims = all.stream().filter(g -> g.craftResult().equals(Optional.of(tier.id()))).count();
            assertEquals(tier.craftResult() == null ? 0 : 1, claims, tier.id() + " claimed by " + claims);
        }
        List<String> problems = new ArrayList<>();
        CraftResultIndex index = CraftResultIndex.build(all, List.of(), problems::add);
        assertEquals(List.of(), problems, "no contest anywhere in shipped gear");
        for (Tier tier : LADDER) {
            if (tier.craftResult() == null) {
                assertTrue(index.forResult(tier.id()).isEmpty(), tier.id());
            } else {
                assertEquals(tier.id(), index.forResult(tier.id()).orElseThrow().id(), "crafting " + tier.id());
            }
        }
    }

    // ironbladeAndEmberbladeClaimNothing DELETED 2026-11-21 with both weapons (dev weapons, deleted that day). It pinned
    // that the two were untouched by the ladder: still on iron_sword, claiming nothing. That half died with them; the
    // other half, "crafting iron mints Iron Sword", is pinned by exactlyOneGearClaimsEachCraftedSwordAndNoneClaimsNetherite
    // above (index.forResult("iron_sword") is iron_sword, across ALL shipped gear).

    /**
     * Part e: what a MINTED tier sword's tooltip reads. A minted weapon carries a gear score, and its damage line is the
     * authored figure x score / 100 (WeaponLoreTest's ruling), so Ben's number is what it reads at score 100. Pinned
     * whole, at 100 and at 150, for the iron and the netherite sword. (golden-lore.txt pins the unscored form.)
     */
    @Test
    void aMintedTierSwordsTooltipReads() {
        WeaponRegistry weapons = shippedWeapons(new ArrayList<>());
        ElementRegistry elements = new ElementLoader(quiet(new ArrayList<>())).loadAll(content().resolve("elements").toFile());
        java.util.function.BiFunction<String, Integer, List<String>> lore = (id, score) ->
                io.github.butterflysmp.rpg.paper.weapon.WeaponLore.build(weapons.find(id).orElseThrow(), elements,
                                java.util.OptionalInt.empty(), java.util.OptionalInt.empty(), java.util.OptionalInt.of(score))
                        .stream().map(c -> net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                                .serialize(c)).toList();
        assertEquals(List.of("Kinetic", "Gear Score: 100", "", "Melee Damage: 23", "Attack Speed: 1.6", "",
                "Common Melee Weapon"), lore.apply("iron_sword", 100));
        assertEquals(List.of("Kinetic", "Gear Score: 150", "", "Melee Damage: 34.5", "Attack Speed: 1.6", "",
                "Common Melee Weapon"), lore.apply("iron_sword", 150));
        assertEquals(List.of("Kinetic", "Gear Score: 100", "", "Melee Damage: 31", "Attack Speed: 1.6", "",
                "Uncommon Melee Weapon"), lore.apply("netherite_sword", 100));
    }

    /** Part d: a tier sword's enchant pool is sharpness and unbreaking, from the shipped enchant files, by class. */
    @Test
    void aTierSwordIsOfferedSharpnessAndUnbreakingOnly() {
        EnchantRegistry enchants = new EnchantLoader(quiet(new ArrayList<>())).loadAll(content().resolve("enchants").toFile());
        List<EnchantRoll.Rollable> roster = new ArrayList<>();
        for (EnchantDefinition d : enchants.all()) roster.add(new EnchantRoll.Rollable(d.id(), d.gearClass()));
        assertTrue(roster.size() >= 10, "the control: the whole roster loaded, " + roster.size());
        WeaponRegistry weapons = shippedWeapons(new ArrayList<>());
        for (String id : List.of("iron_sword", "netherite_sword")) {
            GearClass gearClass = io.github.butterflysmp.rpg.paper.weapon.GearItems.gearClassOf(weapons.find(id).orElseThrow());
            List<String> pool = EnchantRoll.poolFor(gearClass, roster).stream().map(EnchantRoll.Rollable::enchantId).sorted().toList();
            assertEquals(List.of("sharpness", "unbreaking"), pool, id);
        }
    }
}
