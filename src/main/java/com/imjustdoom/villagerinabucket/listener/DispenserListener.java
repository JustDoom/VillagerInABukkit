package com.imjustdoom.villagerinabucket.listener;

import com.imjustdoom.villagerinabucket.BucketUtil;
import com.imjustdoom.villagerinabucket.Config;
import com.imjustdoom.villagerinabucket.VLog;
import com.imjustdoom.villagerinabucket.VillagerInABucket;
import io.papermc.paper.event.block.BlockPreDispenseEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Dispenser;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class DispenserListener implements Listener {
    @EventHandler
    public void dispenserInteract(BlockPreDispenseEvent event) {
        ItemStack itemStack = event.getItemStack();

        if (!BucketUtil.isVillagerBucket(itemStack)) {
            return;
        }

        Block block = event.getBlock();
        if (!(block.getState() instanceof Dispenser dispenser)) {
            return;
        }

        Block targetBlock = block.getRelative(((Directional) block.getBlockData()).getFacing());
        LivingEntity entity = BucketUtil.entityFromBucket(itemStack, block.getWorld());

        if (!Config.PERMISSIONS) {
            VLog.severe("""
                    Not using the new permissions system is about to be removed. Please update otherwise the plugin may not function as intended:
                    Follow this to understand how https://github.com/JustDoom/VillagerInABukkit/wiki/Configuring-using-permissions
                    """);
        }

        if (BucketUtil.isVillagerDisabled(entity) && Config.DISABLE_PLACING_OF_DISABLED) {
            VLog.log(String.format("Attempted to place disabled villager (%s) at %s", entity.getType(), targetBlock.getLocation()));
            return;
        }

        Location location = targetBlock.getLocation().add(0.5, 0, 0.5);
        if (location.getWorld().getBlockAt(location.clone().subtract(0, 1, 0)).isSolid()) {
            location.setY(Math.floor(location.getY()));
        }

        if (location.getWorld().getBlockAt(location).isSolid()) {
            location.setY(Math.floor(location.getY()) + 1);
        }

        if (!BucketUtil.canPlaceWithoutPlayer(entity)) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);

        // paper restores the bucket if i don't delay this
        Bukkit.getScheduler().runTask(VillagerInABucket.get(), () -> {
            Inventory delayedInventory = dispenser.getInventory();
            delayedInventory.setItem(event.getSlot(), new ItemStack(Material.BUCKET));
        });

        entity.spawnAt(location, CreatureSpawnEvent.SpawnReason.BUCKET);
        BucketUtil.playPlaceSound(entity);

        VLog.debug("DISPENSE", "Dispenser", entity, location);
    }
}
