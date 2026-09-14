package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.combat.DrawCharge;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import io.github.butterflysmp.rpg.core.weapon.WeaponRegistry;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * THE DRAGON'S PLUME'S DRAW: the vanilla bow is an INPUT DEVICE, and this reads it.
 *
 * <p>Slice H1, the input layer. <b>It fires NOTHING</b> -- see the section at the bottom, which is a
 * decision rather than an omission. Every DECISION in here lives in {@link DrawCharge}, in
 * {@code core}, where a test reaches it without a server; this class only reads a live player,
 * schedules, and makes a noise.
 *
 * <h2>THE RELEASE ROUTE, AND THE ONE THAT WAS TRIED FIRST AND CANNOT WORK</h2>
 *
 * <p><b>The first route considered was {@code EntityShootBowEvent}: cancel the shot, keep the
 * arrow.</b> It cannot work, and the reason is an ORDERING that is invisible unless stated. Read off
 * the pinned jar:
 *
 * <pre>
 * BowItem.releaseUsing
 *    71:  draw(...)     &lt;- the arrow is CONSUMED in here, via useAmmo
 *   133:  shoot(...)    &lt;- and EntityShootBowEvent is constructed and fired inside THIS
 * </pre>
 *
 * <p><b>The arrow is gone before the event object exists.</b> Cancelling calls {@code remove()} on
 * the projectile and nothing else: it suppresses the SHOT, never the SPEND. Nor can the flag help --
 * {@code shouldConsumeItem()} is read <b>0</b> times in {@code BowItem}, <b>0</b> in
 * {@code ProjectileWeaponItem} and <b>0</b> in {@code CraftEventFactory}, against <b>2</b> in
 * {@code CrossbowItem}, which is what a working consume-flag path looks like.
 *
 * <p><b>So R5 -- one arrow in the off-hand, forever -- would have died on the first shot.</b> It does
 * not, because of what sits one layer up:
 *
 * <pre>
 * LivingEntity.releaseUsingItem
 *    67:  PlayerStopUsingItemEvent.callEvent()
 *    70:  pop                     &lt;- the return is DISCARDED; the event is not cancellable
 *    72:  getfield useItem        &lt;- RE-READ FROM THE FIELD, after our handler has run
 *    84:  ItemStack.releaseUsing(...)   &lt;- which would be BowItem.releaseUsing, and therefore draw()
 * </pre>
 *
 * <p><b>That re-read is the lever.</b> {@link Player#clearActiveItem()} inside the handler is
 * {@code LivingEntity.stopUsingItem()}, which sets {@code useItem = ItemStack.EMPTY}. The re-read at
 * 72 then yields EMPTY, {@code ItemStack.releaseUsing} calls {@code Items.AIR.releaseUsing}, and
 * {@code Item.releaseUsing} is {@code iconst_0; ireturn} -- <b>a pure no-op, which {@code AirItem}
 * does not override.</b> No {@code draw()}, no {@code useAmmo}, no vanilla arrow, no consumption.
 *
 * <p><b>NOT MEASURED, AND IT IS THIS SLICE'S BOOT THAT ANSWERS IT:</b> that
 * {@code clearActiveItem()} called from inside that handler behaves live as the bytecode reads. The
 * handler runs on the main thread inside the same call, so it <i>should</i> -- and <b>"should" is
 * what the boot is for.</b>
 *
 * <h2>IT WRITES A PLATFORM FIELD THREE FILES ALREADY READ</h2>
 *
 * <p>Held-use state is read today by {@code RpgListeners}, {@code ShieldBlock} and
 * {@code ShieldItems.shieldHand}, which calls {@code getActiveItemHand()} -- <b>the same state
 * {@code clearActiveItem()} empties. This is the first thing in the project to WRITE it.</b>
 *
 * <p><b>They cannot overlap, and the reason is that the field is SINGULAR.</b> A player has one
 * active item. If the Plume is being drawn, no shield is raised in that state, and the clear below
 * runs only when the RELEASED item is one of our bows -- so {@code shieldHand} can never observe a
 * clear this class caused. The same sentence is recorded at {@code ShieldItems.shieldHand}, so
 * whoever next touches either side meets it.
 *
 * <p><b>That claim is about the platform's state machine, and this is the layer that has already
 * been wrong once in this project:</b> {@code ShieldBlock} records {@code isBlocking()} as
 * DIRECTION-BLIND, boot-witnessed at 107 and 160 degrees off the victim's facing -- a predicate that
 * did not mean what its name said, found at a boot rather than in a file. So the negative boot row
 * exists: eat, block, fire an ordinary bow, and confirm H1 broke none of them.
 *
 * <h2>THE GATE IS ON THE ITEM, NOT ON THE EVENT</h2>
 *
 * <p>{@code PlayerStopUsingItemEvent} fires for <b>every</b> item release -- eating, drinking,
 * shields, crossbows, tridents, spyglasses. An ungated {@code clearActiveItem()} there would break
 * all of them, silently, on a server where nobody is testing the Plume. Same shape as
 * {@code RpgListeners}'s main-hand check on the interact event, and for the same reason: a general
 * event firing for a case nobody pictured.
 *
 * <h2>H1 FIRES NOTHING, AND THAT IS THE POINT OF THE SPLIT RATHER THAN AN UNFINISHED EDGE</h2>
 *
 * <p>A bow that ticks and does not shoot. <b>Every high-risk unknown in this weapon is in this
 * layer</b> -- does {@code getActiveItemUsedTime} behave live as the jar reads, does the release
 * event arrive when expected, does the off-hand arrow really satisfy the draw gate in play, does the
 * clear leave the arrow alone.
 *
 * <p><b>All four are answered by a bow that only ticks.</b> Layer firing, homing, damage and
 * round-spending on top first and the boot stops being a measurement and becomes a debugging
 * session, with four candidate causes for every symptom and no way to separate them.
 *
 * <p>H2 spawns the projectiles through the existing cast path with F/F2's homing, spends the rounds,
 * and handles R4' (a partial draw: one arrow, no homing, 12 damage) and R10 (under three ticks,
 * nothing -- vanilla's own floor, taken from the platform rather than reimplemented).
 */
public final class PlumeDraw {

    /**
     * THE CHARGE TICK'S SOUND -- RULED, AFTER A LISTEN, WHICH IS THE ONLY INSTRUMENT THERE IS.
     *
     * <p><b>{@code block.note_block.hat}, ruled 2026-09-14: Ben heard it and it suits the weapon.</b>
     * It is the third key this tick has worn. It shipped as {@code block.note_block.pling} on the
     * reasoning that a clean pitched tone is the right shape for a ratio ladder -- withdrawn on a
     * listen, <i>"the piano doesn't suit it, try snare or kick"</i> -- and then as
     * {@code block.note_block.basedrum}, the kick, which this replaces.
     *
     * <h2>THE RULING IS ON FEEL, AND IT DOES NOT DISCHARGE R13</h2>
     *
     * <p>R13's property is that a player can tell <b>HOW CHARGED THEY ARE</b> -- knowing your
     * POSITION on the ladder, nameable by ear, having possibly started listening late. <b>A feel
     * judgement does not establish that</b>, and no position trial has been run on any candidate:
     * {@code GATE-plume-draw.md}'s <b>H-1b is still OPEN</b>, and what it now owes is the position
     * reading <b>on this key</b>.
     *
     * <p><b>Do not read the ruling as the row.</b> A verdict standing in for a measurement is a
     * failure that gate page has already recorded three times.
     *
     * <h2>THE ARGUMENT THAT CHOSE THE KICK IS WITHDRAWN, NOT RETARGETED AT THE HAT</h2>
     *
     * <p>The kick was argued for: <i>a snare's sharper transient is easier to COUNT, a kick carries
     * the rise more legibly and so tells you WHICH STEP you are on.</i> That argument selected a
     * different key from the one now ruled, so <b>it is withdrawn rather than rewritten to land on
     * the hat.</b> Ben's stated reason is that it suits the weapon; manufacturing a mechanism for a
     * feel judgement would be putting an argument in his mouth, and a rationale nobody gave reads a
     * month later exactly like one somebody did.
     *
     * <p><b>ONE RECORDED PROPERTY OF THIS KEY PREDATES THE RULING, AND IT IS NOT THE REASON FOR
     * IT.</b> The deleted {@code /rpg drawsound} candidate list described the hat as <i>the
     * note-block family's own click, and the only candidate designed to be pitched across the full
     * range</i> -- written while the list was being assembled, before any listen. It is kept because
     * it is a fact about the sound and it bears on the fifth step (see {@link DrawCharge}'s ceiling
     * note). <b>It is not evidence for the ruling, and it did not produce it.</b>
     *
     * <p>The precedent for choosing a note-block sound and proving it by ear is
     * {@code BrokenNotice}'s {@code block.note_block.bass}.
     *
     * <h2>A CONSTANT AGAIN, AND THE INSTRUMENT IS GONE</h2>
     *
     * <p>This was a mutable instance field for exactly as long as the sweep needed one.
     * {@code /rpg drawsound} was born with a written deletion trigger -- <i>the charge tick's sound
     * is ruled</i> -- naming the same commit as the ruling, in three places. <b>It was honoured as
     * written: the field, its two accessors and the subcommand all went with this line.</b>
     *
     * <p><b>The cost is real and is recorded rather than glossed:</b> re-auditioning a key now costs
     * an edit, a rebuild, a redeploy and a boot again, which is exactly the cost the command existed
     * to remove. <b>If the sound reopens, re-adding it is a small slice.</b> An instrument that
     * outlives its trigger is the thing this project keeps finding.
     */
    public static final String TICK_SOUND = "block.note_block.hat";

    private static final float TICK_VOLUME = 0.7f;

    /** The material whose vanilla draw this reads. Content authors it as {@code material: bow}. */
    private static final String DRAW_MATERIAL = "bow";

    private final WeaponRegistry weapons;
    private final AdapterContext adapters;

    /**
     * Which STEP each drawing player has been TOLD they are on.
     *
     * <p>Not the step itself: the step is a pure function of the time held and the live magazine,
     * recomputed every tick, which is what makes R3's cap apply <b>as it climbs</b> rather than once
     * at the end -- and what lets a reload completing mid-draw raise the cap and resume the ticks.
     * This map remembers only what has already been ANNOUNCED, so a sound plays once per step
     * gained rather than once per tick.
     *
     * <p><b>STEPS, NOT ARROWS, SINCE R1's AMENDMENT -- and while a step was one arrow this map held
     * both at once without anyone choosing.</b> Under 1/3/5 they are different numbers and only one
     * of them is what the ladder is indexed by: three sounds, at
     * {@link DrawCharge#pitchFor(int) pitchFor(1..3)}. Holding arrows here and converting at the
     * sound would have played five ticks on a three-rung ladder.
     *
     * <p>An INSTANCE field on a listener-owned object, never a static: player state in a static map
     * is the singleton this project refuses.
     */
    private final Map<UUID, Integer> announced = new ConcurrentHashMap<>();

    public PlumeDraw(WeaponRegistry weapons, AdapterContext adapters) {
        this.weapons = weapons;
        this.adapters = adapters;
    }

    /**
     * A weapon of OURS whose vanilla draw we read: bow material, and it binds no {@code right_click}.
     *
     * <p><b>The second half is what makes the gate exact rather than approximate.</b> A bow-material
     * weapon that BINDS right-click has its vanilla interaction cancelled by {@code RpgListeners}
     * before {@code BowItem.use} ever runs -- {@code hunters_bow} is exactly that -- so its draw
     * never starts and this class has nothing to read. Including such a weapon in the gate would be
     * harmless but untrue; excluding it says what the gate MEANS.
     *
     * <p><b>NO SHIPPED CONTENT REACHES THIS TODAY.</b> The only bow in the project is
     * {@code hunters_bow}, which binds {@code right_click}. The first weapon that matches is the
     * Dragon's Plume, whose file is the content chat's seat -- so H1's boot rows are written and
     * NOT RUN, exactly as {@code GATE-locust.md} row 1 was.
     */
    private Optional<WeaponDefinition> drawWeapon(ItemStack item) {
        return WeaponItems.weaponId(item, adapters.keys())
                .flatMap(weapons::find)
                .filter(weapon -> DRAW_MATERIAL.equalsIgnoreCase(weapon.material()))
                .filter(weapon -> weapon.trigger("right_click").isEmpty());
    }

    /**
     * The player right-clicked with a draw weapon. Start watching, one tick later.
     *
     * <p><b>One tick later, and that is not a rounding.</b> {@code PlayerInteractEvent} fires BEFORE
     * {@code ServerPlayerGameMode.useItem} -- measured, offsets 252/385 against 512 -- so at this
     * moment {@code hasActiveItem()} is still false and the draw has not begun. The first tick that
     * can see it is the next one.
     */
    public void onDrawStarted(Player player) {
        if (drawWeapon(player.getInventory().getItemInMainHand()).isEmpty()) return;
        announced.remove(player.getUniqueId());
        adapters.scheduler().onEntityLater(player, () -> tick(player), 1);
    }

    /**
     * One tick of a draw: recompute, announce what is new, and reschedule.
     *
     * <p>Stops -- and forgets the player -- the moment the draw is over, which is any of: gone
     * offline, no longer using an item, or no longer holding a draw weapon. There is no separate
     * "cancel" path, because every way a draw can end is one of those three.
     */
    private void tick(Player player) {
        UUID id = player.getUniqueId();
        if (!player.isOnline() || !player.hasActiveItem()) {
            announced.remove(id);
            return;
        }
        ItemStack held = player.getInventory().getItemInMainHand();
        Optional<WeaponDefinition> weapon = drawWeapon(held);
        if (weapon.isEmpty()) {
            announced.remove(id);
            return;
        }

        int ready = DrawCharge.affordableStep(DrawCharge.stepsFor(player.getActiveItemUsedTime()),
                cap(held, weapon.get(), player));

        int alreadyTold = announced.getOrDefault(id, 0);
        // ONE SOUND PER STEP GAINED, not per tick, and the loop covers a tick that crosses two
        // thresholds at once -- which a lagging server can produce.
        for (int n = alreadyTold + 1; n <= ready; n++) {
            player.playSound(player.getLocation(), TICK_SOUND, TICK_VOLUME, DrawCharge.pitchFor(n));
        }
        announced.put(id, ready);

        adapters.scheduler().onEntityLater(player, () -> tick(player), 1);
    }

    /**
     * R3'S CAP, READ LIVE FROM THE HELD ITEM. Slice G built {@code roundsRemaining()}; this is its
     * first caller.
     *
     * <p>A weapon with no quiver is capped only by {@link DrawCharge#MAX_ARROWS} -- there is no
     * magazine to run out, so nothing is being promised that cannot be paid.
     */
    private int cap(ItemStack held, WeaponDefinition weapon, Player player) {
        if (!weapon.hasQuiver()) return DrawCharge.MAX_ARROWS;
        return Quivers.stateOf(held, adapters.keys(), weapon).roundsRemaining();
    }

    /**
     * THE RELEASE. Take the draw, suppress vanilla's, and -- in H1 -- fire nothing.
     *
     * @param released the item the event says was let go; the gate is on THIS, not on the event
     */
    public void onRelease(Player player, ItemStack released, int ticksHeldFor) {
        if (drawWeapon(released).isEmpty()) return;

        Integer tracked = announced.remove(player.getUniqueId());
        int tracker = tracked == null ? 0 : tracked;

        // THE TWO MEASURES, CHECKED AGAINST EACH OTHER ONCE. The tracker is authoritative -- it is
        // the one carrying R3's cap -- and this only ever REPORTS. See DrawCharge.disagreement for
        // why the comparison is one-directional, and why it is in STEPS: the tracker's own unit is
        // the step, so comparing arrows would check the conversion as much as the quantity.
        DrawCharge.disagreement(tracker, ticksHeldFor)
                .ifPresent(complaint -> adapters.log().warning("[plume] " + complaint));

        // THE MEASURED ROUTE. Everything above this line is bookkeeping; this is the mechanism.
        player.clearActiveItem();
    }

    /**
     * THE BELT-AND-BRACES GUARD, AND ITS FIRING IS THE SIGNAL.
     *
     * <p>With the clear working there is no vanilla arrow to cancel and this never runs. <b>It is
     * kept anyway, and it is LOUD, because if it ever fires the clear did not take and the player
     * has just lost their off-hand arrow</b> -- which is R5's whole premise, and the one mechanism
     * in this weapon that nothing else can watch.
     *
     * <p><b>A silent cancel would hide exactly the failure it exists for.</b> A guard that logs when
     * it fires cannot be hollow: its firing IS the detector.
     *
     * @return true if this was one of ours and the shot was suppressed
     */
    public boolean suppressManagedBowShot(Player player, ItemStack bow) {
        Optional<WeaponDefinition> weapon = drawWeapon(bow);
        if (weapon.isEmpty()) return false;

        adapters.log().warning(
                "[plume] EntityShootBowEvent fired for managed bow '" + weapon.get().id()
                        + "' held by " + player.getName() + " -- THE RELEASE CLEAR DID NOT TAKE."
                        + " Vanilla reached BowItem.releaseUsing, which means draw() ran and AN"
                        + " ARROW HAS ALREADY BEEN CONSUMED from the player's inventory; the shot is"
                        + " suppressed here but the arrow is gone. R5 (one arrow in the off-hand) is"
                        + " broken on this build -- see PlumeDraw's release-route section.");
        return true;
    }

}
