package fr.oreo.oJobs.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import fr.oreo.oJobs.OJobs;
import fr.oreo.oJobs.models.LeaderboardEntry;
import fr.oreo.oJobs.models.PlayerData;
import fr.oreo.oJobs.models.PlayerJobData;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.sql.*;
import java.util.*;


public class SQLiteStorage implements Storage {

    private final OJobs plugin;
    private HikariDataSource dataSource;

    public SQLiteStorage(OJobs plugin) {
        this.plugin = plugin;
    }


    @Override
    public boolean initialize() {
        FileConfiguration cfg = plugin.getConfigManager().getStorageConfig();
        String fileName = cfg.getString("sqlite.file", "data.db");
        File dbFile = new File(plugin.getDataFolder(), fileName);

        HikariConfig hikari = new HikariConfig();
        hikari.setDriverClassName("org.sqlite.JDBC");
        hikari.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        hikari.setMaximumPoolSize(1);
        hikari.setConnectionTestQuery("SELECT 1");
        hikari.setPoolName("oJobs-SQLite");

        try {
            dataSource = new HikariDataSource(hikari);
            createTables();
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("[SQLiteStorage] Failed to initialize: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void createTables() throws SQLException {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS ojobs_players (
                    uuid TEXT NOT NULL PRIMARY KEY,
                    name TEXT NOT NULL
                )""");
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS ojobs_job_data (
                    uuid      TEXT NOT NULL,
                    job_id    TEXT NOT NULL,
                    level     INTEGER NOT NULL DEFAULT 1,
                    xp        REAL    NOT NULL DEFAULT 0,
                    prestige  INTEGER NOT NULL DEFAULT 0,
                    total_xp  REAL    NOT NULL DEFAULT 0,
                    joined_at INTEGER NOT NULL,
                    PRIMARY KEY (uuid, job_id)
                )""");
        }
    }

    @Override
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) dataSource.close();
    }


    @Override
    public PlayerData loadPlayer(UUID uuid) {
        String sql = "SELECT name FROM ojobs_players WHERE uuid = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return null;

            PlayerData data = new PlayerData(uuid, rs.getString("name"));

            String jobSql = "SELECT * FROM ojobs_job_data WHERE uuid = ?";
            try (PreparedStatement jps = conn.prepareStatement(jobSql)) {
                jps.setString(1, uuid.toString());
                ResultSet jrs = jps.executeQuery();
                while (jrs.next()) {
                    String jobId   = jrs.getString("job_id");
                    int level      = jrs.getInt("level");
                    double xp      = jrs.getDouble("xp");
                    int prestige   = jrs.getInt("prestige");
                    double totalXp = jrs.getDouble("total_xp");
                    long joinedAt  = jrs.getLong("joined_at");
                    data.putJobData(jobId, new PlayerJobData(jobId, level, xp, prestige, totalXp, joinedAt));
                }
            }
            return data;
        } catch (SQLException e) {
            plugin.getLogger().severe("[SQLiteStorage] Load failed for " + uuid);
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void savePlayer(PlayerData data) {
        String upsertPlayer = "INSERT OR REPLACE INTO ojobs_players (uuid, name) VALUES (?, ?)";
        String upsertJob    = """
            INSERT OR REPLACE INTO ojobs_job_data
              (uuid, job_id, level, xp, prestige, total_xp, joined_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)""";

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(upsertPlayer)) {
                    ps.setString(1, data.getUuid().toString());
                    ps.setString(2, data.getPlayerName());
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(upsertJob)) {
                    for (Map.Entry<String, PlayerJobData> e : data.getJobs().entrySet()) {
                        PlayerJobData jd = e.getValue();
                        ps.setString(1, data.getUuid().toString());
                        ps.setString(2, e.getKey());
                        ps.setInt(3, jd.getLevel());
                        ps.setDouble(4, jd.getXp());
                        ps.setInt(5, jd.getPrestige());
                        ps.setDouble(6, jd.getTotalXpEarned());
                        ps.setLong(7, jd.getJoinedAt());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[SQLiteStorage] Save failed for " + data.getUuid());
            e.printStackTrace();
        }
    }

    @Override
    public List<LeaderboardEntry> getLeaderboard(String jobId, int limit) {
        List<LeaderboardEntry> entries = new ArrayList<>();
        String sql = """
            SELECT p.uuid, p.name, d.level, d.xp, d.prestige, d.total_xp
            FROM ojobs_job_data d
            JOIN ojobs_players p ON p.uuid = d.uuid
            WHERE d.job_id = ?
            ORDER BY d.prestige DESC, d.level DESC, d.total_xp DESC
            LIMIT ?""";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, jobId.toLowerCase());
            ps.setInt(2, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                entries.add(new LeaderboardEntry(
                        UUID.fromString(rs.getString("uuid")),
                        rs.getString("name"),
                        jobId,
                        rs.getInt("level"),
                        rs.getDouble("xp"),
                        rs.getInt("prestige"),
                        rs.getDouble("total_xp")));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[SQLiteStorage] Leaderboard query failed: " + e.getMessage());
        }
        return entries;
    }

    @Override
    public List<LeaderboardEntry> getGlobalLeaderboard(int limit) {
        List<LeaderboardEntry> entries = new ArrayList<>();
        String sql = """
            SELECT p.uuid, p.name,
                   SUM(d.level) AS total_level,
                   SUM(d.total_xp) AS grand_total_xp
            FROM ojobs_job_data d
            JOIN ojobs_players p ON p.uuid = d.uuid
            GROUP BY d.uuid
            ORDER BY total_level DESC, grand_total_xp DESC
            LIMIT ?""";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                entries.add(new LeaderboardEntry(
                        UUID.fromString(rs.getString("uuid")),
                        rs.getString("name"),
                        "global",
                        rs.getInt("total_level"),
                        0, 0,
                        rs.getDouble("grand_total_xp")));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[SQLiteStorage] Global leaderboard failed: " + e.getMessage());
        }
        return entries;
    }
}
