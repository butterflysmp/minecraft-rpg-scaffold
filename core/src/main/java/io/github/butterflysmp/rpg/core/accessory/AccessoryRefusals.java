package io.github.butterflysmp.rpg.core.accessory;

/**
 * The words every station and command uses to refuse an accessory -- ruling A4: v1 accessories take
 * no part in enchanting, the anvil, the grindstone or gear score, and each refuses them EXPLICITLY.
 *
 * <p>One place for the text, because the tests assert it. Most of these refusals are shadowed by an
 * older one that would also refuse the item, for a different reason and in different words: every
 * enchant gate is an allowlist, the stations' "is this ours" check does not know accessories, and so
 * on. A test that asserted only "refused" would stay green with the explicit arm deleted. <b>Asserting
 * THIS text is the only reading the deletion can redden.</b>
 */
public final class AccessoryRefusals {

    private AccessoryRefusals() {}

    /** The enchant table and {@code /rpg enchant}. */
    public static final String ENCHANT = "Accessories cannot be enchanted.";

    /** The anvil. */
    public static final String ANVIL = "Accessories cannot be worked at the anvil.";

    /** The grindstone. */
    public static final String GRINDSTONE = "Accessories have nothing to grind off.";

    /** {@code /rpg gearscore}. */
    public static final String GEAR_SCORE = "Accessories carry no gear score.";

    /** {@code /rpg durability}. */
    public static final String DURABILITY = "Accessories do not wear out.";

    /** An enchant file whose {@code class:} names accessories -- a boot-log line, so it names the rule. */
    public static final String ENCHANT_CLASS = "class: accessory is refused -- accessories take no part"
            + " in enchanting (ruling A4)";
}
