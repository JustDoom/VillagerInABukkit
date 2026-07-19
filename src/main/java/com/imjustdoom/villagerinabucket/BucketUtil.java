package com.imjustdoom.villagerinabucket;

import com.destroystokyo.paper.entity.villager.Reputation;
import com.destroystokyo.paper.entity.villager.ReputationType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.CustomModelData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.craftbukkit.entity.CraftVillager;
import org.bukkit.entity.*;
import org.bukkit.entity.memory.MemoryKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class BucketUtil {
    /**
     * Checks if the passed in ItemStack is a valid Villager In A Bucket item
     * @param itemStack the item stack to check
     * @return if the item is a Villager In A Bucket item
     */
    public static boolean isVillagerBucket(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() != Material.BUCKET || itemStack.getItemMeta() == null) {
            return false;
        }

        PersistentDataContainer dataContainer = itemStack.getItemMeta().getPersistentDataContainer();
        return dataContainer.has(VillagerInABucket.get().key) && dataContainer.get(VillagerInABucket.get().key, PersistentDataType.BYTE_ARRAY) != null;
    }

    /**
     * Creates a new Villager In A Bucket item
     * @param itemStack the ItemStack to modify
     * @param entity the entity to store in the bucket
     * @param player the player who is picking it up
     */
    public static void createVillagerBucket(ItemStack itemStack, Entity entity, @Nullable Player player) {
        // Reset velocity and fall distance in case of catching a villager when falling so it doesn't add up
        entity.setVelocity(new Vector(0, 0, 0));
        entity.setFallDistance(0);

        switch (entity) {
            case Villager villager -> {
                if (!entity.isSilent()) {
                    entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                }
                itemStack.setData(DataComponentTypes.CUSTOM_MODEL_DATA, CustomModelData.customModelData().addString(villager.getVillagerType().key().value()).build());
            }
            case ZombieVillager ignored -> {
                if (!entity.isSilent()) {
                    entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_AMBIENT, 1.0f, 1.0f);
                }
                itemStack.setData(DataComponentTypes.CUSTOM_MODEL_DATA, CustomModelData.customModelData().addString("zombie_villager").build());
            }
            case WanderingTrader ignored -> {
                if (!entity.isSilent()) {
                    entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_WANDERING_TRADER_NO, 1.0f, 1.0f);
                }
                itemStack.setData(DataComponentTypes.CUSTOM_MODEL_DATA, CustomModelData.customModelData().addString("wandering_trader").build());
            }
            default -> throw new IllegalStateException("Unexpected value: " + entity);
        }
        itemStack.editMeta(meta -> {
            switch (entity) {
                case Villager villager -> {
                    // Player null check since there is no player if it comes from a dispenser
                    if (player != null && player.hasPermission("villagerinabucket.harm-reputation")) {
                        Reputation reputation = villager.getReputation(player.getUniqueId());
                        int minorRep = reputation.getReputation(ReputationType.MINOR_NEGATIVE);
                        reputation.setReputation(ReputationType.MINOR_NEGATIVE, minorRep >= 175 ? 200 : minorRep + 25);
                        villager.setReputation(player.getUniqueId(), reputation);
                    }

                    // Clear the POIs so other Villagers can access them, and they are not stuck in limbo
                    ((CraftVillager) villager).getHandle().releaseAllPois();
                    villager.setMemory(MemoryKey.HOME, null);
                    villager.setMemory(MemoryKey.JOB_SITE, null);
                    villager.setMemory(MemoryKey.POTENTIAL_JOB_SITE, null);
                    villager.setMemory(MemoryKey.MEETING_POINT, null);

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
            meta.getPersistentDataContainer().set(VillagerInABucket.get().key, PersistentDataType.BYTE_ARRAY, Bukkit.getUnsafe().serializeEntity(entity));
            meta.setMaxStackSize(1);
        });
    }

    public static LivingEntity entityFromBucket(ItemStack itemStack, World world) {
        PersistentDataContainer dataContainer = itemStack.getItemMeta().getPersistentDataContainer();
        return (LivingEntity) Bukkit.getUnsafe().deserializeEntity(dataContainer.get(VillagerInABucket.get().key, PersistentDataType.BYTE_ARRAY), world);
    }

    public static void playPlaceSound(LivingEntity entity) {
        if (!entity.isSilent()) {
            switch (entity) {
                case Villager villager -> {
                    if (!entity.isSilent()) {
                        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_VILLAGER_CELEBRATE, 1.0f, 1.0f);
                    }
                }
                case ZombieVillager zombieVillager -> {
                    if (!entity.isSilent()) {
                        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_AMBIENT, 1.0f, 1.0f);
                    }
                }
                case WanderingTrader trader -> {
                    if (!entity.isSilent()) {
                        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_WANDERING_TRADER_YES, 1.0f, 1.0f);
                    }
                }
                default -> throw new IllegalStateException("Unexpected value: " + entity);
            }
        }
    }

    /**
     * Checks if dispensers are allowed to place this villager
     */
    public static boolean canDispenserPlace(LivingEntity entity) {
        return (!Config.DISPENSER_VILLAGER_PLACE && entity.getType() == EntityType.VILLAGER)
                || (!Config.DISPENSER_ZOMBIE_VILLAGER_PLACE && entity.getType() == EntityType.ZOMBIE_VILLAGER)
                || (!Config.DISPENSER_WANDERING_TRADER_PLACE && entity.getType() == EntityType.WANDERING_TRADER);
    }

    /**
     * Checks if dispensers are allowed to place this villager
     */
    public static boolean canDispenserPickup(LivingEntity entity) {
        return (!Config.DISPENSER_VILLAGER_PICKUP && entity.getType() == EntityType.VILLAGER)
                || (!Config.DISPENSER_ZOMBIE_VILLAGER_PICKUP && entity.getType() == EntityType.ZOMBIE_VILLAGER)
                || (!Config.DISPENSER_WANDERING_TRADER_PICKUP && entity.getType() == EntityType.WANDERING_TRADER);
    }

    /**
     * Checks if the player may pick up the given entity type, honouring either
     * permissions mode or the per-type config toggles.
     */
    public static boolean canPickup(Player player, EntityType type) {
        return switch (type) {
            case VILLAGER -> player.hasPermission("villagerinabucket.villager.pickup");
            case ZOMBIE_VILLAGER -> player.hasPermission("villagerinabucket.zombie_villager.pickup");
            case WANDERING_TRADER -> player.hasPermission("villagerinabucket.wandering_trader.pickup");
            default -> false;
        };
    }

    /**
     * Checks if the player may place the given entity, honouring either the
     * per-type place permission or the disabled-placing config rule.
     */
    public static boolean canPlace(Player player, LivingEntity entity) {
        return switch (entity.getType()) {
            case VILLAGER -> player.hasPermission("villagerinabucket.villager.place");
            case ZOMBIE_VILLAGER -> player.hasPermission("villagerinabucket.zombie_villager.place");
            case WANDERING_TRADER -> player.hasPermission("villagerinabucket.wandering_trader.place");
            default -> true;
        };
    }
}
