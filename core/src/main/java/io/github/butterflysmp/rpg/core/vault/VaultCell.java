package io.github.butterflysmp.rpg.core.vault;

import java.util.Objects;

/**
 * One cell on its way INTO the vault: the opaque item text, and a human description of it.
 *
 * <h2>*** THE DESCRIPTION IS NEVER PERSISTED. IT EXISTS FOR ONE LOG LINE. ***</h2>
 *
 * The description is not a field on the stored record and must not become one -- the
 * file holds {@code item} and nothing else. This pair exists so that {@link VaultWriteFailure} can
 * name what did not reach disk in a form <b>an operator can match against what the player is
 * holding</b>, and the encoded text cannot do that: it is Base64 NBT.
 *
 * <h2>WHY A PAIR RATHER THAN TWO PARAMETERS, AND IT IS NOT TIDINESS</h2>
 *
 * {@code VaultService.writePage} would otherwise take {@code Map<Integer, String> contents} and
 * {@code Map<Integer, String> descriptions} <b>side by side, same key type, same value type</b> --
 * a transposable adjacent pair, which this project's own fixture rule exists to eliminate. Swap
 * them and every item in the vault becomes the string {@code "DIAMOND_SWORD x1"}, the write
 * succeeds, and nothing goes red until someone opens the page.
 *
 * <p><b>It is NOT {@code VaultEntry} in a different costume.</b> That record is persistence -- a
 * {@code (page, slot, item)} triple that Gson writes. This is a write-time argument that never
 * leaves memory, and it carries a field the other must never have.
 *
 * @param item        the encoded item, exactly as it will be stored. Blank or null means the cell is
 *                    empty, which is the normal case and not an error.
 * @param description what to call it in a failure log: {@code "DIAMOND_SWORD x1 (Boltor)"}. Never
 *                    null -- pass {@link #UNDESCRIBED} when there is genuinely nothing to say, so
 *                    that a missing description is a visible string in the log rather than a
 *                    {@code null} printed as "null".
 */
public record VaultCell(String item, String description) {

    /**
     * What an item with no available description is called.
     *
     * <p>Reached by an entry that was loaded from disk and could not be DECODED -- there is no
     * {@code ItemStack} to name, and the bytes are kept verbatim rather than discarded. So the one
     * place this appears in a log is also the one place where the cell's content is a mystery to
     * this server, which is worth saying in those words.
     */
    public static final String UNDESCRIBED = "UNDECODABLE -- kept verbatim";

    public VaultCell {
        Objects.requireNonNull(description, "description");
    }

    /** An empty cell: nothing stored, nothing to describe. */
    public static VaultCell empty() {
        return new VaultCell(null, UNDESCRIBED);
    }

    /** Does this cell hold anything? Blank counts as empty, as {@code PlayerVault.withPage} does. */
    public boolean isEmpty() {
        return item == null || item.isBlank();
    }
}
