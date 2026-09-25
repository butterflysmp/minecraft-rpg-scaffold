package io.github.butterflysmp.rpg.core.weapon;

import java.util.Arrays;
import java.util.OptionalInt;

/**
 * An item's power rating, and everything the rating decides. 100 is the game as it plays today.
 *
 * <p>Pure, zero dependencies, and the whole of the arithmetic. The impure half -- reading a slot off
 * a {@code PlayerInventory}, reading the stamp off a PDC -- lives in {@code paper/}, which gathers
 * PLAIN NUMBERS and hands them here. Same split as {@code CombatantSnapshot} and {@code VaultCodec};
 * {@code PlayerLevel} is the closest sibling in shape.
 *
 * <h2>*** GS 100 IS EXACTLY THE GAME AS IT PLAYS TODAY. THAT IS WHAT MAKES THIS SHIPPABLE. ***</h2>
 *
 * {@link #scaledDamage} is {@code authored * score / 100}, so <b>every number in every content file
 * keeps its current meaning</b> and nothing needs re-tuning. GS 500 is 5x. An item with no stamp
 * reads {@link #ABSENT}, which is 100, so <b>gear minted before this slice behaves exactly as it did
 * before it</b> -- no stamp, no migration, no schema bump. That is {@code lifetimeXp}'s case:
 * absence has a CORRECT reading rather than a missing one.
 *
 * <h2>*** THREE DIFFERENT QUANTITIES ARE ALL 100 TODAY, AND NO TEST CAN TELL THEM APART ***</h2>
 *
 * {@link #BASELINE}, {@link #MIN} and {@link #ABSENT} are numerically equal and mean three unrelated
 * things -- <b>the divisor</b>, <b>the floor</b>, and <b>what an unstamped item reads as</b>. Swap
 * any two in the code and every row in the suite stays green, because both readings are the same
 * number. This is CLAUDE.md's collision rule arriving in the constants rather than in a fixture:
 * <i>"when two independent quantities in a row are equal, at least one of them is not being
 * tested."</i>
 *
 * <p><b>Naming all three IS the protection, and it is the only one available.</b> There is no
 * staging that separates them while they are equal, so a reader who finds {@code / 100} written as
 * {@code / MIN} has no test that will ever object. Ben moves any one of the three and the other two
 * must not move with it.
 *
 * <h2>WHAT SCALES: WEAPON DAMAGE AND ARMOUR DEFENSE. NOTHING ELSE.</h2>
 *
 * Operator's ruling. {@code attackSpeed}, sweep, {@code quiverSize}, {@code reloadTicks}, crit, mana
 * and both regens stay AUTHORED AND UNTOUCHED -- <b>those are feel, not power</b>. Two named doors
 * rather than one {@code scaled()} for that reason: each names what must not come through it, and
 * the arithmetic is written once behind them so the two cannot drift.
 *
 * <p><b>RARITY DOES NOT ENTER ANY OF THIS.</b> Rarity and power are independent axes -- an EXOTIC can
 * roll low and a COMMON can roll high -- and rarity keeps meaning exactly what it means today:
 * colour, and the enchant candidate pool. There is deliberately no {@code Rarity} parameter anywhere
 * in this class, and adding one would be the change that has to be argued for.
 *
 * <h2>*** UNTIL MOBS SCALE, THE SERVER GETS EASIER -- AND {@link #MIN} IS WHY THAT IS SURVIVABLE ***</h2>
 *
 * Scaling ships now against unscaled mobs. Ben ruled it: nobody plays on it yet. <b>The floor is
 * what bounds the damage at 5x rather than 500x</b> -- without a minimum, a score could be any
 * fraction of the baseline and the arithmetic has no lower stop at all. So {@link #MIN} is not a
 * tuning number and <b>must not be casually lowered</b>; it is the bound that makes an unscaled-mob
 * world playable, and lowering it re-opens a hole nothing else in this class closes.
 *
 * <h2>A NEW PLAYER HOLDING ONE SWORD AVERAGES 16, AND THAT IS THE INTENDED PRESSURE</h2>
 *
 * {@code 100 / 6 = 16} -- below the minimum any item can roll, so <b>the floor does the early work
 * and drops sit at 100 until slots fill broadly</b>. Ben has seen this and accepted it: it is the
 * pressure to gear every slot, not a defect. Asserted as a row in {@code GearScoreTest} so nobody
 * "fixes" it into a threshold rule.
 *
 * @see GearScoreBand for the two RULED band numbers -- the band is average-5 to average+15
 */
