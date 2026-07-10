package io.github.butterflysmp.rpg.core.combat;

import java.util.UUID;

/**
 * Anything that can be hit: a player, a boss, a training dummy in a unit test.
 *
 * Two halves, deliberately. {@code state} is what was true when the world found this
 * combatant, read on the thread that owned it. {@code handle} is how to act on it. Nothing
 * in core may read the world through a handle, and nothing may act through a snapshot; the
 * types say which is which, so it is no longer possible to issue a read too late.
 *
 * @param state  a photograph, taken on the owning thread. Safe to carry; may go stale.
 * @param handle a dispatcher. Holds a live entity in the Paper adapter, so it must NOT be
 *               carried across a tick -- see EffectApplier, which keeps only a UUID.
 */
public record Combatant(CombatantSnapshot state, CombatantHandle handle) {

    public UUID id() {
        return state.id();
    }
}
