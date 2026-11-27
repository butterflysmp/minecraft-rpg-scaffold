package io.github.butterflysmp.rpg.core.mob;

/**
 * WHO MAY DAMAGE OR TARGET WHOM: THE ONE TABLE (the seat's rulings H1-H8 and P1-P4, 2026-11-24; PLAN-summoner.md
 * section 2). Every core pick filter, the paper side's one classifier and the damage guard ask THIS class; no other copy of
 * the rule exists.
 *
 * <p>The table is ATTACKER SIDE x VICTIM SIDE. Every cell is a plain yes or no, except PLAYER -> PLAYER, which is
 * "per path, as today" and lives in ONE place, {@link PvpPath}:
 *
 * <pre>
 *   attacker \ victim |  PLAYER                      |  MOB  |  SUMMON
 *   ------------------+------------------------------+-------+--------
 *   PLAYER            |  PER PATH, AS TODAY (PvpPath) |  yes  |  no
 *   MOB               |  yes, unless exempt mode     |  no   |  yes
 *   SUMMON            |  no                          |  yes  |  no
 * </pre>
 *
 * <p>Ben: the Phage's blast hurts "Only players and Summons" (G). So MOB -> SUMMON reads the same as MOB -> PLAYER, by
 * the same row {@link #hostileTo} reads; a creative or spectator player is exempt, as in vanilla. The SUMMON cells ignore
 * the path (P4). (An {@code enum} is a fixed set of named values.)
 *
 * <p><b>THE RECORD FROM BEFORE THE SUMMONER SLICE, kept as written:</b> WHO A MOB IS HOSTILE TO: the ONE named predicate
 * (PLAN-dragon-mobs.md section 4.2 N3; the seat's ruling 24). Ben: the Phage's blast hurts "Only players and Summons"
 * (G), and every new mob capability takes a COMBATANT as its victim or target, never specifically a player (seat ruling
 * 3). Today the engine has one side marker, the {@code player} boolean on {@code HealthState} and
 * {@code CombatantSnapshot} (section 7 F-E), so today the answer is exactly "a player". A Summoner widens it HERE, by
 * adding a {@link Side}, and every capability that asks this predicate follows without a rewrite. A creative or
 * spectator player is not a mob's target, as in vanilla, whose swell already skips them
 * ({@code EntitySelector.NO_CREATIVE_OR_SPECTATOR}, section 7 F-C). <i>(2026-11-24: the snapshot's boolean is now a
 * {@code Side}; {@code HealthState} keeps its boolean, which is about stats, not sides.)</i>
 */
public final class MobHostility {

    private MobHostility() {}

    /** Which side a combatant is on. */
    public enum Side { PLAYER, MOB, SUMMON }

    /**
     * THE CORE ABILITY ENGINE'S ATTACKER: a PLAYER. READ (2a): the engine ({@code CastExecutor}, {@code EffectApplier},
     * {@code ProjectileFlight}, {@code BehaviourHits}, {@code Ignite}) is driven in paper only by a player's cast
     * ({@code StoneCaster}, {@code RpgCommand}'s cast, {@code VanillaShots}, {@code WeaponFire} twice), and Ignite's applier
     * is the id a player's scorch stored. A mob's abilities run their own paper classes and ask {@link #hostileTo}.
     *
     * <p>It is a constant, not read off the caster's own entity, because before the Summoner slice the engine never asked
     * who the attacker was: test fixtures cast from a default (mob) dummy, and reading THAT side would have turned every
     * one of their hits into MOB -> MOB. When a summon ever casts through the engine, this becomes a field of the caster.
     */
    public static final Side ENGINE_ATTACKER = Side.PLAYER;

