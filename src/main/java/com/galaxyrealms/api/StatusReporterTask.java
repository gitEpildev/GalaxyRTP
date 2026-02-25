package com.galaxyrealms.api;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Periodically reports server status (online/max players) to the store backend
 * so the dashboard can show player count without RCON.
 */
public class StatusReporterTask extends BukkitRunnable {

    private static final int INTERVAL_TICKS = 600; // 30 seconds
    private static final int TIMEOUT_MS = 5000;

    private final GalaxyRealmsAPI plugin;

    public StatusReporterTask(GalaxyRealmsAPI plugin) {
        this.plugin = plugin;
    }

    public void start() {
        runTaskTimerAsynchronously(plugin, INTERVAL_TICKS, INTERVAL_TICKS);
        plugin.getAPILogger().info("Status reporter started (reporting player count to store every 30s)");
    }

    @Override
    public void run() {
        String backendUrl = plugin.getConfigManager().getBackendUrl();
        String apiKey = plugin.getConfigManager().getBackendApiKey();
        if (backendUrl == null || backendUrl.isEmpty() || apiKey == null || apiKey.isEmpty()) {
            return;
        }
        String urlStr = backendUrl.replaceAll("/$", "") + "/minecraft/status-report";
        int online = Bukkit.getOnlinePlayers().size();
        int max = Bukkit.getMaxPlayers();
        String json = "{\"online_players\":" + online + ",\"max_players\":" + max + "}";

        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setRequestProperty("X-API-Key", apiKey);
            conn.setDoOutput(true);
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = json.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                // Success - no log spam
            } else {
                plugin.getAPILogger().warning("Status report failed: HTTP " + code);
            }
        } catch (Exception e) {
            plugin.getAPILogger().fine("Status report failed: " + e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
