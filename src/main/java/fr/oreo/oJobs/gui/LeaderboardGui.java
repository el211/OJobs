package fr.oreo.oJobs.gui;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import fr.oreo.oJobs.OJobs;
import fr.oreo.oJobs.models.Job;
import fr.oreo.oJobs.models.LeaderboardEntry;
import fr.oreo.oJobs.utils.ItemBuilder;
import fr.oreo.oJobs.utils.XpUtil;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

import static fr.oreo.oJobs.gui.GuiHelper.col;
import static fr.oreo.oJobs.gui.GuiHelper.row;


public class LeaderboardGui implements InventoryProvider {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final OJobs plugin;
    private final String currentJobId;
    private final GuiHelper helper;

    public LeaderboardGui(OJobs plugin, String jobId) {
        this.plugin       = plugin;
        this.currentJobId = jobId;
        this.helper       = plugin.getGuiManager().getHelper();
    }

    @Override
    public void init(Player player, InventoryContents contents) {
        FileConfiguration gui = plugin.getConfigManager().getGuiConfig();

        helper.fillEmpty(contents, "leaderboard-menu");

        List<Integer> tabSlots = gui.getIntegerList("leaderboard-menu.tab-slots");
        List<Job> jobs = new ArrayList<>(plugin.getJobManager().getJobs());

        if (!tabSlots.isEmpty()) {
            boolean globalSelected = currentJobId.equals("global");
            Material globalMat = globalSelected ? Material.NETHER_STAR : Material.COMPASS;
            ItemBuilder gb = ItemBuilder.of(globalMat).name("<gold>Global");
            if (globalSelected) gb.glow();

            int gSlot = tabSlots.get(0);
            contents.set(row(gSlot), col(gSlot),
                    ClickableItem.from(gb.build(), e -> {
                        if (e.getEvent() instanceof org.bukkit.event.Cancellable c) c.setCancelled(true);
                        if (!currentJobId.equals("global"))
                            plugin.getGuiManager().openLeaderboard(player, "global");
                    }));

            for (int i = 0; i < jobs.size() && i + 1 < tabSlots.size(); i++) {
                Job job = jobs.get(i);
                Material mat = Material.matchMaterial(job.getIcon());
                if (mat == null) mat = Material.PAPER;
                boolean selected = job.getId().equals(currentJobId);

                ItemBuilder b = ItemBuilder.of(mat).name(job.getColor() + job.getDisplayName());
                if (selected) b.glow();

                int tSlot = tabSlots.get(i + 1);
                final String jobId = job.getId();
                contents.set(row(tSlot), col(tSlot),
                        ClickableItem.from(b.build(), e -> {
                            if (e.getEvent() instanceof org.bukkit.event.Cancellable c) c.setCancelled(true);
                            if (!jobId.equals(currentJobId))
                                plugin.getGuiManager().openLeaderboard(player, jobId);
                        }));
            }
        }

        List<LeaderboardEntry> entries = currentJobId.equals("global")
                ? plugin.getLeaderboardManager().getGlobalLeaderboard()
                : plugin.getLeaderboardManager().getLeaderboard(currentJobId);

        int[] entrySlots = {10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34};

        for (int i = 0; i < entrySlots.length && i < entries.size(); i++) {
            LeaderboardEntry entry = entries.get(i);
            int rank = i + 1;
            int slot = entrySlots[i];

            ItemStack head = buildPlayerHead(entry, rank);
            contents.set(row(slot), col(slot), ClickableItem.empty(head));
        }

        int closeSlot = gui.getInt("leaderboard-menu.close-slot", 49);
        contents.set(row(closeSlot), col(closeSlot),
                ClickableItem.from(
                        ItemBuilder.of(Material.BARRIER)
                                .name(gui.getString("leaderboard-menu.close-name", "<red>Close"))
                                .build(),
                        e -> {
                            if (e.getEvent() instanceof org.bukkit.event.Cancellable c) c.setCancelled(true);
                            player.closeInventory();
                        }));

        int backSlot = gui.getInt("leaderboard-menu.back-slot", 45);
        contents.set(row(backSlot), col(backSlot),
                ClickableItem.from(
                        ItemBuilder.of(Material.ARROW)
                                .name(gui.getString("leaderboard-menu.back-name", "<yellow>\u25C0 Back"))
                                .build(),
                        e -> {
                            if (e.getEvent() instanceof org.bukkit.event.Cancellable c) c.setCancelled(true);
                            plugin.getGuiManager().openMainMenu(player);
                        }));
    }

    @Override
    public void update(Player player, InventoryContents contents) {
    }


    private ItemStack buildPlayerHead(LeaderboardEntry entry, int rank) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        if (skull.getItemMeta() instanceof SkullMeta meta) {
            OfflinePlayer op = plugin.getServer().getOfflinePlayer(entry.getUuid());
            meta.setOwningPlayer(op);

            String rankPrefix = getRankPrefix(rank);
            meta.displayName(MM.deserialize(rankPrefix + " " + entry.getPlayerName()));

            List<String> loreLines = new ArrayList<>();
            loreLines.add("<gray>Level: <yellow>" + entry.getLevel());
            loreLines.add("<gray>Total XP: <yellow>" + XpUtil.formatXp(entry.getTotalXpEarned()));
            if (entry.getPrestige() > 0) loreLines.add("<gold>Prestige: " + entry.getPrestige());

            meta.lore(loreLines.stream().map(MM::deserialize).toList());
            skull.setItemMeta(meta);
        }
        return skull;
    }

    private String getRankPrefix(int rank) {
        return switch (rank) {
            case 1  -> "<gold>\uD83E\uDD47 #1";
            case 2  -> "<gray>\uD83E\uDD48 #2";
            case 3  -> "<dark_red>\uD83E\uDD49 #3";
            default -> "<dark_gray>#" + rank;
        };
    }
}