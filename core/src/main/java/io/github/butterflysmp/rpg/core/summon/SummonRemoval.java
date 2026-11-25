package io.github.butterflysmp.rpg.core.summon;

/**
 * D7, EVERY WAY A SUMMON LEAVES (the seat, 2026-11-24): one value per cause, logged as {@code SUMMON removed cause=<token>}.
 * VOID and KILLED are Q4's.
 */
public enum SummonRemoval {
    /** Its body died (its custom HP reached 0, or vanilla removed it dead). */
    DEATH("death"),
    /** Its owner died. */
    OWNER_DEATH("owner-death"),
    /** Its owner logged out. */
    QUIT("quit"),
    /** Its owner changed world. */
    WORLD_CHANGE("world"),
    /** A recast of the same weapon replaced it. */
    REPLACE("replace"),
    /** The server (the plugin) stopped. */
    STOP("stop"),
    /** The selftest's clear_state. */
    CLEAR("clear"),
    /** A tagged body the store does not own, found at enable or as it was added. */
    ORPHAN("orphan"),
    /** The 20-tick backstop: unowned, or its owner not online in its world. */
    BACKSTOP("backstop"),
    /** It fell into the void (Q4). */
    VOID("void"),
    /** A KILL damage cause, /kill (Q4). */
    KILLED("killed");

    private final String token;

    SummonRemoval(String token) {
        this.token = token;
    }

    public String token() {
        return token;
    }
}
