package io.github.butterflysmp.rpg.paper.weapon;

import io.github.butterflysmp.rpg.core.weapon.QuiverState;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The safe way to ASK about a quiver must stay reachable, or callers go back to the one that WRITES.
 *
 * <h2>Why this exists rather than a comment</h2>
 *
 * <p>{@link Quivers#resolveForShot} commits: a matured reload refills the magazine, an unstamped item
 * is stamped and written back. That is correct -- the lazy design has no expiry event to miss
 * precisely because the read IS the commit point. But it was first called {@code refusalFor}, which
 * reads as a question, and <b>the next two pieces of work both want to ask that question</b>: the
 * quiver lore line, and the action-bar HUD. Either one calling the obvious-looking method would
 * silently finish reloads as a side effect of RENDERING, and the bug would present as reloads
 * completing early -- which looks like the timer working, not like a read doing a write.
 *
 * <p>{@link Quivers#stateOf} is the answer: pure, no {@code Player}, returns a {@link QuiverState} a
 * caller can interrogate freely. <b>The split only protects anything while that method is public and
 * obvious.</b> Made private in a tidy-up -- it was private until this commit -- and every caller is
 * pushed straight back onto the committing path, with nothing red.
 *
 * <p>So the condition is written where it will be seen, in a red build, on
 * {@code DamageSignatureTest}'s idiom and with its argument: <b>a safety that holds on a condition
 * nobody wrote down where it would be violated is not a safety.</b>
 *
 * <h2>What it does NOT claim</h2>
 *
 * <p>It cannot check that {@code stateOf} is pure, or that {@code resolveForShot}'s name is a good
 * one. Purity and naming are not reflectable. What is checkable is that the pure entry point still
 * EXISTS and is still PUBLIC, and that the committing surface has not quietly grown a new member
 * whose name invites the same mistake -- both of which are the mechanical half of the decision.
 */
class QuiversSignatureTest {

    @Test
    void thePureWayToAskAboutAQuiverIsPublicAndTakesNoPlayer() {
        Method stateOf = Arrays.stream(Quivers.class.getDeclaredMethods())
                .filter(m -> m.getName().equals("stateOf"))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Quivers.stateOf is GONE. It is the only side-effect-free way to ask about a "
                                + "quiver; without it a HUD or tooltip reaches for resolveForShot and "
                                + "commits reloads while rendering."));

        assertTrue(Modifier.isPublic(stateOf.getModifiers()),
                "stateOf must stay PUBLIC -- made private, every caller is pushed back onto the "
                        + "committing path and nothing goes red");
        assertTrue(Modifier.isStatic(stateOf.getModifiers()));
        assertEquals(QuiverState.class, stateOf.getReturnType(),
                "it must hand back the core value type, so the asking happens in core and not here");

        // "TAKES NO Player" IS A PROXY FOR PURITY, AND IT IS DOCUMENTED AS ONE RATHER THAN AS THE
        // RULE -- because this sentence is what a future reader reasons from when deciding whether
        // some NEW method qualifies, and the obvious reading of it is wrong.
        //
        // THE ACTUAL MECHANISM: writes in this class are `held.editMeta(...)` on an ItemStack --
        // the UNSTAMPED arm does exactly that -- followed by setItemInMainHand to persist the copy
        // back. Bukkit's getItemInMainHand hands out a COPY, so editing the meta alone changes
        // nothing the player can see; THE PLAYER IS THE HALF THAT PERSISTS. So a Player is not what
        // ENABLES a write, it is what makes a write STICK.
        //
        // The proxy holds because of that copy semantics, not because Player is the write capability.
        // A method taking only an ItemStack could still call editMeta and mutate a caller's stack --
        // so a future addition must be judged on whether it edits meta at all, and this check is the
        // cheap mechanical half, not the definition.
        for (Class<?> parameter : stateOf.getParameterTypes()) {
            assertTrue(!parameter.getName().endsWith(".Player"),
                    "stateOf must not take a Player. That is a PROXY for purity (see the comment "
                            + "above): writes here are editMeta + setItemInMainHand, and the Player "
                            + "is the half that persists them, so needing one marks the committing "
                            + "path -- it does not define it.");
        }
    }

    /**
     * TODAY'S PUBLIC SURFACE, named rather than counted.
     *
     * <p>Three of these four WRITE ({@code resolveForShot}, {@code spendRound}, {@code beginReload},
     * {@code tryReloadHeldWeapon} -- all but {@code stateOf}), and every one is verb-named so it
     * cannot be mistaken for a query. A new member added here is a deliberate edit to this list,
     * which is the moment to ask whether its name says what it does.
     *
     * <p>Pinned as a SET rather than a count, because a count over an unnamed set cannot be checked
     * by the reader -- which is the same rule this slice's reports are held to.
     */
    @Test
    void theCommittingSurfaceHasNotQuietlyGrown() {
        List<String> publicMethods = Arrays.stream(Quivers.class.getDeclaredMethods())
                .filter(m -> !m.isSynthetic() && Modifier.isPublic(m.getModifiers()))
                .map(Method::getName)
                .distinct()
                .sorted()
                .toList();

        assertEquals(
                List.of("beginReload", "resolveForShot", "spendRound", "stateOf", "tryReloadHeldWeapon"),
                publicMethods,
                "Quivers' public surface changed. Every member but stateOf commits, so a new one "
                        + "needs a verb name that says so -- and a new PURE one belongs beside "
                        + "stateOf in the javadoc that points callers at it.");
    }
}
