package io.github.butterflysmp.rpg.core.combat;

/**
 * Scorch's arithmetic: what one damage tick is worth, and how many stacks a hit is worth.
 *
 * Pure and dependency-free, beside {@link Defense} and for the same reason -- the numbers are the
 * design, and they are testable without a server. The STATE (stacks, the timer, the applier) lives in
 * {@code paper/adapter/ScorchStatus}; only the maths is here.
 *
 * <h2>THE RATE IS FLAT. STACKS DO NOT SCALE IT.</h2>
 *
 * The operator said both of these, in this order:
 *
 * <pre>
 *   FIRST:      "the amount of scorch stacks a target has determines the damage it takes per second"
 *   SUPERSEDED: "on second thought, let's change the numbers a bit"  -&gt;  a FLAT rate
 * </pre>
 *
 * <b>The revision is a decision, not a transcription slip, and the arithmetic is what proves it.</b>
 * The Flint Staff applies TEN stacks. At 5% per stack per second that is 50%/sec -- a two-second kill
 * on anything -- which is precisely the outcome the operator ruled out: <i>"Scorch/Ignite is NOT MEANT
 * TO BE AN EXECUTION, it's meant to be a crowd control that comes from sustained damage."</i>
 *
 * Both statements are recorded here so nobody re-derives the first one from the archive later. What
 * scorch's PRESENCE does is refresh the timer and gate the fire-tick suppression in
 * {@code RpgListeners.onEnvironmentalDamage}. What its COUNT does is nothing -- see below.
 *
 * <h2>THE STACK COUNT HAS NO CONSUMER, AND THE ACCUMULATOR WAS DELETED RATHER THAN LEFT UNREAD</h2>
 *
 * This paragraph read <i>"they carry Ignite's threshold in slice 2"</i>, which was the whole
 * justification for accumulating a count at all. <b>The operator ruled on 2026-09-09 that any mob
 * which dies while scorched ignites -- binary, no count</b> -- so that consumer will never exist.
 *
 * <p>{@code ScorchStatus.Active.stacks} and its {@code stacks(UUID)} accessor were deleted in the
 * same commit as this line, deliberately and not as tidying. <b>A write-only field is worse than a
 * deleted one:</b> the next person wanting a count would find it, read it, and get a number no
 * consumer has ever validated -- and that number carried a recorded defect, an unclamped
 * {@code +=} that let a bow pile ~30 stacks into one window. Keeping it would have preserved the
 * defect and removed the only thing that could ever have surfaced it.
 *
 * <p><b>THE RE-ADD TRIGGER, so this is a decision someone makes rather than something rediscovered:
 * a count comes back only WITH a consumer that defines what it means.</b> That is the moment the
 * unclamped-accumulation question gets answered instead of inherited.
 *
 * <p><b>THE HONEST SCORE FOR BUILDING AHEAD OF A SPEC, recorded because it is unusually clean.</b>
 * {@code ScorchStatus} shipped TWO methods built for a slice-2 consumer that did not exist yet, and
 * nothing called either of them. The ruling resolved both in the same sentence: it <b>killed</b>
 * {@code stacks(UUID)} -- ignite reads no count -- and <b>gave {@code applier(UUID)} its first
 * consumer</b>, since ignite credits whoever lit the fire. Both were speculative; exactly one
 * guessed right. That is the base rate to remember the next time a method is written for a spec that
 * has not been ruled on.
 *
 * <h2>SCORCH IS ONLY HALF THE FIRE KIT -- {@link Ignite} IS THE OTHER HALF</h2>
 *
 * The first person tuning scorch reads this file, and every scorch rate is deliberately here so they
 * find it. <b>Ignite's numbers are NOT here</b> -- radius, damage and fuse live in {@link Ignite},
 * because a propagation engine that happens to be TRIGGERED by scorch is a different mechanism, not
 * more scorch constants. Read that file too, or you have tuned the burn and left the explosion it
 * causes untouched.
 *
 * <p><b>Consequence to know before relying on {@link #stacksFor}'s magnitude:</b> with the
 * accumulator gone, {@code ScorchStatus.apply} reads its {@code stacks} argument only as a GATE
 * ({@code stacks <= 0} means the hit bought none and applied nothing). So {@code stacksFor}'s
 * arithmetic is live, but only its zero / non-zero result is read today. The first consumer of the
 * count is also the first reader of the magnitude.
 *
 * <h2>THE RULING DID NOT ONLY DELETE A THRESHOLD -- IT DELETED THE COUNT'S ONLY CONSUMER</h2>
 *
 * <b>Scorch is now a boolean burn with a timer, a cap and a credit.</b> "Stacking" survives only in
 * this class's name, in prose, and in {@code scorch.yml}'s player-facing <i>"1 per 2 damage dealt"</i>
 * -- nothing computes with it.
 *
 * <p><b>THE DECISION: SCORCH STOPS BEING A STACKING STATUS.</b> Not in this commit -- collapsing it
 * reaches through {@code ScorchStatus.apply}'s signature, {@code ElementAccrual} and the content
 * prose, which is a mechanism sweep inside a slice that has not started.
 *
 * <p><b>And NOT "later", which is how {@link #DEFAULT_DURATION_TICKS}, {@code ScorchStatus.stacks}
 * and {@code ScorchSinkSignatureTest} each survived a slice.</b> The trigger is exact:
 *
 * <blockquote>WHEN IGNITE'S MECHANISM LANDS AND THE COUNT STILL HAS NO READER, THE SWEEP HAPPENS IN
 * THAT SLICE'S OWN CLEANUP -- NOT A FUTURE ONE.</blockquote>
 *
 * By then it is measured from both ends and there is nothing left to wait for.
 *
 * <p><b>DO NOT invent a consumer to justify the machinery.</b> "The blast scales with stacks" would
 * make the count live again, and it is a FEATURE -- adding one to rescue a constant is the tail
 * wagging the dog, and it would re-introduce the unbounded accumulator that {@code NEXT.md} refused
 * to declare a ceiling for.
 *
 * <h2>TWO DIFFERENT SPECIES LIVE IN {@link #stacksFor}, AND THEY WANT DIFFERENT TREATMENT</h2>
 *
 * <ul>
 *   <li><b>{@link #DAMAGE_PER_STACK}'s VALUE has no consumer at all.</b> Its only arithmetic reader
 *       is the division in {@link #stacksFor}; every other mention in the repo is prose. Since the
 *       only reader of that result is {@code stacks <= 0}, and {@code max(1, ...)} returns >= 1 for
 *       every positive input, <b>2, 7 or 1000 give IDENTICAL production behaviour.</b> This is not
 *       unreachability -- the line executes on every hit. Its output MAGNITUDE is never read. The
 *       mutation that proves it: change the constant and nothing reddens except rows asserting the
 *       number itself.</li>
 *   <li><b>The {@code max(1, ...)} FLOOR is separately CONTENT-UNREACHABLE, and is KEPT.</b> It can
 *       only matter for {@code dealt} in {@code (0, 2)}, and nothing produces that today: the
 *       smallest authored fire amount is {@code 2} ({@code solar_grenade}'s field tick), and the
 *       mitigation route below needs a scorchable target with defense > 0, which does not exist --
 *       every mob is defense 0 and nothing can scorch a player. <b>A content author writing
 *       {@code amount: 1} reaches it immediately</b>, so it owes forward cover rather than
 *       deletion: it is the only thing between such a hit and no scorch at all.</li>
 * </ul>
 *
 * <h2>THE CAP IS WHAT MAKES THE DoT RESPOND TO THE APPLIER -- the job stacks would otherwise have</h2>
 *
 * Percent-max-health scales UP with the pool, so it runs away on a boss. The cap stops it, and a
 * stronger fire weapon buys more scorch damage:
 *
 * <pre>
 *   100 HP player, 20-dmg staff -&gt; 5% = 5    cap 20 -&gt; cap NEVER binds
 *   360 HP Knell,  20-dmg staff -&gt; 5% = 18   cap 20 -&gt; barely binds
 *   5000 HP boss,  20-dmg staff -&gt; 5% = 250  cap 20 -&gt; CAP HOLDS IT TO 20
 * </pre>
 *
 * <b>AT MAX 100 THE CAP NEVER BINDS, WHICH IS WHY EVERY TEST AT THE DEFAULT MAX IS BLIND TO IT.</b> A
 * working cap and a cap deleted entirely are the same number there. {@code ScorchTest} pins it at a
 * max where it binds; anything less would pass whether this {@code min} exists or not.
 *
 * <h2>ARMOUR DELAYS THE BURN RATHER THAN BLUNTING IT</h2>
 *
 * Armour reaches scorch by three channels and the operator ruled on all three: the DoT BYPASSES
 * Defense (see {@code CombatantStats.damage}'s {@code bypassesDefense}); stack accrual is POST
 * mitigation ({@link #stacksFor} is fed what actually landed); and the cap is the weapon's AUTHORED
 * number, not what it landed. So armour buys time and does not reduce the burn once it is on
 * you -- coherent with crowd control that comes from sustained damage, and the reason the DoT is not
 * "ordinary damage with extra arithmetic".
 *
 * <p><b>The heading says THE BURN, and the narrowing is deliberate.</b> It previously said SCORCH,
 * which read as a claim about everything scorch does. It is a claim about the DoT only, and the
 * reason is specific to the DoT's shape: it is a PERCENT-OF-MAX effect on a clock, so armour
 * blunting it would mean armour reducing a fraction of your own health. <b>That reasoning does not
 * transfer to anything that deals a flat number once.</b>
 *
 * <p><b>And armour does NOT delay ignition, which the old wording claimed.</b> That sentence read
 * "armour buys time to ignite", true only while ignition needed a stack COUNT to accumulate. Under
 * the 2026-09-09 ruling ignition needs one stack, and {@link #stacksFor} floors at ONE for any
 * damage that lands -- so an armoured target is ignition-ready on the first hit that gets through,
 * exactly like an unarmoured one. Armour buys time on the burn. It buys none on ignition.
 */
