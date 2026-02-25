package com.galaxyrealms.api;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public class ConfigManager {
    
    private final GalaxyRealmsAPI plugin;
    private FileConfiguration config;
    
    public ConfigManager(GalaxyRealmsAPI plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }
    
    public void loadConfig() {
        plugin.reloadConfig();
        config = plugin.getConfig();
    }
    
    public boolean isEnabled() {
        // Check environment variable first, then config
        String envEnabled = System.getenv("GALAXY_API_ENABLED");
        if (envEnabled != null) {
            return Boolean.parseBoolean(envEnabled);
        }
        return config.getBoolean("api.enabled", true);
    }
    
    public int getPort() {
        // Check environment variable first, then config
        String envPort = System.getenv("GALAXY_API_PORT");
        if (envPort != null) {
            try {
                return Integer.parseInt(envPort);
            } catch (NumberFormatException e) {
                plugin.getAPILogger().warning("Invalid GALAXY_API_PORT environment variable, using config value");
            }
        }
        return config.getInt("api.port", 8080);
    }
    
    public String getHost() {
        // Check environment variable first, then config
        String envHost = System.getenv("GALAXY_API_HOST");
        if (envHost != null && !envHost.isEmpty()) {
            return envHost;
        }
        return config.getString("api.host", "0.0.0.0");
    }
    
    public String getSecretKey() {
        // ALWAYS check environment variable first for security
        String envKey = System.getenv("GALAXY_API_SECRET_KEY");
        if (envKey != null && !envKey.isEmpty()) {
            return envKey;
        }
        // Fallback to config (with warning)
        String configKey = config.getString("api.secret-key", "CHANGE_THIS");
        if ("CHANGE_THIS".equals(configKey) || configKey.length() < 20) {
            plugin.getAPILogger().warning("WARNING: Using insecure or default secret key! Set GALAXY_API_SECRET_KEY environment variable.");
        }
        return configKey;
    }
    
    public String getBasePath() {
        return config.getString("api.base-path", "/api");
    }
    
    public boolean isAuthRequired() {
        return config.getBoolean("security.require-auth", true);
    }
    
    public List<String> getAllowedIPs() {
        return config.getStringList("security.allowed-ips");
    }
    
    public boolean isRateLimitEnabled() {
        return config.getBoolean("security.rate-limit.enabled", true);
    }
    
    public int getMaxRequests() {
        return config.getInt("security.rate-limit.max-requests", 60);
    }
    
    public List<String> getAvailableRanks() {
        return config.getStringList("ranks.available");
    }
    
    public String getDefaultRank() {
        return config.getString("ranks.default", "default");
    }
    
    public boolean isRequireOnline() {
        return config.getBoolean("ranks.require-online", true);
    }
    
    public boolean shouldRemoveOldRanks() {
        return config.getBoolean("ranks.remove-old-ranks", false);
    }
    
    public boolean isValidRank(String rank) {
        List<String> availableRanks = getAvailableRanks();
        return availableRanks.contains(rank.toLowerCase());
    }
    
    public boolean isItemsEnabled() {
        return config.getBoolean("items.enabled", true);
    }
    
    public String getDefaultKit() {
        return config.getString("items.default-kit", "");
    }
    
    public boolean shouldLogRequests() {
        return config.getBoolean("logging.log-requests", true);
    }
    
    public boolean shouldLogSuccess() {
        return config.getBoolean("logging.log-success", true);
    }
    
    public boolean shouldLogFailures() {
        return config.getBoolean("logging.log-failures", true);
    }

    public int getVaultMin() {
        return config.getInt("vaults.min", 5);
    }

    public int getVaultMax() {
        return config.getInt("vaults.max", 25);
    }

    public boolean isValidVault(int vaultNumber) {
        int min = getVaultMin();
        int max = getVaultMax();
        return vaultNumber >= min && vaultNumber <= max;
    }

    public List<String> getCrateKeys() {
        return config.getStringList("crates.keys");
    }

    public boolean isValidCrateKey(String keyId) {
        List<String> keys = getCrateKeys();
        if (keys == null || keys.isEmpty()) return false;
        for (String k : keys) {
            if (k != null && k.equalsIgnoreCase(keyId)) return true;
        }
        return false;
    }

    public boolean isCrateRequireOnline() {
        return config.getBoolean("crates.require-online", true);
    }

    public String getCrateCommand() {
        return config.getString("crates.command", "phoenixcrates");
    }

    /**
     * Get the backend API URL for Discord link verification
     * Environment variable: GALAXY_BACKEND_URL
     * Config: backend.url
     */
    public String getBackendUrl() {
        // Check environment variable first
        String envUrl = System.getenv("GALAXY_BACKEND_URL");
        if (envUrl != null && !envUrl.isEmpty()) {
            return envUrl;
        }
        // Fallback to config
        return config.getString("backend.url", "http://localhost:3001/api/v1");
    }

    /**
     * Get the backend API key for plugin-authenticated endpoints (e.g. top-supporters).
     * Config: backend.api-key
     */
    public String getBackendApiKey() {
        return config.getString("backend.api-key", "");
    }
}
