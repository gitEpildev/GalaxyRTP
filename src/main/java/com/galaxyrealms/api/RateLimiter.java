package com.galaxyrealms.api;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class RateLimiter {
    
    private final Map<String, RequestRecord> requests = new ConcurrentHashMap<>();
    
    public boolean checkRateLimit(String ip) {
        GalaxyRealmsAPI plugin = GalaxyRealmsAPI.getInstance();
        if (plugin == null) return true;
        
        ConfigManager config = plugin.getConfigManager();
        if (!config.isRateLimitEnabled()) {
            return true;
        }
        
        int maxRequests = config.getMaxRequests();
        long currentTime = System.currentTimeMillis();
        
        RequestRecord record = requests.computeIfAbsent(ip, k -> new RequestRecord());
        
        // Clean old requests (older than 1 minute)
        record.cleanOldRequests(currentTime, TimeUnit.MINUTES.toMillis(1));
        
        // Check if limit exceeded
        if (record.getRequestCount() >= maxRequests) {
            return false;
        }
        
        // Add new request
        record.addRequest(currentTime);
        return true;
    }
    
    private static class RequestRecord {
        private final java.util.List<Long> requestTimes = new java.util.ArrayList<>();
        
        public synchronized void addRequest(long timestamp) {
            requestTimes.add(timestamp);
        }
        
        public synchronized void cleanOldRequests(long currentTime, long windowMs) {
            requestTimes.removeIf(time -> (currentTime - time) > windowMs);
        }
        
        public synchronized int getRequestCount() {
            return requestTimes.size();
        }
    }
}
