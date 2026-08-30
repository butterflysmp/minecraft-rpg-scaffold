package io.github.butterflysmp.rpg.core.weapon;

/**
 * Immutable-after-load lookup for every shield the server knows about.
 *
 * The body lives in {@link GearRegistry}; this names the kind and the noun its duplicate-id message
 * uses. A named subclass rather than {@code GearRegistry<ShieldDefinition>} at every call site: it
 * keeps ~30 existing signatures unchanged and stops a caller declaring a registry of the wrong kind
 * wherever the type would otherwise be inferred.
 */
public final class ShieldRegistry extends GearRegistry<ShieldDefinition> {
    public ShieldRegistry() { super("shield"); }
}
