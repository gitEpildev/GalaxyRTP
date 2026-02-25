/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.messaging.PluginMessageListener
 */
package com.galaxyrealms.galaxyrtp.paper;

import com.galaxyrealms.galaxyrtp.common.MessageProtocol;
import com.galaxyrealms.galaxyrtp.paper.PendingRtpListener;
import com.galaxyrealms.galaxyrtp.paper.RtpPaperPlugin;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

public final class RtpMessageListener
implements PluginMessageListener {
    private final RtpPaperPlugin plugin;

    public RtpMessageListener(RtpPaperPlugin plugin) {
        this.plugin = plugin;
    }

    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("galaxyrtp:main")) {
            return;
        }
        try {
            String[] decoded = MessageProtocol.decodeRtpPending(message);
            UUID uuid = UUID.fromString(decoded[0]);
            String worldName = decoded[1];
            PendingRtpListener.addPending(this.plugin, uuid, worldName);
            Player p = Bukkit.getPlayer((UUID)uuid);
            if (p != null && p.isOnline()) {
                Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> PendingRtpListener.consumePendingAndRtp(this.plugin, p));
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
    }
}

