package com.galaxyrealms.api;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Manages pending rank rewards for players who were offline when their rank was granted.
 * Stores data in a YAML file so it persists across server restarts.
 */
public class PendingRewardsManager {
    
    private final GalaxyRealmsAPI plugin;
    private final File dataFile;
    private FileConfiguration dataConfig;
    
    // In-memory cache of pending rewards: UUID -> List of ranks
    private final Map<UUID, List<String>> pendingRewards = new HashMap<>();
    
    public PendingRewardsManager(GalaxyRealmsAPI plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "pending_rewards.yml");
        loadData();
    }
    
    /**
     * Load pending rewards from file
     */
    private void loadData() {
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getAPILogger().severe("Could not create pending_rewards.yml: " + e.getMessage());
            }
        }
        
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        
        // Load all pending rewards into memory
        if (dataConfig.contains("pending")) {
            for (String uuidStr : dataConfig.getConfigurationSection("pending").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    List<String> ranks = dataConfig.getStringList("pending." + uuidStr);
                    if (!ranks.isEmpty()) {
                        pendingRewards.put(uuid, new ArrayList<>(ranks));
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getAPILogger().warning("Invalid UUID in pending_rewards.yml: " + uuidStr);
                }
            }
        }
        
        plugin.getAPILogger().info("Loaded " + pendingRewards.size() + " players with pending rewards");
    }
    
    /**
     * Save pending rewards to file
     */
    private void saveData() {
        // Clear existing data
        dataConfig.set("pending", null);
        
        // Save current pending rewards
        for (Map.Entry<UUID, List<String>> entry : pendingRewards.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                dataConfig.set("pending." + entry.getKey().toString(), entry.getValue());
            }
        }
        
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getAPILogger().severe("Could not save pending_rewards.yml: " + e.getMessage());
        }
    }
    
    /**
     * Add a pending reward for a player
     */
    public void addPendingReward(UUID playerUUID, String rank) {
        pendingRewards.computeIfAbsent(playerUUID, k -> new ArrayList<>()).add(rank);
        saveData();
        plugin.getAPILogger().info("Added pending reward '" + rank + "' for player " + playerUUID);
    }
    
    /**
     * Check if a player has pending rewards
     */
    public boolean hasPendingRewards(UUID playerUUID) {
        return pendingRewards.containsKey(playerUUID) && !pendingRewards.get(playerUUID).isEmpty();
    }
    
    /**
     * Get and clear pending rewards for a player
     */
    public List<String> getAndClearPendingRewards(UUID playerUUID) {
        List<String> rewards = pendingRewards.remove(playerUUID);
        if (rewards != null && !rewards.isEmpty()) {
            saveData();
            return rewards;
        }
        return Collections.emptyList();
    }
    
    /**
     * Get pending rewards without clearing them
     */
    public List<String> getPendingRewards(UUID playerUUID) {
        return pendingRewards.getOrDefault(playerUUID, Collections.emptyList());
    }
}
