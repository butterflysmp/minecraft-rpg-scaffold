package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.ability.ResourceCost;
import io.github.butterflysmp.rpg.core.ability.effect.DamagePayload;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.weapon.WeaponLoreLines;
import org.junit.jupiter.api.Test;

import java.util.OptionalInt;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reddening tests for the pure tooltip formatters. Each asserts the exact string/number a tooltip
 * clause should read, so breaking a divisor, dropping the recursion, or mis-labelling a clause
 * fails here in the 2-second loop rather than surfacing only at boot.
 */
class WeaponLoreLinesTest {

    // --- cooldownLabel: divide by 20, one decimal. Redden by changing the /20. ---

    @Test
    void cooldownLabelHalfSecond() {
        assertEquals("0.5s", WeaponLoreLines.cooldownLabel(10));
    }

    @Test
    void cooldownLabelKeepsTrailingDecimal() {
        assertEquals("3.0s", WeaponLoreLines.cooldownLabel(60));
    }

    // --- inputLabel: "left_click" -> "Left-Click". ---

    @Test
    void inputLabelLeftClick() {
        assertEquals("Left-Click", WeaponLoreLines.inputLabel("left_click"));
    }

    @Test
    void inputLabelRightClick() {
        assertEquals("Right-Click", WeaponLoreLines.inputLabel("right_click"));
    }

    // --- cadenceLine: cooldown, folded with the cost clause when costed. ---

    @Test
    void cadenceCooldownOnlyWhenFree() {
        assertEquals("Cooldown: 0.5s", WeaponLoreLines.cadenceLine(10, ResourceCost.FREE));
    }

    @Test
    void cadenceFoldsCostAfterCooldown() {
        assertEquals("Cooldown: 3.0s | Mana Cost: 40",
                WeaponLoreLines.cadenceLine(60, new ResourceCost("mana", 40)));
    }

    @Test
    void cadenceResourceNameComesFromResourceId() {
        assertEquals("Cooldown: 1.0s | Focus Cost: 30",
                WeaponLoreLines.cadenceLine(20, new ResourceCost("focus", 30)));
    }

    @Test
    void cadenceBlankWhenFreeAndInstant() {
        assertEquals("", WeaponLoreLines.cadenceLine(0, ResourceCost.FREE));
    }

    // --- attack speed: ATTACKS PER SECOND, from TWO sources. Melee reads the weapon's authored
    // cadence (vanilla paces the swing); ranged still derives 20/ticks from its trigger cooldown
    // (CooldownTracker paces the shot). Each reads the number that actually governs it. ---

    @Test
    void attackSpeedIsAttacksPerSecondNotSeconds() {
        // 10 ticks between swings is TWO attacks a second. The number must be 2.0, not 0.5 --
        // higher is better here, the opposite of the cooldown lines.
        assertEquals("2.0", WeaponLoreLines.rangedAttackSpeedLabel(10));
    }

    @Test
    void attackSpeedRoundsToOneDecimal() {
        assertEquals("1.3", WeaponLoreLines.rangedAttackSpeedLabel(15));
    }

    @Test
    void attackSpeedOfAZeroCooldownIsBlankRatherThanInfinity() {
        // The guard that keeps a divide-by-zero off a player's tooltip; the caller drops the line.
        assertEquals("", WeaponLoreLines.rangedAttackSpeedLabel(0));
        assertEquals("", WeaponLoreLines.rangedAttackSpeedLabel(-5));
    }


    @Test
    void meleeAttackSpeedIsTheWeaponsAuthoredCadenceNotADerivedOne() {
        // Every vanilla sword is 1.6, and 20/n cannot produce 1.6 for any integer n -- which is the
        // whole reason melee authors the number instead of deriving it from a tick count.
        assertEquals("1.6", WeaponLoreLines.meleeAttackSpeedLabel(1.6));
        assertEquals("2.0", WeaponLoreLines.meleeAttackSpeedLabel(2.0));
        // Mutation: route melee through the ranged formatter -> 20/1.6 prints "12.5" -> reddens.
    }

    @Test
    void meleeAndRangedReadDifferentSourcesAndMustNotBeSwapped() {
        // The same tooltip label, two governing numbers. A weapon authoring 1.6 whose trigger also
        // carried a 15-tick cooldown must show 1.6, not 1.3 -- and the fixture is chosen so the two
        // formatters CANNOT agree by accident, which is what makes a swap visible.
        assertEquals("1.6", WeaponLoreLines.meleeAttackSpeedLabel(1.6));
        assertEquals("1.3", WeaponLoreLines.rangedAttackSpeedLabel(15));
        // Mutation: swap the two arms in WeaponLore's stat block -> the sword reads 1.3 and the bow
        // reads its authored 0.0 (blank, dropping the line entirely) -> reddens there.
    }

