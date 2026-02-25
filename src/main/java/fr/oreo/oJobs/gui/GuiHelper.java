package fr.oreo.oJobs.gui;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.content.InventoryContents;
import fr.oreo.oJobs.OJobs;
import fr.oreo.oJobs.models.Job;
import fr.oreo.oJobs.models.PlayerData;
import fr.oreo.oJobs.models.PlayerJobData;
import fr.oreo.oJobs.utils.ItemBuilder;
import fr.oreo.oJobs.utils.ProgressBar;
import fr.oreo.oJobs.utils.XpUtil;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


public class GuiHelper {

    private final OJobs plugin;

    public GuiHelper(OJobs plugin) {
        this.plugin = plugin;
    }

    public static int row(int slot) { return slot / 9; }
    public static int col(int slot) { return slot % 9; }


    public void fillSlots(InventoryContents contents, String guiSection) {
        FileConfiguration gui = plugin.getConfigManager().getGuiConfig();
        if (!gui.getBoolean(guiSection + ".filler.enabled", true)) return;

        String matName = gui.getString(guiSection + ".filler.material", "GRAY_STAINED_GLASS_PANE");
        Material mat   = Material.matchMaterial(matName);
        if (mat == null) mat = Material.GRAY_STAINED_GLASS_PANE;

        ItemStack filler = ItemBuilder.filler(mat);
        ClickableItem fillerItem = ClickableItem.empty(filler);

        for (int slot : gui.getIntegerList(guiSection + ".filler.slots")) {
            contents.set(row(slot), col(slot), fillerItem);
        }
    }

    public void fillEmpty(InventoryContents contents, String guiSection) {
        FileConfiguration gui = plugin.getConfigManager().getGuiConfig();
        if (!gui.getBoolean(guiSection + ".filler.enabled", true)) return;

        String matName = gui.getString(guiSection + ".filler.material", "BLACK_STAINED_GLASS_PANE");
        Material mat   = Material.matchMaterial(matName);
        if (mat == null) mat = Material.BLACK_STAINED_GLASS_PANE;

        ItemStack filler = ItemBuilder.filler(mat);
        contents.fill(ClickableItem.empty(filler));
    }


    public ItemStack buildJobIcon(Job job, Optional<PlayerData> data, boolean joined, Player player) {
        FileConfiguration gui = plugin.getConfigManager().getGuiConfig();

        Material mat = Material.matchMaterial(job.getIcon());
        if (mat == null) mat = Material.STONE;

        ItemBuilder builder = ItemBuilder.of(mat)
                .name(job.getColor() + job.getDisplayName(), player);

        if (job.getCustomModelData() >= 0) {
            builder.customModelData(job.getCustomModelData());
        }

        String loreKey = joined ? "main-menu.joined-lore" : "main-menu.not-joined-lore";
        List<String> loreTpl = gui.getStringList(loreKey);
        List<String> lore    = new ArrayList<>();

        String progressBar = "";
        String level = "1", xp = "0", xpRequired = "?";
        if (joined && data.isPresent()) {
            Optional<PlayerJobData> jdOpt = data.get().getJobData(job.getId());
            if (jdOpt.isPresent()) {
                PlayerJobData jd = jdOpt.get();
                double xpReq = XpUtil.getXpRequired(job, jd.getLevel());
                boolean barEnabled = plugin.getConfigManager().getConfig()
                        .getBoolean("progress-bar.enabled", true);
                progressBar = barEnabled ? buildProgressBarString(jd.getXp(), xpReq) : "";                level       = String.valueOf(jd.getLevel());
                xp          = XpUtil.formatXp(jd.getXp());
                xpRequired  = XpUtil.formatXp(xpReq);
            }
        }

        for (String line : loreTpl) {
            String processed = line
                    .replace("%level%",        level)
                    .replace("%xp%",           xp)
                    .replace("%xp_required%",  xpRequired)
                    .replace("%progress_bar%", progressBar);

            if (line.contains("%description%")) {
                for (String descLine : job.getDescription()) {
                    lore.add(descLine);
                }
            } else {
                lore.add(processed);
            }
        }

        builder.lore(lore, player);
        return builder.build();
    }


    public ItemStack buildJobIcon(Job job, Optional<PlayerData> data, boolean joined) {
        return buildJobIcon(job, data, joined, null);
    }


    public String buildProgressBarString(double current, double max) {
        FileConfiguration gui = plugin.getConfigManager().getGuiConfig();
        int length       = gui.getInt("progress-bar.length", 20);
        String filled    = gui.getString("progress-bar.filled-char", "\u2588");
        String empty     = gui.getString("progress-bar.empty-char", "\u2591");
        String filledCol = gui.getString("progress-bar.filled-color", "<green>");
        String emptyCol  = gui.getString("progress-bar.empty-color", "<dark_gray>");
        return ProgressBar.buildString(current, max, length, filled, empty, filledCol, emptyCol);
    }


    public ItemStack buildNavButton(String matName, String name) {
        Material mat = Material.matchMaterial(matName);
        if (mat == null) mat = Material.ARROW;
        return ItemBuilder.of(mat).name(name).hideAttributes().build();
    }

    public ItemStack buildCloseButton(String guiSection) {
        FileConfiguration gui = plugin.getConfigManager().getGuiConfig();
        String matName = gui.getString(guiSection + ".close-button.material", "BARRIER");
        String name    = gui.getString(guiSection + ".close-button.name", "<red>Close");
        List<String> lore = gui.getStringList(guiSection + ".close-button.lore");
        Material mat = Material.matchMaterial(matName);
        if (mat == null) mat = Material.BARRIER;
        return ItemBuilder.of(mat).name(name).lore(lore).hideAttributes().build();
    }


    public int getSlot(String guiSection, String key, int def) {
        return plugin.getConfigManager().getGuiConfig().getInt(guiSection + "." + key, def);
    }

    public List<Integer> getSlotList(String guiSection, String key) {
        return plugin.getConfigManager().getGuiConfig().getIntegerList(guiSection + "." + key);
    }
}