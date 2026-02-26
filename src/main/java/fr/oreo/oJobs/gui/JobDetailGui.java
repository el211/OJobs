package fr.oreo.oJobs.gui;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import fr.oreo.oJobs.OJobs;
import fr.oreo.oJobs.models.Job;
import fr.oreo.oJobs.models.LevelReward;
import fr.oreo.oJobs.models.PlayerData;
import fr.oreo.oJobs.models.PlayerJobData;
import fr.oreo.oJobs.utils.ItemBuilder;
import fr.oreo.oJobs.utils.MessageUtil;
import fr.oreo.oJobs.utils.XpUtil;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static fr.oreo.oJobs.gui.GuiHelper.col;
import static fr.oreo.oJobs.gui.GuiHelper.row;


public class JobDetailGui implements InventoryProvider {

    private final OJobs     plugin;
    private final Job       job;
    private final GuiHelper helper;

    public JobDetailGui(OJobs plugin, Job job) {
        this.plugin  = plugin;
        this.job     = job;
        this.helper  = plugin.getGuiManager().getHelper();
    }

    @Override
    public void init(Player player, InventoryContents contents) {
        FileConfiguration gui = plugin.getConfigManager().getGuiConfig();

        Optional<PlayerData>    dataOpt = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        boolean                 joined  = dataOpt.map(d -> d.hasJob(job.getId())).orElse(false);
        Optional<PlayerJobData> jdOpt   = dataOpt.flatMap(d -> d.getJobData(job.getId()));

        int    level       = jdOpt.map(PlayerJobData::getLevel).orElse(0);
        double xp          = jdOpt.map(PlayerJobData::getXp).orElse(0.0);
        int    prestige    = jdOpt.map(PlayerJobData::getPrestige).orElse(0);
        double xpReq       = joined ? XpUtil.getXpRequired(job, level) : 0;
        String progressBar = joined ? helper.buildProgressBarString(xp, xpReq) : "";

        helper.fillEmpty(contents, "job-menu");

        {
            int    slot    = gui.getInt("job-menu.stats-slot", 13);
            String matName = gui.getString("job-menu.stats-material", "BOOK");
            String name    = gui.getString("job-menu.stats-name", "<yellow>Stats")
                    .replace("%job_name%", job.getDisplayName());

            double       totalXp = jdOpt.map(PlayerJobData::getTotalXpEarned).orElse(0.0);
            List<String> loreTpl = gui.getStringList("job-menu.stats-lore");
            List<String> lore    = new ArrayList<>();
            for (String line : loreTpl) {
                lore.add(line
                        .replace("%level%",        String.valueOf(level))
                        .replace("%prestige%",     String.valueOf(prestige))
                        .replace("%xp%",           XpUtil.formatXp(xp))
                        .replace("%xp_required%",  XpUtil.formatXp(xpReq))
                        .replace("%progress_bar%", progressBar)
                        .replace("%total_xp%",     XpUtil.formatXp(totalXp))
                        .replace("%job_name%",     job.getDisplayName()));
            }

            Material mat = Material.matchMaterial(matName);
            if (mat == null) mat = Material.BOOK;

            contents.set(row(slot), col(slot),
                    ClickableItem.empty(ItemBuilder.of(mat).name(name).lore(lore).build()));
        }

        {
            int    slot    = gui.getInt("job-menu.rewards-slot", 31);
            String matName = gui.getString("job-menu.rewards-material", "CHEST");
            String name    = gui.getString("job-menu.rewards-name", "<yellow>Rewards");

            int nextRewardLevel = job.getLevelRewards().keySet().stream()
                    .filter(l -> l > level)
                    .min(Integer::compareTo)
                    .orElse(-1);

            List<String> rewardList = new ArrayList<>();
            if (nextRewardLevel > 0) {
                LevelReward nr = job.getLevelRewards().get(nextRewardLevel);
                if (nr.getMoney() > 0)
                    rewardList.add("<gold>$" + (long) nr.getMoney());
                nr.getItems().forEach(i       -> rewardList.add("<aqua>" + i));
                nr.getCommands().forEach(c    -> rewardList.add("<gray>Command"));
                nr.getPermissions().forEach(p -> rewardList.add("<light_purple>" + p));
            }

            List<String> loreTpl = gui.getStringList("job-menu.rewards-lore");
            List<String> lore    = new ArrayList<>();
            for (String line : loreTpl) {
                if (line.contains("%reward_list%")) {
                    rewardList.forEach(r -> lore.add("  " + r));
                } else {
                    lore.add(line.replace("%next_reward_level%",
                            nextRewardLevel > 0 ? String.valueOf(nextRewardLevel) : "None"));
                }
            }

            Material mat = Material.matchMaterial(matName);
            if (mat == null) mat = Material.CHEST;

            contents.set(row(slot), col(slot),
                    ClickableItem.empty(ItemBuilder.of(mat).name(name).lore(lore).build()));
        }

        {
            int    slot    = gui.getInt("job-menu.all-rewards-slot", 32);
            String matName = gui.getString("job-menu.all-rewards-material", "KNOWLEDGE_BOOK");
            String name    = gui.getString("job-menu.all-rewards-name", "<aqua>\uD83D\uDCD6 All Level Rewards");

            List<String> loreTpl = gui.getStringList("job-menu.all-rewards-lore");
            List<String> lore    = new ArrayList<>();
            if (loreTpl.isEmpty()) {
                lore.add("<gray>Browse every level reward");
                lore.add("<gray>for <white>" + job.getDisplayName() + "<gray>.");
                lore.add("");
                lore.add("<yellow>Click to open!");
            } else {
                for (String line : loreTpl) {
                    lore.add(line.replace("%job_name%", job.getDisplayName()));
                }
            }

            Material mat = Material.matchMaterial(matName);
            if (mat == null) mat = Material.KNOWLEDGE_BOOK;

            contents.set(row(slot), col(slot),
                    ClickableItem.from(
                            ItemBuilder.of(mat).name(name).lore(lore).build(),
                            e -> {
                                cancelClick(e);
                                plugin.getGuiManager().openRewardsGui(player, job, 0);
                            }));
        }

        {
            int slot = gui.getInt("job-menu.leave-slot", 49);

            if (joined) {
                String   matName = gui.getString("job-menu.leave-material", "BARRIER");
                String   name    = gui.getString("job-menu.leave-name", "<red>Leave Job")
                        .replace("%job_name%", job.getDisplayName());
                List<String> lore = new ArrayList<>(gui.getStringList("job-menu.leave-lore"));
                lore.replaceAll(l -> l.replace("%job_name%", job.getDisplayName()));
                Material mat = Material.matchMaterial(matName);
                if (mat == null) mat = Material.BARRIER;

                contents.set(row(slot), col(slot),
                        ClickableItem.from(
                                ItemBuilder.of(mat).name(name).lore(lore).build(),
                                e -> {
                                    cancelClick(e);
                                    plugin.getPlayerDataManager().leaveJob(player, job);
                                    plugin.getGuiManager().openMainMenu(player);
                                }));
            } else {
                Material mat = Material.matchMaterial(job.getIcon());
                if (mat == null) mat = Material.EMERALD;

                String joinName = gui.getString("job-menu.join-name", "<green>Join %job_name%")
                        .replace("%job_name%", job.getDisplayName());
                List<String> joinLore = new ArrayList<>(gui.getStringList("job-menu.join-lore"));
                if (joinLore.isEmpty()) joinLore = new ArrayList<>(List.of("<gray>Click to join this job!"));
                joinLore.replaceAll(l -> l.replace("%job_name%", job.getDisplayName()));

                contents.set(row(slot), col(slot),
                        ClickableItem.from(
                                ItemBuilder.of(mat).name(joinName).lore(joinLore).build(),
                                e -> {
                                    cancelClick(e);
                                    plugin.getPlayerDataManager().joinJob(player, job);
                                    plugin.getGuiManager().openJobMenu(player, job);
                                }));
            }
        }

        {
            int    slot    = gui.getInt("job-menu.back-slot", 45);
            String matName = gui.getString("job-menu.back-material", "ARROW");
            String name    = gui.getString("job-menu.back-name", "<yellow>\u25C0 Back");
            Material mat = Material.matchMaterial(matName);
            if (mat == null) mat = Material.ARROW;

            contents.set(row(slot), col(slot),
                    ClickableItem.from(
                            ItemBuilder.of(mat).name(name).hideAttributes().build(),
                            e -> {
                                cancelClick(e);
                                plugin.getGuiManager().openMainMenu(player);
                            }));
        }

        if (plugin.getPrestigeManager().isEnabled() && joined) {
            int    slot    = gui.getInt("job-menu.prestige-slot", 53);
            String matName = gui.getString("job-menu.prestige-material", "NETHER_STAR");
            String name    = gui.getString("job-menu.prestige-name", "<gold>Prestige");

            List<String> lore = new ArrayList<>(gui.getStringList("job-menu.prestige-lore"));
            lore.replaceAll(l -> l
                    .replace("%prestige%",  String.valueOf(prestige))
                    .replace("%max_level%", String.valueOf(job.getMaxLevel())));

            boolean  canPrestige = dataOpt.map(d -> plugin.getPrestigeManager().canPrestige(d, job)).orElse(false);
            Material mat         = Material.matchMaterial(matName);
            if (mat == null) mat = Material.NETHER_STAR;

            ItemBuilder b = ItemBuilder.of(mat).name(name).lore(lore);
            if (canPrestige) b.glow();

            contents.set(row(slot), col(slot),
                    ClickableItem.from(b.build(), e -> {
                        cancelClick(e);
                        plugin.getPlayerDataManager().getPlayerData(player.getUniqueId()).ifPresent(d -> {
                            if (plugin.getPrestigeManager().canPrestige(d, job)) {
                                plugin.getPrestigeManager().prestige(player, job);
                                plugin.getGuiManager().openJobMenu(player, job);
                            } else {
                                MessageUtil.send(player, "prestige-not-ready", Map.of(
                                        "job",   job.getDisplayName(),
                                        "level", String.valueOf(job.getMaxLevel())));
                            }
                        });
                    }));
        }
    }

    @Override
    public void update(Player player, InventoryContents contents) {}

    private void cancelClick(fr.minuskube.inv.ItemClickData e) {
        if (e.getEvent() instanceof org.bukkit.event.Cancellable c) c.setCancelled(true);
    }
}