package io.github.butterflysmp.rpg.paper.content;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Immutable-after-load lookup for every custom enchant the server knows about. */
public final class EnchantRegistry {
    private final Map<String, EnchantDefinition> byId = new LinkedHashMap<>();

    public void register(EnchantDefinition def) {
        if (byId.putIfAbsent(def.id(), def) != null) {
            throw new IllegalStateException("Duplicate enchant id: " + def.id());
        }
    }

    public Optional<EnchantDefinition> find(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    public Collection<EnchantDefinition> all() {
        return Collections.unmodifiableCollection(byId.values());
    }

    public int size() { return byId.size(); }

    /**
     * HOW MANY CANDIDATES A SLOT OFFERS: Ben's cascade (2026-11-23, the seat's ruling V3), read from
     * {@code content/enchant_candidates.yml} by {@code EnchantCandidatesLoader} and set once at load. Carried here, beside
     * the roster the roll draws from, so every caller that reaches the roster reaches the cascade.
     *
     * <p>Until it is set, and whenever that file is refused, it is {@code CandidateCascade.ONE}: every slot offers one
     * candidate. Fail closed, toward fewer, and never Ben's numbers written as code.
     */
    private volatile io.github.butterflysmp.rpg.core.enchant.CandidateCascade candidates =
            io.github.butterflysmp.rpg.core.enchant.CandidateCascade.ONE;

    public io.github.butterflysmp.rpg.core.enchant.CandidateCascade candidates() { return candidates; }

    public void setCandidates(io.github.butterflysmp.rpg.core.enchant.CandidateCascade candidates) {
        if (candidates == null) throw new IllegalArgumentException("candidates required (CandidateCascade.ONE for none)");
        this.candidates = candidates;
    }
}
