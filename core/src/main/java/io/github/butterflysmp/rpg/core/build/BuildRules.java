package io.github.butterflysmp.rpg.core.build;

import io.github.butterflysmp.rpg.core.build.LoadoutResolution.Equipped;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * What the Build screen may offer, and what a pick does (PLAN-build-system.md section 3.3). Pure: the
 * screen asks, renders the answer and saves it; every rule about WHICH choice is legal lives here.
 *
 * <p><b>Only pools decide.</b> A class is choosable because some pool names it, an element because a
 * pool names it for that class (ruling 7: only cells with a pool file, so FIRE only at ship), and an
 * ability because the cell's pool lists it in that ROLE. Nothing here reads the ability registry,
 * which is the "offer every registered ability" mistake made impossible rather than guarded.
 *
 * <p>"Unchosen" is the caller's sentinel ({@code PlayerProfile.NONE} in paper). Core cannot see the
 * storage module, so it is passed in rather than restated here.
 */
public final class BuildRules {

    private BuildRules() {}

    /** Every class some pool names, sorted, each once. */
    public static List<String> classes(Collection<PoolDefinition> pools) {
        return pools.stream().map(pool -> pool.cell().classId()).distinct().sorted().toList();
    }

    /**
     * The elements offered to a player of this class: those with a pool for it. A class no pool names
     * (the unchosen sentinel, in practice) is offered every pooled element, so picking the element
     * first still works.
     */
    public static List<String> elementsFor(Collection<PoolDefinition> pools, String classId) {
        boolean pooledClass = pools.stream().anyMatch(pool -> pool.cell().classId().equals(classId));
        return pools.stream()
                .filter(pool -> !pooledClass || pool.cell().classId().equals(classId))
                .map(pool -> pool.cell().elementId())
                .distinct().sorted().toList();
    }

    /**
     * The element a player keeps after picking a class: theirs, if that class has a pool for it, else
     * {@code unchosen}. A profile is never left naming a cell with no pool.
     */
    public static String elementAfterClassPick(Collection<PoolDefinition> pools, String classId,
                                               String currentElement, String unchosen) {
        // Null first: a profile's element can be null (a field absent from an old file), and slice 2's
        // one crash was contains(null) on an immutable list.
        return currentElement != null && hasPoolFor(pools, classId)
                && elementsFor(pools, classId).contains(currentElement)
                ? currentElement : unchosen;
    }

    private static boolean hasPoolFor(Collection<PoolDefinition> pools, String classId) {
        return pools.stream().anyMatch(pool -> pool.cell().classId().equals(classId));
    }

    /** What a slot offers: the pool's Ultimates for the Ultimate, its Actives for either Active. */
    public static List<String> choices(PoolDefinition pool, LoadoutSlot slot) {
        return switch (slot) {
            case ULTIMATE -> pool.ultimates();
            case ACTIVE_1, ACTIVE_2 -> pool.actives();
        };
    }

    /**
     * The loadout after putting {@code id} in {@code slot}, starting from what is equipped now (the
     * saved loadout, or the pool default when nothing is saved). Empty if the pool does not offer
     * {@code id} in that role.
     *
     * <p><b>Picking the ability the OTHER Active holds SWAPS the two</b>, so one ability never sits in
     * both Actives (the rule {@code /rpg build set} used in slice 2). An empty other slot swaps as an
     * empty: the hole moves.
     */
    public static Optional<Picked> pick(PoolDefinition pool, Equipped current, LoadoutSlot slot, String id) {
        if (id == null || !choices(pool, slot).contains(id)) return Optional.empty();
        String ultimate = current.ultimate();
        String active1 = current.active1();
        String active2 = current.active2();
        switch (slot) {
            case ULTIMATE -> ultimate = id;
            case ACTIVE_1 -> {
                if (id.equals(active2)) active2 = active1;
                active1 = id;
            }
            case ACTIVE_2 -> {
                if (id.equals(active1)) active1 = active2;
                active2 = id;
            }
        }
        return Optional.of(new Picked(ultimate, active1, active2));
    }

    /**
     * The fragments offered for one slot (slice 4): the pool's, minus any held in ANOTHER slot -- one of each
     * (ruling 11), so the screen never offers a duplicate rather than refusing one after the click. A slot
     * IS offered what it already holds.
     *
     * @param equipped the four slots as equipped ({@code LoadoutResolution.fragments}), nulls for empty
     */
    public static List<String> fragmentChoices(PoolDefinition pool, List<String> equipped, int slot) {
        return choicesFrom(pool.fragments(), equipped, slot);
    }

    /**
     * The four slots after putting {@code id} in {@code slot}; a null {@code id} EMPTIES the slot. Empty if
     * the slot does not exist, or the pool does not offer {@code id} there (outside the pool, or already held
     * in another slot).
     */
    public static Optional<List<String>> pickFragment(PoolDefinition pool, List<String> equipped, int slot, String id) {
        return pickFrom(pool.fragments(), equipped, slot, id, FragmentSlots.COUNT);
    }

    /**
     * The aspects offered for one aspect slot (slice 5): the pool's, minus the one held in the OTHER slot --
     * "the same aspect twice is refused" (section 2.4), by never offering it. Whether an aspect's target is
     * equipped does not matter here: an inactive aspect may still be slotted (amendment 2).
     */
    public static List<String> aspectChoices(PoolDefinition pool, List<String> equipped, int slot) {
        return choicesFrom(pool.aspects(), equipped, slot);
    }

    /** The two aspect slots after putting {@code id} (or null: EMPTY it, ruling 23) in {@code slot}. */
    public static Optional<List<String>> pickAspect(PoolDefinition pool, List<String> equipped, int slot, String id) {
        return pickFrom(pool.aspects(), equipped, slot, id, AspectSlots.COUNT);
    }

    private static List<String> choicesFrom(List<String> offered, List<String> equipped, int slot) {
        return offered.stream().filter(id -> !heldElsewhere(equipped, slot, id)).toList();
    }

    private static Optional<List<String>> pickFrom(List<String> offered, List<String> equipped, int slot, String id,
                                                   int count) {
        if (slot < 0 || slot >= count || equipped.size() != count) return Optional.empty();
        if (id != null && !choicesFrom(offered, equipped, slot).contains(id)) return Optional.empty();
        List<String> next = new java.util.ArrayList<>(equipped);
        next.set(slot, id);
        return Optional.of(java.util.Collections.unmodifiableList(next));
    }

    private static boolean heldElsewhere(List<String> equipped, int slot, String id) {
        for (int other = 0; other < equipped.size(); other++) {
            if (other != slot && id.equals(equipped.get(other))) return true;
        }
        return false;
    }

    /**
     * A loadout the screen will save. Any slot may be null (empty). A record: an immutable value whose
     * equals compares every field, which is what the tests lean on.
     *
     * <p>No duplicate-Active check here, deliberately: {@link #pick} cannot produce one (the swap), and
     * {@code CellLoadout} blanks a duplicate on write anyway. A third guard here could never fire, and it
     * would turn the swap mutation's clean assertion failure into an exception from somewhere else.
     */
    public record Picked(String ultimate, String active1, String active2) {}
}
