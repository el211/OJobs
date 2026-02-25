package fr.oreo.oJobs.listeners;

import dev.lone.itemsadder.api.Events.CustomBlockBreakEvent;
import dev.lone.itemsadder.api.Events.FurnitureBreakEvent;
import fr.oreo.oJobs.OJobs;
import fr.oreo.oJobs.models.ActionEntry;
import fr.oreo.oJobs.models.Job;
import fr.oreo.oJobs.models.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Optional;


public class ItemsAdderBlockListener implements Listener {

    private final OJobs plugin;

    public ItemsAdderBlockListener(OJobs plugin) {
        this.plugin = plugin;
    }


    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCustomBlockBreak(CustomBlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;

        String namespacedId = event.getNamespacedID().toLowerCase();
        String world        = player.getWorld().getName();

        processAction(player, world, namespacedId, "ia-block-break");
    }


    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFurnitureBreak(FurnitureBreakEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;

        var furniture = event.getFurniture();
        if (furniture == null) return;

        String namespacedId = furniture.getNamespacedID().toLowerCase();
        String world        = player.getWorld().getName();

        processAction(player, world, namespacedId, "ia-furniture");
    }


    private void processAction(Player player, String world, String namespacedId, String actionMapKey) {
        Optional<PlayerData> dataOpt = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (dataOpt.isEmpty()) return;
        PlayerData data = dataOpt.get();

        for (Job job : plugin.getJobManager().getJobs()) {
            if (!data.hasJob(job.getId())) continue;
            if (!job.isWorldAllowed(world)) continue;

            String cooldownKey = job.getId() + ":" + actionMapKey;
            if (data.isOnCooldown(cooldownKey)) continue;

            Optional<ActionEntry> entryOpt = actionMapKey.equals("ia-block-break")
                    ? job.getIaBlockBreakEntry(namespacedId)
                    : job.getIaFurnitureEntry(namespacedId);

            entryOpt.ifPresent(entry -> {
                if (!meetsLevelReq(data, job, entry)) return;
                if (!entry.roll()) return;
                plugin.getPlayerDataManager().giveXp(player, job, entry.getXp());
                long cd = job.getCooldown(actionMapKey);
                if (cd > 0) data.setCooldown(cooldownKey, cd);
            });
        }
    }

    private boolean meetsLevelReq(PlayerData data, Job job, ActionEntry entry) {
        if (entry.getLevelRequirement() <= 0) return true;
        return data.getJobData(job.getId())
                .map(jd -> jd.getLevel() >= entry.getLevelRequirement())
                .orElse(false);
    }
}