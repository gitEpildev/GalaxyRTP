/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 */
package com.galaxyrealms.galaxyrtp.paper;

import com.galaxyrealms.galaxyrtp.common.MessageProtocol;
import com.galaxyrealms.galaxyrtp.paper.MessageHelper;
import com.galaxyrealms.galaxyrtp.paper.RtpPaperPlugin;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class RtpConnectCommand
implements CommandExecutor {
    private static final String BUNGEE_CHANNEL = "BungeeCord";
    private final RtpPaperPlugin plugin;

    public RtpConnectCommand(RtpPaperPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player playerSender;
        if (args.length < 3) {
            sender.sendMessage(MessageHelper.format("rtpconnect-usage", this.plugin));
            return true;
        }
        String playerName = args[0];
        String serverName = args[1];
        String worldKey = args[2];
        if (!this.plugin.getPaperConfig().isRtpServer(serverName)) {
            sender.sendMessage(MessageHelper.format("rtpconnect-invalid-server", this.plugin));
            return true;
        }
        String worldName = this.plugin.getPaperConfig().getWorldNameForServer(serverName, worldKey);
        if (worldName == null) {
            sender.sendMessage(MessageHelper.format("rtpconnect-invalid-world", this.plugin));
            return true;
        }
        Player target = Bukkit.getPlayerExact((String)playerName);
        if (target == null || !target.isOnline()) {
            sender.sendMessage(MessageHelper.format("rtpconnect-player-offline", this.plugin));
            return true;
        }
        if (sender instanceof Player && !(playerSender = (Player)sender).getUniqueId().equals(target.getUniqueId())) {
            sender.sendMessage(MessageHelper.format("rtpconnect-player-offline", this.plugin));
            return true;
        }
        if (this.plugin.getCooldownManager().isOnCooldown(target) && !target.hasPermission("rtp.bypasscooldown")) {
            sender.sendMessage(MessageHelper.format("rtpconnect-cooldown", this.plugin));
            return true;
        }
        try {
            byte[] payload = MessageProtocol.encodeConnectRequest(serverName, worldName);
            target.sendPluginMessage((Plugin)this.plugin, "galaxyrtp:main", payload);
        }
        catch (IOException e) {
            this.plugin.getLogger().warning("RtpConnect: " + e.getMessage());
        }
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);
            out.writeUTF("Connect");
            out.writeUTF(serverName);
            target.sendPluginMessage((Plugin)this.plugin, BUNGEE_CHANNEL, baos.toByteArray());
        }
        catch (IOException e) {
            this.plugin.getLogger().warning("RtpConnect BungeeCord: " + e.getMessage());
        }
        return true;
    }
}