public final class Scorch {

    private Scorch() {}

    /**
     * The burn, as a fraction of the victim's MAX health per second.
     *
     * Not a tuning guess: {@code GATE-vanilla-damage.md} D15 measured 391 {@code FIRE_TICK} events and
     * found vanilla's own fire tick already lands here -- a skeleton takes 1 HP/sec on a 20 HP pool,
     * a player {@code raw=1 -> applied=5} on a 100 max. Both are 5% of max per second. So this slice
     * changes ATTRIBUTION, PACING and the CAP; for a standard entity it barely changes magnitude.
     */
    public static final double RATE_PER_SECOND = 0.05;

    /**
     * Scorch's own clock, in ticks. TWENTY, and it is owned rather than inherited.
     *
     * {@code NEXT.md:833}: a rate implemented as "deal damage while the status is active" has no clock
     * of its own and is paced entirely by the invulnerability window -- correct-looking until anything
     * writes {@code lastHurt}, then 20 Hz. That finding cost a boot, a diagnosis and an unusable test
     * world (D3/D3a).
     *
     * <b>Vanilla fire is NOT in that class</b> -- D15's traces show it ticking steadily every 20 ticks
     * under an already-poisoned {@code lastHurt}, which puts it in DROWNING's row, not LAVA's. The
     * reason not to ride it is different and simpler: {@code fireTicks} is not ours. Rain, water, a
     * second fire source and {@code /extinguish} all write it, and it can carry neither the cap, nor
     * the applier, nor the Defense bypass.
     */
    public static final int PERIOD_TICKS = 20;

