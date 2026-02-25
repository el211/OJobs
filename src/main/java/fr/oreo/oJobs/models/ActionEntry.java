package fr.oreo.oJobs.models;


public class ActionEntry {

    private final String key;
    private final double xp;
    private final double chance;
    private final int levelRequirement;

    public ActionEntry(String key, double xp, double chance, int levelRequirement) {
        this.key = key.toUpperCase();
        this.xp = xp;
        this.chance = Math.min(1.0, Math.max(0.0, chance));
        this.levelRequirement = Math.max(0, levelRequirement);
    }


    public boolean roll() {
        return chance >= 1.0 || Math.random() <= chance;
    }


    public String getKey() { return key; }
    public double getXp() { return xp; }
    public double getChance() { return chance; }
    public int getLevelRequirement() { return levelRequirement; }
}
