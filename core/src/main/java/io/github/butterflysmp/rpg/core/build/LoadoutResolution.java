package io.github.butterflysmp.rpg.core.build;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Which abilities a player of a cell actually casts: their SAVED loadout, or the pool's default when they
 * have none (PLAN-build-system.md section 2.1 and 3.2).
 *
 * <p>Pure, and it takes the saved slots as three nullable ids rather than a storage type, because core
 * depends on nothing ({@code CellLoadout} lives in storage, which depends on core).
 *
 * <h2>The two rules</h2>
 *
 * <ol>
 *   <li><b>No saved loadout for this cell: the pool's default</b>, whole. This is the slice-1 behaviour,
 *       kept for a player who never chose.</li>
 *   <li><b>A saved loadout: each slot is its saved id IF THE POOL STILL OFFERS IT IN THAT ROLE</b> -- an
 *       Ultimate from {@code ultimates}, an Active from {@code actives} -- and EMPTY otherwise. An id the
 *       content removed, or moved from Active to Ultimate, therefore reads as an empty slot rather than
 *       casting something the pool no longer offers there. It does NOT fall back to the default slot by
 *       slot: a player who saved a build gets THEIR build, with a visible hole, never a silent mix of
 *       theirs and the default.</li>
 * </ol>
 */
public final class LoadoutResolution {

    private LoadoutResolution() {}

    /**
     * @param saved empty when the player saved nothing for this cell; otherwise the saved ids, any of them
     *              null for an empty slot
     */
    public static Equipped resolve(PoolDefinition pool, Optional<Saved> saved) {
        if (saved.isEmpty()) {
            Loadout d = pool.defaultLoadout();
            return new Equipped(d.ultimate(), d.active1(), d.active2(), true);
        }
        Saved s = saved.get();
        String ultimate = offers(pool.ultimates(), s.ultimate());
        String active1 = offers(pool.actives(), s.active1());
        String active2 = offers(pool.actives(), s.active2());
        if (active1 != null && active1.equals(active2)) active2 = null;   // one ability, one Active slot
        return new Equipped(ultimate, active1, active2, false);
    }

    /**
     * The id if the pool offers it in this role, else null. NULL-SAFE ON PURPOSE: the pool's lists are
     * {@code List.copyOf} copies, whose {@code contains(null)} THROWS rather than answering false -- found
     * by {@code LoadoutResolutionTest.savedEmptySlotsStayEmpty}, the first run of which crashed on exactly
     * the empty slot a saved loadout is allowed to have.
     */
    private static String offers(java.util.List<String> offered, String id) {
        return id != null && offered.contains(id) ? id : null;
    }

    /** The saved ids for one cell, any of them null. */
    /**
     * The four fragment slots as equipped (slice 4): a saved id the pool still offers, once each (ruling 11
     * -- a hand-edited duplicate keeps its first slot); anything else reads EMPTY, never a default. Nothing
     * saved is four empties: a pool's default loadout carries no fragments. Always four entries, nulls for
     * empty, in slot order.
     */
    public static List<String> fragments(PoolDefinition pool, List<String> saved) {
        List<String> equipped = new ArrayList<>(Collections.nCopies(FragmentSlots.COUNT, (String) null));
        if (saved == null) return Collections.unmodifiableList(equipped);
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (int slot = 0; slot < FragmentSlots.COUNT && slot < saved.size(); slot++) {
            String id = offers(pool.fragments(), saved.get(slot));
            if (id != null && seen.add(id)) equipped.set(slot, id);
        }
        return Collections.unmodifiableList(equipped);
    }

    public record Saved(String ultimate, String active1, String active2) {}

    /**
     * What is equipped: any slot may be empty (null).
     *
     * @param fromDefault true when this is the pool's default because nothing was saved
     */
    public record Equipped(String ultimate, String active1, String active2, boolean fromDefault) {

        public Optional<String> idFor(LoadoutSlot slot) {
            return Optional.ofNullable(switch (slot) {
                case ACTIVE_1 -> active1;
                case ACTIVE_2 -> active2;
                case ULTIMATE -> ultimate;
            });
        }

        /** Every equipped id, empties skipped: the castable set for the stone and a non-op's /rpg cast. */
        public List<String> ids() {
            List<String> ids = new ArrayList<>();
            for (String id : new String[] {ultimate, active1, active2}) if (id != null) ids.add(id);
            return Collections.unmodifiableList(ids);
        }
    }
}