    /**
     * The default window, and now the ONLY one -- six seconds.
     *
     * <p><b>THIS SUPERSEDES THE "8 SECONDS BY DEFAULT" RULING, AND THAT RULING WAS CITED AS THE ENTIRE
     * JUSTIFICATION FOR THE CONTENT PASS.</b> The reasoning survives untouched: the eight authored
     * {@code duration_ticks} in content kept their values while changing their meaning under scorch
     * slice 1 and were never re-decided, so ONE duration everywhere is still right. Only the NUMBER
     * moved, 160 -> 120. `PLAN-element-content-pass.md` carries the same correction; the squash body
     * of the content pass cannot be edited and is superseded here.
     *
     * <p>It is also no longer "for the weapon-damage entry point only". The nine explicit
     * {@code status: scorch} content sites were stripped in that pass, so every scorch in the game --
     * ability, weapon, field pulse -- now comes from accrual and runs for this long. The javadoc said
     * "only" while it had no production caller at all; it has one caller now, and that caller is
     * everything.
     *
     * <p>At {@link #PERIOD_TICKS} this is SIX damage ticks at t=20..120, over exactly 6.0 seconds. The
     * first burn lands one period IN rather than on application -- see {@code ScorchStatus}, where
     * deleting the inline first burn and flipping the tick body to burn-then-decrement are one change
     * and cannot be separated.
     */
    public static final int DEFAULT_DURATION_TICKS = 120;

