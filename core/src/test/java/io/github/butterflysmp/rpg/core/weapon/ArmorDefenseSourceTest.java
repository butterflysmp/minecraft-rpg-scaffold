package io.github.butterflysmp.rpg.core.weapon;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A1 and A3 (the seat, 2026-11-25): a worn piece's Defense is the value AUTHORED in its content file, and an UNTAGGED
 * piece of a known material reads the same one, by ONE lookup by material.
 *
 * <p>Staged with values no two of which collide (head 15, chest 24, vanilla's diamond helmet 3 nowhere), so a lookup
 * that read the wrong slot or the wrong piece cannot land on the right number by accident.
 */
class ArmorDefenseSourceTest {

    private static ArmorRegistry diamond() {
        ArmorRegistry r = new ArmorRegistry();
        r.register(new ArmorDefinition("diamond_helmet", "Diamond Helmet", Rarity.UNCOMMON, "diamond_helmet",
                ArmorSlot.HEAD, 15, List.of()));
        r.register(new ArmorDefinition("diamond_chestplate", "Diamond Chestplate", Rarity.UNCOMMON,
                "diamond_chestplate", ArmorSlot.CHEST, 24, List.of()));
        return r;
    }

    @Test
    void aMintedPieceReadsItsAuthoredDefense() {
        assertEquals(OptionalDouble.of(15), ArmorDefenseSource.authored(diamond(), Optional.of("diamond_helmet"),
                "diamond_helmet", ArmorSlot.HEAD));
        // Mutation: return vanilla's points (3) -- the slice 1 source -- and this reddens.
    }

    @Test
    void anUntaggedPieceOfAKnownMaterialReadsTheSameAuthoredDefense() {
        assertEquals(OptionalDouble.of(15), ArmorDefenseSource.authored(diamond(), Optional.empty(),
                "diamond_helmet", ArmorSlot.HEAD), "A3: a mob drop reads what a minted helmet reads");
        assertEquals(OptionalDouble.of(24), ArmorDefenseSource.authored(diamond(), Optional.empty(),
                "diamond_chestplate", ArmorSlot.CHEST));
        // Mutation: drop the material fallback -> empty -> reddens.
    }

    @Test
    void aDanglingTagFallsBackToItsMaterial() {
        assertEquals(OptionalDouble.of(15), ArmorDefenseSource.authored(diamond(), Optional.of("no_such_piece"),
                "diamond_helmet", ArmorSlot.HEAD));
    }

    @Test
    void aMaterialWithNoContentPieceHasNoAuthoredDefense() {
        assertEquals(OptionalDouble.empty(), ArmorDefenseSource.authored(diamond(), Optional.empty(),
                "turtle_helmet", ArmorSlot.HEAD), "C2: no turtle helmet in content");
    }

    @Test
    void aPieceWornOutsideItsOwnSlotContributesNoAuthoredDefense() {
        assertEquals(OptionalDouble.empty(), ArmorDefenseSource.authored(diamond(), Optional.of("diamond_helmet"),
                "diamond_helmet", ArmorSlot.CHEST));
        // Mutation: drop the slot filter -> 15 in the chest slot -> reddens.
    }
}
