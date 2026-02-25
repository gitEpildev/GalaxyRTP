package com.galaxyrealms.api;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ItemManager {
    
    public static boolean grantItem(GalaxyRealmsAPI plugin, Player player, String item, String kit) {
        try {
            if (kit != null && !kit.isEmpty()) {
                // Try to give kit using PlayerKits2 or Essentials
                boolean success = false;
                
                // Try PlayerKits2
                if (Bukkit.getPluginManager().getPlugin("PlayerKits2") != null) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "kit give " + kit + " " + player.getName());
                    success = true;
                }
                // Try Essentials
                else if (Bukkit.getPluginManager().getPlugin("Essentials") != null) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "kit " + kit + " " + player.getName());
                    success = true;
                }
                
                if (success) {
                    plugin.getAPILogger().info("Granted kit " + kit + " to " + player.getName());
                    return true;
                }
            }
            
            if (item != null && !item.isEmpty()) {
                // Parse item string (format: "DIAMOND_SWORD:1" or "minecraft:diamond_sword")
                try {
                    ItemStack itemStack = parseItem(item);
                    if (itemStack != null) {
                        player.getInventory().addItem(itemStack);
                        plugin.getAPILogger().info("Granted item " + item + " to " + player.getName());
                        return true;
                    }
                } catch (Exception e) {
                    plugin.getAPILogger().warning("Could not parse item: " + item);
                }
            }
            
            return false;
        } catch (Exception e) {
            plugin.getAPILogger().severe("Error granting item: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private static ItemStack parseItem(String itemString) {
        // Simple item parser - can be enhanced
        // Format: "MATERIAL" or "MATERIAL:AMOUNT" or "MATERIAL:AMOUNT:DATA"
        String[] parts = itemString.split(":");
        String materialName = parts[0].toUpperCase();
        
        try {
            org.bukkit.Material material = org.bukkit.Material.valueOf(materialName);
            int amount = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
            
            return new ItemStack(material, amount);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
