package com.galaxyrealms.api;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;

/**
 * One-time clear: if CLEAR_PHYSICAL_KEYS_NEXT_START exists in plugin data folder,
 * remove PhoenixCrates key items from all online players' inventories (no command needed).
 */
public final class ClearKeysOnceTask implements Runnable {

    private static final String TRIGGER_FILE = "CLEAR_PHYSICAL_KEYS_NEXT_START";

    private final GalaxyRealmsAPI plugin;

    public ClearKeysOnceTask(GalaxyRealmsAPI plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        File trigger = new File(plugin.getDataFolder(), TRIGGER_FILE);
        if (!trigger.exists()) return;
        int cleared = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            cleared += clearKeyItemsFrom(p);
        }
        if (trigger.delete()) {
            plugin.getAPILogger().info("One-time key clear: removed crate key items from " + cleared + " slots (all online players). Trigger file removed.");
        }
    }

    private int clearKeyItemsFrom(Player player) {
        int n = 0;
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack == null || stack.getType() == Material.AIR) continue;
            ItemMeta meta = stack.getItemMeta();
            if (meta == null || !meta.hasDisplayName()) continue;
            String name = meta.getDisplayName();
            if (name != null && (name.contains("Crate Key") || name.contains("Crates Key"))) {
                player.getInventory().setItem(i, null);
                n += stack.getAmount();
            }
        }
        player.saveData();
        return n;
    }
}
