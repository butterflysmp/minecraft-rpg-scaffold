package io.github.butterflysmp.rpg.paper.menu;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The chrome-over-lore composition, and the trailing-blank case.
 *
 * <p>{@code CraftingMenu.suggestionIcon} needs a live {@code ItemMeta} and a minted stack, so it is
 * boot-gate-only -- gate row Q9. The ORDERING is not, and it is the half that is easy to get wrong,
 * so it is extracted here. Same trade {@code CollectPlan} and {@code GridClickIntent} make.
 *
 * <p>Each test names the mutation it forces red.
 */
class MenuIconsTest {

    private static final Component CHROME_A = Component.text("Craft 4 more");
    private static final Component CHROME_B = Component.text("Uses items from your inventory");
    private static final Component ITEM_A = Component.text("Damage Reduction: 35%");
    private static final Component ITEM_B = Component.text("Common Shield");

    private static List<String> plain(List<Component> lore) {
        List<String> out = new ArrayList<>();
        for (Component line : lore) out.add(PlainTextComponentSerializer.plainText().serialize(line));
        return out;
    }

    @Test
    void chromeGoesONTOP_SoTheRarityFooterStaysLAST() {
        // THE ordering. The footer is the tier badge and the player reads it as the last line on the
        // real item; a suggestion icon that ended in "Uses items from your inventory" would put the
        // badge in the middle, which is the defect GearLore.appendRarityFooter warns about.
        List<String> lore = plain(MenuIcons.chromeOver(
                List.of(CHROME_A, CHROME_B), List.of(ITEM_A, ITEM_B)));

        assertEquals(List.of("Craft 4 more", "Uses items from your inventory", "",
                "Damage Reduction: 35%", "Common Shield"), lore);
        assertEquals("Common Shield", lore.get(lore.size() - 1), "the footer must be last");
        // Mutation: append the chrome instead of prepending -> the footer stops being last -> reddens.
    }

    @Test
    void exactlyONEBlankSeparatesThem() {
        List<String> lore = plain(MenuIcons.chromeOver(List.of(CHROME_A), List.of(ITEM_A)));

        assertEquals(3, lore.size());
        assertEquals("", lore.get(1), "one blank, between");
        assertNotEquals("", lore.get(0));
        assertNotEquals("", lore.get(2));
        // Mutation: add two blanks -> the size assertion reddens.
    }

    @Test
    void NOTrailingBlankWhenTheItemHasNoLoreOfItsOwn() {
        // THE case worth naming, and the reason this is not EnchantLore.applied. That method guards
        // on the PREPENDED block being empty; this one guards on the UNDERNEATH being empty, so
        // applied(existing, chrome) with no existing lore yields chrome + blank -- a separator with
        // nothing after it, rendering as a stray empty row. An unclaimed vanilla result has exactly
        // no lore of its own, so this is the ordinary case rather than an edge one.
        assertEquals(List.of("Craft 4 more", "Uses items from your inventory"),
                plain(MenuIcons.chromeOver(List.of(CHROME_A, CHROME_B), List.of())));
        assertEquals(List.of("Craft 4 more"),
                plain(MenuIcons.chromeOver(List.of(CHROME_A), null)));
        // Mutation: always add the blank -> both redden with a trailing "".
    }

    @Test
    void theItemsOwnLoreIsPRESERVEDInOrderAndNotRewritten() {
        // The whole point of minting the icon: the gear tooltip the mint produced must survive.
        // meta.lore(List.of(..)) REPLACES, so a version that built its own list and called that
        // would wipe stats, flavour and footer -- and would look correct in a screenshot of a
        // vanilla suggestion, which has no lore to lose.
        List<Component> item = List.of(ITEM_A, MenuIcons.blank(), ITEM_B);
        List<String> lore = plain(MenuIcons.chromeOver(List.of(CHROME_A), item));

        assertEquals(plain(item), lore.subList(2, lore.size()),
                "the item's own lore, unchanged and in order, including its own internal blank");
        // Mutation: sort or de-duplicate the merged lore -> reddens. The item's internal blank is in
        // there precisely so a de-duplication of blanks would be caught.
    }

    @Test
    void noChromeMeansTheItemIsHandedBackUntouched() {
        assertEquals(List.of("Damage Reduction: 35%", "Common Shield"),
                plain(MenuIcons.chromeOver(List.of(), List.of(ITEM_A, ITEM_B))));
        assertEquals(List.of(), MenuIcons.chromeOver(null, null));
        // Mutation: drop the empty-chrome guard -> a leading blank appears -> reddens.
    }

    @Test
    void theResultIsNotAViewOfEitherInput() {
        // It is handed to meta.lore(..) and the caller keeps its own lists. Aliasing either one
        // would let a later edit to the icon reach back into the item's real lore.
        List<Component> chrome = new ArrayList<>(List.of(CHROME_A));
        List<Component> existing = new ArrayList<>(List.of(ITEM_A));

        List<Component> out = MenuIcons.chromeOver(chrome, existing);
        chrome.add(Component.text("late"));
        existing.add(Component.text("late"));

        assertEquals(3, out.size(), "the composition must not be backed by its inputs");
        // Mutation: return existing directly when chrome is empty -> the no-chrome case aliases ->
        // reddens there rather than here, which is why List.copyOf is used on both guards.
    }
}
