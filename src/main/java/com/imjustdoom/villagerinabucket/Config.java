package com.imjustdoom.villagerinabucket;

import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Config {
    public static boolean DISPENSER_VILLAGER_PICKUP = true;
    public static boolean DISPENSER_VILLAGER_PLACE = true;
    public static boolean DISPENSER_ZOMBIE_VILLAGER_PICKUP = true;
    public static boolean DISPENSER_ZOMBIE_VILLAGER_PLACE = true;
    public static boolean DISPENSER_WANDERING_TRADER_PICKUP = true;
    public static boolean DISPENSER_WANDERING_TRADER_PLACE = true;

    public static boolean RESOURCE_PACK = true;
    public static String RESOURCE_PACK_URL = "https://cdn.modrinth.com/data/9tf9GGch/versions/ZBgqIN0Y/VillagerInABukkitPack.zip";
    public static String RESOURCE_PACK_HASH = "f2d4dd5bf8ee221234b738236099b2592c58b8e8";
    public static String RESOURCE_PACK_ID = "68a4b411-e409-4d89-b563-66049ba4914b";

    public static boolean CONSOLE_LOGGING = false;
    public static boolean FILE_LOGGING = false;

    public static void init() {
        VillagerInABucket.get().saveDefaultConfig();
        VillagerInABucket.get().reloadConfig();
        FileConfiguration fileConfiguration = VillagerInABucket.get().getConfig();

        if (fileConfiguration.contains("use-permissions", true) && !fileConfiguration.getBoolean("use-permissions", true)) {
            VLog.severe("You must update to the new permission system for configuring how buckets function: https://github.com/JustDoom/VillagerInABukkit/wiki/Configuring-using-permissions");
            VLog.severe("If you do not update, the plugin may not function as intended");
        }

        DISPENSER_VILLAGER_PICKUP = fileConfiguration.getBoolean("dispenser.villager.pickup", DISPENSER_VILLAGER_PICKUP);
        DISPENSER_VILLAGER_PLACE = fileConfiguration.getBoolean("dispenser.villager.place", DISPENSER_VILLAGER_PLACE);
        DISPENSER_WANDERING_TRADER_PICKUP = fileConfiguration.getBoolean("dispenser.wandering-trader.pickup", DISPENSER_WANDERING_TRADER_PICKUP);
        DISPENSER_WANDERING_TRADER_PLACE = fileConfiguration.getBoolean("dispenser.wandering-trader.place", DISPENSER_WANDERING_TRADER_PLACE);
        DISPENSER_ZOMBIE_VILLAGER_PICKUP = fileConfiguration.getBoolean("dispenser.zombie-villager.pickup", DISPENSER_ZOMBIE_VILLAGER_PICKUP);
        DISPENSER_ZOMBIE_VILLAGER_PLACE = fileConfiguration.getBoolean("dispenser.zombie-villager.place", DISPENSER_ZOMBIE_VILLAGER_PLACE);

        RESOURCE_PACK = fileConfiguration.getBoolean("resource-pack", RESOURCE_PACK);
        RESOURCE_PACK_URL = fileConfiguration.getString("resource-pack-url", RESOURCE_PACK_URL);
        RESOURCE_PACK_HASH = fileConfiguration.getString("resource-pack-hash", RESOURCE_PACK_HASH);
        RESOURCE_PACK_ID = fileConfiguration.getString("resource-pack-id", RESOURCE_PACK_ID);

        CONSOLE_LOGGING = fileConfiguration.getBoolean("console-logging", CONSOLE_LOGGING);
        FILE_LOGGING = fileConfiguration.getBoolean("file-logging", FILE_LOGGING);

        if (FILE_LOGGING) {
            try {
                if (VLog.LOG_FILE_WRITER != null) {
                    VLog.LOG_FILE_WRITER.close();
                }
                File logFile = new File(VillagerInABucket.get().getDataFolder(), "villager-actions.log");
                VLog.LOG_FILE_WRITER = new FileWriter(logFile, true);
            } catch (IOException e) {
                FILE_LOGGING = false;
                VillagerInABucket.get().getLogger().severe("Unable to create a log writer for villager actions: " + e.getMessage());
            }
        }
    }
}
