package io.github.butterflysmp.rpg.paper.command;

/**
 * Every permission node, in one place. Declared in paper-plugin.yml with its
 * default; a node used here but not declared there silently defaults to op-only.
 */
public final class Permissions {

    private Permissions() {}

    /** Cast an ability. Granted to everyone by default -- it is the game. */
    public static final String CAST = "rpg.command.cast";

    /** Pick a class. Granted to everyone by default -- choosing one is the game too. */
    public static final String CLASS = "rpg.command.class";

    /** Inspect loaded content. Operators only; it exposes the content pipeline. */
    public static final String ADMIN = "rpg.command.admin";

    /** Mint a weapon into your inventory. Operators only; it spawns items from nothing. */
    public static final String GIVE = "rpg.command.give";
}
