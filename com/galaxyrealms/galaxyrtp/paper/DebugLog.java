/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.plugin.java.JavaPlugin
 */
package com.galaxyrealms.galaxyrtp.paper;

import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class DebugLog {
    private static final String SESSION = "a229f5";

    public static void log(JavaPlugin plugin, String hypothesisId, String location, String message, Map<String, Object> data) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("sessionId", SESSION);
        payload.put("hypothesisId", hypothesisId);
        payload.put("location", location);
        payload.put("message", message);
        payload.put("data", data != null ? data : Map.of());
        payload.put("timestamp", System.currentTimeMillis());
        String line = DebugLog.simpleJson(payload);
        System.out.println("[RTP-DEBUG] " + line);
        Bukkit.getServer().getLogger().info("[RTP-DEBUG] " + line);
        try {
            Path p = plugin.getDataFolder().toPath().resolve("debug-rtp.ndjson");
            Files.createDirectories(p.getParent(), new FileAttribute[0]);
            Files.write(p, (line + "\n").getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
        catch (Throwable p) {
            // empty catch block
        }
        try {
            Path sessionPath = Paths.get("/home/minecraft/.cursor/debug-a229f5.log", new String[0]);
            try (FileOutputStream out = new FileOutputStream(sessionPath.toFile(), true);){
                out.write((line + "\n").getBytes(StandardCharsets.UTF_8));
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    private static String simpleJson(Map<String, Object> m) {
        StringBuilder sb = new StringBuilder().append('{');
        boolean first = true;
        for (Map.Entry<String, Object> e : m.entrySet()) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append('\"').append(DebugLog.escape(e.getKey())).append("\":");
            Object v = e.getValue();
            if (v == null) {
                sb.append("null");
                continue;
            }
            if (v instanceof Number || v instanceof Boolean) {
                sb.append(v);
                continue;
            }
            sb.append('\"').append(DebugLog.escape(String.valueOf(v))).append('\"');
        }
        return sb.append('}').toString();
    }

    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}

