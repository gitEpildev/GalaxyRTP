package com.galaxyrealms.api;

import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * PlaceholderAPI expansion for GalaxyRealms.
 * - %galaxyrealms_balance_short% - abbreviated balance (e.g. 1.5B, 500M)
 * - %galaxyrealms_topbalance_1% through topbalance_5% - formatted balance for leaderboard position (no nesting)
 * - %galaxyrealms_topbalance_1_username% - #1 balance player username (for NPC skin; resolves without player context)
 * - %galaxyrealms_topsupporter_1_username% through topsupporter_3_username% - top 3 website spenders (for NPC skins)
 * - %galaxyrealms_topsupporter_1_amount% through topsupporter_3_amount% - formatted spend ($1.5K, etc.)
 * - %galaxyrealms_format_short_number_<value>% - format raw number as 10K, 1M, 1B (for leaderboard values)
 * - %galaxyrealms_keys_<id>% - crate key count (from PhoenixCrates virtual/physical)
 * - %galaxyrealms_has_any_keys% - "1" if player has any crate keys, "0" else
 * - %galaxyrealms_cratekeys_line1% through line6 - one key per line (vertical, realtime)
 * - %galaxyrealms_rank_display% - LuckPerms-based rank: prefix first, else formatted primary group (for AdvancedChat)
 * - %galaxyrealms_region% - "EU" if this backend is EU (world name contains eu_main), else "USA" (for scoreboard)
 */
public class GalaxyRealmsExpansion extends PlaceholderExpansion {

    private static final String[] KEY_IDS = { "Op", "Cosmic", "Galaxy", "Nebula", "Supernove", "Vote_Crate" };
    private static final String[] KEY_LABELS = { "Op Crate", "Cosmic Crate", "Galaxy Crate", "Nebula", "Supernova Crate", "Vote Crate" };
    private static final int MAX_KEY_LINES = 6;
    /** Virtual key id = physical id + "_Virtual" (both work for opening crates; scoreboard shows combined count). */
    private static final String VIRTUAL_SUFFIX = "_Virtual";

    private final GalaxyRealmsAPI plugin;

    public GalaxyRealmsExpansion(GalaxyRealmsAPI plugin) {
        this.plugin = plugin;
    }

    @Override
    @NotNull
    public String getAuthor() {
        return plugin.getDescription().getAuthors().isEmpty() ? "GalaxyRealms" : String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return "galaxyrealms";
    }

