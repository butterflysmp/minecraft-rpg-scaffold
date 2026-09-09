package io.github.butterflysmp.rpg.paper.content;

import net.kyori.adventure.text.Component;

/**
 * One element: an id, a display name, and -- since accrual -- two properties that do something.
 *
 * <p><b>THIS CLASS'S JAVADOC USED TO SAY "PURE IDENTITY ... CARRIES NO LOGIC", AND SO DID THE COMMENT
 * AT THE TOP OF EVERY {@code content/elements/*.yml}.</b> Both were true from the day the damage
 * multiplier was deleted in Phase 2A until the day accrual was wired, and both are now false. They
 * were rewritten in the same commit as the schema rather than afterwards, because a comment claiming
 * an element "never touches damage" is exactly the {@code flint_staff.yml} trap this repo has already
 * been bitten by twice: correct reasoning, invisible to every compiler and test, wrong on the day it
 * ships.
 *
 * <p>What is still true, and is the line worth keeping: <b>an element is never MATH.</b> It
 * multiplies nothing. The damage triangle is gone and is not coming back. What it now carries is
 * which status its damage accrues, and how its damage numbers are marked -- battlefield state and
 * presentation, neither of which is a factor on the number.
 *
 * <p>Both new fields are OPTIONAL at this layer, and that is deliberate rather than lax:
 * {@code RpgPlugin.saveDefaultContent} uses {@code saveResource(path, false)}, which never
 * overwrites, so a server whose data folder predates this slice holds seven one-field files. Those
 * must keep working. {@code ContentValidator} is where a missing or dangling value gets NAMED at
 * boot; the loader's job is to not take the server down over it.
 *
 * @param id           the filename, minus {@code .yml}
 * @param displayName  the MiniMessage name shown on a weapon tooltip. Unchanged by this slice --
 *                     fire's word stays red even though its damage numbers are marked in orange.
 * @param damageSymbol the glyph drawn before this element's floating damage numbers, ALREADY PARSED.
 *                     Null when the element declares none. Parsed once at load rather than per hit:
 *                     {@code DamagePopupManager.onChange} runs on every hit on every region thread,
 *                     where {@code WeaponLore} can afford a use-time parse because a tooltip is minted
 *                     rarely. Parsing here is also what lets a malformed value be named and skipped.
 * @param appliesStatus the status id this element's damage accrues, or null for none. Six of the
 *                     seven shipped elements are null. Naming the STATUS and not its rate is the
 *                     whole scope of this field -- how fast it stacks lives in {@code Scorch}, and an
 *                     element that could declare arbitrary effects is a bigger feature than anyone
 *                     asked for.
 */
public record ElementDefinition(String id, String displayName,
                                Component damageSymbol, String appliesStatus) {

    /**
     * An element that is only identity: a name, no glyph, no accrual.
     *
     * <p>Kept so the positional construction sites that predate this slice read unchanged, and so a
     * test that cares about neither new field does not have to state them. It is also the honest
     * shape of six of the seven shipped elements before their glyphs were authored.
     */
    public ElementDefinition(String id, String displayName) {
        this(id, displayName, null, null);
    }
}
