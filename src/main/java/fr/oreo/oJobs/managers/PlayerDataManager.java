package fr.oreo.oJobs.managers;

import fr.oreo.oJobs.OJobs;
import fr.oreo.oJobs.models.Job;
import fr.oreo.oJobs.models.LevelReward;
import fr.oreo.oJobs.models.PlayerData;
import fr.oreo.oJobs.models.PlayerJobData;
import fr.oreo.oJobs.utils.MessageUtil;
import fr.oreo.oJobs.utils.XpUtil;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager {

    private final OJobs plugin;

    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();
    private final Map<UUID, Long> notificationCooldowns = new ConcurrentHashMap<>();

    private BukkitTask autoSaveTask;

    public PlayerDataManager(OJobs plugin) {
        this.plugin = plugin;
    }

    public PlayerData loadPlayer(UUID uuid) {
        if (cache.containsKey(uuid)) return cache.get(uuid);

        PlayerData data = plugin.getStorageManager().getStorage().loadPlayer(uuid);
        if (data == null) {
            String name = Optional.ofNullable(Bukkit.getPlayer(uuid))
                    .map(Player::getName)
                    .orElse("Unknown");
            data = new PlayerData(uuid, name);
        }

        cache.put(uuid, data);
        return data;
    }

    public void savePlayer(UUID uuid, boolean sync) {
        PlayerData data = cache.get(uuid);
        if (data == null) return;
        if (!data.isDirty()) return;

        data.setDirty(false);

        if (sync) {
            plugin.getStorageManager().getStorage().savePlayer(data);
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin,
                    () -> plugin.getStorageManager().getStorage().savePlayer(data));
        }
    }

    public void unloadPlayer(UUID uuid) {
        savePlayer(uuid, false);
        cache.remove(uuid);
        notificationCooldowns.remove(uuid);
    }

    public void startAutoSave() {
        if (autoSaveTask != null) autoSaveTask.cancel();

        long intervalSeconds = plugin.getConfigManager().getStorageConfig()
                .getLong("auto-save-interval", 300L);

        autoSaveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
                plugin,
                () -> cache.values().stream()
                        .filter(PlayerData::isDirty)
                        .forEach(pd -> {
                            pd.setDirty(false);
                            plugin.getStorageManager().getStorage().savePlayer(pd);
                        }),
                intervalSeconds * 20L,
                intervalSeconds * 20L
        );
    }

    public Optional<PlayerData> getPlayerData(UUID uuid) {
        return Optional.ofNullable(cache.get(uuid));
    }

    public Collection<PlayerData> getAllPlayerData() {
        return Collections.unmodifiableCollection(cache.values());
    }

    public boolean joinJob(Player player, Job job) {
        PlayerData data = cache.get(player.getUniqueId());
        if (data == null) return false;

        if (data.hasJob(job.getId())) {
            MessageUtil.send(player, "already-in-job", Map.of("job", job.getDisplayName()));
            return false;
        }

        int maxJobs = plugin.getConfigManager().getConfig().getInt("settings.max-jobs", 3);
        if (!player.hasPermission("ojobs.bypass.maxjobs") && data.getJobCount() >= maxJobs) {
            MessageUtil.send(player, "max-jobs-reached", Map.of("max", String.valueOf(maxJobs)));
            return false;
        }

        if (job.getMaxPlayers() > 0 && plugin.getJobManager().countPlayers(job.getId()) >= job.getMaxPlayers()) {
            MessageUtil.send(player, "job-full", Map.of("job", job.getDisplayName()));
            return false;
        }

        if (!job.getRequiredPermission().isBlank() && !player.hasPermission(job.getRequiredPermission())) {
            MessageUtil.send(player, "no-permission-job", Map.of("job", job.getDisplayName()));
            return false;
        }

        data.joinJob(job.getId());

        MessageUtil.send(player, "job-join", Map.of("job", job.getDisplayName()));
        MessageUtil.playSound(
                player,
                plugin.getConfigManager().getConfig().getString("settings.sounds.job-join", "ENTITY_PLAYER_LEVELUP")
        );
        return true;
    }

    public boolean leaveJob(Player player, Job job) {
        PlayerData data = cache.get(player.getUniqueId());
        if (data == null || !data.hasJob(job.getId())) {
            MessageUtil.send(player, "not-in-job", Map.of("job", job.getDisplayName()));
            return false;
        }

        data.leaveJob(job.getId());

        MessageUtil.send(player, "job-leave", Map.of("job", job.getDisplayName()));
        MessageUtil.playSound(
                player,
                plugin.getConfigManager().getConfig().getString("settings.sounds.job-leave", "BLOCK_NOTE_BLOCK_BASS")
        );
        return true;
    }

    public void giveXp(Player player, Job job, double baseXp) {
        PlayerData data = cache.get(player.getUniqueId());
        if (data == null || !data.hasJob(job.getId())) return;

        PlayerJobData jd = data.getJobData(job.getId()).orElse(null);
        if (jd == null) return;

        if (job.getMaxLevel() > 0 && jd.getLevel() >= job.getMaxLevel()) return;

        double multiplier = plugin.getBoosterManager().getMultiplierFor(player, job);
        double finalXp = baseXp * multiplier;

        jd.addXp(finalXp);
        data.setDirty(true);

        sendXpActionBar(player, job, jd, finalXp);
        checkLevelUp(player, job, data, jd);
    }


    private void sendXpActionBar(Player player, Job job, PlayerJobData jd, double gainedXp) {
        if (!plugin.getConfigManager().getConfig().getBoolean("settings.actionbar-enabled", true)) return;

        double xpRequired = XpUtil.getXpRequired(job, jd.getLevel());
        double fraction = XpUtil.getProgressFraction(jd.getXp(), xpRequired);
        String progressPct = String.format(Locale.US, "%.1f", fraction * 100);

        var gui = plugin.getConfigManager().getGuiConfig();
        String progressBar = fr.oreo.oJobs.utils.ProgressBar.buildString(
                jd.getXp(), xpRequired,
                gui.getInt("progress-bar.length", 20),
                gui.getString("progress-bar.filled-char", "█"),
                gui.getString("progress-bar.empty-char", "░"),
                gui.getString("progress-bar.filled-color", "<green>"),
                gui.getString("progress-bar.empty-color", "<dark_gray>")
        );

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("xp", XpUtil.formatXp(gainedXp));
        placeholders.put("job", job.getDisplayName());
        placeholders.put("level", String.valueOf(jd.getLevel()));
        placeholders.put("xp_required", XpUtil.formatXp(xpRequired));
        placeholders.put("current_xp", XpUtil.formatXp(jd.getXp()));
        placeholders.put("progress", progressPct);
        placeholders.put("progress_bar", progressBar);
        placeholders.put("prestige", String.valueOf(jd.getPrestige()));
        placeholders.put("max_level", String.valueOf(job.getMaxLevel()));
        placeholders.put("total_xp", XpUtil.formatXp(jd.getTotalXpEarned()));

        long now = System.currentTimeMillis();
        long flashWindowMs = plugin.getConfigManager().getConfig()
                .getLong("settings.notification-cooldown", 2000L);
        long lastFlash = notificationCooldowns.getOrDefault(player.getUniqueId(), 0L);

        String template;
        if (now - lastFlash >= flashWindowMs) {
            notificationCooldowns.put(player.getUniqueId(), now);

            template = plugin.getConfigManager().getLangConfig().getString(
                    "messages.xp-gain-actionbar",
                    "<gray>+<green><xp> XP</green> " +
                            "<dark_gray>(<yellow><job></yellow>)</dark_gray> " +
                            "Lv.<white><level></white> " +
                            "[<green><current_xp></green><dark_gray>/<gray><xp_required></gray>] " +
                            "<progress_bar>"
            );
        } else {
            template = plugin.getConfigManager().getLangConfig().getString(
                    "messages.xp-progress-actionbar",
                    "<dark_gray>(<yellow><job></yellow>)</dark_gray> " +
                            "Lv.<white><level></white> " +
                            "[<green><current_xp></green><dark_gray>/<gray><xp_required></gray>] " +
                            "<progress_bar>"
            );
        }

        for (Map.Entry<String, String> e : placeholders.entrySet()) {
            template = template.replace("%" + e.getKey() + "%", e.getValue());
        }

        MessageUtil.sendActionBar(player, template, placeholders);
    }

    private void checkLevelUp(Player player, Job job, PlayerData data, PlayerJobData jd) {
        boolean leveledUp = false;

        while (true) {
            int currentLevel = jd.getLevel();
            if (job.getMaxLevel() > 0 && currentLevel >= job.getMaxLevel()) break;

            double xpRequired = XpUtil.getXpRequired(job, currentLevel);
            if (jd.getXp() < xpRequired) break;

            jd.setXp(jd.getXp() - xpRequired);
            jd.setLevel(currentLevel + 1);
            leveledUp = true;

            int newLevel = jd.getLevel();

            Bukkit.getScheduler().runTask(plugin, () -> {
                String titleRaw = plugin.getConfigManager().getLangConfig()
                        .getString("messages.level-up-title", "<gold>⬆ Level Up!");
                String subRaw = plugin.getConfigManager().getLangConfig()
                        .getString("messages.level-up-subtitle", "<yellow>%job% <gray>Level <gold>%level%");

                MessageUtil.sendTitle(player, titleRaw, subRaw, Map.of(
                        "job", job.getDisplayName(),
                        "level", String.valueOf(newLevel)
                ));

                MessageUtil.playSound(
                        player,
                        plugin.getConfigManager().getConfig().getString("settings.sounds.level-up", "UI_TOAST_CHALLENGE_COMPLETE"),
                        1.0f, 1.0f
                );

                if (plugin.getConfigManager().getConfig().getBoolean("settings.broadcast-levelup", false)) {
                    int interval = plugin.getConfigManager().getConfig().getInt("settings.broadcast-levelup-interval", 5);
                    if (newLevel % interval == 0) {
                        String broadcastRaw = plugin.getConfigManager().getLangConfig()
                                .getString("messages.level-up-broadcast", "");
                        if (!broadcastRaw.isBlank()) {
                            MessageUtil.broadcast(broadcastRaw, Map.of(
                                    "player", player.getName(),
                                    "job", job.getDisplayName(),
                                    "level", String.valueOf(newLevel)
                            ));
                        }
                    }
                }
            });

            final int rewardLevel = newLevel;
            job.getRewardForLevel(rewardLevel).ifPresent(reward ->
                    Bukkit.getScheduler().runTask(plugin, () -> grantReward(player, reward)));
        }

        if (leveledUp) data.setDirty(true);
    }

    private void grantReward(Player player, LevelReward reward) {
        if (reward.getMoney() > 0 && plugin.isVaultEnabled()) {
            plugin.getEconomy().depositPlayer(player, reward.getMoney());
        }

        for (String cmd : reward.getCommands()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", player.getName()));
        }

        for (String itemStr : reward.getItems()) {
            grantItem(player, itemStr);
        }

        for (String perm : reward.getPermissions()) {
            if (!player.hasPermission(perm)) {
                Bukkit.dispatchCommand(
                        Bukkit.getConsoleSender(),
                        "lp user " + player.getName() + " permission set " + perm + " true"
                );
            }
        }

        if (reward.getSound() != null) {
            MessageUtil.playSound(player, reward.getSound(), reward.getSoundVolume(), reward.getSoundPitch());
        }

        if (reward.isFireFirework()) {
            launchFirework(player);
        }
    }

    private void grantItem(Player player, String itemStr) {
        String[] parts = itemStr.split(" ");
        if (parts.length < 1) return;

        Material mat = Material.matchMaterial(parts[0]);
        if (mat == null) return;

        int amount = parts.length >= 2 ? parseIntSafe(parts[1], 1) : 1;
        player.getInventory().addItem(new ItemStack(mat, amount));
    }

    private void launchFirework(Player player) {
        Firework fw = player.getWorld().spawn(player.getLocation(), Firework.class);
        FireworkMeta meta = fw.getFireworkMeta();

        meta.addEffect(FireworkEffect.builder()
                .with(FireworkEffect.Type.BALL_LARGE)
                .withColor(Color.YELLOW, Color.fromRGB(255, 215, 0), Color.ORANGE)
                .withFade(Color.WHITE)
                .withTrail()
                .withFlicker()
                .build());

        meta.setPower(1);
        fw.setFireworkMeta(meta);
    }

    private int parseIntSafe(String s, int def) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public void setLevel(UUID uuid, String jobId, int level) {
        getPlayerData(uuid).ifPresent(data ->
                data.getJobData(jobId).ifPresent(jd -> {
                    jd.setLevel(level);
                    jd.setXp(0);
                    data.setDirty(true);
                })
        );
    }

    public void giveXpDirect(UUID uuid, String jobId, double xp) {
        PlayerData data = cache.get(uuid);
        if (data == null || !data.hasJob(jobId)) return;

        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return;

        plugin.getJobManager().getJob(jobId).ifPresent(job -> giveXp(player, job, xp));
    }

    public void resetJob(UUID uuid, String jobId) {
        getPlayerData(uuid).ifPresent(data -> {
            data.leaveJob(jobId);
            data.setDirty(true);
        });
    }

    public void resetAllJobs(UUID uuid) {
        getPlayerData(uuid).ifPresent(data -> {
            new HashSet<>(data.getJobs().keySet()).forEach(data::leaveJob);
            data.setDirty(true);
        });
    }
}