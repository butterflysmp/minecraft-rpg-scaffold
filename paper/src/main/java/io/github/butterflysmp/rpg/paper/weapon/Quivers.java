package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.ability.AbilityService.CastResult;
import io.github.butterflysmp.rpg.core.combat.CooldownTracker;
import io.github.butterflysmp.rpg.core.combat.ReloadTime;
import io.github.butterflysmp.rpg.core.weapon.Quiver;
import io.github.butterflysmp.rpg.core.weapon.QuiverState;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import io.github.butterflysmp.rpg.core.weapon.WeaponRegistry;
import io.github.butterflysmp.rpg.paper.adapter.AdapterContext;
import io.github.butterflysmp.rpg.paper.adapter.Keys;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

/**
 * The quiver's live state on a held weapon: whether it may fire, spending a round, starting and
 * finishing a reload.
 *
 * <p>{@link QuiverItems} is the storage half -- keys in and out of item meta. This is the half that
 * knows what the stored values MEAN together, and it is where the lazy reload lives.
 *
 * <p>Every method here must run on the thread that owns the player: they read and write the main
 * hand. Both call sites ({@code WeaponFire.attempt}, {@code WeaponSwingListener.onSwing} past its
 * Netty hop) already are.
 *
 * <h2>THE RELOAD IS EVALUATED ON READ. ONE TASK IS SCHEDULED, AND IT NOW SETTLES</h2>
 *
 * <p><b>THIS HEADING SAID "AND IT DECIDES NOTHING" UNTIL 2026-09-24, AND IT WAS TRUE FOR ONE
 * COMMIT.</b> {@link QuiverReloadCue} landed as a sound and nothing else; it now calls
 * {@link #settleMaturedReload} when its deadline check passes, so <b>the task WRITES.</b> The heading
 * is corrected rather than left, because "decides nothing" is exactly the sentence a reader would
 * rely on before adding a second task.
 *
 * <p>An earlier revision of this section said <i>"No task is queued when a reload starts"</i>, which
 * the cue falsified first. The cue books one entity task per reload because <b>nothing runs at
 * maturity</b> and a cue's whole value is its timing. The argument under both withdrawn sentences
 * survives intact, and it is the reason the task is shaped the way it is.
 *
 * <p>{@link #resolveForShot} still asks the item whether its deadline has passed and completes it in
 * place if so. <b>That is still what makes the reload leak-proof</b>: a scheduled task needs an expiry
 * event, and an expiry event can be missed -- the player swaps the weapon away, drops it, dies, logs
 * out. {@code ModifierReconciler}'s javadoc makes the identical argument for diffing over listening:
 * <i>"a single missed event LEAKS."</i> There is nothing in the STATE to miss, because it is simply
 * read again from whatever item is in hand.
 *
 * <p><b>THE ITEM STAMP IS STILL THE AUTHORITY, AND THE TASK IS STILL SUBORDINATE TO IT -- AND THAT
 * PROPERTY IS WHAT MAKES IT SAFE FOR THE TASK TO WRITE.</b> The cue holds no state but the deadline it
 * was created for, and at fire time it asks the item whether that deadline is still the one in hand.
 * So a missed or stale task costs <b>the TIMING of a refill and a sound</b>, and can never cost the
 * refill itself: every other path -- the shot, the refusal, the remaining time, the sweep, the draw --
 * reads the item, and a reload the task never settled is settled by the next one of them that looks.
 *
 * <p>It also settles, for free, the case a per-player timer gets wrong: cooldowns are keyed per
 * player per {@code weaponId/input} and could not tell two identical quivers apart, so a swap
 * mid-reload would fill the wrong weapon. On the item, it cannot.
 *
 * <h2>*** AND HERE IS THE RULE THE LAZY DESIGN IMPOSES ON ITS CALLERS, WHICH WENT UNWRITTEN FOR
 * SEVEN WEEKS AND COST A BUG REPORT ***</h2>
 *
 * <p><b>EVERY SITE THAT DECIDES FROM THE MAGAZINE MUST SETTLE A MATURED RELOAD FIRST.</b> A pure
 * {@link #stateOf} read reports the LOADED count, and a matured-but-unsettled reload's rounds are not
 * in it -- they are in {@code quiver_reload_pending}, and {@link #settleMaturedReload} is what moves
 * them. So a decision taken off the pure read sees a magazine that is stale by up to forever.
 *
 * <p><b>The instance, so the rule is not abstract.</b> {@code PlumeDraw.cap} read
 * {@code stateOf(...).roundsRemaining()} and handed it to {@code DrawRelease.decide}. On master since
 * {@code e9e657a} (#78): reload a Dragon's Plume, let it mature without firing, then draw and release
 * -- {@code cap} is 0, the release is {@code Nothing(NO_ROUNDS)}, the player is told their quiver is
 * empty, and <b>because the shot never reached {@code WeaponFire}, nothing settled the reload either.</b>
 * The weapon stayed dead until a left-click, which reached {@link #beginReload}'s own
 * {@code RELOAD_MATURED} arm and fixed it -- and so presented as <i>"it fires after a left-click,
 * without a second reload"</i>.
 *
 * <p><b>THE DEFECT IS SELF-SUSTAINING, WHICH IS WHY IT IS A RULE AND NOT A ONE-LINE FIX.</b> The
 * lazy design's guarantee is <i>"the next read settles it"</i> -- and that guarantee is void for a
 * reader that decides NOT to act on what it read. A refusal is not a read that settles; it is a read
 * that removes the reason anything would look again.
 *
 * <p>Who settles and who does not is pinned by name in
 * {@code QuiversSignatureTest.everyExternalReaderOfTheMagazineDeclaresWhetherItSettles}.
 */
