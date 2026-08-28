package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.enchant.EnchantState;
import io.github.butterflysmp.rpg.core.weapon.Durability;
import io.github.butterflysmp.rpg.core.weapon.Rarity;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.adapter.Keys;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Minting a weapon item, and recognising one. An item is one of ours IFF it carries the
 * weapon_id PDC tag -- no tag, not a weapon, and the system leaves it untouched.
 *
 * This is the only place that reads or writes the weapon_id tag. The read is on the hot
 * path: the swing packet fires for every player on every click in 1b, so heldWeaponId's
 * FIRST act is the cheapest possible reject -- no meta means not ours -- before any
 * lookup or allocation.
 */
public final class WeaponItems {

    private WeaponItems() {}

    /**
     * The vanilla melee a held item deals is a SEPARATE damage path from a weapon's trigger.
     * A left-click swing (1b) would otherwise land both -- the iron sword's vanilla ~6 AND
     * the trigger's content damage -- double-hitting. We cancel the vanilla path so the
     * trigger's number stays authoritative.
     *
     * These two are the single source of truth for that decision, kept server-free so the
     * choice is unit-testable without constructing an ItemStack (which needs a running
     * server). mint() DERIVES the Bukkit Attribute from ATTACK_DAMAGE_ATTRIBUTE below, so
     * the constant the test asserts is the same one that reaches the item -- they cannot
     * drift. The load-bearing property: this touches ONLY vanilla attack damage; the
     * weapon's own damage flows through EffectSpec.Damage -> CombatantHandle.applyDamage,
     * which never goes through an attribute (see WeaponServiceTest, which damages with no
     * item at all).
     */
    public static final String ATTACK_DAMAGE_ATTRIBUTE = "attack_damage";

    /**
     * The attack-SPEED attribute, pinned beside the damage one for a vanilla-driven melee weapon.
     *
     * MEASURED on the 2026-08-28 boot, not assumed: a plain iron sword read attackSpeed 1.6000,
     * while a minted ironblade -- the same material -- read 4.0000. Setting ANY explicit modifier
     * replaces the item's whole default block, so pinning attack damage alone silently discards the
     * sword's native speed and drops the player to the base 4.0. That is a 5-tick charge period
     * against a 10-tick i-frame window, which would leave every allowed swing already fully charged
     * and the charge curve dead code. Hence: pin both, or neither.
     */
    public static final String ATTACK_SPEED_ATTRIBUTE = "attack_speed";

    /** A player's base attack_damage is 1.0, so -1.0 brings a held swing to a flat 0. */
    public static final double VANILLA_MELEE_SUPPRESSION = -1.0;

    /** The player's base values, which every ADD_NUMBER modifier below is expressed relative to. */
    public static final double VANILLA_BASE_ATTACK_DAMAGE = 1.0;
    public static final double VANILLA_BASE_ATTACK_SPEED = 4.0;

    /** Ticks per second, converting a trigger's authored cooldown into an attack-speed value. */
    public static final double TICKS_PER_SECOND = 20.0;

    /**
     * The attack-damage modifier a vanilla-driven melee weapon pins: enough to bring the total to
     * the weapon's declared attack damage.
     *
     * The VALUE is cosmetic -- the rider tokens the vanilla hit to 0.01, and the ATTACK_DAMAGE stat
     * is what actually damages -- but it must be strictly POSITIVE, because vanilla skips its entire
     * attack path when attack damage and enchantment bonus are both zero. That is the whole of
     * Finding 1: the old suppressor brought the total to 0, so no EntityDamageByEntityEvent ever
     * fired for a weapon-holder and the melee rider was dead code. Pinning the identity number keeps
     * the hidden attribute honest should anyone ever reveal it.
     */
    public static double attackDamageModifier(double declaredAttackDamage) {
        return declaredAttackDamage - VANILLA_BASE_ATTACK_DAMAGE;
    }

