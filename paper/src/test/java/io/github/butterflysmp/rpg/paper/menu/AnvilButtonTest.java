package io.github.butterflysmp.rpg.paper.menu;

import io.github.butterflysmp.rpg.core.anvil.Affordability;
import io.github.butterflysmp.rpg.core.anvil.AnvilDecision;
import io.github.butterflysmp.rpg.core.anvil.AnvilVerdict;
import io.github.butterflysmp.rpg.core.anvil.Side;
import io.github.butterflysmp.rpg.core.anvil.TransferKey;
import io.github.butterflysmp.rpg.core.weapon.Rarity;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The confirm button: the clock layered over the decision.
 *
 * <p>Pure, so it runs at the two-second loop. {@code AnvilMenu} cannot be constructed without a
 * server, which is why this logic is here and not there.
 *
 * <p>Each test names the mutation it forces red; kill sets are RECORDED in the PR body.
 */
class AnvilButtonTest {

    private static AnvilDecision ready(int wallet) {
        return AnvilDecision.of(
                new Side.Scored(TransferKey.MELEE, 140, Rarity.RARE),
                new Side.Scored(TransferKey.MELEE, 310, Rarity.RARE),
                wallet);
    }

    private static AnvilDecision refused() {
        return new AnvilDecision(new AnvilVerdict.SlotEmpty(), Affordability.AFFORDABLE);
    }

    /** RARE is 910 points; 5000 covers it and 400 does not. */
    private static final int RICH = 5000;
    private static final int POOR = 400;

    /**
     * *** THE THREE SECONDS, AND IT IS A LITERAL. ***
     *
     * <p>A constant only ever named symbolically has no guard: a mutation moves the code and every
     * expectation with it. The delay is the whole protection against a misclick that costs a player
     * thousands of XP, so the number is pinned.
     */
    @Test
    void theARMIsTHREESeconds_pinnedAsALiteral() {
        assertEquals(60, AnvilButton.ARM_TICKS, "three seconds at twenty ticks");
        assertEquals(10, AnvilButton.PERIOD_TICKS, "repainting twice a second");
        assertEquals(6, AnvilButton.ARM_TICKS / AnvilButton.PERIOD_TICKS,
                "so the countdown fires six times");
        // Mutation MUT13B-ARM0: ARM_TICKS -> 0 -> reddens here AND on the lockout row below, which
        // is the one that matters: a zero-tick arm is a live button with a decoration on it.
    }

    @Test
    void theCountdownRoundsUP_soALockedButtonNeverReadsZero() {
        assertEquals(3, AnvilButton.secondsRemaining(60), "three seconds exactly");
        assertEquals(3, AnvilButton.secondsRemaining(50), "2.5s still reads 3");
        assertEquals(2, AnvilButton.secondsRemaining(40));
        assertEquals(1, AnvilButton.secondsRemaining(10), "half a second still reads 1");
        assertEquals(0, AnvilButton.secondsRemaining(0), "and armed reads 0");
        // A zero on a locked button is the one number that would be a lie.
    }

    /**
     * *** ONLY LIME ACTS, AND A ZERO-TICK ARM IS THE BUG THE DELAY EXISTS TO PREVENT. ***
     */
    @Test
    void ONLYLimeActs_andAClickDuringTheLockoutDoesNOTHING() {
        assertTrue(AnvilButton.acts(AnvilButton.faceFor(ready(RICH), 0)), "armed and legal acts");
        assertFalse(AnvilButton.acts(AnvilButton.faceFor(ready(RICH), 1)),
                "ONE TICK SHORT DOES NOT. The countdown is the feedback; a click here is silent.");
        assertFalse(AnvilButton.acts(AnvilButton.faceFor(ready(RICH), AnvilButton.ARM_TICKS)));
        assertFalse(AnvilButton.acts(AnvilButton.faceFor(ready(POOR), 0)),
                "and an armed button the player cannot pay for does not act either");
        assertFalse(AnvilButton.acts(AnvilButton.faceFor(refused(), 0)));
    }

