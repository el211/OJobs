package fr.oreo.oJobs;

import fr.oreo.oJobs.commands.JobsCommand;
import fr.oreo.oJobs.commands.JobsTabCompleter;
import fr.oreo.oJobs.gui.GuiManager;
import fr.oreo.oJobs.listeners.*;
import fr.oreo.oJobs.managers.*;
import fr.oreo.oJobs.placeholders.JobsPlaceholders;
import fr.oreo.oJobs.storage.StorageManager;
import fr.oreo.oJobs.utils.MessageUtil;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import net.byteflux.libby.BukkitLibraryManager;
import net.byteflux.libby.Library;

public final class OJobs extends JavaPlugin {

    private static OJobs instance;

    private ConfigManager configManager;
    private StorageManager storageManager;
    private JobManager jobManager;
    private PlayerDataManager playerDataManager;
    private BoosterManager boosterManager;
    private PrestigeManager prestigeManager;
    private LeaderboardManager leaderboardManager;
    private GuiManager guiManager;

    private Economy economy;
    private boolean vaultEnabled = false;
    private boolean pApiEnabled  = false;

    @Override
    public void onLoad() {
        loadExternalLibraries();
    }
    @Override
    public void onEnable() {
        instance = this;

        configManager = new ConfigManager(this);
        configManager.loadAll();


        MessageUtil.init(this);

        storageManager = new StorageManager(this);
        if (!storageManager.initialize()) {
            getLogger().severe("Failed to initialize storage backend. Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (!setupEconomy()) {
            getLogger().warning("Vault / economy plugin not found — money rewards disabled.");
        }

        jobManager = new JobManager(this);
        jobManager.loadJobs();

        playerDataManager = new PlayerDataManager(this);
        playerDataManager.startAutoSave();

        boosterManager     = new BoosterManager(this);
        prestigeManager    = new PrestigeManager(this);
        leaderboardManager = new LeaderboardManager(this);

        guiManager = new GuiManager(this);

        registerListeners();

        var cmd = getCommand("jobs");
        if (cmd != null) {
            cmd.setExecutor(new JobsCommand(this));
            cmd.setTabCompleter(new JobsTabCompleter(this));
        }

        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new JobsPlaceholders(this).register();
            pApiEnabled = true;
            getLogger().info("Hooked into PlaceholderAPI.");
        }

        getServer().getOnlinePlayers().forEach(p ->
                playerDataManager.loadPlayer(p.getUniqueId()));

        leaderboardManager.startUpdateTask();

        getLogger().info("oJobs v" + getDescription().getVersion() + " enabled successfully!");
    }

    @Override
    public void onDisable() {
        if (playerDataManager != null) {
            getServer().getOnlinePlayers().forEach(p ->
                    playerDataManager.savePlayer(p.getUniqueId(), true));
        }
        if (storageManager != null) {
            storageManager.shutdown();
        }
        getLogger().info("oJobs disabled. All player data saved.");
    }
    private void loadExternalLibraries() {
        try {
            BukkitLibraryManager libraryManager = new BukkitLibraryManager(this);
            libraryManager.addMavenCentral();

            libraryManager.loadLibrary(Library.builder().groupId("org.mongodb").artifactId("bson").version("4.11.1").build());
            libraryManager.loadLibrary(Library.builder().groupId("org.mongodb").artifactId("mongodb-driver-core").version("4.11.1").build());
            libraryManager.loadLibrary(Library.builder().groupId("org.mongodb").artifactId("mongodb-driver-sync").version("4.11.1").build());

            libraryManager.loadLibrary(Library.builder().groupId("com.zaxxer").artifactId("HikariCP").version("5.0.1").build());
            libraryManager.loadLibrary(Library.builder().groupId("com.mysql").artifactId("mysql-connector-j").version("8.3.0").build());
            libraryManager.loadLibrary(Library.builder().groupId("org.xerial").artifactId("sqlite-jdbc").version("3.44.1.0").build());

            getLogger().info("[LibraryLoader] All dependencies loaded successfully!");
        } catch (Exception e) {
            getLogger().severe("[LibraryLoader] FAILED: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    public void reload() {
        configManager.loadAll();
        jobManager.loadJobs();
        leaderboardManager.startUpdateTask();
        getLogger().info("oJobs reloaded.");
    }


    private void registerListeners() {
        var pm = getServer().getPluginManager();
        pm.registerEvents(new PlayerListener(this),   this);
        pm.registerEvents(new BlockListener(this),    this);
        pm.registerEvents(new MobListener(this),      this);
        pm.registerEvents(new FishingListener(this),  this);
        pm.registerEvents(new CraftingListener(this), this);
        pm.registerEvents(new SmeltingListener(this), this);
        pm.registerEvents(new FarmingListener(this),  this);

        if (pm.isPluginEnabled("ItemsAdder")) {
            pm.registerEvents(new ItemsAdderBlockListener(this), this);
            getLogger().info("Hooked into ItemsAdder — custom blocks & furniture supported.");
        }
        if (pm.isPluginEnabled("Nexo")) {
            pm.registerEvents(new NexoBlockListener(this), this);
            getLogger().info("Hooked into Nexo — custom blocks & furniture supported.");
        }
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp =
                getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        vaultEnabled = economy != null;
        if (vaultEnabled) getLogger().info("Hooked into Vault economy.");
        return vaultEnabled;
    }


    public static OJobs getInstance()               { return instance; }
    public ConfigManager getConfigManager()         { return configManager; }
    public StorageManager getStorageManager()       { return storageManager; }
    public JobManager getJobManager()               { return jobManager; }
    public PlayerDataManager getPlayerDataManager() { return playerDataManager; }
    public BoosterManager getBoosterManager()       { return boosterManager; }
    public PrestigeManager getPrestigeManager()     { return prestigeManager; }
    public LeaderboardManager getLeaderboardManager() { return leaderboardManager; }
    public GuiManager getGuiManager()               { return guiManager; }
    public Economy getEconomy()                     { return economy; }
    public boolean isVaultEnabled()                 { return vaultEnabled; }
    public boolean isPApiEnabled()                  { return pApiEnabled; }
}