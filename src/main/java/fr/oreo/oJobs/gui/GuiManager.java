package fr.oreo.oJobs.gui;

import fr.minuskube.inv.InventoryManager;
import fr.minuskube.inv.SmartInventory;
import fr.oreo.oJobs.OJobs;
import fr.oreo.oJobs.models.Job;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Map;


public class GuiManager {

    private final OJobs plugin;
    private final InventoryManager invManager;
    private final GuiHelper helper;

    public GuiManager(OJobs plugin) {
        this.plugin = plugin;
        this.helper = new GuiHelper(plugin);

        this.invManager = new InventoryManager(plugin);
        this.invManager.init();
    }


    private String parseTitle(String raw) {
        return LegacyComponentSerializer.legacySection()
                .serialize(MiniMessage.miniMessage().deserialize(raw));
    }

    public void openMainMenu(Player player) {
        openMainMenu(player, 0);
    }

    public void openMainMenu(Player player, int page) {
        FileConfiguration gui = plugin.getConfigManager().getGuiConfig();
        String title = gui.getString("main-menu.title", "<gold>Jobs");
        int rows     = gui.getInt("main-menu.size", 54) / 9;

        SmartInventory.builder()
                .id("ojobs-main")
                .title(parseTitle(title))
                .size(rows, 9)
                .provider(new JobsMainGui(plugin))
                .manager(invManager)
                .build()
                .open(player, page);
    }

    public void openJobMenu(Player player, Job job) {
        FileConfiguration gui = plugin.getConfigManager().getGuiConfig();
        String title = gui.getString("job-menu.title", "<gold>%job_name%")
                .replace("%job_name%", job.getDisplayName());
        int rows = gui.getInt("job-menu.size", 54) / 9;

        SmartInventory.builder()
                .id("ojobs-job-" + job.getId())
                .title(parseTitle(title))
                .size(rows, 9)
                .provider(new JobDetailGui(plugin, job))
                .manager(invManager)
                .build()
                .open(player, Map.of("job", job));
    }

    public void openLeaderboard(Player player) {
        openLeaderboard(player, "global");
    }

    public void openLeaderboard(Player player, String jobId) {
        FileConfiguration gui = plugin.getConfigManager().getGuiConfig();
        String title = gui.getString("leaderboard-menu.title", "<gold>\uD83C\uDFC6 Leaderboard");
        int rows     = gui.getInt("leaderboard-menu.size", 54) / 9;

        SmartInventory.builder()
                .id("ojobs-leaderboard")
                .title(parseTitle(title))
                .size(rows, 9)
                .provider(new LeaderboardGui(plugin, jobId))
                .manager(invManager)
                .build()
                .open(player);
    }

    public void openRewardsGui(Player player, Job job, int prestige) {
        openRewardsGui(player, job, prestige, 0);
    }

    public void openRewardsGui(Player player, Job job, int prestige, int page) {
        FileConfiguration gui = plugin.getConfigManager().getGuiConfig();
        String rawTitle = gui.getString("rewards-menu.title", "<aqua>📖 %job_name% Rewards")
                .replace("%job_name%", job.getDisplayName())
                .replace("%prestige%",  String.valueOf(prestige));
        int rows = gui.getInt("rewards-menu.size", 54) / 9;

        SmartInventory.builder()
                .id("ojobs-rewards-" + job.getId() + "-p" + prestige)
                .title(parseTitle(rawTitle))
                .size(rows, 9)
                .provider(new RewardsGui(plugin, job, prestige))
                .manager(invManager)
                .build()
                .open(player, page);
    }

    public void openAdminGui(Player player) {
        FileConfiguration gui = plugin.getConfigManager().getGuiConfig();
        String title = gui.getString("admin-menu.title", "<red>oJobs Admin");
        int rows     = gui.getInt("admin-menu.size", 54) / 9;

        SmartInventory.builder()
                .id("ojobs-admin")
                .title(parseTitle(title))
                .size(rows, 9)
                .provider(new AdminGui(plugin))
                .manager(invManager)
                .build()
                .open(player);
    }

    public InventoryManager getInvManager() { return invManager; }
    public GuiHelper getHelper()            { return helper; }
}