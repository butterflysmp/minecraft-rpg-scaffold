package io.github.butterflysmp.rpg.paper.adapter;

import io.github.butterflysmp.rpg.core.combat.HitAccrual;
import io.github.butterflysmp.rpg.core.combat.CritState;
import io.github.butterflysmp.rpg.core.combat.DefenseRule;

import io.github.butterflysmp.rpg.core.combat.Douse;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.Optional;
import java.util.UUID;

/**
 * Binds a real {@link LivingEntity} to the Bukkit-free {@link ScorchSink} seam. 1:1 with the store
 * and the damage port, no logic -- the clock, the cap, the stacks and the lifetime all live in the
 * tested {@link ScorchStatus}, exactly as {@link EntitySpeedAttribute} carries none of Soaked's.
 *
 * <p><b>{@code bypassesDefense = true} is set HERE, once, and is not a parameter of the seam.</b>
 * Scorch always bypasses; making it a per-call argument would invite a caller to pass false and
 * silently reinstate the armour cut the operator ruled out. The one place it is decided is the one
 * place it is written.
 *
 * <p><b>{@code wasCrit = false}, deliberately.</b> A burn tick is not a swing: there is no roll, and
 * {@code CombatantHandle} is explicit that the flag is presentation-only with the multiplier already
 * inside the amount. A DoT that flashed crit particles every second would be lying about a roll that
 * never happened.
 */
public final class EntityScorchSink implements ScorchSink {

    private final LivingEntity entity;
    private final AdapterContext ctx;

    public EntityScorchSink(LivingEntity entity, AdapterContext ctx) {
        this.entity = entity;
        this.ctx = ctx;
    }

    /**
     * The victim's CUSTOM max, straight off the store.
     *
     * Returns 0 for an untracked combatant, which makes the burn 0 rather than throwing --
     * {@code CombatantStats.max} throws on an untracked id, and a scorch whose victim stopped being
     * tracked mid-burn must fizzle rather than raise inside a scheduled tick. The task stops on its
     * own when the entity goes away; this covers the narrower window where the entity lives but the
     * store no longer knows it.
     */
    @Override public double victimMaxHealth() {
        UUID id = entity.getUniqueId();
        return ctx.stats().tracks(id) ? ctx.stats().max(id) : 0.0;
    }

    /** The same key {@code MOBSEED} prints. */
    @Override public String victimType() {
        return entity.getType().key().value();
    }

    @Override public void deal(double amount, UUID applierId, String element) {
        if (amount <= 0) return;
        // THE BURN CARRIES ITS ELEMENT AND REFUSES TO ACCRUE FROM IT. The element is here so the
        // damage number is MARKED like the hit that lit it; AccrualRule.INERT is what stops the burn
        // feeding itself. Those were ONE fact while the guard was the ABSENCE of an element -- the
        // burn drew an unmarked number as the price of not looping. They are two values now.
        //
        // FLIPPING THIS TO ACCRUES IS A RUNAWAY, NOT A WRONG FIGURE: every period would re-apply
        // the status, refresh the window and add stacks, and the clock would never run out. Bound
        // any mutation of it to a fixed number of periods -- a hang is not evidence a guard works.
        //
        // AND NO UNIT TEST WITNESSES THIS LINE. MEASURED, not assumed: nothing in paper/src/test
        // references EntityScorchSink -- every ScorchStatus row runs against FakeScorchSink -- so
        // flipping INERT to ACCRUES here leaves the whole suite GREEN. It was flipped and the run
        // was 813/17/569, Failures: 0.
        //
        // The GUARD is tested (ElementAccrual.forHit returns empty on INERT, and its mutation
        // reddens two rows); this CALLER choosing INERT is not, because the choice sits behind a
        // real Bukkit entity. Same class of gap as PacketDamagePopupSender and EntityTaskTarget,
        // which this repo already accepts as boot-witnessed -- but louder, because the failure is a
        // runaway rather than a wrong pixel.
        //
        // BOOT ROW THAT WITNESSES IT: light a mob and watch the burn STOP. Six ticks at 20-tick
        // spacing and then silence. If it never expires, this line says ACCRUES.
        BukkitCombatant.of(entity, ctx).handle()
                .applyDamage(amount, applierId, CritState.NORMAL, DefenseRule.BYPASSED,
                        element, HitAccrual.inert());
    }

    @Override public Optional<Douse.Cause> wet() {
        return wetness(entity);
    }

    @Override public void clearFlame() {
        entity.setFireTicks(Math.min(0, entity.getFireTicks()));
    }

    /**
     * THE ADAPTER'S ONE READING OF "WET" (the seat's Y2 and E1): the server's own two answers, read on the body's
     * thread, through the one pure decision. Used by the application and the burn tick (through {@link #wet}) and by
     * both of Ignite's clauses at a death, so all three seams read the same thing.
     *
     * <p>READ by javap, the pinned 26.1.2 server: {@code CraftEntity.isInWater} is {@code Entity.isInWater}, which
     * returns the field {@code wasTouchingWater}, set each tick by {@code Entity.updateFluidInteraction} from
     * {@code EntityFluidInteraction.update}, which walks the fluid box's blocks' {@code getFluidState()} for the WATER
     * tag. So it covers water source and flowing, a waterlogged block and a bubble column (whose
     * {@code getFluidState} is a water source), and NOT a water cauldron: neither cauldron class overrides
     * {@code getFluidState}, so it is {@code BlockBehaviour}'s {@code Fluids.EMPTY} (E4). {@code isInRain} is
     * {@code Entity.isInRain}: raining at the block or the top of the box, which asks the sky, the rain heightmap and
     * the biome's precipitation.
     */
    public static Optional<Douse.Cause> wetness(Entity entity) {
        return Douse.wet(entity.isInWater(), entity.isInRain());
    }
}
