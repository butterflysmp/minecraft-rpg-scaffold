package io.github.butterflysmp.rpg.core.mob;

/**
 * One custom mob: a content-defined identity that is DISTINCT from the vanilla entity it spawns as.
 *
 * The Knell is a wither skeleton with 360 HP; an ordinary wither skeleton is still an ordinary wither
 * skeleton. That separation is the whole point, and it is why a mob is keyed by its own {@code id} and
 * never by {@code baseEntity}: keying on entity type would scale every wither skeleton on the server.
 * The mechanism is the weapon one exactly -- a definition in content, a PDC tag on the specific
 * spawned entity, and a registry lookup by that tag. No tag, no change.
 *
 * {@code baseEntity} is a plain String, not a Bukkit EntityType, because {@code core} has no Bukkit
 * (CLAUDE.md invariant 1). Paper resolves it through {@code Registry.ENTITY_TYPE} and rejects one that
 * is not a living entity, so a typo is a named, skipped file at boot rather than a ClassCastException
 * the first time someone spawns it.
 *
 * Shaped to grow. {@code attackDamage} and the rest of the stat block become further components; only
 * {@code maxHealth} is wired this pass, and the custom health store is uncapped, so 5000 is the same
 * mechanism as 360 rather than a different one.
 *
 * <p><b>Grown in dragon-mobs step R1</b> (PLAN-dragon-mobs.md) by three optional components, each absent by default
 * so every older file and call site means what it always meant:
 * <ul>
 *   <li>{@code body}: scale and a speed multiplier ({@link MobBody}); {@link MobBody#VANILLA} when unauthored.</li>
 *   <li>{@code element}: an existing {@code content/elements} id, or null. IDENTITY ONLY on day one: it types no
 *       damage, resists nothing, and refuses no status. The loader refuses an id that names no element.</li>
 *   <li>{@code stages}: the Phage's per-stage bodies ({@link MobStages}), or null. A staged mob's
 *       {@code maxHealth} is its stage-1 health, and its body lives in its stages, never in {@code body} too.</li>
 * </ul>
 * Gear (the plan's MB1 equipment) is DEFERRED past R1 and has no component here.
 *
 * <p><b>Grown again in step R2</b> by {@code split} ({@link MobSplit}), or null: what a staged mob leaves behind
 * when it dies at its last stage. A split needs stages, and its child stage must be one of them.
 *
 * <p><b>And in the growth step</b> by {@code growth} ({@link MobGrowth}), or null: which deaths nearby move a staged
 * mob up its stages. Growth needs stages.
 *
 * <p><b>And in step R3</b> by {@code blast} ({@link MobBlast}), or null: the self-destruct a staged mob detonates when
 * its fuse completes. A blast's damage is priced by stage, so it needs stages.
 *
 * <p><b>And in step R5</b> by {@code beam} ({@link MobBeam}), or null: the Knell's timed beam. It needs no stages.
 *
 * <p><b>And in step R6</b> by {@code teleport} ({@link MobTeleport}), or null: the move after the beam hits. It needs a
 * beam, and its delay must be shorter than the beam's {@code cycle_ticks.min}.
 *
 * <p><b>And in step R7a</b> by {@code shot} ({@link MobShot}), or null: the Phalanx's destroyable homing shot; and by
 * {@code mainHand}, or null: a vanilla ITEM key the mob holds, never dropped (the Phalanx's bow it never fires, X3).
 * A mob with a shot never looses a vanilla arrow: paper cancels every bow shot of one.
 *
 * <p><b>And in step R8</b> (the Skreel and its mount) by three: {@code mount}, or null: the id of the mob file this one
 * is spawned RIDING (the Skreel's {@code withered_mount}); {@code pounce} ({@link MobPounce}), or null: the pair's cycle,
 * which needs a mount; and {@code riddenSpeed}, or null: a MULTIPLIER of this mob's movement speed that applies only
 * once a rider of ours has been on it (the mount's 1.3), added by paper as a transient modifier, never as its base.
 */
