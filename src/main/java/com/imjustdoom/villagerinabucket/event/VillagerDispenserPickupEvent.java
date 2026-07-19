package com.imjustdoom.villagerinabucket.event;

import org.bukkit.Location;
import org.bukkit.block.Dispenser;
import org.bukkit.entity.Entity;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Event that occurs after a Villager has been picked up by a bucket from a dispenser
 */
public class VillagerDispenserPickupEvent extends EntityEvent {
    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final Dispenser dispenser;
    private final Location location;
    private final ItemStack itemStack;

    public VillagerDispenserPickupEvent(@NotNull Entity entity, @NotNull Dispenser dispenser, @NotNull Location location, ItemStack itemStack) {
        super(entity);
        this.dispenser = dispenser;
        this.location = location;
        this.itemStack = itemStack;
    }

    /**
     * Returns the involved dispenser
     * @return the dispenser who picked up the Villager
     */
    public Dispenser getDispenser() {
        return this.dispenser;
    }

    /**
     * Gets the location where the picked up villager was
     * @return the location of the villager
     */
    public Location getLocation() {
        return location;
    }

    /**
     * Get the ItemStack of the Villager in a bucket item
     * @return the Villager in a bucket item
     */
    public ItemStack getItemStack() {
        return this.itemStack;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }
}
