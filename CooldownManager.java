package com.animeclasses.managers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {

    // key = UUID + ":" + abilityKey   value = System.currentTimeMillis() when ability was used
    private final Map<String, Long> cooldowns = new HashMap<>();

    private String key(UUID uuid, String ability) {
        return uuid + ":" + ability;
    }

    /** Put a cooldown on an ability (milliseconds). */
    public void setCooldown(UUID uuid, String ability, long millis) {
        cooldowns.put(key(uuid, ability), System.currentTimeMillis() + millis);
    }

    /** Returns remaining cooldown in milliseconds, or 0 if ready. */
    public long getRemaining(UUID uuid, String ability) {
        long expires = cooldowns.getOrDefault(key(uuid, ability), 0L);
        return Math.max(0L, expires - System.currentTimeMillis());
    }

    public boolean isReady(UUID uuid, String ability) {
        return getRemaining(uuid, ability) == 0;
    }

    /** Formatted string like "2m 30s" or "45s". */
    public String getFormattedRemaining(UUID uuid, String ability) {
        long ms = getRemaining(uuid, ability);
        if (ms <= 0) return "§aREADY";
        long secs = (ms + 999) / 1000;
        if (secs >= 60) {
            return "§e" + (secs / 60) + "m " + (secs % 60) + "s";
        }
        return "§c" + secs + "s";
    }
}
