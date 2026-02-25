package fr.oreo.oJobs.models;

import java.util.UUID;


public class LeaderboardEntry {

    private final UUID uuid;
    private final String playerName;
    private final String jobId;
    private final int level;
    private final double xp;
    private final int prestige;
    private final double totalXpEarned;

    public LeaderboardEntry(UUID uuid, String playerName, String jobId,
                            int level, double xp, int prestige, double totalXpEarned) {
        this.uuid = uuid;
        this.playerName = playerName;
        this.jobId = jobId;
        this.level = level;
        this.xp = xp;
        this.prestige = prestige;
        this.totalXpEarned = totalXpEarned;
    }


    public UUID getUuid() { return uuid; }
    public String getPlayerName() { return playerName; }
    public String getJobId() { return jobId; }
    public int getLevel() { return level; }
    public double getXp() { return xp; }
    public int getPrestige() { return prestige; }
    public double getTotalXpEarned() { return totalXpEarned; }
}
