package io.github.butterflysmp.rpg.core.build;

import java.util.List;

/**
 * Which ability sits in each casting slot. Ability ids only: the registry lookup, the cooldown and the
 * mana all happen at cast time in {@code AbilityService}.
 *
 * <p>Slice 1 has exactly one loadout per cell, the pool's {@code default}. Slice 2 adds a saved one per
 * player and falls back to this when none is saved.
 */
public record Loadout(String ultimate, String active1, String active2) {

    public Loadout {
        requireId(ultimate, "ultimate");
        requireId(active1, "active 1");
        requireId(active2, "active 2");
        if (active1.equals(active2)) {
            throw new IllegalArgumentException("the two actives must differ, both are '" + active1 + "'");
        }
    }

    /** The ability bound to one slot. */
    public String idFor(LoadoutSlot slot) {
        return switch (slot) {
            case ACTIVE_1 -> active1;
            case ACTIVE_2 -> active2;
            case ULTIMATE -> ultimate;
        };
    }

    /** Every equipped id -- the "castable" set a non-operator's {@code /rpg cast} is limited to. */
    public List<String> ids() {
        return List.of(ultimate, active1, active2);
    }

    private static void requireId(String id, String what) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException(what + " required");
    }
}
