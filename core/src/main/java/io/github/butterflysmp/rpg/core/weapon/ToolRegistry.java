package io.github.butterflysmp.rpg.core.weapon;

/**
 * Immutable-after-load lookup for every tool the server knows about.
 *
 * The body lives in {@link GearRegistry}; this names the kind and the noun its duplicate-id message
 * uses. A named subclass rather than {@code GearRegistry<ToolDefinition>} at every call site: it
 * keeps every existing signature unchanged and stops a caller declaring a registry of the wrong kind
 * wherever the type would otherwise be inferred.
 *
 * <p>The duplicate-id refusal earns more here than in the three siblings. One tool FILE holds many
 * definitions, so a copy-pasted entry whose {@code material:} was not changed is a realistic slip
 * inside a single file -- the same hazard {@link GearRegistry}'s javadoc records for an armor tier's
 * four pieces, arriving through a list instead of a map.
 */
public final class ToolRegistry extends GearRegistry<ToolDefinition> {
    public ToolRegistry() { super("tool"); }
}
