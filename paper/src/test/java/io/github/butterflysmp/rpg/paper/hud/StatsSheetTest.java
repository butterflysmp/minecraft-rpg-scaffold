package io.github.butterflysmp.rpg.paper.hud;

import io.github.butterflysmp.rpg.core.combat.AttackCharge;
import io.github.butterflysmp.rpg.core.combat.Crit;
import io.github.butterflysmp.rpg.core.combat.HealthRegen;
import io.github.butterflysmp.rpg.core.combat.HitDamage;
import io.github.butterflysmp.rpg.core.combat.ManaRegen;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The rendered stat sheet: its eight lines, their order, and their colours.
 *
 * <p>The command around it is Bukkit-facing and boot-verified; the ASSEMBLY is not, which is why it
 * lives in {@link StatsSheet} rather than as a private method inside {@code RpgCommand}. Same move as
 * {@code ApplyArgs} and {@code EnchantEffectLine}, and for the same reason: a renderer buried in a
 * command class is untestable by construction.
 *
 * <p>Colours are asserted against the {@link StatsBarText} constants THEMSELVES, never against a
 * literal, so a changed HUD colour moves the sheet with it instead of silently disagreeing.
 *
 * <p><b>Every expected string was produced by EXECUTING the expression.</b> Each test names the
 * mutation it forces red.
 */
class StatsSheetTest {

    private static final double MANA_PER_TICK = 100.0 / (60 * 20);

    private static String plain(Component c) {
        return PlainTextComponentSerializer.plainText().serialize(c);
    }

    /** A bare-handed level-one player: base everything, no gear. */
    private static List<Component> baseSheet() {
        return StatsSheet.build(100, HealthRegen.BASE_PER_SECOND,
                100, ManaRegen.perSecond(MANA_PER_TICK),
                0, HitDamage.hitBase(0, 0, 0),
                Crit.BASE_CHANCE, Crit.BASE_DAMAGE);
    }

    @Test
    void theSheetIsAHeaderAndExactlyEIGHTStatLinesInOrder() {
        List<Component> sheet = baseSheet();
        assertEquals(9, sheet.size(), "a header and the eight stats");

        assertEquals("Your Stats", plain(sheet.get(0)));
        assertEquals("❤ Max Health   100", plain(sheet.get(1)));
        assertEquals("  Health Regen 0.20/s", plain(sheet.get(2)));
        assertEquals("✦ Max Mana     100", plain(sheet.get(3)));
        assertEquals("  Mana Regen   1.67/s", plain(sheet.get(4)));
        assertEquals("⛨ Defense      0", plain(sheet.get(5)));
        assertEquals("⚔ Damage       0.00", plain(sheet.get(6)));
        assertEquals("  Crit Chance  15%", plain(sheet.get(7)));
        assertEquals("  Crit Damage  2.00x", plain(sheet.get(8)));
        // Mutation: reorder any two lines, or drop one -> reddens by index.
        // Mutation: pass the mana rate per TICK instead of per second -> "0.08/s" -> reddens.
    }

    @Test
    void aRegenLineWearsItsPARENTStatsColourWhichIsWhatMakesThePairingREAD() {
        List<Component> sheet = baseSheet();

        assertEquals(StatsBarText.HEALTH_COLOR, valueColor(sheet.get(1)), "max health is red");
        assertEquals(StatsBarText.HEALTH_COLOR, valueColor(sheet.get(2)),
                "and health regen is red too -- the pair reads as one group without a separator");
        assertEquals(StatsBarText.MANA_COLOR, valueColor(sheet.get(3)), "max mana is blue");
        assertEquals(StatsBarText.MANA_COLOR, valueColor(sheet.get(4)), "and so is mana regen");

        assertNotEquals(StatsBarText.MANA_COLOR, valueColor(sheet.get(2)),
                "health regen must NOT wear mana's colour -- the two regen lines are the likeliest "
                        + "copy-paste in the file");
        // Mutation: give health regen MANA_COLOR -> the last row reddens.
    }