    @Override
    @NotNull
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        // LuckPerms-based rank display for AdvancedChat (prefix or primary group, never hardcoded Owner)
        if (params.equalsIgnoreCase("rank_display")) {
            return RankDisplayResolver.getRankDisplay(plugin, player);
        }
        // Region label for multi-region scoreboard (USA vs EU backend)
        if (params.equalsIgnoreCase("region")) {
            for (org.bukkit.World w : Bukkit.getWorlds()) {
                if (w != null && w.getName().toLowerCase().contains("eu_main")) return "EU";
            }
            return "USA";
        }
        if (params.equalsIgnoreCase("balance_short")) {
            Economy economy = plugin.getEconomy();
            if (economy == null || player == null) return "0";
            try {
                double balance = economy.getBalance(player);
                return formatShort(balance);
            } catch (Throwable t) {
                return "0";
            }
        }
        // Top balance 1-5 from Vault (no ajLeaderboards) – always up to date
        if (params.matches("baltop_[1-5]_name")) {
            int pos = params.charAt(7) - '0';
            TopBalanceCache cache = plugin.getTopBalanceCache();
            TopBalanceCache.Entry e = cache != null ? cache.get(pos) : null;
            return e != null ? e.name : "—";
        }
        if (params.matches("baltop_[1-5]_value")) {
            int pos = params.charAt(7) - '0';
            TopBalanceCache cache = plugin.getTopBalanceCache();
            TopBalanceCache.Entry e = cache != null ? cache.get(pos) : null;
            return e != null ? formatShort(e.balance) : "0";
        }
        // Top supporters (website spending): username and formatted amount for positions 1-3
        if (params.matches("topsupporter_[1-3]_username(_display)?") || params.matches("topsupporter_[1-3]_amount")) {
            int pos = params.length() >= 14 ? (params.charAt(13) - '0') : 0;
            TopSupportersCache.Entry e = pos >= 1 && pos <= 3 ? plugin.getTopSupportersCache().get(pos) : null;
            if (params.endsWith("_username_display")) return e != null && !e.username.isEmpty() ? e.username : "—";
            if (params.endsWith("_username")) return e != null && !e.username.isEmpty() ? e.username : "Steve";
            if (params.endsWith("_amount")) return e != null ? formatDollars(e.totalCents) : "$0.00";
        }
        // Top balance #1 username for NPC skin (must resolve without player context; strip color codes)
        if (params.equalsIgnoreCase("topbalance_1_username")) {
            TopBalanceCache cache = plugin.getTopBalanceCache();
            TopBalanceCache.Entry e = cache != null ? cache.get(1) : null;
            if (e != null && !e.name.isEmpty() && !e.name.equals("—")) return e.name;
            String namePlaceholder = "%ajlb_lb_vault_eco_balance_1_alltime_name%";
            String resolved = PlaceholderAPI.setPlaceholders(player, namePlaceholder);
            return fixEmptyBoardDisplay(stripColorCodes(resolved));
        }
        // Top balance 1-5 name: show "---" when board is empty (e.g. after cache reset) instead of "Board does not exist"
        if (params.matches("topbalance_[1-5]_name")) {
            int pos = params.charAt(12) - '0'; // topbalance_N_name
            String namePlaceholder = "%ajlb_lb_vault_eco_balance_" + pos + "_alltime_name%";
            String resolved = PlaceholderAPI.setPlaceholders(player, namePlaceholder);
            return fixEmptyBoardDisplay(stripColorCodes(resolved));
        }
        // Top balance positions 1-5: resolve ajlb value and format (no nested % in hologram)
        if (params.matches("topbalance_[1-5]")) {
            int pos = params.charAt(params.length() - 1) - '0';
            String ajlbPlaceholder = "%ajlb_lb_vault_eco_balance_" + pos + "_alltime_value%";
            String resolved = PlaceholderAPI.setPlaceholders(player, ajlbPlaceholder);
            if (isEmptyBoardResponse(resolved)) return "---";
            return formatShort(parseDouble(resolved));
        }
        // Format any raw number as short (10K, 1M, 1B). Resolve inner placeholder if not yet parsed (e.g. ajlb)
        if (params.toLowerCase().startsWith("format_short_number_")) {
            String rest = params.substring("format_short_number_".length()).trim();
            double value;
            if (rest.startsWith("%") && rest.endsWith("%")) {
                String resolved = PlaceholderAPI.setPlaceholders(player, rest);
                value = parseDouble(resolved);
            } else {
                value = parseDouble(rest.replace(",", ""));
            }
            return formatShort(value);
        }
        // Crate keys: show physical key count only so scoreboard matches what you have (e.g. 7 keys = 7)
        if (params.toLowerCase().startsWith("keys_")) {
            String keyId = params.substring(5).trim();
            if (keyId.isEmpty()) return "0";
            String phoenixId = "Supernova".equalsIgnoreCase(keyId) ? "Supernove" : keyId;
            int count = getPhysicalKeyCountOnly(player, phoenixId);
            return String.valueOf(Math.max(0, count));
        }
        // Show crate keys section only when player has at least one physical key
        if (params.equalsIgnoreCase("has_any_keys")) {
            if (player == null) return "0";
            for (String phoenixId : KEY_IDS) {
                if (getPhysicalKeyCountOnly(player, phoenixId) > 0) return "1";
            }
            return "0";
        }
        // One key per line going down: cratekeys_line1 .. line6 (physical count only)
        if (params.toLowerCase().startsWith("cratekeys_line")) {
            if (player == null) return "";
            String num = params.substring("cratekeys_line".length()).trim();
            int lineIndex = parseInt(num);
            if (lineIndex < 1 || lineIndex > MAX_KEY_LINES) return "";
            List<String> parts = new ArrayList<>();
            for (int i = 0; i < KEY_IDS.length; i++) {
                int c = getPhysicalKeyCountOnly(player, KEY_IDS[i]);
                if (c > 0) parts.add("&5" + KEY_LABELS[i] + " &8» &f" + c);
            }
            int idx = lineIndex - 1;
            return (idx < parts.size()) ? parts.get(idx) : "";
        }
        return null;
    }

    /** Physical key count only (key items in inventory) so scoreboard shows what you actually have. */
    private int getPhysicalKeyCountOnly(OfflinePlayer player, String physicalKeyId) {
        if (player == null) return 0;
        // 1) Primary: count key items in inventory via PhoenixCrates KeyFacade (works for all keys e.g. Vote_Crate)
        if (Bukkit.getPluginManager().isPluginEnabled("PhoenixCrates") && player.isOnline()) {
            int fromInventory = countPhysicalKeysFromInventory(player.getPlayer(), physicalKeyId);
            if (fromInventory >= 0) return fromInventory;
        }
        // 2) Fallback: PlaceholderAPI (PhoenixCrates expansion uses crate id; may not exist for Vote_Crate)
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                String ph = "%phoenixcrates_physicalkeys_" + physicalKeyId + "%";
                String parsed = PlaceholderAPI.setPlaceholders(player, ph);
                if (parsed != null && !parsed.equals(ph)) {
                    int n = parseInt(parsed);
                    if (n >= 0) return Math.max(0, n);
                }
            } catch (Throwable ignored) { }
        }
        return 0;
    }

    /** Count physical keys in player inventory via PhoenixCrates KeyFacade.countInventoryKeys(Key, Player). Returns -1 if unavailable. */
    private int countPhysicalKeysFromInventory(Player player, String keyId) {
        try {
            Class<?> apiClass = Class.forName("com.phoenixplugins.phoenixcrates.api.PhoenixCratesAPI");
            Object keysMgr = apiClass.getMethod("getKeysManager").invoke(null);
            if (keysMgr == null) return -1;
            Object key = keysMgr.getClass().getMethod("getKeyByIdentifier", String.class).invoke(keysMgr, keyId);
            if (key == null) return -1;
            Class<?> keyClass = Class.forName("com.phoenixplugins.phoenixcrates.managers.keys.Key");
            Class<?> keyFacade = Class.forName("com.phoenixplugins.phoenixcrates.facades.KeyFacade");
            java.lang.reflect.Method countInventoryKeys = keyFacade.getMethod("countInventoryKeys", keyClass, Player.class);
            Object val = countInventoryKeys.invoke(null, key, player);
            if (val instanceof Number) return Math.max(0, ((Number) val).intValue());
        } catch (Throwable ignored) { }
        return -1;
    }

    /** Combined count: physical + virtual (kept for other use if needed). */
    private int getCombinedKeyCount(OfflinePlayer player, String physicalKeyId) {
        int total = 0;
        int vPhysical = getKeyCount(player, physicalKeyId);
        if (vPhysical > 0) total += vPhysical;
        else if (Bukkit.getPluginManager().isPluginEnabled("PhoenixCrates")) {
            try {
                String ph = "%phoenixcrates_physicalkeys_" + physicalKeyId + "%";
                String parsed = PlaceholderAPI.setPlaceholders(player, ph);
                if (parsed != null && !parsed.equals(ph)) {
                    int n = parseInt(parsed);
                    if (n > 0) total += n;
                }
            } catch (Throwable ignored) { }
        }
        String virtualKeyId = physicalKeyId + VIRTUAL_SUFFIX;
        int vVirtual = getKeyCount(player, virtualKeyId);
        if (vVirtual > 0) total += vVirtual;
        else if (Bukkit.getPluginManager().isPluginEnabled("PhoenixCrates")) {
            try {
                String ph = "%phoenixcrates_virtualkeys_" + virtualKeyId + "%";
                String parsed = PlaceholderAPI.setPlaceholders(player, ph);
                if (parsed != null && !parsed.equals(ph)) {
                    int n = parseInt(parsed);
                    if (n > 0) total += n;
                }
            } catch (Throwable ignored) { }
        }
        return total;
    }

    /** Get key count from PhoenixCrates via reflection (virtual keys). Returns -1 if unavailable. */
    private int getKeyCount(OfflinePlayer player, String phoenixKeyId) {
        if (player == null || !Bukkit.getPluginManager().isPluginEnabled("PhoenixCrates")) return -1;
        try {
            Class<?> apiClass = Class.forName("com.phoenixplugins.phoenixcrates.api.PhoenixCratesAPI");
            Object playersMgr = apiClass.getMethod("getPlayersManager").invoke(null);
            if (playersMgr == null) return -1;
            Object playerData;
            if (player.isOnline()) {
                java.lang.reflect.Method getCached = playersMgr.getClass().getMethod("getCachedDataNow", Player.class);
                playerData = getCached.invoke(playersMgr, player.getPlayer());
            } else {
                java.lang.reflect.Method getCached = playersMgr.getClass().getMethod("getPlayerIfCached", OfflinePlayer.class, boolean.class);
                playerData = getCached.invoke(playersMgr, player, true);
            }
            if (playerData == null) return -1;
            java.lang.reflect.Method getVirtualKeys = playerData.getClass().getMethod("getVirtualKeys", String.class);
            Object val = getVirtualKeys.invoke(playerData, phoenixKeyId);
            if (val instanceof Number) return Math.max(0, ((Number) val).intValue());
        } catch (Throwable ignored) { }
        return -1;
    }

    private static int parseInt(String s) {
        if (s == null) return 0;
        try {
            return Integer.parseInt(s.replaceAll("[^0-9-]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static double parseDouble(String s) {
        if (s == null || s.isEmpty()) return 0;
        try {
            return Double.parseDouble(s.replaceAll("[^0-9.\\-Ee]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** When ajLeaderboards cache is empty it returns "Board does not exist". Show "---" instead. */
    private static String fixEmptyBoardDisplay(String s) {
        if (s == null || s.isEmpty()) return "---";
        if (isEmptyBoardResponse(s)) return "---";
        return s;
    }

    private static boolean isEmptyBoardResponse(String s) {
        if (s == null) return true;
        String lower = s.toLowerCase();
        return lower.contains("board does not exist") || lower.contains("does not exist");
    }

    /** Strip & and § color/format codes and hex (#RRGGBB) for use as username (e.g. NPC skin). */
    private static String stripColorCodes(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.replaceAll("(&|§)[0-9a-fk-orA-FK-OR]", "")
                .replaceAll("(&|§)x((&|§)[0-9a-fA-F]){6}", "")
                .replaceAll("&#[0-9a-fA-F]{6}", "")
                .trim();
    }

    /** Format store spend: under $1000 as $XX.XX, else short form ($1.5K, $20, etc.). */
    private static String formatDollars(int totalCents) {
        if (totalCents < 0) totalCents = 0;
        double dollars = totalCents / 100.0;
        if (dollars < 0.01) return "$0.00";
        if (dollars < 1000) return "$" + String.format("%.2f", dollars);
        return "$" + formatShort(dollars);
    }

    /**
     * Format a number in short form: 1.5B, 500M, 1.2K, etc. (caps for scoreboard)
     */
    private static String formatShort(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return "0";
        long n = (long) value;
        if (n < 0) n = 0;
        if (n >= 1_000_000_000_000L) {
            double d = n / 1_000_000_000_000.0;
            return formatDecimal(d) + "T";
        }
        if (n >= 1_000_000_000) {
            double d = n / 1_000_000_000.0;
            return formatDecimal(d) + "B";
        }
        if (n >= 1_000_000) {
            double d = n / 1_000_000.0;
            return formatDecimal(d) + "M";
        }
        if (n >= 1_000) {
            double d = n / 1_000.0;
            return formatDecimal(d) + "K";
        }
        return String.valueOf(n);
    }

    private static String formatDecimal(double d) {
        if (d >= 100) return String.valueOf((long) d);
        if (d >= 10) return String.format("%.1f", d).replace(".0", "");
        if (d >= 1) return String.format("%.1f", d);
        if (d >= 0.1) return String.format("%.2f", d);
        return d >= 0.01 ? String.format("%.2f", d) : "0";
    }
}
