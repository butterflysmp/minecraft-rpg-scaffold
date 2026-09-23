package io.github.butterflysmp.rpg.core.anvil;

import io.github.butterflysmp.rpg.core.weapon.GearScore;
import io.github.butterflysmp.rpg.core.weapon.Rarity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The anvil's decision, one row per verdict.
 *
 * <h2>*** EVERY ROW ASSERTS THE NAMED ARM, NEVER MERELY "REFUSED" ***</h2>
 *
 * Ben's rule: <i>"a row that accepts either cannot fail for its stated reason."</i> An empty slot
 * and a tool refuse for different reasons and say different things to the player, so a row written
 * as {@code assertFalse(ready)} would pass on a build that had collapsed the two -- which is the
 * defect the named refusal set exists to prevent.
 *
 * <h2>FIXTURES ARE STAGED SO NO TWO QUANTITIES A ROW READS ARE EQUAL</h2>
 *
 * {@code CLAUDE.md}'s collision rule. Scores are 140 / 260 / 310 rather than round numbers near the
 * baseline, so a transposition between the two sides, or between a score and a cost, has nowhere to
 * hide. The one row that deliberately stages EQUAL scores is the strictly-higher row, where equality
 * is the condition under test.
 *
 * <p>Each test names the mutation it forces red; kill sets are RECORDED in the PR body.
 */
class AnvilTransferTest {

    private static Side scored(TransferKey key, int score, Rarity rarity) {
        return new Side.Scored(key, score, rarity);
    }

    // --- SLOT_EMPTY, the face the screen opens in ----------------------------------------------

    @Test
    void anEMPTYSlotIsTheRESTINGFace_notAnErrorPath() {
        // BOTH EMPTY is what a player sees the instant the screen opens, so it is the state the
        // whole feature is first judged by. Ben: word it as an instruction, not a complaint.
        assertInstanceOf(AnvilVerdict.SlotEmpty.class,
                AnvilTransfer.evaluate(Side.EMPTY, Side.EMPTY));
        assertInstanceOf(AnvilVerdict.SlotEmpty.class,
                AnvilTransfer.evaluate(scored(TransferKey.MELEE, 140, Rarity.RARE), Side.EMPTY),
                "a target with no donor is still waiting for an item, not refusing one");
        assertInstanceOf(AnvilVerdict.SlotEmpty.class,
                AnvilTransfer.evaluate(Side.EMPTY, scored(TransferKey.MELEE, 260, Rarity.RARE)),
                "and a donor with no target");

        AnvilVerdict verdict = AnvilTransfer.evaluate(Side.EMPTY, Side.EMPTY);
        assertEquals("Place the item to upgrade, and one to sacrifice.", verdict.sentence(),
                "an INSTRUCTION -- it tells a first-time player what the screen is for");
    }

    /**
     * *** SLOT_EMPTY WINS OVER NOT_SCOREABLE, AND IT IS A DECISION RATHER THAN BRANCH ORDER. ***
     *
     * <p>One empty slot and one holding a tool could honestly produce either. Empty wins because it
     * is the screen's resting state and the player's next action -- put something in -- is the same
     * either way. Pinned so a later reorder of the branches is a red row rather than a silent change
     * of message.
     */
    @Test
    void anEMPTYSlotBeatsAnUNSCOREABLEOne_soTheRestingFaceSurvivesAToolInTheOtherCell() {
        assertInstanceOf(AnvilVerdict.SlotEmpty.class,
                AnvilTransfer.evaluate(Side.EMPTY, Side.UNSCOREABLE));
        assertInstanceOf(AnvilVerdict.SlotEmpty.class,
                AnvilTransfer.evaluate(Side.UNSCOREABLE, Side.EMPTY));
    }

    // --- NOT_SCOREABLE, and it must NOT be KEY_MISMATCH -----------------------------------------

