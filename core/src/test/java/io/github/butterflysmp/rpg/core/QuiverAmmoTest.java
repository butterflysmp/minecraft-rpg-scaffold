package io.github.butterflysmp.rpg.core;

import io.github.butterflysmp.rpg.core.weapon.Quiver;
import io.github.butterflysmp.rpg.core.weapon.QuiverState;
import org.junit.jupiter.api.Test;

import java.util.OptionalInt;
import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Slice E: arrows load quivers. The CORE half — what a reload costs, what it yields, and where the
 * no-ammo refusal sits in the verdict ladder.
 *
 * <h2>THE STAGING IS CHOSEN SO NO TWO QUANTITIES COLLIDE</h2>
 *
 * The Boltor's <b>8</b>, a partial load of <b>7</b>, a top-up of <b>5</b> onto <b>3</b>. Every number
 * a row reads differs from every other it could be confused with, so a transposition between
 * {@code loaded} / {@code rounds} / {@code capacity} has nowhere to hide. In particular <b>no row
 * stages arrows equal to the rounds needed</b> — the case that passes whether the {@code min} exists
 * or not.
 *
 * <p>Each test names the mutation it forces red.
 */
class QuiverAmmoTest {

    /** The Boltor. */
    private static final int CAPACITY = 8;
    /** Far enough in the past that nothing is mid-reload. */
    private static final long NOW = 1_000L;

    private static QuiverState idle(int loaded) {
        return QuiverState.from(OptionalInt.of(loaded), OptionalInt.of(CAPACITY), CAPACITY,
                OptionalLong.empty(), OptionalLong.empty());
    }

    // --- What a reload COSTS ------------------------------------------------------------------------

    @Test
    void roundsNeededIsTheGapAndNeverNegative() {
        assertEquals(8, Quiver.roundsNeeded(0, CAPACITY), "an empty 8-round quiver costs 8 arrows");
        assertEquals(1, Quiver.roundsNeeded(7, CAPACITY), "7/8 costs exactly 1, not 8");
        assertEquals(0, Quiver.roundsNeeded(8, CAPACITY), "a full quiver costs nothing");
        assertEquals(0, Quiver.roundsNeeded(11, CAPACITY),
                "OVER-full costs NOTHING rather than refunding -- reachable when a capacity modifier "
                        + "is removed mid-reload, and a negative would invert the debit");
        // Mutation: drop the Math.max(0, ...) -> the over-full row returns -3, which flows into
        // min(needed, available) and makes the reload TAKE a negative number of arrows -> reddens.
    }

    // --- What a reload YIELDS -----------------------------------------------------------------------

    @Test
    void aPartialReloadLoadsWhatWasPaidForAndNoMore() {
        // RULING 5, and the headline row: 7 arrows into an EMPTY 8-round Boltor loads 7.
        assertEquals(7, Quiver.reload(0, 7, CAPACITY), "7 arrows, 8 rounds of room -> 7, not 8");
        // Mutation: reload() ignoring `rounds` and returning capacity -> 8 here -> reddens. That is
        // the pre-slice behaviour wearing the new signature, and this row is what refuses it.
    }

    @Test
    void aTopUpAddsToWhatWasAlreadyThereAndCLAMPS() {
        assertEquals(8, Quiver.reload(3, 5, CAPACITY), "3 + 5 = 8, exactly full");
        assertEquals(8, Quiver.reload(3, 7, CAPACITY),
                "3 + 7 = 10, CLAMPED to 8 -- the clamp is Quiver's and lives in one place");
        assertEquals(5, Quiver.reload(3, 2, CAPACITY), "3 + 2, still short");
        // Mutation: return `rounds` instead of loaded + rounds -> the first row yields 5 -> reddens,
        // and that mutation is the "a reload replaces the magazine" reading of ruling 5.
    }

