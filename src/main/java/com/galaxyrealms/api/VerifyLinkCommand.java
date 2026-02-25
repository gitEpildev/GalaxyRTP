package com.galaxyrealms.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Logger;

/**
 * Command handler for /verifylink <code>
 * Allows players to verify their Discord link code in-game
 */
public class VerifyLinkCommand implements CommandExecutor {
    
    private final GalaxyRealmsAPI plugin;
    private final Gson gson;
    private final Logger logger;
    
    public VerifyLinkCommand(GalaxyRealmsAPI plugin) {
        this.plugin = plugin;
        this.gson = new Gson();
        this.logger = plugin.getAPILogger();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length != 1) {
            player.sendMessage(ChatColor.YELLOW + "Usage: " + ChatColor.WHITE + "/verifylink <code>");
            player.sendMessage(ChatColor.GRAY + "Get your code from the Discord bot using /link");
            return true;
        }
        
        String code = args[0].toUpperCase();
        
        // Validate code format (6 alphanumeric characters)
        if (!code.matches("[A-Z0-9]{6}")) {
            player.sendMessage(ChatColor.RED + "Invalid code format. Codes are 6 characters (letters and numbers).");
            return true;
        }
        
        player.sendMessage(ChatColor.YELLOW + "Verifying your link code...");
        
        // Run async to avoid blocking main thread
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            verifyCode(player, code);
        });
        
        return true;
    }
    
    private void verifyCode(Player player, String code) {
        try {
            String backendUrl = plugin.getConfigManager().getBackendUrl();
            String apiKey = plugin.getConfigManager().getBackendApiKey();
            
            if (backendUrl == null || backendUrl.isEmpty()) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.sendMessage(ChatColor.RED + "Link verification is not configured. Please contact an admin.");
                });
                return;
            }
            if (apiKey == null || apiKey.isEmpty()) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.sendMessage(ChatColor.RED + "Link verification is not configured. Please contact an admin.");
                });
                return;
            }
            
            // Make HTTP request to backend
            URL url = new URL(backendUrl + "/rankrole/link/verify");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setDoOutput(true);
            
            // Send request body
            JsonObject body = new JsonObject();
            body.addProperty("code", code);
            body.addProperty("minecraftUuid", player.getUniqueId().toString());
            body.addProperty("minecraftUsername", player.getName());
            
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = body.toString().getBytes("utf-8");
                os.write(input, 0, input.length);
            }
            
            int responseCode = conn.getResponseCode();
            
            // Read response
            StringBuilder responseText = new StringBuilder();
            BufferedReader br;
            if (responseCode >= 200 && responseCode < 300) {
                br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
            } else {
                br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "utf-8"));
            }
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                responseText.append(responseLine.trim());
            }
            br.close();
            
            // Parse response
            JsonObject backendResponse = gson.fromJson(responseText.toString(), JsonObject.class);
            
            // Send result to player on main thread
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (responseCode >= 200 && responseCode < 300 && backendResponse.has("success") && backendResponse.get("success").getAsBoolean()) {
                    player.sendMessage("");
                    player.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "SUCCESS!" + ChatColor.GREEN + " Your Discord account has been linked!");
                    player.sendMessage(ChatColor.GRAY + "Any rank roles you've purchased will be automatically assigned.");
                    player.sendMessage("");
                    
                    logger.info("Player " + player.getName() + " linked their Discord account successfully");
                } else {
                    String errorMsg = backendResponse.has("message") ? backendResponse.get("message").getAsString() : "Verification failed";
                    
                    if (errorMsg.toLowerCase().contains("expired")) {
                        player.sendMessage(ChatColor.RED + "This code has expired. Please get a new code from Discord using /link");
                    } else if (errorMsg.toLowerCase().contains("invalid")) {
                        player.sendMessage(ChatColor.RED + "Invalid code. Please check your code and try again.");
                    } else if (errorMsg.toLowerCase().contains("username")) {
                        player.sendMessage(ChatColor.RED + "This code was generated for a different Minecraft username.");
                    } else {
                        player.sendMessage(ChatColor.RED + "Verification failed: " + errorMsg);
                    }
                    
                    logger.warning("Player " + player.getName() + " link verification failed: " + errorMsg);
                }
            });
            
        } catch (Exception e) {
            logger.severe("Error verifying link code for " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
            
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                player.sendMessage(ChatColor.RED + "An error occurred while verifying your code. Please try again later.");
            });
        }
    }
}