    /**
     * *** THE ROW BEN ASKED FOR BY NAME: IT MUST DISTINGUISH NOT_SCOREABLE FROM KEY_MISMATCH. ***
     *
     * <p>Both refuse. Only one is right, and the wrong one would tell a player holding two pickaxes
     * that they must both be the same kind of gear -- which they are.
     *
     * <p><b>This is also the row that proves the ORDERING</b> that makes {@code TransferKey.of}'s
     * TOOL arm unreachable in production: eligibility is decided before a key is derived. Here the
     * ordering is enforced by the TYPES -- an {@code Unscoreable} carries no key to compare -- which
     * is the benefit of the sealed {@code Side} over a nullable key, and this row is what says so
     * out loud.
     */
    @Test
    void anUNSCOREABLEPairIsNOTSCOREABLEAndNOTAKeyMismatch_bothRefuseAndOnlyOneIsRight() {
        AnvilVerdict verdict = AnvilTransfer.evaluate(Side.UNSCOREABLE, Side.UNSCOREABLE);

        assertInstanceOf(AnvilVerdict.NotScoreable.class, verdict,
                "two tools are not a KEY MISMATCH -- they have no keys at all, and saying they must"
                        + " be the same kind would be telling the player something that is already"
                        + " true");
        assertEquals("Both items must be gear that carries a score.", verdict.sentence());

        // EITHER SIDE ALONE, so the refusal is not accidentally about the pair.
        assertInstanceOf(AnvilVerdict.NotScoreable.class, AnvilTransfer.evaluate(
                Side.UNSCOREABLE, scored(TransferKey.MELEE, 260, Rarity.RARE)));
        assertInstanceOf(AnvilVerdict.NotScoreable.class, AnvilTransfer.evaluate(
                scored(TransferKey.MELEE, 140, Rarity.RARE), Side.UNSCOREABLE));
    }

    // --- KEY_MISMATCH ---------------------------------------------------------------------------

    /**
     * *** BOOTS MUST NOT FEED A HELMET, AND THIS IS THE HALF {@code GearClass} CANNOT EXPRESS. ***
     *
     * <p>Both sides are {@code GearClass.ARMOR}. A rule written on the gear class alone would let
     * this through, which is the entire reason {@code TransferKey} exists as a third axis.
     */
    @Test
    void BOOTSCannotFeedAHELMET_althoughBothAreGearClassARMOR() {
        AnvilVerdict verdict = AnvilTransfer.evaluate(
                scored(TransferKey.ARMOR_HEAD, 140, Rarity.RARE),
                scored(TransferKey.ARMOR_FEET, 310, Rarity.RARE));

        AnvilVerdict.KeyMismatch mismatch =
                assertInstanceOf(AnvilVerdict.KeyMismatch.class, verdict);
        assertEquals(TransferKey.ARMOR_HEAD, mismatch.targetKey());
        assertEquals(TransferKey.ARMOR_FEET, mismatch.donorKey());

        // THE SENTENCE NAMES THE TARGET'S KIND, because the donor is what the player must change.
        assertEquals("Both items must be helmets.", verdict.sentence());
        // Mutation MUT13A-KEYEQ: make the key test always false -> reddens here and on the row
        // below. MEASURED.
        //
        // *** AND MUT13A-KEYARMOR DOES **NOT** REDDEN THIS ROW, WHICH WAS PREDICTED AND WRONG. ***
        //
        // The prediction was that collapsing the four armour arms of TransferKey.ofArmor would make
        // boots and helmets share a key and turn this pair READY. It does not, and the reason is
        // this row's own staging: it constructs Side.Scored(ARMOR_HEAD, ..) and
        // Side.Scored(ARMOR_FEET, ..) DIRECTLY and never calls TransferKey.of at all. Collapsing
        // the derivation cannot reach a fixture that does not use it.
        //
        // THE SEPARATION IS CORRECT -- this row tests the COMPARISON and TransferKeyTest tests the
        // DERIVATION -- but it means the derivation has exactly ONE guard, and it is
        // TransferKeyTest.ARMOURSplitsFOURWays. Recorded in both places rather than in neither:
        // CLAUDE.md's rule is that when a mutation kills fewer rows than expected, you ask which
        // row is now the only thing holding that behaviour and say so in THAT row.
    }

    @Test
    void aMAGEWeaponCannotFeedARANGERWeapon_andTheSentenceNamesTheTARGET() {
        AnvilVerdict verdict = AnvilTransfer.evaluate(
                scored(TransferKey.RANGER, 140, Rarity.EPIC),
                scored(TransferKey.MAGE, 310, Rarity.EPIC));

        assertInstanceOf(AnvilVerdict.KeyMismatch.class, verdict);
        assertEquals("Both items must be ranged weapons.", verdict.sentence(),
                "the TARGET is the ranged weapon, so that is the kind the sacrifice must be");
    }

    @Test
    void aKeyMismatchRefusesToBeBuiltFromAnABSENTKey_becauseThatWouldBeNotScoreable() {
        // The record's own guard. A null key here would mean an Unscoreable side had reached the
        // key comparison, which is the ordering violation TransferKey.of throws for.
        assertThrows(IllegalArgumentException.class,
                () -> new AnvilVerdict.KeyMismatch(null, TransferKey.MELEE));
        assertThrows(IllegalArgumentException.class,
                () -> new AnvilVerdict.KeyMismatch(TransferKey.MELEE, null));
    }

    // --- DONOR_NOT_HIGHER -----------------------------------------------------------------------

