package io.github.butterflysmp.rpg.core.combat.stat;

import io.github.butterflysmp.rpg.core.enchant.DamageEnchants;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The store is the source of truth for combat health, and the observability seam is what displays
 * ride. These tests pin: damage/heal move the CUSTOM number and the emitted event carries it (the
 * display follows truth, never the reverse); the event carries the dealer identity a future popup
 * targets; and a steady reconcile is silent. Each names the mutation it forces red.
 */
class CombatantStatsTest {

    private static final double EPS = 1e-9;

    /** Records every change so a test can assert on the payload the displays will consume. */
    private static final class Recorder implements HealthListener {
        final List<HealthChange> seen = new ArrayList<>();
        @Override public void onChange(HealthChange change) { seen.add(change); }
        HealthChange last() { return seen.get(seen.size() - 1); }
    }

    @Test
    void damageReducesCustomHealthAndTheEventCarriesIt() {
        var recorder = new Recorder();
        var stats = new CombatantStats(recorder);
        UUID victim = UUID.randomUUID();
        UUID dealer = UUID.randomUUID();
        stats.register(victim, CombatantStats.DEFAULT_PLAYER_BASE, true); // 100/100

        stats.damage(victim, 30, dealer, true);

        assertEquals(70, stats.current(victim), EPS, "damage came off the CUSTOM current, the source of truth");
        HealthChange change = recorder.last();
        assertEquals(HealthChange.Kind.DAMAGE, change.kind(), "emitted a DAMAGE change");
        assertEquals(70, change.newCurrent(), EPS, "the event carries the custom current AFTER the hit");
        assertEquals(100, change.max(), EPS, "and the custom max");
        assertEquals(30, change.amount(), EPS, "and the magnitude");
        // Display FOLLOWS truth: the heart fill is derived from the custom numbers, not read back from vanilla.
        assertEquals(HeartScale.filledHearts(70, 100), HeartScale.filledHearts(change.newCurrent(), change.max()), EPS,
                "the fill the renderer will draw is a pure function of the custom health in the event");
        // Mutation: emit the pre-damage current, or damage a vanilla field instead of custom -> reddens.
    }

    @Test
    void theEventIdentifiesTheDealerAsAPlayerForTheFuturePopup() {
        var recorder = new Recorder();
        var stats = new CombatantStats(recorder);
        UUID mob = UUID.randomUUID();
        UUID dealer = UUID.randomUUID();
        stats.register(mob, 5000, false);                 // a boss, custom HP well past vanilla's 1024

        stats.damage(mob, 250, dealer, true);

        HealthChange change = recorder.last();
        assertFalse(change.targetIsPlayer(), "the target is a mob");
        assertEquals(dealer, change.dealer(), "the dealer id is carried for credit and the popup");
        assertTrue(change.dealerIsPlayer(), "and that the dealer was a player -- so the popup targets their screen");
        assertEquals(4750, change.newCurrent(), EPS, "5000 custom HP is representable -- the whole point of custom");
        // Mutation: drop dealerIsPlayer from the record/emit -> the popup can't find its viewer next phase -> reddens.
    }

    @Test
    void reachedZeroFiresExactlyOnTheHitThatBringsCustomHealthToZero() {
        var recorder = new Recorder();
        var stats = new CombatantStats(recorder);
        UUID mob = UUID.randomUUID();
        stats.register(mob, 100, false);                  // 100/100

        stats.damage(mob, 30, null, false);               // 70/100 -- above 0
        assertFalse(recorder.last().reachedZero(), "a non-lethal hit does not flag reachedZero");

        stats.damage(mob, 70, null, false);               // 0/100 -- the crossing
        assertEquals(0, stats.current(mob), EPS, "custom current floored at 0, still tracked (not killed)");
        assertTrue(recorder.last().reachedZero(), "the hit that brings custom HP to 0 flags reachedZero -- the death hook");

        stats.damage(mob, 5, null, false);                // already 0 -- another hit
        assertFalse(recorder.last().reachedZero(),
                "a later hit on an already-0 target does NOT re-fire -- death fires once, on the transition");
        // Mutation A: drop `before > 0.0` in HealthState.damage -> the already-0 hit re-fires -> the death
        //   system would double-kill -> this reddens.
        // Mutation B: hardcode reachedZero=false at the emit -> the crossing hit never flags -> reddens.
    }

