package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.ability.CastExecutor;
import io.github.butterflysmp.rpg.core.combat.CombatantSnapshot;
import io.github.butterflysmp.rpg.core.combat.CooldownTracker;
import io.github.butterflysmp.rpg.core.weapon.ShotStamp;
import io.github.butterflysmp.rpg.core.weapon.VanillaShot;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import io.github.butterflysmp.rpg.core.weapon.WeaponRegistry;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.adapter.BukkitCombatant;
import io.github.butterflysmp.rpg.paper.adapter.Keys;
import io.github.butterflysmp.rpg.paper.adapter.PaperCombatWorld;
import io.github.butterflysmp.rpg.paper.nexus.NexusHealing;
import io.github.butterflysmp.rpg.paper.nexus.NexusItems;
import io.github.butterflysmp.rpg.paper.nexus.NexusLock;
import io.github.butterflysmp.rpg.paper.nexus.NexusMirror;
import io.github.butterflysmp.rpg.paper.nexus.NexusSlots;
import io.github.butterflysmp.rpg.paper.profile.ProfileService;
import io.github.butterflysmp.rpg.storage.PlayerProfile;
import org.bukkit.Material;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Locale;
import java.util.Optional;

/**
 * THE VANILLA-DRIVEN BOW AND CROSSBOW (the seat's rulings 7 to 10, 2026-11-22, with B and C of the same day). A weapon that
 * binds the {@code shot} trigger is drawn, loaded and shot by VANILLA; this class acts only at the two moments vanilla
 * hands us: the SHOT ({@code EntityShootBowEvent}) and the HIT (the arrow's damage event). The decisions are core's
 * ({@link VanillaShot}, {@link ShotStamp}); this performs them.
 *
 * <p><b>THE SHOT.</b> Vanilla has already taken one projectile (READ, {@code BowItem.releaseUsing}: {@code useAmmo} before
 * {@code shoot}), from the off hand, the main hand, then slots 0 upward (READ, {@code Player.getProjectile}). The plan says
 * what to give back, what to spend, and whether the shot leaves; a shot that leaves is STAMPED, and a quiver-paid one is
 * marked so {@code RpgListeners.onMirrorShot} makes its arrow unretrievable (ruling B). A mob's bow is never touched
 * (ruling 13): only a player's shot of a minted weapon that binds {@code shot} reaches the plan.
 *
 * <p><b>THE HIT.</b> A stamped arrow from an online player striking a mob lands its weapon's {@code shot} on_hit through
 * {@code CastExecutor.landVanillaShot}: the same sum and PLAYERHIT line as every weapon hit, vanilla's number and its
 * critical-arrow bonus replaced by the token damage, as the swords do. Anything else keeps today's path.
 */
public final class VanillaShots {

    private final AdapterContext adapters;
    private final WeaponRegistry weapons;
    private final CooldownTracker cooldowns;
    /** For his locked slot only, when the shot took the mirror's last arrow (the seat's follow-up 2). */
    private final ProfileService profiles;

    public VanillaShots(AdapterContext adapters, WeaponRegistry weapons, CooldownTracker cooldowns,
                        ProfileService profiles) {
        this.adapters = adapters;
        this.weapons = weapons;
        this.cooldowns = cooldowns;
        this.profiles = profiles;
    }

    /**
     * A MINTED CROSSBOW'S VANILLA LOAD IS CANCELLED UNLESS VANILLA DRIVES THE WEAPON (the seat's ruling 11, exemption 2,
     * 2026-11-22): the vanilla-driven Crossbow is loaded by vanilla. A minted id that names no loaded weapon is cancelled
     * as before (fail closed: it was every minted crossbow's answer on the base). Asked by
     * {@code RpgListeners.onWeaponCrossbowLoad}.
     *
     * <p><b>KEYED ON THE WEAPON'S OWN {@code shot} TRIGGER, NARROWER THAN THE RULING'S WORDING</b> ("skips a weapon that
     * binds no right_click"), and identical on every shipped weapon. The wording, built literally, also answered "load"
     * for the Dragon's Plume, which binds only {@code draw}: no behaviour moved (a bow never loads as a crossbow), but the
     * gate was no longer the base's for it, which ruling 11's pin forbids. Every shot weapon binds no right_click
     * ({@code WeaponDefinition} refuses it), so the narrowing loses nothing the ruling wanted. Exemption 3 keys the same
     * way, on {@code draw}.
     */
    public static boolean crossbowLoadCancelled(Optional<WeaponDefinition> weapon) {
        return weapon.map(w -> w.vanillaShotTrigger().isEmpty()).orElse(true);
    }

