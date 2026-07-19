package com.imjustdoom.villagerinabucket.listener;

import com.imjustdoom.villagerinabucket.BucketUtil;
import com.imjustdoom.villagerinabucket.VLog;
import com.imjustdoom.villagerinabucket.event.PreVillagerDispenserPickupEvent;
import com.imjustdoom.villagerinabucket.event.PreVillagerDispenserPlaceEvent;
import com.imjustdoom.villagerinabucket.event.VillagerDispenserPickupEvent;
import com.imjustdoom.villagerinabucket.event.VillagerDispenserPlaceEvent;
import io.papermc.paper.event.block.BlockPreDispenseEvent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Dispenser;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.HashMap;

public class DispenserListener implements Listener {
    @EventHandler
    public void dispenserInteract(BlockPreDispenseEvent event) {
        ItemStack itemStack = event.getItemStack();
        Block block = event.getBlock();
        if (!(block.getState() instanceof Dispenser dispenser)) {
            return;
        }

        if (BucketUtil.isVillagerBucket(itemStack)) {
            placeVillager(event, block, itemStack, dispenser);
        } else if (itemStack.getType() == Material.BUCKET) {
            pickupVillager(event, block, itemStack, dispenser);
        }
    }

    private void placeVillager(BlockPreDispenseEvent event, Block block, ItemStack itemStack, Dispenser dispenser) {
        event.setCancelled(true);

        BlockFace blockFacing = ((Directional) block.getBlockData()).getFacing();
        Block targetBlock = block.getRelative(blockFacing);
        LivingEntity entity = BucketUtil.entityFromBucket(itemStack, block.getWorld());

        if (!BucketUtil.canDispenserPlace(entity)) {
            VLog.log(String.format("Attempted to place disabled villager (%s) at %s", entity.getType(), targetBlock.getLocation()));
            return;
        }

        Location location = targetBlock.getLocation().add(0.5, 0, 0.5);
        if (blockFacing == BlockFace.DOWN) {
            location.subtract(0, entity.getHeight() - 1, 0);
        }

        if (location.getWorld().getBlockAt(location.clone().subtract(0, 1, 0)).isSolid()) {
            location.setY(Math.floor(location.getY()));
        }

        if (location.getWorld().getBlockAt(location).isSolid()) {
            location.setY(Math.floor(location.getY()) + 1);
        }

        PreVillagerDispenserPlaceEvent preVillagerDispenserPlaceEvent = new PreVillagerDispenserPlaceEvent(entity, dispenser, targetBlock.getLocation(), itemStack);
        if (!preVillagerDispenserPlaceEvent.callEvent()) {
            return;
        }

        dispenser.getInventory().setItem(event.getSlot(), new ItemStack(Material.BUCKET));
        entity.spawnAt(location, CreatureSpawnEvent.SpawnReason.BUCKET);
        BucketUtil.playPlaceSound(entity);

        VLog.debug("DISPENSE", "Dispenser", entity, location);
        VillagerDispenserPlaceEvent villagerDispenserPlaceEvent = new VillagerDispenserPlaceEvent(entity, dispenser, entity.getLocation(), itemStack);
        villagerDispenserPlaceEvent.callEvent();
    }

    private void pickupVillager(BlockPreDispenseEvent event, Block block, ItemStack itemStack, Dispenser dispenser) {
        BlockFace blockFacing = ((Directional) block.getBlockData()).getFacing();
        Block targetBlock = block.getRelative(blockFacing);

        Collection<LivingEntity> entities = targetBlock.getLocation().getWorld().getNearbyLivingEntities(targetBlock.getLocation(), 1);
        for (LivingEntity entity : entities) {
            if (!entity.isValid()
                || (entity.getType() != EntityType.VILLAGER
                    && entity.getType() != EntityType.WANDERING_TRADER
                    && entity.getType() != EntityType.ZOMBIE_VILLAGER)) {
                continue;
            }

            if (!BucketUtil.canDispenserPickup(entity)) {
                VLog.log(String.format("Attempted to pickup disabled villager (%s) at %s", entity.getType(), targetBlock.getLocation()));
                return;
            }

            PreVillagerDispenserPickupEvent preVillagerDispenserPickupEvent = new PreVillagerDispenserPickupEvent(entity, dispenser, targetBlock.getLocation(), itemStack);
            if (!preVillagerDispenserPickupEvent.callEvent()) {
                return;
            }

            event.setCancelled(true);

            // Handle single or multiple bucket stacks
            if (itemStack.getAmount() > 1) {
                ItemStack newStack = new ItemStack(Material.BUCKET);
                BucketUtil.createVillagerBucket(newStack, entity, null);
                itemStack.setAmount(itemStack.getAmount() - 1);
                HashMap<Integer, ItemStack> failedMap = dispenser.getInventory().addItem(newStack);
                itemStack = newStack;

                if (!failedMap.isEmpty()) {
                    block.getWorld().dropItem(targetBlock.getLocation().add(0.5, 0.5, 0.5), failedMap.get(0));
                }
            } else {
                BucketUtil.createVillagerBucket(itemStack, entity, null);
            }
            entity.remove();

            VLog.debug("PICKUP", "Dispenser", entity, entity.getLocation());
            VillagerDispenserPickupEvent villagerPickupEvent = new VillagerDispenserPickupEvent(entity, dispenser, entity.getLocation(), itemStack);
            villagerPickupEvent.callEvent();

            break;
        }
    }
}
