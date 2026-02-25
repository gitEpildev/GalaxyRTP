package com.galaxyrealms.api;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.InheritanceNode;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class GalaxyRealmsAPI extends JavaPlugin {
    
    private static GalaxyRealmsAPI instance;
    private Server httpServer;
    private APIHandler apiHandler;
    private LuckPerms luckPerms;
    private Economy economy;
    private ConfigManager configManager;
    private RateLimiter rateLimiter;
    private PendingRewardsManager pendingRewardsManager;
    private PlayerJoinListener playerJoinListener;
    private TopSupportersCache topSupportersCache;
    private TopBalanceCache topBalanceCache;
    private Logger logger;
    
    @Override
    public void onEnable() {
        instance = this;
        logger = getLogger();
        
        // Load configuration
        saveDefaultConfig();
        configManager = new ConfigManager(this);
        configManager.loadConfig();
        
        // Initialize LuckPerms
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") != null) {
            luckPerms = LuckPermsProvider.get();
            logger.info("LuckPerms integration enabled!");
        } else {
            logger.severe("LuckPerms not found! This plugin requires LuckPerms.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Initialize Vault Economy (optional)
        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp != null) {
                economy = rsp.getProvider();
            }
        }
        
        // Initialize rate limiter
        rateLimiter = new RateLimiter();
        
        // Initialize pending rewards manager
        pendingRewardsManager = new PendingRewardsManager(this);
        
        // Register player join listener for pending rewards
        playerJoinListener = new PlayerJoinListener(this);
        getServer().getPluginManager().registerEvents(playerJoinListener, this);
        logger.info("Player join listener registered for pending rewards");

        // Fix crate key display names on join (so names persist after relog)
        getServer().getPluginManager().registerEvents(new CrateKeyNameFixListener(this), this);
        logger.info("Crate key name fix listener registered (Java relog fix)");

        // Crate keys cannot be dropped or sold
        getServer().getPluginManager().registerEvents(new CrateKeyDropListener(), this);
        logger.info("Crate key drop/sell guard registered");
        if (Bukkit.getPluginManager().getPlugin("EconomyShopGUI") != null) {
            getServer().getPluginManager().registerEvents(new CrateKeySellGuardListener(), this);
        }

        // Show sell price on items in inventory + action bar (EconomyShopGUI integration)
        if (Bukkit.getPluginManager().getPlugin("EconomyShopGUI") != null) {
            getServer().getPluginManager().registerEvents(new SellPriceLoreListener(this), this);
            new SellPriceActionBarTask(this).start();
            logger.info("Sell price display enabled (lore + action bar)");
        }
        
        // Register commands
        if (getCommand("verifylink") != null) {
            getCommand("verifylink").setExecutor(new VerifyLinkCommand(this));
            logger.info("Registered /verifylink command");
        }
        if (getCommand("checkkeys") != null) {
            getCommand("checkkeys").setExecutor(new CheckKeysCommand(this));
            logger.info("Registered /checkkeys command");
        }
        if (getCommand("refreshkeys") != null) {
            getCommand("refreshkeys").setExecutor(new RefreshKeysCommand(this));
            logger.info("Registered /refreshkeys command (scoreboard refresh after key given)");
        }
        
        // Top supporters cache (website spending leaderboard)
        topSupportersCache = new TopSupportersCache(this);
        topSupportersCache.start();
        logger.info("Top supporters cache started (website spending)");

        // Top balance cache (from Vault – no ajLeaderboards dependency)
        if (economy != null) {
            topBalanceCache = new TopBalanceCache(this);
            topBalanceCache.start();
            logger.info("Top balance cache started (Vault top 5)");
        }

        // Register PlaceholderAPI expansion (balance_short for scoreboard)
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new GalaxyRealmsExpansion(this).register();
            logger.info("PlaceholderAPI expansion registered (%galaxyrealms_balance_short%)");
        }
        
        // Unified crate key pool (prefer virtual keys first) when PhoenixCrates + PlaceholderAPI present
        if (Bukkit.getPluginManager().isPluginEnabled("PhoenixCrates") && Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            UnifiedCrateKeyListener unifiedKeyListener = new UnifiedCrateKeyListener(this);
            getServer().getPluginManager().registerEvents(unifiedKeyListener, this);
            registerPhoenixCrateOpenEvent(unifiedKeyListener);
            logger.info("Unified crate key pool enabled (virtual keys used first)");
        }
        
        // One-time clear of physical keys from all online players (no command): run 30s after start if trigger file exists
        Bukkit.getScheduler().runTaskLater(this, new ClearKeysOnceTask(this), 600L);

        // Ensure crafting table recipe exists (4 planks -> crafting table); runs delayed after other plugins
        CraftingTableRecipeFix.runDelayed(this);

        // Start HTTP server
        if (configManager.isEnabled()) {
            startAPIServer();
            logger.info("GalaxyRealms API enabled on port " + configManager.getPort());
        } else {
            logger.warning("GalaxyRealms API is disabled in config!");
        }

        // Report player count to store backend (for dashboard) when backend URL and API key are set
        String backendUrl = configManager.getBackendUrl();
        String backendKey = configManager.getBackendApiKey();
        if (backendUrl != null && !backendUrl.isEmpty() && backendKey != null && !backendKey.isEmpty()) {
            new StatusReporterTask(this).start();
        }
    }
    
    @Override
    public void onDisable() {
        if (httpServer != null && httpServer.isStarted()) {
            try {
                httpServer.stop();
                logger.info("API server stopped.");
            } catch (Exception e) {
                logger.severe("Error stopping API server: " + e.getMessage());
            }
        }
    }
    
    /**
     * Register for PhoenixCrates CrateOpenEvent via reflection so we can refund physical and take virtual (unified pool).
     */
    @SuppressWarnings("unchecked")
    private void registerPhoenixCrateOpenEvent(UnifiedCrateKeyListener listener) {
        String[] eventClassNames = {
            "com.phoenixplugins.phoenixcrates.api.crate.events.CrateOpenEvent",
            "com.phoenixplugins.phoenixcrates.events.CrateOpenEvent",
            "com.phoenixplugins.phoenixcrates.api.events.CrateOpenEvent",
            "com.phoenixplugins.phoenixcrates.event.CrateOpenEvent"
        };
        for (String className : eventClassNames) {
            try {
                Class<?> eventClass = Class.forName(className);
                if (!Event.class.isAssignableFrom(eventClass)) {
                    logger.info("PhoenixCrates event class " + className + " is not a Bukkit Event; skipping.");
                    continue;
                }
                @SuppressWarnings("unchecked")
                Class<? extends Event> bukkitEventClass = (Class<? extends Event>) eventClass;
                getServer().getPluginManager().registerEvent(
                    bukkitEventClass,
                    listener,
                    EventPriority.MONITOR,
                    (l, event) -> listener.onCrateOpen(event),
                    this
                );
                logger.info("Registered for PhoenixCrates CrateOpenEvent: " + className);
                return;
            } catch (ClassNotFoundException ignored) {
            } catch (Throwable t) {
                logger.warning("PhoenixCrates CrateOpenEvent registration failed for " + className + ": " + t.getMessage());
            }
        }
        logger.warning("PhoenixCrates CrateOpenEvent not found; unified key pool will not refund physical->virtual.");
    }

    private void startAPIServer() {
        try {
            httpServer = new Server();
            org.eclipse.jetty.server.ServerConnector connector = new org.eclipse.jetty.server.ServerConnector(httpServer);
            connector.setHost(configManager.getHost());
            connector.setPort(configManager.getPort());
            httpServer.addConnector(connector);
            
            apiHandler = new APIHandler(this);
            httpServer.setHandler(apiHandler);
            httpServer.start();
            logger.info("API server started successfully on " + configManager.getHost() + ":" + configManager.getPort());
        } catch (Exception e) {
            logger.severe("Failed to start API server: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static GalaxyRealmsAPI getInstance() {
        return instance;
    }
    
    public LuckPerms getLuckPerms() {
        return luckPerms;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public RateLimiter getRateLimiter() {
        return rateLimiter;
    }
    
    public PendingRewardsManager getPendingRewardsManager() {
        return pendingRewardsManager;
    }
    
    public PlayerJoinListener getPlayerJoinListener() {
        return playerJoinListener;
    }
    
    public Economy getEconomy() {
        return economy;
    }

    public TopSupportersCache getTopSupportersCache() {
        return topSupportersCache;
    }

    public TopBalanceCache getTopBalanceCache() {
        return topBalanceCache;
    }
    
    public Logger getAPILogger() {
        return logger;
    }
}
