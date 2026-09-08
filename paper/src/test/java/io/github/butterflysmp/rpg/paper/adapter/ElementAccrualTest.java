package io.github.butterflysmp.rpg.paper.adapter;

import io.github.butterflysmp.rpg.core.combat.Scorch;
import io.github.butterflysmp.rpg.core.combat.stat.DamageOutcome;
import io.github.butterflysmp.rpg.paper.content.ElementDefinition;
import io.github.butterflysmp.rpg.paper.content.ElementRegistry;
import io.github.butterflysmp.rpg.paper.content.StatusDefinition;
import io.github.butterflysmp.rpg.paper.content.StatusRegistry;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What a landed hit accrues. Pure -- no server, no Bukkit entity -- which is the whole reason the
 * decision was split out of {@code BukkitCombatant}: the rows that matter here are about a hit that
 * accrues NOTHING, and "nothing happened" is only a real assertion when the alternative is in the
 * same fixture.
 *
 * <p>The numbers are deliberately all different from one another. {@code dealt} 25 against
 * {@code amount} 30 against {@code newCurrent} 75 means a transposition at any of the three sites
 * reddens, where equal values would let a swap pass as a coincidence -- the trap that made the
 * {@code DamageOutcome} rows use an armoured victim.
 */
class ElementAccrualTest {

    private static final double EPS = 1e-9;

    private static ElementRegistry elements(String id, String appliesStatus) {
        var registry = new ElementRegistry();
        registry.register(new ElementDefinition(id, id, Component.text("*"), appliesStatus));
        return registry;
    }

    private static StatusRegistry statuses() {
        var registry = new StatusRegistry();
        registry.register(new StatusDefinition.Scorch("scorch"));
        registry.register(new StatusDefinition.Immobilize("rooted", false));
        registry.register(new StatusDefinition.Soaked("soaked"));
        registry.register(new StatusDefinition.Potion("surge", NamespacedKey.minecraft("speed")));
        return registry;
    }

    private static Optional<ElementAccrual.ScorchAccrual> accrue(String element, DamageOutcome outcome,
                                                           double amount) {
        return ElementAccrual.forHit(elements("fire", "scorch"), statuses(), element, outcome, amount);
    }

    // --- THE PAIR. Neither row discriminates alone. ----------------------------------------------

    @Test
    void aNONLETHALFireHitAccruesStacksFromWhatLANDEDAndCapsAtWhatWasASKED() {
        // THE POSITIVE TWIN of the lethal row below, and it exists because "apply was never called"
        // passes just as well when the call is DELETED OUTRIGHT. Same fixture shape, differing only
        // in the victim's remaining health.
        var accrued = accrue("fire", new DamageOutcome(25.0, 75.0), 30.0).orElseThrow();

        assertEquals(12, accrued.stacks(), "stacksFor(25) -- from what LANDED, so armour delays it");
        assertEquals(30.0, accrued.cap(), EPS,
                "capped at what was ASKED, pre-mitigation -- armour must never lower the ceiling, "
                        + "or it re-enters the DoT through the back door after being ruled out of it");
        assertEquals(Scorch.DEFAULT_DURATION_TICKS, accrued.durationTicks(),
                "the constant whose javadoc has said 'weapon-damage entry point only' since it was "
                        + "written; this is that entry point");
        // Mutation: stacksFor(amount) instead of dealt -> 15 != 12 -> reddens.
        // Mutation: cap from dealt instead of amount -> 25 != 30 -> reddens.
        // Mutation: hardcode 1 stack, as the explicit path does -> reddens.
    }

    @Test
    void aLETHALHitAccruesNOTHINGAtAll() {
        // THE ORDERING INVERSION THIS PREVENTS, measured rather than assumed: CombatantStats.damage
        // publishes its seam event BEFORE returning, and MobDeathSystem.onChange calls setHealth(0)
        // SYNCHRONOUSLY -- which fires removal, which calls scorch().forget(id). Accruing here would
        // register a RepeatingTask AFTER the cleanup meant to cancel it, leaving a map entry and a
        // 160-tick task per lethal fire kill with nothing left to cancel them.
        //
        // It also keeps Ignite's death-gate unambiguous: a killing blow's own stacks never count
        // toward the threshold that blow is measured against.
        assertTrue(accrue("fire", new DamageOutcome(25.0, 0.0), 30.0).isEmpty(),
                "a hit that took the target to zero accrues nothing");
        // Mutation: loosen the gate to newCurrent >= 0 -> ONLY THIS ROW reddens.
        // Mutation: delete the accrual call entirely -> ONLY THE ROW ABOVE reddens.
        // That is why both exist: neither alone can tell a broken gate from a missing feature.
    }

