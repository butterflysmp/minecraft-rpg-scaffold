package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.enchant.EnchantEffect;
import io.github.butterflysmp.rpg.core.enchant.EnchantState;
import io.github.butterflysmp.rpg.paper.content.EnchantDefinition;
import io.github.butterflysmp.rpg.paper.content.EnchantRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The enchant block of the tooltip: what it shows, and -- just as load-bearing -- what it does not.
 *
 * Every case here is a guard whose absence is a real, shippable bug: a merely-unlocked candidate
 * rendering as though it were working, an enchant that IS working rendering nothing at all, a
 * duplicate promising a level the seam will not apply, or the rarity footer stopping being last.
 *
 * Pure Adventure and a plain-Java registry -- no ItemStack, so no running server needed, the same
 * arrangement {@link WeaponLoreTest} uses.
 */
class EnchantLoreTest {

    private static final String UNBREAKING = "unbreaking";

    private static EnchantRegistry registryWithUnbreaking() {
        EnchantRegistry registry = new EnchantRegistry();
        registry.register(new EnchantDefinition(UNBREAKING, "Unbreaking", 3, EnchantEffect.DURABILITY, null, List.of()));
        return registry;
    }

    /** One slot, one candidate, unlocked to {@code level} and made active. */
    private static EnchantState active(String id, int level) {
        return EnchantState.empty().addCandidate(0, id).withLevel(0, 0, level).withActive(0, 0);
    }

    private static List<String> textLines(List<Component> lore) {
        List<String> out = new ArrayList<>();
        for (Component line : lore) out.add(PlainTextComponentSerializer.plainText().serialize(line));
        return out;
    }

    /** MiniMessage may hang colour on a child, so look down the tree rather than at the root only. */
    private static TextColor colorOf(Component component) {
        if (component.color() != null) return component.color();
        for (Component child : component.children()) {
            TextColor found = colorOf(child);
            if (found != null) return found;
        }
        return null;
    }

    @Test
    void anActiveEnchantRendersItsNameAndRomanNumeralInVanillaStyle() {
        // What boot gate step 3 reads off the screen: grey, italic, roman. Arabic or a missing
        // numeral is the difference between a tooltip that reads like Minecraft and one that reads
        // like a debug line.
        List<Component> lines = EnchantLore.lines(active(UNBREAKING, 3), registryWithUnbreaking());

        assertEquals(List.of("Unbreaking III"), textLines(lines));
        assertEquals(NamedTextColor.GRAY, colorOf(lines.get(0)));
        assertEquals(TextDecoration.State.TRUE, lines.get(0).decoration(TextDecoration.ITALIC),
                "vanilla renders enchantment lines italic; every other lore line here opts out");
        // Mutation: emit the arabic level -> "Unbreaking 3" -> reddens.
        // Mutation: drop the .decoration(ITALIC, true) -> NOT_SET -> reddens.
    }

    @Test
    void theDisplayNameComesFromContentNotFromTheId() {
        // The whole reason identity is a yml rather than a Java constant. A literal "Unbreaking" in
        // the renderer is exactly the drift the element registry exists to prevent.
        EnchantRegistry renamed = new EnchantRegistry();
        renamed.register(new EnchantDefinition(UNBREAKING, "Everlasting", 3, EnchantEffect.DURABILITY, null, List.of()));

        assertEquals(List.of("Everlasting III"),
                textLines(EnchantLore.lines(active(UNBREAKING, 3), renamed)));
    }

    @Test
    void aLockedOrMerelyUnlockedCandidateRendersNothing() {
        // THE TOOLTIP SHOWS WHAT IS ACTIVE, NEVER WHAT IS MERELY UNLOCKED. Two candidates the
        // player has paid to unlock, neither chosen: the item is doing nothing, and it must say so
        // by saying nothing. Rendering every unlocked candidate would show two working enchants on
        // an item with none.
        EnchantState unlockedButInactive = EnchantState.empty()
                .addCandidate(0, UNBREAKING).withLevel(0, 0, 3)
                .addCandidate(0, "sharpness").withLevel(0, 1, 2);

        assertEquals(List.of(), EnchantLore.lines(unlockedButInactive, registryWithUnbreaking()));
        assertEquals(List.of(), EnchantLore.lines(EnchantState.empty(), registryWithUnbreaking()));
        // Mutation: walk slots().candidates() instead of effective() -> two lines -> reddens.
    }

