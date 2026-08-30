package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.weapon.ArmorRegistry;
import io.github.butterflysmp.rpg.core.weapon.GearDefinition;
import io.github.butterflysmp.rpg.core.weapon.GearRegistry;
import io.github.butterflysmp.rpg.core.weapon.RefreshVerdict;
import io.github.butterflysmp.rpg.core.weapon.ShieldRegistry;
import io.github.butterflysmp.rpg.core.weapon.WeaponRegistry;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.adapter.Keys;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * Rebuild the display of every piece of custom gear a player is carrying, from the content loaded
 * now. Runs on join and on {@code /rpg refresh}.
 *
 * <h2>This closes two deferrals at once, and they were the same deferral</h2>
 *
 * {@code WeaponRefresher} shipped with weapons and was never widened. Shields Slice 1 recorded "no
 * {@code ShieldRefresher}" as outstanding; the armor slice recorded "no {@code ArmorRefresher}" the
 * same way. Both waited HERE deliberately rather than being written twice more: a refresher is a
 * definition lookup plus a re-mint, which is exactly the pair the gear extraction factored, so
 * writing two more copies first would have left five shapes to reconcile instead of three.
 *
 * <p>The consequence until now was quiet and real: a shield or a piece of armor whose content file
 * changed kept its old tooltip forever, because nothing rebuilt it. Only weapons got the tuning
 * loop {@code /rpg refresh} exists to provide.
 *
 * <h2>Resolution order matches {@code /rpg give}</h2>
 *
 * Weapons, then shields, then armor -- the same order, deliberately, so an id that collides across
 * two registries resolves to the SAME definition whether it was minted by {@code give} or rebuilt by
 * a refresh. The boot warns about such a collision; this makes the two paths agree even before
 * anyone fixes it, rather than having an item change kind on rejoin.
 *
 * <p>An item carries at most one of the three tags -- each {@code *Items.mint} writes exactly one --
 * so the order is a tie-break that should never fire, not a precedence rule doing real work.
 */
public final class GearRefresher {

    private GearRefresher() {}

    /**
     * Rebuild every tagged item in {@code player}'s inventory. Returns how many were re-minted.
     *
     * <p>Walks {@code getContents()} by index and writes back to the same slot, so nothing moves in
     * the inventory and a full inventory cannot lose an item -- a refresh must never behave like a
     * give.
     */
    public static int refresh(Player player, WeaponRegistry weapons, ShieldRegistry shields,
                              ArmorRegistry armor, AdapterContext adapters) {
        PlayerInventory inventory = player.getInventory();
        ItemStack[] contents = inventory.getContents();
        Keys keys = adapters.keys();
        int refreshed = 0;

        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if (item == null) continue;

            ItemStack rebuilt = rebuild(item, keys.weaponId, weapons, adapters);
            if (rebuilt == null) rebuilt = rebuild(item, keys.shieldId, shields, adapters);
            if (rebuilt == null) rebuilt = rebuild(item, keys.armorId, armor, adapters);

            if (rebuilt != null) {
                inventory.setItem(slot, rebuilt);
                refreshed++;
            }
        }
        return refreshed;
    }

    /**
     * One tag's worth of the decision: null if this item is not tagged with {@code idKey} (so the
     * caller should try the next registry), or if it is tagged but dangling.
     *
     * <p>The verdict switch is EXHAUSTIVE over a sealed interface with NO default arm, exactly as
     * {@code WeaponRefresher}'s was: a fourth verdict becomes a compile error here until it is
     * handled rather than being swallowed.
     */
    private static ItemStack rebuild(ItemStack item, NamespacedKey idKey,
                                     GearRegistry<? extends GearDefinition> registry,
                                     AdapterContext adapters) {
        String id = GearItems.idOf(item, idKey).orElse(null);
        return switch (RefreshVerdict.decide(id, registry)) {
            // Not tagged with THIS key. Might still be another kind, so say "not mine", not "leave
            // it alone" -- returning null is what lets the caller fall through to the next registry.
            case RefreshVerdict.Untagged ignored -> null;

            // Content was renamed or deleted under an item someone is holding. Leave the item
            // exactly as it is -- stripping it would cost a player real gear over a typo, and it
            // starts working again the moment the definition comes back. warnOnce, not warning:
            // this runs per player per login and would otherwise repeat forever.
            case RefreshVerdict.Dangling dangling -> {
                adapters.warnOnce("Refresh: item carries unknown gear id '" + dangling.id()
                        + "' -- left untouched; its content file is missing or renamed");
                yield null;
            }

            case RefreshVerdict.Remint remint ->
                    GearItems.remint(item, remint.definition(), adapters);
        };
    }
}
