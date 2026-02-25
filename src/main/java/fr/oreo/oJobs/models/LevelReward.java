package fr.oreo.oJobs.models;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;


public class LevelReward {

    private final int level;
    private final double money;
    private final List<String> commands;
    private final List<String> items;
    private final List<String> permissions;
    private final boolean fireFirework;
    private final String sound;
    private final float soundVolume;
    private final float soundPitch;

    public LevelReward(int level, double money, List<String> commands, List<String> items,
                       List<String> permissions, boolean fireFirework,
                       String sound, float soundVolume, float soundPitch) {
        this.level = level;
        this.money = money;
        this.commands = List.copyOf(commands);
        this.items = List.copyOf(items);
        this.permissions = List.copyOf(permissions);
        this.fireFirework = fireFirework;
        this.sound = sound;
        this.soundVolume = soundVolume;
        this.soundPitch = soundPitch;
    }


    public static LevelReward fromSection(int level, ConfigurationSection section) {
        if (section == null) {
            return new LevelReward(level, 0, List.of(), List.of(), List.of(), false, null, 1f, 1f);
        }
        double money        = section.getDouble("money", 0);
        List<String> cmds   = section.getStringList("commands");
        List<String> items  = section.getStringList("items");
        List<String> perms  = section.getStringList("permissions");
        boolean firework    = section.getBoolean("firework", false);
        String sound        = section.getString("sound", null);
        float vol           = (float) section.getDouble("sound-volume", 1.0);
        float pitch         = (float) section.getDouble("sound-pitch", 1.0);
        return new LevelReward(level, money,
                new ArrayList<>(cmds), new ArrayList<>(items), new ArrayList<>(perms),
                firework, sound, vol, pitch);
    }


    public int getLevel() { return level; }
    public double getMoney() { return money; }
    public List<String> getCommands() { return commands; }
    public List<String> getItems() { return items; }
    public List<String> getPermissions() { return permissions; }
    public boolean isFireFirework() { return fireFirework; }
    public String getSound() { return sound; }
    public float getSoundVolume() { return soundVolume; }
    public float getSoundPitch() { return soundPitch; }
}
