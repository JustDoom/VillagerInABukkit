package com.imjustdoom.villagerinabucket;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public class Config {
    public static boolean PERMISSIONS = true;
    public static boolean VILLAGER = true;
    public static boolean ZOMBIE_VILLAGER = true;
    public static boolean WANDERING_TRADER = true;
    public static boolean DISABLE_PLACING_OF_DISABLED = false;
    public static boolean HARM_REPUTATION = false;

    public static boolean RESOURCE_PACK = true;
    public static String RESOURCE_PACK_URL = "https://github.com/JustDoom/VillagerInABukkit/releases/download/resourcepack-1.0.0/VillagerInABukkitPack.zip";
    public static String RESOURCE_PACK_HASH = "f2d4dd5bf8ee221234b738236099b2592c58b8e8";
    public static String RESOURCE_PACK_ID = "68a4b411-e409-4d89-b563-66049ba4914b";

    public static boolean LOG_ENABLED = true;
    public static boolean LOG_SAVE_TO_FILE = true;

    public static void init() {
        VillagerInABucket.get().saveDefaultConfig();
        VillagerInABucket.get().reloadConfig();
        FileConfiguration fileConfiguration = VillagerInABucket.get().getConfig();
        if (!fileConfiguration.contains("use-permissions", true)) {
            VillagerInABucket.get().getConfig().set("use-permissions", false);
            VillagerInABucket.get().getConfig().setComments("use-permissions", List.of(
                    "If enabled, the other options, \"villager\", \"zombie-villager\", \"wandering-trader\", \"disable-bucket-use-on-disable\" and \"harm-reputation\"",
                    "(except the resource pack options) will be ignored in favour of using permissions instead. This is the recommended method.",
                    "The other ones will be removed in the future"));
            VillagerInABucket.get().saveConfig();
            fileConfiguration = VillagerInABucket.get().getConfig();
        } else {
            PERMISSIONS = fileConfiguration.getBoolean("use-permissions", PERMISSIONS);
        }
        if (!PERMISSIONS) {
            VillagerInABucket.get().getLogger().warning("You are currently using the old deprecated methods of configuring the server. Please switch to using permissions.");
            VillagerInABucket.get().getLogger().warning("To do so set the 'use-permissions' config option to true. All of the permissions are given by default (Matches the old default settings) so a permission plugin such as LuckPerms should be used to configure them further.");
            VillagerInABucket.get().getLogger().warning("The old options will be removed in a future version of the plugin. Please read this to begin the switch https://github.com/JustDoom/VillagerInABukkit/wiki/Configuring-using-permissions");
        }

        VILLAGER = fileConfiguration.getBoolean("villager", VILLAGER);
        ZOMBIE_VILLAGER = fileConfiguration.getBoolean("zombie-villager", ZOMBIE_VILLAGER);
        WANDERING_TRADER = fileConfiguration.getBoolean("wandering-trader", WANDERING_TRADER);
        DISABLE_PLACING_OF_DISABLED = fileConfiguration.getBoolean("disable-bucket-use-on-disable", DISABLE_PLACING_OF_DISABLED);
        HARM_REPUTATION = fileConfiguration.getBoolean("harm-reputation", HARM_REPUTATION);

        RESOURCE_PACK = fileConfiguration.getBoolean("resource-pack", RESOURCE_PACK);
        RESOURCE_PACK_URL = fileConfiguration.getString("resource-pack-url", RESOURCE_PACK_URL);
        RESOURCE_PACK_HASH = fileConfiguration.getString("resource-pack-hash", RESOURCE_PACK_HASH);
        RESOURCE_PACK_ID = fileConfiguration.getString("resource-pack-id", RESOURCE_PACK_ID);

        LOG_ENABLED = fileConfiguration.getBoolean("logging.enabled", LOG_ENABLED);
        LOG_SAVE_TO_FILE = fileConfiguration.getBoolean("logging.save_to_file", LOG_SAVE_TO_FILE);
    }
}
