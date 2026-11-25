package io.github.butterflysmp.rpg.paper.selftest;

/**
 * WHO HOLDS THE PAD (P10(c) and its extra guard, the seat's ruling of 2026-11-24). A plugin chunk ticket is one per chunk
 * and plugin, NOT counted (READ by javap: {@code TicketStorage.addPluginRegionTicket} adds a {@code PLUGIN_TICKET} keyed
 * by the plugin), so a pad command releasing its tickets would drop a run's. Hence: no run while a pad command is in
 * flight, no pad command while a run is active, and neither twice. Pure; {@code SelfTestService} asks it.
 */
final class SelfTestOccupancy {

    static final String RUN_ALREADY = "Refusing: a selftest is already running. /rpg selftest stop ends it.";
    static final String RUN_BUSY = "Refusing: a selftest is running; the pad is its own while it does.";
    static final String PAD_BUSY = "Refusing: the pad command is still working; try again when it reports.";

    private boolean run;
    private boolean pad;

    /** Null when a run may start; else the refusal. */
    String refuseRun() {
        if (run) return RUN_ALREADY;
        if (pad) return PAD_BUSY;
        return null;
    }

    /** Null when the pad command may start; else the refusal. */
    String refusePad() {
        if (run) return RUN_BUSY;
        if (pad) return PAD_BUSY;
        return null;
    }

    void runStarted() { run = true; }
    void runEnded() { run = false; }
    void padStarted() { pad = true; }
    void padEnded() { pad = false; }
}
