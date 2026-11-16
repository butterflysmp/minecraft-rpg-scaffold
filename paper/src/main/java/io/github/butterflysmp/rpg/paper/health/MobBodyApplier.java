package io.github.butterflysmp.rpg.paper.health;

import io.github.butterflysmp.rpg.core.mob.MobBodies;
import io.github.butterflysmp.rpg.core.mob.MobDefinition;
import io.github.butterflysmp.rpg.core.mob.StageBody;
import io.github.butterflysmp.rpg.paper.adapter.Keys;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataType;

import java.util.logging.Logger;

/**
 * Puts a custom mob's BODY onto a live entity (PLAN-dragon-mobs.md step R1): its {@code SCALE} and its movement
 * speed, and, for a staged mob, its stage. The arithmetic is {@link MobBodies}', unit-tested in core; this class is
 * the Bukkit binding and nothing else.
 *
 * <h2>WHEN: IN THE SPAWN'S PRE-SPAWN CONSUMER, BESIDE {@code mob_id}</h2>
 *
 * The stage must be on the entity before the add event seeds its health (a stage-3 Phage seeds 120, not 80), for
 * the same reason {@code mob_id} must (RpgCommand.spawnMob's javadoc). The attributes go there too, so the mob never
 * exists in the world at its vanilla size. Attribute BASE values and PDC are both saved with the entity, so a body
 * survives a chunk unload and a restart and is never re-applied on load.
 *
 * <h2>IT WRITES THE BASE, NEVER A MODIFIER</h2>
 *
 * Our slows and roots ({@code EntitySpeedAttribute}) are MODIFIERS on top of the base, so a slowed Knell is slowed
 * from its body's speed, and removing the slow returns it to its body's speed, not to vanilla's.
 *
 * <h2>AND IT READS THE CREEPER'S UNSCALED HEIGHT OFF THE ENTITY</h2>
 *
 * {@code Entity.<init>} sets the entity's dimensions from its TYPE (javap, the server jar of build 74:
 * {@code EntityType.getDimensions} then {@code putfield dimensions}), so inside the consumer {@code getHeight()} is
 * the type's height at scale 1, 1.7 for a creeper. {@link MobBodies#scaleForHeight} divides by the current scale as
 * well, so the same call is right on a body that is already scaled.
 */
public final class MobBodyApplier {

    private MobBodyApplier() {}

    /**
     * Apply {@code def}'s body to {@code entity}, at {@code stage} if the mob is staged (ignored otherwise), and write
     * the stage tag. The caller has already validated the stage.
     */
    public static void apply(LivingEntity entity, MobDefinition def, int stage, Keys keys) {
        if (def.stages() != null) {
            StageBody body = def.stages().at(stage);
            entity.getPersistentDataContainer().set(keys.mobStage, PersistentDataType.INTEGER, stage);
            AttributeInstance scale = entity.getAttribute(Attribute.SCALE);
            if (scale != null) {
                scale.setBaseValue(MobBodies.scaleForHeight(body.height(), entity.getHeight(), scale.getValue()));
            }
            applySpeed(entity, body.speedMultiplier());
            return;
        }
        AttributeInstance scale = entity.getAttribute(Attribute.SCALE);
        if (scale != null) def.body().scale().ifPresent(scale::setBaseValue);
        def.body().speedMultiplier().ifPresent(m -> applySpeed(entity, m));
    }

    private static void applySpeed(LivingEntity entity, double multiplier) {
        AttributeInstance speed = entity.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speed == null) return;
        MobBodies.applySpeed(new MobBodies.BodyAttribute() {
            @Override public double defaultValue() { return speed.getDefaultValue(); }
            @Override public double baseValue() { return speed.getBaseValue(); }
            @Override public void setBaseValue(double value) { speed.setBaseValue(value); }
        }, multiplier);
    }

    /**
     * The stage a live mob carries, for the seed. A staged mob with no tag, or a tag outside its range (only a
     * {@code /summon} with hand-written {@code BukkitValues} can put one there), is treated as stage 1 and the bad
     * value is named at WARN, the way an invalid stored gear score is. An unstaged mob answers 1 and is never asked.
     */
    public static int stageOf(LivingEntity entity, MobDefinition def, Keys keys, Logger log) {
        if (def == null || def.stages() == null) return 1;
        var pdc = entity.getPersistentDataContainer();
        Integer stored = pdc.has(keys.mobStage, PersistentDataType.INTEGER)
                ? pdc.get(keys.mobStage, PersistentDataType.INTEGER) : null;
        if (stored != null && def.stages().isValid(stored)) return stored;
        if (pdc.has(keys.mobStage)) {
            log.warning("mob " + entity.getUniqueId() + " (" + def.id() + ") carried an invalid " + keys.mobStage
                    + " (" + stored + "; must be 1.." + def.stages().count() + "), so it seeds as stage 1");
        }
        return 1;
    }
}
