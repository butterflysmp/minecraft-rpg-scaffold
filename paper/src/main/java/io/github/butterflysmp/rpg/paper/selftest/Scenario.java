package io.github.butterflysmp.rpg.paper.selftest;

import java.util.List;

/**
 * One gate row's scenario: the steps the instrument drives, keyed by the gate's OWN row id
 * ({@code GATE-withered-shortbow.md}'s {@code ### WB2} is {@code withered-shortbow}/{@code WB2}).
 *
 * <p>A {@code record} is a class whose fields are fixed at construction and which gets {@code equals},
 * {@code hashCode} and accessors ({@code gate()}, {@code row()}, {@code steps()}) for free.
 *
 * <p>{@link #SETUP} is not a row: it is the gate file's {@code setup:} block, run once before that gate's first row.
 */
public record Scenario(String gate, String row, List<Step> steps) {

    public static final String SETUP = "setup";

    public Scenario {
        steps = List.copyOf(steps);
    }

    /** Where the scenario is written, as the START line names it: {@code selftest/GATE-level.yml#WD1}. */
    public String source() {
        return "selftest/GATE-" + gate + ".yml#" + row;
    }

    /** One step. {@code arg} is the YAML scalar as written; each {@link Verb} says what it means. */
    public record Step(Verb verb, String arg) {}