    @Test
    void healRaisesCustomHealthCappedAtMaxAndEmits() {
        var recorder = new Recorder();
        var stats = new CombatantStats(recorder);
        UUID id = UUID.randomUUID();
        stats.register(id, 100, true);
        stats.damage(id, 60, null, false);                // 40/100, unattributed

        stats.heal(id, 1000, id, true);

        assertEquals(100, stats.current(id), EPS, "healed to the ceiling, not past it");
        assertEquals(HealthChange.Kind.HEAL, recorder.last().kind(), "emitted a HEAL change");
        // Mutation: heal past max / emit DAMAGE -> reddens.
    }

    @Test
    void reconcileEmitsMaxChangeOnceAndIsSilentOnASteadyState() {
        var recorder = new Recorder();
        var stats = new CombatantStats(recorder);
        UUID id = UUID.randomUUID();
        stats.register(id, 100, true);

        stats.reconcileMaxModifiers(id, Map.of("mainhand", 300.0)); // equip
        assertEquals(1, recorder.seen.size(), "equip fired exactly one MAX_CHANGE");
        assertEquals(HealthChange.Kind.MAX_CHANGE, recorder.last().kind(), "and it was a MAX_CHANGE");
        assertEquals(400, recorder.last().max(), EPS, "carrying the new max");

        stats.reconcileMaxModifiers(id, Map.of("mainhand", 300.0)); // still equipped, unchanged
        assertEquals(1, recorder.seen.size(), "a steady reconcile fired NOTHING -- transition fires once");
        // Mutation: emit unconditionally in reconcile -> a standing equipped player spams the seam every tick -> reddens.
    }

    @Test
    void clearDropsStateAndIsBounded() {
        var stats = new CombatantStats();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        stats.register(a, 100, true);
        stats.register(b, 100, true);
        assertEquals(2, stats.trackedCount(), "two combatants tracked");

        stats.clear(a);
        assertEquals(1, stats.trackedCount(), "clear dropped one");
        assertFalse(stats.tracks(a), "and it is gone");
        assertDoesNotThrow(() -> stats.clear(UUID.randomUUID()), "clearing an unknown id is safe");
        // Mutation: clear() no-ops -> map grows for the life of the server -> reddens.
    }

    @Test
    void bootstrapDoesNotClobberAnExistingCombatant() {
        var stats = new CombatantStats();
        UUID id = UUID.randomUUID();
        stats.register(id, 5000, false);                  // a boss already set up (attack base 0)
        stats.damage(id, 1000, null, false);              // 4000/5000

        stats.bootstrapIfAbsent(id, 20, 3, false);        // a later touch tries to bootstrap from vanilla

        assertEquals(5000, stats.max(id), EPS, "bootstrap left the existing boss max alone");
        assertEquals(4000, stats.current(id), EPS, "and its current");
        assertEquals(0, stats.attackValue(id), EPS, "and its attack -- computeIfAbsent, so no re-seed to 3");
        // Mutation: bootstrap uses put instead of computeIfAbsent -> the boss resets to 20/20 and attack 3 -> reddens.
    }

    @Test
    void bootstrapSeedsAttackDamageForaFreshCombatant() {
        var stats = new CombatantStats();
        UUID mob = UUID.randomUUID();

        stats.bootstrapIfAbsent(mob, 40, 6, false);       // a fresh mob, HP + attack from vanilla

        assertEquals(6, stats.attackValue(mob), EPS, "a fresh mob seeds its attack from the bootstrap value");
        // Mutation: bootstrap ignores baseAttack (seeds 0) -> a mob deals no custom melee -> reddens.
    }

    @Test
    void attackValueOfAnUntrackedCombatantIsZeroNotAThrow() {
        var stats = new CombatantStats();
        assertEquals(0, stats.attackValue(UUID.randomUUID()), EPS,
                "an untracked/unarmed combatant resolves 0 attack -- the melee read paths deal nothing, not crash");
        // Mutation: require() (throw) instead of 0 default -> a hit by an unbootstrapped mob crashes the tick -> reddens.
    }

