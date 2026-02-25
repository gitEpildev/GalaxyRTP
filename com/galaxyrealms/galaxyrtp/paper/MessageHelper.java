/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.minimessage.MiniMessage
 *  net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
 */
package com.galaxyrealms.galaxyrtp.paper;

import com.galaxyrealms.galaxyrtp.paper.RtpPaperPlugin;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class MessageHelper {
    private static final MiniMessage MINI = MiniMessage.miniMessage();

    public static Component format(String messageKey, RtpPaperPlugin plugin) {
        return MessageHelper.format(messageKey, plugin, null);
    }

    public static Component format(String messageKey, RtpPaperPlugin plugin, Map<String, String> placeholders) {
        String raw = plugin.getPaperConfig().getMessage(messageKey, placeholders);
        if (raw == null || raw.isEmpty()) {
            return Component.empty();
        }
        try {
            return MINI.deserialize((Object)raw);
        }
        catch (Exception e) {
            return LegacyComponentSerializer.legacySection().deserialize(raw);
        }
    }
}