    /** Damage dealt, post-mitigation, that buys one stack. */
    public static final double DAMAGE_PER_STACK = 2.0;

    /**
     * The fraction of a hit's magnitude that becomes its burn's CEILING.
     *
     * <p><b>Named, and named HERE, because every other scorch rate is here.</b> {@link #RATE_PER_SECOND},
     * {@link #PERIOD_TICKS}, {@link #DAMAGE_PER_STACK}, {@link #DEFAULT_DURATION_TICKS} and
     * {@link #UNDECLARED_CAP} all live in this file, and the first person tuning scorch will read this
     * file. A bare {@code * 0.5} in a paper adapter would be the only rate in the system stated
     * somewhere else, and they would not find it.
     *
     * <p>Applied at the accrual site to the hit's PRE-mitigation magnitude -- post-payload-resolution,
     * post-charge, post-crit, before Defense. A crit therefore still raises the ceiling, now from a
     * halved base. Armour never lowers it: armour reaches scorch through the STACK COUNT, and letting
     * it touch the ceiling too would be the back door the Defense bypass was ruled out of.
     *
     * <h2>THE CAP IS THE BOSS PROTECTION. THE 5% ARM IS THE THING THAT WOULD RUN AWAY.</h2>
     *
     * Stated because it is easy to get backwards, and was. {@code content/statuses/scorch.yml} has it
     * right: <i>"cap -- the damage of whatever applied the stacks, so a stronger fire weapon buys a
     * stronger burn, and a 5000 HP boss is NOT MELTED by percent-max-health running away (5% of 5000
     * is 250/sec, uncapped)"</i>. {@link #damagePerTick} is {@code min(5% of max, cap)}: the percent
     * arm scales the burn with the VICTIM, and the cap is what stops that scaling from running away on
     * a large pool.
     *
     * <h2>WHAT HALVING ACTUALLY CHANGED: THE TARGET-SIZE RANGE, NOT REACHABILITY</h2>
     *
     * The 5% arm binds when {@code 0.05 * max < cap}, so:
     *
     * <pre>
     *   before halving   the arm binds below   max = 20 x hit magnitude
     *   after  halving   the arm binds below   max = 10 x hit magnitude
     * </pre>
     *
     * <b>So this HALVED THE RANGE OF TARGET SIZES over which the burn scales with the victim.</b> The
     * percent arm governs small targets and the cap governs large ones, before and after. It did NOT
     * make the arm unreachable: it stays fully live on any 20-HP vanilla mob, for every weapon, where
     * {@code min(1.0, cap)} is 1.0 either way.
     *
     * <p><b>Measured on the one custom mob that ships.</b> A knell is 360 max, so 5% is 18. A Flint
     * Staff's 20-damage bolt capped at 20 gave {@code min(18, 20) = 18} -- the arm binding. Halved to
     * 10 it gives {@code min(18, 10) = 10} -- the CAP binding. The arm is dormant on a knell now,
     * because 18 exceeds every shipped weapon's halved cap; it becomes live there again the day a
     * weapon hits for more than 36.
     *
     * <p>Ben ruled this from a measured burn rather than a remembered one: 6 x 18 = 108 on a knell,
     * 30% of the target, observed after the window moved 160 -> 120. The earlier request had been
     * made against 8 x 18 = 144.
     */
    public static final double CAP_FRACTION = 0.5;