    /**
     * The attack-speed modifier, derived from the trigger's authored cooldown_ticks so the vanilla
     * charge period and the tooltip's "Attack Speed" line cannot disagree: 10 ticks is 2.0/s, which
     * is exactly what WeaponLoreLines renders for the same trigger.
     *
     * A declared cooldown of 0 means "ungated" and has no cadence to express, so it pins nothing and
     * leaves the player base -- the same reading AttackSpeed.effectiveCooldownTicks gives it.
     */
    public static OptionalDouble attackSpeedModifier(int cooldownTicks) {
        if (cooldownTicks <= 0) return OptionalDouble.empty();
        return OptionalDouble.of(TICKS_PER_SECOND / cooldownTicks - VANILLA_BASE_ATTACK_SPEED);
    }

    /**
     * The item a weapon is carried in. Its display name is coloured by RARITY, unconditionally
     * (see {@link #displayName}), it carries weapon_id in its PDC -- the whole of its identity --
     * and it carries the vanilla attribute block its melee behaviour needs.
     *
     * Takes the whole AdapterContext rather than just Keys because the lore needs the element
     * registry to colour a weapon's element from that element's own content.
     */
    public static ItemStack mint(WeaponDefinition weapon, AdapterContext adapters) {
        Keys keys = adapters.keys();
        ItemStack item = new ItemStack(materialOf(weapon.material()));
        Attribute attackDamage = Registry.ATTRIBUTE.getOrThrow(
                NamespacedKey.minecraft(ATTACK_DAMAGE_ATTRIBUTE));
        Attribute attackSpeed = Registry.ATTRIBUTE.getOrThrow(
                NamespacedKey.minecraft(ATTACK_SPEED_ATTRIBUTE));

        // Does a VANILLA crosshair attack deliver this weapon's basic hit? Resolved through the one
        // predicate the swing path and the rider also read, so an item's attributes and its hit
        // routing cannot drift apart. attackDamage > 0 is part of the question: a melee trigger on a
        // weapon declaring 0 deals nothing anyway (EffectApplier's amount>0 guard), so there is
        // nothing to let vanilla through for.
        boolean vanillaDrivenMelee =
                weapon.vanillaMeleeTrigger().isPresent() && weapon.attackDamage() > 0;

        item.editMeta(meta -> {
            meta.displayName(displayName(weapon.displayName(), weapon.rarity()));
            meta.getPersistentDataContainer().set(keys.weaponId, PersistentDataType.STRING, weapon.id());
            // A weapon is a SINGLE item, always. Two freshly minted staffs are byte-identical --
            // ember_staff mints on a blaze_rod and ability_stone on an amethyst_shard, both
            // stackable -- so without this, /rpg give ember_staff twice produces a stack of two
            // and every per-item thing we do becomes ambiguous: one enchant write would edit both,
            // and a re-mint (which returns a fresh stack of one) would silently collapse it.
            //
            // Fixed at the SOURCE rather than only guarded at the enchant table, because durability,
            // enchants and instance data are all per-item and the table is not the only thing that
            // will ever read them. remint() calls mint(), so it inherits this.
            meta.setMaxStackSize(1);

            if (vanillaDrivenMelee) {
                // Let vanilla's attack RUN -- it is what picks the victim now -- and give its
                // attack-strength meter a period matching the authored cadence.
                meta.addAttributeModifier(attackDamage, new AttributeModifier(
                        keys.meleeSuppressor, attackDamageModifier(weapon.attackDamage()),
                        AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));
                OptionalDouble speed = weapon.vanillaMeleeTrigger()
                        .map(trigger -> attackSpeedModifier(trigger.cooldownTicks()))
                        .orElse(OptionalDouble.empty());
                if (speed.isPresent()) {
                    meta.addAttributeModifier(attackSpeed, new AttributeModifier(
                            keys.meleeSpeedPin, speed.getAsDouble(),
                            AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));
                }
            } else {
                // No melee hit of ours to deliver (ember_staff, ability_stone, hunters_bow), so
                // vanilla's swing stays suppressed to a flat 0 and a staff still cannot melee.
                meta.addAttributeModifier(attackDamage, new AttributeModifier(
                        keys.meleeSuppressor, VANILLA_MELEE_SUPPRESSION,
                        AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));
            }

            // The custom lore block IS the stat display. Hide vanilla's, or the tooltip carries two
            // sets of numbers saying different things -- the pinned attack damage is deliberately
            // cosmetic, and the pinned speed duplicates the lore's own Attack Speed line.
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

            // Derived stats + authored flavour, plus any enchant block the item's own state calls
            // for. Purely additive; the block above is untouched.
            applyLore(meta, weapon, adapters);
        });
        return item;
    }

