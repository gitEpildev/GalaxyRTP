package com.galaxyrealms.api;

import org.bukkit.*;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.meta.FireworkMeta;

import java.util.List;
import java.util.Random;

/**
 * Listens for player join events to deliver pending rank rewards
 */
public class PlayerJoinListener implements Listener {
    
    private final GalaxyRealmsAPI plugin;
    private final Random random = new Random();
    
    public PlayerJoinListener(GalaxyRealmsAPI plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Check for pending rewards after a short delay (let player fully load in)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            
            PendingRewardsManager rewardsManager = plugin.getPendingRewardsManager();
            if (rewardsManager == null) return;
            
            if (rewardsManager.hasPendingRewards(player.getUniqueId())) {
                List<String> pendingRanks = rewardsManager.getAndClearPendingRewards(player.getUniqueId());
                
                if (!pendingRanks.isEmpty()) {
                    plugin.getAPILogger().info("Delivering " + pendingRanks.size() + " pending reward(s) to " + player.getName());
                    
                    // Show the thank you message for each rank (or combined)
                    for (String rank : pendingRanks) {
                        showThankYouMessage(player, rank);
                    }
                }
            }
        }, 60L); // 3 second delay (60 ticks)
    }
    
    /**
     * Show the thank you message with fireworks
     */
    public void showThankYouMessage(Player player, String rank) {
        String rankDisplay = rank.substring(0, 1).toUpperCase() + rank.substring(1).toLowerCase();
        
        // Send chat messages
        player.sendMessage("");
        player.sendMessage("§d§l✦ §5§lGALAXY REALMS §d§l✦");
        player.sendMessage("");
        player.sendMessage("§fThank you for your purchase!");
        player.sendMessage("§fYou have received the §d" + rankDisplay + " §frank!");
        player.sendMessage("");
        player.sendMessage("§7Enjoy your new perks and cosmetics!");
        player.sendMessage("§7Type §e/rank §7to view your benefits.");
        player.sendMessage("");
        player.sendMessage("§d§l✦ §5§lGALAXY REALMS §d§l✦");
        player.sendMessage("");
        
        // Play sound effect
        try {
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        } catch (Exception e) {
            // Sound might not exist in all versions
        }
        
        // Send title
        player.sendTitle(
            "§d§lRANK RECEIVED!",
            "§fYou are now §d" + rankDisplay,
            10, 70, 20
        );
        
        // Launch fireworks!
        launchFireworks(player);
    }
    
    /**
     * Launch celebratory fireworks around the player
     */
    public void launchFireworks(Player player) {
        Location loc = player.getLocation();
        
        // Launch 3 fireworks with delays
        for (int i = 0; i < 3; i++) {
            final int delay = i * 10; // 0.5 second between each
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) return;
                
                // Random offset around player
                double offsetX = (random.nextDouble() - 0.5) * 4;
                double offsetZ = (random.nextDouble() - 0.5) * 4;
                Location fireworkLoc = loc.clone().add(offsetX, 0, offsetZ);
                
                Firework firework = player.getWorld().spawn(fireworkLoc, Firework.class);
                FireworkMeta meta = firework.getFireworkMeta();
                
                // Create purple/magenta themed firework (Galaxy theme)
                FireworkEffect.Builder effectBuilder = FireworkEffect.builder()
                    .withColor(Color.PURPLE, Color.FUCHSIA, Color.WHITE)
                    .withFade(Color.PURPLE, Color.BLUE)
                    .with(getRandomType())
                    .trail(true)
                    .flicker(true);
                
                meta.addEffect(effectBuilder.build());
                meta.setPower(1); // Height
                firework.setFireworkMeta(meta);
            }, delay);
        }
    }
    
    private FireworkEffect.Type getRandomType() {
        FireworkEffect.Type[] types = {
            FireworkEffect.Type.BALL_LARGE,
            FireworkEffect.Type.STAR,
            FireworkEffect.Type.BURST
        };
        return types[random.nextInt(types.length)];
    }
}