public final class GearScore {

    private GearScore() {}

    /**
     * The divisor. <b>An item at {@code BASELINE} scales nothing</b>, which is the property that lets
     * this ship without re-tuning a single content file.
     *
     * <p>Equal to {@link #MIN} and {@link #ABSENT} today and <b>not the same quantity as either</b> --
     * see the class javadoc on why that collision is undetectable by any test. Written as
     * {@code BASELINE} at every division site on purpose.
     */
    public static final int BASELINE = 100;

    /** There is no score below this. See the class javadoc: it is the bound, not a tuning number. */
    public static final int MIN = 100;

    /**
     * Drops clamp here. {@code 401..}{@link #HARD_CAP} is ACTIVITY-BASED and <b>does not exist
     * yet</b> -- no path in this build can produce a score above this, and {@link #roll} enforces it.
     */
    public static final int SOFT_CAP = 400;

    /**
     * The ceiling for any score from any source, including a hand-edited PDC. Above the soft cap only
     * because the activity tier will eventually reach here; nothing today does.
     */
    public static final int HARD_CAP = 500;

    /**
     * What an item carrying no stamp reads as.
     *
     * <p><b>ABSENCE HAS A CORRECT READING, which is why there is no migration.</b> Unlike
     * {@code Keys.quiverLoaded} -- where an unstamped weapon and an empty one must never resolve
     * alike -- an unstamped item genuinely IS a baseline item, so the default is the answer rather
     * than a mask over a failed mint. Same call, same reasoning, as {@code lifetimeXp}.
     */
    public static final int ABSENT = 100;

    /** Four armour slots, plus the two highest-scored hands. The average's denominator. */
    public static final int AVERAGE_SLOTS = 6;

    /** Helmet, chest, legs, feet. */
    public static final int ARMOR_SLOTS = 4;

    /**
     * How many of the hand pool reach the average.
     *
     * <p><b>TWO, taken across HOTBAR AND OFFHAND TOGETHER</b> -- the offhand is in that pool, not a
     * slot of its own. Ben's ruling, and the reason {@link #topTwo} takes one array rather than a
     * hotbar array and an offhand value.
     */
    public static final int HAND_SLOTS = 2;

    /** An empty slot's contribution. ALWAYS counted, never skipped -- see {@link #averageOf}. */
    public static final int EMPTY = 0;

    // ---------------------------------------------------------------- clamping

    /**
     * Any score, brought inside {@code [}{@link #MIN}{@code , }{@link #HARD_CAP}{@code ]}.
     *
     * <p>The READ clamp: this is what a value off an item's PDC goes through, where it may have been
     * hand-edited to anything at all. Never throws, for {@code PlayerLevel.levelFor}'s reason -- a
     * number off disk is bad data, not a programming error, and a tooltip is not a place to take out
     * a join handler.
     */
    public static int clamp(int score) {
        return Math.min(Math.max(score, MIN), HARD_CAP);
    }

    /**
     * A DROP's score, brought inside {@code [}{@link #MIN}{@code , }{@link #SOFT_CAP}{@code ]}.
     *
     * <p>Distinct from {@link #clamp} because the two ceilings are two different rulings: the hard cap
     * is where the ladder ends, the soft cap is as high as a DROP can roll. Collapsing them would let
     * a drop reach the activity tier, which does not exist yet.
     */
    public static int clampDrop(int score) {
        return Math.min(Math.max(score, MIN), SOFT_CAP);
    }

    /**
     * A stamped score, or {@link #ABSENT} when the item carries none.
     *
     * <p>An {@code OptionalInt} in rather than a sentinel: the paper side reads a PDC that either has
     * an integer or does not, and <b>a sentinel that is a valid score is a defect waiting for the one
     * caller who forgets</b> -- {@code PlayerLevel.xpToNextLevel} records the same argument, and
     * {@code SettingsMenuLayout.chooserFor} refused a {@code -1} for it.
     *
     * <p>A present value still goes through {@link #clamp}, because present does not mean sane.
     */
    public static int orAbsent(OptionalInt stamped) {
        return stamped.isPresent() ? clamp(stamped.getAsInt()) : ABSENT;
    }

