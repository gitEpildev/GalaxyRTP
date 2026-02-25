package com.galaxyrealms.api;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * /checkkeys [player] - Shows physical + virtual key counts (what the scoreboard uses).
 * Use to verify if a player has keys and why they can/can't open crates.
 */
public class CheckKeysCommand implements CommandExecutor {

    private static final String[] KEY_IDS = { "Op", "Cosmic", "Galaxy", "Nebula", "Supernove", "Vote_Crate" };
    private static final String[] KEY_LABELS = { "Op Crate", "Cosmic Crate", "Galaxy Crate", "Nebula", "Supernova Crate", "Vote Crate" };
    private static final String VIRTUAL_SUFFIX = "_Virtual";

    private final GalaxyRealmsAPI plugin;

    public CheckKeysCommand(GalaxyRealmsAPI plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!Bukkit.getPluginManager().isPluginEnabled("PhoenixCrates") || !Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            sender.sendMessage("§cPhoenixCrates and PlaceholderAPI are required.");
            return true;
        }
        String targetName = args.length > 0 ? args[0] : (sender instanceof Player ? sender.getName() : null);
        if (targetName == null || targetName.isEmpty()) {
            sender.sendMessage("§eUsage: /checkkeys <player>");
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage("§cPlayer §f" + targetName + "§c not found.");
            return true;
        }

        sender.sendMessage("§d§lKeys for §f" + target.getName() + "§d (what scoreboard uses):");
        int totalKeys = 0;
        for (int i = 0; i < KEY_IDS.length; i++) {
            String physId = KEY_IDS[i];
            String virtId = physId + VIRTUAL_SUFFIX;
            int phys = parseInt(PlaceholderAPI.setPlaceholders(target, "%phoenixcrates_physicalkeys_" + physId + "%"));
            int virt = parseInt(PlaceholderAPI.setPlaceholders(target, "%phoenixcrates_virtualkeys_" + virtId + "%"));
            int combined = phys + virt;
            if (combined > 0) {
                sender.sendMessage("§7  " + KEY_LABELS[i] + " §8» §fPhysical: §e" + phys + " §7| §fVirtual: §b" + virt + " §7| §aTotal: " + combined);
                totalKeys += combined;
            }
        }
        if (totalKeys == 0) {
            sender.sendMessage("§7  §cNo keys (physical or virtual). Scoreboard should hide keys section.");
        } else {
            sender.sendMessage("§7  §aTotal keys: §f" + totalKeys);
        }
        sender.sendMessage("§8To open with virtual keys: right-click the crate with §oempty hand§8 (no key in hand).");
        return true;
    }

    private static int parseInt(String s) {
        if (s == null) return 0;
        try {
            return Math.max(0, Integer.parseInt(s.replaceAll("[^0-9-]", "")));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
