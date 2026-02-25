package com.galaxyrealms.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.node.types.PermissionNode;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class APIHandler extends AbstractHandler {
    
    private final GalaxyRealmsAPI plugin;
    private final Gson gson;
    
    public APIHandler(GalaxyRealmsAPI plugin) {
        this.plugin = plugin;
        this.gson = new Gson();
    }
    
    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        // Mark request as handled
        baseRequest.setHandled(true);
        
        // Set CORS headers
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        response.setContentType("application/json; charset=utf-8");
        
        // Handle OPTIONS (CORS preflight)
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }
        
        // Get the path
        String path = target;
        if (path == null) path = request.getRequestURI();
        if (path == null) path = "";
        path = path.toLowerCase();
        
        // Remove query string if present
        int queryIndex = path.indexOf('?');
        if (queryIndex > 0) {
            path = path.substring(0, queryIndex);
        }
        
        // Rate limiting
        String clientIP = getClientIP(request);
        if (plugin.getConfigManager().isRateLimitEnabled()) {
            if (!plugin.getRateLimiter().checkRateLimit(clientIP)) {
                sendError(response, 429, "Rate limit exceeded");
                return;
            }
        }
        
        // Authentication check
        if (plugin.getConfigManager().isAuthRequired()) {
            String authHeader = request.getHeader("Authorization");
            String secretKey = plugin.getConfigManager().getSecretKey();
            String queryKey = request.getParameter("key");
            
            boolean authenticated = false;
            if (authHeader != null && authHeader.equals("Bearer " + secretKey)) {
                authenticated = true;
            } else if (queryKey != null && queryKey.equals(secretKey)) {
                authenticated = true;
            }
            
            if (!authenticated) {
                sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid authentication");
                return;
            }
        }
        
        // Route to handlers based on path
        String method = request.getMethod();
        
        // Log for debugging
        plugin.getAPILogger().info("API Request - path: '" + path + "', method: '" + method + "'");
        
        try {
            if (path.contains("status")) {
                plugin.getAPILogger().info("Matched status endpoint");
                handleStatus(request, response);
            } else if (path.contains("get-rank")) {
                plugin.getAPILogger().info("Matched get-rank endpoint");
                handleGetRank(request, response);
            } else if (path.contains("remove-rank")) {
                plugin.getAPILogger().info("Matched remove-rank endpoint");
                handleRemoveRank(request, response);
            } else if (path.contains("grant-rank")) {
                plugin.getAPILogger().info("Matched grant-rank endpoint");
                handleGrantRank(request, response);
            } else if (path.contains("check-player")) {
                plugin.getAPILogger().info("Matched check-player endpoint");
                handleCheckPlayer(request, response);
            } else if (path.contains("grant-item")) {
                plugin.getAPILogger().info("Matched grant-item endpoint");
                handleGrantItem(request, response);
            } else if (path.contains("grant-vault")) {
                plugin.getAPILogger().info("Matched grant-vault endpoint");
                handleGrantVault(request, response);
            } else if (path.contains("grant-crate-key")) {
                plugin.getAPILogger().info("Matched grant-crate-key endpoint");
                handleGrantCrateKey(request, response);
            } else if (path.contains("crate-keys")) {
                plugin.getAPILogger().info("Matched crate-keys endpoint");
                handleGetCrateKeys(request, response);
            } else if (path.contains("vault-range")) {
                plugin.getAPILogger().info("Matched vault-range endpoint");
                handleGetVaultRange(request, response);
            } else if (path.contains("player-vaults")) {
                plugin.getAPILogger().info("Matched player-vaults endpoint");
                handleGetPlayerVaults(request, response);
            } else if (path.contains("verify-link")) {
                plugin.getAPILogger().info("Matched verify-link endpoint");
                handleVerifyLink(request, response);
            } else {
                plugin.getAPILogger().warning("No endpoint matched for path: " + path);
                sendError(response, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found: " + path);
            }
        } catch (Exception e) {
            plugin.getAPILogger().severe("Error handling request: " + e.getMessage());
            e.printStackTrace();
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error: " + e.getMessage());
        }
    }
    
    private void handleRemoveRank(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JsonObject requestBody = parseRequestBody(request);
        
        if (requestBody == null) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid JSON body");
            return;
        }
        
        String username = requestBody.has("username") ? requestBody.get("username").getAsString() : null;
        String uuid = requestBody.has("uuid") ? requestBody.get("uuid").getAsString() : null;
        String rank = requestBody.has("rank") ? requestBody.get("rank").getAsString() : null;
        
        if (rank == null) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Missing 'rank' parameter");
            return;
        }
        
        // Validate rank
        if (!plugin.getConfigManager().isValidRank(rank)) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid rank: " + rank);
            return;
        }
        
        // Find player
        UUID playerUUID = null;
        
        if (username != null) {
            Player player = Bukkit.getPlayer(username);
            if (player != null) {
                playerUUID = player.getUniqueId();
            } else {
                @SuppressWarnings("deprecation")
                org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(username);
                if (offlinePlayer.hasPlayedBefore()) {
                    playerUUID = offlinePlayer.getUniqueId();
                }
            }
        } else if (uuid != null) {
            try {
                playerUUID = UUID.fromString(uuid);
            } catch (IllegalArgumentException e) {
                sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid UUID format");
                return;
            }
        } else {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Missing 'username' or 'uuid' parameter");
            return;
        }
        
        if (playerUUID == null) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Could not resolve player UUID");
            return;
        }
        
        final UUID finalPlayerUUID = playerUUID;
        final String finalRank = rank;
        final String finalUsername = username != null ? username : uuid;
        
        try {
            LuckPerms luckPerms = plugin.getLuckPerms();
            if (luckPerms == null) {
                sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "LuckPerms not available");
                return;
            }
            
            // Load user and remove rank
            CompletableFuture<User> userFuture = luckPerms.getUserManager().loadUser(finalPlayerUUID);
            User user = userFuture.join();
            
            if (user == null) {
                sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not load user data");
                return;
            }
            
            // Remove the rank
            user.data().remove(InheritanceNode.builder(finalRank).build());
            
            // Save changes
            luckPerms.getUserManager().saveUser(user);
            
            plugin.getAPILogger().info("Removed rank '" + finalRank + "' from player '" + finalUsername + "' (UUID: " + finalPlayerUUID + ")");
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Rank removed successfully");
            result.put("player", finalUsername);
            result.put("rank", finalRank);
            result.put("uuid", finalPlayerUUID.toString());
            sendJSON(response, result);
            
        } catch (Exception e) {
            plugin.getAPILogger().severe("Error removing rank: " + e.getMessage());
            e.printStackTrace();
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to remove rank: " + e.getMessage());
        }
    }

    private void handleGrantRank(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JsonObject requestBody = parseRequestBody(request);
        
        if (requestBody == null) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid JSON body");
            return;
        }
        
        String username = requestBody.has("username") ? requestBody.get("username").getAsString() : null;
        String uuid = requestBody.has("uuid") ? requestBody.get("uuid").getAsString() : null;
        String rank = requestBody.has("rank") ? requestBody.get("rank").getAsString() : null;
        
        if (rank == null) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Missing 'rank' parameter");
            return;
        }
        
        // Validate rank
        if (!plugin.getConfigManager().isValidRank(rank)) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid rank: " + rank);
            return;
        }
        
        // Find player
        Player player = null;
        UUID playerUUID = null;
        
        if (username != null) {
            // Try exact username first
            player = Bukkit.getPlayer(username);
            if (player != null) {
                playerUUID = player.getUniqueId();
            } else {
                // Try to get UUID from offline player
                @SuppressWarnings("deprecation")
                org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(username);
                if (offlinePlayer.hasPlayedBefore()) {
                    playerUUID = offlinePlayer.getUniqueId();
                } else {
                    // Try Floodgate username format for Bedrock players
                    // Floodgate adds "_" prefix and replaces spaces with "_"
                    String floodgateUsername = username;
                    if (!floodgateUsername.startsWith("_")) {
                        floodgateUsername = "_" + floodgateUsername.replace(" ", "_");
                    } else {
                        floodgateUsername = floodgateUsername.replace(" ", "_");
                    }
                    
                    if (!floodgateUsername.equals(username)) {
                        player = Bukkit.getPlayer(floodgateUsername);
                        if (player != null) {
                            playerUUID = player.getUniqueId();
                        } else {
                            @SuppressWarnings("deprecation")
                            org.bukkit.OfflinePlayer floodgateOffline = Bukkit.getOfflinePlayer(floodgateUsername);
                            if (floodgateOffline.hasPlayedBefore()) {
                                playerUUID = floodgateOffline.getUniqueId();
                            }
                        }
                    }
                }
            }
        } else if (uuid != null) {
            try {
                playerUUID = UUID.fromString(uuid);
                player = Bukkit.getPlayer(playerUUID);
            } catch (IllegalArgumentException e) {
                sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid UUID format");
                return;
            }
        } else {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Missing 'username' or 'uuid' parameter");
            return;
        }
        
        // Check if player is online (if required)
        if (plugin.getConfigManager().isRequireOnline() && player == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", "Player is not online");
            result.put("message", "Player must be online to receive rank");
            sendJSON(response, result);
            return;
        }
        
        // If we have no UUID at this point, we can't proceed
        if (playerUUID == null) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Could not resolve player UUID");
            return;
        }
        
        // Grant rank via LuckPerms
        final UUID finalPlayerUUID = playerUUID;
        final String finalRank = rank;
        final String finalUsername = username != null ? username : (player != null ? player.getName() : uuid);
        
        try {
            LuckPerms luckPerms = plugin.getLuckPerms();
            if (luckPerms == null) {
                sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "LuckPerms not available");
                return;
            }
            
            // Load user and grant rank
            CompletableFuture<User> userFuture = luckPerms.getUserManager().loadUser(finalPlayerUUID);
            User user = userFuture.join();
            
            if (user == null) {
                sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not load user data");
                return;
            }
            
            // Remove old ranks if configured
            if (plugin.getConfigManager().shouldRemoveOldRanks()) {
                for (String oldRank : plugin.getConfigManager().getAvailableRanks()) {
                    user.data().remove(InheritanceNode.builder(oldRank).build());
                }
            }
            
            // Add new rank
            user.data().add(InheritanceNode.builder(finalRank).build());
            
            // Save changes
            luckPerms.getUserManager().saveUser(user);
            
            plugin.getAPILogger().info("Granted rank '" + finalRank + "' to player '" + finalUsername + "' (UUID: " + finalPlayerUUID + ")");
            
            // Send thank you message to player if online, or save for later
            // Re-fetch player by UUID to ensure we have the correct reference
            final Player onlinePlayer = Bukkit.getPlayer(finalPlayerUUID);
            plugin.getAPILogger().info("Checking if player is online for message: " + (onlinePlayer != null ? "YES - " + onlinePlayer.getName() : "NO"));
            
            if (onlinePlayer != null && onlinePlayer.isOnline()) {
                // Player is online - show message and fireworks immediately
                Bukkit.getScheduler().runTask(plugin, () -> {
                    PlayerJoinListener listener = plugin.getPlayerJoinListener();
                    if (listener != null) {
                        listener.showThankYouMessage(onlinePlayer, finalRank);
                    }
                });
            } else {
                // Player is offline - save pending reward for when they log in
                PendingRewardsManager rewardsManager = plugin.getPendingRewardsManager();
                if (rewardsManager != null) {
                    rewardsManager.addPendingReward(finalPlayerUUID, finalRank);
                    plugin.getAPILogger().info("Player offline - saved pending reward for " + finalUsername);
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Rank granted successfully");
            result.put("player", finalUsername);
            result.put("rank", finalRank);
            result.put("uuid", finalPlayerUUID.toString());
            sendJSON(response, result);
            
        } catch (Exception e) {
            plugin.getAPILogger().severe("Error granting rank: " + e.getMessage());
            e.printStackTrace();
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to grant rank: " + e.getMessage());
        }
    }
    
    private void handleGrantVault(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JsonObject requestBody = parseRequestBody(request);

        if (requestBody == null) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid JSON body");
            return;
        }

        String username = requestBody.has("username") ? requestBody.get("username").getAsString() : null;
        String uuidStr = requestBody.has("uuid") ? requestBody.get("uuid").getAsString() : null;
        int vaultNumber;
        try {
            vaultNumber = requestBody.has("vault") ? requestBody.get("vault").getAsInt() : (requestBody.has("vault_number") ? requestBody.get("vault_number").getAsInt() : -1);
        } catch (Exception e) {
            vaultNumber = -1;
        }

        if (vaultNumber < 0) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Missing or invalid 'vault' or 'vault_number' (5-25)");
            return;
        }

        if (!plugin.getConfigManager().isValidVault(vaultNumber)) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Vault must be between " + plugin.getConfigManager().getVaultMin() + " and " + plugin.getConfigManager().getVaultMax());
            return;
        }

        UUID playerUUID = null;
        Player player = null;

        if (username != null) {
            // Try exact username first
            player = Bukkit.getPlayer(username);
            if (player != null) {
                playerUUID = player.getUniqueId();
            } else {
                @SuppressWarnings("deprecation")
                org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(username);
                if (offlinePlayer.hasPlayedBefore()) {
                    playerUUID = offlinePlayer.getUniqueId();
                } else {
                    // Try Floodgate username format for Bedrock players
                    // Floodgate adds "_" prefix and replaces spaces with "_"
                    String floodgateUsername = username;
                    if (!floodgateUsername.startsWith("_")) {
                        floodgateUsername = "_" + floodgateUsername.replace(" ", "_");
                    } else {
                        floodgateUsername = floodgateUsername.replace(" ", "_");
                    }
                    
                    if (!floodgateUsername.equals(username)) {
                        player = Bukkit.getPlayer(floodgateUsername);
                        if (player != null) {
                            playerUUID = player.getUniqueId();
                        } else {
                            @SuppressWarnings("deprecation")
                            org.bukkit.OfflinePlayer floodgateOffline = Bukkit.getOfflinePlayer(floodgateUsername);
                            if (floodgateOffline.hasPlayedBefore()) {
                                playerUUID = floodgateOffline.getUniqueId();
                            }
                        }
                    }
                }
            }
        } else if (uuidStr != null) {
            try {
                playerUUID = UUID.fromString(uuidStr);
                player = Bukkit.getPlayer(playerUUID);
            } catch (IllegalArgumentException e) {
                sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid UUID format");
                return;
            }
        } else {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Missing 'username' or 'uuid' parameter");
            return;
        }

        if (playerUUID == null) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Could not resolve player UUID");
            return;
        }

        final UUID finalUUID = playerUUID;
        final String displayName = username != null ? username : (player != null ? player.getName() : playerUUID.toString());
        final int finalVaultNumber = vaultNumber;
        final Player finalPlayer = player;

        try {
            LuckPerms luckPerms = plugin.getLuckPerms();
            if (luckPerms == null) {
                sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "LuckPerms not available");
                return;
            }

            User user = luckPerms.getUserManager().loadUser(finalUUID).join();
            if (user == null) {
                sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not load user data");
                return;
            }

            // Grant ALL vault permissions from 1 through vaultNumber (sequential, no gaps)
            // This ensures that purchasing X slots grants axvaults.vault.1 through axvaults.vault.X
            List<String> grantedPermissions = new ArrayList<>();
            for (int slot = 1; slot <= vaultNumber; slot++) {
                String permission = "axvaults.vault." + slot;
                PermissionNode node = PermissionNode.builder(permission).build();
                user.data().add(node);
                grantedPermissions.add(permission);
            }
            luckPerms.getUserManager().saveUser(user);

            plugin.getAPILogger().info("Granted " + vaultNumber + " vault slots (axvaults.vault.1 through axvaults.vault." + vaultNumber + ") to " + displayName + " (UUID: " + finalUUID + ")");

            // Send thank you message if player is online
            if (finalPlayer != null && finalPlayer.isOnline()) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    RankManager.sendPurchaseThankYou(finalPlayer, "vault", finalVaultNumber + " slots", 1);
                });
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", vaultNumber + " vault slots granted (vaults 1-" + vaultNumber + ")");
            result.put("player", displayName);
            result.put("uuid", finalUUID.toString());
            result.put("slots", vaultNumber);
            result.put("permissions", grantedPermissions);
            sendJSON(response, result);

        } catch (Exception e) {
            plugin.getAPILogger().severe("Error granting vault: " + e.getMessage());
            e.printStackTrace();
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to grant vault: " + e.getMessage());
        }
    }

    private void handleGrantCrateKey(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JsonObject requestBody = parseRequestBody(request);

        if (requestBody == null) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid JSON body");
            return;
        }

        String username = requestBody.has("username") ? requestBody.get("username").getAsString() : null;
        String uuidStr = requestBody.has("uuid") ? requestBody.get("uuid").getAsString() : null;
        String keyInput = requestBody.has("key") ? requestBody.get("key").getAsString() : (requestBody.has("key_id") ? requestBody.get("key_id").getAsString() : null);
        int amount = 1;
        try {
            if (requestBody.has("amount")) amount = Math.max(1, requestBody.get("amount").getAsInt());
        } catch (Exception ignored) {}

        if (keyInput == null || keyInput.isEmpty()) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Missing 'key' or 'key_id' parameter");
            return;
        }

        String keyId = null;
        for (String k : plugin.getConfigManager().getCrateKeys()) {
            if (k != null && k.equalsIgnoreCase(keyInput)) {
                keyId = k;
                break;
            }
        }
        if (keyId == null) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid key. Allowed: " + plugin.getConfigManager().getCrateKeys());
            return;
        }

        UUID playerUUID = null;
        String playerName = null;
        Player player = null;

        if (username != null) {
            // Try exact username first
            player = Bukkit.getPlayer(username);
            if (player != null) {
                playerUUID = player.getUniqueId();
                playerName = player.getName();
            } else {
                @SuppressWarnings("deprecation")
                org.bukkit.OfflinePlayer offline = Bukkit.getOfflinePlayer(username);
                if (offline.hasPlayedBefore()) {
                    playerUUID = offline.getUniqueId();
                    playerName = offline.getName();
                } else {
                    // Try Floodgate username format for Bedrock players
                    // Floodgate adds "_" prefix and replaces spaces with "_"
                    String floodgateUsername = username;
                    if (!floodgateUsername.startsWith("_")) {
                        floodgateUsername = "_" + floodgateUsername.replace(" ", "_");
                    } else {
                        floodgateUsername = floodgateUsername.replace(" ", "_");
                    }
                    
                    if (!floodgateUsername.equals(username)) {
                        player = Bukkit.getPlayer(floodgateUsername);
                        if (player != null) {
                            playerUUID = player.getUniqueId();
                            playerName = player.getName();
                        } else {
                            @SuppressWarnings("deprecation")
                            org.bukkit.OfflinePlayer floodgateOffline = Bukkit.getOfflinePlayer(floodgateUsername);
                            if (floodgateOffline.hasPlayedBefore()) {
                                playerUUID = floodgateOffline.getUniqueId();
                                playerName = floodgateOffline.getName();
                            }
                        }
                    }
                }
            }
        } else if (uuidStr != null) {
            try {
                playerUUID = UUID.fromString(uuidStr);
                player = Bukkit.getPlayer(playerUUID);
                if (player != null) {
                    playerName = player.getName();
                } else {
                    @SuppressWarnings("deprecation")
                    org.bukkit.OfflinePlayer off = Bukkit.getOfflinePlayer(playerUUID);
                    playerName = off.hasPlayedBefore() ? off.getName() : null;
                }
            } catch (IllegalArgumentException e) {
                sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid UUID format");
                return;
            }
        } else {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Missing 'username' or 'uuid' parameter");
            return;
        }

        if (playerUUID == null || playerName == null) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Could not resolve player");
            return;
        }

        if (plugin.getConfigManager().isCrateRequireOnline() && (player == null || !player.isOnline())) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", "Player must be online to receive crate keys");
            result.put("message", "Player must be online to receive crate keys");
            sendJSON(response, result);
            return;
        }

        // Grant physical keys only so count is seamless: buy 7 = 7 key items = scoreboard shows 7
        final String physicalKeyId = "Supernova".equalsIgnoreCase(keyId) ? "Supernove" : keyId;
        final String crateCmd = plugin.getConfigManager().getCrateCommand();
        final String finalKeyId = keyId;
        final String finalPlayerName = playerName;
        final int finalAmount = amount;
        final Player finalPlayer = player;

        Bukkit.getScheduler().runTask(plugin, () -> {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), crateCmd + " giveKey " + physicalKeyId + " " + finalPlayerName + " " + finalAmount);
            if (finalPlayer != null && finalPlayer.isOnline()) {
                RankManager.sendPurchaseThankYou(finalPlayer, "crate", finalKeyId, finalAmount);
                // Re-apply key display names after giveKey so they persist on relog
                Bukkit.getScheduler().runTaskLater(plugin, () -> CrateKeyNameFixListener.fixKeyNames(finalPlayer), 20L);
            }
        });

        plugin.getAPILogger().info("Granted " + finalAmount + "x " + physicalKeyId + " (physical) crate key(s) to " + finalPlayerName);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Crate key(s) granted");
        result.put("player", finalPlayerName);
        result.put("uuid", playerUUID.toString());
        result.put("key", finalKeyId);
        result.put("amount", finalAmount);
        sendJSON(response, result);
    }

    private void handleGrantItem(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JsonObject requestBody = parseRequestBody(request);
        
        if (requestBody == null) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid JSON body");
            return;
        }
        
        String username = requestBody.has("username") ? requestBody.get("username").getAsString() : null;
        String item = requestBody.has("item") ? requestBody.get("item").getAsString() : null;
        String kit = requestBody.has("kit") ? requestBody.get("kit").getAsString() : null;
        
        if (item == null && kit == null) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Missing 'item' or 'kit' parameter");
            return;
        }
        
        // Find player
        Player player = null;
        if (username != null) {
            player = Bukkit.getPlayer(username);
        }
        
        if (player == null) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Player not online");
            return;
        }
        
        // Grant item/kit logic would go here
        // For now, just return success
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Item/kit granted successfully");
        result.put("player", username);
        sendJSON(response, result);
    }

    private void handleGetCrateKeys(HttpServletRequest request, HttpServletResponse response) throws IOException {
        List<String> keys = plugin.getConfigManager().getCrateKeys();
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("keys", keys != null ? keys : Collections.emptyList());
        result.put("count", keys != null ? keys.size() : 0);
        sendJSON(response, result);
    }

    private void handleGetVaultRange(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("min", plugin.getConfigManager().getVaultMin());
        result.put("max", plugin.getConfigManager().getVaultMax());
        sendJSON(response, result);
    }

    /**
     * GET /api/player-vaults?username=PlayerName
     * Returns the vault slots a player currently owns by checking their LuckPerms permissions.
     * Response: { success: true, username: "...", currentSlots: 13, vaults: [1,2,3,...,13] }
     */
    private void handleGetPlayerVaults(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String username = request.getParameter("username");
        String uuidStr = request.getParameter("uuid");

        if (username == null && uuidStr == null) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Missing 'username' or 'uuid' parameter");
            return;
        }

        UUID playerUUID = null;

        // Resolve username to UUID
        if (username != null) {
            Player player = Bukkit.getPlayer(username);
            if (player != null) {
                playerUUID = player.getUniqueId();
            } else {
                @SuppressWarnings("deprecation")
                org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(username);
                if (offlinePlayer.hasPlayedBefore()) {
                    playerUUID = offlinePlayer.getUniqueId();
                }
            }
        } else if (uuidStr != null) {
            try {
                playerUUID = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException e) {
                sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid UUID format");
                return;
            }
        }

        if (playerUUID == null) {
            // Player has never joined - they own 0 vaults
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("username", username != null ? username : uuidStr);
            result.put("currentSlots", 0);
            result.put("vaults", Collections.emptyList());
            result.put("note", "Player has never joined the server");
            sendJSON(response, result);
            return;
        }

        try {
            LuckPerms luckPerms = plugin.getLuckPerms();
            if (luckPerms == null) {
                sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "LuckPerms not available");
                return;
            }

            User user = luckPerms.getUserManager().loadUser(playerUUID).join();
            if (user == null) {
                // User doesn't have LuckPerms data yet - owns 0 vaults
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("username", username != null ? username : playerUUID.toString());
                result.put("uuid", playerUUID.toString());
                result.put("currentSlots", 0);
                result.put("vaults", Collections.emptyList());
                sendJSON(response, result);
                return;
            }

            // Check for vault permissions (axvaults.vault.1 through axvaults.vault.25)
            List<Integer> ownedVaults = new ArrayList<>();
            int minVault = plugin.getConfigManager().getVaultMin();
            int maxVault = plugin.getConfigManager().getVaultMax();

            for (int i = 1; i <= maxVault; i++) {
                String permission = "axvaults.vault." + i;
                // Check if user has this permission (directly or via inheritance)
                if (user.getCachedData().getPermissionData().checkPermission(permission).asBoolean()) {
                    ownedVaults.add(i);
                }
            }

            // The current slots is the highest vault number owned
            int currentSlots = ownedVaults.isEmpty() ? 0 : Collections.max(ownedVaults);

            plugin.getAPILogger().info("Player " + (username != null ? username : playerUUID) + 
                " owns " + currentSlots + " vault slots (vaults: " + ownedVaults + ")");

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("username", username != null ? username : user.getUsername());
            result.put("uuid", playerUUID.toString());
            result.put("currentSlots", currentSlots);
            result.put("vaults", ownedVaults);
            sendJSON(response, result);

        } catch (Exception e) {
            plugin.getAPILogger().severe("Error checking player vaults: " + e.getMessage());
            e.printStackTrace();
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to check vault ownership: " + e.getMessage());
        }
    }
    
    private void handleCheckPlayer(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String username = request.getParameter("username");
        String uuid = request.getParameter("uuid");
        
        if (username == null && uuid == null) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Missing 'username' or 'uuid' parameter");
            return;
        }
        
        Player player = null;
        if (username != null) {
            player = Bukkit.getPlayer(username);
        } else if (uuid != null) {
            try {
                UUID playerUUID = UUID.fromString(uuid);
                player = Bukkit.getPlayer(playerUUID);
            } catch (IllegalArgumentException e) {
                sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid UUID format");
                return;
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("online", player != null);
        result.put("username", username != null ? username : (player != null ? player.getName() : null));
        sendJSON(response, result);
    }
    
    private void handleGetRank(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String username = request.getParameter("username");
        String uuid = request.getParameter("uuid");
        
        if (username == null && uuid == null) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Missing 'username' or 'uuid' parameter");
            return;
        }
        
        UUID playerUUID = null;
        
        if (username != null) {
            Player player = Bukkit.getPlayer(username);
            if (player != null) {
                playerUUID = player.getUniqueId();
            } else {
                @SuppressWarnings("deprecation")
                org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(username);
                if (offlinePlayer.hasPlayedBefore()) {
                    playerUUID = offlinePlayer.getUniqueId();
                } else {
                    sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Could not resolve username to UUID");
                    return;
                }
            }
        } else if (uuid != null) {
            try {
                playerUUID = UUID.fromString(uuid);
            } catch (IllegalArgumentException e) {
                sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid UUID format");
                return;
            }
        }
        
        try {
            LuckPerms luckPerms = plugin.getLuckPerms();
            if (luckPerms == null) {
                sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "LuckPerms not available");
                return;
            }
            
            User user = luckPerms.getUserManager().loadUser(playerUUID).join();
            if (user == null) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("rank", null);
                result.put("message", "Player has no rank");
                sendJSON(response, result);
                return;
            }
            
            // Get primary group (rank)
            String primaryGroup = user.getPrimaryGroup();
            plugin.getAPILogger().info("Player " + username + " primary group: " + primaryGroup);
            
            // Check if it's one of our ranks
            List<String> availableRanks = plugin.getConfigManager().getAvailableRanks();
            plugin.getAPILogger().info("Available ranks: " + availableRanks);
            
            String currentRank = null;
            int highestRankIndex = -1;
            
            // First check primary group
            for (int i = 0; i < availableRanks.size(); i++) {
                if (availableRanks.get(i).equalsIgnoreCase(primaryGroup)) {
                    currentRank = availableRanks.get(i);
                    highestRankIndex = i;
                    plugin.getAPILogger().info("Found rank from primary group: " + currentRank);
                    break;
                }
            }
            
            // Also check inherited groups for the highest rank
            try {
                var inheritedGroups = user.getInheritedGroups(user.getQueryOptions());
                plugin.getAPILogger().info("Inherited groups count: " + inheritedGroups.size());
                for (var group : inheritedGroups) {
                    String groupName = group.getName();
                    plugin.getAPILogger().info("Checking inherited group: " + groupName);
                    for (int i = 0; i < availableRanks.size(); i++) {
                        if (availableRanks.get(i).equalsIgnoreCase(groupName)) {
                            // Lower index = higher rank (nova is index 0)
                            if (highestRankIndex == -1 || i < highestRankIndex) {
                                currentRank = availableRanks.get(i);
                                highestRankIndex = i;
                                plugin.getAPILogger().info("Found higher rank from inherited: " + currentRank);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                plugin.getAPILogger().warning("Error checking inherited groups: " + e.getMessage());
            }
            
            plugin.getAPILogger().info("Final rank for " + username + ": " + currentRank);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("rank", currentRank);
            result.put("username", username);
            result.put("uuid", playerUUID.toString());
            sendJSON(response, result);
            
        } catch (Exception e) {
            plugin.getAPILogger().severe("Error getting rank: " + e.getMessage());
            e.printStackTrace();
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to get rank: " + e.getMessage());
        }
    }
    
    private void handleStatus(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("status", "online");
        result.put("server", plugin.getServer().getName());
        result.put("online_players", Bukkit.getOnlinePlayers().size());
        result.put("max_players", Bukkit.getMaxPlayers());
        result.put("version", plugin.getDescription().getVersion());
        sendJSON(response, result);
    }

    /**
     * POST /api/verify-link
     * Verifies a Discord link code for a player
     * Request body: { "code": "ABC123", "minecraftUuid": "uuid", "minecraftUsername": "Steve" }
     * Calls the backend to verify the code and link accounts
     */
    private void handleVerifyLink(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JsonObject requestBody = parseRequestBody(request);
        
        if (requestBody == null) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid JSON body");
            return;
        }
        
        String code = requestBody.has("code") ? requestBody.get("code").getAsString() : null;
        String minecraftUuid = requestBody.has("minecraftUuid") ? requestBody.get("minecraftUuid").getAsString() : null;
        String minecraftUsername = requestBody.has("minecraftUsername") ? requestBody.get("minecraftUsername").getAsString() : null;
        
        if (code == null || minecraftUuid == null || minecraftUsername == null) {
            sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Missing required parameters: code, minecraftUuid, minecraftUsername");
            return;
        }
        
        plugin.getAPILogger().info("Verify link request - code: " + code + ", username: " + minecraftUsername);
        
        // Call the backend API to verify the code
        try {
            String backendUrl = plugin.getConfigManager().getBackendUrl();
            String secretKey = plugin.getConfigManager().getSecretKey();
            
            if (backendUrl == null || backendUrl.isEmpty()) {
                sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Backend URL not configured");
                return;
            }
            
            // Make HTTP request to backend
            java.net.URL url = new java.net.URL(backendUrl + "/rankrole/link/verify");
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + secretKey);
            conn.setDoOutput(true);
            
            // Send request body
            JsonObject body = new JsonObject();
            body.addProperty("code", code);
            body.addProperty("minecraftUuid", minecraftUuid);
            body.addProperty("minecraftUsername", minecraftUsername);
            
            try (java.io.OutputStream os = conn.getOutputStream()) {
                byte[] input = body.toString().getBytes("utf-8");
                os.write(input, 0, input.length);
            }
            
            int responseCode = conn.getResponseCode();
            
            // Read response
            StringBuilder responseText = new StringBuilder();
            java.io.BufferedReader br;
            if (responseCode >= 200 && responseCode < 300) {
                br = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream(), "utf-8"));
            } else {
                br = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getErrorStream(), "utf-8"));
            }
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                responseText.append(responseLine.trim());
            }
            br.close();
            
            // Parse response
            JsonObject backendResponse = gson.fromJson(responseText.toString(), JsonObject.class);
            
            if (responseCode >= 200 && responseCode < 300 && backendResponse.has("success") && backendResponse.get("success").getAsBoolean()) {
                plugin.getAPILogger().info("Link verified successfully for " + minecraftUsername);
                
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("message", "Discord account linked successfully!");
                result.put("minecraftUsername", minecraftUsername);
                if (backendResponse.has("discordUserId")) {
                    result.put("discordUserId", backendResponse.get("discordUserId").getAsString());
                }
                sendJSON(response, result);
            } else {
                String errorMsg = backendResponse.has("message") ? backendResponse.get("message").getAsString() : "Verification failed";
                plugin.getAPILogger().warning("Link verification failed: " + errorMsg);
                sendError(response, HttpServletResponse.SC_BAD_REQUEST, errorMsg);
            }
            
        } catch (Exception e) {
            plugin.getAPILogger().severe("Error verifying link code: " + e.getMessage());
            e.printStackTrace();
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to verify link: " + e.getMessage());
        }
    }
    
    private JsonObject parseRequestBody(HttpServletRequest request) {
        try {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = request.getReader().readLine()) != null) {
                sb.append(line);
            }
            return gson.fromJson(sb.toString(), JsonObject.class);
        } catch (Exception e) {
            return null;
        }
    }
    
    private void sendJSON(HttpServletResponse response, Map<String, Object> data) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        PrintWriter out = response.getWriter();
        out.print(gson.toJson(data));
        out.flush();
    }
    
    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("error", message);
        PrintWriter out = response.getWriter();
        out.print(gson.toJson(error));
        out.flush();
    }
    
    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String cfConnectingIP = request.getHeader("CF-Connecting-IP");
        if (cfConnectingIP != null && !cfConnectingIP.isEmpty()) {
            return cfConnectingIP;
        }
        return request.getRemoteAddr();
    }
}
