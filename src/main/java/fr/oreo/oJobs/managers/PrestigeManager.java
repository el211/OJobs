package fr.oreo.oJobs.managers;

import fr.oreo.oJobs.OJobs;
import fr.oreo.oJobs.models.Job;
import fr.oreo.oJobs.models.PlayerData;
import fr.oreo.oJobs.models.PlayerJobData;
import fr.oreo.oJobs.utils.MessageUtil;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;


public class PrestigeManager {

    private final OJobs plugin;

    public PrestigeManager(OJobs plugin) {
        this.plugin = plugin;
    }

    public boolean isEnabled() {
        return plugin.getConfigManager().getConfig().getBoolean("prestige.enabled", true);
    }


    public boolean canPrestige(PlayerData data, Job job) {
        if (!isEnabled()) return false;
        int maxPrestige = plugin.getConfigManager().getConfig().getInt("prestige.max-prestige", 10);
        return data.getJobData(job.getId()).map(jd -> {
            if (jd.getPrestige() >= maxPrestige) return false;
            return jd.getLevel() >= job.getMaxLevel();
        }).orElse(false);
    }


    public boolean prestige(Player player, Job job) {
        PlayerData data = plugin.getPlayerDataManager()
                .getPlayerData(player.getUniqueId()).orElse(null);
        if (data == null || !canPrestige(data, job)) return false;

        PlayerJobData jd = data.getJobData(job.getId()).get();
        int newPrestige = jd.getPrestige() + 1;
        jd.setPrestige(newPrestige);

        if (plugin.getConfigManager().getConfig().getBoolean("prestige.reset-level", true)) {
            jd.setLevel(1);
        }
        if (plugin.getConfigManager().getConfig().getBoolean("prestige.reset-xp", true)) {
            jd.setXp(0);
        }

        data.setDirty(true);

        grantPrestigeRewards(player, newPrestige);

        String titleRaw = plugin.getConfigManager().getLangConfig()
                .getString("messages.prestige-title", "<gold>⭐ Prestige!");
        String subRaw   = plugin.getConfigManager().getLangConfig()
                .getString("messages.prestige-subtitle", "<yellow>Prestige <gold>%prestige%");

        Map<String, String> repl = Map.of(
                "job",      job.getDisplayName(),
                "prestige", String.valueOf(newPrestige)
        );
        MessageUtil.sendTitle(player, titleRaw, subRaw, repl);
        MessageUtil.send(player, "prestige-success", repl);

        String broadcastKey = "prestige-broadcast";
        String broadcastRaw = plugin.getConfigManager().getLangConfig()
                .getString("messages." + broadcastKey, "");
        if (!broadcastRaw.isBlank()) {
            MessageUtil.broadcast(broadcastRaw, repl);
        }

        return true;
    }

    private void grantPrestigeRewards(Player player, int prestige) {
        var rewardsSec = plugin.getConfigManager().getConfig()
                .getConfigurationSection("prestige.rewards." + prestige);
        if (rewardsSec == null) return;

        double money = rewardsSec.getDouble("money", 0);
        if (money > 0 && plugin.isVaultEnabled()) {
            plugin.getEconomy().depositPlayer(player, money);
        }

        for (String cmd : rewardsSec.getStringList("commands")) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    cmd.replace("%player%", player.getName()));
        }
    }
}
