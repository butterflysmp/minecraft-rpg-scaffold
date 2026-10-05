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
 * </ul>
 */
public final class SelfTestGuard {

    /** The JVM property. {@code Boolean.getBoolean} reads it, so only the literal {@code true} counts. */
    public static final String DEV_PROPERTY = "rpg.dev";

    private SelfTestGuard() {}

    /** Empty when the run may start; otherwise the reason it may not, as the reply says it. */
    public static Optional<String> refusal(boolean devServer, boolean inDevWorld, int playersOnline) {
        if (!devServer) {
            return Optional.of("Refusing: not a dev server (-D" + DEV_PROPERTY + "=true is set only by dev-server.sh).");
        }
        if (!inDevWorld) {
            return Optional.of("Refusing: you are not in the dev world (the server's first world).");
        }
        if (playersOnline != 1) {
            return Optional.of("Refusing: " + playersOnline + " players are online; a selftest needs exactly one.");
        }
        return Optional.empty();
    }
}
