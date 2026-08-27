package io.github.butterflysmp.rpg.paper.menu;

import io.github.butterflysmp.rpg.core.enchant.EnchantCandidate;
import io.github.butterflysmp.rpg.core.enchant.EnchantCost;
import io.github.butterflysmp.rpg.core.enchant.EnchantLoreLines;
import io.github.butterflysmp.rpg.core.enchant.EnchantSlot;
import io.github.butterflysmp.rpg.core.enchant.EnchantState;
import io.github.butterflysmp.rpg.core.weapon.WeaponClass;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import io.github.butterflysmp.rpg.core.weapon.WeaponRegistry;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.content.EnchantDefinition;
import io.github.butterflysmp.rpg.paper.weapon.EnchantEffectLine;
import io.github.butterflysmp.rpg.paper.weapon.EnchantItems;
import io.github.butterflysmp.rpg.paper.weapon.WeaponItems;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static io.github.butterflysmp.rpg.paper.menu.EnchantMenuLayout.BOOKSHELF_SLOT;
import static io.github.butterflysmp.rpg.paper.menu.EnchantMenuLayout.CLOSE_SLOT;
import static io.github.butterflysmp.rpg.paper.menu.EnchantMenuLayout.INFO_SLOT;
import static io.github.butterflysmp.rpg.paper.menu.EnchantMenuLayout.INPUT_SLOT;

/**
 * The enchant table: what a player sees instead of vanilla enchanting.
 *
 * <p>Place a weapon in the input slot and its enchant slots render as three columns of candidates.
 * Every candidate shows its REAL identity in every state -- icon, name, level, and what it actually
 * does on the weapon it is sitting on -- including a locked one. The table exists so a player can
 * decide what to spend on, and a locked candidate rendered as "???" removes the informed choice the
 * whole screen is for.
 *
 * <p>Unlocks are FREE this pass. The XP economy gates the same click, and gates it in FRONT of
 * {@code EnchantClickIntent}, never inside it -- what a click MEANS and whether you can afford it
 * are different questions.
 */
public final class EnchantMenu extends Menu {

    private final WeaponRegistry weapons;
    private final AdapterContext adapters;

    /**
     * This table's bookshelf power, counted ONCE when the menu opened and never again.
     *
     * <p>Frozen deliberately. "Place shelves, then reopen" is a fine interaction, and freezing buys
     * the Folia-correct thing for free: the count runs inside the interact event for the very block
     * that was clicked, on the thread that owns it, where a re-read from an inventory click would
     * not be. The {@code Block} itself is NOT kept, so there is nothing here for a later re-read to
     * be written against.
     */
    private final int bookshelfPower;

    public EnchantMenu(Player viewer, WeaponRegistry weapons, AdapterContext adapters, Block table) {
        super(viewer, EnchantMenuLayout.SIZE,
                MenuIcons.line("Enchantments", NamedTextColor.DARK_GRAY));
        this.weapons = weapons;
        this.adapters = adapters;
        // BEFORE render(), which paints the readout from it. Assigned after, every table on the
        // server reads 0/30 for ever, and only a boot gate with a ring built round it would notice.
        this.bookshelfPower = BookshelfPower.at(table);
        render();
    }

    /** The single named exception to the menu's cancel-everything rule. */
    @Override
    protected Set<Integer> inputSlots() {
        return Set.of(INPUT_SLOT);
    }

