package fr.oreo.oJobs.managers;

import fr.oreo.oJobs.OJobs;
import fr.oreo.oJobs.models.Booster;
import fr.oreo.oJobs.models.Job;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class BoosterManager {

    private final OJobs plugin;
    private final Map<String, Booster> boosters = new ConcurrentHashMap<>();
    private long idCounter = 0;

    public BoosterManager(OJobs plugin) {
        this.plugin = plugin;
    }


    public Booster addGlobalBooster(double multiplier, long durationMs) {
        return register(new Booster(nextId(), Booster.BoosterType.GLOBAL, multiplier,
                durationMs == -1 ? -1 : System.currentTimeMillis() + durationMs, null, null));
    }

    public Booster addJobBooster(String jobId, double multiplier, long durationMs) {
        return register(new Booster(nextId(), Booster.BoosterType.JOB, multiplier,
                durationMs == -1 ? -1 : System.currentTimeMillis() + durationMs, jobId, null));
    }

    public Booster addPlayerBooster(UUID playerUuid, double multiplier, long durationMs) {
        return register(new Booster(nextId(), Booster.BoosterType.PLAYER, multiplier,
                durationMs == -1 ? -1 : System.currentTimeMillis() + durationMs, null, playerUuid));
    }

    private Booster register(Booster booster) {
        int maxGlobal = plugin.getConfigManager().getConfig().getInt("xp.boosters.max-global", 3);
        int maxJob    = plugin.getConfigManager().getConfig().getInt("xp.boosters.max-per-job", 5);
        int maxPlayer = plugin.getConfigManager().getConfig().getInt("xp.boosters.max-per-player", 3);

        long currentCount = switch (booster.getType()) {
            case GLOBAL -> countActive(Booster.BoosterType.GLOBAL, null, null);
            case JOB    -> countActive(Booster.BoosterType.JOB, booster.getJobId(), null);
            case PLAYER -> countActive(Booster.BoosterType.PLAYER, null, booster.getPlayerUuid());
        };
        int limit = switch (booster.getType()) {
            case GLOBAL -> maxGlobal;
            case JOB    -> maxJob;
            case PLAYER -> maxPlayer;
        };

        if (currentCount >= limit) return null;
        boosters.put(booster.getId(), booster);
        return booster;
    }

    private long countActive(Booster.BoosterType type, String jobId, UUID playerUuid) {
        return boosters.values().stream()
                .filter(b -> !b.isExpired())
                .filter(b -> b.getType() == type)
                .filter(b -> jobId == null || jobId.equalsIgnoreCase(b.getJobId()))
                .filter(b -> playerUuid == null || playerUuid.equals(b.getPlayerUuid()))
                .count();
    }


    public double getMultiplierFor(Player player, Job job) {
        removeExpired();

        double total = plugin.getConfigManager().getConfig()
                .getDouble("xp.global-multiplier", 1.0);

        if (plugin.getConfigManager().getConfig().isConfigurationSection("xp.permission-multipliers")) {
            for (String perm : plugin.getConfigManager().getConfig()
                    .getConfigurationSection("xp.permission-multipliers").getKeys(false)) {
                if (player.hasPermission(perm)) {
                    double val = plugin.getConfigManager().getConfig()
                            .getDouble("xp.permission-multipliers." + perm, 1.0);
                    if (val > total) total = val;
                }
            }
        }

        for (Booster b : boosters.values()) {
            if (b.isExpired()) continue;
            boolean applies = switch (b.getType()) {
                case GLOBAL -> true;
                case JOB    -> job.getId().equalsIgnoreCase(b.getJobId());
                case PLAYER -> player.getUniqueId().equals(b.getPlayerUuid())
                        && (b.getJobId() == null || job.getId().equalsIgnoreCase(b.getJobId()));
            };
            if (applies) total *= b.getMultiplier();
        }

        if (plugin.getConfigManager().getConfig().getBoolean("prestige.enabled", true)) {
            double bonusPerPrestige = plugin.getConfigManager().getConfig()
                    .getDouble("prestige.xp-bonus-per-prestige", 0.1);
            var pdOpt = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
            if (pdOpt.isPresent()) {
                int prestige = pdOpt.get().getJobData(job.getId())
                        .map(jd -> jd.getPrestige())
                        .orElse(0);
                if (prestige > 0) {
                    total += total * (prestige * bonusPerPrestige);
                }
            }
        }

        return total;
    }

    public void removeExpired() {
        boosters.entrySet().removeIf(e -> e.getValue().isExpired());
    }

    public boolean removeBooster(String id) {
        return boosters.remove(id) != null;
    }

    public List<Booster> getActiveBoosters() {
        removeExpired();
        return new ArrayList<>(boosters.values());
    }

    public Map<String, Booster> getBoosterMap() {
        removeExpired();
        return Collections.unmodifiableMap(boosters);
    }

    private String nextId() {
        return "booster-" + (++idCounter);
    }
}