    // ---------------------------------------------------------------- the scale

    /**
     * A weapon's authored {@code attack_damage}, scaled by the score of the item carrying it.
     *
     * <p><b>ONLY the authored figure comes through here.</b> Enchant damage is a different source on
     * a different reconcile call ({@code CombatantStats.reconcileEnchantDamageModifiers}, fed from
     * {@code DamageEnchantItems}), so gear score does not multiply an enchant, and the armour side
     * below is scaled before its Protection composes for exactly the same symmetry.
     *
     * <p><b>Not attack SPEED, not crit, not sweep share.</b> Those are feel; this is power.
     */
    public static double scaledDamage(double authoredAttackDamage, int score) {
        return scaled(authoredAttackDamage, score);
    }

    /**
     * An armour piece's own vanilla armour points, scaled by that piece's score.
     *
     * <p><b>THE MATERIAL'S POINTS, NEVER THE PIECE'S TOTAL.</b> {@code Protection.effectiveDefense}
     * adds an enchant's bonus on top, and that bonus must arrive unscaled -- the weapon side above
     * scales the authored figure and not the enchant, and these two are the same ruling seen from two
     * ends.
     *
     * <p><b>AND NEVER THE NATIVE SUM.</b> {@code DefenseModifierItems.Worn} carries a second number
     * -- what the vanilla {@code armor} attribute actually HOLDS -- which {@code ArmorBarOverride}
     * cancels before refilling the bar from damage reduction. Scaling that would make the
     * cancellation over-subtract, the attribute land negative, and Minecraft clamp it to zero: <b>the
     * bar reads EMPTY on the most-armoured player in the game</b> while the stat, the mitigation and
     * the tooltip all stay correct. Nothing throws and no unit test can see it. That exact failure is
     * already recorded in {@code DefenseModifierItems}' own javadoc, for the enchant that caused it
     * the first time.
     */
    public static double scaledDefense(double authoredArmorPoints, int score) {
        return scaled(authoredArmorPoints, score);
    }

    /**
     * {@code authored * score / }{@link #BASELINE}. Written once so the two doors above cannot drift
     * -- {@code ResourceCost} states the rule: <i>"two literals cannot drift apart if there is only
     * one."</i>
     *
     * <p>The score is clamped on the way in, so a hand-edited PDC can neither zero a weapon nor take
     * it past the ceiling.
     */
    private static double scaled(double authored, int score) {
        return authored * clamp(score) / BASELINE;
    }

    // ---------------------------------------------------------------- what carries a score

    /**
     * May an item of this kind carry a score at all?
     *
     * <h2>*** TOOLS DO NOT CARRY A SCORE ***</h2>
     *
     * Ben's ruling, and the reason is specific rather than tidiness: <b>a pickaxe must never become
     * one of the top two and raise a drop level without contributing anything to a fight.</b> A tool
     * in the hotbar is the ordinary case, not an edge one.
     *
     * <p><b>An exhaustive switch with NO default arm</b>, so a fifth {@link GearClass} stops the
     * build here until someone says whether it scores -- the same discipline {@code GearItems.remint}
     * and {@code GearClass.of} keep, and for the same reason: a new kind that "just worked" by
     * falling through a catch-all would be scored, or not, by accident.
     *
     * <p>The three fighting classes are named individually rather than collapsed into a default,
     * which is what makes the mutation that admits {@link GearClass#TOOL} a one-line change this
     * class's own rows can see. Before this lived in core, the only witness was a boot.
     *
     * <p>A {@code null} kind -- an item that is none of ours -- scores nothing. Same
     * null-means-no-gate convention {@code GearClass.of} and {@code DamageEnchants.matching} share.
     */
    static boolean scoreable(GearClass kind) {
        if (kind == null) return false;
        return switch (kind) {
            case MELEE, RANGER, MAGE -> true;
            case SHIELD -> true;
            case ARMOR -> true;
            case TOOL -> false;
            // Ruling A4: v1 accessories take no part in gear score.
            case ACCESSORY -> false;
        };
    }