    /**
     * Three checks, all against the cursor, all before the place is permitted -- so a refusal is a
     * click that did nothing and the item never leaves the player's hand.
     */
    @Override
    protected boolean acceptsInput(ItemStack cursor) {
        if (WeaponItems.weaponId(cursor, adapters.keys()).isEmpty()) {
            say("That is not one of your weapons.", NamedTextColor.GRAY);
            return false;
        }

        // Two shipped weapons mint on STACKABLE materials -- ember_staff is a blaze_rod, and two
        // fresh mints share identical meta, so a stack of 2 is constructible with /rpg give alone.
        // One write would enchant both, and the re-mint would then collapse the stack to one.
        if (cursor.getAmount() != 1) {
            say("One weapon at a time.", NamedTextColor.GRAY);
            return false;
        }

        // Refuse rather than render the first nine cells: the extra slots survive every transition
        // and keep working, so truncating leaves an enchant that is ACTIVE and INVISIBLE.
        Optional<String> problem =
                EnchantMenuLayout.overflow(EnchantItems.read(cursor, adapters.keys()));
        if (problem.isPresent()) {
            say("This weapon carries more than this table can show (" + problem.get()
                    + "). Use /rpg enchant show to read it.", NamedTextColor.RED);
            // warnOnce, not warning: a player can re-attempt the place as often as they like.
            adapters.warnOnce("An item reached the enchant table carrying " + problem.get()
                    + ". The table refused it rather than showing part of it.");
            return false;
        }
        return true;
    }

    @Override
    protected void onClick(MenuClick click) {
        if (click.slot() == CLOSE_SLOT) {
            // Closes, and nothing else. The weapon comes back through onClose -- the SAME path Esc
            // takes -- so the button and the escape key cannot drift apart, because there is only
            // one of them.
            viewer.closeInventory();
            return;
        }

        if (click.itemMoved()) {
            // The weapon has NOT landed yet: InventoryClickEvent fires before the place applies.
            // Repaint next tick, when the slot holds what the player thinks it holds. onEntityLater
            // is the sanctioned route and clamps a zero delay to one tick.
            adapters.scheduler().onEntityLater(viewer, this::render, 1);
        }

        // A candidate cell, or chrome. cellAt returns empty for filler, chrome, and every raw slot
        // in the player's own inventory, so a click that is not a candidate simply is not one.
        EnchantMenuLayout.cellAt(click.slot())
                .ifPresent(cell -> applyCandidateClick(cell.slot(), cell.candidate()));
    }

    @Override
    protected void onClose(InventoryCloseEvent.Reason reason) {
        returnEverything();
    }

    /**
     * Repaint every slot the menu owns.
     *
     * <p><b>NEVER writes {@link EnchantMenuLayout#INPUT_SLOT}.</b> It reads the weapon there and
     * leaves it exactly alone. The only two writers of that slot in this class are the re-mint
     * after a successful click and {@code returnEverything}; a repaint that clobbered or re-minted
     * it would be the same failure class as a bad click route, so it is stated rather than assumed.
     */
    private void render() {
        for (int slot = 0; slot < EnchantMenuLayout.SIZE; slot++) {
            if (slot == INPUT_SLOT) continue;                      // the player's weapon lives here
            getInventory().setItem(slot, MenuIcons.filler());
        }

        getInventory().setItem(CLOSE_SLOT, MenuIcons.close());
        getInventory().setItem(BOOKSHELF_SLOT, bookshelfIcon());

        ItemStack placed = getInventory().getItem(INPUT_SLOT);
        WeaponDefinition weapon = WeaponItems.weaponId(placed, adapters.keys())
                .flatMap(weapons::find).orElse(null);

        if (weapon == null) {
            getInventory().setItem(INFO_SLOT, MenuIcons.icon(Material.ENCHANTING_TABLE,
                    MenuIcons.line("Enchanting", NamedTextColor.WHITE),
                    List.of(MenuIcons.line("Place a weapon above to see", NamedTextColor.GRAY),
                            MenuIcons.line("the enchants it can carry.", NamedTextColor.GRAY),
                            MenuIcons.blank(),
                            MenuIcons.line("Unlocks are free for now.", NamedTextColor.DARK_GRAY))));
            return;
        }

        getInventory().setItem(INFO_SLOT, MenuIcons.icon(Material.ENCHANTING_TABLE,
                MenuIcons.line("Enchanting", NamedTextColor.WHITE),
                List.of(MenuIcons.line("Click a candidate to unlock it,", NamedTextColor.GRAY),
                        MenuIcons.line("to make it active, or to level it.", NamedTextColor.GRAY),
                        MenuIcons.blank(),
                        MenuIcons.line("Swapping keeps the level you paid for.",
                                NamedTextColor.DARK_GRAY))));

        renderCandidates(EnchantItems.read(placed, adapters.keys()), weapon.weaponClass());
    }

