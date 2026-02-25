/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 */
package com.galaxyrealms.galaxyrtp.paper;

import com.galaxyrealms.galaxyrtp.paper.PaperConfig;
import com.galaxyrealms.galaxyrtp.paper.RtpPaperPlugin;
import java.util.UUID;
import org.bukkit.entity.Player;

public final class RtpRadiusResolver {
    private final RtpPaperPlugin plugin;

    public RtpRadiusResolver(RtpPaperPlugin plugin) {
        this.plugin = plugin;
    }

    public int getRadius(Player player) {
        String serverName = this.plugin.getPaperConfig().getThisServerName();
        PaperConfig.ServerRadiusConfig src = this.plugin.getPaperConfig().getServers().get(serverName);
        if (src == null) {
            return 2000;
        }
        String group = this.resolveGroup(player);
        if (group != null && src.groupRadiusOverrides != null && src.groupRadiusOverrides.containsKey(group)) {
            return src.groupRadiusOverrides.get(group);
        }
        return src.defaultRadius;
    }

    private String resolveGroup(Player player) {
        try {
            Class<?> apiClass = Class.forName("net.luckperms.api.LuckPermsProvider");
            Object provider = apiClass.getMethod("get", new Class[0]).invoke(null, new Object[0]);
            Object userManager = provider.getClass().getMethod("getUserManager", new Class[0]).invoke(provider, new Object[0]);
            Object user = userManager.getClass().getMethod("getUser", UUID.class).invoke(userManager, player.getUniqueId());
            if (user == null) {
                return null;
            }
            Object primaryGroup = user.getClass().getMethod("getPrimaryGroup", new Class[0]).invoke(user, new Object[0]);
            return primaryGroup != null ? primaryGroup.toString() : null;
        }
        catch (Throwable ignored) {
            return null;
        }
    }
}

