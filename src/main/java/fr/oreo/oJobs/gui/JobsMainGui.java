package fr.oreo.oJobs.gui;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import fr.oreo.oJobs.OJobs;
import fr.oreo.oJobs.models.Job;
import fr.oreo.oJobs.models.PlayerData;
import fr.oreo.oJobs.utils.MessageUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static fr.oreo.oJobs.gui.GuiHelper.col;
import static fr.oreo.oJobs.gui.GuiHelper.row;


public class JobsMainGui implements InventoryProvider {

    private final OJobs plugin;
    private final GuiHelper helper;

    public JobsMainGui(OJobs plugin) {
        this.plugin = plugin;
        this.helper = plugin.getGuiManager().getHelper();
    }

    @Override
    public void init(Player player, InventoryContents contents) {
        FileConfiguration gui = plugin.getConfigManager().getGuiConfig();

        helper.fillSlots(contents, "main-menu");

        int page = contents.pagination().getPage();

        int prevSlot = gui.getInt("main-menu.prev-page-slot", 45);
        int nextSlot = gui.getInt("main-menu.next-page-slot", 53);
        List<Integer> jobSlots = gui.getIntegerList("main-menu.job-slots");
        if (jobSlots.isEmpty()) {
            for (int r = 1; r <= 4; r++)
                for (int c = 1; c <= 7; c++)
                    jobSlots.add(r * 9 + c);
        }

        List<Job> jobs = new ArrayList<>(plugin.getJobManager().getJobs());
        int pageSize   = jobSlots.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) jobs.size() / pageSize));

        Optional<PlayerData> data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        int start = page * pageSize;

        for (int i = 0; i < pageSize; i++) {
            int jobIndex = start + i;
            if (i >= jobSlots.size()) break;
            int slot = jobSlots.get(i);

            if (jobIndex < jobs.size()) {
                Job job = jobs.get(jobIndex);
                boolean joined = data.map(d -> d.hasJob(job.getId())).orElse(false);

                ItemStack icon = helper.buildJobIcon(job, data, joined, player);

                contents.set(row(slot), col(slot), ClickableItem.from(icon, e -> {
                    if (e.getEvent() instanceof org.bukkit.event.inventory.InventoryClickEvent click) {
                        if (click.isRightClick() && joined) {
                            plugin.getPlayerDataManager().leaveJob(player, job);
                            plugin.getGuiManager().openMainMenu(player);
                        } else {
                            plugin.getGuiManager().openJobMenu(player, job);
                        }
                        MessageUtil.playSound(player,
                                gui.getString("main-menu.click-sound", "UI_BUTTON_CLICK"));
                    }
                }));
            }
        }

        if (page > 0) {
            String matName = gui.getString("main-menu.prev-page-material", "ARROW");
            String name    = gui.getString("main-menu.prev-page-name", "<yellow>\u25C0 Previous");
            contents.set(row(prevSlot), col(prevSlot),
                    ClickableItem.from(helper.buildNavButton(matName, name), e ->
                            plugin.getGuiManager().openMainMenu(player, page - 1)));
        }

        if (page < totalPages - 1) {
            String matName = gui.getString("main-menu.next-page-material", "ARROW");
            String name    = gui.getString("main-menu.next-page-name", "<yellow>Next \u25B6");
            contents.set(row(nextSlot), col(nextSlot),
                    ClickableItem.from(helper.buildNavButton(matName, name), e ->
                            plugin.getGuiManager().openMainMenu(player, page + 1)));
        }

        int closeSlot = gui.getInt("main-menu.close-button.slot", 49);
        contents.set(row(closeSlot), col(closeSlot),
                ClickableItem.from(helper.buildCloseButton("main-menu"), e ->
                        player.closeInventory()));
    }

    @Override
    public void update(Player player, InventoryContents contents) {
    }
}