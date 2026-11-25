package io.github.butterflysmp.rpg.paper.adapter;

import io.github.butterflysmp.rpg.core.mob.MobHostility;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * THE ONE CLASSIFIER (the seat's H1 and H6, 2026-11-24; 2b): which {@link MobHostility.Side} a server entity is on. Every
 * paper site that needs a side asks HERE: the snapshot's builder ({@code BukkitCombatant}), the mob capabilities (the
 * seven {@code instanceof Player} copies this replaced), the damage guard and the vanilla listeners.
 *
 * <p>A player is PLAYER. An entity carrying the summon tag ({@link Keys#summonOwner}) is SUMMON. Anything else is MOB.
 * <b>Nothing sets the tag on a live entity before PR 3</b>, and then only after the summon store owns the body (D7).
 */
public final class CombatSides {

    private CombatSides() {}

    /** The pure decision, so a test can drive it without a server. A player is never a summon, whatever it carries. */
    static MobHostility.Side classify(boolean isPlayer, boolean summonTagged) {
        if (isPlayer) return MobHostility.Side.PLAYER;
        return summonTagged ? MobHostility.Side.SUMMON : MobHostility.Side.MOB;
    }

    /** This entity's side. Read on the entity's own thread (its data container), as every caller already does. */
    public static MobHostility.Side of(Entity entity, Keys keys) {
        return of(entity, keys.summonOwner);
    }

    /** The same, given the tag's key itself (a test has no plugin to build {@link Keys} from). */
    static MobHostility.Side of(Entity entity, org.bukkit.NamespacedKey summonOwner) {
        return classify(entity instanceof Player, entity.getPersistentDataContainer().has(summonOwner));
    }

    /**
     * The side of a damage's SOURCE, named only by id ({@code applyNow}'s {@code sourceId}). An online player is PLAYER;
     * an entity found by id is classified; an id that resolves to nothing is MOB (no source we can see, which the guard
     * never refuses). FOLIA DEBT: {@code Bukkit.getEntity} reads another entity off its region; correct on Paper today.
     */
    public static MobHostility.Side ofSource(UUID sourceId, Keys keys) {
        if (sourceId == null) return MobHostility.Side.MOB;
        if (Bukkit.getPlayer(sourceId) != null) return MobHostility.Side.PLAYER;
        Entity e = Bukkit.getEntity(sourceId);
        return e == null ? MobHostility.Side.MOB : of(e, keys);
    }

    /** The guard's question, read off the one table: is this one of the three summon cells that is a "no"? */
    public static boolean refused(MobHostility.Side attacker, MobHostility.Side victim) {
        return MobHostility.refusedSummonCell(attacker, victim);
    }
}
