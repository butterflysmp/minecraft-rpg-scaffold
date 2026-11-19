package io.github.butterflysmp.rpg.core.fight;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * THE MAGE'S DAMAGE GATE (PLAN-dragon-fight.md step F6, section 4 i): one question asked at the store's damage entry
 * for every mob, "is this hit refused?". A fight GUARDS its Mage here with a {@link Rule}; every other mob is never
 * refused. While the rule says he is not damageable, every damage of every kind to him is refused: the paper side asks
 * at {@code BukkitCombatant}'s one choke point and cancels vanilla's own damage events, so weapon, ability, status
 * tick, blast and vanilla all meet the same answer.
 *
 * <p>One instance per server, owned by the adapter context, never a static (CLAUDE.md invariant 3). Thread-safe maps:
 * the question is asked on a mob's own scheduler.
 */
public final class DamageGate {

    /** A guarded mob's rule. */
    public interface Rule {
        /** Whether a hit lands now. */
        boolean damageable();

        /** A hit was refused: say so (the fight's MAGEGATE line and its cue). */
        void refused(double amount, String source);
    }

    private final Map<UUID, Rule> rules = new ConcurrentHashMap<>();
    private final Set<UUID> allowOnce = ConcurrentHashMap.newKeySet();

    public void guard(UUID mob, Rule rule) {
        rules.put(mob, rule);
    }

    public void release(UUID mob) {
        rules.remove(mob);
        allowOnce.remove(mob);
    }

    public boolean guarded(UUID mob) {
        return rules.containsKey(mob);
    }

    /** The next hit on this mob passes whatever its rule says (the fixture's own {@code mage-die}). */
    public void allowNext(UUID mob) {
        if (rules.containsKey(mob)) allowOnce.add(mob);
    }

    /** Whether this hit is refused; a refused hit is reported to the rule. */
    public boolean refuses(UUID mob, double amount, String source) {
        Rule rule = rules.get(mob);
        if (rule == null) return false;
        if (allowOnce.remove(mob)) return false;
        if (rule.damageable()) return false;
        rule.refused(amount, source);
        return true;
    }
}