    @Test
    void meleeAttackSpeedOfAnUndeclaredWeaponIsBlankRatherThanZero() {
        // Unreachable for a shipped melee weapon -- WeaponDefinition rejects one with no speed --
        // so this covers the weapon that has no melee basic at all, and the caller drops the line.
        assertEquals("", WeaponLoreLines.meleeAttackSpeedLabel(0.0));
        assertEquals("", WeaponLoreLines.meleeAttackSpeedLabel(-1.0));
    }

    // --- triggerDamage: the number, its element, and WHERE IT CAME FROM. ---
    // The source discriminates a basic attack from an ability payload, which is what decides
    // whether the tooltip renders a stat block or an ability block. Redden by collapsing it.

    @Test
    void weaponDamageReadsTheWeaponAttackDamageAndIsAWeaponStat() {
        var onHit = List.<EffectSpec>of(new EffectSpec.WeaponDamage("kinetic"));

        var damage = WeaponLoreLines.triggerDamage(onHit, 8).orElseThrow();

        assertEquals(8, damage.amount());
        assertEquals("kinetic", damage.element());
        assertEquals(DamagePayload.DamageSource.WEAPON_STAT, damage.source(),
                "weapon_damage READS the attack-damage stat, so it is a basic attack");
    }

    @Test
    void literalDamageUsesItsOwnAmountAndIsAnAbilityLiteral() {
        var onHit = List.<EffectSpec>of(new EffectSpec.Damage(6, "fire"));

        var damage = WeaponLoreLines.triggerDamage(onHit, 99).orElseThrow();

        assertEquals(6, damage.amount(), "the literal wins; the weapon's attack damage is ignored");
        assertEquals("fire", damage.element());
        assertEquals(DamagePayload.DamageSource.ABILITY_LITERAL, damage.source(),
                "a literal reads no stat, so no class-typed modifier can reach it");
    }

    @Test
    void damageInsideABurstIsFoundWithItsElementAndSource() {
        // A cosmetic visual, then a burst carrying the damage -- the number must come from inside it,
        // and the element and source must survive the recursion rather than being lost on the way up.
        var onHit = List.<EffectSpec>of(
                new EffectSpec.Visual("solar_detonation"),
                new EffectSpec.Burst(3.0, List.of(
                        new EffectSpec.Damage(12, "fire"),
                        new EffectSpec.Status("scorch", 40, 0))));

        var damage = WeaponLoreLines.triggerDamage(onHit, 0).orElseThrow();

        assertEquals(12, damage.amount());
        assertEquals("fire", damage.element());
        assertEquals(DamagePayload.DamageSource.ABILITY_LITERAL, damage.source());
    }

    @Test
    void noDamageEffectYieldsEmpty() {
        var onHit = List.<EffectSpec>of(new EffectSpec.Heal(6));
        assertFalse(WeaponLoreLines.triggerDamage(onHit, 5).isPresent());
    }

    @Test
    void firstDamageBearingEffectWins() {
        var onHit = List.<EffectSpec>of(
                new EffectSpec.Damage(3, "fire"),
                new EffectSpec.Damage(99, "fire"));
        assertEquals(3, WeaponLoreLines.triggerDamage(onHit, 0).orElseThrow().amount());
    }

    // ---------------------------------------------------------------- the quiver line

    /**
     * THE FIRST TOOLTIP LINE IN THIS FILE THAT IS PER-ITEM RATHER THAN PER-DEFINITION.
     *
     * <p>Every other line here is a function of the weapon's content, so two copies of a weapon
     * render identically forever. This one differs between two stacks of the SAME weapon and changes
     * as one is fired -- which is why the count arrives as a parameter read off the item, and why the
     * stamp must precede {@code applyLore} at the mint.
     */
    @Test
    void theQuiverLineShowsLoadedOverCapacity() {
        assertEquals("Quiver: 9/9", WeaponLoreLines.quiverLine(OptionalInt.of(9), 9));
        assertEquals("Quiver: 8/9", WeaponLoreLines.quiverLine(OptionalInt.of(8), 9));
        assertEquals("Quiver: 0/9", WeaponLoreLines.quiverLine(OptionalInt.of(0), 9),
                "a SPENT magazine reads 0 -- it is a real count, and distinct from an absent one");
    }

    /** A weapon with no magazine renders no line at all, and the caller drops it. */
    @Test
    void aWeaponWithNoQuiverRendersNoQuiverLine() {
        assertEquals("", WeaponLoreLines.quiverLine(OptionalInt.of(4), 0),
                "capacity 0 is the 0-is-absent convention: no quiver, no line, whatever is stored");
        assertEquals("", WeaponLoreLines.quiverLine(OptionalInt.empty(), 0));
    }

    /**
     * ABSENCE RENDERS DASHES, NOT ZERO -- the feature's rule, carried all the way to the player.
     *
     * <p>An unstamped item is a defect in a mint path. Rendering it as {@code 0/9} would disguise
     * that as a spent magazine, which is a tooltip a player reads as ordinary and nobody reports.
     * The dashes are meant to look wrong, because something IS wrong.
     */
    @Test
    void anAbsentCountRendersDashesRatherThanZero() {
        assertEquals("Quiver: --/9", WeaponLoreLines.quiverLine(OptionalInt.empty(), 9));
    }

