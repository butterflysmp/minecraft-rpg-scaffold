package io.github.butterflysmp.rpg.paper.build;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Where the player's derive reaches, read from source (PLAN-build-system.md section 3.5). Paper tests have no
 * server; these are the shapes a gate row could only read in play:
 *
 * <ul>
 *   <li>the stone's cast and a non-op's /rpg cast pass the player's derive to {@code AbilityService.cast};</li>
 *   <li>an operator's dev cast does NOT -- it casts the base ability (ruling 10);</li>
 *   <li>the Build screen's equipped-ability lore is rendered from the DERIVED definition, the same derive the
 *       cast uses -- THE ROW THE "render the lore from the base definition" MUTATION REDDENS.</li>
 * </ul>
 */
class AspectWiringSignatureTest {

    private static final Path CASTER = Path.of("src", "main", "java", "io", "github", "butterflysmp", "rpg", "paper",
            "build", "StoneCaster.java");
    private static final Path COMMAND = Path.of("src", "main", "java", "io", "github", "butterflysmp", "rpg", "paper",
            "command", "RpgCommand.java");
    private static final Path MENU = Path.of("src", "main", "java", "io", "github", "butterflysmp", "rpg", "paper",
            "menu", "BuildMenu.java");

    @Test
    void theStonesCastPassesThePlayersDerive() throws IOException {
        String caster = code(CASTER, 100);
        assertTrue(caster.contains("abilityService.cast(caster, abilityId.get(), aim, castable,"),
                "CONTROL: the stone's cast call is found");
        assertTrue(caster.contains("stones.deriveFor(playerId, profile));"),
                "the stone casts through the player's derive");
    }

    /**
     * Section 7.4: the recast is decided BEFORE the ordinary cast, from EVERY input, and it casts the follow-up
     * through castUnchecked -- never through cast/resolve, which would check Recall's cooldown and charge mana.
     */
    @Test
    void theRecastIsDecidedFirstAndCastsUnchecked() throws IOException {
        String caster = code(CASTER, 100);
        int input = caster.indexOf("tracker.input(slot, inputTick, abilityId.get(),");
        int unchecked = caster.indexOf("abilityService.castUnchecked(caster, followUp.get(), aim)");
        int ordinary = caster.indexOf("abilityService.cast(caster, abilityId.get(), aim, castable,");
        assertTrue(input > 0 && unchecked > input && ordinary > unchecked,
                "the tracker hears the input, then the recast, then the ordinary cast: " + input + " " + unchecked + " " + ordinary);
        assertTrue(caster.contains("tracker.castSucceeded(abilityId.get(), inputTick,"),
                "an ordinary Success opens (or closes) the window");
    }

    @Test
    void aNonOpsCastIsDerivedAndAnOpsIsNot() throws IOException {
        String command = code(COMMAND, 2000);
        assertTrue(command.contains("? abilityService.castUnchecked(caster, abilityId, aim)"),
                "CONTROL: the dev cast is found, and takes no derive (ruling 10)");
        assertTrue(command.contains(": abilityService.cast(caster, abilityId, aim, castable,")
                        && command.contains("adapters.stones().deriveFor(player.getUniqueId(), Optional.of(profile)));"),
                "a non-op's /rpg cast goes through the player's derive");
    }

    @Test
    void theBuildScreensLoreIsRenderedFromTheDerivedDefinition() throws IOException {
        String menu = code(MENU, 200);
        assertTrue(menu.contains(".deriveFor(viewer.getUniqueId(), profiles.profile(viewer.getUniqueId())).apply(def);"),
                "the lore derives the ability for the viewer, as the cast does");
        assertTrue(menu.contains("lore.addAll(numberLines(def, derived));"),
                "and renders its numbers FROM the derived definition, with the base beside them");
    }

    /** The file's code with comment lines removed, so prose naming a call cannot stand in for the call. */
    private static String code(Path path, int atLeast) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        assertTrue(lines.size() > atLeast, "read " + path + ": " + lines.size() + " lines");
        StringBuilder code = new StringBuilder();
        for (String line : lines) {
            String t = line.strip();
            if (t.startsWith("*") || t.startsWith("//") || t.startsWith("/*")) continue;
            code.append(t).append('\n');
        }
        return code.toString();
    }
}
