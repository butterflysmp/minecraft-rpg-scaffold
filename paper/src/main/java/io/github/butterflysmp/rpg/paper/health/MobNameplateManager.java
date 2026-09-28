package io.github.butterflysmp.rpg.paper.health;

import io.github.butterflysmp.rpg.core.combat.stat.CombatantStats;
import io.github.butterflysmp.rpg.core.combat.stat.HealthChange;
import io.github.butterflysmp.rpg.core.combat.stat.HealthListener;
import io.github.butterflysmp.rpg.core.combat.stat.HealthState;
import io.github.butterflysmp.rpg.core.mob.GearScoreSource;
import io.github.butterflysmp.rpg.core.mob.MeleeSeed;
import io.github.butterflysmp.rpg.core.mob.MobDamagePricing;
import io.github.butterflysmp.rpg.core.mob.MobGearScore;
import io.github.butterflysmp.rpg.core.mob.MobRegistry;
import io.github.butterflysmp.rpg.core.mob.MobScaling;
import io.github.butterflysmp.rpg.core.mob.MobSeeding;
import io.github.butterflysmp.rpg.paper.adapter.EntityTaskTarget;
import io.github.butterflysmp.rpg.paper.adapter.Keys;
import io.github.butterflysmp.rpg.paper.scheduler.RepeatingTask;
import io.github.butterflysmp.rpg.paper.scheduler.Scheduler;
import net.kyori.adventure.text.Component;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The mob health nameplate: the SECOND display on the {@link HealthChange} seam (the player heart bar
 * is the first). It shows {@code <name> <cur>/<max> ❤} with the CUSTOM cur/max, gated per viewer by
 * line of sight, sent via per-viewer packets. The HP text is never written to the mob's real
 * server-side name -- it is a per-viewer metadata override, so death messages and /data never carry a
 * health bar. (A CUSTOM mob does carry a real CustomName, set at spawn: its identity is genuine, so
 * "Knell" belongs in a death message. Its HP still lives only in the packet. See
 * {@code PacketNameplateSender}.)
 *
 * Three roles:
 *  - as a {@link HealthListener}: on a mob health change, rebuild the cached name text and bump its
 *    version ({@link #onChange}). Pure -- no Bukkit, any thread. Drives WHAT the name says.
 *  - mob lifecycle ({@link #onMobAppear} / {@link #onMobRemove}, from RpgListeners' entity events):
 *    bootstrap custom HP from vanilla max on appearance, cache the nameplate; drop it on removal so no
 *    state leaks past death/despawn.
 *  - per-viewer LOS loop ({@link #onViewerJoin}): each viewer runs its own loop on its own thread --
 *    where getNearbyEntities and hasLineOfSight are legal -- re-asserting visibility each cycle and
 *    resending text only when it changed. Drives WHO can see it.
 */
public final class MobNameplateManager implements HealthListener {

    /** Rescan cadence. Small so nameplates pop in/out with movement; the scale check tunes it. */
    private static final int NAMEPLATE_PERIOD_TICKS = 4;
    /** Range to scan per viewer -- the vanilla name-render cap, beyond which the client draws no name anyway. */
    private static final double VIEW_RADIUS = 64.0;

    private final Scheduler scheduler;
    /** Where a crashed repeating loop is reported -- see EntityTaskTarget.loopFailed. */
    private final java.util.logging.Logger log;
    private final NameplateSender sender;
    private final Keys keys;
    private final MobRegistry mobs;
    private final Map<UUID, Nameplate> nameplates = new ConcurrentHashMap<>();
    private CombatantStats stats;
    /**
     * Where a hostile mob's score comes from when it carries none. The M3 seam: one field of the
     * interface type, one implementation. A boss or activity source replaces this, and a stored score
     * still wins over it (M5).
     */
    private final GearScoreSource gearScores = GearScoreSource.BLANKET;
    /** {@link #toggleTrace}'s flag. Volatile: toggled on a command thread, read on every entity's. */
    private volatile boolean trace;

    /**
     * The mob registry arrives by CONSTRUCTOR rather than on {@code AdapterContext}, unlike the
     * element registry the tooltip needed. Not a style choice -- it is forced: this manager is built
     * in onEnable BEFORE the AdapterContext exists, and it has to be, because the context needs the
     * stat store and the store needs this manager as a HealthListener. Mirrors
     * {@code PlayerHealthSystem(scheduler, keys, weapons)}, which already takes a content registry
     * exactly this way.
     */
    public MobNameplateManager(Scheduler scheduler, NameplateSender sender, Keys keys, MobRegistry mobs,
                                java.util.logging.Logger log) {
        this.log = log;
        this.scheduler = scheduler;
        this.sender = sender;
        this.keys = keys;
        this.mobs = mobs;
    }

    /** Wire the store; called once in onEnable after the store is built (breaks the listener/store cycle). */
    public void bind(CombatantStats stats) {
        this.stats = stats;
    }

    // --- HealthListener: rebuild the cached text on a mob health change -----------------------------

    @Override
    public void onChange(HealthChange change) {
        if (change.targetIsPlayer()) return;          // the heart bar handles players
        Nameplate nameplate = nameplates.get(change.target());
        if (nameplate == null) return;                // not a nameplated mob (unregistered); nothing to update
        nameplate.update(NameplateText.of(nameplate.baseName(), change.newCurrent(), change.max()));
    }

    // --- Mob lifecycle (driven by RpgListeners' entity add/remove events) --------------------------

    /**
     * Seed a mob's custom combat stats if not already tracked, and return its state (null for a player
     * / armor stand). HP comes from the mob's CONTENT DEFINITION when it carries a {@code mob_id} tag,
     * and from its vanilla max otherwise, and is then SCALED -- x5 and x GS/100, see {@link #seed}.
     * Attack damage is the vanilla ATTACK_DAMAGE attribute scaled the same way (mob-scaling slice 2,
     * M16), for both. Register-if-absent, so repeat calls are idempotent.
     *
     * Register-if-absent is also why the spawn path MUST tag the entity before it enters the world:
     * {@code EntityAddToWorldEvent} lands here first, and a tag applied afterwards would arrive to find
     * the mob already seeded from vanilla -- a Knell with 20 HP and no visible sign of it. See
     * {@code RpgCommand}'s pre-spawn consumer.
     *
     * OPT-OUT-AGNOSTIC on purpose: the {@code nameplateOptOut} flag suppresses only the nameplate
     * DISPLAY, never a mob's combat stats. A mob's damage must not depend on whether it shows a health
     * bar -- so this seeds regardless of the flag, and {@link #onMobMeleeAttack in RpgListeners} reads
     * the attack stat through here. (An opt-out mob whose stats were gated behind the flag would read
     * attackValue 0 and deal zero custom damage -- a silent regression from the old event.getDamage().)
     */
    public HealthState seedCombatStats(LivingEntity mob) {
        Seeded seeded = seed(mob);
        return seeded == null ? null : seeded.state();
    }

    /** What one seed decided: the store entry, the mob's score (empty for a passive mob), and why. */
    private record Seeded(HealthState state, OptionalInt gearScore, String source) {}

    /**
     * The seed, and the only place a mob's max HP is decided (PLAN-mob-scaling.md, slice 1).
     *
     * <ol>
     *   <li><b>Which base</b> -- {@link MobSeeding}: a CUSTOM mob's content definition, keyed by its own
     *       {@code mob_id} tag and never by its type (the Knell is a wither skeleton; ordinary wither
     *       skeletons stay ordinary), else the vanilla attribute.
     *   <li><b>Which score</b> -- {@link #gearScoreOf}, hostile mobs only (M7, M21).
     *   <li><b>How much</b> -- {@link MobScaling}: x5 unless custom (M1, M4), x GS/100 if hostile (M2).
     * </ol>
     *
     * <p><b>*** THE SCALED MAX GOES INTO THE CUSTOM STORE AND NOWHERE ELSE (M9). ***</b> Never into the
     * vanilla {@code MAX_HEALTH} attribute: a GS 500 Warden is 12,500, past vanilla's cap -- and,
     * quieter and worse, environmental damage on a mob is priced as vanilla amount / THAT ATTRIBUTE x
     * the custom max ({@code DamageScale.toCustom}), which is exactly M13's proportion only while the
     * attribute stays vanilla. Write the scaled max into it and every fall, fire and lava hit on every
     * mob silently becomes x1, with the nameplate still reading correctly.
     * {@code MobAttributeUntouchedSignatureTest} is the mechanical guard.
     */
    private Seeded seed(LivingEntity mob) {
        if (mob instanceof Player || mob instanceof ArmorStand) return null;
        String mobId = mob.getPersistentDataContainer().get(keys.mobId, PersistentDataType.STRING);
        boolean custom = MobSeeding.isCustom(mobs, mobId);
        boolean hostile = MobClassifier.isHostile(mob);

        OptionalInt score = OptionalInt.empty();
        String source = "passive";                    // a passive mob's PDC is never read or written (M7)
        if (hostile) {
            Integer stored = storedGearScore(mob);
            if (stored != null) {
                score = OptionalInt.of(stored);
                source = "stored";
            } else {
                score = OptionalInt.of(rollAndStore(mob));
                source = "rolled";
            }
        }

        double base = MobSeeding.maxHealth(mobs, mobId, maxHealthOf(mob));
        double max = MobScaling.maxHealth(base, custom, hostile, score.orElse(0));
        // Slice 2: melee is priced HERE, once, so onMobMeleeAttack's read of the stat is already M16's
        // vanilla x 5 x GS/100. The attribute itself is only read, never written (M9's attack mirror).
        double attack = MobScaling.attackDamage(attackDamageOf(mob), custom, hostile, score.orElse(0));
        HealthState state = stats.bootstrapIfAbsent(mob.getUniqueId(), max, attack, false);
        // F20: the seed is the one store write the HealthChange seam does not carry. A fresh store is at
        // FULL (F1), while vanilla health comes from the entity's NBT and can be anything, so vanilla is
        // mirrored here too. It mirrors the store's CURRENT, not its max, because this method is
        // idempotent and runs again on every melee rider and every /rpg mobdamage.
        MobVanillaMirror.apply(mob, state.current(), state.max());
        return new Seeded(state, score, source);
    }

    /**
     * The mob's stored score if it carries a trustworthy one, else null.
     *
     * <p>A value outside {@code 1..500} (or of the wrong PDC type) was not written by us -- only a
     * {@code /summon ... BukkitValues} can put one there -- so it is reported at WARN and treated as
     * absent, and the caller rolls one from position. That is not a re-roll in M5's sense.
     */
    private Integer storedGearScore(LivingEntity mob) {
        var pdc = mob.getPersistentDataContainer();
        if (!pdc.has(keys.mobGearScore)) return null;
        Integer stored = pdc.has(keys.mobGearScore, PersistentDataType.INTEGER)
                ? pdc.get(keys.mobGearScore, PersistentDataType.INTEGER) : null;
        if (stored != null && MobGearScore.isValidStored(stored)) return stored;
        log.warning("mob " + mob.getUniqueId() + " (" + mob.getType().key() + ") carried an invalid "
                + keys.mobGearScore + " (" + stored + "; must be " + MobGearScore.MIN + ".."
                + MobGearScore.CAP + "), so it is re-rolled from its position");
        return null;
    }

    /**
     * Roll a score from where the mob is NOW, and store it on the mob, once. Every later seed -- a
     * chunk reload, a restart, a portal arrival -- reads it back instead (M5).
     */
    private int rollAndStore(LivingEntity mob) {
        int rolled = gearScores.gearScoreFor(
                MobOrigin.dimensionOf(mob.getWorld()), MobOrigin.horizontalDistance(mob.getLocation()));
        mob.getPersistentDataContainer().set(keys.mobGearScore, PersistentDataType.INTEGER, rolled);
        return rolled;
    }

    /**
     * A mob CONVERTED into another (a villager into a zombie villager, a zombie into a drowned, a slime
     * split into smaller ones) passes its score on, so the new mob is the same fight (PLAN §1.7).
     *
     * <p><b>This runs BEFORE the new entities are added to the world</b> -- measured from the pinned
     * server jar, not assumed: {@code Mob.convertTo} calls {@code callEntityTransformEvent} before
     * {@code addFreshEntity}, and {@code Slime.remove} fires one transform event for all its children
     * before adding any. So this write is in place when their add event seeds them, and inheriting is
     * one PDC write. Vanilla's conversion copies no Bukkit PDC ({@code ConversionType.convertCommon}),
     * which is why this is needed at all.
     *
     * <p>Only a scored source passes anything on (a villager has none, so its zombie rolls at the
     * conversion point), and only a HOSTILE target takes it (a cured zombie villager becomes a passive
     * villager and gets no key).
     */
    public void inheritGearScore(Entity from, java.util.List<Entity> to) {
        var pdc = from.getPersistentDataContainer();
        if (!pdc.has(keys.mobGearScore, PersistentDataType.INTEGER)) return;
        Integer score = pdc.get(keys.mobGearScore, PersistentDataType.INTEGER);
        if (score == null || !MobGearScore.isValidStored(score)) return;
        for (Entity target : to) {
            if (target instanceof LivingEntity living && MobClassifier.isHostile(living)) {
                living.getPersistentDataContainer().set(keys.mobGearScore, PersistentDataType.INTEGER, score);
            }
        }
    }

    /**
     * A mob appeared (spawn or chunk-load) on its own thread. Seed its combat stats (always), then cache
     * the initial nameplate -- UNLESS it opts out, in which case the stats are still seeded and only the
     * display is skipped.
     */
    public void onMobAppear(LivingEntity mob) {
        Seeded seeded = seed(mob);                  // opt-out-AGNOSTIC: combat stats always seed
        if (seeded == null) return;                 // player / armor stand: no plate, no stats
        HealthState state = seeded.state();
        if (trace) {
            log.info("MOBSEED " + mob.getUniqueId() + " " + mob.getType().key().value()
                    + " gs=" + (seeded.gearScore().isPresent() ? seeded.gearScore().getAsInt() : "-")
                    + " source=" + seeded.source() + " max=" + Math.round(state.max()));
        }
        // The opt-out flag suppresses only the NAMEPLATE display -- the stats above are already seeded.
        if (mob.getPersistentDataContainer().has(keys.nameplateOptOut, PersistentDataType.BYTE)) return;
        // Register-if-absent, NOT replace -- mirrors bootstrapIfAbsent (the store half). onMobAppear
        // runs once on spawn, but EVERY /rpg mobdamage cast re-calls it. A replace would build a fresh
        // Nameplate at version 1 each cast; the NEXT TICK's applyDamage bump (1->2) then races the
        // viewer's 4-tick LOS sample, so some casts are missed ("every-other-cast"). The version must
        // climb monotonically for ViewerNameplateState.decide() to resend. Real combat never re-appears
        // a mob, so it was always monotonic there -- only the dev command re-appeared per hit.
        // The [GS] rides in the FIXED base name, so every later onChange rebuild keeps it (M5, M6).
        registerIfAbsent(nameplates, mob.getUniqueId(),
                NameplateText.nameWithScore(seeded.gearScore(), mob.name()), state.current(), state.max());
    }

    /**
     * A dev trace for the gate: when on, every seed logs {@code MOBSEED <uuid> <type> gs=.. source=..}
     * and every removal logs {@code MOBREMOVE <uuid>}. It exists because "the score survived an unload"
     * is HOLLOW IN TIME without proof the chunk unloaded: a mob that never left prints the same number.
     * G12 reads a MOBREMOVE line followed by a {@code source=stored} MOBSEED for the same uuid.
     *
     * @return the new state
     */
    public boolean toggleTrace() {
        trace = !trace;
        return trace;
    }

    /** Whether {@code /rpg mobtrace} is on. Slice 2's {@code MOBHIT} lines ride the same switch. */
    public boolean tracing() {
        return trace;
    }

    /**
     * What one entity in a damage source says about the mob behind it, for
     * {@link MobDamagePricing#resolve}. Null when it says nothing: no entity, a player, an armor stand,
     * or a non-living entity carrying no stamp.
     *
     * <p>A living mob answers for itself: custom-ness from its {@code mob_id}, hostility from
     * {@link MobClassifier}, and its stored score, raw. A projectile answers with the stamp
     * {@link #stampProjectile} wrote at launch; only a hostile shooter is stamped, so a stamp means
     * hostile.
     *
     * <p><b>Reads the entity's PDC on the calling thread.</b> On Paper that is the one main thread. On
     * Folia a causing mob can sit in another region, and this is the read the deferred
     * {@code MobGearScores} map would replace (PLAN §3 slice 2, resolution step 2).
     */
    public MobDamagePricing.MobFacts factsOf(Entity entity) {
        if (entity == null || entity instanceof Player || entity instanceof ArmorStand) return null;
        var pdc = entity.getPersistentDataContainer();
        OptionalInt gs = pdc.has(keys.mobGearScore, PersistentDataType.INTEGER)
                ? OptionalInt.of(pdc.get(keys.mobGearScore, PersistentDataType.INTEGER))
                : OptionalInt.empty();
        boolean custom = MobSeeding.isCustom(mobs, pdc.get(keys.mobId, PersistentDataType.STRING));
        if (entity instanceof LivingEntity living) {
            return new MobDamagePricing.MobFacts(custom, MobClassifier.isHostile(living), gs);
        }
        return gs.isPresent() ? new MobDamagePricing.MobFacts(custom, true, gs) : null;
    }

    /**
     * Stamp a hostile mob's projectile with its shooter's score (and {@code mob_id}, if it is custom),
     * so the hit prices at the score the shot was FIRED at. The shooter can die mid-flight (G10), and a
     * projectile outlives its shooter's chunk. The same key as the mob's own score: it names the same
     * quantity, and {@link #factsOf} reads both the same way.
     *
     * <p>Only a valid stored score is copied. An unseeded or passive shooter leaves the projectile
     * unstamped, and the hit then resolves from the causing entity, if it still exists.
     */
    public void stampProjectile(org.bukkit.entity.Projectile projectile) {
        if (!(projectile.getShooter() instanceof LivingEntity shooter) || shooter instanceof Player) return;
        if (!MobClassifier.isHostile(shooter)) return;
        var from = shooter.getPersistentDataContainer();
        Integer gs = from.get(keys.mobGearScore, PersistentDataType.INTEGER);
        if (gs == null || !MobGearScore.isValidStored(gs)) return;
        var to = projectile.getPersistentDataContainer();
        to.set(keys.mobGearScore, PersistentDataType.INTEGER, gs);
        String mobId = from.get(keys.mobId, PersistentDataType.STRING);
        if (mobId != null) to.set(keys.mobId, PersistentDataType.STRING, mobId);
    }

    /**
     * The pure map half of {@link #onMobAppear}: create the plate only if absent, and return its
     * resulting version. Bukkit-free and {@code static} so it is unit-testable on a plain map -- no
     * manager instance, scheduler, sender, or {@code LivingEntity} needed. The player / armor-stand /
     * opt-out filtering stays in {@code onMobAppear}, so this is reached only for a mob that should be
     * plated.
     */
    static long registerIfAbsent(Map<UUID, Nameplate> plates, UUID id, Component baseName,
                                 double current, double max) {
        return plates.computeIfAbsent(id, k ->
                new Nameplate(baseName, NameplateText.of(baseName, current, max))).version();
    }

    /** A mob was removed (death, despawn, chunk-unload). Drop its nameplate and health state -- no leak. */
    public void onMobRemove(UUID id) {
        if (trace) log.info("MOBREMOVE " + id);
        nameplates.remove(id);
        stats.clear(id);
    }

    // --- Per-viewer LOS loop ------------------------------------------------------------------------

    /** Start this viewer's nameplate loop. Self-cancels when the player leaves (EntityTaskTarget inactive). */
    public void onViewerJoin(Player viewer) {
        EntityTaskTarget target = new EntityTaskTarget(viewer, scheduler, log);
        ViewerNameplateState state = new ViewerNameplateState();
        RepeatingTask.start(target, NAMEPLATE_PERIOD_TICKS, "nameplates", () -> {
            tickViewer(viewer, state);
            return true;                              // runs until the viewer is gone
        }, () -> { });
    }

    /**
     * One cycle for one viewer, on the viewer's thread. Cheapest-first, mirroring the EntityMoveEvent
     * hot path: the 64-block getNearbyEntities bound comes before the per-mob hasLineOfSight raycast,
     * and the name Component is only re-encoded when its version changed.
     */
    private void tickViewer(Player viewer, ViewerNameplateState state) {
        Set<UUID> inRange = new HashSet<>();
        for (Entity entity : viewer.getNearbyEntities(VIEW_RADIUS, VIEW_RADIUS, VIEW_RADIUS)) {
            if (!(entity instanceof LivingEntity mob) || entity instanceof Player || entity instanceof ArmorStand) {
                continue;
            }
            Nameplate nameplate = nameplates.get(entity.getUniqueId());
            if (nameplate == null) continue;          // not registered yet; self-heals after its appear event
            UUID id = entity.getUniqueId();
            inRange.add(id);
            var snapshot = nameplate.snapshot();
            boolean los = viewer.hasLineOfSight(mob);
            var decision = state.decide(id, snapshot.version(), los);
            sender.send(viewer, mob.getEntityId(),
                    decision.includeName() ? Optional.of(snapshot.text()) : Optional.empty(),
                    decision.visible());
        }
        state.retainInRange(inRange);
    }

    private static double maxHealthOf(LivingEntity mob) {
        AttributeInstance attr = mob.getAttribute(Attribute.MAX_HEALTH);
        return attr != null ? attr.getValue() : mob.getHealth();
    }

    /**
     * A mob's vanilla melee attack, before scaling: its {@code ATTACK_DAMAGE} BASE plus its held main-hand
     * weapon's ADD modifiers ({@link MeleeSeed}, the seat's ruling of 2026-09-28, PLAN §6 F16). A mob
     * with no such attribute (many passive mobs) deals 0 custom melee, which is correct: it had no vanilla
     * melee to bridge either.
     *
     * <p><b>Never {@code attr.getValue()}.</b> This runs inside {@code EntityAddToWorldEvent}, and vanilla
     * has folded the held weapon into the live value only if a player paired with the mob first
     * ({@code ServerEntity.sendPairingData} calls {@code detectEquipmentUpdates}). The live value was a
     * fact about who stood nearby. The weapon's modifiers are read from the ITEM instead: its full
     * {@code ATTRIBUTE_MODIFIERS} component, filtered to {@code ATTACK_DAMAGE} entries whose slot group
     * covers the main hand.
     */
    private double attackDamageOf(LivingEntity mob) {
        AttributeInstance attr = mob.getAttribute(Attribute.ATTACK_DAMAGE);
        if (attr == null) return 0.0;
        MeleeSeed.Seed seed = MeleeSeed.attack(attr.getBaseValue(), mainHandAttackModifiers(mob));
        if (seed.unpriced() > 0) {
            log.warning("mob " + mob.getUniqueId() + " (" + mob.getType().key() + ") holds a weapon with "
                    + seed.unpriced() + " non-ADD attack modifier(s), which the seed leaves out (MeleeSeed)");
        }
        return seed.attack();
    }

    /** The held main-hand item's attack-damage modifiers that apply to the main hand. */
    private static java.util.List<MeleeSeed.Modifier> mainHandAttackModifiers(LivingEntity mob) {
        var equipment = mob.getEquipment();
        if (equipment == null) return java.util.List.of();
        var hand = equipment.getItemInMainHand();
        if (hand.isEmpty()) return java.util.List.of();
        var modifiers = hand.getData(io.papermc.paper.datacomponent.DataComponentTypes.ATTRIBUTE_MODIFIERS);
        if (modifiers == null) return java.util.List.of();
        java.util.List<MeleeSeed.Modifier> out = new java.util.ArrayList<>();
        for (var entry : modifiers.modifiers()) {
            if (!Attribute.ATTACK_DAMAGE.equals(entry.attribute())) continue;
            if (!entry.getGroup().test(org.bukkit.inventory.EquipmentSlot.HAND)) continue;
            var op = entry.modifier().getOperation() == org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER
                    ? MeleeSeed.Operation.ADD_VALUE : MeleeSeed.Operation.OTHER;
            out.add(new MeleeSeed.Modifier(op, entry.modifier().getAmount()));
        }
        return out;
    }

    /**
     * A mob's cached nameplate: its base name (fixed), and the current text + version held atomically so
     * a viewer loop reads a consistent (text, version) pair even while onChange rebuilds from another
     * thread. Version bumps on each rebuild so viewers know to resend the text.
     */
    static final class Nameplate {   // package-private so the register-if-absent test can read it

        private record Versioned(Component text, long version) {}

        private final Component baseName;
        private final AtomicReference<Versioned> current;

        Nameplate(Component baseName, Component text) {
            this.baseName = baseName;
            this.current = new AtomicReference<>(new Versioned(text, 1));
        }

        Component baseName() {
            return baseName;
        }

        void update(Component text) {
            current.updateAndGet(v -> new Versioned(text, v.version() + 1));
        }

        /** Atomic (text, version) pair -- the viewer loop reads both consistently across a concurrent update. */
        Versioned snapshot() {
            return current.get();
        }

        /** For tests: the current version / text (not the atomic pair the loop needs). */
        long version() {
            return current.get().version();
        }

        Component text() {
            return current.get().text();
        }
    }
}
