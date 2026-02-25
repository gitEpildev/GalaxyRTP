/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.World
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 */
package com.galaxyrealms.galaxyrtp.paper;

import com.galaxyrealms.galaxyrtp.paper.DebugLog;
import com.galaxyrealms.galaxyrtp.paper.MessageHelper;
import com.galaxyrealms.galaxyrtp.paper.RtpPaperPlugin;
import com.galaxyrealms.galaxyrtp.paper.SafeRtpService;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class ForceRtpCommand
implements CommandExecutor {
    private final RtpPaperPlugin plugin;

    public ForceRtpCommand(RtpPaperPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String worldName;
        Player target;
        DebugLog.log(this.plugin, "E", "ForceRtpCommand.java:onCommand:entry", "forcertp received", Map.of("argsLength", args.length, "args0", args.length > 0 ? args[0] : "", "args1", args.length > 1 ? args[1] : ""));
        if (args.length == 1 && sender instanceof Player) {
            target = (Player)sender;
            worldName = args[0];
        } else if (args.length >= 2) {
            target = Bukkit.getPlayerExact((String)args[0]);
            if (target == null || !target.isOnline()) {
                sender.sendMessage(MessageHelper.format("rtpconnect-player-offline", this.plugin));
                return true;
            }
            worldName = args[1];
        } else {
            sender.sendMessage(String.valueOf(ChatColor.RED) + "Usage: /forcertp <world_name> or /forcertp <player> <world_name>");
            return true;
        }
        World w = Bukkit.getWorld((String)worldName);
        if (w == null) {
            w = Bukkit.getWorld((String)"world");
        }
        if (w == null && !Bukkit.getWorlds().isEmpty()) {
            w = (World)Bukkit.getWorlds().get(0);
        }
        if (w == null) {
            w = target.getWorld();
        }
        SafeRtpService.runRtp(this.plugin, target, w.getName());
        return true;
    }
}

