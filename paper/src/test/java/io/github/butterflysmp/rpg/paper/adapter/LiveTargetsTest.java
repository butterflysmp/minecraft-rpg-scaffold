package io.github.butterflysmp.rpg.paper.adapter;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A mob in its death animation must never take a hit meant for a live one (the {@code 61e722f2} boot: LB2, LB5 (a),
 * WB2, WS2b, WS2c, WS3). The engine (Bukkit's {@code rayTrace}) returns the CLOSEST entity its filter admits, and a
 * unit test cannot run the engine. So this class pins two things: (1) what the filter admits, applied the way the
 * engine applies it, nearest first; (2) that every selection site actually passes this filter, as a structural scan.
 *
 * <p>Mutations, each restored byte-identical:
 * <ul>
 *   <li>{@code isLive} without {@code !living.isDead()}: reddens the dying-mob rows.</li>
 *   <li>{@code isLive} without {@code living.isValid()}: reddens {@link #aRemovedBodyIsNotATarget}.</li>
 *   <li>{@code castRay} with its old lambda restored: reddens {@link #everySelectionSiteUsesTheLiveFilter}.</li>
 * </ul>
 */
class LiveTargetsTest {

    private static final UUID CASTER = UUID.fromString("00000000-0000-0000-0000-00000000c0de");

    /** A stub of {@code type} whose only live answers are isDead, isValid, getUniqueId and identity. */
    private static <T extends Entity> T stub(Class<T> type, UUID id, boolean dead, boolean valid) {
        return type.cast(Proxy.newProxyInstance(LiveTargetsTest.class.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> switch (method.getName()) {
                    case "isDead" -> dead;
                    case "isValid" -> valid;
                    case "getUniqueId" -> id;
                    case "toString" -> type.getSimpleName() + (dead ? "(dying)" : valid ? "(live)" : "(removed)");
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == (args == null ? null : args[0]);
                    default -> method.getReturnType() == boolean.class ? false : null;
                }));
    }

    private static Zombie zombie(boolean dead, boolean valid) {
        return stub(Zombie.class, UUID.randomUUID(), dead, valid);
    }

    /** The engine's contract: the closest entity the filter admits. {@code byDistance} is nearest first. */
    private static Entity firstAdmitted(List<Entity> byDistance, Predicate<Entity> filter) {
        return byDistance.stream().filter(filter).findFirst().orElse(null);
    }

    @Test
    void aDyingMobInFrontOfALiveOneIsPassedOverAndTheShotReachesTheLiveOne() {
        Zombie dying = zombie(true, true);   // killed this tick; still in the world for its death animation
        Zombie live = zombie(false, true);

        assertSame(live, firstAdmitted(List.of(dying, live), LiveTargets.except(CASTER)),
                "the corpse at d=4 must not end the flight; the live zombie at d=8 is the target");
    }

    @Test
    void aDyingMobInFrontOfALiveOneIsPassedOverByTheLookRaysToo() {
        Zombie dying = zombie(true, true);
        Zombie live = zombie(false, true);

        assertSame(live, firstAdmitted(List.of(dying, live), LiveTargets::isLiveMob),
                "/rpg apply and /rpg mobdamage must land on the live mob, not WS2c's dying control (WS3's 81/100)");
    }

    @Test
    void aLiveMobIsATarget() {
        // THE CONTROL: without it, every row above passes against a filter that admits nothing.
        Zombie live = zombie(false, true);
        assertTrue(LiveTargets.isLive(live));
        assertTrue(LiveTargets.except(CASTER).test(live));
        assertTrue(LiveTargets.isLiveMob(live));
    }

    @Test
    void aRemovedBodyIsNotATarget() {
        Zombie removed = zombie(false, false);   // removed, but a stale reference still holds it
        assertFalse(LiveTargets.isLive(removed));
        assertFalse(LiveTargets.except(CASTER).test(removed));
    }

    @Test
    void theCasterIsNeverItsOwnTarget() {
        Player caster = stub(Player.class, CASTER, false, true);
        assertTrue(LiveTargets.isLive(caster), "a live player is a live body ...");
        assertFalse(LiveTargets.except(CASTER).test(caster), "... but never the caster's own hit");
    }

    @Test
    void theLookRaysSkipPlayers() {
        Player other = stub(Player.class, UUID.randomUUID(), false, true);
        assertFalse(LiveTargets.isLiveMob(other));
    }

    @Test
    void aNonLivingEntityIsNotATarget() {
        Item dropped = stub(Item.class, UUID.randomUUID(), false, true);
        assertFalse(LiveTargets.isLive(dropped));
        assertFalse(LiveTargets.except(CASTER).test(dropped));
    }

    // --- the wiring: every selection site passes this filter ---

    private static final Path WORLD = Path.of("src/main/java/io/github/butterflysmp/rpg/paper/adapter/PaperCombatWorld.java");
    private static final Path COMMAND = Path.of("src/main/java/io/github/butterflysmp/rpg/paper/command/RpgCommand.java");

    /** The body of the member whose declaration contains {@code signature}, up to the next member's {@code @Override}. */
    private static String member(String source, String signature) {
        int from = source.indexOf(signature);
        assertTrue(from >= 0, "the declaration '" + signature + "' moved; this scan needs its new name");
        int to = source.indexOf("@Override", from + signature.length());
        assertTrue(to > from + signature.length(), "no following member to bound '" + signature + "' by");
        return source.substring(from, to);
    }

    @Test
    void everySelectionSiteUsesTheLiveFilter() throws IOException {
        String world = Files.readString(WORLD, StandardCharsets.UTF_8);
        assertTrue(member(world, "public Optional<RayHit> castRay(").contains("LiveTargets.except(ignoreId)"),
                "castRay's entity filter must be LiveTargets.except: a dying body ends the flight otherwise");
        assertTrue(member(world, "public Collection<Combatant> combatantsNear(").contains("LiveTargets::isLive"),
                "combatantsNear feeds melee, the dash's swept line, bursts and homing: live bodies only");

        String command = Files.readString(COMMAND, StandardCharsets.UTF_8);
        int sites = command.split("rayTraceEntities\\(", -1).length - 1;
        int filtered = command.split("LiveTargets::isLiveMob\\);", -1).length - 1;
        assertEquals(3, sites, "the dev look-rays are /rpg mobdamage+mobheal, /rpg mobinfo and /rpg apply");
        assertEquals(sites, filtered, "every look-ray passes LiveTargets::isLiveMob");
    }
}
