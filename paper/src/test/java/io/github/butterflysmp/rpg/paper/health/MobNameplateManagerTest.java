package io.github.butterflysmp.rpg.paper.health;

import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The register-if-absent property of {@code onMobAppear}'s map half. {@code /rpg mobdamage} re-calls
 * {@code onMobAppear} every cast; if it REPLACED the plate it would reset the version to 1 each time,
 * and the next tick's {@code applyDamage} bump (1->2) would race the viewer's 4-tick LOS sample --
 * the "every-other-cast" miss. The version must survive a re-appear so it climbs monotonically for
 * {@link ViewerNameplateState#decide}. This guards exactly that, on a plain map -- no manager
 * instance, scheduler, sender, or LivingEntity.
 */
class MobNameplateManagerTest {

    @Test
    void reAppearPreservesTheVersionAndTextOfAnAlreadyRegisteredMob() {
        Map<UUID, MobNameplateManager.Nameplate> plates = new HashMap<>();
        UUID id = UUID.randomUUID();
        Component name = Component.text("Zombie");

        long onSpawn = MobNameplateManager.registerIfAbsent(plates, id, name, 100, 100);
        assertEquals(1, onSpawn, "first appearance creates version 1");

        // A hit bumps the version -- what onChange does on real damage.
        plates.get(id).update(NameplateText.of(name, 70, 100));
        assertEquals(2, plates.get(id).version(), "the hit bumped the version to 2");
        Component afterHit = plates.get(id).text();

        // The per-cast re-appear, carrying DIFFERENT numbers than the live plate.
        long reAppear = MobNameplateManager.registerIfAbsent(plates, id, name, 999, 100);

        assertEquals(2, reAppear, "re-appear PRESERVED the bumped version -- it did not reset to 1");
        assertEquals(afterHit, plates.get(id).text(),
                "and did not replace the live text with the re-appear's numbers");
        // Mutation: registerIfAbsent using plates.put(id, new Nameplate(...)) -> reAppear == 1 and the
        // text becomes 999/100 -> this reddens. (Verified break-first this pass.)
    }
}
