package io.github.butterflysmp.rpg.paper.hud;

import io.github.butterflysmp.rpg.core.combat.AttackCharge;
import io.github.butterflysmp.rpg.core.combat.Crit;
import io.github.butterflysmp.rpg.core.combat.HealthRegen;
import io.github.butterflysmp.rpg.core.combat.HitDamage;
import io.github.butterflysmp.rpg.core.combat.ManaRegen;
import io.github.butterflysmp.rpg.core.combat.StatsSheetLines;
import io.github.butterflysmp.rpg.core.combat.StatsSheetValues;
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

    private static final double MANA_PER_TICK = 100.0 / (100 * 20);

    private static String plain(Component c) {
        return PlainTextComponentSerializer.plainText().serialize(c);
    }

    /** A bare-handed level-one player: base everything, no gear. */
    /**
     * A bare-handed level-one player: base everything, no gear, NO QUIVER WEAPON HELD.
     *
     * <p>Named through {@link StatsSheetValues.Builder} rather than passed positionally. The old form
     * was eight adjacent {@code double}s and this commit would have made it ten -- see
     * {@code StatsSheetValues} for why a record alone would not have fixed that.
     */
    private static List<Component> baseSheet() {
        return StatsSheet.build(StatsSheetValues.builder()
                .maxHealth(100)
                .healthRegenPerSecond(HealthRegen.BASE_PER_SECOND)
                .maxMana(100)
                .manaRegenPerSecond(ManaRegen.perSecond(MANA_PER_TICK))
                .defense(0)
                .damage(HitDamage.hitBase(0, 0, 0))
                .critChance(Crit.BASE_CHANCE)
                .critDamageBonus(Crit.BASE_DAMAGE)
                .build());
    }

    /** The same player holding {@code quiver_stone} with both A2 instruments: 28 rounds, 48 ticks. */
    private static List<Component> quiverSheet() {
        return StatsSheet.build(StatsSheetValues.builder()
                .maxHealth(100)
                .healthRegenPerSecond(HealthRegen.BASE_PER_SECOND)
                .maxMana(100)
                .manaRegenPerSecond(ManaRegen.perSecond(MANA_PER_TICK))
                .defense(0)
                .damage(HitDamage.hitBase(0, 0, 0))
                .critChance(Crit.BASE_CHANCE)
                .critDamageBonus(Crit.BASE_DAMAGE)
                .quiver(28, 48)
                .build());
    }

    @Test
    void theSheetIsAHeaderAndExactlyEIGHTStatLinesInOrder() {
        List<Component> sheet = baseSheet();
        assertEquals(9, sheet.size(), "a header and the eight stats");

        assertEquals("Your Stats", plain(sheet.get(0)));
        assertEquals("❤ Max Health   100", plain(sheet.get(1)));
        assertEquals("  Health Regen 1.00/5s", plain(sheet.get(2)));
        assertEquals("✦ Max Mana     100", plain(sheet.get(3)));
        assertEquals("  Mana Regen   5.00/5s", plain(sheet.get(4)));
        assertEquals("⛨ Defense      0", plain(sheet.get(5)));
        assertEquals("⚔ Damage       0.00", plain(sheet.get(6)));
        assertEquals("  Crit Chance  15%", plain(sheet.get(7)));
        assertEquals("  Crit Damage  2.00x", plain(sheet.get(8)));
        // Mutation: reorder any two lines, or drop one -> reddens by index.
        // Mutation: pass the mana rate per TICK instead of per second -> "0.25/5s" -> reddens.
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
        List<Component> sheet = StatsSheet.build(StatsSheetValues.builder()
                .maxHealth(100)
                .healthRegenPerSecond(HealthRegen.BASE_PER_SECOND)
                .maxMana(100)
                .manaRegenPerSecond(ManaRegen.perSecond(MANA_PER_TICK))
                .defense(0)
                .damage(HitDamage.hitBase(8, 15, 5))
                .critChance(Crit.BASE_CHANCE)
                .critDamageBonus(Crit.BASE_DAMAGE)
                .build());

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
        // TEN now, not eight, and the quiver pair is included ON PURPOSE: it is the newest pair and
        // therefore the likeliest to carry a copy-pasted label. Its two numbers collide with the rest
        // at 1, which is exactly the staging this row wants.
        List<Component> sheet = StatsSheet.build(StatsSheetValues.builder()
                .maxHealth(1).healthRegenPerSecond(1).maxMana(1).manaRegenPerSecond(1)
                .defense(1).damage(1).critChance(1).critDamageBonus(1)
                .quiver(1, 1)
                .build());
        Set<String> rendered = new HashSet<>();
        for (int i = 1; i < sheet.size(); i++) {
            assertTrue(rendered.add(plain(sheet.get(i))),
                    "two lines render identically: " + plain(sheet.get(i)));
        }
        assertEquals(10, rendered.size(), "eight always plus the quiver pair");
        // Mutation: reuse MAX_HEALTH_LABEL for max mana -> two identical renders -> reddens.
    }

    /** The value is the last child; the label is the one before it. */
    private static NamedTextColor valueColor(Component line) {
        List<Component> children = line.children();
        return (NamedTextColor) children.get(children.size() - 1).color();
    }

    /**
     * THE QUIVER PAIR IS ABSENT WITHOUT A QUIVER WEAPON, AND PRESENT WITH ONE.
     *
     * <p>Both halves asserted, because "absent" is the half that a default-zero would satisfy while
     * being wrong: a sheet reading {@code "Quiver 0"} tells a player holding a sword that their
     * magazine is empty. Every other line on this sheet is a fact about the player and needs no such
     * condition.
     *
     * <p>Forces red: a zero default instead of an absent one (the bare-handed row grows to ten);
     * dropping the {@code hasQuiver} branch (the holding row shrinks to eight).
     */
    @Test
    void theQuiverPairIsABSENTBareHandedAndPRESENTWhenAQuiverWeaponIsHeld() {
        assertEquals(9, baseSheet().size(), "header plus EIGHT -- no quiver lines invented");
        for (Component line : baseSheet()) {
            assertFalse(plain(line).contains(StatsSheetLines.QUIVER_SIZE_LABEL.trim()),
                    "nothing on a bare-handed sheet may mention a quiver");
        }

        List<Component> held = quiverSheet();
        assertEquals(11, held.size(), "header plus eight plus the pair");
        assertEquals("⚔ Quiver       28", plain(held.get(9)),
                "the RESOLVED capacity -- 9 authored plus the instrument's 19");
        assertEquals("  Reload       48t (2.40s)", plain(held.get(10)),
                "the RESOLVED duration in BOTH units, indented under the capacity like every "
                        + "second-of-pair line");
    }

    /**
     * THE RELOAD LINE SHOWS BOTH UNITS, AND CLAMPS A NEGATIVE FOR DISPLAY ONLY.
     *
     * <p>{@code ReloadTime.resolve} deliberately has no floor, so a large enough reduction resolves
     * below zero -- and {@code Quiver.reloadCompletesAt}'s {@code Math.max} is what actually governs,
     * making the real behaviour an instant reload. Printing {@code "-6t"} would show a number no
     * mechanic ever uses.
     *
     * <p>Forces red: dropping the display clamp; printing one unit instead of two.
     */
    @Test
    void aNegativeResolvedReloadRENDERSAsZeroBecauseThatIsWhatTheWeaponWillDo() {
        assertEquals("0t (0.00s)", StatsSheetLines.reloadTime(-6),
                "an instant reload, which is what reloadCompletesAt's Math.max produces");
        assertEquals("0t (0.00s)", StatsSheetLines.reloadTime(0));
        assertEquals("34t (1.70s)", StatsSheetLines.reloadTime(34), "quiver_stone's authored base");
        assertEquals("48t (2.40s)", StatsSheetLines.reloadTime(48), "and with the instrument");
    }
}
