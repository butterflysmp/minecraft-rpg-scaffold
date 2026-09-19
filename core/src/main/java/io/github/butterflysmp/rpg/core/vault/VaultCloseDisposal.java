package io.github.butterflysmp.rpg.core.vault;

/**
 * What a closing vault screen does with the cells that are not on disk.
 *
 * <h2>*** THE ARBITRATION, MADE EXECUTABLE -- IT WAS A CORRECT ARGUMENT ABOUT STATEMENT ORDER ***</h2>
 *
 * Two paths can dispose of the unpersisted cells and <b>only one of them may</b>: the close hands
 * them back (or drops them), and an async failure arm drops them if nothing else will. If both ran,
 * every unpersisted stack would exist twice.
 *
 * <p>The rule in one sentence: <b>WHOEVER ACTUALLY DISPOSES OF THE CELLS CLAIMS THE FLAG</b>, and a
 * degraded close disposes of them whether it hands them back or drops them.
 *
 * <h2>WHY IT IS A TYPE AND NOT A CONDITION IN {@code onClose}</h2>
 *
 * The first version was correct and <b>illegible</b>. It claimed the flag on any degraded close, but
 * it did so in a statement AFTER {@code returnEverything}, and its correctness rested on that
 * statement preceding the {@code closed = true} the drop arm gates on. <b>A reviewer read it as
 * broken and described the exact duplication it does not have</b> -- reasonably, because the
 * condition next to the drop names {@code DISCONNECT} and the claim for the other case is eleven
 * lines further down.
 *
 * <p><b>An invariant whose correctness depends on the order of two statements at the end of a long
 * method is a latent defect even while it is right</b>: the next person to reorder them has no
 * warning, and the reviewer who spots nothing wrong has no test to disagree with.
 *
 * <p>So the decision moved here, where both halves are one expression and both are exercised by a
 * unit test. Same reason {@code VaultReturnPolicy} and {@code VaultPageDiff} are in this package:
 * {@code Menu} cannot be constructed without a server, so anything left in the screen can only be
 * read, never run.
 */
public enum VaultCloseDisposal {

    /**
     * Nothing. The file has everything, so the close hands back only the cursor.
     *
     * <p><b>It deliberately does NOT claim</b>: an async failure arriving after this close is the
     * one case the drop arm exists for, and claiming here would silence it.
     */
    NOTHING,

    /**
     * Hand the difference back to the player, through {@code returnEverything}.
     *
     * <p>The ordinary degraded close: the screen is up, the player is there, and their inventory is
     * a better destination than the floor.
     */
    HAND_BACK,

    /**
     * Put the difference on the GROUND.
     *
     * <p>A degraded close caused by a DISCONNECT. {@code MenuSafety.give} is {@code addItem}-first,
     * and on a quit that races the player-data save -- so an item pushed in can vanish with no error
     * anywhere, on the one path where the handed-back copy is the last one.
     */
    DROP;

    /**
     * What this close should do.
     *
     * @param degraded   has a write failed this session? A healthy vault's cells are all on disk.
     * @param disconnect was the close caused by the player leaving, rather than by Esc, a button,
     *                   death or a plugin?
     */
    public static VaultCloseDisposal of(boolean degraded, boolean disconnect) {
        if (!degraded) return NOTHING;
        return disconnect ? DROP : HAND_BACK;
    }

    /**
     * Does this close take responsibility for the unpersisted cells?
     *
     * <h2>*** TRUE FOR HAND_BACK AS WELL AS DROP, AND THAT IS THE WHOLE POINT. ***</h2>
     *
     * {@code returnEverything} disposes of the cells just as surely as a drop does -- it gives them
     * to the player and CLEARS them. A close that handed them back without claiming would leave a
     * later failure callback free to win the flag and put the same items on the floor.
     *
     * <p><b>Folding the disconnect test into the claim is exactly that bug</b>, and
     * {@code MUTCLAIMDISCONNECT} is the mutation that restores it.
     */
    public boolean claimsTheCells() {
        return this != NOTHING;
    }
}
