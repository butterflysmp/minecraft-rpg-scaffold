package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.enchant.EnchantEffect;
import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.ability.ResourceCost;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.weapon.Rarity;
import io.github.butterflysmp.rpg.core.weapon.TriggerBinding;
import io.github.butterflysmp.rpg.core.weapon.WeaponClass;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import io.github.butterflysmp.rpg.core.ability.effect.DamagePayload;
import io.github.butterflysmp.rpg.core.weapon.WeaponLoreLines;
import io.github.butterflysmp.rpg.core.weapon.WeaponRegistry;
import io.github.butterflysmp.rpg.core.enchant.EnchantState;
import io.github.butterflysmp.rpg.paper.content.ElementDefinition;
import io.github.butterflysmp.rpg.paper.content.EnchantDefinition;
import io.github.butterflysmp.rpg.paper.content.EnchantRegistry;
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

    /** A costed special alongside the basic attack: a literal payload, so an ABILITY block. */
    private static WeaponDefinition swordWithFireball() {
        AbilityDefinition slash = new AbilityDefinition(
                "emberblade/left_click", "Ember Slash", "fire", "none",
                10, ResourceCost.FREE, new CastSpec.Melee(3.5, 120),
                List.of(new EffectSpec.WeaponDamage("fire")), List.of("A cut that smoulders."));
        AbilityDefinition fireball = new AbilityDefinition(
                "emberblade/right_click", "Fireball", "fire", "none",
                60, new ResourceCost("mana", 40), new CastSpec.Projectile(1.6, 0.03, 100),
                List.of(new EffectSpec.Burst(3.0, List.of(new EffectSpec.Damage(12, "fire")))),
                List.of("Hurl a bursting ember."));
        return new WeaponDefinition("emberblade", "Emberblade", "fire", Rarity.RARE,
                WeaponClass.MELEE, "iron_sword", 7.0,
                List.of(new TriggerBinding("left_click", slash),
                        new TriggerBinding("right_click", fireball)),
                List.of("Flavour."));
    }

    /** A bow shape: one literal-damage trigger, no weapon_damage anywhere -- so NO stat block. */
    private static WeaponDefinition bowWithNoBasicAttack() {
        AbilityDefinition shot = new AbilityDefinition(
                "hunters_bow/right_click", "Quick Shot", "fire", "none",
                15, ResourceCost.FREE, new CastSpec.Projectile(2.5, 0.05, 60),
                List.of(new EffectSpec.Damage(6, "fire")), List.of("A swift arrow."));
        return new WeaponDefinition("hunters_bow", "Hunter's Bow", "fire", Rarity.UNCOMMON,
                WeaponClass.RANGER, "bow", 0.0,
                List.of(new TriggerBinding("right_click", shot)), List.of());
    }

    private static List<String> textLines(List<Component> lore) {
        return lore.stream().map(WeaponLoreTest::textOf).toList();
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

    // --- basic attack renders as a STAT BLOCK, not an ability section ---

    /**
     * The whole point of this pass. A basic attack is a stat: two lines, no gold ability name, no
     * authored prose, no cadence line. The prose is deliberately still present on the definition
     * here -- the renderer must ignore it on a weapon_damage trigger, not merely happen to have
     * empty content.
     */
    @Test
    void aBasicAttackRendersAsTwoStatLinesWithNoAbilitySection() {
        List<String> lines = textLines(WeaponLore.build(rareFireSword(), elementsWithFire()));

        assertTrue(lines.contains("Melee Damage: 7"), () -> "expected a class-typed stat line in " + lines);
        assertTrue(lines.contains("Attack Speed: 2.0"),
                () -> "10 ticks between swings is 2.0 attacks/sec; got " + lines);
        assertFalse(lines.contains("Ember Slash  Left-Click"),
                () -> "a basic attack must not render a gold ability line; got " + lines);
        assertFalse(lines.contains("A cut that smoulders."),
                () -> "authored prose on a basic attack must not render; got " + lines);
        assertFalse(lines.stream().anyMatch(l -> l.startsWith("Cooldown:")),
                () -> "a basic attack shows Attack Speed, never a cooldown; got " + lines);
    }

    /** The bow: a RANGED basic attack on right-click, behind a leading visual. */
    private static WeaponDefinition huntersBow() {
        AbilityDefinition shot = new AbilityDefinition(
                "hunters_bow/right_click", "Hunter's Bow", "fire", "none",
                15, ResourceCost.FREE, new CastSpec.Projectile(2.5, 0.05, 60),
                List.of(new EffectSpec.Visual("solar_detonation"), new EffectSpec.WeaponDamage("fire")),
                List.of());
        return new WeaponDefinition("hunters_bow", "Hunter's Bow", "fire", Rarity.UNCOMMON,
                WeaponClass.RANGER, "bow", 6.0,
                List.of(new TriggerBinding("right_click", shot)),
                List.of("A swift arrow wreathed in flame."));
    }

    /**
     * The bow's RANGED stat block: a class-labelled damage line off its attack_damage, an attack
     * speed, and nothing else -- no gold name, no prose, no cadence, and NO input label.
     *
     * The stat block carries no input on any weapon, left-click or not. A bow shooting on
     * right-click is universal knowledge, so labelling it reads as clutter in-game; the tooltip
     * says what the weapon IS, not which button to press. The damage assertion is exact-line
     * equality rather than startsWith precisely so that appending anything to it reddens here.
     */
    @Test
    void aRangedBasicAttackRendersAStatBlockWithNoInputLabel() {
        List<String> lines = textLines(WeaponLore.build(huntersBow(), elementsWithFire()));

        assertTrue(lines.contains("Ranged Damage: 6"),
                () -> "the bow's shot is class-labelled RANGED, off its attack_damage, and bare; got " + lines);
        assertTrue(lines.contains("Attack Speed: 1.3"),
                () -> "15 ticks between shots is ~1.3 shots/sec; got " + lines);
        assertFalse(lines.stream().anyMatch(l -> l.contains("Right-Click")),
                () -> "a stat block never names its input, not even a non-default one; got " + lines);
        assertFalse(lines.stream().anyMatch(l -> l.startsWith("Cooldown:")),
                () -> "a basic attack shows Attack Speed, never a cooldown; got " + lines);
        assertTrue(lines.contains("A swift arrow wreathed in flame."),
                () -> "the prose relocated to weapon flavour must still render; got " + lines);
    }

    /**
     * The label collision this pass resolves. The stat line and the fireball are both damage, but
     * only the stat line reads the ATTACK_DAMAGE stat -- so only it may wear the CLASS label. The
     * fireball is a literal and wears its ELEMENT, or the tooltip promises a "+N Melee Damage"
     * modifier relationship that cannot exist.
     */
    @Test
    void anAbilityPayloadIsLabelledByItsElementNotTheWeaponsClass() {
        List<String> lines = textLines(WeaponLore.build(swordWithFireball(), elementsWithFire()));

        assertTrue(lines.contains("Fire Damage: 12"), () -> "expected an element-typed line in " + lines);
        assertFalse(lines.contains("Melee Damage: 12"),
                () -> "a literal payload reads no stat, so it must not wear the class label; got " + lines);
        // The ability block itself is otherwise untouched.
        assertTrue(lines.contains("Fireball  Right-Click"), () -> lines.toString());
        assertTrue(lines.contains("Hurl a bursting ember."), () -> lines.toString());
        assertTrue(lines.contains("Cooldown: 3.0s | Mana Cost: 40"), () -> lines.toString());
        // ...and the basic attack still owns the class label, exactly once.
        assertEquals(1, lines.stream().filter(l -> l.startsWith("Melee Damage: ")).count(),
                () -> "exactly one class-labelled damage line: " + lines);
        assertTrue(lines.contains("Melee Damage: 7"), () -> lines.toString());
    }

    /**
     * The tooltip shows the WEAPON's base attack speed, never the holder's resolved stat.
     *
     * This is the rule that makes lore mint-time safe and non-drifting -- "lore describes the weapon,
     * not whoever is holding it" -- and it is worth a test of its own precisely because wiring the
     * resolved stat in would feel like an improvement. A 10-tick basic attack reads 2.0 for everyone,
     * whether they are swinging at 1.0 or 5.0, so a tooltip minted for one player cannot mislead the
     * next one who picks the item up.
     *
     * Structurally guaranteed today: WeaponLore.build takes only the weapon and the element registry
     * -- there is no CombatantStats in reach to read. If that ever changes, this test is the reason
     * to think twice.
     */
    @Test
    void theTooltipShowsTheWeaponsBaseSpeedNotAnyHoldersResolvedStat() {
        List<String> lines = textLines(WeaponLore.build(rareFireSword(), elementsWithFire()));

        assertTrue(lines.contains("Attack Speed: 2.0"),
                () -> "10 cooldown_ticks is 2.0 attacks/sec, for every holder; got " + lines);
        assertEquals(1, lines.stream().filter(l -> l.startsWith("Attack Speed: ")).count(),
                () -> "exactly one attack-speed line: " + lines);
    }

    @Test
    void aWeaponWithNoBasicAttackRendersNoStatBlock() {
        List<String> lines = textLines(WeaponLore.build(bowWithNoBasicAttack(), elementsWithFire()));

        assertFalse(lines.stream().anyMatch(l -> l.startsWith("Ranged Damage:")),
                () -> "no weapon_damage trigger means no stat block at all; got " + lines);
        assertFalse(lines.stream().anyMatch(l -> l.startsWith("Attack Speed:")),
                () -> "no basic attack means no attack speed; got " + lines);
        // Its shot is an ability block, element-typed like any other literal payload.
        assertTrue(lines.contains("Quick Shot  Right-Click"), () -> lines.toString());
        assertTrue(lines.contains("Fire Damage: 6"), () -> lines.toString());
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

            // No weapon may show another class's damage label -- the label comes from its own class.
            List<String> lines = textLines(lore);
            String ownLabel = WeaponClassLabel.of(weapon.weaponClass()) + " Damage: ";
            for (String other : new String[]{"Melee Damage: ", "Ranged Damage: ", "Magic Damage: "}) {
                if (other.equals(ownLabel)) continue;
                assertFalse(lines.stream().anyMatch(l -> l.startsWith(other)),
                        () -> weapon.id() + " (" + weapon.weaponClass() + ") must not render " + other
                                + "; got " + lines);
            }

            // A class label appears at most once, and only on a weapon that HAS a basic attack.
            long classLabelled = lines.stream().filter(l -> l.startsWith(ownLabel)).count();
            boolean hasBasicAttack = weapon.triggers().stream().anyMatch(t ->
                    DamagePayload.of(t.ability().onHit(), weapon.attackDamage())
                            .map(d -> d.source() == DamagePayload.DamageSource.WEAPON_STAT)
                            .orElse(false));
            assertEquals(hasBasicAttack ? 1 : 0, classLabelled,
                    () -> weapon.id() + " should have " + (hasBasicAttack ? "exactly one" : "no")
                            + " class-labelled damage line; got " + lines);
            assertEquals(hasBasicAttack,
                    lines.stream().anyMatch(l -> l.startsWith("Attack Speed: ")),
                    () -> weapon.id() + ": attack speed must appear iff it has a basic attack; got " + lines);
        }

        // Ironblade, Emberblade and now Hunter's Bow are the weapon_damage weapons in shipped
        // content. If that ever stops being true this test has quietly stopped covering the stat
        // block. The bow joining them is the point of the cast-time-snapshot pass: a projectile
        // basic attack was impossible while WeaponDamage resolved its amount at hit time.
        assertEquals(3, weapons.all().stream().filter(w -> w.triggers().stream().anyMatch(t ->
                        DamagePayload.of(t.ability().onHit(), w.attackDamage())
                                .map(d -> d.source() == DamagePayload.DamageSource.WEAPON_STAT)
                                .orElse(false))).count(),
                "exactly three shipped weapons have a basic attack (ironblade, emberblade, hunters_bow)");
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

    @Test
    void theRarityFooterIsStillLastOnceAnEnchantBlockIsApplied() {
        // The enchant block is prepended at index 0 specifically so this stays true. The footer's
        // "always last" promise is what a player reads a weapon's tier off, and an enchant block
        // appended to the end would quietly push it up one line on every enchanted weapon.
        EnchantRegistry enchants = new EnchantRegistry();
        enchants.register(new EnchantDefinition("unbreaking", "Unbreaking", 3, EnchantEffect.DURABILITY, null, List.of()));
        EnchantState state = EnchantState.empty()
                .addCandidate(0, "unbreaking").withLevel(0, 0, 3).withActive(0, 0);

        List<Component> base = WeaponLore.build(rareFireSword(), elementsWithFire());
        List<Component> lore = EnchantLore.applied(base, EnchantLore.lines(state, enchants));

        assertEquals("Unbreaking III", textOf(lore.get(0)), "the enchant block leads");
        assertEquals("Rare Melee Weapon", textOf(lore.get(lore.size() - 1)),
                "and the rarity footer is still the last line");
        assertEquals(RarityColors.of(Rarity.RARE), colorOf(lore.get(lore.size() - 1)),
                "still in its tier's colour");
        // Mutation: append the enchant block instead of prepending -> the footer is no longer last
        // -> reddens.
    }

    @Test
    void aWeaponWithNoActiveEnchantRendersExactlyTheLoreItAlwaysDid() {
        // The regression guard for EVERY unenchanted weapon in the game -- which today is all of
        // them. Threading an enchant applier through mint() must be a strict no-op when there is
        // nothing active, down to the absence of a leading blank line.
        List<Component> base = WeaponLore.build(rareFireSword(), elementsWithFire());
        List<Component> applied = EnchantLore.applied(
                base, EnchantLore.lines(EnchantState.empty(), new EnchantRegistry()));

        assertEquals(textLines(base), textLines(applied));
        // Mutation: always insert the blank separator -> every plain weapon grows a leading empty
        // line -> reddens.
    }
}
