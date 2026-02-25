package fr.oreo.oJobs.storage;

import fr.oreo.oJobs.OJobs;
import fr.oreo.oJobs.models.LeaderboardEntry;
import fr.oreo.oJobs.models.PlayerData;
import fr.oreo.oJobs.models.PlayerJobData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


public class YamlStorage implements Storage {

    private final OJobs plugin;
    private File dataFolder;

    public YamlStorage(OJobs plugin) {
        this.plugin = plugin;
    }


    @Override
    public boolean initialize() {
        String folder = plugin.getConfigManager().getStorageConfig()
                .getString("yaml.folder", "data");
        dataFolder = new File(plugin.getDataFolder(), folder);
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            plugin.getLogger().severe("[YamlStorage] Could not create data folder: " + dataFolder.getAbsolutePath());
            return false;
        }
        return true;
    }

    @Override
    public void shutdown() {
    }


    @Override
    public PlayerData loadPlayer(UUID uuid) {
        File file = new File(dataFolder, uuid + ".yml");
        if (!file.exists()) return null;

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        String name = cfg.getString("name", "Unknown");
        PlayerData data = new PlayerData(uuid, name);

        if (cfg.isConfigurationSection("jobs")) {
            for (String jobId : cfg.getConfigurationSection("jobs").getKeys(false)) {
                String base    = "jobs." + jobId + ".";
                int level      = cfg.getInt(base + "level", 1);
                double xp      = cfg.getDouble(base + "xp", 0);
                int prestige   = cfg.getInt(base + "prestige", 0);
                double totalXp = cfg.getDouble(base + "total-xp", 0);
                long joinedAt  = cfg.getLong(base + "joined-at", System.currentTimeMillis());
                data.putJobData(jobId, new PlayerJobData(jobId, level, xp, prestige, totalXp, joinedAt));
            }
        }
        return data;
    }

    @Override
    public void savePlayer(PlayerData data) {
        File file = new File(dataFolder, data.getUuid() + ".yml");
        YamlConfiguration cfg = new YamlConfiguration();

        cfg.set("uuid", data.getUuid().toString());
        cfg.set("name", data.getPlayerName());

        for (Map.Entry<String, PlayerJobData> entry : data.getJobs().entrySet()) {
            String base = "jobs." + entry.getKey() + ".";
            PlayerJobData jd = entry.getValue();
            cfg.set(base + "level",     jd.getLevel());
            cfg.set(base + "xp",        jd.getXp());
            cfg.set(base + "prestige",  jd.getPrestige());
            cfg.set(base + "total-xp",  jd.getTotalXpEarned());
            cfg.set(base + "joined-at", jd.getJoinedAt());
        }

        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("[YamlStorage] Failed to save player " + data.getUuid());
            e.printStackTrace();
        }
    }


    @Override
    public List<LeaderboardEntry> getLeaderboard(String jobId, int limit) {
        List<LeaderboardEntry> entries = new ArrayList<>();
        File[] files = dataFolder.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return entries;

        for (File file : files) {
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            String jobPath = "jobs." + jobId.toLowerCase();
            if (!cfg.contains(jobPath)) continue;

            try {
                UUID uuid      = UUID.fromString(Objects.requireNonNull(cfg.getString("uuid")));
                String name    = cfg.getString("name", "Unknown");
                int level      = cfg.getInt(jobPath + ".level", 1);
                double xp      = cfg.getDouble(jobPath + ".xp", 0);
                int prestige   = cfg.getInt(jobPath + ".prestige", 0);
                double totalXp = cfg.getDouble(jobPath + ".total-xp", 0);
                entries.add(new LeaderboardEntry(uuid, name, jobId, level, xp, prestige, totalXp));
            } catch (Exception ignored) {}
        }

        return entries.stream()
                .sorted(Comparator.comparingInt(LeaderboardEntry::getPrestige).reversed()
                        .thenComparingInt(LeaderboardEntry::getLevel).reversed()
                        .thenComparingDouble(LeaderboardEntry::getTotalXpEarned).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public List<LeaderboardEntry> getGlobalLeaderboard(int limit) {
        List<LeaderboardEntry> entries = new ArrayList<>();
        File[] files = dataFolder.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return entries;

        for (File file : files) {
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            String uuidStr = cfg.getString("uuid", "");
            String name    = cfg.getString("name", "Unknown");

            UUID uuid;
            try { uuid = UUID.fromString(uuidStr); } catch (Exception e) { continue; }

            int totalLevel = 0;
            double totalXp = 0;
            if (cfg.isConfigurationSection("jobs")) {
                for (String jId : cfg.getConfigurationSection("jobs").getKeys(false)) {
                    totalLevel += cfg.getInt("jobs." + jId + ".level", 1);
                    totalXp    += cfg.getDouble("jobs." + jId + ".total-xp", 0);
                }
            }
            entries.add(new LeaderboardEntry(uuid, name, "global", totalLevel, 0, 0, totalXp));
        }

        return entries.stream()
                .sorted(Comparator.comparingInt(LeaderboardEntry::getLevel).reversed()
                        .thenComparingDouble(LeaderboardEntry::getTotalXpEarned).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }
}
