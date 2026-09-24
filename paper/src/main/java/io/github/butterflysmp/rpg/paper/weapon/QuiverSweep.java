package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.weapon.QuiverState;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.adapter.Keys;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import io.github.butterflysmp.rpg.core.weapon.WeaponRegistry;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.UseCooldownComponent;

/**
 * The reload, drawn as vanilla's hotbar cooldown sweep over the weapon itself.
 *
 * <p>Ben's pick, 2026-09-23, beside moving the quiver notices to the action bar
 * ({@link QuiverNotice}). The sweep is the PROGRESS indicator, which is why there is no textual
 * countdown: one fact, one readout.
 *
 * <h2>EVERYTHING BELOW WAS READ OUT OF THE PINNED 26.1.2 JARS, NOT REMEMBERED</h2>
 *
 * <p><b>THE API HAS THREE KEYINGS, NOT TWO.</b> {@code HumanEntity} declares
 * {@code setCooldown(Material, int)}, {@code setCooldown(ItemStack, int)} and
 * {@code setCooldown(Key, int)}. The third is the one this class uses, because it names the cooldown
 * GROUP directly.
 *
 * <p><b>AND THE GROUP IS WHAT SCOPES THE SWEEP TO ONE WEAPON.</b>
 * {@code ItemCooldowns.getCooldownGroup(stack)} is, in bytecode, the stack's {@code use_cooldown}
 * component's group if it has one, <b>else {@code BuiltInRegistries.ITEM.getKey(stack.getItem())}</b>
 * -- the item id. So a cooldown set on a Plume that carries no component is a cooldown on
 * {@code minecraft:bow}, and it sweeps <b>every bow in the inventory</b>. That is the whole reason
 * {@link #ensureGroup} exists.
 *
 * <p><b>*** A COOLDOWN BLOCKS USE. THIS IS NOT ONLY COSMETIC. ***</b>
 * {@code ServerPlayerGameMode.useItem} reads
 * {@code if (player.getCooldowns().isOnCooldown(stack)) return InteractionResult.PASS;} at bytecode
 * offset 22, <b>before</b> it calls {@code ItemStack.use} at offset 52. So while the sweep runs, the
 * bow <b>does not start drawing at all</b> -- no charge, no animation, and therefore no tap bands
 * either. For a weapon that is reloading, that is the behaviour we want; it is written down because
 * nothing about {@code setCooldown} says it.
 *
 * <p><b>AND THE NOTICES SURVIVE IT, WHICH IS WHY THE TWO HALVES OF THIS SLICE COMPOSE.</b>
 * {@code ServerGamePacketListenerImpl.handleUseItem} fires
 * {@code CraftEventFactory.callPlayerInteractEvent} at offset 252 and only reaches
 * {@code gameMode.useItem} at offset 512. <b>The Bukkit event precedes the cooldown check</b>, so
 * {@code RpgListeners.onRightClick} still runs and {@link QuiverNotice#reloading} still speaks while
 * the sweep is turning. A player pressing fire mid-reload gets the action-bar line and no draw.
 *
 * <h2>THE 0.01 SECONDS IS A TRICK AND IT NEEDS ITS REASON WRITTEN DOWN</h2>
 *
 * <p>Declaring a group means putting a {@code use_cooldown} component on the item, and that component
 * is not inert: {@code ItemStack.releaseUsing} and {@code ItemStack.use} both call
 * {@code applyAfterUseComponentSideEffects}, which calls {@code UseCooldown.apply} -- so <b>vanilla
 * applies the component's own cooldown on every draw and every release.</b>
 *
 * <p>{@code UseCooldown.ticks()} is {@code (int)(seconds * 20.0f)}, and
 * {@code CraftUseCooldownComponent.setCooldownSeconds} runs the value through
 * {@code BoundChecker.requirePositive}, which demands <b>strictly greater than zero</b>. So zero is
 * refused and {@code 0.01f} gives {@code (int) 0.2 == 0} ticks: <b>the component declares the group
 * and the cooldown vanilla applies by itself is nothing at all.</b>
 *
 * <p><b>{@code getUseCooldown()} NEVER RETURNS NULL, AND ITS DEFAULT IS ONE SECOND.</b> When the
 * component is absent {@code CraftMetaItem} hands back a fresh one built with {@code new
 * UseCooldown(1.0f)} -- twenty ticks. So the seconds must be set EXPLICITLY on every path through
 * {@link #ensureGroup}; reading the component and writing it straight back would stamp a real
 * cooldown on the weapon.
 *
 * <h2>WHAT THIS CANNOT DO, SAID PLAINLY</h2>
 *
 * <p><b>A cooldown belongs to the PLAYER and a reload belongs to the ITEM, so exact fidelity is not
 * available.</b> {@code ItemCooldowns} is a field on {@code Player}, keyed by group; the reload
 * deadline is a PDC pair on the weapon. Two consequences, neither of them a bug to be fixed:
 *
 * <ul>
 *   <li><b>Two Plumes cannot both be represented.</b> They share a group, so there is one sweep for
 *       both. This class resolves that by always describing <b>the weapon in your hand</b>:
 *       {@link #reassert} is called when the held item changes, and it clears the slot you left.</li>
 *   <li><b>A cooldown does not survive death or logout and a reload does.</b> Measured: {@code Player}
 *       holds {@code ItemCooldowns} in a field that neither {@code addAdditionalSaveData} nor
 *       {@code readAdditionalSaveData} touches, and {@code ServerPlayer.restoreFrom} never mentions
 *       it -- so the sweep is gone on respawn and on rejoin while the reload, being on the item, is
 *       still running. {@link #reassert} is therefore called on join and on respawn too, which turns
 *       a divergence into a re-derivation.</li>
 * </ul>
 *
 * <p><b>The rule that falls out: NEVER set this sweep once and trust it.</b> Every entry point
 * re-derives the remaining ticks from the item's own deadline through {@link Quivers#stateOf}, which
 * is the pure read -- never {@code resolveForShot}, which would commit a matured reload as a side
 * effect of drawing a progress bar.
 */
