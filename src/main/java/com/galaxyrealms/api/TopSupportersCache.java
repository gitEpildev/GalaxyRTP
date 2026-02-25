package com.galaxyrealms.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Fetches and caches top supporters (by website spending) from the store backend.
 * Used by placeholders %galaxyrealms_topsupporter_1_username%, %galaxyrealms_topsupporter_1_amount%, etc.
 */
public class TopSupportersCache {

    public static class Entry {
        public final String username;
        public final int totalCents;

        public Entry(String username, int totalCents) {
            this.username = username != null ? username : "";
            this.totalCents = Math.max(0, totalCents);
        }
    }

    private final GalaxyRealmsAPI plugin;
    private final List<Entry> cache = new CopyOnWriteArrayList<>();
    private static final int REFRESH_TICKS = 20 * 20; // 20 seconds

    public TopSupportersCache(GalaxyRealmsAPI plugin) {
        this.plugin = plugin;
    }

    public void start() {
        plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, this::refresh, 20L); // 1 second initial
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this::refresh, REFRESH_TICKS, REFRESH_TICKS);
    }

    public List<Entry> getTopSupporters() {
        return cache.isEmpty() ? Collections.emptyList() : new ArrayList<>(cache);
    }

    public Entry get(int oneBasedIndex) {
        int i = oneBasedIndex - 1;
        if (i < 0 || i >= cache.size()) return null;
        return cache.get(i);
    }

    private void refresh() {
        String baseUrl = plugin.getConfigManager().getBackendUrl();
        String apiKey = plugin.getConfigManager().getBackendApiKey();
        if (baseUrl == null || baseUrl.isEmpty() || apiKey == null || apiKey.isEmpty()) {
            cache.clear();
            plugin.getAPILogger().warning("[TopSupporters] Backend URL or api-key not set. Set backend.url and backend.api-key in config.");
            return;
        }
        try {
            String urlStr = baseUrl + "/deliveries/top-supporters?limit=3";
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("X-API-Key", apiKey);
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            int code = conn.getResponseCode();
            StringBuilder sb = new StringBuilder();
            java.io.InputStream stream = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
            if (stream != null) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                }
            }
            if (code < 200 || code >= 300) {
                cache.clear();
                plugin.getAPILogger().warning("[TopSupporters] Backend returned " + code + " for " + urlStr + (sb.length() > 0 ? " — " + sb.toString().replaceAll("\\s+", " ").trim() : ""));
                if (code == 401) plugin.getAPILogger().warning("[TopSupporters] Check backend.api-key matches store MINECRAFT_API_KEY.");
                return;
            }
            JsonObject root = JsonParser.parseString(sb.toString()).getAsJsonObject();
            JsonArray arr = root.has("topSupporters") ? root.getAsJsonArray("topSupporters") : new JsonArray();
            List<Entry> next = new ArrayList<>();
            for (JsonElement el : arr) {
                if (!el.isJsonObject()) continue;
                JsonObject o = el.getAsJsonObject();
                String username = o.has("username") ? o.get("username").getAsString() : "";
                int totalCents = o.has("totalCents") ? o.get("totalCents").getAsInt() : 0;
                next.add(new Entry(username, totalCents));
            }
            cache.clear();
            cache.addAll(next);
            if (!next.isEmpty()) {
                plugin.getAPILogger().info("[TopSupporters] Loaded " + next.size() + " top supporters from store.");
                List<Entry> copy = new ArrayList<>(next);
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> refreshFancyNpcsSkins(copy), 40L);
            } else {
                plugin.getAPILogger().info("[TopSupporters] Store returned 0 supporters (no paid/completed purchases?).");
            }
        } catch (Throwable t) {
            cache.clear();
            plugin.getAPILogger().warning("[TopSupporters] Failed to fetch: " + t.getMessage());
        }
    }

    /** Run on main thread: set FancyNPCs top1/top2/top3 skins to top supporter usernames and update. */
    private void refreshFancyNpcsSkins(List<Entry> entries) {
        if (entries.isEmpty()) return;
        try {
            org.bukkit.plugin.Plugin fn = plugin.getServer().getPluginManager().getPlugin("FancyNpcs");
            if (fn == null) return;
            Object npcManager = fn.getClass().getMethod("getNpcManager").invoke(fn);
            if (npcManager == null) return;
            java.lang.reflect.Method getNpc = npcManager.getClass().getMethod("getNpc", String.class);
            String[] names = { "top1", "top2", "top3" };
            for (int i = 0; i < names.length && i < entries.size(); i++) {
                Object npc = getNpc.invoke(npcManager, names[i]);
                if (npc == null) continue;
                String username = entries.get(i).username;
                if (username == null || username.isEmpty()) username = "Steve";
                try {
                    Object data = npc.getClass().getMethod("getData").invoke(npc);
                    if (data != null) data.getClass().getMethod("setSkin", String.class).invoke(data, username);
                    npc.getClass().getMethod("updateForAll").invoke(npc);
                } catch (Throwable t) {
                    plugin.getAPILogger().fine("[TopSupporters] FancyNPCs skin update for " + names[i] + ": " + t.getMessage());
                }
            }
        } catch (Throwable t) {
            plugin.getAPILogger().fine("[TopSupporters] FancyNPCs refresh skipped: " + t.getMessage());
        }
    }
}
