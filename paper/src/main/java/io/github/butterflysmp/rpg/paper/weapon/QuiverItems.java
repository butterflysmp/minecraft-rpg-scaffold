package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.weapon.Quiver;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import io.github.butterflysmp.rpg.paper.adapter.Keys;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.OptionalInt;

/**
 * The Bukkit half of the quiver: reading a stack's magazine and writing it back.
 *
 * <p>Every DECISION is {@link Quiver}, in core, where it is unit-tested; this class only moves values
 * in and out of item meta. Same split as {@link WeaponDurability} and for the same reason --
 * {@code new ItemStack(...)} throws without a running server and there is no MockBukkit, so nothing
 * here can be unit-tested and it is boot-witnessed instead. That is the argument for keeping it this
 * thin: <b>every line of arithmetic that lives here is a line no test can reach.</b>
 *
 * <h2>ABSENCE IS NOT A NEUTRAL VALUE</h2>
 *
 * <p>This is the whole reason {@link #loadedIn} returns an {@link OptionalInt} rather than an
 * {@code int} defaulting to 0. <b>An unstamped quiver and an empty one must never resolve alike.</b>
 * A default of 0 would make a forgotten stamp indistinguishable from a spent magazine -- a weapon
 * that silently never fires, on an item that looks completely ordinary.
 *
 * <p>The argument is not new, and it was already won once in this repository for a different field.
 * {@code volley_stone.yml} authors {@code attack_damage: 0} explicitly rather than omitting it,
 * because <i>"the absence would resolve to exactly the value the omission meant -- which is the trap:
 * it works, it is invisible, and it hides a decision."</i> A quiver count has that shape with a
 * player-visible consequence attached.
 *
 * <p>So: {@link #stampFull} is called at mint, {@link #carry} moves the value across every re-mint,
 * and a weapon whose definition declares a quiver but whose item carries no count is a DEFECT the
 * caller reports -- never a zero it accepts.
 */
public final class QuiverItems {

    private QuiverItems() {}

    /**
     * The rounds this stack currently holds, or empty when it carries no quiver stamp at all.
     *
     * <p>Empty means "this item was never stamped", which for a weapon whose definition declares a
     * quiver is a defect and for every other weapon is simply the ordinary answer. It does NOT mean
     * "empty magazine" -- that is {@code OptionalInt.of(0)}, and the two must stay distinguishable
     * all the way to the call site.
     */
    public static OptionalInt loadedIn(ItemStack item, Keys keys) {
        if (item == null || !item.hasItemMeta()) return OptionalInt.empty();
        Integer stored = item.getItemMeta().getPersistentDataContainer()
                .get(keys.quiverLoaded, PersistentDataType.INTEGER);
        return stored == null ? OptionalInt.empty() : OptionalInt.of(stored);
    }

    /**
     * Stamp a freshly minted weapon's magazine FULL, or leave a non-quiver weapon untouched.
     *
     * <p><b>Called before {@code applyLore}</b> -- though NOT yet load-bearing, and saying so is the
     * point. No lore line reads the count today: {@code WeaponLore.build} sees only the definition,
     * and rendering a per-item count means widening its signature, which is owed rather than done.
     * The ordering is established now because it is free now and a wrong tooltip later is not.
     *
     * <p>The enchant block's ordering note in {@code WeaponItems.remint} is the MIRROR of this case,
     * not the same trap, and the difference is what makes this call's placement load-bearing rather
     * than conventional. Enchants were safe at mint because an empty container rendered NOTHING --
     * that note's own words, <i>"which was a no-op"</i> -- and because {@code applyLore} runs TWICE
     * per re-mint, so the second pass sees the carried state and corrects the first. <b>A quiver
     * inverts both halves: absence renders a NUMBER, and a fresh mint has no second pass.</b>
     */
    public static void stampFull(ItemMeta meta, WeaponDefinition weapon, Keys keys) {
        if (!weapon.hasQuiver()) return;
        meta.getPersistentDataContainer().set(
                keys.quiverLoaded, PersistentDataType.INTEGER, Quiver.reload(weapon.quiverSize()));
    }

    /**
     * Carry a quiver's magazine and any reload in progress across a re-mint.
     *
     * <p>The fourth thing {@code GearItems.carryInstanceData} moves, beside the id tag, the wear and
     * the enchant blob. <b>Losing it would be a relog-to-refill exploit</b>, precisely as losing the
     * enchant blob would be a relog-to-unlock one -- and a re-mint happens on every join, every
     * {@code /rpg refresh} and every enchant-table click, so the exploit would be one F3+A away.
     *
     * <p>The three keys move as a group but are not one value. The count is meaningful alone -- most
     * quivers are not mid-reload -- while the two reload ticks are two halves of ONE fact and are
     * carried together or not at all, which is the {@code classDamageBoost} discipline rather than
     * the {@code enchantData} one. A half-carried reload would be a weapon with a deadline and no
     * start, and the restart guard's bound is the start.
     */
    public static void carry(ItemMeta from, ItemMeta to, Keys keys) {
        PersistentDataContainer source = from.getPersistentDataContainer();
        PersistentDataContainer target = to.getPersistentDataContainer();

        Integer loaded = source.get(keys.quiverLoaded, PersistentDataType.INTEGER);
        if (loaded != null) target.set(keys.quiverLoaded, PersistentDataType.INTEGER, loaded);

        Long startedAt = source.get(keys.quiverReloadStartedAt, PersistentDataType.LONG);
        Long completesAt = source.get(keys.quiverReloadCompletesAt, PersistentDataType.LONG);
        if (startedAt != null && completesAt != null) {
            target.set(keys.quiverReloadStartedAt, PersistentDataType.LONG, startedAt);
            target.set(keys.quiverReloadCompletesAt, PersistentDataType.LONG, completesAt);
        }
    }
}
