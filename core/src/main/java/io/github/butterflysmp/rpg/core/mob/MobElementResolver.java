package io.github.butterflysmp.rpg.core.mob;

import java.util.Map;
import java.util.Optional;

/**
 * "The element of this creature" (PLAN-dragon-mobs.md, ruling 2 and step R1). One answer, in this order:
 * <ol>
 *   <li>our mob's file, if the creature carries a {@code mob_id} that names a loaded mob AND that file authors an
 *       {@code element};</li>
 *   <li>else the vanilla table ({@code content/mob_elements.yml}) by the creature's bare entity-type key;</li>
 *   <li>else NONE.</li>
 * </ol>
 *
 * <p><b>A file that authors no element falls through to the table</b> rather than erasing it. No shipped mob reaches
 * the difference today (the only element-less file is the spider mount, and {@code spider} is not in the table); it
 * is a choice, recorded in PLAN-dragon-mobs.md for the seat, and pinned by a row in {@code MobElementResolverTest}.
 *
 * <p><b>Day one this is IDENTITY ONLY.</b> Nothing types a mob's damage, resists anything or refuses a status by its
 * element. Paper supplies only the mob id and the type key, so this class never sees Bukkit (invariant 1).
 */
public final class MobElementResolver {

    private final MobRegistry mobs;
    private final Map<String, String> byType;

    public MobElementResolver(MobRegistry mobs, Map<String, String> byType) {
        this.mobs = mobs;
        this.byType = Map.copyOf(byType);
    }

    /**
     * @param mobId   the creature's {@code mob_id} tag, or null for a vanilla mob
     * @param typeKey the creature's bare entity-type key ({@code enderman}, not {@code minecraft:enderman})
     */
    public Optional<String> elementOf(String mobId, String typeKey) {
        if (mobId != null) {
            Optional<String> authored = mobs.find(mobId).map(MobDefinition::element);
            if (authored.isPresent()) return authored;
        }
        return Optional.ofNullable(typeKey == null ? null : byType.get(typeKey));
    }

    /** How many vanilla types the table names, for the boot's "Loaded ..." line. */
    public int tableSize() {
        return byType.size();
    }
}
