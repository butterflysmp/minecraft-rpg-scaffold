package io.github.yourname.rpg.paper;

import com.github.retrooper.packetevents.PacketEvents;
import io.github.yourname.rpg.core.ability.AbilityRegistry;
import io.github.yourname.rpg.core.ability.AbilityService;
import io.github.yourname.rpg.core.combat.CooldownTracker;
import io.github.yourname.rpg.paper.adapter.Keys;
import io.github.yourname.rpg.paper.command.RpgCommand;
import io.github.yourname.rpg.paper.content.AbilityLoader;
import io.github.yourname.rpg.paper.listener.RpgListeners;
import io.github.yourname.rpg.paper.packet.ExampleTelegraphListener;
import io.github.yourname.rpg.paper.scheduler.PaperScheduler;
import io.github.yourname.rpg.paper.scheduler.Scheduler;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class RpgPlugin extends JavaPlugin {

    private Scheduler scheduler;
    private Keys keys;
    private AbilityRegistry abilities;
    private CooldownTracker cooldowns;
    private AbilityService abilityService;

    @Override
    public void onEnable() {
        this.scheduler = new PaperScheduler(this);

        // Every NamespacedKey in the plugin, built once. Never inline at a call site.
        this.keys = new Keys(this);

        // Content: YAML -> AbilityDefinition. No abilities are hardcoded.
        saveResource("content/abilities/solar_grenade.yml", false);
        File abilitiesDir = new File(getDataFolder(), "content/abilities");
        this.abilities = new AbilityLoader().loadAll(abilitiesDir);
        getLogger().info("Loaded " + abilities.size() + " abilities");

        // core takes a tick supplier, not Bukkit, so it stays unit-testable.
        this.cooldowns = new CooldownTracker(Bukkit::getCurrentTick);
        this.abilityService = new AbilityService(abilities, cooldowns);

        // The one and only registerEvents call. Keep it that way.
        getServer().getPluginManager().registerEvents(new RpgListeners(cooldowns), this);

        // PacketEvents is a SEPARATE PLUGIN on the server, declared in
        // paper-plugin.yml. We do NOT call PacketEvents.setAPI() or .load()
        // here -- that is only for shaded builds, and shading it would drag
        // GPL-3.0 onto this project.
        PacketEvents.getAPI().getEventManager()
                .registerListener(new ExampleTelegraphListener(scheduler));

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
                event.registrar().register(RpgCommand.build(abilities), "RPG commands"));
    }

    @Override
    public void onDisable() {
        // PacketEvents terminates itself; it owns its own lifecycle.
    }

    public Scheduler scheduler() { return scheduler; }
    public Keys keys() { return keys; }
    public AbilityRegistry abilities() { return abilities; }
    public CooldownTracker cooldowns() { return cooldowns; }

    /**
     * cast() only decides; the caller must run the returned effects on the
     * region thread owning the impact point. See AbilityService.
     */
    public AbilityService abilityService() { return abilityService; }
}
