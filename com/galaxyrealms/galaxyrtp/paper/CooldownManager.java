/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 */
package com.galaxyrealms.galaxyrtp.paper;

import com.galaxyrealms.galaxyrtp.paper.RtpPaperPlugin;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Player;

public final class CooldownManager {
    private final RtpPaperPlugin plugin;
    private final Map<UUID, Long> lastRtp = new HashMap<UUID, Long>();

    public CooldownManager(RtpPaperPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isOnCooldown(Player player) {
        if (player.hasPermission("rtp.bypasscooldown")) {
            return false;
        }
        Long last = this.lastRtp.get(player.getUniqueId());
        if (last == null) {
            return false;
        }
        int seconds = player.hasPermission("rtp.fastcooldown") ? this.plugin.getPaperConfig().getCooldownFastSeconds() : this.plugin.getPaperConfig().getCooldownDefaultSeconds();
        return (System.currentTimeMillis() - last) / 1000L < (long)seconds;
    }

    public long getRemainingSeconds(Player player) {
        Long last = this.lastRtp.get(player.getUniqueId());
        if (last == null) {
            return 0L;
        }
        int seconds = player.hasPermission("rtp.fastcooldown") ? this.plugin.getPaperConfig().getCooldownFastSeconds() : this.plugin.getPaperConfig().getCooldownDefaultSeconds();
        long elapsed = (System.currentTimeMillis() - last) / 1000L;
        return Math.max(0L, (long)seconds - elapsed);
    }

    public void setLastRtp(Player player) {
        this.lastRtp.put(player.getUniqueId(), System.currentTimeMillis());
    }
}