public final class Quivers {

    private Quivers() {}

    /**
     * Take a shot's turn at the quiver: settle any matured reload, repair a missing count, and
     * report why the weapon may not fire -- or empty if it may.
     *
     * <h2>THIS WRITES. IT IS NOT A QUESTION, AND ITS FIRST NAME SAID IT WAS.</h2>
     *
     * <p>It was called {@code refusalFor}, which reads as a query, and <b>two of its five arms answer
     * it by mutating the player's item</b>: a matured reload refills the magazine, and an unstamped
     * item is stamped full and written back. That is inherent to lazy integration and the lazy design
     * is right -- <i>the read IS the commit point</i>, which is exactly why there is no expiry event
     * to miss. The hazard was never the behaviour; it was a name that invited a speculative caller.
     *
     * <p><b>The reachable misuse, named because it is one piece of work away.</b> A HUD or a tooltip
     * asking <i>"can this weapon fire right now?"</i> every tick -- and {@code PLAN-action-bar-hud.md}
     * exists, and the quiver lore line is owed -- would reach for the obvious-looking method and
     * silently finish reloads and stamp items as a side effect of RENDERING. Nothing in the signature
     * would have warned them, and the bug would present as reloads completing early, which looks like
     * the timer working rather than like a read doing a write.
     *
     * <p><b>So the two questions are now two methods.</b> A caller that wants to ASK calls
     * {@link #stateOf} and reads {@link QuiverState#fireVerdict} -- pure, no writes, no player
     * needed. A caller that is actually taking a shot calls this. The split is the repo's own
     * {@code CombatantSnapshot} / {@code CombatantHandle} distinction one layer down: a value you may
     * read versus a thing that acts.
     *
     * @param player MUST be the owner of this thread -- this reads and writes their main hand.
     */
    public static Optional<CastResult> resolveForShot(Player player, WeaponDefinition weapon,
                                                      AdapterContext adapters) {
        Keys keys = adapters.keys();
        ItemStack held = player.getInventory().getItemInMainHand();
        long now = Bukkit.getCurrentTick();
        QuiverState state = stateOf(held, keys, weapon);

        // READ, DECIDE, DELIVER -- and the DECIDE is not here. The ordering of these cases is the
        // real content of this method, and every one of them is now a core row with a mutation
        // behind it (QuiverStateTest): a running reload beats empty, a MATURED reload fires rather
        // than dropping the press, an unstamped item is a defect and not an empty magazine.
        return switch (state.fireVerdict(now)) {
            case FIRE -> Optional.empty();
            case EMPTY -> Optional.of(new CastResult.Empty());
            case RELOADING -> Optional.of(new CastResult.Reloading(state.reloadTicksRemaining(now)));
            case RELOAD_MATURED -> {
                // The read IS the tick: the reload finished the moment anything looked. Refill and
                // let the shot through, so the press that matures a reload is not wasted.
                finishReload(player, held, weapon, adapters);
                yield Optional.empty();
            }
            case UNSTAMPED -> {
                // A mint path failed to stamp a count. Refusing to fire would hide that behind a
                // message that reads perfectly reasonable; repairing it loudly leaves the weapon
                // usable and the defect visible. warnOnce because this runs on every shot.
                //
                // *** NO SHIPPED PATH REACHES THIS ARM, AND IT IS KEPT ANYWAY. ***
                //
                // Since the carry landed, EVERY mint and re-mint routes through WeaponItems.mint
                // (which stamps) or GearItems.carryInstanceData (which carries) -- so production
                // cannot currently produce an item that reaches here. This repo's rule is that a
                // guard with no instances is DELETED if mechanism-unreachable and owes FORWARD
                // COVER if it is guarding something that does not exist yet. This owes forward
                // cover, and here it is.
                //
                // WHAT WOULD REACH IT: a fifth mint path added later that builds a weapon ItemStack
                // without going through mint() -- the exact mistake the two-funnel design makes hard
                // and does not make impossible. That is the whole reason absence and emptiness are
                // kept apart: without this arm such a path yields a weapon that silently never
                // fires, and the symptom is indistinguishable from a spent magazine.
                //
                // ITS ONLY EXERCISE IS QuiverStateTest.anUnstampedQuiverIsADefectAndNotAnEmptyMagazine,
                // which covers the VERDICT. The side effect below -- warn, stamp, write back -- is
                // witnessed by NOTHING, because staging it needs an item production cannot make.
                // GATE-quiver.md says so rather than carrying a row nobody can run. Do not read the
                // green suite around this block as coverage of it.
                adapters.warnOnce("weapon '" + weapon.id() + "' declares a quiver but an item in"
                        + " play carries NO count -- it was minted by a path that does not stamp"
                        + " one. Treating it as full; the defect is in that mint path, not the item.");
                held.editMeta(meta -> QuiverItems.setFull(
                        meta, weapon, adapters, player.getUniqueId()));
                player.getInventory().setItemInMainHand(held);
                yield Optional.empty();
            }
        };
    }

