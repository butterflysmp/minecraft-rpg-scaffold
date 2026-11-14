package io.github.butterflysmp.rpg.paper.selftest;

import java.util.Optional;

/**
 * Whether {@code /rpg selftest} may run here at all. Pure, so every refusal is a unit row.
 *
 * <p><b>The seat's ruling b, 2026-10-05.</b> The instrument writes the profile (XP), moves the player, changes game
 * rules and fires synthetic events into every listener, so it runs only where all of that is harmless:
 * <ul>
 *   <li><b>a dev server</b>: the JVM property {@code -Drpg.dev=true}, which {@code scripts/dev-server.sh} sets and
 *       nothing else does;</li>
 *   <li><b>the dev world</b>: the server's first world, the one {@code dev-server.sh} boots. A player in the nether
 *       or a second overworld is refused, because every gate's staging assumes the overworld's blanket gear
 *       score;</li>
 *   <li><b>one player online</b>: a second player would be hit by a sweep, see the replies of a shared command, and
 *       could join the test player's region mid-row.</li>
 *   <li><b>SURVIVAL</b> (the seat, 2026-11-06): every gate row is staged and predicted in SURVIVAL, and the game has
 *       game-mode arms (the quiver's ammo debit is one). The 38cc9903 boot's run 1 ran in CREATIVE and certified
 *       nothing, so the instrument now refuses rather than relying on the reader to notice.</li>
 * </ul>
 */
public final class SelfTestGuard {

    /** The JVM property. {@code Boolean.getBoolean} reads it, so only the literal {@code true} counts. */
    public static final String DEV_PROPERTY = "rpg.dev";

    /** {@code GameMode.SURVIVAL.name()}, as a string so this class stays free of Bukkit types and stays a unit row. */
    public static final String SURVIVAL = "SURVIVAL";

    private SelfTestGuard() {}

    /** Empty when the run may start; otherwise the reason it may not, as the reply says it. */
    public static Optional<String> refusal(boolean devServer, boolean inDevWorld, int playersOnline, String gameMode) {
        if (!devServer) {
            return Optional.of("Refusing: not a dev server (-D" + DEV_PROPERTY + "=true is set only by dev-server.sh).");
        }
        if (!inDevWorld) {
            return Optional.of("Refusing: you are not in the dev world (the server's first world).");
        }
        if (playersOnline != 1) {
            return Optional.of("Refusing: " + playersOnline + " players are online; a selftest needs exactly one.");
        }
        if (!SURVIVAL.equals(gameMode)) {
            return Optional.of("Refusing: you are in " + gameMode + "; a selftest runs only in " + SURVIVAL
                    + " (/gamemode survival).");
        }
        return Optional.empty();
    }
}
