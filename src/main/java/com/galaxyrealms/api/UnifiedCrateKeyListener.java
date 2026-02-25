package com.galaxyrealms.api;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.io.File;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Unified crate key pool: prefer virtual keys first.
 * When a player opens a crate with a physical key but has virtual keys for that type,
 * we refund the physical key and deduct 1 virtual instead (so one pool, virtual used first).
 */
public class UnifiedCrateKeyListener implements Listener {

    private static final long SNAPSHOT_MAX_AGE_MS = 5000L;
    /** Physical key ids in PhoenixCrates (Supernova crate uses "Supernove" key id). */
    private static final String[] PHYSICAL_KEY_IDS = { "Op", "Cosmic", "Galaxy", "Nebula", "Supernove", "Vote_Crate" };
    private static final String[] VIRTUAL_KEY_IDS = { "Op_Virtual", "Cosmic_Virtual", "Galaxy_Virtual", "Nebula_Virtual", "Supernove_Virtual", "Vote_Crate_Virtual" };

    private final GalaxyRealmsAPI plugin;
    private final String crateCommand;
    /** Player UUID -> (physicalKeyId -> count at last chest right-click). */
    private final ConcurrentHashMap<UUID, Snapshot> snapshots = new ConcurrentHashMap<>();
    /** world,x,y,z -> crateId (from PhoenixCrates locations.yml). */
    private volatile Map<String, String> locationToCrateId = new HashMap<>();

