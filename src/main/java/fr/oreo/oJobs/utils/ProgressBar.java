package fr.oreo.oJobs.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;


public final class ProgressBar {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private ProgressBar() {}


    public static Component build(double current, double max,
                                  int length,
                                  String filledChar, String emptyChar,
                                  String filledColor, String emptyColor) {
        if (length <= 0) length = 20;
        if (max <= 0) max = 1;

        int filled = (int) Math.round(Math.min(1.0, current / max) * length);
        int empty  = length - filled;

        StringBuilder sb = new StringBuilder();
        if (filled > 0) {
            sb.append(filledColor)
              .append(filledChar.repeat(filled));
        }
        if (empty > 0) {
            sb.append(emptyColor)
              .append(emptyChar.repeat(empty));
        }

        return MM.deserialize(sb.toString());
    }


    public static String buildString(double current, double max,
                                     int length,
                                     String filledChar, String emptyChar,
                                     String filledColor, String emptyColor) {
        if (length <= 0) length = 20;
        if (max <= 0) max = 1;

        int filled = (int) Math.round(Math.min(1.0, current / max) * length);
        int empty  = length - filled;

        StringBuilder sb = new StringBuilder();
        if (filled > 0) sb.append(filledColor).append(filledChar.repeat(filled));
        if (empty  > 0) sb.append(emptyColor).append(emptyChar.repeat(empty));
        return sb.toString();
    }

    public static String formatPercent(double current, double max) {
        if (max <= 0) return "100%";
        return String.format("%.1f%%", Math.min(100.0, current / max * 100.0));
    }
}
