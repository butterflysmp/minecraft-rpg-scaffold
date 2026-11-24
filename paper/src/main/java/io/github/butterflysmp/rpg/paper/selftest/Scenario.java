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
        /**
         * A weapon id: leave ONLY that weapon in the hotbar, held. Every other hotbar item is MOVED into an empty slot of
         * the main inventory (9-35), never dropped or destroyed; the weapon is found anywhere in the inventory, put in
         * hotbar slot 0 and selected through {@code PlayerItemHeldEvent}, as {@link #HOLD} does. With too few empty
         * slots the row is SKIPPED by name. The gate-boundary restore and the run's restore put the whole inventory
         * back. Added 2026-11-22 (the seat's brief, PR 1): {@code GATE-knell-beam.md} HF1R2 sampled the Knell Beam
         * from slot 1 when the row meant the staff in slot 0.
         */
        HOLD_ONLY,
        /** Log the held slot and the held item's weapon id, {@code HELDID slot=<n> item=<id>}: cheap, for a long wait's ends. */
        PROBE_HELD,
        /**
         * One LEFT-CLICK as the shipped left-click handler takes it: {@code WeaponSwingListener.swing}, the same method the
         * {@code ANIMATION} packet hops to, on the player's own thread. So a quiver weapon RELOADS (or refuses, with its
         * notice) on the shipped path. It SKIPS the packet, its main-hand check and the arm-swing animation (an AUTO
         * reading of the server, never the client). Logged {@code DRIVE reload}. Added 2026-11-22 (the Breacher's brief,
         * PR 2): a reload was only Ben's input before (GATE-deft-hands.md DH2, DH4).
         */
        RELOAD,
        /**
         * {@code <material>}: count that vanilla item in the player's STORAGE slots (the slots a reload draws from,
         * {@code getStorageContents()}), {@code ITEMS material=<m> count=<n> slots=<i:n,...>}. Gear of that material is
         * counted and marked {@code gear=<n>}. Added 2026-11-22 (the Breacher's brief, PR 2): what a reload took.
         */
        PROBE_ITEMS,
        /**
         * Log the Nexus item, found by its TAG anywhere in his inventory: {@code NEXUS slot=<i> material=<m> amount=<n>},
         * or {@code NEXUS none}. The star or its quiver MIRROR (2026-11-22, GATE-vanilla-bows): which, and how many.
         */
        PROBE_NEXUS,
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
        /**
         * {@code "<tag>"} or {@code "<tag> <reach>"}: log every entity within the reach (16 when none is written; 1-64)
         * carrying this tag: uuid, type, custom HP, fire ticks, distance. The reach is the scenario's to state since
         * 2026-11-23 (the seat's rule 4): EC8's F stood 30 out and a reach of 16 could never read it.
         */
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
        /**
         * Log the PLAYER'S mana, {@code MANA current=<c> max=<m>}, from the pool every cast spends from. Added
         * 2026-11-21 (the seat's brief, PR 1): a cast's price is READ from two of these, not inferred from source.
         */
        PROBE_MANA,
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
        /**
         * {@code "<tag> <right|left> <degrees>"}: fix the side and the first angle of each tagged Knell's NEXT teleport
         * decision (dragon-mobs step R6, the brief's ruling 8), so a row predicts exact coordinates. Logs
         * {@code DRIVE teleport_draw tag=<t> <uuid> side=<s> first=<a>} ({@link SelfTestBeam}).
         */
        TELEPORT_DRAW,
        /**
         * {@code <tag>}: make each tagged Phalanx within 48 blocks FIRE its shot on its next tick, frozen or not
         * (dragon-mobs step R7a, the seat's ruling 10). Logs {@code DRIVE shot tag=<t> <uuid>}, or why it did not take;
         * the shot's own {@code [Rpg] SHOT fired ...} line follows from the Phalanx's loop.
         */
        SHOT,
        /**
         * {@code <tag>}: log each live shot of each tagged Phalanx within 48 blocks: its position, AGE in ticks, the
         * straight line from its origin ({@code flown}) and its velocity ({@code [Rpg]}-less {@code SHOT at ...}).
         */
        PROBE_SHOT,
        /** {@code on|off}: while on, log every arm swing, attack and interact of the player against the nearest live shot ({@link SelfTestStrike}). */
        WATCH_SWINGS,
        /** {@code <tag>}: turn the player's EYE to the centre of the VISIBLE shape riding the nearest carrier with this tag, as AIM does. */
        AIM_SHAPE,
        /** {@code <tag>}: ray-cast from the player's eye ALONG HIS LOOK, out to his entity interaction range, and {@code Player#attack} what it picks. */
        STRIKE_LOOK,
        /** {@code "<tag> <age> <timeout ticks>"}: wait until the tagged shot's age reaches {@code age}, then in that tick ray-cast from his eye toward its shape's centre and attack what it picks. */
        STRIKE_AT_AGE,
        /** {@code <tag>}: launch a real arrow, the player its shooter, from his eye toward the tagged shot's shape centre (speed 2.0 blocks a tick). */
        SHOOT_ARROW,
        /**
         * {@code "<tag> <angle> <distance>"}: each tagged Phalanx within 48 blocks BLINKS on its next tick, frozen or not,
         * every attempt this one draw (step R7b). Its own {@code [Rpg] BLINK ...} line is the reading.
         */
        BLINK,
        /** {@code <tag>}: log each tagged mob's one-hit shield, {@code SHIELD tag=<t> <uuid> up=<b> offhand=<item>} (step R7b). */
        PROBE_SHIELD,
        /**
         * {@code <tag>}: each tagged Skreel pair within 48 blocks (its mount or its rider carrying the tag) begins a WIND-UP
         * on its next tick, aimed at the player's FEET as they stand now: a fixed aim (dragon-mobs step R8). Logs
         * {@code DRIVE pounce tag=<t> <uuid> aim=<x y z>}, or why it did not take; the pair's own {@code SKREEL windup}
         * line follows from its loop.
         */
        POUNCE,
        /** {@code <tag>}: one {@code PAIR ...} line for each tagged mob: what it rides or carries, the cycle's stage, its speed (step R8). */
        PROBE_PAIR,
        /**
         * {@code <text>}: show the text to the player in GOLD chat, the ASK prompt's colour, and log
         * {@code TELL <text>}. It does not wait. A Ben row says what to do with it BEFORE the thing it asks about
         * (GATE-implosion-drag.md, 2026-11-18: watch that Phage; walk away from the next one).
         */
        TELL,
        /**
         * {@code <tag>}: LIGHT each tagged Phage's own fuse now, however far its target is, and keep it lit to the blast
         * (the instrument's forced fuse, {@code MobFuse#light}; GATE-implosion-drag.md's Part D row). Its START shows
         * as one {@code SWELL ... reason=light} line, its end as the usual {@code FUSED}; nothing about its distance
         * lights or resets it. Logs {@code DRIVE light tag=<t> <uuid>}, or why it did not take.
         */
        LIGHT,
        /**
         * {@code "on"}: turn the drag's per-tick instrument ON ({@code DragTrace}; GATE-drag-additive.md, Part D): one
         * {@code DRAGTICK} line per player victim per drag tick. The run's restore turns it OFF. Logs
         * {@code DRIVE drag_tick on=true}.
         */
        DRAG_TICK,
        /**
         * {@code "<ceiling ticks>"}: wait, a tick at a time, until the dev command's fight is no longer running, or the
         * ceiling; logs {@code AWAIT fight ended ticks=<n>} or {@code AWAIT fight ceiling ticks=<c> still running}
         * ({@link SelfTestAwaitFight}; GATE-fight-site.md).
         */
        AWAIT_FIGHT,
        /**
         * {@code ""}: EMPTY every storage slot (0-35) but the Nexus item's, so the row's gives land in his hotbar and
         * the row asks nothing of his inventory (the seat's rule 1, 2026-11-23: nine rows of the boot of e774d4b8
         * SKIPPED for want of an empty hotbar slot). Armour and the off hand are left alone: they are his gear, and a
         * damage sum's class bonus and his health read them. The snapshot holds his inventory from RUN START, and the
         * gate-boundary restore and the run's restore (every exit path, the disable's included) put it back. Logs
         * {@code STAGE clear_inventory cleared=<n> kept=<nexus slot|none>}.
         */
        CLEAR_INVENTORY,
        /**
         * {@code ""}: forget his per-player combat state, so a row presses from a CLEARED cooldown and a fresh record
         * (the seat's rule 2): every cooldown ({@code CooldownTracker#clear}, his quit's own call), his behaviour
         * enchants' hit streaks ({@code ShockwaveStreaks#forgetPlayer}, his quit's), and his Lapis Crystal orb, if one
         * stands ({@code OrbRemovals#remove}, cause {@code selftest}, which writes its own {@code ORB removed} line).
         * Logs {@code STAGE clear_state cooldowns=cleared streaks=forgotten orb=<removed|none>}.
         */
        CLEAR_STATE,
        /**
         * {@code "<east from> <south from> <east to> <south to>"}, whole blocks from the player's block, in a gate's
         * {@code setup:}: MEASURE the ground before any row runs ({@link SelfTestPreflight}): air at his feet and head
         * height, and a SOLID floor (not air, not water or any liquid, not waterlogged, not soft). A bad spot runs NO
         * row: he is told what is wrong and where, in two lines, the run ends at once and the snapshot restores. Logs
         * {@code PREFLIGHT ok …} or {@code PREFLIGHT fail …} with the counts. The seat's brief B, 2026-11-23.
         */
        PREFLIGHT,
        /**
         * {@code ""}, in a gate's {@code setup:}, before its preflight: THE PAD (Ben's ruling A, 2026-11-23, "The test
         * builds its own ground"). After the snapshot is taken, the pad's chunks are loaded, the pad is built or checked
         * cell by cell ({@link SelfTestPad}, {@link SelfTestPadShape}), the block under the stand point is confirmed
         * solid, and only then is he teleported to the stand point, facing east. From then on every row's start clean
         * returns him THERE, not to where he typed the command; the run's restore still returns him to where he was.
         * Any failure: no teleport, a plain chat line, the run ends and the snapshot restores. Logs the {@code PAD} line.
         */
        PAD,
        /** A line for the reader. Changes nothing. */
        NOTE
    }
}
