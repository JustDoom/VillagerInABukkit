package com.imjustdoom.villagerinabucket.listener;

import com.imjustdoom.villagerinabucket.BucketUtil;
import com.imjustdoom.villagerinabucket.VLog;
import com.imjustdoom.villagerinabucket.VillagerInABucket;
import com.imjustdoom.villagerinabucket.event.PreVillagerPickupEvent;
import com.imjustdoom.villagerinabucket.event.VillagerPickupEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

public class InteractListener implements Listener {
    @EventHandler(priority = EventPriority.HIGH)
    public void villagerInteract(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();

        if (player.getGameMode() == GameMode.SPECTATOR) return;

        ItemStack itemStack = player.getInventory().getItem(event.getHand());
        Entity clicked = event.getRightClicked();

        // If it is a villager bucket item cancel the event - stops milking and picking up multiple villagers in a single bucket (overrides old one)
        // Needs to be run before checking the clicked entity type because the event won't be cancelled in the case it is
        // not a supported entity type, but it needs to be cancelled if it ends up being an existing villager bucket item.
        if (BucketUtil.isVillagerBucket(itemStack)) {
            event.setCancelled(true);
            return;
        }

        // Make sure there is the possibility of it being picked up. Makes sure it is alive and is the correct entity type
        if (!BucketUtil.hasPossibilityOfPickingUp(clicked, itemStack)) return;

        if (!BucketUtil.canPickup(player, clicked.getType())) {
            player.sendMessage(Component.text("You are not allowed to pickup this villager"));
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

            if (!failedMap.isEmpty()) {
                player.dropItem(failedMap.get(0));
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

    @EventHandler(priority = EventPriority.HIGH)
    public void bucketInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (player.getGameMode() == GameMode.SPECTATOR) return;

        ItemStack itemStack = event.getItem();

        // Ensure interaction point and itemStack is not null
        if (event.getInteractionPoint() == null || itemStack == null) return;

        // Check if the action is related to a villager in a bucket item
        if (!event.getAction().isRightClick() || !BucketUtil.isVillagerBucket(itemStack)) return;
        event.setCancelled(true);

        // Return after cancelling event if a block is null because it is either air or water which could override the villager
        if (event.getClickedBlock() == null) return;

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

        BucketUtil.spawnVillagerFromBucket(entity, player, itemStack, location);
    }

    private NamespacedKey getKey() {
        return VillagerInABucket.get().key;
    }
}
