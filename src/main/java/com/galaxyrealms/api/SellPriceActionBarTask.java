package com.galaxyrealms.api;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Shows held item's sell value in the action bar (above hotbar) - no need to open inventory.
 */
public class SellPriceActionBarTask extends BukkitRunnable {

    private final Plugin plugin;
    private final Map<Material, Double> sellPrices = new HashMap<>();
    private long lastLoad = 0;
    private static final long RELOAD_INTERVAL_MS = 30_000;

    public SellPriceActionBarTask(Plugin plugin) {
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
        var materials = cfg.getConfigurationSection("materials");
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

    private static String formatName(Material mat) {
        String n = mat.name().toLowerCase().replace("_", " ");
        StringBuilder sb = new StringBuilder();
        for (String part : n.split(" ")) {
            if (part.length() > 0) {
                sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }

    @Override
    public void run() {
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (!p.isOnline()) continue;
            ItemStack held = p.getInventory().getItemInMainHand();
            if (held == null || held.getType().isAir()) {
                p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(""));
                continue;
            }
            if (CrateKeyGuard.isCrateKey(held)) {
                p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(""));
                continue;
            }

            double sellEach = getSellPrice(held.getType());
            if (sellEach <= 0) {
                p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(""));
                continue;
            }

            int amount = held.getAmount();
            double total = Math.round(sellEach * amount * 100.0) / 100.0;
            String name = held.hasItemMeta() && held.getItemMeta().hasDisplayName()
                    ? ChatColor.stripColor(held.getItemMeta().getDisplayName())
                    : formatName(held.getType());

            String msg = ChatColor.GREEN + name + " x" + amount
                    + ChatColor.DARK_GRAY + " | "
                    + ChatColor.GREEN + "Sell: " + ChatColor.DARK_GREEN + "$" + String.format("%.2f", sellEach) + " each";
            if (amount > 1) {
                msg += ChatColor.GRAY + " (" + ChatColor.DARK_GREEN + "$" + String.format("%.2f", total) + ChatColor.GRAY + " total)";
            }

            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(msg));
        }
    }

    public void start() {
        runTaskTimer(plugin, 20L, 20L); // 1 sec delay, 1 sec interval
    }
}
