package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.recipe.RecipeRegistry;
import io.github.butterflysmp.rpg.core.weapon.ArrivalClaims;
import io.github.butterflysmp.rpg.core.weapon.CraftResultIndex;
import io.github.butterflysmp.rpg.core.weapon.GearDefinition;
import io.github.butterflysmp.rpg.core.weapon.SmithingUpgrades;
import io.github.butterflysmp.rpg.core.weapon.WeaponRegistry;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * R0c'S TWO COUNT LINES, MEASURED BEFORE THE BOOT (the seat's follow-up 4, 2026-11-22). {@code GATE-vanilla-bows.md} R0c
 * predicted {@code Mint-on-craft} and {@code Convert-on-arrival} by hand ("36 / 36 / 52 + the two claims; derived, not
 * measured"), and an R0 miss costs Ben a boot. This runs the plugin's own counting steps, in {@code RpgPlugin.onEnable}'s
 * order, over the real {@code content/}: the four gear loaders, the recipe and smithing loaders, then
 * {@code CraftResultIndex.build} and {@code ArrivalClaims.build}; and it prints both lines through {@link BootLines}, the
 * methods the plugin logs them with.
 *
 * <p><b>WHAT IT CANNOT RUN WITHOUT A SERVER, MEASURED:</b> the plugin's refusal pre-pass
 * ({@code ContentValidator.refusedCraftResults}) asks {@code Material.getMaxDurability}, which needs the server's
 * registry: with no server it throws {@code IllegalStateException: No RegistryAccess implementation found} (run
 * 2026-11-22). So this builds the index with NOTHING refused, and the smithing loader's material check is answered yes.
 * <b>Both lines are therefore measured GIVEN that nothing is refused</b>; that premise is R0c's own "no {@code Refusing}"
 * prediction, read on the boot, which still fails R0.
 */
class BootCountLinesTest {

    private static Logger quiet() {
        Logger log = Logger.getAnonymousLogger();
        log.setUseParentHandlers(false);
        return log;
    }

    private static File content(String name) {
        File f = Path.of("src", "main", "resources", "content", name).toFile();
        assertTrue(f.exists(), "no content at " + f.getAbsolutePath());
        return f;
    }

    /** The plugin's steps, in its order, over the real content, nothing refused (see the class javadoc). */
    private record Boot(List<GearDefinition> allGear, CraftResultIndex crafts, ArrivalClaims arrivals) {}

    private static Boot boot() {
        Logger log = quiet();
        WeaponRegistry weapons = new WeaponLoader(log).loadAll(content("weapons"));
        RecipeRegistry recipes = new RecipeLoader(log).loadAll(content("recipes"));
        SmithingUpgrades smithing = new SmithingLoader(log, id -> weapons.find(id).isPresent(), name -> true)
                .load(content("smithing.yml"));
        List<GearDefinition> allGear = new ArrayList<>();
        allGear.addAll(weapons.all());
        allGear.addAll(new ShieldLoader(log).loadAll(content("shields")).all());
        allGear.addAll(new ArmorLoader(log).loadAll(content("armor")).all());
        allGear.addAll(new ToolLoader(log).loadAll(content("tools")).all());
        CraftResultIndex crafts = CraftResultIndex.build(allGear, recipes.all(), log::warning, definition -> false);
        ArrivalClaims arrivals = ArrivalClaims.build(weapons.all(), crafts, smithing, log::warning);
        return new Boot(allGear, crafts, arrivals);
    }

    /** CONTROL: the loaders found the content, so a count of the gear is not a count of an empty directory. */
    @Test
    void theContentIsFound() {
        Boot boot = boot();
        assertEquals(24, boot.allGear().stream().filter(g -> g instanceof io.github.butterflysmp.rpg.core.weapon.WeaponDefinition)
                .count(), "R0c's Loaded line: 24 weapons");
    }

    @Test
    void mintOnCraftIsMeasured() {
        Boot boot = boot();
        assertEquals("Mint-on-craft: 38 indexed, 38 claiming, of 54 gear definitions",
                BootLines.mintOnCraft(boot.crafts(), boot.allGear().size()));
    }

    @Test
    void convertOnArrivalIsMeasured() {
        assertEquals("Convert-on-arrival: 9 materials {bow=bow, copper_sword=copper_sword, crossbow=crossbow, "
                        + "diamond_sword=diamond_sword, golden_sword=golden_sword, iron_sword=iron_sword, "
                        + "netherite_sword=netherite_sword, stone_sword=stone_sword, wooden_sword=wooden_sword}",
                BootLines.convertOnArrival(boot().arrivals()));
    }
}
