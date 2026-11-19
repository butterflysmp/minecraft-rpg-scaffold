package io.github.butterflysmp.rpg.core.loot;

import io.github.butterflysmp.rpg.core.weapon.Rarity;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.DoubleSupplier;

/**
 * A CASCADING LOOT TABLE (PLAN-loot.md; Ben, 2026-11-19: "First the rarest rarity is rolled. then if that doesn't land,
 * the next rarest rarity is rolled and so on"). Pure: no Bukkit, and the random source is passed in, never a static.
 * <ul>
 *   <li>The tiers run from RAREST to COMMONEST ({@link Rarity}'s order, which is its declaration order), each a rarity
 *       at most once and a chance in [0, 1].</li>
 *   <li>A roll draws once per tier, in order. A tier LANDS when its draw is below its chance, so a 0 chance never lands
 *       and a 1.0 chance always does. The first to land is the rarity; if none lands, the result is nothing.</li>
 *   <li>SEAT READING, flagged: only the LAST tier may be GUARANTEED (chance 1.0), which a "one guaranteed item" source
 *       needs. A guaranteed tier above another would leave the ones below it unreachable, so it is refused.</li>
 * </ul>
 */
public final class CascadeTable {

    /** One tier: a rarity, and the chance it lands when it is rolled. */
    public record Tier(Rarity rarity, double chance) {}

    /**
     * THE TRUE ODDS: the probability a roll comes to each tier, in tier order, and to nothing.
     *
     * @param byTier  each tier's rarity and its real probability: its chance times the product of the misses above it
     * @param nothing the product of every miss
     */
    public record Odds(Map<Rarity, Double> byTier, double nothing) {
        /** A tier's real probability; 0 for a rarity not in the table. */
        public double of(Rarity rarity) {
            return byTier.getOrDefault(rarity, 0.0);
        }
    }

    private final List<Tier> tiers;

    public CascadeTable(List<Tier> tiers) {
        if (tiers == null || tiers.isEmpty()) throw new IllegalArgumentException("a cascade needs at least one tier");
        for (int i = 0; i < tiers.size(); i++) {
            Tier t = tiers.get(i);
            if (t.rarity() == null) throw new IllegalArgumentException("tier " + (i + 1) + " has no rarity");
            double c = t.chance();
            if (!(c >= 0.0 && c <= 1.0)) {
                throw new IllegalArgumentException("tier " + (i + 1) + " (" + name(t.rarity()) + ") has chance " + c
                        + ", outside [0, 1]");
            }
            if (i > 0 && t.rarity().ordinal() >= tiers.get(i - 1).rarity().ordinal()) {
                throw new IllegalArgumentException("tier " + (i + 1) + " (" + name(t.rarity()) + ") comes after "
                        + name(tiers.get(i - 1).rarity()) + ": the tiers run from rarest to commonest, each once");
            }
            if (c == 1.0 && i < tiers.size() - 1) {
                throw new IllegalArgumentException("tier " + (i + 1) + " (" + name(t.rarity()) + ") is guaranteed (1.0),"
                        + " and only the last tier may be: the tiers below it could never land");
            }
        }
        this.tiers = List.copyOf(tiers);
    }

    public List<Tier> tiers() {
        return tiers;
    }

    /** Whether the last tier is guaranteed (chance 1.0): a roll then always lands something. */
    public boolean guaranteed() {
        return tiers.get(tiers.size() - 1).chance() == 1.0;
    }

    /** One roll: the first tier to land, or empty. Draws one value in [0, 1) per tier rolled. */
    public Optional<Rarity> rollRarity(DoubleSupplier random) {
        for (Tier t : tiers) {
            if (random.getAsDouble() < t.chance()) return Optional.of(t.rarity());
        }
        return Optional.empty();
    }

    /** e: each tier's real probability (its chance times every miss above it) and nothing's. */
    public Odds trueOdds() {
        Map<Rarity, Double> by = new LinkedHashMap<>();
        double miss = 1.0;
        for (Tier t : tiers) {
            by.put(t.rarity(), miss * t.chance());
            miss *= 1.0 - t.chance();
        }
        return new Odds(Collections.unmodifiableMap(by), miss);
    }

    static String name(Rarity r) {
        return r.name().toLowerCase(java.util.Locale.ROOT);
    }
}
