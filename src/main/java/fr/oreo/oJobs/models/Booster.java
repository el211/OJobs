package fr.oreo.oJobs.models;

import java.util.UUID;


public class Booster {

    public enum BoosterType {
        GLOBAL,
        JOB,
        PLAYER
    }

    private final String id;
    private final BoosterType type;
    private final double multiplier;
    private final long expiresAt;
    private final String jobId;
    private final UUID playerUuid;

    public Booster(String id, BoosterType type, double multiplier, long expiresAt,
                   String jobId, UUID playerUuid) {
        this.id = id;
        this.type = type;
        this.multiplier = multiplier;
        this.expiresAt = expiresAt;
        this.jobId = jobId;
        this.playerUuid = playerUuid;
    }

    public boolean isExpired() {
        return expiresAt != -1 && System.currentTimeMillis() >= expiresAt;
    }

    public long getRemainingMs() {
        if (expiresAt == -1) return Long.MAX_VALUE;
        return Math.max(0, expiresAt - System.currentTimeMillis());
    }

    public String getRemainingFormatted() {
        if (expiresAt == -1) return "∞";
        long s = getRemainingMs() / 1000;
        if (s >= 3600) return (s / 3600) + "h " + ((s % 3600) / 60) + "m";
        if (s >= 60)   return (s / 60) + "m " + (s % 60) + "s";
        return s + "s";
    }


    public String getId() { return id; }
    public BoosterType getType() { return type; }
    public double getMultiplier() { return multiplier; }
    public long getExpiresAt() { return expiresAt; }
    public String getJobId() { return jobId; }
    public UUID getPlayerUuid() { return playerUuid; }
}