    /**
     * The bookshelf readout, which is a real measurement now rather than a labelled placeholder.
     *
     * <p><b>A counted zero is not the zero the placeholder was avoiding.</b> The previous pass
     * deliberately said "Not implemented yet" rather than rendering {@code 0%}, because a readout
     * showing a zero when nothing is measured is indistinguishable from a working one that measured
     * zero. Printing the SCALE beside the count is what settles that: "0/30" is legible as a
     * measurement against a known maximum, where a bare "0%" was not.
     */
    private ItemStack bookshelfIcon() {
        return MenuIcons.icon(Material.BOOKSHELF,
                MenuIcons.line("Bookshelf Power " + bookshelfPower + "/" + EnchantCost.MAX_POWER,
                        NamedTextColor.DARK_GRAY),
                List.of(MenuIcons.line("Shelves in a ring around the table.", NamedTextColor.DARK_GRAY)));
    }

    /** The three columns. A slot that rolled fewer than three candidates leaves filler behind. */
    private void renderCandidates(EnchantState state, WeaponClass heldClass) {
        for (int slot = 0; slot < EnchantMenuLayout.SLOTS && slot < state.slots().size(); slot++) {
            EnchantSlot enchantSlot = state.slots().get(slot);
            for (int index = 0; index < EnchantMenuLayout.CANDIDATES
                    && index < enchantSlot.candidates().size(); index++) {
                getInventory().setItem(EnchantMenuLayout.rawSlotFor(slot, index),
                        candidateIcon(enchantSlot.candidates().get(index),
                                enchantSlot.activeIndex() == index, heldClass));
            }
        }
    }

    /**
     * One candidate cell.
     *
     * <p>The four states differ ONLY cosmetically -- the icon, the name colour, and the last lore
     * line. The enchant's identity and its effect are on every one of them.
     */
    private ItemStack candidateIcon(EnchantCandidate candidate, boolean active, WeaponClass heldClass) {
        EnchantDefinition definition =
                adapters.enchants().find(candidate.enchantId()).orElse(null);

        String name = definition != null ? definition.displayName() : candidate.enchantId();
        int maxLevel = definition != null ? definition.maxLevel() : EnchantState.MAX_LEVEL;
        boolean locked = candidate.isLocked();

        // A LOCKED candidate is described at the level it would BECOME. Describing it at its own
        // level 0 would read "+0% damage" for a damage enchant and, worse, "consumes durability on
        // 100% of uses" for Unbreaking -- backwards, and reads as a curse. The "unlock at I" line
        // below says the same number, so the two agree by construction.
        int describedLevel = Math.max(1, candidate.level());

        List<Component> lore = new ArrayList<>();
        lore.add(MenuIcons.line(EnchantEffectLine.bare(definition, describedLevel, heldClass),
                NamedTextColor.GRAY));
        lore.add(MenuIcons.blank());
        if (locked) {
            lore.add(MenuIcons.line("Locked. Click to unlock at "
                    + EnchantLoreLines.romanNumeral(1) + ".", NamedTextColor.DARK_GRAY));
        } else if (active) {
            lore.add(MenuIcons.line("Active on this weapon.", NamedTextColor.GREEN));
            if (candidate.level() < Math.min(maxLevel, EnchantState.MAX_LEVEL)) {
                lore.add(MenuIcons.line("Click to raise its level.", NamedTextColor.GRAY));
            }
        } else {
            lore.add(MenuIcons.line("Unlocked. Click to make it active.", NamedTextColor.GRAY));
        }

        // Level 0 has no numeral, so a locked candidate reads as its bare name.
        Component title = MenuIcons.line(
                EnchantLoreLines.label(name, candidate.level(), maxLevel),
                locked ? NamedTextColor.DARK_GRAY : active ? NamedTextColor.GREEN : NamedTextColor.WHITE);

        ItemStack icon = MenuIcons.icon(iconMaterial(definition, locked), title, lore);
        if (active) {
            icon.editMeta(meta -> meta.setEnchantmentGlintOverride(true));
            // Set ONLY on the active one. Left unset -- not false -- everywhere else: null means
            // "vanilla decides", and vanilla decides no glint because we add no enchantments. The
            // omission is deliberate, not a forgotten branch.
        }
        return icon;
    }

