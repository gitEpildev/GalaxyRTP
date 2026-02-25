/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerJoinEvent
 *  org.bukkit.plugin.Plugin
 */
package com.galaxyrealms.galaxyrtp.paper;

import com.galaxyrealms.galaxyrtp.paper.RtpPaperPlugin;
import com.galaxyrealms.galaxyrtp.paper.SafeRtpService;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

public final class PendingRtpListener
implements Listener {
    private static final Map<UUID, String> PENDING = new ConcurrentHashMap<UUID, String>();
    private final RtpPaperPlugin plugin;

    public PendingRtpListener(RtpPaperPlugin plugin) {
        this.plugin = plugin;
    }

    static void addPending(RtpPaperPlugin plugin, UUID uuid, String worldName) {
        PENDING.put(uuid, worldName);
    }

    static void consumePendingAndRtp(RtpPaperPlugin plugin, Player player) {
        String worldName = PENDING.remove(player.getUniqueId());
        if (worldName == null) {
            return;
        }
        plugin.getPaperConfig().getRtpMaxAttempts();
        SafeRtpService.runRtp(plugin, player, worldName);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> PendingRtpListener.consumePendingAndRtp(this.plugin, player), 20L);
    }
}

