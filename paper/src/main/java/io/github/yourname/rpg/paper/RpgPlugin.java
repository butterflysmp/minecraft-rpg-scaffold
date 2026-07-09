package io.github.yourname.rpg.paper;

import com.github.retrooper.packetevents.PacketEvents;
import io.github.yourname.rpg.core.ability.AbilityRegistry;
import io.github.yourname.rpg.paper.command.RpgCommand;
import io.github.yourname.rpg.paper.content.AbilityLoader;
import io.github.yourname.rpg.paper.packet.ExampleTelegraphListener;
import io.github.yourname.rpg.paper.scheduler.PaperScheduler;
import io.github.yourname.rpg.paper.scheduler.Scheduler;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class RpgPlugin extends JavaPlugin {

    private Scheduler scheduler;
    private AbilityRegistry abilities;

    @Override
    public void onEnable() {
        this.scheduler = new PaperScheduler(this);

        // Content: YAML -> AbilityDefinition. No abilities are hardcoded.
        saveResource("content/abilities/solar_grenade.yml", false);
        File abilitiesDir = new File(getDataFolder(), "content/abilities");
        this.abilities = new AbilityLoader().loadAll(abilitiesDir);
        getLogger().info("Loaded " + abilities.size() + " abilities");

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
    public AbilityRegistry abilities() { return abilities; }
}
