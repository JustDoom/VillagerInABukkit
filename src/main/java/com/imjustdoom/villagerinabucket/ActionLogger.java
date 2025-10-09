package com.imjustdoom.villagerinabucket;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ActionLogger {
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final File logFile;
    private PrintWriter fileWriter;

    public ActionLogger(File dataFolder) {
        this.logFile = new File(dataFolder, "villager-actions.log");
        initializeFileWriter();
    }

    private void initializeFileWriter() {
        if (!Config.LOG_SAVE_TO_FILE) {
            return;
        }

        try {
            // Create parent directories if they don't exist
            if (!logFile.getParentFile().exists()) {
                logFile.getParentFile().mkdirs();
            }

            // Open file in append mode
            this.fileWriter = new PrintWriter(new FileWriter(logFile, true), true);
        } catch (IOException e) {
            VillagerInABucket.get().getLogger().severe("Failed to initialize action log file: " + e.getMessage());
        }
    }

    /**
     * Logs a villager pickup action
     * @param player the player who picked up the entity
     * @param entity the entity that was picked up
     * @param location the location where the pickup occurred
     */
    public void logPickup(Player player, Entity entity, Location location) {
        logAction("PICKUP", player, entity, location);
    }

    /**
     * Logs a villager placement action
     * @param player the player who placed the entity
     * @param entity the entity that was placed
     * @param location the location where the placement occurred
     */
    public void logPlace(Player player, Entity entity, Location location) {
        logAction("PLACE", player, entity, location);
    }

    private void logAction(String action, Player player, Entity entity, Location location) {
        if (!Config.LOG_ENABLED && !Config.LOG_SAVE_TO_FILE) {
            return;
        }

        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String logMessage = String.format(
            "[%s] %s | Player: %s (%s) | Entity: %s | World: %s | Location: %.2f, %.2f, %.2f",
            timestamp,
            action,
            player.getName(),
            player.getUniqueId(),
            entity.getType().name(),
            location.getWorld().getName(),
            location.getX(),
            location.getY(),
            location.getZ()
        );

        // Log to console if enabled
        if (Config.LOG_ENABLED) {
            VillagerInABucket.get().getLogger().info(logMessage);
        }

        // Log to file if enabled
        if (Config.LOG_SAVE_TO_FILE && fileWriter != null) {
            fileWriter.println(logMessage);
        }
    }

    /**
     * Closes the file writer and releases resources
     */
    public void close() {
        if (fileWriter != null) {
            fileWriter.close();
            fileWriter = null;
        }
    }

    /**
     * Reloads the logger (closes and reopens file writer)
     */
    public void reload() {
        close();
        initializeFileWriter();
    }
}
