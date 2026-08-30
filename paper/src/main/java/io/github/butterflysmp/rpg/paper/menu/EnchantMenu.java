package io.github.butterflysmp.rpg.paper.menu;

import io.github.butterflysmp.rpg.core.enchant.EnchantCandidate;
import io.github.butterflysmp.rpg.core.enchant.EnchantCost;
import io.github.butterflysmp.rpg.core.enchant.EnchantLoreLines;
import io.github.butterflysmp.rpg.core.enchant.EnchantSlot;
import io.github.butterflysmp.rpg.core.enchant.EnchantState;
import io.github.butterflysmp.rpg.core.weapon.GearClass;
import io.github.butterflysmp.rpg.core.weapon.ShieldDefinition;
import io.github.butterflysmp.rpg.core.weapon.ArmorDefinition;
import io.github.butterflysmp.rpg.core.weapon.ArmorRegistry;
import io.github.butterflysmp.rpg.core.weapon.ShieldRegistry;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import io.github.butterflysmp.rpg.core.weapon.WeaponRegistry;
import io.github.butterflysmp.rpg.core.xp.XpCurve;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.content.EnchantDefinition;
import io.github.butterflysmp.rpg.paper.weapon.EnchantEffectLine;
import io.github.butterflysmp.rpg.paper.weapon.EnchantItems;
import io.github.butterflysmp.rpg.paper.weapon.ArmorItems;
import io.github.butterflysmp.rpg.paper.weapon.ShieldItems;
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
 * <p><b>Unlocking and levelling up cost XP; swapping does not.</b> The cost check sits in FRONT of
 * {@code EnchantClickIntent}, never inside it -- what a click MEANS and whether you can afford it
 * are different questions, and {@code EnchantCharge} answers the second only after the first.
 *
 * <p>The price is in XP POINTS, not levels, because levels are not a linear currency: 40 of them is
 * 2920 points and 20 is 550. Discounting the level COUNT would be a different discount at every rung
 * and a far bigger one than it claimed -- 30% off III would really have been 59%. See
 * {@code EnchantCost}, and {@code XpCurve} for the conversion.
 *
 * <p>The deduction is the LAST mutation in {@code applyCandidateClick}, which is why there is no
 * rollback anywhere in this class: every path that can refuse sits above it.
 */
public final class EnchantMenu extends Menu {

    private final WeaponRegistry weapons;
    private final ShieldRegistry shields;
    private final ArmorRegistry armor;
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

