package com.galaxyrealms.api;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;

/**
 * Prevents players from dropping crate keys.
 */
public class CrateKeyDropListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (CrateKeyGuard.isCrateKey(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
            Player p = event.getPlayer();
            p.sendMessage(ChatColor.RED + "You cannot drop crate keys.");
        }
    }
}
