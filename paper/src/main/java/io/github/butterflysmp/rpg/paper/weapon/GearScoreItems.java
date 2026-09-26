package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.weapon.GearClass;
import io.github.butterflysmp.rpg.core.weapon.GearDefinition;
import io.github.butterflysmp.rpg.core.weapon.GearScore;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import io.github.butterflysmp.rpg.core.weapon.WeaponRegistry;
import io.github.butterflysmp.rpg.core.weapon.GearScoreBand;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.adapter.Keys;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Reading and writing an item's gear score, and gathering a player's six slots. The Bukkit half only.
 *
 * <p>Same split as {@link EnchantItems} and {@link EnchantRollItems}: <b>every DECISION lives in
 * {@code core/weapon/GearScore}</b> where a unit test reaches it at the two-second loop, and this
 * holds the PDC I/O, the inventory walk and the draw, which need a running server and are
 * boot-witnessed instead.
 *
 * <h2>*** PAPER GATHERS, CORE COMPUTES. THERE IS NO {@code max()} IN THIS FILE. ***</h2>
 *
 * The average reads a {@code PlayerInventory}, which {@code core} cannot see -- the same split
 * {@code CombatantSnapshot} and {@code VaultCodec} make. So this class builds two arrays of PLAIN
 * NUMBERS and hands them to {@link GearScore#sixSlots}; <b>which two hands count, and what the six
 * average to, are decided in core.</b> A {@code max()} appearing here would move a ruling out of the
 * only place it can be reddened.
 *
 * <h2>THE STAMP FIRES AT ACQUISITION, NEVER IN MINT</h2>
 *
 * <b>Never call {@link #stampOnAcquire} from inside {@code WeaponItems.mint}.</b> {@code remint} calls
 * {@code mint}, so a roll placed there would re-roll on every join, every {@code /rpg refresh}, every
 * {@code /rpg enchant} sub-op and every enchant-table click -- <b>re-banding a player's whole
 * wardrobe against their current average four times a session.</b> This is the identical trap
 * {@link EnchantRollItems} documents, and the identical remedy: the rule is about the CALL SITE.
 *
 * <p>And the obvious guard does not save it. {@code mint} builds a FRESH {@code ItemMeta} with an
 * empty container and {@code carryInstanceData} runs AFTER {@code mint} returns, so {@link #read} is
 * always absent inside {@code mint}: the guard would read as present and fail open on every re-mint.
 *
 * <p>The absence check is still made here, and it is what makes this safe to call from any future
 * acquisition path (a loot drop, a quest reward) without that path having to know whether the item is
 * new. {@code GearItems.carryInstanceData} moves the stamp on every re-mint, so a scored item stays
 * scored through every refresh there will ever be.
 *
 * <p><b>Gear already in an inventory is never scored retroactively.</b> A sword minted before this
 * slice carries no stamp and nothing comes back to give it one; it reads {@link GearScore#ABSENT} and
 * behaves exactly as it did yesterday. That matters at a boot gate, where an old sword shows no score
 * line and reads exactly like a broken stamp.
 */
public final class GearScoreItems {

    private GearScoreItems() {}

    /**
     * The nine hotbar slots. Named rather than derived from {@code getContents().length}, which is 41
     * -- the whole inventory, armour and offhand included -- and would put every stored spare weapon
     * into the hand pool.
     */
    static final int HOTBAR_SLOTS = 9;

    /**
     * The four armour slots, in the order they reach {@link GearScore#sixSlots}.
     *
     * <p>The SAME four, in the same order, as {@code DefenseModifierItems.ARMOR_SLOTS}, and named here
     * for the reason that class gives: {@code EquipmentSlot.values()} also yields {@code HAND},
     * {@code OFF_HAND}, {@code BODY} and {@code SADDLE}, and a walk over it would read a horse's
     * barding into a player's gear score the next time the enum grows.
     */
    static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    // ---------------------------------------------------------------- the PDC

    /** The score stamped on an item, or empty. The one place the tag is READ. */
    public static OptionalInt read(ItemStack item, Keys keys) {
        if (item == null || !item.hasItemMeta()) return OptionalInt.empty();
        return readMeta(item.getItemMeta(), keys);
    }

    /**
     * The score stamped on meta being built, or empty -- the {@code applyLore} read.
     *
     * <p>Separate from {@link #read} for {@code QuiverItems.loadedInMeta}'s reason: the lore builders
     * run inside {@code editMeta} and hold an {@code ItemMeta}, not an {@code ItemStack}.
     */
    public static OptionalInt readMeta(ItemMeta meta, Keys keys) {
        if (meta == null) return OptionalInt.empty();
        Integer stamped = meta.getPersistentDataContainer()
                .get(keys.gearScore, PersistentDataType.INTEGER);
        return stamped == null ? OptionalInt.empty() : OptionalInt.of(stamped);
    }

    /**
     * Stamp a score. Clamped through {@link GearScore#clamp} on the way in, so no caller can write a
     * value the readers would then have to defend against.
     */
    public static void write(ItemMeta meta, int score, Keys keys) {
        meta.getPersistentDataContainer().set(keys.gearScore, PersistentDataType.INTEGER,
                GearScore.clamp(score));
    }

    /**
     * Carry the stamp across a re-mint.
     *
     * <p><b>Losing this is a relog-to-downgrade</b> -- every scored item in the game reverting to 100
     * on the player's next login, silently, because {@link GearScore#ABSENT} is a legal reading and
     * nothing throws. Exactly the hazard {@code carryWear} names for durability and
     * {@code carryEnchants} for unlocks, and a re-mint happens on join.
     *
     * <p>Absent in, absent out: an unstamped item stays unstamped rather than being stamped with a
     * baseline, so the no-migration property survives a refresh. Writing 100 here would silently
     * convert every legacy item into an explicitly-scored one, which is a migration performed by a
     * display pass.
     */
    public static void carry(ItemMeta from, ItemMeta to, Keys keys) {
        Integer score = from.getPersistentDataContainer()
                .get(keys.gearScore, PersistentDataType.INTEGER);
        if (score != null) {
            to.getPersistentDataContainer().set(keys.gearScore, PersistentDataType.INTEGER, score);
        }
    }

    // ---------------------------------------------------------------- the roll

    /**
     * Roll and stamp a freshly acquired item's score, if it is scoreable and carries none.
     *
     * <p>A no-op on an item that already carries a stamp, whatever wrote it -- a previous acquisition
     * or {@code /rpg gearscore set}. That is deliberate, and it is the same call
     * {@link EnchantRollItems#rollOnAcquire} makes for the rolled flag: <b>a dev-assigned score must
     * not be re-rolled out from under the gate row it was staged for.</b>
     *
     * <p>A no-op on a TOOL, and on a definition that DECLARES itself unscored, through
     * {@link GearScore#carriesScore} -- the composed door, which asks both. The kind comes from
     * {@code GearItems.gearClassOf}, so the caller passes the definition's own class rather than
     * guessing from the material.
     *
     * <p>The draw is HERE, at the impure call site, and the band is {@code GearScore.roll} in core.
     * {@code ThreadLocalRandom} for {@code EnchantRollItems}' reason: this runs on whichever thread
     * owns the player, many at once once Folia is on, and {@code Math.random()} is a synchronized
     * global.
     *
     * <p><b>The band is centred on the player's average AS IT IS NOW</b>, read one line above the
     * roll -- so an item acquired second in one grant bands against an average the first item has
     * already moved. That is Ben's ruling read literally ("banded on the player's CURRENT AVERAGE")
     * and it is the reason this takes a live {@code Player} rather than a precomputed figure.
     */
    public static void stampOnAcquire(ItemStack item, GearDefinition definition, Player player,
                                      AdapterContext adapters) {
        if (item == null || player == null) return;
        // *** THE DEFINITION, NOT A GearClass AND NOT A BOOLEAN. ***
        // Both facts are derived from the ONE object here, so no caller can hand this a naked
        // `false` for the instance half. A boolean parameter would be defaulted to false by any
        // caller that did not know about the flag -- and that caller is the one this refusal exists
        // for. There is no type that makes the hazard impossible, so the shape removes the argument
        // instead.
        if (!GearScore.carriesScore(GearItems.gearClassOf(definition), declaredUnscored(definition))) return;
        if (read(item, adapters.keys()).isPresent()) return;

        int rolled = GearScore.roll(averageOf(player, adapters.keys(), adapters.weapons()),
                GearScoreBand.SPREAD, GearScoreBand.SKEW,
                ThreadLocalRandom.current().nextDouble());
        // *** WRITE AND RE-RENDER IN ONE editMeta. THE PAIRING IS THE POINT. ***
        //
        // mint() already ran and applyLore read a STILL-EMPTY container, so without the second line
        // a freshly given, granted or crafted item carries a real score in its PDC and shows NO
        // "Gear Score:" line until its next re-mint (a join refresh, /rpg refresh, the enchant table
        // or the grindstone). That shipped in slice 12 and survived 12b.
        //
        // IT ALSO MADE A GATE ROW UNFALSIFIABLE. GATE-gearscore.md R12(i) predicted that a fresh
        // volley_stone shows no score line, to witness the unscored exclusion -- and NO fresh weapon
        // of any kind showed one, so the row passed on a build with no exclusion at all. Its reading
        // is withdrawn; see that file's ONE READING WITHDRAWN section.
        //
        // GearItems.refreshLore, not remint: remint returns a FRESH stack and this method holds an
        // ItemStack its caller is about to put in an inventory, so a returned copy would be dropped
        // silently. The dispatch is exhaustive, so a fifth gear kind cannot skip this.
        item.editMeta(meta -> {
            write(meta, rolled, adapters.keys());
            GearItems.refreshLore(meta, definition, adapters);
        });
    }

    // ---------------------------------------------------------------- the average

    /**
     * A player's average gear score: four armour slots plus their two best hands, over six.
     *
     * <p>Two walks, both read-only, then one call into core. The arrays are plain {@code int[]} for
     * the reason the class javadoc gives -- this is the seam, and it is a list of numbers.
     *
     * <p>Returns {@code 0} for a player with no equipment at all, which is the honest answer and not
     * an error: {@link GearScore#averageOf} counts an empty slot as zero, always, and a naked player
     * genuinely averages nothing. A new player holding one baseline sword averages <b>16</b> -- below
     * the minimum any item can roll, so their first drops sit at 100 until their slots fill. Ben has
     * seen that and accepted it as the pressure to gear every slot.
     */
    public static int averageOf(Player player, Keys keys, WeaponRegistry weapons) {
        return GearScore.averageOf(GearScore.sixSlots(armorScores(player, keys, weapons),
                handScores(player, keys, weapons)));
    }

    /**
     * The four armour slots' scores, in {@link #ARMOR_SLOTS} order, {@link GearScore#EMPTY} for a slot
     * holding nothing scoreable.
     *
     * <p>A slot holding plain vanilla armour reads {@link GearScore#EMPTY} rather than
     * {@link GearScore#ABSENT}, and the distinction is the one worth being careful about:
     * <b>{@code ABSENT} is what OUR gear reads when it predates the stamp; {@code EMPTY} is what a
     * slot reads when it holds nothing of ours.</b> A vanilla chestplate still contributes its full
     * Defense through {@code DefenseModifierItems} -- it is not inert -- but it has never been on this
     * ladder, and reading it as a baseline item would put every player who ever equipped vanilla
     * diamond at an average of 100 before acquiring a single scored piece.
     */
    static int[] armorScores(Player player, Keys keys, WeaponRegistry weapons) {
        int[] scores = new int[GearScore.ARMOR_SLOTS];
        EntityEquipment equipment = player.getEquipment();
        if (equipment == null) return scores;
        for (int i = 0; i < ARMOR_SLOTS.length; i++) {
            scores[i] = candidateScore(equipment.getItem(ARMOR_SLOTS[i]), keys, weapons)
                    .orElse(GearScore.EMPTY);
        }
        return scores;
    }

    /**
     * Every scoreable item in the HOTBAR AND THE OFFHAND, as one pool.
     *
     * <p><b>ONE POOL, NOT A HOTBAR PLUS AN OFFHAND SLOT.</b> Ben's ruling: the two highest-scored
     * items across hotbar and offhand TAKEN TOGETHER, the offhand in that pool rather than a slot of
     * its own. So a player with their two best weapons in the hotbar and a shield in the offhand
     * counts the two weapons, and the shield does not get a guaranteed seat.
     *
     * <p>Unscoreable occupants -- a tool, a stack of dirt, a vanilla sword -- are simply not
     * candidates, so they cannot displace a real weapon from the top two. That is the pickaxe ruling
     * enforced at the gather; {@link GearScore#carriesScore} enforces it at the stamp, and the two
     * together are why a pickaxe can neither carry a score nor be read as having one.
     *
     * <p>Length is whatever the inventory holds, including zero. {@link GearScore#topTwo} pads, so a
     * player holding one weapon still averages over six slots rather than five.
     */
    static int[] handScores(Player player, Keys keys, WeaponRegistry weapons) {
        PlayerInventory inventory = player.getInventory();
        List<Integer> pool = new ArrayList<>();
        for (int slot = 0; slot < HOTBAR_SLOTS; slot++) {
            candidateScore(inventory.getItem(slot), keys, weapons).ifPresent(pool::add);
        }
        candidateScore(inventory.getItemInOffHand(), keys, weapons).ifPresent(pool::add);

        int[] scores = new int[pool.size()];
        for (int i = 0; i < scores.length; i++) scores[i] = pool.get(i);
        return scores;
    }

    /**
     * This item's score as a CANDIDATE for the average, or empty if it is not on the ladder at all.
     *
     * <h2>*** THE THREE TAGS READ HERE ARE THE SCOREABLE KINDS. {@code toolId} IS NOT ONE. ***</h2>
     *
     * A weapon, a shield or a piece of armour of ours is a candidate; <b>a TOOL is not, and neither is
     * anything untagged.</b> Ben's ruling -- a pickaxe must never become one of the top two and raise
     * a drop level without contributing anything to a fight -- and {@code toolId}'s absence from this
     * method is the whole of its enforcement on the read side.
     *
     * <p><b>Adding {@code keys.toolId} here would admit every pickaxe in the game, silently.</b> No
     * unit test can see it (this needs a live {@code Player}), so it is asserted as a source scan by
     * {@code GearScoreWiringSignatureTest} and watched by a gate row.
     *
     * <p>A tagged item with no stamp reads {@link GearScore#ABSENT} -- it is our gear, minted before
     * this slice, sitting at the bottom of the ladder. An untagged item reads empty, which the armour
     * walk turns into {@link GearScore#EMPTY} and the hand walk drops entirely. Those two readings are
     * 100 and 0 and must never be confused; {@link #armorScores} records why.
     */
    static OptionalInt candidateScore(ItemStack item, Keys keys, WeaponRegistry weapons) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return OptionalInt.empty();
        // Ruling A4, EXPLICIT: an accessory is ours and is not a candidate. The allowlist below would
        // leave it out anyway, as it leaves out a tool -- this line makes that a decision here rather
        // than an absence (GearScoreWiringSignatureTest pins that it refuses).
        if (GearItems.idOf(item, keys.accessoryId).isPresent()) return OptionalInt.empty();
        boolean scoreable = GearItems.idOf(item, keys.weaponId).isPresent()
                || GearItems.idOf(item, keys.shieldId).isPresent()
                || GearItems.idOf(item, keys.armorId).isPresent();
        if (!scoreable) return OptionalInt.empty();
        if (declaredUnscored(item, keys, weapons)) return OptionalInt.empty();
        return OptionalInt.of(GearScore.orAbsent(read(item, keys)));
    }

    /**
     * Does this DEFINITION declare that it carries no score? The primitive both doors resolve to.
     *
     * <p><b>Only a {@code WeaponDefinition} can declare it</b> -- armour and shields have no such
     * field, and tools are refused a rung up by {@link GearScore#carriesScore}'s kind-level half. A
     * definition of any other type is therefore {@code false} by pattern-match rather than by a
     * default, which is why widening the flag to {@code GearDefinition} later would be a visible
     * compile error at four implementors instead of a silent behaviour change here.
     *
     * <p>A {@code null} definition is {@code false}: not declared, so not excluded. Same direction as
     * the missing-definition rule below -- <b>only a positive declaration removes anything.</b>
     */
    private static boolean declaredUnscored(GearDefinition definition) {
        return definition instanceof WeaponDefinition weapon && weapon.unscored();
    }

    /**
     * Does this item's own DEFINITION declare that it carries no score? The instance-level half of
     * {@link GearScore#carriesScore}, resolved from the weapon registry.
     *
     * <h2>*** A DEFINITION THAT CANNOT BE FOUND MEANS SCORED. THE {@code orElse(false)} IS THE RULING. ***</h2>
     *
     * <p><b>That line looks exactly like the bare-{@code false} bug it is the opposite of</b>, which
     * is why it is written out rather than left to read as an oversight. A weapon whose content file
     * was removed or renamed keeps the score already in its PDC: <b>only an explicit declaration
     * removes anything.</b>
     *
     * <p>The other default is SILENT and worse in every way -- the item would drop out of the
     * average, the player's drop band would fall, and nothing would report it. An over-inclusive
     * answer costs a score that should not be there and is visible on a tooltip; an over-exclusive
     * one costs a band nobody can see.
     *
     * <p><b>Only WEAPONS can declare it.</b> Armour and shields have no such flag -- shields score by
     * Ben's explicit ruling, and tools are refused a rung up by {@link GearScore#carriesScore}'s
     * kind-level half. So a shield or an armour id resolves to {@code false} here by never matching
     * the weapon lookup at all, which is correct rather than incidental.
     *
     * <p><b>Declared BELOW {@code candidateScore} on purpose, and brace-free above.</b>
     * {@code GearScoreWiringSignatureTest} extracts that method's body by scanning to the first line
     * that trims to {@code "}"}; a braced block inside it would truncate the window and make the
     * {@code toolId} ban pass VACUOUSLY. The guard clause is the shape the three lines above it
     * already use -- this is the surrounding idiom, not a concession to the scanner -- but the
     * HELPER's placement is, and it is recorded here so the next reader knows the constraint exists.
     */
    private static boolean declaredUnscored(ItemStack item, Keys keys, WeaponRegistry weapons) {
        if (weapons == null) return false;
        return GearItems.idOf(item, keys.weaponId)
                .flatMap(weapons::find)
                .map(WeaponDefinition::unscored)
                .orElse(false);
    }

    // ---------------------------------------------------------------- the held weapon

    /**
     * The score of the item in the player's MAIN HAND, or {@link GearScore#ABSENT} when it carries
     * none.
     *
     * <p>Returns a usable score for ANY main-hand item rather than an optional, because its one caller
     * -- {@code WeaponAttackItems.desiredAttackModifiers} -- has already established that the hand
     * holds one of our weapons before it asks. <b>A weapon whose stamp is missing must scale by 1, not
     * by 0</b>, which is what {@link GearScore#ABSENT} delivers.
     *
     * <p>Not {@link #candidateScore}: that method answers "does this belong in the average", and a
     * tool in the main hand is correctly refused there and would be wrongly refused here -- a tool
     * deals no {@code attack_damage} anyway, so there is nothing for its refusal to protect.
     */
    public static int heldScore(Player player, Keys keys, WeaponRegistry weapons) {
        ItemStack held = player.getInventory().getItemInMainHand();
        // *** THE DOOR IS BEFORE THE READ, NOT A FILTER AFTER IT, AND THAT IS FORCED. ***
        // GearScore.orAbsent resolves an absent stamp to ABSENT (100), so by the time this method has
        // an int it has COLLAPSED "declared unscored" with "carries no stamp" -- and the exclusion
        // lives on exactly that difference. A volley_stone minted since slice 12 carries a real score
        // in its PDC; filtering the returned value could not tell it from an ordinary scored weapon.
        if (declaredUnscored(held, keys, weapons)) return GearScore.BASELINE;
        return GearScore.orAbsent(read(held, keys));
    }
}