    /**
     * The three stored values as the one core type that knows what they mean together.
     *
     * <p><b>PURE. Reads an item, writes nothing, and needs no {@code Player}.</b> This is the method
     * a HUD, a tooltip or anything else that wants to ASK about a quiver should call -- then read
     * {@link QuiverState#fireVerdict}, {@link QuiverState#loaded} or
     * {@link QuiverState#reloadTicksRemaining} off the result. It is public for exactly that reason:
     * the safe way to ask has to be the obvious one, or {@link #resolveForShot} will be reached for
     * instead and will quietly commit reloads as a side effect of rendering.
     *
     * <p>The reload pair is taken together or not at all -- {@link QuiverState} refuses a half-reload
     * outright, so a partially-written item surfaces as a thrown exception here rather than as a
     * weapon that behaves oddly.
     *
     * <h2>*** IT IS THE RIGHT METHOD FOR A QUESTION AND THE WRONG ONE FOR A DECISION ***</h2>
     *
     * <p>"Pure" was sold above as the safe property, and for a RENDER it is. <b>For a DECISION it is
     * the hazard</b>: what this returns is the LOADED count, and a matured-but-unsettled reload's
     * rounds are not in it. A caller that refuses a shot on that number gets a weapon that stays dead,
     * because a refusal is also the thing that stops anything looking again.
     *
     * <p><b>So: deciding callers call {@link #settleMaturedReload} first and then this, on a freshly
     * read main hand.</b> That is the class javadoc's rule, {@code PlumeDraw.cap} is the site that
     * broke it, and {@code QuiversSignatureTest} pins who is on which side.
     */
    public static QuiverState stateOf(ItemStack held, Keys keys, WeaponDefinition weapon) {
        OptionalInt loaded = QuiverItems.loadedIn(held, keys);
        // THE SINGLE SITE THAT RESOLVES A CAPACITY FOR READING, and the reason it is one site rather
        // than a convention. QuiverState.capacityOf owns the stamp-then-authored ordering; a second
        // caller writing `stamped.orElse(weapon.quiverSize())` inline would be a second resolver,
        // and the moment two exist the tooltip and the refusal logic can disagree -- which is the
        // exact defect the stamp was introduced to prevent. QuiversSignatureTest fails the build if
        // a sixth resolver appears in EITHER module. Widened from paper-only in commit 1c: capacityOf
        // is public on a CORE class, so a paper-only scan was a claim true by accident of where the
        // code sits.
        OptionalInt stamped = QuiverItems.capacityIn(held, keys);
        Long startedAt = read(held, keys.quiverReloadStartedAt);
        Long completesAt = read(held, keys.quiverReloadCompletesAt);
        OptionalLong from = startedAt == null ? OptionalLong.empty() : OptionalLong.of(startedAt);
        OptionalLong to = completesAt == null ? OptionalLong.empty() : OptionalLong.of(completesAt);
        // NO DECISION HERE. QuiverState.from resolves the capacity, so the stamp-beats-authored rule
        // is exercised by a core row rather than by a source scan over this call site -- see that
        // factory's javadoc for the mutation that walked through here green.
        //
        // WHAT IS STILL UNWITNESSED, SAID RATHER THAN LEFT: that the five values below are the REAL
        // reads. A mutation passing OptionalInt.empty() in place of `stamped` would be green, because
        // nothing here can be unit-tested -- stateOf needs an ItemStack. The DECISION moved to core
        // and is covered; this ARGUMENT PASSING is boot-only, and GATE-quiver-a2 carries the row.
        return QuiverState.from(loaded, stamped, weapon.quiverSize(), from, to);
    }

