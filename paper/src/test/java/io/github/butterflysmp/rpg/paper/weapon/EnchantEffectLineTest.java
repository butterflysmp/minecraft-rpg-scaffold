package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.enchant.EnchantEffect;
import io.github.butterflysmp.rpg.core.weapon.GearClass;
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
            "sharpness", "Sharpness", 3, EnchantEffect.DAMAGE, GearClass.MELEE, List.of(5, 10, 15));
    private static final EnchantDefinition UNBREAKING = new EnchantDefinition(
            "unbreaking", "Unbreaking", 3, EnchantEffect.DURABILITY, null, List.of());
    /** A universal DAMAGE enchant. No shipped file yet, but the schema permits one, so it is guarded. */
    private static final EnchantDefinition KEEN = new EnchantDefinition(
            "keen", "Keen", 3, EnchantEffect.DAMAGE, null, List.of(5, 10, 15));
    // The two shield files. bulwark.yml / riposte.yml.
    private static final EnchantDefinition BULWARK = new EnchantDefinition(
            "bulwark", "Bulwark", 3, EnchantEffect.BLOCK_DR, GearClass.SHIELD, List.of(5, 10, 15));
    private static final EnchantDefinition RIPOSTE = new EnchantDefinition(
            "riposte", "Riposte", 3, EnchantEffect.REFLECT, GearClass.SHIELD, List.of(10, 20, 30));

    @Test
    void damageEnchantReportsItsPercentAndMultiplier() {
        // Byte-for-byte what boot gate step 2 reads off the screen before any swing.
        assertEquals(" (+15% damage, x1.15)",
                EnchantEffectLine.of(SHARPNESS, 3, GearClass.MELEE));
    }

    @Test
    void aDamageEnchantNeverReportsADurabilityRate() {
        // THE bug. Sharpness consumes durability at the vanilla rate like every other item; what it
        // does not do is have a consume RATE to report, and reporting one is a claim about an
        // effect it does not have.
        String line = EnchantEffectLine.of(SHARPNESS, 3, GearClass.MELEE);
        assertFalse(line.contains("durability"),
                "a damage enchant described itself as consuming durability: " + line);
    }

    @Test
    void durabilityEnchantReportsTheConsumeRate() {
        assertEquals(" (consumes durability on 25% of uses)",
                EnchantEffectLine.of(UNBREAKING, 3, GearClass.MELEE));
    }

    @Test
    void theConsumeRateTracksTheLevel() {
        // Level 2 pins the rounding: 1/3 -> 33%, not 34% and not 0.33.
        assertEquals(" (consumes durability on 50% of uses)",
                EnchantEffectLine.of(UNBREAKING, 1, GearClass.MELEE));
        assertEquals(" (consumes durability on 33% of uses)",
                EnchantEffectLine.of(UNBREAKING, 2, GearClass.MELEE));
    }

    @Test
    void aClassGatedDamageEnchantIsInertOnTheWrongClass() {
        // The DISPLAY label, not the enum name: RANGER renders "Ranged". Boot gate step 5.
        //
        // BYTE-IDENTICAL to what Slice 1 produced, and that is the point of asserting it unchanged
        // through the GearClass migration: the held side became a noun phrase (GearClassLabel.
        // describe) instead of a label with " weapon" glued on, and for every weapon class the two
        // spellings agree exactly. If this line ever needs editing for a migration, the migration
        // changed behaviour on the weapon path.
        assertEquals(" (inert: a Melee enchant on a Ranged weapon)",
                EnchantEffectLine.of(SHARPNESS, 3, GearClass.RANGER));
    }

    @Test
    void aWeaponEnchantOnAShieldSaysShieldRatherThanShieldWeapon() {
        // The sentence the hardcoded " weapon" suffix could not produce. A player CAN reach this:
        // /rpg enchant candidate puts any enchant on any gear, so Sharpness on a shield is a real
        // state and its description has to read as English.
        //
        // Reachable in play too, which is why it is worth wording: a shield rolls its own pool, but
        // the dev command and a hand-edited item both bypass the roll.
        assertEquals(" (inert: a Melee enchant on a shield)",
                EnchantEffectLine.of(SHARPNESS, 3, GearClass.SHIELD));
    }

    @Test
    void aUniversalEnchantIsStillNotInertOnAShield() {
        // Unbreaking on a shield is the one enchant combination Slice 1 shipped working, and the
        // migration must not have broken it. It never touches heldClass at all -- the DURABILITY
        // arm does not consult the gate -- so this also pins that the arm stayed class-blind.
        assertEquals(" (consumes durability on 25% of uses)",
                EnchantEffectLine.of(UNBREAKING, 3, GearClass.SHIELD));
        assertEquals(" (+15% damage, x1.15)",
                EnchantEffectLine.of(KEEN, 3, GearClass.SHIELD));
    }

    @Test
    void aUniversalDamageEnchantIsNotInertAnywhere() {
        // Guards isUniversal(): deleting it makes every universal damage enchant inert everywhere,
        // because null != RANGER.
        assertEquals(" (+15% damage, x1.15)",
                EnchantEffectLine.of(KEEN, 3, GearClass.RANGER));
    }

    @Test
    void anUnknownEnchantGrantsNothing() {
        // An id whose content file went missing. The loader fail-softs it; the item's blob still
        // names it. Saying so beats inventing a number for it.
        assertEquals(" (unknown enchant -- grants nothing)",
                EnchantEffectLine.of(null, 3, GearClass.MELEE));
    }

    @Test
    void theTwoEffectsDoNotShareAWording() {
        // The invariant the whole fix is about, asserted without depending on the exact prose: any
        // single description reused across both effects fails here, whichever one it describes.
        assertNotEquals(EnchantEffectLine.of(UNBREAKING, 3, GearClass.MELEE),
                EnchantEffectLine.of(SHARPNESS, 3, GearClass.MELEE));
    }

    @Test
    void noTWOEffectsShareAWordingIncludingTheTwoShieldOnesThatLookAlike() {
        // Generalised from the pair above, because BLOCK_DR and REFLECT are the arms most likely to
        // collide: near-identical code, both `String.format("+%.0f%% ...", percent)`, both shield.
        //
        // WHAT THIS DOES AND DOES NOT CATCH -- measured, because the first draft of this comment
        // claimed the wrong thing. It catches two arms rendering IDENTICAL text. It does NOT catch
        // the copy-paste that leaves "block" in the reflect arm: the two curves differ, so the
        // strings come out "+15% block" and "+30% block", which are unequal and pass here.
        // theReflectArmNamesTHEATTACKER... is what actually reddens on that mutation (run: 1 failure,
        // "expected: < (+30% reflected to the attacker)> but was: < (+30% block)>").
        //
        // Both tests earn their place; this one is the weaker net and says so rather than implying
        // a coverage it does not have.
        EnchantDefinition[] all = {SHARPNESS, UNBREAKING, BULWARK, RIPOSTE};
        for (int i = 0; i < all.length; i++) {
            for (int j = i + 1; j < all.length; j++) {
                // Each on the gear it is actually valid for, so none is describing itself as inert.
                String first = EnchantEffectLine.bare(all[i], 3, all[i].gearClass() == null
                        ? GearClass.MELEE : all[i].gearClass());
                String second = EnchantEffectLine.bare(all[j], 3, all[j].gearClass() == null
                        ? GearClass.MELEE : all[j].gearClass());
                assertNotEquals(first, second,
                        all[i].id() + " and " + all[j].id() + " describe themselves identically");
            }
        }
    }

    @Test
    void theBlockArmSaysPOINTSAndNotAMultiplier() {
        // 2a shipped this arm with NO test at all -- `grep -c BLOCK_DR` on this file returned 0 --
        // and it is the line the boot gate reads off the screen BEFORE blocking.
        //
        // "+15% block" means the shield stops fifteen more POINTS of the hit (0.50 -> 0.65), because
        // Bulwark is additive on the fraction. An "x1.15" here would describe the multiplicative
        // reading that was explicitly rejected, and would be a promise the mechanic does not keep.
        assertEquals(" (+15% block)", EnchantEffectLine.of(BULWARK, 3, GearClass.SHIELD));
        assertEquals(" (+5% block)", EnchantEffectLine.of(BULWARK, 1, GearClass.SHIELD));
        assertFalse(EnchantEffectLine.bare(BULWARK, 3, GearClass.SHIELD).contains("x1."),
                "a multiplier here would describe the rejected composition");
    }

    @Test
    void theReflectArmNamesTHEATTACKERSoItCannotReadAsADamageBonus() {
        // Without "to the attacker" this reads as a bonus to your own hits. And it deliberately says
        // nothing about blocking, because the percent is of the INCOMING blow, not of what got
        // through -- the pre-mitigation rule, restated where a player can see it.
        assertEquals(" (+30% reflected to the attacker)",
                EnchantEffectLine.of(RIPOSTE, 3, GearClass.SHIELD));
        assertEquals(" (+10% reflected to the attacker)",
                EnchantEffectLine.of(RIPOSTE, 1, GearClass.SHIELD));

        String reflect = EnchantEffectLine.bare(RIPOSTE, 3, GearClass.SHIELD);
        assertTrue(reflect.contains("attacker"), "the target of the reflect must be named");
        assertFalse(reflect.contains("block"),
                "the reflect is off the incoming blow, not off what the shield stopped");
        assertFalse(reflect.contains("damage"),
                "'damage' here would read as a bonus to the wearer's own hits");
    }

    @Test
    void bothShieldEnchantsAreInertOnAWeaponAndSayWhichGearTheyWanted() {
        // Reachable: /rpg enchant candidate puts any enchant on any gear, and a hand-edited item can
        // carry anything. The content boundary refuses these gates in a FILE; it does not refuse
        // them on an item.
        assertEquals(" (inert: a Shield enchant on a Ranged weapon)",
                EnchantEffectLine.of(BULWARK, 3, GearClass.RANGER));
        assertEquals(" (inert: a Shield enchant on a Magic weapon)",
                EnchantEffectLine.of(RIPOSTE, 3, GearClass.MAGE));
    }

    @Test
    void bareCarriesNoBracketsAndNoLeadingSpace() {
        // What a LORE LINE renders. A parenthetical aside reads as an afterthought on a tooltip,
        // where this text IS the description of the enchant.
        assertEquals("+15% damage, x1.15", EnchantEffectLine.bare(SHARPNESS, 3, GearClass.MELEE));
        assertEquals("consumes durability on 25% of uses",
                EnchantEffectLine.bare(UNBREAKING, 3, GearClass.MELEE));
        // Mutation: return of(..) from bare(..) -> the leading space and brackets come back -> reddens.
    }

    @Test
    void theBracketedFormIsExactlyTheBareOneWrapped() {
        // The two forms cannot drift, which is the whole reason bare() was extracted rather than
        // the menu trimming the brackets off of()'s output itself. Asserted over every arm --
        // damage, durability, inert and unknown -- so no single arm can be changed in one form only.
        for (EnchantDefinition def : new EnchantDefinition[] {SHARPNESS, UNBREAKING, KEEN,
                BULWARK, RIPOSTE, null}) {
            for (GearClass held : GearClass.values()) {
                assertEquals(" (" + EnchantEffectLine.bare(def, 3, held) + ")",
                        EnchantEffectLine.of(def, 3, held));
            }
        }
        // Mutation: change either form independently -> reddens.
    }
}
