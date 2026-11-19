package io.github.butterflysmp.rpg.core.fight;

/**
 * F9b, section 2: WHEN A REFUSED CRYSTAL HEAL IS LOGGED. Ben: "No crystals should not heal the dragon". The heals are
 * refused one by one (vanilla tries one every ten ticks, READ: {@code EnderDragon.checkCrystals}); the line is written
 * once per crystal lit, not once per heal.
 *
 * <p>A crystal is named by how many of the fight's crystals were lit when its heal came. The count only rises within a
 * Mage's cycle, so each rise is a new crystal; when the crystals go out ({@link #crystalsNow} with 0) the next cycle
 * starts again from one.
 */
public final class CrystalHealLog {

    private static final int NONE_LOGGED = -1;

    private int lastLogged = NONE_LOGGED;

    /**
     * A heal was refused while {@code crystalsLit} of the fight's crystals were lit.
     *
     * @return whether to write its line
     */
    public boolean shouldLog(int crystalsLit) {
        if (crystalsLit == lastLogged) return false;
        lastLogged = crystalsLit;
        return true;
    }

    /** The fight's lit-crystal count as of this tick. All of them going out starts the next cycle's lines afresh. */
    public void crystalsNow(int crystalsLit) {
        if (crystalsLit == 0 && lastLogged > 0) lastLogged = NONE_LOGGED;
    }
}
