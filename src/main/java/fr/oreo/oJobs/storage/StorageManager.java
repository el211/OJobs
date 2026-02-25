package fr.oreo.oJobs.storage;

import fr.oreo.oJobs.OJobs;


public class StorageManager {

    private final OJobs plugin;
    private Storage storage;

    public StorageManager(OJobs plugin) {
        this.plugin = plugin;
    }


    public boolean initialize() {
        String type = plugin.getConfigManager().getStorageConfig()
                .getString("type", "YAML").toUpperCase();

        storage = switch (type) {
            case "MYSQL"   -> new MySQLStorage(plugin);
            case "SQLITE"  -> new SQLiteStorage(plugin);
            case "MONGODB" -> new MongoDBStorage(plugin);
            default        -> new YamlStorage(plugin);
        };

        plugin.getLogger().info("Using storage backend: " + type);
        return storage.initialize();
    }

    public void shutdown() {
        if (storage != null) storage.shutdown();
    }

    public Storage getStorage() {
        return storage;
    }
}