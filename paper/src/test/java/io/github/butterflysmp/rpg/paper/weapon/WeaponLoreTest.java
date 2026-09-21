package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.combat.SweepShare;
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
import java.util.OptionalInt;
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
                WeaponClass.MELEE, "iron_sword", 7.0, 1.6, SweepShare.NONE,
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
                WeaponClass.MELEE, "iron_sword", 7.0, 1.6, SweepShare.NONE,
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
                WeaponClass.RANGER, "bow", 0.0, 0.0, SweepShare.NONE,
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
        assertTrue(lines.contains("Attack Speed: 1.6"),
                () -> "the authored attack_speed, not a derived one; got " + lines);
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
                WeaponClass.RANGER, "bow", 6.0, 0.0, SweepShare.NONE,
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
     * resolved stat in would feel like an improvement, and is now genuinely reachable: since Stage 2
     * the holder HAS a resolved attack speed, and it is written onto their vanilla attribute a few
     * lines away. A weapon authoring 1.6 reads 1.6 for everyone, whether they swing at 1.6 or 3.2,
     * so a tooltip minted for one player cannot mislead the next one who picks the item up.
     *
     * Structurally guaranteed today: WeaponLore.build takes only the weapon and the element registry
     * -- there is no CombatantStats in reach to read. If that ever changes, this test is the reason
     * to think twice.
     */
    @Test
    void theTooltipShowsTheWeaponsBaseSpeedNotAnyHoldersResolvedStat() {
        List<String> lines = textLines(WeaponLore.build(rareFireSword(), elementsWithFire()));

        assertTrue(lines.contains("Attack Speed: 1.6"),
                () -> "the weapon authors 1.6, and reads 1.6 for every holder; got " + lines);
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

    // ------------------------------------------------------------------ the quiver line's denominator

    /** A ranger weapon with a magazine: authored capacity 9, so the stamp has something to differ from. */
    private static WeaponDefinition quiverWeapon() {
        AbilityDefinition shot = new AbilityDefinition(
                "quiver_stone/right_click", "Loose", "kinetic", "none",
                11, ResourceCost.FREE, new CastSpec.Ray(23),
                List.of(new EffectSpec.Damage(13.0, "kinetic")), List.of());
        return new WeaponDefinition("quiver_stone", "Quiver Stone", "kinetic", Rarity.EXOTIC,
                WeaponClass.RANGER, "crossbow", 0.0, 0.0, SweepShare.NONE, 9, 34,
                List.of(new TriggerBinding("right_click", shot)), List.of());
    }

    /**
     * THE STAMPED CAPACITY REACHES THE RENDERED LINE, AND NOTHING ELSE IN THE SUITE PROVES IT.
     *
     * <p><b>This row exists because a mutation came back green that should not have.</b>
     * {@code MUTSTAMPDROP} replaced {@code capacityOf(stampedCapacity, …)} with
     * {@code capacityOf(OptionalInt.empty(), …)} — making the tooltip ignore the stamp forever — and
     * the ENTIRE suite passed: golden, {@code WeaponLoreTest}, {@code QuiverStateTest} and the
     * resolver guard alike.
     *
     * <p><b>The reason is that every other caller passes an EMPTY stamp.</b> {@code GoldenLoreTest}
     * and every pre-existing row here use the 2-arg overload, which supplies
     * {@code OptionalInt.empty()}; the 4-arg form had <b>no test caller at all</b>. So the golden's
     * byte-identity — offered as commit 1b's proof — licenses exactly one thing: <i>the definitions
     * path did not move.</i> That is worth having and it is <b>not</b> evidence the seam works,
     * because the seam is the path the golden does not walk. A control that succeeds for the wrong
     * reason.
     *
     * <p>The discriminating staging is a stamp that DIFFERS from the authored value: 11 against a
     * weapon declaring 9. Under the mutation this renders {@code 8/9}.
     */
    @Test
    void aStampedCapacityDifferentFromTheAuthoredOneReachesTheTooltip() {
        List<String> lines = textLines(WeaponLore.build(quiverWeapon(), elementsWithFire(),
                OptionalInt.of(8), OptionalInt.of(11)));

        assertTrue(lines.contains("Quiver: 8/11"),
                () -> "the STAMP must be the denominator, not the weapon's authored 9; got " + lines);
        assertFalse(lines.contains("Quiver: 8/9"),
                () -> "rendering the authored capacity means gear is invisible to its owner; got " + lines);
    }

    /**
     * And the fallback, pinned beside it so the PAIR discriminates.
     *
     * <p>Alone this row is what the golden already covers. Beside the row above it is what makes
     * "the stamp wins when present, the definition answers when absent" a checkable statement rather
     * than two facts that happen to hold.
     */
    @Test
    void anAbsentStampFallsBackToTheAuthoredCapacityOnTheTooltip() {
        List<String> lines = textLines(WeaponLore.build(quiverWeapon(), elementsWithFire(),
                OptionalInt.of(8), OptionalInt.empty()));

        assertTrue(lines.contains("Quiver: 8/9"),
                () -> "no item to read, so the weapon's own number is the true answer; got " + lines);
    }

    // ---------------------------------------------------------------- gear score on the tooltip

    /**
     * *** EVERY ROW ABOVE THIS LINE RENDERS AT NO SCORE, AND THAT IS WHY NONE OF THEM COULD SEE
     * THIS DEFECT. ***
     *
     * <p>The thirteen rows that predate slice 12c all call the two-argument {@code build}, which
     * passes {@code OptionalInt.empty()}. {@code GearScore.orAbsent(empty)} is the baseline, and
     * scaling by the baseline is the identity -- so <b>the entire existing surface is numerically
     * blind to the score axis, and stayed green while the tooltip under-reported for two slices.</b>
     * {@code golden-lore.txt} is blind for the same reason: it renders through the definitions-only
     * overloads.
     *
     * <p>So these rows stage a NON-IDENTITY score, and every one of them chooses its numbers so
     * that <b>no two quantities the row reads are equal</b> -- the authored figure, the score and
     * the product are three different numbers in each, so a row cannot pass by reading the wrong one.
     */
    @Test
    void aScoredWeaponRendersItsClassDamageScaledByTheItemsOwnScore() {
        List<String> lines = textLines(WeaponLore.build(rareFireSword(), elementsWithFire(),
                OptionalInt.empty(), OptionalInt.empty(), OptionalInt.of(340)));

        // 7.0 authored x 340/100 = 23.8. Three distinct numbers: 7, 340, 23.8.
        assertTrue(lines.contains("Melee Damage: 23.8"),
                () -> "the stat block must show what the weapon DEALS at this score; got " + lines);
        assertFalse(lines.contains("Melee Damage: 7"),
                () -> "7 is the authored figure and is what this slice exists to stop showing; got " + lines);
        assertTrue(lines.contains("Gear Score: 340"),
                () -> "the score line explains the damage line, so both or neither; got " + lines);
    }

    /**
     * *** THE ORDER OF THE ELEMENT AND SCORE LINES, AND IT IS THE ONLY ROW THAT CAN SEE IT. ***
     *
     * <p>Ben ruled on 2026-09-21 that the score sits BELOW the element. {@code WeaponLore} keeps the
     * overturned power-vs-identity argument beside the call; this row is what makes the order a
     * GUARANTEE rather than a comment somebody can reason their way back out of.
     *
     * <p><b>Measured as a mutation-as-probe, BEFORE this row existed:</b> the reorder was applied to
     * a finished tree and the full suite stayed GREEN at <b>2084</b>. The position was unguarded
     * outright -- not weakly covered, not covered by accident.
     *
     * <p><b>Why nothing else could see it, which is the part worth keeping:</b>
     * {@code golden-lore.txt} carries <b>zero</b> {@code Gear Score} lines, so it is blind by
     * construction -- the same definitions-only blindness slice 12c recorded for scaled damage. And
     * the four {@code lore.get(0)} rows above call the two-argument {@code build}, where no score
     * line prints at all: the element sits at index 0 under BOTH orders, so every one of them
     * passes either way. <i>A row that passes under both orders is not a test of this change.</i>
     *
     * <p>So this row stages a REAL score, which is the only condition under which both lines print
     * and an order exists to be wrong.
     */
    @Test
    void theScoreLineRendersBelowTheElementLineNotAboveIt() {
        List<String> lines = textLines(WeaponLore.build(rareFireSword(), elementsWithFire(),
                OptionalInt.empty(), OptionalInt.empty(), OptionalInt.of(340)));

        int element = lines.indexOf("Fire");
        int score = lines.indexOf("Gear Score: 340");

        // BOTH MUST PRINT, or the comparison below is vacuous: a missing line yields -1, and -1 is
        // less than every index, so an absent score line would satisfy the order assertion for the
        // one reason that is not an order at all.
        assertTrue(element >= 0,
                () -> "the element line must print for this row to mean anything; got " + lines);
        assertTrue(score >= 0,
                () -> "the score line must print for this row to mean anything; got " + lines);

        assertTrue(element < score,
                () -> "Ben's ruling of 2026-09-21: the element leads and the score sits under it; got " + lines);
    }

    /**
     * NOT ROUNDED, and the fixture is chosen so rounding would be visible.
     *
     * <p>{@code 23.8} is exactly what {@code WeaponAttackItems} writes onto the attribute. Rounding
     * to {@code 24} on the tooltip would reintroduce this slice's own defect one decimal place
     * smaller -- a number the player sees that the system does not produce.
     */
    @Test
    void aScaledDamageFigureKeepsItsFractionRatherThanRounding() {
        List<String> lines = textLines(WeaponLore.build(rareFireSword(), elementsWithFire(),
                OptionalInt.empty(), OptionalInt.empty(), OptionalInt.of(340)));

        assertFalse(lines.contains("Melee Damage: 24"),
                () -> "rounding is the same lie, smaller; got " + lines);
    }

    /** The OTHER render site: an ability's literal payload, scaled by the same score. */
    @Test
    void aScoredWeaponRendersItsAbilityDamageScaledToo() {
        List<String> lines = textLines(WeaponLore.build(swordWithFireball(), elementsWithFire(),
                OptionalInt.empty(), OptionalInt.empty(), OptionalInt.of(175)));

        // 12 authored x 175/100 = 21. Three distinct numbers: 12, 175, 21.
        assertTrue(lines.contains("Fire Damage: 21"),
                () -> "the ability block reads the literal amount, scaled; got " + lines);
        assertFalse(lines.contains("Fire Damage: 12"),
                () -> "12 is the authored literal; got " + lines);
    }

    /**
     * *** THE EXCLUSION, AND IT IS THE ROW R13 CANNOT SEE. ***
     *
     * <p>R13 boots a stamped {@code volley_stone} and reads its DAMAGE, which is correct and says
     * nothing about the tooltip. <b>A build that scales the display while the exclusion holds at
     * runtime passes R13 and lies on the screen</b> -- so this is the display half, and the two
     * together are what make the claim whole.
     *
     * <p><b>The stamp is REAL and present.</b> Stones minted between slice 12 and 12b carry one, so
     * {@code orAbsent} alone cannot distinguish "declared unscored" from "carries no stamp" -- and
     * {@code scaledDamage(4, 400)} is {@code 16}, which is what a build without the door renders.
     */
    @Test
    void anUnscoredWeaponRendersItsAuthoredDamageEvenWithARealStamp() {
        List<String> lines = textLines(WeaponLore.build(unscoredStone(), elementsWithFire(),
                OptionalInt.empty(), OptionalInt.empty(), OptionalInt.of(400)));

        // 4 authored, score 400, and 16 is the number a missing door produces. All three distinct.
        assertTrue(lines.contains("Kinetic Damage: 4 x 3"),
                () -> "an unscored weapon shows what it deals: the AUTHORED 4; got " + lines);
        assertFalse(lines.stream().anyMatch(l -> l.contains("16")),
                () -> "16 is scaledDamage(4, 400) -- the door did not fire; got " + lines);
        assertFalse(lines.stream().anyMatch(l -> l.startsWith("Gear Score:")),
                () -> "the label promises a contribution to the average, which this item is refused; got " + lines);
    }

    /** The multiplier and the scaling are independent, so one row stages BOTH moving at once. */
    @Test
    void aScoredVolleyRendersBothItsScaledNumberAndItsShotCount() {
        List<String> lines = textLines(WeaponLore.build(scoredVolley(), elementsWithFire(),
                OptionalInt.empty(), OptionalInt.empty(), OptionalInt.of(250)));

        // 4 authored x 250/100 = 10, three shots. Four distinct numbers: 4, 250, 10, 3.
        assertTrue(lines.contains("Kinetic Damage: 10 x 3"),
                () -> "per-shot figure scaled, count beside it -- never multiplied together; got " + lines);
        assertFalse(lines.stream().anyMatch(l -> l.contains("30")),
                () -> "30 would be the volley total, which is true of no single hit; got " + lines);
    }

    /** A three-shot volley of a literal payload, DECLARED UNSCORED: the volley_stone shape. */
    private static WeaponDefinition unscoredStone() {
        return volleyStone(true);
    }

    /** The same shape WITHOUT the declaration, so the only difference between the two rows is it. */
    private static WeaponDefinition scoredVolley() {
        return volleyStone(false);
    }

    /**
     * One builder, one flag. <b>The two fixtures differ in exactly the field under test</b>, so a
     * row that passes for the wrong reason cannot be explained by any other difference between them.
     */
    private static WeaponDefinition volleyStone(boolean unscored) {
        AbilityDefinition scatter = new AbilityDefinition(
                "volley_stone/right_click", "Scatter", "kinetic", "none",
                20, ResourceCost.FREE,
                new CastSpec.Volley(10, 3, 5, new CastSpec.Ray(32.0, "beam")),
                List.of(new EffectSpec.Damage(4, "kinetic")), List.of("Three bolts."));
        return new WeaponDefinition("volley_stone", "Volley Stone", "kinetic", Rarity.RARE,
                WeaponClass.RANGER, "prismarine_shard", 0.0, 0.0, SweepShare.NONE,
                WeaponDefinition.NO_QUIVER, 0,
                List.of(new TriggerBinding("right_click", scatter)), List.of("Flavour."),
                java.util.Optional.empty(), unscored);
    }
}