    @Test
    void reconcileAttackModifiersConvergesTheMainHandAndIsSilent() {
        var recorder = new Recorder();
        var stats = new CombatantStats(recorder);
        UUID id = UUID.randomUUID();
        stats.register(id, 100, true);                    // player: attack base 0

        stats.reconcileAttackModifiers(id, Map.of("MAIN_HAND", 8.0));   // hold ironblade (8)
        assertEquals(8, stats.attackValue(id), EPS, "the held weapon's attack_damage lands as a MAIN_HAND modifier");

        stats.reconcileAttackModifiers(id, Map.of("MAIN_HAND", 7.0));   // swap to emberblade (7)
        assertEquals(7, stats.attackValue(id), EPS, "a weapon swap converges the modifier -- no leak of the old 8");

        stats.reconcileAttackModifiers(id, Map.of());                  // unarmed
        assertEquals(0, stats.attackValue(id), EPS, "empty-handed drops the source back to base 0");

        assertTrue(recorder.seen.isEmpty(),
                "attack reconcile is SILENT -- attack has no display seam, the tooltip reads it on demand");
        // Mutation A: drop the remove-loop (reconcile only adds) -> a swap leaks the old 8, unarmed stays 8 -> reddens.
        // Mutation B: emit a HealthChange from reconcileAttackModifiers -> the recorder is non-empty -> reddens.
    }

    /**
     * The divisor-safety rule, and the reason attack speed's absent value differs from attack
     * damage's. Attack damage is a SUMMAND, so 0 correctly means "deals nothing". Attack speed is a
     * DIVISOR: 0 would mean an infinite cooldown, so an untracked caster would silently never swing
     * again. Neutral is the only safe absent value.
     */
    @Test
    void anUntrackedCombatantsAttackSpeedIsNeutralNotZero() {
        var stats = new CombatantStats(new Recorder());

        assertEquals(1.0, stats.attackSpeedValue(UUID.randomUUID()), EPS,
                "an untracked caster swings at the authored cadence, not never");
        // ...and the deliberate contrast with the other stat, so the difference reads as a decision.
        assertEquals(0.0, stats.attackValue(UUID.randomUUID()), EPS,
                "attack damage is a summand, so 0 is the correct absent value there");
        // Mutation: return 0.0 for an untracked attack speed -> AttackSpeed clamps to MIN_SPEED and
        // every untracked caster's cadence becomes 10x slower -> reddens here and in AttackSpeedTest.
    }

    @Test
    void attackSpeedBasesAtNeutralAndReconcilesLeakProof() {
        var recorder = new Recorder();
        var stats = new CombatantStats(recorder);
        UUID id = UUID.randomUUID();
        stats.register(id, 100, true);

        assertEquals(1.0, stats.attackSpeedValue(id), EPS, "no modifiers -> neutral");

        stats.reconcileAttackSpeedModifiers(id, Map.of("MAIN_HAND", 1.0));
        assertEquals(2.0, stats.attackSpeedValue(id), EPS, "+1.0 on a base of 1.0 is a 2.0 multiplier");

        stats.reconcileAttackSpeedModifiers(id, Map.of("OFF_HAND", 0.5));  // swapped source
        assertEquals(1.5, stats.attackSpeedValue(id), EPS, "the old source must not leak");

        stats.reconcileAttackSpeedModifiers(id, Map.of());                 // unequipped
        assertEquals(1.0, stats.attackSpeedValue(id), EPS, "back to neutral, exactly");

        assertTrue(recorder.seen.isEmpty(),
                "attack-speed reconcile is SILENT -- no display seam; it is felt as a faster swing");
        // Mutation A: drop the remove-loop -> the swap leaks and unequip stays boosted -> reddens.
        // Mutation B: emit a HealthChange here -> the recorder is non-empty -> reddens.
    }

    @Test
    void mutatingAnUntrackedCombatantIsANoOpNotAThrow() {
        var recorder = new Recorder();
        var stats = new CombatantStats(recorder);
        assertDoesNotThrow(() -> stats.damage(UUID.randomUUID(), 10, null, false),
                "damaging an untracked combatant does nothing");
        assertTrue(recorder.seen.isEmpty(), "and emits nothing");
        // Mutation: NPE on a missing state -> a stray damage call crashes a region tick -> reddens.
    }

