package io.github.butterflysmp.rpg.paper.command;

/**
 * Every permission node, in one place. Declared in paper-plugin.yml with its
 * default; a node used here but not declared there silently defaults to op-only.
 */
public final class Permissions {

    private Permissions() {}

    /** Cast an ability. Granted to everyone by default -- it is the game. */
    public static final String CAST = "rpg.command.cast";

    /** Read your own stat sheet. Granted to everyone by default -- knowing your build is the game too. */
    public static final String STATS = "rpg.command.stats";

    /** Inspect loaded content. Operators only; it exposes the content pipeline. */
    public static final String ADMIN = "rpg.command.admin";

    /** Mint a weapon into your inventory. Operators only; it spawns items from nothing. */
    public static final String GIVE = "rpg.command.give";

    /** Apply any status to a mob for testing. Operators only; it mutates the world, bypassing
        the class/element gate -- a dev instrument, not a game feature. */
    public static final String DEV = "rpg.command.dev";

    /**
     * Open the Nexus hub with {@code /menu}. <b>Granted to everyone by default -- it is the door to
     * the game.</b>
     *
     * <p><b>DECLARED RATHER THAN OMITTED, AND THAT IS THE POINT OF HAVING A NODE AT ALL.</b> The
     * command is not op-gated, so {@code requires()} could simply have been left off. A declared
     * {@code default: true} node behaves identically for every player <b>and can be revoked</b>; an
     * absent one cannot be taken away, only worked around.
     */
    public static final String MENU = "rpg.command.menu";
}
