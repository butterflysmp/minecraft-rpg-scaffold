package io.github.butterflysmp.rpg.core.mob;

/**
 * One custom mob: a content-defined identity that is DISTINCT from the vanilla entity it spawns as.
 *
 * The Knell is a wither skeleton with 360 HP; an ordinary wither skeleton is still an ordinary wither
 * skeleton. That separation is the whole point, and it is why a mob is keyed by its own {@code id} and
 * never by {@code baseEntity}: keying on entity type would scale every wither skeleton on the server.
 * The mechanism is the weapon one exactly -- a definition in content, a PDC tag on the specific
 * spawned entity, and a registry lookup by that tag. No tag, no change.
 *
 * {@code baseEntity} is a plain String, not a Bukkit EntityType, because {@code core} has no Bukkit
 * (CLAUDE.md invariant 1). Paper resolves it through {@code Registry.ENTITY_TYPE} and rejects one that
 * is not a living entity, so a typo is a named, skipped file at boot rather than a ClassCastException
 * the first time someone spawns it.
 *
 * Shaped to grow. {@code attackDamage} and the rest of the stat block become further components; only
 * {@code maxHealth} is wired this pass, and the custom health store is uncapped, so 5000 is the same
 * mechanism as 360 rather than a different one.
 */
public record MobDefinition(
        String id,
        String baseEntity,
        String displayName,
        double maxHealth
) {
    public MobDefinition {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("mob id required");
        // Required, never defaulted -- the same reasoning that made a weapon's class required. A
        // silent default here would spawn the wrong creature and look entirely intentional.
        if (baseEntity == null || baseEntity.isBlank()) {
            throw new IllegalArgumentException("mob '" + id + "' requires base_entity (the vanilla entity it spawns as)");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("mob '" + id + "' display_name required");
        }
        // 0 is a content bug, not a legal value: a mob that seeds at 0 custom HP is born dead (or
        // never dies, depending on which side of the reachedZero transition it lands on).
        if (maxHealth <= 0) {
            throw new IllegalArgumentException("mob '" + id + "' max_health must be > 0, was " + maxHealth);
        }
    }
}
