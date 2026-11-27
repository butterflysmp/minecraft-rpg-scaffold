package io.github.butterflysmp.rpg.paper.adapter;

import io.github.butterflysmp.rpg.core.accessory.Blinks;
import org.bukkit.entity.Player;

/**
 * HIS ATTACK, AS THE BLINK SEES IT (the Blink Tome's K5, 2026-11-26; Ben: "Attacking cancels the invisibility", "Casting a
 * spell does, a summon whistle does not"). Told at the ONE place a weapon trigger fires past its cooldown and mana gates
 * ({@code WeaponFire.attempt}'s success, on any weapon whose class is not SUMMONER) and the ONE place his melee hit lands
 * ({@code RpgListeners.onPlayerMeleeAttack}). Never per weapon id.
 */
public interface AttackSink {

    void attacked(Player player, Blinks.EndReason reason);

    /** Bound once at enable; until then, and in a unit test, an attack ends nothing. */
    final class Late implements AttackSink {
        private volatile AttackSink to;

        public void bind(AttackSink sink) {
            this.to = sink;
        }

        @Override
        public void attacked(Player player, Blinks.EndReason reason) {
            AttackSink s = to;
            if (s != null) s.attacked(player, reason);
        }
    }
}
