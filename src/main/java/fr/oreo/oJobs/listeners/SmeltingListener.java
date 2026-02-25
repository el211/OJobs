package fr.oreo.oJobs.listeners;

import fr.oreo.oJobs.OJobs;
import fr.oreo.oJobs.models.ActionEntry;
import fr.oreo.oJobs.models.Job;
import fr.oreo.oJobs.models.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.FurnaceExtractEvent;

import java.util.Optional;


public class SmeltingListener implements Listener {

    private final OJobs plugin;

    public SmeltingListener(OJobs plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFurnaceExtract(FurnaceExtractEvent event) {
        Player player = event.getPlayer();
        String mat    = event.getItemType().name();
        String world  = player.getWorld().getName();
        int amount    = event.getItemAmount();

        Optional<PlayerData> dataOpt = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (dataOpt.isEmpty()) return;
        PlayerData data = dataOpt.get();

        for (Job job : plugin.getJobManager().getJobs()) {
            if (!data.hasJob(job.getId())) continue;
            if (!job.isWorldAllowed(world)) continue;

            String cooldownKey = job.getId() + ":smelting";
            if (data.isOnCooldown(cooldownKey)) continue;

            job.getSmeltingEntry(mat).ifPresent(entry -> {
                if (!meetsLevelReq(data, job, entry)) return;
                if (!entry.roll()) return;
                plugin.getPlayerDataManager().giveXp(player, job, entry.getXp() * amount);
                long cd = job.getCooldown("smelting");
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
