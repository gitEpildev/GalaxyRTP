package com.galaxyrealms.api;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.*;

/**
 * Adds "Sell: $X.XX each" lore to items in player inventory when they have a sell price (EconomyShopGUI).
 * Requires EconomyShopGUI price_tiers.yml - reads sell = 0.25 * buy for materials.
 */
public class SellPriceLoreListener implements Listener {

    private static final String LORE_PREFIX = ChatColor.GRAY + "│ " + ChatColor.GREEN + "Sell: " + ChatColor.DARK_GREEN + "$";
    private static final String LORE_SUFFIX = " each";

    private final Plugin plugin;
    private final Map<Material, Double> sellPrices = new HashMap<>();
    private long lastLoad = 0;
    private static final long RELOAD_INTERVAL_MS = 30_000;

    public SellPriceLoreListener(Plugin plugin) {
        this.plugin = plugin;
        loadPrices();
    }

    private void loadPrices() {
        sellPrices.clear();
        Plugin esg = plugin.getServer().getPluginManager().getPlugin("EconomyShopGUI");
        if (esg == null) return;

        File priceFile = new File(esg.getDataFolder(), "price_tiers.yml");
        if (!priceFile.exists()) return;

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(priceFile);
        org.bukkit.configuration.ConfigurationSection materials = cfg.getConfigurationSection("materials");
        if (materials == null) return;

        for (String key : materials.getKeys(false)) {
            Material mat = Material.matchMaterial(key);
            if (mat == null || !mat.isItem()) continue;

            Object val = materials.get(key);
            if (val instanceof Number) {
                double buy = ((Number) val).doubleValue();
                sellPrices.put(mat, Math.round(buy * 0.25 * 100.0) / 100.0);
            } else if (val instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> m = (Map<String, Object>) val;
                if (Boolean.TRUE.equals(m.get("no_sell"))) continue;
                Object b = m.get("buy");
                if (b instanceof Number) {
                    double buy = ((Number) b).doubleValue();
                    sellPrices.put(mat, Math.round(buy * 0.25 * 100.0) / 100.0);
                }
            }
        }
        lastLoad = System.currentTimeMillis();
    }

    private double getSellPrice(Material mat) {
        if (System.currentTimeMillis() - lastLoad > RELOAD_INTERVAL_MS) {
            loadPrices();
        }
        Double v = sellPrices.get(mat);
        return v == null ? -1 : v;
    }

    /** Lore line contains our sell price pattern (with or without color codes). */
    private static boolean isSellLoreLine(String line) {
        return line != null && (line.contains("Sell: $") || ChatColor.stripColor(line).contains("Sell: $"));
    }

    /**
     * Add or update exactly one "Sell: $X.XX each" line. Removes any existing sell lore lines first
     * so we never get duplicates from repeated inventory opens.
     */
    private void addSellLore(ItemStack stack, double sellPrice) {
        if (sellPrice <= 0) return;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return;

        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        lore.removeIf(SellPriceLoreListener::isSellLoreLine);
        String line = LORE_PREFIX + String.format("%.2f", sellPrice) + LORE_SUFFIX;
        lore.add(line);
        meta.setLore(lore);
        stack.setItemMeta(meta);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent e) {
        if (!(e.getPlayer() instanceof Player)) return;

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Player p = (Player) e.getPlayer();
            if (!p.isOnline()) return;

            for (ItemStack stack : p.getInventory().getContents()) {
                if (stack == null || stack.getType().isAir()) continue;
                if (CrateKeyGuard.isCrateKey(stack)) continue; // Crate keys cannot be sold
                double sell = getSellPrice(stack.getType());
                if (sell > 0) addSellLore(stack, sell);
            }
        }, 1L);
    }
}
