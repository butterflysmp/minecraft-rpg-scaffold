package io.github.butterflysmp.rpg.paper.content;

import io.github.butterflysmp.rpg.core.ability.CastSpec;
import io.github.butterflysmp.rpg.core.ability.effect.EffectSpec;
import io.github.butterflysmp.rpg.core.weapon.Rarity;
import io.github.butterflysmp.rpg.core.weapon.WeaponClass;
import io.github.butterflysmp.rpg.core.weapon.WeaponDefinition;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The SHIPPED {@code withered_shortbow.yml}, through the real loader (WITHERED-SHORTBOW, PLAN-wither.md 9).
 *
 * <p>No general test guards the content rules this file must obey -- a travelling ranged weapon authors
 * knockback; cooldowns on the 4-tick grid; its Wither comes from the element, not a {@code type: status}
 * -- so this pins them for this file, with Ben's provisional 9.1 column (2026-09-30, "everything else is
 * fine"). Changing a number here is a tuning decision, and this reddening is the prompt to record it.
 */
class WitheredShortbowContentTest {

    private static WeaponDefinition bow() {
        Logger log = Logger.getLogger("WitheredShortbowContentTest-" + System.nanoTime());
        log.setUseParentHandlers(false);
        log.setLevel(Level.OFF);
        Path root;
        try {
            var url = WitheredShortbowContentTest.class.getResource("/content");
            assertNotNull(url, "bundled content is missing from the test classpath");
            root = Path.of(url.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
        return new WeaponLoader(log).loadAll(new File(root.resolve("weapons").toString()))
                .find("withered_shortbow")
                .orElseThrow(() -> new AssertionError("withered_shortbow.yml did not load"));
    }

    /** Ben's words: "an Uncommon ranged weapon of Wither element". Mutation: any of the three -> red. */
    @Test
    void itIsAnUncommonRangedWeaponOfTheWitherElement() {
        var bow = bow();
        assertEquals("wither", bow.element());
        assertEquals(Rarity.UNCOMMON, bow.rarity());
        assertEquals(WeaponClass.RANGER, bow.weaponClass());
        assertEquals("Withered Shortbow", bow.displayName());
    }

    /**
     * 9.1's PROVISIONAL column, as accepted. Mutation: any number moved -> red.
     */
    @Test
    void itCarriesTheProvisionalNumbers() {
        var bow = bow();
        var shot = bow.trigger("right_click").orElseThrow().ability();
        var cast = assertInstanceOf(CastSpec.Projectile.class, shot.cast(), "a travelling bow");
        assertEquals(16.0, bow.attackDamage(), 0.0, "16, the Short Bow's");
        assertEquals(16, shot.cooldownTicks(), "16, one step slower than the Short Bow's 12");
        assertEquals(0, shot.cooldownTicks() % 4, "on the 4-tick input grid (CLAUDE.md)");
        assertEquals(8, bow.quiverSize());
        assertEquals(60, bow.reloadTicks());
        assertEquals(3.0, cast.speed(), 0.0);
        assertEquals(0.05, cast.gravity(), 0.0);
        assertEquals(120, cast.maxLifetimeTicks());
        assertEquals("arrow", cast.body());
    }

    /**
     * THE TWO CONTENT RULES THE ON_HIT MUST OBEY. Its Wither is the ELEMENT's (WS3), so a weapon_damage
     * wearing {@code wither} and NO {@code type: status}; and it is a travelling ranged weapon, so it
     * authors knockback (the standing ruling, 2026-09-13).
     *
     * <p>Mutations: delete the knockback entry -> red. Add {@code type: status, status_id: withering} ->
     * red (it would apply Wither twice, once uncapped by the hit).
     */
    @Test
    void itsWitherIsTheElementsAndItAuthorsKnockback() {
        List<EffectSpec> onHit = bow().trigger("right_click").orElseThrow().ability().onHit();
        assertEquals(List.of(new EffectSpec.WeaponDamage("wither"), new EffectSpec.Knockback(0.1)), onHit,
                "weapon_damage wearing wither, then knockback 0.1; nothing else");
    }
}
