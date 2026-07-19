package com.imjustdoom.villagerinabucket.event;

import org.bukkit.Location;
import org.bukkit.block.Dispenser;
import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Event that occurs before a Villager is placed from a dispenser
 */
public class PreVillagerDispenserPlaceEvent extends EntityEvent implements Cancellable {
    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final Dispenser dispenser;
    private final Location location;
    private final ItemStack itemStack;
    private boolean cancelled;

    public PreVillagerDispenserPlaceEvent(@NotNull Entity entity, @NotNull Dispenser dispenser, @NotNull Location location, ItemStack itemStack) {
        super(entity);
        this.dispenser = dispenser;
        this.location = location;
        this.itemStack = itemStack;
    }

    /**
     * Returns the involved dispenser
     * @return the dispenser who is attempting to place the bucket
     */
    public Dispenser getDispenser() {
        return this.dispenser;
    }

    /**
     * Gets the location where the villager will be placed
     * @return the location of the villager
     */
    public Location getLocation() {
        return location;
    }

    /**
     * Get the Villager in a bucket item that is getting placed
     * @return the ItemStack
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

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