    /**
     * *** PRECEDENCE: NOT ACTIONABLE, THEN ARMING, THEN READY. THE ORDER IS THE RULING. ***
     *
     * <p>A refused pair shows its refusal whatever the clock says. <b>A freshly opened screen would
     * otherwise be both "nothing in it" and "arming" at once</b>, and a lock on an action with
     * nothing to lose is not information the player needs.
     */
    @Test
    void aREFUSEDPairShowsItsRefusalAtEVERYPointOnTheClock_neverACountdown() {
        int checked = 0;
        for (int ticks = 0; ticks <= AnvilButton.ARM_TICKS; ticks += 10) {
            AnvilButton.Face face = AnvilButton.faceFor(refused(), ticks);
            assertEquals(AnvilFace.State.EMPTY, face.state(),
                    "an empty screen is GRAY at tick " + ticks + ", not yellow");
            assertNotEquals(AnvilFace.State.ARMING, face.state());
            checked++;
        }
        assertEquals(7, checked, "the sweep has to have actually run");

        // AND SO DOES AN UNAFFORDABLE ONE -- the clock must not promise a transfer that cannot
        // happen when it reaches zero.
        assertEquals(AnvilFace.State.CANNOT_AFFORD,
                AnvilButton.faceFor(ready(POOR), AnvilButton.ARM_TICKS).state());
    }

    /**
     * *** THE TEXT MUST BE INJECTIVE OVER EVERYTHING THE PLAYER MUST SEE CHANGE. ***
     *
     * <p>{@code AnvilMenu.refreshPreview} suppresses a repaint when this string is unchanged, so a
     * text that collapsed two visibly different states would <b>freeze a live surface</b>. That is
     * why the ready face names both the result and the price.
     *
     * <p><b>It is never compared to DECIDE anything</b> -- the decision object is what reconciles --
     * but it is what the player reads, and what the repaint keys on.
     */
    @Test
    void theREADYTextNamesBOTHTheResultAndThePrice_orTheRepaintWouldFreeze() {
        AnvilDecision shown = ready(RICH);
        assertEquals("Raise to 310 -- 910 XP", AnvilButton.faceFor(shown, 0).text());

        // TWO DONORS OF DIFFERENT SCORES AND THE SAME PRICE MUST RENDER DIFFERENTLY.
        AnvilDecision other = AnvilDecision.of(
                new Side.Scored(TransferKey.MELEE, 140, Rarity.RARE),
                new Side.Scored(TransferKey.MELEE, 340, Rarity.RARE), RICH);
        assertNotEquals(AnvilButton.faceFor(shown, 0).text(),
                AnvilButton.faceFor(other, 0).text(),
                "SAME PRICE, DIFFERENT RESULT. A price-only text renders these identically and the "
                        + "cell freezes across exactly the swap that changes what the player gets.");
    }

    @Test
    void theCOUNTDOWNTextChangesOnEveryVisibleSecond_soTheRepaintIsNotSuppressedMidTick() {
        Set<String> texts = new HashSet<>();
        int checked = 0;
        for (int ticks = AnvilButton.ARM_TICKS; ticks > 0; ticks -= AnvilButton.PERIOD_TICKS) {
            texts.add(AnvilButton.faceFor(ready(RICH), ticks).text());
            checked++;
        }
        assertEquals(6, checked, "six fires over one countdown");
        assertEquals(3, texts.size(), "3, 2, 1 -- three distinct texts, so half the writes are "
                + "skipped and the countdown still visibly moves: " + texts);
    }

    @Test
    void anUNAFFORDABLEFaceSaysTheSHORTFALL_notAGenericRefusal() {
        AnvilButton.Face face = AnvilButton.faceFor(ready(POOR), 0);
        assertEquals(AnvilFace.State.CANNOT_AFFORD, face.state());
        assertEquals("This transfer costs 910 XP; you have 400.", face.text(),
                "the button carries the sentence, so the bar's red has a reason on it");
    }
}