    // --- Class-typed damage ---------------------------------------------------------------------

    /**
     * An UNTRACKED combatant's class bonus is 0.0, matching attackValue and deliberately NOT
     * attackSpeedValue's 1.0. The asymmetry is documented on all three and must not be flattened:
     * this is a summand, so 0 is "adds nothing"; attack speed is a divisor, whose only safe absent
     * value is neutral. Copy the wrong one here and every untracked caster silently hits for +1.
     */
    @Test
    void anUntrackedCombatantsClassDamageIsZeroNotNeutral() {
        var stats = new CombatantStats();
        assertEquals(0.0, stats.classDamageValue(UUID.randomUUID()), EPS,
                "a summand's absent value is 0, not the divisor's 1.0");
        // Mutation: return AttackSpeed.BASE -> 1.0 -> reddens.
    }

    /** The reconcile converges: sources newly present are added, departed ones removed. */
    @Test
    void reconcilingClassDamageAddsAndRemovesSourcesCleanly() {
        var stats = new CombatantStats();
        UUID id = UUID.randomUUID();
        stats.register(id, 100, true);

        stats.reconcileClassDamageModifiers(id, Map.of("OFF_HAND", 5.0, "HEAD", 2.0));
        assertEquals(7.0, stats.classDamageValue(id), EPS, "both equipped grants land");

        // The head slot is emptied -- or its grant stops matching the newly held weapon's class.
        stats.reconcileClassDamageModifiers(id, Map.of("OFF_HAND", 5.0));
        assertEquals(5.0, stats.classDamageValue(id), EPS, "the departed source is dropped, not kept");

        stats.reconcileClassDamageModifiers(id, Map.of());
        assertEquals(0.0, stats.classDamageValue(id), EPS, "an empty desired set clears everything");
    }

    /**
     * A WEAPON SWAP is just an empty desired set followed by a different one, and it must leave no
     * residue. This is the "+3 Magic does nothing while you hold a sword" case end to end, at the
     * store: the mage grant is gone the moment it stops matching, not merely outvoted.
     */
    @Test
    void swappingWeaponClassReplacesTheActiveGrantWithNoLeak() {
        var stats = new CombatantStats();
        UUID id = UUID.randomUUID();
        stats.register(id, 100, true);

        stats.reconcileClassDamageModifiers(id, Map.of("OFF_HAND", 5.0));   // holding a staff
        stats.reconcileClassDamageModifiers(id, Map.of("HEAD", 3.0));       // swapped to a sword

        assertEquals(3.0, stats.classDamageValue(id), EPS,
                "the melee grant alone -- the mage grant did not linger alongside it");
    }

    // --- Enchant damage (a PERCENT, base 0.0) -----------------------------------------------------

    /**
     * An UNTRACKED combatant's enchant percent is 0.0 -- and 0.0 is NEUTRAL here, because
     * {@code DamageEnchants.multiplier(0.0)} is exactly 1.0.
     *
     * That is the whole reason this stat carries a percent rather than a multiplier. A
     * multiplier-valued stat would have needed 1.0 as its absent value, adding a second convention
     * beside attack speed's -- and a 0.0 slip on it would not have been a small buff like copying
     * attack speed's 1.0 here would be, it would have ZEROED every untracked combatant's damage. The
     * percent has no such failure mode: both mistakes are impossible because there is only one
     * sensible absent value.
     */
    @Test
    void anUntrackedCombatantsEnchantPercentIsZeroWhichIsTheNeutralMultiplier() {
        var stats = new CombatantStats();
        assertEquals(0.0, stats.enchantDamagePercentValue(UUID.randomUUID()), EPS,
                "0 percent, which multiplier() turns into exactly x1.0");
        assertEquals(1.0, DamageEnchants.multiplier(stats.enchantDamagePercentValue(UUID.randomUUID())),
                EPS, "so an untracked caster deals exactly what it always did");
        // Mutation: return AttackSpeed.BASE -> 1.0 percent -> x1.01 -> reddens.
    }