    /**
     * IS VANILLA'S OWN WEAR CANCELLED ON THIS ITEM? Only on a weapon vanilla shoots (the seat's ruling I, Ben's: "I'd
     * rather have our unbreaking work on the two"). READ: vanilla charges it through {@code ItemStack}'s durability path,
     * which fires a cancellable {@code PlayerItemDamageEvent}; uncancelled, our Unbreaking is never consulted and a worn-out
     * Bow is destroyed. Our wear is charged at the shot instead ({@code WeaponDurability.applyWearOnShot}). A sword (no
     * {@code shot}) and a shield (no weapon id; ShieldDurability owns it) are untouched by this cancel.
     */
    public static boolean cancelsVanillaWear(Optional<WeaponDefinition> weapon) {
        return weapon.filter(w -> w.vanillaShotTrigger().isPresent()).isPresent();
    }

    /** The minted weapon on this item, as {@link #cancelsVanillaWear} asks it: any minted weapon, or empty. */
    public Optional<WeaponDefinition> mintedWeapon(ItemStack item) {
        return WeaponItems.weaponId(item, adapters.keys()).flatMap(weapons::find);
    }

    /** The minted weapon on this item, if it is one vanilla shoots (it binds {@code shot}). */
    public Optional<WeaponDefinition> vanillaDriven(ItemStack item) {
        return WeaponItems.weaponId(item, adapters.keys()).flatMap(weapons::find)
                .filter(w -> w.vanillaShotTrigger().isPresent());
    }

    /** What vanilla took: the mirror (by its TAG), a plain arrow of his own, or anything else (tipped, spectral, ...). */
    public static VanillaShot.Consumable consumableOf(ItemStack taken, Keys keys) {
        if (NexusItems.isNexus(taken, keys)) return VanillaShot.Consumable.MIRROR;
        if (taken != null && taken.getType() == Material.ARROW) return VanillaShot.Consumable.PLAIN_ARROW;
        return VanillaShot.Consumable.OTHER;
    }

    // ------------------------------------------------------------------------------------------ the shot

    public void onShot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;          // ruling 13: a mob's bow is untouched
        ItemStack bow = event.getBow();
        Optional<WeaponDefinition> found = vanillaDriven(bow);
        if (found.isEmpty()) return;
        WeaponDefinition weapon = found.get();
        Keys keys = adapters.keys();

        boolean offHand = event.getHand() == EquipmentSlot.OFF_HAND;
        int before = QuiverItems.loadedIn(bow, keys).orElse(0);
        ItemStack taken = event.getConsumable();
        VanillaShot.Consumable kind = consumableOf(taken, keys);
        boolean broken = WeaponDurability.isBroken(bow);
        VanillaShot.Plan plan = VanillaShot.of(broken, offHand, weapon.hasQuiver(), before, kind);

        if (plan.refund()) giveBack(player, taken);
        if (plan.cancel()) event.setCancelled(true);
        if (broken) BrokenNotice.notify(player, cooldowns);   // refused, still in his hand: vanilla never wears it
        if (plan.spendRound()) Quivers.spendRoundOnShot(player, weapon, adapters, bow);
        if (plan.emptyNotice()) QuiverNotice.empty(player, cooldowns);
        if (!plan.cancel()) {
            // OUR WEAR, BY OUR RULE (the seat's ruling I, Ben's): one use, our Unbreaking consulted, through the live bow.
            WeaponDurability.applyWearOnShot(player, keys, cooldowns, bow);
            Entity projectile = event.getProjectile();
            if (plan.quiverPaid()) {
                projectile.getPersistentDataContainer().set(keys.quiverPaidShot, PersistentDataType.BYTE, (byte) 1);
            }
            stamp(player, weapon, event.getForce(), projectile);
        }
        // THE MIRROR'S LAST ARROW (the seat's follow-up 2): put back here, before the redraw below draws it.
        restoreTakenNexus(player, kind, keys, profiles, adapters.log());
        // A cancelled shot or one that took the mirror: the slot drawn again from the weapon's count (ruling 1).
        if (plan.cancel() || kind == VanillaShot.Consumable.MIRROR) NexusMirror.redraw(player, keys);
        // THE CROSSBOW'S CHARGE AFTER THE SHOT (the seat's follow-up 3), its own line so VANILLASHOT's committed form is
        // unchanged. READ, the pinned jar: CrossbowItem.performShooting sets CHARGED_PROJECTILES to EMPTY BEFORE shoot
        // fires the event, and a cancelled event only removes the projectile and returns, so a refused shot leaves it
        // UNLOADED and the arrow given back is the one its load took: no arrow is made. getBow() is the live stack.
        if (bow.getItemMeta() instanceof CrossbowMeta crossbow) {
            adapters.log().info("CROSSBOWCHARGE " + player.getName() + " weapon=" + weapon.id()
                    + " charged=" + crossbow.hasChargedProjectiles());
        }

