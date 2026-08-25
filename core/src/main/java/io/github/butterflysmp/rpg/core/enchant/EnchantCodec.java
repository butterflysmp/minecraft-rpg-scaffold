package io.github.butterflysmp.rpg.core.enchant;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The wire form of an {@link EnchantState}: one versioned string, for one PDC key.
 *
 * <pre>
 *   state := "v1" { ";" slot }
 *   slot  := [ cand { "," cand } ] ":" activeIndex
 *   cand  := enchantId "=" level
 * </pre>
 *
 * Worked: {@code empty()} is {@code "v1"}; a real item is
 * {@code "v1;unbreaking=3,sharpness=1:0;unbreaking=2:-1"}; a slot that offers nothing is
 * {@code "v1;:-1"}.
 *
 * <p><b>This is the repo's first string codec</b>, and the departure from the house convention (one
 * typed PDC key per scalar, assembled in paper) is deliberate. The arity here is genuinely variable
 * -- slots times candidates, and BOTH counts are decisions the roster pass has not made -- so a
 * per-scalar scheme means either a fixed grid of keys written whether used or not, or a key count
 * that changes with the data. Neither can be versioned atomically, and versioning is the whole
 * point: this string outlives the build that wrote it.
 *
 * <p>Three departures from the old repo's {@code EnchantData}, each buying something:
 * <ol>
 *   <li>the level rides the CANDIDATE ({@code id=level}), not the choice. That difference IS the
 *       richer model -- the old form could only remember one level per slot, which is what made a
 *       swap forget the other candidate's progress;</li>
 *   <li>the active choice is an INDEX, not a repeated id, so it cannot name something the candidate
 *       list does not offer;</li>
 *   <li>{@code slotCount} is dropped. The old parser needed it to pad; this one trusts the
 *       segments, so there is no second source of truth to disagree with the first.</li>
 * </ol>
 *
 * <p><b>{@link #decode} is TOTAL: no input, however malformed or adversarial, throws.</b> It
 * repairs, clamps and drops so it can only ever hand {@link EnchantState}'s constructors legal
 * input. That matters because this string arrives off an item that may have been written by a
 * different build, and an exception here would surface as a failed tooltip render or a failed join,
 * not as a diagnosable error. Every repair fails toward "less enchanted", never toward more.
 *
 * <p>Note what decode does NOT do: resolve ids. It has no idea which enchants exist. An id the
 * registry no longer knows survives decoding and is dealt with by the two readers, each of which
 * has a reason to prefer showing it to hiding it.
 */
public final class EnchantCodec {

    private EnchantCodec() {}

    /**
     * The grammar's version tag. An unrecognised one decodes to empty rather than being guessed at,
     * which is only safe because {@code WeaponItems.carryEnchants} moves the RAW string across a
     * re-mint without ever decoding it -- so a v2 blob read by a v1 build renders as unenchanted
     * and is handed back intact, rather than being silently rewritten into v1 and losing whatever
     * v2 added.
     */
    static final String VERSION = "v1";

    // The four delimiters. EnchantCandidate refuses an id containing any of them, which is what
    // makes encode injective.
    //
    // They are split with Pattern.quote because String.split takes a REGEX, not a literal. ';' and
    // ',' happen to be regex-safe, so today this is belt and braces -- but a delimiter changed to a
    // metacharacter ('|' being the obvious one, and alternation of two empty branches splits
    // between every character) would silently stop parsing every blob already on every item, while
    // encode carried on writing them. Quoting means the grammar is whatever these constants say,
    // not whatever the regex engine reads them as.
    private static final char SLOT = ';';
    private static final char CANDIDATE = ',';
    private static final char LEVEL = '=';
    private static final char ACTIVE = ':';

    /** The exact grammar above. Pinned by a literal-form test, not merely by a round trip. */
    public static String encode(EnchantState state) {
        StringBuilder out = new StringBuilder(VERSION);
        for (EnchantSlot slot : state.slots()) {
            out.append(SLOT);
            List<EnchantCandidate> candidates = slot.candidates();
            for (int i = 0; i < candidates.size(); i++) {
                if (i > 0) out.append(CANDIDATE);
                out.append(candidates.get(i).enchantId()).append(LEVEL).append(candidates.get(i).level());
            }
            out.append(ACTIVE).append(slot.activeIndex());
        }
        return out.toString();
    }

    /**
     * Parse a blob. Never throws; never returns null.
     *
     * A malformed SLOT degrades to an empty slot and the other slots survive, rather than the whole
     * state being discarded -- one corrupt segment must not cost a player the unlocks in the slots
     * either side of it.
     */
    public static EnchantState decode(String raw) {
        if (raw == null || raw.isBlank()) return EnchantState.empty();

        String[] parts = raw.split(Pattern.quote(String.valueOf(SLOT)), -1);
        if (!VERSION.equals(parts[0])) return EnchantState.empty();

        List<EnchantSlot> slots = new ArrayList<>(parts.length - 1);
        for (int i = 1; i < parts.length; i++) slots.add(decodeSlot(parts[i]));
        return new EnchantState(slots);
    }

    private static EnchantSlot decodeSlot(String raw) {
        int split = raw.indexOf(ACTIVE);
        // No active-index separator at all: not a slot this grammar can read. An empty slot loses
        // nothing that was legible, and keeps the slot's POSITION so the ones after it stay put.
        if (split < 0) return EnchantSlot.empty();

        List<EnchantCandidate> candidates = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        String candidatePart = raw.substring(0, split);
        if (!candidatePart.isEmpty()) {
            for (String token : candidatePart.split(Pattern.quote(String.valueOf(CANDIDATE)), -1)) {
                EnchantCandidate candidate = decodeCandidate(token);
                // EnchantSlot forbids a duplicate id, so the codec REPAIRS one rather than handing
                // it over and turning a corrupt blob into a thrown exception. First wins.
                if (candidate != null && seen.add(candidate.enchantId())) candidates.add(candidate);
            }
        }

        int active = parseOr(raw.substring(split + 1), EnchantSlot.NONE);
        // Fail to "nothing active", never to a lie: an index past the list, or one pointing at a
        // candidate this build reads as locked, must not become an active enchant.
        if (active < 0 || active >= candidates.size() || candidates.get(active).isLocked()) {
            active = EnchantSlot.NONE;
        }
        return new EnchantSlot(candidates, active);
    }

    private static EnchantCandidate decodeCandidate(String token) {
        int split = token.indexOf(LEVEL);
        String id = split < 0 ? token : token.substring(0, split);
        if (id.isBlank()) return null;
        // Cannot happen given what we split on, and checked anyway: EnchantCandidate THROWS on a
        // delimiter, and decode's totality is a promise that outranks this branch being dead.
        for (int i = 0; i < id.length(); i++) {
            if (EnchantCandidate.DELIMITERS.indexOf(id.charAt(i)) >= 0) return null;
        }

        // A missing or unreadable level is LOCKED, not level 1: an unparseable blob must never
        // grant an unlock the player did not earn.
        int level = split < 0 ? 0 : parseOr(token.substring(split + 1), 0);
        // Clamped rather than rejected -- this is the "a later build wrote level 5" case, and
        // dropping the candidate would lose the id as well as the excess.
        level = Math.min(Math.max(level, 0), EnchantState.MAX_LEVEL);
        return new EnchantCandidate(id, level);
    }

    /** {@code Integer.parseInt} that answers rather than throwing. Also catches an overflowing value. */
    private static int parseOr(String raw, int fallback) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
