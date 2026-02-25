package fr.oreo.oJobs.listeners;

import fr.oreo.oJobs.OJobs;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;


public class PlayerListener implements Listener {

    private final OJobs plugin;

    public PlayerListener(OJobs plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        var uuid = event.getPlayer().getUniqueId();
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            var data = plugin.getPlayerDataManager().loadPlayer(uuid);
            if (data != null) {
                data.setPlayerName(event.getPlayer().getName());
                data.setDirty(true);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        plugin.getPlayerDataManager().unloadPlayer(event.getPlayer().getUniqueId());
    }
}
