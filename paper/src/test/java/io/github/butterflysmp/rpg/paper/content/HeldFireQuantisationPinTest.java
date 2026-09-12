package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.ability.AttackSpeed;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import io.github.butterflysmp.rpg.core.weapon.WeaponRegistry;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * THE EXECUTABLE PIN FOR THE HELD-FIRE QUANTISATION MODEL, AND FOR THE THREE READINGS IT RESTS ON.
 *
 * <h2>THIS IS A PIN. IT IS NOT THE ACCOUNT, AND IT DELIBERATELY DOES NOT RE-EXPLAIN THE MODEL</h2>
 *
 * <p>One rule, three homes, each saying which it is — {@code CLAUDE.md}'s rule on two homes:
 *
 * <ul>
 *   <li><b>POINTER</b> — {@code CLAUDE.md}: <i>author multiples of 4; multiples of 8 if it may ever
 *       be dual-wielded.</i> The form that changes what you do.</li>
 *   <li><b>ACCOUNT</b> — {@code WeaponLoader}'s {@code cooldown_ticks} section: the mechanism, the
 *       measurements, the tooltip consequence. <b>Do not duplicate it here.</b></li>
 *   <li><b>PIN</b> — this file: the numbers, executable, in a place a citation sweep will not
 *       edit.</li>
 * </ul>
 *
 * <h2>WHY A TEST AND NOT A SECOND PARAGRAPH</h2>
 *
 * <p><b>The account lives in {@code WeaponLoader.java}, which is in the java-mention list for all
 * three weapons in the deletion set.</b> So the sole authoritative account of this model sits in a
 * file guaranteed to be opened during a deletion sweep, by someone whose stated intent is <i>remove
 * references to weapons that no longer exist</i> — and the model's three measurements are STATED ON
 * those weapons. A sweep meets, in one file, prose that must be PRESERVED tangled with prose that
 * must be REMOVED, distinguishable only by reading carefully at exactly the moment the work is
 * mechanical.
 *
 * <p><b>A javadoc is edited by that sweep and never fails. A test is edited by neither, and it fails
 * when the model stops being true.</b> That is the whole reason this file exists.
 *
 * <h2>WHAT THIS ACTUALLY GUARDS, STATED HONESTLY, BECAUSE A PIN CAN BE A TAUTOLOGY</h2>
 *
 * <p><b>It does NOT guard the rounding in {@code effectiveCooldownTicks}</b> — {@code AttackSpeedTest
 * .roundsToTheNearestTickRatherThanTruncating} already pins that at {@code 15 / 2.0 = 7.5 -> 8}, and
 * claiming it twice would be the duplicate-account failure one level down.
 *
 * <p><b>It does NOT prove the 4-tick input floor.</b> That is a fact about the vanilla client,
 * measured at a booted server ({@code GATE-q7.md}) and unreachable from a unit test. {@link
 * #INPUT_FLOOR_TICKS} is a MEASURED CONSTANT transcribed here, not a derived one.
 *
 * <p><b>What it does guard is the thing nothing else does:</b>
 *
 * <ol>
 *   <li><b>THE COMPOSITION.</b> {@code ceil(effectiveCooldownTicks(authored, speed) / 4) * 4} is
 *       asserted nowhere else in the project. The two halves are each tested; their product — which
 *       is what a player actually experiences — was prose only.</li>
 *   <li><b>THE FIXTURES STILL CARRYING THE COOLDOWNS THE READINGS WERE TAKEN AT.</b> Edit
 *       {@code quiver_stone} to 12 and the recorded {@code 11 -> 12} reading silently stops
 *       describing the shipped weapon. Nothing else in the suite notices.</li>
 * </ol>
 *
 * <h2>IT WILL GO RED ON DELETION DAY, AND THAT IS THE POINT RATHER THAN A DEFECT</h2>
 *
 * <p>Two of the three weapons below are in the deletion set. When they go, this test fails — loudly,
 * with the instruction in its own failure message. <b>It is deliberately impossible to delete those
 * weapons without being told that two of three measurements just became unverifiable.</b> The
 * remedy on that day is in the failure message and in {@code NEXT.md}: restate the reading, note the
 * fixture is gone, then drop the row from this pin.
 */
class HeldFireQuantisationPinTest {

    private static final Logger LOG = Logger.getLogger(HeldFireQuantisationPinTest.class.getName());

    /**
     * MEASURED, NOT DERIVED. A held right-click delivers an input every 4 ticks — {@code GATE-q7.md},
     * two weapons, two materials, minimum 4 on both. A unit test cannot observe this; the constant is
     * transcribed from the reading and this sentence is the provenance.
     */
    private static final int INPUT_FLOOR_TICKS = 4;

    /**
     * One reading, as taken. {@code authored} is checked against the live content file, so this record
     * cannot quietly stop describing the weapon it names.
     *
     * @param weaponId        the fixture it was taken on
     * @param authored        {@code cooldown_ticks} at the time of the reading
     * @param measuredMin     the {@code FIRES min} the readout printed
     * @param readout         where the verbatim readout lives
     * @param inDeletionSet   true if this fixture is scheduled for deletion (ruled 2026-09-12)
     */
    private record Reading(String weaponId, int authored, int measuredMin, String readout,
                           boolean inDeletionSet) {}

    /** The three points the model rests on. Restated in {@code NEXT.md} in prose; pinned here. */
    private static final List<Reading> READINGS = List.of(
            new Reading("quiver_stone", 11, 12, "GATE-q7.md", true),
            new Reading("hunters_bow", 15, 16, "GATE-q7.md", true),
            new Reading("boltor", 16, 16, "GATE-boltor.md row 1", false));

    /** The model. One expression, so it cannot drift from the sentence that states it. */
    private static int deliveredIntervalTicks(int authoredCooldown, double attackSpeed) {
        int effective = AttackSpeed.effectiveCooldownTicks(authoredCooldown, attackSpeed);
        return ((effective + INPUT_FLOOR_TICKS - 1) / INPUT_FLOOR_TICKS) * INPUT_FLOOR_TICKS;
    }

    private static WeaponRegistry shipped() {
        return new WeaponLoader(LOG).loadAll(new File("src/main/resources/content/weapons"));
    }

    /**
     * THE MODEL REPRODUCES ALL THREE MEASURED POINTS.
     *
     * <p>{@code 11 -> 12}, {@code 15 -> 16}, {@code 16 -> 16}. The third is the only one that
     * distinguishes {@code >=} from {@code >} in {@code CooldownTracker.isReady}: under {@code >} it
     * would have read 20. It is also the only one consistent with <i>"the interval is just the
     * authored cooldown"</i>, which the other two refute — <b>so all three rows are load-bearing and
     * none is redundant.</b>
     */
    @Test
    void theModelReproducesEveryMeasuredPoint() {
        for (Reading r : READINGS) {
            assertEquals(r.measuredMin(), deliveredIntervalTicks(r.authored(), AttackSpeed.BASE),
                    () -> "the model must reproduce the reading taken on " + r.weaponId()
                            + " (" + r.readout() + "): authored " + r.authored()
                            + " measured min " + r.measuredMin());
        }
    }

    /**
     * EVERY PINNED FIXTURE STILL CARRIES THE COOLDOWN ITS READING WAS TAKEN AT.
     *
     * <p><b>This is the row with teeth, and it is the one nothing else in the suite covers.</b> A
     * reading is a fact about a weapon as it was authored. Re-author {@code quiver_stone} to 12 and
     * the recorded {@code 11 -> 12} still <i>reads</i> correctly while describing a weapon that no
     * longer exists in that form — a preserved measurement and a stale one being indistinguishable is
     * precisely the failure {@code CLAUDE.md} names.
     *
     * <p><b>It fails on deletion day by design</b>, and the message says what to do.
     */
    @Test
    void everyPinnedFixtureStillCarriesTheCooldownItsReadingWasTakenAt() {
        WeaponRegistry weapons = shipped();
        assertTrue(weapons.size() > 0,
                "discovered nothing: a zero-weapon load is a defect, not a quiet pass");

        for (Reading r : READINGS) {
            Optional<WeaponDefinition> found = weapons.find(r.weaponId());
            if (found.isEmpty()) {
                fail("PINNED FIXTURE GONE: '" + r.weaponId() + "' is no longer in content/weapons, "
                        + "and it carries the measured point " + r.authored() + " -> " + r.measuredMin()
                        + " (" + r.readout() + "). This test is the thing that stops that measurement "
                        + "being lost silently. BEFORE removing this row: restate the reading in "
                        + "NEXT.md with the note that its fixture was deleted and the date. Then drop "
                        + "the row. Do NOT simply delete the row -- that is the failure the pin exists "
                        + "to prevent."
                        + (r.inDeletionSet() ? " (This weapon was known to be in the deletion set.)" : ""));
            }
            assertEquals(r.authored(), triggerCooldown(found.orElseThrow()),
                    () -> "'" + r.weaponId() + "' was read at cooldown_ticks " + r.authored()
                            + "; the shipped file now says otherwise, so the recorded reading "
                            + r.authored() + " -> " + r.measuredMin() + " (" + r.readout() + ") no "
                            + "longer describes this weapon. Re-take the reading or correct the pin.");
        }
    }

    /**
     * THE DEAD ZONE IS A COMPOSITION FACT AND IT IS ASSERTED NOWHERE ELSE.
     *
     * <p>On the Boltor's 16, {@code round(16 / s)} must reach 12 before the delivered interval moves
     * at all, and {@code 16 / 12.5 = 1.28}. <b>So every attack-speed bonus up to +28% buys exactly
     * nothing, and the first one that does anything buys 33% at once.</b> Neither half of that is
     * visible from {@code effectiveCooldownTicks} alone — 1.1x, 1.2x and 1.28x all produce DIFFERENT
     * effective cooldowns (15, 13, 13) and the SAME delivered interval.
     *
     * <p>Recorded as an open balance finding in {@code boltor.yml}; pinned here so the arithmetic
     * behind it cannot drift unnoticed. <b>It is Ben's to rule and this test asserts no opinion about
     * whether the dead zone is acceptable</b> — only that it is where it is measured to be.
     */
    @Test
    void theAttackSpeedDeadZoneOnTheBoltorRunsToOnePointTwoEight() {
        assertEquals(16, deliveredIntervalTicks(16, 1.00), "no bonus");
        assertEquals(16, deliveredIntervalTicks(16, 1.20), "+20% buys nothing");
        assertEquals(16, deliveredIntervalTicks(16, 1.28), "+28% still buys nothing -- the boundary");
        assertEquals(12, deliveredIntervalTicks(16, 1.29), "+29% moves it, and moves it by 33%");
        assertEquals(8, deliveredIntervalTicks(16, 1.89), "16/8.5 = 1.8824, so 1.89 is the next step");
    }

    private static int triggerCooldown(WeaponDefinition weapon) {
        return weapon.triggers().stream()
                .filter(b -> b.ability().cooldownTicks() > 0)
                .map(b -> b.ability().cooldownTicks())
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "weapon '" + weapon.id() + "' has no trigger with a positive cooldown_ticks, "
                                + "so the pinned reading cannot be checked against it"));
    }
}
