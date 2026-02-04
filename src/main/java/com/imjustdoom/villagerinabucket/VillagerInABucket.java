package com.imjustdoom.villagerinabucket;

import com.destroystokyo.paper.entity.villager.Reputation;
import com.destroystokyo.paper.entity.villager.ReputationType;
import com.imjustdoom.villagerinabucket.event.PreVillagerPickupEvent;
import com.imjustdoom.villagerinabucket.event.PreVillagerPlaceEvent;
import com.imjustdoom.villagerinabucket.event.VillagerPickupEvent;
import com.imjustdoom.villagerinabucket.event.VillagerPlaceEvent;
import com.imjustdoom.villagerinabucket.listener.ReloadListener;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.CustomModelData;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.slf4j.event.Level;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class VillagerInABucket extends JavaPlugin implements Listener {
    private final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static String PREFIX = "[VIAB]";
    public static TextColor TEXT_COLOR = TextColor.color(2, 220, 5);

    public NamespacedKey key = new NamespacedKey(this, "villager_data");
    public FileWriter logFileWriter;

    public VillagerInABucket() {
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        Config.init();

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            LiteralCommandNode<CommandSourceStack> buildCommand = Commands.literal("villagerinabucket")
                    .requires(sender -> sender.getSender().hasPermission("villagerinabucket.commands"))
                    .executes(ctx -> {
                        ctx.getSource().getSender().sendMessage(Component.text(PREFIX + " VillagerInABucket version " + getPluginMeta().getVersion(), TEXT_COLOR));
                        return Command.SINGLE_SUCCESS;
                    }).then(Commands.literal("reload").executes(ctx -> {
                        Config.init();
                        ctx.getSource().getSender().sendMessage(Component.text(PREFIX + " VillagerInABucket has been reloaded!", TEXT_COLOR));
                        return Command.SINGLE_SUCCESS;
                    }))
                    .build();
            commands.registrar().register(buildCommand, List.of("viab"));
        });
        getServer().getPluginManager().registerEvents(this, this);

        if (getServer().getPluginManager().getPlugin("BetterReload") != null) {
            getServer().getPluginManager().registerEvents(new ReloadListener(), this);
        }

        Metrics metrics = new Metrics(this, 25722);
        metrics.addCustomChart(new SimplePie("updated_to_new_settings", () -> String.valueOf(Config.PERMISSIONS)));
        metrics.addCustomChart(new SimplePie("usingResourcepack", () -> String.valueOf(Config.RESOURCE_PACK)));
    }

    @Override
    public void onDisable() {
        if (this.logFileWriter != null) {
            try {
                this.logFileWriter.close();
            } catch (IOException e) {
                getLogger().severe("Unable to close villager action file logger");
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!Config.RESOURCE_PACK) {
            return;
        }
        try {
            event.getPlayer().sendResourcePacks(ResourcePackInfo.resourcePackInfo(UUID.fromString(Config.RESOURCE_PACK_ID), URI.create(Config.RESOURCE_PACK_URL), Config.RESOURCE_PACK_HASH));
        } catch (IllegalArgumentException exception) {
            getLogger().severe("The UUID '" + Config.RESOURCE_PACK_ID + "' is invalid");
        }
    }

    /**
     * Checks if the passed in ItemStack is a valid Villager In A Bucket item
     * @param itemStack the item stack to check
     * @return if the item is a Villager In A Bucket item
     */
    public boolean isVillagerBucket(ItemStack itemStack) {
        if (itemStack.getType() != Material.BUCKET || itemStack.getItemMeta() == null) {
            return false;
        }

        PersistentDataContainer dataContainer = itemStack.getItemMeta().getPersistentDataContainer();
        return dataContainer.has(this.key) && dataContainer.get(this.key, PersistentDataType.BYTE_ARRAY) != null;
    }

    /**
     * Creates a new Villager In A Bucket item
     * @param itemStack the ItemStack to modify
     * @param entity the entity to store in the bucket
     * @param player the player who is picking it up
     */
    public void createVillagerBucket(ItemStack itemStack, Entity entity, Player player) {
        entity.setVelocity(new Vector(0, 0, 0));
        entity.setFallDistance(0);
        switch (entity) {
            case Villager villager -> {
                if (!entity.isSilent()) {
                    player.getWorld().playSound(entity.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                }
                itemStack.setData(DataComponentTypes.CUSTOM_MODEL_DATA, CustomModelData.customModelData().addString(villager.getVillagerType().key().value()).build());
            }
            case ZombieVillager zombieVillager -> {
                if (!entity.isSilent()) {
                    player.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_AMBIENT, 1.0f, 1.0f);
                }
                itemStack.setData(DataComponentTypes.CUSTOM_MODEL_DATA, CustomModelData.customModelData().addString("zombie_villager").build());
            }
            case WanderingTrader trader -> {
                if (!entity.isSilent()) {
                    player.getWorld().playSound(entity.getLocation(), Sound.ENTITY_WANDERING_TRADER_NO, 1.0f, 1.0f);
                }
                itemStack.setData(DataComponentTypes.CUSTOM_MODEL_DATA, CustomModelData.customModelData().addString("wandering_trader").build());
            }
            default -> throw new IllegalStateException("Unexpected value: " + entity);
        }
        itemStack.editMeta(meta -> {
            switch (entity) {
                case Villager villager -> {
                    if ((Config.HARM_REPUTATION && !Config.PERMISSIONS) || (Config.PERMISSIONS && player.hasPermission("villagerinabucket.harm-reputation"))) {
                        Reputation reputation = villager.getReputation(player.getUniqueId());
                        int minorRep = reputation.getReputation(ReputationType.MINOR_NEGATIVE);
                        reputation.setReputation(ReputationType.MINOR_NEGATIVE, minorRep >= 175 ? 200 : minorRep + 25);
                        villager.setReputation(player.getUniqueId(), reputation);
                    }

                    meta.itemName(Component.text("Villager In A Bucket"));
                    List<Component> lore = new ArrayList<>();
                    lore.add(Component.text("Level: " + villager.getVillagerLevel(), TextColor.color(Color.GRAY.asRGB()), TextDecoration.ITALIC));
                    lore.add(Component.text("Region: " + villager.getVillagerType().getKey(), TextColor.color(Color.GRAY.asRGB()), TextDecoration.ITALIC));
                    lore.add(Component.text("Profession: ", TextColor.color(Color.GRAY.asRGB()), TextDecoration.ITALIC).append(Component.translatable(villager.getProfession().translationKey())));
                    if (!villager.isAdult()) {
                        lore.add(Component.text("Baby", TextColor.color(Color.GRAY.asRGB()), TextDecoration.ITALIC));
                    }
                    meta.lore(lore);
                }
                case ZombieVillager zombieVillager -> {
                    meta.itemName(Component.text("Zombie Villager In A Bucket"));
                    List<Component> lore = new ArrayList<>();
                    if (!zombieVillager.isAdult()) {
                        lore.add(Component.text("Baby", TextColor.color(Color.GRAY.asRGB()), TextDecoration.ITALIC));
                    }
                    meta.lore(lore);
                }
                case WanderingTrader wanderingTrader -> {
                    meta.itemName(Component.text("Wandering Trader In A Bucket"));
                    List<Component> lore = new ArrayList<>();
                    if (!wanderingTrader.isAdult()) {
                        lore.add(Component.text("Baby", TextColor.color(Color.GRAY.asRGB()), TextDecoration.ITALIC));
                    }
                    meta.lore(lore);
                }
                default -> throw new IllegalStateException("Unexpected value: " + entity);
            }
            meta.getPersistentDataContainer().set(this.key, PersistentDataType.BYTE_ARRAY, Bukkit.getUnsafe().serializeEntity(entity));
            meta.setMaxStackSize(1);
        });
    }

    @EventHandler
    public void villagerInteract(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        ItemStack itemStack = player.getInventory().getItem(event.getHand());
        Entity clicked = event.getRightClicked();
        // Make sure it could possibly be a villager bucket item
        if (itemStack.getType() != Material.BUCKET) {
            return;
        }

        // If it is a villager bucket item cancel the event - stops milking and picking up multiple villagers in a single bucket (overrides old one)
        if (isVillagerBucket(itemStack)) {
            event.setCancelled(true);
            return;
        }

        // Check if the clicked entity is able to be picked up
        if (((clicked.getType() != EntityType.VILLAGER || (!Config.VILLAGER && !Config.PERMISSIONS) || (Config.PERMISSIONS && !player.hasPermission("villagerinabucket.villager.pickup")))
                && (clicked.getType() != EntityType.WANDERING_TRADER || (!Config.WANDERING_TRADER && !Config.PERMISSIONS) || (Config.PERMISSIONS && !player.hasPermission("villagerinabucket.wandering_trader.pickup")))
                && (clicked.getType() != EntityType.ZOMBIE_VILLAGER || (!Config.ZOMBIE_VILLAGER && !Config.PERMISSIONS) || (Config.PERMISSIONS && !player.hasPermission("villagerinabucket.zombie_villager.pickup"))))) {
            return;
        }

        Location location = clicked.getLocation();
        PreVillagerPickupEvent preVillagerPickupEvent = new PreVillagerPickupEvent(clicked, player, location, itemStack);
        if (!preVillagerPickupEvent.callEvent()) {
            return;
        }

        // Handle single or multiple bucket stacks
        if (itemStack.getAmount() > 1 || player.getGameMode() == GameMode.CREATIVE) {
            ItemStack newStack = new ItemStack(Material.BUCKET);
            createVillagerBucket(newStack, clicked, player);
            if (player.getGameMode() != GameMode.CREATIVE) {
                itemStack.setAmount(itemStack.getAmount() - 1);
            }
            HashMap<Integer, ItemStack> failedMap = player.getInventory().addItem(newStack);
            itemStack = newStack;

            if (failedMap.size() == 1) {
                player.dropItem((ItemStack) failedMap.values().toArray()[0]);
            } else if (failedMap.size() > 1) {
                getLogger().severe("Yeah somehow you ended up with multiple buckets being created. Not sure what to say here...");
            }
        } else {
            createVillagerBucket(itemStack, clicked, player);
        }
        clicked.remove();
        event.setCancelled(true);

        log("PICKUP", player, clicked, location);

        VillagerPickupEvent villagerPickupEvent = new VillagerPickupEvent(clicked, player, location, itemStack);
        villagerPickupEvent.callEvent();
    }

    @EventHandler
    public void bucketInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack itemStack = event.getItem();

        // Ensure interaction point is not null
        if (event.getInteractionPoint() == null) {
            return;
        }

        // Check if the action is related to a villager in a bucket item
        if (!event.getAction().isRightClick() || itemStack == null || !isVillagerBucket(itemStack)) {
            return;
        }
        event.setCancelled(true);

        // Return after cancelling event if a block is null because it is either air or water which could override the villager
        if (event.getClickedBlock() == null) {
            return;
        }

        PersistentDataContainer dataContainer = itemStack.getItemMeta().getPersistentDataContainer();
        LivingEntity entity = (LivingEntity) Bukkit.getUnsafe().deserializeEntity(dataContainer.get(this.key, PersistentDataType.BYTE_ARRAY), player.getWorld());

        if ((((!Config.VILLAGER && entity.getType() == EntityType.VILLAGER)
                || (!Config.ZOMBIE_VILLAGER && entity.getType() == EntityType.ZOMBIE_VILLAGER)
                || (!Config.WANDERING_TRADER && entity.getType() == EntityType.WANDERING_TRADER))
                && Config.DISABLE_PLACING_OF_DISABLED && !Config.PERMISSIONS)
        ||
                ((!player.hasPermission("villagerinabucket.villager.place") && entity.getType() == EntityType.VILLAGER)
                || (!player.hasPermission("villagerinabucket.zombie_villager.place") && entity.getType() == EntityType.ZOMBIE_VILLAGER)
                || (!player.hasPermission("villagerinabucket.wandering_trader.place") && entity.getType() == EntityType.WANDERING_TRADER)
                && Config.PERMISSIONS)) {
            player.sendMessage(Component.text("You are not allowed to place this villager"));
            return;
        }

        BlockFace clickedFace = event.getBlockFace();
        Location location = event.getInteractionPoint().clone().add(clickedFace.getModX() * 0.5f, 0, clickedFace.getModZ() * 0.5f);
        if (clickedFace == BlockFace.DOWN) {
            location.subtract(0, entity.getHeight(), 0);
        }

        if (player.getWorld().getBlockAt(location.clone().subtract(0, 1, 0)).isSolid()) {
            location.setY(Math.floor(location.getY()));
        }

        if (player.getWorld().getBlockAt(location).isSolid()) {
            location.setY(Math.floor(location.getY()) + 1);
        }

        PreVillagerPlaceEvent preVillagerPlaceEvent = new PreVillagerPlaceEvent(entity, player, location, itemStack);
        if (!preVillagerPlaceEvent.callEvent()) {
            return;
        }

        entity.spawnAt(location, CreatureSpawnEvent.SpawnReason.BUCKET);
        if (player.getGameMode() != GameMode.CREATIVE) {
            itemStack.unsetData(DataComponentTypes.CUSTOM_MODEL_DATA);
            itemStack.editMeta(meta -> {
                meta.itemName(null);
                if (meta.hasCustomName()) { // TODO: Make custom item names rename villager
                    String customName = ((TextComponent) meta.customName()).content();
                    if (customName.equals("Villager In A Bucket") || customName.equals("Zombie Villager In A Bucket") || customName.equals("Wandering Trader In A Bucket")) {
                        meta.customName(null);
                    }
                }
                meta.getPersistentDataContainer().remove(this.key);
                meta.setMaxStackSize(null);
                if (meta.hasLore()) {
                    meta.lore(null);
                }
            });
        }

        if (!entity.isSilent()) {
            switch (entity) {
                case Villager villager -> {
                    if (!entity.isSilent()) {
                        player.getWorld().playSound(entity.getLocation(), Sound.ENTITY_VILLAGER_CELEBRATE, 1.0f, 1.0f);
                    }
                }
                case ZombieVillager zombieVillager -> {
                    if (!entity.isSilent()) {
                        player.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_AMBIENT, 1.0f, 1.0f);
                    }
                }
                case WanderingTrader trader -> {
                    if (!entity.isSilent()) {
                        player.getWorld().playSound(entity.getLocation(), Sound.ENTITY_WANDERING_TRADER_YES, 1.0f, 1.0f);
                    }
                }
                default -> throw new IllegalStateException("Unexpected value: " + entity);
            }
        }

        log("PLACE", player, entity, location);

        VillagerPlaceEvent villagerPlaceEvent = new VillagerPlaceEvent(entity, player, location, itemStack);
        villagerPlaceEvent.callEvent();
    }

    public void log(String action, Player player, Entity entity, Location location) {
        String message = String.format("[DEBUG] [%s] - Player: %s - Entity: %s - Location: %s", action, player.getName(), entity, location);
        if (Config.CONSOLE_LOGGING) {
            getLogger().info(message);
        }
        if (Config.FILE_LOGGING) {
            try {
                this.logFileWriter.write("[" + LocalDateTime.now().format(TIMESTAMP_FORMAT) + "] " + message + System.lineSeparator());
            } catch (IOException e) {
                getLogger().severe("Unable to write to log file: " + e.getMessage());
            }
        }
    }

    private static VillagerInABucket INSTANCE;

    /**
     * Gets the Villager In A Bucket instance
     * @return the instance
     */
    public static VillagerInABucket get() {
        return INSTANCE;
    }
}