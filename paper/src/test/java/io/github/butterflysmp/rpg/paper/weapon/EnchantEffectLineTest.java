package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.enchant.EnchantEffect;
import io.github.butterflysmp.rpg.core.weapon.ArmorLoreLines;
import io.github.butterflysmp.rpg.core.weapon.GearClass;
import io.github.butterflysmp.rpg.core.weapon.ShieldLoreLines;
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
    // The two shield files. bulwark.yml / thorns.yml.
    private static final EnchantDefinition BULWARK = new EnchantDefinition(
            "bulwark", "Bulwark", 3, EnchantEffect.BLOCK_DR, GearClass.SHIELD, List.of(5, 10, 15));
    private static final EnchantDefinition THORNS = new EnchantDefinition(
            "thorns", "Thorns", 3, EnchantEffect.REFLECT, GearClass.SHIELD, List.of(10, 20, 30));
    // The two armor files. protection.yml / growth.yml. Their curves are POINTS, not percentages.
    private static final EnchantDefinition PROTECTION = new EnchantDefinition(
            "protection", "Protection", 3, EnchantEffect.DEFENSE, GearClass.ARMOR, List.of(3, 6, 9));
    private static final EnchantDefinition GROWTH = new EnchantDefinition(
            "growth", "Growth", 3, EnchantEffect.MAX_HEALTH, GearClass.ARMOR, List.of(10, 20, 30));
    private static final EnchantDefinition MANA_BANK = new EnchantDefinition(
            "mana_bank", "Mana Bank", 3, EnchantEffect.MAX_MANA, GearClass.ARMOR, List.of(10, 20, 30));

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
        EnchantDefinition[] all = {SHARPNESS, UNBREAKING, BULWARK, THORNS, PROTECTION, GROWTH, MANA_BANK};
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
    void theBlockArmSaysPOINTSAndNotAMultiplierAndUsesTheITEMSWords() {
        // 2a shipped this arm with NO test at all -- `grep -c BLOCK_DR` on this file returned 0 --
        // and it is the line the boot gate reads off the screen BEFORE blocking.
        //
        // "+15% Damage Reduction" means the shield stops fifteen more POINTS of the hit
        // (0.35 -> 0.50), because Bulwark is additive on the fraction. An "x1.15" would describe the
        // multiplicative reading that was explicitly rejected.
        //
        // THE WORDS ARE THE ITEM'S. ShieldLoreLines.DAMAGE_REDUCTION_LABEL puts "Damage Reduction"
        // on the shield itself, so the enchant that modifies that stat must not call it something
        // else -- "+15% block" above "Damage Reduction: 50%" makes a player work out that those are
        // one number. Asserted against the constant, not a copy of the string, so a reword of the
        // item moves both or fails here.
        assertEquals(" (+15% Damage Reduction)", EnchantEffectLine.of(BULWARK, 3, GearClass.SHIELD));
        assertEquals(" (+5% Damage Reduction)", EnchantEffectLine.of(BULWARK, 1, GearClass.SHIELD));
        assertTrue(EnchantEffectLine.bare(BULWARK, 3, GearClass.SHIELD)
                        .contains(ShieldLoreLines.DAMAGE_REDUCTION_LABEL.replace(": ", "")),
                "the enchant and the item must name the stat identically");
        assertFalse(EnchantEffectLine.bare(BULWARK, 3, GearClass.SHIELD).contains("x1."),
                "a multiplier here would describe the rejected composition");
    }

    @Test
    void theReflectArmNamesTHEATTACKERSoItCannotReadAsADamageBonus() {
        // Without "to the attacker" this reads as a bonus to your own hits. And it deliberately says
        // nothing about blocking, because the percent is of the INCOMING blow, not of what got
        // through -- the pre-mitigation rule, restated where a player can see it.
        assertEquals(" (+30% reflected to the attacker)",
                EnchantEffectLine.of(THORNS, 3, GearClass.SHIELD));
        assertEquals(" (+10% reflected to the attacker)",
                EnchantEffectLine.of(THORNS, 1, GearClass.SHIELD));

        String reflect = EnchantEffectLine.bare(THORNS, 3, GearClass.SHIELD);
        assertTrue(reflect.contains("attacker"), "the target of the reflect must be named");
        assertFalse(reflect.contains("block"),
                "the reflect is off the incoming blow, not off what the shield stopped");
        assertFalse(reflect.contains("damage"),
                "'damage' here would read as a bonus to the wearer's own hits");
    }

    @Test
    void theArmorArmsSayPOINTSWithNoPercentSignAtAll() {
        // THE SAME GAP THE BLOCK ARM SHIPPED WITH IN 2b, and this file already says so three tests
        // up: "2a shipped this arm with NO test at all -- grep -c BLOCK_DR on this file returned 0".
        // Slice 2a then shipped DEFENSE and MAX_HEALTH the same way. Neither ProtectionTest nor
        // GrowthTest can reach here (they are core; this is paper), and the golden dump cannot
        // either, because no SHIPPED item carries an active Protection or Growth -- the golden
        // renders definitions, and enchant state lives on instances. So this is the only net.
        //
        // NO PERCENT SIGN. Protection and Growth grant flat POINTS: the piece's own Defense line
        // reads "Defense: 8" and Protection III takes it to 17, an addition, not a fraction of it.
        // "+9%" would describe an enchant that does not exist and would disagree with the item two
        // lines above it. This is the one assertion that separates the new arms from the three
        // percent-valued ones they were copied from.
        assertEquals(" (+3 Defense)", EnchantEffectLine.of(PROTECTION, 1, GearClass.ARMOR));
        assertEquals(" (+6 Defense)", EnchantEffectLine.of(PROTECTION, 2, GearClass.ARMOR));
        assertEquals(" (+9 Defense)", EnchantEffectLine.of(PROTECTION, 3, GearClass.ARMOR));
        assertEquals(" (+10 Max Health)", EnchantEffectLine.of(GROWTH, 1, GearClass.ARMOR));
        assertEquals(" (+30 Max Health)", EnchantEffectLine.of(GROWTH, 3, GearClass.ARMOR));

        assertFalse(EnchantEffectLine.bare(PROTECTION, 3, GearClass.ARMOR).contains("%"),
                "a percent sign here would describe a fraction of the piece's Defense, not points");
        assertFalse(EnchantEffectLine.bare(GROWTH, 3, GearClass.ARMOR).contains("%"),
                "and max health is points too -- nothing in either arm divides");
        // Mutation: restore the %% the arms were copied from -> "+9% Defense" -> reddens.
    }

    @Test
    void theArmorArmsUseTheITEMSOwnWordsAndGrowthSaysMAXHealth() {
        // The words are the item's, the rule the block arm set. ArmorLoreLines.DEFENSE_LABEL puts
        // "Defense" on the piece itself, so the enchant that modifies that stat must not call it
        // "Armor" -- which is vanilla's word for the raw points and carries vanilla's ~80% reading.
        // Asserted against the constant, not a copy of the string, so a reword of the item moves
        // both or fails here.
        assertTrue(EnchantEffectLine.bare(PROTECTION, 3, GearClass.ARMOR)
                        .contains(ArmorLoreLines.DEFENSE_LABEL.replace(": ", "")),
                "the enchant and the item must name the stat identically");
        assertFalse(EnchantEffectLine.bare(PROTECTION, 3, GearClass.ARMOR).contains("Armor"),
                "'Armor' is vanilla's word for raw points and would invite vanilla's mitigation");

        // "Max Health", never "Health". Growth raises the CEILING and grants no current health at
        // all -- equipping is headroom, never a heal -- so a line reading "+30 Health" would promise
        // exactly the heal HealthState refuses to give.
        String growth = EnchantEffectLine.bare(GROWTH, 3, GearClass.ARMOR);
        assertTrue(growth.contains("Max Health"), "the ceiling is what moves");
        assertFalse(growth.matches(".*[^x] Health.*"),
                "a bare 'Health' would promise current health the enchant never grants: " + growth);
        // Mutation: shorten either arm to "Health" -> reddens.
    }

    @Test
    void theMANAArmSaysPOINTSAndNamesTheCEILINGNotThePool() {
        // THE GAP THAT HAS NOW BITTEN TWICE, shipped with its arm this time. BLOCK_DR shipped with
        // no test in 2b; DEFENSE and MAX_HEALTH shipped the same way in 2a, with the lesson already
        // written three tests above them. Nothing else can reach here: ManaBankTest is core and this
        // is paper, and the golden renders DEFINITIONS while enchant state lives on instances, so no
        // shipped item carries an active Mana Bank for it to see.
        assertEquals(" (+10 Max Mana)", EnchantEffectLine.of(MANA_BANK, 1, GearClass.ARMOR));
        assertEquals(" (+20 Max Mana)", EnchantEffectLine.of(MANA_BANK, 2, GearClass.ARMOR));
        assertEquals(" (+30 Max Mana)", EnchantEffectLine.of(MANA_BANK, 3, GearClass.ARMOR));

        assertFalse(EnchantEffectLine.bare(MANA_BANK, 3, GearClass.ARMOR).contains("%"),
                "mana is points too -- nothing in this arm divides");

        // "Max Mana", never bare "Mana". The enchant raises the CEILING and grants no current mana:
        // ResourcePool pins the pre-change reading precisely so equipping is headroom, so a line
        // reading "+30 Mana" would promise the top-up that pin exists to withhold.
        String mana = EnchantEffectLine.bare(MANA_BANK, 3, GearClass.ARMOR);
        assertTrue(mana.contains("Max Mana"), "the ceiling is what moves");
        assertFalse(mana.matches(".*[^x] Mana.*"),
                "a bare 'Mana' would promise current mana the enchant never grants: " + mana);
        // Mutation: shorten it to "Mana", or restore a %% -> reddens.
    }

    @Test
    void theThreeArmorEnchantsDoNotDescribeThemselvesIdentically() {
        // Growth and Mana Bank share a curve ([10, 20, 30]) AND a sentence shape, so they are the
        // likeliest pair in the game to collide on a copy-paste -- the same risk BLOCK_DR and
        // REFLECT carry, and this is their equivalent guard. The pairwise loop below catches an
        // exact match; this says which pair to look at first.
        assertNotEquals(EnchantEffectLine.bare(GROWTH, 3, GearClass.ARMOR),
                EnchantEffectLine.bare(MANA_BANK, 3, GearClass.ARMOR),
                "two enchants with the same curve must still name different stats");
        assertTrue(EnchantEffectLine.bare(GROWTH, 3, GearClass.ARMOR).contains("Health"));
        assertTrue(EnchantEffectLine.bare(MANA_BANK, 3, GearClass.ARMOR).contains("Mana"));
        // Mutation: copy the MAX_HEALTH arm's body into MAX_MANA -> both read "+30 Max Health"
        // -> reddens here and in the pairwise loop.
    }

    @Test
    void bothArmorEnchantsAreInertOffArmorAndSayWhichGearTheyWanted() {
        // Reachable exactly as the shield pair is: /rpg enchant candidate puts any enchant on any
        // gear, and a hand-edited item can carry anything. The content boundary refuses these gates
        // in a FILE; it does not refuse them on an ITEM.
        assertEquals(" (inert: an Armor enchant on a Melee weapon)",
                EnchantEffectLine.of(PROTECTION, 3, GearClass.MELEE));
        assertEquals(" (inert: an Armor enchant on a shield)",
                EnchantEffectLine.of(GROWTH, 3, GearClass.SHIELD));
        // And the converse, which is the one that would have shipped wrong if GearClassLabel had
        // been given a lazy ARMOR arm: a shield enchant on armor must name armor properly.
        assertEquals(" (inert: a Shield enchant on a piece of armor)",
                EnchantEffectLine.of(BULWARK, 3, GearClass.ARMOR));
        // Mutation: make GearClassLabel.describe return "an Armor armor" for ARMOR -> reddens.
    }

    @Test
    void bothShieldEnchantsAreInertOnAWeaponAndSayWhichGearTheyWanted() {
        // Reachable: /rpg enchant candidate puts any enchant on any gear, and a hand-edited item can
        // carry anything. The content boundary refuses these gates in a FILE; it does not refuse
        // them on an item.
        assertEquals(" (inert: a Shield enchant on a Ranged weapon)",
                EnchantEffectLine.of(BULWARK, 3, GearClass.RANGER));
        assertEquals(" (inert: a Shield enchant on a Magic weapon)",
                EnchantEffectLine.of(THORNS, 3, GearClass.MAGE));
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
                BULWARK, THORNS, null}) {
            for (GearClass held : GearClass.values()) {
                assertEquals(" (" + EnchantEffectLine.bare(def, 3, held) + ")",
                        EnchantEffectLine.of(def, 3, held));
            }
        }
        // Mutation: change either form independently -> reddens.
    }
}
