package io.github.yourname.rpg.core.element;

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
