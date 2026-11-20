package io.github.butterflysmp.rpg.paper.content;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Locale;
import java.util.OptionalDouble;
import java.util.logging.Logger;

/**
 * {@code content/world_damage.yml}, the game-wide damage rules (PLAN-world-damage.md). Today it holds one value: Ben's
 * void share (2026-11-19, PLAN-dragon-fight.md section 1 j), the fraction of a player's MAXIMUM health one void tick
 * takes ({@code core}'s {@code VoidShare}).
 *
 * <p>One {@code Loaded world damage} line, or a {@code Refusing the void rule} line naming why. <b>A refused or absent
 * value leaves the void as the server's own</b> ({@code VanillaDamagePolicy} PASSes VOID), and says so: a rule that
 * silently did not load would read exactly like one that did.
 */
public final class WorldDamageLoader {

    /** The parsed rules. Empty means the void is the server's own. */
    public record Rules(OptionalDouble voidShare) {}

    private final Logger log;

    public WorldDamageLoader(Logger log) {
        this.log = log;
    }

    public Rules load(File file) {
        if (!file.isFile()) {
            log.warning("Refusing the void rule: content/world_damage.yml is absent, so the void is the server's own"
                    + " (4 vanilla points a tick, which our store does not see).");
            return new Rules(OptionalDouble.empty());
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        Object raw = yaml.get("void.share_of_max_per_tick");
        if (raw == null) {
            log.warning("Refusing the void rule: void.share_of_max_per_tick is missing from " + file.getName()
                    + ", so the void is the server's own.");
            return new Rules(OptionalDouble.empty());
        }
        if (!(raw instanceof Number n) || !(n.doubleValue() > 0.0 && n.doubleValue() <= 1.0)) {
            log.warning("Refusing the void rule: void.share_of_max_per_tick is '" + raw + "' in " + file.getName()
                    + "; it must be a number in (0, 1]. The void is the server's own.");
            return new Rules(OptionalDouble.empty());
        }
        double share = n.doubleValue();
        log.info(String.format(Locale.ROOT, "Loaded world damage: void takes %.2f of a player's maximum health per void"
                + " tick, defence bypassed; mobs unchanged", share));
        return new Rules(OptionalDouble.of(share));
    }
}