    /**
     * *** THE ONE DOOR. Does THIS item carry a score -- kind AND instance together? ***
     *
     * <h2>TWO INDEPENDENT REFUSALS, AND ASKING ONLY ONE OF THEM IS THE DEFECT</h2>
     *
     * <p>{@link #scoreable} is the KIND-level rule: a pickaxe is not gear, whatever its content file
     * says. {@code declaredUnscored} is the INSTANCE-level exception: {@code volley_stone} is a dev
     * fixture and carries no score although its class is a fighting one. <b>They are different
     * questions with different authors, and every caller must ask BOTH.</b>
     *
     * <p><b>THE FAILURE THIS EXISTS TO PREVENT IS A SILENT COLLAPSE FROM TWO REFUSALS TO ONE.</b> The
     * READ path and the STAMP path refuse independently today. If the stamp keeps asking
     * {@code scoreable(kind)} alone, a freshly minted {@code volley_stone} still gets a score written
     * into its PDC and only the read refuses -- so the exclusion still LOOKS to work, the average is
     * still correct, and the stamp half is gone with nothing red. The number in the PDC is the only
     * witness, and no unit test can construct an {@code ItemStack} to see it.
     *
     * <p><b>A definition that cannot be found means SCORED, not unscored.</b> A weapon whose content
     * file was removed or renamed must keep the score already in its PDC: only an explicit
     * declaration removes anything. The opposite default is silent -- the item leaves the average, the
     * drop band falls, and nothing reports it. Callers resolve that to {@code false} at the lookup,
     * and the ruling is written at the lookup site rather than assumed here.
     *
     * <h2>*** WHY THIS TAKES A BARE BOOLEAN AND NOT A {@link GearDefinition}. DO NOT "SIMPLIFY" IT
     * WITHOUT READING THIS. ***</h2>
     *
     * <p><b>A definition would be strictly better and is not available.</b> Both call sites now hold
     * one, so the obvious improvement is to take the definition and derive both facts here --
     * deleting the parameter that reads wrong when defaulted, rather than merely warning about it.
     *
     * <p><b>It is blocked on ONE missing accessor:</b> {@code GearDefinition} declares
     * {@code id, displayName, rarity, material, flavor, craftResult} and <b>no gear kind at all</b>.
     * The mapping is {@code GearItems.gearClassOf}, which lives in {@code paper} -- and it is a PURE
     * switch over this very sealed hierarchy, with no Bukkit in it, so <b>it could move here</b>.
     *
     * <p><b>What stops that being this slice's job:</b> measured at 12b, it has FIVE production
     * callers -- mostly {@code EnchantRollItems.rollOnAcquire} and {@code EnchantMenu} -- and its own
     * {@code GearClassOfTest}. Moving it is an enchant-subsystem refactor with its own review, not a
     * tidy-up inside a damage-scaling slice.
     *
     * <p><b>So the condition for deleting this parameter is exact:</b> when the kind mapping reaches
     * core, change this to {@code carriesScore(GearDefinition)} and the hazard disappears instead of
     * being documented. Until then the boolean is load-bearing, and
     * {@code GearScoreWiringSignatureTest} bans a literal at the one call site that could default it.
     *
     * @param kind             the gear kind, or {@code null} for an item that is none of ours
     * @param declaredUnscored whether the item's own DEFINITION declares it carries no score
     */
    public static boolean carriesScore(GearClass kind, boolean declaredUnscored) {
        return scoreable(kind) && !declaredUnscored;
    }

    // ---------------------------------------------------------------- the average