    /**
     * If the held weapon's reload has matured, finish it NOW. Otherwise do nothing.
     *
     * <p><b>This is the one step a deciding reader owes the lazy design</b>, and the class javadoc's
     * rule -- <i>every site that decides from the magazine settles a matured reload first</i> -- is
     * what it exists to make cheap. Call it, then read {@link #stateOf} off a FRESHLY read main hand.
     *
     * <h2>IT IS THE SAME STEP {@code resolveForShot} AND {@code beginReload} ALREADY TAKE. NOTHING
     * HERE IS NEW</h2>
     *
     * <p>Both of those switch on a core verdict and reach {@code finishReload} on their
     * {@code RELOAD_MATURED} arm. This is that arm, on its own, for callers that are not taking a shot
     * and are not starting a reload -- <b>{@code PlumeDraw}'s draw cap and {@link QuiverReloadCue}'s
     * task, which are respectively a DECISION and a moment in time.</b>
     *
     * <p>{@link QuiverState#fireVerdict} is the predicate rather than a new one, deliberately: a
     * second expression of "has this reload matured" is a second home for it, and
     * {@code QuiverStateTest} covers this one. The other four arms are no-ops here by construction --
     * {@code FIRE}, {@code EMPTY} and {@code RELOADING} have nothing to settle, and {@code UNSTAMPED}
     * is repaired on the FIRING path, which is the only place that should invent a count (see
     * {@code resolveForShot}'s arm and {@code beginReload}'s refusal of the same case).
     *
     * <h2>IDEMPOTENT, WHICH IS WHAT MAKES IT SAFE TO CALL EVERY TICK</h2>
     *
     * <p>{@code finishReload} removes all three reload keys, so the second call finds no
     * {@code reloadStartedAt} and returns {@code FIRE} or {@code EMPTY}. <b>At most one write per
     * reload, however many times this is called</b> -- which matters because {@code PlumeDraw.tick}
     * calls it once a tick for the whole length of a draw.
     *
     * <h2>*** WRITING THE MAIN HAND MID-DRAW CANNOT CANCEL THE DRAW, AND THAT WAS MEASURED ***</h2>
     *
     * <p>This method's two new callers can both fire while the player is holding a bow back, which is
     * the one moment at which replacing the held stack looked dangerous. Read off
     * {@code run/versions/26.1.2/paper-26.1.2.jar} with {@code javap -c}:
     *
     * <pre>
     * LivingEntity.updatingUsingItem()
     *    19:  ItemStack.isSameItem(getItemInHand(usedHand), useItem)
     *    22:  ifeq 48          &lt;- NOT the same item -&gt; 48
     *    34:  putfield useItem &lt;- same item: RE-POINT the field at the new stack
     *    49:  stopUsingItem()  &lt;- 48: THE DRAW IS CANCELLED
     *
     * ItemStack.isSameItem(a, b)
     *     2:  b.getItem()
     *     5:  a.is(Object)     &lt;- ITEM TYPE ONLY. No components, no NBT, no count.
     * </pre>
     *
     * <p><b>So a bow replaced by a bow passes, the field is re-pointed at the new stack, and the draw
     * continues.</b> The charge is unharmed too: {@code getTicksUsingItem} is
     * {@code useItem.getUseDuration(this) - useItemRemaining}, and neither term is touched by the
     * re-point -- the remaining count is not reset, and a bow's use duration is a constant.
     *
     * <p><b>It is written here rather than in a plan because the next person to hesitate over this
     * will be reading this method.</b> Had {@code isSameItem} compared components, every quiver write
     * would have been a draw-cancel and the whole shape would have been wrong -- so the answer is
     * load-bearing, and it is a bytecode reading rather than a recollection.
     *
     * <p>What the jar cannot answer is what the CLIENT does with a re-sent hotbar slot mid-draw: the
     * server's draw state is unaffected, but the pull ANIMATION is the client's own. {@code GATE-quiver-feedback.md}
     * row <b>P3</b> is the reading, and it is a cosmetic question rather than a correctness one.
     *
     * @param player MUST be the owner of this thread -- this reads and writes their main hand.
     */
    public static void settleMaturedReload(Player player, WeaponDefinition weapon,
                                           AdapterContext adapters) {
        ItemStack held = player.getInventory().getItemInMainHand();
        QuiverState state = stateOf(held, adapters.keys(), weapon);
        if (state.fireVerdict(Bukkit.getCurrentTick()) == QuiverState.Fire.RELOAD_MATURED) {
            finishReload(player, held, weapon, adapters);
        }
    }