    @Test
    void aFullReloadIsNotASpecialCaseItIsRoundsEqualsCapacityFromEmpty() {
        // What the MINT path passes, and the reason no second method survives for "full".
        assertEquals(CAPACITY, Quiver.reload(0, CAPACITY, CAPACITY));
        // Mutation: add a reload(capacity) overload back -> this row still passes, which is WHY the
        // overload was refused rather than tested against. The guard is the compile error, not a row.
    }

    // --- WHERE the refusal sits ---------------------------------------------------------------------

    @Test
    void roomButNoArrowsIsNO_AMMOAndNeverALREADY_FULL() {
        // THE ROW THE WHOLE LADDER POSITION EXISTS FOR. 7/8 with nothing to load.
        assertEquals(QuiverState.Reload.NO_AMMO, idle(7).reloadVerdict(NOW, 0),
                "there is room, so it is NOT already full -- and a player shown 'already full' on a "
                        + "weapon reading 7/8 files a bug report");
        // Mutation: move the ammo check ABOVE the full/not-full test -> the row below reddens.
        // Mutation: return ALREADY_FULL here -> this row reddens.
    }

    @Test
    void aFULLMagazineWithNoArrowsIsSTILLAlreadyFull() {
        // THE OTHER SIDE, and it is what pins the check BELOW the full test rather than above it.
        // Nothing was going to be spent, so running dry is not the reason to refuse.
        assertEquals(QuiverState.Reload.ALREADY_FULL, idle(CAPACITY).reloadVerdict(NOW, 0),
                "full is full, whatever the player is carrying");
        // Mutation: move the ammo check above the full test -> NO_AMMO here -> reddens.
    }

    @Test
    void oneArrowIsEnoughToBEGIN() {
        assertEquals(QuiverState.Reload.BEGIN, idle(7).reloadVerdict(NOW, 1),
                "one arrow and one round of room -- a reload that loads exactly 1");
        assertEquals(QuiverState.Reload.BEGIN, idle(3).reloadVerdict(NOW, 2),
                "fewer arrows than room is still a BEGIN -- ruling 5 makes it a PARTIAL reload, "
                        + "not a refusal");
        // Mutation: gate on `roundsAvailable < roundsNeeded` instead of `<= 0` -> the second row
        // becomes NO_AMMO, which is the all-or-nothing behaviour ruling 5 refused -> reddens.
    }

    @Test
    void anUnstampedQuiverIsStillUNSTAMPEDWhateverTheAmmo() {
        // The ladder's first rung is untouched by this slice, and a row says so: an item with no
        // count is a DEFECT to repair on the firing path, not a reload question.
        QuiverState unstamped = QuiverState.from(OptionalInt.empty(), OptionalInt.of(CAPACITY),
                CAPACITY, OptionalLong.empty(), OptionalLong.empty());
        assertEquals(QuiverState.Reload.UNSTAMPED, unstamped.reloadVerdict(NOW, 0));
        assertEquals(QuiverState.Reload.UNSTAMPED, unstamped.reloadVerdict(NOW, 64));
        // Mutation: put the ammo check first -> the first row becomes NO_AMMO -> reddens.
    }

    @Test
    void aRunningReloadIsUNAFFECTEDByTheAmmoCountOnASECONDPress() {
        // RULING 3's consequence inside the ladder: the arrows were taken at the START, so a press
        // during the reload must not re-examine the inventory. ALREADY_RELOADING whatever is carried.
        QuiverState running = QuiverState.from(OptionalInt.of(2), OptionalInt.of(CAPACITY), CAPACITY,
                OptionalLong.of(NOW), OptionalLong.of(NOW + 60));
        assertEquals(QuiverState.Reload.ALREADY_RELOADING, running.reloadVerdict(NOW + 10, 0),
                "out of arrows mid-reload changes nothing -- they are already spent");
        assertEquals(QuiverState.Reload.ALREADY_RELOADING, running.reloadVerdict(NOW + 10, 64));
        // Mutation: move the ammo check above the reloadStartedAt branch -> the first row becomes
        // NO_AMMO, which would ABANDON a paid-for reload because the player ran dry -> reddens.
    }
}
