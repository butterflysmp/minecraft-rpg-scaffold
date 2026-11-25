package io.github.butterflysmp.rpg.paper.summon;

import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static io.github.butterflysmp.rpg.paper.summon.SummonWorldDamage.Outcome.MOB_HIT;
import static io.github.butterflysmp.rpg.paper.summon.SummonWorldDamage.Outcome.REFUSED;
import static io.github.butterflysmp.rpg.paper.summon.SummonWorldDamage.Outcome.REMOVE_KILLED;
import static io.github.butterflysmp.rpg.paper.summon.SummonWorldDamage.Outcome.REMOVE_VOID;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Q4 (the seat, 2026-11-24): every world cause refused, void and kill remove it, a mob's hit lands. Over the WHOLE enum. */
class SummonWorldDamageTest {

    @SuppressWarnings("deprecation")
    @Test
    void everyWorldCauseIsRefusedAndOnlyAMobsHitLands() {
        Set<DamageCause> named = EnumSet.of(DamageCause.FALL, DamageCause.FIRE, DamageCause.FIRE_TICK, DamageCause.LAVA,
                DamageCause.DROWNING, DamageCause.SUFFOCATION, DamageCause.HOT_FLOOR, DamageCause.CONTACT,
                DamageCause.FREEZE, DamageCause.LIGHTNING, DamageCause.CRAMMING, DamageCause.FALLING_BLOCK);
        for (DamageCause c : named) assertEquals(REFUSED, SummonWorldDamage.decide(c, false), c + ": the ruling's causes");
        for (DamageCause c : DamageCause.values()) {
            if (c == DamageCause.VOID || c == DamageCause.KILL) continue;
            assertEquals(REFUSED, SummonWorldDamage.decide(c, false), c + " with no mob behind it");
            assertEquals(MOB_HIT, SummonWorldDamage.decide(c, true), c + " with a mob behind it: a mob's hit lands");
        }
        assertEquals(REMOVE_VOID, SummonWorldDamage.decide(DamageCause.VOID, false));
        assertEquals(REMOVE_VOID, SummonWorldDamage.decide(DamageCause.VOID, true), "the void removes it whatever pushed it");
        assertEquals(REMOVE_KILLED, SummonWorldDamage.decide(DamageCause.KILL, false));
        assertEquals(REMOVE_KILLED, SummonWorldDamage.decide(DamageCause.KILL, true));
    }
}
