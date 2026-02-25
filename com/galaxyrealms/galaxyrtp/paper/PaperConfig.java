/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.plugin.java.JavaPlugin
 */
package com.galaxyrealms.galaxyrtp.paper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class PaperConfig {
    private final JavaPlugin plugin;
    private FileConfiguration config;
    private String thisServerName;
    private String hubServerName;
    private List<String> serverNames;
    private SpawnConfig spawn;
    private List<WorldEntry> worlds;
    private Map<String, ServerRadiusConfig> servers;
    private int cooldownDefaultSeconds;
    private int cooldownFastSeconds;
    private int rtpMaxAttempts;
    private Map<String, String> messages;

    public PaperConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        this.reload();
    }

    public void reload() {
        ConfigurationSection cooldownSection;
        this.plugin.reloadConfig();
        this.config = this.plugin.getConfig();
        this.thisServerName = this.config.getString("this-server-name", "hub");
        this.hubServerName = this.config.getString("hub-server-name", "hub");
        this.serverNames = this.config.getStringList("server-names");
        if (this.serverNames == null) {
            this.serverNames = List.of("hub", "eu", "usa");
        }
        ConfigurationSection spawnSection = this.config.getConfigurationSection("spawn");
        this.spawn = new SpawnConfig();
        if (spawnSection != null) {
            this.spawn.worldName = spawnSection.getString("world", "world");
            this.spawn.x = spawnSection.getDouble("x", 0.5);
            this.spawn.y = spawnSection.getDouble("y", 64.0);
            this.spawn.z = spawnSection.getDouble("z", 0.5);
            this.spawn.yaw = (float)spawnSection.getDouble("yaw", 0.0);
            this.spawn.pitch = (float)spawnSection.getDouble("pitch", 0.0);
        }
        this.worlds = new ArrayList<WorldEntry>();
        for (Object entry : this.config.getMapList("worlds")) {
            Map e = entry;
            String key = (String)e.get("key");
            String worldName = (String)e.get("world-name");
            String displayName = (String)e.get("display-name");
            if (key == null || worldName == null) continue;
            this.worlds.add(new WorldEntry(key, worldName, displayName != null ? displayName : key));
        }
        if (this.worlds.isEmpty()) {
            this.worlds.add(new WorldEntry("overworld", "world", "&aOverworld"));
            this.worlds.add(new WorldEntry("nether", "world_nether", "&cNether"));
            this.worlds.add(new WorldEntry("end", "world_the_end", "&eThe End"));
        }
        this.servers = new HashMap<String, ServerRadiusConfig>();
        ConfigurationSection serversSection = this.config.getConfigurationSection("servers");
        if (serversSection != null) {
            for (String serverKey : serversSection.getKeys(false)) {
                ConfigurationSection s = serversSection.getConfigurationSection(serverKey);
                if (s == null) continue;
                ServerRadiusConfig src = new ServerRadiusConfig();
                src.defaultRadius = s.getInt("default-radius", 2000);
                src.groupRadiusOverrides = new HashMap<String, Integer>();
                ConfigurationSection groups = s.getConfigurationSection("group-radius-overrides");
                if (groups != null) {
                    for (String group : groups.getKeys(false)) {
                        src.groupRadiusOverrides.put(group, groups.getInt(group));
                    }
                }
                this.servers.put(serverKey, src);
            }
        }
        this.cooldownDefaultSeconds = (cooldownSection = this.config.getConfigurationSection("cooldown")) != null ? cooldownSection.getInt("default-seconds", 60) : 60;
        this.cooldownFastSeconds = cooldownSection != null ? cooldownSection.getInt("fast-cooldown-seconds", 15) : 15;
        ConfigurationSection rtpSection = this.config.getConfigurationSection("rtp");
        this.rtpMaxAttempts = rtpSection != null ? rtpSection.getInt("max-attempts", 120) : 120;
        this.messages = new HashMap<String, String>();
        ConfigurationSection msgSection = this.config.getConfigurationSection("messages");
        if (msgSection != null) {
            for (String key : msgSection.getKeys(false)) {
                this.messages.put(key, msgSection.getString(key, ""));
            }
        }
    }

    public boolean isHub() {
        if (this.hubServerName == null || this.thisServerName == null) {
            return false;
        }
        if (this.hubServerName.equalsIgnoreCase(this.thisServerName)) {
            return true;
        }
        String a = this.thisServerName.toLowerCase();
        String b = this.hubServerName.toLowerCase();
        return !(!a.equals("hub") && !a.equals("spawn") && !a.equals("lobby") || !b.equals("hub") && !b.equals("spawn") && !b.equals("lobby"));
    }

    public String getThisServerName() {
        return this.thisServerName;
    }

    public String getHubServerName() {
        return this.hubServerName;
    }

    public List<String> getServerNames() {
        return this.serverNames;
    }

    public SpawnConfig getSpawn() {
        return this.spawn;
    }

    public List<WorldEntry> getWorlds() {
        return this.worlds;
    }

    public Map<String, ServerRadiusConfig> getServers() {
        return this.servers;
    }

    public int getCooldownDefaultSeconds() {
        return this.cooldownDefaultSeconds;
    }

    public int getCooldownFastSeconds() {
        return this.cooldownFastSeconds;
    }

    public int getRtpMaxAttempts() {
        return this.rtpMaxAttempts;
    }

    public String getMessage(String key) {
        return this.messages.getOrDefault(key, "<gray>" + key);
    }

    public String getMessage(String key, Map<String, String> placeholders) {
        String s = this.getMessage(key);
        if (placeholders != null) {
            for (Map.Entry<String, String> e : placeholders.entrySet()) {
                s = s.replace("{" + e.getKey() + "}", e.getValue());
            }
        }
        return s;
    }

    public String getWorldNameByKey(String key) {
        for (WorldEntry w : this.worlds) {
            if (!w.key.equalsIgnoreCase(key)) continue;
            return w.worldName;
        }
        return null;
    }

    public String getWorldNameForServer(String serverName, String worldKey) {
        String name;
        ConfigurationSection worldsSection;
        if (serverName == null || worldKey == null) {
            return this.getWorldNameByKey(worldKey);
        }
        ConfigurationSection serverSection = this.config.getConfigurationSection("servers." + serverName);
        if (serverSection != null && (worldsSection = serverSection.getConfigurationSection("worlds")) != null && (name = worldsSection.getString(worldKey)) != null && !name.isEmpty()) {
            return name;
        }
        return this.getWorldNameByKey(worldKey);
    }

    public boolean isRtpServer(String serverName) {
        if (serverName == null) {
            return false;
        }
        if (this.hubServerName != null && this.hubServerName.equalsIgnoreCase(serverName)) {
            return false;
        }
        if (this.serverNames == null) {
            return "eu".equalsIgnoreCase(serverName) || "usa".equalsIgnoreCase(serverName);
        }
        for (String s : this.serverNames) {
            if (!s.equalsIgnoreCase(serverName)) continue;
            return true;
        }
        return false;
    }

    public static final class SpawnConfig {
        public String worldName;
        public double x;
        public double y;
        public double z;
        public float yaw;
        public float pitch;
    }

    public static final class WorldEntry {
        public final String key;
        public final String worldName;
        public final String displayName;

        public WorldEntry(String key, String worldName, String displayName) {
            this.key = key;
            this.worldName = worldName;
            this.displayName = displayName;
        }
    }

    public static final class ServerRadiusConfig {
        public int defaultRadius;
        public Map<String, Integer> groupRadiusOverrides;
    }
}

