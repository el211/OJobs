package fr.oreo.oJobs.gui;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import fr.oreo.oJobs.OJobs;
import fr.oreo.oJobs.models.Job;
import fr.oreo.oJobs.models.LevelReward;
import fr.oreo.oJobs.models.PlayerJobData;
import fr.oreo.oJobs.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.inventory.ItemStack;

import java.util.*;

import static fr.oreo.oJobs.gui.GuiHelper.col;
import static fr.oreo.oJobs.gui.GuiHelper.row;

public class RewardsGui implements InventoryProvider {

    private final OJobs plugin;
    private final Job job;
    private final GuiHelper helper;
    private final int viewingPrestige;

    public RewardsGui(OJobs plugin, Job job, int viewingPrestige) {
        this.plugin = plugin;
        this.job = job;
        this.helper = plugin.getGuiManager().getHelper();
        this.viewingPrestige = viewingPrestige;
    }

    @Override
    public void init(Player player, InventoryContents contents) {
        FileConfiguration gui = plugin.getConfigManager().getGuiConfig();

        helper.fillEmpty(contents, "rewards-menu");

        List<Integer> rewardSlots = new ArrayList<>(gui.getIntegerList("rewards-menu.reward-slots"));
        if (rewardSlots.isEmpty()) {
            for (int s = 9; s <= 35; s++) rewardSlots.add(s);
        }

        int page = contents.pagination().getPage();
        int pageSize = rewardSlots.size();

        int playerLevel = plugin.getPlayerDataManager()
                .getPlayerData(player.getUniqueId())
                .flatMap(d -> d.getJobData(job.getId()))
                .map(PlayerJobData::getLevel)
                .orElse(0);

        int playerPrestige = plugin.getPlayerDataManager()
                .getPlayerData(player.getUniqueId())
                .flatMap(d -> d.getJobData(job.getId()))
                .map(PlayerJobData::getPrestige)
                .orElse(0);

        Map<Integer, LevelReward> selectedRewards = job.getRewardsForPrestige(viewingPrestige);
        if (selectedRewards.isEmpty() && viewingPrestige == 0) {
            selectedRewards = job.getLevelRewards();
        }

        List<Map.Entry<Integer, LevelReward>> rewards = new ArrayList<>(selectedRewards.entrySet());
        rewards.sort(Map.Entry.comparingByKey());

        int start = page * pageSize;
        int totalPages = Math.max(1, (int) Math.ceil((double) rewards.size() / pageSize));

        if (rewards.isEmpty()) {
            int infoSlot = 22;
            ItemStack info = ItemBuilder.of(Material.BARRIER)
                    .name("<red>No rewards configured")
                    .lore(List.of(
                            "<gray>Job: <white>" + job.getDisplayName(),
                            "<gray>Prestige tab: <white>" + viewingPrestige,
                            "",
                            "<yellow>Fix:",
                            "<gray>- Use <white>rewards:</white> (prestige -> level)",
                            "<gray>- or <white>level-rewards:</white> (base rewards)"
                    ))
                    .hideAttributes()
                    .build();
            contents.set(row(infoSlot), col(infoSlot), ClickableItem.empty(info));
        }

        for (int i = 0; i < pageSize; i++) {
            int rewardIndex = start + i;
            if (rewardIndex >= rewards.size()) break;

            int slot = rewardSlots.get(i);

            Map.Entry<Integer, LevelReward> entry = rewards.get(rewardIndex);
            int level = entry.getKey();
            LevelReward reward = entry.getValue();

            boolean prestigeUnlocked = playerPrestige >= viewingPrestige;
            boolean levelUnlocked = playerPrestige > viewingPrestige || (playerPrestige == viewingPrestige && playerLevel >= level);
            boolean unlocked = prestigeUnlocked && levelUnlocked;

            ItemStack icon = buildRewardIcon(reward, level, unlocked, gui);

            contents.set(row(slot), col(slot), ClickableItem.from(icon, click -> {
                cancel(click);

                if (!prestigeUnlocked) {
                    player.sendMessage("§cLocked. You need Prestige " + viewingPrestige + " for this tab.");
                    return;
                }
                if (!levelUnlocked) {
                    player.sendMessage("§cLocked. Reach level " + level + " to unlock.");
                    return;
                }

                player.sendMessage("§aReward for level " + level + " (Prestige " + viewingPrestige + ") in " + job.getDisplayName() + "!");
            }));
        }

        int maxPrestige = plugin.getConfigManager().getConfig().getInt("prestige.max-prestige", 10);
        List<Integer> tabSlots = new ArrayList<>(gui.getIntegerList("rewards-menu.prestige-tab-slots"));
        if (tabSlots.isEmpty()) {
            for (int s = 0; s <= 8; s++) tabSlots.add(s);
        }

        if (!tabSlots.isEmpty()) {
            boolean baseSelected = viewingPrestige == 0;

            Material baseMat = baseSelected ? Material.BOOK : Material.WRITABLE_BOOK;
            ItemBuilder baseB = ItemBuilder.of(baseMat)
                    .name(baseSelected ? "<gold><bold>Base Rewards" : "<yellow>Base Rewards")
                    .loreLine("<gray>Level rewards with no prestige");
            if (baseSelected) baseB.glow();

            int bSlot = tabSlots.get(0);
            contents.set(row(bSlot), col(bSlot), ClickableItem.from(baseB.build(), click -> {
                cancel(click);
                if (viewingPrestige != 0) plugin.getGuiManager().openRewardsGui(player, job, 0);
            }));

            for (int p = 1; p <= maxPrestige && p < tabSlots.size(); p++) {
                int prestige = p;
                boolean selected = viewingPrestige == prestige;

                Material mat = selected ? Material.NETHER_STAR : Material.GHAST_TEAR;
                ItemBuilder b = ItemBuilder.of(mat)
                        .name(selected ? "<gold><bold>✦ Prestige " + prestige : "<gray>✦ Prestige " + prestige)
                        .loreLine("<gray>Rewards after prestiging " + prestige + "x");
                if (selected) b.glow();

                int tSlot = tabSlots.get(p);
                contents.set(row(tSlot), col(tSlot), ClickableItem.from(b.build(), click -> {
                    cancel(click);
                    if (viewingPrestige != prestige) plugin.getGuiManager().openRewardsGui(player, job, prestige);
                }));
            }
        }

        int prevSlot = gui.getInt("rewards-menu.prev-slot", 45);
        int nextSlot = gui.getInt("rewards-menu.next-slot", 53);

        if (page > 0) {
            contents.set(row(prevSlot), col(prevSlot), ClickableItem.from(
                    ItemBuilder.of(Material.ARROW)
                            .name(gui.getString("rewards-menu.prev-name", "<yellow>◀ Previous"))
                            .hideAttributes()
                            .build(),
                    click -> {
                        cancel(click);
                        plugin.getGuiManager().openRewardsGui(player, job, viewingPrestige, page - 1);
                    }
            ));
        }

        if (page < totalPages - 1) {
            contents.set(row(nextSlot), col(nextSlot), ClickableItem.from(
                    ItemBuilder.of(Material.ARROW)
                            .name(gui.getString("rewards-menu.next-name", "<yellow>Next ▶"))
                            .hideAttributes()
                            .build(),
                    click -> {
                        cancel(click);
                        plugin.getGuiManager().openRewardsGui(player, job, viewingPrestige, page + 1);
                    }
            ));
        }

        int backSlot = gui.getInt("rewards-menu.back-slot", 49);
        contents.set(row(backSlot), col(backSlot), ClickableItem.from(
                ItemBuilder.of(Material.ARROW)
                        .name(gui.getString("rewards-menu.back-name", "<yellow>◀ Back"))
                        .hideAttributes()
                        .build(),
                click -> {
                    cancel(click);
                    plugin.getGuiManager().openJobMenu(player, job);
                }
        ));
    }

