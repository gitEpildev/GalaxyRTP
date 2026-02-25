/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  org.bukkit.Bukkit
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 */
package com.galaxyrealms.galaxyrtp.paper;

import com.galaxyrealms.galaxyrtp.paper.MessageHelper;
import com.galaxyrealms.galaxyrtp.paper.RtpPaperPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class RtpCommand
implements CommandExecutor {
    private final RtpPaperPlugin plugin;

    public RtpCommand(RtpPaperPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage((Component) Component.text("Only players can use /rtp."));
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("rtp.use")) {
            player.sendMessage(MessageHelper.format("rtp-no-permission", this.plugin));
            return true;
        }
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "dm open rtp_region_menu " + player.getName());
        return true;
    }
}

