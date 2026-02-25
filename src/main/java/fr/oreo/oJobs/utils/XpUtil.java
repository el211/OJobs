package fr.oreo.oJobs.utils;

import fr.oreo.oJobs.models.Job;


public final class XpUtil {

    private XpUtil() {}


    public static double getXpRequired(Job job, int level) {
        if (level < 1) level = 1;
        return switch (job.getFormulaType()) {
            case "LINEAR"      -> job.getFormulaBase() * level * job.getFormulaMultiplier();
            case "POLYNOMIAL"  -> job.getFormulaBase() * Math.pow(level, job.getFormulaMultiplier());
            default            -> job.getFormulaBase() * Math.pow(job.getFormulaMultiplier(), level - 1);
        };
    }


    public static double getTotalXpForLevel(Job job, int targetLevel) {
        double total = 0;
        for (int i = 1; i < targetLevel; i++) {
            total += getXpRequired(job, i);
        }
        return total;
    }


    public static String formatXp(double xp) {
        if (xp == Math.floor(xp) && !Double.isInfinite(xp)) {
            return String.valueOf((long) xp);
        }
        return String.format("%.1f", xp);
    }


    public static double getProgressFraction(double currentXp, double xpRequired) {
        if (xpRequired <= 0) return 1.0;
        return Math.min(1.0, Math.max(0.0, currentXp / xpRequired));
    }
}
