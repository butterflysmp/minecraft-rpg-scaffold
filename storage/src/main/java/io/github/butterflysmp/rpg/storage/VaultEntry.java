package io.github.butterflysmp.rpg.storage;

import io.github.butterflysmp.rpg.core.vault.VaultShape;

import java.util.Objects;

/**
 * One occupied vault slot: where it is, and the item as opaque text.
 *
 * <h2>*** {@code item} IS OPAQUE HERE AND THAT IS THE WHOLE POINT OF THE SEAM ***</h2>
 *
 * It is a Base64 string and {@code storage} has no idea what is inside it. It cannot have one:
 * decoding an {@code ItemStack} needs {@code Bukkit.getUnsafe()}, and {@code storage} must never
 * import Bukkit -- so the encode and decode live in {@code paper}, on the thread that owns the
 * entity, and only the string crosses.
 *
 * <p>That is {@code CombatantSnapshot}'s rule applied to items: <b>capture a value on the thread that
 * owns it, then hop.</b> A repository that took an {@code ItemStack} would be a repository that
 * cannot be unit-tested, because no test in this project can construct one.
 *
 * <h2>TWO KINDS OF CORRUPTION, TWO DIFFERENT ANSWERS, AND CONFLATING THEM LOSES ITEMS</h2>
 *
 * <pre>
 *   STRUCTURAL   a page or slot out of range, a duplicate slot   -&gt; REFUSE TO LOAD (here)
 *   CODEC        the Base64 is fine but no longer decodes        -&gt; KEEP IT OPAQUE (paper)
 * </pre>
 *
 * Structural damage means the file's shape is not one we wrote, so the honest response is to refuse
 * rather than to load a partial vault and then <b>save that loss back over the good file</b> -- the
 * same reasoning as {@code ProfileMigrations}' newer-server refusal.
 *
 * <p>A codec failure is the opposite case: the file is exactly as we wrote it and one item's bytes
 * cannot be turned into an {@code ItemStack} by THIS server. Refusing there would throw away
 * something a later server might read perfectly. So that entry survives untouched and is written back
 * verbatim. <b>There is no path through either layer that ends with the string deleted.</b>
 */
public record VaultEntry(int page, int slot, String item) {

    public VaultEntry {
        VaultShape.requirePage(page);
        VaultShape.requireSlot(slot);
        Objects.requireNonNull(item, "item");
        if (item.isBlank()) {
            throw new IllegalArgumentException(
                    "vault entry at page " + page + " slot " + slot + " has a blank item;"
                            + " an empty slot is represented by having NO entry, never by a blank one");
        }
    }
}
