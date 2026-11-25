package io.github.butterflysmp.rpg.core.weapon;

import io.github.butterflysmp.rpg.core.accessory.AccessoryStat;
import io.github.butterflysmp.rpg.core.combat.HealthRegen;
import io.github.butterflysmp.rpg.core.combat.stat.CombatantStats;
import io.github.butterflysmp.rpg.core.summon.MaxSummons;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * B3, B4, B6, B7 (the seat, 2026-11-25): each of Ben's set bonuses reaches its stat as ONE source keyed by the set,
 * FLAT, and the resulting rates are the ones the seat asked to be stated.
 */
class ArmorSetContributionsTest {

    private static Optional<ArmorSetDecision.ActiveSet> set(String id, ArmorSetBonus.Type type, double value) {
        return Optional.of(new ArmorSetDecision.ActiveSet(id, List.of(new ArmorSetBonus(type, value))));
    }

    @Test
    void leathersRegenIsTwoPerFiveSecondsOnTopOfTheBase() {
        var c = ArmorSetContributions.of(set("leather", ArmorSetBonus.Type.HEALTH_REGEN, 2));
        assertEquals(Map.of("armorset:leather", 0.4), c.sources(AccessoryStat.HEALTH_REGEN));
        // B7, the resulting rate: base 0.2/s (READ, HealthRegen.BASE_PER_SECOND) + 0.4 = 0.6/s = 3 per 5 s.
        assertEquals(0.6, HealthRegen.BASE_PER_SECOND + c.sources(AccessoryStat.HEALTH_REGEN).get("armorset:leather"),
                1e-12);
    }

    @Test
    void goldensManaRegenIsTwoPerSecond() {
        var c = ArmorSetContributions.of(set("golden", ArmorSetBonus.Type.MANA_REGEN, 2));
        assertEquals(Map.of("armorset:golden", 2.0), c.sources(AccessoryStat.MANA_REGEN));
    }

    @Test
    void ironsMaxHealthDiamondsAndNetheritesDefenseAreFlatPoints() {
        assertEquals(Map.of("armorset:iron", 100.0),
                ArmorSetContributions.of(set("iron", ArmorSetBonus.Type.MAX_HEALTH, 100)).sources(AccessoryStat.MAX_HEALTH));
        assertEquals(Map.of("armorset:diamond", 20.0),
                ArmorSetContributions.of(set("diamond", ArmorSetBonus.Type.DEFENSE, 20)).sources(AccessoryStat.DEFENSE));
        assertEquals(Map.of("armorset:netherite", 40.0),
                ArmorSetContributions.of(set("netherite", ArmorSetBonus.Type.DEFENSE, 40)).sources(AccessoryStat.DEFENSE));
    }

    @Test
    void coppersSummonIsAGearGrantThatRaisesTheCapByOne() {
        var c = ArmorSetContributions.of(set("copper", ArmorSetBonus.Type.MAX_SUMMONS, 1));
        assertEquals(List.of(1), c.summonGrants());
        assertTrue(c.byStat().isEmpty(), "max summons is no Stat; it is read at the cast (B6)");
        assertEquals(2, MaxSummons.effective(null, c.summonGrants()), "Ben's default 1, + 1");
        assertEquals(1, MaxSummons.effective(null, ArmorSetContributions.NONE.summonGrants()), "off: back to 1");
    }

    @Test
    void takingCopperOffWithTwoSummonsOutRemovesNothingAndTheNextCastObeysTheCap() {
        // B6: with the set, two weapons' summons live under a cap of 2. Off, the cap is 1: SummonCap is a pure verdict
        // on the NEXT cast and has no way to remove a body, so both stay, and a recast of either is refused (the other
        // one already fills the cap), as is a third weapon's first.
        java.util.Set<String> live = java.util.Set.of("bone_whistle", "fixture_whistle");
        var off = java.util.OptionalInt.of(MaxSummons.effective(null, ArmorSetContributions.NONE.summonGrants()));
        assertEquals(io.github.butterflysmp.rpg.core.summon.SummonCap.Verdict.REFUSED_FULL,
                io.github.butterflysmp.rpg.core.summon.SummonCap.decide("bone_whistle", live, off));
        assertEquals(io.github.butterflysmp.rpg.core.summon.SummonCap.Verdict.REFUSED_FULL,
                io.github.butterflysmp.rpg.core.summon.SummonCap.decide("third", live, off));
        var on = java.util.OptionalInt.of(MaxSummons.effective(null,
                ArmorSetContributions.of(set("copper", ArmorSetBonus.Type.MAX_SUMMONS, 1)).summonGrants()));
        assertEquals(io.github.butterflysmp.rpg.core.summon.SummonCap.Verdict.REPLACE,
                io.github.butterflysmp.rpg.core.summon.SummonCap.decide("bone_whistle", live, on), "with the set: a recast replaces");
    }

    @Test
    void chainmailsQuiverPercentIsItsOwnTermUnderTheSetsOneKey() {
        var c = ArmorSetContributions.of(set("chainmail", ArmorSetBonus.Type.QUIVER_SIZE_PERCENT, 50));
        assertEquals(Map.of("armorset:chainmail", 50.0), c.quiverPercent());
        assertTrue(c.byStat().isEmpty(), "no other stat moves");
        assertEquals(List.of(), c.summonGrants());
        assertEquals(Map.of(), ArmorSetContributions.NONE.quiverPercent(), "off: absent, so the reconciler drops it");
        // Mutation: route the percent into the additive quiver map -> this map is empty -> reddens.
    }

    @Test
    void noSetContributesNothingAnywhere() {
        assertTrue(ArmorSetContributions.NONE.byStat().isEmpty());
        assertEquals(List.of(), ArmorSetContributions.NONE.summonGrants());
        assertEquals(ArmorSetContributions.NONE, ArmorSetContributions.of(Optional.empty()));
    }

    @Test
    void theKeyIsOnePerSetAndCannotCollideWithAPieceSlot() {
        assertEquals("armorset:iron", ArmorSetContributions.sourceKey("iron"));
        assertTrue(!ArmorSetContributions.sourceKey("iron").equals("CHEST"));
    }

    @Test
    void ironsMaxHealthOnTheRealStoreIsHeadroomThenAClamp() {
        // B7: gaining +100 leaves current unchanged (headroom); losing it clamps current down. HealthState's own rules,
        // reached through the real reconcile path the loop calls, with the set's source.
        CombatantStats stats = new CombatantStats();
        java.util.UUID ben = java.util.UUID.randomUUID();
        stats.register(ben, CombatantStats.DEFAULT_PLAYER_BASE, true);
        var iron = ArmorSetContributions.of(set("iron", ArmorSetBonus.Type.MAX_HEALTH, 100));
        stats.reconcileMaxModifiers(ben, iron.sources(AccessoryStat.MAX_HEALTH));
        assertEquals(200.0, stats.max(ben), 1e-9);
        assertEquals(100.0, stats.current(ben), 1e-9, "headroom: current unchanged");
        stats.heal(ben, 100, ben, false);
        assertEquals(200.0, stats.current(ben), 1e-9);
        stats.reconcileMaxModifiers(ben, ArmorSetContributions.NONE.sources(AccessoryStat.MAX_HEALTH));
        assertEquals(100.0, stats.max(ben), 1e-9);
        assertEquals(100.0, stats.current(ben), 1e-9, "the set off: current clamped down to the new max");
    }
}
