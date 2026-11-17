package io.github.butterflysmp.rpg.paper.scheduler;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * THE SCHEDULER A PLUGIN DISABLE CAN USE (GATE-dragon-knell-4.md, the selftest's server-stop restore). During
 * onDisable the plugin already reads as disabled, so its real scheduler refuses new tasks. What must happen NOW runs
 * now, on the calling (main) thread; what was asked for LATER, or off-thread, cannot happen at all, and is named
 * rather than silently dropped.
 */
class InlineSchedulerTest {

    @Test
    void entityRegionAndGlobalTasksRunAtOnceInOrder() {
        List<String> ran = new ArrayList<>();
        List<String> refused = new ArrayList<>();
        InlineScheduler s = new InlineScheduler(refused::add);
        s.onEntity(null, () -> ran.add("entity"));
        s.onRegion(null, () -> ran.add("region"));
        s.onGlobal(() -> ran.add("global"));
        assertEquals(List.of("entity", "region", "global"), ran);
        assertEquals(List.of(), refused);
    }

    @Test
    void laterAndAsyncTasksAreRefusedByNameAndNeverRun() {
        List<String> ran = new ArrayList<>();
        List<String> refused = new ArrayList<>();
        InlineScheduler s = new InlineScheduler(refused::add);
        s.onEntityLater(null, () -> ran.add("entity-later"), 5);
        s.onRegionLater(null, () -> ran.add("region-later"), 7);
        s.async(() -> ran.add("async"));
        assertEquals(List.of(), ran);
        assertEquals(List.of("onEntityLater 5", "onRegionLater 7", "async"), refused);
    }
}
