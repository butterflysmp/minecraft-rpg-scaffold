package io.github.butterflysmp.rpg.core.fight;

/**
 * F9c, section 3 c: THE SELF-CHECK of an END fight world, read a couple of ticks after it is made, before the arena is
 * built and before anyone enters. Is vanilla's own dragon fight inert? From READ facts only (the pinned server jar):
 * <ul>
 *   <li>{@code EnderDragonFight.tick} begins {@code dragonEvent.setVisible(!dragonKilled)}: after a tick, the bar is
 *       hidden exactly when {@code dragonKilled} is true;</li>
 *   <li>Paper's branch in {@code EnderDragonFight.init} (the record fresh, {@code scan-for-legacy-ender-dragon}
 *       false) sets {@code dragonKilled} true and {@code needsStateScanning} false together; with no scan there is no
 *       exit portal built and no dragon adopted, and while {@code dragonKilled} the tick never creates one;</li>
 *   <li>so a hidden bar, no dragon of its own, no exit portal recorded, no bedrock at the centre and no dragon in the
 *       world (ours is spawned after this check) is an inert fight.</li>
 * </ul>
 * If it is not inert, the caller ends that world and makes a NORMAL one, so a fight never runs beside vanilla's.
 */
public final class EndFightCheck {

    private EndFightCheck() {}

    /**
     * What the check read.
     *
     * @param fightPresent      whether the world has vanilla's fight object at all
     * @param barVisible        its boss bar's visibility, a tick or more after creation
     * @param ownDragon         whether it names a dragon of its own
     * @param portalRecorded    whether it records an exit portal
     * @param bedrockAtCentre   bedrock blocks in the centre column (the exit portal is bedrock)
     * @param dragonsInWorld    Ender Dragons in the world before ours is spawned
     */
    public record Reading(boolean fightPresent, boolean barVisible, boolean ownDragon, boolean portalRecorded,
                          int bedrockAtCentre, int dragonsInWorld) {}

    /** Inert: no fight object, or one whose bar is hidden, with no dragon, no portal, no bedrock and no dragon about. */
    public static boolean inert(Reading r) {
        boolean fightQuiet = !r.fightPresent() || (!r.barVisible() && !r.ownDragon() && !r.portalRecorded());
        return fightQuiet && r.bedrockAtCentre() == 0 && r.dragonsInWorld() == 0;
    }

    /** The check's one line. A failed check reads {@code inert=FALSE}, loud. */
    public static String line(String worldName, Reading r) {
        return "FIGHTWORLD end-check name=" + worldName + " inert=" + (inert(r) ? "true" : "FALSE")
                + " fight=" + (r.fightPresent() ? "present" : "absent") + " bar_visible=" + r.barVisible()
                + " fight_dragon=" + (r.ownDragon() ? "PRESENT" : "none")
                + " portal=" + (r.portalRecorded() ? "RECORDED" : "none")
                + " bedrock_at_centre=" + r.bedrockAtCentre() + " dragons=" + r.dragonsInWorld();
    }
}
