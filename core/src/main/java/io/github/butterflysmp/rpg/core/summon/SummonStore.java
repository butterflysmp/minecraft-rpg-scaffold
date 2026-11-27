package io.github.butterflysmp.rpg.core.summon;

import io.github.butterflysmp.rpg.core.weapon.GearScore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/**
 * WHO OWNS WHICH SUMMON BODY (D6, D7; the seat's 3b, 2026-11-24). Never saved: it lives in memory, and every body it
 * owns is removed at stop. One summon per owner per WEAPON ID. A body is "owned" exactly while its record is here: the
 * orphan sweep and the backstop remove any tagged body that is not.
 *
 * <p>Thread-safe by {@code synchronized}: a summon is placed on the caster's thread and removed on the body's.
 */
public final class SummonStore {

    /**
     * One live summon. {@code health} and {@code damage} are D3's: authored x score / 100, the score READ AT THE CAST and
     * fixed for the summon's life ({@link GearScore#scaledDamage}, the same formula a weapon's damage uses).
     */
    public record Summon(UUID owner, String weaponId, UUID body, String summonId, int score, double health,
                         double damage, double authoredDamage, String element) {   // element: the WEAPON's (E1)

        public static Summon cast(UUID owner, String weaponId, UUID body, SummonDefinition def, int score, String element) {
            int used = GearScore.clamp(score);   // READ: scaledDamage clamps the score (MIN 100); keep the one used
            return new Summon(owner, weaponId, body, def.id(), used, GearScore.scaledDamage(def.health(), used),
                    GearScore.scaledDamage(def.damage(), used), def.damage(), element);
        }
    }

    private final Map<UUID, Summon> byBody = new LinkedHashMap<>();
    private final Map<UUID, Map<String, UUID>> byOwner = new HashMap<>();
    /** D5: a summon's DIRECTED target (set only by the left click), by body; gone with the body. */
    private final Map<UUID, UUID> directed = new HashMap<>();

    /** R3: the guard's HELD target and its tier (G7, 2026-11-26), by body, published by the goal each tick; gone with the body. */
    private final Map<UUID, SummonGuard.Held> current = new HashMap<>();

    /** Own a body. If his summon of this weapon lived, it is no longer owned, and is handed back for removal. */
    public synchronized Optional<Summon> place(Summon s) {
        Map<String, UUID> his = byOwner.computeIfAbsent(s.owner(), k -> new LinkedHashMap<>());
        UUID replaced = his.put(s.weaponId(), s.body());
        byBody.put(s.body(), s);
        if (replaced != null) {   // R3: the replaced body's order and current target go with it
            directed.remove(replaced);
            current.remove(replaced);
        }
        return replaced == null ? Optional.empty() : Optional.ofNullable(byBody.remove(replaced));
    }

    public synchronized boolean owns(UUID body) {
        return byBody.containsKey(body);
    }

    public synchronized Optional<Summon> of(UUID body) {
        return Optional.ofNullable(byBody.get(body));
    }

    /** The weapon ids whose summon of his is alive: what {@link SummonCap#decide} counts. */
    public synchronized Set<String> liveWeapons(UUID owner) {
        Map<String, UUID> his = byOwner.get(owner);
        return his == null ? Set.of() : Set.copyOf(new TreeSet<>(his.keySet()));
    }

    /** Stop owning one body. */
    public synchronized Optional<Summon> remove(UUID body) {
        Summon s = byBody.remove(body);
        directed.remove(body);
        current.remove(body);   // R3
        if (s == null) return Optional.empty();
        Map<String, UUID> his = byOwner.get(s.owner());
        if (his != null) {
            his.remove(s.weaponId(), body);
            if (his.isEmpty()) byOwner.remove(s.owner());
        }
        return Optional.of(s);
    }

    /** Stop owning every body of his (his death, quit, world change). */
    public synchronized List<Summon> removeAllOf(UUID owner) {
        Map<String, UUID> his = byOwner.remove(owner);
        List<Summon> out = new ArrayList<>();
        if (his != null) for (UUID body : his.values()) {
            Summon s = byBody.remove(body);
            directed.remove(body);
            current.remove(body);   // R3
            if (s != null) out.add(s);
        }
        return out;
    }

    /** Stop owning every body (server stop). */
    public synchronized List<Summon> removeAll() {
        List<Summon> out = new ArrayList<>(byBody.values());
        byBody.clear();
        directed.clear();
        current.clear();   // R3
        byOwner.clear();
        return out;
    }

    /** D5: the left click's order for this body's summon. Only an owned body takes one. */
    public synchronized boolean direct(UUID body, UUID target) {
        if (!byBody.containsKey(body)) return false;
        directed.put(body, target);
        return true;
    }

    public synchronized Optional<UUID> directed(UUID body) {
        return Optional.ofNullable(directed.get(body));
    }

    /** R3: the goal publishes the target the guard chose this tick (empty when none). Only an owned body keeps one. */
    public synchronized void setCurrent(UUID body, Optional<SummonGuard.Held> held) {
        if (held.isPresent() && byBody.containsKey(body)) current.put(body, held.get()); else current.remove(body);
    }

    public synchronized Optional<UUID> current(UUID body) {
        return Optional.ofNullable(current.get(body)).map(SummonGuard.Held::id);
    }

    /** G7: the tier the held target is held at, for the probe; empty when nothing is held. */
    public synchronized Optional<SummonGuard.Why> currentTier(UUID body) {
        return Optional.ofNullable(current.get(body)).map(SummonGuard.Held::tier);
    }

    /** F1: the order is cleared when its target is dead, gone or beyond the chase limit from him. */
    public synchronized void clearDirected(UUID body) {
        directed.remove(body);
    }

    /** The owned body of HIS summon of this weapon, if it lives: the left click directs that one (D5). */
    public synchronized Optional<UUID> bodyOf(UUID owner, String weaponId) {
        Map<String, UUID> his = byOwner.get(owner);
        return his == null ? Optional.empty() : Optional.ofNullable(his.get(weaponId));
    }

    public synchronized List<Summon> all() {
        return List.copyOf(byBody.values());
    }
}
