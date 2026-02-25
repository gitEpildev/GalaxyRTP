/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.event.Listener
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 *  org.bukkit.plugin.messaging.PluginMessageListener
 */
package com.galaxyrealms.galaxyrtp.paper;

import com.galaxyrealms.galaxyrtp.paper.CooldownManager;
import com.galaxyrealms.galaxyrtp.paper.ForceRtpCommand;
import com.galaxyrealms.galaxyrtp.paper.PaperConfig;
import com.galaxyrealms.galaxyrtp.paper.PendingRtpListener;
import com.galaxyrealms.galaxyrtp.paper.RtpCommand;
import com.galaxyrealms.galaxyrtp.paper.RtpConnectCommand;
import com.galaxyrealms.galaxyrtp.paper.RtpMessageListener;
import com.galaxyrealms.galaxyrtp.paper.RtpRadiusResolver;
import com.galaxyrealms.galaxyrtp.paper.SpawnCommand;
import org.bukkit.command.CommandExecutor;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

public final class RtpPaperPlugin
extends JavaPlugin {
    private PaperConfig paperConfig;
    private CooldownManager cooldownManager;
    private RtpRadiusResolver radiusResolver;

    public void onEnable() {
        this.paperConfig = new PaperConfig(this);
        this.cooldownManager = new CooldownManager(this);
        this.radiusResolver = new RtpRadiusResolver(this);
        this.getServer().getMessenger().registerOutgoingPluginChannel((Plugin)this, "BungeeCord");
        this.getServer().getMessenger().registerOutgoingPluginChannel((Plugin)this, "galaxyrtp:main");
        this.getServer().getMessenger().registerIncomingPluginChannel((Plugin)this, "galaxyrtp:main", (PluginMessageListener)new RtpMessageListener(this));
        this.getCommand("rtp").setExecutor((CommandExecutor)new RtpCommand(this));
        this.getCommand("rtpmenu").setExecutor((CommandExecutor)new RtpCommand(this));
        this.getCommand("rtpconnect").setExecutor((CommandExecutor)new RtpConnectCommand(this));
        this.getCommand("forcertp").setExecutor((CommandExecutor)new ForceRtpCommand(this));
        this.getCommand("spawn").setExecutor((CommandExecutor)new SpawnCommand(this));
        this.getServer().getPluginManager().registerEvents((Listener)new PendingRtpListener(this), (Plugin)this);
        this.getLogger().info("GalaxyRTP Paper enabled (server: " + this.paperConfig.getThisServerName() + ")");
    }

    public void onDisable() {
        if (this.getServer().getMessenger() != null) {
            this.getServer().getMessenger().unregisterOutgoingPluginChannel((Plugin)this, "BungeeCord");
            this.getServer().getMessenger().unregisterOutgoingPluginChannel((Plugin)this, "galaxyrtp:main");
            this.getServer().getMessenger().unregisterIncomingPluginChannel((Plugin)this, "galaxyrtp:main");
        }
    }

    public PaperConfig getPaperConfig() {
        return this.paperConfig;
    }

    public CooldownManager getCooldownManager() {
        return this.cooldownManager;
    }

    public RtpRadiusResolver getRadiusResolver() {
        return this.radiusResolver;
    }
}