    /**
     * Spend {@code rounds} rounds off the held weapon. Called only after a Success.
     *
     * <p>Takes the DEFINITION as well as the player because the write carries its own render, and
     * rendering needs to know what the weapon IS -- see {@link QuiverItems#setLoaded}. It gained that
     * parameter when the display half of boot row V1 failed.
     *
     * <h2>ONE READ, ONE ARITHMETIC CALL, ONE WRITE -- WHATEVER THE COUNT</h2>
     *
     * <p>A five-arrow release does NOT call this five times. Each call is a read-modify-write of a
     * live {@code ItemStack} plus an {@code updateInventory}, so five would be five tooltip renders
     * for one press, and four of them describing counts the player never had.
     *
     * <p><b>AND THE ARITHMETIC STAYS IN {@code core}. The subtraction is NOT written here.</b> It
     * would have been one character -- {@code loaded - rounds} -- and it would have given the
     * magazine a SECOND HOME: one in {@link Quiver} doing {@code -1}, one here doing {@code -n}, and
     * nothing making them agree. {@code Quiver.spend} took a signature change instead, so a caller
     * that misses the count is a compile error rather than a weapon that quietly bills one round for
     * five arrows.
     *
     * @param rounds how many rounds this press costs -- ONE for every weapon that fires a single
     *               shot, and the arrow count for the Dragon's Plume's fanned release
     */
    public static void spendRounds(Player player, WeaponDefinition weapon, AdapterContext adapters,
                                   int rounds) {
        Keys keys = adapters.keys();
        ItemStack held = player.getInventory().getItemInMainHand();
        OptionalInt loaded = QuiverItems.loadedIn(held, keys);
        if (loaded.isEmpty()) return;   // warned about in resolveForShot; never silently invent a count

        int spent = Quiver.spend(loaded.getAsInt(), rounds);
        // WRITE AND RENDER IN ONE CALL. Writing the key alone is what shipped: the stored count
        // moved, the tooltip did not, and it looked correct until a relog re-minted the item.
        held.editMeta(meta -> QuiverItems.setLoaded(meta, weapon, adapters, player.getUniqueId(), spent));
        // Write the stack back explicitly rather than trusting the main-hand read to be a live
        // mirror, and updateInventory so the tooltip moves on this shot -- the same pair, for the
        // same reasons, as WeaponDurability.applyWearOnUse.
        player.getInventory().setItemInMainHand(held);
        player.updateInventory();
    }

