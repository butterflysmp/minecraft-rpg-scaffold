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
import java.util.Optional;
import java.util.function.Function;
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
 * order or a guaranteed tier above another (core's {@code CascadeTable}), an unknown item id, or an item under a pool
 * of another rarity (the seat's ruling, 2026-11-19: a pool holds only items of its own rarity). Its Loaded line prints
 * each default tier's written chance and its TRUE odds, and nothing's.
 */
public final class LootLoader {

    private final Logger log;
    private final Function<String, Optional<Rarity>> itemRarity;
    /** Item 6: whether {@code dev_sources:} loads (a dev server only). */
    private final boolean dev;

    /**
     * @param itemRarity an item id's own rarity, when it names a loaded weapon, shield, armour piece, tool or accessory;
     *                   empty for an unknown id
     */
    public LootLoader(Logger log, Function<String, Optional<Rarity>> itemRarity, boolean dev) {
        this.log = log;
        this.itemRarity = itemRarity;
        this.dev = dev;
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
                if (sid.startsWith(DEV_PREFIX)) {
                    throw new IllegalArgumentException("source '" + sid + "' starts '" + DEV_PREFIX
                            + "', which only dev_sources may: a real source must not look like a dev one");
                }
                sources.put(sid, source(sid, s, def));
            }
        }
        // Item 6: the dev-only example sources, loaded ONLY on a dev server, each named dev_..., used by nothing in the
        // game (the dev roll command reads them).
        ConfigurationSection ds = root.getConfigurationSection("dev_sources");
        if (ds != null && dev) {
            for (String sid : ds.getKeys(false)) {
                if (!sid.startsWith(DEV_PREFIX)) {
                    throw new IllegalArgumentException("dev source '" + sid + "' must start '" + DEV_PREFIX + "'");
                }
                sources.put(sid, source(sid, ds, def));
            }
        }
        return new LootContent(id, def, java.util.Collections.unmodifiableMap(sources));
    }

    /** What every dev-only source's id starts with, and no real source's may. */
    static final String DEV_PREFIX = "dev_";

    private LootContent.Source source(String sid, ConfigurationSection parent, CascadeTable def) {
        ConfigurationSection one = parent.getConfigurationSection(sid);
        if (one == null) throw new IllegalArgumentException("source '" + sid + "' is not a section");
        CascadeTable t = one.contains("tiers") ? table("source '" + sid + "'", one) : def;
        return new LootContent.Source(t, pools(sid, one.getConfigurationSection("pools")));
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
                Optional<Rarity> own = itemRarity.apply(item);
                if (own.isEmpty()) {
                    throw new IllegalArgumentException("source '" + sid + "' pool " + key + " names an unknown item '"
                            + item + "'");
                }
                // The seat's mechanism ruling (2026-11-19): a pool holds only items of its own rarity.
                if (own.get() != r) {
                    throw new IllegalArgumentException("source '" + sid + "' pool " + key + " names '" + item + "', whose"
                            + " own rarity is " + own.get().name().toLowerCase(Locale.ROOT) + ", not "
                            + r.name().toLowerCase(Locale.ROOT) + ": a pool holds only items of its rarity");
                }
            }
            out.put(r, List.copyOf(items));
        }
        return out;
    }
}