    public UnifiedCrateKeyListener(GalaxyRealmsAPI plugin) {
        this.plugin = plugin;
        this.crateCommand = plugin.getConfigManager().getCrateCommand();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != org.bukkit.Material.CHEST) return;
        if (!Bukkit.getPluginManager().isPluginEnabled("PhoenixCrates")) return;
        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) return;

        Player player = event.getPlayer();
        Map<String, Integer> counts = new HashMap<>();
        for (int i = 0; i < PHYSICAL_KEY_IDS.length; i++) {
            String id = PHYSICAL_KEY_IDS[i];
            int c = getPhysicalKeyCount(player, id);
            counts.put(id, c);
        }
        snapshots.put(player.getUniqueId(), new Snapshot(counts, System.currentTimeMillis()));

        // Digital keys workaround: empty hand + virtual keys -> use 1 virtual, give physical key, they click again to open
        if (isHandEmpty(player) && !event.isCancelled()) {
            String crateId = getCrateIdAt(block);
            if (crateId == null) return;
            int idx = indexForCrateId(crateId);
            if (idx < 0) return;
            String physicalId = PHYSICAL_KEY_IDS[idx];
            String virtualId = VIRTUAL_KEY_IDS[idx];
            if (getVirtualKeyCount(player, virtualId) < 1) return;
            if (getPhysicalKeyCount(player, physicalId) > 0) return;

            event.setCancelled(true);
            runCrateCommand("takeKey", virtualId, player.getName(), 1);
            runCrateCommand("giveKey", physicalId, player.getName(), 1);
            Bukkit.getScheduler().runTaskLater(plugin, () ->
                player.sendMessage("§aVirtual key used. §7Right-click the crate again with the key in your hand to open."), 3L);
        }
    }

    private boolean isHandEmpty(Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off = player.getInventory().getItemInOffHand();
        return (main == null || main.getType() == Material.AIR) && (off == null || off.getType() == Material.AIR);
    }

    private String getCrateIdAt(Block block) {
        String key = block.getWorld().getName() + ";" + block.getX() + ";" + block.getY() + ";" + block.getZ();
        if (locationToCrateId.isEmpty()) loadLocationToCrateId();
        return locationToCrateId.get(key);
    }

    private void loadLocationToCrateId() {
        try {
            Plugin phoenix = Bukkit.getPluginManager().getPlugin("PhoenixCrates");
            if (phoenix == null) return;
            File file = new File(phoenix.getDataFolder(), "locations.yml");
            if (!file.exists()) return;
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            org.bukkit.configuration.ConfigurationSection locs = cfg.getConfigurationSection("locations");
            if (locs == null) return;
            Map<String, String> map = new HashMap<>();
            for (String crateId : locs.getKeys(false)) {
                List<String> list = locs.getStringList(crateId);
                for (String s : list) {
                    if (s != null && s.contains(";")) {
                        String[] parts = s.split(";");
                        if (parts.length >= 4) {
                            String world = parts[0].trim();
                            int x = parseInt(parts[1]);
                            int y = parseInt(parts[2]);
                            int z = parseInt(parts[3]);
                            map.put(world + ";" + x + ";" + y + ";" + z, crateId);
                        }
                    }
                }
            }
            locationToCrateId = map;
        } catch (Throwable t) {
            plugin.getAPILogger().warning("Could not load PhoenixCrates locations: " + t.getMessage());
        }
    }

    /**
     * Called via reflection from our EventExecutor when PhoenixCrates CrateOpenEvent fires.
     */
    public void onCrateOpen(Event event) {
        try {
            Method getPlayer = event.getClass().getMethod("getPlayer");
            Method getCrate = event.getClass().getMethod("getCrate");
            Object crate = getCrate.invoke(event);
            if (crate == null) return;
            String crateId = getCrateIdentifier(crate);
            Player player = (Player) getPlayer.invoke(event);
            if (player == null || crateId == null) return;

            int idx = indexForCrateId(crateId);
            if (idx < 0) return;

            String physicalId = PHYSICAL_KEY_IDS[idx];
            String virtualId = VIRTUAL_KEY_IDS[idx];

            Snapshot snap = snapshots.get(player.getUniqueId());
            if (snap == null || System.currentTimeMillis() - snap.timestamp > SNAPSHOT_MAX_AGE_MS) return;

            int before = snap.physicalCounts.getOrDefault(physicalId, 0);
            int after = getPhysicalKeyCount(player, physicalId);
            if (before - after != 1) return; // they used virtual or didn't use this key type

            int virtualNow = getVirtualKeyCount(player, virtualId);
            if (virtualNow < 1) return; // no virtual to deduct

            // Refund 1 physical and take 1 virtual (unified pool: virtual first)
            runCrateCommand("giveKey", physicalId, player.getName(), 1);
            runCrateCommand("takeKey", virtualId, player.getName(), 1);
        } catch (Throwable t) {
            plugin.getAPILogger().warning("UnifiedCrateKey onCrateOpen: " + t.getMessage());
        }
    }

    private String getCrateIdentifier(Object crate) {
        Class<?> c = crate.getClass();
        for (String methodName : new String[] { "getIdentifier", "getId", "getCrateId" }) {
            try {
                Method m = c.getMethod(methodName);
                Object v = m.invoke(crate);
                if (v instanceof String) return (String) v;
            } catch (Throwable ignored) { }
        }
        return null;
    }

    private int indexForCrateId(String crateId) {
        if (crateId == null) return -1;
        for (int i = 0; i < PHYSICAL_KEY_IDS.length; i++) {
            if (PHYSICAL_KEY_IDS[i].equalsIgnoreCase(crateId)) return i;
        }
        if ("Supernova".equalsIgnoreCase(crateId)) return 4; // crate id "Supernova" -> key "Supernove"
        return -1;
    }

    private int getPhysicalKeyCount(Player player, String keyId) {
        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) return 0;
        String ph = PlaceholderAPI.setPlaceholders(player, "%phoenixcrates_physicalkeys_" + keyId + "%");
        return parseInt(ph);
    }

    private int getVirtualKeyCount(Player player, String keyId) {
        if (!Bukkit.getPluginManager().isPluginEnabled("PhoenixCrates")) return 0;
        try {
            Class<?> apiClass = Class.forName("com.phoenixplugins.phoenixcrates.api.PhoenixCratesAPI");
            Object playersMgr = apiClass.getMethod("getPlayersManager").invoke(null);
            if (playersMgr == null) return 0;
            Object playerData = playersMgr.getClass().getMethod("getCachedDataNow", Player.class).invoke(playersMgr, player);
            if (playerData == null) return 0;
            Object val = playerData.getClass().getMethod("getVirtualKeys", String.class).invoke(playerData, keyId);
            if (val instanceof Number) return Math.max(0, ((Number) val).intValue());
        } catch (Throwable ignored) { }
        return 0;
    }

    private void runCrateCommand(String subCommand, String keyId, String playerName, int amount) {
        String cmd = crateCommand + " " + subCommand + " " + keyId + " " + playerName + " " + amount;
        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd));
    }

    private static int parseInt(String s) {
        if (s == null) return 0;
        try {
            return Integer.parseInt(s.replaceAll("[^0-9-]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static final class Snapshot {
        final Map<String, Integer> physicalCounts;
        final long timestamp;

        Snapshot(Map<String, Integer> physicalCounts, long timestamp) {
            this.physicalCounts = new HashMap<>(physicalCounts);
            this.timestamp = timestamp;
        }
    }
}