    /**
     * The two highest scores in the hand pool, highest first, padded with {@link #EMPTY}.
     *
     * <p><b>THE SELECTION IS ARITHMETIC AND THEREFORE LIVES HERE. {@code paper/} MUST NOT CONTAIN A
     * {@code max()}.</b> The paper side walks the hotbar and the offhand, drops anything
     * {@link #scoreable} refuses, and hands the remainder over as plain numbers; every decision about
     * WHICH of them counts is made in this method, where a unit test reaches it at the two-second
     * loop instead of costing a boot.
     *
     * <p>Always returns {@link #HAND_SLOTS} entries. A pool with one entry pads the second with
     * {@link #EMPTY}, and an EMPTY pool returns two of them -- which is the new player holding
     * nothing, and it must average to something rather than dividing by a smaller denominator. See
     * {@link #averageOf}.
     *
     * <p>Copies before sorting: the caller's array is its own, and a scan that silently reorders its
     * input is the kind of thing a later caller reuses the array after.
     */
    public static int[] topTwo(int[] handPool) {
        int[] padded = new int[HAND_SLOTS];
        if (handPool != null && handPool.length > 0) {
            int[] sorted = handPool.clone();
            Arrays.sort(sorted);
            // Ascending, so the top two are the tail. Read back-to-front for highest-first.
            for (int i = 0; i < HAND_SLOTS && i < sorted.length; i++) {
                padded[i] = sorted[sorted.length - 1 - i];
            }
        }
        return padded;
    }

    /**
     * The six numbers the average is taken over: four armour slots, then the two highest hands.
     *
     * <p>The assembly is here rather than at the call site so {@code paper/} hands over two lists and
     * does no arithmetic -- including no concatenation, which is where an off-by-one denominator
     * would live. {@link #averageOf} then has exactly one shape to accept.
     *
     * @param armorSlots exactly {@link #ARMOR_SLOTS} entries, {@link #EMPTY} for a bare slot. Refused
     *                   loudly at the wrong length: a five-element armour array would average over
     *                   seven slots and read as a slightly generous ladder forever.
     * @param handPool   every scoreable item in the hotbar and the offhand, any length including zero
     */
    public static int[] sixSlots(int[] armorSlots, int[] handPool) {
        if (armorSlots == null || armorSlots.length != ARMOR_SLOTS) {
            throw new IllegalArgumentException("armour is " + ARMOR_SLOTS + " slots, got "
                    + (armorSlots == null ? "null" : String.valueOf(armorSlots.length)));
        }
        int[] six = new int[AVERAGE_SLOTS];
        System.arraycopy(armorSlots, 0, six, 0, ARMOR_SLOTS);
        System.arraycopy(topTwo(handPool), 0, six, ARMOR_SLOTS, HAND_SLOTS);
        return six;
    }