    /**
     * The cap used when the thing applying scorch declares no damage number of its own.
     *
     * <p><b>{@link #CAP_FRACTION} DOES NOT APPLY TO THIS, AND THAT IS DELIBERATE.</b> The fraction
     * halves a WEAPON-DERIVED figure; this is a conservative constant standing in for the absence of
     * one. Halving it to 1.0 "for consistency" would be a tuning change nobody asked for, applied to
     * the one path that has no weapon behind it. The dev apply command passes this value straight to
     * {@code ScorchStatus.apply} without going through {@code ElementAccrual}, so it never meets the
     * fraction at all.
     *
     * <b>A NON-BINDING FALLBACK IS NOT A FALLBACK.</b> Defaulting to the 5% figure makes
     * {@code min(5% of max, 5% of max)} -- an UNCAPPED percent-max-health DoT, 250/sec against a 5000
     * HP boss from an ability whose sibling weapon is held to 20. It hands the boss-melting tool to
     * exactly the appliers the cap exists to restrain, while a cap field sits in the store looking like
     * it does something. And it is invisible at max 100, where the real cap never binds either.
     *
     * <b>THE RULE THAT FIXED THIS VALUE IS RETIRED, AND THE VALUE STAYS AT 2.0.</b> It read: <i>"an
     * undeclared cap must never exceed the SMALLEST DECLARED one, so an author who forgets cannot get
     * a stronger scorch than an author who declares"</i>, and 2.0 is {@code solar_grenade}'s field
     * tick, the smallest authored fire damage in shipped content.
     *
     * <p><b>That rule compared two AUTHORING ROUTES, and the content pass deleted one of them.</b> It
     * weighed "declare a status and give it damage" against "declare a status and forget the damage".
     * After {@code 1233469} stripped the nine explicit sites, nobody writes
     * {@code type: status, status_id: scorch} at all -- so there is no route on which anything can be
     * forgotten, and the comparison has no second side.
     *
     * <p><b>The two numbers also stopped being comparable.</b> This constant stands against
     * {@code caster.payloadDamage()} -- the cast-frozen headline, which {@link #CAP_FRACTION} never
     * touches. Accrual's cap is {@code amount * CAP_FRACTION}. The old assertion set a DAMAGE effect's
     * authored amount against a STATUS effect's fallback: two mechanisms' numbers, which stopped being
     * two spellings of one thing the day a single path applied scorch and it was the element.
     *
     * <blockquote>
     * <b>A RULE OUTLIVES ITS PREMISE SILENTLY, BECAUSE ITS ARITHMETIC KEEPS EVALUATING.</b> The
     * premise died at {@code 1233469}. The check went on returning a boolean for two more commits --
     * green on {@code 2 >= 2.0}, measuring nothing -- and only became visible at {@code eab7738},
     * when {@code CAP_FRACTION} made the two sides diverge by a factor of two. Halving the cap did
     * not break the rule; it made a dead rule's arithmetic diverge, which is the only reason anybody
     * looked.
     * </blockquote>
     *
     * <p><b>NOT LOWERED TO 1.0 TO KEEP THE OLD COMPARISON TRUE.</b> That would change LIVE behaviour
     * -- the dev apply command's burn on anything above 20 max health, halved -- to satisfy a rule
     * with no live instances. And {@code solar_grenade}'s field was NOT raised to 4 to satisfy it
     * either: that is a balance change to shipped content smuggled in as invariant maintenance, and
     * whether the field should hit for 4 is decided watching a grenade.
     *
     * <p><b>This is a different species from the ternary deleted in {@code 20cb8cf}, and the
     * difference decides whether forward cover is owed.</b> A GUARD WITH NO INSTANCES IS NOT A GUARD
     * THAT CANNOT FIRE: {@code ElementAccrual}'s fallback was unreachable BY THE MECHANISM
     * ({@code stacks > 0} implies {@code amount > 0}, always), so it was deleted outright. This is
     * unreachable BY TODAY'S CONTENT, and the schema still permits an explicit scorch tomorrow --
     * hence the cover note in {@code BukkitCombatant}'s Scorch arm, where the two caps would silently
     * disagree by a factor of two.
     *
     * <p><b>AND THE ENFORCEMENT QUESTION IS MOOT NOW, WHICH IS WORTH RECORDING BECAUSE IT WAS
     * ARGUED TWICE.</b> This javadoc once claimed a {@code ContentValidator} boot arm that did not
     * exist -- written from a plan rather than from the code, the same defect as
     * {@code flint_staff.yml}'s "THE MINTED STAFF STACKS TO 64" -- and was corrected to say only
     * bundled content was covered, with the boot arm still owed. <b>Neither is owed any more.</b>
     * There is no rule left to enforce at boot or at build: the comparison it would have made has
     * no second side. The build-time walk that carried it survives with its OTHER job intact --
     * {@code ScorchContentInvariantTest} still asserts it discovered every fire damage site, so a
     * scan gone blind reddens rather than reading clean.
     */
    public static final double UNDECLARED_CAP = 2.0;

