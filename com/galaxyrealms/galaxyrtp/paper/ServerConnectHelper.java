package com.galaxyrealms.galaxyrtp.paper;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Unified proxy connection logic. All cross-server movement goes through connectToServer.
 * Validates server name, closes inventory before connect, and enforces reconnect cooldown.
 */
public final class ServerConnectHelper {
    private static final String BUNGEE_CHANNEL = "BungeeCord";
    private static final long RECONNECT_COOLDOWN_MS = 1000;

    private final RtpPaperPlugin plugin;
    private final Map<UUID, Long> lastConnectTime = new ConcurrentHashMap<>();

    public ServerConnectHelper(RtpPaperPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Connects the player to the target server via BungeeCord/Velocity.
     *
     * @param player     the player to connect
     * @param serverName the Velocity server name (must match velocity.toml)
     * @return true if connect was sent, false if validation failed or cooldown
     */
    public boolean connectToServer(Player player, String serverName) {
        if (player == null || !player.isOnline()) {
            return false;
        }
        if (serverName == null || serverName.isEmpty()) {
            this.plugin.getLogger().warning("ServerConnectHelper: server name is null or empty");
            return false;
        }
        if (!this.plugin.getPaperConfig().isValidConnectTarget(serverName)) {
            this.plugin.getLogger().warning("ServerConnectHelper: unknown server '" + serverName + "'");
            return false;
        }
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = this.lastConnectTime.get(uuid);
        if (last != null && (now - last) < RECONNECT_COOLDOWN_MS) {
            this.plugin.getLogger().info("ServerConnectHelper: cooldown active for " + player.getName() + ", skipping reconnect");
            return false;
        }
        player.closeInventory();
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);
            out.writeUTF("Connect");
            out.writeUTF(serverName);
            player.sendPluginMessage((Plugin) this.plugin, BUNGEE_CHANNEL, baos.toByteArray());
            this.lastConnectTime.put(uuid, now);
            return true;
        } catch (IOException e) {
            this.plugin.getLogger().warning("ServerConnectHelper: " + e.getMessage());
            return false;
        }
    }
}
