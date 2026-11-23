package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.enchant.EnchantCodec;
import io.github.butterflysmp.rpg.core.enchant.EnchantEffect;
import io.github.butterflysmp.rpg.core.enchant.EnchantState;
import io.github.butterflysmp.rpg.core.enchant.GrindstoneRefund;
import io.github.butterflysmp.rpg.core.weapon.GearClass;
import io.github.butterflysmp.rpg.paper.content.EnchantDefinition;
import io.github.butterflysmp.rpg.paper.content.EnchantRegistry;
import io.github.butterflysmp.rpg.paper.menu.EnchantMenuLayout;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * V4 (the seat's amendment, 2026-11-23): AN ITEM ROLLED UNDER THE OLD THREE-SLOT SHAPE. Ben: "we don't care about
 * existing items"; no migration, no re-roll. The only duty: it never throws, never dupes and never charges wrongly when
 * held, dumped or put in the table. The simplest safe behaviour is what the code already does, pinned here:
 *
 * <ul>
 *   <li>HELD: its tooltip shows its ACTIVE enchants exactly as before (EnchantLore reads {@code effective()}, which does
 *       not care how many slots there are or which slot is "unique").</li>
 *   <li>DUMPED ({@code /rpg enchant show}): its three slots print as three slots.</li>
 *   <li>IN THE TABLE: it fits (three of three inside four of four), every candidate has a cell, its slot 0's NORMAL
 *       candidates show in the unique column under the "Unique" label, its missing slot 3 is filler. A click is priced
 *       by the same EnchantCharge as any candidate, so the charge is unchanged. The grindstone refund sums every
 *       candidate's rungs, so it refunds what was paid.</li>
 * </ul>
 */
class OldThreeSlotItemTest {

    private static final String OLD = "v1;sharpness=2,unbreaking=0,attunement=0:0;unbreaking=1:-1;sharpness=0,unbreaking=3:1";

    private static EnchantRegistry registry() {
        EnchantRegistry r = new EnchantRegistry();
        r.register(new EnchantDefinition("sharpness", "Sharpness", 3, EnchantEffect.DAMAGE, GearClass.MELEE, List.of(5, 10, 15)));
        r.register(new EnchantDefinition("unbreaking", "Unbreaking", 3, EnchantEffect.DURABILITY, null, List.of()));
        r.register(new EnchantDefinition("attunement", "Attunement", 3, EnchantEffect.DAMAGE, GearClass.MAGE, List.of(5, 10, 15)));
        return r;
    }

    @Test
    void heldItsTooltipShowsItsActiveEnchantsAsBefore() {
        EnchantState old = EnchantCodec.decode(OLD);
        List<String> lore = EnchantLore.lines(old, registry()).stream()
                .map(PlainTextComponentSerializer.plainText()::serialize).toList();
        assertEquals(List.of("Sharpness II", "Unbreaking III"), lore);
    }

    @Test
    void inTheTableItFitsAndEveryCandidateHasACell() {
        EnchantState old = EnchantCodec.decode(OLD);
        assertTrue(EnchantMenuLayout.overflow(old).isEmpty(), "accepted, not refused at the door");
        int cells = 0;
        for (int s = 0; s < old.slots().size(); s++) {
            for (int c = 0; c < old.slots().get(s).candidates().size(); c++) {
                EnchantMenuLayout.rawSlotFor(s, c);
                cells++;
            }
        }
        assertEquals(6, cells);
    }

    /** The grindstone refunds the old item by the same rule as any: every unlocked rung, 35%. */
    @Test
    void theGrindstoneRefundsWhatItsUnlocksCost() {
        EnchantState old = EnchantCodec.decode(OLD);
        assertEquals(GrindstoneRefund.listSpend(EnchantState.empty().addCandidate(0, "sharpness").withLevel(0, 0, 2))
                        + GrindstoneRefund.listSpend(EnchantState.empty().addCandidate(0, "unbreaking").withLevel(0, 0, 1))
                        + GrindstoneRefund.listSpend(EnchantState.empty().addCandidate(0, "unbreaking").withLevel(0, 0, 3)),
                GrindstoneRefund.listSpend(old));
        assertEquals(3, GrindstoneRefund.stripped(old).slots().size(), "the strip keeps its three slots");
    }
}
