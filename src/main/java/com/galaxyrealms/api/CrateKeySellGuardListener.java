package com.galaxyrealms.api;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Prevents players from selling crate keys via EconomyShopGUI Sell GUI.
 * Cancels moving crate keys into the sell inventory (top inventory when title contains "SellGUI").
 */
public class CrateKeySellGuardListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        String title = ChatColor.stripColor(event.getView().getTitle());
        if (!title.contains("SellGUI") && !title.contains("Sell Gui")) return;

        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();

        // Block putting crate key from cursor into any slot (including sell area)
        if (cursor != null && !cursor.getType().isAir() && CrateKeyGuard.isCrateKey(cursor)) {
            event.setCancelled(true);
            ((Player) event.getWhoClicked()).sendMessage(ChatColor.RED + "You cannot sell crate keys.");
            return;
        }

        // Block shift-clicking crate key from player inv into sell area (top inventory)
        if (event.isShiftClick() && current != null && !current.getType().isAir()
                && CrateKeyGuard.isCrateKey(current)) {
            event.setCancelled(true);
            ((Player) event.getWhoClicked()).sendMessage(ChatColor.RED + "You cannot sell crate keys.");
        }
    }
}
