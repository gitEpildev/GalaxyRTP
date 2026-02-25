package com.galaxyrealms.api;

import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Set;

/**
 * Utility to detect crate key items so we can block selling and dropping them.
 * Uses CustomModelData and display name (e.g. "Cosmic Crate Key") so keys are never shown as sellable.
 */
public final class CrateKeyGuard {

    /** CustomModelData values that identify crate keys (feather 455x, paper 100x). */
    private static final Set<Integer> CRATE_KEY_CMDS = Set.of(
        4561, 4557, 4551, 4555, 4559, 4553,  // Feather (Nexo/ItemsAdder simple_crates)
        1001, 1002, 1003, 1004, 1005, 1006   // Paper keys
    );

    private static final String CRATE_KEY_PHRASE = "crate key";

    private CrateKeyGuard() {}

    /**
     * Returns true if the item is a crate key (by CustomModelData or display name).
     */
    public static boolean isCrateKey(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) return false;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return false;
        if (meta.hasCustomModelData() && CRATE_KEY_CMDS.contains(meta.getCustomModelData())) return true;
        if (meta.hasDisplayName() && ChatColor.stripColor(meta.getDisplayName()).toLowerCase().contains(CRATE_KEY_PHRASE)) return true;
        return false;
    }
}
