package io.github.butterflysmp.rpg.paper.vault;

import org.bukkit.inventory.ItemStack;

import java.util.Base64;
import java.util.Optional;

/**
 * Turns an {@code ItemStack} into text a vault file can hold, and back.
 *
 * <h2>*** THE PER-ITEM PRIMITIVE, NOT PAPER'S ARRAY CONVENIENCE. THREE REASONS. ***</h2>
 *
 * Paper offers {@code ItemStack.serializeItemsAsBytes(Collection)} and
 * {@code deserializeItemsFromBytes(byte[])}, which would encode a whole page in one call. Measured
 * against the pinned {@code paper-api-26.1.2.build.74-stable} sources rather than assumed:
 *
 * <ol>
 *   <li><b>That format carries a version byte we do not control.</b> It writes a private
 *       {@code ARRAY_SERIALIZATION_VERSION = 1} and the reader <b>throws
 *       {@code IllegalArgumentException} on any value but {@code 1}</b>. A Paper bump to 2 would
 *       make every existing vault unreadable, and the file it guards holds players' items.</li>
 *   <li><b>{@code serializeAsBytes} is the documented migration-safe primitive.</b> Its javadoc:
 *       <i>"NBT is safer for data migrations as it will use the built in data converter"</i>, with
 *       the DataVersion stored at the root of the compound. That is what carries a stored weapon
 *       across a Minecraft upgrade.</li>
 *   <li><b>Blast radius, and this is the one that decided it.</b> One entry per slot means one
 *       unreadable item costs ONE SLOT. The array form throws for the whole page, so a single bad
 *       blob would take the other thirty-five items with it.</li>
 * </ol>
 *
 * <h2>WHAT THE BYTES PRESERVE, AND WHY A KEY-BY-KEY COPIER WAS REJECTED</h2>
 *
 * NBT carries the whole item: every PDC key ({@code weapon_id}, {@code enchant_data},
 * {@code enchant_rolled}, the four quiver keys), durability, the display name, the lore, the
 * attribute modifiers and the glint override.
 *
 * <p>A projection that copied known keys would have to restate {@code GearItems.carryInstanceData}'s
 * list, and <b>would go stale silently the next time a key was added</b> -- which is the exact
 * failure that class documents about itself. A bytes-level round trip has no list to forget.
 *
 * <h2>*** BOOT-GATE-ONLY. NOTHING HERE CAN BE UNIT-TESTED, ANYWHERE. ***</h2>
 *
 * Both directions route through {@code Bukkit.getUnsafe()}, and {@code new ItemStack(...)} throws
 * <i>"No RegistryAccess implementation found"</i> without a running server -- <b>the project has no
 * MockBukkit</b>. So there is no module in which a test of this class could be written, and the
 * round trip is witnessed by the boot gate instead. That is why the vault's SEAM is the string:
 * everything on the other side of it is testable at the two-second loop.
 */
public final class VaultCodec {

    private VaultCodec() {}

    /**
     * An item as storable text, or {@code null} for nothing worth storing.
     *
     * <p><b>Null out means "no entry", which is how an empty slot is represented</b> -- absence, not
     * a blank string. The three empty shapes the API produces are all folded here, because
     * {@code getItem} on an empty slot returns null, a cleared slot can come back as AIR, and a
     * decremented stack can be a live {@code ItemStack} of amount zero. Testing one of them is the
     * classic way a menu ends up storing an invisible nothing.
     */
    public static String encode(ItemStack item) {
        if (item == null || item.getType().isAir() || item.getAmount() <= 0) return null;
        return Base64.getEncoder().encodeToString(item.serializeAsBytes());
    }

    /**
     * Text back into an item, or empty if THIS server cannot read it.
     *
     * <h2>EMPTY IS NOT AN INSTRUCTION TO DELETE ANYTHING</h2>
     *
     * A failure here means the bytes are intact and this build cannot turn them into an item -- a
     * data version the converter will not take, a component that no longer exists, a truncated blob.
     * <b>The caller keeps the original text and writes it back verbatim</b>, because a later build
     * may read it perfectly and discarding it would be the one outcome that cannot be undone.
     *
     * <p>Returning {@code Optional} rather than null is what makes that hard to get wrong: a null
     * would flow into the same code path as "empty slot" and the entry would quietly stop being
     * written.
     */
    public static Optional<ItemStack> decode(String text) {
        if (text == null || text.isBlank()) return Optional.empty();
        try {
            ItemStack item = ItemStack.deserializeBytes(Base64.getDecoder().decode(text));
            return item == null || item.getType().isAir() ? Optional.empty() : Optional.of(item);
        } catch (RuntimeException e) {
            // Deliberately broad, and deliberately silent about the CONTENT. Base64 throws
            // IllegalArgumentException, the NBT reader throws whatever it likes, and neither is
            // worth distinguishing: the answer is the same either way -- this server cannot read
            // it, so leave it alone.
            return Optional.empty();
        }
    }
}