    @Test
    void aCombatantALREADYAtZeroAccruesNothingEither() {
        // The predicate is "still standing", NOT "this hit caused the transition" -- and the
        // difference is reachable, because MobDeathSystem.shouldKill excludes players, so a player
        // at zero custom health stays tracked and alive at the floor. A further hit on them reports
        // reachedZero == false while current is still 0. Keyed on the transition, that hit would
        // look like it landed on a live target.
        assertTrue(accrue("fire", new DamageOutcome(4.0, 0.0), 8.0).isEmpty(),
                "zero health is zero health, whichever hit put them there");
    }

    // --- The loop guard, witnessed BEHAVIOURALLY ------------------------------------------------

    @Test
    void aBURNTICKAccruesNoStacksBecauseItCarriesNoElement() {
        // THE LOOP GUARD'S REAL ROW. Scorch's own tick reaches the port through EntityScorchSink,
        // which calls the four-argument applyDamage and so has no element to pass -- the burn cannot
        // feed itself. That is structural upstream, but it is asserted HERE as a BEHAVIOUR, because
        // a structural guard is only as good as the shape that delivers it.
        //
        // Deliberately NOT written as an `element == null` guard in the production code:
        // ElementRegistry.find is Optional.ofNullable over a LinkedHashMap, which permits a null key
        // and returns null rather than throwing, so a null element falls out through the ordinary
        // registry MISS. An explicit null check would be indistinguishable from that miss -- no row
        // could separate them -- which is the dead-guard shape this slice already found once.
        assertTrue(accrue(null, new DamageOutcome(50.0, 300.0), 50.0).isEmpty(),
                "an elementless hit -- a burn tick, a thorns reflect, fall damage -- accrues nothing");
        // Mutation: default a null element to "fire" -> reddens, and that mutation IS the loop.
    }

    @Test
    void anUnknownElementAccruesNothingRatherThanThrowing() {
        // Boot validation names a dangling element; a hit must not be where it is discovered.
        assertTrue(accrue("plazma", new DamageOutcome(25.0, 75.0), 30.0).isEmpty());
    }

    // --- Scope: one status, and only one that can accrue -----------------------------------------

    @Test
    void anElementDeclaringNOStatusAccruesNothing() {
        // Six of the seven shipped elements. Absent means off.
        assertTrue(ElementAccrual.forHit(elements("kinetic", null), statuses(), "kinetic",
                new DamageOutcome(25.0, 75.0), 30.0).isEmpty());
    }

    @Test
    void aNONSTACKINGStatusAccruesNothingEvenThoughItRESOLVES() {
        // applies_status: rooted resolves perfectly -- the file exists, the id is right -- and has
        // no stack count and no duration of its own, so there is nothing to apply.
        // ContentValidator.validateElements NAMES this at boot, which is why this returns empty
        // silently rather than warning per hit.
        for (String id : new String[] {"rooted", "soaked", "surge"}) {
            assertTrue(ElementAccrual.forHit(elements("nature", id), statuses(), "nature",
                            new DamageOutcome(25.0, 75.0), 30.0).isEmpty(),
                    id + " cannot accrue");
        }
        // Mutation: give the non-Scorch switch arms a scorch application -> all three redden.
    }

    // --- The chip hit, and a hit that lands nothing ----------------------------------------------

    @Test
    void aHitThatLANDSNothingAccruesNothing() {
        // dealt <= 0 is a fully absorbed hit. stacksFor returns 0, and returning empty rather than
        // a ScorchAccrual with stacks 0 matters: ScorchStatus.apply early-returns on a non-positive
        // count, so a caller could not tell a refused application from an accepted no-op.
        assertTrue(accrue("fire", new DamageOutcome(0.0, 75.0), 30.0).isEmpty());
    }

    @Test
    void aCHIPHitStillBuysASTACKBecauseAnyLandedFireDamageBurns() {
        // The floor. solar_grenade's field tick declares amount: 2, and against armour the landed
        // figure is 1.67 -- which floored to zero before Scorch.stacksFor gained max(1, ...), so the
        // shipped lingering field would have stopped scorching armoured targets entirely.
        var accrued = accrue("fire", new DamageOutcome(1.67, 18.0), 2.0).orElseThrow();

        assertEquals(1, accrued.stacks(), "1.67 landed still buys one stack");
        assertEquals(2.0, accrued.cap(), EPS, "capped at the authored 2, not at the 1.67 that landed");
    }
}
