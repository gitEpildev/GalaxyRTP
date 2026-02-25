package com.galaxyrealms.api;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;

/**
 * Re-applies crate key and crate display names on player join so that after relog
 * the item names stay correct (fixes names changing to "Paper" etc. when inventory
 * is loaded from disk).
 * Supports: feather CMD 4550-4561 (Nexo/ItemsAdder simple_crates), paper 1001-1006, tripwire 2001-2006.
 * Only renames items that match the expected material for each CMD range, so nexo/ItemsAdder
 * custom items (e.g. chainmail with CMD 1001) are not mistaken for crate keys.
 */
public class CrateKeyNameFixListener implements Listener {

    /** CMD -> display name, only for items that are actually keys/crates by material */
    private static final Map<Integer, String> CMD_TO_DISPLAY_NAME = new HashMap<>();
    /** CMD -> allowed material; if null, any material (legacy). */
    private static final Map<Integer, Material> CMD_TO_MATERIAL = new HashMap<>();

    static {
        // Feather (Nexo/ItemsAdder simple_crates) - keys 4551,4553,4555,4557,4559,4561; crates 4550,4552,4554,4556,4558,4560
        CMD_TO_DISPLAY_NAME.put(4561, "Op Crate Key");
        CMD_TO_DISPLAY_NAME.put(4557, "Cosmic Crate Key");
        CMD_TO_DISPLAY_NAME.put(4551, "Galaxy Crate Key");
        CMD_TO_DISPLAY_NAME.put(4555, "Nebula Crate Key");
        CMD_TO_DISPLAY_NAME.put(4559, "Supernova Crate Key");
        CMD_TO_DISPLAY_NAME.put(4553, "Vote Crate Key");
        CMD_TO_DISPLAY_NAME.put(4560, "Op Crate");
        CMD_TO_DISPLAY_NAME.put(4556, "Cosmic Crate");
        CMD_TO_DISPLAY_NAME.put(4550, "Galaxy Crate");
        CMD_TO_DISPLAY_NAME.put(4554, "Nebula Crate");
        CMD_TO_DISPLAY_NAME.put(4558, "Supernova Crate");
        CMD_TO_DISPLAY_NAME.put(4552, "Vote Crate");
        for (int cmd = 4550; cmd <= 4561; cmd++) {
            CMD_TO_MATERIAL.put(cmd, Material.FEATHER);
        }
        // Paper keys 1001-1006
        CMD_TO_DISPLAY_NAME.put(1001, "Op Crate Key");
        CMD_TO_DISPLAY_NAME.put(1002, "Cosmic Crate Key");
        CMD_TO_DISPLAY_NAME.put(1003, "Galaxy Crate Key");
        CMD_TO_DISPLAY_NAME.put(1004, "Nebula Crate Key");
        CMD_TO_DISPLAY_NAME.put(1005, "Supernova Crate Key");
        CMD_TO_DISPLAY_NAME.put(1006, "Vote Crate Key");
        for (int cmd = 1001; cmd <= 1006; cmd++) {
            CMD_TO_MATERIAL.put(cmd, Material.PAPER);
        }
        // Tripwire crates 2001-2006
        CMD_TO_DISPLAY_NAME.put(2001, "Op Crate");
        CMD_TO_DISPLAY_NAME.put(2002, "Cosmic Crate");
        CMD_TO_DISPLAY_NAME.put(2003, "Galaxy Crate");
        CMD_TO_DISPLAY_NAME.put(2004, "Nebula Crate");
        CMD_TO_DISPLAY_NAME.put(2005, "Supernova Crate");
        CMD_TO_DISPLAY_NAME.put(2006, "Vote Crate");
        for (int cmd = 2001; cmd <= 2006; cmd++) {
            CMD_TO_MATERIAL.put(cmd, Material.TRIPWIRE_HOOK);
        }
    }

    private final GalaxyRealmsAPI plugin;

    public CrateKeyNameFixListener(GalaxyRealmsAPI plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Run after a short delay so inventory is fully loaded from disk
        Bukkit.getScheduler().runTaskLater(plugin, () -> fixKeyNames(event.getPlayer()), 20L);
    }

    /**
     * Re-apply display names to crate keys/crates in the player's inventory by CustomModelData.
     * Call this after granting keys (delayed) or on join so names persist after relog.
     */
    public static void fixKeyNames(Player player) {
        if (player == null || !player.isOnline()) return;

        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || !item.hasItemMeta()) continue;
            ItemMeta meta = item.getItemMeta();
            if (meta == null || !meta.hasCustomModelData()) continue;

            // Never touch nexo or ItemsAdder custom items - they have their own display names.
            if (isNexoOrItemsAdderItem(meta)) continue;

            int cmd = meta.getCustomModelData();
            String correctName = CMD_TO_DISPLAY_NAME.get(cmd);
            if (correctName == null) continue;

            // Only rename if this item is actually a crate key/crate (correct material).
            Material required = CMD_TO_MATERIAL.get(cmd);
            if (required != null && item.getType() != required) continue;

            String current = meta.getDisplayName();
            String withReset = ChatColor.RESET + correctName;
            if (current == null || current.isEmpty() || !current.equals(withReset)) {
                meta.setDisplayName(withReset);
                item.setItemMeta(meta);
            }
        }
    }

    /**
     * Returns true if the item has nexo or ItemsAdder custom data (PersistentDataContainer).
     * We never overwrite display names for these items.
     */
    private static boolean isNexoOrItemsAdderItem(ItemMeta meta) {
        if (meta == null) return false;
        for (NamespacedKey key : meta.getPersistentDataContainer().getKeys()) {
            String ns = key.getNamespace();
            if ("nexo".equals(ns) || "itemsadder".equals(ns)) return true;
        }
        return false;
    }
}

