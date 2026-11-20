package io.github.butterflysmp.rpg.paper.health;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * BEN'S VOID RULING, THE PAPER HALF (2026-11-19; PLAN-world-damage.md). The handler needs a live player below a live
 * world's void, so its wiring is pinned from the source, comments stripped, each method bounded STRUCTURALLY (from its
 * signature to the next member's). The rule itself is core's ({@code VoidShareTest}: the share, maximum not current,
 * defence bypassed, the tenth tick).
 */
class VoidRuleSourceTest {

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
        for (String next : new String[] {"@EventHandler", "\n    public ", "\n    private ", "\n    void ", "\n    static "}) {
            int i = code.indexOf(next, from + signature.length());
            if (i >= 0 && i < to) to = i;
        }
        return code.substring(from, to);
    }

    private static String listeners() throws IOException {
        return code("listener/RpgListeners.java");
    }

    /** A PLAYER's void is taken before the PASS table is asked; a mob's still falls through to VOID's PASS. */
    @Test
    void aPlayersVoidIsTakenBeforeThePassTable() throws IOException {
        String env = body(listeners(), "public void onEnvironmentalDamage(EntityDamageEvent event)");
        int tracked = env.indexOf("if (!adapters.stats().tracks(id)) return;");
        int arm = env.indexOf("if (event.getCause() == EntityDamageEvent.DamageCause.VOID && target instanceof Player player\n"
                + "                && voidShare.isPresent()) {\n            onPlayerVoidTick(event, player);\n            return;");
        int pass = env.indexOf("if (VanillaDamagePolicy.forCause(event.getCause()) == VanillaDamagePolicy.Action.PASS) return;");
        assertTrue(tracked > 0 && arm > tracked && pass > arm, "tracked, then the void arm, then the PASS table");
    }

    /** One claim on the shared window, the token, then the store: his MAXIMUM, then his current, and the hit's own defence. */
    @Test
    void theTickClaimsTokensAndAppliesTheSharesHit() throws IOException {
        String tick = body(listeners(), "private void onPlayerVoidTick(EntityDamageEvent event, Player player)");
        int claim = tick.indexOf("double toDeal = damageWindow.claim(id, event.getDamage(), false);");
        int token = tick.indexOf("event.setDamage(TOKEN_DAMAGE);");
        int absorbed = tick.indexOf("if (toDeal <= 0.0) return;");
        int share = tick.indexOf("VoidShare.hitFor(\n                adapters.stats().max(id), adapters.stats().current(id),"
                + " voidShare.getAsDouble());");
        int mark = tick.indexOf("voidHits.mark(id, Bukkit.getCurrentTick());");
        int apply = tick.indexOf(".applyDamage(hit.amount(), id,\n"
                + "                io.github.butterflysmp.rpg.core.combat.CritState.NORMAL, hit.defense());");
        assertTrue(claim > 0 && token > claim && absorbed > token && share > absorbed && mark > share && apply > mark,
                "claim, token, absorbed, the share (max then current), the mark, the apply: " + tick);
    }

    /** Bound in the plugin from the loaded content, the same VoidHits handed to the selftest's HURT line. */
    @Test
    void theRuleIsBoundFromContent() throws IOException {
        String plugin = code("RpgPlugin.java");
        assertTrue(plugin.contains(".load(new File(contentDir, \"world_damage.yml\"));"));
        int bind = plugin.indexOf("listeners.bindVoidRule(worldDamage.voidShare(), voidHits);");
        int hurt = plugin.indexOf("selfTest.bindVoidHits(voidHits);");
        int reg = plugin.indexOf("getServer().getPluginManager().registerEvents(listeners, this);");
        assertTrue(bind > 0 && hurt > bind && reg > hurt, "bound before the one registration");
    }
}
