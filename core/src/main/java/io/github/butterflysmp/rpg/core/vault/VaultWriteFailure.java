package io.github.butterflysmp.rpg.core.vault;

import io.github.butterflysmp.rpg.core.weapon.PageMath;

import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * What a failed vault write says in the log.
 *
 * <h2>*** THIS IS LOAD-BEARING, NOT LOGGING. THE DECISION ABOVE IT RESTS ON THIS STRING. ***</h2>
 *
 * The vault's failure policy accepts a <b>duplication</b> arm rather than a destruction arm, and the
 * whole argument for that choice is <b>recoverability</b>: a duplicate is visible, logged and
 * reversible by an operator, while a destroyed item is silent and final and the player cannot even
 * prove it existed.
 *
 * <p><b>"Reversible by hand" is only true if the line says WHAT to reverse.</b> A message reading
 * <i>"vault write failed"</i> makes the argument aspirational rather than true -- it names no player,
 * no page, no slot and no item, so nothing can be reversed from it and the trade that was accepted
 * on its strength was never actually available.
 *
 * <p>So this is a pure function with a test asserting its <b>content</b>, and that test is not
 * decoration: it is the thing that keeps the failure policy honest. Ben's condition, 2026-09-17.
 *
 * <h2>WHY IT IS IN {@code core} AND NOT BESIDE THE SERVICE THAT LOGS IT</h2>
 *
 * {@code VaultService} is in {@code paper} and can be unit-tested; the SCREEN cannot, because
 * {@code Menu} needs a running server. Putting the assembly here means the line is asserted at the
 * 2-second loop whichever caller produces it, and the only part left untestable is the per-item
 * description -- which needs {@code ItemStack} and therefore a server, and is why {@link VaultCell}
 * carries the description instead of this class deriving it.
 */
public final class VaultWriteFailure {

    private VaultWriteFailure() {}

    /**
     * The whole message: what failed, what it means, and every cell that is not on disk.
     *
     * <p><b>Multi-line on purpose.</b> One {@code SEVERE} record rather than one per slot, because a
     * page holds up to thirty-six and an operator reading a server log wants the incident, not
     * thirty-six lines they have to correlate by timestamp.
     *
     * <p><b>BOTH PAGE NUMBERS APPEAR, AND THAT IS DELIBERATE RATHER THAN BELT-AND-BRACES.</b> The
     * storage index is what the file and {@code /rpg vault} take; {@code index + 1} is what the
     * player sees on the button. An operator matching a player's account of <i>"page 3"</i> against
     * a log line saying <i>"page 2"</i> has to know which convention each end uses -- and the
     * off-by-one between them is exactly what {@code MUT-PAGEIDX} exists to catch, so the incident
     * report states both rather than picking a side.
     *
     * @param playerId whose vault. The UUID rather than a name: the name is not in {@code core}, and
     *                 the file on disk is named for the UUID, which is what an operator will open.
     * @param page     0-based storage page index.
     * @param cells    slot to cell, as handed to the write. <b>Empty cells are skipped</b> -- they
     *                 are not on disk either, and there is nothing to reverse about a cell that
     *                 holds nothing.
     */
    public static String report(UUID playerId, int page, Map<Integer, VaultCell> cells) {
        VaultShape.requirePage(page);

        StringBuilder message = new StringBuilder()
                .append("Vault write FAILED for ").append(playerId)
                .append(": storage page ").append(page)
                .append(" (page ").append(PageMath.displayPage(page)).append(" as the player sees it)")
                .append(" did NOT reach disk.");

        message.append("\n  This vault is now POISONED for the session: no further write is")
                .append(" attempted, the shutdown flush skips it, and the file keeps its last good")
                .append(" contents.");

        // THE SENTENCE THAT MAKES THE LINE ACTIONABLE, and it states a RISK rather than a fact:
        // whether the items are handed back depends on the screen closing, which has not happened
        // when this is written. Claiming they HAVE been returned would be a report about the future.
        message.append("\n  The cells below are on the player's screen and NOT in the file. If the")
                .append(" screen hands them back, they may ALSO still be in the file -- check and")
                .append(" reverse by hand:");

        // Sorted, so two reports of one page read the same way and a diff of two incidents shows
        // what differed rather than what moved. Same reason PlayerVault sorts its entries.
        Map<Integer, VaultCell> ordered = new TreeMap<>(cells);
        boolean any = false;
        for (Map.Entry<Integer, VaultCell> cell : ordered.entrySet()) {
            if (cell.getValue() == null || cell.getValue().isEmpty()) continue;
            any = true;
            message.append("\n    slot ").append(cell.getKey())
                    .append(": ").append(cell.getValue().description());
        }

        // *** AN EMPTY PAGE SAYS SO, RATHER THAN ENDING ON A COLON WITH NOTHING UNDER IT. ***
        //
        // A failed write of an EMPTY page is a real event and the most likely one to be dismissed:
        // it is what a player taking their LAST item out produces, and it means the file still holds
        // that item while the player is walking away with it. The duplication arm, with no cell to
        // list, because the thing that is wrong is what the page no longer contains.
        if (!any) {
            message.append("\n    (no cells -- the page was being CLEARED, so the file still holds")
                    .append(" whatever it held before; the player may now have it as well)");
        }

        return message.toString();
    }
}
