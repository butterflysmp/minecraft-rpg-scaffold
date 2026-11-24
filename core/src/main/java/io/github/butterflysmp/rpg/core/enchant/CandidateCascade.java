package io.github.butterflysmp.rpg.core.enchant;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleSupplier;

/**
 * HOW MANY CANDIDATES ONE ENCHANT SLOT OFFERS: a cascade, rarest first (the seat's ruling V3, 2026-11-23).
 *
 * <p>Ben, verbatim, 2026-11-23: <i>"I want to change the chances of multiple enchants: 1 enchant = 100%; 2 enchants =
 * 70%; 3 enchants = 40%; 4 enchants = 10%. I want this roll to be cascading like we did with the loot tables. (First it
 * rolls a 10% chance, if that misses it rolls the 40%, if that misses it rolls the 70% and so on)"</i>. It replaced the
 * even 1..3 split {@code EnchantRoll.candidateCount} made.
 *
 * <p><b>HIS NUMBERS ARE CONTENT, NOT CODE</b>: {@code content/enchant_candidates.yml}, read by paper's
 * {@code EnchantCandidatesLoader}, in the manner of {@code loot/cascade.yml}. Nothing here holds them. {@link #ONE} is
 * the fail-closed fallback when that file is refused, and it is deliberately not his.
 *
 * <p><b>The decision is split the way {@code EnchantRoll}'s always was</b>: {@link #lands} is one decision on one
 * already-drawn double, pinned at each boundary in isolation; {@link #count} only orchestrates. A draw is consumed only
 * for a tier actually rolled: the cascade stops at the first tier that lands, the guaranteed last tier takes no draw, and
 * an empty pool takes none.
 *
 * @param tiers rarest (most candidates) first, each chance in [0, 1], the last tier guaranteed (chance 1) and no tier
 *              above it guaranteed
 */
public record CandidateCascade(List<Tier> tiers) {

    /** One rung: offer {@code candidates} when a draw lands under {@code chance}. */
    public record Tier(int candidates, double chance) {}

    /** The fallback when the content file is refused: every slot offers ONE candidate. NOT Ben's numbers. */
    public static final CandidateCascade ONE = new CandidateCascade(List.of(new Tier(1, 1.0)));

    public CandidateCascade {
        if (tiers == null || tiers.isEmpty()) {
            throw new IllegalArgumentException("needs at least one tier (the last, guaranteed, one)");
        }
        tiers = List.copyOf(tiers);
        for (int i = 0; i < tiers.size(); i++) {
            Tier t = tiers.get(i);
            if (t.candidates() < 1 || t.candidates() > EnchantRoll.MAX_CANDIDATES) {
                throw new IllegalArgumentException("tier " + (i + 1) + " offers " + t.candidates()
                        + " candidates; it must be 1.." + EnchantRoll.MAX_CANDIDATES + " (what the table shows)");
            }
            // !(a <= b) rather than (a > b), so NaN is refused too.
            if (!(t.chance() >= 0.0 && t.chance() <= 1.0)) {
                throw new IllegalArgumentException("tier " + (i + 1) + " (" + t.candidates() + " candidates) has chance "
                        + t.chance() + "; a chance must be in [0, 1]");
            }
            if (i > 0 && t.candidates() >= tiers.get(i - 1).candidates()) {
                throw new IllegalArgumentException("tier " + (i + 1) + " offers " + t.candidates() + " after a tier offering "
                        + tiers.get(i - 1).candidates() + "; the tiers go rarest first, most candidates first");
            }
            boolean last = i == tiers.size() - 1;
            if (last && t.chance() != 1.0) {
                throw new IllegalArgumentException("the last tier (" + t.candidates() + " candidates) has chance "
                        + t.chance() + "; it must be guaranteed (1.0), or a slot could offer nothing");
            }
            if (!last && t.chance() == 1.0) {
                throw new IllegalArgumentException("tier " + (i + 1) + " (" + t.candidates() + " candidates) is guaranteed,"
                        + " so every tier after it is unreachable; only the last tier may be 1.0");
            }
        }
    }

    /** One decision: does this already-drawn double land this tier? {@code draw} is half-open [0, 1). */
    public static boolean lands(Tier tier, double draw) {
        return draw < tier.chance();
    }

    /**
     * The candidates one slot offers from a pool of {@code poolSize}: the first tier that lands, rarest first, capped at
     * the pool's size. A pool of 0 gives 0 and draws nothing; the guaranteed tier is taken without a draw.
     */
    public int count(int poolSize, DoubleSupplier draws) {
        if (poolSize <= 0) return 0;
        for (Tier tier : tiers) {
            if (tier.chance() >= 1.0 || lands(tier, draws.getAsDouble())) {
                return Math.min(tier.candidates(), poolSize);
            }
        }
        // Unreachable: the constructor makes the last tier guaranteed. Total anyway, toward fewer.
        return Math.min(tiers.getLast().candidates(), poolSize);
    }

    /** Each tier's TRUE chance, in order: its own chance times every miss above it. They sum to 1. */
    public List<Double> trueOdds() {
        List<Double> out = new ArrayList<>(tiers.size());
        double reach = 1.0;
        for (Tier tier : tiers) {
            out.add(reach * tier.chance());
            reach *= 1.0 - tier.chance();
        }
        return List.copyOf(out);
    }
}
