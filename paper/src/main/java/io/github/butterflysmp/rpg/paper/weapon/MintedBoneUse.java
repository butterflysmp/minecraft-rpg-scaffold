package io.github.butterflysmp.rpg.paper.weapon;

/**
 * M2 (the seat's brief of 2026-11-26, the Bone Club): A MINTED BONE DOES NOTHING A BONE DOES; a plain bone is untouched.
 * ONE guard for every minted bone, the Bone Club's and the Bone Whistle's, replacing the whistle's summoner-only guard
 * (PLAN-summoner.md PR 5). The decision, pure; {@code RpgListeners.onMintedItemEntityInteract} asks it.
 *
 * <p>Every use READ in the pinned server jar ({@code run/versions/26.1.2/paper-26.1.2.jar}, = {@code paper.version}
 * {@code 26.1.2.build.74-stable}), by searching every class's constant pool for the field {@code Items.BONE} and every
 * data file for {@code "minecraft:bone"}:
 *
 * <table>
 *   <tr><th>what it does</th><th>where (READ)</th><th>blocked by</th></tr>
 *   <tr><td>tames an untamed wolf, the bone consumed ({@code consume(1)}, then {@code tryToTame})</td>
 *       <td>{@code Wolf.mobInteract}, reached through {@code ServerPlayer.interactOn} in
 *       {@code ServerGamePacketListenerImpl.handleInteract}, which first fires {@code PlayerInteractAtEntityEvent} and
 *       RETURNS when it is cancelled</td>
 *       <td>{@link #refuseEntityUse}: the event is cancelled when the hand that clicked holds a minted bone.
 *       {@code PlayerInteractAtEntityEvent} declares no handler list of its own (READ, paper-api), so a listener on
 *       {@code PlayerInteractEntityEvent} receives it</td></tr>
 *   <tr><td>a crafting ingredient ({@code bone_meal.json}, shapeless, 3 bone meal)</td>
 *       <td>{@code data/minecraft/recipe}</td>
 *       <td>already: {@code onPrepareCraft}, {@code onCrafterCraft} ({@code CraftMatrixScreen}, any minted gear)</td></tr>
 *   <tr><td>a wolf BEGS (head tilt) at a player holding one</td><td>{@code BegGoal}</td>
 *       <td>NOT blocked: cosmetic, as the zombie horse's tempt at a minted mushroom (CC'S CALL)</td></tr>
 * </table>
 *
 * <p>Nothing else (READ): the constant-pool search finds {@code Items.BONE} only in {@code Wolf}, {@code BegGoal},
 * {@code Items}, {@code CreativeModeTabs} and the data generators (loot tables that DROP bones, the recipe, an
 * advancement). No block class, no dispenser behaviour, no composter entry, no fuel, no brewing, no trade, no tag names it.
 * The bone has no use of its own (registered with {@code registerItem(String)}: a plain {@code Item}), so a right click in
 * the air or on a block does nothing.
 */
public final class MintedBoneUse {

    private MintedBoneUse() {}

    /** The vanilla material a minted weapon must be carried on for this to apply. A vanilla fact, not a weapon id. */
    static final String BONE = "bone";

    /** Is this OUR minted weapon carried on a bone? {@code materialKey} is the item's material key ("bone"). */
    public static boolean isMintedBone(boolean minted, String materialKey) {
        return minted && BONE.equals(materialKey);
    }

    /**
     * THE ONE ENTITY GUARD: is a right click on an entity, with this item in the hand that clicked, refused? Yes for a
     * minted bone (any weapon: the Bone Club, the Bone Whistle), and yes, as before, for any minted SUMMONER weapon (the
     * whistle's guard of PR 5, kept so a future summoner weapon on another material keeps it). A plain bone: no.
     */
    public static boolean refuseEntityUse(boolean minted, String materialKey, boolean summonerWeapon) {
        return isMintedBone(minted, materialKey) || (minted && summonerWeapon);
    }
}
