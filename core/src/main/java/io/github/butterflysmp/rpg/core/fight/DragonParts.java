package io.github.butterflysmp.rpg.core.fight;

/**
 * F9: AN ENDER DRAGON'S PART, NAMED FROM ITS BOX. The API gives a part no name, and reading the server's own would mean
 * NMS (banned here), so a part is named by its box's width and height. READ from the pinned server jar,
 * {@code EnderDragon.<init>}: {@code head} 1 x 1, {@code neck} 3 x 3, {@code body} 5 x 3, {@code tail} 2 x 2 (three of
 * them), {@code wing} 4 x 2 (two). Every pair is distinct, so the box names the part.
 */
public final class DragonParts {

    private static final double TOLERANCE = 1.0e-3;

    private DragonParts() {}

    /** The part a box of this width and height is: head, neck, body, tail or wing; "part" for any other size. */
    public static String nameOf(double widthBlocks, double heightBlocks) {
        if (is(widthBlocks, heightBlocks, 1.0, 1.0)) return "head";
        if (is(widthBlocks, heightBlocks, 3.0, 3.0)) return "neck";
        if (is(widthBlocks, heightBlocks, 5.0, 3.0)) return "body";
        if (is(widthBlocks, heightBlocks, 2.0, 2.0)) return "tail";
        if (is(widthBlocks, heightBlocks, 4.0, 2.0)) return "wing";
        return "part";
    }

    private static boolean is(double w, double h, double width, double height) {
        return Math.abs(w - width) < TOLERANCE && Math.abs(h - height) < TOLERANCE;
    }
}
