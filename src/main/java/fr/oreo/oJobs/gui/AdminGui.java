package fr.oreo.oJobs.gui;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import fr.oreo.oJobs.OJobs;
import fr.oreo.oJobs.utils.ItemBuilder;
import fr.oreo.oJobs.utils.MessageUtil;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

import static fr.oreo.oJobs.gui.GuiHelper.col;
import static fr.oreo.oJobs.gui.GuiHelper.row;


public class AdminGui implements InventoryProvider {

    private final OJobs plugin;
    private final GuiHelper helper;

    public AdminGui(OJobs plugin) {
        this.plugin = plugin;
        this.helper = plugin.getGuiManager().getHelper();
    }

    @Override
    public void init(Player player, InventoryContents contents) {
        FileConfiguration gui = plugin.getConfigManager().getGuiConfig();

        helper.fillEmpty(contents, "admin-menu");

        placeButton(contents, gui,
                "admin-menu.give-xp-slot", 10,
                gui.getString("admin-menu.give-xp-material", "EXPERIENCE_BOTTLE"),
                gui.getString("admin-menu.give-xp-name", "<yellow>Give XP"),
                "<gray>Give XP to a player in a job.",
                e -> {
                    player.closeInventory();
                    MessageUtil.sendRaw(player,
                            "<gray>Use: <white>/jobs giveexp <player> <job> <amount>", Map.of());
                });

        placeButton(contents, gui,
                "admin-menu.set-level-slot", 11,
                gui.getString("admin-menu.set-level-material", "GOLDEN_SWORD"),
                gui.getString("admin-menu.set-level-name", "<yellow>Set Level"),
                "<gray>Set a player's job level.",
                e -> {
                    player.closeInventory();
                    MessageUtil.sendRaw(player,
                            "<gray>Use: <white>/jobs setlevel <player> <job> <level>", Map.of());
                });

        placeButton(contents, gui,
                "admin-menu.reset-slot", 13,
                gui.getString("admin-menu.reset-material", "TNT"),
                gui.getString("admin-menu.reset-name", "<red>Reset Player"),
                "<gray>Reset all jobs for a player.",
                e -> {
                    player.closeInventory();
                    MessageUtil.sendRaw(player,
                            "<gray>Use: <white>/jobs reset <player> [job]", Map.of());
                });

        placeButton(contents, gui,
                "admin-menu.add-booster-slot", 15,
                gui.getString("admin-menu.add-booster-material", "BEACON"),
                gui.getString("admin-menu.add-booster-name", "<gold>Add Booster"),
                "<gray>Add a global XP booster.",
                e -> {
                    player.closeInventory();
                    MessageUtil.sendRaw(player,
                            "<gray>Use: <white>/jobs booster add global <multiplier> <duration_seconds>", Map.of());
                });

        placeButton(contents, gui,
                "admin-menu.reload-slot", 49,
                gui.getString("admin-menu.reload-material", "REDSTONE"),
                gui.getString("admin-menu.reload-name", "<yellow>Reload Plugin"),
                "<gray>Reload all configs and jobs.",
                e -> {
                    player.closeInventory();
                    plugin.reload();
                    MessageUtil.send(player, "reload-success");
                });

        {
            int slot = 51;
            ItemStack item = ItemBuilder.of(Material.CLOCK)
                    .name("<aqua>Refresh Leaderboard")
                    .loreLine("<gray>Force a leaderboard refresh.")
                    .hideAttributes()
                    .build();
            contents.set(row(slot), col(slot),
                    ClickableItem.from(item, e -> {
                        plugin.getLeaderboardManager().forceRefresh();
                        MessageUtil.send(player, "leaderboard-refreshed");
                    }));
        }

        int backSlot = gui.getInt("admin-menu.back-slot", 45);
        contents.set(row(backSlot), col(backSlot),
                ClickableItem.from(
                        ItemBuilder.of(Material.ARROW).name("<yellow>\u25C0 Back").hideAttributes().build(),
                        e -> plugin.getGuiManager().openMainMenu(player)));
    }

    @Override
    public void update(Player player, InventoryContents contents) {
    }


    private void placeButton(InventoryContents contents, FileConfiguration gui,
                             String slotKey, int defaultSlot,
                             String matName, String name, String lore,
                             java.util.function.Consumer<fr.minuskube.inv.ItemClickData> onClick) {
        int slot = slotKey != null ? gui.getInt(slotKey, defaultSlot) : defaultSlot;
        Material mat = Material.matchMaterial(matName);
        if (mat == null) mat = Material.STONE;

        ItemStack item = ItemBuilder.of(mat)
                .name(name)
                .loreLine(lore)
                .hideAttributes()
                .build();
        contents.set(row(slot), col(slot), ClickableItem.from(item, onClick));
    }
}