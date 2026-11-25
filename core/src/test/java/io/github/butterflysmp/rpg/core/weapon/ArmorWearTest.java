package io.github.butterflysmp.rpg.core.weapon;

import io.github.butterflysmp.rpg.core.combat.CritState;
import io.github.butterflysmp.rpg.core.combat.DefenseRule;
import io.github.butterflysmp.rpg.core.combat.stat.CombatantStats;
import io.github.butterflysmp.rpg.core.combat.stat.HealthChange;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * D2', D3', D4' (the seat, 2026-11-25): WHEN a worn piece wears, and by how much, decided purely. The paper side
 * (ArmorDurability) applies it to the four slots through the weapons' own item helpers.
 *
 * <p>Staged so no two read quantities collide: a maximum of 55 (leather's helmet, READ), damage 10, so the worn piece
 * reads 11, broken is 54 (max - 1).
 */
class ArmorWearTest {

    private static final int MAX = 55;

    @Test
    void aHitWearsAnUnbrokenPieceByExactlyOne() {
        assertEquals(OptionalInt.of(11), ArmorWear.wearOne(10, MAX, 0, 0.99));
        // Mutation: wear by 2 -> 12 -> reddens.
    }

    @Test
    void aBrokenPieceTakesNoFurtherWear() {
        assertEquals(OptionalInt.empty(), ArmorWear.wearOne(MAX - 1, MAX, 0, 0.0), "broken at max - 1, as a weapon");
        // Mutation: drop the broken check -> the clamp returns 54 again, OptionalInt.of(54) -> reddens.
    }

    @Test
    void thePieceOneFromBrokenBreaksAndIsNeverDestroyed() {
        // D4': the wear clamps at max - 1, exactly the weapon kernel's floor, so the item is never deleted.
        assertEquals(OptionalInt.of(MAX - 1), ArmorWear.wearOne(MAX - 2, MAX, 0, 0.5));
        assertTrue(Durability.isBroken(MAX - 1, MAX));
    }

    @Test
    void unbreakingSkipsWearAsOnAWeapon() {
        // Unbreaking III consumes with chance 1/4 (Unbreaking.consumeChance): a roll of 0.24 wears, 0.25 does not.
        assertEquals(OptionalInt.of(11), ArmorWear.wearOne(10, MAX, 3, 0.24));
        assertEquals(OptionalInt.empty(), ArmorWear.wearOne(10, MAX, 3, 0.25));
        // Level 0 always wears, whatever the roll.
        assertEquals(OptionalInt.of(11), ArmorWear.wearOne(10, MAX, 0, 0.999));
    }

    @Test
    void aPieceWithNoDurabilityNeverWears() {
        assertEquals(OptionalInt.empty(), ArmorWear.wearOne(0, 0, 0, 0.0));
    }

    @Test
    void aBrokenGrowthPieceClampsHealthAsRemovingItDoes() {
        // D5': the paper scanner drops a broken piece's Growth source (ArmorDurabilityWiringTest pins that); this is
        // what the store then does with the absence: the same clamp as unequipping, on the real reconcile path.
        CombatantStats stats = new CombatantStats();
        UUID ben = UUID.randomUUID();
        stats.register(ben, CombatantStats.DEFAULT_PLAYER_BASE, true);
        stats.reconcileMaxModifiers(ben, java.util.Map.of("growth:CHEST", 30.0));
        stats.heal(ben, 30, ben, true);
        assertEquals(130.0, stats.current(ben), 1e-9);
        stats.reconcileMaxModifiers(ben, java.util.Map.of());   // the piece broke: its source is absent
        assertEquals(100.0, stats.max(ben), 1e-9);
        assertEquals(100.0, stats.current(ben), 1e-9, "clamped, exactly as removal");
    }

    // --- WHICH hits wear (D2') ---

    private static List<HealthChange> changes(CombatantStats[] holder) {
        List<HealthChange> seen = new ArrayList<>();
        holder[0] = new CombatantStats(seen::add);
        return seen;
    }

    @Test
    void aHitThroughDefenseOnAPlayerWearsAndABypassedOneDoesNot() {
        CombatantStats[] s = new CombatantStats[1];
        List<HealthChange> seen = changes(s);
        UUID ben = UUID.randomUUID();
        s[0].register(ben, CombatantStats.DEFAULT_PLAYER_BASE, true);
        s[0].damage(ben, 10, null, false, CritState.NORMAL, DefenseRule.APPLIES, null);
        s[0].damage(ben, 10, null, false, CritState.NORMAL, DefenseRule.BYPASSED, null);
        s[0].heal(ben, 5, ben, true);
        List<Boolean> wears = seen.stream().map(ArmorWear::wears).toList();
        assertEquals(List.of(true, false, false), wears, "through Defense; bypassed; a heal");
        // Mutation: emit throughDefense = true for BYPASSED in CombatantStats.damage -> reddens.
    }

    @Test
    void aMobsHitWearsNothingAndAZeroHitWearsNothing() {
        CombatantStats[] s = new CombatantStats[1];
        List<HealthChange> seen = changes(s);
        UUID mob = UUID.randomUUID();
        UUID ben = UUID.randomUUID();
        s[0].register(mob, 100, false);
        s[0].register(ben, CombatantStats.DEFAULT_PLAYER_BASE, true);
        s[0].damage(mob, 10, null, false, CritState.NORMAL, DefenseRule.APPLIES, null);
        s[0].damage(ben, 0, null, false, CritState.NORMAL, DefenseRule.APPLIES, null);
        assertFalse(ArmorWear.wears(seen.get(0)), "a mob wears no armor of ours");
        assertFalse(ArmorWear.wears(seen.get(1)), "CC'S CALL: nothing offered is not a hit");
    }
}
