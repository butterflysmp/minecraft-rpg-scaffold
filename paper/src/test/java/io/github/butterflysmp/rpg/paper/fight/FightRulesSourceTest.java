package io.github.butterflysmp.rpg.paper.fight;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * F9b, THE FIGHT'S RULES after the dragon boot: the wiring sits behind a real dragon, a real crystal and real events,
 * so it is pinned from the source, comments stripped, each method bounded STRUCTURALLY (from its signature to the next
 * member's). The pure halves are core's: {@code DragonCrossingsTest} (immune while owed), {@code CrystalHealLogTest},
 * {@code DragonAttacksTest}.
 */
class FightRulesSourceTest {

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
                "\n    static ", "\n    String ", "\n    UUID ", "\n    FightInstance("}) {
            int i = code.indexOf(next, from + signature.length());
            if (i >= 0 && i < to) to = i;
        }
        return code.substring(from, to);
    }

    private static String instance() throws IOException {
        return code("fight/FightInstance.java");
    }

    private static String listeners() throws IOException {
        return code("listener/RpgListeners.java");
    }

    // --- section 2: no crystal heals the fight's dragon ---------------------------------------------------------------

    /** The crystal check runs BEFORE M15's reroute, and a refusal cancels the event (which stops vanilla's setHealth). */
    @Test
    void aCrystalHealOnAFightsDragonIsCancelledBeforeTheReroute() throws IOException {
        String h = body(listeners(), "private void healTrackedMob(");
        int check = h.indexOf("event.getRegainReason() == EntityRegainHealthEvent.RegainReason.ENDER_CRYSTAL");
        int refuse = h.indexOf("fights.refusesCrystalHeal(id, event.getAmount())");
        int cancel = h.indexOf("event.setCancelled(true);");
        int reroute = h.indexOf("MobHealPolicy.forReason(");
        assertTrue(check > 0 && refuse > check && cancel > refuse && reroute > cancel, h);
    }

    /** Only THIS fight's dragon: another mob's heal is untouched. */
    @Test
    void theHealRefusalIsForTheFightsOwnDragonOnly() throws IOException {
        String r = body(instance(), "public boolean refusesCrystalHeal(");
        int check = r.indexOf("if (dragon == null || !healed.equals(dragon.getUniqueId())) return false;");
        int yes = r.indexOf("return true;");
        assertTrue(check > 0 && yes > check, r);
        assertTrue(r.contains("healLog.shouldLog(lit)"), "logged once per crystal lit: " + r);
    }

    // --- section 4: the dragon's damage --------------------------------------------------------------------------------

    /** The mob rider prices a fight dragon's hit from content and every other mob's from its seeded stat. */
    @Test
    void theMobRiderAsksTheFightsForADragonsHitFirst() throws IOException {
        String m = body(listeners(), "public void onMobMeleeAttack(");
        assertTrue(m.contains("fights.dragonMelee(attacker, victim, event.getDamage());"), m);
        assertTrue(m.contains("dragonBlow.ours() ? dragonBlow.amount()"), m);
        assertTrue(m.contains(": adapters.stats().attackValue(attacker.getUniqueId());"), "every other mob as before");
    }

    /**
     * F9b fix A: a contact hit inside the window is CANCELLED (no price, no vanilla damage) before anything else the rider
     * does, and the window is asked before the dragon's boxes are read.
     */
    @Test
    void aContactHitInsideTheWindowIsCancelledAndPricesNothing() throws IOException {
        String m = body(listeners(), "public void onMobMeleeAttack(");
        int ask = m.indexOf("fights.dragonMelee(attacker, victim, event.getDamage());");
        int cancel = m.indexOf("if (dragonBlow.ours() && !dragonBlow.lands()) {\n            event.setCancelled(true);\n"
                + "            return;");
        int shield = m.indexOf("ShieldBlock.resolve(");
        assertTrue(ask > 0 && cancel > ask && shield > cancel, "cancelled before the block or the price: " + m);
        String melee = body(instance(), "public Contact dragonMelee(");
        int window = melee.indexOf("ContactHitWindow.Result w = contacts.hit(victim.getUniqueId(), fixture.fightTick());");
        int suppressed = melee.indexOf("if (!w.lands()) return Contact.SUPPRESSED;");
        int price = melee.indexOf("priceAttack(");
        assertTrue(window > 0 && suppressed > window && price > suppressed, melee);
        assertTrue(instance().contains("contacts = new ContactHitWindow(d.attacks().contactHitIntervalTicks());"),
                "the window's length from content");
    }

    /** The addendum: the name follows the part that DEALT it, vanilla's own amount first. */
    @Test
    void theAttacksNameIsReadFromVanillasAmountFirst() throws IOException {
        String melee = body(instance(), "public Contact dragonMelee(");
        assertTrue(melee.contains("DragonAttacks.name(vanillaAmount, difficulty, box(victim.getBoundingBox()), head, neck,"
                + " wings)"), melee);
        assertTrue(melee.contains("DragonAttacks.Difficulty.valueOf(world.getDifficulty().name())"), melee);
    }

    /**
     * F9b fix B: EVERY fight damage is applied only on a participant of that fight, asked WHEN IT IS APPLIED: the
     * dragon's contact hit and its breath, the ring's strike, and the powered kill.
     */
    @Test
    void everyFightDamageIsAppliedOnlyOnAParticipantOfThatFight() throws IOException {
        String m = body(listeners(), "public void onMobMeleeAttack(");
        assertTrue(m.contains(".applyDamageIf(\n                    () -> fights.dragonVictimInside(dragonId, victim),"), m);
        String b = body(listeners(), "public void onFightDragonBreath(");
        assertTrue(b.contains(".applyDamageIf(\n                () -> fights.dragonVictimInside(dragonId, victim),"), b);
        String strike = body(code("fight/BukkitFightPort.java"), "public void damage(UUID player, double amount)");
        assertTrue(strike.contains(".applyDamageIf(() -> site.takesPart(p),"), strike);
        assertTrue(!strike.contains(".applyDamage(amount"), "no unconditional strike left: " + strike);
        String kill = body(instance(), "private void killParticipants(RingFixture ring)");
        assertTrue(kill.contains(".applyDamageIf(() -> site.takesPart(player),"), kill);
        String inside = body(code("fight/FightFixtures.java"), "public boolean dragonVictimInside(");
        assertTrue(inside.contains("return f.running() && f.holds(victim);"), inside);
        assertTrue(body(instance(), "public boolean holds(Player victim)").contains("return site.takesPart(victim);"));
    }

    /** ...and the Paper adapter asks the condition INSIDE its deferral, applying in the same task. */
    @Test
    void theAdapterAsksTheConditionInsideItsDeferral() throws IOException {
        String a = body(code("adapter/BukkitCombatant.java"), "@Override public void applyDamageIf(");
        int defer = a.indexOf("ctx.scheduler().onEntity(entity, () -> {");
        int ask = a.indexOf("if (!applies.getAsBoolean()) {");
        int apply = a.indexOf("applyNow(null, amount, sourceId, crit, defense, null, HitAccrual.weapon());");
        assertTrue(defer > 0 && ask > defer && apply > ask, "the question and the damage in one task: " + a);
    }

    /** Each attack's content number is scaled by gear score as any mob's seed is, and named in its own line. */
    @Test
    void eachAttackIsScaledByGearScoreAsAnyMobsHitIs() throws IOException {
        String p = body(instance(), "private double priceAttack(");
        assertTrue(p.contains("io.github.butterflysmp.rpg.core.mob.MobScaling.attackDamage(base, true,"), p);
        assertTrue(p.contains("DRAGONATTACK t=%d attack=%s"), p);
        String melee = body(instance(), "public Contact dragonMelee(");
        assertTrue(melee.contains("fight.dragon().attacks().base(attack)"), "the bite or wing number, from content");
        String breath = body(instance(), "public java.util.OptionalDouble dragonBreath(");
        assertTrue(breath.contains("DragonAttacks.breathPerApplication(fight.dragon().attacks().breathPerSecond(),")
                && breath.contains("cloud.getReapplicationDelay()"), "30 x interval / 20: " + breath);
    }

    /** Only this fight's dragon is priced: the id check comes before any price. */
    @Test
    void onlyThisFightsDragonIsPriced() throws IOException {
        String melee = body(instance(), "public Contact dragonMelee(");
        assertTrue(melee.indexOf("!attacker.getUniqueId().equals(dragon.getUniqueId())")
                < melee.indexOf("contacts.hit("), "not ours: no window is touched either: " + melee);
        String breath = body(instance(), "public java.util.OptionalDouble dragonBreath(");
        assertTrue(breath.indexOf("!source.getUniqueId().equals(dragon.getUniqueId())")
                < breath.indexOf("priceAttack("), breath);
    }

    /**
     * The breath: a cloud's hit, its vanilla event CANCELLED (F9b fix F, Ben: "remove the knock back from it"; READ: the
     * knockback follows only a hurt that succeeded), the fight's price dealt through armour.
     */
    @Test
    void theBreathIsCancelledNotTokenedAndDealtThroughArmour() throws IOException {
        String b = body(listeners(), "public void onFightDragonBreath(");
        assertTrue(b.contains("event.getDamager() instanceof org.bukkit.entity.AreaEffectCloud cloud"), b);
        int cancel = b.indexOf("event.setCancelled(true);");
        int deal = b.indexOf("each, dragonId, io.github.butterflysmp.rpg.core.combat.CritState.NORMAL,\n"
                + "                io.github.butterflysmp.rpg.core.combat.DefenseRule.APPLIES);");
        assertTrue(cancel > 0 && deal > cancel, "cancelled, then dealt through armour: " + b);
        assertTrue(!b.contains("event.setDamage(TOKEN_DAMAGE);"), "not tokened: a token is a hurt, and a hurt pushes");
        String m = body(listeners(), "public void onMobMeleeAttack(");
        assertTrue(m.contains("event.setDamage(TOKEN_DAMAGE);"), "the bite and the wing keep vanilla's push");
    }

    /** F9b fix D: the instruments are wired: the touch's boxes, the breath's apply, every cast in a fight world. */
    @Test
    void theInstrumentsAreWired() throws IOException {
        assertTrue(body(instance(), "public String dragonTouch(").contains("attack_box=%s inside=%s"));
        assertTrue(body(listeners(), "public void onFightBreathApply(")
                .contains("fights.onCloudApply(event.getEntity(), event.getAffectedEntities());"));
        String cast = listeners();
        int attempt = cast.indexOf("java.util.Optional<CastResult> attempted = WeaponFire.attempt(event.getPlayer(),"
                + " \"right_click\"");
        int note = cast.indexOf("fights.noteCast(event.getPlayer(), \"right_click\", attempted.orElse(null));");
        assertTrue(attempt > 0 && note > attempt, "every right-click's result is offered to the fights");
        String n = body(code("fight/FightFixtures.java"), "public void noteCast(");
        assertTrue(n.contains("if (!FightWorlds.isFightWorld(p.getWorld())) return;") && n.contains("dealt=nothing"), n);
    }

    /** The HURT line names the attack through the late-bound lookup, bound in the plugin. */
    @Test
    void theHurtLineNamesTheAttack() throws IOException {
        // Widened 2026-11-19 by Ben's void ruling: the line also names its cause (cause=void). Both arguments, each
        // anchored at its own terminator so a further widening cannot slide past either needle.
        String run = code("selftest/SelfTestRun.java");
        assertTrue(run.contains("dealer, type, service.attackNameFor(player.getUniqueId(), change.dealer()),"));
        assertTrue(run.contains("service.hurtCauseFor(player.getUniqueId())));"));
        assertTrue(code("RpgPlugin.java").contains("selfTest.bindAttackNames(fightFixtures::attackNameFor);"));
    }

    // --- section 6: the landing's lightning ----------------------------------------------------------------------------

    /** On the landing, behind the switch; an EFFECT only (never the damaging strike). */
    @Test
    void theLandingStrikesAnEffectOnlyBoltBehindItsSwitch() throws IOException {
        String land = body(instance(), "public String land(double dxBlocks, double dzBlocks)");
        int spawn = land.indexOf("spawnMage(");
        int bolt = land.indexOf("if (fight.mageLandingLightning()) landingLightning(");
        assertTrue(spawn > 0 && bolt > spawn, "the bolt on the landing tick, after the spawn: " + land);
        String l = body(instance(), "private void landingLightning(");
        assertTrue(l.contains("world.strikeLightningEffect(at);"), l);
        assertFalse(instance().contains("strikeLightning(") , "no damaging strike anywhere in the fight");
        assertTrue(l.contains("FIGHT lightning t=%d"), "logged");
    }

    // --- section 7: the instruments ------------------------------------------------------------------------------------

    /** The dragon's vanilla hit on its own Mage is cancelled, so vanilla's record never credits the dragon. */
    @Test
    void theDragonsVanillaHitOnItsOwnMageIsCancelled() throws IOException {
        String h = body(listeners(), "public void onFightDragonHitsItsMage(");
        assertTrue(h.contains("fights.refusesDragonOnMage(event.getDamager(), event.getEntity())")
                && h.contains("event.setCancelled(true);"), h);
        String r = body(instance(), "public boolean refusesDragonOnMage(");
        assertTrue(r.contains("!damager.getUniqueId().equals(dragon.getUniqueId())")
                && r.contains("!victim.getUniqueId().equals(mage.getUniqueId())"), "this fight's dragon on its Mage: " + r);
    }

    /** The fireball probe runs on the fight's loop with the dragon, so the play rows read it. */
    @Test
    void theFireballProbeRunsEveryLoopTickWithTheDragon() throws IOException {
        String i = instance();
        assertTrue(i.contains("healLog.crystalsNow(ring.crystalsLit());\n                noteFireballs();"), "in the loop");
        assertTrue(body(i, "private void noteFireballs()").contains("FIGHT fireballs t="), "logged on change");
    }
}