    /**
     * The material an enchant renders as, from its content file.
     *
     * <p>A locked candidate keeps its own icon and is greyed by its NAME rather than by a
     * substituted item: swapping in a padlock would hide which enchant it is, which is the one
     * thing this screen must not do.
     *
     * <p>Fail-soft, the same shape as {@code WeaponItems.materialOf}: an unresolvable name falls
     * back rather than throwing. ContentValidator has already named it at boot, so this does not
     * need to be loud a second time -- but warnOnce catches a definition that never went through
     * the loader.
     */
    private Material iconMaterial(EnchantDefinition definition, boolean locked) {
        if (definition == null) return Material.ENCHANTED_BOOK;
        Material resolved = Material.matchMaterial(definition.icon());
        if (resolved == null) {
            adapters.warnOnce("enchant '" + definition.id() + "' names icon '" + definition.icon()
                    + "', which is not a material; rendering it as "
                    + EnchantDefinition.DEFAULT_ICON);
            return Material.ENCHANTED_BOOK;
        }
        return resolved;
    }

    /**
     * A candidate click, applied.
     *
     * <p>THE SAME SEVEN STEPS {@code /rpg enchant} takes, in the same order, for the same reasons:
     * resolve the weapon, read the state, validate in English, transform, reject the no-op, write,
     * RE-MINT. The only difference is where the item lives -- an input slot rather than the main
     * hand. No scheduler hop: {@code InventoryClickEvent} already fires on this player's thread.
     *
     * <p><b>Never patches lore.</b> Every edit routes through {@code WeaponItems.remint}, which
     * rebuilds the tooltip from the state that just landed and carries the enchant blob across
     * raw. Patching would let the enchant block double or go stale.
     */
    private void applyCandidateClick(int slotIndex, int candidateIndex) {
        ItemStack input = getInventory().getItem(INPUT_SLOT);
        if (input == null || input.getType().isAir()) {
            say("Put one of your weapons in the slot above.", NamedTextColor.GRAY);
            return;
        }

        // DEFENCE IN DEPTH, and NOT a duplicate of acceptsInput's identical-looking check. THIS is
        // the operation that mints: editMeta would enchant every item in a stack, and remint
        // returns a FRESH stack of amount 1, silently collapsing a stack of two into one. That is
        // item destruction rather than a cosmetic glitch, so the operation that can destroy guards
        // itself instead of trusting the insert seam to have held.
        if (input.getAmount() != 1) {
            say("Only one weapon at a time -- take the stack out and re-insert a single one.",
                    NamedTextColor.RED);
            return;
        }

        String weaponId = WeaponItems.weaponId(input, adapters.keys()).orElse(null);
        if (weaponId == null) {
            say("Put one of your weapons in the slot above.", NamedTextColor.GRAY);
            return;
        }

        // A re-mint needs the definition. REFUSE rather than half-edit: writing state we cannot
        // re-mint leaves an item whose PDC and whose lore disagree, which is worse than nothing.
        WeaponDefinition definition = weapons.find(weaponId).orElse(null);
        if (definition == null) {
            say("'" + weaponId + "' has no content file loaded -- cannot re-mint it, so refusing"
                    + " to edit its enchants.", NamedTextColor.RED);
            return;
        }

        EnchantState before = EnchantItems.read(input, adapters.keys());
        if (slotIndex >= before.slots().size()) return;                   // filler, not a refusal
        EnchantSlot slot = before.slots().get(slotIndex);
        if (candidateIndex >= slot.candidates().size()) return;           // filler, not a refusal

        EnchantCandidate candidate = slot.candidates().get(candidateIndex);
        EnchantDefinition enchant = adapters.enchants().find(candidate.enchantId()).orElse(null);
        String name = enchant != null ? enchant.displayName() : candidate.enchantId();
        EnchantClickIntent intent = EnchantClickIntent.of(slot, candidateIndex, enchant);

        EnchantState after;
        try {
            after = switch (intent) {
                // ORDER IS LOAD-BEARING: withActive REFUSES a locked candidate, so the level has to
                // land first and the activation has to run on the RESULT, not on `before`.
                case UNLOCK -> before.withLevel(slotIndex, candidateIndex, 1)
                        .withActive(slotIndex, candidateIndex);
                case ACTIVATE -> before.withActive(slotIndex, candidateIndex);
                case LEVEL_UP -> before.withLevel(slotIndex, candidateIndex, candidate.level() + 1);
                case AT_MAX -> {
                    say(name + " is already at its maximum.", NamedTextColor.GRAY);
                    yield before;
                }
                case UNKNOWN_ENCHANT -> {
                    say("'" + candidate.enchantId() + "' has no content file loaded -- refusing to"
                            + " change a level nothing defines a maximum for.", NamedTextColor.RED);
                    yield before;
                }
                case EMPTY -> before;
            };
        } catch (IllegalArgumentException ex) {
            // The model's own refusal, verbatim. It knows why better than a paraphrase here would.
            say(ex.getMessage(), NamedTextColor.RED);
            return;
        }

        // Records give equals() free, so a no-op is exact rather than inferred. The arms above have
        // already said why, which is what keeps a handled no-op from looking like a dead click.
        if (after.equals(before)) return;

        input.editMeta(meta -> EnchantItems.write(meta, after, adapters.keys()));
        // remint returns a NEW stack, so `input` is stale from here and render() must re-read.
        getInventory().setItem(INPUT_SLOT, WeaponItems.remint(input, definition, adapters));
        render();
        viewer.updateInventory();

        say(feedbackFor(intent, name, after, candidate.enchantId(), definition.weaponClass()),
                NamedTextColor.GRAY);

        // No stat reconcile: PlayerHealthSystem re-reads the held weapon every scan tick, and the
        // weapon is not held while it is sitting in this menu.
    }

