package fr.oreo.oJobs.storage;

import fr.oreo.oJobs.models.LeaderboardEntry;
import fr.oreo.oJobs.models.PlayerData;

import java.util.List;
import java.util.UUID;


public interface Storage {


    boolean initialize();

    void shutdown();


    PlayerData loadPlayer(UUID uuid);

    void savePlayer(PlayerData data);


    List<LeaderboardEntry> getLeaderboard(String jobId, int limit);


    List<LeaderboardEntry> getGlobalLeaderboard(int limit);
}
