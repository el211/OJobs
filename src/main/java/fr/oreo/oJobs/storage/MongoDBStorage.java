package fr.oreo.oJobs.storage;

import com.mongodb.client.*;
import com.mongodb.client.model.*;
import fr.oreo.oJobs.OJobs;
import fr.oreo.oJobs.models.LeaderboardEntry;
import fr.oreo.oJobs.models.PlayerData;
import fr.oreo.oJobs.models.PlayerJobData;
import org.bson.Document;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;


public class MongoDBStorage implements Storage {

    private final OJobs plugin;
    private MongoClient mongoClient;
    private MongoDatabase database;
    private MongoCollection<Document> playersCol;
    private MongoCollection<Document> jobDataCol;

    public MongoDBStorage(OJobs plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean initialize() {
        FileConfiguration cfg = plugin.getConfigManager().getStorageConfig();
        String uri       = cfg.getString("mongodb.connection-uri", "mongodb://localhost:27017");
        String dbName    = cfg.getString("mongodb.database", "ojobs");
        String playerCol = cfg.getString("mongodb.collections.players", "players");
        String jobCol    = cfg.getString("mongodb.collections.job-data", "job_data");

        try {
            mongoClient = MongoClients.create(uri);
            database    = mongoClient.getDatabase(dbName);
            playersCol  = database.getCollection(playerCol);
            jobDataCol  = database.getCollection(jobCol);
            createIndexes();

            database.runCommand(new Document("ping", 1));
            plugin.getLogger().info("[MongoDBStorage] Connected to MongoDB: " + dbName);
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("[MongoDBStorage] Failed to initialize: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void createIndexes() {

        jobDataCol.createIndex(Indexes.ascending("uuid"));
        jobDataCol.createIndex(Indexes.compoundIndex(
                Indexes.ascending("job_id"),
                Indexes.descending("prestige"),
                Indexes.descending("level"),
                Indexes.descending("total_xp")
        ));
    }

    @Override
    public void shutdown() {
        if (mongoClient != null) {
            mongoClient.close();
            plugin.getLogger().info("[MongoDBStorage] Connection closed.");
        }
    }

    @Override
    public PlayerData loadPlayer(UUID uuid) {
        try {
            String uuidStr = uuid.toString();

            Document playerDoc = playersCol.find(Filters.eq("_id", uuidStr)).first();
            if (playerDoc == null) return null;

            String name = playerDoc.getString("name");
            PlayerData data = new PlayerData(uuid, name != null ? name : "Unknown");

            FindIterable<Document> jobDocs = jobDataCol.find(Filters.eq("uuid", uuidStr));
            for (Document doc : jobDocs) {
                String jobId    = doc.getString("job_id");
                int level       = doc.getInteger("level", 1);
                double xp       = doc.getDouble("xp") != null ? doc.getDouble("xp") : 0.0;
                int prestige    = doc.getInteger("prestige", 0);
                double totalXp  = doc.getDouble("total_xp") != null ? doc.getDouble("total_xp") : 0.0;
                long joinedAt   = doc.getLong("joined_at") != null ? doc.getLong("joined_at") : System.currentTimeMillis();
                data.putJobData(jobId, new PlayerJobData(jobId, level, xp, prestige, totalXp, joinedAt));
            }

            return data;
        } catch (Exception e) {
            plugin.getLogger().severe("[MongoDBStorage] Load failed for " + uuid);
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void savePlayer(PlayerData data) {
        try {
            String uuidStr = data.getUuid().toString();

            Document playerDoc = new Document("_id", uuidStr)
                    .append("name", data.getPlayerName());
            playersCol.replaceOne(
                    Filters.eq("_id", uuidStr),
                    playerDoc,
                    new ReplaceOptions().upsert(true)
            );


            Set<String> currentJobIds = new HashSet<>();

            for (Map.Entry<String, PlayerJobData> entry : data.getJobs().entrySet()) {
                String jobId = entry.getKey();
                PlayerJobData jd = entry.getValue();
                currentJobIds.add(jobId);

                String docId = uuidStr + ":" + jobId;
                Document jobDoc = new Document("_id", docId)
                        .append("uuid", uuidStr)
                        .append("job_id", jobId)
                        .append("level", jd.getLevel())
                        .append("xp", jd.getXp())
                        .append("prestige", jd.getPrestige())
                        .append("total_xp", jd.getTotalXpEarned())
                        .append("joined_at", jd.getJoinedAt());

                jobDataCol.replaceOne(
                        Filters.eq("_id", docId),
                        jobDoc,
                        new ReplaceOptions().upsert(true)
                );
            }

            jobDataCol.deleteMany(Filters.and(
                    Filters.eq("uuid", uuidStr),
                    Filters.nin("job_id", new ArrayList<>(currentJobIds))
            ));

        } catch (Exception e) {
            plugin.getLogger().severe("[MongoDBStorage] Save failed for " + data.getUuid());
            e.printStackTrace();
        }
    }


    @Override
    public List<LeaderboardEntry> getLeaderboard(String jobId, int limit) {
        List<LeaderboardEntry> entries = new ArrayList<>();
        try {
            FindIterable<Document> docs = jobDataCol.find(Filters.eq("job_id", jobId.toLowerCase()))
                    .sort(Sorts.orderBy(
                            Sorts.descending("prestige"),
                            Sorts.descending("level"),
                            Sorts.descending("total_xp")
                    ))
                    .limit(limit);

            for (Document doc : docs) {
                String uuidStr = doc.getString("uuid");
                UUID uuid = UUID.fromString(uuidStr);

                Document playerDoc = playersCol.find(Filters.eq("_id", uuidStr)).first();
                String name = playerDoc != null ? playerDoc.getString("name") : "Unknown";

                int level      = doc.getInteger("level", 1);
                double xp      = doc.getDouble("xp") != null ? doc.getDouble("xp") : 0.0;
                int prestige   = doc.getInteger("prestige", 0);
                double totalXp = doc.getDouble("total_xp") != null ? doc.getDouble("total_xp") : 0.0;

                entries.add(new LeaderboardEntry(uuid, name, jobId, level, xp, prestige, totalXp));
            }
        } catch (Exception e) {
            plugin.getLogger().severe("[MongoDBStorage] Leaderboard query failed: " + e.getMessage());
        }
        return entries;
    }

    @Override
    public List<LeaderboardEntry> getGlobalLeaderboard(int limit) {
        List<LeaderboardEntry> entries = new ArrayList<>();
        try {
            List<Document> pipeline = List.of(
                    new Document("$group", new Document("_id", "$uuid")
                            .append("total_level", new Document("$sum", "$level"))
                            .append("grand_xp", new Document("$sum", "$total_xp"))
                    ),
                    new Document("$sort", new Document("total_level", -1)
                            .append("grand_xp", -1)
                    ),
                    new Document("$limit", limit)
            );

            AggregateIterable<Document> results = jobDataCol.aggregate(pipeline);
            for (Document doc : results) {
                String uuidStr = doc.getString("_id");
                UUID uuid = UUID.fromString(uuidStr);

                Document playerDoc = playersCol.find(Filters.eq("_id", uuidStr)).first();
                String name = playerDoc != null ? playerDoc.getString("name") : "Unknown";

                int totalLevel  = doc.getInteger("total_level", 0);
                double grandXp  = doc.getDouble("grand_xp") != null ? doc.getDouble("grand_xp") : 0.0;

                entries.add(new LeaderboardEntry(uuid, name, "global", totalLevel, 0, 0, grandXp));
            }
        } catch (Exception e) {
            plugin.getLogger().severe("[MongoDBStorage] Global leaderboard failed: " + e.getMessage());
        }
        return entries;
    }
}