    // --- deliveredShots: the "x N" criterion. Redden by returning 1 for a Volley. ---

    /**
     * EVERY FIXTURE STAGES windup, shots AND interval AT DIFFERENT VALUES, DELIBERATELY.
     *
     * <p>{@code Volley(windupTicks, shots, intervalTicks, of)} is three ints in a row, so a fixture
     * that staged any two of them equal could not detect a transposition -- the row would pass while
     * reading the wrong field. Staged `20/6/2`, `10/3/5`, `20/8/1` and `7/1/4`, no two equal within
     * a row.
     *
     * <p><b>METHODS, NOT STATIC FIELDS, AND THAT IS A SCAR.</b> These began as
     * {@code private static final} fixtures, and an invalid one -- a {@code Projectile} declaring
     * both {@code item} and {@code body}, which its constructor refuses -- threw in {@code <clinit>}
     * and errored <b>all 26 tests in this class</b>, including the 22 that predate this section.
     * Only the FIRST report named the real cause; the other 25 read {@code NoClassDefFoundError}.
     * A factory method throws only for the rows that call it.
     */
    private static CastSpec.Ray ray() {
        return new CastSpec.Ray(32.0, "beam");
    }

    private static CastSpec.Projectile bolt() {
        // item and body are MUTUALLY EXCLUSIVE -- a projectile declares ONE body.
        return new CastSpec.Projectile(1.4, 0.0, 60, "trail", "ARROW", null, null);
    }

    /** The shipped counts, read off content rather than invented: cursed_emerald authors 6. */
    @Test
    void aVolleyRendersItsAuthoredShotCount() {
        assertEquals(OptionalInt.of(6), WeaponLoreLines.deliveredShots(
                        new CastSpec.Volley(20, 6, 2, ray())),
                "cursed_emerald: shots 6, NOT windup 20 or interval 2");
        assertEquals(OptionalInt.of(3), WeaponLoreLines.deliveredShots(
                        new CastSpec.Volley(10, 3, 5, bolt())),
                "volley_stone right-click: shots 3");
        assertEquals(OptionalInt.of(8), WeaponLoreLines.deliveredShots(
                        new CastSpec.Volley(20, 8, 1, ray())),
                "volley_stone left-click: shots 8");
    }

    /**
     * A ONE-SHOT VOLLEY RENDERS NOTHING, AND THIS IS THE ONLY ROW THAT GUARDS THE {@code > 1}.
     *
     * <p>It is legal content -- {@code Volley}'s constructor refuses only {@code shots < 1} -- and
     * the question the renderer asks is "is there a multiplier worth printing", not "is this a
     * volley". A literal {@code x 1} on a weapon is noise.
     */
    @Test
    void aSingleShotVolleyRendersNoMultiplier() {
        assertEquals(OptionalInt.empty(), WeaponLoreLines.deliveredShots(
                new CastSpec.Volley(7, 1, 4, ray())));
    }

    /**
     * EVERY OTHER SHAPE IS ONE PAYLOAD, so none renders a multiplier.
     *
     * <p><b>This row is the criterion, not a completeness exercise.</b> A fan or a spread is
     * excluded because its payloads arrive AT ONCE across fixed angles -- no single target receives
     * them in full -- and neither {@code ThrowEmbers} nor {@code DrawFan} is a {@code CastSpec} at
     * all, which is why they cannot reach this method to be wrongly counted.
     */
    @Test
    void everySingleShotShapeRendersNoMultiplier() {
        assertEquals(OptionalInt.empty(), WeaponLoreLines.deliveredShots(ray()));
        assertEquals(OptionalInt.empty(), WeaponLoreLines.deliveredShots(bolt()));
        assertEquals(OptionalInt.empty(), WeaponLoreLines.deliveredShots(new CastSpec.Self()));
        assertEquals(OptionalInt.empty(), WeaponLoreLines.deliveredShots(new CastSpec.Melee(3.5, 60.0)));
        assertEquals(OptionalInt.empty(), WeaponLoreLines.deliveredShots(
                new CastSpec.Dash(8.0, 1.2, 0.3, CastSpec.DashDirection.REVERSE_FACING)));
    }

    /**
     * A VOLLEY OF A VOLLEY CANNOT BE AUTHORED, so nothing here has to decide what it would mean.
     *
     * <p>Recorded as a row rather than as a comment because it is the one case where "how many does
     * one press deliver" would need multiplying, and the reason it never arises is a refusal in
     * {@code Volley}'s own compact constructor rather than anything this method does.
     */
    @Test
    void aVolleyOfAVolleyIsRefusedByTheCastItself() {
        assertThrows(IllegalArgumentException.class,
                () -> new CastSpec.Volley(20, 6, 2, new CastSpec.Volley(10, 3, 5, ray())));
    }
}
