package fr.oreo.oJobs.models;

import java.util.*;


public class PlayerData {

    private final UUID uuid;
    private String playerName;

    private final Map<String, PlayerJobData> jobs = new HashMap<>();

    private final Map<String, Long> cooldowns = new HashMap<>();
    private volatile boolean dirty = false;

    public PlayerData(UUID uuid, String playerName) {
        this.uuid = uuid;
        this.playerName = playerName;
    }


    public boolean hasJob(String jobId) {
        return jobs.containsKey(jobId.toLowerCase());
    }

    public Optional<PlayerJobData> getJobData(String jobId) {
        return Optional.ofNullable(jobs.get(jobId.toLowerCase()));
    }

    public Map<String, PlayerJobData> getJobs() {
        return Collections.unmodifiableMap(jobs);
    }

    public void joinJob(String jobId) {
        jobs.put(jobId.toLowerCase(), new PlayerJobData(jobId.toLowerCase()));
        dirty = true;
    }

    public void leaveJob(String jobId) {
        jobs.remove(jobId.toLowerCase());
        dirty = true;
    }


    public void putJobData(String jobId, PlayerJobData data) {
        jobs.put(jobId.toLowerCase(), data);
    }

    public boolean isOnCooldown(String key) {
        Long expiry = cooldowns.get(key);
        if (expiry == null) return false;
        if (System.currentTimeMillis() >= expiry) {
            cooldowns.remove(key);
            return false;
        }
        return true;
    }

    public void setCooldown(String key, long durationMs) {
        if (durationMs > 0) {
            cooldowns.put(key, System.currentTimeMillis() + durationMs);
        }
    }

    public int getTotalLevel() {
        return jobs.values().stream().mapToInt(PlayerJobData::getLevel).sum();
    }

    public int getJobCount() {
        return jobs.size();
    }


    public UUID getUuid() { return uuid; }
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String name) { this.playerName = name; }
    public boolean isDirty() { return dirty; }
    public void setDirty(boolean dirty) { this.dirty = dirty; }
}
