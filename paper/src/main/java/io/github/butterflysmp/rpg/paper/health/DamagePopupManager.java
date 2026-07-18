package io.github.butterflysmp.rpg.paper.health;

import io.github.butterflysmp.rpg.core.combat.stat.HealthChange;
import io.github.butterflysmp.rpg.core.combat.stat.HealthListener;
import io.github.butterflysmp.rpg.paper.scheduler.Scheduler;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * The THIRD display on the {@link HealthChange} seam (after the heart bar and the mob nameplate): a
 * floating damage number shown to the DEALER only, over the target, for ~0.75s, as a fake client-side
 * TEXT_DISPLAY. It reads everything it needs off the seam event, so unlike the nameplate it needs no
 * store binding and no mob-lifecycle hooks -- it is a pure {@link HealthListener}.
 *
 * Threading: {@code onChange} runs synchronously on the TARGET entity's owning thread (applyDamage ->
 * onEntity(target) -> stats.damage -> onChange), so reading the target's {@link Location} here is legal.
 * The dealer is only a UUID and may live on another thread, but PacketEvents sends are thread-agnostic
 * (only Bukkit-world reads are thread-bound), and the destroy is scheduled on the dealer's own thread.
 */
public final class DamagePopupManager implements HealthListener {

    /** How long a number lives before it is destroyed. ~0.75s at 20 tps. */
    static final long LIFETIME_TICKS = 15L;
    /** Spawn the number this far above the target's head. Tuned at boot. */
    private static final double HEIGHT_OFFSET = 0.4;

    private final Scheduler scheduler;
    private final DamagePopupSender sender;
    private final PopupEntityIds ids = new PopupEntityIds();

    public DamagePopupManager(Scheduler scheduler, DamagePopupSender sender) {
        this.scheduler = scheduler;
        this.sender = sender;
    }

    @Override
    public void onChange(HealthChange change) {
        if (!shouldShow(change)) return;
        // Dealer's screen to draw on. Offline -> nothing to show.
        Player dealer = Bukkit.getPlayer(change.dealer());
        if (dealer == null) return;
        // Target position: legal to read here (this runs on the target's owning thread).
        Entity target = Bukkit.getEntity(change.target());
        if (target == null) return;
        Location at = target.getLocation().add(0, target.getHeight() + HEIGHT_OFFSET, 0);

        Component text = DamageNumberText.of(change.amount());
        int id = ids.next();
        sender.spawn(dealer, id, at.getX(), at.getY(), at.getZ(), text);
        // Destroy through the project Scheduler (never a BukkitRunnable), tied to the dealer: if they
        // log off the destroy never fires -- and the fake entity only existed on their now-gone client,
        // so there is nothing to leak.
        scheduler.onEntityLater(dealer, () -> sender.destroy(dealer, id), LIFETIME_TICKS);
    }

    /**
     * The pure gate: a number is shown only for player-dealt DAMAGE with a known dealer. HEAL / MAX_CHANGE
     * show nothing (no heal numbers this pass); a mob dealer (unattributed or non-player) shows nothing
     * until the mob->player pass decides the self-facing case.
     */
    static boolean shouldShow(HealthChange change) {
        return change.kind() == HealthChange.Kind.DAMAGE
                && change.dealerIsPlayer()
                && change.dealer() != null;
    }
}
