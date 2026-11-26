package io.github.butterflysmp.rpg.core.weapon;

import io.github.butterflysmp.rpg.core.ability.CastSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TOKENS IN AN AUTHORED ABILITY DESCRIPTION (the seat's D1, 2026-11-26), filled from the EFFECTIVE cast (T1) when the
 * tooltip is built, so a sentence that states a number an enchant changes reads true with the enchant active.
 *
 * <ul>
 *   <li>{@code {shots}}: the cast's shot count as a numeral ("6").</li>
 *   <li>{@code {shots_word}}: the same number as a lower-case word, one to twelve, a numeral above ("six").</li>
 * </ul>
 *
 * <p>A cast HAS a shot count when it declares one: a {@link CastSpec.Volley} (its shots, times its inner cast's own
 * count) or a {@link CastSpec.Projectile} with a spread. The loaders refuse an unknown token, and a shot token on a cast
 * with no count, by name ({@link #validate}). Not tokenised in this slice, by D3: wind-up, range, reload, cooldown.
 */
public final class DescriptionTokens {

    private DescriptionTokens() {}

    public static final String SHOTS = "shots";
    public static final String SHOTS_WORD = "shots_word";

    private static final Pattern TOKEN = Pattern.compile("\\{([a-z_]+)}");
    private static final String[] WORDS = {"zero", "one", "two", "three", "four", "five", "six", "seven", "eight",
            "nine", "ten", "eleven", "twelve"};

    /** The number as a lower-case word, one to twelve; a numeral otherwise. */
    public static String word(int n) {
        return n >= 1 && n < WORDS.length ? WORDS[n] : String.valueOf(n);
    }

    /** Does this cast declare a shot count a {@code {shots}} token can read? */
    public static boolean hasShotCount(CastSpec cast) {
        return switch (cast) {
            case CastSpec.Volley ignored -> true;
            case CastSpec.Projectile p -> p.spread() != null;
            default -> false;
        };
    }

    /**
     * Refuse a description this cast cannot fill. Returns normally for one with no tokens.
     *
     * @throws IllegalArgumentException naming the token and why, for the caller to prefix with the file
     */
    public static void validate(List<String> description, CastSpec cast) {
        for (String line : description) {
            Matcher m = TOKEN.matcher(line);
            while (m.find()) {
                String token = m.group(1);
                if (!token.equals(SHOTS) && !token.equals(SHOTS_WORD)) {
                    throw new IllegalArgumentException("description token {" + token + "} is unknown; the tokens are {"
                            + SHOTS + "} and {" + SHOTS_WORD + "}");
                }
                if (cast == null || !hasShotCount(cast)) {
                    throw new IllegalArgumentException("description token {" + token + "} needs a cast with a shot count"
                            + " (a volley, or a projectile with a spread), and this cast has none");
                }
            }
        }
    }

    /** The description with every token filled from {@code cast}; a line with no token is returned as it is. */
    public static List<String> fill(List<String> description, CastSpec cast) {
        if (description.stream().noneMatch(l -> l.indexOf('{') >= 0)) return description;
        int shots = WeaponLoreLines.shotCount(cast);
        List<String> out = new ArrayList<>(description.size());
        for (String line : description) {
            out.add(line.replace("{" + SHOTS_WORD + "}", word(shots)).replace("{" + SHOTS + "}", String.valueOf(shots)));
        }
        return out;
    }
}
