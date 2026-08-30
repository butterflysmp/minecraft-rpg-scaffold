package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.enchant.EnchantRoll;
import io.github.butterflysmp.rpg.core.enchant.EnchantRoll.Rollable;
import io.github.butterflysmp.rpg.core.enchant.EnchantState;
import io.github.butterflysmp.rpg.core.weapon.GearClass;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.content.EnchantDefinition;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Rolling a freshly acquired piece of GEAR's candidates onto the item. The Bukkit half only.
 *
 * <p>Same split as {@link EnchantItems} and {@link DamageEnchantItems}: every DECISION lives in
 * {@code core/enchant/EnchantRoll} where a unit test can reach it, and this holds the draw, the
 * registry read and the PDC write, which need a running server and are boot-witnessed instead.
 *
 * <h2>THE ONCE-PER-ITEM RULE, and the trap under it</h2>
 *
 * <b>Never call this from inside {@link WeaponItems}.</b> {@code remint} calls {@code mint}
 * (see {@code WeaponItems.remint}), so a roll placed in {@code mint} would fire on every join, every
 * {@code /rpg refresh}, every {@code /rpg enchant} sub-op and every enchant-table click -- wiping a
 * player's unlocks and replacing their candidates each time.
 *
 * <p>And the obvious guard does not save it. {@code mint} builds a FRESH {@code ItemMeta} with an
 * empty container, and {@code carryInstanceData} -- which restores {@code enchant_rolled} from the
 * old item -- runs AFTER {@code mint} returns. So {@link EnchantItems#isRolled} is always false
 * inside {@code mint}: the guard would read as present and fail open on every re-mint. That is why
 * the rule is about the CALL SITE and not about the flag.
 *
 * <p>The flag is still checked here, and it is what makes this safe to call from any future
 * acquisition path (a loot drop, a starter kit, a quest reward) without that path having to know
 * whether the item is new. {@code carryEnchants} moves both {@code enchant_data} and
 * {@code enchant_rolled} as raw bytes, so an item that has been rolled stays rolled through every
 * re-mint there will ever be.
 *
 * <p>Called at the paths that create GEAR for a player: {@code /rpg give} -- both its weapon and
 * its shield arm since Slice 2 -- and the kit grant.
 *
 * <p><b>Gear already in an inventory is never rolled retroactively.</b> A shield minted before
 * Slice 2 carries no {@code enchant_rolled} flag and nothing comes back to give it one; re-acquire
 * it. This matters at a boot gate, where an old shield shows empty slots and reads exactly like a
 * broken roll. None of these paths routes through {@code remint}.
 */
public final class EnchantRollItems {

    private EnchantRollItems() {}

    /**
     * Roll this item's candidates, if it has never been rolled.
     *
     * <p>A no-op on an item that carries the flag, whatever the flag was set by -- a previous roll,
     * or a hand-built state from {@code /rpg enchant}, which writes it through
     * {@link EnchantItems#write}. That is deliberate: a dev-assigned weapon must not be re-rolled
     * out from under the test it was built for.
     *
     * <p>The draw is HERE, at the impure call site, and the decision is {@code EnchantRoll}, in
     * core, reddening-tested against exact boundary doubles. Same split as
     * {@code WeaponDurability.applyWearOnUse} -- and {@code ThreadLocalRandom} for the reason that
     * one already records: this runs on whichever thread owns the player, many at once once Folia is
     * on, and {@code Math.random()} is a synchronized global.
     *
     * <p>Writes even when the roll came to nothing. {@link EnchantItems#write} sets the flag for an
     * empty state too, which is exactly the distinction {@code Keys.enchantRolled} exists to carry:
     * "this item's slots have been decided, and they came to nothing" must not read as "this item
     * has never been through the process", or the next acquisition path would roll it again.
     */
    public static void rollOnAcquire(ItemStack item, GearClass gearClass, AdapterContext adapters) {
        if (item == null || gearClass == null) return;
        if (EnchantItems.isRolled(item, adapters.keys())) return;

        EnchantState rolled = EnchantRoll.roll(gearClass, roster(adapters),
                () -> ThreadLocalRandom.current().nextDouble());
        item.editMeta(meta -> EnchantItems.write(meta, rolled, adapters.keys()));
    }

    /**
     * The loaded enchants, as the roll needs to see them: an id and the class each is valid on.
     *
     * <p>Built here rather than held anywhere, so the roll always draws from the content loaded NOW
     * -- a {@code --refresh-content} that adds an enchant reaches the next weapon rolled without a
     * restart. The registry is a {@code LinkedHashMap} filled from files the loader sorts, so the
     * order is deterministic and the pool is a pure function of the content.
     *
     * <p>{@code gearClass} is null for a universal enchant, which is the same null
     * {@code EnchantDefinition.isUniversal()} reports and the same one {@code DamageEnchants.Grant}
     * carries. One convention, read the same way on both sides.
     */
    private static List<Rollable> roster(AdapterContext adapters) {
        List<Rollable> roster = new ArrayList<>();
        for (EnchantDefinition definition : adapters.enchants().all()) {
            roster.add(new Rollable(definition.id(), definition.gearClass()));
        }
        return roster;
    }
}