    /**
     * The reconcile converges, keyed by ENCHANT ID. Two damage enchants on one weapon arrive as two
     * sources and Stat SUMS their percentages -- 15 + 10 = 25, i.e. x1.25 -- which is the correct
     * composition for percentages and the second reason the stat is not a multiplier: two
     * multiplier-valued sources would have summed to 2.25 rather than composing.
     */
    @Test
    void reconcilingEnchantDamageSumsPercentagesAndDropsDepartedEnchants() {
        var stats = new CombatantStats();
        UUID id = UUID.randomUUID();
        stats.register(id, 100, true);

        stats.reconcileEnchantDamageModifiers(id, Map.of("sharpness", 15.0, "keen", 10.0));
        assertEquals(25.0, stats.enchantDamagePercentValue(id), EPS, "percentages add: x1.25");

        // The weapon is swapped, or a slot is deactivated: that enchant stops being desired.
        stats.reconcileEnchantDamageModifiers(id, Map.of("sharpness", 15.0));
        assertEquals(15.0, stats.enchantDamagePercentValue(id), EPS, "the departed enchant is dropped");

        stats.reconcileEnchantDamageModifiers(id, Map.of());
        assertEquals(0.0, stats.enchantDamagePercentValue(id), EPS,
                "an unenchanted weapon clears everything, back to x1.0");
    }

    /**
     * Sheathing an enchanted weapon for an unenchanted one leaves NO residue -- the same leak-proof
     * property the class-damage swap test pins, on the stat that would otherwise let a dropped sword
     * keep boosting a bow.
     */
    @Test
    void swappingToAnUnenchantedWeaponLeavesNoEnchantResidue() {
        var stats = new CombatantStats();
        UUID id = UUID.randomUUID();
        stats.register(id, 100, true);

        stats.reconcileEnchantDamageModifiers(id, Map.of("sharpness", 15.0));   // holding the sword
        stats.reconcileEnchantDamageModifiers(id, Map.of());                    // swapped to a stick

        assertEquals(0.0, stats.enchantDamagePercentValue(id), EPS,
                "the sword's Sharpness did not follow the hand that dropped it");
    }

    /**
     * The class-damage reconcile is SILENT. It has no display seam -- no heart bar, no nameplate --
     * so emitting a HealthChange would push spurious events at every one of the loop's ticks into
     * the popup and nameplate paths.
     */
    @Test
    void reconcilingClassDamageEmitsNoHealthChange() {
        var recorder = new Recorder();
        var stats = new CombatantStats(recorder);
        UUID id = UUID.randomUUID();
        stats.register(id, 100, true);
        int afterRegister = recorder.seen.size();

        stats.reconcileClassDamageModifiers(id, Map.of("OFF_HAND", 5.0));
        stats.reconcileClassDamageModifiers(id, Map.of());

        assertEquals(afterRegister, recorder.seen.size(),
                "the class-damage stat has no display to notify");
        // Mutation: emit a MAX_CHANGE like reconcileMaxModifiers does -> two extra events -> reddens.
    }

    /** No-op on an untracked combatant, like every other reconcile. Must not throw. */
    @Test
    void reconcilingClassDamageOnAnUntrackedCombatantIsANoOp() {
        var stats = new CombatantStats();
        assertDoesNotThrow(() ->
                stats.reconcileClassDamageModifiers(UUID.randomUUID(), Map.of("OFF_HAND", 5.0)));
    }

    /**
     * A MOB never receives a class bonus. Mobs are not reconciled -- they have no held-weapon class
     * to gate on -- so a bootstrapped mob's bonus stays at base 0 while its attack damage does not.
     */
    @Test
    void aBootstrappedMobHasNoClassDamageBonus() {
        var stats = new CombatantStats();
        UUID id = UUID.randomUUID();
        stats.bootstrapIfAbsent(id, 20, 3.0, false);

        assertEquals(3.0, stats.attackValue(id), EPS, "its vanilla attack damage bootstrapped");
        assertEquals(0.0, stats.classDamageValue(id), EPS, "but class-typed gear is a player concern");
    }
}
