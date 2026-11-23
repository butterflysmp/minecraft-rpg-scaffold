package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.enchant.CandidateCascade;
import io.github.butterflysmp.rpg.core.enchant.CandidateCascade.Tier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Reads {@code content/enchant_candidates.yml}: Ben's cascade of how many candidates a slot offers (the seat's ruling V3,
 * 2026-11-23), authored in content "in the manner of loot/cascade.yml".
 *
 * <pre>
 *   tiers:                  # rarest first; the last guaranteed
 *     - candidates: 4
 *       chance: 0.10
 *     ...
 *     - candidates: 1
 *       chance: 1.0
 * </pre>
 *
 * The WHOLE file is refused, by name and reason, for anything {@link CandidateCascade}'s constructor refuses (not rarest
 * first, a chance outside [0, 1], a count outside 1..4, a last tier that is not guaranteed, a guaranteed tier above
 * another), and for a missing file, a missing {@code tiers:} or a tier missing a key. A refused file leaves
 * {@link CandidateCascade#ONE}: every slot offers one candidate, toward fewer, until it is fixed.
 *
 * <p>Its Loaded line prints each written chance and its TRUE odds, the way {@code LootLoader}'s does.
 */
public final class EnchantCandidatesLoader {

    private final Logger log;

    public EnchantCandidatesLoader(Logger log) {
        this.log = log;
    }

    /** The cascade, or {@link CandidateCascade#ONE} with one WARN naming the file and the reason. Never throws. */
    public CandidateCascade load(File file) {
        try {
            if (!file.isFile()) throw new IllegalArgumentException("the file is missing");
            CandidateCascade cascade = parse(YamlConfiguration.loadConfiguration(file));
            log.info(loadedLine(cascade));
            return cascade;
        } catch (RuntimeException ex) {
            log.warning("Refusing enchant candidates in " + file.getName() + ": " + ex.getMessage()
                    + "; every enchant slot offers ONE candidate until it is fixed");
            return CandidateCascade.ONE;
        }
    }

    static CandidateCascade parse(ConfigurationSection root) {
        List<Map<?, ?>> raw = root.getMapList("tiers");
        if (raw.isEmpty()) throw new IllegalArgumentException("needs 'tiers:', a list of candidates and chance");
        List<Tier> tiers = new ArrayList<>();
        for (int i = 0; i < raw.size(); i++) {
            Map<?, ?> m = raw.get(i);
            if (!(m.get("candidates") instanceof Number n) || !(m.get("chance") instanceof Number c)) {
                throw new IllegalArgumentException("tier " + (i + 1) + " needs a number 'candidates' and a number 'chance'");
            }
            if (n.doubleValue() != n.intValue()) {
                throw new IllegalArgumentException("tier " + (i + 1) + " 'candidates' must be a whole number, was " + n);
            }
            tiers.add(new Tier(n.intValue(), c.doubleValue()));
        }
        return new CandidateCascade(tiers);
    }

    /** {@code Loaded enchant candidates: 4 at 10.00% (truly 10.000%), 3 at 40.00% (truly 36.000%), ...}. */
    static String loadedLine(CandidateCascade cascade) {
        List<Double> odds = cascade.trueOdds();
        List<String> parts = new ArrayList<>();
        for (int i = 0; i < cascade.tiers().size(); i++) {
            Tier t = cascade.tiers().get(i);
            parts.add(String.format(Locale.ROOT, "%d at %.2f%% (truly %.3f%%)", t.candidates(), 100.0 * t.chance(),
                    100.0 * odds.get(i)));
        }
        return "Loaded enchant candidates: " + String.join(", ", parts);
    }
}