    /**
     * What just happened, and what it means ON THIS WEAPON.
     *
     * <p>Routed through {@code EnchantEffectLine} so the reply says "inert: a Melee enchant on a
     * Ranged weapon" when that is the truth -- the moment the mistake is correctable is right after
     * making it, not on a later inspection. Same reasoning that put the line on
     * {@code /rpg enchant active}.
     */
    private String feedbackFor(EnchantClickIntent intent, String name, EnchantState after,
                               String enchantId, WeaponClass heldClass) {
        EnchantDefinition enchant = adapters.enchants().find(enchantId).orElse(null);
        int level = after.activeLevel(enchantId);
        String effect = EnchantEffectLine.of(enchant, Math.max(1, level), heldClass);

        return switch (intent) {
            case UNLOCK -> "Unlocked " + name + " I and made it active." + effect;
            case ACTIVATE -> name + " is now active." + effect;
            case LEVEL_UP -> name + " is now "
                    + EnchantLoreLines.romanNumeral(level) + "." + effect;
            // Unreachable: these three arms return `before`, so equals() short-circuits above them.
            // Kept because the switch is exhaustive and a silent default arm is how a new intent
            // ships with no words.
            case AT_MAX, UNKNOWN_ENCHANT, EMPTY -> name + " is unchanged.";
        };
    }

    private void say(String message, NamedTextColor color) {
        viewer.sendMessage(Component.text(message, color));
    }
}
