package com.galaxyrealms.api;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Builds top 5 balance leaderboard from Vault/Essentials (no ajLeaderboards).
 * Refreshes async so the hologram always shows real balances.
 */
public class TopBalanceCache {

    public static class Entry {
        public final String name;
        public final double balance;

        public Entry(String name, double balance) {
            this.name = name != null ? name : "—";
            this.balance = Math.max(0, balance);
        }
    }

    private static final int TOP_N = 5;
    private static final long REFRESH_TICKS = 20L * 30; // 30 seconds

    private final GalaxyRealmsAPI plugin;
    private final List<Entry> cache = new CopyOnWriteArrayList<>();

    public TopBalanceCache(GalaxyRealmsAPI plugin) {
        this.plugin = plugin;
    }

    public void start() {
        plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, this::refresh, 40L);
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this::refresh, REFRESH_TICKS, REFRESH_TICKS);
    }

    public Entry get(int oneBasedIndex) {
        int i = oneBasedIndex - 1;
        if (i < 0 || i >= cache.size()) return null;
        return cache.get(i);
    }

    private void refresh() {
        Economy economy = plugin.getEconomy();
        if (economy == null) {
            cache.clear();
            return;
        }
        Map<String, String> uuidToName = buildUuidToNameMaps();
        OfflinePlayer[] players = Bukkit.getOfflinePlayers();
        List<Entry> list = new ArrayList<>();
        for (OfflinePlayer p : players) {
            if (p == null) continue;
            try {
                double balance = economy.getBalance(p);
                String name = p.getName();
                String uuidStr = p.getUniqueId().toString();
                if (name == null || name.isEmpty()) {
                    name = uuidToName.get(uuidStr);
                    if (name == null) name = uuidToName.get(uuidStr.replace("-", ""));
                }
                if (name == null || name.isEmpty()) name = uuidStr;
                list.add(new Entry(name, balance));
            } catch (Throwable t) {
                // skip
            }
        }
        list.sort(Comparator.comparingDouble((Entry e) -> e.balance).reversed());
        cache.clear();
        int n = Math.min(TOP_N, list.size());
        for (int i = 0; i < n; i++) {
            cache.add(list.get(i));
        }
    }

    /** Build UUID -> name from all Essentials userdata files + usercache.json (try multiple paths). */
    private Map<String, String> buildUuidToNameMaps() {
        Map<String, String> map = new HashMap<>();
        File[] roots = {
            plugin.getDataFolder().getParentFile(),
            plugin.getServer().getWorldContainer(),
            plugin.getDataFolder().getParentFile().getParentFile()
        };
        for (File root : roots) {
            if (root == null) continue;
            File userdataDir = new File(root, "Essentials/userdata");
            if (userdataDir.isDirectory()) {
                File[] files = userdataDir.listFiles((dir, name) -> name != null && name.endsWith(".yml"));
                if (files != null) {
                    for (File f : files) {
                        try {
                            String uuid = f.getName().replace(".yml", "");
                            YamlConfiguration yml = YamlConfiguration.loadConfiguration(f);
                            String name = yml.getString("last-account-name");
                            if (name != null && !name.isEmpty()) {
                                map.put(uuid, name);
                                map.put(uuid.replace("-", ""), name);
                            }
                        } catch (Throwable ignored) { }
                    }
                }
            }
            File usercache = new File(root, "usercache.json");
            if (usercache.isFile()) {
                try {
                    String json = new String(Files.readAllBytes(usercache.toPath()), StandardCharsets.UTF_8);
                    int i = 0;
                    while ((i = json.indexOf("\"uuid\":\"", i)) != -1) {
                        int uuidStart = i + 8;
                        int uuidEnd = json.indexOf('"', uuidStart);
                        if (uuidEnd == -1) break;
                        String uuid = json.substring(uuidStart, uuidEnd);
                        int nameStart = json.indexOf("\"name\":\"", uuidEnd);
                        if (nameStart == -1 || nameStart > uuidEnd + 50) { i = uuidEnd; continue; }
                        nameStart += 8;
                        int nameEnd = json.indexOf('"', nameStart);
                        if (nameEnd == -1) break;
                        String name = json.substring(nameStart, nameEnd);
                        if (!name.isEmpty()) {
                            map.put(uuid, name);
                            map.put(uuid.replace("-", ""), name);
                        }
                        i = nameEnd;
                    }
                } catch (Throwable ignored) { }
            }
        }
        return map;
    }
}