public final class QuiverSweep {

    private QuiverSweep() {}

    /**
     * Positive because {@code requirePositive} refuses zero, and small enough that
     * {@code (int)(seconds * 20)} is zero. See the class javadoc: this number's whole job is to make
     * the component exist without making it do anything.
     */
    private static final float GROUP_ONLY_SECONDS = 0.01f;

    /**
     * Give the item a private cooldown group, so a sweep on it is a sweep on THIS weapon.
     *
     * <p>Call it inside the same {@code editMeta} that stamps the reload -- which makes it
     * <b>self-healing and removes any migration step</b>: a Plume minted before this slice has no
     * component, and the first reload it starts gives it one. Nothing has to sweep the world's
     * inventories.
     *
     * <p>The seconds are set on every call and not only when the component is new, because the
     * absent-component default is a real one-second cooldown. See the class javadoc.
     */
    public static void ensureGroup(ItemMeta meta, String weaponId, Keys keys) {
        UseCooldownComponent cooldown = meta.getUseCooldown();
        cooldown.setCooldownSeconds(GROUP_ONLY_SECONDS);
        cooldown.setCooldownGroup(keys.quiverReloadGroup(weaponId));
        meta.setUseCooldown(cooldown);
    }

    /** Turn the sweep on for {@code ticksRemaining}. A value at or below zero clears it instead. */
    public static void show(Player player, String weaponId, Keys keys, long ticksRemaining) {
        NamespacedKey group = keys.quiverReloadGroup(weaponId);
        // setCooldown(Key, int) is the Key overload -- a NamespacedKey implements adventure's Key, and
        // is neither a Material nor an ItemStack, so there is nothing for it to be confused with.
        //
        // Clamped rather than asserted: a deadline in the past is an ordinary state here (the reload
        // matured while the player was elsewhere) and the answer to it is "no sweep", not a throw.
        player.setCooldown(group, (int) Math.max(0L, Math.min(Integer.MAX_VALUE, ticksRemaining)));
    }

    /**
     * Turn the sweep off.
     *
     * <p>{@code CraftHumanEntity.setCooldown} forwards straight to {@code ItemCooldowns.addCooldown}
     * -- it does NOT call {@code removeCooldown} -- so clearing is a zero-length cooldown, which is
     * already expired the moment it is written ({@code isOnCooldown} compares {@code endTime} to the
     * current tick). That is the supported idiom rather than a trick, but it does mean "cleared"
     * leaves a spent map entry behind, and that is harmless.
     */
    public static void clear(Player player, String weaponId, Keys keys) {
        show(player, weaponId, keys, 0L);
    }

    /**
     * Re-derive the sweep for whatever the player is holding NOW, and clear what they were holding.
     *
     * <p>This is the method that keeps the promise the brief asked for -- <i>the sweep must never
     * outlive the reload or end before it</i> -- as closely as a player-keyed cooldown can keep it.
     * It is called from the held-item change, from join and from respawn, and each call throws away
     * the previous answer and asks the ITEM again.
     *
     * @param previous the stack being left, or {@code null}. Its group is cleared if it is a quiver
     *                 weapon, so switching away cannot leave a sweep -- or a use-block -- on another
     *                 weapon of the same kind.
     * @param current  the stack being held, or {@code null}.
     */
    public static void reassert(Player player, ItemStack previous, ItemStack current,
                                WeaponRegistry weapons, AdapterContext adapters, long now) {
        Keys keys = adapters.keys();
        quiverWeaponOf(previous, weapons, keys)
                .ifPresent(weapon -> clear(player, weapon.id(), keys));
        quiverWeaponOf(current, weapons, keys).ifPresent(weapon -> {
            QuiverState state = Quivers.stateOf(current, keys, weapon);
            show(player, weapon.id(), keys, state.reloadTicksRemaining(now));
        });
    }

    /**
     * The weapon definition behind a stack, but only if it is a quiver weapon.
     *
     * <p>The magazine check is what keeps this off every other weapon: a sword has no magazine, so it
     * can never own a reload and must never be given a group.
     *
     * <p><b>{@code hasQuiver()} rather than a comparison against the authored size.</b> That predicate
     * lives in one place by {@code WeaponDefinition}'s own ruling, and the authored number may only be
     * READ in five named files -- {@code QuiversSignatureTest} fails the build for a sixth. The first
     * draft of this class re-expressed it and that guard is what said so.
     */
    private static java.util.Optional<WeaponDefinition> quiverWeaponOf(
            ItemStack stack, WeaponRegistry weapons, Keys keys) {
        if (stack == null || !stack.hasItemMeta()) return java.util.Optional.empty();
        return WeaponItems.weaponId(stack, keys)
                .flatMap(weapons::find)
                .filter(WeaponDefinition::hasQuiver);
    }
}
