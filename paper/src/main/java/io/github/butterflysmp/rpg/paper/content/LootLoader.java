package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.loot.CascadeTable;
import io.github.butterflysmp.rpg.core.weapon.Rarity;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.logging.Logger;

/**
 * THE CASCADING LOOT TABLES (PLAN-loot.md, d; Ben, 2026-11-19). Every {@code *.yml} in {@code content/loot/}:
 * <pre>
 * default:
 *   tiers:                       # rarest to commonest, each a rarity and a chance in [0, 1]
 *     - rarity: exotic
 *       chance: 0.01
 * sources:                       # each drop source, by id
 *   some_source:
 *     tiers: [...]               # optional: this source's own chances; absent, the default's
 *     pools:                     # per rarity, the item ids it can give
 *       rare: [emberblade, locust]
 * </pre>
 * A file is refused WHOLE, by name and reason, for a chance outside [0, 1], an unknown rarity, tiers out of rarity
 * order or a guaranteed tier above another (core's {@code CascadeTable}), or an unknown item id. Its Loaded line prints
 * each default tier's written chance and its TRUE odds, and nothing's.
 */
public final class LootLoader {

    private final Logger log;
    private final Predicate<String> itemKnown;

    /** @param itemKnown whether an item id names a loaded weapon, shield, armour piece, tool or accessory */
    public LootLoader(Logger log, Predicate<String> itemKnown) {
        this.log = log;
        this.itemKnown = itemKnown;
    }

    public Map<String, LootContent> loadAll(File dir) {
        Map<String, LootContent> out = new LinkedHashMap<>();
        File[] files = dir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return out;
        Arrays.sort(files);
        for (File f : files) {
            String id = f.getName().substring(0, f.getName().length() - ".yml".length());
            try {
                LootContent c = parse(id, YamlConfiguration.loadConfiguration(f));
                out.put(id, c);
                log.info(loadedLine(c));
            } catch (RuntimeException ex) {
                log.warning("Refusing loot '" + id + "' in " + f.getName() + ": " + ex.getMessage());
            }
        }
        return out;
    }

    static String loadedLine(LootContent c) {
        StringBuilder b = new StringBuilder("Loaded loot '").append(c.id()).append("': default ");
        CascadeTable.Odds odds = c.defaultTable().trueOdds();
        List<String> tiers = new ArrayList<>();
        for (CascadeTable.Tier t : c.defaultTable().tiers()) {
            tiers.add(String.format(Locale.ROOT, "%s %.2f%% (truly %.3f%%)", t.rarity().name().toLowerCase(Locale.ROOT),
                    100.0 * t.chance(), 100.0 * odds.of(t.rarity())));
        }
        b.append(String.join(", ", tiers));
        b.append(String.format(Locale.ROOT, ", nothing truly %.3f%%; %d sources %s", 100.0 * odds.nothing(),
                c.sources().size(), c.sources().keySet()));
        return b.toString();
    }

    LootContent parse(String id, ConfigurationSection root) {
        ConfigurationSection d = root.getConfigurationSection("default");
        if (d == null) throw new IllegalArgumentException("needs a 'default:' table");
        CascadeTable def = table("default", d);
        Map<String, LootContent.Source> sources = new LinkedHashMap<>();
        ConfigurationSection s = root.getConfigurationSection("sources");
        if (s != null) {
            for (String sid : s.getKeys(false)) {
                ConfigurationSection one = s.getConfigurationSection(sid);
                if (one == null) throw new IllegalArgumentException("source '" + sid + "' is not a section");
                CascadeTable t = one.contains("tiers") ? table("source '" + sid + "'", one) : def;
                sources.put(sid, new LootContent.Source(t, pools(sid, one.getConfigurationSection("pools"))));
            }
        }
        return new LootContent(id, def, java.util.Collections.unmodifiableMap(sources));
    }

    private static CascadeTable table(String where, ConfigurationSection s) {
        List<Map<?, ?>> raw = s.getMapList("tiers");
        if (raw.isEmpty()) throw new IllegalArgumentException(where + " needs 'tiers:', a list of rarity and chance");
        List<CascadeTable.Tier> tiers = new ArrayList<>();
        for (Map<?, ?> m : raw) {
            Rarity r = rarity(where, m.get("rarity"));
            Object c = m.get("chance");
            if (!(c instanceof Number n)) {
                throw new IllegalArgumentException(where + " tier '" + r.name().toLowerCase(Locale.ROOT)
                        + "' needs a number 'chance:'");
            }
            tiers.add(new CascadeTable.Tier(r, n.doubleValue()));
        }
        try {
            return new CascadeTable(tiers);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(where + ": " + ex.getMessage());
        }
    }

    private static Rarity rarity(String where, Object name) {
        Rarity r = name == null ? null : Rarity.fromName(name.toString());
        if (r == null) {
            throw new IllegalArgumentException(where + " has an unknown rarity '" + name + "', expected one of "
                    + Arrays.toString(Rarity.values()).toLowerCase(Locale.ROOT));
        }
        return r;
    }

    private Map<Rarity, List<String>> pools(String sid, ConfigurationSection p) {
        Map<Rarity, List<String>> out = new EnumMap<>(Rarity.class);
        if (p == null) return out;
        for (String key : p.getKeys(false)) {
            Rarity r = rarity("source '" + sid + "' pools", key);
            List<String> items = p.getStringList(key);
            for (String item : items) {
                if (!itemKnown.test(item)) {
                    throw new IllegalArgumentException("source '" + sid + "' pool " + key + " names an unknown item '"
                            + item + "'");
                }
            }
            out.put(r, List.copyOf(items));
        }
        return out;
    }
}
