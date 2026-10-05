package io.github.butterflysmp.rpg.paper.selftest;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The one shape a component takes in a SELFTEST line: G1 and G3 are read off the colour names it prints. */
class SelfTestLinesTest {

    @Test
    void aGreenNameReadsGreenByItsVanillaName() {
        assertEquals("\"Build\" [green]", SelfTestLines.styled(Component.text("Build", NamedTextColor.GREEN)));
    }

    @Test
    void eachColourInAComposedNameIsListedInOrder() {
        Component name = Component.text("Class: ", NamedTextColor.GRAY).append(Component.text("Melee", NamedTextColor.GOLD));
        assertEquals("\"Class: Melee\" [gray, gold]", SelfTestLines.styled(name));
    }

    @Test
    void aColourWithNoVanillaNameIsItsHex() {
        assertEquals("\"x\" [#123456]", SelfTestLines.styled(Component.text("x", TextColor.color(0x123456))));
    }

    @Test
    void italicIsSaidOnlyWhenTheComponentSaysSo() {
        assertEquals("\"flavour\" [gray] italic", SelfTestLines.styled(
                Component.text("flavour", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, true)));
        assertEquals("\"plain\" []", SelfTestLines.styled(Component.text("plain")));
    }

    @Test
    void aRowLineCarriesTheGateAndTheRow() {
        assertEquals("SELFTEST level WD1 DONE ticks=180", SelfTestLines.row("level", "WD1", "DONE ticks=180"));
    }
}