    @Test
    void theSameEnchantInTwoSlotsRendersOnceAtTheEffectiveLevel() {
        // The tooltip and the seam must not contradict each other. effective() resolves I+III to
        // III, so the tooltip says III once -- not "Unbreaking III" and "Unbreaking I" on separate
        // lines, which would promise something no single number can be.
        EnchantState twice = EnchantState.empty()
                .addCandidate(0, UNBREAKING).withLevel(0, 0, 1).withActive(0, 0)
                .addCandidate(1, UNBREAKING).withLevel(1, 0, 3).withActive(1, 0);

        assertEquals(List.of("Unbreaking III"),
                textLines(EnchantLore.lines(twice, registryWithUnbreaking())));
        // Mutation: render per active SLOT rather than per effective enchant -> two lines, one of
        // them a level the seam will not apply -> reddens.
    }

    @Test
    void anEnchantIdTheRegistryNoLongerKnowsStillRendersRatherThanVanishing() {
        // Deliberate, and it follows from the seam rather than from taste: the seam compares ids and
        // NEVER consults the registry, so deleting unbreaking.yml leaves the enchant WORKING. An
        // enchant that silently skips durability while showing nothing is a far worse bug than one
        // with an ugly name -- so the tooltip fails soft and keeps telling the truth.
        assertEquals(List.of("Unbreaking III"),
                textLines(EnchantLore.lines(active(UNBREAKING, 3), new EnchantRegistry())),
                "an empty registry still renders the id, title-cased");
        // Mutation: return an empty list for an unknown id -> an enchant that WORKS and is
        // INVISIBLE -> reddens.
    }

    @Test
    void aSingleLevelEnchantOmitsItsNumeralUsingItsOwnDeclaredMaximum() {
        // max_level is read from that enchant's definition, not assumed -- vanilla's Mending rule.
        EnchantRegistry mending = new EnchantRegistry();
        mending.register(new EnchantDefinition("mending", "Mending", 1, EnchantEffect.DURABILITY, null, List.of()));

        assertEquals(List.of("Mending"), textLines(EnchantLore.lines(active("mending", 1), mending)));
    }

    @Test
    void theBlockGoesAtTheTopAndIsFollowedByOneBlankLine() {
        // Vanilla's order: name, enchantments, then everything else. Index 0 also means no index
        // arithmetic, so nothing here needs updating when the rest of the tooltip changes.
        List<Component> base = List.of(Component.text("Kinetic"), Component.text("Melee Damage: 7"));
        List<Component> out = EnchantLore.applied(base, EnchantLore.lines(
                active(UNBREAKING, 3), registryWithUnbreaking()));

        assertEquals(List.of("Unbreaking III", "", "Kinetic", "Melee Damage: 7"), textLines(out));
        // Mutation: append rather than prepend -> the enchant block lands under the footer -> reddens.
    }

    @Test
    void applyingAnEmptyEnchantBlockLeavesTheLoreIdenticalWithNoStrayBlank() {
        // EVERY unenchanted weapon in the game goes through here. A stray leading empty line on all
        // of them is the kind of thing nobody notices until every tooltip looks subtly wrong.
        List<Component> base = List.of(Component.text("Kinetic"), Component.text("Melee Damage: 7"));

        assertEquals(base, EnchantLore.applied(base, List.of()));
        assertSame(base, EnchantLore.applied(base, List.of()), "unchanged means unchanged");
        // Mutation: always add the blank separator -> a leading "" on every plain weapon -> reddens.
    }

    @Test
    void applyingTwiceYieldsTheSameLoreBecauseTheCallerRebuilds() {
        // The idempotence requirement, pinned at the level it actually holds. EnchantLore.applied
        // itself is a pure prepend and would happily double a block -- what makes the SYSTEM
        // idempotent is that WeaponItems.applyLore rebuilds the base from the definition on every
        // call and never feeds it lore that already carries a block. This pins that contract by
        // imitating it: same base, same state, twice, same answer.
        List<Component> base = List.of(Component.text("Kinetic"));
        List<Component> enchantLines = EnchantLore.lines(active(UNBREAKING, 3), registryWithUnbreaking());

        List<Component> once = EnchantLore.applied(base, enchantLines);
        List<Component> twice = EnchantLore.applied(base, enchantLines);

        assertEquals(textLines(once), textLines(twice));
        assertEquals(1, textLines(once).stream().filter(l -> l.equals("Unbreaking III")).count(),
                "exactly one enchant line, never a doubled block");
    }
}
