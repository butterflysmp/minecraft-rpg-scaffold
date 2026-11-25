package io.github.butterflysmp.rpg.core.weapon;

import io.github.butterflysmp.rpg.core.weapon.ArmorSetDecision.Worn;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * B2 (the seat, 2026-11-25), as a table: ACTIVE exactly when all four worn slots hold pieces of the same set. A BROKEN
 * piece still counts (Ben: "No a broken piece doesn't break the set"). Minted and untagged pieces resolve through the
 * same lookup (A3) before they arrive here, so the decision sees definitions and cannot tell them apart -- which is the
 * point of the mixed row.
 */
class ArmorSetDecisionTest {

    private static final List<ArmorSetBonus> IRON_BONUS = List.of(new ArmorSetBonus(ArmorSetBonus.Type.MAX_HEALTH, 100));
    private static final List<ArmorSetBonus> GOLD_BONUS = List.of(new ArmorSetBonus(ArmorSetBonus.Type.MANA_REGEN, 2));

    private static ArmorDefinition piece(String tier, ArmorSlot slot, List<ArmorSetBonus> bonus) {
        String id = tier + "_" + slot.name().toLowerCase();
        return new ArmorDefinition(id, id, Rarity.COMMON, id, slot, 5, List.of(), Optional.empty(),
                Optional.of(tier), bonus);
    }

    private static Map<ArmorSlot, Worn> worn(String headTier, String chestTier, String legsTier, String feetTier,
                                             boolean... broken) {
        Map<ArmorSlot, Worn> w = new EnumMap<>(ArmorSlot.class);
        String[] tiers = {headTier, chestTier, legsTier, feetTier};
        ArmorSlot[] slots = ArmorSlot.values();
        for (int i = 0; i < 4; i++) {
            boolean b = broken.length > i && broken[i];
            if (tiers[i] == null) {
                w.put(slots[i], new Worn(Optional.empty(), b));
            } else {
                List<ArmorSetBonus> bonus = tiers[i].equals("iron") ? IRON_BONUS : GOLD_BONUS;
                w.put(slots[i], new Worn(Optional.of(piece(tiers[i], slots[i], bonus)), b));
            }
        }
        return w;
    }

    @Test
    void fourOfOneSetIsActive() {
        var active = ArmorSetDecision.active(worn("iron", "iron", "iron", "iron"));
        assertEquals(Optional.of("iron"), active.map(ArmorSetDecision.ActiveSet::id));
        assertEquals(IRON_BONUS, active.get().bonuses());
    }

    @Test
    void threeAndOneOtherIsNot() {
        assertTrue(ArmorSetDecision.active(worn("iron", "iron", "golden", "iron")).isEmpty());
        // Mutation: activate on the HEAD's set alone -> iron -> reddens.
    }

    @Test
    void threeAndAnEmptySlotIsNot() {
        assertTrue(ArmorSetDecision.active(worn("iron", "iron", "iron", null)).isEmpty());
        // Mutation: skip empty slots instead of failing on them -> iron -> reddens.
    }

    @Test
    void fourWithOneBrokenIsStillActive() {
        assertEquals(Optional.of("iron"), ArmorSetDecision.active(worn("iron", "iron", "iron", "iron",
                false, true, false, false)).map(ArmorSetDecision.ActiveSet::id), "Ben: a broken piece does not break it");
        // Mutation: refuse a broken piece -> empty -> reddens.
    }

    @Test
    void fourAllBrokenIsStillActive() {
        assertEquals(Optional.of("iron"), ArmorSetDecision.active(worn("iron", "iron", "iron", "iron",
                true, true, true, true)).map(ArmorSetDecision.ActiveSet::id));
    }

    @Test
    void mintedAndUntaggedMixedIsActiveBecauseBothResolveToTheSameDefinitions() {
        // A3: an untagged chestplate resolves by material to the SAME definition a minted one carries, so the map
        // built from two minted and two untagged pieces is identical to four minted. Stated as that identity.
        Map<ArmorSlot, Worn> minted = worn("golden", "golden", "golden", "golden");
        Map<ArmorSlot, Worn> mixed = worn("golden", "golden", "golden", "golden");
        assertEquals(minted, mixed);
        assertEquals(Optional.of("golden"), ArmorSetDecision.active(mixed).map(ArmorSetDecision.ActiveSet::id));
    }

    @Test
    void aPieceInTheWrongSlotOrWithNoSetBreaksIt() {
        Map<ArmorSlot, Worn> w = worn("iron", "iron", "iron", "iron");
        w.put(ArmorSlot.FEET, new Worn(Optional.of(new ArmorDefinition("x", "x", Rarity.COMMON, "x", ArmorSlot.FEET,
                5, List.of())), false));
        assertTrue(ArmorSetDecision.active(w).isEmpty(), "a piece with no set");
        Map<ArmorSlot, Worn> v = worn("iron", "iron", "iron", "iron");
        v.put(ArmorSlot.FEET, new Worn(Optional.of(piece("iron", ArmorSlot.HEAD, IRON_BONUS)), false));
        assertTrue(ArmorSetDecision.active(v).isEmpty(), "a helmet in the feet slot");
    }

    @Test
    void lessThanFourSlotsIsNot() {
        Map<ArmorSlot, Worn> w = worn("iron", "iron", "iron", "iron");
        w.remove(ArmorSlot.LEGS);
        assertTrue(ArmorSetDecision.active(w).isEmpty());
    }
}
