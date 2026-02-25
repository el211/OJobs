package fr.oreo.oJobs.models;


public class PlayerJobData {

    private final String jobId;
    private int level;
    private double xp;
    private int prestige;
    private double totalXpEarned;
    private final long joinedAt;

    public PlayerJobData(String jobId) {
        this.jobId = jobId.toLowerCase();
        this.level = 1;
        this.xp = 0;
        this.prestige = 0;
        this.totalXpEarned = 0;
        this.joinedAt = System.currentTimeMillis();
    }

    public PlayerJobData(String jobId, int level, double xp, int prestige,
                         double totalXpEarned, long joinedAt) {
        this.jobId = jobId.toLowerCase();
        this.level = Math.max(1, level);
        this.xp = Math.max(0, xp);
        this.prestige = Math.max(0, prestige);
        this.totalXpEarned = Math.max(0, totalXpEarned);
        this.joinedAt = joinedAt;
    }


    public double addXp(double amount) {
        double actual = Math.max(0, amount);
        this.xp += actual;
        this.totalXpEarned += actual;
        return actual;
    }


    public String getJobId() { return jobId; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = Math.max(1, level); }

    public double getXp() { return xp; }
    public void setXp(double xp) { this.xp = Math.max(0, xp); }

    public int getPrestige() { return prestige; }
    public void setPrestige(int prestige) { this.prestige = Math.max(0, prestige); }

    public double getTotalXpEarned() { return totalXpEarned; }
    public void setTotalXpEarned(double v) { this.totalXpEarned = Math.max(0, v); }

    public long getJoinedAt() { return joinedAt; }
}