    /**
     * The player's average gear score: the six slots, summed and divided by six.
     *
     * <h2>*** AN EMPTY SLOT COUNTS 0, ALWAYS. THERE IS NO THRESHOLD RULE. ***</h2>
     *
     * Ben's ruling, and the denominator is the whole of it: skipping empties would divide by the
     * number of FILLED slots, so a new player holding one 100 sword would average <b>100</b> instead
     * of <b>16</b> and their first drop would roll at the top of the early band. <b>The pressure to
     * gear every slot is this division and nothing else.</b>
     *
     * <p>Floors, via integer division -- {@code 100 / 6 = 16}, not 16.67 and not 17. The value is
     * rendered as a whole number and bands a roll; a fractional average would have to be rounded
     * somewhere, and somewhere is here.
     *
     * <p>Each slot is clamped to {@code [}{@link #EMPTY}{@code , }{@link #HARD_CAP}{@code ]} rather
     * than through {@link #clamp}, because <b>{@link #EMPTY} is below {@link #MIN} and must stay
     * there</b>: putting an empty slot through the read clamp would raise it to 100 and silently
     * delete the ruling above. A negative or absurd stamp off a hand-edited PDC is still refused.
     *
     * <h2>*** THE UNFLOORED AVERAGE IS RULED. A FLOOR WAS PROPOSED AND BEN OVERRULED IT. ***</h2>
     *
     * <p>The proposal was {@code average = max(MIN, sum / AVERAGE_SLOTS)}, on the argument that <b>the
     * average is itself a gear score and 16 is outside its own scale</b> -- which is a real
     * inconsistency, not a misreading. <b>Ben ruled: LEAVE IT. THE DEAD ZONE IS THE EARLY GAME.</b>
     *
     * <p>So the consequence is intended, and it is larger than the one number the class javadoc
     * quotes. With every filled slot at {@link #MIN}, the WHOLE ruled band lies below the floor for
     * one through FIVE filled slots, so <b>every drop lands at exactly 100 until the sixth slot
     * fills</b>:
     *
     * <pre>
     *   filled   average   band top (avg + 15)   every drop 100?
     *     1         16              31                  yes
     *     5         83              98                  yes   &lt;- FALLS two SHORT of MIN
     *     6        100             115                  no
     * </pre>
     *
     * <p><b>MEASURED, not derived -- AND THE MEASUREMENT IS DELIBERATELY NOT RESTATED HERE.</b>
     * {@code GearScoreTest.everyDropLandsAtTheFloorUntilTheSixthSlotFills} sweeps all six and is the
     * instrument. It answers <i>how far each of the skew, the spread and this denominator must move
     * before the opening phase ends at five slots instead of six</i> -- and the three answers are not
     * the same, which is precisely what a sentence here would get wrong.
     *
     * <p><b>This is a POINTER, and it states no margin on purpose.</b> A margin written in prose is a
     * figure nothing can falsify; in the sweep it executes and reddens when a constant moves. <b>This
     * very paragraph carried such a figure, wrong in BOTH halves, for two days, with a green suite
     * the whole time.</b> If it reads as thin, that is the point -- read the sweep rather than
     * helpfully inlining the numbers back.
     *
     * <p><b>This note exists because an unfloored average with no note reads as an oversight, and the
     * next person will "fix" it.</b> It is a ruling, and the row above is what makes the ruling fail
     * loudly if anyone floors it.
     *
     * @param sixSlots exactly {@link #AVERAGE_SLOTS} entries, as {@link #sixSlots} builds them
     */
    public static int averageOf(int[] sixSlots) {
        if (sixSlots == null || sixSlots.length != AVERAGE_SLOTS) {
            throw new IllegalArgumentException("the average is over " + AVERAGE_SLOTS + " slots, got "
                    + (sixSlots == null ? "null" : String.valueOf(sixSlots.length)));
        }
        int sum = 0;
        for (int slot : sixSlots) {
            sum += Math.min(Math.max(slot, EMPTY), HARD_CAP);
        }
        return sum / AVERAGE_SLOTS;
    }

    // ---------------------------------------------------------------- the roll

    /**
     * The score a freshly acquired item rolls, banded on the player's CURRENT AVERAGE.
     *
     * <p>Uniform over the whole band, inclusive at both ends, then clamped to
     * {@link #clampDrop}'s range. <b>A drop can never exceed {@link #SOFT_CAP}</b> however high the
     * average or the skew, and never fall below {@link #MIN} however low the average -- which is what
     * makes the 16-average new player's first drop land at 100.
     *
     * <p><b>The band's two numbers are NOT defaulted here.</b> They are parameters, so this
     * arithmetic is reddened at any spread a test cares to stage, and the only place a value is
     * written down is {@link GearScoreBand} -- RULED by Ben as average-5 to average+15, which that
     * class stores as a spread of 10 and a skew of 5.
     *
     * @param average the player's current average, from {@link #averageOf}
     * @param spread  half-width. {@code 0} is a degenerate band: every roll lands on the centre
     * @param skew    shifts the centre off the average. {@code 0} centres it, which CONVERGES rather
     *                than climbs -- see {@link GearScoreBand}
     * @param draw    a uniform draw in {@code [0, 1)}, as {@code ThreadLocalRandom.nextDouble}
     *                produces it. Refused outside that range: a caller passing 1.0 would roll one
     *                past the top of the band, which the clamp hides at the cap and not below it
     */
    public static int roll(int average, int spread, int skew, double draw) {
        if (spread < 0) {
            throw new IllegalArgumentException("a band's spread cannot be negative: " + spread);
        }
        // NaN-safe by construction: every comparison against NaN is false, so this catches it without
        // a separate isNaN test. Same idiom as DamageScale.toCustom's guards.
        if (!(draw >= 0.0 && draw < 1.0)) {
            throw new IllegalArgumentException("a draw is in [0, 1), got " + draw);
        }

        int width = 2 * spread + 1;
        int offset = (int) Math.floor(draw * width);
        return clampDrop(average + skew - spread + offset);
    }
}
