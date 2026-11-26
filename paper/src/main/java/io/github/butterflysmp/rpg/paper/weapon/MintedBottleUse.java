package io.github.butterflysmp.rpg.paper.weapon;

import java.util.Set;

/**
 * K7 (the seat's brief, 2026-11-25): what vanilla does with a glass bottle is BLOCKED for a MINTED one (the Broken Bottle),
 * and a plain glass bottle is untouched. Every one of these turns the bottle into another item, which would destroy a
 * player's weapon.
 *
 * <p>READ on the pinned server jar ({@code run/versions/26.1.2/paper-26.1.2.jar}, build 74), every class that names
 * {@code Items.GLASS_BOTTLE}:
 * <ul>
 *   <li>{@code BottleItem.use}: a WATER SOURCE (a water bottle) and a DRAGON'S BREATH cloud (dragon's breath). The item's
 *       own use: {@code handleUseItem} reads {@code PlayerInteractEvent.useItemInHand()} before {@code useItem}.</li>
 *   <li>{@code CauldronInteractions}: a WATER cauldron (the WATER dispatcher's entry). {@code BeehiveBlock.useItemOn}: a
 *       full beehive or bee nest (a honey bottle; it fires no Bukkit event of its own). Both are the BLOCK's interaction:
 *       {@code ServerPlayerGameMode.useItemOn} reads {@code useInteractedBlock()} before {@code BlockState.useItemOn}.</li>
 *   <li>{@code DispenseItemBehavior$10}: a dispenser holding one fills it from water or a beehive in front of it; it
 *       fires {@code BlockDispenseEvent} first.</li>
 *   <li>{@code BrewingStandMenu$PotionSlot.mayPlaceItem}: a glass bottle may sit in a potion slot. Brewing it is inert
 *       (INFERRED: a mix needs potion contents), and a brew with a minted item in the stand is cancelled anyway.</li>
 *   <li>Crafting ({@code honey_bottle.json}) is the screen every minted item already has ({@code CraftMatrixScreen},
 *       {@code onPrepareCraft} and {@code onCrafterCraft}). No smelting recipe takes it and no fuel value names it.</li>
 * </ul>
 * No entity takes a glass bottle on right click (no entity class names it; the dragon's breath cloud is the item's own
 * use, above).
 */
public final class MintedBottleUse {

    private MintedBottleUse() {}

    /** The vanilla material a minted weapon must be carried on for this to apply. A vanilla fact, not a weapon id. */
    static final String BOTTLE = "glass_bottle";

    /**
     * The blocks whose OWN interaction takes a glass bottle (READ, above). Only these are denied, so a chest or a door
     * still opens with the bottle in hand. CC'S CALL: the alternative, denying every block, closes those too.
     */
    static final Set<String> BOTTLE_BLOCKS = Set.of("water_cauldron", "beehive", "bee_nest");

    /** Is this OUR minted weapon carried on a glass bottle? {@code materialKey} is the item's material key ("glass_bottle"). */
    public static boolean isMintedBottle(boolean minted, String materialKey) {
        return minted && BOTTLE.equals(materialKey);
    }

    /** Is a right click with a minted bottle on this block (its material key) one the block itself would answer? */
    public static boolean denyBlock(String blockKey) {
        return BOTTLE_BLOCKS.contains(blockKey);
    }
}
