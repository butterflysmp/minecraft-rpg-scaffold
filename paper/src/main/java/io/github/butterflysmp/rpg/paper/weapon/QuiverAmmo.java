package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.weapon.CollectPlan;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.List;

/**
 * The arrows a player can put into a quiver, and the debit that takes them.
 *
 * <p>Slice E, ruling 1: <b>arrows load all quivers.</b> This is the only place the inventory is read
 * for ammo and the only place it is written for ammo, so counting and consuming cannot disagree.
 *
 * <h2>IT FOLLOWS THE CRAFTING PATTERN RATHER THAN INVENTING ONE</h2>
 *
 * <p>The three-part split is already in the tree and already solves this problem:
 *
 * <ul>
 *   <li><b>WALK</b> — {@code RecipeProbe.groups} walks {@code getStorageContents()} and emits
 *       {@link CollectPlan.Source}{@code (tier, slot, amount)}. {@link #sources} is that, narrowed to
 *       one material.
 *   <li><b>DECIDE</b> — {@link CollectPlan#plan} is PURE and lives in {@code core}. Not reimplemented
 *       here: it already owns the drain order, the stack arithmetic and the slot tiebreak that makes
 *       a plan stable.
 *   <li><b>DEBIT</b> — {@code InventoryCraft.debit} debits <b>BY RECORDED SLOT, never by re-finding
 *       similar stacks</b>. {@link #consume} is that invariant, applied to arrows.
 * </ul>
 *
 * <p><b>The debit invariant is the arrow-removal hazard exactly.</b> Re-finding stacks at debit time
 * can take the wrong ones if anything moved between the count and the take — and between those two
 * moments sits a verdict, a stat read and a PDC write. Recorded slots cannot drift.
 *
 * <h2>PLAIN ARROWS ONLY, AND THIS IS A MECHANISM RATHER THAN A TASTE</h2>
 *
 * <p>Ruling 2. {@link Material#ARROW} exactly — never a family match, never
 * {@code getType().name().endsWith("ARROW")}.
 *
 * <p><b>{@code QuiverSizeModifierItems} mints the {@code quiver_size_boost} dev instrument as
 * {@link Material#SPECTRAL_ARROW}.</b> A family match would CONSUME THE ITEM THAT ENLARGES THE
 * MAGAZINE, in the hands of the exact person testing this slice — a reload eating the gear that made
 * the quiver bigger. {@code TIPPED_ARROW} and {@code SPECTRAL_ARROW} are separate Bukkit materials,
 * so exact matching already excludes both; this paragraph exists so nobody "improves" it later.
 *
 * <p>Gear is skipped as well, by the same rule the crafting walk applies — <i>a minted item is never
 * a material</i>. Belt and braces today, since no minted item is a plain arrow, and it is the rule
 * that stays true on the day one is.
 *
 * <h2>THE ONLY {@code GameMode} READ IN THE PROJECT</h2>
 *
 * <p>Measured before it was written: {@code grep -rn "GameMode"} over both modules, main and test,
 * returned <b>zero</b>. So this sets the precedent every later consumable will cite, and it is
 * written to be cited.
 *
 * <p><b>ONE read, here, in {@link #supply}.</b> Not one at the count and another at the debit — two
 * would be two places to forget, and they would fail in opposite directions: a creative player
 * charged for arrows, or a survival player given them free.
 */
public final class QuiverAmmo {

    private QuiverAmmo() {}

    /** Ruling 2: vanilla {@code ARROW}, exactly. Not spectral, not tipped. */
    public static final Material AMMO = Material.ARROW;

    /**
     * What a reload can draw, and the draws that would take it.
     *
     * @param rounds how many rounds the reload will deliver. Never more than was asked for.
     * @param draws  the slots to debit, by recorded slot. <b>EMPTY in creative</b>, which is what
     *               makes {@link #consume} a no-op there without a second mode check.
     */
    public record Supply(int rounds, List<CollectPlan.Draw> draws) {}