public record MobDefinition(
        String id,
        String baseEntity,
        String displayName,
        double maxHealth,
        MobBody body,
        String element,
        MobStages stages,
        MobSplit split,
        MobGrowth growth,
        MobBlast blast,
        MobBeam beam,
        MobTeleport teleport,
        MobShot shot,
        String mainHand,
        MobBlink blink,
        MobShield shield,
        String mount,
        MobPounce pounce,
        Double riddenSpeed,
        MobStance stance
) {
    /** Step R8's nineteen-field form: no stance (the dragon fight's step F6). Kept so its call sites read unchanged. */
    public MobDefinition(String id, String baseEntity, String displayName, double maxHealth,
                         MobBody body, String element, MobStages stages, MobSplit split, MobGrowth growth,
                         MobBlast blast, MobBeam beam, MobTeleport teleport, MobShot shot, String mainHand,
                         MobBlink blink, MobShield shield, String mount, MobPounce pounce, Double riddenSpeed) {
        this(id, baseEntity, displayName, maxHealth, body, element, stages, split, growth, blast, beam, teleport,
                shot, mainHand, blink, shield, mount, pounce, riddenSpeed, MobStance.NONE);
    }

    /** Step R7b's sixteen-field form: no mount, no pounce, no ridden speed (step R8's). Kept so its call sites read unchanged. */
    public MobDefinition(String id, String baseEntity, String displayName, double maxHealth,
                         MobBody body, String element, MobStages stages, MobSplit split, MobGrowth growth,
                         MobBlast blast, MobBeam beam, MobTeleport teleport, MobShot shot, String mainHand,
                         MobBlink blink, MobShield shield) {
        this(id, baseEntity, displayName, maxHealth, body, element, stages, split, growth, blast, beam, teleport,
                shot, mainHand, blink, shield, null, null, null);
    }

    /**
     * The original four-field form: a vanilla body, no element, no stages. Kept so the existing call sites (tests
     * and older code) read unchanged. (A record may declare extra constructors; each must call the full one.)
     */
    public MobDefinition(String id, String baseEntity, String displayName, double maxHealth) {
        this(id, baseEntity, displayName, maxHealth, MobBody.VANILLA, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    /** Step R1's seven-field form: no split (step R2's component). Kept so R1's call sites read unchanged. */
    public MobDefinition(String id, String baseEntity, String displayName, double maxHealth,
                         MobBody body, String element, MobStages stages) {
        this(id, baseEntity, displayName, maxHealth, body, element, stages, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    /** Step R2's eight-field form: no growth (the growth step's component). Kept so R2's call sites read unchanged. */
    public MobDefinition(String id, String baseEntity, String displayName, double maxHealth,
                         MobBody body, String element, MobStages stages, MobSplit split) {
        this(id, baseEntity, displayName, maxHealth, body, element, stages, split, null, null, null, null, null, null, null, null, null, null, null);
    }

    /** The growth step's nine-field form: no blast (step R3's component). Kept so its call sites read unchanged. */
    public MobDefinition(String id, String baseEntity, String displayName, double maxHealth,
                         MobBody body, String element, MobStages stages, MobSplit split, MobGrowth growth) {
        this(id, baseEntity, displayName, maxHealth, body, element, stages, split, growth, null, null, null, null, null, null, null, null, null, null);
    }

    /** Step R3's ten-field form: no beam (step R5's component). Kept so its call sites read unchanged. */
    public MobDefinition(String id, String baseEntity, String displayName, double maxHealth,
                         MobBody body, String element, MobStages stages, MobSplit split, MobGrowth growth,
                         MobBlast blast) {
        this(id, baseEntity, displayName, maxHealth, body, element, stages, split, growth, blast, null, null, null, null, null, null, null, null, null);
    }

    /** Step R5's eleven-field form: no teleport (step R6's component). Kept so its call sites read unchanged. */
    public MobDefinition(String id, String baseEntity, String displayName, double maxHealth,
                         MobBody body, String element, MobStages stages, MobSplit split, MobGrowth growth,
                         MobBlast blast, MobBeam beam) {
        this(id, baseEntity, displayName, maxHealth, body, element, stages, split, growth, blast, beam, null, null, null, null, null, null, null, null);
    }

    /** Step R6's twelve-field form: no shot and no main-hand item (step R7a's). Kept so its call sites read unchanged. */
    public MobDefinition(String id, String baseEntity, String displayName, double maxHealth,
                         MobBody body, String element, MobStages stages, MobSplit split, MobGrowth growth,
                         MobBlast blast, MobBeam beam, MobTeleport teleport) {
        this(id, baseEntity, displayName, maxHealth, body, element, stages, split, growth, blast, beam, teleport,
                null, null, null, null, null, null, null);
    }

    /** Step R7a's fourteen-field form: no blink and no shield (step R7b's). Kept so its call sites read unchanged. */
    public MobDefinition(String id, String baseEntity, String displayName, double maxHealth,
                         MobBody body, String element, MobStages stages, MobSplit split, MobGrowth growth,
                         MobBlast blast, MobBeam beam, MobTeleport teleport, MobShot shot, String mainHand) {
        this(id, baseEntity, displayName, maxHealth, body, element, stages, split, growth, blast, beam, teleport,
                shot, mainHand, null, null, null, null, null);
    }

    public MobDefinition {
        if (body == null) body = MobBody.VANILLA;
        if (element != null && element.isBlank()) {
            throw new IllegalArgumentException("mob '" + id + "' element must name an element, or be left out");
        }
        // The growth step: growing moves a STAGE, so it needs stages.
        if (growth != null && stages == null) {
            throw new IllegalArgumentException("mob '" + id + "' authors growth but no stages; growing moves a stage");
        }
        // Step R3: a blast's damage is priced by STAGE, so it needs stages.
        if (blast != null && stages == null) {
            throw new IllegalArgumentException("mob '" + id + "' authors a blast but no stages; its damage is per stage");
        }
        // The void implosion's Part C (2026-11-17): a fuse whose cancel is not beyond its trigger at some stage would
        // reset the tick it starts. Its distances are per stage, so the check is here, where the stages are.
        if (blast != null) blast.checkStages(stages);
        // Step R2: a split names a child STAGE, so it needs stages, and the child stage must be one of them.
        if (split != null && stages == null) {
            throw new IllegalArgumentException("mob '" + id + "' authors a split but no stages; a split names a"
                    + " child stage");
        }
        if (split != null && !stages.isValid(split.childStage())) {
            throw new IllegalArgumentException("mob '" + id + "' split.child_stage " + split.childStage()
                    + " is not one of its stages 1.." + stages.count());
        }
        if (stages != null && !body.isVanilla()) {
            throw new IllegalArgumentException("mob '" + id + "' authors stages AND top-level scale/speed;"
                    + " a staged mob's body lives in its stages only");
        }
        // Step R6: the teleport follows the BEAM's hit, so it needs a beam; and its delay must be shorter than the
        // beam's shortest wait, so a move never falls inside a telegraph (the next attempt is at least
        // cycle_ticks.min after the fire, and the move is delay_ticks after it).
        if (teleport != null && beam == null) {
            throw new IllegalArgumentException("mob '" + id + "' authors a teleport but no beam; the teleport needs a"
                    + " beam to follow");
        }
        if (teleport != null && teleport.delayTicks() >= beam.cycleMinTicks()) {
            throw new IllegalArgumentException("mob '" + id + "' teleport.delay_ticks (" + teleport.delayTicks()
                    + ") must be shorter than beam.cycle_ticks.min (" + beam.cycleMinTicks()
                    + "), or the move could fall inside the next telegraph");
        }
        // Step R8: the pounce is the PAIR'S cycle, so a mob that authors one must name the mount it rides; a mount id
        // names a mob file (paper refuses a spawn whose mount names none); a ridden speed is a multiplier above 0.
        if (mount != null && mount.isBlank()) {
            throw new IllegalArgumentException("mob '" + id + "' mount must name a mob, or be left out");
        }
        if (pounce != null && mount == null) {
            throw new IllegalArgumentException("mob '" + id + "' authors a pounce but no mount; the pounce is the"
                    + " pair's, and needs a mount to ride");
        }
        if (riddenSpeed != null && !(riddenSpeed > 0 && Double.isFinite(riddenSpeed))) {
            throw new IllegalArgumentException("mob '" + id + "' ridden_speed must be a multiplier above 0, was "
                    + riddenSpeed);
        }
        if (mainHand != null && mainHand.isBlank()) {
            throw new IllegalArgumentException("mob '" + id + "' main_hand must name an item, or be left out");
        }
        if (id == null || id.isBlank()) throw new IllegalArgumentException("mob id required");
        // Required, never defaulted -- the same reasoning that made a weapon's class required. A
        // silent default here would spawn the wrong creature and look entirely intentional.
        if (baseEntity == null || baseEntity.isBlank()) {
            throw new IllegalArgumentException("mob '" + id + "' requires base_entity (the vanilla entity it spawns as)");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("mob '" + id + "' display_name required");
        }
        // 0 is a content bug, not a legal value: a mob that seeds at 0 custom HP is born dead (or
        // never dies, depending on which side of the reachedZero transition it lands on).
        if (maxHealth <= 0) {
            throw new IllegalArgumentException("mob '" + id + "' max_health must be > 0, was " + maxHealth);
        }
    }
}