    /**
     * Write this item's whole lore: current content, plus the enchant block THIS ITEM's own state
     * calls for.
     *
     * <p><b>REBUILDS rather than appends</b>, and that is the point. Calling it twice produces the
     * same lore, so no edit path can double the enchant block or strand a stale line, and it needs
     * no block-boundary detection to promise that -- the base is regenerated from the definition
     * every time, so it is never lore that already contains an enchant block.
     *
     * <p>It is also where the ordering problem dies. {@code mint} builds display from current
     * content and cannot know this item's enchants; {@code remint} carries the raw blob across
     * FIRST and calls this after, so lore is never built before the state it describes has arrived.
     * Getting this backwards -- the obvious arrangement, where remint prepends onto whatever mint
     * produced -- is fine today and doubles the block the moment the roster pass gives {@code mint}
     * a roll of its own to render.
     *
     * <p>At a fresh mint the container is empty, {@code lines} is empty, and {@code applied}
     * returns the base list untouched. So this call is live and exercised from day one rather than
     * commented, and the roster pass's mint-time roll is a write ABOVE it and nothing else.
     */
    private static void applyLore(ItemMeta meta, WeaponDefinition weapon, AdapterContext adapters) {
        List<Component> base = WeaponLore.build(weapon, adapters.elements());
        EnchantState state = EnchantItems.read(meta, adapters.keys());
        meta.lore(EnchantLore.applied(base, EnchantLore.lines(state, adapters.enchants())));

        // THE GLINT. Our enchants live in the PDC, not in the item's vanilla enchantment list, so
        // vanilla has nothing to shimmer over and an enchanted weapon looked exactly like a plain
        // one. Driven by state.effective() -- the SAME list the lore block above renders -- so the
        // shimmer and the enchant lines can never disagree about whether this item is enchanted.
        //
        // Set explicitly in BOTH directions, unlike EnchantMenu's icons, which set true and leave
        // the false case unset on purpose. Not because a stale glint is known to survive a re-mint
        // -- carryInstanceData moves weapon_id, wear and the enchant blob, and pointedly not this --
        // but because an explicit false makes the glint a pure function of the enchant state rather
        // than of how the meta happened to arrive. applyLore already runs twice per remint (once
        // against an empty container, once against the carried state), and it is the kind of call
        // order that gets rearranged later by someone who does not know it is load-bearing.
        meta.setEnchantmentGlintOverride(!state.effective().isEmpty());
    }

    /**
     * A weapon's item name: the authored text, in its RARITY's colour, always. Rarity owns this
     * colour outright -- an authored colour in the content no longer wins, because "the name is
     * the tier" is only readable if it is true every time. It was previously
     * {@code colorIfAbsent(...)}, under which Hunter's Bow (authored gold, rarity uncommon) and
     * Ember Staff (authored gold, rarity rare) both rendered gold, and the two weapons that DID
     * look right did so by coincidence.
     *
     * Extracted from mint() so it is unit-testable: {@code new ItemStack(...)} needs a running
     * server, but this is pure Adventure. Same reason the suppressor constants above are constants.
     *
     * The plain-text round trip is not a detour: it is what makes this hold for EVERY authored
     * string, not just the easy ones. Where a tag wraps the whole name ("<gold>Hunter's Bow</gold>")
     * MiniMessage compacts the colour onto the root, and a plain {@code .color(rarity)} would in
     * fact override it. But a PARTIAL tag ("Hunter's <gold>Bow</gold>"), a nested tag, or a gradient
     * puts colour on CHILD components that a root-level {@code .color} does not touch -- so that
     * approach would work on today's content and silently fail the first time someone authored a
     * name with a coloured word in it. Measured, not reasoned: reverting this to {@code .color()}
     * leaves the two whole-tag tests green and fails only the partial-tag one, with gold surviving
     * on a child. Flattening to text and rebuilding cannot be defeated that way.
     *
     * The cost, accepted deliberately: this also drops authored bold/underline/gradient. No shipped
     * weapon uses one, and styled names would need their own mechanism regardless.
     */
    public static Component displayName(String authoredName, Rarity rarity) {
        String plain = PlainTextComponentSerializer.plainText()
                .serialize(MiniMessage.miniMessage().deserialize(authoredName));
        return Component.text(plain, RarityColors.of(rarity))
                // Item names render italic by default; a weapon name should read plainly.
                .decoration(TextDecoration.ITALIC, false);
    }

