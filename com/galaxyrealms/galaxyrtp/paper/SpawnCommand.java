/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.World
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 */
package com.galaxyrealms.galaxyrtp.paper;

import com.galaxyrealms.galaxyrtp.paper.MessageHelper;
import com.galaxyrealms.galaxyrtp.paper.PaperConfig;
import com.galaxyrealms.galaxyrtp.paper.RtpPaperPlugin;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class SpawnCommand
implements CommandExecutor {
    private static final String BUNGEE_CHANNEL = "BungeeCord";
    private final RtpPaperPlugin plugin;

    public SpawnCommand(RtpPaperPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageHelper.format("spawn-no-permission", this.plugin));
            return true;
        }
        Player player = (Player)sender;
        if (!player.hasPermission("spawn.use")) {
            player.sendMessage(MessageHelper.format("spawn-no-permission", this.plugin));
            return true;
        }
        if (this.plugin.getPaperConfig().isHub()) {
            PaperConfig.SpawnConfig spawn = this.plugin.getPaperConfig().getSpawn();
            World world = this.plugin.getServer().getWorld(spawn.worldName);
            if (world == null) {
                world = this.plugin.getServer().getWorld("world");
            }
            if (world == null && !this.plugin.getServer().getWorlds().isEmpty()) {
                world = (World)this.plugin.getServer().getWorlds().get(0);
            }
            if (world == null) {
                world = player.getWorld();
            }
            Location loc = new Location(world, spawn.x, spawn.y, spawn.z, spawn.yaw, spawn.pitch);
            player.teleport(loc);
            player.sendMessage(MessageHelper.format("spawn-success", this.plugin));
            return true;
        }
        try {
            String targetServer = this.plugin.getPaperConfig().getHubServerName();
            if (targetServer == null || targetServer.equalsIgnoreCase("hub")) {
                targetServer = "spawn";
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);
            out.writeUTF("Connect");
            out.writeUTF(targetServer);
            player.sendPluginMessage((Plugin)this.plugin, BUNGEE_CHANNEL, baos.toByteArray());
            player.sendMessage(MessageHelper.format("spawn-connecting", this.plugin));
        }
        catch (IOException e) {
            this.plugin.getLogger().warning("Spawn connect: " + e.getMessage());
        }
        return true;
    }
}