    /**
     * *** EQUAL IS REJECTED, NOT A NO-OP. BEN'S RULING. ***
     *
     * <p>Accepting an equal pair would consume a second item in 13b and change nothing at all -- a
     * transaction whose only effect is a loss, and one the player would notice only afterwards.
     */
    @Test
    void anEQUALDonorIsREJECTED_notTreatedAsAHarmlessNoOp() {
        AnvilVerdict verdict = AnvilTransfer.evaluate(
                scored(TransferKey.MELEE, 260, Rarity.RARE),
                scored(TransferKey.MELEE, 260, Rarity.RARE));

        AnvilVerdict.DonorNotHigher refused =
                assertInstanceOf(AnvilVerdict.DonorNotHigher.class, verdict,
                        "EQUAL must refuse. A build using '<' instead of '<=' offers this pair as a"
                                + " legal transfer that moves nothing and eats the donor.");
        assertEquals(260, refused.targetScore());
        assertEquals(260, refused.donorScore());
        assertEquals("The sacrifice must score above 260. This one is 260.", verdict.sentence());
        // Mutation MUT13A-STRICT: '<=' -> '<' -> reddens HERE and nowhere else. This row is the
        // SOLE guard of the strictly-higher ruling.
    }

    @Test
    void aLOWERDonorIsRefusedAndTheSentenceSaysHOWFARShort() {
        AnvilVerdict verdict = AnvilTransfer.evaluate(
                scored(TransferKey.SHIELD, 310, Rarity.LEGENDARY),
                scored(TransferKey.SHIELD, 140, Rarity.LEGENDARY));

        assertInstanceOf(AnvilVerdict.DonorNotHigher.class, verdict);
        // CARRIES BOTH NUMBERS, so the player is not left to hover two tooltips and subtract --
        // CastResult.OnCooldown's rule: a refusal you can see the end of is a different experience.
        assertEquals("The sacrifice must score above 310. This one is 140.", verdict.sentence());
    }

    // --- READY ----------------------------------------------------------------------------------

    @Test
    void aHIGHERDonorOfTheSameKindIsREADY_andTheScoreMOVESRatherThanBlending() {
        AnvilVerdict verdict = AnvilTransfer.evaluate(
                scored(TransferKey.MELEE, 140, Rarity.RARE),
                scored(TransferKey.MELEE, 310, Rarity.RARE));

        AnvilVerdict.Ready ready = assertInstanceOf(AnvilVerdict.Ready.class, verdict);
        assertEquals(310, ready.newScore(),
                "the target takes the donor's score EXACTLY -- not a blend, not a bonus, and not"
                        + " the difference");
        assertEquals(910, ready.xpPoints(), "RARE is 25 levels, which is 910 points");
        assertEquals("Sacrifice to raise this to 310.", verdict.sentence());
    }

    /**
     * *** THE CROSS-RARITY ROW: THE COST KEYS ON THE ITEM BEING UPGRADED. ***
     *
     * <p><b>Which of the two rarities is read is its own axis</b>, and it is invisible whenever both
     * items share a rarity -- which is most pairs, and is why every other row here stages them
     * equal. Staged COMMON against EXOTIC, the two costs are 160 and 24045: numbers that cannot be
     * confused for one another.
     */
    @Test
    void theCOSTKeysOnTheTARGETSRarity_andTheDONORSIsCarriedButNeverRead() {
        AnvilVerdict.Ready ready = assertInstanceOf(AnvilVerdict.Ready.class,
                AnvilTransfer.evaluate(
                        scored(TransferKey.MELEE, 140, Rarity.COMMON),
                        scored(TransferKey.MELEE, 310, Rarity.EXOTIC)));

        assertEquals(160, ready.xpPoints(),
                "COMMON target, EXOTIC donor: the price is the TARGET's 160, not the donor's 24045."
                        + " Upgrading a common item is cheap however precious the thing you feed it.");
        assertEquals(310, ready.newScore(), "and the score still moves");

        // AND THE OTHER WAY ROUND, so the row cannot pass by reading whichever side happens to be
        // cheaper. An EXOTIC target costs 24045 whatever it is fed.
        AnvilVerdict.Ready reversed = assertInstanceOf(AnvilVerdict.Ready.class,
                AnvilTransfer.evaluate(
                        scored(TransferKey.MELEE, 140, Rarity.EXOTIC),
                        scored(TransferKey.MELEE, 310, Rarity.COMMON)));
        assertEquals(24045, reversed.xpPoints(),
                "EXOTIC target, COMMON donor: 24045, not 160");
        // Mutation MUT13A-WHOSERARITY: read donor.rarity() -> reddens HERE and nowhere else. This
        // row is the SOLE guard of which side prices the transfer.
    }

