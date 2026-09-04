package io.github.butterflysmp.rpg.core.recipe;

import io.github.butterflysmp.rpg.core.weapon.CraftResultToken;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One shaped crafting recipe of OUR OWN, fully described. Constructed from YAML by the content
 * loader in the paper module -- core never reads files, and nothing here knows Bukkit exists.
 *
 * <h2>WE DO NOT OWN VANILLA RECIPES, SO THEY ARE IDENTIFIED BY THEIR RESULT.
 * WE OWN OURS, SO THEY ARE IDENTIFIED BY THEIR KEY.</h2>
 *
 * That sentence is why this record exists BESIDE {@code GearDefinition#craftResult()} rather than
 * replacing it. A vanilla recipe is somebody else's object with no identifier we may rely on, so
 * the only stable thing to key a claim on is the material it produces -- which is CONTESTED BY
 * DESIGN, as {@code CraftResultIndex} argues at length. A recipe we register has a
 * {@code NamespacedKey} we minted ourselves: unique, stable, uncontested. Both directions are
 * permanent and neither is a migration target.
 *
 * <h2>The claim points FROM the recipe TO the gear, and that is not arbitrary</h2>
 *
 * <ol>
 *   <li><b>The reference is mandatory one way and optional the other.</b> A recipe cannot exist
 *       without naming what it produces -- that IS the recipe. {@code craft_result} is optional,
 *       and {@link CraftResultToken#normalise} already says so: "absent is the norm: most gear
 *       does not participate in mint-on-craft". A mandatory field belongs on the record that
 *       requires it.
 *   <li><b>It makes one collision legal and the other impossible.</b> Two recipes minting one
 *       weapon is TWO WAYS TO CRAFT IT -- a feature. Two recipes claiming one key cannot be
 *       authored at all: the id is the filename, and a directory holds one of those. So the recipe
 *       axis needs no contested-claim policy, and MUST NOT GROW ONE -- see
 *       {@code CraftResultIndex}, where the result axis drops both claimants and this one does not.
 *   <li><b>It checks the easy way round.</b> {@code mints} is checkable against four registries
 *       that already exist. A recipe key would only be checkable against a registry being built in
 *       the same pass, and a material token always "exists" as a string.
 * </ol>
 *
 * <h2>EVERY CHECK BELOW IS A BOOT ABORT IF IT DOES NOT HAPPEN HERE</h2>
 *
 * {@code ShapedRecipe.shape}, {@code ShapedRecipe.setIngredient} and {@code NamespacedKey}'s
 * constructor all use {@code Preconditions.checkArgument} -- they THROW, they do not report. An
 * unvalidated definition reaching the registrar therefore takes the whole server down on boot,
 * breaking the contract every other loader keeps: a typo in the 400th file must cost that file and
 * nothing else. So the rules are enforced here, in pure Java, where a throw becomes a named,
 * skipped file and where a unit test can reach them with no server.
 *
 * <p><b>There is no {@code result:} field, deliberately.</b> The registered result is the material
 * of the gear this mints, resolved by paper at registration. Authoring it separately would create
 * two places that can disagree about what a craft produces, and the disagreement would be invisible
 * until a player crafted the wrong item.
 *
 * @param id          the recipe's id: its filename minus {@code .yml}, and the key half of the
 *                    {@code NamespacedKey} paper builds.
 * @param shape       1-3 rows, each 1-3 characters, all the same width. A space is an empty cell.
 * @param ingredients one material token per non-space character in the shape.
 * @param mints       the id of the gear definition a craft of this recipe hands over.
 */
public record RecipeDefinition(String id, List<String> shape, Map<Character, String> ingredients,
                               String mints) {

    /**
     * What {@code NamespacedKey}'s own {@code validate()} accepts for the key half.
     *
     * <p>Checked HERE rather than left to Bukkit because {@code new NamespacedKey(plugin, key)}
     * lower-cases the key and then throws {@code IllegalArgumentException} on anything outside
     * {@code [a-z0-9_-./]}. A file called {@code flint staff.yml} would abort {@code onEnable}
     * rather than being skipped and named.
     */
    private static final String LEGAL_ID = "[a-z0-9._/-]+";

    /** Bukkit's own bound: a crafting grid is at most three by three. */
    private static final int MAX_SIDE = 3;

    public RecipeDefinition {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("recipe id required");
        }
        if (!id.matches(LEGAL_ID)) {
            throw new IllegalArgumentException("recipe id '" + id + "' is not a legal key: only "
                    + "lowercase letters, digits and . _ - / are allowed. The id is the filename, "
                    + "so rename the file");
        }
        // MANDATORY, unlike craft_result. A recipe naming nothing to mint would register, hand the
        // player a plain vanilla item forever, and report nothing -- the same silent failure the
        // blank-craft_result throw exists to prevent, on the axis where the field is the point.
        if (mints == null || mints.isBlank()) {
            throw new IllegalArgumentException("recipe '" + id + "' has no 'mints:'; a recipe of "
                    + "ours exists to produce a gear definition, and must name which one");
        }

        if (shape == null || shape.isEmpty()) {
            throw new IllegalArgumentException("recipe '" + id + "' has no 'shape:'; it must be a "
                    + "YAML list of 1 to 3 rows (one '- ' item per line)");
        }
        if (shape.size() > MAX_SIDE) {
            throw new IllegalArgumentException("recipe '" + id + "' has " + shape.size()
                    + " shape rows; a crafting grid is at most " + MAX_SIDE);
        }

        int width = -1;
        for (String row : shape) {
            if (row == null || row.isEmpty()) {
                throw new IllegalArgumentException("recipe '" + id + "' has an empty shape row; "
                        + "use spaces for empty cells so the row still has a width");
            }
            if (row.length() > MAX_SIDE) {
                throw new IllegalArgumentException("recipe '" + id + "' has a shape row '" + row
                        + "' of " + row.length() + " columns; a crafting grid is at most "
                        + MAX_SIDE);
            }
            // RECTANGULAR. Bukkit's shape() throws on ragged rows, and a throw there is a boot
            // abort rather than a skipped file.
            if (width == -1) {
                width = row.length();
            } else if (row.length() != width) {
                throw new IllegalArgumentException("recipe '" + id + "' has ragged shape rows: '"
                        + row + "' is " + row.length() + " wide but an earlier row is " + width
                        + ". Pad with spaces so every row is the same width");
            }
        }

        Map<Character, String> declared = ingredients == null ? Map.of() : ingredients;

        // EVERY NON-SPACE CELL MUST BE MAPPED. Bukkit's shape() seeds its ingredient map with a
        // NULL per symbol and setIngredient is what replaces it, so a symbol nobody maps stays null
        // and the recipe registers half-formed -- matching nothing, reporting nothing.
        boolean anyCell = false;
        for (String row : shape) {
            for (char cell : row.toCharArray()) {
                if (cell == ' ') continue;
                anyCell = true;
                if (!declared.containsKey(cell)) {
                    throw new IllegalArgumentException("recipe '" + id + "' uses '" + cell
                            + "' in its shape but lists no ingredient for it");
                }
            }
        }
        if (!anyCell) {
            throw new IllegalArgumentException("recipe '" + id + "' has a shape of nothing but "
                    + "spaces; it would match an empty grid");
        }

        for (Map.Entry<Character, String> entry : declared.entrySet()) {
            Character symbol = entry.getKey();
            if (symbol == null) {
                throw new IllegalArgumentException("recipe '" + id + "' has a null ingredient key");
            }
            // Bukkit: "Space in recipe shape must represent no ingredient".
            if (symbol == ' ') {
                throw new IllegalArgumentException("recipe '" + id + "' maps an ingredient to a "
                        + "space; a space is an EMPTY cell and can never carry one");
            }
            if (!appearsInShape(shape, symbol)) {
                throw new IllegalArgumentException("recipe '" + id + "' lists an ingredient for '"
                        + symbol + "', which does not appear in its shape");
            }
            String material = entry.getValue();
            if (material == null || material.isBlank()) {
                throw new IllegalArgumentException("recipe '" + id + "' has a blank ingredient for '"
                        + symbol + "'; name a material or remove the entry");
            }
        }

        // Normalised through the SAME function craft_result claims use, so 'FLINT', 'flint' and
        // 'minecraft:flint' are one token here exactly as they are one token there. Two spellings
        // of one material resolving differently is the failure CraftResultToken was written for.
        Map<Character, String> normalised = new LinkedHashMap<>();
        for (Map.Entry<Character, String> entry : declared.entrySet()) {
            normalised.put(entry.getKey(), CraftResultToken.token(entry.getValue()));
        }

        shape = List.copyOf(shape);
        ingredients = Map.copyOf(normalised);
    }

    /**
     * Would this recipe fit in a player's own 2x2 inventory grid?
     *
     * <h2>THE ONE CRAFTING SURFACE THAT NEITHER MINTS NOR IS HIJACKED</h2>
     *
     * Crafting tables are hijacked to our own menu, which mints. The Crafter block is guarded. The
     * player's 2x2 inventory grid is neither: {@code onPrepareCraft} screens it for gear used as an
     * INGREDIENT, but nothing there replaces a vanilla result with minted gear -- {@code commitCraft}
     * is only reachable from the crafting menu. So a 2x2-shaped custom recipe is craftable in the
     * inventory and hands the player a PLAIN VANILLA ITEM, silently.
     *
     * <p><b>This is a pre-existing hole, not one custom recipes opened.</b> {@code shears} claims
     * {@code craft_result: shears} and vanilla shears is a 2x2 recipe, so crafting shears in the
     * inventory grid today already yields a plain vanilla pair.
     *
     * <p>The Flint Staff is safe from it BY SHAPE, not by guard -- it is three rows tall. That is
     * exactly why this method exists rather than a comment: the person who one day shortens a shape
     * will not think to look, and a boot warning fires at the moment they do.
     */
    public boolean fitsInTwoByTwo() {
        if (shape.size() > 2) return false;
        for (String row : shape) {
            if (row.length() > 2) return false;
        }
        return true;
    }

    private static boolean appearsInShape(List<String> shape, char symbol) {
        for (String row : shape) {
            if (row.indexOf(symbol) >= 0) return true;
        }
        return false;
    }
}
