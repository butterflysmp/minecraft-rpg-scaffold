package io.github.butterflysmp.rpg.paper.selftest;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/**
 * The text of every SELFTEST line, so its shape is one place and a unit row.
 *
 * <p><b>THE INSTRUMENT NEVER JUDGES.</b> Nothing here, or anywhere in this package, prints PASS, FAIL or an
 * expected value (the seat's ruling d). A line says what was driven and what was read; the reading is judged by a
 * person, against the committed gate, from the real witness lines (PLAYERHIT, DOTTICK, MOBSEED) that sit between a
 * row's START and DONE. {@code SelfTestNeverJudgesTest} scans this package's compiled constants for the two words.
 */
public final class SelfTestLines {

    private SelfTestLines() {}

    public static String row(String gate, String row, String text) {
        return "SELFTEST " + gate + " " + row + " " + text;
    }

    /**
     * A component as a reader needs it: its plain text in quotes, then each colour it uses, in order, by its vanilla
     * NAME where it is one ({@code green}, {@code dark_gray}) and as {@code #rrggbb} otherwise; {@code italic} when the
     * component says so explicitly. {@code "Class: Melee" [gray, gold]}. Uncoloured text adds nothing to the list.
     */
    public static String styled(Component component) {
        if (component == null) return "<none>";
        List<String> colours = new ArrayList<>();
        boolean[] italic = {false};
        collect(component, colours, italic);
        String plain = PlainTextComponentSerializer.plainText().serialize(component);
        return "\"" + plain + "\" " + colours + (italic[0] ? " italic" : "");
    }

    private static void collect(Component c, List<String> colours, boolean[] italic) {
        TextColor colour = c.color();
        if (colour != null) {
            NamedTextColor named = NamedTextColor.namedColor(colour.value());
            String name = named != null ? named.toString() : colour.asHexString();
            if (colours.isEmpty() || !colours.getLast().equals(name)) colours.add(name);
        }
        if (c.decoration(TextDecoration.ITALIC) == TextDecoration.State.TRUE) italic[0] = true;
        for (Component child : c.children()) collect(child, colours, italic);
    }
}
