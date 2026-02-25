package fr.oreo.oJobs.listeners;

import fr.oreo.oJobs.OJobs;
import fr.oreo.oJobs.models.ActionEntry;
import fr.oreo.oJobs.models.Job;
import fr.oreo.oJobs.models.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;


public class FishingListener implements Listener {

    private final OJobs plugin;

    public FishingListener(OJobs plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;

        Player player = event.getPlayer();
        String world  = player.getWorld().getName();

        String caughtKey = "FISH"; // generic fallback
        if (event.getCaught() instanceof org.bukkit.entity.Item itemEntity) {
            ItemStack stack = itemEntity.getItemStack();
            caughtKey = stack.getType().name();
        }
        final String lookupKey = caughtKey;

        Optional<PlayerData> dataOpt = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (dataOpt.isEmpty()) return;
        PlayerData data = dataOpt.get();

        for (Job job : plugin.getJobManager().getJobs()) {
            if (!data.hasJob(job.getId())) continue;
            if (!job.isWorldAllowed(world)) continue;

            String cooldownKey = job.getId() + ":fishing";
            if (data.isOnCooldown(cooldownKey)) continue;

            ActionEntry entry = job.getFishingEntry(lookupKey)
                    .or(() -> job.getFishingEntry("FISH"))
                    .orElse(null);

            if (entry == null) continue;
            if (!meetsLevelReq(data, job, entry)) continue;
            if (!entry.roll()) continue;

            plugin.getPlayerDataManager().giveXp(player, job, entry.getXp());
            long cd = job.getCooldown("fishing");
            if (cd > 0) data.setCooldown(cooldownKey, cd);
        }
    }

    private boolean meetsLevelReq(PlayerData data, Job job, ActionEntry entry) {
        if (entry.getLevelRequirement() <= 0) return true;
        return data.getJobData(job.getId())
                .map(jd -> jd.getLevel() >= entry.getLevelRequirement())
                .orElse(false);
    }
}