    /**
     * Begin a reload on the held weapon, if one is warranted.
     *
     * @return true if a reload actually started -- so the caller can speak only on a real transition
     *         rather than on every one of the ~20 left-click packets a held button produces each
     *         second.
     */
    public static boolean beginReload(Player player, WeaponDefinition weapon, AdapterContext adapters,
                                      CooldownTracker cooldowns) {
        Keys keys = adapters.keys();
        ItemStack held = player.getInventory().getItemInMainHand();
        long now = Bukkit.getCurrentTick();

        // SLICE E: WHAT THE RELOAD WOULD COST, AND WHAT THE PLAYER CAN PAY -- both read BEFORE the
        // verdict, because the ammo rung needs the second one.
        //
        // The DECISION is core's and takes an int; the READ is here. That is the same split
        // QuiverSize.resolve's javadoc argues for, and it is why no ItemStack goes near core.
        QuiverState state = stateOf(held, keys, weapon);
        QuiverAmmo.Supply supply = QuiverAmmo.supply(player, keys, state.roundsNeeded());

        // Same split as resolveForShot: the verdict is core's, and each arm below is a core row.
        // ALREADY_RELOADING is the held-input case -- ~20 arm-swing packets a second, every one of
        // which would otherwise push the deadline another reload_ticks away and leave a weapon that
        // never comes back. ALREADY_FULL spares a habitual press three dead seconds.
        switch (state.reloadVerdict(now, supply.rounds())) {
            case ALREADY_RELOADING, ALREADY_FULL -> { return false; }
            case RELOAD_MATURED -> {
                finishReload(player, held, weapon, adapters);
                return false;
            }
            case UNSTAMPED -> {
                // Repaired on the firing path, which every quiver weapon reaches first; reloading a
                // never-stamped item is not the place to invent a count.
                return false;
            }
            case NO_AMMO -> {
                // ROOM BUT NO ARROWS -- ruling 4. Its OWN notice, never the empty-magazine one:
                // "already full" or "out of ammo" on a weapon reading 7/8 is the kind of thing a
                // player screenshots. QuiverNotice keys it separately so two refusals cannot silence
                // each other through one throttle.
                QuiverNotice.noAmmo(player, cooldowns);
                return false;
            }
            case BEGIN -> { /* fall through to the write below */ }
        }

        // THE LAST SUPPLY SITE -- and "supply" is the word, not "the only reader", because the two
        // are different claims and the second one is false.
        //
        // SUPPLY means: reads the weapon's authored reload TO DRIVE BEHAVIOUR. There is exactly one,
        // and it is this line. READOUT means: reads it to show somebody a number. RpgCommand has
        // three, and every one prints the RESOLVED value beside the authored one, so a readout
        // cannot silently become a second source of truth.
        //
        // THE DISTINCTION IS WRITTEN THIS WAY BECAUSE THE COUNT WAS NOT. This comment first said
        // `grep -rn "reloadTicks()" ... finds exactly this one` -- true when drafted, and THE SAME
        // COMMIT WROTE ITS REFUTATION sixty lines away in the /rpg reloadtime block. It now finds
        // four in main source, six unscoped. That is A1's "two call sites" error from the other
        // side: a grep quoted with its command is the most trustworthy-looking form a count can
        // take, which is why a stale one costs more than a vague sentence -- the next reader runs
        // it, gets four, and stops believing the comments that are right.
        //
        // The membership is guarded rather than counted:
        // QuiversSignatureTest.theAuthoredReloadDurationIsReadOnlyWhereThisListSays names the files and
        // their roles, so a new one is a deliberate edit.
        //
        // AND THE DURATION IS RESOLVED HERE, AT THE BEGIN, THEN NEVER AGAIN. The deadline below is
        // stamped from it and no read recomputes it, so gear equipped mid-reload cannot lengthen or
        // shorten a timer already running. A1 made that unrepresentable rather than guarded by
        // removing reloadTicks from Quiver.reloadComplete's parameters; this line is the one place
        // the live duration is allowed to matter.
        int reloadTicks = ReloadTime.resolve(weapon.reloadTicks(),
                adapters.stats().reloadTimeBonusValue(player.getUniqueId()));

        // RULING 3: THE ARROWS ARE TAKEN AT THE START, NOT AT MATURITY. An interrupted reload costs
        // them -- deliberate, unusual, and indistinguishable from a bug unless the record says it was
        // chosen, so GATE-quiver-ammo.md row 3 predicts it in writing.
        //
        // TAKEN HERE, past every refusal arm above, because beginReload "returns true only on a REAL
        // TRANSITION" and that property is what makes it safe to call from the swing path -- roughly
        // twenty arm-swing packets a second. A debit above the switch would charge the player on
        // every one of them.
        //
        // A no-op in creative: supply() returns no draws there, so no second mode check is needed.
        QuiverAmmo.consume(player, supply.draws());

        // ONE DEADLINE, COMPUTED ONCE. It is stamped on the item AND handed to the cue task, and the
        // two must be the same number: the task's only question at fire time is whether the item still
        // carries the deadline it was made for. Recomputing it in the scheduler call would be a second
        // source for one value -- which is how a cue goes silent for a reason nobody can see.
        long completesAt = Quiver.reloadCompletesAt(now, reloadTicks);

        held.editMeta(meta -> {
            meta.getPersistentDataContainer().set(keys.quiverReloadStartedAt,
                    PersistentDataType.LONG, now);
            meta.getPersistentDataContainer().set(keys.quiverReloadCompletesAt,
                    PersistentDataType.LONG, completesAt);
            // THE PENDING COUNT, written with the two stamps and removed with them. It exists because
            // the amount was decided HERE, from an inventory that will have moved on by the time the
            // reload matures -- see Keys.quiverReloadPending for why re-deriving is wrong twice over.
            meta.getPersistentDataContainer().set(keys.quiverReloadPending,
                    PersistentDataType.INTEGER, supply.rounds());
            // THE COOLDOWN GROUP, STAMPED IN THE SAME EDIT AS THE DEADLINE, and that pairing is the
            // whole migration story: a weapon minted before the sweep existed carries no group, and
            // the first reload it starts gives it one. Nothing has to walk the world's inventories.
            //
            // It is inside editMeta because it is an item write like the three above it; the sweep
            // itself is a PLAYER write and therefore sits below, after the item is back in the hand.
            QuiverSweep.ensureGroup(meta, weapon.id(), keys);
        });
        player.getInventory().setItemInMainHand(held);
        player.updateInventory();
        // THE SWEEP, AFTER THE ITEM IS BACK IN THE HAND AND NOT BEFORE. The client renders the
        // overlay against the stack it is holding, and the group it reads comes from that stack's
        // component -- so a cooldown set while the old, group-less copy was still in the slot would
        // be a cooldown in a group nothing in the hotbar belongs to.
        //
        // reloadTicks rather than a re-read of the deadline: this is the one moment the two cannot
        // disagree, because the deadline was computed from this very number three lines ago.
        QuiverSweep.show(player, weapon.id(), keys, reloadTicks);

        // THE CUE, BOOKED FOR THE RELOAD'S REAL LENGTH -- reloadTicks is post-modifier, resolved above
        // through ReloadTime.resolve, so a player with a reload-time bonus hears it on time rather
        // than at the authored number.
        //
        // AFTER the item is back in the hand, for the same reason the sweep is: the task will read the
        // main hand when it fires, and the stamped copy has to be the one sitting there.
        QuiverReloadCue.schedule(player, weapon, adapters, completesAt, reloadTicks);
        return true;
    }