    @Test
    void theSharedStatsWearTheHUDsOWNColoursSoTheSheetAndTheBarCannotDISAGREE() {
        List<Component> sheet = baseSheet();
        assertEquals(StatsBarText.HEALTH_COLOR, valueColor(sheet.get(1)));
        assertEquals(StatsBarText.MANA_COLOR, valueColor(sheet.get(3)));
        assertEquals(StatsBarText.DEFENSE_COLOR, valueColor(sheet.get(5)));
        assertEquals(StatsBarText.DAMAGE_COLOR, valueColor(sheet.get(6)));
        assertEquals(StatsBarText.CRIT_COLOR, valueColor(sheet.get(7)));
        assertEquals(StatsBarText.CRIT_COLOR, valueColor(sheet.get(8)),
                "the crit pair shares one colour, like the regen pairs share their parent's");

        // The icons are the HUD's constants too, not copies of the glyphs.
        assertTrue(plain(sheet.get(1)).startsWith(StatsBarText.HEART));
        assertTrue(plain(sheet.get(3)).startsWith(StatsBarText.SPARK));
        assertTrue(plain(sheet.get(5)).startsWith(StatsBarText.SHIELD));
        assertTrue(plain(sheet.get(6)).startsWith(StatsBarText.SWORDS));
        // Mutation: hardcode NamedTextColor.RED instead of importing HEALTH_COLOR -> passes today and
        // silently diverges the day the HUD colour changes, which is why these assert the CONSTANT.
        // Mutation: spell the heart glyph inline -> the startsWith rows still pass, but the constant
        // is the compile-time link; see StatsBarText's javadoc.
    }

    @Test
    void theLabelIsGRAYAndTheVALUEWearsTheColourJustAsAnItemTooltipDoes() {
        Component health = baseSheet().get(1);
        assertEquals(2, health.children().size(), "an icon lead, then the label and the value");
        assertEquals(NamedTextColor.GRAY, health.children().get(0).color(), "the label is gray");
        assertEquals(StatsBarText.HEALTH_COLOR, health.children().get(1).color(),
                "and the value carries the stat's colour -- GearLore.appendFlatBonus' arrangement, "
                        + "so a stat reads the same way here and on an item");
        // Mutation: colour the label and gray the value -> both rows redden.
    }

    @Test
    void theDamageLineIsTheCOMPOSEDHitAndNotTheRawAttackValue() {
        // 8-damage weapon, Sharpness III (+15%), +5 class gear -- the composition's own witness.
        List<Component> sheet = StatsSheet.build(100, HealthRegen.BASE_PER_SECOND,
                100, ManaRegen.perSecond(MANA_PER_TICK), 0,
                HitDamage.hitBase(8, 15, 5), Crit.BASE_CHANCE, Crit.BASE_DAMAGE);

        assertEquals("⚔ Damage       14.20", plain(sheet.get(6)),
                "8 * 1.15 + 5 = 14.2, the same number a full-charge non-crit swing deals");
        assertFalse(plain(sheet.get(6)).contains("8.00"), "the raw attack value is not what is shown");

        // And it IS the swing: dealt(hitBase, 1, 1) is an exact identity, so no separate claim needed.
        assertEquals(HitDamage.hitBase(8, 15, 5),
                HitDamage.dealt(HitDamage.hitBase(8, 15, 5), AttackCharge.FULL_CHARGE, Crit.NO_CRIT),
                0.0, "the sheet shows a real full-charge non-crit hit, bit for bit");
        // Mutation: pass stats.attackValue directly instead of HitDamage.hitBase -> "8.00" -> reddens.
    }

    @Test
    void noTwoLinesRENDERIdenticallyEvenWhenTheirNUMBERSCollide() {
        // Eight near-identical lines is where a copy-pasted label or colour hides. Force the numbers
        // to collide so ONLY the labels and colours can tell the lines apart.
        List<Component> sheet = StatsSheet.build(1, 1, 1, 1, 1, 1, 1, 1);
        Set<String> rendered = new HashSet<>();
        for (int i = 1; i < sheet.size(); i++) {
            assertTrue(rendered.add(plain(sheet.get(i))),
                    "two lines render identically: " + plain(sheet.get(i)));
        }
        assertEquals(8, rendered.size());
        // Mutation: reuse MAX_HEALTH_LABEL for max mana -> two identical renders -> reddens.
    }

    /** The value is the last child; the label is the one before it. */
    private static NamedTextColor valueColor(Component line) {
        List<Component> children = line.children();
        return (NamedTextColor) children.get(children.size() - 1).color();
    }
}
