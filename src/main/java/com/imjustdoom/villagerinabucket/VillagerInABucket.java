package com.imjustdoom.villagerinabucket;

import com.destroystokyo.paper.entity.villager.Reputation;
import com.destroystokyo.paper.entity.villager.ReputationType;
import com.imjustdoom.villagerinabucket.event.PreVillagerPickupEvent;
import com.imjustdoom.villagerinabucket.event.PreVillagerPlaceEvent;
import com.imjustdoom.villagerinabucket.event.VillagerPickupEvent;
import com.imjustdoom.villagerinabucket.event.VillagerPlaceEvent;
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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class VillagerInABucket extends JavaPlugin implements Listener {
    public static String PREFIX = "[VIAB]";
    public static TextColor TEXT_COLOR = TextColor.color(2, 220, 5);

    public NamespacedKey key = new NamespacedKey(this, "villager_data");

    public VillagerInABucket() {
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        Config.init();

        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
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
        Bukkit.getPluginManager().registerEvents(this, this);

        Metrics metrics = new Metrics(this, 25722);
        metrics.addCustomChart(new SimplePie("updated_to_new_settings", () -> String.valueOf(Config.PERMISSIONS)));
        metrics.addCustomChart(new SimplePie("usingResourcepack", () -> String.valueOf(Config.RESOURCE_PACK)));
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
                    lore.add(Component.text("Region: " + villager.getVillagerType().getKey().getKey().toUpperCase(), TextColor.color(Color.GRAY.asRGB()), TextDecoration.ITALIC));
                    lore.add(Component.text("Profession: " + villager.getProfession(), TextColor.color(Color.GRAY.asRGB()), TextDecoration.ITALIC));
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
            player.getInventory().addItem(newStack);
            itemStack = newStack;
        } else {
            createVillagerBucket(itemStack, clicked, player);
        }
        clicked.remove();
        event.setCancelled(true);

        VillagerPickupEvent villagerPickupEvent = new VillagerPickupEvent(clicked, player, location, itemStack);
        villagerPickupEvent.callEvent();
    }

    @EventHandler
    public void bucketInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack itemStack = event.getItem();
        Location location = event.getInteractionPoint();

        // Ensure interaction point is not null
        if (location == null) {
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
        Entity entity = Bukkit.getUnsafe().deserializeEntity(dataContainer.get(this.key, PersistentDataType.BYTE_ARRAY), player.getWorld());

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

        VillagerPlaceEvent villagerPlaceEvent = new VillagerPlaceEvent(entity, player, location, itemStack);
        villagerPlaceEvent.callEvent();
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