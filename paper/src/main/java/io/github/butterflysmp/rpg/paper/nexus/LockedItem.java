package io.github.butterflysmp.rpg.paper.nexus;

import io.github.butterflysmp.rpg.paper.adapter.Keys;
import org.bukkit.inventory.ItemStack;

import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * One item the Nexus lock protects: what it is, how to make one, and where it may sit
 * (PLAN-build-system.md section 1.3, "GENERALISE, don't copy").
 *
 * <p>There are two: the Nexus star and the Ability Stone. {@link NexusLock} never needed this -- it was
 * already generic over a {@code cursorIsStar} flag and a {@code starAt} predicate. What was hard-wired
 * to the star was the layer above it, {@code NexusSlots}' convergence and removal, and that is what
 * takes a descriptor now. A second COPY of that layer would be two lock implementations that must stay
 * in step, and GATE-nexus.md rows 6.4 and 6.5 were two duplication holes found in ONE.
 *
 * @param noun        how a player-facing line names it ("the Nexus", "the Ability Stone")
 * @param is          identity, by PDC key -- never by material
 * @param mint        a fresh one, for convergence to place when none exists
 * @param defaultSlot where it goes when the stored slot is unusable
 * @param maxSlot     the highest {@code PlayerInventory} index it may occupy (star 35, stone 8)
 */
public record LockedItem(String noun, Predicate<ItemStack> is, Supplier<ItemStack> mint,
                         int defaultSlot, int maxSlot) {

    /** The Nexus star, exactly as {@code NexusSlots} treated it before this record existed. */
    public static LockedItem star(Keys keys) {
        return new LockedItem("the Nexus", item -> NexusItems.isNexus(item, keys), () -> NexusItems.mint(keys),
                NexusLock.DEFAULT_LOCKED_SLOT, NexusSlots.MAX_SLOT);
    }
}