    /**
     * *** AN UNSTAMPED ITEM IS A LEGAL TARGET. THERE IS NO LEGACY ARM AND NO MIGRATION. ***
     *
     * <p>Ben's ruling: zero deployed versions exist, so there is no population of pre-system items
     * to protect. An unstamped item reads {@link GearScore#ABSENT}, which is a CORRECT reading
     * rather than a missing one, and the donor must simply exceed it.
     */
    @Test
    void anUNSTAMPEDTargetReadingABSENTIsAnOrdinaryTarget_noSpecialCase() {
        AnvilVerdict.Ready ready = assertInstanceOf(AnvilVerdict.Ready.class,
                AnvilTransfer.evaluate(
                        scored(TransferKey.MAGE, GearScore.ABSENT, Rarity.UNCOMMON),
                        scored(TransferKey.MAGE, GearScore.ABSENT + 1, Rarity.UNCOMMON)));
        assertEquals(GearScore.ABSENT + 1, ready.newScore(),
                "one point above the absent reading is a real upgrade, and the smallest one there"
                        + " is");

        // AND TWO UNSTAMPED ITEMS REFUSE, because ABSENT equals ABSENT and equal is rejected. This
        // is the ONE pair a player is most likely to try first, so it must refuse for the RIGHT
        // reason rather than as a "legacy item" special case.
        assertInstanceOf(AnvilVerdict.DonorNotHigher.class, AnvilTransfer.evaluate(
                scored(TransferKey.MAGE, GearScore.ABSENT, Rarity.UNCOMMON),
                scored(TransferKey.MAGE, GearScore.ABSENT, Rarity.UNCOMMON)));
    }

    // --- the seam's own guards -------------------------------------------------------------------

    @Test
    void aNULLSideIsAProgrammingErrorAndIsREFUSEDLOUDLY_becauseEmptyHasItsOwnValue() {
        // An absent Side is NOT an empty slot: the empty slot has a name, Side.EMPTY. A null here
        // means the adapter failed to gather one, which must not render as "place two items".
        assertThrows(IllegalArgumentException.class,
                () -> AnvilTransfer.evaluate(null, Side.EMPTY));
        assertThrows(IllegalArgumentException.class,
                () -> AnvilTransfer.evaluate(Side.EMPTY, null));
    }

    @Test
    void aSCOREDSideCannotBeBuiltWithoutAKeyOrARarity_theIllegalStatesAreUNREPRESENTABLE() {
        // This is what replaces the nullable seam. An item with no key is Unscoreable, and there is
        // no way to write it as a Scored with a hole in it.
        assertThrows(IllegalArgumentException.class,
                () -> new Side.Scored(null, 140, Rarity.RARE));
        assertThrows(IllegalArgumentException.class,
                () -> new Side.Scored(TransferKey.MELEE, 140, null));
    }

    @Test
    void theALLOCATIONFreeConstantsAreTheStatesTheyName() {
        assertInstanceOf(Side.Empty.class, Side.EMPTY);
        assertInstanceOf(Side.Unscoreable.class, Side.UNSCOREABLE);
    }

    /**
     * *** EVERY VERDICT SAYS SOMETHING, AND NO TWO SAY THE SAME THING. ***
     *
     * <p>{@code QuiverState}'s rule: <i>"a refusal that reuses another refusal's message is a bug
     * report waiting to be filed."</i> Four causes, four sentences -- a player who hits two of them
     * in a row must be able to tell that something changed.
     */
    @Test
    void allFIVEVerdictsRenderADISTINCTSentence_noRefusalReusesAnothers() {
        AnvilVerdict[] all = {
                new AnvilVerdict.Ready(310, 910),
                new AnvilVerdict.SlotEmpty(),
                new AnvilVerdict.NotScoreable(),
                new AnvilVerdict.KeyMismatch(TransferKey.ARMOR_HEAD, TransferKey.ARMOR_FEET),
                new AnvilVerdict.DonorNotHigher(260, 260),
        };

        java.util.Set<String> sentences = new java.util.HashSet<>();
        int checked = 0;
        for (AnvilVerdict verdict : all) {
            String sentence = verdict.sentence();
            assertTrue(sentence != null && !sentence.isBlank(),
                    verdict + " must say something -- this string is the whole readout");
            assertTrue(sentence.endsWith("."), verdict + " reads as a sentence: " + sentence);
            sentences.add(sentence);
            checked++;
        }
        assertEquals(5, checked, "the sweep has to have actually run");
        assertEquals(5, sentences.size(),
                "five outcomes, five sentences -- a shared one makes two different problems look"
                        + " like one: " + sentences);
    }
}
