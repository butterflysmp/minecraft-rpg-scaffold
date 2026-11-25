package io.github.butterflysmp.rpg.paper.fight;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * F9, THE DRAGON's wiring sits behind a real dragon and real events, so no unit test runs it. This pins it from the
 * source, comments stripped first, each method bounded STRUCTURALLY (from its signature to the next member's), never by
 * a line count. The pure halves are core's ({@code DragonCrossingsTest}, {@code RingFixtureTest}) and
 * {@code LiveTargetsTest} (a part resolves to its dragon).
 */
class FightDragonSourceTest {

    private static final Path PAPER = Path.of("src/main/java/io/github/butterflysmp/rpg/paper");

    private static String code(String file) throws IOException {
        String src = Files.readString(PAPER.resolve(file)).replace("\r\n", "\n");
        src = src.replaceAll("(?s)/\\*.*?\\*/", "");
        return src.replaceAll("//[^\\n]*", "");
    }

    private static String body(String code, String signature) {
        int from = code.indexOf(signature);
        assertTrue(from >= 0, "found: " + signature);
        int to = code.length();
        for (String next : new String[] {"@Override", "@EventHandler", "\n    public ", "\n    private ", "\n    void ",
                "\n    static ", "\n    FightInstance("}) {
            int i = code.indexOf(next, from + signature.length());
            if (i >= 0 && i < to) to = i;
        }
        return code.substring(from, to);
    }

    private static String instance() throws IOException {
        return code("fight/FightInstance.java");
    }

    /** The fireball cancel is scoped to THIS fight's dragon: the id check comes before the cancel. */
    @Test
    void theFireballCancelIsForTheFightsOwnDragonOnly() throws IOException {
        String b = body(instance(), "public boolean onFireball(");
        int check = b.indexOf("!event.getEntity().getUniqueId().equals(dragon.getUniqueId())) return false;");
        int cancel = b.indexOf("event.setCancelled(true);");
        assertTrue(check > 0 && cancel > check, "another dragon's fireball is never cancelled: " + b);
        assertTrue(b.contains("FIGHT fireball-cancelled"), "and each cancel is logged");
        assertTrue(body(code("listener/RpgListeners.java"), "public void onFightDragonFireball(")
                .contains("fights.onFireball(event);"), "the one listener hands every fireball to the fights");
    }

    /** The dragon is removed at EVERY ending: the one cleanup every ending shares removes it. */
    @Test
    void theDragonIsRemovedAtEveryEnding() throws IOException {
        String i = instance();
        assertTrue(body(i, "private void finish(String why)").contains("removeDragon(why);"),
                "finish is the one ending every route shares");
        String r = body(i, "private void removeDragon(String why)");
        assertTrue(r.contains("dragon.remove();") && r.contains("adapters.gate().release(dragon.getUniqueId());")
                && r.contains("hideBar(v, \"fight-ended\")"), "removed, released and its bar hidden: " + r);
    }

    /** The win: drops and experience suppressed, the ring stopped, the return scheduled win_return_ticks later. */
    @Test
    void theDragonsDeathIsTheWinAndTheReturnFollowsAfterTheContentsTicks() throws IOException {
        String w = body(instance(), "private void onDragonDeath(EntityDeathEvent event)");
        for (String needle : new String[] {"event.setDroppedExp(0);", "event.getDrops().clear();", "fixture.stop();",
                "port.tell(", "finish(\"win\");", "fight.dragon().winReturnTicks()"}) {
            assertTrue(w.contains(needle), "the win " + needle + ": " + w);
        }
        assertTrue(w.indexOf("onRegionLater(") < w.indexOf("finish(\"win\");"), "the end is SCHEDULED, not immediate");
        assertTrue(body(instance(), "public void onDeath(EntityDeathEvent event)").contains("onDragonDeath(event);"));
    }

    /**
     * IMMUNE while a Mage is alive OR OWED (F9b, section 1; Ben: "No it can't be hurt while a mage is owed"): the gate
     * asks core's one rule, {@code DragonCrossings.refusal}, and names its reason in the refused line.
     */
    @Test
    void theDragonsGateRuleIsNoMageAliveAndNoneOwed() throws IOException {
        // Bounded by the NEXT METHOD, not by body(): the rule is an anonymous class whose @Override would end body() early.
        String i = instance();
        int from = i.indexOf("private void spawnDragon()");
        int to = i.indexOf("private void trackBossBar()");
        assertTrue(from > 0 && to > from, "spawnDragon, then trackBossBar");
        String s = i.substring(from, to);
        assertTrue(s.contains("return queue.refusal(ring.mageAlive()) == DragonCrossings.Refusal.NONE;"), s);
        assertTrue(s.contains("ring.dragonRefused(amount, source, queue.refusal(ring.mageAlive()).logName());"),
                "its own refused line with the reason, the Mage's cue");
        assertFalse(s.contains("return !ring.mageAlive();"), "the F9 rule (alive only) is gone");
        assertTrue(s.contains("e.setPersistent(false);") && s.contains("e.setPodium(podium);"),
                "not persisted, its podium the centre floor");
        assertTrue(s.contains("dragon.setPhase(org.bukkit.entity.EnderDragon.Phase.CIRCLING);"),
                "told to fly: a new dragon HOVERS, and aiStep moves nothing while it does (READ)");
    }

    /** The queue releases a Mage only when IDLE with no Mage alive, and pays the crossing only once he has landed. */
    @Test
    void aQueuedMageLandsOnlyWhenIdleAndIsPaidOnlyOnceLanded() throws IOException {
        String q = body(instance(), "private void releaseQueuedMage(RingFixture ring)");
        assertTrue(q.contains("crossings.due(ring.phase() == MageCycle.Phase.IDLE && !ring.mageAlive())"), q);
        int land = q.indexOf("landRandom(0.0);");
        int paid = q.indexOf("crossings.landed();");
        assertTrue(land > 0 && paid > land && q.contains("if (ring.phase() == MageCycle.Phase.FAST_CLOSE)"),
                "paid only after a landing that began the fast close: " + q);
    }

    /**
     * VANILLA'S PER-PART SCALING IS NEVER APPLIED TO OUR NUMBER (the seat: our store's number is the number). READ, the
     * pinned jar: a hit on any part but the head is scaled to amount/4 + min(amount, 1) before the damage event. The
     * melee rider TOKENS that amount and lands the weapon's own figure, so the scaling touches only the token: the
     * rider must never read the event's amount.
     */
    @Test
    void theMeleeRiderTokensVanillasAmountAndNeverReadsIt() throws IOException {
        String m = body(code("listener/RpgListeners.java"), "public void onPlayerMeleeAttack(EntityDamageByEntityEvent event)");
        int token = m.indexOf("event.setDamage(TOKEN_DAMAGE);");
        int land = m.indexOf("WeaponFire.landVanillaMelee(");
        assertTrue(token > 0 && land > token, "tokened, then our own figure landed: " + m);
        assertTrue(!m.contains("event.getDamage()") && !m.contains("event.getFinalDamage()"),
                "vanilla's (scaled) amount is never read into ours: " + m);
    }

    /** Every landed hit is logged from the store's one seam, and the tap sits before mobDeath in the composite. */
    @Test
    void theLandedHitLineComesFromTheStoresSeamBeforeTheDeath() throws IOException {
        String plugin = code("RpgPlugin.java");
        int tap = plugin.indexOf("selfTestDamage, fightHits, mobDeath, summonFeeds));");   // the summon feed (4b) appended after
        assertTrue(tap > 0, "the tap is in the composite, before mobDeath");
        assertTrue(plugin.contains("fightHits.bind(fightFixtures::onHealthChange);"), "and bound to the fights");
        String h = body(instance(), "public void onHealthChange(");
        assertTrue(h.contains("FIGHTHIT t=%d target=%s") && h.contains("crossings.observe("), h);
    }

    /**
     * F9b, section 7: a landed hit on the DRAGON names its vanilla phase at the hit (DB1 could not say whether a hit
     * landed in the air or perched). A hit on the Mage carries no phase: it is not the dragon's.
     */
    @Test
    void aLandedHitOnTheDragonNamesItsPhase() throws IOException {
        String h = body(instance(), "public void onHealthChange(");
        assertTrue(h.contains("target.equals(\"dragon\") ? \" phase=\" + dragon.getPhase() : \"\""), h);
    }
}
