package io.github.butterflysmp.rpg.paper.weapon;

import java.util.Set;

/**
 * F11 (the seat's brief of 2026-11-26, the Blast Fungus): A MINTED RED MUSHROOM DOES NOTHING A RED MUSHROOM DOES; a plain
 * one is untouched. The decisions, pure; {@code RpgListeners} asks them. Every use READ in the pinned server jar
 * ({@code run/versions/26.1.2/paper-26.1.2.jar}, MANIFEST {@code 26.1.2.build.74-stable}, = {@code paper.version}):
 *
 * <table>
 *   <tr><th>what it does</th><th>where (READ)</th><th>blocked by</th></tr>
 *   <tr><td>places a red_mushroom block</td><td>{@code Items} registers it with {@code registerBlock(Block)}, a
 *       {@code BlockItem}; {@code ItemStack.useOn} calls {@code callBlockPlaceEvent}</td>
 *       <td>main hand: {@code onRightClick} cancels the whole interaction for a weapon that binds right_click (READ:
 *       {@code ServerPlayerGameMode.useItemOn} returns before the block's use and the item's); either hand:
 *       {@code onWeaponPlace} cancels {@code BlockPlaceEvent} for any minted weapon</td></tr>
 *   <tr><td>pots itself in an empty flower pot</td><td>{@code FlowerPotBlock.useItemOn}
 *       ({@code POTTED_BY_CONTENT}, then {@code consume(1)})</td>
 *       <td>{@code setUseInteractedBlock(DENY)} on {@code flower_pot}: {@link #denyBlock}</td></tr>
 *   <tr><td>composts (a player's click)</td><td>{@code ComposterBlock.useItemOn} → {@code addItem}, chance 0.65
 *       ({@code bootStrap})</td><td>{@code setUseInteractedBlock(DENY)} on {@code composter}</td></tr>
 *   <tr><td>composts (a hopper)</td><td>{@code ComposterBlock$InputContainer.setChanged} → {@code addItem}, firing
 *       {@code CompostItemEvent}, which is NOT cancellable (READ, paper-api: no {@code Cancellable})</td>
 *       <td>{@code InventoryMoveItemEvent} into a composter, cancelled ({@link #refuseMoveInto}); that it fires on this
 *       path is INFERRED (the hopper's own move event, not read here)</td></tr>
 *   <tr><td>a zombie horse eats it (heal 3, temper 3), consumed</td><td>tag {@code zombie_horse_food} =
 *       {@code [red_mushroom]}; {@code ZombieHorse.isFood}; {@code AbstractHorse.handleEating}</td>
 *       <td>{@code PlayerInteractEntityEvent} cancelled when the hand that interacts holds one</td></tr>
 *   <tr><td>a crafting ingredient (mushroom_stew, rabbit_stew, 15 suspicious_stew recipes)</td>
 *       <td>{@code data/minecraft/recipe}</td><td>already: {@code onPrepareCraft}, {@code onCrafterCraft}
 *       ({@code CraftMatrixScreen}, any minted gear)</td></tr>
 * </table>
 *
 * <p>Nothing else (READ): no food or consumable component; {@code PotionBrewing} and {@code FuelValues} name none; no
 * smelting, smoking, campfire, blasting, stonecutting or smithing recipe; a dispenser has no behaviour for it and drops it
 * whole ({@code DEFAULT_BEHAVIOR}); the only trade GIVES red mushrooms; a mooshroom takes none ({@code MushroomCow
 * .mobInteract} reads bowl, stews and shears). A zombie horse is tempted by one held (its {@code TemptGoal}): cosmetic,
 * not blocked (CC'S CALL).
 */
public final class MintedMushroomUse {

    private MintedMushroomUse() {}

    static final String MUSHROOM = "red_mushroom";
    static final Set<String> MUSHROOM_BLOCKS = Set.of("flower_pot", "composter");

    /** A minted weapon whose material is a red mushroom: the Blast Fungus today, any such weapon tomorrow. */
    public static boolean isMintedMushroom(boolean minted, String materialKey) {
        return minted && MUSHROOM.equals(materialKey);
    }

    /** The clicked blocks whose own use takes a red mushroom: the click is denied there, so it is neither potted nor composted. */
    public static boolean denyBlock(String blockKey) {
        return MUSHROOM_BLOCKS.contains(blockKey);
    }

    /** A minted red mushroom moving into a composter (a hopper's insert) is refused. */
    public static boolean refuseMoveInto(boolean mintedMushroom, String destinationBlockKey) {
        return mintedMushroom && "composter".equals(destinationBlockKey);
    }
}
