package io.github.butterflysmp.rpg.core.enchant;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The enchant blob's grammar, whose whole job is that AN ITEM'S ENCHANTS READ BACK AS WHAT WAS
 * WRITTEN -- or, when they cannot, as less rather than as something else.
 *
 * Every case here is a guard whose absence is a real, shippable bug: a level that comes back
 * different, one corrupt slot costing a player the slots either side of it, an exception thrown
 * mid-render because a blob was hand-edited, or a future build's blob silently misparsed as this
 * one's.
 *
 * This is the first string codec in the repo, so the bar is deliberately higher than a round trip:
 * {@link #theEncodedFormIsTheExactGrammarNotJustSomethingThatRoundTrips} pins the literal wire
 * form, because a round-trip test stays green when encode and decode are broken TOGETHER -- the
 * exact failure CLAUDE.md records as a test passing on an accident rather than on the thing it was
 * written to guard.
 */
class EnchantCodecTest {

    private static final String A = "unbreaking";
    private static final String B = "sharpness";

    /** Two slots: the first offering both candidates with the first active, the second inactive. */
    private static EnchantState populated() {
        return EnchantState.empty()
                .addCandidate(0, A).withLevel(0, 0, 3)
                .addCandidate(0, B).withLevel(0, 1, 1)
                .withActive(0, 0)
                .addCandidate(1, A).withLevel(1, 0, 2);
    }

    @Test
    void anEmptyStateRoundTripsThroughTheVersionTokenAlone() {
        // Every unenchanted item writes this, so it is the most common blob in the game. It must
        // not be the empty string: a blank decodes to empty too, but writing the version means an
        // item's blob always announces which grammar it is in.
        assertEquals("v1", EnchantCodec.encode(EnchantState.empty()));
        assertEquals(EnchantState.empty(), EnchantCodec.decode("v1"));
    }

    @Test
    void aFullStateRoundTrips() {
        // Records give equals() for free, so this compares the whole structure -- slots,
        // candidates, levels and active index -- not a summary of it.
        EnchantState state = populated();
        assertEquals(state, EnchantCodec.decode(EnchantCodec.encode(state)));
    }

    @Test
    void theEncodedFormIsTheExactGrammarNotJustSomethingThatRoundTrips() {
        // THE DISCRIMINATING TEST. Change a separator and the round trip above stays green, because
        // encode and decode moved together; this reddens. Without it the pair could drift to any
        // grammar at all and every other test in this file would still pass -- while every blob
        // already on a player's item became unreadable.
        assertEquals("v1;unbreaking=3,sharpness=1:0;unbreaking=2:-1", EnchantCodec.encode(populated()));
        // Mutation: candidate separator ',' -> '|' -> reddens HERE while aFullStateRoundTrips stays green.
        // Mutation: emit the active index before the candidates -> reddens.
        // Mutation: drop the version token -> reddens.
    }

    @Test
    void aSlotOfferingNothingRoundTripsRatherThanDisappearing() {
        // The roster pass will produce these: a slot whose pool rolled empty. It must keep its
        // POSITION, or slot 1 becomes slot 0 on the next read and the item's shape changes.
        EnchantState state = new EnchantState(List.of(EnchantSlot.empty()));
        assertEquals("v1;:-1", EnchantCodec.encode(state));
        assertEquals(state, EnchantCodec.decode("v1;:-1"));
        assertEquals(1, EnchantCodec.decode("v1;:-1").slots().size());
    }

    @Test
    void nullAndBlankDecodeToAnEmptyStateRatherThanThrowing() {
        // The absent-key case, which every unenchanted item hits on every basic attack through the
        // seam's read. A throw here would be an exception per swing.
        assertEquals(EnchantState.empty(), EnchantCodec.decode(null));
        assertEquals(EnchantState.empty(), EnchantCodec.decode(""));
        assertEquals(EnchantState.empty(), EnchantCodec.decode("   "));
    }

    @Test
    void anUnknownVersionDecodesToEmptyRatherThanGuessing() {
        // A v2 blob is not guessed at. This is only SAFE because the carry moves the raw string
        // without decoding it: the item keeps its v2 data byte for byte and renders as unenchanted
        // until the build catches up, rather than being rewritten into v1 and losing whatever v2
        // added. Decode-and-re-encode in the carry would turn this line into data loss.
        assertEquals(EnchantState.empty(), EnchantCodec.decode("v2;unbreaking=3:0"));
        assertEquals(EnchantState.empty(), EnchantCodec.decode("v0;unbreaking=3:0"));
        assertEquals(EnchantState.empty(), EnchantCodec.decode("unbreaking=3:0"));
        // Mutation: drop the version check -> "v2;..." parses as v1 and its first slot is read as
        // the version token -> unlocks come back wrong -> reddens.
    }

    @Test
    void aMalformedSlotDegradesToAnEmptySlotAndTheOtherSlotsSurvive() {
        // One corrupt segment must not cost a player the slots either side of it. Discarding the
        // whole state on the first bad character is the easy implementation and the wrong one.
        EnchantState state = EnchantCodec.decode("v1;unbreaking=3:0;@@@@;sharpness=2:0");

        assertEquals(3, state.slots().size(), "the corrupt slot keeps its position");
        assertEquals(3, state.activeLevel(A), "the slot before it is intact");
        assertEquals(2, state.activeLevel(B), "and so is the slot after it");
        assertEquals(List.of(), state.slots().get(1).candidates(), "the corrupt one is empty");
        assertEquals(EnchantSlot.NONE, state.slots().get(1).activeIndex());
        // Mutation: return EnchantState.empty() on the first unreadable segment -> both surviving
        // slots are lost -> reddens.
    }

    @Test
    void aNonNumericLevelDecodesAsLockedRatherThanAsUnlocked() {
        // Fails toward LESS enchanted. Defaulting an unreadable level to 1 would hand out an unlock
        // the player never earned, on every corrupt blob.
        EnchantState state = EnchantCodec.decode("v1;unbreaking=x:-1");

        assertEquals(1, state.slots().get(0).candidates().size(), "the id survives");
        assertEquals(0, state.slots().get(0).candidates().get(0).level());
        assertEquals(0, EnchantCodec.decode("v1;unbreaking:-1").slots().get(0).candidates().get(0).level(),
                "a candidate with no level at all is locked too");
    }

    @Test
    void aLevelPastTheMaximumIsClampedOnDecodeRatherThanThrowing() {
        // The "a later build wrote level 5" case. EnchantCandidate THROWS on an out-of-range level,
        // so without the clamp an IllegalArgumentException escapes into a tooltip render or a join.
        // Clamped rather than dropped, because dropping loses the id as well as the excess.
        EnchantState state = assertDoesNotThrow(() -> EnchantCodec.decode("v1;unbreaking=9:0"));

        assertEquals(EnchantState.MAX_LEVEL, state.activeLevel(A));
        assertEquals(0, EnchantCodec.decode("v1;unbreaking=-4:-1").slots().get(0).candidates().get(0).level(),
                "and a negative level clamps up to locked");
        // Mutation: pass the raw level straight to the constructor -> IAE escapes -> reddens.
    }

    @Test
    void anActiveIndexPastTheCandidateListDecodesAsNothingActive() {
        // Fail to "nothing active", never to a crash. Keeping the index would put an
        // IndexOutOfBoundsException one method call away, on a real player's item.
        EnchantState state = assertDoesNotThrow(() -> EnchantCodec.decode("v1;unbreaking=3:7"));

        assertEquals(EnchantSlot.NONE, state.slots().get(0).activeIndex());
        assertEquals(3, state.slots().get(0).candidates().get(0).level(), "the unlock still survives");
        assertEquals(0, state.activeLevel(A), "it just is not taking effect");
        // Mutation: keep the out-of-range index -> EnchantSlot's constructor throws -> reddens.
    }

    @Test
    void anActiveIndexPointingAtALockedCandidateDecodesAsNothingActive() {
        // The other contradiction EnchantSlot forbids. A blob can express it; a state cannot.
        EnchantState state = assertDoesNotThrow(() -> EnchantCodec.decode("v1;unbreaking=0:0"));

        assertEquals(EnchantSlot.NONE, state.slots().get(0).activeIndex());
        assertEquals(List.of(), state.effective());
    }

    @Test
    void aDuplicateIdInOneSlotIsRepairedOnDecodeRatherThanThrowing() {
        // EnchantSlot refuses duplicates, so the codec must repair rather than hand one over.
        // First wins -- an arbitrary choice, but a fixed one, so the same blob always reads the same.
        EnchantState state = assertDoesNotThrow(
                () -> EnchantCodec.decode("v1;unbreaking=3,unbreaking=1:0"));

        assertEquals(1, state.slots().get(0).candidates().size());
        assertEquals(3, state.activeLevel(A), "the first of the two survives");
    }

    @Test
    void aBlankCandidateIdIsDroppedRatherThanConstructed() {
        EnchantState state = assertDoesNotThrow(() -> EnchantCodec.decode("v1;=3,unbreaking=2:0"));

        assertEquals(1, state.slots().get(0).candidates().size(), "the blank id is gone");
        assertEquals(A, state.slots().get(0).candidates().get(0).enchantId());
        assertEquals(2, state.activeLevel(A),
                "and the active index still points at the candidate that survived");
    }

    @Test
    void decodeNeverThrowsForAnyInputHoweverAdversarial() {
        // This blob comes off an item that a different build, or a hand editor, may have written.
        // An exception here surfaces as a failed render or a failed join, not as a diagnosable
        // error -- so totality is the contract, and it is checked rather than asserted in prose.
        String[] hostile = {
                "v1;", ";;;", "v1;:", "v1;=:", "v1;:::::", "v1;,,,:0", "v1;a=1:0:extra",
                "v1;a=1,:0", "v1;a=:", "v1;a=1:", "v1;a=1:abc", "v1;a=2147483648:0",
                "v1;a=1:2147483648", "v1; =1:0", "v1;😀=1:0",
                "v1;" + "a=1,".repeat(50) + ":0", "v1;" + "x".repeat(10_000) + "=1:0",
                "v1" + ";a=1:0".repeat(500),
        };

        // DISCOVERY-SHAPED, so it must fail loudly on finding nothing. An empty or truncated
        // battery would run clean and read exactly like a battery that passed.
        assertTrue(hostile.length >= 10,
                "the adversarial battery is empty or truncated -- this test checks nothing");

        for (String raw : hostile) {
            EnchantState state = assertDoesNotThrow(() -> EnchantCodec.decode(raw),
                    "decode must be total, and threw on: " + raw);
            assertNotNull(state, "decode must never return null, and did for: " + raw);
            // Whatever came back must itself be legal, or a later transition throws instead.
            assertDoesNotThrow(state::effective, "the decoded state is malformed for: " + raw);
        }
        // Mutation: Integer.parseInt without the try/catch -> reddens on "v1;a=1:abc".
    }

    @Test
    void everythingDecodeProducesCanBeReEncoded() {
        // A repaired state must be a legal state, not merely a non-throwing one. If decode could
        // yield something encode cannot express, the next re-mint would write a blob that decodes
        // differently again -- an item drifting a little on every join.
        for (String raw : List.of("v1;unbreaking=9:0", "v1;unbreaking=3,unbreaking=1:0",
                "v1;unbreaking=3:7", "v1;@@@@", "v1;=3,unbreaking=2:0")) {
            EnchantState once = EnchantCodec.decode(raw);
            assertEquals(once, EnchantCodec.decode(EnchantCodec.encode(once)),
                    "decode is not idempotent through encode for: " + raw);
        }
    }
}
