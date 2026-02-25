package fr.oreo.oJobs.managers;

import fr.oreo.oJobs.OJobs;
import fr.oreo.oJobs.models.LeaderboardEntry;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class LeaderboardManager {

    private static final int DEFAULT_SIZE = 10;

    private final OJobs plugin;
    private final Map<String, List<LeaderboardEntry>> jobLeaderboards  = new ConcurrentHashMap<>();
    private volatile List<LeaderboardEntry> globalLeaderboard = new ArrayList<>();
    private BukkitTask updateTask;

    public LeaderboardManager(OJobs plugin) {
        this.plugin = plugin;
    }


    public void startUpdateTask() {
        if (updateTask != null) updateTask.cancel();

        long intervalSeconds = plugin.getConfigManager().getConfig()
                .getLong("leaderboard.refresh-interval", 300L);
        long intervalTicks = intervalSeconds * 20L;

        updateTask = plugin.getServer().getScheduler()
                .runTaskTimerAsynchronously(plugin, this::refresh, 40L, intervalTicks);
    }

    public List<LeaderboardEntry> getLeaderboard(String jobId) {
        return jobLeaderboards.getOrDefault(jobId.toLowerCase(), List.of());
    }

    public List<LeaderboardEntry> getGlobalLeaderboard() {
        return Collections.unmodifiableList(globalLeaderboard);
    }

    public void refresh() {
        var storage = plugin.getStorageManager().getStorage();
        int size = plugin.getConfigManager().getConfig()
                .getInt("leaderboard.size", DEFAULT_SIZE);

        for (var job : plugin.getJobManager().getJobs()) {
            List<LeaderboardEntry> entries = storage.getLeaderboard(job.getId(), size);
            jobLeaderboards.put(job.getId(), entries);
        }

        globalLeaderboard = storage.getGlobalLeaderboard(size);
    }

    public void forceRefresh() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this::refresh);
    }
}
