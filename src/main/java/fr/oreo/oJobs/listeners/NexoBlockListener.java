package fr.oreo.oJobs.listeners;

import com.nexomc.nexo.api.NexoBlocks;
import com.nexomc.nexo.api.NexoFurniture;
import com.nexomc.nexo.api.events.custom_block.NexoBlockBreakEvent;
import com.nexomc.nexo.api.events.furniture.NexoFurnitureBreakEvent;
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic;
import fr.oreo.oJobs.OJobs;
import fr.oreo.oJobs.models.ActionEntry;
import fr.oreo.oJobs.models.Job;
import fr.oreo.oJobs.models.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Optional;


public class NexoBlockListener implements Listener {

    private final OJobs plugin;

    public NexoBlockListener(OJobs plugin) {
        this.plugin = plugin;
    }


    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onNexoBlockBreak(NexoBlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;

        var mechanic = NexoBlocks.customBlockMechanic(event.getBlock());
        if (mechanic == null) return;

        String itemId = mechanic.getItemID().toLowerCase();
        String world  = player.getWorld().getName();

        processAction(player, world, itemId, "nexo-block-break");
    }


    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onNexoFurnitureBreak(NexoFurnitureBreakEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;

        FurnitureMechanic mechanic = NexoFurniture.furnitureMechanic(event.getBaseEntity());
        if (mechanic == null) return;

        String itemId = mechanic.getItemID().toLowerCase();
        String world  = player.getWorld().getName();

        processAction(player, world, itemId, "nexo-furniture");
    }


    private void processAction(Player player, String world, String itemId, String actionMapKey) {
        Optional<PlayerData> dataOpt = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (dataOpt.isEmpty()) return;
        PlayerData data = dataOpt.get();

        for (Job job : plugin.getJobManager().getJobs()) {
            if (!data.hasJob(job.getId())) continue;
            if (!job.isWorldAllowed(world)) continue;

            String cooldownKey = job.getId() + ":" + actionMapKey;
            if (data.isOnCooldown(cooldownKey)) continue;

            Optional<ActionEntry> entryOpt = actionMapKey.equals("nexo-block-break")
                    ? job.getNexoBlockBreakEntry(itemId)
                    : job.getNexoFurnitureEntry(itemId);

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