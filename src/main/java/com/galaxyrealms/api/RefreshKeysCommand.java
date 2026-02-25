package com.galaxyrealms.api;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /galaxyrealms refreshkeys [player] - Force TAB scoreboard to refresh for a player
 * after they receive a physical crate key so the key count updates immediately.
 * Call this (e.g. from AxAFKZone) right after giving a key.
 */
public class RefreshKeysCommand implements CommandExecutor {

    private final GalaxyRealmsAPI plugin;

    public RefreshKeysCommand(GalaxyRealmsAPI plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player target;
        if (args.length >= 1 && !args[0].isEmpty()) {
            target = Bukkit.getPlayerExact(args[0]);
            if (target == null || !target.isOnline()) {
                sender.sendMessage("§cPlayer not found or not online: " + args[0]);
                return true;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage("§cUsage: /galaxyrealms refreshkeys <player>");
            return true;
        }
        // Delay so PhoenixCrates has time to add the key and caches update before TAB re-requests placeholders
        Bukkit.getScheduler().runTaskLater(plugin, () -> forceScoreboardRefresh(target), 20L);
        return true;
    }

    /**
     * Force TAB to refresh the scoreboard for this player so key placeholders update
     * (e.g. after receiving a physical key from AFK zone).
     */
    public static void forceScoreboardRefresh(Player player) {
        if (player == null || !player.isOnline()) return;
        try {
            Class<?> tabAPI = Class.forName("me.neznamy.tab.api.TabAPI");
            Object api = tabAPI.getMethod("getInstance").invoke(null);
            Object tabPlayer = tabAPI.getMethod("getPlayer", java.util.UUID.class).invoke(api, player.getUniqueId());
            if (tabPlayer == null) return;
            // TabPlayer.forceRefresh() - force placeholders/scoreboard to update
            try {
                tabPlayer.getClass().getMethod("forceRefresh").invoke(tabPlayer);
            } catch (NoSuchMethodException e) {
                // Fallback: reset scoreboard so TAB re-sends it with fresh placeholders
                Object scoreboardMgr = api.getClass().getMethod("getScoreboardManager").invoke(api);
                if (scoreboardMgr != null) {
                    scoreboardMgr.getClass().getMethod("resetScoreboard", tabPlayer.getClass()).invoke(scoreboardMgr, tabPlayer);
                }
            }
        } catch (Throwable ignored) {
            // TAB not present or API changed - no-op
        }
    }
}
