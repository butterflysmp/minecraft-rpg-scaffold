package io.github.butterflysmp.rpg.paper.content;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * Turns YAML into ElementDefinition. The only class that knows the element schema.
 *
 * <p>That schema was "nothing but an id and a display name -- an element carries no logic" until
 * accrual was wired. It is now three fields, two of them optional, and one of them can be authored
 * WRONG rather than merely omitted. See {@link ElementDefinition} for why the old wording is
 * recorded as falsified rather than quietly replaced.
 *
 * <p>An element's id is its filename minus .yml, as with the other content types. Fails soft: a
 * malformed file is logged, named, and skipped, and every other element still loads.
 *
 * <p><b>THE {@code catch} BELOW HAS NEVER FIRED IN PRODUCTION, AND THIS SLICE IS WHAT ARMS IT.</b>
 * {@code YamlConfiguration.loadConfiguration} does not throw on invalid YAML (it logs and returns an
 * empty config), and ids are filenames in one directory so {@code register} cannot see a duplicate.
 * So the try/catch was the shared fail-soft convention, carried for symmetry and structurally
 * unreachable. {@code damage_symbol} is the first element field that can be WRONG rather than merely
 * absent, and {@link #parse} now throws for it -- which means the skip path, the log line and the
 * summary count all execute for the first time. {@code ElementLoaderTest} exercises it deliberately,
 * because a fail-soft path that has never run is not known to work.
 */
public final class ElementLoader {

    private final Logger log;

    public ElementLoader(Logger log) {
        this.log = log;
    }

    public ElementRegistry loadAll(File elementsDir) {
        ElementRegistry registry = new ElementRegistry();
        File[] files = elementsDir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return registry;

        Arrays.sort(files); // deterministic load order across filesystems
        int skipped = 0;
        for (File f : files) {
            try {
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(f);
                registry.register(parse(idOf(f), yaml));
            } catch (RuntimeException ex) {
                skipped++;
                log.warning("Skipping malformed element '" + f.getName() + "': " + ex.getMessage());
            }
        }
        if (skipped > 0) {
            log.warning(skipped + " element file(s) were skipped. The server is still running, "
                    + "but that element is not loaded.");
        }
        return registry;
    }

    /** The id is the filename: fire.yml -> fire. */
    private static String idOf(File f) {
        String name = f.getName();
        return name.substring(0, name.length() - ".yml".length());
    }

    private ElementDefinition parse(String id, ConfigurationSection s) {
        return new ElementDefinition(
                id,
                s.getString("display_name", id),
                damageSymbol(id, s),
                appliesStatus(id, s));
    }

    /**
     * The element's damage-number glyph, parsed once here, or null when it declares none.
     *
     * <p><b>Absent is not an error at this layer</b>, for the {@code saveResource(path, false)}
     * reason in {@link ElementDefinition}: a data folder that predates this slice holds one-field
     * files, and rejecting them would take seven working elements off a running server to fix a
     * cosmetic gap. {@code ContentValidator} names the omission at boot instead, which is the
     * warn-not-throw shape {@code checkElement} already uses.
     *
     * <h2>MINIMESSAGE THROWS ON NOTHING, SO THE OBVIOUS GUARD WOULD HAVE BEEN DEAD CODE</h2>
     *
     * This method was first written as {@code try { deserialize } catch (RuntimeException)}, with a
     * javadoc claiming a malformed glyph became a named, skipped file. <b>MEASURED, because the test
     * written to prove it went red:</b> {@code MiniMessage.miniMessage().deserialize} threw for none
     * of ten malformed inputs -- an unknown tag, an unknown colour, a bad hex, an unclosed tag, a
     * mismatched close, a bare {@code <}, a bogus gradient. Every one came back as a Component whose
     * text is the literal source string.
     *
     * <p>So the catch could never fire, and the javadoc asserting enforcement would have been the
     * third instance of this repo's recorded trap: a comment that claims a rule is enforced, where
     * opening the file is the only way to find out it is not.
     *
     * <p><b>The real failure mode is not an exception -- it is a glyph that renders as its own
     * source.</b> {@code damage_symbol: "<god>*</god>"} draws the seven characters
     * {@code <god>*</god>} over a mob's head on every hit. Silent, ugly, and visible only in game.
     *
     * <p>Hence the guard is on the RESULT rather than on the call: deserialize, then look at what
     * came back. TWO checks. Empty text means the tags consumed everything
     * ({@code "<gold></gold>"}), so the number would be marked with an invisible glyph. Text still
     * containing {@code <} or {@code >} means MiniMessage printed something it did not parse. Both
     * are authoring mistakes rather than old files, so both take the named-and-skipped path.
     *
     * <p><b>THE SECOND CHECK IS A LIMITATION, NOT A DIAGNOSIS, AND THE MESSAGE MUST NOT PRETEND
     * OTHERWISE.</b> A deliberate chevron reaches this code as the same string a mistyped tag does --
     * {@code damage_symbol: "<white><</white>"} renders as the single character {@code <}, because a
     * bare {@code <} is one of the inputs MiniMessage passes through untouched. <b>The two are
     * indistinguishable here</b>, so this rejects both, and the thrown message says exactly that
     * rather than blaming a tag the author may never have written. An error that misnames its cause
     * sends someone hunting a bug that is not there, which is worse than one that admits what it
     * cannot tell apart. {@code ElementLoaderTest} pins the chevron case so the limitation is a
     * measured, asserted fact rather than a claim in this paragraph.
     *
     * <p><b>What is deliberately NOT guarded: length.</b> A glyph is one or two characters and a
     * whole word here would read badly, but no demonstrated failure produces one, and inventing a
     * cap would be a policy nobody asked for wearing the costume of a safety check. If a long
     * damage_symbol ever ships, that is when the bound gets a number and a reason.
     */
    private static Component damageSymbol(String id, ConfigurationSection s) {
        if (!s.contains("damage_symbol")) return null;
        if (!s.isString("damage_symbol")) {
            throw new IllegalArgumentException(
                    "element '" + id + "' has a non-scalar 'damage_symbol'; it must be one "
                            + "MiniMessage string, e.g. \"<gold>*</gold>\"");
        }
        String raw = s.getString("damage_symbol");
        // PRESENT AND EMPTY IS A CHOICE. ABSENT IS A GAP. THEY MUST NOT COLLAPSE.
        //
        // This threw on a blank value, calling it "a half-finished edit" -- correct while every
        // element was meant to be marked. Kinetic is now deliberately unmarked: it is the NEUTRAL
        // element, and a mark on an unflavoured hit says something the hit does not mean.
        //
        // So `damage_symbol: ""` is a declaration of no glyph, accepted silently. ABSENT keeps its
        // old meaning -- a data folder that predates the field, since saveResource(path, false) never
        // overwrites -- and ContentValidator goes on naming it, so a legacy file is still reported
        // rather than mistaken for a decision.
        //
        // This is the rule Scorch.UNDECLARED_CAP already states for a different field: ABSENCE IS
        // NOT A NEUTRAL VALUE. There, an absent cap is not "no cap" but the absence of one; here, an
        // absent glyph is not "no glyph" but an unanswered question. Same rule, second field.
        if (raw.isBlank()) return Component.empty();

        Component parsed = MiniMessage.miniMessage().deserialize(raw);
        String rendered = PlainTextComponentSerializer.plainText().serialize(parsed);
        if (rendered.isEmpty()) {
            // Non-blank source that renders as nothing -- `"<gold></gold>"`, tags with no content
            // between them. Still a half-finished edit, and STILL DISTINGUISHABLE from the empty
            // declaration above, because that one never reaches here.
            throw new IllegalArgumentException(
                    "element '" + id + "' has a 'damage_symbol' of " + raw + " that renders as "
                            + "NOTHING -- the tags consumed the whole value. Write \"\" if the "
                            + "element is meant to be unmarked");
        }
        if (rendered.indexOf('<') >= 0 || rendered.indexOf('>') >= 0) {
            throw new IllegalArgumentException(
                    "element '" + id + "' has a damage_symbol of " + raw + " that renders as the"
                            + " literal text [" + rendered + "], which is what would be drawn over"
                            + " the mob. A damage symbol cannot contain < or >. MiniMessage prints"
                            + " an unparsed tag as its own source text, so a MISTYPED TAG and a"
                            + " DELIBERATE CHEVRON arrive here as the same string and this rejects"
                            + " both. If a tag was meant, check its spelling; if a chevron was, it"
                            + " is not supported.");
        }
        return parsed;
    }

    /**
     * The status this element's damage accrues, or null when it declares none.
     *
     * <p>Existence is NOT checked here -- {@code ContentValidator} does it, along with the harder
     * question of whether the named status can accrue at all. The loader has no status registry and
     * should not grow one: it would make element loading depend on status loading having finished,
     * for a check that belongs in the pass whose whole job is cross-references.
     *
     * <p>The non-scalar guard is worth its lines even so. {@code applies_status: [scorch, soaked]} is
     * a plausible thing for an author to try, and {@code getString} returns null for a list -- so the
     * element would load, look correct, and silently accrue nothing FOREVER. That is the failure this
     * repo keeps recording, so it is a named skip rather than a shrug.
     */
    private static String appliesStatus(String id, ConfigurationSection s) {
        if (!s.contains("applies_status")) return null;
        if (!s.isString("applies_status")) {
            throw new IllegalArgumentException(
                    "element '" + id + "' has a non-scalar 'applies_status'; an element accrues at "
                            + "most ONE status, named as a plain string");
        }
        String raw = s.getString("applies_status");
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException(
                    "element '" + id + "' has a blank 'applies_status'. Omit the key to accrue "
                            + "nothing; a blank value is a half-finished edit");
        }
        return raw;
    }
}
