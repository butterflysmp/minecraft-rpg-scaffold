package io.github.butterflysmp.rpg.paper.nexus;

import io.github.butterflysmp.rpg.paper.adapter.Keys;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/**
 * The Nexus star: mint one, and recognise one.
 *
 * <p>Slice 1 is the ITEM ONLY. It opens nothing -- right-clicking it does not reach any handler,
 * because none is registered for it. The hub it will eventually open is Slice 2's, and a command
 * that opened a menu which did not exist yet would open nothing, which is why there is no
 * {@code /menu} here either.
 *
 * <h2>IDENTIFIED BY PDC KEY, NEVER BY MATERIAL</h2>
 *
 * {@code HealthModifierItems.mint} already mints a {@code NETHER_STAR} for the health_boost_TEMP
 * dev fixture. The two stars coexist until the dev items are deleted, which is parked behind the
 * owed 4-tick grid readings -- so this is a long overlap, not a transient one. A Material test
 * would lock the DEV star to the hotbar and leave a re-minted Nexus star droppable. The precedent
 * is that same file, which keys off {@code keys.healthBoost} rather than off the material.
 */
public final class NexusItems {

    private NexusItems() {}

    /** Mint the Nexus star. One per player, placed and held at {@link NexusLock#LOCKED_SLOT}. */
    public static ItemStack mint(Keys keys) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        item.editMeta(meta -> {
            meta.displayName(MiniMessage.miniMessage()
                    .deserialize("<gradient:#8be9fd:#bd93f9>The Nexus</gradient>")
                    .decoration(TextDecoration.ITALIC, false));

            // A BYTE whose VALUE is never read. Presence is the whole tag -- isNexus asks
            // has(), not get() == 1, so a future value change cannot silently un-tag every
            // star already in circulation.
            meta.getPersistentDataContainer().set(keys.nexus, PersistentDataType.BYTE, (byte) 1);

            // EXPLICIT, AND NOT INHERITED FROM ANYTHING. The standing no-stacks-above-1 decision
            // names WeaponItems, ToolItems, ArmorItems and ShieldItems; the boost-fixture family
            // -- HealthModifierItems included, on this very material -- does NOT call this. So
            // the Nexus star opts in on purpose rather than by assumed inheritance.
            //
            // Load-bearing rather than tidy: two similar stars in one slot would make the
            // collect-to-cursor refusal in NexusLock reachable by a route that is currently
            // closed by arithmetic, and one write to a stack of two would edit both.
            meta.setMaxStackSize(1);
        });
        return item;
    }

    /**
     * Is this the Nexus star?
     *
     * <p>Null-guarded because this is called against arbitrary inventory slots, and
     * {@code Inventory#getContents()} is mostly nulls -- the same reason {@code GearItems.idOf}
     * is. Presence only: the stored byte's value is never consulted.
     */
    public static boolean isNexus(ItemStack item, Keys keys) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer()
                .has(keys.nexus, PersistentDataType.BYTE);
    }
}
