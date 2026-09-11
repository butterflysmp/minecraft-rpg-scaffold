package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.combat.QuiverSize;
import io.github.butterflysmp.rpg.core.weapon.Quiver;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.adapter.Keys;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.OptionalInt;
import java.util.UUID;

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
        // THE CAPACITY IS STAMPED BESIDE THE COUNT, AND AT MINT IT IS THE AUTHORED ONE. Mint has no
        // player -- several mint paths are previews and icons that describe a WEAPON rather than a
        // held item -- and authored is the right answer for all of them. It is also correct for a
        // wielder whose gear resolves more: DESIGN-stat-engine's ruled semantics make a capacity
        // increase HEADROOM, so a fresh quiver full to its authored size is exactly a count sitting
        // below capacity with room to reload into. No player needed, and no compromise.
        meta.getPersistentDataContainer().set(
                keys.quiverCapacity, PersistentDataType.INTEGER, weapon.quiverSize());
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
     *
     * <p><b>AND NOTE HOW {@code remint} WOULD HAVE BEHAVED: it stamps the magazine full and then
     * carries the old count back over it, so the tooltip would have come out CORRECT.</b> It would
     * have worked by accident — the right number arrived through two writes that happen to cancel,
     * not through anything that says a count must be rendered. A fix that works for a reason nobody
     * wrote down is the kind that a later change to either write silently breaks.
     */
    public static void setLoaded(ItemMeta meta, WeaponDefinition weapon, AdapterContext adapters,
                                 UUID wielder, int count) {
        int capacity = resolveCapacity(weapon, adapters, wielder);

        // AND THE CLAMP IS HERE, WHICH IS WHY NO RECONCILE-LOOP CLAMP IS NEEDED. Capacity cannot
        // change except at a write, and at that write this method holds both numbers -- so
        // DESIGN-stat-engine's ruled decrease-clamps semantics are one call, on the value that just
        // resolved. A clamp in the stat-reconcile loop would be an in-play write with no render,
        // which is boot row V1's defect in a new location.
        int stored = Quiver.clamp(count, capacity);

        meta.getPersistentDataContainer().set(
                adapters.keys().quiverLoaded, PersistentDataType.INTEGER, stored);
        meta.getPersistentDataContainer().set(
                adapters.keys().quiverCapacity, PersistentDataType.INTEGER, capacity);
        WeaponItems.refreshLore(meta, weapon, adapters);
    }

    /**
     * Refill this item to a full magazine at the wielder's CURRENT capacity, and re-render.
     *
     * <p>The reload's completion and the unstamped repair both want "as many rounds as this wielder
     * can hold", and neither should have to know how that number is reached. Before this existed
     * both computed {@code Quiver.reload(weapon.quiverSize())} at the call site — <b>two more
     * authored reads that commit 3 would have had to find and convert</b>, which is exactly the
     * shape that made "two call sites" false the first time it was claimed.
     */
    public static void setFull(ItemMeta meta, WeaponDefinition weapon, AdapterContext adapters,
                                UUID wielder) {
        setLoaded(meta, weapon, adapters, wielder, resolveCapacity(weapon, adapters, wielder));
    }

    /**
     * THE ONE PLACE THE WIELDER'S CAPACITY IS RESOLVED FOR A WRITE.
     *
     * <p><b>The claim held.</b> Commit 1e's javadoc said commit 3 would swap this one expression for
     * the stat read and nothing else in the codebase would change. It swapped one expression --
     * {@code weapon.quiverSize()} became {@code QuiverSize.resolve(weapon.quiverSize(), ...)} -- and
     * what else changed was a PARAMETER, not another resolver: this method, {@link #setLoaded} and
     * {@link #setFull} now take the wielder, because a stat belongs to somebody. Everything that
     * READS a capacity still reads the stamp this produces, never the stat.
     *
     * <p><b>Why a {@code UUID} and not a {@code Player}:</b> it is all {@code CombatantStats} wants,
     * it cannot be dereferenced for anything else by accident, and it does not tempt a later edit
     * into reading equipment here -- which would be a second scanner competing with the reconcile
     * loop's. The arithmetic is {@code core}'s and is unit-tested there; this method is the READ.
     *
     * <p><b>An untracked wielder resolves to the authored capacity</b>, because
     * {@code CombatantStats.quiverSizeBonusValue} returns {@code 0.0} rather than throwing and
     * {@code QuiverSize.resolve(authored, 0.0)} is exactly {@code authored}. That is the right answer
     * and not a fallback: a weapon nobody is tracked for holds what it declares.
     */
    private static int resolveCapacity(WeaponDefinition weapon, AdapterContext adapters,
                                       UUID wielder) {
        return QuiverSize.resolve(weapon.quiverSize(),
                adapters.stats().quiverSizeBonusValue(wielder));
    }

    /**
     * The capacity this stack was last packed at, or empty when it carries no stamp.
     *
     * <p>Empty is <b>not</b> a defect here, unlike an absent count -- see {@code Keys.quiverCapacity}.
     * Resolve it through {@code QuiverState.capacityOf}, which is the single place the
     * stamp-then-authored ordering lives; do not write {@code orElse(weapon.quiverSize())} inline.
     */
    public static OptionalInt capacityInMeta(ItemMeta meta, Keys keys) {
        if (meta == null) return OptionalInt.empty();
        Integer stored = meta.getPersistentDataContainer()
                .get(keys.quiverCapacity, PersistentDataType.INTEGER);
        return stored == null ? OptionalInt.empty() : OptionalInt.of(stored);
    }

    /** {@link #capacityInMeta} against a whole stack. */
    public static OptionalInt capacityIn(ItemStack item, Keys keys) {
        if (item == null || !item.hasItemMeta()) return OptionalInt.empty();
        return capacityInMeta(item.getItemMeta(), keys);
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

        // THE CAPACITY MOVES INDEPENDENTLY OF THE COUNT, and that is deliberate: an item minted
        // before this key existed carries a count and no capacity, and must keep doing so rather
        // than being handed a fabricated one. QuiverState.capacityOf resolves that absence to the
        // authored value on read, which is the migration path.
        Integer capacity = source.get(keys.quiverCapacity, PersistentDataType.INTEGER);
        if (capacity != null) target.set(keys.quiverCapacity, PersistentDataType.INTEGER, capacity);

        Long startedAt = source.get(keys.quiverReloadStartedAt, PersistentDataType.LONG);
        Long completesAt = source.get(keys.quiverReloadCompletesAt, PersistentDataType.LONG);
        if (startedAt != null && completesAt != null) {
            target.set(keys.quiverReloadStartedAt, PersistentDataType.LONG, startedAt);
            target.set(keys.quiverReloadCompletesAt, PersistentDataType.LONG, completesAt);
        }
    }
}