    public EnchantMenu(Player viewer, WeaponRegistry weapons, ShieldRegistry shields,
                       ArmorRegistry armor, AdapterContext adapters, Block table) {
        super(viewer, EnchantMenuLayout.SIZE,
                MenuIcons.line("Enchantments", NamedTextColor.DARK_GRAY));
        this.weapons = weapons;
        this.shields = shields;
        this.armor = armor;
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
     * The gear in the input slot: exactly one of a weapon, a shield or a piece of armor.
     *
     * <p><b>ONE record, ONE dispatch</b>, deliberately shape-aligned with {@code RpgCommand.HeldGear}
     * -- same members, same meanings. The alternative was forking every site that reads a class or
     * re-mints into per-kind arms, which triples them and guarantees they drift.
     *
     * <p><b>Both accessors were TWO-WAY TERNARIES until armor arrived, and that is the trap this
     * comment exists to mark.</b> {@code weapon != null ? ... : shield} reads as a complete
     * dispatch and is not one: adding a third field without rewriting both would have sent every
     * piece of armor down the shield branch, minting a helmet as a shield with no compiler
     * complaint anywhere. Three kinds means an if-chain, not a ternary.
     */
    private record PlacedGear(String id, WeaponDefinition weapon, ShieldDefinition shield,
                              ArmorDefinition armor) {

        /**
         * The enchant-gating class. NEVER NULL: a weapon maps through {@code of()}, a shield IS
         * SHIELD, a piece of armor IS ARMOR.
         */
        GearClass gearClass() {
            if (weapon != null) return GearClass.of(weapon.weaponClass());
            return shield != null ? GearClass.SHIELD : GearClass.ARMOR;
        }

        /** Returns a NEW stack, so the caller's reference is stale afterwards. */
        ItemStack remint(ItemStack placed, AdapterContext adapters) {
            if (weapon != null) return WeaponItems.remint(placed, weapon, adapters);
            if (shield != null) return ShieldItems.remint(placed, shield, adapters);
            return ArmorItems.remint(placed, armor, adapters);
        }
    }

    /**
     * Resolve the placed item, or say why not and return null.
     *
     * <p>Returns null having ALREADY messaged the player when there is something to explain, so
     * callers do a bare {@code if (gear == null) return;} -- the same contract
     * {@code RpgCommand.resolveHeldGear} keeps.
     *
     * <p>An EMPTY slot is silent rather than refused: {@code render} calls this on every repaint,
     * including the one for an empty table, and a chat line per repaint would be spam. The info
     * icon is what speaks there.
     *
     * <p>Weapons are checked first, matching {@code /rpg give} and {@code resolveHeldGear}. An item
     * cannot legitimately carry both tags -- nothing mints one that way.
     */
    private PlacedGear resolveGear(ItemStack placed) {
        if (placed == null || placed.getType().isAir()) return null;

        String weaponId = WeaponItems.weaponId(placed, adapters.keys()).orElse(null);
        if (weaponId != null) {
            WeaponDefinition definition = weapons.find(weaponId).orElse(null);
            if (definition == null) {
                say("'" + weaponId + "' has no content file loaded -- cannot re-mint it, so"
                        + " refusing to edit its enchants.", NamedTextColor.RED);
                return null;
            }
            return new PlacedGear(weaponId, definition, null, null);
        }

        String shieldId = ShieldItems.shieldId(placed, adapters.keys()).orElse(null);
        if (shieldId != null) {
            ShieldDefinition definition = shields.find(shieldId).orElse(null);
            if (definition == null) {
                say("'" + shieldId + "' has no content file loaded -- cannot re-mint it, so"
                        + " refusing to edit its enchants.", NamedTextColor.RED);
                return null;
            }
            return new PlacedGear(shieldId, null, definition, null);
        }

        String armorId = ArmorItems.armorId(placed, adapters.keys()).orElse(null);
        if (armorId != null) {
            ArmorDefinition definition = armor.find(armorId).orElse(null);
            if (definition == null) {
                say("'" + armorId + "' has no content file loaded -- cannot re-mint it, so"
                        + " refusing to edit its enchants.", NamedTextColor.RED);
                return null;
            }
            return new PlacedGear(armorId, null, null, definition);
        }

        return null;   // not ours; acceptsInput already refused it at the door
    }

    /**
     * Three checks, all against the cursor, all before the place is permitted -- so a refusal is a
     * click that did nothing and the item never leaves the player's hand.
     */
    @Override
    protected boolean acceptsInput(ItemStack cursor) {
        if (WeaponItems.weaponId(cursor, adapters.keys()).isEmpty()
                && ShieldItems.shieldId(cursor, adapters.keys()).isEmpty()
                && ArmorItems.armorId(cursor, adapters.keys()).isEmpty()) {
            say("That is not one of your weapons, shields or armor.", NamedTextColor.GRAY);
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
        PlacedGear gear = resolveGear(placed);

        if (gear == null) {
            getInventory().setItem(INFO_SLOT, MenuIcons.icon(Material.ENCHANTING_TABLE,
                    MenuIcons.line("Enchanting", NamedTextColor.WHITE),
                    List.of(MenuIcons.line("Place a weapon, shield or piece of armor above", NamedTextColor.GRAY),
                            MenuIcons.line("to see the enchants it can carry.", NamedTextColor.GRAY),
                            MenuIcons.blank(),
                            MenuIcons.line("Unlocks are paid for in XP.", NamedTextColor.DARK_GRAY))));
            return;
        }

        getInventory().setItem(INFO_SLOT, MenuIcons.icon(Material.ENCHANTING_TABLE,
                MenuIcons.line("Enchanting", NamedTextColor.WHITE),
                List.of(MenuIcons.line("Click a candidate to unlock it,", NamedTextColor.GRAY),
                        MenuIcons.line("to make it active, or to level it.", NamedTextColor.GRAY),
                        MenuIcons.blank(),
                        MenuIcons.line("Swapping keeps the level you paid for.",
                                NamedTextColor.DARK_GRAY))));

        renderCandidates(EnchantItems.read(placed, adapters.keys()), gear.gearClass());
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
        // clampPower, not bookshelfPower directly: the percentage shown and the percentage charged
        // come out of ONE expression, so the number here cannot drift from the number on the cells.
        return MenuIcons.icon(Material.BOOKSHELF,
                MenuIcons.line("Bookshelf Power " + bookshelfPower + "/" + EnchantCost.MAX_POWER,
                        NamedTextColor.DARK_GRAY),
                List.of(MenuIcons.line(EnchantCost.clampPower(bookshelfPower)
                                + "% off unlocks and level-ups.", NamedTextColor.DARK_GRAY),
                        MenuIcons.line("Shelves in a ring around the table.", NamedTextColor.DARK_GRAY)));
    }

    /** The three columns. A slot that rolled fewer than three candidates leaves filler behind. */
    private void renderCandidates(EnchantState state, GearClass heldClass) {
        for (int slot = 0; slot < EnchantMenuLayout.SLOTS && slot < state.slots().size(); slot++) {
            EnchantSlot enchantSlot = state.slots().get(slot);
            for (int index = 0; index < EnchantMenuLayout.CANDIDATES
                    && index < enchantSlot.candidates().size(); index++) {
                getInventory().setItem(EnchantMenuLayout.rawSlotFor(slot, index),
                        candidateIcon(enchantSlot, index, heldClass));
            }
        }
    }

    /**
     * One candidate cell.
     *
     * <p>The four states differ ONLY cosmetically -- the icon, the name colour, and the last lore
     * line. The enchant's identity and its effect are on every one of them.
     */
    private ItemStack candidateIcon(EnchantSlot enchantSlot, int index, GearClass heldClass) {
        EnchantCandidate candidate = enchantSlot.candidates().get(index);
        boolean active = enchantSlot.activeIndex() == index;
        EnchantDefinition definition =
                adapters.enchants().find(candidate.enchantId()).orElse(null);

        String name = definition != null ? definition.displayName() : candidate.enchantId();
        int maxLevel = definition != null ? definition.maxLevel() : EnchantState.MAX_LEVEL;
        boolean locked = candidate.isLocked();

        // THE PRICE ON THIS CELL COMES FROM THE SAME TWO CALLS THE CLICK MAKES. Not a re-derivation
        // from `locked` and `active`: if the printed number and the charged number came from two
        // expressions, they would drift and the boot gate would be checking one against itself --
        // the reason Unbreaking.consumeChance is shared with its tooltip rather than duplicated.
        EnchantClickIntent intent = EnchantClickIntent.of(enchantSlot, index, definition);
        int target = EnchantCharge.targetLevel(intent, candidate.level());
        String price = target == EnchantCharge.FREE
                ? ""
                : " -- " + EnchantCost.xpPoints(target, bookshelfPower) + " XP";

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
                    + EnchantLoreLines.romanNumeral(1) + price + ".", NamedTextColor.DARK_GRAY));
        } else if (active) {
            lore.add(MenuIcons.line("Active on this weapon.", NamedTextColor.GREEN));
            // Driven off the INTENT rather than off the cap arithmetic it used to repeat. Same
            // answer for every shipped case, and it also stops advertising a price on a candidate
            // whose content file is missing -- that click is refused at any price.
            if (intent == EnchantClickIntent.LEVEL_UP) {
                lore.add(MenuIcons.line("Click to raise its level" + price + ".", NamedTextColor.GRAY));
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
     * <p>The same steps {@code /rpg enchant} takes, in the same order, for the same reasons: resolve
     * the weapon, read the state, validate in English, transform, reject the no-op, write, RE-MINT.
     * No scheduler hop: {@code InventoryClickEvent} already fires on this player's thread.
     *
     * <p><b>Two things differ from the command, and the second is deliberate.</b> The item lives in
     * an input slot rather than the main hand. And this path CHARGES where the command does not:
     * the economy gates the table, not the dev instrument, because a dev workflow has to be able to
     * build a state without grinding XP and a priced command would put a wallet in the setup line of
     * every future boot gate.
     *
     * <p><b>Where the cost sits, and why there is no rollback.</b> The check goes in front of the
     * transition and the deduction goes behind it, so everything that can refuse -- unaffordable,
     * the model throwing, the no-op, the two arms that only say something -- happens while the
     * wallet is still untouched. The deduction is then the last mutation in the method, which is
     * what makes a compensating write unnecessary rather than merely omitted.
     *
     * <p><b>Never patches lore.</b> Every edit routes through {@code WeaponItems.remint}, which
     * rebuilds the tooltip from the state that just landed and carries the enchant blob across
     * raw. Patching would let the enchant block double or go stale.
     */
    private void applyCandidateClick(int slotIndex, int candidateIndex) {
        ItemStack input = getInventory().getItem(INPUT_SLOT);
        if (input == null || input.getType().isAir()) {
            say("Put one of your weapons or shields in the slot above.", NamedTextColor.GRAY);
            return;
        }

        // DEFENCE IN DEPTH, and NOT a duplicate of acceptsInput's identical-looking check. THIS is
        // the operation that mints: editMeta would enchant every item in a stack, and remint
        // returns a FRESH stack of amount 1, silently collapsing a stack of two into one. That is
        // item destruction rather than a cosmetic glitch, so the operation that can destroy guards
        // itself instead of trusting the insert seam to have held.
        if (input.getAmount() != 1) {
            say("Only one item at a time -- take the stack out and re-insert a single one.",
                    NamedTextColor.RED);
            return;
        }

        // A re-mint needs the definition. REFUSE rather than half-edit: writing state we cannot
        // re-mint leaves an item whose PDC and whose lore disagree, which is worse than nothing.
        // resolveGear says WHY it refused before returning null, exactly as RpgCommand's
        // resolveHeldGear does -- one resolver, one set of refusals, for weapons and shields alike.
        PlacedGear gear = resolveGear(input);
        if (gear == null) return;

        EnchantState before = EnchantItems.read(input, adapters.keys());
        if (slotIndex >= before.slots().size()) return;                   // filler, not a refusal
        EnchantSlot slot = before.slots().get(slotIndex);
        if (candidateIndex >= slot.candidates().size()) return;           // filler, not a refusal

        EnchantCandidate candidate = slot.candidates().get(candidateIndex);
        EnchantDefinition enchant = adapters.enchants().find(candidate.enchantId()).orElse(null);
        String name = enchant != null ? enchant.displayName() : candidate.enchantId();
        EnchantClickIntent intent = EnchantClickIntent.of(slot, candidateIndex, enchant);

        // THE COST CHECK, IN FRONT OF THE TRANSITION. Nothing has been written and nothing charged,
        // so an unaffordable click is a click that did not happen -- the same shape as acceptsInput
        // refusing a place. The wallet is the player's WHOLE bank including the part-full bar, not a
        // level count: levels are not a linear currency and 40 of them is not twice 20.
        int target = EnchantCharge.targetLevel(intent, candidate.level());
        int cost = target == EnchantCharge.FREE ? 0 : EnchantCost.xpPoints(target, bookshelfPower);
        int wallet = XpCurve.totalPoints(viewer.getLevel(), viewer.getExp());
        if (wallet < cost) {
            say(name + " costs " + cost + " XP; you have " + wallet + ".", NamedTextColor.RED);
            return;
        }

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
        getInventory().setItem(INPUT_SLOT, gear.remint(input, adapters));

        // THE DEDUCTION, AND IT IS THE LAST MUTATION IN THIS METHOD. Every path that can refuse sits
        // ABOVE it -- the affordability check, the model's own IllegalArgumentException, the
        // equals(before) no-op, and the two arms that say something and yield `before` -- so a
        // refused click, a no-op click and an unaffordable click are indistinguishable from the
        // wallet's point of view. THERE IS NO ROLLBACK because the ordering removes the need for
        // one: if the write or the re-mint above threw, no XP has moved. A compensating write in a
        // catch would be a second write on an error path no test can reach, and it double-refunds if
        // the throw lands after the wallet was already restored. The residual case -- an exception
        // between the re-mint and here, which no shipped path produces -- grants an enchant free,
        // which fails TOWARDS the player and is visible on the item. Charging first would fail
        // towards a player charged for nothing, which is not visible anywhere.
        //
        // This is NOT setLevel(getLevel() - costInLevels). The deduction is `wallet - cost` in
        // POINTS; setLevel and setExp are only how that computed total is written back, through
        // XpCurve's exact inverse of the read. giveExp(-n) would walk the levels down through float
        // accumulation inside NMS and bump the scoreboard XP score as a side effect.
        if (cost > 0) {
            // max(0, ..) cannot fire -- the check above passed and nothing between here and it
            // yields this thread -- and is kept rather than argued away.
            int remaining = Math.max(0, wallet - cost);
            viewer.setLevel(XpCurve.levelFor(remaining));
            viewer.setExp(XpCurve.progressFor(remaining));
        }

        render();
        viewer.updateInventory();

        say(feedbackFor(intent, name, after, candidate.enchantId(), gear.gearClass(), cost),
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
                               String enchantId, GearClass heldClass, int cost) {
        EnchantDefinition enchant = adapters.enchants().find(enchantId).orElse(null);
        int level = after.activeLevel(enchantId);
        String effect = EnchantEffectLine.of(enchant, Math.max(1, level), heldClass);

        return switch (intent) {
            // The spend is named on the two arms that spend, and NOT on ACTIVATE -- whose silence
            // about cost is the message. Saying "for 0 XP" there would make a free action read as a
            // transaction.
            case UNLOCK -> "Unlocked " + name + " I and made it active for " + cost + " XP." + effect;
            case ACTIVATE -> name + " is now active." + effect;
            case LEVEL_UP -> name + " is now " + EnchantLoreLines.romanNumeral(level)
                    + " for " + cost + " XP." + effect;
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
