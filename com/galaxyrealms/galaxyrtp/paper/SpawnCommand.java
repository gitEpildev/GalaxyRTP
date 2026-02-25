package com.galaxyrealms.galaxyrtp.paper;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class SpawnCommand implements CommandExecutor {
    private final RtpPaperPlugin plugin;

    public SpawnCommand(RtpPaperPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageHelper.format("spawn-no-permission", this.plugin));
            return true;
        }
        Player player = (Player) sender;
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
                world = this.plugin.getServer().getWorlds().get(0);
            }
            if (world == null) {
                world = player.getWorld();
            }
            Location loc = new Location(world, spawn.x, spawn.y, spawn.z, spawn.yaw, spawn.pitch);
            player.teleport(loc);
            player.sendMessage(MessageHelper.format("spawn-success", this.plugin));
            return true;
        }
        String hubServer = this.plugin.getPaperConfig().getConnectNameForHub();
        if (this.plugin.getServerConnectHelper().connectToServer(player, hubServer)) {
            player.sendMessage(MessageHelper.format("spawn-connecting", this.plugin));
        } else {
            player.sendMessage(MessageHelper.format("rtpconnect-invalid-server", this.plugin));
        }
        return true;
    }
}

