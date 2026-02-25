package com.galaxyrealms.api;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.user.User;
import org.bukkit.OfflinePlayer;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Resolves player rank display from LuckPerms for AdvancedChat and TAB.
 * Uses LuckPerms prefix (keeps custom colours and names) and strips only
 * literal junk like {{dark_red}}, {dark_red} that render as visible text.
 * Preserves &amp; legacy codes and valid MiniMessage tags.
 */
public final class RankDisplayResolver {

    private RankDisplayResolver() {}

    /**
     * Resolves the rank display string for a player using LuckPerms.
     *
     * @param plugin GalaxyRealmsAPI instance (for LuckPerms access)
     * @param player Player to resolve rank for (can be offline)
     * @return LuckPerms prefix (with junk stripped) or formatted primary group. Empty if unavailable.
     */
    public static String getRankDisplay(GalaxyRealmsAPI plugin, OfflinePlayer player) {
        if (plugin == null || player == null) return "";
        LuckPerms luckPerms = plugin.getLuckPerms();
        if (luckPerms == null) return "";

        UUID uuid = player.getUniqueId();
        User user;

        if (player.isOnline()) {
            user = luckPerms.getUserManager().getUser(uuid);
        } else {
            try {
                user = luckPerms.getUserManager().loadUser(uuid)
                        .get(2, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                return "";
            }
        }

        if (user == null) return "";

        return resolveFromUser(user);
    }

    /**
     * Uses LuckPerms prefix first (keeps custom colours/names), strips only literal junk.
     * Fallback: formatted primary group if no prefix.
     */
    public static String resolveFromUser(User user) {
        CachedMetaData meta = user.getCachedData().getMetaData();
        String prefix = meta.getPrefix();
        if (prefix != null && !prefix.trim().isEmpty()) {
            String cleaned = stripLiteralJunkOnly(prefix);
            if (!cleaned.trim().isEmpty()) return cleaned;
        }

        String primaryGroup = user.getPrimaryGroup();
        if (primaryGroup == null || primaryGroup.isEmpty()) {
            primaryGroup = "default";
        }
        return formatGroupNameForDisplay(primaryGroup);
    }

    /**
     * Formats group name for display. Returns bracketed name e.g. [Admin], [Head Dev].
     */
    private static String formatGroupNameForDisplay(String groupName) {
        if (groupName == null || groupName.isEmpty()) return "";
        return "[" + capitalizeFirst(groupName) + "]";
    }

    /**
     * Strips ONLY literal junk that renders as visible text: {{dark_red}}, {dark_red}.
     * Preserves &amp; legacy codes (e.g. &amp;6, &amp;c) and valid MiniMessage tags.
     */
    private static String stripLiteralJunkOnly(String s) {
        if (s == null || s.isEmpty()) return s;
        String out = s;
        // Double brace: {{dark_red}} - never valid, always junk
        out = out.replaceAll("\\{\\{[a-zA-Z0-9_]+\\}\\}", "");
        // Single brace: {dark_red} - often stored literal, renders as text
        out = out.replaceAll("\\{[a-zA-Z0-9_]+\\}", "");
        return out.replaceAll("\\s+", " ").trim();
    }

    private static String capitalizeFirst(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
}