    /**
     * Start a reload if the player's held item is a quiver weapon that wants one.
     *
     * <p>The swing listener's entry point: it holds a {@code Player} and no definition, so the
     * held-weapon resolution lives here rather than being a second copy at the call site.
     *
     * @return true only on a real transition -- a reload that actually began.
     */
    public static boolean tryReloadHeldWeapon(Player player, WeaponRegistry weapons,
                                              AdapterContext adapters, CooldownTracker cooldowns) {
        return WeaponItems.heldWeaponId(player, adapters.keys())
                .flatMap(weapons::find)
                .filter(WeaponDefinition::hasQuiver)
                .map(weapon -> beginReload(player, weapon, adapters, cooldowns))
                .orElse(false);
    }

    /**
     * Add the rounds that were PAID FOR and clear the reload trio. All three keys go together or none
     * does.
     *
     * <h2>THE ONE FUNCTION BOTH MATURITY PATHS SHARE, AND THAT IS LOAD-BEARING</h2>
     *
     * <p>{@code RELOAD_MATURED} exists on BOTH verdict ladders and reaches here from both: the reload
     * path ({@code beginReload}) and the FIRE path, which exists so <i>the press that matures a
     * reload is not wasted</i> — the read refills the item and the shot goes through on the same tick.
     *
     * <p><b>Both refill by the STORED pending amount, and they cannot drift because there is one
     * function.</b> If this is ever split into two, that is the defect: a slice could fix the partial
     * refill on one path and leave the other reloading to capacity, and the difference would show up
     * only when a player happens to mature a reload by shooting rather than by pressing reload.
     *
     * <h2>Why it reads the key rather than recomputing</h2>
     *
     * <p>The amount was decided a whole reload duration ago, from an inventory that has since moved
     * on. Re-reading it would refill by arrows the player never paid for; re-deriving from capacity
     * would silently restore the all-or-nothing reload ruling 5 refused. See
     * {@code Keys.quiverReloadPending}.
     *
     * <p><b>A missing pending count yields zero rounds</b>, not a full magazine: an item stamped by a
     * build before Slice E has the two timestamps and no third key, and the conservative answer there
     * is to add nothing rather than to grant a free magazine. The reload still CLEARS, so the weapon
     * is usable again immediately and the player can simply reload once more — paying for it.
     *
     * <h2>*** THE ARGUMENT PASSING BELOW IS UNWITNESSED, AND IT WAS MEASURED RATHER THAN ASSUMED ***</h2>
     *
     * <p><b>{@code MUT-PENDING} was RUN: replacing the stored count with the resolved capacity here —
     * the exact defect this whole slice exists to prevent — reddened NOTHING across all 1612 rows.</b>
     *
     * <p>The ARITHMETIC is guarded: {@code MUT-RELOAD} on {@link Quiver#reload} reddens two core rows.
     * What nothing can reach is whether THIS method hands it the right numbers, because it needs a
     * {@code Player} and an {@code ItemMeta} and no unit test in this project has either. Same shape
     * as {@code stateOf}'s own note — <i>"a mutation passing OptionalInt.empty() in place of
     * {@code stamped} would be green, because nothing here can be unit-tested"</i> — and the same
     * remedy: the gate carries the row, and the green suite around this block is NOT coverage of it.
     *
     * <p><b>{@code GATE-quiver-ammo.md} row 1 is what catches it</b>, and it catches it precisely: a
     * partial load of 7 into an 8-round Boltor reads {@code 7/8} when this is right and {@code 8/8}
     * when it is wrong. Do not weaken that row, and do not read the suite total as protection here.
     */
    private static void finishReload(Player player, ItemStack held, WeaponDefinition weapon,
                                     AdapterContext adapters) {
        Keys keys = adapters.keys();
        int pending = pendingRounds(held, keys);
        int loaded = QuiverItems.loadedIn(held, keys).orElse(0);
        held.editMeta(meta -> {
            // setLoaded, not stampFull: stampFull is MINT-ONLY, where applyLore follows by
            // construction. This item is in play, so the write must carry its own render.
            //
            // Quiver.reload does the clamp, so the resolved capacity bound lives in one place even
            // though this method now supplies three numbers instead of one.
            QuiverItems.addRounds(meta, weapon, adapters, player.getUniqueId(), loaded, pending);
            meta.getPersistentDataContainer().remove(keys.quiverReloadStartedAt);
            meta.getPersistentDataContainer().remove(keys.quiverReloadCompletesAt);
            meta.getPersistentDataContainer().remove(keys.quiverReloadPending);
        });
        player.getInventory().setItemInMainHand(held);
        player.updateInventory();
        // AND THE SWEEP GOES OUT WITH THE DEADLINE IT WAS DRAWING. This is the path a MATURED reload
        // takes, and it is reached lazily -- from a shot, or from any read that finds the deadline
        // passed -- so the sweep would otherwise sit at zero-width until something else cleared it.
        //
        // Clearing it here rather than trusting expiry is not belt-and-braces: a reload can mature
        // EARLY on this path (finishReload is also called when a reload is settled at the start of
        // another one), and an overlay that outlives its reload is the exact failure the brief named.
        QuiverSweep.clear(player, weapon.id(), adapters.keys());
    }

    /** The rounds a running reload will deliver, or 0 if the item carries no pending count. */
    private static int pendingRounds(ItemStack item, Keys keys) {
        if (item == null || !item.hasItemMeta()) return 0;
        Integer stored = item.getItemMeta().getPersistentDataContainer()
                .get(keys.quiverReloadPending, PersistentDataType.INTEGER);
        return stored == null ? 0 : stored;
    }

    private static Long read(ItemStack item, org.bukkit.NamespacedKey key) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.LONG);
    }
}
