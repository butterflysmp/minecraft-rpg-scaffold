package io.github.butterflysmp.rpg.core.mob;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * ONLY ATTACKS DESTROY THE SHOT (the seat's ruling 1 on #201, after the boot of 8ec16e89, where a wall's SUFFOCATION
 * destroyed one). Ben: "Any attack destroys it". The classification is pinned here, by Bukkit's {@code DamageCause}
 * NAMES, so core needs no Bukkit type.
 */
class ShotDamageTest {

    @Test
    void aCombatantsMeleeSweepProjectileExplosionOrMagicIsAnAttack() {
        for (String cause : List.of("ENTITY_ATTACK", "ENTITY_SWEEP_ATTACK", "PROJECTILE", "ENTITY_EXPLOSION", "MAGIC",
                "SONIC_BOOM")) {
            assertEquals(ShotDamage.Verdict.ATTACK, ShotDamage.classify(cause, true), cause);
        }
    }

    @Test
    void theSameCausesWithNoCombatantBehindThemAreRefused() {
        for (String cause : List.of("PROJECTILE", "ENTITY_EXPLOSION", "MAGIC", "ENTITY_ATTACK")) {
            assertEquals(ShotDamage.Verdict.REFUSED, ShotDamage.classify(cause, false), cause);
        }
    }

    @Test
    void everyEnvironmentalCauseIsRefusedWhoeverIsBehindIt() {
        for (String cause : List.of("SUFFOCATION", "VOID", "CRAMMING", "FALLING_BLOCK", "FIRE", "FIRE_TICK", "LAVA",
                "HOT_FLOOR", "DROWNING", "FREEZE", "LIGHTNING", "WORLD_BORDER", "BLOCK_EXPLOSION", "CONTACT", "FALL",
                "STARVATION", "POISON", "WITHER", "THORNS", "DRAGON_BREATH", "FLY_INTO_WALL", "CAMPFIRE", "CUSTOM")) {
            assertEquals(ShotDamage.Verdict.REFUSED, ShotDamage.classify(cause, true), cause + " by a combatant");
            assertEquals(ShotDamage.Verdict.REFUSED, ShotDamage.classify(cause, false), cause);
        }
    }

    @Test
    void killEndsItAsBefore() {
        assertEquals(ShotDamage.Verdict.END, ShotDamage.classify("KILL", false));
        assertEquals(ShotDamage.Verdict.END, ShotDamage.classify("KILL", true));
    }

    @Test
    void anUnknownCauseIsRefusedNotAnAttack() {
        assertEquals(ShotDamage.Verdict.REFUSED, ShotDamage.classify("SOMETHING_NEW", true));
    }
}