    /**
     * What one scorch tick takes off {@code victimMaxHealth}, held to {@code cap}.
     *
     * {@code cap} is the AUTHORED damage of whatever applied the stacks -- never what that hit actually
     * landed, or armour would lower the cap and re-enter through the back door after being ruled out
     * of the DoT.
     *
     * Worked: {@code (100, 20) -> 5} (cap idle); {@code (360, 20) -> 18} (cap idle, barely);
     * {@code (5000, 20) -> 20} (CAP BINDS -- the only shape that can see the min).
     */
    public static double damagePerTick(double victimMaxHealth, double cap) {
        return Math.min(RATE_PER_SECOND * victimMaxHealth, cap);
    }

    /**
     * How many stacks {@code dealtPostMitigation} buys: one per {@link #DAMAGE_PER_STACK}, floored --
     * but never fewer than ONE for any damage that actually landed.
     *
     * Post-mitigation by the operator's ruling -- this is fed what actually landed, so armour lowers
     * the count a hit buys even though it does not touch the burn itself. <b>It does NOT slow any
     * climb toward ignite:</b> that phrasing outlived the threshold it described, and the floor below
     * means one landed hit is already enough to ignite on death.
     *
     * <b>THE FLOOR-TO-ZERO ARM WAS DELIBERATE, AND WIRING ACCRUAL FALSIFIED IT.</b> This method
     * previously returned 0 below {@code DAMAGE_PER_STACK}, on the argument that rounding would let
     * every glancing hit scorch. Sound while nothing called it. The moment accrual was wired it landed
     * on shipped content: {@code solar_grenade}'s field tick declares {@code amount: 2}, and against an
     * armoured target {@code Defense.applyDefense(2, 20) = 1.67} -- which floored to 0 stacks, and
     * {@code ScorchStatus.apply} early-returns on {@code stacks <= 0}. <b>The lingering field stopped
     * scorching armoured targets entirely</b>, on shipped content, discoverable only in game.
     *
     * <p><b>THAT WORKED EXAMPLE IS CURRENTLY UNREACHABLE, AND SAYING SO IS THE POINT.</b> It needs a
     * scorchable target with defense > 0; every mob is defense 0 and nothing can scorch a player, so
     * no live path produces sub-2 {@code dealt} today. The floor is kept as FORWARD COVER, not as a
     * guard anything currently exercises -- the day a payload authors {@code amount: 1}, or a mob
     * gains defense, it becomes live again with no other warning. Its only exercise is this unit
     * test, by construction; do not read the example above as something production reaches.
     *
     * <b>WHY THE OLD ARGUMENT WAS WRONG, WHICH IS NOT THE SAME AS BEING OVERRULED.</b> It said
     * <i>"1 damage must buy nothing rather than rounding up to a stack, or chip damage scorches."</i>
     * That treats a stack as the thing being CREATED, and against that risk it is correct reasoning.
     * But under {@code Math.max(1, ...)} what a chip hit actually does, almost always, is REFRESH a
     * burn that is already running -- {@code ScorchStatus.apply} adds the stack and resets the window
     * on a live burn, and only mints a new one when there is none. <b>Sustained damage keeping a burn
     * alive is the stated design</b> ("crowd control that comes from sustained damage"), not a leak.
     *
     * So the old reasoning was not outvoted. It was aimed at a risk the floor turns out not to create.
     * Recorded that way on purpose: "we decided differently" invites re-litigation, whereas "the
     * argument targeted the wrong failure" is checkable and settles it.
     *
     * It also overran the ruling it was written under. The Defense bypass was accepted on <i>"armour
     * DELAYS scorch rather than blunting it"</i>. Armour PREVENTING scorch outright is not "delays".
     *
     * <b>The floor is also what let the nine explicit {@code status: scorch} content sites be
     * stripped.</b> Their one real guarantee was "a fire hit burns"; {@code Math.max(1, ...)} keeps it
     * in a single mechanism, so accrual replaces them rather than trading that away.
     *
     * Non-positive still returns 0 -- a heal or a fully-absorbed hit accrues nothing. That arm is
     * untouched, and it is the one the floor must not swallow.
     *
     * <b>THE REMAINDER IS DISCARDED, AND THAT IS THE CHOICE RATHER THAN AN OVERSIGHT.</b> A 3-damage
     * hit buys one stack and drops 1; ten such hits buy ten stacks, not fifteen. There is deliberately
     * NO per-victim accumulator carrying the leftover forward, for two reasons: it would be state that
     * outlives the burn it belongs to (what happens to a remainder when the scorch expires, or when a
     * different applier takes over?), and it would make the stack a target reaches depend on the
     * ORDER its hits arrived in rather than on their total. Chip damage buying fewer stacks than its
     * raw total suggests is the intended shape, not a rounding artefact.
     *
     * <p><b>This paragraph's reasoning is preserved but currently unexercised.</b> It argued against
     * a remainder accumulator on the strength of what the stack COUNT would mean; since the
     * 2026-09-09 ruling nothing reads that count (see the class javadoc), so no caller can observe
     * the discarded remainder either way. Kept rather than deleted because it answers the question a
     * future count-consumer will ask first, and that consumer is exactly the re-add trigger.
     */
    public static int stacksFor(double dealtPostMitigation) {
        if (dealtPostMitigation <= 0) return 0;
        return Math.max(1, (int) (dealtPostMitigation / DAMAGE_PER_STACK));
    }