        adapters.log().info(String.format(Locale.ROOT,
                "VANILLASHOT %s weapon=%s hand=%s took=%s rounds=%d->%d force=%.3f cancel=%b refund=%b paid=%b",
                player.getName(), weapon.id(), offHand ? "off" : "main", kind.name().toLowerCase(Locale.ROOT),
                before, QuiverItems.loadedIn(bow, keys).orElse(0), event.getForce(), plan.cancel(), plan.refund(),
                plan.quiverPaid()));
    }

    /** One of what vanilla took, back into his inventory; what does not fit is dropped at his feet. */
    /**
     * THE MIRROR'S LAST ARROW (the seat's follow-up 2, 2026-11-22; the decision is core's {@link VanillaShot#restoresNexus}).
     * Vanilla took the Nexus item itself (READ, {@code useAmmo} removes an emptied stack), so the shot path puts it back,
     * in the same tick, by the heal's own converge call into his locked slot (the slot it was taken from is not in the
     * event). Its own line, {@code NEXUSRESTORE}, never the heal's: NEXUSHEAL keeps meaning "something else removed it".
     *
     * <p>Two callers, both on the shot event: {@link #onShot} (every vanilla-driven shot, a cancelled one included) and
     * {@code RpgListeners.onMirrorShot} (any other bow whose shot took the mirror, which it cancels; it ignores an event
     * already cancelled, hence the first caller). Whichever runs second finds the item and does nothing.
     */
    public static void restoreTakenNexus(Player player, VanillaShot.Consumable taken, Keys keys, ProfileService profiles,
                                         java.util.logging.Logger log) {
        if (!VanillaShot.restoresNexus(taken, NexusHealing.countNexus(player.getInventory(), keys))) return;
        int slot = profiles.profile(player.getUniqueId()).map(PlayerProfile::nexusSlot)
                .orElse(NexusLock.DEFAULT_LOCKED_SLOT);
        NexusSlots.converge(player, keys, slot);
        log.info("NEXUSRESTORE player=" + player.getName() + " slot=" + slot + " cause=shot");
    }

    private static void giveBack(Player player, ItemStack taken) {
        if (taken == null || taken.getType().isAir()) return;
        ItemStack one = taken.clone();
        one.setAmount(1);
        player.getInventory().addItem(one).values()
                .forEach(left -> player.getWorld().dropItem(player.getLocation(), left));
    }

    /** The stamp (ruling 10): the weapon, the force, and his terms and score snapshotted NOW, as a cast does. */
    private void stamp(Player player, WeaponDefinition weapon, double force, Entity projectile) {
        CombatantSnapshot atShot = BukkitCombatant.snapshot(player, adapters.stats());
        int score = new PaperCombatWorld(player.getWorld(), adapters).triggerScoreOf(player.getUniqueId());
        projectile.getPersistentDataContainer().set(adapters.keys().vanillaShot, PersistentDataType.STRING,
                ShotStamp.of(weapon.id(), force, atShot, score).encode());
    }

    // ------------------------------------------------------------------------------------------ the hit

    /** A stamped arrow, shot by an online player, striking a mob. */
    public record Stamped(Player shooter, LivingEntity victim, ShotStamp stamp) {}

    /**
     * THE ONE PREDICATE both damage handlers ask, so exactly one of them acts: the stamped hit lands ours, and
     * {@code onEnvironmentalDamage} returns early on exactly these. A player victim (PvP, or his own arrow falling on
     * him) keeps today's path, as the swords do: player-to-mob only.
     */
    public Optional<Stamped> stamped(org.bukkit.event.entity.EntityDamageEvent event) {
        if (!(event instanceof EntityDamageByEntityEvent byEntity)) return Optional.empty();
        if (!(byEntity.getDamager() instanceof AbstractArrow arrow)) return Optional.empty();
        if (!(arrow.getShooter() instanceof Player shooter) || !shooter.isOnline()) return Optional.empty();
        if (!(event.getEntity() instanceof LivingEntity victim) || victim instanceof Player) return Optional.empty();
        String raw = arrow.getPersistentDataContainer().get(adapters.keys().vanillaShot, PersistentDataType.STRING);
        return ShotStamp.decode(raw).map(stamp -> new Stamped(shooter, victim, stamp));
    }

    /**
     * Land the stamped shot's on_hit. The caller has already set vanilla's damage to the token. A stamp naming a weapon
     * no longer loaded lands nothing beyond the token (fail closed), and says so.
     */
    public void land(Stamped hit) {
        Optional<AbilityDefinition> trigger = weapons.find(hit.stamp().weaponId())
                .flatMap(WeaponDefinition::vanillaShotTrigger);
        if (trigger.isEmpty()) {
            adapters.log().warning("[vanilla-shot] an arrow stamped '" + hit.stamp().weaponId()
                    + "' hit, and no loaded weapon binds that shot; it dealt only the token");
            return;
        }
        CombatantSnapshot caster = hit.stamp().applyTo(BukkitCombatant.snapshot(hit.shooter(), adapters.stats()));
        new CastExecutor(new PaperCombatWorld(hit.victim().getWorld(), adapters), () -> { })
                .landVanillaShot(trigger.get(), caster, hit.stamp().triggerScore(),
                        BukkitCombatant.of(hit.victim(), adapters), VanillaShot.chargeScale(hit.stamp().force()));
    }
}
