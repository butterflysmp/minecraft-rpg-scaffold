package io.github.butterflysmp.rpg.paper.health;

import io.github.butterflysmp.rpg.core.combat.stat.HealthChange;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The pure show-gate: a number is drawn only for player-dealt DAMAGE with a known dealer. Everything
 * else -- heals, max changes, unattributed or non-player dealers -- draws nothing this pass. The wire
 * format is boot-witnessed; this gate is the one piece of the manager that is unit-testable.
 */
class DamagePopupManagerTest {

    private static HealthChange change(HealthChange.Kind kind, UUID dealer, boolean dealerIsPlayer) {
        return new HealthChange(UUID.randomUUID(), false, kind, 12.0, dealer, dealerIsPlayer, 88, 100, false);
    }

    @Test
    void showsForPlayerDealtDamage() {
        assertTrue(DamagePopupManager.shouldShow(change(HealthChange.Kind.DAMAGE, UUID.randomUUID(), true)),
                "player-dealt damage with a known dealer -> show");
    }

    @Test
    void hidesHealAndMaxChange() {
        UUID dealer = UUID.randomUUID();
        assertFalse(DamagePopupManager.shouldShow(change(HealthChange.Kind.HEAL, dealer, true)),
                "no heal numbers this pass");
        assertFalse(DamagePopupManager.shouldShow(change(HealthChange.Kind.MAX_CHANGE, dealer, true)),
                "a max change is not a hit");
        // Mutation: drop the kind == DAMAGE clause -> a heal pops a number -> reddens.
    }

    @Test
    void hidesNonPlayerAndUnattributedDealers() {
        assertFalse(DamagePopupManager.shouldShow(change(HealthChange.Kind.DAMAGE, UUID.randomUUID(), false)),
                "a mob dealer shows nothing until the mob->player pass");
        assertFalse(DamagePopupManager.shouldShow(change(HealthChange.Kind.DAMAGE, null, true)),
                "no dealer UUID -> no screen to draw on (null-safe)");
        // Mutation: drop the dealerIsPlayer or dealer != null clause -> reddens (and the null case NPEs
        // the manager at Bukkit.getPlayer(null) in the field).
    }
}
