package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.enchant.EnchantEffect;
import io.github.butterflysmp.rpg.core.weapon.WeaponClass;
import io.github.butterflysmp.rpg.paper.content.EnchantDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The effect dispatch, in the two-second loop.
 *
 * <p>Written against a shipped bug: {@code /rpg enchant active} appended Unbreaking's consume rate
 * to EVERY enchant, so activating Sharpness reported "consumes durability on 25% of uses". The
 * describer was Bukkit-free logic living as a private method inside a Bukkit-facing command class,
 * which is why nothing could test it and why the boot gate -- which checked {@code show}'s output,
 * not the activation reply -- did not catch it. Same move as {@code ApplyArgs}.
 */
class EnchantEffectLineTest {

    // The shipped content files, as records. sharpness.yml / unbreaking.yml.
    private static final EnchantDefinition SHARPNESS = new EnchantDefinition(
            "sharpness", "Sharpness", 3, EnchantEffect.DAMAGE, WeaponClass.MELEE, List.of(5, 10, 15));
    private static final EnchantDefinition UNBREAKING = new EnchantDefinition(
            "unbreaking", "Unbreaking", 3, EnchantEffect.DURABILITY, null, List.of());
    /** A universal DAMAGE enchant. No shipped file yet, but the schema permits one, so it is guarded. */
    private static final EnchantDefinition KEEN = new EnchantDefinition(
            "keen", "Keen", 3, EnchantEffect.DAMAGE, null, List.of(5, 10, 15));

    @Test
    void damageEnchantReportsItsPercentAndMultiplier() {
        // Byte-for-byte what boot gate step 2 reads off the screen before any swing.
        assertEquals(" (+15% damage, x1.15)",
                EnchantEffectLine.of(SHARPNESS, 3, WeaponClass.MELEE));
    }

    @Test
    void aDamageEnchantNeverReportsADurabilityRate() {
        // THE bug. Sharpness consumes durability at the vanilla rate like every other item; what it
        // does not do is have a consume RATE to report, and reporting one is a claim about an
        // effect it does not have.
        String line = EnchantEffectLine.of(SHARPNESS, 3, WeaponClass.MELEE);
        assertFalse(line.contains("durability"),
                "a damage enchant described itself as consuming durability: " + line);
    }

    @Test
    void durabilityEnchantReportsTheConsumeRate() {
        assertEquals(" (consumes durability on 25% of uses)",
                EnchantEffectLine.of(UNBREAKING, 3, WeaponClass.MELEE));
    }

    @Test
    void theConsumeRateTracksTheLevel() {
        // Level 2 pins the rounding: 1/3 -> 33%, not 34% and not 0.33.
        assertEquals(" (consumes durability on 50% of uses)",
                EnchantEffectLine.of(UNBREAKING, 1, WeaponClass.MELEE));
        assertEquals(" (consumes durability on 33% of uses)",
                EnchantEffectLine.of(UNBREAKING, 2, WeaponClass.MELEE));
    }

    @Test
    void aClassGatedDamageEnchantIsInertOnTheWrongClass() {
        // The DISPLAY label, not the enum name: RANGER renders "Ranged". Boot gate step 5.
        assertEquals(" (inert: a Melee enchant on a Ranged weapon)",
                EnchantEffectLine.of(SHARPNESS, 3, WeaponClass.RANGER));
    }

    @Test
    void aUniversalDamageEnchantIsNotInertAnywhere() {
        // Guards isUniversal(): deleting it makes every universal damage enchant inert everywhere,
        // because null != RANGER.
        assertEquals(" (+15% damage, x1.15)",
                EnchantEffectLine.of(KEEN, 3, WeaponClass.RANGER));
    }

    @Test
    void anUnknownEnchantGrantsNothing() {
        // An id whose content file went missing. The loader fail-softs it; the item's blob still
        // names it. Saying so beats inventing a number for it.
        assertEquals(" (unknown enchant -- grants nothing)",
                EnchantEffectLine.of(null, 3, WeaponClass.MELEE));
    }

    @Test
    void theTwoEffectsDoNotShareAWording() {
        // The invariant the whole fix is about, asserted without depending on the exact prose: any
        // single description reused across both effects fails here, whichever one it describes.
        assertNotEquals(EnchantEffectLine.of(UNBREAKING, 3, WeaponClass.MELEE),
                EnchantEffectLine.of(SHARPNESS, 3, WeaponClass.MELEE));
    }

    @Test
    void bareCarriesNoBracketsAndNoLeadingSpace() {
        // What a LORE LINE renders. A parenthetical aside reads as an afterthought on a tooltip,
        // where this text IS the description of the enchant.
        assertEquals("+15% damage, x1.15", EnchantEffectLine.bare(SHARPNESS, 3, WeaponClass.MELEE));
        assertEquals("consumes durability on 25% of uses",
                EnchantEffectLine.bare(UNBREAKING, 3, WeaponClass.MELEE));
        // Mutation: return of(..) from bare(..) -> the leading space and brackets come back -> reddens.
    }

    @Test
    void theBracketedFormIsExactlyTheBareOneWrapped() {
        // The two forms cannot drift, which is the whole reason bare() was extracted rather than
        // the menu trimming the brackets off of()'s output itself. Asserted over every arm --
        // damage, durability, inert and unknown -- so no single arm can be changed in one form only.
        for (EnchantDefinition def : new EnchantDefinition[] {SHARPNESS, UNBREAKING, KEEN, null}) {
            for (WeaponClass held : WeaponClass.values()) {
                assertEquals(" (" + EnchantEffectLine.bare(def, 3, held) + ")",
                        EnchantEffectLine.of(def, 3, held));
            }
        }
        // Mutation: change either form independently -> reddens.
    }
}
