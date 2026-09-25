package io.github.butterflysmp.rpg.core.weapon;

/**
 * Immutable-after-load lookup for every accessory the server knows about. The body lives in
 * {@link GearRegistry}; this names the kind, as {@code ShieldRegistry} does.
 */
public final class AccessoryRegistry extends GearRegistry<AccessoryDefinition> {
    public AccessoryRegistry() { super("accessory"); }
}
