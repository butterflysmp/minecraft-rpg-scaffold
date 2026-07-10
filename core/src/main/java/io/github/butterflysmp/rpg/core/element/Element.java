package io.github.butterflysmp.rpg.core.element;

/**
 * Damage elements. Kept as an enum because the *set* is a design decision,
 * not content. Individual abilities are data; the elemental system is not.
 */
public enum Element {
    KINETIC,
    SOLAR,
    ARC,
    VOID,
    STASIS;

    /** values() clones its array on every call, and fromName sits on the damage path. */
    private static final Element[] VALUES = values();

    /**
     * The element with this name, or null if no element has it.
     *
     * Case-insensitive. The two places an element is named -- ability YAML and an
     * entity's persistent data -- are both hand-authored, and neither should care
     * about shouting.
     *
     * Null-returning rather than throwing, because one of those two places is read
     * on the damage path, where an exception would abort the rest of a detonation.
     * Callers that WANT a throw (content loading, where failing the file is right)
     * can raise their own with a better message than valueOf's.
     */
    public static Element fromName(String name) {
        if (name == null) return null;
        for (Element element : VALUES) {
            if (element.name().equalsIgnoreCase(name)) return element;
        }
        return null;
    }

    /**
     * Elemental effectiveness multiplier: 1.5x when the attacking element
     * matches the defender's shield, 1.0x otherwise. An unshielded defender
     * takes 1.0x.
     *
     * This is a placeholder rule, not a placeholder identity -- it already
     * changes damage. Replace it with your real triangle. Lives here (not in
     * config) so it is unit-testable and cannot be broken by a typo in YAML.
     */
    public double multiplierAgainst(Element defenderShield) {
        if (defenderShield == null) return 1.0;
        return this == defenderShield ? 1.5 : 1.0;
    }
}
