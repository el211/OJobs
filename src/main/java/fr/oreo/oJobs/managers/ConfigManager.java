package fr.oreo.oJobs.managers;

import fr.oreo.oJobs.OJobs;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;


public class ConfigManager {

    private final OJobs plugin;

    private FileConfiguration config;
    private FileConfiguration guiConfig;
    private FileConfiguration langConfig;
    private FileConfiguration storageConfig;

    public ConfigManager(OJobs plugin) {
        this.plugin = plugin;
    }


    public void loadAll() {
        saveDefault("config.yml");
        saveDefault("gui.yml");
        saveDefault("lang.yml");
        saveDefault("storage.yml");

        config        = loadFile("config.yml");
        guiConfig     = loadFile("gui.yml");
        langConfig    = loadFile("lang.yml");
        storageConfig = loadFile("storage.yml");
    }


    private void saveDefault(String resourcePath) {
        File file = new File(plugin.getDataFolder(), resourcePath);
        if (!file.exists()) {
            plugin.saveResource(resourcePath, false);
        }
    }

    private FileConfiguration loadFile(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        YamlConfiguration loaded = YamlConfiguration.loadConfiguration(file);
        InputStream jarStream = plugin.getResource(fileName);
        if (jarStream != null) {
            YamlConfiguration jarDefaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(jarStream, StandardCharsets.UTF_8));
            loaded.setDefaults(jarDefaults);
        }
        return loaded;
    }


    public void saveConfig(FileConfiguration cfg, String fileName) {
        try {
            cfg.save(new File(plugin.getDataFolder(), fileName));
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save " + fileName, e);
        }
    }


    public FileConfiguration getConfig()        { return config; }
    public FileConfiguration getGuiConfig()     { return guiConfig; }
    public FileConfiguration getLangConfig()    { return langConfig; }
    public FileConfiguration getStorageConfig() { return storageConfig; }
}
