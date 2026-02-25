package fr.oreo.oJobs.managers;

import fr.oreo.oJobs.OJobs;
import fr.oreo.oJobs.models.Job;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;


public class JobManager {

    private final OJobs plugin;
    private final Map<String, Job> jobMap = new LinkedHashMap<>();
    private File jobsDirectory;

    public JobManager(OJobs plugin) {
        this.plugin = plugin;
    }

    public void loadJobs() {
        jobMap.clear();

        jobsDirectory = new File(plugin.getDataFolder(), "jobs");
        if (!jobsDirectory.exists()) {
            jobsDirectory.mkdirs();
            extractDefaultJobs();
        }

        File[] files = jobsDirectory.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null || files.length == 0) {
            plugin.getLogger().warning("No job files found in jobs/ folder!");
            return;
        }

        for (File file : files) {
            String id = file.getName().replace(".yml", "").toLowerCase();
            try {
                FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
                Job job = new Job(id, cfg);
                jobMap.put(id, job);
                plugin.getLogger().info("Loaded job: " + job.getDisplayName() + " (id=" + id + ")");
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load job file: " + file.getName());
                e.printStackTrace();
            }
        }

        plugin.getLogger().info("Loaded " + jobMap.size() + " job(s) from /jobs/");
    }

    private void extractDefaultJobs() {
        String[] defaults = {"miner.yml", "farmer.yml", "hunter.yml"};
        for (String name : defaults) {
            String resource = "jobs/" + name;
            InputStream in = plugin.getResource(resource);
            if (in == null) continue;
            File out = new File(jobsDirectory, name);
            if (!out.exists()) {
                try (InputStream is = in;
                     OutputStream os = new FileOutputStream(out)) {
                    is.transferTo(os);
                    plugin.getLogger().info("Extracted default job: " + name);
                } catch (IOException e) {
                    plugin.getLogger().warning("Could not extract " + name + ": " + e.getMessage());
                }
            }
        }
    }


    public Optional<Job> getJob(String id) {
        return Optional.ofNullable(jobMap.get(id.toLowerCase()));
    }

    public Collection<Job> getJobs() {
        return Collections.unmodifiableCollection(jobMap.values());
    }

    public Map<String, Job> getJobMap() {
        return Collections.unmodifiableMap(jobMap);
    }

    public boolean jobExists(String id) {
        return jobMap.containsKey(id.toLowerCase());
    }


    public int countPlayers(String jobId) {
        return (int) plugin.getPlayerDataManager()
                .getAllPlayerData().stream()
                .filter(pd -> pd.hasJob(jobId))
                .count();
    }

    public File getJobsDirectory() {
        return jobsDirectory;
    }
}
