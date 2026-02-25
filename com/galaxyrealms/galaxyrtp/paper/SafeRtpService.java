/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.World
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 */
package com.galaxyrealms.galaxyrtp.paper;

import com.galaxyrealms.galaxyrtp.paper.MessageHelper;
import com.galaxyrealms.galaxyrtp.paper.RtpPaperPlugin;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class SafeRtpService {
    private static final Random RANDOM = ThreadLocalRandom.current();

    private SafeRtpService() {
    }

    public static void runRtp(RtpPaperPlugin plugin, Player player, String worldName) {
        if (!player.hasPermission("rtp.use")) {
            return;
        }
        if (plugin.getCooldownManager().isOnCooldown(player)) {
            long sec = plugin.getCooldownManager().getRemainingSeconds(player);
            player.sendMessage(MessageHelper.format("rtp-cooldown", plugin, Map.of("time", sec + "s")));
            return;
        }
        World world = Bukkit.getWorld((String)worldName);
        if (world == null) {
            world = Bukkit.getWorld((String)"world");
        }
        if (world == null && !Bukkit.getWorlds().isEmpty()) {
            world = (World)Bukkit.getWorlds().get(0);
        }
        if (world == null) {
            world = player.getWorld();
        }
        World targetWorld = world;
        int radius = Math.max(500, plugin.getRadiusResolver().getRadius(player));
        Bukkit.getScheduler().runTask((Plugin)plugin, () -> {
            try {
                Location loc = SafeRtpService.pickRtpLocation(targetWorld, radius);
                player.teleport(loc);
                plugin.getCooldownManager().setLastRtp(player);
                player.sendMessage(MessageHelper.format("rtp-success", plugin));
            }
            catch (Throwable t) {
                try {
                    player.teleport(targetWorld.getSpawnLocation());
                    plugin.getCooldownManager().setLastRtp(player);
                    player.sendMessage(MessageHelper.format("rtp-success", plugin));
                }
                catch (Throwable t2) {
                    try {
                        player.teleport(player.getWorld().getSpawnLocation());
                        plugin.getCooldownManager().setLastRtp(player);
                        player.sendMessage(MessageHelper.format("rtp-success", plugin));
                    }
                    catch (Throwable throwable) {
                        // empty catch block
                    }
                }
            }
        });
    }

    private static Location pickRtpLocation(World world, int radius) {
        int surfaceY;
        int x = RANDOM.nextInt(2 * radius + 1) - radius;
        int z = RANDOM.nextInt(2 * radius + 1) - radius;
        try {
            world.getChunkAt(x >> 4, z >> 4);
        }
        catch (Throwable t) {
            return SafeRtpService.fallbackSpawn(world);
        }
        try {
            surfaceY = world.getHighestBlockYAt(x, z);
        }
        catch (Throwable t) {
            return SafeRtpService.fallbackSpawn(world);
        }
        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight() - 1;
        if (surfaceY < minY || surfaceY >= maxY) {
            return SafeRtpService.fallbackSpawn(world);
        }
        return new Location(world, (double)x + 0.5, (double)(surfaceY + 1), (double)z + 0.5, 0.0f, 0.0f);
    }

    private static Location fallbackSpawn(World world) {
        Location spawn = world.getSpawnLocation();
        int dx = RANDOM.nextInt(33) - 16;
        int dz = RANDOM.nextInt(33) - 16;
        Location loc = spawn.clone().add((double)dx, 0.0, (double)dz);
        try {
            int top = world.getHighestBlockYAt(loc.getBlockX(), loc.getBlockZ());
            if (top >= world.getMinHeight() && top < world.getMaxHeight()) {
                loc.setY((double)(top + 1));
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return loc;
    }
}