    /**
     * Resolve a weapon's material string ("bow", "iron_sword") to a Bukkit Material. An
     * unknown material falls back to a sword rather than crashing the give -- a wrong item
     * is visible in-game, where it can be fixed, and never blocks a boot.
     */
    private static Material materialOf(String material) {
        Material resolved = Material.matchMaterial(material);
        return resolved != null ? resolved : Material.IRON_SWORD;
    }

    /**
     * The weapon id of the player's main-hand item, if it is one of ours.
     *
     * Delegates to {@link #weaponId} so there is still exactly ONE place that reads the tag. The
     * untagged fast-path reject lives there and is unchanged: an empty hand, a dirt block, a
     * vanilla sword all lack item meta and return empty having cost nothing. This is the shape
     * 1b's packet listener calls, so it must stay allocation-free on a miss.
     */
    public static Optional<String> heldWeaponId(Player player, Keys keys) {
        return weaponId(player.getInventory().getItemInMainHand(), keys);
    }

    /**
     * The weapon id of ANY item, if it is one of ours -- the same read {@link #heldWeaponId} does,
     * widened from the main hand to an arbitrary stack so the Lore Refresher can walk every
     * inventory slot.
     *
     * The null guard is the one addition. {@code getItemInMainHand()} never returns null, but
     * {@code Inventory#getContents()} is full of nulls for empty slots, and an inventory is mostly
     * empty slots. Same shape as the sibling item readers ({@code ClassDamageModifierItems.grantOf},
     * {@code AttackSpeedModifierItems.boostAmount}).
     */
    public static Optional<String> weaponId(ItemStack item, Keys keys) {
        if (item == null || !item.hasItemMeta()) return Optional.empty();
        return Optional.ofNullable(item.getItemMeta().getPersistentDataContainer()
                .get(keys.weaponId, PersistentDataType.STRING));
    }

    /**
     * Rebuild an existing weapon item's DISPLAY from the definition loaded NOW, keeping everything
     * the item itself earned. This is the Lore Refresher's mechanism.
     *
     * A full re-mint rather than a lore patch, on purpose: MATERIAL is baked at mint too, so a
     * weapon that changed from iron_sword to diamond_sword in content cannot be brought up to date
     * by rewriting meta on the old stack -- it is a different item. Minting fresh and carrying the
     * instance data over is the only shape that covers every baked field, which is exactly why the
     * carry-forward has to be explicit rather than incidental.
     */
    public static ItemStack remint(ItemStack old, WeaponDefinition current, AdapterContext adapters) {
        ItemStack fresh = mint(current, adapters);
        ItemMeta oldMeta = old.getItemMeta();
        if (oldMeta == null) return fresh;   // no meta means no tag; the caller would not have got here
        fresh.editMeta(meta -> {
            carryInstanceData(oldMeta, meta, adapters.keys(), fresh.getType());
            // ...and only NOW render the enchant block, from the state that just arrived. mint()
            // above already ran applyLore against an empty container, which was a no-op; this is
            // the call that can actually see this item's enchants. Order is load-bearing.
            applyLore(meta, current, adapters);
        });
        return fresh;
    }

    /**
     * Copy the old item's INSTANCE data onto the freshly minted one -- the explicit half of the
     * refresh contract, and the half that decides whether this pass needs rewriting later.
     *
     * Everything NOT copied here is DISPLAY, and is deliberately rebuilt from current content. This
     * roster is what separates the two, so a future rarity or enchant roll is one more line in this
     * method and no change anywhere else.
     *
     * Today it is two things:
     *
     *  - the weapon id, which mint() already regenerated from the current definition's id -- the
     *    same value, since that is the id we looked the definition up BY. Copied anyway, so the
     *    step is a real mechanism rather than a comment describing one;
     *  - the item's accumulated wear, which mint() cannot know. This is what keeps the refresh
     *    strictly display-only. Resetting it would silently repair every weapon on every login:
     *    a gameplay change smuggled into a presentation pass, and a relog-to-repair exploit.
     *    Whether custom weapons should wear at all is a separate, deferred decision -- if they
     *    later mint unbreakable, this carry-forward quietly becomes a no-op.
     *
     * ...and, since Enchant Pass 1, a third: the item's enchant state. That is the line this
     * method's javadoc predicted ("a future rarity or enchant roll is one more line in this
     * method"), and it turned out to be exactly that.
     */
    private static void carryInstanceData(ItemMeta from, ItemMeta to, Keys keys, Material material) {
        String id = from.getPersistentDataContainer().get(keys.weaponId, PersistentDataType.STRING);
        if (id != null) {
            to.getPersistentDataContainer().set(keys.weaponId, PersistentDataType.STRING, id);
        }
        carryWear(from, to, material);
        carryEnchants(from, to, keys);
    }

