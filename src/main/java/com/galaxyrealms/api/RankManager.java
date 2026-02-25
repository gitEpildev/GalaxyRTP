package com.galaxyrealms.api;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class RankManager {
    
    public static boolean grantRankSync(GalaxyRealmsAPI plugin, String identifier, String rank, Player player) {
        try {
            LuckPerms luckPerms = plugin.getLuckPerms();
            UUID uuid;
            
            if (player != null) {
                uuid = player.getUniqueId();
            } else {
                // Try to get UUID from username
                Player offlinePlayer = Bukkit.getPlayer(identifier);
                if (offlinePlayer != null) {
                    uuid = offlinePlayer.getUniqueId();
                } else {
                    // Use offline UUID (not recommended, but works)
                    uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + identifier).getBytes());
                }
            }
            
            // Load user asynchronously but wait for result
            User user = luckPerms.getUserManager().loadUser(uuid).join();
            
            if (user == null) {
                plugin.getAPILogger().warning("Could not load user: " + identifier);
                return false;
            }
            
            // Remove old ranks if configured
            if (plugin.getConfigManager().shouldRemoveOldRanks()) {
                // Get current groups and remove them
                user.getNodes().stream()
                    .filter(node -> node instanceof InheritanceNode)
                    .forEach(node -> {
                        InheritanceNode inheritanceNode = (InheritanceNode) node;
                        user.data().remove(inheritanceNode);
                    });
            }
            
            // Add new rank
            InheritanceNode node = InheritanceNode.builder(rank.toLowerCase()).build();
            user.data().add(node);
            
            // Save user
            luckPerms.getUserManager().saveUser(user);
            
            // Apply changes and send thank you message if player is online
            Player onlinePlayer = player != null ? player : Bukkit.getPlayer(identifier);
            if (onlinePlayer != null && onlinePlayer.isOnline()) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    luckPerms.getUserManager().loadUser(uuid).thenAccept(loadedUser -> {
                        if (loadedUser != null) {
                            luckPerms.getContextManager().signalContextUpdate(loadedUser);
                        }
                    });
                    // Send thank you message
                    sendPurchaseThankYou(onlinePlayer, "rank", rank, 1);
                });
            }
            
            plugin.getAPILogger().info("Granted rank " + rank + " to " + identifier);
            return true;
            
        } catch (Exception e) {
            plugin.getAPILogger().severe("Error granting rank: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    public static CompletableFuture<Boolean> grantRank(GalaxyRealmsAPI plugin, String identifier, String rank, Player player) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean result = grantRankSync(plugin, identifier, rank, player);
            future.complete(result);
        });
        return future;
    }
    
    /**
     * Send a thank you message to a player after a purchase
     */
    public static void sendPurchaseThankYou(Player player, String productType, String productName, int quantity) {
        if (player == null || !player.isOnline()) return;
        
        String border = ChatColor.DARK_PURPLE + "" + ChatColor.STRIKETHROUGH + "                                        ";
        String prefix = ChatColor.LIGHT_PURPLE + "✦ " + ChatColor.GOLD;
        
        player.sendMessage("");
        player.sendMessage(border);
        player.sendMessage("");
        player.sendMessage(prefix + ChatColor.BOLD + "Thank you for your purchase!");
        player.sendMessage("");
        
        if ("rank".equalsIgnoreCase(productType)) {
            String formattedRank = productName.substring(0, 1).toUpperCase() + productName.substring(1).toLowerCase();
            player.sendMessage(ChatColor.WHITE + "  You have received the " + ChatColor.GOLD + formattedRank + ChatColor.WHITE + " rank!");
            player.sendMessage(ChatColor.GRAY + "  Your new perks are now active.");
        } else if ("crate".equalsIgnoreCase(productType)) {
            player.sendMessage(ChatColor.WHITE + "  You have received " + ChatColor.GOLD + quantity + "x " + productName + " Key" + (quantity > 1 ? "s" : "") + ChatColor.WHITE + "!");
            player.sendMessage(ChatColor.GRAY + "  Use /crate to open your crates.");
        } else if ("vault".equalsIgnoreCase(productType)) {
            player.sendMessage(ChatColor.WHITE + "  You have unlocked " + ChatColor.GOLD + "Vault #" + productName + ChatColor.WHITE + "!");
            player.sendMessage(ChatColor.GRAY + "  Use /vault to access your storage.");
        } else {
            player.sendMessage(ChatColor.WHITE + "  Your purchase has been delivered!");
        }
        
        player.sendMessage("");
        player.sendMessage(ChatColor.YELLOW + "  Thank you for supporting Galaxy Realms!");
        player.sendMessage("");
        player.sendMessage(border);
        player.sendMessage("");
    }
}
