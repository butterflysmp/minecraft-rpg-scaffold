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
 *
 * <p><b>Grown in dragon-mobs step R1</b> (PLAN-dragon-mobs.md) by three optional components, each absent by default
 * so every older file and call site means what it always meant:
 * <ul>
 *   <li>{@code body}: scale and a speed multiplier ({@link MobBody}); {@link MobBody#VANILLA} when unauthored.</li>
 *   <li>{@code element}: an existing {@code content/elements} id, or null. IDENTITY ONLY on day one: it types no
 *       damage, resists nothing, and refuses no status. The loader refuses an id that names no element.</li>
 *   <li>{@code stages}: the Phage's per-stage bodies ({@link MobStages}), or null. A staged mob's
 *       {@code maxHealth} is its stage-1 health, and its body lives in its stages, never in {@code body} too.</li>
 * </ul>
 * Gear (the plan's MB1 equipment) is DEFERRED past R1 and has no component here.
 */
public record MobDefinition(
        String id,
        String baseEntity,
        String displayName,
        double maxHealth,
        MobBody body,
        String element,
        MobStages stages
) {
    /**
     * The original four-field form: a vanilla body, no element, no stages. Kept so the existing call sites (tests
     * and older code) read unchanged. (A record may declare extra constructors; each must call the full one.)
     */
    public MobDefinition(String id, String baseEntity, String displayName, double maxHealth) {
        this(id, baseEntity, displayName, maxHealth, MobBody.VANILLA, null, null);
    }

    public MobDefinition {
        if (body == null) body = MobBody.VANILLA;
        if (element != null && element.isBlank()) {
            throw new IllegalArgumentException("mob '" + id + "' element must name an element, or be left out");
        }
        if (stages != null && !body.isVanilla()) {
            throw new IllegalArgumentException("mob '" + id + "' authors stages AND top-level scale/speed;"
                    + " a staged mob's body lives in its stages only");
        }
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