    /**
     * Carry the item's enchant state across the re-mint. THE INVARIANT ENCHANT PASS 1 PROTECTS
     * ABOVE ALL: a re-mint regenerates DISPLAY and never touches enchant state, so a content edit,
     * a {@code /rpg refresh} or a rejoin cannot cost a player an unlock they earned.
     *
     * <p><b>The RAW STRING moves. It is deliberately not decoded and re-encoded, and that is the
     * guarantee rather than an optimisation.</b> A blob written by a future build, in a grammar
     * this one cannot parse, arrives on the fresh item byte for byte -- so a version skew degrades
     * to "renders as unenchanted until you update" instead of "silently rewritten into the old
     * grammar, losing whatever the new one added". Only READERS parse; the carry moves bytes.
     *
     * <p>It is also what makes {@code EnchantCodec.decode}'s unknown-version arm safe to write as
     * "return empty": that arm would be data loss if this method round-tripped through it.
     *
     * <p>The two keys are carried INDEPENDENTLY, and a half-tagged item is NOT rejected -- unlike
     * {@code ClassDamageModifierItems.grantOf}, whose two keys are two halves of one value. State
     * without the flag is an item enchanted by hand before any roll existed; the flag without state
     * is a roll that came up empty. Both are legal, and discarding either would be precisely the
     * data loss this method exists to prevent.
     */
    private static void carryEnchants(ItemMeta from, ItemMeta to, Keys keys) {
        String data = from.getPersistentDataContainer().get(keys.enchantData, PersistentDataType.STRING);
        if (data != null) {
            to.getPersistentDataContainer().set(keys.enchantData, PersistentDataType.STRING, data);
        }
        Byte rolled = from.getPersistentDataContainer().get(keys.enchantRolled, PersistentDataType.BYTE);
        if (rolled != null) {
            to.getPersistentDataContainer().set(keys.enchantRolled, PersistentDataType.BYTE, rolled);
        }
    }

    /**
     * Carry accumulated durability damage across the re-mint.
     *
     * The RAW damage value moves, not the wear fraction: if content also changed the material, 50
     * damage out of iron's 250 becomes 50 out of diamond's 1561, so the item reads as less worn
     * than it was. Accepted deliberately -- material changes are rare and the discrepancy is
     * cosmetic.
     *
     * The clamp is NOT cosmetic, and is why this is a method rather than a line. A material change
     * in the other direction -- iron (250) to gold (32) -- would copy a damage value past the new
     * maximum, and an item damaged beyond its maximum is a BROKEN item. Without the clamp, a
     * DISPLAY refresh could destroy a player's weapon outright: the exact class of failure this
     * pass exists to avoid, arriving through the fix rather than the bug.
     *
     * The clamp ITSELF is not here: it is {@link Durability#clamp}, the same floor the durability
     * pass's break gate and its dev commands apply. One definition of "a damage value that never
     * destroys the item", unit-tested in core, rather than a second copy of the arithmetic living
     * in a method no unit test can reach (an ItemStack needs a running server). This method keeps
     * only the item I/O. Worst case is a weapon one use from broken rather than one already gone.
     */
    private static void carryWear(ItemMeta from, ItemMeta to, Material material) {
        if (!(from instanceof Damageable worn) || !(to instanceof Damageable fresh)) return;
        short maxDurability = material.getMaxDurability();
        if (maxDurability <= 0) return;   // not a damageable material -- nothing to carry
        fresh.setDamage(Durability.clamp(worn.getDamage(), maxDurability));
    }
}
