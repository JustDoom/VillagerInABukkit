package com.imjustdoom.villagerinabucket;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class VLog {
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static FileWriter LOG_FILE_WRITER;
    public static final Object LOG_LOCK = new Object();

    public static void log(String log) {
        VillagerInABucket.get().getLogger().info(log);
    }

    public static void severe(String log) {
        VillagerInABucket.get().getLogger().severe(log);
    }

    public static void debug(String action, Player player, Entity entity, Location location) {
        debug(action, player.getName(), entity, location);
    }

    public static void debug(String action, String source, Entity entity, Location location) {
        String message = String.format("[DEBUG] [%s] - Source: %s - Entity: %s - Location: %s", action, source, entity, location);
        if (Config.CONSOLE_LOGGING) {
            VillagerInABucket.get().getLogger().info(message);
        }
        if (Config.FILE_LOGGING) {
            synchronized (LOG_LOCK) {
                if (LOG_FILE_WRITER != null) {
                    try {
                        LOG_FILE_WRITER.write("[" + LocalDateTime.now().format(TIMESTAMP_FORMAT) + "] " + message + System.lineSeparator());
                        LOG_FILE_WRITER.flush();
                    } catch (IOException e) {
                        VillagerInABucket.get().getLogger().severe("Unable to write to log file: " + e.getMessage());
                    }
                }
            }
        }
    }
}
