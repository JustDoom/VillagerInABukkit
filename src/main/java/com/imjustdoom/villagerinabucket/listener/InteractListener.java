package com.imjustdoom.villagerinabucket.listener;

import com.imjustdoom.villagerinabucket.BucketUtil;
import com.imjustdoom.villagerinabucket.VLog;
import com.imjustdoom.villagerinabucket.VillagerInABucket;
import com.imjustdoom.villagerinabucket.event.PreVillagerPickupEvent;
import com.imjustdoom.villagerinabucket.event.PreVillagerPlaceEvent;
import com.imjustdoom.villagerinabucket.event.VillagerPickupEvent;
import com.imjustdoom.villagerinabucket.event.VillagerPlaceEvent;
import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

public class InteractListener implements Listener {
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
        if (BucketUtil.isVillagerBucket(itemStack)) {
            event.setCancelled(true);
            return;
        }

        if (!BucketUtil.canPickup(player, clicked.getType())) {
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
            BucketUtil.createVillagerBucket(newStack, clicked, player);
            if (player.getGameMode() != GameMode.CREATIVE) {
                itemStack.setAmount(itemStack.getAmount() - 1);
            }
            HashMap<Integer, ItemStack> failedMap = player.getInventory().addItem(newStack);
            itemStack = newStack;

            if (failedMap.size() == 1) {
                player.dropItem((ItemStack) failedMap.values().toArray()[0]);
            } else if (failedMap.size() > 1) {
                VLog.severe("Yeah somehow you ended up with multiple buckets being created. Not sure what to say here...");
            }
        } else {
            BucketUtil.createVillagerBucket(itemStack, clicked, player);
        }
        clicked.remove();
        event.setCancelled(true);

        VLog.debug("PICKUP", player, clicked, location);

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
        if (!event.getAction().isRightClick() || !BucketUtil.isVillagerBucket(itemStack)) {
            return;
        }
        event.setCancelled(true);

        // Return after cancelling event if a block is null because it is either air or water which could override the villager
        if (event.getClickedBlock() == null) {
            return;
        }

        LivingEntity entity = BucketUtil.entityFromBucket(itemStack, player.getWorld());

        if (!BucketUtil.canPlace(player, entity)) {
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
                meta.getPersistentDataContainer().remove(getKey());
                meta.setMaxStackSize(null);
                if (meta.hasLore()) {
                    meta.lore(null);
                }
            });
        }

        BucketUtil.playPlaceSound(entity);

        VLog.debug("PLACE", player, entity, location);

        VillagerPlaceEvent villagerPlaceEvent = new VillagerPlaceEvent(entity, player, location, itemStack);
        villagerPlaceEvent.callEvent();
    }

    private NamespacedKey getKey() {
        return VillagerInABucket.get().key;
    }
}
