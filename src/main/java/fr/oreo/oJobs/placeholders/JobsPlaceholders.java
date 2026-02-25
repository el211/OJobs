package fr.oreo.oJobs.placeholders;

import fr.oreo.oJobs.OJobs;
import fr.oreo.oJobs.models.Job;
import fr.oreo.oJobs.models.PlayerData;
import fr.oreo.oJobs.models.PlayerJobData;
import fr.oreo.oJobs.utils.ProgressBar;
import fr.oreo.oJobs.utils.XpUtil;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;


public class JobsPlaceholders extends PlaceholderExpansion {

    private final OJobs plugin;

    public JobsPlaceholders(OJobs plugin) {
        this.plugin = plugin;
    }

    @Override public @NotNull String getIdentifier() { return "jobs"; }
    @Override public @NotNull String getAuthor()     { return "Oreo"; }
    @Override public @NotNull String getVersion()    { return plugin.getDescription().getVersion(); }
    @Override public boolean persist()               { return true; }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";

        Optional<PlayerData> dataOpt = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (dataOpt.isEmpty()) return "";
        PlayerData data = dataOpt.get();

        return switch (params.toLowerCase()) {
            case "count"       -> String.valueOf(data.getJobCount());
            case "total_level" -> String.valueOf(data.getTotalLevel());
            case "job"         -> getFirstJob(data).map(Job::getDisplayName).orElse("None");
            case "level"       -> getFirstJobData(data).map(jd -> String.valueOf(jd.getLevel())).orElse("0");
            case "xp"          -> getFirstJobData(data).map(jd -> XpUtil.formatXp(jd.getXp())).orElse("0");
            case "xp_needed"   -> getFirstJobXpNeeded(data);
            case "progress"    -> getFirstJobProgress(data);
            case "progress_bar"-> getFirstJobBar(data);
            case "max_level"   -> getFirstJob(data).map(j -> String.valueOf(j.getMaxLevel())).orElse("0");
            case "prestige"    -> getFirstJobData(data).map(jd -> String.valueOf(jd.getPrestige())).orElse("0");
            default            -> handlePrefixed(data, params);
        };
    }

    private @Nullable String handlePrefixed(PlayerData data, String params) {
        if (params.startsWith("level_"))         return getLevelFor(data, params.substring(6));
        if (params.startsWith("xp_needed_"))     return getXpNeededFor(data, params.substring(10));
        if (params.startsWith("xp_"))            return getXpFor(data, params.substring(3));
        if (params.startsWith("progress_bar_"))  return getBarFor(data, params.substring(13));
        if (params.startsWith("progress_"))      return getProgressFor(data, params.substring(9));
        if (params.startsWith("max_level_"))     return getMaxLevelFor(params.substring(10));
        if (params.startsWith("prestige_"))      return getPrestigeFor(data, params.substring(9));
        if (params.startsWith("job_"))           return getJobNameFor(params.substring(4));
        return null;
    }


    private Optional<Job> getFirstJob(PlayerData data) {
        return data.getJobs().keySet().stream().findFirst()
                .flatMap(id -> plugin.getJobManager().getJob(id));
    }

    private Optional<PlayerJobData> getFirstJobData(PlayerData data) {
        return data.getJobs().values().stream().findFirst();
    }

    private String getFirstJobXpNeeded(PlayerData data) {
        return getFirstJob(data).flatMap(j -> data.getJobData(j.getId())
                .map(jd -> XpUtil.formatXp(XpUtil.getXpRequired(j, jd.getLevel())))).orElse("0");
    }

    private String getFirstJobProgress(PlayerData data) {
        return getFirstJob(data).flatMap(j -> data.getJobData(j.getId()).map(jd -> {
            double req = XpUtil.getXpRequired(j, jd.getLevel());
            return String.format("%.1f", XpUtil.getProgressFraction(jd.getXp(), req) * 100) + "%";
        })).orElse("0%");
    }

    private String getFirstJobBar(PlayerData data) {
        FileConfiguration gui = plugin.getConfigManager().getGuiConfig();
        return getFirstJob(data).flatMap(j -> data.getJobData(j.getId()).map(jd -> {
            double req = XpUtil.getXpRequired(j, jd.getLevel());
            return ProgressBar.buildString(jd.getXp(), req,
                    gui.getInt("progress-bar.length", 20),
                    gui.getString("progress-bar.filled-char", "█"),
                    gui.getString("progress-bar.empty-char", "░"),
                    gui.getString("progress-bar.filled-color", "<green>"),
                    gui.getString("progress-bar.empty-color", "<dark_gray>"));
        })).orElse("");
    }

    private String getLevelFor(PlayerData data, String jobId) {
        return data.getJobData(jobId).map(jd -> String.valueOf(jd.getLevel())).orElse("0");
    }
    private String getXpFor(PlayerData data, String jobId) {
        return data.getJobData(jobId).map(jd -> XpUtil.formatXp(jd.getXp())).orElse("0");
    }
    private String getXpNeededFor(PlayerData data, String jobId) {
        return plugin.getJobManager().getJob(jobId).flatMap(j ->
                data.getJobData(jobId).map(jd ->
                        XpUtil.formatXp(XpUtil.getXpRequired(j, jd.getLevel())))).orElse("0");
    }
    private String getProgressFor(PlayerData data, String jobId) {
        return plugin.getJobManager().getJob(jobId).flatMap(j ->
                data.getJobData(jobId).map(jd -> {
                    double req = XpUtil.getXpRequired(j, jd.getLevel());
                    return String.format("%.1f", XpUtil.getProgressFraction(jd.getXp(), req) * 100) + "%";
                })).orElse("0%");
    }
    private String getBarFor(PlayerData data, String jobId) {
        FileConfiguration gui = plugin.getConfigManager().getGuiConfig();
        return plugin.getJobManager().getJob(jobId).flatMap(j ->
                data.getJobData(jobId).map(jd -> {
                    double req = XpUtil.getXpRequired(j, jd.getLevel());
                    return ProgressBar.buildString(jd.getXp(), req,
                            gui.getInt("progress-bar.length", 20),
                            gui.getString("progress-bar.filled-char", "█"),
                            gui.getString("progress-bar.empty-char", "░"),
                            gui.getString("progress-bar.filled-color", "<green>"),
                            gui.getString("progress-bar.empty-color", "<dark_gray>"));
                })).orElse("");
    }
    private String getMaxLevelFor(String jobId) {
        return plugin.getJobManager().getJob(jobId)
                .map(j -> String.valueOf(j.getMaxLevel())).orElse("0");
    }
    private String getPrestigeFor(PlayerData data, String jobId) {
        return data.getJobData(jobId).map(jd -> String.valueOf(jd.getPrestige())).orElse("0");
    }
    private String getJobNameFor(String jobId) {
        return plugin.getJobManager().getJob(jobId).map(Job::getDisplayName).orElse("Unknown");
    }
}
