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
 * Event that occurs before a Villager is picked up inside a bucket from a dispenser
 */
public class PreVillagerDispenserPickupEvent extends EntityEvent implements Cancellable {
    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final Dispenser dispenser;
    private final Location location;
    private final ItemStack itemStack;
    private boolean cancelled;

    public PreVillagerDispenserPickupEvent(@NotNull Entity entity, @NotNull Dispenser dispenser, @NotNull Location location, ItemStack itemStack) {
        super(entity);
        this.dispenser = dispenser;
        this.location = location;
        this.itemStack = itemStack;
    }

    /**
     * Returns the involved dispenser
     * @return the dispenser who attempted to pick up the Villager
     */
    public Dispenser getDispenser() {
        return this.dispenser;
    }

    /**
     * Gets the location of the villager that is about to be picked up
     * @return the location of the villager
     */
    public Location getLocation() {
        return location;
    }

    /**
     * Get the ItemStack that was used to click on the villager (Should always be a bucket)
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