    @Override
    public void update(Player player, InventoryContents contents) {
    }

    private void cancel(Object clickData) {
        try {
            Object event = clickData.getClass().getMethod("getEvent").invoke(clickData);
            if (event instanceof Cancellable cancellable) cancellable.setCancelled(true);
        } catch (Throwable ignored) {
        }
    }

    private ItemStack buildRewardIcon(LevelReward reward, int level, boolean unlocked, FileConfiguration gui) {
        String matName = gui.getString("rewards-menu.reward-material-unlocked", "LIME_DYE");
        String lockedMat = gui.getString("rewards-menu.reward-material-locked", "GRAY_DYE");

        Material mat = Material.matchMaterial(unlocked ? matName : lockedMat);
        if (mat == null) mat = unlocked ? Material.LIME_DYE : Material.GRAY_DYE;

        List<String> loreTpl = gui.getStringList(unlocked
                ? "rewards-menu.reward-lore-unlocked"
                : "rewards-menu.reward-lore-locked");

        if (loreTpl.isEmpty()) {
            loreTpl = unlocked
                    ? List.of(
                    "<gray>Level: <green>%level%",
                    "",
                    "%money_line%",
                    "%items_line%",
                    "%commands_line%",
                    "%permissions_line%",
                    "",
                    "<dark_green>✔ Unlocked"
            )
                    : List.of(
                    "<gray>Level: <red>%level%",
                    "",
                    "%money_line%",
                    "%items_line%",
                    "%commands_line%",
                    "%permissions_line%",
                    "",
                    "<red>✘ Locked"
            );
        }

        List<String> lore = new ArrayList<>();
        for (String line : loreTpl) {
            switch (line) {
                case "%money_line%" -> {
                    if (reward.getMoney() > 0) lore.add("<gold>  💰 $" + (long) reward.getMoney());
                }
                case "%items_line%" -> {
                    for (String item : reward.getItems()) lore.add("<aqua>  📦 " + item);
                }
                case "%commands_line%" -> {
                    for (String cmd : reward.getCommands()) lore.add("<gray>  ⚡ /" + cmd);
                }
                case "%permissions_line%" -> {
                    for (String perm : reward.getPermissions()) lore.add("<light_purple>  🔑 " + perm);
                }
                default -> lore.add(line.replace("%level%", String.valueOf(level)));
            }
        }

        while (!lore.isEmpty() && lore.get(lore.size() - 1).isBlank()) {
            lore.remove(lore.size() - 1);
        }

        String nameTemplate = gui.getString(
                unlocked ? "rewards-menu.reward-name-unlocked" : "rewards-menu.reward-name-locked",
                unlocked ? "<green>Level %level% Reward" : "<red>Level %level% Reward"
        );

        ItemBuilder b = ItemBuilder.of(mat)
                .name(nameTemplate.replace("%level%", String.valueOf(level)))
                .lore(lore);

        if (unlocked) b.glow();
        return b.build();
    }
}