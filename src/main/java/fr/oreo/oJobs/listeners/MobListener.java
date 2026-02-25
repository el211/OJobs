package fr.oreo.oJobs.listeners;

import fr.oreo.oJobs.OJobs;
import fr.oreo.oJobs.models.ActionEntry;
import fr.oreo.oJobs.models.Job;
import fr.oreo.oJobs.models.PlayerData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.Optional;


public class MobListener implements Listener {

    private final OJobs plugin;

    public MobListener(OJobs plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();
        if (killer == null) return;

        String entityType = entity.getType().name();
        String world      = killer.getWorld().getName();

        Optional<PlayerData> dataOpt = plugin.getPlayerDataManager().getPlayerData(killer.getUniqueId());
        if (dataOpt.isEmpty()) return;
        PlayerData data = dataOpt.get();

        for (Job job : plugin.getJobManager().getJobs()) {
            if (!data.hasJob(job.getId())) continue;
            if (!job.isWorldAllowed(world)) continue;

            String cooldownKey = job.getId() + ":mob-kill";
            if (data.isOnCooldown(cooldownKey)) continue;

            job.getMobKillEntry(entityType).ifPresent(entry -> {
                if (!meetsLevelReq(data, job, entry)) return;
                if (!entry.roll()) return;
                plugin.getPlayerDataManager().giveXp(killer, job, entry.getXp());
                long cd = job.getCooldown("mob-kill");
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
