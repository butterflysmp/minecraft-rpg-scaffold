package io.github.butterflysmp.rpg.paper.summon;

import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.mob.MobHostility.Side;
import io.github.butterflysmp.rpg.paper.content.AbilitySchemaAccess;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 4c, THE LEFT CLICK'S ORDER (D5 as ruled; the seat, 2026-11-24). */
class SummonDirectTest {

    @Test
    void onlyAMobCanBeOrdered() {
        assertTrue(SummonService.directable(Side.MOB));
        assertFalse(SummonService.directable(Side.PLAYER), "a player: nothing");
        assertFalse(SummonService.directable(Side.SUMMON), "any summon: nothing");
    }

    @Test
    void theCastTypeParsesWithItsReachAndRefusesNone() throws Exception {
        assertEquals(new CastSpec.DirectSummon(32), AbilitySchemaAccess.parse("type: direct_summon\nreach: 32\n"));
        assertThrows(IllegalArgumentException.class, () -> AbilitySchemaAccess.parse("type: direct_summon\n"),
                "reach is required: no default is invented");
    }

    private static final Path MAIN = Path.of("src", "main", "java", "io", "github", "butterflysmp", "rpg", "paper");

    private static List<String> code(Path p) throws IOException {
        return Files.readAllLines(MAIN.resolve(p), StandardCharsets.UTF_8).stream()
                .filter(l -> !(l.trim().startsWith("//") || l.trim().startsWith("*") || l.trim().startsWith("/*"))).toList();
    }

    private static int at(List<String> c, String needle, int from) {
        for (int i = Math.max(0, from); i < c.size(); i++) if (c.get(i).contains(needle)) return i;
        return -1;
    }

    @Test
    void thePickComesBeforeTheChargeAndTheSwingDealsNothing() throws IOException {
        List<String> s = code(Path.of("summon", "SummonService.java"));
        int m = at(s, "public Optional<CastResult> direct(Player player,", 0);
        int noBody = at(s, "if (body.isEmpty()) return Optional.empty();", m);
        int passSummons = at(s, "return b != null && !b.equals(player) && !io.github.butterflysmp.rpg.paper.adapter.CombatSides.refused(", m);
        int notMob = at(s, "if (target == null || !directable(", m);
        int charge = at(s, "CastResult charged = charge.get();", notMob);
        int order = at(s, "if (charged instanceof CastResult.Success) store().direct(body.get(), target.getUniqueId());", charge);
        assertTrue(m > 0 && noBody > m && passSummons > noBody && notMob > passSummons && charge > notMob && order > charge,
                "nothing of his, then the pick through summons, then not-a-mob, THEN the charge, then the order");
        List<String> w = code(Path.of("weapon", "WeaponFire.java"));
        int directIntercept = at(w, "if (binding.get().ability().cast() instanceof io.github.butterflysmp.rpg.core.ability.CastSpec.DirectSummon direct) {", 0);
        int engine = at(w, "Optional<CastResult> result = Optional.of(weaponService.fire(caster, binding.get(), aim));", directIntercept);
        assertTrue(directIntercept > 0 && engine > directIntercept, "handed over before the engine");
        List<String> l = code(Path.of("listener", "RpgListeners.java"));
        int melee = at(l, "public void onPlayerMeleeAttack(", 0);
        // P-2 (2026-11-25): the stand-down is REMOVED; the combination that could reach it is refused at load
        // (WeaponDefinitionTest.aLeftClickThatDirectsASummonMayNotAlsoAuthorAnAttackDamage).
        assertTrue(melee > 0, "the melee handler is still found, so the absence below is not a missed file");
        assertEquals(-1, at(l, "leftClickDirects", 0), "no stand-down remains on the melee path");
    }
}