    /**
     * The number of damage ticks a scorch of {@code durationTicks} deals.
     *
     * One per whole period, which is {@code ceil(duration / period)} -- 120 ticks is 6, not 7. Stated
     * here rather than left implicit in the scheduler because the tick COUNT and the LIFETIME are two
     * numbers and only asserting one of them cannot see an off-by-one-period defect. See
     * {@code ScorchStatusTest}.
     *
     * <p>This said "first one INCLUDED ... one inline on application plus one per whole period after
     * it", and that is no longer how the burn lands: nothing burns on application, and the first tick
     * is the clock's first edge at t=period. THE COUNT IS UNCHANGED by that -- which is exactly why a
     * count cannot pin a lifetime, and why the two are asserted separately.
     *
     * <p>A duration that is not a whole number of periods rounds UP: 50 ticks burns 3 times and lives
     * 60. <b>That rule is what makes a SUB-PERIOD duration ordinary rather than degenerate</b> -- one
     * tick of scorch is its {@code n = 1} instance, burning once and living 20. Measured against the
     * real scheduler at 1, 19, 21, 41 and 50: burns equalled this function at every one. No content
     * authors a duration any more; the dev apply command is the only way to reach one.
     */
    public static int damageTicksFor(int durationTicks) {
        if (durationTicks <= 0) return 0;
        return (durationTicks + PERIOD_TICKS - 1) / PERIOD_TICKS;
    }
}
