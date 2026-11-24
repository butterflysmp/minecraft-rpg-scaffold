package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.ability.AbilityDefinition;
import io.github.butterflysmp.rpg.core.ability.AbilityService;
import io.github.butterflysmp.rpg.core.ability.CastExecutor;
import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.ability.ResourceCost;
import io.github.butterflysmp.rpg.core.combat.Aim;
import io.github.butterflysmp.rpg.core.enchant.ActiveBehaviour;
import io.github.butterflysmp.rpg.core.enchant.EnchantBehaviour;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * NOTHING A SWEEP CAN SEE CARRIES THE ORB TAG BEFORE A LIVE ORB OWNS IT (the seat's fix of #265, 2026-11-23, after the run
 * of 78826389: every block body was swept as an orphan at spawn). READ from the server jar, the order of a spawn is: the
 * spawn's consumer, then the entity's ADD to the world (which raises {@code EntityAddToWorldEvent}, where the sweep runs),
 * then the spawn returns. A body tagged before it is added is seen unowned. So neither spawn tags it; {@code throwOrb}
 * CLAIMS it ({@code CombatWorld.claimOrbBody}) after {@code Orbs.place} owns its id.
 *
 * <p>The fake models that moment: each orb-body spawn runs the sweep's own test (tagged AND not owned) as the body is
 * added, before the spawn returns, and records a claim made before the store owns the body. Both kinds, item and block.
 */
class OrbClaimTest {

    private static final CastSpec.Orb ITEM = new CastSpec.Orb(12, 10, 300, 0.6, "heart_of_the_sea");
    private static final CastSpec.Orb BLOCK = new CastSpec.Orb(12, 10, 300, 0.6, null, null,
            new CastSpec.OrbBlock("lapis_block", 0.5, "#2A5CFF"));
    private static final Aim EAST = new Aim(new Vec3(0, 1.62, 0), new Vec3(1, 0, 0));

    private static AbilityDefinition throwOf(CastSpec.Orb orb) {
        return new AbilityDefinition("lapis_crystal/left_click", "Lapis Crystal", "kinetic", 600,
                new ResourceCost("mana", 30), orb, List.of());
    }

    private static FakeWorld.Dummy throwOne(FakeWorld world, CastSpec.Orb orb) {
        var him = new FakeWorld.Dummy(Vec3.ZERO);
        him.player = true;
        him.behaviours = List.of(new ActiveBehaviour("lapis_crystal", 2,
                new EnchantBehaviour.GrantTrigger("lapis_staff", "left_click", throwOf(orb))));
        world.entities.add(him);
        new CastExecutor(world).execute(new AbilityService.CastResult.Success(throwOf(orb), him.snapshot(), EAST, true));
        return him;
    }

    @Test
    void theSweepAtTheAddFindsNothingAndTheBodyIsClaimedOnlyOnceOwnedForBothKinds() {
        for (CastSpec.Orb orb : List.of(ITEM, BLOCK)) {
            var world = new FakeWorld();
            throwOne(world, orb);
            UUID body = world.orbMarkers.getLast();
            assertEquals(List.of(), world.sweptAtAdd, "nothing to remove at the add: " + orb);
            assertEquals(List.of(), world.claimedUnowned, "never claimed before the store owns it: " + orb);
            assertTrue(world.orbTagged.contains(body), "claimed once owned, so a later sweep can still find it: " + orb);
        }
    }

    /** The control: the fake's sweep is not blind. A claimed body whose orb is gone is an orphan when it is added again. */
    @Test
    void theControlAClaimedBodyNoOrbOwnsIsSweptWhenAddedAgain() {
        for (CastSpec.Orb orb : List.of(ITEM, BLOCK)) {
            var world = new FakeWorld();
            var him = throwOne(world, orb);
            UUID body = world.orbMarkers.getLast();
            world.entityAdded(body);
            assertEquals(List.of(), world.sweptAtAdd, "owned: not swept on a re-add");
            world.orbs.remove(him.id());
            world.entityAdded(body);
            assertEquals(List.of(body), world.sweptAtAdd, "unowned and tagged: swept");
        }
    }
}