    /**
     * PLAYER -> PLAYER: TODAY'S BEHAVIOUR, UNRULED, OPEN FOR BEN (PvP). Ben has been asked "yes, no, or later"; until he
     * answers, nothing about player-to-player damage changes. <b>His answer is a change to THIS enum and nowhere else.</b>
     *
     * <p>Each value is what the code did before the Summoner slice, pinned by a characterisation test that was green
     * before the refactor and is green after it ({@code PvpTodayCharacterisationTest}, and the pins it names).
     */
    public enum PvpPath {
        /** A player's swing ({@code CastSpec.Melee}). */
        SWING(true),
        /** A ray's struck bodies. */
        RAY(true),
        /** A projectile's first body. */
        PROJECTILE(true),
        /** A cone's bodies. */
        CONE(true),
        /** A burst ({@code EffectSpec.Burst}). */
        BURST(true),
        /** An area's pulses ({@code EffectSpec.Area}). */
        AREA(true),
        /** A dash's line ({@code SweptLine}): "mob-only: players are not hit". */
        DASH_LINE(false),
        /** A thrown or travel-end burst ({@code applyToNearbyMobs}). */
        TRAVEL_END_BURST(false),
        /** The Tremors shockwave. */
        SHOCKWAVE(false),
        /** The Lapis Crystal's shot. */
        CRYSTAL_SHOT(false),
        /** Ignite's blast. */
        IGNITE_SPLASH(false),
        /** A homing projectile's choice of whom to chase. */
        HOMING_PICK(false),
        /** A FUSED projectile's detonation (the seat's F4, 2026-11-26, the Blast Fungus): SPARED, as TRAVEL_END_BURST. */
        FUSED_BURST(false),
        /**
         * A PULSE's ring ({@code CastSpec.Pulses}; Smolder, the Fire Mage's ultimate, the seat's Z4, 2026-11-27): SPARED.
         * Ben, verbatim: "Every hostile. No Summons and No players for now just like the summoner weapon. We'll expand on
         * this during the PVP pass."
         */
        PULSE(false),
        /**
         * An implosion. Its {@code affects:} setting is the CONTENT's choice of victims and is applied at its site before
         * this table is asked; for what that setting admits, a player's implosion lands on a player, as today.
         */
        IMPLOSION(true),
        /** A status a player's direct hit put on another player ticks on him (the tick itself is side-blind). */
        STATUS_TICK(true);

        private final boolean landsToday;

        PvpPath(boolean landsToday) {
            this.landsToday = landsToday;
        }

        /** Does a player's hit by this path land on another player? TODAY'S BEHAVIOUR, UNRULED. */
        public boolean landsOnAPlayer() {
            return landsToday;
        }
    }

    /**
     * THE TABLE. May a combatant on side {@code attacker} damage or target one on side {@code victim}?
     *
     * @param victimExempt the victim is a player in creative or spectator (read only by the MOB row)
     * @param path         which path is asking; read ONLY for PLAYER -> PLAYER, where it is required
     * @throws IllegalArgumentException a PLAYER -> PLAYER question that names no path: there is no answer without one
     */
    public static boolean harms(Side attacker, Side victim, boolean victimExempt, PvpPath path) {
        return switch (attacker) {
            case PLAYER -> switch (victim) {
                case PLAYER -> {
                    if (path == null) throw new IllegalArgumentException("PLAYER -> PLAYER is per path; name one");
                    yield path.landsOnAPlayer();
                }
                case MOB -> true;
                case SUMMON -> false;
            };
            case MOB -> switch (victim) {
                case PLAYER -> !victimExempt;
                case MOB -> false;
                case SUMMON -> true;
            };
            case SUMMON -> victim == Side.MOB;
        };
    }

    /**
     * The three cells the damage guard refuses whatever the path (H3, P4): PLAYER -> SUMMON, SUMMON -> PLAYER and
     * SUMMON -> SUMMON. Read off {@link #harms}, never restated. False for every cell without a summon in it, so the
     * guard can never switch off player-to-player damage.
     */
    public static boolean refusedSummonCell(Side attacker, Side victim) {
        boolean summonInIt = attacker == Side.SUMMON || victim == Side.SUMMON;
        return summonInIt && !harms(attacker, victim, false, null);
    }

    /**
     * True when a mob treats this combatant as an enemy: its blast hurts it, its shot seeks it. THE MOB ROW of
     * {@link #harms}.
     *
     * @param exemptMode the combatant is a player in creative or spectator
     */
    public static boolean hostileTo(Side side, boolean exemptMode) {
        return harms(Side.MOB, side, exemptMode, null);
    }

    /**
     * NOT THE TABLE: does a hit on this victim count for the attacker's behavioural enchants, the scorch stamp and the
     * PLAYERHIT trace? Only a hit on a MOB does, as before the Summoner slice ("only what his abilities may hurt counts,
     * and that is mobs"). Kept apart from {@link #harms} on purpose: a player's ray LANDS on a player today and must not
     * start counting for his enchants because the table says it lands.
     */
    public static boolean countsAsAMobHit(Side victim) {
        return victim == Side.MOB;
    }
}
