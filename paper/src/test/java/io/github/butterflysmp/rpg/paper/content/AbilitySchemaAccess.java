package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.ability.CastSpec;
import org.bukkit.configuration.file.YamlConfiguration;

/** Test access to {@code AbilitySchema.parseCast} (package-private) from another test package. */
public final class AbilitySchemaAccess {

    private AbilitySchemaAccess() {}

    public static CastSpec parse(String yaml) throws Exception {
        YamlConfiguration y = new YamlConfiguration();
        y.loadFromString(yaml);
        return AbilitySchema.parseCast(y);
    }
}
