package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.weapon.Quiver;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
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
        return loadedInMeta(item.getItemMeta(), keys);
    }

    /**
     * The same read against a meta already in hand.
     *
     * <p>Exists for one caller: {@code WeaponItems.applyLore}, which is building a tooltip INSIDE an
     * {@code editMeta} block and therefore holds the meta but no finished stack. Going through
     * {@link #loadedIn} there would read the ORIGINAL item and miss the stamp that was written a few
     * lines earlier in the same block -- rendering the previous count on every mint.
     */
    public static OptionalInt loadedInMeta(ItemMeta meta, Keys keys) {
        if (meta == null) return OptionalInt.empty();
        Integer stored = meta.getPersistentDataContainer()
                .get(keys.quiverLoaded, PersistentDataType.INTEGER);
        return stored == null ? OptionalInt.empty() : OptionalInt.of(stored);
    }

    /**
     * Stamp a freshly minted weapon's magazine FULL, or leave a non-quiver weapon untouched.
     *
     * <p><b>MUST be called before {@code applyLore}.</b> That builder now reads this count back off
     * the meta to render the "Quiver: 9/9" line, so a stamp written after it renders the PREVIOUS
     * count -- or "--/9" on a first mint. The ordering was free when this was written and is
     * load-bearing since the lore line landed.
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
     * Set the magazine to {@code count} <b>and re-render the tooltip</b> -- the only way to change a
     * count on an item that is already in play.
     *
     * <h2>WHY THE WRITE AND THE RENDER ARE ONE CALL</h2>
     *
     * <p><b>They were two, and the display silently stopped tracking.</b> The boot gate's V1 found it:
     * a bolt fired, the stored count went 9 → 8, and <i>"the number doesn't change in the lore of the
     * item"</i>. {@code spendRound} wrote the key and called {@code updateInventory}, and nothing
     * anywhere re-ran {@code applyLore} — which executes only from {@code mint} and {@code remint}. So
     * the tooltip carried whatever was rendered AT MINT TIME and never moved again.
     *
     * <p><b>THE SYMPTOM WAS "THE COUNTER ONLY UPDATES WHEN YOU RELOG", AND THAT IS WHY THREE GREEN
     * ROWS WERE NOT EVIDENCE AGAINST IT.</b> Q4, Q5 and Q6 — relog, {@code /rpg refresh}, the enchant
     * table — all route through {@code remint}, which DOES call {@code applyLore}. They passed because
     * the tooltip snaps to the correct value at exactly those three moments. Three greens entirely
     * consistent with the defect.
     *
     * <p><b>So the rule is structural rather than remembered:</b> a per-item value that is RENDERED
     * must be re-rendered wherever it is WRITTEN, or the display is only ever correct at mint. Making
     * it one call means a fourth write site cannot forget — there is no way to express the write
     * without the render. {@link #stampFull} is the single exception and is <b>MINT-ONLY</b>, where
     * {@code applyLore} runs immediately afterwards by construction; {@code QuiversSignatureTest}
     * pins that {@code Quivers} calls neither it nor the raw key write.
     *
     * <h2>refreshLore, NOT remint -- and the reason is correctness before cost</h2>
     *
     * <p>{@code remint} was the call already to hand and it is the wrong one. It builds a <b>NEW
     * ItemStack</b>, so every shot would replace the stack in the player's hand -- churning item
     * identity mid-combat, at up to the input repeat rate, with whatever that does to cursors, menus
     * and {@code isSimilar} comparisons. It also re-resolves the material, re-applies the attribute
     * block, stamps the magazine FULL and then carries the old count back over it, and runs
     * {@code applyLore} TWICE. {@link WeaponItems#refreshLore} rebuilds one list of components on the
     * meta already in hand. The cost argument is real but secondary; the identity argument is the one
     * that decides it.
     */
    public static void setLoaded(ItemMeta meta, WeaponDefinition weapon, AdapterContext adapters,
                                 int count) {
        meta.getPersistentDataContainer().set(
                adapters.keys().quiverLoaded, PersistentDataType.INTEGER, count);
        WeaponItems.refreshLore(meta, weapon, adapters);
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
