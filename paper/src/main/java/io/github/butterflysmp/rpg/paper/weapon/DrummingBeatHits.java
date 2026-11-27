package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.enchant.ActiveBehaviour;
import io.github.butterflysmp.rpg.core.enchant.DrumBeats;
import io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.content.EnchantRegistry;
import org.bukkit.entity.Player;

import java.util.Optional;

/**
 * DRUMMING BEAT'S PAPER HALF (the Bone Club's M4 to M8, 2026-11-26): what the held weapon carries, and the feedback. The
 * decision is core's ({@link DrumBeats}); {@link WeaponFire#landVanillaMelee} asks both, at the one melee crit roll.
 */
public final class DrummingBeatHits {

    private DrummingBeatHits() {}

    /**
     * B2: the Drumming Beat ACTIVE on the weapon in his main hand, at its level, or empty: read off the main hand where
     * every behaviour is read ({@code BehaviourItems.activeOf}, which drops a weapon-specific behaviour on any other weapon).
     */
    public static Optional<ActiveBehaviour> active(Player player, String heldWeaponId, AdapterContext adapters) {
        return BehaviourItems.activeOf(EnchantItems.read(player.getInventory().getItemInMainHand(), adapters.keys()),
                        heldWeaponId, adapters.enchants()::find).stream()
                .filter(a -> a.behaviour() instanceof EnchantBehaviour.DrummingBeat)
                .findFirst();
    }

    /**
     * Is {@code weaponId} a weapon some shipped Drumming Beat names (the Bone Club today)? Its hits carry a STREAK trace
     * line even bare (result=plain, M8). Read off the registry, so no weapon id is written in Java.
     */
    public static boolean drums(String weaponId, EnchantRegistry enchants) {
        return enchants.all().stream().anyMatch(d -> d.behaviour() instanceof EnchantBehaviour.DrummingBeat b
                && b.weaponId().equals(weaponId));
    }

    /**
     * M7 (SEAT PLACEHOLDER 2026-11-26, the old game's sounds and volumes), played to HIM only, on his own thread: the drum
     * on a counted hit (a gap reset's hit is counted, as the first), its pitch rising with the streak to the limit; the
     * reset sound on an uncharged hit that lost a streak (CC'S CALL: a streak of 0 lost nothing, so no sound; the old game
     * played it on every uncharged hit). No visual. Nothing on a plain hit.
     */
    public static void feedback(Player player, DrumBeats.Decision d, EnchantBehaviour.DrummingBeat beat,
                                AdapterContext adapters) {
        if (beat == null) return;
        String sound;
        float volume;
        float pitch;
        switch (d.result()) {
            case COUNTED, RESET_GAP -> {
                sound = beat.beatSound();
                volume = 0.8f;
                pitch = DrumBeats.pitch(d.after().n());
            }
            case RESET_UNCHARGED -> {
                if (d.n() == 0) return;
                sound = beat.resetSound();
                volume = 1.0f;
                pitch = 0.8f;
            }
            default -> {
                return;
            }
        }
        if (sound == null) return;
        String key = sound;
        adapters.scheduler().onEntity(player, () -> player.playSound(player.getLocation(), key, volume, pitch));
    }
}
