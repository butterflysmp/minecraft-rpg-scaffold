package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.enchant.EnchantRoll;
import io.github.butterflysmp.rpg.core.enchant.EnchantRoll.Rollable;
import io.github.butterflysmp.rpg.core.enchant.EnchantState;
import io.github.butterflysmp.rpg.core.weapon.GearClass;
import io.github.butterflysmp.rpg.core.weapon.GearDefinition;
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
 * its shield arm since Slice 2 -- and, until kits were removed in the build system's slice 2, the kit grant.
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
    public static void rollOnAcquire(ItemStack item, GearDefinition definition, AdapterContext adapters) {
        if (item == null || definition == null) return;
        GearClass gearClass = GearItems.gearClassOf(definition);
        if (gearClass == null) return;
        // Ruling A4, EXPLICIT: accessories take no part in enchanting. Without this line the roll
        // below would draw an accessory's pool as "every universal enchant" -- Unbreaking -- and
        // /rpg give would hand out an enchanted accessory.
        if (gearClass == GearClass.ACCESSORY) return;
        if (EnchantItems.isRolled(item, adapters.keys())) return;

        // THE UNIQUE SLOT (2026-11-23): the gear's element through U5's one accessor, GearDefinition.gearElement();
        // the count by Ben's cascade, which the registry carries beside the roster. Its pools are the two below.
        EnchantState rolled = EnchantRoll.roll(slotZeroKey(definition), definition.gearElement(), has(definition), gearClass, roster(adapters),
                adapters.enchants().candidates(), () -> ThreadLocalRandom.current().nextDouble());

        // *** WRITE AND RE-RENDER IN ONE editMeta, for the reason GearScoreItems.stampOnAcquire
        // states at length: a per-item value that is RENDERED has to be re-rendered wherever it is
        // WRITTEN. *** mint() already ran and read an empty container, so without the second line a
        // freshly acquired item carries rolled enchants its tooltip does not show.
        //
        // THE SIGNATURE WIDENED FROM GearClass TO GearDefinition TO MAKE THIS POSSIBLE, and it is a
        // simplification rather than a cost: all three call sites were already passing
        // GearItems.gearClassOf(definition), so deriving it here removes a caller obligation instead
        // of adding one.
        //
        // AND IT CANNOT BE LEFT TO stampOnAcquire'S RE-RENDER, which runs immediately after this at
        // every call site. That method returns EARLY for anything unscoreable -- a TOOL, or a weapon
        // declaring unscored -- and a tool still rolls from the universal pool. Relying on the
        // neighbour would have worked for three gear kinds and silently skipped the fourth.
        item.editMeta(meta -> {
            EnchantItems.write(meta, rolled, adapters.keys());
            GearItems.refreshLore(meta, definition, adapters);
        });
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
        return roster(adapters.enchants().all());
    }

    /**
     * THE IDS SLOTS 1-3 DRAW FROM, in roster order: EnchantRoll's NORMAL pool with this gear's quantities, exactly as
     * {@link #rollOnAcquire} builds it (an accessory's is empty). Read by {@code /rpg enchant pool}'s {@code Pool:} line, so
     * a gate reads the pool the roll uses rather than one random roll of it. Named {@code poolFor} until 2026-11-23.
     */
    public static List<String> normalPoolFor(GearDefinition definition, AdapterContext adapters) {
        GearClass gearClass = GearItems.gearClassOf(definition);
        if (gearClass == null) return List.of();
        return ids(EnchantRoll.normalPoolFor(has(definition), gearClass, roster(adapters)));
    }

    /** THE IDS SLOT 0 DRAWS FROM: the UNIQUE pool, by this gear's element (U5), class and quantities. */
    public static List<String> uniquePoolFor(GearDefinition definition, AdapterContext adapters) {
        GearClass gearClass = GearItems.gearClassOf(definition);
        if (gearClass == null) return List.of();
        return ids(EnchantRoll.uniquePoolFor(definition.gearElement(), has(definition), gearClass, roster(adapters)));
    }

    /**
     * THE WEAPON POOL (W2, 2026-11-23): slot 0's, the enchants naming this gear's id. {@code /rpg enchant pool}'s
     * {@code Weapon pool:} line.
     */
    public static List<String> weaponPoolFor(GearDefinition definition, AdapterContext adapters) {
        GearClass gearClass = GearItems.gearClassOf(definition);
        if (gearClass == null) return List.of();
        return ids(EnchantRoll.weaponPoolFor(slotZeroKey(definition), has(definition), gearClass, roster(adapters)));
    }

    private static List<String> ids(List<Rollable> pool) {
        return pool.stream().map(Rollable::enchantId).toList();
    }

    /**
     * THE DEV TALLY ({@code /rpg enchant rollstats <n>}, dev only): {@code n} rolls of this gear by the SAME call
     * {@link #rollOnAcquire} makes, written nowhere, and each slot's counts of 0..4 candidates beside the cascade's true
     * odds. A READING, not a pass or fail: the gate records it.
     */
    public static List<String> rollStats(GearDefinition definition, AdapterContext adapters, int n) {
        GearClass gearClass = GearItems.gearClassOf(definition);
        if (gearClass == null || gearClass == GearClass.ACCESSORY) return List.of("This gear rolls no enchants.");
        List<EnchantState> rolls = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            rolls.add(EnchantRoll.roll(slotZeroKey(definition), definition.gearElement(), has(definition), gearClass, roster(adapters),
                    adapters.enchants().candidates(), () -> ThreadLocalRandom.current().nextDouble()));
        }
        return rollStatsLines(definition.id(), definition.gearElement(), n, EnchantRoll.tally(rolls),
                adapters.enchants().candidates(), weaponPoolFor(definition, adapters).size(),
                uniquePoolFor(definition, adapters).size(), normalPoolFor(definition, adapters).size());
    }

    /** The lines, pure, so a test pins their exact shape. */
    static List<String> rollStatsLines(String id, java.util.Optional<String> element, int n, int[][] tally,
                                       io.github.butterflysmp.rpg.core.enchant.CandidateCascade cascade,
                                       int weaponPool, int uniquePool, int normalPool) {
        List<String> out = new ArrayList<>();
        out.add("ROLLSTATS " + id + " element=" + element.orElse("none") + " rolls=" + n + " weaponPool=" + weaponPool
                + " uniquePool=" + uniquePool + " normalPool=" + normalPool);
        for (int slot = 0; slot < tally.length; slot++) {
            StringBuilder b = new StringBuilder("ROLLSTATS slot " + slot + slotLabel(slot));
            for (int c = 0; c < tally[slot].length; c++) b.append(' ').append(c).append('=').append(tally[slot][c]);
            out.add(b.toString());
        }
        StringBuilder odds = new StringBuilder("ROLLSTATS true odds (before the pool cap):");
        List<Double> trueOdds = cascade.trueOdds();
        for (int i = 0; i < trueOdds.size(); i++) {
            odds.append(' ').append(cascade.tiers().get(i).candidates()).append('=')
                    .append(String.format(java.util.Locale.ROOT, "%.3f%%", 100.0 * trueOdds.get(i)));
        }
        out.add(odds.toString());
        return out;
    }

    /** {@code " (weapon):"}, {@code " (unique):"} or {@code ":"}, by {@link EnchantRoll#kindOf}. */
    static String slotLabel(int slot) {
        return switch (EnchantRoll.kindOf(slot)) {
            case WEAPON -> " (weapon):";
            case UNIQUE -> " (unique):";
            case NORMAL -> ":";
        };
    }

    /**
     * THE SLOT-0 KEY a gear rolls by (E1, the seat, 2026-11-25): a weapon by its own id (W1), an ARMOR piece by its
     * armor slot's pool ({@link EnchantRoll#armorSlotPool}), so every helmet shares one slot-0 pool. The ONE place
     * the roll, the dev pool line and the tally take it from, so the three cannot disagree.
     */
    public static String slotZeroKey(GearDefinition definition) {
        return definition instanceof io.github.butterflysmp.rpg.core.weapon.ArmorDefinition armor
                ? EnchantRoll.armorSlotPool(armor.slot()) : definition.id();
    }

    /** The roll's roster: each enchant's id, class, quantity requirement (ruling H) and element (U1). Tests build it here too. */
    public static List<Rollable> roster(java.util.Collection<EnchantDefinition> enchants) {
        List<Rollable> roster = new ArrayList<>();
        for (EnchantDefinition definition : enchants) {
            roster.add(new Rollable(definition.id(), definition.gearClass(), definition.requires(), definition.element(),
                    definition.armorSlot() != null ? EnchantRoll.armorSlotPool(definition.armorSlot()) : definition.weapon(),
                    definition.guaranteed()));
        }
        return roster;
    }

    /**
     * THE QUANTITIES THIS GEAR HAS (the seat's ruling H, 2026-11-22): a reload when reload_ticks is above 0, a quiver when
     * quiver_size is. Only a weapon can have either; every other kind has NOTHING, which no shipped requirement needs
     * because deft_hands and expanded_quiver are ranger enchants.
     */
    public static EnchantRoll.Has has(GearDefinition definition) {
        if (definition instanceof io.github.butterflysmp.rpg.core.weapon.WeaponDefinition weapon) {
            return new EnchantRoll.Has(weapon.hasReload(), weapon.hasQuiver());
        }
        return EnchantRoll.Has.NOTHING;
    }
}
