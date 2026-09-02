package io.github.butterflysmp.rpg.core.weapon;

import java.util.Locale;
import java.util.Optional;

/**
 * The one place a {@code craft_result} token is normalised, and the one place a claim is validated.
 *
 * <p>Its own class because FOUR things need the same answer and would otherwise each grow their own:
 * the three gear records validating what content authored, and {@link CraftResultIndex} normalising
 * both the keys it stores and the material names it is later asked about. A lookup that normalised
 * differently from the build would miss every entry, and the symptom would be "crafting mints
 * nothing" with no error anywhere -- which is exactly the failure this arc keeps writing guards
 * against.
 *
 * <p><b>Normalisation is deliberately narrow: lower-case, and strip a leading {@code minecraft:}.</b>
 * Content may reasonably write {@code IRON_SWORD}, {@code iron_sword} or {@code minecraft:iron_sword}
 * -- Bukkit's own {@code Material.matchMaterial} accepts all three -- and the boot check that
 * compares a claim against its material must not fail on a spelling difference. Nothing else is
 * touched: this does not correct typos, and an unresolvable token stays unresolvable so the boot can
 * name it.
 */
public final class CraftResultToken {

    private CraftResultToken() {}

    /** The namespace Bukkit itself omits, and the only one a vanilla material can carry. */
    private static final String VANILLA_NAMESPACE = "minecraft:";

    /**
     * Reduce a material name to the form the index keys on.
     *
     * <p>Applied to BOTH sides -- the authored claim at load time and the crafted material at
     * lookup time -- so the two cannot disagree about spelling.
     *
     * @param raw a material name, however content or Bukkit spelled it. Null yields null.
     */
    public static String token(String raw) {
        if (raw == null) return null;
        String lower = raw.trim().toLowerCase(Locale.ROOT);
        return lower.startsWith(VANILLA_NAMESPACE) ? lower.substring(VANILLA_NAMESPACE.length()) : lower;
    }

    /**
     * Validate and normalise a record's authored claim.
     *
     * <p>Absent is the norm: most gear does not participate in mint-on-craft, and
     * {@code Optional.empty()} is what a file with no {@code craft_result:} key produces. A null
     * Optional is also treated as absent, so a direct caller in a test cannot trip on it.
     *
     * <p>PRESENT BUT BLANK THROWS, rather than being quietly treated as absent. A file that writes
     * {@code craft_result:} with nothing after it is stating an intention it failed to finish, and
     * silently reading that as "does not participate" would leave the author looking for a mint that
     * was never going to happen. The loader turns this into a named, skipped file.
     *
     * @param noun the kind, for the message ("weapon", "shield", "armor").
     * @param id   the definition's id, so the message names the file to open.
     */
    public static Optional<String> normalise(Optional<String> claim, String noun, String id) {
        if (claim == null || claim.isEmpty()) return Optional.empty();

        String value = claim.get();
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(noun + " '" + id
                    + "' has a blank craft_result; remove the key or name a material");
        }
        return Optional.of(token(value));
    }
}