    /**
     * What the player can supply toward {@code needed} rounds, right now.
     *
     * <h2>CREATIVE SKIPS THE CHECK ENTIRELY — it does not "pass it trivially"</h2>
     *
     * <p>Operator ruling: <i>creative mode shouldn't consume arrows.</i> Taken literally that only
     * removes the debit — which would leave a creative player with an empty inventory <b>REFUSED by
     * the no-ammo verdict for lacking arrows they were never going to spend.</b> So creative supplies
     * exactly what the reload needs, and supplies no draws.
     *
     * <p><b>Returning {@code needed} rather than a sentinel is what keeps the verdict ladder
     * uniform:</b> {@code QuiverState.reloadVerdict} never learns that creative exists, the ammo rung
     * is simply satisfied, and {@code BEGIN} follows for the ordinary reason.
     *
     * <h2>ALL FOUR MODES, NAMED — a game-mode branch is a branch over a SET, not a boolean</h2>
     *
     * <p>A {@code switch} with no default, so a fifth mode in a future Paper release is a COMPILE
     * ERROR rather than silently inheriting whichever arm someone reached for. Same discipline as
     * {@code VanillaDamagePolicy.forCause} over its thirty-three constants: <i>the length IS the
     * guard.</i>
     *
     * <ul>
     *   <li>{@code SURVIVAL} — consumes. The ruled default.
     *   <li>{@code CREATIVE} — does not. <b>Ruled.</b>
     *   <li>{@code ADVENTURE} — consumes. A play mode with a restricted inventory, not a build mode;
     *       a player in adventure is playing, and ruling 4 is a statement about play.
     *       <b>DERIVED, not ruled.</b>
     *   <li>{@code SPECTATOR} — consumes, and is believed unreachable: a spectator cannot use an item,
     *       so no reload begins. <b>DERIVED, not ruled.</b> See below.
     * </ul>
     *
     * <p><b>AN ARM DOCUMENTED AS UNREACHABLE MUST STILL BE SAFE IF REACHED.</b> SPECTATOR's
     * unreachability is a derivation about what a spectator can do with an item, not a measurement,
     * and this project's rule is that <i>a guard with no instances is not a guard that cannot
     * fire.</i> So it states a behaviour rather than relying on never firing, and the behaviour is the
     * conservative one. A throw would convert a wrong assumption into a crash; consuming costs a
     * spectator nothing <b>precisely because they cannot reach it</b> — so the safe answer is free.
     */
    public static Supply supply(Player player, int needed) {
        if (needed <= 0) return new Supply(0, List.of());

        boolean consumes = switch (player.getGameMode()) {
            case SURVIVAL, ADVENTURE, SPECTATOR -> true;
            case CREATIVE -> false;
        };
        if (!consumes) return new Supply(needed, List.of());

        List<CollectPlan.Draw> draws = CollectPlan.plan(sources(player.getInventory()), 0, needed);
        return new Supply(CollectPlan.total(draws), draws);
    }

    /**
     * Take the planned draws, <b>by recorded slot</b>.
     *
     * <p>A no-op on an empty plan, which is what creative produces — so creative needs no second
     * check here.
     *
     * <p>Re-reads each slot and skips one that no longer holds what was planned, rather than trusting
     * the plan blindly: the walk and the take are separated by a verdict and a stat read, and a slot
     * that changed underneath is a reason to take nothing rather than to take the wrong thing.
     */
    public static void consume(Player player, List<CollectPlan.Draw> draws) {
        PlayerInventory inventory = player.getInventory();
        for (CollectPlan.Draw draw : draws) {
            int slot = draw.source().slot();
            ItemStack stack = inventory.getItem(slot);
            if (stack == null || stack.getType() != AMMO) continue;
            int left = stack.getAmount() - draw.amount();
            if (left <= 0) {
                inventory.setItem(slot, null);
            } else {
                ItemStack reduced = stack.clone();
                reduced.setAmount(left);
                inventory.setItem(slot, reduced);
            }
        }
    }

    /**
     * Every plain-arrow stack the player is carrying, as {@link CollectPlan.Source}s keyed by slot.
     *
     * <p>{@code getStorageContents()} rather than {@code getContents()}, so armour and the off-hand
     * are out of scope: those slots are addressed by different indices, and a draw planned against
     * one would debit the wrong slot. The same choice {@code RecipeProbe} and {@code MenuSafety} make.
     */
    private static List<CollectPlan.Source> sources(PlayerInventory inventory) {
        List<CollectPlan.Source> sources = new ArrayList<>();
        ItemStack[] contents = inventory.getStorageContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if (item == null || item.getType() != AMMO || item.getAmount() <= 0) continue;
            sources.add(new CollectPlan.Source(CollectPlan.TIER_INVENTORY, slot, item.getAmount()));
        }
        return sources;
    }
}