    /**
     * The fixed vocabulary. Each verb is implemented ONCE, in {@link SelfTestDrivers}, and the drivers are the only
     * code that reaches a shipped path. A row is added in YAML; a verb is added in Java.
     */
    public enum Verb {
        /** {@code Player#performCommand}: the server's one Brigadier dispatcher, as typed. Logged as a CMD line. */
        CMD,
        /** Real ticks on the player's own scheduler. The only way a scenario waits. */
        WAIT,
        /** Turn {@code /rpg mobtrace} ON (it has no setter; the toggle is read back). */
        TRACE,
        /** {@code Player#attack} on the nearest entity carrying this scoreboard tag. */
        ATTACK,
        /**
         * Turn the player's EYE to the eye of the nearest living entity carrying this tag ({@link SelfTestAim}).
         * Not {@code tp … facing entity … eyes}: a player's command source anchors at the FEET, so that aims 1.62 high.
         */
        AIM,
        /** A synthetic {@code PlayerInteractEvent RIGHT_CLICK_AIR, HAND} into every listener, ours included. */
        USE,
        /** A synthetic {@code PlayerArmSwingEvent(HAND)}: the stone's Left. */
        SWING,
        /** {@code HumanEntity#dropItem(false)}: the Q key's own {@code ServerPlayer.drop}. */
        DROP,
        /** Select a hotbar slot holding this ({@code stone}, {@code empty} or a weapon id), firing {@code PlayerItemHeldEvent}. */
        HOLD,
        /** A synthetic left {@code InventoryClickEvent} on the open menu's first slot whose name starts with this text. */
        CLICK,
        /** The same, on the first slot of this material. */
        CLICK_MATERIAL,
        /**
         * {@code "<top|bottom> <weapon id>"}: a synthetic {@code InventoryClickEvent SHIFT_LEFT MOVE_TO_OTHER_INVENTORY}
         * on the first slot of that half of the open view holding that weapon. {@code MenuRouting} PERFORMS a shift-move
         * itself ({@code shiftMove}: clear the source, write the destination), so this moves the item for real, on the
         * shipped path; a plain left place is only PERMITTED and would be applied by nothing. GATE-nexus-back.md: an
         * item into a menu's input slot, and back out of a vault cell.
         */
        SHIFT_CLICK,
        /** {@code Player#closeInventory}. */
        CLOSE,
        /** Log the open screen ({@code menu}) or the held item ({@code held}): names, lore, colours as the server built them. */
        DUMP,
        /** Log every entity within 16 blocks carrying this tag: uuid, type, custom HP, fire ticks, distance. */
        PROBE,
        /** Log the inventory slots holding this weapon id. */
        PROBE_INVENTORY,
        /** {@code "<MATERIAL> dx1 dy1 dz1 dx2 dy2 dz2"}: count that block in a box relative to the player's block. */
        PROBE_BLOCKS,
        /** {@code "<entity type> <radius>"}: count those entities near the player. */
        PROBE_ENTITIES,
        /**
         * {@code "<entity type> <radius>"}: log each of those entities' position RELATIVE TO THE PLAYER'S FEET, so a
         * sample taken each tick shows where a body was last seen (GATE-legacy-b.md LB5's impact witness).
         */
        PROBE_POSITIONS,
        /**
         * Log the PLAYER'S own state, which {@link #PROBE} cannot see ({@code getNearbyEntities} excludes the
         * player): custom HP, feet, on-ground, fall distance. GATE-sunder.md's fall-damage and landing rows read it.
         */
        PROBE_SELF,
        PROBE_BODY,
        /**
         * For THIS ROW ONLY, sets {@code keep_inventory} and {@code immediate_respawn} true, and lets the player die
         * without aborting the run: the run waits for the respawn instead. The values the row found go back at the
         * row's end, and the run's restore holds both as they were at RUN START. GATE-sunder.md SU8 (a death
         * mid-leap, the seat's ruling 2026-11-10) is its one user; the death itself is an ordinary {@code kill @s}
         * step, the server's own damage-to-death path.
         */
        DEATH_RULES,
        /** {@code "dx1 dy1 dz1 dx2 dy2 dz2"}: remember those blocks, so the run's restore puts them back. */
        SAVE_BLOCKS,
        /** {@code "<class> <element>"}: SKIPPED unless the profile's Build cell is this one. */
        EXPECT_CELL,
        /** {@code "<row>"}: SKIPPED unless that row ran, unskipped, immediately before this one in the same run. */
        AFTER,
        /**
         * {@code "dx dy dz FACE"}: turn the player's EYE to the middle of that face of the block at that offset from the
         * player's block ({@link SelfTestMiningAim#faceCentre}), by {@code teleportAsync}, as {@link #AIM} turns it.
         * GATE-mining-auto.md: the wall a slot is placed on (its NORTH face), a floor's UP face, a slot block outside
         * the patch. Wait 2 ticks after it before anything reads the aim.
         */
        AIM_BLOCK,
        /**
         * {@code "live" | "spot <i>" | "pixel <u> <v>" | "beside"}: turn the EYE to a point on the caller's patch at
         * the slot the eye already looks at. It READS the patch from {@code MiningService#peek} (the real book, read-only)
         * and logs what it read and where it aimed. While the slot is regrowing there is no patch: it logs that and
         * leaves the aim alone, so a following DIG reads NOT_LIVE.
         */
        AIM_PATCH,
        /**
         * One mining click, as the shipped path sees one: a {@code BlockDamageEvent} for the block the eye ray meets
         * within the player's block-interaction range, with its face and the MAIN-HAND item, into every listener
         * ({@code RpgListeners} hands it to {@code MiningService}); then {@code Player#resetCooldown}, the reset the
         * click's arm swing does on the server ({@code ServerPlayer.swing} → {@code resetAttackStrengthTicker}).
         * It SKIPS the dig packet, its reach check and the swing animation.
         */
        DIG,
        /** {@code "on"}: turn the player's {@code /rpg mining trace} ON through the shipped toggle, only if it is off. */
        MINING_TRACE,
        /**
         * {@code "<tag> <frozen|live> <mob> [stage <n>] [<gs>]"}: {@code /rpg spawn}'s own path at the player's feet,
         * with the row's tag, {@code t} and {@code selftest} added INSIDE the pre-spawn consumer, and {@code frozen}
         * switching the AI off there too ({@link SelfTestSpawn}). The mob is tagged by identity, so it is never in the
         * world untagged and never confused with the last row's (GATE-dragon-fix.md, D2). Logged as a DRIVE line
         * carrying its uuid; the command's own reply follows as a REPLY line.
         */
        SPAWN,
        /**
         * {@code "<n> <timeout ticks> <question>"}: show the numbered yes/no question in the player's chat, wait for
         * his next chat line or the timeout, and log question and answer verbatim on ONE line ({@link SelfTestAsk}).
         * It never interprets the answer and never judges it.
         */
        ASK,
        /**
         * {@code "<peaceful|easy|normal|hard>"}: the difficulty this gate's rows need, in its {@code setup:}. Logs
         * the difficulty it found and sets this one; the run's one restore puts back what it found at RUN START, on
         * every exit path (GATE-dragon-fix.md, C5). Wait 2 ticks after it: it is set on the global scheduler.
         */
        DIFFICULTY,
        /** Log, for each entity carrying this tag within 16 blocks, what its AI is targeting: {@code self}, none, or a type and uuid. */
        PROBE_TARGET,
        /**
         * Teleport the player back to where the run began (the row-start clean's spot), so a box probed relative to
         * the player's block is the box it was before he was pushed. Wait 2 ticks after it: the teleport is async.
         */
        RETURN,
        /** {@code <tag>}: blow the tagged creeper with {@code Creeper#explode()} (dragon-mobs R3; {@link SelfTestBlast}). */
        DETONATE,
        /** {@code <tag>}: {@code Creeper#ignite()} on the tagged creeper, flint and steel's route ({@link SelfTestBlast}). */
        IGNITE,
        /** {@code <tag>}: log each tagged creeper's FUSE line, its swell over its fuse ({@link SelfTestBlast}). */
        PROBE_FUSE,
        /** {@code "<tag> <min fuse> <timeout ticks>"}: wait until the tagged creeper's swell reaches min ({@link SelfTestBlast}). */
        AWAIT_FUSE,
        /**
         * {@code <tag>}: switch each tagged mob's AI back ON, the instrument's own thaw (the owed finding,
         * PLAN-dragon-mobs.md section 8 F4), in place of a vanilla {@code data merge ... {NoAI:0b}}. Logs
         * {@code DRIVE thaw tag=<t> <uuid> ai=<read back>} ({@link SelfTestBlast}).
         */
        THAW,
        /**
         * {@code <tag>}: log each tagged mob's VELOCITY, {@code MOTION tag=<t> <uuid> vx= vy= vz=}. A frozen mob cannot be
         * moved by a push, so a push on one shows here and never in its position ({@link SelfTestBlast}).
         */
        PROBE_MOTION,
        /**
         * {@code <tag>}: make each tagged beam mob within 48 blocks begin a beam NOW at its normal choice of target
         * (dragon-mobs step R5, the brief's ruling 8), as DETONATE does a blast. Logs
         * {@code DRIVE beam tag=<t> <uuid>} and, if it did not take, why ({@link SelfTestBeam}).
         */
        BEAM,
        /**
         * {@code <tag>}: log each tagged mob's feet, {@code AT tag=<t> <uuid> x= y= z=} to three places, so a row can
         * read whether it moved ({@link SelfTestBeam}).
         */
        PROBE_AT,
        /** A line for the reader. Changes nothing. */
        NOTE
    }
}
